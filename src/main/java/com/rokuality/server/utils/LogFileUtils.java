package com.rokuality.server.utils;

import java.io.File;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.rokuality.server.constants.LogConstants;

import org.eclipse.jetty.util.log.Log;

public class LogFileUtils {

	private static final int MAX_DAYS_IN_SEC = 432000; // 5 DAYS

	public static File getLogFile(String baseFileName, boolean... inUse) {
		String cacheProp = OSUtils.getOSType().toString() + "-" + baseFileName + "-log-file";
		String cacheValue = CacheUtils.get(cacheProp);

		boolean forceNewLog = false;
		if (inUse.length > 0) {
			forceNewLog = inUse[0];
		}
		
		if (cacheValue != null && !forceNewLog) {
			File cachedLogFile = new File(cacheValue);
			if (cachedLogFile.exists()) {
				Log.getRootLogger().info("Cached log file exists at: " + cachedLogFile.getAbsolutePath(), new Object[] {});
				return cachedLogFile;
			}
		}

		DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd");
		Date date = new Date();
		String dateString = dateFormat.format(date);

		String forceNewModifiedComp = "";
		if (forceNewLog) {
			forceNewModifiedComp = System.currentTimeMillis() + "_";
		}

		File todaysLogFile = new File(LogConstants.LOG_DIR.getAbsolutePath() 
				+ File.separator + dateString + "_" + forceNewModifiedComp + baseFileName);

		// delete any previous log files older than x days
		for (File logFile : LogConstants.LOG_DIR.listFiles()) {

			if (logFile.getName().contains(baseFileName)) {
				Long timeLastModified = logFile.lastModified();
				Long currentTime = System.currentTimeMillis();
				Long ageInSec = ((currentTime - timeLastModified) / 1000);

				if (ageInSec > MAX_DAYS_IN_SEC) {
					Log.getRootLogger().info("Deleting legacy file: " + logFile.getAbsolutePath(), new Object[] {});
					FileUtils.deleteFile(logFile);
				}
			}
		}

		if (!todaysLogFile.exists()) {
			Log.getRootLogger().info("Creating new log file at: " + todaysLogFile.getAbsolutePath(), new Object[] {});
			FileUtils.createFile(todaysLogFile);
		}

		if (todaysLogFile.exists()) {
			Log.getRootLogger().info("Caching log file at: " + todaysLogFile.getAbsolutePath(), new Object[] {});
			CacheUtils.add(cacheProp, todaysLogFile.getAbsolutePath());
		}
		
		return todaysLogFile;
	}

	public static File cleanLogFile(File logFile) {
		if (logFile == null || !FileUtils.isSafeFileAction(logFile)) {
			Log.getRootLogger().warn("Not cleaning unsafe log file", logFile);
			return null;
		}
		Log.getRootLogger().info("Cleaning log file at: " + logFile.getAbsolutePath(), new Object[] {});
		try (PrintWriter printWriter = new PrintWriter(logFile)) {
			printWriter.print("");
		} catch (Exception e) {
			if (e.getMessage().contains("The process cannot access the file because it is being used by another process")) {
				String fileName = logFile.getName();
				if (fileName.contains("_")) {
					int index = fileName.lastIndexOf("_");
					fileName = fileName.substring(index + 1, fileName.length());
				}
				Log.getRootLogger().warn("Log file in use - re-seeding...", new Object[] {});
				File reseededLogFile = getLogFile(fileName, true);
				Log.getRootLogger().warn("Reseeded log file: " + reseededLogFile.getAbsolutePath(), new Object[] {});
				return reseededLogFile;
			}
			Log.getRootLogger().warn(e);
		}

		return logFile;
	}


}
