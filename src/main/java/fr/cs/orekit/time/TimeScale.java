package fr.cs.orekit.time;

/** Base class for time scales.
 * <p>This is the base class for all time scales. Time scales are related
 * to each other by some offsets that may be discontinuous (for example
 * the {@link UTCScale UTC scale} with respect to the {@link TAIScale
 * TAI scale}).</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 */
public abstract class TimeScale {

    /** Name of the time scale. */
    private final String name;

    /** Simple constructor.
     * @param name name of the time scale
     */
    protected TimeScale(final String name) {
        this.name = name;
    }

    /** Get the offset to convert locations from {@link TAIScale}  to instance.
     * @param taiTime location of an event in the {@link TAIScale}  time scale
     * as a seconds index starting at 1970-01-01T00:00:00
     * @return offset to <em>add</em> to taiTime to get a location
     * in instance time scale
     */
    public abstract double offsetFromTAI(double taiTime);

    /** Get the offset to convert locations from instance to {@link TAIScale} .
     * @param instanceTime location of an event in the instance time scale
     * as a seconds index starting at 1970-01-01T00:00:00
     * @return offset to <em>add</em> to instanceTime to get a location
     * in {@link TAIScale}  time scale
     */
    public abstract double offsetToTAI(double instanceTime);

    /** Convert the instance to a string (the name of the time scale).
     * @return string representation of the time scale (standard abreviation)
     */
    public String toString() {
        return name;
    }

}
