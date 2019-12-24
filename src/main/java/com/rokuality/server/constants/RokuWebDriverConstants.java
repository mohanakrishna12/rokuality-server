package com.rokuality.server.constants;

import java.io.File;

public class RokuWebDriverConstants {

	public static final File ROKU_WEBDRIVER_BASE_DIR = new File(
			DependencyConstants.DEPENDENCY_DIR.getAbsolutePath() + File.separator + "automated-channel-testing-122219");
	public static final File ROKU_WEBDRIVER_ZIP = new File(ROKU_WEBDRIVER_BASE_DIR.getAbsolutePath() + ".zip");
	public static final File MAIN_GO = new File(
			ROKU_WEBDRIVER_BASE_DIR.getAbsolutePath() + File.separator + "src" + File.separator + "main.go");
	public static final File MAIN = new File(
			ROKU_WEBDRIVER_BASE_DIR.getAbsolutePath() + File.separator + "src" + File.separator + "main");

}
