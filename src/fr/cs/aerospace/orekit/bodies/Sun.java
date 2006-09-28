package fr.cs.aerospace.orekit.bodies;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.frames.Transform;
import fr.cs.aerospace.orekit.time.AbsoluteDate;

/** Sun model.
 * @version $Id: ThirdBody_Sun.java 831 2003-05-12 16:03:04Z internMS $
 * @author E. Delente
 */

public class Sun extends ThirdBody {

  /** Simple constructor.
   */
  public Sun() {
    super(6.96e8, 1.32712440e20);
  }

  /** Gets the position of the Sun in J2000 frame.
   * <p>The position model is the Newcomb theory
   * as used in the MSLIB library.</p>
   * @param date date
   * @return position of the sun (m)
   */
  public Vector3D getPosition(AbsoluteDate date) {

    double t = date.minus(reference) / 86400.0;
    double f = Math.toRadians(225.768 + 13.2293505 * t);
    double d = Math.toRadians(11.786 + 12.190749 * t);
    double xlp = Math.toRadians(134.003 + 0.9856 * t);
    double g = Math.toRadians(282.551 + 0.000047 * t);
    double e = Math.toRadians(23.44223 - 3.5626E-07 * t);
    double ce = Math.cos(e);
    double se = Math.sin(e);
    double rot = 0.6119022e-6 * date.minus(AbsoluteDate.CNES1950Epoch) / 86400.0;
    double cr = Math.cos(rot);
    double sr = Math.sin(rot);

    // Newcomb's theory
    double cl = 99972.0 * Math.cos(xlp + g) + 1671.0 * Math.cos(xlp + xlp + g)
                - 1678.0 * Math.cos(g);
    cl = cl + 32.0 * Math.cos(3.0 * xlp + g) + Math.cos(4.0 * xlp + g) + 2.0
         * Math.cos(xlp + d + g);
    cl = cl - 4.0 * Math.cos(g - xlp) - 2.0 * Math.cos(xlp - d + g) + 4.0
         * Math.cos(f - d);
    cl = cl - 4.0 * Math.cos(xlp + xlp - f + d + g + g);
    cl = cl * 1.E-05;

    double sl = 99972.0 * Math.sin(xlp + g) + 1671.0 * Math.sin(xlp + xlp + g)
                - 1678.0 * Math.sin(g);
    sl = sl + 32.0 * Math.sin(3.0 * xlp + g) + Math.sin(4.0 * xlp + g) + 2.0
         * Math.sin(xlp + d + g);
    sl = sl - 4.0 * Math.sin(g - xlp) - 2.0 * Math.sin(xlp - d + g) + 4.0
         * Math.sin(f - d);
    sl = sl - 4.0 * Math.sin(xlp + xlp - f + d + g + g);
    sl = sl * 1.E-05;

    double q = Math.sqrt(cl * cl + sl * sl);
    double sx = cl / q;
    double sy = sl * ce / q;
    Vector3D centralSun =
      transform.transformVector(new Vector3D(sx * cr + sy * sr,
                                             sy * cr - sx * sr,
                                             sl * se / q));
    double dasr = 1672.2 * Math.cos(xlp) + 28.0 * Math.cos(xlp + xlp)
                - 0.35 * Math.cos(d);

    return new Vector3D(1000.0 * 149597870.0 / (1.0 + 1.E-05 * dasr), centralSun);

  }

  /** Reference date. */
  private static AbsoluteDate reference =
    new AbsoluteDate(AbsoluteDate.CNES1950Epoch, 864000000.0);

  /** Transform from Veis1950 to J2000. */
  private static Transform transform =
    Frame.getVeis1950().getTransformTo(Frame.getJ2000());

}
