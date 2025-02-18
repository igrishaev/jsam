package me.ivan;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Const {

    public static final int readBufSize = 8196;
    public static final int tempBufScaleFactor = 2;
    public static final int tempBufSize = 0xff;
    public static final Charset parserCharset = StandardCharsets.UTF_8;
}
