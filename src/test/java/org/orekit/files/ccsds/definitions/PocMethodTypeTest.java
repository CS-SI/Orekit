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
package org.orekit.files.ccsds.definitions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PocMethodTypeTest {
    @Test
    void testConversionToCCSDSName() {
        // GIVEN
        final PocMethodType alfano2005 = PocMethodType.ALFANO_2005;
        final PocMethodType chan1997   = PocMethodType.CHAN_1997;
        final PocMethodType chan2003   = PocMethodType.CHAN_2003;

        // WHEN
        final String ccsdsAlfano2005 = alfano2005.getCCSDSName();
        final String ccsdsChan1997   = chan1997.getCCSDSName();
        final String ccsdsChan2003   = chan2003.getCCSDSName();

        // THEN
        Assertions.assertEquals(ccsdsAlfano2005, "ALFANO-2005");
        Assertions.assertEquals(ccsdsChan1997, "CHAN-1997");
        Assertions.assertEquals(ccsdsChan2003, "CHAN-2003");
    }
}
