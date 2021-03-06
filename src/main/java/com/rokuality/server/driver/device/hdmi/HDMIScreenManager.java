package com.rokuality.server.driver.device.hdmi;

import java.io.File;

import com.rokuality.server.constants.FFMPEGConstants;
import com.rokuality.server.core.CommandExecutor;
import com.rokuality.server.utils.FileUtils;
import com.rokuality.server.utils.OSUtils;
import com.rokuality.server.utils.SleepUtils;

import org.eclipse.jetty.util.log.Log;

public class HDMIScreenManager {

	private static final Integer DEFAULT_FRAMERATE = 30;
	private static final int MAX_VIDEO_READY_WAIT_S = 15;
	private static final int VIDEO_READY_POLL_MS = 500;
	private static final String AV_FOUNDATION_AUDIO_DEVICES = "AVFoundation audio devices:";

	public static boolean startVideoCapture(String sessionID, File videoToCaptureTo, String videoCaptureInput,
			String audioCaptureInput, Long framerate) {

		long frameRateCapture = framerate == null ? DEFAULT_FRAMERATE : framerate;

		Log.getRootLogger().info(String.format(
				"Starting HDMI screen recording for session %s with video file %s and audio/video inputs: %s, %s",
				sessionID, videoToCaptureTo, videoCaptureInput, audioCaptureInput));
		if (sessionID == null || videoToCaptureTo == null || videoCaptureInput == null || audioCaptureInput == null) {
			Log.getRootLogger()
					.warn(String.format(
							"session %s, video to capture %s, video capture "
									+ "index %s, and audio capture index %s cannot be null!",
							String.valueOf(sessionID), String.valueOf(videoToCaptureTo),
							String.valueOf(videoCaptureInput), String.valueOf(audioCaptureInput)));
			return false;
		}

		String ffmpegPath = FFMPEGConstants.FFMPEG.getAbsolutePath();

		FileUtils.deleteFile(videoToCaptureTo);

		String content = "";
		String command = null;
		if (OSUtils.isWindows()) {
			content = ffmpegPath + " -f dshow -framerate " + frameRateCapture + " -i video=\"" + videoCaptureInput + "\":audio=\""
					+ audioCaptureInput + "\" -max_muxing_queue_size 9999 " + videoToCaptureTo.getAbsolutePath();
			command = content;
		}

		if (!OSUtils.isWindows()) {
			Integer videoCaptureIndex = getUSBCaptureIndex(true, videoCaptureInput);
			Integer audioCaptureIndex = getUSBCaptureIndex(false, audioCaptureInput);

			if (videoCaptureIndex == null || audioCaptureIndex == null) {
				Log.getRootLogger().warn("Failed to capture video/audio indexes!");
				return false;
			}

			Log.getRootLogger().info(String.format("Detected AV Inputs for input names %s/%s: %s, %s",
					videoCaptureInput, audioCaptureInput, videoCaptureIndex, audioCaptureIndex));

			content = "nohup " + ffmpegPath + " -f avfoundation -framerate " + frameRateCapture + " -i \"" + videoCaptureIndex + ":"
					+ audioCaptureIndex + "\" " + videoToCaptureTo.getAbsolutePath();
			command = content;
		}

		Log.getRootLogger()
				.info(String.format("Starting video capture for session %s with command: %s", sessionID, command));
		CommandExecutor commandExecutor = new CommandExecutor();
		commandExecutor.setWaitToComplete(false);
		String output = commandExecutor.execCommand(command, null);

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

		Log.getRootLogger()
				.info(String.format("Failed video capture start for session %s with output: %s", sessionID, output));
		Log.getRootLogger().warn("Failed video count output: " + String.valueOf(getFrameCountOutput(videoToCaptureTo)));

		return false;
	}

	public static void stopVideoCapture(File videoUnderCapture) {
		if (videoUnderCapture == null) {
			return;
		}

		Log.getRootLogger().info("Stopping hdmi video under capture at: " + String.valueOf(videoUnderCapture));
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

		if (OSUtils.isWindows()) {
			// unfortunate delay while windows releases the file lock on the captured asset
			SleepUtils.sleep(250);
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
		Integer lastFrame = null;
		String output = getFrameCountOutput(video);
		if (output == null || !output.contains("frame=")) {
			return 0;
		}

		try {
			if (output.contains("frame= ")) {
				lastFrame = Integer.parseInt(output.split("frame=\\s+")[1].split(" ")[0]);
			} else {
				lastFrame = Integer.parseInt(output.split("frame=")[1].split(" ")[0]);
			}
		} catch (Exception e) {
			Log.getRootLogger().warn(String.format("Unable to parse frame count from video %s with output %s",
					String.valueOf(video), String.valueOf(output)), e);
			return 0;
		}

		return lastFrame;
	}

	public static Integer getUSBCaptureIndex(boolean isVideo, String inputName) {
		String avOutput = executeAVFoundationListDevicesCommand();
		int splitterIndex = isVideo ? 0 : 1;

		if (avOutput.contains(AV_FOUNDATION_AUDIO_DEVICES)) {
			avOutput = avOutput.split(AV_FOUNDATION_AUDIO_DEVICES)[splitterIndex];
		}

		if (!avOutput.contains(System.lineSeparator())) {
			Log.getRootLogger().warn("No audio/video inputs detected during AV Foundation check.");
			return null;
		}

		for (String line : avOutput.split(System.lineSeparator())) {
			if (line.toLowerCase().contains(inputName.toLowerCase())) {
				Log.getRootLogger().info(
						String.format("Found matching HDMI input %s with av foundation line %s", inputName, line));
				try {
					return Integer.parseInt(line.split("\\[")[2].split("]")[0]);
				} catch (Exception e) {
					Log.getRootLogger().warn(String.format(
							"Failed to parse AV Foundation index for input %s for AV line %s", inputName, line), e);
					return null;
				}
			}
		}

		Log.getRootLogger().warn(String.format("Failed to find AV Foundation index for input %s", inputName));
		return null;
	}

	private static String executeAVFoundationListDevicesCommand() {
		String ffmpegPath = FFMPEGConstants.FFMPEG.getAbsolutePath();
		CommandExecutor commandExecutor = new CommandExecutor();
		String[] command = { ffmpegPath, "-f", "avfoundation", "-list_devices", "true", "-i", "\"\"" };
		String output = commandExecutor.execCommand(String.join(" ", command), null);
		Log.getRootLogger().info(String.format("Result of HDMI AV Foundation List: %s", output));
		return output;
	}

	private static String getFrameCountOutput(File video) {
		String[] getFrameCountCommand = { FFMPEGConstants.FFMPEG.getAbsolutePath(), "-i", video.getAbsolutePath(),
				"-map", "0:v:0", "-c", "copy", "-f", "null", "-" };
		String output = new CommandExecutor().execCommand(String.join(" ", getFrameCountCommand), null);
		return output;
	}

}