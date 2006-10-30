package fr.cs.aerospace.orekit.perturbations;

import java.io.FileNotFoundException;
import java.text.ParseException;

import org.spaceroots.mantissa.geometry.Rotation;
import org.spaceroots.mantissa.geometry.Vector3D;
import org.spaceroots.mantissa.ode.DerivativeException;
import org.spaceroots.mantissa.ode.FixedStepHandler;
import org.spaceroots.mantissa.ode.GraggBulirschStoerIntegrator;
import org.spaceroots.mantissa.ode.IntegratorException;

import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.errors.PropagationException;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.frames.FrameSynchronizer;
import fr.cs.aerospace.orekit.frames.ITRF2000Frame;
import fr.cs.aerospace.orekit.frames.SynchronizedFrame;
import fr.cs.aerospace.orekit.frames.Transform;
import fr.cs.aerospace.orekit.models.bodies.Sun;
import fr.cs.aerospace.orekit.orbits.EquinoctialParameters;
import fr.cs.aerospace.orekit.orbits.KeplerianParameters;
import fr.cs.aerospace.orekit.orbits.Orbit;
import fr.cs.aerospace.orekit.orbits.OrbitalParameters;
import fr.cs.aerospace.orekit.propagation.EcksteinHechlerPropagator;
import fr.cs.aerospace.orekit.propagation.NumericalPropagator;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.time.UTCScale;
import fr.cs.aerospace.orekit.utils.PVCoordinates;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class DrozinerAttractionModelTest extends TestCase {
  
  public void testHelioSynchronous()
    throws ParseException, FileNotFoundException,
           OrekitException, DerivativeException, IntegratorException {

    // initialization
    AbsoluteDate date = new AbsoluteDate("2000-07-01T13:59:27.816" , UTCScale.getInstance());
    Transform itrfToJ2000  = itrf2000.getTransformTo(Frame.getJ2000(), date);
    Vector3D pole          = itrfToJ2000.transformVector(Vector3D.plusK);
    Frame poleAligned      = new Frame(Frame.getJ2000(),
                                       new Transform(new Rotation(pole, Vector3D.plusK)),
                                       "pole aligned");

    double i     = Math.toRadians(98.7);
    double omega = Math.toRadians(93.0);
    double OMEGA = Math.toRadians(15.0 * 22.5);
    OrbitalParameters op = new KeplerianParameters(7201009.7124401, 1e-3, i , omega, OMEGA, 
                                                   0, KeplerianParameters.MEAN_ANOMALY,
                                                   poleAligned);
    Orbit orbit = new Orbit(date , op);

    propagator.addForceModel(new DrozinerAttractionModel(mu, itrf2000,
                                                         6378136.460,
                                                         new double[][] { { 1.0 }, { 0.0 }, { c20 } },
                                                         new double[][] { { 0.0 }, { 0.0 }, { 0.0 } }));

    // let the step handler perform the test
    propagator.propagate(orbit, new AbsoluteDate(date , 7 * 86400),
                         86400, new SpotStepHandler(date, mu));

  }

  private static class SpotStepHandler implements FixedStepHandler {

    public SpotStepHandler(AbsoluteDate date, double mu) {
      this.date = date;
      this.mu   = mu;
      sun       = new Sun();
      previous  = Double.NaN;
    }

    public void handleStep(double t, double[] y, boolean isLastStep) {
      OrbitalParameters op =
        new EquinoctialParameters(y[0], y[1], y[2], y[3], y[4], y[5],
                                  EquinoctialParameters.TRUE_LATITUDE_ARGUMENT,
                                  Frame.getJ2000());
      Vector3D pos = op.getPVCoordinates(mu).getPosition();
      Vector3D vel = op.getPVCoordinates(mu).getVelocity();
      AbsoluteDate current = new AbsoluteDate(date, t);
      Vector3D sunPos = sun.getPosition(current , Frame.getJ2000());
      Vector3D normal = Vector3D.crossProduct(pos,vel);
      double angle = Vector3D.angle(sunPos , normal);
      if (! Double.isNaN(previous)) {
        assertEquals(previous, angle, 0.0005);
      }
      previous = angle;
    }

    private AbsoluteDate date;
    private double mu;
    private Sun sun;
    private double previous;

  }
  
  public void testEcksteinHechlerReference()
    throws ParseException, FileNotFoundException,
           OrekitException, DerivativeException, IntegratorException {

    //  Definition of initial conditions with position and velocity
    AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000Epoch, 584.);
    Vector3D position = new Vector3D(3220103., 69623., 6449822.);
    Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);

    Transform itrfToJ2000  = itrf2000.getTransformTo(Frame.getJ2000(), date);
    Vector3D pole          = itrfToJ2000.transformVector(Vector3D.plusK);
    Frame poleAligned      = new Frame(Frame.getJ2000(),
                                       new Transform(new Rotation(pole, Vector3D.plusK)),
                                       "pole aligned");

    Orbit initialOrbit =
      new Orbit(date,
                new EquinoctialParameters(new PVCoordinates(position, velocity),
                                          poleAligned, mu));

    propagator.addForceModel(new DrozinerAttractionModel(mu, itrf2000, ae,
                                                         new double[][] {
                                                           { 1.0 }, { 0.0 }, { c20 }, { c30 },
                                                           { c40 }, { c50 }, { c60 },
                                                         },
                                                         new double[][] {
                                                           { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 },
                                                           { 0.0 }, { 0.0 }, { 0.0 },
                                                         }));
    
    // let the step handler perform the test
    propagator.propagate(initialOrbit, new AbsoluteDate(date , 50000), 20,
                         new EckStepHandler(initialOrbit));
    
  }
  
  private class EckStepHandler implements FixedStepHandler {
    
    private EckStepHandler(Orbit initialOrbit)
      throws FileNotFoundException, OrekitException {
      date = initialOrbit.getDate();
      referencePropagator =
        new EcksteinHechlerPropagator(initialOrbit, ae, mu, c20, c30, c40, c50, c60);
    }
    
    public void handleStep(double t, double[] y, boolean isLastStep) {
      try {
        OrbitalParameters op =
          new EquinoctialParameters(y[0], y[1], y[2], y[3], y[4], y[5],
                                    EquinoctialParameters.TRUE_LATITUDE_ARGUMENT,
                                    Frame.getJ2000());
        AbsoluteDate current = new AbsoluteDate(date, t);

        Orbit EHPOrbit   = referencePropagator.getOrbit(current);
        Vector3D posEHP  = EHPOrbit.getPVCoordinates(mu).getPosition();
        Vector3D posDROZ = op.getPVCoordinates(mu).getPosition();
        Vector3D velEHP  = EHPOrbit.getPVCoordinates(mu).getVelocity();
        Vector3D dif     = Vector3D.subtract(posEHP, posDROZ);

        Vector3D T = new Vector3D(1 / velEHP.getNorm(), velEHP);
        Vector3D W = Vector3D.crossProduct(posEHP, velEHP);
        W.normalizeSelf();
        Vector3D N = Vector3D.crossProduct(W, T);
//System.out.println(dif.getNorm());
        assertTrue(dif.getNorm() < 104);
        assertTrue(Math.abs(Vector3D.dotProduct(dif, T)) < 104);
        assertTrue(Math.abs(Vector3D.dotProduct(dif, N)) <  53);
        assertTrue(Math.abs(Vector3D.dotProduct(dif, W)) <  12);

      } catch (PropagationException e) {
        e.printStackTrace();
      }
    }
    private AbsoluteDate date;
    private EcksteinHechlerPropagator referencePropagator;
    
  }

  protected void setUp() {
    try {
      mu  =  3.986004415e+14;
      ae  =  6378136.460;
      c20 = -1.08262631303e-3;
      c30 =  2.53248017972e-6;
      c40 =  1.61994537014e-6;
      c50 =  2.27888264414e-7;
      c60 = -5.40618601332e-7;
      itrf2000 = new ITRF2000Frame(new FrameSynchronizer(), true);
      propagator =
        new NumericalPropagator(mu,
                                new GraggBulirschStoerIntegrator(1, 1000, 0, 1.0e-4));
    } catch (OrekitException oe) {
      fail(oe.getMessage());
    }
  }

  protected void tearDown() {
    itrf2000   = null;
    propagator = null;
  }

  public static Test suite() {
    return new TestSuite(DrozinerAttractionModelTest.class);
  }

  private double mu;
  private double ae;
  private double c20;
  private double c30;
  private double c40;
  private double c50;
  private double c60;

  private SynchronizedFrame   itrf2000;
  private NumericalPropagator propagator;

}


