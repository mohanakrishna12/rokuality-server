package com.rokuality.server.enums;

public enum RokuAPIType {

	WEBDRIVER("WebDriver"), DEV_API("DevAPI");

	private final String value;

	RokuAPIType(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	public static RokuAPIType getEnumByString(String value) {
		for (RokuAPIType type : RokuAPIType.values()) {
			if (value.equalsIgnoreCase(type.value)) {
				return type;
			}
		}
		return null;
	}

}
