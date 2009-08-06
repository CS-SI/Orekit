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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;

import org.junit.Test;
import org.orekit.data.AbstractFilesLoaderTest;
import org.orekit.errors.OrekitException;
import org.orekit.frames.EOP05C04FilesLoader;
import org.orekit.utils.TimeStampedEntry;


public class EOPC04FilesLoaderTest extends AbstractFilesLoaderTest {

    /** Regular name for the EOPC04 files (IAU2000 compatibles). */
    private static final String EOPC04FILENAME = "^eopc04_IAU2000\\.(\\d\\d)$";

    @Test
    public void testMissingMonths() throws OrekitException {
        setRoot("missing-months");
        new EOP05C04FilesLoader(EOPC04FILENAME, set).loadEOP();
        assertTrue(getMaxGap() > 5);
    }

    @Test
    public void testStartDate() throws OrekitException, ParseException {
        setRoot("regular-data");
        new EOP05C04FilesLoader(EOPC04FILENAME, set).loadEOP();
        assertEquals(52640, ((TimeStampedEntry) set.first()).getMjd());
    }

    @Test
    public void testEndDate() throws OrekitException, ParseException {
        setRoot("regular-data");
        new EOP05C04FilesLoader(EOPC04FILENAME, set).loadEOP();
        assertEquals(53735, ((TimeStampedEntry) set.last()).getMjd());
    }

}
