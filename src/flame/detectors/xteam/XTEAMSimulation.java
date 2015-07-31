package flame.detectors.xteam;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.PatternSyntaxException;

import flame.Constants;
import flame.ScreenLogger;
import flame.Utility;
import Prism.core.Event;

/**
 * XTEAM simulation running code is all in this class
 * 
 * @author 					<a href=mailto:jaeyounb@usc.edu>Jae young Bang</a>
 * @version					2013.05
 */
public class XTEAMSimulation extends Thread {

///////////////////////////////////////////////
// Member Variables
///////////////////////////////////////////////
	
	/**
	 * Simulation code compilation mode (Debug or Release)
	 */
	private final	String 						mode = "Release";
	
	/**
	 * Time at which this simulation began
	 */
	private			String						arrival_time;
	
	/**
	 * XTEAM Engine username. For LocalV engines, this is the corresponding architect's name.
	 */
	private			String						username;
	
	/**
	 * Path to where the XTEAM simulation is
	 */
	private			Path						simulationPath;
	
	/**
	 * Path to where the XTEAM simulation code is
	 */
	private			Path						simulationCodePath;
	
	/**
	 *  Path to where the XTEAM simulation scaffold code is
	 */
	private 		Path 						scaffoldPath;		
	
	/**
	 * Target time (logical time) to which the simulation analysis runs  
	 */
	private			double						targetTime;
	
	/**
	 * Visual Studio 2008 compiler path
	 */
	private			Path						vsCompilerPath;
	
	/**
	 * Path to the file that contains what types of analyses this Engine performs
	 */
	private			Path						xteamInfo;
	
	/**
	 * Mapping from analysis name (e.g. Energy, Latency, ...) to the target value index in the simulation result file (e.g. 1, 2, ...)
	 */
	private			Map<String, Integer>		xteamAnalysisTargetValueIndex 	= new HashMap<>();
	
	/**
	 * XTEAM Engine instance
	 */
	private			XTEAMEngine					engine;
	
	/**
	 * EventReceiver instance in XTEAMEngine
	 */
	private			XTEAMEngine.EventReceiver	receiver;
	
	/**
	 * ScreenLogger instance
	 */
	private			ScreenLogger				sl;

///////////////////////////////////////////////
// Constructors
///////////////////////////////////////////////	
	
	/**
	 * Default constructor
	 * 
	 * @param mode				Simulation mode (e.g. MRV, LSV ...)
	 * @param simulationPath	Path to where the XTEAM simulation project is
	 * @param scaffoldPath		Path to where the scaffold code is
	 * @param targetTime		Target time (logical time) to which the simulation analysis runs
	 * @param vsCompilerPath	Visual Studio 2008 compiler path
	 * @param screenLogger		ScreenLogger instance
	 */
	public XTEAMSimulation (	String 						mode,
								String						username,
								Path 						simulationPath,
								Path						scaffoldPath,
								double						targetTime,
								Path						vsCompilerPath,
								Path						xteamInfo,
								ScreenLogger				screenLogger,
								XTEAMEngine					engine,
								XTEAMEngine.EventReceiver	receiver) throws Exception {
		
		//this.mode 				= mode;
		this.username			= username;
		this.simulationPath		= simulationPath;
		simulationCodePath 		= simulationPath.resolve("XTEAM_Simulation/simulation_code");
		this.scaffoldPath		= scaffoldPath;
		this.targetTime			= targetTime;
		this.vsCompilerPath		= vsCompilerPath;
		this.xteamInfo			= xteamInfo;
		sl						= screenLogger;
		this.engine				= engine;
		this.receiver			= receiver;
		arrival_time			= Utility.convertDate(System.currentTimeMillis());
		
		readXTEAMInfo();
	}
	
///////////////////////////////////////////////
// Member Methods
/////////////////////////////////////////////// 
	
	/**
	 * Reads the XTEAM_INFO file
	 * @throws Exception	Unable to parse the file
	 */
	private void readXTEAMInfo () throws Exception {
		try (	FileReader fr 		= new FileReader(xteamInfo.toString());
				BufferedReader br 	= new BufferedReader(fr)) {
			
			String line;
			
			while ((line = br.readLine()) != null) {
				// tokenize the line
				String[] tokens = line.split(",");
				if(tokens.length < 3) {
					throw new Exception ("XTEAM_INFO file has an unparsable line: " + line);
				}
				
				// reads the analysis name
				String analysisName = Utility.toCamelCase(tokens[0].trim());
				
				// reads the number index
				String valueIndex = tokens[1].trim();
				try {
					xteamAnalysisTargetValueIndex.put(analysisName, Integer.parseInt(valueIndex));
				} catch (NumberFormatException nfe) {
					throw new Exception ("XTEAM_INFO file has an unparsable index number: " + line);
				}
			}
		} catch (Exception e) {
			throw new Exception ("XTEAM_INFO file reading failed: " + e);
		}
	}

	/**
	 * Run the thread
	 */
	public void run () {
		
		try {

			List<Event> events = new ArrayList<>();
			// reads syntax error list and sends the resulting Events
			events.addAll(readSyntaxErrorList());
			for(Event newEvent : events) {
				newEvent.addParameter("SenderUsername", 	username);
				newEvent.addParameter("OriginComponent", 	"XTEAM Engine");
				newEvent.addParameter("ArrivalTime", 		arrival_time);
				
				engine.sendRequest(newEvent);
			}
			
			// if there are syntactic inconsistencies, do not run code
			if(events.size() > 0) {
				return;
			}
			
			// increases simulation event ID		
			int sim_event_id = receiver.tickEventID();
			
			// sends out simulation execution beginning notification
			Event simExecBeginning = new Event("Notification");
			simExecBeginning.addParameter("SenderUsername", 	username);
			simExecBeginning.addParameter("OriginComponent", 	"XTEAM Engine");
			simExecBeginning.addParameter("Value", 				"Simulation execution beginning");
			simExecBeginning.addParameter("EventID", 			new Integer(sim_event_id));
			simExecBeginning.addParameter("ArrivalTime", 		arrival_time);
			engine.sendRequest(simExecBeginning);
			
			// compiles and runs the simulation
			events.addAll(runXTEAMSimulation());
			
			// sends out simulation execution completion notification
			Event simExecCompletion = new Event("Notification");
			simExecCompletion.addParameter("SenderUsername", 	username);
			simExecCompletion.addParameter("OriginComponent", 	"XTEAM Engine");
			simExecCompletion.addParameter("Value", 			"Simulation execution completion");
			simExecCompletion.addParameter("EventID", 			new Integer(sim_event_id));
			simExecCompletion.addParameter("ArrivalTime", 		arrival_time);
			engine.sendRequest(simExecCompletion);
			
			// sends XTEAM simulation result
			for(Event newEvent : events) {
				newEvent.addParameter("SenderUsername", 	username);
				newEvent.addParameter("OriginComponent", 	"XTEAM Engine");
				newEvent.addParameter("ArrivalTime", 		arrival_time);
				
				engine.sendRequest(newEvent);
			}
		} catch (Exception exc) {
			printMsg("Error: XTEAM simulation cannot run: " + exc);
		}
		
	}
	
	/**
	 * Reads syntax error list and creates an XTEAM event with it
	 * 
	 * @return				An XTEAM Event
	 * @throws Exception	File reading error
	 */
	protected List<Event> readSyntaxErrorList() throws Exception {
		
		// List of Events to return
		List<Event> events = new ArrayList<>();
		
		try {
			Path syntaxErrorListPath = simulationPath.resolve("syntax_errors.list");
			ArrayList<String> synErrorList = 
					(ArrayList<String>) Files.readAllLines(syntaxErrorListPath, Constants.charset);
			
			// removes all duplicates from the list
			HashSet<String> hs = new HashSet<>();
			hs.addAll(synErrorList);
			synErrorList.clear();
			synErrorList.addAll(hs);
			
			
			// If there is any syntax error from XTEAM simulation generation
			if(synErrorList.size() > 0) {
				// Generates an XTEAM Event with the SyntacticConflicts parameter
				Event e = new Event("XTEAM");
				
				e.addParameter("SyntacticConflicts", synErrorList);

				events.add(e);
			} 
			// If there is no syntax error
			else {
				
			}
		} catch (InvalidPathException ipe) {
			throw new Exception("Cannot find the syntax error list file: " + ipe);
		} catch (IOException exc) {
			throw new Exception ("Cannot open the syntax error list file: " + exc);
		}
		
		return events;
	}

	/**
	 *  Executes and reads in the XTEAM simulation result, and creates an XTEAM Event with it
	 * 
	 * @param simulationPath		Path to the directory in which the simulation code is
	 * @return						An XTEAM Event that contains the simulation result
	 */
	protected List<Event> runXTEAMSimulation() throws Exception {
		
		// List of Events to return
		List<Event> 	events 	= new ArrayList<>();
	
		/////////////////////////////////////////
		// Gets the simulation result
		/////////////////////////////////////////
		
		// (1) Copies the scaffold code into the simulation path
		// Scaffold code is under: scaffoldPath
		
		FileCopier visitor = new FileCopier(scaffoldPath, simulationPath, sl);
		try {
			Files.walkFileTree(scaffoldPath, visitor);
		} catch (Exception exc) {
			throw new Exception ("During copying scaffold code to simulation: " + exc);
		}
		
		// (2) Finds what .cpp and .h files this simulation has
		FileFinder finder = new FileFinder(sl);
		
		// Traverses the simulation code to have the lists of files
		try {
			Files.walkFileTree(simulationCodePath, finder);
		} catch (IOException ioe) {
			throw new Exception ("During file walk-in to have list of .cpp and .h files: " + ioe);
		}
		
		// (3) Manipulates the .vcproj file
		Path simProjFile = simulationPath.resolve("XTEAM_Simulation/XTEAM_Simulation.vcproj");
		try {
	
			String template1 = new String (Files.readAllBytes(scaffoldPath.resolve("XTEAM_Simulation/1.vcproj.part")));;
			String template2 = new String (Files.readAllBytes(scaffoldPath.resolve("XTEAM_Simulation/2.vcproj.part")));;
			String template3 = new String (Files.readAllBytes(scaffoldPath.resolve("XTEAM_Simulation/3.vcproj.part")));;
			
			String vcproj =	template1 + Constants.endl +
							finder.getCPPFiles() +
							template2 + Constants.endl +
							finder.getHFiles() +
							template3;
			
			Files.write(simProjFile, vcproj.getBytes("UTF-8"));
		} catch (IOException ioe) {
			throw new Exception ("Failed to manipulate the .vcproj file: " + ioe);
		}
		
		/////////////////////////////////////////
		// Compiles the simulation code
		/////////////////////////////////////////
		
		ArrayList<String> errors;
		try {
			engine.getLock_XTEAM();
			printMsg("Compiling the simulation code for [" + arrival_time + "] ...");
			errors = compile(simProjFile);
		} finally {
			printMsg("Compilation done for [" + arrival_time + "]");
			engine.releaseLock_XTEAM();			
		}
		
		// Reads analysis warning messages
		Map<String, List<String>> analysis_warnings = readAnalysisWarnings();
		
		// If there is any syntax error from the simulation project compilation
		if(errors.size() > 0) {
			// Creates an XTEAM Event with the syntax errors
			Event e = new Event ("XTEAM");
			e.addParameter("SyntacticConflicts", errors);
			e.addParameter("AnalysisWarnings", analysis_warnings);
			events.add(e);
			return events;
		} 
		
		/////////////////////////////////////////
		// Runs the simulation code
		/////////////////////////////////////////
		
		Path simulationResultPath;
		
		printMsg("Executing the simulation code for [" + arrival_time + "] ...");
		simulationResultPath = runSimulation();
		printMsg("Execution done for [" + arrival_time + "]");
		
		// checks if the simulation completion tag file has been created,
		// if the files does not exist, that means the simulation was
		// disrupted in the middle.
		if(Files.notExists(simulationResultPath.resolve("Simulation_Completion.txt"), new LinkOption[]{LinkOption.NOFOLLOW_LINKS})) {
			// Creates an XTEAM Event with the simulation incompletion error
			ArrayList<String> sim_incompletion_error = new ArrayList<>();
			sim_incompletion_error.add(new String("Simulation execution was disrupted for an unhandled exception. Manual simulation code inspection recommended."));
			Event e = new Event ("XTEAM");
			e.addParameter("SyntacticConflicts", sim_incompletion_error);
			events.add(e);
			return events;
		}

		
		// Analyzes the results
		printMsg("Analyzing the simulation output for [" + arrival_time + "] ...");
		Results 		results 		= analyzeResults(simulationResultPath);
		Set<Result>		resultsSet 		= results.getResults(); 
		printMsg("Analysis done for [" + arrival_time + "]");
		
		// Adds the results into the Events
		for (Result result : resultsSet) {	
			String 				analysis_type 	= result.getAnalysisType();
			Event 				e 				= new Event ("XTEAM");
			ArrayList<String> 	emptyList 		= new ArrayList<>();
			emptyList.add("None");
			
			e.addParameter("AnalysisType", 			analysis_type);
			e.addParameter("SyntacticConflicts", 	emptyList);
			e.addParameter("AnalysisWarnings", 		analysis_warnings);
			
			// if there is any warning for an analysis, do not send the simulation result of that type
			boolean	found			= false;
			for(String key : analysis_warnings.keySet()) {
				if(key.equals(analysis_type) && analysis_warnings.get(key).size() > 0) {
					found = true;
					break;
				}
			}
			
			if(!found) {
				
				e.addParameter("OverallTotal", 			"Overall total:   " + String.format("%.4f", result.getTotalValue()));
				e.addParameter("OverallMax", 			"Overall maximum: " + String.format("%.4f", result.getMaxValue()));
				e.addParameter("OverallAverage", 		"Overall average: " + String.format("%.4f", result.getAvgValue()));
				
				int 	successes 	= result.getNumberOfSuccesses();
				int 	values		= result.getNumberOfValues();
				double 	ratio		= (double) successes / (double) values;
				
				e.addParameter("OverallSuccess", 		"Overall success: " 
																	+ successes + "/" + values 
																	+ "[" + String.format("%.2f", ratio*100) + "%]");
				
				e.addParameter("PerComponentTotal", 	result.getPerComponentTotalValues());
				e.addParameter("PerComponentMax", 		result.getPerComponentMaxValues());
				e.addParameter("PerComponentAverage", 	result.getPerComponentAvgValues());
				e.addParameter("PerComponentSuccess", 	result.getPerComponentNumberOfSuccesses());	
			}
			
			events.add(e);
		}
		
		return events;
	}
	
	/**
	 * Compiles the simulation code
	 * 
	 * @param simProjFile	Path to the simulation
	 * @return
	 * @throws Exception
	 */
	protected ArrayList<String> compile (Path simProjFile) throws Exception {
		
		ArrayList<String> ret = new ArrayList<> ();
		
		// The compilation error log file path
		Path compileLogPath = simulationPath.resolve("compile_log");
				
		try
		{
			String[] compileCmd = {	vsCompilerPath.toString(), "/Build", mode, 
									simProjFile.toAbsolutePath().toString(), 
									"/out", compileLogPath.toString()};
			Runtime.getRuntime().exec(compileCmd).waitFor();
		} catch (Exception e) {
			throw new Exception ("Compiling the simulation project failed: " + e);
		}
		
		// Checks if there were any errors
		Set<String> errorMessages 	= new TreeSet<>();

		if(Files.exists(compileLogPath)) {
			try {
				// Reads the compilation log file
				ArrayList<String> logs = (ArrayList<String>) Files.readAllLines(compileLogPath, Constants.charset);
				
				for (String log : logs) {
					if(log.toLowerCase().contains("error")) {
						if(!log.contains("warning")) {
							
							String[]	errorFileNameTokens		= log.split("\\(");
							Path 		errorFileName 			= Paths.get(errorFileNameTokens[0].substring(2));
							Path		errorFilePath			= simulationCodePath.resolve(errorFileName.getFileName());
							int			errorLineNumber			= Integer.parseInt(errorFileNameTokens[1].split("\\)")[0]);
							
							String		errorLine				= "";
							String[]	errorTokens 			= log.split(":");
							
							String		errorString				= new String();
							boolean		errorFileReadSuccess	= true;
							
							for(int i=2; i < errorTokens.length; i++) {
								errorString += errorTokens[i].trim() + " ";
							}
							
							// Reads the error part from the error file
							try (BufferedReader br = Files.newBufferedReader(errorFilePath, Constants.charset)) {
								String line = null;
								int lineNumber = 1;
								while((line = br.readLine()) != null) {
									if(lineNumber++ == errorLineNumber) {
										errorLine = line;
										break;
									}
								}
							} catch (IOException ioe_br) {
								printMsg ("Failed to read " + errorFilePath);
								errorFileReadSuccess = false;
							}
							
							String errorMessage = "In " + errorFilePath.getFileName() + ": " + errorString.trim();
							if(errorFileReadSuccess) {
								errorMessage += ": from this line: " + errorLine.trim();
							}
													
							if(!errorMessages.contains(errorMessage)) {
								errorMessages.add(errorMessage);
							}
						}
					}
				}
				
				if(errorMessages.size() > 0) {
					// Prints the error messages to screen
					for(String errorMessage : errorMessages) {
						ret.add(errorMessage);
					}
					
				}
			} catch (IOException e) {
				throw new Exception ("Error while reading the compilation error file: " + e);
			}
			
		} else {
			throw new Exception ("Cannot find the compilation log file");
		}
		
		return ret;
	}
	
	/**
	 * Reads the analysis warnings files generated from the simulation compilation
	 * 
	 * @return				List of warnings mapped to the analysis type names
	 * @throws Exception	Unparsable file name
	 */
	protected Map<String, List<String>> readAnalysisWarnings() throws Exception {
		Map<String, List<String>> ret = new HashMap<>();
		
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(simulationPath)) {
			for (Iterator<Path> it = ds.iterator(); it.hasNext(); ) {
				
				// Gets a file path
				Path 			file 					= (Path) it.next();
				String 			filename				= file.getFileName().toString();
				
				// If the extension is not .list, skips the file
				if(!getExtension(file).toLowerCase().equals("list")) {
					continue;
				}
				
				// gets the filename without the extension
				String[]		filename_ext_tokens		= filename.split("\\.");
				String 			filename_no_extension 	= filename_ext_tokens[0];
				String[] 		token 					= filename_no_extension.split("_");
				
				// if the token count is not 3, it is a strange file
				if(token.length != 3) {
					continue;
				}
				
				// gets the analysis type name
				String			analysis_type			= Utility.toCamelCase(token[2]);
				List<String>	analysis_result			= new ArrayList<>();
				
				// Opens the file, reads in the numbers, and analyzes them
				try (BufferedReader br = Files.newBufferedReader(file, Constants.charset)) {
					String line = null;
					while((line = br.readLine()) != null) {
						analysis_result.add(line);
					}
					
					// removes all duplicates from the list
					HashSet<String> hs = new HashSet<>();
					hs.addAll(analysis_result);
					analysis_result.clear();
					analysis_result.addAll(hs);
				}
				
				// adds the read result to the map
				ret.put(analysis_type, analysis_result);
			}
		} catch (PatternSyntaxException pse) {
			throw new Exception ("Regular expression in a split() method is invalid: " + pse);		
		}catch (Exception e) {
			throw new Exception ("Unexpected error orrcurred while reading analysis warning messages: " + e);
		}
		
		return ret;
	}
	
	/**
	 * Runs the simulation code
	 * 
	 * @return				Path to directory where the simulation result is saved
	 * @throws Exception
	 */
	protected Path runSimulation() throws Exception {
		
		Path executableDirPath 	= simulationPath.resolve(mode);
		Path executablePath		= executableDirPath.resolve("XTEAM_Simulation.exe");
		
		try
		{
			//String[] runCmd = {	executablePath.toString(), executableDirPath.toString() };
			String[] runCmd = { executablePath.toString() };
			Runtime.getRuntime().exec(runCmd, null, executableDirPath.toFile()).waitFor();
		} catch (Exception e) {
			throw new Exception ("Running simulation executable failed: " + e);
		}
		
		return executableDirPath;
	}
	
	/**
	 * Analyzes the simulation results
	 * 
	 * @param analysisType	Analysis type (Energy, Latency, ...)
	 * @return				An XTEAM Event for the given analysis type
	 * @throws Exception	Cannot complete the analysis
	 */
	protected Results analyzeResults (Path targetDir) throws Exception {
		
		Results results = new Results();		
		
		// adds the analysis types that are defined in the XTEAMInfo.dat file
		for(String key : xteamAnalysisTargetValueIndex.keySet()) {
			results.createResult(key);
		}
		
		// Iterates through the simulation directory to find .csv files
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(targetDir)) {
			
			for (Iterator<Path> it = ds.iterator(); it.hasNext(); ) {
				
				// Gets a file path
				Path file 		= (Path) it.next();
				String filename	= file.getFileName().toString();
				
				// If the extension is not .csv, skips the file
				if(!getExtension(file).toLowerCase().equals("csv")) {
					continue;
				}
				
				// Component name and analysis type from the filename
				String 		componentName 	= "";			
				String		analysisType	= "";
				
				// Gets the component name and analysis type from the filename
				try {
					componentName 	= getComponentName(file);
					analysisType 	= getAnalysisType(file);
				} catch (Exception exc) {
					printMsg("Cannot get component information from [" + filename + "]: " + exc);
					continue;
				}
				
				// Gets the Result with the AnalysisType
				Result result = results.getResult(analysisType);
				
				// gets the value index of the analysis type
				Integer 	index = xteamAnalysisTargetValueIndex.get(Utility.toCamelCase(analysisType.trim()));
				if(index == null) {
					printMsg("[" + analysisType + " for " + componentName + "] is an unknown analysis type.");
					continue;
				}
				
				double 	totalValue	= 0;		// the summation of all values 
				double	maxTime		= 0;		// the time that has the maximum value
				double	maxValue	= 0;		// the maximum of all values
				int		count		= 0;		// line count
				int		success		= 0;		// success count
				
				// Opens the file, reads in the numbers, and analyzes them
				try (BufferedReader br = Files.newBufferedReader(file, Constants.charset)) {
					
					String 	line 				= null;		// a line in the .csv file
					double	current_time		= 0;		// max value time cursor
					double	current_cum_value	= 0;		// cumulative value for the time so far
					
					// iterates through the lines in the .csv file
					while((line = br.readLine()) != null) {
						String[] lineTokens = line.split(",");
						
						// Skips the first row that has the column names
						if(count++ == 0) {
							continue;
						}
						
						// gets the time of the line
						double thisTime = Double.parseDouble(lineTokens[0]);
						
						// Checks if the line is within the target time
						if (thisTime <= targetTime) {
							// if the value is NOT a missing value
							if(!lineTokens[index].trim().equals("-")) {
								double value = Double.parseDouble(lineTokens[index]);
								
								// accumulates to total value
								totalValue	+= value;
								
								// checks if the time of the line differs from the current_time
								if(thisTime > current_time) {
									// checks if the cumulative value is larger than the max value
									if(maxValue < current_cum_value) {
										maxValue 	= current_cum_value;
										maxTime 	= current_time;
									}
									
									// resets the current values
									current_time 		= thisTime;
									current_cum_value 	= (double) 0;
								} 
								
								// accumulates to current_cum_value
								current_cum_value += value;
								
								// increases the success number
								success++;
							}
						} else {
							printMsg(componentName + " result ended [" + thisTime + "/" + targetTime + "]");
							break;
						}
					}
				} catch (IOException ioe_br) {
					throw new Exception ("Error while reading " + filename + " : " + ioe_br);
				} catch (NumberFormatException nfe) {
					throw new Exception ("Value parsing exception while reading " + filename + ": " + nfe);
				} catch (Exception exc) {
					throw new Exception ("Unexpected exception while reading " + filename + " : " + exc);
				}
				
				// Adds the component name and the value pair
				result.addComponentAnalysis(componentName, totalValue, maxTime, maxValue, count-1, success);
			}
		} catch (IOException ioe) {
			System.out.println("Error while iterating through the .csv files: " + ioe);
		}
		
		return results;
	}
	
	
	/**
	 * Returns Event with the given AnalysisType
	 * 
	 * @param events		Set of Events
	 * @param analysisType	Analysis type
	 * @return				Event with the given AnalysisType, null if not existing
	 */
	protected Event getEvent (Set<Event> events, String analysisType) {
		for(Event e : events) {
			if(e.hasParameter("AnalysisType")) {
				if(((String) e.getParameter("AnalysisType")).equals(analysisType)) {
					return e;
				} 
			}
		}
		
		return null;
	}
	
	
	/**
	 * Gets the extension of a file
	 * 
	 * @param file			Path to the file
	 * @return				Extension
	 */
	protected String getExtension (Path file) {
		String filename	= file.getFileName().toString();
		String[] tokens = filename.split("\\.");
		
		return tokens[tokens.length - 1];
	}
	
	/**
	 * Gets the component name from the XTEAM simulation result file
	 * 
	 * @param file			Path to the file
	 * @return				Component name
	 */
	protected String getComponentName (Path file) throws Exception {
		String 		filename		= file.getFileName().toString();
		String[] 	tokens 			= filename.split("\\.");
		String[] 	filenameTokens 	= tokens[0].split("_");
		String 		componentName	= null;
		
		// If the file name is too short and does NOT contain the component name
		if(filenameTokens.length >= 2) {
			// Concatenates the component name with the relative ID of the component
			for(int i=0; i < filenameTokens.length-2; i++) {
				if(i != 0) {
					componentName += "_";
				}
				componentName += filenameTokens[i];
			}
			componentName = filenameTokens[0] + "_" + filenameTokens[1];
		} else {
			throw new Exception ("missing the component name");
		}

		return componentName;
	}
	
	/**
	 * Gets the analysis type from the XTEAM simulation result file
	 * 
	 * @param file			Path to the file
	 * @return				Analysis type
	 * @throws Exception	Cannot get the analysis type
	 */
	protected String getAnalysisType (Path file) throws Exception{
		String 		analysisType 	= new String();
		
		String 		filename		= file.getFileName().toString();
		String[] 	tokens 			= filename.split("\\.");
		String[] 	filenameTokens 	= tokens[0].split("_");
		
		// Gets the analysis type
		// Takes the second last token as the analysis type name
		if(filenameTokens.length >= 3) {
			analysisType += filenameTokens[filenameTokens.length-2];
		} else {
			throw new Exception ("missing the analysis type");
		}
		
		return analysisType;
	}
	
	/**
	 * Print screen messages
	 * 
	 * @param msg			Message to print to screen
	 */
	protected void printMsg(String msg) {
		sl.printMsg("XTEAMSimulation", msg);
	}
}
