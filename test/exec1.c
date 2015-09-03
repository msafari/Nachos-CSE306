/* Basic test of Exec() system call */

#include "syscall.h"

int
main()
{
  Exec("halt2");
  Exit(0);
}
