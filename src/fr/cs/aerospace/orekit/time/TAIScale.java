package fr.cs.aerospace.orekit.time;

/** International Atomic Time.
 * <p>This is a singleton class, so there is no public constructor.</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 */
public class TAIScale extends TimeScale {

  /** Private constructor for the singleton.
   */
  private TAIScale() {
    super("TAI");
  }

  /* Get the uniq instance of this class.
   * @return the uniq instance
   */
  public static TimeScale getInstance() {
    if (instance == null) {
      instance = new TAIScale();
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
    return taiTime;
  }

  /** Convert a location in this time scale into {@link TAI} time scale.
   * @param instanceTime location of an event in the instance time scale
   * as a seconds index starting at 1970-01-01T00:00:00
   * @return location of the same event in the {@link TAI} time scale
   * as a seconds index starting at 1970-01-01T00:00:00
   */
  public double toTAI(double instanceTime) {
    return instanceTime;
  }

  /** Uniq instance. */
  private static TimeScale instance = null;

}
