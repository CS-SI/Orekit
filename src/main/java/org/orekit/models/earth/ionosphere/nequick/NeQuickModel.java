/* Copyright 2002-2025 CS GROUP
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
package org.orekit.models.earth.ionosphere.nequick;

import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.ionosphere.IonosphericModel;
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
public abstract class NeQuickModel implements IonosphericModel {

    /** Mean Earth radius in m (Ref Table 2.5.2). */
    public static final double RE = 6371200.0;

    /** Factor for the electron density computation. */
    private static final double DENSITY_FACTOR = 1.0e11;

    /** Factor for the path delay computation. */
    private static final double DELAY_FACTOR = 40.3e16;

    /** F2 coefficients used by the F2 layer (flatten array for cache efficiency). */
    private final double[][] flattenF2;

    /** Fm3 coefficients used by the M(3000)F2 layer(flatten array for cache efficiency). */
    private final double[][] flattenFm3;

    /** UTC time scale. */
    private final TimeScale utc;

    /** Simple constructor.
     * @param utc UTC time scale
     * @since 13.0
     */
    protected NeQuickModel(final TimeScale utc) {

        this.utc = utc;

        // F2 layer values
        this.flattenF2  = new double[12][];
        this.flattenFm3 = new double[12][];

    }

    /** Get UTC time scale.
     * @return UTC time scale
     * @since 13.0
     */
    public TimeScale getUtc() {
        return utc;
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
        final double tec = stec(date, recPoint, satPoint);

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
        return stec(date.getComponents(utc), new Ray(recP, satP));
    }

    /**
     * This method allows the computation of the Slant Total Electron Content (STEC).
     * @param <T> type of the elements
     * @param date current date
     * @param recP receiver position
     * @param satP satellite position
     * @return the STEC in TECUnits
     */
    public <T extends CalculusFieldElement<T>> T stec(final FieldAbsoluteDate<T> date,
                                                      final FieldGeodeticPoint<T> recP,
                                                      final FieldGeodeticPoint<T> satP) {
        return stec(date.getComponents(utc), new FieldRay<>(recP, satP));
    }

    /** Compute modip for a location.
     * @param latitude latitude
     * @param longitude longitude
     * @return modip at specified location
     * @since 13.0
     */
    protected abstract double computeMODIP(double latitude, double longitude);

    /** Compute modip for a location.
     * @param <T> type of the field elements
     * @param latitude latitude
     * @param longitude longitude
     * @return modip at specified location
     * @since 13.0
     */
    protected abstract <T extends CalculusFieldElement<T>> T computeMODIP(T latitude, T longitude);

    /**
     * Compute Fourier time series.
     * @param dateTime current date time components
     * @param az effective ionisation level
     * @return Fourier time series
     * @since 13.1
     */
    public FourierTimeSeries computeFourierTimeSeries(final DateTimeComponents dateTime, final double az) {

         // Load the correct CCIR file
        loadsIfNeeded(dateTime.getDate());

        return new FourierTimeSeries(dateTime, az,
                                     flattenF2[dateTime.getDate().getMonth() - 1],
                                     flattenFm3[dateTime.getDate().getMonth() - 1]);

    }

    /**
     * Computes the electron density at a given height.
     * @param dateTime date
     * @param az effective ionization level
     * @param latitude latitude along the integration path
     * @param longitude longitude along the integration path
     * @param h height along the integration path in m
     * @return electron density [m⁻³]
     * @since 13.0
     * @deprecated as of 13.1, replaced by {@link #electronDensity(FourierTimeSeries, double, double, double)}
     */
    @Deprecated
    public double electronDensity(final DateTimeComponents dateTime, final double az,
                                  final double latitude, final double longitude, final double h) {
        return electronDensity(computeFourierTimeSeries(dateTime, az), latitude, longitude, h);
    }

    /**
     * Computes the electron density at a given height.
     * @param fourierTimeSeries Fourier time series for foF2 and M(3000)F2 layer (flatten array)
     * @param latitude latitude along the integration path
     * @param longitude longitude along the integration path
     * @param h height along the integration path in m
     * @return electron density [m⁻³]
     * @since 13.1
     */
    public double electronDensity(final FourierTimeSeries fourierTimeSeries,
                                  final double latitude, final double longitude, final double h) {

        final double modip = computeMODIP(latitude, longitude);
        final NeQuickParameters parameters = new NeQuickParameters(fourierTimeSeries, latitude, longitude, modip);

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
     * Compute Fourier time series.
     * @param <T> type of the elements
     * @param dateTime current date time components
     * @param az effective ionisation level
     * @return Fourier time series
     * @since 13.1
     */
    public <T extends CalculusFieldElement<T>> FieldFourierTimeSeries<T> computeFourierTimeSeries(final DateTimeComponents dateTime,
                                                                                                  final T az) {

         // Load the correct CCIR file
        loadsIfNeeded(dateTime.getDate());

        return new FieldFourierTimeSeries<>(dateTime, az,
                                            flattenF2[dateTime.getDate().getMonth() - 1],
                                            flattenFm3[dateTime.getDate().getMonth() - 1]);

    }

    /**
     * Computes the electron density at a given height.
     * @param <T> type of the elements
     * @param dateTime date
     * @param az effective ionization level
     * @param latitude latitude along the integration path
     * @param longitude longitude along the integration path
     * @param h height along the integration path in m
     * @return electron density [m⁻³]
     * @since 13.0
     * @deprecated as of 13.1, replaced by {@link #electronDensity(FieldFourierTimeSeries,
     * CalculusFieldElement, CalculusFieldElement, CalculusFieldElement)}
     */
    @Deprecated
    public <T extends CalculusFieldElement<T>> T electronDensity(final DateTimeComponents dateTime, final T az,
                                                                 final T latitude, final T longitude, final T h) {
        return electronDensity(computeFourierTimeSeries(dateTime, az), latitude, longitude, h);
    }

    /**
     * Computes the electron density at a given height.
     * @param <T> type of the elements
     * @param fourierTimeSeries Fourier time series for foF2 and M(3000)F2 layer (flatten array)
     * @param latitude latitude along the integration path
     * @param longitude longitude along the integration path
     * @param h height along the integration path in m
     * @return electron density [m⁻³]
     * @since 13.1
     */
    public <T extends CalculusFieldElement<T>> T electronDensity(final FieldFourierTimeSeries<T> fourierTimeSeries,
                                                                 final T latitude, final T longitude, final T h) {

        final T modip = computeMODIP(latitude, longitude);
        final FieldNeQuickParameters<T> parameters = new FieldNeQuickParameters<>(fourierTimeSeries, latitude, longitude, modip);

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
        final double be  = parameters.getBE(h);
        final double bf1 = parameters.getBF1(h);
        final double bf2 = parameters.getB2Bot();

        // Useful array of constants
        final double[] ct = new double[] {
            1.0 / bf2, 1.0 / bf1, 1.0 / be
        };

        // Compute the exponential argument for each layer (Eq. 111 to 113)
        final double[] arguments = computeExponentialArguments(h, parameters);

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
        final double aNo = s[0] + s[1] + s[2];
        if (applyChapmanParameters(h)) {
            // Chapman parameters (Eq. 119 and 120)
            final double bc = 1.0 - 10.0 * (MathArrays.linearCombination(s[0], ds[0], s[1], ds[1], s[2], ds[2]) / aNo);
            final double z  = 0.1 * (h - 100.0);
            // Electron density (Eq. 121)
            return aNo * clipExp(1.0 - bc * z - clipExp(-z)) * DENSITY_FACTOR;
        } else {
            return aNo * DENSITY_FACTOR;
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
        final T be  = parameters.getBE(h);
        final T bf1 = parameters.getBF1(h);
        final T bf2 = parameters.getB2Bot();

        // Useful array of constants
        final T[] ct = MathArrays.buildArray(field, 3);
        ct[0] = bf2.reciprocal();
        ct[1] = bf1.reciprocal();
        ct[2] = be.reciprocal();

        // Compute the exponential argument for each layer (Eq. 111 to 113)
        final T[] arguments = computeExponentialArguments(h, parameters);

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
        final T aNo = s[0].add(s[1]).add(s[2]);
        if (applyChapmanParameters(h.getReal())) {
            // Chapman parameters (Eq. 119 and 120)
            final T bc = one.linearCombination(s[0], ds[0], s[1], ds[1], s[2], ds[2]).divide(aNo).multiply(10.0).negate().add(1.0);
            final T z  = h.subtract(100.0).multiply(0.1);
            // Electron density (Eq. 121)
            return aNo.multiply(clipExp(bc.multiply(z).add(clipExp(z.negate())).negate().add(1.0))).multiply(DENSITY_FACTOR);
        } else {
            return aNo.multiply(DENSITY_FACTOR);
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
        final double h0     = computeH0(parameters);
        final double z      = deltaH / (h0 * (1.0 + (r * g * deltaH) / (r * h0 + g * deltaH)));

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
        final T h0     = computeH0(parameters);
        final T z      = deltaH.divide(h0.multiply(deltaH.multiply(r).multiply(g).divide(h0.multiply(r).add(deltaH.multiply(g))).add(1.0)));

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
    protected double clipExp(final double power) {
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
    protected <T extends CalculusFieldElement<T>> T clipExp(final T power) {
        if (power.getReal() > 80.0) {
            return power.newInstance(5.5406E34);
        } else if (power.getReal() < -80) {
            return power.newInstance(1.8049E-35);
        } else {
            return FastMath.exp(power);
        }
    }

    /**
     * This method allows the computation of the Slant Total Electron Content (STEC).
     *
     * @param dateTime current date
     * @param ray      ray-perigee parameters
     * @return the STEC in TECUnits
     */
    abstract double stec(DateTimeComponents dateTime, Ray ray);

    /**
     * This method allows the computation of the Slant Total Electron Content (STEC).
     *
     * @param <T>      type of the field elements
     * @param dateTime current date
     * @param ray      ray-perigee parameters
     * @return the STEC in TECUnits
     */
    abstract <T extends CalculusFieldElement<T>> T stec(DateTimeComponents dateTime, FieldRay<T> ray);

    /**
     * Check if Chapman parameters should be applied.
     *
     * @param hInKm height in kilometers
     * @return true if Chapman parameters should be applied
     * @since 13.0
     */
    abstract boolean applyChapmanParameters(double hInKm);

    /**
     * Compute exponential arguments.
     * @param h height in km
     * @param parameters NeQuick model parameters
     * @return exponential arguments
     * @since 13.0
     */
    abstract double[] computeExponentialArguments(double h, NeQuickParameters parameters);

    /**
     * Compute exponential arguments.
     * @param <T>   type of the field elements
     * @param h height in km
     * @param parameters NeQuick model parameters
     * @return exponential arguments
     * @since 13.0
     */
    abstract <T extends CalculusFieldElement<T>> T[] computeExponentialArguments(T h,
                                                                                 FieldNeQuickParameters<T> parameters);

    /**
     * Compute topside thickness parameter.
     * @param parameters NeQuick model parameters
     * @return topside thickness parameter
     * @since 13.0
     */
    abstract double computeH0(NeQuickParameters parameters);

    /**
     * Compute topside thickness parameter.
     * @param <T>   type of the field elements
     * @param parameters NeQuick model parameters
     * @return topside thickness parameter
     * @since 13.0
     */
    abstract <T extends CalculusFieldElement<T>> T computeH0(FieldNeQuickParameters<T> parameters);

}
