package me.ivan;

import java.nio.charset.Charset;

import static me.ivan.Const.parserCharset;

public record Config(int readBufSize,
                     int tempBufScaleFactor,
                     int tempBufSize,
                     Charset parserCharset
) {


    @SuppressWarnings("unused")
    public static Config DEFAULTS = builder().build();

    @SuppressWarnings("unused")
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private int readBufSize = Const.readBufSize;
        private int tempBufScaleFactor = Const.tempBufScaleFactor;
        private int tempBufSize = Const.tempBufSize;
        private Charset parserCharset = Const.parserCharset;

        @SuppressWarnings("unused")
        public Builder readBufSize(final int readBufSize) {
            this.readBufSize = readBufSize;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder tempBufScaleFactor(final int tempBufScaleFactor) {
            this.tempBufScaleFactor = tempBufScaleFactor;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder tempBufSize(final int tempBufSize) {
            this.tempBufSize = tempBufSize;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder parserCharset(final Charset parserCharset) {
            this.parserCharset = parserCharset;
            return this;
        }

        @SuppressWarnings("unused")
        public Config build() {
            return new Config(readBufSize, tempBufScaleFactor, tempBufSize, parserCharset);
        }
    }
}
