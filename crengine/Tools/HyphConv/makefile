################################################################################
# Automatically-generated file. Do not edit!
################################################################################

ROOT := .
CC = g++

	inc =	\
			-I/usr/include \
			-I../../include

	lib =	\
			-L/usr/lib

# type of build we're doing
ifndef BUILD
	BUILD = Debug
endif

#-include $(ROOT)/makefile.init

RM := rm -rf

build = ${BUILD}
builddir = ${build}
targetfile = mkpattern

ccflags = -fPIC -w -Wall -fno-inline -fexceptions -fpermissive
defs = -DLINUX -D_LINUX -D_REENTRANT -D_FILE64_MAPPING

# libraries
libs =		-static-libgcc \
			-L../../lib \
			-L/usr/lib \
			-lstdc++ \
			-lz \
			$(lib)

#			-lpng \
#			-ljpeg \
#			-lz \

#include paths
incpath =	 \
			-I. \
			-I../../include \
			$(inc)


ifeq ($(BUILD),Debug)
	#debug builds..
	flags = -g $(ccflags) $(defs)
	defs += -D_DEBUG 
else
	#release builds
	flags = -s -Os $(ccflags) $(defs)
endif


# all the objects required
srcs	 =	\
		mkpattern \
		../../src/crtxtenc \
		../../src/lvstring \
		../../src/lvmemman \
		../../src/cp_stats \
		../../src/lvstream \
		$(deps)

# executable target
$(targetfile) : $(addsuffix .o,$(srcs))
	@echo Linking $(targetfile) [$(BUILD)]
	$(CC) -g -lc \
		-o $(builddir)/$(targetfile) $(addprefix $(builddir)/,$(^F)) \
		$(libs) 

# compile command
%.o : %.cpp
	@echo "$(<F) [$(gui)]"
	@ls $(builddir) > /dev/null 2> /dev/null || mkdir $(builddir)
	$(CC) $(incpath) $(flags) -c $< -o $(builddir)/$(@F)

# dependency generation
%.d : %.cpp
	@ls .deps > /dev/null 2> /dev/null || mkdir .deps
	$(SHELL) -ec '$(CC) -M $(incpath) $(flags) $< \
		| sed '\''s/\($*\)\.o[ :]*/\1.o $@ : /g'\'' > .deps/$@; \
		[ -s $@ ] || rm -f $@'

depends : $(addsuffix .d,$(srcs))
	@echo "Dependencies done..."

# include all the dependency files...
-include $(addprefix .deps/,$(addsuffix .d,$(srcs)))

# Other Targets

#clean the directory out
clean :
	echo Cleaning $(builddir)
	rm -f $(builddir)/*
	rm -rf .deps

# All Target
all:
	make BUILD=Debug
	make BUILD=Release
