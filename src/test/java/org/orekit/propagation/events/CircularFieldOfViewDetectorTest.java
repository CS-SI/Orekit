package org.orekit.propagation.events;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.BodyCenterPointing;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;



public class CircularFieldOfViewDetectorTest {

    // Body mu
    private double mu;

    // Computation date
    private AbsoluteDate initDate;

    // Orbit
    private Orbit initialOrbit;

    // WGS84 Earth model
    private OneAxisEllipsoid earth;

    // Earth center pointing attitude provider
    private BodyCenterPointing earthCenterAttitudeLaw;

    @Test
    public void testCircularFielOfView() throws OrekitException {

        // Definition of initial conditions with position and velocity
        //------------------------------------------------------------

        // Extrapolator definition
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, earthCenterAttitudeLaw);

        // Event definition : circular field of view, along X axis, aperture 35°
        final double maxCheck  = 1.;
        final PVCoordinatesProvider sunPV = CelestialBodyFactory.getSun();
        final Vector3D center = Vector3D.PLUS_I;
        final double aperture = FastMath.toRadians(35);

        final EventDetector sunVisi =
            new CircularFieldOfViewDetector(maxCheck, sunPV, center, aperture).withHandler(new CircularSunVisiHandler());

        // Add event to be detected
        propagator.addEventDetector(sunVisi);

        // Extrapolate from the initial to the final date
        propagator.propagate(initDate.shiftedBy(6000.));

    }

    @Before
    public void setUp() {
        try {

            Utils.setDataRoot("regular-data");

            // Computation date
            // Satellite position as circular parameters
            mu = 3.9860047e14;

            initDate = new AbsoluteDate(new DateComponents(1969, 8, 28),
                                                     TimeComponents.H00,
                                                     TimeScalesFactory.getUTC());

            Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
            Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
            initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                FramesFactory.getEME2000(), initDate, mu);


            // WGS84 Earth model
            earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                         Constants.WGS84_EARTH_FLATTENING,
                                         FramesFactory.getITRF(IERSConventions.IERS_2010, true));

            // Create earth center pointing attitude provider */
            earthCenterAttitudeLaw = new BodyCenterPointing(initialOrbit.getFrame(), earth);

        } catch (OrekitException oe) {
            Assert.fail(oe.getMessage());
        }

    }


    /** Handler for visibility event. */
    private static class CircularSunVisiHandler implements EventHandler<CircularFieldOfViewDetector> {

        public Action eventOccurred(final SpacecraftState s, final CircularFieldOfViewDetector detector,
                                    final boolean increasing)
            throws OrekitException {
            if (increasing) {
                // System.err.println(" Sun visibility starts " + s.getDate());
                AbsoluteDate startVisiDate = new AbsoluteDate(new DateComponents(1969, 8, 28),
                                                            new TimeComponents(0, 11 , 7.820),
                                                            TimeScalesFactory.getUTC());
              Assert.assertTrue(s.getDate().durationFrom(startVisiDate) <= 1);
                return Action.CONTINUE;
            } else {
                // System.err.println(" Sun visibility ends at " + s.getDate());
                AbsoluteDate endVisiDate = new AbsoluteDate(new DateComponents(1969, 8, 28),
                                                            new TimeComponents(0, 25 , 18.224),
                                                            TimeScalesFactory.getUTC());
                Assert.assertTrue(s.getDate().durationFrom(endVisiDate) <= 1);
                return Action.CONTINUE;//STOP;
            }
        }

        public SpacecraftState resetState(CircularFieldOfViewDetector detector, SpacecraftState oldState) {
            return oldState;
        }

    }

}
