package me.ivan;

import java.io.IOException;
import java.io.Reader;

public class Parser {

    public static final int bufSize = 64;
    private int r;
    private int off;
    private int len;
    private boolean eof;
    private final Reader reader;
    private final char[] buf;
    private State state;
    private StringBuilder sb;

    public Parser(final Reader reader) {
        this.reader = reader;
        this.eof = false;
        this.r = 0;
        this.off = 0;
        this.len = bufSize;
        this.buf = new char[bufSize];
        this.state = State.READY;
        this.sb = new StringBuilder();
    }

    private static enum State {
        READY,
        READING_STRING,
        READING_STRING_ESCAPED,
        N,
        NU,
        NUL,
        NULL
    }

    private void fillBuffer() throws IOException {
        while (!eof) {
            r = reader.read(buf, off, len);
            if (r == -1) {
                eof = true;
            } else {
                off += r;
                len -= r;
            }
        }
    }

    private void parseBuf() {
        char c;
        for (int i = 0; i < off; i++) {
            c = buf[i];
            switch (state) {
                case READY -> {
                    switch (c) {
                        case '"' -> state = State.READING_STRING;
                        case 'n' -> state = State.N;
                        default -> throw new RuntimeException("sss");
                    }
                }
                case READING_STRING_ESCAPED -> {
                    switch (c) {
                        case 'r' -> {sb.append('\r'); state = State.READING_STRING;}
                        case 'n' -> {sb.append('\n'); state = State.READING_STRING;}
                        case 't' -> {sb.append('\t'); state = State.READING_STRING;}
                    }
                }
                case READING_STRING -> {
                    switch (c) {
                        case '\\' -> state = State.READING_STRING_ESCAPED;
                        case '"' -> {
                            state = State.READY;
                            System.out.println(sb);
                            sb.setLength(0);
                        }
                        default -> sb.append(c);

                    }
                }
            }
        }
    }

    private Object complete() {
        return null;
    }

    public Object parse() throws IOException {
        while (!eof) {
            fillBuffer();
            parseBuf();
        }
        return complete();
    }

}
