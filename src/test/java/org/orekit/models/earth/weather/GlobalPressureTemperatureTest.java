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
package org.orekit.models.earth.weather;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.GRGSFormatReader;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.Geoid;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.models.earth.troposphere.TroposphericModelUtils;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalPressureTemperatureTest {

    @BeforeEach
    void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data:potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
    }

    @Test
    void testParameterComputation() {

        // Site Toulouse, Cité de l'Espace (France): latitude:  43.59°N
        //                                           longitude: 1.49°E
        //                                           height:    140 m
        //
        // Date: 09 January 2019 at 0h UT
        //
        // Expected outputs are obtained by performing the Matlab script gpt.m provided by TU WIEN:
        // http://vmf.geo.tuwien.ac.at/codes/
        //
        // Expected parameters : temperature -> 7.3311 °C
        //                       pressure    -> 1010.2749 hPa
        //
        // The real weather conditions are obtained with www.infoclimat.fr
        //
        // Real weather conditions: temperature -> 7.3 °C
        //                          pressure    -> 1027.5 hPa

        final AbsoluteDate date = new AbsoluteDate(2019, 1, 8, 0, 0, 0.0, TimeScalesFactory.getUTC());
        final GeodeticPoint location = new GeodeticPoint(FastMath.toRadians(43.59),
                                                         FastMath.toRadians(1.49),
                                                         140.0);

        // Given by the model
        final double expectedTemperature = 7.3311;
        final double expectedPressure    = 1010.2749;

        final DataContext dataContext = DataContext.getDefault();
        final Geoid geoid = new Geoid(dataContext.getGravityFields().getNormalizedProvider(9, 9),
                                      ReferenceEllipsoid.getWgs84(FramesFactory.getITRF(IERSConventions.IERS_2010, true)));
        final GlobalPressureTemperature model = new GlobalPressureTemperature(geoid,
                                                                              dataContext.getTimeScales().getUTC());
        PressureTemperature pt = model.getWeatherParameters(location, date);

        final double computedTemperature = pt.getTemperature() - 273.15;
        final double computedPressure    = TroposphericModelUtils.HECTO_PASCAL.fromSI(pt.getPressure());

        assertEquals(expectedPressure,    computedPressure,    0.1);
        assertEquals(expectedTemperature, computedTemperature, 0.1);

        // Real weather conditions
        final double realTemperature = 7.3;
        final double realPressure    = 1027.5;

        // We test the model accuracy (10°C and 20 hPa)
        assertEquals(realTemperature, computedTemperature,    10);
        assertEquals(realPressure,    computedPressure,       20);
    }

    @Test
    void testHighAltitude() {

        // Site Pic du Midi de Bigorre (France): latitude:  42.94°N
        //                                       longitude: 0.14°E
        //                                       height:    2877 m
        //
        // Date: 09 January 2019 at 0h UT
        //
        // Expected outputs are obtained by performing the Matlab script gpt.m provided by TU WIEN:
        // http://vmf.geo.tuwien.ac.at/codes/
        //
        // Expected parameters : temperature -> -9.88 °C
        //                       pressure    -> 723.33 hPa
        //
        // The real weather conditions are obtained by the Laboratoire d'Aérologie de l'Observatoire Midi Pyrénées
        //
        // Real weather conditions: temperature -> -8.3 °C
        //                          pressure    -> 717.9 hPa

        final AbsoluteDate date = new AbsoluteDate(2019, 1, 8, 0, 0, 0.0, TimeScalesFactory.getUTC());
        final GeodeticPoint location = new GeodeticPoint(FastMath.toRadians(42.94),
                                                         FastMath.toRadians(0.14),
                                                         2877);

        // Given by the model
        final double expectedTemperature = -9.88;
        final double expectedPressure    = 723.33;

        final DataContext dataContext = DataContext.getDefault();
        final Geoid geoid = new Geoid(dataContext.getGravityFields().getNormalizedProvider(9, 9),
                                      ReferenceEllipsoid.getWgs84(FramesFactory.getITRF(IERSConventions.IERS_2010, true)));
        final GlobalPressureTemperature model = new GlobalPressureTemperature(geoid,
                                                                              dataContext.getTimeScales().getUTC());
        PressureTemperature pt = model.getWeatherParameters(location, date);


        final double computedTemperature = pt.getTemperature() - 273.15;
        final double computedPressure    = TroposphericModelUtils.HECTO_PASCAL.fromSI(pt.getPressure());

        assertEquals(expectedPressure,    computedPressure,    0.1);
        assertEquals(expectedTemperature, computedTemperature, 0.1);

        // Real weather conditions
        final double realTemperature = -8.3;
        final double realPressure    = 717.9;

        // We test the model accuracy (10°C and 20 hPa)
        assertEquals(realTemperature, computedTemperature,    10);
        assertEquals(realPressure,    computedPressure,       20);
    }

}
