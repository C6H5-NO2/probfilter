package com.c6h5no2.probfilter.pdsa.cuckoo;

import scala.reflect.ClassTag;


/**
 * The semantic types of cuckoo entries.
 */
public enum CuckooEntryType {
    /**
     * Entry of max 8-bit fingerprint
     */
    SIMPLE_BYTE {
        @Override
        public ClassTag<?> storageType() {
            return ClassTag.Byte();
        }

        @Override
        public short extractFp(Object entry) {
            // unsigned extension
            return (short) (((Byte) entry) & 0xff);
        }

        @Override
        public boolean matchFp(Object entry, short fp) {
            return (Byte) entry == (byte) fp;
        }
    },

    /**
     * Entry of max 16-bit fingerprint
     */
    SIMPLE_SHORT {
        @Override
        public ClassTag<?> storageType() {
            return ClassTag.Short();
        }

        @Override
        public short extractFp(Object entry) {
            return (Short) entry;
        }
    },

    /**
     * Entry of {@link IntVersionedEntry}
     */
    VERSIONED_INT {
        @Override
        public ClassTag<?> storageType() {
            return ClassTag.Int();
        }

        @Override
        public short extractFp(Object entry) {
            return IntVersionedEntry.extract((Integer) entry);
        }
    },

    /**
     * Entry of {@link LongVersionedEntry}
     */
    VERSIONED_LONG {
        @Override
        public ClassTag<?> storageType() {
            return ClassTag.Long();
        }

        @Override
        public short extractFp(Object entry) {
            return LongVersionedEntry.extract((Long) entry);
        }
    };

    /**
     * @return a {@link scala.reflect.ClassTag} of the primitive type in which entries are stored
     */
    public abstract ClassTag<?> storageType();

    /**
     * Extracts the fingerprint from {@code entry}.
     */
    public abstract short extractFp(Object entry);

    /**
     * {@link CuckooEntryType#extractFp(Object)} and limits the length to {@code numBits} bits.
     */
    public final short extractFp(Object entry, int numBits) {
        return (short) (extractFp(entry) & ((1 << numBits) - 1));
    }

    /**
     * @return {@code true} if the fingerprint in {@code entry} is equal to {@code fp}; {@code false} otherwise
     */
    public boolean matchFp(Object entry, short fp) {
        return extractFp(entry) == fp;
    }
}
