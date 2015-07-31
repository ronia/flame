package flame.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

import flame.ScreenLogger;
import flame.SocketTransferUtility;

/**
 * PortNumberTracker keeps track of the numbers of the ports of XTEAM Engines 
 * that are open and waiting for connection from FLAME Adaptors.
 * 
 * 
 * @author 					<a href=mailto:jaeyounb@usc.edu>Jae young Bang</a>
 * @version					2013.07
 */
public class PortNumberTracker {
///////////////////////////////////////////////////////////
//	Member variables
///////////////////////////////////////////////////////////

	/**
	 * Listening port number; FLAME Adaptors connect to 
	 * this port to be assigned with the corresponding 
	 * XTEAM Engine's port number to connect with
	 */
	protected 		int						listeningPort;
	
	/**
	 * Version Name - port number mapping
	 */
	protected 		Map<String, Integer> 	ports;
	
	/**
	* Binary semaphore for the port numbers
	*/
	protected final	Semaphore 				mSemaphore = new Semaphore (1, true);
	
	/**
	 * Handler for the Listener thread that accepts incoming connections from XTEAM Engines
	 */
	protected 		Listener				listener;
	
	/**
	 * 
	 * @author <a href=mailto:jaeyounb@usc.edu>Jae young Bang</a>
	 *
	 */
	public class Pair {
		public 		String					username;
		public		int						port;
	}
	
	/**
	 * Screen Logger passed from the owner Component
	 */
	protected 		ScreenLogger 			sl;
	
	
///////////////////////////////////////////////
//	Constructors
///////////////////////////////////////////////

	/**
	* Default constructor
	*/
	public PortNumberTracker(int listeningPort, ScreenLogger screenLogger) throws Exception {
		
		// Receives the attributes
		this.listeningPort	= listeningPort;
		sl					= screenLogger;
		
		// Initializes the ports mapping
		ports				= new TreeMap<String, Integer> ();
		
		// Launches the Listener thread
		listener = new Listener();
		listener.start();
	}
	
		
///////////////////////////////////////////////
//	Thread handling incoming connections from the FLAME Adaptor
///////////////////////////////////////////////

	/**
	* Thread class that listens to FLAME Adaptor connections and tells them the socket number of XTEAM Engines
	* 
	* @author 				<a href="mailto:jaeyounb@usc.edu">Jae young Bang</a>
	* @version				2013.07
	*/
	protected class Listener extends Thread {
		
		/**
		 * The listening socket
		 */
		protected ServerSocket serverSocket;
		
		/**
		 * Default constructor
		 */
		public Listener() throws Exception {
			// Creates a new socket
			createSocket();
			
			printMsg("Listener has been created.");
		}
		
		/**
		 * Creates the server socket
		 */
		protected void createSocket() throws Exception {
			// Creates a new socket
			try {
				serverSocket = new ServerSocket();
				serverSocket.setReuseAddress(true);
				serverSocket.bind(new InetSocketAddress(listeningPort));
			} catch(IOException e) {
				throw new Exception ("Could not begin listening on port: " + listeningPort);
			}
		}
		
		/**
		 * Begins accepting connections from XTEAM Engines	
		 */
		public void run() {
			
			// Socket to talk to incoming XTEAM Engine 
			Socket clientSocket = null;
			
			// Begins accepting FLAME Adaptor connection
			try {
				// Iterates accepting XTEAM Engine connections for good
				while (true) {
					// Accepts the connection
		            clientSocket = serverSocket.accept();
		            clientSocket.setSoTimeout(0); 
		            
		            // Launches a PortRequestHandler thread to handle the connection
		            PortRequestHandler prh = new PortRequestHandler(clientSocket);
		            prh.start();
				}
	            
	        } catch (IOException e) {
	            printMsg("Error: Failed to accept an XTEAM Engine.");
	            return;  
	        } finally {
				// close the server socket
	            try {
	            	serverSocket.close();
	            } catch (IOException ioe) {
	            	printMsg("Error: Cannot close the server socket");
	            }	        	
	        }
		}
		
		/**
		 * Thread class that actually talks to XTEAM Engines
		 * 
		 * @author 			<a href=mailto:jaeyounb@usc.edu>Jae young Bang</a>
		 * @version			2013.07
		 */
		protected class PortRequestHandler extends Thread {
			
			/**
			 * Incoming client socket
			 */
			protected Socket clientSocket;
			
			/**
			 * Default constructor
			 * 
			 * @param clientSocket	Incoming client socket
			 */
			public PortRequestHandler(Socket clientSocket) {
				this.clientSocket = clientSocket;
				
				printMsg("PortRequestHandler has been created.");
			}
			
			/**
			 * Begins communicating with the XTEAM Engine
			 */
			public void run() {
				
				PrintWriter 	out;	// the output stream
				BufferedReader 	in;		// the input stream
				
				// Sets up the input and output stream
	            try {     	
					out	= new PrintWriter(clientSocket.getOutputStream(), true);
					in 	= new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));	
	            } catch (IOException ioe) {
	            	printMsg("Error: Failed to initialize BufferedReader.");
	            	return;
	            }
	            
	            try {
					// Receives the port number request "REQUEST"
					String value = SocketTransferUtility.read(in);
					
					// Checks if the received message was "REQUEST"
					if(!value.equals("REQUEST")) {
						printMsg("Error: Failed to comprehend a message from FLAMEAdaptor [" + value + "]");
						return;
					}
					
					// Retrieve a port number
					int port = 0;
					
					while(true) {
						try {
							port = popRandomPort();
						} catch (Exception e) {
							printMsg("PortRequestHandler cannot find an available port. Retrying ...");
						}
						
						// if an available port number is successfully retrieved, break the loop
						if(port != 0) {
							break;
						} 
						
						// if retries, sleeps for 1 second before going back
						Thread.sleep(100);
					}
					
					
					printMsg("Port [" + port + "] is being assigned.");
					
					
					// Sends an assigned port number
					SocketTransferUtility.write(out, Integer.toString(port));
					
					
					// Receives an ACK "ACK"
					value = SocketTransferUtility.read(in);
					
					// Checks if the received message was "ACK"
					if(value.equals("ACK")) {
						printMsg("Port [" + port + "] has succesfully been assigned.");
					} else {
						printMsg("Error: Failed to comprehend a message from FLAMEAdaptor [" + value + "]");
					}
					
				} catch (IOException ioe) {
					printMsg("Error: Failed to communicate with an XTEAM Engine.");
				} catch (Exception e) {
					printMsg(e.toString());
				} finally {
					// close the server socket
		            try {
		            	clientSocket.close();
		            } catch (IOException ioe) {
		            	printMsg("Error: Cannot close the client socket");
		            }	
				}
			}
		}
	}

///////////////////////////////////////////////
//Member Methods
///////////////////////////////////////////////
	
	/**
	 * Adds a port to the mapping
	 * 
	 * @param username		Corresponding username (MRSV, LSV, username) of the XTEAM Engine 
	 * @param port			Port number
	 */
	public void addPort(String username, int port) {
		getLock();
		ports.put(username, port);
		releaseLock();
		
		printMsg("[" + username + "]'s port [" + port + "] has been added.");
	}
	
	/**
	 * Removes a port from the mapping
	 * 
	 * @param username		Corresponding username (MRSV, LSV, username) of the XTEAM Engine
	 */
	public void removePort(String username) {
		getLock();
		ports.remove(username);
		releaseLock();
		
		printMsg("[" + username + "]'s port has been removed.");
	}
	
	/**
	 * "Pops" a port number from the ports map.
	 * 
	 * @return				A port number from the EngineName-Port map
	 */
	public int popRandomPort() throws Exception {
		
		String		returnEngineName 	= "";
		int 		returnPort 			= 0;
		
		// Locks the map
		getLock();
		
		// Checks if the map is empty
		if(ports.isEmpty()) {
			throw new Exception ("The EngineName-Port map is empty.");
		}
		
		// Gets the first entry
		for(Map.Entry<String, Integer> entry : ports.entrySet()) {
			printMsg("Popping a port entry [" + entry.getKey() + "]:[" + entry.getValue() + "]");
			
			returnEngineName	= entry.getKey();		// Gets the key
			returnPort 			= entry.getValue();		// Gets the value

			break;										// Breaks after getting the first entry
		}
		
		// Removes the retrieved entry
		ports.remove(returnEngineName);
		
		// Releases the lock
		releaseLock();
		
		return returnPort;
	}

	/**
	 * Locks the port numbers
	 */
	protected void getLock() {
		try {
			mSemaphore.acquire();	// get the semaphore
		} catch (InterruptedException ie) {
			printMsg("Thread interrupted while waiting for the semaphore");
		}
	}
	
	/**
	 * Releases the port numbers
	 */
	protected void releaseLock() {
		mSemaphore.release();
	}
	
	/**
	 * Print screen messages
	 * 
	 * @param msg			Message to print to screen
	 */
	protected void printMsg(String msg) {
		sl.printMsg("PortTrack", msg);
	}
	
}
