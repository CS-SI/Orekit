package fr.cs.aerospace.orekit.time;

/** Terrestrial Time as defined by IAU(1991) recommandation IV.
 * <p>Coordinate time at the surface of the Earth. IT is the
 * successor of Ephemeris Time TE.</p>
 * <p>By convention, TT = TAI + 32.184 s.</p>
 * <p>This is a singleton class, so there is no public constructor.</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 */
public class TTScale extends TimeScale {

  /** Private constructor for the singleton.
   */
  private TTScale() {
    super("TT");
  }

  /* Get the uniq instance of this class.
   * @return the uniq instance
   */
  public static TimeScale getInstance() {
    if (instance == null) {
      instance = new TTScale();
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
    return taiTime + 32.184;
  }

  /** Convert a location in this time scale into {@link TAI} time scale.
   * @param instanceTime location of an event in the instance time scale
   * as a seconds index starting at 1970-01-01T00:00:00
   * @return location of the same event in the {@link TAI} time scale
   * as a seconds index starting at 1970-01-01T00:00:00
   */
  public double toTAI(double instanceTime) {
    return instanceTime - 32.184;
  }

  /** Uniq instance. */
  private static TimeScale instance = null;

}
