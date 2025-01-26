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
package org.orekit.models.earth.troposphere;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.TrackingCoordinates;

public abstract class AbstractMappingFunctionTest<T extends TroposphereMappingFunction> {

    protected abstract T buildMappingFunction();

    // default values for doTestMappingFactors
    protected AbsoluteDate defaultDate;
    protected GeodeticPoint defaultPoint;
    protected TrackingCoordinates defaultTrackingCoordinates;

    @Test
    public abstract void testMappingFactors();

    protected void doTestMappingFactors(final AbsoluteDate date, final GeodeticPoint point,
                                        final TrackingCoordinates trackingCoordinates,
                                        final double expectedHydro, final double expectedWet) {
        final T model = buildMappingFunction();
        final double[] computedMapping = model.mappingFactors(trackingCoordinates, point,
                                                              TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                              date);
        Assertions.assertEquals(expectedHydro, computedMapping[0], 1.0e-3);
        Assertions.assertEquals(expectedWet,   computedMapping[1], 1.0e-3);
    }

    @Test
    public void testFixedHeight() {
        final AbsoluteDate date = new AbsoluteDate();
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(45.0), FastMath.toRadians(45.0), 350.0);
        T model = buildMappingFunction();
        double[] lastFactors = new double[] {
            Double.MAX_VALUE,
            Double.MAX_VALUE
        };
        // mapping functions shall decline with increasing elevation angle
        for (double elev = 10d; elev < 90d; elev += 8d) {
            final double[] factors = model.mappingFactors(new TrackingCoordinates(0.0, FastMath.toRadians(elev), 0.0),
                                                          point,
                                                          TroposphericModelUtils.STANDARD_ATMOSPHERE,
                                                          date);
            Assertions.assertTrue(Precision.compareTo(factors[0], lastFactors[0], 1.0e-6) < 0);
            Assertions.assertTrue(Precision.compareTo(factors[1], lastFactors[1], 1.0e-6) < 0);
            lastFactors[0] = factors[0];
            lastFactors[1] = factors[1];
        }
    }

    @Test
    public abstract void testDerivatives();

    protected void doTestDerivatives(final double tolValue,
                                     final double tolTimeDerivative,
                                     final double tolAzimuthDerivative,
                                     final double tolElevationDerivative,
                                     final double tolRangeDerivative) {

        T model = buildMappingFunction();

        final AbsoluteDate dateD = new AbsoluteDate(2004, 11, 16, 10, 54, 17.0, TimeScalesFactory.getUTC());
        final GeodeticPoint kuroishiD =
            new GeodeticPoint(FastMath.toRadians(40.64236), FastMath.toRadians(140.59590), 59.0);
        final PressureTemperatureHumidity weatherD = TroposphericModelUtils.STANDARD_ATMOSPHERE;
        final TrackingCoordinates trackingCoordinatesD = new TrackingCoordinates(FastMath.toRadians(210.0),
                                                                                 FastMath.toRadians(15.0),
                                                                                 1.4e6);

        // gradients with respect to date, azimuth, elevation and range
        final FieldAbsoluteDate<Gradient> dateG = new FieldAbsoluteDate<>(dateD, Gradient.variable(4, 0, 0.0));
        final FieldGeodeticPoint<Gradient> kuroishiG =
            new FieldGeodeticPoint<>(dateG.getField(), kuroishiD);
        final FieldPressureTemperatureHumidity<Gradient> weatherG =
            new FieldPressureTemperatureHumidity<>(dateG.getField(), weatherD);
        final FieldTrackingCoordinates<Gradient> trackingCoordinatesG =
            new FieldTrackingCoordinates<>(Gradient.variable(4, 1, trackingCoordinatesD.getAzimuth()),
                                           Gradient.variable(4, 2, trackingCoordinatesD.getElevation()),
                                           Gradient.variable(4, 3, trackingCoordinatesD.getRange()));

        final Gradient[] finiteDifferences = mappingFactorsGradient(model,
                                                                    trackingCoordinatesD, kuroishiD, weatherD, dateD,
                                                                    1000.0, 1.0e-2, 1.0e-2, 100.0);
        final Gradient[] direct = model.mappingFactors(trackingCoordinatesG, kuroishiG, weatherG, dateG);

        // values
        Assertions.assertEquals(finiteDifferences[0].getValue(), direct[0].getValue(), tolValue);
        Assertions.assertEquals(finiteDifferences[1].getValue(), direct[1].getValue(), tolValue);

        // derivatives with respect to date
        Assertions.assertEquals(finiteDifferences[0].getGradient()[0], direct[0].getGradient()[0], tolTimeDerivative);
        Assertions.assertEquals(finiteDifferences[1].getGradient()[0], direct[1].getGradient()[0], tolTimeDerivative);

        // derivatives with respect to azimuth
        Assertions.assertEquals(finiteDifferences[0].getGradient()[1], direct[0].getGradient()[1], tolAzimuthDerivative);
        Assertions.assertEquals(finiteDifferences[1].getGradient()[1], direct[1].getGradient()[1], tolAzimuthDerivative);

        // derivatives with respect to elevation
        Assertions.assertEquals(finiteDifferences[0].getGradient()[2], direct[0].getGradient()[2], tolElevationDerivative);
        Assertions.assertEquals(finiteDifferences[1].getGradient()[2], direct[1].getGradient()[2], tolElevationDerivative);

        // derivatives with respect to range
        Assertions.assertEquals(finiteDifferences[0].getGradient()[3], direct[0].getGradient()[3], tolRangeDerivative);
        Assertions.assertEquals(finiteDifferences[1].getGradient()[3], direct[1].getGradient()[3], tolRangeDerivative);

    }

    private Gradient[] mappingFactorsGradient(final T model,
                                              final TrackingCoordinates trackingCoordinates, final GeodeticPoint point,
                                              final PressureTemperatureHumidity weather, final AbsoluteDate date,
                                              final double dt, final double da, final double de, final double dr) {
        return new Gradient[] {
            new Gradient(model.mappingFactors(trackingCoordinates, point, weather, date)[0],
                         mappingFactorsDerivative(model, trackingCoordinates, point, weather, date, dt, 0)[0],
                         mappingFactorsDerivative(model, trackingCoordinates, point, weather, date, da, 1)[0],
                         mappingFactorsDerivative(model, trackingCoordinates, point, weather, date, de, 2)[0],
                         mappingFactorsDerivative(model, trackingCoordinates, point, weather, date, dr, 3)[0]),
            new Gradient(model.mappingFactors(trackingCoordinates, point, weather, date)[1],
                         mappingFactorsDerivative(model, trackingCoordinates, point, weather, date, dt, 0)[1],
                         mappingFactorsDerivative(model, trackingCoordinates, point, weather, date, da, 1)[1],
                         mappingFactorsDerivative(model, trackingCoordinates, point, weather, date, de, 2)[1],
                         mappingFactorsDerivative(model, trackingCoordinates, point, weather, date, dr, 3)[1])
        };
    }

    private double[] mappingFactorsDerivative(final T model,
                                              final TrackingCoordinates trackingCoordinates, final GeodeticPoint point,
                                              final PressureTemperatureHumidity weather, final AbsoluteDate date,
                                              final double delta, final int index) {

        final double dt = index == 0 ? delta : 0.0;
        final double da = index == 1 ? delta : 0.0;
        final double de = index == 2 ? delta : 0.0;
        final double dr = index == 3 ? delta : 0.0;
        final double[] mM4h = shiftedMappingFactors(model, trackingCoordinates, point, weather, date,
                                                    -4 * dt, -4 * da, -4 * de, -4 * dr);
        final double[] mM3h = shiftedMappingFactors(model, trackingCoordinates, point, weather, date,
                                                    -3 * dt, -3 * da, -3 * de, -3 * dr);
        final double[] mM2h = shiftedMappingFactors(model, trackingCoordinates, point, weather, date,
                                                    -2 * dt, -2 * da, -2 * de, -2 * dr);
        final double[] mM1h = shiftedMappingFactors(model, trackingCoordinates, point, weather, date,
                                                    -1 * dt, -1 * da, -1 * de, -1 * dr);
        final double[] mP1h = shiftedMappingFactors(model, trackingCoordinates, point, weather, date,
                                                     1 * dt,  1 * da,  1 * de,  1 * dr);
        final double[] mP2h = shiftedMappingFactors(model, trackingCoordinates, point, weather, date,
                                                     2 * dt,  2 * da,  2 * de,  2 * dr);
        final double[] mP3h = shiftedMappingFactors(model, trackingCoordinates, point, weather, date,
                                                     3 * dt,  3 * da,  3 * de,  3 * dr);
        final double[] mP4h = shiftedMappingFactors(model, trackingCoordinates, point, weather, date,
                                                     4 * dt,  4 * da,  4 * de,  4 * dr);

        return new double[] {
            differential8(mM4h[0], mM3h[0], mM2h[0], mM1h[0], mP1h[0], mP2h[0], mP3h[0], mP4h[0], delta),
            differential8(mM4h[1], mM3h[1], mM2h[1], mM1h[1], mP1h[1], mP2h[1], mP3h[1], mP4h[1], delta)
        };

    }

    private double[] shiftedMappingFactors(final T model,
                                           final TrackingCoordinates trackingCoordinates, final GeodeticPoint point,
                                           final PressureTemperatureHumidity weather, final AbsoluteDate date,
                                           final double dt, final double da, final double de, final double dr) {
        return model.mappingFactors(new TrackingCoordinates(trackingCoordinates.getAzimuth()   + da,
                                                            trackingCoordinates.getElevation() + de,
                                                            trackingCoordinates.getRange()     + dr),
                                    point, weather, date.shiftedBy(dt));
    }

    private double differential8(final double fM4h, final double fM3h, final double fM2h, final double fM1h,
                                 final double fP1h, final double fP2h, final double fP3h, final double fP4h,
                                 final double h) {

        // eight-points finite differences
        // the remaining error is -h^8/630 d^9f/dx^9 + O(h^10)
        return (-3 * (fP4h - fM4h) + 32 * (fP3h - fM3h) - 168 * (fP2h - fM2h) + 672 * (fP1h - fM1h)) / (840 * h);

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        defaultDate                = new AbsoluteDate(1994, 1, 1, TimeScalesFactory.getUTC());
        defaultPoint               = new GeodeticPoint(FastMath.toRadians(48.0), FastMath.toRadians(0.20), 68.0);
        defaultTrackingCoordinates = new TrackingCoordinates(0.0, FastMath.toRadians(5.0), 0.0);
    }

}
