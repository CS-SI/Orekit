package fr.cs.aerospace.orekit.frames;

import org.spaceroots.mantissa.geometry.Rotation;
import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.IERSData;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.series.BodiesElements;
import fr.cs.aerospace.orekit.frames.series.Development;
import fr.cs.aerospace.orekit.time.AbsoluteDate;

/** International Terrestrial Reference Frame 2000.
 * <p>This frame is the current (as of 2006) reference realization of
 * the International Terrestrial Reference System produced by IERS.
 * It is described in <a
 * href="http://www.iers.org/documents/publications/tn/tn32/tn32.pdf">
 * IERS conventions (2003)</a>. It replaces the Earth Centered Earth Fixed
 * frame which is the reference frame for GPS satellites.</p>
 * <p>This frame is used to define position on solid Earth. It rotates with
 * the Earth and includes the pole motion with respect to Earth crust as
 * provided by {@link IERSData IERS data}. Its pole axis is the IERS Reference
 * Pole (IRP).</p>
 * <p>This implementation follows the new non-rotating origin paradigm
 * mandated by IAU 2000 resolution B1.8. It is therefore based on
 * Celestial Ephemeris Origin (CEO-based) and Earth Rotating Angle. It is
 * consistent to the complete IAU 2000A precession-nutation model and its
 * accuracy level is 0.2 milliarcsecond. The intermediate frames used are
 * not available in the public interface and the parent frame is directly the
 * J2000 frame.</p>
 * <p>Other implementations of the ITRF 2000 are possible by
 * ignoring the B1.8 resolution and using the cmlassical paradigm which
 * is equinox-based and rely on a specifically tuned Greenwich Sidereal Time
 * to achieve the same accuracy if the IAU 2000A precession-nutation model
 * is used, or a 1 milliarcsecond accuracy if the simplified IAU 2000B
 * precession-nutation model is used. They are not yet available
 * in the OREKIT library yet.</p>
 * @author Luc Maisonobe
 */
public class ITRF2000Frame extends DateDependantFrame {

  /** Build an ITRF2000 frame.
   * @param date current date (a <em>new</em> private instance
   * will be built from this date value to ensure proper synchronization,
   * between the instance and all other {@link DateDependantFrame date
   * dependant frames}. Further changes in the parameter instance will
   * <em>not</em> automatically update the frames.
   * @param iers iers data provider
   * @exception OrekitException if the nutation model data embedded in the
   * library cannot be read
   * @see #getDate()
   */
  public ITRF2000Frame(AbsoluteDate date, IERSData iers)
    throws OrekitException {

    super (getJ2000(), date);
    this.iers = iers;

    // read and build the file-based models only once ...
    if ((xDevelopment == null)
        || (yDevelopment == null)
        || (sxy2Development == null)) {
      Class c = getClass();

      // nutation models are in micro arcseconds
      xDevelopment =
        new Development(c.getResourceAsStream(xModel), radiansPerArcsecond * 1.0e-6, xModel);
      yDevelopment =
        new Development(c.getResourceAsStream(yModel), radiansPerArcsecond * 1.0e-6, yModel);
      sxy2Development =
        new Development(c.getResourceAsStream(sxy2Model), radiansPerArcsecond * 1.0e-6, sxy2Model);

    }

  }

  /** Update the frame to the given (shared) date.
   * <p>The update considers the pole motion from IERS data.</p>
   * @param date new value of the shared date
   */
  protected void updateFrame(AbsoluteDate date) {

    // offset from J2000 epoch
    double t = date.minus(AbsoluteDate.J2000Epoch) * julianCenturyPerSecond;

    // luni-solar and planetary elements
    BodiesElements elements = computeBodiesElements(t);

    // compute Earth Rotation Angle using Nicole Capitaine model (2000)
    double dtu1 = iers.getUT1MinusUTC(date);
    double tu   = t * 36525 + dtu1 / 86400;
    era = era0 + era1A * tu + era1B * tu;

    // get the current IERS pole correction parameters
    PoleCorrection iCorr = iers.getPoleCorrection(date);

    // compute the additional terms not included in IERS data
    PoleCorrection tCorr = tidalCorrection(date);
    PoleCorrection nCorr = nutationCorrection(date);

    // elementary rotations due to pole motion in terrestrial frame
    Rotation r1 = new Rotation(Vector3D.plusI, iCorr.yp + tCorr.yp + nCorr.yp);
    Rotation r2 = new Rotation(Vector3D.plusJ, iCorr.xp + tCorr.xp + nCorr.xp);
    Rotation r3 = new Rotation(Vector3D.plusK, -sPrimeRate * t);

    // complete pole motion in terrestrial frame
    Rotation wRot = r3.applyTo(r2.applyTo(r1));

    // simple rotation around the Celestial Intermediate Pole
    Rotation rRot = new Rotation(Vector3D.plusK, -era);

    // precession and nutation effect (pole motion in celestial frame)
    Rotation qRot = precessionNutationEffect(t, elements);

    // combined effects
    Rotation combined = qRot.applyTo(rRot.applyTo(wRot)).revert();

    // set up the transform from parent GCRS (J2000) to ITRF
    updateTransform(new Transform(combined));

  }

  /** Get the Earth Rotation Angle at the current date.
   * @return Earth Rotation Angle at the current date in radians
   */
  public double getEarthRotationAngle() {
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

    // compute harmonic functions for half the E and d angles
    // we try to use numerically stable expressions
    double x2 = x * x;
    double y2 = y * y;
    double r2 = x2 + y2;
    double r  = Math.sqrt(r2);
    double tanHalfE = (Math.abs(x) > Math.abs(y)) ? (r - x) / y : y / (r + x);
    double cosHalfE = 1 / Math.sqrt(1 + tanHalfE * tanHalfE);
    double sinHalfE = tanHalfE * cosHalfE;
    double tanHalfD = r / (1 + Math.sqrt(1 - r2));
    double cosHalfD = 1 / Math.sqrt(1 + tanHalfD * tanHalfD);
    double sinHalfD = tanHalfD * cosHalfD;

    // elementary rotations
    Rotation rpS = new Rotation(Math.cos(s/2), 0, 0, -Math.sin(s/2), false);
    Rotation rpE = new Rotation(cosHalfE, 0, 0, -sinHalfE, false);
    Rotation rmD = new Rotation(cosHalfD, 0, sinHalfD, 0, false);

    // combine the 4 rotations (rpE is used twice)
    // IERS conventions (2003), section 5.3, equation 6
    return rpE.applyInverseTo(rmD.applyTo(rpE.applyTo(rpS)));

  }

  /** Compute the nutation elements.
   * @param t offset from J2000.0 epoch in julian centuries
   * @return luni-solar and planetary elements
   */
  private BodiesElements computeBodiesElements(double t) {
    return new BodiesElements((((f14 * t + f13) * t + f12) * t + f11) * t + f10, // mean anomaly of the Moon
                              (((f24 * t + f23) * t + f22) * t + f21) * t + f20, // mean anomaly of the Sun
                              (((f34 * t + f33) * t + f32) * t + f31) * t + f30, // L - &Omega; where L is the mean longitude of the Moon
                              (((f44 * t + f43) * t + f42) * t + f41) * t + f40, // mean elongation of the Moon from the Sun
                              (((f54 * t + f53) * t + f52) * t + f51) * t + f50, // mean longitude of the ascending node of the Moon
                              f61  * t +  f60, // mean Mercury longitude
                              f71  * t +  f70, // mean Venus longitude
                              f81  * t +  f80, // mean Earth longitude
                              f91  * t +  f90, // mean Mars longitude
                              f101 * t + f100, // mean Jupiter longitude
                              f111 * t + f110, // mean Saturn longitude
                              f121 * t + f120, // mean Uranus longitude
                              f131 * t + f130, // mean Neptune longitude
                              (f142 * t + f141) * t); // general accumulated precession in longitude
  }

  /** IERS data provider. */
  private IERSData iers;

  /** Earth Rotation Angle, in radians. */
  private double era;

  /** Pole position (X). */
  private static Development xDevelopment = null;

  /** Pole position (Y). */
  private static Development yDevelopment = null;

  /** Pole position (S + XY/2). */
  private static Development sxy2Development = null;

  /** 2&pi;. */
  private static final double twoPi = 2.0 * Math.PI;

  /** Radians per arcsecond. */
  private static final double radiansPerArcsecond = twoPi / 1296000;

  /** Julian century per second. */
  private static final double julianCenturyPerSecond = 1.0 / (36525 * 86400);

  /** Constant term of Capitaine's Earth Rotation Angle model. */
  private static final double era0 = twoPi * 0.7790572732640;

  /** Rate term of Capitaine's Earth Rotation Angle model.
   * (radians per day, main part) */
  private static final double era1A = twoPi * 36525;

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
  private static final String xModel    = "/fr/cs/aerospace/orekit/resources/tab5.2a.txt";

  /** Resources for IERS table 5.2b from IERS conventions (2003). */
  private static final String yModel    = "/fr/cs/aerospace/orekit/resources/tab5.2b.txt";

  /** Resources for IERS table 5.2c from IERS conventions (2003). */
  private static final String sxy2Model = "/fr/cs/aerospace/orekit/resources/tab5.2c.txt";

}
