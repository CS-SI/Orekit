package org.orekit.models.earth;

import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.ReferenceEllipsoid;

/**
 * All models of Earth's shape have some common properties that are not shared with
 * arbitrary {@link BodyShape}s. In particular, an ellipsoidal (or spherical) model is
 * used to compute latitude and longitude.
 *
 * @author Evan Ward
 * @see #getEllipsoid()
 */
public interface EarthShape extends BodyShape {

    /**
     * Get the underlying ellipsoid model that defines latitude and longitude. If the
     * height component of a {@link GeodeticPoint} is not needed, then using the ellipsoid
     * will provide the quickest transformation.
     *
     * @return the reference ellipsoid. May be {@code this}, but never {@code null}.
     */
    ReferenceEllipsoid getEllipsoid();

}
