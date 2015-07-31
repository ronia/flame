package flame.client;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import flame.Requirements;
import flame.ScreenLogger;
import Prism.core.Event;

public abstract class FLAMEGUI {
	
///////////////////////////////////////////////
//Member Variables
///////////////////////////////////////////////
	
	/**
	 * FLAMEClient instance to invoke the Snapshot and Update methods
	 */
	protected 		FLAMEClient		 		client;
	
	/**
	 * Display member instance
	 */
	protected 		Display 				display;
	
	/**
	 * Shell member instance
	 */
	protected 		Shell 					shell;
	
	/**
	 * List of target analyses<p>
	 * 
	 * E.g. Energy/Total/less than/700000
	 */
	protected		Requirements			requirements;
	
	/**
	 * Username of the architect
	 */
	protected		String					username;
	
	/**
	 * Switch that turns on and off the XTEAM GUI window for experimental purposes
	 */
	protected		boolean					xteamGUISwitch;
	
	/**
	 * Class that stores the local analysis info
	 */
	protected		LocalAnalysisInfo		localInfo 		= new LocalAnalysisInfo();
	
	/**
	 * Update button enabled?
	 */
	protected		boolean					isUpdateButtonEnabled = false;
	
	/**
	 * The ScreenLogger to print messages to screen
	 */
	protected 		ScreenLogger			sl;
	
///////////////////////////////////////////////
//Constructors
///////////////////////////////////////////////
	
	/**
	 * Default constructor
	 * 
	 * @param flameClient		FLAMEClient instance	
	 * @param username			Username of the architect
	 * @param xteamGUISwitch	Switch to disable realtime conflict information presentation
	 * @param screenLogger		ScreenLogger of FLAMEClient
	 */
	public FLAMEGUI (FLAMEClient flameClient, String username, boolean xteamGUISwitch, ScreenLogger screenLogger) {
		// stores the FLAMEClient instance
		client 					= flameClient;
		
		// stores the username
		this.username 			= username;
		
		// stores the XTEAM GUI window switch
		this.xteamGUISwitch 	= xteamGUISwitch;
		
		// stores the ScreenLogger instance
		sl 						= screenLogger;
		
		// reads in list of target analyses
		try {
			requirements = new Requirements(1, screenLogger);
		} catch (Exception e){
			printMsg("Error: " + e.getMessage());
		}
		
		// Initializes the window components
		initComponents();
	}
	
	/**
	 * Opens the shell
	 */
	public void openShell() {
		shell.open();
		while(!shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
		
		display.dispose();
	}
	
	/**
	 * Initializes the shell with widgets
	 */
	protected abstract void initComponents();
	
	/**
	 * Presents incoming Snapshot information to the FLAME GUI
	 * 
	 * @param e
	 */
	public abstract void presentSnapshot(final Event e);
	
	/**
	 * Updates the Latest Versions field
	 */
	public abstract void updateLV(final ArrayList<String> lv);
	
	/**
	 * Presents XTEAM analysis information an incoming Event contains
	 * 
	 * @param e				Incoming Event
	 */
	public abstract void presentXTEAMInfo(final Event e);
	
	/**
	 * Presents the on-going simulation thread number
	 *  
	 * @param senderUsername	From which XTEAM Engine the Notification Event is
	 * @param thread_count		Number of threads
	 */
	public abstract void setSimStatus(String senderUsername, int thread_count);
	
	/**
	 * Sets the status bar message in cyan
	 * 
	 * @param message		New message
	 */
	public abstract void setInProgressStatus(final String message);
	
	/**
	 * Sets the status bar message in gray
	 * 
	 * @param message		New message
	 */
	public abstract void setIdleStatus(final String message);
	
	/**
	 * Disable commit/update buttons
	 */
	public void disableButtons() {
		disableButtonsImpl();
	}
	
	/**
	 * Enable commit/update buttons
	 */
	public void enableButtons() {
		enableButtonsImpl();
	}
	
	/**
	 * Disables the commit button
	 */
	public void disableCommitButton() {
		disableCommitButtonImpl();
	}
	
	/**
	 * Enables the commit button
	 */
	public void enableCommitButton() {
		enableCommitButtonImpl();
	}
	
	/**
	 * Disables the update button
	 */
	public void disableUpdateButton() {
		isUpdateButtonEnabled = false;
		disableUpdateButtonImpl();
	}
	
	/**
	 * Enables the update button
	 */
	public void enableUpdateButton() {
		isUpdateButtonEnabled = true;
		enableUpdateButtonImpl();
	}

	
	/**
	 * Disable commit/update buttons
	 */
	public abstract void disableButtonsImpl();
	
	/**
	 * Enable commit/update buttons
	 */
	public abstract void enableButtonsImpl();
	
	/**
	 * Disables the commit button
	 */
	public abstract void disableCommitButtonImpl();
	
	/**
	 * Enables the commit button
	 */
	public abstract void enableCommitButtonImpl();
	
	/**
	 * Disables the update button
	 */
	public abstract void disableUpdateButtonImpl();
	
	/**
	 * Enables the update button
	 */
	public abstract void enableUpdateButtonImpl();
	
	
	/**
	 * Presents the stored LocalV analysis information<p>
	 * 
	 * This method is invoked when the Check Local button is pressed.
	 */
	public abstract void presentLocalInfo();
	
	/**
	 * Pops up a question dialog
	 * 
	 * @param title			Dialog window title
	 * @param question		Dialog question
	 * @return				Selected button's ID
	 */
	public int popQuestionDialog(String title, String question) {
		// create dialog with ok and cancel button and info icon
		MessageBox dialog = new MessageBox(shell, SWT.ICON_QUESTION | SWT.OK| SWT.CANCEL);
		dialog.setText(title);
		dialog.setMessage(question);

		// open dialog and await user selection
		return dialog.open();
	}
	
	public int popMessageDialog(String title, String message) {
		// create dialog with ok and cancel button and info icon
		MessageBox dialog = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
		dialog.setText(title);
		dialog.setMessage(message);

		// open dialog and await user selection
		return dialog.open();
	}
	
	/**
	 * Gets the status of the Update button. 
	 * 
	 * @return				True if enabled / false if disabled
	 */
	public boolean getUpdateButtonStatus() {
		return isUpdateButtonEnabled;
	}
	
	/**
	 * Sets the status of the Update button.
	 * 
	 * @param status		True if enabled / false if disabled
	 */
	public void setUpdateButtonStatus(boolean status) {
		isUpdateButtonEnabled = status;
	}
	
	/**
	 * Print screen messages
	 * 
	 * @param msg			Message to print to screen
	 */
	protected void printMsg(String msg) {
		sl.printMsg("FLAME GUI", msg);
	}
}
