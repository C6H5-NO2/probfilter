package probfilter.crdt;

import probfilter.util.UnsignedNumber;

import java.io.Serializable;


public interface CausalHistory extends Serializable {
    /**
     * @param replicaId 16-bit unsigned replica id
     * @return 32-bit unsigned timestamp
     */
    int get(short replicaId);

    default int next(short replicaId) {
        return get(replicaId) + 1;
    }

    /**
     * @implNote The default implementation assumes consecutive timestamps; override if necessary.
     */
    default boolean observes(short replicaId, int timestamp) {
        return UnsignedNumber.compare(timestamp, get(replicaId)) <= 0;
    }

    CausalHistory increase(short replicaId);
}
