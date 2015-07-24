#!/bin/bash

#takes two directories, scans them, generates jobs, syncs them, emails me the stats
#assumes param 1 is the source, param2 is the destination
#must be directories

if [ $# == 2 ]
then
	rm -f /common/jsybrandt/DistSync/tmp/jobs/*
	rm -f /common/jsybrandt/DistSync/tmp/logs/*
	echo "Syncing $1 with $2"
	pushd $1
	/common/jsybrandt/DistSync/fakeScans/runScan.sh /common/jsybrandt/source.scan
	popd
	pushd $2
	/common/jsybrandt/DistSync/fakeScans/runScan.sh /common/jsybrandt/dest.scan
	popd
	/common/jsybrandt/DistSync/jobSplitter/compare -n /common/jsybrandt/source.scan -o /common/jsybrandt/dest.scan -f /common/jsybrandt/DistSync/tmp/jobs
	java -Xmx4G -jar /common/jsybrandt/DistSync/distsync.jar -f $1 -s $2 -m tlfssv229 tlfssv230 tlfssv231 tlfssv232 tlfssv233 tlfssv234 tlfssv235 tlfssv236
	echo "Syncing $1 and $2 has completed." | mail -a /common/jsybrandt/DistSync/tmp/logs/MASTER.log -a /common/jsybrandt/DistSync/tmp/logs/schedule.bmp -s "DistSync Completed" jsybrandt@lbl.gov

	rm -f /common/jsybrandt/source.scan
	rm -f /common/jsybrandt/dest.scan
	

else
	echo "Failed to supply source and destination."
fi
	


