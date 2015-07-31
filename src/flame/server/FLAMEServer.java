package flame.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.eclipse.swt.widgets.Display;

import flame.AbstractImplementationModified;
import flame.Constants;
import flame.EventStorage;
import flame.Tracker;
import flame.Utility;
import flame.EventStorage.EventComparator;
import Prism.core.*;
import Prism.extensions.port.ExtensiblePort;
import Prism.extensions.port.distribution.Connection;
import Prism.extensions.port.distribution.SocketDistribution;

/**
 * FLAMEServer stores all incoming Events, and send them to newly 
 * joining FLAME clients and conflict detection engines. <p>
 * 
 * FLAMEServer tracks the Event ID from each architect, and tell 
 * the architects who resume their sessions the next EventID they 
 * are supposed to use. <p>
 * 
 * @author 					<a href="mailto:jaeyounb@usc.edu">Jae young Bang</a>
 * @version					2013.05
 */
public class FLAMEServer extends AbstractImplementationModified {
	
	///////////////////////////////////////////////
	// Member Variables
	///////////////////////////////////////////////
	
	/**
	 *  Socket connection information, set by invoking setSocketDistribution()
	 */
	private SocketDistribution 		sockets 			= null;
	
	/** 
	 * EventStorage instance that stores all incoming events
	 */
	private EventStorage 			storage				= new EventStorage(getScreenLogger());
	
	/**
	 * Logger instance that logs information about incoming Events
	 */
	private Logger 					logger				= null;
	
	/**
	 * A component that listens to the port number requests from FLAME Adaptor
	 */
	protected PortNumberTracker		ports;
	
	/**
	 * MRSV engine switch -- if it is true, MRSV Detection Engine gets turned on
	 */
	protected boolean				isMRSVOn			= false;
	
	/**
	 * LSV engine switch -- if it is true, LSV Detection Engine gets turned on 
	 */
	protected boolean				isLSVOn				= false;
	
	/**
	 * LocalV engine switch -- if it is true, LocalV Detection engine gets turned on
	 */
	protected boolean				isLocalVOn			= false;
	
	/**
	 * HeadLocalV engine switch -- if it is true, HeadLocalV Detection engine gets turned on
	 */
	protected boolean				isHeadLocalVOn		= false;
	
	
	///////////////////////////////////////////////
	// Constructors
	///////////////////////////////////////////////
	
	/**
	 * Constructor with the logging option <b>off</b>
	 * 
	 * @param tracker_port		Port number PortNumberTracker uses
	 * @param isMRSVon			MRSV engine switch
	 * @param isLSVOn			LSV engine switch
	 * @param isLocalVon		LocalV engine switch
	 * @param isHeadLocalVOn	HeadLocalV engine switch
	 */
	public FLAMEServer(	int 	tracker_port, 
						boolean isMRSVOn, 
						boolean isLSVOn, 
						boolean isLocalVOn,
						boolean isHeadLocalVOn) throws Exception { 
		super("FLAME Server");
		
		// The port number tracker
		ports 				= new PortNumberTracker(tracker_port, getScreenLogger());
	
		// Turns switches
		this.isMRSVOn 		= isMRSVOn;
		this.isLSVOn		= isLSVOn;
		this.isLocalVOn		= isLocalVOn;
		this.isHeadLocalVOn	= isHeadLocalVOn;
	}
	
	/**
	 * Constructor with the logging option <b>on</b>
	 *  
	 * @param logPath			Path to the directory in which the log files are stored
	 * @param tracker_port		Port number PortNumberTracker uses
	 * @param isMRSVon			MRSV engine switch
	 * @param isLSVOn			LSV engine switch
	 * @param isLocalVon		LocalV engine switch
	 * @param isHeadLocalVOn	HeadLocalV engine switch
	 */
	public FLAMEServer(	String 	logPath, 
						int 	tracker_port, 
						boolean isMRSVOn, 
						boolean isLSVOn, 
						boolean isLocalVOn,
						boolean isHeadLocalVOn) throws Exception {
		
		// call the default constructor
		this (	tracker_port, 
				isMRSVOn, 
				isLSVOn, 
				isLocalVOn,
				isHeadLocalVOn);
		
		// create the logger
		try {
			logger = new Logger(logPath, getScreenLogger());
		} catch (Exception e) {
			printMsg(name, "Error: " + e.toString());
			logger = null;
		}
			
	}
		
	///////////////////////////////////////////////
	// Member Methods
	///////////////////////////////////////////////
	
	@Override
	public void start() {
		// launches the non-architect-specific Detection Engines (MRSV and LSV) automatically
		try{
			if(isMRSVOn) {
				launchDetectionEngine("MRSV");
			}
			if(isLSVOn) {
				launchDetectionEngine("LSV");
			}
		} catch(Exception e){
			printMsg(name, "Error: " + e.toString());
		}
	}
		
	/**
	 * Handles incoming Events
	 * 
	 * @param e					The incoming Event
	 */
	@Override
	public void handle(Event e) {
		
		// FLAME Server is supposed to receive only request events
		if(e.eventType == PrismConstants.REQUEST) {		
			
			// log the event
			if(logger != null) {
				try {
					logger.createLog(e);
				} catch (Exception exc) {
					printMsg(name, "Error: Writing a log failed: " + exc.toString());
				}
			}
			
			// print the event to the screen
			printEvent(e);
			
			// get the OriginComponent
			String originComponent = "";
			if(e.hasParameter("OriginComponent")) {
				originComponent = (String) e.getParameter("OriginComponent");
			} else {
				printMsg(name, "Error: Received Event does not have the OriginComponent parameter");
				return;
			}
		
			String 	eventName 		= e.name;		// the received event name
			Event 	evt				= null;			// the new event to send back to clients
			String 	senderUsername;					// SenderUsername parameter
			int 	port;							// Port parameter
		
			switch (eventName) {
				
				///////////////////////////////////////////////////
				// Handling Logout event
				///////////////////////////////////////////////////
	
				case "Logout":
						//here is the code to logout
						String loggedOutUsername="";
						if(e.hasParameter("SenderUsername")) {
							loggedOutUsername = (String) e.getParameter("SenderUsername");
						} else {
							printMsg(name, "Error: Received Logout with no SenderUsername parameter");
							break;
						}
	
						printMsg(name, "[" + loggedOutUsername + "] has logged out.");
						
						evt = new Event ("Finish");
						evt.addParameter("SenderUsername", 		name);
						evt.addParameter("OriginComponent", 	name);
						evt.addParameter("ReceiverUsername", 	loggedOutUsername);
						sendReply(evt);
			
				break;
				
				///////////////////////////////////////////////////
				// Handling Login event
				///////////////////////////////////////////////////
	
			
				case "Login":
					
					// retrieve the connection info (hostname, port number)
					Connection lastConnection = (Connection) sockets.getConnections().get(sockets.getConnections().size()-1);
					
					// gets the SenderUsername
					if(e.hasParameter("SenderUsername")) {
						senderUsername = (String) e.getParameter("SenderUsername");
					} else {
						printMsg(name, "Error: Received Login without SenderUsername");
						return;
					}
					
					// retrieves the next EventID and Snapshot
					int event_id = storage.getNextEventID(senderUsername);
					int snapshot = storage.getSnapshot(senderUsername);
					
					printMsg(name, 	"New login from " + lastConnection.getHost() + ":" + lastConnection.getPort() + Constants.endl +
									"\tOriginComponent:\t" + originComponent + Constants.endl +
									"\tSenderUsername:\t\t" + senderUsername + Constants.endl +
									"\tEventID:\t\t" + event_id + Constants.endl +
									"\tSnapshot:\t\t" + snapshot);
					
					// sends an Init event to the client to sync the EventID
					evt = new Event ("Init");
					evt.addParameter("SenderUsername", 		name);
					evt.addParameter("OriginComponent", 	name);
					evt.addParameter("ReceiverUsername", 	senderUsername);
					evt.addParameter("ReceiverComponent", 	originComponent);
					evt.addParameter("EventID", 			event_id);
					sendReply(evt);
					
					// launches the LocalV Detection Engine for the architect if the Login Event is from FlameClient
					if (originComponent.equals("FLAME Client")) {
						try {
							if(isLocalVOn) {
								launchDetectionEngine("LocalV", senderUsername);
							}
							if(isHeadLocalVOn) {
								launchDetectionEngine("HeadLocalV", senderUsername);
							}
						} catch (Exception exc) {
							printMsg(name, "Error: " + exc.toString());
						}
					}
					
					// manipulates a version (set of EventIDs) for this engine
					Map<String, Tracker> version;
					version = storage.getEventIDTrack();
					
					// retrieves Event history
					ArrayList<Event> history = storage.getEventsFromHistory(version);
					
					// clean up the history before sending
					for(Event evtHistory : history) {
						// manipulates the Event
						evtHistory.removeParameter("OriginComponent");
						evtHistory.removeParameter("ReceiverUsername");
						evtHistory.removeParameter("ReceiverComponent");
						evtHistory.removeParameter("IsBroadcast");
						evtHistory.removeParameter("IsLast");
					}
					
					// sorts the history before sending
					Collections.sort(history, EventComparator.ascending (EventComparator.getComparator (EventComparator.EVENT_ID_SORT)));
					
					// tags the last Design event as the last Design event
					boolean found = false;
					for(int i = history.size()-1; i >= 0 ; i--) {
						Event evtHistory = history.get(i);
						if(!found && evtHistory.name.equals("Design")) {
							found = true;
							evtHistory.addParameter("IsLast", new Boolean (true));
						} else {
							evtHistory.addParameter("IsLast", new Boolean (false));
						}
					}
					
					// sends the history
					for(Event evtHistory : history) {
						evtHistory.addParameter("OriginComponent", name);
						evtHistory.addParameter("ReceiverUsername", senderUsername);
						evtHistory.addParameter("ReceiverComponent", originComponent);
														
						// send
						sendReply(evtHistory);
					}
				
					break;
					
				///////////////////////////////////////////////////
				// Handling Snapshot, Design, and XTEAM event
				///////////////////////////////////////////////////
					
				case "Snapshot":
				case "Design":
				case "XTEAM":
				case "Notification":
				case "Update":
					
					// stores the event in the storage
					evt = e.replicate();
					
					if(!eventName.equals("Notification")){
						storage.addToHistory(evt);	
					}
						
					// manipulates the Event
					evt.removeParameter("ReceiverUsername");
					evt.removeParameter("IsBroadcast");
					
					evt.addParameter("IsBroadcast", new Boolean (true));
					evt.addParameter("AbsoluteTime", Utility.convertDate(System.currentTimeMillis()));
					
					// broadcasts
					sendReply(evt);
					
					break;
					
				///////////////////////////////////////////////////
				// Handling Port event
				///////////////////////////////////////////////////
					
				case "Port":
					
					// adds the port to PortNumberTracker
					if(!e.hasParameter("Port")) {
						printMsg(name, "Error: Failed to find the Port parameter from a Port Event.");
						break;
					} else if(!e.hasParameter("SenderUsername")) {
						printMsg(name, "Error: Failed to find the SenderUsername parameter from a Port Event.");
						break;
					}
					
					port 			= (Integer) e.getParameter("Port");
					senderUsername	= (String)	e.getParameter("SenderUsername");
					
					ports.addPort(senderUsername, port);
					
					break;
			}	
		}
	}
	
	/**
	 * Sets the SocketDistribution instance
	 * 
	 * @param sd			The SocketDistribution instance from the ExtensiblePort
	 */
	public void setSocketDistribution(SocketDistribution sd) {
		sockets = sd;
	}
	
	/**
	 * Launches a global Detection Engine
	 * 
	 * @param mode			Detection Engine mode
	 * @throws Exception	Cannot launch the Detection Engine
	 */
	public void launchDetectionEngine(String mode) throws Exception {
		if(mode.equals("LocalV") || mode.equals("HeadLocalV")) {
			throw new Exception (mode + " Detection Engine requires the username argument.");
		} else {
			launchDetectionEngine(mode, mode);
		}
	}
	
	/**
	 * Launches an Detection Engine
	 * 
	 * @param mode			Detection Engine mode (e.g. MRSV, LSV, or LocalV)
	 * @param username		Corresponding username of architect, mostly used for LocalV
	 * @throws Exception	Cannot launch the Detection Engine
	 */
	public void launchDetectionEngine(String mode, String username) throws Exception {
		
		// throws an exception if the mode is not one of the three supported modes
		if(	!mode.equals("MRSV") 	&& 
			!mode.equals("LSV") 	&& 
			!mode.equals("LocalV")	&&
			!mode.equals("HeadLocalV")) {
			throw new Exception ("The given Detection Engine mode [" + mode + "] is not supported.");
		}
		
		// launches the Detection Engine
		try{
			//Runtime.getRuntime().exec("cmd /c start /min ant -Dusername=" + username + " XTEAMEngine" + mode);
			Runtime.getRuntime().exec("cmd /c start /min XTEAMEngine.bat " + username + " " + mode);
		} catch(Exception e){
			throw new Exception ("Auto-launching XTEAMEngine[mode:" + mode + "/username:" + username + "] failed: " + e);
		}
		
		// prints out the success message
		printMsg(name, "Detection Engine [mode:"+ mode + "/username:" + username + "] has been launched");
	}
	
	
	
	///////////////////////////////////////////////
	// The main() method and the helper methods
	///////////////////////////////////////////////
	
	/**
	 * Configuration properties read from the config.properties file
	 */
	public static Properties 	props 	= new Properties();
	
	
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
		
		// sets the application name
		Display.setAppName("FLAME Server");
		
		// loads up the properties specified in the config.properties file
		try {
			loadProps();
		} catch (Exception e) {
			System.out.println("[FLAME Server]: Error: " + e.getMessage());
			return;
		}
		
		// gets the PortNumberTracker port number 
		int tracker_port = 0;  
		try {
			tracker_port = Integer.parseInt(props.getProperty("TRACKER_PORT"));  
		} catch (NumberFormatException nfe) {
			System.out.println("[FLAME Server]: Error: FLAMEClient requires TRACKER_PORT property in config.properties.");
			return;
		}
		
		// gets the switches
		boolean	isMRSVOn 			= switchCheck(props, "MRSV_SWITCH");
		boolean	isLSVOn 			= switchCheck(props, "LSV_SWITCH");
		boolean	isLocalVOn 			= switchCheck(props, "LOCALV_SWITCH");
		boolean isHeadLocalVOn		= switchCheck(props, "HEADLOCALV_SWITCH");
		
		// Build framework
		FIFOScheduler sched 			= new FIFOScheduler(100); 
		RRobinDispatcher disp 			= new RRobinDispatcher(sched, 10);
		Scaffold s 						= new Scaffold();
		s.dispatcher 					= disp;
		s.scheduler 					= sched;
		
		Architecture arch 				= new Architecture("FLAME");
		arch.scaffold					= s;
		
		// Create components: FlameServer
		AbstractImplementation flame_s;
		
		// check if the logging option is on
		String logPath;
		try {
			if((logPath = props.getProperty("FLAME_SERVER_LOG_PATH")) != null) {
				flame_s 					= new FLAMEServer(	logPath, 
																tracker_port, 
																isMRSVOn,
																isLSVOn,
																isLocalVOn,
																isHeadLocalVOn);	// logger is on
			} else {
				flame_s 					= new FLAMEServer(	tracker_port, 
																isMRSVOn,
																isLSVOn,
																isLocalVOn,
																isHeadLocalVOn);	// logger is off
			}
		} catch (Exception e) {
			System.out.println("Error: " + e);
			return;
		}
		
		Component flame 				= new Component("FLAME Server", flame_s);
		flame.scaffold 					= s;
		
		// Create connectors: between FLAMEServer & FLAMEClient
		Connector flameConn 			= new Connector("FLAME Distributed Connector");
		flameConn.scaffold 				= s;
				
		// Add components
		arch.add(flame);
		arch.add(flameConn);
		
		/* 
		 * Connect components and establish ports: FLAME Server
		 */
		
		// With the connector between FLAME Server & FLAME Client
		Port cwReplyPort 				= new Port("cwReplyPort", PrismConstants.REPLY);
		flame.addCompPort(cwReplyPort);
		
		Port connRequestPort 			= new Port("connRequestPort", PrismConstants.REQUEST);
		flameConn.addConnPort(connRequestPort);
		
		arch.weld(cwReplyPort, connRequestPort);
		
		/*
		 *  Extensible Port Setup for FLAME Clients
		 */ 
		
		ExtensiblePort epServer 		= new ExtensiblePort("epServer", PrismConstants.REPLY);
		SocketDistribution sd 			= new SocketDistribution(epServer, Integer.parseInt(props.getProperty("PORT")));
		
		epServer.addDistributionModule(sd);
		epServer.scaffold 				= s;
		
		// Add to appropriate places
		flameConn.addConnPort(epServer);
		arch.add(epServer);
		
		((FLAMEServer)flame.getImplementation()).setSocketDistribution(sd);
	
		// Start dispatcher and architecture
		disp.start();
		arch.start();
	}
	
	/**
	 * Checks whether an Detection Engine switch is on/off
	 * 
	 * @param props			Java Properties instance
	 * @param switchName	Name of the property that has either on or off
	 * @return				True (on) or false (off)
	 */
	public static boolean switchCheck(Properties props, String switchName) {
		// gets the switch
		String DetectionEngineSwitch = props.getProperty(switchName);
		boolean	isTheEngineOn = false;
		if(DetectionEngineSwitch != null &&	DetectionEngineSwitch.toLowerCase().equals("on")) {
			isTheEngineOn = true;
		}
		
		return isTheEngineOn;
	}
}
