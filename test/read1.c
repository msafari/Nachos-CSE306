#include "syscall.h"

int main()
{
  OpenFileId fd;
  char buf[25];
  int num;

  fd = Open("test/create1");
  num = Read(buf, 25, fd);
  Close(fd);
}
