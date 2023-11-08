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
package org.orekit.files.ccsds.definitions;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinatesProvider;

import java.util.Arrays;

public class FrameFacadeTest {

    /**
     * Configure access to Orekit data folder for simple unit tests.
     */
    @BeforeAll
    public static void configureOrekitDataAccess() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testMapCelestial() {
        for (CelestialBodyFrame cbf : CelestialBodyFrame.values()) {
            FrameFacade ff = FrameFacade.parse(cbf.name(),
                                               IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                               true, true, true);
            Assertions.assertSame(cbf, ff.asCelestialBodyFrame());
            Assertions.assertNull(ff.asOrbitRelativeFrame());
            Assertions.assertNull(ff.asSpacecraftBodyFrame());
        }
    }

    @Test
    public void testMapLOF() {
        for (OrbitRelativeFrame orf : OrbitRelativeFrame.values()) {
            FrameFacade ff = FrameFacade.parse(orf.name(),
                                               IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                               true, true, true);
            Assertions.assertNull(ff.asCelestialBodyFrame());
            Assertions.assertSame(orf, ff.asOrbitRelativeFrame());
            Assertions.assertNull(ff.asSpacecraftBodyFrame());
        }
    }

    @Test
    public void testMapSpacecraft() {
        for (SpacecraftBodyFrame.BaseEquipment be : SpacecraftBodyFrame.BaseEquipment.values()) {
            for (String label : Arrays.asList("1", "2", "A", "B")) {
                SpacecraftBodyFrame sbf = new SpacecraftBodyFrame(be, label);
                FrameFacade ff = FrameFacade.parse(sbf.toString(),
                                                   IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                                   true, true, true);
                Assertions.assertNull(ff.asCelestialBodyFrame());
                Assertions.assertNull(ff.asOrbitRelativeFrame());
                Assertions.assertEquals(be, ff.asSpacecraftBodyFrame().getBaseEquipment());
                Assertions.assertEquals(label, ff.asSpacecraftBodyFrame().getLabel());
            }
        }
    }

    @Test
    public void testUnknownFrame() {
        final String name = "unknown";
        FrameFacade ff = FrameFacade.parse(name,
                                           IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                           true, true, true);
        Assertions.assertNull(ff.asFrame());
        Assertions.assertNull(ff.asCelestialBodyFrame());
        Assertions.assertNull(ff.asOrbitRelativeFrame());
        Assertions.assertNull(ff.asSpacecraftBodyFrame());
        Assertions.assertEquals(name, ff.getName());
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    /**
     * Test that the getTransform method returns expected rotation matrix with theta = 90° when asked the transform
     * between RTN and TNW local orbital frame.
     */
    @Test
    public void testGetTransformLofToLofAtPeriapsis() {

        // Given
        final Frame pivotFrame = FramesFactory.getGCRF();
        final Orbit orbit = new KeplerianOrbit(7.E6, 0.001, FastMath.toRadians(45.), 0., 0., 0.,
                                               PositionAngleType.TRUE, FramesFactory.getEME2000(),
                                               new AbsoluteDate(2000, 1, 1, TimeScalesFactory.getUTC()),
                                               Constants.GRIM5C1_EARTH_MU);

        final FrameFacade RTN = new FrameFacade(null, null, OrbitRelativeFrame.QSW, null, "RTN");
        final FrameFacade TNW = new FrameFacade(null, null, OrbitRelativeFrame.TNW, null, "RTN");

        // When
        final Transform lofInToLofOut = FrameFacade.getTransform(RTN, TNW, pivotFrame, orbit.getDate(), orbit);

        // Then
        final double[][] expectedRotationMatrix = new double[][] {
                { 0, 1, 0 },
                { -1, 0, 0 },
                { 0, 0, 1 } };

        validateMatrix(lofInToLofOut.getRotation().getMatrix(), expectedRotationMatrix, 1e-15);

    }

    @Test
    @DisplayName("Test that an exception is thrown if the pivot frame is not pseudo-inertial")
    void Should_throw_exception_when_given_non_inertial_pivot_frame() {

        // Given
        final FrameFacade  frameFacadeInMock         = Mockito.mock(FrameFacade.class);
        final FrameFacade  frameFacadeOutMock        = Mockito.mock(FrameFacade.class);
        final Orbit        pvCoordinatesProviderMock = Mockito.mock(Orbit.class);
        final AbsoluteDate dateMock                  = Mockito.mock(AbsoluteDate.class);

        final Frame nonInertialPivotFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

        // When & Then
        Assertions.assertThrows(OrekitException.class, () -> {
            FrameFacade.getTransform(frameFacadeInMock, frameFacadeOutMock, nonInertialPivotFrame, dateMock,
                                     pvCoordinatesProviderMock);
        });

    }

    @Test
    @DisplayName("Test that an exception is thrown if the frame facade is defined using a SpacecraftBodyFrame or a CelestialBodyFrame")
    void Should_throw_exception_when_given_unsupported_frame_facade() {

        // Given
        final AbsoluteDate          dateMock       = Mockito.mock(AbsoluteDate.class);
        final Frame                 pivot          = FramesFactory.getGCRF();
        final PVCoordinatesProvider pvProviderMock = Mockito.mock(Orbit.class);

        final FrameFacade inputCelestial =
                new FrameFacade(null, Mockito.mock(CelestialBodyFrame.class),
                                null, null, "Celestial body frame");

        final FrameFacade inputSpacecraftBody =
                new FrameFacade(null, null,
                                null, Mockito.mock(SpacecraftBodyFrame.class), "Celestial body frame");

        final FrameFacade output =
                new FrameFacade(FramesFactory.getEME2000(), null,
                                null, null, "Celestial body frame");

        // When & Then
        Assertions.assertThrows(OrekitException.class, () -> {
            FrameFacade.getTransform(inputSpacecraftBody, output, pivot, dateMock, pvProviderMock);
        });

        Assertions.assertThrows(OrekitException.class, () -> {
            FrameFacade.getTransform(inputCelestial, output, pivot, dateMock, pvProviderMock);
        });
    }

    @Test
    @DisplayName("Test that an exception is thrown if the frame facade is defined using an orbit relative frame returning a null LOFType")
    void Should_throw_exception_when_given_null_LOFType() {

        // Given
        final AbsoluteDate          dateMock       = Mockito.mock(AbsoluteDate.class);
        final Frame                 pivot          = FramesFactory.getGCRF();
        final PVCoordinatesProvider pvProviderMock = Mockito.mock(Orbit.class);

        final FrameFacade inputNullLOFType =
                new FrameFacade(null, null,
                                OrbitRelativeFrame.PQW_INERTIAL, null, "Celestial body frame");

        final FrameFacade output =
                new FrameFacade(FramesFactory.getEME2000(), null,
                                null, null, "Celestial body frame");

        // When & Then
        Assertions.assertThrows(OrekitException.class, () -> {
            FrameFacade.getTransform(inputNullLOFType, output, pivot, dateMock, pvProviderMock);
        });
    }

    @Test
    @DisplayName("Test getTransform method")
    void Should_return_identity_transform_after_multiple_transforms() {

        // Given
        final AbsoluteDate date  = new AbsoluteDate();
        final Frame        pivot = FramesFactory.getGCRF();
        final Orbit orbit = new KeplerianOrbit(7.E6, 0.001, FastMath.toRadians(45.), 0., 0., 0.,
                                               PositionAngleType.MEAN, FramesFactory.getEME2000(),
                                               date, Constants.IERS2010_EARTH_MU);

        final FrameFacade initialFrameFacade =
                new FrameFacade(FramesFactory.getGCRF(),
                                null, null, null,
                                "Initial frame");

        final FrameFacade intermediaryQSWFrameFacade =
                new FrameFacade(null,
                                null, OrbitRelativeFrame.QSW, null,
                                "QSW");

        final FrameFacade intermediaryTNWFrameFacade =
                new FrameFacade(null,
                                null, OrbitRelativeFrame.QSW, null,
                                "TNW");

        final FrameFacade intermediaryNonInertialFrameFacade =
                new FrameFacade(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                                null, null, null,
                                "Non inertial frame");

        // When
        final Transform initialToQSW =
                FrameFacade.getTransform(initialFrameFacade, intermediaryQSWFrameFacade, pivot, date, orbit);

        final Transform QSWToTNW =
                FrameFacade.getTransform(intermediaryQSWFrameFacade, intermediaryTNWFrameFacade, pivot, date, orbit);

        final Transform TNWToNonInertialFrame =
                FrameFacade.getTransform(intermediaryTNWFrameFacade, intermediaryNonInertialFrameFacade, pivot, date,
                                         orbit);

        final Transform nonInertialFrameToInitial =
                FrameFacade.getTransform(intermediaryNonInertialFrameFacade, initialFrameFacade, pivot, date, orbit);

        final Transform composedTransform = composeTransform(date,
                                                             initialToQSW,
                                                             QSWToTNW,
                                                             TNWToNonInertialFrame,
                                                             nonInertialFrameToInitial);

        final Vector3D   computedTranslation        = composedTransform.getTranslation();
        final double[][] computedRotationMatrixData = composedTransform.getRotation().getMatrix();

        // Then
        final Vector3D   expectedTranslation        = new Vector3D(0, 0, 0);
        final double[][] expectedRotationMatrixData = MatrixUtils.createRealIdentityMatrix(3).getData();

        validateVector3D(expectedTranslation, computedTranslation, 2e-9);
        validateMatrix(expectedRotationMatrixData, computedRotationMatrixData, 2e-15);
    }

    private Transform composeTransform(final AbsoluteDate date, final Transform... transforms) {
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

    private void validateVector3D(final Vector3D expected, final Vector3D computed, final double threshold) {
        Assertions.assertEquals(expected.getX(), computed.getX(), threshold);
        Assertions.assertEquals(expected.getY(), computed.getY(), threshold);
        Assertions.assertEquals(expected.getZ(), computed.getZ(), threshold);

    }

    /**
     * Assert that data double array is equals to expected.
     *
     * @param data input data to assert
     * @param expected expected data
     * @param threshold threshold for precision
     */
    private void validateMatrix(double[][] data, double[][] expected, double threshold) {
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                Assertions.assertEquals(expected[i][j], data[i][j], threshold);
            }
        }
    }

}
