package flame;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * FileUtility class has helper methods that handle simulation-related files.
 * 
 * @author 					<a href="mailto:jaeyounb@usc.edu">Jae young Bang</a>
 * @version					2014.12
 */
public class FileUtility {
	
	/**
	 * Traverses a directory to find the list of files with the target extension
	 * 
	 * @param directory				Directory to traverse
	 * @param targetExtension		Target extension to look for
	 * @return						List of files in the directory with the target extension
	 * @throws IOException
	 */
	public static List<Path> findFilesWithExtension (Path directory, String targetExtension) throws IOException {
		List<Path> files = new ArrayList<>();
		
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(directory)) {
			for (Iterator<Path> it = ds.iterator(); it.hasNext(); ) {
				// Gets a file path
				Path 	file 		= (Path) it.next();
				String 	extension	= getExtension(file).toLowerCase(); 

				// If the extension is .h, adds it to the header files list
				if(extension.equals(targetExtension.toLowerCase())) {
					files.add(file);
				} else {
					continue;
				}
			}
		} 
		
		return files;
	}
	
	/**
	 * Gets the extension of a file
	 * 
	 * @param file			Path to the file
	 * @return				Extension
	 * @throws Exception	File not have extension
	 */
	public static String getExtension (Path file) {
		String fullFilename	= file.getFileName().toString();
		String[] tokens 	= fullFilename.split("\\.");
		
		// if the file does not have an extension
		if(tokens.length <= 1) {
			return fullFilename;
		}
		
		return tokens[tokens.length - 1];
	}
	
	/**
	 * Gets the filename without the extension
	 * 
	 * @param file			Path to the file
	 * @return				Filename without extension
	 * @throws Exception	File not have extension
	 */
	public static String getFilenameWithoutExtension (Path file) {
		return getFilenameWithoutExtension(file.getFileName().toString());
	}
	
	/**
	 * Gets the filename without the extension
	 * 
	 * @param fullFilename	Full filename
	 * @return				Filename without extension
	 * @throws Exception	File not have extension
	 */
	public static String getFilenameWithoutExtension (String fullFilename) {
		String[] tokens 	= fullFilename.split("\\.");
		
		// if the file does not have an extension
		if(tokens.length <= 1) {
			return fullFilename;
		}
		
		String filename = new String ();
		for(int i=0; i < tokens.length-1; i++) {
			filename += tokens[i];
		}
		
		return filename;
	}
}
