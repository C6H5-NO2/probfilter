package eval.akka;

import akka.actor.Address;
import akka.cluster.UniqueAddress;
import akka.cluster.ddata.ORSet;
import akka.cluster.ddata.SelfUniqueAddress;
import probfilter.crdt.immutable.ImmCvFilter;


public final class AkkaORSet<E> implements ImmCvFilter<E, AkkaORSet<E>> {
    private final SelfUniqueAddress address;
    private final ORSet<E> set;

    public AkkaORSet(short rid) {
        this.address = new SelfUniqueAddress(new UniqueAddress(new Address("", "", "", 0), (long) rid));
        this.set = ORSet.<E>create();
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
        var newSet = (ORSet<E>) set.add(address, elem).resetDelta().clearAncestor();
        return new AkkaORSet<E>(address, newSet);
    }

    @Override
    public AkkaORSet<E> remove(E elem) {
        var newSet = (ORSet<E>) set.remove(address, elem).resetDelta().clearAncestor();
        return new AkkaORSet<E>(address, newSet);
    }

    @Override
    public AkkaORSet<E> merge(AkkaORSet<E> that) {
        var newSet = (ORSet<E>) this.set.merge(that.set).resetDelta().clearAncestor();
        return new AkkaORSet<E>(this.address, newSet);
    }

    public ORSet<E> getUnderlying() {
        return set;
    }
}
