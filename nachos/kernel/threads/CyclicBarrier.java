package nachos.kernel.threads;

import nachos.Debug;
import nachos.kernel.Nachos;
import nachos.machine.NachosThread;

/**
 * A <CODE>CyclicBarrier</CODE> is an object that allows a set of threads to
 * all wait for each other to reach a common barrier point.
 * To find out more, read
 * <A HREF="http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/CyclicBarrier.html">the documentation</A>
 * for the Java API class <CODE>java.util.concurrent.CyclicBarrier</CODE>.
 *
 * The class is provided in skeletal form.  It is your job to
 * complete the implementation of this class in conformance with the
 * Javadoc specifications provided for each method.  The method
 * signatures specified below must not be changed.  However, the
 * implementation will likely require additional private methods as well
 * as (private) instance variables.  Also, it is essential to provide
 * proper synchronization for any data that can be accessed concurrently.
 * You may use ONLY semaphores for this purpose.  You may NOT disable
 * interrupts or use locks or spinlocks.
 *
 * NOTE: The skeleton below reflects some simplifications over the
 * version of this class in the Java API.
 */
public class CyclicBarrier {
    
    private int parties = 0; //Number of threads to trip the CyclicBarrier
    private int waitingParties = 0; //Number of threads waiting at barrier
    private boolean isBroken = false; //Flag to set if barrier is broken
    private Semaphore block;
    private Semaphore barrierBlock;
    private Semaphore wait;
    private Runnable barrierAction;
    
    /** Class of exceptions thrown in case of a broken barrier. */
    public static class BrokenBarrierException extends Exception { }

   /**
     * Creates a new CyclicBarrier that will trip when the given number
     * of parties (threads) are waiting upon it, and does not perform a
     * predefined action when the barrier is tripped.
     *
     * @param parties  The number of parties.
     */
    public CyclicBarrier(int parties) {
	this(parties, null);
    }
    
    /**
     * Creates a new CyclicBarrier that will trip when the given number of
     * parties (threads) are waiting upon it, and which will execute the
     * given barrier action when the barrier is tripped, performed by the
     * last thread entering the barrier.
     *
     * @param parties  The number of parties.
     * @param barrierAction  An action to be executed when the barrier
     * is tripped, performed by the last thread entering the barrier.
     */
    public CyclicBarrier(int parties, Runnable barrierAction) {
	
	//Size of parties can't be less than 1 thread
	if(parties < 1)
	    throw new IllegalArgumentException();
	
	//Otherwise setup the variables
	this.parties = parties;
	this.barrierAction = barrierAction;
	this.waitingParties = 0;
	this.isBroken = false;
	this.block = new Semaphore("block", 1); //Implement Semaphores as locks hence 1 'marble' 
	this.barrierBlock = new Semaphore("barrierBlock", 1);
	this.wait = new Semaphore("wait", 0);
	
	Debug.println('C', "Max parties: " + parties);
    }

    /**
     * Waits until all parties have invoked await on this barrier.
     * If the current thread is not the last to arrive then it blocks
     * until either the last thread arrives or some other thread invokes
     * reset() on this barrier.
     *
     * @return  The arrival index of the current thread, where index
     * getParties() - 1 indicates the first to arrive and zero indicates
     * the last to arrive.
     * @throws  BrokenBarrierException in case this barrier is broken.
     */
    public int await() throws BrokenBarrierException{
	
	//If barrier is already broken, throw exception
	if(isBroken){
	    throw new BrokenBarrierException();
	}
	else{
	   //Thread is now waiting at barrier
	    block.P();
	    int index = ++waitingParties;
	    Debug.println('C', "Waiting Parties: "+ waitingParties);
	    block.V();
	    
	    //If this is the last thread, decrement waiting parties and pass it through
	    if(index == parties){

		//Block the critical section
		barrierBlock.P();
		Debug.println('C',"Maximum capacity reached: " + index);
		
		wait.V();
	    }
	    
	    //Otherwise wait for all threads to reach barrier
	    wait.P();
	    
	    //Check if barrier was broken while thread was still waiting, throw exception
	    if (isBroken) {
		throw new BrokenBarrierException();
	    }
	    
	    //Now let the threads pass
	    block.P();
	    Debug.println('C', "Threads waiting now: " + waitingParties);
	    waitingParties -= 1;
	    block.V();
	    
	    //Release wait, letting other threads do work
	    wait.V();
	    
	    //Release barrierBlock once last thread finishes
	    if(index == parties){
		Debug.println('C', "Releasing barrierBlock and wait");
		barrierBlock.V();
	    }
	 
	    return index;
	}
    }

    /**
     * Returns the number of parties currently waiting at the barrier.
     * @return the number of parties currently waiting at the barrier.
     */
    public int getNumberWaiting() {
	return waitingParties;
    }

    /**
     * Returns the number of parties required to trip this barrier.
     * @return the number of parties required to trip this barrier.
     */
    public int getParties() {
	return parties;
    }

    /**
     * Queries if this barrier is in a broken state.
     * @return true if this barrier was reset while one or more threads
     * were blocked in await(), false otherwise.
     */
    public boolean isBroken() {
	return isBroken;
    }

    /**
     * Resets the barrier to its initial state. 
     */
    public void reset() {
	//Block off access to variables when resetting them
	block.P();
	isBroken = true;
	
	//Set semaphore to number of waitingParties
	int count = waitingParties;
	Debug.println('C', "Count is: " + count);
	while(count > 0){
	    wait.V();
	    count--;
	}
	waitingParties = 0;
	Debug.println('C', "Barrier has been reset.");
	block.V();
    }

    /**
      * This method can be called to simulate "doing work".
      * Each time it is called it gives control to the NACHOS
      * simulator so that the simulated time can advance by a
      * few "ticks".
      */
    public static void allowTimeToPass() {
	dummy.P();
	dummy.V();
    }

    /** Semaphore used by allowTimeToPass above. */
    private static Semaphore dummy = new Semaphore("Time waster", 1);

    /**
     * Run a demonstration of the CyclicBarrier facility.
     * @param args  Arguments from the "command line" that can be
     * used to modify features of the demonstration such as the
     * number of parties, the amount of "work" performed by
     * each thread, etc.
     *
     * IMPORTANT: Be sure to test your demo with the "-rs xxxxx"
     * command-line option passed to NACHOS (the xxxxx should be
     * replaced by an integer to be used as the seed for
     * NACHOS' pseudorandom number generator).  If you fail to
     * include this option, then a thread that has been started will
     * always run to completion unless it explicitly yields the CPU.
     * This will result in the same (very uninteresting) execution
     * each time NACHOS is run, which will not be a very good
     * test of your code.
     */
    public static void demo() {
	// Very simple example of the intended use of the CyclicBarrier
	// facility: you should replace this code with something much
	// more interesting.
	final CyclicBarrier barrier = new CyclicBarrier(5);
	
	Debug.println('C', "CyclicBarrier Demo starting");
	
	for(int i = 0; i < 5; i++) {
	    NachosThread thread = new NachosThread("Worker thread " + i, new Runnable() {
		    public void run() {
			Debug.println('C', "Thread " + NachosThread.currentThread().name + " is starting");
			CyclicBarrier.allowTimeToPass();  // Do "work".
			Debug.println('C', "Thread " + NachosThread.currentThread().name + " is waiting at the barrier");
			    try {
				barrier.await();
			    } catch (BrokenBarrierException e) {
				// Barrier has been broken
				e.printStackTrace();
			    }
			Debug.println('C', "Thread " + NachosThread.currentThread().name + " has finished");
			Debug.println('C', "Thread " + NachosThread.currentThread().name + " is terminating");
			Nachos.scheduler.finishThread();
		    }
		});
	    Nachos.scheduler.readyToRun(thread);
	}
	Debug.println('C', "Demo terminating");
    }
    
    /**
     * This demo shows the execution of the reset method on the CyclicBarrier. 
     * Once a thread calls the reset method, it will terminate.
     * Any threads following this thread will have a BrokenBarrier 
     * 
     */
    public static void demo2(){
	final CyclicBarrier barrier = new CyclicBarrier(4);
	Debug.println('C', "CyclicBarrier Demo starting");
	
	//Zero thread
	NachosThread thread = new NachosThread("Worker thread " + 0,
		new Runnable() {
		    public void run() {
			Debug.println('C',
				"Thread " + NachosThread.currentThread().name
					+ " is starting");
			Debug.println('C',
				"Thread " + NachosThread.currentThread().name
					+ " is working");
			CyclicBarrier.allowTimeToPass(); // Do "work".

			Debug.println('C',
				"Thread " + NachosThread.currentThread().name
					+ " is waiting at the barrier");
			try {
			    barrier.await();
			} catch (BrokenBarrierException e) {
			    // Barrier has been broken
			    e.printStackTrace();
			}

			Debug.println('C',
				"Thread " + NachosThread.currentThread().name
					+ " is terminating");

			Nachos.scheduler.finishThread();
		    }
		});
	Nachos.scheduler.readyToRun(thread);
	
	//First thread
	thread = new NachosThread("Worker thread " + 1,
		new Runnable() {
		    public void run() {
			Debug.println('C',
				"Thread " + NachosThread.currentThread().name
					+ " is starting");
			Debug.println('C',
				"Thread " + NachosThread.currentThread().name
					+ " is working");
			CyclicBarrier.allowTimeToPass(); // Do "work".

			Debug.println('C',
				"Thread " + NachosThread.currentThread().name
					+ " is waiting at the barrier");
			try {
			    barrier.await();
			} catch (BrokenBarrierException e) {
			    // Barrier has been broken
			    e.printStackTrace();
			}

			Debug.println('C',
				"Thread " + NachosThread.currentThread().name
					+ " is terminating");

			Nachos.scheduler.finishThread();
		    }
		});
	Nachos.scheduler.readyToRun(thread);
	
	//Second thread
	thread = new NachosThread("Worker thread " + 2,
		new Runnable() {
		    public void run() {
			Debug.println('C',
				"Thread " + NachosThread.currentThread().name
					+ " is starting");
			Debug.println('C',
				"Thread " + NachosThread.currentThread().name
					+ " is working");
			CyclicBarrier.allowTimeToPass(); // Do "work".

			Debug.println('C',
				"Thread " + NachosThread.currentThread().name
					+ " is waiting at the barrier");
//			try {
			    barrier.reset();
//			    barrier.await();
//			} catch (BrokenBarrierException e) {
//			    // Barrier has been broken
//			    e.printStackTrace();
//			}

			Debug.println('C',
				"Thread " + NachosThread.currentThread().name
					+ " is terminating");

			Nachos.scheduler.finishThread();
		    }
		});
	Nachos.scheduler.readyToRun(thread);
	
	//third thread
	thread = new NachosThread("Worker thread " + 3,
		new Runnable() {
		    public void run() {
			Debug.println('C',
				"Thread " + NachosThread.currentThread().name
					+ " is starting");
			Debug.println('C',
				"Thread " + NachosThread.currentThread().name
					+ " is working");
			CyclicBarrier.allowTimeToPass(); // Do "work".

			Debug.println('C',
				"Thread " + NachosThread.currentThread().name
					+ " is waiting at the barrier");
			try {
//			    barrier.reset();
			    barrier.await();
			} catch (BrokenBarrierException e) {
			    // Barrier has been broken
			    e.printStackTrace();
			}

			Debug.println('C',
				"Thread " + NachosThread.currentThread().name
					+ " is terminating");

			Nachos.scheduler.finishThread();
		    }
		});
	Nachos.scheduler.readyToRun(thread);
	
	Debug.println('C', "Demo terminating");
	
	
    }
}

