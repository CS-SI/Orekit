package fr.cs.aerospace.orekit.propagation;

import org.spaceroots.mantissa.ode.ContinuousOutputModel;
import fr.cs.aerospace.orekit.errors.PropagationException;
import fr.cs.aerospace.orekit.orbits.Orbit;
import fr.cs.aerospace.orekit.propagation.BoundedEphemeris;
import fr.cs.aerospace.orekit.time.AbsoluteDate;

/**
 * This class stores numerically integrated orbital parameters for
 * later retrieval.
 *
 * <p>Instances of this class are built from the results provided
 * by {@link NumericalPropagator} objects in order to allow random access
 * to any intermediate state of the orbit throughout the integration range.
 * Numerically integrated orbits can therefore be used by algorithms that
 * need to wander around according to their own algorithm without cumbersome
 * tight link with the integrator.</p>
 *
 * @see NumericalPropagator
 *
 * @version $Id$
 * @author M. Romero
 * @author L. Maisonobe
 */
public class IntegratedEphemeris implements BoundedEphemeris {

  /** Creates a new instance of IntegratedEphemeris.
   *  @param model the {@link ContinuousOutputModel} to handle
   *  @param epoch reference epoch for the model
   */
  public IntegratedEphemeris(ContinuousOutputModel model, AbsoluteDate epoch) {

    this.model     = model;
    this.epoch.reset(epoch);

    startDate.reset(epoch, model.getInitialTime());
    endDate.reset(epoch, model.getFinalTime());
    if (endDate.minus(startDate) < 0) {
      AbsoluteDate tmpDate = endDate;
      endDate       = startDate;
      startDate     = tmpDate;
    }

  }

  /** Get the orbit at a specific date.
   * @param date desired date for the orbit
   * @return the orbit at the specified date
   * @exception PropagationException if the date is outside of the range
   */    
  public Orbit getOrbit(AbsoluteDate date)
  throws PropagationException {
    model.setInterpolatedTime(date.minus(epoch));
    double[] state = model.getInterpolatedState();

    Orbit orbit = new Orbit();
    orbit.setDate(date);
    orbit.getParameters().mapStateFromArray(0, state);

    return orbit;

  }

  /** Get the start date of the range.
   * @return the start date of the range
   */
  public AbsoluteDate getStartDate() {
    return startDate;
  }

  /** Get the end date of the range.
   * @return the end date of the range
   */
  public AbsoluteDate getEndDate() {
    return endDate;
  }
  /** Get the reference epoch for the model.
   * @return reference epoch for the model
   */
  public AbsoluteDate getEpoch() {
    return epoch;
  }

  /** Start date of the range. */
  private AbsoluteDate startDate;

  /** End date of the range. */
  private AbsoluteDate endDate;

  /** Reference epoch for the model. */
  private AbsoluteDate epoch;

  /** Underlying raw mathematical model. */
  private ContinuousOutputModel model;

}
