#include "Record.h"

Record::Record(){valid=false;}

void Record::init(string data){

	valid = false;
	
	//check if given data
	if(data==""){
		return;
	}

	//check if data is properly formatted
	int numDelims = count(data.begin(),data.end(),DELIM);
	if(numDelims!=NUM_REQUIRED_DELIMS){
		cerr<<"Found Malformed Record:"<<data<<endl;
		throw InvalidInputException();
	}

	//populate vals
	fillAll(data);
	type = getType(vals[MISC_ATTR]);

	//check type
	if(type==None){
		cerr<<"Found Malformed MISC:"<<vals[MISC_ATTR]<<endl
			<<"\tPath:"<<vals[PATH]<<endl;
		throw InvalidInputException();
	}

	//if we make it here, its good
	valid = true;
	
}

bool Record::isValid() const {return valid;}
void Record::invalidate(){valid=false;}

Record::RecordType Record::getType() const{return type;}

string Record::operator[](int i)const {return vals[i];}

bool Record::operator==(Record const & other) const {
	if(!isValid() || !other.isValid())return false;
	return this->vals[PATH]==other[PATH] && this->type==other.type;
}

bool Record::operator<(Record const & other) const {
	return this->vals[PATH] < other[PATH];
}

bool Record::isModified(Record const & other) const {
	if((*this)==other){

		//dirs can differ by permissions
		if(type == Directory){
			return vals[USER]!=other[USER] 
					|| vals[MODE]!=other[MODE] 
					|| vals[GROUP]!=other[GROUP];
		}
		else{//non-directory
			for(int i = 0 ; i < ARR_LENGTH-1; i++)
				if(i!=BLOCKSIZE 
				   && i!=ACCESS_T 
				   && i!=INODE 
				   && i!=PATH 
				   && i!=FILE_SET 
				   && i!=CHANGE_T 
				   && vals[i]!=other[i])
					return true;
				return false;
		}
	}
	else {
		throw IsModRanOnNERecException();
		return false;
	}
}

void Record::fillAll(string data){
	stringstream ss(data);
    string temp;
    for(int i = 0 ; i < ARR_LENGTH;i++)
    {
		getline(ss,temp,DELIM);
        vals[i]=temp;
    }
}

Record::RecordType Record::getType(string attr){
	for(int i = 0; i<attr.length();i++){
		switch(attr[i]){
			case 'F': return File;
			case 'D': return Directory;
			case 'L': return Link;
			case 'O': return Other;
			default: break;
		}
	}
	return None;
}