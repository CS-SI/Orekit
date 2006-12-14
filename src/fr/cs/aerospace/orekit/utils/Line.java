package fr.cs.aerospace.orekit.utils;

import org.spaceroots.mantissa.geometry.Vector3D;

/** The class represent lines in a three dimensional space.

 * <p>Each oriented line is intrinsically associated with an abscissa
 * wich is a coordinate on the line. The point at abscissa 0 is the
 * orthogonal projection of the origin on the line, another equivalent
 * way to express this is to say that it is the point of the line
 * which is closest to the origin. Abscissa increases in the line
 * direction.</p>

 * <p>This class is a simplification of the
 * <code>org.spaceroots.lift.geometry.threeD.Line</code> class from the Lift
 * library written and owned by Luc Maisonobe. It is reused here with permission,
 * as CS has a source and redistribution licence for it.</p>

 * @author L. Maisonobe
 * @version $Id$
 */
public class Line {

  /** Build a line from a point and a direction.
   * @param p point belonging to the line (this can be any point)
   * @param direction direction of the line
   * @exception IllegalArgumentException if the direction norm is too small
   */
  public Line(Vector3D p, Vector3D direction) {
	  double norm = direction.getNorm();
	    if (norm < 1.0e-10) {
	      throw new IllegalArgumentException("null norm");
	    }
	    this.direction = new Vector3D(1.0 / norm, direction);
	    zero = new Vector3D(1.0, p,
	                        -Vector3D.dotProduct(p, this.direction), this.direction);
  }
  
  /** Revert the line direction.
   * @param line the line to revert
   * @return a new instance of Line wich is the reverse of line 
   */
  public static Line revert(Line line) {
    return new Line(line.zero , line.direction.negate());
  }

  /** Get the normalized direction vector.
   * @return normalized direction vector
   */
  public Vector3D getDirection() {
    return direction;
  }

  /** Get the line point closest to the origin.
   * @return line point closest to the origin
   */
  public Vector3D getOrigin() {
    return zero;
  }

  /** Get the abscissa of a point with respect to the line.
   * <p>The abscissa is 0 if the projection of the point and the
   * projection of the frame origin on the line are the same
   * point.</p>
   * @param point point to check 
   * @return abscissa of the point
   */
  public double getAbscissa(Vector3D point) {
    return Vector3D.dotProduct(point.subtract(zero), direction);
  }

  /** Get one point from the line.
   * @param abscissa desired abscissa for the point 
   * @return one point belonging to the line, at specified abscissa
   * (really a {@link Vector3D Vector3D} instance)
   */
  public Vector3D pointAt(double abscissa) {
    return new Vector3D(1.0, zero, abscissa, direction);
  }

  /** Check if the instance contains a point.
   * @param p point to check
   * @return true if p belongs to the line
   */
  public boolean contains(Vector3D p) {
    return distance(p) < 1.0e-10;
  }

  /** Compute the distance between the instance and a point.
   * @param p to check
   * @return distance between the instance and the point
   */
  public double distance(Vector3D p) {
    Vector3D d = p.subtract(zero);
    Vector3D n = new Vector3D(1.0, d, -Vector3D.dotProduct(d, direction), direction);
    return n.getNorm();
  }

 /** Line direction. */
  private Vector3D direction;

  /** Line point closest to the origin. */
  private Vector3D zero;

}
