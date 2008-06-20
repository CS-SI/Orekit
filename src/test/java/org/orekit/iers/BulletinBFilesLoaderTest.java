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
import org.orekit.iers.BulletinBFilesLoader;
import org.orekit.iers.EarthOrientationParameters;

import junit.framework.Test;
import junit.framework.TestSuite;

public class BulletinBFilesLoaderTest extends AbstractFilesLoaderTest {

    public void testMissingMonths() throws OrekitException {
        setRoot("missing-months");
        new BulletinBFilesLoader(eop).loadEOP();
        assertTrue(getMaxGap() > 5);
    }

    public void testStartDate() throws OrekitException, ParseException {
        setRoot("regular-data");
        new BulletinBFilesLoader(eop).loadEOP();
        assertTrue(getMaxGap() < 5);
        assertEquals(53709, ((EarthOrientationParameters) eop.first()).getMjd());
    }

    public void testEndDate() throws OrekitException, ParseException {
        setRoot("regular-data");
        new BulletinBFilesLoader(eop).loadEOP();
        assertTrue(getMaxGap() < 5);
        assertEquals(53799, ((EarthOrientationParameters) eop.last()).getMjd());
    }


    public static Test suite() {
        return new TestSuite(BulletinBFilesLoaderTest.class);
    }

}
