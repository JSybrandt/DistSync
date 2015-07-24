#!/bin/bash

#takes two directories, scans them, generates jobs, syncs them, emails me the stats
#assumes param 1 is the source, param2 is the destination
#must be directories

if [ $# == 2 ]
then
	rm /global/dv_scratch/jsybrandt/DistSync/tmp/jobs/*
	rm /global/dv_scratch/jsybrandt/DistSync/tmp/logs/*
	echo "Syncing $1 with $2"
	pushd $1
	scandir /global/dv_scratch/jsybrandt/source.scan
	popd
	pushd $2
	scandir /global/dv_scratch/jsybrandt/dest.scan
	popd
	/global/dv_scratch/jsybrandt/DistSync/jobSplitter/compare -n /global/dv_scratch/jsybrandt/source.scan -o /global/dv_scratch/jsybrandt/dest.scan -f /global/dv_scratch/jsybrandt/DistSync/tmp/jobs
	java -Xmx4G -jar /global/dv_scratch/jsybrandt/DistSync/distsync.jar -f $1 -s $2 -m tlfssv75 tlfssv74 tlfssv73 tlfssv72
	echo "Syncing $1 and $2 has completed." | mail -a /global/dv_scratch/jsybrandt/DistSync/tmp/logs/MASTER.log -a /global/dv_scratch/jsybrandt/DistSync/tmp/logs/schedule.bmp -s "DistSync Completed" jsybrandt@lbl.gov

	rm /global/dv_scratch/jsybrandt/source.scan
	rm /global/dv_scratch/jsybrandt/dest.scan
	

else
	echo "Failed to supply source and destination."
fi
	


