/**
 * 
 */
package nachos.kernel.filesys;

import nachos.Debug;

/**
 * @author maedeh
 *
 */
public class IndirectBlock {
    /** Number of pointers to data blocks stored in a file header. */
    private final int NumDirect;

    /** Maximum file size that can be represented in the baseline system. */
    private final int MaxFileSize;

    /** Disk sector numbers for each data block in the file. */
    private int dataSectors[];

    /** The underlying filesystem in which the file header resides. */
    private final FileSystemReal filesystem;

    /** Disk sector size for the underlying filesystem. */
    private final int diskSectorSize;

    /**
     * Allocate a new "in-core" file header.
     * 
     * @param filesystem  The underlying filesystem in which the file header resides.
     */
    public IndirectBlock(FileSystemReal filesystem) {
	this.filesystem = filesystem;
	diskSectorSize = filesystem.diskSectorSize;
	NumDirect = (diskSectorSize / 4); // We are using the full 32 entries of this sector
	MaxFileSize = (NumDirect * diskSectorSize);

	dataSectors = new int[NumDirect];
	
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
	for (int i = 0; i < NumDirect; i++)
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
	for (int i = 0; i < NumDirect; i++)
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

	if (freeMap.numClear() < numSectors || NumDirect < numSectors)
	    return -1;		// not enough space
	
	int allocated = 0;
	for (int i = 0; i < numSectors; i++){
	    if(dataSectors[i] == -1){
		dataSectors[i] = freeMap.find();
		allocated++;
	    }
	}
	    
	return allocated;
    }
    
    int allocateSector (BitMap freeMap) {
	
	int allocated = 0;
	for (int i = 0; i < dataSectors.length; i++){
	    if(dataSectors[i] == -1){
		dataSectors[i] = freeMap.find();
		freeMap.writeBack(filesystem.freeMapFile);
		allocated++;
		break;
	    }
	}
	    
	return allocated;
    }
    

    /**
     * De-allocate all the space allocated for data blocks for this file.
     *
     * @param freeMap is the bit map of free disk sectors.
     */
    void deallocate(BitMap freeMap) {
	for (int i = 0; i < dataSectors.length; i++) {
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
	return(dataSectors[offset]);
    }


}
