package flame.detectors.slave;

import java.net.Socket;

public class SlaveInfo {
	
///////////////////////////////////////////////
// Member Variables
///////////////////////////////////////////////
	
	private Socket					socket;
	
	private String					name 		= null;
	
///////////////////////////////////////////////
// Constructors
///////////////////////////////////////////////
	
	public SlaveInfo (String name, Socket socket) throws Exception {
		this.name	= name;
		this.socket = socket;
	}
	
///////////////////////////////////////////////
// Member methods
///////////////////////////////////////////////	

	public Socket getSocket() {
		return socket;
	}
	
	public String getName() {
		return name;
	}
}
