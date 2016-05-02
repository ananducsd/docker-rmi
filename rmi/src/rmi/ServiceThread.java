package rmi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;

public class ServiceThread<T> implements Runnable{
	
	private Socket clientSocket;
    private T obj;
    private Skeleton<T> skeleton;

    public ServiceThread(Socket clientSocket, T obj, Skeleton<T> skeleton) {
        this.clientSocket = clientSocket;
        this.obj = obj;
        this.skeleton = skeleton;
    }

    /**
     * Services this thread's client by accepting method name and arguments from client object stream, invoke the method 
     * on the object and return the results
     */
    public void run() {
        try {
        	
        	log("New connection with client at " + clientSocket);
        	ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
        	out.flush(); 
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
            
            String interfaceName = (String) in.readObject();
            String methodName = (String) in.readObject();
            //Class interfaceObj =   this.getClass().getClassLoader().loadClass(interfaceName);            
            
            if(!skeleton.interfaceObj.getName().equalsIgnoreCase(interfaceName)) {
            	throw new RMIException("Interfaces do not match for skeleton and stub");
            }
            int argsLen = (Integer)in.readObject();
            Object[] args = null;
            Class<?>[] argsTypes = null;
            if(argsLen != 0) {
            	args = new Object[argsLen];
                argsTypes = new Class[argsLen];
                for(int i = 0; i < args.length; i++) {
                	argsTypes[i] = (Class<?>)in.readObject();
                }
                for(int i = 0; i < args.length; i++) {
                	args[i] = (Object)in.readObject();
                }
            }
            log("Read objects on server side");
            	
            Method method = obj.getClass().getMethod(methodName, (Class<?>[]) argsTypes);
            method.setAccessible(true);
            Object retObj = null;
            Throwable serverException = null;
			try {
				retObj = method.invoke(obj, args);
			} catch (IllegalAccessException e) {
				serverException = e;
			}catch (InvocationTargetException e) {
				serverException = e.getCause();
			}catch (Exception e ) {
				serverException = e;
			}
            // call the object with method and find result
            
           
            log("Wrote object on server side");
            out.writeObject(retObj);
            out.writeObject(serverException);
            
        } catch (IOException e) {
        	
        	log("Exception in service thread. Seems like client closed abruptly");
        	skeleton.service_error(new RMIException(e));
            //log("Error handling client : " + e);
        	//e.printStackTrace();
        } catch(RMIException e){
        	skeleton.service_error(e);
        } catch(Exception e) { 
        	log("Exception in service thread");
        	if (e instanceof RMIException) 
        	skeleton.service_error(new RMIException(e));
        }/*catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}*/ finally {
			
            try {
            	clientSocket.shutdownOutput();
                clientSocket.close();
            } catch (IOException e) {
                log("Couldn't close a socket, what's going on?");
            }
            log("Connection with client closed");
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