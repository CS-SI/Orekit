package fr.cs.aerospace.orekit.tle;

import fr.cs.aerospace.orekit.errors.OrekitException;

class SGP4Extrapolator extends SXP4Extrapolator {
  
  protected SGP4Extrapolator(TLE initialTLE) throws OrekitException {
    super (initialTLE);
  }

  public void sxpInitialize() {
    
    // For perigee less than 220 kilometers, the equations are truncated to 
    // linear variation in sqrt a and quadratic variation in mean anomaly. 
    // Also, the c3 term, the delta omega term, and the delta m term are dropped.      
    lessThan220 = (perige < 220);
    if(!lessThan220) {
      double c1sq = c1 * c1;
      delM0 = 1. + eta * Math.cos(tle.getMeanAnomaly());
      delM0 *= delM0 * delM0;
      d2 = 4 * a0dp * tsi * c1sq;
      double temp = d2 * tsi * c1/3.0;
      d3 = (17 * a0dp + s4) * temp;
      d4 = 0.5*temp * a0dp * tsi * (221 * a0dp + 31*s4) * c1;
      t3cof = d2 + 2*c1sq;
      t4cof = 0.25*(3 * d3 + c1 * (12 * d2 + 10 * c1sq));
      t5cof = 0.2*(3 * d4 + 12 * c1 * d3 + 6 * d2 * d2 + 15 * c1sq *(2 * d2 + c1sq));
      sinM0 = Math.sin(tle.getMeanAnomaly());
      if( tle.getE() < Constants.MINIMAL_E) {
        omgcof = 0.;
        xmcof = 0.;
      }        
      else  {
        double c3 = coef * tsi * Constants.a3ovk2 * xn0dp * Constants.ae * sini0 / tle.getE();
        xmcof = -Constants.twoThirds * coef * tle.getBStar() * Constants.ae / eeta;
        omgcof = tle.getBStar() * c3 * Math.cos(tle.getPerigeeArgument());
      }
    } 
    
    c5 = 2 * coef1 * a0dp * beta02 * (1 + 2.75*(etasq + eeta) + eeta * etasq);
    // initialized
  }
  
  public void sxpExtrapolate(double tSince) {
        
    // Update for secular gravity and atmospheric drag. 
    double xmdf = tle.getMeanAnomaly() + xmdot * tSince;
    double omgadf = tle.getPerigeeArgument() + omgdot * tSince;
    double xn0ddf = tle.getRaan() + xnodot * tSince;
    omega = omgadf;
    double xmp = xmdf;
    double tsq = tSince * tSince;
    xnode = xn0ddf + xnodcf * tsq;
    double tempa = 1-c1 * tSince;
    double tempe = tle.getBStar() * c4 * tSince;
    double templ = t2cof * tsq;
    
    if(!lessThan220) {
      double delomg = omgcof * tSince;
      double delm = 1. + eta * Math.cos(xmdf);

      delm = xmcof * (delm * delm * delm - delM0);
      double temp = delomg + delm;
      xmp = xmdf + temp;
      omega = omgadf-temp;
      double tcube = tsq * tSince;
      double tfour = tSince * tcube;
      tempa = tempa - d2 * tsq - d3 * tcube - d4 * tfour;
      tempe = tempe + tle.getBStar() * c5 * (Math.sin(xmp) - sinM0);
      templ = templ + t3cof * tcube + tfour * (t4cof + tSince * t5cof);
    }

    a = a0dp * tempa * tempa;
    e = tle.getE() - tempe;

    // A highly arbitrary lower limit on e,  of 1e-6:  TODO why ?
    if( e < 1e-6) {
      e = 1e-6;
    }
    
    xl = xmp + omega + xnode + xn0dp*templ;
    
    i = tle.getI();
//    if( tempa < 0.)       /* force negative a,  to indicate error condition */
//      a = -a;   TODO what's the problem with negative tempa ??

//   sxpx_posn_vel( xnode, a, e, params, cosi0, sini0, i, omega, xl);
  }

  /** If perige is less than 220 km, some calculus are avoided. */
  private boolean lessThan220;

  /** intermediate parameters. */
  
  private double delM0; // (1 + eta*cos(M0))^3
  private double d2, d3, d4; // internal coeffs
  private double t3cof, t4cof, t5cof; // internal coefs
  private double sinM0; // sin(M0)
  private double omgcof; // coef for omega computation
  private double xmcof; // coef for M computation
  private double c5; // C5 from SPCTRCK#3
  
}
