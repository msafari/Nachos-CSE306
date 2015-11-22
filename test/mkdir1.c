/* Tests Create() system call */

#include "syscall.h"

int main()
{
  Mkdir("test/dummy");
  Create("test/dummy/create1");
  Mkdir("test/dummy2");
  Create("create0");
  Create("test/dummy2/create2");
}

