package fr.cs.aerospace.orekit.perturbations;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;

import org.spaceroots.mantissa.geometry.Vector3D;
import org.spaceroots.mantissa.ode.DerivativeException;
import org.spaceroots.mantissa.ode.FirstOrderIntegrator;
import org.spaceroots.mantissa.ode.FixedStepHandler;
import org.spaceroots.mantissa.ode.GraggBulirschStoerIntegrator;
import org.spaceroots.mantissa.ode.IntegratorException;

import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.errors.PropagationException;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.frames.FrameSynchronizer;
import fr.cs.aerospace.orekit.frames.ITRF2000Frame;
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
    double mu = 0.3986004415e15;
    final AbsoluteDate date = new AbsoluteDate("2000-07-01T13:59:27.816" , UTCScale.getInstance());
    double i     = Math.toRadians(98.7);
    double omega = Math.toRadians(93.0);
    double OMEGA = Math.toRadians(15.0 * 22.5);
    OrbitalParameters op = new KeplerianParameters(7201009.7124401, 1e-3, i , omega, OMEGA, 
                                                   0, KeplerianParameters.MEAN_ANOMALY, Frame.getJ2000());
    Orbit orbit = new Orbit(date , op);       
     
    // creation of the force model
    FrameSynchronizer fSynch = new FrameSynchronizer(date);
    DrozinerAttractionModel droziner =
      new DrozinerAttractionModel(mu, new ITRF2000Frame(fSynch, true), 
                                  6378136.460, new double[] { 0, 1.082626e-3 },
                                  new double[0][], new double[0][]);
    
    double day = 86400; 
    
    // creation of the propagator
    FirstOrderIntegrator integrator = new GraggBulirschStoerIntegrator(1, day, 0, 1.0e-4);
    NumericalPropagator propagator = new NumericalPropagator(mu, integrator);
    propagator.addForceModel(droziner);
    
    propagator.propagate(orbit, new AbsoluteDate(date , 7 * day),
                         day, new SpotStepHandler(date, mu));
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
      double dot = Vector3D.dotProduct(sunPos , normal)
                 / (sunPos.getNorm() * normal.getNorm());
      if (! Double.isNaN(previous)) {
        assertEquals(previous, dot, 0.0003);
      }
      previous = dot;
    }

    private AbsoluteDate date;
    private double mu;
    private Sun sun;
    private double previous;

  }
  
  public void testEcksteinHechlerReference()
    throws ParseException, FileNotFoundException,
           OrekitException, DerivativeException, IntegratorException {

    // potential
    double mu = 3.9860047e14;
    double ae = 6.378137e6;
    double j2 = 1.08263e-3;
    double j3 = 0.0;//2.54e-6;
    double j4 = 0.0;//1.62e-6;
    double j5 = 0.0;//2.3e-7;
    double j6 = 0.0;//-5.5e-7;

    //  Definition of initial conditions with position and velocity
    AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000Epoch, 584.);
    Vector3D position = new Vector3D(3220103., 69623., 6449822.);
    Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);
    
    Orbit initialOrbit =
      new Orbit(date,
                new EquinoctialParameters(new PVCoordinates(position, velocity),Frame.getJ2000(), mu));
    
    // creation of the force model
    FrameSynchronizer fSynch = new FrameSynchronizer(date);
    DrozinerAttractionModel droziner =
      new DrozinerAttractionModel(mu, new ITRF2000Frame(fSynch, true),
                                  ae, new double[] { 0, j2, j3, j4, j5, j6 },
                                  new double[0][], new double[0][]);
    
    double terPeriod = 86164; 
    
    // creation of the propagator
    FirstOrderIntegrator integrator = new GraggBulirschStoerIntegrator(1, terPeriod, 0, 10e-10);
    NumericalPropagator propagator = new NumericalPropagator(mu, integrator);
    propagator.addForceModel(droziner);
    
    AbsoluteDate finalDate = new AbsoluteDate(date , 50000);
    propagator.propagate(initialOrbit, finalDate, 20,
                         new EckStepHandler(initialOrbit, mu, ae,
                                            -j2, -j3, -j4, -j5, -j6));
    
  }
  
  private class EckStepHandler implements FixedStepHandler {
    
    private EckStepHandler(Orbit initialOrbit,  double mu, double ae,
                           double j2, double j3, double j4, double j5, double j6)
      throws FileNotFoundException, OrekitException {
      date = initialOrbit.getDate();
      this.mu = mu;
      referencePropagator =
        new EcksteinHechlerPropagator(initialOrbit, ae,
                                      mu, j2, j3, j4, j5, j6);
      try {
      w = new PrintWriter(new FileWriter(System.getProperty("user.home") + "/x.dat"));
      } catch (IOException ioe) {
        throw new OrekitException("", ioe);
      }
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

        Vector3D T = new Vector3D(1 / velEHP.getNorm() , velEHP);
        Vector3D cross = Vector3D.crossProduct(posEHP, velEHP);
        Vector3D W = new Vector3D(1 / cross.getNorm() , cross);
        Vector3D N = Vector3D.crossProduct(W,T);

        w.print(t + " ");
        w.print(Vector3D.dotProduct(dif , T) + " ");   
        w.print(Vector3D.dotProduct(dif , N) + " ");
        w.print(Vector3D.dotProduct(dif , W) + " ");
        w.println();
        w.flush();

      } catch (PropagationException e) {
        e.printStackTrace();
      }
    }
    private PrintWriter w;
    private AbsoluteDate date;
    private double mu;
    private EcksteinHechlerPropagator referencePropagator;
    
  }

  public static Test suite() {
    return new TestSuite(DrozinerAttractionModelTest.class);
  }

}


