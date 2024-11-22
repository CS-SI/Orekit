/* Copyright 2002-2024 Thales Alenia Space
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
package org.orekit.models.earth.troposphere.iturp834;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;

public abstract class AbstractGridTest<T extends AbstractGrid> {

    @Test
    public abstract void testMetadata();

    protected void doTestMetadata(final T grid,
                                  final double expectedMinLatDeg, final double expectedMaxLatDeg, final int expectedNbLat,
                                  final double expectedMinLonDeg, final double expectedMaxLonDeg, final int expectedNbLon) {

       Assertions.assertEquals(expectedNbLat,     grid.getLatitudeAxis().size());
       Assertions.assertEquals(expectedMinLatDeg, FastMath.toDegrees(grid.getLatitudeAxis().node(0)), 1.0e-10);
       Assertions.assertEquals(expectedMaxLatDeg, FastMath.toDegrees(grid.getLatitudeAxis().node(120)), 1.0e-10);

       Assertions.assertEquals(expectedNbLon,     grid.getLongitudeAxis().size());
       Assertions.assertEquals(expectedMinLonDeg, FastMath.toDegrees(grid.getLongitudeAxis().node(0)), 1.0e-10);
       Assertions.assertEquals(expectedMaxLonDeg, FastMath.toDegrees(grid.getLongitudeAxis().node(240)), 1.0e-10);

    }

    @Test
    public abstract void testMinMax();

    protected void doTestMinMax(final T grid, final double expectedMin, final double expectedMax, double tol) {

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < grid.getLongitudeAxis().size(); ++i) {
            final double lon = grid.getLongitudeAxis().node(i);
            for (int j = 0; j < grid.getLatitudeAxis().size(); ++j) {
                final double lat = grid.getLatitudeAxis().node(j);
                final double hCell = grid.getCell(new GeodeticPoint(lat, lon, 0), 0.0).evaluate();
                min = FastMath.min(min, hCell);
                max = FastMath.max(max, hCell);
            }
        }
        Assertions.assertEquals(expectedMin, min, tol);
        Assertions.assertEquals(expectedMax, max, tol);

    }

    @Test
    public abstract void testValue();

    protected void doTestValue(final T grid, final GeodeticPoint point, final double soy,
                               final double expectedValue, final double tol) {
       Assertions.assertEquals(expectedValue, grid.getCell(point, soy).evaluate(), tol);
    }

    @Test
    public abstract void testGradient();

    protected void doTestGradient(final T grid, final GeodeticPoint point, final double soy,
                                  final double tolVal, final double tolDer) {

        final Gradient                     soyG   = Gradient.variable(4, 0, soy);
        final FieldGeodeticPoint<Gradient> pointG =
            new FieldGeodeticPoint<>(Gradient.variable(4, 1, point.getLatitude()),
                                     Gradient.variable(4, 2, point.getLongitude()),
                                     Gradient.variable(4, 3, point.getAltitude()));
        final Gradient gradient = grid.getCell(pointG, soyG).evaluate();

        Assertions.assertEquals(grid.getCell(point, soy).evaluate(), gradient.getValue(), tolVal);
        Assertions.assertEquals(cellDerivative(grid, point, soy, 60.0,   0), gradient.getPartialDerivative(0), tolDer);
        Assertions.assertEquals(cellDerivative(grid, point, soy, 0.0001, 1), gradient.getPartialDerivative(1), tolDer);
        Assertions.assertEquals(cellDerivative(grid, point, soy, 0.0001, 2), gradient.getPartialDerivative(2), tolDer);
        Assertions.assertEquals(cellDerivative(grid, point, soy, 0.0001, 3), gradient.getPartialDerivative(3), tolDer);

    }

    private double cellDerivative(final T grid,
                                  final GeodeticPoint point, final double soy,
                                  final double delta, final int index) {

        final double dt   = index == 0 ? delta : 0.0;
        final double dlat = index == 1 ? delta : 0.0;
        final double dlon = index == 2 ? delta : 0.0;
        final double dh   = index == 3 ? delta : 0.0;
        final double fM4h = shiftedCell(grid, point, soy, -4 * dt, -4 * dlat, -4 * dlon, -4 * dh);
        final double fM3h = shiftedCell(grid, point, soy, -3 * dt, -3 * dlat, -3 * dlon, -3 * dh);
        final double fM2h = shiftedCell(grid, point, soy, -2 * dt, -2 * dlat, -2 * dlon, -2 * dh);
        final double fM1h = shiftedCell(grid, point, soy, -1 * dt, -1 * dlat, -1 * dlon, -1 * dh);
        final double fP1h = shiftedCell(grid, point, soy,  1 * dt,  1 * dlat,  1 * dlon,  1 * dh);
        final double fP2h = shiftedCell(grid, point, soy,  2 * dt,  2 * dlat,  2 * dlon,  2 * dh);
        final double fP3h = shiftedCell(grid, point, soy,  3 * dt,  3 * dlat,  3 * dlon,  3 * dh);
        final double fP4h = shiftedCell(grid, point, soy,  4 * dt,  4 * dlat,  4 * dlon,  4 * dh);

        // eight-points finite differences, the remaining error is -h⁸/630 d⁹f/dx⁹ + O(h¹⁰)
        return (-3 * (fP4h - fM4h) + 32 * (fP3h - fM3h) - 168 * (fP2h - fM2h) + 672 * (fP1h - fM1h)) /
               (840 * delta);

    }

    private double shiftedCell(final T grid,
                               final GeodeticPoint point, final double soy,
                               final double dt, final double dlat, final double dlon, final double dh) {
        return grid.getCell(new GeodeticPoint(point.getLatitude()  + dlat,
                                              point.getLongitude() + dlon,
                                              point.getAltitude()  + dh),
                            soy + dt).evaluate();
    }

}
