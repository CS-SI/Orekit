package org.orekit.propagation.events;

import static org.orekit.orbits.PositionAngleType.MEAN;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldEcksteinHechlerPropagator;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

/** Unit tests for {@link FieldLongitudeCrossingDetector}. */
public class FieldLongitudeCrossingDetectorTest {

    /**
     * Arbitrary Field.
     */
    private static final Binary64Field field = Binary64Field.getInstance();


    @Test
    public void testRegularCrossing() {

        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING,
            FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        FieldLongitudeCrossingDetector<Binary64> d =
            new FieldLongitudeCrossingDetector<>(v(60.0), v(1.e-6), earth,
                FastMath.toRadians(10.0)).
                withHandler(new FieldContinueOnEvent<>());

        Assertions.assertEquals(60.0, d.getMaxCheckInterval().currentInterval(null), 1.0e-15);
        Assertions.assertEquals(1.0e-6, d.getThreshold().getReal(), 1.0e-15);
        Assertions.assertEquals(10.0, FastMath.toDegrees(d.getLongitude()), 1.0e-14);
        Assertions.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, d.getMaxIterationCount());
        Assertions.assertSame(earth, d.getBody());

        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(505.848, 942.781, 7435.922);
        final FieldAbsoluteDate<Binary64> date = new FieldAbsoluteDate<>(field, 2003, 9, 16, utc);
        final FieldOrbit<Binary64> orbit = new FieldEquinoctialOrbit<>(
            new FieldPVCoordinates<>(v(1), new PVCoordinates(position, velocity)),
            FramesFactory.getEME2000(), date,
            v(Constants.EIGEN5C_EARTH_MU));

        FieldPropagator<Binary64> propagator =
            new FieldEcksteinHechlerPropagator<>(orbit,
                Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                v(Constants.EIGEN5C_EARTH_MU),
                Constants.EIGEN5C_EARTH_C20,
                Constants.EIGEN5C_EARTH_C30,
                Constants.EIGEN5C_EARTH_C40,
                Constants.EIGEN5C_EARTH_C50,
                Constants.EIGEN5C_EARTH_C60);

        FieldEventsLogger<Binary64> logger = new FieldEventsLogger<>();
        propagator.addEventDetector(logger.monitorDetector(d));

        propagator.propagate(date.shiftedBy(Constants.JULIAN_DAY));
        FieldAbsoluteDate<Binary64> previous = null;
        for (FieldEventsLogger.FieldLoggedEvent<Binary64> e : logger.getLoggedEvents()) {
            FieldSpacecraftState<Binary64> state = e.getState();
            double longitude = earth.transform(state.getPosition(earth.getBodyFrame()),
                earth.getBodyFrame(), date).getLongitude().getReal();
            Assertions.assertEquals(10.0, FastMath.toDegrees(longitude), 3.5e-7);
            if (previous != null) {
                // same time interval regardless of increasing/decreasing,
                // as increasing/decreasing flag is irrelevant for this detector
                Assertions.assertEquals(4954.70, state.getDate().durationFrom(previous).getReal(), 1e10);
            }
            previous = state.getDate();
        }
        Assertions.assertEquals(16, logger.getLoggedEvents().size());

    }

    @Test
    public void testZigZag() {

        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING,
            FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        FieldLongitudeCrossingDetector<Binary64> d =
            new FieldLongitudeCrossingDetector<>(v(600.0), v(1.e-6), earth,
                FastMath.toRadians(-100.0)).
                withHandler(new FieldContinueOnEvent<>());

        Assertions.assertEquals(600.0, d.getMaxCheckInterval().currentInterval(null), 1.0e-15);
        Assertions.assertEquals(1.0e-6, d.getThreshold().getReal(), 1.0e-15);
        Assertions.assertEquals(-100.0, FastMath.toDegrees(d.getLongitude()), 1.0e-14);
        Assertions.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, d.getMaxIterationCount());

        FieldAbsoluteDate<Binary64> date = FieldAbsoluteDate.getJ2000Epoch(field);
        final FieldOrbit<Binary64> orbit =
            new FieldKeplerianOrbit<>(v(24464560.0), v(0.7311), v(0.122138), v(3.10686), v(1.00681),
                v(0.048363), MEAN,
                FramesFactory.getEME2000(),
                date,
                v(Constants.EIGEN5C_EARTH_MU));


        FieldPropagator<Binary64> propagator = new FieldKeplerianPropagator<>(orbit);

        FieldEventsLogger<Binary64> logger = new FieldEventsLogger<>();
        propagator.addEventDetector(logger.monitorDetector(d));

        propagator.propagate(orbit.getDate().shiftedBy(Constants.JULIAN_DAY));
        double[] expectedLatitudes = new double[] { -6.5394381901, -0.4918760372, +6.5916016832 };
        Assertions.assertEquals(3, logger.getLoggedEvents().size());
        for (int i = 0; i < 3; ++i) {
            FieldSpacecraftState<Binary64> state = logger.getLoggedEvents().get(i).getState();
            FieldGeodeticPoint<Binary64> gp = earth.transform(state.getPosition(earth.getBodyFrame()),
                earth.getBodyFrame(), date);
            Assertions.assertEquals(expectedLatitudes[i], FastMath.toDegrees(gp.getLatitude()).getReal(),  1.0e-10);
            Assertions.assertEquals(-100.0, FastMath.toDegrees(gp.getLongitude().getReal()), 1.2e-9);
        }

    }

    /**
     * Convert double to field value.
     *
     * @param value to box.
     * @return boxed value.
     */
    private static Binary64 v(double value) {
        return new Binary64(value);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
