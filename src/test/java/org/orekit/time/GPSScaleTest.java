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

import org.orekit.errors.OrekitException;
import org.orekit.iers.IERSDirectoryCrawler;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class GPSScaleTest
extends TestCase {

    public GPSScaleTest(String name) {
        super(name);
    }

    public void testT0() {
        AbsoluteDate t0 = new AbsoluteDate(new ChunkedDate(1980, 1, 6),
                                           new ChunkedTime(0, 0, 0),
                                           GPSScale.getInstance());
        assertEquals(AbsoluteDate.GPS_EPOCH, t0);
    }

    public void testArbitrary() throws OrekitException {
        AbsoluteDate tGPS =
            new AbsoluteDate(new ChunkedDate(1999, 3, 4), ChunkedTime.H00, GPSScale.getInstance());
        AbsoluteDate tUTC =
            new AbsoluteDate(new ChunkedDate(1999, 3, 3), new ChunkedTime(23, 59, 47),
                             UTCScale.getInstance());
        assertEquals(tUTC, tGPS);
    }

    public void testSymetry() {
        // the loop is around the 1977-01-01 leap second introduction
        double tLeap = 220924815;
        TimeScale scale = GPSScale.getInstance();
        assertEquals("GPS", scale.toString());
        for (double taiTime = tLeap - 60; taiTime < tLeap + 60; taiTime += 0.3) {
            double dt1 = scale.offsetFromTAI(taiTime);
            double dt2 = scale.offsetToTAI(taiTime + dt1);
            assertEquals( 0.0, dt1 + dt2, 1.0e-10);
        }
    }

    public void setUp() {
        System.setProperty(IERSDirectoryCrawler.IERS_ROOT_DIRECTORY, "regular-data");
    }

    public static Test suite() {
        return new TestSuite(GPSScaleTest.class);
    }

}
