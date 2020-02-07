package com.rokuality.server.driver.device.roku;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.rokuality.server.constants.DependencyConstants;
import com.rokuality.server.utils.FileUtils;

import org.apache.commons.net.telnet.TelnetClient;
import org.eclipse.jetty.util.log.Log;

public class RokuLogManager {

	private static final int TIMEOUT = 5000;
	private static final int DEFAULT_PORT = 8085;

	private static Map<String, TelnetClient> telnetMap = new ConcurrentHashMap<String, TelnetClient>();
	private static Map<String, FileOutputStream> outputStreamMap = new ConcurrentHashMap<String, FileOutputStream>();

	public static void startLogCapture(String deviceIP) {
		File logFile = getLogFile(deviceIP);
		FileUtils.cleanFile(logFile);
		Log.getRootLogger().info("Initiating Roku debug log capture at: " + logFile.getAbsolutePath());

		TelnetClient client = new TelnetClient();

		FileOutputStream fileOutputStream = null;

		try {
			fileOutputStream = new FileOutputStream(logFile);
		} catch (Exception e) {
			Log.getRootLogger().warn(String.format("Failed to initiate log capture for device %s", deviceIP));
			Log.getRootLogger().warn(e);
			return;
		}

		client.registerSpyStream(fileOutputStream);
		client.setConnectTimeout(TIMEOUT);

		try {
			client.connect(deviceIP, DEFAULT_PORT);
		} catch (Exception e) {
			Log.getRootLogger().warn(String.format("Failed to initiate log capture for device %s", deviceIP));
			Log.getRootLogger().warn(e);
			return;
		}

		outputStreamMap.put(deviceIP, fileOutputStream);
		telnetMap.put(deviceIP, client);
	}

	public static void stopLogCapture(String deviceIP) {
		TelnetClient client = telnetMap.get(deviceIP);
		FileOutputStream fileOutputStream = outputStreamMap.get(deviceIP);

		if (client != null) {
			Log.getRootLogger().warn(String.format("Stopping Roku log capture for device %s", deviceIP));
			try {
				client.disconnect();
			} catch (Exception e) {
				Log.getRootLogger().warn(e);
			}
			telnetMap.remove(deviceIP);
		}

		if (fileOutputStream != null) {
			try {
				fileOutputStream.close();
			} catch (Exception e) {
				Log.getRootLogger().warn(e);
			}
		}
	}

	public static String getLogContent(String deviceIP) {
		FileOutputStream fileOutputStream = outputStreamMap.get(deviceIP);
		try {
			fileOutputStream.flush();
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}
		return FileUtils.readStringFromFile(getLogFile(deviceIP));
	}

	private static File getLogFile(String deviceIP) {
		return new File(
				DependencyConstants.TEMP_DIR.getAbsolutePath() + File.separator + deviceIP + "_" + "roku_debug.log");
	}

}
