package me.ivan;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Parser {

    public static final int bufSize = 32;
    private int r;
    private int off;
    private int len;
    private boolean eof;
    private final Reader reader;
    private final char[] buf;
    private StringBuilder stringBuilder;
    private Stack<Object> objects;
    private StringBuilder uXXXX;
    private Stack<State> states;

    public Parser(final Reader reader) {
        this.reader = reader;
        this.eof = false;
        this.r = 0;
        this.off = 0;
        this.len = bufSize;
        this.buf = new char[bufSize];
        this.stringBuilder = new StringBuilder();
        this.uXXXX = new StringBuilder(4);
        this.objects = new Stack<>();
        this.states = new Stack<>();

//        this.states.push(State.READING_ANY_DONE);

        this.states.push(State.READ_ANY_DONE);
        this.states.push(State.READ_ANY);
    }

    private static enum State {
        READ_ANY,
        READ_ANY_DONE,
        READING_ANY_DONE,

        READING_STRING,
        READING_STRING_DONE,

        READING_ARR_ITEM,

        READING_ARR_ITEM_READ_NEXT,
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
        READING_ARR_START,
        READING_ARR_DONE,
        READING_ARR_ITEM_READ,


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

    private void commit() {
        final Object curr = objects.pop();
        final Object prev = objects.pop();
        if (prev instanceof List list) {
            list.add(curr);
            objects.push(list);
        } else if (prev == null) {
            objects.push(curr);
        }
    }

    private void emit(final Object value) {

    }

    private void rollback() {
        states.pop();
        final State prev = states.pop();
        switch (prev) {
            case READING_ARR_ITEM -> {
                states.push(State.READING_ARR_ITEM_DONE);
            }
        }
    }

    private void parseBuf() {
        char c;
        for (int i = 0; i < off; i++) {
            c = buf[i];
            switch (states.peek()) {
                case READ_ANY, READING_ARR_ITEM -> {
                    switch (c) {
                        case ' ' -> {}
                        case '"' -> states.push(State.READING_STRING);
                        case 'n' -> states.push(State.N);
                        case '{' -> states.push(State.READING_OBJ);
                        case '[' -> {
                            objects.push(new ArrayList<>());
                            states.push(State.READING_ARR);
                        }
                        default -> throw new RuntimeException("sss");
                    }
                }
                case READING_STRING -> {
                    switch (c) {
                        case '\\' -> states.push(State.READING_STRING_ESCAPED);
                        case '"' -> {
                            final String string = stringBuilder.toString();
                            stringBuilder.setLength(0);
                            objects.push(string);
                            states.pop();
                        }
                        default -> stringBuilder.append(c);
                    }
                }
                case READING_STRING_ESCAPED -> {
                    switch (c) {
                        case '"' -> {
                            stringBuilder.append('\"'); states.pop();}
                        case '\\' -> {
                            stringBuilder.append('\\'); states.pop();}
                        case '/' -> {
                            stringBuilder.append('/'); states.pop();}
                        case 'b' -> {
                            stringBuilder.append('b'); states.pop();}
                        case 'f' -> {
                            stringBuilder.append('f'); states.pop();}
                        case 'n' -> {
                            stringBuilder.append('\n'); states.pop();}
                        case 'r' -> {
                            stringBuilder.append('\r'); states.pop();}
                        case 't' -> {
                            stringBuilder.append('\t'); states.pop();}
//                        case 'u' -> {state = State.READING_STRING_ESCAPED_U;}
                    }
                }
//                case READING_STRING_ESCAPED_U -> {
//                    if (isHex(c)) {
//                        state = State.READING_STRING_ESCAPED_U_X;
//                    } else {
//                        throw new RuntimeException();
//                    }
//                }
//                case READING_STRING_ESCAPED_U_X -> {
//                    if (isHex(c)) {
//                        uXXXX.append(c);
//                        state = State.READING_STRING_ESCAPED_U_XX;
//                    } else {
//                        throw new RuntimeException();
//                    }
//                }
//                case READING_STRING_ESCAPED_U_XX -> {
//                    if (isHex(c)) {
//                        uXXXX.append(c);
//                        state = State.READING_STRING_ESCAPED_U_XXX;
//                    } else {
//                        throw new RuntimeException();
//                    }
//                }
//                case READING_STRING_ESCAPED_U_XXX -> {
//                    if (isHex(c)) {
//                        uXXXX.append(c);
//                        final char cXXXX = (char) Integer.parseInt(uXXXX.toString(), 16);
//                        stringBuilder.append(cXXXX);
//                        uXXXX.setLength(0);
//                        state = State.READING_STRING;
//                    } else {
//                        throw new RuntimeException();
//                    }
//                }
//                case N -> {
//                    switch (c) {
//                        case 'u' -> state = State.NU;
//                        default -> throw new RuntimeException();
//                    }
//                }
//                case NU -> {
//                    switch (c) {
//                        case 'l' -> state = State.NUL;
//                        default -> throw new RuntimeException();
//                    }
//                }
//                case NUL -> {
//                    switch (c) {
//                        case 'l' -> {
//                            System.out.println("null");
//                            state = State.READY;
//                        }
//                        default -> throw new RuntimeException();
//                    }
//                }
//                case READING_OBJ -> {
//                    objStack.add(new HashMap<>());
//                    stateStack.push(State.READ_ALL, State.READING_STRING);
//                    switch (c) {
//                        case ' ' -> {}
//                        case '"' -> {state = State.READING_STRING;}
//                        default -> throw new RuntimeException();
//                    }
//                }

//                case READING_ARR_START -> {
//                    objects.push(new ArrayList<>());
//                    states.pop();
//                    states.push(State.READING_ARR);
//                }
                case READING_ARR -> {
                    switch (c) {
                        case ' ' -> {}
                        case ']' -> {commit(); states.pop();}
                        default -> {
                            i--;
                            states.push(State.READING_ARR_ITEM);
//                            states.push(State.READ_ANY);

//                            states.push(State.READ_ANY);
                        }
                    }
                }
                case READING_ARR_ITEM_READ_NEXT -> {
                    switch (c) {
                        case ' ' -> {}
                        case ']' -> states.pop();
                        case ',' -> {
                            states.push(State.READING_ARR_ITEM_READ);
                            states.push(State.READ_ANY);
                        }
                    }
                }
                case READING_ARR_ITEM_READ -> {
                    commit();
                    i--;
                    states.pop();
                    states.push(State.READING_ARR_ITEM_READ_NEXT);

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
        final Parser p = new Parser(new StringReader("[ \"abc\" , \"xyz\" , [\"ccc\" , \"aaa\" ] ]"));
//        final Parser p = new Parser(new StringReader("  \"absdfsdfsdfc\"  "));
//        final Parser p = new Parser(new StringReader("   "));
        p.parse();
        System.out.println(p.objects);
    }


}
