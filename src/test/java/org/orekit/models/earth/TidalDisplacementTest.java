/* Copyright 2011-2012 Space Applications Services
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.models.earth;


import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

public class TidalDisplacementTest {

    @Test
    public void testFrame() throws OrekitException {
        IERSConventions conventions = IERSConventions.IERS_2010;
        Frame itrf = FramesFactory.getITRF(conventions, false);
        TidalDisplacement td = new TidalDisplacement(itrf, Constants.EIGEN5C_EARTH_MU,
                                                     Constants.JPL_SSD_SUN_EARTH_PLUS_MOON_MASS_RATIO,
                                                     Constants.JPL_SSD_EARTH_MOON_MASS_RATIO,
                                                     CelestialBodyFactory.getSun(),
                                                     CelestialBodyFactory.getMoon(),
                                                     conventions);
        Assert.assertSame(itrf, td.getEarthFrame());
    }

    @Test
    public void testLoveShida() throws OrekitException {
        for (final IERSConventions conventions : IERSConventions.values()) {
            // as of Orekit 9.0, supported conventions are
            // IERS conventions 1996, IERS conventions 2003 and IERS conventions 2010
            // and they all share the same values for anelastic Earth model
            double[] hl = conventions.getNominalTidalDisplacementLoveAndShida();
            Assert.assertEquals(12, hl.length);
            Assert.assertEquals( 0.6078, hl[ 0], 1.0e-15); // h⁽⁰⁾
            Assert.assertEquals(-0.0006, hl[ 1], 1.0e-15); // h⁽²⁾
            Assert.assertEquals( 0.292,  hl[ 2], 1.0e-15); // h₃
            Assert.assertEquals(-0.0025, hl[ 3], 1.0e-15); // hI diurnal
            Assert.assertEquals(-0.0022, hl[ 4], 1.0e-15); // hI semi-diurnal
            Assert.assertEquals( 0.0847, hl[ 5], 1.0e-15); // l⁽⁰⁾
            Assert.assertEquals( 0.0012, hl[ 6], 1.0e-15); // l⁽¹⁾ diurnal
            Assert.assertEquals( 0.0024, hl[ 7], 1.0e-15); // l⁽¹⁾ semi-diurnal
            Assert.assertEquals( 0.0002, hl[ 8], 1.0e-15); // l⁽²⁾
            Assert.assertEquals( 0.015,  hl[ 9], 1.0e-15); // l₃
            Assert.assertEquals(-0.0007, hl[10], 1.0e-15); // lI diurnal
            Assert.assertEquals(-0.0007, hl[11], 1.0e-15); // lI semi-diurnal
        }
    }

    @Test
    public void testDehant() throws OrekitException {

        IERSConventions conventions = IERSConventions.IERS_2010;
        Frame itrf = FramesFactory.getITRF(conventions, false);

        // constants consistent with DEHANTTIDEINEL.F reference program
        // available at <ftp://tai.bipm.org/iers/conv2010/chapter7/dehanttideinel/>
        // and Copyright (C) 2008 IERS Conventions Center
        double massRatioSun  = 332946.0482;
        double massRatioMoon = 0.0123000371;
        double re            = 6378136.6;

        // fake providers generating only the positions from the reference program test
        PVCoordinatesProvider fakeSun  = (date, frame) -> new TimeStampedPVCoordinates(date,
                                                                                       new Vector3D(137859926952.015,
                                                                                                    54228127881.435,
                                                                                                    23509422341.6960),
                                                                                       Vector3D.ZERO,
                                                                                       Vector3D.ZERO);
        PVCoordinatesProvider fakeMoon = (date, frame) -> new TimeStampedPVCoordinates(date,
                                                                                       new Vector3D(-179996231.920342,
                                                                                                    -312468450.131567,
                                                                                                    -169288918.592160),
                                                                                       Vector3D.ZERO,
                                                                                       Vector3D.ZERO);
        TidalDisplacement td = new TidalDisplacement(itrf, re,
                                                     massRatioSun * (1.0 / (1.0 + massRatioMoon)),
                                                     1.0 / massRatioMoon,
                                                     fakeSun,
                                                     fakeMoon,
                                                     conventions);

        Vector3D fundamentalStationWettzell = new Vector3D(4075578.385, 931852.890, 4801570.154);
        AbsoluteDate date = new AbsoluteDate(2009, 4, 13, 0, 0, 0.0,
                                             TimeScalesFactory.getUT1(conventions, false));
        Vector3D displacement = td.displacement(date, fundamentalStationWettzell);
        Assert.assertEquals(0.07700420357108125891, displacement.getX(), 1.0e-10);
        Assert.assertEquals(0.06304056321824967613, displacement.getY(), 1.0e-10);
        Assert.assertEquals(0.05516568152597246810, displacement.getZ(), 1.0e-10);

    }

    @Before
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data");
    }

}
