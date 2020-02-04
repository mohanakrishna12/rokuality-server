package com.rokuality.server.driver.device.xbox;

import java.io.File;
import java.net.Socket;

import com.rokuality.server.constants.DependencyConstants;
import com.rokuality.server.core.CommandExecutor;
import com.rokuality.server.utils.FileUtils;
import com.rokuality.server.utils.LogFileUtils;
import com.rokuality.server.utils.OSUtils;
import com.rokuality.server.utils.SleepUtils;

import org.eclipse.jetty.util.log.Log;

public class XBoxSmartglassFactory {

	public static final int PORT = 5557;
	private static final String XBOX_REST_SERVER = "xbox-rest-server";

	public static boolean isServerRunning() {
		Socket socket = null;
		boolean running = false;
		try {
			socket = new Socket("localhost", PORT);
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

	public static boolean startServer() {
		CommandExecutor commandExecutor = new CommandExecutor();
		commandExecutor.setWaitToComplete(false);

		File logFile = LogFileUtils.getLogFile("xboxsmartglassserver.log");
		logFile = LogFileUtils.cleanLogFile(logFile);

		String[] command = null;
		File startFile = new File(
				DependencyConstants.TEMP_DIR.getAbsolutePath() + File.separator + "StartXBoxSmartglassServer.sh");
		String startContent = "";

		if (OSUtils.isWindows()) {
			startFile = new File(startFile.getAbsolutePath().replace(".sh", ".bat"));
			startContent = XBOX_REST_SERVER + ".exe -l " + logFile.getAbsolutePath();

			File logStartFile = LogFileUtils.getLogFile("startxboxsmartglassserver.log");
			logStartFile = LogFileUtils.cleanLogFile(logStartFile);
			FileUtils.createFile(logStartFile);
			command = new String[] { "cmd", "/c", "start", "/b", "\"\"", ">" + logStartFile.getAbsolutePath(),
					startFile.getAbsolutePath() };
		} else {
			String userPath = OSUtils.getPathVar();
			startContent = "# !/bin/bash" + System.lineSeparator() + "export PATH=" + userPath + System.lineSeparator()
					+ "nohup " + XBOX_REST_SERVER + " -l " + logFile.getAbsolutePath() + " &";
			command = new String[] { "bash", startFile.getAbsolutePath() };
		}
		FileUtils.writeStringToFile(startFile, startContent);
		
		commandExecutor.execCommand(String.join(" ", command), null);

		long pollStart = System.currentTimeMillis();
		long pollMax = pollStart + 10 * 1000;

		while (System.currentTimeMillis() < pollMax) {
			if (isServerRunning()) {
				return true;
			}
			SleepUtils.sleep(250);
		}
		return false;
	}

	public static void stopServer() {
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
			if (line.contains(XBOX_REST_SERVER)) {
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

				Log.getRootLogger().info(String.format("Process id of XBox smartglass server stop: %s", processID));
				Log.getRootLogger().info(String.format("Command of XBox smartglass server stop: %s", killCommand));
				commandExecutor.execCommand(killCommand, null);
			}
		}
	}

}

