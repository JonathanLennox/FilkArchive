# The Filk Archive

This directory contains a number of utilities and notes for use by the Filk Archive project.

The program `split-flac.pl` splits and encodes a flac file into mp3 clips, suitable for zooniverse,
and also outputs a csv file with formatted appropriately for the zooniverse `panoptes` tool.

To create flac files from all the wav files in a directory:

$ `find . -name '*.wav' -print0 | xargs -0 flac`

To generate sha256 checksums of all the wav files in a directory:

$ `find . -name '*.wav' -print0 | xargs -0 sha256sum > sha256-sum.txt`

