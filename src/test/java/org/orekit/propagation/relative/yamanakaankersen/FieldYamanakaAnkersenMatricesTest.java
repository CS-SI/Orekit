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

package org.orekit.propagation.relative.yamanakaankersen;

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.utils.FieldPVCoordinates;

class FieldYamanakaAnkersenMatricesTest {

    @Test
    void testMatrixComputation() {
        final Binary64 timeSinceEpoch = new Binary64(900.);
        final Binary64 targetA = new Binary64(18000e3);
        final Binary64 targetE = new Binary64(0.8);
        final Binary64 targetInitialTheta = new Binary64(15 * FastMath.PI / 180);
        final Binary64 targetTheta = new Binary64(45 * FastMath.PI / 180);
        final Binary64 mu = new Binary64(398600e9);

        final FieldYamanakaAnkersenMatrices<Binary64> matrices =
                        (new FieldYamanakaAnkersenEquations<Binary64>()).computeMatrices(timeSinceEpoch, targetA,
                                                                                         targetE, targetInitialTheta,
                                                                                         targetTheta, mu);
        TestUtils.validateFieldMatrix(MatrixUtils.createRealMatrix(new double[][] {
                        {1., 91.65856555, -57.61316931, 7.03989058}, {0., -31.85972392, 20.71074309, -1.97740435},
                        {0., -65.71944785, 42.42148618, -3.95480869}, {0., -20.28609784, 12.4474521, -0.84853331}
        }), matrices.getInPlaneMatrix(), 1e-8);
        TestUtils.validateFieldMatrix(
                        MatrixUtils.createRealMatrix(new double[][] {{0.8660254, 0.5}, {-0.5, 0.8660254}}),
                        matrices.getOutPlaneMatrix(), 1e-8);
    }

    @Test
    void testTransform() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 timeSinceEpoch = new Binary64(900.);
        final Binary64 targetA = new Binary64(18000e3);
        final Binary64 targetE = new Binary64(0.8);
        final Binary64 targetInitialTheta = new Binary64(15 * FastMath.PI / 180);
        final Binary64 targetTheta = new Binary64(45 * FastMath.PI / 180);
        final Binary64 mu = new Binary64(398600e9);
        final double[] initial_pos = {10, 20, 30};
        final double[] initial_vel = {2, 3, 4};

        final FieldPVCoordinates<Binary64> initialPV =
                        new FieldPVCoordinates<>(new FieldVector3D<>(field, new Vector3D(initial_pos)),
                                                 new FieldVector3D<>(field, new Vector3D(initial_vel)));

        final FieldYamanakaAnkersenMatrices<Binary64> matrices =
                        (new FieldYamanakaAnkersenEquations<Binary64>()).computeMatrices(timeSinceEpoch, targetA,
                                                                                         targetE, targetInitialTheta,
                                                                                         targetTheta, mu);

        final FieldPVCoordinates<Binary64> finalPV =
                        matrices.transform(initialPV, targetInitialTheta, targetTheta, targetE, targetA, mu);

        Assertions.assertEquals(-22744.724949364987, finalPV.getPosition().getX().getReal(), 1e-10);
        Assertions.assertEquals(464.8029487831341, finalPV.getPosition().getY().getReal(), 1e-12);
        Assertions.assertEquals(8873.951448243051, finalPV.getPosition().getZ().getReal(), 1e-11);
        Assertions.assertEquals(29.836822546829715, finalPV.getVelocity().getX().getReal(), 0);
        Assertions.assertEquals(2.7524908430290833, finalPV.getVelocity().getY().getReal(), 0);
        Assertions.assertEquals(26.418869170876388, finalPV.getVelocity().getZ().getReal(), 1e-12);

    }

    @Test
    void testGetTargetTrueAnomaly() {
        final Binary64 targetTheta = new Binary64(0.65);
        final FieldYamanakaAnkersenMatrices<Binary64> matrices =
                        new FieldYamanakaAnkersenMatrices<>(new Binary64(100.), targetTheta,
                                                            MatrixUtils.createFieldIdentityMatrix(
                                                                            Binary64Field.getInstance(), 4),
                                                            MatrixUtils.createFieldIdentityMatrix(
                                                                            Binary64Field.getInstance(), 2));
        Assertions.assertEquals(targetTheta.getReal(), matrices.getTargetTrueAnomaly().getReal(), 0);

    }
}

