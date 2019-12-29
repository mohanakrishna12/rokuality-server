package com.rokuality.server.driver.device.roku;

import java.io.*;
import java.util.UUID;

import com.rokuality.server.constants.DependencyConstants;
import com.rokuality.server.core.CommandExecutor;
import com.rokuality.server.utils.FileUtils;
import com.rokuality.server.utils.LogFileUtils;
import com.rokuality.server.utils.OSUtils;

import org.eclipse.jetty.util.log.Log;

public class RokuDevConsoleManager {

	private String username = "";
	private String password = "";
	private String deviceip = "";
	private CommandExecutor commandExecutor = null;
	private String curlPath = "";

	public RokuDevConsoleManager(String deviceip, String username, String password) {
		this.username = username;
		this.password = password;
		this.deviceip = deviceip;
		this.commandExecutor = new CommandExecutor();
		this.curlPath = OSUtils.getBinaryPath("curl");
		if (!new File(this.curlPath).exists()) {
			Log.getRootLogger().info("DEBUG - CURL DOES NOT EXIST!");
		}
	}

	public boolean installRokuApp(String appPackage) {
		String command = curlPath + " -u " + username + ":" + password + " -v -F mysubmit=Install -F 'archive=@" + appPackage +
					"' http://" + deviceip + "/plugin_install --digest";
		String output = commandExecutor.execCommand(command, null);
		Log.getRootLogger().info("DEBUG - ROKU INSTALL COMMAND: " + commandExecutor.getCommand());
		Log.getRootLogger().info("DEBUG - ROKU INSTALL OUTPUT: " + output);
		return true; // TODO - just for debugging
	}

	public boolean uninstallRokuApp() {
		String command = curlPath + " -u " + username + ":" + password + " -v -F mysubmit=Delete -F 'archive= ' http://"
				+ deviceip + "/plugin_install --digest";

		String output = commandExecutor.execCommand(command, null);
		Log.getRootLogger().info("DEBUG - ROKU UNINSTALL COMMAND: " + commandExecutor.getCommand());
		Log.getRootLogger().info("DEBUG - ROKU UNINSTALL OUTPUT: " + output);
		return true; // TODO - just for debugging purposes
	}

	public File getScreenshot(File fileToSaveAs) {
		String takeCommand = curlPath + " -u " + username + ":" + password
						+ " -v -F mysubmit=Screenshot -F 'archive= ' -F 'passwd= '" + " http://" + deviceip
						+ "/plugin_inspect --digest";
		String output = commandExecutor.execCommand(takeCommand, null);
		Log.getRootLogger().info("DEBUG - ROKU TAKE SCREENSHOT COMMAND: " + commandExecutor.getCommand());
		Log.getRootLogger().info("DEBUG - ROKU TAKE SCREENSHOT OUTPUT: " + output);

		Long currentEpoch = (System.currentTimeMillis() - 3000) / 1000;
		String getCommand = "# !/bin/bash" + System.lineSeparator() + curlPath + " -u " + username + ":" + password + " http://" + deviceip
						+ "/pkgs/dev.jpg?time=" + currentEpoch.toString() + " --digest > " + fileToSaveAs.getAbsolutePath();
		File getScreenshotFile = new File(DependencyConstants.TEMP_DIR.getAbsolutePath() + File.separator + UUID.randomUUID().toString() + ".sh");
		String[] command = { "bash", getScreenshotFile.getAbsolutePath() };
		
		if (OSUtils.isWindows()) {
			getCommand = getCommand.replace("# !/bin/bash", "");
			File logStartFile = LogFileUtils.getLogFile("roku_getscreenshot.log");
				logStartFile = LogFileUtils.cleanLogFile(logStartFile);
				FileUtils.createFile(logStartFile);
				command = new String[] { "cmd", "/c", "start", "/b", "\"\"", ">" + logStartFile.getAbsolutePath(),
						getScreenshotFile.getAbsolutePath() };
		}
		
		FileUtils.writeStringToFile(getScreenshotFile, getCommand);
		
		output = commandExecutor.execCommand(String.join(" ", command), null);
		Log.getRootLogger().info("DEBUG - ROKU GET SCREENSHOT COMMAND: " + commandExecutor.getCommand());
		Log.getRootLogger().info("DEBUG - ROKU GET SCREENSHOT OUTPUT: " + output);
		FileUtils.deleteFile(getScreenshotFile);
		return fileToSaveAs;
	}

}
