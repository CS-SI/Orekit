/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Locale;

import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.analytical.gnss.GPSOrbitalElements;
import org.orekit.time.AbsoluteDate;


public class SEMParserTest {

    @Before
    public void setUp() throws OrekitException {
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
            Assert.assertEquals("aucun fichier d'almanach SEM n'a été trouvé", oe.getMessage(Locale.FRANCE));
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
            Assert.assertEquals("le fichier /gnss/wrong_sem.txt n'est pas un fichier d'almanach SEM supporté",
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

        Assert.assertEquals(".*\\.sem$", reader.getSupportedNames());

        // Checks the number of almanacs read
        Assert.assertEquals(31, reader.getAlmanacs().size());
        Assert.assertEquals(31, reader.getPRNNumbers().size());

        // Checks the first almanac read
        final GPSAlmanac alm = reader.getAlmanacs().get(0);
        Assert.assertEquals(1, alm.getPRN());
        Assert.assertEquals(63, alm.getSVN());
        Assert.assertEquals(862, alm.getWeek());
        Assert.assertEquals(319488.0, alm.getTime(), 0.);
        Assert.assertEquals(5.15360253906250E+03, FastMath.sqrt(alm.getSma()), FastMath.ulp(5.E+03));
        Assert.assertEquals(5.10072708129883E-03, alm.getE(), FastMath.ulp(8E-05));
        Assert.assertEquals(6.84547424316406E-03,  (alm.getI0() / GPSOrbitalElements.GPS_PI) - 0.30, 1.E-17);
        Assert.assertEquals(0., alm.getIDot(), 0.);
        Assert.assertEquals(-2.08778738975525E-01, alm.getOmega0() / GPSOrbitalElements.GPS_PI, FastMath.ulp(-2E-01));
        Assert.assertEquals(-2.48837750405073E-09, alm.getOmegaDot() / GPSOrbitalElements.GPS_PI, FastMath.ulp(-3E-09));
        Assert.assertEquals(1.46086812019348E-01, alm.getPa() / GPSOrbitalElements.GPS_PI, FastMath.ulp(1E-01));
        Assert.assertEquals(4.55284833908081E-01, alm.getM0() / GPSOrbitalElements.GPS_PI, FastMath.ulp(4E-01));
        Assert.assertEquals(1.33514404296875E-05, alm.getAf0(), FastMath.ulp(1E-05));
        Assert.assertEquals(0., alm.getAf1(), 0.);
        Assert.assertEquals(0, alm.getHealth());
        Assert.assertEquals(0, alm.getURA());
        Assert.assertEquals(11, alm.getSatConfiguration());
        Assert.assertEquals("SEM", alm.getSource());
        Assert.assertTrue(alm.getDate().durationFrom(AbsoluteDate.createGPSDate(862, 319488 * 1000.)) == 0);
        Assert.assertEquals(0., alm.getCic(), 0.);
        Assert.assertEquals(0., alm.getCis(), 0.);
        Assert.assertEquals(0., alm.getCrc(), 0.);
        Assert.assertEquals(0., alm.getCrs(), 0.);
        Assert.assertEquals(0., alm.getCuc(), 0.);
        Assert.assertEquals(0., alm.getCus(), 0.);
        Assert.assertEquals(1.4585998186870066E-4, alm.getMeanMotion(), 0.);
    }

    @Test
    public void testLoadDefault() throws IOException, ParseException, OrekitException {
        // the parser for reading SEM files with default supported name *.al3 for SEM files
        SEMParser reader = new SEMParser(null);
        // Reads the SEM file
        reader.loadData();

        Assert.assertEquals(".*\\.al3$", reader.getSupportedNames());

        // Checks the number of almanacs read
        Assert.assertEquals(31, reader.getAlmanacs().size());
        Assert.assertEquals(31, reader.getPRNNumbers().size());

        // Checks the last almanac read
        final GPSAlmanac alm = reader.getAlmanacs().get(reader.getAlmanacs().size() - 1);
        Assert.assertEquals(32, alm.getPRN());
        Assert.assertEquals(70, alm.getSVN());
        Assert.assertEquals(862, alm.getWeek());
        Assert.assertEquals(319488.0, alm.getTime(), 0.);
        Assert.assertEquals(5.16559130859375E+03, FastMath.sqrt(alm.getSma()), FastMath.ulp(5.E+03));
        Assert.assertEquals(7.96318054199219E-05, alm.getE(), FastMath.ulp(8E-05));
        Assert.assertEquals(5.53321838378906E-03,  (alm.getI0() / GPSOrbitalElements.GPS_PI) - 0.30, 1.E-17);
        Assert.assertEquals(0., alm.getIDot(), 0.);
        Assert.assertEquals(4.53996539115906E-01, alm.getOmega0() / GPSOrbitalElements.GPS_PI, FastMath.ulp(5E-01));
        Assert.assertEquals(-2.46291165240109E-09, alm.getOmegaDot() / GPSOrbitalElements.GPS_PI, FastMath.ulp(-3E-09));
        Assert.assertEquals(7.92368650436401E-02, alm.getPa() / GPSOrbitalElements.GPS_PI, FastMath.ulp(8E-02));
        Assert.assertEquals(3.84885787963867E-01, alm.getM0() / GPSOrbitalElements.GPS_PI, FastMath.ulp(4E-01));
        Assert.assertEquals(9.5367431640625E-6, alm.getAf0(), 0.);
        Assert.assertEquals(3.63797880709171E-12, alm.getAf1(), 0.);
        Assert.assertEquals(63, alm.getHealth());
        Assert.assertEquals(0, alm.getURA());
        Assert.assertEquals(11, alm.getSatConfiguration());
        Assert.assertEquals("SEM", alm.getSource());
        Assert.assertTrue(alm.getDate().durationFrom(AbsoluteDate.createGPSDate(862, 319488 * 1000.)) == 0);
        Assert.assertEquals(0., alm.getCic(), 0.);
        Assert.assertEquals(0., alm.getCis(), 0.);
        Assert.assertEquals(0., alm.getCrc(), 0.);
        Assert.assertEquals(0., alm.getCrs(), 0.);
        Assert.assertEquals(0., alm.getCuc(), 0.);
        Assert.assertEquals(0., alm.getCus(), 0.);
        Assert.assertEquals(1.4484676213604244E-4, alm.getMeanMotion(), 0.);
    }

}
