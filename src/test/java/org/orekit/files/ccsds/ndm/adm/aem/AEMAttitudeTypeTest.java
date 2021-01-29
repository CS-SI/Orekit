/* Copyright 2002-2021 CS GROUP
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

import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.files.ccsds.utils.ParsingContext;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedAngularCoordinates;

public class AEMAttitudeTypeTest {

    private static final double QUATERNION_PRECISION = 1.0e-5;
    private static final double ANGLE_PRECISION = 1.0e-3;

    AEMMetadata metadata;
    ParsingContext context;

    @Before
    public void setUp()
        throws Exception {
        Utils.setDataRoot("regular-data");
        metadata = new AEMMetadata(4);
        context  =  new ParsingContext(() -> IERSConventions.IERS_2010,
                                       () -> true,
                                       () -> DataContext.getDefault(),
                                       () -> null,
                                       metadata::getTimeSystem);
        metadata.setTimeSystem(CcsdsTimeScale.TAI);
    }

    /**
     * Attitude type SPIN in CCSDS AEM files is not implemented in Orekit.
     * This test verify if an exception is thrown
     */
    @Test
    public void testSpin() {
        // Initialize the attitude type
        final AEMAttitudeType spin = AEMAttitudeType.parseType("SPIN");
        // Test exception on the first method
        try {
            spin.parse(null, null, new String[6]);
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals(AEMAttitudeType.SPIN.name(), oe.getParts()[0]);
        }
        // Test exception on the second method
        try {
            spin.getAttitudeData(null,false, null);
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals(AEMAttitudeType.SPIN.name(), oe.getParts()[0]);
        }
        // Test exception on the third method
        try {
            spin.getAngularDerivativesFilter();
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals(AEMAttitudeType.SPIN.name(), oe.getParts()[0]);
        }
    }

    /**
     * Attitude type SPIN_NUTATION in CCSDS AEM files is not implemented in Orekit.
     * This test verify if an exception is thrown
     */
    @Test
    public void testSpinNutation() {
        // Initialize the attitude type
        final AEMAttitudeType spinNutation = AEMAttitudeType.parseType("SPIN/NUTATION");
        // Test exception on the first method
        try {
            spinNutation.parse(null, null, new String[6]);
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals(AEMAttitudeType.SPIN_NUTATION.name(), oe.getParts()[0]);
        }
        // Test exception on the second method
        try {
            spinNutation.getAttitudeData(null,false, null);
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals(AEMAttitudeType.SPIN_NUTATION.name(), oe.getParts()[0]);
        }
        // Test exception on the third method
        try {
            spinNutation.getAngularDerivativesFilter();
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals(AEMAttitudeType.SPIN_NUTATION.name(), oe.getParts()[0]);
        }
    }

    @Test
    public void testQuaternion() {
        // Initialize the attitude type
        final AEMAttitudeType quaternion = AEMAttitudeType.parseType("QUATERNION");

        // Test computation of angular coordinates from attitude data
        final String[] attitudeData = new String[] {
            "2021-01-29T21:24:37", "0.68427", "0.56748", "0.03146", "0.45689"
        };
        metadata.setIsFirst(true);
        final TimeStampedAngularCoordinates tsac = quaternion.parse(metadata, context, attitudeData);
        Assert.assertEquals(0.68427, tsac.getRotation().getQ0(), QUATERNION_PRECISION);
        Assert.assertEquals(0.56748, tsac.getRotation().getQ1(), QUATERNION_PRECISION);
        Assert.assertEquals(0.03146, tsac.getRotation().getQ2(), QUATERNION_PRECISION);
        Assert.assertEquals(0.45689, tsac.getRotation().getQ3(), QUATERNION_PRECISION);

        // Test computation of attitude data from angular coordinates
        final double[] attitudeDataBis = quaternion.getAttitudeData(tsac, true, null);
        for (int i = 0; i < attitudeDataBis.length; i++) {
            Assert.assertEquals(Double.parseDouble(attitudeData[i + 1]), attitudeDataBis[i], QUATERNION_PRECISION);
        }

        // Verify angular derivative filter
        Assert.assertEquals(AngularDerivativesFilter.USE_R, quaternion.getAngularDerivativesFilter());
    }

    @Test
    public void testQuaternionDerivates() {
        // Initialize the attitude type
        final AEMAttitudeType quaternion = AEMAttitudeType.parseType("QUATERNION/DERIVATIVE");

        // Test computation of angular coordinates from attitude data
        final String[] attitudeData = new String[] {
            "2021-01-29T21:24:37", "0.68427", "0.56748", "0.03146", "0.45689", "0.0", "0.0", "0.0", "0.0"
        };
        metadata.setIsFirst(true);
        final TimeStampedAngularCoordinates tsac = quaternion.parse(metadata, context, attitudeData);
        Assert.assertEquals(0.68427,    tsac.getRotation().getQ0(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.56748,    tsac.getRotation().getQ1(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.03146,    tsac.getRotation().getQ2(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.45689,    tsac.getRotation().getQ3(),    QUATERNION_PRECISION);

        // Test computation of attitude data from angular coordinates
        final double[] attitudeDataBis = quaternion.getAttitudeData(tsac, true, null);
        for (int i = 0; i < attitudeDataBis.length; i++) {
            Assert.assertEquals(Double.parseDouble(attitudeData[i + 1]), attitudeDataBis[i], QUATERNION_PRECISION);
        }

        // Verify angular derivative filter
        Assert.assertEquals(AngularDerivativesFilter.USE_RR, quaternion.getAngularDerivativesFilter());
    }

    @Test
    public void testQuaternionRate() {
        // Initialize the attitude type
        final AEMAttitudeType quaternionRate = AEMAttitudeType.parseType("QUATERNION/RATE");

        // Test computation of angular coordinates from attitude data
        final String[] attitudeData = new String[] {
            "2021-01-29T21:24:37", "0.56748", "0.03146", "0.45689", "0.68427", "43.1", "12.8", "37.9"
        };
        metadata.setIsFirst(false);
        final TimeStampedAngularCoordinates tsac = quaternionRate.parse(metadata, context, attitudeData);
        Assert.assertEquals(0.68427,  tsac.getRotation().getQ0(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.56748,  tsac.getRotation().getQ1(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.03146,  tsac.getRotation().getQ2(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.45689,  tsac.getRotation().getQ3(),    QUATERNION_PRECISION);
        Assert.assertEquals(FastMath.toRadians(43.1), tsac.getRotationRate().getX(), ANGLE_PRECISION);
        Assert.assertEquals(FastMath.toRadians(12.8), tsac.getRotationRate().getY(), ANGLE_PRECISION);
        Assert.assertEquals(FastMath.toRadians(37.9), tsac.getRotationRate().getZ(), ANGLE_PRECISION);

        // Test computation of attitude data from angular coordinates
        final double[] attitudeDataBis = quaternionRate.getAttitudeData(tsac, false, null);
        for (int i = 0; i < attitudeDataBis.length; i++) {
            Assert.assertEquals(Double.parseDouble(attitudeData[i + 1]), attitudeDataBis[i], QUATERNION_PRECISION);
        }

        // Verify angular derivative filter
        Assert.assertEquals(AngularDerivativesFilter.USE_RR, quaternionRate.getAngularDerivativesFilter());
    }

    @Test
    public void testEulerAngle() {
        // Initialize the attitude type
        final AEMAttitudeType eulerAngle = AEMAttitudeType.parseType("EULER ANGLE");

        // Test computation of angular coordinates from attitude data
        final String[] attitudeData = new String[] {
            "2021-01-29T21:24:37", "43.1", "12.8", "37.9"
        };
        metadata.setEulerRotSeq(RotationOrder.XYZ);
        final TimeStampedAngularCoordinates tsac = eulerAngle.parse(metadata, context, attitudeData);
        final double[] angles = tsac.getRotation().getAngles(RotationOrder.XYZ, RotationConvention.FRAME_TRANSFORM);
        Assert.assertEquals(43.1, FastMath.toDegrees(angles[0]), ANGLE_PRECISION);
        Assert.assertEquals(12.8, FastMath.toDegrees(angles[1]), ANGLE_PRECISION);
        Assert.assertEquals(37.9, FastMath.toDegrees(angles[2]), ANGLE_PRECISION);

        // Test computation of attitude data from angular coordinates
        final double[] attitudeDataBis = eulerAngle.getAttitudeData(tsac, true, RotationOrder.XYZ);
        for (int i = 0; i < attitudeDataBis.length; i++) {
            Assert.assertEquals(Double.parseDouble(attitudeData[i + 1]), attitudeDataBis[i], ANGLE_PRECISION);
        }

        // Verify angular derivative filter
        Assert.assertEquals(AngularDerivativesFilter.USE_R, eulerAngle.getAngularDerivativesFilter());
    }

    @Test
    public void testEulerAngleRate() {
        // Initialize the attitude type
        final AEMAttitudeType eulerAngleRate = AEMAttitudeType.parseType("EULER ANGLE/RATE");

        // Test computation of angular coordinates from attitude data
        final String[] attitudeData = new String[] {
            "2021-01-29T21:24:37", "43.1", "12.8", "37.9", "1.452", "0.475", "1.112"
        };
        metadata.setEulerRotSeq(RotationOrder.ZXZ);
        final TimeStampedAngularCoordinates tsac = eulerAngleRate.parse(metadata, context, attitudeData);
        final double[] angles = tsac.getRotation().getAngles(RotationOrder.ZXZ, RotationConvention.FRAME_TRANSFORM);
        Assert.assertEquals(43.1,  FastMath.toDegrees(angles[0]), ANGLE_PRECISION);
        Assert.assertEquals(12.8,  FastMath.toDegrees(angles[1]), ANGLE_PRECISION);
        Assert.assertEquals(37.9,  FastMath.toDegrees(angles[2]), ANGLE_PRECISION);
        Assert.assertEquals(1.452, FastMath.toDegrees(tsac.getRotationRate().getX()), ANGLE_PRECISION);
        Assert.assertEquals(0.475, FastMath.toDegrees(tsac.getRotationRate().getY()), ANGLE_PRECISION);
        Assert.assertEquals(1.112, FastMath.toDegrees(tsac.getRotationRate().getZ()), ANGLE_PRECISION);

        // Test computation of attitude data from angular coordinates
        final double[] attitudeDataBis = eulerAngleRate.getAttitudeData(tsac, true, RotationOrder.ZXZ);
        for (int i = 0; i < attitudeDataBis.length; i++) {
            Assert.assertEquals(Double.parseDouble(attitudeData[i + 1]), attitudeDataBis[i], ANGLE_PRECISION);
        }

        // Verify angular derivative filter
        Assert.assertEquals(AngularDerivativesFilter.USE_RR, eulerAngleRate.getAngularDerivativesFilter());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidAttitudeType() {
        AEMAttitudeType.parseType("TAG");
    }

}
