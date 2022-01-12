/* Copyright 2002-2022 CS GROUP
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
package org.orekit.files.ccsds.ndm.adm.aem;

import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.CelestialBodyFrame;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.SpacecraftBodyFrame;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.ndm.ParsedUnitsBehavior;
import org.orekit.files.ccsds.ndm.adm.AttitudeType;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedAngularCoordinates;

public class AEMAttitudeTypeTest {

    private static final double QUATERNION_PRECISION = 1.0e-5;
    private static final double ANGLE_PRECISION = 1.0e-3;

    AemMetadata metadata;
    ContextBinding context;

    @Before
    public void setUp()
        throws Exception {
        Utils.setDataRoot("regular-data");
        metadata = new AemMetadata(4);
        context  =  new ContextBinding(() -> IERSConventions.IERS_2010,
                                       () -> true, () -> DataContext.getDefault(),
                                       () -> ParsedUnitsBehavior.STRICT_COMPLIANCE, () -> null,
                                       metadata::getTimeSystem, () -> 0.0, () -> 1.0);
        metadata.setTimeSystem(TimeSystem.TAI);
        metadata.getEndpoints().setFrameA(new FrameFacade(FramesFactory.getGCRF(), CelestialBodyFrame.GCRF,
                                                          null, null, "GCRF"));
        metadata.getEndpoints().setFrameB(new FrameFacade(null, null, null,
                                                          new SpacecraftBodyFrame(SpacecraftBodyFrame.BaseEquipment.GYRO_FRAME, "1"),
                                                          "GYRO 1"));
        metadata.getEndpoints().setA2b(true);
    }

    @After
    public void tearDown() {
        metadata = null;
        context  = null;
    }

    @Test
    public void testSpin() {

        // Initialize the attitude type
        final AttitudeType spin = AttitudeType.parseType("SPIN");

        // Test computation of angular coordinates from attitude data
        final String[] attitudeData = new String[] {
            "2021-03-17T00:00:00.000", "-136.19348942398204", "-33.53517498449179", "86.03122596451917", "-14.816053659122998"
        };
        final TimeStampedAngularCoordinates tsac = spin.parse(true, true, RotationOrder.XYZ, true, context, attitudeData);
        final Vector3D spinInert = tsac.getRotation().applyInverseTo(Vector3D.PLUS_K);
        Assert.assertEquals(-136.19348942398204, FastMath.toDegrees(spinInert.getAlpha()), ANGLE_PRECISION);
        Assert.assertEquals(-33.53517498449179,  FastMath.toDegrees(spinInert.getDelta()), ANGLE_PRECISION);

        final Rotation zeroPhase = new Rotation(RotationOrder.ZYZ, RotationConvention.FRAME_TRANSFORM,
                                                spinInert.getAlpha(), 0.5 * FastMath.PI - spinInert.getDelta(), 0.0);
        Assert.assertEquals(86.03122596451917,   FastMath.toDegrees(Rotation.distance(zeroPhase, tsac.getRotation())), ANGLE_PRECISION);
        Assert.assertEquals(0.0,                 FastMath.toDegrees(tsac.getRotationRate().getX()), ANGLE_PRECISION);
        Assert.assertEquals(0.0,                 FastMath.toDegrees(tsac.getRotationRate().getY()), ANGLE_PRECISION);
        Assert.assertEquals(-14.816053659122998, FastMath.toDegrees(tsac.getRotationRate().getZ()), ANGLE_PRECISION);

        // Test computation of attitude data from angular coordinates
        AemMetadata metadata = new AemMetadata(3);
        metadata.getEndpoints().setFrameA(new FrameFacade(FramesFactory.getGCRF(), CelestialBodyFrame.GCRF,
                                                          null, null, "GCRF"));
        metadata.getEndpoints().setFrameB(new FrameFacade(null, null, null,
                                                          new SpacecraftBodyFrame(SpacecraftBodyFrame.BaseEquipment.GYRO_FRAME, "1"),
                                                          "GYRO 1"));
        metadata.getEndpoints().setA2b(true);
        final String[] attitudeDataBis = spin.createDataFields(metadata.isFirst(),
                                                               metadata.getEndpoints().isExternal2SpacecraftBody(),
                                                               metadata.getEulerRotSeq(),
                                                               metadata.isSpacecraftBodyRate(),
                                                               tsac);
        for (int i = 0; i < attitudeDataBis.length; i++) {
            Assert.assertEquals(Double.parseDouble(attitudeData[i + 1]),
                                Double.parseDouble(attitudeDataBis[i]),
                                ANGLE_PRECISION);
        }

        // Verify angular derivative filter
        Assert.assertEquals(AngularDerivativesFilter.USE_RR, spin.getAngularDerivativesFilter());

    }

    /**
     * Attitude type SPIN_NUTATION in CCSDS AEM files is not implemented in Orekit.
     * This test verify if an exception is thrown
     */
    @Test
    public void testSpinNutation() {

        // Initialize the attitude type
        final AttitudeType spinNutation = AttitudeType.parseType("SPIN/NUTATION");

        // Test exception on the first method
        try {
            spinNutation.parse(true, true, RotationOrder.XYZ, true,
                               context, new String[] { "2021-03-17T00:00:00.000" });
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals(AttitudeType.SPIN_NUTATION.name(), oe.getParts()[0]);
        }

        // Test exception on the second method
        try {
            spinNutation.createDataFields(true, true, RotationOrder.XYZ, true, null);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals(AttitudeType.SPIN_NUTATION.name(), oe.getParts()[0]);
        }

        // no exception on the third method
        Assert.assertEquals(AngularDerivativesFilter.USE_RR, spinNutation.getAngularDerivativesFilter());

    }

    @Test
    public void testQuaternion() {
        // Initialize the attitude type
        final AttitudeType quaternion = AttitudeType.parseType("QUATERNION");

        // Test computation of angular coordinates from attitude data
        final String[] attitudeData = new String[] {
            "2021-01-29T21:24:37", "0.68427", "0.56748", "0.03146", "0.45689"
        };
        metadata.setIsFirst(true);
        final TimeStampedAngularCoordinates tsac = quaternion.parse(metadata.isFirst(),
                                                                    metadata.getEndpoints().isExternal2SpacecraftBody(),
                                                                    metadata.getEulerRotSeq(),
                                                                    metadata.isSpacecraftBodyRate(),
                                                                    context, attitudeData);
        Assert.assertEquals(0.68427, tsac.getRotation().getQ0(), QUATERNION_PRECISION);
        Assert.assertEquals(0.56748, tsac.getRotation().getQ1(), QUATERNION_PRECISION);
        Assert.assertEquals(0.03146, tsac.getRotation().getQ2(), QUATERNION_PRECISION);
        Assert.assertEquals(0.45689, tsac.getRotation().getQ3(), QUATERNION_PRECISION);

        // Test computation of attitude data from angular coordinates
        AemMetadata metadata = new AemMetadata(3);
        metadata.getEndpoints().setFrameA(new FrameFacade(FramesFactory.getGCRF(), CelestialBodyFrame.GCRF,
                                                          null, null, "GCRF"));
        metadata.getEndpoints().setFrameB(new FrameFacade(null, null, null,
                                                          new SpacecraftBodyFrame(SpacecraftBodyFrame.BaseEquipment.GYRO_FRAME, "1"),
                        "GYRO 1"));
        metadata.setIsFirst(true);
        metadata.setRateFrameIsA(false);
        metadata.getEndpoints().setA2b(true);
        final String[] attitudeDataBis = quaternion.createDataFields(metadata.isFirst(),
                                                                     metadata.getEndpoints().isExternal2SpacecraftBody(),
                                                                     metadata.getEulerRotSeq(),
                                                                     metadata.isSpacecraftBodyRate(),
                                                                     tsac);
        for (int i = 0; i < attitudeDataBis.length; i++) {
            Assert.assertEquals(Double.parseDouble(attitudeData[i + 1]),
                                Double.parseDouble(attitudeDataBis[i]),
                                QUATERNION_PRECISION);
        }

        // Verify angular derivative filter
        Assert.assertEquals(AngularDerivativesFilter.USE_R, quaternion.getAngularDerivativesFilter());
    }

    @Test
    public void testQuaternionDerivates() {
        // Initialize the attitude type
        final AttitudeType quaternion = AttitudeType.parseType("QUATERNION/DERIVATIVE");

        // Test computation of angular coordinates from attitude data
        final String[] attitudeData = new String[] {
            "2021-01-29T21:24:37", "0.68427", "0.56748", "0.03146", "0.45689", "0.0", "0.0", "0.0", "0.0"
        };
        metadata.setIsFirst(true);
        final TimeStampedAngularCoordinates tsac = quaternion.parse(metadata.isFirst(),
                                                                    metadata.getEndpoints().isExternal2SpacecraftBody(),
                                                                    metadata.getEulerRotSeq(),
                                                                    metadata.isSpacecraftBodyRate(),
                                                                    context, attitudeData);
        Assert.assertEquals(0.68427,    tsac.getRotation().getQ0(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.56748,    tsac.getRotation().getQ1(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.03146,    tsac.getRotation().getQ2(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.45689,    tsac.getRotation().getQ3(),    QUATERNION_PRECISION);

        // Test computation of attitude data from angular coordinates
        AemMetadata metadata = new AemMetadata(3);
        metadata.setIsFirst(true);
        metadata.getEndpoints().setFrameA(new FrameFacade(FramesFactory.getGCRF(), CelestialBodyFrame.GCRF,
                                                          null, null, "GCRF"));
        metadata.getEndpoints().setFrameB(new FrameFacade(null, null, null,
                                                          new SpacecraftBodyFrame(SpacecraftBodyFrame.BaseEquipment.GYRO_FRAME, "1"),
                                                          "GYRO 1"));
        metadata.setRateFrameIsA(false);
        metadata.getEndpoints().setA2b(true);
        final String[] attitudeDataBis = quaternion.createDataFields(metadata.isFirst(),
                                                                     metadata.getEndpoints().isExternal2SpacecraftBody(),
                                                                     metadata.getEulerRotSeq(),
                                                                     metadata.isSpacecraftBodyRate(),
                                                                     tsac);
        for (int i = 0; i < attitudeDataBis.length; i++) {
            Assert.assertEquals(Double.parseDouble(attitudeData[i + 1]),
                                Double.parseDouble(attitudeDataBis[i]),
                                QUATERNION_PRECISION);
        }

        // Verify angular derivative filter
        Assert.assertEquals(AngularDerivativesFilter.USE_RR, quaternion.getAngularDerivativesFilter());
    }

    @Test
    public void testQuaternionRate() {
        // Initialize the attitude type
        final AttitudeType quaternionRate = AttitudeType.parseType("QUATERNION/RATE");

        // Test computation of angular coordinates from attitude data
        final String[] attitudeData = new String[] {
            "2021-01-29T21:24:37", "0.56748", "0.03146", "0.45689", "0.68427", "43.1", "12.8", "37.9"
        };
        metadata.setRateFrameIsA(true);
        metadata.getEndpoints().setA2b(true);
        metadata.setIsFirst(false);
        final TimeStampedAngularCoordinates tsac = quaternionRate.parse(metadata.isFirst(),
                                                                        metadata.getEndpoints().isExternal2SpacecraftBody(),
                                                                        metadata.getEulerRotSeq(),
                                                                        metadata.isSpacecraftBodyRate(),
                                                                        context, attitudeData);
        Assert.assertEquals(0.68427,  tsac.getRotation().getQ0(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.56748,  tsac.getRotation().getQ1(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.03146,  tsac.getRotation().getQ2(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.45689,  tsac.getRotation().getQ3(),    QUATERNION_PRECISION);
        Vector3D rebuiltRate = tsac.getRotation().applyInverseTo(tsac.getRotationRate());
        Assert.assertEquals(FastMath.toRadians(43.1), rebuiltRate.getX(), ANGLE_PRECISION);
        Assert.assertEquals(FastMath.toRadians(12.8), rebuiltRate.getY(), ANGLE_PRECISION);
        Assert.assertEquals(FastMath.toRadians(37.9), rebuiltRate.getZ(), ANGLE_PRECISION);

        // Test computation of attitude data from angular coordinates
        AemMetadata metadata = new AemMetadata(3);
        metadata.getEndpoints().setFrameA(new FrameFacade(FramesFactory.getGCRF(), CelestialBodyFrame.GCRF,
                                                          null, null, "GCRF"));
        metadata.getEndpoints().setFrameB(new FrameFacade(null, null, null,
                                                          new SpacecraftBodyFrame(SpacecraftBodyFrame.BaseEquipment.GYRO_FRAME, "1"),
                                                          "GYRO 1"));
        metadata.setIsFirst(false);
        metadata.setRateFrameIsA(true);
        metadata.getEndpoints().setA2b(true);
        final String[] attitudeDataBis = quaternionRate.createDataFields(metadata.isFirst(),
                                                                         metadata.getEndpoints().isExternal2SpacecraftBody(),
                                                                         metadata.getEulerRotSeq(),
                                                                         metadata.isSpacecraftBodyRate(),
                                                                         tsac);
        for (int i = 0; i < attitudeDataBis.length; i++) {
            Assert.assertEquals(Double.parseDouble(attitudeData[i + 1]),
                                Double.parseDouble(attitudeDataBis[i]),
                                QUATERNION_PRECISION);
        }

        // Verify angular derivative filter
        Assert.assertEquals(AngularDerivativesFilter.USE_RR, quaternionRate.getAngularDerivativesFilter());
    }

    @Test
    public void testEulerAngle() {
        // Initialize the attitude type
        final AttitudeType eulerAngle = AttitudeType.parseType("EULER ANGLE");

        // Test computation of angular coordinates from attitude data
        final String[] attitudeData = new String[] {
            "2021-01-29T21:24:37", "43.1", "12.8", "37.9"
        };
        metadata.setEulerRotSeq(RotationOrder.XYZ);
        final TimeStampedAngularCoordinates tsac = eulerAngle.parse(metadata.isFirst(),
                                                                    metadata.getEndpoints().isExternal2SpacecraftBody(),
                                                                    metadata.getEulerRotSeq(),
                                                                    metadata.isSpacecraftBodyRate(),
                                                                    context, attitudeData);
        final double[] angles = tsac.getRotation().getAngles(RotationOrder.XYZ, RotationConvention.FRAME_TRANSFORM);
        Assert.assertEquals(43.1, FastMath.toDegrees(angles[0]), ANGLE_PRECISION);
        Assert.assertEquals(12.8, FastMath.toDegrees(angles[1]), ANGLE_PRECISION);
        Assert.assertEquals(37.9, FastMath.toDegrees(angles[2]), ANGLE_PRECISION);

        // Test computation of attitude data from angular coordinates
        AemMetadata metadata = new AemMetadata(3);
        metadata.getEndpoints().setFrameA(new FrameFacade(FramesFactory.getGCRF(), CelestialBodyFrame.GCRF,
                                                          null, null, "GCRF"));
        metadata.getEndpoints().setFrameB(new FrameFacade(null, null, null,
                                                          new SpacecraftBodyFrame(SpacecraftBodyFrame.BaseEquipment.GYRO_FRAME, "1"),
                                                          "GYRO 1"));
        metadata.setRateFrameIsA(false);
        metadata.getEndpoints().setA2b(true);
        metadata.setEulerRotSeq(RotationOrder.XYZ);
        final String[] attitudeDataBis = eulerAngle.createDataFields(metadata.isFirst(),
                                                                     metadata.getEndpoints().isExternal2SpacecraftBody(),
                                                                     metadata.getEulerRotSeq(),
                                                                     metadata.isSpacecraftBodyRate(),
                                                                     tsac);
        for (int i = 0; i < attitudeDataBis.length; i++) {
            Assert.assertEquals(Double.parseDouble(attitudeData[i + 1]),
                                Double.parseDouble(attitudeDataBis[i]),
                                ANGLE_PRECISION);
        }

        // Verify angular derivative filter
        Assert.assertEquals(AngularDerivativesFilter.USE_R, eulerAngle.getAngularDerivativesFilter());
    }

    @Test
    public void testEulerAngleRateMissingRateRefFrame() {
        // Initialize the attitude type
        final AttitudeType eulerAngleRate = AttitudeType.parseType("EULER ANGLE/RATE");

        AemMetadata mdWithoutRateFrame = new AemMetadata(4);
        mdWithoutRateFrame.setTimeSystem(TimeSystem.TAI);
        mdWithoutRateFrame.getEndpoints().setFrameA(new FrameFacade(FramesFactory.getGCRF(), CelestialBodyFrame.GCRF,
                                                                    null, null, "GCRF"));
        mdWithoutRateFrame.getEndpoints().setFrameB(new FrameFacade(null, null, null,
                                                                    new SpacecraftBodyFrame(SpacecraftBodyFrame.BaseEquipment.GYRO_FRAME, "1"),
                                                                    "GYRO 1"));
        mdWithoutRateFrame.setObjectID("9999-999ZZZ");
        mdWithoutRateFrame.setObjectName("the-object");
        mdWithoutRateFrame.setAttitudeType(eulerAngleRate);
        mdWithoutRateFrame.getEndpoints().setA2b(true);
        mdWithoutRateFrame.setEulerRotSeq(RotationOrder.ZXZ);
        try {
            mdWithoutRateFrame.checkMandatoryEntriesExceptDatesAndExternalFrame(1.0);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, oe.getSpecifier());
            Assert.assertEquals(AemMetadataKey.RATE_FRAME.name(), oe.getParts()[0]);
        }
    }

    @Test
    public void testEulerAngleRate() {
        // Initialize the attitude type
        final AttitudeType eulerAngleRate = AttitudeType.parseType("EULER ANGLE/RATE");
        final RotationOrder sequence = RotationOrder.ZXY;

        // Test computation of angular coordinates from attitude data
        final String[] attitudeData = new String[] {
            "2021-01-29T21:24:37", "43.1", "12.8", "37.9", "1.452", "0.475", "1.112"
        };
        metadata.setRateFrameIsA(false);
        metadata.getEndpoints().setA2b(true);
        metadata.setEulerRotSeq(sequence);
        final TimeStampedAngularCoordinates tsac = eulerAngleRate.parse(metadata.isFirst(),
                                                                        metadata.getEndpoints().isExternal2SpacecraftBody(),
                                                                        metadata.getEulerRotSeq(),
                                                                        metadata.isSpacecraftBodyRate(),
                                                                        context, attitudeData);
        final double[] angles = tsac.getRotation().getAngles(sequence, RotationConvention.FRAME_TRANSFORM);
        Assert.assertEquals(43.1,  FastMath.toDegrees(angles[0]), ANGLE_PRECISION);
        Assert.assertEquals(12.8,  FastMath.toDegrees(angles[1]), ANGLE_PRECISION);
        Assert.assertEquals(37.9,  FastMath.toDegrees(angles[2]), ANGLE_PRECISION);
        Assert.assertEquals(1.452, FastMath.toDegrees(tsac.getRotationRate().getZ()), ANGLE_PRECISION);
        Assert.assertEquals(0.475, FastMath.toDegrees(tsac.getRotationRate().getX()), ANGLE_PRECISION);
        Assert.assertEquals(1.112, FastMath.toDegrees(tsac.getRotationRate().getY()), ANGLE_PRECISION);

        // Test computation of attitude data from angular coordinates
        AemMetadata metadata = new AemMetadata(3);
        metadata.getEndpoints().setFrameA(new FrameFacade(FramesFactory.getGCRF(), CelestialBodyFrame.GCRF,
                                                          null, null, "GCRF"));
        metadata.getEndpoints().setFrameB(new FrameFacade(null, null, null,
                                                          new SpacecraftBodyFrame(SpacecraftBodyFrame.BaseEquipment.GYRO_FRAME, "1"),
                                                          "GYRO 1"));
        metadata.setRateFrameIsA(false);
        metadata.getEndpoints().setA2b(true);
        metadata.setEulerRotSeq(sequence);
        final String[] attitudeDataBis = eulerAngleRate.createDataFields(metadata.isFirst(),
                                                                         metadata.getEndpoints().isExternal2SpacecraftBody(),
                                                                         metadata.getEulerRotSeq(),
                                                                         metadata.isSpacecraftBodyRate(),
                                                                         tsac);
        for (int i = 0; i < attitudeDataBis.length; i++) {
            Assert.assertEquals(Double.parseDouble(attitudeData[i + 1]),
                                Double.parseDouble(attitudeDataBis[i]),
                                ANGLE_PRECISION);
        }

        // Verify angular derivative filter
        Assert.assertEquals(AngularDerivativesFilter.USE_RR, eulerAngleRate.getAngularDerivativesFilter());
    }

    @Test
    public void testSymmetryQuaternion() {
        doTestSymmetry(AttitudeType.QUATERNION, 0.1, 0.2, 0.3, -0.7, 0.02, -0.05, 0.1, -0.04,
                       2.0e-16, Double.NaN);
    }

    @Test
    public void testSymmetryQuaternionDerivative() {
        doTestSymmetry(AttitudeType.QUATERNION_DERIVATIVE, 0.1, 0.2, 0.3, -0.7, 0.02, -0.05, 0.1, -0.04,
                       2.0e-16, 1.0e-16);
    }

    @Test
    public void testSymmetryQuaternionRate() {
        doTestSymmetry(AttitudeType.QUATERNION_RATE, 0.1, 0.2, 0.3, -0.7, 0.02, -0.05, 0.1, -0.04,
                       2.0e-16, 9.0e-17);
    }

    @Test
    public void testSymmetryEulerAngle() {
        doTestSymmetry(AttitudeType.EULER_ANGLE, 0.1, 0.2, 0.3, -0.7, 0.02, -0.05, 0.1, -0.04,
                       2.0e-15, Double.NaN);
    }

    @Test
    public void testSymmetryEulerAngleRate() {
        doTestSymmetry(AttitudeType.EULER_ANGLE_RATE, 0.1, 0.2, 0.3, -0.7, 0.02, -0.05, 0.1, -0.04,
                       2.0e-15, 3.0e-16);
    }

    @Test
    public void testSymmetrySpin() {
        doTestSymmetry(AttitudeType.SPIN, 0.1, 0.2, 0.3, -0.7, 0.02, -0.05, 0.1, -0.04,
                       8.0e-16, 5.0e-16);
    }

    private void doTestSymmetry(AttitudeType type,
                                double q0, double q1, double q2, double q3,
                                double q0Dot, double q1Dot, double q2Dot, double q3Dot,
                                double tolAngle, double tolRate) {
        TimeStampedAngularCoordinates tac =
                        new TimeStampedAngularCoordinates(AbsoluteDate.GLONASS_EPOCH,
                                                          new FieldRotation<>(new UnivariateDerivative1(q0, q0Dot),
                                                                              new UnivariateDerivative1(q1, q1Dot),
                                                                              new UnivariateDerivative1(q2, q2Dot),
                                                                              new UnivariateDerivative1(q3, q3Dot),
                                                                              true));
        if (type == AttitudeType.SPIN) {
            // enforce spin axis to be +Z
            tac = new TimeStampedAngularCoordinates(tac.getDate(), tac.getRotation(),
                                                    new Vector3D(tac.getRotationRate().getNorm(), Vector3D.PLUS_K),
                                                    tac.getRotationAcceleration());
        }

        for (RotationOrder order : RotationOrder.values()) {
            final double fixedTolRate;
            if (type == AttitudeType.EULER_ANGLE_RATE &&
                order.name().charAt(0) == order.name().charAt(2)) {
                // the rate definition in CCSDS cannot handle Euler angles with repeated axes
                fixedTolRate = Double.POSITIVE_INFINITY;
            } else {
                fixedTolRate = tolRate;
            }
            checkSymmetry(type, tac, true,  true,  order, true,  tolAngle, fixedTolRate);
            checkSymmetry(type, tac, true,  true,  order, false, tolAngle, fixedTolRate);
            checkSymmetry(type, tac, true,  false, order, true,  tolAngle, fixedTolRate);
            checkSymmetry(type, tac, true,  false, order, false, tolAngle, fixedTolRate);
            checkSymmetry(type, tac, false, true,  order, true,  tolAngle, fixedTolRate);
            checkSymmetry(type, tac, false, true,  order, false, tolAngle, fixedTolRate);
            checkSymmetry(type, tac, false, false, order, true,  tolAngle, fixedTolRate);
            checkSymmetry(type, tac, false, false, order, false, tolAngle, fixedTolRate);
        }
    }

    private void checkSymmetry(AttitudeType type, TimeStampedAngularCoordinates tac,
                               boolean rateFrameIsA, boolean isFirst, RotationOrder order,
                               boolean a2b, double tolAngle, double tolRate) {
        ContextBinding context = new ContextBinding(() -> IERSConventions.IERS_2010,
                                                    () -> true, () -> DataContext.getDefault(),
                                                    () -> ParsedUnitsBehavior.STRICT_COMPLIANCE, () -> null,
                                                    () -> TimeSystem.UTC, () -> 0.0, () -> 1.0);
        AemMetadata metadata = new AemMetadata(3);
        metadata.getEndpoints().setFrameA(new FrameFacade(FramesFactory.getGCRF(), CelestialBodyFrame.GCRF,
                                                          null, null, "GCRF"));
        metadata.getEndpoints().setFrameB(new FrameFacade(null, null, null,
                                                          new SpacecraftBodyFrame(SpacecraftBodyFrame.BaseEquipment.GYRO_FRAME, "1"),
                                                          "GYRO 1"));
        if (type == AttitudeType.QUATERNION_RATE || type == AttitudeType.EULER_ANGLE_RATE) {
            metadata.setRateFrameIsA(rateFrameIsA);
        }
        metadata.setIsFirst(isFirst);
        metadata.setEulerRotSeq(order);
        metadata.getEndpoints().setA2b(a2b);
        String[] data = type.createDataFields(metadata.isFirst(),
                                              metadata.getEndpoints().isExternal2SpacecraftBody(),
                                              metadata.getEulerRotSeq(),
                                              metadata.isSpacecraftBodyRate(),
                                              tac);
        String[] sData = new String[1 + data.length];
        sData[0] = tac.getDate().toString(context.getTimeSystem().getConverter(context).getTimeScale());
        System.arraycopy(data, 0, sData, 1, data.length);
        TimeStampedAngularCoordinates rebuilt = type.parse(metadata.isFirst(),
                                                           metadata.getEndpoints().isExternal2SpacecraftBody(),
                                                           metadata.getEulerRotSeq(),
                                                           metadata.isSpacecraftBodyRate(),
                                                           context, sData);
        TimeStampedAngularCoordinates diff = tac.addOffset(rebuilt.revert());
        Assert.assertEquals(0.0, diff.getRotation().getAngle(),    tolAngle);
        if (type.getAngularDerivativesFilter() != AngularDerivativesFilter.USE_R) {
            Assert.assertEquals(0.0, diff.getRotationRate().getNorm(), tolRate);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidAttitudeType() {
        AttitudeType.parseType("TAG");
    }

}
