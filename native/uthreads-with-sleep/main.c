#include <stdio.h>
#include <stdbool.h>
#include "uthread.h"

#include "log.h"

void sleep_loop(uint64_t delay)
{
  int acc = 0;
  while (true)
  {
    printf("tick %ld - %d\n", delay, acc);
    ut_sleep(delay);
    acc += 1;
  }
}

int main()
{
  printf("starting\n");
  ut_init();
  printf("inited\n");
  for (int i = 1; i < 4; ++i)
  {
    ut_create(sleep_loop, i);
  }
  ut_run();
  printf("ending\n");
  return 0;
}
