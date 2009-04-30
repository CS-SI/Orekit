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
package org.orekit.frames;

import java.text.ParseException;

import org.orekit.data.AbstractFilesLoaderTest;
import org.orekit.errors.OrekitException;
import org.orekit.frames.BulletinBFilesLoader;
import org.orekit.utils.TimeStampedEntry;

import junit.framework.Test;
import junit.framework.TestSuite;

public class BulletinBFilesLoaderTest extends AbstractFilesLoaderTest {

    /** Regular name for the BulletinB files (IAU2000 compatibles). */
    private static final String BULLETFILENAME = "^bulletinb_IAU2000((\\.\\d\\d\\d)|(-\\d\\d\\d\\.txt))$";

    public void testMissingMonths() throws OrekitException {
        setRoot("missing-months");
        new BulletinBFilesLoader(BULLETFILENAME, set).loadEOP();
        assertTrue(getMaxGap() > 5);
    }

    public void testStartDate() throws OrekitException, ParseException {
        setRoot("regular-data");
        new BulletinBFilesLoader(BULLETFILENAME, set).loadEOP();
        assertTrue(getMaxGap() < 5);
        assertEquals(53709, ((TimeStampedEntry) set.first()).getMjd());
    }

    public void testEndDate() throws OrekitException, ParseException {
        setRoot("regular-data");
        new BulletinBFilesLoader(BULLETFILENAME, set).loadEOP();
        assertTrue(getMaxGap() < 5);
        assertEquals(53799, ((TimeStampedEntry) set.last()).getMjd());
    }

    public static Test suite() {
        return new TestSuite(BulletinBFilesLoaderTest.class);
    }

}
