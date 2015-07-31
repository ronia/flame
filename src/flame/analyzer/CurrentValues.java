package flame.analyzer;

import java.util.ArrayList;
import java.util.List;

public class CurrentValues {

	private List<Value>		values		= new ArrayList<>();
	private String			timestamp;
	
	/**
	 * Default creator
	 * 
	 * @param lines			List of value lines read from the default values file
	 */
	public CurrentValues (String timestamp, List<String> lines) throws Exception {
		// iterates through the list and make them into Values
		for(String line : lines) {
			
			// parses the line and creates a new Value instance
			Value value;
			try {
				value = new Value (line);
			} catch (Exception e) {
				printMsg("Error while parsing default values: " + e.getMessage());
				continue;
			}
			
			// adds the Value to the Values list
			values.add(value);
		}
		
		// checks for duplicates 
		for(int i=0; i < values.size(); i++) {
			for(int j=0; j < values.size(); j++) {
				// skips iteself
				if(i != j) {
					// two values to compare
					Value v1 = values.get(i);
					Value v2 = values.get(j);
					
					// if there is a duplicate
					if(v1.sameAttribute(v2)) {
						throw new Exception ("Duplicate default Value found: " + v1.explainValue());
					}
				}
			}
		}
		
		this.timestamp = timestamp;
	}
	
	/**
	 * Updates a Value and returns corresponding message
	 * 
	 * @param objectID		Object ID of the object to update
	 * @param attrName		Name of the attribute to update
	 * @param attrValue		New attribute value
	 * @return				String message to output
	 */
	public List<ValueChange> updateValue (String objectID, String attrName, String attrValue) {
		List<ValueChange> list = new ArrayList<>();
		
		// iterates through the Values
		for(Value value : values) {
			
			// checks if the same object && same attribute
			if(value.getObjectID().equals(objectID) && value.getAttributeName().equals(attrName)) {
				// parses the values
				double currentValue;
				double newValue;
				
				try {
					currentValue = Double.parseDouble(value.getValue());
				} catch (NumberFormatException nfe) {
					continue;
				}
				
				try {
					newValue = Double.parseDouble(attrValue);
				} catch (NumberFormatException nfe) {
					continue;
				}
				
				// checks if the value has changed
				if(currentValue != newValue) {
					ValueChange vc = new ValueChange();
					vc.setAttributeName(attrName);
					vc.setParentObjectName(value.getParentName());
					vc.setObjectName(value.getObjectName());
					vc.setCurrentValue(currentValue);
					vc.setNewValue(newValue);
					list.add(vc);
					
					// updates the current value
					value.setValue(attrValue);
				}
			}
		}
		
		return list;
	}
	
	private void printMsg(String str) {
		System.out.println("[" + timestamp + "/CurrentValues]: " + str);
	}
}
