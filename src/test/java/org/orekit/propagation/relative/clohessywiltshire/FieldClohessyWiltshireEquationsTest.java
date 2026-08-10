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

package org.orekit.propagation.relative.clohessywiltshire;

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;

class FieldClohessyWiltshireEquationsTest {

    private static final double NUMERICAL_TOLERANCE = 1e-7;

    @Test
    void testMatrixComputation() {
        final Binary64 n = new Binary64(FastMath.sqrt(Constants.EIGEN5C_EARTH_MU / FastMath.pow(6677.9933719419e3, 3)));

        final FieldClohessyWiltshireMatrices<Binary64> matrices =
                        (new FieldClohessyWiltshireEquations<Binary64>()).computeMatrices(new Binary64(8 * 3600.0), n);
        TestUtils.validateFieldMatrix(MatrixUtils.createRealMatrix(new double[][] {
                                                      {4.97868541238670, 0.00000000000000, 0.00000000000000},
                                                      {-194.242457498099, 1.00000000000000, 0.00000000000000},
                                                      {0.00000000000000, 0.00000000000000, -0.326228470795567}}),
                                      matrices.getPhiRR(), NUMERICAL_TOLERANCE);
        TestUtils.validateFieldMatrix(MatrixUtils.createRealMatrix(new double[][] {
                                                      {817.081897951423, 2292.70633171889, 0.00000000000000},
                                                      {-2292.70633171889, -83131.6724081943, 0.00000000000000},
                                                      {0.00000000000000, 0.00000000000000, 817.081897951423}}),
                                      matrices.getPhiRV(), NUMERICAL_TOLERANCE);
        TestUtils.validateFieldMatrix(MatrixUtils.createRealMatrix(new double[][] {
                                                      {0.00328085221475134, 0.00000000000000, 0.00000000000000},
                                                      {-0.00920596902838442, 0.00000000000000, 0.00000000000000},
                                                      {0.00000000000000, 0.00000000000000, -0.00109361740491711}}),
                                      matrices.getPhiVR(), NUMERICAL_TOLERANCE);
        TestUtils.validateFieldMatrix(MatrixUtils.createRealMatrix(new double[][] {
                                                      {-0.326228470795567, 1.89058190496195, 0.00000000000000},
                                                      {-1.89058190496195, -4.30491388318227, 0.00000000000000},
                                                      {0.00000000000000, 0.00000000000000, -0.326228470795567}}),
                                      matrices.getPhiVV(), NUMERICAL_TOLERANCE);
    }

    @Test
    void testTransform() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 timeSinceEpoch = new Binary64(28800.);
        final Binary64 meanMotion = new Binary64(0.00115691130679057);
        final double[] initial_pos = {10, 20, 30};
        final double[] initial_vel = {2, 3, 4};

        final FieldPVCoordinates<Binary64> initialPV =
                        new FieldPVCoordinates<>(new FieldVector3D<>(field, new Vector3D(initial_pos)),
                                                 new FieldVector3D<>(field, new Vector3D(initial_vel)));

        final FieldClohessyWiltshireMatrices<Binary64> matrices =
                        (new FieldClohessyWiltshireEquations<Binary64>()).computeMatrices(timeSinceEpoch, meanMotion);

        final FieldPVCoordinates<Binary64> finalPV = matrices.transform(initialPV);

        Assertions.assertEquals(8562.11802279, finalPV.getPosition().getX().getReal(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(-255902.93551691, finalPV.getPosition().getY().getReal(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(3258.5259775, finalPV.getPosition().getZ().getReal(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(5.0520519, finalPV.getVelocity().getX().getReal(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(-16.78808408, finalPV.getVelocity().getY().getReal(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(-1.33776678, finalPV.getVelocity().getZ().getReal(), NUMERICAL_TOLERANCE);
    }
}
