package probfilter.util;

import java.io.Serial;
import java.io.Serializable;


/**
 * A linear congruential generator.
 */
public final class SimpleLCG implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private int state;

    public SimpleLCG(int seed) {
        state = ~seed;
    }

    /**
     * @param until a positive integer
     */
    public int next(int until) {
        state = (state * 1103515245 + 12345) & Integer.MAX_VALUE;
        return state % until;
    }
}
