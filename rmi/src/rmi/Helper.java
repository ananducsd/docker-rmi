package rmi;

import java.lang.reflect.Method;

public class Helper {

	static boolean isRemoteInterface(Class c) {
		
		Method[] methods = c.getMethods();
		
		for (Method method : methods) {
			Class<?>[] exceptions = method.getExceptionTypes();
			boolean isRemote = false;
			for (Class<?> exception : exceptions) {
				if (exception.getName().equals(RMIException.class.getName())) {
					isRemote = true;
					break;
				}
			}
			if(!isRemote) return false;
		}
		
		return true;
	}
}
