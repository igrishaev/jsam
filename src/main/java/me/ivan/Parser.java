package me.ivan;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parser {

    private Reader reader;
    private final char[] buf;
    private char[] uXXXX;
    private int i;
    private StringBuilder sb;

    private final int LEN;

    public Parser(final String content) {
        this.uXXXX = new char[4];
        this.buf = content.toCharArray();
        this.LEN = buf.length;
        this.i = 0;
    }

    public Parser(final File file, final int len) throws IOException {
        this.uXXXX = new char[4];
        this.LEN = len;
        this.buf = new char[len];
//        this.reader = Files.newBufferedReader(file.toPath());
        this.reader = new FileReader(file);
        this.i = 0;
        this.sb = new StringBuilder();
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

    private void readMore() {
        try {
            reader.read(buf);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private char read() {
        if (i == LEN) {
            readMore();
            i = 0;
        }
        // TODO: override it
        return buf[i++];
    }

//    private char peek() {
////        if (i == LEN) {
////            readMore();
////            i = 0;
////        }
//        return buf[i];
//    }

    private void unread() {
//        reader.
        i--;
    }

    private void ws() {
        char c;
        while (true) {
            c = read();
            if (!(c == ' ' || c == '\r' || c == '\n' || c == '\t')) {
                unread();
                break;
            }
        }
    }

    private Map<String, Object> readObject() {
        char c;
        ws();
        c = read();
        if (c != '{') {
            throw new RuntimeException("not a map");
        }
        final Map<String, Object> map = new HashMap<>();
        boolean repeat = true;
        ws();
        c = read();
        if (c == '}') {
            return map;
        }
        unread();
        while (repeat) {
            ws();
            String key = readString();
            ws();
            c = read();
            if (c != ':') {
                throw new RuntimeException("expected :");
            }
            ws();
            Object val = readAny();
            map.put(key, val);
            ws();
            c = read();
            if (c != ',') {
                unread();
                repeat = false;
            }
        }
        c = read();
        if (c != '}') {
            throw new RuntimeException("expected }");
        }
        return map;
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
        if (c == ']') {
            return list;
        }
        unread();
        while (repeat) {
            el = readAny();
            list.add(el);
            ws();
            c = read();
            if (c != ',') {
                unread();
                repeat = false;
            }
        }
        c = read();
        if (c != ']') {
            throw new RuntimeException();
        }
        return list;
    }

    public Object parse() {
        readMore();
        return readAny();
    }

    private Object readAny() {
        ws();
        char c = read();
        unread();
        return switch (c) {
            case 't' -> readTrue();
            case 'f' -> readFalse();
            case 'n' -> readNull();
            case '"' -> readString();
            case '[' -> readArray();
            case '{' -> readObject();
            case '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> readNumber();
            default -> throw new RuntimeException("unknown");
        };
    }

    private void readOneAndMoreDigits() {
        char c;
        c = read();
        if (isZeroNine(c)) {
            sb.append(c);
        } else {
            throw new RuntimeException("expected a digit");
        }
        while (true) {
            c = read();
            if (isZeroNine(c)) {
                sb.append(c);
            } else {
                unread();
                break;
            }
        }
    }

    private void readInteger() {
        char c = read();
        if (c == '-') {
            sb.append(c);
        } else {
            unread();
        }
        c = read();
        if (c == '0') {
            sb.append(c);
        } else if (isOneNine(c)) {
            sb.append(c);
            while (true) {
                c = read();
                if (isZeroNine(c)) {
                    sb.append(c);
                } else {
                    unread();
                    break;
                }
            }
        } else {
            throw new RuntimeException("aaa");
        }

    }

    private void readFraction() {
        char c;
        c = read();
        if (c == '.') {
            sb.append('.');
            readOneAndMoreDigits();
        } else {
            unread();
        }
    }

    private void readExponent() {
        char c = read();
        if (c == 'e' || c == 'E') {
            sb.append(c);
            c = read();
            if (c == '-' || c == '+') {
                sb.append(c);
                readOneAndMoreDigits();
            } else {
                throw new RuntimeException("expected sign");
            }
        } else {
            unread();
        }
    }

    private Number readNumber() {
        readInteger();
        readFraction();
        readExponent();
        final Double num = Double.parseDouble(sb.toString());
        sb.setLength(0);
        return num;
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

    private Object readNull() {
        if (read() == 'n' && read() == 'u' && read() == 'l' && read() == 'l') {
            return null;
        } else {
            throw new RuntimeException();
        }
    }

    private String readString() {
        final String string;
        char c;
        c = read();
        if (c != '"') {
            throw new RuntimeException();
        }
        while (true) {
            c = read();
            if (c == '"') {
                string = sb.toString();
                sb.setLength(0);
                return string;
            } else if (c == '\\') {
                c = read();
                switch (c) {
                    case 'r' -> sb.append('\r');
                    case 'n' -> sb.append('\n');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 't' -> sb.append('\t');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case '"' -> sb.append('"');
                    default -> throw new RuntimeException("dunno " + c);

                }
            } else {
                sb.append(c);
            }

        }
    }

    private static boolean isZeroNine(final char c) {
        return '0' <= c && c <= '9';
    }

    private static boolean isOneNine(final char c) {
        return '1' <= c && c <= '9';
    }

    private static boolean isHex(final char c) {
        return ('0' <= c && c <= '9') || ('a' <= c && c <= 'f') || ('A' <= c && c <= 'F');
    }

    private char getUUUUChar() {
        return (char) (
                ((charToInt(uXXXX[0]) & 0xFF) << 24) |
                ((charToInt(uXXXX[1]) & 0xFF) << 16) |
                ((charToInt(uXXXX[2]) & 0xFF) <<  8) |
                ((charToInt(uXXXX[3]) & 0xFF))
        );
    }

    public static void main(String[] args) throws IOException {
//        final Parser p = new Parser(new StringReader("[ \"abc\" , \"xyz\" , [\"ccc\" , \"aaa\" ] ]"));
//        final Parser p = new Parser(new StringReader("  \"abc\u015Cde\"  "));
//        final Parser p = new Parser(new StringReader("[ 1, 2, 3, 3, 4, 1.3, {\"foo\" : 100} , 2 ] "));
//        final Parser p = new Parser(new StringReader(" [null,1,null,-2.33333,{\"aa\":null, \"bb\":[1,null,2]}]"));
//        final Parser p = new Parser(new StringReader(" [ 1, true, 2 , false, null, 3] "));
//        final Parser p = new Parser(new StringReader("[ \"abc\" , \"xyz\", [ \"a\",  [ \"a\", \"a\", \"a\"  ], \"a\", \"a\"  ], \"1\" , \"2\", \"3\" ]"));


//        final Parser p = new Parser(new StringReader("  [ true , false, [ true, false ], \"abc\" ] "));
//        final Parser p = new Parser("  [ true , false, [ true, false ], \"abc\" ] ");
        final Parser p = new Parser(new File("data.json"), 4096);

        System.out.println(p.parse());
    }


}
