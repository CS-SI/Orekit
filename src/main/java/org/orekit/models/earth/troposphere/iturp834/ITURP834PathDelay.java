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
import org.hipparchus.util.FastMath;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.models.earth.ITURP834AtmosphericRefraction;
import org.orekit.models.earth.troposphere.FieldTroposphericDelay;
import org.orekit.models.earth.troposphere.TroposphereMappingFunction;
import org.orekit.models.earth.troposphere.TroposphericDelay;
import org.orekit.models.earth.troposphere.TroposphericModel;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.models.earth.weather.water.WaterVaporPressureProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
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

    /** Average value of atmospheric refractivity extrapolated to sea level (from ITU-R P.453). */
    private static final double NS = 315;

    /** Molar gas constant (J/mol K). */
    private static final double R = 8.314;

    /** Dry air molar mass (kg/mol). */
    private static final double MD = 0.0289644;

    /** Hydrostatic factor (K/hPa). */
    private static final double K1 = 76.604;

    /** Wet factor (K²/hPa). */
    private static final double K2 = 373900;

    /** Gravity factor. */
    private static final double G = 9.784;

    /** Gravity latitude correction factor. */
    private static final double GL = 0.00266;

    /** Gravity altitude correction factor (rescaled for altitudes in meters). */
    private static final double GH = 2.8e-7;

    /** Computation engine for vertical excess path. */
    private final VerticalExcessPath verticalExcessPathComputer;

    /** Mapping function. */
    private final TroposphereMappingFunction mappingFunction;

    /** Converter between pressure, temperature and humidity and relative humidity. */
    private final WaterVaporPressureProvider waterVaporPressureProvider;

    /** Earth. */
    private final OneAxisEllipsoid earth;

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
     * @param earth Earth model
     * @param utc UTC time scale
     */
    public ITURP834PathDelay(final VerticalExcessPath verticalExcessPathComputer,
                             final WaterVaporPressureProvider waterVaporPressureProvider,
                             final OneAxisEllipsoid earth, final TimeScale utc) {
        this.verticalExcessPathComputer = verticalExcessPathComputer;
        this.mappingFunction            = new ITURP834MappingFunction(utc);
        this.waterVaporPressureProvider = waterVaporPressureProvider;
        this.earth                      = earth;
    }

    /** {@inheritDoc} */
    @Override
    public TroposphericDelay pathDelay(final TrackingCoordinates trackingCoordinates, final GeodeticPoint point,
                                       final PressureTemperatureHumidity weather,
                                       final double[] parameters, final AbsoluteDate date) {

        // compute vertical excess path
        final double deltaLv = verticalExcessPathComputer.verticalExcessPath(weather, waterVaporPressureProvider);

        // corrective factor for exponential atmosphere, equations 21, 22, and 23
        final double h0    = 1.0e6 * deltaLv / NS;
        final double rs    = earth.transform(new GeodeticPoint(point.getLatitude(), point.getLongitude(), 0)).getNorm();
        final double rh0   = earth.transform(new GeodeticPoint(point.getLatitude(), point.getLongitude(), h0)).getNorm();
        final double ns    = Double.NaN; // TODO
        final double nh0   = Double.NaN; // TODO
        final double ratio = ns * rs / (nh0 * rh0);
        final double k     = 1 - ratio * ratio;

        // calculate path delay
        final double gm       = G * (1 - GL * FastMath.cos(2 * point.getLatitude()) - GH * point.getAltitude());
        final double deltaLvh = 1.0e-6 * R * K1 * weather.getPressure() / (MD * gm);
        final double deltaLvw = 1.0e-6 * R * K2 * weather.getWaterVaporPressure() /
                                (MD * gm * (1 + weather.getLambda()) * weather.getTm());

        // apply mapping function
        final double[] mapping = mappingFunction.mappingFactors(trackingCoordinates, point, weather, date);
        return new TroposphericDelay(deltaLvh, deltaLvw,
                                     mapping[0] * deltaLvh, mapping[1] * deltaLvw);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTroposphericDelay<T> pathDelay(final FieldTrackingCoordinates<T> trackingCoordinates,
                                                                                   final FieldGeodeticPoint<T> point,
                                                                                   final FieldPressureTemperatureHumidity<T> weather,
                                                                                   final T[] parameters, final FieldAbsoluteDate<T> date) {
        // compute vertical excess path
        final T deltaLv = verticalExcessPathComputer.verticalExcessPath(weather, waterVaporPressureProvider);
        final T zero    = date.getField().getZero();

        // corrective factor for exponential atmosphere, equations 21, 22, and 23
        final T h0    = deltaLv.multiply(1.0e6 / NS);
        final T rs    = earth.transform(new FieldGeodeticPoint<>(point.getLatitude(), point.getLongitude(), zero)).getNorm();
        final T rh0   = earth.transform(new FieldGeodeticPoint<>(point.getLatitude(), point.getLongitude(), h0)).getNorm();
        final T ns    = zero.newInstance(Double.NaN); // TODO
        final T nh0   = zero.newInstance(Double.NaN); // TODO
        final T ratio = ns.multiply(rs).divide(nh0.multiply(rh0));
        final T k     = ratio.square().subtract(1).negate();

        // calculate path delay
        final T gm       = FastMath.cos(point.getLatitude().multiply(2)).multiply(-GL).
                           add(1).
                           subtract(point.getAltitude().multiply(GH)).
                           multiply(G);
        final T deltaLvh = weather.getPressure().multiply(1.0e-6 * R * K1).
                           divide(gm.multiply(MD));
        final T deltaLvw = weather.getWaterVaporPressure().multiply(1.0e-6 * R * K2).
                           divide(weather.getTm().multiply(weather.getLambda().add(1)).multiply(gm).multiply(MD));

        // apply mapping function
        final T[] mapping = mappingFunction.mappingFactors(trackingCoordinates, point, weather, date);
        return new FieldTroposphericDelay<>(deltaLvh, deltaLvw,
                                            mapping[0].multiply(deltaLvh), mapping[1].multiply(deltaLvw));

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

}
