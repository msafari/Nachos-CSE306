/**
 * 
 */
package nachos.kernel.userprog;

/**
 * @author maedeh
 *
 */
public class MemMappedFile {

    public String fileName;
    
    /** Store the starting address of the newly mapped region*/
    public int startAddr;
    
    public int allocatedSize;
    
    
    public MemMappedFile (String fileName, int startAddr, int allocatedSize){
	this.fileName = fileName;
	this.startAddr = startAddr;
	this.allocatedSize = allocatedSize;
    }
}
