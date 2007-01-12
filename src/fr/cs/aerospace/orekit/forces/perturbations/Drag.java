package fr.cs.aerospace.orekit.forces.perturbations;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.attitudes.AttitudeKinematics;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.forces.ForceModel;
import fr.cs.aerospace.orekit.forces.SWF;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.models.perturbations.Atmosphere;
import fr.cs.aerospace.orekit.models.spacecraft.AtmosphereDragSpacecraft;
import fr.cs.aerospace.orekit.propagation.TimeDerivativesEquations;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;

/** Atmospheric drag force model.
 * 
 * @author E. Delente
 */

public class Drag implements ForceModel {

  /** Simple constructor.
   * @param atmosphere atmospheric model
   * @param spacecraft the object physical and geometrical information
   */
  public Drag(Atmosphere atmosphere, AtmosphereDragSpacecraft spacecraft) {
    this.atmosphere = atmosphere;
    this.spacecraft = spacecraft;
  }

  /** Compute the contribution of the drag to the perturbing acceleration.
   * @param date current date
   * @param pvCoordinates the position and velocity
   * @param frame in which are defined the coordinates
   * @param mass the current mass (kg)
   * @param ak the attitude representation
   * @param adder object where the contribution should be added
   * @throws OrekitException if some specific error occurs
   */
  public void addContribution(AbsoluteDate date, PVCoordinates pvCoordinates,
                              Frame frame, double mass, AttitudeKinematics ak,
                              TimeDerivativesEquations adder)
      throws OrekitException {

    double rho = atmosphere.getDensity(date, pvCoordinates.getPosition());
    Vector3D vAtm;

    vAtm = atmosphere.getVelocity(date, pvCoordinates.getPosition(), frame);

    Vector3D incidence = vAtm.subtract(pvCoordinates.getVelocity());
    double v2 = Vector3D.dotProduct(incidence, incidence);
    
    Vector3D inSpacecraft = ak.getAttitude().applyTo(incidence.normalize());
    double k = rho * v2 * spacecraft.getSurface(inSpacecraft) / (2 * mass);
    Vector3D cD = spacecraft.getDragCoef(inSpacecraft);
    ak.getAttitude().applyInverseTo(cD);

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
