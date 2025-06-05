/* Copyright 2002-2025 CS GROUP
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

import org.hamcrest.MatcherAssert;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1Field;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import java.util.Random;

class FrameTest {

    @Test
    void testSameFrameRoot() {
        Random random = new Random(0x29448c7d58b95565L);
        Frame  frame  = FramesFactory.getEME2000();
        checkNoTransform(frame.getTransformTo(frame, new AbsoluteDate()), random);
        Assertions.assertTrue(frame.getDepth() > 0);
        Assertions.assertEquals(frame.getParent().getDepth() + 1, frame.getDepth());
    }

    @Test
    void testSameFrameNoRoot() {
        Random random = new Random(0xc6e88d0f53e29116L);
        Transform t   = randomTransform(random);
        Frame frame   = new Frame(FramesFactory.getEME2000(), t, null, true);
        checkNoTransform(frame.getTransformTo(frame, new AbsoluteDate()), random);
    }

    @Test
    void testSimilarFrames() {
        Random random = new Random(0x1b868f67a83666e5L);
        Transform t   = randomTransform(random);
        Frame frame1  = new Frame(FramesFactory.getEME2000(), t, null, true);
        Frame frame2  = new Frame(FramesFactory.getEME2000(), t, null, false);
        checkNoTransform(frame1.getTransformTo(frame2, new AbsoluteDate()), random);
    }

    @Test
    void testFromParent() {
        Random random = new Random(0xb92fba1183fe11b8L);
        Transform fromEME2000  = randomTransform(random);
        Frame frame = new Frame(FramesFactory.getEME2000(), fromEME2000, null);
        Transform toEME2000 = frame.getTransformTo(FramesFactory.getEME2000(), new AbsoluteDate());
        checkNoTransform(new Transform(fromEME2000.getDate(), fromEME2000, toEME2000), random);
    }

    @Test
    void testDecomposedTransform() {
        Random random = new Random(0xb7d1a155e726da57L);
        Transform t1  = randomTransform(random);
        Transform t2  = randomTransform(random);
        Transform t3  = randomTransform(random);
        Frame frame1 =
            new Frame(FramesFactory.getEME2000(),
                      new Transform(t1.getDate(), new Transform(t1.getDate(), t1, t2), t3),
                      null);
        Frame frame2 =
            new Frame(new Frame(new Frame(FramesFactory.getEME2000(), t1, null), t2, null), t3, null);
        checkNoTransform(frame1.getTransformTo(frame2, new AbsoluteDate()), random);
    }

    @Test
    void testFindCommon() {

        Random random = new Random(0xb7d1a155e726da57L);
        Transform t1  = randomTransform(random);
        Transform t2  = randomTransform(random);
        Transform t3  = randomTransform(random);

        Frame R1 = new Frame(FramesFactory.getEME2000(), t1, "R1");
        Frame R2 = new Frame(R1, t2, "R2");
        Frame R3 = new Frame(R2, t3, "R3");
        Assertions.assertTrue(R1.getDepth() > 0);
        Assertions.assertEquals(R1.getDepth() + 1, R2.getDepth());
        Assertions.assertEquals(R2.getDepth() + 1, R3.getDepth());

        Transform T = R1.getTransformTo(R3, new AbsoluteDate());

        Transform S = new Transform(t2.getDate(), t2, t3);

        checkNoTransform(new Transform(T.getDate(), T, S.getInverse()) , random);

    }

    @Test
    void testDepthAndAncestor() {
        Random random = new Random(0x01f8d3b944123044L);
        Frame root = Frame.getRoot();

        Frame f1 = new Frame(root, randomTransform(random), "f1");
        Frame f2 = new Frame(f1,   randomTransform(random), "f2");
        Frame f3 = new Frame(f1,   randomTransform(random), "f3");
        Frame f4 = new Frame(f2,   randomTransform(random), "f4");
        Frame f5 = new Frame(f3,   randomTransform(random), "f5");
        Frame f6 = new Frame(f5,   randomTransform(random), "f6");

        Assertions.assertEquals(0, root.getDepth());
        Assertions.assertEquals(1, f1.getDepth());
        Assertions.assertEquals(2, f2.getDepth());
        Assertions.assertEquals(2, f3.getDepth());
        Assertions.assertEquals(3, f4.getDepth());
        Assertions.assertEquals(3, f5.getDepth());
        Assertions.assertEquals(4, f6.getDepth());

        Assertions.assertSame(root, f1.getAncestor(1));
        Assertions.assertSame(root, f6.getAncestor(4));
        Assertions.assertSame(f1, f6.getAncestor(3));
        Assertions.assertSame(f3, f6.getAncestor(2));
        Assertions.assertSame(f5, f6.getAncestor(1));
        Assertions.assertSame(f6, f6.getAncestor(0));

        try {
            f6.getAncestor(5);
            Assertions.fail("an exception should have been triggered");
        } catch (IllegalArgumentException iae) {
            // expected behavior
        } catch (Exception e) {
            Assertions.fail("wrong exception caught: " + e.getClass().getName());
        }

    }

    @Test
    void testIsChildOf() {
        Random random = new Random(0xb7d1a155e726da78L);
        Frame eme2000 = FramesFactory.getEME2000();

        Frame f1 = new Frame(eme2000, randomTransform(random), "f1");
        Frame f2 = new Frame(f1     , randomTransform(random), "f2");
        Frame f4 = new Frame(f2     , randomTransform(random), "f4");
        Frame f5 = new Frame(f4     , randomTransform(random), "f5");
        Frame f6 = new Frame(eme2000, randomTransform(random), "f6");
        Frame f7 = new Frame(f6     , randomTransform(random), "f7");
        Frame f8 = new Frame(f6     , randomTransform(random), "f8");
        Frame f9 = new Frame(f7     , randomTransform(random), "f9");

        // check if the root frame can be an ancestor of another frame
        Assertions.assertFalse(eme2000.isChildOf(f5));

        // check if a frame which belongs to the same branch than the 2nd frame is a branch of it
        Assertions.assertTrue(f5.isChildOf(f1));

        // check if a random frame is the child of the root frame
        Assertions.assertTrue(f9.isChildOf(eme2000));

        // check that a frame is not its own child
        Assertions.assertFalse(f4.isChildOf(f4));

        // check if a frame which belongs to a different branch than the 2nd frame can be a child for it
        Assertions.assertFalse(f9.isChildOf(f5));

        // check if the root frame is not a child of itself
        Assertions.assertFalse(eme2000.isChildOf(eme2000));

        Assertions.assertFalse(f9.isChildOf(f8));

    }

    @Test
    void testH0m9() {
        AbsoluteDate h0         = new AbsoluteDate("2010-07-01T10:42:09", TimeScalesFactory.getUTC());
        Frame itrf              = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        Frame rotatingPadFrame  = new TopocentricFrame(new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                            Constants.WGS84_EARTH_FLATTENING,
                                                                            itrf),
                                                       new GeodeticPoint(FastMath.toRadians(5.0),
                                                                                              FastMath.toRadians(-100.0),
                                                                                              0.0),
                                                       "launch pad");

        // create a new inertially oriented frame that is aligned with ITRF at h0 - 9 seconds
        AbsoluteDate h0M9       = h0.shiftedBy(-9.0);
        Frame eme2000           = FramesFactory.getEME2000();
        Frame frozenLaunchFrame = rotatingPadFrame.getFrozenFrame(eme2000, h0M9, "launch frame");

        // check velocity module is unchanged
        Vector3D pEme2000 = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D vEme2000 = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvEme2000 = new PVCoordinates(pEme2000, vEme2000);
        PVCoordinates pvH0m9 = eme2000.getTransformTo(frozenLaunchFrame, h0M9).transformPVCoordinates(pvEme2000);
        Assertions.assertEquals(vEme2000.getNorm(), pvH0m9.getVelocity().getNorm(), 1.0e-6);
        Vector3D pH0m9 = eme2000.getStaticTransformTo(frozenLaunchFrame, h0M9)
                .transformPosition(pvEme2000.getPosition());
        MatcherAssert.assertThat(pH0m9,
                OrekitMatchers.vectorCloseTo(pvH0m9.getPosition(), 1e-15));

        // this frame is fixed with respect to EME2000 but rotates with respect to the non-frozen one
        // the following loop should have a fixed angle a1 and an evolving angle a2
        double minA1 = Double.POSITIVE_INFINITY;
        double maxA1 = Double.NEGATIVE_INFINITY;
        double minA2 = Double.POSITIVE_INFINITY;
        double maxA2 = Double.NEGATIVE_INFINITY;
        double dt;
        for (dt = 0; dt < 86164; dt += 300.0) {
            AbsoluteDate date = h0M9.shiftedBy(dt);
            double a1 = frozenLaunchFrame.getTransformTo(eme2000,          date).getRotation().getAngle();
            double a2 = frozenLaunchFrame.getTransformTo(rotatingPadFrame, date).getRotation().getAngle();
            minA1 = FastMath.min(minA1, a1);
            maxA1 = FastMath.max(maxA1, a1);
            minA2 = FastMath.min(minA2, a2);
            maxA2 = FastMath.max(maxA2, a2);
        }
        Assertions.assertEquals(0, maxA1 - minA1, 1.0e-12);
        Assertions.assertEquals(FastMath.PI, maxA2 - minA2, 0.01);

    }

    private Transform randomTransform(Random random) {
        Transform transform = Transform.IDENTITY;
        for (int i = random.nextInt(10); i > 0; --i) {
            if (random.nextBoolean()) {
                Vector3D u = new Vector3D(random.nextDouble() * 1000.0,
                                          random.nextDouble() * 1000.0,
                                          random.nextDouble() * 1000.0);
                transform = new Transform(transform.getDate(), transform, new Transform(transform.getDate(), u));
            } else {
                double q0 = random.nextDouble() * 2 - 1;
                double q1 = random.nextDouble() * 2 - 1;
                double q2 = random.nextDouble() * 2 - 1;
                double q3 = random.nextDouble() * 2 - 1;
                double q  = FastMath.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
                Rotation r = new Rotation(q0 / q, q1 / q, q2 / q, q3 / q, false);
                transform = new Transform(transform.getDate(), transform, new Transform(transform.getDate(), r));
            }
        }
        return transform;
    }

    private void checkNoTransform(Transform transform, Random random) {
        for (int i = 0; i < 100; ++i) {
            Vector3D a = new Vector3D(random.nextDouble(),
                                      random.nextDouble(),
                                      random.nextDouble());
            Vector3D b = transform.transformVector(a);
            Assertions.assertEquals(0, a.subtract(b).getNorm(), 1.0e-10);
            Vector3D c = transform.transformPosition(a);
            Assertions.assertEquals(0, a.subtract(c).getNorm(), 1.0e-10);
        }
    }

    @Test
    void testGetKinematicTransformTo() {
        // GIVEN
        final Frame oldFrame = FramesFactory.getEME2000();
        final Frame newFrame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final KinematicTransform kinematicTransform = oldFrame.getKinematicTransformTo(newFrame, date);
        // THEN
        final Transform transform = oldFrame.getTransformTo(newFrame, date);
        Assertions.assertEquals(date, kinematicTransform.getDate());
        Assertions.assertEquals(transform.getCartesian().getPosition(), kinematicTransform.getTranslation());
        Assertions.assertEquals(transform.getCartesian().getVelocity(), kinematicTransform.getVelocity());
        Assertions.assertEquals(0., Rotation.distance(transform.getRotation(), kinematicTransform.getRotation()));
        Assertions.assertEquals(transform.getRotationRate(), kinematicTransform.getRotationRate());
    }

    @Test
    void testGetStaticTransformIdentity() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame mockedFrame = Mockito.mock(Frame.class);
        Mockito.when(mockedFrame.getStaticTransformTo(mockedFrame, date)).thenCallRealMethod();
        // WHEN
        final StaticTransform staticTransform = mockedFrame.getStaticTransformTo(mockedFrame, date);
        // THEN
        Assertions.assertEquals(staticTransform, staticTransform.getStaticInverse());
    }

    @Test
    void testGetStaticTransformIdentityField() {
        // GIVEN
        final FieldAbsoluteDate<Complex> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(ComplexField.getInstance());
        final Frame frame = FramesFactory.getGCRF();
        // WHEN
        final FieldStaticTransform<Complex> staticTransform = frame.getStaticTransformTo(frame, fieldDate);
        // THEN
        Assertions.assertEquals(staticTransform.getClass(), staticTransform.getStaticInverse().getClass());
    }

    @Test
    void testFieldGetKinematicTransformToWithConstantDate() {
        templateTestFieldGetKinematicTransformTo(getComplexDate());
    }

    @Test
    void testFieldGetKinematicTransformToWithNonConstantDate() {
        templateTestFieldGetKinematicTransformTo(getComplexDate().shiftedBy(Complex.I));
    }

    private void templateTestFieldGetKinematicTransformTo(final FieldAbsoluteDate<Complex> fieldDate) {
        // GIVEN
        final Frame oldFrame = FramesFactory.getEME2000();
        final Frame newFrame = FramesFactory.getGCRF();
        // WHEN
        final FieldKinematicTransform<Complex> fieldKinematicTransform = oldFrame.getKinematicTransformTo(newFrame,
                fieldDate);
        // THEN
        final KinematicTransform kinematicTransform = oldFrame.getKinematicTransformTo(newFrame,
                fieldDate.toAbsoluteDate());
        Assertions.assertEquals(kinematicTransform.getDate(), fieldKinematicTransform.getDate());
        Assertions.assertEquals(kinematicTransform.getTranslation(), fieldKinematicTransform.getTranslation().toVector3D());
        Assertions.assertEquals(kinematicTransform.getVelocity(), fieldKinematicTransform.getVelocity().toVector3D());
        Assertions.assertEquals(0., Rotation.distance(kinematicTransform.getRotation(),
                fieldKinematicTransform.getRotation().toRotation()));
        Assertions.assertEquals(kinematicTransform.getRotationRate(),
                fieldKinematicTransform.getRotationRate().toVector3D());
    }

    private FieldAbsoluteDate<Complex> getComplexDate() {
        return FieldAbsoluteDate.getArbitraryEpoch(ComplexField.getInstance());
    }

    @Test
    void testGetTransformTo() {
        // GIVEN
        final Frame oldFrame = FramesFactory.getEME2000();
        final Frame newFrame = FramesFactory.getGTOD(true);
        final UnivariateDerivative1Field field = UnivariateDerivative1Field.getInstance();
        final FieldAbsoluteDate<UnivariateDerivative1> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(field);
        final FieldAbsoluteDate<UnivariateDerivative1> shiftedDate = fieldDate.shiftedBy(new UnivariateDerivative1(0., 1));
        // WHEN
        final FieldTransform<UnivariateDerivative1> fieldTransform = oldFrame.getTransformTo(newFrame, shiftedDate);
        // THEN
        Assertions.assertEquals(shiftedDate, fieldTransform.getFieldDate());
        final FieldTransform<UnivariateDerivative1> referenceTransform = oldFrame.getTransformTo(newFrame, fieldDate);
        Assertions.assertEquals(fieldTransform.getDate(), referenceTransform.getDate());
        Assertions.assertEquals(fieldTransform.getTranslation().toVector3D(),
                referenceTransform.getTranslation().toVector3D());
        compareFieldVectorWithMargin(fieldTransform.getRotationRate(), referenceTransform.getRotationRate());
        compareFieldVectorWithMargin(fieldTransform.getRotationAcceleration(), referenceTransform.getRotationAcceleration());
        Assertions.assertEquals(0., Rotation.distance(fieldTransform.getRotation().toRotation(),
                referenceTransform.getRotation().toRotation()));
    }

    @Test
    public void testNoPeering() {

        Frame eme2000 = FramesFactory.getEME2000();
        Frame itrf    = FramesFactory.getITRF(IERSConventions.IERS_2010, false);
        Assertions.assertNull(eme2000.getPeer());
        Assertions.assertNull(itrf.getPeer());

        // with caching disabled, tB should be a new recomputed instance, similar to tA
        final AbsoluteDate t0 = new AbsoluteDate(2004, 5, 7, 17, 42, 37.5, TimeScalesFactory.getUTC());
        final Transform tA = eme2000.getTransformTo(itrf, t0);
        final Transform tB = eme2000.getTransformTo(itrf, t0);
        final Transform backAndForth = new Transform(t0, tA, tB.getInverse());
        Assertions.assertNotSame(tA, tB);
        Assertions.assertEquals(0.0, backAndForth.getRotation().getAngle(), 1.0e-20);
        Assertions.assertEquals(0.0, backAndForth.getCartesian().getPosition().getNorm(), 1.0e-20);

    }

    @Test
    public void testNoPeeringKinematic() {

        Frame eme2000 = FramesFactory.getEME2000();
        Frame itrf    = FramesFactory.getITRF(IERSConventions.IERS_2010, false);
        Assertions.assertNull(eme2000.getPeer());
        Assertions.assertNull(itrf.getPeer());

        // with caching disabled, tB should be a new recomputed instance, similar to tA
        final AbsoluteDate t0 = new AbsoluteDate(2004, 5, 7, 17, 42, 37.5, TimeScalesFactory.getUTC());
        final KinematicTransform tA = eme2000.getKinematicTransformTo(itrf, t0);
        final KinematicTransform tB = eme2000.getKinematicTransformTo(itrf, t0);
        final KinematicTransform backAndForth = KinematicTransform.compose(t0, tA, tB.getInverse());
        Assertions.assertNotSame(tA, tB);
        Assertions.assertEquals(0.0, backAndForth.getRotation().getAngle(), 1.0e-20);
        Assertions.assertEquals(0.0, backAndForth.getTranslation().getNorm(), 1.0e-20);

    }

    @Test
    public void testNoPeeringStatic() {

        Frame eme2000 = FramesFactory.getEME2000();
        Frame itrf    = FramesFactory.getITRF(IERSConventions.IERS_2010, false);
        Assertions.assertNull(eme2000.getPeer());
        Assertions.assertNull(itrf.getPeer());

        // with caching disabled, tB should be a new recomputed instance, similar to tA
        final AbsoluteDate t0 = new AbsoluteDate(2004, 5, 7, 17, 42, 37.5, TimeScalesFactory.getUTC());
        final StaticTransform tA = eme2000.getStaticTransformTo(itrf, t0);
        final StaticTransform tB = eme2000.getStaticTransformTo(itrf, t0);
        final StaticTransform backAndForth = StaticTransform.compose(t0, tA, tB.getInverse());
        Assertions.assertNotSame(tA, tB);
        Assertions.assertEquals(0.0, backAndForth.getRotation().getAngle(), 1.0e-20);
        Assertions.assertEquals(0.0, backAndForth.getTranslation().getNorm(), 1.0e-20);

    }

    @Test
    public void testNoPeeringField() {
        doTestNoPeeringField(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestNoPeeringField(final Field<T> field) {

        Frame eme2000 = FramesFactory.getEME2000();
        Frame itrf    = FramesFactory.getITRF(IERSConventions.IERS_2010, false);
        Assertions.assertNull(eme2000.getPeer());
        Assertions.assertNull(itrf.getPeer());

        // with caching disabled, tB should be a new recomputed instance, similar to tA
        final FieldAbsoluteDate<T> t0 = new FieldAbsoluteDate<>(field, 2004, 5, 7, 17, 42, 37.5, TimeScalesFactory.getUTC());
        final FieldTransform<T> tA = eme2000.getTransformTo(itrf, t0);
        final FieldTransform<T> tB = eme2000.getTransformTo(itrf, t0);
        final FieldTransform<T> backAndForth = new FieldTransform<>(t0, tA, tB.getInverse());
        Assertions.assertNotSame(tA, tB);
        Assertions.assertEquals(0.0, backAndForth.getRotation().getAngle().getReal(), 1.0e-20);
        Assertions.assertEquals(0.0, backAndForth.getCartesian().getPosition().getNorm().getReal(), 1.0e-20);

    }

    @Test
    public void testNoPeeringFieldKinematic() {
        doTestNoPeeringFieldKinematic(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestNoPeeringFieldKinematic(final Field<T> field) {

        Frame eme2000 = FramesFactory.getEME2000();
        Frame itrf    = FramesFactory.getITRF(IERSConventions.IERS_2010, false);
        Assertions.assertNull(eme2000.getPeer());
        Assertions.assertNull(itrf.getPeer());

        // with caching disabled, tB should be a new recomputed instance, similar to tA
        final FieldAbsoluteDate<T> t0 = new FieldAbsoluteDate<>(field, 2004, 5, 7, 17, 42, 37.5, TimeScalesFactory.getUTC());
        final FieldKinematicTransform<T> tA = eme2000.getKinematicTransformTo(itrf, t0);
        final FieldKinematicTransform<T> tB = eme2000.getKinematicTransformTo(itrf, t0);
        final FieldKinematicTransform<T> backAndForth = FieldKinematicTransform.compose(t0, tA, tB.getInverse());
        Assertions.assertNotSame(tA, tB);
        Assertions.assertEquals(0.0, backAndForth.getRotation().getAngle().getReal(), 1.0e-20);
        Assertions.assertEquals(0.0, backAndForth.getTranslation().getNorm().getReal(), 1.0e-20);

    }

    @Test
    public void testNoPeeringFieldStatic() {
        doTestNoPeeringFieldStatic(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestNoPeeringFieldStatic(final Field<T> field) {

        Frame eme2000 = FramesFactory.getEME2000();
        Frame itrf    = FramesFactory.getITRF(IERSConventions.IERS_2010, false);
        Assertions.assertNull(eme2000.getPeer());
        Assertions.assertNull(itrf.getPeer());

        // with caching disabled, tB should be a new recomputed instance, similar to tA
        final FieldAbsoluteDate<T> t0 = new FieldAbsoluteDate<>(field, 2004, 5, 7, 17, 42, 37.5, TimeScalesFactory.getUTC());
        final FieldStaticTransform<T> tA = eme2000.getStaticTransformTo(itrf, t0);
        final FieldStaticTransform<T> tB = eme2000.getStaticTransformTo(itrf, t0);
        final FieldStaticTransform<T> backAndForth = FieldStaticTransform.compose(t0, tA, tB.getInverse());
        Assertions.assertNotSame(tA, tB);
        Assertions.assertEquals(0.0, backAndForth.getRotation().getAngle().getReal(), 1.0e-20);
        Assertions.assertEquals(0.0, backAndForth.getTranslation().getNorm().getReal(), 1.0e-20);

    }

    @Test
    public void testPeering() {

        Frame eme2000 = FramesFactory.getEME2000();
        Frame itrf    = FramesFactory.getITRF(IERSConventions.IERS_2010, false);
        Assertions.assertNull(eme2000.getPeer());
        Assertions.assertNull(itrf.getPeer());

        final int cachesize = 20;
        eme2000.setPeerCaching(itrf, cachesize);
        Assertions.assertSame(itrf,    eme2000.getPeer());
        Assertions.assertSame(eme2000, itrf.getPeer());

        // with caching activated, tB should be a reference to the same transform as tA
        // without being recomputed
        final AbsoluteDate t0 = new AbsoluteDate(2004, 5, 7, 17, 42, 37.5, TimeScalesFactory.getUTC());
        final Transform tA = eme2000.getTransformTo(itrf, t0);
        final Transform tB = eme2000.getTransformTo(itrf, t0);
        final Transform backAndForth = new Transform(t0, tA, tB.getInverse());
        Assertions.assertSame(tA, tB);
        Assertions.assertEquals(0.0, backAndForth.getRotation().getAngle(), 1.0e-20);
        Assertions.assertEquals(0.0, backAndForth.getCartesian().getPosition().getNorm(), 1.0e-20);

        final Transform[] direct  = new Transform[cachesize];
        final Transform[] inverse = new Transform[cachesize];
        for ( int i = 0; i < cachesize; i++ ) {
            direct[i]  = eme2000.getTransformTo(itrf, t0.shiftedBy(i));
            inverse[i] = itrf.getTransformTo(eme2000, t0.shiftedBy(i));
        }

        // with caching activated, we should not recompute any transform, just retrieve existing ones
        for (int i = 0; i < 10000; ++i) {
            Assertions.assertSame(direct[i % cachesize], eme2000.getTransformTo(itrf, t0.shiftedBy(i % cachesize)));
            Assertions.assertSame(inverse[i % cachesize], itrf.getTransformTo(eme2000, t0.shiftedBy(i % cachesize)));
        }

    }

    @Test
    public void testPeeringKinematic() {

        Frame eme2000 = FramesFactory.getEME2000();
        Frame itrf    = FramesFactory.getITRF(IERSConventions.IERS_2010, false);
        Assertions.assertNull(eme2000.getPeer());
        Assertions.assertNull(itrf.getPeer());

        final int cachesize = 20;
        eme2000.setPeerCaching(itrf, cachesize);
        Assertions.assertSame(itrf,    eme2000.getPeer());
        Assertions.assertSame(eme2000, itrf.getPeer());

        // with caching activated, tB should be a reference to the same transform as tA
        // without being recomputed
        final AbsoluteDate t0 = new AbsoluteDate(2004, 5, 7, 17, 42, 37.5, TimeScalesFactory.getUTC());
        final KinematicTransform tA = eme2000.getKinematicTransformTo(itrf, t0);
        final KinematicTransform tB = eme2000.getKinematicTransformTo(itrf, t0);
        final KinematicTransform backAndForth = KinematicTransform.compose(t0, tA, tB.getInverse());
        Assertions.assertSame(tA, tB);
        Assertions.assertEquals(0.0, backAndForth.getRotation().getAngle(), 1.0e-20);
        Assertions.assertEquals(0.0, backAndForth.getTranslation().getNorm(), 1.0e-20);

        final KinematicTransform[] direct  = new KinematicTransform[cachesize];
        final KinematicTransform[] inverse = new KinematicTransform[cachesize];
        for ( int i = 0; i < cachesize; i++ ) {
            direct[i]  = eme2000.getKinematicTransformTo(itrf, t0.shiftedBy(i));
            inverse[i] = itrf.getKinematicTransformTo(eme2000, t0.shiftedBy(i));
        }

        // with caching activated, we should not recompute any transform, just retrieve existing ones
        for (int i = 0; i < 10000; ++i) {
            Assertions.assertSame(direct[i % cachesize], eme2000.getKinematicTransformTo(itrf, t0.shiftedBy(i % cachesize)));
            Assertions.assertSame(inverse[i % cachesize], itrf.getKinematicTransformTo(eme2000, t0.shiftedBy(i % cachesize)));
        }

    }

    @Test
    public void testPeeringStatic() {

        Frame eme2000 = FramesFactory.getEME2000();
        Frame itrf    = FramesFactory.getITRF(IERSConventions.IERS_2010, false);
        Assertions.assertNull(eme2000.getPeer());
        Assertions.assertNull(itrf.getPeer());

        final int cachesize = 20;
        eme2000.setPeerCaching(itrf, cachesize);
        Assertions.assertSame(itrf,    eme2000.getPeer());
        Assertions.assertSame(eme2000, itrf.getPeer());

        // with caching activated, tB should be a reference to the same transform as tA
        // without being recomputed
        final AbsoluteDate t0 = new AbsoluteDate(2004, 5, 7, 17, 42, 37.5, TimeScalesFactory.getUTC());
        final StaticTransform tA = eme2000.getStaticTransformTo(itrf, t0);
        final StaticTransform tB = eme2000.getStaticTransformTo(itrf, t0);
        final StaticTransform backAndForth = StaticTransform.compose(t0, tA, tB.getInverse());
        Assertions.assertSame(tA, tB);
        Assertions.assertEquals(0.0, backAndForth.getRotation().getAngle(), 1.0e-20);
        Assertions.assertEquals(0.0, backAndForth.getTranslation().getNorm(), 1.0e-20);

        final StaticTransform[] direct  = new StaticTransform[cachesize];
        final StaticTransform[] inverse = new StaticTransform[cachesize];
        for ( int i = 0; i < cachesize; i++ ) {
            direct[i]  = eme2000.getStaticTransformTo(itrf, t0.shiftedBy(i));
            inverse[i] = itrf.getStaticTransformTo(eme2000, t0.shiftedBy(i));
        }

        // with caching activated, we should not recompute any transform, just retrieve existing ones
        for (int i = 0; i < 10000; ++i) {
            Assertions.assertSame(direct[i % cachesize], eme2000.getStaticTransformTo(itrf, t0.shiftedBy(i % cachesize)));
            Assertions.assertSame(inverse[i % cachesize], itrf.getStaticTransformTo(eme2000, t0.shiftedBy(i % cachesize)));
        }

    }

    @Test
    public void testPeeringField() {
        doTestPeeringField(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestPeeringField(final Field<T> field) {

        Frame eme2000 = FramesFactory.getEME2000();
        Frame itrf    = FramesFactory.getITRF(IERSConventions.IERS_2010, false);
        Assertions.assertNull(eme2000.getPeer());
        Assertions.assertNull(itrf.getPeer());

        // with caching activated, tB should be a reference to the same transform as tA
        // without being recomputed
        final int cachesize = 20;
        eme2000.setPeerCaching(itrf, cachesize);
        Assertions.assertSame(itrf,    eme2000.getPeer());
        Assertions.assertSame(eme2000, itrf.getPeer());

        final FieldAbsoluteDate<T> t0 = new FieldAbsoluteDate<>(field, 2004, 5, 7, 17, 42, 37.5, TimeScalesFactory.getUTC());
        final FieldTransform<T> tA = eme2000.getTransformTo(itrf, t0);
        final FieldTransform<T> tB = eme2000.getTransformTo(itrf, t0);
        final FieldTransform<T> backAndForth = new FieldTransform<>(t0, tA, tB.getInverse());
        Assertions.assertSame(tA, tB);
        Assertions.assertEquals(0.0, backAndForth.getRotation().getAngle().getReal(), 1.0e-20);
        Assertions.assertEquals(0.0, backAndForth.getCartesian().getPosition().getNorm().getReal(), 1.0e-20);

        final FieldTransform<T>[] direct  = new FieldTransform[cachesize];
        final FieldTransform<T>[] inverse = new FieldTransform[cachesize];
        for ( int i = 0; i < cachesize; i++ ) {
            direct[i]  = eme2000.getTransformTo(itrf, t0.shiftedBy(i));
            inverse[i] = itrf.getTransformTo(eme2000, t0.shiftedBy(i));
        }

        // with caching activated, we should not recompute any transform, just retrieve existing ones
        for (int i = 0; i < 10000; ++i) {
            Assertions.assertSame(direct[i % cachesize], eme2000.getTransformTo(itrf, t0.shiftedBy(i % cachesize)));
            Assertions.assertSame(inverse[i % cachesize], itrf.getTransformTo(eme2000, t0.shiftedBy(i % cachesize)));
        }

    }

    @Test
    public void testPeeringFieldKinematic() {
        doTestPeeringField(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestPeeringFieldKinematic(final Field<T> field) {

        Frame eme2000 = FramesFactory.getEME2000();
        Frame itrf    = FramesFactory.getITRF(IERSConventions.IERS_2010, false);
        Assertions.assertNull(eme2000.getPeer());
        Assertions.assertNull(itrf.getPeer());

        // with caching activated, tB should be a reference to the same transform as tA
        // without being recomputed
        final int cachesize = 20;
        eme2000.setPeerCaching(itrf, cachesize);
        Assertions.assertSame(itrf,    eme2000.getPeer());
        Assertions.assertSame(eme2000, itrf.getPeer());

        final FieldAbsoluteDate<T> t0 = new FieldAbsoluteDate<>(field, 2004, 5, 7, 17, 42, 37.5, TimeScalesFactory.getUTC());
        final FieldKinematicTransform<T> tA = eme2000.getKinematicTransformTo(itrf, t0);
        final FieldKinematicTransform<T> tB = eme2000.getKinematicTransformTo(itrf, t0);
        final FieldKinematicTransform<T> backAndForth = FieldKinematicTransform.compose(t0, tA, tB.getInverse());
        Assertions.assertSame(tA, tB);
        Assertions.assertEquals(0.0, backAndForth.getRotation().getAngle().getReal(), 1.0e-20);
        Assertions.assertEquals(0.0, backAndForth.getTranslation().getNorm().getReal(), 1.0e-20);

        final FieldKinematicTransform<T>[] direct  = new FieldKinematicTransform[cachesize];
        final FieldKinematicTransform<T>[] inverse = new FieldKinematicTransform[cachesize];
        for ( int i = 0; i < cachesize; i++ ) {
            direct[i]  = eme2000.getKinematicTransformTo(itrf, t0.shiftedBy(i));
            inverse[i] = itrf.getKinematicTransformTo(eme2000, t0.shiftedBy(i));
        }

        // with caching activated, we should not recompute any transform, just retrieve existing ones
        for (int i = 0; i < 10000; ++i) {
            Assertions.assertSame(direct[i % cachesize], eme2000.getKinematicTransformTo(itrf, t0.shiftedBy(i % cachesize)));
            Assertions.assertSame(inverse[i % cachesize], itrf.getKinematicTransformTo(eme2000, t0.shiftedBy(i % cachesize)));
        }

    }

    @Test
    public void testPeeringFieldStatic() {
        doTestPeeringFieldStatic(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestPeeringFieldStatic(final Field<T> field) {

        Frame eme2000 = FramesFactory.getEME2000();
        Frame itrf    = FramesFactory.getITRF(IERSConventions.IERS_2010, false);
        Assertions.assertNull(eme2000.getPeer());
        Assertions.assertNull(itrf.getPeer());

        // with caching activated, tB should be a reference to the same transform as tA
        // without being recomputed
        final int cachesize = 20;
        eme2000.setPeerCaching(itrf, cachesize);
        Assertions.assertSame(itrf,    eme2000.getPeer());
        Assertions.assertSame(eme2000, itrf.getPeer());

        final FieldAbsoluteDate<T> t0 = new FieldAbsoluteDate<>(field, 2004, 5, 7, 17, 42, 37.5, TimeScalesFactory.getUTC());
        final FieldStaticTransform<T> tA = eme2000.getStaticTransformTo(itrf, t0);
        final FieldStaticTransform<T> tB = eme2000.getStaticTransformTo(itrf, t0);
        final FieldStaticTransform<T> backAndForth = FieldStaticTransform.compose(t0, tA, tB.getInverse());
        Assertions.assertSame(tA, tB);
        Assertions.assertEquals(0.0, backAndForth.getRotation().getAngle().getReal(), 1.0e-20);
        Assertions.assertEquals(0.0, backAndForth.getTranslation().getNorm().getReal(), 1.0e-20);

        final FieldStaticTransform<T>[] direct  = new FieldStaticTransform[cachesize];
        final FieldStaticTransform<T>[] inverse = new FieldStaticTransform[cachesize];
        for ( int i = 0; i < cachesize; i++ ) {
            direct[i]  = eme2000.getStaticTransformTo(itrf, t0.shiftedBy(i));
            inverse[i] = itrf.getStaticTransformTo(eme2000, t0.shiftedBy(i));
        }

        // with caching activated, we should not recompute any transform, just retrieve existing ones
        for (int i = 0; i < 10000; ++i) {
            Assertions.assertSame(direct[i % cachesize], eme2000.getStaticTransformTo(itrf, t0.shiftedBy(i % cachesize)));
            Assertions.assertSame(inverse[i % cachesize], itrf.getStaticTransformTo(eme2000, t0.shiftedBy(i % cachesize)));
        }

    }

    private static <T extends CalculusFieldElement<T>> void compareFieldVectorWithMargin(final FieldVector3D<T> expectedVector,
                                                                                         final FieldVector3D<T> actualVector) {
        Assertions.assertEquals(0., actualVector.toVector3D().subtract(expectedVector.toVector3D()).getNorm(),
                1e-12);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
