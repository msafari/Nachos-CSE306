#include "syscall.h"

/*
	Test to write passed a sector. FS should automatically add another sector.
*/
int main()
{
  OpenFileId fd;
  char *buf = "NPBBBZNUKFHJBJSPSAMWPELZGZSVZZMKGDTTQWGQJERULMGLVTCWCKVDJHALWXKZEDZNHRTBRNEYKCTULXQVSZXHAKQSWBHVTAWBEUDXUTFXGXLXFXUJTHJJFPCEWPPHHH";
  Create("write-test");
  fd = Open("write-test");
  int i;
  //Write to however many sectors you want
  for(i = 0; i < 1; i++){
  	Write(buf, 130, fd);
  }
  Close(fd);
}

