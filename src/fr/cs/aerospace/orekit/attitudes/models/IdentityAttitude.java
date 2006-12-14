package fr.cs.aerospace.orekit.attitudes.models;

import org.spaceroots.mantissa.geometry.Rotation;
import org.spaceroots.mantissa.geometry.Vector3D;
import fr.cs.aerospace.orekit.attitudes.AttitudeKinematics;
import fr.cs.aerospace.orekit.attitudes.AttitudeKinematicsProvider;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;

/** Very simple attitude (inertial frame oriented).
 * 
 * @author F.Maussion
 */
public class IdentityAttitude implements AttitudeKinematicsProvider {

  /** Simple constructor. 
   */
  public IdentityAttitude() {
    ak = new AttitudeKinematics(new Rotation(), new Vector3D());
  }
  
  /** Get the attitude representation in the selected frame.
   * @param date the current date
   * @param pv the coordinates in the inertial frame
   * @param frame the inertial frame in which are defined the coordinates
   * @return the attitude representation of the spacecraft
   * @throws OrekitException if some specific error occurs.
   */
  public AttitudeKinematics getAttitudeKinematics(AbsoluteDate date,
                                                  PVCoordinates pv, Frame frame)
      throws OrekitException {
    return ak;
  }
  
  /** The identity attitude */
  private AttitudeKinematics ak;
  
}
