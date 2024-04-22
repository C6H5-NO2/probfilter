package probfilter.pdsa;

import java.io.Serializable;


public interface FilterHashStrategy extends Serializable {
    int capacity();

    double fpp();

    /**
     * @return a new instance of {@link probfilter.pdsa.FilterHashStrategy} with fpp half that of {@code this}
     */
    FilterHashStrategy tighten();
}
