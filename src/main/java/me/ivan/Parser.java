package me.ivan;

import java.io.*;
import java.nio.CharBuffer;
import java.util.*;

public class Parser {

    private Reader reader;
    private final char[] buf;
    private CharBuffer uXXXX;
    private int i;
    private StringBuilder sb;
//    private CharBuffer cbuf;
    private int cbufLen = 0xFF;
    private char[] cbuf;
    private int pos;
    private final Map<Integer, String> cache;
    private boolean overflow;
    private final Map<String, Number> numCache;

    private final int LEN;

    private static int getHash(final char[] buf, final int off, final int len) {
        int result = 1;
        for (int i = off; i < len; i++) {
            result = 31 * result + buf[i];
        }
        return result;
    }

    public Parser(final String content) {
        this.uXXXX = CharBuffer.allocate(4);
        this.buf = content.toCharArray();
        this.LEN = buf.length;
        numCache = new HashMap<>();
        overflow = false;
        cache = new HashMap<>();
        this.i = 0;
    }

    public Parser(final File file, final int len) throws IOException {
        this.uXXXX = CharBuffer.allocate(4);
        this.LEN = len;
        numCache = new HashMap<>();
        cache = new HashMap<>();
        this.buf = new char[len];
//        this.reader = Files.newBufferedReader(file.toPath());
        this.reader = new FileReader(file);
        this.i = 0;
        this.sb = new StringBuilder(0xFFF);
        this.cbuf = new char[cbufLen];
//        this.cbuf = CharBuffer.allocate(0xFF);
    }
    
    private void reset() {
        pos = 0;
//        cbuf.clear();
        sb.setLength(0);
    }
    
    private void append(final char c) {
//        cbuf.append(c);
        if (pos < cbufLen) {
            cbuf[pos++] = c;

//            cbuf.append(c);
        } else {
            sb.append(c);
        }

    }

    private String getCollected() {

//        return "42";

//        final char[] buf = cbuf.array();
//        final int off = 0;
//        final int len = cbuf.position();
//        final int hash = getHash(buf, off, len);
//            String s = cache.get(hash);
//            if {
//                final String result = "test"; // new String(buf, off, len);
//                cache.put(hash, result);
//                return result;
//            }

        if (sb.isEmpty()) {
//            return new String(cbuf);
            final int hash = getHash(cbuf, 0, pos);
            if (cache.containsKey(hash)) {
                return cache.get(hash);
            } else {
                final String result = new String(cbuf, 0, pos);
                cache.put(hash, result);
                return result;
            }
        } else
            return new String(cbuf) + sb;
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
        char c = read();
        if (c == '-') {
            append(c);
        } else {
            unread();
        }
        c = read();
        if (c == '0') {
            append(c);
        } else if (isOneNine(c)) {
            append(c);
            while (true) {
                c = read();
                if (isZeroNine(c)) {
                    append(c);
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
            append('.');
            readOneAndMoreDigits();
        } else {
            unread();
        }
    }

    private void readExponent() {
        char c = read();
        if (c == 'e' || c == 'E') {
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
        final String string = getCollected();
        Number n = numCache.get(string);
        if (n == null) {
            n = Double.parseDouble(string);
            numCache.put(string, n);
            return n;
        } else {
            return n;
        }
//        final Double num = Double.parseDouble(string);
//        return Integer.parseInt("23423423");
//        return 42;
//        sb.setLength(0);
//        return num;
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
        final String string;
        char c;
        c = read();
        if (c != '"') {
            throw new RuntimeException();
        }
        while (true) {
            c = read();
            if (c == '"') {
                return getCollected();
//                string = sb.toString();
//                sb.setLength(0);
//                return string;
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
                        final char xxxx = (char) HexFormat.fromHexDigits(uXXXX);
                        append(xxxx);
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
