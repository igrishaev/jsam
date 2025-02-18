package me.ivan;

import clojure.lang.IFn;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import static me.ivan.ParseError.error;

public class JsonWriter implements AutoCloseable {

    private final Writer writer;
    private final IFn fnProtocol;

    private JsonWriter(final Writer writer, final IFn fnProtocol) {
        this.writer = writer;
        this.fnProtocol = fnProtocol;
    }

    @SuppressWarnings("unused")
    public static JsonWriter create(final Writer writer, final IFn fnProtocol) {
        return new JsonWriter(writer, fnProtocol);
    }

    @SuppressWarnings("unused")
    public void write(final Object value) throws IOException {
        fnProtocol.invoke(value, this);
//        if (value == null) {
//            writeNull(null);
//        } else if (value instanceof Boolean b) {
//            writeBoolean(b);
//        } else if (value instanceof String s) {
//            writeString(s);
//        } else if (value instanceof Number n) {
//            writeNumber(n);
//        } else if (value instanceof List<?> l) {
//            writeArray(l);
//        } else if (value instanceof Map<?,?> m) {
//            writeMap(m);
//        } else {
//            throw error("unsupported value: %s", value);
//        }
    }

    public void writeNull(final Object ignored) throws IOException {
        writer.write("null");
    }

    public void writeBoolean(final Boolean value) throws IOException {
        if (value) {
            writer.write("true");
        } else {
            writer.write("false");
        }
    }

    public void writeNumber(final Number value) throws IOException {
        writer.write(value.toString());
    }

    public void writeMap(final Map<?, ?> map) throws IOException {
        final int len = map.size();
        if (len == 0) {
            writer.write("{}");
            return;
        }
        int i = 0;
        writer.write('{');
        for (Map.Entry<?, ?> kv: map.entrySet()) {
            write(kv.getKey());
            writer.write(':');
            write(kv.getValue());
            i++;
            if (i < len) {
                writer.write(',');
            }
        }
        writer.write('}');
    }

    public void writeArray(final List<?> list) throws IOException {
        final int len = list.size();
        int i = 0;
        if (len == 0) {
            writer.write("[]");
            return;
        }
        writer.write("[");
        for (Object el: list) {
            write(el);
            i++;
            if (i < len) {
                writer.write(',');
            }
        }
        writer.write(']');
    }

    public void writeString(final String value) throws IOException {
        final int len = value.length();
        char c;
        if (len == 0) {
            writer.write("\"\"");
            return;
        }
        writer.write('"');
        for (int i = 0; i < len; i++) {
            c = value.charAt(i);
            switch (c) {
                case '\r' -> writer.write("\\r");
                case '\n' -> writer.write("\\n");
                case '\b' -> writer.write("\\b");
                case '\f' -> writer.write("\\f");
                case '\t' -> writer.write("\\t");
                case '\\' -> writer.write("\\\\");
                case '/' -> writer.write("\\/");
                case '"' -> writer.write("\\\"");
                default -> writer.write(c);
            }
        }
        writer.write('"');
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    public static void main(final String... args) throws IOException {
        StringWriter writer = new StringWriter();
        create(writer, null).write(List.of(1, 2, 3, Map.of("test", 1, "hello", List.of(1,2,3)),  true, List.of(-1.33, "sdf\tsdf", false), false));
        System.out.println(writer);
    }
}
