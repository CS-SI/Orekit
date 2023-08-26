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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.AbstractMatricesHarvester;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.AbstractAnalyticalPropagator;
import org.orekit.propagation.analytical.tle.generation.FixedPointTleGenerationAlgorithm;
import org.orekit.propagation.analytical.tle.generation.TleGenerationAlgorithm;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;


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
public abstract class TLEPropagator extends AbstractAnalyticalPropagator {

    // CHECKSTYLE: stop VisibilityModifier check

    /** Initial state. */
    protected TLE tle;

    /** UTC time scale. */
    protected final TimeScale utc;

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

    // CHECKSTYLE: resume VisibilityModifier check

    /** TLE frame. */
    private final Frame teme;

    /** Spacecraft mass (kg). */
    private final double mass;

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
    protected TLEPropagator(final TLE initialTLE, final AttitudeProvider attitudeProvider,
                            final double mass) {
        this(initialTLE, attitudeProvider, mass,
                DataContext.getDefault().getFrames().getTEME());
    }

    /** Protected constructor for derived classes.
     * @param initialTLE the unique TLE to propagate
     * @param attitudeProvider provider for attitude computation
     * @param mass spacecraft mass (kg)
     * @param teme the TEME frame to use for propagation.
     * @since 10.1
     */
    protected TLEPropagator(final TLE initialTLE,
                            final AttitudeProvider attitudeProvider,
                            final double mass,
                            final Frame teme) {
        super(attitudeProvider);
        setStartDate(initialTLE.getDate());
        this.tle       = initialTLE;
        this.teme      = teme;
        this.mass      = mass;
        this.utc       = initialTLE.getUtc();

        initializeCommons();
        sxpInitialize();
        // set the initial state
        final Orbit orbit = propagateOrbit(initialTLE.getDate());
        final Attitude attitude = attitudeProvider.getAttitude(orbit, orbit.getDate(), orbit.getFrame());
        super.resetInitialState(new SpacecraftState(orbit, attitude, mass));
    }

    /** Selects the extrapolator to use with the selected TLE.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param tle the TLE to propagate.
     * @return the correct propagator.
     * @see #selectExtrapolator(TLE, Frame)
     */
    @DefaultDataContext
    public static TLEPropagator selectExtrapolator(final TLE tle) {
        return selectExtrapolator(tle, DataContext.getDefault().getFrames().getTEME());
    }

    /** Selects the extrapolator to use with the selected TLE.
     * @param tle the TLE to propagate.
     * @param teme TEME frame.
     * @return the correct propagator.
     * @since 10.1
     */
    public static TLEPropagator selectExtrapolator(final TLE tle, final Frame teme) {
        return selectExtrapolator(
                tle,
                FrameAlignedProvider.of(teme),
                DEFAULT_MASS,
                teme);
    }

    /** Selects the extrapolator to use with the selected TLE.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param tle the TLE to propagate.
     * @param attitudeProvider provider for attitude computation
     * @param mass spacecraft mass (kg)
     * @return the correct propagator.
     * @see #selectExtrapolator(TLE, AttitudeProvider, double, Frame)
     */
    @DefaultDataContext
    public static TLEPropagator selectExtrapolator(final TLE tle, final AttitudeProvider attitudeProvider,
                                                   final double mass) {
        return selectExtrapolator(tle, attitudeProvider, mass,
                                  DataContext.getDefault().getFrames().getTEME());
    }

    /** Selects the extrapolator to use with the selected TLE.
     * @param tle the TLE to propagate.
     * @param attitudeProvider provider for attitude computation
     * @param mass spacecraft mass (kg)
     * @param teme the TEME frame to use for propagation.
     * @return the correct propagator.
     * @since 10.1
     */
    public static TLEPropagator selectExtrapolator(final TLE tle,
                                                   final AttitudeProvider attitudeProvider,
                                                   final double mass,
                                                   final Frame teme) {

        final double a1 = FastMath.pow( TLEConstants.XKE / (tle.getMeanMotion() * 60.0), TLEConstants.TWO_THIRD);
        final double cosi0 = FastMath.cos(tle.getI());
        final double temp = TLEConstants.CK2 * 1.5 * (3 * cosi0 * cosi0 - 1.0) *
                            FastMath.pow(1.0 - tle.getE() * tle.getE(), -1.5);
        final double delta1 = temp / (a1 * a1);
        final double a0 = a1 * (1.0 - delta1 * (TLEConstants.ONE_THIRD + delta1 * (delta1 * 134.0 / 81.0 + 1.0)));
        final double delta0 = temp / (a0 * a0);

        // recover original mean motion :
        final double xn0dp = tle.getMeanMotion() * 60.0 / (delta0 + 1.0);

        // Period >= 225 minutes is deep space
        if (MathUtils.TWO_PI / (xn0dp * TLEConstants.MINUTES_PER_DAY) >= (1.0 / 6.4)) {
            return new DeepSDP4(tle, attitudeProvider, mass, teme);
        } else {
            return new SGP4(tle, attitudeProvider, mass, teme);
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
    public PVCoordinates getPVCoordinates(final AbsoluteDate date) {

        sxpPropagate(date.durationFrom(tle.getDate()) / 60.0);

        // Compute PV with previous calculated parameters
        return computePVCoordinates();
    }

    /** Computation of the first commons parameters.
     */
    private void initializeCommons() {

        // Sine and cosine of inclination
        final SinCos scI0 = FastMath.sinCos(tle.getI());

        final double a1 = FastMath.pow(TLEConstants.XKE / (tle.getMeanMotion() * 60.0), TLEConstants.TWO_THIRD);
        cosi0 = scI0.cos();
        theta2 = cosi0 * cosi0;
        final double x3thm1 = 3.0 * theta2 - 1.0;
        e0sq = tle.getE() * tle.getE();
        beta02 = 1.0 - e0sq;
        beta0 = FastMath.sqrt(beta02);
        final double tval = TLEConstants.CK2 * 1.5 * x3thm1 / (beta0 * beta02);
        final double delta1 = tval / (a1 * a1);
        final double a0 = a1 * (1.0 - delta1 * (TLEConstants.ONE_THIRD + delta1 * (1.0 + 134.0 / 81.0 * delta1)));
        final double delta0 = tval / (a0 * a0);

        // recover original mean motion and semi-major axis :
        xn0dp = tle.getMeanMotion() * 60.0 / (delta0 + 1.0);
        a0dp = a0 / (1.0 - delta0);

        // Values of s and qms2t :
        s4 = TLEConstants.S;  // unmodified value for s
        double q0ms24 = TLEConstants.QOMS2T; // unmodified value for q0ms2T

        perige = (a0dp * (1 - tle.getE()) - TLEConstants.NORMALIZED_EQUATORIAL_RADIUS) * TLEConstants.EARTH_RADIUS; // perige

        //  For perigee below 156 km, the values of s and qoms2t are changed :
        if (perige < 156.0) {
            if (perige <= 98.0) {
                s4 = 20.0;
            } else {
                s4 = perige - 78.0;
            }
            final double temp_val = (120.0 - s4) * TLEConstants.NORMALIZED_EQUATORIAL_RADIUS / TLEConstants.EARTH_RADIUS;
            final double temp_val_squared = temp_val * temp_val;
            q0ms24 = temp_val_squared * temp_val_squared;
            s4 = s4 / TLEConstants.EARTH_RADIUS + TLEConstants.NORMALIZED_EQUATORIAL_RADIUS; // new value for q0ms2T and s
        }

        final double pinv = 1.0 / (a0dp * beta02);
        final double pinvsq = pinv * pinv;
        tsi = 1.0 / (a0dp - s4);
        eta = a0dp * tle.getE() * tsi;
        etasq = eta * eta;
        eeta = tle.getE() * eta;

        final double psisq = FastMath.abs(1.0 - etasq); // abs because pow 3.5 needs positive value
        final double tsi_squared = tsi * tsi;
        coef = q0ms24 * tsi_squared * tsi_squared;
        coef1 = coef / FastMath.pow(psisq, 3.5);

        // C2 and C1 coefficients computation :
        c2 = coef1 * xn0dp * (a0dp * (1.0 + 1.5 * etasq + eeta * (4.0 + etasq)) +
             0.75 * TLEConstants.CK2 * tsi / psisq * x3thm1 * (8.0 + 3.0 * etasq * (8.0 + etasq)));
        c1 = tle.getBStar() * c2;
        sini0 = scI0.sin();

        final double x1mth2 = 1.0 - theta2;

        // C4 coefficient computation :
        c4 = 2.0 * xn0dp * coef1 * a0dp * beta02 * (eta * (2.0 + 0.5 * etasq) +
             tle.getE() * (0.5 + 2.0 * etasq) -
             2 * TLEConstants.CK2 * tsi / (a0dp * psisq) *
             (-3.0 * x3thm1 * (1.0 - 2.0 * eeta + etasq * (1.5 - 0.5 * eeta)) +
              0.75 * x1mth2 * (2.0 * etasq - eeta * (1.0 + etasq)) * FastMath.cos(2.0 * tle.getPerigeeArgument())));

        final double theta4 = theta2 * theta2;
        final double temp1 = 3 * TLEConstants.CK2 * pinvsq * xn0dp;
        final double temp2 = temp1 * TLEConstants.CK2 * pinvsq;
        final double temp3 = 1.25 * TLEConstants.CK4 * pinvsq * pinvsq * xn0dp;

        // atmospheric and gravitation coefs :(Mdf and OMEGAdf)
        xmdot = xn0dp +
                0.5 * temp1 * beta0 * x3thm1 +
                0.0625 * temp2 * beta0 * (13.0 - 78.0 * theta2 + 137.0 * theta4);

        final double x1m5th = 1.0 - 5.0 * theta2;

        omgdot = -0.5 * temp1 * x1m5th +
                 0.0625 * temp2 * (7.0 - 114.0 * theta2 + 395.0 * theta4) +
                 temp3 * (3.0 - 36.0 * theta2 + 49.0 * theta4);

        final double xhdot1 = -temp1 * cosi0;

        xnodot = xhdot1 + (0.5 * temp2 * (4.0 - 19.0 * theta2) + 2.0 * temp3 * (3.0 - 7.0 * theta2)) * cosi0;
        xnodcf = 3.5 * beta02 * xhdot1 * c1;
        t2cof = 1.5 * c1;

    }

    /** Retrieves the position and velocity.
     * @return the computed PVCoordinates.
     */
    private PVCoordinates computePVCoordinates() {

        // Sine and cosine of final perigee argument
        final SinCos scOmega = FastMath.sinCos(omega);

        // Long period periodics
        final double axn = e * scOmega.cos();
        double temp = 1.0 / (a * (1.0 - e * e));
        final double xlcof = 0.125 * TLEConstants.A3OVK2 * sini0 * (3.0 + 5.0 * cosi0) / (1.0 + cosi0);
        final double aycof = 0.25 * TLEConstants.A3OVK2 * sini0;
        final double xll = temp * xlcof * axn;
        final double aynl = temp * aycof;
        final double xlt = xl + xll;
        final double ayn = e * scOmega.sin() + aynl;
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

            final SinCos scEPW = FastMath.sinCos(epw);
            sinEPW = scEPW.sin();
            cosEPW = scEPW.cos();
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
        final SinCos scuk   = FastMath.sinCos(uk);
        final SinCos scik   = FastMath.sinCos(xinck);
        final SinCos scnok  = FastMath.sinCos(xnodek);
        final double sinuk  = scuk.sin();
        final double cosuk  = scuk.cos();
        final double sinik  = scik.sin();
        final double cosik  = scik.cos();
        final double sinnok = scnok.sin();
        final double cosnok = scnok.cos();
        final double xmx = -sinnok * cosik;
        final double xmy = cosnok * cosik;
        final double ux  = xmx * sinuk + cosnok * cosuk;
        final double uy  = xmy * sinuk + sinnok * cosuk;
        final double uz  = sinik * sinuk;

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
    protected abstract void sxpPropagate(double t);

    /** {@inheritDoc}
     * <p>
     * For TLE propagator, calling this method is only recommended
     * for covariance propagation when the new <code>state</code>
     * differs from the previous one by only adding the additional
     * state containing the derivatives.
     * </p>
     */
    public void resetInitialState(final SpacecraftState state) {
        super.resetInitialState(state);
        super.setStartDate(state.getDate());
        final TleGenerationAlgorithm algorithm = getDefaultTleGenerationAlgorithm(utc, teme);
        final TLE newTLE = algorithm.generate(state, tle);
        this.tle = newTLE;
        initializeCommons();
        sxpInitialize();
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

    /** {@inheritDoc} */
    @Override
    protected AbstractMatricesHarvester createHarvester(final String stmName, final RealMatrix initialStm,
                                                        final DoubleArrayDictionary initialJacobianColumns) {
        // Create the harvester
        final TLEHarvester harvester = new TLEHarvester(this, stmName, initialStm, initialJacobianColumns);
        // Update the list of additional state provider
        addAdditionalStateProvider(harvester);
        // Return the configured harvester
        return harvester;
    }

    /**
     * Get the names of the parameters in the matrix returned by {@link MatricesHarvester#getParametersJacobian}.
     * @return names of the parameters (i.e. columns) of the Jacobian matrix
     */
    protected List<String> getJacobiansColumnsNames() {
        final List<String> columnsNames = new ArrayList<>();
        for (final ParameterDriver driver : tle.getParametersDrivers()) {

            if (driver.isSelected() && !columnsNames.contains(driver.getNamesSpanMap().getFirstSpan().getData())) {
                // As driver with same name should have same NamesSpanMap we only check the if condition on the
                // first span map and then if the condition is OK all the span names are added to the jacobian column names
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    columnsNames.add(span.getData());
                }
            }
        }
        Collections.sort(columnsNames);
        return columnsNames;
    }

    /**
     * Get the default TLE generation algorithm.
     * @param utc UTC time scale
     * @param teme TEME frame
     * @return a TLE generation algorithm
     * @since 12.0
     */
    public static TleGenerationAlgorithm getDefaultTleGenerationAlgorithm(final TimeScale utc, final Frame teme) {
        return new FixedPointTleGenerationAlgorithm(FixedPointTleGenerationAlgorithm.EPSILON_DEFAULT,
                                                    FixedPointTleGenerationAlgorithm.MAX_ITERATIONS_DEFAULT,
                                                    FixedPointTleGenerationAlgorithm.SCALE_DEFAULT, utc, teme);
    }

}
