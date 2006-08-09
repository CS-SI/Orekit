package fr.cs.aerospace.orekit.frames;

/** Simple container class for pole correction parameters.
 * <p>This class is a simple container, it does not provide
 * any processing method.</p>
 * @author Luc Maisonobe
 */
public class PoleCorrection {

  /** Null correction (xp = 0, yp = 0). */
  public static final PoleCorrection NULL_CORRECTION =
    new PoleCorrection(0, 0);

  /** x<sub>p</sub> parameter (radians). */
  public final double xp;

  /** y<sub>p</sub> parameter (radians). */
  public final double yp;

  /** Simple constructor.
   * @param xp x<sub>p</sub> parameter (radians)
   * @param yp y<sub>p</sub> parameter (radians)
   */
  public PoleCorrection(double xp, double yp) {
    this.xp = xp;
    this.yp = yp;
  }

}