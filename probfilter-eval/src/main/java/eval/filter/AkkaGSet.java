package eval.filter;

import akka.cluster.ddata.GSet;
import com.c6h5no2.probfilter.crdt.CvRFilter;
import com.c6h5no2.probfilter.util.Immutable;


public final class AkkaGSet<E> implements CvRFilter<E, AkkaGSet<E>>, Immutable {
    private final GSet<E> state;

    public AkkaGSet() {
        this.state = GSet.<E>create();
    }

    private AkkaGSet(GSet<E> state) {
        this.state = state;
    }

    @Override
    public int size() {
        return state.size();
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
        return state.contains(elem);
    }

    @Override
    public AkkaGSet<E> add(E elem) {
        var newState = (GSet<E>) state.add(elem).resetDelta().clearAncestor();
        return copy(newState);
    }

    @Override
    public AkkaGSet<E> merge(AkkaGSet<E> that) {
        var newState = (GSet<E>) this.state.merge(that.state).resetDelta().clearAncestor();
        return copy(newState);
    }

    public GSet<E> getAkkaSet() {
        return state;
    }

    private AkkaGSet<E> copy(GSet<E> state) {
        return new AkkaGSet<>(state);
    }
}
