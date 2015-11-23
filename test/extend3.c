#include "syscall.h"
/**
  This test extends both a directory passed its initial 10 entry limit as well as a single file to fill all of its direct, indirect and doubly indirect blocks.
*/
int main()
{
  OpenFileId fd;
  int i;

  Mkdir("dummy");
  Create("dummy/create0");
  Create("dummy/create1");
  Create("dummy/create2");
  Create("dummy/create3");
  Create("dummy/create4");
  Create("dummy/create5");
  Create("dummy/create6");
  Create("dummy/create7");
  Create("dummy/create8");
  Create("dummy/create9");
  Create("dummy/create10");
  Create("dummy/create11");
  Create("dummy/create12");
  Create("dummy/create13");
  Create("dummy/create14");
  Create("dummy/create15");
  Create("dummy/create16");
  Create("dummy/create17");
  fd = Open("dummy/create10");

  //Write in 128 byte chunks
  for(i=0;i<926;i++) {
    Write("TQHJEOLHDETWACEZVJODKBSUZIOLAVRQPGCEJLNXYQNILHEPMPFPDVBRNQWKYMSWXWVOIXNYAHOEPWDZKWKWDRAOTUTEFUEQRPWLCBTXFRZVVGKXFQWGTXBJAUTSRZKL", 128, fd);
  }
}
