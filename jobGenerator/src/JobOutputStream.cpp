#include "JobOutputStream.h"

JobOutputStream::JobOutputStream(string fileRoot, string path)
{
	this->fileRoot = fileRoot;
	this->path = path;
	safeOpenOutputStream(this->out,path+fileRoot);
	numTotalFiles = 0;
}

JobOutputStream::~JobOutputStream(){
	out.close();
	cout<<fileRoot<<":"<<numTotalFiles<<endl;
}

void JobOutputStream::writeRecord(Record const & rec){
	if(rec.isValid()){
		out<<rec[PATH]<<endl;
		numTotalFiles++;
	}
	else
		throw OutputInvalidRecordException();
}

void JobOutputStream::safeOpenOutputStream(fstream & fout, string path){
	fout.open(path.c_str(),ios::out);
	if(!fout.good())
	{
		throw OutputFailedToOpenException();
	}
}