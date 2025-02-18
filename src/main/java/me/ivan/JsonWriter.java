package me.ivan;

import clojure.lang.IFn;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
    public void write(final Object value) {
        fnProtocol.invoke(value, this);
    }

    @SuppressWarnings("unused")
    public void writeNull(final Object ignored) throws IOException {
        writer.write("null");
    }

    @SuppressWarnings("unused")
    public void writeBoolean(final Boolean value) throws IOException {
        if (value) {
            writer.write("true");
        } else {
            writer.write("false");
        }
    }

    @SuppressWarnings("unused")
    public void writeNumber(final Number value) throws IOException {
        writer.write(value.toString());
    }

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
    public void writeArray(final Iterable<?> iterable) throws IOException {
        final Iterator<?> iter = iterable.iterator();
        writer.write("[");
        while (iter.hasNext()) {
            write(iter.next());
            if (iter.hasNext()) {
                writer.write(',');
            }
        }
        writer.write(']');
    }

    @SuppressWarnings("unused")
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

    public static void main(final String... args) {
        StringWriter writer = new StringWriter();
        create(writer, null).write(List.of(1, 2, 3, Map.of("test", 1, "hello", List.of(1,2,3)),  true, List.of(-1.33, "sdf\tsdf", false), false));
        System.out.println(writer);
    }
}
