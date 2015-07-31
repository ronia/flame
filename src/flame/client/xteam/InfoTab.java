package flame.client.xteam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import flame.Constants;
import flame.ScreenLogger;
import flame.client.FLAMEClient;

/**
 * InfoTab is an XTEAM GUI tab that presents XTEAM analysis information for the given type of analysis
 * 
 * @author 					<a href=mailto:jaeyounb@usc.edu>Jae young Bang</a>
 * @version					2013.05
 */
public class InfoTab {
	
///////////////////////////////////////////////
//Member Variables
///////////////////////////////////////////////
	
	/**
	 * Only MRSV and LocalV will show if this value is true
	 * All of MRSV, LSV, and LocalV will show if this value is false
	 */
	protected 	boolean				twoColumnMode;	
	
	/**
	 * Display that the XTEAMGUI uses
	 */
	protected	Display				display;
	
	/**
	 * FLAMEClient instance to invoke the Snapshot and Update methods
	 */
	protected	FLAMEClient		 	c;
	
	/**
	 * ScreenLogger instance
	 */
	protected	ScreenLogger		sl;

	/**
	 * Analysis ID
	 */
	protected 	int					analysis_id;
	
	/**
	 * Analysis type (e.g. Energy Consumption, Latency ...)
	 */
	protected 	String				analysis_type;
	
	/**
	 * Tab to be presented
	 */
	protected 	TabItem 			tabItem;
	
	/**
	 * Group that takes the whole tab
	 */
	protected 	Group				group;

	/**
	 * List of Snapshot numbers
	 */
	protected 	List 				listLV;
	
	/**
	 * List of Snapshot history
	 */
	protected	List 				listSH;
	
	/**
	 * Table that presents list of global syntactic errors
	 */
	protected 	Table 				tableGlobalSCL;
	
	/**
	 * Table that presents list of local syntactic errors
	 */
	protected	Table				tableLocalSCL;

	/**
	 * Global syntactic error label -- to present the absolute time of the analysis
	 */
	protected	Label 				labelGlobalSCLTitle;
	
	/**
	 * Local syntactic error label -- to present the absolute time of the analysis
	 */
	protected	Label				labelLocalSCLTitle;
	
	/**
	 * Label that presents MRSV Overall analysis 
	 */
	protected 	Label 				labelMRSVOverall;
	
	/**
	 * Label that presents LSV Overall analysis
	 */
	protected 	Label 				labelLSVOverall;
	
	/**
	 * Label that presents LocalV Overall analysis
	 */
	protected 	Label 				labelLocalVOverall;
	
	/**
	 * List that presents MRSV per-element analysis
	 */
	protected 	List 				listMRSV;
	
	/**
	 * List that presents LSV per-element analysis
	 */
	protected 	List 				listLSV;
	
	/**
	 * List that presents LocalV per-element analysis
	 */
	protected 	List 				listLocalV;
	
	/**
	 * Label that presents the on-going simulations at the MRSV engine
	 */
	protected	Label				labelMRSVSimStatus;
	
	/**
	 * Label that presents the on-going simulations at the LSV engine
	 */
	protected	Label				labelLSVSimStatus;
	
	/**
	 * Label that presents the on-going simulations at the LocalV engine
	 */
	protected	Label				labelLocalVSimStatus;
	
	/**
	 * Status bar Label
	 */
	protected 	Label				labelStatusBar;
	
	/**
	 * Switch that turns on and off the XTEAM GUI window for experimental purposes
	 */
	protected	boolean				xteamGUISwitch;
	
	/**
	 * Button "Local Check"
	 */
	protected	Button				buttonCheck;
	
	/**
	 * Button "Commit"
	 */
	protected	Button				buttonSnapshot;
	
	
	protected	String				arrivalTime_MRSV_overall			= "00000000_0000_00";
	protected	String				arrivalTime_LSV_overall				= "00000000_0000_00";
	protected	String				arrivalTime_LocalV_overall			= "00000000_0000_00";
	
	protected	String				arrivalTime_MRSV_per_component		= "00000000_0000_00";
	protected	String				arrivalTime_LSV_per_component		= "00000000_0000_00";
	protected	String				arrivalTime_LocalV_per_component	= "00000000_0000_00";
	
	protected	String				arrivalTime_GlobalSCL				= "00000000_0000_00";
	protected	String				arrivalTime_LocalSCL				= "00000000_0000_00";
	
	
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
	 * Default constructor
	 * 
	 * @param client			FLAMEClient instance
	 * @param analysis_id		Analysis ID
	 * @param analysis_type		Analysis type (e.g. Energy Consumption, Latency ...)
	 * @param value_type		Analysis value type (e.g. Average, Maximum, Total, Success, ... )
	 * @param display			XTEAM GUI Display
	 * @param mag				Magnification multiplier
	 * @param tabFolder			TabFolder that this tab goes into
	 * @param xteamGUISwitch	Enable/disable some fields for experimental purposes
	 * @param twoColumnMode		Hide the LSV fields when true
	 * @param screenLogger		ScreenLogger instance
	 */
	public InfoTab(		FLAMEClient		 	client,
						int 				analysis_id, 	
						String 				analysis_type,
						String				value_type,
						Display				display,
						final Shell			shell,
						int					mag,
						TabFolder 			tabFolder,
						boolean				xteamGUISwitch,
						boolean				twoColumnMode,
						ScreenLogger 		screenLogger) {
		
		String 	font_face		= "Courier New";
		int		font_size		= 9;
		
		mLockGUI 				= new Semaphore (1, true);
		
		c						= client;
		sl						= screenLogger;
		this.display			= display;
		
		this.analysis_id 		= analysis_id;
		this.analysis_type		= analysis_type;
		
		this.xteamGUISwitch		= xteamGUISwitch;
		this.twoColumnMode		= twoColumnMode;
		
		int x_size 	= 235;			// horizontal size
		int x_gap	= 10;			// horizontal gap
		
		int y_size	= 16;
		
		String tabName = analysis_type;
		String groupName = analysis_type + " Analysis";
		
		// Initialize the tab
		tabItem = new TabItem (tabFolder, SWT.NULL);
		tabItem.setText(tabName);
		
		// create a group that covers the entire tab
		group = new Group(tabFolder, SWT.NONE);
		group.setText(groupName);
		
		// XTEAM GUI switched off message
		String switchedOffMsg = "DISABLED BY CONFIGURATION";
		
		
		// last versions		
		Label labelLVTitle = new Label(group, SWT.NONE);
		labelLVTitle.setText("Commit counts:");
		labelLVTitle.setSize(x_size * mag, 20 * mag);
		labelLVTitle.setLocation(x_gap * mag, 35 * mag);
		
		listLV = new List(group, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		listLV.setFont(new Font(display, font_face, font_size, SWT.NONE));
		listLV.setSize(x_size * mag, 60 * mag);
		listLV.setLocation(x_gap * mag, 55 * mag);
		
		// Snapshot history
		Label labelSHTitle = new Label(group, SWT.NONE);
		labelSHTitle.setText("Commit history:");
		labelSHTitle.setSize(x_size * mag, 20 * mag);
		labelSHTitle.setLocation(x_gap * mag, 120 * mag);
		
		listSH = new List(group, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		listSH.setFont(new Font(display, font_face, font_size, SWT.NONE));
		listSH.setSize(x_size * mag, 150 * mag);
		listSH.setLocation(x_gap * mag, 140 * mag);
		
		/////////////////////////////////////////////////////////////////////
		// MRSV Fields
		/////////////////////////////////////////////////////////////////////
		
		// MRSV overall
		Label labelMRSVTitle = new Label(group, SWT.NONE);
		labelMRSVTitle.setText("Most recent version:");
		labelMRSVTitle.setSize(x_size * mag, 20 * mag);
		labelMRSVTitle.setLocation((x_gap * 2 + x_size) * mag, 35 * mag);
		
		labelMRSVOverall = new Label(group, SWT.BORDER);
		labelMRSVOverall.setBackground(new Color(display, new RGB(230,230,230)));
		labelMRSVOverall.setFont(new Font(display, font_face, font_size, SWT.NONE));
		labelMRSVOverall.setSize(x_size * mag, 60 * mag);
		labelMRSVOverall.setLocation((x_gap * 2 + x_size) * mag, 55 * mag);
		if(!xteamGUISwitch) { 
			labelMRSVOverall.setText(switchedOffMsg);
			labelMRSVOverall.setEnabled(false);
		}
		
		// MRSV element list
		Label labelMRSVelement = new Label(group, SWT.NONE);
		labelMRSVelement.setText(value_type + " " + analysis_type.toLowerCase() + " values:");
		labelMRSVelement.setSize(x_size * mag, 20 * mag);
		labelMRSVelement.setLocation((x_gap * 2 + x_size) * mag, 120 * mag);
		
		listMRSV = new List(group, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		listMRSV.setFont(new Font(display, font_face, font_size, SWT.NONE));
		listMRSV.setSize(x_size * mag, (150-y_size) * mag);
		listMRSV.setLocation((x_gap * 2 + x_size) * mag, 140 * mag);
		if(!xteamGUISwitch) { 
			listMRSV.add(switchedOffMsg);
			listMRSV.setEnabled(false);
		}
		
		// MRSV simulation status
		labelMRSVSimStatus = new Label(group, SWT.BORDER_SOLID);
		labelMRSVSimStatus.setText("MRSV Sim Stat");
		labelMRSVSimStatus.setSize(x_size * mag, y_size * mag);
		labelMRSVSimStatus.setLocation((x_gap * 2 + x_size) * mag, (290-y_size) * mag);
		labelMRSVSimStatus.setBackground(display.getSystemColor(SWT.COLOR_GRAY));
		
		/////////////////////////////////////////////////////////////////////
		// LSV Fields
		/////////////////////////////////////////////////////////////////////
		
		// LSV overall
		Label labelLSVTitle = new Label(group, SWT.NONE);
		labelLSVTitle.setText("Last committed version:");
		labelLSVTitle.setSize(x_size * mag, 20 * mag);
		labelLSVTitle.setLocation((x_size * 2 + x_gap * 3) * mag, 35 * mag);
		
		labelLSVOverall = new Label(group, SWT.BORDER);
		labelLSVOverall.setBackground(new Color(display, new RGB(230,230,230)));
		labelLSVOverall.setFont(new Font(display, font_face, font_size, SWT.NONE));
		labelLSVOverall.setSize(x_size * mag, 60 * mag);
		labelLSVOverall.setLocation((x_size * 2 + x_gap * 3) * mag, 55 * mag);
		if(!xteamGUISwitch) { 
			labelLSVOverall.setText(switchedOffMsg);
			labelLSVOverall.setEnabled(false);
		}
		
		// LSV element list
		Label labelLSVelement = new Label(group, SWT.NONE);
		labelLSVelement.setText(value_type + " " + analysis_type.toLowerCase() + " values:");
		labelLSVelement.setSize(x_size * mag, 20 * mag);
		labelLSVelement.setLocation((x_size * 2 + x_gap * 3) * mag, 120 * mag);
		
		listLSV = new List(group, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		listLSV.setFont(new Font(display, font_face, font_size, SWT.NONE));
		listLSV.setSize(x_size * mag, (150-y_size) * mag);
		listLSV.setLocation((x_size * 2 + x_gap * 3) * mag, 140 * mag);
		if(!xteamGUISwitch) { 
			listLSV.add(switchedOffMsg);
			listLSV.setEnabled(false);
		}
		
		// LSV simulation status
		labelLSVSimStatus = new Label(group, SWT.BORDER_SOLID);
		labelLSVSimStatus.setText("LSV Sim Stat");
		labelLSVSimStatus.setSize(x_size * mag, y_size * mag);
		labelLSVSimStatus.setLocation((x_size * 2 + x_gap * 3) * mag, (290-y_size) * mag);
		labelLSVSimStatus.setBackground(display.getSystemColor(SWT.COLOR_GRAY));
		
		/////////////////////////////////////////////////////////////////////
		// LocalV Fields
		/////////////////////////////////////////////////////////////////////

		// LocalV overall
		Label labelLocalVTitle = new Label(group, SWT.NONE);
		labelLocalVTitle.setText("Local version:");
		labelLocalVTitle.setSize(x_size * mag, 20 * mag);
		labelLocalVTitle.setLocation((x_size * 3 + x_gap * 4) * mag, 35 * mag);
		
		labelLocalVOverall = new Label(group, SWT.BORDER);
		labelLocalVOverall.setBackground(new Color(display, new RGB(230,230,230)));
		labelLocalVOverall.setFont(new Font(display, font_face, font_size, SWT.NONE));
		labelLocalVOverall.setSize(x_size * mag, 60 * mag);
		labelLocalVOverall.setLocation((x_size * 3 + x_gap * 4) * mag, 55 * mag);
		
		// LocalV element list
		Label labelLocalVelement = new Label(group, SWT.NONE);
		labelLocalVelement.setText(value_type + " " + analysis_type.toLowerCase() + " values:");
		labelLocalVelement.setSize(x_size * mag, 20 * mag);
		labelLocalVelement.setLocation((x_size * 3 + x_gap * 4) * mag, 120 * mag);
		
		listLocalV = new List(group, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		listLocalV.setFont(new Font(display, font_face, font_size, SWT.NONE));
		listLocalV.setSize(x_size * mag, (150-y_size) * mag);
		listLocalV.setLocation((x_size * 3 + x_gap * 4) * mag, 140 * mag);
		
		// LocalV simulation status
		labelLocalVSimStatus = new Label(group, SWT.BORDER_SOLID);
		labelLocalVSimStatus.setText("LocalV Sim Stat");
		labelLocalVSimStatus.setSize(x_size * mag, y_size * mag);
		labelLocalVSimStatus.setLocation((x_size * 3 + x_gap * 4) * mag, (290-y_size) * mag);
		labelLocalVSimStatus.setBackground(display.getSystemColor(SWT.COLOR_GRAY));
		
		/////////////////////////////////////////////////////////////////////
		// Syntactic Conflict Fields
		/////////////////////////////////////////////////////////////////////
		
		// Global syntactic conflict list
		labelGlobalSCLTitle = new Label(group, SWT.NONE);
		labelGlobalSCLTitle.setText("Global syntax errors (from MRV)");
		labelGlobalSCLTitle.setSize((x_size*4 + x_gap*3) * mag, 20 * mag);
		labelGlobalSCLTitle.setLocation(x_gap * mag, 295 * mag);
		
		tableGlobalSCL = new Table(group, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		tableGlobalSCL.setFont(new Font(display, font_face, font_size, SWT.NONE));
		tableGlobalSCL.setSize((x_size*4 + x_gap*3) * mag, 100 * mag);
		tableGlobalSCL.setLocation(x_gap * mag, 315 * mag);
		if(!xteamGUISwitch) {
			TableItem item = new TableItem(tableGlobalSCL, SWT.None);
			item.setText(switchedOffMsg);
			tableGlobalSCL.setEnabled(false);
		}
		
		// Local syntactic conflict list
		labelLocalSCLTitle = new Label(group, SWT.NONE);
		labelLocalSCLTitle.setText("Local syntax errors (from LocalV)");
		labelLocalSCLTitle.setSize((x_size*4 + x_gap*3) * mag, 20 * mag);
		labelLocalSCLTitle.setLocation(x_gap * mag, 425 * mag);
		
		tableLocalSCL = new Table(group, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		tableLocalSCL.setFont(new Font(display, font_face, font_size, SWT.NONE));
		tableLocalSCL.setSize((x_size*4 + x_gap*3) * mag, 100 * mag);
		tableLocalSCL.setLocation(x_gap * mag, 445 * mag);
		
		// commit button
		buttonSnapshot = new Button(group, SWT.PUSH);
		buttonSnapshot.setLocation(x_gap * mag, 560 * mag);
		buttonSnapshot.setSize(130 * mag, 23 * mag);
		buttonSnapshot.setText("Commit");
		
		buttonSnapshot.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				printMsg("Commit button pressed");
				c.commit();
			}
		});
		
		// Update button
		Button buttonUpdate = new Button(group, SWT.PUSH);
		buttonUpdate.setLocation((x_gap * 2 + 130) * mag, 560 * mag);
		buttonUpdate.setSize(130 * mag, 23 * mag);
		buttonUpdate.setText("Update");
		
		buttonUpdate.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				printMsg("Update button pressed");
				c.update();
			}
		});
		
		// Exit button
		Button buttonExit = new Button(group, SWT.PUSH);
		buttonExit.setLocation((x_gap * 3 + 130 * 2) * mag, 560 * mag);
		buttonExit.setSize(130 * mag, 23 * mag);
		buttonExit.setText("Exit");
		
		buttonExit.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				printMsg("Exit button pressed");
				c.getModelingTool().destroy();
				shell.dispose();
			}
		});
		
		
		// Check Local button
		/*
		buttonCheck = new Button(group, SWT.PUSH);
		buttonCheck.setLocation(x_gap * 2 + 130, 660);
		buttonCheck.setSize(130, 23);
		buttonCheck.setText("Check Local");
		if(xteamGUISwitch) {
			disableCheckButton();
		}
		
		buttonCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				printMsg("Check Local button pressed");
				c.presentLocalInfo();
			}
		});
		*/
		
		// Status bar
		labelStatusBar = new Label(group, SWT.NONE);
		labelStatusBar.setText("Ready");
		labelStatusBar.setSize(1000 * mag, 20 * mag);
		labelStatusBar.setLocation(0 * mag, 598 * mag);
		labelStatusBar.setBackground(display.getSystemColor(SWT.COLOR_GRAY));
		
		tabItem.setControl(group);
	}
	
	
///////////////////////////////////////////////
//Member Methods
///////////////////////////////////////////////
	
	public 		int 		getAnalisysID() 	{ return analysis_id; }
	public 		String		getAnalisysType() 	{ return analysis_type; }
	
	// Data across all analysis results
	public		void		updateLV			(ArrayList<String> data) 	{ updateList(listLV, data); }
	public		void		updateSH			(ArrayList<String> data) 	{ updateList(listSH, data); }
	public		void		addToSH				(String data) 				{ addToList(listSH, data); }
	public		void		updateGlobalSCL		(ArrayList<String> data, Map<String, java.util.List<String>> warnings, String arrivalTime) 	{

		// if the new data is from the past, throw it away
		if(arrivalTime_GlobalSCL.compareTo(arrivalTime) < 0) {
			
			getLock();
			
			arrivalTime_GlobalSCL = arrivalTime;
			ArrayList<String> scl = manipulateSCL(data, warnings);
			updateTable(tableGlobalSCL, scl);
			labelGlobalSCLTitle.setText("Global syntax errors (from MRV) @ " + arrivalTime);
			highlightGSCL();
			
			releaseLock();
		}
		
	}
	
	public		void		updateLocalSCL		(ArrayList<String> data, Map<String, java.util.List<String>> warnings, String arrivalTime) 	{
		
		// if the new data is from the past, throw it away
		if(arrivalTime_LocalSCL.compareTo(arrivalTime) < 0) {
			getLock();
			
			arrivalTime_LocalSCL = arrivalTime;
			ArrayList<String> scl = manipulateSCL(data, warnings);
			updateTable(tableLocalSCL, scl);
			labelLocalSCLTitle.setText("Local syntax errors (from LocalV) @ " + arrivalTime);
			if(xteamGUISwitch) {
				highlightGSCL();
			}
			
			releaseLock();
		}	
	}
	
	/**
	 * Adds warning messages to the errors list and sorts it
	 * 
	 * @param data			The errors (conflicts) list
	 * @param warnings		The warning messages
	 * @return				Sorted List of errors and warnings
	 */
	protected	ArrayList<String> manipulateSCL(ArrayList<String> data, Map<String, java.util.List<String>> warnings) {
		
		// creates a new list with the errors
		ArrayList<String> ret = new ArrayList<>();
		ret.addAll(data);
		
		// only when there is warning messages
		if(warnings != null) {
			// adds warning messages to the list
			ArrayList<String> warningsForThisTab = (ArrayList<String>) warnings.get(analysis_type);
			if(warningsForThisTab != null) {
				for(String entry : warningsForThisTab) {
					String newConflict 	= "[WRN]:[" + analysis_type + "]:";
					
					// pads the new conflict string with spaces
					for(int i=0; i < 18 - newConflict.length(); i++) {
						newConflict += " ";
					}
					newConflict += entry;
					
					ret.add(newConflict);
				}		
			}
		}
		
		if(ret.size() == 0) {
			// adds "None" if the list is empty
			ret.add("None");
		} else {
			// sorts the list
			Collections.sort(ret);
		}
		
		return ret;
	}
	
	/**
	 * highlights elements in Global SCL that also appear in Local SCL
	 */
	protected	void		highlightGSCL		() {
		for(int i=0; i < tableGlobalSCL.getItemCount(); i++) {
			// gets the current TableItem
			TableItem globalConflict = tableGlobalSCL.getItem(i);
			
			boolean found = false;
			
			// iterates through the local conflicts
			for(TableItem localConflict : tableLocalSCL.getItems()) {
				// compares a local conflict and a global conflict
				if(localConflict.getText().equals(globalConflict.getText())) {
					found = true;
					break;
				}
			}
			
			if(found) {
				// colors gray if a match
				setForegroundColor(globalConflict, SWT.COLOR_GRAY);
			} else {
				// colors black if not a match
				setForegroundColor(globalConflict, SWT.COLOR_BLACK);
			}
		}
	}
	
	/**
	 * Sets background color of a TableItem
	 * 
	 * @param item			TableItem to change the background color
	 * @param color			Target color
	 */
	protected	void		setBackgroundColor(TableItem item, int color) {
		item.setBackground(display.getSystemColor(color));
	}
	
	protected	void		setForegroundColor(TableItem item, int color) {
		item.setForeground(display.getSystemColor(color));
	}
	
	
	// Analysis-type-specific results
	public		void		updateMRSVOverall	(String analysisType, String data, String arrivalTime) {
		if(check(analysisType) && arrivalTime_MRSV_overall.compareTo(arrivalTime) < 0) { 
			getLock();
			arrivalTime_MRSV_overall = arrivalTime;
			updateLabel(labelMRSVOverall, data);
			releaseLock();
		}
	}
	
	public		void		outdateMRSVOverall	(String analysisType, String arrivalTime) {
		if(check(analysisType) && arrivalTime_MRSV_overall.compareTo(arrivalTime) < 0) {
			getLock();
			outdateLabel(labelMRSVOverall);
			releaseLock();
		}
	}
	
	public		void		updateLSVOverall	(String analysisType, String data, String arrivalTime) { 
		if(check(analysisType) && arrivalTime_LSV_overall.compareTo(arrivalTime) < 0) {
			getLock();
			arrivalTime_LSV_overall = arrivalTime;
			updateLabel(labelLSVOverall, data); 
			releaseLock();
		}
	}
	
	public		void		outdateLSVOverall	(String analysisType, String arrivalTime) {
		if(check(analysisType) && arrivalTime_LSV_overall.compareTo(arrivalTime) < 0) {
			getLock();
			outdateLabel(labelLSVOverall);
			releaseLock();
		}
	}
	
	public		void		updateLocalVOverall	(String analysisType, String data, String arrivalTime) { 
		if(check(analysisType) && arrivalTime_LocalV_overall.compareTo(arrivalTime) < 0) {
			getLock();
			arrivalTime_LocalV_overall = arrivalTime;
			updateLabel(labelLocalVOverall, data);
			releaseLock();
		}
	}
	
	public		void		outdateLocalVOverall (String analysisType, String arrivalTime) {
		if(check(analysisType) && arrivalTime_LocalV_overall.compareTo(arrivalTime) < 0) {
			getLock();
			outdateLabel(labelLocalVOverall);
			releaseLock();
		}
	}
	
	public		void		updateListMRSV		(String analysisType, ArrayList<String> data, String arrivalTime) { 
		if(check(analysisType) && arrivalTime_MRSV_per_component.compareTo(arrivalTime) < 0) {
			getLock();
			arrivalTime_MRSV_per_component = arrivalTime;
			updateList(listMRSV, data); 
			releaseLock();
		}
	}
	
	public		void		updateListLSV		(String analysisType, ArrayList<String> data, String arrivalTime) { 
		if(check(analysisType) && arrivalTime_LSV_per_component.compareTo(arrivalTime) < 0) {
			getLock();
			arrivalTime_LSV_per_component = arrivalTime;
			updateList(listLSV, data);
			releaseLock();
		}
	}
	
	public		void		updateListLocalV	(String analysisType, ArrayList<String> data, String arrivalTime) { 
		if(check(analysisType) && arrivalTime_LocalV_per_component.compareTo(arrivalTime) < 0) {
			getLock();
			arrivalTime_LocalV_per_component = arrivalTime;
			updateList(listLocalV, data);
			releaseLock();
		}
	}
	
	/**
	 * Checks if the analysis type matches this InfoTab
	 * 
	 * @param analysisType	Analysis type
	 * @return				True if matches, false otherwise
	 */
	private		boolean		check (String analysisType) {
		if(this.analysis_type.equals(analysisType) || analysisType.toLowerCase().equals("all")) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Updates a Label with the new data
	 * @param label			Target Label
	 * @param data			New data
	 */
	protected	void				updateLabel(Label label, String data) {
		label.setText(data);
	}
	
	/**
	 * Adds *OLD* message at the end of a label's first line of text
	 * 
	 * @param label			Target Label
	 */
	protected	void				outdateLabel(Label label) {
		String 		originalText 	= label.getText();
		if(originalText.length() == 0) {
			return;
		}
		
		String[] 	tokens			= originalText.split(Constants.endl);
		String		phrase			= "*OLD*";
		
		// checks if the label has any text
		if(tokens.length > 0) {
			// checks if the first line already has the *OLD* phrase at the end
			String[] words = tokens[0].trim().split(" ");
			if(words.length > 0 && !words[words.length - 1].equals(phrase)) {
				tokens[0] += " " + phrase;
			}
		} else {
			return;
		}
		
		String newText = new String();
		for(String token : tokens) {
			newText += token + Constants.endl;
		}
		
		label.setText(newText);
	}
	
	/**
	 * Updates a List with the new data
	 * @param list			Target List
	 * @param data			New data
	 */
	protected	void				updateList(List list, ArrayList<String> data) {
		list.setItems(data.toArray(new String[data.size()]));
	}

	
	/**
	 * Updates a Table with the new data
	 * @param table			Target Table
	 * @param data			New data
	 */
	protected	void				updateTable(Table table, ArrayList<String> new_data) {
		
		// old items existing in the table
		TableItem[] old_table_items = table.getItems();
		
		// if the old data is empty
		if(old_table_items.length == 0) {
			for(String str : new_data) {
				TableItem item = new TableItem(table, SWT.None);
				item.setText(str);
			}
			return;
		}
		
		/////////////////////////////////////////////////////////////
		// removes items that do not exist in the new data
		/////////////////////////////////////////////////////////////
		
		ArrayList<Integer> items_to_remove = new ArrayList<>();
		try {
			for(TableItem item : old_table_items) {
				boolean found = false;
	
				for(String new_item : new_data) {
					if(item.getText().equals(new_item)) {
						found = true;
						break;
					}
				}
				
				if(found == false) {
					items_to_remove.add(table.indexOf(item));
				}
			}
			
			if(items_to_remove.size() > 0) {
				// converts the index arraylist into an int array
				int[] indexes = new int[items_to_remove.size()];
				for(int i=0; i < items_to_remove.size(); i++) {
					indexes[i] = items_to_remove.get(i).intValue();
				}
				
				// removes
				table.remove(indexes);
			}
		} catch (Exception e) {
			printMsg("Error: Failed to remove items from old data: " + e);
		}
		
		
		/////////////////////////////////////////////////////////////
		// adds new items
		/////////////////////////////////////////////////////////////

		int cursor_new_data	= 0;
		int cursor_old_data	= 0;
		int size_new_data	= new_data.size();
		
		try {
			// iterates through the new data
			while (cursor_new_data < size_new_data) {
				
				old_table_items = table.getItems();
				int size_old_data	= table.getItemCount();
				String current_new_data = new_data.get(cursor_new_data);
				
				if(cursor_old_data >= size_old_data) {
					// no more old item: add the new item at the end of the old data
					TableItem item = new TableItem(table, SWT.None, cursor_old_data);
					item.setText(new_data.get(cursor_new_data));
					cursor_new_data++;
				} else {
					// at least one more old item: the old item at the cursor
					String current_old_data = old_table_items[cursor_old_data].getText();
					
					// compares the two strings
					int comparison = current_new_data.compareTo(current_old_data);
					
					if(comparison < 0) {
						// if the new item precedes
						TableItem item = new TableItem(table, SWT.None, cursor_old_data);
						item.setText(new_data.get(cursor_new_data));
						cursor_new_data++;
					} else if (comparison == 0) {
						// if the current_new_data string equals
						cursor_new_data++;
						cursor_old_data++;
						
					} else {
						// if the current_new_data string follows
						cursor_old_data++;
					}
				}
			}
		} catch (Exception e) {
			printMsg("Error: Failed to add a new item to the table: " + e);
		}

		
		/*
		table.removeAll();
		for(String str : data) {
			TableItem item = new TableItem(table, SWT.None);
			item.setText(str);
		}
		*/
	}
	
	/**
	 * Adds an item to a List
	 * @param list			Target List
	 * @param data			New entry
	 */
	protected	void				addToList(List list, String data) {
		list.add(data);
		
		String[] items = list.getItems();
		Arrays.sort(items, Collections.reverseOrder());
		
		list.setItems(items);
	}
	
	/**
	 * Sets the MRSV simulation status message
	 * 
	 * @param thread_count	Number of threads running simulation now
	 */
	public		void				setMRSVSimStatus(Integer thread_count) {
		int color = thread_count == 0 ? SWT.COLOR_GRAY : SWT.COLOR_CYAN;
		setStatus(labelMRSVSimStatus, thread_count + " simulations running", color);
	}
	
	/**
	 * Sets the LSV simulation status message
	 * 
	 * @param thread_count	Number of threads running simulation now
	 */
	public		void				setLSVSimStatus(Integer thread_count) {
		int color = thread_count == 0 ? SWT.COLOR_GRAY : SWT.COLOR_CYAN;
		setStatus(labelLSVSimStatus, thread_count + " simulations running", color);
	}
	
	/**
	 * Sets the LocalV simulation status message
	 * 
	 * @param thread_count	Number of threads running simulation now
	 */
	public		void				setLocalVSimStatus(Integer thread_count) {
		int color = thread_count == 0 ? SWT.COLOR_GRAY : SWT.COLOR_CYAN;
		setStatus(labelLocalVSimStatus, thread_count + " simulations running", color);
	}
	
	/**
	 * Sets the status bar message in cyan
	 * 
	 * @param message		New message
	 */
	public 		void				setInProgressStatus(String message) {
		setStatus(labelStatusBar, message, SWT.COLOR_CYAN);
	}
	
	/**
	 * Sets the status bar message in gray
	 * 
	 * @param message		New message
	 */
	public		void				setIdleStatus(String message) {
		setStatus(labelStatusBar, message, SWT.COLOR_GRAY);
	}
	
	/**
	 * Sets the status bar message with given background color
	 * 
	 * @param message		New message
	 * @param color			Target color
	 */
	protected	void				setStatus(Label label, String message, int color) {
		label.setText(message);
		label.setBackground(display.getSystemColor(color));
	}
	
	
	public		void				disableSnapshotButton() {
		buttonSnapshot.setEnabled(false);
	}
	
	public		void				disableCheckButton() {
		buttonCheck.setEnabled(false);
	}
	
	public		void				enableSnapshotButton() {
		buttonSnapshot.setEnabled(true);
	}
	
	public		void				enableCheckButton() {
		buttonCheck.setEnabled(true);
	}
	
	/**
	 * Print screen messages
	 * 
	 * @param msg			Message to print to screen
	 */
	private void printMsg(String msg) {
		sl.printMsg("InfoTab(" + analysis_type + ")", msg);
	}
}
