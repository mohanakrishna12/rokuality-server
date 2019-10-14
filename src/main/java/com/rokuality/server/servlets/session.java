package com.rokuality.server.servlets;

import com.google.auth.oauth2.GoogleCredentials;
import com.rokuality.server.constants.DependencyConstants;
import com.rokuality.server.constants.ServerConstants;
import com.rokuality.server.constants.SessionConstants;
import com.rokuality.server.core.ImageCollector;
import com.rokuality.server.core.drivers.SessionManager;
import com.rokuality.server.driver.device.roku.RokuDevAPIManager;
import com.rokuality.server.driver.device.roku.RokuKeyPresser;
import com.rokuality.server.driver.device.roku.RokuPackageHandler;
import com.rokuality.server.driver.device.xbox.XBoxDevConsoleManager;
import com.rokuality.server.driver.device.xbox.XBoxPackageHandler;
import com.rokuality.server.driver.host.GlobalDependencyInstaller;
import com.rokuality.server.enums.OCRType;
import com.rokuality.server.enums.PlatformType;
import com.rokuality.server.enums.RokuButton;
import com.rokuality.server.enums.SessionCapabilities;
import com.rokuality.server.utils.FileToStringUtils;
import com.rokuality.server.utils.FileUtils;
import com.rokuality.server.utils.ImageUtils;
import com.rokuality.server.utils.ServletJsonParser;
import com.rokuality.server.utils.SleepUtils;

import org.eclipse.jetty.util.log.Log;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@SuppressWarnings({ "serial", "unchecked" })
public class session extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

		JSONObject requestObj = new ServletJsonParser().getRequestJSON(request, response);

		JSONObject results = null;

		String action = requestObj.get(ServerConstants.SERVLET_ACTION).toString();
		switch (action) {
		case "start":
			Log.getRootLogger().info("Starting new session.");
			results = startSession(requestObj);
			break;
		case "stop":
			Log.getRootLogger().info("Stopping session.");
			results = stopSession(requestObj);
		default:

			break;
		}

		response.setContentType("application/json");

		if (results != null) {
			response.getWriter().println(results.toJSONString());
		}

		if (results != null && results.containsValue(ServerConstants.SERVLET_SUCCESS)) {
			response.setStatus(HttpServletResponse.SC_OK);
		} else {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

	}

	public static JSONObject startSession(JSONObject requestObj) {
		JSONObject sessionInfo = new JSONObject();
		String sessionID = UUID.randomUUID().toString();
		String deviceIP = null;
		PlatformType platformType = null;
		OCRType ocrType = null;
		String appPackage = null;
		File imageCollectionDir = null;
		Double imageMatchSimilarity = null;
		String screenSizeOverride = null;

		String deviceUsername = null;
		String devicePassword = null;

		platformType = PlatformType
				.getEnumByString(String.valueOf(requestObj.get(SessionCapabilities.PLATFORM.value())));
		if (platformType == null) {
			sessionInfo.put(ServerConstants.SERVLET_RESULTS,
					String.format("The %s capability cannot be null", SessionCapabilities.PLATFORM.value()));
			return sessionInfo;
		}
		sessionInfo.put(SessionConstants.PLATFORM, platformType.value());

		boolean tesseractInstalled = GlobalDependencyInstaller.isTesseractInstalled();
		if (!tesseractInstalled) {
			sessionInfo.put(ServerConstants.SERVLET_RESULTS, String.format(
					"Unable to find tesseract on your path! Is it installed and available? See the Rokuality Server"
							+ " README for details but it can easily be installed via 'brew install tesseract' "
							+ "on MAC and via 'scoop install tesseract' for windows.",
					SessionCapabilities.PLATFORM.value()));
			return sessionInfo;
		}

		if (isXBox(platformType)) {
			boolean nodeInstalled = GlobalDependencyInstaller.isNodeInstalled();
			if (!nodeInstalled) {
				sessionInfo.put(ServerConstants.SERVLET_RESULTS, String.format(
						"Unable to find node on your path! Is it installed and available? See the Rokuality Server"
								+ " README for details but it can easily be installed via 'brew install node' "
								+ "on MAC and via 'scoop install nodejs' for windows.",
						SessionCapabilities.PLATFORM.value()));
				return sessionInfo;
			}
		}
		
		deviceIP = (String) requestObj.get(SessionCapabilities.DEVICE_IP_ADDRESS.value());
		if (deviceIP == null || deviceIP.isEmpty()) {
			sessionInfo.put(ServerConstants.SERVLET_RESULTS, String.format("The %s capability cannot be null or empty!",
					SessionCapabilities.DEVICE_IP_ADDRESS.value()));
			return sessionInfo;
		}
		sessionInfo.put(SessionConstants.DEVICE_IP, deviceIP);

		if (isXBox(platformType)) {
			String deviceName = (String) requestObj.get(SessionCapabilities.DEVICE_NAME.value());
			if (deviceName == null || deviceName.isEmpty()) {
				sessionInfo.put(ServerConstants.SERVLET_RESULTS, String
						.format("The %s capability cannot be null or empty!", SessionCapabilities.DEVICE_NAME.value()));
				return sessionInfo;
			}
			sessionInfo.put(SessionConstants.DEVICE_NAME, deviceName);

			String hubIP = (String) requestObj.get(SessionCapabilities.HOME_HUB_IP_ADDRESS.value());
			if (hubIP == null || hubIP.isEmpty()) {
				sessionInfo.put(ServerConstants.SERVLET_RESULTS, String.format(
						"The %s capability cannot be null or empty!", SessionCapabilities.HOME_HUB_IP_ADDRESS.value()));
				return sessionInfo;
			}
			sessionInfo.put(SessionConstants.HOME_HUB_DEVICE_IP, hubIP);
		}

		appPackage = String.valueOf(requestObj.get(SessionCapabilities.APP_PACKAGE.value()));
		String app = String.valueOf(requestObj.get(SessionCapabilities.APP.value()));
		if (isRoku(platformType)
				&& ((appPackage.equals("null") && app.equals("null")) || (appPackage.isEmpty() && app.isEmpty()))) {
			sessionInfo.put(ServerConstants.SERVLET_RESULTS, String.format(
					"You must provide either the %1$s or %2$s capabilities! If the %1$s is provided then it must be "
							+ "either the path to a local sideloadable .zip package OR the url to a sideloadable .zip package. "
							+ "If the %2$s is provided then it must be the id of an already sideloaded .zip that exists on the device!",
					SessionCapabilities.APP_PACKAGE.value(), SessionCapabilities.APP.value()));
			return sessionInfo;
		}

		if (isXBox(platformType) && (app == "null" || app.isEmpty())) {
			sessionInfo.put(ServerConstants.SERVLET_RESULTS, String.format(
					"You must provide the %s capability that matches the id of the app you are trying to launch, or "
							+ "you are trying to install!",
					SessionCapabilities.APP.value()));
			return sessionInfo;
		}

		if (isXBox(platformType) && !appPackage.equals("null") && appPackage.isEmpty()) {
			sessionInfo.put(ServerConstants.SERVLET_RESULTS, String.format(
					"The %s capability is not valid! It must be either the path to a local .appxbundle OR a url "
							+ "to a .appxbundle that you are tryig to install.",
					SessionCapabilities.APP_PACKAGE.value()));
			return sessionInfo;
		}

		sessionInfo.put(SessionConstants.APP, app);
		sessionInfo.put(SessionConstants.APP_PACKAGE, appPackage);

		ocrType = OCRType.getEnumByString(String.valueOf(requestObj.get(SessionCapabilities.OCR_TYPE.value())));
		if (ocrType == null) {
			sessionInfo.put(ServerConstants.SERVLET_RESULTS,
					String.format("The %s capability cannot be null!", SessionCapabilities.OCR_TYPE.value()));
			return sessionInfo;
		}
		sessionInfo.put(SessionConstants.OCR_MODULE, ocrType.value());

		if (isRoku(platformType)) {
			deviceUsername = (String) requestObj.get(SessionCapabilities.DEVICE_USERNAME.value());
			if (deviceUsername == null || deviceUsername.isEmpty()) {
				sessionInfo.put(ServerConstants.SERVLET_RESULTS, String.format(
						"The %s capability cannot be null or empty!", SessionCapabilities.DEVICE_USERNAME.value()));
				return sessionInfo;
			}
			sessionInfo.put(SessionConstants.DEVICE_USERNAME, deviceUsername);

			devicePassword = (String) requestObj.get(SessionCapabilities.DEVICE_PASSWORD.value());
			if (devicePassword == null || devicePassword.isEmpty()) {
				sessionInfo.put(ServerConstants.SERVLET_RESULTS, String.format(
						"The %s capability cannot be null or empty!", SessionCapabilities.DEVICE_PASSWORD.value()));
				return sessionInfo;
			}
			sessionInfo.put(SessionConstants.DEVICE_PASSWORD, devicePassword);
		}

		imageCollectionDir = new File(
				DependencyConstants.TEMP_DIR.getAbsolutePath() + File.separator + "imagecollection_" + sessionID);
		sessionInfo.put(SessionConstants.IMAGE_COLLECTION_DIRECTORY, imageCollectionDir.getAbsolutePath());

		imageMatchSimilarity = (Double) requestObj.get(SessionCapabilities.IMAGE_MATCH_SIMILARITY.value());
		if (imageMatchSimilarity == null) {
			imageMatchSimilarity = SessionConstants.DEFAULT_IMAGE_MATCH_SIMILARITY;
		}
		sessionInfo.put(SessionConstants.IMAGE_MATCH_SIMILARITY, imageMatchSimilarity);

		screenSizeOverride = (String) requestObj.get(SessionCapabilities.SCREEN_SIZE_OVERRIDE.value());
		if (screenSizeOverride != null && !screenSizeOverride.toLowerCase().contains("x")) {
			sessionInfo.put(ServerConstants.SERVLET_RESULTS, String.format(
					"The Optional %s capability (if provided) must be in the widthxheight format! A working example would look like '1920x1080'.",
					SessionCapabilities.SCREEN_SIZE_OVERRIDE.value()));
			return sessionInfo;
		}

		if (screenSizeOverride != null) {
			sessionInfo.put(SessionConstants.SCREEN_SIZE_OVERRIDE, screenSizeOverride);
		}

		String googleCredentials = String.valueOf(requestObj.get(SessionCapabilities.GOOGLE_CREDENTIALS.value()));
		if (OCRType.GOOGLE_VISION.equals(ocrType) && googleCredentials.equals("null")) {
			sessionInfo.put(ServerConstants.SERVLET_RESULTS,
					String.format("The %s capability must be provided if using the Google Vision OCR module!",
							SessionCapabilities.GOOGLE_CREDENTIALS.value()));
			return sessionInfo;
		}

		GoogleCredentials credentials = null;
		if (OCRType.GOOGLE_VISION.equals(ocrType)) {
			try {
				credentials = prepareGoogleVision(googleCredentials);
			} catch (Exception e) {
				Log.getRootLogger().warn(e);
				sessionInfo.put(ServerConstants.SERVLET_RESULTS, String.format(
						"Failed to decode Google API credentials! Is your %s capability set with the proper path to your Google API json auth file?",
						SessionCapabilities.GOOGLE_CREDENTIALS.value()));
				return sessionInfo;
			}
		}

		JSONObject deviceOnlineResults = isRoku(platformType) ? info.getRokuDeviceInfo(deviceIP)
				: info.getXBoxDeviceInfo(deviceIP);
		if (deviceOnlineResults == null
				|| !ServerConstants.SERVLET_SUCCESS.equals(deviceOnlineResults.get(ServerConstants.SERVLET_RESULTS))) {
			sessionInfo.put(ServerConstants.SERVLET_RESULTS, String.format(
					"The device at %s did not respond! Is the device online and reachable on your network?", deviceIP));
			return sessionInfo;
		}

		if (isRoku(platformType)) {
			boolean homeScreenSuccess = returnToRokuHomeScreen(
					String.valueOf(sessionInfo.get(SessionConstants.DEVICE_IP)));
			if (!homeScreenSuccess) {
				sessionInfo.put(ServerConstants.SERVLET_RESULTS, String
						.format("The device at %s did not return to the home screen on session start!", deviceIP));
				return sessionInfo;
			}
		}

		JSONObject appHandleResults = isRoku(platformType) ? RokuPackageHandler.installPackage(requestObj)
				: XBoxPackageHandler.installPackage(requestObj);
		if (appHandleResults == null || !ServerConstants.SERVLET_SUCCESS
				.equals(String.valueOf(appHandleResults.get(ServerConstants.SERVLET_RESULTS)))) {
			sessionInfo.put(ServerConstants.SERVLET_RESULTS, appHandleResults.get(ServerConstants.SERVLET_RESULTS));
			sessionInfo.remove(SessionConstants.APP_PACKAGE);
			return sessionInfo;
		}
		sessionInfo.remove(SessionConstants.APP_PACKAGE);

		ImageCollector imageCollector = new ImageCollector(platformType, sessionID, deviceIP.toString(),
				imageCollectionDir, deviceUsername, devicePassword);
		boolean recordingStarted = imageCollector.startRecording();
		boolean imageCollectionStarted = false;
		if (recordingStarted) {
			imageCollectionStarted = imageCollector.waitForImageCollectionStart();
		}
		if (!imageCollectionStarted) {
			sessionInfo.put(ServerConstants.SERVLET_RESULTS, String.format(
					"Failed to start image collection on the device! Is the device provisionsed properly for test and listening at %s",
					deviceIP));
			imageCollector.stopRecording();
			FileUtils.deleteDirectory(imageCollectionDir);
			return sessionInfo;
		}

		File imageFile = imageCollector.getCurrentImage(true);
		if (imageFile == null || !imageFile.exists() || !imageFile.isFile()) {
			sessionInfo.put(ServerConstants.SERVLET_RESULTS, String.format(
					"Failed to capture starting image on the device! Is the device provisionsed properly for test and listening at %s",
					deviceIP));
			return sessionInfo;
		}

		String ocrText = ImageUtils.getTextFromImage(ocrType, imageFile, credentials);
		if (ocrText == null) {
			String ocrFailMessage = "Failed to perform OCR evaluation on the device!";
			ocrFailMessage = ocrFailMessage
					+ (OCRType.TESSERACT.equals(ocrType) ? ocrFailMessage + " Is tesseract installed on your machine?"
							: " Is there an issue with your Google Vision account?");
			sessionInfo.put(ServerConstants.SERVLET_RESULTS, ocrFailMessage);
			return sessionInfo;
		}

		if (imageFile != null && imageFile.exists() && imageFile.isFile()) {
			FileUtils.deleteFile(imageFile);
		}

		sessionInfo.put(SessionConstants.ELEMENT_FIND_TIMEOUT, 0L);

		sessionInfo.put(SessionConstants.SESSION_ID, sessionID);
		SessionManager.addImageCollector(sessionID, imageCollector);
		SessionManager.addSessionInfo(sessionID, sessionInfo);
		if (credentials != null) {
			SessionManager.addGoogleCredentials(sessionID, credentials);
		}

		SessionManager.addSessionActivity(sessionID, System.currentTimeMillis());

		sessionInfo.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
		Log.getRootLogger().info("Success session object " + sessionInfo.toJSONString(), new Object[] {});
		return sessionInfo;

	}

	public static JSONObject stopSession(JSONObject requestObj) {
		JSONObject resultObj = new JSONObject();

		String sessionID = String.valueOf(requestObj.get(SessionConstants.SESSION_ID));
		JSONObject sessionInfo = SessionManager.getSessionInfo(sessionID);

		if (sessionInfo == null) {
			resultObj.put(ServerConstants.SERVLET_RESULTS,
					"No session found during teardown for session " + String.valueOf(sessionID));
			return resultObj;
		}

		ImageCollector imageCollector = SessionManager.getImageCollector(sessionID);
		if (imageCollector == null) {
			resultObj.put(ServerConstants.SERVLET_RESULTS,
					"No image collector found during teardown for session " + String.valueOf(sessionID));
			return resultObj;
		}
		imageCollector.stopRecording();

		if (isRoku(PlatformType.getEnumByString(String.valueOf(sessionInfo.get(SessionConstants.PLATFORM))))) {
			returnToRokuHomeScreen(String.valueOf(sessionInfo.get(SessionConstants.DEVICE_IP)));
		}

		if (isXBox(PlatformType.getEnumByString(String.valueOf(sessionInfo.get(SessionConstants.PLATFORM))))) {
			String appID = String.valueOf(sessionInfo.get(SessionConstants.APP));
			returnToXBoxHomeScreen(String.valueOf(sessionInfo.get(SessionConstants.DEVICE_IP)), appID);
		}

		String capturePath = (String) sessionInfo.get(SessionConstants.IMAGE_COLLECTION_DIRECTORY);
		if (capturePath == null || !new File(capturePath).exists()) {
			resultObj.put(ServerConstants.SERVLET_RESULTS,
					"No image capture directory found during teardown for session " + String.valueOf(sessionID));
			return resultObj;
		}
		File captureDir = new File(capturePath);
		FileUtils.deleteDirectory(captureDir);

		// TODO - app uninstall handling

		SessionManager.removeSessionActivity(sessionID);
		SessionManager.removeGoogleCredentials(sessionID);
		SessionManager.removeImageCollector(sessionID);
		SessionManager.removeSessionInfo(sessionID);

		resultObj.put(ServerConstants.SERVLET_RESULTS, ServerConstants.SERVLET_SUCCESS);
		return resultObj;
	}

	private static GoogleCredentials prepareGoogleVision(String content) throws IOException {
		String data = new FileToStringUtils().convertFromBaseStringToString(content);
		InputStream resourceAsStream = new ByteArrayInputStream(data.getBytes());
		GoogleCredentials credential = GoogleCredentials.fromStream(resourceAsStream);

		return credential;
	}

	private static boolean returnToXBoxHomeScreen(String deviceIP, String appID) {
		return new XBoxDevConsoleManager(deviceIP).closeApp(appID);
	}

	private static boolean returnToRokuHomeScreen(String deviceIP) {
		long pollStart = System.currentTimeMillis();
		long pollMax = pollStart + 15 * 1000; // 10 seconds from now - TODO - config/constant
		while (System.currentTimeMillis() < pollMax) {
			boolean onHomeScreen = isRokuHomeScreenLoaded(deviceIP);
			if (onHomeScreen) {
				return true;
			}

			try {
				RokuKeyPresser.rokuKeyPresser(deviceIP, RokuButton.getDeviceButton(RokuButton.HOME));
			} catch (Exception e) {
				Log.getRootLogger()
						.warn(String.format(
								"Failed to perform return to home screen during " + "session start/stop for device %s.",
								String.valueOf(deviceIP)));
			}
			SleepUtils.sleep(100);
		}
		return false;
	}

	private static boolean isRokuHomeScreenLoaded(String deviceIP) {
		RokuDevAPIManager rokuDevAPIManager = new RokuDevAPIManager(deviceIP, "/query/active-app", "GET");
		rokuDevAPIManager.sendDevAPICommand();
		String output = rokuDevAPIManager.getResponseContent();
		return (output != null && output.contains("<app>Roku</app>"));
	}

	private static boolean isRoku(PlatformType platformType) {
		return PlatformType.ROKU.equals(platformType);
	}

	private static boolean isXBox(PlatformType platformType) {
		return PlatformType.XBOX.equals(platformType);
	}

}
