package common;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

/** Distributed filesystem paths.
    <p>
    Objects of type <code>Path</code> are used by all filesystem interfaces.
    Path objects are immutable.
    <p>
    The string representation of paths is a forward-slash-delimeted sequence of
    path components. The root directory is represented as a single forward
    slash.
    <p>
    The colon (<code>:</code>) and forward slash (<code>/</code>) characters are
    not permitted within path components. The forward slash is the delimeter,
    and the colon is reserved as a delimeter for application use.
 */
public class Path implements Iterable<String>, Comparable<Path>, Serializable
{

    private static final long serialVersionUID = 123123L;
    final public List<String> pathItems;
    final public String pathString;

    /** Creates a new path which represents the root directory. */
    public Path()
    {
        pathItems = new ArrayList<String> ();
        pathString = getPathString();
    }

    /** Creates a new path by appending the given component to an existing path.
        @param path The existing path.
        @param component The new component.
        @throws IllegalArgumentException If <code>component</code> includes the
                                         separator, a colon, or
                                         <code>component</code> is the empty
                                         string.
    */
    public Path(Path path, String component)
    {
        if (component == null || component.length() == 0
                              || !checkValidPath(component) ) {
            throw new IllegalArgumentException("Malformed Path: Component is invalid.");
        }
        pathItems = new ArrayList<String>(path.pathItems);
        pathItems.add(component);  
        pathString = getPathString();
    }

    /** Creates a new path from a path object.
     *
     * <p>
     *
     * @param components The path in form of ArrayList<String>.
     */
    public Path(List<String> components) {
        pathItems = new ArrayList<String>(components);
        pathString = getPathString();
    }


    /** Creates a new path from a path string.
        <p>
        The string is a sequence of components delimited with forward slashes.
        Empty components are dropped. The string must begin with a forward
        slash.
        @param path The path string.
        @throws IllegalArgumentException If the path string does not begin with
                                         a forward slash, or if the path
                                         contains a colon character.
     */
    public Path(String path)
    {
        if (path == null || path.length() == 0 ) {
            throw new IllegalArgumentException("Malformed Path: Null or Empty String found");
        }
        if (path.charAt(0) != '/' ) {
            throw new IllegalArgumentException("Malformed Path: doesn't begin with '/'");
        }

        pathItems = new ArrayList<String>();
        for (String component : path.split("/")) {
            
        	if (component.length() == 0) {
                continue;
            } else if (!checkValidPath(component)) {
                throw new IllegalArgumentException("Malformed Path: Unexpected /" 
                                                    + " Special characters found in path");
            } 
            else {
                pathItems.add(component);
            }
        }
        
        pathString = this.getPathString();
    }

    /** Returns an iterator over the components of the path.
        <p>
        The iterator cannot be used to modify the path object - the
        <code>remove</code> method is not supported.
        @return The iterator.
     */
    public Iterator<String> iterator()
    {
    	final Iterator<String> iter = pathItems.iterator();
        return new Iterator<String>() {

            @Override
            public boolean hasNext() {
            	return iter.hasNext();
            }

            @Override
            public String next() {
            	return iter.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Remove operation not supported");
            }
        };
    }
    
    /** 
     *   Determines whether the path represents the root directory.
     *   
     *   @return <code>true</code> if the path does represent the root directory,
     *           and <code>false</code> if it does not.
     */
    public boolean isRoot()
    {
        return pathItems.isEmpty();
    }

    /** Returns the path to the parent of this path.
        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no parent.
     */
    public Path parent()
    {
        if(isRoot()) {
            throw new IllegalArgumentException("Path represents the root"
                    + "directory and has no parent");
        }
        if (pathItems.size() == 1) {
            return new Path();
        }
        List<String> parentPath = new ArrayList<String>(pathItems);
        parentPath.remove(parentPath.size() - 1);
        return new Path(parentPath);        
    }

    /** Returns the last component in the path.
     *   
     *  @throws IllegalArgumentException If the path represents the root
     *                                   directory, and therefore has no last
     *                                   component.
     */
    public String last()
    {
        if (this.isRoot())
            throw new IllegalArgumentException("Given path represents the root directory");
        return pathItems.get(pathItems.size() - 1);
    }

    /** Determines if the given path is a subpath of this path.
        <p>
        The other path is a subpath of this path if it is a prefix of this path.
        Note that by this definition, each path is a subpath of itself.
        @param other The path to be tested.
        @return <code>true</code> If and only if the other path is a subpath of
                this path.
     */
    public boolean isSubpath(Path other)
    {
    	return this.pathString.startsWith(other.pathString);
    	
    	/*
        Iterator<String>  itOrig  = this.iterator();
        Iterator<String>  itOther = other.iterator();

        if(other.pathItems.size() > this.pathItems.size()) {
            return false;
        }

        while(itOther.hasNext()) {
            if(!itOrig.next().equals(itOther.next())) {
                return false;
            }
        }
        return true;*/
    }

    /** Converts the path to <code>File</code> object.
        @param root The resulting <code>File</code> object is created relative
                    to this directory.
        @return The <code>File</code> object.
     */
    public File toFile(File root)
    {
        if (root == null)
            return new File(this.toString());
        return new File(root, this.toString());
    }

    /** Compares this path to another.
        <p>
        An ordering upon <code>Path</code> objects is provided to prevent
        deadlocks between applications that need to lock multiple filesystem
        objects simultaneously. By convention, paths that need to be locked
        simultaneously are locked in increasing order.
        <p>
        Because locking a path requires locking every component along the path,
        the order is not arbitrary. For example, suppose the paths were ordered
        first by length, so that <code>/etc</code> precedes
        <code>/bin/cat</code>, which precedes <code>/etc/dfs/conf.txt</code>.
        <p>
        Now, suppose two users are running two applications, such as two
        instances of <code>cp</code>. One needs to work with <code>/etc</code>
        and <code>/bin/cat</code>, and the other with <code>/bin/cat</code> and
        <code>/etc/dfs/conf.txt</code>.
        <p>
        Then, if both applications follow the convention and lock paths in
        increasing order, the following situation can occur: the first
        application locks <code>/etc</code>. The second application locks
        <code>/bin/cat</code>. The first application tries to lock
        <code>/bin/cat</code> also, but gets blocked because the second
        application holds the lock. Now, the second application tries to lock
        <code>/etc/dfs/conf.txt</code>, and also gets blocked, because it would
        need to acquire the lock for <code>/etc</code> to do so. The two
        applications are now deadlocked.
        
        DESCRIPTION:
        
        Hence to avoid deadlocks, we need to impose a total topological ordering
        on the sequence of lock requests. We equate this ordering to the lexicographical 
        ordering of absolute path names. A dictionary order also satisfies the requirement
        that parent directories are locked prior to requesting access to any child 
        subdirectories or files. Thus, while locking a path object, shared locks are 
        requested for all the parent objects leading to the path - which satisfies the 
        lexical order. If well-behaved want simultaneous access to multiple paths, it is 
        their responsibility to order their access requests using this <code>compareTo</code>
        method. Thus, in the above example, the first application will request lock on 
        <code>/bin/cat</code> and not <code>/etc</code>. Whoever gets the lock first on / (root)
        will win and the operation will go through. The other application will wait to acquire 
        the lock, and thus deadlock will be avoided. 
        
        
        @param other The other path.
        @return Zero if the two paths are equal, a negative number if this path
                precedes the other path, or a positive number if this path
                follows the other path.
     */
    @Override
    public int compareTo(Path other)
    {
    	
    	return this.toString().compareTo(other.toString());
    	
        /*if (this.equals(other) || this.pathItems.isEmpty() && other.pathItems.isEmpty() )
    	if(this.equals(other))
            return 0;
        else if ( this.isSubpath(other))
            return 1;
        else 
            return -1;*/
    }

    /** Compares two paths for equality.
        <p>
        Two paths are equal if they share all the same components.
        @param other The other path.
        @return <code>true</code> if and only if the two paths are equal.
     */
    @Override
    public boolean equals(Object other)
    {
        return this.toString().equals(other.toString());
    }

    /** Returns the hash code of the path. */
    @Override
    public int hashCode()
    {
    	return this.toString().hashCode();
    	
        /* int hash = 0;
        int prime = 23;

        for(String component : this.pathItems) {
            hash += prime * component.hashCode();
            prime *= 23;
        }
        return hash;*/
    }
    
    public String getPathString() {
    	StringBuffer result = new StringBuffer("/");
        if (pathItems.size() != 0) {
        	int i =0;
            for (i = 0; i < pathItems.size()-1 ; i++) {
                result.append(pathItems.get(i));
                result.append("/") ;
            }
            result.append(pathItems.get(i)) ;
        }
        return result.toString();
    }
    
    /** Converts the path to a string.
        <p>
        The string may later be used as an argument to the
        <code>Path(String)</code> constructor.
        @return The string representation of the path.
     */
    @Override
    public String toString(){
        return pathString;
    }

    /** 
     *   Checks the validity of the path.
     *   <p>
     *   If the string passes the test, it may later be used 
     *   as an argument to the <code>Path(String)</code> constructor.
     *
     *   @param path The path string.
     *   @return <code>true</code> if the string doesn't contain '/' or ':'
     *         
     */
    boolean checkValidPath (String path) {
        return !(path.contains("/") || path.contains(":"));
    }

	/** Adds the paths of all files in a directory tree recursively 
	 *
	 *  @param directory The root directory of the directory tree.
	 *  @param paths An array of relative paths, upto the directory root.
	 *  @throws FileNotFoundException If the root directory does not exist.
	 *  @throws IllegalArgumentException If <code>directory</code> exists but
	                                     does not refer to a directory.
	 */
	public static void listDirectory (File root, File directory, List<Path> paths) 
	                                                    throws FileNotFoundException 
	{
    	String relativePath = Paths.get(root.getPath()).relativize(Paths.get(directory.getPath())).toString().replace('\\', '/');
	
	    if (directory == null || !directory.exists() )
	        throw new FileNotFoundException("Directory is null or doesn't exist");
	
	    if (!directory.isDirectory())
	        throw new IllegalArgumentException("Given path is not a directory.");
	
	    // for each file, either recurses on subdirectories or adds file
	    for(File f : directory.listFiles()) {
	        
	        if(f.isDirectory()) {
	            listDirectory(root, f, paths);
	        } else {
	        	
	        	Path p = new Path("/" + relativePath);
	        	Path filePath = new Path(p, f.getName());
	        	/*Path filePath = new Path( new Path(new Path(), directory.getName())
                        , f.getName());*/
	            paths.add(filePath);
	        }
	    }
	}

	/** Lists the paths of all files in a directory tree on the local
	    filesystem.
	    @param directory The root directory of the directory tree.
	    @return An array of relative paths, one for each file in the directory
	            tree.
	    @throws FileNotFoundException If the root directory does not exist.
	    @throws IllegalArgumentException If <code>directory</code> exists but
	                                     does not refer to a directory.
	 */
	public static Path[] list(File directory) throws FileNotFoundException
	{
	    if (directory == null || !directory.exists() )
	        throw new FileNotFoundException("Directory is null or doesn't exist");
	
	    if (!directory.isDirectory())
	        throw new IllegalArgumentException("Given path is not a directory.");
	
	    List<Path> paths = new ArrayList<Path>();
	    
	    for(File f : directory.listFiles()) {
	
	        if(f.isDirectory()) {
	            Path.listDirectory(directory, f, paths);
	        } else {
	        	Path filePath = new Path(new Path(), f.getName());
	            paths.add(filePath);
	        }
	    }
	    return paths.toArray(new Path[0]);        
	}

}
