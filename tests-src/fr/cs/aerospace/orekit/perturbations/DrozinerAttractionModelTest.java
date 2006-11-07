package fr.cs.aerospace.orekit.perturbations;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
import fr.cs.aerospace.orekit.utils.Vector;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class DrozinerAttractionModelTest extends TestCase {
  
  // rough test to determine if J2 alone creates heliosynchronism
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
                                                         new double[][] { { 0.0 }, { 0.0 }, { c20 } },
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
  // test the difference with the analytical extrapolator Eckstein Hechler
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
      try {
        w = new PrintWriter(new FileWriter(new File(new File(System.getProperty("user.home")), "drozi.dat")));
      } catch (IOException ioe) {
        ioe.printStackTrace(System.out);
        System.exit(1);
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

        Vector3D T = new Vector3D(1 / velEHP.getNorm(), velEHP);
        Vector3D W = Vector3D.crossProduct(posEHP, velEHP);
        W.normalizeSelf();
        Vector3D N = Vector3D.crossProduct(W, T);
        w.println(t + " " + dif.getNorm()
                  + " " + Vector3D.dotProduct(dif, T)
                  + " " + Vector3D.dotProduct(dif, N)
                  + " " + Vector3D.dotProduct(dif, W));
        w.flush();
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
    private PrintWriter w;
  }
  
  // test the difference with the Cunningham model
  public void testTesserealWithCunninghamReference()
  throws OrekitException, IOException, DerivativeException, IntegratorException, ParseException {
    //  initialization
    AbsoluteDate date = new AbsoluteDate("2000-07-01T13:59:27.816" , UTCScale.getInstance());
    double i     = Math.toRadians(98.7);
    double omega = Math.toRadians(93.0);
    double OMEGA = Math.toRadians(15.0 * 22.5);
    OrbitalParameters op = new KeplerianParameters(7201009.7124401, 1e-3, i , omega, OMEGA, 
                                                   0, KeplerianParameters.MEAN_ANOMALY,
                                                   Frame.getJ2000());
    Orbit orbit = new Orbit(date , op);
    
    propagator.addForceModel(new CunninghamAttractionModel(mu, itrf2000, ae,C, S));

    Orbit cunnOrb = propagator.propagate(orbit, new AbsoluteDate(date ,  86400));

    propagator.removeForceModels();
    
    propagator.addForceModel(new DrozinerAttractionModel(mu, itrf2000, ae,
                                                         C, S));

    Orbit drozOrb = propagator.propagate(orbit, new AbsoluteDate(date ,  86400));
    
    Vector3D dif = Vector3D.subtract(cunnOrb.getPVCoordinates(mu).getPosition(),drozOrb.getPVCoordinates(mu).getPosition());
    System.out.println(Vector.toString(dif));
    assertTrue(dif.getNorm() < 1.70e-4);
    assertTrue(Math.abs(dif.getX()) < 5.9e-5);
    assertTrue(Math.abs(dif.getY()) < 5.4e-5); 
    assertTrue(Math.abs(dif.getZ()) < 1.5e-4);
    

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

  private double[][] C = new double[][] {
      {  1.000000000000e+00 },
      { -1.863039013786e-09, -5.934448524722e-10 },
      { -1.082626313026e-03, -5.880684168557e-10,  5.454582196865e-06 },
      {  2.532480179720e-06,  5.372084926301e-06,  2.393880978120e-06,  1.908327022943e-06 },
      {  1.619945370141e-06, -1.608435522852e-06,  1.051465706331e-06,  2.972622682182e-06,
        -5.654946679590e-07 },
      {  2.278882644141e-07, -2.086346283172e-07,  2.162761961684e-06, -1.498655671702e-06,
        -9.794826452868e-07,  5.797035241535e-07 },
      { -5.406186013322e-07, -2.736882085330e-07,  1.754209863998e-07,  2.063640268613e-07,
        -3.101287736303e-07, -9.633248308263e-07,  3.414597413636e-08 }
    };
  private double[][] S  = new double[][] {
      {  0.000000000000e+00 },
      {  0.000000000000e+00,  1.953002572897e-10 },
      {  0.000000000000e+00,  3.277637296181e-09, -3.131184828481e-06 },
      {  0.000000000000e+00,  6.566367025901e-07, -1.637705321455e-06,  3.742073902553e-06 },
      {  0.000000000000e+00, -1.420694191113e-06,  1.987395414651e-06, -6.029325532200e-07,
         9.265045448070e-07 },
      {  0.000000000000e+00, -3.130219048314e-07, -1.072392243018e-06, -7.130099408898e-07,
         1.651623310985e-07, -2.220047616004e-06 },
      {  0.000000000000e+00,  9.562397128532e-08, -1.347688934659e-06,  3.220292843428e-08,
        -1.699735804354e-06, -1.934323349167e-06, -8.559943406892e-07 }
    };
  
  private SynchronizedFrame   itrf2000;
  private NumericalPropagator propagator;

}


