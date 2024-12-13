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
package org.orekit.models.earth.ionosphere;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

import java.io.StringReader;

public class ModipGridTest {

    @Test
    public void testGalileoGrid() {
        final ModipGrid modipGrid = new ModipGrid(NeQuickVersion.NEQUICK_2_GALILEO.getnbCellsLon(),
                                                  NeQuickVersion.NEQUICK_2_GALILEO.getnbCellsLat(),
                                                  NeQuickVersion.NEQUICK_2_GALILEO.getSource(),
                                                  NeQuickVersion.NEQUICK_2_GALILEO.isWrappingAlreadyIncluded());

        // check non-existent points past pole
        for (double lonDeg = -1000.0; lonDeg <= 1000.0; lonDeg += 0.125) {
            checkValue(modipGrid, -91.0, lonDeg, -90.0);
            checkValue(modipGrid,  91.0, lonDeg,  90.0);
        }

        // check points just before pole
        for (double lonDeg = -1000.0; lonDeg <= 1000.0; lonDeg += 0.125) {
            checkValue(modipGrid, -90.0 + 1.0e-10,  lonDeg, -90.0);
            checkValue(modipGrid,  90.0 - 1.0e-10, lonDeg,  90.0);
        }

        // check wrapping in longitude of the first/last three columns
        for (double latDeg = -85.0; latDeg <= 90.0; latDeg += 5.0) {
            checkEquals(modipGrid, latDeg, -190.0, 170.0);
            checkEquals(modipGrid, latDeg, -180.0, 180.0);
            checkEquals(modipGrid, latDeg, -170.0, 190.0);
        }

        // check a few points exactly on grid points
        checkValue(modipGrid, -85.0, -190.0, -77.31);
        checkValue(modipGrid, -85.0, -120.0, -76.81);
        checkValue(modipGrid, -85.0,  110.0, -77.19);
        checkValue(modipGrid,  -5.0, -110.0,   3.20);
        checkValue(modipGrid,  60.0,   20.0,  60.85);

        // check interpolated points
        checkValue(modipGrid,  55.1,   13.4,  57.954196);
        checkValue(modipGrid,  60.1,   13.4,  60.849386);
        checkValue(modipGrid,  65.1,   13.4,  63.771290);
        checkValue(modipGrid,  55.1,   23.4,  58.088051);
        checkValue(modipGrid,  60.1,   23.4,  60.946660);
        checkValue(modipGrid,  65.1,   23.4,  63.837296);
        checkValue(modipGrid,  55.1,   33.4,  58.259195);
        checkValue(modipGrid,  60.1,   33.4,  61.087074);
        checkValue(modipGrid,  65.1,   33.4,  63.954376);

    }

    @Test
    public void testGalileoGridField() {
        final ModipGrid modipGrid = new ModipGrid(NeQuickVersion.NEQUICK_2_GALILEO.getnbCellsLon(),
                                                  NeQuickVersion.NEQUICK_2_GALILEO.getnbCellsLat(),
                                                  NeQuickVersion.NEQUICK_2_GALILEO.getSource(),
                                                  NeQuickVersion.NEQUICK_2_GALILEO.isWrappingAlreadyIncluded());

        final Field<Binary64> field = Binary64Field.getInstance();

        // check non-existent points past pole
        for (double lonDeg = -1000.0; lonDeg <= 1000.0; lonDeg += 0.125) {
            checkValue(field, modipGrid, -91.0, lonDeg, -90.0);
            checkValue(field, modipGrid,  91.0, lonDeg,  90.0);
        }

        // check points just before pole
        for (double lonDeg = -1000.0; lonDeg <= 1000.0; lonDeg += 0.125) {
            checkValue(field, modipGrid, -90.0 + 1.0e-10,  lonDeg, -90.0);
            checkValue(field, modipGrid,  90.0 - 1.0e-10, lonDeg,  90.0);
        }

        // check wrapping in longitude of the first/last three columns
        for (double latDeg = -85.0; latDeg <= 90.0; latDeg += 5.0) {
            checkEquals(field, modipGrid, latDeg, -190.0, 170.0);
            checkEquals(field, modipGrid, latDeg, -180.0, 180.0);
            checkEquals(field, modipGrid, latDeg, -170.0, 190.0);
        }

        // check a few points exactly on grid points
        checkValue(field, modipGrid, -85.0, -190.0, -77.31);
        checkValue(field, modipGrid, -85.0, -120.0, -76.81);
        checkValue(field, modipGrid, -85.0,  110.0, -77.19);
        checkValue(field, modipGrid,  -5.0, -110.0,   3.20);
        checkValue(field, modipGrid,  60.0,   20.0,  60.85);

        // check interpolated points
        checkValue(field, modipGrid,  55.1,   13.4,  57.954196);
        checkValue(field, modipGrid,  60.1,   13.4,  60.849386);
        checkValue(field, modipGrid,  65.1,   13.4,  63.771290);
        checkValue(field, modipGrid,  55.1,   23.4,  58.088051);
        checkValue(field, modipGrid,  60.1,   23.4,  60.946660);
        checkValue(field, modipGrid,  65.1,   23.4,  63.837296);
        checkValue(field, modipGrid,  55.1,   33.4,  58.259195);
        checkValue(field, modipGrid,  60.1,   33.4,  61.087074);
        checkValue(field, modipGrid,  65.1,   33.4,  63.954376);

    }

    @Test
    public void testItuGrid() {
        final ModipGrid modipGrid = new ModipGrid(NeQuickVersion.NEQUICK_2_ITU.getnbCellsLon(),
                                                  NeQuickVersion.NEQUICK_2_ITU.getnbCellsLat(),
                                                  NeQuickVersion.NEQUICK_2_ITU.getSource(),
                                                  NeQuickVersion.NEQUICK_2_ITU.isWrappingAlreadyIncluded());

        // check non-existent points past pole
        for (double lonDeg = -1000.0; lonDeg <= 1000.0; lonDeg += 0.125) {
            checkValue(modipGrid, -91.0, lonDeg, -90.0);
            checkValue(modipGrid,  91.0, lonDeg,  90.0);
        }

        // check points just before pole
        for (double lonDeg = -1000.0; lonDeg <= 1000.0; lonDeg += 0.125) {
            checkValue(modipGrid, -90.0 + 1.0e-10,  lonDeg, -89.99);
            checkValue(modipGrid,  90.0 - 1.0e-10, lonDeg,  89.99);
        }

        // check wrapping in longitude of the first/last three columns
        for (double latDeg = -85.0; latDeg <= 90.0; latDeg += 5.0) {
            checkEquals(modipGrid, latDeg, -190.0, 170.0);
            checkEquals(modipGrid, latDeg, -180.0, 180.0);
            checkEquals(modipGrid, latDeg, -170.0, 190.0);
        }

        // check a few points exactly on grid points
        checkValue(modipGrid, -85.0, -190.0, -77.91);
        checkValue(modipGrid, -85.0, -120.0, -77.41);
        checkValue(modipGrid, -85.0,  110.0, -77.75);
        checkValue(modipGrid,  -5.0, -110.0,   1.97);
        checkValue(modipGrid,  60.0,   20.0,  60.87);

        // check interpolated points
        checkValue(modipGrid,  55.1,   13.4,  57.975913);
        checkValue(modipGrid,  60.1,   13.4,  60.877488);
        checkValue(modipGrid,  65.1,   13.4,  63.776020);
        checkValue(modipGrid,  55.1,   23.4,  58.069325);
        checkValue(modipGrid,  60.1,   23.4,  60.961068);
        checkValue(modipGrid,  65.1,   23.4,  63.851642);
        checkValue(modipGrid,  55.1,   33.4,  58.262319);
        checkValue(modipGrid,  60.1,   33.4,  61.135017);
        checkValue(modipGrid,  65.1,   33.4,  63.995992);

    }

    @Test
    public void testItuGridField() {
        final ModipGrid modipGrid = new ModipGrid(NeQuickVersion.NEQUICK_2_ITU.getnbCellsLon(),
                                                  NeQuickVersion.NEQUICK_2_ITU.getnbCellsLat(),
                                                  NeQuickVersion.NEQUICK_2_ITU.getSource(),
                                                  NeQuickVersion.NEQUICK_2_ITU.isWrappingAlreadyIncluded());

        final Field<Binary64> field = Binary64Field.getInstance();

        // check non-existent points past pole
        for (double lonDeg = -1000.0; lonDeg <= 1000.0; lonDeg += 0.125) {
            checkValue(field, modipGrid, -91.0, lonDeg, -90.0);
            checkValue(field, modipGrid,  91.0, lonDeg,  90.0);
        }

        // check points just before pole
        for (double lonDeg = -1000.0; lonDeg <= 1000.0; lonDeg += 0.125) {
            checkValue(field, modipGrid, -90.0 + 1.0e-10,  lonDeg, -89.99);
            checkValue(field, modipGrid,  90.0 - 1.0e-10, lonDeg,  89.99);
        }

        // check wrapping in longitude of the first/last three columns
        for (double latDeg = -85.0; latDeg <= 90.0; latDeg += 5.0) {
            checkEquals(field, modipGrid, latDeg, -190.0, 170.0);
            checkEquals(field, modipGrid, latDeg, -180.0, 180.0);
            checkEquals(field, modipGrid, latDeg, -170.0, 190.0);
        }

        // check a few points exactly on grid points
        checkValue(field, modipGrid, -85.0, -190.0, -77.91);
        checkValue(field, modipGrid, -85.0, -120.0, -77.41);
        checkValue(field, modipGrid, -85.0,  110.0, -77.75);
        checkValue(field, modipGrid,  -5.0, -110.0,   1.97);
        checkValue(field, modipGrid,  60.0,   20.0,  60.87);

        // check interpolated points
        checkValue(field, modipGrid,  55.1,   13.4,  57.975913);
        checkValue(field, modipGrid,  60.1,   13.4,  60.877488);
        checkValue(field, modipGrid,  65.1,   13.4,  63.776020);
        checkValue(field, modipGrid,  55.1,   23.4,  58.069325);
        checkValue(field, modipGrid,  60.1,   23.4,  60.961068);
        checkValue(field, modipGrid,  65.1,   23.4,  63.851642);
        checkValue(field, modipGrid,  55.1,   33.4,  58.262319);
        checkValue(field, modipGrid,  60.1,   33.4,  61.135017);
        checkValue(field, modipGrid,  65.1,   33.4,  63.995992);

    }

    @Test
    public void testNoGridData() {
        try {
            new ModipGrid(12, 12, new DataSource("empty", () -> new StringReader("")), false);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.MODIP_GRID_NOT_LOADED, oe.getSpecifier());
            Assertions.assertEquals("empty", oe.getParts()[0]);
        }
    }

    @Test
    public void testParsingError() {
        try {
            new ModipGrid(4, 2,
                          new DataSource("dummy", () -> new StringReader("-90 -90 -90 -90 -90\n" +
                                                                         "  0   0  ##   0   0\n" +
                                                                         " 90  90  90  90  90\n")),
                          false);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(2, (Integer) oe.getParts()[0]);
            Assertions.assertEquals("dummy", oe.getParts()[1]);
            Assertions.assertEquals("  0   0  ##   0   0", oe.getParts()[2]);
        }
    }

    private void checkValue(final ModipGrid grid,
                            final double latDeg, final double lonDeg, final double expected) {
        Assertions.assertEquals(expected,
                                grid.computeMODIP(FastMath.toRadians(latDeg), FastMath.toRadians(lonDeg)),
                                1.0e-6);
    }

    private void checkEquals(final ModipGrid grid,
                            final double latDeg, final double lonDeg1, final double lonDeg2) {
        Assertions.assertEquals(grid.computeMODIP(FastMath.toRadians(latDeg), FastMath.toRadians(lonDeg1)),
                                grid.computeMODIP(FastMath.toRadians(latDeg), FastMath.toRadians(lonDeg2)),
                                1.0e-6);
    }

    private <T extends CalculusFieldElement<T>> void checkValue(final Field<T> field,
                                                                final ModipGrid grid,
                                                                final double latDeg, final double lonDeg,
                                                                final double expected) {
        Assertions.assertEquals(expected,
                                grid.computeMODIP(FastMath.toRadians(field.getZero().newInstance(latDeg)),
                                                  FastMath.toRadians(field.getZero().newInstance(lonDeg))).
                                    getReal(),
                                1.0e-6);
    }

    private <T extends CalculusFieldElement<T>> void checkEquals(final Field<T> field,
                                                                 final ModipGrid grid,
                                                                 final double latDeg, final double lonDeg1,
                                                                 final double lonDeg2) {
        Assertions.assertEquals(grid.computeMODIP(FastMath.toRadians(field.getZero().newInstance(latDeg)),
                                                  FastMath.toRadians(field.getZero().newInstance(lonDeg1))).
                                    getReal(),
                                grid.computeMODIP(FastMath.toRadians(field.getZero().newInstance(latDeg)),
                                                  FastMath.toRadians(field.getZero().newInstance(lonDeg2))).
                                    getReal(),
                                1.0e-6);
    }

}
