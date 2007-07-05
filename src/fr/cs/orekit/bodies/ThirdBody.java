package fr.cs.orekit.bodies;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.time.AbsoluteDate;
import org.apache.commons.math.geometry.Vector3D;

/** This class represents an attracting body different from the central one.
 * @author E. Delente
 */

public abstract class ThirdBody {

  /** Simple constructor.
   * @param radius equatorial radius
   * @param mu attraction coefficient
   */
  protected ThirdBody(double radius, double mu) {
    this.radius = radius;
    this.mu = mu;
  }

  /** Get the position of the body in the selected frame.
   * @param date current date
   * @param frame the frame where to define the position
   * @return position of the body (m)
   * @throws OrekitException 
   */
  public abstract Vector3D getPosition(AbsoluteDate date , Frame frame) throws OrekitException;

  /** Get the equatorial radius of the body.
   * @return equatorial radius of the body (m)
   */
  public double getRadius() {
    return radius;
  }

  /** Get the attraction coefficient of the body.
   * @return attraction coefficient of the body (m<sup>3</sup>/s<sup>2</sup>)
   */
  public double getMu() {
    return mu;
  }

  /** Equatorial radius. */
  protected double radius;

  /** Attraction coefficient. */
  protected double mu;

}
