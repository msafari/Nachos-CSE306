/* This program creates a copy of itself, then both of them die. */
#include "syscall.h"

void SimpleThread() {
    int i;
    
    for (i = 0; i < 5; i++) {
      Write("*** thread is looping\n", 20, ConsoleOutput);
    }
    Exit(0);
}

void AnotherThread() {
    int i;
    
    for (i = 0; i < 5; i++) {
      Write("*** In the second function\n", 30, ConsoleOutput);
    }
    Exit(0);
}

int main (void) {

  Write ("Here I am in the program!\n", 30, ConsoleOutput);
  Fork(SimpleThread);
  Fork(AnotherThread);
  Exec("test/join1");
  return 0;
}

