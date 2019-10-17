package com.rokuality.server.driver.device.hdmi;

import java.io.File;

import com.rokuality.server.constants.DependencyConstants;
import com.rokuality.server.core.CommandExecutor;
import com.rokuality.server.utils.OSUtils;

import org.eclipse.jetty.util.log.Log;

public class HDMIKeyPresser {

	private static final int DEFAULT_TIMEOUT = 30;

	private String homeHubDeviceIP = null;
	private String deviceName = null;
	
	public HDMIKeyPresser(String homeHubDeviceIP, String deviceName) {
		this.homeHubDeviceIP = homeHubDeviceIP;
		this.deviceName = deviceName;
	}

	public boolean pressButton(String button) {
		File node = new File(OSUtils.getBinaryPath("node"));
		if (!node.exists()) {
			Log.getRootLogger().warn("Node was not found in user's path. Please install!");
			return false;
		}

		String command = node.getAbsolutePath() + " " + DependencyConstants.HARMONY_BIN.getAbsolutePath() + " -l " + homeHubDeviceIP
				+ " -d " + "\"" + deviceName + "\"" + " -c " + button;
		Log.getRootLogger().info(String.format("Sending %s command to harmony.", command));
		
		String output = new CommandExecutor().execCommand(command, DEFAULT_TIMEOUT);
		Log.getRootLogger().info("Result of harmony command: " + output);
		return output != null && output.toLowerCase().contains("success");
	}

	public String getButtons() {
		File node = new File(OSUtils.getBinaryPath("node"));
		if (!node.exists()) {
			Log.getRootLogger().warn("Node was not found in user's path. Please install!");
			return null;
		}

		String command = node.getAbsolutePath() + " " + DependencyConstants.HARMONY_BIN.getAbsolutePath() + " -l " + homeHubDeviceIP
				+ " -d " + "\"" + deviceName + "\"" + " -r commands";
		Log.getRootLogger().info(String.format("Sending %s command to harmony.", command));
		String output = new CommandExecutor().execCommand(command, DEFAULT_TIMEOUT);
		return output;
	}

}
