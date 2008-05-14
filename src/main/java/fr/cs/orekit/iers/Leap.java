package fr.cs.orekit.iers;

/** UTC Time steps.
 * <p>This class is a simple container.</p>
 * @author Luc Maisonobe
 * @see fr.cs.orekit.time.UTCScale
 */
public class Leap {

    /** Time in UTC at which the step occurs. */
    private final double utcTime;

    /** Step value. */
    private final double step;

    /** Offset in seconds after the leap. */
    private final double offsetAfter;

    /** Simple constructor.
     * @param utcTime time in UTC at which the step occurs
     * @param step step value
     * @param offsetAfter offset in seconds after the leap
     */
    public Leap(final double utcTime, final double step,
                final double offsetAfter) {
        this.utcTime     = utcTime;
        this.step        = step;
        this.offsetAfter = offsetAfter;
    }

    /** Get the time in UTC at which the step occurs.
     * @return time in UTC at which the step occurs.
     */
    public double getUtcTime() {
        return utcTime;
    }

    /** Get the step value.
     * @return step value.
     */
    public double getStep() {
        return step;
    }

    /** Get the offset in seconds after the leap.
     * @return offset in seconds after the leap.
     */
    public double getOffsetAfter() {
        return offsetAfter;
    }

}
