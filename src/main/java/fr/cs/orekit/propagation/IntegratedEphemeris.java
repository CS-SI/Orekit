package fr.cs.orekit.propagation;

import org.apache.commons.math.ode.ContinuousOutputModel;

import fr.cs.orekit.attitudes.AttitudeKinematics;
import fr.cs.orekit.attitudes.AttitudeKinematicsProvider;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.errors.PropagationException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.orbits.EquinoctialParameters;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.propagation.BoundedEphemeris;
import fr.cs.orekit.time.AbsoluteDate;

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

    /** Serializable UID. */
    private static final long serialVersionUID = -6109078744629339488L;

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

    /** Creates a new instance of IntegratedEphemeris wich must be
     *  filled by the propagator.
     */
    public IntegratedEphemeris() {
        isInitialized = false;
    }

    /** Initialize the ephemeris propagator.
     * @param model interpolation model containing the state evolution
     * @param ref reference date
     * @param frame frame in which state has been integrated
     * @param akProvider provider for attitude
     * @param mu central body attraction coefficient
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
        } else {
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
        if (isInitialized) {
            model.setInterpolatedTime(date.minus(startDate));
            final double[] state = model.getInterpolatedState();

            final EquinoctialParameters eq =
                new EquinoctialParameters(state[0], state[1], state[2],
                                          state[3], state[4],state[5], 2, frame);
            final double mass = state[6];

            try {
                AttitudeKinematics ak =
                    akProvider.getAttitudeKinematics(date, eq.getPVCoordinates(mu), frame);
                return new SpacecraftState(new Orbit(date , eq), mass, ak);
            } catch (OrekitException oe) {
                throw new PropagationException(oe.getMessage(), oe);
            }
        } else {
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

}
