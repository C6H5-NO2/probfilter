package eval.akka;

import akka.cluster.ddata.GSet;
import com.c6h5no2.probfilter.crdt.CvRFilter;
import com.c6h5no2.probfilter.util.Immutable;


public final class AkkaGSet<E> implements CvRFilter<E, AkkaGSet<E>>, Immutable {
    private final GSet<E> set;

    public AkkaGSet() {
        this.set = GSet.create();
    }

    private AkkaGSet(GSet<E> set) {
        this.set = set;
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public int capacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public double fpp() {
        return 0;
    }

    @Override
    public boolean contains(E elem) {
        return set.contains(elem);
    }

    @Override
    public AkkaGSet<E> add(E elem) {
        var newSet = set.add(elem).resetDelta().clearAncestor();
        return copy(newSet);
    }

    @Override
    public AkkaGSet<E> merge(AkkaGSet<E> that) {
        var newSet = this.set.merge(that.set).resetDelta().clearAncestor();
        return copy(newSet);
    }

    public GSet<E> getAkkaSet() {
        return set;
    }

    private AkkaGSet<E> copy(GSet<E> set) {
        return new AkkaGSet<>(set);
    }
}
