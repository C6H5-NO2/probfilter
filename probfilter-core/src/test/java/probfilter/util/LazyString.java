package probfilter.util;


public final class LazyString {
    private LazyString() {}

    /**
     * @return a thread-<b>un</b>safe {@code Object} whose {@code toString} returns the lazily formatted string
     * @see java.lang.String#format(String, Object...)
     */
    public static Object format(String format, Object... args) {
        return new LazyFormatter(format, args);
    }

    private static final class LazyFormatter {
        private final String format;
        private final Object[] args;

        private String formatted;
        private volatile boolean bitmap;

        private LazyFormatter(String format, Object... args) {
            this.format = format;
            this.args = args;
        }

        private /* synchronized */ String lazyToString() {
            if (!this.bitmap) {
                this.formatted = String.format(this.format, this.args);
                this.bitmap = true;
            }
            return this.formatted;
        }

        @Override
        public String toString() {
            return !this.bitmap ? this.lazyToString() : this.formatted;
        }
    }
}
