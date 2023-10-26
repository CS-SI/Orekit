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
package org.orekit.models.earth.ionosphere;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.SinCos;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
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

/**
 * NeQuick ionospheric delay model.
 *
 * @author Bryan Cazabonne
 *
 * @see "European Union (2016). European GNSS (Galileo) Open Service-Ionospheric Correction
 *       Algorithm for Galileo Single Frequency Users. 1.2."
 *
 * @since 10.1
 */
public class NeQuickModel implements IonosphericModel {

    /** NeQuick resources base directory. */
    private static final String NEQUICK_BASE = "/assets/org/orekit/nequick/";

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile("\\s+");

    /** Mean Earth radius in m (Ref Table 2.5.2). */
    private static final double RE = 6371200.0;

    /** Meters to kilometers converter. */
    private static final double M_TO_KM = 0.001;

    /** Factor for the electron density computation. */
    private static final double DENSITY_FACTOR = 1.0e11;

    /** Factor for the path delay computation. */
    private static final double DELAY_FACTOR = 40.3e16;

    /** The three ionospheric coefficients broadcast in the Galileo navigation message. */
    private final double[] alpha;

    /** MODIP grid. */
    private final double[][] stModip;

    /** Month used for loading CCIR coefficients. */
    private int month;

    /** F2 coefficients used by the F2 layer. */
    private double[][][] f2;

    /** Fm3 coefficients used by the F2 layer. */
    private double[][][] fm3;

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
     * Build a new instance.
     * @param alpha effective ionisation level coefficients
     * @param utc UTC time scale.
     * @since 10.1
     */
    public NeQuickModel(final double[] alpha,
                        final TimeScale utc) {
        // F2 layer values
        this.month = 0;
        this.f2    = null;
        this.fm3   = null;
        // Read modip grid
        final MODIPLoader parser = new MODIPLoader();
        parser.loadMODIPGrid();
        this.stModip = parser.getMODIPGrid();
        // Ionisation level coefficients
        this.alpha = alpha.clone();
        this.utc = utc;
    }

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
        final GeodeticPoint satPoint = ellipsoid.transform(state.getPosition(ellipsoid.getBodyFrame()), ellipsoid.getBodyFrame(), state.getDate());

        // Total Electron Content
        final double tec = stec(date, recPoint, satPoint);

        // Ionospheric delay
        final double factor = DELAY_FACTOR / (frequency * frequency);
        return factor * tec;
    }

    @Override
    public <T extends CalculusFieldElement<T>> T pathDelay(final FieldSpacecraftState<T> state, final TopocentricFrame baseFrame,
                                                       final double frequency, final T[] parameters) {
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

    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /**
     * This method allows the computation of the Stant Total Electron Content (STEC).
     * <p>
     * This method follows the Gauss algorithm exposed in section 2.5.8.2.8 of
     * the reference document.
     * </p>
     * @param date current date
     * @param recP receiver position
     * @param satP satellite position
     * @return the STEC in TECUnits
     */
    public double stec(final AbsoluteDate date, final GeodeticPoint recP, final GeodeticPoint satP) {

        // Ray-perigee parameters
        final Ray ray = new Ray(recP, satP);

        // Load the correct CCIR file
        final DateTimeComponents dateTime = date.getComponents(utc);
        loadsIfNeeded(dateTime.getDate());

        // Tolerance for the integration accuracy. Defined inside the reference document, section 2.5.8.1.
        final double h1 = recP.getAltitude();
        final double tolerance;
        if (h1 < 1000000.0) {
            tolerance = 0.001;
        } else {
            tolerance = 0.01;
        }

        // Integration
        int n = 8;
        final Segment seg1 = new Segment(n, ray);
        double gn1 = stecIntegration(seg1, dateTime);
        n *= 2;
        final Segment seg2 = new Segment(n, ray);
        double gn2 = stecIntegration(seg2, dateTime);

        int count = 1;
        while (FastMath.abs(gn2 - gn1) > tolerance * FastMath.abs(gn1) && count < 20) {
            gn1 = gn2;
            n *= 2;
            final Segment seg = new Segment(n, ray);
            gn2 = stecIntegration(seg, dateTime);
            count += 1;
        }

        // If count > 20 the integration did not converge
        if (count == 20) {
            throw new OrekitException(OrekitMessages.STEC_INTEGRATION_DID_NOT_CONVERGE);
        }

        // Eq. 202
        return (gn2 + ((gn2 - gn1) / 15.0)) * 1.0e-16;
    }

    /**
     * This method allows the computation of the Stant Total Electron Content (STEC).
     * <p>
     * This method follows the Gauss algorithm exposed in section 2.5.8.2.8 of
     * the reference document.
     * </p>
     * @param <T> type of the elements
     * @param date current date
     * @param recP receiver position
     * @param satP satellite position
     * @return the STEC in TECUnits
     */
    public <T extends CalculusFieldElement<T>> T stec(final FieldAbsoluteDate<T> date,
                                                  final FieldGeodeticPoint<T> recP,
                                                  final FieldGeodeticPoint<T> satP) {

        // Field
        final Field<T> field = date.getField();

        // Ray-perigee parameters
        final FieldRay<T> ray = new FieldRay<>(field, recP, satP);

        // Load the correct CCIR file
        final DateTimeComponents dateTime = date.getComponents(utc);
        loadsIfNeeded(dateTime.getDate());

        // Tolerance for the integration accuracy. Defined inside the reference document, section 2.5.8.1.
        final T h1 = recP.getAltitude();
        final double tolerance;
        if (h1.getReal() < 1000000.0) {
            tolerance = 0.001;
        } else {
            tolerance = 0.01;
        }

        // Integration
        int n = 8;
        final FieldSegment<T> seg1 = new FieldSegment<>(field, n, ray);
        T gn1 = stecIntegration(field, seg1, dateTime);
        n *= 2;
        final FieldSegment<T> seg2 = new FieldSegment<>(field, n, ray);
        T gn2 = stecIntegration(field, seg2, dateTime);

        int count = 1;
        while (FastMath.abs(gn2.subtract(gn1)).getReal() > FastMath.abs(gn1).multiply(tolerance).getReal() && count < 20) {
            gn1 = gn2;
            n *= 2;
            final FieldSegment<T> seg = new FieldSegment<>(field, n, ray);
            gn2 = stecIntegration(field, seg, dateTime);
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
     * This method perfoms the STEC integration.
     * @param seg coordinates along the integration path
     * @param dateTime current date and time componentns
     * @return result of the integration
     */
    private double stecIntegration(final Segment seg, final DateTimeComponents dateTime) {
        // Integration points
        final double[] heightS    = seg.getHeights();
        final double[] latitudeS  = seg.getLatitudes();
        final double[] longitudeS = seg.getLongitudes();

        // Compute electron density
        double density = 0.0;
        for (int i = 0; i < heightS.length; i++) {
            final NeQuickParameters parameters = new NeQuickParameters(dateTime, f2, fm3,
                                                                       latitudeS[i], longitudeS[i],
                                                                       alpha, stModip);
            density += electronDensity(heightS[i], parameters);
        }

        return 0.5 * seg.getInterval() * density;
    }

    /**
     * This method perfoms the STEC integration.
     * @param <T> type of the elements
     * @param field field of the elements
     * @param seg coordinates along the integration path
     * @param dateTime current date and time componentns
     * @return result of the integration
     */
    private <T extends CalculusFieldElement<T>> T stecIntegration(final Field<T> field,
                                                              final FieldSegment<T> seg,
                                                              final DateTimeComponents dateTime) {
        // Integration points
        final T[] heightS    = seg.getHeights();
        final T[] latitudeS  = seg.getLatitudes();
        final T[] longitudeS = seg.getLongitudes();

        // Compute electron density
        T density = field.getZero();
        for (int i = 0; i < heightS.length; i++) {
            final FieldNeQuickParameters<T> parameters = new FieldNeQuickParameters<>(field, dateTime, f2, fm3,
                                                                                      latitudeS[i], longitudeS[i],
                                                                                      alpha, stModip);
            density = density.add(electronDensity(field, heightS[i], parameters));
        }

        return seg.getInterval().multiply(density).multiply(0.5);
    }

    /**
     * Computes the electron density at a given height.
     * @param h height in m
     * @param parameters NeQuick model parameters
     * @return electron density [m^-3]
     */
    private double electronDensity(final double h, final NeQuickParameters parameters) {
        // Convert height in kilometers
        final double hInKm = h * M_TO_KM;
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
     * @param field field of the elements
     * @param h height in m
     * @param parameters NeQuick model parameters
     * @return electron density [m^-3]
     */
    private <T extends CalculusFieldElement<T>> T electronDensity(final Field<T> field,
                                                              final T h,
                                                              final FieldNeQuickParameters<T> parameters) {
        // Convert height in kilometers
        final T hInKm = h.multiply(M_TO_KM);
        // Electron density
        final T n;
        if (hInKm.getReal() <= parameters.getHmF2().getReal()) {
            n = bottomElectronDensity(field, hInKm, parameters);
        } else {
            n = topElectronDensity(field, hInKm, parameters);
        }
        return n;
    }

    /**
     * Computes the electron density of the bottomside.
     * @param h height in km
     * @param parameters NeQuick model parameters
     * @return the electron density N in m-3
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
     * @param field field of the elements
     * @param h height in km
     * @param parameters NeQuick model parameters
     * @return the electron density N in m-3
     */
    private <T extends CalculusFieldElement<T>> T bottomElectronDensity(final Field<T> field,
                                                                    final T h,
                                                                    final FieldNeQuickParameters<T> parameters) {

        // Zero and One
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
        final T   hTemp = FastMath.max(zero.add(100.0), h);
        final T   exp   = clipExp(field, FastMath.abs(hTemp.subtract(parameters.getHmF2())).add(1.0).divide(10.0).reciprocal());
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
                final T expA   = clipExp(field, arguments[i]);
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
            final T bc = s[0].multiply(ds[0]).add(one.linearCombination(s[0], ds[0], s[1], ds[1], s[2], ds[2])).divide(aNo).multiply(10.0).negate().add(1.0);
            final T z  = h.subtract(100.0).multiply(0.1);
            // Electron density (Eq. 121)
            return aNo.multiply(clipExp(field, bc.multiply(z).add(clipExp(field, z.negate())).negate().add(1.0))).multiply(DENSITY_FACTOR);
        }
    }

    /**
     * Computes the electron density of the topside.
     * @param h height in km
     * @param parameters NeQuick model parameters
     * @return the electron density N in m-3
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
     * @param field field of the elements
     * @param h height in km
     * @param parameters NeQuick model parameters
     * @return the electron density N in m-3
     */
    private <T extends CalculusFieldElement<T>> T topElectronDensity(final Field<T> field,
                                                                 final T h,
                                                                 final FieldNeQuickParameters<T> parameters) {

        // Constant parameters (Eq. 122 and 123)
        final double g = 0.125;
        final double r = 100.0;

        // Arguments deltaH and z (Eq. 124 and 125)
        final T deltaH = h.subtract(parameters.getHmF2());
        final T z      = deltaH.divide(parameters.getH0().multiply(deltaH.multiply(r).multiply(g).divide(parameters.getH0().multiply(r).add(deltaH.multiply(g))).add(1.0)));

        // Exponential (Eq. 126)
        final T ee = clipExp(field, z);

        // Electron density (Eq. 127)
        if (ee.getReal() > 1.0e11) {
            return parameters.getNmF2().multiply(4.0).divide(ee).multiply(DENSITY_FACTOR);
        } else {
            final T opExpZ = ee.add(field.getOne());
            return parameters.getNmF2().multiply(4.0).multiply(ee).divide(opExpZ.multiply(opExpZ)).multiply(DENSITY_FACTOR);
        }
    }

    /**
     * Lazy loading of CCIR data.
     * @param date current date components
     */
    private void loadsIfNeeded(final DateComponents date) {

        // Current month
        final int currentMonth = date.getMonth();

        // Check if date have changed or if f2 and fm3 arrays are null
        if (currentMonth != month || f2 == null || fm3 == null) {
            this.month = currentMonth;

            // Read file
            final CCIRLoader loader = new CCIRLoader();
            loader.loadCCIRCoefficients(date);

            // Update arrays
            this.f2 = loader.getF2();
            this.fm3 = loader.getFm3();
        }
    }

    /**
     * A clipped exponential function.
     * <p>
     * This function, describe in section F.2.12.2 of the reference document, is
     * recommanded for the computation of exponential values.
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
     * recommanded for the computation of exponential values.
     * </p>
     * @param <T> type of the elements
     * @param field field of the elements
     * @param power power for exponential function
     * @return clipped exponential value
     */
    private <T extends CalculusFieldElement<T>> T clipExp(final Field<T> field, final T power) {
        final T zero = field.getZero();
        if (power.getReal() > 80.0) {
            return zero.add(5.5406E34);
        } else if (power.getReal() < -80) {
            return zero.add(1.8049E-35);
        } else {
            return FastMath.exp(power);
        }
    }

    /** Get a data stream.
     * @param name file name of the resource stream
     * @return stream
     */
    private static InputStream getStream(final String name) {
        return NeQuickModel.class.getResourceAsStream(name);
    }

    /**
     * Parser for Modified Dip Latitude (MODIP) grid file.
     * <p>
     * The MODIP grid allows to estimate MODIP μ [deg] at a given point (φ,λ)
     * by interpolation of the relevant values contained in the support file.
     * </p> <p>
     * The file contains the values of MODIP (expressed in degrees) on a geocentric grid
     * from 90°S to 90°N with a 5-degree step in latitude and from 180°W to 180°E with a
     * 10-degree in longitude.
     * </p>
     */
    private static class MODIPLoader {

        /** Supported name for MODIP grid. */
        private static final String SUPPORTED_NAME = NEQUICK_BASE + "modip.txt";

        /** MODIP grid. */
        private double[][] grid;

        /**
         * Build a new instance.
         */
        MODIPLoader() {
            this.grid = null;
        }

        /** Returns the MODIP grid array.
         * @return the MODIP grid array
         */
        public double[][] getMODIPGrid() {
            return grid.clone();
        }

        /**
         * Load the data using supported names.
         */
        public void loadMODIPGrid() {
            try (InputStream in = getStream(SUPPORTED_NAME)) {
                loadData(in, SUPPORTED_NAME);
            } catch (IOException e) {
                throw new OrekitException(OrekitMessages.INTERNAL_ERROR, e);
            }

            // Throw an exception if MODIP grid was not loaded properly
            if (grid == null) {
                throw new OrekitException(OrekitMessages.MODIP_GRID_NOT_LOADED, SUPPORTED_NAME);
            }
        }

        /**
         * Load data from a stream.
         * @param input input stream
         * @param name name of the file
         * @throws IOException if data can't be read
         */
        public void loadData(final InputStream input, final String name)
            throws IOException {

            // Grid size
            final int size = 39;

            // Initialize array
            final double[][] array = new double[size][size];

            // Open stream and parse data
            int   lineNumber = 0;
            String line      = null;
            try (InputStreamReader isr = new InputStreamReader(input, StandardCharsets.UTF_8);
                 BufferedReader    br = new BufferedReader(isr)) {

                for (line = br.readLine(); line != null; line = br.readLine()) {
                    ++lineNumber;
                    line = line.trim();

                    // Read grid data
                    if (line.length() > 0) {
                        final String[] modip_line = SEPARATOR.split(line);
                        for (int column = 0; column < modip_line.length; column++) {
                            array[lineNumber - 1][column] = Double.parseDouble(modip_line[column]);
                        }
                    }

                }

            } catch (NumberFormatException nfe) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          lineNumber, name, line);
            }

            // Clone parsed grid
            grid = array.clone();

        }
    }

    /**
     * Parser for CCIR files.
     * <p>
     * Numerical grid maps which describe the regular variation of the ionosphere.
     * They are used to derive other variables such as critical frequencies and transmission factors.
     * </p> <p>
     * The coefficients correspond to low and high solar activity conditions.
     * </p> <p>
     * The CCIR file naming convention is ccirXX.asc where each XX means month + 10.
     * </p> <p>
     * Coefficients are store into tow arrays, F2 and Fm3. F2 coefficients are used for the computation
     * of the F2 layer critical frequency. Fm3 for the computation of the F2 layer maximum usable frequency factor.
     * The size of these two arrays is fixed and discussed into the section 2.5.3.2
     * of the reference document.
     * </p>
     */
    private static class CCIRLoader {

        /** Default supported files name pattern. */
        public static final String DEFAULT_SUPPORTED_NAME = "ccir**.asc";

        /** Total number of F2 coefficients contained in the file. */
        private static final int NUMBER_F2_COEFFICIENTS = 1976;

        /** Rows number for F2 and Fm3 arrays. */
        private static final int ROWS = 2;

        /** Columns number for F2 array. */
        private static final int TOTAL_COLUMNS_F2 = 76;

        /** Columns number for Fm3 array. */
        private static final int TOTAL_COLUMNS_FM3 = 49;

        /** Depth of F2 array. */
        private static final int DEPTH_F2 = 13;

        /** Depth of Fm3 array. */
        private static final int DEPTH_FM3 = 9;

        /** Regular expression for supported file name. */
        private String supportedName;

        /** F2 coefficients used for the computation of the F2 layer critical frequency. */
        private double[][][] f2Loader;

        /** Fm3 coefficients used for the computation of the F2 layer maximum usable frequency factor. */
        private double[][][] fm3Loader;

        /**
         * Build a new instance.
         */
        CCIRLoader() {
            this.supportedName = DEFAULT_SUPPORTED_NAME;
            this.f2Loader  = null;
            this.fm3Loader = null;
        }

        /**
         * Get the F2 coefficients used for the computation of the F2 layer critical frequency.
         * @return the F2 coefficients
         */
        public double[][][] getF2() {
            return f2Loader.clone();
        }

        /**
         * Get the Fm3 coefficients used for the computation of the F2 layer maximum usable frequency factor.
         * @return the F2 coefficients
         */
        public double[][][] getFm3() {
            return fm3Loader.clone();
        }

        /** Load the data for a given month.
         * @param dateComponents month given but its DateComponents
         */
        public void loadCCIRCoefficients(final DateComponents dateComponents) {

            // The files are named ccirXX.asc where XX substitute the month of the year + 10
            final int currentMonth = dateComponents.getMonth();
            this.supportedName = NEQUICK_BASE + String.format("ccir%02d.asc", currentMonth + 10);
            try (InputStream in = getStream(supportedName)) {
                loadData(in, supportedName);
            } catch (IOException e) {
                throw new OrekitException(OrekitMessages.INTERNAL_ERROR, e);
            }
            // Throw an exception if F2 or Fm3 were not loaded properly
            if (f2Loader == null || fm3Loader == null) {
                throw new OrekitException(OrekitMessages.NEQUICK_F2_FM3_NOT_LOADED, supportedName);
            }

        }

        /**
         * Load data from a stream.
         * @param input input stream
         * @param name name of the file
         * @throws IOException if data can't be read
         */
        public void loadData(final InputStream input, final String name)
            throws IOException {

            // Initialize arrays
            final double[][][] f2Temp  = new double[ROWS][TOTAL_COLUMNS_F2][DEPTH_F2];
            final double[][][] fm3Temp = new double[ROWS][TOTAL_COLUMNS_FM3][DEPTH_FM3];

            // Placeholders for parsed data
            int    lineNumber       = 0;
            int    index            = 0;
            int    currentRowF2     = 0;
            int    currentColumnF2  = 0;
            int    currentDepthF2   = 0;
            int    currentRowFm3    = 0;
            int    currentColumnFm3 = 0;
            int    currentDepthFm3  = 0;
            String line             = null;

            try (InputStreamReader isr = new InputStreamReader(input, StandardCharsets.UTF_8);
                 BufferedReader    br = new BufferedReader(isr)) {

                for (line = br.readLine(); line != null; line = br.readLine()) {
                    ++lineNumber;
                    line = line.trim();

                    // Read grid data
                    if (line.length() > 0) {
                        final String[] ccir_line = SEPARATOR.split(line);
                        for (int i = 0; i < ccir_line.length; i++) {

                            if (index < NUMBER_F2_COEFFICIENTS) {
                                // Parse F2 coefficients
                                if (currentDepthF2 >= DEPTH_F2 && currentColumnF2 < (TOTAL_COLUMNS_F2 - 1)) {
                                    currentDepthF2 = 0;
                                    currentColumnF2++;
                                } else if (currentDepthF2 >= DEPTH_F2 && currentColumnF2 >= (TOTAL_COLUMNS_F2 - 1)) {
                                    currentDepthF2 = 0;
                                    currentColumnF2 = 0;
                                    currentRowF2++;
                                }
                                f2Temp[currentRowF2][currentColumnF2][currentDepthF2++] = Double.parseDouble(ccir_line[i]);
                                index++;
                            } else {
                                // Parse Fm3 coefficients
                                if (currentDepthFm3 >= DEPTH_FM3 && currentColumnFm3 < (TOTAL_COLUMNS_FM3 - 1)) {
                                    currentDepthFm3 = 0;
                                    currentColumnFm3++;
                                } else if (currentDepthFm3 >= DEPTH_FM3 && currentColumnFm3 >= (TOTAL_COLUMNS_FM3 - 1)) {
                                    currentDepthFm3 = 0;
                                    currentColumnFm3 = 0;
                                    currentRowFm3++;
                                }
                                fm3Temp[currentRowFm3][currentColumnFm3][currentDepthFm3++] = Double.parseDouble(ccir_line[i]);
                                index++;
                            }

                        }
                    }

                }

            } catch (NumberFormatException nfe) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          lineNumber, name, line);
            }

            f2Loader  = f2Temp.clone();
            fm3Loader = fm3Temp.clone();

        }

    }

    /**
     * Container for ray-perigee parameters.
     * By convention, point 1 is at lower height.
     */
    private static class Ray {

        /** Threshold for ray-perigee parameters computation. */
        private static final double THRESHOLD = 1.0e-10;

        /** Distance of the first point from the ray perigee [m]. */
        private final double s1;

        /** Distance of the second point from the ray perigee [m]. */
        private final double s2;

        /** Ray-perigee radius [m]. */
        private final double rp;

        /** Ray-perigee latitude [rad]. */
        private final double latP;

        /** Ray-perigee longitude [rad]. */
        private final double lonP;

        /** Sine and cosine of ray-perigee latitude. */
        private final SinCos scLatP;

        /** Sine of azimuth of satellite as seen from ray-perigee. */
        private final double sinAzP;

        /** Cosine of azimuth of satellite as seen from ray-perigee. */
        private final double cosAzP;

        /**
         * Constructor.
         * @param recP receiver position
         * @param satP satellite position
         */
        Ray(final GeodeticPoint recP, final GeodeticPoint satP) {

            // Integration limits in meters (Eq. 140 and 141)
            final double r1 = RE + recP.getAltitude();
            final double r2 = RE + satP.getAltitude();

            // Useful parameters
            final double lat1     = recP.getLatitude();
            final double lat2     = satP.getLatitude();
            final double lon1     = recP.getLongitude();
            final double lon2     = satP.getLongitude();
            final SinCos scLatSat = FastMath.sinCos(lat2);
            final SinCos scLatRec = FastMath.sinCos(lat1);
            final SinCos scLon21  = FastMath.sinCos(lon2 - lon1);

            // Zenith angle computation (Eq. 153 to 155)
            final double cosD = scLatRec.sin() * scLatSat.sin() +
                                scLatRec.cos() * scLatSat.cos() * scLon21.cos();
            final double sinD = FastMath.sqrt(1.0 - cosD * cosD);
            final double z = FastMath.atan2(sinD, cosD - (r1 / r2));

            // Ray-perigee computation in meters (Eq. 156)
            this.rp = r1 * FastMath.sin(z);

            // Ray-perigee latitude and longitude
            if (FastMath.abs(FastMath.abs(lat1) - 0.5 * FastMath.PI) < THRESHOLD) {

                // Ray-perigee latitude (Eq. 157)
                if (lat1 < 0) {
                    this.latP = -z;
                } else {
                    this.latP = z;
                }

                // Ray-perigee longitude (Eq. 164)
                if (z < 0) {
                    this.lonP = lon2;
                } else {
                    this.lonP = lon2 + FastMath.PI;
                }

            } else {

                // Ray-perigee latitude (Eq. 158 to 163)
                final double deltaP   = 0.5 * FastMath.PI - z;
                final SinCos scDeltaP = FastMath.sinCos(deltaP);
                final double sinAz    = scLon21.sin() * scLatSat.cos() / sinD;
                final double cosAz    = (scLatSat.sin() - cosD * scLatRec.sin()) / (sinD * scLatRec.cos());
                final double sinLatP  = scLatRec.sin() * scDeltaP.cos() - scLatRec.cos() * scDeltaP.sin() * cosAz;
                final double cosLatP  = FastMath.sqrt(1.0 - sinLatP * sinLatP);
                this.latP = FastMath.atan2(sinLatP, cosLatP);

                // Ray-perigee longitude (Eq. 165 to 167)
                final double sinLonP = -sinAz * scDeltaP.sin() / cosLatP;
                final double cosLonP = (scDeltaP.cos() - scLatRec.sin() * sinLatP) / (scLatRec.cos() * cosLatP);
                this.lonP = FastMath.atan2(sinLonP, cosLonP) + lon1;

            }

            // Sine and cosine of ray-perigee latitude
            this.scLatP = FastMath.sinCos(latP);

            final SinCos scLon = FastMath.sinCos(lon2 - lonP);
            // Sine and cosine of azimuth of satellite as seen from ray-perigee
            final double psi   = greatCircleAngle(scLatSat, scLon);
            final SinCos scPsi = FastMath.sinCos(psi);
            if (FastMath.abs(FastMath.abs(latP) - 0.5 * FastMath.PI) < THRESHOLD) {
                // Eq. 172 and 173
                this.sinAzP = 0.0;
                if (latP < 0.0) {
                    this.cosAzP = 1;
                } else {
                    this.cosAzP = -1;
                }
            } else {
                // Eq. 174 and 175
                this.sinAzP =  scLatSat.cos() * scLon.sin()                 /  scPsi.sin();
                this.cosAzP = (scLatSat.sin() - scLatP.sin() * scPsi.cos()) / (scLatP.cos() * scPsi.sin());
            }

            // Integration en points s1 and s2 in meters (Eq. 176 and 177)
            this.s1 = FastMath.sqrt(r1 * r1 - rp * rp);
            this.s2 = FastMath.sqrt(r2 * r2 - rp * rp);
        }

        /**
         * Get the distance of the first point from the ray perigee.
         * @return s1 in meters
         */
        public double getS1() {
            return s1;
        }

        /**
         * Get the distance of the second point from the ray perigee.
         * @return s2 in meters
         */
        public double getS2() {
            return s2;
        }

        /**
         * Get the ray-perigee radius.
         * @return the ray-perigee radius in meters
         */
        public double getRadius() {
            return rp;
        }

        /**
         * Get the ray-perigee latitude.
         * @return the ray-perigee latitude in radians
         */
        public double getLatitude() {
            return latP;
        }

        /**
         * Get the ray-perigee longitude.
         * @return the ray-perigee longitude in radians
         */
        public double getLongitude() {
            return lonP;
        }

        /**
         * Get the sine of azimuth of satellite as seen from ray-perigee.
         * @return the sine of azimuth
         */
        public double getSineAz() {
            return sinAzP;
        }

        /**
         * Get the cosine of azimuth of satellite as seen from ray-perigee.
         * @return the cosine of azimuth
         */
        public double getCosineAz() {
            return cosAzP;
        }

        /**
         * Compute the great circle angle from ray-perigee to satellite.
         * <p>
         * This method used the equations 168 to 171 pf the reference document.
         * </p>
         * @param scLat sine and cosine of satellite latitude
         * @param scLon sine and cosine of satellite longitude minus receiver longitude
         * @return the great circle angle in radians
         */
        private double greatCircleAngle(final SinCos scLat, final SinCos scLon) {
            if (FastMath.abs(FastMath.abs(latP) - 0.5 * FastMath.PI) < THRESHOLD) {
                return FastMath.abs(FastMath.asin(scLat.sin()) - latP);
            } else {
                final double cosPhi = scLatP.sin() * scLat.sin() +
                                      scLatP.cos() * scLat.cos() * scLon.cos();
                final double sinPhi = FastMath.sqrt(1.0 - cosPhi * cosPhi);
                return FastMath.atan2(sinPhi, cosPhi);
            }
        }
    }

    /**
     * Container for ray-perigee parameters.
     * By convention, point 1 is at lower height.
     */
    private static class FieldRay <T extends CalculusFieldElement<T>> {

        /** Threshold for ray-perigee parameters computation. */
        private static final double THRESHOLD = 1.0e-10;

        /** Distance of the first point from the ray perigee [m]. */
        private final T s1;

        /** Distance of the second point from the ray perigee [m]. */
        private final T s2;

        /** Ray-perigee radius [m]. */
        private final T rp;

        /** Ray-perigee latitude [rad]. */
        private final T latP;

        /** Ray-perigee longitude [rad]. */
        private final T lonP;

        /** Sine and cosine of ray-perigee latitude. */
        private final FieldSinCos<T> scLatP;

        /** Sine of azimuth of satellite as seen from ray-perigee. */
        private final T sinAzP;

        /** Cosine of azimuth of satellite as seen from ray-perigee. */
        private final T cosAzP;

        /**
         * Constructor.
         * @param field field of the elements
         * @param recP receiver position
         * @param satP satellite position
         */
        FieldRay(final Field<T> field, final FieldGeodeticPoint<T> recP, final FieldGeodeticPoint<T> satP) {

            // Integration limits in meters (Eq. 140 and 141)
            final T r1 = recP.getAltitude().add(RE);
            final T r2 = satP.getAltitude().add(RE);

            // Useful parameters
            final T pi   = r1.getPi();
            final T lat1 = recP.getLatitude();
            final T lat2 = satP.getLatitude();
            final T lon1 = recP.getLongitude();
            final T lon2 = satP.getLongitude();
            final FieldSinCos<T> scLatSat = FastMath.sinCos(lat2);
            final FieldSinCos<T> scLatRec = FastMath.sinCos(lat1);

            // Zenith angle computation (Eq. 153 to 155)
            final T cosD = scLatRec.sin().multiply(scLatSat.sin()).
                            add(scLatRec.cos().multiply(scLatSat.cos()).multiply(FastMath.cos(lon2.subtract(lon1))));
            final T sinD = FastMath.sqrt(cosD.multiply(cosD).negate().add(1.0));
            final T z = FastMath.atan2(sinD, cosD.subtract(r1.divide(r2)));

            // Ray-perigee computation in meters (Eq. 156)
            this.rp = r1.multiply(FastMath.sin(z));

            // Ray-perigee latitude and longitude
            if (FastMath.abs(FastMath.abs(lat1).subtract(pi.multiply(0.5)).getReal()) < THRESHOLD) {

                // Ray-perigee latitude (Eq. 157)
                if (lat1.getReal() < 0) {
                    this.latP = z.negate();
                } else {
                    this.latP = z;
                }

                // Ray-perigee longitude (Eq. 164)
                if (z.getReal() < 0) {
                    this.lonP = lon2;
                } else {
                    this.lonP = lon2.add(pi);
                }

            } else {

                // Ray-perigee latitude (Eq. 158 to 163)
                final T deltaP = z.negate().add(pi.multiply(0.5));
                final FieldSinCos<T> scDeltaP = FastMath.sinCos(deltaP);
                final T sinAz    = FastMath.sin(lon2.subtract(lon1)).multiply(scLatSat.cos()).divide(sinD);
                final T cosAz    = scLatSat.sin().subtract(cosD.multiply(scLatRec.sin())).divide(sinD.multiply(scLatRec.cos()));
                final T sinLatP  = scLatRec.sin().multiply(scDeltaP.cos()).subtract(scLatRec.cos().multiply(scDeltaP.sin()).multiply(cosAz));
                final T cosLatP  = FastMath.sqrt(sinLatP.multiply(sinLatP).negate().add(1.0));
                this.latP = FastMath.atan2(sinLatP, cosLatP);

                // Ray-perigee longitude (Eq. 165 to 167)
                final T sinLonP = sinAz.negate().multiply(scDeltaP.sin()).divide(cosLatP);
                final T cosLonP = scDeltaP.cos().subtract(scLatRec.sin().multiply(sinLatP)).divide(scLatRec.cos().multiply(cosLatP));
                this.lonP = FastMath.atan2(sinLonP, cosLonP).add(lon1);

            }

            // Sine and cosine of ray-perigee latitude
            this.scLatP = FastMath.sinCos(latP);

            final FieldSinCos<T> scLon = FastMath.sinCos(lon2.subtract(lonP));
            // Sine and cosie of azimuth of satellite as seen from ray-perigee
            final T psi = greatCircleAngle(scLatSat, scLon);
            final FieldSinCos<T> scPsi = FastMath.sinCos(psi);
            if (FastMath.abs(FastMath.abs(latP).subtract(pi.multiply(0.5)).getReal()) < THRESHOLD) {
                // Eq. 172 and 173
                this.sinAzP = field.getZero();
                if (latP.getReal() < 0.0) {
                    this.cosAzP = field.getOne();
                } else {
                    this.cosAzP = field.getOne().negate();
                }
            } else {
                // Eq. 174 and 175
                this.sinAzP = scLatSat.cos().multiply(scLon.sin()).divide(scPsi.sin());
                this.cosAzP = scLatSat.sin().subtract(scLatP.sin().multiply(scPsi.cos())).divide(scLatP.cos().multiply(scPsi.sin()));
            }

            // Integration en points s1 and s2 in meters (Eq. 176 and 177)
            this.s1 = FastMath.sqrt(r1.multiply(r1).subtract(rp.multiply(rp)));
            this.s2 = FastMath.sqrt(r2.multiply(r2).subtract(rp.multiply(rp)));
        }

        /**
         * Get the distance of the first point from the ray perigee.
         * @return s1 in meters
         */
        public T getS1() {
            return s1;
        }

        /**
         * Get the distance of the second point from the ray perigee.
         * @return s2 in meters
         */
        public T getS2() {
            return s2;
        }

        /**
         * Get the ray-perigee radius.
         * @return the ray-perigee radius in meters
         */
        public T getRadius() {
            return rp;
        }

        /**
         * Get the ray-perigee latitude.
         * @return the ray-perigee latitude in radians
         */
        public T getLatitude() {
            return latP;
        }

        /**
         * Get the ray-perigee longitude.
         * @return the ray-perigee longitude in radians
         */
        public T getLongitude() {
            return lonP;
        }

        /**
         * Get the sine of azimuth of satellite as seen from ray-perigee.
         * @return the sine of azimuth
         */
        public T getSineAz() {
            return sinAzP;
        }

        /**
         * Get the cosine of azimuth of satellite as seen from ray-perigee.
         * @return the cosine of azimuth
         */
        public T getCosineAz() {
            return cosAzP;
        }

        /**
         * Compute the great circle angle from ray-perigee to satellite.
         * <p>
         * This method used the equations 168 to 171 pf the reference document.
         * </p>
         * @param scLat sine and cosine of satellite latitude
         * @param scLon sine and cosine of satellite longitude minus receiver longitude
         * @return the great circle angle in radians
         */
        private T greatCircleAngle(final FieldSinCos<T> scLat, final FieldSinCos<T> scLon) {
            if (FastMath.abs(FastMath.abs(latP).getReal() - 0.5 * FastMath.PI) < THRESHOLD) {
                return FastMath.abs(FastMath.asin(scLat.sin()).subtract(latP));
            } else {
                final T cosPhi = scLatP.sin().multiply(scLat.sin()).add(
                                 scLatP.cos().multiply(scLat.cos()).multiply(scLon.cos()));
                final T sinPhi = FastMath.sqrt(cosPhi.multiply(cosPhi).negate().add(1.0));
                return FastMath.atan2(sinPhi, cosPhi);
            }
        }
    }

    /** Performs the computation of the coordinates along the integration path. */
    private static class Segment {

        /** Latitudes [rad]. */
        private final double[] latitudes;

        /** Longitudes [rad]. */
        private final double[] longitudes;

        /** Heights [m]. */
        private final double[] heights;

        /** Integration step [m]. */
        private final double deltaN;

        /**
         * Constructor.
         * @param n number of points used for the integration
         * @param ray ray-perigee parameters
         */
        Segment(final int n, final Ray ray) {
            // Integration en points
            final double s1 = ray.getS1();
            final double s2 = ray.getS2();

            // Integration step (Eq. 195)
            this.deltaN = (s2 - s1) / n;

            // Segments
            final double[] s = getSegments(n, s1, s2);

            // Useful parameters
            final double rp = ray.getRadius();
            final SinCos scLatP = FastMath.sinCos(ray.getLatitude());

            // Geodetic coordinates
            final int size = s.length;
            heights    = new double[size];
            latitudes  = new double[size];
            longitudes = new double[size];
            for (int i = 0; i < size; i++) {
                // Heights (Eq. 178)
                heights[i] = FastMath.sqrt(s[i] * s[i] + rp * rp) - RE;

                // Great circle parameters (Eq. 179 to 181)
                final double tanDs = s[i] / rp;
                final double cosDs = 1.0 / FastMath.sqrt(1.0 + tanDs * tanDs);
                final double sinDs = tanDs * cosDs;

                // Latitude (Eq. 182 to 183)
                final double sinLatS = scLatP.sin() * cosDs + scLatP.cos() * sinDs * ray.getCosineAz();
                final double cosLatS = FastMath.sqrt(1.0 - sinLatS * sinLatS);
                latitudes[i] = FastMath.atan2(sinLatS, cosLatS);

                // Longitude (Eq. 184 to 187)
                final double sinLonS = sinDs * ray.getSineAz() * scLatP.cos();
                final double cosLonS = cosDs - scLatP.sin() * sinLatS;
                longitudes[i] = FastMath.atan2(sinLonS, cosLonS) + ray.getLongitude();
            }
        }

        /**
         * Computes the distance of a point from the ray-perigee.
         * @param n number of points used for the integration
         * @param s1 lower boundary
         * @param s2 upper boundary
         * @return the distance of a point from the ray-perigee in km
         */
        private double[] getSegments(final int n, final double s1, final double s2) {
            // Eq. 196
            final double g = 0.5773502691896 * deltaN;
            // Eq. 197
            final double y = s1 + (deltaN - g) * 0.5;
            final double[] segments = new double[2 * n];
            int index = 0;
            for (int i = 0; i < n; i++) {
                // Eq. 198
                segments[index] = y + i * deltaN;
                index++;
                segments[index] = y + i * deltaN + g;
                index++;
            }
            return segments;
        }

        /**
         * Get the latitudes of the coordinates along the integration path.
         * @return the latitudes in radians
         */
        public double[] getLatitudes() {
            return latitudes;
        }

        /**
         * Get the longitudes of the coordinates along the integration path.
         * @return the longitudes in radians
         */
        public double[] getLongitudes() {
            return longitudes;
        }

        /**
         * Get the heights of the coordinates along the integration path.
         * @return the heights in m
         */
        public double[] getHeights() {
            return heights;
        }

        /**
         * Get the integration step.
         * @return the integration step in meters
         */
        public double getInterval() {
            return deltaN;
        }
    }

    /** Performs the computation of the coordinates along the integration path. */
    private static class FieldSegment <T extends CalculusFieldElement<T>> {

        /** Latitudes [rad]. */
        private final T[] latitudes;

        /** Longitudes [rad]. */
        private final T[] longitudes;

        /** Heights [m]. */
        private final T[] heights;

        /** Integration step [m]. */
        private final T deltaN;

        /**
         * Constructor.
         * @param field field of the elements
         * @param n number of points used for the integration
         * @param ray ray-perigee parameters
         */
        FieldSegment(final Field<T> field, final int n, final FieldRay<T> ray) {
            // Integration en points
            final T s1 = ray.getS1();
            final T s2 = ray.getS2();

            // Integration step (Eq. 195)
            this.deltaN = s2.subtract(s1).divide(n);

            // Segments
            final T[] s = getSegments(field, n, s1, s2);

            // Useful parameters
            final T rp = ray.getRadius();
            final FieldSinCos<T> scLatP = FastMath.sinCos(ray.getLatitude());

            // Geodetic coordinates
            final int size = s.length;
            heights    = MathArrays.buildArray(field, size);
            latitudes  = MathArrays.buildArray(field, size);
            longitudes = MathArrays.buildArray(field, size);
            for (int i = 0; i < size; i++) {
                // Heights (Eq. 178)
                heights[i] = FastMath.sqrt(s[i].multiply(s[i]).add(rp.multiply(rp))).subtract(RE);

                // Great circle parameters (Eq. 179 to 181)
                final T tanDs = s[i].divide(rp);
                final T cosDs = FastMath.sqrt(tanDs.multiply(tanDs).add(1.0)).reciprocal();
                final T sinDs = tanDs.multiply(cosDs);

                // Latitude (Eq. 182 to 183)
                final T sinLatS = scLatP.sin().multiply(cosDs).add(scLatP.cos().multiply(sinDs).multiply(ray.getCosineAz()));
                final T cosLatS = FastMath.sqrt(sinLatS.multiply(sinLatS).negate().add(1.0));
                latitudes[i] = FastMath.atan2(sinLatS, cosLatS);

                // Longitude (Eq. 184 to 187)
                final T sinLonS = sinDs.multiply(ray.getSineAz()).multiply(scLatP.cos());
                final T cosLonS = cosDs.subtract(scLatP.sin().multiply(sinLatS));
                longitudes[i] = FastMath.atan2(sinLonS, cosLonS).add(ray.getLongitude());
            }
        }

        /**
         * Computes the distance of a point from the ray-perigee.
         * @param field field of the elements
         * @param n number of points used for the integration
         * @param s1 lower boundary
         * @param s2 upper boundary
         * @return the distance of a point from the ray-perigee in km
         */
        private T[] getSegments(final Field<T> field, final int n, final T s1, final T s2) {
            // Eq. 196
            final T g = deltaN.multiply(0.5773502691896);
            // Eq. 197
            final T y = s1.add(deltaN.subtract(g).multiply(0.5));
            final T[] segments = MathArrays.buildArray(field, 2 * n);
            int index = 0;
            for (int i = 0; i < n; i++) {
                // Eq. 198
                segments[index] = y.add(deltaN.multiply(i));
                index++;
                segments[index] = y.add(deltaN.multiply(i)).add(g);
                index++;
            }
            return segments;
        }

        /**
         * Get the latitudes of the coordinates along the integration path.
         * @return the latitudes in radians
         */
        public T[] getLatitudes() {
            return latitudes;
        }

        /**
         * Get the longitudes of the coordinates along the integration path.
         * @return the longitudes in radians
         */
        public T[] getLongitudes() {
            return longitudes;
        }

        /**
         * Get the heights of the coordinates along the integration path.
         * @return the heights in m
         */
        public T[] getHeights() {
            return heights;
        }

        /**
         * Get the integration step.
         * @return the integration step in meters
         */
        public T getInterval() {
            return deltaN;
        }
    }

}
