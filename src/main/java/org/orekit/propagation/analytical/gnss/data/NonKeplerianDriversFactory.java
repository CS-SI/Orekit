/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.propagation.analytical.gnss.data;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.FieldGradient;
import org.hipparchus.analysis.differentiation.FieldGradientField;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;

import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleFunction;

/** Factory for non-Keplerian drivers.
 * @since 14.0
 */
public class NonKeplerianDriversFactory {

    /** Name for time parameter. */
    public static final String TIME = "GnssTime";

    /** Name for change rate in semi-major axis parameter. */
    public static final String A_DOT = "GnssADot";

    /** Name for delta of satellite mean motion. */
    public static final String DELTA_N0 = "GnssDeltaN0";

    /** Name for change rate in Δn₀. */
    public static final String DELTA_N0_DOT = "GnssDeltaN0Dot";

    /** Name for inclination rate parameter. */
    public static final String INCLINATION_RATE = "GnssInclinationRate";

    /** Name for longitude rate parameter. */
    public static final String LONGITUDE_RATE = "GnssLongitudeRate";

    /** Name for cosine of latitude argument harmonic parameter. */
    public static final String LATITUDE_COSINE = "GnssLatitudeCosine";

    /** Name for sine of latitude argument harmonic parameter. */
    public static final String LATITUDE_SINE = "GnssLatitudeSine";

    /** Name for cosine of orbit radius harmonic parameter. */
    public static final String RADIUS_COSINE = "GnssRadiusCosine";

    /** Name for sine of orbit radius harmonic parameter. */
    public static final String RADIUS_SINE = "GnssRadiusSine";

    /** Name for cosine of inclination harmonic parameter. */
    public static final String INCLINATION_COSINE = "GnssInclinationCosine";

    /** Name for sine of inclination harmonic parameter. */
    public static final String INCLINATION_SINE = "GnssInclinationSine";

    /** Name for zero-th order clock correction parameter. */
    public static final String AF0 = "GnssClock0";

    /** Name for first order clock correction parameter. */
    public static final String AF1 = "GnssClock1";

    /** Name for second order clock correction parameter. */
    public static final String AF2 = "GnssClock2";

    /** Index of time in the list returned by {@link #getParametersDrivers()}. */
    public static final int TIME_INDEX = 0;

    /** Index of change rate in semi-major axis parameter in the list returned by {@link #getParametersDrivers()}. */
    public static final int A_DOT_INDEX = TIME_INDEX + 1;

    /** Index of delta of satellite mean motion in the list returned by {@link #getParametersDrivers()}. */
    public static final int DELTA_N0_INDEX = A_DOT_INDEX + 1;

    /** Index of change rate in Δn₀ in the list returned by {@link #getParametersDrivers()}. */
    public static final int DELTA_N0_DOT_INDEX = DELTA_N0_INDEX + 1;

    /** Index of inclination rate in the list returned by {@link #getParametersDrivers()}. */
    public static final int I_DOT_INDEX = DELTA_N0_DOT_INDEX + 1;

    /** Index of longitude rate in the list returned by {@link #getParametersDrivers()}. */
    public static final int OMEGA_DOT_INDEX = I_DOT_INDEX + 1;

    /** Index of cosine on latitude argument in the list returned by {@link #getParametersDrivers()}. */
    public static final int CUC_INDEX = OMEGA_DOT_INDEX + 1;

    /** Index of sine on latitude argument in the list returned by {@link #getParametersDrivers()}. */
    public static final int CUS_INDEX = CUC_INDEX + 1;

    /** Index of cosine on radius in the list returned by {@link #getParametersDrivers()}. */
    public static final int CRC_INDEX = CUS_INDEX + 1;

    /** Index of sine on radius in the list returned by {@link #getParametersDrivers()}. */
    public static final int CRS_INDEX = CRC_INDEX + 1;

    /** Index of cosine on inclination in the list returned by {@link #getParametersDrivers()}. */
    public static final int CIC_INDEX = CRS_INDEX + 1;

    /** Index of sine on inclination in the list returned by {@link #getParametersDrivers()}. */
    public static final int CIS_INDEX = CIC_INDEX + 1;

    /** Index of zero-th order clock correction in the list returned by {@link #getParametersDrivers()}. */
    public static final int AF0_INDEX = CIS_INDEX + 1;

    /** Index of first order clock correction in the list returned by {@link #getParametersDrivers()}. */
    public static final int AF1_INDEX = AF0_INDEX + 1;

    /** Index of second order clock correction in the list returned by {@link #getParametersDrivers()}. */
    public static final int AF2_INDEX = AF1_INDEX + 1;

    /** Size of parameters array. */
    public static final int SIZE = AF2_INDEX + 1;

    /** Reference time. */
    private final ParameterDriver timeDriver;

    /** Change rate in semi-major axis (m/s). */
    private final ParameterDriver aDotDriver;

    /** Delta of satellite mean motion. */
    private final ParameterDriver deltaN0Driver;

    /** Change rate in Δn₀. */
    private final ParameterDriver deltaN0DotDriver;

    /** Inclination rate (rad/s). */
    private final ParameterDriver iDotDriver;

    /** Rate of right ascension (rad/s). */
    private final ParameterDriver domDriver;

    /** Amplitude of the cosine harmonic correction term to the argument of latitude. */
    private final ParameterDriver cucDriver;

    /** Amplitude of the sine harmonic correction term to the argument of latitude. */
    private final ParameterDriver cusDriver;

    /** Amplitude of the cosine harmonic correction term to the orbit radius. */
    private final ParameterDriver crcDriver;

    /** Amplitude of the sine harmonic correction term to the orbit radius. */
    private final ParameterDriver crsDriver;

    /** Amplitude of the cosine harmonic correction term to the inclination. */
    private final ParameterDriver cicDriver;

    /** Amplitude of the sine harmonic correction term to the inclination. */
    private final ParameterDriver cisDriver;

    /** SV zero-th order clock correction (s). */
    private final ParameterDriver af0Driver;

    /** SV first order clock correction (s/s). */
    private final ParameterDriver af1Driver;

    /** SV second order clock correction (s/s²). */
    private final ParameterDriver af2Driver;

    /** Simple constructor.
     */
    public NonKeplerianDriversFactory() {

        // propagation drivers
        this.timeDriver       = new ParameterDriver(TIME,                0.0, FastMath.scalb(1.0, -10),
                                                    0, 7 * Constants.JULIAN_DAY);
        this.aDotDriver       = new ParameterDriver(A_DOT,               0.0, FastMath.scalb(1.0, -10),
                                                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.deltaN0Driver    = new ParameterDriver(DELTA_N0,            0.0, FastMath.scalb(1.0, -36),
                                                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.deltaN0DotDriver = new ParameterDriver(DELTA_N0_DOT,        0.0, FastMath.scalb(1.0, -46),
                                                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.iDotDriver       = new ParameterDriver(INCLINATION_RATE,    0.0, FastMath.scalb(1.0, -34),
                                                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.domDriver        = new ParameterDriver(LONGITUDE_RATE,      0.0, FastMath.scalb(1.0, -34),
                                                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.cucDriver        = new ParameterDriver(LATITUDE_COSINE,     0.0, FastMath.scalb(1.0, -24),
                                                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.cusDriver        = new ParameterDriver(LATITUDE_SINE,       0.0, FastMath.scalb(1.0, -24),
                                                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.crcDriver        = new ParameterDriver(RADIUS_COSINE,       0.0, FastMath.scalb(1.0,   0),
                                                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.crsDriver        = new ParameterDriver(RADIUS_SINE,         0.0, FastMath.scalb(1.0,   0),
                                                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.cicDriver        = new ParameterDriver(INCLINATION_COSINE,  0.0, FastMath.scalb(1.0, -24),
                                                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.cisDriver        = new ParameterDriver(INCLINATION_SINE,    0.0, FastMath.scalb(1.0, -24),
                                                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        // clock drivers
        this.af0Driver = new ParameterDriver(AF0, 0.0, FastMath.scalb(1.0, -26),
                                             Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.af1Driver = new ParameterDriver(AF1, 0.0, FastMath.scalb(1.0, -42),
                                             Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.af2Driver = new ParameterDriver(AF2, 0.0, FastMath.scalb(1.0, -58),
                                             Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

    }

    /** Reset the parameters drivers from existing elements.
     * @param elements elements to use for reset
     */
    public void reset(final GNSSOrbitalElements<?> elements) {
        reset(timeDriver,       elements.getGnssDate().getSecondsInWeek());
        reset(aDotDriver,       elements.getADot());
        reset(deltaN0Driver,    elements.getDeltaN0());
        reset(deltaN0DotDriver, elements.getDeltaN0Dot());
        reset(iDotDriver,       elements.getIDot());
        reset(domDriver,        elements.getOmegaDot());
        reset(cucDriver,        elements.getCuc());
        reset(cusDriver,        elements.getCus());
        reset(crcDriver,        elements.getCrc());
        reset(crsDriver,        elements.getCrs());
        reset(cicDriver,        elements.getCic());
        reset(cisDriver,        elements.getCis());
        reset(af0Driver,        elements.getAf0());
        reset(af1Driver,        elements.getAf1());
        reset(af2Driver,        elements.getAf2());
    }

    /** Reset the parameters drivers from existing elements.
     * @param elements elements to use for reset
     */
    public void reset(final FieldGnssOrbitalElements<?, ?, ?> elements) {
        reset(timeDriver,       elements.getGnssDate().getGnssDate().getSecondsInWeek());
        reset(aDotDriver,       elements.getADot().getReal());
        reset(deltaN0Driver,    elements.getDeltaN0().getReal());
        reset(deltaN0DotDriver, elements.getDeltaN0Dot().getReal());
        reset(iDotDriver,       elements.getIDot().getReal());
        reset(domDriver,        elements.getOmegaDot().getReal());
        reset(cucDriver,        elements.getCuc().getReal());
        reset(cusDriver,        elements.getCus().getReal());
        reset(crcDriver,        elements.getCrc().getReal());
        reset(crsDriver,        elements.getCrs().getReal());
        reset(cicDriver,        elements.getCic().getReal());
        reset(cisDriver,        elements.getCis().getReal());
        reset(af0Driver,        elements.getAf0().getReal());
        reset(af1Driver,        elements.getAf1().getReal());
        reset(af2Driver,        elements.getAf2().getReal());
    }

    /** Reset one driver.
     * @param driver driver to reset
     * @param value new value (also used as reference)
     */
    private void reset (final ParameterDriver driver, final double value) {
        driver.setValue(value);
        driver.setReferenceValue(value);
    }

    /** {@inheritDoc}
     * <p>
     * Only the 15 non-Keplerian parameters (12 evolution parameters and 3 clock parameters)
     * are listed here:
     * Time driver at index {@link #TIME_INDEX},
     * ADot driver at index {@link #A_DOT_INDEX},
     * DeltaN0 driver at index {@link #DELTA_N0_INDEX},
     * DeltaN0Dot driver at index {@link #DELTA_N0_DOT_INDEX},
     * IDot driver at index {@link #I_DOT_INDEX},
     * OmegaDot driver at index {@link #OMEGA_DOT_INDEX},
     * Cuc driver at index {@link #CUC_INDEX},
     * Cus driver at index {@link #CUS_INDEX},
     * Crc driver at index {@link #CRC_INDEX},
     * Crs driver at index {@link #CRS_INDEX},
     * Cic driver at index {@link #CIC_INDEX},
     * Cis driver at index {@link #CIS_INDEX},
     * af0 driver at index {@link #AF0_INDEX},
     * af1 driver at index {@link #AF1_INDEX},
     * and af2 driver at index {@link #AF2_INDEX}.
     * </p>
     */
    public List<ParameterDriver> getParametersDrivers() {

        // ensure the parameters are really at the advertised indices
        final ParameterDriver[] array = new ParameterDriver[SIZE];

        array[TIME_INDEX]         = timeDriver;
        array[A_DOT_INDEX]        = aDotDriver;
        array[DELTA_N0_INDEX]     = deltaN0Driver;
        array[DELTA_N0_DOT_INDEX] = deltaN0DotDriver;
        array[I_DOT_INDEX]        = iDotDriver;
        array[OMEGA_DOT_INDEX]    = domDriver;
        array[CUC_INDEX]          = cucDriver;
        array[CUS_INDEX]          = cusDriver;
        array[CRC_INDEX]          = crcDriver;
        array[CRS_INDEX]          = crsDriver;
        array[CIC_INDEX]          = cicDriver;
        array[CIS_INDEX]          = cisDriver;

        array[AF0_INDEX]         = af0Driver;
        array[AF1_INDEX]         = af1Driver;
        array[AF2_INDEX]         = af2Driver;

        return Arrays.asList(array);

    }

    /** Get driver for reference time of the GNSS orbit as a duration from week start.
     * @return driver for reference time of the GNSS orbit (s)
     */
    public ParameterDriver getTimeDriver() {
        return timeDriver;
    }

    /** Get driver for change rate in semi-major axis.
     * @return driver for the change rate in semi-major axis
     */
    public ParameterDriver getADotDriver() {
        return aDotDriver;
    }

    /** Get driver for the delta of satellite mean motion.
     * @return driver for the delta of satellite mean motion
     */
    public ParameterDriver getDeltaN0Driver() {
        return deltaN0Driver;
    }

    /** Get driver for the change rate in Δn₀.
     * @return driver for change rate in Δn₀
     */
    public ParameterDriver getDeltaN0DotDriver() {
        return deltaN0DotDriver;
    }

    /** Get driver for rate of inclination angle.
     * @return driver for rate of inclination angle (rad/s)
     */
    public ParameterDriver getIDotDriver() {
        return iDotDriver;
    }

    /** Get driver for rate of right ascension.
     * @return driver for rate of right ascension (rad/s)
     */
    public ParameterDriver getOmegaDotDriver() {
        return domDriver;
    }

    /** Get driver for amplitude of the cosine harmonic correction term to the argument of latitude.
     * @return driver for amplitude of the cosine harmonic correction term to the argument of latitude (rad)
     */
    public ParameterDriver getCucDriver() {
        return cucDriver;
    }

    /** Get driver for amplitude of the sine harmonic correction term to the argument of latitude.
     * @return driver for amplitude of the sine harmonic correction term to the argument of latitude (rad)
     */
    public ParameterDriver getCusDriver() {
        return cusDriver;
    }

    /** Get driver for amplitude of the cosine harmonic correction term to the orbit radius.
     * @return driver for amplitude of the cosine harmonic correction term to the orbit radius (m)
     */
    public ParameterDriver getCrcDriver() {
        return crcDriver;
    }

    /** Get driver for amplitude of the sine harmonic correction term to the orbit radius.
     * @return driver for amplitude of the sine harmonic correction term to the orbit radius (m)
     */
    public ParameterDriver getCrsDriver() {
        return crsDriver;
    }

    /** Get driver for amplitude of the cosine harmonic correction term to the angle of inclination.
     * @return driver for amplitude of the cosine harmonic correction term to the angle of inclination (rad)
     */
    public ParameterDriver getCicDriver() {
        return cicDriver;
    }

    /** Get driver for amplitude of the sine harmonic correction term to the angle of inclination.
     * @return driver for amplitude of the sine harmonic correction term to the angle of inclination (rad)
     */
    public ParameterDriver getCisDriver() {
        return cisDriver;
    }

    /** Get driver for SV zero-th order clock correction.
     * @return driver for SV zero-th order clock correction (s)
     */
    public ParameterDriver getAf0Driver() {
        return af0Driver;
    }

    /** Get driver for SV first order clock correction.
     * @return driver for SV first order clock correction (s/s)
     */
    public ParameterDriver getAf1Driver() {
        return af1Driver;
    }

    /** Get driver for SV second order clock correction.
     * @return driver for SV second order clock correction (s/s²)
     */
    public ParameterDriver getAf2Driver() {
        return af2Driver;
    }

    /** Get the non-Keplerian elements as a flat array.
     * @param <T> type of the field elements
     * @param field field to which elements belong
     * @param converter converter for parameters values
     */
    public <T extends CalculusFieldElement<T>> T[] toArray(final Field<T> field,
                                                           final DoubleFunction<T> converter) {
        final T[] array = MathArrays.buildArray(field, SIZE);
        array[TIME_INDEX]         = converter.apply(timeDriver.getValue());
        array[A_DOT_INDEX]        = converter.apply(aDotDriver.getValue());
        array[DELTA_N0_INDEX]     = converter.apply(deltaN0Driver.getValue());
        array[DELTA_N0_DOT_INDEX] = converter.apply(deltaN0DotDriver.getValue());
        array[I_DOT_INDEX]        = converter.apply(iDotDriver.getValue());
        array[OMEGA_DOT_INDEX]    = converter.apply(domDriver.getValue());
        array[CUC_INDEX]          = converter.apply(cucDriver.getValue());
        array[CUS_INDEX]          = converter.apply(cusDriver.getValue());
        array[CRC_INDEX]          = converter.apply(crcDriver.getValue());
        array[CRS_INDEX]          = converter.apply(crsDriver.getValue());
        array[CIC_INDEX]          = converter.apply(cicDriver.getValue());
        array[CIS_INDEX]          = converter.apply(cisDriver.getValue());
        array[AF0_INDEX]          = converter.apply(af0Driver.getValue());
        array[AF1_INDEX]          = converter.apply(af1Driver.getValue());
        array[AF2_INDEX]          = converter.apply(af2Driver.getValue());
        return array;
    }

    /** Get the non-Keplerian elements as gradient variables or constants, depending on selection status.
     * @param freeParameters total number of free parameters in the gradient
     */
    public Gradient[] toGradients(final int freeParameters) {
        final Filler filler = new Filler(freeParameters);
        filler.manage(timeDriver,       TIME_INDEX);
        filler.manage(aDotDriver,       A_DOT_INDEX);
        filler.manage(deltaN0Driver,    DELTA_N0_INDEX);
        filler.manage(deltaN0DotDriver, DELTA_N0_DOT_INDEX);
        filler.manage(iDotDriver,       I_DOT_INDEX);
        filler.manage(domDriver,        OMEGA_DOT_INDEX);
        filler.manage(cucDriver,        CUC_INDEX);
        filler.manage(cusDriver,        CUS_INDEX);
        filler.manage(crcDriver,        CRC_INDEX);
        filler.manage(crsDriver,        CRS_INDEX);
        filler.manage(cicDriver,        CIC_INDEX);
        filler.manage(cisDriver,        CIS_INDEX);
        filler.manage(af0Driver,        AF0_INDEX);
        filler.manage(af1Driver,        AF1_INDEX);
        filler.manage(af2Driver,        AF2_INDEX);
        return filler.gradients;
    }

    /** Get the non-Keplerian elements as gradient variables or constants, depending on selection status.
     * @param field field
     * @param freeParameters total number of free parameters in the gradient
     */
    public <T extends CalculusFieldElement<T>> FieldGradient<T>[] toGradients(final Field<T> field,
                                                                              final int freeParameters) {
        final FieldFiller<T> filler = new FieldFiller<>(field, freeParameters);
        filler.manage(timeDriver,       TIME_INDEX);
        filler.manage(aDotDriver,       A_DOT_INDEX);
        filler.manage(deltaN0Driver,    DELTA_N0_INDEX);
        filler.manage(deltaN0DotDriver, DELTA_N0_DOT_INDEX);
        filler.manage(iDotDriver,       I_DOT_INDEX);
        filler.manage(domDriver,        OMEGA_DOT_INDEX);
        filler.manage(cucDriver,        CUC_INDEX);
        filler.manage(cusDriver,        CUS_INDEX);
        filler.manage(crcDriver,        CRC_INDEX);
        filler.manage(crsDriver,        CRS_INDEX);
        filler.manage(cicDriver,        CIC_INDEX);
        filler.manage(cisDriver,        CIS_INDEX);
        filler.manage(af0Driver,        AF0_INDEX);
        filler.manage(af1Driver,        AF1_INDEX);
        filler.manage(af2Driver,        AF2_INDEX);
        return filler.gradients;
    }

    /** Array filler for gradients.
     * @since 14.0
     */
    private static class Filler {

        /** Total number of free parameters in the gradient. */
        private final int freeParameters;

        /** Gradient array. */
        private final Gradient[] gradients;

        /** Partial derivative index. */
        private int derivative;

        /** Simple constructor.
          * @param freeParameters total number of free parameters in the gradient
          */
        Filler(final int freeParameters) {
            this.freeParameters = freeParameters;
            this.gradients      = new Gradient[SIZE];
            this.derivative     = 6;
        }

        /** Manage one driver.
         * @param driver driver to manage
         * @param index index of the driver in the array
         */
        private void manage(final ParameterDriver driver, final int index) {
            if (driver.isSelected()) {
                // this driver should be managed as a variable
                gradients[index] = Gradient.variable(freeParameters, derivative, driver.getValue());
                ++derivative;
            } else {
                // this driver should be managed as a constant
                gradients[index] = Gradient.constant(freeParameters, driver.getValue());
            }
        }

    }

    /** Array filler for gradients.
     * @param <T> field to which elements belong
     * @since 14.0
     */
    private static class FieldFiller<T extends CalculusFieldElement<T>> {

        /** Field. */
        private final Field<T> field;

        /** Total number of free parameters in the gradient. */
        private final int freeParameters;

        /** Gradient array. */
        private final FieldGradient<T>[] gradients;

        /** Partial derivative index. */
        private int derivative;

        /** Simple constructor.
         * @param field field
         * @param freeParameters total number of free parameters in the gradient
         */
        FieldFiller(final Field<T> field, final int freeParameters) {
            this.field          = field;
            this.freeParameters = freeParameters;
            this.gradients      = MathArrays.buildArray(FieldGradientField.getField(field, freeParameters), SIZE);
            this.derivative     = 6;
        }

        /** Manage one driver.
         * @param driver driver to manage
         * @param index index of the driver in the array
         */
        private void manage(final ParameterDriver driver, final int index) {
            if (driver.isSelected()) {
                // this driver should be managed as a variable
                gradients[index] = FieldGradient.variable(freeParameters, derivative,
                                                          field.getZero().newInstance(driver.getValue()));
                ++derivative;
            } else {
                // this driver should be managed as a constant
                gradients[index] = FieldGradient.constant(freeParameters,
                                                          field.getZero().newInstance(driver.getValue()));
            }
        }

    }

}
