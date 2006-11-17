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
   * @param initialState initial state
   * @param mu central acceleration coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public KeplerianPropagator(SpacecraftState initialState, double mu) {
    this.initialDate = initialState.getDate();
    this.initialParameters = new EquinoctialParameters(initialState.getParameters(), mu);
    this.mass = initialState.getMass();
    this.n = Math.sqrt(mu / initialParameters.getA()) / initialParameters.getA();
  }

  public SpacecraftState getSpacecraftState(AbsoluteDate date)
  throws PropagationException {
    
    // evaluation of LM = PA + RAAN + M at extrapolated time
        
    EquinoctialParameters extrapolated = new EquinoctialParameters(
    		 initialParameters.getA(), initialParameters.getEquinoctialEx(),
    		 initialParameters.getEquinoctialEy(), initialParameters.getHx(),
    		 initialParameters.getHy(), 
    		 initialParameters.getLM() + n * date.minus(initialDate) ,
    		 EquinoctialParameters.MEAN_LATITUDE_ARGUMENT, initialParameters.getFrame());
    
    return new SpacecraftState(new Orbit(date, extrapolated), mass);

  }

  /** Initial orbit date. */
  private AbsoluteDate initialDate;

  /** Initial orbit parameters. */
  private EquinoctialParameters initialParameters;
  
  /** Initial mass. */
  private double mass;

  /** Mean motion. */
  private double n;

}