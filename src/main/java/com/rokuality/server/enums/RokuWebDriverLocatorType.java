package com.rokuality.server.enums;

public enum RokuWebDriverLocatorType {

	TEXT("text"), ATTR("attr"), TAG("tag");

	private final String value;

	RokuWebDriverLocatorType(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	public static RokuWebDriverLocatorType getEnumByString(String value) {
		for (RokuWebDriverLocatorType type : RokuWebDriverLocatorType.values()) {
			if (value.equalsIgnoreCase(type.value)) {
				return type;
			}
		}
		return null;
	}

}
