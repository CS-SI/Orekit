package fr.cs.aerospace.orekit;

import org.spaceroots.mantissa.geometry.Vector3D;

/** Interface for atmospheric models.
 * @version $Id$
 * @author Luc Maisonobe
 */
public interface Atmosphere {

  /** Get the local density.
   * @param date current date
   * @param position current position
   * @return local density (kg/m<sup>3</sup>)
   */
  public double getDensity(RDate date, Vector3D position);

  /** Get the inertial velocity of atmosphere modecules.
   * @param date current date
   * @param position current position
   * @return volocity (m/s)
   */
  public Vector3D getVelocity(RDate date, Vector3D position);

}
