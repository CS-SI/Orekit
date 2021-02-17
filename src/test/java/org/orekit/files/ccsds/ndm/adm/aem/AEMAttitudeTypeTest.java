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

import java.util.Locale;

import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
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
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.files.ccsds.utils.ParsingContext;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedAngularCoordinates;

public class AEMAttitudeTypeTest {

    private static final double QUATERNION_PRECISION = 1.0e-5;
    private static final double ANGLE_PRECISION = 1.0e-3;

    AemMetadata metadata;
    ParsingContext context;

    @Before
    public void setUp()
        throws Exception {
        Utils.setDataRoot("regular-data");
        metadata = new AemMetadata(4);
        context  =  new ParsingContext(() -> IERSConventions.IERS_2010,
                                       () -> true,
                                       () -> DataContext.getDefault(),
                                       () -> null,
                                       metadata::getTimeSystem);
        metadata.setTimeSystem(CcsdsTimeScale.TAI);
        metadata.getEndPoints().setExternal2Local(true);
    }

    @After
    public void tearDown() {
        metadata = null;
        context  = null;
    }

    /**
     * Attitude type SPIN in CCSDS AEM files is not implemented in Orekit.
     * This test verify if an exception is thrown
     */
    @Test
    public void testSpin() {
        // Initialize the attitude type
        final AemAttitudeType spin = AemAttitudeType.parseType("SPIN");
        // Test exception on the first method
        try {
            spin.parse(null, null, new String[6], null);
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals(AemAttitudeType.SPIN.name(), oe.getParts()[0]);
        }
        // Test exception on the second method
        try {
            spin.getAttitudeData(null, new AemMetadata(1));
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals(AemAttitudeType.SPIN.name(), oe.getParts()[0]);
        }
        // Test exception on the third method
        try {
            spin.getAngularDerivativesFilter();
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals(AemAttitudeType.SPIN.name(), oe.getParts()[0]);
        }
    }

    /**
     * Attitude type SPIN_NUTATION in CCSDS AEM files is not implemented in Orekit.
     * This test verify if an exception is thrown
     */
    @Test
    public void testSpinNutation() {
        // Initialize the attitude type
        final AemAttitudeType spinNutation = AemAttitudeType.parseType("SPIN/NUTATION");
        // Test exception on the first method
        try {
            spinNutation.parse(null, null, new String[6], null);
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals(AemAttitudeType.SPIN_NUTATION.name(), oe.getParts()[0]);
        }
        // Test exception on the second method
        try {
            spinNutation.getAttitudeData(null, new AemMetadata(1));
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals(AemAttitudeType.SPIN_NUTATION.name(), oe.getParts()[0]);
        }
        // Test exception on the third method
        try {
            spinNutation.getAngularDerivativesFilter();
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals(AemAttitudeType.SPIN_NUTATION.name(), oe.getParts()[0]);
        }
    }

    @Test
    public void testQuaternion() {
        // Initialize the attitude type
        final AemAttitudeType quaternion = AemAttitudeType.parseType("QUATERNION");

        // Test computation of angular coordinates from attitude data
        final String[] attitudeData = new String[] {
            "2021-01-29T21:24:37", "0.68427", "0.56748", "0.03146", "0.45689"
        };
        metadata.setIsFirst(true);
        final TimeStampedAngularCoordinates tsac = quaternion.parse(metadata, context, attitudeData, null);
        Assert.assertEquals(0.68427, tsac.getRotation().getQ0(), QUATERNION_PRECISION);
        Assert.assertEquals(0.56748, tsac.getRotation().getQ1(), QUATERNION_PRECISION);
        Assert.assertEquals(0.03146, tsac.getRotation().getQ2(), QUATERNION_PRECISION);
        Assert.assertEquals(0.45689, tsac.getRotation().getQ3(), QUATERNION_PRECISION);

        // Test computation of attitude data from angular coordinates
        AemMetadata metadata = new AemMetadata(3);
        metadata.setIsFirst(true);
        metadata.setLocalRates(true);
        metadata.getEndPoints().setExternal2Local(true);
        final double[] attitudeDataBis = quaternion.getAttitudeData(tsac, metadata);
        for (int i = 0; i < attitudeDataBis.length; i++) {
            Assert.assertEquals(Double.parseDouble(attitudeData[i + 1]), attitudeDataBis[i], QUATERNION_PRECISION);
        }

        // Verify angular derivative filter
        Assert.assertEquals(AngularDerivativesFilter.USE_R, quaternion.getAngularDerivativesFilter());
    }

    @Test
    public void testQuaternionDerivates() {
        // Initialize the attitude type
        final AemAttitudeType quaternion = AemAttitudeType.parseType("QUATERNION/DERIVATIVE");

        // Test computation of angular coordinates from attitude data
        final String[] attitudeData = new String[] {
            "2021-01-29T21:24:37", "0.68427", "0.56748", "0.03146", "0.45689", "0.0", "0.0", "0.0", "0.0"
        };
        metadata.setIsFirst(true);
        final TimeStampedAngularCoordinates tsac = quaternion.parse(metadata, context, attitudeData, null);
        Assert.assertEquals(0.68427,    tsac.getRotation().getQ0(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.56748,    tsac.getRotation().getQ1(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.03146,    tsac.getRotation().getQ2(),    QUATERNION_PRECISION);
        Assert.assertEquals(0.45689,    tsac.getRotation().getQ3(),    QUATERNION_PRECISION);

        // Test computation of attitude data from angular coordinates
        AemMetadata metadata = new AemMetadata(3);
        metadata.setIsFirst(true);
        metadata.setLocalRates(true);
        metadata.getEndPoints().setExternal2Local(true);
        final double[] attitudeDataBis = quaternion.getAttitudeData(tsac, metadata);
        for (int i = 0; i < attitudeDataBis.length; i++) {
            Assert.assertEquals(Double.parseDouble(attitudeData[i + 1]), attitudeDataBis[i], QUATERNION_PRECISION);
        }

        // Verify angular derivative filter
        Assert.assertEquals(AngularDerivativesFilter.USE_RR, quaternion.getAngularDerivativesFilter());
    }

    @Test
    public void testQuaternionRate() {
        // Initialize the attitude type
        final AemAttitudeType quaternionRate = AemAttitudeType.parseType("QUATERNION/RATE");

        // Test computation of angular coordinates from attitude data
        final String[] attitudeData = new String[] {
            "2021-01-29T21:24:37", "0.56748", "0.03146", "0.45689", "0.68427", "43.1", "12.8", "37.9"
        };
        metadata.setLocalRates(false);
        metadata.setIsFirst(false);
        final TimeStampedAngularCoordinates tsac = quaternionRate.parse(metadata, context, attitudeData, null);
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
        metadata.setIsFirst(false);
        metadata.setLocalRates(false);
        metadata.getEndPoints().setExternal2Local(true);
        final double[] attitudeDataBis = quaternionRate.getAttitudeData(tsac, metadata);
        for (int i = 0; i < attitudeDataBis.length; i++) {
            Assert.assertEquals(Double.parseDouble(attitudeData[i + 1]), attitudeDataBis[i], QUATERNION_PRECISION);
        }

        // Verify angular derivative filter
        Assert.assertEquals(AngularDerivativesFilter.USE_RR, quaternionRate.getAngularDerivativesFilter());
    }

    @Test
    public void testEulerAngle() {
        // Initialize the attitude type
        final AemAttitudeType eulerAngle = AemAttitudeType.parseType("EULER ANGLE");

        // Test computation of angular coordinates from attitude data
        final String[] attitudeData = new String[] {
            "2021-01-29T21:24:37", "43.1", "12.8", "37.9"
        };
        metadata.setEulerRotSeq(RotationOrder.XYZ);
        final TimeStampedAngularCoordinates tsac = eulerAngle.parse(metadata, context, attitudeData, null);
        final double[] angles = tsac.getRotation().getAngles(RotationOrder.XYZ, RotationConvention.FRAME_TRANSFORM);
        Assert.assertEquals(43.1, FastMath.toDegrees(angles[0]), ANGLE_PRECISION);
        Assert.assertEquals(12.8, FastMath.toDegrees(angles[1]), ANGLE_PRECISION);
        Assert.assertEquals(37.9, FastMath.toDegrees(angles[2]), ANGLE_PRECISION);

        // Test computation of attitude data from angular coordinates
        AemMetadata metadata = new AemMetadata(3);
        metadata.setLocalRates(true);
        metadata.setEulerRotSeq(RotationOrder.XYZ);
        metadata.getEndPoints().setExternal2Local(true);
        final double[] attitudeDataBis = eulerAngle.getAttitudeData(tsac, metadata);
        for (int i = 0; i < attitudeDataBis.length; i++) {
            Assert.assertEquals(Double.parseDouble(attitudeData[i + 1]), attitudeDataBis[i], ANGLE_PRECISION);
        }

        // Verify angular derivative filter
        Assert.assertEquals(AngularDerivativesFilter.USE_R, eulerAngle.getAngularDerivativesFilter());
    }

    @Test
    public void testEulerAngleRateMissingRateRefFrame() {
        // Initialize the attitude type
        final AemAttitudeType eulerAngleRate = AemAttitudeType.parseType("EULER ANGLE/RATE");

        // Test computation of angular coordinates from attitude data
        final String[] attitudeData = new String[] {
            "2021-01-29T21:24:37", "43.1", "12.8", "37.9", "1.452", "0.475", "1.112"
        };
        metadata.setEulerRotSeq(RotationOrder.ZXZ);
        try {
            eulerAngleRate.parse(metadata, context, attitudeData, "some-file");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_MISSING_KEYWORD, oe.getSpecifier());
            Assert.assertEquals(AemMetadataKey.RATE_FRAME.name(), oe.getParts()[0]);
            Assert.assertEquals("some-file", oe.getParts()[1]);
        }
    }

    @Test
    public void testEulerAngleRate() {
        // Initialize the attitude type
        final AemAttitudeType eulerAngleRate = AemAttitudeType.parseType("EULER ANGLE/RATE");

        // Test computation of angular coordinates from attitude data
        final String[] attitudeData = new String[] {
            "2021-01-29T21:24:37", "43.1", "12.8", "37.9", "1.452", "0.475", "1.112"
        };
        metadata.setLocalRates(true);
        metadata.setEulerRotSeq(RotationOrder.ZXZ);
        final TimeStampedAngularCoordinates tsac = eulerAngleRate.parse(metadata, context, attitudeData, null);
        final double[] angles = tsac.getRotation().getAngles(RotationOrder.ZXZ, RotationConvention.FRAME_TRANSFORM);
        Assert.assertEquals(43.1,  FastMath.toDegrees(angles[0]), ANGLE_PRECISION);
        Assert.assertEquals(12.8,  FastMath.toDegrees(angles[1]), ANGLE_PRECISION);
        Assert.assertEquals(37.9,  FastMath.toDegrees(angles[2]), ANGLE_PRECISION);
        Assert.assertEquals(1.452, FastMath.toDegrees(tsac.getRotationRate().getX()), ANGLE_PRECISION);
        Assert.assertEquals(0.475, FastMath.toDegrees(tsac.getRotationRate().getY()), ANGLE_PRECISION);
        Assert.assertEquals(1.112, FastMath.toDegrees(tsac.getRotationRate().getZ()), ANGLE_PRECISION);

        // Test computation of attitude data from angular coordinates
        AemMetadata metadata = new AemMetadata(3);
        metadata.setLocalRates(true);
        metadata.setEulerRotSeq(RotationOrder.ZXZ);
        metadata.getEndPoints().setExternal2Local(true);
        final double[] attitudeDataBis = eulerAngleRate.getAttitudeData(tsac, metadata);
        for (int i = 0; i < attitudeDataBis.length; i++) {
            Assert.assertEquals(Double.parseDouble(attitudeData[i + 1]), attitudeDataBis[i], ANGLE_PRECISION);
        }

        // Verify angular derivative filter
        Assert.assertEquals(AngularDerivativesFilter.USE_RR, eulerAngleRate.getAngularDerivativesFilter());
    }

    @Test
    public void testSymmetryQuaternion() {
        doTestSymmetry(AemAttitudeType.QUATERNION, 0.1, 0.2, 0.3, -0.7, 0.02, -0.05, 0.1, -0.04,
                       2.0e-16, Double.NaN);
    }

    @Test
    public void testSymmetryQuaternionDerivative() {
        doTestSymmetry(AemAttitudeType.QUATERNION_DERIVATIVE, 0.1, 0.2, 0.3, -0.7, 0.02, -0.05, 0.1, -0.04,
                       2.0e-16, 8.0e-17);
    }

    @Test
    public void testSymmetryQuaternionRate() {
        doTestSymmetry(AemAttitudeType.QUATERNION_RATE, 0.1, 0.2, 0.3, -0.7, 0.02, -0.05, 0.1, -0.04,
                       2.0e-16, 9.0e-17);
    }

    @Test
    public void testSymmetryEulerAngle() {
        doTestSymmetry(AemAttitudeType.EULER_ANGLE, 0.1, 0.2, 0.3, -0.7, 0.02, -0.05, 0.1, -0.04,
                       2.0e-15, Double.NaN);
    }

    @Test
    public void testSymmetryEulerAngleRate() {
        doTestSymmetry(AemAttitudeType.EULER_ANGLE_RATE, 0.1, 0.2, 0.3, -0.7, 0.02, -0.05, 0.1, -0.04,
                       2.0e-15, 3.0e-16);
    }

    private void doTestSymmetry(AemAttitudeType type,
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
        for (RotationOrder order : RotationOrder.values()) {
            checkSymmetry(type, tac, true,  true,  order, true,  tolAngle, tolRate);
            checkSymmetry(type, tac, true,  true,  order, false, tolAngle, tolRate);
            checkSymmetry(type, tac, true,  false, order, true,  tolAngle, tolRate);
            checkSymmetry(type, tac, true,  false, order, false, tolAngle, tolRate);
            checkSymmetry(type, tac, false, true,  order, true,  tolAngle, tolRate);
            checkSymmetry(type, tac, false, true,  order, false, tolAngle, tolRate);
            checkSymmetry(type, tac, false, false, order, true,  tolAngle, tolRate);
            checkSymmetry(type, tac, false, false, order, false, tolAngle, tolRate);
        }
    }

    private void checkSymmetry(AemAttitudeType type, TimeStampedAngularCoordinates tac,
                               boolean localRates, boolean isFirst, RotationOrder order, boolean ext2Local,
                               double tolAngle, double tolRate) {
        ParsingContext context = new ParsingContext(() -> IERSConventions.IERS_2010,
                                                    () -> true,
                                                    () -> DataContext.getDefault(),
                                                    () -> null,
                                                    () -> CcsdsTimeScale.UTC);
        AemMetadata metadata = new AemMetadata(3);
        metadata.setLocalRates(localRates);
        metadata.setIsFirst(isFirst);
        metadata.setEulerRotSeq(order);
        metadata.getEndPoints().setExternal2Local(ext2Local);
        double[] dData = type.getAttitudeData(tac, metadata);
        String[] sData = new String[1 + dData.length];
        sData[0] = tac.getDate().toString(context.
                                          getTimeScale().
                                          getTimeScale(context.getConventions(),
                                                       context.getDataContext().getTimeScales()));
        for (int i = 0; i < dData.length; ++i) {
            sData[i + 1] = String.format(Locale.US, "%21.15e", dData[i]);
        }
        TimeStampedAngularCoordinates rebuilt = type.parse(metadata, context, sData, "");
        TimeStampedAngularCoordinates diff = tac.addOffset(rebuilt.revert());
        Assert.assertEquals(0.0, diff.getRotation().getAngle(),    tolAngle);
        if (type.getAngularDerivativesFilter() != AngularDerivativesFilter.USE_R) {
            Assert.assertEquals(0.0, diff.getRotationRate().getNorm(), tolRate);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidAttitudeType() {
        AemAttitudeType.parseType("TAG");
    }

}
