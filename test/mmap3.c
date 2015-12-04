/* Basic test of Mmap() system call */

#include "syscall.h"

int main(){
  int i;
  int sizep=  -1, sizep2 = -1, sizep3 = -1;
  char *cp, *cp2, *cp3;
  cp = Mmap("test/foobar", &sizep);
  for(i = 0; i < 10; i++)
    cp[i]= 'a';

  cp2 = Mmap("test/foobar2", &sizep2);
  for(i = 0; i < 10; i++)
    cp2[i]= 'b';

  cp3 = Mmap("test/foobar3", &sizep3);
  for(i = 0; i < 10; i++)
    cp3[i]= 'c';


  Munmap(cp2);

  Munmap(cp);

  Munmap(cp3);
}
