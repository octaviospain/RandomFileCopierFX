/**
 * Copyright 2016 Octavio Calleya
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.randomfilecopier;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.docopt.Docopt;

/**
 * This class copies random files that are located in a folder and it
 * subsequent folders to a destination, supplying options such as limiting
 * the number of files, the total space to copy, or filtering the files by its extension.
 * 
 * @version 0.1
 * @author Octavio Calleya
 *
 */
public class RandomFileCopier {
	
	
	private File sourceDir, targetDir;
	private int maxFiles;
	private long maxBytes, copiedBytes;
	private List<File> randomFiles;
	private ExtensionFileFilter filter;
	private boolean verbose;
	private Random rnd;
	private PrintStream out;
	
	/**
	 * Constructor for a <tt>RandomFileCopier</tt> object 
	 * 
	 * @param sourcePath The source folder where the desired files are
	 * @param targetPath The target folder to copy the files
	 * @param maxFiles The maximum number of files to copy. 0 will copy all the files
	 */
	public RandomFileCopier(String sourcePath, String targetPath, int maxFiles) {
		sourceDir = new File(sourcePath);
		targetDir = new File(targetPath.startsWith("/") ? targetPath : "/"+ targetPath);
		this.maxFiles = maxFiles;
		verbose = false;
		rnd = new Random();
		randomFiles = new ArrayList<>();
		out = System.out;
		maxBytes = targetDir.getUsableSpace();		
		filter = new ExtensionFileFilter();
		copiedBytes = 0;
	}
	
	/**
	 * Constructor for a <tt>RandomFileCopier</tt> object
	 * 
	 * @param sourcePath The source folder where the desired files are
	 * @param targetPath The target folder to copy the files
	 * @param maxFiles The maximum number of files to copy. 0 will copy all the files
	 * @param output The OutputStream where the log messages will be printed
	 */
	public RandomFileCopier(String sourcePath, String targetPath, int maxFiles, PrintStream output) {
		this(sourcePath, targetPath, maxFiles);
		out = output;
	}

	/**
	 * Sets the extensions that the files must match to be copied
	 * @param extensions A String array containing the extensions without the initial dot '.'
	 */
	public void setFilterExtensions(String... extensions) {
		filter.setExtensions(extensions);
	}
	
	public String[] getFilterExtensions() {
		return filter.getExtensions();
	}

	/**
	 * Sets the maximum number of bytes that should be copied to the destination.
	 * @param maxBytesToCopy
	 */
	public void setMaxBytesToCopy(long maxBytesToCopy) {
		File root = targetDir;
		while(root.getParentFile() != null) 
				root = root.getParentFile();
		if(maxBytesToCopy < root.getUsableSpace())
			maxBytes = maxBytesToCopy;
		else
			maxBytes = root.getUsableSpace();
	}
	
	public long getMaxBytesToCopy() {
		return maxBytes <= targetDir.getUsableSpace() ? maxBytes : targetDir.getUsableSpace();
	}
	
	/**
	 * Sets if the application should print to the standard or given output some useful info
	 * @param verbose
	 */
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	
	/**
	 * Copies random files from a source path to a target path
	 * up to a maximum number satisfying a file filter condition
	 * 
	 * @throws IOException 
	 */
	public void randomCopy() throws IOException {
		getRandomFilesInFolderTree();
		copyFiles();
	}
	
	/**
	 * Scans the source folder and its subfolders to collect the files satisfying
	 * the given conditions and selects randomly a certain number of them
	 */
	private void getRandomFilesInFolderTree() {
		copiedBytes = 0;
		randomFiles.clear();
		out.println("Scanning source directory... ");
		List<File> allFiles = getAllFilesInFolder(sourceDir, filter, 0);
		long allFilesBytes = allFiles.stream().mapToLong(File::length).sum();
		out.println(allFiles.size() + " files found");
		int selectedIndex;
		if(maxFiles >= allFiles.size() || maxFiles == 0) {	// if all files should be copied regarding the maxFiles constraint
			if(allFilesBytes > getMaxBytesToCopy())				// checks the maximum bytes required to be copied
				while(allFilesBytes > getMaxBytesToCopy()) {
					selectedIndex = rnd.nextInt(allFiles.size());
					allFilesBytes -= allFiles.get(selectedIndex).length();
					allFiles.remove(selectedIndex);
				}
			copiedBytes = allFilesBytes;
			randomFiles.addAll(allFiles);
		} else {											// if a certain number of files should be copied
			while (randomFiles.size() < maxFiles && !allFiles.isEmpty()) {
				File selectedFile = allFiles.get(rnd.nextInt(allFiles.size()));
				long selectedFileSize = selectedFile.length();
				if(selectedFileSize + copiedBytes <= getMaxBytesToCopy()) {		// checking the maximum bytes to be copied
					randomFiles.add(selectedFile);
					copiedBytes += selectedFileSize;	
				}
				allFiles.remove(selectedFile);
			}
		}
	}
	
	/**
	 * Copies the randomly selected files to the target destination
	 * Renames duplicated files to ensure that files with the same name are not overwritten
	 * 
	 * @throws IOException 
	 */
	private void copyFiles() throws IOException {
		CopyOption[] options = new CopyOption[]{COPY_ATTRIBUTES};
		out.println("Copying files to target directory... ");
		for(File f: randomFiles)
			if(!Thread.currentThread().isInterrupted())	{
				Path filePath = f.toPath();
				Files.copy(filePath, targetDir.toPath().resolve(ensureFileName(f.getName())), options);
				if(verbose)
					out.println("Copied "+".../"+filePath.subpath(filePath.getNameCount()-3, filePath.getNameCount()) +" [" + f.length() + " bytes]");
			}
		out.println("Done. " + copiedBytes + " bytes copied");
	}
	
	/**
	 * Ensures that the file name given is unique in the target directory, appending
	 * (1), (2)... (n+1) to the file name in case it already exists
	 * @param name The string of the file name
	 * @return The modified string
	 */
	private String ensureFileName(String name) {
		String newName = name;
		if(targetDir.toPath().resolve(name).toFile().exists()) {
			int pos = name.lastIndexOf(".");
			newName = name.substring(0, pos) + "(1)." + name.substring(pos+1);
		}
		while(targetDir.toPath().resolve(newName).toFile().exists()) {
			int posL = newName.lastIndexOf("(");
			int posR = newName.lastIndexOf(")");
			int num = Integer.parseInt(newName.substring(posL+1, posR));
			newName = newName.substring(0, posL+1) + ++num +newName.substring(posR);
		}
		return newName;
	}

	/**
	 * Retrieves a list with at most <tt>maxFiles</tt> files that are in a folder or
	 * any of the subfolders in that folder satisfying a condition.
	 * If <tt>maxFilesRequired</tt> is 0 all the files will be retrieved.
	 * 
	 * @param rootFolder The folder from within to find the files
	 * @param filter The FileFilter condition
	 * @param maxFilesRequired Maximun number of files in the List. 0 indicates no maximum
	 * @return The list containing all the files
	 * @throws IllegalArgumentException Thrown if <tt>maxFilesRequired</tt> argument is less than zero
	 */
	private List<File> getAllFilesInFolder(File rootFolder, FileFilter filter, int maxFilesRequired) throws IllegalArgumentException {
		List<File> finalFiles = new ArrayList<>();
		if(!Thread.currentThread().isInterrupted()) {
			if(maxFilesRequired < 0)
				throw new IllegalArgumentException("maxFilesRequired argument less than zero");
			if(rootFolder == null || filter == null)
				throw new IllegalArgumentException("folder or filter null");
			if(!rootFolder.exists() || !rootFolder.isDirectory())
				throw new IllegalArgumentException("rootFolder argument is not a directory");
			File[] subFiles = rootFolder.listFiles(filter);
			int remainingFiles = maxFilesRequired;
			if(maxFilesRequired == 0)	// No max = add all files
				finalFiles.addAll(Arrays.asList(subFiles));
			else if(maxFilesRequired < subFiles.length) {	// There are more valid files than the required
					finalFiles.addAll(Arrays.asList(Arrays.copyOfRange(subFiles, 0, maxFilesRequired)));
					remainingFiles -= finalFiles.size();		// Zero files remaining
				}
			else if (subFiles.length > 0) {
						finalFiles.addAll(Arrays.asList(subFiles));	// Add all valid files
						remainingFiles -= finalFiles.size();		// If remainingFiles == 0, end;
					}
			
			if(maxFilesRequired == 0 || remainingFiles > 0) {
				File[] rootSubFolders = rootFolder.listFiles(file -> {return file.isDirectory();});
				int sbFldrsCount = 0;
				while((sbFldrsCount < rootSubFolders.length) && !Thread.currentThread().isInterrupted()) {
					File subFolder = rootSubFolders[sbFldrsCount++];
					List<File> subFolderFiles = getAllFilesInFolder(subFolder, filter, remainingFiles);
					finalFiles.addAll(subFolderFiles);
					if(remainingFiles > 0)
						remainingFiles = maxFilesRequired - finalFiles.size();
					if(maxFilesRequired > 0 && remainingFiles == 0)
						break;
				}
			}
		}
		return finalFiles;
	}
	
	/**
	 * This class implements <code>{@link java.io.FileFilter}</code> to
	 * accept a file with some of the given extensions. If no extensions are given
	 * the file is accepted. The extensions must be given without the dot.
	 * 
	 * @author Octavio Calleya
	 *
	 */
	public class ExtensionFileFilter implements FileFilter {
		
		private String[] extensions;
		private int numExtensions;
		
		public ExtensionFileFilter(String... extensions) {
			this.extensions = extensions;
			numExtensions = extensions.length;
		}
		
		public ExtensionFileFilter() {
			extensions = new String[] {};
			numExtensions = 0;
		}
		
		public void addExtension(String ext) {
			boolean contains = false;
			for(String e: extensions)
				if(e != null && ext.equals(e))
					contains = true;
			if(!contains) {
				ensureArrayLength();
				extensions[numExtensions++] = ext;
			}
		}
		
		public void removeExtension(String ext) {
			for(int i=0; i<extensions.length; i++)
				if(extensions[i].equals(ext)) {
					extensions[i] = null;
					numExtensions--;
				}
			extensions = Arrays.copyOf(extensions, numExtensions);
		}
		
		public boolean hasExtension(String ext) {
			for(String e: extensions)
				if(ext.equals(e))
					return true;
			return false;
		}
		
		public void setExtensions(String... extensions) {
			if(extensions == null)
				this.extensions = new String[] {};
			else
				this.extensions = extensions;
			numExtensions = this.extensions.length;
		}
		
		public String[] getExtensions() {
			return extensions;
		}
		
		private void ensureArrayLength() {
			if(numExtensions == extensions.length)
				extensions = Arrays.copyOf(extensions, numExtensions == 0 ? 1 : 2*numExtensions);
			
		}

		@Override
		public boolean accept(File pathname) {
			boolean res = false;
			if(!pathname.isDirectory() && !pathname.isHidden()) {
				int pos = pathname.getName().lastIndexOf(".");
				if(pos != -1) {
					String extension = pathname.getName().substring(pos+1);
					if(numExtensions == 0)
						res = true;
					else
						for(String requiredExtension: extensions)
							if(extension.equals(requiredExtension))
								res = true;
				}
			}
			return res;
		}		
	}
	
	/***************************		Command line usage		***************************/
	
	private static final String DOC = "Random File Copier.\n\n"+
	                                  "Usage:\n"+
	                                  "  RandomFileCopier <source_directory> <target_directory> <max_files> [-v] [-s=<maxbytes>] [-e=<extension>]...\n\n"+
	                                  "Options:\n"+
	                                  "  -h, --help  Show this help text.\n"+
	                                  "  <max_files>  The maximum number of files.\n"+
	                                  "  -v, --verbose  Show some extra information of the process.\n"+
	                                  "  -e, --extension=<extension> Extension required to the file.\n"+
	                                  "  -s, --space=<maxbytes> Max bytes to copy in the destination.\n";
	private static File src, tgt;
	private static int max;

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {
		Map<String, Object> opts = new Docopt(DOC).withVersion("Random File Copier 0.1").parse(args);
		String srcString = (String) opts.get("<source_directory>");
		String tgtString = (String) opts.get("<target_directory>");
		String maxString = (String) opts.get("<max_files>");
		boolean verbose = (Boolean) opts.get("--verbose");
		
		List<String> extensionsList = (List<String>) opts.get("--extension");
		String[] extensions = Arrays.stream(extensionsList.toArray()).map(s -> ((String)s).substring(1)).toArray(String[]::new);

		String maxBytesString = ((String) opts.get("--space"));
		long maxBytes = 0;
		if(maxBytesString != null)
			maxBytes = Long.valueOf(maxBytesString.substring(1));
		
		if(validDirectories(srcString, tgtString) && validMaxFiles(maxString)) {
			RandomFileCopier copier = new RandomFileCopier(src.toString(), tgt.toString(), max);
			copier.setVerbose(verbose);
			copier.setFilterExtensions(extensions);
			if(maxBytes > 0)
				copier.setMaxBytesToCopy(maxBytes);
			copier.randomCopy();
		}
	}
	
	private static boolean validDirectories(String arg0, String arg1) {
		boolean res = false;
		src = new File(arg0);
		tgt = new File(arg1);
		if(!tgt.exists())
			tgt.mkdir();
		if(!src.exists())
			printUsage("Source path doesn't exist");
		else if(!src.isDirectory())
			printUsage("Source path is not a directory");
		else if(tgt.exists() && !tgt.isDirectory())
			printUsage("Target path is not a directory");
		else
			res = true;
		if(src.equals(tgt)) {
			printUsage("Source and target directory are the same");
			res = false;
		}
		return res;
	}
	
	private static boolean validMaxFiles(String arg2) {
		boolean res = false;
		try {
			max = Integer.parseInt(arg2);
			if(max >= 0 || max <= Integer.MAX_VALUE)
				res = true;
			else
				printUsage("MaxFiles must be between 0 and "+Integer.MAX_VALUE+" inclusives");
		} catch (NumberFormatException e) {}
		if(!res)
			printUsage("Invalid arguments");
		return res;
	}
	
	private static void printUsage(String detail) {
		System.out.println("ERROR: " + detail + "\n\n" + DOC);
	}
}