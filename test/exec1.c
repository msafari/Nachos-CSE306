/* Basic test of Exec() system call */

#include "syscall.h"

int
main()
{
  Exec("haltwhoami");
  Exit(0);
}
