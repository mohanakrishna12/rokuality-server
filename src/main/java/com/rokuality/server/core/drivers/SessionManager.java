package com.rokuality.server.core.drivers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.auth.oauth2.GoogleCredentials;
import com.rokuality.server.constants.SessionConstants;
import com.rokuality.server.core.ImageCollector;
import com.rokuality.server.enums.OCRType;
import com.rokuality.server.enums.PlatformType;

import org.eclipse.jetty.util.log.Log;
import org.json.simple.JSONObject;

@SuppressWarnings("unchecked")
public class SessionManager {

	private static Map<String, ImageCollector> imageCollectorMap = Collections
			.synchronizedMap(new HashMap<String, ImageCollector>());
	private static Map<String, Long> sessionActivityMap = Collections
			.synchronizedMap(new HashMap<String, Long>());
	private static Map<String, JSONObject> sessionInfoMap = Collections
			.synchronizedMap(new HashMap<String, JSONObject>());
	private static Map<String, GoogleCredentials> googleCredentialMap = Collections.synchronizedMap(new HashMap<String, GoogleCredentials>());

	public static void addSessionInfo(String sessionID, JSONObject sessionInfo) {
		sessionInfoMap.put(sessionID, sessionInfo);
	}

	public static void removeSessionInfo(String sessionID) {
		sessionInfoMap.remove(sessionID);
	}

	public static JSONObject getSessionInfo(String sessionID) {
		return sessionInfoMap.get(sessionID);
	}

	public static void updateSessionInfoItem(String sessionID, String keyToUpdate, Object valueToUpdate) {
		JSONObject json = getSessionInfo(sessionID);
		if (json != null) {
			json.put(keyToUpdate, valueToUpdate);
			addSessionInfo(sessionID, json);
		}
	}

	public static void addGoogleCredentials(String sessionID, GoogleCredentials credentials) {
		googleCredentialMap.put(sessionID, credentials);
	}

	public static void removeGoogleCredentials(String sessionID) {
		googleCredentialMap.remove(sessionID);
	}

	public static GoogleCredentials getGoogleCredentials(String sessionID) {
		return googleCredentialMap.get(sessionID);
	}

	public static void addImageCollector(String sessionID, ImageCollector imageCollector) {
		imageCollectorMap.put(sessionID, imageCollector);
	}

	public static void removeImageCollector(String sessionID) {
		imageCollectorMap.remove(sessionID);
	}

	public static ImageCollector getImageCollector(String sessionID) {
		return imageCollectorMap.get(sessionID);
	}

	public static void addSessionActivity(String sessionID, long activityTime) {
		sessionActivityMap.put(sessionID, activityTime);
	}

	public static void removeSessionActivity(String sessionID) {
		sessionActivityMap.remove(sessionID);
	}

	public static void updateSessionActivity(String sessionID) {
		sessionActivityMap.put(sessionID, System.currentTimeMillis());
	}

	public static Map<String, Long> getAllSessionActivityEntries() {
		return sessionActivityMap;
	}

	public static boolean isSessionInitiated(String sessionID) {
		return getSessionInfo(sessionID) != null;
	}

	public static boolean isRoku(String sessionID) {
		return String.valueOf(getSessionInfo(sessionID).get(SessionConstants.PLATFORM))
				.equals(PlatformType.ROKU.value());
	}

	public static boolean isXBox(String sessionID) {
		return String.valueOf(getSessionInfo(sessionID).get(SessionConstants.PLATFORM))
				.equals(PlatformType.XBOX.value());
	}

	public static boolean isHDMI(String sessionID) {
		return String.valueOf(getSessionInfo(sessionID).get(SessionConstants.PLATFORM))
				.equals(PlatformType.HDMI.value());
	}

	public static OCRType getOCRType(String sessionID) {
		String ocrModule = (String) getSessionInfo(sessionID).get(SessionConstants.OCR_MODULE);
		if (ocrModule == null) {
			Log.getRootLogger().info("Using default tesseract OCR type.", sessionID);
			return OCRType.TESSERACT;
		}
		return OCRType.getEnumByString(ocrModule);
	}

	public static boolean isTesseractOCR(String sessionID) {
		return OCRType.TESSERACT.equals(getOCRType(sessionID));
	}

	public static boolean isGoogleVisionOCR(String sessionID) {
		return OCRType.GOOGLE_VISION.equals(getOCRType(sessionID));
	}

}
