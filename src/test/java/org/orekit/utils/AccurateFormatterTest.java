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
package org.orekit.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AccurateFormatterTest {

    @Test
    public void testNumber() {
        // these tests come from the RyuDouble tests
        Assertions.assertEquals("4.940656E-318",          AccurateFormatter.format( 4.940656E-318d));
        Assertions.assertEquals("1.18575755E-316",        AccurateFormatter.format( 1.18575755E-316d));
        Assertions.assertEquals("2.989102097996E-312",    AccurateFormatter.format( 2.989102097996E-312d));
        Assertions.assertEquals("9.0608011534336E15",     AccurateFormatter.format( 9.0608011534336E15d));
        Assertions.assertEquals("4.708356024711512E18",   AccurateFormatter.format( 4.708356024711512E18));
        Assertions.assertEquals("9.409340012568248E18",   AccurateFormatter.format( 9.409340012568248E18));
        Assertions.assertEquals("1.8531501765868567E21",  AccurateFormatter.format( 1.8531501765868567E21));
        Assertions.assertEquals("-3.347727380279489E33",  AccurateFormatter.format(-3.347727380279489E33));
        Assertions.assertEquals("-6.9741824662760956E19", AccurateFormatter.format(-6.9741824662760956E19));
        Assertions.assertEquals("4.3816050601147837E18",  AccurateFormatter.format( 4.3816050601147837E18));
    }

    @Test
    public void testDateNonTruncated() {
        Assertions.assertEquals("2021-03-26T09:45:32.4576",
                            AccurateFormatter.format(2021, 3, 26, 9, 45, 32.4576));
        Assertions.assertEquals("2021-03-26T09:45:00.00000000000001",
                            AccurateFormatter.format(2021, 3, 26, 9, 45, 1.0e-14));
    }

    @Test
    public void testDateTruncated() {
        Assertions.assertEquals("2021-03-26T09:45:00.0",
                            AccurateFormatter.format(2021, 3, 26, 9, 45, 1.0e-16));
    }

}

