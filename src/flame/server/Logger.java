package flame.server;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import flame.Constants;
import flame.ScreenLogger;
import flame.Utility;
import Prism.core.Event;


/**
 * Logger leaves the details of incoming Events into CSV files
 * 
 * The basic fields are in the order listed below:<br>
 * 1. Absolute time of the log<br>
 * 2. Event name { Login, Init, Design, XTEAM, Snapshot }<br>
 * 3. OriginComponent { FLAME Server, FLAME Client, conflict detection engine }<br>
 * 4. SenderUsername<br>
 * 5. EventID
 * 
 * @author 					<a href=mailto:jaeyounb@usc.edu>Jae young Bang</a>
 * @version					2013.05
 */
public class Logger {
	
///////////////////////////////////////////////
//Member Variables
///////////////////////////////////////////////
		
	/**
	 * Version of this run (the time FLAME Server was turned on)
	 */
	private String 				versionName;
	
	/**
	 * Log destination directory path
	 */
	private Path				versionPath;				
	
	/**
	 * Name of file that has all Events log
	 */
	private static final String	logFileName_event 		= "log_events.csv";
	
	/**
	 * Path of file that has all Events log
	 */
	private Path				logFilePath_event;
	
	/**
	 * Name of file that has XTEAM analysis Events log
	 */
	private static final String	logFileName_analysis 	= "log_analyses.csv";
	
	/**
	 * Path of file that has XTEAM analysis Events log
	 */
	private Path				logFilePath_analysis;
	
	/**
	 * Name of file that has Snapshot Events log
	 */
	private static final String	logFileName_snapshot 	= "log_snapshots.csv";
	
	/**
	 * Path of file that has Snapshot Events log 
	 */
	private Path				logFilePath_snapshot;
	
	/**
	 * Basic parameters array
	 */
	private final String[] 		basicParas 		= {	"OriginComponent",
													"SenderUsername",
													"EventID"
													};
	
	/**
	 * Parameters that only Design Events have 
	 */
	private final String[]		designParas		= { "Value" };
	
	/**
	 * Parameters that only XTEAM Events have
	 */
	private final String[]		XTEAMParas		= { "AnalysisType",
													"SyntacticConflicts",
													"AnalysisWarnings",
													"OverallTotal",
													"OverallMax",
													"OverallAverage",
													"OverallSuccess",
													"PerComponentTotal",
													"PerComponentMax",
													"PerComponentAverage",
													"PerComponentSuccess",
													"ArrivalTime"
													};
	
	/**
	 * Delimiter used to separate parameter entries in the log
	 */
	private final String		delimiter		= ",";

	/**
	 * ScreenLogger instance
	 */
	private ScreenLogger		sl;
	
	
	
///////////////////////////////////////////////
//Constructors
///////////////////////////////////////////////
	
	/**
	 * Default constructor
	 * 
	 * @param logPath		Path string to the log destination directory
	 * @throws Exception	Cannot create the log directory or the files
	 */
	public Logger (String logPath, ScreenLogger screenLogger) throws Exception {
		
		// get the screen logger instance
		sl = screenLogger;

		// calculate version
		versionName = Utility.convertDate(System.currentTimeMillis());
		
		// create the log directory
		try { 
			Files.createDirectories(versionPath = Paths.get(logPath));
		} catch (Exception e) {
			throw new Exception ("The log directory cannot be created");
		}
		
		// create the particular version's log directory
		// add a 000~ioIterations number at the end of the version in case there are
		// multiple versions within the second
		boolean mkdirSucceeded = false;
		for(int i=0; i < Constants.ioIterations; i++) {
			String 	new_versionName = versionName + "_" + String.format("%03d", i);
			Path 	new_versionPath = versionPath.resolve(new_versionName);
			
			// in case the new version does not already exist
			if(Files.notExists(new_versionPath, LinkOption.NOFOLLOW_LINKS)) {
			
				try {
					Files.createDirectory(new_versionPath);
					
					versionName = new_versionName;
					versionPath = new_versionPath;
				} catch (FileAlreadyExistsException faee) {
					throw new FileAlreadyExistsException ("Log directory for version [" + versionName + "] already exists");
				} catch (Exception e) {
					throw new Exception ("Log directory for version [" + versionName + "] cannot be created");
				}
			
				mkdirSucceeded = true;
				break;
			}
		}
		
		if(mkdirSucceeded == false) {
			throw new Exception ("Log directory for version [" + versionName + "] cannot be created");
		}
			
		// define file paths
		logFilePath_event 		= versionPath.resolve(logFileName_event);
		logFilePath_analysis	= versionPath.resolve(logFileName_analysis);
		logFilePath_snapshot	= versionPath.resolve(logFileName_snapshot);
			
		// (1) create log file of Design Events
		try {
			Files.createFile(logFilePath_event);
		} catch (FileAlreadyExistsException faee) {
			throw new Exception ("Event log file already exists: " + logFilePath_event.toString());
		} catch (Exception e) {
			throw new Exception ("Event log file creation failed: " + e);
		}
		
		String eventFields = new String();
		eventFields += createBasicFieldNames();
		eventFields += createFieldNames (designParas);
		writeLog(logFilePath_event, eventFields);
		
		// (2) create log file of XTEAM analysis Events
		try {
			Files.createFile(logFilePath_analysis);
		} catch (FileAlreadyExistsException faee) {
			throw new Exception ("Analysis log file already exists: " + logFilePath_analysis.toString());
		} catch (Exception e) {
			throw new Exception ("Analysis log file creation failed: " + e);
		}
		
		String XTEAMFields = new String();
		XTEAMFields += createBasicFieldNames();
		XTEAMFields += createFieldNames (XTEAMParas);
		writeLog(logFilePath_analysis, XTEAMFields);
		
		// (3) create log file of Snapshot Events
		try {
			Files.createFile(logFilePath_snapshot);
		} catch (FileAlreadyExistsException faee) {
			throw new Exception ("Snapshot log file already exists: " + logFilePath_snapshot.toString());
		} catch (Exception e) {
			throw new Exception ("Snapshot log file creation failed: " + e);
		}
		
		String snapshotFields = new String();
		snapshotFields += createBasicFieldNames();
		writeLog(logFilePath_snapshot, snapshotFields);
	}
	
	
	
	
///////////////////////////////////////////////
//Member Methods
///////////////////////////////////////////////
	
	/**
	 * Gets the path to the log destination directory
	 * @return				Path to the log destination directory
	 */
	public Path getLogVersionPath() {
		return versionPath;
	}
		
	/**
	 * Creates a log entry
	 * 
	 * @param e				Incoming Event
	 * @throws Exception	Cannot write on the log file
	 */
	public void createLog (Event e) throws Exception {
		
		// get the Event Name
		String eventName = e.name;
				
		// the log message for this event
		String log = new String();
		
		log += createBasicLogString(e);
		
		switch(eventName) {
		
			case "Snapshot":
				writeLog(logFilePath_event, log);
				writeLog(logFilePath_snapshot, log);
				break;
				
			case "CheckLocal":
				writeLog(logFilePath_event, log);
				writeLog(logFilePath_snapshot, log);
				break;
				
			case "Notification":
				log += createLogString(e, designParas);
				writeLog(logFilePath_event, log);
				break;
				
			case "XTEAM":
				writeLog(logFilePath_event, log);
				
				log += createLogString(e, XTEAMParas);
				writeLog(logFilePath_analysis, log);
				break;
				
			case "Design":
				log += createLogString(e, designParas);
				writeLog(logFilePath_event, log);
				break;
				
			case "Login":
				writeLog(logFilePath_event, log);
				break;
				
			case "Update":
				writeLog(logFilePath_event, log);
				break;
		}

		
	}
	

	/**
	 * Manipulates the basic field names string
	 * 
	 * @return				String that contains the basic field names
	 */
	private String createBasicFieldNames () {
		
		// the field names
		String fields = new String();
		
		fields += addDelimiter("TIME");
		fields += addDelimiter("Event name");
		
		fields += createFieldNames (basicParas);
		
		return fields;	
	}
	
	/**
	 * Manipulates basic log string for an Event
	 * @param e				Incoming Event
	 * @return				String that contains the basic log string
	 */
	private String createBasicLogString (Event e) {
		
		// the log message for this event
		String log = new String();
		
		// log the time
		log += addDelimiter(Utility.convertDate(System.currentTimeMillis()));
		
		// log the event name
		log += addDelimiter((String) e.name);
		
		// log the basic parameters
		log += createLogString(e, basicParas);
		
		return log;
	}
	
	/**
	 * Manipulates the field names
	 * @param paras			Field names array
	 * @return				Field names string
	 */
	private String createFieldNames (String [] paras) {
		
		// the log message for this event
		String fields = new String();		
				
		for(String str : paras) {
			fields += addDelimiter(str);
		}

		return fields;
	}
	
	/**
	 * Manipulates XTEAM parameter string for an Event
	 * @param e				Incoming Event
	 * @param paras			Parameters to leave a log with
	 * @return				Log string
	 */
	private String createLogString (Event e, String [] paras) {
		
		// the log message for this event
		String log = new String();		
		
		// log the parameters
		for(String str : paras) {
			log += addParameter(e, str);
		}
		
		return log;
	}
	
	/**
	 * Gets an empty parameter entry with a delimiter
	 * @return				Empty parameter entry with a delimiter
	 */
	private String addDelimiter () { 
		return delimiter + " "; 
	}
	
	/**
	 * Gets a string with the delimiter attached at the end
	 * 
	 * @param str			Original string
	 * @return				String with the delimiter attached
	 */
	private String addDelimiter (String str) {
		return str + addDelimiter();
	}
	
	/** 
	 * Manipulates a parameter string
	 * 
	 * @param e				Incoming Event
	 * @param parameter		Parameter to create a string with		
	 * @return				String with XTEAM analysis information for a parameter
	 */
	@SuppressWarnings("unchecked")
	private String addParameter (Event e, String parameter) {
		if(e.hasParameter(parameter)) {
			
			String logString = new String();
			
			// some parameters are not String, handle them separately
			switch (parameter) {
					
				case "EventID":
					logString += ((Integer) e.getParameter(parameter)).toString();
					break;
					
				case "SyntacticConflicts":
				case "PerComponentAnalysis":
				case "PerComponentTotal":
				case "PerComponentMax":
				case "PerComponentAverage":
				case "PerComponentSuccess":
					for(String str : (ArrayList<String>) e.getParameter(parameter) ) {
						logString += str + "\\n";
					}
					break;
				
				case "AnalysisWarnings":
					Map<String, List<String>> analysis_warnings = (Map<String, List<String>>) e.getParameter("AnalysisWarnings");
					for(String key : analysis_warnings.keySet()) {
						for(String str : analysis_warnings.get(key)) {
							logString += key + ": " + str + "\\n";
						}
					}
					break;
					
				default:
					logString += (String) e.getParameter(parameter);
					break;
			}
			
			// Chomps the string. the "\\n" added to sync conflicts would survive because they are \\n not \n
			logString = logString.replaceAll("(\\r|\\n)", "");
			logString = logString.replaceAll(",", ".");
			
			return addDelimiter (logString);
		} else {
			return addDelimiter ();
		}
	}
	
	/**
	 * Writes a log entry to a log file
	 * @param targetFile	Path to target log file
	 * @param log			Log string to leave (a line)
	 * @throws Exception	Cannot write on the log file
	 */
	private synchronized void writeLog (Path targetFile, String log) throws Exception {		
		Files.write(targetFile, (log + "\n").getBytes(), StandardOpenOption.APPEND);
	}
	
	/**
	 * Prints a message to screen with the component name
	 * 
	 * @param msg			Message to print to screen
	 */
	protected void printMsg(String msg) {
		sl.printMsg("Logger", msg);
	}
}
