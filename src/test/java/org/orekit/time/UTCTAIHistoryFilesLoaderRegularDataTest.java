/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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


import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;

public class UTCTAIHistoryFilesLoaderRegularDataTest {

    @Test
    public void testRegular() throws OrekitException {
        assertEquals(-32.0, TimeScalesFactory.getUTC().offsetFromTAI(AbsoluteDate.J2000_EPOCH), 10e-8);
    }

    @Test
    public void testFirstLeap() throws OrekitException {
        UTCScale utc = (UTCScale) TimeScalesFactory.getUTC();
        assertEquals("1971-12-31T23:59:60.000", utc.getFirstKnownLeapSecond().toString(utc));
    }

    @Test
    public void testLaststLeap() throws OrekitException {
        // the data files ends at 2006-01-01,
        // but predefined data also contain the leap second from 2009-01-01
        UTCScale utc = (UTCScale) TimeScalesFactory.getUTC();
        assertEquals("2008-12-31T23:59:60.000", utc.getLastKnownLeapSecond().toString(utc));
    }

    @Before
    public void setUp() {
        String root = getClass().getClassLoader().getResource("regular-data").getPath();
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, root);
    }

}
