package org.orekit.propagation.analytical.gnss;

import org.hipparchus.RealFieldElement;
import org.orekit.time.FieldTimeStamped;

/** This interface provides the minimal set of Field orbital elements needed by the {@link FieldSBASPropagator}.
*
* @author Bryan Cazabonne
* @author Nicolas Fialton (field translation)
*
*/

public interface FieldSBASOrbitalElements<T extends RealFieldElement<T>>  extends FieldTimeStamped<T> {

	/** WGS 84 value of the Earth's universal gravitational parameter for SBAS user in m³/s². */
    double SBAS_MU = 3.986005e+14;

    /**
     * Gets the PRN number of the SBAS satellite.
     *
     * @return the PRN number of the SBAS satellite
     */
    int getPRN();

    /**
     * Gets the Reference Week of the SBAS orbit.
     *
     * @return the Reference Week of the SBAS orbit
     */
    int getWeek();

    /**
     * Gets the Reference Time of the SBAS orbit in GPS seconds of the week.
     *
     * @return the Reference Time of the SBAS orbit (s)
     */
    T getTime();

    /**
     * Get the ECEF-X component of satellite coordinates.
     *
     * @return the ECEF-X component of satellite coordinates (m)
     */
    T getX();

    /**
     * Get the ECEF-X component of satellite velocity vector.
     *
     * @return the the ECEF-X component of satellite velocity vector (m/s)
     */
    T getXDot();

    /**
     * Get the ECEF-X component of satellite acceleration vector.
     *
     * @return the GLONASS ECEF-X component of satellite acceleration vector (m/s²)
     */
    T getXDotDot();

    /**
     * Get the ECEF-Y component of satellite coordinates.
     *
     * @return the ECEF-Y component of satellite coordinates (m)
     */
    T getY();

    /**
     * Get the ECEF-Y component of satellite velocity vector.
     *
     * @return the ECEF-Y component of satellite velocity vector (m/s)
     */
    T getYDot();

    /**
     * Get the ECEF-Y component of satellite acceleration vector.
     *
     * @return the ECEF-Y component of satellite acceleration vector (m/s²)
     */
    T getYDotDot();

    /**
     * Get the ECEF-Z component of satellite coordinates.
     *
     * @return the ECEF-Z component of satellite coordinates (m)
     */
    T getZ();

    /**
     * Get the ECEF-Z component of satellite velocity vector.
     *
     * @return the the ECEF-Z component of satellite velocity vector (m/s)
     */
    T getZDot();

    /**
     * Get the ECEF-Z component of satellite acceleration vector.
     *
     * @return the ECEF-Z component of satellite acceleration vector (m/s²)
     */
    T getZDotDot();

    /**
     * Gets the Issue Of Data Navigation (IODN).
     *
     * @return the IODN
     */
    int getIODN();

    /**
     * Gets the Zeroth Order Clock Correction.
     *
     * @return the Zeroth Order Clock Correction (s)
     */
    T getAGf0();

    /**
     * Gets the First Order Clock Correction.
     *
     * @return the First Order Clock Correction (s/s)
     */
    T getAGf1();

    /**
     * Gets the clock correction reference time toc.
     *
     * @return the clock correction reference time (s)
     */
    T getToc();

}

