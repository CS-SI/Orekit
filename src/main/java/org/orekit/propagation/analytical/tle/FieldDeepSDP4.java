/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
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
 * @author Thomas Paulet (field translation)
 * @since 11.0
 * @param <T> type of the field elements
 */
public class FieldDeepSDP4<T extends CalculusFieldElement<T>> extends FieldSDP4<T> {

    // CHECKSTYLE: stop JavadocVariable check

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
    private T savtsn;

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

    private T d2201;
    private T d2211;
    private T d3210;
    private T d3222;
    private T d4410;
    private T d4422;
    private T d5220;
    private T d5232;
    private T d5421;
    private T d5433;
    private T xlamo;

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

    private T del1;
    private T del2;
    private T del3;
    private T xfact;
    private T xli;
    private T xni;
    private T atime;

    private T pe;
    private T pinc;
    private T pl;
    private T pgh;
    private T ph;

    private T[] derivs;

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
     * @param parameters SGP4 and SDP4 model parameters
     * @see #FieldDeepSDP4(FieldTLE, AttitudeProvider, CalculusFieldElement, Frame, CalculusFieldElement[])
     */
    @DefaultDataContext
    public FieldDeepSDP4(final FieldTLE<T> initialTLE, final AttitudeProvider attitudeProvider,
                    final T mass, final T[] parameters) {
        this(initialTLE, attitudeProvider, mass,
                DataContext.getDefault().getFrames().getTEME(), parameters);
    }

    /** Constructor for a unique initial TLE.
     * @param initialTLE the TLE to propagate.
     * @param attitudeProvider provider for attitude computation
     * @param mass spacecraft mass (kg)
     * @param teme the TEME frame to use for propagation.
     * @param parameters SGP4 and SDP4 model parameters
     */
    public FieldDeepSDP4(final FieldTLE<T> initialTLE,
                         final AttitudeProvider attitudeProvider,
                         final T mass,
                         final Frame teme,
                         final T[] parameters) {
        super(initialTLE, attitudeProvider, mass, teme, parameters);
    }

    /** Computes luni - solar terms from initial coordinates and epoch.
     */
    protected void luniSolarTermsComputation() {

        final T zero = tle.getPerigeeArgument().getField().getZero();
        final T pi   = zero.getPi();

        final FieldSinCos<T> scg  = FastMath.sinCos(tle.getPerigeeArgument());
        final T sing = scg.sin();
        final T cosg = scg.cos();

        final FieldSinCos<T> scq  = FastMath.sinCos(tle.getRaan());
        final T sinq = scq.sin();
        final T cosq = scq.cos();
        final T aqnv = a0dp.reciprocal();

        // Compute julian days since 1900
        final double daysSince1900 = (tle.getDate()
                .getComponents(utc)
                .offsetFrom(DateTimeComponents.JULIAN_EPOCH)) /
                Constants.JULIAN_DAY - 2415020;

        double cc = TLEConstants.C1SS;
        double ze = TLEConstants.ZES;
        double zn = TLEConstants.ZNS;
        T zsinh = sinq;
        T zcosh = cosq;

        thgr = thetaG(tle.getDate());
        xnq = xn0dp;
        omegaq = tle.getPerigeeArgument();

        final double xnodce = 4.5236020 - 9.2422029e-4 * daysSince1900;
        final SinCos scTem  = FastMath.sinCos(xnodce);
        final double stem = scTem.sin();
        final double ctem = scTem.cos();
        final double c_minus_gam = 0.228027132 * daysSince1900 - 1.1151842;
        final double gam = 5.8351514 + 0.0019443680 * daysSince1900;

        zcosil = 0.91375164 - 0.03568096 * ctem;
        zsinil = FastMath.sqrt(1.0 - zcosil * zcosil);
        zsinhl = 0.089683511 * stem / zsinil;
        zcoshl = FastMath.sqrt(1.0 - zsinhl * zsinhl);
        zmol = MathUtils.normalizeAngle(c_minus_gam, pi.getReal());

        double zx = 0.39785416 * stem / zsinil;
        final double zy = zcoshl * ctem + 0.91744867 * zsinhl * stem;
        zx = FastMath.atan2( zx, zy) + gam - xnodce;
        final SinCos scZx = FastMath.sinCos(zx);
        zcosgl = scZx.cos();
        zsingl = scZx.sin();
        zmos = MathUtils.normalizeAngle(6.2565837 + 0.017201977 * daysSince1900, pi.getReal());

        // Do solar terms
        savtsn = zero.add(1e20);

        T zcosi = zero.add(0.91744867);
        T zsini = zero.add(0.39785416);
        T zsing = zero.add(-0.98088458);
        T zcosg = zero.add(0.1945905);

        T se =  zero;
        T sgh = zero;
        T sh =  zero;
        T si =  zero;
        T sl =  zero;

        // There was previously some convoluted logic here, but it boils
        // down to this:  we compute the solar terms,  then the lunar terms.
        // On a second pass,  we recompute the solar terms, taking advantage
        // of the improved data that resulted from computing lunar terms.
        for (int iteration = 0; iteration < 2; ++iteration) {
            final T a1  = zcosh.multiply(zcosg).add(zsinh.multiply(zsing).multiply(zcosi));
            final T a3  = zcosh.multiply(zsing.negate()).add(zsinh.multiply(zcosg).multiply(zcosi));
            final T a7  = zsinh.negate().multiply(zcosg).add(zcosh.multiply(zcosi).multiply(zsing));
            final T a8  = zsing.multiply(zsini);
            final T a9  = zsinh.multiply(zsing).add(zcosh.multiply(zcosi).multiply(zcosg));
            final T a10 = zcosg.multiply(zsini);
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
            final T z11 = a1.multiply(-6).multiply(a5).add(e0sq.multiply(x1.multiply(x7).multiply(-24).add(x3.multiply(x5).multiply(-6))));
            final T z12 = a1.multiply(a6).add(a3.multiply(a5)).multiply(-6).add(
                                e0sq.multiply(x2.multiply(x7).add(x1.multiply(x8)).multiply(-24).add(
                                x3.multiply(x6).add(x4.multiply(x5)).multiply(-6))));
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
            if (tle.getI().getReal() < pi.divide(60.0).getReal()) {
                // inclination smaller than 3 degrees
                sh = zero;
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
                ssh = (tle.getI().getReal() < pi.divide(60.0).getReal()) ? zero : sh.divide(sini0);
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
                zcosg = zero.add(zcosgl);
                zsing = zero.add(zsingl);
                zcosi = zero.add(zcosil);
                zsini = zero.add(zsinil);
                zcosh = cosq.multiply(zcoshl).add(sinq.multiply(zsinhl));
                zsinh = sinq.multiply(zcoshl).subtract(cosq.multiply(zsinhl));
                zn = TLEConstants.ZNL;
                cc = TLEConstants.C1L;
                ze = TLEConstants.ZEL;
            }
        } // end of solar - lunar - solar terms computation

        sse = sse.add(se);
        ssi = ssi.add(si);
        ssl = ssl.add(sl);
        ssg = ssg.add(sgh).subtract((tle.getI().getReal() < pi.divide(60.0).getReal()) ? zero : (cosi0.divide(sini0).multiply(sh)));
        ssh = ssh.add((tle.getI().getReal() < pi.divide(60.0).getReal()) ? zero : sh.divide(sini0));



        //        Start the resonant-synchronous tests and initialization

        T bfact = zero;

        // if mean motion is 1.893053 to 2.117652 revs/day, and eccentricity >= 0.5,
        // start of the 12-hour orbit, e > 0.5 section
        if (xnq.getReal() >= 0.00826 && xnq.getReal() <= 0.00924 && tle.getE().getReal() >= 0.5) {

            final T g201  = tle.getE().subtract(0.64).negate().multiply(0.440).add(-0.306);
            final T eoc   = tle.getE().multiply(e0sq);
            final T sini2 = sini0.multiply(sini0);
            final T f220  = cosi0.multiply(2).add(theta2).add(1).multiply(0.75);
            final T f221  = sini2.multiply(1.5);
            final T f321  = sini0.multiply(1.875).multiply(cosi0.multiply(2).negate().subtract(theta2.multiply(3)).add(1));
            final T f322  = sini0.multiply(-1.875).multiply(cosi0.multiply(2).subtract(theta2.multiply(3)).add(1));
            final T f441  = sini2.multiply(f220).multiply(35);
            final T f442  = sini2.multiply(sini2).multiply(39.3750);
            final T f522  = sini0.multiply(9.84375).multiply(sini2.multiply(cosi0.multiply(-2).add(theta2.multiply(-5)).add(1.0)).add(
                                    cosi0.multiply(4.0).add(theta2.multiply(6.0)).add(-2).multiply(0.33333333)));
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
                g533 = tle.getE().multiply(  4988.61).add(  e0sq.multiply(  -9064.77)).add(  eoc.multiply(  5542.21)).add(  -919.2277);
                g521 = tle.getE().multiply(4568.6173).add(  e0sq.multiply(-8491.4146)).add(  eoc.multiply( 5337.524)).add( -822.71072);
                g532 = tle.getE().multiply(  4690.25).add(  e0sq.multiply(  -8624.77)).add(  eoc.multiply(   5341.4)).add(   -853.666);
            } else {
                g533 = tle.getE().multiply(161616.52).add(  e0sq.multiply( -229838.2)).add(  eoc.multiply(109377.94)).add(  -37995.78);
                g521 = tle.getE().multiply(218913.95).add(  e0sq.multiply(-309468.16)).add(  eoc.multiply(146349.42)).add( -51752.104);
                g532 = tle.getE().multiply(170470.89).add(  e0sq.multiply(-242699.48)).add(  eoc.multiply(115605.82)).add(  -40023.88);
            }

            T temp1 = xnq.multiply(xnq).multiply(aqnv).multiply(aqnv).multiply(3);
            T temp  = temp1.multiply(TLEConstants.ROOT22);
            d2201   = temp.multiply(f220).multiply(g201);
            d2211   = temp.multiply(f221).multiply(g211);
            temp1   = temp1.multiply(aqnv);
            temp    = temp1.multiply(TLEConstants.ROOT32);
            d3210   = temp.multiply(f321).multiply(g310);
            d3222   = temp.multiply(f322).multiply(g322);
            temp1   = temp1.multiply(aqnv);
            temp    = temp1.multiply(2 * TLEConstants.ROOT44);
            d4410   = temp.multiply(f441).multiply(g410);
            d4422   = temp.multiply(f442).multiply(g422);
            temp1   = temp1.multiply(aqnv);
            temp    = temp1.multiply(TLEConstants.ROOT52);
            d5220   = temp.multiply(f522).multiply(g520);
            d5232   = temp.multiply(f523).multiply(g532);
            temp    = temp1.multiply(2 * TLEConstants.ROOT54);
            d5421   = temp.multiply(f542).multiply(g521);
            d5433   = temp.multiply(f543).multiply(g533);
            xlamo   = tle.getMeanAnomaly().add(tle.getRaan()).add(tle.getRaan()).subtract(thgr + thgr);
            bfact   = xmdot.add(xnodot).add(xnodot).subtract(TLEConstants.THDT + TLEConstants.THDT);
            bfact   = bfact.add(ssl).add(ssh).add(ssh);
        } else if (xnq.getReal() < 0.0052359877 && xnq.getReal() > 0.0034906585) {
            // if mean motion is .8 to 1.2 revs/day : (geosynch)

            final T cosio_plus_1 = cosi0.add(1.0);
            final T g200 = e0sq.multiply(e0sq.multiply(0.8125).add(-2.5)).add(1);
            final T g300 = e0sq.multiply(e0sq.multiply(6.60937).add(-6)).add(1);
            final T f311 = sini0.multiply(0.9375).multiply(sini0.multiply(cosi0.multiply(3).add(1))).subtract(cosio_plus_1.multiply(0.75));
            final T g310 = e0sq.multiply(2).add(1);
            final T f220 = cosio_plus_1.multiply(cosio_plus_1).multiply(0.75);
            final T f330 = f220.multiply(cosio_plus_1).multiply(2.5);

            resonant = true;
            synchronous = true;

            // Synchronous resonance terms initialization
            del1 = xnq.multiply(xnq).multiply(aqnv).multiply(aqnv).multiply(3);
            del2 = del1.multiply(f220).multiply(g200).multiply(2 * TLEConstants.Q22);
            del3 = del1.multiply(f330).multiply(g300).multiply(aqnv).multiply(3 * TLEConstants.Q33);
            del1 = del1.multiply(f311).multiply(g310).multiply(TLEConstants.Q31).multiply(aqnv);
            xlamo = tle.getMeanAnomaly().add(tle.getRaan()).add(tle.getPerigeeArgument()).subtract(thgr);
            bfact = xmdot.add(omgdot).add(xnodot).subtract(TLEConstants.THDT);
            bfact = bfact.add(ssl).add(ssg).add(ssh);
        } else {
            // it's neither a high-e 12-hours orbit nor a geosynchronous:
            resonant = false;
            synchronous = false;
        }

        if (resonant) {
            xfact = bfact.subtract(xnq);

            // Initialize integrator
            xli   = xlamo;
            xni   = xnq;
            atime = zero;
        }
        derivs = MathArrays.buildArray(xnq.getField(), 2);
    }

    /** Computes secular terms from current coordinates and epoch.
     * @param t offset from initial epoch (minutes)
     */
    protected void deepSecularEffects(final T t)  {

        xll     = xll.add(ssl.multiply(t));
        omgadf  = omgadf.add(ssg.multiply(t));
        xnode   = xnode.add(ssh.multiply(t));
        em      = tle.getE().add(sse.multiply(t));
        xinc    = tle.getI().add(ssi.multiply(t));

        if (resonant) {
            // If we're closer to t = 0 than to the currently-stored data
            // from the previous call to this function,  then we're
            // better off "restarting",  going back to the initial data.
            // The Dundee code rigs things up to _always_ take 720-minute
            // steps from epoch to end time,  except for the final step.
            // Easiest way to arrange similar behavior in this code is
            // just to always do a restart,  if we're in Dundee-compliant
            // mode.
            if (FastMath.abs(t).getReal() < FastMath.abs(t.subtract(atime)).getReal() || isDundeeCompliant)  {
                // Epoch restart
                atime = t.getField().getZero();
                xni = xnq;
                xli = xlamo;
            }
            boolean lastIntegrationStep = false;
            // if |step|>|step max| then do one step at step max
            while (!lastIntegrationStep) {
                double delt = t.subtract(atime).getReal();
                if (delt > SECULAR_INTEGRATION_STEP) {
                    delt = SECULAR_INTEGRATION_STEP;
                } else if (delt < -SECULAR_INTEGRATION_STEP) {
                    delt = -SECULAR_INTEGRATION_STEP;
                } else {
                    lastIntegrationStep = true;
                }

                computeSecularDerivs();

                final T xldot = xni.add(xfact);

                T xlpow = t.getField().getZero().add(1.);
                xli = xli.add(xldot.multiply(delt));
                xni = xni.add(derivs[0].multiply(delt));
                double delt_factor = delt;
                xlpow = xlpow.multiply(xldot);
                derivs[1] = derivs[1].multiply(xlpow);
                delt_factor *= delt / 2;
                xli = xli.add(derivs[0].multiply(delt_factor));
                xni = xni.add(derivs[1].multiply(delt_factor));
                atime = atime.add(delt);
            }
            xn = xni;
            final T temp = xnode.negate().add(thgr).add(t.multiply(TLEConstants.THDT));
            xll = xli.add(temp).add(synchronous ? omgadf.negate() : temp);
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
        if (FastMath.abs(savtsn.subtract(t).getReal()) >= 30.0 || isDundeeCompliant)  {

            savtsn = t;

            // Update solar perturbations for time T
            T zm = t.multiply(TLEConstants.ZNS).add(zmos);
            T zf = zm.add(FastMath.sin(zm).multiply(2 * TLEConstants.ZES));
            FieldSinCos<T> sczf = FastMath.sinCos(zf);
            T sinzf = sczf.sin();
            T f2 = sinzf.multiply(sinzf).multiply(0.5).subtract(0.25);
            T f3 = sinzf.multiply(sczf.cos()).multiply(-0.5);
            final T ses = se2.multiply(f2).add(se3.multiply(f3));
            final T sis = si2.multiply(f2).add(si3.multiply(f3));
            final T sls = sl2.multiply(f2).add(sl3.multiply(f3)).add(sl4.multiply(sinzf));
            final T sghs = sgh2.multiply(f2).add(sgh3.multiply(f3)).add(sgh4.multiply(sinzf));
            final T shs = sh2.multiply(f2).add(sh3.multiply(f3));

            // Update lunar perturbations for time T
            zm = t.multiply(TLEConstants.ZNL).add(zmol);
            zf = zm.add(FastMath.sin(zm).multiply(2 * TLEConstants.ZEL));
            sczf = FastMath.sinCos(zf);
            sinzf = sczf.sin();
            f2 =  sinzf.multiply(sinzf).multiply(0.5).subtract(0.25);
            f3 = sinzf.multiply(sczf.cos()).multiply(-0.5);
            final T sel = ee2.multiply(f2).add(e3.multiply(f3));
            final T sil = xi2.multiply(f2).add(xi3.multiply(f3));
            final T sll = xl2.multiply(f2).add(xl3.multiply(f3)).add(xl4.multiply(sinzf));
            final T sghl = xgh2.multiply(f2).add(xgh3.multiply(f3)).add(xgh4.multiply(sinzf));
            final T sh1 = xh2.multiply(f2).add(xh3.multiply(f3));

            // Sum the solar and lunar contributions
            pe   = ses.add(sel);
            pinc = sis.add(sil);
            pl   = sls.add(sll);
            pgh  = sghs.add(sghl);
            ph   = shs.add(sh1);
        }

        xinc = xinc.add(pinc);

        final FieldSinCos<T> scis = FastMath.sinCos(xinc);
        final T sinis = scis.sin();
        final T cosis = scis.cos();

        /* Add solar/lunar perturbation correction to eccentricity: */
        em     = em.add(pe);
        xll    = xll.add(pl);
        omgadf = omgadf.add(pgh);
        xinc   = MathUtils.normalizeAngle(xinc, t.getField().getZero());

        if (FastMath.abs(xinc).getReal() >= 0.2) {
            // Apply periodics directly
            final T temp_val = ph.divide(sinis);
            omgadf = omgadf.subtract(cosis.multiply(temp_val));
            xnode  = xnode.add(temp_val);
        } else {
            // Apply periodics with Lyddane modification
            final FieldSinCos<T> scok = FastMath.sinCos(xnode);
            final T sinok = scok.sin();
            final T cosok = scok.cos();
            final T alfdp =  ph.multiply(cosok).add((pinc.multiply(cosis).add(sinis)).multiply(sinok));
            final T betdp = ph.negate().multiply(sinok).add((pinc.multiply(cosis).add(sinis)).multiply(cosok));
            final T delta_xnode = MathUtils.normalizeAngle(FastMath.atan2(alfdp, betdp).subtract(xnode), t.getField().getZero());
            final T dls = xnode.negate().multiply(sinis).multiply(pinc);
            omgadf = omgadf.add(dls.subtract(cosis.multiply(delta_xnode)));
            xnode  = xnode.add(delta_xnode);
        }
    }

    /** Computes internal secular derivs. */
    private void computeSecularDerivs() {

        final FieldSinCos<T> sc_li  = FastMath.sinCos(xli);
        final T sin_li = sc_li.sin();
        final T cos_li = sc_li.cos();
        final T sin_2li = sin_li.multiply(cos_li).multiply(2.);
        final T cos_2li = cos_li.multiply(cos_li).multiply(2.).subtract(1.);

        // Dot terms calculated :
        if (synchronous)  {
            final T sin_3li = sin_2li.multiply(cos_li).add(cos_2li.multiply(sin_li));
            final T cos_3li = cos_2li.multiply(cos_li).subtract(sin_2li.multiply(sin_li));
            final T term1a = del1.multiply(sin_li .multiply(TLEConstants.C_FASX2) .subtract(cos_li .multiply(TLEConstants.S_FASX2 )));
            final T term2a = del2.multiply(sin_2li.multiply(TLEConstants.C_2FASX4).subtract(cos_2li.multiply(TLEConstants.S_2FASX4)));
            final T term3a = del3.multiply(sin_3li.multiply(TLEConstants.C_3FASX6).subtract(cos_3li.multiply(TLEConstants.S_3FASX6)));
            final T term1b = del1.multiply(cos_li .multiply(TLEConstants.C_FASX2)      .add(sin_li .multiply(TLEConstants.S_FASX2 )));
            final T term2b = del2.multiply(cos_2li.multiply(TLEConstants.C_2FASX4)     .add(sin_2li.multiply(TLEConstants.S_2FASX4))).multiply(2.0);
            final T term3b = del3.multiply(cos_3li.multiply(TLEConstants.C_3FASX6)     .add(sin_3li.multiply(TLEConstants.S_3FASX6))).multiply(3.0);
            derivs[0] = term1a.add(term2a).add(term3a);
            derivs[1] = term1b.add(term2b).add(term3b);
        } else {
            // orbit is a 12-hour resonant one
            final T xomi = omegaq.add(omgdot.multiply(atime));
            final FieldSinCos<T> sc_omi  = FastMath.sinCos(xomi);
            final T sin_omi = sc_omi.sin();
            final T cos_omi = sc_omi.cos();
            final T sin_li_m_omi = sin_li.multiply(cos_omi).subtract(sin_omi.multiply(cos_li));
            final T sin_li_p_omi = sin_li.multiply(cos_omi).add(     sin_omi.multiply(cos_li));
            final T cos_li_m_omi = cos_li.multiply(cos_omi).add(     sin_omi.multiply(sin_li));
            final T cos_li_p_omi = cos_li.multiply(cos_omi).subtract(sin_omi.multiply(sin_li));
            final T sin_2omi = sin_omi.multiply(cos_omi).multiply(2.0);
            final T cos_2omi = cos_omi.multiply(cos_omi).multiply(2.0).subtract(1.0);
            final T sin_2li_m_omi  = sin_2li.multiply(cos_omi ).subtract(sin_omi .multiply(cos_2li));
            final T sin_2li_p_omi  = sin_2li.multiply(cos_omi ).add(     sin_omi .multiply(cos_2li));
            final T cos_2li_m_omi  = cos_2li.multiply(cos_omi ).add(     sin_omi .multiply(sin_2li));
            final T cos_2li_p_omi  = cos_2li.multiply(cos_omi ).subtract(sin_omi .multiply(sin_2li));
            final T sin_2li_p_2omi = sin_2li.multiply(cos_2omi).add(     sin_2omi.multiply(cos_2li));
            final T cos_2li_p_2omi = cos_2li.multiply(cos_2omi).subtract(sin_2omi.multiply(sin_2li));
            final T sin_2omi_p_li  = sin_li .multiply(cos_2omi).add(     sin_2omi.multiply(cos_li ));
            final T cos_2omi_p_li  = cos_li .multiply(cos_2omi).subtract(sin_2omi.multiply(sin_li ));
            final T term1a = d2201.multiply(sin_2omi_p_li .multiply(TLEConstants.C_G22).subtract(cos_2omi_p_li .multiply(TLEConstants.S_G22))) .add(
                             d2211.multiply(sin_li        .multiply(TLEConstants.C_G22).subtract(cos_li        .multiply(TLEConstants.S_G22)))).add(
                             d3210.multiply(sin_li_p_omi  .multiply(TLEConstants.C_G32).subtract(cos_li_p_omi  .multiply(TLEConstants.S_G32)))).add(
                             d3222.multiply(sin_li_m_omi  .multiply(TLEConstants.C_G32).subtract(cos_li_m_omi  .multiply(TLEConstants.S_G32)))).add(
                             d5220.multiply(sin_li_p_omi  .multiply(TLEConstants.C_G52).subtract(cos_li_p_omi  .multiply(TLEConstants.S_G52)))).add(
                             d5232.multiply(sin_li_m_omi  .multiply(TLEConstants.C_G52).subtract(cos_li_m_omi  .multiply(TLEConstants.S_G52))));
            final T term2a = d4410.multiply(sin_2li_p_2omi.multiply(TLEConstants.C_G44).subtract(cos_2li_p_2omi.multiply(TLEConstants.S_G44))) .add(
                             d4422.multiply(sin_2li       .multiply(TLEConstants.C_G44).subtract(cos_2li       .multiply(TLEConstants.S_G44)))).add(
                             d5421.multiply(sin_2li_p_omi .multiply(TLEConstants.C_G54).subtract(cos_2li_p_omi .multiply(TLEConstants.S_G54)))).add(
                             d5433.multiply(sin_2li_m_omi .multiply(TLEConstants.C_G54).subtract(cos_2li_m_omi .multiply(TLEConstants.S_G54))));
            final T term1b = d2201.multiply(cos_2omi_p_li .multiply(TLEConstants.C_G22)     .add(sin_2omi_p_li .multiply(TLEConstants.S_G22))) .add(
                             d2211.multiply(cos_li        .multiply(TLEConstants.C_G22)     .add(sin_li        .multiply(TLEConstants.S_G22)))).add(
                             d3210.multiply(cos_li_p_omi  .multiply(TLEConstants.C_G32)     .add(sin_li_p_omi  .multiply(TLEConstants.S_G32)))).add(
                             d3222.multiply(cos_li_m_omi  .multiply(TLEConstants.C_G32)     .add(sin_li_m_omi  .multiply(TLEConstants.S_G32)))).add(
                             d5220.multiply(cos_li_p_omi  .multiply(TLEConstants.C_G52)     .add(sin_li_p_omi  .multiply(TLEConstants.S_G52)))).add(
                             d5232.multiply(cos_li_m_omi  .multiply(TLEConstants.C_G52)     .add(sin_li_m_omi  .multiply(TLEConstants.S_G52))));
            final T term2b = d4410.multiply(cos_2li_p_2omi.multiply(TLEConstants.C_G44)     .add(sin_2li_p_2omi.multiply(TLEConstants.S_G44))) .add(
                             d4422.multiply(cos_2li       .multiply(TLEConstants.C_G44)     .add(sin_2li       .multiply(TLEConstants.S_G44)))).add(
                             d5421.multiply(cos_2li_p_omi .multiply(TLEConstants.C_G54)     .add(sin_2li_p_omi .multiply(TLEConstants.S_G54)))).add(
                             d5433.multiply(cos_2li_m_omi .multiply(TLEConstants.C_G54)     .add(sin_2li_m_omi .multiply(TLEConstants.S_G54)))).multiply(2.0);

            derivs[0] = term1a.add(term2a);
            derivs[1] = term1b.add(term2b);

        }
    }

}
