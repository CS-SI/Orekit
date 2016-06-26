package org.orekit.forces.maneuvers;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

/**
 * Tests the functionality of starting a propagation in between and on start and end times
 * of a Constant Thrust Maneuver
 *
 * @author Greg Carbott
 */
public class ConstantThrustManeuverInitializationTest {

    private NumericalPropagator propagator;
    private AbsoluteDate startDate;
    private SpacecraftState initialState;
    /** Propagation duration in seconds */
    private double propDuration = 1200.;

    /** Maneuver duration in seconds */
    private double duration = 60.;

    /** Maneuver Thrust Force in Newtons */
    private double thrust = 1e3;

    /** Maneuver Specific Impulse in seconds */
    private double isp = 100.;

    /** Mass of Spacecraft in kilograms */
    private double mass = 2e3;

    /** Tolerance of deltaV (m/s) and mass (kg) difference */
    private double tolerance = 1e-8;

    /** Direction of Maneuver */
    private Vector3D direction = new Vector3D(1, 0, 1);

    /** Control Items */
    private double deltaVControlFullForward;
    private double massControlFullForward;
    private double massControlHalfForward;
    private double deltaVControlFullReverse;
    private double deltaVControlHalfReverse;
    private double massControlFullReverse;
    private double massControlHalfReverse;


    /** set orekit data */
    @BeforeClass
    public static void setUpBefore() {
        Utils.setDataRoot("regular-data");
    }

    @Before
    public void setUp() throws OrekitException {
        startDate = new AbsoluteDate();
        double a = Constants.EGM96_EARTH_EQUATORIAL_RADIUS + 400e3;
        double e = 0.001;
        double i = (Math.PI / 4);
        double pa = 0.0;
        double raan = 0.0;
        double anomaly = 0.0;
        PositionAngle type = PositionAngle.MEAN;
        Frame frame = FramesFactory.getEME2000();
        double mu = Constants.EGM96_EARTH_MU;
        Orbit orbit = new KeplerianOrbit(a, e, i, pa, raan, anomaly,
                type, frame, startDate, mu);
        initialState = new SpacecraftState(orbit, mass);

        //Numerical Propagator
        double minStep = 0.001;
        double maxStep = 1000.0;
        double positionTolerance = 10.;
        OrbitType propagationType = OrbitType.KEPLERIAN;
        double[][] tolerances =
                NumericalPropagator.tolerances(positionTolerance, orbit, propagationType);
        AdaptiveStepsizeIntegrator integrator =
                new DormandPrince853Integrator(minStep, maxStep,
                        tolerances[0], tolerances[1]);
        //Set up propagator
        propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(propagationType);

        //Control deltaVs and mass changes
        double flowRate = -thrust / (Constants.G0_STANDARD_GRAVITY * isp);
        massControlFullForward = mass + (flowRate * duration);
        deltaVControlFullForward = isp * Constants.G0_STANDARD_GRAVITY *
                FastMath.log(mass / massControlFullForward);

        massControlHalfForward = mass + (flowRate * duration / 2);

        massControlFullReverse = mass - (flowRate * duration);
        deltaVControlFullReverse = isp * Constants.G0_STANDARD_GRAVITY *
                FastMath.log(massControlFullReverse / mass);

        massControlHalfReverse = mass - (flowRate * duration / 2);
        deltaVControlHalfReverse = isp * Constants.G0_STANDARD_GRAVITY *
                FastMath.log(massControlHalfReverse / mass);


    }


    @Test
    public void testInBetween() throws OrekitException {
        //Create test Thrust Maneuver
        ConstantThrustManeuver ctm = new ConstantThrustManeuver(
                startDate.shiftedBy(-(duration / 2)),
                duration, thrust, isp, direction);
        //Reset and populate propagator
        propagator.removeForceModels();
        propagator.addForceModel(ctm);
        propagator.setInitialState(initialState);
        SpacecraftState finalStateTest =
                propagator.propagate(startDate.shiftedBy(propDuration));

        Assert.assertEquals(massControlHalfForward, finalStateTest.getMass(), tolerance);

    }

    @Test
    public void testOnStart() throws OrekitException {
        //Create test Thrust Maneuver
        ConstantThrustManeuver ctm = new ConstantThrustManeuver(
                startDate.shiftedBy(0.0),
                duration, thrust, isp, direction);
        //Reset and populate propagator
        propagator.removeForceModels();
        propagator.addForceModel(ctm);
        propagator.setInitialState(initialState);
        SpacecraftState finalStateTest =
                propagator.propagate(startDate.shiftedBy(propDuration));

        double deltaVTest = isp * Constants.G0_STANDARD_GRAVITY *
                FastMath.log(mass / finalStateTest.getMass());

        Assert.assertEquals(deltaVControlFullForward, deltaVTest, tolerance);
        Assert.assertEquals(massControlFullForward, finalStateTest.getMass(), tolerance);

    }

    @Test
    public void testOnEnd() throws OrekitException {
        //Create test Thrust Maneuver
        ConstantThrustManeuver ctm = new ConstantThrustManeuver(
                startDate.shiftedBy(0.0 - duration),
                duration, thrust, isp, direction);
        //Reset and populate propagator
        propagator.removeForceModels();
        propagator.addForceModel(ctm);
        propagator.setInitialState(initialState);
        SpacecraftState finalStateTest =
                propagator.propagate(startDate.shiftedBy(propDuration));

        double deltaVTest = isp * Constants.G0_STANDARD_GRAVITY *
                FastMath.log(mass / finalStateTest.getMass());

        Assert.assertTrue(deltaVTest == 0.0);
        Assert.assertTrue(finalStateTest.getMass() == mass);

    }

    @Test
    public void testOnEndReverse() throws OrekitException {
        //Create test Thrust Maneuver
        ConstantThrustManeuver ctm = new ConstantThrustManeuver(
                startDate.shiftedBy(0.0),
                -duration, thrust, isp, direction);
        //Reset and populate propagator
        propagator.removeForceModels();
        propagator.addForceModel(ctm);
        propagator.setInitialState(initialState);
        SpacecraftState finalStateTest =
                propagator.propagate(startDate.shiftedBy(-propDuration));

        double deltaVTest = isp * Constants.G0_STANDARD_GRAVITY *
                FastMath.log(finalStateTest.getMass() / mass);

        Assert.assertEquals(deltaVControlFullReverse, deltaVTest, tolerance);
        Assert.assertEquals(massControlFullReverse, finalStateTest.getMass(), tolerance);

    }

    @Test
    public void testOnStartReverse() throws OrekitException {
        //Create test Thrust Maneuver
        ConstantThrustManeuver ctm = new ConstantThrustManeuver(
                startDate.shiftedBy(0.0),
                duration, thrust, isp, direction);
        //Reset and populate propagator
        propagator.removeForceModels();
        propagator.addForceModel(ctm);
        propagator.setInitialState(initialState);
        SpacecraftState finalStateTest =
                propagator.propagate(startDate.shiftedBy(-propDuration));

        double deltaVTest = isp * Constants.G0_STANDARD_GRAVITY *
                FastMath.log(finalStateTest.getMass() / mass);

        Assert.assertTrue(deltaVTest == 0.0);
        Assert.assertTrue(finalStateTest.getMass() == mass);

    }

    @Test
    public void testInBetweenReverse() throws OrekitException {
        //Create test Thrust Maneuver
        ConstantThrustManeuver ctm = new ConstantThrustManeuver(
                startDate.shiftedBy(duration / 2),
                -duration, thrust, isp, direction);
        //Reset and populate propagator
        propagator.removeForceModels();
        propagator.addForceModel(ctm);
        propagator.setInitialState(initialState);
        SpacecraftState finalStateTest =
                propagator.propagate(startDate.shiftedBy(-propDuration));

        double deltaVTest = isp * Constants.G0_STANDARD_GRAVITY *
                FastMath.log(finalStateTest.getMass() / mass);

        Assert.assertEquals(deltaVControlHalfReverse, deltaVTest, tolerance);
        Assert.assertEquals(massControlHalfReverse, finalStateTest.getMass(), tolerance);

    }

    @Test
    public void testControlForward() throws OrekitException {
        //Create test Thrust Maneuver
        ConstantThrustManeuver ctm = new ConstantThrustManeuver(
                startDate.shiftedBy(1.0),
                duration, thrust, isp, direction);
        //Reset and populate propagator
        propagator.removeForceModels();
        propagator.addForceModel(ctm);
        propagator.setInitialState(initialState);
        SpacecraftState finalStateTest =
                propagator.propagate(startDate.shiftedBy(propDuration));

        double deltaVTest = isp * Constants.G0_STANDARD_GRAVITY *
                FastMath.log(mass / finalStateTest.getMass());

        Assert.assertEquals(deltaVControlFullForward, deltaVTest, tolerance);
        Assert.assertEquals(massControlFullForward, finalStateTest.getMass(), tolerance);

    }

    @Test
    public void testControlReverse() throws OrekitException {
        //Create test Thrust Maneuver
        ConstantThrustManeuver ctm = new ConstantThrustManeuver(
                startDate.shiftedBy(-1.0),
                -duration, thrust, isp, direction);
        //Reset and populate propagator
        propagator.removeForceModels();
        propagator.addForceModel(ctm);
        propagator.setInitialState(initialState);
        SpacecraftState finalStateTest =
                propagator.propagate(startDate.shiftedBy(-propDuration));

        double deltaVTest = isp * Constants.G0_STANDARD_GRAVITY *
                FastMath.log(finalStateTest.getMass() / mass);

        Assert.assertEquals(deltaVControlFullReverse, deltaVTest, tolerance);
        Assert.assertEquals(massControlFullReverse, finalStateTest.getMass(), tolerance);

    }
}
