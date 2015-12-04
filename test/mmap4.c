/* Basic test of Mmap() and Munmap()system call */

#include "syscall.h"

int main(){
  int i;
  int sizep=  -1;
  char *cp;

  //Map a lot of files
  for(i = 0; i < 1020; i++) {
    cp= Mmap("test/foobar", &sizep);
    cp[i] = 'a';
  }

}
