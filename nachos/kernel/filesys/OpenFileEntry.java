package nachos.kernel.filesys;

/**
 * Class to keep track of open files and their unique ids
 *
 */
public class OpenFileEntry {

    public int id = -1;
    public String name;
    public OpenFile file;
    
    /**
     * Create a new entry. Id is set in the syscall
     * @param file
     */
    public OpenFileEntry(OpenFile file){
	this.file = file;
    }
}
