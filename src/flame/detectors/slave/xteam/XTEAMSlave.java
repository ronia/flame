package flame.detectors.slave.xteam;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import flame.ArchiveUtility;
import flame.Constants;
import flame.FileUtility;
import flame.ScreenLogger;
import flame.SocketTransferUtility;
import flame.Utility;
import flame.detectors.slave.SlaveManager;
import flame.detectors.xteam.FileCopier;

/**
 * XTEAMSlave is a FLAME Slave that<br> 
 * 	(1) receives a FLAME simulation source code archive from a {@link SlaveManager},<br>
 *  (2) compiles the code,<br>
 *  (3) executes the compiled binary,<br>
 *  (4) sends back the simulation result back to the {@link SlaveManager}.<p>
 *
 * @author 					<a href="mailto:jaeyounb@usc.edu">Jae young Bang</a>
 * @version					2014.12
 */
public class XTEAMSlave {
	
///////////////////////////////////////////////
// Member Variables
///////////////////////////////////////////////
	
	/**
	 * Directory where the scaffold project is
	 */
	protected	Path			scaffoldDirectory;
	
	/**
	 * Directory where the received simulation source code archive files are
	 */
	protected	Path			receivedDirectory;
	
	/**
	 * Directory where the extracted simulation source code is
	 */
	protected	Path			simulationDirectory;
	
	/**
	 * Path to the GNU Make program
	 */
	protected	Path			makePath;			
	
	/**
	 * Path to the "makefile" template
	 */
	protected	Path			makefileTemplate;
	
	/**
	 * Name of the executable file of the compiled simulation code
	 */
	protected	Path			executableFile;
	
	/**
	 * Name of the errors list file
	 */
	protected	Path			errorsFilename;
	
	/**
	 * Number of cores this machine has
	 */
	protected	int				cores;
	
	/**
	 * Path to the file that contains the name of this node
	 */
	protected	Path			nodeNameFile;
	
	/**
	 * Host address of the slave manager
	 */
	protected	String			slaveManagerHostAddress;
	
	/**
	 * Port number of the slave manager
	 */
	protected	int				slaveManagerPort;		
	
	/**
	 * The socket to use to connect to Slave Manager
	 */
	protected	Socket			clientSocket;
	
	/**
	 * Screen Logger instance
	 */
	protected	ScreenLogger 	screenLogger = new ScreenLogger();
	
///////////////////////////////////////////////
// Constructors
///////////////////////////////////////////////

	/**
	 * Default constructor
	 * 
	 * @param scaffoldDirectory
	 * @param receivedArchivesDirectory
	 * @param simulationsDirectory
	 * @param makePath
	 * @param makefileTemplate
	 * @param adevsMakefileDirectory
	 * @param newranMakefileDirectory
	 * @param executableFile
	 * @param errorsFilename
	 * @param cores
	 * @param slaveManagerHostAddress
	 * @param slaveManagerPort
	 * @throws Exception
	 */
	public XTEAMSlave(	Path 		scaffoldDirectory,
						Path		receivedArchivesDirectory,
						Path		simulationsDirectory,
						Path		makePath,
						Path		makefileTemplate,
						Path		adevsMakefileDirectory,
						Path 		newranMakefileDirectory,
						Path		executableFile,
						Path		errorsFilename,
						int			cores,
						Path		nodeNameFile,
						String 		slaveManagerHostAddress,
						int			slaveManagerPort) throws Exception {
		
		this.scaffoldDirectory 			= scaffoldDirectory;
		this.makePath					= makePath;
		this.makefileTemplate			= makefileTemplate;
		this.executableFile				= executableFile;
		this.errorsFilename				= errorsFilename;
		this.cores						= cores;
		this.nodeNameFile				= nodeNameFile;
		this.slaveManagerHostAddress	= slaveManagerHostAddress;
		this.slaveManagerPort			= slaveManagerPort;
		
		// creates the necessary directories
		createDirectories(receivedArchivesDirectory, simulationsDirectory);
		
		// copies the scaffold code to the simulation directory
		copyScaffold();
		
		// compiles the Adevs and Newran libraries
		compileLibraries(adevsMakefileDirectory, newranMakefileDirectory);
	}
	
///////////////////////////////////////////////
// Member Methods
///////////////////////////////////////////////
	
	/**
	 * Creates the necessary directories to run {@link XTEAMSlave}
	 * 
	 * @param receivedArchivesDirectory
	 * @param simulationsDirectory
	 * @throws Exception
	 */
	protected void createDirectories(	Path		receivedArchivesDirectory,
										Path		simulationsDirectory)  throws Exception{
		// creates the received archives directory
		try { 
			Files.createDirectories(receivedArchivesDirectory);
		} catch (Exception e) {
			throw new Exception ("Directory " + receivedArchivesDirectory + " cannot be created");
		}
		
		// creates the simulations directory
		try { 
			Files.createDirectories(simulationsDirectory);
		} catch (Exception e) {
			throw new Exception ("Directory " + simulationsDirectory + " cannot be created");
		}
		
		// finds what time it is launched
		String launchTimeString = Utility.convertDate(System.currentTimeMillis());
		
		// creates the received archive directory for this run
		receivedDirectory = receivedArchivesDirectory.resolve(launchTimeString);
		try { 
			Files.createDirectories(receivedDirectory);
		} catch (Exception e) {
			throw new Exception ("Directory " + receivedDirectory + " cannot be created");
		}
		
		printMsg("Directory " + receivedDirectory + " has been created.");
		
		// creates the simulation directory for this run
		simulationDirectory = simulationsDirectory.resolve(launchTimeString);
		try { 
			Files.createDirectories(simulationDirectory);
		} catch (Exception e) {
			throw new Exception ("Directory " + simulationDirectory + " cannot be created");
		}
		
		printMsg("Directory " + simulationDirectory + " has been created.");
	}
	
	
	/**
	 * Copies the scaffold project into the target directory
	 */
	protected void copyScaffold () throws Exception {
		// copies the scaffold project into the source code directory
		FileCopier visitor = new FileCopier(scaffoldDirectory, simulationDirectory, screenLogger);
		try {
			Files.walkFileTree(scaffoldDirectory, visitor);
		} catch (Exception e) {
			throw new Exception ("Error while copying scaffold code to simulation: " + e);
		}
		
		printMsg("Scaffold code has been copied.");
	}
	
	/**
	 * Compiles the Adevs and Newran libraries
	 */
	protected void compileLibraries (Path adevsMakefileDirectory, Path newranMakefileDirectory) throws Exception {
		// compiles Adevs
		List<String> adevsCompErrs = compile("Adevs", simulationDirectory.resolve(adevsMakefileDirectory));
		if(adevsCompErrs.size() > 0) {
			throw new Exception (adevsCompErrs.size() + " errors found in Adevs compilation");
		}
		
		// compiles Newran
		List<String> newranCompErrs = compile("Newran", simulationDirectory.resolve(newranMakefileDirectory));
		if(newranCompErrs.size() > 0) {
			throw new Exception (newranCompErrs.size() + " errors found in Newran compilation");
		}
	}
	
	/**
	 * Compiles a library; invokes "make"
	 * 
	 * @param name						The name of the component being compiled
	 * @param makefileDirectory			The directory where the "makefile" file is
	 * @return							List of errors; empty when the compilation succeeded
	 * @throws Exception				Unexpected error during the compilation
	 */
	protected List<String> compile (String name, Path makefileDirectory) throws Exception {
		List<String>	errorsList 				= new ArrayList<>();
		Path			compilationErrorLogFile	= makefileDirectory.resolve("error_log_" + name + "_compilation");
		
		// forms the compilation command
		String[] runCmd = {		makePath.toString(), 
								"-C", 
								makefileDirectory.toString(), 
								"-j", 
								Integer.toString((int)(cores * 1.5))};
		
		printMsgTarget(name, "Compilation begins ...");
		
		try {
			// executes "make"
			Process proc = Runtime.getRuntime().exec(runCmd);
			
			// waits until the compilation ends
			proc.waitFor();
			
			// reads the error output of the "make" process
			try (BufferedReader in = new BufferedReader(new InputStreamReader(proc.getErrorStream()))) {
				Files.write(compilationErrorLogFile, "".getBytes(), StandardOpenOption.CREATE);
				String line;
				while ((line = in.readLine()) != null) {
					Files.write(compilationErrorLogFile, (line+"\n").getBytes(), StandardOpenOption.APPEND);
				}
			} catch (IOException ioe) {
				throw new IOException ("Error while reading the compilation error output: " + ioe);
			}
			
			// checks if the error log file has been created
			if(Files.notExists(compilationErrorLogFile, new LinkOption[]{LinkOption.NOFOLLOW_LINKS})) {
				throw new Exception (name + " compilation error log cannot be found: " + compilationErrorLogFile);
			}
			
			// checks for any compilation errors
			if(Files.size(compilationErrorLogFile) > 0) {
				List<String> log = Files.readAllLines(compilationErrorLogFile, Constants.charset);
				
				// finds lines with the string "error" and adds them to the errors list
				// filters out the lines that begins with "make" since they are from make but not g++
				for(String line : log) {
					if(!line.substring(0, 4).equals("make")) {
						if(line.contains("error") || line.contains("Error")) {
							errorsList.add(line);
						}
					}
				}
			}
			
		} catch (Exception e) {
			throw new Exception ("Error while compiling " + name + ": " + e);
		}
		
		// prints how the compilation went
		int errorCount = errorsList.size();
		String errorCountMsg;
		switch (errorCount) {
			case 0:
				errorCountMsg = "no error";
				break;
			case 1:
				errorCountMsg = "1 error";
				break;
			default:
				errorCountMsg = errorCount + " errors";
				break;
		}
		printMsgTarget(name, "Compilation finished with " + errorCountMsg + ".");
		
		return errorsList;
	}
	
	/**
	 * Launches the {@link XTEAMSlave}
	 */
	public void launch() throws Exception {
		// connects to the Slave Manager
		try {
			clientSocket = new Socket(slaveManagerHostAddress, slaveManagerPort);
			clientSocket.setSoTimeout(0);
			clientSocket.setKeepAlive(true);
		} catch (UnknownHostException uhe) {
			throw new Exception ("Error while connecting to Slave Manager: host address unknown: " + uhe);
		} catch (IOException ioe) {
			throw new Exception ("Error while connecting to Slave Manager: unknown issue: " + ioe);
		}
		
		printMsg("Connecting to Slave Manager completed.");
		
		// reads slave name
		String slaveName = "";
		if(Files.exists(nodeNameFile, new LinkOption[]{LinkOption.NOFOLLOW_LINKS})) {
			List<String> lines = Files.readAllLines(nodeNameFile, Constants.charset);
			if(lines.size() > 0) {
				slaveName = lines.get(0);
			}
		}
		
		// if the node name is not given, generates a GUID name
		if(slaveName.equals("")) {
			slaveName = UUID.randomUUID().toString();
		}
		
		// sends slave name
		try {
			SocketTransferUtility.sendString(clientSocket, slaveName);
		} catch (Exception e) {
			throw new Exception ("Error while sending slave name to Slave Manager: " + e);
		}
		
		printMsg("Slave name \"" + slaveName +"\" has been sent.");
		
		while (true) {
			try {
				// receives a simulation source code archive (ZIP)
				printMsg("Waiting for simulation code archive ...");
				Path receivedSimulationArchive = SocketTransferUtility.receiveFile(clientSocket, receivedDirectory);
				printMsg("Received simluation code: " + receivedSimulationArchive.getFileName());

				// handles the simulation request
				Path simulationResultArchive = handleRequest(receivedSimulationArchive);

				// sends the simulation result archive (ZIP)
				printMsg("Sending simulation result: " + simulationResultArchive.getFileName());
				SocketTransferUtility.sendFile(clientSocket, simulationResultArchive);
				printMsg("Sending " + simulationResultArchive.getFileName() + " has been completed.");
			} catch (IOException ioe) {
				printMsg("Kill the slave due to an error: " + ioe);
				System.exit(1);
			} 
		}
	}
	
	/**
	 * Handles a simulation request from the Slave Manager
	 * @param sourceCodeArchive		Path to the ZIP file that contains the source code
	 * @return						Path to the ZIP file that contains the simulation result files
	 * @throws Exception
	 */
	public Path handleRequest(Path sourceCodeArchive) throws Exception {
		Path 	resultArchive;
		String 	filename;
		String	simulationName; 
		
		filename 						= sourceCodeArchive.getFileName().toString();
		simulationName					= FileUtility.getFilenameWithoutExtension(filename);
		printMsgTarget(simulationName, "Simulation request handling begins ...");
		
		// extracts the received source code to a source code directory
		Path sourceCodeDirectory 		= extractSourceCode(sourceCodeArchive);
		printMsgTarget(simulationName, "Extracting source code archive completed.");
		
		// generates a "makefile" file
		generateMakefile(sourceCodeDirectory);
		printMsgTarget(simulationName, "The \"makefile\" generation completed.");	
		
		// compiles the source code
		List<String> errors = compile(simulationName, sourceCodeDirectory);
		
		// in case there was no compilation error
		Path simulationExecutable 		= sourceCodeDirectory.resolve(executableFile);
		Path simulationResultDirectory 	= simulationExecutable.getParent();
		if(errors.size() == 0) {
			// executes the compiled binary
			printMsgTarget(simulationName, "Simulation execution begins ...");
			execute(simulationExecutable);
			
			// in case the execution finished right; looks for "Simulation_Completion.txt"
			if(Files.exists(simulationResultDirectory.resolve("Simulation_Completion.txt"), new LinkOption[]{LinkOption.NOFOLLOW_LINKS})) {
				printMsgTarget(simulationName, "Simulation execution successfully completed.");
			} else {
				// adds the error to the errors list
				errors.add(new String("Simulation execution was disrupted for an unhandled exception. Manual simulation code inspection recommended."));
				
				printMsgTarget(simulationName, "Simulation execution did not succeed.");
			}
		}
		
		// creates (ignores if already existing) the simulation result directory
		Files.createDirectories(simulationResultDirectory);
		
		// writes the errors file
		Path errorsFile = simulationResultDirectory.resolve(errorsFilename);
		Files.write(errorsFile, "".getBytes(), StandardOpenOption.CREATE);
		for(String error : errors) {
			Files.write(errorsFile, (error+"\n").getBytes(), StandardOpenOption.APPEND);
		}
		
		// archives the errors file + simulation results
		printMsgTarget(simulationName, "Archiving the simulation result begins ...");
		resultArchive = archiveSimulationResult(simulationName, simulationResultDirectory);
		printMsgTarget(simulationName, resultArchive.getFileName().toString() + " has been created.");
		
		printMsgTarget(simulationName, "Simulation request handling completed.");
		
		return resultArchive;
	}
	
	/**
	 * Extracts a source code archive (ZIP) and returns the sourceCodeDirectory
	 * 
	 * @param sourceCodeArchive			The source code archive to extract
	 * @return							Path to the directory in which the source code is 
	 */
	protected Path extractSourceCode (Path sourceCodeArchive) throws Exception {
		// creates a directory with the source code name under the simulation directory 
		Path sourceCodeDirectory = simulationDirectory.resolve("simulation_code/" + sourceCodeArchive.getFileName());
		try { 
			Files.createDirectories(sourceCodeDirectory);
		} catch (Exception e) {
			throw new Exception ("Directory " + sourceCodeDirectory + " cannot be created");
		}
		
		// extracts the source code archive
		ArchiveUtility.extract(sourceCodeArchive, sourceCodeDirectory);
		
		return sourceCodeDirectory;
	}
	
	/**
	 * Generates the "makefile" file for the source code
	 * 
	 * @param sourceCodeDirectory		The directory in which the source code is
	 * @param makefileTemplate			The makefile template file
	 * @throws IOException 
	 */
	protected void generateMakefile (Path sourceCodeDirectory) throws IOException {
		String			versionName	= sourceCodeDirectory.getFileName().toString();
		Path 			makefile 	= sourceCodeDirectory.resolve("makefile");
		
		// reads the makefile template
		List<String>	template;
		try {
			template = Files.readAllLines(makefileTemplate, Constants.charset);
		} catch (IOException ioe) {
			throw new IOException("Error while reading \"makefile\" template: " + ioe);
		}
		
		// makes the lists of the header and source code files
		List<Path>	headerFiles = FileUtility.findFilesWithExtension(sourceCodeDirectory, "h");
		List<Path>	sourceFiles	= FileUtility.findFilesWithExtension(sourceCodeDirectory, "cpp");
		
		// writes the "makefile" file
		try (	FileWriter writer	= new FileWriter(makefile.toFile());
				PrintWriter pw 		= new PrintWriter(writer)) {
			
			// writes the dependency files list
			pw.println("# Dependency files");
			pw.print("DEPS = ");
			for(Path headerFile : headerFiles) {
				pw.print(headerFile.getFileName().toString() + " ");
			}
			pw.println("\n");
			
			// writes the object files list
			pw.println("# Object files");
			pw.print("OBJ = ");
			for(Path sourceFile : sourceFiles) {
				// splits the filename based on where the period is
				String[] tokens = sourceFile.getFileName().toString().split("\\.");
				if (tokens.length <= 1) {
					printMsg("File does not have an extension: " + sourceFile);
					continue;
				}
				
				// replaces the extension from .cpp to .o
				String objectFile = new String("$(OBJ_DIR)/");
				for(int i=0; i < tokens.length - 1; i++) {
					objectFile += tokens[i];
				}
				objectFile += ".o";
				
				pw.print(objectFile + " ");
			}
			pw.println("\n");
			
			// writes the template 
			for(String line : template) {
				pw.println(line);
			}
		} catch(IOException ioe) { 
			throw new IOException("Error while generating \"makefile\" for [" + versionName + "]: " + ioe); 
		}
	}
	
	/**
	 * Executes the simulation
	 * @param executableFile		Path to the simulation's executable file
	 * @throws Exception
	 */
	protected void execute (Path executableFile) throws Exception {
		// checks if the execution file exists
		if(Files.notExists(executableFile, new LinkOption[]{LinkOption.NOFOLLOW_LINKS})) {
			throw new Exception ("Execution file does not exist: " + executableFile.toString());
		}
		
		// forms the compilation command
		String[] runCmd = { "./" + executableFile.getFileName().toString() };
		
		// executes "simulation"
		Process proc = Runtime.getRuntime().exec(runCmd, null, executableFile.getParent().toFile());

		// waits until the compilation ends
		proc.waitFor();
	}
	
	/**
	 * Archives the result files and returns the path to the archived file (ZIP)
	 * 
	 * @param resultDirectory			The result directory
	 * @return							Path to the result archive file
	 */
	protected Path archiveSimulationResult (String simulationName, Path resultDirectory) throws Exception {
		Path resultArchive = resultDirectory.resolve(simulationName + "_result.zip");
		
		// finds all .csv and .txt files
		List<Path> filesToArchive = new ArrayList<>(); 
		List<Path> csvFiles = FileUtility.findFilesWithExtension(resultDirectory, "csv");
		List<Path> txtFiles = FileUtility.findFilesWithExtension(resultDirectory, "txt");
		filesToArchive.addAll(csvFiles);
		filesToArchive.addAll(txtFiles);
		
		// creates the archive
		ArchiveUtility.archive(resultArchive, filesToArchive);
				
		return resultArchive;
	}
	
	/**
	 * Prints a message to screen
	 * @param msg			Message to print
	 */
	protected void printMsg(String msg) {
		screenLogger.printMsg("Slave", msg);
	}
	
	/**
	 * Prints a message for a simulation to screen
	 * @param simulationName	Name of the simulation
	 * @param msg				Message to print
	 */
	protected void printMsgTarget(String targetName, String msg) {
		screenLogger.printMsg("Slave/"+targetName, msg);
	}
	
	/**
	 * Prints an empty line
	 */
	protected void printEmptyLine() {
		screenLogger.printEmptyLine();
	}

	
///////////////////////////////////////////////
// The main () method
///////////////////////////////////////////////
	
	/**
	 * Configuration properties read from the config.properties file
	 */
	public static Properties props = new Properties();
	
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
	
	/**
	 * The main() method
	 */
	public static void main(String[] args) {
		// load up the properties specified in the config.properties file
		try {
			loadProps();
		} catch (Exception e) {
			System.out.println("[Slave] Error: " + e.getMessage());
			return;
		}
		
		// gets properties
		Path 		scaffoldDirectory;
		Path		receivedArchivesDirectory;
		Path		simulationsDirectory;
		Path		makePath;
		Path		makefileTemplate;
		Path		adevsMakefileDirectory;
		Path		newranMakefileDirectory;
		Path		executableFile;
		Path		errorsFilename;
		int			cores;
		Path		nodeNameFile;
		String 		slaveManagerHostAddress;
		int			slaveManagerPort;
		try {
			scaffoldDirectory			= Utility.getPathProperty	(props, "SLAVE_SCAFFOLD_DIR");
			receivedArchivesDirectory	= Utility.getPathProperty	(props, "SLAVE_RECEIVED_ARCHIVES_DIR");
			simulationsDirectory		= Utility.getPathProperty	(props, "SLAVE_SIMULATIONS_DIR");
			makePath					= Utility.getPathProperty	(props, "SLAVE_MAKE_PATH");
			makefileTemplate			= Utility.getPathProperty	(props, "SLAVE_MAKEFILE_TEMPLATE");
			adevsMakefileDirectory		= Utility.getPathProperty	(props, "SLAVE_MAKEFILE_ADEVS");
			newranMakefileDirectory		= Utility.getPathProperty	(props, "SLAVE_MAKEFILE_NEWRAN");
			executableFile				= Utility.getPathProperty	(props, "SLAVE_EXECUTABLE_FILE");
			errorsFilename				= Utility.getPathProperty	(props, "SLAVE_ERRORS_FILENAME");
			cores						= Utility.getIntProperty	(props, "SLAVE_CORES");
			nodeNameFile				= Utility.getPathProperty	(props, "SLAVE_NODE_NAME_FILE");
			slaveManagerHostAddress		= Utility.getProperty		(props, "SERVER");
			slaveManagerPort			= Utility.getIntProperty	(props, "SM_PORT");
		}  catch (Exception e) {
			System.out.println("[Slave]: Error: " + e);
			return;
		}
		
		if(args.length >= 2) {
			System.out.println("[Slave]: Host address argument received: " + args[0]);
			System.out.println("[Slave]: Host port argument received: " + args[1]);
			slaveManagerHostAddress	= args[0];
			try {
				slaveManagerPort		= Integer.parseInt(args[1]);
			} catch (NumberFormatException nfe) {
				System.out.println("[Slave]: Error while parsing the port number: " + args[1]);
			}
		}
		
		// initializes the slave instance
		XTEAMSlave slave;
		try {
			slave = new XTEAMSlave(	scaffoldDirectory,
									receivedArchivesDirectory,
									simulationsDirectory, 
									makePath,
									makefileTemplate, 
									adevsMakefileDirectory,
									newranMakefileDirectory,
									executableFile,
									errorsFilename,
									cores,
									nodeNameFile,
									slaveManagerHostAddress, 
									slaveManagerPort );
		} catch (Exception e) {
			System.out.println("[Slave]: " + e);
			return;
		}
		
		try {
			slave.launch();
		} catch (Exception e) {
			System.out.println("[Slave]: " + e);
			return;
		}
	}
}
