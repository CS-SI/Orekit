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
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.PVCoordinates;

public class TransformProviderUtilTest {

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
