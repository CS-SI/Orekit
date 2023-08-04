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
import org.orekit.propagation.analytical.gnss.data.GNSSConstants;
import org.orekit.propagation.analytical.gnss.data.GPSAlmanac;
import org.orekit.time.GNSSDate;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Locale;


public class SEMParserTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("gnss");
    }

    @Test
    public void testNoFile() throws IOException, ParseException {
        // the parser for reading SEM files with *.sem as supported name for SEM files
        SEMParser reader = new SEMParser(".*\\.sem$");
        // No such YUMA file, should throw an exception
        try {
            reader.loadData();
        } catch (OrekitException oe) {
            Assertions.assertEquals("aucun fichier d'almanach SEM n'a été trouvé", oe.getMessage(Locale.FRANCE));
        }
    }

    @Test
    public void testWrongFile() throws IOException, ParseException {
        // the parser for reading SEM files with default supported name *.al3 for SEM files
        SEMParser reader = new SEMParser(null);
        // the SEM file to read
        final String fileName = "/gnss/wrong_sem.txt";
        final InputStream in = getClass().getResourceAsStream(fileName);
        // Reads the SEM file
        // No such YUMA file, should throw an exception
        try {
            reader.loadData(in, fileName);
        } catch (OrekitException oe) {
            Assertions.assertEquals("le fichier /gnss/wrong_sem.txt n'est pas un fichier d'almanach SEM supporté",
                                oe.getMessage(Locale.FRANCE));
        }
    }

    @Test
    public void testLoadData() throws IOException, ParseException, OrekitException {
        // the parser for reading SEM files with *.sem as supported name for SEM files
        SEMParser reader = new SEMParser(".*\\.sem$");
        // the SEM file to read
        final String fileName = "/gnss/current.al3";
        final InputStream in = getClass().getResourceAsStream(fileName);
        // Reads the SEM file
        reader.loadData(in, fileName);

        Assertions.assertEquals(".*\\.sem$", reader.getSupportedNames());

        // Checks the number of almanacs read
        Assertions.assertEquals(31, reader.getAlmanacs().size());
        Assertions.assertEquals(31, reader.getPRNNumbers().size());

        // Checks the first almanac read
        final GPSAlmanac alm = reader.getAlmanacs().get(0);
        Assertions.assertEquals(1, alm.getPRN());
        Assertions.assertEquals(63, alm.getSVN());
        Assertions.assertEquals(862, alm.getWeek());
        Assertions.assertEquals(319488.0, alm.getTime(), 0.);
        Assertions.assertEquals(5.15360253906250E+03, FastMath.sqrt(alm.getSma()), FastMath.ulp(5.E+03));
        Assertions.assertEquals(5.10072708129883E-03, alm.getE(), FastMath.ulp(8E-05));
        Assertions.assertEquals(6.84547424316406E-03,  (alm.getI0() / GNSSConstants.GNSS_PI) - 0.30, 1.E-17);
        Assertions.assertEquals(0., alm.getIDot(), 0.);
        Assertions.assertEquals(-2.08778738975525E-01, alm.getOmega0() / GNSSConstants.GNSS_PI, FastMath.ulp(-2E-01));
        Assertions.assertEquals(-2.48837750405073E-09, alm.getOmegaDot() / GNSSConstants.GNSS_PI, FastMath.ulp(-3E-09));
        Assertions.assertEquals(1.46086812019348E-01, alm.getPa() / GNSSConstants.GNSS_PI, FastMath.ulp(1E-01));
        Assertions.assertEquals(4.55284833908081E-01, alm.getM0() / GNSSConstants.GNSS_PI, FastMath.ulp(4E-01));
        Assertions.assertEquals(1.33514404296875E-05, alm.getAf0(), FastMath.ulp(1E-05));
        Assertions.assertEquals(0., alm.getAf1(), 0.);
        Assertions.assertEquals(0, alm.getHealth());
        Assertions.assertEquals(0, alm.getURA());
        Assertions.assertEquals(11, alm.getSatConfiguration());
        Assertions.assertEquals("SEM", alm.getSource());
        Assertions.assertEquals(alm.getDate().durationFrom(new GNSSDate(862, 319488.0, SatelliteSystem.GPS).getDate()), 0, 0);
        Assertions.assertEquals(0., alm.getCic(), 0.);
        Assertions.assertEquals(0., alm.getCis(), 0.);
        Assertions.assertEquals(0., alm.getCrc(), 0.);
        Assertions.assertEquals(0., alm.getCrs(), 0.);
        Assertions.assertEquals(0., alm.getCuc(), 0.);
        Assertions.assertEquals(0., alm.getCus(), 0.);
        Assertions.assertEquals(1.4585998186870066E-4, alm.getMeanMotion(), 0.);
    }

    @Test
    public void testLoadDefault() throws IOException, ParseException, OrekitException {
        // the parser for reading SEM files with default supported name *.al3 for SEM files
        SEMParser reader = new SEMParser(null);
        // Reads the SEM file
        reader.loadData();

        Assertions.assertEquals(".*\\.al3$", reader.getSupportedNames());

        // Checks the number of almanacs read
        Assertions.assertEquals(31, reader.getAlmanacs().size());
        Assertions.assertEquals(31, reader.getPRNNumbers().size());

        // Checks the last almanac read
        final GPSAlmanac alm = reader.getAlmanacs().get(reader.getAlmanacs().size() - 1);
        Assertions.assertEquals(32, alm.getPRN());
        Assertions.assertEquals(70, alm.getSVN());
        Assertions.assertEquals(862, alm.getWeek());
        Assertions.assertEquals(319488.0, alm.getTime(), 0.);
        Assertions.assertEquals(5.16559130859375E+03, FastMath.sqrt(alm.getSma()), FastMath.ulp(5.E+03));
        Assertions.assertEquals(7.96318054199219E-05, alm.getE(), FastMath.ulp(8E-05));
        Assertions.assertEquals(5.53321838378906E-03,  (alm.getI0() / GNSSConstants.GNSS_PI) - 0.30, 1.E-17);
        Assertions.assertEquals(0., alm.getIDot(), 0.);
        Assertions.assertEquals(4.53996539115906E-01, alm.getOmega0() / GNSSConstants.GNSS_PI, FastMath.ulp(5E-01));
        Assertions.assertEquals(-2.46291165240109E-09, alm.getOmegaDot() / GNSSConstants.GNSS_PI, FastMath.ulp(-3E-09));
        Assertions.assertEquals(7.92368650436401E-02, alm.getPa() / GNSSConstants.GNSS_PI, FastMath.ulp(8E-02));
        Assertions.assertEquals(3.84885787963867E-01, alm.getM0() / GNSSConstants.GNSS_PI, FastMath.ulp(4E-01));
        Assertions.assertEquals(9.5367431640625E-6, alm.getAf0(), 0.);
        Assertions.assertEquals(3.63797880709171E-12, alm.getAf1(), 0.);
        Assertions.assertEquals(63, alm.getHealth());
        Assertions.assertEquals(0, alm.getURA());
        Assertions.assertEquals(11, alm.getSatConfiguration());
        Assertions.assertEquals("SEM", alm.getSource());
        Assertions.assertTrue(alm.getDate().durationFrom(new GNSSDate(862, 319488.0, SatelliteSystem.GPS).getDate()) == 0);
        Assertions.assertEquals(0., alm.getCic(), 0.);
        Assertions.assertEquals(0., alm.getCis(), 0.);
        Assertions.assertEquals(0., alm.getCrc(), 0.);
        Assertions.assertEquals(0., alm.getCrs(), 0.);
        Assertions.assertEquals(0., alm.getCuc(), 0.);
        Assertions.assertEquals(0., alm.getCus(), 0.);
        Assertions.assertEquals(1.4484676213604242E-4, alm.getMeanMotion(), 0.);
    }

}
