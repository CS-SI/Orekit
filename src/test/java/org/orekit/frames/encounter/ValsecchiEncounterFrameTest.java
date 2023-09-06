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
import org.orekit.frames.LOF;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

class ValsecchiEncounterFrameTest {

    @Test
    @DisplayName("Test rotationFromInertial method")
    void testReturnExpectedRotation() {
        // Given
        final PVCoordinates originPV = new PVCoordinates(new Vector3D(6378000. + 400000., 0., 0.),
                                                         new Vector3D(0., 7668.63, 0.));

        final PVCoordinates otherPV = new PVCoordinates(new Vector3D(6378000. + 400000 + 1., 0., 0.),
                                                        new Vector3D(0., 0., 7668.63));

        final EncounterLOF encounterFrame = new ValsecchiEncounterFrame(otherPV);

        // When
        final Rotation   computedRotation       = encounterFrame.rotationFromInertial(originPV);
        final RealMatrix computedRotationMatrix = new BlockRealMatrix(computedRotation.getMatrix());

        // Then
        final SinCos sinCos = FastMath.sinCos(MathUtils.SEMI_PI * 1.5);
        final double sin    = sinCos.sin();
        final double cos    = sinCos.cos();

        final RealMatrix expectedRotationMatrix = new BlockRealMatrix(
                new double[][] { { 1, 0, 0 }, { 0, cos, sin }, { 0, -sin, cos } });

        TestUtils.validateRealMatrix(expectedRotationMatrix, computedRotationMatrix, 1e-15);

    }

    @Test
    @DisplayName("Test rotationFromInertial (field version) method")
    void testReturnExpectedFieldRotation() {
        // Given
        final Binary64Field field = Binary64Field.getInstance();
        final FieldPVCoordinates<Binary64> originPV = new FieldPVCoordinates<>(field, new PVCoordinates(
                new Vector3D(6378000. + 400000., 0., 0.), new Vector3D(0., 7668.63, 0.)));

        final PVCoordinates otherPV = new PVCoordinates(new Vector3D(6378000. + 400000 + 1., 0., 0.),
                                                        new Vector3D(0., 0., 7668.63));

        final EncounterLOF encounterFrame = new ValsecchiEncounterFrame(otherPV);

        // When
        final FieldRotation<Binary64> computedRotation       = encounterFrame.rotationFromInertial(field, originPV);
        final FieldMatrix<Binary64>   computedRotationMatrix = new BlockFieldMatrix<>(computedRotation.getMatrix());

        // Then
        final SinCos sinCos = FastMath.sinCos(MathUtils.SEMI_PI * 1.5);
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

        final LOF encounterLOF = new ValsecchiEncounterFrame(otherMock);

        // When
        final boolean returnedInertialFlag = encounterLOF.isQuasiInertial();

        // Then
        Assertions.assertTrue(returnedInertialFlag);

    }

    @Test
    @DisplayName("Test projectOntoCollisionPlane")
    void testReturnExpectedProjectedMatrix() {
        // Given
        final PVCoordinates otherPV = Mockito.mock(PVCoordinates.class);

        final RealMatrix matrixInEncounterFrame = new BlockRealMatrix(new double[][] {
                { 1, 2, 3 },
                { 4, 5, 6 },
                { 7, 8, 9 }
        });

        final EncounterLOF encounterFrame = new ValsecchiEncounterFrame(otherPV);

        // When
        final RealMatrix computedProjectedMatrix = encounterFrame.projectOntoCollisionPlane(matrixInEncounterFrame);

        // Then
        final RealMatrix expectedProjectedMatrix = new BlockRealMatrix(new double[][] {
                { 1, 3 },
                { 7, 9 }
        });

        TestUtils.validateRealMatrix(expectedProjectedMatrix, computedProjectedMatrix, 1e-15);

    }

    @Test
    @DisplayName("Test projectOntoCollisionPlane")
    void testReturnExpectedProjectedVector() {
        // Given
        final PVCoordinates otherPVMock = Mockito.mock(PVCoordinates.class);

        final Vector3D vectorInEncounterFrame = new Vector3D(1, 2, 3);

        final EncounterLOF encounterFrame = new ValsecchiEncounterFrame(otherPVMock);

        // When
        final Vector2D computedProjectedVector = encounterFrame.projectOntoCollisionPlane(vectorInEncounterFrame);

        // Then
        final Vector2D expectedProjectedVector = new Vector2D(1, 3);

        TestUtils.validateVector2D(expectedProjectedVector, computedProjectedVector, 1e-15);

    }

    @Test
    @DisplayName("Test getAxisNormalToCollisionPlane")
    void testReturnExpectedAxisNormalToCollisionPlane() {
        // Given
        final Field<Binary64> field = Binary64Field.getInstance();

        final PVCoordinates pvMock = Mockito.mock(PVCoordinates.class);

        final EncounterLOF encounterFrame = new ValsecchiEncounterFrame(pvMock);

        // When
        final Vector3D                gottenAxis      = encounterFrame.getAxisNormalToCollisionPlane();
        final FieldVector3D<Binary64> gottenFieldAxis = encounterFrame.getAxisNormalToCollisionPlane(field);

        // Then
        final Vector3D expectedAxis = new Vector3D(0, 1, 0);

        TestUtils.validateVector3D(expectedAxis, gottenAxis, 1e-15);
        TestUtils.validateFieldVector3D(expectedAxis, gottenFieldAxis, 1e-15);
    }

    @Test
    void testReturnExpectedName() {
        // GIVEN
        final PVCoordinates pvMock = Mockito.mock(PVCoordinates.class);

        final ValsecchiEncounterFrame encounterLOF = new ValsecchiEncounterFrame(pvMock);

        // WHEN
        final String name = encounterLOF.getName();

        // THEN
        final String expectedName = "VALSECCHI_ENCOUNTER_LOF";

        Assertions.assertEquals(expectedName, name);
    }

}
