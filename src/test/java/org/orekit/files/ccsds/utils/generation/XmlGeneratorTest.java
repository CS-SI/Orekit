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
package org.orekit.files.ccsds.utils.generation;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.utils.Constants;
import org.orekit.utils.units.Unit;

import java.io.CharArrayWriter;
import java.io.IOException;

public class XmlGeneratorTest {

    @Test
    public void testSections() throws IOException {
        CharArrayWriter caw = new CharArrayWriter();
        try (Generator generator = new XmlGenerator(caw, 2, "", Constants.JULIAN_DAY, true, null)) {
            generator.startMessage("abc", "CCSDS_ABC_VERSION", 99.0);
            generator.enterSection("BLOCK");
            generator.writeEntry("KEY", 1234567.8, Unit.parse("Hz"), false);
            generator.exitSection();
            generator.endMessage("abc");
            Assertions.assertEquals(String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>%n" +
                                              "<abc id=\"CCSDS_ABC_VERSION\" version=\"99.0\">%n" +
                                              "  <BLOCK>%n" +
                                              "    <KEY units=\"Hz\">1234567.8</KEY>%n" +
                                              "  </BLOCK>%n" +
                                              "</abc>%n"),
                                caw.toString());

        }
    }

    @Test
    public void testCcsdsUnits() throws IOException {
        CharArrayWriter caw = new CharArrayWriter();
        try (Generator generator = new XmlGenerator(caw, 2, "", Constants.JULIAN_DAY, true, "nowhere")) {
            generator.writeEntry("KEY_1",    1234567.8,   Unit.parse("km.kg³/√s"), false);
            generator.writeEntry("KEY_2",    1234567.8,   Unit.parse("n/a"),       false);
            generator.writeEntry("KEY_3",    1234567.8,   Unit.parse("1"),         false);
            generator.writeEntry("LOOOOONG", "1234567.8", null,                    false);
            Assertions.assertEquals(String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>%n" +
                                              "<KEY_1 units=\"km*kg**3/s**0.5\">1234.5678</KEY_1>%n" +
                                              "<KEY_2>1234567.8</KEY_2>%n" +
                                              "<KEY_3>1234567.8</KEY_3>%n" +
                                              "<LOOOOONG>1234567.8</LOOOOONG>%n"),
                                caw.toString());
        }
    }

    @Test
    public void testUnitsPadding() throws IOException {
        CharArrayWriter caw = new CharArrayWriter();
        try (Generator generator = new XmlGenerator(caw, 2, "", Constants.JULIAN_DAY, true, "nowhere")) {
            generator.writeEntry("KEY_1", 0.5 * FastMath.PI, Unit.parse("°"), false);
            generator.writeEntry("KEY_2", FastMath.PI, Unit.parse("◦"), false);
            generator.writeEntry("PERCENT", 0.25, Unit.parse("%"), false);
            Assertions.assertEquals(String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>%n" +
                                              "<KEY_1 units=\"deg\">90.0</KEY_1>%n" +
                                              "<KEY_2 units=\"deg\">180.0</KEY_2>%n" +
                                              "<PERCENT units=\"%%\">25.0</PERCENT>%n"),
                                caw.toString());
        }
    }

    @Test
    public void testNoUnits() throws IOException {
        CharArrayWriter caw = new CharArrayWriter();
        try (Generator generator = new XmlGenerator(caw, 2, "", Constants.JULIAN_DAY, false, "nowhere")) {
            generator.writeEntry("KEY_1", 0.5 * FastMath.PI, Unit.parse("°"), false);
            generator.writeEntry("KEY_2", FastMath.PI, Unit.parse("◦"), true);
            Assertions.assertEquals(String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>%n" +
                                              "<KEY_1>90.0</KEY_1>%n" +
                                              "<KEY_2>180.0</KEY_2>%n"),
                                caw.toString());
        }
    }

}
