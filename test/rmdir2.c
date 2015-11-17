/* Tests Create() system call */

#include "syscall.h"

int main()
{
  Mkdir("test/dummy");
  Create("test/dummy/create1");
  Create("create0");
  Mkdir("test/dummy2");
  Create("test/dummy2/create2");
  Rmdir("test/dummy2");
}

