package fr.cs.orekit.propagation.numerical;

import java.io.Serializable;

import org.apache.commons.math.ode.FixedStepHandler;

import fr.cs.orekit.attitudes.Attitude;
import fr.cs.orekit.attitudes.AttitudeLaw;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.errors.PropagationException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.orbits.EquinoctialOrbit;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.time.AbsoluteDate;

/** This class is a space-dynamics aware fixed size step handler.
 *
 * <p>It mirrors the {@link org.apache.commons.math.ode.FixedStepHandler
 * FixedStepHandler} interface from <a href="http://commons.apache.org/math/">
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
    protected void initialize(// CHECKSTYLE: stop HiddenField check
                              final AbsoluteDate reference, final Frame frame,
                              final double mu, final AttitudeLaw attitudeLaw
                              // CHECKSTYLE: resume HiddenField check
                              ) {
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
    public abstract void handleStep(final SpacecraftState currentState, final boolean isLast)
        throws PropagationException;

    /** Check if the handler requires dense output.
     * @return true if the handler requires dense output
     * @see org.apache.commons.math.ode.StepHandler#requiresDenseOutput()
     */
    public abstract boolean requiresDenseOutput();

    /** Reset the step handler.
     * Initialize the internal data as required before the first step is
     * handled.
     * @see org.apache.commons.math.ode.StepHandler#reset()
     */
    public abstract void reset();

    /** {@inheritDoc}  */
    public void handleStep(final double t, final double[] y, final boolean isLast) {
        try {
            final AbsoluteDate current = new AbsoluteDate(reference, t);
            final Orbit orbit =
                new EquinoctialOrbit(y[0], y[1], y[2], y[3], y[4], y[5],
                                          EquinoctialOrbit.TRUE_LATITUDE_ARGUMENT,
                                          frame, current, mu);
            final Attitude attitude =
                attitudeLaw.getState(current, orbit.getPVCoordinates(), frame);
            final SpacecraftState state =
                new SpacecraftState(orbit, y[6], attitude);
            handleStep(state, isLast);
        } catch (OrekitException e) {
            throw new RuntimeException(e.getLocalizedMessage());
        }

    }

}
