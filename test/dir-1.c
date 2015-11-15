#include "syscall.h"

/*
  Test to write passed a sector. FS should automatically add another sector.
*/
int main()
{
  // int i;
  // char[] index;
  // //Write to however many sectors you want
  // for(i = 0; i < 12; i++){
  //   index = i + '0';
  //   Create("test" + i);
  // }
  
  Create("test1");
  Create("test2");
  Create("test3");
  Create("test4");
  Create("test5");
  Create("test6");
  Create("test7");
  Create("test8");
  Create("test9");
  Create("test10");
  Create("test11");

}


