/* Copyright 2002-2024 Thales Alenia Space
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.models.earth.troposphere.iturp834;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.interpolation.BilinearInterpolatingFunction;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.ITURP834AtmosphericRefraction;
import org.orekit.models.earth.troposphere.FieldTroposphericDelay;
import org.orekit.models.earth.troposphere.TroposphericDelay;
import org.orekit.models.earth.troposphere.TroposphericModel;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.models.earth.weather.water.WaterVaporPressureProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TrackingCoordinates;

import java.util.Collections;
import java.util.List;

/** The ITU-R P.834 tropospheric model.
 * <p>
 * This class implements the excess radio path length part of the model,
 * i.e. section 6 of the recommendation. The ray bending part of the model,
 * i.e. section 1 of the recommendation, is implemented in the
 * {@link ITURP834AtmosphericRefraction} class.
 * </p>
 * @author Luc Maisonobe
 * @see <a href="https://www.itu.int/rec/R-REC-P.834/en">P.834 : Effects of tropospheric refraction on radiowave propagation</>
 * @since 13.0
 */
public class ITURP834PathDelay implements TroposphericModel {

    /** ITU-R P.834 data resources directory. */
    private static final String ITU_R_P_834 = "/assets/org/orekit/ITU-R-P.834/";

    /** Average of air total pressure at the Earth surface. */
    private static final BilinearInterpolatingFunction AIR_TOTAL_PRESSURE_AVERAGE;

    /** Seasonal fluctuation of air total pressure at the Earth surface. */
    private static final BilinearInterpolatingFunction AIR_TOTAL_PRESSURE_SEASONAL;

    /** Day of minimum of air total pressure at the Earth surface. */
    private static final BilinearInterpolatingFunction AIR_TOTAL_PRESSURE_MINIMUM;

    /** Average of water vapour partial pressure at the Earth surface. */
    private static final BilinearInterpolatingFunction WATER_VAPOUR_PARTIAL_PRESSURE_AVERAGE;

    /** Seasonal fluctuation of water vapour partial pressure at the Earth surface. */
    private static final BilinearInterpolatingFunction WATER_VAPOUR_PARTIAL_PRESSURE_SEASONAL;

    /** Day of minimum of water vapour partial pressure at the Earth surface. */
    private static final BilinearInterpolatingFunction WATER_VAPOUR_PARTIAL_PRESSURE_MINIMUM;

    /** Average of mean temperature of the water vapour column above the surface. */
    private static final BilinearInterpolatingFunction MEAN_TEMPERATURE_AVERAGE;

    /** Seasonal fluctuation of mean temperature of the water vapour column above the surface. */
    private static final BilinearInterpolatingFunction MEAN_TEMPERATURE_SEASONAL;

    /** Day of minimum of mean temperature of the water vapour column above the surface. */
    private static final BilinearInterpolatingFunction MEAN_TEMPERATURE_MINIMUM;

    /** Average of vapour pressure decrease factor. */
    private static final BilinearInterpolatingFunction VAPOUR_PRESSURE_DECREASE_FACTOR_AVERAGE;

    /** Seasonal fluctuation of vapour pressure decrease factor. */
    private static final BilinearInterpolatingFunction VAPOUR_PRESSURE_DECREASE_FACTOR_SEASONAL;

    /** Day of minimum of vapour pressure decrease factor. */
    private static final BilinearInterpolatingFunction VAPOUR_PRESSURE_DECREASE_FACTOR_MINIMUM;

    /** Average of lapse rate of mean temperature of water vapour from Earth surface. */
    private static final BilinearInterpolatingFunction LAPSE_RATE_MEAN_TEMPERATURE_AVERAGE;

    /** Seasonal fluctuation of lapse rate of mean temperature of water vapour from Earth surface. */
    private static final BilinearInterpolatingFunction LAPSE_RATE_MEAN_TEMPERATURE_SEASONAL;

    /** Day of minimum of lapse rate of mean temperature of water vapour from Earth surface. */
    private static final BilinearInterpolatingFunction LAPSE_RATE_MEAN_TEMPERATURE_MINIMUM;

    /** Average height of reference level with respect to mean seal level. */
    private static final BilinearInterpolatingFunction AVERAGE_HEIGHT_REFERENCE_LEVEL;

    // load all model data files
    static {
        final MeteorologicalParameterParser parser = new MeteorologicalParameterParser();
        AIR_TOTAL_PRESSURE_AVERAGE =
            parser.parse(ITU_R_P_834 + MeteorologicalParameter.AIR_TOTAL_PRESSURE.getAverageValueFileName());
        AIR_TOTAL_PRESSURE_SEASONAL =
            parser.parse(ITU_R_P_834 + MeteorologicalParameter.AIR_TOTAL_PRESSURE.getSeasonalFluctuationFileName());
        AIR_TOTAL_PRESSURE_MINIMUM =
            parser.parse(ITU_R_P_834 + MeteorologicalParameter.AIR_TOTAL_PRESSURE.getDayMinimumFileName());
        WATER_VAPOUR_PARTIAL_PRESSURE_AVERAGE =
            parser.parse(ITU_R_P_834 + MeteorologicalParameter.WATER_VAPOUR_PARTIAL_PRESSURE.getAverageValueFileName());
        WATER_VAPOUR_PARTIAL_PRESSURE_SEASONAL =
            parser.parse(ITU_R_P_834 + MeteorologicalParameter.WATER_VAPOUR_PARTIAL_PRESSURE.getSeasonalFluctuationFileName());
        WATER_VAPOUR_PARTIAL_PRESSURE_MINIMUM =
            parser.parse(ITU_R_P_834 + MeteorologicalParameter.WATER_VAPOUR_PARTIAL_PRESSURE.getDayMinimumFileName());
        MEAN_TEMPERATURE_AVERAGE =
            parser.parse(ITU_R_P_834 + MeteorologicalParameter.MEAN_TEMPERATURE.getAverageValueFileName());
        MEAN_TEMPERATURE_SEASONAL =
            parser.parse(ITU_R_P_834 + MeteorologicalParameter.MEAN_TEMPERATURE.getSeasonalFluctuationFileName());
        MEAN_TEMPERATURE_MINIMUM =
            parser.parse(ITU_R_P_834 + MeteorologicalParameter.MEAN_TEMPERATURE.getDayMinimumFileName());
        VAPOUR_PRESSURE_DECREASE_FACTOR_AVERAGE =
            parser.parse(ITU_R_P_834 + MeteorologicalParameter.VAPOUR_PRESSURE_DECREASE_FACTOR.getAverageValueFileName());
        VAPOUR_PRESSURE_DECREASE_FACTOR_SEASONAL =
            parser.parse(ITU_R_P_834 + MeteorologicalParameter.VAPOUR_PRESSURE_DECREASE_FACTOR.getSeasonalFluctuationFileName());
        VAPOUR_PRESSURE_DECREASE_FACTOR_MINIMUM =
            parser.parse(ITU_R_P_834 + MeteorologicalParameter.VAPOUR_PRESSURE_DECREASE_FACTOR.getDayMinimumFileName());
        LAPSE_RATE_MEAN_TEMPERATURE_AVERAGE =
            parser.parse(ITU_R_P_834 + MeteorologicalParameter.LAPSE_RATE_MEAN_TEMPERATURE.getAverageValueFileName());
        LAPSE_RATE_MEAN_TEMPERATURE_SEASONAL =
            parser.parse(ITU_R_P_834 + MeteorologicalParameter.LAPSE_RATE_MEAN_TEMPERATURE.getSeasonalFluctuationFileName());
        LAPSE_RATE_MEAN_TEMPERATURE_MINIMUM =
            parser.parse(ITU_R_P_834 + MeteorologicalParameter.LAPSE_RATE_MEAN_TEMPERATURE.getDayMinimumFileName());
        AVERAGE_HEIGHT_REFERENCE_LEVEL =
            parser.parse(ITU_R_P_834 + "hreflev.dat");
    }

    /** Computation engine for vertical excess path. */
    private final VerticalExcessPath verticalExcessPathComputer;

    /** Converter between pressure, temperature and humidity and relative humidity. */
    private final WaterVaporPressureProvider waterVaporPressureProvider;

    /** Simple constructor.
     * <p>
     * The model uses a fixed computation engine for vertical excess path, which should
     * be chosen among the three possibilities in table 2 of ITU-R P.834
     * </p>
     * <ul>
     *     <li>{@link VerticalExcessPath#COASTAL COASTAL}: coastal areas (islands and locations less than 10km from sea shore)</li>
     *     <li>{@link VerticalExcessPath#NON_COASTAL_EQUATORIAL NON_COASTAL_EQUATORIAL}: non-coastal equatorial areas</li>
     *     <li>{@link VerticalExcessPath#NON_COASTAL_NON_EQUATORIAL NON_COASTAL_NON_EQUATORIAL}: all other areas</li>
     * </ul>
     * <p>
     * It is the responsibility of the caller to use the proper vertical excess path computation engine,
     * no attempt is made in this class to associate the {@link GeodeticPoint geodetic point} passed as
     * an argument to the {@link
     * #pathDelay(TrackingCoordinates, GeodeticPoint, PressureTemperatureHumidity, double[], AbsoluteDate)
     * pathDelay} method to a type of area. This means that if the model is intended to be used on sites
     * belonging to different types of areas, then separate instances of the model should be built with
     * different vertical excess path computation engines and associated to their respective sites.
     * </p>
     * @param verticalExcessPathComputer computation engine for vertical excess path
     * @param waterVaporPressureProvider converter between pressure, temperature and humidity and relative humidity
     */
    public ITURP834PathDelay(final VerticalExcessPath verticalExcessPathComputer,
                             final WaterVaporPressureProvider waterVaporPressureProvider) {
        this.verticalExcessPathComputer = verticalExcessPathComputer;
        this.waterVaporPressureProvider = waterVaporPressureProvider;
    }

    /** {@inheritDoc} */
    @Override
    public TroposphericDelay pathDelay(final TrackingCoordinates trackingCoordinates, final GeodeticPoint point,
                                       final PressureTemperatureHumidity weather,
                                       final double[] parameters, final AbsoluteDate date) {

        // compute vertical excess path
        final double deltaLv = verticalExcessPathComputer.verticalExcessPath(weather, waterVaporPressureProvider);

        // TODO: calculate the path delay
        final double zh     = Double.NaN;
        final double zw     = Double.NaN;
        final double sh     = Double.NaN;
        final double sw     = Double.NaN;
        return new TroposphericDelay(zh, zw, sh, sw);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTroposphericDelay<T> pathDelay(final FieldTrackingCoordinates<T> trackingCoordinates,
                                                                                   final FieldGeodeticPoint<T> point,
                                                                                   final FieldPressureTemperatureHumidity<T> weather,
                                                                                   final T[] parameters, final FieldAbsoluteDate<T> date) {
        // compute vertical excess path
        final T deltaLv = verticalExcessPathComputer.verticalExcessPath(weather, waterVaporPressureProvider);

        // TODO: calculate the path delay
        final T zh     = deltaLv.newInstance(Double.NaN);
        final T zw     = deltaLv.newInstance(Double.NaN);
        final T sh     = deltaLv.newInstance(Double.NaN);
        final T sw     = deltaLv.newInstance(Double.NaN);
        return new FieldTroposphericDelay<>(zh, zw, sh, sw);

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

}
