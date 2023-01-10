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
package org.orekit.time;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.SatelliteSystem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class GNSSDateTest {

    @Test
    public void testFromWeekAndSecondsGPS() {
        final DateComponents date = new DateComponents(2006, 8, 9);
        final TimeComponents time = new TimeComponents(16, 31, 3.0);
        doTestFromWeekAndSeconds(SatelliteSystem.GPS, date, time, 1387, 318677.0);
    }

    @Test
    public void testFromWeekAndSecondsGalileo() {
        final DateComponents date = new DateComponents(2006, 8, 9);
        final TimeComponents time = new TimeComponents(16, 31, 3.0);
        doTestFromWeekAndSeconds(SatelliteSystem.GALILEO, date, time, 363, 318677.0);
    }

    @Test
    public void testFromWeekAndSecondsQZSS() {
        final DateComponents date = new DateComponents(2006, 8, 9);
        final TimeComponents time = new TimeComponents(16, 31, 3.0);
        doTestFromWeekAndSeconds(SatelliteSystem.QZSS, date, time, 1387, 318677.0);
    }

    @Test
    public void testFromWeekAndSecondsBeidou() {
        final DateComponents date = new DateComponents(2010, 2, 26);
        final TimeComponents time = new TimeComponents(23, 15, 12);
        doTestFromWeekAndSeconds(SatelliteSystem.BEIDOU, date, time, 216, 515713.0);
    }

    @Test
    public void testFromWeekAndSecondsIRNSS() {
        final DateComponents date = new DateComponents(2006, 8, 9);
        final TimeComponents time = new TimeComponents(16, 31, 3.0);
        doTestFromWeekAndSeconds(SatelliteSystem.IRNSS, date, time, 363, 318677.0);
    }

    @Test
    public void testFromWeekAndSecondsSBAS() {
        final DateComponents date = new DateComponents(2006, 8, 9);
        final TimeComponents time = new TimeComponents(16, 31, 3.0);
        doTestFromWeekAndSeconds(SatelliteSystem.SBAS, date, time, 1387, 318677.0);
    }

    private void doTestFromWeekAndSeconds(final SatelliteSystem system,
                                        final DateComponents date, final TimeComponents time,
                                        final int refWeek, final double refSeconds) {
        GNSSDate GNSSDate  = new GNSSDate(refWeek, refSeconds, system);
        AbsoluteDate ref  = new AbsoluteDate(date, time, utc);
        Assertions.assertEquals(refWeek, GNSSDate.getWeekNumber());
        Assertions.assertEquals(1000 * refSeconds, GNSSDate.getMilliInWeek(), 1.0e-15);
        Assertions.assertEquals(refSeconds, GNSSDate.getSecondsInWeek(), 1.0e-15);
        Assertions.assertEquals(0, GNSSDate.getDate().durationFrom(ref), 1.0e-15);
    }

    @Test
    public void testFromAbsoluteDateGPS() {
        final DateComponents date = new DateComponents(2006, 8, 9);
        final TimeComponents time = new TimeComponents(16, 31, 3.0);
        doTestFromAbsoluteDate(SatelliteSystem.GPS, date, time, 1387, 318677000.0);
    }

    @Test
    public void testFromAbsoluteDateGalileo() {
        final DateComponents date = new DateComponents(2006, 8, 9);
        final TimeComponents time = new TimeComponents(16, 31, 3.0);
        doTestFromAbsoluteDate(SatelliteSystem.GALILEO, date, time, 363, 318677000.0);
    }

    @Test
    public void testFromAbsoluteDateQZSS() {
        final DateComponents date = new DateComponents(2006, 8, 9);
        final TimeComponents time = new TimeComponents(16, 31, 3.0);
        doTestFromAbsoluteDate(SatelliteSystem.QZSS, date, time, 1387, 318677000.0);
    }

    @Test
    public void testFromAbsoluteDateBeidou() {
        final DateComponents date = new DateComponents(2010, 2, 26);
        final TimeComponents time = new TimeComponents(23, 15, 12.0);
        doTestFromAbsoluteDate(SatelliteSystem.BEIDOU, date, time, 216, 515713000.0);
    }

    @Test
    public void testFromAbsoluteDateIRNSS() {
        final DateComponents date = new DateComponents(2006, 8, 9);
        final TimeComponents time = new TimeComponents(16, 31, 3.0);
        doTestFromAbsoluteDate(SatelliteSystem.IRNSS, date, time, 363, 318677000.0);
    }

    @Test
    public void testFromAbsoluteDateSBAS() {
        final DateComponents date = new DateComponents(2006, 8, 9);
        final TimeComponents time = new TimeComponents(16, 31, 3.0);
        doTestFromAbsoluteDate(SatelliteSystem.SBAS, date, time, 1387, 318677000.0);
    }

    private void doTestFromAbsoluteDate(final SatelliteSystem system,
                                        final DateComponents date, final TimeComponents time,
                                        final int refWeek, final double refMilliSeconds) {
        GNSSDate GNSSDate = new GNSSDate(new AbsoluteDate(date, time, utc), system);
        Assertions.assertEquals(refWeek, GNSSDate.getWeekNumber());
        Assertions.assertEquals(refMilliSeconds, GNSSDate.getMilliInWeek(), 1.0e-15);
    }

    @Test
    public void testZeroGPS() {
        doTestZero(SatelliteSystem.GPS);
    }

    @Test
    public void testZeroGalileo() {
        doTestZero(SatelliteSystem.GALILEO);
    }

    @Test
    public void testZeroQZSS() {
        doTestZero(SatelliteSystem.QZSS);
    }

    @Test
    public void testZeroBeidou() {
        doTestZero(SatelliteSystem.BEIDOU);
    }

    @Test
    public void testZeroIRNSS() {
        doTestZero(SatelliteSystem.IRNSS);
    }

    @Test
    public void testZeroSBAS() {
        doTestZero(SatelliteSystem.SBAS);
    }

    private void doTestZero(final SatelliteSystem system) {
        AbsoluteDate epoch = null;
        switch (system) {
            case GPS:
            case SBAS:
                epoch = AbsoluteDate.GPS_EPOCH;
                break;
            case GALILEO:
                epoch = AbsoluteDate.GALILEO_EPOCH;
                break;
            case QZSS:
                epoch = AbsoluteDate.QZSS_EPOCH;
                break;
            case BEIDOU:
                epoch = AbsoluteDate.BEIDOU_EPOCH;
                break;
            case IRNSS:
                epoch = AbsoluteDate.IRNSS_EPOCH;
                break;
            default:
                break;
        }
        GNSSDate date = new GNSSDate(epoch, system);
        Assertions.assertEquals(0, date.getWeekNumber());
        Assertions.assertEquals(0.0, date.getMilliInWeek(), 1.0e-15);
    }

    @Test
    public void testZeroZeroGPS() {
        GNSSDate.setRolloverReference(new DateComponents(DateComponents.GPS_EPOCH, 7 * 512));
        GNSSDate date1 = new GNSSDate(0, 0.0, SatelliteSystem.GPS);
        Assertions.assertEquals(0.0, date1.getDate().durationFrom(AbsoluteDate.GPS_EPOCH), 1.0e-15);
        GNSSDate.setRolloverReference(new DateComponents(GNSSDate.getRolloverReference(), 1));
        GNSSDate date2 = new GNSSDate(0, 0.0, SatelliteSystem.GPS);
        Assertions.assertEquals(1024, date2.getWeekNumber());
    }

    @Test
    public void testZeroZeroGalileo() {
        GNSSDate.setRolloverReference(new DateComponents(DateComponents.GALILEO_EPOCH, 7 * 2048));
        GNSSDate date1 = new GNSSDate(0, 0.0, SatelliteSystem.GALILEO);
        Assertions.assertEquals(0.0, date1.getDate().durationFrom(AbsoluteDate.GALILEO_EPOCH), 1.0e-15);
        GNSSDate.setRolloverReference(new DateComponents(GNSSDate.getRolloverReference(), 1));
        GNSSDate date2 = new GNSSDate(0, 0.0, SatelliteSystem.GALILEO);
        Assertions.assertEquals(4096, date2.getWeekNumber());
    }

    @Test
    public void testZeroZeroQZSS() {
        GNSSDate.setRolloverReference(new DateComponents(DateComponents.QZSS_EPOCH, 7 * 512));
        GNSSDate date1 = new GNSSDate(0, 0.0, SatelliteSystem.QZSS);
        Assertions.assertEquals(0.0, date1.getDate().durationFrom(AbsoluteDate.QZSS_EPOCH), 1.0e-15);
        GNSSDate.setRolloverReference(new DateComponents(GNSSDate.getRolloverReference(), 1));
        GNSSDate date2 = new GNSSDate(0, 0.0, SatelliteSystem.QZSS);
        Assertions.assertEquals(1024, date2.getWeekNumber());
    }

    @Test
    public void testZeroZeroBeidou() {
        GNSSDate.setRolloverReference(new DateComponents(DateComponents.BEIDOU_EPOCH, 7 * 4096));
        GNSSDate date1 = new GNSSDate(0, 0.0, SatelliteSystem.BEIDOU);
        Assertions.assertEquals(0.0, date1.getDate().durationFrom(AbsoluteDate.BEIDOU_EPOCH), 1.0e-15);
        GNSSDate.setRolloverReference(new DateComponents(GNSSDate.getRolloverReference(), 1));
        GNSSDate date2 = new GNSSDate(0, 0.0, SatelliteSystem.BEIDOU);
        Assertions.assertEquals(8192, date2.getWeekNumber());
    }

    @Test
    public void testZeroZeroIRNSS() {
        GNSSDate.setRolloverReference(new DateComponents(DateComponents.IRNSS_EPOCH, 7 * 512));
        GNSSDate date1 = new GNSSDate(0, 0.0, SatelliteSystem.IRNSS);
        Assertions.assertEquals(0.0, date1.getDate().durationFrom(AbsoluteDate.IRNSS_EPOCH), 1.0e-15);
        GNSSDate.setRolloverReference(new DateComponents(GNSSDate.getRolloverReference(), 1));
        GNSSDate date2 = new GNSSDate(0, 0.0, SatelliteSystem.IRNSS);
        Assertions.assertEquals(1024, date2.getWeekNumber());
    }

    @Test
    public void testZeroZeroSBAS() {
        GNSSDate.setRolloverReference(new DateComponents(DateComponents.GPS_EPOCH, 7 * 512));
        GNSSDate date1 = new GNSSDate(0, 0.0, SatelliteSystem.SBAS);
        Assertions.assertEquals(0.0, date1.getDate().durationFrom(AbsoluteDate.GPS_EPOCH), 1.0e-15);
        GNSSDate.setRolloverReference(new DateComponents(GNSSDate.getRolloverReference(), 1));
        GNSSDate date2 = new GNSSDate(0, 0.0, SatelliteSystem.SBAS);
        Assertions.assertEquals(1024, date2.getWeekNumber());
    }

    @Test
    public void testSerializationGPS() throws ClassNotFoundException, IOException {
        final DateComponents date = new DateComponents(2006, 8, 9);
        final TimeComponents time = new TimeComponents(16, 31, 3.0);
        doTestSerialization(SatelliteSystem.GPS, date, time, 1387, 318677.0);
    }

    @Test
    public void testSerializationGalileo() throws ClassNotFoundException, IOException {
        final DateComponents date = new DateComponents(2006, 8, 9);
        final TimeComponents time = new TimeComponents(16, 31, 3.0);
        doTestSerialization(SatelliteSystem.GALILEO, date, time, 363, 318677.0);
    }

    @Test
    public void testSerializationQZSS() throws ClassNotFoundException, IOException {
        final DateComponents date = new DateComponents(2006, 8, 9);
        final TimeComponents time = new TimeComponents(16, 31, 3.0);
        doTestSerialization(SatelliteSystem.QZSS, date, time, 1387, 318677.0);
    }

    @Test
    public void testSerializationBeidou() throws ClassNotFoundException, IOException {
        final DateComponents date = new DateComponents(2010, 2, 26);
        final TimeComponents time = new TimeComponents(23, 15, 12.0);
        doTestSerialization(SatelliteSystem.BEIDOU, date, time, 216, 515713.0);
    }

    @Test
    public void testSerializationIRNSS() throws ClassNotFoundException, IOException {
        final DateComponents date = new DateComponents(2006, 8, 9);
        final TimeComponents time = new TimeComponents(16, 31, 3.0);
        doTestSerialization(SatelliteSystem.IRNSS, date, time, 363, 318677.0);
    }

    @Test
    public void testSerializationSBAS() throws ClassNotFoundException, IOException {
        final DateComponents date = new DateComponents(2006, 8, 9);
        final TimeComponents time = new TimeComponents(16, 31, 3.0);
        doTestSerialization(SatelliteSystem.SBAS, date, time, 1387, 318677.0);
    }

    @Test
    public void testDefaultRolloverReference() {
        Assertions.assertNull(GNSSDate.getRolloverReference());
        GNSSDate date = new GNSSDate(305, 1.5e-3, SatelliteSystem.GPS);
        // the default reference is extracted from last EOP entry
        // which in this test comes from bulletin B 218, in the final values section
        Assertions.assertEquals("2006-03-05", GNSSDate.getRolloverReference().toString());
        Assertions.assertEquals(305 + 1024, date.getWeekNumber());
    }

    @Test
    public void testUserRolloverReference() {
        GNSSDate.setRolloverReference(new DateComponents(DateComponents.GPS_EPOCH, 7 * (3 * 1024 + 512)));
        GNSSDate date = new GNSSDate(305, 1.5e-3, SatelliteSystem.GPS);
        Assertions.assertEquals("2048-09-13", GNSSDate.getRolloverReference().toString());
        Assertions.assertEquals(305 + 3 * 1024, date.getWeekNumber());
    }

    private void doTestSerialization(final SatelliteSystem system,
                                     final DateComponents date, final TimeComponents time,
                                     final int refWeek, final double refSeconds)
        throws IOException, ClassNotFoundException {
        GNSSDate GNSSDate = new GNSSDate(refWeek, refSeconds, system);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(GNSSDate);

        Assertions.assertTrue(bos.size() > 230);
        Assertions.assertTrue(bos.size() < 240);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        GNSSDate deserialized  = (GNSSDate) ois.readObject();
        AbsoluteDate ref  = new AbsoluteDate(date, time, utc);
        Assertions.assertEquals(refWeek, deserialized.getWeekNumber());
        Assertions.assertEquals(1000 * refSeconds, deserialized.getMilliInWeek(), 1.0e-15);
        Assertions.assertEquals(refSeconds, deserialized.getSecondsInWeek(), 1.0e-15);
        Assertions.assertEquals(0, deserialized.getDate().durationFrom(ref), 1.0e-15);

    }

    @Test
    public void testBadSatelliteSystem() {
        try {
            @SuppressWarnings("unused")
            GNSSDate date = new GNSSDate(new AbsoluteDate(), SatelliteSystem.GLONASS);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INVALID_SATELLITE_SYSTEM, oe.getSpecifier());
        }
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        utc = TimeScalesFactory.getUTC();
    }

    private TimeScale utc;

}
