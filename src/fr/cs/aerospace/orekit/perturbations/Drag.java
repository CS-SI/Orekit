package fr.cs.aerospace.orekit.perturbations;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.Atmosphere;
import fr.cs.aerospace.orekit.Attitude;
import fr.cs.aerospace.orekit.RDate;
import fr.cs.aerospace.orekit.Spacecraft;
import fr.cs.aerospace.orekit.orbits.OrbitDerivativesAdder;

/** Atmospheric drag force model.
 * @version $Id$
 * @author E. Delente
 */

public class Drag implements ForceModel {

  /** Simple constructor.
   * @param atmosphere atmospheric model
   * @param spacecraft spacecraft
   */
  public Drag(Atmosphere atmosphere, Spacecraft spacecraft) {
    this.atmosphere = atmosphere;
    this.spacecraft = spacecraft;
  }

  /** Compute the contribution of the drag to the perturbing acceleration.
   * @param date current date
   * @param position current position(m)
   * @param velocity current velocity (m/s)
   * @param Attitude current attitude
   * @param adder object where the contribution should be added
   */
  public void addContribution(RDate date,
                              Vector3D position, Vector3D velocity,
                              Attitude Attitude, OrbitDerivativesAdder adder) {

    double   rho       = atmosphere.getDensity(date, position);
    Vector3D vAtm      = atmosphere.getVelocity(date, position);
    Vector3D incidence = Vector3D.subtract(vAtm, velocity);
    double   v2        = Vector3D.dotProduct(incidence, incidence);
    incidence.normalizeSelf();
    double   k         = rho * v2 * spacecraft.getSurface(incidence)
                       / (2 * spacecraft.getMass());
    Vector3D cD        = spacecraft.getDragCoef(incidence);

    // Additition of calculated accelration to adder
    adder.addXYZAcceleration(k * cD.getX(), k * cD.getY(), k * cD.getZ());

  }

  public SWF[] getSwitchingFunctions() {
    return null;
  }

  /** Atmospheric model */
  private Atmosphere atmosphere;

  /** Spacecraft. */
  private Spacecraft spacecraft;

}
