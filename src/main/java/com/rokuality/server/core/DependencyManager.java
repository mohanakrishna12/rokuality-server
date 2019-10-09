package com.rokuality.server.core;

import java.io.File;
import java.net.URL;

import com.rokuality.server.constants.DependencyConstants;
import com.rokuality.server.utils.ZipUtils;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.util.log.Log;

public class DependencyManager {

	public static final int DEFAULT_DOWNLOAD_TIMEOUT_S = 180;

	public boolean downloadDependency(File targetFile) {
		String fileUrl = DependencyConstants.DEPENDENCY_URL + targetFile.getName();
		Log.getRootLogger().info("Downloading dependency from: " + fileUrl, new Object[] {});

		boolean downloadSuccess = false;

		try {
			FileUtils.copyURLToFile(
				new URL(fileUrl), 
				targetFile, 
				(DEFAULT_DOWNLOAD_TIMEOUT_S * 1000), 
				(DEFAULT_DOWNLOAD_TIMEOUT_S * 1000));

			downloadSuccess = targetFile.exists();
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}
		
		if (downloadSuccess) {
			Log.getRootLogger().info("Dependency saved to '" + targetFile.getAbsolutePath() + "'.");
		}

		return downloadSuccess;
	}

	public void unzipDependency(File dependencyZipFile) {
		ZipUtils.unzipZipFile(dependencyZipFile.getAbsolutePath(), dependencyZipFile.getParent());
		com.rokuality.server.utils.FileUtils.deleteFile(dependencyZipFile);
	}

}