/* Copyright 2002-2023 CS GROUP
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.DateComponents;

public class KlobucharIonoCoefficientsLoaderTest {

    @Test
    /**
     * Regular test for 1st of January 2017
     */
    public void testRegularFile() {

        Utils.setDataRoot("klobuchar-ionospheric-coefficients");

        KlobucharIonoCoefficientsLoader ionoLoader = new KlobucharIonoCoefficientsLoader();
        DateComponents dateComponents = new DateComponents(2017, 1);
        ionoLoader.loadKlobucharIonosphericCoefficients(dateComponents);

        final double alpha[] = ionoLoader.getAlpha();
        final double beta[]  = ionoLoader.getBeta();

        Assertions.assertEquals(1.2821e-08 , alpha[0], 1e-16);
        Assertions.assertEquals(-9.6222e-09, alpha[1], 1e-16);
        Assertions.assertEquals(-3.5982e-07, alpha[2], 1e-16);
        Assertions.assertEquals(-6.0901e-07, alpha[3], 1e-16);

        Assertions.assertEquals(1.0840e+05 , beta[0], 1e-16);
        Assertions.assertEquals(-1.3197e+05, beta[1], 1e-16);
        Assertions.assertEquals(-2.6331e+05, beta[2], 1e-16);
        Assertions.assertEquals(4.0570e+05 , beta[3], 1e-16);
    }

    @Test
    /**
     * Test of a corrupted file without keyword ALPHA
     */
    public void testCorruptedFileBadKeyword() {

        Utils.setDataRoot("klobuchar-ionospheric-coefficients");
        final String fileName = "corrupted-bad-keyword-CGIM0020.17N";
        KlobucharIonoCoefficientsLoader ionoLoader =
                        new KlobucharIonoCoefficientsLoader(fileName);

        try {
            ionoLoader.loadKlobucharIonosphericCoefficients();
            Assertions.fail("An exception should have been thrown");

        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NO_KLOBUCHAR_ALPHA_BETA_IN_FILE, oe.getSpecifier());
            Assertions.assertTrue(((String) oe.getParts()[0]).endsWith(fileName));
        }
    }

    @Test
    /**
     * Test of a corrupted file with improper data
     */
    public void testCorruptedFileBadData() {

        Utils.setDataRoot("klobuchar-ionospheric-coefficients");
        final String fileName = "corrupted-bad-data-CGIM0020.17N";
        KlobucharIonoCoefficientsLoader ionoLoader =
                        new KlobucharIonoCoefficientsLoader(fileName);

        try {
            ionoLoader.loadKlobucharIonosphericCoefficients();
            Assertions.fail("An exception should have been thrown");

        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
        }
    }

    @Test
    /**
     * Test for a file that cannot be found
     */
    public void testAbsentFile() {

        Utils.setDataRoot("klobuchar-ionospheric-coefficients");
        KlobucharIonoCoefficientsLoader ionoLoader = new KlobucharIonoCoefficientsLoader();
        DateComponents dateComponents = new DateComponents(2017, 3);

        try {
            ionoLoader.loadKlobucharIonosphericCoefficients(dateComponents);
            Assertions.fail("An exception should have been thrown");

        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.KLOBUCHAR_ALPHA_BETA_NOT_AVAILABLE_FOR_DATE,
                                oe.getSpecifier());
            Assertions.assertEquals(dateComponents.toString(),
                                (String) oe.getParts()[0]);
        }
    }
}
