package nachos.kernel.userprog;

import java.util.LinkedList;
import nachos.Debug;
import nachos.kernel.threads.Lock;
import nachos.machine.*;

public class MemoryManager {
    
    public static int processID; // Every time a new thread is created, give it a new processID
    public static LinkedList<Integer> freePagesList = new LinkedList<Integer>(); //List to keep track of free pages
    public static Lock processIDLock;
    public static Lock freePagesLock;
    
    public MemoryManager(){
	processIDLock = new Lock("processIDLock");
	freePagesLock = new Lock("freePagesLock");
	
	//add all physical pages to freePages linked list
	for (int i=0; i < Machine.NumPhysPages; i++) {
	    freePagesList.add(i);
	}
	
	Debug.println('M', "Creating Memory Manager. Size: " + freePagesList.size());
	
    }
}
