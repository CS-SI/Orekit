package fr.cs.orekit.attitudes.models;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import fr.cs.orekit.attitudes.AttitudeKinematics;
import fr.cs.orekit.attitudes.AttitudeKinematicsProvider;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.utils.PVCoordinates;

/** Earth centered attitute representation.
 * 
 * <p> It ensures that the Z axis of the specraft is pointing on the inertial
 * frame origin, and that the Y axis is orthogonal to the orbital plane. <p> 
 * 
 * <p> Perfectly automatised attitude, as it does not consider the
 *  perturbing couples, the captors and spacecraft dynamic.</p>
 *    
 * @author F. Maussion
 */
public class EarthCenteredAttitude implements AttitudeKinematicsProvider {

  /** Simple Constructor.
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public EarthCenteredAttitude(double mu) {
    this.mu = mu;
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
    
    Vector3D pos = pv.getPosition().negate();
    Vector3D vel = pv.getPosition();
    Rotation R = new Rotation(pos , Vector3D.crossProduct(pos, vel),
                              Vector3D.plusK, Vector3D.plusJ);
    
    //  compute semi-major axis
    double r       = pv.getPosition().getNorm();
    double V2      = Vector3D.dotProduct(pv.getVelocity(), pv.getVelocity());
    double rV2OnMu = r * V2 / mu;
    double a       = r / (2 - rV2OnMu);
    
    Vector3D spin = new Vector3D(Math.sqrt(mu/(a*a*a)), Vector3D.minusJ); 
    
    return new AttitudeKinematics(R, spin);
  }
  
  /** Central body gravitation constant */
  private double mu;
  
}
