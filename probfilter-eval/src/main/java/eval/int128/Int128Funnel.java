package eval.int128;

import probfilter.hash.Funnel;
import probfilter.hash.Sink;


public final class Int128Funnel implements Funnel<Int128> {
    @Override
    public void funnel(Int128 from, Sink into) {
        into.putLong(from.high()).putLong(from.low());
    }
}
