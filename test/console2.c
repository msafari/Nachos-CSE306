/* Basic test of console reads and writes */

#include "syscall.h"
#define INPUTSIZE 5

int main()
{
  Exec("test/console1");
  Write("aabbaabbaabbaabbaabbaabb\n", 25, ConsoleOutput);
  // num = Read(buffer, INPUTSIZE, ConsoleInput);
  // Write("You said: \n", 11, ConsoleOutput);
  // Write(buffer, num , ConsoleOutput);
}

