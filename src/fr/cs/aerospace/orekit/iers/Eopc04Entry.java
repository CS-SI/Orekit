package fr.cs.aerospace.orekit.iers;

import fr.cs.aerospace.orekit.frames.PoleCorrection;

/** Container class for EOP C 04 entries.
 * @author Luc Maisonobe
 * @see IERSData
 */
public class Eopc04Entry implements Comparable {

  /** Entry date (modified julian day, 00h00 UTC scale). */
  public final int mjd;

  /** UT1-UTC (seconds). */
  public final double ut1MinusUtc;

  /** Pole correction. */
  public final PoleCorrection pole;

  /** Simple constructor.
   * @param mjd entry date
   * @param UT1-UTC (seconds)
   * @param pole pole correction
   */
  public Eopc04Entry(int mjd, double ut1MinusUtc, PoleCorrection pole) {
    this.mjd         = mjd;
    this.ut1MinusUtc = ut1MinusUtc;
    this.pole        = pole;
  }

  /** Compare an entry with another one, according to date. */
  public int compareTo(Object entry) {
    return mjd - ((Eopc04Entry) entry).mjd;
  }

}
