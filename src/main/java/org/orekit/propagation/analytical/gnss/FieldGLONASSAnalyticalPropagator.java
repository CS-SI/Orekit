/* Copyright 2002-2021 CS GROUP
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

import java.util.Collections;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.Precision;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.FieldAbstractAnalyticalPropagator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;

/**
 * This class aims at propagating a GLONASS orbit from
 * {@link GLONASSOrbitalElements}.
 *
 * @see <a href=
 *      "http://russianspacesystems.ru/wp-content/uploads/2016/08/ICD-GLONASS-CDMA-General.-Edition-1.0-2016.pdf">
 *      GLONASS Interface Control Document</a>
 * @author Bryan Cazabonne
 * @author Nicolas Fialton (field translation)
 */
public class FieldGLONASSAnalyticalPropagator<T extends RealFieldElement<T>>
    extends
    FieldAbstractAnalyticalPropagator<T> {

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

    /**
     * Mean value of Draconian period for Glonass orbit is equal to 40544s : 11
     * hours 15 minutes 44 seconds.
     */
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
        A = 3 * k2 * k2 / k1;
        B = k3 * k3 / (6 * k1);
    }

    // Fields
    /** The GLONASS orbital elements used. */
    private final FieldGLONASSOrbitalElements<T> glonassOrbit;

    /** The spacecraft mass (kg). */
    private final T mass;

    /** The ECI frame used for GLONASS propagation. */
    private final Frame eci;

    /** The ECEF frame used for GLONASS propagation. */
    private final Frame ecef;

    /** Data context for propagation. */
    private final DataContext dataContext;

    /**
     * Default constructor.
     * <p>
     * The Field GLONASS orbital elements is the only requested parameter to
     * build a FieldGLONASSPropagator.
     * </p>
     * <p>
     * The attitude provider is set by default to the
     * {@link org.orekit.propagation.Propagator#DEFAULT_LAW DEFAULT_LAW} in the
     * default data context.<br>
     * The mass is set by default to the
     * {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
     * The data context is by default to the {@link DataContext#getDefault()
     * default data context}.<br>
     * The ECI frame is set by default to the
     * {@link org.orekit.frames.Predefined#EME2000 EME2000 frame} in the default
     * data context.<br>
     * The ECEF frame is set by default to the
     * {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP
     * CIO/2010-based ITRF simple EOP} in the default data context.
     * </p>
     * <p>
     * This constructor uses the {@link DataContext#getDefault() default data
     * context}. Another data context can be set using
     * {@code FieldGLONASSAnalyticalPropagator(final Field<T> field, final FieldGLONASSOrbitalElements<T> glonassOrbElt, final DataContext dataContext)}
     * </p>
     *
     * @param field
     * @param glonassOrbElt the Field GLONASS orbital elements to be used by the
     *        Field GLONASS propagator.
     */
    @DefaultDataContext
    public FieldGLONASSAnalyticalPropagator(final Field<T> field,
                                            final FieldGLONASSOrbitalElements<T> glonassOrbElt) {
        this(field, glonassOrbElt, DataContext.getDefault());
    }

    /**
     * Constructor.
     * <p>
     * The Field GLONASS orbital elements is the only requested parameter to
     * build a FieldGLONASSPropagator.
     * </p>
     * <p>
     * The attitude provider is set by default to the
     * {@link org.orekit.propagation.Propagator#DEFAULT_LAW DEFAULT_LAW} in the
     * default data context.<br>
     * The mass is set by default to the
     * {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
     * The data context is by default to the {@link DataContext#getDefault()
     * default data context}.<br>
     * The ECI frame is set by default to the
     * {@link org.orekit.frames.Predefined#EME2000 EME2000 frame} in the default
     * data context.<br>
     * The ECEF frame is set by default to the
     * {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP
     * CIO/2010-based ITRF simple EOP} in the default data context.
     * </p>
     * <p>
     * This constructor uses the {@link DataContext#getDefault() default data
     * context}. Another data context can be set using
     * {@code FieldGLONASSAnalyticalPropagator(final Field<T> field, final FieldGLONASSOrbitalElements<T> glonassOrbElt,
     * final DataContext dataContext, final AttitudeProvider attitudeProvider, T mass, final Frame eci, final Frame ecef)}
     * </p>
     *
     * @param field
     * @param glonassOrbElt the Field GLONASS orbital elements to be used by the
     *        Field GLONASS propagator.
     * @param dataContext the data
     */
    public FieldGLONASSAnalyticalPropagator(final Field<T> field,
                                            final FieldGLONASSOrbitalElements<T> glonassOrbElt,
                                            final DataContext dataContext) {
        this(field, glonassOrbElt, dataContext,
             Propagator.getDefaultLaw(dataContext.getFrames()),
             field.getZero().add(DEFAULT_MASS),
             dataContext.getFrames().getEME2000(),
             dataContext.getFrames().getITRF(IERSConventions.IERS_2010, true));
    }

    /**
     * Constructor. *
     * <p>
     * The Field GLONASS orbital elements is the only requested parameter to
     * build a FieldGLONASSPropagator.
     * </p>
     * <p>
     * The attitude provider is set by default to the
     * {@link org.orekit.propagation.Propagator#DEFAULT_LAW DEFAULT_LAW} in the
     * default data context.<br>
     * The mass is set by default to the
     * {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
     * The data context is by default to the {@link DataContext#getDefault()
     * default data context}.<br>
     * The ECI frame is set by default to the
     * {@link org.orekit.frames.Predefined#EME2000 EME2000 frame} in the default
     * data context.<br>
     * The ECEF frame is set by default to the
     * {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP
     * CIO/2010-based ITRF simple EOP} in the default data context.
     * </p>
     *
     * @param field
     * @param glonassOrbElt
     * @param dataContext
     * @param attitudeProvider
     * @param mass
     * @param eci
     * @param ecef
     */
    public FieldGLONASSAnalyticalPropagator(final Field<T> field,
                                            final FieldGLONASSOrbitalElements<T> glonassOrbElt,
                                            final DataContext dataContext,
                                            final AttitudeProvider attitudeProvider,
                                            final T mass, final Frame eci,
                                            final Frame ecef) {

        super(field, attitudeProvider);
        this.dataContext = dataContext;
        // Stores the GLONASS orbital elements
        this.glonassOrbit = glonassOrbElt;
        // Sets the start date as the date of the orbital elements
        setStartDate(glonassOrbit.getDate());
        // Sets the mass
        this.mass = mass;
        // Sets the Earth Centered Inertial frame
        this.eci = eci;
        // Sets the Earth Centered Earth Fixed frame
        this.ecef = ecef;
    }

    /**
     * Gets the FieldPVCoordinates of the GLONASS SV in {@link #getECEF() ECEF
     * frame}.
     * <p>
     * The algorithm is defined at Appendix M.1 from GLONASS Interface Control
     * Document, with automatic differentiation added to compute velocity and
     * acceleration.
     * </p>
     *
     * @param date the computation date
     * @return the GLONASS SV FieldPVCoordinates in {@link #getECEF() ECEF
     *         frame}
     */
    public FieldPVCoordinates<T>
        propagateInEcef(final FieldAbsoluteDate<T> date) {

        // Interval of prediction dTpr
        final FieldUnivariateDerivative2<T> dTpr = getdTpr(date);

        // Zero
        final FieldUnivariateDerivative2<T> zero = dTpr.getField().getZero();

        // The number of whole orbits "w" on a prediction interval
        final FieldUnivariateDerivative2<T> w =
            FastMath.floor(dTpr.divide(glonassOrbit.getDeltaT()
                .add(GLONASS_MEAN_DRACONIAN_PERIOD)));

        // Current inclination
        final FieldUnivariateDerivative2<T> i =
            zero.add(zero
                .add(GLONASS_MEAN_INCLINATION /
                     180 * GLONASSOrbitalElements.GLONASS_PI)
                .add(glonassOrbit.getDeltaI()));

        // Eccentricity
        final FieldUnivariateDerivative2<T> e = zero.add(glonassOrbit.getE());

        // Mean draconique period in orbite w+1 and mean motion
        final FieldUnivariateDerivative2<T> tDR =
            w.multiply(2.0).add(1.0).multiply(glonassOrbit.getDeltaTDot())
                .add(glonassOrbit.getDeltaT())
                .add(GLONASS_MEAN_DRACONIAN_PERIOD);
        final FieldUnivariateDerivative2<T> n =
            tDR.divide(2.0 * GLONASSOrbitalElements.GLONASS_PI).reciprocal();

        // Semi-major axis : computed by successive approximation
        final FieldUnivariateDerivative2<T> sma = computeSma(tDR, i, e);

        // (ae / p)^2 term
        final FieldUnivariateDerivative2<T> p =
            sma.multiply(e.multiply(e).negate().add(1.0));
        final FieldUnivariateDerivative2<T> aeop =
            p.divide(GLONASS_EARTH_EQUATORIAL_RADIUS).reciprocal();
        final FieldUnivariateDerivative2<T> aeop2 = aeop.multiply(aeop);

        // Current longitude of the ascending node
        final FieldUnivariateDerivative2<T> lambda =
            computeLambda(dTpr, n, aeop2, i);

        // Current argument of perigee
        final FieldUnivariateDerivative2<T> pa = computePA(dTpr, n, aeop2, i);

        // Mean longitude at the instant the spacecraft passes the current
        // ascending
        // node
        final FieldUnivariateDerivative2<T> tanPAo2 =
            FastMath.tan(pa.divide(2.0));
        final FieldUnivariateDerivative2<T> coef =
            tanPAo2.multiply(FastMath
                .sqrt(e.negate().add(1.0).divide(e.add(1.0))));
        final FieldUnivariateDerivative2<T> e0 =
            FastMath.atan(coef).multiply(2.0).negate();
        final FieldUnivariateDerivative2<T> m1 =
            pa.add(e0).subtract(FastMath.sin(e0).multiply(e));

        // Current mean longitude
        final FieldUnivariateDerivative2<T> correction =
            dTpr.subtract(w.multiply(zero.add(GLONASS_MEAN_DRACONIAN_PERIOD)
                .add(glonassOrbit.getDeltaT())))
                .subtract(w.multiply(w).multiply(glonassOrbit.getDeltaTDot()));
        final FieldUnivariateDerivative2<T> m = m1.add(n.multiply(correction));

        // Take into consideration the periodic perturbations
        final FieldSinCos<FieldUnivariateDerivative2<T>> scPa =
            FastMath.sinCos(pa);
        final FieldUnivariateDerivative2<T> h = e.multiply(scPa.sin());
        final FieldUnivariateDerivative2<T> l = e.multiply(scPa.cos());
        // δa1
        final FieldUnivariateDerivative2<T>[] d1 =
            getParameterDifferentials(sma, i, h, l, m1);
        // δa2
        final FieldUnivariateDerivative2<T>[] d2 =
            getParameterDifferentials(sma, i, h, l, m);
        // Apply corrections
        final FieldUnivariateDerivative2<T> smaCorr =
            sma.add(d2[0]).subtract(d1[0]);
        final FieldUnivariateDerivative2<T> hCorr =
            h.add(d2[1]).subtract(d1[1]);
        final FieldUnivariateDerivative2<T> lCorr =
            l.add(d2[2]).subtract(d1[2]);
        final FieldUnivariateDerivative2<T> lambdaCorr =
            lambda.add(d2[3]).subtract(d1[3]);
        final FieldUnivariateDerivative2<T> iCorr =
            i.add(d2[4]).subtract(d1[4]);
        final FieldUnivariateDerivative2<T> mCorr =
            m.add(d2[5]).subtract(d1[5]);
        final FieldUnivariateDerivative2<T> eCorr =
            FastMath.sqrt(hCorr.multiply(hCorr).add(lCorr.multiply(lCorr)));
        final FieldUnivariateDerivative2<T> paCorr;
        if (eCorr.getValue().getReal() == 0.) {
            paCorr = zero;
        } else {
            if (lCorr.getValue() == eCorr.getValue()) {
                paCorr = zero.add(0.5 * FieldGLONASSOrbitalElements.GLONASS_PI);
            } else if (lCorr.getValue()
                .getReal() == -eCorr.getValue().getReal()) {
                paCorr =
                    zero.add(-0.5 * FieldGLONASSOrbitalElements.GLONASS_PI);
            } else {
                paCorr = FastMath.atan2(hCorr, lCorr);
            }
        }

        // Eccentric Anomaly
        final FieldUnivariateDerivative2<T> mk = mCorr.subtract(paCorr);
        final FieldUnivariateDerivative2<T> ek = getEccentricAnomaly(mk, eCorr);

        // True Anomaly
        final FieldUnivariateDerivative2<T> vk = getTrueAnomaly(ek, eCorr);

        // Argument of Latitude
        final FieldUnivariateDerivative2<T> phik = vk.add(paCorr);

        // Corrected Radius
        final FieldUnivariateDerivative2<T> pCorr =
            smaCorr.multiply(eCorr.multiply(eCorr).negate().add(1.0));
        final FieldUnivariateDerivative2<T> rk =
            pCorr.divide(eCorr.multiply(FastMath.cos(vk)).add(1.0));

        // Positions in orbital plane
        final FieldSinCos<FieldUnivariateDerivative2<T>> scPhik =
            FastMath.sinCos(phik);
        final FieldUnivariateDerivative2<T> xk = scPhik.cos().multiply(rk);
        final FieldUnivariateDerivative2<T> yk = scPhik.sin().multiply(rk);

        // Coordinates of position
        final FieldSinCos<FieldUnivariateDerivative2<T>> scL =
            FastMath.sinCos(lambdaCorr);
        final FieldSinCos<FieldUnivariateDerivative2<T>> scI =
            FastMath.sinCos(iCorr);
        final FieldVector3D<FieldUnivariateDerivative2<T>> positionwithDerivatives =
            new FieldVector3D<>(xk.multiply(scL.cos())
                .subtract(yk.multiply(scL.sin()).multiply(scI.cos())),
                                xk.multiply(scL.sin())
                                    .add(yk.multiply(scL.cos())
                                        .multiply(scI.cos())),
                                yk.multiply(scI.sin()));

        return new FieldPVCoordinates<T>(new FieldVector3D<T>(positionwithDerivatives
            .getX().getValue(), positionwithDerivatives.getY().getValue(),
                                                              positionwithDerivatives
                                                                  .getZ()
                                                                  .getValue()),
                                         new FieldVector3D<T>(positionwithDerivatives
                                             .getX().getFirstDerivative(),
                                                              positionwithDerivatives
                                                                  .getY()
                                                                  .getFirstDerivative(),
                                                              positionwithDerivatives
                                                                  .getZ()
                                                                  .getFirstDerivative()),
                                         new FieldVector3D<T>(positionwithDerivatives
                                             .getX().getSecondDerivative(),
                                                              positionwithDerivatives
                                                                  .getY()
                                                                  .getSecondDerivative(),
                                                              positionwithDerivatives
                                                                  .getZ()
                                                                  .getSecondDerivative()));
    }

    /**
     * Gets eccentric anomaly from mean anomaly.
     * <p>
     * The algorithm used to solve the Kepler equation has been published in:
     * "Procedures for solving Kepler's Equation", A. W. Odell and R. H.
     * Gooding, Celestial Mechanics 38 (1986) 307-334
     * </p>
     * <p>
     * It has been copied from the OREKIT library (KeplerianOrbit class).
     * </p>
     *
     * @param mk the mean anomaly (rad)
     * @param e the eccentricity
     * @return the eccentric anomaly (rad)
     */
    private FieldUnivariateDerivative2<T>
        getEccentricAnomaly(final FieldUnivariateDerivative2<T> mk,
                            final FieldUnivariateDerivative2<T> e) {

        // reduce M to [-PI PI] interval
        final T zero = mk.getValue().getField().getZero();
        final FieldUnivariateDerivative2<T> reducedM =
            new FieldUnivariateDerivative2<T>(MathUtils
                .normalizeAngle(mk.getValue(), zero), mk.getFirstDerivative(),
                                              mk.getSecondDerivative());

        // compute start value according to A. W. Odell and R. H. Gooding S12
        // starter
        FieldUnivariateDerivative2<T> ek;
        if (FastMath.abs(reducedM.getValue()).getReal() < 1.0 / 6.0) {
            if (FastMath.abs(reducedM.getValue())
                .getReal() < Precision.SAFE_MIN) {
                // this is an Orekit change to the S12 starter.
                // If reducedM is 0.0, the derivative of cbrt is infinite which
                // induces NaN
                // appearing later in
                // the computation. As in this case E and M are almost equal, we
                // initialize ek
                // with reducedM
                ek = reducedM;
            } else {
                // this is the standard S12 starter
                ek =
                    reducedM.add(reducedM.multiply(6).cbrt().subtract(reducedM)
                        .multiply(e));
            }
        } else {
            if (reducedM.getValue().getReal() < 0) {
                final FieldUnivariateDerivative2<T> w =
                    reducedM.add(FastMath.PI);
                ek =
                    reducedM.add(w.multiply(-A).divide(w.subtract(B))
                        .subtract(FastMath.PI).subtract(reducedM).multiply(e));
            } else {
                final FieldUnivariateDerivative2<T> minusW =
                    reducedM.subtract(FastMath.PI);
                ek =
                    reducedM.add(minusW.multiply(A).divide(minusW.add(B))
                        .add(FastMath.PI).subtract(reducedM).multiply(e));
            }
        }

        final FieldUnivariateDerivative2<T> e1 = e.negate().add(1.0);
        final boolean noCancellationRisk =
            (e1.getReal() +
             ek.getValue().getReal() * ek.getValue().getReal() / 6.0) >= 0.1;

        // perform two iterations, each consisting of one Halley step and one
        // Newton-Raphson step
        for (int j = 0; j < 2; ++j) {
            final FieldUnivariateDerivative2<T> f;
            FieldUnivariateDerivative2<T> fd;
            final FieldUnivariateDerivative2<T> fdd = ek.sin().multiply(e);
            final FieldUnivariateDerivative2<T> fddd = ek.cos().multiply(e);
            if (noCancellationRisk) {
                f = ek.subtract(fdd).subtract(reducedM);
                fd = fddd.subtract(1).negate();
            } else {
                f = eMeSinE(ek, e).subtract(reducedM);
                final FieldUnivariateDerivative2<T> s = ek.multiply(0.5).sin();
                fd = s.multiply(s).multiply(e.multiply(2.0)).add(e1);
            }
            final FieldUnivariateDerivative2<T> dee =
                f.multiply(fd).divide(f.multiply(0.5).multiply(fdd)
                    .subtract(fd.multiply(fd)));

            // update eccentric anomaly, using expressions that limit underflow
            // problems
            final FieldUnivariateDerivative2<T> w =
                fd.add(dee.multiply(0.5)
                    .multiply(fdd.add(dee.multiply(fdd).divide(3))));
            fd = fd.add(dee.multiply(fdd.add(dee.multiply(0.5).multiply(fdd))));
            ek =
                ek.subtract(f.subtract(dee.multiply(fd.subtract(w)))
                    .divide(fd));
        }

        // expand the result back to original range
        ek = ek.add(mk.getValue().subtract(reducedM.getValue()));

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
    private FieldUnivariateDerivative2<T>
        eMeSinE(final FieldUnivariateDerivative2<T> E,
                final FieldUnivariateDerivative2<T> ecc) {
        FieldUnivariateDerivative2<T> x =
            E.sin().multiply(ecc.negate().add(1.0));
        final FieldUnivariateDerivative2<T> mE2 = E.negate().multiply(E);
        FieldUnivariateDerivative2<T> term = E;
        FieldUnivariateDerivative2<T> d = E.getField().getZero();
        // the inequality test below IS intentional and should NOT be replaced
        // by a
        // check with a small tolerance
        for (FieldUnivariateDerivative2<T> x0 = d.add(Double.NaN);
             !(x.getValue()).equals(x0.getValue());) {
            d = d.add(2);
            term = term.multiply(mE2.divide(d.multiply(d.add(1))));
            x0 = x;
            x = x.subtract(term);
        }
        return x;
    }

    /**
     * Gets true anomaly from eccentric anomaly.
     *
     * @param ek the eccentric anomaly (rad)
     * @param ecc the eccentricity
     * @return the true anomaly (rad)
     */
    private FieldUnivariateDerivative2<T>
        getTrueAnomaly(final FieldUnivariateDerivative2<T> ek,
                       final FieldUnivariateDerivative2<T> ecc) {
        final FieldUnivariateDerivative2<T> svk =
            ek.sin()
                .multiply(FastMath.sqrt(ecc.multiply(ecc).negate().add(1.0)));
        final FieldUnivariateDerivative2<T> cvk = ek.cos().subtract(ecc);
        return svk.atan2(cvk);
    }

    /**
     * Get the interval of prediction.
     *
     * @param date the considered date
     * @return the duration from GLONASS orbit Reference epoch (s)
     */
    private FieldUnivariateDerivative2<T>
        getdTpr(final FieldAbsoluteDate<T> date) {
        final TimeScale glonass = dataContext.getTimeScales().getGLONASS();
        final FieldGLONASSDate<T> tEnd =
            new FieldGLONASSDate<T>(date.getField(), date, glonass);
        final FieldGLONASSDate<T> tSta =
            new FieldGLONASSDate<T>(date.getField(), glonassOrbit.getDate(),
                                    glonass);
        final int n = tEnd.getDayNumber();
        final int na = tSta.getDayNumber();
        final int deltaN;
        if (na == 27) {
            deltaN = n - na - FastMath.round((float) (n - na) / 1460) * 1460;
        } else {
            deltaN = n - na - FastMath.round((float) (n - na) / 1461) * 1461;
        }

        final T zero = date.getField().getZero();
        final FieldUnivariateDerivative2<T> ti =
            new FieldUnivariateDerivative2<T>(zero.add(tEnd.getSecInDay()),
                                              zero.add(1.0), zero);

        return ti.subtract(glonassOrbit.getTime()).add(86400 * deltaN);
    }

    /**
     * Computes the semi-major axis of orbit using technique of successive
     * approximations.
     *
     * @param tDR mean draconique period (s)
     * @param i current inclination (rad)
     * @param e eccentricity
     * @return the semi-major axis (m).
     */
    private FieldUnivariateDerivative2<T>
        computeSma(final FieldUnivariateDerivative2<T> tDR,
                   final FieldUnivariateDerivative2<T> i,
                   final FieldUnivariateDerivative2<T> e) {

        // Zero
        final FieldUnivariateDerivative2<T> zero = tDR.getField().getZero();

        // If one of the input parameter is equal to Double.NaN, an infinite
        // loop can
        // occur.
        // In that case, we do not compute the value of the semi major axis.
        // We decided to return a Double.NaN value instead.
        if (Double.isNaN(tDR.getValue().getReal()) ||
            Double.isNaN(i.getValue().getReal()) ||
            Double.isNaN(e.getValue().getReal())) {
            return zero.add(Double.NaN);
        }

        // Common parameters
        final FieldUnivariateDerivative2<T> sinI = FastMath.sin(i);
        final FieldUnivariateDerivative2<T> sin2I = sinI.multiply(sinI);
        final FieldUnivariateDerivative2<T> ome2 =
            e.multiply(e).negate().add(1.0);
        final FieldUnivariateDerivative2<T> ome2Pow3o2 =
            FastMath.sqrt(ome2).multiply(ome2);
        final FieldUnivariateDerivative2<T> pa = zero.add(glonassOrbit.getPa());
        final FieldUnivariateDerivative2<T> cosPA = FastMath.cos(pa);
        final FieldUnivariateDerivative2<T> opecosPA =
            e.multiply(cosPA).add(1.0);
        final FieldUnivariateDerivative2<T> opecosPAPow2 =
            opecosPA.multiply(opecosPA);
        final FieldUnivariateDerivative2<T> opecosPAPow3 =
            opecosPAPow2.multiply(opecosPA);

        // Initial approximation
        FieldUnivariateDerivative2<T> tOCK = tDR;

        // Successive approximations
        // The process of approximation ends when fulfilling the following
        // condition:
        // |a(n+1) - a(n)| < 1cm
        FieldUnivariateDerivative2<T> an = zero;
        FieldUnivariateDerivative2<T> anp1 = zero;
        boolean isLastStep = false;
        while (!isLastStep) {

            // a(n+1) computation
            final FieldUnivariateDerivative2<T> tOCKo2p =
                tOCK.divide(2.0 * GLONASSOrbitalElements.GLONASS_PI);
            final FieldUnivariateDerivative2<T> tOCKo2pPow2 =
                tOCKo2p.multiply(tOCKo2p);
            anp1 =
                FastMath.cbrt(tOCKo2pPow2
                    .multiply(GLONASSOrbitalElements.GLONASS_MU));

            // p(n+1) computation
            final FieldUnivariateDerivative2<T> p = anp1.multiply(ome2);

            // Tock(n+1) computation
            final FieldUnivariateDerivative2<T> aeop =
                p.divide(GLONASS_EARTH_EQUATORIAL_RADIUS).reciprocal();
            final FieldUnivariateDerivative2<T> aeop2 = aeop.multiply(aeop);
            final FieldUnivariateDerivative2<T> term1 =
                aeop2.multiply(GLONASS_J20).multiply(1.5);
            final FieldUnivariateDerivative2<T> term2 =
                sin2I.multiply(2.5).negate().add(2.0);
            final FieldUnivariateDerivative2<T> term3 =
                ome2Pow3o2.divide(opecosPAPow2);
            final FieldUnivariateDerivative2<T> term4 =
                opecosPAPow3.divide(ome2);
            tOCK =
                tDR.divide(term1.multiply(term2.multiply(term3).add(term4))
                    .negate().add(1.0));

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
     *
     * @param dTpr interval of prediction (s)
     * @param n mean motion (rad/s)
     * @param aeop2 square of the ratio between the radius of the ellipsoid and
     *        p, with p = sma * (1 - ecc²)
     * @param i inclination (rad)
     * @return the current longitude of the ascending node (rad)
     */
    private FieldUnivariateDerivative2<T>
        computeLambda(final FieldUnivariateDerivative2<T> dTpr,
                      final FieldUnivariateDerivative2<T> n,
                      final FieldUnivariateDerivative2<T> aeop2,
                      final FieldUnivariateDerivative2<T> i) {
        final FieldUnivariateDerivative2<T> cosI = FastMath.cos(i);
        final FieldUnivariateDerivative2<T> precession =
            aeop2.multiply(n).multiply(cosI).multiply(1.5 * GLONASS_J20);
        return dTpr.multiply(precession.add(GLONASS_AV)).negate()
            .add(glonassOrbit.getLambda());
    }

    /**
     * Computes the current argument of perigee.
     *
     * @param dTpr interval of prediction (s)
     * @param n mean motion (rad/s)
     * @param aeop2 square of the ratio between the radius of the ellipsoid and
     *        p, with p = sma * (1 - ecc²)
     * @param i inclination (rad)
     * @return the current argument of perigee (rad)
     */
    private FieldUnivariateDerivative2<T>
        computePA(final FieldUnivariateDerivative2<T> dTpr,
                  final FieldUnivariateDerivative2<T> n,
                  final FieldUnivariateDerivative2<T> aeop2,
                  final FieldUnivariateDerivative2<T> i) {
        final FieldUnivariateDerivative2<T> cosI = FastMath.cos(i);
        final FieldUnivariateDerivative2<T> cos2I = cosI.multiply(cosI);
        final FieldUnivariateDerivative2<T> precession =
            aeop2.multiply(n).multiply(cos2I.multiply(5.0).negate().add(1.0))
                .multiply(0.75 * GLONASS_J20);
        return dTpr.multiply(precession).negate().add(glonassOrbit.getPa());
    }

    /**
     * Computes the differentials δa<sub>i</sub>.
     * <p>
     * The value of i depends of the type of longitude (i = 2 for the current
     * mean longitude; i = 1 for the mean longitude at the instant the
     * spacecraft passes the current ascending node)
     * </p>
     *
     * @param a semi-major axis (m)
     * @param i inclination (rad)
     * @param h x component of the eccentricity (rad)
     * @param l y component of the eccentricity (rad)
     * @param m longitude (current or at the ascending node instant)
     * @return the differentials of the orbital parameters
     */
    private FieldUnivariateDerivative2<T>[]
        getParameterDifferentials(final FieldUnivariateDerivative2<T> a,
                                  final FieldUnivariateDerivative2<T> i,
                                  final FieldUnivariateDerivative2<T> h,
                                  final FieldUnivariateDerivative2<T> l,
                                  final FieldUnivariateDerivative2<T> m) {

        // B constant
        final FieldUnivariateDerivative2<T> aeoa =
            a.divide(GLONASS_EARTH_EQUATORIAL_RADIUS).reciprocal();
        final FieldUnivariateDerivative2<T> aeoa2 = aeoa.multiply(aeoa);
        final FieldUnivariateDerivative2<T> b =
            aeoa2.multiply(1.5 * GLONASS_J20);

        // Commons Parameters
        final FieldSinCos<FieldUnivariateDerivative2<T>> scI =
            FastMath.sinCos(i);
        final FieldSinCos<FieldUnivariateDerivative2<T>> scLk =
            FastMath.sinCos(m);
        final FieldSinCos<FieldUnivariateDerivative2<T>> sc2Lk =
            FieldSinCos.sum(scLk, scLk);
        final FieldSinCos<FieldUnivariateDerivative2<T>> sc3Lk =
            FieldSinCos.sum(scLk, sc2Lk);
        final FieldSinCos<FieldUnivariateDerivative2<T>> sc4Lk =
            FieldSinCos.sum(sc2Lk, sc2Lk);
        final FieldUnivariateDerivative2<T> cosI = scI.cos();
        final FieldUnivariateDerivative2<T> sinI = scI.sin();
        final FieldUnivariateDerivative2<T> cosI2 = cosI.multiply(cosI);
        final FieldUnivariateDerivative2<T> sinI2 = sinI.multiply(sinI);
        final FieldUnivariateDerivative2<T> cosLk = scLk.cos();
        final FieldUnivariateDerivative2<T> sinLk = scLk.sin();
        final FieldUnivariateDerivative2<T> cos2Lk = sc2Lk.cos();
        final FieldUnivariateDerivative2<T> sin2Lk = sc2Lk.sin();
        final FieldUnivariateDerivative2<T> cos3Lk = sc3Lk.cos();
        final FieldUnivariateDerivative2<T> sin3Lk = sc3Lk.sin();
        final FieldUnivariateDerivative2<T> cos4Lk = sc4Lk.cos();
        final FieldUnivariateDerivative2<T> sin4Lk = sc4Lk.sin();

        // h*cos(nLk), l*cos(nLk), h*sin(nLk) and l*sin(nLk)
        // n = 1
        final FieldUnivariateDerivative2<T> hCosLk = h.multiply(cosLk);
        final FieldUnivariateDerivative2<T> hSinLk = h.multiply(sinLk);
        final FieldUnivariateDerivative2<T> lCosLk = l.multiply(cosLk);
        final FieldUnivariateDerivative2<T> lSinLk = l.multiply(sinLk);
        // n = 2
        final FieldUnivariateDerivative2<T> hCos2Lk = h.multiply(cos2Lk);
        final FieldUnivariateDerivative2<T> hSin2Lk = h.multiply(sin2Lk);
        final FieldUnivariateDerivative2<T> lCos2Lk = l.multiply(cos2Lk);
        final FieldUnivariateDerivative2<T> lSin2Lk = l.multiply(sin2Lk);
        // n = 3
        final FieldUnivariateDerivative2<T> hCos3Lk = h.multiply(cos3Lk);
        final FieldUnivariateDerivative2<T> hSin3Lk = h.multiply(sin3Lk);
        final FieldUnivariateDerivative2<T> lCos3Lk = l.multiply(cos3Lk);
        final FieldUnivariateDerivative2<T> lSin3Lk = l.multiply(sin3Lk);
        // n = 4
        final FieldUnivariateDerivative2<T> hCos4Lk = h.multiply(cos4Lk);
        final FieldUnivariateDerivative2<T> hSin4Lk = h.multiply(sin4Lk);
        final FieldUnivariateDerivative2<T> lCos4Lk = l.multiply(cos4Lk);
        final FieldUnivariateDerivative2<T> lSin4Lk = l.multiply(sin4Lk);

        // 1 - (3 / 2)*sin²i
        final FieldUnivariateDerivative2<T> om3o2xSinI2 =
            sinI2.multiply(1.5).negate().add(1.0);

        // Compute Differentials
        // δa
        final FieldUnivariateDerivative2<T> dakT1 =
            b.multiply(2.0).multiply(om3o2xSinI2).multiply(lCosLk.add(hSinLk));
        final FieldUnivariateDerivative2<T> dakT2 =
            b.multiply(sinI2)
                .multiply(hSinLk.multiply(0.5).subtract(lCosLk.multiply(0.5))
                    .add(cos2Lk).add(lCos3Lk.multiply(3.5))
                    .add(hSin3Lk.multiply(3.5)));
        final FieldUnivariateDerivative2<T> dak = dakT1.add(dakT2);

        // δh
        final FieldUnivariateDerivative2<T> dhkT1 =
            b.multiply(om3o2xSinI2).multiply(sinLk.add(lSin2Lk.multiply(1.5))
                .subtract(hCos2Lk.multiply(1.5)));
        final FieldUnivariateDerivative2<T> dhkT2 =
            b.multiply(sinI2).multiply(0.25)
                .multiply(sinLk.subtract(sin3Lk.multiply(SEVEN_THIRD))
                    .add(lSin2Lk.multiply(5.0)).subtract(lSin4Lk.multiply(8.5))
                    .add(hCos4Lk.multiply(8.5)).add(hCos2Lk));
        final FieldUnivariateDerivative2<T> dhkT3 =
            lSin2Lk.multiply(cosI2).multiply(b).multiply(0.5).negate();
        final FieldUnivariateDerivative2<T> dhk =
            dhkT1.subtract(dhkT2).add(dhkT3);

        // δl
        final FieldUnivariateDerivative2<T> dlkT1 =
            b.multiply(om3o2xSinI2).multiply(cosLk.add(lCos2Lk.multiply(1.5))
                .add(hSin2Lk.multiply(1.5)));
        final FieldUnivariateDerivative2<T> dlkT2 =
            b.multiply(sinI2).multiply(0.25)
                .multiply(cosLk.negate().subtract(cos3Lk.multiply(SEVEN_THIRD))
                    .subtract(hSin2Lk.multiply(5.0))
                    .subtract(lCos4Lk.multiply(8.5))
                    .subtract(hSin4Lk.multiply(8.5)).add(lCos2Lk));
        final FieldUnivariateDerivative2<T> dlkT3 =
            hSin2Lk.multiply(cosI2).multiply(b).multiply(0.5);
        final FieldUnivariateDerivative2<T> dlk =
            dlkT1.subtract(dlkT2).add(dlkT3);

        // δλ
        final FieldUnivariateDerivative2<T> dokT1 = b.negate().multiply(cosI);
        final FieldUnivariateDerivative2<T> dokT2 =
            lSinLk.multiply(3.5).subtract(hCosLk.multiply(2.5))
                .subtract(sin2Lk.multiply(0.5))
                .subtract(lSin3Lk.multiply(SEVEN_SIXTH))
                .add(hCos3Lk.multiply(SEVEN_SIXTH));
        final FieldUnivariateDerivative2<T> dok = dokT1.multiply(dokT2);

        // δi
        final FieldUnivariateDerivative2<T> dik =
            b.multiply(sinI).multiply(cosI).multiply(0.5)
                .multiply(lCosLk.negate().add(hSinLk).add(cos2Lk)
                    .add(lCos3Lk.multiply(SEVEN_THIRD))
                    .add(hSin3Lk.multiply(SEVEN_THIRD)));

        // δL
        final FieldUnivariateDerivative2<T> dLkT1 =
            b.multiply(2.0).multiply(om3o2xSinI2).multiply(lSinLk.multiply(1.75)
                .subtract(hCosLk.multiply(1.75)));
        final FieldUnivariateDerivative2<T> dLkT2 =
            b.multiply(sinI2).multiply(3.0)
                .multiply(hCosLk.multiply(SEVEN_24TH).negate()
                    .subtract(lSinLk.multiply(SEVEN_24TH))
                    .subtract(hCos3Lk.multiply(FN_72TH))
                    .add(lSin3Lk.multiply(FN_72TH)).add(sin2Lk.multiply(0.25)));
        final FieldUnivariateDerivative2<T> dLkT3 =
            b.multiply(cosI2)
                .multiply(lSinLk.multiply(3.5).subtract(hCosLk.multiply(2.5))
                    .subtract(sin2Lk.multiply(0.5))
                    .subtract(lSin3Lk.multiply(SEVEN_SIXTH))
                    .add(hCos3Lk.multiply(SEVEN_SIXTH)));
        final FieldUnivariateDerivative2<T> dLk = dLkT1.add(dLkT2).add(dLkT3);

        // Final array
        final FieldUnivariateDerivative2<T>[] differentials =
            MathArrays.buildArray(a.getField(), 6);
        differentials[0] = dak.multiply(a);
        differentials[1] = dhk;
        differentials[2] = dlk;
        differentials[3] = dok;
        differentials[4] = dik;
        differentials[5] = dLk;

        return differentials;
    }

    /** {@inheritDoc} */
    protected T getMass(final FieldAbsoluteDate<T> date) {
        return mass;
    }

    /**
     * Get the Earth gravity coefficient used for GLONASS propagation.
     *
     * @return the Earth gravity coefficient.
     */
    public T getMU() {
        return getField().getZero().add(GLONASSOrbitalElements.GLONASS_MU);
    }

    /**
     * Gets the underlying Field GLONASS orbital elements.
     *
     * @return the underlying Field GLONASS orbital elements
     */
    public FieldGLONASSOrbitalElements<T> getGLONASSOrbitalElements() {
        return glonassOrbit;
    }

    /**
     * Gets the Earth Centered Inertial frame used to propagate the orbit.
     *
     * @return the ECI frame
     */
    public Frame getECI() {
        return eci;
    }

    /**
     * Gets the Earth Centered Earth Fixed frame used to propagate GLONASS
     * orbits.
     *
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
    public void resetInitialState(final FieldSpacecraftState<T> state) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    protected void resetIntermediateState(final FieldSpacecraftState<T> state,
                                          final boolean forward) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    protected FieldOrbit<T> propagateOrbit(final FieldAbsoluteDate<T> date,
                                           final T[] parameters) {
        // Gets the PVCoordinates in ECEF frame
        final FieldPVCoordinates<T> pvaInECEF = propagateInEcef(date);
        // Transforms the PVCoordinates to ECI frame
        final FieldPVCoordinates<T> pvaInECI =
            ecef.getTransformTo(eci, date).transformPVCoordinates(pvaInECEF);
        // Returns the Cartesian orbit
        return new FieldCartesianOrbit<>(pvaInECI, eci, date, date.getField()
            .getZero().add(FieldGLONASSOrbitalElements.GLONASS_MU));
    }

    /**
     * Get the parameters driver for the Field Glonass propagation model.
     *
     * @return an empty list.
     */
    @Override
    protected List<ParameterDriver> getParametersDrivers() {
        // The Field Glonass propagation model does not have parameter drivers.
        return Collections.emptyList();
    }

}
