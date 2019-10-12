package com.rokuality.server.constants;

import com.rokuality.server.utils.OSUtils;

import java.io.File;

public class DependencyConstants {

	public static final String ROKUALITY_NAME = "Rokuality";
	public static final Integer MAX_DEPENDENCY_INSTALL_TIMEOUT_S = 60;
	public static final String SHELL_EXECUTOR = OSUtils.isWindows() ? "powershell.exe" : "bash";

	public static final File DEPENDENCY_DIR = new File(
			OSUtils.getUserBaseDir().getAbsolutePath() + File.separator + "dependencies");
	public static final File TEMP_DIR = new File(OSUtils.getUserBaseDir().getAbsolutePath() + File.separator + "temp");
	public static final String DEPENDENCY_URL = "https://rokualitypublic.s3.amazonaws.com/";

	public static final File HARMONY_BIN = new File(DEPENDENCY_DIR.getAbsolutePath() + File.separator
			+ "harmonyhub_v0.0.1" + File.separator + "harmonyHubCli.js");

}
