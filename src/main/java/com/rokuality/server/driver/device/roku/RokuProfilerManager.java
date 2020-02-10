package com.rokuality.server.driver.device.roku;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;

import com.rokuality.server.constants.DependencyConstants;
import com.rokuality.server.core.CommandExecutor;
import com.rokuality.server.servlets.session;
import com.rokuality.server.utils.FileToStringUtils;
import com.rokuality.server.utils.FileUtils;
import com.rokuality.server.utils.OSUtils;
import com.rokuality.server.utils.SleepUtils;
import com.rokuality.server.utils.ZipUtils;

import org.eclipse.jetty.util.log.Log;

public class RokuProfilerManager {

	private static final int DEFAULT_CONNECT_TIMEOUT_S = 10;
	private static final int PORT = 8090;

	public static File prepareAppPackage(File appPackage) {
		if (appPackage == null || !appPackage.exists()) {
			Log.getRootLogger().warn(String.format("Cannot prepare Roku profiling for package %s", appPackage));
			return null;
		}

		File tempDirectory = new File(
				DependencyConstants.TEMP_DIR.getAbsolutePath() + File.separator + UUID.randomUUID().toString());
		boolean unzipped = ZipUtils.unzipZipFile(appPackage.getAbsolutePath(), tempDirectory.getAbsolutePath());
		if (!unzipped) {
			Log.getRootLogger().warn(String.format("Failed to unzip app %s during Roku profile prep.", appPackage));
			return null;
		}

		File manifest = new File(tempDirectory.getAbsolutePath() + File.separator + "manifest");
		if (!manifest.exists()) {
			Log.getRootLogger()
					.warn(String.format("No manifest found in package %s during Roku profile prep.", appPackage));
			FileUtils.deleteDirectory(tempDirectory);
			return null;
		}

		String fileContent = FileUtils.readStringFromFile(manifest);

		if (fileContent.contains("bsprof_data_dest")) {
			Log.getRootLogger().info(String.format(
					"Manifest %s already contains profile destination in manifest. Updating accordingly.", manifest));
			fileContent = fileContent.replace("bsprof_data_dest=local", "bsprof_data_dest=network");
		} else {
			fileContent = fileContent + System.lineSeparator() + "bsprof_data_dest=network";
		}

		if (fileContent.contains("bsprof_enable")) {
			Log.getRootLogger().info(String.format(
					"Manifest %s already contains profile enable in manifest. Updating accordingly.", manifest));
			fileContent = fileContent.replace("bsprof_enable=0", "bsprof_enable=1");
		} else {
			fileContent = fileContent + System.lineSeparator() + "bsprof_enable=1";
		}

		if (fileContent.contains("bsprof_enable_mem")) {
			Log.getRootLogger().info(String.format(
					"Manifest %s already contains profile memory enable in manifest. Updating accordingly.", manifest));
			fileContent = fileContent.replace("bsprof_enable_mem=0", "bsprof_enable_mem=1");
		} else {
			fileContent = fileContent + System.lineSeparator() + "bsprof_enable_mem=1";
		}

		FileUtils.deleteFile(manifest);
		boolean manifestUpdated = FileUtils.writeStringToFile(manifest, fileContent, false);
		if (!manifestUpdated) {
			Log.getRootLogger().warn(String.format("Failed to write updated manifest content %s", fileContent));
			FileUtils.deleteDirectory(tempDirectory);
			return null;
		}

		File updatedPackage = new File(DependencyConstants.TEMP_DIR.getAbsolutePath() + File.separator
				+ UUID.randomUUID().toString() + ".zip");
		boolean packageUpdated = ZipUtils.createZipFile(updatedPackage, Arrays.asList(tempDirectory.listFiles()));
		if (!packageUpdated || !updatedPackage.exists()) {
			Log.getRootLogger().warn(String.format("Failed to prepare updated package zip at %s", updatedPackage));
			FileUtils.deleteDirectory(tempDirectory);
			return null;
		}

		FileUtils.deleteDirectory(tempDirectory);
		FileUtils.deleteFile(appPackage);
		return updatedPackage;
	}

	public static boolean startProfileCapture(String deviceIP) {
		File profFile = getProfileFile(deviceIP);
		FileUtils.deleteFile(profFile);
		Log.getRootLogger().info(String.format("Initiating Roku profiling capture on device %s at: %s", deviceIP,
				profFile.getAbsolutePath()));

		String urlLoc = "http://" + deviceIP + ":" + PORT + "/bsprof/channel.bsprof";
		String curlPath = OSUtils.getBinaryPath("curl");

		String[] command = { curlPath, urlLoc, "-o", profFile.getAbsolutePath(), "--connect-timeout",
				String.valueOf(DEFAULT_CONNECT_TIMEOUT_S) };
		CommandExecutor commandExecutor = new CommandExecutor();
		commandExecutor.setWaitToComplete(false);
		commandExecutor.execCommand(String.join(" ", command), null);

		long pollStart = System.currentTimeMillis();
		long pollMax = pollStart + (DEFAULT_CONNECT_TIMEOUT_S * 1000);

		while (System.currentTimeMillis() <= pollMax) {
			if (profFile.exists()) {
				Log.getRootLogger().info(String.format("Performance profiling on %s has been initiated.", deviceIP));
				return true;
			}

			SleepUtils.sleep(250);
		}

		return false;
	}

	public static String processProfileData(String deviceIP) {
		Log.getRootLogger().info(String.format("Processing Roku profiling data for %s", deviceIP));
		session.returnToRokuHomeScreen(deviceIP);
		// TODO - find a dynamic way to wait for profile data written to the network.
		// This always seems to work for now...
		SleepUtils.sleep(1000);
		String content = new FileToStringUtils().convertToString(getProfileFile(deviceIP));
		if (content == null) {
			return null;
		}

		RokuPackageHandler.launchInstalledApp(deviceIP, "dev");
		boolean appRelaunched = RokuPackageHandler.waitForAppLaunched(deviceIP, "dev");
		boolean profileCaptureRestarted = startProfileCapture(deviceIP);
		if (!appRelaunched || !profileCaptureRestarted) {
			Log.getRootLogger().warn(String.format("Failed to restart profiling capture data on device %s", deviceIP));
			return null;
		}

		return content;
	}

	public static File getProfileFile(String deviceIP) {
		return new File(DependencyConstants.TEMP_DIR.getAbsolutePath() + File.separator + deviceIP + "_"
				+ "roku_profile.bsprof");
	}

}
