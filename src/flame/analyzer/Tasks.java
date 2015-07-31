package flame.analyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Tasks {
	
//////////////////////////////////////////////////////////////////////////
//
// Member variables
//
//////////////////////////////////////////////////////////////////////////	
	
	/**
	 * List of Tasks. Tasks have Participants that have DesignObjects that have Options
	 */
	private List<Task> tasks = new ArrayList<>();
	
	
//////////////////////////////////////////////////////////////////////////
//
// Data Structure classes
//
//////////////////////////////////////////////////////////////////////////

	/**
	 * Task class
	 */
	private class Task {
		private	int					taskNumber;
		private List<Participant> 	participants 			= new ArrayList<>();
		
		public 						Task (int taskNumber) 	{ this.taskNumber = taskNumber; }
		public int 					getTaskNumber () 		{ return taskNumber; }
		
		public Participant	findParticipant (int participantNumber) {
			// finds the corresponding Participant
			for (Participant participant : participants) {
				if (participant.getParticipantNumber() == participantNumber) {
					return participant;
				}
			}
			
			// creates a new Participant, adds it to the list, returns the new Participant
			Participant participant = new Participant (participantNumber);
			participants.add(participant);
			return participant;
		}
		
		public List<OptionChange> updateOption (String objectID, String attrName, String attrValue) {
			List<OptionChange> list = new ArrayList<>();
			
			for(Participant participant : participants) {
				for(OptionChange oc : participant.updateOption(objectID, attrName, attrValue)) {
					oc.setTaskNumber(taskNumber);
					list.add(oc);
				}
			}
			
			return list;
		}
	}

	/**
	 * Participant class
	 */
	private class Participant {
		private	int			participantNumber;
		private List<Obj> 	objs		 							= new ArrayList<>();
		
		public				Participant (int participantNumber)		{ this.participantNumber = participantNumber; }
		public int			getParticipantNumber ()					{ return participantNumber; }
		
		public Obj			findObj (int objNumber) {
			// finds the corresponding Obj
			for (Obj obj : objs) {
				if (obj.getObjNumber() == objNumber) {
					return obj;
				}
			}
			
			// creates a new Obj, adds it to the list, returns the new Obj
			Obj obj = new Obj (objNumber);
			objs.add(obj);
			return obj;
		}
		
		public List<OptionChange> updateOption (String objectID, String attrName, String attrValue) {
			List<OptionChange> list = new ArrayList<>();
			
			for(Obj obj : objs) {
				for(OptionChange oc : obj.updateOption(objectID, attrName, attrValue)) {
					oc.setParticipantNumber(participantNumber);
					list.add(oc);
				}
			}
			
			return list;
		}
	}

	/**
	 * Obj class
	 */
	private class Obj {
		private int						objNumber;
		private int						currentOption			= 0;
		
		private List<Option> 			options 				= new ArrayList<>();
		
		public							Obj (int objNumber)		{ this.objNumber = objNumber; }
		public 	int						getObjNumber ()			{ return objNumber; }
		
		/**
		 * Adds a new Option to this object
		 * @param option	Option to add
		 */
		public	void addOption (Option option) {
			// adds the option to the list of Options
			options.add(option);
		}
		
		public List<OptionChange> updateOption (String objectID, String attrName, String attrValue) {
			List<OptionChange> list = new ArrayList<>();
			
			// iterates through all the options
			// gathers the option flags
			List<Map<Integer, Boolean>> flags = new ArrayList<>();
			for(Option option : options) {
				flags.add(option.updateOption(objectID, attrName, attrValue));
			}
			
			
			// finds which option has all the flags up
			int[] optionFlags = new int [flags.get(0).size() + 1];
			for(int i=0; i < flags.get(0).size() + 1; i++) { optionFlags[i] = 0; }
			
			for(Map<Integer, Boolean> map : flags) {
				for(Integer i : map.keySet()) {
					if(map.get(i)) {
						optionFlags[i]++;
					}
				}
			}
			
			
			for(int i=0; i < flags.get(0).size(); i++) {
				if(optionFlags[i] == flags.size()) {
					// if the new option differs from the currentOption
					if(currentOption != i) {
						// creates a new option chance instance
						OptionChange oc = new OptionChange();
						oc.setObjectNumber(objNumber);
						oc.setCurrentOption(currentOption);
						oc.setNewOption(i);
						
						// adds 
						list.add(oc);
						currentOption = i;
						break;
					}
				}
			}
		
			return list;
		}
	}
	
//////////////////////////////////////////////////////////////////////////
//
// Member methods
//
//////////////////////////////////////////////////////////////////////////
	
	/**
	 * Adds a new Option to the data structure
	 * @param option		New Option to add
	 */
	public void addOption(Option option) {
		int taskNo 			= option.getTaskNumber();
		int participantNo 	= option.getParticipantNumber();
		int objNo			= option.getObjectNumber();
		
		findTask(taskNo).findParticipant(participantNo).findObj(objNo).addOption(option);
	}
	
	/**
	 * Finds the Task with the given task number. Creates a new one when not existing.
	 * 
	 * @param taskNumber	Task number to find
	 * @return				The corresponding Task
	 */
	private Task findTask(int taskNumber) {
		// finds the corresponding Task
		for(Task task : tasks) {
			if(task.getTaskNumber() == taskNumber) {
				return task;
			}
		}
		
		// creates a new Task, adds it to the list, returns the new Task
		Task task = new Task(taskNumber);
		tasks.add(task);
		return task;
	}
	
	/**
	 * Updates an Option and returns the corresponding message
	 * 
	 * @param objectID		Object to update
	 * @param attrName		Name of the attribute to update
	 * @param attrValue		New attribute value
	 * @return				String message to output
	 */
	public List<OptionChange> updateOption(String objectID, String attrName, String attrValue) {
		List<OptionChange> list = new ArrayList<>();
		
		// iterates through the Tasks
		for (Task task : tasks) {
			list.addAll(task.updateOption(objectID, attrName, attrValue));
		}
		
		return list;
	}
}
