package flame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

import Prism.core.Event;

/**
 * EventStorage not only stores events, but it also tracks versions and snapshots.<p>
 * 
 * It stores new incoming events in the buffer first, and apply them to the model
 * later when the architect presses the snapshot button. When the snapshot button
 * is pressed, apply Events in the buffer. <p>
 * 
 * Arrival queue is used to order the incoming events that arrive in a random order.
 * 
 * @author 					<a href=mailto:jaeyounb@usc.edu>Jae young Bang</a>
 * @version					2013.05
 */
public class EventStorage {

///////////////////////////////////////////////////////////
//Member variables
///////////////////////////////////////////////////////////
	
	/**
	 * Binary semaphore for the entire EventStorage
	 */
	protected final Semaphore mSemaphore = new Semaphore (1, true);
	
	/**
	 * List of Events that have been forwarded to FLAME Adaptor
	 */
	protected ArrayList<Event> 		eventHistory;
	
	/**
	 * List of Events that have been ordered through the arrival queue, but have NOT YET been forwarded to FLAME Adaptor
	 */
	protected ArrayList<Event>		eventBuffer;
	
	/**
	 * List of Events that are just arriving, but have NOT YET been ordered by their EventID
	 */
	protected ArrayList<Event>		eventArrival; 
	
	/**
	 * Username-EventID mapping: this indicates the last EventID that was assigned to an Event by each architect
	 */
	protected Map<String, Tracker> 	event_id_track;
	
	/**
	 * Username-Snapshot EventID mapping: this indicates the EventID of the last Snapshot from each architect
	 */
	protected Map<String, Tracker> 	snapshot_track;
	
	/**
	 * Username-Snapshot mapping: this indicates how many times each architect has made Snapshots so far
	 */
	protected Map<String, Tracker>	snapshot_version;
	
	/**
	 * Screen Logger passed from the owner Component
	 */
	protected ScreenLogger sl;


	
///////////////////////////////////////////////
//	Constructors
///////////////////////////////////////////////
	
	/**
	 * Default constructor
	 */
	public EventStorage(ScreenLogger screenLogger) {
		sl					= screenLogger;
		
		eventHistory 		= new ArrayList<Event> ();
		eventBuffer			= new ArrayList<Event> ();
		eventArrival		= new ArrayList<Event> ();
		
		event_id_track 		= new TreeMap<String, Tracker> ();
		snapshot_track 		= new TreeMap<String, Tracker> ();
		snapshot_version	= new TreeMap<String, Tracker> ();
	}
	
	
	
///////////////////////////////////////////////
//	Member Methods
///////////////////////////////////////////////
	
	/**
	 * Returns the EventID tracker -- Most Recent Version
	 * 
	 * @return				Username-EventID mapping of MRV				
	 */
	public	Map<String, Tracker>	getEventIDTrack() {
		Map<String, Tracker> ret;
		
		getLock();												// Locks the EventStorage
		ret = new TreeMap<String, Tracker> (event_id_track);	// Retrieves the EventID Tracker
		releaseLock();											// Releases the EventStorage
		
		return ret;
	}
	
	/**
	 * Return the Snapshot tracker -- Latest Snapshots Version
	 * 
	 * @return				Username-EventID mapping of LSV
	 */
	public	Map<String, Tracker>	getSnapshotTrack() {
		Map<String, Tracker> ret;
		
		getLock();												// Locks the EventStorage
		ret = new TreeMap<String, Tracker> (snapshot_track);	// Retrieves the Snapshot Tracker
		releaseLock();											// Releases the EventStorage
		
		return ret;
	}
	

	/**
	 * Adds incoming Event to the History -- the Events sit in there for good
	 * 
	 * @param e				Incoming Event
	 */
	public void addToHistory(Event e) {
		getLock();												// Locks the EventStorage
		eventHistory.add(e); 									// Adds the Event to the History
		updateTrackers(e); 										// Updates all Trackers
		releaseLock();											// Releases the EventStorage
	}
	
	
	/**
	 * Adds incoming Design Event to the Buffer <p>
	 * 
	 * Incoming Events are ordered using an arrival queue.
	 * 
	 * @param e				Incoming Event
	 */
	public void addToBuffer(Event e) {
		
		// Locks the EventStorage
		getLock();				
			
		// Adds the incoming Event to the arrival queue
		eventArrival.add(e); 	
		
		// Sorts the arrival queue 
		Collections.sort(eventArrival, EventComparator.ascending (EventComparator.getComparator (EventComparator.SENDER_USERNAME_SORT, EventComparator.EVENT_ID_SORT)));
		
		// Creates a new arrival queue
		ArrayList<Event> newArrival = new ArrayList<Event> ();

		
		// Move Events that are in order from the arrival queue to the buffer  
		for (Event evt : eventArrival) {
			
			// Checks if the Event Name is Design or Snapshot 
			if(!evt.name.equals("Design") && !evt.name.equals("Snapshot") && !evt.name.equals("Update")) {
				printMsg("Error: A [" + evt.name + "] Event is not supposed to be in the Event arrival queue");
				continue;
			}
			
			// Gets the SenderUsername
			String senderUsername;
			if (evt.hasParameter("SenderUsername")) {
				senderUsername = (String) evt.getParameter("SenderUsername");
			} else {
				printMsg("Error: " + evt.name + " Event in the Event arrival queue does not have the SenderUsername parameter");
				continue;
			}
			
			// Gets the last EventID that has been passed to the buffer for the Architect
			// If never, set it to 0
			int lastEventID = 0;
			if (exists(senderUsername)) {
				lastEventID = event_id_track.get(senderUsername).get_number();
			} else {
				// Creates a new Architect if the EventID tracker of the SenderUsername does not exist
				createNewUserImpl(senderUsername);
			}
			
			// Gets the EventID of this Event
			int eventID = -1;
			if (evt.hasParameter("EventID")) {
				eventID = ((Integer) evt.getParameter("EventID")).intValue();
			} else {
				printMsg("Error: A Design Event in the Event arrival queue does not have the EventID parameter");
				continue;
			}
			
			// Adds the Event to buffer if the EventID of incoming Event is 1 greater than the last Event ID
			if(eventID == lastEventID + 1) {
				eventBuffer.add(evt);		// Adds the Event to the buffer
				updateTrackers(evt); 		// Updates trackers
			} else {
				newArrival.add(evt);		// Adds the Event to the new arrival queue
			}
		}
		
		// Overwrite the arrival queue with the newly created queue
		eventArrival = newArrival;

		// Releases the EventStorage
		releaseLock();
	}
	
	/**
	 * Updates all Trackers according to the outgoing Design Event
	 * 
	 * @param e				Outgoing Design Event
	 */
	public void updateTrackersForOutgoingEvent (Event e) {
		if(e.name.equals("Design")) {
			updateTrackers (e);
		}
	}
	
	
	/**
	 * Updates all Trackers according to the incoming Event
	 * @param e				Incoming Event
	 */
	protected void updateTrackers (Event e) {
		
		// Gets parameters from the incoming Event
		String	eventName		= e.name;				// Event Name
		String 	senderUsername 	= "";					// SenderUsername
		int		eventID 		= -1;					// EventID
		
		// Ignores XTEAM Events
		if(e.name.equals("XTEAM")) {
			return;
		}
		
		// Gets the SenderUsername
		if(e.hasParameter("SenderUsername")) {
			senderUsername = (String) e.getParameter("SenderUsername");
		} else {
			printMsg("Error: An Event for tracker update does not have SenderUsername parameter");
			return;
		}
		
		// Adds a new Architect in case the SenderUsername does not exist in the Tracker
		if(!exists(senderUsername)) {
			// add username to the version tracker and the snapshot tracker
			createNewUserImpl(senderUsername);
		}
		
		// Gets the EventID
		if(e.hasParameter("EventID")) {
			eventID = (Integer) e.getParameter("EventID");
		} else {
			printMsg("Error: An Event for tracker update does not have EventID parameter");
			return;
		}
		
		// Does things per Event Name
		switch(eventName) {
		
			// In case the Event is a Snapshot event
			case "Snapshot":
				// Updates the snapshot tracker
				snapshot_track.get(senderUsername).set_number(eventID);
				snapshot_version.get(senderUsername).tick_number();
				
				// does not break here to also update the event_id_track
		
			// In case the Event is a Design decision event
			case "Update":
			case "Design":
				// Updates the EventID tracker
				event_id_track.get(senderUsername).set_number(eventID);
				break;
		}
	}
	
	/**
	 * Checks if a Username exists in the Tracker
	 * 
	 * @param username		Architect Username
	 * @return				True if the Username exists, false otherwise
	 */
	protected boolean exists (String username) {
		
		// Checks if the Username exists in the event id tracker
		if (!event_id_track.containsKey(username)) {
			return false; 
		}
		
		// Checks if the Username exists in the snapshot tracker
		if (!snapshot_track.containsKey(username)) {
			return false;
		}
		
		// Checks if the Username exists in the snapshot version tracker
		if (!snapshot_version.containsKey(username)) {
			return false;
		}
		
		return true;
	}
	
	
	/**
	 * Returns the next EventID to be assigned with the new Event from an Architect
	 * 
	 * @param username		Architect Username
	 * @return				Next EventID to be assigned
	 */
	public int getNextEventID (String username) {
		return getEventID(event_id_track, username) + 1;
	}
	
	
	/**
	 * Return the current Snapshot EventID (the EventID of the last Snapshot) for an Architect
	 * @param username		Architect Username
	 * @return				Last Snapshot EventID
	 */
	public int getSnapshot (String username) {
		return getEventID(snapshot_track, username);
	}
	
	/**
	 * Returns the entire Snapshot tracker information
	 * 
	 * @return				List of Username-Snapshot EventID pairs
	 */
	public ArrayList<String> getSnapshots() {
		ArrayList<String> ret = new ArrayList<String> ();
		
		for(Map.Entry<String, Tracker> entry : snapshot_version.entrySet()) {
			String username = entry.getKey();
			int version = entry.getValue().get_number();
			
			// Generates a Username @ EventID pair string
			ret.add(username + " @ " + version);
		}
		
		return ret;
	}
	
	/**
	 * Gets the last EventID of an Architect (Username) from a given version
	 * @param version		Username-EventID mapping -- a Version
	 * @param username		Architect Username
	 * @return				Last EventID of the Architect
	 */
	protected int getEventID (Map<String, Tracker> version, String username) {
		
		int lastEventID = 0;
		
		// Locks the EventStorage
		getLock();
		
		// Checks if the username has the snapshot tracker
		if(version.containsKey(username)) {
			lastEventID = version.get(username).get_number();
		} else {
			// Adds Username to the Tracker
			createNewUserImpl(username);
		}
		
		// Releases the EventStorage
		releaseLock();
		
		return lastEventID;
	}
	
	/**
	 * Creates a Tracker (set of three) for an Architect Username
	 * @param username		Architect Username
	 */
	public void createNewUser (String username) {
		
		// Locks the EventStorage
		getLock();
		
		// Checks if the Username already exists in the Tracker
		if(	exists(username) ) {
			return;
		}
		
		// Adds Username to the Tracker
		createNewUserImpl(username);
		
		// Releases the EventStorage
		releaseLock();
	}
	
	/**
	 * Creates a Tracker without locking the EventStorage<p>
	 * 
	 * To be used by the internal methods that locks EventStorage only
	 * 
	 * @param username		Architect Username
	 */
	protected void createNewUserImpl (String username) {
		event_id_track.put		(username, new Tracker());
		snapshot_track.put		(username, new Tracker());
		snapshot_version.put	(username, new Tracker());
	}
	
	
	/**
	 * Retrieves and returns all Events up until a particular Version 
	 * 
	 * @param toThisVersion	Target Version consisting of a set of EventIDs of each Architect
	 * @return				List of Events of the target Version
	 */
	public ArrayList<Event> getEventsFromHistory(Map<String, Tracker> targetVersion) {
		
		// List of Events to return
		ArrayList<Event> ret = new ArrayList<>();
		
		// Locks the EventStorage
		getLock();
		
		// Iterates through the Event History
		for(Event e : eventHistory) {
			
			// filters out XTEAM Events
			if(e.name.equals("XTEAM")) {
				continue;
			}
			
			// Gets the SenderUsername
			String username;
			if(e.hasParameter("SenderUsername")) {
				username = (String) e.getParameter("SenderUsername");
			} else {
				// if it does not have the username, error
				printMsg("Error: Found an Event with no SenderUsername parameter");
				continue;
			}
			
			// Gets the Event ID
			int event_id;
			if(e.hasParameter("EventID")) {
				event_id = (Integer) e.getParameter("EventID");
			} else {
				// if it does not have the event id, error
				printMsg("Error: Found an Event [" + e.name + "] with no Event ID parameter");
				continue;
			}
			
			// Gets the Snapshot info for the particular username
			int target;
			if(targetVersion.containsKey(username)) {
				target = targetVersion.get(username).get_number();
			} else {
				// if it does not have the Snapshot entry, error
				printMsg("Error: Found an Event whose Username does not have a Snapshot Version Tracker");
				continue;
			}
			
			// Checks if the event is included in the version
			if(event_id <= target) {
				ret.add(e);
			}
		}
		
		// Releases the EventStorage
		releaseLock();
		
		return ret;
	}
	
	/**
	 * Purges Events that are part of LSV + all Events from the architect, 
	 * adds them to the history, and returns them back to forward to FLAME Adaptor
	 *   
	 * @param username		Architect username
	 * @return				List of Events to forward to FLAME Adaptor
	 */
	public ArrayList<Event> purgeBufferForArchitect(String username) {
	
		
		// get the snapshot tracker
		Map <String, Tracker> target = getSnapshotTrack();
		
		// manipulate the architect's Event ID to the last Event ID
		Tracker track = target.get(username);
		if(track != null) {
			track.set_number(getEventID(event_id_track, username));
		}
		
		return purgeBuffer(target);
	}
	
	/**
	 * Purges all Events from buffer, adds them to the history, and 
	 * return them back to forward to FLAME Adaptor
	 *   
	 * @return				List of Events to forward to FLAME Adaptor
	 */
	public ArrayList<Event> purgeBufferAll() {
		// get the snapshot tracker
		Map <String, Tracker> target = getEventIDTrack();
				
		return purgeBuffer(target);
	}
	
	/**
	 * Purges Events to a particular Version from the buffer
	 * 
	 * @param targetVersion	Target version (Username-EventID mapping)
	 * @return				List of Events of the target version
	 */
	protected ArrayList<Event> purgeBuffer(Map <String, Tracker> targetVersion) {
		
		ArrayList<Event> ret 			= new ArrayList<>();	// the array to be forwarded to FLAME Adaptor
		ArrayList<Event> newBuffer		= new ArrayList<>();	// the array to be the new eventBuffer
		
		// Locks the EventStorage
		getLock();
		
		// Iterates through the storage
		for(Event e : eventBuffer) {
			
			// Gets the SenderUsername
			String senderUsername;
			if(e.hasParameter("SenderUsername")) {
				senderUsername = (String) e.getParameter("SenderUsername");
			} else {
				// if it does not have the username, error
				printMsg("Error: Found an Event with no SenderUsername parameter");
				continue;
			}
			
			// Gets the Event ID
			int event_id;
			if(e.hasParameter("EventID")) {
				event_id = (Integer) e.getParameter("EventID");
			} else {
				// if it does not have the event id, error
				printMsg("Error: Found an Event with no Event ID parameter");
				continue;
			}
			
			// Gets the target version of the particular username
			int target = 0;
			if(targetVersion.containsKey(senderUsername)) {
				Tracker track = targetVersion.get(senderUsername);
				if(track != null) {
					target = track.get_number();	
				}
			} else {
				// if it does not have the Snapshot entry, error
				printMsg("Error: Found an Event whose Username does not have a Snapshot Version Tracker");
				continue;
			}
			
			// Checks if the event is included in the version
			// In case the Event is from this architect, include it anyway
			
			if(event_id <= target) {
				ret.add(e);				// Adds the Event to the returning array
				eventHistory.add(e);	// Adds the Event to the event history
			} else {
				newBuffer.add(e);		// Adds the Event to the new buffers
			}
		}
		
		// Overwrites the buffer with the new buffer
		eventBuffer = newBuffer;
	
		// Releases the EventStorage
		releaseLock();
		
		// Sorts the list 
		Collections.sort(ret, EventComparator.ascending (EventComparator.getComparator (EventComparator.SENDER_USERNAME_SORT, EventComparator.EVENT_ID_SORT)));
		
		
		
		return ret;
	}
	
	/**
	 * Event comparator for sorting with two comparators: SenderUsername first and then EventID
	 * 
	 * @author 				<a href=mailto:jaeyounb@usc.edu>Jae young Bang</a>
	 * @version				2013.05
	 */
	public static enum EventComparator implements Comparator<Event> {
		
		SENDER_USERNAME_SORT {
			public int compare(Event o1, Event o2) {
				String senderUsername1 = "";
				String senderUsername2 = "";
				
				if(o1.hasParameter("SenderUsername")) {
					senderUsername1 = (String) o1.getParameter("SenderUsername");
				}
				
				if(o2.hasParameter("SenderUsername")) {
					senderUsername2 = (String) o2.getParameter("SenderUsername");
				}
				
				return senderUsername1.compareTo(senderUsername2);
			}}, 
			
		EVENT_ID_SORT {
			public int compare(Event o1, Event o2) {
				Integer eventID1 = new Integer (0);
				Integer eventID2 = new Integer (0);
				
				if(o1.hasParameter("EventID")) {
					eventID1 = (Integer) o1.getParameter("EventID");
				}	
				
				if(o2.hasParameter("EventID")) {
					eventID2 = (Integer) o2.getParameter("EventID");
				}
				
				return eventID1.compareTo(eventID2);
			}};
			
		public static Comparator<Event> ascending (final Comparator<Event> other) {
			return new Comparator<Event>() {
	            public int compare(Event o1, Event o2) {
	                return other.compare(o1, o2);
	            }
	        };
		}
		
		public static Comparator<Event> getComparator(final EventComparator... multipleOptions) {
			return new Comparator<Event>() {
	            public int compare(Event o1, Event o2) {
	                for (EventComparator option : multipleOptions) {
	                    int result = option.compare(o1, o2);
	                    if (result != 0) {
	                        return result;
	                    }
	                }
	                return 0;
	            }
	        };
		}	
	}
	
	/**
	 * Locks the EventStorage
	 */
	protected void getLock() {
		try {
			mSemaphore.acquire();	// get the semaphore
		} catch (InterruptedException ie) {
			printMsg("Thread interrupted while waiting for the semaphore");
		}
	}
	
	/**
	 * Releases the EventStorage
	 */
	protected void releaseLock() {
		mSemaphore.release();
	}
	
	/**
	 * Print screen messages
	 * 
	 * @param msg			Message to print to screen
	 */
	protected void printMsg(String msg) {
		sl.printMsg("Event Storage", msg);
	}
	
	
	
}
