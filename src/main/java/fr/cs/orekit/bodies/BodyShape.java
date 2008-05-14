package fr.cs.orekit.bodies;

import java.io.Serializable;

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
public interface BodyShape extends Serializable {

    /** Get body frame related to body shape.
     * @return body frame related to body shape
     */
    Frame getBodyFrame();

    /** Get the intersection point of a line with the surface of the body.
     * <p>A line may have several intersection points with a closed
     * surface (we consider the one point case as a degenerated two
     * points case). The close parameter is used to select which of
     * these points should be returned. The selected point is the one
     * that is closest to the close point.</p>
     * @param line test line (may intersect the body or not)
     * @param close point used for intersections selection
     * @param frame frame in which line is expressed
     * @param date date of the line in given frame
     * @return intersection point at altitude zero or null if the line does
     * not intersect the surface
     * @exception OrekitException if line cannot be converted to body frame
     * @see fr.cs.orekit.utils.Line#getAbscissa(Vector3D)
     * @see fr.cs.orekit.utils.Line#pointAt(double)
     */
    GeodeticPoint getIntersectionPoint(Line line, Vector3D close,
                                       Frame frame, AbsoluteDate date)
        throws OrekitException;

    /** Transform a cartesian point to a surface-relative point.
     * @param point cartesian point
     * @param frame frame in which cartesian point is expressed
     * @param date date of the computation (used for frames conversions)
     * @return point at the same location but as a surface-relative point
     * @exception OrekitException if point cannot be converted to body frame
     */
    GeodeticPoint transform(Vector3D point, Frame frame, AbsoluteDate date)
        throws OrekitException;

    /** Transform a surface-relative point to a cartesian point.
     * @param point surface-relative point
     * @return point at the same location but as a cartesian point
     */
    Vector3D transform(GeodeticPoint point);

}
