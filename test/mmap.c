/* Basic test of Mmap() system call */

#include "syscall.h"

int main(){

  int sizep = -1;
  char* addr;
  addr = Mmap("test/halt", &sizep);
  Munmap(addr);
}
