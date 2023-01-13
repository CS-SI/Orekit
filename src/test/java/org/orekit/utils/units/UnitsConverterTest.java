/* Copyright 2002-2023 CS GROUP
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/**
 * Unit tests for {@link Lexer}.
 *
 * @author Luc Maisonobe
 */
public class UnitsConverterTest {

    @Test
    public void testTime() {
        final UnitsConverter day2second = new UnitsConverter(Unit.DAY, Unit.SECOND);
        Assertions.assertEquals(388800.0, day2second.convert(4.5), 1.0e-9);
    }

    @Test
    public void testAngle() {
        final UnitsConverter rev2deg = new UnitsConverter(Unit.REVOLUTION, Unit.DEGREE);
        Assertions.assertEquals(360.0, rev2deg.convert(1.0), 1.0e-12);
    }

    @Test
    public void testRotationRate() {

    }

    @Test
    public void testPressure() {
        final double atm = 1013.25;
        final Unit mbar = Unit.parse("mbar");
        final Unit hPa  = Unit.parse("hPa");
        final UnitsConverter mbar2hPa = new UnitsConverter(mbar, hPa);
        Assertions.assertEquals(atm, mbar2hPa.convert(atm), 1.0e-12);
    }

    @Test
    public void testIncompatibleUnits() {
        try {
            new UnitsConverter(Unit.parse("km³/s²"), Unit.parse("MΩ"));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPATIBLE_UNITS, oe.getSpecifier());
            Assertions.assertEquals("km³/s²", oe.getParts()[0]);
            Assertions.assertEquals("MΩ", oe.getParts()[1]);
        }
    }

}
