package com.rokuality.server.driver.device.hdmi;

import java.io.File;

import com.rokuality.server.constants.FFMPEGConstants;
import com.rokuality.server.core.CommandExecutor;
import com.rokuality.server.utils.FileUtils;
import com.rokuality.server.utils.LogFileUtils;
import com.rokuality.server.utils.OSUtils;
import com.rokuality.server.utils.SleepUtils;

import org.eclipse.jetty.util.log.Log;

public class HDMIScreenManager {

	private static final int MAX_VIDEO_READY_WAIT_S = 15;
	private static final int VIDEO_READY_POLL_MS = 500;

	public static boolean startVideoCapture(String sessionID, File videoToCaptureTo, String videoCaptureIndex,
			String audioCaptureIndex) {

		if (sessionID == null || videoToCaptureTo == null || videoCaptureIndex == null || audioCaptureIndex == null) {
			Log.getRootLogger()
					.warn(String.format(
							"session %s, video to capture %s, video capture "
									+ "index %s, and audio capture index %s cannot be null!",
							String.valueOf(sessionID), String.valueOf(videoToCaptureTo),
							String.valueOf(videoCaptureIndex), String.valueOf(audioCaptureIndex)));
			return false;
		}

		String ffmpegPath = FFMPEGConstants.FFMPEG.getAbsolutePath();

		FileUtils.deleteFile(videoToCaptureTo);

		File logFile = LogFileUtils.getLogFile("videocapture.log");
		logFile = LogFileUtils.cleanLogFile(logFile);

		String content = "";
		String command = null;
		if (OSUtils.isWindows()) {
			content = ffmpegPath + " -f dshow -i video=\"" + videoCaptureIndex + "\":audio=\"" + audioCaptureIndex
					+ "\" " + videoToCaptureTo.getAbsolutePath();
			command = "cmd /c start /b \"\" > " + logFile.getAbsolutePath() + " " + content;
		}

		if (!OSUtils.isWindows()) {
			content = "nohup " + ffmpegPath + " -f avfoundation -framerate 30 -i \"" + videoCaptureIndex + ":"
					+ audioCaptureIndex + "\" " + videoToCaptureTo.getAbsolutePath() + " &>" + logFile.getAbsolutePath()
					+ " &";
			command = content;
		}

		CommandExecutor commandExecutor = new CommandExecutor();
		String output = commandExecutor.execCommand(command, null);
		Log.getRootLogger().info("DEBUG - OUTPUT OF SCREEN CAPTURE VIDEO START: " + output);

		long pollStart = System.currentTimeMillis();
		long pollMax = pollStart + (MAX_VIDEO_READY_WAIT_S * 1000);

		while (System.currentTimeMillis() <= pollMax) {
			boolean videoExists = videoToCaptureTo.exists();
			int frameCount = 0;
			if (videoExists) {
				frameCount = getFrameCountFromVideo(videoToCaptureTo);
			}

			if (frameCount > 1) {
				Log.getRootLogger().info("Video capture is ready");
				return true;
			}

			SleepUtils.sleep(VIDEO_READY_POLL_MS);
		}

		return false;
	}

	public static void stopVideoCapture(File videoUnderCapture) {
		Log.getRootLogger().info("Stoppingg hdmi video under capture at: " + String.valueOf(videoUnderCapture));
		CommandExecutor commandExecutor = new CommandExecutor();

		String command = null;
		if (OSUtils.isWindows()) {
			String wmicPath = OSUtils.getBinaryPath("WMIC");
			command = wmicPath + " path win32_process get Caption,Processid,Commandline";
		}

		if (OSUtils.isMac()) {
			command = "ps aux";
		}

		String[] processLns = commandExecutor.execCommand(command, null).split("\\r?\\n");
		for (String line : processLns) {
			if (line.contains(FFMPEGConstants.FFMPEG.getName()) && line.contains(videoUnderCapture.getName())) {
				String processID = null;
				String killCommand = null;

				if (OSUtils.isWindows()) {
					String taskkill = OSUtils.getBinaryPath("taskkill");
					String[] entries = line.trim().split("\\s+");
					processID = entries[entries.length - 1];
					killCommand = taskkill + " /F /PID " + processID;
				}

				if (!OSUtils.isWindows()) {
					processID = line.trim().split("\\s+")[1];
					killCommand = "kill -INT " + processID;
				}

				Log.getRootLogger()
						.info(String.format("Process id of video %s stop: %s", videoUnderCapture, processID));
				Log.getRootLogger().info(String.format("Command of video %s stop: %s", videoUnderCapture, killCommand));
				String output = commandExecutor.execCommand(killCommand, null);
				Log.getRootLogger().info(String.format("Output of video %s stop: %s", videoUnderCapture, output));
			}
		}
	}

	public static File getScreenShotFromVideo(File videoUnderCapture, File screenshotToSaveAs) {
		if (videoUnderCapture == null || !videoUnderCapture.exists()) {
			Log.getRootLogger()
					.warn(String.format("Video capture file %s is invalid or does not exist!", videoUnderCapture));
			return null;
		}

		if (screenshotToSaveAs == null) {
			Log.getRootLogger().warn("Video capture screenshot file cannot be null!");
			return null;
		}

		String ffmpegPath = FFMPEGConstants.FFMPEG.getAbsolutePath();

		CommandExecutor commandExecutor = new CommandExecutor();

		int lastFrame = getFrameCountFromVideo(videoUnderCapture) - 1;
		if (lastFrame < 1) {
			return null;
		}

		String command = null;
		if (OSUtils.isWindows()) {
			command = ffmpegPath + " -i " + videoUnderCapture.getAbsolutePath() + " -vf \"select=gte(n\\," + lastFrame
					+ ")\" -vframes 1 " + screenshotToSaveAs.getAbsolutePath();
		}

		if (!OSUtils.isWindows()) {
			command = ffmpegPath + " -i " + videoUnderCapture.getAbsolutePath() + " -vf \"select=gte(n\\," + lastFrame
					+ ")\" -vframes 1 " + screenshotToSaveAs.getAbsolutePath();
		}

		String output = commandExecutor.execCommand(command, null);

		if (!screenshotToSaveAs.exists()) {
			Log.getRootLogger().warn(String.format("Failed to capture screenshot from video %s with output %s",
					String.valueOf(videoUnderCapture), String.valueOf(output)));
			return null;
		}

		return screenshotToSaveAs;
	}

	public static int getFrameCountFromVideo(File video) {
		String ffmpegPath = FFMPEGConstants.FFMPEG.getAbsolutePath();
		Integer lastFrame = null;
		String[] getFrameCountCommand = { ffmpegPath, "-i", video.getAbsolutePath(), "-map", "0:v:0", "-c", "copy",
				"-f", "null", "-" };
		String output = new CommandExecutor().execCommand(String.join(" ", getFrameCountCommand), null);
		if (output == null || !output.contains("frame=")) {
			Log.getRootLogger().warn(String.format("Unable to detect frame count from video %s with output %s"),
					String.valueOf(video), String.valueOf(output));
			return 0;
		}

		try {
			lastFrame = Integer.parseInt(output.split("frame=\\s+")[1].split(" ")[0]);
		} catch (Exception e) {
			Log.getRootLogger().warn(String.format("Unable to parse frame count from video %s with output %s"),
					String.valueOf(video), String.valueOf(output), e);
			return 0;
		}

		return lastFrame;
	}

}