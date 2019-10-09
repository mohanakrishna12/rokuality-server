package com.rokuality.server.enums;

public enum PlatformType {

	ROKU("roku"), CHROMECAST("chromecast"), // coming soon
	XBOX("xbox"), // coming soon
	PLAYSTATION("playstation"), // coming soon
	HDMI("hdmi"); // coming soon

	private final String value;

	private PlatformType(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	public static PlatformType getEnumByString(String value) {
		for (PlatformType platformType : PlatformType.values()) {
			if (value.equalsIgnoreCase(platformType.value)) {
				return platformType;
			}
		}
		return null;
	}

}
