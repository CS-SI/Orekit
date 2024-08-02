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
package org.orekit.utils.units;

import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit tests for {@link Lexer}.
 *
 * @author Luc Maisonobe
 */
class UnitsConverterTest {

    @Test
    void testTime() {
        final UnitsConverter day2second = new UnitsConverter(Unit.DAY, Unit.SECOND);
        assertEquals(388800.0, day2second.convert(4.5), 1.0e-9);
    }

    @Test
    void testAngle() {
        final UnitsConverter rev2deg = new UnitsConverter(Unit.REVOLUTION, Unit.DEGREE);
        assertEquals(360.0, rev2deg.convert(1.0), 1.0e-12);
    }

    @Test
    void testRotationRate() {

    }

    @Test
    void testPressure() {
        final double atm = 1013.25;
        final Unit mbar  = Unit.parse("mbar");
        final Unit hPa   = Unit.parse("hPa");
        final UnitsConverter mbar2hPa = new UnitsConverter(mbar, hPa);
        assertEquals(atm, mbar2hPa.convert(atm), 1.0e-12);
    }

    @Test
    void testIncompatibleUnits() {
        try {
            new UnitsConverter(Unit.parse("km³/s²"), Unit.parse("MΩ"));
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.INCOMPATIBLE_UNITS, oe.getSpecifier());
            assertEquals("km³/s²", oe.getParts()[0]);
            assertEquals("MΩ", oe.getParts()[1]);
        }
    }

}
