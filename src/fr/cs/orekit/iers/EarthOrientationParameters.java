package fr.cs.orekit.iers;

import java.io.Serializable;
import java.util.Date;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.PoleCorrection;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.time.UTCScale;

/** Container class for Earth Orientation Parameters provided by IERS.
 * <p>Instances of this class correspond to lines from either the
 * EOP C 04 yearly files or the bulletin B monthly files.</p>
 * @author Luc Maisonobe
 * @see EOPC04FilesLoader
 * @see BulletinBFilesLoader
 * @see fr.cs.orekit.frames.Frame
 */
public class EarthOrientationParameters
  implements Serializable {

  /** Simple constructor.
   * @param mjd entry date
   * @param ut1MinusUtc UT1-UTC (seconds)
   * @param pole pole correction
   * @exception OrekitException if the UTC scale cannot be initialized
   */
  public EarthOrientationParameters(int mjd, double ut1MinusUtc,
                                    PoleCorrection pole)
    throws OrekitException {

    this.mjd         = mjd;
    this.ut1MinusUtc = ut1MinusUtc;
    this.pole        = pole;

    // convert mjd date at 00h00 UTC to absolute date
    long javaTime = (mjd - 40587) * 86400000l;
    this.date     = new AbsoluteDate(new Date(javaTime), UTCScale.getInstance());

  }

  /** Entry date (modified julian day, 00h00 UTC scale). */
  public final int mjd;

  /** UT1-UTC (seconds). */
  public final double ut1MinusUtc;

  /** Pole correction. */
  public final PoleCorrection pole;

  /** Entry date (absolute date). */
  public final AbsoluteDate date;

  private static final long serialVersionUID = 3827689974193144490L;

}
