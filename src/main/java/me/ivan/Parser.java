package me.ivan;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
    private char[] uXXXX;
    private Stack<State> states;

    public Parser(final Reader reader) {
        this.reader = reader;
        this.eof = false;
        this.r = 0;
        this.off = 0;
        this.len = bufSize;
        this.buf = new char[bufSize];
        this.stringBuilder = new StringBuilder();
        this.uXXXX = new char[4];
        this.objects = new Stack<>();
        this.states = new Stack<>();

//        this.states.push(State.READING_ANY_DONE);

//        this.states.push(State.READ_ANY);
//        this.states.push(State.WS);

        plan(State.WS, State.ANY, State.DONE);
    }

    private void plan(final State... args) {
        for (int i = args.length; i > 0; i--) {
            states.push(args[i-1]);
        }
    }

    private int charToInt(final char c) {
        return switch (c) {
            case '0' -> 0;
            case '1' -> 1;
            case '2' -> 2;
            case '3' -> 3;
            case '4' -> 4;
            case '5' -> 5;
            case '6' -> 6;
            case '7' -> 7;
            case '8' -> 8;
            case '9' -> 9;
            case 'a', 'A' -> 10;
            case 'b', 'B' -> 11;
            case 'c', 'C' -> 12;
            case 'd', 'D' -> 13;
            case 'e', 'E' -> 14;
            case 'f', 'F' -> 15;
            default -> throw new RuntimeException("wrong char");
        };
    }

    private static enum State {
        ANY,
        WS, COLON, QUOTE,
        ARR, ARR_NEXT,
        STR, STR_ESC, ESC_U, U_X, U_XX, U_XXX,
        N, NU, NUL,
        T, TR, TRU,
        F, FA, FAL, FALS,
        OBJ, OBJ_NEXT,
        DONE,
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

    private void collapseList() {
        final Object last = objects.pop();
        final List list = (List) objects.peek();
        list.add(last);
    }

    private void collapseMap() {
        final Object v = objects.pop();
        final Object k = objects.pop();
        final Map map = (Map) objects.peek();
        map.put(k, v);
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

    private char getUUUUChar() {
        return (char) (
                ((charToInt(uXXXX[0]) & 0xFF) << 24) |
                ((charToInt(uXXXX[1]) & 0xFF) << 16) |
                ((charToInt(uXXXX[2]) & 0xFF) <<  8) |
                ((charToInt(uXXXX[3]) & 0xFF))
        );
    }

    private void parseBuf() {
        char c;
        for (int i = 0; i < off; i++) {
            c = buf[i];
            switch (states.peek()) {

                case WS -> {
                    switch (c) {
                        case ' ', '\n', '\r', '\t' -> {}
                        default -> {
                            i--;
                            states.pop();
                        }
                    }
                }

                case ANY -> {
                    states.pop();
                    switch (c) {
                        case '"' -> states.push(State.STR);
                        case 'n' -> states.push(State.N);
                        case '{' -> {
                            objects.push(new HashMap<>());
                            plan(State.WS, State.OBJ);
                        }
                        case '[' -> {
                            objects.push(new ArrayList<>());
                            plan(State.WS, State.ARR);
                        }
                        default -> throw new RuntimeException(objects.toString());
                    }
                }
                case STR -> {
                    switch (c) {
                        case '\\' -> states.push(State.STR_ESC);
                        case '"' -> {
                            final String string = stringBuilder.toString();
                            stringBuilder.setLength(0);
                            objects.push(string);
                            states.pop();
                        }
                        default -> stringBuilder.append(c);
                    }
                }
                case STR_ESC -> {
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
                        case 'u' -> plan(State.ESC_U);
                    }
                }
                case ESC_U -> {
                    if (isHex(c)) {
                        uXXXX[0] = c;
                        plan(State.U_X);
                    } else {
                        throw new RuntimeException();
                    }
                }
                case U_X -> {
                    if (isHex(c)) {
                        uXXXX[1] = c;
                        plan(State.U_XX);
                    } else {
                        throw new RuntimeException();
                    }
                }
                case U_XX -> {
                    if (isHex(c)) {
                        uXXXX[2] = c;
                        plan(State.U_XXX);
                    } else {
                        throw new RuntimeException();
                    }
                }
                case U_XXX -> {
                    if (isHex(c)) {
                        uXXXX[3] = c;
                        final char cXXXX = getUUUUChar();
                        stringBuilder.append(cXXXX);
                        states.pop();
                        states.pop();
                        states.pop();
                        states.pop();
                    } else {
                        throw new RuntimeException();
                    }
                }
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

                case ARR_NEXT -> {
                    states.pop();
                    collapseList();
                    switch (c) {
                        case ',' ->
                                plan(State.WS, State.ANY, State.WS, State.ARR_NEXT);
                        default -> i--;

                    }
                }
                case ARR -> {
                    switch (c) {
                        case ']' -> states.pop();
                        default -> {
                            i--;
                            plan(State.WS, State.ANY, State.WS, State.ARR_NEXT);
                        }
                    }
                }
                case OBJ_NEXT -> {
                    states.pop();
                    collapseMap();
                    switch (c) {
                        case ',' ->
                                plan(State.WS, State.QUOTE, State.STR, State.WS, State.COLON, State.WS, State.ANY, State.WS, State.OBJ_NEXT);
                        default -> i--;
                    }
                }
                case COLON -> {
                    switch (c) {
                        case ':' -> states.pop();
                        default -> throw new RuntimeException(String.format("not a colon: %s", c));
                    }
                }
                case QUOTE -> {
                    switch (c) {
                        case '"' -> states.pop();
                        default -> throw new RuntimeException(String.format("not a quote: %s", c));
                    }
                }
                case OBJ -> {
                    switch (c) {
                        case '}' -> states.pop();
                        default -> {
                            i--;
                            plan(State.QUOTE, State.STR, State.WS, State.COLON, State.WS, State.ANY, State.WS, State.OBJ_NEXT);
                        }
                    }
                }
                case DONE -> {
                    //states.pop();
                    break;
                }
                default -> throw new RuntimeException(String.format("unknown state: %s", states.peek()));
//                case READING_ARR_ITEM_READ_NEXT -> {
//                    switch (c) {
//                        case ' ' -> {}
//                        case ']' -> states.pop();
//                        case ',' -> {
//                            states.push(State.READING_ARR_ITEM_READ);
//                            states.push(State.READ_ANY);
//                        }
//                    }
//                }
//                case READING_ARR_ITEM_READ -> {
//                    commit();
//                    i--;
//                    states.pop();
//                    states.push(State.READING_ARR_ITEM_READ_NEXT);
//
//                }

            }
        }
    }

    private Object complete() {
        return null;
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
//        final Parser p = new Parser(new StringReader("[ \"abc\" , \"xyz\" , [\"ccc\" , \"aaa\" ] ]"));
        final Parser p = new Parser(new StringReader("  \"abc\u015Cde\"  "));
//        final Parser p = new Parser(new StringReader("   "));
//        final Parser p = new Parser(new StringReader("[ \"abc\" , \"xyz\", [ \"a\",  [ \"a\", \"a\", \"a\"  ], \"a\", \"a\"  ], \"1\" , \"2\", \"3\" ]"));


        p.parse();
        System.out.println(p.objects.peek());
    }


}
