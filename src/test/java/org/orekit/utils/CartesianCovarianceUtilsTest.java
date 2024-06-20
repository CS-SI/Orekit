/* Copyright 2022-2024 Romain Serra
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

package org.orekit.utils;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.orekit.frames.*;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.StateCovariance;
import org.orekit.time.AbsoluteDate;

class CartesianCovarianceUtilsTest {

    @Test
    void testConvertToLofType() {
        // GIVEN
        final Frame inputFrame = FramesFactory.getEME2000();
        final Frame outputFrame = FramesFactory.getGCRF();
        final RealMatrix matrix = buildSomeMatrix();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final RealMatrix actualMatrix = CartesianCovarianceUtils.changeReferenceFrame(inputFrame, matrix, date,
                outputFrame);
        // THEN
        final Transform transform = inputFrame.getTransformTo(outputFrame, date);
        final RealMatrix jacobianMatrix = MatrixUtils.createRealMatrix(transform.getPVJacobian());
        final RealMatrix expectedMatrix = jacobianMatrix.multiply(matrix).multiplyTransposed(jacobianMatrix);
        Assertions.assertEquals(0., expectedMatrix.subtract(actualMatrix).getNorm1(), 1e-15);
    }

    @ParameterizedTest
    @EnumSource(value = LOFType.class, names = {"QSW_INERTIAL", "TNW_INERTIAL", "LVLH_INERTIAL", "NTW_INERTIAL"})
    void testConvertToLofType(final LOFType lofType) {
        // GIVEN
        final Vector3D position = new Vector3D(1.0, 2.0, 3.0);
        final Vector3D velocity = new Vector3D(4., -0.5);
        final RealMatrix matrix = buildSomeMatrix();
        // WHEN
        final RealMatrix actualMatrix = CartesianCovarianceUtils.convertToLofType(position, velocity, matrix, lofType);
        // THEN
        final StateCovariance covariance = new StateCovariance(matrix, AbsoluteDate.ARBITRARY_EPOCH,
                FramesFactory.getGCRF(), OrbitType.CARTESIAN, null);
        final CartesianOrbit orbit = new CartesianOrbit(new PVCoordinates(position, velocity), covariance.getFrame(),
                covariance.getDate(), 1.);
        final RealMatrix expectedMatrix = covariance.changeCovarianceFrame(orbit, lofType).getMatrix();
        Assertions.assertEquals(0, expectedMatrix.subtract(actualMatrix).getNorm1(), 1e-15);
    }

    @ParameterizedTest
    @EnumSource(LOFType.class)
    void testConvertFromLofType(final LOFType lofType) {
        // GIVEN
        final Vector3D position = new Vector3D(1.0, 2.0, 3.0);
        final Vector3D velocity = new Vector3D(4., -0.5);
        final RealMatrix expectedMatrix = buildSomeMatrix();
        // WHEN
        final RealMatrix matrix = CartesianCovarianceUtils.convertToLofType(position, velocity, expectedMatrix, lofType);
        // THEN
        final RealMatrix actualMatrix = CartesianCovarianceUtils.convertFromLofType(lofType, matrix, position, velocity);
        Assertions.assertEquals(0., expectedMatrix.subtract(actualMatrix).getNorm1(), 1e-14);
    }

    private static RealMatrix buildSomeMatrix() {
        final RealMatrix matrix = MatrixUtils.createRealIdentityMatrix(6);
        final double entry = 0.4;
        final int index2 = 2;
        final int index3 = 3;
        matrix.setEntry(index3, index3, entry);
        matrix.setEntry(index2, index2, entry);
        final int index4 = 4;
        final int index5 = 5;
        final double otherEntry = -0.2;
        matrix.setEntry(index4, index5, otherEntry);
        matrix.setEntry(index5, index4, otherEntry);
        return matrix;
    }

}
