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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import flame.Constants;
import flame.Utility;

public class FLAMESlaveNumCalc {
	
	protected Path							logEventPath;			// path of the event log file
	protected Path							resultDirPath;			// path under where the result directory will be saved
	protected Path							outAllSlaveNum;			// path of all engine number file
	
	protected long							targetTimeMilliseconds;	// target time to monitor
	protected int							maxNumberOfSlave = 0;	// maximum number of Detection Engine necessary
	
	/**
	 * The Constructor
	 * 
	 * @param dataDirPath
	 * @param resultDirPath
	 * @param resultPath
	 * @param logEventPath
	 * @param outAllEngineNum
	 */
	public FLAMESlaveNumCalc (		int		targetTime,
									Path 	logEventPath,		
									Path 	resultDirPath,
									Path 	outAllSlaveNum ) {
		this.targetTimeMilliseconds	= targetTime * 1000;
		this.logEventPath			= logEventPath;
		this.resultDirPath 			= resultDirPath;
		this.outAllSlaveNum			= outAllSlaveNum;
	}
	
	public void analyze() {
		
		List<String> originalEvents;
		List<String> events = new ArrayList<>();
		
		// opens the analysis file and reads in the simulation notifications
		try {
			originalEvents = Files.readAllLines(logEventPath, Constants.charset);
		} catch (IOException e) {
			printMsg("Reading " + logEventPath.getFileName().toString() + " failed: " + e.toString());
			return;
		}
		
		// filters the Design events only
		for (String event : originalEvents) {
			// splits the CSV line
			String[] tokens = event.split(",");
						
			// checks for Notification type
			if(tokens.length > 1 && tokens[1].trim().equals("Design")) {
				events.add(event);
			}
		}
		
		// iterates through the list of simulation notifications
		for(int i=0; i < events.size() - 1; i++) {
			
			int 	nextIndex 	= i+1;
			String	event 		= events.get(i);
			long	eventTime;
			
			try {
				eventTime = getTime(event);
			} catch (Exception e) {
				printMsg("Skipping an Event: " + e.toString());
				continue;
			}
			
			int numberEventsInTargetTime = 1;
			
			// goes through the following events until hitting the targetTime
			while (true) {
				String 	nextEvent = events.get(nextIndex++);
				long	nextEventTime;
				
				try {
					nextEventTime = getTime(nextEvent);
				} catch (Exception e) {
					printMsg("Skipping a nextEvent: " + e.toString());
					continue;
				}
				
				// compares the times
				if(nextEventTime - eventTime <= targetTimeMilliseconds) {
					numberEventsInTargetTime++;
				} else {
					break;
				}
				
				// breaks if last Event 
				if(nextIndex >= events.size()) {
					break;
				}
			}
			
			// updates the max slave number
			if (numberEventsInTargetTime > maxNumberOfSlave) {
				maxNumberOfSlave = numberEventsInTargetTime;
			}
		}
		
		// checks if the output file exists
		boolean firstTime = false;
		if(Files.notExists(resultDirPath.resolve(outAllSlaveNum), LinkOption.NOFOLLOW_LINKS)) {
			firstTime = true;			
		}
		
		// writes the result
		try (OutputStream out = Files.newOutputStream(	resultDirPath.resolve(outAllSlaveNum),
														StandardOpenOption.CREATE,
														StandardOpenOption.APPEND)) {
			// prints header
			if(firstTime) {
				writeEntry(out, "session, max_slave_number,");
			}
			
			// prints the max number
			writeEntry(out, logEventPath.getParent().toString() + "/" + logEventPath.getFileName().toString() + ", " + maxNumberOfSlave + ",");
		} catch (IOException e) {
			printMsg("Writing the analysis result to " + outAllSlaveNum.getFileName().toString() + " failed: " + e.toString());
		}
	}
	
	/**
	 * Parses the timestamp of an Event
	 * @param event
	 * @return
	 * @throws ParseException
	 */
	protected long getTime (String event) throws Exception {
		// splits the CSV line
		String[] tokens = event.split(",");
		
		long eventTime;
		
		try {
			eventTime = Utility.revertDate(tokens[0]);
		} catch (ParseException pe) {
			throw new Exception ("Event does not have a timestamp: " + event);
		}
		
		return eventTime;
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
		Path 	dataDirPath;					// path under where the data directories are
		Path	resultDirPath;					// path under where the result directory will be saved
		Path	resultPath;						// path of the result director
		Path	logEventPath;					// path of the event log file
		Path	outAllSlaveNum;					// path of all engine number file
		int		targetTime;						// target duration of time 
		
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
			dataDirPath 				= Utility.getPathProperty(props, 	"DATA_DIRECTORY");
			resultDirPath				= Utility.getPathProperty(props, 	"RESULT_DIRECTORY");
			logEventPath				= Utility.getPathProperty(props, 	"FILENAME_LOG_EVENT");
			outAllSlaveNum				= Utility.getPathProperty(props, 	"FILENAME_OUT_ALL_SLAVE_NUM");
			targetTime					= Utility.getIntProperty(props, 	"LONGEST_CONFLICT_DETECTION_TIME");
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
			
			FLAMESlaveNumCalc svc = new FLAMESlaveNumCalc(targetTime, dir.resolve(logEventPath), resultPath, outAllSlaveNum);
			svc.analyze();
		}
		
		System.out.println("FLAME Slave Number Calculator finished.");
	}

}
