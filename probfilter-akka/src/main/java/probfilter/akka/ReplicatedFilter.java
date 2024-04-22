package probfilter.akka;

import akka.cluster.ddata.ReplicatedData;
import probfilter.crdt.immutable.CvFilter;

import java.io.Serial;
import java.io.Serializable;


/**
 * A type-erased adapter for {@link probfilter.crdt.immutable.CvFilter CvFilter}.
 *
 * @see probfilter.akka.ReplicatedFilterKey
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class ReplicatedFilter implements ReplicatedData, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final CvFilter filter;

    public ReplicatedFilter(CvFilter<?, ?> filter) {
        this.filter = filter;
    }

    public <E, T> CvFilter<E, T> typed() {
        return filter;
    }

    public boolean contains(Object elem) {
        return filter.contains(elem);
    }

    public ReplicatedFilter add(Object elem) {
        return new ReplicatedFilter((CvFilter) filter.add(elem));
    }

    public ReplicatedFilter remove(Object elem) {
        return new ReplicatedFilter((CvFilter) filter.remove(elem));
    }

    public ReplicatedFilter merge(ReplicatedFilter that) {
        return new ReplicatedFilter((CvFilter) this.filter.merge(that.filter));
    }

    @Override
    public ReplicatedData merge(ReplicatedData that) {
        return this.merge((ReplicatedFilter) that);
    }
}
