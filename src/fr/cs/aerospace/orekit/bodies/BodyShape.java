package fr.cs.aerospace.orekit.bodies;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.utils.Line;

/** Interface representing the rigid surface shape of a natural body.
 * <p>The shape is not provided as a single complete geometrical
 * model, but single points can be queried ({@link #getIntersectionPoint}).</p>
 * @author Luc Maisonobe
 * $Id$
 */
public interface BodyShape {

  /** Get the intersection point of a line with the surface of the body.
   * @param line test line in inertial frame (may intersect the body or not)
   * @return intersection point at altitude zero or null if the line does
   * not intersect the surface
   */
  public GeodeticPoint getIntersectionPoint(Line line);

  /** Transform a cartesian point to a surface-relative point.
   * @param point cartesian point
   * @return point at the same location but as a surface-relative point
   */
  public GeodeticPoint transform(Vector3D point);

  /** Transform a surface-relative point to a cartesian point.
   * @param point surface-relative point
   * @return point at the same location but as a cartesian point
   */
  public Vector3D transform(GeodeticPoint point);

}
