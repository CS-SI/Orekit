package fr.cs.orekit.propagation.numerical;

import org.apache.commons.math.ode.SwitchException;
import org.apache.commons.math.ode.SwitchingFunction;

import fr.cs.orekit.attitudes.AttitudeLaw;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.orbits.EquinoctialParameters;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.time.AbsoluteDate;

/** Converts OREKIT switching functions to commons-math interface. */
class WrappedSwitchingFunction implements SwitchingFunction {

    /** Serializable UID. */
    private static final long serialVersionUID = 4547309170150559639L;

    /** Underlying Orekit switching function. */
    private final OrekitSwitchingFunction swf;

    /** Reference date from which t is counted. */
    private final AbsoluteDate referenceDate;

    /** Central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private final double mu;

    /** integrationFrame frame in which integration is performed. */
    private final Frame integrationFrame;

    /** attitudeLaw spacecraft attitude law. */
    private final AttitudeLaw attitudeLaw;

    /** Build a wrapped switching function.
     * @param swf Orekit switching function
     * @param referenceDate reference date from which t is counted
     * @param mu central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param integrationFrame frame in which integration is performed
     * @param attitudeLaw spacecraft attitude law
     */
    public WrappedSwitchingFunction(final OrekitSwitchingFunction swf,
                                    final AbsoluteDate referenceDate, final double mu,
                                    final Frame integrationFrame, final AttitudeLaw attitudeLaw) {
        this.swf              = swf;
        this.referenceDate    = referenceDate;
        this.mu               = mu;
        this.integrationFrame = integrationFrame;
        this.attitudeLaw      = attitudeLaw;
    }

    /** {@inheritDoc} */
    public double g(final double t, final double[] y)
        throws SwitchException {
        try {
            return swf.g(mapState(t, y, referenceDate, mu, integrationFrame, attitudeLaw), mu);
        } catch (OrekitException oe) {
            throw new SwitchException(oe);
        }
    }

    /** {@inheritDoc} */
    public int eventOccurred(final double t, final double[] y)
        throws SwitchException {
        try {
            swf.eventOccurred(mapState(t, y, referenceDate, mu, integrationFrame, attitudeLaw), mu);
            return RESET_DERIVATIVES;
        } catch (OrekitException oe) {
            throw new SwitchException(oe);
        }
    }

    /** {@inheritDoc} */
    public void resetState(final double t, final double[] y) {
        // never called since eventOccured never returns CallResetState
    }

    /** Convert state array to space dynamics objects
     * ({@link fr.cs.orekit.time.AbsoluteDate AbsoluteDate} and
     * ({@link fr.cs.orekit.orbits.OrbitalParameters OrbitalParameters}).
     * @param t integration time (s)
     * @param y state as a flat array
     * @param referenceDate reference date from which t is counted
     * @param mu central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param integrationFrame frame in which integration is performed
     * @param attitudeLaw spacecraft attitude law
     * @return state corresponding to the flat array as a space dynamics object
     * @exception OrekitException if attitude law cannot provide state
     */
    private static SpacecraftState mapState(final double t, final double [] y,
                                            final AbsoluteDate referenceDate, final double mu,
                                            final Frame integrationFrame,
                                            final AttitudeLaw attitudeLaw)
        throws OrekitException {

        // update space dynamics view
        final EquinoctialParameters currentParameters =
            new EquinoctialParameters(y[0], y[1],y[2],y[3],y[4],y[5],
                                      EquinoctialParameters.TRUE_LATITUDE_ARGUMENT,
                                      integrationFrame);
        final AbsoluteDate currentDate = new AbsoluteDate(referenceDate, t);
        return
            new SpacecraftState(new Orbit(currentDate, currentParameters), y[6],
                                attitudeLaw.getState(currentDate,
                                                     currentParameters.getPVCoordinates(mu),
                                                     integrationFrame));
    }

}
