package fr.cs.aerospace.orekit.frames;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.iers.BulletinBFilesLoader;
import fr.cs.aerospace.orekit.iers.EOPC04FilesLoader;
import fr.cs.aerospace.orekit.iers.EarthOrientationParameters;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.time.UTCScale;
import fr.cs.aerospace.orekit.utils.DateFormatter;


/** This class holds Earth Orientation data throughout a large time range.
 * It is a singleton since it handles voluminous data.
 * @author L. Maisonobe
 * @author F. Maussion
 */
public class EarthOrientationHistory implements Serializable {
  
  /** Private constructor for the singleton.
   * @throws OrekitException if there is a problem while reading IERS data
   */
  private EarthOrientationHistory()
    throws OrekitException {

    // set up a date-ordered set, able to use either
    // EarthOrientationParameters or AbsoluteDate instances
    // (beware to use AbsoluteDate ONLY as arguments to
    // headSet or tailSet and NOT to add them in the set)
    eop = new TreeSet(new EOPComparator());

    // consider first the more accurate EOP C 04 entries
    new EOPC04FilesLoader().loadEOP(eop);

    // add the final values from bulletin B entries for new dates
    // (if duplicated dates occur, the existing data will be preserved)
    new BulletinBFilesLoader().loadEOP(eop);

    // check the continuity of the loaded data
    checkEOPContinuity(5);

  }

  /** Get the singleton instance.
   * @return the unique dated eop reader instance.
   * @throws OrekitException when there is a problem while reading IERS datas
   */
  public static EarthOrientationHistory getInstance()
    throws OrekitException {
    if (instance == null) {
      instance = new EarthOrientationHistory();
    }
    return instance;
  }
 
  /** Check Earth orientation parameters continuity.
   * @param maxGap maximal allowed gap between entries (in days)
   * @exception OrekitException if there are holes in the data sequence
   */
  private void checkEOPContinuity(int maxGap)
    throws OrekitException {
    EarthOrientationParameters current = null;
    for (Iterator iterator = eop.iterator(); iterator.hasNext();) {
      EarthOrientationParameters previous = current;
      current = (EarthOrientationParameters) iterator.next();

      // compare the dates of previous and current entries
      if ((previous != null) && ((current.mjd - previous.mjd) > maxGap)) {
        throw new OrekitException("missing Earth Orientation Parameters between {0} and {1}",
                                  new String[] {
                                    DateFormatter.toString(previous.date, UTCScale.getInstance()),
                                    DateFormatter.toString(current.date, UTCScale.getInstance())
                                  });

      }
    }
  }

  /** Get the date of the first available Earth Orientation Parameters.
   * @return the start date of the available data
   */
  public AbsoluteDate getStartDate() {
    return ((EarthOrientationParameters)eop.first()).date;
  }
  
  /** Get the date of the last available Earth Orientation Parameters.
   * @return the end date of the available data
   */
  public AbsoluteDate getEndDate() {
    return ((EarthOrientationParameters)eop.last()).date;
  }
  
  /** Get the UT1-UTC value.
   * <p>The data provided comes from the EOP C 04 files. It
   * is smoothed data.</p>
   * @param date date at which the value is desired
   * @return UT1-UTC in seconds (0 if date is outside covered range)
   */
  protected double getUT1MinusUTC(AbsoluteDate date) {
    if (selectBracketingEntries(date)) {
      double dtP = date.minus(previous.date);
      double dtN = next.date.minus(date);
      return (dtP * next.ut1MinusUtc + dtN * previous.ut1MinusUtc) / (dtN + dtP);
    }
    return 0;
  }

  /** Get the pole IERS Reference Pole correction.
   * <p>The data provided comes from the EOP C 04 files. It
   * is smoothed data.</p>
   * @param date date at which the correction is desired
   * @return pole correction ({@link PoleCorrection#NULL_CORRECTION
   * PoleCorrection.NULL_CORRECTION} if date is outside covered range)
   * @exception OrekitException if the IERS data cannot be read
   */
  protected PoleCorrection getPoleCorrection(AbsoluteDate date) {
    if (selectBracketingEntries(date)) {
      double dtP    = date.minus(previous.date);
      double dtN    = next.date.minus(date);
      double sum    = dtN + dtP;
      double coeffP = dtN / sum;
      double coeffN = dtP / sum;
      return new PoleCorrection(coeffP * previous.pole.xp + coeffN * next.pole.xp,
                                coeffP * previous.pole.yp + coeffN * next.pole.yp);
    }
    return PoleCorrection.NULL_CORRECTION;
  }
  
  /** Select the entries bracketing a specified date.
   * @param  date target date
   * @return true if the date was found in the tables
   */
  private boolean selectBracketingEntries(AbsoluteDate date) {

    // don't search if the cached selection is fine
    if ((previous != null) && (date.minus(previous.date) >= 0)
        && (next != null) && (date.minus(next.date) < 0)) {
      // the current selection is already good
      return true;
    }

    // select the bracketing elements (may be null)
    SortedSet head = eop.headSet(date);
    previous = (EarthOrientationParameters) (head.isEmpty() ? null : head.last());
    SortedSet tail = eop.tailSet(date);
    next     = (EarthOrientationParameters) (tail.isEmpty() ? null : tail.first());

    return (previous != null) && (next != null);

  }

  /** Specialized comparator handling both EarthOrientationParameters
   * and AbsoluteDate instances.
   */
  private static class EOPComparator implements Comparator, Serializable {

    public int compare(Object o1, Object o2) {
      AbsoluteDate d1 = (o1 instanceof AbsoluteDate) ?
          ((AbsoluteDate) o1) : ((EarthOrientationParameters) o1).date;
      AbsoluteDate d2 = (o2 instanceof AbsoluteDate) ?
          ((AbsoluteDate) o2) : ((EarthOrientationParameters) o2).date;
      return d1.compareTo(d2);
    }

    private static final long serialVersionUID = 612278880319640448L;

  }

  /** Earth Orientation Parameters. */
  private TreeSet eop = null;
  
  /** Previous EOP entry. */
  private EarthOrientationParameters previous;

  /** Next EOP entry. */
  private EarthOrientationParameters next;
  
  /** Singleton instance. */
  private static EarthOrientationHistory instance = null;

  private static final long serialVersionUID = 4028633968968154618L;

}
