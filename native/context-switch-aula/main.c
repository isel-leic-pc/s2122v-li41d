#include <stdlib.h>
#include <stdint.h>
#include <stdio.h>

#define STACK_LEN (8 * 1024)
typedef struct uthread
{
  uint64_t rsp;
  uint8_t stack[STACK_LEN];
} uthread_t;

typedef struct stack_layout
{
  uint64_t r15;
  uint64_t r14;
  uint64_t r13;
  uint64_t r12;
  uint64_t rbx;
  uint64_t rbp;
  void (*fptr)();
} stack_layout_t;

void context_switch(uthread_t *self, uthread_t *next);

void context_prepare(uthread_t *self, void (*start_routine)())
{
  stack_layout_t *pctx = (stack_layout_t *)(self->stack + STACK_LEN - sizeof(stack_layout_t));
  pctx->fptr = start_routine;
  self->rsp = (uint64_t)pctx;
}

uthread_t th1;
uthread_t th2;
uthread_t thmain;

void routine1()
{
  for (int i = 0; i < 3; ++i)
  {
    printf("routine 1, iteration %d, before\n", i);
    if (i % 2 == 1)
    {
      printf("routine 1, iteration %d, inside if \n", i);
      context_switch(&th1, &th2);
      printf("routine 1, iteration %d, after\n", i);
    }
  }
  context_switch(&th1, &th2);
  context_switch(&th1, &thmain);
}
void routine2()
{
  for (int i = 0; i < 3; ++i)
  {
    printf("routine 2, iteration %d, before\n", i);
    context_switch(&th2, &th1);
    printf("routine 2, iteration %d, after\n", i);
  }
  context_switch(&th2, &thmain);
}

int main()
{
  context_prepare(&th1, routine1);
  context_prepare(&th2, routine2);
  printf("main started\n");
  context_switch(&thmain, &th1);
  printf("main ended\n");
  return 0;
}
