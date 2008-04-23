package fr.cs.orekit.maneuvers;

import java.text.ParseException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.GraggBulirschStoerIntegrator;
import org.apache.commons.math.ode.IntegratorException;
import org.apache.commons.math.util.MathUtils;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.iers.IERSDataResetter;
import fr.cs.orekit.orbits.CircularParameters;
import fr.cs.orekit.orbits.KeplerianParameters;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.orbits.OrbitalParameters;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.propagation.numerical.NumericalPropagator;
import fr.cs.orekit.propagation.numerical.OrekitSwitchingFunction;
import fr.cs.orekit.propagation.numerical.forces.maneuvers.ConstantThrustManeuver;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.time.ChunkedDate;
import fr.cs.orekit.time.ChunkedTime;
import fr.cs.orekit.time.UTCScale;
import fr.cs.orekit.utils.PVCoordinates;


public class ConstantThrustManeuverTest extends TestCase {

    public void testBadFrame() {
        try {
            new ConstantThrustManeuver(new AbsoluteDate(new ChunkedDate(2004, 01, 01),
                                                        new ChunkedTime(23, 30, 00.000),
                                                        UTCScale.getInstance()),
                                       10.0, 400.0, 300.0, Vector3D.plusK, Integer.MAX_VALUE);
            fail("an exception should have been thrown");
        } catch (IllegalArgumentException iae) {
            // expected behavior
        } catch (OrekitException e) {
            fail("wrong exception caught");
        }
    }

    public void testPositiveDuration() throws OrekitException {
        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2004, 01, 01),
                                             new ChunkedTime(23, 30, 00.000),
                                             UTCScale.getInstance());
        ConstantThrustManeuver maneuver =
            new ConstantThrustManeuver(date, 10.0, 400.0, 300.0, Vector3D.plusK,
                                       ConstantThrustManeuver.INERTIAL);
        OrekitSwitchingFunction[] switches = maneuver.getSwitchingFunctions();
        double mu = 3.986004415e14;
        OrbitalParameters dummyParameters =
            new CircularParameters(new PVCoordinates(Vector3D.plusI, Vector3D.plusJ),
                                   Frame.getJ2000(), mu);
        Orbit o1 = new Orbit(new AbsoluteDate(date, - 1.0), dummyParameters);
        assertTrue(switches[0].g(new SpacecraftState(o1, mu), mu) > 0);
        Orbit o2 = new Orbit(new AbsoluteDate(date,   1.0), dummyParameters);
        assertTrue(switches[0].g(new SpacecraftState(o2, mu), mu) < 0);
        Orbit o3 = new Orbit(new AbsoluteDate(date,   9.0), dummyParameters);
        assertTrue(switches[1].g(new SpacecraftState(o3, mu), mu) > 0);
        Orbit o4 = new Orbit(new AbsoluteDate(date,  11.0), dummyParameters);
        assertTrue(switches[1].g(new SpacecraftState(o4, mu), mu) < 0);
    }

    public void testNegativeDuration() throws OrekitException {
        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2004, 01, 01),
                                             new ChunkedTime(23, 30, 00.000),
                                             UTCScale.getInstance());
        ConstantThrustManeuver maneuver =
            new ConstantThrustManeuver(date, -10.0, 400.0, 300.0, Vector3D.plusK,
                                       ConstantThrustManeuver.INERTIAL);
        OrekitSwitchingFunction[] switches = maneuver.getSwitchingFunctions();
        double mu = 3.986004415e14;
        OrbitalParameters dummyParameters =
            new CircularParameters(new PVCoordinates(Vector3D.plusI, Vector3D.plusJ),
                                   Frame.getJ2000(), mu);
        Orbit o1 = new Orbit(new AbsoluteDate(date, -11.0), dummyParameters);
        assertTrue(switches[0].g(new SpacecraftState(o1, mu), mu) > 0);
        Orbit o2 = new Orbit(new AbsoluteDate(date,  -9.0), dummyParameters);
        assertTrue(switches[0].g(new SpacecraftState(o2, mu), mu) < 0);
        Orbit o3 = new Orbit(new AbsoluteDate(date,  -1.0), dummyParameters);
        assertTrue(switches[1].g(new SpacecraftState(o3, mu), mu) > 0);
        Orbit o4 = new Orbit(new AbsoluteDate(date,   1.0), dummyParameters);
        assertTrue(switches[1].g(new SpacecraftState(o4, mu), mu) < 0);
    }

    public void testRoughBehaviour() throws DerivativeException, IntegratorException, OrekitException, ParseException {
        double mu =  3.9860064e+14;
        double isp = 318;
        double mass = 2500;
        double a = 24396159;
        double e = 0.72831215;
        double i = Math.toRadians(7);
        double omega = Math.toRadians(180);
        double OMEGA = Math.toRadians(261);
        double lv = 0;

        double duration = 3653.99;
        double f = 420;
        double delta = Math.toRadians(-7.4978);
        double alpha = Math.toRadians(351);

        Vector3D dir = new Vector3D ( Math.cos(alpha)*Math.cos(delta),
                                      Math.cos(alpha)*Math.sin(delta),
                                      Math.sin(delta));

        OrbitalParameters transPar = new KeplerianParameters(a, e, i,
                                                             omega, OMEGA,
                                                             lv, KeplerianParameters.TRUE_ANOMALY, Frame.getJ2000());

        AbsoluteDate initDate = new AbsoluteDate(new ChunkedDate(2004, 01, 01),
                                                 new ChunkedTime(23, 30, 00.000),
                                                 UTCScale.getInstance());
        AbsoluteDate fireDate = new AbsoluteDate(new ChunkedDate(2004, 01, 02),
                                                 new ChunkedTime(04, 15, 34.080),
                                                 UTCScale.getInstance());

        SpacecraftState transOrb = new SpacecraftState(new Orbit(initDate, transPar), mass);

        ConstantThrustManeuver man = new ConstantThrustManeuver(fireDate,
                                                                duration, f, isp, dir , ConstantThrustManeuver.INERTIAL);
        GraggBulirschStoerIntegrator gragg = new GraggBulirschStoerIntegrator(1e-50, 1000, 0, 1e-08);

        NumericalPropagator pro = new NumericalPropagator(mu, gragg);

        pro.addForceModel(man);

        SpacecraftState finalorb = pro.propagate(transOrb, new AbsoluteDate(fireDate, 3800));

        assertEquals(2007.882454, finalorb .getMass(), 1e-6);
        assertEquals(2.6792, Math.toDegrees(MathUtils.normalizeAngle(finalorb.getI(), Math.PI)), 1e-4);
        assertEquals(28969, finalorb.getA()/1000, 1);

    }

    public void setUp() {
        IERSDataResetter.setUp("regular-data");
    }

    public void tearDown() {
        IERSDataResetter.tearDown();
    }

    public static Test suite() {
        return new TestSuite(ConstantThrustManeuverTest.class);
    }

}
