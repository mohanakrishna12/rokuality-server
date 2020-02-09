package com.rokuality.server.driver.device.roku;

import org.eclipse.jetty.util.log.Log;
import org.json.simple.JSONObject;

import com.rokuality.server.constants.DependencyConstants;
import com.rokuality.server.constants.ServerConstants;
import com.rokuality.server.constants.SessionConstants;
import com.rokuality.server.driver.host.APIManager;
import com.rokuality.server.enums.APIType;
import com.rokuality.server.enums.SessionCapabilities;
import com.rokuality.server.utils.FileToStringUtils;
import com.rokuality.server.utils.FileUtils;
import com.rokuality.server.utils.HttpUtils;
import com.rokuality.server.utils.SleepUtils;

import java.io.File;
import java.util.UUID;

@SuppressWarnings("unchecked")
public class RokuPackageHandler {

	private static final String ZIP_EXTENSION = ".zip";

	public static JSONObject installPackage(JSONObject requestObj) {
		JSONObject results = new JSONObject();
		boolean success = false;

		File appPackage = null;
		String packageName = null;
		boolean validPackage = false;

		String appPackageCap = (String) requestObj.get(SessionCapabilities.APP_PACKAGE.value());
		String appCap = (String) requestObj.get(SessionCapabilities.APP.value());

		boolean isPreInstalledApp = appCap != null;
		boolean appIsUrl = false;

		String profileCap = (String) requestObj.get(SessionCapabilities.ENABLE_PERFORMANCE_MONITORING);
		boolean prepareAppForProfiling = profileCap != null && profileCap.toLowerCase().equals("true");

		if (appCap == null && appPackageCap != null) {
			appIsUrl = HttpUtils.isValidUrl(appPackageCap);
		}

		// app is from a url
		if (appIsUrl) {
			Log.getRootLogger().info("App is from a url. Downloading and saving to a file.");
			packageName = UUID.randomUUID().toString() + ZIP_EXTENSION;
			appPackage = new File(HttpUtils.downloadFile(appPackageCap, packageName));
		}

		// app is base 64 string
		if (!appIsUrl && !isPreInstalledApp) {
			Log.getRootLogger().info("App is base64 string. Converting to a file.");
			appPackage = new File(DependencyConstants.TEMP_DIR.getAbsolutePath() + File.separator
					+ UUID.randomUUID().toString() + ZIP_EXTENSION);
			try {
				appPackage = new FileToStringUtils().convertToFile(appPackageCap, appPackage);
			} catch (Exception e) {
				Log.getRootLogger().warn(e);
				results.put(ServerConstants.SERVLET_RESULTS,
						String.format("Failed to decode app package file at %s. The %s capability must be either a "
								+ "valid url to a sideloadable .zip package OR a valid file path to a sideloadable .zip package!",
								String.valueOf(appPackageCap), SessionCapabilities.APP_PACKAGE.value()));
				FileUtils.deleteFile(appPackage);
				return results;
			}
		}

		// app is preinstalled and must be launched
		if (!appIsUrl && isPreInstalledApp) {
			prepareAppForProfiling = false;
			Log.getRootLogger().info("App is preinstalled/sideloaded. Launching.");
			String deviceIP = String.valueOf(requestObj.get(SessionCapabilities.DEVICE_IP_ADDRESS.value()));
			boolean appPreInstalledSuccess = false;
			boolean appPreInstalledLaunchSuccess= false;
			try {
				appPreInstalledSuccess = launchInstalledApp(deviceIP, appCap);
				appPreInstalledLaunchSuccess = waitForAppLaunched(deviceIP, appCap);
			} catch (Exception e) {
				Log.getRootLogger().warn(e);
			}

			if (!appPreInstalledSuccess || !appPreInstalledLaunchSuccess) {
				results.put(ServerConstants.SERVLET_RESULTS, String.format(
						"Failed to launch the %s application! If you provide the %s capability then the identified app "
								+ "id must be an app that is already sideloaded (and the cap value should almost certainly be 'dev'!"
								+ " Otherwise, please use the %s capability and identify "
								+ "the path or url to sideloadable .zip package which will be installed.",
						appCap, SessionCapabilities.APP_PACKAGE.value(), SessionCapabilities.APP_PACKAGE.value()));
				return results;
			}
			results.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
			return results;
		}

		Log.getRootLogger().info("Installing url/file app and launching.");
		validPackage = (appPackage.exists() && appPackage.isFile() && appPackage.length() > 0);

		if (!validPackage) {
			results.put(ServerConstants.SERVLET_RESULTS,
					String.format("The provided app package is not valid! The %s capability must be either a "
							+ "valid url to a sideloadable .zip package OR a valid file path to a sideloadable .zip package!",
							SessionCapabilities.APP_PACKAGE.value()));
			FileUtils.deleteFile(appPackage);
			return results;
		}

		if (validPackage) {
			String username = (String) requestObj.get(SessionCapabilities.DEVICE_USERNAME.value());
			String password = (String) requestObj.get(SessionCapabilities.DEVICE_PASSWORD.value());
			String deviceip = (String) requestObj.get(SessionCapabilities.DEVICE_IP_ADDRESS.value());

			if (prepareAppForProfiling) {
				Log.getRootLogger().info(String.format("Preparing app %s for profiling for device %s", appPackage, deviceip));
				appPackage = RokuProfilerManager.prepareAppPackage(appPackage);
				if (appPackage == null) {
					results.put(ServerConstants.SERVLET_RESULTS,
					String.format("Failed to prepare app package %s for performance profiling. Check the logs for more details.",
							SessionCapabilities.APP_PACKAGE.value()));
					FileUtils.deleteFile(appPackage);
					return results;
				}
			}

			RokuDevConsoleManager rokuDevConsoleManager = new RokuDevConsoleManager(deviceip, username, password);
			rokuDevConsoleManager.uninstallRokuApp();
			success = rokuDevConsoleManager.installRokuApp(appPackage.getAbsolutePath()) 
				&& waitForAppLaunched(deviceip, SessionConstants.DEV_PACKAGE);

			results.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
			if (!success) {
				results.put(ServerConstants.SERVLET_RESULTS,
						"Failed to install/launch Roku app! The app package must be a url or local path to a valid sideloadable Roku .zip package!");
			}
		}

		if (appPackage != null && appPackage.exists()) {
			FileUtils.deleteFile(appPackage);
		}

		return results;
	}

	public static boolean isAppLaunched(String deviceIP, String appID) {
		String output = getActiveApp(deviceIP);
		return (output != null && output.contains("app id=\"" + appID + "\""));
	}

	public static String getActiveApp(String deviceIP) {
		APIManager rokuDevAPIManager = new APIManager(APIType.ROKU_DEV_API, deviceIP, "/query/active-app", "GET");
		rokuDevAPIManager.sendDevAPICommand();
		return rokuDevAPIManager.getResponseContent();
	}

	public static String getInstalledApps(String deviceIP) {
		APIManager rokuDevAPIManager = new APIManager(APIType.ROKU_DEV_API, deviceIP, "/query/apps", "GET");
		rokuDevAPIManager.sendDevAPICommand();
		return rokuDevAPIManager.getResponseContent();
	}

	public static boolean launchInstalledApp(String deviceIP, String appID) {
		APIManager rokuDevAPIManager = new APIManager(APIType.ROKU_DEV_API, deviceIP, "/launch/" + appID, "POST");
		return rokuDevAPIManager.sendDevAPICommand();
	}

	public static boolean waitForAppLaunched(String deviceIP, String appID) {
		long pollStart = System.currentTimeMillis();
		long pollMax = pollStart + (SessionConstants.DEFAULT_APP_LAUNCH_TIMEOUT_S * 1000);
		while (System.currentTimeMillis() < pollMax) {
			boolean launched = isAppLaunched(deviceIP, appID);
			if (launched) {
				return true;
			}
			SleepUtils.sleep(100);
		}
		return false;
	}

}
