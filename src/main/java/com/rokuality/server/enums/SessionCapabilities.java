package com.rokuality.server.enums;

public enum SessionCapabilities {

	PLATFORM("Platform"), APP("App"), APP_PACKAGE("AppPackage"), DEVICE_IP_ADDRESS("DeviceIPAddress"),
	DEVICE_USERNAME("DeviceUsername"), DEVICE_PASSWORD("DevicePassword"), IMAGE_MATCH_SIMILARITY("ImageMatchSimilarity"),
	SCREEN_SIZE_OVERRIDE("ScreenSizeOverride"), OCR_TYPE("OCRType"), GOOGLE_CREDENTIALS("GoogleCredentials"),
	DEVICE_NAME("DeviceName"), HOME_HUB_IP_ADDRESS("HomeHubIPAddress");

	private final String value;

	private SessionCapabilities(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	public static SessionCapabilities getEnumByString(String value) {
		for (SessionCapabilities capType : SessionCapabilities.values()) {
			if (value.equals(capType.value)) {
				return capType;
			}
		}
		return null;
	}

}
