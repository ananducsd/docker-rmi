package storage;

import java.io.*;
import java.net.*;
import java.util.Arrays;

import common.*;
import rmi.*;
import naming.*;

/** Storage server.

    <p>
    Storage servers respond to client file access requests. The files accessible
    through a storage server are those accessible under a given directory of the
    local filesystem.
 */
public class StorageServer implements Storage, Command
{

    File root;
    Skeleton<Storage> storage;
    Skeleton<Command> command; 

    /** Creates a storage server, given a directory on the local filesystem, and
        ports to use for the client and command interfaces.

        <p>
        The ports may have to be specified if the storage server is running
        behind a firewall, and specific ports are open.

        @param root Directory on the local filesystem. The contents of this
                    directory will be accessible through the storage server.
        @param client_port Port to use for the client interface, or zero if the
                           system should decide the port.
        @param command_port Port to use for the command interface, or zero if
                            the system should decide the port.
        @throws NullPointerException If <code>root</code> is <code>null</code>.
    */
    public StorageServer(File root, int client_port, int command_port)
    {
        InetSocketAddress addrStorage, addrCommand;
        if (root == null) {
            throw new NullPointerException("Root is null");
        }
        
        this.root = root;
        if(client_port > 0) {
            addrStorage = new InetSocketAddress(client_port);
            storage = new Skeleton<Storage>(Storage.class, this, addrStorage);
        } else {
            storage = new Skeleton<Storage>(Storage.class, this);
        }

        if(command_port > 0) {
            addrCommand = new InetSocketAddress(command_port);
            command = new Skeleton<Command>(Command.class, this, addrCommand);
        } else {
            command = new Skeleton<Command>(Command.class, this);
        }

    }

    /** Creats a storage server, given a directory on the local filesystem.

        <p>
        This constructor is equivalent to
        <code>StorageServer(root, 0, 0)</code>. The system picks the ports on
        which the interfaces are made available.

        @param root Directory on the local filesystem. The contents of this
                    directory will be accessible through the storage server.
        @throws NullPointerException If <code>root</code> is <code>null</code>.
     */
    public StorageServer(File root)
    {
        this(root, 0, 0);
    }

    /** Starts the storage server and registers it with the given naming
        server.

        @param hostname The externally-routable hostname of the local host on
                        which the storage server is running. This is used to
                        ensure that the stub which is provided to the naming
                        server by the <code>start</code> method carries the
                        externally visible hostname or address of this storage
                        server.
        @param naming_server Remote interface for the naming server with which
                             the storage server is to register.
        @throws UnknownHostException If a stub cannot be created for the storage
                                     server because a valid address has not been
                                     assigned.
        @throws FileNotFoundException If the directory with which the server was
                                      created does not exist or is in fact a
                                      file.
        @throws RMIException If the storage server cannot be started, or if it
                             cannot be registered.
     */
    public synchronized void start(String hostname, Registration naming_server)
        throws RMIException, UnknownHostException, FileNotFoundException
    {
        if( !root.isDirectory() || !root.exists() ) {
            throw new FileNotFoundException("Server root not found or is not a directory");
        }
        storage.start();
        command.start();

        Path[] serverFiles = naming_server.register(
                Stub.create(Storage.class, storage, hostname),
                Stub.create(Command.class, command, hostname),
                Path.list(root));

        // Storage Server startup  deletes all duplicate files on server.
        for(Path p : serverFiles) {
            p.toFile(root).delete();
            File parent = new File(p.toFile(root).getParent());
            
            while(!parent.equals(root)) {
                if(parent.list().length == 0) {
                    parent.delete();
                } else {
                    break;
                }
                parent = new File(parent.getParent());
            }
        }        
    }

    /** Stops the storage server.

        <p>
        The server should not be restarted.
     */
    public void stop()
    {
        storage.stop();
        command.stop();
    }

    /** Called when the storage server has shut down.

        @param cause The cause for the shutdown, if any, or <code>null</code> if
                     the server was shut down by the user's request.
     */
    protected void stopped(Throwable cause)
    {
    }

    // The following methods are documented in Storage.java.
    @Override
    public synchronized long size(Path file) throws FileNotFoundException
    {
        File f = file.toFile(root);
        if (!f.exists() || f.isDirectory())
            throw new FileNotFoundException("File not found or is a directory");
        return f.length();    
    }

    @Override
    public synchronized byte[] read(Path file, long offset, int length)
        throws FileNotFoundException, IOException
    {
        File f = file.toFile(root);
        if(!f.exists() || f.isDirectory()) {
            throw new FileNotFoundException("File not found or is a directory");
        }
        if((offset < 0) 
            || (length < 0)
            || offset > Integer.MAX_VALUE  
            || (offset + length) > f.length()
            ) {
            throw new IndexOutOfBoundsException("Illegal Memory Access");
        }
        // reads from the file using FileInputStream and returns the content
        InputStream reader = new FileInputStream(f);
        byte[] output = new byte[length];
        reader.read(output, (int) offset, length);
        return output;    
    }

    @Override
    public synchronized void write(Path file, long offset, byte[] data)
        throws FileNotFoundException, IOException
    {
        File f = file.toFile(root);
        if(!f.exists() || f.isDirectory()) {
            throw new FileNotFoundException("File not found or is a directory");
        }
        if(offset < 0) {
            throw new IndexOutOfBoundsException("The offset is negative");
        }

        InputStream reader = new FileInputStream(f);
        FileOutputStream writer = new FileOutputStream(f);

        long bufferSize = Math.min(offset, f.length());
        byte[] buffer = new byte[(int) bufferSize];

        reader.read(buffer);
        writer.write(buffer, 0, (int) bufferSize);

        long fill = offset - f.length();
        if(fill > 0) {
            for(int i = 0; i < (int) fill; i ++) {
                writer.write(0);
            }
        }
        writer.write(data);
    }

    // The following methods are documented in Command.java.
    @Override
    public synchronized boolean create(Path file)
    {
        File f = file.toFile(root);
        if(file == null) {
            throw new NullPointerException("File to be created can't be null");
        }

        if (file.isRoot()) {
            return false;
        }

        File parent = file.parent().toFile(root);
        parent.mkdirs();

        try {
            return f.createNewFile();
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public synchronized boolean delete(Path path)
    {
        File f = path.toFile(root);
        if(path.isRoot() || !f.exists() ) {
            return false;
        }

        if(f.isDirectory()) {
            return deleteDirectory(f);
        } else {
            return f.delete();
        }        
    }

    private boolean deleteDirectory(File f) {
        if(f.isDirectory()) {
            for(File item : f.listFiles()) {
                if(!deleteDirectory(item)) {
                    return false;
                }
            }
        }
        return f.delete();
    }


    @Override
    public synchronized boolean copy(Path file, Storage server)
        throws RMIException, FileNotFoundException, IOException
    {
       if (file == null || server == null)
            throw new NullPointerException("Illegal / Null arguments provided");

        File f = file.toFile(root);
        if (f.exists()) {
            delete(file);
        }

        if (!create(file)) {
            throw new IOException("Error creating file");
        }

        long size = server.size(file);

        int buffsize = 8192; 
        byte[] buffer = new byte[buffsize];

        for (long i = 0; i * buffsize <= size; i++) {
            long done = i * buffsize;
            if ( (i+1) * buffsize > size) {
                buffer = server.read(file, 
                                done, 
                                (int) (size - done));
                write(file,
                    done,
                    Arrays.copyOfRange(buffer, 
                                        0, 
                                        (int) (size - done)));
            } else {
                buffer = server.read(file, (int) done, buffer.length);
                write(file, (int) done, buffer);
            }
        }
        return true;        
    }
}
