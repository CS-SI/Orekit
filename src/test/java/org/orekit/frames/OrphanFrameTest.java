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
package org.orekit.frames;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class OrphanFrameTest {

    @Test
    void testNotAttached() {

        OrphanFrame level0  = new OrphanFrame("l0");
        OrphanFrame level1A = new OrphanFrame("l1A");
        OrphanFrame level1B = new OrphanFrame("l1B");
        OrphanFrame level2  = new OrphanFrame("l2");

        level0.addChild(level1A, Transform.IDENTITY, false);
        level0.addChild(level1B, Transform.IDENTITY, false);
        level1B.addChild(level2, Transform.IDENTITY, false);

        assertEquals(2, level0.getChildren().size());
        assertEquals(0, level1A.getChildren().size());
        assertEquals(1, level1B.getChildren().size());
        assertEquals(0, level2.getChildren().size());

        for (OrphanFrame of : Arrays.asList(level0, level1A, level1B, level2)) {
            try {
                of.getFrame();
                fail("an exception should have been thrown");
            } catch (OrekitException oe) {
                assertEquals(OrekitMessages.FRAME_NOT_ATTACHED, oe.getSpecifier());
            }
        }

    }

    @Test
    void testAlreadyAttachedSubTree() {
        OrphanFrame level0 = new OrphanFrame("l0");
        OrphanFrame level1 = new OrphanFrame("l1");
        level0.addChild(level1, Transform.IDENTITY, false);
        try {
            level0.addChild(level1, Transform.IDENTITY, false);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.FRAME_ALREADY_ATTACHED, oe.getSpecifier());
        }
    }

    @Test
    void testAlreadyAttachedMainTree() {
        OrphanFrame level0 = new OrphanFrame("l0");
        level0.attachTo(FramesFactory.getGCRF(), Transform.IDENTITY, false);
        try {
            level0.attachTo(FramesFactory.getEME2000(), Transform.IDENTITY, false);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.FRAME_ALREADY_ATTACHED, oe.getSpecifier());
        }
    }

    @Test
    void testSimpleUse() {
        OrphanFrame level0  = new OrphanFrame("l0");
        OrphanFrame level1A = new OrphanFrame("l1A");
        OrphanFrame level1B = new OrphanFrame("l1B");
        OrphanFrame level2  = new OrphanFrame("l2");
        level0.addChild(level1A, Transform.IDENTITY, false);
        level0.addChild(level1B, Transform.IDENTITY, false);
        level1B.addChild(level2, Transform.IDENTITY, false);
        level0.attachTo(FramesFactory.getGCRF(), Transform.IDENTITY, false);
        assertEquals(1, level0.getFrame().getDepth());
        assertEquals(level0.toString(), level0.getFrame().getName());
        assertEquals(2, level1A.getFrame().getDepth());
        assertEquals(level1A.toString(), level1A.getFrame().getName());
        assertEquals(2, level1B.getFrame().getDepth());
        assertEquals(level1B.toString(), level1B.getFrame().getName());
        assertEquals(3, level2.getFrame().getDepth());
        assertEquals(level2.toString(), level2.getFrame().getName());
    }

    @Test
    void testLateAddition() {
        OrphanFrame level0  = new OrphanFrame("l0");
        OrphanFrame level1A = new OrphanFrame("l1A");
        OrphanFrame level1B = new OrphanFrame("l1B");
        OrphanFrame level2  = new OrphanFrame("l2");
        level0.addChild(level1A, Transform.IDENTITY, false);
        level0.addChild(level1B, Transform.IDENTITY, false);

        level0.attachTo(FramesFactory.getGCRF(), Transform.IDENTITY, false);
        assertEquals(1, level0.getFrame().getDepth());
        assertEquals(level0.toString(), level0.getFrame().getName());
        assertEquals(2, level1A.getFrame().getDepth());
        assertEquals(level1A.toString(), level1A.getFrame().getName());
        assertEquals(2, level1B.getFrame().getDepth());
        assertEquals(level1B.toString(), level1B.getFrame().getName());

        // level2 is not attached to anything yet
        try {
            level2.getFrame();
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.FRAME_NOT_ATTACHED, oe.getSpecifier());
        }

        // adding a new child after the top level has been attached
        level1B.addChild(level2, Transform.IDENTITY, false);

        // now level2 is attached to the main tree
        assertEquals(3, level2.getFrame().getDepth());
        assertEquals(level2.toString(), level2.getFrame().getName());

    }

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
