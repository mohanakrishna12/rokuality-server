package com.rokuality.server.driver.device.xbox;

import org.eclipse.jetty.util.log.Log;

import java.io.*;

import com.rokuality.server.constants.DependencyConstants;
import com.rokuality.server.core.CommandExecutor;
import com.rokuality.server.enums.XBoxButton;
import com.rokuality.server.utils.OSUtils;

public class XBoxKeyPresser {

	private static final int DEFAULT_TIMEOUT = 30;

	private String homeHubDeviceIP = null;
	private String deviceName = null;
	
	public XBoxKeyPresser(String homeHubDeviceIP, String deviceName) {
		this.homeHubDeviceIP = homeHubDeviceIP;
		this.deviceName = deviceName;
	}

	public boolean pressButton(XBoxButton button) {
		File node = new File(OSUtils.getBinaryPath("node"));
		if (!node.exists()) {
			Log.getRootLogger().warn("Node was not found in user's path. Please install!");
			return false;
		}

		String command = node.getAbsolutePath() + DependencyConstants.HARMONY_BIN.getAbsolutePath() + " -l " + homeHubDeviceIP
				+ " -d " + "\"" + deviceName + "\"" + " -c " + button.value();
		Log.getRootLogger().info(String.format("Sending %s command to XBox.", command));
		
		String output = new CommandExecutor().execCommand(command, DEFAULT_TIMEOUT);
		Log.getRootLogger().info("Result of XBox command: " + output);
		return output != null && output.toLowerCase().contains("success");
	}

}
