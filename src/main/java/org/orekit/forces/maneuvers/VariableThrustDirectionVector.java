// ///////////////////////////////////////////////////
// Copyright Exotrail (c), 2019
// ///////////////////////////////////////////////////

package org.orekit.forces.maneuvers;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;

/** Interface to compute the thrust direction of a maneuver
 * @author Mikael Fillastre
 * @author Andrea Fiorentino
 */

public interface VariableThrustDirectionVector {

    /** Compute the thrust direction corresponding to an orbital state.
     * @param pvProv local position-velocity provider around current date
     * @param date current date
     * @param frame reference frame from which attitude is computed
     * @return direction thrust direction at the specified date and position-velocity state
     */
    public Vector3D computeThrustDirection(PVCoordinatesProvider pvProv, AbsoluteDate date,
            Frame frame);
}
