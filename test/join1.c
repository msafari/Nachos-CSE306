

#include "syscall.h"

int main()
{
  SpaceId waitpid;
  int result;

  waitpid =  Exec("timeshare2");
  result = Join(waitpid);
  waitpid = Exec("timeshare3");
  result = Join(waitpid);
  waitpid = Exec("timeshare4");
  result = Join(waitpid);
  Exit(result);
}
