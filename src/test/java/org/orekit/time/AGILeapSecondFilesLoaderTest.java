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
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

public class AGILeapSecondFilesLoaderTest {

    @Test
    public void testRegularFile() {

        Utils.setDataRoot("AGI");

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
    public void testInconsistentDate() {
        checkException("LeapSecond-inconsistent-date.dat",
                       OrekitMessages.INCONSISTENT_DATES_IN_IERS_FILE);
    }

    @Test
    public void testNonChronological() {
        checkException("LeapSecond-non-chronological.dat",
                       OrekitMessages.NON_CHRONOLOGICAL_DATES_IN_FILE);
    }

    @Test
    public void testFormatError() {
        checkException("LeapSecond-format-error.dat",
                       OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE);
    }

    private void checkOffset(int year, int month, int day, double offset) {
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate date = new AbsoluteDate(year, month, day, utc);
        Assertions.assertEquals(offset, utc.offsetFromTAI(date), 1.0e-10);
    }

    private void checkException(String name, OrekitMessages message) {
        Utils.setDataRoot("AGI");
        TimeScalesFactory.addUTCTAIOffsetsLoader(new AGILeapSecondFilesLoader(name));
        try {
            TimeScalesFactory.getUTC();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(message, oe.getSpecifier());
        }
    }

}
