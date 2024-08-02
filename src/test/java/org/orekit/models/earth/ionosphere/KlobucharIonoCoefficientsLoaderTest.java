/* Copyright 2002-2024 CS GROUP
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

package org.orekit.models.earth.ionosphere;

import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.DateComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class KlobucharIonoCoefficientsLoaderTest {

    /**
     * Regular test for 1st of January 2017
     */
    @Test
    void testRegularFile() {

        Utils.setDataRoot("klobuchar-ionospheric-coefficients");

        KlobucharIonoCoefficientsLoader ionoLoader = new KlobucharIonoCoefficientsLoader();
        DateComponents dateComponents = new DateComponents(2017, 1);
        ionoLoader.loadKlobucharIonosphericCoefficients(dateComponents);

        final double alpha[] = ionoLoader.getAlpha();
        final double beta[]  = ionoLoader.getBeta();

        assertEquals(1.2821e-08 , alpha[0], 1e-16);
        assertEquals(-9.6222e-09, alpha[1], 1e-16);
        assertEquals(-3.5982e-07, alpha[2], 1e-16);
        assertEquals(-6.0901e-07, alpha[3], 1e-16);

        assertEquals(1.0840e+05 , beta[0], 1e-16);
        assertEquals(-1.3197e+05, beta[1], 1e-16);
        assertEquals(-2.6331e+05, beta[2], 1e-16);
        assertEquals(4.0570e+05 , beta[3], 1e-16);
    }

    /**
     * Test of a corrupted file without keyword ALPHA
     */
    @Test
    void testCorruptedFileBadKeyword() {

        Utils.setDataRoot("klobuchar-ionospheric-coefficients");
        final String fileName = "corrupted-bad-keyword-CGIM0020.17N";
        KlobucharIonoCoefficientsLoader ionoLoader =
                        new KlobucharIonoCoefficientsLoader(fileName);

        try {
            ionoLoader.loadKlobucharIonosphericCoefficients();
            fail("An exception should have been thrown");

        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.NO_KLOBUCHAR_ALPHA_BETA_IN_FILE, oe.getSpecifier());
            assertTrue(((String) oe.getParts()[0]).endsWith(fileName));
        }
    }

    /**
     * Test of a corrupted file with improper data
     */
    @Test
    void testCorruptedFileBadData() {

        Utils.setDataRoot("klobuchar-ionospheric-coefficients");
        final String fileName = "corrupted-bad-data-CGIM0020.17N";
        KlobucharIonoCoefficientsLoader ionoLoader =
                        new KlobucharIonoCoefficientsLoader(fileName);

        try {
            ionoLoader.loadKlobucharIonosphericCoefficients();
            fail("An exception should have been thrown");

        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
        }
    }

    /**
     * Test for a file that cannot be found
     */
    @Test
    void testAbsentFile() {

        Utils.setDataRoot("klobuchar-ionospheric-coefficients");
        KlobucharIonoCoefficientsLoader ionoLoader = new KlobucharIonoCoefficientsLoader();
        DateComponents dateComponents = new DateComponents(2017, 3);

        try {
            ionoLoader.loadKlobucharIonosphericCoefficients(dateComponents);
            fail("An exception should have been thrown");

        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.KLOBUCHAR_ALPHA_BETA_NOT_AVAILABLE_FOR_DATE,
                                oe.getSpecifier());
            assertEquals(dateComponents.toString(),
                                (String) oe.getParts()[0]);
        }
    }
}
