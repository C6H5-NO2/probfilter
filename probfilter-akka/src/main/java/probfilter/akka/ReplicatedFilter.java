package probfilter.akka;

import akka.cluster.ddata.ReplicatedData;
import probfilter.crdt.immutable.ImmCvFilter;

import java.io.Serial;
import java.io.Serializable;


/**
 * A type-erased adapter for {@link ImmCvFilter CvFilter}.
 *
 * @see probfilter.akka.ReplicatedFilterKey
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class ReplicatedFilter implements ReplicatedData, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final ImmCvFilter filter;

    public ReplicatedFilter(ImmCvFilter<?, ?> filter) {
        this.filter = filter;
    }

    public <E, T> ImmCvFilter<E, T> typed() {
        return filter;
    }

    public boolean contains(Object elem) {
        return filter.contains(elem);
    }

    public ReplicatedFilter add(Object elem) {
        return new ReplicatedFilter((ImmCvFilter) filter.add(elem));
    }

    public ReplicatedFilter remove(Object elem) {
        return new ReplicatedFilter((ImmCvFilter) filter.remove(elem));
    }

    public ReplicatedFilter merge(ReplicatedFilter that) {
        return new ReplicatedFilter((ImmCvFilter) this.filter.merge(that.filter));
    }

    @Override
    public ReplicatedData merge(ReplicatedData that) {
        return this.merge((ReplicatedFilter) that);
    }
}
