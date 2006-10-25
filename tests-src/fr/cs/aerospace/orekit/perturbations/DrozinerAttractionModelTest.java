package fr.cs.aerospace.orekit.perturbations;

import java.io.FileNotFoundException;
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
import fr.cs.aerospace.orekit.utils.Angle;
import fr.cs.aerospace.orekit.utils.PVCoordinates;
import fr.cs.aerospace.orekit.utils.Vector;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class DrozinerAttractionModelTest extends TestCase {
  
  public void aaatestJ2SpotOrbit() throws ParseException, OrekitException, DerivativeException, IntegratorException, FileNotFoundException {
    
    // initialization
    spotDate = new AbsoluteDate("2000-07-01T13:59:27.816" , UTCScale.getInstance());
    double i = (98.7*Math.PI/180.0);
    double omega = (93*Math.PI/180.0);
    double OMEGA = (Math.PI*22.5/12.0);
    OrbitalParameters op = new KeplerianParameters(7200000, 1e-3, i , omega, OMEGA, 
                                                   0, KeplerianParameters.MEAN_ANOMALY, Frame.getJ2000());
    Orbit orbit = new Orbit(spotDate , op);       
     
    // creation of the force model
    FrameSynchronizer fSynch = new FrameSynchronizer(spotDate);
    double[][] C = new double[0][0];
    double[][] S = new double[0][0];
    double[] J = new double[2];
    J[1] = j2Eigen;
    DrozinerAttractionModel TAM =  new DrozinerAttractionModel(muEigen, new ITRF2000Frame(fSynch), 
                                                               eqRadiusEigen,
                                                               J, C, S);
    
    double terPeriod = 86164; 
    
    // creation of the propagator
    FirstOrderIntegrator integrator = new GraggBulirschStoerIntegrator(1, terPeriod, 0, 10e-10);
    NumericalPropagator calc = new NumericalPropagator(muEigen, integrator);
    calc.addForceModel(TAM);
    
    // Step Handler
    
    TAMStepHandler sh = new TAMStepHandler(TAMStepHandler.SPOT);
    AbsoluteDate finalDate = new AbsoluteDate(spotDate , 60*terPeriod);
    calc.propagate(orbit , finalDate, terPeriod, sh );
    
  }
  
  public void testJ2CloseFromEcksteinHechler() throws ParseException, OrekitException, DerivativeException, IntegratorException, FileNotFoundException {
    
    //  Definition of initial conditions with position and velocity
    Vector3D position = new Vector3D(3220103., 69623., 6449822.);
    Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);
    
    set();
    
    EHPDAte = new AbsoluteDate(AbsoluteDate.J2000Epoch, 584.);
    Orbit initialOrbit =
      new Orbit(EHPDAte,
                new EquinoctialParameters(new PVCoordinates(position, velocity),Frame.getJ2000(), mu));
    
    // creation of the force model
    FrameSynchronizer fSynch = new FrameSynchronizer(EHPDAte);
    double[][] C = new double[0][0];
    double[][] S = new double[0][0];
    double[] J = new double[6];
    J[1] = -j2;
    J[2] = j3;
    J[3] = j4;
    J[4] = j5;
    J[5] = -j6;

    DrozinerAttractionModel TAM =  new DrozinerAttractionModel(mu, new ITRF2000Frame(fSynch), 
                                                               ae,
                                                               J, C, S);
    
    double terPeriod = 86164; 
    
    // creation of the propagator
    FirstOrderIntegrator integrator = new GraggBulirschStoerIntegrator(1, terPeriod, 0, 10e-10);
    NumericalPropagator calc = new NumericalPropagator(mu, integrator);
    calc.addForceModel(TAM);
    
    // Step Handler
    
    TAMStepHandler sh = new TAMStepHandler(TAMStepHandler.ECK);
    AbsoluteDate finalDate = new AbsoluteDate(EHPDAte , 10000);
    calc.propagate(initialOrbit , finalDate, 20, sh );
    
  }
  
  private double muEigen = 0.3986004415e15;
  private double eqRadiusEigen = 6378136.460;
  private double j2Eigen = 1.08262631303e-3;
  private AbsoluteDate spotDate;
  private AbsoluteDate EHPDAte;
  
  private void set() {
    mu = 3.9860047e14;
    ae = 6.378137e6;
    j2 = -1.08263e-3;
//    j3 = 2.54e-6;
//    j4 = 1.62e-6;
//    j5 = 2.3e-7;
//    j6 = -5.5e-7;
  }

  private double mu;
  private double ae;
  private double j2;
  private double j3;
  private double j4;
  private double j5;
  private double j6;
  
  private class TAMStepHandler implements FixedStepHandler {
    
    public static final int SPOT = 1;
    public static final int ECK = 2;
    
    private Vector3D T;
    private Vector3D N;
    private Vector3D W;
    
    private int type;

    private Sun sun;
    
    private EcksteinHechlerPropagator EHP;
    
    private TAMStepHandler(int type) throws FileNotFoundException, OrekitException {
      this.type = type;
      if (type == SPOT) {
        this.sun = new Sun();
      }
      if (type == ECK) {

        Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);
                
        Orbit initialOrbit =
          new Orbit(EHPDAte,
                    new EquinoctialParameters(new PVCoordinates(position, velocity),Frame.getJ2000(), mu));
        
        // Extrapolator definition

        EHP = new EcksteinHechlerPropagator(initialOrbit, ae,
                                            mu, j2, j3, j4, j5, j6); 
      }
    }
    
    public void handleStep(double t, double[]y, boolean isLastStep) {
      
      if (type == SPOT) {
        OrbitalParameters op = new EquinoctialParameters(
                                                         y[0], y[1],y[2],y[3],y[4],y[5],
                                                         EquinoctialParameters.TRUE_LATITUDE_ARGUMENT,
                                                         Frame.getJ2000());
        
        Vector3D pos = op.getPVCoordinates(muEigen).getPosition();
        Vector3D vel = op.getPVCoordinates(muEigen).getVelocity();
        AbsoluteDate current = new AbsoluteDate(spotDate, t);
        Vector3D sunPos = sun.getPosition(current , Frame.getJ2000());
        Vector3D normal = Vector3D.crossProduct(pos,vel); 
        sunPos.normalizeSelf();
        normal.normalizeSelf();
        System.out.print(current + " pos : " + pos.getNorm() + "   ");
        System.out.println(Vector3D.dotProduct(sunPos , normal));
      }
      
      if (type == ECK) {
        OrbitalParameters op = new EquinoctialParameters(
                                                         y[0], y[1],y[2],y[3],y[4],y[5],
                                                         EquinoctialParameters.TRUE_LATITUDE_ARGUMENT,
                                                         Frame.getJ2000());
        AbsoluteDate current = new AbsoluteDate(EHPDAte, t);
        
        Orbit EHPOrbit = new Orbit();
        try {
          EHPOrbit = EHP.getOrbit(current);
        } catch (PropagationException e) {
          e.printStackTrace();
        }
        Vector3D posEHP = EHPOrbit.getPVCoordinates(mu).getPosition();
        Vector3D posDROZ = op.getPVCoordinates(mu).getPosition();
        Vector3D velEHP = EHPOrbit.getPVCoordinates(mu).getVelocity();
        Vector3D dif = Vector3D.subtract(posEHP,posDROZ);
 
        T = new Vector3D(1/velEHP.getNorm() , velEHP);
        Vector3D cross = Vector3D.crossProduct(posEHP , velEHP);
        W = new Vector3D(1/cross.getNorm() , cross );
        N = Vector3D.crossProduct(W,T);
        
      System.out.print(t + " ");
      System.out.print(Vector3D.dotProduct(dif , T) + " ");   
      System.out.print(Vector3D.dotProduct(dif , N) + " ");
      System.out.print(Vector3D.dotProduct(dif , W) + " ");
      System.out.println();
      
      }
    }
    
  }
  public static Test suite() {
    return new TestSuite(DrozinerAttractionModelTest.class);
  }
}


