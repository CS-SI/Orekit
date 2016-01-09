/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

public class OrphanFrameTest {

    @Test
    public void testNotAttached() throws OrekitException {

        OrphanFrame level0  = new OrphanFrame("l0");
        OrphanFrame level1A = new OrphanFrame("l1A");
        OrphanFrame level1B = new OrphanFrame("l1B");
        OrphanFrame level2  = new OrphanFrame("l2");

        level0.addChild(level1A, Transform.IDENTITY, false);
        level0.addChild(level1B, Transform.IDENTITY, false);
        level1B.addChild(level2, Transform.IDENTITY, false);

        Assert.assertEquals(2, level0.getChildren().size());
        Assert.assertEquals(0, level1A.getChildren().size());
        Assert.assertEquals(1, level1B.getChildren().size());
        Assert.assertEquals(0, level2.getChildren().size());

        for (OrphanFrame of : Arrays.asList(level0, level1A, level1B, level2)) {
            try {
                of.getFrame();
                Assert.fail("an exception should have been thrown");
            } catch (OrekitException oe) {
                Assert.assertEquals(OrekitMessages.FRAME_NOT_ATTACHED, oe.getSpecifier());
            }
        }

    }

    @Test
    public void testAlreadyAttachedSubTree() throws OrekitException {
        OrphanFrame level0 = new OrphanFrame("l0");
        OrphanFrame level1 = new OrphanFrame("l1");
        level0.addChild(level1, Transform.IDENTITY, false);
        try {
            level0.addChild(level1, Transform.IDENTITY, false);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.FRAME_ALREADY_ATTACHED, oe.getSpecifier());
        }
    }

    @Test
    public void testAlreadyAttachedMainTree() throws OrekitException {
        OrphanFrame level0 = new OrphanFrame("l0");
        level0.attachTo(FramesFactory.getGCRF(), Transform.IDENTITY, false);
        try {
            level0.attachTo(FramesFactory.getEME2000(), Transform.IDENTITY, false);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.FRAME_ALREADY_ATTACHED, oe.getSpecifier());
        }
    }

    @Test
    public void testSimpleUse() throws OrekitException {
        OrphanFrame level0  = new OrphanFrame("l0");
        OrphanFrame level1A = new OrphanFrame("l1A");
        OrphanFrame level1B = new OrphanFrame("l1B");
        OrphanFrame level2  = new OrphanFrame("l2");
        level0.addChild(level1A, Transform.IDENTITY, false);
        level0.addChild(level1B, Transform.IDENTITY, false);
        level1B.addChild(level2, Transform.IDENTITY, false);
        level0.attachTo(FramesFactory.getGCRF(), Transform.IDENTITY, false);
        Assert.assertEquals(1, level0.getFrame().getDepth());
        Assert.assertEquals(level0.toString(), level0.getFrame().getName());
        Assert.assertEquals(2, level1A.getFrame().getDepth());
        Assert.assertEquals(level1A.toString(), level1A.getFrame().getName());
        Assert.assertEquals(2, level1B.getFrame().getDepth());
        Assert.assertEquals(level1B.toString(), level1B.getFrame().getName());
        Assert.assertEquals(3, level2.getFrame().getDepth());
        Assert.assertEquals(level2.toString(), level2.getFrame().getName());
    }

    @Test
    public void testLateAddition() throws OrekitException {
        OrphanFrame level0  = new OrphanFrame("l0");
        OrphanFrame level1A = new OrphanFrame("l1A");
        OrphanFrame level1B = new OrphanFrame("l1B");
        OrphanFrame level2  = new OrphanFrame("l2");
        level0.addChild(level1A, Transform.IDENTITY, false);
        level0.addChild(level1B, Transform.IDENTITY, false);

        level0.attachTo(FramesFactory.getGCRF(), Transform.IDENTITY, false);
        Assert.assertEquals(1, level0.getFrame().getDepth());
        Assert.assertEquals(level0.toString(), level0.getFrame().getName());
        Assert.assertEquals(2, level1A.getFrame().getDepth());
        Assert.assertEquals(level1A.toString(), level1A.getFrame().getName());
        Assert.assertEquals(2, level1B.getFrame().getDepth());
        Assert.assertEquals(level1B.toString(), level1B.getFrame().getName());

        // level2 is not attached to anything yet
        try {
            level2.getFrame();
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.FRAME_NOT_ATTACHED, oe.getSpecifier());
        }

        // adding a new child after the top level has been attached
        level1B.addChild(level2, Transform.IDENTITY, false);

        // now level2 is attached to the main tree
        Assert.assertEquals(3, level2.getFrame().getDepth());
        Assert.assertEquals(level2.toString(), level2.getFrame().getName());

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
