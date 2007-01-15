package fr.cs.aerospace.orekit.iers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.PoleCorrection;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.time.UTCScale;

/** Class loading IERS data files.

 * <p>
 * This class handles the IERS data files recursively starting
 * from a root directory tree specified by the java property
 * <code>orekit.iers.directory</code>. If the property is not set or is null,
 * no IERS data will be used (i.e. no pole correction and no UTC steps will
 * be taken into account). If the property is set, it must correspond to an
 * existing directory tree. The organisation of files in the tree
 * is free, sub-directories can be used at will. Gzip-compressed files are
 * supported. The class handles the following files:
 * </p>
 * <ul>
 *   <li>
 *     any number of EOP C 04 files, whose base names match the pattern
 *     <code>eopc04_IAU2000.##</code> (or <code>eopc04_IAU2000.##.gz</code>
 *     for gzip-compressed files) where # stands for a digit character
 *   </li>
 *   <li>
 *     any number of bulletin B files, whose base names match the pattern
 *     <code>bulletinb_IAU2000-##.txt</code> (or
 *     <code>bulletinb_IAU2000-##.txt.gz</code> for gzip-compressed files)
 *     where # stands for a digit character
 *   </li>
 *   <li>
 *     one UTC time steps file which must be called <code>UTC-TAI.history</code>
 *     (or <code>UTC-TAI.history.gz</code> for a gzip-compressed file)
 *   </li>
 * </ul>
 * 
 * <p>
 * This is a singleton class since it handles voluminous datas. There is no public constructor.
 * </p>
 * 
 * @author Luc Maisonobe
 */
public class IERSData {

  /** Private constructor for the singleton.
   * @exception OrekitException if the IERS data cannot be loaded
   */
  private IERSData()
    throws OrekitException {

    // default values: no data at all
    eop       = new TreeSet();
    timeSteps = new Leap[0];

    // check the directory tree
    String directoryName = System.getProperty("orekit.iers.directory");
    if ((directoryName != null) && ! "".equals(directoryName)) {

      File directory = new File(directoryName);

      // safety checks
      if (! directory.exists()) {
        throw new OrekitException("IERS root directory {0} does not exist",
                                  new String[] {
                                    directory.getAbsolutePath()
                                  });
      }
      if (! directory.isDirectory()) {
        throw new OrekitException("{0} is not a directory",
                                  new String[] {
                                    directory.getAbsolutePath()
                                  });
      }

      // recursively explores the local cache
      FoundFiles files = new FoundFiles();
      findIERSFiles(files, directory);

      // check Earth Orientation Parameters continuity
      TreeSet availableMonths = new TreeSet();
      for (Iterator iterator = files.eopc04Yearly.iterator(); iterator.hasNext();) {
        Matcher matcher = yearlyPattern.matcher(((File) iterator.next()).getName());
        matcher.matches();
        int year = 2000 + Integer.parseInt(matcher.group(1));
        for (int month = 1; month <= 12; ++month) {
          // build the index for the bulletin B file containing
          // definitive data for this month
          Integer index = new Integer(getIndex(year, month));
          if (availableMonths.contains(index)) {
            throw new OrekitException("duplicated Earth Orientation Parameters for month {0}-{1}",
                                      new String[] {
                                        Integer.toString(year), Integer.toString(month)
                                      });            
          }
          availableMonths.add(index);
        }
      }
      for (Iterator iterator = files.bulletinBMonthly.iterator(); iterator.hasNext();) {
        Matcher matcher = monthlyPattern.matcher(((File) iterator.next()).getName());
        matcher.matches();
        Integer index = new Integer(Integer.parseInt(matcher.group(1)));
        if (availableMonths.contains(index)) {
          throw new OrekitException("duplicated Earth Orientation Parameters for month {0}-{1}",
                                    new String[] {
                                      Integer.toString(getYear(index.intValue())),
                                      Integer.toString(getMonth(index.intValue()))
                                    });            
        }
        availableMonths.add(index);
      }

      int index = -1;
      for (Iterator iterator = availableMonths.iterator(); iterator.hasNext();) {
        int previousIndex = index;
        index = ((Integer) iterator.next()).intValue();
        if ((previousIndex >= 0) && (index != previousIndex + 1)) {
          throw new OrekitException("missing Earth Orientation Parameters between {0}-{1} and {2}-{3}",
                                    new String[] {
                                      Integer.toString(getYear(previousIndex)),
                                      Integer.toString(getMonth(previousIndex)),
                                      Integer.toString(getYear(index)),
                                      Integer.toString(getMonth(index))
                                    });
        }
      }

      // load UTC-TAI history
      if (files.utcTaiHistory != null) {
        loadTimeStepsFile(files.utcTaiHistory);
      }

      // load EOP C 04 data      
      for (Iterator iterator = files.eopc04Yearly.iterator(); iterator.hasNext();) {
        loadYearlyFile((File) iterator.next());
      }

      // load bulletin B data
      for (Iterator iterator = files.bulletinBMonthly.iterator(); iterator.hasNext();) {
        loadMonthlyFile((File) iterator.next());
      }

    }
  }

  /** Get the singleton instance.
   * @return the singleton instance
   * @exception OrekitException if the IERS data cannot be loaded
   */
  public static IERSData getInstance()
    throws OrekitException {
    if (instance == null) {
      instance = new IERSData();
    }
    return instance;
  }

  /** Get the UTC time steps.
   * <p>The time steps are extracted from the
   * <code>UTC-TAI.history[.gz]</code> file.</p>
   * @return UTC time steps in chronological order
   */
  public Leap[] getTimeSteps() {
    return (Leap[])timeSteps.clone();
  }

  /** Get the Earth Orientation Parameter entries.
   * <p>The Earth Orientation Parameters are extracted from the
   * <code>eopc04_IAU2000.*[.gz]</code> yearly files and the
   * <code>bulletinb_IAU2000-*.txt[.gz]</code> monthly files.</p>
   * @return Earth Orientation Parameters entries
   */
  public TreeSet getEarthOrientationParameters() {
    return eop;
  }

  /** Get the date of the first available Earth Orientation Parameters.
   *  <p>The Earth Orientation Parameters are extracted from the
   * <code>eopc04_IAU2000.*[.gz]</code> yearly files chosen by the user.
   * 
   * @return the start date of the available datas
   * @throws OrekitException
   */
  public AbsoluteDate getFirstDate() throws OrekitException {
	  
	  if (firstDate==null){
		  long javaTime = (((EarthOrientationParameters)eop.first()).mjd - 40587) * 86400000l;
		  firstDate = new AbsoluteDate(new Date(javaTime), UTCScale.getInstance());
	  }
	  return firstDate;
 
  }
  
  /** Get the date of the last available Earth Orientation Parameters.
   *  <p>The Earth Orientation Parameters are extracted from the
   * <code>eopc04_IAU2000.*[.gz]</code> yearly files chosen by the user.
   * 
   * @return the end date of the available datas
   * @throws OrekitException
   */
  public AbsoluteDate getEndDate() throws OrekitException {
	  
	  if (endDate==null){
		  long javaTime = (((EarthOrientationParameters)eop.last()).mjd - 40587) * 86400000l;
		  endDate = new AbsoluteDate(new Date(javaTime), UTCScale.getInstance());
	  }
	  return endDate;
	  
  }
  
  /** Get the date of the first available known TUC steps.
   *  <p>The Steps are extracted from the 
   * <code>bulletinb_IAU2000-*.txt[.gz]</code> monthly files.</p>
   * @return the start date of the available datas
   * @throws ParseException
   * @throws OrekitException
   */
  public AbsoluteDate getUTCStartDate() throws ParseException, OrekitException {
	  
	  if (UTCStartDate==null){
		  AbsoluteDate ref = new AbsoluteDate("1970-01-01T00:00:00",UTCScale.getInstance());
		  UTCStartDate = new AbsoluteDate(ref,timeSteps[0].utcTime-timeSteps[0].step);
	  }
	  return UTCStartDate;
  }
   
  /** Get the year with definitive data from a bulletin B index.
   * @param index bulletin B index
   * @return year with definitive data in the corresponding bulletin B
   */
  private int getYear(int index) {
    return 1987 + (index + 11) / 12;
  }

  /** Get the month with definitive data from a bulletin B index.
   * @param index bulletin B index
   * @return month with definitive data in the corresponding bulletin B
   */
  private int getMonth(int index) {
    return 1 + (index + 11) % 12;
  }

  /** Get the bulletin B index containing definitive data for the given month.
   * @param year data year
   * @param month data month
   * @return bulletin B index
   */
  private int getIndex(int year, int month) {
    return 12 * (year - 1988) + month;
  }

  /** Recursively search for IERS data files in a directory tree.
   * @param files container where to put the files found
   * @param directory directory where to search
   * @exception IOException if some files are missing, duplicated or
   * monthly and yearly files overlap
   */
  private void findIERSFiles(FoundFiles files, File directory)
    throws OrekitException {

    // search in current directory
    File[] list = directory.listFiles();

    for (int i = 0; i < list.length; ++i) {
      if (list[i].isDirectory()) {
        // recurse in the sub-directory
        findIERSFiles(files, list[i]);
      } else  if (tuctaiPattern.matcher(list[i].getName()).matches()) {
        if (files.utcTaiHistory != null) {
          throw new OrekitException("several IERS UTC-TAI history files found: {0} and {1}",
                                    new String[] {
                                      files.utcTaiHistory.getAbsolutePath(),
                                      list[i].getAbsolutePath()
                                    });
        } else {
          files.utcTaiHistory = list[i];
        }
      } else  if (yearlyPattern.matcher(list[i].getName()).matches()) {
        files.eopc04Yearly.add(list[i]);
      } else  if (monthlyPattern.matcher(list[i].getName()).matches()) {
        files.bulletinBMonthly.add(list[i]);
      }
    }

  }

  /** Get a buffered reader for a file.
   * @param file file to read (considered compressed with gzip
   * if it ends with ".gz")
   * @return a buffered reader for the file
   * @exception IOException if the file cannot be opened for reading
   */
  private BufferedReader getReader(File file)
    throws IOException {
    InputStream is = new FileInputStream(file);
    if (file.getName().endsWith(".gz")) {
      // add the decompression filter
      is = new GZIPInputStream(is);
    }
    return new BufferedReader(new InputStreamReader(is));
  }

  /** Load an UTC time steps data file.
   * @param file file to read
   * @exception OrekitException if the data cannot be loaded
   */
  private void loadTimeStepsFile(File file)
    throws OrekitException {
    try {

      // the data lines in the UTS time steps data files have the following form:
      // 1966  Jan.  1 - 1968  Feb.  1     4.313 170 0s + (MJD - 39 126) x 0.002 592s
      // 1968  Feb.  1 - 1972  Jan.  1     4.213 170 0s +        ""
      // 1972  Jan.  1 -       Jul.  1    10s            
      //       Jul.  1 - 1973  Jan.  1    11s
      // 1973  Jan.  1 - 1974  Jan.  1    12s
      //  ...
      // 2006  Jan.  1.-                  33s
      // we ignore the non-constant and non integer offsets before 1972-01-01
      String yearField   = "\\p{Blank}*((?:\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit})|(?:    ))";
      String monthField  = "\\p{Blank}+(\\p{Upper}\\p{Lower}+)\\.?";
      String dayField    = "\\p{Blank}+([ 0-9]+)\\.?";
      String offsetField = "\\p{Blank}+(\\p{Digit}+)s";
      Pattern regularPattern =
        Pattern.compile("^" + yearField + monthField + dayField
                        + "\\p{Blank}*-\\p{Blank}+"
                        + yearField + monthField + dayField
                        + offsetField + "\\p{Blank}*$");
      Pattern lastPattern =
        Pattern.compile("^" + yearField + monthField + dayField
                        + "\\p{Blank}*-\\p{Blank}+"
                        + offsetField + "\\p{Blank}*$");
      SimpleDateFormat format = new SimpleDateFormat("yyyy MMM dd Z", Locale.US);

      // read all file, ignoring not recognized lines
      int lineNumber = 0;
      ArrayList leaps = new ArrayList();
      Leap last = null;
      int lastLine = 0;
      String previousYear = "    ";
      BufferedReader reader = getReader(file);
      for (String line = reader.readLine(); line != null; line = reader.readLine()) {
        ++lineNumber;

        // check matching for regular lines and last line
        Matcher matcher = regularPattern.matcher(line);
        if (matcher.matches()) {
          if (lastLine > 0) {
            throw new OrekitException("unexpected data line {0} in file {1}"
                                      + "(line {2} should not be followed by data)",
                                      new String[] {
                                        Integer.toString(lineNumber),
                                        file.getAbsolutePath(),
                                        Integer.toString(lastLine)
                                      });
          }
        } else {
          matcher = lastPattern.matcher(line);
          if (matcher.matches()) {
            // this is the last line (there is a start date but no end date)
            lastLine = lineNumber;
          }
        }

        if (matcher.matches()) {
          try {
            // build an entry from the extracted fields

            String year = matcher.group(1);
            if ("    ".equals(year)) {
              year = previousYear;
            }
            if (lineNumber != lastLine) {
              if ("    ".equals(matcher.group(4))) {
                previousYear = year;
              } else {
                previousYear = matcher.group(4);
              }
            }
            String sDate =
              year + ' ' + matcher.group(2) + ' ' + matcher.group(3) + " +0000";
            double utcTime = format.parse(sDate).getTime() * 1.0e-3;
            if ((last != null) && (utcTime < last.utcTime)) {
              throw new OrekitException("non-increasing dates in file {0}, line {1}",
                                        new String[] {
                                          file.getAbsolutePath(),
                                          Integer.toString(lineNumber)
                                        });
            }

            double offset = -Double.parseDouble(matcher.group(matcher.groupCount()));
            last = new Leap(utcTime,
                            offset - ((last == null) ? 0 : last.offsetAfter),
                            offset);
            leaps.add(last);

          } catch (NumberFormatException nfe) {
            throw new OrekitException("unable to parse line {0} in IERS UTC-TAI history file {1}",
                                      new String[] {
                                        Integer.toString(lineNumber),
                                        file.getAbsolutePath()
                                      });
          }
         }
      }

      if (leaps.isEmpty()) {
        throw new OrekitException("file {0} is not an IERS UTC-TAI history file",
                                  new String[] {
                                    file.getAbsolutePath()
                                  });        
      }
      timeSteps = (Leap[]) leaps.toArray(new Leap[leaps.size()]);

    } catch (ParseException pe) {
      throw new OrekitException(pe.getMessage(), pe);
    } catch (IOException ioe) {
      throw new OrekitException(ioe.getMessage(), ioe);
    }
  }

  /** Load an IERS EOP C 04 data file.
   * @param file file to read
   * @exception OrekitException if the data cannot be loaded
   */
  private void loadYearlyFile(File file)
    throws OrekitException {
    try {

      double arcSecondsToRadians = 2 * Math.PI / 1296000;

      // the data lines in the EOP C 04 yearly data files have the following fixed form:
      //   JAN   1  52275-0.176980 0.293952-0.1158223   0.0008163    0.00044  0.00071
      //   JAN   2  52276-0.177500 0.297468-0.1166973   0.0009382    0.00030  0.00043
      // the corresponding fortran format is:
      //  2X,A4,I3,2X,I5,2F9.6,F10.7,2X,F10.7,2X,2F9.5
      String yearField  = "\\p{Upper}\\p{Upper}\\p{Upper}\\p{Blank}";
      String dayField   = "\\p{Blank}[ 0-9]\\p{Digit}";
      String mjdField   = "(\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit})";
      String poleField  = "(.........)";
      String dtU1Field  = "(..........)";
      String lodField   = "..........";
      String deltaField = ".........";
      Pattern pattern = Pattern.compile("^  " + yearField + dayField + "  "
                                        + mjdField + poleField + poleField
                                        + dtU1Field + "  " + lodField
                                        + "  " + deltaField + deltaField + "\\p{Blank}*$");

      // read all file, ignoring header
      int lineNumber = 0;
      boolean inHeader = true;
      BufferedReader reader = getReader(file);
      for (String line = reader.readLine(); line != null; line = reader.readLine()) {
        ++lineNumber;
        boolean parsed = false;
        Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
          inHeader = false;
          try {
            // this is a data line, build an entry from the extracted fields
            int    date = Integer.parseInt(matcher.group(1));
            double x    = Double.parseDouble(matcher.group(2)) * arcSecondsToRadians;
            double y    = Double.parseDouble(matcher.group(3)) * arcSecondsToRadians;
            double dtu1 = Double.parseDouble(matcher.group(4));
            eop.add(new EarthOrientationParameters(date, dtu1, new PoleCorrection(x, y)));
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

  /** Compute the mjd for a calendar date.
   * @param year  date year
   * @param month date month
   * @param day   date day
   * @return modified julian day
   */
  private int mjd(int year, int month, int day) {
    boolean isLeap =
      (year % 4 == 0) || ((year % 100 == 0) && (year % 400 != 0));
    int c = (year - 1) / 100;
    return (1461 * (year - 1)) / 4 + c / 4 - c
         + (month * 489) / 16 - ((month > 2) ? (isLeap ? 32 : 31) : 30)
         + day - 678577;
  }

  /** Load a bulletin B data file.
   * @param file file to read
   * @exception OrekitException if the data cannot be loaded
   */
  private void loadMonthlyFile(File file)
    throws OrekitException {
    try {

      // Compute mjd bounds for the month having definitive data
      Matcher matcher = monthlyPattern.matcher(file.getName());
      matcher.matches();
      int index  = Integer.parseInt(matcher.group(1));
      int year   = 1987 + (index + 11) / 12;
      int month  = 1 + (index + 11) % 12;
      int mjdMin = mjd(year, month, 1);
      int mjdMax = ((month < 12) ? mjd(year, month + 1, 1) : mjd(year + 1, 1, 1)) - 1;

      double arcSecondsToRadians = 2 * Math.PI / 1296000;

      // the section headers lines in the bulletin B monthly data files have the following form:
      // 1 - EARTH ORIENTATION PARAMETERS (IERS evaluation).
      // 2 - SMOOTHED VALUES OF x, y, UT1, D, dX, dY (IERS EVALUATION)
      // 3 - NORMAL VALUES OF THE EARTH ORIENTATION PARAMETERS AT FIVE-DAY INTERVALS 
      // 4 - DURATION OF THE DAY AND ANGULAR VELOCITY OF THE EARTH (IERS evaluation).
      // 5 - INFORMATION ON TIME SCALES 
      //       6 - SUMMARY OF CONTRIBUTED EARTH ORIENTATION PARAMETERS SERIES
      Pattern sectionHeaderPattern =
        Pattern.compile("^ +([123456]) - \\p{Upper}+ \\p{Upper}+ \\p{Upper}+ ");

      // the data lines in the bulletin B monthly data files have the following form:
      //   NOV   5  53679   0.07316  0.39628 -0.628071  -32.628071    0.27   -0.24
      //   NOV  10  53684   0.07220  0.39438 -0.631300  -32.631300    0.35    0.02
      String yearField       = "\\p{Upper}\\p{Upper}\\p{Upper}\\p{Blank}";
      String dayField        = "\\p{Blank}+[ 0-9]\\p{Digit}";
      String mjdField        = "\\p{Blank}+(\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit})";
      String storedRealField = "\\p{Blank}+(-?\\p{Digit}+\\.(?:\\p{Digit})+)";
      String ignoredealField = "\\p{Blank}+-?\\p{Digit}+\\.(?:\\p{Digit})+";
      Pattern dataPattern =
        Pattern.compile("^" + yearField + dayField + mjdField
                        + storedRealField + storedRealField + storedRealField
                        + ignoredealField + ignoredealField + ignoredealField
                        + "\\p{Blank}*$");

      // read the data lines in section 2
      int section = 0;
      BufferedReader reader = getReader(file);
      for (String line = reader.readLine(); line != null; line = reader.readLine()) {
        matcher = sectionHeaderPattern.matcher(line);
        if (matcher.matches()) {
          section = Integer.parseInt(matcher.group(1));
        } else if (section == 2){
          matcher = dataPattern.matcher(line);
          if (matcher.matches()) {
            // this is a data line, build an entry from the extracted fields
            int    date = Integer.parseInt(matcher.group(1));
            double x    = Double.parseDouble(matcher.group(2)) * arcSecondsToRadians;
            double y    = Double.parseDouble(matcher.group(3)) * arcSecondsToRadians;
            double dtu1 = Double.parseDouble(matcher.group(4));
            if (date >= mjdMin) {
              eop.add(new EarthOrientationParameters(date, dtu1, new PoleCorrection(x, y)));
            } else if (date > mjdMax) {
              // don't bother reading the rest of the file
              return;
            }
          }
        }
      }
    } catch (IOException ioe) {
      throw new OrekitException(ioe.getMessage(), ioe);
    }
  }

  /** Container for {@link #findIERSFiles} results. */
  private static class FoundFiles {

    /** Simple constructor.
     */
    public FoundFiles() {
      eopc04Yearly     = new TreeSet(new YearlyFilesComparator());
      bulletinBMonthly = new TreeSet(new MonthlyFilesComparator());
    }

    private static class YearlyFilesComparator implements Comparator {
      public int compare(Object arg0, Object arg1) {
        Matcher matcher0 = yearlyPattern.matcher(((File) arg0).getName());
        matcher0.matches();
        int year0 = Integer.parseInt(matcher0.group(1));
        Matcher matcher1 = yearlyPattern.matcher(((File) arg1).getName());
        matcher1.matches();
        int year1 = Integer.parseInt(matcher1.group(1));
        return year0 - year1;
      }
    }
    
    private static class MonthlyFilesComparator implements Comparator {
      public int compare(Object arg0, Object arg1) {
        Matcher matcher0 = monthlyPattern.matcher(((File) arg0).getName());
        matcher0.matches();
        int year0 = Integer.parseInt(matcher0.group(1));
        Matcher matcher1 = monthlyPattern.matcher(((File) arg1).getName());
        matcher1.matches();
        int year1 = Integer.parseInt(matcher1.group(1));
        return year0 - year1;
      }
    }

    /** UTC-TAI history file. */
    public File utcTaiHistory;

    /** EOP C 04 yearly files. */
    public TreeSet eopc04Yearly;

    /** Bulletin B monthly files. */
    public TreeSet bulletinBMonthly;

  }
  
  /** Earth Orientation Parameters entries. */
  private TreeSet eop;

  /** UTC time steps. */
  private Leap[] timeSteps;

  /** Singleton instance. */
  private static IERSData instance = null;

  /** UTC-TAI file names pattern. */
  private static final Pattern tuctaiPattern =
    Pattern.compile("^UTC-TAI\\.history(?:\\.gz)?$");

  /** EOP C 04 file names pattern. */
  private static final Pattern yearlyPattern =
    Pattern.compile("^eopc04_IAU2000\\.(\\d\\d)(?:\\.gz)?$");

  /** Bulletin B file names pattern. */
  private static final Pattern monthlyPattern =
    Pattern.compile("^bulletinb_IAU2000-(\\d\\d\\d)\\.txt(?:\\.gz)?$");
  
  /** first date of the EOPC04 files datas */
  private AbsoluteDate firstDate;

  /** final date of the EOPC04 files datas */
  private AbsoluteDate endDate;
  
  /** start of the UTC steps inventory */
  private AbsoluteDate UTCStartDate;

}
