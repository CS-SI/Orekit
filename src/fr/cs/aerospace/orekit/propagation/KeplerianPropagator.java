package fr.cs.aerospace.orekit.propagation;

import fr.cs.aerospace.orekit.errors.PropagationException;
import fr.cs.aerospace.orekit.orbits.EquinoctialParameters;
import fr.cs.aerospace.orekit.orbits.Orbit;
import fr.cs.aerospace.orekit.time.AbsoluteDate;

/** Simple keplerian orbit extrapolator.
 * @author G. Prat
 * @version $Id$
 */
public class KeplerianPropagator implements Ephemeris {

  /** Build a new instance.
   * @param mu central acceleration coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public KeplerianPropagator(double mu) {
    this.mu = mu;
  }

  public Orbit getOrbit(AbsoluteDate date, Orbit orbit)
  throws PropagationException {
    
    // mean motion
    double a = orbit.getA(); 
    double n = Math.sqrt(mu / a) / a;
    
    // evaluation of LM = PA + RAAN + M at extrapolated time
    EquinoctialParameters extrapolated =
      new EquinoctialParameters(orbit.getParameters(), mu);
    extrapolated.setLM(extrapolated.getLM() + n * date.minus(orbit.getDate()));
    
    return new Orbit(date, extrapolated);

  }

  /** Central acceleration coefficient. */
  private double mu;
  
}