package flame.analyzer;

/**
 * OptionChange carries the details of a change in the given model change options
 * 
 * @author 					<a href="mailto:jaeyounb@usc.edu">Jae young Bang</a>
 * @version					2014.06
 */
public class OptionChange {
	private int 	taskNumber;
	private int		participantNumber;
	private int		objectNumber;
	private int		currentOption;
	private int		newOption;
	
	/**
	 * @return the taskNumber
	 */
	public int getTaskNumber() {
		return taskNumber;
	}
	
	/**
	 * @param taskNumber the taskNumber to set
	 */
	public void setTaskNumber(int taskNumber) {
		this.taskNumber = taskNumber;
	}

	/**
	 * @return the participantNumber
	 */
	public int getParticipantNumber() {
		return participantNumber;
	}

	/**
	 * @param participantNumber the participantNumber to set
	 */
	public void setParticipantNumber(int participantNumber) {
		this.participantNumber = participantNumber;
	}

	/**
	 * @return the objectNumber
	 */
	public int getObjectNumber() {
		return objectNumber;
	}

	/**
	 * @param objectNumber the objectNumber to set
	 */
	public void setObjectNumber(int objectNumber) {
		this.objectNumber = objectNumber;
	}

	/**
	 * @return the currentOption
	 */
	public int getCurrentOption() {
		return currentOption;
	}

	/**
	 * @param currentOption the currentOption to set
	 */
	public void setCurrentOption(int currentOption) {
		this.currentOption = currentOption;
	}

	/**
	 * @return the newOption
	 */
	public int getNewOption() {
		return newOption;
	}

	/**
	 * @param newOption the newOption to set
	 */
	public void setNewOption(int newOption) {
		this.newOption = newOption;
	}
	
	
}
