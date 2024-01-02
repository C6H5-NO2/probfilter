package probfilter.akka;

import akka.cluster.ddata.ReplicatedData;
import probfilter.crdt.BaseFilter;

import java.io.Serial;
import java.io.Serializable;


/**
 * A type-erased wrapper for {@link probfilter.crdt.BaseFilter BaseFilter}.
 *
 * @see probfilter.akka.ReplicatedFilterKey
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ReplicatedFilter implements ReplicatedData, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final BaseFilter filter;

    public ReplicatedFilter(BaseFilter<?, ?> filter) {
        this.filter = filter;
    }

    public <E, T extends BaseFilter<E, T>> BaseFilter<E, T> as() {
        return filter;
    }

    public boolean contains(Object elem) {
        return filter.mightContains(elem);
    }

    public ReplicatedFilter add(Object elem) {
        return new ReplicatedFilter(filter.add(elem));
    }

    public ReplicatedFilter remove(Object elem) {
        return new ReplicatedFilter(filter.remove(elem));
    }

    public ReplicatedFilter merge(ReplicatedFilter that) {
        return new ReplicatedFilter((BaseFilter) this.filter.merge(that.filter));
    }

    @Override
    public ReplicatedData merge(ReplicatedData that) {
        return this.merge((ReplicatedFilter) that);
    }
}
