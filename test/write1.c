#include "syscall.h"



int main()
{
  OpenFileId fd;
  char *buf = "A buffer.";
  Create("test/write-test");
  fd = Open("test/write-test");
  Write(buf, 5, fd);
  Write(buf, 9, fd);
  Close(fd);
}

