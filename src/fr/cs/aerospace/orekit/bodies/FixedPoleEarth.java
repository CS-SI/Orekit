package fr.cs.aerospace.orekit.bodies;

import org.spaceroots.mantissa.geometry.Rotation;
import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.RDate;

/** Simple Earth rotation model with a linear sidereal time.
 * @author Luc Maisonobe
 * @version $Id$
 */
public class FixedPoleEarth implements RotatingBody {

  /** Get the orientation of the body.
   * @param date date to consider
   * @return orientation of the body (rotation transforming a vector projected
   * in intertial frame in the same vector projected in body frame)
   */
  public Rotation getOrientation(RDate date) {

    // sidereal time upgraded wrt the CNES julian date t
    // (seconds are expressed in fraction of day)
    double offset = date.minus(RDate.CNES1950RDate);
    double r      = offset - Math.floor(offset);

    // for accuracy purposes, omegaA and omegaB should NOT be summed
    double theta  = theta0 + omegaA * offset + omegaB * offset
                  + 2 * Math.PI * r;

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

  /** Rotation coefficients. */
  private static double theta0 = 1.746647708617871;
  private static double omegaA = 1.7202179573714597e-2;
  private static double omegaB = 7.292115146705e-5;

  /** Fixed rotation vector. */
  private static Vector3D rotationVector =
    new Vector3D(omegaA + omegaB, Vector3D.plusK);

}
