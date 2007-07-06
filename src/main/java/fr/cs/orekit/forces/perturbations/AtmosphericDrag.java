package fr.cs.orekit.forces.perturbations;

import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.forces.ForceModel;
import fr.cs.orekit.forces.SWF;
import fr.cs.orekit.models.perturbations.Atmosphere;
import fr.cs.orekit.models.spacecraft.AtmosphereDragSpacecraft;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.propagation.TimeDerivativesEquations;

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
   * @throws OrekitException if some specific error occurs
   */
  public void addContribution(SpacecraftState s, TimeDerivativesEquations adder, double mu)
      throws OrekitException {
    double rho = atmosphere.getDensity(s.getDate(), s.getPVCoordinates(mu).getPosition(), s.getFrame());
    
    Vector3D vAtm;
    vAtm = atmosphere.getVelocity(s.getDate(), s.getPVCoordinates(mu).getPosition(), s.getFrame());

    Vector3D incidence = vAtm.subtract(s.getPVCoordinates(mu).getVelocity());
    double v2 = Vector3D.dotProduct(incidence, incidence);
    
    Vector3D inSpacecraft = s.getAttitudeKinematics().getAttitude().applyTo(incidence.normalize());
    double k = rho * v2 * spacecraft.getSurface(inSpacecraft) / (2 * s.getMass());
    Vector3D cD = spacecraft.getDragCoef(inSpacecraft);
    s.getAttitudeKinematics().getAttitude().applyInverseTo(cD);

    // Additition of calculated acceleration to adder
    adder.addXYZAcceleration(k * cD.getX(), k * cD.getY(), k * cD.getZ());

  }

  public SWF[] getSwitchingFunctions() {
    return new SWF[0];
  }

  /** Atmospheric model */
  private Atmosphere atmosphere;

  /** Spacecraft. */
  private AtmosphereDragSpacecraft spacecraft;

}
