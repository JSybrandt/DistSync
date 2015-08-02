#pragma once

#include "Constants.h"
#include <iostream>
#include <sstream>
#include <string>
#include <cstring>
#include <algorithm>
#include <exception>

using std::cin;
using std::cout;
using std::cerr;
using std::endl;
using std::string;
using std::stringstream;
using std::count;
using std::exception;

using namespace Constants;

class IsModRanOnNERecException : public exception{};
class InvalidInputException: public exception{};

class Record{

public:
	enum RecordType{
		Directory, //D
		File,	   //F
		Link,	   //L
		Other,	   //O
		None
	};
	
	Record();
	void init(string data);
	void invalidate();
	bool isValid() const;
	RecordType getType() const;
	bool isModified(Record const & other) const;
	string operator[](int i) const;
	bool operator==(Record const & rec) const;
	bool operator<(Record const & rec) const;

private:

	//private functions
	void fillAll(string data);
	RecordType getType(string attr);
	
	//private data
	bool valid;
	string vals[ARR_LENGTH];
	RecordType type;

};