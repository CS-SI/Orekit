package fr.cs.aerospace.orekit.tutorials;

import java.text.ParseException;

import org.spaceroots.mantissa.ode.FirstOrderIntegrator;
import org.spaceroots.mantissa.ode.FixedStepHandler;
import org.spaceroots.mantissa.ode.GraggBulirschStoerIntegrator;

import fr.cs.aerospace.orekit.attitudes.AttitudeKinematics;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.forces.ForceModel;
import fr.cs.aerospace.orekit.forces.perturbations.CunninghamAttractionModel;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.frames.ITRF2000Frame;
import fr.cs.aerospace.orekit.orbits.EquinoctialParameters;
import fr.cs.aerospace.orekit.orbits.KeplerianParameters;
import fr.cs.aerospace.orekit.orbits.Orbit;
import fr.cs.aerospace.orekit.orbits.OrbitalParameters;
import fr.cs.aerospace.orekit.propagation.IntegratedEphemeris;
import fr.cs.aerospace.orekit.propagation.NumericalPropagator;
import fr.cs.aerospace.orekit.propagation.SpacecraftState;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.time.UTCScale;


public class NumericalPropagation {
 
  public static void numericalPropagation() throws ParseException, OrekitException {
    
    // physical constants :
    
    double mu =  3.9860064e+14; // gravitation coefficient
    double ae =  6378136.460; // equatorial radius in meter
    double c20 = -1.08262631303e-3; // J2 potential coefficent
    Frame itrf2000 = new ITRF2000Frame(new AbsoluteDate(), true); // terrestrial frame at an arbitrary date
    
    
    //  Initial state definition :    
    
      // parameters :
    double a = 24396159; // semi major axis in meters
    double e = 0.72831215; // eccentricity
    double i = Math.toRadians(7); // inclination
    double omega = Math.toRadians(180); // perigee argument
    double raan = Math.toRadians(261); // right ascention of ascending node
    double lv = 0; // mean anomaly
    
    double mass = 2500; // mass of the spacecraft in Kg
    
    AttitudeKinematics initialAK = new AttitudeKinematics(); // identity attitude
      // date and frame
    
    AbsoluteDate initialDate = 
      new AbsoluteDate("2004-01-01T23:30:00.000" , UTCScale.getInstance());
    
    Frame inertialFrame = Frame.getJ2000();
    
      // OREKIT objects construction:
    
    OrbitalParameters initialParameters =
      new KeplerianParameters(a, e, i, omega, raan, lv, 
                                KeplerianParameters.MEAN_ANOMALY, inertialFrame);
    
    Orbit initialOrbit = new Orbit(initialDate , initialParameters);       
    
    SpacecraftState initialState = new SpacecraftState(initialOrbit, mass, initialAK); 
  
    /* ***************** */
    /*   Extrapolation   */
    /* ***************** */
    
    // Integrator
       
    FirstOrderIntegrator integrator = new GraggBulirschStoerIntegrator(1, 1000, 0, 1.0e-8);
       // adaptive step integrator with a minimum step of 1 and a maximum step of 1000, and a relative precision of 1.0e-8
         
    NumericalPropagator propagator = new NumericalPropagator(mu, integrator);
    
    // Pertubative gravity field :
    
    double[][] c = new double[3][1];
    c[0][0] = 0.0;
    c[2][0] = c20;
    double[][] s = new double[3][1]; // potential coeffs arrays (only J2 is considered here)
  
    ForceModel cunningham = new CunninghamAttractionModel(mu, itrf2000, ae, c, s);
    propagator.addForceModel(cunningham);
    
    // propagation with storage of the results in an integrated ephemeris
    
    AbsoluteDate finalDate = new AbsoluteDate(initialDate, 500);
    IntegratedEphemeris ephemeris = new IntegratedEphemeris();
    SpacecraftState finalState = propagator.propagate(initialState, finalDate, ephemeris);
    System.out.println(" Final state  : " + 
                       finalState.getParameters());
    AbsoluteDate intermediateDate = new AbsoluteDate(initialDate, 214);
    SpacecraftState intermediateState = ephemeris.getSpacecraftState(intermediateDate);
    System.out.println("  intermediate state  :  " + 
                       intermediateState.getParameters());  
  }
  
  public static void numericalPropagationWithStepHandler() throws ParseException, OrekitException {
// physical constants :
    
    double mu =  3.9860064e+14; // gravitation coefficient
    double ae =  6378136.460; // equatorial radius in meter
    double c20 = -1.08262631303e-3; // J2 potential coefficent
    Frame itrf2000 = new ITRF2000Frame(new AbsoluteDate(), true); // terrestrial frame at an arbitrary date
    
    
    //  Initial state definition :    
    
      // parameters :
    double a = 24396159; // semi major axis in meters
    double e = 0.72831215; // eccentricity
    double i = Math.toRadians(7); // inclination
    double omega = Math.toRadians(180); // perigee argument
    double raan = Math.toRadians(261); // right ascention of ascending node
    double lv = 0; // mean anomaly
    
    double mass = 2500; // mass of the spacecraft in Kg
    
    AttitudeKinematics initialAK = new AttitudeKinematics(); // identity attitude
      // date and frame
    
    AbsoluteDate initialDate = 
      new AbsoluteDate("2004-01-01T23:30:00.000" , UTCScale.getInstance());
    
    Frame inertialFrame = Frame.getJ2000();
    
      // OREKIT objects construction:
    
    OrbitalParameters initialParameters =
      new KeplerianParameters(a, e, i, omega, raan, lv, 
                                KeplerianParameters.MEAN_ANOMALY, inertialFrame);
    
    Orbit initialOrbit = new Orbit(initialDate , initialParameters);       
    
    SpacecraftState initialState = new SpacecraftState(initialOrbit, ae, initialAK); 
  
    /* ***************** */
    /*   Extrapolation   */
    /* ***************** */
    
    // Integrator
       
    FirstOrderIntegrator integrator = new GraggBulirschStoerIntegrator(1, 1000, 0, 1.0e-8);
       // adaptive step integrator with a minimum step of 1 and a maximum step of 1000, and a relative precision of 1.0e-8
         
    NumericalPropagator propagator = new NumericalPropagator(mu, integrator);
    
    // Pertubative gravity field :
    
    double[][] c = new double[3][1];
    c[0][0] = 0.0;
    c[2][0] = c20;
    double[][] s = new double[3][1]; // potential coeffs arrays (only J2 is considered here)
  
    ForceModel cunningham = new CunninghamAttractionModel(mu, itrf2000, mass, c, s);
    propagator.addForceModel(cunningham);
    AbsoluteDate finalDate = new AbsoluteDate(initialDate, 500);
    
    SpacecraftState finalState = 
      propagator.propagate(initialState, finalDate, 100, new tutorialStepHandler());
    System.out.println(" Final state  : " + 
                       finalState.getParameters());
    }
  
    private static class tutorialStepHandler implements FixedStepHandler {

      private tutorialStepHandler() {
        //private constructor
      }
      
      public void handleStep(double t, double[] y, boolean isLast) {
        System.out.println(" step time : " + t);
        OrbitalParameters stepParameters = 
          new EquinoctialParameters(y[0], y[1], y[2], y[3], y[4], y[5],
                                    EquinoctialParameters.TRUE_LATITUDE_ARGUMENT,
                                    Frame.getJ2000());
        System.out.println(" step state : " + stepParameters);
        if (isLast) {
          System.out.println(" this was the last step ");
        }
      }
      
    }
  
  
}
