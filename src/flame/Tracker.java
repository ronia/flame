package flame;

/**
 * Tracker manages EventID
 * 
 * @author 					<a href=mailto:jaeyounb@usc.edu>Jae young Bang</a>
 * @version					2013.05
 */
public class Tracker {
	
	/**
	 * EventID
	 */
	private int number 		= 0;
	
	/**
	 * Default constructor
	 */
	public Tracker() {
		
	}
	
	/**
	 * Updates the EventID only if the new EventID is greater
	 * 
	 * @param new_number	New EventID
	 */
	public synchronized void set_number (int new_number) {
		if(number < new_number) {
			number = new_number;
		}
	}
	
	/**
	 * Increases the EventID
	 */
	public synchronized void tick_number () {
		number++;
	}
	
	/**
	 * Returns the EventID
	 * 
	 * @return				EventID
	 */
	public int get_number() {
		return number;
	}
}