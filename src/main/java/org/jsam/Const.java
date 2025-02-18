package org.jsam;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

public class Const {
    public static final int readBufSize = 8196;
    public static final int tempBufScaleFactor = 2;
    public static final int tempBufSize = 0xff;
    public static final Charset parserCharset = StandardCharsets.UTF_8;
    public static final Callable<IArrayBuilder> arrayBuilderFactory = ArrayListBuilder::new;
    public static final Callable<IObjectBuilder> objectBuilderFactory = HashMapBuilder::new;
}
