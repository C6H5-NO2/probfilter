package probfilter.util;

import scala.Some;
import scala.math.PartiallyOrdered;

import java.util.HashSet;
import java.util.Set;


public final class PartialOrderOps {
    private PartialOrderOps() {}

    public static <T extends PartiallyOrdered<T>> Set<T> max(Iterable<T> iterable) {
        var set = new HashSet<T>();
        outer:
        for (var it : iterable) {
            var seiter = set.iterator();
            while (seiter.hasNext()) {
                var maxs = seiter.next();
                var cmp = it.tryCompareTo(maxs, x -> x);
                if (cmp instanceof Some) {
                    var val = (int) cmp.get();
                    if (val <= 0)
                        continue outer;
                    seiter.remove();
                }
            }
            set.add(it);
        }
        return set;
    }
}
