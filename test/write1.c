#include "syscall.h"



int main()
{
  OpenFileId fd;
  char *buf = "A buffer.";

  fd = Open("create-test");
  Write(buf, 5, fd);
  Write(buf, 9, fd);
  Close(fd);
}

