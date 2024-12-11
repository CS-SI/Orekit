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

    /** Mean Earth radius in m (Ref Table 2.5.2). */
    static final double RE = 6371200.0;

    /** NeQuick resources base directory. */
    static final String NEQUICK_BASE = "/assets/org/orekit/nequick";

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

    /** F2 coefficients used by the F2 layer (flatten array for cache efficiency). */
    private double[] flattenF2;

    /** Fm3 coefficients used by the F2 layer(flatten array for cache efficiency). */
    private double[] flattenFm3;

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
    public NeQuickModel(final double[] alpha, final TimeScale utc) {
        // F2 layer values
        this.month      = 0;
        this.flattenF2  = null;
        this.flattenFm3 = null;
        // Read modip grid
        final MODIP parser = new MODIP();
        parser.loadMODIPGrid();
        this.stModip = parser.getMODIPGrid();
        // Ionisation level coefficients
        this.alpha = alpha.clone();
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
            final NeQuickParameters parameters = new NeQuickParameters(dateTime, flattenF2, flattenFm3,
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
            final FieldNeQuickParameters<T> parameters = new FieldNeQuickParameters<>(dateTime, flattenF2, flattenFm3,
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
        final T   hTemp = FastMath.max(zero.newInstance(100.0), h);
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
        if (currentMonth != month || flattenF2 == null || flattenFm3 == null) {
            this.month = currentMonth;

            // Read file
            final CCIRLoader loader = new CCIRLoader();
            loader.loadCCIRCoefficients(date);

            // Update arrays
            this.flattenF2  = flatten(loader.getF2());
            this.flattenFm3 = flatten(loader.getFm3());
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
     * @param field field of the elements
     * @param power power for exponential function
     * @return clipped exponential value
     */
    private <T extends CalculusFieldElement<T>> T clipExp(final Field<T> field, final T power) {
        final T zero = field.getZero();
        if (power.getReal() > 80.0) {
            return zero.newInstance(5.5406E34);
        } else if (power.getReal() < -80) {
            return zero.newInstance(1.8049E-35);
        } else {
            return FastMath.exp(power);
        }
    }

}
