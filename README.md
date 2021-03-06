# The Filk Archive

This directory contains a number of utilities and notes for use by the Filk Archive project.

The program `zoom-mixdown.sh` mixes down Zoom H2 and H2n 4-channel recordings to 2-channel stereo.

The program `split-audio.pl` splits and encodes an audio file (in any format readable by SoX) into mp3 clips,
suitable for zooniverse, and also outputs a csv file formatted appropriately for the zooniverse `panoptes` tool.
You must have the SoX toolchain installed.

The program `analyze-zooniverse.pl` processes the output of the zooniverse classifications.csv into
a csv file suitable for human consumption (when imported into a spreadsheet).


To generate sha256 checksums of all the files in a directory:

$ `find . -name sha256-sum.txt -o -type f -print0 | xargs -0 sha256sum >> sha256-sum.txt`


To create flac files from all the wav files in a directory:

$ `find . -iname '*.wav' -print0 | xargs -0 flac -V --keep-foreign-metadata -f --delete-input-file`

