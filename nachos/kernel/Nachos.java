// Nachos.java
//	Bootstrap code to initialize the operating system kernel.
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

/**
 *  Nachos "main class".
 *  Instance variables provide access to the main subsystems.
 *  Contains code for bootstrapping the system and launching tests
 *  and demos.
 * 
 * @author Thomas Anderson (UC Berkeley), original C++ version
 * @author Peter Druschel (Rice University), Java translation
 * @author Eugene W. Stark (Stony Brook University)
 */

package nachos.kernel;

import nachos.Options;
import nachos.Debug;
import nachos.machine.CPU;
import nachos.machine.Machine;
import nachos.machine.NachosThread;
import nachos.kernel.devices.ConsoleDriver;
import nachos.kernel.devices.DiskDriver;
import nachos.kernel.devices.NetworkDriver;
import nachos.kernel.devices.SerialDriver;
import nachos.kernel.devices.test.ConsoleTest;
import nachos.kernel.devices.test.NetworkTest;
import nachos.kernel.devices.test.SerialTest;
import nachos.kernel.threads.Scheduler;
import nachos.kernel.userprog.ExceptionHandler;
import nachos.kernel.filesys.FileSystem;
import nachos.kernel.threads.test.SMPTest;
import nachos.kernel.threads.test.ThreadTest;
import nachos.kernel.userprog.test.ProgTest;
import nachos.kernel.filesys.test.FileSystemTest;

/**
 * The Nachos main class.  Nachos is "booted up" when a Java thread calls the
 * main() method of this class.
 */
public class Nachos implements Runnable {
    
    /** Option settings. */
    public static Options options;
    
    /** Access to the scheduler. */
    public static Scheduler scheduler;

    /** Access to the file system. */
    public static FileSystem fileSystem;

    /** Access to the console. */
    public static ConsoleDriver consoleDriver;

    /** Access to the disk. */
    public static DiskDriver diskDriver;

    /** Access to the network. */
    public static NetworkDriver networkDriver;

    /** Access to serial ports. */
    public static SerialDriver serialDriver;

    /**
     * 	Nachos initialization -- performed by first Nachos thread.
     *	Initialize various subsystems, depending on configuration options.
    *	Start test programs, if appropriate.
     *	Once this method is finished, the first thread terminates.
     *	Any activities that are to continue must have their own threads
     *	by that point.
     */
    @SuppressWarnings("unused")
    public void run() {
	// Initialize device drivers.

	if(Machine.NUM_CONSOLES > 0)
	    consoleDriver = new ConsoleDriver(Machine.getConsole(0));

	if(Machine.NUM_DISKS > 0)
	    diskDriver = new DiskDriver(0);

	if(Machine.NUM_PORTS > 0)
	    serialDriver = new SerialDriver();

	if(Machine.NUM_NETWORKS > 0)
	    networkDriver = new NetworkDriver();
	
	// Initialize the filesystem.

	if(options.FILESYS_STUB || options.FILESYS_REAL)
	    fileSystem = FileSystem.init(diskDriver);

	// Do per-CPU initialization:  Before we can run user programs,
	// we need to set an exception handler on each CPU to handle
	// exceptions (traps) from user mode.
	for(int i = 0; i < options.NUM_CPUS; i++) {
	    CPU cpu = Machine.getCPU(i);
	    cpu.setCPUExceptionHandler(new ExceptionHandler());
	}

	// Run test/demo programs, according to the supplied options.
	// These will typically create additional threads to do the actual
	// work, leaving the first thread free to go on and start the rest.
	if(options.THREAD_TEST)
	    ThreadTest.start();
	if(options.SMP_TEST)
	    SMPTest.start();
	if(options.PROG_TEST)
	    ProgTest.start();
	if(options.FILESYS_TEST)
	    FileSystemTest.start();
	if(options.SERIAL_TEST)
	    SerialTest.start();
	if(options.NETWORK_TEST)
	    NetworkTest.start();
	if(options.CONSOLE_TEST)
	    ConsoleTest.start();
	
	// Terminate the first thread, its job is done.
	// Alternatively, you could give this thread the responsibility
	// of waiting for all other threads to terminate and then shutting
	// Nachos down nicely.  Without this, once certain interrupting
	// devices, such as timers or the console keyboard, have been
	// started, Nachos will not shut down by itself because there is
	// no way to tell what a future interrupt might cause to happen!
	scheduler.finishThread();
    }

  /**
   * Bootstrap the operating system kernel.  
   *
   * @param args is the array of command line arguments, which is
   * used to initialize a global Options object, which is used by the
   * various subsystems to figure out what to do.
   */
  public static void main(String args[]) {
      Debug.init(args);
      options = new Options(args);
      Debug.println('+', "Entering main");
      
      // Initialize the hardware.
      Machine.init();
      
      // The kernel code assumes that it is running in the context of a
      // Nachos thread, but right now we are only in a Java thread.
      // So, we need to create the first Nachos thread and start it running
      // under the control of the Nachos scheduler.
      NachosThread firstThread = new NachosThread("FirstThread", new Nachos());
      scheduler = new Scheduler(firstThread);
      
      // The Nachos thread we just created will begin running in the run()
      // method of this class.  The remainder of the system initialization will
      // be taken care of there, so our responsibility here is finished.
  }
}

