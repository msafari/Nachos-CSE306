// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.userprog;

import nachos.Debug;
import nachos.machine.CPU;
import nachos.machine.MIPS;
import nachos.machine.Machine;
import nachos.machine.MachineException;
import nachos.kernel.userprog.Syscall;

/**
 * An ExceptionHandler object provides an entry point to the operating system
 * kernel, which can be called by the machine when an exception occurs during
 * execution in user mode.  Examples of such exceptions are system call
 * exceptions, in which the user program requests service from the OS,
 * and page fault exceptions, which occur when the user program attempts to
 * access a portion of its address space that currently has no valid
 * virtual-to-physical address mapping defined.  The operating system
 * must register an exception handler with the machine before attempting
 * to execute programs in user mode.
 */
public class ExceptionHandler implements nachos.machine.ExceptionHandler {

  /**
   * Entry point into the Nachos kernel.  Called when a user program
   * is executing, and either does a syscall, or generates an addressing
   * or arithmetic exception.
   *
   * 	For system calls, the following is the calling convention:
   *
   * 	system call code -- r2,
   *		arg1 -- r4,
   *		arg2 -- r5,
   *		arg3 -- r6,
   *		arg4 -- r7.
   *
   *	The result of the system call, if any, must be put back into r2. 
   *
   * And don't forget to increment the pc before returning. (Or else you'll
   * loop making the same system call forever!)
   *
   * @param which The kind of exception.  The list of possible exceptions 
   *	is in CPU.java.
   *
   * @author Thomas Anderson (UC Berkeley), original C++ version
   * @author Peter Druschel (Rice University), Java translation
   * @author Eugene W. Stark (Stony Brook University)
   */
    public void handleException(int which) {
	int type = CPU.readRegister(2);

	if (which == MachineException.SyscallException) {

	    switch (type) {
	    case Syscall.SC_Halt:
		Syscall.halt();
		break;
	    case Syscall.SC_Exit:
		Syscall.exit(CPU.readRegister(4));
		break;
	    case Syscall.SC_Exec:
		Syscall.exec("");
		break;
	    case Syscall.SC_Write:
		int ptr = CPU.readRegister(4);
		int len = CPU.readRegister(5);
		byte buf[] = new byte[len];

		System.arraycopy(Machine.mainMemory, ptr, buf, 0, len);
		Syscall.write(buf, len, CPU.readRegister(6));
		break;
	    }

	    // Update the program counter to point to the next instruction
	    // after the SYSCALL instruction.
	    CPU.writeRegister(MIPS.PrevPCReg,
		    CPU.readRegister(MIPS.PCReg));
	    CPU.writeRegister(MIPS.PCReg,
		    CPU.readRegister(MIPS.NextPCReg));
	    CPU.writeRegister(MIPS.NextPCReg,
		    CPU.readRegister(MIPS.NextPCReg)+4);
	    return;
	}

	System.out.println("Unexpected user mode exception " + which +
		", " + type);
	Debug.ASSERT(false);

    }
}
