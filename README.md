# The Filk Archive

This directory contains a number of utilities and notes for use by the Filk Archive project.

The program `zoom-mixdown.sh` mixes down Zoom H2 and H2n 4-channel recordings to 2-channel stereo.

The program `split-audio.pl` splits and encodes an audio file (in any format readable by SoX) into mp3 clips,
suitable for zooniverse, and also outputs a csv file formatted appropriately for the zooniverse `panoptes` tool.
You must have the SoX toolchain installed, as well as the Perl DateTime module.  (On Cygwin, both of these
can be installed as Cygwin packages with the Cygwin `setup` tool.)

The program `analyze-zooniverse.pl` processes the output of the zooniverse classifications.csv into
a csv file suitable for human consumption (when imported into a spreadsheet).


To generate sha256 checksums of all the files in a directory:

$ `find . -name sha256-sum.txt -o -type f -print0 | xargs -0 sha256sum >> sha256-sum.txt`


To create flac files from all the wav files in a directory:

$ `find . -iname '*.wav' -print0 | xargs -0 flac -V --keep-foreign-metadata -f --delete-input-file`


# Uploading split files to panoptes

To split files, run the `split-audio` file something like:

$ `~/split-audio.pl --recorded-by="J. Spencer Love" --metadata="British Filk Convention: Obliter-8"/metadata.json --output-dir=/cygdrive/g/Split/Obliter-8 "British Filk Convention: Obliter-8"/*.wav`

The `metadata` file is a JSON file containing metadata both for the entire "location" (Convention), and the individual
"events" (recordings).  For example the metadata for the above invocaton looks like:

```
{
    "location": "British Filk Convention: Obliter-8",
    "date": "31 Jan - 2 Feb 1997",
    
    "British Filk Convention: Obliter-8/Obliter-8 #1.wav": {"event": "Tape 1"},
    "British Filk Convention: Obliter-8/Obliter-8 #2.wav": {"event": "Tape 2"},
    "British Filk Convention: Obliter-8/Obliter-8 #3.wav": {"event": "Tape 3"},
    "British Filk Convention: Obliter-8/Obliter-8 #4.wav": {"event": "Tape 4"},
    "British Filk Convention: Obliter-8/Obliter-8 #5.wav": {"event": "Tape 5"},
    "British Filk Convention: Obliter-8/Obliter-8 #6.wav": {"event": "Tape 6"},
    "British Filk Convention: Obliter-8/Obliter-8 #8.wav": {"event": "Tape 8"}
}
```

Once the files are produced, they can be uploaded to Zooniverse using the panoptes tool.  Change to the
output directory specified above, and then run (e.g.)

$ `panoptes subject-set upload-subjects 78566 subjects.csv`

The correct subject-set ID must be chosen; subject-sets can be listed with

$ `panoptes subject-set ls -p 9901`

The panoptes tool can be installed using Python's `pip` utility, as

$ `pip install panoptescli`

