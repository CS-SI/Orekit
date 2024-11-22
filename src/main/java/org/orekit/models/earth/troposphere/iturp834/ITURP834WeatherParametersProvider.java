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
import org.orekit.models.earth.troposphere.TroposphericModelUtils;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidityProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.Constants;
import org.orekit.utils.units.Unit;

/** Provider for the ITU-R P.834 weather parameters.
 * <p>
 * This class implements the weather parameters part of the model,
 * i.e. equations 27b to 27i in section 6 of the recommendation.
 * </p>
 * @see ITURP834PathDelay
 * @see ITURP834MappingFunction
 * @author Luc Maisonobe
 * @see <a href="https://www.itu.int/rec/R-REC-P.834/en">P.834 : Effects of tropospheric refraction on radiowave propagation</a>
 * @since 13.0
 */
public class ITURP834WeatherParametersProvider implements PressureTemperatureHumidityProvider {

    /** Prefix fo air total pressure at the Earth surface. */
    private static final String AIR_TOTAL_PRESSURE_PREFIX = "pres";

    /** Prefix for water vapour partial pressure at the Earth surface. */
    private static final String WATER_VAPOUR_PARTIAL_PRESSURE_PREFIX = "vapr";

    /** Prefix for mean temperature of the water vapour column above the surface. */
    private static final String MEAN_TEMPERATURE_PREFIX = "tmpm";

    /** Prefix for vapour pressure decrease factor. */
    private static final String VAPOUR_PRESSURE_DECREASE_FACTOR_PREFIX = "lamd";

    /** Prefix for lapse rate of mean temperature of water vapour from Earth surface. */
    private static final String LAPSE_RATE_MEAN_TEMPERATURE_WATER_VAPOUR_PREFIX = "alfm";

    /** Suffix for average data.*/
    private static final String AVERAGE_SUFFIX = "_gd_a1.dat";

    /** Suffix for seasonal fluctuation.*/
    private static final String SEASONAL_SUFFIX = "_gd_a2.dat";

    /** Suffix for day of minimum value. */
    private static final String DAY_OF_MINIMUM_SUFFIX = "_gd_a3.dat";

    /** Name of height reference level. */
    private static final String AVERAGE_HEIGHT_REFERENCE_LEVEL_NAME = "hreflev.dat";

    /** Molar gas constant (J/mol K). */
    private static final double R = 8.314;

    /** Dry air molar mass (kg/mol). */
    private static final double MD = Unit.GRAM.toSI(28.9644);

    /** Rd factor. **/
    private static final double RD = R / MD;

    /** Air total pressure at the Earth surface. */
    private static final SeasonalGrid AIR_TOTAL_PRESSURE;

    /** Water vapour partial pressure at the Earth surface. */
    private static final SeasonalGrid WATER_VAPOUR_PARTIAL_PRESSURE;

    /** Mean temperature of the water vapour column above the surface. */
    private static final SeasonalGrid MEAN_TEMPERATURE;

    /** Vapour pressure decrease factor. */
    private static final SeasonalGrid VAPOUR_PRESSURE_DECREASE_FACTOR;

    /** Lapse rate of mean temperature of water vapour from Earth surface. */
    private static final SeasonalGrid LAPSE_RATE_MEAN_TEMPERATURE_WATER_VAPOUR;

    /** Average height of reference level with respect to mean seal level. */
    private static final ConstantGrid AVERAGE_HEIGHT_REFERENCE_LEVEL;

    /** UTC time scale to evaluate time-dependent tables. */
    private final TimeScale utc;

    // load all model data files
    static {

        // load data files
        AIR_TOTAL_PRESSURE =
                new SeasonalGrid(TroposphericModelUtils.HECTO_PASCAL,
                                 AIR_TOTAL_PRESSURE_PREFIX + AVERAGE_SUFFIX,
                                 AIR_TOTAL_PRESSURE_PREFIX + SEASONAL_SUFFIX,
                                 AIR_TOTAL_PRESSURE_PREFIX + DAY_OF_MINIMUM_SUFFIX);
        WATER_VAPOUR_PARTIAL_PRESSURE =
                new SeasonalGrid(TroposphericModelUtils.HECTO_PASCAL,
                                 WATER_VAPOUR_PARTIAL_PRESSURE_PREFIX + AVERAGE_SUFFIX,
                                 WATER_VAPOUR_PARTIAL_PRESSURE_PREFIX + SEASONAL_SUFFIX,
                                 WATER_VAPOUR_PARTIAL_PRESSURE_PREFIX + DAY_OF_MINIMUM_SUFFIX);
        MEAN_TEMPERATURE =
                new SeasonalGrid(Unit.NONE,
                                 MEAN_TEMPERATURE_PREFIX + AVERAGE_SUFFIX,
                                 MEAN_TEMPERATURE_PREFIX + SEASONAL_SUFFIX,
                                 MEAN_TEMPERATURE_PREFIX + DAY_OF_MINIMUM_SUFFIX);
        VAPOUR_PRESSURE_DECREASE_FACTOR =
                new SeasonalGrid(Unit.NONE,
                                 VAPOUR_PRESSURE_DECREASE_FACTOR_PREFIX + AVERAGE_SUFFIX,
                                 VAPOUR_PRESSURE_DECREASE_FACTOR_PREFIX + SEASONAL_SUFFIX,
                                 VAPOUR_PRESSURE_DECREASE_FACTOR_PREFIX + DAY_OF_MINIMUM_SUFFIX);
        LAPSE_RATE_MEAN_TEMPERATURE_WATER_VAPOUR =
                new SeasonalGrid(Unit.parse("km⁻¹"),
                                 LAPSE_RATE_MEAN_TEMPERATURE_WATER_VAPOUR_PREFIX + AVERAGE_SUFFIX,
                                 LAPSE_RATE_MEAN_TEMPERATURE_WATER_VAPOUR_PREFIX + SEASONAL_SUFFIX,
                                 LAPSE_RATE_MEAN_TEMPERATURE_WATER_VAPOUR_PREFIX + DAY_OF_MINIMUM_SUFFIX);
        AVERAGE_HEIGHT_REFERENCE_LEVEL = new ConstantGrid(Unit.METRE, AVERAGE_HEIGHT_REFERENCE_LEVEL_NAME);

    }

    /** Simple constructor.
     * @param utc UTC time scale to evaluate time-dependent tables
     */
    public ITURP834WeatherParametersProvider(final TimeScale utc) {
        this.utc = utc;
    }

    /** {@inheritDoc} */
    @Override
    public PressureTemperatureHumidity getWeatherParameters(final GeodeticPoint location, final AbsoluteDate date) {

        // evaluate grid points for current date at reference height
        final double   soy        = date.getDayOfYear(utc) * Constants.JULIAN_DAY;
        final GridCell pHRef      = AIR_TOTAL_PRESSURE.getCell(location, soy);
        final GridCell eHRef      = WATER_VAPOUR_PARTIAL_PRESSURE.getCell(location, soy);
        final GridCell tmHRef     = MEAN_TEMPERATURE.getCell(location, soy);
        final GridCell lambdaHRef = VAPOUR_PRESSURE_DECREASE_FACTOR.getCell(location, soy);
        final GridCell alphaHRef  = LAPSE_RATE_MEAN_TEMPERATURE_WATER_VAPOUR.getCell(location, soy);
        final GridCell hRef       = AVERAGE_HEIGHT_REFERENCE_LEVEL.getCell(location, soy);
        final GridCell g          = Gravity.getGravityAtSurface(location);

        // mean temperature at current height, equation 27b
        final GridCell tm    = new GridCell((ct, ca, ch) -> ct - ca * (location.getAltitude() - ch),
                                            tmHRef, alphaHRef, hRef);

        // lapse rate of air temperature, equation 27f, using Rd instead of Rd' because we have SI units
        final GridCell fraction = new GridCell((cl, cg) -> (cl + 1) * cg / RD,
                                               lambdaHRef, g);
        final GridCell alpha = new GridCell((cf, ca) -> 0.5 * (cf - FastMath.sqrt(cf * (cf - 4 * ca))),
                                            fraction, alphaHRef);

        // temperature at Earth surface, equation 27e
        final GridCell t     = new GridCell((ct, ca, cf) -> ct / (1 - ca / cf),
                                            tmHRef, alpha, fraction);

        // pressure at current height, equation 27c, using Rd instead of Rd' because we have SI units
        final GridCell p = new GridCell((cp, ca, ch, ct, cg) ->
                                        cp * FastMath.pow(1 - ca * (location.getAltitude() - ch) / ct, cg / (ca * RD)),
                                        pHRef, alpha, hRef, t, g);

        // water vapour pressure et current height, equation 27d
        final GridCell e = new GridCell((ce, cp, cpr, cl) ->
                                        ce * FastMath.pow(cp / cpr, cl + 1),
                                        eHRef, p, pHRef, lambdaHRef);

        // ITU-R P.834 recommendation calls for computing ΔLᵥ (both hydrostatic and wet versions)
        // at the four corners of the cell using the weather parameters et each corner, and to perform
        // bi-linear interpolation on the cell corners afterward (equations 27h and 27i)
        // the TroposphericModel.pathDelay method, on the other hand, calls for a single weather parameter
        // valid at the desired location, hence the bi-linear interpolation is performed on each weather
        // parameter independently first, and they are combined afterward to compute ΔLᵥ
        // in order to reconcile both approaches, i.e. implement properly ITU-R P.834 that applies
        // 27h and 27i first and interpolates afterward despite using a single set of weather parameters,
        // we set up scaling factors that compensate interpolation effect, by very slightly changing the
        // pressure parameter (for hydrostatic ΔLᵥ) and the water vapour pressure parameter (for wet ΔLᵥ)
        final GridCell gm                       = Gravity.getGravityAtAltitude(location);
        final double   gmInterp                 = gm.evaluate();
        final double   lambdaInterp             = lambdaHRef.evaluate();
        final double   tmInterp                 = tm.evaluate();
        final GridCell pOverG                   = new GridCell((cp, cg) -> cp / cg, p, gm);
        final double   compensatedPressure      = pOverG.evaluate() * gmInterp;
        final GridCell eOverGLT                 = new GridCell((ce, cg, cl, ctm) -> ce / (cg * (cl + 1) * ctm),
                                                               e, gm, lambdaHRef, tm);
        final double   compensatedWaterPressure = eOverGLT.evaluate() * gmInterp * (lambdaInterp + 1) * tmInterp;
        return new PressureTemperatureHumidity(location.getAltitude(),
                                               compensatedPressure,
                                               t.evaluate(),
                                               compensatedWaterPressure,
                                               tmInterp,
                                               lambdaInterp);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldPressureTemperatureHumidity<T>
        getWeatherParameters(final FieldGeodeticPoint<T> location, final FieldAbsoluteDate<T> date) {

        // evaluate grid points for current date at reference height
        final T                soy        = date.getDayOfYear(utc).multiply(Constants.JULIAN_DAY);
        final FieldGridCell<T> pHRef      = AIR_TOTAL_PRESSURE.getCell(location, soy);
        final FieldGridCell<T> eHRef      = WATER_VAPOUR_PARTIAL_PRESSURE.getCell(location, soy);
        final FieldGridCell<T> tmHRef     = MEAN_TEMPERATURE.getCell(location, soy);
        final FieldGridCell<T> lambdaHRef = VAPOUR_PRESSURE_DECREASE_FACTOR.getCell(location, soy);
        final FieldGridCell<T> alphaHRef  = LAPSE_RATE_MEAN_TEMPERATURE_WATER_VAPOUR.getCell(location, soy);
        final FieldGridCell<T> hRef       = AVERAGE_HEIGHT_REFERENCE_LEVEL.getCell(location, soy);
        final FieldGridCell<T> g          = Gravity.getGravityAtSurface(location);

        // mean temperature at current height, equation 27b
        final FieldGridCell<T> tm    =
            new FieldGridCell<>((ct, ca, ch) -> ct.subtract(ca.multiply(location.getAltitude().subtract(ch))),
                                tmHRef, alphaHRef, hRef);

        // lapse rate of air temperature, equation 27f, using Rd instead of Rd' because we have SI units
        final FieldGridCell<T> fraction =
            new FieldGridCell<>((cl, cg) -> cl.add(1).multiply(cg).divide(RD),
                                lambdaHRef, g);
        final FieldGridCell<T> alpha =
            new FieldGridCell<>((cf, ca) -> cf.subtract(FastMath.sqrt(cf.multiply(cf.subtract(ca.multiply(4))))).multiply(0.5),
                                fraction, alphaHRef);

        // temperature at Earth surface, equation 27e
        final FieldGridCell<T> t     =
            new FieldGridCell<>((ct, ca, cf) -> ct.divide(ca.divide(cf).subtract(1).negate()),
                                tmHRef, alpha, fraction);

        // pressure at current height, equation 27c, using Rd instead of Rd' because we have SI units
        final FieldGridCell<T> p =
            new FieldGridCell<>((cp, ca, ch, ct, cg) ->
                                cp.multiply(FastMath.pow(ca.multiply(location.getAltitude().subtract(ch)).divide(ct).subtract(1).negate(),
                                                         cg.divide(ca.multiply(RD)))),
                                pHRef, alpha, hRef, t, g);

        // water vapour pressure et current height, equation 27d
        final FieldGridCell<T> e =
            new FieldGridCell<>((ce, cp, cpr, cl) ->
                                ce.multiply(FastMath.pow(cp.divide(cpr), cl.add(1))),
                                eHRef, p, pHRef, lambdaHRef);

        // ITU-R P.834 recommendation calls for computing ΔLᵥ (both hydrostatic and wet versions)
        // at the four corners of the cell using the weather parameters et each corner, and to perform
        // bi-linear interpolation on the cell corners afterward (equations 27h and 27i)
        // the TroposphericModel.pathDelay method, on the other hand, calls for a single weather parameter
        // valid at the desired location, hence the bi-linear interpolation is performed on each weather
        // parameter independently first, and they are combined afterward to compute ΔLᵥ
        // in order to reconcile both approaches, i.e. implement properly ITU-R P.834 that applies
        // 27h and 27i first and interpolates afterward despite using a single set of weather parameters,
        // we set up scaling factors that compensate interpolation effect, by very slightly changing the
        // pressure parameter (for hydrostatic ΔLᵥ) and the water vapour pressure parameter (for wet ΔLᵥ)
        final FieldGridCell<T> gm           = Gravity.getGravityAtAltitude(location);
        final T                gmInterp     = gm.evaluate();
        final T                lambdaInterp = lambdaHRef.evaluate();
        final T                tmInterp     = tm.evaluate();
        final FieldGridCell<T> pOverG       = new FieldGridCell<>(CalculusFieldElement::divide, p, gm);
        final T compensatedPressure         = pOverG.evaluate().multiply(gmInterp);
        final FieldGridCell<T> eOverGLT     = new FieldGridCell<>((ce, cg, cl, ctm) ->
                                                                  ce.divide(cg.multiply(cl.add(1)).multiply(ctm)),
                                                                  e, gm, lambdaHRef, tm);
        final T compensatedWaterPressure    = eOverGLT.evaluate().multiply(gmInterp).multiply(lambdaInterp.add(1)).multiply(tmInterp);
        return new FieldPressureTemperatureHumidity<>(location.getAltitude(),
                                                      compensatedPressure,
                                                      t.evaluate(),
                                                      compensatedWaterPressure,
                                                      tmInterp,
                                                      lambdaInterp);

    }

}
