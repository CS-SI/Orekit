package org.orekit.data;

import org.orekit.bodies.CelestialBodies;
import org.orekit.forces.gravity.potential.GravityFields;
import org.orekit.frames.Frames;
import org.orekit.models.earth.GeoMagneticFields;
import org.orekit.time.TimeScales;

/**
 * A simple implementation of {@link DataContext} that composes the constituent factories
 * into a data context.
 *
 * @author Evan Ward
 * @since 10.1
 */
public class CompositeDataContext implements DataContext {

    /** Time scales in this data context. */
    private final TimeScales timeScales;
    /** Frames in this data context. */
    private final Frames frames;
    /** Celestial bodies in this data context. */
    private final CelestialBodies celestialBodies;
    /** Gravity fields in this data context. */
    private final GravityFields gravityFields;
    /** Magnetic fields in this data context. */
    private final GeoMagneticFields geoMagneticFields;

    /**
     * Simple constructor.
     *
     * @param timeScales        used in this data context.
     * @param frames            used in this data context.
     * @param celestialBodies   used in this data context.
     * @param gravityFields     used in this data context.
     * @param geoMagneticFields used in this data context.
     */
    public CompositeDataContext(final TimeScales timeScales,
                                final Frames frames,
                                final CelestialBodies celestialBodies,
                                final GravityFields gravityFields,
                                final GeoMagneticFields geoMagneticFields) {
        this.timeScales = timeScales;
        this.frames = frames;
        this.celestialBodies = celestialBodies;
        this.gravityFields = gravityFields;
        this.geoMagneticFields = geoMagneticFields;
    }

    @Override
    public TimeScales getTimeScales() {
        return timeScales;
    }

    @Override
    public Frames getFrames() {
        return frames;
    }

    @Override
    public CelestialBodies getCelestialBodies() {
        return celestialBodies;
    }

    @Override
    public GravityFields getGravityFields() {
        return gravityFields;
    }

    @Override
    public GeoMagneticFields getGeoMagneticFields() {
        return geoMagneticFields;
    }

}
