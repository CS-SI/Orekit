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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.orekit.files.ccsds.definitions.PocMethodType;

class ShortTermEncounter2DPOCMethodTypeTest {

    @Test
    @DisplayName("Test Alfano2005 enum")
    void testReturnAlfanoMethod() {
        // GIVEN
        final ShortTermEncounter2DPOCMethodType methodEnum = ShortTermEncounter2DPOCMethodType.ALFANO_2005;

        // WHEN
        final ShortTermEncounter2DPOCMethod method          = methodEnum.getMethod();
        final PocMethodType                 ccsdsEquivalent = methodEnum.getCCSDSType();

        // THEN
        Assertions.assertInstanceOf(Alfano2005.class, method);
        Assertions.assertEquals(PocMethodType.ALFANO_2005, ccsdsEquivalent);
    }

    @Test
    @DisplayName("Test Laas2015 enum")
    void testReturnLaasMethod() {
        // GIVEN
        final ShortTermEncounter2DPOCMethodType methodEnum = ShortTermEncounter2DPOCMethodType.LAAS_2015;

        // WHEN
        final ShortTermEncounter2DPOCMethod method          = methodEnum.getMethod();
        final PocMethodType                 ccsdsEquivalent = methodEnum.getCCSDSType();

        // THEN
        Assertions.assertInstanceOf(Laas2015.class, method);
        Assertions.assertNull(ccsdsEquivalent);
    }

    @Test
    @DisplayName("Test Alfriend1999 enum")
    void testReturnAlfriend1999Method() {
        // GIVEN
        final ShortTermEncounter2DPOCMethodType methodEnum =
                ShortTermEncounter2DPOCMethodType.ALFRIEND_1999;

        // WHEN
        final ShortTermEncounter2DPOCMethod method          = methodEnum.getMethod();
        final PocMethodType                 ccsdsEquivalent = methodEnum.getCCSDSType();

        // THEN
        Assertions.assertInstanceOf(Alfriend1999.class, method);
        Assertions.assertEquals(PocMethodType.ALFRIEND_1999, ccsdsEquivalent);
    }

    @Test
    @DisplayName("Test Alfriend1999Max enum")
    void testReturnAlfriendMax1999Method() {
        // GIVEN
        final ShortTermEncounter2DPOCMethodType methodEnum =
                ShortTermEncounter2DPOCMethodType.ALFRIEND_1999_MAX;

        // WHEN
        final ShortTermEncounter2DPOCMethod method          = methodEnum.getMethod();
        final PocMethodType                 ccsdsEquivalent = methodEnum.getCCSDSType();

        // THEN
        Assertions.assertInstanceOf(Alfriend1999Max.class, method);
        Assertions.assertNull(ccsdsEquivalent);
    }

    @Test
    @DisplayName("Test Chan1997 enum")
    void testReturnChan1997Method() {
        // GIVEN
        final ShortTermEncounter2DPOCMethodType methodEnum = ShortTermEncounter2DPOCMethodType.CHAN_1997;

        // WHEN
        final ShortTermEncounter2DPOCMethod method          = methodEnum.getMethod();
        final PocMethodType                 ccsdsEquivalent = methodEnum.getCCSDSType();

        // THEN
        Assertions.assertInstanceOf(Chan1997.class, method);
        Assertions.assertEquals(PocMethodType.CHAN_1997, ccsdsEquivalent);
    }

    @Test
    @DisplayName("Test Patera2005 enum")
    void testReturnPatera2005Method() {
        // GIVEN
        final ShortTermEncounter2DPOCMethodType methodEnum = ShortTermEncounter2DPOCMethodType.PATERA_2005;

        // WHEN
        final ShortTermEncounter2DPOCMethod method          = methodEnum.getMethod();
        final PocMethodType                 ccsdsEquivalent = methodEnum.getCCSDSType();

        // THEN
        Assertions.assertInstanceOf(Patera2005.class, method);
        Assertions.assertEquals(PocMethodType.PATERA_2005, ccsdsEquivalent);
    }

    @Test
    @DisplayName("Test conversion between method and method type")
    void theConversionBetweenMethodAndMethodType() {
        for (ShortTermEncounter2DPOCMethodType methodType : ShortTermEncounter2DPOCMethodType.values()) {
            // GIVEN
            final ShortTermEncounter2DPOCMethod method = methodType.getMethod();

            // WHEN
            final ShortTermEncounter2DPOCMethodType backToMethodType = method.getType();

            // THEN
            Assertions.assertEquals(methodType, backToMethodType);
        }
    }

}
