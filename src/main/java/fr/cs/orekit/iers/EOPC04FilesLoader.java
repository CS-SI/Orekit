package fr.cs.orekit.iers;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.PoleCorrection;

/** Loader for EOP C 04 files.
 * <p>EOP C 04 files contain {@link EarthOrientationParameters
 * Earth Orientation Parameters} for one year periods.</p>
 * <p>The EOP C 04 files are recognized thanks to their base names,
 * which must match the pattern <code>eopc04_IAU2000.##</code>
 * (or <code>eopc04_IAU2000.##.gz</code> for gzip-compressed files)
 * where # stands for a digit character.</p>
 * @author Luc Maisonobe
 */
public class EOPC04FilesLoader extends IERSFileVisitor {

  public EOPC04FilesLoader() {

    super("^eopc04_IAU2000\\.(\\d\\d)(?:\\.gz)?$");

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
    dataPattern = Pattern.compile("^  " + yearField + dayField + "  "
                                  + mjdField + poleField + poleField
                                  + dtU1Field + "  " + lodField
                                  + "  " + deltaField + deltaField + "\\p{Blank}*$");

    arcSecondsToRadians = 2 * Math.PI / 1296000;

  }

  /** Load Earth Orientation Parameters.
   * <p>The data is concatenated from all EOP C 04 data files
   * which can be found in the configured IERS directory.</p>
   * @param eop set where to <em>add</em> EOP data (pre-existing
   * data is preserved)
   * @exception OrekitException if some data can't be read or some
   * file content is corrupted
   */
  public void loadEOP(TreeSet eop)
    throws OrekitException {
    this.eop = eop;
    new IERSDirectoryCrawler().crawl(this);
  }

  protected void visit(BufferedReader reader)
    throws IOException, OrekitException {

    // read all file, ignoring header
    int lineNumber = 0;
    boolean inHeader = true;
    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
      ++lineNumber;
      boolean parsed = false;
      Matcher matcher = dataPattern.matcher(line);
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

    // check if we have read something
    if (inHeader) {
      throw new OrekitException("file {0} is not an IERS data file",
                                new String[] { file.getAbsolutePath() });        
    }

  }

  /** Data line pattern. */
  private Pattern dataPattern;

  /** Conversion factor. */
  private double arcSecondsToRadians;

  /** Earth Orientation Parameters entries. */
  private TreeSet eop;

}
