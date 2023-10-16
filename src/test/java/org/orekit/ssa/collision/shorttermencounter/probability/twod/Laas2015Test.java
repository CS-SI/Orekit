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
package org.orekit.ssa.collision.shorttermencounter.probability.twod;

import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.util.Binary64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.cdm.Cdm;
import org.orekit.ssa.metrics.FieldProbabilityOfCollision;
import org.orekit.ssa.metrics.ProbabilityOfCollision;

class Laas2015Test {

    /** Systems analysis and architecture laboratory's method to compute probability of collision. */
    private final ShortTermEncounter2DPOCMethod method = new Laas2015();

    @BeforeAll
    static void initializeOrekitData() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    @DisplayName("Chan test case 01")
    void ChanTestCase01() {
        // GIVEN
        final double xm     = 0;
        final double ym     = 10;
        final double sigmaX = 25;
        final double sigmaY = 50;
        final double radius = 5;

        // WHEN
        final ProbabilityOfCollision result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(9.742e-3, result.getValue(), 1e-6);
        Assertions.assertEquals(9.704e-3, result.getLowerLimit(), 1e-6);
        Assertions.assertEquals(9.742e-3, result.getUpperLimit(), 1e-6);
    }

    @Test
    @DisplayName("Chan test case 02")
    void ChanTestCase02() {
        // GIVEN
        final double xm     = 10;
        final double ym     = 0;
        final double sigmaX = 25;
        final double sigmaY = 50;
        final double radius = 5;

        // WHEN
        final ProbabilityOfCollision result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(9.181e-3, result.getValue(), 1e-6);
        Assertions.assertEquals(9.139e-3, result.getLowerLimit(), 1e-6);
        Assertions.assertEquals(9.182e-3, result.getUpperLimit(), 1e-6);
    }

    @Test
    @DisplayName("Chan test case 03")
    void ChanTestCase03() {
        // GIVEN
        final double xm     = 0;
        final double ym     = 10;
        final double sigmaX = 25;
        final double sigmaY = 75;
        final double radius = 5;

        // WHEN
        final ProbabilityOfCollision result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(6.571e-3, result.getValue(), 1e-6);
        Assertions.assertEquals(6.542e-3, result.getLowerLimit(), 1e-6);
        Assertions.assertEquals(6.572e-3, result.getUpperLimit(), 1e-6);
    }

    @Test
    @DisplayName("Chan test case 04")
    void ChanTestCase04() {
        // GIVEN
        final double xm     = 10;
        final double ym     = 0;
        final double sigmaX = 25;
        final double sigmaY = 75;
        final double radius = 5;

        // WHEN
        final ProbabilityOfCollision result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(6.125e-3, result.getValue(), 1e-6);
        Assertions.assertEquals(6.09e-3, result.getLowerLimit(), 1e-5);
        Assertions.assertEquals(6.13e-3, result.getUpperLimit(), 1e-5);
    }

    @Test
    @DisplayName("Chan test case 05")
    void ChanTestCase05() {
        // GIVEN
        final double xm     = 0;
        final double ym     = 1000;
        final double sigmaX = 1000;
        final double sigmaY = 3000;
        final double radius = 10;

        // WHEN
        final ProbabilityOfCollision result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(1.577e-5, result.getValue(), 1e-8);
        Assertions.assertEquals(1.576561e-5, result.getLowerLimit(), 1e-9);
        Assertions.assertEquals(1.576576e-5, result.getUpperLimit(), 1e-9);
    }

    @Test
    @DisplayName("Chan test case 06")
    void ChanTestCase06() {
        // GIVEN
        final double xm     = 1000;
        final double ym     = 0;
        final double sigmaX = 1000;
        final double sigmaY = 3000;
        final double radius = 10;

        // WHEN
        final ProbabilityOfCollision result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(1.011e-5, result.getValue(), 1e-8);
        Assertions.assertEquals(1.010860e-5, result.getLowerLimit(), 1e-11);
        Assertions.assertEquals(1.010883e-5, result.getUpperLimit(), 1e-11);
    }

    @Test
    @DisplayName("Chan test case 07")
    void ChanTestCase07() {
        // GIVEN
        final double xm     = 0;
        final double ym     = 10000;
        final double sigmaX = 1000;
        final double sigmaY = 3000;
        final double radius = 10;

        // WHEN
        final ProbabilityOfCollision result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(6.443e-8, result.getValue(), 1e-11);
        Assertions.assertEquals(6.44304e-8, result.getLowerLimit(), 1e-13);
        Assertions.assertEquals(6.44321e-8, result.getUpperLimit(), 1e-13);
    }

    @Test
    @DisplayName("Chan test case 08")
    void ChanTestCase08() {
        // GIVEN
        final double xm     = 10000;
        final double ym     = 0;
        final double sigmaX = 1000;
        final double sigmaY = 3000;
        final double radius = 10;

        // WHEN
        final ProbabilityOfCollision result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(3.219e-27, result.getValue(), 1e-30);
        Assertions.assertEquals(3.2145e-27, result.getLowerLimit(), 1e-31);
        Assertions.assertEquals(3.2186e-27, result.getUpperLimit(), 1e-31);
    }

    @Test
    @DisplayName("Chan test case 09")
    void ChanTestCase09() {
        // GIVEN
        final double xm     = 0;
        final double ym     = 10000;
        final double sigmaX = 1000;
        final double sigmaY = 10000;
        final double radius = 10;

        // WHEN
        final ProbabilityOfCollision result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(3.033e-6, result.getValue(), 1e-9);
        Assertions.assertEquals(3.03258e-6, result.getLowerLimit(), 1e-11);
        Assertions.assertEquals(3.03261e-6, result.getUpperLimit(), 1e-11);
    }

    @Test
    @DisplayName("Chan test case 10")
    void ChanTestCase10() {
        // GIVEN
        final double xm     = 10000;
        final double ym     = 0;
        final double sigmaX = 1000;
        final double sigmaY = 10000;
        final double radius = 10;

        // WHEN
        final ProbabilityOfCollision result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(9.656e-28, result.getValue(), 1e-31);
        Assertions.assertEquals(9.643e-28, result.getLowerLimit(), 1e-31);
        Assertions.assertEquals(9.656e-28, result.getUpperLimit(), 1e-31);
    }

    @Test
    @DisplayName("Chan test case 11")
    void ChanTestCase11() {
        // GIVEN
        final double xm     = 0;
        final double ym     = 5000;
        final double sigmaX = 1000;
        final double sigmaY = 3000;
        final double radius = 50;

        // WHEN
        final ProbabilityOfCollision result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(1.039e-4, result.getValue(), 1e-7);
        Assertions.assertEquals(1.03831e-4, result.getLowerLimit(), 1e-9);
        Assertions.assertEquals(1.03871e-4, result.getUpperLimit(), 1e-9);
    }

    @Test
    @DisplayName("Chan test case 12")
    void ChanTestCase12() {
        // GIVEN
        final double xm     = 5000;
        final double ym     = 0;
        final double sigmaX = 1000;
        final double sigmaY = 3000;
        final double radius = 50;

        // WHEN
        final ProbabilityOfCollision result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(1.564e-9, result.getValue(), 1e-12);
        Assertions.assertEquals(1.552e-9, result.getLowerLimit(), 1e-12);
        Assertions.assertEquals(1.565e-9, result.getUpperLimit(), 1e-12);
    }

    @Test
    @DisplayName("CSM test case 1")
    void CsmTestCase1() {
        // GIVEN
        final double xm     = 84.875546;
        final double ym     = 60.583685;
        final double sigmaX = 57.918666;
        final double sigmaY = 152.8814468;
        final double radius = 10.3;

        // WHEN
        final ProbabilityOfCollision result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(1.9002e-3, result.getValue(), 1e-7);
        Assertions.assertEquals(1.878e-3, result.getLowerLimit(), 1e-6);
        Assertions.assertEquals(1.900e-3, result.getUpperLimit(), 1e-6);
    }

    @Test
    @DisplayName("CSM test case 2")
    void CsmTestCase2() {
        // GIVEN
        final double xm     = -81.618369;
        final double ym     = 115.055899;
        final double sigmaX = 15.988242;
        final double sigmaY = 5756.840725;
        final double radius = 1.3;

        // WHEN
        final ProbabilityOfCollision result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(2.0553e-11, result.getValue(), 1e-15);
        Assertions.assertEquals(2.0101e-11, result.getLowerLimit(), 1e-15);
        Assertions.assertEquals(2.0557e-11, result.getUpperLimit(), 1e-15);
    }

    @Test
    @DisplayName("CSM test case 3")
    void CsmTestCase3() {
        // GIVEN
        final double xm     = 102.177247;
        final double ym     = 693.405893;
        final double sigmaX = 94.230921;
        final double sigmaY = 643.409272;
        final double radius = 5.3;

        // WHEN
        final ProbabilityOfCollision result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(7.2003e-5, result.getValue(), 1e-9);
        Assertions.assertEquals(7.194e-5, result.getLowerLimit(), 1e-8);
        Assertions.assertEquals(7.200e-5, result.getUpperLimit(), 1e-8);
    }

    @Test
    @DisplayName("CDM test case 1")
    void CdmTestCase1() {
        // GIVEN
        final double xm     = -752.672701;
        final double ym     = 644.939441;
        final double sigmaX = 445.859950;
        final double sigmaY = 6095.858688;
        final double radius = 3.5;

        // WHEN
        final ProbabilityOfCollision result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(5.3904e-7, result.getValue(), 1e-11);
        Assertions.assertEquals(5.3902e-7, result.getLowerLimit(), 1e-11);
        Assertions.assertEquals(5.3904e-7, result.getUpperLimit(), 1e-11);
    }

    @Test
    @DisplayName("CDM test case 2")
    void CdmTestCase2() {
        // GIVEN
        final double xm     = -692.362272;
        final double ym     = 4475.456261;
        final double sigmaX = 193.454603;
        final double sigmaY = 562.027293;
        final double radius = 13.2;

        // WHEN
        final ProbabilityOfCollision result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(2.2796e-20, result.getValue(), 1e-24);
        Assertions.assertEquals(2.2517e-20, result.getLowerLimit(), 1e-24);
        Assertions.assertEquals(2.2797e-20, result.getUpperLimit(), 1e-24);
    }

    @Test
    @DisplayName("Alfano test case 3")
    void AlfanoTestCase3() {
        // GIVEN
        final double xm     = -3.8872073;
        final double ym     = 0.1591646;
        final double sigmaX = 1.4101830;
        final double sigmaY = 114.2585190;
        final double radius = 15;

        // WHEN
        final ProbabilityOfCollision result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(1.0038e-1, result.getValue(), 1e-5);
    }

    /**
     * Alfano test case 5.
     * <p>
     * Initial expected result of 4.4509e-2 has been slightly modified to 4.4507e-2 as this test case is numerically
     * unstable. This has been validated with Romain SERRA.
     * <p>
     * Result obtained here is (slightly) different that what is obtained with the field version because the non-field uses
     * the {@link org.hipparchus.util.MathArrays} linearCombination method to be more precise. However, the field equivalent
     * does not exist.
     */
    @Test
    @DisplayName("Alfano test case 5")
    void AlfanoTestCase5() {
        // GIVEN
        final double xm     = -1.2217895;
        final double ym     = 2.1230067;
        final double sigmaX = 0.0373279;
        final double sigmaY = 177.8109003;
        final double radius = 10;

        // WHEN
        final ProbabilityOfCollision result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(4.4507e-2, result.getValue(), 1e-6);
    }

    @Test
    @DisplayName("Chan test case 04 with custom method constructor and relative boundaries close enough to the desired accuracy")
    void ChanTestCase04WithCustomLaasMethod() {
        // GIVEN
        final ShortTermEncounter2DPOCMethod customMethod = new Laas2015(1e-4, 1000);

        final double xm     = 10;
        final double ym     = 0;
        final double sigmaX = 25;
        final double sigmaY = 75;
        final double radius = 5;

        // WHEN
        final ProbabilityOfCollision result = customMethod.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(6.1e-3, result.getValue(), 1e-4);
    }

    @Test
    @DisplayName(
            "Test probability from a real CDM. It has been mentioned in https://forum.orekit.org/t/collision-package-in-development/2638/16"
                    + " and define an expected probability of collision of 0.004450713 which is higher than the value found using"
                    + " the methods provided in this ssa package because the CSpOC use the projection of a square containing the"
                    + " collision disk instead of the collision disk directly. Hence an overestimation of the probability of"
                    + " collision. This test serves as a non regression test")
    void testComputeProbabilityFromACdm() {

        // GIVEN
        final String     cdmPath = "/ccsds/cdm/ION_SCV8_vs_STARLINK_1233.txt";
        final DataSource data    = new DataSource(cdmPath, () -> getClass().getResourceAsStream(cdmPath));
        final Cdm        cdm     = new ParserBuilder().buildCdmParser().parseMessage(data);

        // Radii taken from comments in the conjunction data message
        final double primaryRadius   = 5;
        final double secondaryRadius = 5;

        // WHEN
        final ProbabilityOfCollision result = method.compute(cdm, primaryRadius, secondaryRadius);

        // THEN
        Assertions.assertEquals(0.0034965176443840897, result.getValue(), 1e-18);
    }

    @Test
    @DisplayName("Chan test case 01 Field version")
    void ChanTestCase01Field() {
        // GIVEN
        final Binary64 xm     = new Binary64(0);
        final Binary64 ym     = new Binary64(10);
        final Binary64 sigmaX = new Binary64(25);
        final Binary64 sigmaY = new Binary64(50);
        final Binary64 radius = new Binary64(5);

        // WHEN
        final FieldProbabilityOfCollision<Binary64> result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(9.742e-3, result.getValue().getReal(), 1e-6);
        Assertions.assertEquals(9.704e-3, result.getLowerLimit().getReal(), 1e-6);
        Assertions.assertEquals(9.742e-3, result.getUpperLimit().getReal(), 1e-6);
    }

    @Test
    @DisplayName("Chan test case 02 Field version")
    void ChanTestCase02Field() {
        // GIVEN
        final Binary64 xm     = new Binary64(10);
        final Binary64 ym     = new Binary64(0);
        final Binary64 sigmaX = new Binary64(25);
        final Binary64 sigmaY = new Binary64(50);
        final Binary64 radius = new Binary64(5);

        // WHEN
        final FieldProbabilityOfCollision<Binary64> result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(9.181e-3, result.getValue().getReal(), 1e-6);
        Assertions.assertEquals(9.139e-3, result.getLowerLimit().getReal(), 1e-6);
        Assertions.assertEquals(9.182e-3, result.getUpperLimit().getReal(), 1e-6);
    }

    @Test
    @DisplayName("Chan test case 03 Field version")
    void ChanTestCase03Field() {
        // GIVEN
        final Binary64 xm     = new Binary64(0);
        final Binary64 ym     = new Binary64(10);
        final Binary64 sigmaX = new Binary64(25);
        final Binary64 sigmaY = new Binary64(75);
        final Binary64 radius = new Binary64(5);

        // WHEN
        final FieldProbabilityOfCollision<Binary64> result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(6.571e-3, result.getValue().getReal(), 1e-6);
        Assertions.assertEquals(6.542e-3, result.getLowerLimit().getReal(), 1e-6);
        Assertions.assertEquals(6.572e-3, result.getUpperLimit().getReal(), 1e-6);
    }

    @Test
    @DisplayName("Chan test case 04 Field version")
    void ChanTestCase04Field() {
        // GIVEN
        final Binary64 xm     = new Binary64(10);
        final Binary64 ym     = new Binary64(0);
        final Binary64 sigmaX = new Binary64(25);
        final Binary64 sigmaY = new Binary64(75);
        final Binary64 radius = new Binary64(5);

        // WHEN
        final FieldProbabilityOfCollision<Binary64> result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(6.125e-3, result.getValue().getReal(), 1e-6);
        Assertions.assertEquals(6.09e-3, result.getLowerLimit().getReal(), 1e-5);
        Assertions.assertEquals(6.13e-3, result.getUpperLimit().getReal(), 1e-5);
    }

    @Test
    @DisplayName("Chan test case 05 Field version")
    void ChanTestCase05Field() {
        // GIVEN
        final Binary64 xm     = new Binary64(0);
        final Binary64 ym     = new Binary64(1000);
        final Binary64 sigmaX = new Binary64(1000);
        final Binary64 sigmaY = new Binary64(3000);
        final Binary64 radius = new Binary64(10);

        // WHEN
        final FieldProbabilityOfCollision<Binary64> result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(1.577e-5, result.getValue().getReal(), 1e-8);
        Assertions.assertEquals(1.576561e-5, result.getLowerLimit().getReal(), 1e-9);
        Assertions.assertEquals(1.576576e-5, result.getUpperLimit().getReal(), 1e-9);
    }

    @Test
    @DisplayName("Chan test case 06 Field version")
    void ChanTestCase06Field() {
        // GIVEN
        final Binary64 xm     = new Binary64(1000);
        final Binary64 ym     = new Binary64(0);
        final Binary64 sigmaX = new Binary64(1000);
        final Binary64 sigmaY = new Binary64(3000);
        final Binary64 radius = new Binary64(10);

        // WHEN
        final FieldProbabilityOfCollision<Binary64> result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(1.011e-5, result.getValue().getReal(), 1e-8);
        Assertions.assertEquals(1.010860e-5, result.getLowerLimit().getReal(), 1e-11);
        Assertions.assertEquals(1.010883e-5, result.getUpperLimit().getReal(), 1e-11);
    }

    @Test
    @DisplayName("Chan test case 07 Field version")
    void ChanTestCase07Field() {
        // GIVEN
        final Binary64 xm     = new Binary64(0);
        final Binary64 ym     = new Binary64(10000);
        final Binary64 sigmaX = new Binary64(1000);
        final Binary64 sigmaY = new Binary64(3000);
        final Binary64 radius = new Binary64(10);

        // WHEN
        final FieldProbabilityOfCollision<Binary64> result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(6.443e-8, result.getValue().getReal(), 1e-11);
        Assertions.assertEquals(6.44304e-8, result.getLowerLimit().getReal(), 1e-13);
        Assertions.assertEquals(6.44321e-8, result.getUpperLimit().getReal(), 1e-13);
    }

    @Test
    @DisplayName("Chan test case 08 Field version")
    void ChanTestCase08Field() {
        // GIVEN
        final Binary64 xm     = new Binary64(10000);
        final Binary64 ym     = new Binary64(0);
        final Binary64 sigmaX = new Binary64(1000);
        final Binary64 sigmaY = new Binary64(3000);
        final Binary64 radius = new Binary64(10);

        // WHEN
        final FieldProbabilityOfCollision<Binary64> result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(3.219e-27, result.getValue().getReal(), 1e-30);
        Assertions.assertEquals(3.2145e-27, result.getLowerLimit().getReal(), 1e-31);
        Assertions.assertEquals(3.2186e-27, result.getUpperLimit().getReal(), 1e-31);
    }

    @Test
    @DisplayName("Chan test case 09 Field version")
    void ChanTestCase09Field() {
        // GIVEN
        final Binary64 xm     = new Binary64(0);
        final Binary64 ym     = new Binary64(10000);
        final Binary64 sigmaX = new Binary64(1000);
        final Binary64 sigmaY = new Binary64(10000);
        final Binary64 radius = new Binary64(10);

        // WHEN
        final FieldProbabilityOfCollision<Binary64> result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(3.033e-6, result.getValue().getReal(), 1e-9);
        Assertions.assertEquals(3.03258e-6, result.getLowerLimit().getReal(), 1e-11);
        Assertions.assertEquals(3.03261e-6, result.getUpperLimit().getReal(), 1e-11);
    }

    @Test
    @DisplayName("Chan test case 10 Field version")
    void ChanTestCase10Field() {
        // GIVEN
        final Binary64 xm     = new Binary64(10000);
        final Binary64 ym     = new Binary64(0);
        final Binary64 sigmaX = new Binary64(1000);
        final Binary64 sigmaY = new Binary64(10000);
        final Binary64 radius = new Binary64(10);

        // WHEN
        final FieldProbabilityOfCollision<Binary64> result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(9.656e-28, result.getValue().getReal(), 1e-31);
        Assertions.assertEquals(9.643e-28, result.getLowerLimit().getReal(), 1e-31);
        Assertions.assertEquals(9.656e-28, result.getUpperLimit().getReal(), 1e-31);
    }

    @Test
    @DisplayName("Chan test case 11 Field version")
    void ChanTestCase11Field() {
        // GIVEN
        final Binary64 xm     = new Binary64(0);
        final Binary64 ym     = new Binary64(5000);
        final Binary64 sigmaX = new Binary64(1000);
        final Binary64 sigmaY = new Binary64(3000);
        final Binary64 radius = new Binary64(50);

        // WHEN
        final FieldProbabilityOfCollision<Binary64> result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(1.039e-4, result.getValue().getReal(), 1e-7);
        Assertions.assertEquals(1.03831e-4, result.getLowerLimit().getReal(), 1e-9);
        Assertions.assertEquals(1.03871e-4, result.getUpperLimit().getReal(), 1e-9);
    }

    @Test
    @DisplayName("Chan test case 12 Field version")
    void ChanTestCase12Field() {
        // GIVEN
        final Binary64 xm     = new Binary64(5000);
        final Binary64 ym     = new Binary64(0);
        final Binary64 sigmaX = new Binary64(1000);
        final Binary64 sigmaY = new Binary64(3000);
        final Binary64 radius = new Binary64(50);

        // WHEN
        final FieldProbabilityOfCollision<Binary64> result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(1.564e-9, result.getValue().getReal(), 1e-12);
        Assertions.assertEquals(1.552e-9, result.getLowerLimit().getReal(), 1e-12);
        Assertions.assertEquals(1.565e-9, result.getUpperLimit().getReal(), 1e-12);
    }

    @Test
    @DisplayName("CSM test case 1 Field version")
    void CsmTestCase1Field() {
        // GIVEN
        final Binary64 xm     = new Binary64(84.875546);
        final Binary64 ym     = new Binary64(60.583685);
        final Binary64 sigmaX = new Binary64(57.918666);
        final Binary64 sigmaY = new Binary64(152.8814468);
        final Binary64 radius = new Binary64(10.3);

        // WHEN
        final FieldProbabilityOfCollision<Binary64> result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(1.9002e-3, result.getValue().getReal(), 1e-7);
        Assertions.assertEquals(1.878e-3, result.getLowerLimit().getReal(), 1e-6);
        Assertions.assertEquals(1.900e-3, result.getUpperLimit().getReal(), 1e-6);
    }

    @Test
    @DisplayName("CSM test case 2 Field version")
    void CsmTestCase2Field() {
        // GIVEN
        final Binary64 xm     = new Binary64(-81.618369);
        final Binary64 ym     = new Binary64(115.055899);
        final Binary64 sigmaX = new Binary64(15.988242);
        final Binary64 sigmaY = new Binary64(5756.840725);
        final Binary64 radius = new Binary64(1.3);

        // WHEN
        final FieldProbabilityOfCollision<Binary64> result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(2.0553e-11, result.getValue().getReal(), 1e-15);
        Assertions.assertEquals(2.0101e-11, result.getLowerLimit().getReal(), 1e-15);
        Assertions.assertEquals(2.0557e-11, result.getUpperLimit().getReal(), 1e-15);
    }

    @Test
    @DisplayName("CSM test case 3 Field version")
    void CsmTestCase3Field() {
        // GIVEN
        final Binary64 xm     = new Binary64(102.177247);
        final Binary64 ym     = new Binary64(693.405893);
        final Binary64 sigmaX = new Binary64(94.230921);
        final Binary64 sigmaY = new Binary64(643.409272);
        final Binary64 radius = new Binary64(5.3);

        // WHEN
        final FieldProbabilityOfCollision<Binary64> result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(7.2003e-5, result.getValue().getReal(), 1e-9);
        Assertions.assertEquals(7.194e-5, result.getLowerLimit().getReal(), 1e-8);
        Assertions.assertEquals(7.200e-5, result.getUpperLimit().getReal(), 1e-8);
    }

    @Test
    @DisplayName("CDM test case 1 Field version")
    void CdmTestCase1Field() {
        // GIVEN
        final Binary64 xm     = new Binary64(-752.672701);
        final Binary64 ym     = new Binary64(644.939441);
        final Binary64 sigmaX = new Binary64(445.859950);
        final Binary64 sigmaY = new Binary64(6095.858688);
        final Binary64 radius = new Binary64(3.5);

        // WHEN
        final FieldProbabilityOfCollision<Binary64> result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(5.3904e-7, result.getValue().getReal(), 1e-11);
        Assertions.assertEquals(5.3902e-7, result.getLowerLimit().getReal(), 1e-11);
        Assertions.assertEquals(5.3904e-7, result.getUpperLimit().getReal(), 1e-11);
    }

    @Test
    @DisplayName("CDM test case 2 Field version")
    void CdmTestCase2Field() {
        // GIVEN
        final Binary64 xm     = new Binary64(-692.362272);
        final Binary64 ym     = new Binary64(4475.456261);
        final Binary64 sigmaX = new Binary64(193.454603);
        final Binary64 sigmaY = new Binary64(562.027293);
        final Binary64 radius = new Binary64(13.2);

        // WHEN
        final FieldProbabilityOfCollision<Binary64> result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(2.2796e-20, result.getValue().getReal(), 1e-24);
        Assertions.assertEquals(2.2517e-20, result.getLowerLimit().getReal(), 1e-24);
        Assertions.assertEquals(2.2797e-20, result.getUpperLimit().getReal(), 1e-24);
    }

    @Test
    @DisplayName("Alfano test case 3 Field version.")
    void AlfanoTestCase3Field() {
        // GIVEN
        final Binary64 xm     = new Binary64(-3.8872073);
        final Binary64 ym     = new Binary64(0.1591646);
        final Binary64 sigmaX = new Binary64(1.4101830);
        final Binary64 sigmaY = new Binary64(114.2585190);
        final Binary64 radius = new Binary64(15);

        // WHEN
        final FieldProbabilityOfCollision<Binary64> result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(1.0038e-1, result.getValue().getReal(), 1e-5);
    }

    /**
     * Alfano test case 5.
     * <p>
     * Initial expected result of 4.4509e-2 has been slightly modified to 4.4515e-2 as this test case is numerically
     * unstable. This has been validated with Romain SERRA.
     * <p>
     * Result obtained here is (slightly) different that what is obtained with the non field version because the non-field
     * uses the {@link org.hipparchus.util.MathArrays} linearCombination method to be more precise. However, the field
     * equivalent does not exist.
     */
    @Test
    @DisplayName("Alfano test case 5 Field version")
    void AlfanoTestCase5Field() {
        // GIVEN
        final Binary64 xm     = new Binary64(-1.2217895);
        final Binary64 ym     = new Binary64(2.1230067);
        final Binary64 sigmaX = new Binary64(0.0373279);
        final Binary64 sigmaY = new Binary64(177.8109003);
        final Binary64 radius = new Binary64(10);

        // WHEN
        final FieldProbabilityOfCollision<Binary64> result = method.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(4.4515e-2, result.getValue().getReal(), 1e-6);
    }

    @Test
    @DisplayName("Chan test case 04 with custom method constructor and relative boundaries close enough to the desired accuracy Field version")
    void ChanTestCase04WithCustomLaasMethodField() {
        // GIVEN
        final ShortTermEncounter2DPOCMethod customMethod = new Laas2015(1e-4, 1000);

        final Binary64 xm     = new Binary64(10);
        final Binary64 ym     = new Binary64(0);
        final Binary64 sigmaX = new Binary64(25);
        final Binary64 sigmaY = new Binary64(75);
        final Binary64 radius = new Binary64(5);

        // WHEN
        final FieldProbabilityOfCollision<Binary64> result = customMethod.compute(xm, ym, sigmaX, sigmaY, radius);

        // THEN
        Assertions.assertEquals(6.1e-3, result.getValue().getReal(), 1e-4);
    }

    @Test
    @DisplayName(
            "Test probability from a real CDM. It has been mentioned in https://forum.orekit.org/t/collision-package-in-development/2638/16"
                    + " and define an expected probability of collision of 0.004450713 which is higher than the value found using"
                    + " the methods provided in this ssa package because the CSpOC use the projection of a square containing the"
                    + " collision disk instead of the collision disk directly. Hence an overestimation of the probability of"
                    + " collision. This test serves as a non regression test")
    void testReturnExpectedProbabilityFromACdmField() {

        // GIVEN
        final String     cdmPath = "/ccsds/cdm/ION_SCV8_vs_STARLINK_1233.txt";
        final DataSource data    = new DataSource(cdmPath, () -> getClass().getResourceAsStream(cdmPath));
        final Cdm        cdm     = new ParserBuilder().buildCdmParser().parseMessage(data);

        // Radii taken from comments in the conjunction data message
        final Binary64 primaryRadius   = new Binary64(5);
        final Binary64 secondaryRadius = new Binary64(5);

        // WHEN
        final FieldProbabilityOfCollision<Binary64> result = method.compute(cdm, primaryRadius, secondaryRadius);

        // THEN
        Assertions.assertEquals(0.0034965176443840893, result.getValue().getReal(), 1e-19);
    }

    @Test
    @DisplayName("Test impact on the probability of collision of a slight difference in combined radius in Chan test case 4")
    void testReturnExpectedValueWhenIntroducingSmallChangeOnCombinedRadius() {
        // GIVEN
        final DSFactory factory = new DSFactory(1, 10);

        final double xmNominal     = 10;
        final double ymNominal     = 0;
        final double sigmaXNominal = 25;
        final double sigmaYNominal = 75;
        final double radiusNominal = 5;
        final double dRadius       = 2.5;

        final DerivativeStructure xm     = factory.constant(xmNominal);
        final DerivativeStructure ym     = factory.constant(ymNominal);
        final DerivativeStructure sigmaX = factory.constant(sigmaXNominal);
        final DerivativeStructure sigmaY = factory.constant(sigmaYNominal);
        final DerivativeStructure radius = factory.variable(0, radiusNominal);

        // WHEN
        final FieldProbabilityOfCollision<DerivativeStructure> resultNominal =
                method.compute(xm, ym, sigmaX, sigmaY, radius);
        final double taylorResult = resultNominal.getValue().taylor(dRadius);
        final double exactResult =
                method.compute(xmNominal, ymNominal, sigmaXNominal, sigmaYNominal, radiusNominal + dRadius).getValue();

        // THEN
        Assertions.assertEquals(6.1e-3, resultNominal.getValue().getReal(), 1e-4);
        Assertions.assertEquals(exactResult, taylorResult, 1e-16);
    }

    @Test
    @DisplayName("Alfano test case 1 (CDM) from NASA CARA")
    void AlfanoCDMTestCase01() {
        // Inputs from NASA CARA
        String cdmName = "AlfanoTestCase01.cdm";
        final double combinedHbr = 15.;

        // Excepted outcome
        final double expectedPc = 0.146749549;
        final double tolerance = 1e-6;

        computeAndCheckCollisionProbability(cdmName, combinedHbr, expectedPc, tolerance);
    }

    @Test
    @DisplayName("Alfano test case 2 (CDM) from NASA CARA")
    void AlfanoCDMTestCase02() {
        // Inputs from NASA CARA
        String cdmName = "AlfanoTestCase02.cdm";
        final double combinedHbr = 4.;

        // Excepted outcome
        final double expectedPc = 0.006222267;
        final double tolerance = 1e-6;

        computeAndCheckCollisionProbability(cdmName, combinedHbr, expectedPc, tolerance);
    }

    @Test
    @DisplayName("Alfano test case 3 (CDM) from NASA CARA")
    void AlfanoCDMTestCase03() {
        // Inputs from NASA CARA
        String cdmName = "AlfanoTestCase03.cdm";
        final double combinedHbr = 15.;

        // Excepted outcome
        final double expectedPc = 0.100351176;
        final double tolerance = 1e-6;

        computeAndCheckCollisionProbability(cdmName, combinedHbr, expectedPc, tolerance);
    }

    @Test
    @DisplayName("Alfano test case 4 (CDM) from NASA CARA")
    void AlfanoCDMTestCase04() {
        // Inputs from NASA CARA
        String cdmName = "AlfanoTestCase04.cdm";
        final double combinedHbr = 15.;

        // Excepted outcome
        final double expectedPc = 0.049323406;
        final double tolerance = 1e-5;

        computeAndCheckCollisionProbability(cdmName, combinedHbr, expectedPc, tolerance);
    }

    @Test
    @DisplayName("Alfano test case 5 (CDM) from NASA CARA")
    void AlfanoCDMTestCase05() {
        // Inputs from NASA CARA
        String cdmName = "AlfanoTestCase05.cdm";
        final double combinedHbr = 10.;

        // Excepted outcome
        final double expectedPc = 0.044487386;
        final double tolerance = 1e-5;

        computeAndCheckCollisionProbability(cdmName, combinedHbr, expectedPc, tolerance);
    }

    @Test
    @DisplayName("Alfano test case 6 (CDM) from NASA CARA")
    void AlfanoCDMTestCase06() {
        // Inputs from NASA CARA
        String cdmName = "AlfanoTestCase06.cdm";
        final double combinedHbr = 10.;

        // Excepted outcome
        final double expectedPc = 0.004335455;
        final double tolerance = 1e-8;

        computeAndCheckCollisionProbability(cdmName, combinedHbr, expectedPc, tolerance);
    }

    @Test
    @DisplayName("Alfano test case 7 (CDM) from NASA CARA")
    void AlfanoCDMTestCase07() {
        // Inputs from NASA CARA
        String cdmName = "AlfanoTestCase07.cdm";
        final double combinedHbr = 10.;

        // Excepted outcome
        final double expectedPc = 0.000158147;
        final double tolerance = 1e-9;

        computeAndCheckCollisionProbability(cdmName, combinedHbr, expectedPc, tolerance);
    }

    @Test
    @DisplayName("Alfano test case 8 (CDM) from NASA CARA")
    void AlfanoCDMTestCase08() {
        // Inputs from NASA CARA
        String cdmName = "AlfanoTestCase08.cdm";
        final double combinedHbr = 4.;

        // Excepted outcome
        final double expectedPc = 0.036948008;
        final double tolerance = 1e-5;

        computeAndCheckCollisionProbability(cdmName, combinedHbr, expectedPc, tolerance);
    }

    @Test
    @DisplayName("Alfano test case 9 (CDM) from NASA CARA")
    void AlfanoCDMTestCase09() {
        // Inputs from NASA CARA
        String cdmName = "AlfanoTestCase09.cdm";
        final double combinedHbr = 6.;

        // Excepted outcome
        final double expectedPc = 0.290146291;
        final double tolerance = 2e-5;

        computeAndCheckCollisionProbability(cdmName, combinedHbr, expectedPc, tolerance);
    }

    // Alfano test case 10 is a duplicate of test 9 in the NASA CARA unit tests.

    @Test
    @DisplayName("Alfano test case 11 (CDM) from NASA CARA")
    void AlfanoCDMTestCase11() {
        // Inputs from NASA CARA
        String cdmName = "AlfanoTestCase11.cdm";
        final double combinedHbr = 4.;

        // Excepted outcome
        final double expectedPc = 0.002672026;
        final double tolerance = 1e-7;

        computeAndCheckCollisionProbability(cdmName, combinedHbr, expectedPc, tolerance);
    }

    private void computeAndCheckCollisionProbability(
            final String cdmName,
            final double combinedHbr,
            final double expected,
            final double tolerance) {

        // Given
        final String     cdmPath = "/ccsds/cdm/" + cdmName;
        final DataSource data    = new DataSource(cdmPath, () -> getClass().getResourceAsStream(cdmPath));
        final Cdm        cdm     = new ParserBuilder().buildCdmParser().parseMessage(data);

        // When
        final ProbabilityOfCollision result = method.compute(cdm, combinedHbr);

        // Then
        Assertions.assertEquals(expected, result.getValue(), tolerance);
    }
}
