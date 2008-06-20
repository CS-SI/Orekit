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
package org.orekit.iers;

import java.text.ParseException;

import org.orekit.errors.OrekitException;
import org.orekit.iers.IERSDirectoryCrawler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChunkedDate;
import org.orekit.time.ChunkedTime;
import org.orekit.time.UTCScale;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class UTCTAIHistoryFilesLoaderRegularDataTest extends TestCase {

    public void testRegular() throws OrekitException {
        assertEquals(-32.0, UTCScale.getInstance().offsetFromTAI(946684800), 10e-8);
    }

    public void testUTCDate() throws OrekitException, ParseException {
        UTCScale scale = (UTCScale) UTCScale.getInstance();
        AbsoluteDate startDate = scale.getStartDate();
        double delta = startDate.minus(new AbsoluteDate(new ChunkedDate(1972, 01, 01),
                                                        ChunkedTime.H00, scale));
        assertEquals(0, delta, 0);
    }

    public void setUp() {
        System.setProperty(IERSDirectoryCrawler.IERS_ROOT_DIRECTORY, "regular-data");
    }

    public static Test suite() {
        return new TestSuite(UTCTAIHistoryFilesLoaderRegularDataTest.class);
    }

}
