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
package org.orekit.files.ccsds.ndm.cdm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.BodyFacade;
import org.orekit.files.ccsds.definitions.CenterName;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.utils.IERSConventions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

/** Tests for CdmMetaData class.
 * These tests are used to increase condition coverage, other global tests are in CdmParser/WriterTest classes.
 */
class CdmMetaDataTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

    /** Condition coverage on the getFrame method. */
    @Test
    @DefaultDataContext
    void testGetFrame() {
        
        final CdmMetadata meta = new CdmMetadata();
        
        // refFrame == null
        try {
            meta.getFrame();
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.CCSDS_INVALID_FRAME, oe.getSpecifier());
            assertEquals("No reference frame", oe.getParts()[0]);
        }
        
        // orbitCenter.getBody() == null
        try {
            meta.setOrbitCenter(new BodyFacade("dummy center", null));
            meta.getFrame();
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.NO_DATA_LOADED_FOR_CELESTIAL_BODY, oe.getSpecifier());
            assertEquals("No Orbit center name", oe.getParts()[0]);
        }
        
        // refFrame.asFrame() == null
        meta.setOrbitCenter(BodyFacade.create(CenterName.MARS));
        try {
            meta.setRefFrame(new FrameFacade(null, null, null, null, "dummy frame"));
            meta.getFrame();
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.CCSDS_INVALID_FRAME, oe.getSpecifier());
            assertEquals("dummy frame", oe.getParts()[0]);
        }
        
        // refFrame.asCelestialBodyFrame() == CelestialBodyFrame.MCI
        // AND
        // CelestialBodyFactory.MARS.equals(orbitCenter.getBody().getName())
        meta.setRefFrame(FrameFacade.map(CelestialBodyFactory.getMars().getInertiallyOrientedFrame()));
        assertEquals(CelestialBodyFactory.getMars().getInertiallyOrientedFrame(), meta.getFrame());
        
        // refFrame.asCelestialBodyFrame() == CelestialBodyFrame.ICRF
        // AND
        // isIcrf && isSolarSystemBarycenter
        final Frame icrf = FramesFactory.getICRF();
        meta.setOrbitCenter(BodyFacade.create(CenterName.SOLAR_SYSTEM_BARYCENTER));
        meta.setRefFrame(FrameFacade.map(icrf));
        assertEquals(icrf, meta.getFrame());
    }

    /** Condition coverage on the setAltCovRefFrame method. */
    @Test
    @DefaultDataContext
    void testSetAltCovRefFrame() {
        
        final CdmMetadata meta = new CdmMetadata();
        final FrameFacade altCovRefFrame = new FrameFacade(null, null, null, null, null);

        // getAltCovType()
        try {
            meta.setAltCovRefFrame(altCovRefFrame);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.CCSDS_MISSING_KEYWORD, oe.getSpecifier());
            assertEquals(CdmMetadataKey.ALT_COV_TYPE, oe.getParts()[0]);
        }
        
        // altCovRefFrame.asFrame() == null
        meta.setAltCovType(AltCovarianceType.CSIG3EIGVEC3);
        try {
            meta.setAltCovRefFrame(altCovRefFrame);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.CCSDS_INVALID_FRAME, oe.getSpecifier());
            assertNull(oe.getParts()[0]);
        }
        
        // Frame not allowed (allowed frames are GCRF, EME2000, ITRF)
        FrameFacade frameFacade = FrameFacade.map(FramesFactory.getICRF());
        try {
            meta.setAltCovRefFrame(frameFacade);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.CCSDS_INVALID_FRAME, oe.getSpecifier());
            assertEquals(frameFacade.getName(), oe.getParts()[0]);
        }
        
        // Test the 3 allowed frames
        frameFacade = FrameFacade.map(FramesFactory.getGCRF());
        meta.setAltCovRefFrame(frameFacade);
        assertEquals(frameFacade, meta.getAltCovRefFrame());
        
        frameFacade = FrameFacade.map(FramesFactory.getEME2000());
        meta.setAltCovRefFrame(frameFacade);
        assertEquals(frameFacade, meta.getAltCovRefFrame());
        
        frameFacade = FrameFacade.map(FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        meta.setAltCovRefFrame(frameFacade);
        assertEquals(frameFacade, meta.getAltCovRefFrame());
    }
}
