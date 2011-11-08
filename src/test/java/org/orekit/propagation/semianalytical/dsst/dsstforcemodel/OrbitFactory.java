package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import org.apache.commons.math.util.FastMath;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

public class OrbitFactory {


    private final static double       mu   = Constants.WGS84_EARTH_MU;

    /**
     * @param alt
     *            satellite altitude
     * @return Heliosynchronous orbit
     */
    public static Orbit getHeliosynchronousOrbit(final double ae,
                                                 final double alt,
                                                 final double eccentricity,
                                                 final double pa,
                                                 final double raan,
                                                 final double meanAnomaly,
                                                 final double mu,
                                                 final Frame frame,
                                                 AbsoluteDate date) {
        // Get inclination :
        final double a = ae + alt;
        final double period = 2.0 * FastMath.PI * a * FastMath.sqrt(a / mu);
        final double rotationPerDay = 86400d / period;
        // Daily precession in degrees for heliosynchonism
        final double daj = 365 / 365.25;
        // Daily precession for the current satellite
        final double da = daj / rotationPerDay;

        final double coeff = -0.58 * Math.pow(ae / a, 2);
        final double i = Math.acos(da / coeff);
        final double h = eccentricity * Math.sin(pa + raan);
        final double k = eccentricity * Math.cos(pa + raan);
        final double p = Math.tan(i/2) * Math.sin(raan);
        final double q = Math.tan(i/2) * Math.cos(raan);
        final double mean = meanAnomaly + pa + raan;
        return new EquinoctialOrbit(a, k, h, q, p, mean, PositionAngle.MEAN, frame, date, mu);

    }

    public static Orbit getGeostationnaryOrbit() {
        /** geostationnary orbit */
        double a = 42166712;
        double ix = 1.200e-04;
        double iy = -1.16e-04;
        double inc = 2 * FastMath.asin(FastMath.sqrt((ix * ix + iy * iy) / 4.));
        double hx = FastMath.tan(inc / 2.) * ix / (2 * FastMath.sin(inc / 2.));
        double hy = FastMath.tan(inc / 2.) * iy / (2 * FastMath.sin(inc / 2.));
        return new EquinoctialOrbit(a, 1e-4, 2e-4, hx, hy, 0, PositionAngle.MEAN, FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, mu);
    }

}
