/**
 * 
 */
package nachos.kernel.filesys;

import nachos.Debug;

/**
 * @author maedeh
 *
 */
public class DoublyIndirectBlock {
    /** Number of pointers to indirect data blocks stored on disk. */
    private final int NumIndirect;

    /** Maximum file size that can be represented in the baseline system. */
    private final int MaxFileSize;

    /** Disk sector numbers for each data block in the file. */
    private int dataSectors[];

    /** The underlying filesystem in which the file header resides. */
    private final FileSystemReal filesystem;

    /** Disk sector size for the underlying filesystem. */
    private final int diskSectorSize;

    /** Indirect block stored in each dataSector entry*/
    private IndirectBlock iBlock;
    
    /**
     * Allocate a new "in-core" file header.
     * 
     * @param filesystem  The underlying filesystem in which the file header resides.
     */
    public DoublyIndirectBlock(FileSystemReal filesystem) {
	this.filesystem = filesystem;
	diskSectorSize = filesystem.diskSectorSize;
	NumIndirect = ((diskSectorSize) / 4);
	
	MaxFileSize = (NumIndirect * diskSectorSize);

	dataSectors = new int[NumIndirect];
	// Safest to fill the table with garbage sector numbers,
	// so that we error out quickly if we forget to initialize it properly.
	for(int i = 0; i < NumIndirect; i++)
	    dataSectors[i] = -1;
	
    }

    // the following methods deal with conversion between the on-disk and
    // the in-memory representation of a DirectoryEnry.
    // Note: these methods must be modified if any instance variables 
    // are added!!

    /**
     * Initialize the fields of this FileHeader object using
     * data read from the disk.
     *
     * @param buffer A buffer holding the data read from the disk.
     * @param pos Position in the buffer at which to start.
     */
    private void internalize(byte[] buffer, int pos) {
	for (int i = 0; i < NumIndirect; i++)
	    dataSectors[i] = FileSystem.bytesToInt(buffer, pos+i*4);
    }

    /**
     * Export the fields of this FileHeader object to a buffer
     * in a format suitable for writing to the disk.
     *
     * @param buffer A buffer into which to place the exported data.
     * @param pos Position in the buffer at which to start.
     */
    private void externalize(byte[] buffer, int pos) {
	for (int i = 0; i < NumIndirect; i++)
	    FileSystem.intToBytes(dataSectors[i], buffer, pos+i*4);
    }

    /**
     * Initialize a fresh file header for a newly created file.
     * Allocate data blocks for the file out of the map of free disk blocks.
     * Return FALSE if there are not enough free blocks to accommodate
     *	the new file.
     *
     * @param freeMap is the bit map of free disk sectors.
     * @param numSectors is the number of sectors to allocate
     */
   int allocate(BitMap freeMap, int numSectors) {
	if(numSectors * diskSectorSize > MaxFileSize)
	    return -1;		// file too large

	if (freeMap.numClear() < numSectors || NumIndirect + 2 < numSectors)
	    return -1;		// not enough space

	Debug.println('f', "Allocating memory for doubly indirect block");
	int allocated = 0;
	for (int i = 0; i < numSectors; i++){	    
	   
	    //Create a new indirect block
	    iBlock = new IndirectBlock(filesystem); 
	    if( dataSectors[i] != -1 ) {	
		iBlock.fetchFrom(dataSectors[i]);	
	    }
	    else {
		dataSectors[i] = freeMap.find(); 
	    }
	    
	    //Allocate memory for the indirect block
	    int res = iBlock.allocate(freeMap, numSectors - allocated);
	    iBlock.writeBack(dataSectors[i]);
	    allocated += res;
	}
	    
	return allocated;
    }
    
   /**
    * 
    * @param freeMap
    * @return
    */
   int allocateIndirectBlock(BitMap freeMap) {

	Debug.println('f', "Allocating memory for doubly indirect block");
	int allocated = 0;
	for (int i = 0; i < dataSectors.length; i++){	    
	   
	    //Create a new indirect block
	    iBlock = new IndirectBlock(filesystem); 
	    if( dataSectors[i] == -1 ) {	

		dataSectors[i] = freeMap.find(); 
		freeMap.writeBack(filesystem.freeMapFile);
		//Allocate memory for the indirect block
		int res = iBlock.allocateSector(freeMap);
		iBlock.writeBack(dataSectors[i]);
		allocated += res;
		break;
	    }
	    
	    
	}
	    
	return allocated;
    }

    /**
     * De-allocate all the indirect sectors stored by the doublyIndirect object
     *
     * @param freeMap is the bit map of free disk sectors.
     */
    void deallocate(BitMap freeMap) {
	IndirectBlock iblock;
	
	for (int i = 0; i < dataSectors.length; i++) {
	    
	    Debug.ASSERT(freeMap.test(dataSectors[i]));  // ought to be marked!
	    iblock = new IndirectBlock(filesystem);
	    iblock.fetchFrom(dataSectors[i]); 	//Fetch the indirect block if it exists
	    iblock.deallocate(freeMap);		//Deallocate the indirect block
	    freeMap.clear(dataSectors[i]);
	}
    }

    /**
     * Fetch contents of file header from disk. 
     *
     * @param sector is the disk sector containing the file header.
     */
    void fetchFrom(int sector) {
	byte buffer[] = new byte[diskSectorSize];
	filesystem.readSector(sector, buffer, 0);
	internalize(buffer, 0);
    }

    /**
     * Write the modified contents of the file header back to disk. 
     *
     * @param sector is the disk sector to contain the file header.
     */
    void writeBack(int sector) {
	byte buffer[] = new byte[diskSectorSize];
	externalize(buffer, 0);
	filesystem.writeSector(sector, buffer, 0); 
    }

    /**
     * Calculate which disk sector is storing a particular byte within the file.
     *    This is essentially a translation from a virtual address (the
     *	offset in the file) to a physical address (the sector where the
     *	data at the offset is stored).
     *
     * @param offset The location within the file of the byte in question.
     * @return the disk sector number storing the specified byte.
     */
    int byteToSector(int offset) {
	return(dataSectors[offset]);
    }
    
    
    /**
     * 
     */
    void validate () {
	BitMap freeMap = new BitMap(filesystem.numDiskSectors);
	freeMap.fetchFrom(filesystem.freeMapFile);
	IndirectBlock iBlock;
	
	for(int i = 0; i < NumIndirect; i++) {
	    if(dataSectors[i] != -1 ) {
		if(!freeMap.test(dataSectors[i])){
		    Debug.println('V', "Sector " + dataSectors[i] + " is in use but not marked as used in BitMap.");
		}
		else {
		    iBlock = new IndirectBlock(filesystem);
		    iBlock.fetchFrom(dataSectors[i]);
		    
		    iBlock.validate();
		}
	    }
	}
    }

}
