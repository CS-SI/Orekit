package fr.cs.aerospace.orekit.propagation;

import org.spaceroots.mantissa.ode.ContinuousOutputModel;

import fr.cs.aerospace.orekit.attitudes.AttitudeKinematicsProvider;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.errors.PropagationException;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.orbits.EquinoctialParameters;
import fr.cs.aerospace.orekit.orbits.Orbit;
import fr.cs.aerospace.orekit.propagation.BoundedEphemeris;
import fr.cs.aerospace.orekit.time.AbsoluteDate;

/** This class stores numerically integrated orbital parameters for
 * later retrieval.
 *
 * <p>Instances of this class are built and then must be filled with the results
 * provided by {@link NumericalPropagator} objects in order to allow random 
 * access to any intermediate state of the orbit throughout the integration range.
 * Numerically integrated orbits can therefore be used by algorithms that
 * need to wander around according to their own algorithm without cumbersome
 * tight link with the integrator.</p>
 * 
 * <p> This class handles a {@link ContinuousOutputModel} and can be very
 *  voluminous. Refer to {@link ContinuousOutputModel} for more information.</p>
 *
 * @see NumericalPropagator
 *
 * @version $Id$
 * @author M. Romero
 * @author L. Maisonobe
 */
public class IntegratedEphemeris implements BoundedEphemeris {

  /** Creates a new instance of IntegratedEphemeris wich must be 
   *  filled by the propagator. 
   */
  public IntegratedEphemeris() {
    isInitialized = false;
  }

  /** This method is called by the propagator.
   */
  protected void initialize(ContinuousOutputModel model, AbsoluteDate ref, Frame frame,
                            AttitudeKinematicsProvider akProvider, double mu) {
    this.model     = model;
    this.frame = frame;
    this.akProvider = akProvider;
    this.mu = mu;
    startDate = new AbsoluteDate(ref, model.getInitialTime());
    maxDate = new AbsoluteDate(ref, model.getFinalTime());
    if (maxDate.minus(startDate) < 0) {
      minDate = maxDate;
      maxDate = startDate;
    }
    else {
      minDate = startDate;
    }
    this.isInitialized = true;
  }

  /** Get the orbit at a specific date.
   * @param date desired date for the orbit
   * @return the {@link SpacecraftState} at the specified date and null if not initialized.
   * @exception PropagationException if the date is outside of the range
   */    
  public SpacecraftState getSpacecraftState(AbsoluteDate date)
  throws PropagationException {
    if(isInitialized) {
      model.setInterpolatedTime(date.minus(startDate));
      double[] state = model.getInterpolatedState();

      EquinoctialParameters eq = new EquinoctialParameters(state[0],state[1],state[2],
                                                           state[3], state[4],state[5], 2, frame);

      double mass = state[6];

      try {
        return new SpacecraftState(new Orbit(date , eq), mass, 
                                   akProvider.getAttitudeKinematics(date, eq.getPVCoordinates(mu), frame));
      } catch (OrekitException oe) {
        throw new PropagationException(oe.getMessage(), oe);
      }
    }
    else {
      return null;
    }
  }

  /** Get the first date of the range.
   * @return the first date of the range
   */
  public AbsoluteDate getMinDate() {
    return minDate;
  }

  /** Get the last date of the range.
   * @return the last date of the range
   */
  public AbsoluteDate getMaxDate() {
    return maxDate;
  }

  /** Central body gravitational constant. */
  private double mu;

  /** Attitude provider */
  private AttitudeKinematicsProvider akProvider;

  /** Start date of the integration (can be min or max). */
  private AbsoluteDate startDate;

  /** First date of the range. */
  private AbsoluteDate minDate;

  /** Last date of the range. */
  private AbsoluteDate maxDate;

  /** Underlying raw mathematical model. */
  private ContinuousOutputModel model;

  /** Frame */
  private Frame frame;

  private boolean isInitialized;

}
