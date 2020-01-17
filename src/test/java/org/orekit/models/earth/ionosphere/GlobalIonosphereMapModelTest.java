/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
/*
 * Copyright 2018-2019 CS Group.
 * All rights reserved.
 */
package org.orekit.models.earth.ionosphere;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.gnss.Frequency;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class GlobalIonosphereMapModelTest {

    private static double epsilonParser = 1.0e-16;
    private static double epsilonDelay  = 0.001;
    private SpacecraftState state;
    private OneAxisEllipsoid earth;
    private GlobalIonosphereMapModel model;

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data:ionex");
        model = new GlobalIonosphereMapModel("gpsg0150.19i");
        final Orbit orbit = new KeplerianOrbit(24464560.0, 0.0, 1.122138, 1.10686, 1.00681,
                                               0.048363, PositionAngle.MEAN,
                                               FramesFactory.getEME2000(),
                                               new AbsoluteDate(2019, 1, 14, 23, 59, 59.0, TimeScalesFactory.getUTC()),
                                               Constants.WGS84_EARTH_MU);
        state = new SpacecraftState(orbit);
        earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                     Constants.WGS84_EARTH_FLATTENING,
                                     FramesFactory.getITRF(IERSConventions.IERS_2010, true));
    }

    @Test
    public void testTEC() {
        final double latitude  = FastMath.toRadians(30.0);
        final double longitude = FastMath.toRadians(-130.0); 
        final double tec = model.getTEC(new AbsoluteDate(2019, 1, 15, 3, 43, 12.0, TimeScalesFactory.getUTC()),
                                        new GeodeticPoint(latitude, longitude, 0.0));
        Assert.assertEquals(9.592, tec, epsilonDelay);
    }

    @Test
    public void testFieldTEC() {
        doTestFieldTEC(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestFieldTEC(final Field<T> field) {
        final double latitude  = FastMath.toRadians(30.0);
        final double longitude = FastMath.toRadians(-130.0);
        final T tec = model.getTEC(new FieldAbsoluteDate<>(field, 2019, 1, 15, 3, 43, 12.0, TimeScalesFactory.getUTC()),
                                   new GeodeticPoint(latitude, longitude, 0.0));
        Assert.assertEquals(9.592, tec.getReal(), epsilonDelay);
    }

    @Test
    public void testDelay() {
        final double latitude  = FastMath.toRadians(30.0);
        final double longitude = FastMath.toRadians(-130.0);
        final double delay = model.pathDelay(new AbsoluteDate(2019, 1, 15, 3, 43, 12.0, TimeScalesFactory.getUTC()),
                                             new GeodeticPoint(latitude, longitude, 0.0),
                                             0.5 * FastMath.PI, Frequency.G01.getMHzFrequency() * 1.0e6);
        Assert.assertEquals(1.557, delay, epsilonDelay);
    }

    @Test
    public void testFieldDelay() {
        doTestFieldDelay(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestFieldDelay(final Field<T> field) {
        final T zero = field.getZero();
        final double latitude  = FastMath.toRadians(30.0);
        final double longitude = FastMath.toRadians(-130.0);
        final T delay = model.pathDelay(new FieldAbsoluteDate<>(field, 2019, 1, 15, 3, 43, 12.0, TimeScalesFactory.getUTC()),
                                        new GeodeticPoint(latitude, longitude, 0.0),
                                        zero.add(0.5 * FastMath.PI),
                                        Frequency.G01.getMHzFrequency() * 1.0e6);
        Assert.assertEquals(1.557, delay.getReal(), epsilonDelay);
    }

    @Test
    public void testParser() {

        // Commons parameters
        AbsoluteDate date = new AbsoluteDate(2019, 1, 15, 0, 0, 0.0, TimeScalesFactory.getUTC());
        final double latitude = FastMath.toRadians(45.0);
        
        double longitude1;
        double longitude2;

        // Test longitude = 181° and longitude = -179°
        longitude1 = FastMath.toRadians(181.0);
        longitude2 = FastMath.toRadians(-179.0);

        Assert.assertEquals(model.getTEC(date, new GeodeticPoint(latitude, longitude1, 0.0)), model.getTEC(date, new GeodeticPoint(latitude, longitude2, 0.0)), epsilonParser);

        // Test longitude = 180° and longitude = -180°
        longitude1 = FastMath.toRadians(180.0);
        longitude2 = FastMath.toRadians(-180.0);

        Assert.assertEquals(model.getTEC(date, new GeodeticPoint(latitude, longitude1, 0.0)), model.getTEC(date, new GeodeticPoint(latitude, longitude2, 0.0)), epsilonParser);

        // Test longitude = 0° and longitude = 360°
        longitude1 =  FastMath.toRadians(0.);
        longitude2 =  FastMath.toRadians(360.0);

        Assert.assertEquals(model.getTEC(date, new GeodeticPoint(latitude, longitude1, 0.0)), model.getTEC(date, new GeodeticPoint(latitude, longitude2, 0.0)), epsilonParser);

    }

    @Test
    public void testCorruptedFileBadData() {
        final String fileName = "corrupted-bad-data-gpsg0150.19i";
        final double latitude  = FastMath.toRadians(30.0);
        final double longitude = FastMath.toRadians(-130.0);
          
        try {
            GlobalIonosphereMapModel corruptedModel = new GlobalIonosphereMapModel(fileName);
            corruptedModel.getTEC(new AbsoluteDate(2019, 1, 15, 0, 0, 0.0, TimeScalesFactory.getUTC()),
                         new GeodeticPoint(latitude, longitude, 0.0));
            Assert.fail("An exception should have been thrown");
            
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
        }
    }

    @Test
    public void testEarlierDate() {
        final double latitude  = FastMath.toRadians(60.0);
        final double longitude = FastMath.toRadians(-130.0);
        final GeodeticPoint point = new GeodeticPoint(latitude, longitude, 0.0);

        try {
            model.pathDelay(state, new TopocentricFrame(earth, point, null),
                            Frequency.G01.getMHzFrequency() * 1.0e6,
                            model.getParameters());
            Assert.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NO_TEC_DATA_IN_FILE_FOR_DATE, oe.getSpecifier());
        }
    }

    @Test
    public void testFieldEarlierDate() {
        doTestFieldEarlierDate(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestFieldEarlierDate(final Field<T> field) {
        final double latitude  = FastMath.toRadians(60.0);
        final double longitude = FastMath.toRadians(-130.0);
        final GeodeticPoint point = new GeodeticPoint(latitude, longitude, 0.0);

        try {
            model.pathDelay(new FieldSpacecraftState<>(field, state), new TopocentricFrame(earth, point, null),
                            Frequency.G01.getMHzFrequency() * 1.0e6,
                            model.getParameters(field));
            Assert.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NO_TEC_DATA_IN_FILE_FOR_DATE, oe.getSpecifier());
        }
    }

    @Test
    public void testLaterDate() {
        final double latitude  = FastMath.toRadians(60.0);
        final double longitude = FastMath.toRadians(-130.0);
        final GeodeticPoint point = new GeodeticPoint(latitude, longitude, 0.0);

        try {
            model.pathDelay(state, new TopocentricFrame(earth, point, null),
                            Frequency.G01.getMHzFrequency() * 1.0e6,
                            model.getParameters());
            Assert.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NO_TEC_DATA_IN_FILE_FOR_DATE, oe.getSpecifier());
        }

    }

    @Test
    public void testFieldLaterDate() {
        doTestFieldLaterDate(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestFieldLaterDate(final Field<T> field) {
        final double latitude  = FastMath.toRadians(60.0);
        final double longitude = FastMath.toRadians(-130.0);
        final GeodeticPoint point = new GeodeticPoint(latitude, longitude, 0.0);
        try {
            model.pathDelay(new FieldSpacecraftState<>(field, state), new TopocentricFrame(earth, point, null),
                            Frequency.G01.getMHzFrequency() * 1.0e6,
                            model.getParameters(field));
            Assert.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NO_TEC_DATA_IN_FILE_FOR_DATE, oe.getSpecifier());
        }

    }

    @Test
    /**
     * The goal of this test is to verify if an OrekitException is thrown when latitude or longitude
     * bondaries are not present in the header section of the Global Ionosphere Map.
     */
    public void testIssue621() {
        final String fileName  = "missing-lat-lon-header-gpsg0150.19i";
        final double latitude  = FastMath.toRadians(30.0);
        final double longitude = FastMath.toRadians(-130.0);

        try {
            GlobalIonosphereMapModel corruptedModel = new GlobalIonosphereMapModel(fileName);
            corruptedModel.getTEC(new AbsoluteDate(2019, 1, 15, 0, 0, 0.0, TimeScalesFactory.getUTC()),
                         new GeodeticPoint(latitude, longitude, 0.0));
            Assert.fail("An exception should have been thrown");
            
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NO_LATITUDE_LONGITUDE_BONDARIES_IN_IONEX_HEADER, oe.getSpecifier());
        }
    }

    @Test
    public void testMissingEpochInHeader() {
        final String fileName  = "missing-epoch-header-gpsg0150.19i";
        final double latitude  = FastMath.toRadians(30.0);
        final double longitude = FastMath.toRadians(-130.0);

        try {
            GlobalIonosphereMapModel corruptedModel = new GlobalIonosphereMapModel(fileName);
            corruptedModel.getTEC(new AbsoluteDate(2019, 1, 15, 0, 0, 0.0, TimeScalesFactory.getUTC()),
                         new GeodeticPoint(latitude, longitude, 0.0));
            Assert.fail("An exception should have been thrown");
            
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NO_EPOCH_IN_IONEX_HEADER, oe.getSpecifier());
        }

    }

}
