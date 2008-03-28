package fr.cs.orekit.propagation.numerical;

import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.StepInterpolator;

import fr.cs.orekit.attitudes.AttitudeKinematicsProvider;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.orbits.EquinoctialParameters;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.orbits.OrbitalParameters;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.time.AbsoluteDate;


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

    public OrekitStepInterpolator(AbsoluteDate reference, Frame frame, double mu,
                                  AttitudeKinematicsProvider provider,
                                  StepInterpolator interpolator) {
        this.reference    = reference;
        this.frame        = frame;
        this.mu           = mu;
        this.provider     = provider;
        this.interpolator = interpolator;
    }

    public AbsoluteDate getCurrentDate() {
        return new AbsoluteDate(reference, interpolator.getCurrentTime());
    }

    public AbsoluteDate getInterpolatedDate() {
        return new AbsoluteDate(reference, interpolator.getInterpolatedTime());
    }

    public SpacecraftState getInterpolatedState() throws OrekitException {
        final double[] y = interpolator.getInterpolatedState();

        final OrbitalParameters op =
            new EquinoctialParameters(y[0], y[1], y[2], y[3], y[4], y[5],
                                      EquinoctialParameters.TRUE_LATITUDE_ARGUMENT,
                                      frame);
        final AbsoluteDate current = new AbsoluteDate(reference, interpolator.getCurrentTime());
        return new SpacecraftState(new Orbit(current, op), y[6],
                                   provider.getAttitudeKinematics(current, op.getPVCoordinates(mu), frame));
    }

    public AbsoluteDate getPreviousDate() {
        return new AbsoluteDate(reference, interpolator.getPreviousTime());
    }

    public boolean isForward() {
        return interpolator.isForward();
    }

    public void setInterpolatedDate(AbsoluteDate date) throws DerivativeException {
        interpolator.setInterpolatedTime(date.minus(reference));
    }

}
