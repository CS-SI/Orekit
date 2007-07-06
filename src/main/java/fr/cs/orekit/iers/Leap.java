package fr.cs.orekit.iers;

import fr.cs.orekit.time.UTCScale;

/** UTC Time steps.
 * <p>This class is a simple container.</p>
 * @author Luc Maisonobe
 * @see UTCScale
 */
public class Leap {

  /** Time in UTC at which the step occurs. */
  public final double utcTime;

  /** Step value. */
  public final double step;

  /** Offset in seconds after the leap. */
  public final double offsetAfter;

  /** Simple constructor.
   * @param utcTime time in UTC at which the step occurs
   * @param step step value
   * @param offsetAfter offset in seconds after the leap
   */
  public Leap(double utcTime, double step, double offsetAfter) {
    this.utcTime     = utcTime;
    this.step        = step;
    this.offsetAfter = offsetAfter;
  }

}
