package flame;

import Prism.core.AbstractImplementation;
import Prism.core.Event;
import Prism.core.PrismConstants;

/**
 * AbstractImplementationModified is a FLAME customization of AbstractImplementation.<p>
 * 
 * It makes the PrismConstant (e.g. REQUEST of REPLY) explicit by implementing
 * {@link #sendRequest(Event)} and {@link #sendReply(Event)}.
 * 
 * @author 					<a href=mailto:jaeyounb@usc.edu>Jae young Bang</a>
 * @version 				2013.05
 */
public abstract class AbstractImplementationModified extends AbstractImplementation {

///////////////////////////////////////////////
//Member Variables
///////////////////////////////////////////////
	
	/**
	 * Name of the Component that inherits this class
	 */
	protected 		String 			name 			= new String("Undefined");
	
	/**
	 * ScreenLogger that manages screen output
	 */
	protected 		ScreenLogger 	screenLogger 	= new ScreenLogger();

///////////////////////////////////////////////
//Constructors
///////////////////////////////////////////////
	
	/**
	 * Default constructor that calls {@link #setComponentName(String)}
	 * 
	 * @param componentName		Component name
	 */
	public AbstractImplementationModified (String componentName) {
		setComponentName(componentName);
	}
	

	
///////////////////////////////////////////////
//Member Methods
///////////////////////////////////////////////
	
	/**
	 * Gets the Screen Logger
	 * @return				Screen Logger
	 */
	public ScreenLogger getScreenLogger() {
		return screenLogger;
	}
	
	
	/**
	 * Sets up the component name -- it must be called at the constructor of class that inherit this class
	 * @param componentName		Component name
	 */
	protected void setComponentName(String componentName) {
		name = componentName;
		
		printMsg(name, "Component name is set as [" + name + "]");
	}
	
	
	/**
	 * Handles incoming Events
	 * 
	 * @param e					Incoming Event
	 */
	@Override
	public abstract void handle(Event e);

	/**
	 * Adds REQEUST event type and sends it over
	 * 
	 * @param e					Outgoing Event
	 */
	public void sendRequest(Event e) {
		e.eventType = PrismConstants.REQUEST;
		send(e);
	}
	
	/**
	 * Adds REPLY event type and sends it over
	 * 
	 * @param e					Outgoing Event
	 */
	public void sendReply (Event e) {
		e.eventType = PrismConstants.REPLY;
		send(e);
	}
	
	/**
	 * Manipulates a String for a specified parameter from an Event for printEvent()
	 * 
	 * @param e				The Event
	 * @param parameterName	Parameter name
	 * @return				Manipulated String
	 */
	private String manipulateParameterInfo(Event e, String parameterName) {
		if(e.hasParameter(parameterName)) {
			String tabs = "\t";
			if(parameterName.length() <= 12) {
				tabs += "\t";
			}
			return new String(	"\t[" + parameterName 
								+ "]:" + tabs 
								+ e.getParameter(parameterName).toString() 
								+ System.lineSeparator());
		} else {
			return "";
		}
	}
	
	/**
	 * Prints detailed information about an incoming Event to screen
	 * 
	 * @param e					Incoming Event
	 */
	protected void printEvent(Event e) {
		printMsg(name, manipulateEventPrintString(e));
	}
	
	/**
	 * Prints detailed information about an incoming Event to screen
	 * 
	 * @param e					Incoming Event
	 */
	protected void printEvent(Event e, String actionString) {
		printMsg(name, manipulateEventPrintString(e, actionString));
	}
	
	
	
	/**
	 * Manipulates a String that carries all information about an Event
	 * 
	 * @param e					Incoming Event
	 * @return					String that contains the Event information
	 */
	protected String manipulateEventPrintString(Event e) {
		return manipulateEventPrintString(e, "Received");
	}
	
	protected String manipulateEventPrintString(Event e, String actionString) {
		String 		eventName 	= e.name;
		String		msg 		= new String ();
		String[] 	parameters 	= {	"OriginComponent" ,
									"SenderUsername",
									"ReceiverUsername",
									"ReceiverComponent",
									"IsBroadcast",
									"EventID",
									"AnalysisType",
									"AbsoluteTime",
									"IsLast"};
		
		msg += actionString + " [" + eventName + "]" + Constants.endl;
		
		for(String parameter : parameters) {
			msg += manipulateParameterInfo(e, parameter);
		}
		
		return msg;
	}

	/**
	 * Prints a message to screen with the component name
	 * 
	 * @param name			Component name
	 * @param msg			Message to be printed
	 */
	protected void printMsg(String name, String msg) {
		screenLogger.printMsg(name, msg);
	}
	
	
}
