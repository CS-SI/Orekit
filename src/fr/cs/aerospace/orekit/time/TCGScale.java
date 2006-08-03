package fr.cs.aerospace.orekit.time;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/** Geocentric Coordinate Time.
 * <p>Coordinate time at the center of mass of the Earth.
 * This time scale depends linearily from {@link TTScale
 * Terrestrial Time}.</p>
 * <p>This is a singleton class, so there is no public constructor.</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 */
public class TCGScale extends TimeScale {

  // reference time tor TCG is 1977-01-01 (MJD = 43144)
  private static final double reference;

  static {
    try {
      SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd");
      iso8601.setTimeZone(TimeZone.getTimeZone("UTC"));
      reference = iso8601.parse("1977-01-01").getTime();
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

  /** Convert a location in {@link TAI} time scale into the instance time scale.
   * @param taiTime location of an event in the {@link TAI} time scale
   * as a seconds index starting at 1970-01-01T00:00:00
   * @return location of the same event in the instance time scale
   * as a seconds index starting at 1970-01-01T00:00:00
   */
  public double fromTAI(double taiTime) {
    double ttTime = TTScale.getInstance().fromTAI(taiTime);
    return ttTime + lg * (ttTime - reference);
  }

  /** Convert a location in this time scale into {@link TAI} time scale.
   * @param instanceTime location of an event in the instance time scale
   * as a seconds index starting at 1970-01-01T00:00:00
   * @return location of the same event in the {@link TAI} time scale
   * as a seconds index starting at 1970-01-01T00:00:00
   */
  public double toTAI(double instanceTime) {
    double ttTime = (instanceTime + lg * reference) / (1 + lg);
    return TTScale.getInstance().toTAI(ttTime);
  }

  /** Uniq instance. */
  private static TimeScale instance = null;

  /** LG rate. */
  private static double lg = 6.969290134e-10;

}
