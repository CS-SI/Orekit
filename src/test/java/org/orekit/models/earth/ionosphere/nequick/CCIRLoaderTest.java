/* Copyright 2002-2025 Thales Alenia Space
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
package org.orekit.models.earth.ionosphere.nequick;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.DateComponents;

import java.io.IOException;
import java.io.PipedReader;
import java.io.StringReader;

public class CCIRLoaderTest {

    @Test
    public void testAllMonths() {
        for (int month = 1; month <= 12; month++) {
            final CCIRLoader loader = new CCIRLoader();
            loader.loadCCIRCoefficients(new DateComponents(2003, month, 15));
            Assertions.assertEquals( 2, loader.getF2().length);
            Assertions.assertEquals(76, loader.getF2()[0].length);
            Assertions.assertEquals(13, loader.getF2()[0][0].length);
            Assertions.assertEquals( 2, loader.getFm3().length);
            Assertions.assertEquals(49, loader.getFm3()[0].length);
            Assertions.assertEquals( 9, loader.getFm3()[0][0].length);
        }
    }

    @Test
    public void testNoData() {
        try {
            new CCIRLoader().loadData(new DataSource("empty", () -> new StringReader("")));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NEQUICK_F2_FM3_NOT_LOADED, oe.getSpecifier());
            Assertions.assertEquals("empty", oe.getParts()[0]);
        }
    }

    @Test
    public void testIOException() {
        try {
            new CCIRLoader().loadData(new DataSource("exception", () -> new PipedReader()));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(IOException.class, oe.getCause().getClass());
            Assertions.assertEquals(OrekitMessages.NEQUICK_F2_FM3_NOT_LOADED, oe.getSpecifier());
            Assertions.assertEquals("exception", oe.getParts()[0]);
        }
    }

    @Test
    public void testParsingError() {
        try {
            new CCIRLoader().
                loadData(new DataSource("dummy", () -> new StringReader("\n" +
                                                                        "  0.52396593E+01 -0.56523629E-01 -0.18704616E-01  0.12128916E-01\n" +
                                                                        "  0.79412200E-02 -0.10031432E-01  0.21567253E-01 -0.68602669E-02\n" +
                                                                        "  0.37022347E-02  0.78359321E-02  0.63161589E-02 -0.10695398E-01\n" +
                                                                        "  0.29390156E-01  not-a-number   -0.28997501E-01  0.10946779E+00\n")));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(5, (Integer) oe.getParts()[0]);
            Assertions.assertEquals("dummy", oe.getParts()[1]);
            Assertions.assertEquals("0.29390156E-01  not-a-number   -0.28997501E-01  0.10946779E+00", oe.getParts()[2]);
        }
    }

}
