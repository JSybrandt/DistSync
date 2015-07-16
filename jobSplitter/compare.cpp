//non-merged compare
//requires two sorted file lists
#include <iostream>
#include <fstream>
#include <cstdlib>
#include <sstream>
#include <string>
#include <cstring>
#include <algorithm>
using namespace std;

//int to string
#define SSTR( x ) (dynamic_cast< std::ostringstream & >( std::ostringstream() << std::dec << x )).str()

string folder;//used to house all of the job files


const char DELIM = '|';

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
//const int VERSION=13;
const int ARR_LENGTH=13;
const int NUM_REQUIRED_DELIMS = ARR_LENGTH-1;
const char flagSymbols[] = {'f','i','?','s','l','u','g','p','a','m','b','c','p'};

const unsigned int NUM_ENTRIES_IN_JOB_FILE = 10000;
const double MAX_JOB_FILE_SIZE = 1E12; //a tb



struct Record
{

  bool isValid;
  string vals[ARR_LENGTH];
  string flags;
  string raw;
  char type; //will be D,F,L,or O
  Record()
  {
    isValid = false;
  }

  void init(string data)
  {
    if(data=="")
    {
      isValid = false;
      return;
    }
    if(count(data.begin(),data.end(),DELIM)!=NUM_REQUIRED_DELIMS)
    {
      isValid=false;
      cerr<<"Found Malformed Record."<<endl;
      return;
    }
    raw = data;
    flags="";
    isValid=true;
    fillAll();

    type = '_';
    for(int i = 0 ; i < vals[MISC_ATTR].length();i++){
      char c = vals[MISC_ATTR][i];
      if(c=='F'||c=='D'||c=='L'||c=='O'){
		type=c;
		break;
      }
    }

	if(type=='O')
	{
		cerr<<"Found socket/device."<<endl;
	}

    if(type=='_'){
      cerr<<"Found malformed MISC:"<<vals[MISC_ATTR]<<endl;
      isValid=false;
    }

  }
  void fillAll(){
    stringstream ss(raw);
    string temp;
    for(int i = 0 ; i < ARR_LENGTH;i++)
      {
        getline(ss,temp,DELIM);
        vals[i]=temp;
      }
    flags="";
  }

  void clear()
  {
    isValid=false;
  }

  string operator[](int i) const
  {
    return vals[i];
  }

  bool operator==(Record const & rec) const
  {
    return vals[PATH]==rec[PATH];
  }
  bool operator<(Record const & rec) const
  {
    return vals[PATH]<rec[PATH];
  }
};

ostream& operator<<(ostream& out, const Record& rec)
{
  out<<rec[PATH];
  return out;
}

bool isRecModified(Record &rec1, Record &rec2)
{

    for(int i = 0 ; i < ARR_LENGTH-1; i++)
      if(i!=BLOCKSIZE && i!=ACCESS_T && i!=INODE && i!=PATH && i!=CHANGE_T && rec1[i]!=rec2[i])//don't report access time change
	return true;
    return false;
}


struct Job{
    double totalSize;
    unsigned int fileCount;
    Job(){totalSize = 0 ; fileCount=0;}
    void reset(){totalSize=0;fileCount=0;}
    void addFile(Record &r){totalSize += atof(r[SIZE].c_str()); fileCount++;}
};

Job modJob,createJob,removeJob; //used to count job files

bool safeOpenFStream(fstream & fs,ios::openmode mode,  int i, int argc, char** argv)
{
  if(i>=argc || argv[i][0]=='-')
  {
    cerr<<"No filename supplied."<<endl;
    return false;
  }
  fs.open(argv[i], mode);
  if(!fs.good())
  {
    cerr<<"Bad filename supplied."<<endl;
    fs.close();
    return false;
  }
  return true;
}

bool parseArgs(int argc, char** argv, fstream &n, fstream &o, fstream &c, fstream &d, fstream &m,fstream &cd,fstream &dd, fstream &lc, fstream &md)
{
  bool foundNew=false, foundOld=false, foundFolder=false;
  folder = "res/";

  for(int i = 1 ; i < argc; i++){
    string op(argv[i]);
    if(op=="-n" && !foundNew){
      ++i;
      if(safeOpenFStream(n,ios::in,i,argc,argv)) foundNew = true;
      else return false;
    }
    else if (op=="-o" && !foundOld){
      ++i;
      if(safeOpenFStream(o,ios::in,i,argc,argv)) foundOld = true;
      else return false;
    }
    else if (op=="-f"&&!foundFolder){
      ++i;
      if(i<argc && argv[i][0]!='-')
      {
		folder = argv[i];
		if(folder[folder.length()-1]!='/')
		  folder+='/';
		foundFolder = true;
      }
      else{
		cerr<<"No foldername specified.";
		return false;
      }
    }
    else{
      cerr<<"Invalid Option: "<<argv[i]<<endl;
      return false;
    }
  }

  if(foundNew && foundOld){
    system(("mkdir -p " + folder).c_str());
    c.open((folder + "A").c_str(),ios::out);
    d.open((folder + "D").c_str(),ios::out);
    m.open((folder + "M").c_str(),ios::out);
    cd.open((folder + "C").c_str(),ios::out);
    dd.open((folder + "R").c_str(),ios::out);
    lc.open((folder + "L").c_str(),ios::out);
    md.open((folder + "Y").c_str(),ios::out);
  }
  else{
    cerr<<"Must supply new and old files."<<endl;
    return false;
  }
}



void evalJobSplit(Job & job, Record & r,  fstream & stream, string type)
{
    job.addFile(r);
  static unsigned int jobFileCount = 1;
  if(job.fileCount >= NUM_ENTRIES_IN_JOB_FILE || job.totalSize > MAX_JOB_FILE_SIZE)
  {
    stream.close();
    jobFileCount++;
    string newFile = folder+type + SSTR(jobFileCount);
    stream.open((newFile).c_str(),ios::out);
    cout<<job.fileCount<<","<<job.totalSize<<endl;
    job.reset();
  }
}

//used for old records
void printRec(Record & rec, fstream &file, fstream &dir){
	if(rec.type=='D'){
	        dir<<rec<<endl;
		//WE DONT SPLIT DIR's
	}
	else{ //counts files, pipes, devices, soft links
	  file<<rec<<endl;
	  evalJobSplit(removeJob,rec,file,"D");
	}
}
//used for new records
void printRec(Record &rec, fstream &file, fstream &dir,fstream &hlink){
        if(rec.type!='D' && rec[NLINK]!="1"){
		hlink<<rec<<endl;
		//WE DONT SPLIT L
	}
	else if(rec.type=='D'){
		dir<<rec<<endl;
		//WE DONT SPIT C
	}
	else{ //counts files, pipes, devices, soft links
	        file<<rec<<endl;
		evalJobSplit(createJob,rec,file,"A");
	}
}


int main(int argc, char** argv)
{
        int jobFileCount=1;

  	fstream inNew, inOld,outFCreated,outFDeleted,outModified,outDCreated,outDDeleted,outLCreated,outDirMod;
  	if(parseArgs(argc, argv,inNew,inOld,outFCreated,outFDeleted,outModified,outDCreated,outDDeleted,outLCreated,outDirMod))
  	{

		string line;
		Record newRec, oldRec;
		unsigned int nRecords=0,nChanged=0,nUnchanged=0,nCreated=0,nDeleted=0;


		while(inNew && inOld){
			if(newRec.isValid && oldRec.isValid){
			  //cout<<"N:"<<newRec[PATH]<<"\nO:"<<oldRec[PATH]<<endl;
				if(oldRec[PATH] == newRec[PATH]){
				  //cout<<"\tEQUAL"<<endl;
				  if(isRecModified(newRec, oldRec))
					{
					  printRec(newRec,outModified,outDirMod);
					  nChanged++;
					}
					else
						nUnchanged++;
    					newRec.isValid=false; oldRec.isValid = false;
				}
				else if(oldRec[PATH] < newRec[PATH]){ //old rec doesn't have a match
				  //cout<<"\tDeleted"<<endl;
				  printRec(oldRec,outFDeleted, outDDeleted);
					nDeleted++;
					oldRec.isValid=false;
				}
				else{ //new rec dosn't have a match
				  //we only care about new links for the link job
				  //cout<<"\tAdded"<<endl;      
				  printRec(newRec,outFCreated,outDCreated,outLCreated);
      					nCreated++;
					newRec.isValid=false;
				}
			}
			if(!newRec.isValid){
				nRecords++;getline(inNew,line);newRec.init(line);
			}
			if(!oldRec.isValid){
				nRecords++;getline(inOld,line);oldRec.init(line);
			}
		}

		while(newRec.isValid){
			nCreated++;
			printRec(newRec,outFCreated,outDCreated,outLCreated);
			//(newRec.type=='D' ? outDCreated : outFCreated)<<newRec<<endl;
			getline(inNew,line);
			newRec.init(line);
			nRecords++;
		}

		while(oldRec.isValid){
			nDeleted++;
			printRec(oldRec,outFDeleted, outDDeleted);
			//(oldRec.type=='D' ? outDDeleted : outFDeleted)<<oldRec<<endl;
			getline(inOld,line);
			oldRec.init(line);
			nRecords++;
		}

		cout<<"#Records:"<<nRecords<<endl
			<<"#Created:"<<nCreated<<endl
			<<"#Deleted:"<<nDeleted<<endl
			<<"#Unchanged:"<<nUnchanged<<endl
			<<"#Changed:"<<nChanged<<endl;


		inNew.close();
		inOld.close();
	 	outFCreated.close();
	 	outFDeleted.close();
	 	outModified.close();
	 	outDCreated.close();
	 	outDDeleted.close();
	 	outLCreated.close();
		outDirMod.close();

  	}

  	return 0;
}
