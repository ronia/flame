package flame.analyzer;

public class Value {
	private String 			objectID;
	private String		 	parentName;
	private String 			objectName;
	private String 			attributeName;
	private String 			value;
	
	private final int		indexObjectID		= 0;
	private final int		indexParentName		= 1;
	private final int		indexObjectName		= 2;
	private final int		indexAttributeName	= 3;
	private final int		indexValue			= 4;
	
	/**
	 * Default creator
	 * 
	 * @param objectID
	 * @param parentName
	 * @param objectName
	 * @param attributeName
	 * @param value
	 */
	public Value (							String objectID,
											String parentName,
											String objectName,
											String attributeName,
											String value ) {
		this.objectID 		= objectID.trim();
		this.objectName		= objectName.trim();
		this.parentName		= parentName.trim();
		this.attributeName	= attributeName.trim();
		this.value			= value.trim();
	}
	
	/**
	 * Creator that parses a line
	 * 
	 * @param line
	 * @throws Exception
	 */
	public Value (String line) throws Exception {
		String[] tokens = line.split(",");
		
		// checks the length
		if(tokens.length < 5) {
			throw new Exception ("Too few elements: " + line);
		}
		
		// assigns each token to variables
		objectID 		= tokens[indexObjectID].trim();
		objectName		= tokens[indexObjectName].trim();
		parentName		= tokens[indexParentName].trim();
		attributeName	= tokens[indexAttributeName].trim();
		value			= tokens[indexValue].trim();
	}
	
	public String 	getObjectID () 				{ return objectID; }
	public String 	getParentName ()			{ return parentName; }
	public String 	getObjectName () 			{ return objectName; }
	public String 	getAttributeName ()			{ return attributeName; }
	public String 	getValue()					{ return value; }
	public void		setValue(String value)		{ this.value = value; }
	
	/**
	 * Returns a string that explains this value
	 * @return				Explanation of this Value
	 */
	public String explainValue()		{
		String ret = new String();
		
		ret += 	attributeName +
				" of " +
				parentName + "/" +
				objectName +
				" to " +
				value;
		
		return ret;
	}
	
	/**
	 * Checks if it is a completely same Value
	 * 
	 * @param v				Target Value
	 * @return				True/False
	 */
	public boolean equals (Value v) {
		if(!objectID.equals(v.getObjectID()))	 		{ return false; }
		if(!parentName.equals(v.getParentName()))		{ return false; }
		if(!objectName.equals(v.getObjectName())) 		{ return false; }
		if(!attributeName.equals(v.getAttributeName())) { return false; }
		if(!value.equals(v.getValue()))					{ return false; }
		
		return true;
	}
	
	/**
	 * Checks if it is the same object's Value
	 * @param v				Target Value
	 * @return				True/False
	 */
	public boolean sameObject (Value v) {
		if(!objectID.equals(v.getObjectID()))	 		{ return false; }	
		
		return true;
	}
	
	public boolean sameAttribute (Value v) {
		if(!objectID.equals(v.getObjectID()))	 		{ return false; }	
		if(!attributeName.equals(v.getAttributeName())) { return false; }
		
		return true;
	}
}
