package com.rokuality.server.enums;

import java.util.stream.Stream;

public enum SpecialCharacters {

	AT('@', "%40"), ASTERISK('*', "%2A"), APOSTROPHE('\'', "%27"), AMPERSAND('&', "%26"), COLON(':', "%3A"),
	COMMA(',', "%2C"), DOLLAR('$', "%24"), EQUAL('=', "%3D"), FORWARD_SLASH('/', "%2F"), GRAVE_ACCENT('`', "%60"),
	GREATER('>', "%3E"), LESS('<', "%3C"), LEFT_CURLY('{', "%7B"), LEFT_SQUARE('[', "%5C"),
	LEFT_PARENTHESIS('(', "%28"), MINUS('-', "%2D"), PERCENT('%', "%25"), POUND('#', "%23"), PLUS('+', "%2B"),
	QUESTION('?', "%3F"), SEMICOLON(';', "%3B"), SPACE(' ', "%20"), TILDE('~', "%7E"), RIGHT_CURLY('}', "%7D"),
	RIGHT_SQUARE(']', "%5D"), RIGHT_PARENTHESIS(')', "%29");

	private final char symbol;
	private final String code;

	SpecialCharacters(char symbol, String code) {
		this.symbol = symbol;
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	public char getSymbol() {
		return symbol;
	}

	public static String getCodeBySymbol(char symbol) {
		return Stream.of(values()).filter(c -> c.getSymbol() == symbol).findFirst().map(SpecialCharacters::getCode)
				.orElseThrow(() -> new RuntimeException("Unable to find code for symbol: " + symbol));
	}

	public static boolean isKnownSymbol(char symbol) {
		return Stream.of(values()).anyMatch(c -> c.getSymbol() == symbol);
	}
}
