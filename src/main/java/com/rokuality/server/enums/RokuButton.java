package com.rokuality.server.enums;

public enum RokuButton {

	RIGHT_ARROW("rightArrow"), LEFT_ARROW("leftArrow"), DOWN_ARROW("downArrow"), UP_ARROW("upArrow"), SELECT("select"),
	BACK("back"), HOME("home"), PLAY("play"), PAUSE("pause"), FAST_FORWARD("fastForward"), REWIND("rewind"),
	OPTION("option");

	private final String value;

	private RokuButton(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	public static RokuButton getEnumByString(String value) {
		for (RokuButton tvButton : RokuButton.values()) {
			if (value.equalsIgnoreCase(tvButton.value)) {
				return tvButton;
			}
		}
		return null;
	}

	public static String getDeviceButton(RokuButton button) {
		if (button.equals(RIGHT_ARROW)) {
			return "Right";
		}

		if (button.equals(LEFT_ARROW)) {
			return "Left";
		}

		if (button.equals(DOWN_ARROW)) {
			return "Down";
		}

		if (button.equals(UP_ARROW)) {
			return "Up";
		}

		if (button.equals(SELECT)) {
			return "Select";
		}

		if (button.equals(BACK)) {
			return "Back";
		}

		if (button.equals(HOME)) {
			return "Home";
		}

		if (button.equals(PLAY)) {
			return "Play";
		}

		if (button.equals(PAUSE)) {
			return "Play";
		}

		if (button.equals(FAST_FORWARD)) {
			return "Fwd";
		}

		if (button.equals(REWIND)) {
			return "Rev";
		}

		if (button.equals(OPTION)) {
			return "Info";
		}

		return null;
	}

}
