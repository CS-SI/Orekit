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
package org.orekit.models.earth.troposphere;

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.interpolation.LinearInterpolator;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataContext;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;

/** The Niell Mapping Function  model for radio wavelengths.
 *  This model is an empirical mapping function. It only needs the
 *  values of the station latitude, height and the date for the computations.
 *  <p>
 *  With this model, the hydrostatic mapping function is time and latitude dependent
 *  whereas the wet mapping function is only latitude dependent.
 *  </p>
 *
 * @see "A. E. Niell(1996), Global mapping functions for the atmosphere delay of radio wavelengths,
 *      J. Geophys. Res., 101(B2), pp.  3227–3246, doi:  10.1029/95JB03048."
 *
 * @author Bryan Cazabonne
 *
 */
public class NiellMappingFunctionModel implements MappingFunction {

    /** Values for the ah average function. */
    private static final double[] VALUES_FOR_AH_AVERAGE = {
        1.2769934e-3, 1.2683230e-3, 1.2465397e-3, 1.2196049e-3, 1.2045996e-3
    };

    /** Values for the bh average function. */
    private static final double[] VALUES_FOR_BH_AVERAGE = {
        2.9153695e-3, 2.9152299e-3, 2.9288445e-3, 2.9022565e-3, 2.9024912e-3
    };

    /** Values for the ch average function. */
    private static final double[] VALUES_FOR_CH_AVERAGE = {
        62.610505e-3, 62.837393e-3, 63.721774e-3, 63.824265e-3, 64.258455e-3
    };

    /** Values for the ah amplitude function. */
    private static final double[] VALUES_FOR_AH_AMPLITUDE = {
        0.0, 1.2709626e-5, 2.6523662e-5, 3.4000452e-5, 4.1202191e-5
    };

    /** Values for the bh amplitude function. */
    private static final double[] VALUES_FOR_BH_AMPLITUDE = {
        0.0, 2.1414979e-5, 3.0160779e-5, 7.2562722e-5, 11.723375e-5
    };

    /** X values for the ch amplitude function. */
    private static final double[] VALUES_FOR_CH_AMPLITUDE = {
        0.0, 9.0128400e-5, 4.3497037e-5, 84.795348e-5, 170.37206e-5
    };

    /** Values for the aw function. */
    private static final double[] VALUES_FOR_AW = {
        5.8021897e-4, 5.6794847e-4, 5.8118019e-4, 5.9727542e-4, 6.1641693e-4
    };

    /** Values for the bw function. */
    private static final double[] VALUES_FOR_BW = {
        1.4275268e-3, 1.5138625e-3, 1.4572752e-3, 1.5007428e-3, 1.7599082e-3
    };

    /** Values for the cw function. */
    private static final double[] VALUES_FOR_CW = {
        4.3472961e-2, 4.6729510e-2, 4.3908931e-2, 4.4626982e-2, 5.4736038e-2
    };

    /** Values for the cw function. */
    private static final double[] LATITUDE_VALUES = {
        FastMath.toRadians(15.0), FastMath.toRadians(30.0), FastMath.toRadians(45.0), FastMath.toRadians(60.0), FastMath.toRadians(75.0),
    };

    /** Interpolation function for the ah (average) term. */
    private final UnivariateFunction ahAverageFunction;

    /** Interpolation function for the bh (average) term. */
    private final UnivariateFunction bhAverageFunction;

    /** Interpolation function for the ch (average) term. */
    private final UnivariateFunction chAverageFunction;

    /** Interpolation function for the ah (amplitude) term. */
    private final UnivariateFunction ahAmplitudeFunction;

    /** Interpolation function for the bh (amplitude) term. */
    private final UnivariateFunction bhAmplitudeFunction;

    /** Interpolation function for the ch (amplitude) term. */
    private final UnivariateFunction chAmplitudeFunction;

    /** Interpolation function for the aw term. */
    private final UnivariateFunction awFunction;

    /** Interpolation function for the bw term. */
    private final UnivariateFunction bwFunction;

    /** Interpolation function for the cw term. */
    private final UnivariateFunction cwFunction;

    /** UTC time scale. */
    private final TimeScale utc;

    /** Builds a new instance.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @see #NiellMappingFunctionModel(TimeScale)
     */
    @DefaultDataContext
    public NiellMappingFunctionModel() {
        this(DataContext.getDefault().getTimeScales().getUTC());
    }

    /** Builds a new instance.
     * @param utc UTC time scale.
     * @since 10.1
     */
    public NiellMappingFunctionModel(final TimeScale utc) {
        this.utc = utc;
        // Interpolation functions for hydrostatic coefficients
        this.ahAverageFunction    = new LinearInterpolator().interpolate(LATITUDE_VALUES, VALUES_FOR_AH_AVERAGE);
        this.bhAverageFunction    = new LinearInterpolator().interpolate(LATITUDE_VALUES, VALUES_FOR_BH_AVERAGE);
        this.chAverageFunction    = new LinearInterpolator().interpolate(LATITUDE_VALUES, VALUES_FOR_CH_AVERAGE);
        this.ahAmplitudeFunction  = new LinearInterpolator().interpolate(LATITUDE_VALUES, VALUES_FOR_AH_AMPLITUDE);
        this.bhAmplitudeFunction  = new LinearInterpolator().interpolate(LATITUDE_VALUES, VALUES_FOR_BH_AMPLITUDE);
        this.chAmplitudeFunction  = new LinearInterpolator().interpolate(LATITUDE_VALUES, VALUES_FOR_CH_AMPLITUDE);

        // Interpolation functions for wet coefficients
        this.awFunction  = new LinearInterpolator().interpolate(LATITUDE_VALUES, VALUES_FOR_AW);
        this.bwFunction  = new LinearInterpolator().interpolate(LATITUDE_VALUES, VALUES_FOR_BW);
        this.cwFunction  = new LinearInterpolator().interpolate(LATITUDE_VALUES, VALUES_FOR_CW);
    }

    /** {@inheritDoc} */
    @Override
    public double[] mappingFactors(final double elevation, final GeodeticPoint point,
                                   final AbsoluteDate date) {
        // Day of year computation
        final DateTimeComponents dtc = date.getComponents(utc);
        final int dofyear = dtc.getDate().getDayOfYear();

        // Temporal factor
        double t0 = 28;
        if (point.getLatitude() < 0) {
            // southern hemisphere: t0 = 28 + an integer half of year
            t0 += 183;
        }
        final double coef    = 2 * FastMath.PI * ((dofyear - t0) / 365.25);
        final double cosCoef = FastMath.cos(coef);

        // Compute ah, bh and ch Eq. 5
        double absLatidude = FastMath.abs(point.getLatitude());
        // there are no data in the model for latitudes lower than 15°
        absLatidude = FastMath.max(FastMath.toRadians(15.0), absLatidude);
        // there are no data in the model for latitudes greater than 75°
        absLatidude = FastMath.min(FastMath.toRadians(75.0), absLatidude);
        final double ah = ahAverageFunction.value(absLatidude) - ahAmplitudeFunction.value(absLatidude) * cosCoef;
        final double bh = bhAverageFunction.value(absLatidude) - bhAmplitudeFunction.value(absLatidude) * cosCoef;
        final double ch = chAverageFunction.value(absLatidude) - chAmplitudeFunction.value(absLatidude) * cosCoef;

        final double[] function = new double[2];

        // Hydrostatic mapping factor
        function[0] = TroposphericModelUtils.mappingFunction(ah, bh, ch, elevation);

        // Wet mapping factor
        function[1] = TroposphericModelUtils.mappingFunction(awFunction.value(absLatidude), bwFunction.value(absLatidude), cwFunction.value(absLatidude), elevation);

        // Apply height correction
        final double correction = TroposphericModelUtils.computeHeightCorrection(elevation, point.getAltitude());
        function[0] = function[0] + correction;

        return function;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T[] mappingFactors(final T elevation, final FieldGeodeticPoint<T> point,
                                                              final FieldAbsoluteDate<T> date) {
        final Field<T> field = date.getField();
        final T zero = field.getZero();

        // Day of year computation
        final DateTimeComponents dtc = date.getComponents(utc);
        final int dofyear = dtc.getDate().getDayOfYear();

        // Temporal factor
        double t0 = 28;
        if (point.getLatitude().getReal() < 0) {
            // southern hemisphere: t0 = 28 + an integer half of year
            t0 += 183;
        }
        final T coef    = zero.getPi().multiply(2.0).multiply((dofyear - t0) / 365.25);
        final T cosCoef = FastMath.cos(coef);

        // Compute ah, bh and ch Eq. 5
        double absLatidude = FastMath.abs(point.getLatitude().getReal());
        // there are no data in the model for latitudes lower than 15°
        absLatidude = FastMath.max(FastMath.toRadians(15.0), absLatidude);
        // there are no data in the model for latitudes greater than 75°
        absLatidude = FastMath.min(FastMath.toRadians(75.0), absLatidude);
        final T ah = cosCoef.multiply(ahAmplitudeFunction.value(absLatidude)).negate().add(ahAverageFunction.value(absLatidude));
        final T bh = cosCoef.multiply(bhAmplitudeFunction.value(absLatidude)).negate().add(bhAverageFunction.value(absLatidude));
        final T ch = cosCoef.multiply(chAmplitudeFunction.value(absLatidude)).negate().add(chAverageFunction.value(absLatidude));

        final T[] function = MathArrays.buildArray(field, 2);

        // Hydrostatic mapping factor
        function[0] = TroposphericModelUtils.mappingFunction(ah, bh, ch, elevation);

        // Wet mapping factor
        function[1] = TroposphericModelUtils.mappingFunction(zero.add(awFunction.value(absLatidude)), zero.add(bwFunction.value(absLatidude)),
                                                             zero.add(cwFunction.value(absLatidude)), elevation);

        // Apply height correction
        final T correction = TroposphericModelUtils.computeHeightCorrection(elevation, point.getAltitude(), field);
        function[0] = function[0].add(correction);

        return function;
    }

}
