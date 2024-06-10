package eval.int128;

import com.c6h5no2.probfilter.hash.Funnel;
import com.c6h5no2.probfilter.hash.Sink;


public enum Int128Funnel implements Funnel<Int128> {
    INSTANCE;

    @Override
    public void apply(Int128 from, Sink into) {
        into.putLong(from.high()).putLong(from.low());
    }
}
