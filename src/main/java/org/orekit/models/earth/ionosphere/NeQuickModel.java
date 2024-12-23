/* Copyright 2002-2024 CS GROUP
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
package org.orekit.models.earth.ionosphere;

import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.units.Unit;

/**
 * NeQuick ionospheric delay model.
 *
 * @author Bryan Cazabonne
 *
 * @see "European Union (2016). European GNSS (Galileo) Open Service-Ionospheric Correction
 *       Algorithm for Galileo Single Frequency Users. 1.2."
 * @see <a href="https://www.itu.int/rec/R-REC-P.531/en">ITU-R P.531</a>
 *
 * @since 10.1
 */
public class NeQuickModel implements IonosphericModel {

    /** Mean Earth radius in m (Ref Table 2.5.2). */
    static final double RE = 6371200.0;

    /** NeQuick resources base directory. */
    static final String NEQUICK_BASE = "/assets/org/orekit/nequick/";

    /** Factor for the electron density computation. */
    private static final double DENSITY_FACTOR = 1.0e11;

    /** Factor for the path delay computation. */
    private static final double DELAY_FACTOR = 40.3e16;

    /** Engine for computing various low-level aspects of NeQuick model. */
    private final NeQuickEngine engine;

    /** Modip grid. */
    private final ModipGrid modipGrid;

    /** F2 coefficients used by the F2 layer (flatten array for cache efficiency). */
    private final double[][] flattenF2;

    /** Fm3 coefficients used by the M(3000)F2 layer(flatten array for cache efficiency). */
    private final double[][] flattenFm3;

    /** UTC time scale. */
    private final TimeScale utc;

    /**
     * Build a new instance.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param alpha effective ionisation level coefficients
     * @see #NeQuickModel(double[], TimeScale)
     */
    @DefaultDataContext
    public NeQuickModel(final double[] alpha) {
        this(alpha, DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Build a new instance of the Galileo version of the NeQuick-2 model.
     * <p>
     * The Galileo version uses a loose modip grid and 3 broadcast parameters to compute
     * effective ionization level.
     * </p>
     * @param alpha broadcast effective ionisation level coefficients
     * @param utc UTC time scale.
     * @since 10.1
     */
    public NeQuickModel(final double[] alpha, final TimeScale utc) {

        // F2 layer values
        this.flattenF2  = new double[12][];
        this.flattenFm3 = new double[12][];
        // Modip grid
        this.modipGrid  = getModipGrid(NeQuickVersion.NEQUICK_2_GALILEO);

        // Ionisation level
        this.engine = new GalileoEngine(alpha);
        this.utc = utc;

    }

    /**
     * Build a new instance of the original ITU version of the NeQuick-2 model.
     * <p>
     * The original ITU version uses a fine modip grid and effective ionization
     * is used directly as solar flux
     * </p>
     * @param f107 solar flux at 10.7cm (in Solar Flux Units)
     * @param utc UTC time scale.
     * @since 10.1
     */
    public NeQuickModel(final double f107, final TimeScale utc) {

        // F2 layer values
        this.flattenF2  = new double[12][];
        this.flattenFm3 = new double[12][];
        // Modip grid
        this.modipGrid  = getModipGrid(NeQuickVersion.NEQUICK_2_ITU);

        // Ionisation level
        this.engine = new ItuEngine(f107);
        this.utc = utc;

    }

    /** {@inheritDoc} */
    @Override
    public double pathDelay(final SpacecraftState state, final TopocentricFrame baseFrame,
                            final double frequency, final double[] parameters) {
        // Point
        final GeodeticPoint recPoint = baseFrame.getPoint();
        // Date
        final AbsoluteDate date = state.getDate();

        // Reference body shape
        final BodyShape ellipsoid = baseFrame.getParentShape();
        // Satellite geodetic coordinates
        final GeodeticPoint satPoint = ellipsoid.transform(state.getPosition(ellipsoid.getBodyFrame()),
                                                           ellipsoid.getBodyFrame(), state.getDate());

        // Total Electron Content
        final double tec = stec(date,recPoint, satPoint);

        // Ionospheric delay
        final double factor = DELAY_FACTOR / (frequency * frequency);
        return factor * tec;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T pathDelay(final FieldSpacecraftState<T> state,
                                                           final TopocentricFrame baseFrame,
                                                           final double frequency,
                                                           final T[] parameters) {
        // Date
        final FieldAbsoluteDate<T> date = state.getDate();
        // Point
        final FieldGeodeticPoint<T> recPoint = baseFrame.getPoint(date.getField());


        // Reference body shape
        final BodyShape ellipsoid = baseFrame.getParentShape();
        // Satellite geodetic coordinates
        final FieldGeodeticPoint<T> satPoint = ellipsoid.transform(state.getPosition(ellipsoid.getBodyFrame()), ellipsoid.getBodyFrame(), state.getDate());

        // Total Electron Content
        final T tec = stec(date, recPoint, satPoint);

        // Ionospheric delay
        final double factor = DELAY_FACTOR / (frequency * frequency);
        return tec.multiply(factor);
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /**
     * This method allows the computation of the Slant Total Electron Content (STEC).
     * @param date current date
     * @param recP receiver position
     * @param satP satellite position
     * @return the STEC in TECUnits
     */
    public double stec(final AbsoluteDate date, final GeodeticPoint recP, final GeodeticPoint satP) {

        // Load the correct CCIR file
        final DateTimeComponents dateTime = date.getComponents(utc);
        loadsIfNeeded(dateTime.getDate());

        return engine.stec(dateTime, new Ray(recP, satP));

    }

    /**
     * This method allows the computation of the Slant Total Electron Content (STEC).
     * @param date current date
     * @param recP receiver position
     * @param satP satellite position
     * @return the STEC in TECUnits
     */
    public <T extends CalculusFieldElement<T>> T stec(final FieldAbsoluteDate<T> date,
                                                      final FieldGeodeticPoint<T> recP,
                                                      final FieldGeodeticPoint<T> satP) {

        // Load the correct CCIR file
        final DateTimeComponents dateTime = date.getComponents(utc);
        loadsIfNeeded(dateTime.getDate());

        return engine.stec(dateTime, new FieldRay<>(recP, satP));

    }

    /**
     * Computes the electron density at a given height.
     * @param dateTime date
     * @param modip modified dip latitude
     * @param az effective ionization level
     * @param latitude latitude along the integration path
     * @param longitude longitude along the integration path
     * @param h height along the integration path in m
     * @return electron density [m⁻³]
     */
    private double electronDensity(final DateTimeComponents dateTime, final double modip, final double az,
                                   final double latitude, final double longitude, final double h) {

        final NeQuickParameters parameters = new NeQuickParameters(dateTime,
                                                                   flattenF2[dateTime.getDate().getMonth() - 1],
                                                                   flattenFm3[dateTime.getDate().getMonth() - 1],
                                                                   latitude, longitude, az, modip);
        // Convert height in kilometers
        final double hInKm = Unit.KILOMETRE.fromSI(h);
        // Electron density
        final double n;
        if (hInKm <= parameters.getHmF2()) {
            n = bottomElectronDensity(hInKm, parameters);
        } else {
            n = topElectronDensity(hInKm, parameters);
        }
        return n;
    }

    /**
     * Computes the electron density at a given height.
     * @param <T> type of the elements
     * @param dateTime date
     * @param modip modified dip latitude
     * @param az effective ionization level
     * @param latitude latitude along the integration path
     * @param longitude longitude along the integration path
     * @param h height along the integration path in m
     * @return electron density [m⁻³]
     */
    private <T extends CalculusFieldElement<T>> T electronDensity(final DateTimeComponents dateTime,
                                                                  final T modip, final T az,
                                                                  final T latitude, final T longitude, final T h) {

        final FieldNeQuickParameters<T> parameters =
            new FieldNeQuickParameters<>(dateTime,
                                         flattenF2[dateTime.getDate().getMonth() - 1],
                                         flattenFm3[dateTime.getDate().getMonth() - 1],
                                         latitude, longitude, az, modip);

        // Convert height in kilometers
        final T hInKm = Unit.KILOMETRE.fromSI(h);
        // Electron density
        final T n;
        if (hInKm.getReal() <= parameters.getHmF2().getReal()) {
            n = bottomElectronDensity(hInKm, parameters);
        } else {
            n = topElectronDensity(hInKm, parameters);
        }
        return n;
    }

    /**
     * Computes the electron density of the bottomside.
     * @param h height in km
     * @param parameters NeQuick model parameters
     * @return the electron density N in m⁻³
     */
    private double bottomElectronDensity(final double h, final NeQuickParameters parameters) {

        // Select the relevant B parameter for the current height (Eq. 109 and 110)
        final double be;
        if (h > parameters.getHmE()) {
            be = parameters.getBETop();
        } else {
            be = parameters.getBEBot();
        }
        final double bf1;
        if (h > parameters.getHmF1()) {
            bf1 = parameters.getB1Top();
        } else {
            bf1 = parameters.getB1Bot();
        }
        final double bf2 = parameters.getB2Bot();

        // Useful array of constants
        final double[] ct = new double[] {
            1.0 / bf2, 1.0 / bf1, 1.0 / be
        };

        // Compute the exponential argument for each layer (Eq. 111 to 113)
        // If h < 100km we use h = 100km as recommended in the reference document
        final double   hTemp = FastMath.max(100.0, h);
        final double   exp   = clipExp(10.0 / (1.0 + FastMath.abs(hTemp - parameters.getHmF2())));
        final double[] arguments = new double[3];
        arguments[0] = (hTemp - parameters.getHmF2()) / bf2;
        arguments[1] = ((hTemp - parameters.getHmF1()) / bf1) * exp;
        arguments[2] = ((hTemp - parameters.getHmE()) / be) * exp;

        // S coefficients
        final double[] s = new double[3];
        // Array of corrective terms
        final double[] ds = new double[3];

        // Layer amplitudes
        final double[] amplitudes = parameters.getLayerAmplitudes();

        // Fill arrays (Eq. 114 to 118)
        for (int i = 0; i < 3; i++) {
            if (FastMath.abs(arguments[i]) > 25.0) {
                s[i]  = 0.0;
                ds[i] = 0.0;
            } else {
                final double expA   = clipExp(arguments[i]);
                final double opExpA = 1.0 + expA;
                s[i]  = amplitudes[i] * (expA / (opExpA * opExpA));
                ds[i] = ct[i] * ((1.0 - expA) / (1.0 + expA));
            }
        }

        // Electron density
        final double aNo = MathArrays.linearCombination(s[0], 1.0, s[1], 1.0, s[2], 1.0);
        if (h >= 100) {
            return aNo * DENSITY_FACTOR;
        } else {
            // Chapman parameters (Eq. 119 and 120)
            final double bc = 1.0 - 10.0 * (MathArrays.linearCombination(s[0], ds[0], s[1], ds[1], s[2], ds[2]) / aNo);
            final double z  = 0.1 * (h - 100.0);
            // Electron density (Eq. 121)
            return aNo * clipExp(1.0 - bc * z - clipExp(-z)) * DENSITY_FACTOR;
        }
    }

    /**
     * Computes the electron density of the bottomside.
     * @param <T> type of the elements
     * @param h height in km
     * @param parameters NeQuick model parameters
     * @return the electron density N in m⁻³
     */
    private <T extends CalculusFieldElement<T>> T bottomElectronDensity(final T h,
                                                                        final FieldNeQuickParameters<T> parameters) {

        // Zero and One
        final Field<T> field = h.getField();
        final T zero = field.getZero();
        final T one  = field.getOne();

        // Select the relevant B parameter for the current height (Eq. 109 and 110)
        final T be;
        if (h.getReal() > parameters.getHmE().getReal()) {
            be = parameters.getBETop();
        } else {
            be = parameters.getBEBot();
        }
        final T bf1;
        if (h.getReal() > parameters.getHmF1().getReal()) {
            bf1 = parameters.getB1Top();
        } else {
            bf1 = parameters.getB1Bot();
        }
        final T bf2 = parameters.getB2Bot();

        // Useful array of constants
        final T[] ct = MathArrays.buildArray(field, 3);
        ct[0] = bf2.reciprocal();
        ct[1] = bf1.reciprocal();
        ct[2] = be.reciprocal();

        // Compute the exponential argument for each layer (Eq. 111 to 113)
        // If h < 100km we use h = 100km as recommended in the reference document
        final T   hTemp = FastMath.max(zero.newInstance(100.0), h);
        final T   exp   = clipExp(FastMath.abs(hTemp.subtract(parameters.getHmF2())).add(1.0).divide(10.0).reciprocal());
        final T[] arguments = MathArrays.buildArray(field, 3);
        arguments[0] = hTemp.subtract(parameters.getHmF2()).divide(bf2);
        arguments[1] = hTemp.subtract(parameters.getHmF1()).divide(bf1).multiply(exp);
        arguments[2] = hTemp.subtract(parameters.getHmE()).divide(be).multiply(exp);

        // S coefficients
        final T[] s = MathArrays.buildArray(field, 3);
        // Array of corrective terms
        final T[] ds = MathArrays.buildArray(field, 3);

        // Layer amplitudes
        final T[] amplitudes = parameters.getLayerAmplitudes();

        // Fill arrays (Eq. 114 to 118)
        for (int i = 0; i < 3; i++) {
            if (FastMath.abs(arguments[i]).getReal() > 25.0) {
                s[i]  = zero;
                ds[i] = zero;
            } else {
                final T expA   = clipExp(arguments[i]);
                final T opExpA = expA.add(1.0);
                s[i]  = amplitudes[i].multiply(expA.divide(opExpA.multiply(opExpA)));
                ds[i] = ct[i].multiply(expA.negate().add(1.0).divide(expA.add(1.0)));
            }
        }

        // Electron density
        final T aNo = one.linearCombination(s[0], one, s[1], one, s[2], one);
        if (h.getReal() >= 100) {
            return aNo.multiply(DENSITY_FACTOR);
        } else {
            // Chapman parameters (Eq. 119 and 120)
            final T bc = one.linearCombination(s[0], ds[0], s[1], ds[1], s[2], ds[2]).divide(aNo).multiply(10.0).negate().add(1.0);
            final T z  = h.subtract(100.0).multiply(0.1);
            // Electron density (Eq. 121)
            return aNo.multiply(clipExp(bc.multiply(z).add(clipExp(z.negate())).negate().add(1.0))).multiply(DENSITY_FACTOR);
        }
    }

    /**
     * Computes the electron density of the topside.
     * @param h height in km
     * @param parameters NeQuick model parameters
     * @return the electron density N in m⁻³
     */
    private double topElectronDensity(final double h, final NeQuickParameters parameters) {

        // Constant parameters (Eq. 122 and 123)
        final double g = 0.125;
        final double r = 100.0;

        // Arguments deltaH and z (Eq. 124 and 125)
        final double deltaH = h - parameters.getHmF2();
        final double z      = deltaH / (parameters.getH0() * (1.0 + (r * g * deltaH) / (r * parameters.getH0() + g * deltaH)));

        // Exponential (Eq. 126)
        final double ee = clipExp(z);

        // Electron density (Eq. 127)
        if (ee > 1.0e11) {
            return (4.0 * parameters.getNmF2() / ee) * DENSITY_FACTOR;
        } else {
            final double opExpZ = 1.0 + ee;
            return ((4.0 * parameters.getNmF2() * ee) / (opExpZ * opExpZ)) * DENSITY_FACTOR;
        }
    }

    /**
     * Computes the electron density of the topside.
     * @param <T> type of the elements
     * @param h height in km
     * @param parameters NeQuick model parameters
     * @return the electron density N in m⁻³
     */
    private <T extends CalculusFieldElement<T>> T topElectronDensity(final T h,
                                                                     final FieldNeQuickParameters<T> parameters) {

        // Constant parameters (Eq. 122 and 123)
        final double g = 0.125;
        final double r = 100.0;

        // Arguments deltaH and z (Eq. 124 and 125)
        final T deltaH = h.subtract(parameters.getHmF2());
        final T z      = deltaH.divide(parameters.getH0().multiply(deltaH.multiply(r).multiply(g).divide(parameters.getH0().multiply(r).add(deltaH.multiply(g))).add(1.0)));

        // Exponential (Eq. 126)
        final T ee = clipExp(z);

        // Electron density (Eq. 127)
        if (ee.getReal() > 1.0e11) {
            return parameters.getNmF2().multiply(4.0).divide(ee).multiply(DENSITY_FACTOR);
        } else {
            final T opExpZ = ee.add(1.0);
            return parameters.getNmF2().multiply(4.0).multiply(ee).divide(opExpZ.multiply(opExpZ)).multiply(DENSITY_FACTOR);
        }
    }

    /**
     * Lazy loading of CCIR data.
     * @param date current date components
     */
    private void loadsIfNeeded(final DateComponents date) {

        // Month index
        final int monthIndex = date.getMonth() - 1;

        // Check if CCIR has already been loaded for this month
        if (flattenF2[monthIndex] == null) {

            // Read file
            final CCIRLoader loader = new CCIRLoader();
            loader.loadCCIRCoefficients(date);

            // Update arrays
            this.flattenF2[monthIndex]  = flatten(loader.getF2());
            this.flattenFm3[monthIndex] = flatten(loader.getFm3());
        }
    }

    /** Flatten a 3-dimensions array.
     * <p>
     * This method convert 3-dimensions arrays into 1-dimension arrays
     * optimized to avoid cache misses when looping over all elements.
     * </p>
     * @param original original array a[i][j][k]
     * @return flatten array, for embedded loops on j (outer), k (intermediate), i (inner)
     */
    private double[] flatten(final double[][][] original) {
        final double[] flatten = new double[original.length * original[0].length * original[0][0].length];
        int index = 0;
        for (int j = 0; j < original[0].length; j++) {
            for (int k = 0; k < original[0][0].length; k++) {
                for (final double[][] doubles : original) {
                    flatten[index++] = doubles[j][k];
                }
            }
        }
        return flatten;
    }

    /**
     * A clipped exponential function.
     * <p>
     * This function, describe in section F.2.12.2 of the reference document, is
     * recommended for the computation of exponential values.
     * </p>
     * @param power power for exponential function
     * @return clipped exponential value
     */
    private double clipExp(final double power) {
        if (power > 80.0) {
            return 5.5406E34;
        } else if (power < -80) {
            return 1.8049E-35;
        } else {
            return FastMath.exp(power);
        }
    }

    /**
     * A clipped exponential function.
     * <p>
     * This function, describe in section F.2.12.2 of the reference document, is
     * recommended for the computation of exponential values.
     * </p>
     * @param <T> type of the elements
     * @param power power for exponential function
     * @return clipped exponential value
     */
    private <T extends CalculusFieldElement<T>> T clipExp(final T power) {
        if (power.getReal() > 80.0) {
            return power.newInstance(5.5406E34);
        } else if (power.getReal() < -80) {
            return power.newInstance(1.8049E-35);
        } else {
            return FastMath.exp(power);
        }
    }

    /** Engine for computing various low-level aspects of NeQuick model.
     * @since 13.0
     */
    private interface NeQuickEngine {

        /**
         * This method allows the computation of the Slant Total Electron Content (STEC).
         * <p>
         * This method follows the Gauss algorithm exposed in section 2.5.8.2.8 of
         * the reference document.
         * </p>
         * @param dateTime current date
         * @param ray ray-perigee parameters
         * @return the STEC in TECUnits
         */
        double stec(DateTimeComponents dateTime, Ray ray);

        /**
         * This method allows the computation of the Slant Total Electron Content (STEC).
         * <p>
         * This method follows the Gauss algorithm exposed in section 2.5.8.2.8 of
         * the reference document.
         * </p>
         * @param <T> type of the field elements
         * @param dateTime current date
         * @param ray ray-perigee parameters
         * @return the STEC in TECUnits
         */
        <T extends CalculusFieldElement<T>> T stec(DateTimeComponents dateTime, FieldRay<T> ray);

    }

    /** Galileo-specific version of NeQuick engine.
     * @since 13.0
     */
    private class GalileoEngine implements NeQuickEngine {

        /** Starting number of points for integration. */
        private static final int N_START = 8;

        /** Broadcast ionization engine coefficients. */
        private final double[] alpha;

        /** Build a new instance.
         * @param alpha broadcast ionization engine coefficients
         */
        GalileoEngine(final double[] alpha) {
            this.alpha = alpha.clone();
        }

        /** Compute effective ionization level.
         * @param modip modified dip latitude at receiver location
         * @return effective ionization level (Az in Nequick Galileo, R12 in original Nequick ITU)
         */
        private double effectiveIonizationLevel(final double modip) {
            // Particular condition (Eq. 17)
            if (alpha[0] == 0.0 && alpha[1] == 0.0 && alpha[2] == 0.0) {
                return 63.7;
            } else {
                // Az = a0 + modip * a1 + modip² * a2 (Eq. 18)
                return FastMath.min(FastMath.max(alpha[0] + modip * (alpha[1] + modip * alpha[2]), 0.0), 400.0);
            }
        }

        /** Compute effective ionization level.
         * @param <T> type of the field elements
         * @param modip modified dip latitude at receiver location
         * @return effective ionization level (Az in Nequick Galileo, R12 in original Nequick ITU)
         */
        private <T extends CalculusFieldElement<T>> T effectiveIonizationLevel(final T modip) {
            // Particular condition (Eq. 17)
            if (alpha[0] == 0.0 && alpha[1] == 0.0 && alpha[2] == 0.0) {
                return modip.newInstance(63.7);
            } else {
                // Az = a0 + modip * a1 + modip² * a2 (Eq. 18)
                return FastMath.min(FastMath.max(modip.multiply(alpha[2]).add(alpha[1]).multiply(modip).add(alpha[0]),
                                                 0.0),
                                    400.0);
            }
        }

        /** {@inheritDoc} */
        @Override
        public double stec(final DateTimeComponents dateTime, final Ray ray) {

            // Tolerance for the integration accuracy. Defined inside the reference document, section 2.5.8.1.
            final double h1 = ray.getRecH();
            final double tolerance;
            if (h1 < 1000000.0) {
                tolerance = 0.001;
            } else {
                tolerance = 0.01;
            }

            // Integration
            int n = N_START;
            final Segment seg1 = new Segment(n, ray, ray.getS1(), ray.getS2());
            double gn1 = stecIntegration(dateTime, seg1);
            n *= 2;
            final Segment seg2 = new Segment(n, ray, ray.getS1(), ray.getS2());
            double gn2 = stecIntegration(dateTime, seg2);

            int count = 1;
            while (FastMath.abs(gn2 - gn1) > tolerance * FastMath.abs(gn1) && count < 20) {
                gn1 = gn2;
                n *= 2;
                final Segment seg = new Segment(n, ray, ray.getS1(), ray.getS2());
                gn2 = stecIntegration(dateTime, seg);
                count += 1;
            }

            // If count > 20 the integration did not converge
            if (count == 20) {
                throw new OrekitException(OrekitMessages.STEC_INTEGRATION_DID_NOT_CONVERGE);
            }

            // Eq. 202
            return (gn2 + ((gn2 - gn1) / 15.0)) * 1.0e-16;
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> T stec(final DateTimeComponents dateTime, final FieldRay<T> ray) {

            // Tolerance for the integration accuracy. Defined inside the reference document, section 2.5.8.1.
            final T h1 = ray.getRecH();
            final double tolerance;
            if (h1.getReal() < 1000000.0) {
                tolerance = 0.001;
            } else {
                tolerance = 0.01;
            }

            // Integration
            int n = N_START;
            final FieldSegment<T> seg1 = new FieldSegment<>(n, ray, ray.getS1(), ray.getS2());
            T gn1 = stecIntegration(dateTime, seg1);
            n *= 2;
            final FieldSegment<T> seg2 = new FieldSegment<>(n, ray, ray.getS1(), ray.getS2());
            T gn2 = stecIntegration(dateTime, seg2);

            int count = 1;
            while (FastMath.abs(gn2.subtract(gn1)).getReal() > FastMath.abs(gn1).multiply(tolerance).getReal() &&
                   count < 20) {
                gn1 = gn2;
                n *= 2;
                final FieldSegment<T> seg = new FieldSegment<>(n, ray, ray.getS1(), ray.getS2());
                gn2 = stecIntegration(dateTime, seg);
                count += 1;
            }

            // If count > 20 the integration did not converge
            if (count == 20) {
                throw new OrekitException(OrekitMessages.STEC_INTEGRATION_DID_NOT_CONVERGE);
            }

            // Eq. 202
            return gn2.add(gn2.subtract(gn1).divide(15.0)).multiply(1.0e-16);
        }

        /**
         * This method performs the STEC integration.
         *
         * @param dateTime current date and time components
         * @param seg      coordinates along the integration path
         * @return result of the integration
         */
        private double stecIntegration(final DateTimeComponents dateTime, final Segment seg) {

            // Compute electron density
            double density = 0.0;
            for (int i = 0; i < seg.getNbPoints(); i++) {
                final GeodeticPoint gp = seg.getPoint(i);
                final double modip = modipGrid.computeMODIP(gp.getLatitude(), gp.getLongitude());
                final double az = effectiveIonizationLevel(modip);
                density += electronDensity(dateTime, modip, az,
                                           gp.getLatitude(), gp.getLongitude(), gp.getAltitude());
            }

            return 0.5 * seg.getInterval() * density;
        }

        /**
         * This method performs the STEC integration.
         *
         * @param <T>      type of the elements
         * @param dateTime current date and time components
         * @param seg      coordinates along the integration path
         * @return result of the integration
         */
        private <T extends CalculusFieldElement<T>> T stecIntegration(final DateTimeComponents dateTime,
                                                                      final FieldSegment<T> seg) {
            // Compute electron density
            T density = seg.getInterval().getField().getZero();
            for (int i = 0; i < seg.getNbPoints(); i++) {
                final FieldGeodeticPoint<T> gp = seg.getPoint(i);
                final T modip = modipGrid.computeMODIP(gp.getLatitude(), gp.getLongitude());
                final T az = effectiveIonizationLevel(modip);
                density = density.add(electronDensity(dateTime, modip, az,
                                                      gp.getLatitude(), gp.getLongitude(), gp.getAltitude()));
            }

            return seg.getInterval().multiply(density).multiply(0.5);
        }

    }

    /** Original ITU version of NeQuick engine.
     * @since 13.0
     */
    private class ItuEngine implements NeQuickEngine {

        /** One thousand kilometer height. */
        private static final double H_1000 = 1000000.0;

        /** Two thousands kilometer height. */
        private static final double H_2000 = 2000000.0;

        /** Starting number of points for integration. */
        private static final int N_START = 8;

        /** Max number of points for integration. */
        private static final int N_STOP = 1024;

        /** Small convergence criterion. */
        private static final double EPS_SMALL = 1.0e-3;

        /** Medium convergence criterion. */
        private static final double EPS_MEDIUM = 1.0e-2;

        /** Solar flux. */
        private final double f107;

        /** Build a new instance.
         * @param f107 solar flux
         */
        ItuEngine(final double f107) {
            this.f107 = f107;
        }

        /** {@inheritDoc} */
        @Override
        public double stec(final DateTimeComponents dateTime, final Ray ray) {
            if (ray.getSatH() <= H_2000) {
                if (ray.getRecH() >= H_1000) {
                    // only one integration interval
                    return stecIntegration(dateTime, EPS_SMALL, ray, ray.getS1(), ray.getS2());
                } else {
                    // two integration intervals, below and above 1000km
                    final double h1000 = RE + H_1000;
                    final double s1000 = FastMath.sqrt(h1000 * h1000 - ray.getRadius() * ray.getRadius());
                    return stecIntegration(dateTime, EPS_SMALL, ray, ray.getS1(), s1000) +
                           stecIntegration(dateTime, EPS_MEDIUM, ray, s1000, ray.getS2());
                }
            } else {
                if (ray.getRecH() >= H_2000) {
                    // only one integration interval
                    return stecIntegration(dateTime, EPS_SMALL, ray, ray.getS1(), ray.getS2());
                } else {
                    final double h2000 = RE + H_2000;
                    final double s2000 = FastMath.sqrt(h2000 * h2000 - ray.getRadius() * ray.getRadius());
                    if (ray.getRecH() >= H_1000) {
                        // two integration intervals, below and above 2000km
                        return stecIntegration(dateTime, EPS_SMALL, ray, ray.getS1(), s2000) +
                               stecIntegration(dateTime, EPS_SMALL, ray, s2000, ray.getS2());
                    } else {
                        // three integration intervals, below 1000km, between 1000km and 2000km, and above 2000km
                        final double h1000 = RE + H_1000;
                        final double s1000 = FastMath.sqrt(h1000 * h1000 - ray.getRadius() * ray.getRadius());
                        return stecIntegration(dateTime, EPS_SMALL,  ray, ray.getS1(), s1000) +
                               stecIntegration(dateTime, EPS_MEDIUM, ray, s1000, s2000) +
                               stecIntegration(dateTime, EPS_MEDIUM, ray, s2000, ray.getS2());
                    }
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> T stec(final DateTimeComponents dateTime, final FieldRay<T> ray) {
            if (ray.getSatH().getReal() <= H_2000) {
                if (ray.getRecH().getReal() >= H_1000) {
                    // only one integration interval
                    return stecIntegration(dateTime, EPS_SMALL, ray, ray.getS1(), ray.getS2());
                } else {
                    // two integration intervals, below and above 1000km
                    final double h1000 = RE + H_1000;
                    final T s1000 = FastMath.sqrt(ray.getRadius().multiply(ray.getRadius()).negate().add(h1000 * h1000));
                    return stecIntegration(dateTime, EPS_SMALL, ray, ray.getS1(), s1000).
                           add(stecIntegration(dateTime, EPS_MEDIUM, ray, s1000, ray.getS2()));
                }
            } else {
                if (ray.getRecH().getReal() >= H_2000) {
                    // only one integration interval
                    return stecIntegration(dateTime, EPS_SMALL, ray, ray.getS1(), ray.getS2());
                } else {
                    final double h2000 = RE + H_2000;
                    final T s2000 = FastMath.sqrt(ray.getRadius().multiply(ray.getRadius()).negate().add(h2000 * h2000));
                    if (ray.getRecH().getReal() >= H_1000) {
                        // two integration intervals, below and above 2000km
                        return stecIntegration(dateTime, EPS_SMALL, ray, ray.getS1(), s2000).
                               add(stecIntegration(dateTime, EPS_SMALL, ray, s2000, ray.getS2()));
                    } else {
                        // three integration intervals, below 1000km, between 1000km and 2000km, and above 2000km
                        final double h1000 = RE + H_1000;
                        final T s1000 = FastMath.sqrt(ray.getRadius().multiply(ray.getRadius()).negate().add(h1000 * h1000));
                        return stecIntegration(dateTime, EPS_SMALL, ray, ray.getS1(), s1000).
                               add(stecIntegration(dateTime, EPS_MEDIUM, ray, s1000, s2000)).
                               add(stecIntegration(dateTime, EPS_MEDIUM, ray, s2000, ray.getS2()));
                    }
                }
            }
        }

        /**
         * This method performs the STEC integration.
         *
         * @param dateTime current date and time components
         * @param eps convergence criterion
         * @param ray ray-perigee parameters
         * @param s1  lower boundary of integration
         * @param s2  upper boundary for integration
         * @return result of the integration
         */
        private double stecIntegration(final DateTimeComponents dateTime, final double eps,
                                       final Ray ray, final double s1, final double s2) {

            double gInt1 = Double.NaN;
            double gInt2 = Double.NaN;

            for (int n = N_START; n <= N_STOP; n = 2 * n) {

                // integrate with n intervals (2n points)
                final Segment segment = new Segment(n, ray, s1, s2);
                double sum = 0;
                for (int i = 0; i < segment.getNbPoints(); ++i) {
                    final GeodeticPoint gp = segment.getPoint(i);
                    final double modip = modipGrid.computeMODIP(gp.getLatitude(), gp.getLongitude());
                    final double ed    = electronDensity(dateTime, modip, f107,
                                           gp.getLatitude(), gp.getLongitude(), gp.getAltitude());
                    sum += ed;
                }

                gInt1 = gInt2;
                gInt2 = sum * 0.5 * segment.getInterval();
                if (FastMath.abs(gInt1 - gInt2) <= FastMath.abs(gInt1 * eps)) {
                    // convergence reached
                    break;
                }

            }

            return Unit.TOTAL_ELECTRON_CONTENT_UNIT.fromSI(gInt1 + (gInt2 - gInt1) / 15.0);

        }

        /**
         * This method performs the STEC integration.
         *
         * @param dateTime current date and time components
         * @param eps convergence criterion
         * @param ray ray-perigee parameters
         * @param s1  lower boundary of integration
         * @param s2  upper boundary for integration
         * @return result of the integration
         */
        private <T extends CalculusFieldElement<T>> T stecIntegration(final DateTimeComponents dateTime,
                                                                      final double eps,
                                                                      final FieldRay<T> ray, final T s1, final T s2) {

            T gInt1 = s1.newInstance(Double.NaN);
            T gInt2 = s1.newInstance(Double.NaN);
            T f107T = s1.newInstance(f107);

            for (int n = N_START; n <= N_STOP; n = 2 * n) {

                // integrate with n intervals (2n points)
                final FieldSegment<T> segment = new FieldSegment<>(n, ray, s1, s2);
                T sum = s1.getField().getZero();
                for (int i = 0; i < segment.getNbPoints(); ++i) {
                    final FieldGeodeticPoint<T> gp = segment.getPoint(i);
                    final T modip = modipGrid.computeMODIP(gp.getLatitude(), gp.getLongitude());
                    final T ed    = electronDensity(dateTime, modip, f107T,
                                                    gp.getLatitude(), gp.getLongitude(), gp.getAltitude());
                    sum = sum.add(ed);
                }

                gInt1 = gInt2;
                gInt2 = sum.multiply(0.5).multiply(segment.getInterval());
                if (FastMath.abs(gInt1.subtract(gInt2).getReal()) <= FastMath.abs(gInt1.getReal() * eps)) {
                    // convergence reached
                    break;
                }

            }

            return Unit.TOTAL_ELECTRON_CONTENT_UNIT.fromSI(gInt1.add(gInt2.subtract(gInt1).divide(15.0)));

        }

    }

    /** Get the modip grid for specified NeQuick version.
     * @param version version of the NeQuick model
     * @return modip grid for specified version
     */
    private static ModipGrid getModipGrid(final NeQuickVersion version) {
        switch (version) {
            case NEQUICK_2_GALILEO:
                return GalileoHolder.INSTANCE;
            case NEQUICK_2_ITU:
                return ItuHolder.INSTANCE;
            default:
                // this should never happen
                throw new OrekitInternalError(null);
        }
    }

    /** Holder for the Galileo-specific modip singleton.
     * <p>
     * We use the initialization on demand holder idiom to store the singleton,
     * as it is both thread-safe, efficient (no synchronization) and works with
     * all versions of java.
     * </p>
     */
    private static class GalileoHolder {

        /** Unique instance. */
        private static final ModipGrid INSTANCE =
            new ModipGrid(NeQuickVersion.NEQUICK_2_GALILEO.getnbCellsLon(),
                          NeQuickVersion.NEQUICK_2_GALILEO.getnbCellsLat(),
                          NeQuickVersion.NEQUICK_2_GALILEO.getSource(),
                          NeQuickVersion.NEQUICK_2_GALILEO.isWrappingAlreadyIncluded());

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private GalileoHolder() {
        }

    }

    /** Holder for the ITU modip singleton.
     * <p>
     * We use the initialization on demand holder idiom to store the singleton,
     * as it is both thread-safe, efficient (no synchronization) and works with
     * all versions of java.
     * </p>
     */
    private static class ItuHolder {

        /** Unique instance. */
        private static final ModipGrid INSTANCE =
            new ModipGrid(NeQuickVersion.NEQUICK_2_ITU.getnbCellsLon(),
                          NeQuickVersion.NEQUICK_2_ITU.getnbCellsLat(),
                          NeQuickVersion.NEQUICK_2_ITU.getSource(),
                          NeQuickVersion.NEQUICK_2_ITU.isWrappingAlreadyIncluded());

        /** Private constructor.
         * <p>This class is a utility class, it should neither have a public
         * nor a default constructor. This private constructor prevents
         * the compiler from generating one automatically.</p>
         */
        private ItuHolder() {
        }

    }

}
