package fr.cs.orekit.propagation;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.util.MathUtils;

import fr.cs.orekit.Utils;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.orbits.EquinoctialOrbit;
import fr.cs.orekit.orbits.KeplerianOrbit;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.propagation.analytical.KeplerianPropagator;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.utils.PVCoordinates;

public class KeplerianPropagatorTest extends TestCase {

    // Body mu
    private double mu;
    
    public KeplerianPropagatorTest(String name) {
        super(name);
    }

    public void testSameDateCartesian() throws OrekitException {

        // Definition of initial conditions with position and velocity
        //------------------------------------------------------------
        Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);

        AbsoluteDate initDate = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 584.);
        Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                  Frame.getJ2000(), initDate, mu);

        // Extrapolator definition
        // -----------------------
        KeplerianPropagator extrapolator = new KeplerianPropagator(new SpacecraftState(initialOrbit));

        // Extrapolation at the initial date
        // ---------------------------------
        double delta_t = 0.0; // extrapolation duration in seconds
        AbsoluteDate extrapDate = new AbsoluteDate(initDate, delta_t);

        SpacecraftState finalOrbit = extrapolator.propagate(extrapDate);

        double a = finalOrbit.getA();
        // another way to compute n
        double n = Math.sqrt(finalOrbit.getMu()/Math.pow(a, 3));

        assertEquals(n*delta_t,
                     finalOrbit.getLM() - initialOrbit.getLM(),
                     Utils.epsilonTest * Math.abs(n*delta_t));
        assertEquals(MathUtils.normalizeAngle(finalOrbit.getLM(),initialOrbit.getLM()), initialOrbit.getLM(), Utils.epsilonAngle * Math.abs(initialOrbit.getLM()));

        assertEquals(finalOrbit.getA(), initialOrbit.getA(), Utils.epsilonTest * initialOrbit.getA());
        assertEquals(finalOrbit.getE(), initialOrbit.getE(), Utils.epsilonE * initialOrbit.getE());
        assertEquals(MathUtils.normalizeAngle(finalOrbit.getI(), initialOrbit.getI()), initialOrbit.getI(), Utils.epsilonAngle * Math.abs(initialOrbit.getI()));

    }

    public void testSameDateKeplerian() throws OrekitException {
        // Definition of initial conditions with keplerian parameters
        //-----------------------------------------------------------
        AbsoluteDate initDate = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 584.);
        Orbit initialOrbit = new KeplerianOrbit(7209668.0, 0.5e-4, 1.7, 2.1, 2.9,
                                                6.2, KeplerianOrbit.TRUE_ANOMALY, 
                                                Frame.getJ2000(), initDate, mu);

        // Extrapolator definition
        // -----------------------
        KeplerianPropagator extrapolator = new KeplerianPropagator(new SpacecraftState(initialOrbit));

        // Extrapolation at the initial date
        // ---------------------------------
        double delta_t = 0.0; // extrapolation duration in seconds
        AbsoluteDate extrapDate = new AbsoluteDate(initDate, delta_t);

        SpacecraftState finalOrbit = extrapolator.propagate(extrapDate);

        double a = finalOrbit.getA();
        // another way to compute n
        double n = Math.sqrt(finalOrbit.getMu()/Math.pow(a, 3));

        assertEquals(n*delta_t,
                     finalOrbit.getLM() - initialOrbit.getLM(),
                     Utils.epsilonTest * Math.max(100.,Math.abs(n*delta_t)));
        assertEquals(MathUtils.normalizeAngle(finalOrbit.getLM(),initialOrbit.getLM()), initialOrbit.getLM(), Utils.epsilonAngle * Math.abs(initialOrbit.getLM()));

        assertEquals(finalOrbit.getA(), initialOrbit.getA(), Utils.epsilonTest * initialOrbit.getA());
        assertEquals(finalOrbit.getE(), initialOrbit.getE(), Utils.epsilonE * initialOrbit.getE());
        assertEquals(MathUtils.normalizeAngle(finalOrbit.getI(),initialOrbit.getI()), initialOrbit.getI(), Utils.epsilonAngle * Math.abs(initialOrbit.getI()));

    }


    public void testPropagatedCartesian() throws OrekitException {

        // Definition of initial conditions with position and velocity
        //------------------------------------------------------------
        Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        double mu = 3.9860047e14;

        AbsoluteDate initDate = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 584.);
        Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                  Frame.getJ2000(), initDate, mu);

        // Extrapolator definition
        // -----------------------
        KeplerianPropagator extrapolator = new KeplerianPropagator(new SpacecraftState(initialOrbit));

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        double delta_t = 100000.0; // extrapolation duration in seconds
        AbsoluteDate extrapDate = new AbsoluteDate(initDate, delta_t);

        SpacecraftState finalOrbit = extrapolator.propagate(extrapDate);


        // computation of (M final - M initial) with another method
        double a = finalOrbit.getA();
        // another way to compute n
        double n = Math.sqrt(finalOrbit.getMu()/Math.pow(a, 3));

        assertEquals(n * delta_t,
                     finalOrbit.getLM() - initialOrbit.getLM(),
                     Utils.epsilonAngle);

        // computation of M final orbit
        double LM = finalOrbit.getLE()
        - finalOrbit.getEquinoctialEx()*Math.sin(finalOrbit.getLE())
        + finalOrbit.getEquinoctialEy()*Math.cos(finalOrbit.getLE());

        assertEquals(LM , finalOrbit.getLM() , Utils.epsilonAngle);

        // test of tan ((LE - Lv)/2) :
        assertEquals(Math.tan((finalOrbit.getLE() - finalOrbit.getLv())/2.),
                     tangLEmLv(finalOrbit.getLv(),finalOrbit.getEquinoctialEx(),finalOrbit.getEquinoctialEy()),
                     Utils.epsilonAngle);

        // test of evolution of M vs E: LM = LE - ex*sin(LE) + ey*cos(LE)
        // with ex and ey the same for initial and final orbit
        double deltaM = finalOrbit.getLM() - initialOrbit.getLM();
        double deltaE = finalOrbit.getLE() - initialOrbit.getLE();
        double delta  = finalOrbit.getEquinoctialEx() * (Math.sin(finalOrbit.getLE()) - Math.sin(initialOrbit.getLE()))
        - finalOrbit.getEquinoctialEy() * (Math.cos(finalOrbit.getLE()) - Math.cos(initialOrbit.getLE()));

        assertEquals(deltaM, deltaE - delta, Utils.epsilonAngle);

        // the orbital elements except for Mean/True/Excentric latitude arguments are the same
        assertEquals(finalOrbit.getA(), initialOrbit.getA(), Utils.epsilonTest * initialOrbit.getA());
        assertEquals(finalOrbit.getEquinoctialEx(), initialOrbit.getEquinoctialEx(), Utils.epsilonE);
        assertEquals(finalOrbit.getEquinoctialEy(), initialOrbit.getEquinoctialEy(), Utils.epsilonE);
        assertEquals(finalOrbit.getHx(), initialOrbit.getHx(), Utils.epsilonAngle);
        assertEquals(finalOrbit.getHy(), initialOrbit.getHy(), Utils.epsilonAngle);

        // for final orbit
        double ex = finalOrbit.getEquinoctialEx();
        double ey = finalOrbit.getEquinoctialEy();
        double hx = finalOrbit.getHx();
        double hy = finalOrbit.getHy();
        double LE = finalOrbit.getLE();

        double ex2 = ex*ex;
        double ey2 = ey*ey;
        double hx2 = hx*hx;
        double hy2 = hy*hy;
        double h2p1 = 1. + hx2 + hy2;
        double beta = 1. / (1. + Math.sqrt(1. - ex2 - ey2));

        double x3 = -ex + (1.- beta*ey2)*Math.cos(LE) + beta*ex*ey*Math.sin(LE);
        double y3 = -ey + (1. -beta*ex2)*Math.sin(LE) + beta*ex*ey*Math.cos(LE);

        Vector3D U = new Vector3D((1. + hx2 - hy2)/ h2p1,
                                  (2.*hx*hy)/h2p1,
                                  (-2.*hy)/h2p1);

        Vector3D V = new Vector3D((2.*hx*hy)/ h2p1,
                                  (1.- hx2+ hy2)/h2p1,
                                  (2.*hx)/h2p1);

        Vector3D r = new Vector3D(finalOrbit.getA(),(new Vector3D(x3,U,y3,V)));

        assertEquals(finalOrbit.getPVCoordinates().getPosition().getNorm(), r.getNorm(), Utils.epsilonTest * r.getNorm());

    }

    public void testPropagatedKeplerian() throws OrekitException {

        // Definition of initial conditions with keplerian parameters
        //-----------------------------------------------------------
        AbsoluteDate initDate = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 584.);
        Orbit initialOrbit = new KeplerianOrbit(7209668.0, 0.5e-4, 1.7, 2.1, 2.9,
                                                6.2, KeplerianOrbit.TRUE_ANOMALY, 
                                                Frame.getJ2000(), initDate, mu);

        // Extrapolator definition
        // -----------------------
        KeplerianPropagator extrapolator = new KeplerianPropagator(new SpacecraftState(initialOrbit));

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        double delta_t = 100000.0; // extrapolation duration in seconds
        AbsoluteDate extrapDate = new AbsoluteDate(initDate, delta_t);

        SpacecraftState finalOrbit = extrapolator.propagate(extrapDate);


        // computation of (M final - M initial) with another method
        double a = finalOrbit.getA();
        // another way to compute n
        double n = Math.sqrt(finalOrbit.getMu()/Math.pow(a, 3));

        assertEquals(n * delta_t,
                     finalOrbit.getLM() - initialOrbit.getLM(),
                     Utils.epsilonAngle);

        // computation of M final orbit
        double LM = finalOrbit.getLE()
        - finalOrbit.getEquinoctialEx()*Math.sin(finalOrbit.getLE())
        + finalOrbit.getEquinoctialEy()*Math.cos(finalOrbit.getLE());

        assertEquals(LM , finalOrbit.getLM() , Utils.epsilonAngle);

        // test of tan ((LE - Lv)/2) :
        assertEquals(Math.tan((finalOrbit.getLE() - finalOrbit.getLv())/2.),
                     tangLEmLv(finalOrbit.getLv(),finalOrbit.getEquinoctialEx(),finalOrbit.getEquinoctialEy()),
                     Utils.epsilonAngle);

        // test of evolution of M vs E: LM = LE - ex*sin(LE) + ey*cos(LE)
        // with ex and ey the same for initial and final orbit
        double deltaM = finalOrbit.getLM() - initialOrbit.getLM();
        double deltaE = finalOrbit.getLE() - initialOrbit.getLE();
        double delta  = finalOrbit.getEquinoctialEx() * (Math.sin(finalOrbit.getLE()) - Math.sin(initialOrbit.getLE())) - finalOrbit.getEquinoctialEy() * (Math.cos(finalOrbit.getLE()) - Math.cos(initialOrbit.getLE()));

        assertEquals(deltaM, deltaE - delta, Utils.epsilonAngle);

        // the orbital elements except for Mean/True/Excentric latitude arguments are the same
        assertEquals(finalOrbit.getA(), initialOrbit.getA(), Utils.epsilonTest * initialOrbit.getA());
        assertEquals(finalOrbit.getEquinoctialEx(), initialOrbit.getEquinoctialEx(), Utils.epsilonE);
        assertEquals(finalOrbit.getEquinoctialEy(), initialOrbit.getEquinoctialEy(), Utils.epsilonE);
        assertEquals(finalOrbit.getHx(), initialOrbit.getHx(), Utils.epsilonAngle);
        assertEquals(finalOrbit.getHy(), initialOrbit.getHy(), Utils.epsilonAngle);

        // for final orbit
        double ex = finalOrbit.getEquinoctialEx();
        double ey = finalOrbit.getEquinoctialEy();
        double hx = finalOrbit.getHx();
        double hy = finalOrbit.getHy();
        double LE = finalOrbit.getLE();

        double ex2 = ex*ex;
        double ey2 = ey*ey;
        double hx2 = hx*hx;
        double hy2 = hy*hy;
        double h2p1 = 1. + hx2 + hy2;
        double beta = 1. / (1. + Math.sqrt(1. - ex2 - ey2));

        double x3 = -ex + (1.- beta*ey2)*Math.cos(LE) + beta*ex*ey*Math.sin(LE);
        double y3 = -ey + (1. -beta*ex2)*Math.sin(LE) + beta*ex*ey*Math.cos(LE);

        Vector3D U = new Vector3D((1. + hx2 - hy2)/ h2p1,
                                  (2.*hx*hy)/h2p1,
                                  (-2.*hy)/h2p1);

        Vector3D V = new Vector3D((2.*hx*hy)/ h2p1,
                                  (1.- hx2+ hy2)/h2p1,
                                  (2.*hx)/h2p1);

        Vector3D r = new Vector3D(finalOrbit.getA(),(new Vector3D(x3,U,y3,V)));

        assertEquals(finalOrbit.getPVCoordinates().getPosition().getNorm(), r.getNorm(), Utils.epsilonTest * r.getNorm());

    }

    private static double tangLEmLv(double Lv,double ex,double ey){
        // tan ((LE - Lv) /2)) =
        return (ey*Math.cos(Lv) - ex*Math.sin(Lv)) /
        (1 + ex*Math.cos(Lv) + ey*Math.sin(Lv) + Math.sqrt(1 - ex*ex - ey*ey));
    }

    public void setUp() {
        mu  = 3.9860047e14;
    }

    public void tearDown() {
        mu   = Double.NaN;
    }
    
    public static Test suite() {
        return new TestSuite(KeplerianPropagatorTest.class);
    }

}

