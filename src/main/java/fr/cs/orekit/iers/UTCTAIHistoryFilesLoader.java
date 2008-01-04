package fr.cs.orekit.iers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.cs.orekit.errors.OrekitException;

/** Loader for UTC versus TAI history files.
 * <p>UTC versus TAI history files contain {@link Leap
 * leap seconds} data since.</p>
 * <p>The UTC versus TAI history files are recognized thanks to their
 * base names, which must match the pattern <code>UTC-TAI.history</code>
 * (or <code>UTC-TAI.history.gz</code> for gzip-compressed files)</p>
 * <p>Only one history file must be present in the IERS directories
 * hierarchy.</p>
 * @author Luc Maisonobe
 */
public class UTCTAIHistoryFilesLoader extends IERSFileVisitor {

  public UTCTAIHistoryFilesLoader() {

    super("^UTC-TAI\\.history(?:\\.gz)?$");

    // the data lines in the UTC time steps data files have the following form:
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
    regularPattern = Pattern.compile("^" + yearField + monthField + dayField
                                     + "\\p{Blank}*-\\p{Blank}+"
                                     + yearField + monthField + dayField
                                     + offsetField + "\\p{Blank}*$");
    lastPattern    = Pattern.compile("^" + yearField + monthField + dayField
                                     + "\\p{Blank}*-\\p{Blank}+"
                                     + offsetField + "\\p{Blank}*$");

    format = new SimpleDateFormat("yyyy MMM dd Z", Locale.US);

  }

  /** Get the UTC time steps.
   * <p>The time steps are extracted from the
   * <code>UTC-TAI.history[.gz]</code> file.</p>
   * @return UTC time steps in <em>reversed</em> chronological order
   * @exception OrekitException if some data can't be read or some
   * file content is corrupted
   */
  public Leap[] getTimeSteps()
    throws OrekitException {

    leaps = new ArrayList();
    new IERSDirectoryCrawler().crawl(this);
    Leap[] timeSteps = (Leap[]) leaps.toArray(new Leap[leaps.size()]);

    // put the array in reversed chronological order
    // to optimize access to the more recent step (at index 0)
    for (int i = 0, j = timeSteps.length - 1; i < j; ++i, --j) {
      Leap l   = timeSteps[i];
      timeSteps[i] = timeSteps[j];
      timeSteps[j] = l;
    }

    return timeSteps;

  }

  protected void visit(BufferedReader reader)
  throws OrekitException, IOException, ParseException {

    if (readFile != null) {
      throw new OrekitException("several IERS UTC-TAI history files found: {0} and {1}",
                                new Object[] {
                                  readFile.getAbsolutePath(),
                                  file.getAbsolutePath()
                                });
    }

    // read all file, ignoring not recognized lines
    int lineNumber = 0;
    Leap last = null;
    int lastLine = 0;
    String previousYear = "    ";
    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
      ++lineNumber;

      // check matching for regular lines and last line
      Matcher matcher = regularPattern.matcher(line);
      if (matcher.matches()) {
        if (lastLine > 0) {
          throw new OrekitException("unexpected data line {0} in file {1}"
                                    + "(line {2} should not be followed by data)",
                                    new Object[] {
                                        new Integer(lineNumber),
                                        file.getAbsolutePath(),
                                        new Integer(lastLine)
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
                                      new Object[] {
                                        file.getAbsolutePath(),
                                        new Integer(lineNumber)
                                      });
          }

          double offset = -Double.parseDouble(matcher.group(matcher.groupCount()));
          last = new Leap(utcTime,
                          offset - ((last == null) ? 0 : last.offsetAfter),
                          offset);
          leaps.add(last);

        } catch (NumberFormatException nfe) {
          throw new OrekitException("unable to parse line {0} in IERS UTC-TAI history file {1}",
                                    new Object[] {
                                      new Integer(lineNumber),
                                      file.getAbsolutePath()
                                    });
        }
      }
    }

    if (leaps.isEmpty()) {
      throw new OrekitException("file {0} is not an IERS UTC-TAI history file",
                                new Object[] { file.getAbsolutePath() });        
    }

    readFile = file;

  }

  /** Already read file. */
  private File readFile;

  /** Regular data lines pattern. */
  private Pattern regularPattern;

  /** Last line pattern pattern. */
  private Pattern lastPattern;

  /** Parsing format for reconstructed dates. */
  private SimpleDateFormat format;

  /** Time leaps. */
  private ArrayList leaps;

}
