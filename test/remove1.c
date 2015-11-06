/* Tests Remove() system call */

#include "syscall.h"

int main()
{
  Create("test/remove-test");
  Remove("test/remove-test");
}
