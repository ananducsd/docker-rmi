package rmi;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Invocation handler for the client proxy class
 */
public class StubInvocationHandler implements InvocationHandler,Serializable {

    /**
     * Server socket address (hostname, port)
     */
	
    public InetSocketAddress serverAddr;
    public Class c; // Interface
	
	public StubInvocationHandler(Class c, InetSocketAddress address) {
	    this.serverAddr = address;
	    this.c = c;
	}
	
    
	@Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		
		try {
			Method isPresent = c.getMethod(method.getName(), method.getParameterTypes());
		} catch(NoSuchMethodException e) {
			if(method.getName().equalsIgnoreCase("toString")) {
	    		return "Interface name = " + c.toString() + ", Skeleton address =  " + serverAddr.toString() + "\n"; 
	    	} else if(method.getName().equalsIgnoreCase("hashCode")) {
	    		
	    		int prime = 31;
	    		int code = prime * serverAddr.hashCode();
	    		code  += prime * c.hashCode();
	    		return code;
	    		
	    	}else if(method.getName().equalsIgnoreCase("equals") && args.length == 1) {
	    		Object ob = args[0];
	    		if(ob == null) return false;
	    		if(!Proxy.isProxyClass(ob.getClass())) return false;
	    		InvocationHandler handler = Proxy.getInvocationHandler(ob);
	    		if( handler instanceof StubInvocationHandler) {
	    			StubInvocationHandler stubHandler = (StubInvocationHandler) handler;
	    			
	    			if ( !stubHandler.c.equals(c) ) {
	    				return false;
	    			}
	    			
	    			if ( !stubHandler.serverAddr.equals(serverAddr)){
	    				return false;
	    			}
	    			return true;
	    		}
	    	   
	    	    return false;

	    	} else {
	    		throw new RMIException("method " + method.getName() + " is not present in interface " + c.getName());
	    	}
		}
    	
    	
        Socket socket = null;
        
        if(serverAddr == null) System.out.println("Server address is null! Error!");
        
        
        if(serverAddr.getHostName() == null)
        	socket = new Socket(serverAddr.getAddress(), serverAddr.getPort());
        else 
        	socket = new Socket(serverAddr.getHostName(), serverAddr.getPort());
        
        Object retVal = null;
        Throwable serverException = null;
        
        try {
        	ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            Class<?> [] param = method.getParameterTypes();
        	String methodName = method.getName();
        	
        	out.writeObject(c.getName());
            out.writeObject(methodName);
            
            int argsLength = 0;
            if(args != null) argsLength = args.length;
            
            out.writeObject(argsLength);
            for ( int i = 0; i< argsLength ; i++) {
            	out.writeObject(param[i]);
            }
            for ( int i = 0; i< argsLength ; i++) {
            	out.writeObject(args[i]);
            }
            log("Wrote objects on client side");
            
            out.flush();
            
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            retVal = in.readObject();
            serverException = (Throwable) in.readObject();
            log("Got result on client side");
            
            if (serverException != null)
            	throw serverException;
            return retVal;
		} catch (Exception e) {
			if(serverException != null) {
				// Exception on the server side, throw as it is
				throw e;
			}
			else {
				// Exception on stub side 
				throw new RMIException(e);
			}
		} finally {
		    try {
		    	socket.shutdownOutput();
		        socket.close();
		    } catch (IOException e) {
		        log("Couldn't close a socket, what's going on?");
		    }
		}
        
    }
    
    
    private void log(String message) {
    	//System.out.println("C: " + message);
    }
}
