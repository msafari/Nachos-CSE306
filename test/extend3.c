#include "syscall.h"

int main()
{
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
  Rmdir("dummy");
}