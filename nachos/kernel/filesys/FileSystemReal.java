// FileSystemReal.java
//	Class to manage the overall operation of the file system.
//	Implements methods to map from textual file names to files.
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and 
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.filesys;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import nachos.Debug;
import nachos.kernel.Nachos;
import nachos.kernel.devices.DiskDriver;

/**
 * This class manages the overall operation of the file system.
 *	It implements methods to map from textual file names to files.
 *	Each file in the file system has:
 *	   A file header, stored in a sector on disk 
 *		(the size of the file header data structure is arranged
 *		to be precisely the size of 1 disk sector);
 *	   A number of data blocks;
 *	   An entry in the file system directory.
 *
 * 	The file system consists of several data structures:
 *	   A bitmap of free disk sectors (cf. bitmap.h);
 *	   A directory of file names and file headers.
 *
 *      Both the bitmap and the directory are represented as normal
 *	files.  Their file headers are located in specific sectors
 *	(sector 0 and sector 1), so that the file system can find them 
 *	on bootup.
 *
 *	The file system assumes that the bitmap and directory files are
 *	kept "open" continuously while Nachos is running.
 *
 *	For those operations (such as create, remove) that modify the
 *	directory and/or bitmap, if the operation succeeds, the changes
 *	are written immediately back to disk (the two files are kept
 *	open during all this time).  If the operation fails, and we have
 *	modified part of the directory and/or bitmap, we simply discard
 *	the changed version, without writing it back to disk.
 *
 * 	Our implementation at this point has the following restrictions:
 *
 *	   there is no synchronization for concurrent accesses;
 *	   files have a fixed size, set when the file is created;
 *	   files cannot be bigger than about 3KB in size;
 *	   there is no hierarchical directory structure, and only a limited
 *	     number of files can be added to the system;
 *	   there is no attempt to make the system robust to failures
 *	    (if Nachos exits in the middle of an operation that modifies
 *	    the file system, it may corrupt the disk).
 *
 *	A file system is a set of files stored on disk, organized
 *	into directories.  Operations on the file system have to
 *	do with "naming" -- creating, opening, and deleting files,
 *	given a textual file name.  Operations on an individual
 *	"open" file (read, write, close) are to be found in the OpenFile
 *	class (OpenFile.java).
 *
 *	We define two separate implementations of the file system. 
 *	This version is a "real" file system, built on top of 
 *	a disk simulator.  The disk is simulated using the native
 *	file system on the host platform (in a file named "DISK"). 
 *
 *	In the "real" implementation, there are two key data structures used 
 *	in the file system.  There is a single "root" directory, listing
 *	all of the files in the file system; unlike UNIX, the baseline
 *	system does not provide a hierarchical directory structure.  
 *	In addition, there is a bitmap for allocating
 *	disk sectors.  Both the root directory and the bitmap are themselves
 *	stored as files in the Nachos file system -- this causes an interesting
 *	bootstrap problem when the simulated disk is initialized. 
 *
 * @author Thomas Anderson (UC Berkeley), original C++ version
 * @author Peter Druschel (Rice University), Java translation
 * @author Eugene W. Stark (Stony Brook University)
 */
class FileSystemReal extends FileSystem {

  // Sectors containing the file headers for the bitmap of free sectors,
  // and the directory of files.  These file headers are placed in 
  // well-known sectors, so that they can be located on boot-up.

  /** The disk sector containing the bitmap of free sectors. */
  public static final int FreeMapSector = 0;

  /** The disk sector containing the directory of files. */
  private static final int DirectorySector = 1;
  
  /** The maximum number of entries in a directory. */
  public static final int NumDirEntries = 10;

  /** Access to the disk on which the filesystem resides. */
  private final DiskDriver diskDriver;
  
  public BitMap diskSectors;
 
  
  /** Sector size of the disk. */
  public final int diskSectorSize;
  
  // Initial file sizes for the bitmap and directory; until the file system
  // supports extensible files, the directory size sets the maximum number 
  // of files that can be loaded onto the disk.

  /** The initial file size for the bitmap file. */
  private final int FreeMapFileSize;

  /** The initial size of a directory file. */
  public final int DirectoryFileSize;


  /** "Root" directory -- list of file names, represented as a file. */
  private final OpenFile directoryFile;
  
  /**
   * Initialize the file system.  If format = true, the disk has
   * nothing on it, and we need to initialize the disk to contain
   * an empty directory, and a bitmap of free sectors (with almost but
   * not all of the sectors marked as free).  
   *
   * If format = false, we just have to open the files
   * representing the bitmap and the directory.
   *
   * @param diskDriver  Access to the disk on which the filesystem resides.
   * @param format  Should we initialize the disk?
   */
  protected FileSystemReal(DiskDriver diskDriver, boolean format) { 
    Debug.print('f', "Initializing the real file system.\n");
    this.diskDriver = diskDriver;
    numDiskSectors = diskDriver.getNumSectors();
    diskSectorSize = diskDriver.getSectorSize();
    FreeMapFileSize = (numDiskSectors / BitMap.BitsInByte);
    DirectoryFileSize = (DirectoryEntry.sizeOf() * NumDirEntries);
    
    if (format) {
      BitMap freeMap = new BitMap(numDiskSectors);
      Directory directory = new Directory(NumDirEntries, this);
      FileHeader mapHdr = new FileHeader(this);
      FileHeader dirHdr = new FileHeader(this);

      Debug.print('f', "Formatting the file system.\n");

      // First, allocate space for FileHeaders for the directory and bitmap
      // (make sure no one else grabs these!)
      freeMap.mark(FreeMapSector);	    
      freeMap.mark(DirectorySector);
      // Second, allocate space for the data blocks containing the contents
      // of the directory and bitmap files.  There better be enough space!

      Debug.ASSERT(mapHdr.allocate(freeMap, FreeMapFileSize));
      Debug.ASSERT(dirHdr.allocate(freeMap, DirectoryFileSize));
      // Flush the bitmap and directory FileHeaders back to disk
      // We need to do this before we can "Open" the file, since open
      // reads the file header off of disk (and currently the disk has 
      // garbage on it!).

      Debug.print('f', "Writing headers back to disk.\n");
      mapHdr.writeBack(FreeMapSector);    
      dirHdr.writeBack(DirectorySector);

      // OK to open the bitmap and directory files now
      // The file system operations assume these two files are left open
      // while Nachos is running.

      freeMapFile = new OpenFileReal(FreeMapSector, this);
      directoryFile = new OpenFileReal(DirectorySector, this);
    
      
      // Once we have the files "open", we can write the initial version
      // of each file back to disk. The directory at this point is completely
      // empty; but the bitmap has been changed to reflect the fact that
      // sectors on the disk have been allocated for the file headers and
      // to hold the file data for the directory and bitmap.
      Debug.print('f', "Writing bitmap and directory back to disk.\n");
      freeMap.writeBack(freeMapFile);	 // flush changes to disk
      directory.writeBack(directoryFile);

      //Create the first directory 'test' for copying in files
      makeDirectory("test", DirectoryFileSize);

      if (Debug.isEnabled('f')) {
	printBitMap();
      }
      
    } else {
      // if we are not formatting the disk, just open the files representing
      // the bitmap and directory; these are left open while Nachos is 
      // running
      freeMapFile = new OpenFileReal(FreeMapSector, this);
      directoryFile = new OpenFileReal(DirectorySector, this);
    }
  }
  
  /**
   * Read a sector of the filesystem, using the underlying disk driver.
   *
   * @param sectorNumber The disk sector to read.
   * @param data The buffer to hold the contents of the disk sector.
   * @param index Offset in the buffer at which to place the data.
   */
  void readSector(int sectorNumber, byte[] data, int index) {
      diskDriver.readSector(sectorNumber, data, index);
  }
  
  /**
   * Write a sector of the filesystem, using the underlying disk driver.
   *
   * @param sectorNumber The disk sector to be written.
   * @param data The new contents of the disk sector.
   * @param index Offset in the buffer from which to get the data.
   */
  void writeSector(int sectorNumber, byte[] data, int index) {
      diskDriver.writeSector(sectorNumber, data, index);
  }

  /**
   * Create a file in the Nachos file system (similar to UNIX create).
   * Since we can't increase the size of files dynamically, we have
   * to supply the initial size of the file.
   *
   * The steps to create a file are:
   *  Make sure the file doesn't already exist;
   *  Allocate a sector for the file header;
   *  Allocate space on disk for the data blocks for the file;
   *  Add the name to the directory;
   *  Store the new file header on disk;
   *  Flush the changes to the bitmap and the directory back to disk.
   *
   * Return true if everything goes ok, otherwise, return false.
   *
   * Create fails if:
   * 	file is already in directory;
   *	no free space for file header;
   *	no free entry for file in directory;
   *	no free space for data blocks for the file.
   *
   * Note that this implementation assumes there is no concurrent access
   *	to the file system!
   *
   * @param name  The path of file to be created.
   * @param initialSize  The size of file to be created.
   * @return true if the file was successfully created, otherwise false.
   */
  public boolean create(String path, long initialSize) {
    Directory directory = new Directory(NumDirEntries, this);
    BitMap freeMap;
    FileHeader hdr;
    int sector;
    boolean success;

    Debug.printf('f', "Creating file %s, size %d\n", path, 
		 new Long(initialSize));

    int directorySector = getDirectory(path);
    OpenFile dirFile = new OpenFileReal(directorySector, this);
    directory.fetchFrom(dirFile);

    if (directory.find(getFileName(path)) != -1)
      success = false;			// file is already in directory
    else {	
      freeMap = new BitMap(numDiskSectors);
      freeMap.fetchFrom(freeMapFile);
      sector = freeMap.find();	// find a sector to hold the file header
      if (sector == -1) 		
	success = false;		// no free block for file header 
      else if (!directory.add(getFileName(path), sector))
	success = false;	// no space in directory
      else {
	hdr = new FileHeader(this);
	if (!hdr.allocate(freeMap, (int)initialSize))
	  success = false;	// no space on disk for data
	else {	
	  success = true;
	  // everthing worked, flush all changes back to disk
	  hdr.writeBack(sector); 		
	  OpenFileReal parentFile = new OpenFileReal(directorySector, this);
  	  directory.writeBack(parentFile);
	  freeMap.writeBack(freeMapFile);
	  printBitMap();
	}
      }
    }
    return success;
  }
  
  /**
   * Checks that all disk sectors are valid, in particular:
   * -Disk sectors that are used by files (or file headers), but that are also marked as "free" in the bitmap.
   * -Disk sectors that are not used by any files (or file headers), but that are marked as "in use" in the bitmap.
   * -Disk sectors that are referenced by more than one file header.
   * -Multiple directory entries that refer to the same file header.
   * @return
   */
  public void checkValid () {
      
      Debug.println('V', "Validating Disk Sectors.");
      
      //Validate disk sectors against bitmap
      BitMap freeMap = new BitMap(numDiskSectors);
      Directory root = new Directory(NumDirEntries, this);
      OpenFileReal rootFile = new OpenFileReal(DirectorySector, this);
      root.fetchFrom(rootFile);
     
      //Validate disk sectors against fileHeaders;
      diskSectors = new BitMap(numDiskSectors);
      
      root.validate();
      
      
      //Disk sectors that are referenced by more than one file header.
      Debug.println('V', "Finished Validating Disk Sectors.");
  }
  
  
  

  /**
   * Open a file for reading and writing.  
   * To open a file:
   *	  Find the location of the file's header, using the directory;
   *	  Bring the header into memory.
   *
   * @param path The absolute path of the file to be opened.
   */
  public OpenFile open(String path) { 
    Directory directory = new Directory(NumDirEntries, this);
    int directorySector = getDirectory(path);
    OpenFile dirFile = new OpenFileReal(directorySector, this);
    directory.fetchFrom(dirFile);
    
    OpenFile openFile = null;
    int sector;

    Debug.printf('f', "Opening file %s\n", path);
    sector = directory.find(getFileName(path)); 
    if (sector >= 0) 		
      openFile = new OpenFileReal(sector, this);// name was found in directory 
    return openFile;			        // return null if not found
  }

  /**
   * Delete a file from the file system.  This requires:
   *    Remove it from the directory;
   *    Delete the space for its header;
   *    Delete the space for its data blocks;
   *    Write changes to directory, bitmap back to disk.
   *
   * Return true if the file was deleted, false if the file wasn't
   *	in the file system.
   *
   * @param path The absolute path of the file to be removed.
   */
  public boolean remove(String path) { 
    Directory directory = new Directory(NumDirEntries, this);
    BitMap freeMap;
    FileHeader fileHdr;
    int sector;
    
    int directorySector = getDirectory(path);
    OpenFile dirFile = new OpenFileReal(directorySector, this);
    directory.fetchFrom(dirFile);

    sector = directory.find(getFileName(path));
    if (sector == -1) {
       return false;			 // file not found 
    }
    fileHdr = new FileHeader(this);
    fileHdr.fetchFrom(sector);

    freeMap = new BitMap(numDiskSectors);
    freeMap.fetchFrom(freeMapFile);

    fileHdr.deallocate(freeMap);  		// remove data blocks
    freeMap.clear(sector);			// remove header block
    directory.remove(getFileName(path));

    freeMap.writeBack(freeMapFile);		// flush to disk
    OpenFileReal parentFile = new OpenFileReal(directorySector, this);
    directory.writeBack(parentFile);
    return true;
  } 

  /**
   * Copy the contents of the host file "from" to the Nachos file "to"
   *
   * @param from The name of the file to be copied from the host filesystem.
   * @param to The name of the file to create on the Nachos filesystem.
   */
  public void copy(String from, String to) {
      	Debug.println('f', "Copying " + from + " into Nachos file " + to);
	File fp;
	FileInputStream fs;
	OpenFile openFile;
	long fileLength;
	byte buffer[];

	// Open UNIX file
	fp = new File(from);
	if (!fp.exists()) {
	    Debug.printf('+', "Copy: couldn't open input file %s\n", from);
	    return;
	}

	// Figure out length of UNIX file
	fileLength = fp.length();

	// Create a Nachos file of the same length
	Debug.printf('f', "Copying file %s, size %d, to file %s\n", from, new Long(fileLength), to);
	
	//If it doesn't exist already, create it.
	if(Nachos.fileSystem.open(to) == null){

	    if (!Nachos.fileSystem.create(to, (int) fileLength)) {
		// Create Nachos file
		Debug.printf('+',"Copy: couldn't create output file %s.\n",to);
		return;
	    }
	}
	
	//Otherwise just open it
	Debug.println('f', "File already exists, opening it instead.");
	openFile = Nachos.fileSystem.open(to);
	Debug.ASSERT(openFile != null);

	// Copy the data in TransferSize chunks
	buffer = new byte[(int) fileLength];
	try {
	    fs = new FileInputStream(fp);
	    fs.read(buffer);
	    openFile.write(buffer, 0, buffer.length);	
	} catch (IOException e) {
	    Debug.print('+', "Copy: data copy failed\n");      
	    return;
	}
	// Close the UNIX and the Nachos files
	//delete openFile;
	try {fs.close();} catch (IOException e) {}
  }
  
  /**
   * List all the files in the file system directory (for debugging).
   */
  public void list() {
    Directory directory = new Directory(NumDirEntries, this);
    directory.fetchFrom(directoryFile);
    directory.list("");
    
  }
  
  
  /**
   * Make a directory
   * @param path
   * @param initialSize
   * @return
   */
  public boolean makeDirectory (String path, long initialSize) {
      Directory directory = new Directory(NumDirEntries, this);
      BitMap freeMap;
      FileHeader hdr;
      int sector;
      boolean success;

      Debug.printf('f', "Creating Diretory %s, size %d\n", path, 
  		 new Long(initialSize));

      int directorySector = getDirectory(path);
      OpenFile dirFile = new OpenFileReal(directorySector, this);
      directory.fetchFrom(dirFile);
  	
      if (directory.find(getFileName(path)) != -1)
        success = false;			// file is already in directory
      else {	
        freeMap = new BitMap(numDiskSectors);
        freeMap.fetchFrom(freeMapFile);
        sector = freeMap.find();	// find a sector to hold the file header
        if (sector == -1) 		
  	success = false;		// no free block for file header 
        else if (!directory.addDirectory(getFileName(path), sector))
  	success = false;	// no space in directory
        else {
  	hdr = new FileHeader(this);
  	if (!hdr.allocate(freeMap, (int)initialSize))
  	  success = false;	// no space on disk for data
  	else {	
  	  success = true;
  	  // everthing worked, flush all changes back to disk
  	  hdr.writeBack(sector);
  	  OpenFileReal parentFile = new OpenFileReal(directorySector, this);
  	  directory.writeBack(parentFile);
  	  directory.fetchFrom(parentFile);
  	  
  	  Directory newDir = new Directory(NumDirEntries, this);
  	  OpenFileReal newDirFile = new OpenFileReal(sector, this);
  	  newDir.writeBack(newDirFile);
  	  freeMap.writeBack(freeMapFile);
  	}
        }
      }
      
      return success;
  }
  
/**
   * Frees the directory and all the subdirectories and files inside of it as well.
   * @return
   */
  public boolean removeDirectory(String path){
      Directory directory = new Directory(NumDirEntries, this);
      BitMap freeMap;
      FileHeader fileHdr;
      int sector;
      
      int directorySector = getDirectory(path);
      OpenFile dirFile = new OpenFileReal(directorySector, this);
      directory.fetchFrom(dirFile);

      sector = directory.find(getFileName(path));
      if (sector == -1) {
         return false;			 // directory not found 
      }
      
      //Load up the directory to be removed
      Directory removeMe = new Directory(NumDirEntries, this);
      OpenFile removeMeDirFile = new OpenFileReal(sector, this);
      removeMe.fetchFrom(removeMeDirFile);
      
      //Remove all the subdirectories and files first
      removeMe.removeAll();
      
      //Finally remove the directory itself
      fileHdr = new FileHeader(this);
      fileHdr.fetchFrom(sector);

      freeMap = new BitMap(numDiskSectors);
      freeMap.fetchFrom(freeMapFile);

      fileHdr.deallocate(freeMap);  		// remove data blocks
      freeMap.clear(sector);			// remove header block
      directory.remove(getFileName(path));

      freeMap.writeBack(freeMapFile);		// flush to disk
      OpenFileReal parentFile = new OpenFileReal(directorySector, this);
      directory.writeBack(parentFile);
      
      return true;
  }
  
  /**
   * 
   * @param path
   * @return
   */
  public String getFileName (String path) {
     String[] subDirs = path.split("/");
     return subDirs[subDirs.length - 1];
  }
  /**
   *	
   * @param path
   * @return
   */
  public int getFileSector(String path) {
      //fetch the root directory
      Directory curDirectory = new Directory(NumDirEntries, this);
      curDirectory.fetchFrom(directoryFile);
      
      String[] subDirs = path.split("/");
      String fileName = subDirs[subDirs.length - 1];
      
      for (int i = 0; i < subDirs.length - 1; i++) {
	 
	  //find in current directory
	  int dirSectorNum = curDirectory.findDirectory(subDirs[i]);
	  if (dirSectorNum == -1) {
	      Debug.println('f', "Couldn't find the directory: "+ subDirs[i] + " in its parent Directory!");
	      return -1;
	  }
	  
	  OpenFile dirFile = new OpenFileReal(dirSectorNum, this);
	  curDirectory.fetchFrom(dirFile);	  
      }
      
      return curDirectory.find(fileName);
     
  }
  

  /**
   *	
   * @param path
   * @return
   */
  public int getDirectory(String path) {
      //fetch the root directory
      Directory curDirectory = new Directory(NumDirEntries, this);
      curDirectory.fetchFrom(directoryFile);
      
      String[] subDirs = path.split("/");
      if(subDirs.length == 1) {
	  return DirectorySector;
      }
      int dirSectorNum = -1;
      for (int i = 0; i < subDirs.length - 1; i++) {
	 
	  //find in current directory
	  dirSectorNum = curDirectory.findDirectory(subDirs[i]);
	  if (dirSectorNum == -1) {
	      Debug.println('f', "Couldn't find the directory: "+ subDirs[i] + " in its parent Directory!");
	      return -1;
	  }
	  
	  OpenFile dirFile = new OpenFileReal(dirSectorNum, this);
	  curDirectory = new Directory(NumDirEntries, this);
	  curDirectory.fetchFrom(dirFile);	  
      }
     
      return dirSectorNum;
     
  }
  
  
  /**
   * Print everything about the file system (for debugging):
   *  the contents of the bitmap;
   *  the contents of the directory;
   *  for each file in the directory:
   *      the contents of the file header;
   *      the data in the file.
   */
  public void print() {
    FileHeader bitHdr = new FileHeader(this);
    FileHeader dirHdr = new FileHeader(this);
    BitMap freeMap = new BitMap(numDiskSectors);
    Directory directory = new Directory(NumDirEntries, this);

    Debug.print('+', "Bit map file header:\n");
    bitHdr.fetchFrom(FreeMapSector);
    bitHdr.print();

    Debug.print('+', "Directory file header:\n");
    dirHdr.fetchFrom(DirectorySector);
    dirHdr.print();

    freeMap.fetchFrom(freeMapFile);
    freeMap.print();

    directory.fetchFrom(directoryFile);
    directory.print();

  }
  
  public void printBitMap() {
      BitMap freeMap = new BitMap(numDiskSectors);
      Debug.print('+', "Free map:\n");
      freeMap.fetchFrom(freeMapFile);
      freeMap.print();
  }
  
  public int getDirectoryFileSize(){
      return DirectoryFileSize;
  }

    @Override
    public BitMap getDiskMap() {
	return diskSectors;
    }

}
