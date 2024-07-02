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

public class IGSUtilsTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("gnss");
    }

    @Test
    public void testItrfVersion() {
        Assertions.assertSame(ITRFVersion.ITRF_1996, getItrfVersion("ITRF96"));
        Assertions.assertSame(ITRFVersion.ITRF_2014, getItrfVersion("IGS14"));
        Assertions.assertSame(ITRFVersion.ITRF_2020, getItrfVersion("ITR20"));
        Assertions.assertSame(ITRFVersion.ITRF_2008, getItrfVersion("SLR08"));
    }

    @Test public void testUnknown() {
        Assertions.assertSame(Predefined.ITRF_CIO_CONV_2010_ACCURATE_EOP,
                              ((FactoryManagedFrame) IGSUtils.guessFrame("UNDEF")).getFactoryKey());
        Assertions.assertSame(Predefined.ITRF_CIO_CONV_2010_ACCURATE_EOP,
                              ((FactoryManagedFrame) IGSUtils.guessFrame("WGS84")).getFactoryKey());
    }

    @Test
    public void testIersConvention() {
        Assertions.assertSame(IERSConventions.IERS_1996, getConvention("ITRF88"));
        Assertions.assertSame(IERSConventions.IERS_1996, getConvention("ITRF89"));
        Assertions.assertSame(IERSConventions.IERS_1996, getConvention("ITRF96"));
        Assertions.assertSame(IERSConventions.IERS_1996, getConvention("ITRF00"));
        Assertions.assertSame(IERSConventions.IERS_2003, getConvention("ITRF05"));
        Assertions.assertSame(IERSConventions.IERS_2003, getConvention("ITRF08"));
        Assertions.assertSame(IERSConventions.IERS_2010, getConvention("ITRF14"));
        Assertions.assertSame(IERSConventions.IERS_2010, getConvention("ITRF20"));
    }

    @Test
    public void testGcrf() {
        Assertions.assertNull(IGSUtils.guessFrame("GCRF").getParent());
        Assertions.assertNull(IGSUtils.guessFrame(" GCRF").getParent());
        Assertions.assertNull(IGSUtils.guessFrame("GCRF ").getParent());
    }

    @Test
    public void testEME2000() {
        Assertions.assertSame(FramesFactory.getEME2000(), IGSUtils.guessFrame("EME00"));
        Assertions.assertSame(FramesFactory.getEME2000(), IGSUtils.guessFrame("EME2K"));
    }

    @Test
    public void testITRFNames() {
        Assertions.assertEquals("IGS89", IGSUtils.frameName(FramesFactory.getITRF(ITRFVersion.ITRF_1989,
                                                                                  IERSConventions.IERS_2010,
                                                                                  false)));
        Assertions.assertEquals("IGS96", IGSUtils.frameName(FramesFactory.getITRF(ITRFVersion.ITRF_1996,
                                                                                  IERSConventions.IERS_2010,
                                                                                  false)));
        Assertions.assertEquals("IGS00", IGSUtils.frameName(FramesFactory.getITRF(ITRFVersion.ITRF_2000,
                                                                                  IERSConventions.IERS_2010,
                                                                                  false)));
        Assertions.assertEquals("IGS05", IGSUtils.frameName(FramesFactory.getITRF(ITRFVersion.ITRF_2005,
                                                                                  IERSConventions.IERS_2010,
                                                                                  false)));
        Assertions.assertEquals("IGS14", IGSUtils.frameName(FramesFactory.getITRF(ITRFVersion.ITRF_2014,
                                                                                  IERSConventions.IERS_2010,
                                                                                  false)));
        Assertions.assertEquals("IGS20", IGSUtils.frameName(FramesFactory.getITRF(ITRFVersion.ITRF_2020,
                                                                                  IERSConventions.IERS_2010,
                                                                                  false)));
    }

    @Test
    public void testInertialNames() {
        Assertions.assertEquals("GCRF",  IGSUtils.frameName(FramesFactory.getGCRF()));
        Assertions.assertEquals("EME2K", IGSUtils.frameName(FramesFactory.getEME2000()));
    }

    @Test
    public void testUnsupportedFrame() {
        try {
            IGSUtils.frameName(FramesFactory.getMOD(IERSConventions.IERS_2010));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.FRAME_NOT_ALLOWED, oe.getSpecifier());
            Assertions.assertEquals("MOD/2010", oe.getParts()[0]);
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
