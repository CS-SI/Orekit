/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.propagation.analytical.tle;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;


/** This class contains the methods that compute deep space perturbation terms.
 * <p>
 * The user should not bother in this class since it is handled internaly by the
 * {@link TLEPropagator}.
 * </p>
 * <p>This implementation is largely inspired from the paper and source code <a
 * href="http://www.celestrak.com/publications/AIAA/2006-6753/">Revisiting Spacetrack
 * Report #3</a> and is fully compliant with its results and tests cases.</p>
 * @author Felix R. Hoots, Ronald L. Roehrich, December 1980 (original fortran)
 * @author David A. Vallado, Paul Crawford, Richard Hujsak, T.S. Kelso (C++ translation and improvements)
 * @author Fabien Maussion (java translation)
 */
public class DeepSDP4 extends SDP4 {

    // CHECKSTYLE: stop JavadocVariable check

    // Internal constants
    private static final double ZNS      = 1.19459E-5;
    private static final double ZES      = 0.01675;
    private static final double ZNL      = 1.5835218E-4;
    private static final double ZEL      = 0.05490;
    private static final double THDT     = 4.3752691E-3;
    private static final double C1SS     =  2.9864797E-6;
    private static final double C1L      = 4.7968065E-7;

    private static final double ROOT22   = 1.7891679E-6;
    private static final double ROOT32   = 3.7393792E-7;
    private static final double ROOT44   = 7.3636953E-9;
    private static final double ROOT52   = 1.1428639E-7;
    private static final double ROOT54   = 2.1765803E-9;

    private static final double Q22      =  1.7891679E-6;
    private static final double Q31      =  2.1460748E-6;
    private static final double Q33      =  2.2123015E-7;

    private static final double C_FASX2  =  0.99139134268488593;
    private static final double S_FASX2  =  0.13093206501640101;
    private static final double C_2FASX4 =  0.87051638752972937;
    private static final double S_2FASX4 = -0.49213943048915526;
    private static final double C_3FASX6 =  0.43258117585763334;
    private static final double S_3FASX6 =  0.90159499016666422;

    private static final double C_G22    =  0.87051638752972937;
    private static final double S_G22    = -0.49213943048915526;
    private static final double C_G32    =  0.57972190187001149;
    private static final double S_G32    =  0.81481440616389245;
    private static final double C_G44    = -0.22866241528815548;
    private static final double S_G44    =  0.97350577801807991;
    private static final double C_G52    =  0.49684831179884198;
    private static final double S_G52    =  0.86783740128127729;
    private static final double C_G54    = -0.29695209575316894;
    private static final double S_G54    = -0.95489237761529999;

    /** Integration step (seconds). */
    private static final double SECULAR_INTEGRATION_STEP  = 720.0;

    /** Integration order. */
    private static final int    SECULAR_INTEGRATION_ORDER = 2;

    /** Intermediate values. */
    private double thgr;
    private double xnq;
    private double omegaq;
    private double zcosil;
    private double zsinil;
    private double zsinhl;
    private double zcoshl;
    private double zmol;
    private double zcosgl;
    private double zsingl;
    private double zmos;
    private double savtsn;

    private double ee2;
    private double e3;
    private double xi2;
    private double xi3;
    private double xl2;
    private double xl3;
    private double xl4;
    private double xgh2;
    private double xgh3;
    private double xgh4;
    private double xh2;
    private double xh3;

    private double d2201;
    private double d2211;
    private double d3210;
    private double d3222;
    private double d4410;
    private double d4422;
    private double d5220;
    private double d5232;
    private double d5421;
    private double d5433;
    private double xlamo;

    private double sse;
    private double ssi;
    private double ssl;
    private double ssh;
    private double ssg;
    private double se2;
    private double si2;
    private double sl2;
    private double sgh2;
    private double sh2;
    private double se3;
    private double si3;
    private double sl3;
    private double sgh3;
    private double sh3;
    private double sl4;
    private double sgh4;

    private double del1;
    private double del2;
    private double del3;
    private double xfact;
    private double xli;
    private double xni;
    private double atime;

    private double pe;
    private double pinc;
    private double pl;
    private double pgh;
    private double ph;

    private double[] derivs;

    // CHECKSTYLE: resume JavadocVariable check

    /** Flag for resonant orbits. */
    private boolean resonant;

    /** Flag for synchronous orbits. */
    private boolean synchronous;

    /** Flag for compliance with Dundee modifications. */
    private boolean isDundeeCompliant = true;

    /** Constructor for a unique initial TLE.
     * @param initialTLE the TLE to propagate.
     * @param attitudeProvider provider for attitude computation
     * @param mass spacecraft mass (kg)
     * @exception OrekitException if some specific error occurs
     */
    public DeepSDP4(final TLE initialTLE, final AttitudeProvider attitudeProvider,
                       final double mass) throws OrekitException {
        super(initialTLE, attitudeProvider, mass);
    }

    /** Computes luni - solar terms from initial coordinates and epoch.
     * @exception OrekitException when UTC time steps can't be read
     */
    protected void luniSolarTermsComputation() throws OrekitException {

        final double sing = FastMath.sin(tle.getPerigeeArgument());
        final double cosg = FastMath.cos(tle.getPerigeeArgument());

        final double sinq = FastMath.sin(tle.getRaan());
        final double cosq = FastMath.cos(tle.getRaan());
        final double aqnv = 1.0 / a0dp;

        // Compute julian days since 1900
        final double daysSince1900 =
            (tle.getDate().durationFrom(AbsoluteDate.JULIAN_EPOCH) +
             tle.getDate().timeScalesOffset(TimeScalesFactory.getUTC(), TimeScalesFactory.getTT())) / Constants.JULIAN_DAY - 2415020;


        double cc = C1SS;
        double ze = ZES;
        double zn = ZNS;
        double zsinh = sinq;
        double zcosh = cosq;

        thgr = thetaG(tle.getDate());
        xnq = xn0dp;
        omegaq = tle.getPerigeeArgument();

        final double xnodce = 4.5236020 - 9.2422029e-4 * daysSince1900;
        final double stem = FastMath.sin(xnodce);
        final double ctem = FastMath.cos(xnodce);
        final double c_minus_gam = 0.228027132 * daysSince1900 - 1.1151842;
        final double gam = 5.8351514 + 0.0019443680 * daysSince1900;

        zcosil = 0.91375164 - 0.03568096 * ctem;
        zsinil = FastMath.sqrt(1.0 - zcosil * zcosil);
        zsinhl = 0.089683511 * stem / zsinil;
        zcoshl = FastMath.sqrt(1.0 - zsinhl * zsinhl);
        zmol = MathUtils.normalizeAngle(c_minus_gam, FastMath.PI);

        double zx = 0.39785416 * stem / zsinil;
        final double zy = zcoshl * ctem + 0.91744867 * zsinhl * stem;
        zx = FastMath.atan2( zx, zy) + gam - xnodce;
        zcosgl = FastMath.cos( zx);
        zsingl = FastMath.sin( zx);
        zmos = MathUtils.normalizeAngle(6.2565837 + 0.017201977 * daysSince1900, FastMath.PI);

        // Do solar terms
        savtsn = 1e20;

        double zcosi =  0.91744867;
        double zsini =  0.39785416;
        double zsing = -0.98088458;
        double zcosg =  0.1945905;

        double se = 0;
        double sgh = 0;
        double sh = 0;
        double si = 0;
        double sl = 0;

        // There was previously some convoluted logic here, but it boils
        // down to this:  we compute the solar terms,  then the lunar terms.
        // On a second pass,  we recompute the solar terms, taking advantage
        // of the improved data that resulted from computing lunar terms.
        for (int iteration = 0; iteration < 2; ++iteration) {
            final double a1 = zcosg * zcosh + zsing * zcosi * zsinh;
            final double a3 = -zsing * zcosh + zcosg * zcosi * zsinh;
            final double a7 = -zcosg * zsinh + zsing * zcosi * zcosh;
            final double a8 = zsing * zsini;
            final double a9 = zsing * zsinh + zcosg * zcosi * zcosh;
            final double a10 = zcosg * zsini;
            final double a2 = cosi0 * a7 + sini0 * a8;
            final double a4 = cosi0 * a9 + sini0 * a10;
            final double a5 = -sini0 * a7 + cosi0 * a8;
            final double a6 = -sini0 * a9 + cosi0 * a10;
            final double x1 = a1 * cosg + a2 * sing;
            final double x2 = a3 * cosg + a4 * sing;
            final double x3 = -a1 * sing + a2 * cosg;
            final double x4 = -a3 * sing + a4 * cosg;
            final double x5 = a5 * sing;
            final double x6 = a6 * sing;
            final double x7 = a5 * cosg;
            final double x8 = a6 * cosg;
            final double z31 = 12 * x1 * x1 - 3 * x3 * x3;
            final double z32 = 24 * x1 * x2 - 6 * x3 * x4;
            final double z33 = 12 * x2 * x2 - 3 * x4 * x4;
            final double z11 = -6 * a1 * a5 + e0sq * (-24 * x1 * x7 - 6 * x3 * x5);
            final double z12 = -6 * (a1 * a6 + a3 * a5) +
                               e0sq * (-24 * (x2 * x7 + x1 * x8) - 6 * (x3 * x6 + x4 * x5));
            final double z13 = -6 * a3 * a6 + e0sq * (-24 * x2 * x8 - 6 * x4 * x6);
            final double z21 = 6 * a2 * a5 + e0sq * (24 * x1 * x5 - 6 * x3 * x7);
            final double z22 = 6 * (a4 * a5 + a2 * a6) +
                               e0sq * (24 * (x2 * x5 + x1 * x6) - 6 * (x4 * x7 + x3 * x8));
            final double z23 = 6 * a4 * a6 + e0sq * (24 * x2 * x6 - 6 * x4 * x8);
            final double s3 = cc / xnq;
            final double s2 = -0.5 * s3 / beta0;
            final double s4 = s3 * beta0;
            final double s1 = -15 * tle.getE() * s4;
            final double s5 = x1 * x3 + x2 * x4;
            final double s6 = x2 * x3 + x1 * x4;
            final double s7 = x2 * x4 - x1 * x3;
            double z1 = 3 * (a1 * a1 + a2 * a2) + z31 * e0sq;
            double z2 = 6 * (a1 * a3 + a2 * a4) + z32 * e0sq;
            double z3 = 3 * (a3 * a3 + a4 * a4) + z33 * e0sq;

            z1 = z1 + z1 + beta02 * z31;
            z2 = z2 + z2 + beta02 * z32;
            z3 = z3 + z3 + beta02 * z33;
            se = s1 * zn * s5;
            si = s2 * zn * (z11 + z13);
            sl = -zn * s3 * (z1 + z3 - 14 - 6 * e0sq);
            sgh = s4 * zn * (z31 + z33 - 6);
            if (tle.getI() < (FastMath.PI / 60.0)) {
                // inclination smaller than 3 degrees
                sh = 0;
            } else {
                sh = -zn * s2 * (z21 + z23);
            }
            ee2  =  2 * s1 * s6;
            e3   =  2 * s1 * s7;
            xi2  =  2 * s2 * z12;
            xi3  =  2 * s2 * (z13 - z11);
            xl2  = -2 * s3 * z2;
            xl3  = -2 * s3 * (z3 - z1);
            xl4  = -2 * s3 * (-21 - 9 * e0sq) * ze;
            xgh2 =  2 * s4 * z32;
            xgh3 =  2 * s4 * (z33 - z31);
            xgh4 = -18 * s4 * ze;
            xh2  = -2 * s2 * z22;
            xh3  = -2 * s2 * (z23 - z21);

            if (iteration == 0) { // we compute lunar terms only on the first pass:
                sse = se;
                ssi = si;
                ssl = sl;
                ssh = (tle.getI() < (FastMath.PI / 60.0)) ? 0 : sh / sini0;
                ssg = sgh - cosi0 * ssh;
                se2 = ee2;
                si2 = xi2;
                sl2 = xl2;
                sgh2 = xgh2;
                sh2 = xh2;
                se3 = e3;
                si3 = xi3;
                sl3 = xl3;
                sgh3 = xgh3;
                sh3 = xh3;
                sl4 = xl4;
                sgh4 = xgh4;
                zcosg = zcosgl;
                zsing = zsingl;
                zcosi = zcosil;
                zsini = zsinil;
                zcosh = zcoshl * cosq + zsinhl * sinq;
                zsinh = sinq * zcoshl - cosq * zsinhl;
                zn = ZNL;
                cc = C1L;
                ze = ZEL;
            }
        } // end of solar - lunar - solar terms computation

        sse += se;
        ssi += si;
        ssl += sl;
        ssg += sgh - ((tle.getI() < (FastMath.PI / 60.0)) ? 0 : (cosi0 / sini0 * sh));
        ssh += (tle.getI() < (FastMath.PI / 60.0)) ? 0 : sh / sini0;



        //        Start the resonant-synchronous tests and initialization

        double bfact = 0;

        // if mean motion is 1.893053 to 2.117652 revs/day, and eccentricity >= 0.5,
        // start of the 12-hour orbit, e > 0.5 section
        if ((xnq >= 0.00826) && (xnq <= 0.00924) && (tle.getE() >= 0.5)) {

            final double g201 = -0.306 - (tle.getE() - 0.64) * 0.440;
            final double eoc = tle.getE() * e0sq;
            final double sini2 = sini0 * sini0;
            final double f220 = 0.75 * (1 + 2 * cosi0 + theta2);
            final double f221 = 1.5 * sini2;
            final double f321 =  1.875 * sini0 * (1 - 2 * cosi0 - 3 * theta2);
            final double f322 = -1.875 * sini0 * (1 + 2 * cosi0 - 3 * theta2);
            final double f441 = 35 * sini2 * f220;
            final double f442 = 39.3750 * sini2 * sini2;
            final double f522 = 9.84375 * sini0 * (sini2 * (1 - 2 * cosi0 - 5 * theta2) +
                                                   0.33333333 * (-2 + 4 * cosi0 + 6 * theta2));
            final double f523 = sini0 * (4.92187512 * sini2 * (-2 - 4 * cosi0 + 10 * theta2) +
                                         6.56250012 * (1 + 2 * cosi0 - 3 * theta2));
            final double f542 = 29.53125 * sini0 * (2 - 8 * cosi0 + theta2 * (-12 + 8 * cosi0 + 10 * theta2));
            final double f543 = 29.53125 * sini0 * (-2 - 8 * cosi0 + theta2 * (12 + 8 * cosi0 - 10 * theta2));
            final double g211;
            final double g310;
            final double g322;
            final double g410;
            final double g422;
            final double g520;

            resonant = true;       // it is resonant...
            synchronous = false;     // but it's not synchronous

            // Geopotential resonance initialization for 12 hour orbits :
            if (tle.getE() <= 0.65) {
                g211 =    3.616  -   13.247  * tle.getE() +   16.290  * e0sq;
                g310 =  -19.302  +  117.390  * tle.getE() -  228.419  * e0sq +  156.591  * eoc;
                g322 =  -18.9068 +  109.7927 * tle.getE() -  214.6334 * e0sq +  146.5816 * eoc;
                g410 =  -41.122  +  242.694  * tle.getE() -  471.094  * e0sq +  313.953  * eoc;
                g422 = -146.407  +  841.880  * tle.getE() - 1629.014  * e0sq + 1083.435  * eoc;
                g520 = -532.114  + 3017.977  * tle.getE() - 5740.032  * e0sq + 3708.276  * eoc;
            } else  {
                g211 =   -72.099 +   331.819 * tle.getE() -   508.738 * e0sq +   266.724 * eoc;
                g310 =  -346.844 +  1582.851 * tle.getE() -  2415.925 * e0sq +  1246.113 * eoc;
                g322 =  -342.585 +  1554.908 * tle.getE() -  2366.899 * e0sq +  1215.972 * eoc;
                g410 = -1052.797 +  4758.686 * tle.getE() -  7193.992 * e0sq +  3651.957 * eoc;
                g422 = -3581.69  + 16178.11  * tle.getE() - 24462.77  * e0sq + 12422.52  * eoc;
                if (tle.getE() <= 0.715) {
                    g520 = 1464.74 - 4664.75 * tle.getE() + 3763.64 * e0sq;
                } else {
                    g520 = -5149.66 + 29936.92 * tle.getE() - 54087.36 * e0sq + 31324.56 * eoc;
                }
            }

            final double g533;
            final double g521;
            final double g532;
            if (tle.getE() < 0.7) {
                g533 = -919.2277  + 4988.61   * tle.getE() - 9064.77   * e0sq + 5542.21  * eoc;
                g521 = -822.71072 + 4568.6173 * tle.getE() - 8491.4146 * e0sq + 5337.524 * eoc;
                g532 = -853.666   + 4690.25   * tle.getE() - 8624.77   * e0sq + 5341.4   * eoc;
            } else {
                g533 = -37995.78  + 161616.52 * tle.getE() - 229838.2  * e0sq + 109377.94 * eoc;
                g521 = -51752.104 + 218913.95 * tle.getE() - 309468.16 * e0sq + 146349.42 * eoc;
                g532 = -40023.88  + 170470.89 * tle.getE() - 242699.48 * e0sq + 115605.82 * eoc;
            }

            double temp1 = 3 * xnq * xnq * aqnv * aqnv;
            double temp = temp1 * ROOT22;
            d2201 = temp * f220 * g201;
            d2211 = temp * f221 * g211;
            temp1 *= aqnv;
            temp = temp1 * ROOT32;
            d3210 = temp * f321 * g310;
            d3222 = temp * f322 * g322;
            temp1 *= aqnv;
            temp = 2 * temp1 * ROOT44;
            d4410 = temp * f441 * g410;
            d4422 = temp * f442 * g422;
            temp1 *= aqnv;
            temp = temp1 * ROOT52;
            d5220 = temp * f522 * g520;
            d5232 = temp * f523 * g532;
            temp = 2 * temp1 * ROOT54;
            d5421 = temp * f542 * g521;
            d5433 = temp * f543 * g533;
            xlamo = tle.getMeanAnomaly() + tle.getRaan() + tle.getRaan() - thgr - thgr;
            bfact = xmdot + xnodot + xnodot - THDT - THDT;
            bfact += ssl + ssh + ssh;
        } else if ((xnq < 0.0052359877) && (xnq > 0.0034906585)) {
            // if mean motion is .8 to 1.2 revs/day : (geosynch)

            final double cosio_plus_1 = 1.0 + cosi0;
            final double g200 = 1 + e0sq * (-2.5 + 0.8125  * e0sq);
            final double g300 = 1 + e0sq * (-6   + 6.60937 * e0sq);
            final double f311 = 0.9375 * sini0 * sini0 * (1 + 3 * cosi0) - 0.75 * cosio_plus_1;
            final double g310 = 1 + 2 * e0sq;
            final double f220 = 0.75 * cosio_plus_1 * cosio_plus_1;
            final double f330 = 2.5 * f220 * cosio_plus_1;

            resonant = true;
            synchronous = true;

            // Synchronous resonance terms initialization
            del1 = 3 * xnq * xnq * aqnv * aqnv;
            del2 = 2 * del1 * f220 * g200 * Q22;
            del3 = 3 * del1 * f330 * g300 * Q33 * aqnv;
            del1 = del1 * f311 * g310 * Q31 * aqnv;
            xlamo = tle.getMeanAnomaly() + tle.getRaan() + tle.getPerigeeArgument() - thgr;
            bfact = xmdot + omgdot + xnodot - THDT;
            bfact = bfact + ssl + ssg + ssh;
        } else {
            // it's neither a high-e 12-hours orbit nor a geosynchronous:
            resonant = false;
            synchronous = false;
        }

        if (resonant) {
            xfact = bfact - xnq;

            // Initialize integrator
            xli   = xlamo;
            xni   = xnq;
            atime = 0;
        }
        derivs = new double[SECULAR_INTEGRATION_ORDER];
    }

    /** Computes secular terms from current coordinates and epoch.
     * @param t offset from initial epoch (minutes)
     */
    protected void deepSecularEffects(final double t)  {

        xll    += ssl * t;
        omgadf += ssg * t;
        xnode  += ssh * t;
        em      = tle.getE() + sse * t;
        xinc    = tle.getI() + ssi * t;

        if (resonant) {
            // If we're closer to t = 0 than to the currently-stored data
            // from the previous call to this function,  then we're
            // better off "restarting",  going back to the initial data.
            // The Dundee code rigs things up to _always_ take 720-minute
            // steps from epoch to end time,  except for the final step.
            // Easiest way to arrange similar behavior in this code is
            // just to always do a restart,  if we're in Dundee-compliant
            // mode.
            if (FastMath.abs(t) < FastMath.abs(t - atime) || isDundeeCompliant)  {
                // Epoch restart
                atime = 0;
                xni = xnq;
                xli = xlamo;
            }
            boolean lastIntegrationStep = false;
            // if |step|>|step max| then do one step at step max
            while (!lastIntegrationStep) {
                double delt = t - atime;
                if (delt > SECULAR_INTEGRATION_STEP) {
                    delt = SECULAR_INTEGRATION_STEP;
                } else if (delt < -SECULAR_INTEGRATION_STEP) {
                    delt = -SECULAR_INTEGRATION_STEP;
                } else {
                    lastIntegrationStep = true;
                }

                computeSecularDerivs();

                final double xldot = xni + xfact;

                double xlpow = 1.;
                xli += delt * xldot;
                xni += delt * derivs[0];
                double delt_factor = delt;
                for (int j = 2; j <= SECULAR_INTEGRATION_ORDER; ++j) {
                    xlpow *= xldot;
                    derivs[j - 1] *= xlpow;
                    delt_factor *= delt / (double) j;
                    xli += delt_factor * derivs[j - 2];
                    xni += delt_factor * derivs[j - 1];
                }
                atime += delt;
            }
            xn = xni;
            final double temp = -xnode + thgr + t * THDT;
            xll = xli + temp + (synchronous ? -omgadf : temp);
        }
    }

    /** Computes periodic terms from current coordinates and epoch.
     * @param t offset from initial epoch (min)
     */
    protected void deepPeriodicEffects(final double t)  {

        // If the time didn't change by more than 30 minutes,
        // there's no good reason to recompute the perturbations;
        // they don't change enough over so short a time span.
        // However,  the Dundee code _always_ recomputes,  so if
        // we're attempting to replicate its results,  we've gotta
        // recompute everything,  too.
        if ((FastMath.abs(savtsn - t) >= 30.0) || isDundeeCompliant)  {

            savtsn = t;

            // Update solar perturbations for time T
            double zm = zmos + ZNS * t;
            double zf = zm + 2 * ZES * FastMath.sin(zm);
            double sinzf = FastMath.sin(zf);
            double f2 = 0.5 * sinzf * sinzf - 0.25;
            double f3 = -0.5 * sinzf * FastMath.cos(zf);
            final double ses = se2 * f2 + se3 * f3;
            final double sis = si2 * f2 + si3 * f3;
            final double sls = sl2 * f2 + sl3 * f3 + sl4 * sinzf;
            final double sghs = sgh2 * f2 + sgh3 * f3 + sgh4 * sinzf;
            final double shs = sh2 * f2 + sh3 * f3;

            // Update lunar perturbations for time T
            zm = zmol + ZNL * t;
            zf = zm + 2 * ZEL * FastMath.sin(zm);
            sinzf = FastMath.sin(zf);
            f2 =  0.5 * sinzf * sinzf - 0.25;
            f3 = -0.5 * sinzf * FastMath.cos(zf);
            final double sel = ee2 * f2 + e3 * f3;
            final double sil = xi2 * f2 + xi3 * f3;
            final double sll = xl2 * f2 + xl3 * f3 + xl4 * sinzf;
            final double sghl = xgh2 * f2 + xgh3 * f3 + xgh4 * sinzf;
            final double sh1 = xh2 * f2 + xh3 * f3;

            // Sum the solar and lunar contributions
            pe   = ses  + sel;
            pinc = sis  + sil;
            pl   = sls  + sll;
            pgh  = sghs + sghl;
            ph   = shs  + sh1;
        }

        xinc += pinc;

        final double sinis = FastMath.sin( xinc);
        final double cosis = FastMath.cos( xinc);

        /* Add solar/lunar perturbation correction to eccentricity: */
        em     += pe;
        xll    += pl;
        omgadf += pgh;
        xinc    = MathUtils.normalizeAngle(xinc, 0);

        if (FastMath.abs(xinc) >= 0.2) {
            // Apply periodics directly
            final double temp_val = ph / sinis;
            omgadf -= cosis * temp_val;
            xnode += temp_val;
        } else {
            // Apply periodics with Lyddane modification
            final double sinok = FastMath.sin(xnode);
            final double cosok = FastMath.cos(xnode);
            final double alfdp =  ph * cosok + (pinc * cosis + sinis) * sinok;
            final double betdp = -ph * sinok + (pinc * cosis + sinis) * cosok;
            final double delta_xnode = MathUtils.normalizeAngle(FastMath.atan2(alfdp, betdp) - xnode, 0);
            final double dls = -xnode * sinis * pinc;
            omgadf += dls - cosis * delta_xnode;
            xnode  += delta_xnode;
        }
    }

    /** Computes internal secular derivs. */
    private void computeSecularDerivs() {

        final double sin_li = FastMath.sin(xli);
        final double cos_li = FastMath.cos(xli);
        final double sin_2li = 2. * sin_li * cos_li;
        final double cos_2li = 2. * cos_li * cos_li - 1.;

        // Dot terms calculated :
        if (synchronous)  {
            final double sin_3li = sin_2li * cos_li + cos_2li * sin_li;
            final double cos_3li = cos_2li * cos_li - sin_2li * sin_li;
            double term1a = del1 * (sin_li  * C_FASX2  - cos_li  * S_FASX2);
            double term2a = del2 * (sin_2li * C_2FASX4 - cos_2li * S_2FASX4);
            double term3a = del3 * (sin_3li * C_3FASX6 - cos_3li * S_3FASX6);
            double term1b = del1 * (cos_li  * C_FASX2  + sin_li  * S_FASX2);
            double term2b = 2.0 * del2 * (cos_2li * C_2FASX4 + sin_2li * S_2FASX4);
            double term3b = 3.0 * del3 * (cos_3li * C_3FASX6 + sin_3li * S_3FASX6);

            for (int j = 0; j < SECULAR_INTEGRATION_ORDER; j += 2)  {
                derivs[j]     = term1a + term2a + term3a;
                derivs[j + 1] = term1b + term2b + term3b;
                if ((j + 2) < SECULAR_INTEGRATION_ORDER) {
                    term1a  = -term1a;
                    term2a *= -4.0;
                    term3a *= -9.0;
                    term1b = -term1b;
                    term2b *= -4.0;
                    term3b *= -9.0;
                }
            }
        } else {
            // orbit is a 12-hour resonant one
            final double xomi = omegaq + omgdot * atime;
            final double sin_omi = FastMath.sin(xomi);
            final double cos_omi = FastMath.cos(xomi);
            final double sin_li_m_omi = sin_li * cos_omi - sin_omi * cos_li;
            final double sin_li_p_omi = sin_li * cos_omi + sin_omi * cos_li;
            final double cos_li_m_omi = cos_li * cos_omi + sin_omi * sin_li;
            final double cos_li_p_omi = cos_li * cos_omi - sin_omi * sin_li;
            final double sin_2omi = 2. * sin_omi * cos_omi;
            final double cos_2omi = 2. * cos_omi * cos_omi - 1.;
            final double sin_2li_m_omi = sin_2li * cos_omi - sin_omi * cos_2li;
            final double sin_2li_p_omi = sin_2li * cos_omi + sin_omi * cos_2li;
            final double cos_2li_m_omi = cos_2li * cos_omi + sin_omi * sin_2li;
            final double cos_2li_p_omi = cos_2li * cos_omi - sin_omi * sin_2li;
            final double sin_2li_p_2omi = sin_2li * cos_2omi + sin_2omi * cos_2li;
            final double cos_2li_p_2omi = cos_2li * cos_2omi - sin_2omi * sin_2li;
            final double sin_2omi_p_li = sin_li * cos_2omi + sin_2omi * cos_li;
            final double cos_2omi_p_li = cos_li * cos_2omi - sin_2omi * sin_li;
            double term1a = d2201 * (sin_2omi_p_li * C_G22 - cos_2omi_p_li * S_G22) +
                            d2211 * (sin_li * C_G22 - cos_li * S_G22) +
                            d3210 * (sin_li_p_omi * C_G32 - cos_li_p_omi * S_G32) +
                            d3222 * (sin_li_m_omi * C_G32 - cos_li_m_omi * S_G32) +
                            d5220 * (sin_li_p_omi * C_G52 - cos_li_p_omi * S_G52) +
                            d5232 * (sin_li_m_omi * C_G52 - cos_li_m_omi * S_G52);
            double term2a = d4410 * (sin_2li_p_2omi * C_G44 - cos_2li_p_2omi * S_G44) +
                            d4422 * (sin_2li * C_G44 - cos_2li * S_G44) +
                            d5421 * (sin_2li_p_omi * C_G54 - cos_2li_p_omi * S_G54) +
                            d5433 * (sin_2li_m_omi * C_G54 - cos_2li_m_omi * S_G54);
            double term1b = d2201 * (cos_2omi_p_li * C_G22 + sin_2omi_p_li * S_G22) +
                            d2211 * (cos_li * C_G22 + sin_li * S_G22) +
                            d3210 * (cos_li_p_omi * C_G32 + sin_li_p_omi * S_G32) +
                            d3222 * (cos_li_m_omi * C_G32 + sin_li_m_omi * S_G32) +
                            d5220 * (cos_li_p_omi * C_G52 + sin_li_p_omi * S_G52) +
                            d5232 * (cos_li_m_omi * C_G52 + sin_li_m_omi * S_G52);
            double term2b = 2.0 * (d4410 * (cos_2li_p_2omi * C_G44 + sin_2li_p_2omi * S_G44) +
                                   d4422 * (cos_2li * C_G44 + sin_2li * S_G44) +
                                   d5421 * (cos_2li_p_omi * C_G54 + sin_2li_p_omi * S_G54) +
                                   d5433 * (cos_2li_m_omi * C_G54 + sin_2li_m_omi * S_G54));

            for (int j = 0; j < SECULAR_INTEGRATION_ORDER; j += 2) {
                derivs[j]     = term1a + term2a;
                derivs[j + 1] = term1b + term2b;
                if ((j + 2) < SECULAR_INTEGRATION_ORDER)  {
                    term1a  = -term1a;
                    term2a *= -4.0;
                    term1b  = -term1b;
                    term2b *= -4.0;
                }
            }
        }
    }

}
