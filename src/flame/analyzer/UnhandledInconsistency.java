package flame.analyzer;

import java.text.ParseException;
import java.util.Calendar;

import flame.Utility;

public class UnhandledInconsistency extends Inconsistency {
	
	private		String				inconsistency_type;
	public		String				getInconsistencyType()	{ return inconsistency_type; }
	
	private		String				username;
	public		String				getUsername() 			{ return username; }
	
	private		String				engine_name;
	public		String				getEngineName()			{ return engine_name; }
	
	private		Calendar			sync_action_time;
	public		String				getSyncActionTimeString() { return Utility.convertTimeCtoS(sync_action_time); }
	
	public UnhandledInconsistency(	String			inconsistency_type,
									String			engine_name,
									String			username,
									String 			snapshot_time_string, 
									Inconsistency 	inc) throws ParseException {
		super(inc);
		
		this.inconsistency_type	= inconsistency_type;
		this.engine_name		= engine_name;
		this.username			= username;
		this.sync_action_time 	= Utility.convertTimeStoC(snapshot_time_string);
	}
	
	public		Long				getLifetimeUntilSyncAction() {
		return Utility.timeDiff(start_time, sync_action_time);
	}
}
