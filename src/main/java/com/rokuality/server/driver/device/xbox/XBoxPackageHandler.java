package com.rokuality.server.driver.device.xbox;

import org.eclipse.jetty.util.log.Log;
import org.json.simple.JSONObject;

import com.rokuality.server.constants.DependencyConstants;
import com.rokuality.server.constants.ServerConstants;
import com.rokuality.server.enums.SessionCapabilities;
import com.rokuality.server.utils.FileToStringUtils;
import com.rokuality.server.utils.FileUtils;
import com.rokuality.server.utils.HttpUtils;

import java.io.File;
import java.util.UUID;

@SuppressWarnings("unchecked")
public class XBoxPackageHandler {

	private static final String APPX_EXTENSION = ".appxbundle";

	public static JSONObject installPackage(JSONObject requestObj) {
		String deviceip = (String) requestObj.get(SessionCapabilities.DEVICE_IP_ADDRESS.value());

		JSONObject results = new JSONObject();
		boolean success = false;

		File appPackage = null;
		String packageName = null;
		boolean validPackage = false;

		String appPackageCap = (String) requestObj.get(SessionCapabilities.APP_PACKAGE.value());
		String appCap = (String) requestObj.get(SessionCapabilities.APP.value());

		boolean isPreInstalledApp = appCap != null && appPackageCap == null;
		boolean appIsUrl = false;

		if (appPackageCap != null) {
			appIsUrl = HttpUtils.isValidUrl(appPackageCap);
		}

		// app is from a url
		if (appIsUrl) {
			Log.getRootLogger().info("App is from a url. Downloading and saving to a file.");
			packageName = UUID.randomUUID().toString() + APPX_EXTENSION;
			appPackage = new File(HttpUtils.downloadFile(appPackageCap, packageName));
		}

		// app is base 64 string
		if (!appIsUrl && !isPreInstalledApp) {
			Log.getRootLogger().info("App is base64 string. Converting to a file.");
			appPackage = new File(DependencyConstants.TEMP_DIR.getAbsolutePath() + File.separator
					+ UUID.randomUUID().toString() + APPX_EXTENSION);
			try {
				appPackage = new FileToStringUtils().convertToFile(appPackageCap, appPackage);
			} catch (Exception e) {
				Log.getRootLogger().warn(e);
				results.put(ServerConstants.SERVLET_RESULTS,
						String.format("Failed to decode app package file at %s. The %s capability must be either a "
								+ "valid url to a .appxbundle package OR a valid file path to a .appxbundle package!",
								String.valueOf(appPackageCap), SessionCapabilities.APP_PACKAGE.value()));
				return results;
			}
		}

		// app is preinstalled and must be launched
		if (isPreInstalledApp) {
			Log.getRootLogger().info("App is preinstalled. Launching.");
			boolean appPreInstalledSuccess = false;
			try {

				appPreInstalledSuccess = new XBoxDevConsoleManager(deviceip).launchApp(appCap);
			} catch (Exception e) {
				Log.getRootLogger().warn(e);
			}

			if (!appPreInstalledSuccess) {
				results.put(ServerConstants.SERVLET_RESULTS, String.format(
						"Failed to launch the %s application! If you provide the %s capability then the identified app "
								+ "id must be an app that is already installed!"
								+ " Otherwise, please use the %s capability and identify "
								+ "the path or url to a valid .appxbundle package which will be installed.",
						appCap, SessionCapabilities.APP_PACKAGE.value(), SessionCapabilities.APP_PACKAGE.value()));
				return results;
			}
			results.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
			return results;
		}

		Log.getRootLogger().info("Installing url/file app and launching.");
		if (appPackage.exists() && appPackage.isFile()) {
			appPackage.setExecutable(true);
		}

		validPackage = (appPackage.exists() && appPackage.isFile() && appPackage.length() > 0);

		if (!validPackage) {
			results.put(ServerConstants.SERVLET_RESULTS,
					String.format("The provided app package is not valid! The %s capability must be either a "
							+ "valid url to a appx/appxbundle package OR a valid file path to a appx/appxbundle package!",
							SessionCapabilities.APP_PACKAGE.value()));
			return results;
		}

		if (validPackage) {
			XBoxDevConsoleManager xboxDevConsoleManager = new XBoxDevConsoleManager(deviceip);
			xboxDevConsoleManager.uninstallApp(appCap);
			success = xboxDevConsoleManager.installApp(appPackage.getAbsolutePath(), appCap);
			results.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
			if (!success) {
				results.put(ServerConstants.SERVLET_RESULTS,
						"Failed to install/launch XBox app! The app package must be a url or local path to a valid sideloadable appx or appxbundle package!");
			}
		}

		if (appPackage != null && appPackage.exists()) {
			FileUtils.deleteFile(appPackage);
		}

		return results;
	}

}
