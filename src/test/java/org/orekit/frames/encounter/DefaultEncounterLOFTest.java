/* Copyright 2002-2023 CS GROUP
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
package org.orekit.frames.encounter;

import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.FieldVector2D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.hipparchus.linear.BlockFieldMatrix;
import org.hipparchus.linear.BlockRealMatrix;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.TestUtils;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

class DefaultEncounterLOFTest {

    @Test
    @DisplayName("Test rotationFromInertial default method")
    void testReturnExpectedRotation() {
        // Given
        final AbsoluteDate arbritraryEpoch = AbsoluteDate.ARBITRARY_EPOCH;
        final PVCoordinates originPV = new PVCoordinates(new Vector3D(6378000. + 400000., 0., 0.),
                                                         new Vector3D(0., 7668.63, 0.));

        final PVCoordinates otherPV = new PVCoordinates(new Vector3D(6378000. + 400000 + 1., 0., 0.),
                                                        new Vector3D(0., 0., 7668.63));

        final EncounterLOF encounterFrame = new DefaultEncounterLOF(otherPV);

        // When
        final Rotation   computedRotation       = encounterFrame.rotationFromInertial(arbritraryEpoch, originPV);
        final RealMatrix computedRotationMatrix = new BlockRealMatrix(computedRotation.getMatrix());

        // Then
        final SinCos sinCos = FastMath.sinCos(MathUtils.SEMI_PI / 2);
        final double sin    = sinCos.sin();
        final double cos    = sinCos.cos();

        final RealMatrix expectedRotationMatrix = new BlockRealMatrix(
                new double[][] { { 1, 0, 0 }, { 0, cos, sin }, { 0, -sin, cos } });

        TestUtils.validateRealMatrix(expectedRotationMatrix, computedRotationMatrix, 1e-15);

    }

    @Test
    @DisplayName("Test rotationFromInertial (field version) default method")
    void testReturnExpectedFieldRotation() {
        // Given
        final Binary64Field     field           = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> arbritraryEpoch = new FieldAbsoluteDate<>(field, AbsoluteDate.ARBITRARY_EPOCH);
        final FieldPVCoordinates<Binary64> originPV = new FieldPVCoordinates<>(field, new PVCoordinates(
                new Vector3D(6378000. + 400000., 0., 0.), new Vector3D(0., 7668.63, 0.)));

        final PVCoordinates otherPV = new PVCoordinates(
                new Vector3D(6378000. + 400000 + 1., 0., 0.), new Vector3D(0., 0., 7668.63));

        final EncounterLOF encounterFrame = new DefaultEncounterLOF(otherPV);

        // When
        final FieldRotation<Binary64> computedRotation       = encounterFrame.rotationFromInertial(field, arbritraryEpoch, originPV);
        final FieldMatrix<Binary64>   computedRotationMatrix = new BlockFieldMatrix<>(computedRotation.getMatrix());

        // Then
        final SinCos sinCos = FastMath.sinCos(MathUtils.SEMI_PI / 2);
        final double sin    = sinCos.sin();
        final double cos    = sinCos.cos();

        final RealMatrix expectedRotationMatrix = new BlockRealMatrix(
                new double[][] { { 1, 0, 0 }, { 0, cos, sin }, { 0, -sin, cos } });

        TestUtils.validateFieldMatrix(expectedRotationMatrix, computedRotationMatrix, 1e-15);
    }

    @Test
    @DisplayName("Test isQuasiInertial method")
    void testReturnTrue() {
        // Given
        final PVCoordinates otherMock = Mockito.mock(PVCoordinates.class);

        final EncounterLOF encounterLOF = new DefaultEncounterLOF(otherMock);

        // When
        final boolean returnedInertialFlag = encounterLOF.isQuasiInertial();

        // Then
        Assertions.assertTrue(returnedInertialFlag);

    }

    @Test
    @DisplayName("Test projectOntoCollisionPlane")
    void testReturnExpectedProjectedMatrix() {
        // Given
        final PVCoordinates otherPVMock = Mockito.mock(PVCoordinates.class);

        final RealMatrix matrix = new BlockRealMatrix(new double[][] {
                { 1, 2, 3 },
                { 4, 5, 6 },
                { 7, 8, 9 }
        });

        final FieldMatrix<Binary64> fieldMatrix = new BlockFieldMatrix<>(new Binary64[][] {
                { new Binary64(1), new Binary64(2), new Binary64(3) },
                { new Binary64(4), new Binary64(5), new Binary64(6) },
                { new Binary64(7), new Binary64(8), new Binary64(9) }
        });

        final AbstractEncounterLOF encounterFrame = new DefaultEncounterLOF(otherPVMock);

        // When
        final RealMatrix            computedProjectedMatrix      = encounterFrame.projectOntoCollisionPlane(matrix);
        final FieldMatrix<Binary64> computedProjectedFieldMatrix = encounterFrame.projectOntoCollisionPlane(fieldMatrix);

        // Then
        final RealMatrix expectedProjectedMatrix = new BlockRealMatrix(new double[][] {
                { 1, 2 },
                { 4, 5 }
        });

        TestUtils.validateRealMatrix(expectedProjectedMatrix, computedProjectedMatrix, 1e-15);
        TestUtils.validateFieldMatrix(expectedProjectedMatrix, computedProjectedFieldMatrix, 1e-15);

    }

    @Test
    @DisplayName("Test projectOntoCollisionPlane")
    void testReturnExpectedProjectedVector() {
        // Given
        final PVCoordinates otherPVMock = Mockito.mock(PVCoordinates.class);

        final Vector3D vector = new Vector3D(1, 2, 3);
        final FieldVector3D<Binary64> fieldVector = new FieldVector3D<>(new Binary64(1),
                                                                        new Binary64(2),
                                                                        new Binary64(3));

        final EncounterLOF encounterFrame = new DefaultEncounterLOF(otherPVMock);

        // When
        final Vector2D                computedProjectedVector      = encounterFrame.projectOntoCollisionPlane(vector);
        final FieldVector2D<Binary64> computedProjectedFieldVector = encounterFrame.projectOntoCollisionPlane(fieldVector);

        // Then
        final Vector2D expectedProjectedVector = new Vector2D(1, 2);

        TestUtils.validateVector2D(expectedProjectedVector, computedProjectedVector, 1e-15);
        TestUtils.validateFieldVector2D(expectedProjectedVector, computedProjectedFieldVector, 1e-15);

    }

    @Test
    @DisplayName("Test getAxisNormalToCollisionPlane")
    void testReturnExpectedAxisNormalToCollisionPlane() {
        // Given
        final Field<Binary64> field = Binary64Field.getInstance();

        final PVCoordinates otherPV = Mockito.mock(PVCoordinates.class);

        final EncounterLOF encounterFrame = new DefaultEncounterLOF(otherPV);

        // When
        final Vector3D                gottenAxis      = encounterFrame.getAxisNormalToCollisionPlane();
        final FieldVector3D<Binary64> gottenFieldAxis = encounterFrame.getAxisNormalToCollisionPlane(field);

        // Then
        final Vector3D expectedAxis = new Vector3D(0, 0, 1);

        TestUtils.validateVector3D(expectedAxis, gottenAxis, 1e-15);
        TestUtils.validateFieldVector3D(expectedAxis, gottenFieldAxis, 1e-15);
    }

}
