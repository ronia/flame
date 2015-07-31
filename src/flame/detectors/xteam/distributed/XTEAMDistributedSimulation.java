package flame.detectors.xteam.distributed;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

import flame.Constants;
import flame.ScreenLogger;
import flame.Utility;
import flame.detectors.xteam.Result;
import flame.detectors.xteam.Results;
import flame.detectors.xteam.XTEAMEngine;
import Prism.core.Event;

public class XTEAMDistributedSimulation {

///////////////////////////////////////////////
// Member Variables
///////////////////////////////////////////////	
	
	/**
	 * XTEAM Engine username. For LocalV engines, this is the corresponding architect's name.
	 */
	protected		String						username;
	
	/**
	 * Path to where the XTEAM simulation is
	 */
	protected		Path						simulationPath;

	/**
	 * Path to where the XTEAM simulation code is
	 */
	protected		Path						simulationCodePath;
	
	/**
	 * Path to where the XTEAM simulation result is
	 */
	protected		Path 						simulationResultPath;
	
	/**
	 * Path to the file that contains what types of analyses this Engine performs
	 */
	private			Path						xteamInfo;
	
	/**
	 * Mapping from analysis name (e.g. Energy, Latency, ...) to the target value index in the simulation result file (e.g. 1, 2, ...)
	 */
	private			Map<String, Integer>		xteamAnalysisTargetValueIndex 	= new HashMap<>();
		
	/**
	 * Name of the errors list file
	 */
	protected		Path						errorsFilename;
	
	/**
	 * Event ID of the simulation notification
	 */
	protected		int							simulationEventID;
	
	/**
	 * Target time (logical time) to which the simulation analysis runs  
	 */
	private			double						targetTime;
	
	/**
	 * Time at which this simulation began
	 */
	protected		String						arrival_time;
	
	/**
	 * XTEAM Engine instance
	 */
	protected		XTEAMEngine					engine;
	
	/**
	 * EventReceiver instance in XTEAMEngine
	 */
	protected		XTEAMEngine.EventReceiver	receiver;
	
	/**
	 * ScreenLogger instance
	 */
	protected		ScreenLogger				screenLogger;
	
///////////////////////////////////////////////
// Constructors
///////////////////////////////////////////////
	
	public XTEAMDistributedSimulation (	String						username,
										Path 						simulationPath,
										Path						simulationResultPath,
										Path						errorsFileName,
										double						targetTime,
										Path						xteamInfo,
										ScreenLogger 				screenLogger,
										XTEAMEngine					engine,
										XTEAMEngine.EventReceiver	receiver) throws Exception {
		this.username				= username;
		this.simulationPath			= simulationPath;
		this.simulationCodePath		= simulationPath.resolve("XTEAM_Simulation/simulation_code");
		this.simulationResultPath	= simulationPath.resolve(simulationResultPath);
		this.errorsFilename			= errorsFileName;
		this.targetTime				= targetTime;		
		this.xteamInfo				= xteamInfo;
		this.screenLogger			= screenLogger;
		this.engine					= engine;
		this.receiver				= receiver;
		arrival_time				= Utility.convertDate(System.currentTimeMillis());
		
		readXTEAMInfo();
	}

///////////////////////////////////////////////
// Data Access Member Methods
///////////////////////////////////////////////	

	public Path getSimulationCodePath() {
		return simulationCodePath;
	}

	public Path getSimulationPath() {
		return simulationPath;
	}
	
	public Path getSimulationResultPath() {
		return simulationResultPath;
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
	 * Checks for syntax errors
	 * 
	 * @return		Number of syntax errors
	 */
	public int checkSyntaxErrors () {
		// list of events to send back to architects
		List<Event> events = new ArrayList<>();
		
		// reads syntax error list and sends the resulting Events
		try {
			events.addAll(readSyntaxErrorList());
		} catch (Exception e) {
			printMsg("Error while reading the syntax errors list: " + e);
		}
		
		// creates and sends the syntax error events
		for(Event newEvent : events) {
			newEvent.addParameter("SenderUsername", 	username);
			newEvent.addParameter("OriginComponent", 	"XTEAM Engine");
			newEvent.addParameter("ArrivalTime", 		arrival_time);
			
			engine.sendRequest(newEvent);
		}
		
		return events.size();
	}
	
	/**
	 * Sends a simulation beginning notification event
	 */
	public void sendSimulationBeginningNotification() {
		// increases simulation event ID		
		simulationEventID = receiver.tickEventID();
		
		// sends out simulation execution beginning notification
		Event simExecBeginning = new Event("Notification");
		simExecBeginning.addParameter("SenderUsername", 	username);
		simExecBeginning.addParameter("OriginComponent", 	"XTEAM Engine");
		simExecBeginning.addParameter("Value", 				"Simulation execution beginning");
		simExecBeginning.addParameter("EventID", 			new Integer(simulationEventID));
		simExecBeginning.addParameter("ArrivalTime", 		arrival_time);
		engine.sendRequest(simExecBeginning);
	}
	
	/**
	 * Sends a simulation completion notification event
	 */
	public void sendSimulationCompletionNotification() {
		// sends out simulation execution completion notification
		Event simExecCompletion = new Event("Notification");
		simExecCompletion.addParameter("SenderUsername", 	username);
		simExecCompletion.addParameter("OriginComponent", 	"XTEAM Engine");
		simExecCompletion.addParameter("Value", 			"Simulation execution completion");
		simExecCompletion.addParameter("EventID", 			new Integer(simulationEventID));
		simExecCompletion.addParameter("ArrivalTime", 		arrival_time);
		engine.sendRequest(simExecCompletion);
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
	 * Analyzes the simulation result and sends the events
	 */
	public void analyze() throws Exception {
		// analyzes simulation result
		List<Event> events = analyzeSimulationResult();
		
		// sends XTEAM simulation result
		for(Event event : events) {
			event.addParameter("SenderUsername", 	username);
			event.addParameter("OriginComponent", 	"XTEAM Engine");
			event.addParameter("ArrivalTime", 		arrival_time);
			
			engine.sendRequest(event);
		}
	}
	
	/**
	 * Analyzes the simulation result
	 * 
	 * @return				Events to send
	 */
	protected List<Event> analyzeSimulationResult() throws Exception {
		// list of Events to return
		List<Event> events = new ArrayList<>();
				
		// reads analysis warning messages
		Map<String, List<String>> analysis_warnings = readAnalysisWarnings();
		
		// reads syntax errors list
		List<String> errors = Files.readAllLines(simulationResultPath.resolve(errorsFilename), Constants.charset);
		
		// If there is any syntax error from the simulation project compilation
		if(errors.size() > 0) {
			// Creates an XTEAM Event with the syntax errors
			Event e = new Event ("XTEAM");
			e.addParameter("SyntacticConflicts", errors);
			e.addParameter("AnalysisWarnings", analysis_warnings);
			events.add(e);
			return events;
		} 
		
		/*
		 *  checks if the simulation completion tag file has been created, if the files does 
		 *  not exist, that means the simulation was disrupted in the middle.
		 */ 
		if(Files.notExists(simulationResultPath.resolve("Simulation_Completion.txt"), new LinkOption[]{LinkOption.NOFOLLOW_LINKS})) {
			// creates an XTEAM Event with the simulation incompletion error
			ArrayList<String> sim_incompletion_error = new ArrayList<>();
			sim_incompletion_error.add(new String("Simulation execution was disrupted for an unhandled exception. Manual simulation code inspection recommended."));
			Event e = new Event ("XTEAM");
			e.addParameter("SyntacticConflicts", sim_incompletion_error);
			events.add(e);
			return events;
		}

		// analyzes the results
		printMsg("Analyzing the sim output for [" + arrival_time + "] ...");
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
				if(!flame.FileUtility.getExtension(file).toLowerCase().equals("list")) {
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
				if(!flame.FileUtility.getExtension(file).toLowerCase().equals("csv")) {
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
		screenLogger.printMsg("DistSim", msg);
	}
}
