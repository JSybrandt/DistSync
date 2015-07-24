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

string rootDir;//used to house all of the job files


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

const int ARR_LENGTH=13;

const int NUM_REQUIRED_DELIMS = ARR_LENGTH-1;


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
      cerr<<"Found malformed MISC"<<endl;
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

void run(string c)
{
  if(system(c.c_str())!=0)
    system(("echo \"" + c + "\" >> logFile.txt").c_str());
}

int main(int argc, char** argv)
{
  if(argc != 2){
    cerr<<"NEED TO SUPPLY OUTPUT ROOT DIR"<<endl;
    return 1;
  }
  
  cout<<"Building file structure in "<<argv[1]<<endl;
  rootDir = argv[1];

  run("mkdir -p " + rootDir);

  string temp;

  Record rec;

  while(getline(cin,temp))
    {
      rec.init(temp);
      if(rec.type=='D'){
	run("mkdir \""+ rootDir + rec[PATH]+"\"");
      }
      else if(rec.type == 'F' || rec.type == 'O'){
	run("touch \"" + rootDir + rec[PATH]+"\"");
      }
      else if(rec.type == 'L'){
	run("ln -s ./link \"" + rootDir + rec[PATH]+"\"");
      }
    }
  run("echo "" | mail -s \"Job Done\" jsybrandt@lbl.gov");
  return 0;
}
