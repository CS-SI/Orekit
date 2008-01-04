package fr.cs.orekit.utils;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.time.TAIScale;
import fr.cs.orekit.time.TimeScale;
import fr.cs.orekit.time.UTCScale;

/** Converts a date into a standard ISO-8601 string representation.
 *
 * @author F. Maussion
 */
public class DateFormatter {

  /** Date formats to use for string conversion. */
  private static SimpleDateFormat output = null;
  static {
      output = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
      output.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  /** Get a String representation of the instant location in UTC time scale.
   * @param date the date to format
   * @return a string representation of the instance,
   * in ISO-8601 format with milliseconds accuracy
   */
  public static String toString(AbsoluteDate date) {
    try {
      return toString(date,UTCScale.getInstance());
    } catch (OrekitException oe) {
      return toString(date,TAIScale.getInstance());
    }
  }

  /** Get a String representation of the instant location.
   * @param date the date to format
   * @param timeScale time scale to use
   * @return a string representation of the instance,
   * in ISO-8601 format with milliseconds accuracy
   */
  public static String toString(AbsoluteDate date, TimeScale timeScale) {
    return output.format(date.toDate(timeScale));
  }

}
