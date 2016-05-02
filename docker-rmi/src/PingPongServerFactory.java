import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import rmi.RMIException;
import rmi.Skeleton;
import rmi.Stub;


public class PingPongServerFactory {

    
    static String filePath;
    static int port;
    static BufferedReader br;
    
    
    public static void main(String[] args) throws Exception {
        
        System.out.println("The ping pong server factory is running.");
        
        if(args.length < 1) {
        	// Ping Pong server factory runs at <port-number>
            System.out.println("Cmd format : java PingPongServerFactory <port-number>");
            return;
        }
        
        port = Integer.parseInt(args[0]);
        
        int clientNumber = 0;
        ServerSocket listener = new ServerSocket(port);
        System.out.println("Listening on port " + port);
        try {
            new PingPongInit(listener.accept(), clientNumber++).start();
        } finally {
            listener.close();
        }
    }

   
    private static class PingPongInit extends Thread {
        private Socket clientSocket;
        private int clientNumber;

        public PingPongInit(Socket socket, int clientNumber) {
            this.clientSocket = socket;
            this.clientNumber = clientNumber;
            log("New connection with client# " + clientNumber + " at " + socket);
        }

        /**
         * Services this thread's client by repeatedly reading strings
         * and sending back the capitalized version of the string.
         */
        public void run() {
        	
        	Skeleton<PingPongInterface> skeleton = null;
            try {

            	
            	ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            	out.flush();
            	
            	PingPongInterface obj = new PingPongServer();
        		skeleton = new Skeleton<PingPongInterface>(PingPongInterface.class, obj);
        		skeleton.start();
        		
        		
        		// creating client stub
        		PingPongInterface client = Stub.create(PingPongInterface.class, skeleton, "server");
        		out.writeObject(client);
                
            } catch (IOException e) {
                log("IO Exception handling client# " + clientNumber + ": " + e);
            } catch(RMIException e){
            	log("RMI Exception handling client# " + clientNumber + ": " + e);
            }finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    log("Couldn't close a socket, what's going on?");
                }
                log("Connection with client# " + clientNumber + " closed");
            }
        }

        private void log(String message) {
            System.out.println(message);
        }
    }
}