#!/bin/bash

# Find Zoom H2 and H2n 4-channel files, and mix them down using sox to 2-channel sterero.

# Zoom H2
find "$@" -path '*/4CH/SR*F.WAV' -o -path '*/4CH/SR*F.wav' -o -path '*/4CH/SR*F.flac' | while read F
do
    R="$(echo "$F" | sed 's/\(SR[0-9]*\)F/\1R/')"
    if test -r "$R"
    then
	OUT="$(echo "$F" | sed 's/\(SR[0-9]*\)F/\1-mixed/')"
	sox -S -m "$F" "$R" "$OUT"
    fi
done

# Zoom H2n
find "$@" -name 'SR*XY.WAV' -o -name 'SR*XY.wav' -o -name 'SR*XY.flac' | while read XY
do
    MS="$(echo "$XY" | sed 's/\(SR[0-9]*\)XY/\1MS/')"
    if test -r "$MS"
    then
	OUT="$(echo "$XY" | sed 's/\(SR[0-9]*\)XY/\1-mixed/')"
	sox "$MS" -t wav - remix -m 1,2 1,2i | sox -S -m "$XY" - "$OUT"
    fi
done


