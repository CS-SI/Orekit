/* Copyright 2002-2020 CS GROUP
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
package org.orekit.files.ccsds;

import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.TimeStampedAngularCoordinates;

public class AEMAttitudeTypeTest {

    private static final double QUATERNION_PRECISION = 1.0e-5;
    private static final double ANGLE_PRECISION = 1.0e-3;

    @Before
    public void setUp()
        throws Exception {
        Utils.setDataRoot("regular-data");
    }

    /**
     * Attitude type SPIN in CCSDS AEM files is not implemented in Orekit.
     * This test verify if an exception is thrown
     */
    @Test
    public void testSpin() {
        // Initialize the attitude type
        final AEMAttitudeType spin = AEMAttitudeType.getAttitudeType("SPIN");
        // Test exception on the first method
        try {
            spin.getAngularCoordinates(null, null, false, null);
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals(AEMAttitudeType.SPIN.getName(), oe.getParts()[0]);
        }
        // Test exception on the second method
        try {
            spin.getAttitudeData(null,false, null);
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals(AEMAttitudeType.SPIN.getName(), oe.getParts()[0]);
        }
        // Test exception on the third method
        try {
            spin.getAngularDerivativesFilter();
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals(AEMAttitudeType.SPIN.getName(), oe.getParts()[0]);
        }
    }

    /**
     * Attitude type SPIN_NUTATION in CCSDS AEM files is not implemented in Orekit.
     * This test verify if an exception is thrown
     */
    @Test
    public void testSpinNutation() {
        // Initialize the attitude type
        final AEMAttitudeType spinNutation = AEMAttitudeType.getAttitudeType("SPIN NUTATION");
        // Test exception on the first method
        try {
            spinNutation.getAngularCoordinates(null, null, false, null);
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals(AEMAttitudeType.SPIN_NUTATION.getName(), oe.getParts()[0]);
        }
        // Test exception on the second method
        try {
            spinNutation.getAttitudeData(null,false, null);
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals(AEMAttitudeType.SPIN_NUTATION.getName(), oe.getParts()[0]);
        }
        // Test exception on the third method
        try {
            spinNutation.getAngularDerivativesFilter();
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals(AEMAttitudeType.SPIN_NUTATION.getName(), oe.getParts()[0]);
        }
    }

    @Test
    public void testQuaternion() {
        // Initialize the attitude type
        final AEMAttitudeType quaternion = AEMAttitudeType.getAttitudeType("QUATERNION");

        // Test computation of angular coordinates from attitude data
        final double[] attitudeData = new double[] {
            0.68427, 0.56748, 0.03146, 0.45689
        };
        final TimeStampedAngularCoordinates tsac = quaternion.getAngularCoordinates(AbsoluteDate.J2000_EPOCH,
                                                                                    attitudeData, true, null);
        Assert.assertEquals(0.68427, tsac.getRotation().getQ0(), QUATERNION_PRECISION);
        Assert.assertEquals(0.56748, tsac.getRotation().getQ1(), QUATERNION_PRECISION);
        Assert.assertEquals(0.03146, tsac.getRotation().getQ2(), QUATERNION_PRECISION);
        Assert.assertEquals(0.45689, tsac.getRotation().getQ3(), QUATERNION_PRECISION);

        // Test computation of attitude data from angular coordinates
        final double[] attitudeDataBis = quaternion.getAttitudeData(tsac, true, null);
        for (int i = 0; i < attitudeData.length; i++) {
            Assert.assertEquals(attitudeData[i], attitudeDataBis[i], QUATERNION_PRECISION);
        }

        // Verify angular derivative filter
        Assert.assertEquals(AngularDerivativesFilter.USE_R, quaternion.getAngularDerivativesFilter());
    }

    @Test
    public void testQuaternionDerivates() {
        // Initialize the attitude type
        final AEMAttitudeType quaternion = AEMAttitudeType.getAttitudeType("QUATERNION DERIVATIVE");

        // Test computation of angular coordinates from attitude data
        final double[] attitudeData = new double[] {
            0.68427, 0.56748, 0.03146, 0.45689, 0.0, 0.0, 0.0, 0.0
        };
        final TimeStampedAngularCoordinates tsac = quaternion.getAngularCoordinates(AbsoluteDate.J2000_EPOCH,
                                                                                    attitudeData, true, null);
        Assert.assertEquals(0.68427,    tsac.getRotation().getQ0(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.56748,    tsac.getRotation().getQ1(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.03146,    tsac.getRotation().getQ2(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.45689,    tsac.getRotation().getQ3(),    QUATERNION_PRECISION);

        // Test computation of attitude data from angular coordinates
        final double[] attitudeDataBis = quaternion.getAttitudeData(tsac, true, null);
        for (int i = 0; i < attitudeData.length; i++) {
            Assert.assertEquals(attitudeData[i], attitudeDataBis[i], QUATERNION_PRECISION);
        }

        // Verify angular derivative filter
        Assert.assertEquals(AngularDerivativesFilter.USE_RR, quaternion.getAngularDerivativesFilter());
    }

    @Test
    public void testQuaternionRate() {
        // Initialize the attitude type
        final AEMAttitudeType quaternionRate = AEMAttitudeType.getAttitudeType("QUATERNION RATE");

        // Test computation of angular coordinates from attitude data
        final double[] attitudeData = new double[] {
            0.68427, 0.56748, 0.03146, 0.45689, 43.1, 12.8, 37.9
        };
        final TimeStampedAngularCoordinates tsac = quaternionRate.getAngularCoordinates(AbsoluteDate.J2000_EPOCH,
                                                                                        attitudeData, true, null);
        Assert.assertEquals(0.68427,  tsac.getRotation().getQ0(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.56748,  tsac.getRotation().getQ1(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.03146,  tsac.getRotation().getQ2(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.45689,  tsac.getRotation().getQ3(),    QUATERNION_PRECISION);
        Assert.assertEquals(FastMath.toRadians(43.1), tsac.getRotationRate().getX(), ANGLE_PRECISION);
        Assert.assertEquals(FastMath.toRadians(12.8), tsac.getRotationRate().getY(), ANGLE_PRECISION);
        Assert.assertEquals(FastMath.toRadians(37.9), tsac.getRotationRate().getZ(), ANGLE_PRECISION);

        // Test computation of attitude data from angular coordinates
        final double[] attitudeDataBis = quaternionRate.getAttitudeData(tsac, true, null);
        for (int i = 0; i < attitudeData.length; i++) {
            Assert.assertEquals(attitudeData[i], attitudeDataBis[i], QUATERNION_PRECISION);
        }

        // Verify angular derivative filter
        Assert.assertEquals(AngularDerivativesFilter.USE_RR, quaternionRate.getAngularDerivativesFilter());
    }

    @Test
    public void testEulerAngle() {
        // Initialize the attitude type
        final AEMAttitudeType eulerAngle = AEMAttitudeType.getAttitudeType("EULER ANGLE");

        // Test computation of angular coordinates from attitude data
        final double[] attitudeData = new double[] {
            43.1, 12.8, 37.9
        };
        final TimeStampedAngularCoordinates tsac = eulerAngle.getAngularCoordinates(AbsoluteDate.J2000_EPOCH,
                                                                                    attitudeData, true,
                                                                                    RotationOrder.XYZ);
        final double[] angles = tsac.getRotation().getAngles(RotationOrder.XYZ, RotationConvention.FRAME_TRANSFORM);
        Assert.assertEquals(43.1, FastMath.toDegrees(angles[0]), ANGLE_PRECISION);
        Assert.assertEquals(12.8, FastMath.toDegrees(angles[1]), ANGLE_PRECISION);
        Assert.assertEquals(37.9, FastMath.toDegrees(angles[2]), ANGLE_PRECISION);

        // Test computation of attitude data from angular coordinates
        final double[] attitudeDataBis = eulerAngle.getAttitudeData(tsac, true, RotationOrder.XYZ);
        for (int i = 0; i < attitudeData.length; i++) {
            Assert.assertEquals(attitudeData[i], attitudeDataBis[i], ANGLE_PRECISION);
        }

        // Verify angular derivative filter
        Assert.assertEquals(AngularDerivativesFilter.USE_R, eulerAngle.getAngularDerivativesFilter());
    }

    @Test
    public void testEulerAngleRate() {
        // Initialize the attitude type
        final AEMAttitudeType eulerAngleRate = AEMAttitudeType.getAttitudeType("EULER ANGLE RATE");

        // Test computation of angular coordinates from attitude data
        final double[] attitudeData = new double[] {
            43.1, 12.8, 37.9, 1.452, 0.475, 1.112
        };
        final TimeStampedAngularCoordinates tsac = eulerAngleRate.getAngularCoordinates(AbsoluteDate.J2000_EPOCH,
                                                                                        attitudeData, true,
                                                                                        RotationOrder.XYZ);
        final double[] angles = tsac.getRotation().getAngles(RotationOrder.XYZ, RotationConvention.FRAME_TRANSFORM);
        Assert.assertEquals(43.1,  FastMath.toDegrees(angles[0]), ANGLE_PRECISION);
        Assert.assertEquals(12.8,  FastMath.toDegrees(angles[1]), ANGLE_PRECISION);
        Assert.assertEquals(37.9,  FastMath.toDegrees(angles[2]), ANGLE_PRECISION);
        Assert.assertEquals(1.452, FastMath.toDegrees(tsac.getRotationRate().getX()), ANGLE_PRECISION);
        Assert.assertEquals(0.475, FastMath.toDegrees(tsac.getRotationRate().getY()), ANGLE_PRECISION);
        Assert.assertEquals(1.112, FastMath.toDegrees(tsac.getRotationRate().getZ()), ANGLE_PRECISION);

        // Test computation of attitude data from angular coordinates
        final double[] attitudeDataBis = eulerAngleRate.getAttitudeData(tsac, true, RotationOrder.XYZ);
        for (int i = 0; i < attitudeData.length; i++) {
            Assert.assertEquals(attitudeData[i], attitudeDataBis[i], ANGLE_PRECISION);
        }

        // Verify angular derivative filter
        Assert.assertEquals(AngularDerivativesFilter.USE_RR, eulerAngleRate.getAngularDerivativesFilter());
    }

    @Test
    public void testInvalidAttitudeType() {
        try {
            AEMAttitudeType.getAttitudeType("TAG");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_NULL_ATTITUDE_TYPE, oe.getSpecifier());
        }
    }

}
