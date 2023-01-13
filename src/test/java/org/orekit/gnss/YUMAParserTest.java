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
package org.orekit.gnss;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.analytical.gnss.data.GPSAlmanac;
import org.orekit.time.GNSSDate;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Locale;


public class YUMAParserTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("gnss");
    }

    @Test
    public void testNoFile() throws IOException, ParseException {
        // the parser for reading Yuma files with a pattern
        YUMAParser reader = new YUMAParser(".*\\.yum$");
        // No such YUMA file, should throw an exception
        try {
            reader.loadData();
        } catch (OrekitException oe) {
            Assertions.assertEquals("aucun fichier d'almanach Yuma n'a été trouvé", oe.getMessage(Locale.FRANCE));
        }
    }

    @Test
    public void testWrongFile() throws IOException, ParseException {
        // the parser for reading Yuma files with a pattern
        YUMAParser reader = new YUMAParser(".*\\.yum$");
        // the SEM file to read
        final String fileName = "/gnss/wrong_yuma.txt";
        final InputStream in = getClass().getResourceAsStream(fileName);
        // Reads the YUMA file, should throw an exception
        try {
            reader.loadData(in, fileName);
        } catch (OrekitException oe) {
            Assertions.assertEquals("le fichier /gnss/wrong_yuma.txt n'est pas un fichier d'almanach Yuma supporté",
                                oe.getMessage(Locale.FRANCE));
        }
    }

    @Test
    public void testLoadData() throws IOException, ParseException, OrekitException {
        // the parser for reading Yuma files with a pattern
        YUMAParser reader = new YUMAParser(".*\\.yum$");
        // the YUMA file to read
        final String fileName = "/gnss/yuma.txt";
        final InputStream in = getClass().getResourceAsStream(fileName);
        reader.loadData(in, fileName);

        Assertions.assertEquals(".*\\.yum$", reader.getSupportedNames());

        // Checks the whole file read
        Assertions.assertEquals(31, reader.getAlmanacs().size());
        Assertions.assertEquals(31, reader.getPRNNumbers().size());

        // Checks the last almanac read
        final GPSAlmanac alm = reader.getAlmanacs().get(reader.getAlmanacs().size() - 1);
        Assertions.assertEquals(32, alm.getPRN());
        Assertions.assertEquals(-1, alm.getSVN());
        Assertions.assertEquals(862, alm.getWeek());
        Assertions.assertEquals(319488.0, alm.getTime(), 0.);
        Assertions.assertEquals(5165.591309, FastMath.sqrt(alm.getSma()), FastMath.ulp(5.E+03));
        Assertions.assertEquals(0.7963180542E-004, alm.getE(), FastMath.ulp(8E-05));
        Assertions.assertEquals(0.9598609143,  alm.getI0(), 0.);
        Assertions.assertEquals(0., alm.getIDot(), 0.);
        Assertions.assertEquals(0.1426272192E+001, alm.getOmega0(), 0.);
        Assertions.assertEquals(-0.7737465154E-008, alm.getOmegaDot(), FastMath.ulp(-8E-09));
        Assertions.assertEquals(0.248929953, alm.getPa(), 0.);
        Assertions.assertEquals(0.1209154364E+001, alm.getM0(), 0.);
        Assertions.assertEquals(0.9536743164E-005, alm.getAf0(), 0.);
        Assertions.assertEquals(0.3637978807E-011, alm.getAf1(), 0.);
        Assertions.assertEquals(63, alm.getHealth());
        Assertions.assertEquals(-1, alm.getURA());
        Assertions.assertEquals(-1, alm.getSatConfiguration());
        Assertions.assertEquals("YUMA", alm.getSource());
        Assertions.assertTrue(alm.getDate().durationFrom(new GNSSDate(862, 319488.0, SatelliteSystem.GPS).getDate()) == 0);
        Assertions.assertEquals(0., alm.getCic(), 0.);
        Assertions.assertEquals(0., alm.getCis(), 0.);
        Assertions.assertEquals(0., alm.getCrc(), 0.);
        Assertions.assertEquals(0., alm.getCrs(), 0.);
        Assertions.assertEquals(0., alm.getCuc(), 0.);
        Assertions.assertEquals(0., alm.getCus(), 0.);
        Assertions.assertEquals(1.4484676210186782E-4, alm.getMeanMotion(), 0.);
    }

    @Test
    public void testLoadDefault() throws IOException, ParseException, OrekitException {
        // the parser for reading Yuma files
        YUMAParser reader = new YUMAParser(null);
        reader.loadData();

        Assertions.assertEquals(".*\\.alm$", reader.getSupportedNames());

        Assertions.assertEquals(31, reader.getAlmanacs().size());
        Assertions.assertEquals(31, reader.getPRNNumbers().size());

        // Checks the first almanac read
        final GPSAlmanac alm = reader.getAlmanacs().get(0);
        Assertions.assertEquals(1, alm.getPRN());
        Assertions.assertEquals(-1, alm.getSVN());
        Assertions.assertEquals(866, alm.getWeek());
        Assertions.assertEquals(589824.0, alm.getTime(), 0.);
        Assertions.assertEquals(5153.602051, FastMath.sqrt(alm.getSma()), FastMath.ulp(5.E+03));
        Assertions.assertEquals(0.5221366882E-02, alm.getE(), 0.);
        Assertions.assertEquals(0.963785748,  alm.getI0(), 0.);
        Assertions.assertEquals(0., alm.getIDot(), 0.);
        Assertions.assertEquals(-1.159458779E+000, alm.getOmega0(), 1.e-9);
        Assertions.assertEquals(-0.7897471819E-008, alm.getOmegaDot(), FastMath.ulp(-8E-09));
        Assertions.assertEquals(0.451712027, alm.getPa(), 1.e-9);
        Assertions.assertEquals(-0.2105941778E+001, alm.getM0(), 1.e-9);
        Assertions.assertEquals(0.1621246338E-004, alm.getAf0(), 1.e-14);
        Assertions.assertEquals(0.0, alm.getAf1(), 0.);
        Assertions.assertEquals(0, alm.getHealth());
        Assertions.assertEquals(-1, alm.getURA());
        Assertions.assertEquals(-1, alm.getSatConfiguration());
        Assertions.assertEquals("YUMA", alm.getSource());
        Assertions.assertTrue(alm.getDate().durationFrom(new GNSSDate(866, 589824.0, SatelliteSystem.GPS).getDate()) == 0);
        Assertions.assertEquals(0., alm.getCic(), 0.);
        Assertions.assertEquals(0., alm.getCis(), 0.);
        Assertions.assertEquals(0., alm.getCrc(), 0.);
        Assertions.assertEquals(0., alm.getCrs(), 0.);
        Assertions.assertEquals(0., alm.getCuc(), 0.);
        Assertions.assertEquals(0., alm.getCus(), 0.);
        Assertions.assertEquals(1.45860023309E-4, alm.getMeanMotion(), 1.e-15);
    }

}
