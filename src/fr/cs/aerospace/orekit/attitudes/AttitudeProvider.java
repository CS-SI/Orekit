package fr.cs.aerospace.orekit.attitudes;

import org.spaceroots.mantissa.geometry.Rotation;
import org.spaceroots.mantissa.geometry.Vector3D;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.propagation.SpacecraftState;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;
//TODO Validation of the javadoc by the headchief
/** This interface represents the attitude of a space system.
 * 
 * <p> It returns the attitude quaternion (used internally but represented by the
 * {@link Rotation} object) and the instant rotation axis (first time derivative)
 *  at a given time. This interface will be used as a state parameter to represent
 *  a {@link SpacecraftState} <p>
 *  
 *  <p> Orekit will propose several implemented attitude interfaces, providing 
 *  specific and perfectly automatised attitudes, as they do not consider for 
 *  the moment the perturbing couples, the captors and spacecraft dynamic.</p> 
 *  
 * @see SpacecraftState
 * @author F. Maussion
 */
public interface AttitudeProvider { 
// TODO cannot "implement" serialasible, problem
  /** Get the attitude rotation.
   * <p> The {@link Rotation} returned by this method represents the rotation
   * to apply to a vector expressed in the inertial frame to obtain the same vector 
   * defined in the spacecraft frame </p> 
   * @param date the current date
   * @param pv the coordinates in the inertial frame
   * @param frame the inertial frame in which are defined the coordinates
   * @return the attitude rotation of the spacecraft
   * @throws OrekitException if some specific error occurs.
   */
  public Rotation getAttitude(AbsoluteDate date, PVCoordinates pv, Frame frame)
  throws OrekitException;

  /** Get the attitute rotation derivative.
   * <p> The {@link Vector3D} returned by this method is the rotation axis 
   * in the spacecraft frame. Its norm represents the spin. </p> 
   * @param date the current date
   * @param pv the coordinates in the inertial frame
   * @param frame the inertial frame
   * @return the instant rotation of the spacecraft
   * @throws OrekitException if some specific error occurs.
   */
  public Vector3D getInstantRotAxis(AbsoluteDate date, PVCoordinates pv, Frame frame)
  throws OrekitException;

}
