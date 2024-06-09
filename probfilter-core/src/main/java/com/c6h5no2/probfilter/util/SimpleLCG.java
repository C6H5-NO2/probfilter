package com.c6h5no2.probfilter.util;

import java.io.Serial;


/**
 * A linear congruential generator.
 */
public final class SimpleLCG implements RandomIntGenerator {
    @Serial
    private static final long serialVersionUID = 1L;

    private int state;

    public SimpleLCG(int seed) {
        this.state = ~seed;
    }

    @Override
    public int nextInt() {
        state = (state * 1103515245 + 12345) & Integer.MAX_VALUE;
        return state;
    }

    @Override
    public SimpleLCG copy() {
        var rng = new SimpleLCG(0);
        rng.state = this.state;
        return rng;
    }
}
