package fr.cs.aerospace.orekit.tle;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.time.UTCScale;

/** This class converts and contains TLE datas.
 * 
 * @author F. Maussion
 */
public class TLE  implements Comparable {

  /** Simple constructor with one TLE.
   * <p> The static method {@link #isFormatOK(String, String)} should be called
   * before trying to build this object. <p>
   * @param line1 the first element (69 char String)
   * @param line2 the second element (69 char String)
   * @throws OrekitException if some format error occurs
   */
  public TLE(String line1, String line2) throws OrekitException {

    satelliteNumber = Integer.parseInt(line1.substring(2,7).replace(" ", "0"));
    internationalDesignator = line1.substring(9,17);

    // Date format transform :
    int year = Integer.parseInt(line1.substring(18,20).replace(" ", "0"));
    AbsoluteDate t;
    if (year<57) {
      try {
        year = 2000 + year;
        t = new AbsoluteDate(Integer.toString(year)+"-01-01T00:00:00",
                             UTCScale.getInstance());
      } catch (ParseException e) {
        // should not happen
        throw new RuntimeException(e);
      }
    }
    else {
      try {
        year = 1900 + year;
        t = new AbsoluteDate(Integer.toString(year)+"-01-01T00:00:00",
                             UTCScale.getInstance());
      } catch (ParseException e) {
        // should not happen
        throw new RuntimeException(e);
      }
    }
    double dayNb = Double.parseDouble(line1.substring(20,32).replace(" ", "0"));
    epoch = new AbsoluteDate(t, (dayNb-1.0)*86400);
    bStar = Double.parseDouble(line1.substring(53,54).replace(" ", "0") + "." + line1.substring(54,59).replace(" ", "0") +
                               "e" + line1.substring(59,61).replace(" ", "0"));
    ephemerisType = Integer.parseInt(line1.substring(62,63).replace(" ", "0"));
    elementNumber = Integer.parseInt(line1.substring(64,68).replace(" ", "0"));
    i = Math.toRadians(Double.parseDouble(line2.substring(8,16).replace(" ", "0")));
    raan = Math.toRadians(Double.parseDouble(line2.substring(17,25).replace(" ", "0")));
    e = Double.parseDouble("."+line2.substring(26,33).replace(" ", "0"));
    pa = Math.toRadians(Double.parseDouble(line2.substring(34,42).replace(" ", "0")));
    meanAnomaly = Math.toRadians(Double.parseDouble(line2.substring(43,51).replace(" ", "0")));
    meanMotion = Math.PI * Double.parseDouble(line2.substring(52,63).replace(" ", "0")) / (43200.0);
    
    revolutionNumberAtEpoch = Integer.parseInt(line2.substring(63,68).replace(" ", "0"));

  }

  /** Specific constructor for series handling. */
  protected TLE(AbsoluteDate date) {
    epoch = date;
    satelliteNumber = 0;
    internationalDesignator = new String();
    bStar = 0;
    ephemerisType = 0;
    elementNumber = 0;
    i = 0;
    raan = 0;
    e = 0;
    pa = 0;
    meanAnomaly = 0;
    meanMotion = 0;
    revolutionNumberAtEpoch = 0;
  }

  /** Compare an entry with another one, according to date. 
   * <p> Entry should be a TLE, to avoid ClassCastExceptions. </p>
   */
  public int compareTo(Object entry) {
    return epoch.compareTo(((TLE)entry).getEpoch());
  }

  /** The satellite id */
  private final int satelliteNumber;

  /** International designator */
  private final String internationalDesignator;

  /** the TLE current date */
  private final AbsoluteDate epoch;

  /** Ballistic coefficient */
  private final double bStar;

  /** Type of ephemeris */
  private final int ephemerisType;

  /** Line element number */
  private final int elementNumber;

  /** Inclination (rad) */
  private final double i;

  /** Right Ascension of the Ascending node (rad) */
  private final double raan;

  /** Eccentricity */
  private final double e;

  /** Argument of perigee (rad) */
  private final double pa;

  /** Mean anomaly (rad) */
  private final double meanAnomaly;

  /** Mean Motion (rad/sec) */
  private final double meanMotion;

  /** revolution number at epoch */
  private final int revolutionNumberAtEpoch;

  /** Identifier for default type of ephemeris (SGP4/SDP4). */
  public static final int DEFAULT = 0;

  /** Identifier for SGP type of ephemeris. */
  public static final int SGP = 1;

  /** Identifier for SGP4 type of ephemeris. */
  public static final int SGP4 = 2;

  /** Identifier for SDP4 type of ephemeris. */
  public static final int SDP4 = 3;

  /** Identifier for SGP8 type of ephemeris. */
  public static final int SGP8 = 4;

  /** Identifier for SDP8 type of ephemeris. */
  public static final int SDP8 = 5;

  /** Gets the ballistic coefficient.
   * @return bStar
   */
  public double getBStar() {
    return bStar;
  }

  /** Gets the eccentricity.
   * @return the eccentricity
   */
  public double getE() {
    return e;
  }

  /** Gets the type of ephemeris.
   * @return the ephemeris type (one of {@link #DEFAULT}, {@link #SGP},{@link #SGP4},{@link #SGP8},
   *                                    {@link #SDP4},{@link #SDP8})
   */
  public int getEphemerisType() {
    return ephemerisType;
  }

  /** Gets the TLE current date
   * @return the epoch
   */
  public AbsoluteDate getEpoch() {
    return epoch;
  }

  /** Gets the inclination
   * @return the inclination (rad)
   */
  public double getI() {
    return i;
  }

  /** Gets the international designator.
   * @return the internationalDesignator
   */
  public String getInternationalDesignator() {
    return internationalDesignator;
  }

  /** Gets the  element number.
   * @return the element number
   */
  public int getElementNumber() {
    return elementNumber;
  }

  /** Gets the mean anomaly.
   * @return the mean anomaly  (rad)
   */
  public double getMeanAnomaly() {
    return meanAnomaly;
  }

  /** Gets the mean motion.
   * @return the meanMotion (rad/s)
   */
  public double getMeanMotion() {
    return meanMotion;
  }

  /** Gets the argument of perigee.
   * @return omega (rad)
   */
  public double getPerigeeArgument() {
    return pa;
  }

  /** Gets Right Ascension of the Ascending node
   * @return the raan (rad)
   */
  public double getRaan() {
    return raan;
  }

  /** Gets the revolution number.
   * @return the revolutionNumberAtEpoch
   */
  public int getRevolutionNumberAtEpoch() {
    return revolutionNumberAtEpoch;
  }

  /** Gets the satellite id.
   * @return the satellite number
   */
  public int getSatelliteNumber() {
    return satelliteNumber;
  }  

  /** Check the entries to determine if the element format is correct.
   * @param line1 the first element (69 char String)
   * @param line2 the second element (69 char String)
   * @return true if format is recognised, false if not
   * @throws OrekitException if checksum is not valid
   */
  public static boolean isFormatOK(String line1, String line2) throws OrekitException {

    if (line1.length()!=69 ||line2.length()!=69 ) {
      return false;
    } 
    if(isLine1OK(line1)&&isLine2OK(line2)) {

      // check sums 
      int chksum1  = 0;
      int chksum2  = 0;

      for(int j = 0 ; j<68; j++) {
        String x = line1.substring(j,j+1);
        String y = line2.substring(j,j+1);
        try {
          chksum1 += Integer.parseInt(x);
        } catch(NumberFormatException nb) {
          if (x.equals("-")) chksum1++;
        }       
        try {
          chksum2 += Integer.parseInt(y);
        } catch(NumberFormatException nb) {
          if (y.equals("-")) chksum2++;
        }       
      }

      double decimal = chksum1/10.0;
      decimal = (decimal - Math.floor(decimal))*10;
      if(Integer.parseInt(line1.substring(68))!= Math.round(decimal)) {
        throw new OrekitException("Cheksum of line 1 is not correct. Should be: {0} but is: {1}",
                                  new String[] {
            line1.substring(68) ,
            Double.toString(Math.round(decimal))
        });
      }
      decimal = chksum2/10.0;
      decimal = (decimal - Math.floor(decimal))*10;
      if(Integer.parseInt(line2.substring(68))!= Math.round(decimal)) {
        throw new OrekitException("Cheksum of line 2 is not correct. Should be: {0} but is: {1}",
                                  new String[] {
            line2.substring(68) ,
            Double.toString(Math.round(decimal))
        });
      }
      return true;
    }
    return false;
  }

  private static boolean isLine1OK(String line1) throws OrekitException {
    Matcher matcher = line1Pattern.matcher(line1);
    return matcher.matches();
  }

  private static boolean isLine2OK(String line2) throws OrekitException {
    Matcher matcher = line2Pattern.matcher(line2);
    return matcher.matches();
  }
  
  /** Patterns */
  private static String pn  = "[ 0-9]";
  private static String i5 = "[ 0-9]{5}";
  private static String i8 = "[ 0-9]{8}";
  private static String c  = "[ A-Z]{3}";

  private static String line1Nb = "1 ";
  private static String satNb1 = i5 + "U ";
  private static String ID = i5 + c + " ";
  private static String pepoch = i5 + "[.]" + i8 +" ";
  private static String ftdMM = "[ +-]" + "[.]" + i8+ " ";
  private static String stdMM = "[ +-]" + i5 + "[+-]" + pn + " ";
  private static String pbStar = "[ +-]" + i5 + "[+-]" + pn + " ";
  private static String eph = pn + " ";
  private static String eltN = pn + "{4}";
  private static String pchK = pn;

  private static final Pattern line1Pattern = Pattern.compile(line1Nb + satNb1 + ID + pepoch + ftdMM +
                                         stdMM + pbStar + eph + eltN + pchK);
 
  private static String p  = "[.]";
  private static String line2Nb = "2 ";
  private static String satNb2 = pn + "{5}" + " ";
  private static String inc = pn + "{3}" + p + pn + "{4}" + " ";
  private static String praan = pn + "{3}" + p + pn + "{4}" + " ";
  private static String pe = pn + "{7}" + " ";
  private static String arg = pn + "{3}" + p + pn + "{4}" + " ";
  private static String ma = pn + "{3}" + p +pn + "{4}" + " ";
  private static String revs= pn + "{2}" + p +  pn + "{13}";
  private static String chK = pn;
 
  private static final Pattern line2Pattern = Pattern.compile(line2Nb + satNb2 + inc + praan + pe +
                                         arg + ma + revs + chK);  
  
}
