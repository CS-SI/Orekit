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
package org.orekit.attitudes;

import java.io.InputStream;
import java.util.Collections;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.util.Decimal64Field;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.AEMFile;
import org.orekit.files.ccsds.AEMFile.AemSatelliteEphemeris;
import org.orekit.files.ccsds.AEMParser;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;

public class AggregateBoundedAttitudeProviderTest {

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data:ccsds");
    }

    @Test
    public void testEmptyList() {
        try {
            new AggregateBoundedAttitudeProvider(Collections.emptyList());
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NOT_ENOUGH_ATTITUDE_PROVIDERS, oe.getSpecifier());
        }
    }

    @Test
    public void testAEM() {

        final String ex = "/ccsds/AEMExample10.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser().withMu(CelestialBodyFactory.getEarth().getGM()).
                        withConventions(IERSConventions.IERS_2010).
                        withSimpleEOP(true);
        final AEMFile file = parser.parse(inEntry, "AEMExample10.txt");

        final AemSatelliteEphemeris ephemeris = file.getSatellites().get("1996-062A");
        final BoundedAttitudeProvider provider = ephemeris.getAttitudeProvider();

        // Verify dates
        Assert.assertEquals(0.0, provider.getMinDate().durationFrom(ephemeris.getStart()), 1.0e-10);
        Assert.assertEquals(0.0, provider.getMaxDate().durationFrom(ephemeris.getStop()),  1.0e-10);
        Assert.assertEquals(0.0, provider.getMinDate().durationFrom(ephemeris.getSegments().get(0).getStart()), 1.0e-10);
        Assert.assertEquals(0.0, provider.getMaxDate().durationFrom(ephemeris.getSegments().get(1).getStop()), 1.0e-10);

        // Verify computation with data in first segment
        Attitude attitude = provider.getAttitude(null, new AbsoluteDate("1996-11-28T22:08:04.555", TimeScalesFactory.getUTC()), null);
        Rotation rotation = attitude.getRotation();
        Assert.assertEquals(0.45652, rotation.getQ0(), 0.00001);
        Assert.assertEquals(-0.84532, rotation.getQ1(), 0.00001);
        Assert.assertEquals(0.26974, rotation.getQ2(), 0.00001);
        Assert.assertEquals(-0.06532, rotation.getQ3(), 0.00001);

    }

    @Test
    public void testFieldAEM() {
        doTestFieldAEM(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestFieldAEM(final Field<T> field) {

        final String ex = "/ccsds/AEMExample10.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser().withMu(CelestialBodyFactory.getEarth().getGM()).
                        withConventions(IERSConventions.IERS_2010).
                        withSimpleEOP(true);
        final AEMFile file = parser.parse(inEntry, "AEMExample10.txt");

        final AemSatelliteEphemeris ephemeris = file.getSatellites().get("1996-062A");
        final BoundedAttitudeProvider provider = ephemeris.getAttitudeProvider();

        // Verify dates
        Assert.assertEquals(0.0, provider.getMinDate().durationFrom(ephemeris.getStart()), 1.0e-10);
        Assert.assertEquals(0.0, provider.getMaxDate().durationFrom(ephemeris.getStop()),  1.0e-10);
        Assert.assertEquals(0.0, provider.getMinDate().durationFrom(ephemeris.getSegments().get(0).getStart()), 1.0e-10);
        Assert.assertEquals(0.0, provider.getMaxDate().durationFrom(ephemeris.getSegments().get(1).getStop()), 1.0e-10);

        // Verify computation with data in first segment
        FieldAttitude<T> attitude = provider.getAttitude(null, new FieldAbsoluteDate<>(new AbsoluteDate("1996-11-28T22:08:04.555", TimeScalesFactory.getUTC()), field.getZero()), null);
        FieldRotation<T> rotation = attitude.getRotation();
        Assert.assertEquals(0.45652, rotation.getQ0().getReal(), 0.00001);
        Assert.assertEquals(-0.84532, rotation.getQ1().getReal(), 0.00001);
        Assert.assertEquals(0.26974, rotation.getQ2().getReal(), 0.00001);
        Assert.assertEquals(-0.06532, rotation.getQ3().getReal(), 0.00001);

    }

    @Test
    public void testOutsideBounds() throws Exception {

        final String ex = "/ccsds/AEMExample10.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser().withMu(CelestialBodyFactory.getEarth().getGM()).
                        withConventions(IERSConventions.IERS_2010).
                        withSimpleEOP(true);
        final AEMFile file = parser.parse(inEntry, "AEMExample10.txt");

        final AemSatelliteEphemeris ephemeris = file.getSatellites().get("1996-062A");
        final BoundedAttitudeProvider provider = ephemeris.getAttitudeProvider();
        
        // before bound of first attitude provider
        try {
            provider.getAttitude(null, provider.getMinDate().shiftedBy(-60.0), null);
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE, oe.getSpecifier());
        }

        // after bound of last attitude provider
        try {
            provider.getAttitude(null, provider.getMaxDate().shiftedBy(60.0), null);
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_AFTER, oe.getSpecifier());
        }

    }

    @Test
    public void testFieldOutsideBounds() throws Exception {
        doTestFieldOutsideBounds(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestFieldOutsideBounds(final Field<T> field) throws Exception {

        final String ex = "/ccsds/AEMExample10.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final AEMParser parser = new AEMParser().withMu(CelestialBodyFactory.getEarth().getGM()).
                        withConventions(IERSConventions.IERS_2010).
                        withSimpleEOP(true);
        final AEMFile file = parser.parse(inEntry, "AEMExample10.txt");

        final AemSatelliteEphemeris ephemeris = file.getSatellites().get("1996-062A");
        final BoundedAttitudeProvider provider = ephemeris.getAttitudeProvider();
        
        // before bound of first attitude provider
        try {
            provider.getAttitude(null, new FieldAbsoluteDate<>(provider.getMinDate(), field.getZero().subtract(60.0)), null);
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE, oe.getSpecifier());
        }

        // after bound of last attitude provider
        try {
            provider.getAttitude(null, new FieldAbsoluteDate<>(provider.getMinDate(), field.getZero().add(60.0)), null);
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_AFTER, oe.getSpecifier());
        }

    }

    
}
