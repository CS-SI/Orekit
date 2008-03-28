package fr.cs.orekit.propagation;

import fr.cs.orekit.attitudes.AttitudeKinematics;
import fr.cs.orekit.attitudes.AttitudeKinematicsProvider;
import fr.cs.orekit.attitudes.models.IdentityAttitude;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.errors.PropagationException;
import fr.cs.orekit.orbits.EquinoctialParameters;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.time.AbsoluteDate;

/** Simple keplerian orbit propagator.
 * @author G. Prat
 * @version $Id$
 */
public class KeplerianPropagator implements Ephemeris, AttitudePropagator {

    /** Serializable UID. */
    private static final long serialVersionUID = -4933358784345601310L;

    /** Attitude provider */
    private AttitudeKinematicsProvider akProvider;

    /** Initial orbit date. */
    private AbsoluteDate initialDate;

    /** Initial orbit parameters. */
    private EquinoctialParameters initialParameters;

    /** Initial mass. */
    private double mass;

    /** Central body attraction coefficient. */
    private double mu;

    /** Mean motion. */
    private double n;

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

    /** {@inheritDoc} */
    public SpacecraftState getSpacecraftState(AbsoluteDate date)
        throws PropagationException {

        // evaluation of LM = PA + RAAN + M at extrapolated time

        final EquinoctialParameters extrapolated =
            new EquinoctialParameters(initialParameters.getA(), initialParameters.getEquinoctialEx(),
                                      initialParameters.getEquinoctialEy(), initialParameters.getHx(),
                                      initialParameters.getHy(),
                                      initialParameters.getLM() + n * date.minus(initialDate) ,
                                      EquinoctialParameters.MEAN_LATITUDE_ARGUMENT,
                                      initialParameters.getFrame());

        try {
            final AttitudeKinematics ak =
                akProvider.getAttitudeKinematics(date,
                                                 extrapolated.getPVCoordinates(mu),
                                                 extrapolated.getFrame());
            return new SpacecraftState(new Orbit(date, extrapolated), mass, ak);
        } catch (OrekitException oe) {
            throw new PropagationException(oe.getMessage(), oe);
        }

    }

    public void setAkProvider(AttitudeKinematicsProvider akProvider) {
        this.akProvider = akProvider;
    }

}
