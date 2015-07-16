#!/bin/bash

jobDir=tmp/jobs/
freshDir=expirement/fresh/
staleDir=expirement/stale/

#A - cFile - remove from stale
#C - cDir - remove from stale
#R - rmDir - add to stale
#D - rmFile - add to stale
#M - modFile - do nothing? my test here can't handle that
#L - linkFile - do nothing? my test here can't handle that

for t in A C R D
do
    for f in $(ls $jobDir | grep $t)
    do
	while read line
	do
	    if [ $t == A ];	then
		rm "$staleDir$line"
	    elif [ $t == C ]; then
		rm -rf "$staleDir$line"
	    elif [ $t == R ]; then
		mkdir "$staleDir$line"
	    elif [ $t == D ]; then
		touch "$staleDir$line"
	    fi
	done < "$jobDir$f"
    done
done
