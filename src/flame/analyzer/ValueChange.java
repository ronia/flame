package flame.analyzer;

/**
 * ValueChange carries the details of a change of a model element's value
 * 
 * @author 					<a href="mailto:jaeyounb@usc.edu">Jae young Bang</a>
 * @version					2014.06
 */
public class ValueChange {
	private String	attributeName;
	private String	parentObjectName;
	private String	objectName;
	private double	currentValue;
	private double	newValue;
	
	/**
	 * @return the attributeName
	 */
	public String getAttributeName() {
		return attributeName;
	}
	
	/**
	 * @param attributeName the attributeName to set
	 */
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}
	
	/**
	 * @return the parentObjectName
	 */
	public String getParentObjectName() {
		return parentObjectName;
	}
	
	/**
	 * @param parentObjectName the parentObjectName to set
	 */
	public void setParentObjectName(String parentObjectName) {
		this.parentObjectName = parentObjectName;
	}
	
	/**
	 * @return the objectName
	 */
	public String getObjectName() {
		return objectName;
	}
	
	/**
	 * @param objectName the objectName to set
	 */
	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}

	/**
	 * @return the newValue
	 */
	public double getNewValue() {
		return newValue;
	}

	/**
	 * @param newValue the newValue to set
	 */
	public void setNewValue(double newValue) {
		this.newValue = newValue;
	}

	/**
	 * @return the currentValue
	 */
	public double getCurrentValue() {
		return currentValue;
	}

	/**
	 * @param currentValue the currentValue to set
	 */
	public void setCurrentValue(double currentValue) {
		this.currentValue = currentValue;
	}
	
	
}
