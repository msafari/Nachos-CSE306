#include "syscall.h"

/**
	Test of mmap and munmap syscalls
*/
int main()
{
	char* test = "hello";
	int i;
	i = 0;
	Mmap(test, i);
	Munmap(i);

}
