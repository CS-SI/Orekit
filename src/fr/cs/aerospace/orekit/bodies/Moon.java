package fr.cs.aerospace.orekit.bodies;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.frames.Transform;
import fr.cs.aerospace.orekit.time.AbsoluteDate;

/** Moon model.
 * @version $Id: ThirdBody_Moon.java 831 2003-05-12 16:03:04Z internMS $
 * @author E. Delente
 */

public class Moon extends ThirdBody {

  /**
   * Creates a new instance of ThirdBody_Moon.
   */
  public Moon() {
    super(1737400.0, 4.9027989e12);
  }

  /** Gets the position of the Moon in J2000 frame.
   * <p>The position model is the Brown theory
   * as used in the MSLIB library.</p>
   * @param date current date
   * @return position of the Moon wrt the central body (m)
   */
  public Vector3D getPosition(AbsoluteDate date) {

    double t = date.minus(reference) / 86400.0;
    double f = Math.toRadians(225.768 + 13.2293505 * t);
    double xl = Math.toRadians(185.454 + 13.064992 * t);
    double d = Math.toRadians(11.786 + 12.190749 * t);
    double xlp = Math.toRadians(134.003 + 0.9856 * t);
    double e = Math.toRadians(23.44223 - 3.5626e-07 * t);
    double ce = Math.cos(e);
    double se = Math.sin(e);
    double rot = 0.6119022E-06 * date.minus(AbsoluteDate.CNES1950Epoch) / 86400.0;
    double cr = Math.cos(rot);
    double sr = Math.sin(rot);

    // Brown's theory
    double dl = 10976.0 * Math.sin(xl) - 2224.0 * Math.sin(xl - d - d) + 1149.0
                * Math.sin(d + d);
    dl = dl + 373.0 * Math.sin(xl + xl) - 324.0 * Math.sin(xlp) - 200.0
         * Math.sin(f + f);
    dl = dl - 103.0 * Math.sin(xl + xl - d - d) - 100.0
         * Math.sin(xl + xlp - d - d);
    dl = dl + 93.0 * Math.sin(xl + d + d);
    dl = dl - 80.0 * Math.sin(xlp - d - d) + 72.0 * Math.sin(xl - xlp) - 61.0
         * Math.sin(d);
    dl = dl - 53.0 * Math.sin(xl + xlp);
    dl = dl + 14.0 * Math.sin(xl - xlp - d - d) + 19.0 * Math.sin(xl - f - f);
    dl = dl - 19.0 * Math.sin(xl - 4.0 * d);
    dl = dl + 17.0 * Math.sin(3.0 * xl) - 27.0 * Math.sin(f + f - d - d);
    dl = dl - 12.0 * Math.sin(xlp + d + d);
    dl = dl - 22.0 * Math.sin(xl + f + f) - 15.0 * Math.sin(xl + xl - 4.0 * d);
    dl = dl + 7.0 * Math.sin(xl + xl + d + d) + 9.0 * Math.sin(xl - d);
    dl = dl - 6.0 * Math.sin(3.0 * xl - d - d);
    dl = dl + 7.0 * Math.sin(4.0 * d) + 9.0 * Math.sin(xlp + d) + 7.0
         * Math.sin(xl - xlp + d + d);
    dl = dl + 5.0 * Math.sin(xl + xl - xlp);
    dl = dl * 1.E-05;

    double b = 8950.0 * Math.sin(f) + 490.0 * Math.sin(xl + f) + 485.0
               * Math.sin(xl - f);
    b = b - 302.0 * Math.sin(f - d - d);
    b = b - 97.0 * Math.sin(xl - f - d - d) - 81.0 * Math.sin(xl + f - d - d);
    b = b + 57.0 * Math.sin(f + d + d);
    b = b - 14.0 * Math.sin(xlp + f - d - d) + 16.0 * Math.sin(xl - f + d + d);
    b = b + 15.0 * Math.sin(xl + xl - f) + 30.0 * Math.sin(xl + xl + f);
    b = b - 6.0 * Math.sin(xlp - f + d + d) - 7.0
        * Math.sin(xl + xl + f - d - d);
    b = b + 7.0 * Math.sin(xl + f + d + d);
    b = b * 1.E-05;

    double u = Math.toRadians(68.341 + 13.176397 * t) + dl;
    double cu = Math.cos(u);
    double su = Math.sin(u);
    double cb = Math.cos(b);
    double sb = Math.sin(b);
    double rx = cu * cb;
    double ry = su * cb * ce - sb * se;

    Vector3D centralMoon =
      transform.transformVector(new Vector3D(rx * cr + ry * sr,
                                             ry * cr - rx * sr,
                                             sb * ce + su * cb * se));

    double dasr = 5450.0 * Math.cos(xl) + 1002.0 * Math.cos(xl - d - d) + 825.0
                  * Math.cos(d + d);
    dasr = dasr + 297.0 * Math.cos(xl + xl) + 90.0 * Math.cos(xl + d + d);
    dasr = dasr + 56.0 * Math.cos(xlp - d - d);
    dasr = dasr + 42.0 * Math.cos(xl + xlp - d - d) + 34.0 * Math.cos(xl - xlp);
    dasr = dasr - 12.0 * Math.cos(xlp) - 29.0 * Math.cos(d) - 21.0
           * Math.cos(xl - f - f);
    dasr = dasr + 18.0 * Math.cos(xl - 4.0 * d) - 28.0 * Math.cos(xl + xlp);
    dasr = dasr + 11.0 * Math.cos(xl + xl - 4.0 * d) + 18.0
           * Math.cos(3.0 * xl);
    dasr = dasr - 9.0 * Math.cos(xlp + d + d) - 7.0
           * Math.cos(xl - xlp - d - d);
    dasr = dasr + 7.0 * Math.cos(xl - xlp + d + d);
    dasr = dasr - 9.0 * Math.cos(xl + xl - d - d) + 8.0
           * Math.cos(xl + xl + d + d);
    dasr = dasr + 8.0 * Math.cos(4.0 * d);

    return new Vector3D(1000.0 * 384389.3 / (1.0 + 1.E-05 * dasr),
                        centralMoon);

  }

  /** Reference date. */
  private static AbsoluteDate reference =
    new AbsoluteDate(AbsoluteDate.CNES1950Epoch, 864000000.0);

  /** Transform from Veis1950 to J2000. */
  private static Transform transform =
    Frame.getVeis1950().getTransformTo(Frame.getJ2000());

}
