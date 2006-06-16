package fr.cs.aerospace.orekit.bodies;

import org.spaceroots.mantissa.geometry.Rotation;
import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.RDate;

/** Simple Earth rotation model with a linear sidereal time.
 * @author Luc Maisonobe
 * @version $Id$
 */
public class FixedPoleEarth implements RotatingBody {

  /** Model for &Delta;TU<sub>1</sub>. */
  public static interface DTU1Model {
    /** Get the &Delta;TU<sub>1</sub> value.
     * @param RDate date UTC date
     * @return &Delta;TU<sub>1</sub> (t<sub>UTC</sub> - t<sub>TU1</sub>) in seconds
     */
    public double getDTU1(RDate date);
  }

  /** Build a fixed pole Earth rotation model with null &Delta;TU<sub>1</sub>.
   */
  public FixedPoleEarth() {
    this(null);
  }

  /** Build a fixed pole Earth rotation model.
   * @param model &Delta;TU<sub>1</sub> model, may be null in which
   * case &Delta;TU<sub>1</sub> is considered to be always zero
   */
  public FixedPoleEarth(DTU1Model model) {
    if (model == null) {
      // build a constant dTU1 model
      this.model = new DTU1Model() {
        public double getDTU1(RDate date) {
          return 0.0;
        }
      };
    } else {
      // use the user-provided model
      this.model = model;
    }
  }

  /** Get the orientation of the body.
   * <p>The {@link DTU1Model &Delta;TU<sub>1</sub> model} that was set up
   * in the constructor is automatically taken into account.</p>
   * @param date date to consider
   * @return orientation of the body (rotation transforming a vector projected
   * in intertial frame in the same vector projected in body frame)
   */
  public Rotation getOrientation(RDate date) {

    // CNES sidereal time model (from Veis ?)
    double offset = date.minus(RDate.CNES1950RDate) / 86400.0;
    double r      = offset - Math.floor(offset);
    
    double theta  = theta0 + omegaB * model.getDTU1(date)
                  + omegaA * offset + 2 * Math.PI * r;

    // rotation around a fixed polar axis
    return new Rotation(Vector3D.plusK, theta);

  }

  /** Get the current rotation vector.
   * <p>The rotation vector is the instantaneous rotation axis scaled
   * such as having the angular rate as its norm.</p>
   * @param date date to consider
   * @return current rotation vector in inertial frame (fixed in this
   * model)
   */
  public Vector3D getRotationVector(RDate date) {
    return rotationVector;
  }

  /** &Delta;TU<sub>1</sub> model. */
  private DTU1Model model;

  /** Rotation coefficients. */
  private static double theta0 = 1.746647708617871;
  private static double omegaA = 1.7202179573714597e-2;
  private static double omegaB = 7.292115146705e-5;


  /** Fixed rotation vector. */
  private static Vector3D rotationVector =
    new Vector3D((omegaA + 2 * Math.PI) / 86400.0, Vector3D.plusK);

}
