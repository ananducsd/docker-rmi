package naming;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.util.Arrays;

public class Temp {

	public static void main(String[] args) {
		
		
		common.Path p = new common.Path("/");
		System.out.println(p.toString());
		String s = "/e:";
		String t = "/dir1";
		System.out.println(s.compareTo(t));
		String arr[] = s.split("/");
		System.out.println(Arrays.toString(arr));
		
	    /*FileSystem fs = FileSystems.getDefault();

	    System.out.println("Read-only file system: " + fs.isReadOnly());
	    System.out.println("File name separator: " + fs.getSeparator());

	    for (FileStore store : fs.getFileStores()) {
	      printDetails(store);
	    }
	    for (Path root : fs.getRootDirectories()) {
	      System.out.println(root);
	    }*/
	  }

	  public static void printDetails(FileStore store) {
	    try {
	      String desc = store.toString();
	      String type = store.type();
	      long totalSpace = store.getTotalSpace();
	      long unallocatedSpace = store.getUnallocatedSpace();
	      long availableSpace = store.getUsableSpace();
	      System.out.println(desc + ", Total: " + totalSpace + ",  Unallocated: "
	          + unallocatedSpace + ",  Available: " + availableSpace);
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	  }
}
