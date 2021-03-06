#include "SplitJobOutputStream.h"

SplitJobOutputStream::SplitJobOutputStream(string fileRoot, string path)
	:JobOutputStream(fileRoot,path)
{
	jobCount=1;
	numberOfFiles=0;
	totalSize=0;
}

void SplitJobOutputStream::writeRecord(Record const & rec){
	 JobOutputStream::writeRecord(rec);
	numberOfFiles++;
	totalSize += atof(rec[SIZE].c_str());

	if(numberOfFiles >= Constants::MAX_ENTRIES_IN_JOB_FILE
	   || totalSize >= Constants::MAX_JOB_FILE_SIZE)
	{
		openNewFile();
	}
}

void SplitJobOutputStream::openNewFile(){
	out.close();
	numberOfFiles=0;
	totalSize=0;
	
	jobCount++;
	stringstream fileName;
	fileName<<path<<fileRoot<<jobCount;
	safeOpenOutputStream(out,fileName.str());
}