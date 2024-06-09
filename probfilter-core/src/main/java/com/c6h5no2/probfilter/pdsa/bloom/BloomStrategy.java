package probfilter.pdsa.bloom;

import probfilter.pdsa.FilterHashStrategy;
import probfilter.util.JavaFriendly;

import java.util.PrimitiveIterator;


/**
 * @see probfilter.pdsa.bloom.SimpleBloomStrategy
 */
public interface BloomStrategy<E> extends FilterHashStrategy {
    @Override
    default double fpp() {
        return Math.pow(-Math.expm1(-1.0 * capacity() / numBits() * numHashes()), numHashes());
    }

    @Override
    BloomStrategy<E> tighten();

    int numBits();

    int numHashes();

    /**
     * @return an iterator over the hash values of `elem`
     */
    scala.collection.Iterator<scala.Int> hashIterator(E elem);

    @JavaFriendly(scalaDelegate = "probfilter.pdsa.bloom.BloomStrategy#hashIterator(E)")
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
