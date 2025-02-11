package me.ivan;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Stack;

public class Parser {

    public static final int bufSize = 0xFFFF;
    private int r;
    private int off;
    private int len;
    private boolean eof;
    private final Reader reader;
    private final PushbackReader pushbackReader;
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
        this.pushbackReader = new PushbackReader(reader);
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
        N, U, L, NULL,
        T, R, E, TRUE,
        F, A, S, FALSE,
        OBJ, OBJ_NEXT,
        DONE,
        NUM, INT, UINT, FRAC, EXP, DIGITS, SIGN, NUM_DONE
    }

    private char read() {
        final int r;
        try {
            r = pushbackReader.read();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (r == -1) {
            throw new RuntimeException("EOF");
        } else {
            return (char) r;
        }
    }

    private void unread(final char c) {
        try {
            pushbackReader.unread(c);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void ws() {
        char c;
        while (true) {
            c = read();
            if (!(c == ' ' || c == '\r' || c == '\n' || c == '\t')) {
                unread(c);
                break;
            }
        }
    }

    private List<Object> readArray() {
        char c;
        Object el;
        boolean repeat = true;
        final List<Object> list = new ArrayList<>();
        ws();
        c = read();
        if (c != '[') {
            throw new RuntimeException();
        }
        ws();
        c = read();
        if (c != ']') {
            unread(c);
            while (repeat) {
                el = readAny();
                list.add(el);
                ws();
                c = read();
                if (c != ',') {
                    unread(c);
                    repeat = false;
                }
            }
            c = read();
            if (c != ']') {
                throw new RuntimeException();
            }
        }
        return list;
    }

    public Object readAny() {
        char c;
        ws();
        while (true) {
            c = read();
            if (c == 't') {
                unread(c);
                return readTrue();
            } else if (c == 'f') {
                unread(c);
                return readFalse();
            } else if (c == '[') {
                unread(c);
                return readArray();
            } else if (c == '"') {
                unread(c);
                return readString();
            }
        }
    }

    private boolean readTrue() {
        if (read() == 't' && read() == 'r' && read() == 'u' && read() == 'e') {
            return true;
        } else {
            throw new RuntimeException();
        }
    }

    private boolean readFalse() {
        if (read() == 'f' && read() == 'a' && read() == 'l' && read() == 's' && read() == 'e') {
            return false;
        } else {
            throw new RuntimeException();
        }
    }

    private String readString() {
        char c;
        c = read();
        if (c != '"') {
            throw new RuntimeException();
        }
        final StringBuilder sb = new StringBuilder();
        while (true) {
            c = read();
            if (c == '"') {
                return sb.toString();
            } else if (c == '\\') {
                c = read();
                if (c == 'r') {
                    sb.append('\r');
                } else if (c == 'n') {
                    sb.append('\n');
                } else {
                    throw new RuntimeException();
                }
            } else {
                sb.append(c);
            }

        }
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
                        case 'n' -> {
                            i--;
                            plan(State.N, State.U, State.L, State.L, State.NULL);
                        }
                        case 't' -> {
                            i--;
                            plan(State.T, State.R, State.U, State.E, State.TRUE);
                        }
                        case 'f' -> {
                            i--;
                            plan(State.F, State.A, State.L, State.S, State.E, State.FALSE);
                        }
                        case '{' -> {
                            objects.push(new HashMap<>());
                            plan(State.WS, State.OBJ);
                        }
                        case '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                            i--;
                            plan(State.INT, State.FRAC, State.EXP, State.NUM_DONE);
                        }
                        case '[' -> {
                            objects.push(new ArrayList<>());
                            plan(State.WS, State.ARR);
                        }
                        default -> throw new RuntimeException(objects.toString());
                    }
                }
                case NUM_DONE -> {
                    states.pop();
                    i--;
                    final Double number = Double.parseDouble(stringBuilder.toString());
                    stringBuilder.setLength(0);
                    objects.push(number);
                }
                case UINT -> {
                    states.pop();
                    switch (c) {
                        case '0' -> stringBuilder.append(c);
                        case '1', '2', '3', '4', '5', '6', '7', '8', '9'  -> {
                            stringBuilder.append(c);
                            plan(State.DIGITS);
                        }
                    }
                }
                case INT -> {
                    states.pop();
                    plan(State.UINT);
                    switch (c) {
                        case '-' -> stringBuilder.append(c);
                        default -> i--;
                    }
                }
                case FRAC -> {
                    states.pop();
                    switch (c) {
                        case '.' -> {
                            stringBuilder.append(c);
                            plan(State.DIGITS);
                        }
                        default -> i--;
                    }
                }
                case DIGITS -> {
                    switch (c) {
                        case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' ->
                                stringBuilder.append(c);
                        default -> {
                            i--;
                            states.pop();
                        }
                    }
                }
                case SIGN -> {
                    states.pop();
                    switch (c) {
                        case '-', '+' -> stringBuilder.append(c);
                        default -> i--;
                    }
                }
                case EXP -> {
                    states.pop();
                    switch (c) {
                        case 'e', 'E' -> plan(State.SIGN, State.DIGITS);
                        default -> i--;
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
                case N -> {
                    states.pop();
                    switch (c) {
                        case 'n' -> stringBuilder.append(c);
                        default -> throw new RuntimeException();
                    }
                }
                case U -> {
                    states.pop();
                    switch (c) {
                        case 'u' -> stringBuilder.append(c);
                        default -> throw new RuntimeException();
                    }
                }
                case L -> {
                    states.pop();
                    switch (c) {
                        case 'l' -> stringBuilder.append(c);
                        default -> throw new RuntimeException();
                    }
                }
                case R -> {
                    states.pop();
                    switch (c) {
                        case 'r' -> stringBuilder.append(c);
                        default -> throw new RuntimeException();
                    }
                }
                case E -> {
                    states.pop();
                    switch (c) {
                        case 'e' -> stringBuilder.append(c);
                        default -> throw new RuntimeException();
                    }
                }
                case F -> {
                    states.pop();
                    switch (c) {
                        case 'f' -> stringBuilder.append(c);
                        default -> throw new RuntimeException();
                    }
                }
                case A -> {
                    states.pop();
                    switch (c) {
                        case 'a' -> stringBuilder.append(c);
                        default -> throw new RuntimeException();
                    }
                }
                case S -> {
                    states.pop();
                    switch (c) {
                        case 's' -> stringBuilder.append(c);
                        default -> throw new RuntimeException();
                    }
                }
                case T -> {
                    states.pop();
                    switch (c) {
                        case 't' -> stringBuilder.append(c);
                        default -> throw new RuntimeException();
                    }
                }
                case NULL -> {
                    i--;
                    stringBuilder.setLength(0);
                    states.pop();
                    objects.push(null);
                }
                case TRUE -> {
                    i--;
                    stringBuilder.setLength(0);
                    states.pop();
                    objects.push(true);
                }
                case FALSE -> {
                    i--;
                    stringBuilder.setLength(0);
                    states.pop();
                    objects.push(false);
                }
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

    public Object parse() throws IOException {
        while (!eof) {
            fillBuffer();
            parseBuf();
        }
        if (objects.empty()) {
            return null;
        } else {
            return objects.peek();
        }
    }

    public static void main(String[] args) throws IOException {
//        final Parser p = new Parser(new StringReader("[ \"abc\" , \"xyz\" , [\"ccc\" , \"aaa\" ] ]"));
//        final Parser p = new Parser(new StringReader("  \"abc\u015Cde\"  "));
//        final Parser p = new Parser(new StringReader("[ 1, 2, 3, 3, 4, 1.3, {\"foo\" : 100} , 2 ] "));
//        final Parser p = new Parser(new StringReader(" [null,1,null,-2.33333,{\"aa\":null, \"bb\":[1,null,2]}]"));
//        final Parser p = new Parser(new StringReader(" [ 1, true, 2 , false, null, 3] "));
//        final Parser p = new Parser(new StringReader("[ \"abc\" , \"xyz\", [ \"a\",  [ \"a\", \"a\", \"a\"  ], \"a\", \"a\"  ], \"1\" , \"2\", \"3\" ]"));


        final Parser p = new Parser(new StringReader("  [ true , false, [ true, false ], \"abc\" ] "));


        System.out.println(p.readAny());
    }


}
