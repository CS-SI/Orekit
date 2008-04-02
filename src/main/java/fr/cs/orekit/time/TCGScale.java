package fr.cs.orekit.time;

/** Geocentric Coordinate Time.
 * <p>Coordinate time at the center of mass of the Earth.
 * This time scale depends linearly from {@link TTScale
 * Terrestrial Time}.</p>
 * <p>This is a singleton class, so there is no public constructor.</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 */
public class TCGScale extends TimeScale {

    /** Unique instance. */
    private static TimeScale instance = null;

    /** LG rate. */
    private static double lg = 6.969290134e-10;

    /** Inverse rate. */
    private static double inverse = 1.0 / (1.0 + lg);

    // reference time scale
    private static final TimeScale tt = TTScale.getInstance();

    // reference time for TCG is 1977-01-01 (2557 days after 1970-01-01)
    private static final double reference = 2557l * 86400000l;

    /** Private constructor for the singleton.
     */
    private TCGScale() {
        super("TCG");
    }

    /** Get the unique instance of this class.
     * @return the unique instance
     */
    public static TimeScale getInstance() {
        if (instance == null) {
            instance = new TCGScale();
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
        final double ttOffset = tt.offsetFromTAI(taiTime);
        return ttOffset + lg * (ttOffset + taiTime - reference);
    }

    /** Get the offset to convert locations from instance to {@link TAIScale} .
     * @param instanceTime location of an event in the instance time scale
     * as a seconds index starting at 1970-01-01T00:00:00
     * @return offset to <em>add</em> to instanceTime to get a location
     * in {@link TAIScale}  time scale
     */
    public double offsetToTAI(double instanceTime) {
        final double ttTime = inverse * (instanceTime + lg * reference);
        return tt.offsetToTAI(ttTime) - lg * inverse * (instanceTime - reference);
    }

}
