# The Filk Archive

This directory contains a number of utilities and notes for use by the Filk Archive project.

The program `split-audio.pl` splits and encodes an audio file (in any format readable by SoX) into mp3 clips,
suitable for zooniverse, and also outputs a csv file formatted appropriately for the zooniverse `panoptes` tool.
You must have the SoX toolchain installed.

The program `analyze-zooniverse.pl` processes the output of the zooniverse classifications.csv into
a csv file suitable for human consumption (when imported into a spreadsheet).

To create flac files from all the wav files in a directory:

$ `find . -name '*.wav' -print0 | xargs -0 flac`

To generate sha256 checksums of all the wav files in a directory:

$ `find . -name '*.wav' -print0 | xargs -0 sha256sum > sha256-sum.txt`

