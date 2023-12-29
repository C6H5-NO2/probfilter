package probfilter.crdt;


public interface Mergeable<T> {
    T merge(T that);
}
