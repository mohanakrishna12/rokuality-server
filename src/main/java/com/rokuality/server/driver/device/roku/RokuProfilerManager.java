package com.rokuality.server.driver.device.roku;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.rokuality.server.constants.DependencyConstants;
import com.rokuality.server.servlets.session;
import com.rokuality.server.utils.FileUtils;
import com.rokuality.server.utils.SleepUtils;
import com.rokuality.server.utils.ZipUtils;

import org.apache.http.HttpStatus;
import org.eclipse.jetty.util.log.Log;

public class RokuProfilerManager {

	private static final int DEFAULT_API_TIMEOUT_MS = 10000;
	private static final int PORT = 8090;

	private static Map<String, HttpURLConnection> connectionMap = new ConcurrentHashMap<String, HttpURLConnection>();
	private static Map<String, InputStream> inputStreamMap = new ConcurrentHashMap<String, InputStream>();

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

		FileUtils.deleteFile(appPackage);
		return updatedPackage;
	}

	public static boolean startProfileCapture(String deviceIP) {
		File profFile = getProfileFile(deviceIP);
		FileUtils.cleanFile(profFile);
		Log.getRootLogger().info(String.format("Initiating Roku profiling capture on device %s at: %s", deviceIP,
				profFile.getAbsolutePath()));

		int responseCode = 200;

		HttpURLConnection con = null;
		InputStream inputStream = null;
		try {
			String urlLoc = "http://" + deviceIP + ":" + PORT + "/bsprof/channel.bsprof";
			URL url = new URL(urlLoc);
			con = (HttpURLConnection) url.openConnection();
			con.setConnectTimeout(DEFAULT_API_TIMEOUT_MS);
			con.setRequestMethod("GET");

			responseCode = con.getResponseCode();

			if (responseCode == HttpStatus.SC_OK) {
				inputStream = con.getInputStream();
				inputStreamMap.put(deviceIP, inputStream);
				connectionMap.put(deviceIP, con);
				return true;
			}
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}

		if (inputStream != null) {
			try {
				inputStream.close();
			} catch (Exception e) {
				Log.getRootLogger().warn(e);
			}
		}

		if (con != null) {
			try {
				con.disconnect();
			} catch (Exception e) {
				Log.getRootLogger().warn(e);
			}
		}

		return false;
	}

	public static void stopProfileCapture(String deviceIP) {
		Log.getRootLogger().info(String.format("Stopping profile capture for device %s", deviceIP));
		if (deviceIP == null) {
			return;
		}

		HttpURLConnection connection = connectionMap.get(deviceIP);
		InputStream inputStream = inputStreamMap.get(deviceIP);

		try {
			if (inputStream != null) {
				org.apache.commons.io.FileUtils.copyInputStreamToFile(inputStream, getProfileFile(deviceIP));
				inputStream.close();
			}
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}

		try {
			if (connection != null) {
				connection.disconnect();
			}
		} catch (Exception e) {
			Log.getRootLogger().warn(e);
		}

		connectionMap.remove(deviceIP);
		inputStreamMap.remove(deviceIP);
	}

	/**
	 * 1) Returns the Roku to the home screen so the profiling data is flushed on
	 * the device. 2) Closes the InputStream and Connection. 3) Re-opens the app. 4)
	 * Re-initiates the profile data collection.
	 *
	 */
	public static boolean processProfileData(String deviceIP) {
		Log.getRootLogger().info(String.format("Processing Roku profiling data for %s", deviceIP));
		session.returnToRokuHomeScreen(deviceIP);
		SleepUtils.sleep(1000); // TODO dynamic
		stopProfileCapture(deviceIP);
		RokuPackageHandler.launchInstalledApp(deviceIP, "dev");
		RokuPackageHandler.waitForAppLaunched(deviceIP, "dev");
		return startProfileCapture(deviceIP);
	}

	public static File getProfileFile(String deviceIP) {
		return new File(DependencyConstants.TEMP_DIR.getAbsolutePath() + File.separator + deviceIP + "_"
				+ "roku_profile.bsprof");
	}

}
