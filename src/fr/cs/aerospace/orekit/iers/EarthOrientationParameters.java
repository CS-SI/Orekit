package fr.cs.aerospace.orekit.iers;

import fr.cs.aerospace.orekit.frames.PoleCorrection;

/** Container class for Earth Orientation Parameters provided by IERS.
 * <p>Instances of this class correspond to lines from either the
 * EOP C 04 yearly files or the bulletin B monthly files.</p>
 * @author Luc Maisonobe
 * @see IERSData
 * @see fr.cs.aerospace.orekit.frames.ITRF2000Frame
 */
public class EarthOrientationParameters implements Comparable {

  /** Entry date (modified julian day, 00h00 UTC scale). */
  public final int mjd;

  /** UT1-UTC (seconds). */
  public final double ut1MinusUtc;

  /** Pole correction. */
  public final PoleCorrection pole;

  /** Simple constructor.
   * @param mjd entry date
   * @param ut1MinusUtc UT1-UTC (seconds)
   * @param pole pole correction
   */
  public EarthOrientationParameters(int mjd,
                                    double ut1MinusUtc,
                                    PoleCorrection pole) {
    this.mjd         = mjd;
    this.ut1MinusUtc = ut1MinusUtc;
    this.pole        = pole;
  }

  /** Compare an entry with another one, according to date. */
  public int compareTo(Object entry) {
    return mjd - ((EarthOrientationParameters) entry).mjd;
  }

}
