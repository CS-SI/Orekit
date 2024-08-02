/* Copyright 2002-2024 CS GROUP
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

import org.hamcrest.CoreMatchers;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldLine;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Test;
import org.orekit.OrekitMatchers;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link StaticTransform}.
 *
 * @author Evan Ward
 */
class FieldStaticTransformTest {

    /** Test creating, composing, and using a StaticTransform. */
    @Test
    void testSimpleComposition() {
        doTestSimpleComposition(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestSimpleComposition(Field<T> field) {
        
        // setup
        
        // Rotation of π/2 around Z axis
        FieldRotation<T> fieldRotation = new FieldRotation<>(
                FieldVector3D.getPlusK(field), field.getZero().newInstance(0.5 * FastMath.PI),
                RotationConvention.VECTOR_OPERATOR);
        FieldAbsoluteDate<T> date = FieldAbsoluteDate.getJ2000Epoch(field);

        // action
        
        // Compose rotation and a translation of one along X axis with using two different constructors
        FieldStaticTransform<T> fieldTransform = FieldStaticTransform.compose(
                date,
                FieldStaticTransform.of(date, fieldRotation),
                FieldStaticTransform.of(date, FieldVector3D.getPlusI(field)));
        
        // From unfielded static transform
        StaticTransform transform = StaticTransform.compose(date.toAbsoluteDate(),
                                                            StaticTransform.of(date.toAbsoluteDate(), fieldRotation.toRotation()),
                                                            StaticTransform.of(date.toAbsoluteDate(), Vector3D.PLUS_I));
        FieldStaticTransform<T> fieldTransform2 = FieldStaticTransform.of(date, transform);

        // verify
        verifyTransform(field, fieldRotation, fieldTransform);
        verifyTransform(field, fieldRotation, fieldTransform2);
    }
    
    /** Verify the transform built. */
    private <T extends CalculusFieldElement<T>> void verifyTransform(final Field<T> field,
                                                                     final FieldRotation<T> rotation,
                                                                     final FieldStaticTransform<T> transform) {
        
        final T zero = field.getZero();
        final T one  = field.getOne();
        
        // identity transform
        FieldStaticTransform<T> identity = FieldStaticTransform
                        .compose(new FieldAbsoluteDate<>(field, transform.getDate()), transform, transform.getInverse());
        
        // verify
        double tol = 1e-15;
        FieldVector3D<T> u = transform.transformPosition(new FieldVector3D<>(one, one, one));
        FieldVector3D<T> v = new FieldVector3D<>(zero, one, one);
        assertThat(u.toVector3D(), OrekitMatchers.vectorCloseTo(v.toVector3D(), tol));
        FieldVector3D<T> w = transform.transformVector(new FieldVector3D<>(zero.newInstance(1), zero.newInstance(2), zero.newInstance(3)));
        FieldVector3D<T> x = new FieldVector3D<>(zero.newInstance(-2), zero.newInstance(1), zero.newInstance(3));
        assertThat(w.toVector3D(), OrekitMatchers.vectorCloseTo(x.toVector3D(), tol));
        assertThat(transform.getTranslation().toVector3D(),
                OrekitMatchers.vectorCloseTo(Vector3D.MINUS_J, tol));
        assertThat(transform.getRotation().getAngle().getReal(),
                CoreMatchers.is(rotation.getAngle().getReal()));
        assertThat(transform.getRotation().getAxis(RotationConvention.VECTOR_OPERATOR).toVector3D(),
                CoreMatchers.is(rotation.getAxis(RotationConvention.VECTOR_OPERATOR).toVector3D()));
        assertThat(
                identity.transformPosition(u).toVector3D(),
                OrekitMatchers.vectorCloseTo(u.toVector3D(), tol));
        assertThat(
                identity.transformVector(u).toVector3D(),
                OrekitMatchers.vectorCloseTo(u.toVector3D(), tol));
        // check line transform
        FieldVector3D<T> p1 = new FieldVector3D<>(zero.newInstance(42.1e6), zero.newInstance(42.1e6), zero.newInstance(42.1e6));
        FieldVector3D<T> d  = new FieldVector3D<>(zero.newInstance(-42e6), zero.newInstance(42e6), zero.newInstance(-42e6));
        FieldLine<T> line = new FieldLine<>(p1, p1.add(d), 0);
        FieldLine<T> actualLine = transform.transformLine(line);
        assertThat(
                actualLine.getDirection().toVector3D(),
                OrekitMatchers.vectorCloseTo(transform.transformVector(d).normalize().toVector3D(), 44));
        // account for translation
        FieldVector3D<T> expectedOrigin = new FieldVector3D<>(
                        zero.newInstance(-56133332.666666666), zero.newInstance(28066666.333333333), zero.newInstance(28066666.333333333));
        assertThat(
                actualLine.getOrigin().toVector3D(),
                OrekitMatchers.vectorCloseTo(expectedOrigin.toVector3D(), 33));
        assertThat(
                actualLine.getTolerance(),
                CoreMatchers.is(line.getTolerance()));
    }

    @Test
    void testOf() {
        // GIVEN
        final AbsoluteDate expectedDate = AbsoluteDate.ARBITRARY_EPOCH;
        final Vector3D expectedTranslation = new Vector3D(1., 2., 3.);
        final Rotation rotation = new Rotation(Vector3D.MINUS_J, Vector3D.PLUS_I);
        final ComplexField field = ComplexField.getInstance();
        final Complex imaginaryComplex = Complex.I;
        final FieldAbsoluteDate<Complex> expectedFieldDate = new FieldAbsoluteDate<>(field, expectedDate)
                .shiftedBy(imaginaryComplex);
        final FieldVector3D<Complex> fieldTranslation = new FieldVector3D<>(field, expectedTranslation);
        final FieldRotation<Complex> fieldRotation = new FieldRotation<>(field, rotation);
        // WHEN
        final FieldStaticTransform<Complex> staticTransform = FieldStaticTransform.of(expectedFieldDate,
                fieldTranslation, fieldRotation);
        // WHEN
        assertEquals(expectedDate, staticTransform.getDate());
        final FieldAbsoluteDate<Complex> actualFieldDate = staticTransform.getFieldDate();
        assertEquals(staticTransform.getDate(), actualFieldDate.toAbsoluteDate());
        assertEquals(Complex.ZERO, actualFieldDate.durationFrom(expectedFieldDate));
        assertEquals(expectedTranslation, staticTransform.getTranslation().toVector3D());
        assertEquals(0., Rotation.distance(fieldRotation.toRotation(),
                staticTransform.getRotation().toRotation()));
    }

    @Test
    void testGetStaticInverse() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Vector3D translation = new Vector3D(1., 2., 3.);
        final Rotation rotation = new Rotation(Vector3D.MINUS_J, Vector3D.PLUS_I);
        final ComplexField field = ComplexField.getInstance();
        final FieldAbsoluteDate<Complex> fieldDate = new FieldAbsoluteDate<>(field, date);
        final FieldVector3D<Complex> fieldTranslation = new FieldVector3D<>(field, translation);
        final FieldRotation<Complex> fieldRotation = new FieldRotation<>(field, rotation);
        final FieldStaticTransform<Complex> staticTransform = FieldStaticTransform.of(fieldDate, fieldTranslation,
                fieldRotation);
        // WHEN
        final FieldStaticTransform<Complex> actualInverseStaticTransform = staticTransform.getStaticInverse();
        // THEN
        final FieldStaticTransform<Complex> expectedInverseStaticTransform = staticTransform.getInverse();
        assertEquals(expectedInverseStaticTransform.getDate(), actualInverseStaticTransform.getDate());
        assertEquals(expectedInverseStaticTransform.getFieldDate(),
                actualInverseStaticTransform.getFieldDate());
        assertEquals(expectedInverseStaticTransform.getTranslation().toVector3D(),
                actualInverseStaticTransform.getTranslation().toVector3D());
        assertEquals(0., Rotation.distance(expectedInverseStaticTransform.getRotation().toRotation(),
                actualInverseStaticTransform.getRotation().toRotation()));
    }

    @Test
    void testGetFieldDate() {
        // GIVEN
        final AbsoluteDate arbitraryEpoch = AbsoluteDate.ARBITRARY_EPOCH;
        final TestFieldStaticTransform testFieldStaticTransform = new TestFieldStaticTransform(arbitraryEpoch);
        // WHEN
        final FieldAbsoluteDate<Complex> actualFieldDate = testFieldStaticTransform.getFieldDate();
        // THEN
        assertEquals(testFieldStaticTransform.getDate(), actualFieldDate.toAbsoluteDate());
    }

    private static class TestFieldStaticTransform implements FieldStaticTransform<Complex> {

        private final AbsoluteDate date;

        TestFieldStaticTransform(final AbsoluteDate date) {
            this.date = date;
        }

        @Override
        public FieldVector3D<Complex> getTranslation() {
            return FieldVector3D.getPlusI(ComplexField.getInstance());
        }

        @Override
        public FieldRotation<Complex> getRotation() {
            return null;
        }

        @Override
        public FieldStaticTransform<Complex> getInverse() {
            return null;
        }

        @Override
        public AbsoluteDate getDate() {
            return date;
        }
    }

}
