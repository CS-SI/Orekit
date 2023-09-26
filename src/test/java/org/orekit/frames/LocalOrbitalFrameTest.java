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
package org.orekit.frames;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.BlockFieldMatrix;
import org.hipparchus.linear.BlockRealMatrix;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

public class LocalOrbitalFrameTest {

    @Test
    public void testIssue977() {
        LOFType type = LOFType.TNW;
        LocalOrbitalFrame lof = new LocalOrbitalFrame(FramesFactory.getGCRF(), type, provider, type.name());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            lof.getTransformProvider().getTransform(FieldAbsoluteDate.getJ2000Epoch(Binary64Field.getInstance()));
        });
    }

    @Test
    public void testTNW() {
        AbsoluteDate  date = initDate.shiftedBy(400);
        PVCoordinates pv   = provider.getPVCoordinates(date, inertialFrame);
        checkFrame(LOFType.TNW, date,
                   pv.getVelocity(),
                   Vector3D.crossProduct(pv.getMomentum(), pv.getVelocity()),
                   pv.getMomentum(),
                   pv.getMomentum().negate());
    }

    @Test
    public void testQSW() {
        AbsoluteDate  date = initDate.shiftedBy(400);
        PVCoordinates pv   = provider.getPVCoordinates(date, inertialFrame);
        checkFrame(LOFType.QSW, date,
                   pv.getPosition(),
                   Vector3D.crossProduct(pv.getMomentum(), pv.getPosition()),
                   pv.getMomentum(),
                   pv.getMomentum().negate());
    }

    @Test
    public void testLVLH() {
        AbsoluteDate  date = initDate.shiftedBy(400);
        PVCoordinates pv   = provider.getPVCoordinates(date, inertialFrame);
        checkFrame(LOFType.LVLH, date,
                   pv.getPosition(),
                   Vector3D.crossProduct(pv.getMomentum(), pv.getPosition()),
                   pv.getMomentum(),
                   pv.getMomentum().negate());
    }

    @Test
    public void testLVLH_CCSDS() {
        AbsoluteDate  date = initDate.shiftedBy(400);
        PVCoordinates pv   = provider.getPVCoordinates(date, inertialFrame);
        checkFrame(LOFType.LVLH_CCSDS, date,
                   Vector3D.crossProduct(pv.getMomentum(), pv.getPosition()),
                   pv.getMomentum().negate(),
                   pv.getPosition().negate(),
                   pv.getMomentum().negate());
    }

    @Test
    public void testVVLH() {
        AbsoluteDate  date = initDate.shiftedBy(400);
        PVCoordinates pv   = provider.getPVCoordinates(date, inertialFrame);
        checkFrame(LOFType.VVLH, date,
                   Vector3D.crossProduct(pv.getMomentum(), pv.getPosition()),
                   pv.getMomentum().negate(),
                   pv.getPosition().negate(),
                   pv.getMomentum().negate());
    }

    @Test
    public void testVNC() {
        AbsoluteDate  date = initDate.shiftedBy(400);
        PVCoordinates pv   = provider.getPVCoordinates(date, inertialFrame);
        checkFrame(LOFType.VNC, date,
                   pv.getVelocity(),
                   pv.getMomentum(),
                   Vector3D.crossProduct(pv.getVelocity(), pv.getMomentum()),
                   pv.getMomentum().negate());
    }

    @Test
    @DisplayName("Test transformFromLOFInToLOFOut method")
    void should_return_expected_transform_from_LOFIn_To_LOFOut() {
        // Given
        final AbsoluteDate date = new AbsoluteDate();
        final PVCoordinates pv = new PVCoordinates(new Vector3D(6378000 + 400000, 0, 0),
                                                   new Vector3D(0, 7669, 0));

        // When
        final Transform transformFromTNWToQSW = LOF.transformFromLOFInToLOFOut(LOFType.TNW, LOFType.QSW, date, pv);
        final Transform transformFromQSWToNTW = LOF.transformFromLOFInToLOFOut(LOFType.QSW, LOFType.NTW, date, pv);
        final Transform transformFromNTWToTNW = LOF.transformFromLOFInToLOFOut(LOFType.NTW, LOFType.TNW, date, pv);
        final Transform composedTransform = composeTransform(date,
                                                             transformFromTNWToQSW,
                                                             transformFromQSWToNTW,
                                                             transformFromNTWToTNW);

        final Vector3D        computedTranslation = composedTransform.getTranslation();
        final BlockRealMatrix computedRotation    = new BlockRealMatrix(composedTransform.getRotation().getMatrix());

        // Then
        final Vector3D expectedTranslation = new Vector3D(0, 0, 0);
        final RealMatrix expectedRotation = MatrixUtils.createRealIdentityMatrix(3);

        TestUtils.validateVector3D(expectedTranslation, computedTranslation, 1e-15);
        TestUtils.validateRealMatrix(expectedRotation, computedRotation, 1e-15);

    }

    @Test
    @DisplayName("Test transformFromLOFInToLOFOut (field version) method")
    void should_return_expected_field_transform_from_LOFIn_To_LOFOut() {
        // Given
        final Field<Binary64> field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> date = new FieldAbsoluteDate<>(field);
        final FieldPVCoordinates<Binary64> pv =
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(6378000 + 400000),
                                                             new Binary64(0),
                                                             new Binary64(0)),
                                         new FieldVector3D<>(new Binary64(0),
                                                             new Binary64(7669),
                                                             new Binary64(0)));

        // When
        final FieldTransform<Binary64> transformFromTNWToQSW =
                LOF.transformFromLOFInToLOFOut(LOFType.TNW, LOFType.QSW, date, pv);
        final FieldTransform<Binary64> transformFromQSWToNTW =
                LOF.transformFromLOFInToLOFOut(LOFType.QSW, LOFType.NTW, date, pv);
        final FieldTransform<Binary64> transformFromNTWToTNW =
                LOF.transformFromLOFInToLOFOut(LOFType.NTW, LOFType.TNW, date, pv);
        final FieldTransform<Binary64> composedTransform = composeFieldTransform(date,
                                                                                  transformFromTNWToQSW,
                                                                                  transformFromQSWToNTW,
                                                                                  transformFromNTWToTNW);

        final FieldVector3D<Binary64>    computedTranslation = composedTransform.getTranslation();
        final BlockFieldMatrix<Binary64> computedRotation    =
                new BlockFieldMatrix<>(composedTransform.getRotation().getMatrix());

        // Then
        final Vector3D expectedTranslation = new Vector3D(0, 0, 0);
        final RealMatrix expectedRotation = MatrixUtils.createRealIdentityMatrix(3);

        TestUtils.validateFieldVector3D(expectedTranslation, computedTranslation, 1e-15);
        TestUtils.validateFieldMatrix(expectedRotation, computedRotation, 1e-15);

    }

    @Test
    @DisplayName("Test transformFromInertial method")
    void should_return_expected_transform_from_inertial() {
        // Given
        final AbsoluteDate date = new AbsoluteDate();
        final PVCoordinates pv = new PVCoordinates(new Vector3D(6378000 + 400000, 0, 0),
                                                   new Vector3D(0, 7669, 0));

        // When
        final Transform  transformFromInertialToLOF = LOFType.TNW.transformFromInertial(date, pv);
        final Vector3D   computedTranslation        = transformFromInertialToLOF.getTranslation();
        final RealMatrix computedRotation           =
                new BlockRealMatrix(transformFromInertialToLOF.getRotation().getMatrix());

        // Then
        final Vector3D expectedTranslation = new Vector3D(-(6378000 + 400000), 0, 0);
        final RealMatrix expectedRotation = new BlockRealMatrix(new double[][] {
                { 0, 1, 0 },
                { -1, 0, 0 },
                { 0, 0, 1 }
        });

        TestUtils.validateVector3D(expectedTranslation, computedTranslation, 1e-15);
        TestUtils.validateRealMatrix(expectedRotation, computedRotation, 1e-15);

    }

    @Test
    @DisplayName("Test transformFromInertial (field version) method")
    void should_return_expected_field_transform_from_inertial() {
        // Given
        final Field<Binary64>             field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> date  = new FieldAbsoluteDate<>(field);
        final FieldPVCoordinates<Binary64> pv =
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(6378000 + 400000),
                                                             new Binary64(0),
                                                             new Binary64(0)),
                                         new FieldVector3D<>(new Binary64(0),
                                                             new Binary64(7669),
                                                             new Binary64(0)));

        // When
        final FieldTransform<Binary64> transformFromInertialToLOF = LOFType.TNW.transformFromInertial(date, pv);
        final FieldVector3D<Binary64>  computedTranslation        = transformFromInertialToLOF.getTranslation();
        final BlockFieldMatrix<Binary64> computedRotation =
                new BlockFieldMatrix<>(transformFromInertialToLOF.getRotation().getMatrix());

        // Then
        final Vector3D expectedTranslation = new Vector3D(-(6378000 + 400000), 0, 0);
        final RealMatrix expectedRotation = new BlockRealMatrix(new double[][] {
                { 0, 1, 0 },
                { -1, 0, 0 },
                { 0, 0, 1 }
        });

        TestUtils.validateFieldVector3D(expectedTranslation, computedTranslation, 1e-15);
        TestUtils.validateFieldMatrix(expectedRotation, computedRotation, 1e-15);

    }

    @Test
    @DisplayName("Test all rotation methods using default method of the interface")
    void should_return_initial_value_after_multiple_rotations_default_method() {
        // Given
        final AbsoluteDate arbitraryEpoch = AbsoluteDate.ARBITRARY_EPOCH;
        final PVCoordinates pv = new PVCoordinates(new Vector3D(6378000 + 400000, 0, 0),
                                                   new Vector3D(0, 5422.8, 5422.8));

        // When
        final Rotation rotationFromTNWToTNWInertial =
                LOFType.TNW_INERTIAL.rotationFromLOF(LOFType.TNW, arbitraryEpoch, pv);

        final Rotation rotationFromTNWInertialToQSW =
                LOFType.QSW.rotationFromLOF(LOFType.TNW_INERTIAL, arbitraryEpoch, pv);

        final Rotation rotationFromQSWToQSWInertial =
                LOFType.QSW_INERTIAL.rotationFromLOF(LOFType.QSW, arbitraryEpoch, pv);

        final Rotation rotationFromQSWInertialToLVLH =
                LOFType.LVLH.rotationFromLOF(LOFType.QSW_INERTIAL, arbitraryEpoch, pv);

        final Rotation rotationFromLVLHToLVLHInertial =
                LOFType.LVLH_INERTIAL.rotationFromLOF(LOFType.LVLH, arbitraryEpoch, pv);

        final Rotation rotationFromLVLHInertialToLVLH_CCSDS =
                LOFType.LVLH_CCSDS.rotationFromLOF(LOFType.LVLH_INERTIAL, arbitraryEpoch, pv);

        final Rotation rotationFromLVLH_CCSDSToLVLH_CCSDSInertial =
                LOFType.LVLH_CCSDS_INERTIAL.rotationFromLOF(LOFType.LVLH_CCSDS, arbitraryEpoch, pv);

        final Rotation rotationFromLVLH_CCSDSInertialToVVLH =
                LOFType.VVLH.rotationFromLOF(LOFType.LVLH_CCSDS_INERTIAL, arbitraryEpoch, pv);

        final Rotation rotationFromVVLHToVVLHInertial =
                LOFType.VVLH_INERTIAL.rotationFromLOF(LOFType.VVLH, arbitraryEpoch, pv);

        final Rotation rotationFromVVLHInertialToVNC =
                LOFType.VNC.rotationFromLOF(LOFType.VVLH_INERTIAL, arbitraryEpoch, pv);

        final Rotation rotationFromVNCToVNCInertial =
                LOFType.VNC_INERTIAL.rotationFromLOF(LOFType.VNC, arbitraryEpoch, pv);

        final Rotation rotationFromVNCInertialToNTW =
                LOFType.NTW.rotationFromLOF(LOFType.VNC_INERTIAL, arbitraryEpoch, pv);

        final Rotation rotationFromNTWToNTWInertial =
                LOFType.NTW_INERTIAL.rotationFromLOF(LOFType.NTW, arbitraryEpoch, pv);

        final Rotation rotationFromNTWInertialToEQW =
                LOFType.EQW.rotationFromLOF(LOFType.NTW_INERTIAL, arbitraryEpoch, pv);

        final Rotation rotationFromEQWToTNW =
                LOF.rotationFromLOFInToLOFOut(LOFType.EQW, LOFType.TNW, arbitraryEpoch, pv);

        final Rotation rotationFromTNWToTNW =
                composeRotations(rotationFromTNWToTNWInertial,
                                 rotationFromTNWInertialToQSW,
                                 rotationFromQSWToQSWInertial,
                                 rotationFromQSWInertialToLVLH,
                                 rotationFromLVLHToLVLHInertial,
                                 rotationFromLVLHInertialToLVLH_CCSDS,
                                 rotationFromLVLH_CCSDSToLVLH_CCSDSInertial,
                                 rotationFromLVLH_CCSDSInertialToVVLH,
                                 rotationFromVVLHToVVLHInertial,
                                 rotationFromVVLHInertialToVNC,
                                 rotationFromVNCToVNCInertial,
                                 rotationFromVNCInertialToNTW,
                                 rotationFromNTWToNTWInertial,
                                 rotationFromNTWInertialToEQW,
                                 rotationFromEQWToTNW);

        final RealMatrix rotationMatrixFromTNWToTNW =
                new BlockRealMatrix(rotationFromTNWToTNW.getMatrix());

        // Then
        final RealMatrix identityMatrix = MatrixUtils.createRealIdentityMatrix(3);

        TestUtils.validateRealMatrix(identityMatrix, rotationMatrixFromTNWToTNW, 1e-15);

    }

    @Test
    @DisplayName("Test all rotation methods (field version) using default method of the interface")
    void should_return_initial_value_after_multiple_field_rotations_default_method() {
        // Given
        final Binary64Field               field          = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> arbitraryEpoch = new FieldAbsoluteDate<>(field, AbsoluteDate.ARBITRARY_EPOCH);
        final FieldPVCoordinates<Binary64> pv =
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(6378000 + 400000),
                                                             new Binary64(0),
                                                             new Binary64(0)),
                                         new FieldVector3D<>(new Binary64(0),
                                                             new Binary64(5422.8),
                                                             new Binary64(5422.8)));

        // When
        final FieldRotation<Binary64> rotationFromTNWToTNWInertial =
                LOFType.TNW_INERTIAL.rotationFromLOF(field, LOFType.TNW, arbitraryEpoch, pv);

        final FieldRotation<Binary64> rotationFromTNWInertialToQSW =
                LOFType.QSW.rotationFromLOF(field, LOFType.TNW_INERTIAL, arbitraryEpoch, pv);

        final FieldRotation<Binary64> rotationFromQSWToQSWInertial =
                LOFType.QSW_INERTIAL.rotationFromLOF(field, LOFType.QSW, arbitraryEpoch, pv);

        final FieldRotation<Binary64> rotationFromQSWInertialToLVLH =
                LOFType.LVLH.rotationFromLOF(field, LOFType.QSW_INERTIAL, arbitraryEpoch, pv);

        final FieldRotation<Binary64> rotationFromLVLHToLVLHInertial =
                LOFType.LVLH_INERTIAL.rotationFromLOF(field, LOFType.LVLH, arbitraryEpoch, pv);

        final FieldRotation<Binary64> rotationFromLVLHInertialToLVLH_CCSDS =
                LOFType.LVLH_CCSDS.rotationFromLOF(field, LOFType.LVLH_INERTIAL, arbitraryEpoch, pv);

        final FieldRotation<Binary64> rotationFromLVLH_CCSDSToLVLH_CCSDSInertial =
                LOFType.LVLH_CCSDS_INERTIAL.rotationFromLOF(field, LOFType.LVLH_CCSDS, arbitraryEpoch, pv);

        final FieldRotation<Binary64> rotationFromLVLH_CCSDSInertialToVVLH =
                LOFType.VVLH.rotationFromLOF(field, LOFType.LVLH_CCSDS_INERTIAL, arbitraryEpoch, pv);

        final FieldRotation<Binary64> rotationFromVVLHToVVLHInertial =
                LOFType.VVLH_INERTIAL.rotationFromLOF(field, LOFType.VVLH, arbitraryEpoch, pv);

        final FieldRotation<Binary64> rotationFromVVLHInertialToVNC =
                LOFType.VNC.rotationFromLOF(field, LOFType.VVLH_INERTIAL, arbitraryEpoch, pv);

        final FieldRotation<Binary64> rotationFromVNCToVNCInertial =
                LOFType.VNC_INERTIAL.rotationFromLOF(field, LOFType.VNC, arbitraryEpoch, pv);

        final FieldRotation<Binary64> rotationFromVNCInertialToNTW =
                LOFType.NTW.rotationFromLOF(field, LOFType.VNC_INERTIAL, arbitraryEpoch, pv);

        final FieldRotation<Binary64> rotationFromNTWToNTWInertial =
                LOFType.NTW_INERTIAL.rotationFromLOF(field, LOFType.NTW, arbitraryEpoch, pv);

        final FieldRotation<Binary64> rotationFromNTWInertialToEQW =
                LOFType.EQW.rotationFromLOF(field, LOFType.NTW_INERTIAL, arbitraryEpoch, pv);

        final FieldRotation<Binary64> rotationFromEQWToTNW =
                LOF.rotationFromLOFInToLOFOut(field, LOFType.EQW, LOFType.TNW, arbitraryEpoch, pv);

        final FieldRotation<Binary64> rotationFromTNWToTNW =
                composeFieldRotations(rotationFromTNWToTNWInertial,
                                      rotationFromTNWInertialToQSW,
                                      rotationFromQSWToQSWInertial,
                                      rotationFromQSWInertialToLVLH,
                                      rotationFromLVLHToLVLHInertial,
                                      rotationFromLVLHInertialToLVLH_CCSDS,
                                      rotationFromLVLH_CCSDSToLVLH_CCSDSInertial,
                                      rotationFromLVLH_CCSDSInertialToVVLH,
                                      rotationFromVVLHToVVLHInertial,
                                      rotationFromVVLHInertialToVNC,
                                      rotationFromVNCToVNCInertial,
                                      rotationFromVNCInertialToNTW,
                                      rotationFromNTWToNTWInertial,
                                      rotationFromNTWInertialToEQW,
                                      rotationFromEQWToTNW);

        final FieldMatrix<Binary64> rotationMatrixFromTNWToTNW =
                new BlockFieldMatrix<>(rotationFromTNWToTNW.getMatrix());

        // Then
        final RealMatrix identityMatrix = MatrixUtils.createRealIdentityMatrix(3);

        TestUtils.validateFieldMatrix(identityMatrix, rotationMatrixFromTNWToTNW, 1e-15);

    }

    @Test
    @DisplayName("Test all rotation methods")
    void should_return_initial_value_after_multiple_rotations() {
        // Given
        final PVCoordinates pv = new PVCoordinates(new Vector3D(6378000 + 400000, 0, 0),
                                                   new Vector3D(0, 5422.8, 5422.8));

        // When
        final Rotation rotationFromTNWToTNWInertial =
                LOFType.TNW_INERTIAL.rotationFromLOF(LOFType.TNW, pv);

        final Rotation rotationFromTNWInertialToQSW =
                LOFType.QSW.rotationFromLOF(LOFType.TNW_INERTIAL, pv);

        final Rotation rotationFromQSWToQSWInertial =
                LOFType.QSW_INERTIAL.rotationFromLOF(LOFType.QSW, pv);

        final Rotation rotationFromQSWInertialToLVLH =
                LOFType.LVLH.rotationFromLOF(LOFType.QSW_INERTIAL, pv);

        final Rotation rotationFromLVLHToLVLHInertial =
                LOFType.LVLH_INERTIAL.rotationFromLOF(LOFType.LVLH, pv);

        final Rotation rotationFromLVLHInertialToLVLH_CCSDS =
                LOFType.LVLH_CCSDS.rotationFromLOF(LOFType.LVLH_INERTIAL, pv);

        final Rotation rotationFromLVLH_CCSDSToLVLH_CCSDSInertial =
                LOFType.LVLH_CCSDS_INERTIAL.rotationFromLOF(LOFType.LVLH_CCSDS, pv);

        final Rotation rotationFromLVLH_CCSDSInertialToVVLH =
                LOFType.VVLH.rotationFromLOF(LOFType.LVLH_CCSDS_INERTIAL, pv);

        final Rotation rotationFromVVLHToVVLHInertial =
                LOFType.VVLH_INERTIAL.rotationFromLOF(LOFType.VVLH, pv);

        final Rotation rotationFromVVLHInertialToVNC =
                LOFType.VNC.rotationFromLOF(LOFType.VVLH_INERTIAL, pv);

        final Rotation rotationFromVNCToVNCInertial =
                LOFType.VNC_INERTIAL.rotationFromLOF(LOFType.VNC, pv);

        final Rotation rotationFromVNCInertialToNTW =
                LOFType.NTW.rotationFromLOF(LOFType.VNC_INERTIAL, pv);

        final Rotation rotationFromNTWToNTWInertial =
                LOFType.NTW_INERTIAL.rotationFromLOF(LOFType.NTW, pv);

        final Rotation rotationFromNTWInertialToEQW =
                LOFType.EQW.rotationFromLOF(LOFType.NTW_INERTIAL, pv);

        final Rotation rotationFromEQWToTNW =
                LOFType.rotationFromLOFInToLOFOut(LOFType.EQW, LOFType.TNW, pv);

        final Rotation rotationFromTNWToTNW =
                composeRotations(rotationFromTNWToTNWInertial,
                        rotationFromTNWInertialToQSW,
                        rotationFromQSWToQSWInertial,
                        rotationFromQSWInertialToLVLH,
                        rotationFromLVLHToLVLHInertial,
                        rotationFromLVLHInertialToLVLH_CCSDS,
                        rotationFromLVLH_CCSDSToLVLH_CCSDSInertial,
                        rotationFromLVLH_CCSDSInertialToVVLH,
                        rotationFromVVLHToVVLHInertial,
                        rotationFromVVLHInertialToVNC,
                        rotationFromVNCToVNCInertial,
                        rotationFromVNCInertialToNTW,
                        rotationFromNTWToNTWInertial,
                        rotationFromNTWInertialToEQW,
                        rotationFromEQWToTNW);

        final RealMatrix rotationMatrixFromTNWToTNW =
                new BlockRealMatrix(rotationFromTNWToTNW.getMatrix());

        // Then
        final RealMatrix identityMatrix = MatrixUtils.createRealIdentityMatrix(3);

        TestUtils.validateRealMatrix(identityMatrix, rotationMatrixFromTNWToTNW, 1e-15);

    }

    @Test
    @DisplayName("Test all rotation methods (field version)")
    void should_return_initial_value_after_multiple_field_rotations() {
        // Given
        final Binary64Field               field = Binary64Field.getInstance();
        final FieldPVCoordinates<Binary64> pv =
                new FieldPVCoordinates<>(new FieldVector3D<>(new Binary64(6378000 + 400000),
                                                             new Binary64(0),
                                                             new Binary64(0)),
                                         new FieldVector3D<>(new Binary64(0),
                                                             new Binary64(5422.8),
                                                             new Binary64(5422.8)));

        // When
        final FieldRotation<Binary64> rotationFromTNWToTNWInertial =
                LOFType.TNW_INERTIAL.rotationFromLOF(field, LOFType.TNW, pv);

        final FieldRotation<Binary64> rotationFromTNWInertialToQSW =
                LOFType.QSW.rotationFromLOF(field, LOFType.TNW_INERTIAL, pv);

        final FieldRotation<Binary64> rotationFromQSWToQSWInertial =
                LOFType.QSW_INERTIAL.rotationFromLOF(field, LOFType.QSW, pv);

        final FieldRotation<Binary64> rotationFromQSWInertialToLVLH =
                LOFType.LVLH.rotationFromLOF(field, LOFType.QSW_INERTIAL, pv);

        final FieldRotation<Binary64> rotationFromLVLHToLVLHInertial =
                LOFType.LVLH_INERTIAL.rotationFromLOF(field, LOFType.LVLH, pv);

        final FieldRotation<Binary64> rotationFromLVLHInertialToLVLH_CCSDS =
                LOFType.LVLH_CCSDS.rotationFromLOF(field, LOFType.LVLH_INERTIAL, pv);

        final FieldRotation<Binary64> rotationFromLVLH_CCSDSToLVLH_CCSDSInertial =
                LOFType.LVLH_CCSDS_INERTIAL.rotationFromLOF(field, LOFType.LVLH_CCSDS, pv);

        final FieldRotation<Binary64> rotationFromLVLH_CCSDSInertialToVVLH =
                LOFType.VVLH.rotationFromLOF(field, LOFType.LVLH_CCSDS_INERTIAL, pv);

        final FieldRotation<Binary64> rotationFromVVLHToVVLHInertial =
                LOFType.VVLH_INERTIAL.rotationFromLOF(field, LOFType.VVLH, pv);

        final FieldRotation<Binary64> rotationFromVVLHInertialToVNC =
                LOFType.VNC.rotationFromLOF(field, LOFType.VVLH_INERTIAL, pv);

        final FieldRotation<Binary64> rotationFromVNCToVNCInertial =
                LOFType.VNC_INERTIAL.rotationFromLOF(field, LOFType.VNC, pv);

        final FieldRotation<Binary64> rotationFromVNCInertialToNTW =
                LOFType.NTW.rotationFromLOF(field, LOFType.VNC_INERTIAL, pv);

        final FieldRotation<Binary64> rotationFromNTWToNTWInertial =
                LOFType.NTW_INERTIAL.rotationFromLOF(field, LOFType.NTW, pv);

        final FieldRotation<Binary64> rotationFromNTWInertialToEQW =
                LOFType.EQW.rotationFromLOF(field, LOFType.NTW_INERTIAL, pv);

        final FieldRotation<Binary64> rotationFromEQWToTNW =
                LOFType.rotationFromLOFInToLOFOut(field, LOFType.EQW, LOFType.TNW, pv);

        final FieldRotation<Binary64> rotationFromTNWToTNW =
                composeFieldRotations(rotationFromTNWToTNWInertial,
                                      rotationFromTNWInertialToQSW,
                                      rotationFromQSWToQSWInertial,
                                      rotationFromQSWInertialToLVLH,
                                      rotationFromLVLHToLVLHInertial,
                                      rotationFromLVLHInertialToLVLH_CCSDS,
                                      rotationFromLVLH_CCSDSToLVLH_CCSDSInertial,
                                      rotationFromLVLH_CCSDSInertialToVVLH,
                                      rotationFromVVLHToVVLHInertial,
                                      rotationFromVVLHInertialToVNC,
                                      rotationFromVNCToVNCInertial,
                                      rotationFromVNCInertialToNTW,
                                      rotationFromNTWToNTWInertial,
                                      rotationFromNTWInertialToEQW,
                                      rotationFromEQWToTNW);

        final FieldMatrix<Binary64> rotationMatrixFromTNWToTNW =
                new BlockFieldMatrix<>(rotationFromTNWToTNW.getMatrix());

        // Then
        final RealMatrix identityMatrix = MatrixUtils.createRealIdentityMatrix(3);

        TestUtils.validateFieldMatrix(identityMatrix, rotationMatrixFromTNWToTNW, 1e-15);

    }

    @Test
    @DisplayName("Test isQuasiInertialMethod")
    void should_return_expected_boolean() {

        // Local orbital frame considered as pseudo-inertial
        Assertions.assertTrue(LOFType.TNW_INERTIAL.isQuasiInertial());
        Assertions.assertTrue(LOFType.NTW_INERTIAL.isQuasiInertial());
        Assertions.assertTrue(LOFType.QSW_INERTIAL.isQuasiInertial());
        Assertions.assertTrue(LOFType.VNC_INERTIAL.isQuasiInertial());
        Assertions.assertTrue(LOFType.LVLH_INERTIAL.isQuasiInertial());
        Assertions.assertTrue(LOFType.LVLH_CCSDS_INERTIAL.isQuasiInertial());
        Assertions.assertTrue(LOFType.EQW.isQuasiInertial());
        Assertions.assertTrue(LOFType.VVLH_INERTIAL.isQuasiInertial());

        // Local orbital frame considered as non pseudo-inertial
        Assertions.assertFalse(LOFType.TNW.isQuasiInertial());
        Assertions.assertFalse(LOFType.NTW.isQuasiInertial());
        Assertions.assertFalse(LOFType.QSW.isQuasiInertial());
        Assertions.assertFalse(LOFType.VNC.isQuasiInertial());
        Assertions.assertFalse(LOFType.LVLH.isQuasiInertial());
        Assertions.assertFalse(LOFType.LVLH_CCSDS.isQuasiInertial());
        Assertions.assertFalse(LOFType.VVLH.isQuasiInertial());

    }

    private Rotation composeRotations(final Rotation... rotations) {

        Rotation composedRotations = null;

        for (Rotation rotation : rotations) {
            if (composedRotations == null) {
                composedRotations = rotation;
            }
            else {
                composedRotations = composedRotations.compose(rotation, RotationConvention.FRAME_TRANSFORM);
            }
        }

        return composedRotations;
    }

    @SafeVarargs private final <T extends CalculusFieldElement<T>> FieldRotation<T> composeFieldRotations(
            final FieldRotation<T>... rotations) {

        FieldRotation<T> composedRotations = null;

        for (FieldRotation<T> rotation : rotations) {
            if (composedRotations == null) {
                composedRotations = rotation;
            }
            else {
                composedRotations = composedRotations.compose(rotation, RotationConvention.FRAME_TRANSFORM);
            }
        }

        return composedRotations;
    }

    private Transform composeTransform(final AbsoluteDate date, final Transform... transforms){
        Transform composedTransform = null;

        for (Transform transform : transforms) {
            if (composedTransform == null) {
                composedTransform = transform;
            }
            else {
                composedTransform = new Transform(date, composedTransform, transform);
            }
        }

        return composedTransform;
    }

    @SafeVarargs
    private final <T extends CalculusFieldElement<T>> FieldTransform<T> composeFieldTransform(
            final FieldAbsoluteDate<T> date,
            final FieldTransform<T>... transforms) {
        FieldTransform<T> composedTransform = null;

        for (FieldTransform<T> transform : transforms) {
            if (composedTransform == null) {
                composedTransform = transform;
            }
            else {
                composedTransform = new FieldTransform<>(date, composedTransform, transform);
            }
        }

        return composedTransform;
    }

    private void checkFrame(LOFType type, AbsoluteDate date,
                            Vector3D expectedXDirection, Vector3D expectedYDirection,
                            Vector3D expectedZDirection, Vector3D expectedRotationDirection) {
        LocalOrbitalFrame lof = new LocalOrbitalFrame(FramesFactory.getGCRF(), type, provider, type.name());

        Transform     t   = lof.getTransformTo(FramesFactory.getGCRF(), date);
        PVCoordinates pv1 = t.transformPVCoordinates(PVCoordinates.ZERO);
        Vector3D      p1  = pv1.getPosition();
        Vector3D      v1  = pv1.getVelocity();
        PVCoordinates pv2 = provider.getPVCoordinates(date, FramesFactory.getGCRF());
        Vector3D      p2  = pv2.getPosition();
        Vector3D      v2  = pv2.getVelocity();
        Assertions.assertEquals(0, p1.subtract(p2).getNorm(), 1.0e-14 * p1.getNorm());
        Assertions.assertEquals(0, v1.subtract(v2).getNorm(), 1.0e-14 * v1.getNorm());

        Vector3D xDirection = t.transformVector(Vector3D.PLUS_I);
        Vector3D yDirection = t.transformVector(Vector3D.PLUS_J);
        Vector3D zDirection = t.transformVector(Vector3D.PLUS_K);
        Assertions.assertEquals(0, Vector3D.angle(expectedXDirection, xDirection), 2.0e-15);
        Assertions.assertEquals(0, Vector3D.angle(expectedYDirection, yDirection), 1.0e-15);
        Assertions.assertEquals(0, Vector3D.angle(expectedZDirection, zDirection), 1.0e-15);
        Assertions.assertEquals(0, Vector3D.angle(expectedRotationDirection, t.getRotationRate()), 1.0e-15);

        Assertions.assertEquals(initialOrbit.getKeplerianMeanMotion(), t.getRotationRate().getNorm(), 1.0e-7);

    }

    @BeforeEach
    public void setUp() {
        inertialFrame = FramesFactory.getGCRF();
        initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        initialOrbit =
                new KeplerianOrbit(7209668.0, 0.5e-4, 1.7, 2.1, 2.9, 6.2, PositionAngleType.TRUE,
                                   inertialFrame, initDate, 3.986004415e14);
        provider = new KeplerianPropagator(initialOrbit);

    }

    private Frame                 inertialFrame;
    private AbsoluteDate          initDate;
    private Orbit                 initialOrbit;
    private PVCoordinatesProvider provider;

}
