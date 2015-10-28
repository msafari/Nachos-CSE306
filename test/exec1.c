/* Basic test of Exec() system call */

#include "syscall.h"

int
main()
{
  Exec("test/console1");
  Exec("test/console1");
  Exit(0);
}
