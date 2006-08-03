package fr.cs.aerospace.orekit.time;

/** Base class for time scales.
 * <p>This is the base class for all time scales. Time scales are related
 * to each other by some offsets that may be discontinuous (for example
 * the {@link UTC UTC} with respect to the {@link TAI TAI}).</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 */
public abstract class TimeScale {

  /** Simple constructor.
   * @param name name of the time scale
   */
  protected TimeScale(String name) {
    this.name = name;
  }

  /** Convert a location in {@link TAI} time scale into the instance time scale.
   * @param taiTime location of an event in the {@link TAI} time scale
   * as a seconds index starting at 1970-01-01T00:00:00
   * @return location of the same event in the instance time scale
   * as a seconds index starting at 1970-01-01T00:00:00
   */
  public abstract double fromTAI(double taiTime);

  /** Convert a location in this time scale into {@link TAI} time scale.
   * @param instanceTime location of an event in the instance time scale
   * as a seconds index starting at 1970-01-01T00:00:00
   * @return location of the same event in the {@link TAI} time scale
   * as a seconds index starting at 1970-01-01T00:00:00
   */
  public abstract double toTAI(double instanceTime);

  /** Convert the instance to a string (the name of the time scale).
   * @return string representation of the time scale (standard abreviation)
   */
  public String toString() {
    return name;
  }

  /** Name of the time scale. */
  private final String name;

}
