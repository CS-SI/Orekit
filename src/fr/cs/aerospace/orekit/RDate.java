package fr.cs.aerospace.orekit;

import org.spaceroots.mantissa.utilities.ArraySliceMappable;

import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/** This class represents a specific instant in time, as a double precision
 * offset relative to a reference epoch.

 * <p>This class is devoted to represent accurate dates and to use them in
 * computation.</p>

 * <p>They are considered to be seconds offsets relative to a reference date
 * called the <em>epoch</em>. The epoch is a classical
 * <code>java.util.Date</code> instance, which as millisecond precision.
 * Several standard epoch for space applications are available as public static
 * fields. Since offsets are double precision numbers and given the relative
 * accuracy of IEEE 754 is 2^-52 (which is about 2.22e-16), the accuracy of the
 * offsets can be estimated as a function of the gap between the epoch and the
 * date.</p>

 * <p>The farthest epoch in the past is the julian day reference, which
 * is first of January -4713 at noon. With this reference, offsets for dates
 * at the beginning of the 21st century are about 2.12e11 seconds (2.11e11
 * seconds occurs in 1974, 2.12e11 seconds occurs in 2005 and 2.13e11 seconds
 * occurs in 2037). The accuracy is therefore about 0.5 milliseconds. If the
 * date is less than 95 years away from the epoch, the offset is smaller that
 * 3e9 seconds, which leads to an accuracy of about 0.7 microseconds. If
 * nanosecond precision is needed, the date and the epoch should not be more
 * than 52 days away from each other.</p>

 * <p>Microseconds precision is often sufficient to represent instants of time
 * that have a macroscopic meaning (orbit dates for example). As an example, a
 * low earth orbiting satellite travels less than one centimeter along track
 * during one microsecond, which is good enough even for highly demanding
 * applications. A simple way to improve a little the accuracy to nanosecond
 * level is to use a local epoch (for example the date of the initial orbit, or
 * the first measurement, or the beginning of a search period) for each
 * application. This works as long as the dates used by the application cover
 * only a few weeks time range. This is however largely insufficient for events
 * that only have local meaning, like the flight time of a signal during a
 * pseudo-distance measurement. The speed of light is much larger that a
 * satellite velocity, so a one centimeter accuracy needs a 0.03 nanosecond
 * accuracy on the flight time. It would be impractical to update the epoch in
 * order to guaranty if is less than 40 hours away from the event date. Such
 * events should <em>not</em> be represented by the departure and arrival dates,
 * but rather by the travel time itself, which is the quantity that is really
 * physically measured.</p>

 * @author L. Maisonobe
 */
public class RDate implements ArraySliceMappable {

    /** Reference epoch for julian dates: -4712-01-01T12:00:00.
     * <p>The java.util.Date class follows the astronomical convention
     * and uses a year 0 between years -1 and +1, hence this reference
     * date is in year -4712 and not in year -4713 as can be seen in
     * other documents that obey a different convention.</p>
     */
    public static final Date JulianEpoch;
    /** Reference julian date. */
    public static RDate JulianRDate;
    
    /** Reference epoch for modified julian dates: 1858-11-17T00:00:00. */
    public static final Date ModifiedJulianEpoch;
    /** Reference modified julian date. */
    public static RDate ModifiedJulianRDate;
    
    /** Reference epoch for CNES 1950 dates: 1950-01-01T00:00:00. */
    public static final Date CNES1950Epoch;
    /** Reference CNES 1950 date. */
    public static RDate CNES1950RDate;
    
    /** Reference epoch for GPS weeks: 1980-01-06T00:00:00. */
    public static final Date GPSEpoch;
    /** Reference GPS date. */
    public static RDate GPSRDate;

    /** Reference epoch for J2000 dates: 2000-01-01T12:00:00.
     * This is the default epoch
     */
    public static final Date J2000Epoch;
    /** Reference J2000 date. */
    public static RDate J2000RDate;

    /** Reference epoch. */
    private Date epoch;
    
    /** Offset from the reference epoch in seconds. */
    private double offset;
    
    /** Date format to use for string conversion. */
    private static SimpleDateFormat iso8601 = null;

    static {
      try {
        iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss");
        iso8601.setTimeZone(TimeZone.getTimeZone("UTC"));
        JulianEpoch         = iso8601.parse("-4712-01-01T12:00:00");
        ModifiedJulianEpoch = iso8601.parse("1858-11-17T00:00:00");
        CNES1950Epoch       = iso8601.parse("1950-01-01T00:00:00");
        GPSEpoch            = iso8601.parse("1980-01-06T00:00:00");
        J2000Epoch          = iso8601.parse("2000-01-01T12:00:00");
        
        JulianRDate         = new RDate(JulianEpoch, 0.0);
        ModifiedJulianRDate = new RDate(ModifiedJulianEpoch, 0.0);
        CNES1950RDate       = new RDate(CNES1950Epoch, 0.0);
        GPSRDate            = new RDate(GPSEpoch, 0.0);
        J2000RDate          = new RDate(J2000Epoch, 0.0);
        
      } catch (ParseException pe) {
        throw new RuntimeException(pe);
      }
    }
    
    /** Simple Constructor.
     * Create an instance with a default value ({@link #J2000Epoch} for the
     * epoch, and 0 for the offset)
     */    
    public RDate() {
      epoch  = J2000Epoch;
      offset = 0.0;
    }

    /** Simple constructor.
     * Build a date with the given offset from the reference date.
     * @param epoch reference epoch for the date (a reference to this object
     * will be stored in the instance). Predefined epochs are provided by the
     * following static fields: {@link #JulianEpoch},
     * {@link #ModifiedJulianEpoch}, {@link #CNES1950Epoch}, {@link #GPSEpoch}
     * and {@link #J2000Epoch}, but custom dates can also be used.
     * @param offset offset from the reference epoch (seconds)
     */    
    public RDate(Date epoch, double offset) {
      this.epoch  = epoch;
      this.offset = offset;
    }    
    
    /** Simple constructor.
     * Build a date with the given offset from another date.
     * @param date reference date (the epoch will be copied from this date)
     * @param offset offset from the reference date (seconds, the global offset
     * of the instance will be the sum of this offset and the internal offset of
     * the reference date)
     */    
    public RDate(RDate date, double offset) {
      this.epoch  = date.epoch;
      this.offset = date.offset + offset;
    }    
    
    /** Copy constructor.
     * @param d date to copy values from
     */    
    public RDate(RDate d) {
      epoch  = d.epoch;
      offset = d.offset;
    }    
    
    /** Get the reference epoch.
     * @return the reference epoch for this date
     */
    public Date getEpoch() {
      return epoch;
    }
    
    /** Set the reference epoch.
     * Setting the epoch does <em>not</em> change the instant
     * represented by this date, the offset is adjusted
     * in order to compensate for the epoch change
     * @param epoch new reference epoch for the date
     */
    public void setEpoch(Date epoch) {
      offset += 0.001 * (this.epoch.getTime() - epoch.getTime());
      this.epoch = epoch;
    }
    
    /** Get the offset from the reference epoch.
     * @return offset from the reference epoch (s)
     */    
    public double getOffset() {
      return offset;
    }
    
    /** Set the offset from the reference epoch.
     * @param offset new offset in seconds from the reference epoch for the date
     */    
   public void setOffset(double offset) {
      this.offset = offset;
    }

   /** Reset the date.
     * @param epoch new reference epoch for the date
     * @param offset new offset in seconds from the reference epoch for the date
     */
   public void reset(Date epoch, double offset) {
     this.epoch  = epoch;
     this.offset = offset;
   }
   
   /** Reset the date to default values.
     */
   public void reset() {
     epoch  = J2000Epoch;
     offset = 0.0;
   }
   
   /** Reset the date.
     * @param date to copy values from
     */
   public void reset(RDate date) {
     epoch  = date.epoch;
     offset = date.offset;
   }
   
   /** Shift a date.
    * @param dt time shift in seconds to add to the date
    */
   public void shift(double dt) {
     offset += dt;
   }
   
   /** Compute the gap between two dates.
    * The gap is the number of seconds between the dates
    * @param date date to subtract from the instance
    * @return gap in seconds between the two dates
    */
   public double minus(RDate date) {
     return 0.001 * (epoch.getTime() - date.epoch.getTime())
          + (offset - date.offset);
   }
   
   /** Convert the instance to Date.
    * Conversion to the Date class induces a loss of precision because
    * the Date class does not provide sub-millisecond information.
    * @return a date object containing the same date as the instance
    */
   public Date toDate() {
     return new Date(Math.round(epoch.getTime() + 1000 * offset));
   }
   
   /** Get a String representation of this date.
    * @return a string representation of this date, in ISO-8601 format
    */
   public String toString() {
     return iso8601.format(toDate());
   }

   /** Get the state dimension.
    * @return an integer representing the state dimension
    */
   public int getStateDimension() {
     return 1;
   }
   /** Reinitialize internal state from the specified array slice data.
    * @param start - start index in the array
    * @param array - array holding the data to extract
    */
   public void mapStateFromArray(int start, double[] array) {
     offset = array[start];
   }
   /** Store internal state data into the specified array slice.
    * @param start - start index in the array
    * @param array - array holding the data to extract
    */
   public void mapStateToArray(int start, double[] array) {
     array[start] = offset;
   }
   
}
