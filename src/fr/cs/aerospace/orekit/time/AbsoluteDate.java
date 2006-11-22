package fr.cs.aerospace.orekit.time;

import java.util.Date;
import java.util.TimeZone;
import java.io.Serializable;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import fr.cs.aerospace.orekit.errors.OrekitException;

/** This class represents a specific instant in time.

 * <p>Instances of this class are considered to be absolute in the sense
 * that each one represent the occurrence of some event and can be compared
 * to other instances or located in any {@link TimeScale time scale}. In
 * order to represent a specific event instant in two different time scales
 * (say {@link TAIScale TAI} and {@link UTCScale UTC} for example), only
 * one instance is needed, the representations are made available by
 * calling the appropriate methods on this instance several time with
 * several different time scales as parameters. Two complementary views are
 * available:</p>
 * <ul>
 *   <li><p>location view (mainly for input/output or conversions)</p>
 *   <p>locations represent the coordinate of one event with respect to a
 *   {@link TimeScale time scale}. The related methods are {@link
 *   #AbsoluteDate(Date, TimeScale) AbsoluteDate(location, timeScale)}, {@link
 *   #AbsoluteDate(String, TimeScale) AbsoluteDate(location, timeScale)},
 *    {@link #toDate}, {@link #toString() toString()}, {@link #toString(TimeScale)
 *   toString(timeScale)} and {@link #timeScalesOffset}.</p>
 *   </li>
 *   <li><p>offset view (mainly for physical computation)</p>
 *   <p>offsets represent either the flow of time between two events
 *   (two instances of the class) or durations. They are counted in seconds,
 *   are continuous and could be measured using only a virtual perfect stopwatch.
 *   The related methods are {@link #AbsoluteDate(AbsoluteDate, double) AbsoluteDate(instant,
 *   offset)}, {@link #AbsoluteDate(AbsoluteDate) AbsoluteDate(instant)},
 *   {@link #minus}.</p>
 *   </li>
 * </ul>
 * <p>
 * The instance <code>AbsoluteDate</code> is guaranted to be immutable.
 * </p>
 * @author L. Maisonobe
 * @see TimeScale
 */
public class AbsoluteDate implements Comparable, Serializable {

    /** Reference epoch for julian dates: -4712-01-01T12:00:00.
     * <p>The java.util.Date class follows the astronomical convention
     * and uses a year 0 between years -1 and +1, hence this reference
     * date is in year -4712 and not in year -4713 as can be seen in
     * other documents that obey a different convention.</p>
     */
    public static final AbsoluteDate JulianEpoch;
    
    /** Reference epoch for modified julian dates: 1858-11-17T00:00:00. */
    public static final AbsoluteDate ModifiedJulianEpoch;
    
    /** Reference epoch for CNES 1950 dates: 1950-01-01T00:00:00. */
    public static final AbsoluteDate CNES1950Epoch;
    
    /** Reference epoch for GPS weeks: 1980-01-06T00:00:00. */
    public static final AbsoluteDate GPSEpoch;

    /** J2000.0 Reference epoch: 2000-01-01T12:00:00 TT. */
    public static final AbsoluteDate J2000Epoch;

    /** Java Reference epoch: 1970-01-01T00:00:00 TT. */
    public static final AbsoluteDate JavaEpoch;

    /** Date formats to use for string conversion. */
    private static SimpleDateFormat input  = null;
    private static SimpleDateFormat output = null;

    static {
      try {
        input = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        input.setTimeZone(TimeZone.getTimeZone("UTC"));
        output = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        output.setTimeZone(TimeZone.getTimeZone("UTC"));
        TimeScale tt = TTScale.getInstance();
        JulianEpoch         = new AbsoluteDate("-4712-01-01T12:00:00", tt);
        ModifiedJulianEpoch = new AbsoluteDate("1858-11-17T00:00:00",  tt);
        CNES1950Epoch       = new AbsoluteDate("1950-01-01T00:00:00",  tt);
        GPSEpoch            = new AbsoluteDate("1980-01-06T00:00:00",  tt);
        JavaEpoch           = new AbsoluteDate("1970-01-01T00:00:00",  tt);
        J2000Epoch          = new AbsoluteDate("2000-01-01T12:00:00",  tt);
      } catch (ParseException pe) {
        // should not happen
        throw new RuntimeException(pe);
      }
    }
    
    /** Create an instance with a default value ({@link #J2000Epoch}).
     */    
    public AbsoluteDate() {
      epoch  = J2000Epoch.epoch;
      offset = J2000Epoch.offset;
    }

    /** Build an instant from a location in a {@link TimeScale time scale}.
     * @param location location in the time scale
     * @param timeScale time scale
     */    
    public AbsoluteDate(Date location, TimeScale timeScale) {
      epoch  = location.getTime();
      offset = timeScale.offsetToTAI(epoch * 0.001);
    }    
    
    /** Build an instant from a location in a {@link TimeScale time scale}.
     * <p>The recognized format is only a subset of ISO-8601. It is
     * yyyy-mm-ddThh:mm:ss[.sss] where the fractional part of the second
     * is optional. Timezones are explicitely <em>not</em> supported.</p>
     * @param location location in the time scale in a subset ISO-8601 format
     * @param timeScale time scale
     * @exception ParseException if the string cannot be parsed
     */    
    public AbsoluteDate(String location, TimeScale timeScale)
      throws ParseException {
            ParsePosition position = new ParsePosition(0);
            Date parsed = input.parse(location, position);
            double fraction = 0;
            if (position.getIndex() < location.length()) {
              fraction =
                Double.parseDouble(location.substring(position.getIndex()));
            }
            epoch  = parsed.getTime();
            offset = fraction + timeScale.offsetToTAI(epoch * 0.001 + fraction);
    }    
    
    /** Build an instant from an offset with respect to another instant.
     * <p>It is important to note that the <code>offset</code> is <em>not</em>
     * the difference between two readings on a time scale. As an example,
     * the offset between the two instants leading to the readings
     * 2005-12-31T23:59:59 and 2006-01-01T00:00:00 on {@link UTCScale UTC}
     * time scale is <em>not</em> 1 second, but 2 seconds because a leap
     * second has been introduced at the end of 2005 in this time scale.</p>
     * @param instant reference instant
     * @param offset offset from the reference instant (seconds physically
     * separating the two instants)
     */    
    public AbsoluteDate(AbsoluteDate instant, double offset) {
        epoch = instant.epoch;
        this.offset = instant.offset + offset;
    }    
    
    public AbsoluteDate(AbsoluteDate date) {
    	epoch = date.epoch;
    	offset = date.offset;
    }
       
   /** Compute the offset between two instant.
    * <p>The offset is the number of seconds physically elapsed
    * between the two instants.</p>
    * @param instant instant to subtract from the instance
    * @return offset in seconds between the two instant (positive
    * if the instance is posterior to the argument)
    */
   public double minus(AbsoluteDate instant) {
     return 0.001 * (epoch - instant.epoch) + (offset - instant.offset);
   }

   /** Compute the offset between two time scales at the current instant.
    * <p>The offset is defined as <i>l<sub>1</sub>-l<sub>2</sub></i>
    * where <i>l<sub>1</sub></i> is the location of the instant in
    * the <code>scale1</code> time scale and <i>l<sub>2</sub></i> is the
    * location of the instant in the <code>scale2</code> time scale.</p>
    * @param scale1 first time scale
    * @param scale2 second time scale
    * @return offset in seconds between the two time scales at the
    * current instant
    */
   public double timeScalesOffset(TimeScale scale1, TimeScale scale2) {
     double taiTime = 0.001 * epoch + offset;
     return scale1.offsetFromTAI(taiTime) - scale2.offsetFromTAI(taiTime);
   }

   /** Convert the instance to a Java {@link java.util.Date Date}.
    * <p>Conversion to the Date class induces a loss of precision because
    * the Date class does not provide sub-millisecond information. Java Dates
    * are considered to be locations in some times scales.</p>
    * @param timeScale time scale to use
    * @return a {@link java.util.Date Date} instance representing the location
    * of the instant in the time scale
    */
   public Date toDate(TimeScale timeScale) {
     double time = 0.001 * epoch + offset;
     time += timeScale.offsetFromTAI(time);
     return new Date(Math.round(time * 1000));
   }
   
   /** Get a String representation of the instant location in UTC time scale.
    * @return a string representation of the instance,
    * in ISO-8601 format with milliseconds accuracy
    */
   public String toString() {
     try {
       return toString(UTCScale.getInstance());
     } catch (OrekitException oe) {
       return toString(TAIScale.getInstance());       
     }
   }

   /** Get a String representation of the instant location.
    * @param timeScale time scale to use
    * @return a string representation of the instance,
    * in ISO-8601 format with milliseconds accuracy
    */
   public String toString(TimeScale timeScale) {
     return output.format(toDate(timeScale));
   }

   /** Compare the instance with another date.
    * @param date other date to compare the instance to
    * @return a negative integer, zero, or a positive integer as this date
    * is before, simultaneous, or after the specified date.
    * @exception ClassCastException if the parameter is not an AbsoluteDate
    * instance
    */
   public int compareTo(Object date) {
     double delta = minus((AbsoluteDate) date);
     if (delta < 0) {
       return -1;
     } else if (delta > 0) {
       return +1;
     }
     return 0;
   }

   /** Reference epoch in milliseconds from 1970-01-01T00:00:00 TAI. */
   private final long epoch;
   
   /** Offset from the reference epoch in milliseconds. */
   private final double offset;

   private static final long serialVersionUID = -4127860894692239957L;

}
