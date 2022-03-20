#include <stdlib.h>
#include <time.h>

#include "list.h"
#include "uthread.h"
#include "log.h"

#define STACK_SIZE (8 * 1024)

// -- structures

// Represents a thread
struct uthread
{
  // needs to be the first field
  uint64_t rsp;
  start_routine_t start;
  uint64_t arg;
  list_entry_t list_entry;
};

// Represents a thread initial stack content
typedef struct uthread_context
{
  uint64_t r15;
  uint64_t r14;
  uint64_t r13;
  uint64_t r12;
  uint64_t rbx;
  uint64_t rbp;
  void (*func_addr)();
} uthread_context_t;

// Represents a sleep request
typedef struct sleep_request
{
  uint32_t end;
  uthread_t *thread;
  list_entry_t list_entry;
} sleep_request_t;

// globals ...
// ... the currently running thread
uthread_t *thread_running;
// ... the thread where the uthread system is running
uthread_t *thread_main;
// ... the queue with ready threads
list_entry_t queue_ready;
// ... the queue with sleeping threads
list_entry_t queue_sleep;

void context_switch(uthread_t *curr_thread, uthread_t *next_thread);
void context_switch_without_save(uthread_t *curr_thread, uthread_t *next_thread);

// helper function to retrieve the current timestamp
static uint32_t get_time()
{
  struct timespec ts;
  clock_gettime(CLOCK_MONOTONIC, &ts);
  return ts.tv_sec;
}

// moves all completed sleep requests into the ready queue
// takes advantage of the sleep queue being sorted
void complete_all_sleep_requests()
{
  uint32_t now = get_time();
  while (true)
  {
    list_entry_t *first_sleep_request = list_peek_head(&queue_sleep);
    if (first_sleep_request == NULL)
    {
      // no more requests
      break;
    }
    sleep_request_t *sleep_request = node_of(first_sleep_request, sleep_request_t, list_entry);
    if (sleep_request->end > now)
    {
      // no more completed sleep requests
      break;
    }
    // remove from the sleep queue
    list_remove_head(&queue_sleep);
    // add into the ready queue (i.e. the thread transitions from the not-ready to the ready state)
    list_add_tail(&queue_ready, &(sleep_request->thread->list_entry));
  }
}

void schedule()
{
  do
  {
    complete_all_sleep_requests();
  } while (list_is_empty(&queue_ready) && !list_is_empty(&queue_sleep));

  uthread_t *next_thread = list_is_empty(&queue_ready)
                               ? thread_main
                               : node_of(list_remove_head(&queue_ready), uthread_t, list_entry);
  if (next_thread == thread_running)
  {
    return;
  }
  uthread_t *current = thread_running;
  thread_running = next_thread;
  context_switch(current, next_thread);
}

void schedule_without_save()
{
  do
  {
    complete_all_sleep_requests();
  } while (list_is_empty(&queue_ready) && !list_is_empty(&queue_sleep));

  uthread_t *next_thread = list_is_empty(&queue_ready)
                               ? thread_main
                               : node_of(list_remove_head(&queue_ready), uthread_t, list_entry);
  if (next_thread == thread_running)
  {
    return;
  }
  uthread_t *current = thread_running;
  thread_running = next_thread;
  context_switch_without_save(current, next_thread);
}

void internal_start()
{
  // call the threads entry-point
  thread_running->start(thread_running->arg);

  // the thread's entry point returned
  schedule_without_save();
}

void ut_init()
{
  list_init(&queue_ready);
  list_init(&queue_sleep);
}

uthread_t *ut_create(start_routine_t start_routine, uint64_t arg)
{
  uthread_t *thread = (uthread_t *)malloc(STACK_SIZE);
  uthread_context_t *context = (uthread_context_t *)(((uint8_t *)thread) + STACK_SIZE - sizeof(uthread_context_t));

  context->rbp = 0;
  context->func_addr = internal_start;

  thread->rsp = (uint64_t)context;
  thread->start = start_routine;
  thread->arg = arg;

  list_add_tail(&queue_ready, &(thread->list_entry));

  return thread;
}

void ut_run()
{
  uthread_t main_thread_struct;
  thread_main = &main_thread_struct;
  thread_running = thread_main;
  schedule();
}

void ut_yield()
{
  list_add_tail(&queue_ready, &(thread_running->list_entry));
  schedule();
}

void ut_sleep(uint32_t delay)
{
  sleep_request_t sleep_request;
  sleep_request.end = get_time() + delay;
  sleep_request.thread = thread_running;
  list_entry_t *curr = queue_sleep.next;
  // sorted insert
  while (true)
  {
    // reached end
    if (curr == &queue_sleep)
    {
      break;
    }
    sleep_request_t *req = node_of(curr, sleep_request_t, list_entry);
    if (req->end >= sleep_request.end)
    {
      break;
    }
    curr = curr->next;
  }
  list_add_before(curr, &(sleep_request.list_entry));
  schedule();
}
