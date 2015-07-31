package flame.analyzer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import flame.Constants;
import flame.Utility;

/**
 * SimulationNumberCalculator reads in the FLAMEServer log files, and computes
 * the maximum number of FLAME Engines necessary to avoid any stack-up of 
 * simulation requests based on the logs.
 * 
 * @author 					<a href="mailto:jaeyounb@usc.edu">Jae young Bang</a>
 * @version					2014.12
 */
public class FLAMEEngineNumCalc {
	
	protected Path							logEventPath;			// path of the event log file
	protected Path							resultDirPath;			// path under where the result directory will be saved
	protected Path							outAllEngineNum;		// path of all engine number file
	
	protected int							maxNumberOfEngine = 0;	// maximum number of Detection Engine necessary
	
	/**
	 * List of in-progress simulation IDs
	 */	
	protected	Map<String, Set<Integer>> 	mapSimIDs				= new TreeMap<>();
	
	/**
	 * The Constructor
	 * 
	 * @param dataDirPath
	 * @param resultDirPath
	 * @param resultPath
	 * @param logEventPath
	 * @param outAllEngineNum
	 */
	public FLAMEEngineNumCalc (		Path logEventPath,		
									Path resultDirPath,
									Path outAllEngineNum ) {
		this.logEventPath		= logEventPath;
		this.resultDirPath 		= resultDirPath;
		this.outAllEngineNum	= outAllEngineNum;
	}
	
	
	public void analyze() {
		List<String> events;
		
		// opens the analysis file and reads in the simulation notifications
		try {
			events = Files.readAllLines(logEventPath, Constants.charset);
		} catch (IOException e) {
			printMsg("Reading " + logEventPath.getFileName().toString() + " failed: " + e.toString());
			return;
		}
		
		// iterates through the list of simulation notifications
		for(String event : events) {
			// splits the CSV line
			String[] tokens = event.split(",");
			
			// checks for Notification type
			if(tokens.length > 1 && tokens[1].trim().equals("Notification")) {
				if(tokens.length > 5) {
					String 	username 	= tokens[3].trim();
					Integer	eventID		= Integer.parseInt(tokens[4].trim());
					String 	value		= tokens[5].trim();
					
					
					// checks the Username in the map
					if(!mapSimIDs.keySet().contains(username)) {
						// in case it is the first time appearance of the Username, creates an entry
						mapSimIDs.put(username, new HashSet<Integer>());
					}
					
					// increase/decrease thread number
					switch (value) {
						// a simulation has begun
						case "Simulation execution beginning":
							mapSimIDs.get(username).add(eventID);
						break;
						
						// a simulation has completed
						case "Simulation execution completion":
							mapSimIDs.get(username).remove(eventID);
							break;
							
						default:
							printMsg("A Notification Event carries unparsable Value: " + event);
							break;
					}
					
					// checks for the total number of on-going simulations
					int totalSims = 0;
					for(String key : mapSimIDs.keySet()) {
						totalSims += mapSimIDs.get(key).size();
					}
					
					// updates the max. number
					if(totalSims > maxNumberOfEngine) {
						printMsg("Max. simultaneous simulations to " + totalSims);
						
						for(String key : mapSimIDs.keySet()) {
							printMsg("\t" + key + ": " + mapSimIDs.get(key).size());
						}
						
						maxNumberOfEngine = totalSims;
					}
				} else {
					printMsg("A Notification event is too short: " + event);
					continue;
				}
			}
		}
		
		// checks if the output file exists
		boolean firstTime = false;
		if(Files.notExists(resultDirPath.resolve(outAllEngineNum), LinkOption.NOFOLLOW_LINKS)) {
			firstTime = true;			
		}
		
		// writes the result
		try (OutputStream out = Files.newOutputStream(	resultDirPath.resolve(outAllEngineNum),
														StandardOpenOption.CREATE,
														StandardOpenOption.APPEND)) {
			// prints header
			if(firstTime) {
				writeEntry(out, "session, max_engine_number,");
			}
			
			// prints the max number
			writeEntry(out, logEventPath.getParent().toString() + "/" + logEventPath.getFileName().toString() + ", " + maxNumberOfEngine + ",");
		} catch (IOException e) {
			printMsg("Writing the analysis result to " + outAllEngineNum.getFileName().toString() + " failed: " + e.toString());
		}
	}
	
	protected void writeEntry (OutputStream out, String entry) throws IOException {
		out.write(entry.getBytes());
		out.write(Constants.endl.getBytes());
	}

	/**
	* Configuration properties read from the config.properties file
	*/
	public static Properties props = new Properties();
	
	/**
	 * Prints out a message
	 * 
	 * @param msg			Message to print out
	 */
	public static void printMsg (String msg) {
		System.out.println("[EngineNumCalc]: " + msg);
	}
	
	/**
	 * The main() method
	 */
	public static void main(String[] args) {
		Path 	dataDirPath;											// path under where the data directories are
		Path	resultDirPath;											// path under where the result directory will be saved
		Path	resultPath;												// path of the result director
		Path	logEventPath;											// path of the event log file
		Path	outAllEngineNum;										// path of all engine number file
		
		String 	resultName 			= Utility.convertDate(System.currentTimeMillis());	// result name of this run
		
		
		// load up the properties specified in the config.properties file
		try {
			props.load(new FileInputStream(new File("config.properties")));
		} catch (FileNotFoundException e) {
			printMsg ("config.properties is missing");
			return;
		} catch (IOException e) {
			printMsg ("config.properties cannot be read");
			return;
		}	

		// gets the directory path under where the data directories are
		try {
			dataDirPath 				= Utility.getPathProperty(props, "DATA_DIRECTORY");
			resultDirPath				= Utility.getPathProperty(props, "RESULT_DIRECTORY");
			logEventPath				= Utility.getPathProperty(props, "FILENAME_LOG_EVENT");
			outAllEngineNum				= Utility.getPathProperty(props, "FILENAME_OUT_ALL_ENGINE_NUM");
		} catch (Exception e) {
			printMsg("Reading configuration failed: " + e.toString());
			return;
		}
		
		// create the results directory
		try { 
			Files.createDirectories(resultDirPath);
		} catch (Exception e) {
			printMsg("Directory " + resultDirPath + " cannot be created");
			return;
		}
		
		// create the particular version's log directory
		// add a 000~ioIterations number at the end of the version in case there are
		// multiple versions within the second
		resultPath = resultDirPath;
		boolean mkdirSucceeded = false;
		
		for(int i=0; i < Constants.ioIterations; i++) {
			String 	new_resultName = resultName + "_" + String.format("%03d", i);
			Path 	new_resultPath = resultDirPath.resolve(new_resultName);
			
			// in case the new result directory does not already exist
			if(Files.notExists(new_resultPath, LinkOption.NOFOLLOW_LINKS)) {
			
				try {
					Files.createDirectory(new_resultPath);
					
					resultName = new_resultName;
					resultPath = new_resultPath;
				} catch (FileAlreadyExistsException faee) {
					printMsg("Result directory [" + new_resultName + "] already exists");
					return;
				} catch (Exception e) {
					printMsg("Result directory [" + new_resultName + "] cannot be created");
					return;
				}
			
				mkdirSucceeded = true;
				break;
			}
		}
		
		if(mkdirSucceeded == false) {
			printMsg("Result directory [" + resultName + "] cannot be created");
			return;
		}
		
		// searches for the data directories
		List<Path> dataDirectories = new ArrayList<>();
		
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(dataDirPath)) {
			for (Path dir : ds) {
				if(Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) {
					dataDirectories.add(dir);
				}
			}
		} catch (IOException ioe) {
			printMsg("Failed to read data directories: " + ioe.getMessage());
			return;
		}
		
		
		// iterates through the data directories
		for(Path dir : dataDirectories) {
			printMsg("Analyzing " + dir.getFileName().toString() + " ...");
			
			// locates the event log file
			if(Files.notExists(dir.resolve(logEventPath))) {
				printMsg(dir.getFileName().toString() + " does not have " + logEventPath.getFileName().toString() + ". Ignoring ...");
				continue;
			}
			
			FLAMEEngineNumCalc enc = new FLAMEEngineNumCalc(dir.resolve(logEventPath), resultPath, outAllEngineNum);
			enc.analyze();
		}
		
		System.out.println("FLAME Engine Number Calculator finished.");
	}

}
