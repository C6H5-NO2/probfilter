package eval.filter;

import akka.actor.Address;
import akka.cluster.UniqueAddress;
import akka.cluster.ddata.ORSet;
import akka.cluster.ddata.SelfUniqueAddress;
import com.c6h5no2.probfilter.crdt.CvRFilter;
import com.c6h5no2.probfilter.util.Immutable;


public final class AkkaORSet<E> implements CvRFilter<E, AkkaORSet<E>>, Immutable {
    private final SelfUniqueAddress address;
    private final ORSet<E> state;

    public AkkaORSet(short rid) {
        this.address = new SelfUniqueAddress(new UniqueAddress(new Address("", "", "", 0), (long) rid));
        this.state = ORSet.<E>create();
    }

    private AkkaORSet(SelfUniqueAddress address, ORSet<E> state) {
        this.address = address;
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
    public AkkaORSet<E> add(E elem) {
        var newState = (ORSet<E>) state.add(address, elem).resetDelta().clearAncestor();
        return copy(newState);
    }

    @Override
    public AkkaORSet<E> remove(E elem) {
        var newState = (ORSet<E>) state.remove(address, elem).resetDelta().clearAncestor();
        return copy(newState);
    }

    @Override
    public AkkaORSet<E> merge(AkkaORSet<E> that) {
        var newState = (ORSet<E>) this.state.merge(that.state).resetDelta().clearAncestor();
        return copy(newState);
    }

    public ORSet<E> getAkkaSet() {
        return state;
    }

    private AkkaORSet<E> copy(ORSet<E> state) {
        return new AkkaORSet<>(this.address, state);
    }
}
