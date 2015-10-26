package nachos.kernel.userprog;

import java.util.LinkedList;

import nachos.Debug;
import nachos.machine.MIPS;
import nachos.machine.Machine;
import nachos.machine.NachosThread;
import nachos.machine.CPU;
import nachos.kernel.Nachos;
import nachos.kernel.threads.Semaphore;
import nachos.kernel.threads.SpinLock;
import nachos.kernel.userprog.MemoryManager;

/**
 * A UserThread is a NachosThread extended with the capability of
 * executing user code.  It is kept separate from AddrSpace to provide
 * for the possibility of having multiple UserThreads running in a
 * single AddrSpace.
 * 
 */
public class UserThread extends NachosThread {

    public int processID;
    public int exitStatus;
    public LinkedList<UserThread> childThreads = new LinkedList<UserThread>();
    public Semaphore joinSem;
    public Runnable runnable;

    /** The context in which this thread will execute. */
    public final AddrSpace space;

    // A thread running a user program actually has *two* sets of 
    // CPU registers -- one for its state while executing user code,
    // and one for its state while executing kernel code.
    // The kernel registers are managed by the super class.
    // The user registers are managed here.

    /** User-level CPU register state. */
    private int userRegisters[] = new int[MIPS.NumTotalRegs];

    /**
     * Initialize a new user thread.
     *
     * @param name  An arbitrary name, useful for debugging.
     * @param runObj Execution of the thread will begin with the run()
     * method of this object.
     * @param addrSpace  The context to be installed when this thread
     * is executing in user mode.
     */
    public UserThread(String name, Runnable runObj, AddrSpace addrSpace) {
	super(name, runObj);
	
	runnable = runObj;
	
	//Lock
	MemoryManager.processIDLock.acquire();
	
	//Set the processID
	this.processID  = MemoryManager.processID;
	
	//Increment memory manager's processID
	MemoryManager.processID += 1;
	
	//Create the address space
	space = addrSpace;
	
	//make the join semaphore
	joinSem = new Semaphore("joinSem", 0);
	
	//Release lock
	MemoryManager.processIDLock.release();
	
	Syscall.runningThreads.add(this);
    }

    /**
     * Save the CPU state of a user program on a context switch.
     */
    @Override
    public void saveState() {
	// Save state associated with the address space.
	space.saveState();  

	// Save user-level CPU registers.
	for (int i = 0; i < MIPS.NumTotalRegs; i++)
	    userRegisters[i] = CPU.readRegister(i);

	// Save kernel-level CPU state.
	super.saveState();
    }

    /**
     * Restore the CPU state of a user program on a context switch.
     */
    @Override
    public void restoreState() {
	// Restore the kernel-level CPU state.
	super.restoreState();

	// Restore the user-level CPU registers.
	for (int i = 0; i < MIPS.NumTotalRegs; i++)
	    CPU.writeRegister(i, userRegisters[i]);

	// Restore state associated with the address space.
	space.restoreState();
    }
    
}
