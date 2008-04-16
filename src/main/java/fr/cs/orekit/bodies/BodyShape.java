package fr.cs.orekit.bodies;

import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.utils.Line;

/** Interface representing the rigid surface shape of a natural body.
 * <p>The shape is not provided as a single complete geometric
 * model, but single points can be queried ({@link #getIntersectionPoint}).</p>
 * @author Luc Maisonobe
 */
public interface BodyShape {

    /** Get body frame related to body shape.
     * @return body frame related to body shape
     */
    public Frame getBodyFrame();

    /** Get the intersection point of a line with the surface of the body.
     * @param line test line in inertial frame (may intersect the body or not)
     * @param frame frame in which line is expressed
     * @return intersection point at altitude zero or null if the line does
     * not intersect the surface
     */
    public GeodeticPoint getIntersectionPoint(Line line, Frame frame, AbsoluteDate date)
           throws OrekitException ;

    /** Transform a cartesian point to a surface-relative point.
     * @param point cartesian point
     * @param frame frame in which cartesian point is expressed
     * @return point at the same location but as a surface-relative point
     */
    public GeodeticPoint transform(Vector3D point, Frame frame, AbsoluteDate date)
           throws OrekitException ;

    /** Transform a surface-relative point to a cartesian point.
     * @param point surface-relative point
     * @return point at the same location but as a cartesian point
     */
    public Vector3D transform(GeodeticPoint point);

}
