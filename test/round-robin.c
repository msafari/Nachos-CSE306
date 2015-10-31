
#include "syscall.h"

int main(){
  int i,j;

  for(i=0;i<3;i++) {
    for(j=0; j < 200; j++);
    Sleep(250);
 }
}
