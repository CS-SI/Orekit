package fr.cs.orekit.propagation.analytical;

import org.apache.commons.math.geometry.RotationOrder;

import fr.cs.orekit.attitudes.AttitudeLaw;
import fr.cs.orekit.attitudes.LofOffset;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.errors.PropagationException;
import fr.cs.orekit.orbits.EquinoctialParameters;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.propagation.AttitudePropagator;
import fr.cs.orekit.propagation.Ephemeris;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.time.AbsoluteDate;

/** Simple keplerian orbit propagator.
 * @author G. Prat
 * @version $Id$
 */
public class KeplerianPropagator implements Ephemeris, AttitudePropagator {

    /** Serializable UID. */
    private static final long serialVersionUID = -4355212980078171786L;

    /** Attitude law. */
    private AttitudeLaw attitudeLaw;

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
    public KeplerianPropagator(final SpacecraftState initialState, final double mu) {
        this.initialDate = initialState.getDate();
        this.initialParameters = new EquinoctialParameters(initialState.getParameters(), mu);
        this.mass = initialState.getMass();
        this.n = Math.sqrt(mu / initialParameters.getA()) / initialParameters.getA();
        final AttitudeLaw lofAligned = new LofOffset(RotationOrder.ZYX, 0., 0., 0.);
        this.attitudeLaw = lofAligned;
        this.mu = mu;
    }

    /** {@inheritDoc} */
    public SpacecraftState getSpacecraftState(final AbsoluteDate date)
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
            return new SpacecraftState(new Orbit(date, extrapolated),
                                       mass,
                                       attitudeLaw.getState(date,
                                                            extrapolated.getPVCoordinates(mu),
                                                            extrapolated.getFrame()));
        } catch (OrekitException oe) {
            throw new PropagationException(oe.getMessage(), oe);
        }

    }

    public void setAttitudeLaw(final AttitudeLaw attitudeLaw) {
        this.attitudeLaw = attitudeLaw;
    }

}
