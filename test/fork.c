/* This program creates a copy of itself, then both of them die. */
#include "syscall.h"

void SimpleThread() {
    int i;
    
    for (i = 0; i < 5; i++) {
      Write("*** thread is looping\n", 20, ConsoleOutput);
    }
}

int main (void) {

  Write ("Here I am in the program!\n", 20, ConsoleOutput);
  //oour fork doesn;t work like this we need to pass a pointer to a function
  Fork(SimpleThread);
  return 0;
}

