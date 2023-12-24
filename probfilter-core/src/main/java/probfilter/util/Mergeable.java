package probfilter.util;


public interface Mergeable<T> {
    T merge(T that);
}
