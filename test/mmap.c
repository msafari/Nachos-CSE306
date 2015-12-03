/* Basic test of Mmap() system call */

#include "syscall.h"

int main(){

  int sizep = -1;
  int addr = -1;
  addr = Mmap("test/halt", &sizep);
  Munmap(addr);
}
