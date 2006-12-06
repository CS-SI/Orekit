package fr.cs.aerospace.orekit.models.attitudes;

import org.spaceroots.mantissa.geometry.Rotation;
import org.spaceroots.mantissa.geometry.Vector3D;
import fr.cs.aerospace.orekit.attitudes.AttitudeKinematics;
import fr.cs.aerospace.orekit.attitudes.AttitudeKinematicsProvider;
import fr.cs.aerospace.orekit.bodies.BodyShape;
import fr.cs.aerospace.orekit.bodies.GeodeticPoint;
import fr.cs.aerospace.orekit.bodies.OneAxisEllipsoid;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.orbits.EquinoctialParameters;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;

/** Nadir pointing attitute representation.
 * 
 * <p> It ensures that the X axis of the specraft is pointing verticaly of the 
 *  given {@link BodyShape} surface, and that the Z axis is as close as possible of the
 *  spacecraft velocity direction. <p> 
 * 
 * <p> Perfectly automatised attitude, as it does not consider the
 *  perturbing couples, the captors and spacecraft dynamic.</p>
 *    
 * @author F. Maussion
 */
public class NadirPointingAttitude implements AttitudeKinematicsProvider {

  /** Constructor with any {@link BodyShape}.
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   * @param body the body shpae to point at
   */
  public NadirPointingAttitude(double mu, BodyShape body) {
    this.body = body;
    this.mu = mu;
  }
  
  /** Simple constructor with a classical ellipsoid earth.
   * <p> The earth {@link BodyShape} is a {@link OneAxisEllipsoid}
   * with a equatorial radius of 6378136.460 m and a flatness of 
   * 1.0 / 298.257222101 <p>
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public NadirPointingAttitude(double mu) {
    this.body = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101);
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
    
    GeodeticPoint geo = body.transform(pv.getPosition());
    
    Vector3D direction = new Vector3D(-Math.cos(geo.longitude)*Math.cos(geo.latitude),
                                      -Math.sin(geo.longitude)*Math.cos(geo.latitude), 
                                      -Math.sin(geo.latitude));
    
    Rotation R = new Rotation(direction , pv.getVelocity()  ,
                              Vector3D.plusI, Vector3D.plusK);
    
    EquinoctialParameters ep = new EquinoctialParameters(pv ,frame, mu);    
    
    double a = ep.getA();
    
    Vector3D spin = new Vector3D(Math.sqrt(mu/(a*a*a)), Vector3D.plusJ); 
    return new AttitudeKinematics(R, spin);
    
  }

  /** The body to point at. */
  private BodyShape body;
  
  /** Central body gravitation coefficient */
  private double mu;
}
