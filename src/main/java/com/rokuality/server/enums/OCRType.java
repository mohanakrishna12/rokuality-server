package com.rokuality.server.enums;

public enum OCRType {

	TESSERACT("Tesseract"), GOOGLE_VISION("GoogleVision");

	private final String value;

	OCRType(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	public static OCRType getEnumByString(String value) {
		for (OCRType ocr : OCRType.values()) {
			if (value.equalsIgnoreCase(ocr.value)) {
				return ocr;
			}
		}
		return null;
	}

}
