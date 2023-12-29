package probfilter.pdsa;

import java.io.Serializable;


/**
 * An immutable table of 64-bit entries for cuckoo filter.
 */
public interface LongCuckooTable extends Serializable {
    LongCuckooBucket at(int i);
}
