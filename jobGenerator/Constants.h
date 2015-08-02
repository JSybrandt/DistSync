#pragma once

namespace Constants{
	
	const char DELIM = '|';

	//file attributes
	const int FILE_SET = 0;
	const int INODE=1;
	const int MISC_ATTR=2;
	const int SIZE=3;
	const int NLINK=4;
	const int USER=5;
	const int GROUP=6;
	const int MODE=7;
	const int ACCESS_T=8;
	const int MOD_T=9;
	const int BLOCKSIZE=10;
	const int CHANGE_T=11;
	const int PATH=12;
	const int ARR_LENGTH=13;
	const int NUM_REQUIRED_DELIMS = ARR_LENGTH-1;

	//job size
	const unsigned int MAX_ENTRIES_IN_JOB_FILE = 10000;
	const double MAX_JOB_FILE_SIZE = 1E12; //a tb

}