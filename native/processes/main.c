#include <stdio.h>
#include <stdbool.h>

int global_var = 0;

int some_function()
{
  int local_var = 2;

  local_var += 2;
}

int main()
{
  int local_var = 0;
  printf("global_var is located at %p\n", (void *)&global_var);
  while (true)
  {
    global_var += 1;
    printf("global_var = %d\n", global_var);
    getchar();
  }
  return 0;
}