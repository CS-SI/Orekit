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


import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;


public class FieldTwoImpulseTransferTest {
    // Tolerance for floating point numbers comparison
    public static final double NUMERICAL_TOLERANCE = 1e-12;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }
    @Test
    public void testTwoImpulseTransferGetters() {
        final Binary64Field field = Binary64Field.getInstance();
        final Frame eme2000 = FramesFactory.getEME2000();
        final FieldAbsoluteDate<Binary64> date = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        final TimeStampedFieldPVCoordinates<Binary64> pvt1 = new TimeStampedFieldPVCoordinates<>(date, new FieldPVCoordinates<>(new FieldVector3D<>(field,new Vector3D(0,0,0)), new FieldVector3D<>(field,new Vector3D(0,0,0))));
        final TimeStampedFieldPVCoordinates<Binary64> pvt2 = new TimeStampedFieldPVCoordinates<>(date.shiftedBy(1000.), new FieldPVCoordinates<>(new FieldVector3D<>(field, new Vector3D(1,0,0)), new FieldVector3D<>(field, new Vector3D(0,1,0))));
        final FieldVector3D<Binary64> deltaV1 = new FieldVector3D<>(field, new Vector3D(20,17,16));
        final FieldVector3D<Binary64> deltaV2 = new FieldVector3D<>(field, new Vector3D(58,45,96));
        final FieldTwoImpulseTransfer<Binary64> transfer = new FieldTwoImpulseTransfer<>(pvt1,pvt2,deltaV1,deltaV2,eme2000);

        TestUtils.validateFieldVector3D(new Vector3D(0,0,0),transfer.getPvt1().getPosition(),NUMERICAL_TOLERANCE);
        TestUtils.validateFieldVector3D(new Vector3D(0,0,0),transfer.getPvt1().getVelocity(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(date.toAbsoluteDate().toDouble(),pvt1.getDate().toAbsoluteDate().toDouble(),NUMERICAL_TOLERANCE);

        TestUtils.validateFieldVector3D(new Vector3D(1,0,0),transfer.getPvt2().getPosition(),NUMERICAL_TOLERANCE);
        TestUtils.validateFieldVector3D(new Vector3D(0,1,0),transfer.getPvt2().getVelocity(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(date.shiftedBy(1000.).toAbsoluteDate().toDouble(),pvt2.getDate().toAbsoluteDate().toDouble(),NUMERICAL_TOLERANCE);

        TestUtils.validateFieldVector3D(new Vector3D(20,17,16),transfer.getDeltaV1(),NUMERICAL_TOLERANCE);
        TestUtils.validateFieldVector3D(new Vector3D(58,45,96),transfer.getDeltaV2(),NUMERICAL_TOLERANCE);

        Assertions.assertEquals(1000., transfer.getDuration().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(date.toAbsoluteDate().toDouble(), transfer.getDepartureDate().toAbsoluteDate().toDouble() ,NUMERICAL_TOLERANCE);
        Assertions.assertEquals(date.shiftedBy(1000.).toAbsoluteDate().toDouble(), transfer.getArrivalDate().toAbsoluteDate().toDouble(),NUMERICAL_TOLERANCE);

        Assertions.assertEquals(deltaV1.getNorm().add(deltaV2.getNorm()).getReal(),transfer.getTotalDeltaV().getReal(),NUMERICAL_TOLERANCE);

        Assertions.assertEquals(date.toAbsoluteDate().toDouble(), transfer.getPvt1BeforeMan().getDate().toAbsoluteDate().toDouble(),NUMERICAL_TOLERANCE);
        TestUtils.validateFieldVector3D(new Vector3D(0,0,0),transfer.getPvt1BeforeMan().getPosition(),NUMERICAL_TOLERANCE);
        TestUtils.validateFieldVector3D(new Vector3D(-20., -17., -16.), transfer.getPvt1BeforeMan().getVelocity(),NUMERICAL_TOLERANCE);

        Assertions.assertEquals(date.shiftedBy(1000.).toAbsoluteDate().toDouble(), transfer.getPvt2AfterMan().getDate().toAbsoluteDate().toDouble(),NUMERICAL_TOLERANCE);
        TestUtils.validateFieldVector3D(new Vector3D(1,0,0),transfer.getPvt2AfterMan().getPosition(),NUMERICAL_TOLERANCE);
        TestUtils.validateFieldVector3D(new Vector3D(58., 46., 96.), transfer.getPvt2AfterMan().getVelocity(),NUMERICAL_TOLERANCE);
    }

    @Test
    public void testExpressInFrame() {
        final Binary64Field field = Binary64Field.getInstance();
        final Frame eme2000 = FramesFactory.getEME2000();
        final Frame outputFrame = FramesFactory.getGCRF();
        final FieldAbsoluteDate<Binary64> date = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);
        final TimeStampedFieldPVCoordinates<Binary64> pvt1BeforeMan = new TimeStampedFieldPVCoordinates<>(date, new FieldPVCoordinates<>(new FieldVector3D<>(field, new Vector3D(1,2,3)), new FieldVector3D<>(field, new Vector3D(1,2,3))));
        final FieldVector3D<Binary64> v1AfterMan = new FieldVector3D<>(field, new Vector3D(2,3,4));
        final TimeStampedFieldPVCoordinates<Binary64> pvt2AfterMan = new TimeStampedFieldPVCoordinates<>(date.shiftedBy(100.),new FieldPVCoordinates<>(new FieldVector3D<>(field, new Vector3D(11,12,13)),new FieldVector3D<>(field,new Vector3D(11,12,13))));
        final FieldVector3D<Binary64> v2BeforeMan = new FieldVector3D<>(field, new Vector3D(10,11,12));
        //Initialize a false Transfer in order to use the fromPVTAndVelocities method
        final FieldTwoImpulseTransfer<Binary64> init = new FieldTwoImpulseTransfer<>(pvt1BeforeMan,pvt1BeforeMan,v1AfterMan,v2BeforeMan,eme2000);
        final FieldTwoImpulseTransfer<Binary64> transfer = init.fromPVTAndVelocities(pvt1BeforeMan,v1AfterMan,pvt2AfterMan,v2BeforeMan,eme2000);
        TestUtils.validateFieldVector3D(new Vector3D(1,1,1),transfer.getDeltaV1(),NUMERICAL_TOLERANCE);
        TestUtils.validateFieldVector3D(new Vector3D(1,1,1),transfer.getDeltaV2(),NUMERICAL_TOLERANCE);

        final FieldTwoImpulseTransfer<Binary64> transferGCRF = transfer.expressInFrame(outputFrame);

        final TimeStampedFieldPVCoordinates<Binary64> pvt1InGCRF = eme2000.getTransformTo(outputFrame,transfer.getPvt1().getDate()).transformPVCoordinates(transfer.getPvt1());
        final TimeStampedFieldPVCoordinates<Binary64> pvt2InGCRF = eme2000.getTransformTo(outputFrame,transfer.getPvt2().getDate()).transformPVCoordinates(transfer.getPvt2());
        final FieldVector3D<Binary64> deltaV1InGCRF = eme2000.getTransformTo(outputFrame,transfer.getPvt1().getDate()).transformVector(transfer.getDeltaV1());
        final FieldVector3D<Binary64> deltaV2InGCRF = eme2000.getTransformTo(outputFrame,transfer.getPvt2().getDate()).transformVector(transfer.getDeltaV2());

        final Vector3D pos1_GCRF = new Vector3D(pvt1InGCRF.getPosition().getX().getReal(), pvt1InGCRF.getPosition().getY().getReal(),pvt1InGCRF.getPosition().getZ().getReal());
        final Vector3D vel1_GCRF = new Vector3D(pvt1InGCRF.getVelocity().getX().getReal(), pvt1InGCRF.getVelocity().getY().getReal(),pvt1InGCRF.getVelocity().getZ().getReal());

        final Vector3D pos2_GCRF = new Vector3D(pvt2InGCRF.getPosition().getX().getReal(), pvt2InGCRF.getPosition().getY().getReal(),pvt2InGCRF.getPosition().getZ().getReal());
        final Vector3D vel2_GCRF = new Vector3D(pvt2InGCRF.getVelocity().getX().getReal(), pvt2InGCRF.getVelocity().getY().getReal(),pvt2InGCRF.getVelocity().getZ().getReal());

        final Vector3D deltaV1_GCRF = new Vector3D(deltaV1InGCRF.getX().getReal(),deltaV1InGCRF.getY().getReal(),deltaV1InGCRF.getZ().getReal());
        final Vector3D deltaV2_GCRF = new Vector3D(deltaV2InGCRF.getX().getReal(),deltaV2InGCRF.getY().getReal(),deltaV2InGCRF.getZ().getReal());

        TestUtils.validateFieldVector3D(pos1_GCRF,transferGCRF.getPvt1().getPosition(),NUMERICAL_TOLERANCE);
        TestUtils.validateFieldVector3D(vel1_GCRF,transferGCRF.getPvt1().getVelocity(),NUMERICAL_TOLERANCE);
        TestUtils.validateFieldVector3D(pos2_GCRF,transferGCRF.getPvt2().getPosition(),NUMERICAL_TOLERANCE);
        TestUtils.validateFieldVector3D(vel2_GCRF,transferGCRF.getPvt2().getVelocity(),NUMERICAL_TOLERANCE);
        TestUtils.validateFieldVector3D(deltaV1_GCRF,transferGCRF.getDeltaV1(),NUMERICAL_TOLERANCE);
        TestUtils.validateFieldVector3D(deltaV2_GCRF,transferGCRF.getDeltaV2(),NUMERICAL_TOLERANCE);

    }
}
