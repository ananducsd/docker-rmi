package naming;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import common.Path;
import rmi.RMIException;
import rmi.Skeleton;
import storage.Command;
import storage.Storage;

/** Naming server.

    <p>
    Each instance of the filesystem is centered on a single naming server. The
    naming server maintains the filesystem directory tree. It does not store any
    file data - this is done by separate storage servers. The primary purpose of
    the naming server is to map each file name (path) to the storage server
    which hosts the file's contents.

    <p>
    The naming server provides two interfaces, <code>Service</code> and
    <code>Registration</code>, which are accessible through RMI. Storage servers
    use the <code>Registration</code> interface to inform the naming server of
    their existence. Clients use the <code>Service</code> interface to perform
    most filesystem operations. The documentation accompanying these interfaces
    provides details on the methods supported.

    <p>
    Stubs for accessing the naming server must typically be created by directly
    specifying the remote network address. To make this possible, the client and
    registration interfaces are available at well-known ports defined in
    <code>NamingStubs</code>.
 */
public class NamingServer implements Service, Registration
{
    /** Creates the naming server object.

        <p>
        The naming server is not started.
     */
	
	Skeleton<Service> clientService;
	Skeleton<Registration> registration; 
	
	Random rand = new Random();
	
	Set<StorageServerStubs> storageServers = new HashSet<StorageServerStubs>();
	
	ConcurrentMap<Path, List<StorageServerStubs>> m = new ConcurrentHashMap<Path, List<StorageServerStubs>>();
	Node root;
	
    public NamingServer()
    {
        // throw new UnsupportedOperationException("not implemented");
    	clientService = new Skeleton<Service>(Service.class, this);
    	registration = new Skeleton<Registration>(Registration.class, this);
    	
    	root = new Node("/", false);
    }

    /** Starts the naming server.

        <p>
        After this method is called, it is possible to access the client and
        registration interfaces of the naming server remotely.

        @throws RMIException If either of the two skeletons, for the client or
                             registration server interfaces, could not be
                             started. The user should not attempt to start the
                             server again if an exception occurs.
     */
    public synchronized void start() throws RMIException
    {
        //throw new UnsupportedOperationException("not implemented");
    	clientService.start();
    	registration.start();
    }

    /** Stops the naming server.

        <p>
        This method commands both the client and registration interface
        skeletons to stop. It attempts to interrupt as many of the threads that
        are executing naming server code as possible. After this method is
        called, the naming server is no longer accessible remotely. The naming
        server should not be restarted.
     */
    public void stop()
    {
        //throw new UnsupportedOperationException("not implemented");
    	clientService.stop();
    	registration.stop();
    }

    /** Indicates that the server has completely shut down.

        <p>
        This method should be overridden for error reporting and application
        exit purposes. The default implementation does nothing.

        @param cause The cause for the shutdown, or <code>null</code> if the
                     shutdown was by explicit user request.
     */
    protected void stopped(Throwable cause)
    {
    }

    // The following public methods are documented in Service.java.
    @Override
    public void lock(Path path, boolean exclusive) throws FileNotFoundException
    {
        
    	// throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void unlock(Path path, boolean exclusive)
    {
        // throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean isDirectory(Path path) throws FileNotFoundException
    {
    	try {
			Node curr = getPathNode(path);
			return !curr.isFile;
		} catch(FileNotFoundException e){
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
    }

    @Override
    public String[] list(Path directory) throws FileNotFoundException
    {
    	
    	try {
			Node curr = getPathNode(directory);
			if(curr.isFile) throw new FileNotFoundException("path does not refer to directory");
			return curr.childMap.keySet().toArray(new String[0]);
		} catch(FileNotFoundException e){
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
        // throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean createFile(Path file)
        throws RMIException, FileNotFoundException
    {
    	try {
			Node parent = getPathNode(file.parent());
			
			if(parent.childMap.containsKey(file.last())) {
				// Already a directory/file of the same name
				return false;
			}
			int i = rand.nextInt(storageServers.size());
			Iterator<StorageServerStubs> iter = storageServers.iterator();
			for (int j = 0; j < i; iter.next());
			StorageServerStubs stubs = iter.next();
			stubs.command_stub.create(file);
			
			// add to dfs
			parent.childMap.put(file.last(), new Node(file.last(), true));
			
			// add to map
			m.putIfAbsent(file, new ArrayList<StorageServerStubs>());
			m.get(file).add(stubs);
			
			return true;
		} catch(FileNotFoundException e){
			throw e;
		} catch (RMIException e) {
			throw e;
		} catch (Exception e) {
			return false;
		}
    	
    	
        // throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean createDirectory(Path directory) throws FileNotFoundException, RMIException
    {
		try {
			Node parent = getPathNode(directory.parent());
			
			if(parent.childMap.containsKey(directory.last())) {
				// Already a directory/file of the same name
				return false;
			}
			int i = rand.nextInt(storageServers.size());
			Iterator<StorageServerStubs> iter = storageServers.iterator();
			for (int j = 0; j < i; iter.next());
			StorageServerStubs stubs = iter.next();
			stubs.command_stub.create(directory);
			
			// add to dfs
			parent.childMap.put(directory.last(), new Node(directory.last(), false));
		} catch(FileNotFoundException e){
			throw e;
		} catch (RMIException e) {
			throw e;
		} catch (Exception e) {
			return false;
		}

		return true;

        //throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean delete(Path path) throws RMIException, FileNotFoundException
    {
    	try {
			List<String> pathItems = path.path;
			
			if(path.isRoot()) throw new IllegalArgumentException("Root cannot be deleted");
			Node parent = getPathNode(path.parent());
			Node curr = getPathNode(path);
			parent.childMap.remove(path.last());
			
			
			if(!curr.isFile) {
				// deleting a directory
				// delete all files within subdirectories
				Set<StorageServerStubs> storageServers = new HashSet<StorageServerStubs>();
				Set<Path> pathsToRemove = new HashSet<Path>();
				for(Path filePath : m.keySet()) {
					if(path.isSubpath(filePath)) {
						storageServers.addAll(m.get(filePath));
						pathsToRemove.add(filePath);
					}
				}
				
				for(Path filePath: pathsToRemove) m.remove(filePath);
				for(StorageServerStubs storageStub : storageServers) storageStub.command_stub.delete(path);
			}
			
			
		} catch(RMIException e) {
			throw e;
		} catch (Exception e) {
			return false;
		}
    	
    	return true;
        //throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Storage getStorage(Path file) throws FileNotFoundException
    {
    	
    	List<StorageServerStubs> stubs = m.get(file);
    	if(stubs == null) throw new FileNotFoundException();
    	int i = rand.nextInt(stubs.size());
    	return stubs.get(i).client_stub;
        //throw new UnsupportedOperationException("not implemented");
    }

    // The method register is documented in Registration.java.
    @Override
    public Path[] register(Storage client_stub, Command command_stub,
                           Path[] files)
    {
    	
    	List<Path> failedToAdd = new ArrayList<Path>();
    	for(Path file: files) {
    		boolean success = addFileToDFS(file);
    		if(!success) {
    			failedToAdd.add(file);
    		}
    		else {
    			m.putIfAbsent(file, new ArrayList<StorageServerStubs>());
    			m.get(file).add(new StorageServerStubs(client_stub, command_stub));
    		}
    	}
    	
    	return (Path[]) failedToAdd.toArray();
        //throw new UnsupportedOperationException("not implemented");
    }
    
    /*
    private boolean removeFileFromDFS(Path path)  {
    	List<String> pathItems = path.path;
		
		Node parent = null;
		Node curr = root;
		for(int i = 0; i < pathItems.size(); i++) {
			String elem = pathItems.get(i);
			if(curr.childMap == null) return false;
			if(curr.childMap.get(elem) == null) {
				return false;
			}
			parent = curr;
			curr = curr.childMap.get(elem);
		}
		
		if(parent == null) {
			return false;
		} else {
			parent.childMap.remove(curr.name);
			return true;
		}
    }*/
   
    private boolean addFileToDFS(Path file) {
    	
    	List<String> pathItems = file.path;
    	
    	Node curr = root;
    	for(int i = 0; i < pathItems.size() - 1; i++) {
    		String elem = pathItems.get(i);
    		// Intermediate component is a file, expecting a directory
    		if(curr.childMap == null) return false;
    		if(curr.childMap.get(elem) == null) {
    			curr.childMap.put(elem, new Node(elem, false));
    		}
    		curr = curr.childMap.get(elem);
    	}
    	
    	String elem = pathItems.get(pathItems.size() - 1);
    	if(curr.childMap.get(elem) != null) {
    		// can't add
    		return false;
    	} else {
    		curr.childMap.put(elem, new Node(elem, true));
    		return true;
    	}
    	
    }    
    
    
    private Node getPathNode(Path path) throws FileNotFoundException{
    	
    	List<String> pathItems = path.path;
    	
    	Node curr = root;
    	
    	for(int i = 0; i < pathItems.size(); i++) {
			String elem = pathItems.get(i);
			if(curr.childMap == null) throw new FileNotFoundException();
			if(curr.childMap.get(elem) == null) {
				throw new FileNotFoundException();
			}
			curr = curr.childMap.get(elem);
		}
    	
    	return curr;
    }
    
    private class StorageServerStubs {
    	public Storage client_stub;
    	public Command command_stub;
		public StorageServerStubs(Storage client_stub, Command command_stub) {
			super();
			this.client_stub = client_stub;
			this.command_stub = command_stub;
		}
    }
    
    private class Node {
    	boolean isFile;
    	Map<String, Node> childMap;
    	String name;
    	ReadWriteLock lock;
    	
    	public Node(String name, boolean isFile){
    		this.name = name;
    		this.isFile = isFile;
    		if(!isFile) {
    			childMap = new HashMap<String, Node>();
    		}
    		lock = new ReentrantReadWriteLock(true);
    	}
    }
}
