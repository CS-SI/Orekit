package fr.cs.orekit.propagation.numerical;

import org.apache.commons.math.ode.FixedStepHandler;

import fr.cs.orekit.attitudes.AttitudeKinematics;
import fr.cs.orekit.attitudes.AttitudeKinematicsProvider;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.orbits.EquinoctialParameters;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.orbits.OrbitalParameters;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.time.AbsoluteDate;

public abstract class OrekitFixedStepHandler implements FixedStepHandler {

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

    public abstract void handleStep(SpacecraftState currentState, boolean isLast);

    public abstract boolean requiresDenseOutput();

    public abstract void reset() ;

    public void handleStep(double t, double[] y, boolean isLast) {
        try {
            final OrbitalParameters op =
                new EquinoctialParameters(y[0], y[1], y[2], y[3], y[4], y[5],
                                          EquinoctialParameters.TRUE_LATITUDE_ARGUMENT,
                                          frame);
            final AbsoluteDate current = new AbsoluteDate(reference, t);

            final AttitudeKinematics ak =
                provider.getAttitudeKinematics(current, op.getPVCoordinates(mu), frame);
            final SpacecraftState state =
                new SpacecraftState(new Orbit(current, op), y[6], ak);
            handleStep(state, isLast);
        } catch (OrekitException e) {
            throw new RuntimeException(e.getLocalizedMessage());
        }

    }

}
