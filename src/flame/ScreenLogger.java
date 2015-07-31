package flame;

import java.util.concurrent.Semaphore;

/**
 * Screen Logger manages the messages printed to the screen
 * 
 * @author 					<a href=mailto:jaeyounb@usc.edu>Jae young Bang</a>
 * @version					2013.05
 */
public class ScreenLogger {
	
	/**
	 * Default constructor
	 */
	public ScreenLogger() {
		
	}
	
	/**
	 * Prints an empty line to screen
	 */
	public void printEmptyLine() {
		getLock_Screen();
		System.out.println();
		releaseLock_Screen();
	}
	
	/**
	 * Prints a message to screen
	 * 
	 * @param name			Component name
	 * @param msg			Message to print to screen
	 */
	public void printMsg(String name, String msg) {
		getLock_Screen();										// Locks the logger
		System.out.println("[" + name + "]: " + msg);			// Prints the message
		releaseLock_Screen();									// Releases the logger
	}
	
	/**
	 * Binary semaphore for the screen<p>
	 *  
	 * There could be several threads that want to print messages
	 * to the screen at the same time. That messes up the screen
	 * output when the messages are mixed up. This semaphore makes
	 * sure the messages do not mix up on the screen.
	 */
	private final Semaphore mLockScreen = new Semaphore (1, true);
	
	/**
	 * Locks the screen
	 */
	private void getLock_Screen () {
		try {
			mLockScreen.acquire();	// Gets the semaphore
		} catch (InterruptedException ie) {
			System.out.println ("ScreenLogger thread interrupted while waiting for the semaphore");
		}
	}
	
	/**
	 * Releases the screen
	 */
	private void releaseLock_Screen () {
		mLockScreen.release();
	}
}
