package fr.cs.aerospace.orekit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.PoleCorrection;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.time.TTScale;

/** Class allowing access to IERS data.
 * @author Luc Maisonobe
 */
public class IERSData {

  /** Build an instance using locally cached data.
   * <p>This class searches for IERS EOP C 04 data files recursively
   * in a local cache starting at a specified directory tree. The
   * organisation of files in the tree free and sub-directories can be
   * used at will. The EOP C 04 files are recognised from their base
   * name which should match the pattern "eopc04_IAU2000.##" where
   * # stands for a digit character.</p>
   * @param cacheTopDirectory top directory of the local IERS data cache
   * @exception OrekitException if the IERS data cannot be loaded
   */
  public IERSData(File cacheTopDirectory)
    throws OrekitException {
    this.cacheTopDirectory = cacheTopDirectory;
    eopc04 = new TreeSet();
    loadData();
  }

  /** Get the UT1-UTC value.
   * <p>The data provided comes from the EOP C 04 files. It
   * is smoothed data.</p>
   * @param date date at which the value is desired
   * @return UT1-UTC in seconds
   */
  public double getUT1MinusUTC(AbsoluteDate date) {
    if (selectEntries(date)) {
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
   * @return pole correction
   */
  public PoleCorrection getPoleCorrection(AbsoluteDate date) {
    if (selectEntries(date)) {
      double dtP    = date.minus(previous.date);
      double dtN    = next.date.minus(date);
      double coeffP = dtN / (dtN + dtP);
      double coeffN = dtP / (dtN + dtP);
      return new PoleCorrection(coeffP * previous.pole.xp + coeffN * next.pole.xp,
                                coeffP * previous.pole.yp + coeffN * next.pole.yp);
    }
    return PoleCorrection.NULL_CORRECTION;
  }

  /** Select the entries surrounding a specified date.
   * @param  date target date
   * @return true if the date has correctly been surrounded
   */
  private boolean selectEntries(AbsoluteDate date) {

    // don't search if the cached selection is fine
    if ((previous != null) && (date.compareTo(previous.date) >= 0)
        && (next != null) && (date.compareTo(next.date) < 0)) {
      // the current selection is already good
      return true;
    }

    // reset the selection before the search phase
    previous = null;
    next     = null;

    // depending on IERS products,
    // entries are provided either every day or every five days
    double margin = 6 * 86400.0;
    EopC04Entry before =
      new EopC04Entry(new AbsoluteDate(date, -margin), 0, null);

    // search starting from entries a few steps before the target date
    for (Iterator iterator = eopc04.tailSet(before).iterator();
         iterator.hasNext() && (next == null);) {
      EopC04Entry entry = (EopC04Entry) iterator.next();
      if (date.compareTo(entry.date) > 0) {
        previous = entry;
      } else {
        next = entry;
      }
    }

    return next != null;

  }

  /** Load the IERS data.
   * @exception OrekitException if the IERS data cannot be loaded
   */
  private void loadData()
    throws OrekitException {

    // safety checks
    if (cacheTopDirectory == null) {
      throw new OrekitException("IERS local cache top directory not defined",
                                new String[0]);
    }
    if (! cacheTopDirectory.exists()) {
      throw new OrekitException("IERS local cache top directory {0}",
                                new String[] {
                                  cacheTopDirectory.getAbsolutePath()
                                });
    }
    if (! cacheTopDirectory.isDirectory()) {
      throw new OrekitException("{0} is not a directory",
                                new String[] {
                                  cacheTopDirectory.getAbsolutePath()
                                });
    }

    // recursively explores the local cache
    Pattern yearlyPattern = Pattern.compile("eopc04_IAU2000\\.\\d\\d");
    recurseLoadData(cacheTopDirectory, yearlyPattern);

    // safety checks
    if (eopc04.isEmpty()) {
      throw new OrekitException("no IERS data file found in local cache {0}",
                                new String[] {
                                  cacheTopDirectory.getAbsolutePath()
                                });
      
    }

    // check for duplicated entries or holes in the loaded files
    Iterator iterator = eopc04.iterator();
    next = (EopC04Entry) iterator.next();
    double tooSmall = 1.0;
    double tooLarge = 6 * 86400.0;
    while (iterator.hasNext()) {
      previous = next;
      next     = (EopC04Entry) iterator.next();
      if (next.date.minus(previous.date) <= tooSmall) {
        throw new OrekitException("duplicated IERS data at {0}",
                                  new String[] {
                                    previous.date.toString(TTScale.getInstance())
                                  });
      }
      if (next.date.minus(previous.date) >= tooLarge) {
        throw new OrekitException("missing IERS data between {0} and {1}",
                                  new String[] {
                                    previous.date.toString(TTScale.getInstance()),
                                    next.date.toString(TTScale.getInstance()),
                                  });
      }
    }

  }

  /** Recursively search for IERS data files in a directory tree.
   * @param directory directory where to search
   * @param yearlyPattern pattern for yearly data files
   * @exception OrekitException if the IERS data cannot be loaded
   */
  private void recurseLoadData(File directory,
                               Pattern yearlyPattern)
    throws OrekitException {

    // search in current directory
    File[] list = directory.listFiles();

    for (int i = 0; i < list.length; ++i) {
      if (list[i].isDirectory()) {
        // recurse in the sub-directory
        recurseLoadData(list[i], yearlyPattern);
      } else  if (yearlyPattern.matcher(list[i].getName()).matches()) {
        loadYearlyFile(list[i]);
      }
    }

  }

  /** Load an IERS EOP C 04 data file.
   * @param file file to load
   * @exception OrekitException if the data cannot be loaded
   */
  private void loadYearlyFile(File file)
    throws OrekitException {
    try {

      double arcSecondsToRadians = 2 * Math.PI / 1296000;

      // the data lines in the EOP C 04 yearly data files have the following fixed form:
      // "  JAN   1  52275-0.176980 0.293952-0.1158223   0.0008163    0.00044  0.00071"
      // "  JAN   2  52276-0.177500 0.297468-0.1166973   0.0009382    0.00030  0.00043"
      // the corresponding fortran format is:
      //  2X,A4,I3,2X,I5,2F9.6,F10.7,2X,F10.7,2X,2F9.5
      String yearField  = "\\p{Alpha}\\p{Alpha}\\p{Alpha}\\p{Blank}";
      String dayField   = "\\p{Blank}\\p{Digit}\\p{Digit}";
      String mjdField   = "\\(\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit}\\)";
      String poleField  = "\\(.........\\)";
      String dtU1Field  = "\\(..........\\)";
      String lodField   = "..........";
      String deltaField = ".........";
      Pattern pattern = Pattern.compile("  " + yearField + dayField + "  "
                                        + mjdField + poleField + poleField
                                        + dtU1Field + "  " + lodField
                                        + "  " + deltaField + deltaField);

      // read all file, ignoring header
      BufferedReader reader = new BufferedReader(new FileReader(file));
      int lineNumber = 0;
      boolean inHeader = true;
      for (String line = reader.readLine(); line != null; line = reader.readLine()) {
        ++lineNumber;
        boolean parsed = false;
        Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
          inHeader = false;
          try {
            // this is a data line, build an entry from the extracted fields
            int mjd = Integer.parseInt(matcher.group(1));
            AbsoluteDate date =
              new AbsoluteDate(AbsoluteDate.J2000Epoch, 86400 * (mjd - 51544.5));
            double x    = Double.parseDouble(matcher.group(2)) * arcSecondsToRadians;
            double y    = Double.parseDouble(matcher.group(3)) * arcSecondsToRadians;
            double dtu1 = Double.parseDouble(matcher.group(4));
            eopc04.add(new EopC04Entry(date, dtu1, new PoleCorrection(x, y)));
            parsed = true;
          } catch (NumberFormatException nfe) {
            // ignored, will be handled by the parsed boolean
          }
        }
        if (! (inHeader || parsed)) {
          throw new OrekitException("unable to parse line {0} in IERS data file {1}",
                                    new String[] {
                                      Integer.toString(lineNumber),
                                      file.getAbsolutePath()
                                    });
        }
      }
      if (inHeader) {
        throw new OrekitException("file {0} is not an IERS data file",
                                  new String[] {
                                    file.getAbsolutePath()
                                  });        
      }

    } catch (IOException ioe) {
      throw new OrekitException(ioe.getMessage(), ioe);
    }

  }

  /** Container class for EOP C 04 entries. */
  private static class EopC04Entry implements Comparable {

    /** Entry date. */
    public final AbsoluteDate date;

    /** UT1-UTC (seconds). */
    public final double ut1MinusUtc;

    /** Pole correction. */
    public final PoleCorrection pole;

    /** Simple constructor.
     * @param date entry date
     * @param UT1-UTC (seconds)
     * @param pole pole correction
     */
    public EopC04Entry(AbsoluteDate date, double ut1MinusUtc,
                       PoleCorrection pole) {
      this.date        = date;
      this.ut1MinusUtc = ut1MinusUtc;
      this.pole        = pole;
    }

    /** Compare an entry with another one, according to date. */
    public int compareTo(Object entry) {
      return date.compareTo(((EopC04Entry) entry).date);
    }

  }

  /** Top directory of the local IERS data cache. */
  private File cacheTopDirectory;

  /** EOP C 04 entries. */
  private TreeSet eopc04;

  /** Previous entry. */
  private EopC04Entry previous;

  /** Next entry. */
  private EopC04Entry next;

}
