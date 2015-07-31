package flame.analyzer;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import flame.Utility;

/**
 * Inconsistency class trackas each inconsistency
 * 
 * @author 					<a href="mailto:jaeyounb@usc.edu">Jae young Bang</a>
 * @version					2014.12
 */
public class Inconsistency {
	protected 	Calendar 			start_time;
	public		Calendar			getStartTime() 		{ return start_time; }
	
	protected	Synchronization		commit 				= new Synchronization ("Commit");
	public		Synchronization		getCommitHistory() 	{ return commit.duplicate(); }
	protected	Synchronization		update				= new Synchronization ("Update");
	public		Synchronization		getUpdateHistory()	{ return update.duplicate(); }
	
	public		int					getCommitCounter() 	{ return commit.getPerformedCounter(); }
	public		int					getUpdateCounter()	{ return update.getPerformedCounter(); }
	
	protected	Calendar			first_commit_time;
	
	protected	boolean				has_end_time;
	public		boolean				getHasEndTime()	{ return has_end_time; }	
	
	protected 	Calendar			end_time;
	public		Calendar			getEndTime() 	{ return end_time; }
	
	protected 	String				message;
	public		String				getInconsistencyMessage()	{ return message; }
	
	protected 	boolean				unresolved = false;
	public		void				setUnresolved()	{ unresolved = true; }
	public		boolean				getUnresolved()	{ return unresolved; }
	
	
	public 		Inconsistency (		String 		start_time_string,
									String		inconsistency_message) throws ParseException {
		start_time			= Utility.convertTimeStoC(start_time_string);
		has_end_time 		= false;
		this.message 		= inconsistency_message;
	}
	
	/**
	 * Creator for duplication 
	 * 
	 * @param start_time
	 * @param has_end_time
	 * @param end_time
	 * @param message
	 */
	public		Inconsistency (		Calendar	start_time,
									boolean		has_end_time,
									Calendar	end_time,
									String		message) {
		this.start_time 	= start_time;
		this.has_end_time	= has_end_time;
		this.end_time		= end_time;
		this.message		= message;
	}
	
	/**
	 * Creator for duplication
	 * @param start_time
	 * @param has_end_time
	 * @param end_time
	 * @param message
	 * @param commit
	 * @param update
	 */
	public		Inconsistency (		Calendar		start_time,
									boolean			has_end_time,
									Calendar		end_time,
									String			message,
									Synchronization	commit,
									Synchronization update) {
		this.start_time 		= start_time;
		this.has_end_time		= has_end_time;
		this.end_time			= end_time;
		this.message			= message;
		this.commit				= new Synchronization (commit);
		this.update				= new Synchronization (update);
	}
	
	/**
	 * Creator for duplication
	 * @param inc
	 */
	public		Inconsistency (		Inconsistency inc) {
		this.start_time			= inc.getStartTime();
		this.has_end_time		= inc.getHasEndTime();
		this.end_time			= inc.getEndTime();
		this.message			= inc.getInconsistencyMessage();
	}
	
	public		Inconsistency		duplicate() {
		return new Inconsistency (	start_time,
									has_end_time,
									end_time,
									message,
									commit.duplicate(),
									update.duplicate());
	}
	
	
	/**
	 * Returns the first commit time of the inconsistency if it has previously been committed
	 * 
	 * @return
	 * @throws Exception		This inconsistency has never been committed
	 */
	public		String			getFirstCommitTime() throws Exception { 
		return Utility.convertTimeCtoS(commit.getFirstPerformTime());
	}
	
	/**
	 * Returns the first update time of the inconsistency if it has previously been updated
	 * @return
	 * @throws Exception
	 */
	public		String			getFirstUpdateTime() throws Exception {
		return Utility.convertTimeCtoS(update.getFirstPerformTime());
	}
	
	/**
	 * Records a commit performed while the inconsistency exists
	 * @param username
	 * @param commit_time
	 * @throws ParseException
	 */
	public		void				setCommitted(String username, String commit_time) throws ParseException { 
		commit.perform(username, commit_time);
	}
	
	/**
	 * Records an update performed while the inconsistency exists
	 * @param username
	 * @param update_time
	 * @throws ParseException
	 */
	public		void				setUpdated(String username, String update_time)	throws ParseException {
		update.perform(username, update_time);
	}
	
	public		void				setEndTime(String end_time_string) throws ParseException {
		end_time 				= Utility.convertTimeStoC(end_time_string);
		has_end_time 			= true;
	}
	
		public 		long				getLifetimeInSecs()  throws Exception {
		if(!has_end_time) {
			throw new Exception ("End time has not been set");
		}
		
		return Utility.timeDiff(start_time, end_time);
	}
	
	public		String				getStartTimeString() {
		return Utility.convertTimeCtoS(start_time);
	}
	
	public		String				getEndTimeString() throws Exception {
		if(!has_end_time) {
			throw new Exception ("End time has not been set");
		}
		
		return Utility.convertTimeCtoS(end_time);
	}
	
	
	/**
	 * Synchronization class represents that history of this inconsistency
	 * with regards to the synchronization actions such as commit and update.
	 * 
	 * @author 					<a href="mailto:jaeyounb@usc.edu">Jae young Bang</a>
	 * @version					2014.12
	 */
	protected class Synchronization {
		
		/**
		 * The name of the synchronization action this class represents
		 */
		private String name;
		public 	String getName() { return name; }

		/**
		 * The times that the synchronization action has been performed
		 */
		private	List<Performance> performedTimes = new ArrayList<>();
		public	List<Performance> getPerformedTimes () { return performedTimes; }
		
		/**
		 * Constructor
		 * 
		 * @param name	The name of the synchronization action this class represents
		 */
		public Synchronization (	String name) {
			this.name = Utility.toCamelCase(name);
		}
		
		/**
		 * Duplication constructor
		 * @param name
		 * @param performed
		 * @param performedTimes
		 */
		public Synchronization (	String 				name,
									List<Performance> 	performedTimes) {
			this.name				= name;
			for(Performance p : performedTimes) {
				this.performedTimes.add(new Performance(p.getUsername(), p.getTime()));
			}
		}
		
		/**
		 * Duplication constructor
		 * @param sync
		 */
		public Synchronization (	Synchronization sync) {
			name					= sync.getName();
			for(Performance p : sync.getPerformedTimes()) {
				performedTimes.add(new Performance(p.getUsername(), p.getTime()));
			}
		}
		
		/**
		 * Returns a duplicate
		 * @return
		 */
		public Synchronization duplicate() {
			return new Synchronization(	name,
										performedTimes);
		}
		
		/**
		 * One instance of this synchronization action
		 */
		public class Performance {
			private String 		username;
			private Calendar 	time;
			
			public Performance (String username, Calendar time) {
				this.username 	= username;
				this.time		= time;
			}
			
			public String 	getUsername() 	{ return username; }
			public Calendar getTime() 		{ return time; }
		}
		
		/**
		 * Returns the number of times this sync action has been performed
		 * @return
		 */
		public	int getPerformedCounter() {
			return performedTimes.size();
		}
		
		/**
		 * Performs the synchronization action
		 * @param timestamp			FLAME timestamp string
		 * @throws ParseException	Given timestamp string cannot be parsed
		 */
		public	void perform(String username, String timestamp) throws ParseException {
			performedTimes.add(new Performance (username, Utility.convertTimeStoC(timestamp)));
		}
		
		/**
		 * Returns the time of the first performed instance of this synchronization action on this inconsistency
		 * 
		 * @return
		 * @throws Exception
		 */
		public	Calendar getFirstPerformTime() throws Exception {
			if(performedTimes.size() == 0) {
				throw new Exception ("No prior " + name + " has been performed to this inconsistency");
			}
			
			return performedTimes.get(0).getTime();
		}
		
		/**
		 * Returns the user who first performed this synchronization action on this inconsistency
		 * 
		 * @return
		 * @throws Exception
		 */
		public	String getFirstPerformUser() throws Exception {
			if(performedTimes.size() == 0) {
				throw new Exception ("No prior " + name + " has been performed to this inconsistency");
			}
			
			return performedTimes.get(0).getUsername();
		}
	}
}
