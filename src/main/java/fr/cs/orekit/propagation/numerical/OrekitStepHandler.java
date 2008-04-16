package fr.cs.orekit.propagation.numerical;

import java.io.Serializable;

import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.StepHandler;
import org.apache.commons.math.ode.StepInterpolator;

import fr.cs.orekit.attitudes.AttitudeLaw;
import fr.cs.orekit.errors.PropagationException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.time.AbsoluteDate;

/** This class is a space-dynamics aware step handler.
 * 
 * <p>It mirrors the {@link org.apache.commons.math.ode.StepHandler
 * StepHandler} interface from <a href="http://commons.apache.org/math/"
 * commons-math</a> but provides a space-dynamics interface to the methods.</p>
 * 
 */
public abstract class OrekitStepHandler
    implements StepHandler, Serializable {

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
     * @param interpolator interpolator set up for the current step
     * @param isLast if true, this is the last integration step
     * @exception PropagationException if step cannot be handled
     */
    public abstract void handleStep(OrekitStepInterpolator interpolator, boolean isLast)
        throws PropagationException;

    /** {@inheritDoc} */
    public abstract boolean requiresDenseOutput();

    /** {@inheritDoc} */
    public abstract void reset() ;

    /** {@inheritDoc} */
    public void handleStep(StepInterpolator interpolator, boolean isLast)
        throws DerivativeException {
        try {
            final OrekitStepInterpolator orekitInterpolator =
                new OrekitStepInterpolator(reference, frame, mu, attitudeLaw, interpolator);
            handleStep(orekitInterpolator, isLast);
        } catch (PropagationException pe) {
            throw new DerivativeException(pe);
        }
    }

}
