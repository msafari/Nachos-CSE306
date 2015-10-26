// ProgTest.java
//	Test class for demonstrating that Nachos can load
//	a user program and execute it.  
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.userprog;

import nachos.Debug;
import nachos.Options;
import nachos.machine.CPU;
import nachos.machine.NachosThread;
import nachos.kernel.Nachos;
import nachos.kernel.userprog.AddrSpace;
import nachos.kernel.userprog.UserThread;
import nachos.kernel.filesys.OpenFile;

/**
 * This is a test class for demonstrating that Nachos can load a user
 * program and execute it.
 * 
 * @author Thomas Anderson (UC Berkeley), original C++ version
 * @author Peter Druschel (Rice University), Java translation
 * @author Eugene W. Stark (Stony Brook University)
 */
public class UserProcess implements Runnable {

    /** The name of the program to execute. */
    private String execName;

    public int processID;
    /**
     * Start the test by creating a new address space and user thread,
     * then arranging for the new thread to begin executing the run() method
     * of this class.
     *
     * @param filename The name of the program to execute.
     */
    public UserProcess(String filename) {
	String name = "ProgTest (" + filename + ")";
	
	Debug.println('+', "starting ProgTest: " + name);

	execName = filename;
	AddrSpace space = new AddrSpace();
	UserThread t = new UserThread(name, this, space);
	
	
	this.processID = t.processID;
	//add this to the child thread list
	//join syscall uses this list
	if(this.processID != 0)
	    ((UserThread)NachosThread.currentThread()).childThreads.add(t);
	
	Nachos.scheduler.readyToRun(t);
    }

    /**
     * Entry point for the thread created to run the user program.
     * The specified executable file is used to initialize the address
     * space for the current thread.  Once this has been done,
     * CPU.run() is called to transfer control to user mode.
     */
    public void run() {
	OpenFile executable;

	if((executable = Nachos.fileSystem.open(execName)) == null) {
	    Debug.println('+', "Unable to open executable file: " + execName);
	    Nachos.scheduler.finishThread();
	    return;
	}

	AddrSpace space = ((UserThread)NachosThread.currentThread()).space;
	if(space.exec(executable) == -1) {
	    Debug.println('+', "Unable to read executable file: " + execName);
	    Nachos.scheduler.finishThread();
	    return;
	}

	space.initRegisters();		// set the initial register values
	space.restoreState();		// load page table register

	CPU.runUserCode();			// jump to the user progam
	Debug.ASSERT(false);		// machine->Run never returns;
	// the address space exits
	// by doing the syscall "exit"
    }
    
}
