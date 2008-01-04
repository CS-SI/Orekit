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

// FIXME : JAVADOC and TEST ! ! !
public abstract class StepHandler {

  public StepHandler() {
    mantissaStepHandler = new mappingStepHandler();
  }

  protected void initialize(AbsoluteDate date, AttitudeKinematicsProvider provider,
                              Frame frame, double mu) {
     initialDate = date;
     inertialFrame = frame;
     akProvider = provider;
     this.mu = mu;
  }

  protected org.apache.commons.math.ode.StepHandler getMantissaStepHandler() {
   return mantissaStepHandler;
 }

  public abstract void handleStep(
   fr.cs.orekit.propagation.StepInterpolator interpolator, boolean isLast)
    throws DerivativeException;

  public abstract boolean requiresDenseOutput();

  public abstract void reset() ;

  AbsoluteDate initialDate;
  Frame inertialFrame;
  AttitudeKinematicsProvider akProvider;
  double mu;

  mappingStepHandler mantissaStepHandler;

  private class mappingStepHandler implements org.apache.commons.math.ode.StepHandler {

    public void handleStep(StepInterpolator interpolator, boolean isLast) throws DerivativeException {

      StepHandler.this.handleStep(new mappingStepInterpolator(interpolator), isLast);

    }

    public boolean requiresDenseOutput() {
      return StepHandler.this.requiresDenseOutput();
    }

    public void reset() {
     StepHandler.this.reset();
    }

  }

  private class mappingStepInterpolator implements fr.cs.orekit.propagation.StepInterpolator {

    private mappingStepInterpolator(StepInterpolator mantissaInterpolator) {

    }

    public AbsoluteDate getCurrentDate() {
      return new AbsoluteDate(initialDate, mantissaInterpolator.getCurrentTime());
    }

    public AbsoluteDate getInterpolatedDate() {
      return new AbsoluteDate(initialDate, mantissaInterpolator.getInterpolatedTime());
    }

    public SpacecraftState getInterpolatedState() throws OrekitException {
      double[] y = mantissaInterpolator.getInterpolatedState();

      OrbitalParameters op =
        new EquinoctialParameters(y[0], y[1], y[2], y[3], y[4], y[5],
                                  EquinoctialParameters.TRUE_LATITUDE_ARGUMENT,
                                  inertialFrame);
      AbsoluteDate current = new AbsoluteDate(initialDate, mantissaInterpolator.getCurrentTime());
      return new SpacecraftState(new Orbit(current, op), y[6],
                          akProvider.getAttitudeKinematics(current, op.getPVCoordinates(mu), inertialFrame));
    }

    public AbsoluteDate getPreviousDate() {
      return new AbsoluteDate(initialDate, mantissaInterpolator.getPreviousTime());
    }

    public boolean isForward() {
      return mantissaInterpolator.isForward();
    }

    public void setInterpolatedDate(AbsoluteDate date) throws DerivativeException {
      mantissaInterpolator.setInterpolatedTime(date.minus(initialDate));
    }

    private StepInterpolator mantissaInterpolator;

  }

}
