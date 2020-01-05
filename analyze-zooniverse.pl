#!/usr/bin/perl -w

use strict;
use Parse::CSV;
use JSON::PP;
use Data::Dump 'dump';
use Data::Dump::Filtered 'dump_filtered';

use sort 'stable';

my $filename = 'filk-archive-beta-test-classifications.csv';

my $audio_output_file = 'audio_classifications.csv';
my $file_output_file = 'file_classifications.csv';

my (%audio_classifications, %file_classifications);

my (%audio_tasklabels, %file_tasklabels);
		    
sub dump_json($) {
    return dump_filtered ( $_[0], sub { { dump => $_[1] ? "true" : "false" } if JSON::PP::is_bool $_[1] } );
}

sub parse_classification_audio($)
{
    my ($value) = @_;

    my @subject_ids = keys(%{$value->{subject_data}});

    my $subject_id = $subject_ids[0];

    my $orig_file = $value->{subject_data}{$subject_id}{"#origfile"};

    my $key = $value->{subject_data}{$subject_id}{clipstart};
    my $location = $value->{subject_data}{$subject_id}{location};
    my $event = $value->{subject_data}{$subject_id}{event};

    my $cliptime = $value->{subject_data}{$subject_id}{clipstart} . " - " . $value->{subject_data}{$subject_id}{clipend};

    my $workflow_version = $value->{workflow_version};

    my %classes;

    foreach my $class (@{$value->{annotations}}) {
	my $task_id = $class->{task};
	if (!exists($audio_tasklabels{$task_id}) || $audio_tasklabels{$task_id}[1] < $workflow_version) {
	    $audio_tasklabels{$task_id} = [ $class->{task_label}, $workflow_version ];
	}

	my $class_value;
	if (ref $class->{value}) {
	    $class_value = join('; ', @{$class->{value}});
	}
	else {
	    $class_value = $class->{value};
	}
	$classes{$task_id} = $class_value;
    }
    
    push(@{$audio_classifications{$orig_file}{$key}},
	 {user => $value->{user_name}, time => $value->{metadata}{finished_at}, cliptime => $cliptime,
	  location => $location, event => $event, classifications => \%classes});
}


sub parse_classification_file($)
{
    my ($value) = @_;
    my @subject_ids = keys(%{$value->{subject_data}});

    my $subject_id = $subject_ids[0];

    my $source = $value->{subject_data}{$subject_id}{"source"};
    my $file = $value->{subject_data}{$subject_id}{"#file"};

    my $workflow_version = $value->{workflow_version};

    my %classes;

    foreach my $class (@{$value->{annotations}}) {
	my $task_id = $class->{task};
	if (!exists($file_tasklabels{$task_id}) || $file_tasklabels{$task_id}[1] < $workflow_version) {
	    $file_tasklabels{$task_id} = [ $class->{task_label}, $workflow_version ];
	}

	my $class_value;
	if (ref $class->{value}) {
	    $class_value = join('; ', @{$class->{value}});
	}
	else {
	    $class_value = $class->{value};
	}
	$classes{$task_id} = $class_value;
    }

    push(@{$file_classifications{$source}{$file}},
	 {user => $value->{user_name}, time => $value->{metadata}{finished_at},
	  classifications => \%classes});
}


sub parse_classification($)
{
    my ($value) = @_;

    my @subject_ids = keys(%{$value->{subject_data}});

    my $subject_id = $subject_ids[0];

    if (exists($value->{subject_data}{$subject_id}{"#origfile"})) {
	parse_classification_audio($value);
    }
    elsif (exists($value->{subject_data}{$subject_id}{"#file"}) &&
	   exists($value->{subject_data}{$subject_id}{"source"})) {
	parse_classification_file($value);
	return;
    }
    else {
	print STDERR "Unexpected subject data:\n";
	print STDERR dump_json($value), "\n";
    }
}

# Sort clip start times.  Takes advantage of the fact that if durations are the same 
# lexicographic length they sort lexicographically, and if they're not then longer strings
# are longer durations.
sub sortclips
{
    return (length($a) != length($b) ? length($a) <=> length($b) : $a cmp $b);
}


# Quote a string for a csvfile
sub cq($)
{
    my ($str) = @_;
    if (!defined($str)) {
	return "";
    }
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

my @rows;

my $parser = Parse::CSV->new(
    file => "$filename",
    names => 1,
    );

while ( my $value = $parser->fetch ) {
    foreach my $field ("subject_data", "metadata", "annotations") {
	$value->{$field} = decode_json $value->{$field};
    }

    parse_classification($value);
    push(@rows, $value);
}

if ( $parser->errstr ) {
    die("$filename: $parser->errstr")
}

# print dump_json( \@rows );

# exit 0;

sub output_audio($) {
    my ($f) = @_;

    my @audio_columns = (
	"Location",
	"Event",
	"Original File",
	"Classifier",
	"Classification Time",
	"Clip time"
	);

    my @tasks = sort keys %audio_tasklabels;

    foreach my $task_id (@tasks) {
	push(@audio_columns, $audio_tasklabels{$task_id}[0]);
    }

    print $f csv(@audio_columns);

    foreach my $file (sort keys %audio_classifications) {
	foreach my $clipstart (sort sortclips keys %{$audio_classifications{$file}}) {
	    foreach my $class (@{$audio_classifications{$file}{$clipstart}}) {
		my @row = (
		    $class->{location},
		    $class->{event},
		    $file,
		    $class->{user},
		    $class->{time},
		    $class->{cliptime}
		    );

		foreach my $task_id (@tasks) {
		    push(@row, $class->{classifications}{$task_id});
		}

		print $f csv(@row);
	    }
	}
	print $f "\n";
    }
}


sub output_files($) {
    my ($f) = @_;

    my @file_columns = (
	"Source",
	"File",
	"Classifier",
	"Classification Time",
	);

    my @tasks = sort keys %file_tasklabels;

    foreach my $task_id (@tasks) {
	push(@file_columns, $file_tasklabels{$task_id}[0]);
    }

    print $f csv(@file_columns);

    foreach my $source (sort keys %file_classifications) {
	foreach my $file (sort keys %{$file_classifications{$source}}) {
	    foreach my $class (@{$file_classifications{$source}{$file}}) {
		my @row = (
		    $source,
		    $file,
		    $class->{user},
		    $class->{time},
		    );

		foreach my $task_id (@tasks) {
		    push(@row, $class->{classifications}{$task_id});
		}

		print $f csv(@row);
	    }
	}
	print $f "\n";
    }
}

open(my $audio_out, ">", $audio_output_file) or die ("Couldn't open $audio_output_file: $!");

output_audio($audio_out);

close($audio_out) or die ("Couldn't close $audio_output_file: $!");

open(my $file_out, ">", $file_output_file) or die ("Couldn't open $file_output_file: $!");

output_files($file_out);

close($file_out) or die ("Couldn't close $file_output_file: $!");
