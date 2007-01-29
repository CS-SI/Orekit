package fr.cs.aerospace.orekit.tle;

import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.time.AbsoluteDate;

abstract class SDP4Extrapolator  extends TLEPropagator {

  protected SDP4Extrapolator(TLE initialTLE) throws OrekitException {
    super (initialTLE);
  }

  protected void sxpInitialize() throws OrekitException {
    luniSolarTermsComputation();
  }  // End of initialization

  protected void sxpExtrapolate(double tSince) throws OrekitException {

    // Update for secular gravity and atmospheric drag
    omgadf = tle.getPerigeeArgument() + omgdot * tSince;
    double xnoddf = tle.getRaan() + xnodot * tSince;
    double tSinceSq = tSince * tSince;
    xnode = xnoddf + xnodcf * tSinceSq;
    xn = xn0dp;

    // Update for deep-space secular effects
    xll = tle.getMeanAnomaly() + xmdot * tSince;

    deepSecularEffects(tSince);

    double tempa = 1 - c1 * tSince;    
    a = Math.pow(Constants.xke/xn, Constants.twoThirds)*tempa*tempa;
    em -= tle.getBStar()*c4*tSince;
    
    // Update for deep-space periodic effects
    xll += xn0dp * t2cof * tSinceSq;

    deepPeriodicEffects(tSince);

    xl = xll + omgadf + xnode;
    
    // Dundee change:  Reset cosio,  sinio for new xinc:
    cosi0 = Math.cos(xinc);
    sini0 = Math.sin(xinc);
    e = em;
    i = xinc;
    omega = omgadf;
    // end of calculus, go for PV computation
  }

  /** Computes NORAD compliant earth rotation angle.
   * @param date the current date
   * @return the ERA (rad)
   */
  public static double thetaG(AbsoluteDate date) {

    // Reference:  The 1992 Astronomical Almanac, page B6.
    double omega_E = 1.00273790934;
    double jd = date.minus(AbsoluteDate.JulianEpoch)/86400;

    // Earth rotations per sidereal day (non-constant)

    double UT = (jd + .5)%1;
    double seconds_per_day = 86400.;
    double jd_2000 = 2451545.0;   /* 1.5 Jan 2000 = JD 2451545. */
    double t_cen, GMST, rval;

    t_cen = (jd - UT - jd_2000) / 36525.;
    GMST = 24110.54841 + t_cen * (8640184.812866 + t_cen *
        (0.093104 - t_cen * 6.2E-6));
    GMST = (GMST + seconds_per_day * omega_E * UT)%seconds_per_day;
    if( GMST < 0.) GMST += seconds_per_day;
    rval = 2 * Math.PI * GMST / seconds_per_day;

    return( rval);
  }
  
  protected abstract void deepPeriodicEffects(double t);
  
  protected abstract void deepSecularEffects(double t);
  
  protected abstract void luniSolarTermsComputation() throws OrekitException;
      
  /** Params to determine for PV computation. */
  protected double omgadf; // new perigee argument
  protected double xn; // new mean motion
  protected double xll; // parameter for xl computation
  protected double em; // new eccentricity
  protected double xinc; // new inclination
  
}
