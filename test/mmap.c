/* Basic test of Mmap() system call */

#include "syscall.h"

int main(){

  int sizep = -1;
  Mmap("test/halt", &sizep);

}
