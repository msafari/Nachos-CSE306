# use normal make for this Makefile
#
# Makefile for building user programs to run on top of Nachos
#
# Several things to be aware of:
#
#    Nachos assumes that the location of the program startup routine (the
# 	location the kernel jumps to when the program initially starts up)
#       is at location 0.  This means: start.o must be the first .o passed 
# 	to ld, in order for the routine "Start" to be loaded at location 0
#

# Change the following to point to where you have the MIPS cross-compiler
# installed.
#Specify absolute path or it will complain
GCCDIR = /home/zerusski/Downloads/CSE306/mips-gcc/

# Change the following to point to where you have Java executables installed.aesome
JAVADIR = /usr/lib/jvm/java-7-oracle/bin/

# Uncomment the following when running on Windows.
#JAVA= $(JAVADIR)java -classpath '..\machine.jar'

# Uncomment the following when running on Unix.
JAVA= $(JAVADIR)java -classpath '../machine.jar'

CC = $(GCCDIR)gcc
AS = $(GCCDIR)as
LD = $(GCCDIR)ld
CPP = $(GCCDIR)cpp

COFF2NOFF = $(JAVA) nachos.noff.Coff2Noff

LDFLAGS = -T script -N
ASFLAGS = -mips1
CPPFLAGS = -I$(INCDIR) -B$(GCCDIR)

CFLAGS = -v -G 0 -I$(INCDIR) -B$(GCCDIR) -c -fno-builtin
SFLAGS = -G 0 -I$(INCDIR) -B$(GCCDIR) -c -S -fno-builtin

NACHOSDIR=..
INCDIR = $(NACHOSDIR)/test

.SUFFIXES:

all:

start.o: start.s $(INCDIR)/syscall.h
	$(CPP) $(CPPFLAGS) start.s > strt.s
	$(AS) $(ASFLAGS) -o start.o strt.s
	rm strt.s

halt.s: halt.c
	$(CC) $(SFLAGS) halt.c
halt.o: halt.c
	$(CC) $(CFLAGS) halt.c
halt: halt.o start.o
	$(LD) $(LDFLAGS) start.o halt.o -o halt.coff
	rm halt.o
	$(COFF2NOFF) halt.coff halt
	rm halt.coff

# If you are using gmake, the following rules should enable you to
# compile any of the test programs.  Otherwise, you have to copy the
# above code for "halt", making appropriate changes, to compile other
# test programs.

%.s: %.c
	$(CC) $(SFLAGS) $< 
%.o: %.c
	$(CC) $(CFLAGS) $<
%: %.o start.o
	$(LD) $(LDFLAGS) start.o $< -o $*.coff
	rm $*.o
	$(COFF2NOFF) $*.coff $@
	rm $*.coff

#Delete only the executables that we made
.PHONY: clean

clean:
	-find . -type f -not \( -name '*c' -or -name '*h' -or -name '*s' -or -name 'README' -or -name 'Makefile' \
	-or -name 'script' -or -name 'script_paged' -or -name 'script_segmented' \) -delete