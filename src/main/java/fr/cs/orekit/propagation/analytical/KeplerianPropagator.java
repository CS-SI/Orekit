package fr.cs.orekit.propagation.analytical;

import org.apache.commons.math.geometry.RotationOrder;

import fr.cs.orekit.attitudes.AttitudeLaw;
import fr.cs.orekit.attitudes.LofOffset;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.errors.PropagationException;
import fr.cs.orekit.orbits.EquinoctialOrbit;
import fr.cs.orekit.propagation.Propagator;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.time.AbsoluteDate;

/** Simple keplerian orbit propagator.
 * @author G. Prat
 * @version $Id$
 */
public class KeplerianPropagator implements Propagator {

    /** Serializable UID. */
    private static final long serialVersionUID = 3701238437110897073L;

    /** Attitude law. */
    private AttitudeLaw attitudeLaw;

    /** Initial orbit date. */
    private AbsoluteDate initialDate;

    /** Initial orbit parameters. */
    private EquinoctialOrbit initialParameters;

    /** Initial mass. */
    private double mass;

    /** Mean motion. */
    private double n;

    /** Build a new instance.
     * @param initialState initial state
     * @param mu central acceleration coefficient (m<sup>3</sup>/s<sup>2</sup>)
     */
    public KeplerianPropagator(final SpacecraftState initialState) {
        this.initialDate = initialState.getDate();
        this.initialParameters = new EquinoctialOrbit(initialState.getOrbit());
        this.mass = initialState.getMass();
        this.n = Math.sqrt(initialState.getOrbit().getMu() / initialParameters.getA()) / initialParameters.getA();
        final AttitudeLaw lofAligned = new LofOffset(RotationOrder.ZYX, 0., 0., 0.);
        this.attitudeLaw = lofAligned;
    }

    /** {@inheritDoc} */
    public SpacecraftState getSpacecraftState(final AbsoluteDate date)
        throws PropagationException {

        // evaluation of LM = PA + RAAN + M at extrapolated time

        final EquinoctialOrbit extrapolated =
            new EquinoctialOrbit(initialParameters.getA(), initialParameters.getEquinoctialEx(),
                                      initialParameters.getEquinoctialEy(), initialParameters.getHx(),
                                      initialParameters.getHy(),
                                      initialParameters.getLM() + n * date.minus(initialDate) ,
                                      EquinoctialOrbit.MEAN_LATITUDE_ARGUMENT,
                                      initialParameters.getFrame(),
                                      date, initialParameters.getMu());

        try {
            return new SpacecraftState(extrapolated,
                                       mass,
                                       attitudeLaw.getState(date,
                                                            extrapolated.getPVCoordinates(),
                                                            extrapolated.getFrame()));
        } catch (OrekitException oe) {
            throw new PropagationException(oe.getMessage(), oe);
        }

    }

}
