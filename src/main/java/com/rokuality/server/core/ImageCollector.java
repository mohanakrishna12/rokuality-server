package com.rokuality.server.core;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import com.rokuality.server.constants.DependencyConstants;
import com.rokuality.server.constants.FFMPEGConstants;
import com.rokuality.server.driver.device.RokuPackageHandler;
import com.rokuality.server.utils.FileUtils;
import com.rokuality.server.utils.ImageUtils;
import com.rokuality.server.utils.LogFileUtils;
import com.rokuality.server.utils.OSUtils;
import com.rokuality.server.utils.SleepUtils;

import org.eclipse.jetty.util.log.Log;

public class ImageCollector {

	private static final Integer MIN_FILE_SIZE_B = 100;

	private Boolean videoComplete = true;
	private Thread captureImageThread = null;
	private File currentCaptureFile = null;
	private File imageCaptureDir = null;
	private File recordedVideo = null;
	private File stitchVideoSh = null;
	private File videoReady = null;
	private String username = null;
	private String password = null;
	private String deviceIP = null;

	public ImageCollector(String sessionID, String deviceIP, File imageCaptureDir, String username, String password) {
		this.deviceIP = deviceIP;
		this.imageCaptureDir = imageCaptureDir;
		this.username = username;
		this.password = password;
		videoComplete = false;

		recordedVideo = new File(
				imageCaptureDir.getAbsolutePath() + File.separator + "RokuVideoRecording_" + sessionID + ".mp4");
		stitchVideoSh = new File(
				imageCaptureDir.getAbsolutePath() + File.separator + "RokuVideoSticher_" + sessionID + ".sh");
		videoReady = new File(imageCaptureDir.getAbsolutePath() + File.separator + "RokuVideoReady_" + sessionID);
	}

	public boolean startRecording() {
		Boolean imageDirSetup = false;
		try {
			if (imageCaptureDir != null && !imageCaptureDir.exists()) {
				imageDirSetup = FileUtils.createDirectory(imageCaptureDir);
			} 
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}

		if (!imageDirSetup) {
			videoComplete = true;
			Log.getRootLogger()
					.warn("Failed to properly setup the roku screen recording image collection directory. "
							+ "Screen recording will NOT be initiated!");
			return false;
		}

		captureImageThread = new Thread() {
			public void run() {
				try {
					startImageCollection();
				} catch (Exception e) {
					Log.getRootLogger().warn("Failed to initiate roku screen recording!", e);
				}
			}
		};

		if (imageDirSetup) {
			captureImageThread.start();
			return true;
		}
		return false;
	}

	private void startImageCollection() {
		Integer counter = 0;
		while (!videoComplete) {
			counter++;

			try {
				File image = ImageUtils.getScreenImage(username, password, deviceIP);
				if (image != null && image.exists()) {
					String paddedCounter = String.format("%05d", counter);
					File baseImage = image;
					File copiedImage = new File(
							imageCaptureDir.getAbsolutePath() + File.separator + paddedCounter + "." + "jpg");
					if (!copiedImage.exists()) {
						baseImage.renameTo(copiedImage);
					}
					
					if (copiedImage.exists()) {
						currentCaptureFile = copiedImage;
					} else {
						counter--;
					}
	
					if (copiedImage.exists() && copiedImage.length() < MIN_FILE_SIZE_B) {
						counter--;
						FileUtils.deleteFile(copiedImage);
					}
	
					if (baseImage.exists()) {
						FileUtils.deleteFile(baseImage);
					}
				}
			} catch (Exception e) {
				Log.getRootLogger().warn("Failed to capture screen shot/convert to capture dir during recording!", e);
				counter--;
			}
		}
	}

	public void stopRecording() {
		try {
			if (!videoComplete) {
				videoComplete = true;
				captureImageThread.join();
			}
		} catch (Exception e) {
			Log.getRootLogger().warn("Failed to terminate desktop screen recording!", e);
		}
	}

	public File getRecording() {
		return recordedVideo;
	}

	public File getCurrentImage(boolean allowCache) {
		// check if the captured file exists
		if (currentCaptureFile == null || !currentCaptureFile.exists() || !currentCaptureFile.isFile()) {
			return null;
		}

		// check if the device is hittable and the app is launched, allowing capturing
		if (!RokuPackageHandler.isAppLaunched(deviceIP, "dev")) {
			return null;
		}

		String jpgName = currentCaptureFile.getName();
		if (!allowCache) {
			Log.getRootLogger().info("Waiting for non-cached image from collector.");
			long pollStart = System.currentTimeMillis();
			long pollMax = pollStart + 10 * 1000;

			while (System.currentTimeMillis() < pollMax) {
				String uncachedJPGName = currentCaptureFile.getName();
				if (!uncachedJPGName.equals(jpgName)) {
					Log.getRootLogger().info("Image in collector is no longer cached. Exiting cache poller.");
					jpgName = uncachedJPGName;
					break;
				}

				SleepUtils.sleep(100);
			}
		}

		String pngLocation = imageCaptureDir.getAbsolutePath() + File.separator + jpgName.replace(".jpg", ".png");
		File pngFile = new File(pngLocation);
		try {
			if (currentCaptureFile.exists() && currentCaptureFile.isFile()) {
				BufferedImage bufferedImage = ImageIO.read(currentCaptureFile);
				ImageIO.write(bufferedImage, "png", pngFile);
				if (pngFile.exists()) {
					return pngFile;
				}
			}
		} catch (Exception e) {
			Log.getRootLogger().warn("Failed to convert Roku jpg to png!", e);
		}

		return null;
	}

	public boolean waitForImageCollectionStart() {
		long pollStart = System.currentTimeMillis();
		long pollMax = pollStart + 10 * 1000;

		while (System.currentTimeMillis() < pollMax) {
			File currentImage = getCurrentImage(true);
			if (currentImage != null && currentImage.exists() && currentImage.isFile()) {
				Log.getRootLogger().info("Roku image collection successfully initiated.", new Object[] {});
				return true;
			}

			Log.getRootLogger().info("Roku image collection not yet initiated. Waiting...", new Object[] {});
			SleepUtils.sleep(250);
		}

		return false;
	}

	public void prepareVideo() {
		try {
			FileUtils.deleteFile(videoReady);
			String ffmpegPath = FFMPEGConstants.FFMPEG.getAbsolutePath();

			CommandExecutor commandExecutor = new CommandExecutor();
			String[] command = null;
			String input = "";
			if (!OSUtils.isWindows()) {
				input = "# !/bin/bash" + System.lineSeparator() + "cd " + imageCaptureDir.getAbsolutePath()
						+ System.lineSeparator() + ffmpegPath
						+ " -framerate 3 -i '%05d.jpg' -c:v libx264 -pix_fmt yuv420p " + recordedVideo.getAbsolutePath()
						+ System.lineSeparator() + "touch " + videoReady.getAbsolutePath();
				command = new String[] { DependencyConstants.SHELL_EXECUTOR, stitchVideoSh.getAbsolutePath() };
			}

			if (OSUtils.isWindows()) {
				stitchVideoSh = new File(stitchVideoSh.getAbsolutePath().replace(".sh", ".bat"));
				input = "cd " + imageCaptureDir.getAbsolutePath() + System.lineSeparator() + ffmpegPath
						+ " -framerate 3 -i \"%%05d.jpg\" -c:v libx264 -pix_fmt yuv420p "
						+ recordedVideo.getAbsolutePath() + System.lineSeparator() + "copy NUL "
						+ videoReady.getAbsolutePath();

				File logStartFile = LogFileUtils.getLogFile("RokuVideoStich.log");
				logStartFile = LogFileUtils.cleanLogFile(logStartFile);
				FileUtils.createFile(logStartFile);
				command = new String[] { "cmd", "/c", "start", "/b", "\"\"", ">" + logStartFile.getAbsolutePath(),
						stitchVideoSh.getAbsolutePath() };
			}

			FileUtils.writeStringToFile(stitchVideoSh, input, true);
			commandExecutor.execCommand(String.join(" ", command), null);

			waitForVideoReady();
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}
	}

	// TODO - custom video not ready exception
	private void waitForVideoReady() throws Exception {
		long pollStart = System.currentTimeMillis();
		long pollMax = pollStart + 60 * 1000; // TODO - user provided max video ready

		while (System.currentTimeMillis() < pollMax) {
			if (getRecording().exists() && videoReady.exists()) {
				Log.getRootLogger().info("Roku screen recording is ready and available.", new Object[] {});
				break;
			}

			Log.getRootLogger().info("Roku screen recording is not ready. Waiting...", new Object[] {});
			SleepUtils.sleep(250);
		}
	}

}