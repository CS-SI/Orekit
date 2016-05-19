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

public class TAIUTCDatAFilesLoaderTest {

    @Test
    public void testRegularFile() throws OrekitException {

        Utils.setDataRoot("USNO");

        // we arbitrary put UTC == TAI before 1961-01-01
        checkOffset(1950,  1,  1,   0);

        // linear models between 1961 and 1972
        checkOffset(1961,  1,  2,  -(1.422818 +   1 * 0.001296));  // MJD 37300 +   1
        checkOffset(1961,  8,  2,  -(1.372818 + 213 * 0.001296));  // MJD 37300 + 213
        checkOffset(1962,  1,  2,  -(1.845858 +   1 * 0.0011232)); // MJD 37665 +   1
        checkOffset(1963, 11,  2,  -(1.945858 + 670 * 0.0011232)); // MJD 37665 + 670
        checkOffset(1964,  1,  2,  -(3.240130 - 365 * 0.001296));  // MJD 38761 - 365
        checkOffset(1964,  4,  2,  -(3.340130 - 274 * 0.001296));  // MJD 38761 - 274
        checkOffset(1964,  9,  2,  -(3.440130 - 121 * 0.001296));  // MJD 38761 - 121
        checkOffset(1965,  1,  2,  -(3.540130 +   1 * 0.001296));  // MJD 38761 +   1
        checkOffset(1965,  3,  2,  -(3.640130 +  60 * 0.001296));  // MJD 38761 +  60
        checkOffset(1965,  7,  2,  -(3.740130 + 182 * 0.001296));  // MJD 38761 + 182
        checkOffset(1965,  9,  2,  -(3.840130 + 244 * 0.001296));  // MJD 38761 + 244
        checkOffset(1966,  1,  2,  -(4.313170 +   1 * 0.002592));  // MJD 39126 +   1
        checkOffset(1968,  2,  2,  -(4.213170 + 762 * 0.002592));  // MJD 39126 + 762

        // since 1972-01-01, offsets are only whole seconds
        checkOffset(1972,  3,  5, -10);
        checkOffset(1972,  7, 14, -11);
        checkOffset(1979, 12, 31, -18);
        checkOffset(1980,  1, 22, -19);
        checkOffset(2006,  7,  7, -33);
        checkOffset(2010,  7,  7, -34);
        checkOffset(2012,  7,  7, -35);
        checkOffset(2015,  7,  7, -36);

    }

    @Test
    public void testOnlyPre1972Data() throws OrekitException {

        Utils.setDataRoot("USNO");
        TimeScalesFactory.addUTCTAIOffsetsLoader(new TAIUTCDatFilesLoader("tai-utc-only-pre-1972-data.dat"));

        // linear models between 1961 and 1972
        checkOffset(1961,  1,  2,  -(1.422818 +   1 * 0.001296));  // MJD 37300 +   1
        checkOffset(1961,  8,  2,  -(1.372818 + 213 * 0.001296));  // MJD 37300 + 213
        checkOffset(1962,  1,  2,  -(1.845858 +   1 * 0.0011232)); // MJD 37665 +   1
        checkOffset(1963, 11,  2,  -(1.945858 + 670 * 0.0011232)); // MJD 37665 + 670
        checkOffset(1964,  1,  2,  -(3.240130 - 365 * 0.001296));  // MJD 38761 - 365
        checkOffset(1964,  4,  2,  -(3.340130 - 274 * 0.001296));  // MJD 38761 - 274
        checkOffset(1964,  9,  2,  -(3.440130 - 121 * 0.001296));  // MJD 38761 - 121
        checkOffset(1965,  1,  2,  -(3.540130 +   1 * 0.001296));  // MJD 38761 +   1
        checkOffset(1965,  3,  2,  -(3.640130 +  60 * 0.001296));  // MJD 38761 +  60
        checkOffset(1965,  7,  2,  -(3.740130 + 182 * 0.001296));  // MJD 38761 + 182
        checkOffset(1965,  9,  2,  -(3.840130 + 244 * 0.001296));  // MJD 38761 + 244
        checkOffset(1966,  1,  2,  -(4.313170 +   1 * 0.002592));  // MJD 39126 +   1
        checkOffset(1968,  2,  2,  -(4.213170 + 762 * 0.002592));  // MJD 39126 + 762

        // last linear drift is not stopped as we miss the first constant offset in 1972
        checkOffset(1972,  3,  5,  -(4.213170 +  2255 * 0.002592));  // MJD 39126 +  2255
        checkOffset(1972,  7, 14,  -(4.213170 +  2386 * 0.002592));  // MJD 39126 +  2386
        checkOffset(1979, 12, 31,  -(4.213170 +  5112 * 0.002592));  // MJD 39126 +  5112
        checkOffset(1980,  1, 22,  -(4.213170 +  5134 * 0.002592));  // MJD 39126 +  5134
        checkOffset(2006,  7,  7,  -(4.213170 + 14797 * 0.002592));  // MJD 39126 + 14797
        checkOffset(2010,  7,  7,  -(4.213170 + 16258 * 0.002592));  // MJD 39126 + 16258
        checkOffset(2012,  7,  7,  -(4.213170 + 16989 * 0.002592));  // MJD 39126 + 16989
        checkOffset(2015,  7,  7,  -(4.213170 + 18084 * 0.002592));  // MJD 39126 + 18084

    }

    @Test
    public void testModifiedLinearData() throws OrekitException {

        Utils.setDataRoot("USNO");
        TimeScalesFactory.addUTCTAIOffsetsLoader(new TAIUTCDatFilesLoader("tai-utc-modified-linear.dat"));

        // linear models between 1961 and 1972
        checkOffset(1961,  1,  2,  -(1.4000000 +   1 * 0.001000));  // MJD 37300 +   1
        checkOffset(1961,  8,  2,  -(1.5000000 + 213 * 0.001100));  // MJD 37300 + 213
        checkOffset(1962,  1,  2,  -(1.6000000 +   1 * 0.001200));  // MJD 37665 +   1
        checkOffset(1963, 11,  2,  -(1.7000000 + 670 * 0.001300));  // MJD 37665 + 670
        checkOffset(1964,  1,  2,  -(1.8000000 - 365 * 0.001400));  // MJD 38761 - 365
        checkOffset(1964,  4,  2,  -(1.9000000 - 274 * 0.001500));  // MJD 38761 - 274
        checkOffset(1964,  9,  2,  -(2.0000000 - 121 * 0.001600));  // MJD 38761 - 121
        checkOffset(1965,  1,  2,  -(2.1000000 +   1 * 0.001700));  // MJD 38761 +   1
        checkOffset(1965,  3,  2,  -(2.2000000 +  60 * 0.001800));  // MJD 38761 +  60
        checkOffset(1965,  7,  2,  -(2.3000000 + 182 * 0.001900));  // MJD 38761 + 182
        checkOffset(1965,  9,  2,  -(2.4000000 + 244 * 0.002000));  // MJD 38761 + 244
        checkOffset(1966,  1,  2,  -(2.5000000 +   1 * 0.002100));  // MJD 39126 +   1
        checkOffset(1968,  2,  2,  -(2.6000000 + 762 * 0.002200));  // MJD 39126 + 762

        // last linear drift is not stopped as we miss the first constant offset in 1972
        checkOffset(1972,  3,  5,  -(2.6000000 +  2255 * 0.002200));  // MJD 39126 +  2255
        checkOffset(1972,  7, 14,  -(2.6000000 +  2386 * 0.002200));  // MJD 39126 +  2386
        checkOffset(1979, 12, 31,  -(2.6000000 +  5112 * 0.002200));  // MJD 39126 +  5112
        checkOffset(1980,  1, 22,  -(2.6000000 +  5134 * 0.002200));  // MJD 39126 +  5134
        checkOffset(2006,  7,  7,  -(2.6000000 + 14797 * 0.002200));  // MJD 39126 + 14797
        checkOffset(2010,  7,  7,  -(2.6000000 + 16258 * 0.002200));  // MJD 39126 + 16258
        checkOffset(2012,  7,  7,  -(2.6000000 + 16989 * 0.002200));  // MJD 39126 + 16989
        checkOffset(2015,  7,  7,  -(2.6000000 + 18084 * 0.002200));  // MJD 39126 + 18084

    }

    @Test
    public void testInconsistentDate() throws OrekitException {
        checkException("tai-utc-inconsistent-date.dat",
                       OrekitMessages.INCONSISTENT_DATES_IN_IERS_FILE);
    }

    @Test
    public void testNonChronological() throws OrekitException {
        checkException("tai-utc-non-chronological.dat",
                       OrekitMessages.NON_CHRONOLOGICAL_DATES_IN_FILE);
    }

    @Test
    public void testFormatError() throws OrekitException {
        checkException("tai-utc-format-error.dat",
                       OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE);
    }

    private void checkOffset(int year, int month, int day, double offset) throws OrekitException {
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate date = new AbsoluteDate(year, month, day, utc);
        Assert.assertEquals(offset, utc.offsetFromTAI(date), 1.0e-10);
    }

    private void checkException(String name, OrekitMessages message) {
        Utils.setDataRoot("USNO");
        TimeScalesFactory.addUTCTAIOffsetsLoader(new TAIUTCDatFilesLoader(name));
        try {
            TimeScalesFactory.getUTC();
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(message, oe.getSpecifier());
        }
    }

}
