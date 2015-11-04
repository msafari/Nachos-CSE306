/* Basic test of console reads*/

#include "syscall.h"
#define INPUTSIZE 5

int main()
{
  char buffer[INPUTSIZE];
  int num;
  num = Read(buffer, INPUTSIZE, ConsoleInput);
  // Write("You said: \n", 11, ConsoleOutput);
  // Write(buffer, num , ConsoleOutput);
}

