#include <iostream>
#include <fstream>
#include <exception>

#include "Record.h"
#include "Constants.h"
#include "JobOutputStream.h"
#include "SplitJobOutputStream.h"

using namespace std;

class InvalidScanFileException:public exception{
public:
	InvalidScanFileException(string s){message=s;}
	~InvalidScanFileException() throw() {}
	 const char* what() const throw() {return message.c_str();}
private:
		string message;
};

int main(int argc, char** argv)
{
	string freshScanPath="";
	string staleScanPath="";
	string outputDirPath="";

	//get command line args
	for(int i=1; i<argc; i++)
	{
		if(i!=argc-1){
			if(argv[i]=="-n"){
				i++;
				freshScanPath = argv[i];
			}
			if(argv[i]=="-o"){
				i++;
				staleScanPath = argv[i];
			}
			if(argv[i]=="-f"){
				i++;
				outputDirPath = argv[i];
			}
		}
	}

	//error checking
	if(freshScanPath==""||staleScanPath==""||outputDirPath==""){
		if(freshScanPath==""){
			cerr<<"Specify up-to-date scan with -n [file]"<<endl;
		}
		if(staleScanPath==""){
			cerr<<"Specify out-of-date scan with -o [file]"<<endl;
		}
		if(outputDirPath==""){
			cerr<<"Specify output directory with -f [directory]"<<endl;
		}
		return 1;
	}

	//instantiate all objects
	fstream freshScan(freshScanPath.c_str(),ios::in);
	fstream staleScan(staleScanPath.c_str(),ios::in);

	//error checking
	if(!freshScan.good() || !staleScan.good())
	{
		if(!staleScan.good()){
			cerr<<"File path:"<<staleScanPath<<" is inaccessable."<<endl;
		}
		if(!freshScan.good()){
			cerr<<"File path:"<<freshScanPath<<" is inaccessable."<<endl;
		}
		return 2;
	}

	try{
		JobOutputStream createDirs("C",outputDirPath);
		JobOutputStream removeDirs("R",outputDirPath);
		SplitJobOutputStream createFiles("A",outputDirPath);
		SplitJobOutputStream removeFiles("D",outputDirPath);
		SplitJobOutputStream modifyFiles("M",outputDirPath);
		SplitJobOutputStream modifyDirs("Y",outputDirPath);
		JobOutputStream buildLinks("L",outputDirPath);

		Record freshRec, staleRec;
		string tmp;
		
		while(freshScan && staleScan){
			if(freshRec.isValid() && staleRec.isValid()){

				if(freshRec==staleRec){

					if(freshRec.getType() == Record::Link){
						buildLinks.writeRecord(freshRec);
					}
					else if(freshRec.isModified(staleRec)){
						if(freshRec.getType() == Record::Directory)
							modifyDirs.writeRecord(freshRec);
						else
							modifyFiles.writeRecord(freshRec);
					}

					freshRec.invalidate();
					staleRec.invalidate();
					
				}
				else if(freshRec < staleRec){
					//record has been added
					if(freshRec.getType() == Record::Directory)
						createDirs.writeRecord(freshRec);
					else if(freshRec.getType() == Record::Link)
						buildLinks.writeRecord(freshRec);
					else
						createFiles.writeRecord(freshRec);
					freshRec.invalidate();
				}
				else{ //staleRec < freshRec
					//record has been removed
					if(staleRec.getType() == Record::Directory)
						removeDirs.writeRecord(staleRec);
					else
						removeFiles.writeRecord(staleRec);
					staleRec.invalidate();
				}
			}

			//get new records
			if(!freshRec.isValid()){
				getline(freshScan,tmp);
				if(tmp!="" &&freshRec[PATH]>tmp){
					throw InvalidScanFileException(freshScanPath);
				}
				freshRec.init(tmp);
			}
			if(!staleRec.isValid()){
				getline(staleScan,tmp);
				if(tmp!="" &&staleRec[PATH]>tmp){
					throw InvalidScanFileException(staleScanPath);
				}
				staleRec.init(tmp);
			}
		}//while both inputs are valid

		//get any remaining fresh records
		while(freshScan){
			if(freshRec.isValid()){
				//record has been added
				if(freshRec.getType() == Record::Directory)
					createDirs.writeRecord(freshRec);
				else if(freshRec.getType() == Record::Link)
					buildLinks.writeRecord(freshRec);
				else
					createFiles.writeRecord(freshRec);
				freshRec.invalidate();
			}
			
			if(!freshRec.isValid()){
				getline(freshScan,tmp);
				if(tmp!="" && freshRec[PATH]>tmp){
					throw InvalidScanFileException(freshScanPath);
				}
				freshRec.init(tmp);
			}
		}

		//get any remaining stale records
		while(staleScan){
			if(staleRec.isValid()){
				//record has been removed
				if(staleRec.getType() == Record::Directory)
					removeDirs.writeRecord(staleRec);
				else
					removeFiles.writeRecord(staleRec);
				staleRec.invalidate();
			}
			if(!staleRec.isValid()){
				getline(staleScan,tmp);
				if(tmp!="" &&staleRec[PATH]>tmp){
					throw InvalidScanFileException(staleScanPath);
				}
				staleRec.init(tmp);
			}
		}
		

	}catch(OutputFailedToOpenException e){
		cerr<<"Output path:"<<outputDirPath<<" is inaccessable."<<endl;
		return 3;
	}catch(IsModRanOnNERecException e){
		cerr<<"One of the scan files is invalid."<<endl;
		return 4;
	}catch(InvalidScanFileException e){
		cerr<<"Scan file:"<<e.what()<<" is not sorted by path."<<endl;
		return 5;
	}catch(exception e){
		cerr<<e.what()<<endl;
		return 999;
	}
	
	return 0;
}

