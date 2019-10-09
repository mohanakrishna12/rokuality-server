package com.rokuality.server.utils;

import org.eclipse.jetty.util.log.Log;

public class SleepUtils {

	public static void sleep(Integer sleepInMS) {
		try {
			Thread.sleep(sleepInMS);
		} catch (InterruptedException e) {
			Log.getRootLogger().warn(e);
		}
	}

}
