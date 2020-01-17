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
package org.orekit.time;


import java.io.IOException;
import java.util.Date;
import java.util.TimeZone;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.Constants;

public class FieldAbsoluteDateTest {

    private TimeScale utc;

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        utc = TimeScalesFactory.getUTC();
    }

    @Test
    public void testStandardEpoch() {
        doTestStandardEpoch(Decimal64Field.getInstance());
    }

    @Test
    public void testStandardEpochStrings() {
        doTestStandardEpochStrings(Decimal64Field.getInstance());
    }

    @Test
    public void testJulianEpochRate() {
        doTestJulianEpochRate(Decimal64Field.getInstance());
    }

    @Test
    public void testBesselianEpochRate() {
        doTestBesselianEpochRate(Decimal64Field.getInstance());
    }

    @Test
    public void testLieske() {
        doTestLieske(Decimal64Field.getInstance());
    }

    @Test
    public void testParse() {
        doTestParse(Decimal64Field.getInstance());
    }

    @Test
    public void testLocalTimeParsing() {
        doTestLocalTimeParsing(Decimal64Field.getInstance());
    }

    @Test
    public void testTimeZoneDisplay() {
        doTestTimeZoneDisplay(Decimal64Field.getInstance());
    }

    @Test
    public void testLocalTimeLeapSecond() throws IOException {
        doTestLocalTimeLeapSecond(Decimal64Field.getInstance());
    }

    @Test
    public void testTimeZoneLeapSecond() {
        doTestTimeZoneLeapSecond(Decimal64Field.getInstance());
    }

    @Test
    public void testParseLeap() {
        doTestParseLeap(Decimal64Field.getInstance());
    }

    @Test
    public void testOutput() {
        doTestOutput(Decimal64Field.getInstance());
    }

    @Test
    public void testJ2000() {
        doTestJ2000(Decimal64Field.getInstance());
    }

    @Test
    public void testFraction() {
        doTestFraction(Decimal64Field.getInstance());
    }

    @Test
    public void testScalesOffset() {
        doTestScalesOffset(Decimal64Field.getInstance());
    }

    @Test
    public void testUTC() {
        doTestUTC(Decimal64Field.getInstance());
    }

    @Test
    public void test1970() {
        doTest1970(Decimal64Field.getInstance());
    }

    @Test
    public void testUtcGpsOffset() {
        doTestUtcGpsOffset(Decimal64Field.getInstance());
    }

    @Test
    public void testGpsDate() {
        doTestGpsDate(Decimal64Field.getInstance());
    }

    @Test
    public void testMJDDate() {
        doTestMJDDate(Decimal64Field.getInstance());
    }

    @Test
    public void testJDDate() {
        doTestJDDate(Decimal64Field.getInstance());
    }

    @Test
    public void testOffsets() {
        doTestOffsets(Decimal64Field.getInstance());
    }

    @Test
    public void testBeforeAndAfterLeap() {
        doTestBeforeAndAfterLeap(Decimal64Field.getInstance());
    }

    @Test
    public void testSymmetry() {
        doTestSymmetry(Decimal64Field.getInstance());
    }

    @Test
    public void testEquals() {
        doTestEquals(Decimal64Field.getInstance());
    }

    @Test
    public void testIsEqualTo() { doTestIsEqualTo(Decimal64Field.getInstance()); }

    @Test
    public void testIsCloseTo() { doTestIsCloseTo(Decimal64Field.getInstance()); }

    @Test
    public void testIsBefore() { doTestIsBefore(Decimal64Field.getInstance()); }

    @Test
    public void testIsAfter() { doTestIsAfter(Decimal64Field.getInstance()); }

    @Test
    public void testIsBeforeOrEqualTo() { doTestIsBeforeOrEqualTo(Decimal64Field.getInstance()); }

    @Test
    public void testIsAfterOrEqualTo() { doTestIsAfterOrEqualTo(Decimal64Field.getInstance()); }

    @Test
    public void testIsBetween() { doTestIsBetween(Decimal64Field.getInstance()); }

    @Test
    public void testIsBetweenOrEqualTo() { doTestIsBetweenOrEqualTo(Decimal64Field.getInstance()); }

    @Test
    public void testComponents() {
        doTestComponents(Decimal64Field.getInstance());
    }

    @Test
    public void testMonth() {
        doTestMonth(Decimal64Field.getInstance());
    }

    @Test
    public void testCCSDSUnsegmentedNoExtension() {
        doTestCCSDSUnsegmentedNoExtension(Decimal64Field.getInstance());
    }

    @Test
    public void testCCSDSUnsegmentedWithExtendedPreamble() {
        doTestCCSDSUnsegmentedWithExtendedPreamble(Decimal64Field.getInstance());
    }

    @Test
    public void testCCSDSDaySegmented() {
        doTestCCSDSDaySegmented(Decimal64Field.getInstance());
    }

    @Test
    public void testCCSDSCalendarSegmented() {
        doTestCCSDSCalendarSegmented(Decimal64Field.getInstance());
    }

    @Test
    public void testExpandedConstructors() {
        doTestExpandedConstructors(Decimal64Field.getInstance());
    }

    @Test
    public void testHashcode() {
        doTestHashcode(Decimal64Field.getInstance());
    }

    @Test
    public void testInfinity() {
        doTestInfinity(Decimal64Field.getInstance());
    }

    @Test
    public void testAccuracy() {
        doTestAccuracy(Decimal64Field.getInstance());
    }

    @Test
    public void testAccuracyIssue348() {
        doTestAccuracyIssue348(Decimal64Field.getInstance());
    }

    @Test
    public void testIterationAccuracy() {
        doTestIterationAccuracy(Decimal64Field.getInstance());
    }

    @Test
    public void testIssue142() {
        doTestIssue142(Decimal64Field.getInstance());
    }

    @Test
    public void testIssue148() {
        doTestIssue148(Decimal64Field.getInstance());
    }

    @Test
    public void testIssue149() {
        doTestIssue149(Decimal64Field.getInstance());
    }

    @Test
    public void testWrapAtMinuteEnd() {
        doTestWrapAtMinuteEnd(Decimal64Field.getInstance());
    }

    @Test
    public void testIssue508() {
        doTestIssue508(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestStandardEpoch(final Field<T> field) {

        TimeScale tai = TimeScalesFactory.getTAI();
        TimeScale tt  = TimeScalesFactory.getTT();

        FieldAbsoluteDate<T> JuEp  = FieldAbsoluteDate.getJulianEpoch(field);
        FieldAbsoluteDate<T> MJuEp = FieldAbsoluteDate.getModifiedJulianEpoch(field);
        FieldAbsoluteDate<T> FiEp  = FieldAbsoluteDate.getFiftiesEpoch(field);
        FieldAbsoluteDate<T> CCSDS = FieldAbsoluteDate.getCCSDSEpoch(field);
        FieldAbsoluteDate<T> GaEp  = FieldAbsoluteDate.getGalileoEpoch(field);
        FieldAbsoluteDate<T> GPSEp = FieldAbsoluteDate.getGPSEpoch(field);
        FieldAbsoluteDate<T> JTTEP = FieldAbsoluteDate.getJ2000Epoch(field);

        Assert.assertEquals(-210866760000000l, JuEp.toDate(tt).getTime());
        Assert.assertEquals(-3506716800000l, MJuEp.toDate(tt).getTime());
        Assert.assertEquals(-631152000000l, FiEp.toDate(tt).getTime());
        Assert.assertEquals(-378691200000l, CCSDS.toDate(tai).getTime());
        Assert.assertEquals(935280019000l,  GaEp.toDate(tai).getTime());
        Assert.assertEquals(315964819000l,  GPSEp.toDate(tai).getTime());
        Assert.assertEquals(946728000000l,  JTTEP.toDate(tt).getTime());

    }

    private <T extends RealFieldElement<T>> void doTestStandardEpochStrings(final Field<T> field) {

        Assert.assertEquals("-4712-01-01T12:00:00.000",
                            FieldAbsoluteDate.getJulianEpoch(field).toString(TimeScalesFactory.getTT()));
        Assert.assertEquals("1858-11-17T00:00:00.000",
                            FieldAbsoluteDate.getModifiedJulianEpoch(field).toString(TimeScalesFactory.getTT()));
        Assert.assertEquals("1950-01-01T00:00:00.000",
                            FieldAbsoluteDate.getFiftiesEpoch(field).toString(TimeScalesFactory.getTT()));
        Assert.assertEquals("1958-01-01T00:00:00.000",
                            FieldAbsoluteDate.getCCSDSEpoch(field).toString(TimeScalesFactory.getTAI()));
        Assert.assertEquals("1999-08-21T23:59:47.000",
                            FieldAbsoluteDate.getGalileoEpoch(field).toString(TimeScalesFactory.getUTC()));
        Assert.assertEquals("1980-01-06T00:00:00.000",
                            FieldAbsoluteDate.getGPSEpoch(field).toString(TimeScalesFactory.getUTC()));
        Assert.assertEquals("2000-01-01T12:00:00.000",
                            FieldAbsoluteDate.getJ2000Epoch(field).toString(TimeScalesFactory.getTT()));
        Assert.assertEquals("1970-01-01T00:00:00.000",
                            FieldAbsoluteDate.getJavaEpoch(field).toString(TimeScalesFactory.getUTC()));
    }

    private <T extends RealFieldElement<T>> void doTestJulianEpochRate(final Field<T> field) {

        for (int i = 0; i < 10; ++i) {
            FieldAbsoluteDate<T> j200i = FieldAbsoluteDate.createJulianEpoch(field.getZero().add(2000.0+i));
            FieldAbsoluteDate<T> j2000 = FieldAbsoluteDate.getJ2000Epoch(field);
            double expected    = i * Constants.JULIAN_YEAR;
            Assert.assertEquals(expected, j200i.durationFrom(j2000).getReal(), 4.0e-15 * expected);
        }

    }

    private <T extends RealFieldElement<T>> void doTestBesselianEpochRate(final Field<T> field) {

        for (int i = 0; i < 10; ++i) {
            FieldAbsoluteDate<T> b195i = FieldAbsoluteDate.createBesselianEpoch(field.getZero().add(1950.0 + i));
            FieldAbsoluteDate<T> b1950 = FieldAbsoluteDate.createBesselianEpoch(field.getZero().add(1950.0));
            double expected    = i * Constants.BESSELIAN_YEAR;
            Assert.assertEquals(expected, b195i.durationFrom(b1950).getReal(), 4.0e-15 * expected);
        }

    }

    private <T extends RealFieldElement<T>> void doTestLieske(final Field<T> field) {

        // the following test values correspond to table 1 in the paper:
        // Precession Matrix Based on IAU (1976) System of Astronomical Constants,
        // Jay H. Lieske, Astronomy and Astrophysics, vol. 73, no. 3, Mar. 1979, p. 282-284
        // http://articles.adsabs.harvard.edu/cgi-bin/nph-iarticle_query?1979A%26A....73..282L&defaultprint=YES&filetype=.pdf

        // published table, with limited accuracy


        final double publishedEpsilon = 1.0e-6 * Constants.JULIAN_YEAR;
        checkEpochs(field, 1899.999142, 1900.000000, publishedEpsilon);
        checkEpochs(field, 1900.000000, 1900.000858, publishedEpsilon);
        checkEpochs(field, 1950.000000, 1949.999790, publishedEpsilon);
        checkEpochs(field, 1950.000210, 1950.000000, publishedEpsilon);
        checkEpochs(field, 2000.000000, 1999.998722, publishedEpsilon);
        checkEpochs(field, 2000.001278, 2000.000000, publishedEpsilon);

        // recomputed table, using directly Lieske formulas (i.e. *not* Orekit implementation) with high accuracy
        final double accurateEpsilon = 1.2e-13 * Constants.JULIAN_YEAR;
        checkEpochs(field, 1899.99914161068724704, 1900.00000000000000000, accurateEpsilon);
        checkEpochs(field, 1900.00000000000000000, 1900.00085837097878165, accurateEpsilon);
        checkEpochs(field, 1950.00000000000000000, 1949.99979044229979466, accurateEpsilon);
        checkEpochs(field, 1950.00020956217615449, 1950.00000000000000000, accurateEpsilon);
        checkEpochs(field, 2000.00000000000000000, 1999.99872251362080766, accurateEpsilon);
        checkEpochs(field, 2000.00127751366506194, 2000.00000000000000000, accurateEpsilon);

    }

    private <T extends RealFieldElement<T>> void checkEpochs(final Field<T> field, final double besselianEpoch, final double julianEpoch, final double epsilon) {
        final FieldAbsoluteDate<T> b = FieldAbsoluteDate.createBesselianEpoch(field.getZero().add(besselianEpoch));
        final FieldAbsoluteDate<T> j = FieldAbsoluteDate.createJulianEpoch(field.getZero().add(julianEpoch));
        Assert.assertEquals(0.0, b.durationFrom(j).getReal(), epsilon);
    }

    private <T extends RealFieldElement<T>> void doTestParse(final Field<T> field) {

        Assert.assertEquals(FieldAbsoluteDate.getModifiedJulianEpoch(field),
                            new FieldAbsoluteDate<>(field, "1858-W46-3", TimeScalesFactory.getTT()));
        Assert.assertEquals(FieldAbsoluteDate.getJulianEpoch(field),
                            new FieldAbsoluteDate<>(field, "-4712-01-01T12:00:00.000", TimeScalesFactory.getTT()));
        Assert.assertEquals(FieldAbsoluteDate.getFiftiesEpoch(field),
                            new FieldAbsoluteDate<>(field, "1950-01-01", TimeScalesFactory.getTT()));
        Assert.assertEquals(FieldAbsoluteDate.getCCSDSEpoch(field),
                            new FieldAbsoluteDate<>(field, "1958-001", TimeScalesFactory.getTAI()));
    }

    private <T extends RealFieldElement<T>> void doTestLocalTimeParsing(final Field<T> field) {
        TimeScale utc = TimeScalesFactory.getUTC();
        Assert.assertEquals(new FieldAbsoluteDate<>(field, "2011-12-31T23:00:00",       utc),
                            new FieldAbsoluteDate<>(field, "2012-01-01T03:30:00+04:30", utc));
        Assert.assertEquals(new FieldAbsoluteDate<>(field, "2011-12-31T23:00:00",       utc),
                            new FieldAbsoluteDate<>(field, "2012-01-01T03:30:00+0430",  utc));
        Assert.assertEquals(new FieldAbsoluteDate<>(field, "2011-12-31T23:30:00",       utc),
                            new FieldAbsoluteDate<>(field, "2012-01-01T03:30:00+04",    utc));
        Assert.assertEquals(new FieldAbsoluteDate<>(field, "2012-01-01T05:17:00",       utc),
                            new FieldAbsoluteDate<>(field, "2011-12-31T22:17:00-07:00", utc));
        Assert.assertEquals(new FieldAbsoluteDate<>(field, "2012-01-01T05:17:00",       utc),
                            new FieldAbsoluteDate<>(field, "2011-12-31T22:17:00-0700",  utc));
        Assert.assertEquals(new FieldAbsoluteDate<>(field, "2012-01-01T05:17:00",       utc),
                            new FieldAbsoluteDate<>(field, "2011-12-31T22:17:00-07",    utc));
    }

    private <T extends RealFieldElement<T>> void doTestTimeZoneDisplay(final Field<T> field) {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, "2000-01-01T01:01:01.000", utc);
        Assert.assertEquals("2000-01-01T01:01:01.000",       date.toString());
        Assert.assertEquals("2000-01-01T11:01:01.000+10:00", date.toString( 600));
        Assert.assertEquals("1999-12-31T23:01:01.000-02:00", date.toString(-120));

        // winter time, Europe is one hour ahead of UTC
        final TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        Assert.assertEquals("2001-01-22T11:30:00.000+01:00",
                            new FieldAbsoluteDate<>(field, "2001-01-22T10:30:00", utc).toString(tz));

        // summer time, Europe is two hours ahead of UTC
        Assert.assertEquals("2001-06-23T11:30:00.000+02:00",
                            new FieldAbsoluteDate<>(field, "2001-06-23T09:30:00", utc).toString(tz));

    }

    private <T extends RealFieldElement<T>> void doTestLocalTimeLeapSecond(final Field<T> field) throws IOException {

        TimeScale utc = TimeScalesFactory.getUTC();
        FieldAbsoluteDate<T> beforeLeap = new FieldAbsoluteDate<>(field, "2012-06-30T23:59:59.8", utc);
        FieldAbsoluteDate<T> inLeap     = new FieldAbsoluteDate<>(field, "2012-06-30T23:59:60.5", utc);
        Assert.assertEquals(0.7, inLeap.durationFrom(beforeLeap).getReal(), 1.0e-12);
       for (int minutesFromUTC = -1500; minutesFromUTC < -1499; ++minutesFromUTC) {
            DateTimeComponents dtcBeforeLeap = beforeLeap.getComponents(minutesFromUTC);
            DateTimeComponents dtcInsideLeap = inLeap.getComponents(minutesFromUTC);


            Assert.assertEquals(dtcBeforeLeap.getDate(), dtcInsideLeap.getDate());

            Assert.assertEquals(dtcBeforeLeap.getTime().getHour(), dtcInsideLeap.getTime().getHour());
            Assert.assertEquals(dtcBeforeLeap.getTime().getMinute(), dtcInsideLeap.getTime().getMinute());
            Assert.assertEquals(minutesFromUTC, dtcBeforeLeap.getTime().getMinutesFromUTC());
            Assert.assertEquals(minutesFromUTC, dtcInsideLeap.getTime().getMinutesFromUTC());
            Assert.assertEquals(59.8, dtcBeforeLeap.getTime().getSecond(), 1.0e-10);
            Assert.assertEquals(60.5, dtcInsideLeap.getTime().getSecond(), 1.0e-10);
        }

    }

    private <T extends RealFieldElement<T>> void doTestTimeZoneLeapSecond(final Field<T> field) {

        TimeScale utc = TimeScalesFactory.getUTC();
        final TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        FieldAbsoluteDate<T> localBeforeMidnight = new FieldAbsoluteDate<>(field, "2012-06-30T21:59:59.800", utc);
        Assert.assertEquals("2012-06-30T23:59:59.800+02:00",
                            localBeforeMidnight.toString(tz));
        Assert.assertEquals("2012-07-01T00:00:00.800+02:00",
                            localBeforeMidnight.shiftedBy(1.0).toString(tz));

        FieldAbsoluteDate<T> beforeLeap = new FieldAbsoluteDate<>(field, "2012-06-30T23:59:59.8", utc);
        FieldAbsoluteDate<T> inLeap     = new FieldAbsoluteDate<>(field, "2012-06-30T23:59:60.5", utc);
        Assert.assertEquals(0.7, inLeap.durationFrom(beforeLeap).getReal(), 1.0e-12);
        Assert.assertEquals("2012-07-01T01:59:59.800+02:00", beforeLeap.toString(tz));
        Assert.assertEquals("2012-07-01T01:59:60.500+02:00", inLeap.toString(tz));

    }

    private <T extends RealFieldElement<T>> void doTestParseLeap(final Field<T> field) {
        TimeScale utc = TimeScalesFactory.getUTC();
        FieldAbsoluteDate<T> beforeLeap = new FieldAbsoluteDate<>(field, "2012-06-30T23:59:59.8", utc);
        FieldAbsoluteDate<T> inLeap     = new FieldAbsoluteDate<>(field, "2012-06-30T23:59:60.5", utc);
        Assert.assertEquals(0.7, inLeap.durationFrom(beforeLeap).getReal(), 1.0e-12);
        Assert.assertEquals("2012-06-30T23:59:60.500", inLeap.toString(utc));
    }

    private <T extends RealFieldElement<T>> void doTestOutput(final Field<T> field) {
        TimeScale tt = TimeScalesFactory.getTT();
        Assert.assertEquals("1950-01-01T01:01:01.000",
                            FieldAbsoluteDate.getFiftiesEpoch(field).shiftedBy(3661.0).toString(tt));
        Assert.assertEquals("2000-01-01T13:01:01.000",
                            FieldAbsoluteDate.getJ2000Epoch(field).shiftedBy(3661.0).toString(tt));
    }

    private <T extends RealFieldElement<T>> void doTestJ2000(final Field<T> field) {
        FieldAbsoluteDate<T> FAD = new FieldAbsoluteDate<>(field);
        Assert.assertEquals("2000-01-01T12:00:00.000",
                            FAD.toString(TimeScalesFactory.getTT()));
        Assert.assertEquals("2000-01-01T11:59:27.816",
                            FAD.toString(TimeScalesFactory.getTAI()));
        Assert.assertEquals("2000-01-01T11:58:55.816",
                            FAD.toString(utc));
        Assert.assertEquals("2000-01-01T12:00:00.000",
                            FieldAbsoluteDate.getJ2000Epoch(field).toString(TimeScalesFactory.getTT()));
        Assert.assertEquals("2000-01-01T11:59:27.816",
                            FieldAbsoluteDate.getJ2000Epoch(field).toString(TimeScalesFactory.getTAI()));
        Assert.assertEquals("2000-01-01T11:58:55.816",
                            FieldAbsoluteDate.getJ2000Epoch(field).toString(utc));
    }

    private <T extends RealFieldElement<T>> void doTestFraction(final Field<T> field) {
        FieldAbsoluteDate<T> d =
            new FieldAbsoluteDate<>(field, new DateComponents(2000, 01, 01), new TimeComponents(11, 59, 27.816),
                             TimeScalesFactory.getTAI());
        Assert.assertEquals(0, d.durationFrom(FieldAbsoluteDate.getJ2000Epoch(field)).getReal(), 1.0e-10);
    }

    private <T extends RealFieldElement<T>> void doTestScalesOffset(final Field<T> field) {
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, new DateComponents(2006, 02, 24),
                                                            new TimeComponents(15, 38, 00),
                                                            utc);
        Assert.assertEquals(33,
                            date.timeScalesOffset(TimeScalesFactory.getTAI(), utc).getReal(),
                            1.0e-10);
    }

    private <T extends RealFieldElement<T>> void doTestUTC(final Field<T> field) {
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, new DateComponents(2002, 01, 01),
                                                            new TimeComponents(00, 00, 01),
                                                            utc);
        Assert.assertEquals("2002-01-01T00:00:01.000", date.toString());
    }

    private <T extends RealFieldElement<T>> void doTest1970(final Field<T> field) {
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, new Date(0l), utc);
        Assert.assertEquals("1970-01-01T00:00:00.000", date.toString());
    }

    private <T extends RealFieldElement<T>> void doTestUtcGpsOffset(final Field<T> field) {
        FieldAbsoluteDate<T> date1   = new FieldAbsoluteDate<>(field, new DateComponents(2005, 8, 9),
                                                               new TimeComponents(16, 31, 17),
                                                               utc);
        FieldAbsoluteDate<T> date2   = new FieldAbsoluteDate<>(field, new DateComponents(2006, 8, 9),
                                                               new TimeComponents(16, 31, 17),
                                                               utc);
        FieldAbsoluteDate<T> dateRef = new FieldAbsoluteDate<>(field, new DateComponents(1980, 1, 6),
                                                               TimeComponents.H00,
                                                               utc);

        // 13 seconds offset between GPS time and UTC in 2005
        long noLeapGap = ((9347 * 24 + 16) * 60 + 31) * 60 + 17;
        long realGap   = (long) date1.durationFrom(dateRef).getReal();
        Assert.assertEquals(13l, realGap - noLeapGap);

        // 14 seconds offset between GPS time and UTC in 2006
        noLeapGap = ((9712 * 24 + 16) * 60 + 31) * 60 + 17;
        realGap   = (long) date2.durationFrom(dateRef).getReal();
        Assert.assertEquals(14l, realGap - noLeapGap);

    }

    private <T extends RealFieldElement<T>> void doTestGpsDate(final Field<T> field) {
        FieldAbsoluteDate<T> date = FieldAbsoluteDate.createGPSDate(1387, field.getZero().add(318677000.0));
        FieldAbsoluteDate<T> ref  = new FieldAbsoluteDate<>(field, new DateComponents(2006, 8, 9),
                                                            new TimeComponents(16, 31, 03),
                                                            utc);
        Assert.assertEquals(0, date.durationFrom(ref).getReal(), 1.0e-15);
    }

    private <T extends RealFieldElement<T>> void doTestMJDDate(final Field<T> field) {
        FieldAbsoluteDate<T> dateA = FieldAbsoluteDate.createMJDDate(51544, field.getZero().add(0.5 * Constants.JULIAN_DAY),
                                                                     TimeScalesFactory.getTT());
        Assert.assertEquals(0.0, FieldAbsoluteDate.getJ2000Epoch(field).durationFrom(dateA).getReal(), 1.0e-15);
        FieldAbsoluteDate<T> dateB = FieldAbsoluteDate.createMJDDate(53774, field.getZero(), TimeScalesFactory.getUTC());
        FieldAbsoluteDate<T> dateC = new FieldAbsoluteDate<>(field, "2006-02-08T00:00:00", TimeScalesFactory.getUTC());
        Assert.assertEquals(0.0, dateC.durationFrom(dateB).getReal(), 1.0e-15);
    }

    private <T extends RealFieldElement<T>> void doTestJDDate(final Field<T> field) {
        final FieldAbsoluteDate<T> date = FieldAbsoluteDate.createJDDate(2400000, field.getZero().add(0.5 * Constants.JULIAN_DAY),
                                                                         TimeScalesFactory.getTT());
        Assert.assertEquals(0.0, FieldAbsoluteDate.getModifiedJulianEpoch(field).durationFrom(date).getReal(), 1.0e-15);
    }

    private <T extends RealFieldElement<T>> void doTestOffsets(final Field<T> field) {
        final TimeScale tai = TimeScalesFactory.getTAI();
        FieldAbsoluteDate<T> leapStartUTC = new FieldAbsoluteDate<>(field, 1976, 12, 31, 23, 59, 59, utc);
        FieldAbsoluteDate<T> leapEndUTC   = new FieldAbsoluteDate<>(field, 1977,  1,  1,  0,  0,  0, utc);
        FieldAbsoluteDate<T> leapStartTAI = new FieldAbsoluteDate<>(field, 1977,  1,  1,  0,  0, 14, tai);
        FieldAbsoluteDate<T> leapEndTAI   = new FieldAbsoluteDate<>(field, 1977,  1,  1,  0,  0, 16, tai);
        Assert.assertEquals(leapStartUTC, leapStartTAI);
        Assert.assertEquals(leapEndUTC, leapEndTAI);
        Assert.assertEquals(1, leapEndUTC.offsetFrom(leapStartUTC, utc).getReal(), 1.0e-10);
        Assert.assertEquals(1, leapEndTAI.offsetFrom(leapStartTAI, utc).getReal(), 1.0e-10);
        Assert.assertEquals(2, leapEndUTC.offsetFrom(leapStartUTC, tai).getReal(), 1.0e-10);
        Assert.assertEquals(2, leapEndTAI.offsetFrom(leapStartTAI, tai).getReal(), 1.0e-10);
        Assert.assertEquals(2, leapEndUTC.durationFrom(leapStartUTC).getReal(),    1.0e-10);
        Assert.assertEquals(2, leapEndTAI.durationFrom(leapStartTAI).getReal(),    1.0e-10);
    }

    private <T extends RealFieldElement<T>> void doTestBeforeAndAfterLeap(final Field<T> field) {
        final TimeScale tai = TimeScalesFactory.getTAI();
        FieldAbsoluteDate<T> leapStart = new FieldAbsoluteDate<>(field, 1977,  1,  1,  0,  0, 14, tai);
        FieldAbsoluteDate<T> leapEnd   = new FieldAbsoluteDate<>(field, 1977,  1,  1,  0,  0, 16, tai);
        for (int i = -10; i < 10; ++i) {
            final double dt = 1.1 * (2 * i - 1);
            FieldAbsoluteDate<T> d1 = leapStart.shiftedBy(dt);
            FieldAbsoluteDate<T> d2 = new FieldAbsoluteDate<>(leapStart, dt, tai);
            FieldAbsoluteDate<T> d3 = new FieldAbsoluteDate<>(leapStart, dt, utc);
            FieldAbsoluteDate<T> d4 = new FieldAbsoluteDate<>(leapEnd,   dt, tai);
            FieldAbsoluteDate<T> d5 = new FieldAbsoluteDate<>(leapEnd,   dt, utc);
            Assert.assertTrue(FastMath.abs(d1.durationFrom(d2).getReal()) < 1.0e-10);
            if (dt < 0) {
                Assert.assertTrue(FastMath.abs(d2.durationFrom(d3).getReal()) < 1.0e-10);
                Assert.assertTrue(d4.durationFrom(d5).getReal() > (1.0 - 1.0e-10));
            } else {
                Assert.assertTrue(d2.durationFrom(d3).getReal() < (-1.0 + 1.0e-10));
                Assert.assertTrue(FastMath.abs(d4.durationFrom(d5).getReal()) < 1.0e-10);
            }
        }
    }

    private <T extends RealFieldElement<T>> void doTestSymmetry(final Field<T> field) {
        final TimeScale tai = TimeScalesFactory.getTAI();
        FieldAbsoluteDate<T> leapStart = new FieldAbsoluteDate<>(field, 1977,  1,  1,  0,  0, 14, tai);
        for (int i = -10; i < 10; ++i) {
            final double dt = 1.1 * (2 * i - 1);
            Assert.assertEquals(dt, new FieldAbsoluteDate<>(leapStart, dt, utc).offsetFrom(leapStart, utc).getReal(), 1.0e-10);
            Assert.assertEquals(dt, new FieldAbsoluteDate<>(leapStart, dt, tai).offsetFrom(leapStart, tai).getReal(), 1.0e-10);
            Assert.assertEquals(dt, leapStart.shiftedBy(dt).durationFrom(leapStart).getReal(), 1.0e-10);
        }
    }

    @SuppressWarnings("unlikely-arg-type")
    private <T extends RealFieldElement<T>> void doTestEquals(final Field<T> field) {
        FieldAbsoluteDate<T> d1 =
            new FieldAbsoluteDate<>(field, new DateComponents(2006, 2, 25),
                                    new TimeComponents(17, 10, 34),
                                    utc);
        FieldAbsoluteDate<T> d2 = new FieldAbsoluteDate<>(field, new DateComponents(2006, 2, 25),
                                                          new TimeComponents(17, 10, 0),
                                                          utc).shiftedBy(34);
        Assert.assertTrue(d1.equals(d2));
        Assert.assertFalse(d1.equals(this));
    }

    private <T extends RealFieldElement<T>> void doTestIsEqualTo(final Field<T> field) {
        TestDates<T> dates = new TestDates<>(field);
        FieldAbsoluteDate<T> present = dates.getPresentFieldAbsoluteDate();
        Assert.assertTrue(present.isEqualTo(dates.present));
        Assert.assertTrue(present.isEqualTo(dates.presentToo));
        Assert.assertFalse(present.isEqualTo(dates.past));
        Assert.assertFalse(present.isEqualTo(dates.future));
    }

    private <T extends RealFieldElement<T>> void doTestIsCloseTo(final Field<T> field) {
        double tolerance = 10;
        TestDates<T> dates = new TestDates<>(field);
        FieldAbsoluteDate<T> present = dates.getPresentFieldAbsoluteDate();
        FieldTimeStamped<T> closeToPresent = new AnyFieldTimeStamped<>(field, dates.presentDate.shiftedBy(5));
        Assert.assertTrue(present.isCloseTo(present, tolerance));
        Assert.assertTrue(present.isCloseTo(dates.presentToo, tolerance));
        Assert.assertTrue(present.isCloseTo(closeToPresent, tolerance));
        Assert.assertFalse(present.isCloseTo(dates.past, tolerance));
        Assert.assertFalse(present.isCloseTo(dates.future, tolerance));
    }

    private <T extends RealFieldElement<T>> void doTestIsBefore(final Field<T> field) {
        TestDates<T> dates = new TestDates<>(field);
        FieldAbsoluteDate<T> present = dates.getPresentFieldAbsoluteDate();
        Assert.assertFalse(present.isBefore(dates.past));
        Assert.assertFalse(present.isBefore(present));
        Assert.assertFalse(present.isBefore(dates.presentToo));
        Assert.assertTrue(present.isBefore(dates.future));
    }

    private <T extends RealFieldElement<T>> void doTestIsAfter(final Field<T> field) {
        TestDates<T> dates = new TestDates<>(field);
        FieldAbsoluteDate<T> present = dates.getPresentFieldAbsoluteDate();
        Assert.assertTrue(present.isAfter(dates.past));
        Assert.assertFalse(present.isAfter(present));
        Assert.assertFalse(present.isAfter(dates.presentToo));
        Assert.assertFalse(present.isAfter(dates.future));
    }

    private <T extends RealFieldElement<T>> void doTestIsBeforeOrEqualTo(final Field<T> field) {
        TestDates<T> dates = new TestDates<>(field);
        FieldAbsoluteDate<T> present = dates.getPresentFieldAbsoluteDate();
        Assert.assertFalse(present.isBeforeOrEqualTo(dates.past));
        Assert.assertTrue(present.isBeforeOrEqualTo(present));
        Assert.assertTrue(present.isBeforeOrEqualTo(dates.presentToo));
        Assert.assertTrue(present.isBeforeOrEqualTo(dates.future));
    }

    private <T extends RealFieldElement<T>> void doTestIsAfterOrEqualTo(final Field<T> field) {
        TestDates<T> dates = new TestDates<>(field);
        FieldAbsoluteDate<T> present = dates.getPresentFieldAbsoluteDate();
        Assert.assertTrue(present.isAfterOrEqualTo(dates.past));
        Assert.assertTrue(present.isAfterOrEqualTo(present));
        Assert.assertTrue(present.isAfterOrEqualTo(dates.presentToo));
        Assert.assertFalse(present.isAfterOrEqualTo(dates.future));
    }

    private <T extends RealFieldElement<T>> void doTestIsBetween(final Field<T> field) {
        TestDates<T> dates = new TestDates<>(field);
        FieldAbsoluteDate<T> present = dates.getPresentFieldAbsoluteDate();
        Assert.assertTrue(present.isBetween(dates.past, dates.future));
        Assert.assertTrue(present.isBetween(dates.future, dates.past));
        Assert.assertFalse(dates.past.getDate().isBetween(present, dates.future));
        Assert.assertFalse(dates.past.getDate().isBetween(dates.future, present));
        Assert.assertFalse(dates.future.getDate().isBetween(dates.past, present));
        Assert.assertFalse(dates.future.getDate().isBetween(present, dates.past));
        Assert.assertFalse(present.isBetween(present, dates.future));
        Assert.assertFalse(present.isBetween(dates.past, present));
        Assert.assertFalse(present.isBetween(dates.past, dates.past));
        Assert.assertFalse(present.isBetween(present, present));
        Assert.assertFalse(present.isBetween(present, dates.presentToo));
    }

    private <T extends RealFieldElement<T>> void doTestIsBetweenOrEqualTo(final Field<T> field) {
        TestDates<T> dates = new TestDates<>(field);
        FieldAbsoluteDate<T> present = dates.getPresentFieldAbsoluteDate();
        Assert.assertTrue(present.isBetweenOrEqualTo(dates.past, dates.future));
        Assert.assertTrue(present.isBetweenOrEqualTo(dates.future, dates.past));
        Assert.assertFalse(dates.past.getDate().isBetweenOrEqualTo(present, dates.future));
        Assert.assertFalse(dates.past.getDate().isBetweenOrEqualTo(dates.future, present));
        Assert.assertFalse(dates.future.getDate().isBetweenOrEqualTo(dates.past, present));
        Assert.assertFalse(dates.future.getDate().isBetweenOrEqualTo(present, dates.past));
        Assert.assertTrue(present.isBetweenOrEqualTo(present, dates.future));
        Assert.assertTrue(present.isBetweenOrEqualTo(dates.past, present));
        Assert.assertFalse(present.isBetweenOrEqualTo(dates.past, dates.past));
        Assert.assertTrue(present.isBetweenOrEqualTo(present, present));
        Assert.assertTrue(present.isBetweenOrEqualTo(present, dates.presentToo));
    }

    private <T extends RealFieldElement<T>> void doTestComponents(final Field<T> field) {
        // this is NOT J2000.0,
        // it is either a few seconds before or after depending on time scale
        DateComponents date = new DateComponents(2000, 1, 1);
        TimeComponents time = new TimeComponents(11, 59, 10);
        TimeScale[] scales = {
            TimeScalesFactory.getTAI(), TimeScalesFactory.getUTC(),
            TimeScalesFactory.getTT(), TimeScalesFactory.getTCG()
        };
        for (int i = 0; i < scales.length; ++i) {
            FieldAbsoluteDate<T> in = new FieldAbsoluteDate<>(field, date, time, scales[i]);
            for (int j = 0; j < scales.length; ++j) {
                DateTimeComponents pair = in.getComponents(scales[j]);
                if (i == j) {
                    Assert.assertEquals(date, pair.getDate());
                    Assert.assertEquals(time, pair.getTime());
                } else {
                    Assert.assertNotSame(date, pair.getDate());
                    Assert.assertNotSame(time, pair.getTime());
                }
            }
        }
    }

    private <T extends RealFieldElement<T>> void doTestMonth(final Field<T> field) {
        TimeScale utc = TimeScalesFactory.getUTC();
        Assert.assertEquals(new FieldAbsoluteDate<>(field, 2011, 2, 23, utc),
                            new FieldAbsoluteDate<>(field, 2011, Month.FEBRUARY, 23, utc));
        Assert.assertEquals(new FieldAbsoluteDate<>(field, 2011, 2, 23, 1, 2, 3.4, utc),
                            new FieldAbsoluteDate<>(field, 2011, Month.FEBRUARY, 23, 1, 2, 3.4, utc));
    }

    private <T extends RealFieldElement<T>> void doTestCCSDSUnsegmentedNoExtension(final Field<T> field) {
        FieldAbsoluteDate<T> reference = new FieldAbsoluteDate<>(field, "2002-05-23T12:34:56.789", utc);
        double lsb = FastMath.pow(2.0, -24);

        byte[] timeCCSDSEpoch = new byte[] { 0x53, 0x7F, 0x40, -0x70, -0x37, -0x05, -0x19 };
        for (int preamble = 0x00; preamble < 0x80; ++preamble) {
            if (preamble == 0x1F) {
                // using CCSDS reference epoch
                FieldAbsoluteDate<T> ccsds1 =
                                FieldAbsoluteDate.parseCCSDSUnsegmentedTimeCode(field, (byte) preamble, (byte) 0x0, timeCCSDSEpoch, null);
                Assert.assertEquals(0, ccsds1.durationFrom(reference).getReal(), lsb / 2);
            } else {
                try {
                    FieldAbsoluteDate.parseCCSDSUnsegmentedTimeCode(field, (byte) preamble, (byte) 0x0, timeCCSDSEpoch, null);
                    Assert.fail("an exception should have been thrown");
                } catch (OrekitException iae) {
                    // expected
                }

            }
        }

        // missing epoch
        byte[] timeJ2000Epoch = new byte[] { 0x04, 0x7E, -0x0B, -0x10, -0x07, 0x16, -0x79 };
        try {
            FieldAbsoluteDate.parseCCSDSUnsegmentedTimeCode(field, (byte) 0x2F, (byte) 0x0, timeJ2000Epoch, null);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException iae) {
            // expected
        }

        // using J2000.0 epoch
        FieldAbsoluteDate<T> ccsds3 =
                        FieldAbsoluteDate.parseCCSDSUnsegmentedTimeCode(field, (byte) 0x2F, (byte) 0x0, timeJ2000Epoch,
                                                                        FieldAbsoluteDate.getJ2000Epoch(field));
        Assert.assertEquals(0, ccsds3.durationFrom(reference).getReal(), lsb / 2);

    }

    private <T extends RealFieldElement<T>> void doTestCCSDSUnsegmentedWithExtendedPreamble(final Field<T> field) {

        FieldAbsoluteDate<T> reference = new FieldAbsoluteDate<>(field, "2095-03-03T22:02:45.789012345678901", utc);
        int leap = (int) FastMath.rint(utc.offsetFromTAI(reference.toAbsoluteDate()));
        double lsb = FastMath.pow(2.0, -48);

        byte extendedPreamble = (byte) -0x80;
        byte identification   = (byte)  0x10;
        byte coarseLength1    = (byte)  0x0C; // four (3 + 1) bytes
        byte fineLength1      = (byte)  0x03; // 3 bytes
        byte coarseLength2    = (byte)  0x20; // 1 additional byte for coarse time
        byte fineLength2      = (byte)  0x0C; // 3 additional bytes for fine time
        byte[] timeCCSDSEpoch = new byte[] {
             0x01,  0x02,  0x03,  0x04,  (byte)(0x05 - leap), // 5 bytes for coarse time (seconds)
            -0x37, -0x04, -0x4A, -0x74, -0x2C, -0x3C          // 6 bytes for fine time (sub-seconds)
        };
        byte preamble1 = (byte) (extendedPreamble | identification | coarseLength1 | fineLength1);
        byte preamble2 = (byte) (coarseLength2 | fineLength2);
        FieldAbsoluteDate<T> ccsds1 =
                        FieldAbsoluteDate.parseCCSDSUnsegmentedTimeCode(field, preamble1, preamble2, timeCCSDSEpoch, null);
        Assert.assertEquals(0, ccsds1.durationFrom(reference).getReal(), lsb / 2);

    }

    private <T extends RealFieldElement<T>> void doTestCCSDSDaySegmented(final Field<T> field) {
        FieldAbsoluteDate<T> reference = new FieldAbsoluteDate<>(field, "2002-05-23T12:34:56.789012345678", TimeScalesFactory.getUTC());
        double lsb = 1.0e-13;
        byte[] timeCCSDSEpoch = new byte[] { 0x3F, 0x55, 0x02, -0x4D, 0x2C, -0x6B, 0x00, -0x44, 0x61, 0x4E };

        for (int preamble = 0x00; preamble < 0x100; ++preamble) {
            if (preamble == 0x42) {
                // using CCSDS reference epoch

                FieldAbsoluteDate<T> ccsds1 =
                                FieldAbsoluteDate.parseCCSDSDaySegmentedTimeCode(field, (byte) preamble, timeCCSDSEpoch, null);
                Assert.assertEquals(0, ccsds1.durationFrom(reference).getReal(), lsb / 2);
            } else {
                try {
                    FieldAbsoluteDate.parseCCSDSDaySegmentedTimeCode(field, (byte) preamble, timeCCSDSEpoch, null);
                    Assert.fail("an exception should have been thrown");
                } catch (OrekitException iae) {
                    // expected
                }

            }
        }

        // missing epoch
        byte[] timeJ2000Epoch = new byte[] { 0x03, 0x69, 0x02, -0x4D, 0x2C, -0x6B, 0x00, -0x44, 0x61, 0x4E };
        try {
            FieldAbsoluteDate.parseCCSDSDaySegmentedTimeCode(field, (byte) 0x4A, timeJ2000Epoch, null);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException iae) {
            // expected
        }

        // using J2000.0 epoch
        FieldAbsoluteDate<T> ccsds3 =
                        FieldAbsoluteDate.parseCCSDSDaySegmentedTimeCode(field, (byte) 0x4A, timeJ2000Epoch, DateComponents.J2000_EPOCH);
        Assert.assertEquals(0, ccsds3.durationFrom(reference).getReal(), lsb / 2);

        // limit to microsecond
        byte[] timeMicrosecond = new byte[] { 0x03, 0x69, 0x02, -0x4D, 0x2C, -0x6B, 0x00, 0x0C };
        FieldAbsoluteDate<T> ccsds4 =
                        FieldAbsoluteDate.parseCCSDSDaySegmentedTimeCode(field, (byte) 0x49, timeMicrosecond, DateComponents.J2000_EPOCH);
        Assert.assertEquals(-0.345678e-6, ccsds4.durationFrom(reference).getReal(), lsb / 2);

    }

    private <T extends RealFieldElement<T>> void doTestCCSDSCalendarSegmented(final Field<T> field) {

        FieldAbsoluteDate<T> reference = new FieldAbsoluteDate<>(field, "2002-05-23T12:34:56.789012345678", TimeScalesFactory.getUTC());
        double lsb = 1.0e-13;
        FieldAbsoluteDate<T> FAD = new FieldAbsoluteDate<>(field);
        // month of year / day of month variation
        byte[] timeMonthDay = new byte[] { 0x07, -0x2E, 0x05, 0x17, 0x0C, 0x22, 0x38, 0x4E, 0x5A, 0x0C, 0x22, 0x38, 0x4E };
        for (int preamble = 0x00; preamble < 0x100; ++preamble) {
            if (preamble == 0x56) {
                FieldAbsoluteDate<T> ccsds1 =
                    FAD.parseCCSDSCalendarSegmentedTimeCode((byte) preamble, timeMonthDay);
                Assert.assertEquals(0, ccsds1.durationFrom(reference).getReal(), lsb / 2);
            } else {
                try {
                    FAD.parseCCSDSCalendarSegmentedTimeCode((byte) preamble, timeMonthDay);
                    Assert.fail("an exception should have been thrown");
                } catch (OrekitException iae) {
                    // expected
                } catch (IllegalArgumentException iae) {
                    // should happen when preamble specifies day of year variation
                    // since there is no day 1303 (= 5 * 256 + 23) in any year ...
                    Assert.assertEquals(preamble & 0x08, 0x08);
                }

            }
        }

        // day of year variation
        byte[] timeDay = new byte[] { 0x07, -0x2E, 0x00, -0x71, 0x0C, 0x22, 0x38, 0x4E, 0x5A, 0x0C, 0x22, 0x38, 0x4E };
        for (int preamble = 0x00; preamble < 0x100; ++preamble) {
            if (preamble == 0x5E) {
                FieldAbsoluteDate<T> ccsds1 =
                    FAD.parseCCSDSCalendarSegmentedTimeCode((byte) preamble, timeDay);
                Assert.assertEquals(0, ccsds1.durationFrom(reference).getReal(), lsb / 2);
            } else {
                try {
                    FAD.parseCCSDSCalendarSegmentedTimeCode((byte) preamble, timeDay);
                    Assert.fail("an exception should have been thrown");
                } catch (OrekitException iae) {
                    // expected
                } catch (IllegalArgumentException iae) {
                    // should happen when preamble specifies month of year / day of month variation
                    // since there is no month 0 in any year ...
                    Assert.assertEquals(preamble & 0x08, 0x00);
                }

            }
        }

        // limit to microsecond
        byte[] timeMicrosecond = new byte[] { 0x07, -0x2E, 0x00, -0x71, 0x0C, 0x22, 0x38, 0x4E, 0x5A, 0x0C };
        FieldAbsoluteDate<T> ccsds4 =
            FAD.parseCCSDSCalendarSegmentedTimeCode((byte) 0x5B, timeMicrosecond);
        Assert.assertEquals(-0.345678e-6, ccsds4.durationFrom(reference).getReal(), lsb / 2);

    }

    private <T extends RealFieldElement<T>> void doTestExpandedConstructors(final Field<T> field) {
        Assert.assertEquals(new FieldAbsoluteDate<>(field, new DateComponents(2002, 05, 28),
                                                    new TimeComponents(15, 30, 0),
                                                    TimeScalesFactory.getUTC()),
                     new FieldAbsoluteDate<>(field, 2002, 05, 28, 15, 30, 0, TimeScalesFactory.getUTC()));
        Assert.assertEquals(new FieldAbsoluteDate<>(field, new DateComponents(2002, 05, 28), TimeComponents.H00,
                                                    TimeScalesFactory.getUTC()),
                     new FieldAbsoluteDate<>(field, 2002, 05, 28, TimeScalesFactory.getUTC()));
        try {
            new FieldAbsoluteDate<>(field, 2002, 05, 28, 25, 30, 0, TimeScalesFactory.getUTC());
        } catch (OrekitIllegalArgumentException oiae) {
            Assert.assertEquals(OrekitMessages.NON_EXISTENT_HMS_TIME, oiae.getSpecifier());
            Assert.assertEquals(25, ((Integer) oiae.getParts()[0]).intValue());
            Assert.assertEquals(30, ((Integer) oiae.getParts()[1]).intValue());
            Assert.assertEquals( 0, ((Double) oiae.getParts()[2]).doubleValue(), 1.0e-15);
        }
    }

    private <T extends RealFieldElement<T>> void doTestHashcode(final Field<T> field) {
        FieldAbsoluteDate<T> d1 =
            new FieldAbsoluteDate<>(field, new DateComponents(2006, 2, 25),
                                    new TimeComponents(17, 10, 34),
                                    utc);
        FieldAbsoluteDate<T> d2 = new FieldAbsoluteDate<>(field, new DateComponents(2006, 2, 25),
                                                          new TimeComponents(17, 10, 0),
                                                          utc).shiftedBy(34);
        Assert.assertEquals(d1.hashCode(), d2.hashCode());
        Assert.assertTrue(d1.hashCode() != d1.shiftedBy(1.0e-3).hashCode());
    }

    private <T extends RealFieldElement<T>> void doTestInfinity(final Field<T> field) {
        Assert.assertTrue(FieldAbsoluteDate.getJulianEpoch(field).compareTo(FieldAbsoluteDate.getPastInfinity(field)) > 0);
        Assert.assertTrue(FieldAbsoluteDate.getJulianEpoch(field).compareTo(FieldAbsoluteDate.getFutureInfinity(field)) < 0);
        Assert.assertTrue(FieldAbsoluteDate.getJ2000Epoch(field).compareTo(FieldAbsoluteDate.getPastInfinity(field)) > 0);
        Assert.assertTrue(FieldAbsoluteDate.getJ2000Epoch(field).compareTo(FieldAbsoluteDate.getFutureInfinity(field)) < 0);
        Assert.assertTrue(FieldAbsoluteDate.getPastInfinity(field).compareTo(FieldAbsoluteDate.getJulianEpoch(field)) < 0);
        Assert.assertTrue(FieldAbsoluteDate.getPastInfinity(field).compareTo(FieldAbsoluteDate.getJ2000Epoch(field)) < 0);
        Assert.assertTrue(FieldAbsoluteDate.getPastInfinity(field).compareTo(FieldAbsoluteDate.getFutureInfinity(field)) < 0);
        Assert.assertTrue(FieldAbsoluteDate.getFutureInfinity(field).compareTo(FieldAbsoluteDate.getJulianEpoch(field)) > 0);
        Assert.assertTrue(FieldAbsoluteDate.getFutureInfinity(field).compareTo(FieldAbsoluteDate.getJ2000Epoch(field)) > 0);
        Assert.assertTrue(FieldAbsoluteDate.getFutureInfinity(field).compareTo(FieldAbsoluteDate.getPastInfinity(field)) > 0);
        Assert.assertTrue(Double.isInfinite(FieldAbsoluteDate.getFutureInfinity(field).durationFrom(FieldAbsoluteDate.getJ2000Epoch(field)).getReal()));
        Assert.assertTrue(Double.isInfinite(FieldAbsoluteDate.getFutureInfinity(field).durationFrom(FieldAbsoluteDate.getPastInfinity(field)).getReal()));
        Assert.assertTrue(Double.isInfinite(FieldAbsoluteDate.getPastInfinity(field).durationFrom(FieldAbsoluteDate.getJ2000Epoch(field)).getReal()));
        Assert.assertEquals("5881610-07-11T23:59:59.999",  FieldAbsoluteDate.getFutureInfinity(field).toString());
        Assert.assertEquals("-5877490-03-03T00:00:00.000", FieldAbsoluteDate.getPastInfinity(field).toString());
    }

    private <T extends RealFieldElement<T>> void doTestAccuracy(final Field<T> field) {
        TimeScale tai = TimeScalesFactory.getTAI();
        double sec = 0.281;
        FieldAbsoluteDate<T> t = new FieldAbsoluteDate<>(field, 2010, 6, 21, 18, 42, sec, tai);
        double recomputedSec = t.getComponents(tai).getTime().getSecond();
        Assert.assertEquals(sec, recomputedSec, FastMath.ulp(sec));
    }

    private <T extends RealFieldElement<T>> void doTestAccuracyIssue348(final Field<T> field)
        {
        FieldAbsoluteDate<T> tF = new FieldAbsoluteDate<>(field,
                                                          new DateComponents(1970, 01, 01),
                                                          new TimeComponents(3, 25, 45.6789),
                                                          TimeScalesFactory.getUTC());
        AbsoluteDate tA = tF.toAbsoluteDate();
        double delta = -0.01;
        T recomputedDelta = tF.shiftedBy(delta).durationFrom(tA);
        Assert.assertEquals(delta, recomputedDelta.getReal(), 1.0e-17);
    }

    private <T extends RealFieldElement<T>> void doTestIterationAccuracy(final Field<T> field) {

        final TimeScale tai = TimeScalesFactory.getTAI();
        final FieldAbsoluteDate<T> t0 = new FieldAbsoluteDate<>(field, 2010, 6, 21, 18, 42, 0.281, tai);

        // 0.1 is not representable exactly in double precision
        // we will accumulate error, between -0.5ULP and -3ULP at each iteration
        checkIteration(0.1, t0, 10000, 3.0, -1.19, 1.0e-4);

        // 0.125 is representable exactly in double precision
        // error will be null
        checkIteration(0.125, t0, 10000, 1.0e-15, 0.0, 1.0e-15);
    }

    private <T extends RealFieldElement<T>> void checkIteration(final double step, final FieldAbsoluteDate<T> t0, final int nMax,
                                final double maxErrorFactor,
                                final double expectedMean, final double meanTolerance) {
        final double epsilon = FastMath.ulp(step);
        FieldAbsoluteDate<T> iteratedDate = t0;
        double mean = 0;
        for (int i = 1; i < nMax; ++i) {
            iteratedDate = iteratedDate.shiftedBy(step);
            FieldAbsoluteDate<T> directDate = t0.shiftedBy(i * step);
            final T error = iteratedDate.durationFrom(directDate);
            mean += error.getReal() / (i * epsilon);
            Assert.assertEquals(0.0, iteratedDate.durationFrom(directDate).getReal(), maxErrorFactor * i * epsilon);
        }
        mean /= nMax;
        Assert.assertEquals(expectedMean, mean, meanTolerance);
    }

    private <T extends RealFieldElement<T>> void doTestIssue142(final Field<T> field) {
        final FieldAbsoluteDate<T> epoch = FieldAbsoluteDate.getJavaEpoch(field);
        final TimeScale utc = TimeScalesFactory.getUTC();

        Assert.assertEquals("1970-01-01T00:00:00.000", epoch.toString(utc));
        Assert.assertEquals(0.0, epoch.durationFrom(new FieldAbsoluteDate<>(field, 1970, 1, 1, utc)).getReal(), 1.0e-15);
        Assert.assertEquals(8.000082,
                            epoch.durationFrom(new FieldAbsoluteDate<>(field, DateComponents.JAVA_EPOCH, TimeScalesFactory.getTAI())).getReal(),
                            1.0e-15);

        //Milliseconds - April 1, 2006, in UTC
        long msOffset = 1143849600000l;
        final FieldAbsoluteDate<T> ad = new FieldAbsoluteDate<>(epoch, msOffset / 1000, TimeScalesFactory.getUTC());
        Assert.assertEquals("2006-04-01T00:00:00.000", ad.toString(utc));
    }

    private <T extends RealFieldElement<T>> void doTestIssue148(final Field<T> field) {
        final TimeScale utc = TimeScalesFactory.getUTC();
        FieldAbsoluteDate<T> t0 = new FieldAbsoluteDate<>(field, 2012, 6, 30, 23, 59, 50.0, utc);
        DateTimeComponents components = t0.shiftedBy(11.0 - 200 * Precision.EPSILON).getComponents(utc);
        Assert.assertEquals(2012, components.getDate().getYear());
        Assert.assertEquals(   6, components.getDate().getMonth());
        Assert.assertEquals(  30, components.getDate().getDay());
        Assert.assertEquals(  23, components.getTime().getHour());
        Assert.assertEquals(  59, components.getTime().getMinute());
        Assert.assertEquals(  61 - 200 * Precision.EPSILON,
                            components.getTime().getSecond(), 1.0e-15);
    }

    private <T extends RealFieldElement<T>> void doTestIssue149(final Field<T> field) {
        final TimeScale utc = TimeScalesFactory.getUTC();
        FieldAbsoluteDate<T> t0 = new FieldAbsoluteDate<>(field, 2012, 6, 30, 23, 59, 59, utc);
        DateTimeComponents components = t0.shiftedBy(1.0 - Precision.EPSILON).getComponents(utc);
        Assert.assertEquals(2012, components.getDate().getYear());
        Assert.assertEquals(   6, components.getDate().getMonth());
        Assert.assertEquals(  30, components.getDate().getDay());
        Assert.assertEquals(  23, components.getTime().getHour());
        Assert.assertEquals(  59, components.getTime().getMinute());
        Assert.assertEquals(  60 - Precision.EPSILON,
                            components.getTime().getSecond(), 1.0e-15);
    }

    private <T extends RealFieldElement<T>> void doTestWrapAtMinuteEnd(final Field<T> field) {
        TimeScale tai = TimeScalesFactory.getTAI();
        TimeScale utc = TimeScalesFactory.getUTC();
        FieldAbsoluteDate<T> date0 = new FieldAbsoluteDate<>(field, DateComponents.J2000_EPOCH, TimeComponents.H12, tai);
        FieldAbsoluteDate<T> ref = date0.shiftedBy(496891466.0).shiftedBy(0.7320114066633323);
        FieldAbsoluteDate<T> date = ref.shiftedBy(33 * -597.9009700426262);
        DateTimeComponents dtc = date.getComponents(utc);
        Assert.assertEquals(2015, dtc.getDate().getYear());
        Assert.assertEquals(   9, dtc.getDate().getMonth());
        Assert.assertEquals(  30, dtc.getDate().getDay());
        Assert.assertEquals(   7, dtc.getTime().getHour());
        Assert.assertEquals(  54, dtc.getTime().getMinute());
        Assert.assertEquals(60 - 9.094947e-13, dtc.getTime().getSecond(), 1.0e-15);
        Assert.assertEquals("2015-09-30T07:55:00.000",
                            date.toString(utc));
        FieldAbsoluteDate<T> beforeMidnight = new FieldAbsoluteDate<>(field, 2008, 2, 29, 23, 59, 59.9994, utc);
        FieldAbsoluteDate<T> stillBeforeMidnight = beforeMidnight.shiftedBy(2.0e-4);
        Assert.assertEquals(59.9994, beforeMidnight.getComponents(utc).getTime().getSecond(), 1.0e-15);
        Assert.assertEquals(59.9996, stillBeforeMidnight.getComponents(utc).getTime().getSecond(), 1.0e-15);
        Assert.assertEquals("2008-02-29T23:59:59.999", beforeMidnight.toString(utc));
        Assert.assertEquals("2008-03-01T00:00:00.000", stillBeforeMidnight.toString(utc));
    }

    private <T extends RealFieldElement<T>> void doTestIssue508(final Field<T> field) {
        AbsoluteDate date = new AbsoluteDate(2000, 2, 24, 17, 5, 30.047, TimeScalesFactory.getUTC());
        FieldAbsoluteDate<T> tA = new FieldAbsoluteDate<>(field, date);
        FieldAbsoluteDate<T> tB = new FieldAbsoluteDate<>(date, field.getZero());
        Assert.assertEquals(0.0, tA.durationFrom(tB).getReal(), Precision.SAFE_MIN);
    }

    static class AnyFieldTimeStamped<T extends RealFieldElement<T>> implements FieldTimeStamped<T> {
        AbsoluteDate date;
        Field<T> field;

        public AnyFieldTimeStamped(Field<T> field, AbsoluteDate date) {
            this.date = date;
            this.field = field;
        }

        @Override
        public FieldAbsoluteDate<T> getDate() {
            return new FieldAbsoluteDate<>(field, date);
        }
    }

    static class TestDates<T extends RealFieldElement<T>> {
        private final AbsoluteDate presentDate;
        private final AnyFieldTimeStamped<T> present;
        private final AnyFieldTimeStamped<T> past;
        private final AnyFieldTimeStamped<T> presentToo;
        private final AnyFieldTimeStamped<T> future;

        public TestDates(Field<T> field) {
            presentDate = new AbsoluteDate(new DateComponents(2000, 1, 1),
                                                new TimeComponents(12, 00, 00),
                                                TimeScalesFactory.getUTC());
            present    = new AnyFieldTimeStamped<>(field, presentDate);
            presentToo = new AnyFieldTimeStamped<>(field, presentDate.shiftedBy(0));
            past       = new AnyFieldTimeStamped<>(field, presentDate.shiftedBy(-1000));
            future     = new AnyFieldTimeStamped<>(field, presentDate.shiftedBy(1000));
        }

        public FieldAbsoluteDate<T> getPresentFieldAbsoluteDate() {
            return present.getDate();
        }
    }

}
