package flame.analyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import flame.Constants;
import flame.Requirements;
import flame.ScreenLogger;
import flame.Utility;

/**
 * XTEAMLogAnalyzer reads in all FLAMEServer log files, analyzes, and outputs 
 * the summarized result 
 * 
 * @author 					<a href="mailto:jaeyounb@usc.edu">Jae young Bang</a>
 * @version					2014.04
 */
public class XTEAMLogAnalyzer {
	
//////////////////////////////////////////////////////////////////////////
//
//Member variables
//
//////////////////////////////////////////////////////////////////////////

	/**
	 * All analysis and snapshot from the log files
	 */
	private 		List<String[]> 			entries_analysis			= new ArrayList<>();	
	
	/**
	 * All analysis and snapshot with the open conflict counts
	 */
	private			List<String[]>			entries_analysis_con_cnt	= new ArrayList<>();
	
	/**
	 * All event and snapshot from the log files 
	 */
	private			List<String[]>			entries_event				= new ArrayList<>();
	
	/**
	 * All login events list
	 */
	private			List<String[]>			entries_login				= new ArrayList<>();
	
	/**
	 * All value changes
	 */
	private			List<String[]>			entries_value_changes		= new ArrayList<>();			
	
	/**
	 * All option changes
	 */
	private			List<String[]>			entries_option_changes		= new ArrayList<>();
	
	/**
	 * Index of arrival_time in the .csv source file
	 */
	private final	int							maxNumberOfColumns			= 17;
	
	private final	int							indexTimestamp				= 0;
	private final	int							indexEventName				= 1;
	private final	int							indexUsername				= 3;
	private final	int							indexEventID				= 4;
	private final	int							indexAnalysisType			= 5;
	private final	int							indexSyntacticInconsistency	= 6;
	private final	int							indexWarning				= 7;
	private final	int							indexOverallTotal			= 8;
	private final	int							indexOverallMax				= 9;
	private final	int							indexOverallAverage			= 10;
	private final	int							indexArrivalTime			= 16;
	
	private final	int							indexValue					= 5;
	private final 	int							indexHumanReadableValue		= 6;
	private final	int							indexOptionChange			= 7;
	
	/**
	 * The delimiter used in the value attribute
	 */
	private final	String						delimiterValue				= "`";
	
	
	
	/**
	 * List of target analyses<p>
	 * 
	 * E.g. Energy/Total/less than/700000
	 */
	protected		Requirements				requirements;
	
	/**
	 * List of current values
	 */
	protected		CurrentValues				currentValues;
	
	/**
	 * List of options and currently chosen options
	 */
	protected		OptionTracker				optionTracker;
	
	
	/**
	 * String that contains the absolute time of the beginning of the collaborative modeling session.
	 * It looks at the Login events, and finds the time when both of the architects are logged in.
	 */
	private			String						beginTimeString;
	
	/**
	 * String that contains the absolute time of the end of the collaborative modeling session
	 */
	private			String						endTimeString;
	
	/**
	 * ScreenLogger that manages screen output
	 */
	protected 		ScreenLogger 				screenLogger 				= new ScreenLogger();
	
//////////////////////////////////////////////////////////////////////////
//
// File paths
//
//////////////////////////////////////////////////////////////////////////	
	
	private			LogFilePaths				lfp;
	
	private			InconsistencyMapping		syntacticIncMap 			= new InconsistencyMapping();
	private			InconsistencyMapping		semanticIncMap_warning		= new InconsistencyMapping();
	private			InconsistencyMapping		semanticIncMap_overall		= new InconsistencyMapping();
	private			InconsistencyMapping		semanticIncMap_component	= new InconsistencyMapping();						
	
	private			List<UnhandledInconsistency> inconsistenciesAtSnapshots = new ArrayList<>();
	private			List<UnhandledInconsistency> inconsistenciesAtUpdates	= new ArrayList<>();

	
//////////////////////////////////////////////////////////////////////////
//
// Constructors
//
//////////////////////////////////////////////////////////////////////////
	
	/**
	 * Default constructor of XTEAMLogAnalyzer
	 */
	public XTEAMLogAnalyzer (LogFilePaths lfp) {
		
		// stores the paths
		this.lfp = lfp;
		
		printMsg("dataPath: " 	+ lfp.getDataPath());
		printMsg("resultPath: "	+ lfp.getResultPath());
		
		// reads in list of target analyses
		try {
			requirements = new Requirements(0, screenLogger);
		} catch (Exception e){
			printMsg("Error while reading the list of target analyses: " + e.getMessage());
		}	
	}
	
	
//////////////////////////////////////////////////////////////////////////
//
// Member methods
//
//////////////////////////////////////////////////////////////////////////
		
	/**
	 * Controls the main process
	 */
	public void analyze() {
		// reads all log files
		readLogFiles();
		
		// analyzes the inconsistencies and snapshots
		analyzeInconsistency();
		
		// analyzes the events and options
		analyzeOptions();
	}
	
	/**
	 * Reads all log files
	 */
	private void readLogFiles() {
		// reads analyses log file
		try {
			readAnalysesLogFile(lfp.getDataPath().resolve(lfp.getLogAnalysisPath()), entries_analysis);
		} catch (IOException ioe) {
			printMsg("Error: Failed to read " + lfp.getLogAnalysisPath().getFileName() + ": " + ioe);
			return;
		}
		
		// reads events log file
		try {
			readEventLogFile(lfp.getDataPath().resolve(lfp.getLogEventPath()), entries_event);
			readSynchronizationLogFile(lfp.getDataPath().resolve(lfp.getLogEventPath()), entries_analysis);
			readSynchronizationLogFile(lfp.getDataPath().resolve(lfp.getLogEventPath()), entries_event);
			readLoginEventsFromEventLogFile(lfp.getDataPath().resolve(lfp.getLogEventPath()), entries_login);
		} catch (IOException ioe) {
			printMsg("Error: Failed to read " + lfp.getLogEventPath().getFileName() + ": " + ioe);
			return;
		}
	}
	
	/**
	 * Analyzes the inconsistencies and snapshots
	 */
	private void analyzeInconsistency() {
		// sorts entries, ordered by the Arrival Time
		sortLogs(entries_analysis);
		
		// remembers the very last arrival timestamp
		endTimeString = entries_analysis.get(entries_analysis.size()-1)[indexArrivalTime];
		
		// the list of usernames in this session
		List<String> usernames;
		
		try {
			// finds Usernames
			usernames = findUsernames(entries_analysis);
			
			// initiates mappings for the usernames found
			for(String username : usernames) {
				syntacticIncMap.newUsername(username);
				semanticIncMap_warning.newUsername(username);
				semanticIncMap_overall.newUsername(username);
				semanticIncMap_component.newUsername(username);
			}
		} catch (Exception e) {
			printMsg("Error while making a list of usernames:" + e);
			return;
		} 
		
		// A session begins when all of the users logged in
		// finds when the session began by finding the latest first Login event among the users
		Calendar latestLoginTime;
		try {
			latestLoginTime = Utility.convertTimeStoC(entries_login.get(0)[indexTimestamp]);
			for(String username : usernames) {
				// finds the first login time of this username
				for(String[] entry : entries_login) {
					if(entry[indexUsername].trim().equals(username)) {
						// if this login time is after the latestLoginTime
						Calendar firstLoginTimeOfThisUsername = Utility.convertTimeStoC(entry[indexTimestamp]);
						if(Utility.timeDiff(latestLoginTime, firstLoginTimeOfThisUsername) > 0) {
							latestLoginTime = firstLoginTimeOfThisUsername;
						}
						break;
					}
				}
			}
		} catch (ParseException e1) {
			printMsg("Error while searching for the latest login time: " + e1.toString());
			return;
		} catch (Exception e) {
			printMsg("Unknown error while searching for the latest login time: " + e.toString());
			return;
		}
		
		// stores the session beginning time
		beginTimeString = Utility.convertTimeCtoS(latestLoginTime);
		
		// computes numbers
		try {
			computeLogs();
		} catch (Exception e) {
			printMsg("Error while computing numbers: " + e.toString());
			return;
		}
		
		// writes analysis logs
		writeLogs(lfp.getOutCombinedAnalysisPath(), lfp.getAllAnalysesPath(), Headers.analyses, convertsTimestampToExcelFriendly(entries_analysis_con_cnt));
		
		// writes inconsistency information
		writeConflictsInfo();
		
		// writes unhandled inconsistency at Snapshots information
		writeUnhandledInconsistenciesAtSnapshots();
		
		// writes unhandled inconsistency at Updates information
		writeUnhandledInconsistenciesAtUpdates();
	}
	
	private void analyzeOptions () {
		
		// sorts entries, ordered by the Arrival Time
		sortLogs(entries_event);
				
		// reads default attribute values
		try {
			currentValues = new CurrentValues(lfp.getResultPath().getFileName().toString(), readDATFile(lfp.getDefaultValuePath()));
		} catch (IOException ioe) {
			printMsg("Error while reading " + ioe.getMessage());
			return;
		} catch (Exception e) {
			printMsg(e.toString());
			return;
		}
		
		// reads option attribute values
		try {
			optionTracker = new OptionTracker(lfp.getResultPath().getFileName().toString(), readDATFile(lfp.getOptionValuePath()));
		} catch (IOException ioe) {
			printMsg("Error while reading " + ioe.getMessage());
			return;
		} catch (Exception e) {
			printMsg(e.toString());
			return;
		}
		
		
		// transforms from FLAME Events to human-readable format
		transformEventLog();
	
		// writes combined events and snapshots
		writeLogs(	lfp.getOutCombinedEventPath(),
					lfp.getAllEventsPath(),
					Headers.events, 
					convertsTimestampToExcelFriendly(entries_event));
		
		// removes snapshot events that do not have any design events ahead
		// Dec. 15, 2014: No need for this since it has been prevented at the GUI.
		// Dec. 15, 2014: It is still necessary for the Spring, 2014 data.
		
		// writes the human-readable value changes
		writeLogs(	lfp.getOutValueChangePath(), 
					lfp.getAllValueChangesPath(),
					Headers.value_changes, 
					entries_value_changes);
		
		// writes the human-readable option changes
		writeLogs(	lfp.getOutOptionChangePath(), 
					lfp.getAllOptionChangesPath(),
					Headers.option_changes, 
					entries_option_changes);
		
		// translates FLAME events to trade-off decision options
		
		// writes the trade-off decision options
		
	}
	
	/**
	 * Reads in the analyses log file
	 * 
	 * @param file			File to read
	 * @throws IOException
	 */
	private void readAnalysesLogFile(Path file, List<String[]> list) throws IOException {
		FileInputStream	fis			= new FileInputStream(file.toFile());
		CharsetDecoder 	decoder 	= Charset.forName("UTF-8").newDecoder();
        decoder.onMalformedInput(CodingErrorAction.IGNORE);
		BufferedReader 	reader 		= new BufferedReader(new InputStreamReader(fis, decoder)); 
				
		try {
			String line = reader.readLine(); // ignoring the header line
			if(line != null) {
				while ((line = reader.readLine()) != null) {
					String[] tokens = line.split(",");
					
					// trim
					for(int i=0; i < tokens.length; i++) {
						tokens[i] = tokens[i].trim();
					}
					
					list.add(tokens);
				}
			}
		} finally {
			try {
				reader.close();
			} catch (IOException ioe) {
				throw new IOException ("Closing BufferedReader failed: " + ioe);
			}
		}
	}
	
	/**
	 * Reads in the snapshot log file
	 * 
	 * @param file			File to read
	 * @throws IOException
	 */
	private void readSynchronizationLogFile(Path file, List<String[]> list) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(file, Constants.charset)) { 
			String line = reader.readLine(); // ignoring the header line
			if(line != null) {
				while ((line = reader.readLine()) != null) {
					String[] 	temp_tokens	= line.split(",");
					String 		eventType 	= temp_tokens[1].trim(); 
					
					// filters only the synchronization activities
					if(!eventType.equals("Snapshot") && !eventType.equals("Update")) {
						continue;
					}
					
					// duplicates absolute_time to arrival_time	
					line += ",,,,,,,,,,," + temp_tokens[0] + ",";
					
					
					String[] tokens = line.split(",");
					// trims
					for(int i=0; i < tokens.length; i++) {
						tokens[i] = tokens[i].trim();
					}
					
					list.add(tokens);
				}
			}
		}
	}
	
	/**
	 * Reads in the event log file
	 * 
	 * @param file			File to read
	 * @throws IOException
	 */
	private void readEventLogFile(Path file, List<String[]> list) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(file, Constants.charset)) { 
			String line = reader.readLine();
			if(line != null) {
				while ((line = reader.readLine()) != null) {
					
					String[] 	temp_tokens = line.split(",");
					String 		eventType 	= temp_tokens[1].trim(); 
					
					// filters out events that are not Design
					if(!eventType.equals("Design")) {
						continue;
					}
					
					// duplicates absolute_time to arrival_time
					line += ",,,,,,,,,," + temp_tokens[0] + ",";
					
					
					String[] tokens = line.split(",");
					// trims
					for(int i=0; i < tokens.length; i++) {
						tokens[i] = tokens[i].trim();
					}

					list.add(tokens);
				}
			}
		}
	}
	
	/**
	 * Reads in the login Events from the event log file
	 * @param file
	 * @param list
	 * @throws IOException
	 */
	private void readLoginEventsFromEventLogFile(Path file, List<String[]> list) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(file, Constants.charset)) { 
			String line = reader.readLine();
			if(line != null) {
				while ((line = reader.readLine()) != null) {
					
					String[] 	tokens = line.split(",");
					String 		eventType 	= tokens[1].trim(); 
					
					// filters out events that are not Design
					if(!eventType.equals("Login")) {
						continue;
					}
					
					list.add(tokens);
				}
			}
		}
	}
	
	/**
	 * Reads .dat files and returns a list of String lines
	 * 
	 * @param file			File to read
	 * @return				List of String lines
	 * @throws IOException	Cannot read the file
	 */
	private List<String> readDATFile(Path file) throws IOException {
		// the lines read
		List<String> lines = new ArrayList<>();
		
		try (BufferedReader reader = Files.newBufferedReader(file, Constants.charset)) { 
			String line = reader.readLine();
			if(line != null) {
				while ((line = reader.readLine()) != null) {
					// if the line is empty, continue
					if(line.length() == 0) {
						continue;
					}
					
					// if the line starts with a #, continue
					if(line.charAt(0) == '#') {
						continue;
					}
					
					lines.add(line);
				}
			}
		}
		
		return lines;
	}
	
	/**
	 * Opens and closes inconsistencies according to the given input data
	 * 
	 * @throws Exception	Unknown type of data found
	 */
	private void computeLogs() throws Exception {
		
		// iterates through the entries
		for(String[] entry : entries_analysis) {
			
			if(entry.length < maxNumberOfColumns) {
				throw new Exception ("Entry has too few arguments [" + entry.length + "]: " + entry);
			}
			
			// Somehow, XTEAM entries have one extra element at the end
			int length_manipulation = 0;
			
			// computes numbers!
			String arrival_time = entry[indexArrivalTime];;
			String username		= entry[indexUsername];
			switch (entry[indexEventName]) {
				
				// opens and closes inconsistencies according to the analysis result
				case "XTEAM":
					try {
						handleXTEAM(entry);
					} catch (Exception e) {
						throw new Exception ("Cannot handle an XTEAM event: " + e.toString());
					}
					break;
					
				// adds all unhandled inconsistencies to the inconsistencies at snapshot list
				case "Snapshot":
					handleSnapshot(username, arrival_time);
					length_manipulation = 1;
					break;
					
				// adds all unhandled inconsistencies to the inconsistencies at update list
				case "Update":
					handleUpdate(username, arrival_time);
					length_manipulation = 1;
					break;
					
				default:
					throw new Exception ("An entry with unknown type has been found: " + entry[indexEventName]);
			}
			
			// creates a new entry with inconsistency counters at the end
			int length = entry.length + length_manipulation;
			String[] new_entry_con_cnt = new String[length + 5];
			System.arraycopy(entry, 0, new_entry_con_cnt, 0, entry.length);
			
			int total 		= 0;				// total conflict counter
			int con_index	= length;			// the index of the syntactic conflict in the entry
			
			// syntactic 
			new_entry_con_cnt[con_index - 1] 	= String.format("%d", syntacticIncMap.getOpenIncs().get(username).size());
			total 								= total + syntacticIncMap.getOpenIncs().get(username).size();
			
			// semantic warning
			new_entry_con_cnt[con_index]		= String.format("%d", semanticIncMap_warning.getOpenIncs().get(username).size());
			total								= total + semanticIncMap_warning.getOpenIncs().get(username).size();
			
			// semantic conflict -- overall
			new_entry_con_cnt[con_index + 1]	= String.format("%d", semanticIncMap_overall.getOpenIncs().get(username).size());
			total								= total + semanticIncMap_overall.getOpenIncs().get(username).size();
			
			// semantic conflict -- per component
			new_entry_con_cnt[con_index + 2]	= String.format("%d", semanticIncMap_component.getOpenIncs().get(username).size());
			total								= total + semanticIncMap_component.getOpenIncs().get(username).size();
			
			// total
			new_entry_con_cnt[con_index + 3]	= String.format("%d", total);
			
			// session begin time
			try {
				new_entry_con_cnt[con_index + 4]	= Utility.convertDateForExcel(beginTimeString);
			} catch (ParseException pe) {
				throw new Exception ("Session begin time cannot be set: " + pe.toString());
			}
			
			// adds them to the list
			entries_analysis_con_cnt.add(new_entry_con_cnt);
		}
		
		// closes all inconsistencies that are still open after the computation
		try {
			syntacticIncMap.closeAllIncs(endTimeString);
			semanticIncMap_warning.closeAllIncs(endTimeString);
			semanticIncMap_overall.closeAllIncs(endTimeString);
			semanticIncMap_component.closeAllIncs(endTimeString);
		} catch (ParseException pe) {
			throw new Exception ("Closing inconsistencies failed with a parsing eror: " + pe.toString());
		} catch (Exception e) {
			throw new Exception ("Closing inconsistencies failed with an unknown error: " + e.toString());
		}
		
	}
	
	/**
	 * Computes logs for XTEAM Events
	 * @param entry
	 * @throws Exception
	 */
	private void handleXTEAM(String[] entry) throws Exception {
		String			username								= entry[indexUsername];
		String 			arrival_time 							= entry[indexArrivalTime];
		
		/////////////////////////////////////////////////////////////////////////
		// Syntactic inconsistency
		/////////////////////////////////////////////////////////////////////////
		Set<Inconsistency> user_open_syntactic_inc 				= syntacticIncMap.getOpenIncs().get(username);
		Set<Inconsistency> user_closed_syntactic_inc 			= syntacticIncMap.getClosedIncs().get(username);
		
		// checks if any of the maps is missing
		if(user_open_syntactic_inc == null || user_closed_syntactic_inc == null ) {
			throw new Exception ("An entry does not have a matching inconsistency map: " + flattenEntry(entry));
		}
				
		// checks if the entry carries syntactic inconsistency
		if (entry.length < indexSyntacticInconsistency + 1) {
			throw new Exception ("An XTEAM entry does not have the syntactic inconsistency field");
		} 
			
		// parses the message
		List<String> syntactic_inconsistencies = parseMultiLineField(entry[indexSyntacticInconsistency]);
		
		/////////////////////////////////// First iteration
		//  to close open inconsistencies that are not included in the new inconsistencies (syntactic_Inconsistencies).
		for(Iterator<Inconsistency> i = user_open_syntactic_inc.iterator(); i.hasNext();) {
			Inconsistency inc = i.next();
			boolean match = false;
			for(String syntactic_inconsistency : syntactic_inconsistencies) {
				// checks if the new inconsistency matches an open inconsistency
				if(inc.getInconsistencyMessage().equals(syntactic_inconsistency)) {
					match = true;
					break;
				}
			}
			
			// if there is no match, close the inconsistency
			if(!match) {
				inc.setEndTime(arrival_time);
				user_closed_syntactic_inc.add(inc.duplicate());
				i.remove();
			}
		}
		
		/////////////////////////////////// Second iteration
		// to open new open inconsistencies that had not existed in the session.
		for(String syntactic_inconsistency : syntactic_inconsistencies) {
			
			// "None" represents no inconsistency, continues
			if(syntactic_inconsistency.equals("None")) {
				continue;
			}
			
			boolean match = false;
			for(Inconsistency inc : user_open_syntactic_inc) {
				if(inc.getInconsistencyMessage().equals(syntactic_inconsistency)) {
					match = true;
					break;
				}
			}
			
			// if there is no match, open a new inconsistency
			if(!match) {
				user_open_syntactic_inc.add(new Inconsistency (arrival_time, syntactic_inconsistency));
			}
		}
		
		/////////////////////////////////////////////////////////////////////////
		// Semantic inconsistency: Warning
		/////////////////////////////////////////////////////////////////////////
		Set<Inconsistency> user_open_semantic_inc_warning		= semanticIncMap_warning.getOpenIncs().get(username);
		Set<Inconsistency> user_closed_semantic_inc_warning		= semanticIncMap_warning.getClosedIncs().get(username);
		
		// checks if any of the maps is missing
		if (user_open_semantic_inc_warning == null || user_closed_semantic_inc_warning == null) {
			throw new Exception ("An entry does not have a matching inconsistency map: " + flattenEntry(entry));
		}
		
		// parses the messages
		List<String> semantic_inconsistencies_warning = parseWarnings(entry[indexWarning]);
		
		/////////////////////////////////// First iteration
		//  to close open inconsistencies that are not included in the new inconsistencies
		for(Iterator<Inconsistency> i = user_open_semantic_inc_warning.iterator(); i.hasNext();) {
			Inconsistency inc = i.next();
			boolean match = false;
			for(String semantic_inconsistency_warning : semantic_inconsistencies_warning) {
				// checks if the new inconsistency matches an open inconsistency
				if(inc.getInconsistencyMessage().equals(semantic_inconsistency_warning)) {
					match = true;
					break;
				}
			}
			
			// if there is no match, close the inconsistency
			if(!match) {
				inc.setEndTime(arrival_time);
				user_closed_semantic_inc_warning.add(inc.duplicate());
				i.remove();
			}
		}
		
		/////////////////////////////////// Second iteration
		// to open new open inconsistencies that had not existed in the session.
		for(String semantic_inconsistency_warning : semantic_inconsistencies_warning) {
			boolean match = false;
			for(Inconsistency inc : user_open_semantic_inc_warning) {
				if(inc.getInconsistencyMessage().equals(semantic_inconsistency_warning)) {
					match = true;
					break;
				}
			}
			
			// if there is no match, open a new inconsistency
			if(!match) {
				user_open_semantic_inc_warning.add(new Inconsistency (arrival_time, semantic_inconsistency_warning));
			}
		}
		
		/////////////////////////////////////////////////////////////////////////
		// Semantic inconsistency: Overall
		/////////////////////////////////////////////////////////////////////////
		Set<Inconsistency> user_open_semantic_inc_overall		= semanticIncMap_overall.getOpenIncs().get(username);
		Set<Inconsistency> user_closed_semantic_inc_overall		= semanticIncMap_overall.getClosedIncs().get(username);
		
		// checks if any of the map (username-inconsistency) is missing
		if (user_open_semantic_inc_overall == null || user_closed_semantic_inc_overall == null) {
			throw new Exception ("An entry does not have a matching inconsistency map: " + flattenEntry(entry));
		}
		
		// gets the analysis type (Energy, Latency, Memory ...)
		String analysis_type = entry[indexAnalysisType].toLowerCase();
		if (analysis_type.length() == 0) {
			// finishes handling if no analysis type
			return;
		}
		
		// checks if there was any warning for that analysis type
		for(String semantic_inconsistency_warning : semantic_inconsistencies_warning) {
			if(semantic_inconsistency_warning.split(":")[0].trim().toLowerCase().equals(analysis_type)) {
				return;
			}
		}
		
		// checks if there is a threshold value set for the analysis type in threshold.csv
		if(!requirements.hasAnalysisType(analysis_type)) {
			return;
		}
		
		// gets the value type (Total, Maximum, Average ...)
		String value_type;
		try {
			value_type = requirements.getFirstTargetValueName(analysis_type);
		} catch (Exception e) {
			throw new Exception ("Cannot find the first target value type");
		}
		
		// gets the value direction
		String value_direction;
		try {
			value_direction = requirements.getFirstTargetDirection(analysis_type);
		} catch (Exception e) {
			throw new Exception ("Cannot find the first target value direction");
		}
					
		// finds the index that the overall value of this entry is located
		int index_overall_value;
		switch(value_type.toLowerCase()) {
			case "total":
				index_overall_value = indexOverallTotal;
				break;
			
			case "maximum":
				index_overall_value = indexOverallMax;
				break;
			
			case "average":
				index_overall_value = indexOverallAverage;
				break;
				
			default:
				throw new Exception ("Unknown value type is found: " + value_type);
		}
		
		// values that this entry carries
		String			semantic_overall_string;
		double			semantic_overall_value;
		
		// checks if this entry has the value 
		if(entry.length < index_overall_value + 1) {
			return;
		}
		
		// takes the overall value field, and checks if this entry carries the overall value
		semantic_overall_string = entry[index_overall_value].trim();
		if(semantic_overall_string.length() != 0) {
			// splits the string, and gets the overall value from the string
			String[] 	tokens 		= semantic_overall_string.split(" ");
			String 		last_token 	= tokens[tokens.length-1].trim();
			try {
				semantic_overall_value = Double.parseDouble(last_token);
			} catch (NumberFormatException nfe) {
				throw new Exception ("The token of [" + semantic_overall_string + "] is unparsable");
			}
			
			// checks if the value is over or below the threshold value
			// if the value is 0, that is also a conflict
			if(!requirements.isSatisfied(analysis_type, value_type, semantic_overall_value) || semantic_overall_value == 0) {
			
				// checks if there is no open semantic inconsistency that is the same type
				boolean found = false;
				for(Inconsistency inc : user_open_semantic_inc_overall) {
					String message = inc.getInconsistencyMessage();
					if(message.split(":")[0].trim().toLowerCase().equals(analysis_type)) {
						found = true;
					}
				}
				
				// in case there is none of the same type semantic inconsistency already open; opens a new inconsistency
				if(!found) {
					if(semantic_overall_value != 0) {
						user_open_semantic_inc_overall.add(new Inconsistency (arrival_time, 	Utility.toCamelCase(analysis_type) + 
																								": Overall " + value_type + " violates requirement [" + 
																								semantic_overall_value + 
																								" " + value_direction + " " + 
																								requirements.getFirstTargetThreshold(analysis_type) + 
																								"]"));	
					} else {
						user_open_semantic_inc_overall.add(new Inconsistency (arrival_time, 	Utility.toCamelCase(analysis_type) + 
																								": Overall " + value_type + " is not supposed to be zero"));
					}
				}
			} 
			// if the value is equal to or less than the max value threshold
			else {
				// checks if there is an open semantic inconsistency in the same type
				for(Iterator<Inconsistency> i = user_open_semantic_inc_overall.iterator(); i.hasNext();) {
					Inconsistency inc = i.next();
					String message = inc.getInconsistencyMessage();
					if(message.split(":")[0].trim().toLowerCase().equals(analysis_type)) {
						// sets the end time of the inconsistency
						inc.setEndTime(arrival_time);
						
						// moves to the closed set
						user_closed_semantic_inc_overall.add(inc.duplicate());
						i.remove();
					}
				}
			}
		}
	}
	
	/**
	 * Splits a message string based on "\n" letters
	 * 
	 * @param field			Message string
	 * @return				Split tokens
	 */
	private List<String> parseMultiLineField(String field) {
		List<String> list = new ArrayList<>(); 
		
		// parses the syntactic inconsistency field
		String[] inconsistency_strings = field.split("\\\\n");
		
		// iterates through the list of inconsistencies
		for(String inconsistency_string : inconsistency_strings) {
			list.add(inconsistency_string);
		}
	
		return list;
	}
	
	/**
	 * Splits a message string based on "\n" letters, and filter out the ones that are not used according to the configuration
	 * 
	 * @param field			Message string
	 * @return				Split tokens
	 */
	private List<String> parseWarnings(String field) {
		List<String> full_list 	= parseMultiLineField(field);
		List<String> list 		= new ArrayList<>();
		
		for(String inconsistency_string : full_list) {
			boolean found = false;
			for(String type : requirements.getAnalysisTypes()) {
				if(type.toLowerCase().equals(inconsistency_string.split(":")[0].trim().toLowerCase())) {
					// if the warning's type matches a type that is in the threshold map
					found = true;
					break;
				}
			}
			
			if(found) {
				list.add(inconsistency_string);
			}
		}
		
		return list;
	}
	
	/**
	 * Flattens a tokenized entry into a string to print out
	 * @param entry			Tokenized entry
	 * @return				Flattened String
	 */
	private String flattenEntry(String [] entry) {
		String entry_output = new String();
		for(String token : entry) {
			entry_output += token + ",";
		}
		return entry_output;
	}
	
	/**
	 * Adds all open inconsistencies to the unhandled inconsistencies set for a Snapshot
	 * 
	 * @param arrival_time	Time string of the Snapshot
	 */
	private void handleSnapshot (String username, String arrival_time) {
		try {
			addUnhandledInconsistencies(username, inconsistenciesAtSnapshots, arrival_time, syntacticIncMap.getOpenIncs(), 			"syntactic");
			addUnhandledInconsistencies(username, inconsistenciesAtSnapshots, arrival_time, semanticIncMap_overall.getOpenIncs(), 	"semantic_overall");
			addUnhandledInconsistencies(username, inconsistenciesAtSnapshots, arrival_time, semanticIncMap_component.getOpenIncs(), 	"semantic_component");
			
			addCommitTimeToOpenInconsistencies(username, syntacticIncMap.getOpenIncs(), arrival_time);
			addCommitTimeToOpenInconsistencies(username, semanticIncMap_overall.getOpenIncs(), arrival_time);
			addCommitTimeToOpenInconsistencies(username, semanticIncMap_component.getOpenIncs(), arrival_time);
		} catch (ParseException e) {
			printMsg("Error: Unable to parse the commit time: " + arrival_time);
		}
	}
	
	/**
	 * Adds all open inconsistencies to the unhandled inconsistencies set for an Update
	 * @param username
	 * @param arrival_time
	 */
	private void handleUpdate (String username, String arrival_time) {
		try {
			addUnhandledInconsistencies(username, inconsistenciesAtUpdates, arrival_time, syntacticIncMap.getOpenIncs(), 				"syntatic");
			addUnhandledInconsistencies(username, inconsistenciesAtUpdates, arrival_time, semanticIncMap_overall.getOpenIncs(), 		"semantic_overall");
			addUnhandledInconsistencies(username, inconsistenciesAtUpdates, arrival_time, semanticIncMap_component.getOpenIncs(), 	"semantic_component");
			
			addUpdateTimeToOpenInconsistencies(username, syntacticIncMap.getOpenIncs(), arrival_time);
			addUpdateTimeToOpenInconsistencies(username, semanticIncMap_overall.getOpenIncs(), arrival_time);
			addUpdateTimeToOpenInconsistencies(username, semanticIncMap_component.getOpenIncs(), arrival_time);
		} catch (ParseException e) {
			printMsg("Error: Unable to parse the update time: " + arrival_time);
		}
	}
	
	/**
	 * Adds the unhandled inconsistencies at synchronizations
	 * 
	 * @param list				The list of unhandled inconsistencies
	 * @param arrival_time		The time the sync happened
	 * @param inconsistencies	The list of inconsistencies
	 * @param type				The type of inconsistencies
	 * @throws ParseException
	 */	
	private void addUnhandledInconsistencies(	String username,
												List<UnhandledInconsistency> list, 
												String arrival_time, 
												Map<String, Set<Inconsistency>> inconsistencies, 
												String type) throws ParseException {
		for (String engine_name : inconsistencies.keySet()) {
			Set<Inconsistency> incs = inconsistencies.get(engine_name);
			
			for(Inconsistency inc : incs) {
				list.add(new UnhandledInconsistency(type, engine_name, username, arrival_time, inc.duplicate()));
			}
		}
	}
	
	/**
	 * Records the commits (snapshots) performed while inconsistencies exist
	 * @param username
	 * @param inconsistencies
	 * @param arrival_time
	 * @throws ParseException
	 */
	private void addCommitTimeToOpenInconsistencies(String username, Map<String, Set<Inconsistency>> inconsistencies, String arrival_time) throws ParseException {
		for (String key : inconsistencies.keySet()) {
			Set<Inconsistency> incs = inconsistencies.get(key);
			
			for(Inconsistency inc : incs) {
				inc.setCommitted(username, arrival_time);
			}
		}
	}
	
	/**
	 * Records the updates performed while inconsistencies exist
	 * @param username
	 * @param inconsistencies
	 * @param arrival_time
	 * @throws ParseException
	 */
	private void addUpdateTimeToOpenInconsistencies(String username, Map<String, Set<Inconsistency>> inconsistencies, String arrival_time) throws ParseException {
		for (String key : inconsistencies.keySet()) {
			Set<Inconsistency> incs = inconsistencies.get(key);
			
			for(Inconsistency inc :incs) {
				inc.setUpdated(username, arrival_time);
			}
		}
	}
	
	/**
	 * Writes a list into a log file with the headers
	 * 
	 * @param logFile		Target log file
	 * @param allLogFile	Combined log file
	 * @param headers		Header String array
	 * @param list			List of String arrays
	 */
	private void writeLogs(Path logFile, Path allLogFile, String[] headers, List<String[]> list) {
		
		// writes logs
		try (OutputStream out = Files.newOutputStream(lfp.getResultPath().resolve(logFile))) {
			
			// writes the headers 
			writeEntry(out, headers);
			
			// writes the entries
			for(String[] entry : list) {
				writeEntry(out, entry);
			}
			
		} catch (IOException ioe) {
			printMsg("Error: Failed to write a log: " + ioe.toString());
		}
		
		
		
		// checks if the combined log file exists
		boolean firstTime = false;
		if(Files.notExists(lfp.getAllResultPath().resolve(allLogFile), LinkOption.NOFOLLOW_LINKS)) {
			firstTime = true;			
		}
		
		// writes the combined logs
		try (OutputStream out = Files.newOutputStream(	lfp.getAllResultPath().resolve(allLogFile),
														StandardOpenOption.CREATE,
														StandardOpenOption.APPEND)) {
			
			// writes the headers if this is the first time writing on the combined log file
			if(firstTime) {
				// generates a new header with the extra fields that combined log files have
				String[] newHeader = new String [Headers.extra_headers_for_all.length + headers.length];
				System.arraycopy(Headers.extra_headers_for_all, 0, newHeader, 0, Headers.extra_headers_for_all.length);
				System.arraycopy(headers, 0, newHeader, Headers.extra_headers_for_all.length, headers.length);
				
				// writes the new headers
				writeEntry(out, newHeader);
			}
			
			// generates the extra fields that combined log files have
			String 		resultPath 	= lfp.getResultPath().getFileName().toString();
			String[]	tokens		= resultPath.split("_");
			String[]	extraFields	= new String[3];
			
			// checks the length
			if(tokens.length != 7) {
				throw new Exception("Error: Failde to parse a target directory name: " + resultPath);
			}
			
			// gets the team number
			extraFields[0] = tokens[4].substring(4);
			
			// gets the session number
			extraFields[1] = tokens[5].substring(7);
			
			// gets whether proactive conflict detection was provided: 1 is yes, 0 is no 
			extraFields[2] = tokens[6].compareTo("with") == 0 ? "1" : "0";
			
			
			// writes the entries
			for(String[] entry : list) {
				// generates a new entry with the extra fields that combined log files have
				String[] newEntry = new String [extraFields.length + entry.length];
				System.arraycopy(extraFields, 0, newEntry, 0, extraFields.length);
				System.arraycopy(entry, 0, newEntry, extraFields.length, entry.length);
				
				writeEntry(out, newEntry);
			}
			
		} catch (Exception e) {
			printMsg("Error: Failed to write a log: " + e.toString());
		}
	}
	
	/**
	 * Writes an entry into a log file
	 * 
	 * @param out			Target log file output stream
	 * @param entry			Entry to write
	 * @throws IOException	Write failed
	 */
	private void writeEntry(OutputStream out, String[] entry) throws IOException{
		out.write(flattenEntry(entry).getBytes());
		out.write(Constants.endl.getBytes());
	}
	
	/**
	 * Writes inconsistencies information
	 */
	private void writeConflictsInfo() {

		// interprets conflicts information to printable format 
		List<String[]> conflicts = new ArrayList<>();
		conflicts.addAll(conflictsToStrings(syntacticIncMap.getClosedIncs(), "syntactic"));
		conflicts.addAll(conflictsToStrings(semanticIncMap_warning.getClosedIncs(), "semantic_warning"));
		conflicts.addAll(conflictsToStrings(semanticIncMap_overall.getClosedIncs(), "semantic_overall"));
		conflicts.addAll(conflictsToStrings(semanticIncMap_component.getClosedIncs(), "semantic_per_component"));
		
		// writes them into the file
		writeLogs(lfp.getOutInconsistencyPath(), lfp.getAllConflictsPath(), Headers.conflicts, conflicts);
	}
	
	private List<String[]> conflictsToStrings(Map<String, Set<Inconsistency>> inconsistencies, String type) {
		List<String[]> conflicts = new ArrayList<>();
		
		for(String key : inconsistencies.keySet()) {
			Set<Inconsistency> incs = inconsistencies.get(key);
			
			for(Inconsistency inc : incs) {
				try {
					conflicts.add(conflictToString(type, key, inc));
				} catch (Exception e) {
					printMsg("Error: Failed to write " + type + " inconsistency information: " + e);
				}
			}
		}
		
		return conflicts;
	}
	
	/**
	 * Flatten a conflict record to a String
	 * @param type				Conflict type
	 * @param username			Username (XTEAM Engine's)
	 * @param inc				Conflict
	 * @return
	 * @throws Exception
	 */
	private String[] conflictToString(String type, String username, Inconsistency inc) throws Exception {
		String line[] = new String[12];
		line[0] = Utility.convertDateForExcel(beginTimeString);					// session begin time
		line[1] = type;															// conflict type
		line[2] = username;														// username
		line[3] = Utility.convertDateForExcel(inc.getStartTimeString());		// start time
		
		Integer commitCounts = new Integer(inc.getCommitCounter());
		line[4] = commitCounts.toString();										// number of times performed commits
		if(commitCounts.intValue() > 0) {
			line[5] = Utility.convertDateForExcel(inc.getFirstCommitTime());	// time first committed
		} else {
			line[5] = "";
		}
		
		Integer updateCounts = new Integer(inc.getUpdateCounter());
		line[6] = updateCounts.toString();										// number of times performed updates
		if(updateCounts.intValue() > 0) {
			line[7] = Utility.convertDateForExcel(inc.getFirstUpdateTime());	// time first updated
		} else {
			line[7] = "";
		}
		
		line[8] = Utility.convertDateForExcel(inc.getEndTimeString());			// end time
		line[9] = inc.getUnresolved() ? "0" : "1";								// resolved by architect?
		line[10] = Long.toString(inc.getLifetimeInSecs());						// lifetime
		line[11] = inc.getInconsistencyMessage();								// inconsistency message
		return line;
	}
	
	/**
	 * Converts timestamps to the Excel-friendly format
	 * 
	 * @param list
	 */
	private List<String[]> convertsTimestampToExcelFriendly (List<String[]> list) {
		// transforms the timestamps to Excel-friendly format
		for(String [] entry : list) {
			try {
				entry[indexTimestamp] 	= Utility.convertDateForExcel(entry[0]);
				entry[indexArrivalTime] = Utility.convertDateForExcel(entry[16]);
			} catch (ParseException e) {
				String entryString = new String();
				for(int i=0; i < entry.length; i++) {
					entryString += entry[i] + ", ";
				}
				printMsg("Timestamp conversion to Excel-friendly format failed: " + entryString);
				continue;
			}
		}
		
		return list;
	}
	
	private void writeUnhandledInconsistenciesAtSnapshots() {
		List<String[]> unhandledConflicts = new ArrayList<>();
		
		// for all usernames of closed semantic inconsistencies (per-component value)
		for(UnhandledInconsistency inc : inconsistenciesAtSnapshots) {
			try {
				unhandledConflicts.add(unhandledConflictToString(inc));
			} catch (Exception e) {
				printMsg("Error: Failed to write an unhandled conflicts at commits information: " + e);
			}
		}
		
		// writes them into the file
		writeLogs(	lfp.getOutSnapshotPath(), 
					lfp.getAllConflictsAtCommitsPath(), 
					Headers.conflicts_at_sync, 
					unhandledConflicts);
		
	}
	
	private void writeUnhandledInconsistenciesAtUpdates() {
		List<String[]> unhandledConflicts = new ArrayList<>();
		
		// for all usernames of closed semantic inconsistencies (per-component value)
		for(UnhandledInconsistency inc : inconsistenciesAtUpdates) {
			try {
				unhandledConflicts.add(unhandledConflictToString(inc));
			} catch (Exception e) {
				printMsg("Error: Failed to write an unhandled conflicts at updates information: " + e);
			}
		}
		
		// writes them into the file
		writeLogs(	lfp.getOutUpdatePath(), 
					lfp.getAllConflictsAtUpdatesPath(), 
					Headers.conflicts_at_sync, 
					unhandledConflicts);
	}

	private String[] unhandledConflictToString(UnhandledInconsistency inc) throws Exception {
		String[] line = new String[7];
		line[0] = inc.getInconsistencyType();
		line[1] = inc.getEngineName();
		line[2] = inc.getUsername();
		line[3] = Utility.convertDateForExcel(inc.getStartTimeString());
		line[4] = Utility.convertDateForExcel(inc.getSyncActionTimeString());
		line[5] = Long.toString(inc.getLifetimeUntilSyncAction());
		line[6] = inc.getInconsistencyMessage();
		return line;
	}
	
	/**
	 * Transforms the raw design events to human-readable format
	 */
	private void transformEventLog() {
		for (String[] line : entries_event) {
			
			/*
			 * In entries_event, there are Design events and Snapshot events
			 * In this method, it only manipulates the Design events. It passes if it's something else. 
			 */
			if(!line[indexEventName].equals("Design")) {
				continue;
			}
			
			// checks if the value field exists 
			if(line.length < indexValue + 1) {
				printMsg("Error: A Design event does not have the value attributed.");
				continue;
			}
			
			// gets the necessary information for output
			String timestamp		= line[indexTimestamp]; 	// timestamp
			String username			= line[indexUsername];		// username
			String eventID			= line[indexEventID];		// event ID
			String value 			= line[indexValue];			// value field
			
			// tokenizes what's in the value field
			String[] tokens 		= value.trim().split(delimiterValue);

			// indexes of the value field
			int indexObjectID		= 1;
			int indexEventType 		= 2;
			int indexAttributes		= 10;
			
			/*
			 * The Design events could be of various operations.
			 * In the FLAME experiment conducted in Spring 2014, the students were
			 * directed to make only the ATTR events -- the ones that occur when attribute values
			 * of model objects are modified. All others will thus be passed.
			 */
			if(!tokens[indexEventType].equals("ATTR")) {
				continue;
			}
			
			// checks the length
			if(tokens.length < 11) {
				printMsg("Too few elements in the value: " + value);
			}
			
			// parses the attributes
			Map<String, String> mapAttrValue = parseAttributes(tokens[indexAttributes]);
			
			String 	objectID			= tokens[indexObjectID];
			String 	valueChanges 		= new String();
			String	optionChanges		= new String();
			
			// updates current values
			for(String attrName : mapAttrValue.keySet()) {
				List<ValueChange> vcs = currentValues.updateValue(objectID, attrName, mapAttrValue.get(attrName));
				
				// if any value has changed
				for(int i=0; i < vcs.size(); i++) {
					ValueChange vc = vcs.get(i);
					valueChanges += new String (	vc.getAttributeName() + " of " +
													vc.getParentObjectName() + "/" +
													vc.getObjectName() + ": from " +
													String.format("%.2f", vc.getCurrentValue()) + 
													" to " + String.format("%.2f", vc.getNewValue()));
					if(i < vcs.size() - 1) {
						valueChanges += "|";
					}
					
					String[] entry = new String[8];
					entry[0] = timestamp;
					entry[1] = username;
					entry[2] = eventID;
					entry[3] = vc.getParentObjectName();
					entry[4] = vc.getObjectName();
					entry[5] = vc.getAttributeName();
					entry[6] = String.format("%.2f", vc.getCurrentValue());
					entry[7] = String.format("%.2f", vc.getNewValue());
					
					entries_value_changes.add(entry);
				}
			}
			
			// updates the human-readable value change field
			if(valueChanges.length() > 0) {
				line[indexHumanReadableValue] = valueChanges;
			}
			
			// updates current options
			for(String attrName : mapAttrValue.keySet()) {
				List<OptionChange> ocs = optionTracker.updateOption(objectID, attrName, mapAttrValue.get(attrName));
				
				// if any option has changed
				for(int i=0; i < ocs.size(); i++) {
					OptionChange oc = ocs.get(i);
					optionChanges += new String (	"[" + oc.getTaskNumber() +
													"/" + oc.getParticipantNumber() +
													"/" + oc.getObjectNumber() + 
													"] changed from " + oc.getCurrentOption() +
													" to " + oc.getNewOption());
													
					if(i < ocs.size() - 1) {
						optionChanges += "|";
					}
					
					String[] entry = new String[8];
					entry[0] = timestamp;
					entry[1] = username;
					entry[2] = eventID;
					entry[3] = String.format("%d", oc.getTaskNumber());
					entry[4] = String.format("%d", oc.getParticipantNumber());
					entry[5] = String.format("%d", oc.getObjectNumber());
					entry[6] = String.format("%d", oc.getCurrentOption());
					entry[7] = String.format("%d", oc.getNewOption());
					
					entries_option_changes.add(entry);
				}
			}
			
			// updates the option change field
			if(optionChanges.length() > 0) {
				line[indexOptionChange] = optionChanges;
			}
			
			
		}
	}
	
	/**
	 * Parses the attributes into a map
	 * 
	 * @param attributes
	 * @return				Map of attributes
	 */
	private Map<String, String> parseAttributes (String attributes) {
		Map<String, String> map = new TreeMap<>();
		
		String[] attribute = attributes.split("&");
		
		for (String token : attribute) {
			String[] keyAndValue = token.split("=");
			String value = new String();
			
			// checks the length
			if(keyAndValue.length > 2) {
				for(int i=1; i < keyAndValue.length; i++) {
					value += keyAndValue[i];
				}
			} else if (keyAndValue.length < 2) {
				value = "";
			} else {
				value = keyAndValue[1];
			}
			
			map.put(keyAndValue[0], value);
		}
		
		return map;
	}
	
	/**
	 * Sorts the log lines. Ordered by the absolute time.
	 */
	private void sortLogs(List<String[]> list) {
		Collections.sort(list, new EntryComparator());
	}
	
	/**
	 * The comparator used for sorting the log lines. Ordered by the absolute time.
	 * 
	 * @author <a href=mailto:jaeyounb@usc.edu>Jae young Bang</a>
	 */
	public class EntryComparator implements Comparator<String[]> {
	    @Override
	    public int compare(String[] o1, String[] o2) {
	        return o1[indexArrivalTime].compareTo(o2[indexArrivalTime]);
	    }
	}
	
	/**
	 * Looks for new Usernames and make a list of them
	 */
	private List<String> findUsernames(List<String[]> list) throws Exception {
		
		List<String> usernames = new ArrayList<>();
		
		for(String[] entry : list) {
			// checks if the entry has the Username field
			if(entry.length < indexUsername + 1) {
				throw new Exception ("An entry does not have its Username field");
			} else {
				
				String newUsername = entry[indexUsername].trim();
				
				// checks for duplicates
				boolean found = false;
				for(String username : usernames) {
					if(username.equals(newUsername)) {
						found = true;
						break;
					}
				}
				
				// adds the new username to the usernames list
				if(!found) {
					usernames.add(newUsername);
				}
			}
		}
		
		return usernames;
	}
	
	
	/**
	 * Print screen messages
	 * 
	 * @param msg			Message to print to screen
	 */
	protected void printMsg (String msg) {
		screenLogger.printMsg("Analyzer", msg);
	}

//////////////////////////////////////////////////////////////////////////
//
// The main() method and the helper methods
// 
//////////////////////////////////////////////////////////////////////////


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
	 * Print out an error message
	 * 
	 * @param msg			Message to print out
	 */
	public static void printErrMessage (String msg) {
		System.out.println("[main]: Err: " + msg);
	}
	
	/**
	 * The main() method
	 */
	public static void main(String[] args) {
		
		Path 	dataDirPath;											// path under where the data directories are
		Path	resultDirPath;											// path under where the result directory will be saved
		Path	resultPath;												// path of the result directory
		
		Path	datDefault;												// path of the default values file
		Path	datOption;												// path of the option values file
		
		Path	logEventPath;											// path of the event log file
		Path	logAnalysisPath;										// path of the analysis log file
		Path	logSnapshotPath;										// path of the snapshot log file
		
		Path	outCombinedAnalysisPath;								// path of combined analysis/snapshot file
		Path	outCombinedEventPath;									// path of combined event/snapshot file
		Path	outInconsistencyPath;									// path of inconsistency analysis file
		Path	outSnapshotPath;										// path of unhandled inconsistencies at commits file
		Path	outUpdatePath;											// path of unhandled inconsistecies at updates file
		Path	outValueChangePath;										// path of value change file
		Path	outOptionChangePath;									// path of option change file
		
		Path	outAllAnalyses;											// path of all analyses file
		Path	outAllEvents;											// path of all events file
		Path	outAllConflicts;										// path of all conflicts file
		Path	outAllConflictsAtCommits;								// path of all conflicts at commits file
		Path	outAllConflictsAtUpdates;								// path of all conflicts at updates file
		Path	outAllValueChanges;										// path of all value changes file
		Path	outAllOptionChanges;									// path of all option changes file
		
		String 	resultName 			= Utility.convertDate(System.currentTimeMillis());	// result name of this run
		
		
		// load up the properties specified in the config.properties file
		try {
			loadProps();
		} catch (Exception e) {
			printErrMessage(e.getMessage());
			return;
		}

		// gets the directory path under where the data directories are
		try {
			dataDirPath 				= Utility.getPathProperty(props, "DATA_DIRECTORY");
			resultDirPath				= Utility.getPathProperty(props, "RESULT_DIRECTORY");
			
			logEventPath				= Utility.getPathProperty(props, "FILENAME_LOG_EVENT");
			logAnalysisPath				= Utility.getPathProperty(props, "FILENAME_LOG_ANALYSIS");
			logSnapshotPath				= Utility.getPathProperty(props, "FILENAME_LOG_SNAPSHOT");
			
			datDefault					= Utility.getPathProperty(props, "FILENAME_DEFAULT_VALUES");
			datOption					= Utility.getPathProperty(props, "FILENAME_OPTIONS_VALUES");
					
			outCombinedAnalysisPath		= Utility.getPathProperty(props, "FILENAME_OUT_COMBINED_ANALYSIS");
			outCombinedEventPath		= Utility.getPathProperty(props, "FILENAME_OUT_COMBINED_EVENT");
			outInconsistencyPath		= Utility.getPathProperty(props, "FILENAME_OUT_INCONSISTENCY");
			outSnapshotPath				= Utility.getPathProperty(props, "FILENAME_OUT_SNAPSHOT");
			outUpdatePath				= Utility.getPathProperty(props, "FILENAME_OUT_UPDATE");
			outValueChangePath			= Utility.getPathProperty(props, "FILENAME_OUT_VALUE_CHANGE");
			outOptionChangePath			= Utility.getPathProperty(props, "FILENAME_OUT_OPTION_CHANGE");
			
			outAllAnalyses				= Utility.getPathProperty(props, "FILENAME_OUT_ALL_ANALYSES");
			outAllEvents				= Utility.getPathProperty(props, "FILENAME_OUT_ALL_EVENTS");
			outAllConflicts				= Utility.getPathProperty(props, "FILENAME_OUT_ALL_CONFLICTS");
			outAllConflictsAtCommits	= Utility.getPathProperty(props, "FILENAME_OUT_ALL_CONFLICTS_AT_COMMITS");
			outAllConflictsAtUpdates	= Utility.getPathProperty(props, "FILENAME_OUT_ALL_CONFLICTS_AT_UPDATES");
			outAllValueChanges			= Utility.getPathProperty(props, "FILENAME_OUT_ALL_VALUE_CHANGES");
			outAllOptionChanges			= Utility.getPathProperty(props, "FILENAME_OUT_ALL_OPTION_CHANGES");
			
		} catch (Exception e) {
			printErrMessage(e.toString());
			return;
		}
		
		// create the results directory
		try { 
			Files.createDirectories(resultDirPath);
		} catch (Exception e) {
			printErrMessage("Directory " + resultDirPath + " cannot be created");
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
					printErrMessage("Result directory [" + new_resultName + "] already exists");
					return;
				} catch (Exception e) {
					printErrMessage("Result directory [" + new_resultName + "] cannot be created");
					return;
				}
			
				mkdirSucceeded = true;
				break;
			}
		}
		
		if(mkdirSucceeded == false) {
			printErrMessage("Result directory [" + resultName + "] cannot be created");
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
			printErrMessage("Failed to read data directories: " + ioe.getMessage());
			return;
		}
		
		
		// iterates through the data directories
		for(Path dir : dataDirectories) {
			
			// creates the result directory
			Path subResultPath = resultPath.resolve(dir.getFileName());
			try { 
				Files.createDirectories(subResultPath);
			} catch (Exception e) {
				printErrMessage("The result directory " + subResultPath.getFileName() + " cannot be created.");
				return;
			}
			
			// creates the LogFilePath instance to pass to XTEAMLogAnalyzer
			LogFilePaths lfp = new LogFilePaths(	dir, 
													resultPath,
													subResultPath,
													datDefault,
													datOption,
													logEventPath,
													logAnalysisPath,
													logSnapshotPath,
													outCombinedAnalysisPath,
													outCombinedEventPath,
													outInconsistencyPath,
													outSnapshotPath,
													outUpdatePath,
													outValueChangePath,
													outOptionChangePath,
													outAllAnalyses,
													outAllEvents,
													outAllConflicts,
													outAllConflictsAtCommits,
													outAllConflictsAtUpdates,
													outAllValueChanges,
													outAllOptionChanges);
			
			// runs the LogAnalyzer per each subdirectory
			XTEAMLogAnalyzer log = new XTEAMLogAnalyzer(lfp);
			
			log.analyze();
		}
		
		System.out.println("FLAME Log Analyzer finished.");
	}

}
