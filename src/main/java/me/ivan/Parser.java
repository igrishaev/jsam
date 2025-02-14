package me.ivan;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.CharBuffer;
import java.util.*;

public class Parser {

    private int numIntSize = 0;
    private boolean numHasFrac = false;
    private boolean numHasExp = false;
    private Reader reader;
    private int bufPos;
    private final char[] buf;
    private final CharBuffer uXXXX;
    private int i;
    private int cbufLen = 0xFF;
    private char[] cbuf;
    private int pos;

    private void scaleBuffer() {
        final int newLen = cbufLen * 2;
        char[] newBuf = new char[newLen];
        System.arraycopy(cbuf, 0, newBuf, 0, cbufLen);
        this.cbuf = newBuf;
        this.cbufLen = newLen;
    }

    public Parser(final String content) {
//        this.hash = 1;
        this.uXXXX = CharBuffer.allocate(4);
        this.buf = content.toCharArray();
        this.i = 0;
    }

    public Parser(final File file, final int len) throws IOException {
        this.bufPos = 0;
        this.uXXXX = CharBuffer.allocate(4);
        this.buf = new char[len];
        this.reader = new FileReader(file);
        this.i = 0;
        this.cbuf = new char[cbufLen];
    }

    private void reset() {
//        hash = 1;
        pos = 0;
    }

    private void append(final char c) {
        if (pos >= cbufLen) {
            scaleBuffer();
        }
        cbuf[pos++] = c;
    }

    private String getCollectedString() {
        return new String(cbuf, 0, pos);
    }

    private void readMore() {
        try {
            final int r = reader.read(buf);
            if (r == -1) {
                throw new RuntimeException("EOF");
            }
            bufPos = r;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private char read() {
//        if (i == bufPos) {
//            readMore();
//            i = 0;
//        }
        try {
            return buf[i++];
        } catch (ArrayIndexOutOfBoundsException e) {
            readMore();
            i = 0;
        }
        return buf[i++];
//        return buf[i++];
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

    @SuppressWarnings("UnusedReturnValue")
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
            append(c);
        } else {
            throw new RuntimeException("expected a digit");
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
//            numNegative = true;
            append(c);
        } else {
//            numNegative = false;
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
            throw new RuntimeException("aaa");
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
                throw new RuntimeException("expected sign");
            }
        } else {
            unread();
        }
    }

    private Number readNumber() {
        reset();
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
        reset();
        char c;
        c = read();
        if (c != '"') {
            throw new RuntimeException();
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
                    default -> throw new RuntimeException("dunno " + c);

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
        final Parser p = new Parser(new File("100mb.json"), 4096);
        final long t1 = System.currentTimeMillis();
        p.parse();
        final long t2 = System.currentTimeMillis();
        System.out.println(t2 - t1);
//        final StringBuilder sb = new StringBuilder(4);
//        System.out.println(sb.hashCode());




//        System.out.println();
    }


}
