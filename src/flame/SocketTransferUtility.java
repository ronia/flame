package flame;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * SocketTransferUtility class has helper methods that send and receive files over socket connections.
 * 
 * @author 					<a href="mailto:jaeyounb@usc.edu">Jae young Bang</a>
 * @version					2015.01
 */
public class SocketTransferUtility {
	
	/**
	 * The buffer size used for sending and receiving files
	 */
	private static final int 		bufferSize 		= 1024;
	
	/**
	 * The file size request message exchanged after file name has been exchanged and before sending file size begins
	 */
	private static final String		sizeMessage		= "SIZE";
	
	/**
	 * The ready message exchanged after file size has been exchanged and before the file transfer begins
	 */
	private static final String		readyMessage	= "READY";
	
	/**
	 * The DONE message exchanged after file transfer is complete
	 */
	private static final String 	doneMessage		= "DONE";
	
	/**
	 * Sends a file over socket
	 * 
	 * @param socket			Socket
	 * @param file				The file to send
	 * @throws Exception
	 */
	public static void sendFile(Socket socket, Path file) throws IOException {
		// sends the file name first
		sendMessage(socket, "file name", file.getFileName().toString());
		//System.out.println("File name sent: " + file.getFileName().toString());
		
		// receives SIZE message
		receiveMessageCheck(socket, sizeMessage);
		
		// sends the file size second
		long fileSize = Files.size(file);
		sendMessage(socket, "file size", Long.toString(fileSize));
		//System.out.println("File size sent: " + Long.toString(fileSize));
		
		// receives READY message
		receiveMessageCheck(socket, readyMessage);
		
		// sends the file content
		try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file.toFile()))) {
			BufferedOutputStream 	bos 	= new BufferedOutputStream(socket.getOutputStream());			
			byte[] 					bytes	= new byte[bufferSize];
			
			//long totalSize = 0;
			for(int size; (size = bis.read(bytes)) > 0;) {
				bos.write(bytes, 0, size);
				
				//totalSize += size;
				//System.out.println("Sent " + size + "/" + totalSize);
			}
			bos.flush();
		} catch (IOException e) {
			throw new IOException ("Error while sending file " + file.getFileName() + ": " + e);
		}
		
		// receives DONE message
		receiveMessageCheck(socket, doneMessage);
	}
	
	/**
	 * Receives a file from socket and stores it in the given path
	 * 
	 * @param socket			Socket
	 * @param targetDirectory	Directory in which the received file will be stored
	 * @throws Exception
	 */
	public static Path receiveFile(Socket socket, Path targetDirectory) throws IOException, NumberFormatException {
		// receives the file name first
		String filename = receiveMessage(socket, "file name");
		//System.out.println("File name received: " + filename);
		
		// sends SIZE
		sendMessage(socket, sizeMessage, sizeMessage);
		
		// receives the file size second
		String fileSizeString = receiveMessage(socket, "file size");
		//System.out.println("File size received: " + fileSizeString);
		
		// parses the received file size string
		long fileSize;
		try {
			fileSize = Long.parseLong(fileSizeString);
		} catch (NumberFormatException nfe) {
			throw new NumberFormatException ("Error while parsing received file size \"" + fileSizeString + "\": " + nfe);
		}
		
		// sends READY
		sendMessage(socket, readyMessage, readyMessage);
		
		// receives the file content
		Path file = targetDirectory.resolve(filename);
		try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file.toFile()))) {
			BufferedInputStream 	bis 	= new BufferedInputStream(socket.getInputStream());
			byte[] 					buff 	= new byte[bufferSize];
			
			int size;
			long bytesRead = 0;
			while (true) {
				// computes the number of bytes to read
				int bytesToRead;
				if((fileSize - bytesRead) > bufferSize) {
					bytesToRead = bufferSize;
				} else {
					bytesToRead = (int)(fileSize - bytesRead);
				}
				
				// reads from the socket
				size = bis.read(buff, 0, bytesToRead);
				
				// checks to total received bytes
				bytesRead += size;
				
				//System.out.println("Received " + size + "/" + bytesRead);
				
				// writes what's read to the file
				bos.write(buff, 0, size);
				
				if(bytesRead == fileSize) {
					break;
				}
			}
			bos.flush();
		} catch (IOException e) {
			throw new IOException ("Error while receiving file " + file.getFileName() + ": " + e);
		}
		
		// sends DONE
		sendMessage(socket, doneMessage, doneMessage);
		
		return file;
	}
	
	/**
	 * Sends a message over a socket
	 * @param socket			Socket
	 * @param messageName		The message name
	 * @param messageStr		The String that the message contains
	 * @throws IOException
	 */
	public static void sendMessage(Socket socket, String messageName, String messageStr) throws IOException {
		try {
			sendString(socket, messageStr);
		} catch (IOException ioe) {
			throw new IOException ("Error while sending " + messageName + " " + messageStr + ": " + ioe);
		}
	}
	
	/**
	 * Sends a Keepalive message over a socket
	 * @param socket			Socket
	 * @throws IOException
	 */
	public static void sendKeepalive(Socket socket) throws IOException {
		try {
			sendString(socket, "");
		} catch (IOException ioe) {
			throw new IOException ("Error while sending Keepalive: " + ioe);
		}
	}
	
	/**
	 * Receives a message over a socket
	 * @param socket			Socket
	 * @param messageName		The message name
	 * @return					The received message
	 * @throws IOException
	 */
	public static String receiveMessage(Socket socket, String messageName) throws IOException {
		String receivedMessage;
		try {
			while (true) {
				receivedMessage = receiveString(socket);
				
				if(receivedMessage == null) {
					
				} else {
					break;
				}
			}
			
		} catch (IOException ioe) {
			throw new IOException ("Error while receiving " + messageName + ": " + ioe);
		}
		
		return receivedMessage;
	}
	
	/**
	 * Receives a message and checks whether the received message is the intended message
	 * @param socket			Socket
	 * @param messageToReceive	The intended message to receive
	 * @throws IOException
	 */
	public static void receiveMessageCheck(Socket socket, String messageToReceive) throws IOException {
		String receivedMessage;
		try {
			receivedMessage = receiveString(socket);
			if(!receivedMessage.equals(messageToReceive)) {
				throw new IOException ("Unknown message received while waiting for " + messageToReceive);
			}
		} catch (IOException ioe) {
			throw new IOException ("Error while receiving " + messageToReceive + ": " + ioe);
		}
	}
	
	/**
	 * Sends a string over socket
	 * @param socket			Socket
	 * @param str				The string to send
	 * @throws Exception
	 */
	public static void sendString(Socket socket, String str) throws IOException {
		try {
			PrintWriter pw	= new PrintWriter(socket.getOutputStream(), true);
			SocketTransferUtility.write(pw, str);
		} catch (IOException e) {
			throw new IOException ("Error while sending \"" + str + "\": " + e);
		}
	}
	
	/**
	 * Receives a string from socket
	 * @param socket			Socket
	 * @return					The string read
	 * @throws Exception
	 */
	public static String receiveString(Socket socket) throws IOException {
		String line = null;
		
		try {
			BufferedReader br 	= new BufferedReader(new InputStreamReader(socket.getInputStream()));
			line 				= SocketTransferUtility.read(br);
		} catch (IOException e) {
			throw new IOException ("Error while reading from socket: " + e);
		}
		
		return line;
	}
	
	/**
	 * Receives a String from a socket with a 2-byte length code at the beginning.<p>
	 * 
	 * @param in		BufferedReader of the socket
	 * @return			Received String
	 * @throws Exception
	 */
	public static String read (BufferedReader in) throws IOException {
	
		// in case the socket connection has not yet been established
		if(in == null) {
			throw new IOException ("Socket connection has not yet been established");
		}
		
		char 	len_buf[] 	= new char[3];			// buffer storing the two bytes of length
		int		length 		= 0;					// the length of the following message
		char	buffer[]	= null;					// the message buffer
		
		try {
			while (true) {
				// reads the first two bytes that say how long the following message is
				if (in.read(len_buf, 0, 2) != 2) {
					throw new IOException ("Error while reading the length code");
				}
				
				// decodes the length
				length = ((int) (len_buf[0])) * 128 + ((int) len_buf[1]);
				
				// 0 length means it is a Keepalive message
				if (length == 0) {
					System.out.println("DEBUG: Keepalive received.");
				} else {
					break;
				}
			}
			
			// allocates the buffer
			buffer = new char[length];
			
			// reads the message
			if (in.read(buffer, 0, length) != length) {
				throw new IOException ("Received message is too short");
			}
		} catch (IOException ioe) {
			throw new IOException ("Error while reading a message: " + ioe);
		}
		
		return new String (buffer);
	}

	/**
	 * Sends a String over a socket with a 2-byte length code at the beginning
	 * 
	 * @param out			PrintWriter of the socket
	 * @param value			String to send
	 * @throws Exception
	 */
	public static void write (PrintWriter out, String value) throws IOException {
		
		// in case the socket connection has not yet been established
		if(out == null) {
			throw new IOException ("Socket connection has not yet been established");
		}
		
		// encodes the length
		char len_buf[] = new char[2];
		len_buf[0] = (char) (value.length() / 128);
		len_buf[1] = (char)	(value.length() % 128);
		
		// forward the length and the value
		out.print(len_buf[0]);
		out.print(len_buf[1]);
		if(value.length() != 0) {
			out.print(value);
		}
		out.flush();
	}
}
