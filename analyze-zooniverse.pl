#!/usr/bin/perl -w

use strict;
use Parse::CSV;
use JSON::PP;
use Data::Dump 'dump';
use Data::Dump::Filtered 'dump_filtered';

use sort 'stable';

my $filename = 'filk-archive-beta-test-classifications.csv';

my %has_index;
my %classifications;


my %tasklabels;
my %shortlabels;
		    
sub dump_json($) {
    return dump_filtered ( $_[0], sub { { dump => $_[1] ? "true" : "false" } if JSON::PP::is_bool $_[1] } );
}


sub parse_classification($)
{
    my ($value) = @_;

    my @subject_ids = keys(%{$value->{subject_data}});

    my $subject_id = $subject_ids[0];

    my $orig_file = $value->{subject_data}{$subject_id}{"#origfile"};

    my $have_index = exists($value->{subject_data}{$subject_id}{"#index"});

    my $key = $value->{subject_data}{$subject_id}{clipstart};
    my $location = $value->{subject_data}{$subject_id}{location};
    my $event = $value->{subject_data}{$subject_id}{event};

    my $cliptime = $value->{subject_data}{$subject_id}{clipstart} . " - " . $value->{subject_data}{$subject_id}{clipend};

    my $workflow_version = $value->{workflow_version};

    my %classes;

    foreach my $class (@{$value->{annotations}}) {
	my $task_id = $class->{task};
	if (!exists($tasklabels{$task_id}) || $tasklabels{$task_id}[1] < $workflow_version) {
	    $tasklabels{$task_id} = [ $class->{task_label}, $workflow_version ];
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
    
    push(@{$classifications{$orig_file}{$key}},
	 {user => $value->{user_name}, time => $value->{metadata}{finished_at}, cliptime => $cliptime,
	  location => $location, event => $event, classifications => \%classes});
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

my @columns = (
    "Location",
    "Event",
    "Original File",
    "Classifier",
    "Classification Time",
    "Clip time"
    );

my @tasks = sort keys %tasklabels;

foreach my $task_id (@tasks) {
    push(@columns, $tasklabels{$task_id}[0]);
}

print csv(@columns);

foreach my $file (sort keys %classifications) {
    foreach my $clipstart (sort sortclips keys %{$classifications{$file}}) {
	foreach my $class (@{$classifications{$file}{$clipstart}}) {
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

	    print csv(@row);
	}
    }
    print "\n";
}
