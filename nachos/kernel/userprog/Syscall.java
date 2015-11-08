// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.userprog;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.LinkedList;

import nachos.Debug;
import nachos.kernel.Nachos;
import nachos.kernel.devices.ConsoleDriver;
import nachos.kernel.filesys.OpenFile;
import nachos.kernel.filesys.OpenFileEntry;
import nachos.kernel.threads.Lock;
import nachos.kernel.threads.Semaphore;
import nachos.kernel.threads.SpinLock;
import nachos.kernel.userprog.test.ProgTest;
import nachos.machine.CPU;
import nachos.machine.MIPS;
import nachos.machine.Machine;
import nachos.machine.NachosThread;
import nachos.machine.Simulation;


/**
 * Nachos system call interface.  These are Nachos kernel operations
 * 	that can be invoked from user programs, by trapping to the kernel
 *	via the "syscall" instruction.
 *
 * @author Thomas Anderson (UC Berkeley), original C++ version
 * @author Peter Druschel (Rice University), Java translation
 * @author Eugene W. Stark (Stony Brook University)
 */
public class Syscall {

    // System call codes -- used by the stubs to tell the kernel 
    // which system call is being asked for.

    /** Integer code identifying the "Halt" system call. */
    public static final byte SC_Halt = 0;

    /** Integer code identifying the "Exit" system call. */
    public static final byte SC_Exit = 1;

    /** Integer code identifying the "Exec" system call. */
    public static final byte SC_Exec = 2;

    /** Integer code identifying the "Join" system call. */
    public static final byte SC_Join = 3;

    /** Integer code identifying the "Create" system call. */
    public static final byte SC_Create = 4;

    /** Integer code identifying the "Open" system call. */
    public static final byte SC_Open = 5;

    /** Integer code identifying the "Read" system call. */
    public static final byte SC_Read = 6;

    /** Integer code identifying the "Write" system call. */
    public static final byte SC_Write = 7;

    /** Integer code identifying the "Close" system call. */
    public static final byte SC_Close = 8;

    /** Integer code identifying the "Fork" system call. */
    public static final byte SC_Fork = 9;

    /** Integer code identifying the "Yield" system call. */
    public static final byte SC_Yield = 10;

    /** Integer code identifying the "Remove" system call. */
    public static final byte SC_Remove = 11;
    
    /** Integer code identifying the "Sleep" system call. */
    public static final byte SC_Sleep = 12;
    
    /** Integer code identifying the "Mkdir" system call. */
    public static final byte SC_Mkdir = 13;
    
    /** Integer code identifying the "Rmdir" system call. */
    public static final byte SC_Rmdir = 14;
    
    public static Lock writeLock = new Lock("writeLock");
    
    public static Lock readLock = new Lock("readLock");
    
    public static Semaphore joinSem = new Semaphore("joinSem", 0);
    
    public static AddrSpace addrSpace;

    public static LinkedList<UserThread> runningThreads = new LinkedList<UserThread>();
    
    //Keep track of open/closed files
    public static Lock openFileLock = new Lock("openFileLock");
    public static LinkedList<OpenFileEntry> openFileList = new LinkedList<OpenFileEntry>();
    public static int openFileID = 2; //Starts at 2 since 0 & 1 are ConsoleInput/Output
    
    
    /**
     * Stop Nachos, and print out performance stats.
     */
    public static void halt() {
	if(((UserThread)NachosThread.currentThread()).processID == 0){
        	Debug.print('+', "Shutdown, initiated by user program.\n");
        	Simulation.stop();
	}
    }
    
    public static void sleep(int numOfTicks) {
	UserThread threadToSleep = ((UserThread)NachosThread.currentThread());
	
	Debug.println('S', "Sleeping thread: " + threadToSleep.name + " for: "+ numOfTicks + " ticks.");
	
	threadToSleep.numOfTicksToSleep = numOfTicks;
	Nachos.scheduler.sleepList.offer(threadToSleep);
	threadToSleep.sleepSemaphore.P();
    }
    
    /* Address space control operations: Exit, Exec, and Join */

    /**
     * The Exit() call takes a single argument, which is an integer status value as in Unix. 
     * The calling thread is terminated, and the memory pages belonging to its stack area are deallocated. 
     * When the last thread in an address space exits, 
     * the remaining pages are deallocated, the address space terminates, 
     * and the argument passed by the last thread to Exit() becomes the exit status for the address space. The exit status will be used by the Join() system call as described below.
     *
     * @param status Status code to pass to processes doing a Join().
     * status = 0 means the program exited normally.
     */
    public static void exit(int status) {
	
	//Deallocate any physical memory and other resources that are assigned to this thread
	UserThread currThrd = ((UserThread)NachosThread.currentThread());
	
	//Set the exit status of the thread
	currThrd.exitStatus = status;
	
	Debug.println('M', "User program exits with status=" + status + ": " + currThrd.name);
	
	//Free the address space
	AddrSpace space = currThrd.space;
	space.free();
	
	currThrd.joinSem.V(); 		//unblock join
	
	//Remove thread from running list
	runningThreads.remove(currThrd);
	
	//if there are no more running threads exit
	if(runningThreads.isEmpty()) {
	   Debug.println('+', "Exiting last thread. Setting exitStatus to: "+ status);   
	   currThrd.exitStatus = status; 	// set the exit status of the addrspace   
	   Simulation.stop(); 			//halt nachos machine
	   
	}
	
	Debug.println('S', "Current thread: " + ((UserThread)NachosThread.currentThread()).processID);

	Nachos.scheduler.finishThread();
    }
    /**
     * Run the executable, stored in the Nachos file "name", and return the 
     * address space identifier.
     *
     * @param name The name of the file to execute.
     */
    public static int exec(final String name) {
	
	Debug.println('S', "Exec SysCall is called");
	
	//Create a new ProgTest object, ignore num since processID is managed in the UserThread class
	UserProcess userProcess = new UserProcess(name);
	
	//An integer value ("SpaceId") that uniquely identifies the newly created process is returned to the caller
	return userProcess.processID;
	
    }

    /**
     * 
     * The Join() system call takes as its single argument a SpaceId returned by a previous call to Exec(). 
     * The thread making the Join() call should block until the address space with the given ID has terminated, 
     * as a result of an Exit() call having been executed by the last thread executing in that address space. 
     * The exit status that was supplied by that thread as the argument to the Exit() call should be returned as as the result of the Join() call.
     * 
     * Wait for the user program specified by "id" to finish, and
     * return its exit status.
     * 
     *
     * @param id The "space ID" of the program to wait for.
     * @return the exit status of the specified program.
     */
    public static int join(int id) {
	
	UserThread currThrd = (UserThread)NachosThread.currentThread();
	Debug.println('J', "Starting System Call Join with id: "+ id);
	
	for(UserThread child: currThrd.childThreads){
	    if (child.processID == id) {
		Debug.println('J', "blocking proccesID "+ child.processID +" until process is terminated"); 
		
		child.joinSem.P(); 			//block join
		Debug.println('J', "Thread "+ child.name + " terminated with status: "+ child.exitStatus);
		return child.exitStatus; 	//return child's exitStatus after termination
	    }
	}
	
	//if it gets here means it couldn't match the processId to an existing process's ID
	Debug.println('J', "There's no existing thread with proccesID: "+ id);
	return -1;
    }


    /* File system operations: Create, Open, Read, Write, Close
     * These functions are patterned after UNIX -- files represent
     * both files *and* hardware I/O devices.
     *
     * If this assignment is done before doing the file system assignment,
     * note that the Nachos file system has a stub implementation, which
     * will work for the purposes of testing out these routines.
     */

    // When an address space starts up, it has two open files, representing 
    // keyboard input and display output (in UNIX terms, stdin and stdout).
    // Read and write can be used directly on these, without first opening
    // the console device.

    /** OpenFileId used for input from the keyboard. */
    public static final int ConsoleInput = 0;

    /** OpenFileId used for output to the display. */
    public static final int ConsoleOutput = 1;

    /**
     * Create a Nachos file with a specified name.
     *
     * @param name  The name of the file to be created.
     */
    public static void create(String name) {
	Debug.println('S', "Syscall Create is called for: " + name + ", size: 0");
	boolean result = Nachos.fileSystem.create(name, 0);
	if(!result){
	    Debug.println('S', "Could not create file: " + name);
	    Debug.ASSERT(false);
	}
    }
    
    /**
     * Remove a Nachos file.
     *
     * @param name  The name of the file to be removed.
     */
    public static void remove(String name) {
	
	//If file is open, close it and remove it from the list
	OpenFileEntry fe = findOpenFileEntry(name);
	if(fe != null){
	    close(fe.id);
	}
	
	//Have the file system remove it regardless
	boolean result = Nachos.fileSystem.remove(name);
	if(!result){
	    Debug.println('S', "File: " + name + " was not removed!");
	    Debug.ASSERT(false);
	}
	
    }
    
    
    /**
     * Close the file, we're done reading and writing to it.
     *
     * @param id  The OpenFileId of the file to be closed.
     */
    public static void close(int id) {
	
	Debug.println('S', "Syscall Close is called for: " + id);
	
	//Close the file and remove from openFile list
	OpenFileEntry fe = findOpenFileEntry(id);
	if(fe != null){
	    fe.file.close();
	    removeOpenFileEntry(fe);
	}
	else{
	    Debug.println('S', "File could not be closed");
	}
    }
    
    
    /**
     * Returns an entry with the given id
     */
    private static OpenFileEntry findOpenFileEntry(int id){
	openFileLock.acquire();
	for(OpenFileEntry e: openFileList){
	    if(e.id == id){
		openFileLock.release();
		return e;
	    }
	}
	openFileLock.release();
	return null;
    }
    
    /**
     * Returns an entry with the given name
     */
    private static OpenFileEntry findOpenFileEntry(String name){
	openFileLock.acquire();
	for(OpenFileEntry e: openFileList){
	    if(e.name.equals(name)){
		openFileLock.release();
		return e;
	    }
	}
	openFileLock.release();
	return null;
    }
    
    /**
     * Removes an entry from the open file list
     * @param name
     * @return
     */
    private static boolean removeOpenFileEntry(OpenFileEntry fe){
	
	boolean result = false;
	openFileLock.acquire();
	result = openFileList.remove(fe);
	openFileLock.release();
	Debug.println('S', "OpenFileEntry removed: " + result);
	return result;
	
    }

    /**
     * Adds an entry to the open file list
     * @param fileName
     * @param file
     * @return
     */
    private static int addOpenFileEntry(OpenFile file, String fileName){
	openFileLock.acquire();
	OpenFileEntry fileEntry = new OpenFileEntry(file);
	fileEntry.id = openFileID;
	fileEntry.name = fileName;
	openFileID++;
	openFileList.offer(fileEntry);
	openFileLock.release();
	
	Debug.println('S', "Added open file entry id: " + fileEntry.id);
	
	return fileEntry.id;
    }
    
    /**
     * Open the Nachos file "name", and return an "OpenFileId" that can 
     * be used to read and write to the file.
     *
     * @param name  The name of the file to open.
     * @return  An OpenFileId that uniquely identifies the opened file.
     */
    public static int open(String name) {
	
	//Open the file
	OpenFile file = Nachos.fileSystem.open(name);
	
	//If file is null, fs was not able to open it
	if(file == null){
	    Debug.println('S', "Unable to open file: " + name);
	    Debug.ASSERT(false);
	}
	
	//Otherwise add it to the openFile list
	int fileEntryId = addOpenFileEntry(file, name);
	
	return fileEntryId;
	}

    /**
     * Write "size" bytes from "buffer" to the open file.
     *
     * @param buffer Location of the data to be written.
     * @param size The number of bytes to write.
     * @param id The OpenFileId of the file to which to write the data.
     */
    public static void write(byte buffer[], int size, int id) {
	writeLock.acquire();
	UserThread curThrd = (UserThread)NachosThread.currentThread();
	byte[] b = new byte[1];
	char[] c = new char[1];
	
	if (id == ConsoleOutput) {
	    
	    curThrd.writeSize = size;		//store the size of write for each userthread
	    
	    for(int i = 0; i < size; i++) {
		b[0] = buffer[i];
		try {
		    c = Nachos.consoleDriver.translate(b);
		    
		    if(c[0] == '\n'){
			curThrd.writeSize++;
			Nachos.consoleDriver.putChar('\r');
		    }
		    
		    Nachos.consoleDriver.putChar(c[0]);
		} catch (UnsupportedEncodingException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
	    }
	    
	    writeLock.release();
	}
	
	//Otherwise write to file
	else{
	    //Check if file is open
	    OpenFileEntry e = findOpenFileEntry(id);
	    if(e != null){
		// Write to it
		int result = e.file.write(buffer, 0, size);
		Debug.println('S', "Wrote " + result + " bytes to file: " + e.name);
	    }
	    else{
		Debug.println('S', "File: " + id + " must be opened first");
	    }
	    
	    
	    writeLock.release();
	}
	
	
    }

    
    /**
     * Read "size" bytes from the open file into "buffer".  
     * Return the number of bytes actually read -- if the open file isn't
     * long enough, or if it is an I/O device, and there aren't enough 
     * characters to read, return whatever is available (for I/O devices, 
     * you should always wait until you can return at least one character).
     *
     * @param buffer Where to put the data read.
     * @param size The number of bytes requested.
     * @param id The OpenFileId of the file from which to read the data.
     * @return The actual number of bytes read.
     */
    public static int read(byte buffer[], int size, int id) {
	readLock.acquire();
	int i = 0;
	UserThread curThrd = (UserThread) NachosThread.currentThread();
	curThrd.readSize = size; // store the size of read for each userthread
	Debug.println('S', "Reading: size: " + size + ", id: " + id);
	
	//Read from Console
	if (id == ConsoleInput) {
	    try {
		for (i = 0; i < size; i++) {
		    buffer[i] = (byte) Nachos.consoleDriver.getChar();
		    
		}
		
	    } catch (Exception e) {
		Debug.println('S', "Exception occured");
		return i;
	    }

	    //Return num of bytes read
	    readLock.release();
	    return i;
	    
	}
	//Otherwise read from file
	else {
	    // Check if file is open
	    OpenFileEntry e = findOpenFileEntry(id);
	    int result = -1;
	    if (e != null) {
		// Read from it
		result = e.file.read(buffer, 0, size);
		Debug.println('S', "Read " + result + " bytes to file: " + e.name);
	    } else {
		Debug.println('S', "File: " + id + " must be opened first");
	    }

	    readLock.release();
	    return result;
	}
    }

    /*
     * User-level thread operations: Fork and Yield.  To allow multiple
     * threads to run within a user program. 
     */

    /**
     * The Fork() system call takes a single void (*)() function pointer as an argument, 
     * it creates a new UserThread that shares its address space (except for the stack) with the calling thread, 
     * and the new thread begins execution with a call to the argument function. 
     * The threads sharing an address space will each have their own page table. 
     * Although the stack portion of each page table should map physical pages that are private to one thread, 
     * the code and data pages will be the same for (i.e. shared between) all the threads executing in the same address space.
     * 
     * Fork a thread to run a procedure ("func") in the *same* address space 
     * as the current thread.
     *
     * @param func The user address of the procedure to be run by the
     * new thread.
     */
    public static void fork(int func) {
	Debug.println('F', "Syscall fork is getting called");
	
	AddrSpace newSpace = ((UserThread)NachosThread.currentThread()).space.clone();
	
	UserProcess forkedProcess = new UserProcess(func, newSpace);	
	
    }
    
    public static void forkHelper(int funcAddr) {
	((UserThread)NachosThread.currentThread()).space.restoreState(); 	//load page tables? or not?
	CPU.writeRegister(MIPS.PCReg, funcAddr);
	CPU.writeRegister(MIPS.NextPCReg, funcAddr + 4);
	
	
    }

    /**
     * Yield the CPU to another runnable thread, whether in this address space 
     * or not. 
     */
    public static void yield() {
	Debug.println('Y', "Syscall Yield is called");
	//Yield the CPU to another thread
	Nachos.scheduler.yieldThread();
	
    }

}
