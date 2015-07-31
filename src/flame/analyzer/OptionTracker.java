package flame.analyzer;

import java.util.ArrayList;
import java.util.List;

public class OptionTracker {
	
//////////////////////////////////////////////////////////////////////////
//
// Member variables
//
//////////////////////////////////////////////////////////////////////////
	
	/**
	 * Timestamp of the data
	 */
	private String			timestamp;
	
	private Tasks			tasks 		= new Tasks();
	
//////////////////////////////////////////////////////////////////////////
//
// Creators
//
//////////////////////////////////////////////////////////////////////////
	
	/**
	 * Default creator
	 * 
	 * @param readOptions	List of lines read from the Options file
	 */
	public OptionTracker (String timestamp, List<String> lines) throws Exception {
		
		List<Option> options = new ArrayList<>();
		
		// iterates through the list and make them into Values
		for(String line : lines) {
			
			// parses the line and creates a new Value instance
			Option option;
			try {
				option = new Option (line);
			} catch (Exception e) {
				printMsg("Error while parsing options: " + e.getMessage());
				continue;
			}
			
			// adds the Value to the Values list
			options.add(option);
		}
		
		// checks for duplicate options
		for(int i=0; i < options.size(); i++) {
			for(int j=0; j < options.size(); j++) {
				// skips itself
				if(i != j) {
					// two options to compare
					Option o1 = options.get(i);
					Option o2 = options.get(j);
					
					// if there is a duplicate
					if(o1.isDuplicate(o2)) {
						throw new Exception ("Duplicate Option found: " + o1.explainOption());
					}
				}
			}
		}
		
		// stores the options in the data structure
		for(Option option : options) {
			tasks.addOption(option);
		}
		
		this.timestamp = timestamp;
	}
	
//////////////////////////////////////////////////////////////////////////
//
// Member methods
//
//////////////////////////////////////////////////////////////////////////
	
	/**
	 * Updates an Option and returns the corresponding message
	 * 
	 * @param objectID		Object to update
	 * @param attrName		Name of the attribute to update
	 * @param attrValue		New attribute value
	 * @return				String message to output
	 */
	public List<OptionChange> updateOption(String objectID, String attrName, String attrValue) {
		return tasks.updateOption(objectID, attrName, attrValue);
	}
	
	private void printMsg(String str) {
		System.out.println("[" + timestamp + "/OptionTracker]: " + str);
	}
}
