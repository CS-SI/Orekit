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
package org.orekit.orbits;

import static org.orekit.OrekitMatchers.relativelyCloseTo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

import org.hamcrest.MatcherAssert;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.analysis.differentiation.UnivariateDifferentiableFunction;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.FieldMatrixPreservingVisitor;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


public class FieldCartesianOrbitTest {


    // Body mu
    private double mu;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        // Body mu
        mu = 3.9860047e14;
    }

    @Test
    public void testCartesianToCartesian()
        throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        doTestCartesianToCartesian(Binary64Field.getInstance());
    }

    @Test
    public void testCartesianToEquinoctial() {
        doTestCartesianToEquinoctial(Binary64Field.getInstance());
    }

    @Test
    public void testCartesianToKeplerian() {
        doTestCartesianToKeplerian(Binary64Field.getInstance());
    }

    @Test
    public void testPositionVelocityNorms() {
        doTestPositionVelocityNorms(Binary64Field.getInstance());
    }

    @Test
    public void testGeometry() {
        doTestGeometry(Binary64Field.getInstance());
    }

    @Test
    public void testHyperbola1() {
        doTestHyperbola1(Binary64Field.getInstance());
    }

    @Test
    public void testHyperbola2() {
        doTestHyperbola2(Binary64Field.getInstance());
    }

    @Test
    public void testNumericalIssue25() {
        doTestNumericalIssue25(Binary64Field.getInstance());
    }

    @Test
    public void testDerivativesConversionSymmetry() {
        doTestDerivativesConversionSymmetry(Binary64Field.getInstance());
    }

    @Test
    public void testDerivativesConversionSymmetryHyperbolic() {
        doTestDerivativesConversionSymmetryHyperbolic(Binary64Field.getInstance());
    }

    @Test
    public void testShiftElliptic() {
        doTestShiftElliptic(Binary64Field.getInstance());
    }

    @Test
    public void testShiftCircular() {
        doTestShiftCircular(Binary64Field.getInstance());
    }

    @Test
    public void testShiftEquinoctial() {
        doTestShiftEquinoctial(Binary64Field.getInstance());
    }

    @Test
    public void testShiftHyperbolic() {
        doTestShiftHyperbolic(Binary64Field.getInstance());
    }

    @Test
    public void testNumericalIssue135() {
        doTestNumericalIssue135(Binary64Field.getInstance());
    }

    @Test
    public void testNumericalIssue1015() {
        doTestNumericalIssue1015(Binary64Field.getInstance());
    }

    @Test
    public void testJacobianReference() {
        doTestJacobianReference(Binary64Field.getInstance());
    }

    @Test
    public void testErr1(){
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            doTestErr1(Binary64Field.getInstance());
        });
    }

    @Test
    public void testToOrbitWithoutDerivatives() {
        doTestToOrbitWithoutDerivatives(Binary64Field.getInstance());
    }

    @Test
    public void testToOrbitWithDerivatives() {
        doTestToOrbitWithDerivatives(Binary64Field.getInstance());
    }

    @Test
    public void testToString() {
        doTestToString(Binary64Field.getInstance());
    }

    @Test
    public void testNonKeplerianDerivatives() {
        doTestNonKeplerianDerivatives(Binary64Field.getInstance());
    }

    @Test
    public void testEquatorialRetrograde() {
        doTestEquatorialRetrograde(Binary64Field.getInstance());
    }

    @Test
    public void testCopyNonKeplerianAcceleration() {
        doTestCopyNonKeplerianAcceleration(Binary64Field.getInstance());
    }

    @Test
    public void testNormalize() {
        doTestNormalize(Binary64Field.getInstance());
    }

    @Test
    public void testIssue1139() {
        doTestIssue1139(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestCartesianToCartesian(Field<T> field)
                    throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        FieldVector3D<T> position = new FieldVector3D<>(zero.add(-29536113.0), zero.add(30329259.0), zero.add(-100125.0));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-2194.0), zero.add(-2141.0), zero.add(-8.0));
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<>( position, velocity);
        double mu = 3.9860047e14;

        FieldCartesianOrbit<T> p = new FieldCartesianOrbit<>(FieldPVCoordinates, FramesFactory.getEME2000(), date, zero.add(mu));

        Assertions.assertEquals(p.getPosition().getX().getReal(), FieldPVCoordinates.getPosition().getX().getReal(), Utils.epsilonTest * FastMath.abs(FieldPVCoordinates.getPosition().getX().getReal()));
        Assertions.assertEquals(p.getPosition().getY().getReal(), FieldPVCoordinates.getPosition().getY().getReal(), Utils.epsilonTest * FastMath.abs(FieldPVCoordinates.getPosition().getY().getReal()));
        Assertions.assertEquals(p.getPosition().getZ().getReal(), FieldPVCoordinates.getPosition().getZ().getReal(), Utils.epsilonTest * FastMath.abs(FieldPVCoordinates.getPosition().getZ().getReal()));
        Assertions.assertEquals(p.getPVCoordinates().getVelocity().getX().getReal(), FieldPVCoordinates.getVelocity().getX().getReal(), Utils.epsilonTest * FastMath.abs(FieldPVCoordinates.getVelocity().getX().getReal()));
        Assertions.assertEquals(p.getPVCoordinates().getVelocity().getY().getReal(), FieldPVCoordinates.getVelocity().getY().getReal(), Utils.epsilonTest * FastMath.abs(FieldPVCoordinates.getVelocity().getY().getReal()));
        Assertions.assertEquals(p.getPVCoordinates().getVelocity().getZ().getReal(), FieldPVCoordinates.getVelocity().getZ().getReal(), Utils.epsilonTest * FastMath.abs(FieldPVCoordinates.getVelocity().getZ().getReal()));

        Method initPV = FieldCartesianOrbit.class.getDeclaredMethod("initPVCoordinates", new Class[0]);
        initPV.setAccessible(true);
        Assertions.assertSame(p.getPVCoordinates(), initPV.invoke(p, new Object[0]));

    }

    private <T extends CalculusFieldElement<T>> void doTestCartesianToEquinoctial(Field<T> field) {
        T zero = field.getZero();

        FieldVector3D<T> position = new FieldVector3D<>(zero.add(-29536113.0), zero.add(30329259.0), zero.add(-100125.0));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-2194.0), zero.add(-2141.0), zero.add(-8.0));
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<>( position, velocity);

        FieldCartesianOrbit<T> p = new FieldCartesianOrbit<>(FieldPVCoordinates, FramesFactory.getEME2000(),
                                                             FieldAbsoluteDate.getJ2000Epoch(field), zero.add(mu));

        Assertions.assertEquals(42255170.0028257,  p.getA().getReal(), Utils.epsilonTest * p.getA().getReal());
        Assertions.assertEquals(0.592732497856475e-03,  p.getEquinoctialEx().getReal(), Utils.epsilonE * FastMath.abs(p.getE().getReal()));
        Assertions.assertEquals(-0.206274396964359e-02, p.getEquinoctialEy().getReal(), Utils.epsilonE * FastMath.abs(p.getE().getReal()));
        Assertions.assertEquals(FastMath.sqrt(FastMath.pow(0.592732497856475e-03, 2)+FastMath.pow(-0.206274396964359e-02, 2)), p.getE().getReal(), Utils.epsilonAngle * FastMath.abs(p.getE().getReal()));
        Assertions.assertEquals(MathUtils.normalizeAngle(2*FastMath.asin(FastMath.sqrt((FastMath.pow(0.128021863908325e-03, 2)+FastMath.pow(-0.352136186881817e-02, 2))/4.)), p.getI().getReal()), p.getI().getReal(), Utils.epsilonAngle * FastMath.abs(p.getI().getReal()));
        Assertions.assertEquals(MathUtils.normalizeAngle(0.234498139679291e+01, p.getLM().getReal()), p.getLM().getReal(), Utils.epsilonAngle * FastMath.abs(p.getLM().getReal()));

        // trigger a specific path in copy constructor
        FieldCartesianOrbit<T> q = new FieldCartesianOrbit<>(p);

        Assertions.assertEquals(42255170.0028257,  q.getA().getReal(), Utils.epsilonTest * q.getA().getReal());
        Assertions.assertEquals(0.592732497856475e-03,  q.getEquinoctialEx().getReal(), Utils.epsilonE * FastMath.abs(q.getE().getReal()));
        Assertions.assertEquals(-0.206274396964359e-02, q.getEquinoctialEy().getReal(), Utils.epsilonE * FastMath.abs(q.getE().getReal()));
        Assertions.assertEquals(FastMath.sqrt(FastMath.pow(0.592732497856475e-03, 2)+FastMath.pow(-0.206274396964359e-02, 2)), q.getE().getReal(), Utils.epsilonAngle * FastMath.abs(q.getE().getReal()));
        Assertions.assertEquals(MathUtils.normalizeAngle(2*FastMath.asin(FastMath.sqrt((FastMath.pow(0.128021863908325e-03, 2)+FastMath.pow(-0.352136186881817e-02, 2))/4.)), q.getI().getReal()), q.getI().getReal(), Utils.epsilonAngle * FastMath.abs(q.getI().getReal()));
        Assertions.assertEquals(MathUtils.normalizeAngle(0.234498139679291e+01, q.getLM().getReal()), q.getLM().getReal(), Utils.epsilonAngle * FastMath.abs(q.getLM().getReal()));

        Assertions.assertNull(q.getADot());
        Assertions.assertNull(q.getEquinoctialExDot());
        Assertions.assertNull(q.getEquinoctialEyDot());
        Assertions.assertNull(q.getHxDot());
        Assertions.assertNull(q.getHyDot());
        Assertions.assertNull(q.getLvDot());
        Assertions.assertNull(q.getEDot());
        Assertions.assertNull(q.getIDot());

    }

    private <T extends CalculusFieldElement<T>> void doTestCartesianToKeplerian(Field<T> field){
        T zero = field.getZero();

        FieldVector3D<T> position = new FieldVector3D<>(zero.add(-26655470.0), zero.add(29881667.0), zero.add(-113657.0));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-1125.0), zero.add(-1122.0), zero.add(195.0));
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<>( position, velocity);
        double mu = 3.9860047e14;

        FieldCartesianOrbit<T> p = new FieldCartesianOrbit<>(FieldPVCoordinates, FramesFactory.getEME2000(),
                                                             FieldAbsoluteDate.getJ2000Epoch(field), zero.add(mu));
        FieldKeplerianOrbit<T> kep = new FieldKeplerianOrbit<>(p);

        Assertions.assertEquals(22979265.3030773,  p.getA().getReal(), Utils.epsilonTest  * p.getA().getReal());
        Assertions.assertEquals(0.743502611664700, p.getE().getReal(), Utils.epsilonE     * FastMath.abs(p.getE().getReal()));
        Assertions.assertEquals(0.122182096220906, p.getI().getReal(), Utils.epsilonAngle * FastMath.abs(p.getI().getReal()));
        T pa = kep.getPerigeeArgument();
        Assertions.assertEquals(MathUtils.normalizeAngle(3.09909041016672, pa.getReal()), pa.getReal(),
                     Utils.epsilonAngle * FastMath.abs(pa.getReal()));
        T raan = kep.getRightAscensionOfAscendingNode();
        Assertions.assertEquals(MathUtils.normalizeAngle(2.32231010979999, raan.getReal()), raan.getReal(),
                     Utils.epsilonAngle * FastMath.abs(raan.getReal()));
        T m = kep.getMeanAnomaly();
        Assertions.assertEquals(MathUtils.normalizeAngle(3.22888977629034, m.getReal()), m.getReal(),
                     Utils.epsilonAngle * FastMath.abs(FastMath.abs(m.getReal())));
    }

    private <T extends CalculusFieldElement<T>> void doTestPositionVelocityNorms(Field<T> field){ T zero=field.getZero();T one=field.getOne(); FieldAbsoluteDate<T> date=new FieldAbsoluteDate<>(field);

        FieldVector3D<T> position = new FieldVector3D<>(zero.add(-29536113.0), zero.add(30329259.0), zero.add(-100125.0));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-2194.0), zero.add(-2141.0), zero.add(-8.0));
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<>( position, velocity);

        FieldCartesianOrbit<T> p = new FieldCartesianOrbit<>(FieldPVCoordinates, FramesFactory.getEME2000(), date, zero.add(mu));

        T e       = p.getE();
        T v       = new FieldKeplerianOrbit<>(p).getTrueAnomaly();
        T ksi     = e.multiply(v.cos()).add(1);
        T nu      = e.multiply(v.sin());
        T epsilon = one.subtract(e).multiply(e.add(1)).sqrt();

        T a  = p.getA();
        T na = a.reciprocal().multiply(mu).sqrt();

        // validation of: r = a .(1 - e2) / (1 + e.cos(v))
        Assertions.assertEquals(a.getReal() * epsilon.getReal() * epsilon.getReal() / ksi.getReal(),
                     p.getPosition().getNorm().getReal(),
                     Utils.epsilonTest * FastMath.abs(p.getPosition().getNorm().getReal()));

        // validation of: V = sqrt(mu.(1+2e.cos(v)+e2)/a.(1-e2) )
        Assertions.assertEquals(na.getReal() * FastMath.sqrt(ksi.getReal() * ksi.getReal() + nu .getReal()* nu.getReal()) / epsilon.getReal(),
                     p.getPVCoordinates().getVelocity().getNorm().getReal(),
                     Utils.epsilonTest * FastMath.abs(p.getPVCoordinates().getVelocity().getNorm().getReal()));

    }

    private <T extends CalculusFieldElement<T>> void doTestGeometry(Field<T> field) {
        T zero = field.getZero();

        FieldVector3D<T> position = new FieldVector3D<>(zero.add(-29536113.0), zero.add(30329259.0), zero.add(-100125.0));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-2194.0), zero.add(-2141.0), zero.add(-8.0));
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<>( position, velocity);

        FieldVector3D<T> momentum = FieldPVCoordinates.getMomentum().normalize();

        FieldEquinoctialOrbit<T> p = new FieldEquinoctialOrbit<>(FieldPVCoordinates, FramesFactory.getEME2000(),
                                                                 FieldAbsoluteDate.getJ2000Epoch(field), zero.add(mu));

        T apogeeRadius  = p.getA().multiply( p.getE().add(1.0));
        T perigeeRadius = p.getA().multiply( p.getE().negate().add(1.0));

        for (T lv = zero; lv.getReal() <= 2 * FastMath.PI; lv = lv.add(2 * FastMath.PI/100.)) {
            p = new FieldEquinoctialOrbit<>(p.getA(), p.getEquinoctialEx(), p.getEquinoctialEy(),
                                            p.getHx(), p.getHy(), lv, PositionAngleType.TRUE, p.getFrame(),
                                            FieldAbsoluteDate.getJ2000Epoch(field), zero.add(mu));
            position = p.getPosition();

            // test if the norm of the position is in the range [perigee radius, apogee radius]
            // Warning: these tests are without absolute value by choice
            Assertions.assertTrue((position.getNorm().getReal() - apogeeRadius.getReal())  <= (  apogeeRadius.getReal() * Utils.epsilonTest));
            Assertions.assertTrue((position.getNorm().getReal() - perigeeRadius.getReal()) >= (- perigeeRadius.getReal() * Utils.epsilonTest));
            // Assertions.assertTrue(position.getNorm() <= apogeeRadius);
            // Assertions.assertTrue(position.getNorm() >= perigeeRadius);

            position= position.normalize();
            velocity = p.getPVCoordinates().getVelocity().normalize();

            // at this stage of computation, all the vectors (position, velocity and momemtum) are normalized here

            // test of orthogonality between position and momentum
            Assertions.assertTrue(FastMath.abs(FieldVector3D.dotProduct(position, momentum).getReal()) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assertions.assertTrue(FastMath.abs(FieldVector3D.dotProduct(velocity, momentum).getReal()) < Utils.epsilonTest);
        }
    }

    private <T extends CalculusFieldElement<T>> void doTestHyperbola1(final Field<T> field) {
        T zero = field.getZero();
        FieldCartesianOrbit<T> orbit = new FieldCartesianOrbit<>(new FieldKeplerianOrbit<>(zero.add(-10000000.0), zero.add(2.5), zero.add(0.3),
                                                                                           zero, zero, zero,
                                                                                           PositionAngleType.TRUE,
                                                                                           FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field),
                                                                                           zero.add(mu)));
        FieldVector3D<T> perigeeP  = orbit.getPosition();
        FieldVector3D<T> u = perigeeP.normalize();
        FieldVector3D<T> focus1 = new FieldVector3D<>(zero, zero, zero);
        FieldVector3D<T> focus2 = new FieldVector3D<>(orbit.getA().multiply(-2).multiply(orbit.getE()), u);
        for (T dt = zero.add(-5000); dt.getReal() < 5000; dt = dt.add(60)) {
            FieldPVCoordinates<T> pv = orbit.shiftedBy(dt).getPVCoordinates();
            T d1 = FieldVector3D.distance(pv.getPosition(), focus1);
            T d2 = FieldVector3D.distance(pv.getPosition(), focus2);
            Assertions.assertEquals(orbit.getA().multiply(-2).getReal(), d1.subtract(d2).abs().getReal(), 1.0e-6);
            FieldCartesianOrbit<T> rebuilt =
                            new FieldCartesianOrbit<>(pv, orbit.getFrame(), orbit.getDate().shiftedBy(dt), zero.add(mu));
            Assertions.assertEquals(-10000000.0, rebuilt.getA().getReal(), 1.0e-6);
            Assertions.assertEquals(2.5, rebuilt.getE().getReal(), 1.0e-13);
        }
    }

    private <T extends CalculusFieldElement<T>> void doTestHyperbola2(final Field<T> field) {
        T zero = field.getZero();
        FieldCartesianOrbit<T> orbit = new FieldCartesianOrbit<>(new FieldKeplerianOrbit<>(zero.add(-10000000.0), zero.add(1.2), zero.add(0.3),
                                                                                           zero, zero, zero.add(-1.75),
                                                                                           PositionAngleType.MEAN,
                                                                                           FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field),
                                                                                           zero.add(mu)));
        FieldVector3D<T> perigeeP  = new FieldKeplerianOrbit<>(zero.add(-10000000.0), zero.add(1.2), zero.add(0.3),
                                                               zero, zero, zero,
                                                               PositionAngleType.TRUE,
                                                               orbit.getFrame(), orbit.getDate(), orbit.getMu()).getPosition();
        FieldVector3D<T> u = perigeeP.normalize();
        FieldVector3D<T> focus1 = new FieldVector3D<>(zero, zero, zero);
        FieldVector3D<T> focus2 = new FieldVector3D<>(orbit.getA().multiply(-2).multiply(orbit.getE()), u);
        for (T dt = zero.add(-5000); dt.getReal() < 5000; dt = dt.add(60)) {
            FieldPVCoordinates<T> pv = orbit.shiftedBy(dt).getPVCoordinates();
            T d1 = FieldVector3D.distance(pv.getPosition(), focus1);
            T d2 = FieldVector3D.distance(pv.getPosition(), focus2);
            Assertions.assertEquals(orbit.getA().multiply(-2).getReal(), d1.subtract(d2).abs().getReal(), 1.0e-6);
            FieldCartesianOrbit<T> rebuilt =
                            new FieldCartesianOrbit<>(pv, orbit.getFrame(), orbit.getDate().shiftedBy(dt), zero.add(mu));
            Assertions.assertEquals(-10000000.0, rebuilt.getA().getReal(), 1.0e-6);
            Assertions.assertEquals(1.2, rebuilt.getE().getReal(), 1.0e-13);
        }
    }

    private <T extends CalculusFieldElement<T>> void doTestNumericalIssue25(Field<T> field) {
        T zero = field.getZero();
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(3782116.14107698), zero.add(416663.11924914), zero.add(5875541.62103057));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-6349.7848910501), zero.add(288.4061811651), zero.add(4066.9366759691));
        FieldCartesianOrbit<T> orbit = new FieldCartesianOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                                 FramesFactory.getEME2000(),
                                                                 new FieldAbsoluteDate<>(field, "2004-01-01T23:00:00.000",
                                                                                         TimeScalesFactory.getUTC()),
                                                                 zero.add(3.986004415E14));
        Assertions.assertEquals(0.0, orbit.getE().getReal(), 2.0e-14);
    }

    private <T extends CalculusFieldElement<T>> void doTestDerivativesConversionSymmetry(Field<T> field) {
        T zero = field.getZero();
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, "2003-05-01T00:01:20.000", TimeScalesFactory.getUTC());
        FieldVector3D<T> position     = new FieldVector3D<>(zero.add(6893443.400234382),
                                                            zero.add(1886406.1073757345),
                                                            zero.add(-589265.1150359757));
        FieldVector3D<T> velocity     = new FieldVector3D<>(zero.add(-281.1261461082365),
                                                            zero.add(-1231.6165642450928),
                                                            zero.add(-7348.756363469432));
        FieldVector3D<T> acceleration = new FieldVector3D<>(zero.add(-7.460341170581685),
                                                            zero.add(-2.0415957334584527),
                                                            zero.add(0.6393322823627762));
        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<>( position, velocity, acceleration);
        FieldCartesianOrbit<T> orbit = new FieldCartesianOrbit<>(pvCoordinates, FramesFactory.getEME2000(),
                                                                 date, zero.add(Constants.EIGEN5C_EARTH_MU));
        Assertions.assertTrue(orbit.hasDerivatives());
        T r2 = position.getNormSq();
        T r  = r2.sqrt();
        FieldVector3D<T> keplerianAcceleration = new FieldVector3D<>(r.multiply(r2).reciprocal().multiply(orbit.getMu().negate()),
                                                                     position);
        Assertions.assertEquals(0.0101, FieldVector3D.distance(keplerianAcceleration, acceleration).getReal(), 1.0e-4);

        for (OrbitType type : OrbitType.values()) {
            FieldOrbit<T> converted = type.convertType(orbit);
            Assertions.assertTrue(converted.hasDerivatives());
            FieldCartesianOrbit<T> rebuilt = (FieldCartesianOrbit<T>) OrbitType.CARTESIAN.convertType(converted);
            Assertions.assertTrue(rebuilt.hasDerivatives());
            Assertions.assertEquals(0, FieldVector3D.distance(rebuilt.getPosition(),     position).getReal(),     2.0e-9);
            Assertions.assertEquals(0, FieldVector3D.distance(rebuilt.getPVCoordinates().getVelocity(),     velocity).getReal(),     7.0e-12);
            Assertions.assertEquals(0, FieldVector3D.distance(rebuilt.getPVCoordinates().getAcceleration(), acceleration).getReal(), 4.9e-15);
        }

    }

    private <T extends CalculusFieldElement<T>> void doTestDerivativesConversionSymmetryHyperbolic(Field<T> field) {
        T zero = field.getZero();
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, "2003-05-01T00:00:20.000", TimeScalesFactory.getUTC());
        FieldVector3D<T> position     = new FieldVector3D<>(zero.add(224267911.905821),
                                                            zero.add(290251613.109399),
                                                            zero.add(45534292.777492));
        FieldVector3D<T> velocity     = new FieldVector3D<>(zero.add(-1494.068165293),
                                                            zero.add(1124.771027677),
                                                            zero.add(526.915286134));
        FieldVector3D<T> acceleration = new FieldVector3D<>(zero.add(-0.001295920501),
                                                            zero.add(-0.002233045187),
                                                            zero.add(-0.000349906292));
        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<>( position, velocity, acceleration);
        FieldCartesianOrbit<T> orbit = new FieldCartesianOrbit<>(pvCoordinates, FramesFactory.getEME2000(),
                                                                 date, zero.add(Constants.EIGEN5C_EARTH_MU));
        Assertions.assertTrue(orbit.hasDerivatives());
        T r2 = position.getNormSq();
        T r  = r2.sqrt();
        FieldVector3D<T> keplerianAcceleration = new FieldVector3D<>(r.multiply(r2).reciprocal().multiply(orbit.getMu().negate()),
                                                                     position);
        Assertions.assertEquals(4.78e-4, FieldVector3D.distance(keplerianAcceleration, acceleration).getReal(), 1.0e-6);

        OrbitType type = OrbitType.KEPLERIAN;
        FieldOrbit<T> converted = type.convertType(orbit);
        Assertions.assertTrue(converted.hasDerivatives());
        FieldCartesianOrbit<T> rebuilt = (FieldCartesianOrbit<T>) OrbitType.CARTESIAN.convertType(converted);
        Assertions.assertTrue(rebuilt.hasDerivatives());
        Assertions.assertEquals(0, FieldVector3D.distance(rebuilt.getPosition(),     position).getReal(),     1.0e-15);
        Assertions.assertEquals(0, FieldVector3D.distance(rebuilt.getPVCoordinates().getVelocity(),     velocity).getReal(),     1.0e-15);
        Assertions.assertEquals(0, FieldVector3D.distance(rebuilt.getPVCoordinates().getAcceleration(), acceleration).getReal(), 1.0e-15);

    }

    private <T extends CalculusFieldElement<T>> void doTestShiftElliptic(Field<T> field) {
        T zero = field.getZero();
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(-29536113.0), zero.add(30329259.0), zero.add(-100125.0));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-2194.0), zero.add(-2141.0), zero.add(-8.0));
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<>( position, velocity);
        FieldCartesianOrbit<T> orbit = new FieldCartesianOrbit<>(FieldPVCoordinates, FramesFactory.getEME2000(),
                                                                 FieldAbsoluteDate.getJ2000Epoch(field), zero.add(mu));
        testShift(orbit, new FieldKeplerianOrbit<>(orbit), 1e-13);
    }

    private <T extends CalculusFieldElement<T>> void doTestShiftCircular(Field<T> field) {
        T zero = field.getZero();
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(-29536113.0), zero.add(30329259.0), zero.add(-100125.0));
        FieldVector3D<T> velocity = new FieldVector3D<>(position.getNorm().reciprocal().multiply(mu).sqrt(), position.orthogonal());
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<>( position, velocity);
        FieldCartesianOrbit<T> orbit = new FieldCartesianOrbit<>(FieldPVCoordinates, FramesFactory.getEME2000(),
                                                                 FieldAbsoluteDate.getJ2000Epoch(field), zero.add(mu));
        testShift(orbit, new FieldCircularOrbit<>(orbit), 2.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void doTestShiftEquinoctial(Field<T> field) {
        T zero = field.getZero();
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(-29536113.0), zero.add(30329259.0), zero.add(-100125.0));
        FieldVector3D<T> velocity = new FieldVector3D<>(position.getNorm().reciprocal().multiply(mu).sqrt(), position.orthogonal());
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<>( position, velocity);
        FieldCartesianOrbit<T> orbit = new FieldCartesianOrbit<>(FieldPVCoordinates, FramesFactory.getEME2000(),
                                                                 FieldAbsoluteDate.getJ2000Epoch(field), zero.add(mu));
        testShift(orbit, new FieldEquinoctialOrbit<>(orbit), 5.0e-14);
    }

    private <T extends CalculusFieldElement<T>> void doTestShiftHyperbolic(Field<T> field) {
        T zero = field.getZero();
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(-29536113.0), zero.add(30329259.0), zero.add(-100125.0));
        FieldVector3D<T> velocity = new FieldVector3D<>(position.getNorm().reciprocal().multiply(mu).sqrt().multiply(3.0), position.orthogonal());
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<>(position, velocity);
        FieldCartesianOrbit<T> orbit = new FieldCartesianOrbit<>(FieldPVCoordinates, FramesFactory.getEME2000(),
                                                                 FieldAbsoluteDate.getJ2000Epoch(field), zero.add(mu));
        testShift(orbit, new FieldKeplerianOrbit<>(orbit), 2.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void doTestNumericalIssue135(Field<T> field) {
        T zero = field.getZero();
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(-6.7884943832e7), zero.add(-2.1423006112e7), zero.add(-3.1603915377e7));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-4732.55), zero.add(-2472.086), zero.add(-3022.177));
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<>(position, velocity);
        FieldCartesianOrbit<T> orbit = new FieldCartesianOrbit<>(FieldPVCoordinates, FramesFactory.getEME2000(),
                                                                 FieldAbsoluteDate.getJ2000Epoch(field),
                                                                 zero.add(324858598826460.));
        testShift(orbit, new FieldKeplerianOrbit<>(orbit), 6.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void doTestNumericalIssue1015(Field<T> field) {
        T zero = field.getZero();
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(-1466739.735988), zero.add(1586390.713569), zero.add(6812901.677773));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-9532.812), zero.add(-4321.894), zero.add(-1409.018));
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<>(position, velocity);
        FieldCartesianOrbit<T> orbit = new FieldCartesianOrbit<>(FieldPVCoordinates, FramesFactory.getEME2000(),
                                                                 FieldAbsoluteDate.getJ2000Epoch(field),
                                                                 zero.add(3.986004415E14));
        testShift(orbit, new FieldKeplerianOrbit<>(orbit), 1.0e-10);
    }

    private <T extends CalculusFieldElement<T>> void testShift(FieldCartesianOrbit<T> tested, FieldOrbit<T> reference, double threshold) {
        Field<T> field = tested.getA().getField();
        T zero = field.getZero();
        for (T dt = zero.add(- 1000); dt.getReal() < 1000; dt = dt.add(10.0)) {

            FieldPVCoordinates<T> pvTested    = tested.shiftedBy(dt).getPVCoordinates();
            FieldVector3D<T>      pTested     = pvTested.getPosition();
            FieldVector3D<T>      vTested     = pvTested.getVelocity();

            FieldPVCoordinates<T> pvReference = reference.shiftedBy(dt).getPVCoordinates();
            FieldVector3D<T>      pReference  = pvReference.getPosition();
            FieldVector3D<T>      vReference  = pvReference.getVelocity();
            Assertions.assertEquals(0.0, pTested.subtract(pReference).getNorm().getReal(), threshold * pReference.getNorm().getReal());
            Assertions.assertEquals(0.0, vTested.subtract(vReference).getNorm().getReal(), threshold * vReference.getNorm().getReal());

        }
    }

    private <T extends CalculusFieldElement<T> >void doTestErr1(Field<T> field) throws IllegalArgumentException {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(-26655470.0), zero.add(29881667.0), zero.add(-113657.0));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-1125.0), zero.add(-1122.0), zero.add(195.0));
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<>( position, velocity);
        double mu = 3.9860047e14;
        new FieldCartesianOrbit<>(FieldPVCoordinates,
                                  new Frame(FramesFactory.getEME2000(), Transform.IDENTITY, "non-inertial", false),
                                  date, zero.add(mu));
    }

    private <T extends CalculusFieldElement<T>> void doTestToOrbitWithoutDerivatives(Field<T> field) {
        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));
        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<>(position, velocity);
        FieldCartesianOrbit<T>  fieldOrbit = new FieldCartesianOrbit<>(pvCoordinates, FramesFactory.getEME2000(), date, zero.add(mu));
        CartesianOrbit orbit = fieldOrbit.toOrbit();
        Assertions.assertFalse(orbit.hasDerivatives());
        MatcherAssert.assertThat(orbit.getPosition().getX(), relativelyCloseTo(fieldOrbit.getPVCoordinates().getPosition().getX().getReal(), 0));
        MatcherAssert.assertThat(orbit.getPosition().getY(), relativelyCloseTo(fieldOrbit.getPVCoordinates().getPosition().getY().getReal(), 0));
        MatcherAssert.assertThat(orbit.getPosition().getZ(), relativelyCloseTo(fieldOrbit.getPVCoordinates().getPosition().getZ().getReal(), 0));
        MatcherAssert.assertThat(orbit.getPVCoordinates().getVelocity().getX(), relativelyCloseTo(fieldOrbit.getPVCoordinates().getVelocity().getX().getReal(), 0));
        MatcherAssert.assertThat(orbit.getPVCoordinates().getVelocity().getY(), relativelyCloseTo(fieldOrbit.getPVCoordinates().getVelocity().getY().getReal(), 0));
        MatcherAssert.assertThat(orbit.getPVCoordinates().getVelocity().getZ(), relativelyCloseTo(fieldOrbit.getPVCoordinates().getVelocity().getZ().getReal(), 0));
        MatcherAssert.assertThat(orbit.getPVCoordinates().getAcceleration().getX(), relativelyCloseTo(fieldOrbit.getPVCoordinates().getAcceleration().getX().getReal(), 0));
        MatcherAssert.assertThat(orbit.getPVCoordinates().getAcceleration().getY(), relativelyCloseTo(fieldOrbit.getPVCoordinates().getAcceleration().getY().getReal(), 0));
        MatcherAssert.assertThat(orbit.getPVCoordinates().getAcceleration().getZ(), relativelyCloseTo(fieldOrbit.getPVCoordinates().getAcceleration().getZ().getReal(), 0));

    }

    private <T extends CalculusFieldElement<T>> void doTestToOrbitWithDerivatives(Field<T> field) {
        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));
        T r2 = position.getNormSq();
        T r = r2.sqrt();
        FieldVector3D<T> acceleration = new FieldVector3D<>(r.multiply(r2).reciprocal().multiply(-mu).add(0.1), position);
        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<>(position, velocity,acceleration);
        FieldCartesianOrbit<T>  fieldOrbit = new FieldCartesianOrbit<>(pvCoordinates, FramesFactory.getEME2000(), date, zero.add(mu));
        CartesianOrbit orbit = fieldOrbit.toOrbit();
        Assertions.assertTrue(orbit.hasDerivatives());
        MatcherAssert.assertThat(orbit.getPosition().getX(), relativelyCloseTo(fieldOrbit.getPVCoordinates().getPosition().getX().getReal(), 0));
        MatcherAssert.assertThat(orbit.getPosition().getY(), relativelyCloseTo(fieldOrbit.getPVCoordinates().getPosition().getY().getReal(), 0));
        MatcherAssert.assertThat(orbit.getPosition().getZ(), relativelyCloseTo(fieldOrbit.getPVCoordinates().getPosition().getZ().getReal(), 0));
        MatcherAssert.assertThat(orbit.getPVCoordinates().getVelocity().getX(), relativelyCloseTo(fieldOrbit.getPVCoordinates().getVelocity().getX().getReal(), 0));
        MatcherAssert.assertThat(orbit.getPVCoordinates().getVelocity().getY(), relativelyCloseTo(fieldOrbit.getPVCoordinates().getVelocity().getY().getReal(), 0));
        MatcherAssert.assertThat(orbit.getPVCoordinates().getVelocity().getZ(), relativelyCloseTo(fieldOrbit.getPVCoordinates().getVelocity().getZ().getReal(), 0));
        MatcherAssert.assertThat(orbit.getPVCoordinates().getAcceleration().getX(), relativelyCloseTo(fieldOrbit.getPVCoordinates().getAcceleration().getX().getReal(), 0));
        MatcherAssert.assertThat(orbit.getPVCoordinates().getAcceleration().getY(), relativelyCloseTo(fieldOrbit.getPVCoordinates().getAcceleration().getY().getReal(), 0));
        MatcherAssert.assertThat(orbit.getPVCoordinates().getAcceleration().getZ(), relativelyCloseTo(fieldOrbit.getPVCoordinates().getAcceleration().getZ().getReal(), 0));
    }

    private <T extends CalculusFieldElement<T>> void doTestJacobianReference(Field<T> field) {
        T zero = field.getZero();
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(-29536113.0), zero.add(30329259.0), zero.add(-100125.0));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-2194.0), zero.add(-2141.0), zero.add(-8.0));
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<>( position, velocity);
        FieldCartesianOrbit<T> orbit = new FieldCartesianOrbit<>(FieldPVCoordinates, FramesFactory.getEME2000(),
                                                                 FieldAbsoluteDate.getJ2000Epoch(field), zero.add(mu));

        T[][] jacobian = MathArrays.buildArray(field, 6, 6);
        orbit.getJacobianWrtCartesian(PositionAngleType.MEAN, jacobian);

        for (int i = 0; i < jacobian.length; i++) {
            T[] row    = jacobian[i];
            for (int j = 0; j < row.length; j++) {
                Assertions.assertEquals((i == j) ? 1 : 0, row[j].getReal(), 1.0e-15);
            }
        }

        T[][] invJacobian = MathArrays.buildArray(field, 6, 6);
        orbit.getJacobianWrtParameters(PositionAngleType.MEAN, invJacobian);
        MatrixUtils.createFieldMatrix(jacobian).
                        multiply(MatrixUtils.createFieldMatrix(invJacobian)).
        walkInRowOrder(new FieldMatrixPreservingVisitor<T>() {
            public void start(int rows, int columns,
                              int startRow, int endRow, int startColumn, int endColumn) {
            }

            public void visit(int row, int column, T value) {
                Assertions.assertEquals(row == column ? 1.0 : 0.0, value.getReal(), 1.0e-15);
            }

            public T end() {
                return null;
            }
        });

    }

    private <T extends CalculusFieldElement<T>> void doTestNonKeplerianDerivatives(Field<T> field) {
        final T zero = field.getZero();
        final FieldAbsoluteDate<T> date         = new FieldAbsoluteDate<>(field, "2003-05-01T00:00:20.000", TimeScalesFactory.getUTC());
        final FieldVector3D<T>     position     = new FieldVector3D<>(field.getZero().add(6896874.444705),  field.getZero().add(1956581.072644),  field.getZero().add(-147476.245054));
        final FieldVector3D<T>     velocity     = new FieldVector3D<>(field.getZero().add(166.816407662), field.getZero().add(-1106.783301861), field.getZero().add(-7372.745712770));
        final FieldVector3D <T>    acceleration = new FieldVector3D<>(field.getZero().add(-7.466182457944), field.getZero().add(-2.118153357345),  field.getZero().add(0.160004048437));
        final TimeStampedFieldPVCoordinates<T> pv = new TimeStampedFieldPVCoordinates<>(date, position, velocity, acceleration);
        final Frame frame = FramesFactory.getEME2000();
        final T mu   = zero.add(Constants.EIGEN5C_EARTH_MU);
        final FieldCartesianOrbit<T> orbit = new FieldCartesianOrbit<>(pv, frame, zero.add(mu));

        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getA()),
                            orbit.getADot().getReal(),
                            4.3e-8);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getEquinoctialEx()),
                            orbit.getEquinoctialExDot().getReal(),
                            2.1e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getEquinoctialEy()),
                            orbit.getEquinoctialEyDot().getReal(),
                            5.3e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getHx()),
                            orbit.getHxDot().getReal(),
                            4.4e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getHy()),
                            orbit.getHyDot().getReal(),
                            8.0e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLv()),
                            orbit.getLvDot().getReal(),
                            1.2e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLE()),
                            orbit.getLEDot().getReal(),
                            7.8e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLM()),
                            orbit.getLMDot().getReal(),
                            8.8e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getE()),
                            orbit.getEDot().getReal(),
                            7.0e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getI()),
                            orbit.getIDot().getReal(),
                            5.7e-16);

    }

    private <T extends CalculusFieldElement<T>, S extends Function<FieldCartesianOrbit<T>, T>>
    double differentiate(TimeStampedFieldPVCoordinates<T> pv, Frame frame, T mu, S picker) {
        final DSFactory factory = new DSFactory(1, 1);
        FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(8, 0.1);
        UnivariateDifferentiableFunction diff = differentiator.differentiate(new UnivariateFunction() {
            public double value(double dt) {
                return picker.apply(new FieldCartesianOrbit<>(pv.shiftedBy(dt), frame, mu)).getReal();
            }
        });
        return diff.value(factory.variable(0, 0.0)).getPartialDerivative(1);
     }

    private <T extends CalculusFieldElement<T>> void doTestEquatorialRetrograde(Field<T> field) {
        final T zero = field.getZero();
        FieldVector3D<T> position = new FieldVector3D<>(field.getZero().add(10000000.0), field.getZero(), field.getZero());
        FieldVector3D<T> velocity = new FieldVector3D<>(field.getZero(), field.getZero().add(-6500.0), field.getZero());
        T r2 = position.getNormSq();
        T r  = r2.sqrt();
        FieldVector3D<T> acceleration = new FieldVector3D<>(r.multiply(r2).reciprocal().multiply(zero.add(mu).negate()), position,
                                                            field.getOne(), new FieldVector3D<>(field.getZero().add(-0.1),
                                                                                                field.getZero().add(0.2),
                                                                                                field.getZero().add(0.3)));
        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<>(position, velocity, acceleration);
        FieldCartesianOrbit<T> orbit = new FieldCartesianOrbit<>(pvCoordinates, FramesFactory.getEME2000(),
                                                                 FieldAbsoluteDate.getJ2000Epoch(field), zero.add(mu));
        Assertions.assertEquals(10637829.465, orbit.getA().getReal(), 1.0e-3);
        Assertions.assertEquals(-738.145, orbit.getADot().getReal(), 1.0e-3);
        Assertions.assertEquals(0.05995861, orbit.getE().getReal(), 1.0e-8);
        Assertions.assertEquals(-6.523e-5, orbit.getEDot().getReal(), 1.0e-8);
        Assertions.assertEquals(FastMath.PI, orbit.getI().getReal(), 1.0e-15);
        Assertions.assertTrue(Double.isNaN(orbit.getIDot().getReal()));
        Assertions.assertTrue(Double.isNaN(orbit.getHx().getReal()));
        Assertions.assertTrue(Double.isNaN(orbit.getHxDot().getReal()));
        Assertions.assertTrue(Double.isNaN(orbit.getHy().getReal()));
        Assertions.assertTrue(Double.isNaN(orbit.getHyDot().getReal()));
    }

    private <T extends CalculusFieldElement<T>> void doTestToString(Field<T> field) {
        final T zero = field.getZero();
        FieldVector3D<T> position = new FieldVector3D<>(field.getZero().add(-29536113.0),
                                                        field.getZero().add(30329259.0),
                                                        field.getZero().add(-100125.0));
        FieldVector3D<T> velocity = new FieldVector3D<>(field.getZero().add(-2194.0),
                                                        field.getZero().add(-2141.0),
                                                        field.getZero().add(-8.0));
        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<>(position, velocity);
        FieldCartesianOrbit<T> orbit = new FieldCartesianOrbit<>(pvCoordinates, FramesFactory.getEME2000(),
                                                                 FieldAbsoluteDate.getJ2000Epoch(field), zero.add(mu));
        Assertions.assertEquals("Cartesian parameters: {P(-2.9536113E7, 3.0329259E7, -100125.0), V(-2194.0, -2141.0, -8.0)}",
                            orbit.toString());
    }

    private <T extends CalculusFieldElement<T>> void doTestCopyNonKeplerianAcceleration(Field<T> field) {

        final T zero = field.getZero();
        final Frame eme2000     = FramesFactory.getEME2000();

        // Define GEO satellite position
        final FieldVector3D<T> position = new FieldVector3D<>(field.getZero().add(42164140),
                        field.getZero(),
                        field.getZero());
        // Build PVCoodrinates starting from its position and computing the corresponding circular velocity
        final FieldPVCoordinates<T> pv  =
                        new FieldPVCoordinates<>(position,
                                        new FieldVector3D<>(field.getZero(),
                                                        position.getNorm().reciprocal().multiply(mu).sqrt(),
                                                        field.getZero()));
        // Build a KeplerianOrbit in eme2000
        final FieldOrbit<T> orbit = new FieldCartesianOrbit<>(pv, eme2000, FieldAbsoluteDate.getJ2000Epoch(field), zero.add(mu));

        // Build another KeplerianOrbit as a copy of the first one
        final FieldOrbit<T> orbitCopy = new FieldCartesianOrbit<>(orbit);

        // Shift the orbit of a time-interval
        final FieldOrbit<T> shiftedOrbit     = orbit.shiftedBy(10); // This works good
        final FieldOrbit<T> shiftedOrbitCopy = orbitCopy.shiftedBy(10); // This does not work

        Assertions.assertEquals(0.0,
                            FieldVector3D.distance(shiftedOrbit.getPosition(),
                                                   shiftedOrbitCopy.getPosition()).getReal(),
                            1.0e-10);
        Assertions.assertEquals(0.0,
                            FieldVector3D.distance(shiftedOrbit.getPVCoordinates().getVelocity(),
                                                   shiftedOrbitCopy.getPVCoordinates().getVelocity()).getReal(),
                            1.0e-10);

    }

    private <T extends CalculusFieldElement<T>> void doTestNormalize(Field<T> field) {
        final T zero = field.getZero();
        final FieldVector3D<T> position = new FieldVector3D<>(zero.newInstance(42164140.0), zero, zero);
        final FieldPVCoordinates<T> pv  = new FieldPVCoordinates<>(position,
                                                                   new FieldVector3D<>(zero,
                                                                                       FastMath.sqrt(position.getNorm().reciprocal().multiply(mu)),
                                                                                       zero));
        final FieldOrbit<T> orbit = new FieldCartesianOrbit<>(pv,
                                                              FramesFactory.getEME2000(),
                                                              FieldAbsoluteDate.getJ2000Epoch(field),
                                                              field.getZero().newInstance(mu));
        Assertions.assertSame(orbit, orbit.getType().normalize(orbit, null));
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue1139(Field<T> field) {

        // Create
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        FieldVector3D<T> position = new FieldVector3D<>(zero.add(-29536113.0), zero.add(30329259.0), zero.add(-100125.0));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-2194.0), zero.add(-2141.0), zero.add(-8.0));
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<>( position, velocity);
        double mu = 3.9860047e14;

        FieldCartesianOrbit<T> p = new FieldCartesianOrbit<>(FieldPVCoordinates, FramesFactory.getEME2000(), date, zero.add(mu));

        double dt = 60.0;
        FieldAbsoluteDate<T> shiftedEpoch = date.shiftedBy(dt);

        FieldCartesianOrbit<T> p2 = new FieldCartesianOrbit<>(FieldPVCoordinates, FramesFactory.getEME2000(), shiftedEpoch, zero.add(mu));

        // Verify
        Assertions.assertEquals(dt, shiftedEpoch.durationFrom(date).getReal());
        Assertions.assertEquals(dt, p2.durationFrom(p).getReal());
        Assertions.assertEquals(dt, p2.getDate().durationFrom(p).getReal());
        Assertions.assertEquals(dt, p2.durationFrom(p.getDate()).getReal());
        Assertions.assertEquals(dt, p2.getDate().durationFrom(p.getDate()).getReal());
        Assertions.assertEquals(-dt, p.durationFrom(p2).getReal());

    }

    @Test
    void testFromCartesianOrbitWithoutDerivatives() {
        // GIVEN
        final ComplexField field = ComplexField.getInstance();
        final CartesianOrbit orbit = createOrbitTestFromCartesianOrbit(false);
        // WHEN
        final FieldCartesianOrbit<Complex> fieldOrbit = new FieldCartesianOrbit<>(field, orbit);
        // THEN
        compareFieldOrbitToOrbit(fieldOrbit, orbit);
    }

    @Test
    void testFromCartesianOrbitWithDerivatives() {
        // GIVEN
        final ComplexField field = ComplexField.getInstance();
        final CartesianOrbit orbit = createOrbitTestFromCartesianOrbit(true);
        // WHEN
        final FieldCartesianOrbit<Complex> fieldOrbit = new FieldCartesianOrbit<>(field, orbit);
        // THEN
        compareFieldOrbitToOrbit(fieldOrbit, orbit);
    }

    private CartesianOrbit createOrbitTestFromCartesianOrbit(final boolean withAcceleration) {
        final Vector3D position = Vector3D.MINUS_I;
        final Vector3D velocity = Vector3D.PLUS_K;
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame frame = FramesFactory.getEME2000();
        final TimeStampedPVCoordinates pv;
        if (withAcceleration) {
            pv = new TimeStampedPVCoordinates(date, position, velocity, Vector3D.PLUS_J);
        } else {
            pv = new TimeStampedPVCoordinates(date, position, velocity);
        }
        return new CartesianOrbit(pv, frame, mu);
    }

    private <T extends CalculusFieldElement<T>> void compareFieldOrbitToOrbit(final FieldCartesianOrbit<T> fieldOrbit,
                                                                              final CartesianOrbit orbit) {
        Assertions.assertEquals(orbit.getFrame(), fieldOrbit.getFrame());
        Assertions.assertEquals(orbit.getMu(), fieldOrbit.getMu().getReal());
        Assertions.assertEquals(orbit.getDate(), fieldOrbit.getDate().toAbsoluteDate());
        Assertions.assertEquals(orbit.getPosition(), fieldOrbit.getPosition().toVector3D());
        Assertions.assertEquals(orbit.getPVCoordinates().getVelocity(),
                fieldOrbit.getPVCoordinates().getVelocity().toVector3D());
        Assertions.assertEquals(orbit.getPVCoordinates().getAcceleration(),
                fieldOrbit.getPVCoordinates().getAcceleration().toVector3D());
        Assertions.assertEquals(orbit.hasDerivatives(), fieldOrbit.hasDerivatives());
        Assertions.assertEquals(orbit.getPVCoordinates().getAcceleration(),
                fieldOrbit.getPVCoordinates().getAcceleration().toVector3D());
    }

}

