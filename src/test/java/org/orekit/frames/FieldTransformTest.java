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
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldLine;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeInterpolator;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinatesHermiteInterpolator;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class FieldTransformTest {

    @Test
    public void testIdentityTranslation() {
        doTestIdentityTranslation(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIdentityTranslation(Field<T> field) {
        checkNoTransform(FieldTransform.getIdentity(field).shiftedBy(12345.0),
                         new Well19937a(0xfd118eac6b5ec136l));
    }

    @Test
    public void testIdentityRotation() {
        doTestIdentityRotation(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIdentityRotation(Field<T> field) {
        checkNoTransform(FieldTransform.getIdentity(field),
                         new Well19937a(0xfd118eac6b5ec136l));
    }

    @Test
    public void testIdentityLine() {
        doTestIdentityLine(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIdentityLine(Field<T> field) {
        RandomGenerator random = new Well19937a(0x98603025df70db7cl);
        FieldVector3D<T> p1 = randomVector(field, 100.0, random);
        FieldVector3D<T> p2 = randomVector(field, 100.0, random);
        FieldLine<T> line = new FieldLine<>(p1, p2, 1.0e-6);
        FieldLine<T> transformed = FieldTransform.getIdentity(field).transformLine(line);
        Assertions.assertSame(line, transformed);
    }

    @Test
    public void testSimpleComposition() {
        doTestSimpleComposition(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestSimpleComposition(Field<T> field) {
        FieldTransform<T> transform =
            new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field),
                                 new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field),
                                                      new FieldRotation<>(FieldVector3D.getPlusK(field),
                                                                          field.getZero().add(0.5 * FastMath.PI),
                                                                          RotationConvention.VECTOR_OPERATOR)),
                                 new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), FieldVector3D.getPlusI(field)));
        FieldVector3D<T> u = transform.transformPosition(createVector(field, 1.0, 1.0, 1.0));
        FieldVector3D<T> v = createVector(field, 0.0, 1.0, 1.0);
        Assertions.assertEquals(0, u.subtract(v).getNorm().getReal(), 1.0e-15);
    }

    @Test
    public void testAcceleration() {
        doTestAcceleration(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestAcceleration(Field<T> field) {

        FieldPVCoordinates<T> initPV = new FieldPVCoordinates<>(createVector(field, 9, 8, 7),
                                                                createVector(field, 6, 5, 4),
                                                                createVector(field, 3, 2, 1));
        for (double dt = 0; dt < 1; dt += 0.01) {
            FieldPVCoordinates<T> basePV        = initPV.shiftedBy(dt);
            FieldPVCoordinates<T> transformedPV = evolvingTransform(FieldAbsoluteDate.getJ2000Epoch(field), dt).transformPVCoordinates(basePV);

            // rebuild transformed acceleration, relying only on transformed position and velocity
            List<TimeStampedFieldPVCoordinates<T>> sample = new ArrayList<TimeStampedFieldPVCoordinates<T>>();
            double h = 1.0e-2;
            for (int i = -3; i < 4; ++i) {
                FieldTransform<T> t = evolvingTransform(FieldAbsoluteDate.getJ2000Epoch(field), dt + i * h);
                FieldPVCoordinates<T> pv = t.transformPVCoordinates(initPV.shiftedBy(dt + i * h));
                sample.add(new TimeStampedFieldPVCoordinates<>(t.getDate(), pv.getPosition(), pv.getVelocity(), FieldVector3D.getZero(field)));
            }

            // create interpolator
            final FieldTimeInterpolator<TimeStampedFieldPVCoordinates<T>, T> interpolator =
                    new TimeStampedFieldPVCoordinatesHermiteInterpolator<>(sample.size(), CartesianDerivativesFilter.USE_PV);

            FieldPVCoordinates<T> rebuiltPV = interpolator.interpolate(FieldAbsoluteDate.getJ2000Epoch(field).shiftedBy(dt),
                                                                       sample);

            checkVector(rebuiltPV.getPosition(),     transformedPV.getPosition(),     3.0e-15);
            checkVector(rebuiltPV.getVelocity(),     transformedPV.getVelocity(),     2.0e-15);
            checkVector(rebuiltPV.getAcceleration(), transformedPV.getAcceleration(), 5.0e-10);

        }

    }

    @Test
    public void testAccelerationComposition() {
        doTestAccelerationComposition(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestAccelerationComposition(Field<T> field) {
        RandomGenerator random = new Well19937a(0x41fdd07d6c9e9f65l);

        FieldVector3D<T>  p1 = randomVector(field, 1.0e3,  random);
        FieldVector3D<T>  v1 = randomVector(field, 1.0,    random);
        FieldVector3D<T>  a1 = randomVector(field, 1.0e-3, random);
        FieldRotation<T>  r1 = randomRotation(field, random);
        FieldVector3D<T>  o1 = randomVector(field, 0.1, random);

        FieldVector3D<T>  p2 = randomVector(field, 1.0e3,  random);
        FieldVector3D<T>  v2 = randomVector(field, 1.0,    random);
        FieldVector3D<T>  a2 = randomVector(field, 1.0e-3, random);
        FieldRotation<T>  r2 = randomRotation(field, random);
        FieldVector3D<T>  o2 = randomVector(field, 0.1, random);

        FieldTransform<T> t1  = new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field),
                                                     new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), p1, v1, a1),
                                                     new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), r1, o1));
        FieldTransform<T> t2  = new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field),
                                                     new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), p2, v2, a2),
                                                     new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), r2, o2));
        FieldTransform<T> t12 = new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), t1, t2);

        FieldVector3D<T> q       = randomVector(field, 1.0e3,  random);
        FieldVector3D<T> qDot    = randomVector(field, 1.0,    random);
        FieldVector3D<T> qDotDot = randomVector(field, 1.0e-3, random);

        FieldPVCoordinates<T> pva0 = new FieldPVCoordinates<>(q, qDot, qDotDot);
        FieldPVCoordinates<T> pva1 = t1.transformPVCoordinates(pva0);
        FieldPVCoordinates<T> pva2 = t2.transformPVCoordinates(pva1);
        FieldPVCoordinates<T> pvac = t12.transformPVCoordinates(pva0);

        checkVector(pva2.getPosition(),     pvac.getPosition(),     1.0e-15);
        checkVector(pva2.getVelocity(),     pvac.getVelocity(),     1.0e-15);
        checkVector(pva2.getAcceleration(), pvac.getAcceleration(), 1.0e-15);

        // despite neither raw transforms have angular acceleration,
        // the combination does have an angular acceleration,
        // it is due to the cross product Ω₁ ⨯ Ω₂
        Assertions.assertEquals(0.0, t1.getAngular().getRotationAcceleration().getNorm().getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, t2.getAngular().getRotationAcceleration().getNorm().getReal(), 1.0e-15);
        Assertions.assertTrue(t12.getAngular().getRotationAcceleration().getNorm().getReal() > 0.01);

        Assertions.assertEquals(0.0, t12.freeze().getCartesian().getVelocity().getNorm().getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, t12.freeze().getCartesian().getAcceleration().getNorm().getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, t12.freeze().getAngular().getRotationRate().getNorm().getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, t12.freeze().getAngular().getRotationAcceleration().getNorm().getReal(), 1.0e-15);
    }

    @Test
    public void testRandomComposition() {
        doTestRandomComposition(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestRandomComposition(Field<T> field) {

        RandomGenerator random = new Well19937a(0x171c79e323a1123l);
        for (int i = 0; i < 20; ++i) {

            // build a complex transform by composing primitive ones
            int n = random.nextInt(20);
            @SuppressWarnings("unchecked")
            FieldTransform<T>[] transforms = (FieldTransform<T>[]) Array.newInstance(FieldTransform.class, n);
            FieldTransform<T> combined = FieldTransform.getIdentity(field);
            for (int k = 0; k < n; ++k) {
                transforms[k] = random.nextBoolean()
                ? new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), randomVector(field, 1.0e3, random), randomVector(field, 1.0, random), randomVector(field, 1.0e-3, random))
                : new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), randomRotation(field, random), randomVector(field, 0.01, random), randomVector(field, 1.0e-4, random));
                combined = new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), combined, transforms[k]);
            }

            // check the composition
            for (int j = 0; j < 10; ++j) {
                FieldVector3D<T> a = randomVector(field, 1.0, random);
                FieldVector3D<T> b = randomVector(field, 1.0e3, random);
                FieldPVCoordinates<T> c = new FieldPVCoordinates<>(randomVector(field, 1.0e3, random), randomVector(field, 1.0, random), randomVector(field, 1.0e-3, random));
                FieldVector3D<T>                 aRef  = a;
                FieldVector3D<T>                 bRef  = b;
                FieldPVCoordinates<T>            cRef  = c;
                for (int k = 0; k < n; ++k) {
                    aRef  = transforms[k].transformVector(aRef);
                    bRef  = transforms[k].transformPosition(bRef);
                    cRef  = transforms[k].transformPVCoordinates(cRef);
                }

                FieldVector3D<T> aCombined = combined.transformVector(a);
                FieldVector3D<T> bCombined = combined.transformPosition(b);
                FieldPVCoordinates<T> cCombined = combined.transformPVCoordinates(c);
                checkVector(aRef, aCombined, 3.0e-15);
                checkVector(bRef, bCombined, 5.0e-15);
                checkVector(cRef.getPosition(),     cCombined.getPosition(),     1.0e-14);
                checkVector(cRef.getVelocity(),     cCombined.getVelocity(),     1.0e-14);
                checkVector(cRef.getAcceleration(), cCombined.getAcceleration(), 1.0e-14);

            }
        }

    }

    @Test
    public void testReverse() {
        doTestReverse(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestReverse(Field<T> field) {
        RandomGenerator random = new Well19937a(0x9f82ba2b2c98dac5l);
        for (int i = 0; i < 20; ++i) {
            FieldTransform<T> combined = randomTransform(field, random);

            checkNoTransform(new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), combined, combined.getInverse()), random);

        }

    }

    @Test
    public void testIdentityJacobianP() {
        doTestIdentityJacobianP(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIdentityJacobianP(Field<T> field) {
        doTestIdentityJacobian(field, 3, CartesianDerivativesFilter.USE_P);
    }

    @Test
    public void testIdentityJacobianPV() {
        doTestIdentityJacobianPV(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIdentityJacobianPV(Field<T> field) {
        doTestIdentityJacobian(field, 6, CartesianDerivativesFilter.USE_PV);
    }

    @Test
    public void testIdentityJacobianPVA() {
        doTestIdentityJacobianPVA(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIdentityJacobianPVA(Field<T> field) {
        doTestIdentityJacobian(field, 9, CartesianDerivativesFilter.USE_PVA);
    }

    private <T extends CalculusFieldElement<T>> void doTestIdentityJacobian(Field<T> field, int n, CartesianDerivativesFilter filter) {
        T[][] jacobian = MathArrays.buildArray(field, n, n);
        FieldTransform.getIdentity(field).getJacobian(filter, jacobian);
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                Assertions.assertEquals(i == j ? 1.0 : 0.0, jacobian[i][j].getReal(), 1.0e-15);
            }
        }
    }

    @Test
    public void testDecomposeAndRebuild() {
        doTestDecomposeAndRebuild(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestDecomposeAndRebuild(Field<T> field) {
        RandomGenerator random = new Well19937a(0xb8ee9da1b05198c9l);
        for (int i = 0; i < 20; ++i) {
            FieldTransform<T> combined = randomTransform(field, random);
            FieldTransform<T> rebuilt  = new FieldTransform<>(combined.getFieldDate(),
                                                              new FieldTransform<>(combined.getFieldDate(), combined.getTranslation(),
                                                                                   combined.getVelocity(), combined.getAcceleration()),
                                                              new FieldTransform<>(combined.getFieldDate(), combined.getRotation(),
                                                                                   combined.getRotationRate(), combined.getRotationAcceleration()));

            checkNoTransform(new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), combined, rebuilt.getInverse()), random);

        }

    }

    @Test
    public void testTranslation() {
        doTestTranslation(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestTranslation(Field<T> field) {
        RandomGenerator rnd = new Well19937a(0x7e9d737ba4147787l);
        for (int i = 0; i < 10; ++i) {
            FieldVector3D<T> delta = randomVector(field, 1.0e3, rnd);
            FieldTransform<T> transform = new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), delta);
            for (int j = 0; j < 10; ++j) {
                FieldVector3D<T> a = createVector(field, rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble());
                FieldVector3D<T> b = transform.transformVector(a);
                Assertions.assertEquals(0, b.subtract(a).getNorm().getReal(), 1.0e-15);
                FieldVector3D<T> c = transform.transformPosition(a);
                Assertions.assertEquals(0,
                                    c.subtract(a).subtract(delta).getNorm().getReal(),
                                    1.0e-14);
            }
        }
    }

    @Test
    public void testTranslationDouble() {
        doTestTranslationDouble(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestTranslationDouble(Field<T> field) {
        RandomGenerator rnd = new Well19937a(0x7e9d737ba4147787l);
        for (int i = 0; i < 10; ++i) {
            FieldVector3D<T> delta = randomVector(field, 1.0e3, rnd);
            FieldTransform<T> transform = new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), delta);
            for (int j = 0; j < 10; ++j) {
                Vector3D a = createVector(field, rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble()).toVector3D();
                FieldVector3D<T> b = transform.transformVector(a);
                Assertions.assertEquals(0, b.subtract(a).getNorm().getReal(), 1.0e-15);
                FieldVector3D<T> c = transform.transformPosition(a);
                Assertions.assertEquals(0,
                                    c.subtract(a).subtract(delta).getNorm().getReal(),
                                    1.0e-14);
            }
        }
    }

    @Test
    public void testRoughTransPV() {
        doTestRoughTransPV(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestRoughTransPV(Field<T> field) {

        FieldPVCoordinates<T> pointP1 = new FieldPVCoordinates<>(FieldVector3D.getPlusI(field), FieldVector3D.getPlusI(field), FieldVector3D.getPlusI(field));

        // translation transform test
        FieldPVCoordinates<T> pointP2 = new FieldPVCoordinates<>(createVector(field, 0, 0, 0), createVector(field, 0, 0, 0));
        FieldTransform<T> R1toR2 = new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), FieldVector3D.getMinusI(field), FieldVector3D.getMinusI(field), FieldVector3D.getMinusI(field));
        FieldPVCoordinates<T> result1 = R1toR2.transformPVCoordinates(pointP1);
        checkVector(pointP2.getPosition(),     result1.getPosition(),     1.0e-15);
        checkVector(pointP2.getVelocity(),     result1.getVelocity(),     1.0e-15);
        checkVector(pointP2.getAcceleration(), result1.getAcceleration(), 1.0e-15);

        // test inverse translation
        FieldTransform<T> R2toR1 = R1toR2.getInverse();
        FieldPVCoordinates<T> invResult1 = R2toR1.transformPVCoordinates(pointP2);
        checkVector(pointP1.getPosition(),     invResult1.getPosition(),     1.0e-15);
        checkVector(pointP1.getVelocity(),     invResult1.getVelocity(),     1.0e-15);
        checkVector(pointP1.getAcceleration(), invResult1.getAcceleration(), 1.0e-15);

        // rotation transform test
        FieldPVCoordinates<T> pointP3 = new FieldPVCoordinates<>(FieldVector3D.getPlusJ(field), createVector(field, -2, 1, 0), createVector(field, -4, -3, -1));
        FieldRotation<T> R = new FieldRotation<>(FieldVector3D.getPlusK(field), field.getZero().add(FastMath.PI / 2), RotationConvention.VECTOR_OPERATOR);
        FieldTransform<T> R1toR3 = new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), R, createVector(field, 0, 0, -2), createVector(field, 1, 0, 0));
        FieldPVCoordinates<T> result2 = R1toR3.transformPVCoordinates(pointP1);
        checkVector(pointP3.getPosition(),     result2.getPosition(),     1.0e-15);
        checkVector(pointP3.getVelocity(),     result2.getVelocity(),     1.0e-15);
        checkVector(pointP3.getAcceleration(), result2.getAcceleration(), 1.0e-15);

        // test inverse rotation
        FieldTransform<T> R3toR1 = R1toR3.getInverse();
        FieldPVCoordinates<T> invResult2 = R3toR1.transformPVCoordinates(pointP3);
        checkVector(pointP1.getPosition(),     invResult2.getPosition(),     1.0e-15);
        checkVector(pointP1.getVelocity(),     invResult2.getVelocity(),     1.0e-15);
        checkVector(pointP1.getAcceleration(), invResult2.getAcceleration(), 1.0e-15);

        // combine 2 velocity transform
        FieldTransform<T> R1toR4 = new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), createVector(field, -2, 0, 0), createVector(field, -2, 0, 0), createVector(field, -2, 0, 0));
        FieldPVCoordinates<T> pointP4 = new FieldPVCoordinates<>(createVector(field, -1, 0, 0), createVector(field, -1, 0, 0), createVector(field, -1, 0, 0));
        FieldTransform<T> R2toR4 = new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), R2toR1, R1toR4);
        FieldPVCoordinates<T> compResult = R2toR4.transformPVCoordinates(pointP2);
        checkVector(pointP4.getPosition(),     compResult.getPosition(),     1.0e-15);
        checkVector(pointP4.getVelocity(),     compResult.getVelocity(),     1.0e-15);
        checkVector(pointP4.getAcceleration(), compResult.getAcceleration(), 1.0e-15);

        // combine 2 rotation tranform
        FieldPVCoordinates<T> pointP5 = new FieldPVCoordinates<>(createVector(field, -1, 0, 0), createVector(field, -1, 0, 3), createVector(field, 8, 0, 6));
        FieldRotation<T> R2 = new FieldRotation<>(createVector(field, 0, 0, 1), field.getZero().add(FastMath.PI), RotationConvention.VECTOR_OPERATOR);
        FieldTransform<T> R1toR5 = new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), R2, createVector(field, 0, -3, 0));
        FieldTransform<T> R3toR5 = new FieldTransform<> (FieldAbsoluteDate.getJ2000Epoch(field), R3toR1, R1toR5);
        FieldPVCoordinates<T> combResult = R3toR5.transformPVCoordinates(pointP3);
        checkVector(pointP5.getPosition(),     combResult.getPosition(),     1.0e-15);
        checkVector(pointP5.getVelocity(),     combResult.getVelocity(),     1.0e-15);
        checkVector(pointP5.getAcceleration(), combResult.getAcceleration(), 1.0e-15);

        // combine translation and rotation
        FieldTransform<T> R2toR3 = new FieldTransform<> (FieldAbsoluteDate.getJ2000Epoch(field), R2toR1, R1toR3);
        FieldPVCoordinates<T> result = R2toR3.transformPVCoordinates(pointP2);
        checkVector(pointP3.getPosition(),     result.getPosition(),     1.0e-15);
        checkVector(pointP3.getVelocity(),     result.getVelocity(),     1.0e-15);
        checkVector(pointP3.getAcceleration(), result.getAcceleration(), 1.0e-15);

        FieldTransform<T> R3toR2 = new FieldTransform<> (FieldAbsoluteDate.getJ2000Epoch(field), R3toR1, R1toR2);
        result = R3toR2.transformPVCoordinates(pointP3);
        checkVector(pointP2.getPosition(),     result.getPosition(),     1.0e-15);
        checkVector(pointP2.getVelocity(),     result.getVelocity(),     1.0e-15);
        checkVector(pointP2.getAcceleration(), result.getAcceleration(), 1.0e-15);

        FieldTransform<T> newR1toR5 = new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), R1toR2, R2toR3);
        newR1toR5 = new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), newR1toR5, R3toR5);
        result = newR1toR5.transformPVCoordinates(pointP1);
        checkVector(pointP5.getPosition(),     result.getPosition(),     1.0e-15);
        checkVector(pointP5.getVelocity(),     result.getVelocity(),     1.0e-15);
        checkVector(pointP5.getAcceleration(), result.getAcceleration(), 1.0e-15);

        // more tests
        newR1toR5 = new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), R1toR2, R2toR3);
        FieldTransform<T> R3toR4 = new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), R3toR1, R1toR4);
        newR1toR5 = new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), newR1toR5, R3toR4);
        FieldTransform<T> R4toR5 = new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), R1toR4.getInverse(), R1toR5);
        newR1toR5 = new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), newR1toR5, R4toR5);
        result = newR1toR5.transformPVCoordinates(pointP1);
        checkVector(pointP5.getPosition(),     result.getPosition(), 1.0e-15);
        checkVector(pointP5.getVelocity(),     result.getVelocity(), 1.0e-15);
        checkVector(pointP5.getAcceleration(), result.getAcceleration(), 1.0e-15);

    }

    @Test
    public void testRotPV() {
        doTestRotPV(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestRotPV(Field<T> field) {

        RandomGenerator rnd = new Well19937a(0x73d5554d99427af0l);

        // Instant Rotation only

        for (int i = 0; i < 10; ++i) {

            // Random instant rotation

            FieldRotation<T> instantRot    = randomRotation(field, rnd);
            FieldVector3D<T> normAxis = instantRot.getAxis(RotationConvention.VECTOR_OPERATOR);
            T w  = instantRot.getAngle().abs().divide(Constants.JULIAN_DAY);

            // random rotation
            FieldRotation<T> rot    = randomRotation(field, rnd);

            // so we have a transform
            FieldTransform<T> tr = new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field),
                                                        rot, new FieldVector3D<>(w, normAxis));

            // random position, velocity, acceleration
            FieldVector3D<T> pos = randomVector(field, 1.0e3, rnd);
            FieldVector3D<T> vel = randomVector(field, 1.0, rnd);
            FieldVector3D<T> acc = randomVector(field, 1.0e-3, rnd);

            FieldPVCoordinates<T> pvOne = new FieldPVCoordinates<>(pos, vel, acc);

            // we obtain

            FieldPVCoordinates<T> pvTwo = tr.transformPVCoordinates(pvOne);

            // test inverse

            FieldVector3D<T> resultvel = tr.getInverse().transformPVCoordinates(pvTwo).getVelocity();

            checkVector(resultvel, vel, 1.0e-15);

        }

    }

    @Test
    public void testTransPV() {
        doTestTransPV(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestTransPV(Field<T> field) {

        RandomGenerator rnd = new Well19937a(0x73d5554d99427af0l);

        // translation velocity only :

        for (int i = 0; i < 10; ++i) {

            // random position, velocity and acceleration
            FieldVector3D<T> pos = randomVector(field, 1.0e3,  rnd);
            FieldVector3D<T> vel = randomVector(field, 1.0,    rnd);
            FieldVector3D<T> acc = randomVector(field, 1.0e-3, rnd);
            FieldPVCoordinates<T> pvOne = new FieldPVCoordinates<>(pos, vel, acc);

            // random transform
            FieldVector3D<T> transPos = randomVector(field, 1.0e3,  rnd);
            FieldVector3D<T> transVel = randomVector(field, 1.0,    rnd);
            FieldVector3D<T> transAcc = randomVector(field, 1.0e-3, rnd);
            FieldTransform<T> tr = new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), transPos, transVel, transAcc);

            double dt = 1;

            // we should obtain
            FieldVector3D<T> good = tr.transformPosition(pos.add(new FieldVector3D<>(dt, vel))).add(new FieldVector3D<>(dt, transVel));

            // we have
            FieldPVCoordinates<T> pvTwo = tr.transformPVCoordinates(pvOne);
            FieldVector3D<T> result  = pvTwo.getPosition().add(new FieldVector3D<>(dt, pvTwo.getVelocity()));
            checkVector(good, result, 1.0e-15);

            // test inverse
            FieldVector3D<T> resultvel = tr.getInverse().
            transformPVCoordinates(pvTwo).getVelocity();
            checkVector(resultvel, vel, 1.0e-15);

        }

    }

    @Test
    public void testTransPVDouble() {
        doTestTransPVDouble(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestTransPVDouble(Field<T> field) {

        RandomGenerator rnd = new Well19937a(0x73d5554d99427af0l);

        // translation velocity only :

        for (int i = 0; i < 10; ++i) {

            // random position, velocity and acceleration
            Vector3D pos = randomVector(field, 1.0e3,  rnd).toVector3D();
            Vector3D vel = randomVector(field, 1.0,    rnd).toVector3D();
            Vector3D acc = randomVector(field, 1.0e-3, rnd).toVector3D();
            PVCoordinates pvOne = new PVCoordinates(pos, vel, acc);

            // random transform
            FieldVector3D<T> transPos = randomVector(field, 1.0e3,  rnd);
            FieldVector3D<T> transVel = randomVector(field, 1.0,    rnd);
            FieldVector3D<T> transAcc = randomVector(field, 1.0e-3, rnd);
            FieldTransform<T> tr = new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), transPos, transVel, transAcc);

            T dt = field.getZero().add(1);

            // we should obtain
            FieldVector3D<T> good = tr.transformPosition(new FieldVector3D<>(dt, vel).add(pos)).add(new FieldVector3D<>(dt, transVel));

            // we have
            FieldPVCoordinates<T> pvTwo = tr.transformPVCoordinates(pvOne);
            FieldVector3D<T> result  = pvTwo.getPosition().add(new FieldVector3D<>(dt, pvTwo.getVelocity()));
            checkVector(good, result, 1.0e-15);

            // test inverse
            FieldVector3D<T> resultvel = tr.getInverse().
            transformPVCoordinates(pvTwo).getVelocity();
            checkVector(resultvel, new FieldVector3D<>(field, vel), 1.0e-15);

        }

    }

    @Test
    public void testRotation() {
        doTestRotation(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestRotation(Field<T> field) {
        RandomGenerator rnd = new Well19937a(0x73d5554d99427af0l);
        for (int i = 0; i < 10; ++i) {

            FieldRotation<T> r    = randomRotation(field, rnd);
            FieldVector3D<T> axis = r.getAxis(RotationConvention.VECTOR_OPERATOR);
            T angle  = r.getAngle();

            FieldTransform<T> transform = new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), r);
            for (int j = 0; j < 10; ++j) {
                FieldVector3D<T> a = createVector(field, rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble());
                FieldVector3D<T> b = transform.transformVector(a);
                Assertions.assertEquals(FieldVector3D.angle(axis, a).getReal(), FieldVector3D.angle(axis, b).getReal(), 1.0e-14);
                FieldVector3D<T> aOrtho = FieldVector3D.crossProduct(axis, a);
                FieldVector3D<T> bOrtho = FieldVector3D.crossProduct(axis, b);
                Assertions.assertEquals(angle.getReal(), FieldVector3D.angle(aOrtho, bOrtho).getReal(), 1.0e-14);
                FieldVector3D<T> c = transform.transformPosition(a);
                Assertions.assertEquals(0, c.subtract(b).getNorm().getReal(), 1.0e-14);
            }

        }
    }

    @Test
    public void testJacobianP() {
        doTestJacobianP(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestJacobianP(Field<T> field) {

        // base directions for finite differences
        @SuppressWarnings("unchecked")
        FieldPVCoordinates<T>[] directions = (FieldPVCoordinates<T>[]) Array.newInstance(FieldPVCoordinates.class, 3);
        directions[0] = new FieldPVCoordinates<>(FieldVector3D.getPlusI(field), FieldVector3D.getZero(field), FieldVector3D.getZero(field));
        directions[1] = new FieldPVCoordinates<>(FieldVector3D.getPlusJ(field), FieldVector3D.getZero(field), FieldVector3D.getZero(field));
        directions[2] = new FieldPVCoordinates<>(FieldVector3D.getPlusK(field), FieldVector3D.getZero(field), FieldVector3D.getZero(field));
        double h = 0.01;

        RandomGenerator random = new Well19937a(0x47fd0d6809f4b173l);
        for (int i = 0; i < 20; ++i) {

            // generate a random transform
            FieldTransform<T> combined = randomTransform(field, random);

            // compute Jacobian
            T[][] jacobian = MathArrays.buildArray(field, 9, 9);
            for (int l = 0; l < jacobian.length; ++l) {
                for (int c = 0; c < jacobian[l].length; ++c) {
                    jacobian[l][c] = field.getZero().add(l + 0.1 * c);
                }
            }
            combined.getJacobian(CartesianDerivativesFilter.USE_P, jacobian);

            for (int j = 0; j < 100; ++j) {

                FieldPVCoordinates<T> pv0 = new FieldPVCoordinates<>(randomVector(field, 1e3, random),
                                                                     randomVector(field, 1.0, random),
                                                                     randomVector(field, 1.0e-3, random));
                double epsilonP = 2.0e-12 * pv0.getPosition().getNorm().getReal();

                for (int c = 0; c < directions.length; ++c) {

                    // eight points finite differences estimation of a Jacobian column
                    FieldPVCoordinates<T> pvm4h = combined.transformPVCoordinates(new FieldPVCoordinates<>(1.0, pv0, -4 * h, directions[c]));
                    FieldPVCoordinates<T> pvm3h = combined.transformPVCoordinates(new FieldPVCoordinates<>(1.0, pv0, -3 * h, directions[c]));
                    FieldPVCoordinates<T> pvm2h = combined.transformPVCoordinates(new FieldPVCoordinates<>(1.0, pv0, -2 * h, directions[c]));
                    FieldPVCoordinates<T> pvm1h = combined.transformPVCoordinates(new FieldPVCoordinates<>(1.0, pv0, -1 * h, directions[c]));
                    FieldPVCoordinates<T> pvp1h = combined.transformPVCoordinates(new FieldPVCoordinates<>(1.0, pv0, +1 * h, directions[c]));
                    FieldPVCoordinates<T> pvp2h = combined.transformPVCoordinates(new FieldPVCoordinates<>(1.0, pv0, +2 * h, directions[c]));
                    FieldPVCoordinates<T> pvp3h = combined.transformPVCoordinates(new FieldPVCoordinates<>(1.0, pv0, +3 * h, directions[c]));
                    FieldPVCoordinates<T> pvp4h = combined.transformPVCoordinates(new FieldPVCoordinates<>(1.0, pv0, +4 * h, directions[c]));
                    FieldPVCoordinates<T> d4   = new FieldPVCoordinates<>(pvm4h, pvp4h);
                    FieldPVCoordinates<T> d3   = new FieldPVCoordinates<>(pvm3h, pvp3h);
                    FieldPVCoordinates<T> d2   = new FieldPVCoordinates<>(pvm2h, pvp2h);
                    FieldPVCoordinates<T> d1   = new FieldPVCoordinates<>(pvm1h, pvp1h);
                    double d = 1.0 / (840 * h);
                    FieldPVCoordinates<T> estimatedColumn = new FieldPVCoordinates<>(-3 * d, d4, 32 * d, d3, -168 * d, d2, 672 * d, d1);

                    // check analytical Jacobian against finite difference reference
                    Assertions.assertEquals(estimatedColumn.getPosition().getX().getReal(), jacobian[0][c].getReal(), epsilonP);
                    Assertions.assertEquals(estimatedColumn.getPosition().getY().getReal(), jacobian[1][c].getReal(), epsilonP);
                    Assertions.assertEquals(estimatedColumn.getPosition().getZ().getReal(), jacobian[2][c].getReal(), epsilonP);

                    // check the rest of the matrix remains untouched
                    for (int l = 3; l < jacobian.length; ++l) {
                        Assertions.assertEquals(l + 0.1 * c, jacobian[l][c].getReal(), 1.0e-15);
                    }

                }

                // check the rest of the matrix remains untouched
                for (int c = directions.length; c < jacobian[0].length; ++c) {
                    for (int l = 0; l < jacobian.length; ++l) {
                        Assertions.assertEquals(l + 0.1 * c, jacobian[l][c].getReal(), 1.0e-15);
                    }
                }

            }
        }

    }

    @Test
    public void testJacobianPV() {
        doTestJacobianPV(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestJacobianPV(Field<T> field) {

        // base directions for finite differences
        @SuppressWarnings("unchecked")
        FieldPVCoordinates<T>[] directions = (FieldPVCoordinates<T>[]) Array.newInstance(FieldPVCoordinates.class, 6);
        directions[0] = new FieldPVCoordinates<>(FieldVector3D.getPlusI(field), FieldVector3D.getZero(field),  FieldVector3D.getZero(field));
        directions[1] = new FieldPVCoordinates<>(FieldVector3D.getPlusJ(field), FieldVector3D.getZero(field),  FieldVector3D.getZero(field));
        directions[2] = new FieldPVCoordinates<>(FieldVector3D.getPlusK(field), FieldVector3D.getZero(field),  FieldVector3D.getZero(field));
        directions[3] = new FieldPVCoordinates<>(FieldVector3D.getZero(field),  FieldVector3D.getPlusI(field), FieldVector3D.getZero(field));
        directions[4] = new FieldPVCoordinates<>(FieldVector3D.getZero(field),  FieldVector3D.getPlusJ(field), FieldVector3D.getZero(field));
        directions[5] = new FieldPVCoordinates<>(FieldVector3D.getZero(field),  FieldVector3D.getPlusK(field), FieldVector3D.getZero(field));
        double h = 0.01;

        RandomGenerator random = new Well19937a(0xce2bfddfbb9796bel);
        for (int i = 0; i < 20; ++i) {

            // generate a random transform
            FieldTransform<T> combined = randomTransform(field, random);

            // compute Jacobian
            T[][] jacobian = MathArrays.buildArray(field, 9, 9);
            for (int l = 0; l < jacobian.length; ++l) {
                for (int c = 0; c < jacobian[l].length; ++c) {
                    jacobian[l][c] = field.getZero().add(l + 0.1 * c);
                }
            }
            combined.getJacobian(CartesianDerivativesFilter.USE_PV, jacobian);

            for (int j = 0; j < 100; ++j) {

                FieldPVCoordinates<T> pv0 = new FieldPVCoordinates<>(randomVector(field, 1e3, random),
                                                                     randomVector(field, 1.0, random),
                                                                     randomVector(field, 1.0e-3, random));
                double epsilonP = 2.0e-12 * pv0.getPosition().getNorm().getReal();
                double epsilonV = 6.0e-11  * pv0.getVelocity().getNorm().getReal();

                for (int c = 0; c < directions.length; ++c) {

                    // eight points finite differences estimation of a Jacobian column
                    FieldPVCoordinates<T> pvm4h = combined.transformPVCoordinates(new FieldPVCoordinates<>(1.0, pv0, -4 * h, directions[c]));
                    FieldPVCoordinates<T> pvm3h = combined.transformPVCoordinates(new FieldPVCoordinates<>(1.0, pv0, -3 * h, directions[c]));
                    FieldPVCoordinates<T> pvm2h = combined.transformPVCoordinates(new FieldPVCoordinates<>(1.0, pv0, -2 * h, directions[c]));
                    FieldPVCoordinates<T> pvm1h = combined.transformPVCoordinates(new FieldPVCoordinates<>(1.0, pv0, -1 * h, directions[c]));
                    FieldPVCoordinates<T> pvp1h = combined.transformPVCoordinates(new FieldPVCoordinates<>(1.0, pv0, +1 * h, directions[c]));
                    FieldPVCoordinates<T> pvp2h = combined.transformPVCoordinates(new FieldPVCoordinates<>(1.0, pv0, +2 * h, directions[c]));
                    FieldPVCoordinates<T> pvp3h = combined.transformPVCoordinates(new FieldPVCoordinates<>(1.0, pv0, +3 * h, directions[c]));
                    FieldPVCoordinates<T> pvp4h = combined.transformPVCoordinates(new FieldPVCoordinates<>(1.0, pv0, +4 * h, directions[c]));
                    FieldPVCoordinates<T> d4   = new FieldPVCoordinates<>(pvm4h, pvp4h);
                    FieldPVCoordinates<T> d3   = new FieldPVCoordinates<>(pvm3h, pvp3h);
                    FieldPVCoordinates<T> d2   = new FieldPVCoordinates<>(pvm2h, pvp2h);
                    FieldPVCoordinates<T> d1   = new FieldPVCoordinates<>(pvm1h, pvp1h);
                    double d = 1.0 / (840 * h);
                    FieldPVCoordinates<T> estimatedColumn = new FieldPVCoordinates<>(-3 * d, d4, 32 * d, d3, -168 * d, d2, 672 * d, d1);

                    // check analytical Jacobian against finite difference reference
                    Assertions.assertEquals(estimatedColumn.getPosition().getX().getReal(), jacobian[0][c].getReal(), epsilonP);
                    Assertions.assertEquals(estimatedColumn.getPosition().getY().getReal(), jacobian[1][c].getReal(), epsilonP);
                    Assertions.assertEquals(estimatedColumn.getPosition().getZ().getReal(), jacobian[2][c].getReal(), epsilonP);
                    Assertions.assertEquals(estimatedColumn.getVelocity().getX().getReal(), jacobian[3][c].getReal(), epsilonV);
                    Assertions.assertEquals(estimatedColumn.getVelocity().getY().getReal(), jacobian[4][c].getReal(), epsilonV);
                    Assertions.assertEquals(estimatedColumn.getVelocity().getZ().getReal(), jacobian[5][c].getReal(), epsilonV);

                    // check the rest of the matrix remains untouched
                    for (int l = 6; l < jacobian.length; ++l) {
                        Assertions.assertEquals(l + 0.1 * c, jacobian[l][c].getReal(), 1.0e-15);
                    }

                }

                // check the rest of the matrix remains untouched
                for (int c = directions.length; c < jacobian[0].length; ++c) {
                    for (int l = 0; l < jacobian.length; ++l) {
                        Assertions.assertEquals(l + 0.1 * c, jacobian[l][c].getReal(), 1.0e-15);
                    }
                }

            }
        }

    }

    @Test
    public void testJacobianPVA() {
        doTestJacobianPVA(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestJacobianPVA(Field<T> field) {

        // base directions for finite differences
        @SuppressWarnings("unchecked")
        FieldPVCoordinates<T>[] directions = (FieldPVCoordinates<T>[]) Array.newInstance(FieldPVCoordinates.class, 9);
        directions[0] = new FieldPVCoordinates<>(FieldVector3D.getPlusI(field), FieldVector3D.getZero(field),  FieldVector3D.getZero(field));
        directions[1] = new FieldPVCoordinates<>(FieldVector3D.getPlusJ(field), FieldVector3D.getZero(field),  FieldVector3D.getZero(field));
        directions[2] = new FieldPVCoordinates<>(FieldVector3D.getPlusK(field), FieldVector3D.getZero(field),  FieldVector3D.getZero(field));
        directions[3] = new FieldPVCoordinates<>(FieldVector3D.getZero(field),  FieldVector3D.getPlusI(field), FieldVector3D.getZero(field));
        directions[4] = new FieldPVCoordinates<>(FieldVector3D.getZero(field),  FieldVector3D.getPlusJ(field), FieldVector3D.getZero(field));
        directions[5] = new FieldPVCoordinates<>(FieldVector3D.getZero(field),  FieldVector3D.getPlusK(field), FieldVector3D.getZero(field));
        directions[6] = new FieldPVCoordinates<>(FieldVector3D.getZero(field),  FieldVector3D.getZero(field),  FieldVector3D.getPlusI(field));
        directions[7] = new FieldPVCoordinates<>(FieldVector3D.getZero(field),  FieldVector3D.getZero(field),  FieldVector3D.getPlusJ(field));
        directions[8] = new FieldPVCoordinates<>(FieldVector3D.getZero(field),  FieldVector3D.getZero(field),  FieldVector3D.getPlusK(field));
        double h = 0.01;

        RandomGenerator random = new Well19937a(0xd223e88b6232198fl);
        for (int i = 0; i < 20; ++i) {

            // generate a random transform
            FieldTransform<T> combined = randomTransform(field, random);

            // compute Jacobian
            T[][] jacobian = MathArrays.buildArray(field, 9, 9);
            for (int l = 0; l < jacobian.length; ++l) {
                for (int c = 0; c < jacobian[l].length; ++c) {
                    jacobian[l][c] = field.getZero().add(1 + 0.1 * c);
                }
            }
            combined.getJacobian(CartesianDerivativesFilter.USE_PVA, jacobian);

            for (int j = 0; j < 100; ++j) {

                FieldPVCoordinates<T> pv0 = new FieldPVCoordinates<>(randomVector(field, 1e3, random),
                                                                     randomVector(field, 1.0, random),
                                                                     randomVector(field, 1.0e-3, random));
                double epsilonP = 2.0e-12 * pv0.getPosition().getNorm().getReal();
                double epsilonV = 6.0e-11 * pv0.getVelocity().getNorm().getReal();
                double epsilonA = 2.0e-9  * pv0.getAcceleration().getNorm().getReal();

                for (int c = 0; c < directions.length; ++c) {

                    // eight points finite differences estimation of a Jacobian column
                    FieldPVCoordinates<T> pvm4h = combined.transformPVCoordinates(new FieldPVCoordinates<>(1.0, pv0, -4 * h, directions[c]));
                    FieldPVCoordinates<T> pvm3h = combined.transformPVCoordinates(new FieldPVCoordinates<>(1.0, pv0, -3 * h, directions[c]));
                    FieldPVCoordinates<T> pvm2h = combined.transformPVCoordinates(new FieldPVCoordinates<>(1.0, pv0, -2 * h, directions[c]));
                    FieldPVCoordinates<T> pvm1h = combined.transformPVCoordinates(new FieldPVCoordinates<>(1.0, pv0, -1 * h, directions[c]));
                    FieldPVCoordinates<T> pvp1h = combined.transformPVCoordinates(new FieldPVCoordinates<>(1.0, pv0, +1 * h, directions[c]));
                    FieldPVCoordinates<T> pvp2h = combined.transformPVCoordinates(new FieldPVCoordinates<>(1.0, pv0, +2 * h, directions[c]));
                    FieldPVCoordinates<T> pvp3h = combined.transformPVCoordinates(new FieldPVCoordinates<>(1.0, pv0, +3 * h, directions[c]));
                    FieldPVCoordinates<T> pvp4h = combined.transformPVCoordinates(new FieldPVCoordinates<>(1.0, pv0, +4 * h, directions[c]));
                    FieldPVCoordinates<T> d4   = new FieldPVCoordinates<>(pvm4h, pvp4h);
                    FieldPVCoordinates<T> d3   = new FieldPVCoordinates<>(pvm3h, pvp3h);
                    FieldPVCoordinates<T> d2   = new FieldPVCoordinates<>(pvm2h, pvp2h);
                    FieldPVCoordinates<T> d1   = new FieldPVCoordinates<>(pvm1h, pvp1h);
                    double d = 1.0 / (840 * h);
                    FieldPVCoordinates<T> estimatedColumn = new FieldPVCoordinates<>(-3 * d, d4, 32 * d, d3, -168 * d, d2, 672 * d, d1);

                    // check analytical Jacobian against finite difference reference
                    Assertions.assertEquals(estimatedColumn.getPosition().getX().getReal(),     jacobian[0][c].getReal(), epsilonP);
                    Assertions.assertEquals(estimatedColumn.getPosition().getY().getReal(),     jacobian[1][c].getReal(), epsilonP);
                    Assertions.assertEquals(estimatedColumn.getPosition().getZ().getReal(),     jacobian[2][c].getReal(), epsilonP);
                    Assertions.assertEquals(estimatedColumn.getVelocity().getX().getReal(),     jacobian[3][c].getReal(), epsilonV);
                    Assertions.assertEquals(estimatedColumn.getVelocity().getY().getReal(),     jacobian[4][c].getReal(), epsilonV);
                    Assertions.assertEquals(estimatedColumn.getVelocity().getZ().getReal(),     jacobian[5][c].getReal(), epsilonV);
                    Assertions.assertEquals(estimatedColumn.getAcceleration().getX().getReal(), jacobian[6][c].getReal(), epsilonA);
                    Assertions.assertEquals(estimatedColumn.getAcceleration().getY().getReal(), jacobian[7][c].getReal(), epsilonA);
                    Assertions.assertEquals(estimatedColumn.getAcceleration().getZ().getReal(), jacobian[8][c].getReal(), epsilonA);

                }

            }
        }

    }

    @Test
    public void testLine() {
        doTestLine(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestLine(Field<T> field) {
        RandomGenerator random = new Well19937a(0x4a5ff67426c5731fl);
        for (int i = 0; i < 100; ++i) {
            FieldTransform<T> transform = randomTransform(field, random);
            for (int j = 0; j < 20; ++j) {
                FieldVector3D<T> p0 = randomVector(field, 1.0e3, random);
                FieldVector3D<T> p1 = randomVector(field, 1.0e3, random);
                FieldLine<T> l = new FieldLine<>(p0, p1, 1.0e-10);
                FieldLine<T> transformed = transform.transformLine(l);
                for (int k = 0; k < 10; ++k) {
                    FieldVector3D<T> p = l.pointAt(random.nextDouble() * 1.0e6);
                    Assertions.assertEquals(0.0, transformed.distance(transform.transformPosition(p)).getReal(), 1.0e-9);
                }
            }
        }
    }

    @Test
    public void testLineDouble() {
        doTestLineDouble(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestLineDouble(Field<T> field) {
        RandomGenerator random = new Well19937a(0x4a5ff67426c5731fl);
        for (int i = 0; i < 100; ++i) {
            FieldTransform<T> transform = randomTransform(field, random);
            for (int j = 0; j < 20; ++j) {
                Vector3D p0 = randomVector(field, 1.0e3, random).toVector3D();
                Vector3D p1 = randomVector(field, 1.0e3, random).toVector3D();
                Line l = new Line(p0, p1, 1.0e-10);
                FieldLine<T> transformed = transform.transformLine(l);
                for (int k = 0; k < 10; ++k) {
                    Vector3D p = l.pointAt(random.nextDouble() * 1.0e6);
                    Assertions.assertEquals(0.0, transformed.distance(transform.transformPosition(p)).getReal(), 1.0e-9);
                }
            }
        }
    }

    @Test
    public void testLinear() {
        doTestLinear(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestLinear(final Field<T> field) {

        RandomGenerator random = new Well19937a(0x14f6411217b148d8l);
        for (int n = 0; n < 100; ++n) {
            FieldTransform<T> t = randomTransform(field, random);

            // build an equivalent linear transform by extracting raw translation/rotation
            FieldMatrix<T> linearA = MatrixUtils.createFieldMatrix(field, 3, 4);
            linearA.setSubMatrix(t.getRotation().getMatrix(), 0, 0);
            FieldVector3D<T> rt = t.getRotation().applyTo(t.getTranslation());
            linearA.setEntry(0, 3, rt.getX());
            linearA.setEntry(1, 3, rt.getY());
            linearA.setEntry(2, 3, rt.getZ());

            // build an equivalent linear transform by observing transformed points
            FieldMatrix<T> linearB = MatrixUtils.createFieldMatrix(field, 3, 4);
            FieldVector3D<T> p0 = t.transformPosition(FieldVector3D.getZero(field));
            FieldVector3D<T> pI = t.transformPosition(FieldVector3D.getPlusI(field)).subtract(p0);
            FieldVector3D<T> pJ = t.transformPosition(FieldVector3D.getPlusJ(field)).subtract(p0);
            FieldVector3D<T> pK = t.transformPosition(FieldVector3D.getPlusK(field)).subtract(p0);
            linearB.setEntry(0, 0, pI.getX());
            linearB.setEntry(1, 0, pI.getY());
            linearB.setEntry(2, 0, pI.getZ());
            linearB.setEntry(0, 1, pJ.getX());
            linearB.setEntry(1, 1, pJ.getY());
            linearB.setEntry(2, 1, pJ.getZ());
            linearB.setEntry(0, 2, pK.getX());
            linearB.setEntry(1, 2, pK.getY());
            linearB.setEntry(2, 2, pK.getZ());
            linearB.setEntry(0, 3, p0.getX());
            linearB.setEntry(1, 3, p0.getY());
            linearB.setEntry(2, 3, p0.getZ());

            // both linear transforms should be equal
            FieldMatrix<T> sub = linearB.subtract(linearA);
            double refMax = 0;
            double diffMax = 0;
            for (int i = 0; i < linearA.getRowDimension(); ++i) {
                for (int j = 0; j < linearA.getColumnDimension(); ++j) {
                    refMax = FastMath.max(linearA.getEntry(i, j).getReal(), refMax);
                    diffMax = FastMath.max(sub.getEntry(i, j).getReal(), diffMax);
                }
            }
            Assertions.assertEquals(0.0, diffMax, 2.0e-12 * refMax);


            for (int i = 0; i < 100; ++i) {
                FieldVector3D<T> p  = randomVector(field, 1.0e3, random);
                FieldVector3D<T> q  = t.transformPosition(p);

                T[] pField = MathArrays.buildArray(field, 4);
                pField[0] = p.getX();
                pField[1] = p.getY();
                pField[2] = p.getZ();
                pField[3] = field.getOne();
                T[] qA = linearA.operate(pField);
                Assertions.assertEquals(q.getX().getReal(), qA[0].getReal(), 1.0e-13 * p.getNorm().getReal());
                Assertions.assertEquals(q.getY().getReal(), qA[1].getReal(), 1.0e-13 * p.getNorm().getReal());
                Assertions.assertEquals(q.getZ().getReal(), qA[2].getReal(), 1.0e-13 * p.getNorm().getReal());

                T[] qB = linearB.operate(pField);
                Assertions.assertEquals(q.getX().getReal(), qB[0].getReal(), 1.0e-10 * p.getNorm().getReal());
                Assertions.assertEquals(q.getY().getReal(), qB[1].getReal(), 1.0e-10 * p.getNorm().getReal());
                Assertions.assertEquals(q.getZ().getReal(), qB[2].getReal(), 1.0e-10 * p.getNorm().getReal());

            }

        }

    }

    @Test
    public void testShift() {
        doTestShift(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestShift(Field<T> field) {

        // the following transform corresponds to a frame moving along the line x=1 and rotating around its -z axis
        // the linear motion velocity is (0, +1, 0), the angular rate is PI/2
        // at t = -1 the frame origin is at (1, -1, 0), its X axis is equal to  Xref and its Y axis is equal to  Yref
        // at t =  0 the frame origin is at (1,  0, 0), its X axis is equal to -Yref and its Y axis is equal to  Xref
        // at t = +1 the frame origin is at (1, +1, 0), its X axis is equal to -Xref and its Y axis is equal to -Yref
        FieldAbsoluteDate<T> date = FieldAbsoluteDate.getGalileoEpoch(field);
        double alpha0 = 0.5 * FastMath.PI;
        double omega  = 0.5 * FastMath.PI;
        FieldTransform<T> t   = new FieldTransform<>(date,
                                      new FieldTransform<>(date, FieldVector3D.getMinusI(field), FieldVector3D.getMinusJ(field)),
                                      new FieldTransform<>(date,
                                                           new FieldRotation<>(FieldVector3D.getPlusK(field),
                                                                               field.getZero().add(alpha0),
                                                                               RotationConvention.VECTOR_OPERATOR),
                                                           new FieldVector3D<>(omega, FieldVector3D.getMinusK(field))));

        for (double dt = -10.0; dt < 10.0; dt += 0.125) {

            FieldTransform<T> shifted = t.shiftedBy(dt);

            // the following point should always remain at moving frame origin
            FieldPVCoordinates<T> expectedFixedPoint =
                    shifted.transformPVCoordinates(new FieldPVCoordinates<>(createVector(field, 1, dt, 0),
                                                                            FieldVector3D.getPlusJ(field),
                                                                            FieldVector3D.getZero(field)));
            checkVector(expectedFixedPoint.getPosition(),     FieldVector3D.getZero(field), 1.0e-14);
            checkVector(expectedFixedPoint.getVelocity(),     FieldVector3D.getZero(field), 1.0e-14);
            checkVector(expectedFixedPoint.getAcceleration(), FieldVector3D.getZero(field), 1.0e-14);

            // fixed frame origin apparent motion in moving frame
            FieldPVCoordinates<T> expectedApparentMotion = shifted.transformPVCoordinates(PVCoordinates.ZERO);
            double c = FastMath.cos(alpha0 + omega * dt);
            double s = FastMath.sin(alpha0 + omega * dt);
            FieldVector3D<T> referencePosition = createVector(field,
                                                              -c + dt * s,
                                                              -s - dt * c,
                                                              0);
            FieldVector3D<T> referenceVelocity = createVector(field,
                                                              (1 + omega) * s + dt * omega * c,
                                                             -(1 + omega) * c + dt * omega * s,
                                                              0);
            FieldVector3D<T> referenceAcceleration = createVector(field,
                                                                  omega * (2 + omega) * c - dt * omega * omega * s,
                                                                  omega * (2 + omega) * s + dt * omega * omega * c,
                                                                  0);
            checkVector(expectedApparentMotion.getPosition(),     referencePosition,     1.0e-14);
            checkVector(expectedApparentMotion.getVelocity(),     referenceVelocity,     1.0e-14);
            checkVector(expectedApparentMotion.getAcceleration(), referenceAcceleration, 1.0e-14);

        }

    }

    @Test
    public void testShiftDerivatives() {
        doTestShiftDerivatives(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestShiftDerivatives(Field<T> field) {

        RandomGenerator random = new Well19937a(0x5acda4f605aadce7l);
        for (int i = 0; i < 10; ++i) {
            FieldTransform<T> t = randomTransform(field, random);

            for (double dtD = -10.0; dtD < 10.0; dtD += 0.125) {

                T dt = field.getZero().add(dtD);
                FieldTransform<T> t0    = t.shiftedBy(dt);
                T v        = t0.getVelocity().getNorm();
                T a        = t0.getAcceleration().getNorm();
                T omega    = t0.getRotationRate().getNorm();
                T omegaDot = t0.getRotationAcceleration().getNorm();

                // numerical derivatives
                T h = omega.reciprocal().multiply(0.01);
                FieldTransform<T> tm4h = t.shiftedBy(dt.subtract(h.multiply(4)));
                FieldTransform<T> tm3h = t.shiftedBy(dt.subtract(h.multiply(3)));
                FieldTransform<T> tm2h = t.shiftedBy(dt.subtract(h.multiply(2)));
                FieldTransform<T> tm1h = t.shiftedBy(dt.subtract(h.multiply(1)));
                FieldTransform<T> tp1h = t.shiftedBy(dt.add(h.multiply(1)));
                FieldTransform<T> tp2h = t.shiftedBy(dt.add(h.multiply(2)));
                FieldTransform<T> tp3h = t.shiftedBy(dt.add(h.multiply(3)));
                FieldTransform<T> tp4h = t.shiftedBy(dt.add(h.multiply(4)));
                T numXDot = derivative(h,
                                            tm4h.getTranslation().getX(), tm3h.getTranslation().getX(),
                                            tm2h.getTranslation().getX(), tm1h.getTranslation().getX(),
                                            tp1h.getTranslation().getX(), tp2h.getTranslation().getX(),
                                            tp3h.getTranslation().getX(), tp4h.getTranslation().getX());
                T numYDot = derivative(h,
                                            tm4h.getTranslation().getY(), tm3h.getTranslation().getY(),
                                            tm2h.getTranslation().getY(), tm1h.getTranslation().getY(),
                                            tp1h.getTranslation().getY(), tp2h.getTranslation().getY(),
                                            tp3h.getTranslation().getY(), tp4h.getTranslation().getY());
                T numZDot = derivative(h,
                                       tm4h.getTranslation().getZ(), tm3h.getTranslation().getZ(),
                                       tm2h.getTranslation().getZ(), tm1h.getTranslation().getZ(),
                                       tp1h.getTranslation().getZ(), tp2h.getTranslation().getZ(),
                                       tp3h.getTranslation().getZ(), tp4h.getTranslation().getZ());
                T numXDot2 = derivative(h,
                                       tm4h.getVelocity().getX(), tm3h.getVelocity().getX(),
                                       tm2h.getVelocity().getX(), tm1h.getVelocity().getX(),
                                       tp1h.getVelocity().getX(), tp2h.getVelocity().getX(),
                                       tp3h.getVelocity().getX(), tp4h.getVelocity().getX());
                T numYDot2 = derivative(h,
                                       tm4h.getVelocity().getY(), tm3h.getVelocity().getY(),
                                       tm2h.getVelocity().getY(), tm1h.getVelocity().getY(),
                                       tp1h.getVelocity().getY(), tp2h.getVelocity().getY(),
                                       tp3h.getVelocity().getY(), tp4h.getVelocity().getY());
                T numZDot2 = derivative(h,
                                       tm4h.getVelocity().getZ(), tm3h.getVelocity().getZ(),
                                       tm2h.getVelocity().getZ(), tm1h.getVelocity().getZ(),
                                       tp1h.getVelocity().getZ(), tp2h.getVelocity().getZ(),
                                       tp3h.getVelocity().getZ(), tp4h.getVelocity().getZ());
                T numQ0Dot = derivative(h,
                                        tm4h.getRotation().getQ0(), tm3h.getRotation().getQ0(),
                                        tm2h.getRotation().getQ0(), tm1h.getRotation().getQ0(),
                                        tp1h.getRotation().getQ0(), tp2h.getRotation().getQ0(),
                                        tp3h.getRotation().getQ0(), tp4h.getRotation().getQ0());
                T numQ1Dot = derivative(h,
                                        tm4h.getRotation().getQ1(), tm3h.getRotation().getQ1(),
                                        tm2h.getRotation().getQ1(), tm1h.getRotation().getQ1(),
                                        tp1h.getRotation().getQ1(), tp2h.getRotation().getQ1(),
                                        tp3h.getRotation().getQ1(), tp4h.getRotation().getQ1());
                T numQ2Dot = derivative(h,
                                        tm4h.getRotation().getQ2(), tm3h.getRotation().getQ2(),
                                        tm2h.getRotation().getQ2(), tm1h.getRotation().getQ2(),
                                        tp1h.getRotation().getQ2(), tp2h.getRotation().getQ2(),
                                        tp3h.getRotation().getQ2(), tp4h.getRotation().getQ2());
                T numQ3Dot = derivative(h,
                                        tm4h.getRotation().getQ3(), tm3h.getRotation().getQ3(),
                                        tm2h.getRotation().getQ3(), tm1h.getRotation().getQ3(),
                                        tp1h.getRotation().getQ3(), tp2h.getRotation().getQ3(),
                                        tp3h.getRotation().getQ3(), tp4h.getRotation().getQ3());
                T numOxDot = derivative(h,
                                       tm4h.getRotationRate().getX(), tm3h.getRotationRate().getX(),
                                       tm2h.getRotationRate().getX(), tm1h.getRotationRate().getX(),
                                       tp1h.getRotationRate().getX(), tp2h.getRotationRate().getX(),
                                       tp3h.getRotationRate().getX(), tp4h.getRotationRate().getX());
                T numOyDot = derivative(h,
                                       tm4h.getRotationRate().getY(), tm3h.getRotationRate().getY(),
                                       tm2h.getRotationRate().getY(), tm1h.getRotationRate().getY(),
                                       tp1h.getRotationRate().getY(), tp2h.getRotationRate().getY(),
                                       tp3h.getRotationRate().getY(), tp4h.getRotationRate().getY());
                T numOzDot = derivative(h,
                                       tm4h.getRotationRate().getZ(), tm3h.getRotationRate().getZ(),
                                       tm2h.getRotationRate().getZ(), tm1h.getRotationRate().getZ(),
                                       tp1h.getRotationRate().getZ(), tp2h.getRotationRate().getZ(),
                                       tp3h.getRotationRate().getZ(), tp4h.getRotationRate().getZ());

                // theoretical derivatives
                T theXDot  = t0.getVelocity().getX();
                T theYDot  = t0.getVelocity().getY();
                T theZDot  = t0.getVelocity().getZ();
                T theXDot2 = t0.getAcceleration().getX();
                T theYDot2 = t0.getAcceleration().getY();
                T theZDot2 = t0.getAcceleration().getZ();
                FieldRotation<T>  r0 = t0.getRotation();
                FieldVector3D<T>  w  = t0.getRotationRate();
                FieldVector3D<T>  q  = new FieldVector3D<>(r0.getQ1(), r0.getQ2(), r0.getQ3());
                FieldVector3D<T>  qw = FieldVector3D.crossProduct(q, w);
                T theQ0Dot  = FieldVector3D.dotProduct(q, w).multiply(-0.5);
                T theQ1Dot  = r0.getQ0().multiply(w.getX()).add(qw.getX()).multiply(0.5);
                T theQ2Dot  = r0.getQ0().multiply(w.getY()).add(qw.getY()).multiply(0.5);
                T theQ3Dot  = r0.getQ0().multiply(w.getZ()).add(qw.getZ()).multiply(0.5);
                T theOxDot2 = t0.getRotationAcceleration().getX();
                T theOyDot2 = t0.getRotationAcceleration().getY();
                T theOzDot2 = t0.getRotationAcceleration().getZ();

                // check consistency
                Assertions.assertEquals(theXDot.getReal(), numXDot.getReal(), 1.0e-13 * v.getReal());
                Assertions.assertEquals(theYDot.getReal(), numYDot.getReal(), 1.0e-13 * v.getReal());
                Assertions.assertEquals(theZDot.getReal(), numZDot.getReal(), 1.0e-13 * v.getReal());

                Assertions.assertEquals(theXDot2.getReal(), numXDot2.getReal(), 1.0e-13 * a.getReal());
                Assertions.assertEquals(theYDot2.getReal(), numYDot2.getReal(), 1.0e-13 * a.getReal());
                Assertions.assertEquals(theZDot2.getReal(), numZDot2.getReal(), 1.0e-13 * a.getReal());

                Assertions.assertEquals(theQ0Dot.getReal(), numQ0Dot.getReal(), 1.0e-13 * omega.getReal());
                Assertions.assertEquals(theQ1Dot.getReal(), numQ1Dot.getReal(), 1.0e-13 * omega.getReal());
                Assertions.assertEquals(theQ2Dot.getReal(), numQ2Dot.getReal(), 1.0e-13 * omega.getReal());
                Assertions.assertEquals(theQ3Dot.getReal(), numQ3Dot.getReal(), 1.0e-13 * omega.getReal());


                Assertions.assertEquals(theOxDot2.getReal(), numOxDot.getReal(), 1.0e-12 * omegaDot.getReal());
                Assertions.assertEquals(theOyDot2.getReal(), numOyDot.getReal(), 1.0e-12 * omegaDot.getReal());
                Assertions.assertEquals(theOzDot2.getReal(), numOzDot.getReal(), 1.0e-12 * omegaDot.getReal());

            }
        }
    }

    @Test
    void testToStaticTransform() {
        // GIVEN
        final Field<Complex> field = ComplexField.getInstance();
        final PVCoordinates pvCoordinates = new PVCoordinates();
        final AngularCoordinates angularCoordinates = new AngularCoordinates();
        final Transform transform = new Transform(AbsoluteDate.ARBITRARY_EPOCH, pvCoordinates, angularCoordinates);
        final FieldTransform<Complex> fieldTransform = new FieldTransform<>(field, transform);
        // WHEN
        final FieldStaticTransform<Complex> actualStaticTransform = fieldTransform.toStaticTransform();
        // THEN
        final FieldStaticTransform<Complex> expectedStaticTransform = fieldTransform.staticShiftedBy(Complex.ZERO);
        Assertions.assertEquals(expectedStaticTransform.getDate(), actualStaticTransform.getDate());
        Assertions.assertEquals(expectedStaticTransform.getTranslation(), actualStaticTransform.getTranslation());
        Assertions.assertEquals(0., Rotation.distance(expectedStaticTransform.getRotation().toRotation(),
                actualStaticTransform.getRotation().toRotation()));
    }

    @Test
    public void testInterpolation() {
        doTestInterpolation(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestInterpolation(Field<T> field) {
        FieldAbsoluteDate<T> t0 = FieldAbsoluteDate.getGalileoEpoch(field);
        List<FieldTransform<T>> sample = new ArrayList<FieldTransform<T>>();
        for (int i = 0; i < 5; ++i) {
            sample.add(evolvingTransform(t0, i * 0.8));
        }

        for (double dt = 0.1; dt <= 3.1; dt += 0.01) {
            FieldTransform<T> reference = evolvingTransform(t0, dt);
            FieldTransform<T> interpolated = FieldTransform.interpolate(reference.getFieldDate(), sample);
            FieldTransform<T> error = new FieldTransform<>(reference.getFieldDate(), reference, interpolated.getInverse());
            Assertions.assertEquals(0.0, error.getCartesian().getPosition().getNorm().getReal(),           4.0e-12);
            Assertions.assertEquals(0.0, error.getCartesian().getVelocity().getNorm().getReal(),           3.0e-11);
            Assertions.assertEquals(0.0, error.getCartesian().getAcceleration().getNorm().getReal(),       3.0e-10);
            Assertions.assertEquals(0.0, error.getAngular().getRotation().getAngle().getReal(),            2.0e-10);
            Assertions.assertEquals(0.0, error.getAngular().getRotationRate().getNorm().getReal(),         2.0e-09);
            Assertions.assertEquals(0.0, error.getAngular().getRotationAcceleration().getNorm().getReal(), 8.0e-09);

        }

    }

    private <T extends CalculusFieldElement<T>> FieldTransform<T> evolvingTransform(final FieldAbsoluteDate<T> t0, final double dt) {
        // the following transform corresponds to a frame moving along the circle r = 1
        // with its x axis always pointing to the reference frame center
        final Field<T> field = t0.getField();
        final double omega = 0.2;
        final FieldAbsoluteDate<T> date = t0.shiftedBy(dt);
        final double cos = FastMath.cos(omega * dt);
        final double sin = FastMath.sin(omega * dt);
        return new FieldTransform<>(date,
                                    new FieldTransform<>(date,
                                                         createVector(field, -cos, -sin, 0),
                                                         createVector(field, omega * sin, -omega * cos, 0),
                                                         createVector(field, omega * omega * cos, omega * omega * sin, 0)),
                                    new FieldTransform<>(date,
                                                         new FieldRotation<>(FieldVector3D.getPlusK(field),
                                                                             field.getZero().add(FastMath.PI - omega * dt),
                                                                             RotationConvention.VECTOR_OPERATOR),
                                                         new FieldVector3D<>(omega, FieldVector3D.getPlusK(field))));
    }

    private <T extends CalculusFieldElement<T>> T derivative(T h,
                                                         T ym4h, T ym3h, T ym2h, T ym1h,
                                                         T yp1h, T yp2h, T yp3h, T yp4h) {
        return     yp4h.subtract(ym4h).multiply(  -3).
               add(yp3h.subtract(ym3h).multiply(  32)).
               add(yp2h.subtract(ym2h).multiply(-168)).
               add(yp1h.subtract(ym1h).multiply( 672)).
               divide(h.multiply(840));
    }

    private <T extends CalculusFieldElement<T>> FieldTransform<T> randomTransform(Field<T> field, RandomGenerator random) {
        // generate a random transform
        FieldTransform<T> combined = FieldTransform.getIdentity(field);
        for (int k = 0; k < 20; ++k) {
            FieldTransform<T> t = random.nextBoolean() ?
                                  new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field),
                                                       randomVector(field, 1.0e3, random),
                                                       randomVector(field, 1.0, random),
                                                       randomVector(field, 1.0e-3, random)) :
                                  new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field),
                                                       randomRotation(field, random),
                                                       randomVector(field, 0.01, random),
                                                       randomVector(field, 1.0e-4, random));
            combined = new FieldTransform<>(FieldAbsoluteDate.getJ2000Epoch(field), combined, t);
        }
        return combined;
    }

    private <T extends CalculusFieldElement<T>> FieldVector3D<T> randomVector(Field<T> field, double scale, RandomGenerator random) {
        return createVector(field,
                            random.nextDouble() * scale,
                            random.nextDouble() * scale,
                            random.nextDouble() * scale);
    }

    private <T extends CalculusFieldElement<T>> FieldRotation<T> randomRotation(Field<T> field, RandomGenerator random) {
        double q0 = random.nextDouble() * 2 - 1;
        double q1 = random.nextDouble() * 2 - 1;
        double q2 = random.nextDouble() * 2 - 1;
        double q3 = random.nextDouble() * 2 - 1;
        return createRotation(field, q0, q1, q2, q3, true);
    }

    private <T extends CalculusFieldElement<T>> void checkNoTransform(FieldTransform<T> transform,
                                                                  RandomGenerator random) {
        for (int i = 0; i < 100; ++i) {
            FieldVector3D<T> a = randomVector(transform.getFieldDate().getField(), 1.0e3, random);
            FieldVector3D<T> tA = transform.transformVector(a);
            Assertions.assertEquals(0, a.subtract(tA).getNorm().getReal(), 1.0e-10 * a.getNorm().getReal());
            FieldVector3D<T> b = randomVector(transform.getFieldDate().getField(), 1.0e3, random);
            FieldVector3D<T> tB = transform.transformPosition(b);
            Assertions.assertEquals(0, b.subtract(tB).getNorm().getReal(), 1.0e-10 * b.getNorm().getReal());
            FieldPVCoordinates<T> pv  = new FieldPVCoordinates<>(randomVector(transform.getFieldDate().getField(), 1.0e3, random),
                                                                 randomVector(transform.getFieldDate().getField(), 1.0, random),
                                                                 randomVector(transform.getFieldDate().getField(), 1.0e-3, random));
            FieldPVCoordinates<T> tPv = transform.transformPVCoordinates(pv);
            checkVector(pv.getPosition(),     tPv.getPosition(), 1.0e-10);
            checkVector(pv.getVelocity(),     tPv.getVelocity(), 3.0e-9);
            checkVector(pv.getAcceleration(), tPv.getAcceleration(), 3.0e-9);
        }
    }

    private <T extends CalculusFieldElement<T>> void checkVector(FieldVector3D<T> reference, FieldVector3D<T> result,
                                                             double relativeTolerance) {
        T refNorm = reference.getNorm();
        T resNorm = result.getNorm();
        double tolerance = relativeTolerance * (1 + FastMath.max(refNorm.getReal(), resNorm.getReal()));
        Assertions.assertEquals(0, FieldVector3D.distance(reference, result).getReal(), tolerance,
                "ref = " + reference + ", res = " + result + " -> " +
                (FieldVector3D.distance(reference, result).divide(1 + FastMath.max(refNorm.getReal(), resNorm.getReal()))));
    }

    private <T extends CalculusFieldElement<T>> FieldVector3D<T> createVector(Field<T> field,
                                                                          double x, double y, double z) {
        return new FieldVector3D<>(field.getZero().add(x),
                                   field.getZero().add(y),
                                   field.getZero().add(z));
    }

    private <T extends CalculusFieldElement<T>> FieldRotation<T> createRotation(Field<T> field,
                                                                           double q0, double q1,
                                                                           double q2, double q3,
                                                                           boolean needsNormalization) {
        return new FieldRotation<>(field.getZero().add(q0),
                                   field.getZero().add(q1),
                                   field.getZero().add(q2),
                                   field.getZero().add(q3),
                                   needsNormalization);
    }

}
