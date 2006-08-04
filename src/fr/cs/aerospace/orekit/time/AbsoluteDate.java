package fr.cs.aerospace.orekit.time;

import java.util.Date;
import java.util.TimeZone;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.text.ParseException;

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
 *   #Instant(Date, TimeScale) Instant(location, timeScale)}, {@link
 *   #Instant(String, TimeScale) Instant(location, timeScale)}, {@link
 *   #reset(Date, TimeScale) reset(location, timeScale)}, {@link
 *   #reset(String, TimeScale) reset(location, timeScale)}, {@link #toDate}
 *   , {@link #toString() toString()}, {@link #toString(TimeScale)
 *   toString(timeScale)} and {@link timeScalesOffset}.</p>
 *   </li>
 *   <li><p>offset view (mainly for physical computation)</p>
 *   <p>offsets represent either the flow of time between two events
 *   (two instances of the class) or durations. They are counted in seconds,
 *   are continuous and could be measured using only a virtual perfect stopwatch.
 *   The related methods are {@link #Instant(AbsoluteDate, double) Instant(instant,
 *   offset)}, {@link #Instant(AbsoluteDate) Instant(instant)}, {@link
 *   #reset(AbsoluteDate, double) reset(instant, offset)}, {@link
 *   #reset(AbsoluteDate) reset(instant)}, {@link #minus} and {@link #shift}.</p>
 *   </li>
 * </ul>

 * @author L. Maisonobe
 * @see TimeScale
 */
public class AbsoluteDate {

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

    /** Date formats to use for string conversion. */
    private static SimpleDateFormat input  = null;
    private static SimpleDateFormat output = null;

    static {
      try {
        input = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss");
        input.setTimeZone(TimeZone.getTimeZone("UTC"));
        output = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss.SSS");
        output.setTimeZone(TimeZone.getTimeZone("UTC"));
        TimeScale tt = TTScale.getInstance();
        JulianEpoch         = new AbsoluteDate("-4712-01-01T12:00:00", tt);
        ModifiedJulianEpoch = new AbsoluteDate("1858-11-17T00:00:00",  tt);
        CNES1950Epoch       = new AbsoluteDate("1950-01-01T00:00:00",  tt);
        GPSEpoch            = new AbsoluteDate("1980-01-06T00:00:00",  tt);
        J2000Epoch          = new AbsoluteDate("2000-01-01T12:00:00",  tt);
      } catch (ParseException pe) {
        // should not happen
        throw new RuntimeException(pe);
      }
    }
    
    /** Create an instance with a default value ({@link #J2000Epoch}).
     */    
    public AbsoluteDate() {
      reset();
    }

    /** Build an instant from a location in a {@link TimeScale time scale}.
     * @param location location in the time scale
     * @param timeScale time scale
     */    
    public AbsoluteDate(Date location, TimeScale timeScale) {
      reset(location, timeScale);
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
      reset(location, timeScale);
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
      reset(instant, offset);
    }    
    
    /** Copy constructor.
     * @param i instant to copy values from
     */    
    public AbsoluteDate(AbsoluteDate i) {
      reset(i);
    }    

    /** Reset the instant to a default value ({@link #J2000Epoch}).
     */    
    public void reset() {
      reset(J2000Epoch);
    }

    /** Reset the instant from a location in a {@link TimeScale time scale}.
     * @param location location in the time scale
     * @param timeScale time scale
     */    
    public void reset(Date location, TimeScale timeScale) {
      epoch  = location.getTime();
      double t = epoch * 0.001;
      offset = timeScale.toTAI(t) - t;
    }    
   
    /** Reset the instant from a location in a {@link TimeScale time scale}.
     * <p>The recognized format is only a subset of ISO-8601. It is
     * yyyy-mm-ddThh:mm:ss[.sss] where the fractional part of the second
     * is optional. Timezones are explicitely <em>not</em> supported.</p>
     * @param location location in the time scale in a subset ISO-8601 format
     * @param timeScale time scale
     * @exception ParseException if the string cannot be parsed
     */    
    public void reset(String location, TimeScale timeScale)
      throws ParseException {
      ParsePosition position = new ParsePosition(0);
      Date parsed = input.parse(location, position);
      double fraction = 0;
      if (position.getIndex() < location.length()) {
        fraction =
          Double.parseDouble(location.substring(position.getIndex()));
      }
      epoch  = parsed.getTime();
      double t = epoch * 0.001;
      offset = timeScale.toTAI(t + fraction) - t;
    }    
   
    /** Reset the instant from an offset with respect to another instant.
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
    public void reset(AbsoluteDate instant, double offset) {
      epoch = instant.epoch;
      this.offset = instant.offset + offset;
    }    
    
    /** Reset the instant by copy.
     * @param instant instant to copy values from
     */    
    public void reset(AbsoluteDate instant) {
      epoch  = instant.epoch;
      offset = instant.offset;
    }    

   /** Shift an instant.
    * @param offset time shift in seconds
    */
   public void shift(double offset) {
     this.offset += offset;
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
     return scale1.fromTAI(taiTime) - scale2.fromTAI(taiTime);
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
     long time = Math.round(timeScale.fromTAI(0.001 * epoch + offset) * 1000);
     return new Date(time);
   }
   
   /** Get a String representation of the instant location in UTC time scale.
    * @return a string representation of the instance,
    * in ISO-8601 format with milliseconds accuracy
    */
   public String toString() {
     return toString(UTCScale.getInstance());
   }

   /** Get a String representation of the instant location.
    * @param timeScale time scale to use
    * @return a string representation of the instance,
    * in ISO-8601 format with milliseconds accuracy
    */
   public String toString(TimeScale timeScale) {
     long time = Math.round(timeScale.fromTAI(0.001 * epoch + offset) * 1000);
     return output.format(new Date(time));
   }

   /** Reference epoch in milliseconds from 1970-01-01T00:00:00 TAI. */
   private long epoch;
   
   /** Offset from the reference epoch in milliseconds. */
   private double offset;

}
