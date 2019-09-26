package org.orekit.data;

import org.orekit.time.LazyLoadedTimeScales;

/**
 * A data context that aims to match the behavior of Orekit 10.0 regarding auxiliary data.
 * This data context only loads auxiliary data when it is first accessed. It allows data
 * loaders to be added before the data is loaded.
 *
 * @author Evan Ward
 * @since 10.1
 */
public class LazyLoadedDataContext implements DataContext {

    /** The time scales. */
    private final LazyLoadedTimeScales timeScales = new LazyLoadedTimeScales();

    @Override
    public LazyLoadedTimeScales getTimeScales() {
        return timeScales;
    }

}
