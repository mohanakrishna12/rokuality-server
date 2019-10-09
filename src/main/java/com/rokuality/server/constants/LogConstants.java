package com.rokuality.server.constants;

import java.io.File;

import com.rokuality.server.utils.OSUtils;

public class LogConstants {

	public static final File LOG_DIR = new File(OSUtils.getUserBaseDir().getAbsolutePath() + File.separator +
			"logs");
	public static final String SERVER = "_server_";
	public static final String SERVER_EVENT_LOG_NAME = "yyyy_MM_dd" + SERVER + "event.log";
	public static final String SERVER_REQUEST_LOG_NAME = "yyyy_MM_dd" + SERVER + "request.log";
	public static final Integer MAX_RECENT_LOG_LINES = 3000;

}
