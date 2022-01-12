package org.orekit.propagation.events;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

public class AngularSeparationFromSatelliteDetectorTest {

    private OneAxisEllipsoid earth;
    private TopocentricFrame acatenango;
    private AbsoluteDate     iniDate;
    private Orbit            initialOrbit;
    private Propagator       propagator;

    @Test
    public void testCentralSunTransit() {

        double proximityAngle = FastMath.toRadians(10.0);
        double maxCheck = 0.1 * proximityAngle / initialOrbit.getKeplerianMeanMotion();
        PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        AngularSeparationFromSatelliteDetector detector =
                        new AngularSeparationFromSatelliteDetector(sun, acatenango, proximityAngle).
                        withMaxCheck(maxCheck).
                        withThreshold(1.0e-6);
        Assert.assertEquals(proximityAngle, detector.getProximityAngle(), 1.0e-15);
        Assert.assertSame(sun,    detector.getPrimaryObject());
        Assert.assertSame(acatenango,  detector.getSecondaryObject());
        Assert.assertEquals(maxCheck, detector.getMaxCheckInterval(), 1.0e-15);
        propagator.addEventDetector(detector);
        final SpacecraftState finalState = propagator.propagate(iniDate.shiftedBy(3600 * 2));
        Assert.assertEquals(4587.6472, finalState.getDate().durationFrom(iniDate), 1.0e-3);
        
        final PVCoordinates sPV = finalState.getPVCoordinates();
        final PVCoordinates primaryPV   = sun       .getPVCoordinates(finalState.getDate(), finalState.getFrame());
        final PVCoordinates secondaryPV = acatenango.getPVCoordinates(finalState.getDate(), finalState.getFrame());
        final double separation = Vector3D.angle(primaryPV  .getPosition().subtract(sPV.getPosition()),
                                                 secondaryPV.getPosition().subtract(sPV.getPosition()));
        Assert.assertTrue(separation < proximityAngle);

    }

    @Test
    public void testRegularProximity() {

        double proximityAngle = FastMath.toRadians(15.0);
        double maxCheck = 0.1 * proximityAngle / initialOrbit.getKeplerianMeanMotion();
        PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        AngularSeparationFromSatelliteDetector detector =
                        new AngularSeparationFromSatelliteDetector(sun, acatenango, proximityAngle).
                        withMaxCheck(maxCheck).
                        withThreshold(1.0e-6).
                        withHandler(new EventHandler<AngularSeparationFromSatelliteDetector>() {
                public Action eventOccurred(SpacecraftState s, AngularSeparationFromSatelliteDetector detector, boolean increasing) {
                    if (increasing) {
                        Assert.assertEquals(5259.6649, s.getDate().durationFrom(iniDate), 1.0e-3);
                    } else {
                        Assert.assertEquals(4410.2581, s.getDate().durationFrom(iniDate), 1.0e-3);
                    }
                    return Action.CONTINUE;
                }
            });
        Assert.assertEquals(proximityAngle, detector.getProximityAngle(), 1.0e-15);
        Assert.assertSame(sun,    detector.getPrimaryObject());
        Assert.assertSame(acatenango,  detector.getSecondaryObject());
        Assert.assertEquals(maxCheck, detector.getMaxCheckInterval(), 1.0e-15);
        propagator.addEventDetector(detector);
        final SpacecraftState finalState = propagator.propagate(iniDate.shiftedBy(3600 * 2));
        Assert.assertEquals(7200.0, finalState.getDate().durationFrom(iniDate), 1.0e-3);

    }

    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");
            earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                         Constants.WGS84_EARTH_FLATTENING,
                                         FramesFactory.getITRF(IERSConventions.IERS_2010, true));
            acatenango = new TopocentricFrame(earth,
                                              new GeodeticPoint(FastMath.toRadians(14.500833),
                                                                FastMath.toRadians(-90.87583),
                                                                3976.0),
                                              "Acatenango");
            iniDate = new AbsoluteDate(2003, 5, 1, 17, 30, 0.0, TimeScalesFactory.getUTC());
            initialOrbit = new KeplerianOrbit(7e6, 1.0e-4, FastMath.toRadians(98.5),
                                              FastMath.toRadians(87.0), FastMath.toRadians(216.59976025619),
                                              FastMath.toRadians(319.7), PositionAngle.MEAN,
                                              FramesFactory.getEME2000(), iniDate,
                                              Constants.EIGEN5C_EARTH_MU);
            propagator = new KeplerianPropagator(initialOrbit);
        } catch (OrekitException oe) {
            Assert.fail(oe.getLocalizedMessage());
        }
    }

    @After
    public void tearDown() {
        earth        = null;
        iniDate      = null;
        initialOrbit = null;
        propagator   = null;
    }
}
