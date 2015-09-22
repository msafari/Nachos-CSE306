package nachos.kernel.threads;

import nachos.Debug;
import nachos.machine.NachosThread;

/**
 * This class provides a facility for scheduling work to be performed
 * "in the background" by "child" threads and safely communicating the
 * results back to a "parent" thread.  It is loosely modeled after the
 * AsyncTask facility provided in the Android API.
 *
 * The class is provided in skeletal form.  It is your job to
 * complete the implementation of this class in conformance with the
 * Javadoc specifications provided for each method.  The method
 * signatures specified below must not be changed.  However, the
 * implementation will likely require additional private methods as well
 * as (private) instance variables.  Also, it is essential to provide
 * proper synchronization for any data that can be accessed concurrently.
 * You may use any combination of semaphores, locks, and conditions
 * for this purpose.
 *
 * NOTE: You may NOT disable interrupts or use spinlocks.
 */
public class TaskManager {
    
    /**
     * Initialize a new TaskManager object, and register the
     * calling thread as the "parent" thread.  The parent thread is
     * responsible (after creating at least one Task object and
     * calling its execute() method) for calling processRequests() to
     * track the completion of "child" threads and process onCompletion()
     * or onCancellation() requests on their behalf.
     */
    public TaskManager() {
    }
    
    /**
     * Posts a request for a Runnable to be executed by the parent thread.
     * Such a Runnable might consist of a call to <CODE>onCompletion()</CODE>
     * or <CODE>onCancellation() associated with the completion of a task
     * being performed by a child thread, or it might consist of
     * completely unrelated work (such as responding to user interface
     * events) for the parent thread to perform.
     * 
     * NOTE: This method should be safely callable by any thread.
     *
     * @param runnable  Runnable to be executed by the parent thread.
     */
    public void postRequest(Runnable runnable) {
    }

    /**
     * Called by the parent thread to process work requests posted
     * for it.  This method does not return unless there are no
     * further pending requests to be processed AND all child threads
     * have terminated.  If there are no requests to be processed,
     * but some child threads are still active, then the parent thread
     * will block within this method awaiting further requests
     * (for example, those that will eventually result from the termination
     * of the currently active child threads).
     *
     * @throws IllegalStateException  if the calling thread is not
     * registered as the parent thread for this TaskManager.
     */
    public void processRequests() throws IllegalStateException {
    }

    /**
     * Inner class representing a task to be executed in the background
     * by a child thread.  This class must be subclassed in order to
     * override the doInBackground() method and possibly also the
     * onCompletion() and onCancellation() methods.
     */
    public class Task {
	
	/**
	 * Cause the current task to be executed by a new child thread.
	 * In more detail, a new child thread is created, the child
	 * thread runs the doInBackground() method and upon termination
	 * of that method a request is posted for the parent thread to
	 * run either onCancellation() or onCompletion(), respectively,
	 * depending on	whether or not the task was cancelled.
	 */
	public void execute() {
	}

	/**
	 * Flag the current Task as "cancelled", if the task has not
	 * already completed.  Successful cancellation (as indicated
	 * by a return value of true) guarantees that the onCancellation()
	 * method will be executed instead of the normal onCompletion()
	 * method.  This method should be safely callable by any thread.
	 *
	 * @return true if the task was successfully cancelled,
	 * otherwise false.
	 */
	public boolean cancel() {
	    //Remove this later
	    return false;
	}

	/**
	 * Determine whether this Task has been cancelled.
	 * This method should be safely callable by any thread.
	 *
	 * @return true if this Task has been cancelled, false otherwise.
	 */
	public boolean isCancelled() {
	    //Remove this later
	    return false;
	}

	/**
	 * Method to be executed in the background by a child thread.
	 * Subclasses will override this with desired code.  The default
	 * implementation is to do nothing and terminate immediately.
	 * Subclass implementations should call isCancelled() periodically
	 * so that this method will return promptly if this Task is
	 * cancelled.  This method should not be called directly;
	 * rather, it will be called indirectly as a result of a call to
	 * the execute() method.
	 */
	protected void doInBackground() {
	}

	/**
	 * Method to be executed by the main thread upon termination of
	 * of doInBackground().  Will not be executed if the task was
	 * cancelled.  This method should not be called directly; 
	 * rather, it will be called indirectly as a result of a call to
	 * the execute() method.
	 */
	protected void onCompletion() {
	}

	/**
	 * Method to be executed by the main thread upon termination
	 * of doInBackground().  Will only be executed if the task
	 * was cancelled.
	 */
	protected void onCancellation() {
	}
	
	/**
	 * This method can be called to simulate "doing work".
	 * Each time it is called it gives control to the NACHOS
	 * simulator so that the simulated time can advance by a
	 * few "ticks".
	 */
	protected void allowTimeToPass() {
	    dummy.P();
	    dummy.V();
	}

    }

    /** Semaphore used by allowTimeToPass above. */
    private static Semaphore dummy = new Semaphore("Time waster", 1);

    /**
     *  Run a demonstration of the TaskManager facility.
     *
     * @param args  Arguments from the "command line" that can be
     * used to modify features of the demonstration such as the
     * number of tasks to execute, the amount of "work" performed by
     * each task, etc.
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
	// Very simple example of the intended use of the TaskManager
	// facility: you should replace this code with something much
	// more interesting.
	TaskManager mgr = new TaskManager();
	Debug.println('T', "TaskManager Demo starting");
	for(int i = 0; i < 5; i++) {
	    final int tn = i;
	    Task task =
		mgr.new Task() {
		    protected void doInBackground() {
			Debug.println('1', "Thread "
				      + NachosThread.currentThread().name
				      + " is starting task " + tn);
			for(int j = 0; j < 10; j++) {
			    allowTimeToPass();   // Do "work".
			    Debug.println('T', "Thread "
					  + NachosThread.currentThread().name
					  + " is working on task " + tn);
			}
			Debug.println('T', "Thread "
				      + NachosThread.currentThread().name
				      + " is finishing task " + tn);
		    }

		    protected void onCompletion() {
			Debug.println('T', "Thread "
				      + NachosThread.currentThread().name
				      + " is executing onCompletion() "
				      + " for task " + tn);
		    }
		};
	    task.execute();
	}
	mgr.processRequests();
	Debug.println('T', "Demo terminating");
    }
}
