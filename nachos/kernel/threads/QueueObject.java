package nachos.kernel.threads;

import nachos.machine.NachosThread;

/**
 * Object to be placed in a MLF queue.
 *
 */
public class QueueObject {

    public NachosThread thread;
    
    public int currentPLevelIndex;
    
    public int sampleVal;
    
    public double avgCPUBurst;
    
    public int numInterrupts;

    public String objectName;
    
    public QueueObject(NachosThread thread){
	this.thread = thread;
	currentPLevelIndex = -1;
	sampleVal = 0;
	avgCPUBurst = 0;
	objectName = thread.name;
	numInterrupts = 1;
    }
}
