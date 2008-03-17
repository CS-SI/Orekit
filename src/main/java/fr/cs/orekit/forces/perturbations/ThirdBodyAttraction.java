package fr.cs.orekit.forces.perturbations;

import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.bodies.ThirdBody;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.forces.ForceModel;
import fr.cs.orekit.forces.SWF;
import fr.cs.orekit.models.bodies.Moon;
import fr.cs.orekit.models.bodies.Sun;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.propagation.TimeDerivativesEquations;

/** Third body attraction force model.
 *
 * @author F. Maussion
 */
public class ThirdBodyAttraction implements ForceModel {

    /** Simple constructor.
     * @param body the third body to consider
     * (ex: {@link Sun} or {@link Moon})
     */
    public ThirdBodyAttraction(ThirdBody body) {
        this.body = body;
    }

    /** Compute the contribution of the body attraction to the perturbing
     * acceleration.
     * @param s the current state information : date, cinematics, attitude
     * @param adder object where the contribution should be added
     * @param mu central gravitation coefficient
     * @throws OrekitException if some specific error occurs
     */
    public void addContribution(SpacecraftState s, TimeDerivativesEquations adder, double mu)
    throws OrekitException {
        Vector3D otherBody = body.getPosition(s.getDate(), s.getFrame());

        Vector3D centralBody = new Vector3D(-1.0 , s.getPVCoordinates(mu).getPosition());
        centralBody = centralBody.add(otherBody);
        centralBody = centralBody.scalarMultiply(1.0/Math.pow(centralBody.getNorm(), 3));

        otherBody = otherBody.scalarMultiply(1.0/Math.pow(otherBody.getNorm(), 3));

        Vector3D gamma = centralBody.subtract(otherBody);
        gamma = gamma.scalarMultiply(body.getMu());

        adder.addXYZAcceleration(gamma.getX(), gamma.getY(), gamma.getZ());
    }

    /** Ther are no SwitchingFunctions for this model.
     * @return null
     */
    public SWF[] getSwitchingFunctions() {
        return new SWF[0];
    }

    /** The body to consider */
    private ThirdBody body;

}
