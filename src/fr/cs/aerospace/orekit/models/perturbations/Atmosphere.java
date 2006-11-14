package fr.cs.aerospace.orekit.models.perturbations;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.time.AbsoluteDate;

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
  public double getDensity(AbsoluteDate date, Vector3D position);

  /** Get the inertial velocity of atmosphere modecules.
   * @param date current date
   * @param position current position in frame
   * @param frame the frame in which is defined the position
   * @return velocity (m/s) (defined in the same frame à the position
   * @throws OrekitException 
   */
  public Vector3D getVelocity(AbsoluteDate date, Vector3D position, Frame frame) 
    throws OrekitException;

}
