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
package org.orekit.time;



import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

public class UTCTAIBulletinAFilesLoaderTest {

    @Test
    public void test2006Leap() throws OrekitException {
        Utils.setDataRoot("bulletinA");
        // this file contains a single leap second on 2006-01-01, from 32s to 33s
        TimeScalesFactory.addUTCTAIOffsetsLoader(new UTCTAIBulletinAFilesLoader("bulletina-xix-001\\.txt$"));

        UTCScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate afterLeap = new AbsoluteDate(1961, 1, 1, 0, 0, 0.0, utc);
        Assert.assertEquals(1.4228180,
                            afterLeap.durationFrom(utc.getFirstKnownLeapSecond()),
                            1.0e-12);
        afterLeap = new AbsoluteDate(2006, 1, 1, 0, 0, 0.0, utc);
        Assert.assertEquals(1.0,
                            afterLeap.durationFrom(utc.getLastKnownLeapSecond()),
                            1.0e-12);

        // correct values, as the single leap second is close enough
        checkOffset("2000-01-01", -32.0);
        checkOffset("2008-01-01", -33.0);

        // expected wrong estimation as the leap seconds from 1997-01-01 and 1999-01-01 are not known from the file read
        checkOffset("1996-04-03", -32.0);

        // expected wrong estimation as the leap seconds from 2009-01-01 and 2012-07-01 are not known from the file read
        checkOffset("2013-01-22", -33.0);

    }

    @Test
    public void test2009WrongLeap() throws OrekitException {
        Utils.setDataRoot("bulletinA");
        // this file contains a single leap second on 2009-01-01, from 33s to 34s,
        // but it has a known ERROR in it, as line 66 reads:
        //    TAI-UTC(MJD 54832) = 33.0
        // whereas the value should be 34.0, as the leap second was introduced
        // just before this day
        TimeScalesFactory.addUTCTAIOffsetsLoader(new UTCTAIBulletinAFilesLoader("bulletina-xxi-053-original\\.txt$"));

        UTCScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate afterLeap = new AbsoluteDate(1961, 1, 1, 0, 0, 0.0, utc);
        Assert.assertEquals(1.4228180,
                            afterLeap.durationFrom(utc.getFirstKnownLeapSecond()),
                            1.0e-12);
        afterLeap = new AbsoluteDate(2009, 1, 1, 0, 0, 0.0, utc);
        Assert.assertEquals(1.0,
                            afterLeap.durationFrom(utc.getLastKnownLeapSecond()),
                            1.0e-12);

        // expected incorrect values, as the file contains an error
        checkOffset("2008-01-01", -32.0); // the real value should be -33.0
        checkOffset("2009-06-30", -33.0); // the real value should be -34.0

    }

    @Test
    public void test2009FixedLeap() throws OrekitException {
        Utils.setDataRoot("bulletinA");
        // this file is a fixed version of IERS bulletin
        TimeScalesFactory.addUTCTAIOffsetsLoader(new UTCTAIBulletinAFilesLoader("bulletina-xxi-053-fixed\\.txt$"));

        UTCScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate afterLeap = new AbsoluteDate(1961, 1, 1, 0, 0, 0.0, utc);
        Assert.assertEquals(1.4228180,
                            afterLeap.durationFrom(utc.getFirstKnownLeapSecond()),
                            1.0e-12);
        afterLeap = new AbsoluteDate(2009, 1, 1, 0, 0, 0.0, utc);
        Assert.assertEquals(1.0,
                            afterLeap.durationFrom(utc.getLastKnownLeapSecond()),
                            1.0e-12);

        // correct values, as the original file error has been fixed
        checkOffset("2008-01-01", -33.0);
        checkOffset("2009-06-30", -34.0);

    }

    @Test
    public void testNoLeap() throws OrekitException {
        Utils.setDataRoot("bulletinA");
        // these files contains no leap seconds
        TimeScalesFactory.addUTCTAIOffsetsLoader(new UTCTAIBulletinAFilesLoader("bulletina-xxvi.*\\.txt$"));

        UTCScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate afterLeap = new AbsoluteDate(1961, 1, 1, 0, 0, 0.0, utc);
        Assert.assertEquals(1.4228180,
                            afterLeap.durationFrom(utc.getFirstKnownLeapSecond()),
                            1.0e-12);

        // the artificial first leap is big ...
        afterLeap = new AbsoluteDate(1972, 1, 1, 0, 0, 0.0, utc);
        Assert.assertTrue(afterLeap.durationFrom(utc.getLastKnownLeapSecond()) > 25.1);

        // as there are no leap seconds identified, everything should be at 35s
        checkOffset("1973-01-01", -35.0);
        checkOffset("2000-01-01", -35.0);
        checkOffset("2002-01-01", -35.0);
        checkOffset("2004-01-01", -35.0);
        checkOffset("2006-01-01", -35.0);
        checkOffset("2008-01-01", -35.0);
        checkOffset("2010-01-01", -35.0);
        checkOffset("2012-01-01", -35.0);
        checkOffset("2014-01-01", -35.0);
        checkOffset("2100-01-01", -35.0);

    }

    @Test
    public void testMissingTimeSteps() throws OrekitException {
        checkException("bulletina-(?:xix|xxii)-001\\.txt",
                       OrekitMessages.MISSING_EARTH_ORIENTATION_PARAMETERS_BETWEEN_DATES);
    }

    @Test
    public void testMissingRapidSections() throws OrekitException {
        checkException("bulletina-missing-eop-rapid-service.txt",
                       OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE);
        checkException("bulletina-missing-eop-rapid-service.txt",
                       OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE);
    }

    @Test
    public void testMissingData() throws OrekitException {
        checkException("bulletina-truncated-in-prediction-header.txt",
                       OrekitMessages.UNEXPECTED_END_OF_FILE_AFTER_LINE);
        checkException("bulletina-truncated-after-prediction-header.txt",
                       OrekitMessages.UNEXPECTED_END_OF_FILE_AFTER_LINE);
    }

    @Test
    public void testInconsistentDate() throws OrekitException {
        checkException("bulletina-inconsistent-year.txt", OrekitMessages.INCONSISTENT_DATES_IN_IERS_FILE);
        checkException("bulletina-inconsistent-month.txt", OrekitMessages.INCONSISTENT_DATES_IN_IERS_FILE);
        checkException("bulletina-inconsistent-day.txt", OrekitMessages.INCONSISTENT_DATES_IN_IERS_FILE);
    }

    private void checkOffset(final String s, final double expected) throws OrekitException {
        final AbsoluteDate date = new AbsoluteDate(s, TimeScalesFactory.getTAI());
        Assert.assertEquals(expected, TimeScalesFactory.getUTC().offsetFromTAI(date), 10e-8);
    }

    private void checkException(String name, OrekitMessages message) {
        Utils.setDataRoot("bulletinA");
        TimeScalesFactory.addUTCTAIOffsetsLoader(new UTCTAIBulletinAFilesLoader(name));
        try {
            TimeScalesFactory.getUTC();
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(message, oe.getSpecifier());
        }
    }

}
