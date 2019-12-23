package com.rokuality.server.driver.device.roku;

import java.net.InetAddress;

public class RokuWebDriverFactory {

	public static boolean isRokuWebDriverRunning() {
		try {
            InetAddress inet = InetAddress.getByName("http://localhost:9000");
            return inet.isReachable(1000);
        } catch (Exception e) {
            return false;
		}
	}

	public static void startRokuWebDriver() {
		
	}

	


}
