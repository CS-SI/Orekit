/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.hipparchus.RealFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.time.DateTimeComponents;
import org.orekit.utils.Constants;


/** This class contains the methods that compute deep space perturbation terms.
 * <p>
 * The user should not bother in this class since it is handled internaly by the
 * {@link TLEPropagator}.
 * </p>
 * <p>This implementation is largely inspired from the paper and source code <a
 * href="https://www.celestrak.com/publications/AIAA/2006-6753/">Revisiting Spacetrack
 * Report #3</a> and is fully compliant with its results and tests cases.</p>
 * @author Felix R. Hoots, Ronald L. Roehrich, December 1980 (original fortran)
 * @author David A. Vallado, Paul Crawford, Richard Hujsak, T.S. Kelso (C++ translation and improvements)
 * @author Fabien Maussion (java translation)
 */
public class FieldDeepSDP4<T extends RealFieldElement<T>> extends FieldSDP4<T> {

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

    /** Intermediate values. */
    private double thgr;
    private T xnq;
    private T omegaq;
    private double zcosil;
    private double zsinil;
    private double zsinhl;
    private double zcoshl;
    private double zmol;
    private double zcosgl;
    private double zsingl;
    private double zmos;
    private double savtsn;

    private T ee2;
    private T e3;
    private T xi2;
    private T xi3;
    private T xl2;
    private T xl3;
    private T xl4;
    private T xgh2;
    private T xgh3;
    private T xgh4;
    private T xh2;
    private T xh3;

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

    private T sse;
    private T ssi;
    private T ssl;
    private T ssh;
    private T ssg;
    private T se2;
    private T si2;
    private T sl2;
    private T sgh2;
    private T sh2;
    private T se3;
    private T si3;
    private T sl3;
    private T sgh3;
    private T sh3;
    private T sl4;
    private T sgh4;

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
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param initialTLE the TLE to propagate.
     * @param attitudeProvider provider for attitude computation
     * @param mass spacecraft mass (kg)
     * @see #DeepSDP4(TLE, AttitudeProvider, double, Frame)
     */
    @DefaultDataContext
    public FieldDeepSDP4(final FieldTLE<T> initialTLE, final AttitudeProvider attitudeProvider,
                    final T mass) {
        this(initialTLE, attitudeProvider, mass,
                DataContext.getDefault().getFrames().getTEME());
    }

    /** Constructor for a unique initial TLE.
     * @param initialTLE the TLE to propagate.
     * @param attitudeProvider provider for attitude computation
     * @param mass spacecraft mass (kg)
     * @param teme the TEME frame to use for propagation.
     * @since 10.1
     */
    public FieldDeepSDP4(final FieldTLE<T> initialTLE,
                    final AttitudeProvider attitudeProvider,
                    final T mass,
                    final Frame teme) {
        super(initialTLE, attitudeProvider, mass, teme);
    }

    /** Computes luni - solar terms from initial coordinates and epoch.
     */
    protected void luniSolarTermsComputation() {

        final T sing = FastMath.sin(tle.getPerigeeArgument());
        final T cosg = FastMath.cos(tle.getPerigeeArgument());

        final T sinq = FastMath.sin(tle.getRaan());
        final T cosq = FastMath.cos(tle.getRaan());
        final T aqnv = a0dp.reciprocal();

        // Compute julian days since 1900
        final double daysSince1900 = (tle.getDate()
                .getComponents(utc)
                .offsetFrom(DateTimeComponents.JULIAN_EPOCH)) /
                Constants.JULIAN_DAY - 2415020;

        double cc = C1SS;
        double ze = ZES;
        double zn = ZNS;
        T zsinh = sinq;
        T zcosh = cosq;

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

        T se = xnq.getField().getZero();
        T sgh = xnq.getField().getZero();
        T sh = xnq.getField().getZero();
        T si = xnq.getField().getZero();
        T sl = xnq.getField().getZero();

        // There was previously some convoluted logic here, but it boils
        // down to this:  we compute the solar terms,  then the lunar terms.
        // On a second pass,  we recompute the solar terms, taking advantage
        // of the improved data that resulted from computing lunar terms.
        for (int iteration = 0; iteration < 2; ++iteration) {
            final T a1  = zcosh.multiply(zcosg).add(zsinh.multiply(zsing).multiply(zcosi));
            final T a3  = zcosh.multiply(-zsing).add(zsinh.multiply(zcosg).multiply(zcosi));
            final T a7  = zsinh.multiply(-zcosg).add(zcosh.multiply(zcosi).multiply(zsing));
            final double a8 = zsing * zsini;
            final T a9  = zsinh.multiply(zsing).add(zcosh.multiply(zcosi).multiply(zcosg));
            final double a10 = zcosg * zsini;
            final T a2  = cosi0.multiply(a7).add(sini0.multiply(a8));
            final T a4  = cosi0.multiply(a9).add(sini0.multiply(a10));
            final T a5  = sini0.negate().multiply(a7).add(cosi0.multiply(a8));
            final T a6  = sini0.negate().multiply(a9).add(cosi0.multiply(a10));
            final T x1  = a1.multiply(cosg).add(a2.multiply(sing));
            final T x2  = a3.multiply(cosg).add(a4.multiply(sing));
            final T x3  = a1.negate().multiply(sing).add(a2.multiply(cosg));
            final T x4  = a3.negate().multiply(sing).add(a4.multiply(cosg));
            final T x5  = a5.multiply(sing);
            final T x6  = a6.multiply(sing);
            final T x7  = a5.multiply(cosg);
            final T x8  = a6.multiply(cosg);
            final T z31 = x1.multiply(x1).multiply(12).subtract(x3.multiply(x3).multiply(3));
            final T z32 = x1.multiply(x2).multiply(24).subtract(x3.multiply(x4).multiply(6));
            final T z33 = x2.multiply(x2).multiply(12).subtract(x4.multiply(x4).multiply(3));
            final T z11 = a1.multiply(-6).multiply(a5).multiply(e0sq).multiply(x1.multiply(x7).multiply(-24).subtract(x3.multiply(x5).multiply(6)));
            final T z12 = a1.multiply(a6).add(a3.multiply(a5)).multiply(-6).add(
                               e0sq.multiply(x2.multiply(x7).add(x1.multiply(x8))).multiply(-24).subtract(
                               x3.multiply(x6).add(x4.multiply(x5)).multiply(6)));
            final T z13 = a3.multiply(a6).multiply(-6).add(e0sq.multiply(
                               x2.multiply(x8).multiply(-24).subtract(x4.multiply(x6).multiply(6))));
            final T z21 = a2.multiply(a5).multiply(6).add(e0sq.multiply(
                               x1.multiply(x5).multiply(24).subtract(x3.multiply(x7).multiply(6))));
            final T z22 = a4.multiply(a5).add(a2.multiply(a6)).multiply(6).add(
                               e0sq.multiply(x2.multiply(x5).add(x1.multiply(x6)).multiply(24).subtract(
                               x4.multiply(x7).add(x3.multiply(x8)).multiply(6))));
            final T z23 = a4.multiply(a6).multiply(6).add(e0sq.multiply(x2.multiply(x6).multiply(24).subtract(x4.multiply(x8).multiply(6))));
            final T s3  = xnq.reciprocal().multiply(cc);
            final T s2  = beta0.reciprocal().multiply(s3.multiply(-0.5));
            final T s4  = s3.multiply(beta0);
            final T s1  = tle.getE().multiply(s4).multiply(-15);
            final T s5  = x1.multiply(x3).add(x2.multiply(x4));
            final T s6  = x2.multiply(x3).add(x1.multiply(x4));
            final T s7  = x2.multiply(x4).subtract(x1.multiply(x3));
            T z1 = a1.multiply(a1).add(a2.multiply(a2)).multiply(3).add(z31.multiply(e0sq));
            T z2 = a1.multiply(a3).add(a2.multiply(a4)).multiply(6).add(z32.multiply(e0sq));
            T z3 = a3.multiply(a3).add(a4.multiply(a4)).multiply(3).add(z33.multiply(e0sq));

            z1 = z1.add(z1).add(beta02.multiply(z31));
            z2 = z2.add(z2).add(beta02.multiply(z32));
            z3 = z3.add(z3).add(beta02.multiply(z33));
            se = s1.multiply(zn).multiply(s5);
            si = s2.multiply(zn).multiply(z11.add(z13));
            sl = s3.multiply(-zn).multiply(z1.add(z3).subtract(14).subtract(e0sq.multiply(6)));
            sgh = s4.multiply(zn).multiply(z31.add(z33).subtract(6));
            if (tle.getI().getReal() < (FastMath.PI / 60.0)) {
                // inclination smaller than 3 degrees
                sh = xnq.getField().getZero();
            } else {
                sh = s2.multiply(-zn).multiply(z21.add(z23));
            }
            ee2  = s1.multiply(s6).multiply(2);
            e3   = s1.multiply(s7).multiply(2);
            xi2  = s2.multiply(z12).multiply(2);
            xi3  = s2.multiply(z13.subtract(z11)).multiply(2);
            xl2  = s3.multiply(z2).multiply(-2);
            xl3  = s3.multiply(z3.subtract(z1)).multiply(-2);
            xl4  = s3.multiply(e0sq.multiply(-9).add(-21)).multiply(ze).multiply(-2);
            xgh2 = s4.multiply(z32).multiply(2);
            xgh3 = s4.multiply(z33.subtract(z31)).multiply(2);
            xgh4 = s4.multiply(ze).multiply(-18);
            xh2  = s2.multiply(z22).multiply(-2);
            xh3  = s2.multiply(z23.subtract(z21)).multiply(-2);

            if (iteration == 0) { // we compute lunar terms only on the first pass:
                sse = se;
                ssi = si;
                ssl = sl;
                ssh = (tle.getI().getReal() < (FastMath.PI / 60.0)) ? xnq.getField().getZero() : sh.divide(sini0);
                ssg = sgh.subtract(cosi0.multiply(ssh));
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
                zcosh = cosq.multiply(zcoshl).add(sinq.multiply(zsinhl));
                zsinh = sinq.multiply(zcoshl).subtract(cosq.multiply(zsinhl));
                zn = ZNL;
                cc = C1L;
                ze = ZEL;
            }
        } // end of solar - lunar - solar terms computation

        sse = sse.add(se);
        ssi = si.add(si);
        ssl = ssl.add(sl);
        ssg = ssg.add(sgh).subtract((tle.getI().getReal() < (FastMath.PI / 60.0)) ? xnq.getField().getZero() : (cosi0.divide(sini0).multiply(sh)));
        ssh = ssh.add((tle.getI().getReal() < (FastMath.PI / 60.0)) ? xnq.getField().getZero() : sh.divide(sini0));



        //        Start the resonant-synchronous tests and initialization

        double bfact = 0;

        // if mean motion is 1.893053 to 2.117652 revs/day, and eccentricity >= 0.5,
        // start of the 12-hour orbit, e > 0.5 section
        if ((xnq.getReal() >= 0.00826) && (xnq.getReal() <= 0.00924) && (tle.getE().getReal() >= 0.5)) {

            final T g201  = tle.getE().subtract(0.64).negate().multiply(0.440).add(-0.306);
            final T eoc   = tle.getE().multiply(e0sq);
            final T sini2 = sini0.multiply(sini0);
            final T f220  = cosi0.multiply(2).add(theta2).add(1).multiply(0.75);
            final T f221  = sini2.multiply(1.5);
            final T f321  = sini0.multiply(1.875).multiply(cosi0.multiply(2).negate().subtract(theta2.multiply(3)).add(1));
            final T f322  = sini0.multiply(-1.875).multiply(cosi0.multiply(2).subtract(theta2.multiply(3)).add(1));
            final T f441  = sini2.multiply(f220).multiply(35);
            final T f442  = sini2.multiply(sini2).multiply(39.3750);
            final T f522  = sini0.multiply(sini2.multiply(cosi0.multiply(2).negate().subtract(theta2.multiply(5)).add(1)).add(
                                    cosi0.multiply(4).add(theta2.multiply(6)).add(-2).multiply(0.33333333))).multiply(9.84359);
            final T f523  = sini0.multiply(sini2.multiply(cosi0.multiply(-4).add(theta2.multiply(10)).add(-2)).multiply(4.92187512).add(
                                    cosi0.multiply(2).subtract(theta2.multiply(3)).add(1).multiply(6.56250012)));
            final T f542  = sini0.multiply(29.53125).multiply(cosi0.multiply(-8).add(2).add(
                                    theta2.multiply(cosi0.multiply(8).add(theta2.multiply(10)).add(-12))));
            final T f543  = sini0.multiply(29.53125).multiply(cosi0.multiply(-8).add(-2).add(
                                    theta2.multiply(cosi0.multiply(8).subtract(theta2.multiply(10)).add(12))));
            final T g211;
            final T g310;
            final T g322;
            final T g410;
            final T g422;
            final T g520;

            resonant = true;       // it is resonant...
            synchronous = false;     // but it's not synchronous

            // Geopotential resonance initialization for 12 hour orbits :
            if (tle.getE().getReal() <= 0.65) {
                g211 = tle.getE().multiply( -13.247).add(  e0sq.multiply(   16.290)).add(                                  3.616);
                g310 = tle.getE().multiply( 117.390).add(  e0sq.multiply( -228.419)).add(  eoc.multiply( 156.591)).add(  -19.302);
                g322 = tle.getE().multiply(109.7927).add(  e0sq.multiply(-214.6334)).add(  eoc.multiply(146.5816)).add( -18.9068);
                g410 = tle.getE().multiply( 242.694).add(  e0sq.multiply( -471.094)).add(  eoc.multiply( 313.953)).add(  -41.122);
                g422 = tle.getE().multiply( 841.880).add(  e0sq.multiply(-1629.014)).add(  eoc.multiply(1083.435)).add( -146.407);
                g520 = tle.getE().multiply(3017.977).add(  e0sq.multiply(-5740.032)).add(  eoc.multiply(3708.276)).add( -532.114);
            } else  {
                g211 = tle.getE().multiply( 331.819).add(  e0sq.multiply( -508.738)).add(  eoc.multiply( 266.724)).add(  -72.099);
                g310 = tle.getE().multiply(1582.851).add(  e0sq.multiply(-2415.925)).add(  eoc.multiply(1246.113)).add( -346.844);
                g322 = tle.getE().multiply(1554.908).add(  e0sq.multiply(-2366.899)).add(  eoc.multiply(1215.972)).add( -342.585);
                g410 = tle.getE().multiply(4758.686).add(  e0sq.multiply(-7193.992)).add(  eoc.multiply(3651.957)).add(-1052.797);
                g422 = tle.getE().multiply(16178.11).add(  e0sq.multiply(-24462.77)).add(  eoc.multiply(12422.52)).add( -3581.69);
                if (tle.getE().getReal() <= 0.715) {
                    g520 = tle.getE().multiply(-4664.75).add(  e0sq.multiply(  3763.64)).add(                                1464.74);
                } else {
                    g520 = tle.getE().multiply(29936.92).add(  e0sq.multiply(-54087.36)).add(  eoc.multiply(31324.56)).add( -5149.66);
                }
            }

            final T g533;
            final T g521;
            final T g532;
            if (tle.getE().getReal() < 0.7) {
                g533 = tle.getE().multiply(  4988.61).add(  e0sq.multiply(  -9064.77)).add(  eoc.multiply( 5542.21)).add(   -919.2277);
                g521 = tle.getE().multiply(4568.6173).add(  e0sq.multiply(-8491.4146)).add(  eoc.multiply( 5337.524)).add( -822.71072);
                g532 = tle.getE().multiply(  4690.25).add(  e0sq.multiply(  -8624.77)).add(  eoc.multiply(   5341.4)).add(   -853.666);
            } else {
                g533 = tle.getE().multiply(161616.52).add(  e0sq.multiply(-229838.2)).add(   eoc.multiply(109377.94)).add(  -37995.78);
                g521 = tle.getE().multiply(218913.95).add(  e0sq.multiply(-309468.16)).add(  eoc.multiply(146349.42)).add( -51752.104);
                g532 = tle.getE().multiply(170470.89).add(  e0sq.multiply(-242699.48)).add(  eoc.multiply(115605.82)).add(  -40023.88);
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
        derivs = new double[2];
    }

    /** Computes secular terms from current coordinates and epoch.
     * @param t offset from initial epoch (minutes)
     */
    protected void deepSecularEffects(final T t)  {

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
                xlpow *= xldot;
                derivs[1] *= xlpow;
                delt_factor *= delt / 2;
                xli += delt_factor * derivs[0];
                xni += delt_factor * derivs[1];
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
    protected void deepPeriodicEffects(final T t)  {

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
            final double term1a = del1 * (sin_li  * C_FASX2  - cos_li  * S_FASX2);
            final double term2a = del2 * (sin_2li * C_2FASX4 - cos_2li * S_2FASX4);
            final double term3a = del3 * (sin_3li * C_3FASX6 - cos_3li * S_3FASX6);
            final double term1b = del1 * (cos_li  * C_FASX2  + sin_li  * S_FASX2);
            final double term2b = 2.0 * del2 * (cos_2li * C_2FASX4 + sin_2li * S_2FASX4);
            final double term3b = 3.0 * del3 * (cos_3li * C_3FASX6 + sin_3li * S_3FASX6);
            derivs[0] = term1a + term2a + term3a;
            derivs[1] = term1b + term2b + term3b;
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
            final double term1a = d2201 * (sin_2omi_p_li * C_G22 - cos_2omi_p_li * S_G22) +
                                  d2211 * (sin_li * C_G22 - cos_li * S_G22) +
                                  d3210 * (sin_li_p_omi * C_G32 - cos_li_p_omi * S_G32) +
                                  d3222 * (sin_li_m_omi * C_G32 - cos_li_m_omi * S_G32) +
                                  d5220 * (sin_li_p_omi * C_G52 - cos_li_p_omi * S_G52) +
                                  d5232 * (sin_li_m_omi * C_G52 - cos_li_m_omi * S_G52);
            final double term2a = d4410 * (sin_2li_p_2omi * C_G44 - cos_2li_p_2omi * S_G44) +
                                  d4422 * (sin_2li * C_G44 - cos_2li * S_G44) +
                                  d5421 * (sin_2li_p_omi * C_G54 - cos_2li_p_omi * S_G54) +
                                  d5433 * (sin_2li_m_omi * C_G54 - cos_2li_m_omi * S_G54);
            final double term1b = d2201 * (cos_2omi_p_li * C_G22 + sin_2omi_p_li * S_G22) +
                                  d2211 * (cos_li * C_G22 + sin_li * S_G22) +
                                  d3210 * (cos_li_p_omi * C_G32 + sin_li_p_omi * S_G32) +
                                  d3222 * (cos_li_m_omi * C_G32 + sin_li_m_omi * S_G32) +
                                  d5220 * (cos_li_p_omi * C_G52 + sin_li_p_omi * S_G52) +
                                  d5232 * (cos_li_m_omi * C_G52 + sin_li_m_omi * S_G52);
            final double term2b = 2.0 * (d4410 * (cos_2li_p_2omi * C_G44 + sin_2li_p_2omi * S_G44) +
                                         d4422 * (cos_2li * C_G44 + sin_2li * S_G44) +
                                         d5421 * (cos_2li_p_omi * C_G54 + sin_2li_p_omi * S_G54) +
                                         d5433 * (cos_2li_m_omi * C_G54 + sin_2li_m_omi * S_G54));

            derivs[0] = term1a + term2a;
            derivs[1] = term1b + term2b;

        }
    }

}
