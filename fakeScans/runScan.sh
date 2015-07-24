#!/bin/bash

echo "Running Scan" 1>&2

if [ $# -ge 1 ]; then
echo "Printing scan results in $1" 1>&2
find -printf "%P\n" | /global/dv_scratch/jsybrandt/DistSync/fakeScans/scanMaker.sh $1
else
find -printf "%P\n" | /global/dv_scratch/jsybrandt/DistSync/fakeScans/scanMaker.sh
fi
