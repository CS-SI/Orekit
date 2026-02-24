/* Copyright 2002-2026 CS GROUP
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
package org.orekit.propagation.relative;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;


public class TwoImpulseTransferTest {
    // Tolerance for floating point numbers comparison
    public static final double NUMERICAL_TOLERANCE = 1e-12;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }
    @Test
    public void testTwoImpulseTransferGetters() {
        final Frame eme2000 = FramesFactory.getEME2000();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final TimeStampedPVCoordinates pvt1 = new TimeStampedPVCoordinates(date,new Vector3D(0,0,0), new Vector3D(0,0,0));
        final TimeStampedPVCoordinates pvt2 = new TimeStampedPVCoordinates(date.shiftedBy(1000.), new Vector3D(1,0,0), new Vector3D(0,1,0));
        final Vector3D deltaV1 = new Vector3D(20,17,16);
        final Vector3D deltaV2 = new Vector3D(58,45,96);
        final TwoImpulseTransfer transfer = new TwoImpulseTransfer(pvt1, pvt2, deltaV1, deltaV2, eme2000);

        final Frame transferFrame = transfer.getFrame();

        Assertions.assertEquals(eme2000.getName(),transferFrame.getName());

        TestUtils.validateVector3D(pvt1.getPosition(),transfer.getPvt1().getPosition(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(pvt1.getVelocity(),transfer.getPvt1().getVelocity(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(date.toDouble(),pvt1.getDate().toDouble(),NUMERICAL_TOLERANCE);

        TestUtils.validateVector3D(pvt2.getPosition(),transfer.getPvt2().getPosition(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(pvt2.getVelocity(),transfer.getPvt2().getVelocity(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(date.shiftedBy(1000.).toDouble(),pvt2.getDate().toDouble(),NUMERICAL_TOLERANCE);

        TestUtils.validateVector3D(deltaV1,transfer.getDeltaV1(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(deltaV2,transfer.getDeltaV2(),NUMERICAL_TOLERANCE);

        Assertions.assertEquals(1000., transfer.getDuration(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(date.toDouble(), transfer.getDepartureDate().toDouble() ,NUMERICAL_TOLERANCE);
        Assertions.assertEquals(date.shiftedBy(1000.).toDouble(), transfer.getArrivalDate().toDouble(),NUMERICAL_TOLERANCE);

        Assertions.assertEquals(deltaV1.getNorm()+deltaV2.getNorm(),transfer.getTotalDeltaV(),NUMERICAL_TOLERANCE);

        Assertions.assertEquals(date.toDouble(), transfer.getPvt1BeforeMan().getDate().toDouble(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(pvt1.getPosition(),transfer.getPvt1BeforeMan().getPosition(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(-20., -17., -16.), transfer.getPvt1BeforeMan().getVelocity(),NUMERICAL_TOLERANCE);

        Assertions.assertEquals(date.shiftedBy(1000.).toDouble(), transfer.getPvt2AfterMan().getDate().toDouble(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(pvt2.getPosition(),transfer.getPvt2AfterMan().getPosition(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(58., 46., 96.), transfer.getPvt2AfterMan().getVelocity(),NUMERICAL_TOLERANCE);
    }

    @Test
    public void testExpressInFrame() {
        final Frame eme2000 = FramesFactory.getEME2000();
        final Frame outputFrame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final TimeStampedPVCoordinates pvt1BeforeMan = new TimeStampedPVCoordinates(date, new Vector3D(1,2,3), new Vector3D(1,2,3));
        final Vector3D v1AfterMan = new Vector3D(2,3,4);
        final TimeStampedPVCoordinates pvt2AfterMan = new TimeStampedPVCoordinates(date.shiftedBy(100.),new Vector3D(11,12,13),new Vector3D(11,12,13));
        final Vector3D v2BeforeMan = new Vector3D(10,11,12);

        final TwoImpulseTransfer transfer = TwoImpulseTransfer.fromPVTAndVelocities(pvt1BeforeMan,v1AfterMan,pvt2AfterMan,v2BeforeMan,eme2000);
        TestUtils.validateVector3D(new Vector3D(1,1,1),transfer.getDeltaV1(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(1,1,1),transfer.getDeltaV2(),NUMERICAL_TOLERANCE);

        final TwoImpulseTransfer transferGCRF = transfer.expressInFrame(outputFrame);

        final TimeStampedPVCoordinates pvt1InGCRF = eme2000.getTransformTo(outputFrame,transfer.getPvt1().getDate()).transformPVCoordinates(transfer.getPvt1());
        final TimeStampedPVCoordinates pvt2InGCRF = eme2000.getTransformTo(outputFrame,transfer.getPvt2().getDate()).transformPVCoordinates(transfer.getPvt2());
        final Vector3D deltaV1InGCRF = eme2000.getTransformTo(outputFrame,transfer.getPvt1().getDate()).transformVector(transfer.getDeltaV1());
        final Vector3D deltaV2InGCRF = eme2000.getTransformTo(outputFrame,transfer.getPvt2().getDate()).transformVector(transfer.getDeltaV2());

        TestUtils.validateVector3D(pvt1InGCRF.getPosition(),transferGCRF.getPvt1().getPosition(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(pvt1InGCRF.getVelocity(),transferGCRF.getPvt1().getVelocity(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(pvt2InGCRF.getPosition(),transferGCRF.getPvt2().getPosition(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(pvt2InGCRF.getVelocity(),transferGCRF.getPvt2().getVelocity(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(deltaV1InGCRF,transferGCRF.getDeltaV1(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(deltaV2InGCRF,transferGCRF.getDeltaV2(),NUMERICAL_TOLERANCE);

    }
}
