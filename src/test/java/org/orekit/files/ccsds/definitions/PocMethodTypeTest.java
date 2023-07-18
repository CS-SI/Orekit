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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.orekit.ssa.collision.shorttermencounter.probability.twod.ShortTermEncounter2DPOCMethodType;

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

    @Test
    @DisplayName("Test link to ShortTermEncounter2DPOCMethodType")
    void should_return_null() {
        // Given
        final PocMethodType akellaalFriend2000   = PocMethodType.AKELLAALFRIEND_2000;
        final PocMethodType foster1992           = PocMethodType.FOSTER_1992;
        final PocMethodType patera2001           = PocMethodType.PATERA_2001;
        final PocMethodType patera2003           = PocMethodType.PATERA_2003;
        final PocMethodType chan2003             = PocMethodType.CHAN_2003;
        final PocMethodType alfanoTubes2007      = PocMethodType.ALFANO_TUBES_2007;
        final PocMethodType alfanoVoxels2006     = PocMethodType.ALFANO_VOXELS_2006;
        final PocMethodType alfanoParal2007      = PocMethodType.ALFANO_PARAL_2007;
        final PocMethodType alfanoMaxProbability = PocMethodType.ALFANO_MAX_PROBABILITY;
        final PocMethodType mckinley2006         = PocMethodType.MCKINLEY_2006;

        // When
        final ShortTermEncounter2DPOCMethodType akellaalFriend2000Equivalent =
                akellaalFriend2000.getMethodType();
        final ShortTermEncounter2DPOCMethodType foster1992Equivalent       = foster1992.getMethodType();
        final ShortTermEncounter2DPOCMethodType patera2001Equivalent       = patera2001.getMethodType();
        final ShortTermEncounter2DPOCMethodType patera2003Equivalent       = patera2003.getMethodType();
        final ShortTermEncounter2DPOCMethodType chan2003Equivalent         = chan2003.getMethodType();
        final ShortTermEncounter2DPOCMethodType alfanoTubes2007Equivalent  = alfanoTubes2007.getMethodType();
        final ShortTermEncounter2DPOCMethodType alfanoVoxels2006Equivalent = alfanoVoxels2006.getMethodType();
        final ShortTermEncounter2DPOCMethodType alfanoParal2007Equivalent  = alfanoParal2007.getMethodType();
        final ShortTermEncounter2DPOCMethodType alfanoMaxProbabilityEquivalent =
                alfanoMaxProbability.getMethodType();
        final ShortTermEncounter2DPOCMethodType mckinley2006Equivalent = mckinley2006.getMethodType();

        // Then
        Assertions.assertNull(akellaalFriend2000Equivalent);
        Assertions.assertNull(foster1992Equivalent);
        Assertions.assertNull(patera2001Equivalent);
        Assertions.assertNull(patera2003Equivalent);
        Assertions.assertNull(chan2003Equivalent);
        Assertions.assertNull(alfanoTubes2007Equivalent);
        Assertions.assertNull(alfanoVoxels2006Equivalent);
        Assertions.assertNull(alfanoParal2007Equivalent);
        Assertions.assertNull(alfanoMaxProbabilityEquivalent);
        Assertions.assertNull(mckinley2006Equivalent);

    }

    @Test
    @DisplayName("Test link to ShortTermEncounter2DPOCMethodType")
    void should_return_corresponding_type() {
        // Given
        final PocMethodType chan1997     = PocMethodType.CHAN_1997;
        final PocMethodType alfriend1999 = PocMethodType.ALFRIEND_1999;
        final PocMethodType alfano2005   = PocMethodType.ALFANO_2005;
        final PocMethodType patera2005   = PocMethodType.PATERA_2005;

        // When
        final ShortTermEncounter2DPOCMethodType chan1997Equivalent     = chan1997.getMethodType();
        final ShortTermEncounter2DPOCMethodType alfriend1999Equivalent = alfriend1999.getMethodType();
        final ShortTermEncounter2DPOCMethodType alfano2005Equivalent   = alfano2005.getMethodType();
        final ShortTermEncounter2DPOCMethodType patera2005Equivalent   = patera2005.getMethodType();

        // Then
        Assertions.assertEquals(ShortTermEncounter2DPOCMethodType.CHAN_1997, chan1997Equivalent);
        Assertions.assertEquals(ShortTermEncounter2DPOCMethodType.ALFRIEND_1999, alfriend1999Equivalent);
        Assertions.assertEquals(ShortTermEncounter2DPOCMethodType.ALFANO_2005, alfano2005Equivalent);
        Assertions.assertEquals(ShortTermEncounter2DPOCMethodType.PATERA_2005, patera2005Equivalent);

    }
}
