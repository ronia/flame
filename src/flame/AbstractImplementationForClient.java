package flame;

import java.util.ArrayList;

import Prism.core.Event;


public abstract class AbstractImplementationForClient extends
		AbstractImplementationModified {

	public AbstractImplementationForClient(String componentName) {
		super(componentName);
	}
	
	/**
	 * Sends a Login event
	 * 
	 * @param username		Username of the Architect
	 * @param password		Password of the Architect
	 */
	protected void sendLoginEvent (String username, String password) {
		// create a login event
		Event loginEvent = (Event) new Prism.core.Event("Login");
		
        loginEvent.addParameter("SenderUsername", 	username);
        loginEvent.addParameter("SenderPassword", 	password);
        loginEvent.addParameter("OriginComponent", 	name);
       
        // send the login event to CoWareServer
        sendRequest(loginEvent);
	}
	
	/**
	 * Decides whether an incoming Event is for this architect
	 * 
	 * @param e				Incoming Event
	 * @return				Decision -- true if the Event is for this Architect, false otherwise
	 */
	public boolean is_this_for_me(Event e, String username) {
	
		String	originComponent		= "";
		String	senderUsername		= "";
		String 	receiverUsername 	= "";
		String	receiverComponent	= "";
		boolean	isBroadcast 		= false;
		
		
		// gets the Event Name of the event
		String eventName = e.name;		
		
		// gets the OriginComponent from the Event
		if (e.hasParameter("OriginComponent")) {
			originComponent = (String) e.getParameter("OriginComponent");
		}
		
		// gets the Sender Username from the Event
		if (e.hasParameter("SenderUsername")) {
			senderUsername = (String) e.getParameter("SenderUsername");
		}
				
		// gets the Receiver Username from the Event
		if (e.hasParameter("ReceiverUsername")) {
			receiverUsername = (String) e.getParameter("ReceiverUsername");
		}
		
		// gets the Receiver component name from the Event
		if (e.hasParameter("ReceiverComponent")) {
			receiverComponent = (String) e.getParameter("ReceiverComponent");
		}
		
		// gets the IsBroadcast from the Event
		if (e.hasParameter("IsBroadcast")) {
			isBroadcast = (Boolean) e.getParameter("IsBroadcast");
		}
		
		
		switch(eventName) {
		
		
			case "Finish" : 
				
				if(isBroadcast == false && receiverUsername.equals(username) )
					return true;
				
				break;
		
			/*
			 *  Accept a Design when
			 *  IsBroadcast == false && ReceiverUsername == username && ReceiverComponent == name
			 *  IsBroadcast == true && !(SenderUsername == username && OriginComponent == name)
			 */
		
			case "Design" :
				
				if ( 		isBroadcast == false && 
							receiverUsername.equals(username) && 
							receiverComponent.equals(name)) {
					return true;
				} else if ( isBroadcast == true && 
							!(senderUsername.equals(username) && originComponent.equals(name)) ){
					return true;
				}
			
				break;
			
			/*
			 * Accept an XTEAM when
			 * IsBroadcast == false && ReceiverUsername == username && ReceiverComponent == name
			 * IsBroadcast == true
			 */
				
			case "XTEAM":
				
				if ( 	isBroadcast == false && 
						receiverUsername.equals(username) && 
						receiverComponent.equals(name)) {
					return true;
				} else if ( isBroadcast == true ) {
					return true;
				}
				
				break;
				
			/*
			 * Accept a Snapshot when
			 * IsBroadcast == false && ReceiverUsername == username && ReceiverComponent == name
			 * IsBroadcast == true
			 */
				
			case "Snapshot" :
				
				if ( 	isBroadcast == false && 
						receiverUsername.equals(username) && 
						receiverComponent.equals(name) ) {
					return true;
				} else if ( isBroadcast == true ) {
					return true;
				}
				
				break;
				
			/*
			 * Accept an Update when
			 * IsBroadcast == false && ReceiverUsername == username && ReceiverComponent == name
			 * IsBroadcast == true
			 */
				
			case "Update" :
				
				if ( 	isBroadcast == false && 
						receiverUsername.equals(username) && 
						receiverComponent.equals(name) ) {
					return true;
				} else if ( isBroadcast == true ) {
					return true;
				}
				
				break;
				
			/*
			 * Accept an Init when
			 * ReceiverUsername == username && ReceiverComponent == name
			 */
				
			case "Init":
				// if it is targeted at this architect
				if (	receiverUsername.equals(username) &&
						receiverComponent.equals(name)) {
					return true;
				}
				
				break;
			
			/*
			 * Accept a Notification when
			 * SenderUsername == "MRSV" || SenderUsername == "LSV" || SenderUsername == username
			 */
			case "Notification":
				if (	senderUsername.equals("MRSV") 	|| 
						senderUsername.equals("LSV") 	|| 
						senderUsername.equals(username) ) {
					return true;
				}
				
				break;
		}
	
		
		return false;
	}
	
	/**
	 * Filters Events leaving only Design and Init Events in the list
	 * 
	 * @param events		List of Events
	 * @return				New list of Events with only Design and Init Events
	 */
	protected ArrayList<Event> filterEvents (ArrayList<Event> events) {
		ArrayList<Event> newEvents = new ArrayList<>();
		
		for(Event e : events) {
			String eventName = e.name;
			
			switch(eventName) {
				case "Design":
				case "Init":
					newEvents.add(e);
					break;
				case "Snapshot":
				case "Update":
					break;
				default:
					printMsg(name, "Error: Found a [" + eventName + "] Event from the Event buffer");
					break;
			}
		}
		
		return newEvents;
	}
}
