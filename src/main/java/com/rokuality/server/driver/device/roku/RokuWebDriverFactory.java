package com.rokuality.server.driver.device.roku;

import java.io.File;
import java.net.Socket;

import com.rokuality.server.constants.DependencyConstants;
import com.rokuality.server.constants.RokuWebDriverConstants;
import com.rokuality.server.core.CommandExecutor;
import com.rokuality.server.utils.FileUtils;
import com.rokuality.server.utils.LogFileUtils;
import com.rokuality.server.utils.OSUtils;
import com.rokuality.server.utils.SleepUtils;

import org.eclipse.jetty.util.log.Log;

public class RokuWebDriverFactory {

	public static boolean isRokuWebDriverRunning() {
		Socket socket = null;
		boolean running = false;
		try {
			socket = new Socket("localhost", 9000);
			running = true;
		} catch (Exception e) {
			running = false;
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (Exception e) {
				}
			}
		}
		return running;
	}

	public static boolean startRokuWebDriver() {
		File logFile = LogFileUtils.getLogFile("startrokuwebdriver.log");
		logFile = LogFileUtils.cleanLogFile(logFile);

		String userPath = OSUtils.getPathVar();
		String buildContent = "# !/bin/bash" + System.lineSeparator() + "export PATH=" + userPath
				+ System.lineSeparator() + "export GOPATH="
				+ RokuWebDriverConstants.ROKU_WEBDRIVER_BASE_DIR.getAbsolutePath() + System.lineSeparator() + "nohup "
				+ RokuWebDriverConstants.MAIN.getAbsolutePath() + " &>" + logFile.getAbsolutePath() + " &";
		File startFile = new File(
				DependencyConstants.TEMP_DIR.getAbsolutePath() + File.separator + "StartRokuWebDriver.sh");
		FileUtils.writeStringToFile(startFile, buildContent);
		new CommandExecutor().execCommand("bash " + startFile.getAbsolutePath(), null);

		long pollStart = System.currentTimeMillis();
		long pollMax = pollStart + 10 * 1000;

		while (System.currentTimeMillis() < pollMax) {
			if (isRokuWebDriverRunning()) {
				return true;
			}
			SleepUtils.sleep(250);
		}
		return false;
	}

	public static void stopRokuWebDriver() {
		CommandExecutor commandExecutor = new CommandExecutor();

		String command = null;
		if (OSUtils.isWindows()) {
			String wmicPath = OSUtils.getBinaryPath("WMIC");
			command = wmicPath + " path win32_process get Caption,Processid,Commandline";
		}

		if (OSUtils.isMac()) {
			command = "ps aux";
		}

		String[] processLns = commandExecutor.execCommand(command, null).split("\\r?\\n");
		for (String line : processLns) {
			if (line.contains(RokuWebDriverConstants.MAIN.getAbsolutePath())) {
				String processID = null;
				String killCommand = null;

				if (OSUtils.isWindows()) {
					String taskkill = OSUtils.getBinaryPath("taskkill");
					String[] entries = line.trim().split("\\s+");
					processID = entries[entries.length - 1];
					killCommand = taskkill + " /F /PID " + processID;
				}

				if (!OSUtils.isWindows()) {
					processID = line.trim().split("\\s+")[1];
					killCommand = "kill -9 " + processID;
				}

				Log.getRootLogger().info(String.format("Process id of Roku WebDriver stop: %s", processID));
				Log.getRootLogger().info(String.format("Command of Roku WebDriver stop: %s", killCommand));
				commandExecutor.execCommand(killCommand, null);
			}
		}
	}

}
