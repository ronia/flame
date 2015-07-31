package flame.detectors.xteam;

import java.io.IOException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import flame.Constants;
import flame.ScreenLogger;

public class FileFinder extends SimpleFileVisitor <Path> {
	
	protected 		String 			cppFiles 			= new String();
	protected 		String 			hFiles				= new String();

	protected final String 			fileTagBeginning	= "\t\t\t<File" + Constants.endl + "\t\t\t\tRelativePath=\".\\simulation_code\\";
	protected final String 			fileTagEnding 		= "\"" + Constants.endl + "\t\t\t\t>" + Constants.endl + "\t\t\t</File>" + Constants.endl;
	
	protected		ScreenLogger	sl;
	
	public FileFinder(ScreenLogger screenLogger) {
		sl = screenLogger;
	}
	
	public String getCPPFiles() {
		return cppFiles;
	}
	
	public String getHFiles() {
		return hFiles;
	}
	
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) 
		throws IOException {
		
		String filename = file.getFileName().toString();
		String[] tokens = filename.split("\\.");
		
		switch (tokens[tokens.length-1].toLowerCase()) {
		
			case "cpp":
				cppFiles += fileTagBeginning + filename + fileTagEnding;
				break;
				
			case "h":
				hFiles += fileTagBeginning + filename + fileTagEnding;
				break;
		}
		
		return FileVisitResult.CONTINUE;
	}
	
	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc)
			throws IOException {
		
		if(exc instanceof FileSystemLoopException) {
			sl.printMsg("File Finder", "Cycle was detected: " + (Path) file);
		} else {
			sl.printMsg("File Finder", "Error occurred, unable to copy: " + (Path) file + "[" + exc + "]");
		}
		
		return FileVisitResult.CONTINUE;
	}
}