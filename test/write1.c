#include "syscall.h"



int main()
{
  OpenFileId fd;
  char *buf = "A buffer.";
  Create("write-test");
  fd = Open("write-test");
  Write(buf, 5, fd);
  Write(buf, 9, fd);
  Close(fd);
}

