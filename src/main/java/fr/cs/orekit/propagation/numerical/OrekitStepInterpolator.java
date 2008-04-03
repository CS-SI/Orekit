package fr.cs.orekit.propagation.numerical;

import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.StepInterpolator;

import fr.cs.orekit.attitudes.AttitudeKinematicsProvider;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.errors.PropagationException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.orbits.EquinoctialParameters;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.orbits.OrbitalParameters;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.time.AbsoluteDate;

/** This class is a space-dynamics aware step interpolator.
 * 
 * <p>It mirrors the {@link org.apache.commons.math.ode.StepInterpolator
 * StepInterpolator} interface from <a href="http://commons.apache.org/math/"
 * commons-math</a> but provides a space-dynamics interface to the methods.</p>
 * 
 */
public class OrekitStepInterpolator {

    /** Reference date. */
    private final AbsoluteDate reference;

    /** Reference frame. */
    private final Frame frame;

    /** Central body attraction coefficient. */
    private final double mu;

    /** Provider for attitude data. */
    private final AttitudeKinematicsProvider provider;

    /** Underlying non-space-dynamics aware interpolator. */
    private final StepInterpolator interpolator;

    /** Build an interpolator.
     * @param reference reference date
     * @param frame reference frame
     * @param mu central body attraction coefficient
     * @param provider attitude data provider
     * @param interpolator underlying non space dynamics interpolator
     */
    public OrekitStepInterpolator(AbsoluteDate reference, Frame frame, double mu,
                                  AttitudeKinematicsProvider provider,
                                  StepInterpolator interpolator) {
        this.reference    = reference;
        this.frame        = frame;
        this.mu           = mu;
        this.provider     = provider;
        this.interpolator = interpolator;
    }

    /** Get the current grid date.
     * @return current grid date
     */
    public AbsoluteDate getCurrentDate() {
        return new AbsoluteDate(reference, interpolator.getCurrentTime());
    }

    /** Get the previous grid date.
     * @return previous grid date
     */
    public AbsoluteDate getPreviousDate() {
        return new AbsoluteDate(reference, interpolator.getPreviousTime());
    }

    /** Get the interpolated date.
     * <p>If {@link #setInterpolatedDate(AbsoluteDate) setInterpolatedDate}
     * has not been called, the date returned is the same as  {@link
     * #getCurrentDate() getCurrentDate}.</p>
     * @return interpolated date
     * @see #setInterpolatedDate(AbsoluteDate)
     * @see #getInterpolatedState()
     */
    public AbsoluteDate getInterpolatedDate() {
        return new AbsoluteDate(reference, interpolator.getInterpolatedTime());
    }

    /** Set the interpolated date.
     * <p>It is possible to set the interpolation date outside of the current
     * step range, but accuracy will decrease as date is farther.</p>
     * @param date interpolated date to set
     * @exception PropagationException if underlying interpolator cannot handle
     * the date
     * @see #getInterpolatedDate()
     * @see #getInterpolatedState()
     */
    public void setInterpolatedDate(AbsoluteDate date) throws PropagationException {
        try {
            interpolator.setInterpolatedTime(date.minus(reference));
        } catch (DerivativeException de) {
            throw new PropagationException(de.getMessage(), de);
        }
    }

    /** Get the interpolated state.
     * @return interpolated state at the current interpolation date
     * @exception OrekitException if state cannot be interpolated or converted
     * @see #getInterpolatedDate()
     * @see #setInterpolatedDate(AbsoluteDate)
     */
    public SpacecraftState getInterpolatedState() throws OrekitException {
        final double[] y = interpolator.getInterpolatedState();
        final OrbitalParameters op =
            new EquinoctialParameters(y[0], y[1], y[2], y[3], y[4], y[5],
                                      EquinoctialParameters.TRUE_LATITUDE_ARGUMENT,
                                      frame);
        final AbsoluteDate current = new AbsoluteDate(reference, interpolator.getCurrentTime());
        return new SpacecraftState(new Orbit(current, op), y[6],
                                   provider.getAttitudeKinematics(current, op.getPVCoordinates(mu),
                                                                  frame));
    }

    /** Check is integration direction is forward in date.
     * @return true if integration is forward in date
     */
    public boolean isForward() {
        return interpolator.isForward();
    }

}
