package org.orekit.data;

import org.orekit.bodies.LazyLoadedCelestialBodies;
import org.orekit.forces.gravity.potential.LazyLoadedGravityFields;
import org.orekit.frames.LazyLoadedEop;
import org.orekit.frames.LazyLoadedFrames;
import org.orekit.models.earth.GeoMagneticFields;
import org.orekit.models.earth.LazyLoadedGeoMagneticFields;
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

    /** The data provider manager. */
    private final DataProvidersManager dataProvidersManager;
    /** The time scales. */
    private final LazyLoadedTimeScales timeScales;
    /** The reference frames. */
    private final LazyLoadedFrames frames;
    /** The celestial bodies. */
    private final LazyLoadedCelestialBodies bodies;
    /** The gravity fields. */
    private final LazyLoadedGravityFields gravityFields;
    /** The magnetic fields. */
    private final LazyLoadedGeoMagneticFields geoMagneticFields;

    /**
     * Create a new data context that only loads auxiliary data when it is first accessed
     * and allows configuration of the auxiliary data sources until then.
     */
    public LazyLoadedDataContext() {
        this.dataProvidersManager = new DataProvidersManager();
        final LazyLoadedEop lazyLoadedEop = new LazyLoadedEop(dataProvidersManager);
        this.timeScales = new LazyLoadedTimeScales(lazyLoadedEop);
        this.bodies = new LazyLoadedCelestialBodies(dataProvidersManager, timeScales);
        this.frames = new LazyLoadedFrames(lazyLoadedEop, timeScales, bodies);
        this.gravityFields = new LazyLoadedGravityFields(dataProvidersManager);
        this.geoMagneticFields = new LazyLoadedGeoMagneticFields(dataProvidersManager);
    }

    /**
     * Get the provider of auxiliary data for this data context.
     *
     * @return the provider that supplies auxiliary data to all of the other methods of
     * this data context.
     */
    public DataProvidersManager getDataProvidersManager() {
        return dataProvidersManager;
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

    @Override
    public LazyLoadedGravityFields getGravityFields() {
        return gravityFields;
    }

    @Override
    public LazyLoadedGeoMagneticFields getGeoMagneticFields() {
        return geoMagneticFields;
    }

}
