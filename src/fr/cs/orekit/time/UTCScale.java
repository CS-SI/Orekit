package fr.cs.orekit.time;

import java.text.ParseException;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.iers.Leap;
import fr.cs.orekit.iers.UTCTAIHistoryFilesLoader;

/** Coordinated Universal Time.
 * <p>UTC is related to TAI using step adjustments from time to time
 * according to IERS (International Earth Rotation Service) rules. These
 * adjustments require introduction of leap seconds.</p>
 * <p>The handling of time <em>during</em> the leap seconds insertion has
 * been adapted from the standard in order to compensate for the lack of
 * support for leap seconds in the standard java {@link java.util.Date}
 * class. We consider the leap is introduced as one clock reset at the
 * end of the leap. For example when a one second leap was introduced
 * between 2005-12-31T23:59:59 UTC and 2006-01-01T00:00:00 UTC, we
 * consider time flowed continuously for one second in UTC time scale
 * from 23:59:59 to 00:00:00 and <em>then</em> a -1 second leap reset
 * the clock to 23:59:59 again, leading to have to wait one second more
 * before 00:00:00 was reached. The standard would have required to have
 * introduced a second corresponding to location 23:59:60, i.e. the
 * last minute of 2005 was 61 seconds long instead of 60 seconds.</p>
 * <p>The OREKIT library retrieves time steps data thanks to the {@link
 * fr.cs.orekit.iers.IERSDirectoryCrawler IERSDirectoryCrawler} class.</p>
 * <p>This is a singleton class, so there is no public constructor.</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 */
public class UTCScale extends TimeScale {

  /** Private constructor for the singleton.
   * @exception OrekitException if the time steps cannot be read
   */
  private UTCScale()
    throws OrekitException {
    super("UTC");

    // get the time steps from the history file
    // found in the IERS directories hierarchy
    leaps = new UTCTAIHistoryFilesLoader().getTimeSteps();

  }

  /** Get the unique instance of this class.
   * @return the unique instance
   * @exception OrekitException if the time steps cannot be read
   */
  public static TimeScale getInstance()
    throws OrekitException {
    if (instance == null) {
      instance = new UTCScale();
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
    for (int i = 0; i < leaps.length; ++i) {
      Leap leap = leaps[i];
      if ((taiTime  + (leap.offsetAfter - leap.step)) >= leap.utcTime) {
        return leap.offsetAfter;
      }
    }
    return 0;
  }

  /** Get the offset to convert locations from instance to {@link TAIScale} .
   * @param instanceTime location of an event in the instance time scale
   * as a seconds index starting at 1970-01-01T00:00:00
   * @return offset to <em>add</em> to instanceTime to get a location
   * in {@link TAIScale}  time scale
   */
  public double offsetToTAI(double instanceTime) {
    for (int i = 0; i < leaps.length; ++i) {
      Leap leap = leaps[i];
      if (instanceTime >= leap.utcTime) {
        return -leap.offsetAfter;
      }
    }
    return 0;
  }

  /** Get the date of the first available known UTC steps.
   * @return the start date of the available data
   * @throws OrekitException
   */
  public AbsoluteDate getStartDate()
    throws ParseException, OrekitException {
    if (UTCStartDate == null) {
      try {
        AbsoluteDate ref = new AbsoluteDate("1970-01-01T00:00:00", this);
        Leap firstLeap = leaps[leaps.length - 1];
        UTCStartDate = new AbsoluteDate(ref, firstLeap.utcTime - firstLeap.step);
      } catch (ParseException pe) {
        // this should never happen with the previous fixed date string
        throw new RuntimeException("internal error");
      }
    }
    return UTCStartDate;
  }

  /** Uniq instance. */
  private static TimeScale instance = null;

  /** Time steps. */
  private Leap[] leaps;

  /** Date of the first available known UTC steps. */
  private AbsoluteDate UTCStartDate;

}
