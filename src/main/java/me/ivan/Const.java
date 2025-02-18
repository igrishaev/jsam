package me.ivan;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class Const {
    public static final int readBufSize = 8196;
    public static final int tempBufScaleFactor = 2;
    public static final int tempBufSize = 0xff;
    public static final Charset parserCharset = StandardCharsets.UTF_8;
    public static final Supplier<IArrayBuilder> arrayBuilderFactory = ArrayListBuilder::new;
    public static final Supplier<IObjectBuilder> objectBuilderFactory = HashMapBuilder::new;
}
