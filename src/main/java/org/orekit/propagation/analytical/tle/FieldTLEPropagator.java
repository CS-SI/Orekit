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


import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.FieldAbstractAnalyticalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;


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
 * <a href="https://www.celestrak.com/">CelesTrak</a>, the precision is close to one kilometer
 * and error won't probably rise above 2 km).
 * </p>
 * <p>This implementation is largely inspired from the paper and source code <a
 * href="https://www.celestrak.com/publications/AIAA/2006-6753/">Revisiting Spacetrack
 * Report #3</a> and is fully compliant with its results and tests cases.</p>
 * @author Felix R. Hoots, Ronald L. Roehrich, December 1980 (original fortran)
 * @author David A. Vallado, Paul Crawford, Richard Hujsak, T.S. Kelso (C++ translation and improvements)
 * @author Fabien Maussion (java translation)
 * @see TLE
 */
public abstract class FieldTLEPropagator<T extends RealFieldElement<T>> extends FieldAbstractAnalyticalPropagator<T> {

    // CHECKSTYLE: stop VisibilityModifier check

    /** Initial state. */
    protected final FieldTLE<T> tle;

    /** UTC time scale. */
    protected final TimeScale utc;

    /** final RAAN. */
    protected T xnode;

    /** final semi major axis. */
    protected T a;

    /** final eccentricity. */
    protected T e;

    /** final inclination. */
    protected T i;

    /** final perigee argument. */
    protected T omega;

    /** L from SPTRCK #3. */
    protected T xl;

    /** original recovered semi major axis. */
    protected T a0dp;

    /** original recovered mean motion. */
    protected T xn0dp;

    /** cosinus original inclination. */
    protected T cosi0;

    /** cos io squared. */
    protected T theta2;

    /** sinus original inclination. */
    protected T sini0;

    /** common parameter for mean anomaly (M) computation. */
    protected T xmdot;

    /** common parameter for perigee argument (omega) computation. */
    protected T omgdot;

    /** common parameter for raan (OMEGA) computation. */
    protected T xnodot;

    /** original eccentricity squared. */
    protected T e0sq;
    /** 1 - e2. */
    protected T beta02;

    /** sqrt (1 - e2). */
    protected T beta0;

    /** perigee, expressed in KM and ALTITUDE. */
    protected T perige;

    /** eta squared. */
    protected T etasq;

    /** original eccentricity * eta. */
    protected T eeta;

    /** s* new value for the contant s. */
    protected T s4;

    /** tsi from SPTRCK #3. */
    protected T tsi;

    /** eta from SPTRCK #3. */
    protected T eta;

    /** coef for SGP C3 computation. */
    protected T coef;

    /** coef for SGP C5 computation. */
    protected T coef1;

    /** C1 from SPTRCK #3. */
    protected T c1;

    /** C2 from SPTRCK #3. */
    protected T c2;

    /** C4 from SPTRCK #3. */
    protected T c4;

    /** common parameter for raan (OMEGA) computation. */
    protected T xnodcf;

    /** 3/2 * C1. */
    protected T t2cof;

    // CHECKSTYLE: resume VisibilityModifier check

    /** TLE frame. */
    private final Frame teme;

    /** Spacecraft mass (kg). */
    private final T mass;

    /** Protected constructor for derived classes.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param initialTLE the unique TLE to propagate
     * @param attitudeProvider provider for attitude computation
     * @param mass spacecraft mass (kg)
     * @see #TLEPropagator(TLE, AttitudeProvider, double, Frame)
     */
    @DefaultDataContext
    protected FieldTLEPropagator(final FieldTLE<T> initialTLE, Field<T> field,
                            final AttitudeProvider attitudeProvider,
                            final T mass) {
        this(initialTLE, field, attitudeProvider, mass,
                DataContext.getDefault().getFrames().getTEME());
    }

    /** Protected constructor for derived classes.
     * @param initialTLE the unique TLE to propagate
     * @param attitudeProvider provider for attitude computation
     * @param mass spacecraft mass (kg)
     * @param teme the TEME frame to use for propagation.
     * @param field field used as default
     * @since 10.1
     */
    protected FieldTLEPropagator(final FieldTLE<T> initialTLE,
                            final Field<T> field,
                            final AttitudeProvider attitudeProvider,
                            final T mass,
                            final Frame teme) {
        super(field, attitudeProvider);
        setStartDate(initialTLE.getDate());  //requires a FieldTLE class.
        this.tle  = initialTLE;
        this.teme = teme;
        this.mass = mass;
        this.utc = initialTLE.getUtc();
        initializeCommons();
        sxpInitialize();
        // set the initial state
        final FieldOrbit<T> orbit = propagateOrbit(initialTLE.getDate());
        final FieldAttitude<T> attitude = attitudeProvider.getAttitude(orbit, orbit.getDate(), orbit.getFrame());
        super.resetInitialState(new FieldSpacecraftState<T>(orbit, attitude, mass));
    }

    /** Selects the extrapolator to use with the selected TLE.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param tle the TLE to propagate.
     * @return the correct propagator.
         * @param <T> elements type
     * @see #selectExtrapolator(TLE, Frames)
     */
    @DefaultDataContext
    public static <T extends RealFieldElement<T>> FieldTLEPropagator<T> selectExtrapolator(final FieldTLE<T> tle) {
        return selectExtrapolator(tle, DataContext.getDefault().getFrames());
    }

    /** Selects the extrapolator to use with the selected TLE.
     * @param tle the TLE to propagate.
     * @param frames set of Frames to use in the propagator.
     * @return the correct propagator.
         * @param <T> elements type
     * @since 10.1
     */
    public static <T extends RealFieldElement<T>> FieldTLEPropagator<T> selectExtrapolator(final FieldTLE<T> tle, final Frames frames) {
        return selectExtrapolator(
                tle,
                Propagator.getDefaultLaw(frames),
                DEFAULT_MASS,
                frames.getTEME());
    }

    /** Selects the extrapolator to use with the selected TLE.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param tle the TLE to propagate.
     * @param attitudeProvider provider for attitude computation
     * @param mass spacecraft mass (kg)
     * @return the correct propagator.
         * @param <T> elements type
     * @see #selectExtrapolator(TLE, AttitudeProvider, double, Frame)
     */
    @DefaultDataContext
    public static <T extends RealFieldElement<T>> FieldTLEPropagator<T> selectExtrapolator(final FieldTLE<T> tle,
                                                   final AttitudeProvider attitudeProvider,
                                                   final T mass) {
        return selectExtrapolator(tle, attitudeProvider, mass,
                DataContext.getDefault().getFrames().getTEME());
    }

    /** Selects the extrapolator to use with the selected TLE.
     * @param tle the TLE to propagate.
     * @param attitudeProvider provider for attitude computation
     * @param mass spacecraft mass (kg)
     * @param teme the TEME frame to use for propagation.
     * @return the correct propagator.
         * @param <T> elements type
     * @since 10.1
     */
    public static <T extends RealFieldElement<T>> FieldTLEPropagator<T> selectExtrapolator(final FieldTLE<T> tle,
                                                   final AttitudeProvider attitudeProvider,
                                                   final T mass,
                                                   final Frame teme) {

        final T a1 = tle.getMeanMotion().multiply(60.0).reciprocal().multiply(TLEConstants.XKE).pow(TLEConstants.TWO_THIRD);
        final T cosi0 = FastMath.cos(tle.getI());
        final T temp1 = cosi0.multiply(cosi0.multiply(3.0)).subtract(1.0).multiply(1.5 * TLEConstants.CK2);
        final T temp2 = tle.getE().multiply(tle.getE()).negate().add(1.0).pow(-1.5);
        final T temp = temp1.multiply(temp2);
        final T delta1 = temp.divide(a1.multiply(a1));
        final T a0 = a1.multiply(delta1.multiply(delta1.multiply(delta1.multiply(134.0 / 81.0).add(1.0)).add(TLEConstants.ONE_THIRD)).negate().add(1.0));
        final T delta0 = temp.divide(a0.multiply(a0));

        // recover original mean motion :
        final T xn0dp = tle.getMeanMotion().multiply(60.0).divide(delta0.add(1.0));

        // Period >= 225 minutes is deep space
        if (MathUtils.TWO_PI / (xn0dp.multiply(TLEConstants.MINUTES_PER_DAY).getReal()) >= (1.0 / 6.4)) {
            return new FieldDeepSDP4(tle, attitudeProvider, mass, teme);
        } else {
            return new FieldSGP4(tle, attitudeProvider, mass, teme);
        }
    }

    /** Get the Earth gravity coefficient used for TLE propagation.
     * @return the Earth gravity coefficient.
     */
    public static double getMU() {
        return TLEConstants.MU;
    }

    /** Get the extrapolated position and velocity from an initial TLE.
     * @param date the final date
     * @return the final PVCoordinates
     */
    public FieldPVCoordinates<T> getPVCoordinates(final FieldAbsoluteDate<T> date) {

        sxpPropagate(date.durationFrom(tle.getDate()).divide(60.0));

        // Compute PV with previous calculated parameters
        return computePVCoordinates();
    }

    /** Computation of the first commons parameters.
     */
    private void initializeCommons() {

        final T a1 = tle.getMeanMotion().multiply(60.0).reciprocal().multiply(TLEConstants.XKE).pow(TLEConstants.TWO_THIRD);
        cosi0 = FastMath.cos(tle.getI());
        theta2 = cosi0.multiply(cosi0);
        final T x3thm1 = theta2.multiply(3.0).subtract(1.0);
        e0sq = tle.getE().multiply(tle.getE());
        beta02 = e0sq.negate().add(1.0);
        beta0 = FastMath.sqrt(beta02);
        final T tval = x3thm1.multiply(1.5 * TLEConstants.CK2).divide(beta0.multiply(beta02));
        final T delta1 = tval.divide(a1.multiply(a1));
        final T temp = delta1.multiply(134.0 / 81.0).add(1.0).multiply(delta1).add(TLEConstants.ONE_THIRD);
        final T a0 = a1.multiply(delta1.multiply(temp).negate().add(1.0));
        final T delta0 = tval.divide(a0.multiply(a0));

        // recover original mean motion and semi-major axis :
        xn0dp = tle.getMeanMotion().multiply(60.0).divide(delta0.add(1.0));
        a0dp = a0.divide(delta0.negate().add(1.0));

        // Values of s and qms2t :
        s4 = s4.getField().getZero().add(TLEConstants.S);  // unmodified value for s
        T q0ms24 = q0ms24.getField().getZero().add(TLEConstants.QOMS2T); // unmodified value for q0ms2T

        perige = a0dp.multiply(tle.getE().negate().add(1.0)).subtract(TLEConstants.NORMALIZED_EQUATORIAL_RADIUS).multiply(
                                                                                                TLEConstants.EARTH_RADIUS); // perige

        //  For perigee below 156 km, the values of s and qoms2t are changed :
        if (perige.getReal() < 156.0) {
            if (perige.getReal() <= 98.0) {
                s4 = s4.getField().getZero().add(20.0);
            } else {
                s4 = perige.subtract(78.0);
            }
            final T temp_val = s4.negate().add(120.0).multiply(TLEConstants.NORMALIZED_EQUATORIAL_RADIUS / TLEConstants.EARTH_RADIUS);
            final T temp_val_squared = temp_val.multiply(temp_val);
            q0ms24 = temp_val_squared.multiply(temp_val_squared);
            s4 = s4.divide(TLEConstants.EARTH_RADIUS).add(TLEConstants.NORMALIZED_EQUATORIAL_RADIUS); // new value for q0ms2T and s
        }

        final T pinv = a0dp.multiply(beta02).reciprocal();
        final T pinvsq = pinv.multiply(pinv);
        tsi = a0dp.subtract(s4).reciprocal();
        eta = a0dp.multiply(tle.getE()).multiply(tsi);
        etasq = eta.multiply(eta);
        eeta = tle.getE().multiply(eta);

        final T psisq = etasq.negate().add(1.0).abs(); // abs because pow 3.5 needs positive value
        final T tsi_squared = tsi.multiply(tsi);
        coef = q0ms24.multiply(tsi_squared.multiply(tsi_squared));
        coef1 = coef.divide(psisq.pow(3.5));

        // C2 and C1 coefficients computation :
        final T temp1 = etasq.multiply(1.5).add(eeta.multiply(etasq.add(4.0))).multiply(coef1).multiply(xn0dp);
        final T temp2 = x3thm1.multiply(etasq.multiply(etasq.add(8.0)).multiply(3.0).add(8.0));
        c2 = temp1.add(temp2.multiply(tsi.divide(psisq).multiply(0.75 * TLEConstants.CK2)));
        c1 = tle.getBStar().multiply(c2);
        sini0 = FastMath.sin(tle.getI());

        final T x1mth2 = theta2.negate().add(1.0);

        // C4 coefficient computation :
        final T temp3 = etasq.multiply(0.5).add(2.0).multiply(eta).multiply(beta02).multiply(a0dp).multiply(coef1).multiply(xn0dp).multiply(2.0);
        final T temp4 = tle.getE().multiply(etasq.multiply(2.0).add(0.5));
        final T temp5 = tsi.divide(a0dp.multiply(psisq)).multiply(2.0 * TLEConstants.CK2);
        final T temp6 = etasq.multiply(0.5).add(2.0).multiply(etasq).add(eeta.multiply(2.0)).add(1.0).multiply(x3thm1).multiply(-3.0);
        final T temp7 = x1mth2.multiply(0.75).multiply(etasq.multiply(2.0).subtract(
                        FastMath.cos(tle.getPerigeeArgument().multiply(2.0)).multiply(etasq.add(1.0)).multiply(eeta)));
        c4 = temp3.add(temp4).subtract(temp5.multiply(temp6.add(temp7)));

        final T theta4 = theta2.multiply(theta2);
        final T temp8 = pinvsq.multiply(xn0dp).multiply(3 * TLEConstants.CK2);
        final T temp9 = temp8.multiply(pinvsq).multiply(TLEConstants.CK2);
        final T temp10 = pinvsq.multiply(pinvsq).multiply(xn0dp).multiply(1.25 * TLEConstants.CK4);

        // atmospheric and gravitation coefs :(Mdf and OMEGAdf)
        xmdot = xn0dp.add(
                temp8.multiply(0.5).multiply(beta0).multiply(x3thm1)).add(
                temp9.multiply(0.0625).multiply(beta0).multiply(
                theta2.multiply(78.0).negate().add(13.0).add(theta4.multiply(137.0))));

        final T x1m5th = theta2.multiply(5.0).negate().add(1.0);

        omgdot = -0.5 * temp8 * x1m5th +
                 0.0625 * temp9 * (7.0 - 114.0 * theta2 + 395.0 * theta4) +
                 temp10 * (3.0 - 36.0 * theta2 + 49.0 * theta4);

        final T xhdot1 = temp8.negate().multiply(cosi0);

        xnodot = xhdot1 + (0.5 * temp9 * (4.0 - 19.0 * theta2) + 2.0 * temp10 * (3.0 - 7.0 * theta2)) * cosi0;
        xnodcf = 3.5 * beta02 * xhdot1 * c1;
        t2cof = 1.5 * c1;

    }

    /** Retrieves the position and velocity.
     * @return the computed PVCoordinates.
     */
    private FieldPVCoordinates<T> computePVCoordinates() {

        // Long period periodics
        final double axn = e * FastMath.cos(omega);
        double temp = 1.0 / (a * (1.0 - e * e));
        final double xlcof = 0.125 * TLEConstants.A3OVK2 * sini0 * (3.0 + 5.0 * cosi0) / (1.0 + cosi0);
        final double aycof = 0.25 * TLEConstants.A3OVK2 * sini0;
        final double xll = temp * xlcof * axn;
        final double aynl = temp * aycof;
        final double xlt = xl + xll;
        final double ayn = e * FastMath.sin(omega) + aynl;
        final double elsq = axn * axn + ayn * ayn;
        final double capu = MathUtils.normalizeAngle(xlt - xnode, FastMath.PI);
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
            throw new OrekitException(OrekitMessages.TOO_LARGE_ECCENTRICITY_FOR_PROPAGATION_MODEL, e);
        }

        // Solve Kepler's' Equation.
        final double newtonRaphsonEpsilon = 1e-12;
        for (int j = 0; j < 10; j++) {

            boolean doSecondOrderNewtonRaphson = true;

            sinEPW = FastMath.sin( epw);
            cosEPW = FastMath.cos( epw);
            ecosE = axn * cosEPW + ayn * sinEPW;
            esinE = axn * sinEPW - ayn * cosEPW;
            final double f = capu - epw + esinE;
            if (FastMath.abs(f) < newtonRaphsonEpsilon) {
                break;
            }
            final double fdot = 1.0 - ecosE;
            double delta_epw = f / fdot;
            if (j == 0) {
                final double maxNewtonRaphson = 1.25 * FastMath.abs(e);
                doSecondOrderNewtonRaphson = false;
                if (delta_epw > maxNewtonRaphson) {
                    delta_epw = maxNewtonRaphson;
                } else if (delta_epw < -maxNewtonRaphson) {
                    delta_epw = -maxNewtonRaphson;
                } else {
                    doSecondOrderNewtonRaphson = true;
                }
            }
            if (doSecondOrderNewtonRaphson) {
                delta_epw = f / (fdot + 0.5 * esinE * delta_epw);
            }
            epw += delta_epw;
        }

        // Short period preliminary quantities
        temp = 1.0 - elsq;
        final double pl = a * temp;
        final double r = a * (1.0 - ecosE);
        double temp2 = a / r;
        final double betal = FastMath.sqrt(temp);
        temp = esinE / (1.0 + betal);
        final double cosu = temp2 * (cosEPW - axn + ayn * temp);
        final double sinu = temp2 * (sinEPW - ayn - axn * temp);
        final double u = FastMath.atan2(sinu, cosu);
        final double sin2u = 2.0 * sinu * cosu;
        final double cos2u = 2.0 * cosu * cosu - 1.0;
        final double temp1 = TLEConstants.CK2 / pl;
        temp2 = temp1 / pl;

        // Update for short periodics
        final double rk = r * (1.0 - 1.5 * temp2 * betal * x3thm1) + 0.5 * temp1 * x1mth2 * cos2u;
        final double uk = u - 0.25 * temp2 * x7thm1 * sin2u;
        final double xnodek = xnode + 1.5 * temp2 * cosi0 * sin2u;
        final double xinck = i + 1.5 * temp2 * cosi0 * sini0 * cos2u;

        // Orientation vectors
        final double sinuk = FastMath.sin(uk);
        final double cosuk = FastMath.cos(uk);
        final double sinik = FastMath.sin(xinck);
        final double cosik = FastMath.cos(xinck);
        final double sinnok = FastMath.sin(xnodek);
        final double cosnok = FastMath.cos(xnodek);
        final double xmx = -sinnok * cosik;
        final double xmy = cosnok * cosik;
        final double ux = xmx * sinuk + cosnok * cosuk;
        final double uy = xmy * sinuk + sinnok * cosuk;
        final double uz = sinik * sinuk;

        // Position and velocity
        final double cr = 1000 * rk * TLEConstants.EARTH_RADIUS;
        final Vector3D pos = new Vector3D(cr * ux, cr * uy, cr * uz);

        final double rdot   = TLEConstants.XKE * FastMath.sqrt(a) * esinE / r;
        final double rfdot  = TLEConstants.XKE * FastMath.sqrt(pl) / r;
        final double xn     = TLEConstants.XKE / (a * FastMath.sqrt(a));
        final double rdotk  = rdot - xn * temp1 * x1mth2 * sin2u;
        final double rfdotk = rfdot + xn * temp1 * (x1mth2 * cos2u + 1.5 * x3thm1);
        final double vx     = xmx * cosuk - cosnok * sinuk;
        final double vy     = xmy * cosuk - sinnok * sinuk;
        final double vz     = sinik * cosuk;

        final double cv = 1000.0 * TLEConstants.EARTH_RADIUS / 60.0;
        final Vector3D vel = new Vector3D(cv * (rdotk * ux + rfdotk * vx),
                                          cv * (rdotk * uy + rfdotk * vy),
                                          cv * (rdotk * uz + rfdotk * vz));

        return new PVCoordinates(pos, vel);

    }

    /** Initialization proper to each propagator (SGP or SDP).
     */
    protected abstract void sxpInitialize();

    /** Propagation proper to each propagator (SGP or SDP).
     * @param t the offset from initial epoch (min)
     */
    protected abstract void sxpPropagate(T t);

    /** {@inheritDoc} */
    public void resetInitialState(final SpacecraftState state) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    protected void resetIntermediateState(final SpacecraftState state, final boolean forward) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    protected double getMass(final AbsoluteDate date) {
        return mass;
    }

    /** {@inheritDoc} */
    protected Orbit propagateOrbit(final AbsoluteDate date) {
        return new CartesianOrbit(getPVCoordinates(date), teme, date, TLEConstants.MU);
    }

    /** Get the underlying TLE.
     * @return underlying TLE
     */
    public TLE getTLE() {
        return tle;
    }

    /** {@inheritDoc} */
    public Frame getFrame() {
        return teme;
    }

}
