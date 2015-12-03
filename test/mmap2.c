/* Basic test of Mmap() system call */

#include "syscall.h"

int main(){
	int i;
	int sizep=  -1;
	char* cp;
	cp = Mmap("test/foobar", &sizep);
	for(i = 0; i < 10; i++)
		cp[i]= 'a';
	Munmap(cp);
}
