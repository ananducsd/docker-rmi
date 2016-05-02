import rmi.RMIException;

public class PingPongServer implements PingPongInterface{

	@Override
	public String ping(int idNumber) throws RMIException {
		String res =  "Pong " + idNumber;
		return res;
	}

}
