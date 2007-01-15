package fr.cs.aerospace.orekit.frames;

import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.iers.EarthOrientationParameters;
import fr.cs.aerospace.orekit.iers.IERSData;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.time.UTCScale;


/** This class formats and reads IERS datas adapted to the OREKIT format.
 * It is a singleton since it handles voluminous datas.
 * @author L. Maisonobe
 * @author F. Maussion
 */
class DatedEOPReader {
  
  /** Private constructor for the singleton.
   * @throws OrekitException when there is a problem while reading IERS datas
   */
  private DatedEOPReader() throws OrekitException {
    
    // convert the mjd dates in the raw entries into AbsoluteDate instances
    eop = new TreeSet();
    TreeSet rawEntries = IERSData.getInstance().getEarthOrientationParameters();
    for (Iterator iterator = rawEntries.iterator(); iterator.hasNext();) {
      eop.add(new DatedEop((EarthOrientationParameters) iterator.next()));
    }
    
  }
  
  /** Get the singleton instance.
   * @return the unique dated eop reader instance.
   * @throws OrekitException when there is a problem while reading IERS datas
   */
  protected static DatedEOPReader getInstance() throws OrekitException {
    if (instance == null) {
      instance = new DatedEOPReader();
    }
    return instance;
  }
  
  /** Get the UT1-UTC value.
   * <p>The data provided comes from the EOP C 04 files. It
   * is smoothed data.</p>
   * @param date date at which the value is desired
   * @return UT1-UTC in seconds
   */
  protected double getUT1MinusUTC(AbsoluteDate date) {
    if (selectEOPEntries(date)) {
      double dtP = date.minus(previous.date);
      double dtN = next.date.minus(date);
      return (dtP * next.rawEntry.ut1MinusUtc + dtN * previous.rawEntry.ut1MinusUtc)
           / (dtN + dtP);
    }
    return 0;
  }

  /** Get the pole IERS Reference Pole correction.
   * <p>The data provided comes from the EOP C 04 files. It
   * is smoothed data.</p>
   * @param date date at which the correction is desired
   * @return pole correction
   * @exception OrekitException if the IERS data cannot be read
   */
  protected PoleCorrection getPoleCorrection(AbsoluteDate date) {
    if (selectEOPEntries(date)) {
      double dtP    = date.minus(previous.date);
      double dtN    = next.date.minus(date);
      double sum    = dtN + dtP;
      double coeffP = dtP / sum;
      double coeffN = dtN / sum;
      return new PoleCorrection(coeffP * previous.rawEntry.pole.xp
                              + coeffN * next.rawEntry.pole.xp,
                                coeffP * previous.rawEntry.pole.yp
                              + coeffN * next.rawEntry.pole.yp);
    }
    return PoleCorrection.NULL_CORRECTION;
  }
  
  /** Select the entries bracketing a specified date.
   * @param  date target date
   * @return true if the date was found in the tables
   */
  private boolean selectEOPEntries(AbsoluteDate date) {

    // don't search if the cached selection is fine
    if ((previous != null) && (date.minus(previous.date) >= 0)
        && (next != null) && (date.minus(next.date) < 0)) {
      // the current selection is already good
      return true;
    }

    // reset the selection before the search phase
    previous = null;
    next     = null;

    // depending on IERS products,
    // entries are provided either every day or every five days
    double margin = 6 * 86400;
    DatedEop before =
      new DatedEop(new AbsoluteDate(date, -margin), null);

    // search starting from entries a few steps before the target date
    SortedSet tailSet = eop.tailSet(before);
    if (tailSet != null) {
      for (Iterator iterator = tailSet.iterator(); iterator.hasNext() && (next == null);) {
        DatedEop entry = (DatedEop) iterator.next();
        if ((previous == null) || (date.minus(entry.date) > 0)) {
          previous = entry;
        } else {
          next = entry;
        }
      }
    }

    return (previous != null) && (next != null);

  }
  
  /** Earth Orientation Parameter format more adapted to the OREKIT representation.
   */
  private static class DatedEop implements Comparable, Serializable {

    private static final long serialVersionUID = -6893523591361188479L;

    /** Absolute date. */
    public final AbsoluteDate date;

    /** Raw entry. */
    public final EarthOrientationParameters rawEntry;

    /** Simple constructor.
     * @param date absolute date
     * @param rawEntry raw entry
     */
    public DatedEop(AbsoluteDate date, EarthOrientationParameters rawEntry) {
      this.date     = date;
      this.rawEntry = rawEntry;
    }

    /** Simple constructor.
     * @param rawEntry raw entry
     * @exception OrekitException if the time steps data cannot be read
     */
    public DatedEop(EarthOrientationParameters rawEntry)
      throws OrekitException {
      long javaTime = (rawEntry.mjd - 40587) * 86400000l;
      this.date     =
        new AbsoluteDate(new Date(javaTime), UTCScale.getInstance());
      this.rawEntry = rawEntry;
    }

    /** Compare an entry with another one, according to date. */
    public int compareTo(Object entry) {
      return date.compareTo(((DatedEop) entry).date);
    }

  }
    
  /** Earth Orientation Parameters. */
  private TreeSet eop = null;
  
  /** Previous EOP entry. */
  private DatedEop previous;

  /** Next EOP entry. */
  private DatedEop next;
  
  /** Singleton instance. */
  private static DatedEOPReader instance = null;
}
