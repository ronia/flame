package flame;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * ArchiveUtility class has helper methods that handle ZIP archives.
 * 
 * @author 					<a href="mailto:jaeyounb@usc.edu">Jae young Bang</a>
 * @version					2014.12
 */
public class ArchiveUtility {
	protected static final int bufferSize = 1024;
	
	/**
	 * Extracts a ZIP archive into the target directory
	 * 
	 * @param archive				Path to the archive to extract
	 * @param targetDirectory		Directory to which the files will be extracted
	 * @throws Exception
	 */
	public static synchronized void extract (Path archive, Path targetDirectory) throws Exception {
		System.out.println("Unzipping " + archive.getFileName().toString() + "...");
		byte[] buffer = new byte[bufferSize];
		
		// extracts the source code archive to the target directory
		try (ZipInputStream zin = new ZipInputStream(new FileInputStream(archive.toFile()))) {
			ZipEntry 		ze;
			while ((ze = zin.getNextEntry()) != null) {
				Path extractedFile = targetDirectory.resolve(ze.getName());
				try (FileOutputStream fout = new FileOutputStream(extractedFile.toFile())) {
					
					int len;
					while ((len = zin.read(buffer)) > 0) {
						fout.write(buffer, 0, len);
					}
			      
					zin.closeEntry();
				} catch (IOException ioe_inner) {
					throw new Exception ("Error while extracting a file from the archive: " + extractedFile);
				}
			}
		} catch (IOException ioe_outter) {
			throw new Exception ("Error while extracting " + archive);
		}
		System.out.println("Unzipping " + archive.getFileName().toString() + " done.");
	}
	
	/**
	 * Archives a list of target files into a ZIP archive
	 * 
	 * @param archive			Path to the archive
	 * @param targetFiles		List of files to put into the archive
	 * @throws Exception
	 */
	public static void archive (Path archive, List<Path> targetFiles) throws Exception {
		// creates the archive
		byte[] buffer = new byte[bufferSize];
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(archive.toFile()))) {

			//	Loop through each file
			for(Path file : targetFiles) {
				ZipEntry ze = new ZipEntry(file.getFileName().toString());
				zos.putNextEntry(ze);
				try (FileInputStream in = new FileInputStream(file.toFile())) {
					int len;
					while((len = in.read(buffer)) > 0) {
						zos.write(buffer, 0, len);
					}
				}  catch (IOException ioe_inner) {
					throw new Exception ("Error while writing a file into the archive: " + file.getFileName());
				}
				zos.closeEntry();
			}
		} catch (IOException ioe_outter) {
			throw new Exception ("Error while archiving " + archive.getFileName());
		}
	}
}
