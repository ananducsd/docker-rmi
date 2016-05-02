package rmi;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ListeningThread<T> implements Runnable{

    protected int          serverPort   = 8080;
    protected ServerSocket serverSocket = null;
    
    protected boolean      isStopped    = false;
    protected Thread       runningThread= null;

    private T obj;
    private Skeleton<T> skeleton;
    
    public ListeningThread(int port, T obj, Skeleton<T> skeleton){
        this.serverPort = port;
        this.obj = obj;
        this.skeleton = skeleton;
    }

    public void run(){
        synchronized(this){
            this.runningThread = Thread.currentThread();
        }
        openServerSocket();
        while(! isStopped()){
            Socket clientSocket = null;
            try {
                clientSocket = this.serverSocket.accept();
            } catch (IOException e) {
                if(isStopped()) {
                    log("Server Stopped.") ;
                    return;
                }
                throw new RuntimeException(
                    "Error accepting client connection", e);
            }
            new Thread(
                new ServiceThread<T>(
                    clientSocket, obj, skeleton)
            ).start();
        }
        log("Server Stopped.") ;
    }


    public synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop(){
        this.isStopped = true;
        try {
            this.serverSocket.close();
            skeleton.stopped(null);
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }

    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port 8080", e);
        }
    }
    
    /**
     * Logs a simple message.  In this case we just write the
     * message to the server applications standard output.
     */
    private void log(String message) {
        //System.out.println("S: " + message);
    }

}
