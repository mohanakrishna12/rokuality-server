package com.rokuality.server.driver.device.roku;

import org.eclipse.jetty.util.log.Log;

import com.rokuality.server.enums.RokuButton;
import com.rokuality.server.utils.SleepUtils;

public class RokuKeyPresser {

	public static boolean rokuKeyPresser(String ipAddress, String keyName) {
		RokuDevAPIManager rokuDevAPIManager = new RokuDevAPIManager(ipAddress, "/keypress/" + keyName, "POST");
		boolean success = rokuDevAPIManager.sendDevAPICommand();

		return success;
	}

	// TODO - replace with custom button not presses/command not sent exception
	public static void rebootRoku(String ipAddress) throws Exception {
		Log.getRootLogger().info("Rebooting Roku device.", new Object[] {});
		rokuKeyPresser(ipAddress, RokuButton.getDeviceButton(RokuButton.HOME));
		SleepUtils.sleep(5000); // TOOD - dynamic wait for home screen

		rokuKeyPresser(ipAddress, RokuButton.getDeviceButton(RokuButton.UP_ARROW));
		rokuKeyPresser(ipAddress, RokuButton.getDeviceButton(RokuButton.RIGHT_ARROW));
		rokuKeyPresser(ipAddress, RokuButton.getDeviceButton(RokuButton.UP_ARROW));
		rokuKeyPresser(ipAddress, RokuButton.getDeviceButton(RokuButton.RIGHT_ARROW));
		rokuKeyPresser(ipAddress, RokuButton.getDeviceButton(RokuButton.UP_ARROW));
		rokuKeyPresser(ipAddress, RokuButton.getDeviceButton(RokuButton.UP_ARROW));
		rokuKeyPresser(ipAddress, RokuButton.getDeviceButton(RokuButton.UP_ARROW));
		rokuKeyPresser(ipAddress, RokuButton.getDeviceButton(RokuButton.RIGHT_ARROW));
		rokuKeyPresser(ipAddress, RokuButton.getDeviceButton(RokuButton.SELECT));
	}
}
