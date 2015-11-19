// Directory.java
//	Class to manage a directory of file names.
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and 
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.filesys;

import nachos.Debug;

/**
 * This class class defines a UNIX-like "directory".  Each entry in
 * the directory describes a file, and where to find it on disk.
 *
 * The directory is a table of fixed length entries; each
 * entry represents a single file, and contains the file name,
 * and the location of the file header on disk.  The fixed size
 * of each directory entry means that we have the restriction
 * of a fixed maximum size for file names.
 *
 * Also, this implementation has the restriction that the size
 * of the directory cannot expand.  In other words, once all the
 * entries in the directory are used, no more files can be created.
 * Fixing this is one of the parts to the assignment.
 *
 * The directory data structure can be stored in memory, or on disk.
 * When it is on disk, it is stored as a regular Nachos file.
 * The constructor initializes a directory structure in memory; the
 * fetchFrom/writeBack operations shuffle the directory information
 * from/to disk. 
 * 
 * We assume mutual exclusion is provided by the caller.
 * 
 * @author Thomas Anderson (UC Berkeley), original C++ version
 * @author Peter Druschel (Rice University), Java translation
 * @author Eugene W. Stark (Stony Brook University)
 */
class Directory {

    /** Number of entries in the directory. */
    private int tableSize;

    /** Table of pairs: file name/file header location. */
    private DirectoryEntry table[];
    

    /** The underlying filesystem in which the directory resides. */
    private final FileSystemReal filesystem;

    /**
     * Initialize a directory; initially, the directory is completely
     * empty.  If the disk is being formatted, an empty directory
     * is all we need, but otherwise, we need to call FetchFrom in order
     * to initialize it from disk.
     *
     * @param size The number of entries in the directory.
     * @param filesystem  The underlying filesystem in which this directory exists.
     */
    Directory(int size, FileSystemReal filesystem)
    {
	this.filesystem = filesystem;
	table = new DirectoryEntry[size];
	tableSize = size;
	for (int i = 0; i < tableSize; i++) {
	    table[i] = new DirectoryEntry();
	}
    }

    /**
     * Read the contents of the directory from disk.
     *
     * @param file The file containing the directory contents.
     */
    void fetchFrom(OpenFile file) {
	byte tableBuffer[] = new byte[4];
	file.readAt(tableBuffer, 0, 4, 0);
	tableSize = filesystem.bytesToInt(tableBuffer, 0);
	
	table = new DirectoryEntry[tableSize];
	for (int i=0; i< tableSize; i++) {
	    table[i] = new DirectoryEntry();
	}
	
	byte buffer[] = new byte[tableSize * DirectoryEntry.sizeOf()];
	file.readAt(buffer, 0, tableSize * DirectoryEntry.sizeOf(), 4);

	int pos = 0;
	for (int i = 0; i < tableSize; i++) {
	    table[i].internalize(buffer, pos);
	    pos += DirectoryEntry.sizeOf();
	}
    }

    /**
     * Write any modifications to the directory back to disk
     *
     * @param file The file to contain the new directory contents.
     */
    void writeBack(OpenFile file) {
	int dirSize = (tableSize * DirectoryEntry.sizeOf()) + 4;
	byte buffer[] = new byte[dirSize];
	
	FileSystem.intToBytes(tableSize, buffer, 0);
	int pos = 4;
	for (int i = 0; i < tableSize; i++) {
	    table[i].externalize(buffer, pos);
	    pos += DirectoryEntry.sizeOf();
	}
	
	file.writeAt(buffer, 0, dirSize, 0);
    }

    /**
     * Look up file name in directory, and return its location in the table of
     * directory entries.  Return -1 if the name isn't in the directory.
     *
     * @param name The file name to look up.
     * @return The index of the entry in the table, if present, otherwise -1.
     */
    private int findIndex(String name) {
	for (int i = 0; i < tableSize; i++) {
	    if (table[i].inUse() && name.equals(table[i].getName()))
		return i;
	}
	return -1;		// name not in directory
    }

    /**
     * Look up file name in directory, and return the disk sector number
     * where the file's header is stored. Return -1 if the name isn't 
     * in the directory.
     *
     * @param name The file name to look up.
     * @return The disk sector number where the file's header is stored,
     * if the entry was found, otherwise -1.
     */
    int find(String name) {
	int i = findIndex(name);

	if (i != -1)
	    return table[i].getSector();
	return -1;
    }
    
    /**
     * Validates the sectors allocated in this directory by comparing them to the freemap sectors.
     * @return
     */
    void validate() {
	BitMap freeMap = new BitMap(filesystem.numDiskSectors);
	FileHeader hdr;
	freeMap.fetchFrom(filesystem.freeMapFile);
	Directory dir;
	
	for (int i=0; i<table.length; i++) {
	    if(table[i].inUse()) {
		if(filesystem.diskSectors.test(table[i].getSector())) {
		    Debug.println('V', "Disk Sector " + table[i].getSector() + " is referenced by more than one header.");
		}
		else {
		    filesystem.diskSectors.mark(table[i].getSector());
		}
				
		if(!freeMap.test(table[i].getSector())){
		    Debug.println('V', "Sector " + table[i].getSector() + " is in use but not marked as used in BitMap.");
		}
		//Otherwise valid, check subdirectory/file
		else{
		    
		    //check subdirectories if entry is a directory itself
		    if (table[i].isDir() ) {
			OpenFileReal dirFile = new OpenFileReal(table[i].getSector(), filesystem);
			dir = new Directory(filesystem.NumDirEntries, filesystem);
			dir.fetchFrom(dirFile);
			
			//now validate this sub directory
			dir.validate();
		    }
		    else {
			hdr = new FileHeader(filesystem);
			hdr.fetchFrom(table[i].getSector());
			hdr.validate();
			
		    }
		}
	    }
	}
	
	
    }

    /**
     * Look up file name in directory, and return the disk sector number
     * where the file's header is stored. Return -1 if the name isn't 
     * in the directory.
     *
     * @param name The file name to look up.
     * @return The disk sector number where the file's header is stored,
     * if the entry was found, otherwise -1.
     */
    int findDirectory(String name) {
	int i = 0;
	for (i = 0; i < tableSize; i++) {
	    if (table[i].inUse() && name.equals(table[i].getName()) && table[i].isDir())
		return table[i].getSector();
	}
	    
	return -1;
    }

    /**
     * Add a file into the directory.  Return TRUE if successful;
     * return FALSE if the file name is already in the directory,
     * or if the directory is completely full, and has no more space for
     * additional file names, or if the file name cannot be represented
     * in the number of bytes available in a directory entry.
     *
     * @param name The name of the file being added.
     * @param newSector The disk sector containing the added file's header.
     * @return true if the file was successfully added, otherwise false.
     */
    boolean add(String name, int newSector) { 
	if (findIndex(name) != -1)
	    return false;

	for (int i = 0; i < tableSize; i++)
	    if (!table[i].inUse()) {
		if(!table[i].setUsed(name, newSector))
		    return(false);
		return(true);
	    }
	
	//extend the directory if full
	Debug.println('f', "Directory Full! Extending it...");
	
	DirectoryEntry[] extendedTable = new DirectoryEntry[tableSize + 1];
	System.arraycopy(table, 0, extendedTable, 0, tableSize);
	table = extendedTable;
	table[tableSize] = new DirectoryEntry();
	table[tableSize].setUsed(name, newSector);
	
	Debug.ASSERT(table[tableSize++].inUse());
	return true;	// no space.  Fix when we have extensible files.
    }
    
    /**
     * Add a child directory to this directory
     * @param name
     * @param newSector
     * @return
     */
    boolean addDirectory(String name, int newSector) { 
   	if (findDirectory(name) != -1)
   	    return false;

   	for (int i = 0; i < tableSize; i++)
   	    if (!table[i].inUse()) {
   		if(!table[i].setUsed(name, newSector))
   		    return(false);
   		table[i].setIsDir(true);
   		return(true);
   	    }
   	
   	//extend the directory if full
   	Debug.println('f', "Directory Full! Extending it...");
   	
   	DirectoryEntry[] extendedTable = new DirectoryEntry[tableSize + 1];
   	System.arraycopy(table, 0, extendedTable, 0, tableSize);
   	table = extendedTable;
   	table[tableSize] = new DirectoryEntry();
   	table[tableSize].setUsed(name, newSector);
   	table[tableSize].setIsDir(true);
   	
   	Debug.ASSERT(table[tableSize++].inUse());
   	return true;	// no space.  Fix when we have extensible files.
   }

    /**
     * Remove a file name from the directory.  Return TRUE if successful;
     * return FALSE if the file isn't in the directory. 
     *
     * @param name The file name to be removed.
     */
    boolean remove(String name) { 
	int i = findIndex(name);

	if (i == -1)
	    return false; 		// name not in directory
	table[i].setUnused();
	table[i].setIsDir(false);
	return true;	
    }

    /**
     * Removes all files and subdirectories in this directory, freeing their sectors in the bitmap
     * @return
     */
    boolean removeAll(){
	
	BitMap freeMap;
	FileHeader fileHdr;
	int fileSector, dirSector;
	
	//Loop through table size, set their flags and remove them if 
	for(int i = 0; i < tableSize; i++){
	    
	    //If its a file
	    if(table[i].inUse() && !table[i].isDir()){
		//Load the file header
		fileSector = table[i].getSector();
		fileHdr = new FileHeader(filesystem);
		fileHdr.fetchFrom(fileSector);
		
		//Load the freemap
		freeMap = new BitMap(filesystem.numDiskSectors);
		freeMap.fetchFrom(filesystem.freeMapFile);
		
		fileHdr.deallocate(freeMap); // remove data blocks
		freeMap.clear(fileSector); // remove header block
		freeMap.writeBack(filesystem.freeMapFile);
	    }
	    //Otherwise its a directory
	    else if(table[i].inUse() && table[i].isDir()){
		//Fetch the directory from disk
		dirSector = table[i].getSector();
		OpenFileReal directoryFile = new OpenFileReal(dirSector, filesystem);
		Directory directory = new Directory(tableSize, filesystem);
		directory.fetchFrom(directoryFile);
		
		//Remove the subdirectories if any
		directory.removeAll();
		
		//Remove the directory
		fileHdr = new FileHeader(filesystem);
		fileHdr.fetchFrom(dirSector);
		
		//Load the freemap
		freeMap = new BitMap(filesystem.numDiskSectors);
		freeMap.fetchFrom(filesystem.freeMapFile);
		
		fileHdr.deallocate(freeMap); // remove data blocks
		freeMap.clear(dirSector); // remove header block
		freeMap.writeBack(filesystem.freeMapFile);
		
	    }
	    
	    //Whether its a file or directory set the flags
	    table[i].setUnused();
	    table[i].setIsDir(false);
	}
	
	return true;
    }
    
    
    /**
     * List all the file names in the directory (for debugging).
     */
    void list(String indent) {
	for (int i = 0; i < tableSize; i++)
	    if (table[i].inUse()) {
		
		if(table[i].isDir()) {
		    System.out.println(indent + ">" + table[i].getName());
		    OpenFileReal childDirFile = new OpenFileReal(table[i].getSector(), filesystem);
		    Directory childDir = new Directory(10, filesystem);
		    childDir.fetchFrom(childDirFile);
		    indent += "\t";
		    childDir.list(indent);
		    indent = indent.substring(0, indent.length() - 1);
		    
		}
		else {
		    System.out.println(indent + table[i].getName());
		}
		
	    }
    }

    /**
     * List all the file names in the directory, their FileHeader locations,
     * and the contents of each file (for debugging).
     */
    void print() {
	FileHeader hdr = new FileHeader(filesystem);

	System.out.print("Directory contents: ");
	for (int i = 0; i < tableSize; i++)
	    if (table[i].inUse()) {
		System.out.println("Name " + table[i].getName()
			+ ", Sector: " + table[i].getSector());
		hdr.fetchFrom(table[i].getSector());
		hdr.print();
	    }
	System.out.println("");
    }

}
