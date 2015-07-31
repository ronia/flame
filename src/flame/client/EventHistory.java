package flame.client;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import Prism.core.Event;

/**
 * EventHistory is used to avoid echoing of Events that have been forwarded to FLAME Adaptor
 * 
 * @author 					<a href=mailto:jaeyounb@usc.edu>Jae young Bang</a>
 * @version					2013.05
 */
public class EventHistory {
	
	/**
	 * List of Events that have been forwarded to FLAME Adaptor
	 */
	private ArrayList<Event> 		events 		= new ArrayList<Event>();
	
	/**
	 * Maximum size of the buffer up which it could grow
	 */
	private final int 				max_size	= 10000;
	
	/**
	 * Default constructor
	 */
	public EventHistory() {
		
	}
	
	/**
	 * Inserts Event into the list
	 * 
	 * @param e				Event to insert to the list
	 */
	public void insertEvent(Event e) {
		
		// Locks the list
		getLock_List();
		
		if(events.size() <= max_size) {
			events.add(e);
		} else {
			events.remove(0);
			events.add(e);
		}
		
		// Releases the list
		releaseLock_List();
	}
	
	/**
	 * Checks if an Event is echoing (meaning if it exists in the list) <p>
	 * 
	 * It compares the Value parameter of an Event with the values in the Events in the list
	 * 
	 * @param value			Value of an Event			
	 * @return				True if there is an existing Event with the same Value parameter
	 */
	public boolean isEchoing(String value) {
		
		boolean ret = false;
		
		// Locks the list
		getLock_List();
		
		for(int i=events.size()-1; i>=0; i--) {
			if(((String)events.get(i).getParameter("Value")).compareTo(value) == 0 ) {
				
				// Removes the element from the list
				events.remove(i);
				ret = true;
				
				break;
			}
		}
		
		// Releases the list
		releaseLock_List();
		
		return ret;
	}
	
	/**
	 * Binary semaphore for the list
	 */
	private final Semaphore mLockList = new Semaphore (1, true);
	
	/**
	 * Locks the screen
	 */
	private void getLock_List () {
		try {
			mLockList.acquire();	// Gets the semaphore
		} catch (InterruptedException ie) {
			System.out.println ("ScreenLogger thread interrupted while waiting for the semaphore");
		}
	}
	
	/**
	 * Releases the screen
	 */
	private void releaseLock_List () {
		mLockList.release();
	}
}
