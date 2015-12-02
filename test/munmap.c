/* Basic test of Munmap() system call */

#include "syscall.h"

int main() {
  int addr;
  int sizep = -1;

  //Map the file into memory
  addr = Mmap("test/halt", &sizep);

  //Free that memory
  Munmap(addr);

}
