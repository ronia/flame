package flame.detectors.xteam;

import java.io.IOException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import flame.ScreenLogger;


/**
 * FileCopier implements the iterative file copy code for copying
 * the scaffold code into the simulation code
 * 
 * @author 					<a href=mailto:jaeyounb@usc.edu>Jae young Bang</a>
 * @version					2013.05
 */
public class FileCopier implements FileVisitor <Path>{
	
	/**
	 * Origin directory path
	 */
	private Path 			fromDir;
	
	/**
	 * Target directory path
	 */
	private Path 			toDir;
	
	/**
	 * ScreenLogger instance
	 */
	private ScreenLogger	sl;
	
	/**
	 * Default constructor
	 * 
	 * @param fromDir		Origin directory path
	 * @param toDir			Target directory path
	 * @param screenLogger	ScreenLogger instance
	 */
	public FileCopier (Path fromDir, Path toDir, ScreenLogger screenLogger) {
		this.fromDir 	= fromDir;
		this.toDir		= toDir;
		
		sl 				= screenLogger;
	}
	
	/**
	 * Copies a file; recursively invoked
	 * 
	 * @param copyFrom		Origin file path
	 * @param copyTo		Target file path
	 * @throws IOException	Cannot copy the file
	 */
	public void copySubTree(Path copyFrom, Path copyTo) throws IOException {
		try {
			Files.copy(copyFrom, copyTo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
		} catch (IOException e) {
			sl.printMsg("File Copier", "Unable to copy: " + copyFrom.toString());
		}
	}

	/**
	 * Overwrites the LastModifiedTime attribute of the directory
	 */
	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc)
			throws IOException {
		if(exc == null) {
			Path newDir = toDir.resolve(fromDir.relativize((Path) dir));
			
			try {
				FileTime time = Files.getLastModifiedTime((Path) dir);
				Files.setLastModifiedTime(newDir, time);
			} catch (IOException e) {
				sl.printMsg("File Copier", "Unable to copy attributes to: " + newDir.toString());
			}
		}
		
		return FileVisitResult.CONTINUE;
	}

	/**
	 * Creates origin directory at target location
	 */
	@Override
	public FileVisitResult preVisitDirectory(Path dir,
			BasicFileAttributes attrs) throws IOException {
		
		Path newDir = toDir.resolve(fromDir.relativize((Path) dir));

		if(Files.notExists(newDir, LinkOption.NOFOLLOW_LINKS)) {
			try {
				Files.copy((Path) dir, newDir, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
			} catch (IOException e) {
				sl.printMsg("File Copier", "Unable to create: " + newDir.toString());
				return FileVisitResult.SKIP_SUBTREE;
			}
		}
		
		return FileVisitResult.CONTINUE;
	}

	/**
	 * Invokes the {@link #copySubTree(Path, Path)} method
	 */
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
			throws IOException {
		
		copySubTree ((Path) file, toDir.resolve(fromDir.relativize((Path) file)));
		
		return FileVisitResult.CONTINUE;
	}

	/**
	 * Handles file copy failure
	 */
	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc)
			throws IOException {
		
		if(exc instanceof FileSystemLoopException) {
			sl.printMsg("File Copier", "Cycle was detected: " + (Path) file);
		} else {
			sl.printMsg("File Copier", "Error occurred, unable to copy: " + (Path) file + "[" + exc + "]");
		}
		
		return FileVisitResult.CONTINUE;
	}

}