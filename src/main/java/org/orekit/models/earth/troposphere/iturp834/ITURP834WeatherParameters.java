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
import org.hipparchus.analysis.interpolation.GridAxis;
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
import org.orekit.utils.units.Unit;

/** The ITU-R P.834 weather parameters.
 * <p>
 * This class implements the weather parameters part of the model,
 * i.e. equations 27b to 27j in section 6 of the recommendation.
 * </p>
 * @author Luc Maisonobe
 * @see <a href="https://www.itu.int/rec/R-REC-P.834/en">P.834 : Effects of tropospheric refraction on radiowave propagation</>
 * @since 13.0
 */
public class ITURP834WeatherParameters implements PressureTemperatureHumidityProvider {

    /** ITU-R P.834 data resources directory. */
    private static final String ITU_R_P_834 = "/assets/org/orekit/ITU-R-P.834/";

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
    public static final String SEASONAL_SUFFIX = "_gd_a2.dat";

    /** Suffix for day of minimum value. */
    public static final String DAY_OF_MINIMUM_SUFFIX = "_gd_a3.dat";

    /** Name of height reference level. */
    public static final String AVERAGE_HEIGHT_REFERENCE_LEVEL_NAME = "hreflev.dat";

    /** Gravity factor for equation 27g. */
    private static final double G_27G = 9.806;

    /** Gravity latitude correction factor for equation 27g. */
    private static final double GL_27G = 0.002637;

    /** Gravity altitude correction factor for equation 27g (rescaled for altitudes in meters). */
    private static final double GH_27G = 3.1e-7;

    /** Gravity factor for equation 27j. */
    private static final double G_27J = 9.784;

    /** Gravity latitude correction factor for equation 27j. */
    private static final double GL_27J = 0.00266;

    /** Gravity altitude correction factor for equation 27j (rescaled for altitudes in meters). */
    private static final double GH_27J = 2.8e-7;

    /** Molar gas constant (J/mol K). */
    private static final double R = 8.314;

    /** Dry air molar mass (kg/mol). */
    private static final double MD = 0.0289644;

    /** R'd factor. **/
    private static final double R_PRIME_D = R / (1000 * MD);

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

    /** Gravity at Earth surface. */
    private static final ConstantGrid G;

    /** UTC time scale to evaluate time-dependent tables. */
    private final TimeScale utc;

    // load all model data files
    static {

        // load data files
        AIR_TOTAL_PRESSURE =
                new SeasonalGrid(TroposphericModelUtils.HECTO_PASCAL,
                                 ITU_R_P_834 + AIR_TOTAL_PRESSURE_PREFIX + AVERAGE_SUFFIX,
                                 ITU_R_P_834 + AIR_TOTAL_PRESSURE_PREFIX + SEASONAL_SUFFIX,
                                 ITU_R_P_834 + AIR_TOTAL_PRESSURE_PREFIX + DAY_OF_MINIMUM_SUFFIX);
        WATER_VAPOUR_PARTIAL_PRESSURE =
                new SeasonalGrid(TroposphericModelUtils.HECTO_PASCAL,
                                 ITU_R_P_834 + WATER_VAPOUR_PARTIAL_PRESSURE_PREFIX + AVERAGE_SUFFIX,
                                 ITU_R_P_834 + WATER_VAPOUR_PARTIAL_PRESSURE_PREFIX + SEASONAL_SUFFIX,
                                 ITU_R_P_834 + WATER_VAPOUR_PARTIAL_PRESSURE_PREFIX + DAY_OF_MINIMUM_SUFFIX);
        MEAN_TEMPERATURE =
                new SeasonalGrid(Unit.NONE,
                                 ITU_R_P_834 + MEAN_TEMPERATURE_PREFIX + AVERAGE_SUFFIX,
                                 ITU_R_P_834 + MEAN_TEMPERATURE_PREFIX + SEASONAL_SUFFIX,
                                 ITU_R_P_834 + MEAN_TEMPERATURE_PREFIX + DAY_OF_MINIMUM_SUFFIX);
        VAPOUR_PRESSURE_DECREASE_FACTOR =
                new SeasonalGrid(Unit.NONE,
                                 ITU_R_P_834 + VAPOUR_PRESSURE_DECREASE_FACTOR_PREFIX + AVERAGE_SUFFIX,
                                 ITU_R_P_834 + VAPOUR_PRESSURE_DECREASE_FACTOR_PREFIX + SEASONAL_SUFFIX,
                                 ITU_R_P_834 + VAPOUR_PRESSURE_DECREASE_FACTOR_PREFIX + DAY_OF_MINIMUM_SUFFIX);
        LAPSE_RATE_MEAN_TEMPERATURE_WATER_VAPOUR =
                new SeasonalGrid(Unit.parse("km⁻¹"),
                                 ITU_R_P_834 + LAPSE_RATE_MEAN_TEMPERATURE_WATER_VAPOUR_PREFIX + AVERAGE_SUFFIX,
                                 ITU_R_P_834 + LAPSE_RATE_MEAN_TEMPERATURE_WATER_VAPOUR_PREFIX + SEASONAL_SUFFIX,
                                 ITU_R_P_834 + LAPSE_RATE_MEAN_TEMPERATURE_WATER_VAPOUR_PREFIX + DAY_OF_MINIMUM_SUFFIX);
        AVERAGE_HEIGHT_REFERENCE_LEVEL =
                new ConstantGrid(Unit.METRE,
                           ITU_R_P_834 + AVERAGE_HEIGHT_REFERENCE_LEVEL_NAME);

        // precompute gravity at Earth surface throughout the grid, using equation 27g
        G   = AVERAGE_HEIGHT_REFERENCE_LEVEL.
                apply((lat, lon, h) -> G_27G * ((1 - GL_27G * FastMath.cos(2 * lat)) - GH_27G * h));

    }

    /** Simple constructor.
     * @param utc UTC time scale to evaluate time-dependent tables
     */
    public ITURP834WeatherParameters(final TimeScale utc) {
        this.utc = utc;
    }

    /** {@inheritDoc} */
    @Override
    public PressureTemperatureHumidity getWeatherParameters(final GeodeticPoint location, final AbsoluteDate date) {

        // evaluate grid points for current date at reference height
        final double   doy        = date.getDayOfYear(utc);
        final GridCell pHRef      = AIR_TOTAL_PRESSURE.getCell(location, doy);
        final GridCell eHRef      = WATER_VAPOUR_PARTIAL_PRESSURE.getCell(location, doy);
        final GridCell tmHRef     = MEAN_TEMPERATURE.getCell(location, doy);
        final GridCell lambdaHRef = VAPOUR_PRESSURE_DECREASE_FACTOR.getCell(location, doy);
        final GridCell alphaHRef  = LAPSE_RATE_MEAN_TEMPERATURE_WATER_VAPOUR.getCell(location, doy);
        final GridCell hRef       = AVERAGE_HEIGHT_REFERENCE_LEVEL.getCell(location, doy);
        final GridCell g          = G.getCell(location, doy);

        // mean temperature at current height, equation 27b
        final GridCell tm    = new GridCell((ct, ca, ch) -> ct - ca * (location.getAltitude() - ch),
                                            tmHRef, alphaHRef, hRef);

        // lapse rate of air temperature, equation 27f
        final GridCell fraction = new GridCell((cl, cg) -> (cl + 1) * cg / R_PRIME_D,
                                               lambdaHRef, g);
        final GridCell alpha = new GridCell((cf, ca) -> 0.5 * (cf - FastMath.sqrt(cf * (cf - 4 * ca))),
                                            fraction, alphaHRef);

        // temperature, equation 27e
        final GridCell t     = new GridCell((ct, ca, cf) -> ct / (1 - ca / cf),
                                            tmHRef, alpha, fraction);

        // pressure at current height, equation 27c
        final GridCell p = new GridCell((cp, ca, ch, ct, cg) ->
                                        cp * FastMath.pow(1 - ca * (location.getAltitude() - ch) / ct, cg / (ca * R_PRIME_D)),
                                        pHRef, alpha, hRef, t, g);

        // water vapour pressure et current height, equation 27d
        final GridCell e = new GridCell((ce, cp, cpr, cl) ->
                                        ce * FastMath.pow(cp / cpr, cl + 1),
                                        eHRef, p, pHRef, lambdaHRef);

        // gravity at point altitude
        final GridAxis latitudeAxis  = G.getLatitudeAxis();
        final int      southIndex    = latitudeAxis.interpolationIndex(location.getLatitude());
        final double   northLatitude = latitudeAxis.node(southIndex + 1);
        final double   southLatitude = latitudeAxis.node(southIndex);
        final GridAxis longitudeAxis = G.getLongitudeAxis();
        final int      westIndex     = longitudeAxis.interpolationIndex(location.getLongitude());
        final double   westLongitude = longitudeAxis.node(westIndex);
        final double   mga           = -GH_27J * location.getAltitude();
        final double   gNorth        = (mga + (1 - GL_27J * FastMath.cos(2 * northLatitude))) * G_27J;
        final double   gSouth        = (mga + (1 - GL_27J * FastMath.cos(2 * southLatitude))) * G_27J;
        final GridCell gm            = new GridCell(location.getLatitude()  - southLatitude,
                                                    location.getLongitude() - westLongitude,
                                                    G.getSizeLat(), G.getSizeLon(),
                                                    gNorth, gSouth, gSouth, gNorth);

        // the ITU-R P.834 recommendation calls for computing ΔLᵥ (both hydrostatic and wet versions)
        // at the four corners of the cell using the weather parameters et each corner, and to perform
        // bi-linear interpolation on the cell corners afterwards
        // the TroposphericModel.pathDelay method, on the other hand, calls for a single weather parameter
        // valid at the desired location, hence the bi-linear interpolation is performed on each weather
        // parameter independently first, and they are combined afterwards to compute ΔLᵥ
        // if we ignored these differences of algorithms, we would observe small differences between the
        // recommendation and what Orekit computes, as one implementation would do weather parameters
        // combination followed by bi-linear interpolation whereas the other would do bi-linear
        // interpolation followed by weather parameters combination
        // in order to reproduce exactly what is asked for in the recommendation, we set up
        // scaling factors that compensate this effect, by very slightly changing the pressure
        // parameter (for hydrostatic ΔLᵥ) and the water vapour pressure parameter (for wet ΔLᵥ)
        final GridCell pOverG = new GridCell((cp, cg) -> cp / cg,
                                             p, gm);
        final double pressureInterpolationCompensation =
                pOverG.evaluate() * gm.evaluate();
        final GridCell eOverGLT = new GridCell((ce, cg, cl, ctm) -> ce / (cg * (cl + 1) * ctm),
                                               e, gm, lambdaHRef, tm);
        final double waterInterpolationCompensation =
                eOverGLT.evaluate() * gm.evaluate() * (lambdaHRef.evaluate() + 1) * tm.evaluate();
        return new PressureTemperatureHumidity(location.getAltitude(),
                                               p.evaluate() * pressureInterpolationCompensation,
                                               t.evaluate(),
                                               eHRef.evaluate() * waterInterpolationCompensation,
                                               tm.evaluate(),
                                               lambdaHRef.evaluate());

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldPressureTemperatureHumidity<T>
    getWeatherParameters(final FieldGeodeticPoint<T> location, final FieldAbsoluteDate<T> date) {

        // evaluate grid points for current date at reference height
        final T                doy        = date.getDayOfYear(utc);
        final FieldGridCell<T> pHRef      = AIR_TOTAL_PRESSURE.getCell(location, doy);
        final FieldGridCell<T> eHRef      = WATER_VAPOUR_PARTIAL_PRESSURE.getCell(location, doy);
        final FieldGridCell<T> tmHRef     = MEAN_TEMPERATURE.getCell(location, doy);
        final FieldGridCell<T> lambdaHRef = VAPOUR_PRESSURE_DECREASE_FACTOR.getCell(location, doy);
        final FieldGridCell<T> alphaHRef  = LAPSE_RATE_MEAN_TEMPERATURE_WATER_VAPOUR.getCell(location, doy);
        final FieldGridCell<T> hRef       = AVERAGE_HEIGHT_REFERENCE_LEVEL.getCell(location, doy);
        final FieldGridCell<T> g          = G.getCell(location, doy);

        // mean temperature at current height, equation 27b
        final FieldGridCell<T> tm    =
            new FieldGridCell<>((ct, ca, ch) -> ct.subtract(ca.multiply(location.getAltitude().subtract(ch))),
                                tmHRef, alphaHRef, hRef);

        // lapse rate of air temperature, equation 27f
        final FieldGridCell<T> fraction =
            new FieldGridCell<>((cl, cg) -> cl.add(1).multiply(cg).divide(R_PRIME_D),
                                lambdaHRef, g);
        final FieldGridCell<T> alpha =
            new FieldGridCell<>((cf, ca) -> cf.subtract(FastMath.sqrt(cf.multiply(cf.subtract(ca.multiply(4))))).multiply(0.5),
                                fraction, alphaHRef);

        // temperature, equation 27e
        final FieldGridCell<T> t     =
            new FieldGridCell<>((ct, ca, cf) -> ct.divide(ca.divide(cf).subtract(1).negate()),
                                tmHRef, alpha, fraction);

        // pressure at current height, equation 27c
        final FieldGridCell<T> p =
            new FieldGridCell<>((cp, ca, ch, ct, cg) ->
                                cp.multiply(FastMath.pow(ca.multiply(location.getAltitude().subtract(ch)).divide(ct).subtract(1).negate(),
                                                         cg.divide(ca.multiply(R_PRIME_D)))),
                                pHRef, alpha, hRef, t, g);

        // water vapour pressure et current height, equation 27d
        final FieldGridCell<T> e =
            new FieldGridCell<>((ce, cp, cpr, cl) ->
                                ce.multiply(FastMath.pow(cp.divide(cpr), cl.add(1))),
                                eHRef, p, pHRef, lambdaHRef);

        // gravity at point altitude
        final GridAxis latitudeAxis  = G.getLatitudeAxis();
        final int      southIndex    = latitudeAxis.interpolationIndex(location.getLatitude().getReal());
        final double   northLatitude = latitudeAxis.node(southIndex + 1);
        final double   southLatitude = latitudeAxis.node(southIndex);
        final GridAxis longitudeAxis = G.getLongitudeAxis();
        final int      westIndex     = longitudeAxis.interpolationIndex(location.getLongitude().getReal());
        final double   westLongitude = longitudeAxis.node(westIndex);
        final T        mga           = location.getAltitude().multiply(GH_27J).negate();
        final T        gNorth        = mga.add(1 - GL_27J * FastMath.cos(2 * northLatitude)).multiply(G_27J);
        final T        gSouth        = mga.add(1 - GL_27J * FastMath.cos(2 * southLatitude)).multiply(G_27J);
        final FieldGridCell<T> gm    =
            new FieldGridCell<>(location.getLatitude().subtract(southLatitude),
                                location.getLongitude().subtract(westLongitude),
                                G.getSizeLat(), G.getSizeLon(),
                                gNorth, gSouth, gSouth, gNorth);

        // the ITU-R P.834 recommendation calls for computing ΔLᵥ (both hydrostatic and wet versions)
        // at the four corners of the cell using the weather parameters et each corner, and to perform
        // bi-linear interpolation on the cell corners afterwards
        // the TroposphericModel.pathDelay method, on the other hand, calls for a single weather parameter
        // valid at the desired location, hence the bi-linear interpolation is performed on each weather
        // parameter independently first, and they are combined afterwards to compute ΔLᵥ
        // if we ignored these differences of algorithms, we would observe small differences between the
        // recommendation and what Orekit computes, as one implementation would do weather parameters
        // combination followed by bi-linear interpolation whereas the other would do bi-linear
        // interpolation followed by weather parameters combination
        // in order to reproduce exactly what is asked for in the recommendation, we set up
        // scaling factors that compensate this effect, by very slightly changing the pressure
        // parameter (for hydrostatic ΔLᵥ) and the water vapour pressure parameter (for wet ΔLᵥ)
        final FieldGridCell<T> pOverG = new FieldGridCell<>(CalculusFieldElement::divide, p, gm);
        final T pressureInterpolationCompensation = pOverG.evaluate().multiply(gm.evaluate());
        final FieldGridCell<T> eOverGLT =
            new FieldGridCell<>((ce, cg, cl, ctm) -> ce.divide(cg.multiply(cl.add(1)).multiply(ctm)),
                                e, gm, lambdaHRef, tm);
        final T waterInterpolationCompensation =
                eOverGLT.evaluate().multiply(gm.evaluate()).multiply(lambdaHRef.evaluate().add(1)).multiply(tm.evaluate());
        return new FieldPressureTemperatureHumidity<>(location.getAltitude(),
                                                      p.evaluate().multiply(pressureInterpolationCompensation),
                                                      t.evaluate(),
                                                      eHRef.evaluate().multiply(waterInterpolationCompensation),
                                                      tm.evaluate(),
                                                      lambdaHRef.evaluate());

    }

}
