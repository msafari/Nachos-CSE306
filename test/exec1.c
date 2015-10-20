/* Basic test of Exec() system call */

#include "syscall.h"

int
main()
{
  Exec("test/cs1");
  Exit(0);
}
