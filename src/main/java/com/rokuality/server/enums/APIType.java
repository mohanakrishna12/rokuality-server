package com.rokuality.server.enums;

public enum APIType {

	ROKU_WEBDRIVER("RokuWebDriver"), ROKU_DEV_API("RokuDevAPI"), XBOX_SMARTGLASS("XBoxSmartglass");

	private final String value;

	APIType(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	public static APIType getEnumByString(String value) {
		for (APIType type : APIType.values()) {
			if (value.equalsIgnoreCase(type.value)) {
				return type;
			}
		}
		return null;
	}

}
