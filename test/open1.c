#include "syscall.h"

int main()
{
  OpenFileId fd;
  fd = Open("test/create1");
  Close(fd);
}
