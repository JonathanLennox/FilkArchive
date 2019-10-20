#!/usr/bin/perl -w

use strict;
use DateTime::Duration;
use File::Spec;
use File::Path qw(make_path);
use POSIX qw(ceil strftime);
use JSON::PP;
use Getopt::Long;

# A script to split audio files into short chunks, and encode them as MP3s.
# The input can be any format understood by SoX, but wav or flac are recommended.
# This is intended for use by the FilkArchive project to create listening samples as subjects for zooniverse.

# Parameters

my $clip_duration = 60; # Seconds
my $mp3_bitrate = 128; # kbps
# Note that zooniverse requires that subject files be no bigger than 1 meg;
# consider this when setting the above two parameters.
my $output_dir_name = "Split";
my $output_file_random_bytes = 8;
my $csv_file = 'subjects.csv';

my $recorded_by = "Harold Stein";
my $file_date_cutoff = 2019; # The cut-off after which we will consider file dates to be "new".

my $metadata_file;
my $metadata;
my $output_dir;

GetOptions("recorded-by=s" => \$recorded_by,
	   "metadata=s" => \$metadata_file,
	   "output-dir=s" => \$output_dir);

# Make sure SoX is installed

my $ret = system("sox --version > /dev/null 2>&1");
die "SoX tool not found" if ($ret != 0);
    
# Given a filename to an audio file, determine its length in samples, and its sampling rate.
sub read_audio_length($)
{
    my ($audiofile) = (@_);

    my (@cmd_s) = ("soxi", '-s', $audiofile);
    open(my $soxi_samp, "-|", @cmd_s) or die("Couldn't exec soxi on $audiofile: $!");
    my $samples;
    while (<$soxi_samp>) {
	chomp;
	$samples = $_;
    }
    if (!close $soxi_samp) {
	print STDERR ("Error executing soxi on $audiofile: not an audio file?\n");
	return undef;
    }
    if (!defined($samples)) {
	print STDERR "Couldn't parse soxi output on $audiofile: not an audio file?\n";
	return undef;
    }

    my (@cmd_r) = ("soxi", '-r', $audiofile);
    open(my $soxi_rate, "-|", @cmd_r) or die("Couldn't exec soxi on $audiofile: $!");
    my $hz;
    while (<$soxi_rate>) {
	chomp;
	$hz = $_;
    }
    if (!close $soxi_rate) {
	print STDERR ("Error executing soxi on $audiofile: not an audio file?\n");
	return undef;
    }
    if (!defined($hz)) {
	print STDERR "Couldn't parse soxi output on $audiofile: not an audio file?\n";
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
    if ($ns >= 0.001) {
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
    if (defined($output_dir)) {
	make_path($output_dir);
	return $output_dir;
    }

    my ($volume, $dir, $filename) = File::Spec->splitpath($file);
    my $ret = File::Spec->catpath($volume, $dir, $output_dir_name);
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
	print $fh "#filename,#origfile,location,event,recorded by,#index,date,clipstart,clipend\n";
    }
    return $fh;
}

sub extract_loc_and_event($)
{
    my ($file) = @_;

    my ($volume, $dir, $filename) = File::Spec->splitpath($file);
    my (@dirs) = File::Spec->splitdir($dir);

    my ($location, $event);

    if (exists($metadata->{$file}{location})) {
	$location = $metadata->{$file}{location};
    }
    elsif (exists($metadata->{location})) {
	$location = $metadata->{location};
    }
    else {
	$location = $dirs[-2];
    }

    if (exists($metadata->{$file}{event})) {
	$event = $metadata->{$file}{event};
    }
    else {
	($event = $filename) =~ s/\.[a-z0-9]*$//;

	$event =~ s/ - TRUNCATED//;
    }

    return ($location, $event)
}

# Get the date of the file, if it seems to be in the past.
sub get_file_date($)
{
    my ($file) = @_;

    if (exists($metadata->{$file}{date})) {
	return $metadata->{$file}{date};
    }
    elsif (exists($metadata->{date})) {
	return $metadata->{date};
    }

    my (@stat) = stat($file) or die("Couldn't stat $file: $!");
    my $mtime = $stat[9];
    my (@ltime) = localtime($mtime);
    my $year = $ltime[5] + 1900;
    return "" if ($year >= $file_date_cutoff);
    return strftime("%e %b %Y", @ltime);
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

sub process_file($)
{
    my ($file) = @_;

    my ($samples, $hz) = read_audio_length($file);
    next if !defined($samples);

    my $duration = $samples / $hz;
    my $clip_duration_samples = $clip_duration * $hz;

    my $file_date = get_file_date($file);
    my $file_date_desc = ($file_date ne "" ? $file_date : "date unknown");
    my ($location, $event) = extract_loc_and_event($file);

    printf "%s: %s (%d samples at %d hz)\n", $file, format_duration($duration), $samples, $hz;
    printf "%s, %s, %s\n", $location, $event, $file_date_desc;

    my $num_clips = ceil($duration/$clip_duration);
    my $clipdigits = ceil(log($num_clips)/log(10));

    my $outdir = get_output_dir($file);
    my $csv = open_csvfile($outdir);

    my @cmd = ("sox", "-S", $file, "-C", "128", "$outdir/subj-%${clipdigits}n.mp3",
	       "trim", "0", $clip_duration, ":", "newfile", ":", "restart");
    print join(" ", @cmd), "\n";
    my $status = system(@cmd);
    if ($status != 0) {
	next;
    }

    for (my $idx = 1; $idx <= $num_clips; $idx++) {
	my $clipstart = ($idx-1) * $clip_duration_samples;
	my $clipend = $clipstart + $clip_duration_samples;
	$clipend = $samples if $clipend >= $samples;

	my $infile = File::Spec->catfile($outdir, sprintf("subj-%.${clipdigits}d.mp3", $idx));
	my $outfile = random_file($outdir, "subj-", ".mp3");
	
	if (! -r $infile) {
	    die("$infile not found: error in sox command?");
	}

	rename($infile, $outfile) or die ("Error renaming $infile to $outfile: $!\n");

	my (undef, undef, $outfilebase) = File::Spec->splitpath($outfile);

	print $csv csv($outfilebase, $file, $location, $event, $recorded_by, $idx, $file_date, format_duration($clipstart/$hz), format_duration($clipend/$hz));
    }
    close($csv) or die("Error closing csv file");
}

if (defined($metadata_file)) {
    local $/;
    my $json = JSON::PP->new;
    $json->relaxed();
    open (my $fh, '<', $metadata_file) or die ("Couldn't open $metadata_file: $!");
    my $metadata_text = <$fh>;
    $metadata = $json->decode( $metadata_text );
}

foreach my $file (@ARGV) {
    process_file($file);
}
