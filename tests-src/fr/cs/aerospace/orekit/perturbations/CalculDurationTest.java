package fr.cs.aerospace.orekit.perturbations;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import org.spaceroots.mantissa.geometry.Vector3D;
import org.spaceroots.mantissa.ode.ClassicalRungeKuttaIntegrator;
import org.spaceroots.mantissa.ode.DerivativeException;
import org.spaceroots.mantissa.ode.FixedStepHandler;
import org.spaceroots.mantissa.ode.GraggBulirschStoerIntegrator;
import org.spaceroots.mantissa.ode.IntegratorException;
import org.spaceroots.mantissa.ode.StepHandler;
import org.spaceroots.mantissa.ode.StepInterpolator;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.forces.perturbations.CunninghamAttractionModel;
import fr.cs.aerospace.orekit.forces.perturbations.DrozinerAttractionModel;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.frames.ITRF2000Frame;
import fr.cs.aerospace.orekit.orbits.EquinoctialParameters;
import fr.cs.aerospace.orekit.orbits.KeplerianParameters;
import fr.cs.aerospace.orekit.orbits.Orbit;
import fr.cs.aerospace.orekit.orbits.OrbitalParameters;
import fr.cs.aerospace.orekit.propagation.NumericalPropagator;
import fr.cs.aerospace.orekit.propagation.SpacecraftState;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.time.UTCScale;
import fr.cs.aerospace.orekit.utils.Vector;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class CalculDurationTest extends TestCase {
  
  public void aatestMeanDuration()
    throws OrekitException, IOException, DerivativeException, IntegratorException, ParseException {
    //  initialization
    AbsoluteDate date = new AbsoluteDate("2000-07-09T13:59:27.816" , UTCScale.getInstance());
    double i     = Math.toRadians(98.7);
    double omega = Math.toRadians(93.0);
    double OMEGA = Math.toRadians(15.0 * 22.5);
    OrbitalParameters op = new KeplerianParameters(7201009.7124401, 1e-3, i , omega, OMEGA, 
                                                   0, KeplerianParameters.MEAN_ANOMALY,
                                                   Frame.getJ2000());
    Orbit orbit = new Orbit(date , op);
    double result = 0;
    propagator.addForceModel(new CunninghamAttractionModel(mu, itrf2000, ae,C, S));
    for (int k = 1; k<=10; k++) {
      double start = System.currentTimeMillis();
      propagator.propagate(new SpacecraftState(orbit), new AbsoluteDate(date ,  4*86400));
       result += System.currentTimeMillis()-start;
    }
    
    result = result/10;
        
    System.out.println(result);

  }

  public void testMassPrecisionNormHandler()
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
    propagator =
      new NumericalPropagator(mu,
//                              new GraggBulirschStoerIntegrator(1, 1000,  0, 1.0e-8), true);
                                 new ClassicalRungeKuttaIntegrator(100));
                              propagator.addForceModel(new DrozinerAttractionModel(mu, itrf2000, ae,
                                                         C, S));

    SpacecraftState massOrb = propagator.propagate(new SpacecraftState(orbit),
            new AbsoluteDate(date ,  86400), new MassHandler(orbit));
    propagator =
      new NumericalPropagator(mu,
//                              new GraggBulirschStoerIntegrator(1, 1000, 0, 1.0e-8), false);
                              new ClassicalRungeKuttaIntegrator(100));
                              propagator.addForceModel(new DrozinerAttractionModel(mu, itrf2000, ae,
                                                         C, S));
  
    SpacecraftState nomassOrb = propagator.propagate(new SpacecraftState(orbit),
            new AbsoluteDate(date ,  86400), new NoMassHandler(orbit));
    System.out.println(massOrb.getMass());
    Vector3D dif = Vector3D.subtract(massOrb.getPVCoordinates(mu).getPosition(),nomassOrb.getPVCoordinates(mu).getPosition());
    System.out.println(Vector.toString(dif));
  }
  
private class MassHandler implements StepHandler {
    
    private MassHandler(Orbit initialOrbit)
      throws FileNotFoundException, OrekitException {
      w = new PrintWriter(new FileOutputStream("/home/fab/mass.dat"));
      c = 0;
    }
    int c;
    private PrintWriter w;

    public void handleStep(StepInterpolator inter, boolean isLastStep) throws DerivativeException {
      double[] y = inter.getInterpolatedState();
      c++;
      OrbitalParameters op =
        new EquinoctialParameters(y[0], y[1], y[2], y[3], y[4], y[5],
                                  EquinoctialParameters.TRUE_LATITUDE_ARGUMENT,
                                  Frame.getJ2000());
      Vector3D pos = op.getPVCoordinates(mu).getPosition();
      w.println(c + " " + inter.getCurrentTime() + "  " +  pos.getNorm());
      if(isLastStep) {
        w.close();
      }
    }

    public boolean requiresDenseOutput() {
      return true;
    }

    public void reset() {
    }
  }
  
private class NoMassHandler implements StepHandler {
  
  private NoMassHandler(Orbit initialOrbit)
    throws FileNotFoundException, OrekitException {
    w = new PrintWriter(new FileOutputStream("/home/fab/nomass.dat"));
    c = 0;
  }
  int c;
  private PrintWriter w;

  public void handleStep(StepInterpolator inter, boolean isLastStep) throws DerivativeException {
    double[] y = inter.getInterpolatedState();
    c++;
    OrbitalParameters op =
      new EquinoctialParameters(y[0], y[1], y[2], y[3], y[4], y[5],
                                EquinoctialParameters.TRUE_LATITUDE_ARGUMENT,
                                Frame.getJ2000());
    Vector3D pos = op.getPVCoordinates(mu).getPosition();
    w.println(c + " " + inter.getCurrentTime() + "  " +  pos.getNorm());
    if(isLastStep) {
      w.close();
    }
  }

  public boolean requiresDenseOutput() {
    return true;
  }

  public void reset() {
  }
} 
  
  public void testMassPrecisionFixedHandler()
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
  propagator =
    new NumericalPropagator(mu,
                            new GraggBulirschStoerIntegrator(1, 1000, 0, 1.0e-8));
  propagator.addForceModel(new DrozinerAttractionModel(mu, itrf2000, ae,
                                                       C, S));

  SpacecraftState massOrb = propagator.propagate(new SpacecraftState(orbit),
          new AbsoluteDate(date ,  86400), 100 ,new MassStepHandler(orbit));
  propagator =
    new NumericalPropagator(mu,
                            new GraggBulirschStoerIntegrator(1, 1000, 0, 1.0e-8));
  propagator.addForceModel(new DrozinerAttractionModel(mu, itrf2000, ae,
                                                       C, S));

  SpacecraftState nomassOrb = propagator.propagate(new SpacecraftState(orbit),
          new AbsoluteDate(date ,  86400),100 ,new noMassStepHandler(orbit));
  
  Vector3D dif = Vector3D.subtract(massOrb.getPVCoordinates(mu).getPosition(),nomassOrb.getPVCoordinates(mu).getPosition());
  System.out.println(Vector.toString(dif));
}

private class noMassStepHandler implements FixedStepHandler {
    
    private noMassStepHandler(Orbit initialOrbit)
      throws FileNotFoundException, OrekitException {
      w = new PrintWriter(new FileOutputStream("/home/fab/nomassfixed.dat"));
      }
    
    public void handleStep(double t, double[] y, boolean isLastStep) {

      OrbitalParameters op =
        new EquinoctialParameters(y[0], y[1], y[2], y[3], y[4], y[5],
                                  EquinoctialParameters.TRUE_LATITUDE_ARGUMENT,
                                  Frame.getJ2000());
      Vector3D pos = op.getPVCoordinates(mu).getPosition();
      w.println(t + "  " +  pos.getNorm());
      if(isLastStep) {
        w.close();
      }
    }
    private PrintWriter w;
    
  }
private class MassStepHandler implements FixedStepHandler {
  
  private MassStepHandler(Orbit initialOrbit)
    throws FileNotFoundException, OrekitException {
    w = new PrintWriter(new FileOutputStream("/home/fab/massfixed.dat"));
    }
  
  public void handleStep(double t, double[] y, boolean isLastStep) {

    OrbitalParameters op =
      new EquinoctialParameters(y[0], y[1], y[2], y[3], y[4], y[5],
                                EquinoctialParameters.TRUE_LATITUDE_ARGUMENT,
                                Frame.getJ2000());
    Vector3D pos = op.getPVCoordinates(mu).getPosition();
    w.println(t + "  " +  pos.getNorm());
    if(isLastStep) {
      w.close();
    }
  }
  private PrintWriter w;
  
}

  protected void setUp() {
    try {
      itrf2000 = new ITRF2000Frame(new AbsoluteDate(), true);      
    } catch (OrekitException oe) {
      fail(oe.getMessage());
    }
  }

  protected void tearDown() {
    itrf2000   = null;
    propagator = null;
  }
  
  private Frame   itrf2000;
  private NumericalPropagator propagator;
  
  private double mu  =  3.986004415e+14;
  private double ae  =  6378136.460;

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

  public static Test suite() {
    return new TestSuite(CalculDurationTest.class);
  }
}
