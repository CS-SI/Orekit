package org.orekit.propagation.analytical.gnss;

import org.hipparchus.RealFieldElement;

/** This interface provides the minimal set of orbital elements needed by the {@link QZSSPropagator}.
*
* @see <a href="http://qzss.go.jp/en/technical/download/pdf/ps-is-qzss/is-qzss-pnt-003.pdf?t=1549268771755">
*       QZSS Interface Specification</a>
*
* @author Bryan Cazabonne
* @author Nicolas Fialton (field translation)
*
*/
public interface FieldQZSSOrbitalElements<T extends RealFieldElement<T>> extends FieldGNSSOrbitalElements<T> {
	
	// Constants
    /** WGS 84 value of the Earth's universal gravitational parameter for QZSS user in m³/s². */
    double QZSS_MU = 3.986005e+14;

    /** Value of Pi for conversion from semicircles to radian. */
    double QZSS_PI = 3.1415926535898;

    /** Duration of the QZSS week in seconds. */
    double QZSS_WEEK_IN_SECONDS = 604800.;

    /** Number of weeks in the QZSS cycle. */
    int QZSS_WEEK_NB = 1024;

    /**
     * Gets the Issue Of Data Clock (IODC).
     *
     * @return the Issue Of Data Clock (IODC)
     */
    int getIODC();

    /**
     * Gets the Issue Of Data Ephemeris (IODE).
     *
     * @return the Issue Of Data Ephemeris (IODE)
     */
    int getIODE();

    /**
     * Gets the estimated group delay differential TGD between SV clock and L1C/A.
     *
     * @return the estimated group delay differential TGD between SV clock and L1C/A (s)
     */
    T getTGD();
}
