package fr.cs.aerospace.orekit.tle;

import org.spaceroots.mantissa.geometry.Vector3D;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.errors.Translator;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;

/** This class provides elements to propagate TLE's.
 * 
 * The models used are SGP4 and SDP4, initialy proposed by NORAD as the unique convenient
 * propagator for TLE's. The code is largely inspired from the (...)
 *
 * @author SPACETRACK Report #3 project. Felix R. Hoots, Ronald L. Roehrich, December 1980 (original fortran)
 * @author Revisiting Spacetrack Report #3. David A. Vallado, Paul Crawford, Richard Hujsak, T.S. Kelso (C++ translation and improvements)
 * @author Fabien Maussion (Java translation)
 */
public abstract class TLEPropagator {

  /** Protected constructor for herited classes. 
   * @param initialTLE the unique TLE to propagate
   */
  protected TLEPropagator(TLE initialTLE) {
    tle = initialTLE;
    initializeCommons();
    sxpInitialize();
  }
  
  /** Selects the extrapolator to use with the selected TLE.
   * @param tle the TLE to propagate.
   * @return the correct propagator.
   * @throws OrekitException 
   */
  public static TLEPropagator selectExtrapolator(TLE tle) throws OrekitException {

    double a1 = Math.pow( TLEConstants.xke / (tle.getMeanMotion()*60.0), TLEConstants.twoThirds);
    double cosi0 = Math.cos(tle.getI());
    double temp = TLEConstants.ck2 * 1.5 * (3*cosi0*cosi0 - 1.0) * Math.pow(1.0 - tle.getE()*tle.getE(),-1.5);
    double delta1 = temp/(a1*a1);
    double a0 = a1 * (1.0 - delta1 * (TLEConstants.oneThird + delta1 * (delta1 * 134./81.+1.0)));
    double delta0 = temp/(a0*a0);

    // recover original mean motion :
    double xn0dp = tle.getMeanMotion()*60.0/(delta0 + 1.0);

    // Period >= 225 minutes is deep space 
    if (2*Math.PI / (xn0dp*TLEConstants.minutesPerDay) >= (1. / 6.4)) {
      return new DeepSDP4(tle);
    }
    else {
      return new SGP4(tle);
    }
  }

  /** Get the extrapolated position and velocity from an initial TLE.
   * @param date the final date
   * @return the final PVCoordinates
   * @throws OrekitException
   */
  public PVCoordinates getPVCoordinates(AbsoluteDate date)
    throws OrekitException {  
    
    double tSince = date.minus(tle.getEpoch())/60.0;
    
    sxpPropagate(tSince);  
    
    // Compute PV with previous calculated parameters
    return computePVCoordinates();
  }

  /** Computation of the first commons parameters.  */
  private void initializeCommons() {
    
    double a1 = Math.pow( TLEConstants.xke / (tle.getMeanMotion()*60.0), TLEConstants.twoThirds);
    cosi0 = Math.cos(tle.getI());
    theta2 = cosi0 * cosi0;
    double x3thm1 = 3.* theta2 - 1.;
    e0sq = tle.getE() * tle.getE();
    beta02 = 1 - e0sq;
    beta0 = Math.sqrt(beta02);
    double tval = TLEConstants.ck2 * 1.5 * x3thm1 / (beta0 * beta02);
    double delta1 = tval/(a1 * a1);
    double a0 = a1 * (1 - delta1 * (TLEConstants.oneThird + delta1 * (1. + 134./81.* delta1)));
    double delta0 = tval/(a0 * a0);

    // recover original mean motion and semi-major axis :
    xn0dp = tle.getMeanMotion() * 60.0 /(delta0 + 1.0);
    a0dp = a0/(1.0 - delta0);    
    
    // Values of s and qms2t :    
    s4 = TLEConstants.s;  // unmodified value for s
    double q0ms24 = TLEConstants.qoms2t; // unmodified value for q0ms2T
    
    perige = (a0dp*(1-tle.getE()) - TLEConstants.ae)* TLEConstants.er; // perige 
    
    //  For perigee below 156 km, the values of s and qoms2t are changed :
    if(perige < 156) {
      if(perige <= 98) {
        s4 = 20.; 
      }
      else {
        s4 = perige - 78.; 
      }
      double temp_val = (120.0 - s4) * TLEConstants.ae / TLEConstants.er;
      double temp_val_squared = temp_val * temp_val;
      q0ms24 = temp_val_squared * temp_val_squared;
      s4 = s4 / TLEConstants.er + TLEConstants.ae; // new value for q0ms2T and s
    } 
    
    double pinv = 1. / (a0dp * beta02);
    double pinvsq = pinv * pinv;
    tsi = 1. / (a0dp - s4);
    eta = a0dp*tle.getE()*tsi;
    etasq = eta*eta;
    eeta = tle.getE()*eta;

    double psisq = Math.abs(1-etasq); // abs because pow 3.5 needs positive value
    double tsi_squared = tsi * tsi;
    coef = q0ms24 * tsi_squared * tsi_squared;
    coef1 = coef / Math.pow(psisq,3.5);

    // C2 and C1 coefficients computation :
    c2 = coef1 * xn0dp * (a0dp * (1 + 1.5*etasq + eeta * (4 + etasq)) + 
        0.75*TLEConstants.ck2 * tsi/psisq * x3thm1 *(8 + 3*etasq * (8 + etasq)));
    c1 = tle.getBStar() * c2;
    sini0 = Math.sin(tle.getI());    
   
    double x1mth2 = 1-theta2;

    // C4 coefficient computation :
    c4 = 2 * xn0dp * coef1 * a0dp * beta02 * (eta * (2 + 0.5*etasq) + tle.getE() * 
        (0.5 + 2*etasq) - 2 * TLEConstants.ck2 * tsi/(a0dp*psisq) * (-3*x3thm1 * 
            (1 - 2*eeta + etasq * (1.5 - 0.5*eeta)) + 0.75*x1mth2 * 
              (2 * etasq - eeta * (1+etasq)) * Math.cos(2*tle.getPerigeeArgument())));
    
    double theta4 = theta2 * theta2;
    double temp1 = 3 * TLEConstants.ck2 * pinvsq * xn0dp;
    double temp2 = temp1 * TLEConstants.ck2 * pinvsq;
    double temp3 = 1.25 * TLEConstants.ck4 * pinvsq * pinvsq * xn0dp;

    // atmospheric and gravitation coefs :(Mdf and OMEGAdf)
    xmdot = xn0dp + 0.5*temp1 * beta0 * x3thm1 + 0.0625*temp2 * beta0 * 
                 (13 - 78*theta2 + 137*theta4);
    
    double x1m5th = 1 - 5*theta2;
    
    omgdot = -0.5*temp1 * x1m5th + 0.0625*temp2 * (7 - 114*theta2 + 395*theta4) + 
                temp3*(3 - 36*theta2 + 49*theta4);
    
    double xhdot1 = -temp1 * cosi0;
    
    xnodot = xhdot1 + (0.5*temp2 * (4 - 19*theta2) + 2*temp3 * (3 - 7*theta2)) * cosi0;
    xnodcf = 3.5 * beta02 * xhdot1 * c1;
    t2cof = 1.5 * c1;    

  }

  /** Retrieves the position and velocity.
   * @return the computed PVCoordinates.
   * @throws OrekitException 
   */
  private PVCoordinates computePVCoordinates() throws OrekitException {

    // Long period periodics 
    double axn = e * Math.cos(omega);
    double temp = 1. / (a * (1. - e*e));
    double xlcof = .125 * TLEConstants.a3ovk2 * sini0 * (3 + 5*cosi0) / (1. + cosi0);
    double aycof = 0.25 * TLEConstants.a3ovk2 * sini0;
    double xll = temp * xlcof * axn;
    double aynl = temp * aycof;
    double xlt = xl + xll;
    double ayn = e * Math.sin(omega) + aynl;
    double elsq = axn * axn + ayn * ayn;
    double capu = trimAngle(( xlt - xnode), Math.PI); 
    double epw = capu;
    double temp1, temp2;
    double ecosE = 0;
    double esinE = 0;
    double pl, r;
    double betal;
    double u, sinu, cosu, sin2u, cos2u;
    double rk, uk, xnodek, xinck;
    double sinuk, cosuk, sinik, cosik, sinnok, cosnok, xmx, xmy;
    double sinEPW = 0;
    double cosEPW = 0;

    // Dundee changes:  items dependent on cosio get recomputed:
    double cosi0Sq = cosi0 * cosi0;
    double x3thm1 = 3.0 * cosi0Sq - 1.0;
    double x1mth2 = 1.0 - cosi0Sq;
    double x7thm1 = 7.0 * cosi0Sq - 1.0;

    if (e > (1 - 1e-6)) {
      String message = Translator.getInstance().translate(
         "Eccentricity is becoming greater than 1. Unable to continue TLE propagation." +
         "Satellite number : {0}. Element number : {1}.");
      throw new OrekitException(message, new String[] 
         {Integer.toString(tle.getSatelliteNumber()),
          Integer.toString(tle.getElementNumber())});
    }   
    if ( (a * (1. - e) < 1.) || (a * (1. + e) < 1.) ) {
      String message = Translator.getInstance().translate(
         "Perige within earth." + "Satellite number : {0}. Element number : {1}.");
      throw new OrekitException(message, new String[] 
         {Integer.toString(tle.getSatelliteNumber()),
          Integer.toString(tle.getElementNumber())});
    }   
    
    // Solve Kepler's' Equation.     
    double newtonRaphsonEpsilon = 1e-12;

    for(int j = 0; j < 10; j++) {     

      double f, fdot, delta_epw;
      boolean doSecondOrderNewtonRaphson = true;

      sinEPW = Math.sin( epw);
      cosEPW = Math.cos( epw);
      ecosE = axn * cosEPW + ayn * sinEPW;
      esinE = axn * sinEPW - ayn * cosEPW;
      f = capu - epw + esinE;
      if (Math.abs(f) < newtonRaphsonEpsilon) break;
      fdot = 1. - ecosE;
      delta_epw = f / fdot;
      if(j==0) {
        double maxNewtonRaphson = 1.25 * Math.abs(e);
        doSecondOrderNewtonRaphson = false;        
        if( delta_epw > maxNewtonRaphson) {
          delta_epw = maxNewtonRaphson;
        }          
        else if( delta_epw < -maxNewtonRaphson) {
          delta_epw = -maxNewtonRaphson;
        }          
        else {
          doSecondOrderNewtonRaphson = true;
        }          
      }
      if(doSecondOrderNewtonRaphson) {
        delta_epw = f / (fdot + 0.5 * esinE * delta_epw);
      }        
      epw += delta_epw;
    }

    // Short period preliminary quantities
    temp = 1 - elsq;
    pl = a * temp;
    r = a * (1-ecosE);
    temp2 = a / r;
    betal = Math.sqrt(temp);
    temp = esinE / (1 + betal);
    cosu = temp2 * (cosEPW - axn + ayn * temp);
    sinu = temp2 * (sinEPW - ayn - axn * temp);
    u = Math.atan2(sinu, cosu);
    sin2u = 2 * sinu * cosu;
    cos2u = 2 * cosu * cosu - 1;
    temp1 = TLEConstants.ck2 / pl;
    temp2 = temp1 / pl;

    // Update for short periodics
    rk = r * (1 - 1.5 * temp2 * betal * x3thm1) + 0.5 * temp1 * x1mth2 * cos2u;
    uk = u - 0.25 * temp2 * x7thm1 * sin2u;
    xnodek = xnode + 1.5 * temp2 * cosi0 * sin2u;
    xinck = i + 1.5 * temp2 * cosi0 * sini0 * cos2u;

    // Orientation vectors
    sinuk = Math.sin(uk);
    cosuk = Math.cos(uk);
    sinik = Math.sin(xinck);
    cosik = Math.cos(xinck);
    sinnok = Math.sin(xnodek);
    cosnok = Math.cos(xnodek);
    xmx = -sinnok * cosik;
    xmy = cosnok * cosik;
    double ux = xmx * sinuk + cosnok * cosuk;
    double uy = xmy * sinuk + sinnok * cosuk;
    double uz = sinik * sinuk;
    
    // Position and velocity

    Vector3D pos = new Vector3D(1000*rk*ux*TLEConstants.er,
                                1000*rk*uy*TLEConstants.er,
                                1000*rk*uz*TLEConstants.er);
    
    double rdot = TLEConstants.xke*Math.sqrt(a)*esinE/r;
    double rfdot = TLEConstants.xke*Math.sqrt(pl)/r;
    double xn = TLEConstants.xke/(a * Math.sqrt(a));
    double rdotk = rdot-xn*temp1*x1mth2*sin2u;
    double rfdotk = rfdot+xn*temp1*(x1mth2*cos2u+1.5*x3thm1);
    double vx = xmx*cosuk-cosnok*sinuk;
    double vy = xmy*cosuk-sinnok*sinuk;
    double vz = sinik*cosuk;
    
    Vector3D vel = new Vector3D(1000*(rdotk*ux+rfdotk*vx)*TLEConstants.er/60.0,
                                1000*(rdotk*uy+rfdotk*vy)*TLEConstants.er/60.0,
                                1000*(rdotk*uz+rfdotk*vz)*TLEConstants.er/60.0);  

    return new PVCoordinates(pos,vel);

  }

  /** Trim an angle between ref - PI and ref + PI.
   * @param a the angle (rad)
   * @param ref the reference
   * @return the trimed angle
   */
  protected static double trimAngle (double a, double ref) {
    double twoPi = 2 * Math.PI;
    return a - twoPi * Math.floor ((a + Math.PI - ref) / twoPi);
  }
  
  /** Initialization proper to each propagator (SGP or SDP).
   * @param tSince the offset from initial epoch (min)
   */
  protected abstract void sxpInitialize();
  
  /** Propagation proper to each propagator (SGP or SDP).
   * @param t the offset from initial epoch (min)
   */
  protected abstract void sxpPropagate(double t) throws OrekitException;
  
  /** Initial state. */
  protected final TLE tle;

  /** Elements to determine for PV computation. */
  protected double xnode; // final RAAN
  protected double a; // final semi major axis
  protected double e; // final eccentricity
  protected double i; // final inclination
  protected double omega; // final perigee argument
  protected double xl; // L from SPTRCK #3

  /** Intermediate values. */
  protected double a0dp; // original recovered semi major axis
  protected double xn0dp; // original recovered mean motion
  protected double cosi0; // cosinus original inclination
  protected double theta2; // cos io squared
  protected double sini0; // sinus original inclination
  protected double xmdot; // common parameter for mean anomaly (M) computation
  protected double omgdot; // common parameter for perigee argument (omega) computation
  protected double xnodot; // common parameter for raan (OMEGA) computation
  protected double e0sq; // original eccentricity squared
  protected double beta02; // 1 - e2
  protected double beta0; // sqrt (1 - e2)
  protected double perige; // perigee, expressed in KM and ALTITUDE
  
  protected double etasq; // eta squared
  protected double eeta; // original eccentricity * eta
  protected double s4; // s* new value for the contant s
  protected double tsi; // tsi from SPTRCK #3
  protected double eta; // eta from SPTRCK #3
  protected double coef; // coef for SGP C3 computation
  protected double coef1; // coef for SGP C5 computation

  protected double c1; // C1 from SPTRCK #3
  protected double c2; // C2 from SPTRCK #3
  protected double c4; // C4 from SPTRCK #3
  protected double xnodcf; // common parameter for raan (OMEGA) computation 
  protected double t2cof; // 3/2 * C1
  
  public boolean isSDP;
}
//      My comments

// tle->xn0 = meanmotion*60 (n)
// tle->xincl = I
// tle->omega0 = perigeeArgument (omega)
// tle->xmo = mean anomaly (M)
// tle->xnodeo = raan
