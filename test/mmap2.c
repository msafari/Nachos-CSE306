/* Basic test of Mmap() system call */

#include "syscall.h"

int main(){
	int i;
	int sizep = -1;
	char* cp;
	cp = Mmap('foobar', &sizep);
	for(i = 0; i < sizep; i++)
		cp[i] = 5;
	Munmap(cp);
}
