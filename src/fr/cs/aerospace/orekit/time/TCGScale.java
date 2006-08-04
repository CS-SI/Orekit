package fr.cs.aerospace.orekit.time;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/** Geocentric Coordinate Time.
 * <p>Coordinate time at the center of mass of the Earth.
 * This time scale depends linearily from {@link TTScale
 * Terrestrial Time}.</p>
 * <p>This is a singleton class, so there is no public constructor.</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 */
public class TCGScale extends TimeScale {

  // reference time scale
  private static final TimeScale tt;

  // reference time tor TCG is 1977-01-01 (MJD = 43144)
  private static final double reference;

  static {
    try {
      tt = TTScale.getInstance();
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
      reference = format.parse("1977-01-01").getTime();
    } catch (ParseException pe) {
      throw new RuntimeException("internal error"); // should not happen
    }
  }

  /** Private constructor for the singleton.
   */
  private TCGScale() {
    super("TCG");
  }

  /* Get the uniq instance of this class.
   * @return the uniq instance
   */
  public static TimeScale getInstance() {
    if (instance == null) {
      instance = new TCGScale();
    }
    return instance;
  }

  /** Get the offset to convert locations from {@link TAI} to instance.
   * @param taiTime location of an event in the {@link TAI} time scale
   * as a seconds index starting at 1970-01-01T00:00:00
   * @return offset to <em>add</em> to taiTime to get a location
   * in instance time scale
   */
  public double offsetFromTAI(double taiTime) {
    double ttOffset = tt.offsetFromTAI(taiTime);
    return ttOffset + lg * (ttOffset + taiTime - reference);
  }

  /** Get the offset to convert locations from instance to {@link TAI}.
   * @param instanceTime location of an event in the instance time scale
   * as a seconds index starting at 1970-01-01T00:00:00
   * @return offset to <em>add</em> to instanceTime to get a location
   * in {@link TAI} time scale
   */
  public double offsetToTAI(double instanceTime) {
    double ttTime = inverse * (instanceTime + lg * reference);
    return tt.offsetToTAI(ttTime) - lg * inverse * (instanceTime - reference);
  }

  /** Uniq instance. */
  private static TimeScale instance = null;

  /** LG rate. */
  private static double lg = 6.969290134e-10;

  /** Inverse rate. */
  private static double inverse = 1.0 / (1.0 + lg);

}
