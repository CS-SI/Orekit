package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import org.apache.commons.math.util.FastMath;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

public class OrbitFactory {

    private final static AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

    private final static double       mu   = Constants.WGS84_EARTH_MU;

    /**
     * @param a
     *            satellite altitude
     * @return Heliosynchronous orbit
     */
    public static Orbit getHeliosynchronousOrbit(final double alt,
                                                 final double eccentricity,
                                                 final double pa,
                                                 final double raan,
                                                 final double anomaly,
                                                 final PositionAngle angleType,
                                                 final double mu) {
        // Get inclination :
        final double ae = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        final double a = ae + alt;
        final double period = 2.0 * FastMath.PI * a * FastMath.sqrt(a / mu);
        final double rotationPerDay = 86400d / period;
        // Daily precession in degrees for heliosynchonism
        final double daj = 365 / 365.25;
        // Daily precession for the current satellite
        final double da = daj / rotationPerDay;

        final double coeff = -0.58 * Math.pow(ae / a, 2);
        final double i = Math.acos(da / coeff);
        return new KeplerianOrbit(a, eccentricity, i, pa, raan, anomaly, angleType, FramesFactory.getEME2000(), date, mu);

    }

    public static Orbit getGeostationnaryOrbit() {
        /** geostationnary orbit */
        double a = 42166712;
        double ix = 1.200e-04;
        double iy = -1.16e-04;
        double inc = 2 * FastMath.asin(FastMath.sqrt((ix * ix + iy * iy) / 4.));
        double hx = FastMath.tan(inc / 2.) * ix / (2 * FastMath.sin(inc / 2.));
        double hy = FastMath.tan(inc / 2.) * iy / (2 * FastMath.sin(inc / 2.));
        return new EquinoctialOrbit(a, 1e-4, 2e-4, hx, hy, 0, PositionAngle.MEAN, FramesFactory.getEME2000(), date, mu);
    }

}
