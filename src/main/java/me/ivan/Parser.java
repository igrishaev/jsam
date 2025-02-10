package me.ivan;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class Parser {

    public static final int bufSize = 1;
    private int r;
    private int off;
    private int len;
    private boolean eof;
    private final Reader reader;
    private final char[] buf;
    private State state;
    private StringBuilder sb;
    private Stack<Object> objStack;
    private Stack<State> stateStack;
    private StringBuilder uXXXX;

    public Parser(final Reader reader) {
        this.reader = reader;
        this.eof = false;
        this.r = 0;
        this.off = 0;
        this.len = bufSize;
        this.buf = new char[bufSize];
        this.state = State.READY;
        this.sb = new StringBuilder();
        this.uXXXX = new StringBuilder(4);
        this.objStack = new Stack<>();
        this.stateStack = new Stack<>();
        stateStack.push(State.READY);
    }

    private static enum State {
        READY,
        READING_STRING,
        READING_STRING_ESCAPED,
        READING_STRING_ESCAPED_U,
        READING_STRING_ESCAPED_U_X,
        READING_STRING_ESCAPED_U_XX,
        READING_STRING_ESCAPED_U_XXX,
        N,
        NU,
        NUL,
        READING_OBJ,
        READING_ARR,

        READ_ALL

    }

    private void fillBuffer() throws IOException {
        off = 0;
        len = bufSize;
        while (!eof) {
            r = reader.read(buf, off, len);
            if (r == -1) {
                eof = true;
            } else if (r == 0) {
                break;
            } else {
                off += r;
                len -= r;
            }
        }
    }

    private static boolean isHex(final char c) {
        return switch (c) {
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                    'a', 'b', 'c', 'd', 'e', 'f',
                    'A', 'B', 'C', 'D', 'E', 'F' ->
                    true;
            default -> false;
        };
    }

    private void parseBuf() {
        char c;
        for (int i = 0; i < off; i++) {
            c = buf[i];
            switch (stateStack.peek()) {
                case READY -> {
                    switch (c) {
                        case ' ' -> {}
                        case '"' -> state = State.READING_STRING;
                        case 'n' -> state = State.N;
                        case '{' -> state = State.READING_OBJ;
                        default -> throw new RuntimeException("sss");
                    }
                }
                case READING_STRING -> {
                    switch (c) {
                        case '\\' -> state = State.READING_STRING_ESCAPED;
                        case '"' -> {
                            System.out.println(sb);
                            stateStack.pop();
                            state = State.READY;
                            sb.setLength(0);
                        }
                        default -> sb.append(c);
                    }
                }
                case READING_STRING_ESCAPED -> {
                    switch (c) {
                        case '"' -> {sb.append('\"'); state = State.READING_STRING;}
                        case '\\' -> {sb.append('\\'); state = State.READING_STRING;}
                        case '/' -> {sb.append('/'); state = State.READING_STRING;}
                        case 'b' -> {sb.append('b'); state = State.READING_STRING;}
                        case 'f' -> {sb.append('f'); state = State.READING_STRING;}
                        case 'n' -> {sb.append('\n'); state = State.READING_STRING;}
                        case 'r' -> {sb.append('\r'); state = State.READING_STRING;}
                        case 't' -> {sb.append('\t'); state = State.READING_STRING;}
                        case 'u' -> {state = State.READING_STRING_ESCAPED_U;}
                    }
                }
                case READING_STRING_ESCAPED_U -> {
                    if (isHex(c)) {
                        state = State.READING_STRING_ESCAPED_U_X;
                    } else {
                        throw new RuntimeException();
                    }
                }
                case READING_STRING_ESCAPED_U_X -> {
                    if (isHex(c)) {
                        uXXXX.append(c);
                        state = State.READING_STRING_ESCAPED_U_XX;
                    } else {
                        throw new RuntimeException();
                    }
                }
                case READING_STRING_ESCAPED_U_XX -> {
                    if (isHex(c)) {
                        uXXXX.append(c);
                        state = State.READING_STRING_ESCAPED_U_XXX;
                    } else {
                        throw new RuntimeException();
                    }
                }
                case READING_STRING_ESCAPED_U_XXX -> {
                    if (isHex(c)) {
                        uXXXX.append(c);
                        final char cXXXX = (char) Integer.parseInt(uXXXX.toString(), 16);
                        sb.append(cXXXX);
                        uXXXX.setLength(0);
                        state = State.READING_STRING;
                    } else {
                        throw new RuntimeException();
                    }
                }
                case N -> {
                    switch (c) {
                        case 'u' -> state = State.NU;
                        default -> throw new RuntimeException();
                    }
                }
                case NU -> {
                    switch (c) {
                        case 'l' -> state = State.NUL;
                        default -> throw new RuntimeException();
                    }
                }
                case NUL -> {
                    switch (c) {
                        case 'l' -> {
                            System.out.println("null");
                            state = State.READY;
                        }
                        default -> throw new RuntimeException();
                    }
                }
                case READING_OBJ -> {
                    objStack.add(new HashMap<>());
                    stateStack.push(State.READ_ALL, State.READING_STRING);
                    switch (c) {
                        case ' ' -> {}
                        case '"' -> {state = State.READING_STRING;}
                        default -> throw new RuntimeException();
                    }
                }
                case READING_ARR -> {
                    switch (c) {
                        case ' ' -> {}
                        case ']' -> {}
                        default -> 
                    }
                    objStack.push(new ArrayList<>());
                    stateStack.push(State.READ_ALL);

                }

            }
        }
    }

    private Object complete() {
        return null;
    }

    private void printBuffer() {
        for (int i = 0; i < off; i++) {
            System.out.println(buf[i]);
        }
    }

    public Object parse() throws IOException {
        while (!eof) {
            fillBuffer();
//            printBuffer();
            parseBuf();
        }
        return complete();
    }

    public static void main(String[] args) throws IOException {
        final Parser p = new Parser(new StringReader("null"));
        p.parse();
    }


}
