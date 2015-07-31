DISTSYNC USERS MANUAL
======================

Overview
--------

Distsync currently is comprised of two seperate software components. The job generator (Located in ./jobGenerator/) and the distsync runner (Linked to at ./distsync.jar). The generator was written in C++0x while the runner was written in Java 1.7. The general command flow is to obtain two file scans and generate jobs in ./tmp/jobs, then using distsync.jar the job files are acted upon by multiple workers.

Job Generator
-------------

###File Scans###

Two scan files are needed to produce jobs. One which is taken from an up-to-date file system, and one which is taken from an out-of-date file system. They scan must be sorted alphabetically by path, and must contain the following attributes, in order, seperated by the pipe character "|".

The attributes are:
1. File Set Name
2. Inode Number
3. Misc Attributes (as defined by https://www-01.ibm.com/support/knowledgecenter/api/content/SSFKCN_4.1.0/com.ibm.cluster.gpfs.v4r1.gpfs200.doc/bl1adv_usngfileattrbts.htm?locale=en )
4. File Size
5. Number of Hard Links
6. User ID
7. Group ID
8. Mode
9. Access Time
10. Modification Time
11. Block Size
12. Change Time
13. Path

###Generation Options###

The Job Generator requires three command line arguments to run.

**-n [file]** - The path specified after the -n flag directs the job generator to the up-to-date file scan.

**-o [file]** - The path specified after the -o flag directs the job generator to the out-of-date file scan.

**-f [directory]** - The path specified after the -f flag directs the job Generator to a directory which it will place the resulting job files.

Example:
./jobGenerator/compare -o /scan/oldScan.txt -n /scan/newScan.txt -f ./tmp/jobs/

###Output###

The job generator lists its status to stdout, and outputs jobs in the directory spedified by the -f flag. A series of number pairs are output to stdout every time a job is generated. The first number represents the number of files in the job, and the second represents the number of bytes. Any errors or problematic records are also reported to stdout. At the end of generation, a simple overview of the number of changes is output as well. 


Distsync Runner
---------------

###Distributed Architecture###

Distsync.jar is used to start both the distsync manager and worker. Both the manager and a set of workers can be spawned by a single call to distsync using the -m option as described below. 

###Job Files###

The distsync java program will respond to jobs located in "./tmp/jobs" and place log files for each job in "./tmp/logs". 

###Distsync Options###

**-f [directory]** - the -f flag specifies the fresh, or up-to-date directory. This must be the root directory that was used in the up-to-date scan. In order words, every path in the up-to-date scan must be valid if referenced relative to this directory.

**-s [directory]** - the -s flag specifies the stale, or out-of-date directory. The requirements for this directory are the same as the fresh directory.

**-w [name]** - the -w flag specifies that distsync should be started as a worker. This flag is included for debugging purposes only. This flag must also be specified **last.** The name which follows -w should be a valid ip or local name of a running distsync manager.

**-m [list of names]** - the -m flag specifies that distsync should be started as a manager. This flag must be specified **last.** The list of names is space seperated, and each name must be a valid ip or local name. A distsync worker will be spawned on each host specified in the list. Each worker must have the same access to the fresh and stale file systems, as well as to distsync.jar. 

 Example:

java -jar distsync.jar -f /global/FRESH -s /global/STALE/ -m tlfssv75 tlfssv74 tlfssv73 tlfssv72

###Output###

The distsync manager will produce debugging output to stdout, describing the jobs that are being sent to each worker. Each worker will output debugging information to stdout, although this output is normally not assiciated with any screen and therefore ignored. Each worker will also output results to "./tmp/logs/"  with the time of that log, followed by any errors that occured. The number at the end of the logfile is the numer of nanoseconds that worker spent processing. A successful run with no errors will still result in a time being output. After all workers have finished, the distsync manager outputs some debugging information regarding the time spent in the run, and produces an image at "./tmp/logs/schedule.bmp". This diagram details the job scheduling from that run. 

