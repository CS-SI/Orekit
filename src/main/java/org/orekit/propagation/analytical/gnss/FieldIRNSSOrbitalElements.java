package org.orekit.propagation.analytical.gnss;

import org.hipparchus.RealFieldElement;

public interface FieldIRNSSOrbitalElements<T extends RealFieldElement<T>> extends FieldGNSSOrbitalElements<T> {
	
	/** WGS 84 value of the Earth's universal gravitational parameter for IRNSS user in m³/s². */
    double IRNSS_MU = 3.986005e+14;

    /** Value of Pi for conversion from semicircles to radian. */
    double IRNSS_PI = 3.1415926535898;

    /** Duration of the IRNSS week in seconds. */
    double IRNSS_WEEK_IN_SECONDS = 604800.;

    /** Number of weeks in the IRNSS cycle. */
    int IRNSS_WEEK_NB = 1024;

    /**
     * Gets the Issue Of Data Ephemeris and Clock (IODEC).
     *
     * @return the Issue Of Data Ephemeris and Clock (IODEC)
     */
    int getIODEC();

    /**
     * Gets the estimated group delay differential TGD for L5-S correction.
     *
     * @return the estimated group delay differential TGD for L5-S correction (s)
     */
    T getTGD();
}
