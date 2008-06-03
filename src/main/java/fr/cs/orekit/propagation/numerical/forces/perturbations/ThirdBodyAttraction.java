package fr.cs.orekit.propagation.numerical.forces.perturbations;

import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.bodies.ThirdBody;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.propagation.numerical.OrekitSwitchingFunction;
import fr.cs.orekit.propagation.numerical.TimeDerivativesEquations;
import fr.cs.orekit.propagation.numerical.forces.ForceModel;

/** Third body attraction force model.
 *
 * @author F. Maussion
 * @author  V. Pommier-Maurussane
 */
public class ThirdBodyAttraction implements ForceModel {

    /** Serializable UID. */
    private static final long serialVersionUID = 9017402538195695004L;

    /** The body to consider. */
    private final ThirdBody body;

    /** Simple constructor.
     * @param body the third body to consider
     * (ex: {@link fr.cs.orekit.models.bodies.Sun} or
     * {@link fr.cs.orekit.models.bodies.Moon})
     */
    public ThirdBodyAttraction(final ThirdBody body) {
        this.body = body;
    }

    /** {@inheritDoc} */
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder)
        throws OrekitException {

        Vector3D otherBody = body.getPosition(s.getDate(), s.getFrame());
        Vector3D centralBody =
            new Vector3D(-1.0, s.getPVCoordinates().getPosition(), 1.0, otherBody);
        centralBody = centralBody.scalarMultiply(1.0 / Math.pow(centralBody.getNorm(), 3));
        otherBody = otherBody.scalarMultiply(1.0 / Math.pow(otherBody.getNorm(), 3));

        Vector3D gamma = centralBody.subtract(otherBody);
        gamma = gamma.scalarMultiply(s.getOrbit().getMu());
        adder.addXYZAcceleration(gamma.getX(), gamma.getY(), gamma.getZ());

    }

    /** {@inheritDoc} */
    public OrekitSwitchingFunction[] getSwitchingFunctions() {
        return new OrekitSwitchingFunction[0];
    }

}
