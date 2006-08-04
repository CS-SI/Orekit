package fr.cs.aerospace.orekit.time;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import fr.cs.aerospace.orekit.errors.OrekitException;

/** Coordinated Universal Time.
 * <p>UTC is related to TAI using step adjustments from time to time
 * according to IERS (International Earth Rotation Service) rules. This
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
 * <p> The OREKIT library embeds current time steps data known at the
 * library publishing date.Users can provide updated data by setting the
 * <code>orekit.time-step.file</code> Java property to the name of an
 * XML file containing such data. The following example shows the
 * format of this file:</p>
 * <pre>
 * <?xml version="1.0"?>
 * 
 * <!-- This file contains the history of time steps between UTC and TAI -->
 * <!-- The data has been retrieved from the IERS site                   -->
 * <!-- http://hpiers.obspm.fr/eoppc/bul/bulc/TimeSteps.history          -->
 * 
 * <time-steps>
 *   <leap date="1972-01-01" step="-10" />
 *   <leap date="1972-07-01" step="-1"  />
 *   <leap date="1973-01-01" step="-1"  />
 *   <leap date="1974-01-01" step="-1"  />
 *   <leap date="1975-01-01" step="-1"  />
 *   <leap date="1976-01-01" step="-1"  />
 *   <leap date="1977-01-01" step="-1"  />
 *   <leap date="1978-01-01" step="-1"  />
 *   <leap date="1979-01-01" step="-1"  />
 *   <leap date="1980-01-01" step="-1"  />
 *   <leap date="1981-07-01" step="-1"  />
 *   <leap date="1982-07-01" step="-1"  />
 *   <leap date="1983-07-01" step="-1"  />
 *   <leap date="1985-07-01" step="-1"  />
 *   <leap date="1988-01-01" step="-1"  />
 *   <leap date="1990-01-01" step="-1"  />
 *   <leap date="1991-01-01" step="-1"  />
 *   <leap date="1992-07-01" step="-1"  />
 *   <leap date="1993-07-01" step="-1"  />
 *   <leap date="1994-07-01" step="-1"  />
 *   <leap date="1996-01-01" step="-1"  />
 *   <leap date="1997-07-01" step="-1"  />
 *   <leap date="1999-01-01" step="-1"  />
 *   <leap date="2006-01-01" step="-1"  />
 * </time-steps>
 * </pre>
 * <p>This is a singleton class, so there is no public constructor.</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 */
public class UTCScale extends TimeScale {

  /** Private constructor for the singleton.
   */
  private UTCScale() {
    super("UTC");

    try {

      // choose the data source
      String fileName = System.getProperty("orekit.time-step.file");
      InputStream stream;
      if ((fileName != null) && new File(fileName).exists()) {
        // use the user-provided file
        try {
          stream = new FileInputStream(fileName);
        } catch (FileNotFoundException fnfe) {
          // should not happen
          throw new RuntimeException("internal error");
        }
      } else {
        // use the embedded time steps file
        stream =
          getClass().getResourceAsStream("/fr/cs/aerospace/orekit/resources/time-steps.xml");
      }

      // read the time-steps data
      XMLReader reader = XMLReaderFactory.createXMLReader();
      TimeStepsHandler handler = new TimeStepsHandler();
      reader.setContentHandler(handler);
      reader.setErrorHandler(handler);
      reader.parse(new InputSource(stream));

      // put the most recent leap first,
      // as it will often be the only one really used
      leaps = handler.getTimeSteps();
      for (int i = 0, j = leaps.length - 1; i < j; ++i, --j) {
        Leap l   = leaps[i];
        leaps[i] = leaps[j];
        leaps[j] = l;
      }

    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    } catch (SAXException se) {
      if (se.getCause() != null) {
        // we may have embedded ParseException, OrekitException ...
        throw new RuntimeException(se.getCause());
      }
      throw new RuntimeException(se);
    }

  }

  /* Get the uniq instance of this class.
   * @return the uniq instance
   */
  public static TimeScale getInstance() {
    if (instance == null) {
      instance = new UTCScale();
    }
    return instance;
  }

  /** Get the offset to convert locations from {@link TAI} to instance.
   * @param taiTime location of an event in the {@link TAI} time scale
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

  /** Get the offset to convert locations from instance to {@link TAI}.
   * @param instanceTime location of an event in the instance time scale
   * as a seconds index starting at 1970-01-01T00:00:00
   * @return offset to <em>add</em> to instanceTime to get a location
   * in {@link TAI} time scale
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

  /** XML parsing utility class. */
  private class TimeStepsHandler extends DefaultHandler {

    public TimeStepsHandler() {
      format = new SimpleDateFormat("yyyy-MM-dd");
      format.setTimeZone(TimeZone.getTimeZone("UTC"));
      leaps  = new ArrayList();
      last   = null;
    }

    public void startElement(String namespaceURI, String localName,
                             String qName, Attributes attributes)
      throws SAXException {
      try {
        String eltName = "".equals(namespaceURI) ? qName : localName;
        if (eltName.equals("time-steps")) {
          // do nothing
        } else if (eltName.equals("leap")) {
          String dateString = getAttribute(attributes, "date");
          String stepString = getAttribute(attributes, "step");
          try {
            double utcTime = format.parse(dateString).getTime() * 1.0e-3;
            double step    = Double.parseDouble(stepString);
            double offset  = ((last == null) ? 0 : last.offsetAfter) + step;
            if ((last != null) && (utcTime < last.utcTime)) {
              throw new OrekitException("non-increasing dates in UTC/TAI"
                                      + " time steps file ({0})",
                                        new String[] { dateString });
            }
            last = new Leap(utcTime, step, offset);
            leaps.add(last);
          } catch (ParseException pe) {
            throw new OrekitException("unparsable date in UTC/TAI"
                                    + " time steps file ({0})",
                                      new String[] { dateString });
          } catch (NumberFormatException nfe) {
            throw new OrekitException("unparsable step value in UTC/TAI"
                                    + " time steps file ({0})",
                                      new String[] { stepString });
          }
        } else {
          throw new OrekitException("unexpected element \"{0}\""
                                  + " in UTC/TAI time steps file",
                                    new String[] { eltName });
        }
      } catch (OrekitException oe) {
        throw new SAXException(oe);
      }
    }

    private String getAttribute(Attributes attributes, String name)
      throws OrekitException {
      String attribute = attributes.getValue(name);
      if (attribute == null) {
        throw new OrekitException(
          "missing attribute \"{0}\" in UTC/TAI time steps file",
          new String[] { name });
      }
      return attribute;
    }

    /** Get the time steps.
     * @return time steps
     */
    public Leap[] getTimeSteps() {
      return (Leap[]) leaps.toArray(new Leap[leaps.size()]);
    }

    private SimpleDateFormat format;
    private ArrayList        leaps;
    private Leap             last;

  }

  /** Time steps.
   * <p>This class i a simple container.</p>
   */
  private static class Leap {

    /** Time in UTC at which the step occurs. */
    public final double utcTime;

    /** Step value. */
    public final double step;

    /** Offset in seconds after the leap. */
    public final double offsetAfter;

    /** Simple constructor.
     * @param utcTime time in UTC at which the step occurs
     * @param step step value
     * @param offsetAfter offset in seconds after the leap
     */
    public Leap(double utcTime, double step, double offsetAfter) {
      this.utcTime      = utcTime;
      this.step         = step;
      this.offsetAfter = offsetAfter;
    }

  }

  /** Uniq instance. */
  private static TimeScale instance = null;

  /** Time steps. */
  private Leap[] leaps;

}
