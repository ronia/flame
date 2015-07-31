package flame.analyzer;

import java.text.ParseException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Class that stores a pair of open/closed inconsistencies
 */
public class InconsistencyMapping {
	private 	Map<String, Set<Inconsistency>> openInconsistencies;
	private		Map<String, Set<Inconsistency>> closedInconsistencies;
	
	public		InconsistencyMapping() {
		openInconsistencies 	= new TreeMap<>();
		closedInconsistencies 	= new TreeMap<>();
	}
	
	public		Map<String, Set<Inconsistency>> getOpenIncs() 	{ return openInconsistencies; }
	public		Map<String, Set<Inconsistency>> getClosedIncs()	{ return closedInconsistencies; }
	
	/**
	 * Creates a new username entry for both open and closed inconsistencies maps
	 * @param username		New username
	 */
	public		void newUsername(String username) {
		openInconsistencies.put(username, new HashSet<Inconsistency>());
		closedInconsistencies.put(username, new HashSet<Inconsistency>());
	}
	
	/**
	 * Closes all open inconsistencies by setting the end time and moving to closed inc set
	 * @param endTimeString
	 * @throws ParseException
	 */
	public		void closeAllIncs(String endTimeString) throws ParseException {
		// closes all open syntactic inconsistencies
		for (String key : openInconsistencies.keySet()) {
			Set<Inconsistency> open_incs 	= openInconsistencies.get(key);
			Set<Inconsistency> closed_incs 	= closedInconsistencies.get(key);
			
			for(Inconsistency open_inc : open_incs) {
				// marks as unresolved
				open_inc.setUnresolved();
				
				// sets the end time
				open_inc.setEndTime(endTimeString);
				
				// adds to the closed list
				closed_incs.add(open_inc);
			}
			
			open_incs.clear();
		}
	}
}
