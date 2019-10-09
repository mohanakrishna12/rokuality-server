package com.rokuality.server.constants;

import java.io.File;

import com.rokuality.server.utils.OSUtils;

public class FFMPEGConstants {

	public static final File FFMPEG_WIN_ZIP = new File(DependencyConstants.DEPENDENCY_DIR.getAbsolutePath() 
			+ File.separator + "ffmpeg_win_v4.1.zip");
	public static final File FFMPEG = OSUtils.isWindows() ? new File(DependencyConstants.DEPENDENCY_DIR.getAbsolutePath() 
			+ File.separator + "ffmpeg_win_v4.1" + File.separator + "bin" + File.separator + "ffmpeg.exe") 
		: new File(DependencyConstants.DEPENDENCY_DIR.getAbsolutePath() + File.separator + "ffmpeg_v4.1");

}
