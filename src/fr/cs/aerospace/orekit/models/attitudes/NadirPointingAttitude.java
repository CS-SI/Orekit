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
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;
//TODO Approximative Javadoc
/** Nadir pointing attitute representation.
 * 
 * <p> Two simple ways to define this attitude have been implemented :
 * 
 *  <p> - The first one ({@link #PURENADIR}) ensures that the Z axis of
 *  the specraft is pointing orthogonaly on the given {@link BodyShape} surface, 
 *  and that the X axis is as close as possible of the spacecraft velocity direction,
 *  but not necessarily in the orbital plane. Actually, this direction depends
 *  on the bodyshape and the orbit inclination </p> 
 *  
 *  <p> - The second one ({@link #ORBITALPLANE}) ensures that the Y axis of 
 *  the specraft is exactly orthogonal to the orbital plane, wich contains
 *  the X and Z axis. So the Z axis direction is as close as possible of 
 *  the {@link BodyShape} surface normale. </p>
 *   
 * </p>
 * 
 * <p> Perfectly automatised attitude, as it does not consider the
 *  perturbing couples, the captors and spacecraft dynamic.</p>
 * 
 * @author F. Maussion
 */
public class NadirPointingAttitude implements AttitudeKinematicsProvider {

  /** Identifier for the pure Nadir attitude. */
  public static final int PURENADIR = 0;

  /** Identifier for the "orbital plane oriented" attitude. */
  public static final int ORBITALPLANE = 1;

  /** Constructor with any {@link BodyShape}.
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   * @param body the body shape to point at
   * @param type, {@link #PURENADIR} or {@link #ORBITALPLANE}
   * @param pitchBias the bias around Y
   * @param rollBias the bias around K (applied after the pitch)
   */
  public NadirPointingAttitude(double mu, BodyShape body, int type, 
                               double pitchBias, double rollBias) {
    this.body = body;
    this.mu = mu;
    this.type = type;
    this.roll = rollBias;
    this.pitch = pitchBias;
  }

  /** Simple constructor with a classical ellipsoid earth.
   * <p> The earth {@link BodyShape} is a {@link OneAxisEllipsoid}
   * with a equatorial radius of 6378136.460 m and a flatness of 
   * 1.0 / 298.257222101 <p>
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   * @param type, {@link #PURENADIR} or {@link #ORBITALPLANE}
   * @param pitchBias the bias around Y
   * @param rollBias the bias around K (applied after the pitch)
   */
  public NadirPointingAttitude(double mu, int type, 
                               double pitchBias, double rollBias) {
    this.body = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101);
    this.mu = mu;
    this.type = type;
    this.roll = rollBias;
    this.pitch = pitchBias;
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

    // define nadir pointing attitude
    GeodeticPoint geo = body.transform(pv.getPosition());    
    Vector3D direction = new Vector3D(geo.longitude,geo.latitude);
    Rotation R;
    switch (type) {
    case PURENADIR :
      R = new Rotation(direction , pv.getVelocity()  ,
                       Vector3D.minusK, Vector3D.plusI);
      break;
    case ORBITALPLANE :
      Vector3D angMom = Vector3D.crossProduct(pv.getVelocity(), pv.getPosition());
      R = new Rotation(angMom , direction  ,
                       Vector3D.plusJ, Vector3D.minusK);
      break;
    default :
      throw new IllegalArgumentException(" Attitude type is not correct ");
    }

    Rotation pitch = new Rotation(Vector3D.plusJ, this.pitch);
    Rotation roll = new Rotation(Vector3D.plusK, this.roll);
    R = roll.applyTo(pitch.applyTo(R));
    
    //  compute semi-major axis
    double r       = pv.getPosition().getNorm();
    double V2      = Vector3D.dotProduct(pv.getVelocity(), pv.getVelocity());
    double rV2OnMu = r * V2 / mu;
    double a       = r / (2 - rV2OnMu);

    // TODO Spin is not rigorously exact
    Vector3D spin = new Vector3D(Math.sqrt(mu/(a*a*a)), Vector3D.plusJ); 

    return new AttitudeKinematics(R, spin);

  }

  /** The body to point at. */
  private BodyShape body;

  /** Type of attitude. */
  private int type;

  /** Central body gravitation constant */
  private double mu;
  
  /** Bias */
  private double roll;
  private double pitch;
}
