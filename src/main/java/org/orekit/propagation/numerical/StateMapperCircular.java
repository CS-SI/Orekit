package org.orekit.propagation.numerical;

import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** Implementation of the {@link StateMapper} interface for state arrays in circular parameters.
 * <p>
 * Instances of this class are guaranteed to be immutable
 * </p>
 *
 * @see org.orekit.propagation.SpacecraftState
 * @see org.orekit.propagation.numerical.NumericalPropagator
 * @see TimeDerivativesEquationsKeplerian
 * @author Luc Maisonobe
 */
public class StateMapperCircular implements StateMapper {

    /** Serializable UID. */
    private static final long serialVersionUID = -8601058172096831979L;

    /** Position angle type. */
    private final PositionAngle type;

    /** Attitude provider. */
    private final AttitudeProvider attitudeProvider;

    /** Create a new instance.
     * @param type position angle type
     * @param attitudeProvider attitude provider
     */
    public StateMapperCircular(final PositionAngle type, final AttitudeProvider provider) {
        this.type             = type;
        this.attitudeProvider = provider;
    }

    /** {@inheritDoc} */
    public void mapStateToArray(final SpacecraftState s, final double[] stateVector) {

        final CircularOrbit circularOrbit =
            (CircularOrbit) OrbitType.CIRCULAR.convertType(s.getOrbit());

        stateVector[0] = circularOrbit.getA();
        stateVector[1] = circularOrbit.getCircularEx();
        stateVector[2] = circularOrbit.getCircularEy();
        stateVector[3] = circularOrbit.getI();
        stateVector[4] = circularOrbit.getRightAscensionOfAscendingNode();
        stateVector[5] = circularOrbit.getAlpha(type);
        stateVector[6] = s.getMass();

    }

    /** {@inheritDoc} */
    public SpacecraftState mapArrayToState(final double[] stateVector, final AbsoluteDate date,
                                           final double mu, final Frame frame) throws OrekitException {
        final CircularOrbit orbit =
            new CircularOrbit(stateVector[0], stateVector[1], stateVector[2], stateVector[3],
                              stateVector[4], stateVector[5], type,
                              frame, date, mu);

        final Attitude attitude = attitudeProvider.getAttitude(orbit, date, frame);

        return new SpacecraftState(orbit, attitude, stateVector[6]);

    }

}
