package org.orekit.frames;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeFunction;
import org.orekit.utils.IERSConventions;

/**
 * An inertial frame aligned with the ecliptic.
 *
 * The IAU defines the ecliptic as "the plane perpendicular to the mean heliocentric
 * orbital angular momentum vector of the Earth-Moon barycentre in the BCRS (IAU 2006
 * Resolution B1)." The +z axis is aligned with the angular momentum vector, and the +x
 * axis is aligned with +x axis of {@link FramesFactory#getMOD(IERSConventions) MOD}.
 *
 * <p> This implementation agrees with the JPL 406 ephemerides to within 0.5 arc seconds.
 *
 * @since 6.2
 */
public class EclipticFrame extends Frame {

    /**
     * Create a frame aligned with the ecliptic.
     *
     * @param conventions the IERS conventions for the mean obliquity of the ecliptic.
     * @throws OrekitException if the mean obliquity of the ecliptic function can not be
     *                         loaded.
     */
    public EclipticFrame(final IERSConventions conventions) throws OrekitException {
        super(FramesFactory.getMOD(conventions),
                new EclipticTransform(conventions.getMeanObliquityFunction()),
                "Ecliptic " + conventions.toString(),
                true);
    }

    /** A transform provider from ICRF/GCRF to an ecliptically aligned frame. */
    private static class EclipticTransform implements TransformProvider {

        /** the obliquity of the ecliptic, in radians as a function of time. */
        private final TimeFunction<Double> obliquity;

        /**
         * Create a transform provider from MOD to an ecliptically aligned frame.
         *
         * @param obliquity the mean obliquity of the ecliptic, in radians.
         */
        public EclipticTransform(final TimeFunction<Double> obliquity) {
            this.obliquity = obliquity;
        }

        @Override
        public Transform getTransform(final AbsoluteDate date) throws OrekitException {
            //mean obliquity of date
            final double epsA = obliquity.value(date);
            return new Transform(date, new Rotation(Vector3D.MINUS_I, epsA));
        }

    }

}
