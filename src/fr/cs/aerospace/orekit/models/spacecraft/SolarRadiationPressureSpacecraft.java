package fr.cs.aerospace.orekit.models.spacecraft;

import org.spaceroots.mantissa.geometry.Vector3D;
import fr.cs.aerospace.orekit.forces.perturbations.SolarRadiationPressure;
//TODO clarify these notions
/** Adapted container for the SolarRadiationPressure force model.
 * 
 * @see SolarRadiationPressure
 * @author F. Maussion
 */
public interface SolarRadiationPressureSpacecraft {

  /** Get the surface.
   * @param direction direction of the light flux in the spacecraft frame
   * @return surface (m<sup>2</sup>)
   */
  public double getSurface(Vector3D direction);

  /** Get the absorption coefficients vector.
   * @param direction direction of the light flux in the spacecraft frame
   * @return absorption coefficients vector in the spacecraft frame
   */
  public Vector3D getAbsCoef(Vector3D direction);

  /** Get the specular reflection coefficients vector.
   * @param direction direction of the light flux in the spacecraft frame
   * @return specular reflection coefficients vector in the spacecraft frame
   */
  public Vector3D getReflectionCoef(Vector3D direction);

}
