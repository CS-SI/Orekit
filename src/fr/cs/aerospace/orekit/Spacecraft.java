package fr.cs.aerospace.orekit;

import org.spaceroots.mantissa.geometry.Vector3D;

/** This interface represents spacecraft characteristics.
 * <p>In the {@link #getDragCoef}, {@link #getAbsCoef} and
 * {@link #getReflectionCoef} methods, the coefficients vectors are
 * expressed in the same inertial frame as the argument direction. They
 * represent the effect of a unit flux in the given direction. For example,
 * a spherical shape would return c<sub>D</sub> * direction where
 * c<sub>D</sub> is a positive drag coefficient for drag effect, whereas
 * a non-spherical shape would return something that may not be aligned
 * with direction if there are some lift effects.
 * @version $Id$
 * @author E. Delente
 */

public interface Spacecraft {

  /** Get the mass.
   * @return mass (kg)
   */
  public double getMass();

  /** Get the surface.
   * @param direction direction of the flux
   * @return surface (m<sup>2</sup>)
   */
  public double getSurface(Vector3D direction);

  /** Get the drag coefficients vector.
   * @param direction direction of the atmospheric flux
   * @return drag coefficients vector
   */
  public Vector3D getDragCoef(Vector3D direction);

  /** Get the absorption coefficients vector.
   * @param direction direction of the light flux
   * @return absorption coefficients vector
   */
  public Vector3D getAbsCoef(Vector3D direction);

  /** Get the specular reflection coefficients vector.
   * @param direction direction of the light flux
   * @return specular reflection coefficients vector
   */
  public Vector3D getReflectionCoef(Vector3D direction);

  /** Set the mass.
   * @param mass new mass (kg)
   */
  public void setMass(double mass);

}
