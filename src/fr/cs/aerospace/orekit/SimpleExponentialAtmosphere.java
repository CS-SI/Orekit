package fr.cs.aerospace.orekit;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.bodies.BodyShape;
import fr.cs.aerospace.orekit.bodies.RotatingBody;
import fr.cs.aerospace.orekit.time.AbsoluteDate;

/** Simple exponential atmospheric model.
 * <p>This model represents a simple atmosphere with an exponential
 * density and rigidly bound to the underlying rotating body.</p>
 * @version $Id: Atmosphere.java 840 2006-06-07 09:41:38Z internMS $
 * @author E. Delente
 * @author Luc Maisonobe
 */
public class SimpleExponentialAtmosphere implements Atmosphere {

  /** Create an exponential atmosphere.
   * @param shape body shape model
   * @param motion body rotation model
   * @param rho0 Density at the altitude h0
   * @param h0 Altitude of reference (m)
   * @param hscale Scale factor
   */
  public SimpleExponentialAtmosphere(BodyShape shape, RotatingBody motion,
                                     double rho0, double h0, double hscale) {
    this.shape  = shape;
    this.motion = motion;
    this.rho0   = rho0;
    this.h0     = h0;
    this.hscale = hscale;
  }

  /** Get the density at a given inertial position.
   * @param date current date (ignored in this implementation)
   * @param position current position
   * @return local density (kg/m<sup>3</sup>)
   */
  public double getDensity(AbsoluteDate date, Vector3D position) {
    double altitude = shape.transform(position).altitude;
    return rho0 * Math.exp((h0 - altitude) / hscale);
  }

  /** Get the inertial velocity of atmosphere modecules.
   * @param date current date
   * @param position current position
   * @return volocity (m/s)
   */
  public Vector3D getVelocity(AbsoluteDate date, Vector3D position) {
    return Vector3D.crossProduct(motion.getRotationVector(date), position);
  }

  private BodyShape    shape;
  private RotatingBody motion;
  private double       rho0;
  private double       h0;
  private double       hscale;

}
