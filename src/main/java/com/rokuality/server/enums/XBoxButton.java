package com.rokuality.server.enums;

public enum XBoxButton {

	RIGHT_ARROW("DirectionRight"), LEFT_ARROW("DirectionLeft"), DOWN_ARROW("DirectionDown"), UP_ARROW("DirectionUp"),
	A("A"), B("B"), X("X"), Y("Y"), MENU("Menu"), PLAY("Play"), PAUSE("Pause"), FAST_FORWARD("FastForward"),
	REWIND("Rewind"), XBOX("Xbox"), VIEW("View");

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

	public static String getSmartglassInput(XBoxButton button) {
		switch (button) {
			case UP_ARROW:
			return "/input/dpad_up";
			case DOWN_ARROW:
			return "/input/dpad_down";
			case RIGHT_ARROW:
			return "/input/dpad_right";
			case LEFT_ARROW:
			return "/input/dpad_left";
			case A:
			return "/input/a";
			case B:
			return "/input/b";
			case X:
			return "/input/x";
			case Y:
			return "/input/y";
			case MENU:
			return "/input/menu";
			case VIEW:
			return "/input/view";
			case PLAY:
			return "/media/play";
			case PAUSE:
			return "/media/pause";
			case FAST_FORWARD:
			return "/media/fast_forward";
			case REWIND:
			return "/media/rewind";

			default:
			return "";
		}
	}

}
