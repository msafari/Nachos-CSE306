/**
 * 
 */
package nachos.kernel.filesys;

import com.sun.org.apache.bcel.internal.generic.NEWARRAY;

import nachos.Debug;

/**
 * @author maedeh
 *
 */
public class DoublyIndirectBlock {
    /** Number of pointers to data blocks stored in a file header. */
    private final int NumDirect;

    /** Maximum file size that can be represented in the baseline system. */
    private final int MaxFileSize;

    /** Number of bytes in the file. */
    private int numBytes;

    /** Number of data sectors in the file. */
    private int numSectors;

    /** Disk sector numbers for each data block in the file. */
    private int dataSectors[];

    /** The underlying filesystem in which the file header resides. */
    private final FileSystemReal filesystem;
    
    private IndirectBlock iBlock;

    /** Disk sector size for the underlying filesystem. */
    private final int diskSectorSize;

    /**
     * Allocate a new "in-core" file header.
     * 
     * @param filesystem  The underlying filesystem in which the file header resides.
     */
    public DoublyIndirectBlock(FileSystemReal filesystem) {
	this.filesystem = filesystem;
	diskSectorSize = filesystem.diskSectorSize;
	NumDirect = ((diskSectorSize - 2 * 4) / 4);
	MaxFileSize = (NumDirect * diskSectorSize);

	dataSectors = new int[NumDirect + 2];
	// Safest to fill the table with garbage sector numbers,
	// so that we error out quickly if we forget to initialize it properly.
	for(int i = 0; i < NumDirect; i++)
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
	numBytes = FileSystem.bytesToInt(buffer, pos);
	numSectors = FileSystem.bytesToInt(buffer, pos+4);
	for (int i = 0; i < NumDirect; i++)
	    dataSectors[i] = FileSystem.bytesToInt(buffer, pos+8+i*4);
    }

    /**
     * Export the fields of this FileHeader object to a buffer
     * in a format suitable for writing to the disk.
     *
     * @param buffer A buffer into which to place the exported data.
     * @param pos Position in the buffer at which to start.
     */
    private void externalize(byte[] buffer, int pos) {
	FileSystem.intToBytes(numBytes, buffer, pos);
	FileSystem.intToBytes(numSectors, buffer, pos+4);
	for (int i = 0; i < NumDirect; i++)
	    FileSystem.intToBytes(dataSectors[i], buffer, pos+8+i*4);
    }

    /**
     * Initialize a fresh file header for a newly created file.
     * Allocate data blocks for the file out of the map of free disk blocks.
     * Return FALSE if there are not enough free blocks to accommodate
     *	the new file.
     *
     * @param freeMap is the bit map of free disk sectors.
     * @param fileSize is size of the new file.
     */
    boolean allocate(BitMap freeMap, int numSectors) {
	if(numSectors * diskSectorSize > MaxFileSize)
	    return false;		// file too large

	if (freeMap.numClear() < numSectors || NumDirect + 2 < numSectors)
	    return false;		// not enough space

	for (int i = 0; i < numSectors; i++){	    
	   
	    if( dataSectors[i] != -1 ) {	
		iBlock = new IndirectBlock(filesystem);
		iBlock.fetchFrom(dataSectors[i]);	
	    }
	    else {
		dataSectors[i] = freeMap.find(); 
	    }
	    
	    boolean res = iBlock.allocate(freeMap, numSectors * );
	    
	}
	    
	return true;
    }
    

    /**
     * De-allocate all the space allocated for data blocks for this file.
     *
     * @param freeMap is the bit map of free disk sectors.
     */
    void deallocate(BitMap freeMap) {
	for (int i = 0; i < numSectors; i++) {
	    Debug.ASSERT(freeMap.test(dataSectors[i]));  // ought to be marked!
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
	return(dataSectors[offset / diskSectorSize]);
    }

}
