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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

public class TransformProviderUtilTest {

    @Test
    void testGetCombinedProviderGetKinematicTransform() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final TransformProvider mockedProvider1 = Mockito.mock(TransformProvider.class);
        final TransformProvider mockedProvider2 = Mockito.mock(TransformProvider.class);
        final KinematicTransform kinematicTransform = KinematicTransform.of(date, new PVCoordinates(Vector3D.MINUS_I,
                Vector3D.PLUS_J));
        Mockito.when(mockedProvider1.getKinematicTransform(date)).thenReturn(kinematicTransform);
        Mockito.when(mockedProvider2.getKinematicTransform(date)).thenReturn(kinematicTransform.getInverse());
        final TransformProvider combinedProvider = TransformProviderUtils.getCombinedProvider(mockedProvider1,
                mockedProvider2);
        // WHEN
        final KinematicTransform actualTransform = combinedProvider.getKinematicTransform(date);
        // THEN
        final KinematicTransform expectedTransform = KinematicTransform.getIdentity();
        Assertions.assertEquals(expectedTransform.getDate(), actualTransform.getDate());
        Assertions.assertEquals(expectedTransform.getTranslation(), actualTransform.getTranslation());
        Assertions.assertEquals(expectedTransform.getVelocity(), actualTransform.getVelocity());
    }

    @Test
    void testGetCombinedProviderFieldGetKinematicTransform() {
        // GIVEN
        final ComplexField field = ComplexField.getInstance();
        final FieldAbsoluteDate<Complex> date = new FieldAbsoluteDate<>(field, AbsoluteDate.ARBITRARY_EPOCH);
        final TransformProvider mockedProvider1 = Mockito.mock(TransformProvider.class);
        final TransformProvider mockedProvider2 = Mockito.mock(TransformProvider.class);
        final FieldKinematicTransform<Complex> kinematicTransform = FieldKinematicTransform.of(date,
                new FieldPVCoordinates<>(field, new PVCoordinates(Vector3D.MINUS_I, Vector3D.PLUS_J)));
        Mockito.when(mockedProvider1.getKinematicTransform(date)).thenReturn(kinematicTransform);
        Mockito.when(mockedProvider2.getKinematicTransform(date)).thenReturn(kinematicTransform.getInverse());
        final TransformProvider combinedProvider = TransformProviderUtils.getCombinedProvider(mockedProvider1,
                mockedProvider2);
        // WHEN
        final FieldKinematicTransform<Complex> actualTransform = combinedProvider.getKinematicTransform(date);
        // THEN
        final FieldKinematicTransform<Complex> expectedTransform = FieldKinematicTransform.getIdentity(field);
        Assertions.assertEquals(expectedTransform.getDate(), actualTransform.getDate());
        Assertions.assertEquals(expectedTransform.getTranslation().toVector3D(),
                actualTransform.getTranslation().toVector3D());
        Assertions.assertEquals(expectedTransform.getVelocity().toVector3D(),
                actualTransform.getVelocity().toVector3D());
    }

    @Test
    void testGetCombinedProviderFieldGetStaticTransform() {
        // GIVEN
        final ComplexField field = ComplexField.getInstance();
        final FieldAbsoluteDate<Complex> date = new FieldAbsoluteDate<>(field, AbsoluteDate.ARBITRARY_EPOCH);
        final TransformProvider mockedProvider1 = Mockito.mock(TransformProvider.class);
        final TransformProvider mockedProvider2 = Mockito.mock(TransformProvider.class);
        final FieldStaticTransform<Complex> staticTransform = FieldStaticTransform.of(date,
                new FieldVector3D<>(field, Vector3D.PLUS_J));
        Mockito.when(mockedProvider1.getStaticTransform(date)).thenReturn(staticTransform);
        Mockito.when(mockedProvider2.getStaticTransform(date)).thenReturn(staticTransform.getInverse());
        final TransformProvider combinedProvider = TransformProviderUtils.getCombinedProvider(mockedProvider1,
                mockedProvider2);
        // WHEN
        final FieldStaticTransform<Complex> actualTransform = combinedProvider.getStaticTransform(date);
        // THEN
        final FieldStaticTransform<Complex> expectedTransform = FieldStaticTransform.getIdentity(field);
        Assertions.assertEquals(expectedTransform.getDate(), actualTransform.getDate());
        Assertions.assertEquals(expectedTransform.getTranslation().toVector3D(),
                actualTransform.getTranslation().toVector3D());
    }

    @Test
    void testGetReversedProviderGetKinematicTransform() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final TransformProvider mockedProvider = Mockito.mock(TransformProvider.class);
        final TransformProvider reversedProvider = TransformProviderUtils.getReversedProvider(mockedProvider);
        final KinematicTransform expectedTransform = Mockito.mock(KinematicTransform.class);
        final KinematicTransform mockedTransform = Mockito.mock(KinematicTransform.class);
        Mockito.when(mockedTransform.getInverse()).thenReturn(expectedTransform);
        Mockito.when(mockedProvider.getKinematicTransform(date)).thenReturn(mockedTransform);
        // WHEN
        final KinematicTransform actualTransform = reversedProvider.getKinematicTransform(date);
        // THEN
        Assertions.assertEquals(expectedTransform, actualTransform);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetReversedProviderFieldGetKinematicTransform() {
        // GIVEN
        final FieldAbsoluteDate<Complex> date = new FieldAbsoluteDate<>(ComplexField.getInstance(),
                AbsoluteDate.ARBITRARY_EPOCH);
        final TransformProvider mockedProvider = Mockito.mock(TransformProvider.class);
        final TransformProvider reversedProvider = TransformProviderUtils.getReversedProvider(mockedProvider);
        final FieldKinematicTransform<Complex> expectedTransform = Mockito.mock(FieldKinematicTransform.class);
        final FieldKinematicTransform<Complex> mockedTransform = Mockito.mock(FieldKinematicTransform.class);
        Mockito.when(mockedTransform.getInverse()).thenReturn(expectedTransform);
        Mockito.when(mockedProvider.getKinematicTransform(date)).thenReturn(mockedTransform);
        // WHEN
        final FieldKinematicTransform<Complex> actualTransform = reversedProvider.getKinematicTransform(date);
        // THEN
        Assertions.assertEquals(expectedTransform, actualTransform);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetReversedProviderFieldGetStaticTransform() {
        // GIVEN
        final FieldAbsoluteDate<Complex> date = new FieldAbsoluteDate<>(ComplexField.getInstance(),
                AbsoluteDate.ARBITRARY_EPOCH);
        final TransformProvider mockedProvider = Mockito.mock(TransformProvider.class);
        final TransformProvider reversedProvider = TransformProviderUtils.getReversedProvider(mockedProvider);
        final FieldStaticTransform<Complex> expectedTransform = Mockito.mock(FieldStaticTransform.class);
        final FieldStaticTransform<Complex> mockedTransform = Mockito.mock(FieldStaticTransform.class);
        Mockito.when(mockedTransform.getInverse()).thenReturn(expectedTransform);
        Mockito.when(mockedProvider.getStaticTransform(date)).thenReturn(mockedTransform);
        // WHEN
        final FieldStaticTransform<Complex> actualTransform = reversedProvider.getStaticTransform(date);
        // THEN
        Assertions.assertEquals(expectedTransform, actualTransform);
    }

    @Test
    public void testIdentity() {
        RandomGenerator random = new Well19937a(0x87c3a5c51fb0235el);
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        checkNoTransform(TransformProviderUtils.IDENTITY_PROVIDER.getTransform(date), random);
    }

    @Test
    public void testIdentityField() {
        RandomGenerator random = new Well19937a(0x7086a8e4ad1265b0l);
        final FieldAbsoluteDate<Binary64> date = new FieldAbsoluteDate<>(Binary64Field.getInstance(),
                                                                          AbsoluteDate.J2000_EPOCH);
        checkNoTransform(TransformProviderUtils.IDENTITY_PROVIDER.getTransform(date), random);
    }

    @Test
    public void testReverse() {
        RandomGenerator random = new Well19937a(0xba49d4909717ec6cl);
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        for (int i = 0; i < 20; ++i) {
            TransformProvider tp = constantProvider(random);
            TransformProvider reversed = TransformProviderUtils.getReversedProvider(tp);
            checkNoTransform(new Transform(date, tp.getTransform(date), reversed.getTransform(date)), random);
            checkNoTransform(new Transform(date, reversed.getTransform(date), tp.getTransform(date)), random);
        }
    }

    @Test
    public void testReverseField() {
        RandomGenerator random = new Well19937a(0xd74443b3079403e7l);
        final FieldAbsoluteDate<Binary64> date = new FieldAbsoluteDate<>(Binary64Field.getInstance(),
                                                                          AbsoluteDate.J2000_EPOCH);
        for (int i = 0; i < 20; ++i) {
            TransformProvider tp = constantProvider(random);
            TransformProvider reversed = TransformProviderUtils.getReversedProvider(tp);
            checkNoTransform(new FieldTransform<>(date, tp.getTransform(date), reversed.getTransform(date)), random);
            checkNoTransform(new FieldTransform<>(date, reversed.getTransform(date), tp.getTransform(date)), random);
        }
    }

    @Test
    public void testCombine() {
        RandomGenerator random = new Well19937a(0x6e3b2c793680e7e3l);
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        for (int i = 0; i < 20; ++i) {
            TransformProvider first  = constantProvider(random);
            TransformProvider second = constantProvider(random);
            TransformProvider combined = TransformProviderUtils.getCombinedProvider(first, second);
            checkNoTransform(new Transform(date,
                                           new Transform(date, first.getTransform(date), second.getTransform(date)).getInverse(),
                                           combined.getTransform(date)),
                             random);
        }
    }

    @Test
    public void testCombineField() {
        RandomGenerator random = new Well19937a(0x1f8bf20bfa4b54eal);
        final FieldAbsoluteDate<Binary64> date = new FieldAbsoluteDate<>(Binary64Field.getInstance(),
                                                                          AbsoluteDate.J2000_EPOCH);
        for (int i = 0; i < 20; ++i) {
            TransformProvider first  = constantProvider(random);
            TransformProvider second = constantProvider(random);
            TransformProvider combined = TransformProviderUtils.getCombinedProvider(first, second);
            checkNoTransform(new FieldTransform<>(date,
                                                  new FieldTransform<>(date, first.getTransform(date), second.getTransform(date)).getInverse(),
                                                  combined.getTransform(date)),
                             random);
        }
    }

    private TransformProvider constantProvider(RandomGenerator random) {
        final Transform combined = randomTransform(random);
        return new TransformProvider() {
            private static final long serialVersionUID = 20180330L;
            public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(FieldAbsoluteDate<T> date)
                {
                return new FieldTransform<>(date.getField(), combined);
            }
            public Transform getTransform(AbsoluteDate date)
                {
                return combined;
            }
        };
    }

    private Transform randomTransform(RandomGenerator random) {
        // generate a random transform
        Transform combined = Transform.IDENTITY;
        for (int k = 0; k < 20; ++k) {
            Transform t = random.nextBoolean() ?
                          new Transform(AbsoluteDate.J2000_EPOCH, randomVector(1.0e3, random), randomVector(1.0, random), randomVector(1.0e-3, random)) :
                          new Transform(AbsoluteDate.J2000_EPOCH, randomRotation(random), randomVector(0.01, random), randomVector(1.0e-4, random));
            combined = new Transform(AbsoluteDate.J2000_EPOCH, combined, t);
        }
        return combined;
    }

    private Vector3D randomVector(double scale, RandomGenerator random) {
        return new Vector3D(random.nextDouble() * scale,
                            random.nextDouble() * scale,
                            random.nextDouble() * scale);
    }

    private Rotation randomRotation(RandomGenerator random) {
        double q0 = random.nextDouble() * 2 - 1;
        double q1 = random.nextDouble() * 2 - 1;
        double q2 = random.nextDouble() * 2 - 1;
        double q3 = random.nextDouble() * 2 - 1;
        double q  = FastMath.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        return new Rotation(q0 / q, q1 / q, q2 / q, q3 / q, false);
    }

    private void checkNoTransform(FieldTransform<Binary64> transform, RandomGenerator random) {
        for (int i = 0; i < 100; ++i) {
            Vector3D a = randomVector(1.0e3, random);
            Vector3D tA = transform.transformVector(a).toVector3D();
            Assertions.assertEquals(0, a.subtract(tA).getNorm(), 1.0e-10 * a.getNorm());
            Vector3D b = randomVector(1.0e3, random);
            Vector3D tB = transform.transformPosition(b).toVector3D();
            Assertions.assertEquals(0, b.subtract(tB).getNorm(), 1.0e-10 * b.getNorm());
            PVCoordinates pv  = new PVCoordinates(randomVector(1.0e3, random), randomVector(1.0, random), randomVector(1.0e-3, random));
            PVCoordinates tPv = transform.transformPVCoordinates(pv).toPVCoordinates();
            checkVector(pv.getPosition(),     tPv.getPosition(), 1.0e-10);
            checkVector(pv.getVelocity(),     tPv.getVelocity(), 3.0e-9);
            checkVector(pv.getAcceleration(), tPv.getAcceleration(), 3.0e-9);
        }
    }

    private void checkNoTransform(Transform transform, RandomGenerator random) {
        for (int i = 0; i < 100; ++i) {
            Vector3D a = randomVector(1.0e3, random);
            Vector3D tA = transform.transformVector(a);
            Assertions.assertEquals(0, a.subtract(tA).getNorm(), 1.0e-10 * a.getNorm());
            Vector3D b = randomVector(1.0e3, random);
            Vector3D tB = transform.transformPosition(b);
            Assertions.assertEquals(0, b.subtract(tB).getNorm(), 1.0e-10 * b.getNorm());
            PVCoordinates pv  = new PVCoordinates(randomVector(1.0e3, random), randomVector(1.0, random), randomVector(1.0e-3, random));
            PVCoordinates tPv = transform.transformPVCoordinates(pv);
            checkVector(pv.getPosition(),     tPv.getPosition(), 1.0e-10);
            checkVector(pv.getVelocity(),     tPv.getVelocity(), 3.0e-9);
            checkVector(pv.getAcceleration(), tPv.getAcceleration(), 3.0e-9);
        }
    }

    private void checkVector(Vector3D reference, Vector3D result, double relativeTolerance) {
        double refNorm = reference.getNorm();
        double resNorm = result.getNorm();
        double tolerance = relativeTolerance * (1 + FastMath.max(refNorm, resNorm));
        Assertions.assertEquals(0, Vector3D.distance(reference, result), tolerance,"ref = " + reference + ", res = " + result + " -> " +
                (Vector3D.distance(reference, result) / (1 + FastMath.max(refNorm, resNorm))));
    }

}
