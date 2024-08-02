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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.EOPBasedTransformProvider;
import org.orekit.frames.FactoryManagedFrame;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.ITRFVersion;
import org.orekit.frames.Predefined;
import org.orekit.frames.VersionedITRF;
import org.orekit.utils.IERSConventions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

class IGSUtilsTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("gnss");
    }

    @Test
    void testItrfVersion() {
        assertSame(ITRFVersion.ITRF_1996, getItrfVersion("ITRF96"));
        assertSame(ITRFVersion.ITRF_2014, getItrfVersion("IGS14"));
        assertSame(ITRFVersion.ITRF_2020, getItrfVersion("ITR20"));
        assertSame(ITRFVersion.ITRF_2008, getItrfVersion("SLR08"));
    }

    @Test
    void testUnknown() {
        assertSame(Predefined.ITRF_CIO_CONV_2010_ACCURATE_EOP,
                              ((FactoryManagedFrame) IGSUtils.guessFrame("UNDEF")).getFactoryKey());
        assertSame(Predefined.ITRF_CIO_CONV_2010_ACCURATE_EOP,
                              ((FactoryManagedFrame) IGSUtils.guessFrame("WGS84")).getFactoryKey());
    }

    @Test
    void testIersConvention() {
        assertSame(IERSConventions.IERS_1996, getConvention("ITRF88"));
        assertSame(IERSConventions.IERS_1996, getConvention("ITRF89"));
        assertSame(IERSConventions.IERS_1996, getConvention("ITRF96"));
        assertSame(IERSConventions.IERS_1996, getConvention("ITRF00"));
        assertSame(IERSConventions.IERS_2003, getConvention("ITRF05"));
        assertSame(IERSConventions.IERS_2003, getConvention("ITRF08"));
        assertSame(IERSConventions.IERS_2010, getConvention("ITRF14"));
        assertSame(IERSConventions.IERS_2010, getConvention("ITRF20"));
    }

    @Test
    void testGcrf() {
        assertNull(IGSUtils.guessFrame("GCRF").getParent());
        assertNull(IGSUtils.guessFrame(" GCRF").getParent());
        assertNull(IGSUtils.guessFrame("GCRF ").getParent());
    }

    @Test
    void testEME2000() {
        assertSame(FramesFactory.getEME2000(), IGSUtils.guessFrame("EME00"));
        assertSame(FramesFactory.getEME2000(), IGSUtils.guessFrame("EME2K"));
    }

    @Test
    void testITRFNames() {
        assertEquals("IGS89", IGSUtils.frameName(FramesFactory.getITRF(ITRFVersion.ITRF_1989,
                                                                                  IERSConventions.IERS_2010,
                                                                                  false)));
        assertEquals("IGS96", IGSUtils.frameName(FramesFactory.getITRF(ITRFVersion.ITRF_1996,
                                                                                  IERSConventions.IERS_2010,
                                                                                  false)));
        assertEquals("IGS00", IGSUtils.frameName(FramesFactory.getITRF(ITRFVersion.ITRF_2000,
                                                                                  IERSConventions.IERS_2010,
                                                                                  false)));
        assertEquals("IGS05", IGSUtils.frameName(FramesFactory.getITRF(ITRFVersion.ITRF_2005,
                                                                                  IERSConventions.IERS_2010,
                                                                                  false)));
        assertEquals("IGS14", IGSUtils.frameName(FramesFactory.getITRF(ITRFVersion.ITRF_2014,
                                                                                  IERSConventions.IERS_2010,
                                                                                  false)));
        assertEquals("IGS20", IGSUtils.frameName(FramesFactory.getITRF(ITRFVersion.ITRF_2020,
                                                                                  IERSConventions.IERS_2010,
                                                                                  false)));
    }

    @Test
    void testInertialNames() {
        assertEquals("GCRF",  IGSUtils.frameName(FramesFactory.getGCRF()));
        assertEquals("EME2K", IGSUtils.frameName(FramesFactory.getEME2000()));
    }

    @Test
    void testUnsupportedFrame() {
        try {
            IGSUtils.frameName(FramesFactory.getMOD(IERSConventions.IERS_2010));
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.FRAME_NOT_ALLOWED, oe.getSpecifier());
            assertEquals("MOD/2010", oe.getParts()[0]);
        }
    }

    private ITRFVersion getItrfVersion(String key) {
        return ((VersionedITRF) IGSUtils.guessFrame(key)).getITRFVersion();
    }

   private IERSConventions getConvention(final String key) {
        final Frame frame = IGSUtils.guessFrame(key);
        final EOPBasedTransformProvider provider =
            (EOPBasedTransformProvider) frame.getTransformProvider();
        return provider.getEOPHistory().getConventions();
    }

}
