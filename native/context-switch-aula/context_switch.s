	.text
	
	.global context_switch

// %rdi points to the th0 thread structure
// %rsi points to the th1 thread structure
context_switch:
  // suspend th0 ...
  // ... by saving ve the registers that aren't saved by the C compiler generated code 
	pushq %rbp
	pushq %rbx
	pushq %r12
	pushq %r13
	pushq %r14
	pushq %r15

  // ... %rsp is the only one that cannot be saved in the stack
	movq %rsp, (%rdi)
	
  // resume th1 ...
  // ... loading into the stack the value of th1 %rsp
	movq (%rsi), %rsp
	
  // ... and restoring the saved registers when th1 was suspended
	popq %r15
	popq %r14
	popq %r13
	popq %r12
	popq %rbx
	popq %rbp
	
  // ... returning to the point on th1 that called context_switch
	ret
