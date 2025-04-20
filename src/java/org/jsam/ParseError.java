package org.jsam;

public class ParseError extends RuntimeException {

    private ParseError (final String message) {
        super(message);
    }

    private ParseError (final Throwable cause, final String message) {
        super(message, cause);
    }

    public static ParseError error(final String message) {
        return new ParseError(message);
    }

    public static ParseError error(final String template, final Object... args) {
        return new ParseError(String.format(template, args));
    }

    public static ParseError error(final Throwable cause, final String template, final Object... args) {
        return new ParseError(cause, String.format(template, args));
    }
}
