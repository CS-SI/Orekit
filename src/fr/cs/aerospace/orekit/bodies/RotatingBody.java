package fr.cs.aerospace.orekit.bodies;

import org.spaceroots.mantissa.geometry.Rotation;

import fr.cs.aerospace.orekit.RDate;

/** Interface representing a rotating natural body.
 * <p>The orientation of a natural body is similar to the attitude for a satellite,
 * it is a 3D rotation that applied to a vector expreseed in inertial frame
 * returns the same vector but expressed in the body frame. The rotation may be
 * a simple rotation around an inertially fixed polar axis, or it may take into
 * account more accurate models for precession and nutation).</p>
 * @author Luc Maisonobe
 * $Id$
 */
public interface RotatingBody {

  /** Get the orientation of the body at a given date.
   * @param date date to consider
   * @return orientation of the body (rotation transforming a vector projected
   * in intertial frame in the same vector projected in body frame)
   */
  public Rotation getOrientation(RDate date);

}
