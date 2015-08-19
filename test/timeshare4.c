
#include "syscall.h"

int main()
{
  int i;

  for(i=0;i<10;i++) {
    Write("Timesharing 4\n",14,ConsoleOutput);
  }
  
  Exit(0);
}
