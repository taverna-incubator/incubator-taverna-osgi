/*******************************************************************************
 * Copyright (C) 2013 The University of Manchester
 *
 *  Modifications to the initial code base are copyright of their
 *  respective authors, or their employers as appropriate.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 ******************************************************************************/
package net.sf.taverna.t2.maven.plugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authorization.AuthorizationException;

/**
 * @author David Withers
 */
public class Utils {

	public static String uploadFile(File file, String resourceName, Wagon wagon, Log log)
			throws MojoExecutionException {
		String resourceUrl = getResourceUrl(wagon, resourceName);
		File digestFile = new File(file.getPath() + ".md5");
		try {
			String digestString = DigestUtils.md5Hex(new FileInputStream(file));
			FileUtils.writeStringToFile(digestFile, digestString);
		} catch (IOException e) {
			throw new MojoExecutionException(
					String.format("Error generating digest for %1$s", file), e);
		}
		try {
			log.info(String.format("Uploading %1$s to %2$s", file, resourceUrl));
			wagon.put(file, resourceName);
			wagon.put(digestFile, resourceName + ".md5");
		} catch (TransferFailedException e) {
			throw new MojoExecutionException(String.format("Error transferring %1$s to %2$s", file,
					resourceUrl), e);
		} catch (ResourceDoesNotExistException e) {
			throw new MojoExecutionException(String.format("%1$s does not exist", resourceUrl), e);
		} catch (AuthorizationException e) {
			throw new MojoExecutionException(String.format(
					"Authentication error transferring %1$s to %2$s", file, resourceUrl), e);
		}
		return resourceUrl;
	}

	public static void downloadFile(String resourceName, File file, Wagon wagon, Log log)
			throws MojoExecutionException, ResourceDoesNotExistException {
		String resourceUrl = getResourceUrl(wagon, resourceName);
		File digestFile = new File(file.getPath() + ".md5");
		try {
			log.info(String.format("Downloading %1$s to %2$s", resourceUrl, file));
			wagon.get(resourceName, file);
			wagon.get(resourceName + ".md5", digestFile);
		} catch (TransferFailedException e) {
			throw new MojoExecutionException(String.format("Error transferring %1$s to %2$s",
					resourceUrl, file), e);
		} catch (AuthorizationException e) {
			throw new MojoExecutionException(String.format(
					"Authentication error transferring %1$s to %2$s", resourceUrl, file), e);
		}
		try {
			String digestString1 = DigestUtils.md5Hex(new FileInputStream(file));
			String digestString2 = FileUtils.readFileToString(digestFile);
			if (!digestString1.equals(digestString2)) {
				throw new MojoExecutionException(String.format(
						"Error downloading file: digsests not equal. (%1$s != %2$s)",
						digestString1, digestString2));
			}
		} catch (IOException e) {
			throw new MojoExecutionException(String.format("Error checking digest for %1$s", file),
					e);
		}
	}

	public static String getResourceUrl(Wagon wagon, String resourceName) {
		StringBuilder urlBuilder = new StringBuilder(wagon.getRepository().getUrl());
		for (String part : resourceName.split("/")) {
			urlBuilder.append('/');
			urlBuilder.append(URLEncoder.encode(part));
		}
		return urlBuilder.toString();
	}

	public static String timestamp() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmm");
		return dateFormat.format(new Date());
	}

	public static Set<String> getJavaPackages(Log log) {
		Set<String> javaPackages = new HashSet<String>();
		InputStream resource = Utils.class.getClassLoader().getResourceAsStream("java7-packages");
		if (resource != null) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(resource));
			try {
				String line = reader.readLine();
				while (line != null) {
					if (!line.isEmpty()) {
						javaPackages.add(line.trim());
					}
					line = reader.readLine();
				}
			} catch (IOException e) {
				log.warn(
						"Problem while reading to readinf java package list from resource file java7-packages",
						e);
			}
		} else {
			log.warn("Unable to read java package list from resource file java7-packages");
		}
		return javaPackages;
	}

	public static <T> List<Set<T>> getSubsets(Set<T> set) {
		List<Set<T>> subsets = new ArrayList<Set<T>>();
        List<T> list = new ArrayList<T>(set);
		int numOfSubsets = 1 << set.size();
		for (int i = 0; i < numOfSubsets; i++){
			Set<T> subset = new HashSet<T>();
		    for (int j = 0; j < numOfSubsets; j++){
		        if (((i>>j) & 1) == 1) {
		            subset.add(list.get(j));
		        }
		    }
		    if (!subset.isEmpty()) {
		    	subsets.add(subset);
		    }
		}
		Collections.sort(subsets, new Comparator<Set<T>>() {
			@Override
			public int compare(Set<T> o1, Set<T> o2) {
				return o1.size() - o2.size();
			}
		});
		return subsets;
	}

}
