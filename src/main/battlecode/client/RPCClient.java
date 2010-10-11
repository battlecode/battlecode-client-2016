package battlecode.client;

import battlecode.server.Server;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * A utility class for making remote method calls.
 */
public class RPCClient {
	
	/** The socket to use for communication. */
	private final Socket socket;
	
	/** The default RPC port. */
	private static final int DEFAULT_PORT = 12370;
	
	/**
	 * Creates a new RPC client that will talk to the given host over the
	 * default port.
	 *
	 * @param host the hostname of the remote machine
	 * @throws UnknownHostException if the host couldn't be resolved
	 * @throws IOException if the host couldn't be reached
	 */
	public RPCClient(String host) throws UnknownHostException, IOException {
		this(host, DEFAULT_PORT);
	}
	
	/**
	 * Creates a new RPC client that will talk to the given host over the given
	 * port.
	 *
	 * @param host the hostname of the remote machine
	 * @throws UnknownHostException if the host couldn't be resolved
	 * @throws IOException if the host couldn't be reached
	 */
	public RPCClient(String host, int port) throws UnknownHostException, IOException {
		socket = new Socket(host, port);
	}
	
	/**
	 * Pass the given argument to the RPC server and return its reply.
	 *
	 * @param arg the argument to the RPC server
	 * @param <T> the type of the argument to the RPC server
	 * @return the RPC server's response
	 */
	public <T> Object call(T arg) {
		try {
			// Use serialization.
			ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
			ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
			
			// Write the argument to the stream.
			output.writeObject(arg);
			output.reset();
			output.flush();
			
			// Get the input.
			Object result = input.readObject();
			
			// Close the socket.
			socket.close();
			
			return result;
		} catch (IOException e) {
			Server.error("RPC client error: " + e.getMessage());
		} catch (ClassNotFoundException e) {
			Server.error("RPC client error: " + e.getMessage());
		}
		
		// If something went wrong...
		return null;
	}
}
