package nachos.kernel.userprog;

import nachos.Debug;
import nachos.Options;
import nachos.machine.CPU;
import nachos.machine.MIPS;
import nachos.machine.NachosThread;
import nachos.kernel.Nachos;
import nachos.kernel.userprog.AddrSpace;
import nachos.kernel.userprog.UserThread;
import nachos.kernel.filesys.OpenFile;

/**
 * This is a class for a User Process that makes a new UserThread with it's own address space
 * 
 * @author Thomas Anderson (UC Berkeley), original C++ version
 * @author Peter Druschel (Rice University), Java translation
 * @author Eugene W. Stark (Stony Brook University)
 */
public class UserProcess implements Runnable {

    /** The name of the program to execute. */
    private String execName;
    
    /** Function address for forking*/
    private int funcAddr = -1;
    
    private AddrSpace space;
    
    public int processID;
    
    /**
     * Overloaded constructor for forking processes
     *
     * @param filename The name of the program to execute.
     */
    public UserProcess(int funcAddr, AddrSpace space) {
	
	this.space = space;
	String name = "UserProcess (" + funcAddr + ")";
	
	Debug.println('+', "starting forked UserProcess: " + name);

	UserThread t = new UserThread(name, this, space);
	
	
	this.processID = t.processID;
	this.funcAddr = funcAddr;
	
	//add this to the child thread list, join syscall uses this list

	if(this.processID != 0)		//check if it's not the parent(main) thread
	    ((UserThread)NachosThread.currentThread()).childThreads.add(t);
	
	Nachos.scheduler.readyToRun(t);
    }
    
    /**
     * Start the test by creating a new address space and user thread,
     * then arranging for the new thread to begin executing the run() method
     * of this class.
     *
     * @param filename The name of the program to execute.
     */
    public UserProcess(String filename) {
	String name = "UserProcess (" + filename + ")";
	
	Debug.println('+', "starting UserProcess: " + name);

	execName = filename;
	AddrSpace space = new AddrSpace();
	this.space = space;
	UserThread t = new UserThread(name, this, space);
	
	
	this.processID = t.processID;
	
	//add this to the AddrSpace space = ((UserThread)NachosThread.currentThread()).space;child thread list, join syscall uses this list

	if(this.processID != 0)		//check if it's not the parent(main) thread
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
	
	if(this.funcAddr == -1) {
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
	}
	
	else {
	    
	    //this.space.initRegisters();
	    this.space.restoreState();
	    CPU.writeRegister(MIPS.PCReg, this.funcAddr);
	    CPU.writeRegister(MIPS.NextPCReg, this.funcAddr + 4);
	}
	

	

	CPU.runUserCode();		// jump to the user progam
	Debug.ASSERT(false);		// machine->Run never returns;
	// the address space exits
	// by doing the syscall "exit"
    }
    
}
