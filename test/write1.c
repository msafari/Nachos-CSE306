#include "syscall.h"

int main()
{
  OpenFileId fd;
  char *buf = "A buffer.";
  char *buf2 = "heyitsme";
  Create("write-test");
  fd = Open("write-test");
  Write(buf, 5, fd);
  Write(buf2, 3, fd);
  Close(fd);
}

