package fr.cs.orekit.tle;

import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.util.MathUtils;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.utils.PVCoordinates;

/** This class provides elements to propagate TLE's.
 * <p>
 * The models used are SGP4 and SDP4, initially proposed by NORAD as the unique convenient
 * propagator for TLE's. Inputs and outputs of this propagator are only suited for
 * NORAD two lines elements sets, since it uses estimations and mean values appropriate
 * for TLE's only.
 * </p>
 * <p>
 * Deep- or near- space propagator is selected internally according to NORAD recommendations
 * so that the user has not to worry about the used computation methods. One instance is created
 * for each TLE (this instance can only be get using {@link #selectExtrapolator(TLE)} method,
 * and can compute {@link PVCoordinates position and velocity coordinates} at any
 * time. Maximum accuracy is guaranteed in a 24h range period before and after the provided
 * TLE epoch (of course this accuracy is not really measurable nor predictable: according to
 * <a href="http://www.celestrak.com/">CelesTrak</a>, the precision is close to one kilometer
 * and error won't probably rise above 2 km).
 * </p>
 * <p>This implementation is largely inspired from the paper and source code <a
 * href="http://www.celestrak.com/publications/AIAA/2006-6753/">Revisiting Spacetrack
 * Report #3</a> and is fully compliant with its results and tests cases.</p>
 * @author SPACETRACK Report #3 project. Felix R. Hoots, Ronald L. Roehrich, December 1980 (original fortran)
 * @author Revisiting Spacetrack Report #3. David A. Vallado, Paul Crawford, Richard Hujsak, T.S. Kelso (C++ translation and improvements)
 * @author F. Maussion (Java translation)
 * @see TLE
 */
public abstract class TLEPropagator {

    /** Initial state. */
    protected final TLE tle;

    /** final RAAN. */
    protected double xnode;

    /** final semi major axis. */
    protected double a;

    /** final eccentricity. */
    protected double e;

    /** final inclination. */
    protected double i;

    /** final perigee argument. */
    protected double omega;

    /** L from SPTRCK #3. */
    protected double xl;

    /** original recovered semi major axis. */
    protected double a0dp;

    /** original recovered mean motion. */
    protected double xn0dp;

    /** cosinus original inclination. */
    protected double cosi0;

    /** cos io squared. */
    protected double theta2;

    /** sinus original inclination. */
    protected double sini0;

    /** common parameter for mean anomaly (M) computation. */
    protected double xmdot;

    /** common parameter for perigee argument (omega) computation. */
    protected double omgdot;

    /** common parameter for raan (OMEGA) computation. */
    protected double xnodot;

    /** original eccentricity squared. */
    protected double e0sq;
    /** 1 - e2. */
    protected double beta02;
    /** sqrt (1 - e2). */
    protected double beta0;

    /** perigee, expressed in KM and ALTITUDE. */
    protected double perige;

    /** eta squared. */
    protected double etasq;

    /** original eccentricity * eta. */
    protected double eeta;

    /** s* new value for the contant s. */
    protected double s4;

    /** tsi from SPTRCK #3. */
    protected double tsi;

    /** eta from SPTRCK #3. */
    protected double eta;

    /** coef for SGP C3 computation. */
    protected double coef;

    /** coef for SGP C5 computation. */
    protected double coef1;

    /** C1 from SPTRCK #3. */
    protected double c1;

    /** C2 from SPTRCK #3. */
    protected double c2;

    /** C4 from SPTRCK #3. */
    protected double c4;

    /** common parameter for raan (OMEGA) computation. */
    protected double xnodcf;

    /** 3/2 * C1. */
    protected double t2cof;

    /** Protected constructor for derived classes.
     * @param initialTLE the unique TLE to propagate
     * @exception OrekitException if some specific error occurs
     */
    protected TLEPropagator(TLE initialTLE) throws OrekitException {
        tle = initialTLE;
        initializeCommons();
        sxpInitialize();
    }

    /** Selects the extrapolator to use with the selected TLE.
     * @param tle the TLE to propagate.
     * @return the correct propagator.
     * @exception OrekitException if the underlying model cannot be initialized
     */
    public static TLEPropagator selectExtrapolator(TLE tle) throws OrekitException {

        final double a1 = Math.pow( TLEConstants.xke / (tle.getMeanMotion()*60.0), TLEConstants.twoThirds);
        final double cosi0 = Math.cos(tle.getI());
        final double temp = TLEConstants.ck2 * 1.5 * (3*cosi0*cosi0 - 1.0) * Math.pow(1.0 - tle.getE()*tle.getE(),-1.5);
        final double delta1 = temp/(a1*a1);
        final double a0 = a1 * (1.0 - delta1 * (TLEConstants.oneThird + delta1 * (delta1 * 134./81.+1.0)));
        final double delta0 = temp/(a0*a0);

        // recover original mean motion :
        final double xn0dp = tle.getMeanMotion()*60.0/(delta0 + 1.0);

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
     * @exception OrekitException if propagation cannot be performed at given date
     */
    public PVCoordinates getPVCoordinates(AbsoluteDate date)
        throws OrekitException {

        sxpPropagate(date.minus(tle.getEpoch()) / 60.0);

        // Compute PV with previous calculated parameters
        return computePVCoordinates();
    }

    /** Computation of the first commons parameters.
     */
    private void initializeCommons() {

        final double a1 = Math.pow( TLEConstants.xke / (tle.getMeanMotion()*60.0), TLEConstants.twoThirds);
        cosi0 = Math.cos(tle.getI());
        theta2 = cosi0 * cosi0;
        final double x3thm1 = 3.* theta2 - 1.;
        e0sq = tle.getE() * tle.getE();
        beta02 = 1 - e0sq;
        beta0 = Math.sqrt(beta02);
        final double tval = TLEConstants.ck2 * 1.5 * x3thm1 / (beta0 * beta02);
        final double delta1 = tval/(a1 * a1);
        final double a0 = a1 * (1 - delta1 * (TLEConstants.oneThird + delta1 * (1. + 134./81.* delta1)));
        final double delta0 = tval/(a0 * a0);

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
            final double temp_val = (120.0 - s4) * TLEConstants.ae / TLEConstants.er;
            final double temp_val_squared = temp_val * temp_val;
            q0ms24 = temp_val_squared * temp_val_squared;
            s4 = s4 / TLEConstants.er + TLEConstants.ae; // new value for q0ms2T and s
        }

        final double pinv = 1. / (a0dp * beta02);
        final double pinvsq = pinv * pinv;
        tsi = 1. / (a0dp - s4);
        eta = a0dp*tle.getE()*tsi;
        etasq = eta*eta;
        eeta = tle.getE()*eta;

        final double psisq = Math.abs(1-etasq); // abs because pow 3.5 needs positive value
        final double tsi_squared = tsi * tsi;
        coef = q0ms24 * tsi_squared * tsi_squared;
        coef1 = coef / Math.pow(psisq,3.5);

        // C2 and C1 coefficients computation :
        c2 = coef1 * xn0dp * (a0dp * (1 + 1.5*etasq + eeta * (4 + etasq)) +
                0.75*TLEConstants.ck2 * tsi/psisq * x3thm1 *(8 + 3*etasq * (8 + etasq)));
        c1 = tle.getBStar() * c2;
        sini0 = Math.sin(tle.getI());

        final double x1mth2 = 1-theta2;

        // C4 coefficient computation :
        c4 = 2 * xn0dp * coef1 * a0dp * beta02 * (eta * (2 + 0.5*etasq) + tle.getE() *
                (0.5 + 2*etasq) - 2 * TLEConstants.ck2 * tsi/(a0dp*psisq) * (-3*x3thm1 *
                        (1 - 2*eeta + etasq * (1.5 - 0.5*eeta)) + 0.75*x1mth2 *
                        (2 * etasq - eeta * (1+etasq)) * Math.cos(2*tle.getPerigeeArgument())));

        final double theta4 = theta2 * theta2;
        final double temp1 = 3 * TLEConstants.ck2 * pinvsq * xn0dp;
        final double temp2 = temp1 * TLEConstants.ck2 * pinvsq;
        final double temp3 = 1.25 * TLEConstants.ck4 * pinvsq * pinvsq * xn0dp;

        // atmospheric and gravitation coefs :(Mdf and OMEGAdf)
        xmdot = xn0dp +
                0.5 * temp1 * beta0 * x3thm1 +
                0.0625 * temp2 * beta0 * (13 - 78 * theta2 + 137 * theta4);

        final double x1m5th = 1 - 5 * theta2;

        omgdot = -0.5 * temp1 * x1m5th +
                 0.0625 * temp2 * (7 - 114 * theta2 + 395 * theta4) +
                 temp3 * (3 - 36 * theta2 + 49 * theta4);

        final double xhdot1 = -temp1 * cosi0;

        xnodot = xhdot1 + (0.5 * temp2 * (4 - 19 * theta2) + 2 * temp3 * (3 - 7 * theta2)) * cosi0;
        xnodcf = 3.5 * beta02 * xhdot1 * c1;
        t2cof = 1.5 * c1;

    }

    /** Retrieves the position and velocity.
     * @return the computed PVCoordinates.
     * @exception OrekitException
     */
    private PVCoordinates computePVCoordinates() throws OrekitException {

        // Long period periodics
        final double axn = e * Math.cos(omega);
        double temp = 1. / (a * (1. - e*e));
        final double xlcof = .125 * TLEConstants.a3ovk2 * sini0 * (3 + 5*cosi0) / (1. + cosi0);
        final double aycof = 0.25 * TLEConstants.a3ovk2 * sini0;
        final double xll = temp * xlcof * axn;
        final double aynl = temp * aycof;
        final double xlt = xl + xll;
        final double ayn = e * Math.sin(omega) + aynl;
        final double elsq = axn * axn + ayn * ayn;
        final double capu = MathUtils.normalizeAngle(xlt - xnode, Math.PI);
        double epw = capu;
        double ecosE = 0;
        double esinE = 0;
        double sinEPW = 0;
        double cosEPW = 0;

        // Dundee changes:  items dependent on cosio get recomputed:
        final double cosi0Sq = cosi0 * cosi0;
        final double x3thm1 = 3.0 * cosi0Sq - 1.0;
        final double x1mth2 = 1.0 - cosi0Sq;
        final double x7thm1 = 7.0 * cosi0Sq - 1.0;

        if (e > (1 - 1e-6)) {
            throw new OrekitException("eccentricity becomes too large for TLE propagation " +
                                      "(e: {0}, satellite number: {1})",
                                      new Object[] {
                                          new Double(e), new Integer(tle.getSatelliteNumber())
                                      });
        }
        if ((a * (1. - e) < 1.) || (a * (1. + e) < 1.)) {
            throw new OrekitException("too small perigee radius for TLE propagation " +
                                      "(r: {0}, satellite number: {1})",
                                      new Object[] {
                                          new Double(a * (1. - e)), new Integer(tle.getSatelliteNumber())
                                      });
        }

        // Solve Kepler's' Equation.
        final double newtonRaphsonEpsilon = 1e-12;

        for(int j = 0; j < 10; j++) {

            boolean doSecondOrderNewtonRaphson = true;

            sinEPW = Math.sin( epw);
            cosEPW = Math.cos( epw);
            ecosE = axn * cosEPW + ayn * sinEPW;
            esinE = axn * sinEPW - ayn * cosEPW;
            final double f = capu - epw + esinE;
            if (Math.abs(f) < newtonRaphsonEpsilon) break;
            final double fdot = 1. - ecosE;
            double delta_epw = f / fdot;
            if(j==0) {
                final double maxNewtonRaphson = 1.25 * Math.abs(e);
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
        final double pl = a * temp;
        final double r = a * (1-ecosE);
        double temp2 = a / r;
        final double betal = Math.sqrt(temp);
        temp = esinE / (1 + betal);
        final double cosu = temp2 * (cosEPW - axn + ayn * temp);
        final double sinu = temp2 * (sinEPW - ayn - axn * temp);
        final double u = Math.atan2(sinu, cosu);
        final double sin2u = 2 * sinu * cosu;
        final double cos2u = 2 * cosu * cosu - 1;
        final double temp1 = TLEConstants.ck2 / pl;
        temp2 = temp1 / pl;

        // Update for short periodics
        final double rk = r * (1 - 1.5 * temp2 * betal * x3thm1) + 0.5 * temp1 * x1mth2 * cos2u;
        final double uk = u - 0.25 * temp2 * x7thm1 * sin2u;
        final double xnodek = xnode + 1.5 * temp2 * cosi0 * sin2u;
        final double xinck = i + 1.5 * temp2 * cosi0 * sini0 * cos2u;

        // Orientation vectors
        final double sinuk = Math.sin(uk);
        final double cosuk = Math.cos(uk);
        final double sinik = Math.sin(xinck);
        final double cosik = Math.cos(xinck);
        final double sinnok = Math.sin(xnodek);
        final double cosnok = Math.cos(xnodek);
        final double xmx = -sinnok * cosik;
        final double xmy = cosnok * cosik;
        final double ux = xmx * sinuk + cosnok * cosuk;
        final double uy = xmy * sinuk + sinnok * cosuk;
        final double uz = sinik * sinuk;

        // Position and velocity

        final Vector3D pos = new Vector3D(1000*rk*ux*TLEConstants.er,
                                          1000*rk*uy*TLEConstants.er,
                                          1000*rk*uz*TLEConstants.er);

        final double rdot = TLEConstants.xke*Math.sqrt(a)*esinE/r;
        final double rfdot = TLEConstants.xke*Math.sqrt(pl)/r;
        final double xn = TLEConstants.xke/(a * Math.sqrt(a));
        final double rdotk = rdot-xn*temp1*x1mth2*sin2u;
        final double rfdotk = rfdot+xn*temp1*(x1mth2*cos2u+1.5*x3thm1);
        final double vx = xmx*cosuk-cosnok*sinuk;
        final double vy = xmy*cosuk-sinnok*sinuk;
        final double vz = sinik*cosuk;

        final Vector3D vel = new Vector3D(1000*(rdotk*ux+rfdotk*vx)*TLEConstants.er/60.0,
                                          1000*(rdotk*uy+rfdotk*vy)*TLEConstants.er/60.0,
                                          1000*(rdotk*uz+rfdotk*vz)*TLEConstants.er/60.0);

        return new PVCoordinates(pos,vel);

    }

    /** Initialization proper to each propagator (SGP or SDP).
     * @exception OrekitException if some specific error occurs
     */
    protected abstract void sxpInitialize() throws OrekitException;

    /** Propagation proper to each propagator (SGP or SDP).
     * @param t the offset from initial epoch (min)
     * @exception OrekitException if current state cannot be propagated
     */
    protected abstract void sxpPropagate(double t) throws OrekitException;

}
