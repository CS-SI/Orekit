/* Copyright 2022-2026 Romain Serra
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
package org.orekit.bodies;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1Field;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.orekit.Utils;
import org.orekit.frames.FieldKinematicTransform;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.KinematicTransform;
import org.orekit.frames.StaticTransform;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.ExtendedPositionProvider;
import org.orekit.utils.PVCoordinates;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

class JPLInertialTransformProviderTest {

    private static IAUPole earthIauPole;

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
        earthIauPole = PredefinedIAUPoles.getIAUPole(JPLEphemeridesLoader.EphemerisType.EARTH, TimeScalesFactory.getTimeScales());
    }

    @ParameterizedTest
    @EnumSource(value = JPLEphemeridesLoader.EphemerisType.class, names = {"SUN", "MOON", "JUPITER", "VENUS", "MARS"})
    void testFieldTransform(final JPLEphemeridesLoader.EphemerisType ephemerisType) {
        // GIVEN
        final JPLInertialTransformProvider provider = new JPLInertialTransformProvider(mock(Frame.class),
                new TestPositionProvider(), PredefinedIAUPoles.getIAUPole(ephemerisType, TimeScalesFactory.getTimeScales()));
        final UnivariateDerivative1Field field = UnivariateDerivative1Field.getInstance();
        final FieldAbsoluteDate<UnivariateDerivative1> fieldAbsoluteDate = FieldAbsoluteDate.getArbitraryEpoch(field)
                .shiftedBy(new UnivariateDerivative1(0., 1));
        // WHEN
        final FieldTransform<UnivariateDerivative1> fieldTransform = provider.getTransform(fieldAbsoluteDate);
        // THEN
        final Transform transform = provider.getTransform(fieldAbsoluteDate.toAbsoluteDate());
        assertEquals(fieldAbsoluteDate, fieldTransform.getFieldDate());
        assertEquals(transform.getCartesian().getPosition(), fieldTransform.getTranslation().toVector3D());
        assertEquals(transform.getCartesian().getVelocity(), fieldTransform.getVelocity().toVector3D());
        assertEquals(transform.getCartesian().getAcceleration(), fieldTransform.getAcceleration().toVector3D());
        assertEquals(0., Rotation.distance(transform.getRotation(), fieldTransform.getRotation().toRotation()));
        assertArrayEquals(transform.getRotationRate().toArray(), fieldTransform.getRotationRate().toVector3D().toArray(), 1e-14);
    }

    @Test
    void testFieldKinematic() {
        // GIVEN
        final JPLInertialTransformProvider provider = new JPLInertialTransformProvider(mock(Frame.class),
                new TestPositionProvider(), earthIauPole);
        final FieldAbsoluteDate<Complex> fieldAbsoluteDate = FieldAbsoluteDate.getArbitraryEpoch(ComplexField.getInstance())
                .shiftedBy(Complex.I);
        // WHEN
        final FieldKinematicTransform<Complex> kinematicTransform = provider.getKinematicTransform(fieldAbsoluteDate);
        // THEN
        assertNotEquals(0., kinematicTransform.getRotation().getAngle().getImaginaryPart());
        final FieldTransform<Complex> transform = provider.getTransform(fieldAbsoluteDate);
        assertEquals(fieldAbsoluteDate, kinematicTransform.getFieldDate());
        assertArrayEquals(transform.getCartesian().getPosition().toVector3D().toArray(),
                kinematicTransform.getTranslation().toVector3D().toArray(), 1e-14);
        assertArrayEquals(transform.getCartesian().getVelocity().toVector3D().toArray(),
                kinematicTransform.getVelocity().toVector3D().toArray(), 1e-14);
        assertEquals(0., Rotation.distance(transform.getRotation().toRotation(), kinematicTransform.getRotation().toRotation()));
        assertEquals(transform.getRotationRate().getNorm(), kinematicTransform.getRotationRate().getNorm());
        assertArrayEquals(transform.getRotationRate().toVector3D().toArray(), kinematicTransform.getRotationRate().toVector3D().toArray(),
                1e-14);
    }

    @Test
    void testFieldStatic() {
        // GIVEN
        final JPLInertialTransformProvider provider = new JPLInertialTransformProvider(mock(Frame.class),
                new TestPositionProvider(), earthIauPole);
        final FieldAbsoluteDate<Binary64> fieldAbsoluteDate = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        // WHEN
        final FieldStaticTransform<Binary64> staticTransform = provider.getStaticTransform(fieldAbsoluteDate);
        // THEN
        final FieldTransform<Binary64> transform = provider.getTransform(fieldAbsoluteDate);
        assertEquals(fieldAbsoluteDate, staticTransform.getFieldDate());
        assertArrayEquals(transform.getTranslation().toVector3D().toArray(),
                staticTransform.getTranslation().toVector3D().toArray(), 1e-14);
        assertEquals(staticTransform.getRotation().getAngle(), transform.getRotation().getAngle());
        assertEquals(0., Rotation.distance(transform.getRotation().toRotation(), staticTransform.getRotation().toRotation()));
    }

    @Test
    void testKinematic() {
        // GIVEN
        final JPLInertialTransformProvider provider = new JPLInertialTransformProvider(mock(Frame.class),
                new TestPositionProvider(), earthIauPole);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final KinematicTransform kinematicTransform = provider.getKinematicTransform(date);
        // THEN
        final Transform transform = provider.getTransform(date);
        assertEquals(date, kinematicTransform.getDate());
        assertEquals(transform.getCartesian().getPosition(), kinematicTransform.getTranslation());
        assertEquals(transform.getCartesian().getVelocity(), kinematicTransform.getVelocity());
        assertEquals(0., Rotation.distance(transform.getRotation(), kinematicTransform.getRotation()), 1e-15);
        assertArrayEquals(transform.getRotationRate().toArray(), kinematicTransform.getRotationRate().toArray(), 1e-14);
    }

    @Test
    void testStatic() {
        // GIVEN
        final JPLInertialTransformProvider provider = new JPLInertialTransformProvider(mock(Frame.class),
                new TestPositionProvider(), earthIauPole);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final StaticTransform staticTransform = provider.getStaticTransform(date);
        // THEN
        final Transform transform = provider.getTransform(date);
        assertEquals(date, staticTransform.getDate());
        assertEquals(transform.getCartesian().getPosition(), staticTransform.getTranslation());
        assertEquals(0., Rotation.distance(transform.getRotation(), staticTransform.getRotation()), 1e-15);
    }

    @Test
    void testTransform() {
        // GIVEN
        final TestPositionProvider positionProvider = new TestPositionProvider();
        final JPLInertialTransformProvider provider = new JPLInertialTransformProvider(FramesFactory.getICRF(),
                positionProvider, earthIauPole);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final Transform transform = provider.getTransform(date);
        // THEN
        assertEquals(date, transform.getDate());
        final PVCoordinates expected = positionProvider.getPVCoordinates(date, provider.getDefiningFrame()).negate();
        assertEquals(expected.getPosition(), transform.getTranslation());
        assertEquals(expected.getVelocity(), transform.getVelocity());
        assertEquals(expected.getAcceleration(), transform.getAcceleration());
        final Vector3D pole  = earthIauPole.getPole(date);
        final Vector3D qNode = earthIauPole.getNode(date);
        final Rotation rotation = new Rotation(pole, qNode, Vector3D.PLUS_K, Vector3D.PLUS_I);
        assertEquals(0., Rotation.distance(rotation, transform.getRotation()), 1e-15);
        final FieldAbsoluteDate<UnivariateDerivative1> fieldDate = new FieldAbsoluteDate<>(UnivariateDerivative1Field.getInstance(), date)
                .shiftedBy(new UnivariateDerivative1(0., 1.));
        final FieldVector3D<UnivariateDerivative1> fieldPole = earthIauPole.getPole(fieldDate);
        final FieldVector3D<UnivariateDerivative1> fieldQNode = earthIauPole.getNode(fieldDate);
        final FieldRotation<UnivariateDerivative1> fieldRotation = new FieldRotation<>(fieldPole, fieldQNode,
                FieldVector3D.getPlusK(fieldDate.getField()), FieldVector3D.getPlusI(fieldDate.getField()));
        final AngularCoordinates coordinates = new AngularCoordinates(fieldRotation);
        assertArrayEquals(coordinates.getRotationRate().toArray(), transform.getRotationRate().toArray(), 1e-20);
        assertNotEquals(Vector3D.ZERO, transform.getRotationAcceleration());
    }

    private static class TestPositionProvider implements ExtendedPositionProvider {

        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getPosition(FieldAbsoluteDate<T> date, Frame frame) {
            return FieldVector3D.getPlusK(date.getField()).scalarMultiply(date.getMJD());
        }
    }
}
