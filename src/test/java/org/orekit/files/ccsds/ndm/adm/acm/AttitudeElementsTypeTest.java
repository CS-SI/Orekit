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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AttitudeElementsTypeTest {

    @Test
    public void testQuaternion() {
        doTest(AttitudeElementsType.QUATERNION);
    }

    @Test
    public void testEulerAngles() {
        doTest(AttitudeElementsType.EULER_ANGLES);
    }

    @Test
    public void testDirectCosineMatrix() {
        doTest(AttitudeElementsType.DCM);
    }

    private void doTest(final AttitudeElementsType type) {
        final Rotation r = new Rotation(0.180707, 0.73566, -0.50547, 0.41309, true);
        for (final RotationOrder o : RotationOrder.values()) {
            final double[] elements = type.toRawElements(r, o);
            final Rotation rebuilt = type.toRotation(o, elements);
            Assertions.assertEquals(0.0, Rotation.distance(r, rebuilt), 1.0e-15);
        }
    }

}
