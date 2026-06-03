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
package org.orekit.files.ccsds.utils.generation;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.files.ccsds.definitions.TimeConverter;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.units.Unit;

import java.io.CharArrayWriter;
import java.io.IOException;

public class KvnGeneratorTest {

    @Test
    public void testSection() throws IOException {
        CharArrayWriter caw = new CharArrayWriter();
        try (Generator generator = new KvnGenerator(caw, 10, "", Constants.JULIAN_DAY, 25)) {
            generator.startMessage("abc", "CCSDS_ABC_VERSION", 99.0);
            generator.enterSection("BLOCK");
            generator.writeEntry("KEY", 1234567.8, Unit.parse("Hz"), false);
            generator.exitSection();
            generator.endMessage("abc");
            Assertions.assertEquals("CCSDS_ABC_VERSION = 99.0\n" +
                                "BLOCK_START\n" +
                                "KEY        = 1234567.8   [Hz]\n" +
                                "BLOCK_STOP\n",
                                caw.toString());
        }
    }

    @Test
    public void testCcsdsUnits() throws IOException {
        CharArrayWriter caw = new CharArrayWriter();
        try (Generator generator = new KvnGenerator(caw, 10, "", Constants.JULIAN_DAY, 25)) {
            generator.writeEntry("KEY_1",    1234567.8,   Unit.parse("km.kg³/√s"), false);
            generator.writeEntry("KEY_2",    1234567.8,   Unit.parse("n/a"),       false);
            generator.writeEntry("KEY_3",    1234567.8,   Unit.parse("1"),         false);
            generator.writeEntry("LOOOOONG", "1234567.8", null,                    false);
            Assertions.assertEquals("KEY_1      = 1234.5678   [km*kg**3/s**0.5]\n" +
                                "KEY_2      = 1234567.8\n" +
                                "KEY_3      = 1234567.8\n" +
                                "LOOOOONG   = 1234567.8\n",
                                caw.toString());
        }
    }

    @Test
    public void testUnitsPadding() throws IOException {
        CharArrayWriter caw = new CharArrayWriter();
        try (Generator generator = new KvnGenerator(caw, 10, "", Constants.JULIAN_DAY, 20)) {
            generator.writeEntry("KEY_1", 0.5 * FastMath.PI, Unit.parse("°"), false);
            generator.writeEntry("KEY_2", FastMath.PI, Unit.parse("◦"), false);
            generator.writeEntry("PERCENT", 0.25, Unit.parse("%"), false);
            Assertions.assertEquals("KEY_1      = 90.0   [deg]\n" +
                                "KEY_2      = 180.0  [deg]\n" +
                                "PERCENT    = 25.0   [%]\n",
                                caw.toString());
        }
    }

    @Test
    public void testNoUnits() throws IOException {
        CharArrayWriter caw = new CharArrayWriter();
        try (Generator generator = new KvnGenerator(caw, 10, "", Constants.JULIAN_DAY, 0)) {
            generator.writeEntry("KEY_1", 0.5 * FastMath.PI, Unit.parse("°"), false);
            generator.writeEntry("KEY_2", FastMath.PI, Unit.parse("◦"), true);
            Assertions.assertEquals("KEY_1      = 90.0\n" +
                                "KEY_2      = 180.0\n", caw.toString());
        }
    }

    /**
     * Test to write a date with non-representable seconds on a double.
     *
     * @see <a href="https://gitlab.orekit.org/orekit/orekit/issues/1962">Issue 1962</a>
     */
    @Test
    void testWriteDateEntry() throws IOException {
        // GIVEN
        // Load orekit data
        Utils.setDataRoot("regular-data");

        // Create the generator
        final Appendable appendable = new StringBuilder();
        final Generator  generator  = new KvnGenerator(appendable, 0, "", 0, 0);

        // Create the time converter
        final TimeScale     utc       = TimeScalesFactory.getUTC();
        final TimeConverter converter = new TimeConverter(utc, new AbsoluteDate());

        // Create date (seconds cannot be stored inside a double, will introduce an ULP drift).
        final String       referenceDateString = "2026-05-21T23:04:00.934";
        final AbsoluteDate date                = new AbsoluteDate(referenceDateString, utc);

        // WHEN
        generator.writeEntry("date", converter, date, false, true);

        // THEN
        Assertions.assertEquals("date = 2026-05-21T23:04:00.934\n", appendable.toString());
    }

}
