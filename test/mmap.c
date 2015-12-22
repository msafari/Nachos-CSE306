/* Basic test of Mmap() and Munmap() system calls */

#include "syscall.h"

int main() {
  int sizep = -1;

  //Map the file into memory
  char* addr;
  addr = Mmap("test/foobar", &sizep);

  //Free that memory
  Munmap(addr);

}
