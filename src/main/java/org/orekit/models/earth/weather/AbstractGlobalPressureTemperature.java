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
package org.orekit.models.earth.weather;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.hipparchus.CalculusFieldElement;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataSource;
import org.orekit.models.earth.troposphere.AzimuthalGradientCoefficients;
import org.orekit.models.earth.troposphere.AzimuthalGradientProvider;
import org.orekit.models.earth.troposphere.FieldAzimuthalGradientCoefficients;
import org.orekit.models.earth.troposphere.FieldViennaACoefficients;
import org.orekit.models.earth.troposphere.TroposphericModelUtils;
import org.orekit.models.earth.troposphere.ViennaACoefficients;
import org.orekit.models.earth.troposphere.ViennaAProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/** Base class for Global Pressure and Temperature 2, 2w and 3 models.
 * These models are empirical models that provide the temperature, the pressure and the water vapor pressure
 * of a site depending its latitude and  longitude. These models also {@link ViennaACoefficients provide}
 * the a<sub>h</sub> and a<sub>w</sub> coefficients for Vienna models.
 * <p>
 * The requisite coefficients for the computation of the weather parameters are provided by the
 * Department of Geodesy and Geoinformation of the Vienna University. They are based on an
 * external grid file like "gpt2_1.grd" (1° x 1°), "gpt2_5.grd" (5° x 5°), "gpt2_1w.grd" (1° x 1°),
 * "gpt2_5w.grd" (5° x 5°), "gpt3_1.grd" (1° x 1°), or "gpt3_5.grd" (5° x 5°) available at:
 * <a href="https://vmf.geo.tuwien.ac.at/codes/"> link</a>
 * </p>
 * <p>
 * A bilinear interpolation is performed in order to obtained the correct values of the weather parameters.
 * </p>
 * <p>
 * The format is always the same, with and example shown below for the pressure and the temperature.
 * The "GPT2w" model (w stands for wet) also provide humidity parameters and the "GPT3" model also
 * provides horizontal gradient, so the number of columns vary depending on the model.
 * <p>
 * Example:
 * </p>
 * <pre>
 * %  lat    lon   p:a0    A1   B1   A2   B2  T:a0    A1   B1   A2   B2
 *   87.5    2.5 101421    21  409 -217 -122 259.2 -13.2 -6.1  2.6  0.3
 *   87.5    7.5 101416    21  411 -213 -120 259.3 -13.1 -6.1  2.6  0.3
 *   87.5   12.5 101411    22  413 -209 -118 259.3 -13.1 -6.1  2.6  0.3
 *   87.5   17.5 101407    23  415 -205 -116 259.4 -13.0 -6.1  2.6  0.3
 *   ...
 * </pre>
 *
 * @see "K. Lagler, M. Schindelegger, J. Böhm, H. Krasna, T. Nilsson (2013),
 * GPT2: empirical slant delay model for radio space geodetic techniques. Geophys
 * Res Lett 40(6):1069–1073. doi:10.1002/grl.50288"
 *
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 12.1
 */
public abstract class AbstractGlobalPressureTemperature
    implements ViennaAProvider, AzimuthalGradientProvider, PressureTemperatureHumidityProvider {

    /** Loaded grid. */
    private final Grid grid;

    /**
     * Constructor with source of GPTn auxiliary data given by user.
     *
     * @param source grid data source
     * @param expected expected seasonal models types
     * @exception IOException if grid data cannot be read
     */
    protected AbstractGlobalPressureTemperature(final DataSource source, final SeasonalModelType... expected)
        throws IOException {

        // load the grid data
        try (InputStream         is     = source.getOpener().openStreamOnce();
             BufferedInputStream bis    = new BufferedInputStream(is)) {
            final GptNParser     parser = new GptNParser(expected);
            parser.loadData(bis, source.getName());
            grid = parser.getGrid();
        }

    }

    /** Get duration since reference date.
     * @param date date
     * @return duration since reference date
     * @since 13.0
     */
    protected abstract double deltaRef(AbsoluteDate date);

    /** Get duration since reference date.
     * @param <T> type of the field elements
     * @param date date
     * @return duration since reference date
     * @since 13.0
     */
    protected abstract <T extends CalculusFieldElement<T>> T deltaRef(FieldAbsoluteDate<T> date);

    /** {@inheritDoc} */
    @Override
    public ViennaACoefficients getA(final GeodeticPoint location, final AbsoluteDate date) {

        // set up interpolation parameters
        final CellInterpolator interpolator =
            grid.getInterpolator(location.getLatitude(), location.getLongitude(), location.getAltitude(),
                                 deltaRef(date));

        // ah and aw coefficients
        return new ViennaACoefficients(interpolator.interpolate(e -> e.getEvaluatedModel(SeasonalModelType.AH)) * 0.001,
                                       interpolator.interpolate(e -> e.getEvaluatedModel(SeasonalModelType.AW)) * 0.001);

    }

    /** {@inheritDoc} */
    @Override
    public PressureTemperatureHumidity getWeatherParameters(final GeodeticPoint location, final AbsoluteDate date) {

        // set up interpolation parameters
        final CellInterpolator interpolator =
            grid.getInterpolator(location.getLatitude(), location.getLongitude(), location.getAltitude(),
                                 deltaRef(date));

        // Temperature [K]
        final double temperature = interpolator.interpolate(EvaluatedGridEntry::getTemperature);

        // Pressure [hPa]
        final double pressure = interpolator.interpolate(EvaluatedGridEntry::getPressure);

        // water vapor decrease factor
        final double lambda = grid.hasModels(SeasonalModelType.LAMBDA) ?
                              interpolator.interpolate(e -> e.getEvaluatedModel(SeasonalModelType.LAMBDA)) :
                              Double.NaN;

        // Water vapor pressure [hPa]
        final double el;
        if (Double.isNaN(lambda)) {
            // GPT2
            final double qv = interpolator.interpolate(e -> e.getEvaluatedModel(SeasonalModelType.QV)) * 0.001;
            el = qv * pressure / (0.622 + 0.378 * qv);
        } else {
            // GPT2w and GPT3
            el = interpolator.interpolate(EvaluatedGridEntry::getWaterVaporPressure);
        }

        // mean temperature weighted with water vapor pressure
        final double tm = grid.hasModels(SeasonalModelType.TM) ?
                          interpolator.interpolate(e -> e.getEvaluatedModel(SeasonalModelType.TM)) :
                          Double.NaN;

        return new PressureTemperatureHumidity(location.getAltitude(),
                                               TroposphericModelUtils.HECTO_PASCAL.toSI(pressure),
                                               temperature,
                                               TroposphericModelUtils.HECTO_PASCAL.toSI(el),
                                               tm,
                                               lambda);

    }

    /** {@inheritDoc} */
    @Override
    public AzimuthalGradientCoefficients getGradientCoefficients(final GeodeticPoint location, final AbsoluteDate date) {

        if (grid.hasModels(SeasonalModelType.GN_H, SeasonalModelType.GE_H, SeasonalModelType.GN_W, SeasonalModelType.GE_W)) {
            // set up interpolation parameters
            final CellInterpolator interpolator = grid.getInterpolator(location.getLatitude(), location.getLongitude(),
                                                                       location.getAltitude(), deltaRef(date));

            return new AzimuthalGradientCoefficients(interpolator.interpolate(e -> e.getEvaluatedModel(SeasonalModelType.GN_H) * 0.00001),
                                                     interpolator.interpolate(e -> e.getEvaluatedModel(SeasonalModelType.GE_H) * 0.00001),
                                                     interpolator.interpolate(e -> e.getEvaluatedModel(SeasonalModelType.GN_W) * 0.00001),
                                                     interpolator.interpolate(e -> e.getEvaluatedModel(SeasonalModelType.GE_W) * 0.00001));
        } else {
            return null;
        }

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldViennaACoefficients<T> getA(final FieldGeodeticPoint<T> location,
                                                                                final FieldAbsoluteDate<T> date) {

        // set up interpolation parameters
        final FieldCellInterpolator<T> interpolator =
            grid.getInterpolator(location.getLatitude(), location.getLongitude(), location.getAltitude(),
                                 deltaRef(date));

        // ah and aw coefficients
        return new FieldViennaACoefficients<>(interpolator.fieldInterpolate(e -> e.getEvaluatedModel(SeasonalModelType.AH)).multiply(0.001),
                                              interpolator.fieldInterpolate(e -> e.getEvaluatedModel(SeasonalModelType.AW)).multiply(0.001));

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldPressureTemperatureHumidity<T> getWeatherParameters(final FieldGeodeticPoint<T> location,
                                                                                                        final FieldAbsoluteDate<T> date) {

        // set up interpolation parameters
        final FieldCellInterpolator<T> interpolator =
            grid.getInterpolator(location.getLatitude(), location.getLongitude(), location.getAltitude(),
                                 deltaRef(date));

        // Temperature [K]
        final T temperature = interpolator.fieldInterpolate(FieldEvaluatedGridEntry::getTemperature);

        // Pressure [hPa]
        final T pressure = interpolator.fieldInterpolate(FieldEvaluatedGridEntry::getPressure);

        // water vapor decrease factor
        final T lambda = grid.hasModels(SeasonalModelType.LAMBDA) ?
                         interpolator.fieldInterpolate(e -> e.getEvaluatedModel(SeasonalModelType.LAMBDA)) :
                         date.getField().getZero().newInstance(Double.NaN);

        // Water vapor pressure [hPa]
        final T el;
        if (lambda.isNaN()) {
            // GPT2
            final T qv = interpolator.fieldInterpolate(e -> e.getEvaluatedModel(SeasonalModelType.QV)).multiply(0.001);
            el = qv.multiply(pressure).divide(qv.multiply(0.378).add(0.622));
        } else {
            // GPT3
            el = interpolator.fieldInterpolate(FieldEvaluatedGridEntry::getWaterVaporPressure);
        }

        // mean temperature weighted with water vapor pressure
        final T tm = grid.hasModels(SeasonalModelType.TM) ?
                     interpolator.fieldInterpolate(e -> e.getEvaluatedModel(SeasonalModelType.TM)) :
                     date.getField().getZero().newInstance(Double.NaN);

        return new FieldPressureTemperatureHumidity<>(location.getAltitude(),
                                                      TroposphericModelUtils.HECTO_PASCAL.toSI(pressure),
                                                      temperature,
                                                      TroposphericModelUtils.HECTO_PASCAL.toSI(el),
                                                      tm,
                                                      lambda);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldAzimuthalGradientCoefficients<T> getGradientCoefficients(final FieldGeodeticPoint<T> location,
                                                                                                             final FieldAbsoluteDate<T> date) {

        if (grid.hasModels(SeasonalModelType.GN_H, SeasonalModelType.GE_H, SeasonalModelType.GN_W, SeasonalModelType.GE_W)) {
            // set up interpolation parameters
            final FieldCellInterpolator<T> interpolator =
                grid.getInterpolator(location.getLatitude(), location.getLongitude(), location.getAltitude(),
                                     deltaRef(date));

            return new FieldAzimuthalGradientCoefficients<>(interpolator.fieldInterpolate(e -> e.getEvaluatedModel(SeasonalModelType.GN_H)).multiply(0.00001),
                                                            interpolator.fieldInterpolate(e -> e.getEvaluatedModel(SeasonalModelType.GE_H)).multiply(0.00001),
                                                            interpolator.fieldInterpolate(e -> e.getEvaluatedModel(SeasonalModelType.GN_W)).multiply(0.00001),
                                                            interpolator.fieldInterpolate(e -> e.getEvaluatedModel(SeasonalModelType.GE_W)).multiply(0.00001));
        } else {
            return null;
        }

    }

}
