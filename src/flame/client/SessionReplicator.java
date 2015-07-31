package flame.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Text;

import flame.AbstractImplementationModified;
import flame.Constants;
import flame.Utility;
import Prism.core.Architecture;
import Prism.core.Component;
import Prism.core.Connector;
import Prism.core.Event;
import Prism.core.FIFOScheduler;
import Prism.core.Port;
import Prism.core.PrismConstants;
import Prism.core.RRobinDispatcher;
import Prism.core.Scaffold;
import Prism.extensions.port.ExtensiblePort;
import Prism.extensions.port.distribution.SocketDistribution;

/**
 * SessionReplicator is a client instance of FLAME that reads a FLAME log file and recreates 
 * the scenario by manipulating and pushing the logged FLAME events to the FLAME Server <p>
 * 
 * @author					<a href="mailto:aalotaib@usc.edu">Ali Saleh T Alotaibi</a>
 * @author 					<a href="mailto:jaeyounb@usc.edu">Jae young Bang</a>
 * @version					2015.3
 */
public class SessionReplicator extends AbstractImplementationModified {
	
///////////////////////////////////////////////////////////
// Member variables
///////////////////////////////////////////////////////////

	protected		Display					display;
	protected 		Shell 					shell;
	protected 		String 					file_path			= "";
	protected 		Text 					textboxFilePath;
	protected		Label					labelConsole;	
	
	/**
	 * Usernames of the logged-in architects
	 */
	protected		List<String> 			usernames			= new ArrayList<>();
	
	/**
	 * Extensions of files that will be presented to the user
	 */
	private 	static final String[] 	FILTER_EXTS 		= {"*.csv", "*.*"};
	
	/**
	 * A logged FLAME Event with time and the event
	 * 
	 * @author 					<a href="mailto:jaeyounb@usc.edu">Jae young Bang</a>
	 * @version					2014.12
	 */
	protected	class LogEvent {
		private long time;
		private Event event;
		
		public LogEvent(long time, Event event) {
			this.time	 	= time;
			this.event		= event;
		}
		
		public long getTime() {
			return time;
		}
		
		public Event getEvent() {
			return event;
		}
		
		public LogEvent duplicate() {
			LogEvent newEvent = new LogEvent(time, event);
			return newEvent;
		}
	}
	
	private 	List<LogEvent>	 		events 		= new ArrayList<>();
	

	/**
	 * Configuration properties read from the config.properties file
	 */
	public static Properties props = new Properties();
	
///////////////////////////////////////////////////////////
// Creators
///////////////////////////////////////////////////////////
	
	/**
	 * Creator
	 * @param componentName
	 */
	public SessionReplicator(String componentName){
		super(componentName);
	}
	
///////////////////////////////////////////////////////////
// Member methods
///////////////////////////////////////////////////////////
	
	/**
	 * Opens the GUI.
	 */
	public void open() {
		display = Display.getDefault();
		createContents();
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Creates contents of the GUI.
	 */
	protected void createContents() {
		shell = new Shell();
		shell.setSize(450, 300);
		shell.setText("Session Replicator");
		
		// creates the Send button
		Button btnSendToServer = new Button(shell, SWT.NONE);
		btnSendToServer.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				// checks whether a file has been selected
				if(file_path.length() == 0) {
					popMessageDialog("No file has been selected!");
				} else {
					try {
						createEventList();
						sendEvents();
					} catch (IOException ioe) {
						popMessageDialog("Input file read error!");
					}
				}		
			}
		});
		btnSendToServer.setBounds(270, 230, 75, 21);
		btnSendToServer.setText("Send");
		
		// creates the Exit button
		Button btnExit = new Button(shell, SWT.NONE);
		btnExit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				printMsg(name, "Exit button pressed. Exiting ...");
				System.exit(0);
			}
		});
		btnExit.setBounds(350, 230, 75, 21);
		btnExit.setText("Exit");
		
		// creates the Browse button
		final Button btnBrowse = new Button(shell, SWT.NONE);
		btnBrowse.setBounds(350, 10, 75, 21);
		btnBrowse.setText("Browse");
		textboxFilePath = new Text(shell, SWT.BORDER);
		textboxFilePath.setBounds(10, 10, 340, 21);
		
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
					file_path = selectFile();
					textboxFilePath.setText(file_path); 
			}
		});
		
		// creates the console box
		labelConsole = new Label(shell, SWT.BORDER);
		labelConsole.setText("Select a FLAME log file.");
		labelConsole.setSize(415, 190);
		labelConsole.setLocation(10, 35);
	}
	
	/**
	 * Pops up a message dialogue
	 * @param title			Message dialogue's title
	 * @param message		The message
	 * @return				Button ID
	 */
	public int popMessageDialog(String message) {
		// create dialog with an OK button
		MessageBox dialog = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
		dialog.setText("Session Replicator");
		dialog.setMessage(message);

		// open dialog and await user selection
		return dialog.open();
	}
	
	/**
	 * Reads in the log file and creates the list
	 */
	protected void createEventList() throws IOException {
		Map<String, Integer> 	headers			= new HashMap<>();
		
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(file_path), Constants.charset)) { 

			// reads the header line
			String headerLine = reader.readLine();
			
			// creates the header map
			int index = 0;
			for(String token : headerLine.split(",")) {
				headers.put(token.trim(), index++);
			}
			
			// processes the lines of Events
			String line;
			while ((line = reader.readLine()) != null) {
				String[] 	tokens	= line.split(",");
				
				// retrieves the values of the event
				String valueTime 				= tokens[headers.get("TIME")].trim();
				String valueOriginComponent		= tokens[headers.get("OriginComponent")].trim();
			    String valueEventName 			= tokens[headers.get("Event name")].trim();
			    String valueSenderUsername		= tokens[headers.get("SenderUsername")].trim();
			    String valueEventID				= tokens[headers.get("EventID")].trim();
			    String valueValue				= tokens[headers.get("Value")].trim();
			    
			    // filters out events from a component other than FLAME Client
			    if (!(valueOriginComponent.equals("FLAME Client") || valueOriginComponent.equals("CoWare XTEAM Client"))) {
					continue;
				}
			 
			    // filters out duplicate login events
			    if(valueEventName.equals("Login")) {
			    	boolean found = false;
			    	for(String username : usernames) {
			    		if (valueSenderUsername.equals(username)) {
			    			found = true;
			    		}
			    	}
			    	
			    	if(found) {
			    		continue;
			    	} else {
			    		usernames.add(new String(valueSenderUsername));
			    	}
			    }
				
			    // re-creates the event
			    Event gmeEvent 					= (Event) new Event(valueEventName);
				gmeEvent.addParameter("OriginComponent", 	"FLAME Client");
				gmeEvent.addParameter("SenderUsername", 	valueSenderUsername);
				gmeEvent.addParameter("Value", 				valueValue);
				if(valueEventID.trim().length() != 0) {
					try {
						gmeEvent.addParameter("EventID", 			Integer.parseInt(valueEventID));
					} catch (NumberFormatException nfe) {
						printMsg(name, "Event ID exists but not valid: " + valueEventID);
					}
				}
				
				// adds the event to the event list
				try {
					events.add(new LogEvent(Utility.revertDate(valueTime), gmeEvent));
				} catch (ParseException pe) {
					printMsg(name, "Error: A timestamp is invalid: " + pe.toString());
				}
			}
		}
	}
	
	/**
	 * Pushes all events to FLAME Server
	 */
	public void sendEvents(){
		EventSender es = new EventSender();
		es.start();
	}
	
	/**
	 * EventSender thread
	 */
	protected class EventSender extends Thread {
		
		public void run() {
			// checks if the event list is not empty
			if(events.size() == 0) {
				printMsg(name, "Error: No event to send.");
				return;
			}
			
			// the clock
			long clock 			= 0;
			
			// the session beginning time
			long timeBeginning 	= events.get(0).getTime();
			
			// the session ending time
			long timeEnding		= events.get(events.size() - 1).getTime();
			
			// the event list size at the beginning
			int eventListSize	= events.size();
			
			while (events.size() > 0) {
				updateConsoleWithTime(clock, timeEnding, timeBeginning, eventListSize, events.size());
			
				List<LogEvent> eventsToSend = new ArrayList<>();
				
				// iterates through the list of events and finds the ones that are due sending
				Iterator<LogEvent> it = events.iterator();
				while(it.hasNext()) {
					LogEvent nextEvent = it.next();
					
					// checks if the event is due sending
					if(nextEvent.getTime() - timeBeginning <= clock) {
						// adds the event to the list of events to send
						eventsToSend.add(nextEvent.duplicate());
						
						// removes the event from the list of events
						it.remove();
					}
				}
				
				// sends the events due sending
				for(LogEvent lg : eventsToSend) {
					// transforms the event before sending
					Event evt = transformEvent(lg.getEvent());
					
					// prints the event to screen
					printEvent(evt, "Sent");
					
					// sends the event to FLAME Server
					sendRequest(evt);
				}
				
				// tick the clock for 1000 milliseconds (== 1 second)
				clock += 1000;
				
				// sleeps for 1000 milliseconds
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					printMsg(name, "The clock has been interrupted.");
				}
			}
			
			updateConsoleWithTime(clock, timeEnding, timeBeginning, eventListSize, events.size());
			
			printMsg(name, "Done sending events.");
			
			//closeGUI();
		}
	}
	
	
	
	/**
	 * Selects a FLAME log file
	 * @return
	 */
	protected String selectFile() {
		FileDialog dialog=new FileDialog(shell, SWT.OPEN);
		dialog.setFilterExtensions(FILTER_EXTS);
		String path=dialog.open();
		dialog.getFileName();
		if (path != null) {
			System.out.println(path);
			return path;
		} else {
			return null;
		}
	}
	
	protected void closeGUI () {
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					shell.dispose();
				}
			}
		});
	}
	
	/**
	 * Updates the console window
	 * @param msg			New message
	 */
	protected void updateConsole (String msg) { 
		final String newConsoleString = new String(msg);
		display.asyncExec(new Runnable () {
			public void run() {
				if(!shell.isDisposed()) {
					labelConsole.setText(newConsoleString);
				}
			}
		});
	}
	
	/**
	 * Updates the console window with the given time info
	 * @param clock
	 * @param timeEnding
	 * @param timeBeginning
	 * @param eventListSize
	 */
	protected void updateConsoleWithTime (long clock, long timeEnding, long timeBeginning, int eventListSize_beginning, int eventListSize) {
		long totalSecondsLeft 	= ((timeEnding - timeBeginning - clock) / 1000);
		long remainingMins 		= (long)(totalSecondsLeft / 60);
		long remainingSecs		= totalSecondsLeft % 60;
		
		String usernameListString = new String("Logged users: " + Constants.endl);
		
		if(usernames.size() == 0) {
			usernameListString += "None";
		} else {
			for(String username : usernames) {
				usernameListString += username + Constants.endl;
			}
		}
		
		updateConsole(	"The clock ticks at " + (clock / 1000) + " seconds." + Constants.endl +
						"The remaining time is " + remainingMins + " minutes " + remainingSecs + " seconds." + Constants.endl +
						(eventListSize_beginning - eventListSize) + " out of " + eventListSize_beginning + " events have been sent." + Constants.endl + Constants.endl +
						usernameListString);
	}
	
	/**
	 * Transforms a logged FLAME Event back to its active state
	 * @param e				Event to transform
	 */
	protected Event transformEvent (Event e) {
		final int attrElementID = 10;
		
		// replicates the event
		Event evt = e.replicate();
		
		// Snapshot and Update do not have Values. Need to create empty Values for those.
		if(evt.name.equals("Snapshot") || evt.name.equals("Update")) {
			// removes the old Value
			if(evt.hasParameter("Value")) {
				evt.removeParameter("Value");
			}
			
			// adds new, empty Value
			evt.addParameter("Value", FLAMEClient.manipulateEmptyValue(evt.name));
		}
		
		// All ","s have been replaced with "."s when logged. Need to change it back.
		else if(evt.hasParameter("Value")) {
			String oldValue = (String) evt.getParameter("Value");
			String newValue = new String();
			
			if(oldValue.length() > 0) {
				// tokenizes the old Value
				String tokens[] = oldValue.split("`", -1);
				
				// transforms the Value value
				for (int i=0; i < tokens.length; i++){
					if(i != attrElementID) {
						newValue += tokens[i].replaceAll("\\.", ",") + "`";
					} else if (i != tokens.length - 1) {
						newValue += tokens[i];
					} else {
						newValue += tokens[i] + "`";
					}
				}
				
				// drops the old Value and adds the new Value
				evt.removeParameter("Value");
				evt.addParameter("Value", newValue);
			}
		}
		
		return evt;
	}
	
	/**
	 * initializes the Session Replicator, invoked by Prism-MW
	 */
	public void start () {
		// opens the GUI
		open();
		
		printMsg(name, "Exiting ...");
		System.exit(0);
	}

	/**
	 * handle() method in SessionReplicator does nothing.
	 */
	@Override
	public void handle(Event arg0) {}


///////////////////////////////////////////////
// The main() method
///////////////////////////////////////////////
	
	/**
	 * Launches the application.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// reads the properties
		try {
			loadProps();
		} catch (Exception e) {
			System.out.println("[Session Replicator]: Error: " + e.getMessage());
			return;}

		// Build Prism-MW framework
		FIFOScheduler sched 			= new FIFOScheduler(100); 
		RRobinDispatcher disp 			= new RRobinDispatcher(sched, 10);
		Scaffold s 						= new Scaffold();
		s.dispatcher 					= disp;
		s.scheduler 					= sched;
		
		Architecture arch		 		= new Architecture("FLAME");
		arch.scaffold					= s;
		
		// Create components
		SessionReplicator sr; 	
		
		try {
			sr = new SessionReplicator("Session Replicator");
		} catch (Exception e) {
			System.out.println("Error: " + e);
			return;
		}
		
		Component Dclient 				= new Component("FLAME Client", sr);
		Dclient.scaffold 				= s;
		
		Connector eBus 					= new Connector("Event Bus");
		eBus.scaffold 					= s;
		
		// Add components
		arch.add(Dclient);
		arch.add(eBus);
		
		// Connect components and establish ports
		Port DclientRequestPort 		= new Port("DclientRequestPort", PrismConstants.REQUEST);
		Dclient.addCompPort(DclientRequestPort);
		
		Port eBusReplyPort 				= new Port("eBusReplyPort", PrismConstants.REPLY);
		eBus.addConnPort(eBusReplyPort);
		
		arch.weld(DclientRequestPort, eBusReplyPort);
		
		/*
		 *  Setup distribution components
		 *  Since each Prism instance on each side is a peer, both have will have
		 *  both server and client ports which connect to each other in 2 channels
		 */ 
		
		// Client socket
		ExtensiblePort epClient 		= new ExtensiblePort("epClient", PrismConstants.REQUEST);
		SocketDistribution sd 			= new SocketDistribution(epClient);
		
		epClient.addDistributionModule(sd);
		epClient.scaffold = s;
		
		// Add to appropriate places
		eBus.addConnPort(epClient);
		arch.add(epClient);
		
		// Connect to appropriate places
		epClient.connect(props.getProperty("SERVER"), Integer.parseInt(props.getProperty("PORT")));
		
		// Start dispatcher and architecture
		disp.start();
		arch.start();
	}
	
	/**
	 * Loads the configuration properties from the config.properties file
	 * 
	 * @throws Exception	An error occurred with opening the config.properties file
	 */
	public static void loadProps() throws Exception {
		try {
			props.load(new FileInputStream(new File("config.properties")));
		} catch (FileNotFoundException e) {
			throw new Exception ("config.properties is missing");
		} catch (IOException e) {
			throw new Exception ("config.properties cannot be read");
		}		
	}
}
