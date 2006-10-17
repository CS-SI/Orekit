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
   * @param orbit initial orbit
   * @param mu central acceleration coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public KeplerianPropagator(Orbit orbit, double mu) {
    this.initialDate = orbit.getDate();
    this.initialParameters = new EquinoctialParameters(orbit.getParameters(), mu);
    this.n = Math.sqrt(mu / initialParameters.getA()) / initialParameters.getA();
  }

  public Orbit getOrbit(AbsoluteDate date)
  throws PropagationException {
    
    // evaluation of LM = PA + RAAN + M at extrapolated time
        
    EquinoctialParameters extrapolated = new EquinoctialParameters(
    		 initialParameters.getA(), initialParameters.getEquinoctialEx(),
    		 initialParameters.getEquinoctialEy(), initialParameters.getHx(),
    		 initialParameters.getHy(), 
    		 initialParameters.getLM() + n * date.minus(initialDate) ,
    		 0, initialParameters.getFrame());
    
    return new Orbit(date, extrapolated);

  }

  /** Initial orbit date. */
  private AbsoluteDate initialDate;

  /** Initial orbit parameters. */
  private EquinoctialParameters initialParameters;

  /** Mean motion. */
  private double n;

}