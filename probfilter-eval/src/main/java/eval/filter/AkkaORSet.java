package eval.filter;

import akka.actor.Address;
import akka.cluster.UniqueAddress;
import akka.cluster.ddata.ORSet;
import akka.cluster.ddata.SelfUniqueAddress;
import com.c6h5no2.probfilter.crdt.CvRFilter;
import com.c6h5no2.probfilter.util.Immutable;


public final class AkkaORSet<E> implements CvRFilter<E, AkkaORSet<E>>, Immutable {
    private final SelfUniqueAddress address;
    private final ORSet<E> set;

    public AkkaORSet(short rid) {
        this.address = new SelfUniqueAddress(new UniqueAddress(new Address("", "", "", 0), (long) rid));
        this.set = ORSet.create();
    }

    private AkkaORSet(SelfUniqueAddress address, ORSet<E> set) {
        this.address = address;
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
    public AkkaORSet<E> add(E elem) {
        var newSet = set.add(address, elem).resetDelta().clearAncestor();
        return copy(newSet);
    }

    @Override
    public AkkaORSet<E> remove(E elem) {
        var newSet = set.remove(address, elem).resetDelta().clearAncestor();
        return copy(newSet);
    }

    @Override
    public AkkaORSet<E> merge(AkkaORSet<E> that) {
        var newSet = this.set.merge(that.set).resetDelta().clearAncestor();
        return copy(newSet);
    }

    public ORSet<E> getAkkaSet() {
        return set;
    }

    private AkkaORSet<E> copy(ORSet<E> set) {
        return new AkkaORSet<>(this.address, set);
    }
}
