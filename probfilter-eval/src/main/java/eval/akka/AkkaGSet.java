package eval.akka;

import akka.cluster.ddata.GSet;
import probfilter.crdt.immutable.CvFilter;


public final class AkkaGSet<E> implements CvFilter<E, AkkaGSet<E>> {
    private final GSet<E> set;

    public AkkaGSet() {
        this.set = GSet.<E>create();
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
        var newSet = (GSet<E>) set.add(elem).resetDelta().clearAncestor();
        return new AkkaGSet<E>(newSet);
    }

    @Override
    public AkkaGSet<E> merge(AkkaGSet<E> that) {
        var newSet = (GSet<E>) this.set.merge(that.set).resetDelta().clearAncestor();
        return new AkkaGSet<E>(newSet);
    }

    public GSet<E> getUnderlying() {
        return set;
    }
}
