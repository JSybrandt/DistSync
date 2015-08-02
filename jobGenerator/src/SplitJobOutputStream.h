#pragma once
#include "JobOutputStream.h"
#include <sstream>
using std::stringstream;

class SplitJobOutputStream: public JobOutputStream{
public:
	SplitJobOutputStream(string fileRoot, string path);
	void writeRecord(Record const & rec); //overloaded
private:
	void openNewFile();
	int jobCount,numberOfFiles;
	double totalSize;
};