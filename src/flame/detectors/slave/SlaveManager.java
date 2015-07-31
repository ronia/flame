package flame.detectors.slave;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import flame.ArchiveUtility;
import flame.FileUtility;
import flame.ScreenLogger;
import flame.SocketTransferUtility;
import flame.detectors.slave.xteam.XTEAMSlave;
import flame.detectors.xteam.distributed.XTEAMDistributedSimulation;
/**
 * SlaveManager manages the {@link XTEAMSlave} instances, forwards simulations requests to, 
 * and receives the simulation results from them.<p>
 * 
 * SlaveManager and forward the simulation requests either in the forward order (the earlier
 * a request was made, the earlier it would be served) or in the reverse order (the earlier
 * a request was made, the later it would be served).<p>
 * 
 * @author 					<a href="mailto:jaeyounb@usc.edu">Jae young Bang</a>
 * @version					2015.01
 *
 */
public class SlaveManager {
	
///////////////////////////////////////////////
// Member Variables
///////////////////////////////////////////////
	
	/**
	 * Queue of simulation requests. XTEAMEngine adds simulation requests in this queue, while
	 * Slave Manager continuously pops the head element, runs the simulation to the point right
	 * before compilation. If a compilation is necessary, Slave Manager will put the request into
	 * the compilationRequests deque. 
	 */
	protected BlockingQueue<XTEAMDistributedSimulation>
										simulationRequests		= new ArrayBlockingQueue<>(100, true);
	
	/**
	 * Queue of compilation requests. It is a Deque for the Slave Manager to be able to "take" 
	 * from the head or the tail depending on the configuration. Slave Manager has a separate thread
	 * that continuously pops the head (or the tail, depending on the configuration) and creates a 
	 * new thread that recruits an available slave, assigns the slave to run the compilation (and 
	 * execution), and waits until the slave finishes working.
	 */
	protected BlockingDeque<XTEAMDistributedSimulation> 		
										compilationRequests 	= new LinkedBlockingDeque<>();
	
	/**
	 * Queue of available slaves; only the slaves in this list are available for new
	 * simulation requests
	 */
	protected BlockingQueue<SlaveInfo>	availableSlaves			= new ArrayBlockingQueue<>(100, true);
	
	/**
	 * Port number that will wait for slaves to connect to
	 */
	protected int						slaveManagerPort;
	
	/**
	 * Switch to run later simulation requests first
	 */
	protected boolean					reverse;
	
	/**
	 * Screen Logger instance
	 */
	protected ScreenLogger				screenLogger;
	
	/**
	 * The Slave Machine server socket
	 */
	protected ServerSocket				serverSocket;
	
	/**
	 * The amount of time in minutes between each Keepalive message
	 */
	protected final int					keepaliveTimeMin		= 2;

///////////////////////////////////////////////
// Constructors
///////////////////////////////////////////////
	
	/**
	 * Default constructor
	 * @param slaveManagerPort		Port that will wait for slaves to connect to
	 * 
	 * @param screenLogger			Screen Logger instance
	 */
	public SlaveManager (	int 			slaveManagerPort,
							boolean			reverse,
							ScreenLogger 	screenLogger) {
		this.slaveManagerPort	= slaveManagerPort;
		this.reverse			= reverse;
		this.screenLogger 		= screenLogger;
	}
	
///////////////////////////////////////////////
// Slave Accepting Thread
///////////////////////////////////////////////
	
	public class SlaveAccepter extends Thread {
		protected final String threadName = "Accpt";
		@Override
		public void run() {
			Socket clientSocket;
			
			printMsgTarget(threadName, "Accepting slave connections ...");
			while (true) {			
				String slaveName;
				
				try {
					// accepts the connection
					clientSocket = serverSocket.accept();
					clientSocket.setSoTimeout(0);
					clientSocket.setKeepAlive(true);
					
					// receives the slave name
					slaveName = SocketTransferUtility.receiveString(clientSocket);
					
					// puts the SlaveInfo instance in the available slave queue
					availableSlaves.put(new SlaveInfo(slaveName, clientSocket));
				} catch (Exception e) {
					printMsgTarget(threadName, "Error while accepting a new slave: " + e);
					
					// closes the server socket
					try {
						serverSocket.close();
					} catch (IOException ioe) {
						printMsgTarget(threadName, "Error while closing the server socket" + ioe);
					}
					
					break;
				}
				
				printMsgActivityTarget(threadName, slaveName, "Accepted a new slave.");
			}
		}
	}
	
///////////////////////////////////////////////
// Simulation Request Handling Thread
///////////////////////////////////////////////
	
	public class SimulationRequestHandler extends Thread {
		protected final String threadName = "SimHndl";
		
		@Override
		public void run() {
			while (true) {
				// pops a simulation request from the queue
				XTEAMDistributedSimulation simulation;
				try {
					simulation = simulationRequests.take();
				} catch (InterruptedException ie) {
					printMsgTarget(threadName, "Interrupted while waiting to take a simulation request");
					return;
				}
				
				String simulationName = simulation.getSimulationPath().getFileName().toString();
				printMsgActivityTarget(threadName, simulationName, "sim request taken from queue.");
				
				// checks for syntax errors
				int numErrors = simulation.checkSyntaxErrors();
				printMsgActivityTarget(threadName, simulationName, numErrors + " syntax errors found.");
				
				if(numErrors == 0) {
					try {
						// sends simulation beginning notification
						simulation.sendSimulationBeginningNotification();
						
						// puts it in the compilation request queue
						compilationRequests.put(simulation);
						printMsgActivityTarget(threadName, simulationName, "Compilation request created.");
					} catch (InterruptedException ie) {
						printMsgTarget(threadName, "Interrupted while waiting to put a compilation request");
						return;
					}
				}
			}
			
		}
	}
	
///////////////////////////////////////////////
// Compilation Request Handling Thread
///////////////////////////////////////////////
	
	public class CompilationRequestHandler extends Thread {
		protected final String threadName = "CompHndler";
		
		@Override
		public void run() {
			while (true) {
				// waits for a slave to become available
				SlaveInfo slave;
				try {
					slave = availableSlaves.take();
				} catch (InterruptedException e1) {
					printMsgTarget(threadName, "Interrupted while waiting to recruit a FLAME slave.");
					return;
				}
				String 		slaveName	= slave.getName();
				printMsgActivityTarget(threadName, slaveName, "A slave has been recruited.");

				// takes a compilation request
				XTEAMDistributedSimulation compilation;
				try {
					/* 
					 * waits for a compilation request for the keepaliveTimeMin
					 * in case of no compilation request until keepaliveTimeMin,
					 * send a KEEPALIVE message and retry
					 */
					while (true) {
						if(reverse == false) {
							// if reverse conflict detection is off, takes the head of the queue
							compilation = compilationRequests.pollFirst(keepaliveTimeMin, TimeUnit.MINUTES);
						} else {
							// if reverse conflict detection is on, takes the tail of the queue
							compilation = compilationRequests.pollLast(keepaliveTimeMin, TimeUnit.MINUTES);
						}
						
						// if there was a compilation request
						if(compilation != null) {
							break;
						} else {
							SocketTransferUtility.sendKeepalive(slave.getSocket());
							printMsgActivityTarget(threadName, slaveName, "Keepalive sent.");
						}
					}
					
				} catch (InterruptedException ie) {
					printMsgTarget(threadName, "Interrupted while waiting to take a compilation request");
					return;
				} catch (Exception e) {
					printMsgTarget(threadName, e.getMessage());
					return;
				}
				
				CompileRequest cr = new CompileRequest(slave, compilation);
				cr.start();
			}
		}
	}
	
	public class CompileRequest extends Thread {
		protected final	String 						threadName = "CompHndl";
		private 		SlaveInfo					slave;
		private			String						slaveName;
		private 		XTEAMDistributedSimulation 	compilation;
		
		public CompileRequest (SlaveInfo slave, XTEAMDistributedSimulation compilation) {
			this.slave 			= slave;
			slaveName			= slave.getName();
			this.compilation	= compilation;
		}
		
		@Override
		public void run() {
			String compilationName = compilation.getSimulationPath().getFileName().toString();
			printMsgActivityTarget(threadName, compilationName, "Compilation request taken.");
			
			// compiles and runs simulation
			try {
				handleSimulationRequest(slave, compilation);
			} catch (Exception e) {
				printMsg("Error while handling simulation request: " + e);
				return;
			}
			
			// puts the slave back into the available slaves list
			try {
				availableSlaves.put(slave);
			} catch (InterruptedException e) {
				printMsgActivityTarget(threadName, slaveName, "Interrupted while waiting to release a slave.");
				return;
			}		
			printMsgActivityTarget(threadName, slaveName, "Slave has been released.");
		}
	}
	
	
///////////////////////////////////////////////
// Keepalive Thread
///////////////////////////////////////////////
	
	public class Keepalive extends Thread {
		protected final String threadName = "Keepalive";
		
		@Override
		public void run() {
			while (true) {
				// sleeps first
				try {
					Thread.sleep(keepaliveTimeMin * 1000 * 60);
				} catch (InterruptedException e) {
					printMsgTarget(threadName, e.getMessage());
					return;
				}
				
				// queue of slaves to which a Keepalive has been sent
				List<SlaveInfo> doneSlaves = new ArrayList<>();
				
				// pops slaves until the queue is empty and sends keepalive
				while (true) {
					SlaveInfo slave = availableSlaves.poll();
					
					if (slave == null) {
						break;
					} else {
						try {
							SocketTransferUtility.sendKeepalive(slave.getSocket());
							printMsgActivityTarget(threadName, slave.getName(), "Keepalive sent.");
						} catch (Exception e) {
							printMsgTarget(threadName, e.getMessage());
							return;
						}
						doneSlaves.add(slave);
					}
				}
				
				// pushes slaves back to the available slaves queue
				for (SlaveInfo slave : doneSlaves) {
					try {
						availableSlaves.put(slave);
					} catch (InterruptedException e) {
						printMsgActivityTarget(threadName, slave.getName(), "Interrupted while waiting to release a slave.");
						return;
					}
				}
			}
		}
	}
	
///////////////////////////////////////////////
// Member Methods
///////////////////////////////////////////////
	
	/**
	 * Launches the Slave Manager
	 */
	public void launch () throws Exception {
		// opens the server socket
		try {
			serverSocket = new ServerSocket(slaveManagerPort);
		} catch(Exception e) {
			throw new Exception ("Error while opening up the server socket: " + e);
		}
		
		printMsg("Socket initialization completed at port " + serverSocket.getLocalPort() + ".");
		
		// launches the SlaveAccepter thread
		SlaveAccepter sa = new SlaveAccepter();
		sa.start();
		
		// launches the SimulationRequestHandler thread
		SimulationRequestHandler srh = new SimulationRequestHandler();
		srh.start();
		
		// launches the CompilationRequestHandler thread
		CompilationRequestHandler crh = new CompilationRequestHandler();
		crh.start();	
		
		// launches the Keepalive thread
		Keepalive keep = new Keepalive();
		keep.start();
	}
	
	/**
	 * Adds a simulation request to the queue
	 * @param simulation				Simulation to add
	 * @throws InterruptedException
	 */
	public void addSimulationRequest (XTEAMDistributedSimulation simulation) {
		try {
			simulationRequests.put(simulation);
		} catch (InterruptedException e) {
			printMsg("Interrupted while waiting to put a simulation request");
		}
	}
	
	/**
	 * Handles a simulation request
	 * 
	 * @param simulation				Simulation instance 
	 * @throws Exception
	 */
	protected void handleSimulationRequest (SlaveInfo slave, 
											XTEAMDistributedSimulation simulation) throws Exception {
		String 	simulationName		= simulation.getSimulationPath().getFileName().toString();
		Path 	sourceCodeDirectory = simulation.getSimulationCodePath();
		Path	resultDirectory		= simulation.getSimulationResultPath();
		
		List<Path> filesToSend	= new ArrayList<>();
		try {
			printMsgTarget(simulationName, "Handling sim request begins.");

			// makes the list of source code files to send
			List<Path> headerFiles 	= FileUtility.findFilesWithExtension(sourceCodeDirectory, "h");
			List<Path> sourceFiles 	= FileUtility.findFilesWithExtension(sourceCodeDirectory, "cpp");
			filesToSend.addAll(headerFiles);
			filesToSend.addAll(sourceFiles);
			
			printMsgTarget(simulationName, 	"Sim code files list created.");
			
			// makes a ZIP archive of the files to send
			Path sourceCodeArchive = sourceCodeDirectory.resolve(simulationName + ".zip");
			ArchiveUtility.archive(sourceCodeArchive, filesToSend);
			
			printMsgTarget(simulationName, "Sim code archive created.");
			
			// requests the simulation to a slave
			requestSimulation(slave, simulationName, sourceCodeArchive, resultDirectory);
			
			printMsgTarget(simulationName, "Sim has completed.");

			// analyzes the received simulation result
			simulation.analyze();
			
			printMsgTarget(simulationName, "Sim result analysis has completed.");
			
		} catch (Exception e) {
			throw new Exception ("Error while handling a simulation request: " + e);
		} finally {
			// sends simulation completion notification
			simulation.sendSimulationCompletionNotification();
		}
	}
	
	/**
	 * Makes a simulation request to a slave
	 * 
	 * @param simulationName			Name of the simulation (FLAME timestamp)
	 * @param sourceCodeArchive			Path to the simulation source code archive
	 * @param resultDirectory			Path to the simulation result directory 
	 * @throws Exception
	 */
	public void requestSimulation (	SlaveInfo	slave,
									String		simulationName,
									Path		sourceCodeArchive,
									Path		resultDirectory ) throws Exception {
		
		// sends the selected slave the source code archive
		printMsgTarget(simulationName, "Sending sim code archive begins ...");
		try {
			SocketTransferUtility.sendFile(slave.getSocket(), sourceCodeArchive);
		} catch (Exception e) {
			throw new Exception ("Error while sending simulation archive: " + e);
		}
		printMsgTarget(simulationName, "Sending sim code archive completed.");
		
		// creates the simulation result directory
		try {
			Files.createDirectories(resultDirectory);
		} catch (Exception e) {
			throw new Exception ("Directory " + resultDirectory + " cannot be created");
		}
		printMsgTarget(simulationName, "sim result dir created: " + resultDirectory.getFileName().toString());

		// receives the result
		printMsgTarget(simulationName, "Waiting for sim result archive ...");
		Path simulationResultArchive;
		try {
			simulationResultArchive = SocketTransferUtility.receiveFile(slave.getSocket(), resultDirectory);
		} catch (Exception e) {
			throw new Exception ("Error while receiving simulation result archive: " + e);
		}
		printMsgTarget(simulationName, "Receiving sim result archive completed.");

		// extracts the received simulation result
		printMsgTarget(simulationName, "Extracting sim result archive begins ...");
		ArchiveUtility.extract(simulationResultArchive, resultDirectory);
		printMsgTarget(simulationName, "Extracting sim result archive completed.");
	}

	
///////////////////////////////////////////////
// Utility Member Methods
///////////////////////////////////////////////
	
	protected final String instanceName = "SvMngr"; 
	
	/**
	 * Prints a message to screen
	 * @param msg			Message to print
	 */
	protected void printMsg(String msg) {
		screenLogger.printMsg(instanceName, msg);
	}
	
	/**
	 * Prints a message for a target to screen
	 * @param targetName		Name of the target
	 * @param msg				Message to print
	 */
	protected void printMsgTarget(String targetName, String msg) {
		screenLogger.printMsg(instanceName + "/" + targetName, msg);
	}
	
	/**
	 * Prints a message for an activity to a target to screen
	 * @param activityName		Name of the activity
	 * @param targetName		Name of the target
	 * @param msg				Message to print
	 */
	protected void printMsgActivityTarget(String activityName, String targetName, String msg) {
		screenLogger.printMsg(instanceName + "/" + activityName + "/" + targetName, msg);
	}
	
	/**
	 * Prints an empty line
	 */
	protected void printEmptyLine() {
		screenLogger.printEmptyLine();
	}
	
///////////////////////////////////////////////
// The main () method
///////////////////////////////////////////////

	/**
	 * The main() for testing SlaveManager stand-alone
	 * @param args
	 */
	public static void main(String[] args) {
		ScreenLogger screenLogger 	= new ScreenLogger();
		SlaveManager sm 			= new SlaveManager(52531, false, screenLogger);
		try {
			sm.launch();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
