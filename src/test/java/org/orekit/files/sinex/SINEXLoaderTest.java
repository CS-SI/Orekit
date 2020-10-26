/* Copyright 2002-2020 CS GROUP
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
package org.orekit.files.sinex;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.sinex.Station.ReferenceSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

public class SINEXLoaderTest {

    @Before
    public void setUp() {
        // Sets the root of data to read
        Utils.setDataRoot("gnss:sinex");
    }

    @Test
    public void testSmallIGSSinexFile() {

        SINEXLoader loader = new SINEXLoader("cod20842-small.snx");
 
        Assert.assertEquals(2, loader.getStations().size());

        checkStation(loader.getStation("ABMF"), 2019, 350, 0.0, 2019, 352, 86370, 2019, 351, 43185,
                     "ABMF", "97103M001", Vector3D.ZERO,
                     new Vector3D(0.291978579235962e07, -0.538374495897593e07, 0.177460486102077e07),
                     Vector3D.ZERO);

        checkStation(loader.getStation("ABPO"), 2019, 350, 0.0, 2019, 352, 86370, 2019, 351, 43185,
                     "ABPO", "33302M001", new Vector3D(0.0083, 0., 0.),
                     new Vector3D(0.409721654480569e07, 0.442911920899428e07, -.206577118054971e07),
                     Vector3D.ZERO);

    }

    @Test
    public void testSLRSinexFile() {

        SINEXLoader loader = new SINEXLoader("SLRF2008_150928_2015.09.28.snx");

        // Test date computation using format description
        try {
            Method method = SINEXLoader.class.getDeclaredMethod("stringEpochToAbsoluteDate", String.class);
            method.setAccessible(true);
            final AbsoluteDate date = (AbsoluteDate) method.invoke(loader, "95:120:86399");
            final AbsoluteDate refDate = new AbsoluteDate("1995-04-30T23:59:59.000", TimeScalesFactory.getUTC());
            Assert.assertEquals(0., refDate.durationFrom(date), 0.);
        } catch (NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        // Test some values
        checkStation(loader.getStation("1885"), 1996, 310, 71317, 1996, 320, 73221, 2005, 1, 0.,
                     "1885", "12302S006", Vector3D.ZERO,
                     new Vector3D(0.318389220590831e07, 0.142146588920043e07, 0.532281398355808e07),
                     new Vector3D(-.239370506815545e-01 / Constants.JULIAN_YEAR,
                                  0.114173567092327e-01 / Constants.JULIAN_YEAR,
                                  -.145139658580209e-02 / Constants.JULIAN_YEAR));

        checkStation(loader.getStation("7082"), 1983, 313, 13398, 1984, 4, 83080, 2005, 1, 0.,
                     "7082", "40438M001", Vector3D.ZERO,
                     new Vector3D(-.173599736285899e07, -.442504854754010e07, 0.424143058893134e07),
                     new Vector3D(-.142509359401051e-01 / Constants.JULIAN_YEAR,
                                  -.975043019205914e-02 / Constants.JULIAN_YEAR,
                                  -.506419781207987e-03 / Constants.JULIAN_YEAR));
    }

    @Test
    public void testStationEccentricityXYZFile() {

        // Load file (it corresponds to a small version of the real entier file)
        SINEXLoader loader = new SINEXLoader("ecc_xyz-small.snx");
        Assert.assertEquals(3, loader.getStations().size());

        // Reference values
        final Vector3D ecc1148 = Vector3D.ZERO;
        final Vector3D ecc7035 = new Vector3D(-0.9670, -1.9490, 1.3990);
        final Vector3D ecc7120 = new Vector3D(-3.0850, -1.3670, 1.2620);

        // Verify
        Assert.assertEquals(ReferenceSystem.XYZ, loader.getStation("1148").getEccRefSystem());
        Assert.assertEquals(0., ecc1148.distance(loader.getStation("1148").getEccentricities()), 1.0e-15);
        Assert.assertEquals(ReferenceSystem.XYZ, loader.getStation("7035").getEccRefSystem());
        Assert.assertEquals(0., ecc7035.distance(loader.getStation("7035").getEccentricities()), 1.0e-15);
        Assert.assertEquals(ReferenceSystem.XYZ, loader.getStation("7120").getEccRefSystem());
        Assert.assertEquals(0., ecc7120.distance(loader.getStation("7120").getEccentricities()), 1.0e-15);

    }

    @Test
    public void testStationEccentricityUNEFile() {

        // Load file (it corresponds to a small version of the real entier file)
        SINEXLoader loader = new SINEXLoader("ecc_une-small.snx");
        Assert.assertEquals(3, loader.getStations().size());

        // Reference values
        final Vector3D ecc1148 = Vector3D.ZERO;
        final Vector3D ecc7035 = new Vector3D(2.5870, 0.0060, 0.0170);
        final Vector3D ecc7120 = new Vector3D(3.6020, -0.0130, 0.0090);

        // Verify
        Assert.assertEquals(ReferenceSystem.UNE, loader.getStation("1148").getEccRefSystem());
        Assert.assertEquals(0., ecc1148.distance(loader.getStation("1148").getEccentricities()), 1.0e-15);
        Assert.assertEquals(ReferenceSystem.UNE, loader.getStation("7035").getEccRefSystem());
        Assert.assertEquals(0., ecc7035.distance(loader.getStation("7035").getEccentricities()), 1.0e-15);
        Assert.assertEquals(ReferenceSystem.UNE, loader.getStation("7120").getEccRefSystem());
        Assert.assertEquals(0., ecc7120.distance(loader.getStation("7120").getEccentricities()), 1.0e-15);

    }

    @Test
    public void testCorruptedFile() {
        try {
            new SINEXLoader("cod20842-corrupted.snx");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assert.assertEquals(52, ((Integer) oe.getParts()[0]).intValue());
        }
    }

    private void checkStation(final Station station, final int startYear, final int startDay, final double secInStartDay,
                              final int endYear, final int endDay, final double secInEndDay,
                              final int epochYear, final int epochDay, final double secInEpoch,
                              final String siteCode, final String refDomes, final Vector3D refEcc,
                              final Vector3D refPos, final Vector3D refVel) {

        final AbsoluteDate start = new AbsoluteDate(new DateComponents(startYear, startDay),
                                                    new TimeComponents(secInStartDay),
                                                    TimeScalesFactory.getUTC());
        final AbsoluteDate end = new AbsoluteDate(new DateComponents(endYear, endDay),
                                                  new TimeComponents(secInEndDay),
                                                  TimeScalesFactory.getUTC());
        final AbsoluteDate epoch = new AbsoluteDate(new DateComponents(epochYear, epochDay),
                                                    new TimeComponents(secInEpoch),
                                                    TimeScalesFactory.getUTC());

        Assert.assertEquals(0., start.durationFrom(station.getValidFrom()), 1.0e-10);
        Assert.assertEquals(0., end.durationFrom(station.getValidUntil()),  1.0e-10);
        Assert.assertEquals(0., epoch.durationFrom(station.getEpoch()),     1.0e-10);
        Assert.assertEquals(siteCode, station.getSiteCode());
        Assert.assertEquals(refDomes, station.getDomes());
        Assert.assertEquals(0., refEcc.distance(station.getEccentricities()), 1.0e-10);
        Assert.assertEquals(0., refPos.distance(station.getPosition()), 1.0e-10);
        Assert.assertEquals(0., refVel.distance(station.getVelocity()), 1.0e-10);

    }
    
}
