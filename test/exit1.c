/* Basic test of Exit() system call */

#include "syscall.h"

int
main()
{
  Exec("test/cs2");
  Exit(0);
}
