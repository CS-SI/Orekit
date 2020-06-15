/* Copyright 2002-2020 CS GROUP
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
package org.orekit.propagation.analytical.gnss;

import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.Precision;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.AbstractAnalyticalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.GLONASSDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

/**
 * This class aims at propagating a GLONASS orbit from {@link GLONASSOrbitalElements}.
 *
 * @see <a href="http://russianspacesystems.ru/wp-content/uploads/2016/08/ICD-GLONASS-CDMA-General.-Edition-1.0-2016.pdf">
 *       GLONASS Interface Control Document</a>
 *
 * @author Bryan Cazabonne
 * @since 10.0
 *
 */
public class GLONASSAnalyticalPropagator extends AbstractAnalyticalPropagator {

    // Constants
    /** Constant 7.0 / 3.0. */
    private static final double SEVEN_THIRD = 7.0 / 3.0;

    /** Constant 7.0 / 6.0. */
    private static final double SEVEN_SIXTH = 7.0 / 6.0;

    /** Constant 7.0 / 24.0. */
    private static final double SEVEN_24TH = 7.0 / 24.0;

    /** Constant 49.0 / 72.0. */
    private static final double FN_72TH = 49.0 / 72.0;

    /** Value of the earth's rotation rate in rad/s. */
    private static final double GLONASS_AV = 7.2921150e-5;

    /** Mean value of inclination for Glonass orbit is equal to 63°. */
    private static final double GLONASS_MEAN_INCLINATION = 64.8;

    /** Mean value of Draconian period for Glonass orbit is equal to 40544s : 11 hours 15 minutes 44 seconds. */
    private static final double GLONASS_MEAN_DRACONIAN_PERIOD = 40544;

    /** Second degree zonal coefficient of normal potential. */
    private static final double GLONASS_J20 = 1.08262575e-3;

    /** Equatorial radius of Earth (m). */
    private static final double GLONASS_EARTH_EQUATORIAL_RADIUS = 6378136;

    // Data used to solve Kepler's equation
    /** First coefficient to compute Kepler equation solver starter. */
    private static final double A;

    /** Second coefficient to compute Kepler equation solver starter. */
    private static final double B;

    static {
        final double k1 = 3 * FastMath.PI + 2;
        final double k2 = FastMath.PI - 1;
        final double k3 = 6 * FastMath.PI - 1;
        A  = 3 * k2 * k2 / k1;
        B  = k3 * k3 / (6 * k1);
    }

    // Fields
    /** The GLONASS orbital elements used. */
    private final GLONASSOrbitalElements glonassOrbit;

    /** The spacecraft mass (kg). */
    private final double mass;

    /** The ECI frame used for GLONASS propagation. */
    private final Frame eci;

    /** The ECEF frame used for GLONASS propagation. */
    private final Frame ecef;

    /** Data context for propagation. */
    private final DataContext dataContext;

    /**
     * This nested class aims at building a GLONASSPropagator.
     * <p>It implements the classical builder pattern.</p>
     *
     */
    public static class Builder {

        // Required parameter
        /** The GLONASS orbital elements. */
        private final GLONASSOrbitalElements orbit;

        // Optional parameters
        /** The attitude provider. */
        private AttitudeProvider attitudeProvider;
        /** The mass. */
        private double mass = DEFAULT_MASS;
        /** The ECI frame. */
        private Frame eci  = null;
        /** The ECEF frame. */
        private Frame ecef = null;
        /** Data context. */
        private DataContext dataContext;

        /** Initializes the builder.
         * <p>The GLONASS orbital elements is the only requested parameter to build a GLONASSPropagator.</p>
         * <p>The attitude provider is set by default to the
         *  {@link org.orekit.propagation.Propagator#DEFAULT_LAW DEFAULT_LAW} in the
         *  default data context.<br>
         * The mass is set by default to the
         *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
         * The data context is by default to the
         *  {@link DataContext#getDefault() default data context}.<br>
         * The ECI frame is set by default to the
         *  {@link org.orekit.frames.Predefined#EME2000 EME2000 frame} in the default data
         *  context.<br>
         * The ECEF frame is set by default to the
         *  {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP
         *  CIO/2010-based ITRF simple EOP} in the default data context.
         * </p>
         *
         * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
         * Another data context can be set using
         * {@code Builder(final GLONASSOrbitalElements gpsOrbElt, final DataContext dataContext)}</p>
         *
         * @param glonassOrbElt the GLONASS orbital elements to be used by the GLONASS propagator.
         * @see #attitudeProvider(AttitudeProvider provider)
         * @see #mass(double mass)
         * @see #eci(Frame inertial)
         * @see #ecef(Frame bodyFixed)
         */
        @DefaultDataContext
        public Builder(final GLONASSOrbitalElements glonassOrbElt) {
            this(glonassOrbElt, DataContext.getDefault());
        }

        /** Initializes the builder.
         * <p>The GLONASS orbital elements is the only requested parameter to build a GLONASSPropagator.</p>
         * <p>The attitude provider is set by default to the
         *  {@link org.orekit.propagation.Propagator#getDefaultLaw(Frames)}.<br>
         * The mass is set by default to the
         *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
         * The ECI frame is set by default to the
         *  {@link Frames#getEME2000() EME2000 frame}.<br>
         * The ECEF frame is set by default to the
         *  {@link Frames#getITRF(IERSConventions, boolean) CIO/2010-based ITRF simple
         *  EOP}.
         * </p>
         *
         * @param glonassOrbElt the GLONASS orbital elements to be used by the GLONASS propagator.
         * @param dataContext the data context to use for frames and time scales.
         * @see #attitudeProvider(AttitudeProvider provider)
         * @see #mass(double mass)
         * @see #eci(Frame inertial)
         * @see #ecef(Frame bodyFixed)
         * @since 10.1
         */
        public Builder(final GLONASSOrbitalElements glonassOrbElt,
                       final DataContext dataContext) {
            this.orbit = glonassOrbElt;
            this.dataContext = dataContext;
            final Frames frames = dataContext.getFrames();
            this.eci   = frames.getEME2000();
            this.ecef  = frames.getITRF(IERSConventions.IERS_2010, true);
            attitudeProvider = Propagator.getDefaultLaw(frames);
        }

        /** Sets the attitude provider.
         *
         * @param userProvider the attitude provider
         * @return the updated builder
         */
        public Builder attitudeProvider(final AttitudeProvider userProvider) {
            this.attitudeProvider = userProvider;
            return this;
        }

        /** Sets the mass.
         *
         * @param userMass the mass (in kg)
         * @return the updated builder
         */
        public Builder mass(final double userMass) {
            this.mass = userMass;
            return this;
        }

        /** Sets the Earth Centered Inertial frame used for propagation.
         *
         * @param inertial the ECI frame
         * @return the updated builder
         */
        public Builder eci(final Frame inertial) {
            this.eci = inertial;
            return this;
        }

        /** Sets the Earth Centered Earth Fixed frame assimilated to the WGS84 ECEF.
         *
         * @param bodyFixed the ECEF frame
         * @return the updated builder
         */
        public Builder ecef(final Frame bodyFixed) {
            this.ecef = bodyFixed;
            return this;
        }

        /**
         * Sets the data context used by the propagator. Does not update the ECI or ECEF
         * frames which must be done separately using {@link #eci(Frame)} and {@link
         * #ecef(Frame)}.
         *
         * @param context used for propagation.
         * @return the updated builder.
         */
        public Builder dataContext(final DataContext context) {
            this.dataContext = context;
            return this;
        }

        /** Finalizes the build.
         *
         * @return the built GLONASSPropagator
         */
        public GLONASSAnalyticalPropagator build() {
            return new GLONASSAnalyticalPropagator(this);
        }

    }

    /**
     * Private constructor.
     * @param builder the builder
     */
    private GLONASSAnalyticalPropagator(final Builder builder) {
        super(builder.attitudeProvider);
        this.dataContext = builder.dataContext;
        // Stores the GLONASS orbital elements
        this.glonassOrbit = builder.orbit;
        // Sets the start date as the date of the orbital elements
        setStartDate(glonassOrbit.getDate());
        // Sets the mass
        this.mass = builder.mass;
        // Sets the Earth Centered Inertial frame
        this.eci  = builder.eci;
        // Sets the Earth Centered Earth Fixed frame
        this.ecef = builder.ecef;
    }

    /**
     * Gets the PVCoordinates of the GLONASS SV in {@link #getECEF() ECEF frame}.
     *
     * <p>The algorithm is defined at Appendix M.1 from GLONASS Interface Control Document,
     * with automatic differentiation added to compute velocity and
     * acceleration.</p>
     *
     * @param date the computation date
     * @return the GLONASS SV PVCoordinates in {@link #getECEF() ECEF frame}
     */
    public PVCoordinates propagateInEcef(final AbsoluteDate date) {

        // Interval of prediction dTpr
        final UnivariateDerivative2 dTpr = getdTpr(date);

        // Zero
        final UnivariateDerivative2 zero = dTpr.getField().getZero();

        // The number of whole orbits "w" on a prediction interval
        final UnivariateDerivative2 w = FastMath.floor(dTpr.divide(GLONASS_MEAN_DRACONIAN_PERIOD + glonassOrbit.getDeltaT()));

        // Current inclination
        final UnivariateDerivative2 i = zero.add(GLONASS_MEAN_INCLINATION / 180 * GLONASSOrbitalElements.GLONASS_PI + glonassOrbit.getDeltaI());

        // Eccentricity
        final UnivariateDerivative2 e = zero.add(glonassOrbit.getE());

        // Mean draconique period in orbite w+1 and mean motion
        final UnivariateDerivative2 tDR = w.multiply(2.0).add(1.0).multiply(glonassOrbit.getDeltaTDot()).
                                          add(glonassOrbit.getDeltaT()).
                                          add(GLONASS_MEAN_DRACONIAN_PERIOD);
        final UnivariateDerivative2 n = tDR.divide(2.0 * GLONASSOrbitalElements.GLONASS_PI).reciprocal();

        // Semi-major axis : computed by successive approximation
        final UnivariateDerivative2 sma = computeSma(tDR, i, e);

        // (ae / p)^2 term
        final UnivariateDerivative2 p     = sma.multiply(e.multiply(e).negate().add(1.0));
        final UnivariateDerivative2 aeop  = p.divide(GLONASS_EARTH_EQUATORIAL_RADIUS).reciprocal();
        final UnivariateDerivative2 aeop2 = aeop.multiply(aeop);

        // Current longitude of the ascending node
        final UnivariateDerivative2 lambda = computeLambda(dTpr, n, aeop2, i);

        // Current argument of perigee
        final UnivariateDerivative2 pa = computePA(dTpr, n, aeop2, i);

        // Mean longitude at the instant the spacecraft passes the current ascending node
        final UnivariateDerivative2 tanPAo2 = FastMath.tan(pa.divide(2.0));
        final UnivariateDerivative2 coef    = tanPAo2.multiply(FastMath.sqrt(e.negate().add(1.0).divide(e.add(1.0))));
        final UnivariateDerivative2 e0      = FastMath.atan(coef).multiply(2.0).negate();
        final UnivariateDerivative2 m1      = pa.add(e0).subtract(FastMath.sin(e0).multiply(e));

        // Current mean longitude
        final UnivariateDerivative2 correction = dTpr.
                                                 subtract(w.multiply(GLONASS_MEAN_DRACONIAN_PERIOD + glonassOrbit.getDeltaT())).
                                                 subtract(w.multiply(w).multiply(glonassOrbit.getDeltaTDot()));
        final UnivariateDerivative2 m = m1.add(n.multiply(correction));

        // Take into consideration the periodic perturbations
        final UnivariateDerivative2 h = e.multiply(FastMath.sin(pa));
        final UnivariateDerivative2 l = e.multiply(FastMath.cos(pa));
        // δa1
        final UnivariateDerivative2[] d1 = getParameterDifferentials(sma, i, h, l, m1);
        // δa2
        final UnivariateDerivative2[] d2 = getParameterDifferentials(sma, i, h, l, m);
        // Apply corrections
        final UnivariateDerivative2 smaCorr    = sma.add(d2[0]).subtract(d1[0]);
        final UnivariateDerivative2 hCorr      = h.add(d2[1]).subtract(d1[1]);
        final UnivariateDerivative2 lCorr      = l.add(d2[2]).subtract(d1[2]);
        final UnivariateDerivative2 lambdaCorr = lambda.add(d2[3]).subtract(d1[3]);
        final UnivariateDerivative2 iCorr      = i.add(d2[4]).subtract(d1[4]);
        final UnivariateDerivative2 mCorr      = m.add(d2[5]).subtract(d1[5]);
        final UnivariateDerivative2 eCorr      = FastMath.sqrt(hCorr.multiply(hCorr).add(lCorr.multiply(lCorr)));
        final UnivariateDerivative2 paCorr;
        if (eCorr.getValue() == 0.) {
            paCorr = zero;
        } else {
            if (lCorr.getValue() == eCorr.getValue()) {
                paCorr = zero.add(0.5 * GLONASSOrbitalElements.GLONASS_PI);
            } else if (lCorr.getValue() == -eCorr.getValue()) {
                paCorr = zero.add(-0.5 * GLONASSOrbitalElements.GLONASS_PI);
            } else {
                paCorr = FastMath.atan2(hCorr, lCorr);
            }
        }

        // Eccentric Anomaly
        final UnivariateDerivative2 mk = mCorr.subtract(paCorr);
        final UnivariateDerivative2 ek = getEccentricAnomaly(mk, eCorr);

        // True Anomaly
        final UnivariateDerivative2 vk =  getTrueAnomaly(ek, eCorr);

        // Argument of Latitude
        final UnivariateDerivative2 phik = vk.add(paCorr);

        // Corrected Radius
        final UnivariateDerivative2 pCorr = smaCorr.multiply(eCorr.multiply(eCorr).negate().add(1.0));
        final UnivariateDerivative2 rk    = pCorr.divide(eCorr.multiply(FastMath.cos(vk)).add(1.0));

        // Positions in orbital plane
        final UnivariateDerivative2 xk = FastMath.cos(phik).multiply(rk);
        final UnivariateDerivative2 yk = FastMath.sin(phik).multiply(rk);

        // Coordinates of position
        final UnivariateDerivative2 cosL = FastMath.cos(lambdaCorr);
        final UnivariateDerivative2 sinL = FastMath.sin(lambdaCorr);
        final UnivariateDerivative2 cosI = FastMath.cos(iCorr);
        final UnivariateDerivative2 sinI = FastMath.sin(iCorr);
        final FieldVector3D<UnivariateDerivative2> positionwithDerivatives =
                        new FieldVector3D<>(xk.multiply(cosL).subtract(yk.multiply(sinL).multiply(cosI)),
                                            xk.multiply(sinL).add(yk.multiply(cosL).multiply(cosI)),
                                            yk.multiply(sinI));

        return new PVCoordinates(new Vector3D(positionwithDerivatives.getX().getValue(),
                                              positionwithDerivatives.getY().getValue(),
                                              positionwithDerivatives.getZ().getValue()),
                                 new Vector3D(positionwithDerivatives.getX().getFirstDerivative(),
                                              positionwithDerivatives.getY().getFirstDerivative(),
                                              positionwithDerivatives.getZ().getFirstDerivative()),
                                 new Vector3D(positionwithDerivatives.getX().getSecondDerivative(),
                                              positionwithDerivatives.getY().getSecondDerivative(),
                                              positionwithDerivatives.getZ().getSecondDerivative()));
    }

    /**
     * Gets eccentric anomaly from mean anomaly.
     * <p>The algorithm used to solve the Kepler equation has been published in:
     * "Procedures for  solving Kepler's Equation", A. W. Odell and R. H. Gooding,
     * Celestial Mechanics 38 (1986) 307-334</p>
     * <p>It has been copied from the OREKIT library (KeplerianOrbit class).</p>
     *
     * @param mk the mean anomaly (rad)
     * @param e the eccentricity
     * @return the eccentric anomaly (rad)
     */
    private UnivariateDerivative2 getEccentricAnomaly(final UnivariateDerivative2 mk, final UnivariateDerivative2 e) {

        // reduce M to [-PI PI] interval
        final UnivariateDerivative2 reducedM = new UnivariateDerivative2(MathUtils.normalizeAngle(mk.getValue(), 0.0),
                                                                         mk.getFirstDerivative(),
                                                                         mk.getSecondDerivative());

        // compute start value according to A. W. Odell and R. H. Gooding S12 starter
        UnivariateDerivative2 ek;
        if (FastMath.abs(reducedM.getValue()) < 1.0 / 6.0) {
            if (FastMath.abs(reducedM.getValue()) < Precision.SAFE_MIN) {
                // this is an Orekit change to the S12 starter.
                // If reducedM is 0.0, the derivative of cbrt is infinite which induces NaN appearing later in
                // the computation. As in this case E and M are almost equal, we initialize ek with reducedM
                ek = reducedM;
            } else {
                // this is the standard S12 starter
                ek = reducedM.add(reducedM.multiply(6).cbrt().subtract(reducedM).multiply(e));
            }
        } else {
            if (reducedM.getValue() < 0) {
                final UnivariateDerivative2 w = reducedM.add(FastMath.PI);
                ek = reducedM.add(w.multiply(-A).divide(w.subtract(B)).subtract(FastMath.PI).subtract(reducedM).multiply(e));
            } else {
                final UnivariateDerivative2 minusW = reducedM.subtract(FastMath.PI);
                ek = reducedM.add(minusW.multiply(A).divide(minusW.add(B)).add(FastMath.PI).subtract(reducedM).multiply(e));
            }
        }

        final UnivariateDerivative2 e1 = e.negate().add(1.0);
        final boolean noCancellationRisk = (e1.getValue() + ek.getValue() * ek.getValue() / 6) >= 0.1;

        // perform two iterations, each consisting of one Halley step and one Newton-Raphson step
        for (int j = 0; j < 2; ++j) {
            final UnivariateDerivative2 f;
            UnivariateDerivative2 fd;
            final UnivariateDerivative2 fdd  = ek.sin().multiply(e);
            final UnivariateDerivative2 fddd = ek.cos().multiply(e);
            if (noCancellationRisk) {
                f  = ek.subtract(fdd).subtract(reducedM);
                fd = fddd.subtract(1).negate();
            } else {
                f  = eMeSinE(ek, e).subtract(reducedM);
                final UnivariateDerivative2 s = ek.multiply(0.5).sin();
                fd = s.multiply(s).multiply(e.multiply(2.0)).add(e1);
            }
            final UnivariateDerivative2 dee = f.multiply(fd).divide(f.multiply(0.5).multiply(fdd).subtract(fd.multiply(fd)));

            // update eccentric anomaly, using expressions that limit underflow problems
            final UnivariateDerivative2 w = fd.add(dee.multiply(0.5).multiply(fdd.add(dee.multiply(fdd).divide(3))));
            fd = fd.add(dee.multiply(fdd.add(dee.multiply(0.5).multiply(fdd))));
            ek = ek.subtract(f.subtract(dee.multiply(fd.subtract(w))).divide(fd));
        }

        // expand the result back to original range
        ek = ek.add(mk.getValue() - reducedM.getValue());

        // Returns the eccentric anomaly
        return ek;
    }

    /**
     * Accurate computation of E - e sin(E).
     *
     * @param E eccentric anomaly
     * @param ecc the eccentricity
     * @return E - e sin(E)
     */
    private UnivariateDerivative2 eMeSinE(final UnivariateDerivative2 E, final UnivariateDerivative2 ecc) {
        UnivariateDerivative2 x = E.sin().multiply(ecc.negate().add(1.0));
        final UnivariateDerivative2 mE2 = E.negate().multiply(E);
        UnivariateDerivative2 term = E;
        UnivariateDerivative2 d    = E.getField().getZero();
        // the inequality test below IS intentional and should NOT be replaced by a check with a small tolerance
        for (UnivariateDerivative2 x0 = d.add(Double.NaN); !Double.valueOf(x.getValue()).equals(Double.valueOf(x0.getValue()));) {
            d = d.add(2);
            term = term.multiply(mE2.divide(d.multiply(d.add(1))));
            x0 = x;
            x = x.subtract(term);
        }
        return x;
    }

    /** Gets true anomaly from eccentric anomaly.
    *
    * @param ek the eccentric anomaly (rad)
    * @param ecc the eccentricity
    * @return the true anomaly (rad)
    */
    private UnivariateDerivative2 getTrueAnomaly(final UnivariateDerivative2 ek, final UnivariateDerivative2 ecc) {
        final UnivariateDerivative2 svk = ek.sin().multiply(FastMath.sqrt( ecc.multiply(ecc).negate().add(1.0)));
        final UnivariateDerivative2 cvk = ek.cos().subtract(ecc);
        return svk.atan2(cvk);
    }

    /**
     * Get the interval of prediction.
     *
     * @param date the considered date
     * @return the duration from GLONASS orbit Reference epoch (s)
     */
    private UnivariateDerivative2 getdTpr(final AbsoluteDate date) {
        final TimeScale glonass = dataContext.getTimeScales().getGLONASS();
        final GLONASSDate tEnd = new GLONASSDate(date, glonass);
        final GLONASSDate tSta = new GLONASSDate(glonassOrbit.getDate(), glonass);
        final int n  = tEnd.getDayNumber();
        final int na = tSta.getDayNumber();
        final int deltaN;
        if (na == 27) {
            deltaN = n - na - FastMath.round((float) (n - na) / 1460) * 1460;
        } else {
            deltaN = n - na - FastMath.round((float) (n - na) / 1461) * 1461;
        }
        final UnivariateDerivative2 ti = new UnivariateDerivative2(tEnd.getSecInDay(), 1.0, 0.0);

        return ti.subtract(glonassOrbit.getTime()).add(86400 * deltaN);
    }

    /**
     * Computes the semi-major axis of orbit using technique of successive approximations.
     * @param tDR mean draconique period (s)
     * @param i current inclination (rad)
     * @param e eccentricity
     * @return the semi-major axis (m).
     */
    private UnivariateDerivative2 computeSma(final UnivariateDerivative2 tDR,
                                             final UnivariateDerivative2 i,
                                             final UnivariateDerivative2 e) {

        // Zero
        final UnivariateDerivative2 zero = tDR.getField().getZero();

        // If one of the input parameter is equal to Double.NaN, an infinite loop can occur.
        // In that case, we do not compute the value of the semi major axis.
        // We decided to return a Double.NaN value instead.
        if (Double.isNaN(tDR.getValue()) || Double.isNaN(i.getValue()) || Double.isNaN(e.getValue())) {
            return zero.add(Double.NaN);
        }

        // Common parameters
        final UnivariateDerivative2 sinI         = FastMath.sin(i);
        final UnivariateDerivative2 sin2I        = sinI.multiply(sinI);
        final UnivariateDerivative2 ome2         = e.multiply(e).negate().add(1.0);
        final UnivariateDerivative2 ome2Pow3o2   = FastMath.sqrt(ome2).multiply(ome2);
        final UnivariateDerivative2 pa           = zero.add(glonassOrbit.getPa());
        final UnivariateDerivative2 cosPA        = FastMath.cos(pa);
        final UnivariateDerivative2 opecosPA     = e.multiply(cosPA).add(1.0);
        final UnivariateDerivative2 opecosPAPow2 = opecosPA.multiply(opecosPA);
        final UnivariateDerivative2 opecosPAPow3 = opecosPAPow2.multiply(opecosPA);

        // Initial approximation
        UnivariateDerivative2 tOCK = tDR;

        // Successive approximations
        // The process of approximation ends when fulfilling the following condition: |a(n+1) - a(n)| < 1cm
        UnivariateDerivative2 an   = zero;
        UnivariateDerivative2 anp1 = zero;
        boolean isLastStep = false;
        while (!isLastStep) {

            // a(n+1) computation
            final UnivariateDerivative2 tOCKo2p     = tOCK.divide(2.0 * GLONASSOrbitalElements.GLONASS_PI);
            final UnivariateDerivative2 tOCKo2pPow2 = tOCKo2p.multiply(tOCKo2p);
            anp1 = FastMath.cbrt(tOCKo2pPow2.multiply(GLONASSOrbitalElements.GLONASS_MU));

            // p(n+1) computation
            final UnivariateDerivative2 p = anp1.multiply(ome2);

            // Tock(n+1) computation
            final UnivariateDerivative2 aeop  = p.divide(GLONASS_EARTH_EQUATORIAL_RADIUS).reciprocal();
            final UnivariateDerivative2 aeop2 = aeop.multiply(aeop);
            final UnivariateDerivative2 term1 = aeop2.multiply(GLONASS_J20).multiply(1.5);
            final UnivariateDerivative2 term2 = sin2I.multiply(2.5).negate().add(2.0);
            final UnivariateDerivative2 term3 = ome2Pow3o2.divide(opecosPAPow2);
            final UnivariateDerivative2 term4 = opecosPAPow3.divide(ome2);
            tOCK = tDR.divide(term1.multiply(term2.multiply(term3).add(term4)).negate().add(1.0));

            // Check convergence
            if (FastMath.abs(anp1.subtract(an).getReal()) <= 0.01) {
                isLastStep = true;
            }

            an = anp1;
        }

        return an;

    }

    /**
     * Computes the current longitude of the ascending node.
     * @param dTpr interval of prediction (s)
     * @param n mean motion (rad/s)
     * @param aeop2 square of the ratio between the radius of the ellipsoid and p, with p = sma * (1 - ecc²)
     * @param i inclination (rad)
     * @return the current longitude of the ascending node (rad)
     */
    private UnivariateDerivative2 computeLambda(final UnivariateDerivative2 dTpr,
                                                final UnivariateDerivative2 n,
                                                final UnivariateDerivative2 aeop2,
                                                final UnivariateDerivative2 i) {
        final UnivariateDerivative2 cosI = FastMath.cos(i);
        final UnivariateDerivative2 precession = aeop2.multiply(n).multiply(cosI).multiply(1.5 * GLONASS_J20);
        return dTpr.multiply(precession.add(GLONASS_AV)).negate().add(glonassOrbit.getLambda());
    }

    /**
     * Computes the current argument of perigee.
     * @param dTpr interval of prediction (s)
     * @param n mean motion (rad/s)
     * @param aeop2 square of the ratio between the radius of the ellipsoid and p, with p = sma * (1 - ecc²)
     * @param i inclination (rad)
     * @return the current argument of perigee (rad)
     */
    private UnivariateDerivative2 computePA(final UnivariateDerivative2 dTpr,
                                            final UnivariateDerivative2 n,
                                            final UnivariateDerivative2 aeop2,
                                            final UnivariateDerivative2 i) {
        final UnivariateDerivative2 cosI  = FastMath.cos(i);
        final UnivariateDerivative2 cos2I = cosI.multiply(cosI);
        final UnivariateDerivative2 precession = aeop2.multiply(n).multiply(cos2I.multiply(5.0).negate().add(1.0)).multiply(0.75 * GLONASS_J20);
        return dTpr.multiply(precession).negate().add(glonassOrbit.getPa());
    }

    /**
     * Computes the differentials δa<sub>i</sub>.
     * <p>
     * The value of i depends of the type of longitude (i = 2 for the current mean longitude;
     * i = 1 for the mean longitude at the instant the spacecraft passes the current ascending node)
     * </p>
     * @param a semi-major axis (m)
     * @param i inclination (rad)
     * @param h x component of the eccentricity (rad)
     * @param l y component of the eccentricity (rad)
     * @param m longitude (current or at the ascending node instant)
     * @return the differentials of the orbital parameters
     */
    private UnivariateDerivative2[] getParameterDifferentials(final UnivariateDerivative2 a, final UnivariateDerivative2 i,
                                                              final UnivariateDerivative2 h, final UnivariateDerivative2 l,
                                                              final UnivariateDerivative2 m) {

        // B constant
        final UnivariateDerivative2 aeoa  = a.divide(GLONASS_EARTH_EQUATORIAL_RADIUS).reciprocal();
        final UnivariateDerivative2 aeoa2 = aeoa.multiply(aeoa);
        final UnivariateDerivative2 b     = aeoa2.multiply(1.5 * GLONASS_J20);

        // Commons Parameters
        final UnivariateDerivative2 cosI   = FastMath.cos(i);
        final UnivariateDerivative2 sinI   = FastMath.sin(i);
        final UnivariateDerivative2 cosI2  = cosI.multiply(cosI);
        final UnivariateDerivative2 sinI2  = sinI.multiply(sinI);
        final UnivariateDerivative2 cosLk  = FastMath.cos(m);
        final UnivariateDerivative2 sinLk  = FastMath.sin(m);
        final UnivariateDerivative2 cos2Lk = FastMath.cos(m.multiply(2.0));
        final UnivariateDerivative2 sin2Lk = FastMath.sin(m.multiply(2.0));
        final UnivariateDerivative2 cos3Lk = FastMath.cos(m.multiply(3.0));
        final UnivariateDerivative2 sin3Lk = FastMath.sin(m.multiply(3.0));
        final UnivariateDerivative2 cos4Lk = FastMath.cos(m.multiply(4.0));
        final UnivariateDerivative2 sin4Lk = FastMath.sin(m.multiply(4.0));

        // h*cos(nLk), l*cos(nLk), h*sin(nLk) and l*sin(nLk)
        // n = 1
        final UnivariateDerivative2 hCosLk = h.multiply(cosLk);
        final UnivariateDerivative2 hSinLk = h.multiply(sinLk);
        final UnivariateDerivative2 lCosLk = l.multiply(cosLk);
        final UnivariateDerivative2 lSinLk = l.multiply(sinLk);
        // n = 2
        final UnivariateDerivative2 hCos2Lk = h.multiply(cos2Lk);
        final UnivariateDerivative2 hSin2Lk = h.multiply(sin2Lk);
        final UnivariateDerivative2 lCos2Lk = l.multiply(cos2Lk);
        final UnivariateDerivative2 lSin2Lk = l.multiply(sin2Lk);
        // n = 3
        final UnivariateDerivative2 hCos3Lk = h.multiply(cos3Lk);
        final UnivariateDerivative2 hSin3Lk = h.multiply(sin3Lk);
        final UnivariateDerivative2 lCos3Lk = l.multiply(cos3Lk);
        final UnivariateDerivative2 lSin3Lk = l.multiply(sin3Lk);
        // n = 4
        final UnivariateDerivative2 hCos4Lk = h.multiply(cos4Lk);
        final UnivariateDerivative2 hSin4Lk = h.multiply(sin4Lk);
        final UnivariateDerivative2 lCos4Lk = l.multiply(cos4Lk);
        final UnivariateDerivative2 lSin4Lk = l.multiply(sin4Lk);

        // 1 - (3 / 2)*sin²i
        final UnivariateDerivative2 om3o2xSinI2 = sinI2.multiply(1.5).negate().add(1.0);

        // Compute Differentials
        // δa
        final UnivariateDerivative2 dakT1 = b.multiply(2.0).multiply(om3o2xSinI2).multiply(lCosLk.add(hSinLk));
        final UnivariateDerivative2 dakT2 = b.multiply(sinI2).multiply(hSinLk.multiply(0.5).subtract(lCosLk.multiply(0.5)).
                                                                     add(cos2Lk).add(lCos3Lk.multiply(3.5)).add(hSin3Lk.multiply(3.5)));
        final UnivariateDerivative2 dak = dakT1.add(dakT2);

        // δh
        final UnivariateDerivative2 dhkT1 = b.multiply(om3o2xSinI2).multiply(sinLk.add(lSin2Lk.multiply(1.5)).subtract(hCos2Lk.multiply(1.5)));
        final UnivariateDerivative2 dhkT2 = b.multiply(sinI2).multiply(0.25).multiply(sinLk.subtract(sin3Lk.multiply(SEVEN_THIRD)).add(lSin2Lk.multiply(5.0)).
                                                                                    subtract(lSin4Lk.multiply(8.5)).add(hCos4Lk.multiply(8.5)).add(hCos2Lk));
        final UnivariateDerivative2 dhkT3 = lSin2Lk.multiply(cosI2).multiply(b).multiply(0.5).negate();
        final UnivariateDerivative2 dhk = dhkT1.subtract(dhkT2).add(dhkT3);

        // δl
        final UnivariateDerivative2 dlkT1 = b.multiply(om3o2xSinI2).multiply(cosLk.add(lCos2Lk.multiply(1.5)).add(hSin2Lk.multiply(1.5)));
        final UnivariateDerivative2 dlkT2 = b.multiply(sinI2).multiply(0.25).multiply(cosLk.negate().subtract(cos3Lk.multiply(SEVEN_THIRD)).subtract(hSin2Lk.multiply(5.0)).
                                                                                    subtract(lCos4Lk.multiply(8.5)).subtract(hSin4Lk.multiply(8.5)).add(lCos2Lk));
        final UnivariateDerivative2 dlkT3 = hSin2Lk.multiply(cosI2).multiply(b).multiply(0.5);
        final UnivariateDerivative2 dlk = dlkT1.subtract(dlkT2).add(dlkT3);

        // δλ
        final UnivariateDerivative2 dokT1 = b.negate().multiply(cosI);
        final UnivariateDerivative2 dokT2 = lSinLk.multiply(3.5).subtract(hCosLk.multiply(2.5)).subtract(sin2Lk.multiply(0.5)).
                                          subtract(lSin3Lk.multiply(SEVEN_SIXTH)).add(hCos3Lk.multiply(SEVEN_SIXTH));
        final UnivariateDerivative2 dok = dokT1.multiply(dokT2);

        // δi
        final UnivariateDerivative2 dik = b.multiply(sinI).multiply(cosI).multiply(0.5).
                        multiply(lCosLk.negate().add(hSinLk).add(cos2Lk).add(lCos3Lk.multiply(SEVEN_THIRD)).add(hSin3Lk.multiply(SEVEN_THIRD)));

        // δL
        final UnivariateDerivative2 dLkT1 = b.multiply(2.0).multiply(om3o2xSinI2).multiply(lSinLk.multiply(1.75).subtract(hCosLk.multiply(1.75)));
        final UnivariateDerivative2 dLkT2 = b.multiply(sinI2).multiply(3.0).multiply(hCosLk.multiply(SEVEN_24TH).negate().subtract(lSinLk.multiply(SEVEN_24TH)).
                                                                                   subtract(hCos3Lk.multiply(FN_72TH)).add(lSin3Lk.multiply(FN_72TH)).add(sin2Lk.multiply(0.25)));
        final UnivariateDerivative2 dLkT3 = b.multiply(cosI2).multiply(lSinLk.multiply(3.5).subtract(hCosLk.multiply(2.5)).subtract(sin2Lk.multiply(0.5)).
                                                                     subtract(lSin3Lk.multiply(SEVEN_SIXTH)).add(hCos3Lk.multiply(SEVEN_SIXTH)));
        final UnivariateDerivative2 dLk = dLkT1.add(dLkT2).add(dLkT3);

        // Final array
        final UnivariateDerivative2[] differentials = MathArrays.buildArray(a.getField(), 6);
        differentials[0] = dak.multiply(a);
        differentials[1] = dhk;
        differentials[2] = dlk;
        differentials[3] = dok;
        differentials[4] = dik;
        differentials[5] = dLk;

        return differentials;
    }

    /** {@inheritDoc} */
    protected double getMass(final AbsoluteDate date) {
        return mass;
    }

    /**
     * Get the Earth gravity coefficient used for GLONASS propagation.
     * @return the Earth gravity coefficient.
     */
    public static double getMU() {
        return GLONASSOrbitalElements.GLONASS_MU;
    }

    /**
     * Gets the underlying GLONASS orbital elements.
     *
     * @return the underlying GLONASS orbital elements
     */
    public GLONASSOrbitalElements getGLONASSOrbitalElements() {
        return glonassOrbit;
    }

    /**
     * Gets the Earth Centered Inertial frame used to propagate the orbit.
     * @return the ECI frame
     */
    public Frame getECI() {
        return eci;
    }

    /**
     * Gets the Earth Centered Earth Fixed frame used to propagate GLONASS orbits.
     * @return the ECEF frame
     */
    public Frame getECEF() {
        return ecef;
    }

    /** {@inheritDoc} */
    public Frame getFrame() {
        return eci;
    }

    /** {@inheritDoc} */
    public void resetInitialState(final SpacecraftState state) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    protected void resetIntermediateState(final SpacecraftState state, final boolean forward) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    protected Orbit propagateOrbit(final AbsoluteDate date) {
        // Gets the PVCoordinates in ECEF frame
        final PVCoordinates pvaInECEF = propagateInEcef(date);
        // Transforms the PVCoordinates to ECI frame
        final PVCoordinates pvaInECI = ecef.getTransformTo(eci, date).transformPVCoordinates(pvaInECEF);
        // Returns the Cartesian orbit
        return new CartesianOrbit(pvaInECI, eci, date, GLONASSOrbitalElements.GLONASS_MU);
    }

}
