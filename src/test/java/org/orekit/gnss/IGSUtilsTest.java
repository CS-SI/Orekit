/* Copyright 2002-2024 Thales Alenia Space
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
package org.orekit.gnss;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.FactoryManagedFrame;
import org.orekit.frames.ITRFVersion;
import org.orekit.frames.Predefined;
import org.orekit.frames.VersionedITRF;

public class IGSUtilsTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("gnss");
    }

    @Test
    public void testGuessFrame() {
        Assertions.assertSame(ITRFVersion.ITRF_1996,
                              ((VersionedITRF) IGSUtils.guessFrame("ITRF96")).getITRFVersion());
        Assertions.assertSame(ITRFVersion.ITRF_2014,
                              ((VersionedITRF) IGSUtils.guessFrame("IGS14")).getITRFVersion());
        Assertions.assertSame(ITRFVersion.ITRF_2020,
                              ((VersionedITRF) IGSUtils.guessFrame("ITR20")).getITRFVersion());
        Assertions.assertSame(ITRFVersion.ITRF_2008,
                              ((VersionedITRF) IGSUtils.guessFrame("SLR08")).getITRFVersion());
        Assertions.assertSame(Predefined.ITRF_CIO_CONV_2010_ACCURATE_EOP,
                              ((FactoryManagedFrame) IGSUtils.guessFrame("UNDEF")).getFactoryKey());
        Assertions.assertSame(Predefined.ITRF_CIO_CONV_2010_ACCURATE_EOP,
                              ((FactoryManagedFrame) IGSUtils.guessFrame("WGS84")).getFactoryKey());
    }

}
