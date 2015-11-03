/* Basic test of console reads and writes */

#include "syscall.h"
#define INPUTSIZE 5

int main()
{
  char buffer[INPUTSIZE];
  int num;
  Write("Mad3", 3, ConsoleOutput);
  // num = Read(buffer, INPUTSIZE, ConsoleInput);
  // Write("You said: \n", 11, ConsoleOutput);
  // Write(buffer, num , ConsoleOutput);
}

