package fr.cs.orekit.propagation.numerical;

import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.StepHandler;
import org.apache.commons.math.ode.StepInterpolator;

import fr.cs.orekit.attitudes.AttitudeKinematicsProvider;
import fr.cs.orekit.errors.PropagationException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.time.AbsoluteDate;

public abstract class OrekitStepHandler implements StepHandler {

    private AbsoluteDate reference;
    private Frame frame;
    private AttitudeKinematicsProvider provider;
    private double mu;

    protected void initialize(AbsoluteDate reference, Frame frame, double mu,
                              AttitudeKinematicsProvider provider) {
        this.reference = reference;
        this.frame     = frame;
        this.provider  = provider;
        this.mu        = mu;
    }

    public abstract void handleStep(OrekitStepInterpolator interpolator, boolean isLast)
        throws PropagationException;

    public abstract boolean requiresDenseOutput();

    public abstract void reset() ;

    public void handleStep(StepInterpolator interpolator, boolean isLast)
        throws DerivativeException {
        try {
            handleStep(new OrekitStepInterpolator(reference, frame, mu, provider, interpolator),
                       isLast);
        } catch (PropagationException pe) {
            throw new DerivativeException(pe);
        }
    }

}
