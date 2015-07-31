package flame.analyzer;

import java.util.Map;
import java.util.TreeMap;

public class Option {
	private int						taskNumber;
	private int						participantNumber;
	private int						objectNumber;
	
	private String					objectID;
	private String					parentName;
	private String					objectName;
	private String					attrName;
	
	/**
	 * Flags indicating whether a particular option for this attribute has been chosen
	 */
	private Map<Integer, Boolean>	flags						= new TreeMap<>();
	private Map<Integer, Value>		values 						= new TreeMap<>();
	
	public	int						getTaskNumber () 			{ return taskNumber; }
	public	int						getParticipantNumber () 	{ return participantNumber; }
	public 	int						getObjectNumber ()			{ return objectNumber; }
	
	public	String					getObjectID ()				{ return objectID; }
	public 	String					getParentName ()			{ return parentName; }
	public	String					getObjectName ()			{ return objectName; }
	public	String					getAttrName ()				{ return attrName; }
	
	public	String					explainOption() {
		String ret = new String();
		
		ret +=	"[" + taskNumber + "/" +
				participantNumber + "/" +
				objectNumber + "] " +
				attrName + " of " +
				parentName + "/" + objectName;
				
		return ret;
	}
	
	private final int 				indexTaskNumber 			= 0;
	private final int				indexParticipantNumber		= 1; 
	private final int 				indexObjectNumber 			= 2;
	private final int 				indexObjectID 				= 3;
	private final int 				indexParentName 			= 4;
	private final int 				indexObjectName				= 5;
	private final int 				indexAttributeName 			= 6;
	private final int 				indexValues 				= 7;
	
	/**
	 * Default creator
	 * 
	 * @param line			A line of string from exp_option.csv
	 */
	public Option (String line) throws Exception {
		parseLine(line);
	}
	
	private void parseLine (String line) throws Exception {
		// splits the line
		String tokens[] = line.split(",");
		for (int i=0; i < tokens.length; i++) {
			tokens[i] = tokens[i].trim();
		}
		
		// checks the length
		if(tokens.length != 8) {
			throw new Exception ("Incorrect number of elements: " + line);
		}
		
		// gets the task number
		try {
			taskNumber = Integer.parseInt(tokens[indexTaskNumber]);
		} catch (NumberFormatException nfe) {
			throw new Exception ("Task number cannot be parsed: " + line);
		}
		
		// gets the participant number
		try {
			participantNumber = Integer.parseInt(tokens[indexParticipantNumber]);
		} catch (NumberFormatException nfe) {
			throw new Exception ("Participant number cannot be parsed: " + line);
		}
		
		// gets the object number
		try {
			objectNumber = Integer.parseInt(tokens[indexObjectNumber]);
		} catch (NumberFormatException nfe) {
			throw new Exception ("Object number cannot be parsed: " + line);
		}
		
		// splits the values
		String valueTokens[] = tokens[indexValues].split("/");
		
		// checks the length
		if(valueTokens.length < 1) {
			throw new Exception ("Too few values: " + line);
		}
		
		// assigns objectID, parentName, objectName, and attributeName
		objectID 		= tokens[indexObjectID];
		parentName		= tokens[indexParentName];
		objectName		= tokens[indexObjectName];
		attrName		= tokens[indexAttributeName];
		
		// iterates through the list of values
		int counter = 1;
		for (String valueToken : valueTokens) {
			flags.put(	new Integer(counter), 
						new Boolean(false));
			values.put(	new Integer(counter), 
						new Value(	objectID, 
									parentName, 
									objectName, 
									attrName, 
									valueToken.trim()));
			
			counter++;
		}
	}
	
	/**
	 * Updates the option value
	 * 
	 * @param objectID		Object ID
	 * @param attrName		Attribute name
	 * @param attrValue		New attribute value
	 * @return				The current option flags
	 */
	public Map<Integer, Boolean> updateOption (String objectID, String attrName, String attrValue) {
		// only if this is the Option to be updated
		if(this.objectID.equals(objectID) && this.attrName.equals(attrName)) {
			
			// iterates through the possible values for this attribute
			for(int i : values.keySet()) {
				
				// parses the values
				double currentValue;
				double newValue;
				
				try {
					currentValue = Double.parseDouble(values.get(i).getValue());
				} catch (NumberFormatException nfe) {
					continue;
				}
				
				try {
					newValue = Double.parseDouble(attrValue);
				} catch (NumberFormatException nfe) {
					continue;
				}
				
				if(currentValue == newValue) {
					// if it is a match, up the flag!
					flags.remove(i);
					flags.put(new Integer(i), new Boolean(true));
				} else {
					// if it is NOT a math, lower the flag!
					flags.remove(i);
					flags.put(new Integer(i), new Boolean(false));
				}
			}
		}
		
		return flags;
	}
	
	/**
	 * Checks if the given Option is a dupliate of itself
	 * @param o				Option to compare with
	 * @return				True/False answer
	 */
	public boolean isDuplicate(Option o) {
		if(		taskNumber 				== o.getTaskNumber() 			&&
				participantNumber 		== o.getParticipantNumber() 	&&
				objectNumber			== o.getObjectNumber()			&&
				
				objectID.equals(o.getObjectID()) &&
				parentName.equals(o.getParentName()) &&
				objectName.equals(o.getObjectName()) &&
				attrName.equals(o.getAttrName())) {
			return true;
		} else {
			return false;
		}
	}
}
