#!/bin/bash

set -x
set -e

panoptes project download -g -t classifications 9901 filk-archive-classifications.csv
