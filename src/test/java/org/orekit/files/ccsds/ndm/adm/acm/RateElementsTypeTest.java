/* Copyright 2022-2026 Thales Alenia Space
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
package org.orekit.files.ccsds.ndm.adm.acm;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedAngularCoordinates;

public class RateElementsTypeTest {

    @Test
    public void testAngVel() {
        doTest(RateElementsType.ANGVEL);
    }

    @Test
    public void testQDot() {
        doTest(RateElementsType.Q_DOT);
    }

    @Test
    public void testEularRate() {
        doTest(RateElementsType.EULER_RATE);
    }

    @Test
    public void testGyroBias() {
        try {
            doTest(RateElementsType.GYRO_BIAS);
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_UNSUPPORTED_ELEMENT_SET_TYPE, oe.getSpecifier());
        }
    }

    @Test
    public void testNone() {
        doTest(RateElementsType.NONE);
    }

    private void doTest(final RateElementsType type) {
        final TimeStampedAngularCoordinates ac =
            new TimeStampedAngularCoordinates(AbsoluteDate.ARBITRARY_EPOCH,
                                              new Rotation(0.180707, 0.73566, -0.50547, 0.41309, true),
                                              new Vector3D(1.0e-2, -2.1e-3, 4.5e-4),
                                              Vector3D.ZERO);
        final Vector3D expectedRate = type == RateElementsType.NONE ? Vector3D.ZERO : ac.getRotationRate();
        for (final RotationOrder o : RotationOrder.values()) {
            final double[] elements = type.toRawElements(ac, o);
            final TimeStampedAngularCoordinates rebuilt =
                type.toAngular(ac.getDate(), o, ac.getRotation(), 0, elements);
            Assertions.assertEquals(0.0,
                                    Vector3D.distance(expectedRate, rebuilt.getRotationRate()),
                                    1.0e-15);
        }
    }

}
