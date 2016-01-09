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
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;

public class UTCTAIHistoryFilesLoaderRegularDataTest {

    @Test
    public void testRegular() throws OrekitException {
        Assert.assertEquals(-32.0, TimeScalesFactory.getUTC().offsetFromTAI(AbsoluteDate.J2000_EPOCH), 10e-8);
    }

    @Test
    public void testFirstLeap() throws OrekitException {
        UTCScale utc = (UTCScale) TimeScalesFactory.getUTC();
        AbsoluteDate afterLeap = new AbsoluteDate(1961, 1, 1, 0, 0, 0.0, utc);
        Assert.assertEquals(1.4228180,
                            afterLeap.durationFrom(utc.getFirstKnownLeapSecond()),
                            1.0e-12);
    }

    @Test
    public void testLaststLeap() throws OrekitException {
        UTCScale utc = (UTCScale) TimeScalesFactory.getUTC();
        AbsoluteDate afterLeap = new AbsoluteDate(2015, 7, 1, 0, 0, 0.0, utc);
        Assert.assertEquals(1.0,
                            afterLeap.durationFrom(utc.getLastKnownLeapSecond()),
                            1.0e-12);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
