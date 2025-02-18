package org.jsam;

import java.nio.charset.Charset;
import java.util.concurrent.Callable;

public record Config(int readBufSize,
                     int tempBufScaleFactor,
                     int tempBufSize,
                     Charset parserCharset,
                     Charset writerCharset,
                     Callable<IArrayBuilder> arrayBuilderFactory,
                     Callable<IObjectBuilder> objectBuilderFactory,
                     boolean isPretty
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
        private Charset writerCharset = Const.parserCharset;
        private Callable<IArrayBuilder> arrayBuilderFactory = Const.arrayBuilderFactory;
        private Callable<IObjectBuilder> objectBuilderFactory = Const.objectBuilderFactory;
        private boolean isPretty = Const.isPretty;

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
        public Builder writerCharset(final Charset writerCharset) {
            this.writerCharset = writerCharset;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder arrayBuilderFactory(final Callable<IArrayBuilder> arrayBuilderFactory) {
            this.arrayBuilderFactory = arrayBuilderFactory;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder objectBuilderFactory(final Callable<IObjectBuilder> objectBuilderFactory) {
            this.objectBuilderFactory = objectBuilderFactory;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder isPretty(final boolean isPretty) {
            this.isPretty = isPretty;
            return this;
        }

        @SuppressWarnings("unused")
        public Config build() {
            return new Config(
                    readBufSize,
                    tempBufScaleFactor,
                    tempBufSize,
                    parserCharset,
                    writerCharset,
                    arrayBuilderFactory,
                    objectBuilderFactory,
                    isPretty
            );
        }
    }
}
