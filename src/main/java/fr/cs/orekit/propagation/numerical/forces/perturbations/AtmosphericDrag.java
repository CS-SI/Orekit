package fr.cs.orekit.propagation.numerical.forces.perturbations;

import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.models.perturbations.Atmosphere;
import fr.cs.orekit.models.spacecraft.AtmosphereDragSpacecraft;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.propagation.numerical.OrekitSwitchingFunction;
import fr.cs.orekit.propagation.numerical.TimeDerivativesEquations;
import fr.cs.orekit.propagation.numerical.forces.ForceModel;

/** Atmospheric drag force model.
 * The drag acceleration is computed as follows :
 *
 * &gamma = (1/2 * Ro * V<sup>2</sup> * S / Mass) * DragCoefVector
 *
 * With DragCoefVector = {Cx, Cy, Cz} and S given by the user threw the interface
 * {@link AtmosphereDragSpacecraft}
 *
 * @author E. Delente
 * @author F. Maussion
 */

public class AtmosphericDrag implements ForceModel {

    /** Atmospheric model */
    private final Atmosphere atmosphere;

    /** Spacecraft. */
    private final AtmosphereDragSpacecraft spacecraft;

    /** Simple constructor.
     * @param atmosphere atmospheric model
     * @param spacecraft the object physical and geometrical information
     */
    public AtmosphericDrag(Atmosphere atmosphere, AtmosphereDragSpacecraft spacecraft) {
        this.atmosphere = atmosphere;
        this.spacecraft = spacecraft;
    }

    /** Compute the contribution of the drag to the perturbing acceleration.
     * @param s the current state information : date, cinematics, attitude
     * @param adder object where the contribution should be added
     * @param mu central gravitation coefficient
     * @exception OrekitException if some specific error occurs
     */
    public void addContribution(SpacecraftState s, TimeDerivativesEquations adder, double mu)
        throws OrekitException {
        final double rho = atmosphere.getDensity(s.getDate(), s.getPVCoordinates(mu).getPosition(), s.getFrame());

        final Vector3D vAtm =
            atmosphere.getVelocity(s.getDate(), s.getPVCoordinates(mu).getPosition(), s.getFrame());

        final Vector3D incidence = vAtm.subtract(s.getPVCoordinates(mu).getVelocity());
        final double v2 = Vector3D.dotProduct(incidence, incidence);

        final Vector3D inSpacecraft =
            s.getAttitudeKinematics().getAttitude().applyTo(incidence.normalize());
        final double k = rho * v2 * spacecraft.getSurface(inSpacecraft) / (2 * s.getMass());
        final Vector3D cD = spacecraft.getDragCoef(inSpacecraft);

        // Additition of calculated acceleration to adder
        adder.addXYZAcceleration(k * cD.getX(), k * cD.getY(), k * cD.getZ());

    }

    /** There are no SwitchingFunctions for this model.
     * @return an empty array
     */
    public OrekitSwitchingFunction[] getSwitchingFunctions() {
        return new OrekitSwitchingFunction[0];
    }

}
