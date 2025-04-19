package org.jsam;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.CharBuffer;
import java.util.*;
import static org.jsam.ParseError.error;

public class Parser {

    private int numIntSize = 0;
    private boolean numHasFrac = false;
    private boolean numHasExp = false;

    private final Reader reader;

    private final char[] readBuf;
    private int readLen = 8192;
    private int readOff = 0;
    private int readPos = 0;

    private final int scaleFactor = 2;

    private final CharBuffer uXXXX = CharBuffer.allocate(4);

    private char[] tempBuf;
    private int tempLen = 255;
    private int tempPos = 0;

    private void scaleBuffer() {
        tempLen = tempLen * scaleFactor;
        char[] newBuf = new char[tempLen];
        System.arraycopy(tempBuf, 0, newBuf, 0, tempBuf.length);
        this.tempBuf = newBuf;
    }

    public Parser(final String content) {
        this.readBuf = content.toCharArray();
        this.readLen = readBuf.length;
        this.readOff = readBuf.length;
        this.reader = Reader.nullReader();
        this.tempBuf = new char[tempLen];
    }

    public Parser(final File file) throws IOException {
        this.readBuf = new char[readLen];
        this.tempBuf = new char[tempLen];
        this.reader = new FileReader(file);
    }

    private void resetTempBuf() {
        tempPos = 0;
    }

    private void append(final char c) {
        if (tempPos >= tempLen) {
            scaleBuffer();
        }
        tempBuf[tempPos++] = c;
    }

    private String getCollectedString() {
        return new String(tempBuf, 0, tempPos);
    }

    private void readMore() {
        try {
            final int r = reader.read(readBuf);
            if (r == -1) {
                throw new EOFException();
            }
            readOff = r;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private char read() {
        if (readPos == readOff) {
            readMore();
            readPos = 0;
        }
        return readBuf[readPos++];
    }

    private void unread() {
        readPos--;
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
            throw error("reading object: expected '{' but got '%s'", c);
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
                throw error("reading object: expected ':' after a key but got '%s'", c);
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
            throw error("reading object: expected '}' but got '%s'", c);
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
            throw error("reading array: expected '[' but got '%s'", c);
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
            throw error("reading array: expected ']' but got '%s'", c);
        }
        return list;
    }

    @SuppressWarnings("UnusedReturnValue")
    public Object parse() {
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
            default -> throw error("reading any: unexpected character '%s'", c);
        };
    }

    private void readOneAndMoreDigits() {
        char c;
        c = read();
        if (isZeroNine(c)) {
            append(c);
        } else {
            throw error("reading digits: expected 0-9 but got '%s'", c);
        }
        while (true) {
            c = read();
            if (isZeroNine(c)) {
                append(c);
            } else {
                unread();
                break;
            }
        }
    }

    private void readInteger() {
        numIntSize = 0;
        char c = read();
        if (c == '-') {
            append(c);
        } else {
            unread();
        }
        c = read();
        if (c == '0') {
            append(c);
            numIntSize++;
        } else if (isOneNine(c)) {
            append(c);
            numIntSize++;
            while (true) {
                c = read();
                if (isZeroNine(c)) {
                    append(c);
                    numIntSize++;
                } else {
                    unread();
                    break;
                }
            }
        } else {
            throw error("reading integer: unexpected character '%s'", c);
        }

    }

    private void readFraction() {
        numHasFrac = false;
        char c;
        c = read();
        if (c == '.') {
            append('.');
            numHasFrac = true;
            readOneAndMoreDigits();
        } else {
            unread();
        }
    }

    private void readExponent() {
        numHasExp = false;
        char c = read();
        if (c == 'e' || c == 'E') {
            numHasExp = true;
            append(c);
            c = read();
            if (c == '-' || c == '+') {
                append(c);
                readOneAndMoreDigits();
            } else {
                throw error("reading exponent: expected -/+ but got '%s'", c);
            }
        } else {
            unread();
        }
    }

    private Number readNumber() {
        resetTempBuf();
        readInteger();
        readFraction();
        readExponent();
        final String string = getCollectedString();
        Number n;
        if (!numHasFrac && !numHasExp) {
            if (numIntSize < 5) {
                n = Short.parseShort(string);
            } else if (numIntSize < 9) {
                n = Integer.parseInt(string);
            } else if (numIntSize < 18) {
                n = Long.parseLong(string);
            } else {
                n = new BigInteger(string);
            }
        } else {
            if (numIntSize < 38) {
                n = Float.parseFloat(string);
            } else if (numIntSize < 307) {
                n = Double.parseDouble(string);
            } else {
                n = new BigDecimal(string);
            }
        }
        return n;
    }

    private boolean readTrue() {
        final char c1 = read();
        final char c2 = read();
        final char c3 = read();
        final char c4 = read();
        if (c1 == 't' && c2 == 'r' && c3 == 'u' && c4 == 'e') {
            return true;
        } else {
            throw error("reading true literal: unexpected sequence '%s', '%s', '%s', '%s'", c1, c2, c3, c4);
        }
    }

    private boolean readFalse() {
        final char c1 = read();
        final char c2 = read();
        final char c3 = read();
        final char c4 = read();
        final char c5 = read();
        if (c1 == 'f' && c2 == 'a' && c3 == 'l' && c4 == 's' && c5 == 'e') {
            return false;
        } else {
            throw error("reading false literal: unexpected sequence '%s', '%s', '%s', '%s', '%s'", c1, c2, c3, c4, c5);
        }
    }

    private Object readNull() {
        final char c1 = read();
        final char c2 = read();
        final char c3 = read();
        final char c4 = read();
        if (c1 == 'n' && c2 == 'u' && c3 == 'l' && c4 == 'l') {
            return null;
        } else {
            throw error("reading null literal: unexpected sequence '%s', '%s', '%s', '%s'", c1, c2, c3, c4);
        }
    }

    private String readString() {
        resetTempBuf();
        char c;
        c = read();
        if (c != '"') {
            throw error("reading string: expected '\"' but got '%s'", c);
        }
        while (true) {
            c = read();
            if (c == '"') {
                return getCollectedString();
            } else if (c == '\\') {
                c = read();
                switch (c) {
                    case 'r' -> append('\r');
                    case 'n' -> append('\n');
                    case 'b' -> append('\b');
                    case 'f' -> append('\f');
                    case 't' -> append('\t');
                    case '\\' -> append('\\');
                    case '/' -> append('/');
                    case '"' -> append('"');
                    case 'u' -> {
                        uXXXX.append(read());
                        uXXXX.append(read());
                        uXXXX.append(read());
                        uXXXX.append(read());
                        uXXXX.rewind();
                        final char cXXXX = (char) HexFormat.fromHexDigits(uXXXX);
                        append(cXXXX);
                    }
                    default -> throw error("reading escaped: unknown character '%s'", c);

                }
            } else {
                append(c);
            }
        }
    }

    private static boolean isZeroNine(final char c) {
        return '0' <= c && c <= '9';
    }

    private static boolean isOneNine(final char c) {
        return '1' <= c && c <= '9';
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
        final Parser p = new Parser(new File("100mb.json"));
        final long t1 = System.currentTimeMillis();
        p.parse();
        final long t2 = System.currentTimeMillis();
        System.out.println(t2 - t1);
//        final StringBuilder sb = new StringBuilder(4);
//        System.out.println(sb.hashCode());




//        System.out.println();
    }


}
