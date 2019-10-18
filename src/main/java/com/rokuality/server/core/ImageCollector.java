package com.rokuality.server.core;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import com.rokuality.server.constants.DependencyConstants;
import com.rokuality.server.constants.FFMPEGConstants;
import com.rokuality.server.driver.device.roku.RokuPackageHandler;
import com.rokuality.server.enums.PlatformType;
import com.rokuality.server.utils.FileUtils;
import com.rokuality.server.utils.ImageUtils;
import com.rokuality.server.utils.LogFileUtils;
import com.rokuality.server.utils.OSUtils;
import com.rokuality.server.utils.SleepUtils;

import org.eclipse.jetty.util.log.Log;

public class ImageCollector {

	private static final Integer MIN_FILE_SIZE_B = 100;

	private PlatformType platform = null;
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
	private File videoCapture = null;

	public ImageCollector(PlatformType platform, String sessionID, String deviceIP, File imageCaptureDir,
			String username, String password, File videoCapture) {
		this.platform = platform;
		this.deviceIP = deviceIP;
		this.imageCaptureDir = imageCaptureDir;
		this.username = username;
		this.password = password;
		this.videoCapture = videoCapture;
		videoComplete = false;

		recordedVideo = new File(imageCaptureDir.getAbsolutePath() + File.separator + platform.value()
				+ "_videorecording_" + sessionID + ".mp4");
		stitchVideoSh = new File(imageCaptureDir.getAbsolutePath() + File.separator + platform.value()
				+ "_videostitcher_" + sessionID + ".sh");
		videoReady = new File(
				imageCaptureDir.getAbsolutePath() + File.separator + platform.value() + "_videoready_" + sessionID);
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
			Log.getRootLogger().warn("Failed to properly setup the screen recording image collection directory. "
					+ "Screen recording will NOT be initiated!");
			return false;
		}

		captureImageThread = new Thread() {
			public void run() {
				try {
					startImageCollection();
				} catch (Exception e) {
					Log.getRootLogger().warn("Failed to initiate screen recording!", e);
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
				String paddedCounter = String.format("%05d", counter);
				File captureFile = new File(imageCaptureDir.getAbsolutePath() + File.separator + paddedCounter + getImageCaptureFormat());
				File image = ImageUtils.getScreenImage(captureFile, platform, username, password, deviceIP, videoCapture);
				if (image != null && image.exists() && image.length() > MIN_FILE_SIZE_B) {
					currentCaptureFile = image;
				} else {
					counter--;
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
			Log.getRootLogger().warn("Failed to terminate recording!", e);
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
		if (PlatformType.ROKU.equals(platform) && !RokuPackageHandler.isAppLaunched(deviceIP, "dev")) {
			return null;
		}

		String imageName = currentCaptureFile.getName();
		if (!allowCache) {
			Log.getRootLogger().info("Waiting for non-cached image from collector.");
			long pollStart = System.currentTimeMillis();
			long pollMax = pollStart + 10 * 1000;

			while (System.currentTimeMillis() < pollMax) {
				String uncachedImageName = currentCaptureFile.getName();
				if (!uncachedImageName.equals(imageName)) {
					Log.getRootLogger().info("Image in collector is no longer cached. Exiting cache poller.");
					imageName = uncachedImageName;
					break;
				}

				SleepUtils.sleep(100);
			}
		}

		String pngLocation = imageCaptureDir.getAbsolutePath() + File.separator + imageName.replace(".jpg", ".png");
		File pngFile = new File(pngLocation);
		try {
			if (PlatformType.ROKU.equals(platform) && currentCaptureFile.exists() && currentCaptureFile.isFile()) {
				BufferedImage bufferedImage = ImageIO.read(currentCaptureFile);
				ImageIO.write(bufferedImage, "png", pngFile);
				if (pngFile.exists()) {
					return pngFile;
				}
			}

			if (!PlatformType.ROKU.equals(platform) && currentCaptureFile.exists() && currentCaptureFile.isFile()) {
				File copiedImage = new File(imageCaptureDir.getAbsolutePath() + File.separator + "copied_" + pngFile.getName());
				FileUtils.copyFile(pngFile, copiedImage);
				if (copiedImage.exists()) {
					return copiedImage;
				}
			}
		} catch (Exception e) {
			Log.getRootLogger().warn("Failed to prepare current image for evaluation!", e);
		}

		return null;
	}

	public boolean waitForImageCollectionStart() {
		long pollStart = System.currentTimeMillis();
		long pollMax = pollStart + 10 * 1000;

		while (System.currentTimeMillis() < pollMax) {
			File currentImage = getCurrentImage(true);
			if (currentImage != null && currentImage.exists() && currentImage.isFile()) {
				Log.getRootLogger().info("Image collection successfully initiated.", new Object[] {});
				return true;
			}

			Log.getRootLogger().info("Image collection not yet initiated. Waiting...", new Object[] {});
			SleepUtils.sleep(250);
		}

		return false;
	}

	public void prepareVideo() {
		try {
			prepVideoFileList();
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}

		try {
			FileUtils.deleteFile(videoReady);
			String ffmpegPath = FFMPEGConstants.FFMPEG.getAbsolutePath();

			CommandExecutor commandExecutor = new CommandExecutor();
			String[] command = null;
			String input = "";
			if (!OSUtils.isWindows()) {
				input = "# !/bin/bash" + System.lineSeparator() + "cd " + imageCaptureDir.getAbsolutePath()
						+ System.lineSeparator() + ffmpegPath + " -framerate 3 -i '%05d" + getImageCaptureFormat()
						+ "' -c:v libx264 -pix_fmt yuv420p " + recordedVideo.getAbsolutePath() + System.lineSeparator()
						+ "touch " + videoReady.getAbsolutePath();
				command = new String[] { DependencyConstants.SHELL_EXECUTOR, stitchVideoSh.getAbsolutePath() };
			}

			if (OSUtils.isWindows()) {
				stitchVideoSh = new File(stitchVideoSh.getAbsolutePath().replace(".sh", ".bat"));
				input = "cd " + imageCaptureDir.getAbsolutePath() + System.lineSeparator() + ffmpegPath
						+ " -framerate 3 -i \"%%05d" + getImageCaptureFormat() + "\" -c:v libx264 -pix_fmt yuv420p "
						+ recordedVideo.getAbsolutePath() + System.lineSeparator() + "copy NUL "
						+ videoReady.getAbsolutePath();

				File logStartFile = LogFileUtils.getLogFile(platform.value() + "_videostich.log");
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
				Log.getRootLogger().info("Screen recording is ready and available.", new Object[] {});
				break;
			}

			Log.getRootLogger().info("Screen recording is not ready. Waiting...", new Object[] {});
			SleepUtils.sleep(250);
		}
	}

	private String getImageCaptureFormat() {
		return PlatformType.ROKU.equals(platform) ? ".jpg" : ".png";
	}

	private void prepVideoFileList() throws Exception {
		File[] allFiles = imageCaptureDir.listFiles();
		Set<File> allNumericFiles = new HashSet<File>();
		for (File file : allFiles) {
			try {
				int index = file.getName().lastIndexOf(".");
				if (index != -1) {
					String fileName = file.getName().substring(0, index);
					Long.parseLong(fileName);
					allNumericFiles.add(file);
				}
			} catch (NumberFormatException | NullPointerException nfe) {
				//ignore as it's not an alpha numeric file of the capture
			}
		}

		for (int i = 1; i < allNumericFiles.size(); i++) {
			String paddedCounter = String.format("%05d", i);
			File expectedImage = new File(imageCaptureDir + File.separator + paddedCounter + getImageCaptureFormat());
			if (!expectedImage.exists()) {
				Log.getRootLogger().info(String.format(
						"Video compilation file %s does not exist. " + "Copying either a parent or child in it's place",
						expectedImage.getName()));

				File parent = new File(expectedImage.getParentFile() + File.separator + String.format("%05d", i - 1)
						+ getImageCaptureFormat());
				File child = new File(expectedImage.getParentFile() + File.separator + String.format("%05d", i + 1)
						+ getImageCaptureFormat());
				if (parent.exists()) {
					FileUtils.copyFile(parent, expectedImage);
				}

				if (!parent.exists()) {
					FileUtils.copyFile(child, expectedImage);
				}

				// TODO - come up with a better option for this
				if (!parent.exists() && !child.exists()) {
					Log.getRootLogger().warn("No captured parent or child images in "
							+ "the collector! Video compilation will likely stop at this point!");
				}
			}
		}
	}

}