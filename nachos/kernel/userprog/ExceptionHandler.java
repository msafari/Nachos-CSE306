// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.userprog;

import nachos.kernel.threads.Semaphore;
import nachos.Debug;
import nachos.machine.CPU;
import nachos.machine.MIPS;
import nachos.machine.Machine;
import nachos.machine.MachineException;
import nachos.machine.NachosThread;
import nachos.kernel.userprog.Syscall;

/**
 * An ExceptionHandler object provides an entry point to the operating system
 * kernel, which can be called by the machine when an exception occurs during
 * execution in user mode. Examples of such exceptions are system call
 * exceptions, in which the user program requests service from the OS, and page
 * fault exceptions, which occur when the user program attempts to access a
 * portion of its address space that currently has no valid virtual-to-physical
 * address mapping defined. The operating system must register an exception
 * handler with the machine before attempting to execute programs in user mode.
 */
public class ExceptionHandler implements nachos.machine.ExceptionHandler {

    //Semaphore for read/write
    private Semaphore ioSemaphore = new Semaphore("ioSemaphore", 1);
    
    /**
     * Entry point into the Nachos kernel. Called when a user program is
     * executing, and either does a syscall, or generates an addressing or
     * arithmetic exception.
     * 
     * For system calls, the following is the calling convention:
     * 
     * system call code -- r2, arg1 -- r4, arg2 -- r5, arg3 -- r6, arg4 -- r7.
     * 
     * The result of the system call, if any, must be put back into r2.
     * 
     * And don't forget to increment the pc before returning. (Or else you'll
     * loop making the same system call forever!)
     * 
     * @param which
     *            The kind of exception. The list of possible exceptions is in
     *            CPU.java.
     * 
     * @author Thomas Anderson (UC Berkeley), original C++ version
     * @author Peter Druschel (Rice University), Java translation
     * @author Eugene W. Stark (Stony Brook University)
     */
    public void handleException(int which) {
	int type = CPU.readRegister(2);
	int result = -1;
	
	//Handle syscalls here
	if (which == MachineException.SyscallException) {

	    switch (type) {

	    case Syscall.SC_Join:
		result = Syscall.join(CPU.readRegister(4));
		CPU.writeRegister(2, result);
		break;
	    case Syscall.SC_Create:
		String createFile = getFileName(4);
		Syscall.create(createFile);
		break;
	    case Syscall.SC_Open:
		String openFile = getFileName(4);
		result = Syscall.open(openFile);
		CPU.writeRegister(2, result);
		break;
	    case Syscall.SC_Read:
		//Block on read
		ioSemaphore.P();
		
		int virtualAddress = CPU.readRegister(4);
		int inputLength = CPU.readRegister(5);
		byte readBuf[] = new byte[inputLength];
		result = Syscall.read(readBuf, inputLength, CPU.readRegister(6));
		int physicalAddress = ((UserThread)NachosThread.currentThread()).space.translate(virtualAddress, inputLength, true);
		System.arraycopy(readBuf, 0, Machine.mainMemory, physicalAddress, inputLength);
		CPU.writeRegister(2, result);
		
		//Release block
		ioSemaphore.V();
		break;
	    case Syscall.SC_Close:
		Syscall.close(CPU.readRegister(4));
		break;
	    case Syscall.SC_Fork:
		Syscall.fork(CPU.readRegister(4));
		break;
	    case Syscall.SC_Yield:
		Syscall.yield();
		break;
	    case Syscall.SC_Remove:
		String removeFile = getFileName(4);
		Syscall.remove(removeFile);
		break;
	    case Syscall.SC_Halt:
		Syscall.halt();
		break;
	    case Syscall.SC_Exit:
		Syscall.exit(CPU.readRegister(4));
		break;
	    case Syscall.SC_Exec:
		String fileName = getFileName(4);
		result = Syscall.exec(fileName);
		CPU.writeRegister(2, result);
		break;
	    case Syscall.SC_Write:
		//Block on write
		ioSemaphore.P();
		Debug.println('S', "Write syscall");
		int ptr = CPU.readRegister(4);
		int len = CPU.readRegister(5);
		
		byte buf[] = new byte[len];
		((UserThread)NachosThread.currentThread()).space.readVirtualMemory(ptr, buf, 0, len, false);
		
		Syscall.write(buf, len, CPU.readRegister(6));
		
		//Release block
		ioSemaphore.V();
		break;
	    case Syscall.SC_Sleep:
		Syscall.sleep(CPU.readRegister(4));
		break;
	    case Syscall.SC_Mkdir:
		String makedirName = getFileName(4);
		Syscall.makeDirectory(makedirName);
		break;
	    case Syscall.SC_Rmdir:
		String removedirName = getFileName(4);
		Syscall.removeDirectory(removedirName);
		break;
	    case Syscall.SC_Mmap:
		System.out.println("Mmap called");
		Debug.showArguments();
		break;
	    case Syscall.SC_Munmap:
		System.out.println("Munmap called");
		Debug.showArguments();
		break;
		
	    default:
		Debug.println('S', "Invalid Syscall: " + type);
		Debug.ASSERT(false);
	    }

	    // Write syscall status back to result register and update the program counter.
	    CPU.writeRegister(MIPS.PrevPCReg, CPU.readRegister(MIPS.PCReg));
	    CPU.writeRegister(MIPS.PCReg, CPU.readRegister(MIPS.NextPCReg));
	    CPU.writeRegister(MIPS.NextPCReg, CPU.readRegister(MIPS.NextPCReg) + 4);
	    
	    return;
	}
	//Handle page faults here
	else if(which == MachineException.PageFaultException){
	    System.out.println("Page Fault: " + CPU.readRegister(4));
	    
	}
	else{
	    System.out.println("Unexpected user mode exception " + which + ", " + type);
	    Debug.ASSERT(false);
	}

    }

    /*
     * Function to return a string from an array of bytes.
     */
    public String bytesToString(byte[] buf) {
	
	int i = 0;
	for(i = 0; i < buf.length; i++){
	    if(buf[i] == 0)
		break;
	}
	
	byte[] shortBuf = new byte[i];
	
	int j = 0;
	while(j < i){
	    shortBuf[j] = buf[j];
	    j++;
	}
	
	String fileName = new String(shortBuf);
	return fileName;
    }

    /*
     * Returns filename of address specified by register number. Register number
     * must be between 2-25 to be a valid reg.
     */
    public String getFileName(int reg) {

	Debug.println('S', "getFileName obtaining name from pointer.");

	// Check reg number
	if (reg >= 2 && reg <= 25) {
	    int length = 20; // Typically allow only 255 chars for filename.
	    int ptr = CPU.readRegister(reg); // Get the address this pointer is pointing to.
	    byte buf[] = new byte[length];
	    String fileName;
	    
	    int pAddr = ((UserThread)NachosThread.currentThread()).space.translate(ptr, length, false);

	    System.arraycopy(Machine.mainMemory, pAddr, buf, 0, length);
	    fileName = bytesToString(buf);
	    Debug.println('S', "File name is: " + fileName);

	    return fileName;
	}
	// Otherwise return null
	else {
	    Debug.println('S', "Error: Invalid register number.");
	    return null;
	}
    }

}
