#include "syscall.h"
int main()
{
   char buf[2] = {'a', 'a'};

   Write(&buf[0],2,ConsoleOutput);
}
