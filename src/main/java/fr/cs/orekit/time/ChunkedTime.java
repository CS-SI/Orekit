package fr.cs.orekit.time;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import fr.cs.orekit.errors.OrekitException;

/** Class representing a time within the day as hour, minute and second chunks.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 */
public class ChunkedTime implements Serializable, Comparable {

    /** Serializable UID. */
    private static final long serialVersionUID = 4492296986280760487L;

    /** Constant for commonly used hour 00:00:00. */
    public static final ChunkedTime H00   = new ChunkedTime(0, 0, 0);

    /** Constant for commonly used hour 12:00:00. */
    public static final ChunkedTime H12 = new ChunkedTime(12, 0, 0);

    /** Format for hours and minutes. */
    private static final DecimalFormat twoDigits = new DecimalFormat("00");

    /** Format for seconds. */
    private static final DecimalFormat secondsDigits =
        new DecimalFormat("00.000", new DecimalFormatSymbols(Locale.US));

    /** Hour number. */
    public final int hour;

    /** Minute number. */
    public final int minute;

    /** Second number. */
    public final double second;

    /** Build a time from its clock elements.
     * @param hour hour number from 0 to 23
     * @param minute minute number from 0 to 59
     * @param second second number from 0.0 to 60.0 (excluded)
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range)
     */
    public ChunkedTime(int hour, int minute, double second) throws IllegalArgumentException {

        // range check
        if ((hour   < 0) || (hour   >  23) ||
                (minute < 0) || (minute >  59) ||
                (second < 0) || (second >= 60.0)) {
            OrekitException.throwIllegalArgumentException("non-existent hour {0}:{1}:{2}",
                                                          new Object[] {
                                                              new Integer(hour),
                                                              new Integer(minute),
                                                              new Double(second)
                                                          });
        }

        this.hour = hour;
        this.minute = minute;
        this.second = second;

    }

    /** Build a time from the second number within the day.
     * @param secondInDay second number from 0.0 to 86400.0 (excluded)
     * @exception IllegalArgumentException if seconds number is out of range
     */
    public ChunkedTime(double secondInDay) {
        // range check
        if ((secondInDay < 0) || (secondInDay >= 86400.0)) {
            OrekitException.throwIllegalArgumentException("out of range seconds number: {0}",
                                                          new Object[] {
                                                              new Double(secondInDay)
                                                          });
        }

        // extract the time chunks
        hour = (int) Math.floor(secondInDay / 3600.0);
        secondInDay -= hour * 3600;
        minute = (int) Math.floor(secondInDay / 60.0);
        secondInDay -= minute * 60;
        second = secondInDay;

    }

    /** Get the second number within the day.
     * @return second number from 0.0 to 86400.0
     */
    public double getSecondsInDay() {
        return second + 60 * minute + 3600 * hour;
    }

    /** Get a string representation of the time.
     * @return string representation of the time
     */
    public String toString() {
        return new StringBuffer().
        append(twoDigits.format(hour)).append(':').
        append(twoDigits.format(minute)).append(':').
        append(secondsDigits.format(second)).
        toString();
    }

    /** {@inheritDoc} */
    public int compareTo(Object other) {
        double seconds = getSecondsInDay();
        double otherSeconds = ((ChunkedTime) other).getSecondsInDay();
        if (seconds < otherSeconds) {
            return -1;
        } else if (seconds > otherSeconds) {
            return 1;
        }
        return 0;
    }

    /** {@inheritDoc} */
    public boolean equals(Object other) {
        try {
            ChunkedTime otherTime = (ChunkedTime) other;
            return (otherTime != null) && (hour == otherTime.hour) &&
                   (minute == otherTime.minute) && (second == otherTime.second);
        } catch (ClassCastException cce) {
            return false;
        }
    }

    /** {@inheritDoc} */
    public int hashCode() {
        long bits = Double.doubleToLongBits(second);
        return ((hour << 8) | (minute << 8)) ^ (int) (bits ^ (bits >>> 32));
    }

}
