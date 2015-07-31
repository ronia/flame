package flame.detectors.xteam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.eclipse.swt.widgets.Display;

import flame.*;
import flame.client.*;
import flame.detectors.slave.SlaveManager;
import flame.detectors.xteam.distributed.XTEAMDistributedSimulation;
import Prism.core.AbstractImplementation;
import Prism.core.Architecture;
import Prism.core.Component;
import Prism.core.Connector;
import Prism.core.Event;
import Prism.core.FIFOScheduler;
import Prism.core.Port;
import Prism.core.PrismConstants;
import Prism.core.RRobinDispatcher;
import Prism.core.Scaffold;
import Prism.extensions.port.ExtensiblePort;
import Prism.extensions.port.distribution.SocketDistribution;

/**
 * XTEAMEngine can run in either centralized or distributed. When it runs centralized,
 * it creates instances of XTEAMSimulation to run on the same machine on which it runs.
 * When it runs distributed, it initiates the Slave Manager, and exploits the computation
 * power of the slaves for simulation execution.<p>  
 * 
 * XTEAMEngine can run in four different modes: <br>
 * 		(1) MRSV:		Analyzes the Global version<br>
 * 		(2) LSV:		Analyzes the Head version<br>
 * 		(3) LocalV:		Analyzes the Local version<br>
 * 		(4) HeadLocalV:	Analyzes the Head & Local Version<p>
 * 
 * @author 					<a href="mailto:jaeyounb@usc.edu">Jae young Bang</a>
 * @version					2015.01
 */
public class XTEAMEngine extends FLAMEClient {
	
///////////////////////////////////////////////
// Member Variables
///////////////////////////////////////////////

	/** 
	 * XTEAM Engine Mode could be one of three:<br>
	 * 	(1) MRSV:		Most Recent Syntactically-correct Version<br>
	 * 	(2) LSV:		Last Snapshots Version<br>
	 * 	(3) LocalV:		Local Version<br>
	 * 	(4) HeadLocalV:	Analyze the Head & Local Version<br>
	 */
	private 	String 			mode;
	
	/**
	 *  Path to where the XTEAM simulation scaffold code is
	 */
	private 	Path 			scaffoldPath;		
	
	/**
	 * Target time (logical time) to which the simulation analysis runs  
	 */
	private		double			targetTime;
	
	/**
	 * Visual Studio 2008 compiler path
	 */
	private		Path			vsCompilerPath;
	
	/**
	 * Maximum number of threads running XTEAM simulations concurrently
	 */
	protected	int				numberOfThreads;
	
	/**
	 * Path to the file that contains what types of analyses this Engine performs
	 */
	protected	Path			xteamInfo;
	
	/**
	 * List of Snapshot Events for LocalV engine. LocalV engine stores Snapshot Events from architects other 
	 * than the corresponding architect, and pushes them to the buffer when a snapshot from the corresponding
	 * architect arrives.
	 */
	protected	List<Event>		snapshotsForLocalV;
	
	/**
	 * Switch whether the distributed conflict detection will be used with slave machines
	 */
	protected	boolean			distributed;
	
	/**
	 * Slave Manager that manages XTEAMSlave instances
	 */
	protected 	SlaveManager 	slaveManager;

///////////////////////////////////////////////
// Semaphores
///////////////////////////////////////////////	
	
	/**
	 * Semaphore to control the maximum number of concurrent XTEAM simulation threads.
	 */
	protected Semaphore 		mLockXTEAMSimulations;
	
	/**
	 * Locks the outgoing Events semaphore
	 */
	public void getLock_XTEAM() {
		try {
			mLockXTEAMSimulations.acquire();	// get the semaphore
		} catch (InterruptedException ie) {
			printMsg(name, "Thread interrupted while waiting for the semaphore");
		}
	}
	
	/**
	 * Releases the outgoing Event semaphore
	 */
	public void releaseLock_XTEAM() {
		mLockXTEAMSimulations.release();
	}
	
	
///////////////////////////////////////////////
// Constructors
///////////////////////////////////////////////
	
	/**
	 * Default constructor
	 *  
	 * @param username				Username (e.g., LocalV would target a specific username)
	 * @param xteamEngineMode		XTEAMEngine mode, which could be one of MRSV, LSV, LocalV, HeadLocalV
	 * @param xteamScaffoldPath		Path to the directory that contains the XTEAM simulation scaffold code
	 * @param xteamTargetTime		Target time (logical time) to which the simulation analysis runs  
	 * @param xteamCompilerPath		Path to the Visual Studio 2008 compiler
	 * @param xteamMaxThreads		Number of simultaneous threads used for conflict detection
	 * @param xteamInfo				XTEAM_Info.dat file path; this is where system requirements are
	 * @param modelPath				Path to the model file used in this session
	 * @param modelsDirPath			Path to the directory where model files are
	 * @param GMEPath				Path to GME executable
	 * @param distributed			Switch of the distributed conflict detection  
	 */
	public XTEAMEngine(	String 	username, 
						String	xteamEngineMode, 
						Path 	xteamScaffoldPath,
						double	xteamTargetTime,
						Path	xteamCompilerPath,
						int		xteamMaxThreads,
						Path	xteamInfo,
						Path 	modelPath,
						Path 	modelsDirPath,
						Path	GMEPath,
						boolean	distributed) {
		super("XTEAM Engine", modelPath, modelsDirPath, GMEPath, false);
		
		this.username			= username;
		mode 					= xteamEngineMode;
		scaffoldPath 			= xteamScaffoldPath;
		targetTime				= xteamTargetTime;
		vsCompilerPath			= xteamCompilerPath;
		numberOfThreads			= xteamMaxThreads;
		this.xteamInfo			= xteamInfo;
		this.distributed		= distributed;
		
		storage					= new EventStorage(getScreenLogger());
		snapshotsForLocalV		= new ArrayList<>();
		mLockXTEAMSimulations 	= new Semaphore (xteamMaxThreads, true);
	}
	
	
///////////////////////////////////////////////
//Member Methods
///////////////////////////////////////////////
	
	/**
	 * Send an Event to CoWareServer to add the port to PortNumberTracker
	 * 
	 * @param username		Username of this component
	 * @param port			Port that is open
	 */
	@Override
	protected void addPort(String username, int port) {
		printMsg(name, "Requesting port addition [username:" + username + "/port:" + port + "]");
		
		// create a login event
		Event e = (Event) new Prism.core.Event("Port");
		
		e.addParameter("OriginComponent", 	name);
        e.addParameter("SenderUsername", 	username);
        e.addParameter("Port", 				port);
       
        // send the login event to CoWareServer
        sendRequest(e);
	}
	
	/**
	 * Initializes this XTEAMEngine, invoked by Prism-MW
	 */
	@Override
	public void start () {
		printMsg(name, "Beginning the XTEAM Engine [" + mode + "] ...");
		
		// login as the XTEAMEngineMode
		sendLoginEvent(username, username);
		
		printMsg(name, "Connecting FLAME Adaptor ...");
		
		// launches GME automatically
		try{
			launchGME();
		} catch(Exception e){
			printMsg(name, "Error: " + e.toString());
		}
		
		// manage connection with FLAME adaptor
        connectFLAME();

        printMsg(name, "Finished the initialization");
	}
	
	///////////////////////////////////////////////////////////
	//	Event handling
	///////////////////////////////////////////////////////////

	/**
	 * Handles incoming Events
	 * 
	 * @param e					The incoming Event
	 */
	@Override
	public void handle(Event e) {		
		
		// check if this architect is supposed to receive this event
		if(is_this_for_me(e, username)) {

			// the event name of the Event
			String eventName = (String) e.name;
			
			// throw away XTEAM Events
			if(eventName.equals("XTEAM") || eventName.equals("Notification")) {
				return;
			}
			
			// print the received message to screen
			printEvent(e);

			
			switch (eventName) {
			
				//////////////////////////////////
				// handling Init events
				//////////////////////////////////
				
				case "Init":
					
					addEmptyValue(e, "XTEAM");					
					
					ArrayList<Event> events = new ArrayList<>();
					events.add(e);
					forwardEventsToAdaptor(filterEvents(events));
					
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
				// handling Design & Snapshot
				//////////////////////////////////
			
				case "Design":
				case "Snapshot":
				case "Update":
					
					// If the Event has IsLast parameter, that means
					// the Event is a part of the initial update Events
					if(e.hasParameter("IsLast")){
						if( (boolean) e.getParameter("IsLast") ) {
							// replace the XTEAM analysis attribute to 'O'
							try {
								flipXTEAMAnalysisSwitch(e, true);
							} catch (Exception exc) {
								printMsg(name, "Error: Failed to flip XTEAM analysis switch of an Event with IsLast because the Event does not have Value parameter: " + exc);
								break;
							}
						}
					}
					// If the Event does not have IsLast parameter, that means
					// the Event is NOT a part of the initial update Events
					else {
						// replace the XTEAM analysis attributed to 'O'
						try {
							flipXTEAMAnalysisSwitch(e, true);
						} catch (Exception exc) {
							printMsg(name, "Error: Failed to flip XTEAM analysis switch of an Event without IsLast because the Event does not have Value parameter: " + exc);
							break;
						}
					}
										
					/*
					 * The way LocalV is implemented is to invoke purgeBuffer() like the LSV 
					 * engine while ignoring all Snapshot Events but the ones from the 
					 * corresponding architect. By doing so, all Events from the corresponding
					 * architect would be forwarded as they arrive while the other Events are
					 * only forwarded when a Snapshot Event arrives from the corresponding 
					 * architect.
					 * 
					 * For the record, purgeBuffer() purges the buffer to LSV + all events from
					 * the corresponding SenderUsername.
					 * 
					 * While the code for LSV and HeadLocalV are identical, since LSV has the 
					 * username of "LSV" and HeadLocalV has the username of the corresponding
					 * architect's username, they would essentially behave differently.
					 */
					
					
					// Handles Events according to the mode 
					switch (mode) {
						case "MRSV":
							// adds to the buffer
							storage.addToBuffer(e);
							
							// purges the buffer to MRV
							forwardEventsToAdaptor(filterEvents(flipXTEAMAnalysisSwitches(purgeBufferAll())));
							
							
							break;
							
						case "LSV":
							// adds to the buffer
							storage.addToBuffer(e);
							
							// purges the buffer to LSV
							forwardEventsToAdaptor(filterEvents(flipXTEAMAnalysisSwitches(purgeBuffer())));
							break;
							
						case "LocalV":
							// adds to the buffer (ignore Snapshot Events from other architects)
							if(e.hasParameter("SenderUsername")) {
								String senderUsername = (String) e.getParameter("SenderUsername");
								
								// if the Event is a Snapshot Event
								if(eventName.equals("Snapshot")) {
									if(senderUsername.equals(username)) {
										
										/*
										 *  if the Snapshot Event is from the corresponding architect
										 */
										
										// pushes all stored Snapshot Events from other architects to the buffer
										for(Event snapshotEvent : snapshotsForLocalV) {
											storage.addToBuffer(snapshotEvent);
										}
										
										// adds the incoming Snapshot Event to the buffer as well
										storage.addToBuffer(e);
										
									} else {
										/*
										 * If the Snapshot Event is from the other architects
										 */
										
										// stores the Snapshot Event
										snapshotsForLocalV.add(e);
									}
								} else {
									
									// if the Event is a Design Event
									storage.addToBuffer(e);
								}
							}
							
							// purges the buffer to LSV
							forwardEventsToAdaptor(filterEvents(flipXTEAMAnalysisSwitches(purgeBuffer())));
							break;
							
						case "HeadLocalV":
							// adds to the buffer
							storage.addToBuffer(e);
							
							// purges the buffer to LSV
							forwardEventsToAdaptor(filterEvents(flipXTEAMAnalysisSwitches(purgeBuffer())));
							break;
					}
				
					break;
				
				//////////////////////////////////
				// handling Finish
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
				// Error handling
				//////////////////////////////////
					
				default:
					
					printMsg(name, "Error: Received incomprehensible Event [" + e.name + "]");
					break;	
			}
		}
	}
	
	/**
	 * Flips the XTEAM analysis switch attribute of an Event
	 * 
	 * @param 	e			Event to switch the attribute
	 * @param 	to			The target state (on if true, off if false)
	 * @throws	Exception	Given Event does not have the Value parameter
	 */
	protected void flipXTEAMAnalysisSwitch(Event e, boolean to) throws Exception {
		if(!e.hasParameter("Value")) {
			throw new Exception ("Below Event does not have the Value parameter to flip: " + Constants.endl + manipulateEventPrintString(e));
		} else {
			
			// manipulates the new Value
			char	new_flag	= to?'O':'X';
			String 	value 		= (String) e.getParameter("Value");
			String 	new_value 	= new_flag + value.substring(1);
			
			// replaces the Value parameter of the Event
			e.removeParameter("Value");
			e.addParameter("Value", new_value);
		}
	}
	
	/**
	 * Flips the XTEAM analysis switches of given list of Events. <p>
	 * 
	 * Turns "off" all of them but does not modify the last one
	 * @param 	events		List of Events
	 * @throws 	Exception	One of the Events does not have the Value parameter
	 */
	protected ArrayList<Event> flipXTEAMAnalysisSwitches(ArrayList<Event> events) {
		
		boolean found = false;	
		
		// from the back, finds a Design Event and leaves the flag as true
		// flips the rest to false
		for(int i = events.size()-1; i >= 0 ; i--) {
			if(!found) {
				if(events.get(i).name.equals("Design")) {
					found = true;
				}
			} else {
				try {
					flipXTEAMAnalysisSwitch(events.get(i), false);
				} catch (Exception e) {
					printMsg(name, e.toString());
				}
			}
		}
		
		return events;
	}
	
	/**
	 * Initiates and runs the EventReceiver thread
	 * 
	 * @param clientSocket	Socket that talks with FLAME Adaptor
	 */
	@Override
	protected void createEventReceiver(Socket clientSocket) {
		eventReceiver = new XTEAMEngine.EventReceiver(this, clientSocket);
		eventReceiver.setEventID(event_id);
		eventReceiver.start();
	}
	
	
	/**
	 * Customized version of {@link CoWareXTEAMClient.EventReceiver} for {@link XTEAMEngine}
	 * 
	 * @author 				<a href=mailto:jaeyounb@usc.edu>Jae young Bang</a>
	 * @version				2015.01
	 */
	public class EventReceiver extends FLAMEClient.EventReceiver {
		
		/**
		 * The parent class
		 */
		protected XTEAMEngine engine;

		/**
		 * Constructor
		 * 
		 * @param engine	Parent class
		 * @param s			Socket that talks with FLAME Adaptor
		 */
		public EventReceiver(XTEAMEngine engine, Socket s) {
			super(engine, s);
			
			this.engine = engine;
		}
		
		@Override
		public void run() {
			/* 
			 * if the distributed conflict detection switch is true, XTEAMEngine will
			 * off-load simulation compilation and execution duty to slaves using a
			 * Slave Manager. Initiates the Slave Manager. 
			 */
			int 	slaveManagerPort;
			boolean	reverse;
			Path 	simulationResultPath;
			Path	errorsFilename;
			if(distributed == true) {
				try {
					// reads Slave Manager properties
					slaveManagerPort 		= Utility.getIntProperty		(props, "SM_PORT");
					reverse					= Utility.getBooleanProperty	(props, "REVERSE_CONFLICT_DETECTION");
					
					// initiates Slave Manager
					slaveManager 		= new SlaveManager(slaveManagerPort, reverse, getScreenLogger());
					
					// launches Slave Manager
					slaveManager.launch();
				} catch (Exception e) {
					printMsg(name, "Error while launching Slave Manager: " + e);
					return;
				}
			}
		
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				
				char 	len_buf[] = new char[3];	// buffer storing the two bytes of length
				int		length = 0;					// the length of the following message
				char	buffer[];					// the message buffer
				String 	inputLine;					// the received message
				
				while (true) {
					// reads the first two bytes that say how long the following message is
					if (in.read(len_buf, 0, 2) != 2)
						break;
					
					// decodes the length
					length = ((int) (len_buf[0])) * 128 + ((int) len_buf[1]);
					
					// allocates the buffer
					buffer = new char[length];
					
					// reads the message
					if (in.read(buffer, 0, length) != length) {
						break;
					}
					
					// parses the message
					inputLine 		= new String(buffer);
					String[] tokens	= inputLine.split("`");
					
					// if the message is an XTEAM message with simulation code path
					if(tokens[2].equals("XTEAM")) {
						Path simulationPath = Paths.get(tokens[3]);
						printMsg(name, "Received simulation code: " + simulationPath.getFileName().toString());
						
						// off-loads simulation to a slave
						if(distributed == true) {
							try {
								// reads Slave Manager properties
								simulationResultPath	= Utility.getPathProperty		(props, "SM_RECEIVED_SIMULATION_RESULT_DIR");
								errorsFilename			= Utility.getPathProperty		(props, "SLAVE_ERRORS_FILENAME");
							
								// initiates a XTEAMDistributedSimulation
								XTEAMDistributedSimulation simulation =
											new XTEAMDistributedSimulation(
																	username, 
																	simulationPath, 
																	simulationResultPath, 
																	errorsFilename, 
																	targetTime, 
																	xteamInfo, 
																	getScreenLogger(), 
																	engine, 
																	this);
								// puts the XTEAMDistributedSimulation to the simulation request queue of Slave Manager
								slaveManager.addSimulationRequest(simulation);
								printMsg(name, "Sim request " + simulationPath.getFileName().toString() + " created.");
							} catch (Exception e) {
								printMsg(name, "Error while initiating XTEAMDistributedSimulation: " + e);
								continue;
							}
						} 
						// runs simulation locally
						else {
							try{
								XTEAMSimulation simulation = 
										new XTEAMSimulation(	mode,
																username,
																simulationPath,
																scaffoldPath,
																targetTime,
																vsCompilerPath,
																xteamInfo,
																getScreenLogger(),
																engine,
																this);
								simulation.start();
							} catch (Exception e) {
								printMsg(name, "Error: " + e);
							}
						}
					}
				}
			} catch (IOException e) {
				printMsg(name, "FLAME Adaptor has closed the socket connection.");
				System.exit(0);
			}
		}
	}
	
	
	
	
	///////////////////////////////////////////////
	// The main() method
	///////////////////////////////////////////////
	
	/**
	 * The main() method
	 */
	public static void main(String[] args) {
		
		// the application name
		Display.setAppName("XTEAM Engine");
		
		// load up the properties specified in the config.properties file
		try {
			loadProps();
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
			return;
		}
		
		if(args.length < 3) {
			System.out.println("[XTEAM Engine]: Error: main() arguments are too few.");
			System.out.println("\tUsage: XTEAMEngine Mode Username");
			return;
		}
		
		// gets the XTEAM Engine Mode and the username
		String mode 		= args[1];
		String username 	= args[2];
		
		// gets properties
		Path 	scaffoldPath;
		Path 	vsCompilerPath;
		double 	targetTime;
		int 	maxThreads;
		Path	xteamInfo;
		Path 	modelPath;
		Path 	modelsDirPath;
		Path	GMEPath;
		boolean	distributed;
		try {
			scaffoldPath	= Utility.getPathProperty		(props, "XTEAM_ENGINE_SCAFFOLD_PATH");
			vsCompilerPath	= Utility.getPathProperty		(props, "XTEAM_VS_PATH");
			targetTime 		= Utility.getDoubleProperty		(props, "XTEAM_TARGET_TIME");
			maxThreads		= Utility.getIntProperty		(props, "XTEAM_THREADS");
			xteamInfo		= Utility.getPathProperty		(props, "XTEAM_INFO");
			modelPath 		= Utility.getPathProperty		(props, "MODEL_FILE_PATH");
			modelsDirPath	= Utility.getPathProperty		(props, "MODEL_FILE_DIR_PATH");
			GMEPath			= Utility.getPathProperty		(props, "GME_EXE_PATH");
			distributed		= Utility.getBooleanProperty	(props, "DISTRIBUTED_CONFLICT_DETECTION");
		}  catch (Exception e) {
			System.out.println("[XTEAM Engine]: Error: " + e.toString());
			return;
		}
		
		// Builds Prism-MW framework
		FIFOScheduler sched 			= new FIFOScheduler(100); 
		RRobinDispatcher disp 			= new RRobinDispatcher(sched, 10);
		Scaffold s 						= new Scaffold();
		s.dispatcher 					= disp;
		s.scheduler 					= sched;
		
		Architecture arch		 		= new Architecture("FLAME");
		arch.scaffold					= s;
		
		// Create components
		AbstractImplementation gme_t 	= new XTEAMEngine(	username, 
															mode, 
															scaffoldPath, 
															targetTime, 
															vsCompilerPath, 
															maxThreads, 
															xteamInfo,
															modelPath, 
															modelsDirPath, 
															GMEPath,
															distributed);
		Component gme 					= new Component("XTEAM Engine", gme_t);
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
		//epClient.connect(props.getProperty("SERVER"), Integer.parseInt(props.getProperty("PORT")));
		epClient.connect("localhost", Integer.parseInt(props.getProperty("PORT")));
		
		// Start dispatcher and architecture
		disp.start();
		arch.start();
	}
}
