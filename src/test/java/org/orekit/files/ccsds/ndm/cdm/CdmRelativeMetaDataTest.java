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
package org.orekit.files.ccsds.ndm.cdm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;

/** Tests for CdmRelativeMetaData class.
 * These tests are used to increase condition coverage, other global tests are in CdmParser/WriterTest classes.
 */
public class CdmRelativeMetaDataTest {

    /** Condition coverage on the checkScreenVolumeConditions method. */
    @Test
    public void testCheckScreenVolumeConditions() {

        final CdmRelativeMetadata meta = new CdmRelativeMetadata();
        meta.setScreenType(ScreenType.SHAPE);

        // getScreenEntryTime() == null
        try {
            meta.checkScreenVolumeConditions();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_MISSING_KEYWORD, oe.getSpecifier());
            Assertions.assertEquals(CdmRelativeMetadataKey.SCREEN_ENTRY_TIME, oe.getParts()[0]);
        }
        meta.setScreenEntryTime(AbsoluteDate.ARBITRARY_EPOCH);

        // getScreenExitTime() == null
        try {
            meta.checkScreenVolumeConditions();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_MISSING_KEYWORD, oe.getSpecifier());
            Assertions.assertEquals(CdmRelativeMetadataKey.SCREEN_EXIT_TIME, oe.getParts()[0]);
        }
        meta.setScreenExitTime(AbsoluteDate.ARBITRARY_EPOCH);

        // getScreenVolumeShape() == null
        try {
            meta.checkScreenVolumeConditions();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_MISSING_KEYWORD, oe.getSpecifier());
            Assertions.assertEquals(CdmRelativeMetadataKey.SCREEN_VOLUME_SHAPE, oe.getParts()[0]);
        }

        // ScreenVolumeShape.SPHERE && getScreenVolumeRadius() is NaN
        meta.setScreenVolumeShape(ScreenVolumeShape.SPHERE);
        try {
            meta.checkScreenVolumeConditions();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_MISSING_KEYWORD, oe.getSpecifier());
            Assertions.assertEquals(CdmRelativeMetadataKey.SCREEN_VOLUME_RADIUS, oe.getParts()[0]);
        }
        
        // ScreenVolumeShape.SPHERE && getScreenVolumeRadius() not NaN
        meta.setScreenVolumeShape(ScreenVolumeShape.SPHERE);
        meta.setScreenVolumeRadius(1.);
        meta.checkScreenVolumeConditions();
        Assertions.assertEquals(1.,  meta.getScreenVolumeRadius(), 0.);
        
        // ScreenVolumeShape.BOX or ScreenVolumeShape.ELLIPSOID
        final ScreenVolumeShape[] shapes = new ScreenVolumeShape[] { ScreenVolumeShape.BOX, ScreenVolumeShape.ELLIPSOID };

        for (final ScreenVolumeShape shape : shapes) {

            meta.setScreenVolumeShape(shape);

            // Re-init errors
            meta.setScreenVolumeFrame(null);
            meta.setScreenVolumeX(Double.NaN);
            meta.setScreenVolumeY(Double.NaN);
            meta.setScreenVolumeZ(Double.NaN);

            // getScreenVolumeFrame() == null
            try {
                meta.checkScreenVolumeConditions();
                Assertions.fail("an exception should have been thrown");
            } catch (OrekitException oe) {
                Assertions.assertEquals(OrekitMessages.CCSDS_MISSING_KEYWORD, oe.getSpecifier());
                Assertions.assertEquals(CdmRelativeMetadataKey.SCREEN_VOLUME_FRAME, oe.getParts()[0]);
            }
            meta.setScreenVolumeFrame(ScreenVolumeFrame.RTN);

            // getScreenVolumeX() is NaN
            try {
                meta.checkScreenVolumeConditions();
                Assertions.fail("an exception should have been thrown");
            } catch (OrekitException oe) {
                Assertions.assertEquals(OrekitMessages.CCSDS_MISSING_KEYWORD, oe.getSpecifier());
                Assertions.assertEquals(CdmRelativeMetadataKey.SCREEN_VOLUME_X, oe.getParts()[0]);
            }
            meta.setScreenVolumeX(1.);

            // getScreenVolumeY() is NaN
            try {
                meta.checkScreenVolumeConditions();
                Assertions.fail("an exception should have been thrown");
            } catch (OrekitException oe) {
                Assertions.assertEquals(OrekitMessages.CCSDS_MISSING_KEYWORD, oe.getSpecifier());
                Assertions.assertEquals(CdmRelativeMetadataKey.SCREEN_VOLUME_Y, oe.getParts()[0]);
            }
            meta.setScreenVolumeY(1.);

            // getScreenVolumeZ() is NaN
            try {
                meta.checkScreenVolumeConditions();
                Assertions.fail("an exception should have been thrown");
            } catch (OrekitException oe) {
                Assertions.assertEquals(OrekitMessages.CCSDS_MISSING_KEYWORD, oe.getSpecifier());
                Assertions.assertEquals(CdmRelativeMetadataKey.SCREEN_VOLUME_Z, oe.getParts()[0]);
            }
            meta.setScreenVolumeZ(1.);
        }
        
        // Screen type PC or PC_MAX && getScreenPcThreshold() is Nan
        meta.setScreenType(ScreenType.PC);
        try {
            meta.checkScreenVolumeConditions();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_MISSING_KEYWORD, oe.getSpecifier());
            Assertions.assertEquals(CdmRelativeMetadataKey.SCREEN_PC_THRESHOLD, oe.getParts()[0]);
        }
        meta.setScreenType(ScreenType.PC_MAX);
        try {
            meta.checkScreenVolumeConditions();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_MISSING_KEYWORD, oe.getSpecifier());
            Assertions.assertEquals(CdmRelativeMetadataKey.SCREEN_PC_THRESHOLD, oe.getParts()[0]);
        }
        
        // Case where it works
        meta.setScreenPcThreshold(1.);
        meta.checkScreenVolumeConditions();
        meta.setScreenType(ScreenType.PC);
        meta.checkScreenVolumeConditions();
        
        // Addendum: Test setCollisionPercentile when null
        meta.setCollisionPercentile(null);
        Assertions.assertEquals(null,  meta.getCollisionPercentile());
    }
}
