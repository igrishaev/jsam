package org.jsam;

public class Error extends RuntimeException {
    private Error(final String message) {
        super(message);
    }
    private Error(final Throwable e, final String message) {
        super(message, e);
    }
    public static Error error(final String message) {
        return new Error(message);
    }
    public static Error error(final String template, final Object... args) {
        return new Error(String.format(template, args));
    }
    public static Error error(final Throwable e, final String message) {
        return new Error(e, message);
    }
}
