## Process this file with automake to produce Makefile.in

## Created by Anjuta

AM_CPPFLAGS = \
	-DPACKAGE_LOCALE_DIR=\""$(localedir)"\" \
	-DPACKAGE_SRC_DIR=\""$(srcdir)"\" \
	-DPACKAGE_DATA_DIR=\""$(pkgdatadir)"\"

AM_CFLAGS =\
	 -Wall\
	 -g

bin_PROGRAMS = jobgenerator

jobgenerator_SOURCES = \
	main.cc \
	Record.cpp \
	SplitJobOutputStream.cpp \
	JobOutputStream.cpp

jobgenerator_LDFLAGS = 

jobgenerator_LDADD = 



include_HEADERS = Record.h \
	JobOutputStream.h \
	SplitJobOutputStream.h \
	Constants.h
