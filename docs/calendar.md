# High Level Weekly Outline

## Work assignments calendar

* First exercise set: published until 28 March (week 4) e delivered until 23 April (end of week 7).
* Second exercise set: published until 25 April (week 8) e delivered until 14 May (end of week 10).
* Third exercise set: published until 23 May (week 12) e delivered until 18 June (end of week 15).

## Weekly tentative schedule

### W1 - 2022-03-07

- Course introduction.
- Setting the baseline.
- Why have multiple sequential computations - e.g. use the chat server.
- Thread creation and synchronization in the JVM.
  
### W2 - 2022-03-14

- OS/Platform threads. 
- JVM usage of platform threads.
- _uthreads_: context, context switch, scheduling, thread states.
  
### W3 - 2022-03-21

- Data sharing between threads: identification (args, locals, fields, statics, captured context).
- Data sharing hazards and Data Synchronization.
- Mutual exclusion in Java.
- Examples: echo server - count number of echos per client, count number of echos globally, caching.
- Thread coordination and Control Synchronization.
- The synchronizer concept.
- The semaphore synchronizer.
  
### W4 - 2022-03-28

- The monitor concept.
- Implementing custom synchronizers using monitors.

### W5 - 2022-04-04

- Implementing custom synchronizers using monitors.

### W6 - 2022-04-11

- What is a memory model and why do we need one.
- The Java Memory Model.
- Before Easter, no classes on Thursday and Friday.
  
### W7 - 2022-04-18

- Lock-free algorithms and data structures.
- After Easter, no classes on Monday.

### W8 - 2022-04-25

- The problem with blocking/synchronous I/O and coordination.
- Asynchronous I/O on the JVM - NIO2.
- The problem of defining computations using the callback model. Examples using state machines.
- Holiday on Monday.
  
### W9 - 2022-05-02

- Futures and promises.
- `CompletableFuture` and its methods ("map", "flatMap", ...)
- Promises in javascript and asynchronous methods.
  
### W10 - 2022-05-09

- Kotlin coroutines as a way to have suspendable sequential computations.
- Structured concurrency.
- Continuation Passing Style, coroutine suspension, and converting callbacks to suspend functions.
    
### W11 - 2022-05-16

- More of the above.
- Asynchronous coordination (_asynchronizers_).

### W12 - 2022-05-23

- More of the above (i.e. Kotlin coroutines and related subjects).
  
### W13 - 2022-05-30

- Kotlin flow and reactive streams.
  
### W14 - 2022-06-06

- More of the above (i.e. Kotlin flow and reactive streams).
- Holiday on Friday.
  
### W15 - 2022-06-13

- Revisions and support. 
- Holidays on Monday and Thursday.
