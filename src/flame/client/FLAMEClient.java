package flame.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Display;

import flame.AbstractImplementationForClient;
import flame.Constants;
import flame.EventStorage;
import flame.SocketTransferUtility;
import flame.Utility;
import flame.server.PortNumberTracker;
import flame.client.xteam.*;
import Prism.core.*;
import Prism.extensions.port.ExtensiblePort;
import Prism.extensions.port.distribution.SocketDistribution;

/**
 * FLAMEClient is the client instance of FLAME that is 
 * deployed on the architect's machine. <p>
 * 
 * FLAMEClient uses a GUI window to present the proactive 
 * conflict information to the architect.<p>
 * 
 * @author 					<a href="mailto:jaeyounb@usc.edu">Jae young Bang</a>
 * @version					2013.05
 */
public class FLAMEClient extends AbstractImplementationForClient {
	
///////////////////////////////////////////////////////////
//	Member variables
///////////////////////////////////////////////////////////
	
	/**
	 * Port number for server listening socket
	 */
	protected 	int 						port;
	
	/**
	 * Socket stream to talk with FLAME adaptor
	 */
	protected 	PrintWriter 				outAdaptor;					
	
	/**
	 * Username of this Architect
	 */
	protected 	String 						username;
	
	/**
	 * LoginGUI window
	 */
	protected 	LoginGUI 					loginGUI;
	
	/**
	 * FLAME GUI
	 */
	public 		FLAMEGUI 					flameGUI;
	
	/**
	 * Last EventID counter -- local model version
	 */
	protected 	int							event_id;
	
	/**
	 * Thread that talks to FLAME Adaptor 
	 */
	protected 	EventReceiver 				eventReceiver;
	
	/**
	 * Simple event history to avoid Event echoing from FLAME Adaptor<p>
	 * 
	 * Without this mechanism, the same Event comes around when one Event is forwarded 
	 * to FLAME Adaptor
	 */
	protected 	EventHistory 				eventHistory; 
	
	/**
	 * Main event storage where all incoming events are stored
	 */
	protected 	EventStorage				storage; 
	
	/**
	 * CoWareClient & XTEAMEngine original model file (.mga) path
	 */
	protected	Path						modelPath;
	
	/**
	 * CoWareClient & XTEAMEngine model file (.mga) copy directory path
	 */
	protected	Path						modelsDirPath;
	
	/**
	 * GME executable path
	 */
	protected	Path						GMEPath;
	
	/**
	 * A component that listens to the port number requests from FLAME Adaptor
	 */
	protected	PortNumberTracker			ports;
	
	/**
	 * Switch that turns on and off the XTEAM GUI window for experimental purposes
	 */
	protected	boolean						xteamGUISwitch;
	
	/**
	 * List of in-progress simulation IDs
	 */
	protected	Map<String, Set<Integer>> 	mapSimIDs	= new TreeMap<>();
	
	/**
	 * Model editor process
	 */
	private	Process							modelingTool;
	
	/**
	 * Delimiter used in the FLAME Adaptor messages
	 */
	protected static String					delimiter = "`";
	
	/**
	 * Last Event of the list of Event for purging buffer
	 */
	protected	Event 						lastEvent;
	
	/**
	 * Gets the last Event of the list of Event for purging buffer
	 * @return				Last Event
	 */
	public		Event						getLastEvent() { return lastEvent; }
	
	/**
	 * Empties the last Event of the list of Event for purging buffer
	 */
	public		void						emptyLastEvent() { lastEvent = null; }
	
	/**
	 * Semaphore to control the outgoing Events
	 */
	protected Semaphore 					mLockOutgoingEvents = new Semaphore (1, true);
	
	/**
	 * Locks the outgoing Events semaphore
	 */
	public void getLock() {
		try {
			mLockOutgoingEvents.acquire();	// get the semaphore
		} catch (InterruptedException ie) {
			printMsg(name, "Thread interrupted while waiting for the semaphore");
		}
	}
	
	/**
	 * Releases the outgoing Event semaphore
	 */
	public void releaseLock() {
		mLockOutgoingEvents.release();
	}
	
	
///////////////////////////////////////////////
//	Constructors
///////////////////////////////////////////////
	public FLAMEClient(String componentName){
		super(componentName);
	}
	
	/**
	 * Default constructor
	 * 
	 * @param componentName		Component name
	 */
	public FLAMEClient (	String 	componentName,
								Path 	modelPath,
								Path 	modelsDirPath,
								Path	GMEPath,
								boolean xteamGUISwitch) {
		super(componentName);
		this.modelPath		= modelPath;
		this.modelsDirPath	= modelsDirPath;
		this.GMEPath		= GMEPath;
		this.xteamGUISwitch	= xteamGUISwitch;
		
		outAdaptor 				= null;
		event_id 			= 0;
		eventReceiver 		= null;
		eventHistory 		= new EventHistory();
		storage				= new EventStorage(getScreenLogger());
		port 				= 0;
		flameGUI			= null;
	}
	
	/**
	 * Constructor with PortNumberTracker on
	 * 
	 * @param componentName		Component name
	 * @param tracker_port		Port for PortNumberTracker, open for FLAME Adaptor connection
	 */
	public FLAMEClient (	String 	componentName,
								Path 	modelPath,
								Path 	modelsDirPath,
								Path	GMEPath,
								int 	tracker_port,
								boolean	xteamGUISwitch) throws Exception {
		this (componentName, modelPath, modelsDirPath, GMEPath, xteamGUISwitch);
		ports				= new PortNumberTracker(tracker_port, getScreenLogger());
	}
	


///////////////////////////////////////////////
//	Member Methods
///////////////////////////////////////////////
	
	/**
	 * Initializes this XTEAMEngine, invoked by Prism-MW
	 */
	public void start () {
		printMsg(name, "Brining up the login window ...");
		
		// pop up the login window
		try {
			loginGUI = new LoginGUI("");
		} catch (SWTException se) {
			printMsg(name, "Login GUI cannot find the background image file: " + se.toString());
			System.exit(0);
		}
		
		// get the user name from the login window
		username = loginGUI.get_username();
		
		// initializes XTEAM GUI or Simple GUI
		flameGUI = new SimpleGUI(this, "MRSV", "MRSV", xteamGUISwitch, getScreenLogger());
		//flameGUI = new SimpleGUI(this, "HeadLocalV", username, xteamGUISwitch, getScreenLogger());
		//flameGUI = new XTEAMGUI(this, username, xteamGUISwitch, getScreenLogger());
		
		// disables the buttons initially
		//flameGUI.disableButtons();
		
		// initializes in-progress simulation ID list mapping
		mapSimIDs.put("MRSV", new HashSet<Integer>());
		mapSimIDs.put("LSV", new HashSet<Integer>());
		mapSimIDs.put(username, new HashSet<Integer>());
		
		// send the login event
		sendLoginEvent(username, loginGUI.get_password());
		
		printMsg(name, "Connecting FLAME Adaptor ...");
		
		// launches GME automatically
 		try{
 			launchGME();
 		} catch(Exception e){
 			printMsg(name, "Error: " + e.toString());
 		}
		
        // manage connection with FLAME adaptor
        connectFLAME();
        
        printMsg(name, "Creating the KeepAlive thread ...");
        
        createKeepAlive();
        
		printMsg(name, "Bringing up the FLAME GUI ...");
		
		flameGUI.openShell();
		
		printMsg(name, "Finished the FLAME GUI");
		
		sendLogoutEvent(username);
		
	}
	
	/**
	 * Sends a Logout Event
	 * @param username	Username of the architecture
	 */
	 
	public void sendLogoutEvent(String username){
		// create the logout event
		Event logoutEvent = (Event) new Prism.core.Event("Logout"); //check if this constructor works fine
		
		logoutEvent.addParameter("SenderUsername", username); // check if this parameters are ok
		logoutEvent.addParameter("OriginComponent", 	name);
		
		// send the logout Event to the server
		sendRequest(logoutEvent);
	}
	
	/**
	 * Add port to the PortNumberTracker
	 * 
	 * @param username		Username of this component
	 * @param port			Port that is open
	 */
	protected void addPort(String username, int port) {
		printMsg(name, "Adding " + username + "'s port [" + port + "] to port tracker");
		ports.addPort(username, port);
	}
	
	/**
	 * Sets up the socket that talks with FLAME Adaptor
	 */
	protected void connectFLAME() {		
		
		ServerSocket 	serverSocket;			// the server socket that waits for FLAME adaptor
		Socket 			clientSocket;			// the socket that talks to FLAME adaptor
		
		// Creates a new socket
		try {
			// Opens a socket with a random port number
			serverSocket 	= new ServerSocket(0);
			
			// Stores the port number
			port			= serverSocket.getLocalPort();
			
			// Adds the port number to PortNumberTracker
			addPort(username, port);
		} catch(IOException e) {
			printMsg(name, "Error: Could not begin listening on port: " + port);
            return;
		}
		
		// Begins accepting FLAME Adaptor connection
		try {
			// Accepts the connection
            clientSocket = serverSocket.accept();
            clientSocket.setSoTimeout(0);
            printMsg(name, "Adaptor is connected [username: " + username + "]");
            
            // Sets up the output stream
            outAdaptor = new PrintWriter(clientSocket.getOutputStream(), true);
            
            // Creates and runs the EventReceiver thread
            createEventReceiver(clientSocket);
            
        } catch (IOException e) {
            printMsg(name, "Error: FLAME Adaptor accept failed");
            return;
            
        } finally {        	
        	// close the server socket
            try {
            	serverSocket.close();
            } catch (IOException ioe) {
            	printMsg(name, "Error: Cannot close the server socket");
            }
        }
	}
	
	/**
	 * Initiates and runs the EventReceiver thread
	 * 
	 * @param clientSocket	Socket that talks with FLAME Adaptor
	 */
	protected void createEventReceiver(Socket clientSocket) {
		eventReceiver = new EventReceiver(this, clientSocket);
		eventReceiver.setEventID(event_id);
		eventReceiver.start();
	}
	
	/**
	 * Initiates and runs the KeepAlive thread
	 */
	protected void createKeepAlive() {
		keepAlive ka = new keepAlive();
		ka.start();
	}
	
///////////////////////////////////////////////
// Dealing with files
///////////////////////////////////////////////	
	
	
	/**
	 * Copies a file; recursively invoked
	 * 
	 * @param copyFrom		Origin file path
	 * @param copyTo		Target file path
	 * @throws IOException	Cannot copy the file
	 */
	public void copySubTree(Path copyFrom, Path copyTo) throws IOException {
		try {
			Files.copy(copyFrom, copyTo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
		} catch (IOException e) {
			System.out.println("Unable to copy: " + copyFrom.toString());
		}
	}
	
///////////////////////////////////////////////
// Launching GME automatically
///////////////////////////////////////////////	
	
	/**
	 * Creates a directory for each run of CoWareClient or XTEAMEngine to
	 * copy the initial model file and open GME with the model file.
	 * 
	 * @return				Path to the newly created directory
	 * @throws Exception	Failed to create the directory
	 */
	public Path prepareModelDirectory() throws Exception{
		
		// The name for this run instance
		String 	randomName 	= Utility.convertDate(System.currentTimeMillis());
		Path 	randomPath 	= modelsDirPath.resolve(randomName);
		
		// if the models directory does not exist, creates one
		if(Files.notExists(modelsDirPath, LinkOption.NOFOLLOW_LINKS)) {
			try { 
				Files.createDirectories(modelsDirPath);
			} catch (Exception e) {
				throw new Exception ("The models directory cannot be created");
			}
		}
		
		// create the particular run instance's model directory
		// add a 000~Constants.ioIterations number at the end of the version in case there are
		// multiple run instances within the second
		boolean mkdirSucceeded = false;
		for(int i=0; i < Constants.ioIterations; i++) {
			String 	new_randomName = randomName + "_" + String.format("%03d", i);
			Path	new_randomPath = modelsDirPath.resolve(new_randomName);
			
			// in case the new version does not already exist
			if(Files.notExists(new_randomPath, LinkOption.NOFOLLOW_LINKS)) {
				try {
					Files.createDirectory(new_randomPath);
					mkdirSucceeded = true;
					
					randomName = new_randomName;
					randomPath = new_randomPath;
					break;
				} catch (FileAlreadyExistsException faee) {
					printMsg(name, faee.toString());
					if (i == Constants.ioIterations-1) {
						throw new FileAlreadyExistsException ("Model directory for instance [" + randomName + "] already exists");
					}
				} catch (Exception e) {
					if (i == Constants.ioIterations-1) {
						throw new Exception ("Model directory for instance [" + randomName + "] cannot be created: " + e);
					}
				}	
			}
		}
		
		if(mkdirSucceeded == false) {
			throw new Exception ("Model directory for instance [" + randomName + "] cannot be created");
		}
				
		return randomPath;
	}
	
	/**
	 * Copies the model file and launches GME with it
	 * 
	 * @throws Exception	Failed to copy the model file or to launch GME
	 */
	public void launchGME() throws Exception{		
		Path p1, p2;
		Path randomPath;
		
		// Computes and generates the model directory
		try {
			randomPath = prepareModelDirectory();
		} catch (Exception e) {
			throw e;
		}
		
		// computes the origin and target model file paths
		p1 = modelPath;
		p2 = randomPath.resolve(modelPath.getFileName());
		
		// copies the model file
		try {
			copySubTree(p1 , p2);
		} catch (IOException ioe) {
			throw new Exception ("Failed to copy the model file: " + ioe);
		}
		
		// launches GME with the model file
		try {
			//String[] compileMiniCmd	= {"cmd", "/c", "start", "/min", GMEPath.toString(), p2.toString()};
			String[] compileMiniCmd	= {GMEPath.toString(), p2.toString()};
			String[] compileCmd 	= {GMEPath.toString(), p2.toString()};
			
			if(name.equals("XTEAM Engine")) {
				// launches GME as minimized
				modelingTool = Runtime.getRuntime().exec(compileMiniCmd);
			} else {
				// launches GME normally
				modelingTool = Runtime.getRuntime().exec(compileCmd);
			}
		} catch (Exception e) {
			throw new Exception ("Failed to launch GME: " + e);
		}
	}
	
	
///////////////////////////////////////////////
//	Handling incoming Events
///////////////////////////////////////////////	
	
	/**
	 * Handles incoming Events
	 * 
	 * @param e					The incoming Event
	 */
	public void handle(Event e) {
		
		// check if this architect is supposed to receive this event
		if(is_this_for_me(e, username)) {
			
			// the event name of the Event
			String eventName = (String) e.name;
			
			// print the received message to screen
			printEvent(e);
			
			switch (eventName) {
			
				//////////////////////////////////
				// handling Design and Snapshot events
				//////////////////////////////////
			
				case "Design":
				case "Update":

					// store the design event in the buffer
					storage.addToBuffer(e);
					
					/* If the Event has IsLast parameter, that means the Event is a part 
					 * of the initial update Events.
					 * 
					 * Enables the Update button
					 */
					if (e.hasParameter("IsLast")) {
						flameGUI.enableUpdateButton();
					}
					
					break;
					
				case "Snapshot":
					
					// store the design event in the buffer
					storage.addToBuffer(e);
					
					// show it at the Snapshots History field
					flameGUI.presentSnapshot(e);
					
					// show it on the Latest Versions field 
					flameGUI.updateLV(storage.getSnapshots());
					
					// enables the update button, only when the Snapshot is not from self
					if (e.hasParameter("SenderUsername")) {
						String senderUsername = (String) e.getParameter("SenderUsername");
						if(!senderUsername.equals(username)) {
							flameGUI.enableUpdateButton();
						}
					} else {
						printMsg(name, "Error: a Snapshot does not have SenderUsername");
					}
					
					
					
					break;
							
				//////////////////////////////////
				// handling Init events
				//////////////////////////////////
					
				case "Init":
					
					// update the local Event ID
					if(e.hasParameter("EventID")) {
						// set the local EventID number to the given number by the server
						event_id = (Integer) e.getParameter("EventID");
						
						// just in case the thread is already on
						if(eventReceiver != null) {
							eventReceiver.setEventID(event_id);
						}
					}
										
					break;
					
					
				//////////////////////////////////
				// handling XTEAM events
				//////////////////////////////////
				
				case "XTEAM":
					
					handleXTEAMEvent(e);
					
					break;
					
				//////////////////////////////////
				// handling XTEAM events
				//////////////////////////////////
				
				case "Finish":
					if(e.hasParameter("ReceiverUsername")){
						String receiverUsername="";
						receiverUsername = (String) e.getParameter("ReceiverUsername"); 
						if(receiverUsername.equals(username)) {
							getModelingTool().destroy();
							System.exit(0);
						}
					}
					break;
					
				//////////////////////////////////
				// handling Notification events -- presents notification message on the GUI
				//////////////////////////////////
					
				case "Notification":
					
					// gets SenderUsername, Value, and EventID
					if(!e.hasParameter("SenderUsername")) {
						printMsg(name, "Error: Notification Event does not have SenderUsername");
						return;
					} else if (!e.hasParameter("Value")) {
						printMsg(name, "Error: Notification Event does not have Value");
						return;
					} else if (!e.hasParameter("EventID")) {
						printMsg(name, "Error: Notification Event does not have EventID");
						return;
					}
					
					String 	value			= (String) e.getParameter("Value");
					String 	senderUsername 	= (String) e.getParameter("SenderUsername");
					int 	eventID			= (Integer) e.getParameter("EventID");
					
					if(!mapSimIDs.containsKey(senderUsername)) {
						printMsg(name, "Error: Notification event has an unknown SenderUsername: " + senderUsername);
						return;
					}
					
					// increase/decrease thread number
					switch (value) {
						// a simulation has begun
						case "Simulation execution beginning":
							mapSimIDs.get(senderUsername).add(eventID);
						break;
						
						// a simulation has completed
						case "Simulation execution completion":
							mapSimIDs.get(senderUsername).remove(eventID);
							break;
							
						default:
							printMsg(name, "Error: Notification Event carries unparsable Value: " + value);
							break;
					}
					
					// updates GUI
					flameGUI.setSimStatus(senderUsername, mapSimIDs.get(senderUsername).size());
					
					break;
					
				//////////////////////////////////
				// Error handling
				//////////////////////////////////
					
				default:
					
					printMsg(name, "Error: Received incomprehensible Event [" + e.name + "]");
					break;	
			}
		}
	}

///////////////////////////////////////////////
//	Present XTEAM analysis information to XTEAM GUI
///////////////////////////////////////////////
	
	/**
	 * Handles the XTEAM Event and presents the information to XTEAM GUI
	 * @param e				The Received Event to be presented
	 */
	protected void handleXTEAMEvent(Event e) {
		
		// Checks if the passed Event is an XTEAM Event
		if (!e.name.equals("XTEAM")) {
			printMsg(name, "A non-XTEAM Event has been passed to handleXTEAMEvent method");
			return;
		}
		
		// present the info on the screen
		flameGUI.presentXTEAMInfo(e.replicate());
	}
	
	/**
	 * Keep alive thread
	 * 
	 * @author Jae young Bang
	 */
	protected class keepAlive extends Thread {
		public void run () {
			while(true) {
				sendPacket();
				waitForNextPacket();
			}
		}
		
		protected void waitForNextPacket () {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// do nothing
			}
		}
		
		protected void sendPacket () {
			// Creates a Snapshot Event and sends it over
			Event evt = (Event) new Event("KeepAlive");
			evt.addParameter("OriginComponent", name);
			evt.addParameter("SenderUsername", username);
			sendRequest(evt);
		}
	}
	
///////////////////////////////////////////////
// Incoming from the FLAME Adaptor
///////////////////////////////////////////////

	/**
	 * Thread class that receives new Events from FLAME Adaptor, creates a new PrismMW Event accordingly, and sends it to the {@link flame.Server.CoWareXTEAMServer}
	 * 
	 * @author 				<a href="mailto:jaeyounb@usc.edu">Jae young Bang</a>
	 * @version				2013.05
	 */
	protected class EventReceiver extends Thread {
		
		
		/**
		 * Socket that talks with the FLAME adaptor
		 */
		protected Socket 	clientSocket;
		
		/**
		 * Last EventID -- the local model version
		 */
		protected int		event_id;
		
		/**
		 * The parent Client
		 */
		protected FLAMEClient client;
		
		/** 
		 * Default constructor
		 * 
		 * @param socket	Socket that talks with FLAME Adaptor
		 */
		public EventReceiver(FLAMEClient client, Socket socket) {
			this.client			= client;
			clientSocket 		= socket;
			event_id 			= 0;
		}
		
		/**
		 * Sets the last EventID variable
		 * 
		 * @param event_id	Last EventID
		 */
		public void setEventID(int eid) {
			getLock();
			event_id = eid;
			releaseLock();
		}
		
		/**
		 * Increases the last EventID variable
		 * 
		 * @return			Last EventID			
		 */
		public int tickEventID() {
			int eid;
			
			getLock();
			eid = event_id++;
			releaseLock();
			
			return eid;
		}
		
		/**
		 * Runs thread that receives Events from FLAME Adaptor
		 */
		public void run() {
			
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				
				while (true) {
					String inputLine = SocketTransferUtility.read(in);
					
					// checks if the last event has been applied to the local model
					Event lastEvent = client.getLastEvent();
					if(lastEvent != null) {
						String value = (String) lastEvent.getParameter("Value");
						
						// if a match, that means the application has been completed
						if(value.equals(inputLine)) {
							// changes the status message
							client.flameGUI.setIdleStatus("Ready");
							
							// empties the last event
							client.emptyLastEvent();
							
							// enables buttons
							//client.flameGUI.enableButtons();
							
							// releases the button semaphore
							client.releaseLock();
						} 
					} 
					
					// echoing event checking from FLAME Adaptor
					if(!eventHistory.isEchoing(inputLine)) {
						Event gmeEvent = (Event) new Event("Design");
						gmeEvent.addParameter("OriginComponent", 	name);
						gmeEvent.addParameter("SenderUsername", 	username);
						gmeEvent.addParameter("Value", 				inputLine);
						gmeEvent.addParameter("EventID", 			new Integer(tickEventID()));
						
						printMsg(name, "Sending [event_id:" + event_id + "] to server");
						sendRequest(gmeEvent);
						client.flameGUI.enableCommitButton();
						storage.updateTrackersForOutgoingEvent(gmeEvent);
					} else {
						//printMsg(name, "Echoing Event Dropped");
					}
				}
				
			} catch (IOException ioe) {
				printMsg(name, "Error: Failed to initialize BufferedReader.");
			} catch (Exception e) {
				printMsg(name, e.toString());
			} finally {
				try {
					clientSocket.close();
				} catch (IOException ioe) {
					printMsg(name, "Error: Failed to close the socket with FLAME Adaptor.");
				}	
			}
		}
		
		
		/**
		 * EventID semaphore
		 */
		protected final Semaphore mSemaphore = new Semaphore (1, true);
		
		/**
		 * Locks the EventID semaphore
		 */
		protected void getLock() {
			try {
				mSemaphore.acquire();	// get the semaphore
			} catch (InterruptedException ie) {
				printMsg(name, "Thread interrupted while waiting for the semaphore");
			}
		}
		
		/**
		 * Releases the EventID semaphore
		 */
		protected void releaseLock() {
			mSemaphore.release();
		}
	}
	
///////////////////////////////////////////////
// Outgoing to the FLAME Adaptor
///////////////////////////////////////////////
	
	/**
	 * Forward one Event to FLAME Adaptor
	 * 
	 * @param e				Event to be forwarded to FLAME Adaptor
	 */
	protected void forwardEventToAdaptor (Event e) {
		
		// check if it is a Design decision event
		if(!e.name.equals("Design") && !e.name.equals("Init")) {
			printMsg(name, "Error: A [" + e.name + "] Event cannot be forwarded to FLAME Adaptor");
			return;
		}
		
		// get the Value parameter
		if(!e.hasParameter("Value")) {
			printMsg(name, "Error: A Design Event does not have the Value parameter");
			return;
		}
		String value = (String) e.getParameter("Value");
		
		String senderUsername = "";
		if(e.hasParameter("SenderUsername")) {
			senderUsername = (String) e.getParameter("SenderUsername");
		}
		
		Integer eventID = new Integer (-1);
		if(e.hasParameter("EventID")) {
			eventID = (Integer) e.getParameter("EventID");
		}
		
		// insert the event to the event queue to prevent echoing event from FLAME Adaptor
		if(e.name.equals("Design")) {
			eventHistory.insertEvent(e);
		}

		// wait until the socket connection goes live
		for( int loop_cnt = 1; outAdaptor == null; loop_cnt++ ) {
			
			try {
				Thread.sleep(1000);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			
			String cnt_msg = "[" + loop_cnt;
			if(loop_cnt == 1) {
				cnt_msg += " second";
			} else {
				cnt_msg += " seconds";
			}
			
			printMsg(name, cnt_msg + "] Waiting for FLAME Adaptor ...");
		}
		
		try {
			SocketTransferUtility.write(outAdaptor, value);
			printMsg(name, "Forwarded ["+senderUsername+"/EID:"+eventID+"/flag:" + value.substring(0, 1) + "] to Adaptor");
		} catch (Exception exc) {
			printMsg(name, exc.toString());
		}
	}
	
	/**
	 * Forwards multiple Events to the FLAME Adaptor<p>
	 * 
	 * Calls {@link #forwardEventToAdaptor(Event)} iteratively
	 * 
	 * @param events		List of Events to be forwarded to FLAME Adaptor
	 */
	protected void forwardEventsToAdaptor (ArrayList<Event> events) {
		
		getLock_OutBufferToAdaptor();
		
		for(Event e : events) {
			forwardEventToAdaptor(e);
		}
		
		releaseLock_OutBufferToAdaptor();
	}

	
	/**
	 * Binary semaphore for outgoing Events forwarded to the FLAME Adaptor<p>
	 * 
	 * There could be several threads created by Prism
	 * for incoming Events from FLAME Server to forward
	 * them to FLAME Adaptor. This semaphore makes the
	 * threads synchronized to make sure the Events going
	 * into the Adaptor ordered by their EventIDs.
	 */
	protected final Semaphore mLockOutBufferToAdaptor = new Semaphore (1, true);
	
	/**
	 * Locks the outgoing Events semaphore
	 */
	protected void getLock_OutBufferToAdaptor() {
		try {
			mLockOutBufferToAdaptor.acquire();	// get the semaphore
		} catch (InterruptedException ie) {
			printMsg(name, "Thread interrupted while waiting for the semaphore");
		}
	}
	
	/**
	 * Releases the outgoing Event semaphore
	 */
	protected void releaseLock_OutBufferToAdaptor() {
		mLockOutBufferToAdaptor.release();
	}

	
///////////////////////////////////////////////
// Dealing with Buffer and Snapshots
///////////////////////////////////////////////
	
	/**
	 * Purges and applies the Event buffer to the LSV + all Events from this Architect
	 * 
	 * @return 				Purged Events from the buffer
	 */
	public ArrayList<Event> purgeBuffer() {
		ArrayList<Event> newEvents = storage.purgeBufferForArchitect(username);
		return newEvents;
	}
	
	/**
	 * Purges and applies the entire Event buffer
	 * 
	 * @return				Purged Events from the buffer
	 */
	public ArrayList<Event> purgeBufferAll() {
		ArrayList<Event> newEvents = storage.purgeBufferAll();
		return newEvents;
	}
	
	/**
	 * Purges the Event buffer (applying to the local model) and takes a Snapshot<p>
	 * 
	 * This method is invoked when the Architect presses the Snapshot button on the GUI window.
	 */
	public void takeSnapshot () {
		
		// purges the buffer first
		ArrayList<Event> listEvents = filterEvents(purgeBuffer());
		
		// only if the buffer had any Event
		if (listEvents.size() > 0) {
			
			// locks the buttons
			getLock();
			
			// sets the status bar message
			flameGUI.setInProgressStatus("Forwarding " + listEvents.size() + " Events to FLAME Adaptor ... DO NOT UPDATE MODEL WHILE FORWARDING.");
			
			// disables the buttons
			//flameGUI.disableButtons();
			
			// finds the last Event
			lastEvent = listEvents.get(listEvents.size() - 1);
			
			// forwards the Events to FLAME Adaptor
			forwardEventsToAdaptor(listEvents);
		}
		
		// Commits (by sending a Snapshot event)
		commit();
	}
	
	/**
	 * Purges the buffer to update 
	 */
	public void update () {
		// disables the update button to avoid updating with nothing to update
		flameGUI.disableUpdateButton();
		
		// purges the buffer first
		ArrayList<Event> listEvents = filterEvents(purgeBuffer());
		
		// only if the buffer had any Event
		if (listEvents.size() > 0) {
			
			// locks commit/update
			getLock();
			
			Integer eid = 0;
			// only if the event receiver (connected to FLAME Adaptor) is running
			if(eventReceiver != null) {
				// tick the event ID and get the number
				eid = eventReceiver.tickEventID();
			}
			
			// Creates an Update Event and sends it over
			Event evt = (Event) new Event("Update");
			evt.addParameter("OriginComponent", name);
			evt.addParameter("SenderUsername", username);
			evt.addParameter("EventID", new Integer(eid));
			addEmptyValue(evt, "Update");
			sendRequest(evt);
			
			// sets the status bar message 
			flameGUI.setInProgressStatus("Forwarding " + listEvents.size() + " Events to FLAME Adaptor ... DO NOT UPDATE MODEL WHILE FORWARDING.");
			
			// disables the buttons
			//flameGUI.disableButtons();
			
			// finds the last Event
			lastEvent = listEvents.get(listEvents.size() - 1);
			
			// forwards the Events to FLAME Adaptor
			forwardEventsToAdaptor(listEvents);
		}
	}
	
	/**
	 * Commits at this point by sending a Snapshot event.<p>
	 * 
	 * This method is invoked when the Architect presses the Commit button on the GUI window.
	 */
	public void commit() {
		
		// disables the commit button to avoid committing with no new change
		flameGUI.disableCommitButton();
		
		/* checks whether the base version is behind the repository
		 * If the Update button is enabled, that means there is something to update, which
		 * then means the base version is behind the repository version. Gotta update first!
		 */
		if(flameGUI.getUpdateButtonStatus()) {
			flameGUI.popMessageDialog("Commit Warning", "Your base version is behind the repository version. FLAME recommends you perform Update prior to Commit.");
		}
		
		// pops up a dialog asking whether the architect wants to 
		int commitChoice = flameGUI.popQuestionDialog("Commit", "Do you really want to commit your changes?");
		if(commitChoice == SWT.OK) {
			printMsg(name, "Commit question YES pressed.");
			
			Integer eid = 0;
			// only if the event receiver (connected to FLAME Adaptor) is running
			if(eventReceiver != null) {
				// tick the event ID and get the number
				eid = eventReceiver.tickEventID();
			}
			
			// Creates a Snapshot Event and sends it over
			Event evt = (Event) new Event("Snapshot");
			evt.addParameter("OriginComponent", name);
			evt.addParameter("SenderUsername", username);
			evt.addParameter("EventID", new Integer(eid));
			addEmptyValue(evt, "Snapshot");
			sendRequest(evt);
			
			/* disables the commit button again just in case the architect has made new changes 
			 * while the commit question window is on
			 */
			flameGUI.disableCommitButton();
		} else {
			printMsg(name, "Commit question NO pressed.");
			flameGUI.enableCommitButton();
		}
	}
	
	/**
	 * Presents the stored LocalV analysis information<p>
	 * 
	 * This method is invoked when the Check Local button is pressed.
	 */
	public void presentLocalInfo() {
		flameGUI.presentLocalInfo();
		
		// creates a CheckLocal Event and sends it over
		Event evt = (Event) new Event("CheckLocal");
		evt.addParameter("OriginComponent", name);
		evt.addParameter("SenderUsername", username);
		sendRequest(evt);
	}
	
	/**
	 * Adds an empty Value parameter to Event
	 * 
	 * @param e				Target Event
	 * @param type			Operation type
	 */
	public void addEmptyValue(Event e, String type) {
		e.addParameter("Value", manipulateEmptyValue(type));
	}
	
	/**
	 * Manipulates an empty Value parameter string
	 * 
	 * @param type			Operation type
	 * @return				An empty Value string
	 */
	public static String manipulateEmptyValue(String type) {
		return new String (			"X" 		+ 
									delimiter 	+ "1,1,1,1" 	+ 
									delimiter 	+ type 			+ 
									delimiter 	+ delimiter 	+ delimiter +													
									delimiter 	+ delimiter 	+ delimiter);
	}
	

	/**
	 * Gets the process handle of the modeling tool
	 * @return				Process handle of the modeling tool
	 */
	public Process getModelingTool() {
		return modelingTool;
	}
	
///////////////////////////////////////////////
//The main() method and the helper methods
///////////////////////////////////////////////

	
	/**
	 * Configuration properties read from the config.properties file
	 */
	public static Properties props = new Properties();
	
	/**
	 * Loads the configuration properties from the config.properties file
	 * 
	 * @throws Exception	An error occurred with opening the config.properties file
	 */
	public static void loadProps() throws Exception {
		try {
			props.load(new FileInputStream(new File("config.properties")));
		} catch (FileNotFoundException e) {
			throw new Exception ("config.properties is missing");
		} catch (IOException e) {
			throw new Exception ("config.properties cannot be read");
		}		
	}
	
	/**
	 * The main() method
	*/
	public static void main(String[] args) {
		
		// the application name
		Display.setAppName("FLAME Client");
		
		// load up the properties specified in the config.properties file
		try {
			loadProps();
		} catch (Exception e) {
			System.out.println("[FLAME Client]: Error: " + e.getMessage());
			return;
		}
		
		// gets properties from config.properties
		Path 	modelPath;
		Path 	modelsDirPath;
		Path	GMEPath;
		int 	tracker_port;
		boolean	xteamGUISwitch;
		try {
			modelPath 		= Utility.getPathProperty(props, "MODEL_FILE_PATH");
			modelsDirPath	= Utility.getPathProperty(props, "MODEL_FILE_DIR_PATH");
			GMEPath			= Utility.getPathProperty(props, "GME_EXE_PATH");
			tracker_port 	= Utility.getIntProperty(props, "TRACKER_PORT");
			xteamGUISwitch	= Utility.getBooleanProperty(props, "XTEAM_GUI");
		} catch (Exception e) {
			System.out.println("[FLAME Client]: Error: " + e.toString());
			return;
		}
		
		// gets the first main argument
		if(args.length >= 1) {
			String xteamGUIArg = args[0].toLowerCase();
			if(xteamGUIArg.equals("yes")) {
				xteamGUISwitch = true;
			} else if (xteamGUIArg.equals("no")) {
				xteamGUISwitch = false;
			}
			
			System.out.println("arg[0]: " + args[0] + "XTEAM GUI argument:" + xteamGUISwitch);
		}
		
		
		
		// Build Prism-MW framework
		FIFOScheduler sched 			= new FIFOScheduler(100); 
		RRobinDispatcher disp 			= new RRobinDispatcher(sched, 10);
		Scaffold s 						= new Scaffold();
		s.dispatcher 					= disp;
		s.scheduler 					= sched;
		
		Architecture arch		 		= new Architecture("FLAME");
		arch.scaffold					= s;
		
		// Create components
		AbstractImplementation gme_t; 	
		
		try {
			gme_t = new FLAMEClient(	"FLAME Client", 
											modelPath, 
											modelsDirPath, 
											GMEPath, 
											tracker_port,
											xteamGUISwitch);
		} catch (Exception e) {
			System.out.println("Error: " + e);
			return;
		}
		
		Component gme 					= new Component("FLAME Client", gme_t);
		gme.scaffold 					= s;
		
		Connector eBus 					= new Connector("Event Bus");
		eBus.scaffold 					= s;
		
		// Add components
		arch.add(gme);
		arch.add(eBus);
		
		// Connect components and establish ports
		Port gmeRequestPort 			= new Port("gmeRequestPort", PrismConstants.REQUEST);
		gme.addCompPort(gmeRequestPort);
		
		Port eBusReplyPort 				= new Port("eBusReplyPort", PrismConstants.REPLY);
		eBus.addConnPort(eBusReplyPort);
		
		arch.weld(gmeRequestPort, eBusReplyPort);
		
		/*
		 *  Setup distribution components
		 *  Since each Prism instance on each side is a peer, both have will have
		 *  both server and client ports which connect to each other in 2 channels
		 */ 
		
		// Client socket
		ExtensiblePort epClient 		= new ExtensiblePort("epClient", PrismConstants.REQUEST);
		SocketDistribution sd 			= new SocketDistribution(epClient);
		
		epClient.addDistributionModule(sd);
		epClient.scaffold = s;
		
		// Add to appropriate places
		eBus.addConnPort(epClient);
		arch.add(epClient);
		
		// Connect to appropriate places
		epClient.connect(props.getProperty("SERVER"), Integer.parseInt(props.getProperty("PORT")));
		
		// Start dispatcher and architecture
		disp.start();
		arch.start();
		
	}
}
