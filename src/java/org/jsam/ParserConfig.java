package org.jsam;

public record ParserConfig(
        int readLen,
        int scaleFactor,
        int tempLen
) {

    public static ParserConfig DEFAULT = builder().build();

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private int readLen = Const.readLen;
        private int scaleFactor = Const.scaleFactor;
        private int tempLen = Const.tempLen;

        @SuppressWarnings("unused")
        public Builder readLen(final int readLen) {
            this.readLen = readLen;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder scaleFactor(final int scaleFactor) {
            this.scaleFactor = scaleFactor;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder tempLen(final int tempLen) {
            this.tempLen = tempLen;
            return this;
        }

        public ParserConfig build() {
            return new ParserConfig(
                    readLen,
                    scaleFactor,
                    tempLen
            );
        }
    }
}
