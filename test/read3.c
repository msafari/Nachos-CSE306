#include "syscall.h"

int main()
{
  OpenFileId fd;
  char buf[8];
  int num;

  fd = Open("write-test");
  num = Read(buf, 8, fd);
  Close(fd);
}
