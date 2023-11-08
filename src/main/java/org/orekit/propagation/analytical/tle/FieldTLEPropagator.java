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


import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldAbstractAnalyticalPropagator;
import org.orekit.propagation.analytical.tle.generation.TleGenerationAlgorithm;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;


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
 * for each TLE (this instance can only be get using {@link #selectExtrapolator(FieldTLE, CalculusFieldElement[])} method,
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
 * @author Thomas Paulet (field translation)
 * @since 11.0
 * @see FieldTLE
 * @param <T> type of the field elements
 */
public abstract class FieldTLEPropagator<T extends CalculusFieldElement<T>> extends FieldAbstractAnalyticalPropagator<T> {

    // CHECKSTYLE: stop VisibilityModifier check

    /** Initial state. */
    protected FieldTLE<T> tle;

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
     * @param parameters SGP4 and SDP4 model parameters
     * @see #FieldTLEPropagator(FieldTLE, AttitudeProvider, CalculusFieldElement, Frame, CalculusFieldElement[])
     */
    @DefaultDataContext
    protected FieldTLEPropagator(final FieldTLE<T> initialTLE,
                            final AttitudeProvider attitudeProvider,
                            final T mass,
                            final T[] parameters) {
        this(initialTLE, attitudeProvider, mass,
                DataContext.getDefault().getFrames().getTEME(), parameters);
    }

    /** Protected constructor for derived classes.
     * @param initialTLE the unique TLE to propagate
     * @param attitudeProvider provider for attitude computation
     * @param mass spacecraft mass (kg)
     * @param teme the TEME frame to use for propagation.
     * @param parameters SGP4 and SDP4 model parameters
     */
    protected FieldTLEPropagator(final FieldTLE<T> initialTLE,
                            final AttitudeProvider attitudeProvider,
                            final T mass,
                            final Frame teme,
                            final T[] parameters) {
        super(initialTLE.getE().getField(), attitudeProvider);
        setStartDate(initialTLE.getDate());
        this.tle  = initialTLE;
        this.teme = teme;
        this.mass = mass;
        this.utc = initialTLE.getUtc();

        initializeCommons(parameters);
        sxpInitialize(parameters);
        // set the initial state
        final FieldOrbit<T> orbit = propagateOrbit(initialTLE.getDate(), parameters);
        final FieldAttitude<T> attitude = attitudeProvider.getAttitude(orbit, orbit.getDate(), orbit.getFrame());
        super.resetInitialState(new FieldSpacecraftState<>(orbit, attitude, mass));
    }

    /** Selects the extrapolator to use with the selected TLE.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param tle the TLE to propagate.
     * @param parameters SGP4 and SDP4 model parameters
     * @return the correct propagator.
     * @param <T> elements type
     * @see #selectExtrapolator(FieldTLE, Frame, CalculusFieldElement[])
     */
    @DefaultDataContext
    public static <T extends CalculusFieldElement<T>> FieldTLEPropagator<T> selectExtrapolator(final FieldTLE<T> tle, final T[] parameters) {
        return selectExtrapolator(tle, DataContext.getDefault().getFrames().getTEME(), parameters);
    }

    /** Selects the extrapolator to use with the selected TLE.
     *
     *<p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param tle the TLE to propagate.
     * @param teme TEME frame.
     * @param parameters SGP4 and SDP4 model parameters
     * @return the correct propagator.
     * @param <T> elements type
     */
    public static <T extends CalculusFieldElement<T>> FieldTLEPropagator<T> selectExtrapolator(final FieldTLE<T> tle, final Frame teme, final T[] parameters) {
        return selectExtrapolator(
                tle,
                FrameAlignedProvider.of(teme),
                tle.getE().getField().getZero().add(DEFAULT_MASS),
                teme,
                parameters);
    }

    /** Selects the extrapolator to use with the selected TLE.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param tle the TLE to propagate.
     * @param attitudeProvider provider for attitude computation
     * @param mass spacecraft mass (kg)
     * @param parameters SGP4 and SDP4 model parameters
     * @return the correct propagator.
     * @param <T> elements type
     * @see #selectExtrapolator(FieldTLE, AttitudeProvider, CalculusFieldElement, Frame, CalculusFieldElement[])
     */
    @DefaultDataContext
    public static <T extends CalculusFieldElement<T>> FieldTLEPropagator<T> selectExtrapolator(final FieldTLE<T> tle,
                                                   final AttitudeProvider attitudeProvider,
                                                   final T mass,
                                                   final T[] parameters) {
        return selectExtrapolator(tle, attitudeProvider, mass,
                DataContext.getDefault().getFrames().getTEME(), parameters);
    }

    /** Selects the extrapolator to use with the selected TLE.
     *
     * @param tle the TLE to propagate.
     * @param attitudeProvider provider for attitude computation
     * @param mass spacecraft mass (kg)
     * @param teme the TEME frame to use for propagation.
     * @param parameters SGP4 and SDP4 model parameters
     * @return the correct propagator.
     * @param <T> elements type
     */
    public static <T extends CalculusFieldElement<T>> FieldTLEPropagator<T> selectExtrapolator(final FieldTLE<T> tle,
                                                   final AttitudeProvider attitudeProvider,
                                                   final T mass,
                                                   final Frame teme,
                                                   final T[] parameters) {

        final T a1 = tle.getMeanMotion().multiply(60.0).reciprocal().multiply(TLEConstants.XKE).pow(TLEConstants.TWO_THIRD);
        final T cosi0 = FastMath.cos(tle.getI());
        final T temp1 = cosi0.multiply(cosi0.multiply(3.0)).subtract(1.0).multiply(1.5 * TLEConstants.CK2);
        final T temp2 = tle.getE().multiply(tle.getE()).negate().add(1.0).pow(-1.5);
        final T temp = temp1.multiply(temp2);
        final T delta1 = temp.divide(a1.multiply(a1));
        final T a0 = a1.multiply(delta1.multiply(delta1.multiply(
                        delta1.multiply(134.0 / 81.0).add(1.0)).add(TLEConstants.ONE_THIRD)).negate().add(1.0));
        final T delta0 = temp.divide(a0.multiply(a0));

        // recover original mean motion :
        final T xn0dp = tle.getMeanMotion().multiply(60.0).divide(delta0.add(1.0));

        // Period >= 225 minutes is deep space
        if (MathUtils.TWO_PI / (xn0dp.multiply(TLEConstants.MINUTES_PER_DAY).getReal()) >= (1.0 / 6.4)) {
            return new FieldDeepSDP4<>(tle, attitudeProvider, mass, teme, parameters);
        } else {
            return new FieldSGP4<>(tle, attitudeProvider, mass, teme, parameters);
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
     * @param parameters values of the model
     * @return the final PVCoordinates
     */
    public FieldPVCoordinates<T> getPVCoordinates(final FieldAbsoluteDate<T> date, final T[] parameters) {

        sxpPropagate(date.durationFrom(tle.getDate()).divide(60.0), parameters);

        // Compute PV with previous calculated parameters
        return computePVCoordinates();
    }

    /** Computation of the first commons parameters.
     * @param parameters SGP4 and SDP4 model parameters
     */
    private void initializeCommons(final T[] parameters) {

        final T zero = mass.getField().getZero();
        final T bStar = parameters[0];
        final T a1 = tle.getMeanMotion().multiply(60.0).reciprocal().multiply(TLEConstants.XKE).pow(TLEConstants.TWO_THIRD);
        cosi0 = FastMath.cos(tle.getI());
        theta2 = cosi0.multiply(cosi0);
        final T x3thm1 = theta2.multiply(3.0).subtract(1.0);
        e0sq = tle.getE().multiply(tle.getE());
        beta02 = e0sq.negate().add(1.0);
        beta0 = FastMath.sqrt(beta02);
        final T tval = x3thm1.multiply(1.5 * TLEConstants.CK2).divide(beta0.multiply(beta02));
        final T delta1 = tval.divide(a1.multiply(a1));
        final T a0 = a1.multiply(delta1.multiply(
                     delta1.multiply(134.0 / 81.0).add(1.0).multiply(delta1).add(TLEConstants.ONE_THIRD)).negate().add(1.0));
        final T delta0 = tval.divide(a0.multiply(a0));

        // recover original mean motion and semi-major axis :
        xn0dp = tle.getMeanMotion().multiply(60.0).divide(delta0.add(1.0));
        a0dp = a0.divide(delta0.negate().add(1.0));

        // Values of s and qms2t :
        s4 = zero.add(TLEConstants.S);  // unmodified value for s
        T q0ms24 = zero.add(TLEConstants.QOMS2T); // unmodified value for q0ms2T

        perige = a0dp.multiply(tle.getE().negate().add(1.0)).subtract(TLEConstants.NORMALIZED_EQUATORIAL_RADIUS).multiply(
                                                                                                TLEConstants.EARTH_RADIUS); // perige

        //  For perigee below 156 km, the values of s and qoms2t are changed :
        if (perige.getReal() < 156.0) {
            if (perige.getReal() <= 98.0) {
                s4 = zero.add(20.0);
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
        c2 = coef1.multiply(xn0dp).multiply(a0dp.multiply(
                           etasq.multiply(1.5).add(eeta.multiply(etasq.add(4.0))).add(1.0)).add(
                           tsi.divide(psisq).multiply(x3thm1).multiply(0.75 * TLEConstants.CK2).multiply(
                           etasq.multiply(etasq.add(8.0)).multiply(3.0).add(8.0))));
        c1 = bStar.multiply(c2);
        sini0 = FastMath.sin(tle.getI());

        final T x1mth2 = theta2.negate().add(1.0);

        // C4 coefficient computation :
        c4 = xn0dp.multiply(coef1).multiply(a0dp).multiply(2.0).multiply(beta02).multiply(
                           eta.multiply(etasq.multiply(0.5).add(2.0)).add(tle.getE().multiply(etasq.multiply(2.0).add(0.5))).subtract(
                           tsi.divide(a0dp.multiply(psisq)).multiply(2 * TLEConstants.CK2).multiply(
                           x3thm1.multiply(-3).multiply(etasq.multiply(eeta.multiply(-0.5).add(1.5)).add(eeta.multiply(-2.0)).add(1.0)).add(
                           x1mth2.multiply(0.75).multiply(etasq.multiply(2.0).subtract(eeta.multiply(etasq.add(1.0)))).multiply(FastMath.cos(tle.getPerigeeArgument().multiply(2.0)))))));

        final T theta4 = theta2.multiply(theta2);
        final T temp1  = pinvsq.multiply(xn0dp).multiply(3 * TLEConstants.CK2);
        final T temp2  = temp1.multiply(pinvsq).multiply(TLEConstants.CK2);
        final T temp3  = pinvsq.multiply(pinvsq).multiply(xn0dp).multiply(1.25 * TLEConstants.CK4);

        // atmospheric and gravitation coefs :(Mdf and OMEGAdf)
        xmdot = xn0dp.add(
                temp1.multiply(0.5).multiply(beta0).multiply(x3thm1)).add(
                temp2.multiply(0.0625).multiply(beta0).multiply(
                theta2.multiply(78.0).negate().add(13.0).add(theta4.multiply(137.0))));

        final T x1m5th = theta2.multiply(5.0).negate().add(1.0);

        omgdot = temp1.multiply(-0.5).multiply(x1m5th).add(
                 temp2.multiply(0.0625).multiply(theta2.multiply(114.0).negate().add(
                 theta4.multiply(395.0)).add(7.0))).add(
                 temp3.multiply(theta2.multiply(36.0).negate().add(theta4.multiply(49.0)).add(3.0)));

        final T xhdot1 = temp1.negate().multiply(cosi0);

        xnodot = xhdot1.add(temp2.multiply(0.5).multiply(theta2.multiply(19.0).negate().add(4.0)).add(
                 temp3.multiply(2.0).multiply(theta2.multiply(7.0).negate().add(3.0))).multiply(cosi0));
        xnodcf = beta02.multiply(xhdot1).multiply(c1).multiply(3.5);
        t2cof = c1.multiply(1.5);

    }

    /** Retrieves the position and velocity.
     * @return the computed PVCoordinates.
     */
    private FieldPVCoordinates<T> computePVCoordinates() {

        final T zero = mass.getField().getZero();
        // Long period periodics
        final T axn = e.multiply(FastMath.cos(omega));
        T temp = a.multiply(e.multiply(e).negate().add(1.0)).reciprocal();
        final T xlcof = sini0.multiply(0.125 * TLEConstants.A3OVK2).multiply(
                             cosi0.multiply(5.0).add(3.0).divide(cosi0.add(1.0)));
        final T aycof = sini0.multiply(0.25 * TLEConstants.A3OVK2);
        final T xll   = temp.multiply(xlcof).multiply(axn);
        final T aynl  = temp.multiply(aycof);
        final T xlt   = xl.add(xll);
        final T ayn   = e.multiply(FastMath.sin(omega)).add(aynl);
        final T elsq  = axn.multiply(axn).add(ayn.multiply(ayn));
        final T capu  = MathUtils.normalizeAngle(xlt.subtract(xnode), zero.getPi());
        T epw    = capu;
        T ecosE  = zero;
        T esinE  = zero;
        T sinEPW = zero;
        T cosEPW = zero;

        // Dundee changes:  items dependent on cosio get recomputed:
        final T cosi0Sq = cosi0.multiply(cosi0);
        final T x3thm1  = cosi0Sq.multiply(3.0).subtract(1.0);
        final T x1mth2  = cosi0Sq.negate().add(1.0);
        final T x7thm1  = cosi0Sq.multiply(7.0).subtract(1.0);

        if (e.getReal() > (1 - 1e-6)) {
            throw new OrekitException(OrekitMessages.TOO_LARGE_ECCENTRICITY_FOR_PROPAGATION_MODEL, e);
        }

        // Solve Kepler's' Equation.
        final double newtonRaphsonEpsilon = 1e-12;
        for (int j = 0; j < 10; j++) {

            boolean doSecondOrderNewtonRaphson = true;

            sinEPW = FastMath.sin( epw);
            cosEPW = FastMath.cos( epw);
            ecosE  = axn.multiply(cosEPW).add(ayn.multiply(sinEPW));
            esinE  = axn.multiply(sinEPW).subtract(ayn.multiply(cosEPW));
            final T f = capu.subtract(epw).add(esinE);
            if (FastMath.abs(f.getReal()) < newtonRaphsonEpsilon) {
                break;
            }
            final T fdot = ecosE.negate().add(1.0);
            T delta_epw = f.divide(fdot);
            if (j == 0) {
                final T maxNewtonRaphson = e.abs().multiply(1.25);
                doSecondOrderNewtonRaphson = false;
                if (delta_epw.getReal() > maxNewtonRaphson.getReal()) {
                    delta_epw = maxNewtonRaphson;
                } else if (delta_epw.getReal() < -maxNewtonRaphson.getReal()) {
                    delta_epw = maxNewtonRaphson.negate();
                } else {
                    doSecondOrderNewtonRaphson = true;
                }
            }
            if (doSecondOrderNewtonRaphson) {
                delta_epw = f.divide(fdot.add(esinE.multiply(0.5).multiply(delta_epw)));
            }
            epw = epw.add(delta_epw);
        }

        // Short period preliminary quantities
        temp = elsq.negate().add(1.0);
        final T pl = a.multiply(temp);
        final T r  = a.multiply(ecosE.negate().add(1.0));
        T temp2 = a.divide(r);
        final T betal = FastMath.sqrt(temp);
        temp = esinE.divide(betal.add(1.0));
        final T cosu  = temp2.multiply(cosEPW.subtract(axn).add(ayn.multiply(temp)));
        final T sinu  = temp2.multiply(sinEPW.subtract(ayn).subtract(axn.multiply(temp)));
        final T u     = FastMath.atan2(sinu, cosu);
        final T sin2u = sinu.multiply(cosu).multiply(2.0);
        final T cos2u = cosu.multiply(cosu).multiply(2.0).subtract(1.0);
        final T temp1 = pl.reciprocal().multiply(TLEConstants.CK2);
        temp2         = temp1.divide(pl);

        // Update for short periodics
        final T rk = r.multiply(temp2.multiply(betal).multiply(x3thm1).multiply(-1.5).add(1.0)).add(
                     temp1.multiply(x1mth2).multiply(cos2u).multiply(0.5));
        final T uk = u.subtract(temp2.multiply(x7thm1).multiply(sin2u).multiply(0.25));
        final T xnodek = xnode.add(temp2.multiply(cosi0).multiply(sin2u).multiply(1.5));
        final T xinck = i.add(temp2.multiply(cosi0).multiply(sini0).multiply(cos2u).multiply(1.5));

        // Orientation vectors
        final T sinuk  = FastMath.sin(uk);
        final T cosuk  = FastMath.cos(uk);
        final T sinik  = FastMath.sin(xinck);
        final T cosik  = FastMath.cos(xinck);
        final T sinnok = FastMath.sin(xnodek);
        final T cosnok = FastMath.cos(xnodek);
        final T xmx    = sinnok.negate().multiply(cosik);
        final T xmy    = cosnok.multiply(cosik);
        final T ux     = xmx.multiply(sinuk).add(cosnok.multiply(cosuk));
        final T uy     = xmy.multiply(sinuk).add(sinnok.multiply(cosuk));
        final T uz     = sinik.multiply(sinuk);

        // Position and velocity
        final T cr = rk.multiply(1000 * TLEConstants.EARTH_RADIUS);
        final FieldVector3D<T> pos = new FieldVector3D<>(cr.multiply(ux), cr.multiply(uy), cr.multiply(uz));

        final T rdot   = FastMath.sqrt(a).multiply(esinE.divide(r)).multiply(TLEConstants.XKE);
        final T rfdot  = FastMath.sqrt(pl).divide(r).multiply(TLEConstants.XKE);
        final T xn     = a.multiply(FastMath.sqrt(a)).reciprocal().multiply(TLEConstants.XKE);
        final T rdotk  = rdot.subtract(xn.multiply(temp1).multiply(x1mth2).multiply(sin2u));
        final T rfdotk = rfdot.add(xn.multiply(temp1).multiply(x1mth2.multiply(cos2u).add(x3thm1.multiply(1.5))));
        final T vx     = xmx.multiply(cosuk).subtract(cosnok.multiply(sinuk));
        final T vy     = xmy.multiply(cosuk).subtract(sinnok.multiply(sinuk));
        final T vz     = sinik.multiply(cosuk);

        final double cv = 1000.0 * TLEConstants.EARTH_RADIUS / 60.0;
        final FieldVector3D<T> vel = new FieldVector3D<>(rdotk.multiply(ux).add(rfdotk.multiply(vx)).multiply(cv),
                                                          rdotk.multiply(uy).add(rfdotk.multiply(vy)).multiply(cv),
                                                          rdotk.multiply(uz).add(rfdotk.multiply(vz)).multiply(cv));
        return new FieldPVCoordinates<T>(pos, vel);

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return tle.getParametersDrivers();
    }

    /** Initialization proper to each propagator (SGP or SDP).
     * @param parameters model parameters
     */
    protected abstract void sxpInitialize(T[] parameters);

    /** Propagation proper to each propagator (SGP or SDP).
     * @param t the offset from initial epoch (min)
     * @param parameters model parameters
     */
    protected abstract void sxpPropagate(T t, T[] parameters);

    /** {@inheritDoc}
     * <p>
     * For TLE propagator, calling this method is only recommended
     * for covariance propagation when the new <code>state</code>
     * differs from the previous one by only adding the additional
     * state containing the derivatives.
     * </p>
     */
    public void resetInitialState(final FieldSpacecraftState<T> state) {
        super.resetInitialState(state);
        super.setStartDate(state.getDate());
        final TleGenerationAlgorithm algorithm = TLEPropagator.getDefaultTleGenerationAlgorithm(utc, teme);
        final FieldTLE<T> newTLE = algorithm.generate(state, tle);
        this.tle = newTLE;
        initializeCommons(tle.getParameters(state.getDate().getField()));
        sxpInitialize(tle.getParameters(state.getDate().getField()));
    }

    /** {@inheritDoc} */
    protected void resetIntermediateState(final FieldSpacecraftState<T> state, final boolean forward) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    protected T getMass(final FieldAbsoluteDate<T> date) {
        return mass;
    }

    /** {@inheritDoc} */
    public FieldOrbit<T> propagateOrbit(final FieldAbsoluteDate<T> date, final T[] parameters) {
        return new FieldCartesianOrbit<>(getPVCoordinates(date, parameters), teme, date, date.getField().getZero().add(TLEConstants.MU));
    }

    /** Get the underlying TLE.
     * @return underlying TLE
     */
    public FieldTLE<T> getTLE() {
        return tle;
    }

    /** {@inheritDoc} */
    public Frame getFrame() {
        return teme;
    }

}
