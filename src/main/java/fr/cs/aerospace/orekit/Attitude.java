package fr.cs.aerospace.orekit;

import org.spaceroots.mantissa.geometry.Vector3D;
import org.spaceroots.mantissa.geometry.Rotation;
import java.io.Serializable;

/**
 * This class handles attitude of vehicles.

 * <p>
 * This class handles attitude of vehicles.
 * </p>

 * <p>
 * The attributes of this class are spin and attitude.
 * </p>

 * @see     Rotation
 * @version $Id$
 * @author  E. Delente

 */

public class Attitude implements Serializable {

  /** spin. */
  private Vector3D spin;

  /** attitude. */
  private Rotation attitude;

  /** Default constructor.
   * Build a new instance with arbitrary default elements.
   */
  public Attitude() {
    spin = new Vector3D();
    attitude = new Rotation();
  }

  /** Creates a new instance of Attitude
   * @param spin Vector3D spin of the vehicle
   * @param attitude Rotation attitude of the vehicle
   */
  public Attitude(Vector3D spin, Rotation attitude) {
    this.spin = spin;
    attitude = new Rotation();
  }
  
  /** Reset the orbit to default.
   * Reset the orbit with arbitrary default elements.
   */
  public void reset() {
    this.spin = new Vector3D();
    this.attitude = new Rotation();
  }

  /** Reset the attitude.
   * @param spin  spin
   * @param attitude attitude 
   */
  public void reset(Vector3D spin, Rotation attitude) {
    this.spin.reset(spin);
    this.attitude = attitude;
  }
  /** Get the spin.
   * @return spin
   */
  public Vector3D getspin() {
    return spin;
  }

  /** Set the spin.
   * @param spin Vector3D
   */
  public void setspin(Vector3D spin) {
    this.spin.reset(spin);
  }

  /** Get the attitude.
   * @return attitude Rotation
   */
  public Rotation getattitude() {
    return attitude;
  }

  /** Set the attitude.
  * @param attitude Rotation
  */
  public void setattitude(Rotation attitude) {
      this.attitude = attitude;
  }

  /** convert a vector to the satellite frame
  * <p>
  * apply the rotation matrix to the inertial frame vector and
  * return the satellite frame vector
  * </p>
  * @param inertialFrameVector Vector3D
  */
  public Vector3D convertToSatelliteFrame(Vector3D inertialFrameVector) {
    Vector3D satelliteFrameVector = new Vector3D();
    satelliteFrameVector = attitude.applyInverseTo(inertialFrameVector);
    return satelliteFrameVector;
  }
  
  /** convert a vector to the inertial frame
  * <p>
  * apply the rotation matrix to the satellite frame vector and
  * return the inertial frame vector
  * </p>
  * @param satelliteFrameVector Vector3D
  */
  public Vector3D convertToInertialFrame(Vector3D satelliteFrameVector) {
    Vector3D inertialFrameVector = new Vector3D();
    inertialFrameVector = attitude.applyTo(satelliteFrameVector);
    return inertialFrameVector;
  }
  
 /**  Returns a string representation of this Attitude object
  * @return a string representation of this object
  */
 public String toString() {
   StringBuffer sb = new StringBuffer();
   sb.append('{');
   sb.append(spin.toString());
   sb.append(' ');
   sb.append(attitude.toString());
   sb.append('}');
   return sb.toString();
 }
}
