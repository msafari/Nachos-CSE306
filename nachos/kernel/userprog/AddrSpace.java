// AddrSpace.java
//	Class to manage address spaces (executing user programs).
//
//	In order to run a user program, you must:
//
//	1. link with the -N -T 0 option 
//	2. run coff2noff to convert the object file to Nachos format
//		(Nachos object code format is essentially just a simpler
//		version of the UNIX executable object code format)
//	3. load the NOFF file into the Nachos file system
//		(if you haven't implemented the file system yet, you
//		don't need to do this last step)
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.userprog;

import java.util.ArrayList;
import java.util.List;

import nachos.Debug;
import nachos.machine.CPU;
import nachos.machine.MIPS;
import nachos.machine.Machine;
import nachos.machine.MachineException;
import nachos.machine.NachosThread;
import nachos.machine.Simulation;
import nachos.machine.TranslationEntry;
import nachos.noff.NoffHeader;
import nachos.noff.NoffHeader.NoffSegment;
import nachos.kernel.filesys.OpenFile;
import nachos.kernel.filesys.OpenFileEntry;

/**
 * This class manages "address spaces", which are the contexts in which
 * user programs execute.  For now, an address space contains a
 * "segment descriptor", which describes the the virtual-to-physical
 * address mapping that is to be used when the user program is executing.
 * As you implement more of Nachos, it will probably be necessary to add
 * other fields to this class to keep track of things like open files,
 * network connections, etc., in use by a user program.
 *
 * NOTE: Most of what is in currently this class assumes that just one user
 * program at a time will be executing.  You will have to rewrite this
 * code so that it is suitable for multiprogramming.
 * 
 * @author Thomas Anderson (UC Berkeley), original C++ version
 * @author Peter Druschel (Rice University), Java translation
 * @author Eugene W. Stark (Stony Brook University)
 */
public class AddrSpace {

  /** Page table that describes a virtual-to-physical address mapping. */
  public TranslationEntry pageTable[];

  /** Default size of the user stack area -- increase this as necessary! */
  private static final int UserStackSize = 1024;
  
  private static final long LOW32BITS = 0x00000000ffffffffL;
  
  public int nextVPN;
  
  private int numPages;
  private long sharedSize;

  /**
   * Create a new address space.
   */
  public AddrSpace() { 
      nextVPN = 0;
  }

  /**
   * Load the program from a file "executable", and set everything
   * up so that we can start executing user instructions.
   *
   * Assumes that the object code file is in NOFF format.
   *
   * First, set up the translation from program memory to physical 
   * memory.  For now, this is really simple (1:1), since we are
   * only uniprogramming.
   *
   * @param executable The file containing the object code to 
   * 	load into memory
   * @return -1 if an error occurs while reading the object file,
   *    otherwise 0.
   */
  public int exec(OpenFile executable) {
    NoffHeader noffH;
    long size;
    
    if((noffH = NoffHeader.readHeader(executable)) == null){
	Debug.println('M', "Executable header is empty");
	return(-1);
    }

    // how big is address space?
    sharedSize = roundToPage(noffH.code.size)		//need this for clone
	    	+ roundToPage(noffH.initData.size + noffH.uninitData.size);
    size = sharedSize  + UserStackSize;	// we need to increase the size
    				// to leave room for the stack
    numPages = (int)(size / Machine.PageSize);

    Debug.ASSERT((numPages <= Machine.NumPhysPages),// check we're not trying
		 "AddrSpace constructor: Not enough memory!");
                                                // to run anything too big --
						// at least until we have
						// virtual memory

    Debug.println('M', "Initializing address space, numPages=" 
		+ numPages + ", size=" + size);

    // first, set up the translation 
    pageTable = new TranslationEntry[numPages];
    
    //initializing page tables
    for (int i = 0; i < numPages; i++) {
      pageTable[i] = new TranslationEntry();
      pageTable[i].virtualPage = i; 	
      pageTable[i].physicalPage = -1; 	//these will get over written later in malloc
      pageTable[i].valid = false;
      pageTable[i].use = false;
      pageTable[i].dirty = false;
      pageTable[i].readOnly = false;  // if code and data segments live on separate pages, we could set code pages to be read-only
    }
    
    // Zero out the entire address space, to zero the uninitialized data 
    // segment and the stack segment.

    // then, copy in the code and data segments into memory
//    if (noffH.code.size > 0) {
//      Debug.println('M', "Initializing code segment, at " + noffH.code.virtualAddr + ", size " + noffH.code.size);
//      
//      malloc(noffH.code, executable, true);
//    }
//
//    if (noffH.initData.size > 0) {
//      Debug.println('M', "Initializing data segment, at " + noffH.initData.virtualAddr + ", size " + noffH.initData.size);
//      malloc(noffH.initData, executable, false);
//    }
//    
//    if(noffH.uninitData.size > 0) {
//	Debug.println('M', "Initializing uninitialized data segment, at " + noffH.uninitData.virtualAddr + ", size " + noffH.uninitData.size);
//	malloc(noffH.uninitData, executable, false);
//    }
//    
//
//    //Print out pages for debug
//    for(int i = 0; i < pageTable.length; i++){
//	Debug.println('M', "Entry: " + i + ", vpn: " + pageTable[i].virtualPage 
//					+ ", ppn: " + pageTable[i].physicalPage
//					+ ", valid: " + pageTable[i].valid);
//    }
//    
//    //allocate space for the stack 
//    mallocStack(pageTable, this);
    
    return(0);
  }


  /**
   * Initialize the user-level register set to values appropriate for
   * starting execution of a user program loaded in this address space.
   *
   * We write these directly into the "machine" registers, so
   * that we can immediately jump to user code.
   */
  public void initRegisters() {
    int i;
   
    for (i = 0; i < MIPS.NumTotalRegs; i++)
      CPU.writeRegister(i, 0);

    // Initial program counter -- must be location of "Start"
    CPU.writeRegister(MIPS.PCReg, 0);	

    // Need to also tell MIPS where next instruction is, because
    // of branch delay possibility
    CPU.writeRegister(MIPS.NextPCReg, 4);

    // Set the stack register to the end of the segment.
    // NOTE: Nachos traditionally subtracted 16 bytes here,
    // but that turns out to be to accomodate compiler convention that
    // assumes space in the current frame to save four argument registers.
    // That code rightly belongs in start.s and has been moved there.
    int sp = pageTable.length * Machine.PageSize;
    CPU.writeRegister(MIPS.StackReg, sp);
    Debug.println('a', "Initializing stack register to " + sp);
  }

  /**
   * On a context switch, save any machine state, specific
   * to this address space, that needs saving.
   *
   * For now, nothing!
   */
  public void saveState() {}

  /**
   * On a context switch, restore any machine state specific
   * to this address space.
   *, 
   * For now, just tell the machine where to find the page table.
   */
  public void restoreState() {
    CPU.setPageTable(pageTable);
  }
  
  
  /**
   * Utility method for rounding up to a multiple of CPU.PageSize;
   */
  public long roundToPage(long size) {
    return(Machine.PageSize * ((size+(Machine.PageSize-1))/Machine.PageSize));
  }

  
  /**
   * 
   * @param bufferAddr address of virtual memory to start writing to
   * @param data byte array to be written in virtual memory
   * @return the number of bytes written
   */
  public int writeToVirtualMem(int bufferAddr,  byte[] data, int startIndex, boolean isEntryVPN, TranslationEntry pageTable[], int length){
	
	int vOffset = (int) ((bufferAddr & LOW32BITS ) % Machine.PageSize);	//calculate virtual offset
	int vpn;
	
	if(isEntryVPN){
	    vpn = bufferAddr;
	}
	else {
	    vpn = (int) ((bufferAddr & LOW32BITS) / Machine.PageSize);	//calculate virtual page number 
	}
	
	//check for page faults
	if (vpn >= Machine.PageSize) {
  	    Debug.println('a', "virtual page # " + vpn + 
  			  " too large for page table size " + Machine.PageSize);
  	} else if (!pageTable[(int)vpn].valid) {
  	    Debug.println('a', "virtual page # " + vpn + " not valid");
  	}
  	TranslationEntry entry = pageTable[(int)vpn];
      
	entry.use = true;				//set use flag of entry to true
	int pAddr = (entry.physicalPage * Machine.PageSize) + vOffset; 		//entry.physicalPage is ppn or frame number
										//paddr = ppn * pagesize + offset  basically calculating PFN::offset
	
	System.arraycopy(data, startIndex, Machine.mainMemory, pAddr, length);	//copy data to main mem starting from the physical address calculated above
	return data.length;
  }
  
  /**
   * 
   * @param bufferAddr
   * @return
   */
  public TranslationEntry getEntry(int bufferAddr) {
      int VPN = (int) ((bufferAddr & LOW32BITS) / Machine.PageSize);	//calculate virtual page number
      TranslationEntry entry = pageTable[VPN];	//get the page table
      return entry;
  }
  
  
  /**
   * allocate more memory for UserThread
   * @return
   */
  protected int malloc(NoffSegment segment, OpenFile executable, boolean readOnly) {
     
      
      if(numPages <= Machine.NumPhysPages && numPages<=MemoryManager.freePagesList.size()){
	  
	    long size = roundToPage(segment.size);
	    int numSegmentPages = (int)(size / Machine.PageSize);
	    
	    byte[] data = new byte[(int)size]; 		//buffer to store segment data in
	    executable.seek(segment.inFileAddr);
	    executable.read(data, 0, segment.size);	//read the entire segment into a buffer
	    
	    TranslationEntry entry;
	    
	    for(int i = 0; i < numSegmentPages; i++){
		// Get the vpn and entry
		
		int startIndex = i * Machine.PageSize;
		int bufferAddr = segment.virtualAddr + startIndex;
		
		entry = getEntry(bufferAddr);
		
		// Allocate some pages
		MemoryManager.freePagesLock.acquire();
		int freePageAddr= MemoryManager.freePagesList.removeFirst();
		MemoryManager.freePagesLock.release();
		entry.physicalPage = freePageAddr;
		entry.valid = true;
		entry.readOnly = readOnly;
		
		writeToVirtualMem(bufferAddr, data, startIndex, false, pageTable, Machine.PageSize);
		nextVPN++;
	    }
	  

	  return 0;
      }
      
      else{
	  Debug.println('M', "Not enough physical memory!");
	  return -1;
      }
      
  }
  
  /**
   * find segment(s) the segment that the page fault is happening at
   * the noff header has the base of the virtual address and the we have the size of each segment
   * so just check if the pageNumber (which is essentially a virtual address) falls in between the base vaddr of the segment and vaddr + numOfPages in segment
   *  
   * @return a list of segments:  returning a list in case the page fault is happening at a page boundary.
   */
  private List<NoffSegment> findSegmentsAtPageFault (int pageNumber, NoffHeader noffH) {
      List<NoffSegment> segmentsToAllocate = new ArrayList<NoffSegment>();
      
      NoffSegment[] segments = {noffH.code, noffH.initData, noffH.uninitData};
      
      for(int i=0; i< segments.length; i++) {
	  int baseVpn = (int )(segments[i].virtualAddr & LOW32BITS) / Machine.PageSize;;		//base virtual address of the segment
	  long size = roundToPage(segments[i].size);	// number of pages in the segment
	  int numOfPages = (int) (size/Machine.PageSize);
	  
	  if(pageNumber >= baseVpn && pageNumber < baseVpn + numOfPages) {
	      segmentsToAllocate.add(segments[i]); // add the segment to list that needs to be allocated
	      
	      // check if it's the last page in segment and also check
	      //if segment size is not a multiple of page size then it's at a page boundary
	      if( segments[i].size % Machine.PageSize != 0 && 
		      pageNumber == (baseVpn + numOfPages -1)){
		  //since it's at a page boundary we also need to allocate the next segment
		  if(segments[i+1] != null) {
		      segmentsToAllocate.add(segments[i + 1]);	//so add the next segment to the list
		  }
	      }
	      
	      break;
		  
	  }
      }
      
      return segmentsToAllocate;
  }
  
  
  /**
   * dynamically allocate more pages for a segment
   * faults on different address space segments are handled in different ways. 
   * For example, a fault on a code page should read the corresponding code from the executable file, 
   * a fault on a data page should read the corresponding data from the executable file, 
   * and a fault on a stack frame should zero-fill the frame.
   * 
   * check for page boundaries and load both segments if that's the case
   * @param pageNumber: page number the fault occurred at
   * @param executable: the OpenFile Executable
   * @return
   */
  public int demandMalloc (int virtAddr, OpenFile executable) {
      int vpn = (int)(virtAddr & LOW32BITS) / Machine.PageSize;
      NoffHeader noffH;
      executable.seek(0);
      if((noffH = NoffHeader.readHeader(executable)) == null){
  	Debug.println('M', "Executable header is empty");
  	return(-1);
      }
      
      //get the segment(s) it's happening at
      List<NoffSegment> segments = findSegmentsAtPageFault(vpn, noffH);
      
      
      // If vpn exceeds the pageTable, need to extend it
      allocatePageTableEntry(vpn);

      // Read in the page from the disk
      byte[] buf = new byte[Machine.PageSize];

      // TODO Maybe ? handle the case if unitData is in the segments list
      // no need to read in the unitData just zero fill

      if (segments.size() == 1) {
	  executable.seek(segments.get(0).inFileAddr + vpn * Machine.PageSize);
	  executable.read(buf, 0, Machine.PageSize);
      }

      else if (segments.size() == 2) {
	  int firstSegmentNumBytes = segments.get(0).size - vpn * Machine.PageSize;
	  executable.seek(segments.get(0).inFileAddr + vpn * Machine.PageSize);
	  executable.read(buf, 0, firstSegmentNumBytes);

	  // Write to main memory
	  int offset = (int) (virtAddr & LOW32BITS) % Machine.PageSize;
	  int pAddr = pageTable[vpn].physicalPage * Machine.PageSize;
	  System.arraycopy(buf, 0, Machine.mainMemory, pAddr, Machine.PageSize);

	  // Get a free page for the next segment because this is a page boundary and we round the segments to page size
	  vpn += 1;
	  allocatePageTableEntry(vpn);
	  buf = new byte[Machine.PageSize];
	  executable.seek(segments.get(1).inFileAddr);
	  executable.read(buf, 0, Machine.PageSize - firstSegmentNumBytes);

	  // Write the next segment to main memory
	  offset = (int) (virtAddr & LOW32BITS) % Machine.PageSize;
	  pAddr = pageTable[vpn].physicalPage * Machine.PageSize;
	  System.arraycopy(buf, 0, Machine.mainMemory, pAddr, Machine.PageSize);
	  return vpn;
      }

      // Write to main memory
      //int offset = (int) (virtAddr & LOW32BITS) % Machine.PageSize;
      int pAddr = pageTable[vpn].physicalPage * Machine.PageSize;
      System.arraycopy(buf, 0, Machine.mainMemory, pAddr, Machine.PageSize);

      return vpn;
  }
  
  /**
   * 
   * @param vpn
   */
  private void allocatePageTableEntry (int vpn) {
	if (vpn >= numPages) {
	    int newPages = vpn - numPages - 1;
	    
	    TranslationEntry newPageTable[] = new TranslationEntry[newPages];
	    System.arraycopy(pageTable, 0, newPageTable, 0, numPages);
	    pageTable = newPageTable;
	    numPages += newPages;
	    
	}

	//Get a free page for this address
	MemoryManager.freePagesLock.acquire();
	int freePageAddr= MemoryManager.freePagesList.removeFirst();
	MemoryManager.freePagesLock.release();
	
	//Mark it as valid
	pageTable[vpn].virtualPage = vpn;
	pageTable[vpn].physicalPage = freePageAddr;
	pageTable[vpn].valid = true;
	pageTable[vpn].use = true;
	pageTable[vpn].dirty = false;
	pageTable[vpn].readOnly = false;
  }
  
  
  /**
   * 
   */
  protected int mallocStack(TranslationEntry pageTable[], AddrSpace space){
      Debug.println('M', "Allocating Space for stack");
      int numStackPages = UserStackSize / Machine.PageSize;
      if(numPages <= Machine.NumPhysPages && numPages<=MemoryManager.freePagesList.size()) {
          for (int i = 0; i < numStackPages; i++) {
    	  TranslationEntry entry = pageTable[space.nextVPN];
    	  
    	  // Allocate some pages
    	  MemoryManager.freePagesLock.acquire();
    	  int freePageIndex = MemoryManager.freePagesList.removeFirst();
    	  MemoryManager.freePagesLock.release();
    	  entry.physicalPage = freePageIndex;
    	  entry.valid = true;
    	  entry.use= true;
    	  Debug.println('M', "Stack Entry: " + i + ", vpn: " + entry.virtualPage 
    			+ ", ppn: " + entry.physicalPage
    			+ ", valid: " + entry.valid);
    	  space.nextVPN++;
    	  
          }
      }
      else {
	  Debug.println('M', "Not enough physical memory!");
	  return -1;
      }
      return 0;
  }
  
  /**
   * free all resources for the current thread
   */
  protected int free() {
      Debug.println('+', "freeing all resources for thread: " + ((UserThread)NachosThread.currentThread()).name);
      try {
	  for (int i=0; i< pageTable.length ; i++) {
		TranslationEntry entry = pageTable[i];
		if (entry.valid) {
		    MemoryManager.freePagesLock.acquire();
		    MemoryManager.freePagesList.add(entry.physicalPage);
		    MemoryManager.freePagesLock.release();
		}
	    }
	  return 0;
      } catch(Exception e) {
	  Debug.println('M', "Freeing memory failed!");
	  return -1;
      }
      
  }
  
 /**
  * 
  * @param virtAddr
  * @param size
  * @param writing
  * @return
  */
 public int translate(int virtAddr, int size, boolean writing){
      int i = 0;
      int physAddr;

      // check for alignment errors
      if (((size == 4) && (virtAddr & 0x3) != 0) || 
  	((size == 2) && (virtAddr & 0x1) != 0)) {
        Debug.println('a', "alignment problem at " + virtAddr + ", size " + size);
        return -1;
      }

      // Translation using page table
      long vpn, offset;
      TranslationEntry entry;
      long pageFrame;
      
      // calculate the virtual page number, and offset within the page,
      // from the virtual address
      vpn = (virtAddr & LOW32BITS) / Machine.PageSize;
      offset = (virtAddr & LOW32BITS) % Machine.PageSize;

	if (vpn >= pageTable.length) {
	    Debug.println('a', "virtual page # " + vpn
		    + " too large for page table size " + pageTable.length);
	    return -1;
	} else if (!pageTable[(int) vpn].valid) {
	    Debug.println('a', "virtual page # " + vpn + " not valid");
	    UserThread curUserThrd = ((UserThread)NachosThread.currentThread());
	    OpenFileEntry fileEntry = Syscall.findOpenFileEntry(curUserThrd.filename);
	    demandMalloc(virtAddr, fileEntry.file);
	    Simulation.stats.numPageFaults++;
//	    return -1;
	}
     entry = pageTable[(int)vpn];

      if (entry.readOnly && writing) {	// trying to write to a read-only page
        Debug.println('a', virtAddr + " mapped read-only at " + i + " in TLB!");
        return -1;
      }
      pageFrame = entry.physicalPage;

      // if the pageFrame is too big, there is something really wrong! 
      // An invalid translation was loaded into the page table or TLB. 
      if (pageFrame >= Machine.NumPhysPages) { 
        Debug.println('a', "*** frame " + pageFrame + " > " + Machine.NumPhysPages);
        return -1;
      }
      entry.use = true;		// set the use, dirty bits
      if (writing)
  	entry.dirty = true;
      physAddr = (int) (pageFrame * Machine.PageSize + offset);

      Debug.ASSERT((physAddr >= 0) && ((physAddr + size) <= Machine.MemorySize));
      if (Debug.isEnabled('a')) {
        Debug.printf('a', "phys addr = 0x%x\n", new Integer(physAddr));
      }

      return physAddr;
    }
 
    public int readVirtualMemory(int virtualAddress, byte[] data, int offset, int length, boolean isEntryVPN) {

	byte[] memory = Machine.mainMemory;

	int vpn;
	// address translationTranslationEntry page
	if(isEntryVPN) {
	    vpn = virtualAddress;
	}
	else {
	    vpn = virtualAddress / Machine.PageSize;   
	}
	
	int voffset = virtualAddress % Machine.PageSize;
	TranslationEntry entry = pageTable[vpn];
	entry.use = true;
	int physicalAddress = entry.physicalPage * Machine.PageSize + voffset;

	// if entry is not valid, then don't read
	if (physicalAddress < 0 || physicalAddress >= memory.length || !entry.valid)
	    return 0;

	int amount = Math.min(length, memory.length - physicalAddress);

	// copies 'amount' bytes from byte array 'memory' starting at byte 'vaddr' to byte array 'data' starting 'offset' bytes into 'data'
	System.arraycopy(memory, physicalAddress, data, offset, amount);

	return amount;
    }
    
    /**
     * Clone will make a new addrspace
     * it will allocate memory for each pagetable
     * it will copy over the original addrspace's code, data, and uninitdata segement
     * 
     */
    public AddrSpace clone() {
	AddrSpace newSpace = new AddrSpace();
	newSpace.pageTable = new TranslationEntry[numPages];
	int sharedPages = (int)sharedSize / Machine.PageSize;
	
	//initializing page tables
	for (int i = 0; i < numPages; i++) {
	    newSpace.pageTable[i] = new TranslationEntry();
	    newSpace.pageTable[i].virtualPage = i; 	
	    newSpace.pageTable[i].physicalPage = -1; 	//these will get over written later after mem allocation
	    newSpace.pageTable[i].valid = false;
	    newSpace.pageTable[i].use = false;
	    newSpace.pageTable[i].dirty = false;
	    newSpace.pageTable[i].readOnly = false;  // if code and data segments live on separate pages, we could set code pages to be read-only
	}
	
	byte[] tmpBuffer = new byte[Machine.PageSize];
	 
	for(int i = 0; i <sharedPages; i++) {
	    // Allocate some pages
	    MemoryManager.freePagesLock.acquire();
	    int freePageAddr= MemoryManager.freePagesList.removeFirst();
	    MemoryManager.freePagesLock.release();
	    newSpace.pageTable[i].physicalPage = freePageAddr;
	    newSpace.pageTable[i].valid = true;
	    newSpace.pageTable[i].readOnly = this.pageTable[i].readOnly;
	    
	   readVirtualMemory(this.pageTable[i].virtualPage, tmpBuffer, 0, Machine.PageSize, true);	//read in a page to tmpBuffer
	   writeToVirtualMem(newSpace.pageTable[i].virtualPage, tmpBuffer, 0, true, newSpace.pageTable, Machine.PageSize);			//write tmpBuffer's data to mem
	   
	    //Print out pages for debug
	    Debug.println('M', "Entry: " + i + ", vpn: "
		    + newSpace.pageTable[i].virtualPage + ", ppn: "
		    + newSpace.pageTable[i].physicalPage + ", valid: "
		    + newSpace.pageTable[i].valid);
	    
	    newSpace.nextVPN++; 
	    
	}
	
	//allocate space for the new space's userstack
	mallocStack(newSpace.pageTable, newSpace);
	
	return newSpace;
	
    }
    
    /**
     * Extends the address space by the file size. Used by mmap syscall.
     * @param size
     * @return  the size of the newly allocated region of address space
     */
    public int extend(long size){
	int n = (int) roundToPage(size);	//number of pages to extend by
	int N = n / Machine.PageSize;
	int oldPageTableLength = pageTable.length;
	
	int newTotalPages = N + oldPageTableLength;
	    
	TranslationEntry newPageTable[] = new TranslationEntry[newTotalPages];
	System.arraycopy(pageTable, 0, newPageTable, 0, numPages);
	pageTable = newPageTable;
	numPages += newTotalPages;
	    
	
	for(int i = oldPageTableLength; i < newTotalPages; i++ ) {
	    pageTable[i] = new TranslationEntry();
	    pageTable[i].virtualPage = i;
	    pageTable[i].physicalPage = -1; // these will get over written later
					    // in malloc
	    pageTable[i].valid = false;
	    pageTable[i].use = false;
	    pageTable[i].dirty = false;
	    pageTable[i].readOnly = false; // if code and data segments live on
					   // separate pages, we could set code
					   // pages to be read-only
	}
	
	return N;	
    }
 
}
