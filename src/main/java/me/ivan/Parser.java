package me.ivan;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Parser {

    private final char[] buf;
    private char[] uXXXX;
    private int i;

    public Parser(final String content) {
        this.uXXXX = new char[4];
        this.buf = content.toCharArray();
        this.i = 0;
    }

    public Parser(final Reader reader) {
        this.uXXXX = new char[4];
        this.buf = new char[2048];
        this.i = 0;
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

    private char read() {
        return buf[i++];
    }

    private void unread() {
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
        }
        return list;
    }

    public Object parse() {
        return readAny();
    }

    private Object readAny() {
        ws();
        char c = read();
        unread();
        while (true) {
            if (c == 't') {
                return readTrue();
            } else if (c == 'f') {
                return readFalse();
            } else if (c == '[') {
                return readArray();
            } else if (c == '"') {
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

    public static void main(String[] args) throws IOException {
//        final Parser p = new Parser(new StringReader("[ \"abc\" , \"xyz\" , [\"ccc\" , \"aaa\" ] ]"));
//        final Parser p = new Parser(new StringReader("  \"abc\u015Cde\"  "));
//        final Parser p = new Parser(new StringReader("[ 1, 2, 3, 3, 4, 1.3, {\"foo\" : 100} , 2 ] "));
//        final Parser p = new Parser(new StringReader(" [null,1,null,-2.33333,{\"aa\":null, \"bb\":[1,null,2]}]"));
//        final Parser p = new Parser(new StringReader(" [ 1, true, 2 , false, null, 3] "));
//        final Parser p = new Parser(new StringReader("[ \"abc\" , \"xyz\", [ \"a\",  [ \"a\", \"a\", \"a\"  ], \"a\", \"a\"  ], \"1\" , \"2\", \"3\" ]"));


//        final Parser p = new Parser(new StringReader("  [ true , false, [ true, false ], \"abc\" ] "));
        final Parser p = new Parser("  [ true , false, [ true, false ], \"abc\" ] ");

        System.out.println(p.parse());
    }


}
