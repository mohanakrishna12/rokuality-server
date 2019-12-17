package com.rokuality.server.enums;

public enum SessionStatus {

	PASSED("passed"), 
	FAILED("failed"),
	BROKEN("broken"),
	IN_PROGRESS("in progress");

	private final String value;

	private SessionStatus(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	public static SessionStatus getEnumByString(String value) {
		for (SessionStatus status : SessionStatus.values()) {
			if (value.equalsIgnoreCase(status.value)) {
				return status;
			}
		}
		return null;
	}

}
