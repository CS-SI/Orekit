package fr.cs.orekit.propagation.numerical;

import java.io.Serializable;

import org.apache.commons.math.ode.FixedStepHandler;

import fr.cs.orekit.attitudes.Attitude;
import fr.cs.orekit.attitudes.AttitudeLaw;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.errors.PropagationException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.orbits.EquinoctialParameters;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.orbits.OrbitalParameters;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.time.AbsoluteDate;

/** This class is a space-dynamics aware fixed size step handler.
 * 
 * <p>It mirrors the {@link org.apache.commons.math.ode.FixedStepHandler
 * FixedStepHandler} interface from <a href="http://commons.apache.org/math/"
 * commons-math</a> but provides a space-dynamics interface to the methods.</p>
 * 
 */
public abstract class OrekitFixedStepHandler
    implements FixedStepHandler, Serializable {

    /** Reference date. */
    private AbsoluteDate reference;

    /** Reference frame. */
    private Frame frame;

    /** Central body attraction coefficient. */
    private double mu;

    /** Attitude law. */
    private AttitudeLaw attitudeLaw;

    /** Initialize the handler.
     * @param reference reference date
     * @param frame reference frame
     * @param mu central body attraction coefficient
     * @param attitudeLaw attitude law
     */
    protected void initialize(AbsoluteDate reference, Frame frame, double mu,
                              AttitudeLaw attitudeLaw) {
        this.reference   = reference;
        this.frame       = frame;
        this.attitudeLaw = attitudeLaw;
        this.mu          = mu;
    }

    /** Handle the current step.
     * @param currentState current state at step time
     * @param isLast if true, this is the last integration step
     * @exception PropagationException if step cannot be handled
     */
    public abstract void handleStep(SpacecraftState currentState, boolean isLast)
        throws PropagationException;

    /** Check if the handler requires dense output
     * @return true if the handler requires dense output
     * @see org.apache.commons.math.ode.StepHandler#requiresDenseOutput()
     */
    public abstract boolean requiresDenseOutput();

    /** Reset the step handler.
     * Initialize the internal data as required before the first step is
     * handled.
     * @see org.apache.commons.math.ode.StepHandler#reset()
     */
    public abstract void reset() ;

    /**
     * Handle the last accepted step
     * @param t step time
     * @param y step state
     * @param isLast true if the step is the last one
     * @see org.apache.commons.math.ode.StepHandler#handleStep(org.apache.commons.math.ode.StepInterpolator, boolean)
     */
    public void handleStep(double t, double[] y, boolean isLast) {
        try {
            final OrbitalParameters op =
                new EquinoctialParameters(y[0], y[1], y[2], y[3], y[4], y[5],
                                          EquinoctialParameters.TRUE_LATITUDE_ARGUMENT,
                                          frame);
            final AbsoluteDate current = new AbsoluteDate(reference, t);
            final Attitude attitude =
                attitudeLaw.getState(current, op.getPVCoordinates(mu), frame);
            final SpacecraftState state =
                new SpacecraftState(new Orbit(current, op), y[6], attitude);
            handleStep(state, isLast);
        } catch (OrekitException e) {
            throw new RuntimeException(e.getLocalizedMessage());
        }

    }

}
