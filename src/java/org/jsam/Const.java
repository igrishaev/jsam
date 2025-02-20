package org.jsam;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class Const {
    public static final int readBufSize = 8196;
    public static final int tempBufScaleFactor = 2;
    public static final int tempBufSize = 0xff;
    public static final Charset parserCharset = StandardCharsets.UTF_8;
    public static final Supplier<IArrayBuilder> arrayBuilderSupplier = Suppliers.ARR_CLJ_VECTOR;
    public static final Supplier<IObjectBuilder> objectBuilderSupplier = Suppliers.OBJ_CLJ_MAP;
    public static boolean isPretty = false;
    public static int prettyIndent = 2;
}
