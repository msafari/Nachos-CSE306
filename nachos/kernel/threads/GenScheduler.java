/**
 * 
 */
package nachos.kernel.threads;

import java.util.LinkedList;

import nachos.Debug;
import nachos.kernel.Nachos;
import nachos.kernel.userprog.UserThread;
import nachos.machine.CPU;
import nachos.machine.InterruptHandler;
import nachos.machine.Machine;
import nachos.machine.NachosThread;
import nachos.machine.Timer;
import nachos.util.FIFOQueue;
import nachos.util.Queue;

/**
 * Abstract class for the different schedulers
 * @author maedeh
 *
 */
public abstract class GenScheduler {
    
    /** Queue of threads that are ready to run, but not running. */
    private final Queue<NachosThread> readyList;

    /** Queue of threads that are sleeping. */
    public LinkedList<NachosThread> sleepList;

    /**
     * Initialize the scheduler.
     * Set the list of ready but not running threads to empty.
     * Initialize the list of CPUs to contain all the available CPUs.
     * 
     * @param firstThread  The first NachosThread to run.
     */
    public GenScheduler(NachosThread firstThread) {
	readyList = new FIFOQueue<NachosThread>();
	sleepList = new LinkedList<NachosThread>();
    };

    /**
     * Stop the timers on all CPUs, in preparation for shutdown.
     */
    public void stop() {
    }

    /**
     * Mark a thread as ready, but not running, and put it on the ready list
     * for later scheduling onto a CPU.
     * If there are idle CPUs then threads are dispatched onto CPUs until either
     * all CPUs are in use or there are no more threads are ready to run.
     * 
     * It is assumed that multiple concurrent calls of this method will not be
     * made with the same thread as parameter, as that could result in the thread
     * being added more than once to the ready queue, in addition to introducing
     * the possibility of races in the setting of the thread status.
     *
     * @param thread The thread to be put on the ready list.
     */
    public void readyToRun(NachosThread thread) {
    }

    /**
     * Relinquish the CPU if any other thread is ready to run.
     * If so, put the thread on the end of the ready list, so that
     * it will eventually be re-scheduled.
     *
     * NOTE: returns immediately if no other thread on the ready queue.
     * Otherwise returns when the thread eventually works its way
     * to the front of the ready list and gets re-scheduled.
     *
     * NOTE: we disable interrupts and acquire the scheduler mutex,
     * so that looking at the thread on the front of the ready list,
     * and switching to it, can be done atomically.  On return, we release
     * the scheduler mutex and re-set the interrupt level to its
     * original state.  This means this method will work properly no
     * matter whether interrupts are enabled or disabled when it is called,
     * but it should never be called with the scheduler mutex already locked.
     *
     * Similar to sleep(), but a little different.
     */
    public void yieldThread () {
    }

    /**
     * Relinquish the CPU, because the current thread is going to block
     * (i.e. either wait on a synchronization variable or, if the thread is finishing,
     * await final destruction).  If the thread is not finishing, then arrangements
     * should have been made for this thread to eventually be placed back on the ready
     * list, so that it can be re-scheduled.  If the thread is finishing, then
     * it will remain blocked until it is destroyed.
     *
     * NOTE: this method assumes interrupts are disabled, so that there can't be a time
     * slice between pulling the first thread off the ready list, and switching to it.
     * 
     * @param  toRelease  A spinlock held by the caller that is to be released atomically
     * with relinquishing the CPU.
     */
    public void sleepThread (SpinLock toRelease) {
    }

    /**
     * Called by a thread to terminate itself.
     * A thread can't completely destroy itself, because it needs some
     * resources (e.g. a stack) as long as it is running.  So it is the
     * responsibility of the next thread to run to finish the job.
     */
    public void finishThread() {

    }

    /**
     * Returns a queue object for a given thread. To be used in the MLF scheduler.
     * @return
     */
    public QueueObject findQueueObject(NachosThread thread){
	return null;
    }

    

}
