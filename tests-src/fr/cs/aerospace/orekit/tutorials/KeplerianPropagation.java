package fr.cs.aerospace.orekit.tutorials;

import java.text.ParseException;

import org.spaceroots.mantissa.ode.ClassicalRungeKuttaIntegrator;
import org.spaceroots.mantissa.ode.FirstOrderIntegrator;
import fr.cs.aerospace.orekit.attitudes.AttitudeKinematics;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.orbits.KeplerianParameters;
import fr.cs.aerospace.orekit.orbits.Orbit;
import fr.cs.aerospace.orekit.orbits.OrbitalParameters;
import fr.cs.aerospace.orekit.propagation.KeplerianPropagator;
import fr.cs.aerospace.orekit.propagation.NumericalPropagator;
import fr.cs.aerospace.orekit.propagation.SpacecraftState;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.time.UTCScale;

/** The aim of this tutorial is to manipulate spacecraft states, orbital parameters
 * and keplerian propagation
 */
public class KeplerianPropagation {

  public static void keplerianPropagation() throws ParseException, OrekitException {
    
    // physical constants :
    
    double mu =  3.9860064e+14; // gravitation coefficient
    
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
    
    // Simple Keplerian extrapolation
    
    KeplerianPropagator kepler = new KeplerianPropagator(initialState, mu);
    
    double deltaT = 1000; // extrapolation lenght in seconds
    
    AbsoluteDate finalDate = new AbsoluteDate(initialDate, deltaT);
    SpacecraftState finalState = kepler.getSpacecraftState(finalDate);
    
    System.out.println(" Final parameters with deltaT = +1000 s : " + 
                             finalState.getParameters());
    
    deltaT = -1000; // extrapolation lenght
    
    finalDate = new AbsoluteDate(initialDate, deltaT);
    finalState = kepler.getSpacecraftState(finalDate);
    
    System.out.println(" Final parameters with deltaT = -1000 s : " + 
                          finalState.getParameters());
    
    // numerical propagation with no perturbation (only keplerian movement)
             // we use a very simple integrator with a fixed step : Runge Kutta
    
    FirstOrderIntegrator integrator = new ClassicalRungeKuttaIntegrator(1); // the step is one second
    
    NumericalPropagator propagator = new NumericalPropagator(mu, integrator);
    
    finalState = propagator.propagate(initialState, finalDate);
    
    System.out.println(" Final parameters with deltaT = -1000 s : " + 
                       finalState.getParameters());
  }
  
}
