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
 * @author V. Pommier-Maurussane
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

        // compute bodies separation vectors and squared norm
        final Vector3D centralToBody = body.getPosition(s.getDate(), s.getFrame());
        final double r2Central       = Vector3D.dotProduct(centralToBody, centralToBody);
        final Vector3D satToBody     = centralToBody.subtract(s.getPVCoordinates().getPosition());
        final double r2Sat           = Vector3D.dotProduct(satToBody, satToBody);

        // compute relative acceleration
        final Vector3D gamma =
            new Vector3D(body.getMu() * Math.pow(r2Sat, -1.5), satToBody,
                        -body.getMu() * Math.pow(r2Central, -1.5), centralToBody);

        // add contribution to the ODE second member
        adder.addXYZAcceleration(gamma.getX(), gamma.getY(), gamma.getZ());

    }

    /** {@inheritDoc} */
    public OrekitSwitchingFunction[] getSwitchingFunctions() {
        return new OrekitSwitchingFunction[0];
    }

}
