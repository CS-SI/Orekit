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

import org.orekit.data.DataDirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.UTCScale;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class UTCTAIHistoryFilesLoaderNoDataTest extends TestCase {

    public void testNoData() throws OrekitException {
        // despite there is no data files, some leap seconds are predefined in the library
        assertEquals(-32.0, UTCScale.getInstance().offsetFromTAI(AbsoluteDate.J2000_EPOCH), 1.0e-8);
    }

    public void setUp() {
        String root = getClass().getClassLoader().getResource("no-data").getPath();
        System.setProperty(DataDirectoryCrawler.OREKIT_DATA_PATH, root);
    }

    public static Test suite() {
        return new TestSuite(UTCTAIHistoryFilesLoaderNoDataTest.class);
    }

}
