package fr.cs.aerospace.orekit.frames;
import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.spaceroots.mantissa.geometry.Rotation;
import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.series.BodiesElements;
import fr.cs.aerospace.orekit.frames.series.Development;
import fr.cs.aerospace.orekit.iers.EarthOrientationParameters;
import fr.cs.aerospace.orekit.iers.IERSData;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.time.TTScale;
import fr.cs.aerospace.orekit.time.UTCScale;

/** International Terrestrial Reference Frame 2000.
 * <p>This frame is the current (as of 2006) reference realization of
 * the International Terrestrial Reference System produced by IERS.
 * It is described in <a>
 * href="http://www.iers.org/documents/publications/tn/tn32/tn32.pdf">
 * IERS conventions (2003)</a>. It replaces the Earth Centered Earth Fixed
 * frame which is the reference frame for GPS satellites.</p>
 * <p>This frame is used to define position on solid Earth. It rotates with
 * the Earth and includes the pole motion with respect to Earth crust as
 * provided by {@link IERSData IERS data}. Its pole axis is the IERS Reference
 * Pole (IRP).</p>
 * <p>This implementation follows the new non-rotating origin paradigm
 * mandated by IAU 2000 resolution B1.8. It is therefore based on
 * Celestial Ephemeris Origin (CEO-based) and Earth Rotating Angle. Depending
 * on user choice at construction, it is either consistent to the complete IAU
 * 2000A precession-nutation model with an accuracy level is 0.2 milliarcsecond
 * or consistent to the reduced IAU 2000B precession-nutation model with an
 * accuracy level is 1.0 milliarcsecond. The intermediate frames used are
 * not available in the public interface and the parent frame is directly the
 * J2000 frame.</p>
 * <p>Other implementations of the ITRF 2000 are possible by
 * ignoring the B1.8 resolution and using the classical paradigm which
 * is equinox-based and relies on a specifically tuned Greenwich Sidereal Time.
 * They are not yet available in the OREKIT library.</p>
 * @author Luc Maisonobe
 */
public class ITRF2000Frame extends Frame {

  /** Build an ITRF2000 frame.
   * <p>If the <code>useIAU2000B</code> boolean parameter is true (which is the
   * recommended value) the reduced IAU2000B precession-nutation model will be
   * used, otherwise the complete IAU2000A precession-nutation model will be used.
   * The IAU2000B is recommended for most applications since it is <strong>far
   * less</strong> computation intensive than the IAU2000A model and its accuracy
   * is only slightly degraded (1 milliarcsecond instead of 0.2 milliarcsecond).</p>
   * @param date the date.
   * @param useIAU2000B if true (recommended value), the IAU2000B model will be used
   * @exception OrekitException if the nutation model data embedded in the
   * library cannot be read
   * @see Frame
   */
  public ITRF2000Frame(AbsoluteDate date, boolean useIAU2000B)
    throws OrekitException {

    super(getJ2000(), null , "ITRF2000");

    this.useIAU2000B = useIAU2000B;

    // nutation models are in micro arcseconds
    Class c = getClass();
    String xModel = useIAU2000B ? xModel2000B : xModel2000A;
    xDevelopment =
      new Development(c.getResourceAsStream(xModel), radiansPerArcsecond * 1.0e-6, xModel);
    String yModel = useIAU2000B ? yModel2000B : yModel2000A;
    yDevelopment =
      new Development(c.getResourceAsStream(yModel), radiansPerArcsecond * 1.0e-6, yModel);
    String sxy2Model = useIAU2000B ? sxy2Model2000B : sxy2Model2000A;
    sxy2Development =
      new Development(c.getResourceAsStream(sxy2Model), radiansPerArcsecond * 1.0e-6, sxy2Model);

    // convert the mjd dates in the raw entries into AbsoluteDate instances
    eop = new TreeSet();
    TreeSet rawEntries = IERSData.getInstance().getEarthOrientationParameters();
    for (Iterator iterator = rawEntries.iterator(); iterator.hasNext();) {
      eop.add(new DatedEop((EarthOrientationParameters) iterator.next()));
    }

    // everything is in place, we can now synchronize the frame
    updateFrame(date);

  }

  /** Update the frame to the given date.
   * <p>The update considers the pole motion from IERS data.</p>
   * @param date new value of the date
   * @exception OrekitException if the nutation model data embedded in the
   * library cannot be read
   */
  protected void updateFrame(AbsoluteDate date)
    throws OrekitException {
    
    if (cachedDate == null||cachedDate!=date) {
      //    offset from J2000 epoch in julian centuries
      double tts = date.minus(AbsoluteDate.J2000Epoch);
      double ttc =  tts * julianCenturyPerSecond;
      
      // luni-solar and planetary elements
      BodiesElements elements = computeBodiesElements(ttc);

      // compute Earth Rotation Angle using Nicole Capitaine model (2000)
      double dtu1 = getUT1MinusUTC(date);
      double taiMinusTt  = TTScale.getInstance().offsetToTAI(tts + j2000MinusJava);
      double utcMinusTai = UTCScale.getInstance().offsetFromTAI(tts + taiMinusTt + j2000MinusJava);
      double tu = (tts + taiMinusTt + utcMinusTai + dtu1) / 86400 ;
      era  = era0 + era1A * tu + era1B * tu;
      era -= twoPi * Math.floor((era + Math.PI) / twoPi);

      // get the current IERS pole correction parameters
      PoleCorrection iCorr = getPoleCorrection(date);

      // compute the additional terms not included in IERS data
      PoleCorrection tCorr = tidalCorrection(date);
      PoleCorrection nCorr = nutationCorrection(date);

      // elementary rotations due to pole motion in terrestrial frame
      Rotation r1 = new Rotation(Vector3D.plusI, -(iCorr.yp + tCorr.yp + nCorr.yp));
      Rotation r2 = new Rotation(Vector3D.plusJ, -(iCorr.xp + tCorr.xp + nCorr.xp));
      Rotation r3 = new Rotation(Vector3D.plusK, sPrimeRate * ttc);

      // complete pole motion in terrestrial frame
      Rotation wRot = r3.applyTo(r2.applyTo(r1));

      // simple rotation around the Celestial Intermediate Pole
      Rotation rRot = new Rotation(Vector3D.plusK, era);

      // precession and nutation effect (pole motion in celestial frame)
      Rotation qRot = precessionNutationEffect(ttc, elements);

      // combined effects
      Rotation combined = qRot.applyTo(rRot.applyTo(wRot)).revert();
      
      // set up the transform from parent GCRS (J2000) to ITRF
      Vector3D rotationRate = new Vector3D((era1A + era1B) / -86400, rRot.getAxis());
      updateTransform(new Transform(combined , rotationRate));      
      cachedDate = date;
    }
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

  /** Get the UT1-UTC value.
   * <p>The data provided comes from the EOP C 04 files. It
   * is smoothed data.</p>
   * @param date date at which the value is desired
   * @return UT1-UTC in seconds
   */
  private double getUT1MinusUTC(AbsoluteDate date) {
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
  private PoleCorrection getPoleCorrection(AbsoluteDate date) {
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

  /** Get the Earth Rotation Angle at the current date.
   * @param  date the date
   * @return Earth Rotation Angle at the current date in radians
   * @throws OrekitException 
   */
  public double getEarthRotationAngle(AbsoluteDate date) throws OrekitException {
    updateFrame(date);
    return era;
  }

  /** Compute tidal correction to the pole motion.
   * @param date current date
   * @return tidal correction
   */
  private PoleCorrection tidalCorrection(AbsoluteDate date) {
    // TODO compute tidal correction to pole motion
   return PoleCorrection.NULL_CORRECTION;
  }

  /** Compute nutation correction due to tidal gravity.
   * @param date current date
   * @return nutation correction
   */
  private PoleCorrection nutationCorrection(AbsoluteDate date) {
    // this factor seems to be of order of magnitude a few tens of
    // micro arcseconds. It is computed from the classical approach
    // (not the new one used here) and hence requires computation
    // of GST, IAU2000A nutation, equations of equinoxe ...
    // For now, this term is ignored
    return PoleCorrection.NULL_CORRECTION;
  }

  /** Compute precession and nutation effects.
   * @param t offset from J2000.0 epoch in julian centuries
   * @param elements luni-solar and planetary elements for the current date
   * @return precession and nutation rotation
   */
  public Rotation precessionNutationEffect(double t, BodiesElements elements) {

    // pole position
    double x =    xDevelopment.value(t, elements);
    double y =    yDevelopment.value(t, elements);
    double s = sxy2Development.value(t, elements) - x * y / 2;

    double x2 = x * x;
    double y2 = y * y;
    double r2 = x2 + y2;
    double e = Math.atan2(y, x);
    double d = Math.acos(Math.sqrt(1 - r2));
    Rotation rpS = new Rotation(Vector3D.plusK, -s);
    Rotation rpE = new Rotation(Vector3D.plusK, -e);
    Rotation rmD = new Rotation(Vector3D.plusJ, +d);

    // combine the 4 rotations (rpE is used twice)
    // IERS conventions (2003), section 5.3, equation 6
    return rpE.applyInverseTo(rmD.applyTo(rpE.applyTo(rpS)));

  }

  /** Compute the nutation elements.
   * @param tt offset from J2000.0 epoch in julian centuries
   * @return luni-solar and planetary elements
   */
  private BodiesElements computeBodiesElements(double tt) {
    return useIAU2000B
      ? new BodiesElements(f11 * tt + f10, // mean anomaly of the Moon
                           f21 * tt + f20, // mean anomaly of the Sun
                           f31 * tt + f30, // L - &Omega; where L is the mean longitude of the Moon
                           f41 * tt + f40, // mean elongation of the Moon from the Sun
                           f51 * tt + f50, // mean longitude of the ascending node of the Moon
                           Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                           Double.NaN, Double.NaN, Double.NaN, Double.NaN)
      : new BodiesElements((((f14 * tt + f13) * tt + f12) * tt + f11) * tt + f10, // mean anomaly of the Moon
                           (((f24 * tt + f23) * tt + f22) * tt + f21) * tt + f20, // mean anomaly of the Sun
                           (((f34 * tt + f33) * tt + f32) * tt + f31) * tt + f30, // L - &Omega; where L is the mean longitude of the Moon
                           (((f44 * tt + f43) * tt + f42) * tt + f41) * tt + f40, // mean elongation of the Moon from the Sun
                           (((f54 * tt + f53) * tt + f52) * tt + f51) * tt + f50, // mean longitude of the ascending node of the Moon
                           f61  * tt +  f60, // mean Mercury longitude
                           f71  * tt +  f70, // mean Venus longitude
                           f81  * tt +  f80, // mean Earth longitude
                           f91  * tt +  f90, // mean Mars longitude
                           f101 * tt + f100, // mean Jupiter longitude
                           f111 * tt + f110, // mean Saturn longitude
                           f121 * tt + f120, // mean Uranus longitude
                           f131 * tt + f130, // mean Neptune longitude
                           (f142 * tt + f141) * tt); // general accumulated precession in longitude
  }

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

  /** Earth Rotation Angle, in radians. */
  private double era;

  /** Previous EOP entry. */
  private DatedEop previous;

  /** Next EOP entry. */
  private DatedEop next;

  /** Indicator for complete or reduced precession-nutation model. */
  private boolean useIAU2000B;

  /** Pole position (X). */
  private Development xDevelopment = null;

  /** Pole position (Y). */
  private Development yDevelopment = null;

  /** Pole position (S + XY/2). */
  private Development sxy2Development = null;

  /** Earth Orientation Parameters. */
  private TreeSet eop = null;
  
  /** Cached date to avoid useless calculus */
  private AbsoluteDate cachedDate;

  /** 2&pi;. */
  private static final double twoPi = 2.0 * Math.PI;

  /** Radians per arcsecond. */
  private static final double radiansPerArcsecond = twoPi / 1296000;

  /** Julian century per second. */
  private static final double julianCenturyPerSecond = 1.0 / (36525.0 * 86400.0);

  /** Offset between J2000.0 epoch and Java epoch in seconds. */
  private static final double j2000MinusJava =
    AbsoluteDate.J2000Epoch.minus(AbsoluteDate.JavaEpoch);

  /** Constant term of Capitaine's Earth Rotation Angle model. */
  private static final double era0 = twoPi * 0.7790572732640;

  /** Rate term of Capitaine's Earth Rotation Angle model.
   * (radians per day, main part) */
  private static final double era1A = twoPi;

  /** Rate term of Capitaine's Earth Rotation Angle model.
   * (radians per day, fractional part) */
  private static final double era1B = era1A * 0.00273781191135448;

  /** S' rate in radians per julian century.
   * Approximately -47 microarcsecond per julian century (Lambert and Bizouard, 2002)
   */
  private static final double sPrimeRate = -47e-6 * radiansPerArcsecond ;

  // lunisolar nutation elements
  private static final double f10 = Math.toRadians(134.96340251);
  private static final double f11 = 1717915923.217800  * radiansPerArcsecond;
  private static final double f12 =         31.879200  * radiansPerArcsecond;
  private static final double f13 =          0.051635  * radiansPerArcsecond;
  private static final double f14 =         -0.0002447 * radiansPerArcsecond;

  private static final double f20 = Math.toRadians(357.52910918);
  private static final double f21 = 129596581.048100   * radiansPerArcsecond;
  private static final double f22 =        -0.553200   * radiansPerArcsecond;
  private static final double f23 =         0.000136   * radiansPerArcsecond;
  private static final double f24 =        -0.00001149 * radiansPerArcsecond;

  private static final double f30 = Math.toRadians(93.27209062);
  private static final double f31 = 1739527262.847800   * radiansPerArcsecond;
  private static final double f32 =        -12.751200   * radiansPerArcsecond;
  private static final double f33 =         -0.001037   * radiansPerArcsecond;
  private static final double f34 =          0.00000417 * radiansPerArcsecond;

  private static final double f40 = Math.toRadians(297.85019547);
  private static final double f41 = 1602961601.209000   * radiansPerArcsecond;
  private static final double f42 =         -6.370600   * radiansPerArcsecond;
  private static final double f43 =          0.006593   * radiansPerArcsecond;
  private static final double f44 =         -0.00003169 * radiansPerArcsecond;

  private static final double f50 = Math.toRadians(125.04455501);
  private static final double f51 = -6962890.543100   * radiansPerArcsecond;
  private static final double f52 =        7.472200   * radiansPerArcsecond;
  private static final double f53 =        0.007702   * radiansPerArcsecond;
  private static final double f54 =       -0.00005939 * radiansPerArcsecond;

  // planetary nutation elements
  private static final double f60 = 4.402608842;
  private static final double f61 = 2608.7903141574;

  private static final double f70 = 3.176146697;
  private static final double f71 = 1021.3285546211;

  private static final double f80 = 1.753470314;
  private static final double f81 = 628.3075849991;

  private static final double f90 = 6.203480913;
  private static final double f91 = 334.0612426700;

  private static final double f100 = 0.599546497;
  private static final double f101 = 52.9690962641;

  private static final double f110 = 0.874016757;
  private static final double f111 = 21.3299104960;

  private static final double f120 = 5.481293872;
  private static final double f121 = 7.4781598567;

  private static final double f130 = 5.311886287;
  private static final double f131 = 3.8133035638;

  private static final double f141 = 0.024381750;
  private static final double f142 = 0.00000538691;

  /** Resources for IERS table 5.2a from IERS conventions (2003). */
  private static final String xModel2000A    = "/fr/cs/aerospace/orekit/resources/tab5.2a.txt";
  private static final String xModel2000B    = "/fr/cs/aerospace/orekit/resources/tab5.2a.reduced.txt";

  /** Resources for IERS table 5.2b from IERS conventions (2003). */
  private static final String yModel2000A    = "/fr/cs/aerospace/orekit/resources/tab5.2b.txt";
  private static final String yModel2000B    = "/fr/cs/aerospace/orekit/resources/tab5.2b.reduced.txt";

  /** Resources for IERS table 5.2c from IERS conventions (2003). */
  private static final String sxy2Model2000A = "/fr/cs/aerospace/orekit/resources/tab5.2c.txt";
  private static final String sxy2Model2000B = "/fr/cs/aerospace/orekit/resources/tab5.2c.reduced.txt";

  private static final long serialVersionUID = -2361037148063033307L;
}
