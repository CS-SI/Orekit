package fr.cs.aerospace.orekit.frames;

import org.spaceroots.mantissa.geometry.Rotation;
import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.IERSData;
import fr.cs.aerospace.orekit.time.AbsoluteDate;

/** International Terrestrial Reference System.
 * <p>This frame is the IERS terrestrial reference frame defined in the IERS
 * conventions 2003. It replaces the Earth Centered Earth Fixed frame which is
 * the reference frame for GPS satellites.</p>
 * <p>This frame is used to define position on solid Earth. It rotates with
 * the Earth and includes the pole motion with respect to Earth crust as
 * provided by {@link IERSData IERS data}. Its pole axis is the IERS Reference
 * Pole (IRP).</p>
 * @author Luc Maisonobe
 */
public class ITRSFrame extends DateDependantFrame {

  /** Build an ITRS frame.
   * @param date current date (a <em>new</em> private instance
   * will be built from this date value to ensure proper sharing,
   * further changes in the parameter instance will <em>not</em>
   * automatically update the frames)
   * @param iers iers data provider
   * @see #getDate()
   */
  public ITRSFrame(AbsoluteDate date, IERSData iers) {
    super (getJ2000(), date);
    this.iers = iers;
  }

  /** Update the frame to the given (shared) date.
   * <p>The update considers the pole motion from IERS data.</p>
   * @param date new value of the shared date
   */
  protected void updateFrame(AbsoluteDate date) {

    // offset from J2000 epoch
    double dt = date.minus(AbsoluteDate.J2000Epoch);

    // get the current IERS pole correction parameters
    PoleCorrection iCorr = iers.getPoleCorrection(date);

    // compute the additional terms not included in IERS data
    PoleCorrection tCorr = tidalCorrection(date);
    PoleCorrection nCorr = nutationCorrection(date);

    // elementary rotations due to pole motion in terrestrial frame
    // s' is approximately -47e-6 arcsecond per julian century
    // (Lambert and Bizouard, 2002)
    Rotation r1 = new Rotation(Vector3D.plusI, iCorr.yp + tCorr.yp + nCorr.yp);
    Rotation r2 = new Rotation(Vector3D.plusJ, iCorr.xp + tCorr.xp + nCorr.xp);
    Rotation r3 = new Rotation(Vector3D.plusK, 7.2205247e-20 * dt);

    // complete pole motion in terrestrial frame
    Rotation wRot = r3.applyTo(r2.applyTo(r1));

    // Earth Rotation Angle computed from IERS conventions (2003),
    // Nicole Capitaine model (2000)
    // we DON'T use (twoPi + era1) * tu for the sake of accuracy
    double dtu1 = iers.getUT1MinusUTC(date);
    double tu   = 86400 * (dt + dtu1);
    double era  = era0 + twoPi * tu + era1 * tu;

    // simple rotation around the Celestial Intermediate Pole
    Rotation eRot  = new Rotation(Vector3D.plusK, era);

    // elementary rotations due to pole motion in celestial frame
    Rotation pRot  = precessionEffect(date);
    Rotation nRot  = nutationEffect(date);

    // combined effects
    Rotation combined = pRot.applyTo(nRot.applyTo(eRot.applyTo(wRot)));

    // set up the transform from ITRS to parent GCRS (J2000)
    updateTransform(new Transform(combined));

  }

  /** Compute tidal correction to the pole motion.
   * @param date current date
   * @return tidal correction
   */
  private PoleCorrection tidalCorrection(AbsoluteDate date) {
    // TODO compute tidal correction to pole motion
    return new PoleCorrection(0, 0);
  }

  /** Compute nutation correction to the pole motion.
   * @param date current date
   * @return nutation correction
   */
  private PoleCorrection nutationCorrection(AbsoluteDate date) {
    // TODO compute nutation correction to pole motion
    return new PoleCorrection(0, 0);
  }

  /** Compute pole motion due to precession.
   * @param date current date
   * @return pole motion due to precession
   */
  private Rotation precessionEffect(AbsoluteDate date) {
    // TODO compute precession effect
    return new Rotation();
  }

  /** Compute pole motion due to nutation.
   * @param date current date
   * @return pole motion due to nutation
   */
  private Rotation nutationEffect(AbsoluteDate date) {
    // TODO compute nutation effect
    return new Rotation();
  }

  /** IERS data provider. */
  private IERSData iers;

  /** 2 &pi;. */
  private static double twoPi = 2.0 * Math.PI;

  /** Constant term of Capitaine's Earth Rotation Angle model. */
  private static double era0 = twoPi * 0.7790572732640;

  /** Rate term of Capitaine's Earth Rotation Angle model. */
  private static double era1 = twoPi * 0.00273781191135448;

}
