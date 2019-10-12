package com.rokuality.server.enums;

public enum XBoxButton {

	RIGHT_ARROW("DirectionRight"), LEFT_ARROW("DirectionLeft"), DOWN_ARROW("DirectionDown"), 
      UP_ARROW("DirectionUp"), A("A"), B("B"), X("X"), Y("Y"), XBOX("Xbox"), INFO("Info"),
      PLAY ("Play"), PAUSE ("Pause"), FAST_FORWARD ("FastForward"), REWIND ("Rewind");

	private final String value;

	private XBoxButton(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	public static XBoxButton getEnumByString(String value) {
		for (XBoxButton tvButton : XBoxButton.values()) {
			if (value.equalsIgnoreCase(tvButton.value)) {
				return tvButton;
			}
		}
		return null;
	}

}
