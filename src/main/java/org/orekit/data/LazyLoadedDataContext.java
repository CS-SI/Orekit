package org.orekit.data;

import org.orekit.bodies.LazyLoadedCelestialBodies;
import org.orekit.frames.LazyLoadedEop;
import org.orekit.frames.LazyLoadedFrames;
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
    private final LazyLoadedTimeScales timeScales;
    /** The reference frames. */
    private final LazyLoadedFrames frames;
    /** The celestial bodies. */
    private final LazyLoadedCelestialBodies bodies;

    /**
     * Create a new data context that only loads auxiliary data when it is first accessed
     * and allows configuration of the auxiliary data sources until then.
     */
    public LazyLoadedDataContext() {
        final LazyLoadedEop lazyLoadedEop = new LazyLoadedEop();
        this.timeScales = new LazyLoadedTimeScales(lazyLoadedEop);
        this.bodies = new LazyLoadedCelestialBodies();
        this.frames = new LazyLoadedFrames(lazyLoadedEop, timeScales, bodies);
    }

    @Override
    public LazyLoadedTimeScales getTimeScales() {
        return timeScales;
    }

    @Override
    public LazyLoadedFrames getFrames() {
        return frames;
    }

    @Override
    public LazyLoadedCelestialBodies getCelestialBodies() {
        return bodies;
    }

}
