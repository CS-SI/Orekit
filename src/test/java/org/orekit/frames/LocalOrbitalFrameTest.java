/* Copyright 2002-2022 CS GROUP
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
import org.hipparchus.util.Decimal64;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
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
            lof.getTransformProvider().getTransform(FieldAbsoluteDate.getJ2000Epoch(Decimal64Field.getInstance()));
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
        final Transform transformFromTNWToQSW = LOFType.transformFromLOFInToLOFOut(LOFType.TNW, LOFType.QSW, date, pv);
        final Transform transformFromQSWToNTW = LOFType.transformFromLOFInToLOFOut(LOFType.QSW, LOFType.NTW, date, pv);
        final Transform transformFromNTWToTNW = LOFType.transformFromLOFInToLOFOut(LOFType.NTW, LOFType.TNW, date, pv);
        final Transform composedTransform = composeTransform(date,
                                                             transformFromTNWToQSW,
                                                             transformFromQSWToNTW,
                                                             transformFromNTWToTNW);

        final Vector3D        computedTranslation = composedTransform.getTranslation();
        final BlockRealMatrix computedRotation    = new BlockRealMatrix(composedTransform.getRotation().getMatrix());

        // Then
        final Vector3D expectedTranslation = new Vector3D(0, 0, 0);
        final RealMatrix expectedRotation = MatrixUtils.createRealIdentityMatrix(3);

        validateVector3D(expectedTranslation, computedTranslation, 1e-15);
        validateRealMatrix(expectedRotation, computedRotation, 1e-15);

    }

    @Test
    @DisplayName("Test transformFromLOFInToLOFOut (field version) method")
    void should_return_expected_field_transform_from_LOFIn_To_LOFOut() {
        // Given
        final Field<Decimal64> field = Decimal64Field.getInstance();
        final FieldAbsoluteDate<Decimal64> date = new FieldAbsoluteDate<>(field);
        final FieldPVCoordinates<Decimal64> pv =
                new FieldPVCoordinates<>(new FieldVector3D<>(new Decimal64(6378000 + 400000),
                                                             new Decimal64(0),
                                                             new Decimal64(0)),
                                         new FieldVector3D<>(new Decimal64(0),
                                                             new Decimal64(7669),
                                                             new Decimal64(0)));

        // When
        final FieldTransform<Decimal64> transformFromTNWToQSW =
                LOFType.transformFromLOFInToLOFOut(field, LOFType.TNW, LOFType.QSW, date, pv);
        final FieldTransform<Decimal64> transformFromQSWToNTW =
                LOFType.transformFromLOFInToLOFOut(field, LOFType.QSW, LOFType.NTW, date, pv);
        final FieldTransform<Decimal64> transformFromNTWToTNW =
                LOFType.transformFromLOFInToLOFOut(field, LOFType.NTW, LOFType.TNW, date, pv);
        final FieldTransform<Decimal64> composedTransform = composeFieldTransform(date,
                                                                                  transformFromTNWToQSW,
                                                                                  transformFromQSWToNTW,
                                                                                  transformFromNTWToTNW);

        final FieldVector3D<Decimal64>    computedTranslation = composedTransform.getTranslation();
        final BlockFieldMatrix<Decimal64> computedRotation    =
                new BlockFieldMatrix<>(composedTransform.getRotation().getMatrix());

        // Then
        final Vector3D expectedTranslation = new Vector3D(0, 0, 0);
        final RealMatrix expectedRotation = MatrixUtils.createRealIdentityMatrix(3);

        validateFieldVector3D(expectedTranslation, computedTranslation, 1e-15);
        validateFieldMatrix(expectedRotation, computedRotation, 1e-15);

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

        validateVector3D(expectedTranslation, computedTranslation, 1e-15);
        validateRealMatrix(expectedRotation, computedRotation, 1e-15);

    }

    @Test
    @DisplayName("Test transformFromInertial (field version) method")
    void should_return_expected_field_transform_from_inertial() {
        // Given
        final Field<Decimal64>             field = Decimal64Field.getInstance();
        final FieldAbsoluteDate<Decimal64> date  = new FieldAbsoluteDate<>(field);
        final FieldPVCoordinates<Decimal64> pv =
                new FieldPVCoordinates<>(new FieldVector3D<>(new Decimal64(6378000 + 400000),
                                                             new Decimal64(0),
                                                             new Decimal64(0)),
                                         new FieldVector3D<>(new Decimal64(0),
                                                             new Decimal64(7669),
                                                             new Decimal64(0)));

        // When
        final FieldTransform<Decimal64> transformFromInertialToLOF = LOFType.TNW.transformFromInertial(date, pv);
        final FieldVector3D<Decimal64>  computedTranslation        = transformFromInertialToLOF.getTranslation();
        final BlockFieldMatrix<Decimal64> computedRotation =
                new BlockFieldMatrix<>(transformFromInertialToLOF.getRotation().getMatrix());

        // Then
        final Vector3D expectedTranslation = new Vector3D(-(6378000 + 400000), 0, 0);
        final RealMatrix expectedRotation = new BlockRealMatrix(new double[][] {
                { 0, 1, 0 },
                { -1, 0, 0 },
                { 0, 0, 1 }
        });

        validateFieldVector3D(expectedTranslation, computedTranslation, 1e-15);
        validateFieldMatrix(expectedRotation, computedRotation, 1e-15);

    }

    @Test
    @DisplayName("Tests all rotation methods")
    void should_return_initial_value_after_multiple_rotations() {
        // Given
        final PVCoordinates pv = new PVCoordinates(new Vector3D(6378000 + 400000, 0, 0),
                                                   new Vector3D(0, 5422.8, 5422.8));

        // When
        final Rotation rotationFromTNWToQSW         = LOFType.QSW.rotationFromLOFType(LOFType.TNW, pv);
        final Rotation rotationFromQSWToLVLH        = LOFType.LVLH.rotationFromLOFType(LOFType.QSW, pv);
        final Rotation rotationFromLVLHToLVLH_CCSDS = LOFType.LVLH_CCSDS.rotationFromLOFType(LOFType.LVLH, pv);
        final Rotation rotationFromLVLH_CCSDSToVVLH = LOFType.VVLH.rotationFromLOFType(LOFType.LVLH_CCSDS, pv);
        final Rotation rotationFromVVLHToVNC        = LOFType.VNC.rotationFromLOFType(LOFType.VVLH, pv);
        final Rotation rotationFromVNCToNTW         = LOFType.NTW.rotationFromLOFType(LOFType.VNC, pv);
        final Rotation rotationFromNTWToEQW         = LOFType.EQW.rotationFromLOFType(LOFType.NTW, pv);
        final Rotation rotationFromEQWToTNW         = LOFType.TNW.rotationFromLOFType(LOFType.EQW, pv);

        final Rotation rotationFromTNWToTNW =
                composeRotations(rotationFromTNWToQSW,
                                 rotationFromQSWToLVLH,
                                 rotationFromLVLHToLVLH_CCSDS,
                                 rotationFromLVLH_CCSDSToVVLH,
                                 rotationFromVVLHToVNC,
                                 rotationFromVNCToNTW,
                                 rotationFromNTWToEQW,
                                 rotationFromEQWToTNW);

        final RealMatrix rotationMatrixFromTNWToTNW =
                new BlockRealMatrix(rotationFromTNWToTNW.getMatrix());

        // Then
        final RealMatrix identityMatrix = MatrixUtils.createRealIdentityMatrix(3);

        validateRealMatrix(identityMatrix, rotationMatrixFromTNWToTNW, 1e-15);

    }

    @Test
    @DisplayName("Tests all rotation methods (field version)")
    void should_return_initial_value_after_multiple_field_rotations() {
        // Given
        final Decimal64Field               field = Decimal64Field.getInstance();
        final FieldPVCoordinates<Decimal64> pv =
                new FieldPVCoordinates<>(new FieldVector3D<>(new Decimal64(6378000 + 400000),
                                                             new Decimal64(0),
                                                             new Decimal64(0)),
                                         new FieldVector3D<>(new Decimal64(0),
                                                             new Decimal64(5422.8),
                                                             new Decimal64(5422.8)));

        // When
        final FieldRotation<Decimal64> rotationFromTNWToQSW =
                LOFType.QSW.rotationFromLOFType(field, LOFType.TNW, pv);
        final FieldRotation<Decimal64> rotationFromQSWToLVLH =
                LOFType.LVLH.rotationFromLOFType(field, LOFType.QSW, pv);
        final FieldRotation<Decimal64> rotationFromLVLHToLVLH_CCSDS =
                LOFType.LVLH_CCSDS.rotationFromLOFType(field, LOFType.LVLH, pv);
        final FieldRotation<Decimal64> rotationFromLVLH_CCSDSToVVLH =
                LOFType.VVLH.rotationFromLOFType(field, LOFType.LVLH_CCSDS, pv);
        final FieldRotation<Decimal64> rotationFromVVLHToVNC =
                LOFType.VNC.rotationFromLOFType(field, LOFType.VVLH, pv);
        final FieldRotation<Decimal64> rotationFromVNCToNTW =
                LOFType.NTW.rotationFromLOFType(field, LOFType.VNC, pv);
        final FieldRotation<Decimal64> rotationFromNTWToEQW =
                LOFType.EQW.rotationFromLOFType(field, LOFType.NTW, pv);
        final FieldRotation<Decimal64> rotationFromEQWToTNW =
                LOFType.TNW.rotationFromLOFType(field, LOFType.EQW, pv);

        final FieldRotation<Decimal64> rotationFromTNWToTNW =
                composeFieldRotations(rotationFromTNWToQSW,
                                      rotationFromQSWToLVLH,
                                      rotationFromLVLHToLVLH_CCSDS,
                                      rotationFromLVLH_CCSDSToVVLH,
                                      rotationFromVVLHToVNC,
                                      rotationFromVNCToNTW,
                                      rotationFromNTWToEQW,
                                      rotationFromEQWToTNW);

        final FieldMatrix<Decimal64> rotationMatrixFromTNWToTNW =
                new BlockFieldMatrix<>(rotationFromTNWToTNW.getMatrix());

        // Then
        final RealMatrix identityMatrix = MatrixUtils.createRealIdentityMatrix(3);

        validateFieldMatrix(identityMatrix, rotationMatrixFromTNWToTNW, 1e-15);

    }

    private void validateVector3D(final Vector3D expected, final Vector3D computed, final double threshold) {
        Assertions.assertEquals(expected.getX(), computed.getX(), threshold);
        Assertions.assertEquals(expected.getY(), computed.getY(), threshold);
        Assertions.assertEquals(expected.getZ(), computed.getZ(), threshold);

    }

    private <T extends CalculusFieldElement<T>> void validateFieldVector3D(final Vector3D expected,
                                                                           final FieldVector3D<T> computed,
                                                                           final double threshold) {
        Assertions.assertEquals(expected.getX(), computed.getX().getReal(), threshold);
        Assertions.assertEquals(expected.getY(), computed.getY().getReal(), threshold);
        Assertions.assertEquals(expected.getZ(), computed.getZ().getReal(), threshold);
    }

    private <T extends CalculusFieldElement<T>> void validateFieldMatrix(final RealMatrix reference,
                                                                         final FieldMatrix<T> computed,
                                                                         final double threshold) {
        for (int row = 0; row < reference.getRowDimension(); row++) {
            for (int column = 0; column < reference.getColumnDimension(); column++) {
                if (reference.getEntry(row, column) == 0) {
                    Assertions.assertEquals(reference.getEntry(row, column), computed.getEntry(row, column).getReal(),
                                            threshold);
                }
                else {
                    Assertions.assertEquals(reference.getEntry(row, column), computed.getEntry(row, column).getReal(),
                                            FastMath.abs(threshold * reference.getEntry(row, column)));
                }
            }
        }

    }

    private void validateRealMatrix(final RealMatrix reference,
                                    final RealMatrix computed,
                                    final double threshold) {
        for (int row = 0; row < reference.getRowDimension(); row++) {
            for (int column = 0; column < reference.getColumnDimension(); column++) {
                if (reference.getEntry(row, column) == 0) {
                    Assertions.assertEquals(reference.getEntry(row, column), computed.getEntry(row, column),
                                            threshold);
                }
                else {
                    Assertions.assertEquals(reference.getEntry(row, column), computed.getEntry(row, column),
                                            FastMath.abs(threshold * reference.getEntry(row, column)));
                }
            }
        }

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
                new KeplerianOrbit(7209668.0, 0.5e-4, 1.7, 2.1, 2.9, 6.2, PositionAngle.TRUE,
                                   inertialFrame, initDate, 3.986004415e14);
        provider = new KeplerianPropagator(initialOrbit);

    }

    private Frame                 inertialFrame;
    private AbsoluteDate          initDate;
    private Orbit                 initialOrbit;
    private PVCoordinatesProvider provider;

}
