package fr.cs.aerospace.orekit.models.perturbations;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.bodies.BodyShape;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.frames.Transform;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;

/** Simple exponential atmospheric model.
 * <p>This model represents a simple atmosphere with an exponential
 * density and rigidly bound to the underlying rotating body.</p>
 * @version $Id: Atmosphere.java 840 2006-06-07 09:41:38Z internMS $
 * @author F.Maussion
 * @author L.Maisonobe
 */
public class SimpleExponentialAtmosphere implements Atmosphere {

  /** Create an exponential atmosphere.
   * @param shape body shape model
   * @param bodyFrame body rotation frame
   * @param rho0 Density at the altitude h0
   * @param h0 Altitude of reference (m)
   * @param hscale Scale factor
   */
  public SimpleExponentialAtmosphere(BodyShape shape, Frame bodyFrame,
                                     double rho0, double h0, double hscale) {
    this.shape  = shape;
    this.bodyFrame = bodyFrame;
    this.rho0   = rho0;
    this.h0     = h0;
    this.hscale = hscale;
  }

  /** Get the density at a given inertial position.
   * @param date current date (ignored in this implementation)
   * @param position current position
   * @param frame the Frame in which is defined the position
   * @return local density (kg/m<sup>3</sup>)
   */
  public double getDensity(AbsoluteDate date, Vector3D position, Frame frame) {
    double altitude = shape.transform(position).altitude;
    return rho0 * Math.exp((h0 - altitude) / hscale);
  }

  /** Get the inertial velocity of atmosphere modecules.
   * Here the case is simplified : atmosphere is supposed to have a null velocity
   * in earth frame.
   * @param date current date
   * @param position current position in frame
   * @param frame the frame in which is defined the position
   * @return velocity (m/s) (defined in the same frame than the position)
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

  private BodyShape    shape;
  private Frame        bodyFrame;
  private double       rho0;
  private double       h0;
  private double       hscale;

}
