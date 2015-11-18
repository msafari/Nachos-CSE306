/* Tests Create() system call */

#include "syscall.h"

int main()
{
  Mkdir("test/dummy");
  Create("test/dummy/create1");
  Create("test/dummy/create2");
  Create("test/dummy/create3");
  Create("test/dummy/create4");
  Create("test/dummy/create5");
  Create("test/dummy/create6");
  Create("test/dummy/create7");
  Create("test/dummy/create8");
  Create("test/dummy/create9");
  Create("test/dummy/create10");
  Create("test/dummy/create11");
  Create("test/dummy/create12");
  Create("test/dummy/create13");
  Create("test/dummy/create14");
  Create("test/dummy/create15");

 
  //Exit(0);
}

