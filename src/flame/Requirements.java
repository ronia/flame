package flame;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Requirements class manages the system requirements to be satisfied by the model
 * 
 * @author 					<a href=mailto:jaeyounb@usc.edu>Jae young Bang</a>
 * @version					2014.03
 */
public class Requirements {
	
///////////////////////////////////////////////////////////
// Requirement class
///////////////////////////////////////////////////////////	
	
	/**
	 * Requirement represent a system requirement that the model should satisfy. <p>
	 * 
	 * It stores the target value type (e.g. Total, Maximum, Average), the threshold 
	 * value, and the direction. For example, if the requirement is "Total", "<", and "700", 
	 * that means it is a requirement that is regarding the total summation of a certain 
	 * value should be less than 700.
	 */
	protected class	Requirement {
		/**
		 * Target value (e.g. Total, Maximum, Average)
		 */
		protected String valueName;
		
		/**
		 * Threshold direction (e.g. >, <, or =)
		 */
		protected String direction;
		
		/**
		 * The threshold value 
		 */
		protected double threshold;
		
		
		/**
		 * Default creator
		 * 
		 * @param value			Target value name (e.g. Total, Average, Maximum)
		 * @param direction		Requirement direction (e.g. >, <, =)
		 * @param threshold		Requirement threshold
		 */
		public Requirement (String valueName, String direction, double threshold) {
			this.valueName 	= valueName;
			this.direction	= direction;
			this.threshold 	= threshold;
		}
		
		public String getValueName() 	{ return valueName; }
		public String getDirection() 	{ return direction; }
		public double getThreshold()	{ return threshold; }
	}
	
///////////////////////////////////////////////////////////
// Member variables
///////////////////////////////////////////////////////////	
	
	/**
	 * Mapping from analysis type (e.g. Energy, Latency, ...) to the target value (e.g. Total, Maximum, Average)
	 */
	protected		Map<String, List<Requirement>>	xteamRequirements 	= new HashMap<>();
	
	/**
	 * This variable indicates the level of verbosity of this class. 0 is 
	 * no screen output, 1 is little screen output, and 2 is full screen output.
	 */
	protected		int								verbosity			= 0;
	
	/**
	 * The ScreenLogger to print messages to screen
	 */
	protected 		ScreenLogger					sl;
	
///////////////////////////////////////////////////////////
// Member methods
///////////////////////////////////////////////////////////		
	
	/**
	 * Returns the list of analysis types (e.g. Energy, Latency, Memory)
	 * that the system requirements require to verify if those requirements
	 * have been met.
	 * 
	 * @return 				Set of analysis types
	 */
	public Set<String> getAnalysisTypes() {
		return xteamRequirements.keySet();
	}
	
	/**
	 * Checks if there exists a matching analysis type in the list of analysis 
	 * types (e.g. Energy, Latency, Memory) that the system requirements require 
	 * to verify if those requirements have been met.
	 * 
	 * @param type			Analysis type to check for
	 * @return				True/false answer
	 */
	public boolean hasAnalysisType(String type) {
		for(String key : getAnalysisTypes()) {
			if(key.toLowerCase().equals(type.toLowerCase())) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Finds the corresponding requirements regarding the given analysis type 
	 * (e.g. Energy, Latency, ...)
	 * 
	 * @param type			Analysis type to look for
	 * @return				List of requirements that match the analysis type
	 * @throws Exception	No requirement matches the given analysis type
	 */
	protected List<Requirement> getRequirements (String type) throws Exception {
		List<Requirement> reqs = null;
		
		// tries with the camel case first
		if(reqs == null) {
			reqs = xteamRequirements.get(Utility.toCamelCase(type));
		}
		
		// tries with the lower case second
		if(reqs == null) {
			reqs = xteamRequirements.get(type.toLowerCase());
		}
		
		// if it failed to find the requirements
		if(reqs == null) {
			throw new Exception ("No requirment that match the analysis type [" + type + "] exists");
		}
		
		return reqs;
	}
	
	/**
	 * Returns the first (in order) target value name (e.g. Total, Maximum, Average) 
	 * for the given analysis type
	 * 
	 * @param type			Analysis type to look for
	 * @return				First target value name
	 * @exception			No requirement matches the given analysis type
	 */
	public String getFirstTargetValueName(String type) throws Exception {
		return getRequirements(type).get(0).getValueName();
	}
	
	/**
	 * Returns the first (in order) target value direction (e.g. <, >, =) 
	 * for the given analysis type
	 * 
	 * @param type			Analysis type to look for
	 * @return				First target value direction
	 * @exception			No requirement matches the given analysis type
	 */
	public String getFirstTargetDirection(String type) throws Exception {
		return getRequirements(type).get(0).getDirection();
	}
	
	/**
	 * Returns the first (in order) target threshold for the given analysis type
	 * 
	 * @param type			Analysis type to look for
	 * @return				First target threshold
	 * @exception			No requirement matches the given analysis type
	 */
	public double getFirstTargetThreshold(String type) throws Exception {
		return getRequirements(type).get(0).getThreshold();
	}
	
	/**
	 * Returns the list of target value names (e.g. Maximum, Average, etc.) 
	 * for the given analysis type
	 * 
	 * @param type			Analysis type to look for
	 * @return				List of target value names
	 * @exception			No requirement matches the given analysis type
	 */
	public List<String> getTargetValueNames(String type) throws Exception {
		List<String> targetValueNames = new ArrayList<>();
		
		for (Requirement requirement : getRequirements(type)) {
			targetValueNames.add(requirement.getValueName());
		}
		
		return targetValueNames;
	}
	
	/**
	 * Default creator <p>
	 * 
	 * The level of verbosity. 0 is no screen output, 1 is little screen 
	 * output, and 2 is full screen output.
	 * 
	 * @param verbosity		0 is no screen output, 1 is little screen output, and 2 is full screen output.
	 * @param screenLogger	ScreenLogger
	 * 
	 */
	public Requirements (int verbosity, ScreenLogger screenLogger) throws Exception {
		this.verbosity 	= verbosity;
		sl 				= screenLogger;
		
		readXTEAMRequirements();
	}
	
	/**
	 * Checks if a requirement has been violated
	 * 
	 * @param analysisType			Analysis type (e.g. Energy, Latency, etc.)
	 * @param targetValueName		Target value name (e.g. Total, Average, etc.)
	 * @param value					Simulated value
	 * @return						Yes/no answer
	 * @exception
	 */
	public boolean isSatisfied(String analysisType, String targetValueName, double value) throws Exception {
		List<Requirement> requirements = getRequirements(analysisType);
		
		boolean debug = false;
		
		if(verbosity == 2) {
			debug = true;
		}
		
		// finds the right target value name
		for(Requirement requirement : requirements) {
			
			// if the target value name matches
			if(requirement.getValueName().toLowerCase().equals(targetValueName.toLowerCase())) {
				// checks whether the value satisfies the requirement
				switch (requirement.getDirection()) {
					case "<":
						if (value < requirement.getThreshold()) {
							if (debug) printMsg("GOOD:" + analysisType + " value [" + String.format("%.2f", value) + "] < threshold [" + requirement.getThreshold() + "]");
							return true;
						} else {
							if (debug) printMsg("BAD:" + analysisType + " value [" + String.format("%.2f", value) + "] < threshold [" + requirement.getThreshold() + "]");
						}
						break;
						
					case ">":
						if (value > requirement.getThreshold()) {
							if (debug) printMsg("GOOD:" + analysisType + " value [" + String.format("%.2f", value) + "] > threshold [" + requirement.getThreshold() + "]");
							return true;
						} else {
							if (debug) printMsg("BAD:" + analysisType + " value [" + String.format("%.2f", value) + "] > threshold [" + requirement.getThreshold() + "]");
						}
						break;
						
					case "=":
						if (value == requirement.getThreshold()) {
							if (debug) printMsg("GOOD:" + analysisType + " value [" + String.format("%.2f", value) + "] == threshold [" + requirement.getThreshold() + "]");
							return true;
						} else {
							if (debug) printMsg("BAD:" + analysisType + " value [" + String.format("%.2f", value) + "] == threshold [" + requirement.getThreshold() + "]");
						}
						break;
						
					default:
						printMsg("Unknown requirement direction found: " + requirement.getDirection());
				}
			}
		}
		
		
		return false;
	}
	
	/**
	 * Reads the requirements from the file
	 * 
	 * @throws Exception	
	 */
	private void readXTEAMRequirements() throws Exception {
		// reads in the configuration file to get the requirements file name
		Properties props = new Properties();
		try {
			props.load(new FileInputStream(new File("config.properties")));
		} catch (FileNotFoundException e) {
			throw new Exception ("Unable to locate config.properties");
		} catch (IOException e) {
			throw new Exception ("Unable to read config.properties");
		}
		
		// gets the analysis info file name
		String filename = props.getProperty("XTEAM_INFO");
		if(filename == null) {
			throw new Exception("Unable to locate the XTEAM_INFO attribute in config.properties");
		}

		try (	FileReader fr 		= new FileReader(filename);
				BufferedReader br 	= new BufferedReader(fr)) {
			
			// the "cursor" line
			String line;
			
			while ((line = br.readLine()) != null) {
				
				// tokenizes the line
				String[] tokens = line.split(",");
				if(tokens.length < 3) {
					throw new Exception ("Requirement file " + filename + " has an unparsable line: " + line);
				}
				
				// reads the analysis type
				String analysisType = Utility.toCamelCase(tokens[0].trim());
				
				// checks if it is "All"
				if(analysisType.toLowerCase().equals("all")) {
					printMsg("Error: Analysis name cannot be " + analysisType);
					continue;
				}
				
				// reads the value types
				String[] 			predefinedValueNames 	= {"total", "maximum", "average", "success"};
				List<Requirement>	valueNames				= new ArrayList<>();
								
				for(int i=2; i < tokens.length; i++) {
					// gets the target value name and threshold
					String[] 	valueNameAndThreshold 		= tokens[i].trim().toLowerCase().split("<|>|=");
									
					// should only have two elements: target value name and threshold
					if(valueNameAndThreshold.length != 2) {
						throw new Exception ("XTEAM_INFO file has an ill-formed requirement: " + line);
					}
					
					String valueName = valueNameAndThreshold[0];
					Double threshold = (double) 0;
					try {
						threshold = Double.parseDouble(valueNameAndThreshold[1]);
					} catch (NumberFormatException nfe) {
						throw new Exception ("XTEAM_INFO file has an ill-formed threshold: " + line);
					}
					
					// checks if the value type is a predefined one
					boolean unknown = true;
					for(String type : predefinedValueNames) {
						if(valueName.equals(type)) {
							unknown = false;
							break;
						}
					}
					if(unknown) {
						throw new Exception ("XTEAM_INFO file has an unknown value name: " + line);
					}
					
					// gets the direction operator (one of <, >, or =)
					String		direction				= "";
					String[] 	directionTokens 		= tokens[i].trim().toLowerCase().split("[^<|>|=]");
					for(String token : directionTokens) {
						if(token.equals("<") || token.equals(">") || token.equals("=")) {
							direction = token;
						}
					}
					
					if(direction == "") {
						throw new Exception ("XTEAM_INFO file has an unknown direction operator: " + line);
					}
	
					// adds to the value types
					valueNames.add(new Requirement(Utility.toCamelCase(valueName), direction, threshold));
					
					// prints a message to the screen about the new type
					String directionMsg;
					switch(direction) {
						case "<": 
							directionMsg = "less than";
							break;
						case ">":
							directionMsg = "greater than";
							break;
						case "=":
							directionMsg = "equal to";
							break;
						default:
							throw new Exception ("XTEAM_INFO file has an unknown direction operator: " + line);
					}
					
					if(verbosity >= 1) {
						printMsg(Utility.toCamelCase(valueName) + " of " + analysisType + " must be " + directionMsg + " " + threshold);
					}
				}
				
				xteamRequirements.put(analysisType, valueNames);
			}
		} catch (Exception e) {
			printMsg ("Reading file " + filename + " failed: " + e);
		}
	}
	
	/**
	 * Print screen messages
	 * 
	 * @param msg			Message to print to screen
	 */
	protected void printMsg(String msg) {
		sl.printMsg("Requirements", msg);
	}
}
