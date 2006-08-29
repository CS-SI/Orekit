package fr.cs.aerospace.orekit.bodies;

import org.spaceroots.mantissa.geometry.Rotation;
import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.time.AbsoluteDate;

/** Interface representing a rotating natural body.
 * <p>The orientation of a natural body is similar to the attitude for a satellite,
 * it is a 3D rotation that applied to a vector expressed in inertial frame
 * returns the same vector but expressed in the body frame. The rotation may be
 * a simple rotation around an inertially fixed polar axis, or it may take into
 * account more accurate models for precession and nutation.</p>
 * @author Luc Maisonobe
 * $Id$
 */
public interface RotatingBody {

  /** Get the orientation of the body.
   * @param date date to consider
   * @return orientation of the body (rotation transforming a vector projected
   * in intertial frame in the same vector projected in body frame)
   */
  public Rotation getOrientation(AbsoluteDate date);

  /** Get the current rotation vector.
   * <p>The rotation vector is the instantaneous rotation axis scaled
   * such as having the angular rate as its norm.</p>
   * @param date date to consider
   * @return current rotation vector in inertial frame
   */
  public Vector3D getRotationVector(AbsoluteDate date);

}
