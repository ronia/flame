package flame.client.xteam;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Label;

import flame.client.FLAMEClient;
import flame.client.FLAMEGUI;
import flame.ScreenLogger;
import Prism.core.Event;

public class SimpleGUI extends FLAMEGUI {
	
///////////////////////////////////////////////
//Member Variables
///////////////////////////////////////////////
	
	/**
	 * The mode of the XTEAM engine of which this GUI presents the result
	 */
	protected	String				mode;
	
	/**
	 * The 'C' Commit button
	 */
	protected 	Button 				buttonCommit;
	
	/**
	 * The 'U' Update button
	 */
	protected 	Button				buttonUpdate;
	
	/**
	 * The 'E' Exit button
	 */
	protected 	Button				buttonExit;
	
	/**
	 * Flag boxes
	 */
	protected	Label[]				flagBoxes;
	
	/**
	 * The simulation status bar
	 */
	protected	Label				labelSimStatusBar;
	
	/**
	 * The update status bar
	 */
	protected 	Label				labelUpdateStatusBar;
	
	/**
	 * Timestamp of the last message
	 */
	protected	String				arrivalTime_overall			= new String("00000000_0000_00");
	
	/**
	 * Semaphore to control the maximum number of concurrent XTEAM simulation threads.
	 */
	protected Semaphore 			mLockGUI;
	
	/**
	 * Locks the outgoing Events semaphore
	 */
	public void getLock() {
		try {
			mLockGUI.acquire();	// get the semaphore
		} catch (InterruptedException ie) {
			printMsg("Thread interrupted while waiting for the semaphore");
		}
	}
	
	/**
	 * Releases the outgoing Event semaphore
	 */
	public void releaseLock() {
		mLockGUI.release();
	}

///////////////////////////////////////////////
//Constructors
///////////////////////////////////////////////
	
	/**
	 * Default constructor. Mainly invokes the super constructor.
	 * 
	 * @param flameClient		FLAMEClient instance	
	 * @param username			Username of the architect
	 * @param mode				The mode of the XTEAMEngine of which this GUI presents. Could be one of MRSV, LSV, LocalV, or HeadLocalV
	 * @param xteamGUISwitch	Switch to disable realtime conflict information presentation
	 * @param screenLogger		ScreenLogger of FLAMEClient
	 */
	public SimpleGUI(	FLAMEClient flameClient, 
						String username,
						String mode,
						boolean xteamGUISwitch, 
						ScreenLogger screenLogger) {
		super(flameClient, username, xteamGUISwitch, screenLogger);
		this.mode = mode;
	}

	@Override
	protected void initComponents() {
		mLockGUI 			= new Semaphore (1, true);
		
		// Initializes the output window
		display = new Display();
		
		shell = new Shell(display, SWT.TITLE | SWT.ON_TOP);
		//shell.setLayout(new FillLayout());
		shell.setText("FLAME");
		
		// reads the screen DPI setting and adjust magnification
		int mag = shell.getDisplay().getDPI().x > 96 ? 2 : 1;
		
		int x_gap 				= 	5;										// landscape gap between things
		int y_gap 				= 	3;										// vertical gap between things
		
		int box_width 			= 	55;										// flag box width
		int box_height			= 	20;										// flag box height
		int box_number			= 	requirements.getAnalysisTypes().size(); // number of flag boxes
		int max_box_number		= 	box_number > 3 ? box_number : 3;
		
		int status_bar_height	= 	18;
		int	button_height		=	23;
		
		int	title_bar_height	=	25;
		int window_width		= 	x_gap + (box_width + x_gap) * max_box_number + x_gap;
		int window_height		= 	title_bar_height + 
									status_bar_height + 
									y_gap + 
									box_height + 
									y_gap + 
									button_height + 
									y_gap +
									status_bar_height;
		printMsg("Windows width: " + window_width + " window_height: " + window_height);
		
		shell.setSize(window_width * mag, window_height * mag);
		
		Monitor primary = display.getPrimaryMonitor();
		Rectangle bounds = primary.getBounds();
		Rectangle rect = shell.getBounds();
    
	    int x = bounds.width - rect.width - 100;
	    int y = bounds.height - rect.height - 100;
		
		shell.setLocation(x, y);
		
		// creates flag boxes
		int index = 0;
		flagBoxes = new Label[requirements.getAnalysisTypes().size()];
		
		for(String analysisType : requirements.getAnalysisTypes()) {
			Label label = new Label(shell, SWT.BORDER | SWT.CENTER);
			label.setSize(box_width * mag, box_height * mag);
			label.setText(analysisType);
			if(!xteamGUISwitch) {
				label.setEnabled(false);
			}
			label.setLocation((x_gap + (index * (box_width + x_gap))) * mag, (status_bar_height+y_gap) * mag);
			
			flagBoxes[index++] = label;
		}
		
		// creates simulation status bar
		labelSimStatusBar = new Label(shell, SWT.NONE);
		if(!xteamGUISwitch) {
			labelSimStatusBar.setText("Simulation disabled");
		} else {
			labelSimStatusBar.setText("Simulation status");
		}
		
		labelSimStatusBar.setSize(window_width * mag, status_bar_height * mag);
		labelSimStatusBar.setLocation(0 * mag, 0 * mag);
		labelSimStatusBar.setBackground(display.getSystemColor(SWT.COLOR_GRAY));
		
		// creates update status bar
		labelUpdateStatusBar = new Label(shell, SWT.NONE);
		labelUpdateStatusBar.setText("Ready");
		labelUpdateStatusBar.setSize(window_width * mag, status_bar_height * mag);
		labelUpdateStatusBar.setLocation(0 * mag, (window_height-status_bar_height-title_bar_height) * mag);
		labelUpdateStatusBar.setBackground(display.getSystemColor(SWT.COLOR_GRAY));
		
		// commit button
		buttonCommit = new Button(shell, SWT.PUSH);
		buttonCommit.setLocation(x_gap * mag, (status_bar_height + y_gap + box_height + y_gap) * mag);
		buttonCommit.setSize(box_width * mag, button_height * mag);
		buttonCommit.setText("Commit");
		buttonCommit.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				printMsg("Commit button pressed");
				client.commit();
			}
		});
		//buttonCommit.setEnabled(false);
		
		// Update button
		buttonUpdate = new Button(shell, SWT.PUSH);
		buttonUpdate.setLocation((x_gap + ((x_gap + box_width) * 1)) * mag, (status_bar_height + y_gap + box_height + y_gap) * mag);
		buttonUpdate.setSize(box_width * mag, button_height * mag);
		buttonUpdate.setText("Update");
		buttonUpdate.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				printMsg("Update button pressed");
				client.update();
			}
		});
		//buttonUpdate.setEnabled(false);
		
		// Exit button
		buttonExit = new Button(shell, SWT.PUSH);
		buttonExit.setLocation((x_gap + ((x_gap + box_width) * 2)) * mag, (status_bar_height + y_gap + box_height + y_gap) * mag);
		buttonExit.setSize(box_width * mag, button_height * mag);
		buttonExit.setText("Exit");
		buttonExit.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				printMsg("Exit button pressed");
				client.getModelingTool().destroy();
				shell.dispose();
			}
		});
		
		printMsg("Component initialization complete.");
	}
	
	@Override
	public void presentSnapshot(final Event e) {
		// do nothing
	}

	@Override
	public void updateLV(ArrayList<String> lv) {
		// do nothing
	}

	@SuppressWarnings("unchecked")
	@Override
	public void presentXTEAMInfo(Event e) {
		
		// If the proactive conflict detection option is turned off, stops it.
		if(!xteamGUISwitch) {
			return;
		}
		
		// Checks if the event is an XTEAM info
		if(!e.name.equals("XTEAM")) {
			return;
		}
		
		// gets the username of the XTEAM Event ... SenderUsername is the mode (MRSV, LSV, LocalV)!
		String senderUsername;
		if(e.hasParameter("SenderUsername")) {
			senderUsername = (String) e.getParameter("SenderUsername");
		} else {
			printMsg("Error: XTEAM Event does not have the SenderUsername parameter");
			return;
		}
		
		// Checks if the senderUsername is NOT equal to the mode
		if(!senderUsername.equals(mode)) {
			return;
		}
		
		// Gets the arrival time (Design Event arrival at the Engine) of the Event
		String arrivalTime;
		if(e.hasParameter("ArrivalTime")) {
			arrivalTime = (String) e.getParameter("ArrivalTime");
		} else {
			printMsg("Error: XTEAM Event does not have the ArrivalTime parameter");
			return;
		}
		
		// checks the time stamp to see if it is from the past
		if(arrivalTime_overall.compareTo(arrivalTime) > 0) {
			return;
		}
		
		/*
		 * 
		 * If the flow has reached here, that means at least one flag operation
		 * (e.g. down, green up, red up) will be performed. 
		 * 
		 */
		
		// lock the semaphore
		getLock();
		
		// overwrites the most recent time stamp
		arrivalTime_overall = arrivalTime;
		
		ArrayList<String> 						syntactic_conflicts = new ArrayList<>();
		Map<String, java.util.List<String>> 	warnings 			= null;
		boolean									analysisComplete 	= true;
		
		// If the Event has a list of syntactic conflicts
		if(e.hasParameter("SyntacticConflicts")) {
			ArrayList<String> data = (ArrayList<String>) e.getParameter("SyntacticConflicts");
			
			// If the size is 1, the only entry could be a "None"
			for(String entry : data) {
				if(!entry.equals("None")) {
					syntactic_conflicts.add("[ERR]: " + entry);
					analysisComplete = false;
				}
			}
		}
		
		// Gets the XTEAM analysis type (Energy, Latency, ...).
		String 	analysisType 		= "All";
		if(e.hasParameter("AnalysisType")) {	
			analysisType = (String) e.getParameter("AnalysisType");
		} 
		
		// Checks if the analysis type is known
		if(requirements.hasAnalysisType(analysisType)) {
			// If the Event has a list of analysis warning messages
			if(e.hasParameter("AnalysisWarnings")) {
				warnings = (Map<String, java.util.List<String>>) e.getParameter("AnalysisWarnings");
				
				for(String key : warnings.keySet()) {
					// if the analysis type of this Event has any warnings
					if(key.equals(analysisType) && warnings.get(key).size() > 0) {
						analysisComplete = false;
					}
				}
			}
			
			// Checks if there was any errors or warnings. Turns the flagbox to gray.
			if(analysisComplete == false) {
				// turns the flagbox to gray
				flagDown(analysisType);
			} else {
				// Checks if the values satisfy the requirement(s)
				boolean requirementSatisfied = true;
				
				try {
					// Looks at all requirements for the analysis type (e.g. Energy, Latency, etc.)
					for(String target : requirements.getTargetValueNames(analysisType)) {
						
						// Received simulation result is in string format. Needs to be transformed to a double value.
						double doubleValue 	= 0;
						String strValue		= "";
						
						// Depending on what target value (total, maximum, etc.) the requirement wants to look at
						String parameterName = "";
						switch (target.toLowerCase()) {
							case "total":
								parameterName = "OverallTotal"; break;
							case "maximum":
								parameterName = "OverallMax"; break;
							case "average":
								parameterName = "OverallAverage"; break;
							case "success":
								parameterName = "OverallSuccess"; break;
							default:
								printMsg("Unknown overall target value name found: " + target);
								continue; // to the next requirement!
						}
						
						if(e.hasParameter(parameterName)) {
							strValue = (String) e.getParameter(parameterName);
							try {
									// transforms the String formatted value to double for computation
									if(!target.toLowerCase().equals("success")) {
										doubleValue = transformToDouble(strValue);
									} else {
										doubleValue = transformSuccessRateToDouble(strValue);
									}
									
									// checks if the related requirement is satisfied
									requirementSatisfied = requirements.isSatisfied(analysisType, target, doubleValue);
									if(requirementSatisfied == false) {
										break;
									}
									
							} catch (Exception exc) {
								printMsg("Received " + parameterName + " value is incomprehensible: " + strValue);
							}
						} else {
							printMsg("Event lacks " + parameterName + " attribute");
						}
					}
					
					// Flags up depending on whether the requirements are satisfied
					if(requirementSatisfied) {
						greenFlagUp(analysisType);
					} else {
						redFlagUp(analysisType);
					}
				} catch (Exception exc) {
					printMsg("Requirements validation: " + exc.getMessage());
				}
			}	
		}
		
		releaseLock();
	}
	
	/**
	 * XTEAMEngine sends values in the "TargetValueName: value" format. 
	 * It is necessary to strip the irrelevant text around the value to use it
	 * for computation.
	 * 
	 * @param strValue			String value received from XTEAMEngine
	 * @return					double value
	 */
	protected double transformToDouble(String strValue) throws Exception {
		double value = (double) 0;
		
		String[] tokens = strValue.split(":");
		if(tokens.length != 2) {
			throw new Exception ("Received string value is ill-formatted: " + strValue);
		}
		
		try {
			value = Double.parseDouble(tokens[1].trim());
		} catch (NumberFormatException nfe) {
			throw new Exception ("Received string value contains ill-formatted number: " + strValue);
		}
		
		return value;
	}
	
	/**
	 * XTEAMEngine sends success rate values in the "success/total[rate]" format. 
	 * It is necessary to strip the irrelevant text around the value to use it
	 * for computation.
	 * 
	 * @param strValue			String success value received from XTEAMEngine
	 * @return					double success value
	 */
	protected double transformSuccessRateToDouble(String strValue) throws Exception {
		double value = (double) 0;
		
		String[] tokens = strValue.split("\\[");
		if(tokens.length != 2) {
			throw new Exception ("Received string value is ill-formatted: " + strValue);
		}
		
		try {
			value = Double.parseDouble(tokens[1].trim().substring(0, tokens[1].length()-2));
		} catch (NumberFormatException nfe) {
			throw new Exception ("Received string value contains ill-formatted number: " + strValue);
		}
		
		return value;
	}
	
	/**
	 * Turns the background color of a flagbox to gray
	 * 
	 * @param analysisType		Anaysis type of the flagbox to change
	 */
	protected void flagDown(String analysisType) {
		changeFlagColor(analysisType, SWT.COLOR_GRAY);
	}
	
	/**
	 * Turns the background color of a flagbox to green
	 * 
	 * @param analysisType		Anaysis type of the flagbox to change
	 */
	protected void greenFlagUp(String analysisType) {
		changeFlagColor(analysisType, SWT.COLOR_GREEN);
	}
	
	/**
	 * Turns the background color of a flagbox to red
	 * 
	 * @param analysisType		Anaysis type of the flagbox to change
	 */
	protected void redFlagUp(String analysisType) {
		changeFlagColor(analysisType, SWT.COLOR_RED);
	}
	
	/**
	 * Turns the background color of a flagbox
	 * 
	 * @param analysisType		Analysis type of the flagbox to change
	 * @param color				Target color
	 */
	protected void changeFlagColor(final String analysisType, final int color) {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					for(int i=0; i < flagBoxes.length; i++) {
						if(flagBoxes[i].getText().equals(analysisType)) {
							flagBoxes[i].setBackground(display.getSystemColor(color));
						}
					}	
				}
			}
		});
	}

	@Override
	public void setSimStatus(String senderUsername, int thread_count) {
		if (senderUsername.equals(mode)) {
			if(xteamGUISwitch) {
				updateSimStatus(thread_count);
			}
		}
	}
	
	/**
	 * Sets the simulation status
	 * 
	 * @param thread_count	Number of threads running simulation now
	 */
	protected void updateSimStatus(final Integer thread_count) {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					//int color = thread_count == 0 ? SWT.COLOR_GRAY : SWT.COLOR_CYAN;
					labelSimStatusBar.setText(thread_count + " simulations running");
					//labelSimStatusBar.setBackground(display.getSystemColor(color));
				}
			}
		});
	}

	@Override
	public void setInProgressStatus(final String message) {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					labelUpdateStatusBar.setText(message);
					labelUpdateStatusBar.setBackground(display.getSystemColor(SWT.COLOR_RED));
				}
			}
		});
	}

	@Override
	public void setIdleStatus(final String message) {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					labelUpdateStatusBar.setText(message);
					labelUpdateStatusBar.setBackground(display.getSystemColor(SWT.COLOR_GRAY));
				}
			}
		});
	}

	@Override
	public void disableButtonsImpl() {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					buttonCommit.setEnabled(false);
					buttonUpdate.setEnabled(false);
				}
			}
		});
	}

	@Override
	public void enableButtonsImpl() {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					buttonCommit.setEnabled(true);
					buttonUpdate.setEnabled(true);
				}
			}
		});
	}
	
	@Override
	public void disableCommitButtonImpl() {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					buttonCommit.setEnabled(false);
				}
			}
		});
	}
	
	@Override
	public void enableCommitButtonImpl() {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					buttonCommit.setEnabled(true);
				}
			}
		});
	}
	
	@Override
	public void disableUpdateButtonImpl() {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					buttonUpdate.setEnabled(false);
				}
			}
		});
	}
	
	@Override
	public void enableUpdateButtonImpl() {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					buttonUpdate.setEnabled(true);
				}
			}
		});
	}

	@Override
	public void presentLocalInfo() {
		// do nothing
	}
	
	
}
