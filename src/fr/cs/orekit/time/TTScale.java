package fr.cs.orekit.time;

/** Terrestrial Time as defined by IAU(1991) recommendation IV.
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

  /** Get the unique instance of this class.
   * @return the unique instance
   */
  public static TimeScale getInstance() {
    if (instance == null) {
      instance = new TTScale();
    }
    return instance;
  }

  /** Get the offset to convert locations from {@link TAIScale}  to instance.
   * @param taiTime location of an event in the {@link TAIScale}  time scale
   * as a seconds index starting at 1970-01-01T00:00:00
   * @return offset to <em>add</em> to taiTime to get a location
   * in instance time scale
   */
  public double offsetFromTAI(double taiTime) {
    return 32.184;
  }

  /** Get the offset to convert locations from instance to {@link TAIScale} .
   * @param instanceTime location of an event in the instance time scale
   * as a seconds index starting at 1970-01-01T00:00:00
   * @return offset to <em>add</em> to instanceTime to get a location
   * in {@link TAIScale}  time scale
   */
  public double offsetToTAI(double instanceTime) {
    return -32.184;
  }

  /** Uniq instance. */
  private static TimeScale instance = null;

}
