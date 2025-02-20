package org.jsam;

import java.nio.charset.Charset;
import java.util.function.Supplier;

public record Config(int readBufSize,
                     int tempBufScaleFactor,
                     int tempBufSize,
                     Charset parserCharset,
                     Charset writerCharset,
                     Supplier<IArrayBuilder> arrayBuilderSupplier,
                     Supplier<IObjectBuilder> objectBuilderSupplier,
                     boolean isPretty,
                     int prettyIndent
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
        private Supplier<IArrayBuilder> arrayBuilderSupplier = Const.arrayBuilderSupplier;
        private Supplier<IObjectBuilder> objectBuilderSupplier = Const.objectBuilderSupplier;
        private boolean isPretty = Const.isPretty;
        private int prettyIndent = Const.prettyIndent;

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
        public Builder arrayBuilderSupplier(final Supplier<IArrayBuilder> arrayBuilderSupplier) {
            this.arrayBuilderSupplier = arrayBuilderSupplier;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder objectBuilderSupplier(final Supplier<IObjectBuilder> objectBuilderSupplier) {
            this.objectBuilderSupplier = objectBuilderSupplier;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder isPretty(final boolean isPretty) {
            this.isPretty = isPretty;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder prettyIndent(final int prettyIndent) {
            this.prettyIndent = prettyIndent;
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
                    arrayBuilderSupplier,
                    objectBuilderSupplier,
                    isPretty,
                    prettyIndent
            );
        }
    }
}
