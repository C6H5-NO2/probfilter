package com.c6h5no2.probfilter.pdsa.bloom;

import com.c6h5no2.probfilter.pdsa.HashStrategy;

import java.util.PrimitiveIterator;


public interface BloomStrategy<E> extends HashStrategy {
    @Override
    default double fpp() {
        return Math.pow(-Math.expm1(-1.0 * capacity() / numBits() * numHashes()), numHashes());
    }

    @Override
    BloomStrategy<E> tighten();

    int numBits();

    int numHashes();

    /**
     * @return an iterator over the hash values of {@code elem}
     */
    scala.collection.Iterator<scala.Int> hashIterator(E elem);

    default PrimitiveIterator.OfInt hashIteratorAsJava(E elem) {
        var iter = hashIterator(elem);
        return new PrimitiveIterator.OfInt() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public int nextInt() {
                return iter.next().toInt();
            }
        };
    }
}
