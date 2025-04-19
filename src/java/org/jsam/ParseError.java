package org.jsam;

public class ParseError extends RuntimeException {
    private ParseError (final String message) {
        super(message);
    }
    public static ParseError error(final String message) {
        return new ParseError(message);
    }
    public static ParseError error(final String template, final Object... args) {
        return new ParseError(String.format(template, args));
    }
}
