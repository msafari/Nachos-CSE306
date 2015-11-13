// FileHeader.jave
//	Routines for managing the disk file header (in UNIX, this
//	would be called the i-node).
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and 
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.filesys;

import nachos.Debug;

/**
 * This class defines the Nachos "file header" (in UNIX terms, the "i-node"),
 * describing where on disk to find all of the data in the file. The file header
 * is organized as a simple table of pointers to data blocks.
 * 
 * The file header data structure can be stored in memory or on disk. When it is
 * on disk, it is stored in a single sector -- this means that we assume the
 * size of this data structure to be the same as one disk sector. Without
 * indirect addressing, this limits the maximum file length to just under 4K
 * bytes.
 * 
 * The file header is used to locate where on disk the file's data is stored. We
 * implement this as a fixed size table of pointers -- each entry in the table
 * points to the disk sector containing that portion of the file data (in other
 * words, there are no indirect or doubly indirect blocks). The table size is
 * chosen so that the file header will be just big enough to fit in one disk
 * sector,
 * 
 * Unlike in a real system, we do not keep track of file permissions, ownership,
 * last modification date, etc., in the file header.
 * 
 * A file header can be initialized in two ways: for a new file, by modifying
 * the in-memory data structure to point to the newly allocated data blocks; for
 * a file already on disk, by reading the file header from disk.
 * 
 * @author Thomas Anderson (UC Berkeley), original C++ version
 * @author Peter Druschel (Rice University), Java translation
 * @author Eugene W. Stark (Stony Brook University)
 */
class FileHeader {

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

    /** Disk sector size for the underlying filesystem. */
    private final int diskSectorSize;

    /**
     * Allocate a new "in-core" file header.
     * 
     * @param filesystem
     *            The underlying filesystem in which the file header resides.
     */
    FileHeader(FileSystemReal filesystem) {
	this.filesystem = filesystem;
	diskSectorSize = filesystem.diskSectorSize;
	NumDirect = ((diskSectorSize - 2 * 4) / 4);
	MaxFileSize = (NumDirect * diskSectorSize);

	// Number of dataSectors for FileHeader
	dataSectors = new int[NumDirect];

	// Safest to fill the table with garbage sector numbers,
	// so that we error out quickly if we forget to initialize it properly.
	for (int i = 0; i < NumDirect; i++)
	    dataSectors[i] = -1;

    }

    // the following methods deal with conversion between the on-disk and
    // the in-memory representation of a DirectoryEnry.
    // Note: these methods must be modified if any instance variables
    // are added!!

    /**
     * Initialize the fields of this FileHeader object using data read from the
     * disk.
     * 
     * @param buffer
     *            A buffer holding the data read from the disk.
     * @param pos
     *            Position in the buffer at which to start.
     */
    private void internalize(byte[] buffer, int pos) {
	numBytes = FileSystem.bytesToInt(buffer, pos);
	numSectors = FileSystem.bytesToInt(buffer, pos + 4);
	for (int i = 0; i < NumDirect; i++)
	    dataSectors[i] = FileSystem.bytesToInt(buffer, pos + 8 + i * 4);
    }

    /**
     * Export the fields of this FileHeader object to a buffer in a format
     * suitable for writing to the disk.
     * 
     * @param buffer
     *            A buffer into which to place the exported data.
     * @param pos
     *            Position in the buffer at which to start.
     */
    private void externalize(byte[] buffer, int pos) {
	FileSystem.intToBytes(numBytes, buffer, pos);
	FileSystem.intToBytes(numSectors, buffer, pos + 4);
	for (int i = 0; i < NumDirect; i++)
	    FileSystem.intToBytes(dataSectors[i], buffer, pos + 8 + i * 4);
    }

    /**
     * Initialize a fresh file header for a newly created file. Allocate data
     * blocks for the file out of the map of free disk blocks. Return FALSE if
     * there are not enough free blocks to accommodate the new file.
     * 
     * @param freeMap
     *            is the bit map of free disk sectors.
     * @param fileSize
     *            is size of the new file.
     */
    boolean allocate(BitMap freeMap, int fileSize) {

	if (fileSize > MaxFileSize)
	    return false; // file too large
	
	numBytes = fileSize;
	numSectors = fileSize / diskSectorSize;
	
	if (fileSize % diskSectorSize != 0)
	    numSectors++;

	if (freeMap.numClear() < numSectors || NumDirect < numSectors)
	    return false; // not enough space
	
	// Allocate memory for the direct blocks
	for (int i = 0; i < NumDirect - 2; i++) {
	    dataSectors[i] = freeMap.find();
	}
	
	//Calculate the number of sectors exceeding the 28 direct blocks we have
	int sectorsLeft = numSectors - NumDirect - 2;
	
	//We did not need to allocate more memory. Done allocating
	if(sectorsLeft <= 0){
	    Debug.println('f', "File with " + numSectors + " sectors fit in direct blocks! Not allocating indirect/doublyindirect blocks");
	    return true;
	}
	//Otherwise we need to allocate more memory
	else{
	    Debug.println('f', "File did not fit in direct blocks. Sectors left: " + sectorsLeft);
	    
	    //First allocate the indirect block
	    sectorsLeft = allocateIndirectBlock(freeMap, sectorsLeft);
	   
	    //Then allocate the remaining blocks 
	    sectorsLeft = allocateDoublyIndirectBlock(freeMap, sectorsLeft);

	}

	return true;
    }
    /**
     * Function to allocate the doubly indirect block
     * @param freeMap
     * @param sectorsLeft
     * @return number of sectors allocated
     */
    private int allocateDoublyIndirectBlock(BitMap freeMap, int sectorsLeft){

	int doublyIndirectSectors = sectorsLeft;

	// Doubly Indirect block can only hold 32 * 32 data sectors
	if (sectorsLeft >= (NumDirect + 2) * (NumDirect + 2)) {
	    Debug.println('f', "File is too large!");
	    Debug.ASSERT(false);
	}

	// First allocate the doubly indirect block
	DoublyIndirectBlock dblock = new DoublyIndirectBlock(filesystem);

	// If doubly indirect block has not been used before, get a new sector
	if (dataSectors[NumDirect - 1] == -1) {
	    dataSectors[NumDirect - 1] = freeMap.find();
	}
	// If its already used, just load the sector
	else {
	    dblock.fetchFrom(dataSectors[NumDirect - 1]);
	}
	// Allocate a sector for this block
	int allocated = dblock.allocate(freeMap, doublyIndirectSectors);
	
	// Write the dblock back to the disk
	dblock.writeBack(dataSectors[NumDirect - 1]);

	Debug.println('f', "Wrote " + allocated + " sectors to double indirect block");
	
	return allocated;
    }
    
    /**
     * Function to allocate the indirect block
     * @param freeMap
     * @param sectorsLeft
     * @return number of sectors allocated
     */
    private int allocateIndirectBlock(BitMap freeMap, int sectorsLeft){

	int indirectSectors = sectorsLeft;

	// Indirect block can only hold 32 sectors
	if (sectorsLeft > NumDirect + 2) {
	    // Allocate max sectors to indirect block, and the rest to the doubly indirect block
	    indirectSectors = NumDirect + 2;
	}

	// First allocate the indirect block
	IndirectBlock iblock = new IndirectBlock(filesystem);

	// If indirect block has not been used before, get a new sector
	if (dataSectors[NumDirect - 2] == -1) {
	    dataSectors[NumDirect - 2] = freeMap.find();
	}
	// If its already used, just load the sector
	else {
	    iblock.fetchFrom(dataSectors[NumDirect - 2]);
	}
	// Allocate a sector for this block
	int allocated = iblock.allocate(freeMap, indirectSectors);
	
	// Write the iblock back to the disk
	iblock.writeBack(dataSectors[NumDirect - 2]);

	Debug.println('f', "Wrote " + allocated + " sectors to indirect block");
	
	return allocated;
    }
    
    /**
     * De-allocate all the space allocated for data blocks for this file.
     * 
     * @param freeMap
     *            is the bit map of free disk sectors.
     */
    void deallocate(BitMap freeMap) {

	DoublyIndirectBlock dblock;
	IndirectBlock iblock;

	int i;
	// First free the direct blocks
	for (i = 0; i < numSectors - 2; i++) {
	    Debug.ASSERT(freeMap.test(dataSectors[i])); // ought to be marked!
	    freeMap.clear(dataSectors[i]);
	}
	// Next free the indirect block if it has been used
	if (dataSectors[i] != -1) {
	    iblock = new IndirectBlock(filesystem);
	    iblock.fetchFrom(dataSectors[i]);
	    iblock.deallocate(freeMap);
	    Debug.ASSERT(freeMap.test(dataSectors[i])); // ought to be marked!
	    freeMap.clear(dataSectors[i]);
	}
	// Finally free the doubly indirect block
	i += 1;
	if (dataSectors[i] != -1) {
	    dblock = new DoublyIndirectBlock(filesystem);
	    dblock.fetchFrom(dataSectors[i]);
	    dblock.deallocate(freeMap);
	    Debug.ASSERT(freeMap.test(dataSectors[i])); // ought to be marked!
	    freeMap.clear(dataSectors[i]);
	}
    }

    /**
     * Fetch contents of file header from disk.
     * 
     * @param sector
     *            is the disk sector containing the file header.
     */
    void fetchFrom(int sector) {
	byte buffer[] = new byte[diskSectorSize];
	filesystem.readSector(sector, buffer, 0);
	internalize(buffer, 0);
    }

    /**
     * Write the modified contents of the file header back to disk.
     * 
     * @param sector
     *            is the disk sector to contain the file header.
     */
    void writeBack(int sector) {
	byte buffer[] = new byte[diskSectorSize];
	externalize(buffer, 0);
	filesystem.writeSector(sector, buffer, 0);
    }

    /**
     * Calculate which disk sector is storing a particular byte within the file.
     * This is essentially a translation from a virtual address (the offset in
     * the file) to a physical address (the sector where the data at the offset
     * is stored).
     * 
     * @param offset
     *            The location within the file of the byte in question.
     * @return the disk sector number storing the specified byte.
     */
    int byteToSector(int offset) {
	return (dataSectors[offset / diskSectorSize]);
    }

    /**
     * Retrieve the number of bytes in the file.
     * 
     * @return the number of bytes in the file.
     */
    int fileLength() {
	return numBytes;
    }

    /**
     * Print the contents of the file header, and the contents of all the data
     * blocks pointed to by the file header.
     */
    void print() {
	int i, j, k;
	byte data[] = new byte[diskSectorSize];

	System.out.print("FileHeader contents.  File size: " + numBytes
		+ ".,  File blocks: ");
	for (i = 0; i < numSectors; i++)
	    System.out.print(dataSectors[i] + " ");

	System.out.println("\nFile contents:");
	for (i = k = 0; i < numSectors; i++) {
	    filesystem.readSector(dataSectors[i], data, 0);
	    for (j = 0; (j < diskSectorSize) && (k < numBytes); j++, k++) {
		if ('\040' <= data[j] && data[j] <= '\176')   // isprint(data[j])
		    System.out.print((char)data[j]);
		else
		    System.out.print("\\" + Integer.toHexString(data[j] & 0xff));
	    }
	    System.out.println();
	}
    }

}
