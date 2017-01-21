/******************************************************************************
 * Copyright 2016, 2017 Octavio Calleya                                       *
 *                                                                            *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at                                    *
 *                                                                            *
 * http://www.apache.org/licenses/LICENSE-2.0                                 *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 *                                                                            *
 ******************************************************************************/

package com.transgressoft.util;

import java.io.*;
import java.math.*;
import java.nio.file.*;
import java.text.*;
import java.util.*;

/**
 * Class that does some useful operations with files, directories, strings
 * or other operations utilities to be used for the application
 *
 * @author Octavio Calleya
 *
 */
public class TransgressoftUtils {

	private TransgressoftUtils() {}

	/**
	 * Retrieves a {@link List} with at most <tt>maxFiles</tt> files that are in a folder or
	 * any of the subfolders in that folder satisfying a condition.
	 * If <tt>maxFilesRequired</tt> is 0 all the files will be retrieved.
	 *
	 * @param rootFolder       The folder from within to find the files
	 * @param filter           The {@link FileFilter} condition
	 * @param maxFilesRequired Maximum number of files in the List. 0 indicates no maximum
	 *
	 * @return The list containing all the files
	 *
	 * @throws IllegalArgumentException Thrown if <tt>maxFilesRequired</tt> argument is less than zero
	 */
	public static List<File> getAllFilesInFolder(File rootFolder, FileFilter filter, int maxFilesRequired) {
		List<File> finalFiles = new ArrayList<>();
		if (! Thread.currentThread().isInterrupted()) {
			if (maxFilesRequired < 0)
				throw new IllegalArgumentException("maxFilesRequired argument less than zero");
			if (rootFolder == null || filter == null)
				throw new IllegalArgumentException("folder or filter null");
			if (! rootFolder.exists() || ! rootFolder.isDirectory())
				throw new IllegalArgumentException("rootFolder argument is not a directory");

			int remainingFiles = addFilesDependingMax(finalFiles, rootFolder.listFiles(filter), maxFilesRequired);

			if (maxFilesRequired == 0 || remainingFiles > 0) {
				File[] rootSubFolders = rootFolder.listFiles(File::isDirectory);
				addFilesFromFolders(finalFiles, rootSubFolders, maxFilesRequired, remainingFiles, filter);
			}
		}
		return finalFiles;
	}

	/**
	 * Add files to a {@link List} depending a {@code maxFilesRequired} parameter.
	 * <ul>
	 *     <li>
	 *         If it's 0, all files are added.
	 *     </li>
	 *     <li>
	 *         If it's greater than the actual number of files, all files are added too.
	 *     </li>
	 *     <li>
	 *         If it's less than the actual number of files, the required number
	 *         of files are added
	 *     </li>
	 * </ul>
	 *
	 * @param files            The collection of final files
	 * @param subFiles         An Array of files to add to the collection
	 * @param maxFilesRequired The maximum number of files to add to the collection
	 *
	 * @return The remaining number of files to be added
	 */
	private static int addFilesDependingMax(List<File> files, File[] subFiles, int maxFilesRequired) {
		int remainingFiles = maxFilesRequired;
		if (maxFilesRequired == 0)    						// No max = add all files
			files.addAll(Arrays.asList(subFiles));
		else if (maxFilesRequired < subFiles.length) {    	// There are more valid files than the required
			files.addAll(Arrays.asList(Arrays.copyOfRange(subFiles, 0, maxFilesRequired)));
			remainingFiles -= files.size();        			// Zero files remaining in the folder
		}
		else if (subFiles.length > 0) {
			files.addAll(Arrays.asList(subFiles));   		// Add all valid files
			remainingFiles -= files.size();
		}
		return remainingFiles;
	}

	/**
	 * Adds files to a {@link List} from several folders depending of a maximum required files,
	 * the remaining files to be added, using a {@link FileFilter}.
	 *
	 * @param files 		   The collection of final files
	 * @param folders 		   The folders where the files are
	 * @param maxFilesRequired The maximum number of files to add to the collection
	 * @param remainingFiles   The remaining number of files to add
	 * @param filter 		   The {@link FileFilter} to use to filter the files in the folders
	 */
	private static void addFilesFromFolders(List<File> files, File[] folders, int maxFilesRequired, int remainingFiles, FileFilter filter) {
		int subFoldersCount = 0;
		int remaining = remainingFiles;
		while ((subFoldersCount < folders.length) && ! Thread.currentThread().isInterrupted()) {
			File subFolder = folders[subFoldersCount++];
			List<File> subFolderFiles = getAllFilesInFolder(subFolder, filter, remaining);
			files.addAll(subFolderFiles);
			if (remaining > 0)
				remaining = maxFilesRequired - files.size();
			if (maxFilesRequired > 0 && remaining == 0)
				break;
		}
	}

	/**
	 * Returns a {@link String} representing the given <tt>bytes</tt>, with a textual representation
	 * depending if the given amount can be represented as KB, MB, GB or TB
	 *
	 * @param bytes The <tt>bytes</tt> to be represented
	 * @return The <tt>String</tt> that represents the given bytes
	 * @throws IllegalArgumentException Thrown if <tt>bytes</tt> is negative
	 */
	public static String byteSizeString(long bytes) {
		if(bytes < 0)
			throw new IllegalArgumentException("Given bytes can't be less than zero");

		String sizeText;
		String[] bytesUnits = {"B", "KB", "MB", "GB", "TB"};
		long bytesAmount = bytes;
		short binRemainder;
		float decRemainder = 0;
		int u;
		for(u = 0; bytesAmount > 1024 && u < bytesUnits.length; u++) {
			bytesAmount /= 1024;
			binRemainder = (short) (bytesAmount % 1024);
			decRemainder += Float.valueOf((float) binRemainder / 1024);
		}
		String remainderStr = String.format("%f", decRemainder).substring(2);
		sizeText = bytesAmount + (remainderStr.equals("0") ? "" : ","+remainderStr) + " " + bytesUnits[u];
		return sizeText;
	}

	/**
	 * Returns a {@link String} representing the given <tt>bytes</tt>, with a textual representation
	 * depending if the given amount can be represented as KB, MB, GB or TB, limiting the number
	 * of decimals, if there are any
	 *
	 * @param bytes The <tt>bytes</tt> to be represented
	 * @param numDecimals The maximum number of decimals to be shown after the comma
	 * @return The <tt>String</tt> that represents the given bytes
	 * @throws IllegalArgumentException Thrown if <tt>bytes</tt> or <tt>numDecimals</tt> are negative
	 */
	public static String byteSizeString(long bytes, int numDecimals) {
		if(numDecimals < 0)
			throw new IllegalArgumentException("Given number of decimals can't be less than zero");

		String byteSizeString = byteSizeString(bytes);
		String decimalSharps = "";
		for(int n = 0; n < numDecimals; n++)
			decimalSharps += "#";
		DecimalFormat decimalFormat = new DecimalFormat("#." + decimalSharps);
		decimalFormat.setRoundingMode(RoundingMode.CEILING);

		int unitPos = byteSizeString.lastIndexOf(' ');
		String stringValue = byteSizeString.substring(0, unitPos);
		stringValue = stringValue.replace(',', '.');
		float floatValue = Float.parseFloat(stringValue);
		byteSizeString = decimalFormat.format(floatValue) + byteSizeString.substring(unitPos);
		return byteSizeString;
	}


	/**
	 * Ensures that the file name given is unique in the target directory, appending
	 * (1), (2)... (n+1) to the file name in case it already exists
	 *
	 * @param fileName The string of the file name
	 * @param targetPath The path to check if there is a file with the name equals <tt>fileName</tt>
	 * @return The modified string
	 */
	public static String ensureFileNameOnPath(Path targetPath, String fileName) {
		String newName = fileName;
		if(targetPath.resolve(fileName).toFile().exists()) {
			int pos = fileName.lastIndexOf('.');
			newName = fileName.substring(0, pos) + "(1)." + fileName.substring(pos+1);
		}
		while(targetPath.resolve(newName).toFile().exists()) {
			int posL = newName.lastIndexOf('(');
			int posR = newName.lastIndexOf(')');
			int num = Integer.parseInt(newName.substring(posL + 1, posR));
			newName = newName.substring(0, posL + 1) + ++num + newName.substring(posR);
		}
		return newName;
	}


	/**
	 * This class implements <code>{@link java.io.FileFilter}</code> to
	 * accept a file with some of the given extensionsToFilter. If no extensionsToFilter are given
	 * the file is accepted. The extensionsToFilter must be given without the dot.
	 *
	 * @author Octavio Calleya
	 *
	 */
	public static class ExtensionFileFilter implements FileFilter {

		private String[] extensionsToFilter;
		private int numExtensions;

		public ExtensionFileFilter(String... extensionsToFilter) {
			this.extensionsToFilter = extensionsToFilter;
			numExtensions = extensionsToFilter.length;
		}

		public ExtensionFileFilter() {
			extensionsToFilter = new String[] {};
			numExtensions = 0;
		}

		public void addExtension(String extension) {
			boolean contains = false;
			for(String someExtension: extensionsToFilter)
				if(someExtension != null && extension.equals(someExtension))
					contains = true;
			if(!contains) {
				ensureArrayLength();
				extensionsToFilter[numExtensions++] = extension;
			}
		}

		public void removeExtension(String extension) {
			for(int i = 0; i< extensionsToFilter.length; i++)
				if(extensionsToFilter[i].equals(extension)) {
					extensionsToFilter[i] = null;
					numExtensions--;
				}
			extensionsToFilter = Arrays.copyOf(extensionsToFilter, numExtensions);
		}

		public boolean hasExtension(String extension) {
			for(String someExtension: extensionsToFilter)
				if(extension.equals(someExtension))
					return true;
			return false;
		}

		public void setExtensionsToFilter(String... extensionsToFilter) {
			if(extensionsToFilter == null)
				this.extensionsToFilter = new String[] {};
			else
				this.extensionsToFilter = extensionsToFilter;
			numExtensions = this.extensionsToFilter.length;
		}

		public String[] getExtensionsToFilter() {
			return extensionsToFilter;
		}

		private void ensureArrayLength() {
			if(numExtensions == extensionsToFilter.length)
				extensionsToFilter = Arrays.copyOf(extensionsToFilter, numExtensions == 0 ? 1 : 2 * numExtensions);

		}

		@Override
		public boolean accept(File pathname) {
			boolean res = false;
			if(!pathname.isDirectory() && !pathname.isHidden()) {
				int pos = pathname.getName().lastIndexOf('.');
				if(pos != -1) {
					String extension = pathname.getName().substring(pos + 1);
					if(numExtensions == 0)
						res = true;
					else
						res = hasExtension(extension);
				}
			}
			return res;
		}
	}
}