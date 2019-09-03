#!/usr/bin/perl -w

use strict;
use DateTime::Duration;
use File::Spec;
use File::Path qw(make_path);

# A script to split flac files into short chunks, and encode them as MP3s.
# This is intended for use by the FilkArchive project to create listening samples as subjects for zooniverse.

# Parameters

my $clip_duration = 60; # Seconds
my $mp3_bitrate = 128; # kbps
# Note that zooniverse requires that subject files be no bigger than 1 meg;
# consider this when setting the above two parameters.
my $output_dir = "Split";
my $output_file_random_bytes = 8;
my $csv_file = 'subjects.csv';

# Make sure flac and lame are installed

my $ret = system("flac --version > /dev/null 2>&1");
die "flac decoder not found" if ($ret != 0);
    
$ret = system("lame --version > /dev/null 2>&1");
die "lame MP3 encoder not found" if ($ret != 0);

# Given a filename to a flac file, determine its length in samples, and its sampling rate.
sub read_flac_length($)
{
    my ($flacfile) = (@_);
    my (@cmd) = ("metaflac", '--show-total-samples', '--show-sample-rate', $flacfile);
    open(my $flacdesc, "-|", @cmd) or die("Couldn't exec metaflac on $flacfile: $!");
    my ($hz, $samples);
    while (<$flacdesc>) {
	chomp;
	$samples = $_ if ($. == 1);
	$hz = $_ if ($. == 2);
    }
    if (!close $flacdesc) {
	print STDERR ("Error executing metaflac on $flacfile: not a flac file?\n");
	return undef;
    }
    if (!defined($samples)) {
	print STDERR "Couldn't parse metaflac output on $flacfile: not a flac file?\n";
	return undef;
    }
    return ($samples, $hz);
}

sub format_duration($) {
    my ($duration) = @_;

    my $min = int($duration/60);
    my $nsec = 1e9 * ($duration - $min*60);

    my $dur = DateTime::Duration->new(
             minutes     => $min,
             nanoseconds => $nsec
	);

    my ($h, $m, $s, $ns) = $dur->in_units('hours', 'minutes', 'seconds', 'nanoseconds'),
    my $ret;

    if ($dur->in_units('hours') > 0) {
	$ret = sprintf("%d:%02d:%02d", $h, $m, $s);
    }
    elsif ($dur->in_units('minutes') > 0) {
	$ret = sprintf("%d:%02d", $m, $s);
    }
    else {
	$ret = sprintf("%d", $s);
    }
    if ($ns >= 0.001 ) {
	$ret .= sprintf(".%03d", $ns/1.0e6);
    }

    return $ret;
}

sub random_file($$$)
{
    my ($outdir, $prefix, $ext) = @_;
    if (!defined $::urandom) {
	open ($::urandom, "<", "/dev/urandom") or die("Couldn't open /dev/urandom: $!\n");
    }
    read($::urandom, my $random_bytes, $output_file_random_bytes)
	or die("Couldn't read $output_file_random_bytes bytes from /dev/urandom");
    my $filebase = unpack("h*", $random_bytes);
    return File::Spec->catfile($outdir, $prefix . $filebase . $ext);
    
}

sub get_output_dir($)
{
    my ($file) = @_;
    my ($volume, $dir, $filename) = File::Spec->splitpath($file);
    my $ret = File::Spec->catpath($volume, $dir, $output_dir);
    make_path($ret);
    return $ret;
}

sub open_csvfile($)
{
    my ($outdir) = @_;
    my $csvfile = File::Spec->catfile($outdir, $csv_file);
    my $fh;
    if (-w "$csvfile") {
	open ($fh, ">>", $csvfile) or die("Couldn't open $csvfile: $!");
    }
    else {
	open ($fh, ">>", $csvfile) or die("Couldn't open $csvfile: $!");
	print $fh "#filename,#origfile,location,event,clipstart,clipend\n";
    }
    return $fh;
}

sub extract_loc_and_event($)
{
    my ($file) = @_;

    my ($volume, $dir, $filename) = File::Spec->splitpath($file);
    my (@dirs) = File::Spec->splitdir($dir);

    my ($location) = $dirs[-2];
    my ($event);
    ($event = $filename) =~ s/\.[a-z0-9]*$//;
    
    return ($location, $event)
}

# Quote a string for a csvfile
sub cq($)
{
    my ($str) = @_;
    if ($str !~ /[,\"\n\r]/) {
	return $str;
    }
    $str =~ s/\"/\"\"/g;
    return "\"$str\"";
}

sub csv(@)
{
    return join(",", map {cq($_)} @_) . "\n";
}

sub process_flac($)
{
    my ($file) = @_;

    my ($samples, $hz) = read_flac_length($file);
    next if !defined($samples);

    my $duration = $samples / $hz;

    printf "%s: %s (%d samples at %d hz)\n", $file, format_duration($duration), $samples, $hz;

    my $outdir = get_output_dir($file);
    my $csv = open_csvfile($outdir);

    my $clip_duration_samples = $clip_duration * $hz;
    
    for (my $clipstart = 0; $clipstart < $samples; $clipstart += $clip_duration_samples) {
	my $clipend = $clipstart + $clip_duration_samples;
	$clipend = $samples if $clipend >= $samples;

	my ($location, $event) = extract_loc_and_event($file);

	my $outfile = random_file($outdir, "subj-", ".mp3");
	my (undef, undef, $outfilebase) = File::Spec->splitpath($outfile);

	my $cmd = "flac -d -o - --skip=$clipstart --until=$clipend \"$file\" | lame --preset $mp3_bitrate - \"$outfile\"";
	print "$cmd\n";
	my $status = system($cmd);
	if ($status != 0) {
	    next;
	}
	print $csv csv($outfilebase, $file, $location, $event, format_duration($clipstart/$hz), format_duration($clipend/$hz));
    }
    close($csv) or die("Error closing csv file");
}

foreach my $file (@ARGV) {
    process_flac($file);
}
