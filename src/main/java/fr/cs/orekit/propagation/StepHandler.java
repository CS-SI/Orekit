package fr.cs.orekit.propagation;

import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.StepInterpolator;
import fr.cs.orekit.attitudes.AttitudeKinematicsProvider;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.orbits.EquinoctialParameters;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.orbits.OrbitalParameters;
import fr.cs.orekit.time.AbsoluteDate;

//FIXME : JAVADOC and TEST ! ! !
public abstract class StepHandler {

    private AbsoluteDate initialDate;
    private Frame inertialFrame;
    private AttitudeKinematicsProvider akProvider;
    private double mu;
    private MappingStepHandler handler;

    public StepHandler() {
        handler = new MappingStepHandler();
    }

    protected void initialize(AbsoluteDate date, AttitudeKinematicsProvider provider,
                              Frame frame, double mu) {
        initialDate = date;
        inertialFrame = frame;
        akProvider = provider;
        this.mu = mu;
    }

    protected org.apache.commons.math.ode.StepHandler getMantissaStepHandler() {
        return handler;
    }

    public abstract void handleStep(MappingStepInterpolator interpolator, boolean isLast)
        throws DerivativeException;

    public abstract boolean requiresDenseOutput();

    public abstract void reset() ;

    private class MappingStepHandler implements org.apache.commons.math.ode.StepHandler {

        public void handleStep(StepInterpolator interpolator, boolean isLast) throws DerivativeException {

            StepHandler.this.handleStep(new MappingStepInterpolator(interpolator), isLast);

        }

        public boolean requiresDenseOutput() {
            return StepHandler.this.requiresDenseOutput();
        }

        public void reset() {
            StepHandler.this.reset();
        }

    }

    private class MappingStepInterpolator {

        private MappingStepInterpolator(StepInterpolator interpolator) {
            this.interpolator = interpolator;
        }

        public AbsoluteDate getCurrentDate() {
            return new AbsoluteDate(initialDate, interpolator.getCurrentTime());
        }

        public AbsoluteDate getInterpolatedDate() {
            return new AbsoluteDate(initialDate, interpolator.getInterpolatedTime());
        }

        public SpacecraftState getInterpolatedState() throws OrekitException {
            double[] y = interpolator.getInterpolatedState();

            OrbitalParameters op =
                new EquinoctialParameters(y[0], y[1], y[2], y[3], y[4], y[5],
                                          EquinoctialParameters.TRUE_LATITUDE_ARGUMENT,
                                          inertialFrame);
            AbsoluteDate current = new AbsoluteDate(initialDate, interpolator.getCurrentTime());
            return new SpacecraftState(new Orbit(current, op), y[6],
                                       akProvider.getAttitudeKinematics(current, op.getPVCoordinates(mu), inertialFrame));
        }

        public AbsoluteDate getPreviousDate() {
            return new AbsoluteDate(initialDate, interpolator.getPreviousTime());
        }

        public boolean isForward() {
            return interpolator.isForward();
        }

        public void setInterpolatedDate(AbsoluteDate date) throws DerivativeException {
            interpolator.setInterpolatedTime(date.minus(initialDate));
        }

        private StepInterpolator interpolator;

    }

}
