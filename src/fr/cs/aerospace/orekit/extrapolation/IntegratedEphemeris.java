package fr.cs.aerospace.orekit.extrapolation;

import org.spaceroots.mantissa.ode.StepHandler;
import org.spaceroots.mantissa.ode.ContinuousOutputModel;
import org.spaceroots.mantissa.ode.DerivativeException;
import org.spaceroots.mantissa.ode.StepInterpolator;

import fr.cs.aerospace.orekit.RDate;
import fr.cs.aerospace.orekit.Orbit;
import fr.cs.aerospace.orekit.extrapolation.BoundedEphemeris;
import fr.cs.aerospace.orekit.extrapolation.ExtrapolationException;

import java.util.Date;

/**
 * This class stores numerically integrated orbital parameters for
 * later retrieval.
 *
 * <p>Instances of this class are built by {@link NumericalExtrapolator}
 * objects in order to allow random access to any intermediate state of the
 * orbit throughout the integration range. Numerically integrated orbits can
 * therefore be used by algorithms that need to wander around according to their
 * own algorithm without cumbersome tight link with the integrator.</p>
 *
 * @see NumericalExtrapolator
 *
 * @version $Id$
 * @author M. Romero
 * @author L. Maisonobe
 */
public class IntegratedEphemeris implements BoundedEphemeris {
    
    /** start date of the range. */
    private RDate startDate;
    
    /** end date of the range. */
    private RDate endDate;
    
    /** underlying raw mathematical model. */
    private ContinuousOutputModel model;
    
    /** Creates a new instance of IntegratedEphemeris with null time range
     * and empty model.
     */
    public IntegratedEphemeris() {
        startDate = new RDate(RDate.J2000Epoch, 0.0);
        endDate   = new RDate(RDate.J2000Epoch, 0.0);
        model     = new ContinuousOutputModel();
    }
    
    /** Set the start and end dates from the given epoch and the underlying
     * raw mathematical model.
     * @param epoch reference epoch to use for all orbit dates
     */
    public void setDates(Date epoch) {
        startDate.reset(epoch, model.getInitialTime());
        endDate.reset(epoch, model.getFinalTime());
        if (endDate.minus(startDate) < 0) {
            RDate tmpDate = endDate;
            endDate       = startDate;
            startDate     = tmpDate;
        }
    }
    
    /** Get the orbit at a specific date.
     * @param date desired date for the orbit
     * @param orbit reference to the object to initialize (may be null)
     * @return the orbit at the specified date (reference to orbit if it was non null,
     * reference to a new object otherwise)
     * @exception ExtrapolationException if the date is outside of the range
     */    
    public Orbit getOrbit(RDate date, Orbit orbit)
      throws ExtrapolationException {
       try {

           model.setInterpolatedTime(date.getOffset());
           double[] state = model.getInterpolatedState();

           if (orbit == null) {
             orbit = new Orbit();
           }
           orbit.setDate(date);
           orbit.getParameters().mapStateFromArray(0, state);

           return orbit;
           
       } catch(IllegalArgumentException iae) {
          throw new ExtrapolationException(iae);
       }
    }
    
    
    /** Get the end date of the range.
     * @return the end date of the range
     */
    public RDate getEndDate() {
        return startDate;
    }
    
    /** Get the start date of the range.
     * @return the start date of the range
     */
    public RDate getStartDate() {
        return endDate;
    }
    
    /** Get the underlying raw continuous model.
     * @return underlying raw continuous model
     */
    public ContinuousOutputModel getModel() {
        return model;
    }
    
}
