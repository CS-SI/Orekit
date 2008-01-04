package fr.cs.orekit.propagation;

import fr.cs.orekit.Utils;

import junit.framework.*;
import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.errors.PropagationException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.orbits.EquinoctialParameters;
import fr.cs.orekit.orbits.KeplerianParameters;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.propagation.KeplerianPropagator;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.utils.PVCoordinates;

public class KeplerianPropagatorTest extends TestCase {

  public KeplerianPropagatorTest(String name) {
    super(name);
  }

  public void testSameDateCartesian() throws PropagationException {

     // Definition of initial conditions with position and velocity
      //------------------------------------------------------------
      Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
      Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
      double mu = 3.9860047e14;

      AbsoluteDate initDate = new AbsoluteDate(AbsoluteDate.J2000Epoch, 584.);
      Orbit initialOrbit =
        new Orbit(initDate,
                  new EquinoctialParameters(new PVCoordinates(position, velocity),Frame.getJ2000(), mu));

      // Extrapolator definition
      // -----------------------
      KeplerianPropagator extrapolator = new KeplerianPropagator(new SpacecraftState(initialOrbit), mu);

      // Extrapolation at the initial date
      // ---------------------------------
      double delta_t = 0.0; // extrapolation duration in seconds
      AbsoluteDate extrapDate = new AbsoluteDate(initDate, delta_t);

      SpacecraftState finalOrbit = extrapolator.getSpacecraftState(extrapDate);

      double a = finalOrbit.getA();
      // another way to compute n
      double n = Math.sqrt(mu/Math.pow(a, 3));

      assertEquals(n*delta_t,
                   finalOrbit.getLM() - initialOrbit.getLM(),
                   Utils.epsilonTest * Math.abs(n*delta_t));
      assertEquals(Utils.trimAngle(finalOrbit.getLM(),initialOrbit.getLM()), initialOrbit.getLM(), Utils.epsilonAngle * Math.abs(initialOrbit.getLM()));

      assertEquals(finalOrbit.getA(), initialOrbit.getA(), Utils.epsilonTest * initialOrbit.getA());
      assertEquals(finalOrbit.getE(), initialOrbit.getE(), Utils.epsilonE * initialOrbit.getE());
      assertEquals(Utils.trimAngle(finalOrbit.getI(),initialOrbit.getI()), initialOrbit.getI(), Utils.epsilonAngle * Math.abs(initialOrbit.getI()));

    }

  public void testSameDateKeplerian() throws PropagationException {
      // Definition of initial conditions with keplerian parameters
      //-----------------------------------------------------------
      AbsoluteDate initDate = new AbsoluteDate(AbsoluteDate.J2000Epoch, 584.);
      Orbit initialOrbit =
        new Orbit(initDate,
                  new KeplerianParameters(7209668.0, 0.5e-4, 1.7, 2.1, 2.9,
                                          6.2, KeplerianParameters.TRUE_ANOMALY, Frame.getJ2000()));
      double mu = 3.9860047e14;

      // Extrapolator definition
      // -----------------------
      KeplerianPropagator extrapolator = new KeplerianPropagator(new SpacecraftState(initialOrbit), mu);

      // Extrapolation at the initial date
      // ---------------------------------
      double delta_t = 0.0; // extrapolation duration in seconds
      AbsoluteDate extrapDate = new AbsoluteDate(initDate, delta_t);

      SpacecraftState finalOrbit = extrapolator.getSpacecraftState(extrapDate);

      double a = finalOrbit.getA();
      // another way to compute n
      double n = Math.sqrt(mu/Math.pow(a, 3));

      assertEquals(n*delta_t,
                   finalOrbit.getLM() - initialOrbit.getLM(),
                   Utils.epsilonTest * Math.max(100.,Math.abs(n*delta_t)));
      assertEquals(Utils.trimAngle(finalOrbit.getLM(),initialOrbit.getLM()), initialOrbit.getLM(), Utils.epsilonAngle * Math.abs(initialOrbit.getLM()));

      assertEquals(finalOrbit.getA(), initialOrbit.getA(), Utils.epsilonTest * initialOrbit.getA());
      assertEquals(finalOrbit.getE(), initialOrbit.getE(), Utils.epsilonE * initialOrbit.getE());
      assertEquals(Utils.trimAngle(finalOrbit.getI(),initialOrbit.getI()), initialOrbit.getI(), Utils.epsilonAngle * Math.abs(initialOrbit.getI()));

  }


  public void testPropagatedCartesian() throws PropagationException {

      // Definition of initial conditions with position and velocity
      //------------------------------------------------------------
      Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
      Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
      double mu = 3.9860047e14;

      AbsoluteDate initDate = new AbsoluteDate(AbsoluteDate.J2000Epoch, 584.);
      Orbit initialOrbit =
        new Orbit(initDate,
                  new EquinoctialParameters(new PVCoordinates(position, velocity),Frame.getJ2000(), mu));

      // Extrapolator definition
      // -----------------------
      KeplerianPropagator extrapolator = new KeplerianPropagator(new SpacecraftState(initialOrbit), mu);

      // Extrapolation at a final date different from initial date
      // ---------------------------------------------------------
      double delta_t = 100000.0; // extrapolation duration in seconds
      AbsoluteDate extrapDate = new AbsoluteDate(initDate, delta_t);

      SpacecraftState finalOrbit = extrapolator.getSpacecraftState(extrapDate);


      // computation of (M final - M initial) with another method
      double a = finalOrbit.getA();
      // another way to compute n
      double n = Math.sqrt(mu/Math.pow(a, 3));

      assertEquals(n * delta_t,
               finalOrbit.getLM() - initialOrbit.getLM(),
               Utils.epsilonAngle);

      // computation of M final orbit
      double LM = finalOrbit.getLE()
                  - finalOrbit.getEx()*Math.sin(finalOrbit.getLE())
                  + finalOrbit.getEy()*Math.cos(finalOrbit.getLE());

      assertEquals(LM , finalOrbit.getLM() , Utils.epsilonAngle);

      // test of tan ((LE - Lv)/2) :
      assertEquals(Math.tan((finalOrbit.getLE() - finalOrbit.getLv())/2.),
               tangLEmLv(finalOrbit.getLv(),finalOrbit.getEx(),finalOrbit.getEy()),
               Utils.epsilonAngle);

      // test of evolution of M vs E: LM = LE - ex*sin(LE) + ey*cos(LE)
      // with ex and ey the same for initial and final orbit
      double deltaM = finalOrbit.getLM() - initialOrbit.getLM();
      double deltaE = finalOrbit.getLE() - initialOrbit.getLE();
      double delta  = finalOrbit.getEx() * (Math.sin(finalOrbit.getLE()) - Math.sin(initialOrbit.getLE()))
                    - finalOrbit.getEy() * (Math.cos(finalOrbit.getLE()) - Math.cos(initialOrbit.getLE()));

      assertEquals(deltaM, deltaE - delta, Utils.epsilonAngle);

      // the orbital elements except for Mean/True/Excentric latitude arguments are the same
      assertEquals(finalOrbit.getA(), initialOrbit.getA(), Utils.epsilonTest * initialOrbit.getA());
      assertEquals(finalOrbit.getEx(), initialOrbit.getEx(), Utils.epsilonE);
      assertEquals(finalOrbit.getEy(), initialOrbit.getEy(), Utils.epsilonE);
      assertEquals(finalOrbit.getHx(), initialOrbit.getHx(), Utils.epsilonAngle);
      assertEquals(finalOrbit.getHy(), initialOrbit.getHy(), Utils.epsilonAngle);

      // for final orbit
      double ex = finalOrbit.getEx();
      double ey = finalOrbit.getEy();
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

      assertEquals(finalOrbit.getPVCoordinates(mu).getPosition().getNorm(), r.getNorm(), Utils.epsilonTest * r.getNorm());

    }

    public void testPropagatedKeplerian() throws PropagationException {

      // Definition of initial conditions with keplerian parameters
      //-----------------------------------------------------------
      AbsoluteDate initDate = new AbsoluteDate(AbsoluteDate.J2000Epoch, 584.);
      Orbit initialOrbit =
        new Orbit(initDate,
                  new KeplerianParameters(7209668.0, 0.5e-4, 1.7, 2.1, 2.9,
                                          6.2, KeplerianParameters.TRUE_ANOMALY, Frame.getJ2000()));
      double mu = 3.9860047e14;

      // Extrapolator definition
      // -----------------------
      KeplerianPropagator extrapolator = new KeplerianPropagator(new SpacecraftState(initialOrbit), mu);

      // Extrapolation at a final date different from initial date
      // ---------------------------------------------------------
      double delta_t = 100000.0; // extrapolation duration in seconds
      AbsoluteDate extrapDate = new AbsoluteDate(initDate, delta_t);

      SpacecraftState finalOrbit = extrapolator.getSpacecraftState(extrapDate);


      // computation of (M final - M initial) with another method
      double a = finalOrbit.getA();
      // another way to compute n
      double n = Math.sqrt(mu/Math.pow(a, 3));

      assertEquals(n * delta_t,
               finalOrbit.getLM() - initialOrbit.getLM(),
               Utils.epsilonAngle);

      // computation of M final orbit
      double LM = finalOrbit.getLE()
                  - finalOrbit.getEx()*Math.sin(finalOrbit.getLE())
                  + finalOrbit.getEy()*Math.cos(finalOrbit.getLE());

      assertEquals(LM , finalOrbit.getLM() , Utils.epsilonAngle);

      // test of tan ((LE - Lv)/2) :
      assertEquals(Math.tan((finalOrbit.getLE() - finalOrbit.getLv())/2.),
               tangLEmLv(finalOrbit.getLv(),finalOrbit.getEx(),finalOrbit.getEy()),
               Utils.epsilonAngle);

      // test of evolution of M vs E: LM = LE - ex*sin(LE) + ey*cos(LE)
      // with ex and ey the same for initial and final orbit
      double deltaM = finalOrbit.getLM() - initialOrbit.getLM();
      double deltaE = finalOrbit.getLE() - initialOrbit.getLE();
      double delta  = finalOrbit.getEx() * (Math.sin(finalOrbit.getLE()) - Math.sin(initialOrbit.getLE())) - finalOrbit.getEy() * (Math.cos(finalOrbit.getLE()) - Math.cos(initialOrbit.getLE()));

      assertEquals(deltaM, deltaE - delta, Utils.epsilonAngle);

      // the orbital elements except for Mean/True/Excentric latitude arguments are the same
      assertEquals(finalOrbit.getA(), initialOrbit.getA(), Utils.epsilonTest * initialOrbit.getA());
      assertEquals(finalOrbit.getEx(), initialOrbit.getEx(), Utils.epsilonE);
      assertEquals(finalOrbit.getEy(), initialOrbit.getEy(), Utils.epsilonE);
      assertEquals(finalOrbit.getHx(), initialOrbit.getHx(), Utils.epsilonAngle);
      assertEquals(finalOrbit.getHy(), initialOrbit.getHy(), Utils.epsilonAngle);

      // for final orbit
      double ex = finalOrbit.getEx();
      double ey = finalOrbit.getEy();
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

      assertEquals(finalOrbit.getPVCoordinates(mu).getPosition().getNorm(), r.getNorm(), Utils.epsilonTest * r.getNorm());

    }

  private static double tangLEmLv(double Lv,double ex,double ey){
    // tan ((LE - Lv) /2)) =
    return (ey*Math.cos(Lv) - ex*Math.sin(Lv)) /
           (1 + ex*Math.cos(Lv) + ey*Math.sin(Lv) + Math.sqrt(1 - ex*ex - ey*ey));
  }

  public static Test suite() {
    return new TestSuite(KeplerianPropagatorTest.class);
  }

}

