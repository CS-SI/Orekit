/* Contributed in the public domain.
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
package org.orekit.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class TruncatedCcsdsFormatterTest {

    @Test
    public void testNumber() {

        Assertions.assertEquals("4.940656E-318",          new TruncatedCcsdsFormatter().toString( 4.940656E-318d));
        Assertions.assertEquals("1.18575755E-316",        new TruncatedCcsdsFormatter().toString( 1.18575755E-316d));
        Assertions.assertEquals("2.989102097996E-312",    new TruncatedCcsdsFormatter().toString( 2.989102097996E-312d));
        Assertions.assertEquals("9.0608011534336E15",     new TruncatedCcsdsFormatter().toString( 9.0608011534336E15d));
        Assertions.assertEquals("4.708356024711512E18",   new TruncatedCcsdsFormatter().toString( 4.708356024711512E18d));
        Assertions.assertEquals("9.409340012568248E18",   new TruncatedCcsdsFormatter().toString( 9.409340012568248E18d));
        Assertions.assertEquals("1.853150176586857E21",  new TruncatedCcsdsFormatter().toString( 1.8531501765868567E21d));
        Assertions.assertEquals("-3.347727380279489E33",  new TruncatedCcsdsFormatter().toString(-3.347727380279489E33d));

        // These are truncated
        Assertions.assertEquals("-6.974182466276096E19", new TruncatedCcsdsFormatter().toString(-6.9741824662760956E19d));
        Assertions.assertEquals("4.381605060114784E18",  new TruncatedCcsdsFormatter().toString( 4.3816050601147837E18d));
        Assertions.assertEquals("1.0E-3",  new TruncatedCcsdsFormatter().toString( .00100d));
        Assertions.assertEquals("1.0E0",  new TruncatedCcsdsFormatter().toString( 1.0000000000000004d));
    }

    @Test
    public void testDateNonTruncated() {
        Assertions.assertEquals("2021-03-26T09:45:32.4576",
                new TruncatedCcsdsFormatter().toString(2021, 3, 26, 9, 45, 32.4576d));
        Assertions.assertEquals("2021-03-26T09:45:00.00000000000001",
                new TruncatedCcsdsFormatter().toString(2021, 3, 26, 9, 45, 1.0e-14d));
    }

    @Test
    public void testDateTruncated() throws IOException {
        Assertions.assertEquals("2021-03-26T09:45:00.0",
                new TruncatedCcsdsFormatter().toString(2021, 3, 26, 9, 45, 1.0e-16d));
        Assertions.assertEquals("2021-03-26T09:45:12.34567891234568",
                new TruncatedCcsdsFormatter().toString(2021, 3, 26, 9, 45, 12.3456789123456789d));
    }


    /**
     * This test contains edge cases that are or would result in invalid date times.
     * This demonstrates that the formatter will NOT catch/correct any invalid inputs and shows how the invalid inputs will
     * be formatted.
     */
    @Test
    public void testInvalidEdgeCases() {
        Assertions.assertEquals("2021-03-26T09:45:59.99999999999999",
                new TruncatedCcsdsFormatter().toString(2021, 3, 26, 9, 45, Math.nextDown(60.0)));
        Assertions.assertEquals("20210-300-260T900:450:600.0",
                new TruncatedCcsdsFormatter().toString(20210, 300, 260, 900, 450, 600.0));
        Assertions.assertEquals("-2021--3--26T-9:-45:-01.0",
                new TruncatedCcsdsFormatter().toString(-2021, -3, -26, -9, -45, -1.0));
    }

}

