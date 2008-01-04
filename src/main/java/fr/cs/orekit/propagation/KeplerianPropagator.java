package fr.cs.orekit.propagation;

import fr.cs.orekit.attitudes.AttitudeKinematicsProvider;
import fr.cs.orekit.attitudes.models.IdentityAttitude;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.errors.PropagationException;
import fr.cs.orekit.orbits.EquinoctialParameters;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.time.AbsoluteDate;

/** Simple keplerian orbit extrapolator.
 * @author G. Prat
 * @version $Id$
 */
public class KeplerianPropagator implements Ephemeris, AttitudePropagator {

  /** Build a new instance.
   * @param initialState initial state
   * @param mu central acceleration coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public KeplerianPropagator(SpacecraftState initialState, double mu) {
    this.initialDate = initialState.getDate();
    this.initialParameters = new EquinoctialParameters(initialState.getParameters(), mu);
    this.mass = initialState.getMass();
    this.n = Math.sqrt(mu / initialParameters.getA()) / initialParameters.getA();
    this.akProvider = new IdentityAttitude();
    this.mu = mu;
  }

  public SpacecraftState getSpacecraftState(AbsoluteDate date)
  throws PropagationException {

    // evaluation of LM = PA + RAAN + M at extrapolated time

    EquinoctialParameters extrapolated = new EquinoctialParameters(
             initialParameters.getA(), initialParameters.getEquinoctialEx(),
             initialParameters.getEquinoctialEy(), initialParameters.getHx(),
             initialParameters.getHy(),
             initialParameters.getLM() + n * date.minus(initialDate) ,
             EquinoctialParameters.MEAN_LATITUDE_ARGUMENT, initialParameters.getFrame());

    try {
      return new SpacecraftState(new Orbit(date, extrapolated), mass,
                                akProvider.getAttitudeKinematics(date,
                                                                 extrapolated.getPVCoordinates(mu),
                                                                 extrapolated.getFrame()));
    } catch (OrekitException oe) {
      throw new PropagationException(oe.getMessage(), oe);
    }

  }

  public void setAkProvider(AttitudeKinematicsProvider akProvider) {
    this.akProvider = akProvider;
  }

  /** Attitude provider */
  private AttitudeKinematicsProvider akProvider;

  /** Initial orbit date. */
  private AbsoluteDate initialDate;

  /** Initial orbit parameters. */
  private EquinoctialParameters initialParameters;

  /** Initial mass. */
  private double mass;

  /** Mu. */
  private double mu;

  /** Mean motion. */
  private double n;

}