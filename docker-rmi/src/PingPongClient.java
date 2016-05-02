import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class PingPongClient {

	private BufferedReader in;
	private PrintWriter out;
	static String filePath;
	static String serverAddress;
	static int port;
	BufferedReader br;
	

	/**
	 * Implements the connection logic by prompting the end user for the
	 * server's IP address, connecting, setting up streams, and consuming the
	 * welcome messages from the server. The Capitalizer protocol says that the
	 * server sends three lines of text to the client immediately after
	 * establishing a connection.
	 */
	public void connectToServer() throws IOException {
		
		Socket socket = null;
		try {

			System.out.println("Trying to connect to server");
			serverAddress = "server";
			socket = new Socket(serverAddress, port);
			
			ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            PingPongInterface remoteObj = (PingPongInterface) in.readObject();
            
            // test ping method 4 times
            int testsPassed = 0;
            for(int i = 0; i < 4; i++) {
            	try {
            		System.out.println("Sent value " + i);
            		String res = remoteObj.ping(i);
            		System.out.println("Received value " + res);
					String val = "Pong " + i;

					if(res.equals(val)) testsPassed++;
				} catch (Exception e) {
					e.printStackTrace();
				}
            }
            int testsFailed = 4 - testsPassed;
            System.out.println("4 tests completed," + testsFailed + " tests failed");
            
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
            try {
                if(socket != null) socket.close();
            } catch (IOException e) {
                System.out.println("Couldn't close a socket, what's going on?");
            }
        }

	}

	/**
	 * Runs the client application.
	 */
	public static void main(String[] args) throws Exception {
		PingPongClient client = new PingPongClient();

		if (args.length < 1) {
			System.out.println("Cmd format : java CatClient <port-number>");
			return;
		}

		port = Integer.parseInt(args[0]);
		client.connectToServer();
	}
}