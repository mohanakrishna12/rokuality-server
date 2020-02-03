package com.rokuality.server.driver.device.roku;

import org.eclipse.jetty.util.log.Log;

import com.rokuality.server.driver.host.APIManager;
import com.rokuality.server.enums.APIType;
import com.rokuality.server.enums.RokuButton;
import com.rokuality.server.utils.SleepUtils;

public class RokuKeyPresser {

	public static boolean rokuKeyPresser(String ipAddress, String keyName) {
		APIManager rokuDevAPIManager = new APIManager(APIType.ROKU_DEV_API, ipAddress, "/keypress/" + keyName, "POST");
		boolean success = rokuDevAPIManager.sendDevAPICommand();

		return success;
	}

	// NOTE - dependent on firmware version
	public static void rebootRoku(String ipAddress) throws Exception {
		Log.getRootLogger().info(String.format("Rebooting device %s", ipAddress));
		
		String[] rebootButtons = { 
			RokuButton.getDeviceButton(RokuButton.HOME), 
			RokuButton.getDeviceButton(RokuButton.HOME),
			RokuButton.getDeviceButton(RokuButton.HOME), 
			RokuButton.getDeviceButton(RokuButton.UP_ARROW),
			RokuButton.getDeviceButton(RokuButton.RIGHT_ARROW),
			RokuButton.getDeviceButton(RokuButton.UP_ARROW),
			RokuButton.getDeviceButton(RokuButton.RIGHT_ARROW),
			RokuButton.getDeviceButton(RokuButton.UP_ARROW),
			RokuButton.getDeviceButton(RokuButton.UP_ARROW),
			RokuButton.getDeviceButton(RokuButton.UP_ARROW),
			RokuButton.getDeviceButton(RokuButton.UP_ARROW),
			RokuButton.getDeviceButton(RokuButton.RIGHT_ARROW),
			RokuButton.getDeviceButton(RokuButton.SELECT) 
		};
		
		for (String button : rebootButtons) {
			rokuKeyPresser(ipAddress, button);
			SleepUtils.sleep(350);
		}
		
	}
}
