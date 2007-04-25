package fr.cs.aerospace.orekit.models.perturbations;

import org.spaceroots.mantissa.geometry.Vector3D;
import fr.cs.aerospace.orekit.bodies.BodyShape;
import fr.cs.aerospace.orekit.bodies.GeodeticPoint;
import fr.cs.aerospace.orekit.bodies.ThirdBody;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.forces.perturbations.AtmosphericDrag;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.frames.Transform;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;

/** This class is the OREKIT compliant realization of the JB2006 atmosphere model.
 * 
 * It should be instancied to be used by the {@link AtmosphericDrag drag force model} as it 
 * implements the {@link Atmosphere} interface.
 *  
 *  The input parameters are computed with orbital state information, but solar
 *  activity and magnetic acivity datas must be provided by the user threw
 *  the interface {@link JB2006InputParameters}.
 *  
 * @author F. Maussion
 * @see JB2006Atmosphere
 */
public class JB2006AtmosphereModel extends JB2006Atmosphere implements
                                                           Atmosphere {
  
  /** Constructor with space environment information for internal computation.
   * @param parameters the solar and magnetic activity datas
   * @param sun the sun position
   * @param earth the earth body shape
   * @param earthFixed the earth fixed frame
   */
  public JB2006AtmosphereModel(JB2006InputParameters parameters, 
                                ThirdBody sun, BodyShape earth, Frame earthFixed) {
    super();
    this.earth = earth;
    this.sun = sun;
    this.inputParams = parameters;
    this.bodyFrame = earthFixed;       
  }
  
  /** Get the local density.
   * @param date current date
   * @param position current position in frame
   * @param frame the frame in which is defined the position
   * @return local density (kg/m<sup>3</sup>)
   * @throws OrekitException 
   */
  public double getDensity(AbsoluteDate date, Vector3D position, Frame frame)
      throws OrekitException {
    // check if datas are available :
    if(date.compareTo(inputParams.getMaxDate())>0 ||
        date.compareTo(inputParams.getMinDate())<0) {
      throw new OrekitException("Current date is out of range. " + 
                                "Solar activity datas are not available",
                                new String[0]);      
    }

    // compute modifed julian days date
    double dateMJD = date.minus(AbsoluteDate.ModifiedJulianEpoch);
    dateMJD /= 84000.;
    
    // compute geodetic position
    Vector3D posInBody = frame.getTransformTo(bodyFrame, date).transformPosition(position);
    GeodeticPoint inBody = earth.transform(posInBody);
   
    // compute sun position
    Vector3D sunPosInBody = frame.getTransformTo(bodyFrame, date).transformPosition(sun.getPosition(date, frame));
    GeodeticPoint sunInBody = earth.transform(sunPosInBody);
    
    return getDensity(dateMJD, sunInBody.longitude, sunInBody.latitude, inBody.longitude, inBody.latitude,
                      inBody.altitude, inputParams.getF10(date), inputParams.getF10B(date), 
                      inputParams.getAp(date), inputParams.getS10(date),
                      inputParams.getS10B(date), inputParams.getXM10(date), 
                      inputParams.getXM10B(date));
  }

  /** Get the inertial velocity of atmosphere molecules.
   * Here the case is simplified : atmosphere is supposed to have a null velocity
   * in earth frame.
   * @param date current date
   * @param position current position in frame
   * @param frame the frame in which is defined the position
   * @return velocity (m/s) (defined in the same frame as the position)
   * @throws OrekitException 
   */
  public Vector3D getVelocity(AbsoluteDate date, Vector3D position, Frame frame)
  throws OrekitException {
    Transform bodyToFrame = bodyFrame.getTransformTo(frame, date);
    Vector3D posInBody = bodyToFrame.getInverse().transformPosition(position);
    PVCoordinates pvBody = new PVCoordinates(posInBody, new Vector3D(0, 0, 0));
    PVCoordinates pvFrame = bodyToFrame.transformPVCoordinates(pvBody);
    return pvFrame.getVelocity();
  }
  
  /** Sun position */
  private ThirdBody sun;
  /** External data container */
  private JB2006InputParameters inputParams;
  /** Earth body shape */
  private BodyShape earth;
  /** Earth fixed frame */
  private Frame bodyFrame;  
  
}
