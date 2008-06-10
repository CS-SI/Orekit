/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.cs.orekit.iers;

import java.text.ParseException;

import fr.cs.orekit.errors.OrekitException;
import junit.framework.Test;
import junit.framework.TestSuite;

public class EOPC04FilesLoaderTest extends AbstractFilesLoaderTest {

    public void testMissingMonths() throws OrekitException {
        setRoot("missing-months");
        new EOPC04FilesLoader(eop).loadEOP();
        assertTrue(getMaxGap() > 5);
    }

    public void testStartDate() throws OrekitException, ParseException {
        setRoot("regular-data");
        new EOPC04FilesLoader(eop).loadEOP();
        assertEquals(52640, ((EarthOrientationParameters) eop.first()).getMjd());
    }

    public void testEndDate() throws OrekitException, ParseException {
        setRoot("regular-data");
        new EOPC04FilesLoader(eop).loadEOP();
        assertEquals(53735, ((EarthOrientationParameters) eop.last()).getMjd());
    }

    public static Test suite() {
        return new TestSuite(EOPC04FilesLoaderTest.class);
    }

}
