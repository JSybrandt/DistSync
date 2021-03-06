#pragma once

#include <fstream>
#include <iostream>
#include <string>
#include <exception>

#include "Record.h"
#include "Constants.h"

using std::fstream;
using std::string;
using std::endl;
using std::ios;
using std::exception;

class OutputFailedToOpenException: public exception{};
class OutputInvalidRecordException: public exception{};

//this class does not split output.
class JobOutputStream{
public:
	//path must end in '/'
	JobOutputStream(string fileRoot, string path);
	~JobOutputStream();
	void writeRecord(Record const & rec);
	
protected:

	static void safeOpenOutputStream(fstream & fout, string path);
	
	string fileRoot;
	string path;
	fstream out;
	unsigned int numTotalFiles;
};