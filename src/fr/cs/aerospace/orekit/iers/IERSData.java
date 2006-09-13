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
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.PoleCorrection;

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
 *     one UTC time steps files which must be called <code>UTC-TAI.history</code>
 *     (or <code>UTC-TAI.history.gz</code> for gzip-compressed files)
 *   </li>
 * </ul>
 * 
 * <p>
 * This is a singleton class, so there is no public constructor.
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
    eopc04    = new TreeSet();
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
      recurseLoadData(directory,
                      Pattern.compile("^UTC-TAI\\.history(?:\\.gz)?$"),
                      Pattern.compile("^eopc04_IAU2000\\.\\d\\d(?:\\.gz)?$"));

      if (! eopc04.isEmpty()) {
        // check for duplicated entries or holes in the loaded files
        Iterator iterator = eopc04.iterator();
        Eopc04Entry next = (Eopc04Entry) iterator.next();
        while (iterator.hasNext()) {
          Eopc04Entry previous = next;
          next = (Eopc04Entry) iterator.next();
          if (next.mjd == previous.mjd) {
            throw new OrekitException("duplicated IERS data at modified julian day {0}",
                                      new String[] {
                                        Integer.toString(previous.mjd)
                                      });
          }
          if ((next.mjd - previous.mjd) >= 6) {
            throw new OrekitException("missing IERS data between modified julian days {0} and {1}",
                                      new String[] {
                                        Integer.toString(previous.mjd),
                                        Integer.toString(next.mjd)
                                      });
          }
        }
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
   * @return UTC time steps in chronological order
   */
  public Leap[] getTimeSteps() {
    return timeSteps;
  }

  /** Get the Earth Orientation Parameter entries.
   * @return Earth Orientation Parameter entries
   */
  public TreeSet getEopc04Entries() {
    return eopc04;
  }

  /** Recursively search for IERS data files in a directory tree.
   * @param directory directory where to search
   * @param timeStepsPattern pattern for UTC time steps file
   * @param yearlyPattern pattern for yearly EOPC 04 data files
   * @exception OrekitException if the IERS data cannot be loaded
   */
  private void recurseLoadData(File directory,
                               Pattern timeStepsPattern,
                               Pattern yearlyPattern)
    throws OrekitException {

    // search in current directory
    File[] list = directory.listFiles();

    for (int i = 0; i < list.length; ++i) {
      if (list[i].isDirectory()) {
        // recurse in the sub-directory
        recurseLoadData(list[i], timeStepsPattern, yearlyPattern);
      } else  if (timeStepsPattern.matcher(list[i].getName()).matches()) {
        loadTimeStepsFile(list[i]);
      } else  if (yearlyPattern.matcher(list[i].getName()).matches()) {
        loadYearlyFile(list[i]);
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

      if (timeStepsFile == null) {
        timeStepsFile = file;
      } else {
        throw new OrekitException("several IERS UTC-TAI history files found: {0} and {1}",
                                  new String[] {
                                    timeStepsFile.getAbsolutePath(),
                                    file.getAbsolutePath()
                                  });
      }

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
            eopc04.add(new Eopc04Entry(date, dtu1, new PoleCorrection(x, y)));
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

  /** EOP C 04 entries. */
  private TreeSet eopc04;

  /** UTC time steps. */
  private Leap[] timeSteps;

  /** UTC times steps history file. */
  private File timeStepsFile;

  /** Singleton instance. */
  private static IERSData instance = null;

}
