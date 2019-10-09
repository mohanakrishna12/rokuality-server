package com.rokuality.server.enums;

public enum OSType {

	MAC("mac"), WINDOWS("windows"), LINUX("linux");

	private final String value;

	private OSType(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	public static OSType getEnumByString(String value) {
		for (OSType osType : OSType.values()) {
			if (value.equalsIgnoreCase(osType.value)) {
				return osType;
			}
		}
		return null;
	}

}
