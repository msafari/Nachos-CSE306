/* Basic test of console reads and writes */

#include "syscall.h"
#define INPUTSIZE 5

int main()
{
  char buffer[INPUTSIZE];
  int num;
  Write("123456789123456789\n", 19, ConsoleOutput);
  // num = Read(buffer, INPUTSIZE, ConsoleInput);
  // Write("You said: \n", 11, ConsoleOutput);
  // Write(buffer, num , ConsoleOutput);
}

