package flame.client.xteam;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;

import flame.client.FLAMEClient;
import flame.client.FLAMEGUI;
import flame.Constants;
import flame.ScreenLogger;
import flame.client.LocalAnalysisInfo.AnalysisInfo;
import Prism.core.Event;

/**
 * XTEAM GUI presents a GUI window that shows XTEAM analysis information to Architects
 * 
 * @author 					<a href=mailto:jaeyounb@usc.edu>Jae young Bang</a>
 * @version					2013.05
 */
public class XTEAMGUI extends FLAMEGUI {
	
///////////////////////////////////////////////
//Member Variables
///////////////////////////////////////////////
	
	/**
	 * Only MRSV and LocalV will show if this value is true
	 * All of MRSV, LSV, and LocalV will show if this value is false
	 */
	protected final	boolean					twoColumnMode = true;	
	
	/**
	 * InfoTabs that each presents its own type of analysis (e.g. Energy Consumption, Latency ...)
	 */
	protected 		ArrayList<InfoTab> 		tabs;
	
	
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
	public XTEAMGUI (FLAMEClient flameClient, String username, boolean xteamGUISwitch, ScreenLogger screenLogger) {
		super(flameClient, username, xteamGUISwitch, screenLogger);
	}
	
///////////////////////////////////////////////
//Member Methods
///////////////////////////////////////////////
	
	/**
	 * Finds an InfoTab with the same ID and type
	 * 
	 * @param analysisType	Analysis type (e.g. Energy Consumption, Latency ...)
	 * @return				InfoTab found; null if the InfoTab does not exist
	 */
	protected InfoTab getInfoTab(String analysisType) {
		
		for(InfoTab tab : tabs) {
			if(	tab.getAnalisysType().equals(analysisType) ) {
				return tab;
			}
		}
	
		return null;
	}
	
	
	/**
	 * Gets all existing InfoTabs
	 * 
	 * @return				List of InfoTabs
	 */
	/*
	public ArrayList<InfoTab> getAllInfoTabs() {
		return tabs;
	}
	*/
	

	/**
	 * Initializes and draws the GUI window
	 */
	@Override
	protected void initComponents() {
		
		// initializes the tabs
		tabs 					= new ArrayList<InfoTab>();
				
		// Initializes the output window
		display = new Display();
		
		shell = new Shell(display, SWT.TITLE);
		shell.setLayout(new FillLayout());
		
		// reads the screen DPI setting and adjust magnification
		int mag = 1;
		if (shell.getDisplay().getDPI().x > 96) {
			mag = 2;
		}
		
		shell.setText("FLAME GUI");
		shell.setSize(1005 * mag, 670 * mag);
		
		Monitor primary = display.getPrimaryMonitor();
		Rectangle bounds = primary.getBounds();
		Rectangle rect = shell.getBounds();
    		
	    int x = bounds.x + (bounds.width - rect.width) / 2;
	    int y = 0; // bounds.y + (bounds.height - rect.height) / 2;
		
		shell.setLocation(x, y);
		
		TabFolder tabFolder = new TabFolder (shell, SWT.NONE);
		
		// Creates tabs for each XTEAM analysis
		int count = 0;
		for(String key : requirements.getAnalysisTypes()){
			InfoTab tab;
			try {
				tab = new InfoTab(client, count++, key, requirements.getFirstTargetValueName(key), display, shell, mag, tabFolder, xteamGUISwitch, twoColumnMode, sl);
				tabs.add(tab);
			} catch (Exception e) {
				printMsg("No requirement has been defined for analysis type [" + key + "]");
			}
		}
	}
	
	
	
	/**
	 * Presents incoming Snapshot information to the XTEAM GUI
	 * 
	 * @param e
	 */
	@Override
	public void presentSnapshot(final Event e) {
		
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					// get the SenderUsername of the Snapshot
					String senderUsername = "";
					if(e.hasParameter("SenderUsername")) {
						senderUsername = (String) e.getParameter("SenderUsername");
					}
					
					// get the AbsoluteTime of the Snapshot
					String absoluteTime = "";
					if(e.hasParameter("AbsoluteTime")) {
						absoluteTime = (String) e.getParameter("AbsoluteTime");
					}
					
					for(InfoTab tab : tabs) {
						tab.addToSH("@ " + absoluteTime + " by " + senderUsername);
					}
				}
			}
		});
	}
	
	/**
	 * Updates the Latest Versions field
	 */
	@Override
	public void updateLV(final ArrayList<String> lv) {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					for(InfoTab tab : tabs) {
						tab.updateLV(lv);
					}
				}
			}
		});
	}
	
	/**
	 * Presents XTEAM analysis information an incoming Event contains
	 * 
	 * @param e				Incoming Event
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void presentXTEAMInfo(final Event e) {
	
		// Checks if the event is an XTEAM info
		if(!e.name.equals("XTEAM")) {
			return;
		}
		
		// gets the username of the XTEAM Event ... SenderUsername is the mode (MRSV, LSV, LocalV)!
		String mode;
		if(e.hasParameter("SenderUsername")) {
			mode = (String) e.getParameter("SenderUsername");
		} else {
			printMsg("Error: XTEAM Event does not have the SenderUsername parameter");
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
		
		// Gets the XTEAM Engine mode (Energy, Latency, ...).
		String 	analysisType 		= "All";
		if(e.hasParameter("AnalysisType")) {	
			analysisType = (String) e.getParameter("AnalysisType");
		} 
		
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
		
		// if the mode is MRSV == the global engine
		if(mode.equals("MRSV")) {
			if(xteamGUISwitch) {
				updateGlobalSCL(syntactic_conflicts, warnings, arrivalTime);
			}
		} else if (mode.equals(this.username)){
			if(xteamGUISwitch) {
				updateLocalSCL(syntactic_conflicts, warnings, arrivalTime);
			} else {
				// updating Local SCL is postponed when the XTEAM GUI Switch is off (false)
				localInfo.setLocalSCL(syntactic_conflicts, warnings, arrivalTime);
			}
		}

		// only if the analysis does not have any warnings
		if(analysisComplete) {
			try {
				// gets the overall values
				String overall = new String(mode + " @ " + arrivalTime);
				//ArrayList<AnalysisThreshold> targetValue = xteamAnalysisTargetValueType.get(analysisType);
				List<String> targetValueNames = requirements.getTargetValueNames(analysisType);
				
				for(String target : targetValueNames) {
					overall += Constants.endl;
					switch (target.toLowerCase()) {
						case "total":
							if(e.hasParameter("OverallTotal")) {
								overall += (String) e.getParameter("OverallTotal");
							} else {
								overall += "Event lacks OverallTotal attribute!";
							}
							break;
						
						case "maximum":
							if(e.hasParameter("OverallMax")) {
								overall += (String) e.getParameter("OverallMax");
							} else {
								overall += "Event lacks OverallMax attribute!";
							}
							break;
						
						case "average":
							if(e.hasParameter("OverallAverage")) {
								overall += (String) e.getParameter("OverallAverage");
							} else {
								overall += "Event lacks OverallAverage attribute!";
							}
							break;
							
						case "success":
							if(e.hasParameter("OverallSuccess")) {
								overall += (String) e.getParameter("OverallSuccess");
							} else {
								overall += "Event lacks OverallSuccess attribute!";
							}
							break;
						
						default:
							printMsg("Unknown overall target value: " + target);
							break;
					}
				}
				
				// gets the per-component values
				ArrayList<String>	perComponentAnalysis 	= null;
				String target = requirements.getFirstTargetValueName(analysisType).toLowerCase();
				
				switch(target) {
					case "total":
						perComponentAnalysis = getPerComponentValues(e, "PerComponentTotal");
						break;
					
					case "maximum":
						perComponentAnalysis = getPerComponentValues(e, "PerComponentMax");
						break;
					
					case "average":
						perComponentAnalysis = getPerComponentValues(e, "PerComponentAverage");
						break;
						
					case "success":
						perComponentAnalysis = getPerComponentSuccesses(e, "PerComponentSuccess");
						break;
					
					default:
						printMsg("Unknown per-component target value: " + target);
						break;
				}
				
				switch (mode) {
					case "MRSV":
						if(xteamGUISwitch) {
							updateMRSVOverall(analysisType, overall, arrivalTime);
							if(perComponentAnalysis != null) {
								updateListMRSV(analysisType, perComponentAnalysis, arrivalTime);
							}
						}
						break;
						
					case "LSV":
						if(xteamGUISwitch) {
							updateLSVOverall(analysisType, overall, arrivalTime);
							if(perComponentAnalysis != null) {
								updateListLSV(analysisType, perComponentAnalysis, arrivalTime);
							}
						}
						break;
						
					default:	
						// this is LocalV
						if(mode.equals(this.username)) {
							if(xteamGUISwitch) {
								updateLocalVOverall(analysisType, overall, arrivalTime); 
								if(perComponentAnalysis != null) {
									updateListLocalV(analysisType, perComponentAnalysis, arrivalTime);
								}
							} else {
								// updating Local SCL is postponed when the XTEAM GUI Switch is off (false)
								localInfo.setLocalAnalysis(analysisType, overall, perComponentAnalysis, arrivalTime);
							}
						}
						break;
				}
			} catch (Exception exc) {
				printMsg("No requirement has been defined for analysis type [" + analysisType + "]");
			}
		} else {
			switch (mode) {
			case "MRSV":
				if(xteamGUISwitch) {
					outdateMRSVOverall(analysisType, arrivalTime);
				}
				break;
				
			case "LSV":
				if(xteamGUISwitch) {
					outdateLSVOverall(analysisType, arrivalTime);
				}
				break;
				
			default:	
				// this is LocalV
				if(mode.equals(this.username)) {
					if(xteamGUISwitch) {
						outdateLocalVOverall(analysisType, arrivalTime);
					}
				}
				break;
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	protected ArrayList<String> getPerComponentValues(Event e, String attributeName) {
		if (e.hasParameter(attributeName)) {
			ArrayList<String> list = (ArrayList<String>) e.getParameter(attributeName);
			return manipulatePerComponentAnalysis(list);
		} else {
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	protected ArrayList<String> getPerComponentSuccesses(Event e, String attributeName) {
		if (e.hasParameter(attributeName)) {
			ArrayList<String> list = (ArrayList<String>) e.getParameter(attributeName);
			return manipulatePerComponentSuccess(list);
		} else {
			return null;
		}
	}
	
	/**
	 * Manipulates the per-component analysis data to a better-looking format
	 * 
	 * @param data			Received per-component analysis
	 * @return				Manipulated per-component analysis
	 */
	protected ArrayList<String> manipulatePerComponentAnalysis(ArrayList<String> data) {
		ArrayList<String> list = new ArrayList<>();
		
		// finds out what the maximum length of the component values is
		int max_length = 0;
		for(String entry : data) {
			
			// splits the entry
			String[] tokens = entry.split(": ");
			if(tokens.length != 2) {
				printMsg("Error: Unable to parse a component value: " + entry);
				continue;
			}
			
			// parses the component value
			double component_value = 0;
			try {
				component_value = Double.parseDouble(tokens[1]);
			} catch (NumberFormatException nfe) {
				printMsg("Error: Unable to parse a component value: " + entry);
				continue;
			} catch (IllegalArgumentException iae) {
				printMsg("Error: Illegal argument" + entry);
				continue;
			}
			
			// computes the "length" of the value, and finds max.
			Integer component_value_no_below_1 = new Integer((int) component_value);
			int length = component_value_no_below_1.toString().length();
			if(max_length < length) {
				max_length = length;
			}
		}
		
		// actual manipulation happens here
		for(String entry : data) {
			
			// splits the entry
			String[] tokens = entry.split(": ");
			if(tokens.length != 2) {
				printMsg("Error: Unable to parse a component value: " + entry);
				continue;
			}
			
			// parses the component value
			double component_value = 0;
			try {
				component_value = Double.parseDouble(tokens[1]);
			} catch (NumberFormatException nfe) {
				printMsg("Error: Unable to parse a component value: " + entry);
				continue;
			} catch (IllegalArgumentException iae) {
				printMsg("Error: Illegal argument" + entry);
				continue;
			}
			
			// gets the component name
			String component_name = tokens[0].substring(0, tokens[0].length());
			list.add(new String(String.format("%"+(max_length+3)+".2f: ", component_value) + component_name));
			
		}
		
		// sorts the data
		Collections.sort(list, new Comparator<String>() {
		    public int compare(String o1, String o2) {
		        return o2.compareTo(o1);
		    }
		});
		return list;
	}
	
	/**
	 * Manipulates the per-component success data to a better-looking format
	 * 
	 * @param data			Received per-component success analysis
	 * @return				Manipulated per-component success analysis
	 */
	protected ArrayList<String> manipulatePerComponentSuccess(ArrayList<String> data) {
		ArrayList<String> list = new ArrayList<>();
		
		for(String entry : data) {
			
			// splits the entry
			String[] tokens = entry.split(" ");
			if(tokens.length != 3) {
				printMsg("Error: Unable to parse a success value: " + entry);
				continue;
			}
			
			// gets the success information
			String success = tokens[1] + " " + tokens[2];
			
			// gets the component name
			String component_name = tokens[0].substring(0, tokens[0].length()-1);
			
			list.add(new String(success + ": " + component_name));
			
		}
		
		// sorts the data
		Collections.sort(list, new Comparator<String>() {
		    public int compare(String o1, String o2) {
		        return o2.compareTo(o1);
		    }
		});
		return list;
	}
	
	/**
	 * Presents the stored LocalV analysis information<p>
	 * 
	 * This method is invoked when the Check Local button is pressed.
	 */
	@Override
	public void presentLocalInfo() {
		// updates the Local SCL
		updateLocalSCL((ArrayList<String>)localInfo.getConflicts(), localInfo.getWarnings(), localInfo.getArrivalTimeSCL());
		
		// updates the Local analysis
		for(String type : localInfo.getAnalysisTypeMap().keySet()) {
			AnalysisInfo info = localInfo.getAnalysisTypeMap().get(type);
			
			// updates the overall
			updateLocalVOverall(type, info.getOverall(), info.getArrivalTimeAnalysis());
			
			// checks if the per-component analysis is empty
			ArrayList<String> componentAnalysis = (ArrayList<String>) info.getComponentAnalysis();
			if(componentAnalysis.size() > 0) {
				updateListLocalV(type, componentAnalysis, info.getArrivalTimeAnalysis());
			}
		}
	}
	
	
	/**
	 * Updates the Syntactic Conflict List (global)
	 * 
	 * @param analysisType	Analysis type to find the appropriate tab
	 * @param data			List of data to present
	 */
	protected void updateGlobalSCL(final ArrayList<String> data, final Map<String, java.util.List<String>> warnings, final String absoluteTime) {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					for(InfoTab tab : tabs) {
						tab.updateGlobalSCL(data, warnings, absoluteTime);
					}
				}
			}
		});
	}
	
	/**
	 * Updates the Syntactic Conflict List (local)
	 * 
	 * @param analysisType	Analysis type to find the appropriate tab
	 * @param data			List of data to present
	 */
	protected void updateLocalSCL(final ArrayList<String> data, final Map<String, java.util.List<String>> warnings, final String absoluteTime) {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					for(InfoTab tab : tabs) {
						tab.updateLocalSCL(data, warnings, absoluteTime);
					}
				}
			}
		});
	}

	
	/**
	 * Updates the MRSV list
	 * 
	 * @param analysisType	Analysis type to find the appropriate tab
	 * @param data			List of data to present
	 */
	protected void updateListMRSV(final String analysisType, final ArrayList<String> data, final String arrivalTime) {
		
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					for(InfoTab tab : tabs) {
						tab.updateListMRSV(analysisType, data, arrivalTime);
					}
				}
			}
		});
	}
	
	/**
	 * Updates the LSV list
	 * 
	 * @param analysisType	Analysis type to find the appropriate tab
	 * @param data			List of data to present
	 */
	protected void updateListLSV(final String analysisType, final ArrayList<String> data, final String arrivalTime) {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					for(InfoTab tab : tabs) {
						tab.updateListLSV(analysisType, data, arrivalTime);
					}
				}
			}
		});
	}
	
	/**
	 * Update the Syntactic Conflict List (global)
	 * 
	 * @param analysisType	Analysis type to find the appropriate tab
	 * @param data			List of data to present
	 */
	protected void updateListLocalV(final String analysisType, final ArrayList<String> data, final String arrivalTime) {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					for(InfoTab tab : tabs) {
						tab.updateListLocalV(analysisType, data, arrivalTime);
					}
				}
			}
		});
	}
	
	/**
	 * Updates the XTEAM GUI text
	 * 
	 * @param analysisType	Analysis type to find the appropriate tab
	 * @param data			List of data to present
	 */
	protected void updateMRSVOverall(final String analysisType, final String data, final String arrivalTime) {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					for(InfoTab tab : tabs) {
						tab.updateMRSVOverall(analysisType, data, arrivalTime);
					}
				}
			}
		});
	}
	
	/**
	 * Attaches "*OLD*" message at the end of overall analysis result
	 * @param analysisType
	 * @param arrivalTime
	 */
	protected void outdateMRSVOverall(final String analysisType, final String arrivalTime) {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					for(InfoTab tab : tabs) {
						tab.outdateMRSVOverall(analysisType, arrivalTime);
					}
				}
			}
		});
	}
	
	/**
	 * Updates the XTEAM GUI text
	 * 
	 * @param analysisType	Analysis type to find the appropriate tab
	 * @param data			List of data to present
	 */
	protected void updateLSVOverall(final String analysisType, final String data, final String arrivalTime) {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					for(InfoTab tab : tabs) {
						tab.updateLSVOverall(analysisType, data, arrivalTime);
					}
				}
			}
		});
	}
	
	/**
	 * Attaches "*OLD*" message at the end of overall analysis result
	 * @param analysisType
	 * @param arrivalTime
	 */
	protected void outdateLSVOverall(final String analysisType, final String arrivalTime) {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					for(InfoTab tab : tabs) {
						tab.outdateLSVOverall(analysisType, arrivalTime);
					}
				}
			}
		});
	}
	
	/**
	 * Updates the XTEAM GUI text
	 * 
	 * @param analysisType	Analysis type to find the appropriate tab
	 * @param data			List of data to present
	 */
	protected void updateLocalVOverall(final String analysisType, final String data, final String arrivalTime) {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					for(InfoTab tab : tabs) {
						tab.updateLocalVOverall(analysisType, data, arrivalTime);
					}
				}
			}
		});
	}
	
	/**
	 * Attaches "*OLD*" message at the end of overall analysis result
	 * @param analysisType
	 * @param arrivalTime
	 */
	protected void outdateLocalVOverall(final String analysisType, final String arrivalTime) {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					for(InfoTab tab : tabs) {
						tab.outdateLocalVOverall(analysisType, arrivalTime);
					}
				}
			}
		});
	}
	
	/**
	 * Presents the on-going simulation thread number
	 *  
	 * @param senderUsername	From which XTEAM Engine the Notification Event is
	 * @param thread_count		Number of threads
	 */
	@Override
	public void setSimStatus(String senderUsername, int thread_count) {
		if (senderUsername.equals("MRSV")) {
			if(xteamGUISwitch) {
				setMRSVSimStatus(new Integer(thread_count));
			}
		} else if (senderUsername.equals("LSV")) {
			if(xteamGUISwitch) {
				setLSVSimStatus(new Integer(thread_count));
			}
		} else if (senderUsername.equals(username)) {
			setLocalVSimStatus(new Integer(thread_count));
		} else {
			printMsg("Error: senderUsername should be one of MRSV||LSV||username: " + senderUsername);
			return;
		}
	}
	
	/**
	 * Sets the MRSV simulation status
	 * 
	 * @param thread_count	Number of threads running simulation now
	 */
	protected void setMRSVSimStatus(final Integer thread_count) {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					for(InfoTab tab : tabs) {
						tab.setMRSVSimStatus(thread_count);
					}
				}
			}
		});
	}
	
	/**
	 * Sets the LSV simulation status
	 * 
	 * @param thread_count	Number of threads running simulation now
	 */
	protected void setLSVSimStatus(final Integer thread_count) {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					for(InfoTab tab : tabs) {
						tab.setLSVSimStatus(thread_count);
					}
				}
			}
		});
	}
	
	/**
	 * Sets the LocalV simulation status
	 * 
	 * @param thread_count	Number of threads running simulation now
	 */
	protected void setLocalVSimStatus(final Integer thread_count) {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					for(InfoTab tab : tabs) {
						tab.setLocalVSimStatus(thread_count);
					}
				}
			}
		});
	}
	
	/**
	 * Sets the status bar message in cyan
	 * 
	 * @param message		New message
	 */
	@Override
	public void setInProgressStatus(final String message) {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					for(InfoTab tab : tabs) {
						tab.setInProgressStatus(message);
					}
				}
			}
		});
	}
	
	/**
	 * Sets the status bar message in gray
	 * 
	 * @param message		New message
	 */
	@Override
	public void setIdleStatus(final String message) {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					for(InfoTab tab : tabs) {
						tab.setIdleStatus(message);
					}
				}
			}
		});
	}
	
	/**
	 * Disable commit/update buttons
	 */
	@Override
	public void disableButtonsImpl() {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					for(InfoTab tab : tabs) {
						tab.disableSnapshotButton();
						tab.disableCheckButton();
					}
				}
			}
		});
	}
	
	/**
	 * Enable commit/update buttons
	 */
	@Override
	public void enableButtonsImpl() {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					for(InfoTab tab : tabs) {
						tab.enableSnapshotButton();
						if(!xteamGUISwitch) {
							tab.enableCheckButton();
						}
					}
				}
			}
		});
	}
	
	@Override
	public void disableCommitButtonImpl() {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					for(InfoTab tab : tabs) {
						tab.disableSnapshotButton();
					}
				}
			}
		});
	}
	
	@Override
	public void enableCommitButtonImpl() {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					for(InfoTab tab : tabs) {
						tab.enableSnapshotButton();
					}
				}
			}
		});
	}
	
	@Override
	public void disableUpdateButtonImpl() {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					for(InfoTab tab : tabs) {
						tab.disableCheckButton();
					}
				}
			}
		});
	}
	
	@Override
	public void enableUpdateButtonImpl() {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					for(InfoTab tab : tabs) {
						if(!xteamGUISwitch) {
							tab.enableCheckButton();
						}
					}
				}
			}
		});
	}
}
