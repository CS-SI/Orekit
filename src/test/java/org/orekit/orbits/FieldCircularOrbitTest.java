/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.analysis.differentiation.UnivariateDifferentiableFunction;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.FieldMatrixPreservingVisitor;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.propagation.analytical.FieldEcksteinHechlerPropagator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;


public class FieldCircularOrbitTest {

    // Body mu
    private double mu;

    @Before
    public void setUp() {

        Utils.setDataRoot("regular-data");

        // Body mu
        mu = 3.9860047e14;

    }

    @Test
    public void testCircularToEquinoc() {
        doTestCircularToEquinoctialEll(Decimal64Field.getInstance());
    }

    @Test
    public void testCircToEquinoc() {
        doTestCircularToEquinoctialCirc(Decimal64Field.getInstance());
    }

    @Test
    public void testAnomalyCirc() {
        doTestAnomalyCirc(Decimal64Field.getInstance());
    }

    @Test
    public void testAnomalyEll() {
        doTestAnomalyEll(Decimal64Field.getInstance());
    }

    @Test
    public void testCircToCart() {
        doTestCircularToCartesian(Decimal64Field.getInstance());
    }

    @Test
    public void testCircToKepl() {
        doTestCircularToKeplerian(Decimal64Field.getInstance());
    }

    @Test
    public void testGeometryCirc() {
        doTestGeometryCirc(Decimal64Field.getInstance());
    }

    @Test
    public void testGeometryEll() {
        doTestGeometryEll(Decimal64Field.getInstance());
    }

    @Test
    public void testInterpolationWithDerivatives() {
        doTestInterpolation(Decimal64Field.getInstance(), true,
                            397, 2.27e-8,
                            610, 3.24e-6,
                            4870, 115);
    }

    @Test
    public void testInterpolationWithoutDerivatives() {
        doTestInterpolation(Decimal64Field.getInstance(), false,
                            397, 0.0372,
                            610.0, 1.23,
                            4870, 8869);
    }

    @Test
    public void testJacobianFinited() {
        doTestJacobianFinitedifferences(Decimal64Field.getInstance());
    }

    @Test
    public void testJacoabianReference() {
        doTestJacobianReference(Decimal64Field.getInstance());
    }

    @Test
    public void testNumericalIssue25() {
        doTestNumericalIssue25(Decimal64Field.getInstance());
    }

    @Test
    public void testPerfectlyEquatorial() {
        doTestPerfectlyEquatorial(Decimal64Field.getInstance());
    }

    @Test
    public void testPositionVelocityNormsCirc() {
        doTestPositionVelocityNormsCirc(Decimal64Field.getInstance());
    }

    @Test
    public void testPositionVelocity() {
        doTestPositionVelocityNormsEll(Decimal64Field.getInstance());
    }

    @Test
    public void testSymmetryCir() {
        doTestSymmetryCir(Decimal64Field.getInstance());
    }

    @Test
    public void testSymmetryEll() {
        doTestSymmetryEll(Decimal64Field.getInstance());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testErrors()  {
        doTestNonInertialFrame(Decimal64Field.getInstance());
    }

    @Test
    public void testHyperbolic1() {
        doTestHyperbolic1(Decimal64Field.getInstance());
    }

    @Test
    public void testHyperbolic2() {
        doTestHyperbolic2(Decimal64Field.getInstance());
    }

    @Test
    public void testToOrbitWithoutDerivatives() {
        doTestToOrbitWithoutDerivatives(Decimal64Field.getInstance());
    }

    @Test
    public void testToOrbitWithDerivatives() {
        doTestToOrbitWithDerivatives(Decimal64Field.getInstance());
    }

    @Test
    public void testDerivativesConversionSymmetry() {
        doTestDerivativesConversionSymmetry(Decimal64Field.getInstance());
    }

    @Test
    public void testToString() {
        doTestToString(Decimal64Field.getInstance());
    }

    @Test
    public void testNonKeplerianDerivatives() {
        doTestNonKeplerianDerivatives(Decimal64Field.getInstance());
    }

    @Test
    public void testPositionAngleDerivatives() {
        doTestPositionAngleDerivatives(Decimal64Field.getInstance());
    }

    @Test
    public void testEquatorialRetrograde() {
        doTestEquatorialRetrograde(Decimal64Field.getInstance());
    }

    @Test
    public void testCopyNonKeplerianAcceleration() {
        doTestCopyNonKeplerianAcceleration(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestCircularToEquinoctialEll(Field<T> field) {

        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        T ix = zero.add(1.200e-04);
        T iy = zero.add(-1.16e-04);
        T i  = ix.multiply(ix).add(iy.multiply(iy)).divide(4).sqrt().asin().multiply(2);


        T raan = iy.atan2(ix);

        // elliptic orbit
        FieldCircularOrbit<T> circ =
            new FieldCircularOrbit<>(zero.add(42166.712), zero.add(0.5), zero.add(-0.5), i, raan,
                                     zero.add(5.300).subtract(raan), PositionAngle.MEAN,
                                     FramesFactory.getEME2000(), date, zero.add(mu));
        FieldVector3D<T> pos = circ.getPVCoordinates().getPosition();
        FieldVector3D<T> vit = circ.getPVCoordinates().getVelocity();

        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<>(pos, vit);



        FieldEquinoctialOrbit<T> param = new FieldEquinoctialOrbit<>(pvCoordinates, FramesFactory.getEME2000(), date, zero.add(mu));

        Assert.assertEquals(param.getA().getReal(),  circ.getA().getReal(), Utils.epsilonTest * circ.getA().getReal());
        Assert.assertEquals(param.getEquinoctialEx().getReal(), circ.getEquinoctialEx().getReal(), Utils.epsilonE * FastMath.abs(circ.getE().getReal()));
        Assert.assertEquals(param.getEquinoctialEy().getReal(), circ.getEquinoctialEy().getReal(), Utils.epsilonE * FastMath.abs(circ.getE().getReal()));
        Assert.assertEquals(param.getHx().getReal(), circ.getHx().getReal(), Utils.epsilonAngle * FastMath.abs(circ.getI().getReal()));
        Assert.assertEquals(param.getHy().getReal(), circ.getHy().getReal(), Utils.epsilonAngle * FastMath.abs(circ.getI().getReal()));


        Assert.assertEquals(MathUtils.normalizeAngle(param.getLv().getReal(), circ.getLv().getReal()), circ.getLv().getReal(), Utils.epsilonAngle * FastMath.abs(circ.getLv().getReal()));

        Assert.assertFalse(circ.hasDerivatives());
        Assert.assertNull(circ.getADot());
        Assert.assertNull(circ.getEquinoctialExDot());
        Assert.assertNull(circ.getEquinoctialEyDot());
        Assert.assertNull(circ.getHxDot());
        Assert.assertNull(circ.getHyDot());
        Assert.assertNull(circ.getLvDot());
        Assert.assertNull(circ.getLEDot());
        Assert.assertNull(circ.getLMDot());
        Assert.assertNull(circ.getEDot());
        Assert.assertNull(circ.getIDot());
        Assert.assertNull(circ.getCircularExDot());
        Assert.assertNull(circ.getCircularEyDot());
        Assert.assertNull(circ.getRightAscensionOfAscendingNodeDot());
        Assert.assertNull(circ.getAlphaVDot());
        Assert.assertNull(circ.getAlphaEDot());
        Assert.assertNull(circ.getAlphaMDot());
        Assert.assertNull(circ.getAlphaDot(PositionAngle.TRUE));
        Assert.assertNull(circ.getAlphaDot(PositionAngle.ECCENTRIC));
        Assert.assertNull(circ.getAlphaDot(PositionAngle.MEAN));

    }

    private <T extends RealFieldElement<T>> void doTestToOrbitWithoutDerivatives(Field<T> field) {
        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));
        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<>(position, velocity);
        FieldCircularOrbit<T>  fieldOrbit = new FieldCircularOrbit<>(pvCoordinates, FramesFactory.getEME2000(), date, zero.add(mu));
        CircularOrbit orbit = fieldOrbit.toOrbit();
        Assert.assertFalse(orbit.hasDerivatives());
        Assert.assertThat(orbit.getA(),                             relativelyCloseTo(fieldOrbit.getA().getReal(),                             0));
        Assert.assertThat(orbit.getCircularEx(),                    relativelyCloseTo(fieldOrbit.getCircularEx().getReal(),                    0));
        Assert.assertThat(orbit.getCircularEy(),                    relativelyCloseTo(fieldOrbit.getCircularEy().getReal(),                    0));
        Assert.assertThat(orbit.getI(),                             relativelyCloseTo(fieldOrbit.getI().getReal(),                             0));
        Assert.assertThat(orbit.getRightAscensionOfAscendingNode(), relativelyCloseTo(fieldOrbit.getRightAscensionOfAscendingNode().getReal(), 0));
        Assert.assertThat(orbit.getAlphaV(),                        relativelyCloseTo(fieldOrbit.getAlphaV().getReal(),                        0));
        Assert.assertTrue(Double.isNaN(orbit.getADot()));
        Assert.assertTrue(Double.isNaN(orbit.getEquinoctialExDot()));
        Assert.assertTrue(Double.isNaN(orbit.getEquinoctialEyDot()));
        Assert.assertTrue(Double.isNaN(orbit.getHxDot()));
        Assert.assertTrue(Double.isNaN(orbit.getHyDot()));
        Assert.assertTrue(Double.isNaN(orbit.getLvDot()));
        Assert.assertTrue(Double.isNaN(orbit.getLEDot()));
        Assert.assertTrue(Double.isNaN(orbit.getLMDot()));
        Assert.assertTrue(Double.isNaN(orbit.getEDot()));
        Assert.assertTrue(Double.isNaN(orbit.getIDot()));
        Assert.assertTrue(Double.isNaN(orbit.getCircularExDot()));
        Assert.assertTrue(Double.isNaN(orbit.getCircularEyDot()));
        Assert.assertTrue(Double.isNaN(orbit.getRightAscensionOfAscendingNodeDot()));
        Assert.assertTrue(Double.isNaN(orbit.getAlphaVDot()));
        Assert.assertTrue(Double.isNaN(orbit.getAlphaEDot()));
        Assert.assertTrue(Double.isNaN(orbit.getAlphaMDot()));
        Assert.assertTrue(Double.isNaN(orbit.getAlphaDot(PositionAngle.TRUE)));
        Assert.assertTrue(Double.isNaN(orbit.getAlphaDot(PositionAngle.ECCENTRIC)));
        Assert.assertTrue(Double.isNaN(orbit.getAlphaDot(PositionAngle.MEAN)));
    }

    private <T extends RealFieldElement<T>> void doTestToOrbitWithDerivatives(Field<T> field) {
        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));
        T r2 = position.getNormSq();
        T r = r2.sqrt();
        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<>(position, velocity,
                                                                       new FieldVector3D<>(r.multiply(r2).reciprocal().multiply(zero.add(mu).negate()),
                                                                                           position));
        FieldCircularOrbit<T>  fieldOrbit = new FieldCircularOrbit<>(pvCoordinates, FramesFactory.getEME2000(), date, zero.add(mu));
        CircularOrbit orbit = fieldOrbit.toOrbit();
        Assert.assertTrue(orbit.hasDerivatives());
        Assert.assertThat(orbit.getA(),                                 relativelyCloseTo(fieldOrbit.getA().getReal(),                                 0));
        Assert.assertThat(orbit.getCircularEx(),                        relativelyCloseTo(fieldOrbit.getCircularEx().getReal(),                        0));
        Assert.assertThat(orbit.getCircularEy(),                        relativelyCloseTo(fieldOrbit.getCircularEy().getReal(),                        0));
        Assert.assertThat(orbit.getI(),                                 relativelyCloseTo(fieldOrbit.getI().getReal(),                                 0));
        Assert.assertThat(orbit.getRightAscensionOfAscendingNode(),     relativelyCloseTo(fieldOrbit.getRightAscensionOfAscendingNode().getReal(),     0));
        Assert.assertThat(orbit.getAlphaV(),                            relativelyCloseTo(fieldOrbit.getAlphaV().getReal(),                            0));
        Assert.assertThat(orbit.getADot(),                              relativelyCloseTo(fieldOrbit.getADot().getReal(),                              0));
        Assert.assertThat(orbit.getEquinoctialExDot(),                  relativelyCloseTo(fieldOrbit.getEquinoctialExDot().getReal(),                  0));
        Assert.assertThat(orbit.getEquinoctialEyDot(),                  relativelyCloseTo(fieldOrbit.getEquinoctialEyDot().getReal(),                  0));
        Assert.assertThat(orbit.getHxDot(),                             relativelyCloseTo(fieldOrbit.getHxDot().getReal(),                             0));
        Assert.assertThat(orbit.getHyDot(),                             relativelyCloseTo(fieldOrbit.getHyDot().getReal(),                             0));
        Assert.assertThat(orbit.getLvDot(),                             relativelyCloseTo(fieldOrbit.getLvDot().getReal(),                             0));
        Assert.assertThat(orbit.getLEDot(),                             relativelyCloseTo(fieldOrbit.getLEDot().getReal(),                             0));
        Assert.assertThat(orbit.getLMDot(),                             relativelyCloseTo(fieldOrbit.getLMDot().getReal(),                             0));
        Assert.assertThat(orbit.getEDot(),                              relativelyCloseTo(fieldOrbit.getEDot().getReal(),                              0));
        Assert.assertThat(orbit.getIDot(),                              relativelyCloseTo(fieldOrbit.getIDot().getReal(),                              0));
        Assert.assertThat(orbit.getCircularExDot(),                     relativelyCloseTo(fieldOrbit.getCircularExDot().getReal(),                     0));
        Assert.assertThat(orbit.getCircularEyDot(),                     relativelyCloseTo(fieldOrbit.getCircularEyDot().getReal(),                     0));
        Assert.assertThat(orbit.getRightAscensionOfAscendingNodeDot(),  relativelyCloseTo(fieldOrbit.getRightAscensionOfAscendingNodeDot().getReal(),  0));
        Assert.assertThat(orbit.getAlphaVDot(),                         relativelyCloseTo(fieldOrbit.getAlphaVDot().getReal(),                         0));
        Assert.assertThat(orbit.getAlphaEDot(),                         relativelyCloseTo(fieldOrbit.getAlphaEDot().getReal(),                         0));
        Assert.assertThat(orbit.getAlphaMDot(),                         relativelyCloseTo(fieldOrbit.getAlphaMDot().getReal(),                         0));
        Assert.assertThat(orbit.getAlphaDot(PositionAngle.TRUE),        relativelyCloseTo(fieldOrbit.getAlphaDot(PositionAngle.TRUE).getReal(),        0));
        Assert.assertThat(orbit.getAlphaDot(PositionAngle.ECCENTRIC),   relativelyCloseTo(fieldOrbit.getAlphaDot(PositionAngle.ECCENTRIC).getReal(),   0));
        Assert.assertThat(orbit.getAlphaDot(PositionAngle.MEAN),        relativelyCloseTo(fieldOrbit.getAlphaDot(PositionAngle.MEAN).getReal(),        0));
    }

    private <T extends RealFieldElement<T>> void doTestCircularToEquinoctialCirc(Field<T> field) {

        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        T ix = zero.add(1.200e-04);
        T iy = zero.add(-1.16e-04);
        T i  = ix.multiply(ix).add(iy.multiply(iy)).divide(4).sqrt().asin().multiply(2);
        T raan = iy.atan2(ix);

        // circular orbit
        FieldEquinoctialOrbit<T> circCir =
            new FieldEquinoctialOrbit<>(zero.add(42166.712), zero.add(0.1e-10), zero.add(-0.1e-10), i, raan,
                                        raan.negate().add(5.300), PositionAngle.MEAN,
                                        FramesFactory.getEME2000(), date, zero.add(mu));
        FieldVector3D<T> posCir = circCir.getPVCoordinates().getPosition();
        FieldVector3D<T> vitCir = circCir.getPVCoordinates().getVelocity();

        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<>( posCir, vitCir);

        FieldEquinoctialOrbit<T> paramCir = new FieldEquinoctialOrbit<>(pvCoordinates, FramesFactory.getEME2000(), date, zero.add(mu));
        Assert.assertEquals(paramCir.getA().getReal(), circCir.getA().getReal(), Utils.epsilonTest * circCir.getA().getReal());
        Assert.assertEquals(paramCir.getEquinoctialEx().getReal(), circCir.getEquinoctialEx().getReal(), Utils.epsilonEcir * FastMath.abs(circCir.getE().getReal()));
        Assert.assertEquals(paramCir.getEquinoctialEy().getReal(), circCir.getEquinoctialEy().getReal(), Utils.epsilonEcir * FastMath.abs(circCir.getE().getReal()));
        Assert.assertEquals(paramCir.getHx().getReal(), circCir.getHx().getReal(), Utils.epsilonAngle * FastMath.abs(circCir.getI().getReal()));
        Assert.assertEquals(paramCir.getHy().getReal(), circCir.getHy().getReal(), Utils.epsilonAngle * FastMath.abs(circCir.getI().getReal()));
        Assert.assertEquals(MathUtils.normalizeAngle(paramCir.getLv().getReal(), circCir.getLv().getReal()), circCir.getLv().getReal(), Utils.epsilonAngle * FastMath.abs(circCir.getLv().getReal()));

    }

    private <T extends RealFieldElement<T>> void doTestCircularToCartesian(Field<T> field) {
        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        T ix = zero.add(1.200e-04);
        T iy = zero.add(-1.16e-04);
        T i  = ix.multiply(ix).add(iy.multiply(iy)).divide(4).sqrt().asin().multiply(2);
        T raan = iy.atan2(ix);
        T cosRaan = raan.cos();
        T sinRaan = raan.sin();
        T exTilde = zero.add(-7.900e-6);
        T eyTilde = zero.add(1.100e-4);
        T ex = exTilde.multiply(cosRaan).add(eyTilde.multiply(sinRaan));
        T ey = eyTilde.multiply(cosRaan).subtract(exTilde.multiply(sinRaan));

        FieldCircularOrbit<T> circ=
            new FieldCircularOrbit<>(zero.add(42166.712), ex, ey, i, raan,
                                     raan.negate().add(5.300), PositionAngle.MEAN,
                                     FramesFactory.getEME2000(), date, zero.add(mu));
        FieldVector3D<T> pos = circ.getPVCoordinates().getPosition();
        FieldVector3D<T> vel = circ.getPVCoordinates().getVelocity();

        // check 1/a = 2/r  - V2/mu
        T r = pos.getNorm();
        T v = vel.getNorm();
        Assert.assertEquals(2 / r.getReal() - v.getReal() * v.getReal() / mu, 1 / circ.getA().getReal(), 1.0e-7);

        Assert.assertEquals( 0.233745668678733e+05, pos.getX().getReal(), Utils.epsilonTest * r.getReal());
        Assert.assertEquals(-0.350998914352669e+05, pos.getY().getReal(), Utils.epsilonTest * r.getReal());
        Assert.assertEquals(-0.150053723123334e+01, pos.getZ().getReal(), Utils.epsilonTest * r.getReal());

        Assert.assertEquals(0.809135038364960e+05, vel.getX().getReal(), Utils.epsilonTest * v.getReal());
        Assert.assertEquals(0.538902268252598e+05, vel.getY().getReal(), Utils.epsilonTest * v.getReal());
        Assert.assertEquals(0.158527938296630e+02, vel.getZ().getReal(), Utils.epsilonTest * v.getReal());

    }

    private <T extends RealFieldElement<T>> void doTestCircularToKeplerian(Field<T> field) {
        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        T ix   = zero.add(1.20e-4);
        T iy   = zero.add(-1.16e-4);
        T i    = ix.multiply(ix).add(iy.multiply(iy)).divide(4).sqrt().asin().multiply(2);
        T raan = iy.atan2(ix);
        T cosRaan = raan.cos();
        T sinRaan = raan.sin();
        T exTilde = zero.add(-7.900e-6);
        T eyTilde = zero.add(1.100e-4);
        T ex = exTilde.multiply(cosRaan).add(eyTilde.multiply(sinRaan));
        T ey = eyTilde.multiply(cosRaan).subtract(exTilde.multiply(sinRaan));

        FieldCircularOrbit<T> circ=
            new FieldCircularOrbit<>(zero.add(42166.712), ex, ey, i, raan,
                                     raan.negate().add(5.300), PositionAngle.MEAN,
                                     FramesFactory.getEME2000(), date, zero.add(mu));
        FieldKeplerianOrbit<T> kep = new FieldKeplerianOrbit<>(circ);

        Assert.assertEquals(42166.71200, circ.getA().getReal(), Utils.epsilonTest * kep.getA().getReal());
        Assert.assertEquals(0.110283316961361e-03, kep.getE().getReal(), Utils.epsilonE * FastMath.abs(kep.getE().getReal()));
        Assert.assertEquals(0.166901168553917e-03, kep.getI().getReal(),
                     Utils.epsilonAngle * FastMath.abs(kep.getI().getReal()));
        Assert.assertEquals(MathUtils.normalizeAngle(-3.87224326008837, kep.getPerigeeArgument().getReal()),
                     kep.getPerigeeArgument().getReal(),
                     Utils.epsilonTest * 6 * FastMath.abs(kep.getPerigeeArgument().getReal())); //numerical propagation we changed to 6 time the precision used
        Assert.assertEquals(MathUtils.normalizeAngle(5.51473467358854, kep.getRightAscensionOfAscendingNode().getReal()),
                     kep.getRightAscensionOfAscendingNode().getReal(),
                     Utils.epsilonTest * FastMath.abs(kep.getRightAscensionOfAscendingNode().getReal()));

        Assert.assertEquals(MathUtils.normalizeAngle(zero.add(3.65750858649982), kep.getMeanAnomaly()).getReal(),
                     kep.getMeanAnomaly().getReal(),
                     Utils.epsilonTest * 5 * FastMath.abs(kep.getMeanAnomaly().getReal())); //numerical propagation we changed to 6 time the precision used

    }

    private <T extends RealFieldElement<T>> void doTestHyperbolic1(Field<T> field) {
        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        try {
            new FieldCircularOrbit<>(zero.add(42166.712), zero.add(0.9), zero.add(0.5), zero.add(0.01), zero.add(-0.02), zero.add( 5.300),
                                     PositionAngle.MEAN,  FramesFactory.getEME2000(), date, zero.add(mu));
        } catch (OrekitIllegalArgumentException oe) {
            Assert.assertEquals(OrekitMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS, oe.getSpecifier());
        }
    }

    private <T extends RealFieldElement<T>>  void doTestHyperbolic2(Field<T> field) {
        T zero =  field.getZero();
       FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        FieldOrbit<T> orbit = new FieldKeplerianOrbit<>(zero.add(42166.712), zero.add(0.9), zero.add(0.5), zero.add(0.01), zero.add(-0.02), zero.add( 5.300),
                                                        PositionAngle.MEAN,  FramesFactory.getEME2000(), date, zero.add(mu));
        try {
            new FieldCircularOrbit<>(orbit.getPVCoordinates(), orbit.getFrame(), orbit.getMu());
        } catch (OrekitIllegalArgumentException oe) {
            Assert.assertEquals(OrekitMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS, oe.getSpecifier());
        }
    }

    private <T extends RealFieldElement<T>> void doTestAnomalyEll(Field<T> field) {
        T zero =  field.getZero();
        T one =  field.getOne();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        // elliptic orbit
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<>( position, velocity);

        FieldCircularOrbit<T>  p   = new FieldCircularOrbit<>(pvCoordinates, FramesFactory.getEME2000(), date, zero.add(mu));
        FieldKeplerianOrbit<T> kep = new FieldKeplerianOrbit<>(p);

        T e       = p.getE();
        T eRatio  = one.subtract(e).divide(one.add(e)).sqrt();
        T raan    = kep.getRightAscensionOfAscendingNode();
        T paPraan = kep.getPerigeeArgument().add(raan);

        T lv = zero.add(1.1);
        // formulations for elliptic case
        T lE = lv.subtract(paPraan).divide(2).tan().multiply(eRatio).atan().multiply(2).add(paPraan);
        T lM = lE.subtract(e.multiply(lE.subtract(paPraan).sin()));

        p = new FieldCircularOrbit<>(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                     p.getRightAscensionOfAscendingNode(),
                                     p.getAlphaV(), lv.subtract(raan), PositionAngle.TRUE, p.getFrame(), date, zero.add(mu));
        Assert.assertEquals(p.getAlphaV().getReal() + raan.getReal(), lv.getReal(), Utils.epsilonAngle * FastMath.abs(lv.getReal()));
        Assert.assertEquals(p.getAlphaE().getReal() + raan.getReal(), lE.getReal(), Utils.epsilonAngle * FastMath.abs(lE.getReal()));
        Assert.assertEquals(p.getAlphaM().getReal() + raan.getReal(), lM.getReal(), Utils.epsilonAngle * FastMath.abs(lM.getReal()));
        p = new FieldCircularOrbit<>(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                     p.getRightAscensionOfAscendingNode(),
                                     p.getAlphaV(), zero, PositionAngle.TRUE, p.getFrame(), date, zero.add(mu));


        p = new FieldCircularOrbit<>(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                     p.getRightAscensionOfAscendingNode(),
                                     p.getAlphaV(), lE.subtract(raan), PositionAngle.ECCENTRIC, p.getFrame(), date, zero.add(mu));
        Assert.assertEquals(p.getAlphaV().getReal() + raan.getReal(), lv.getReal(), Utils.epsilonAngle * FastMath.abs(lv.getReal()));
        Assert.assertEquals(p.getAlphaE().getReal() + raan.getReal(), lE.getReal(), Utils.epsilonAngle * FastMath.abs(lE.getReal()));
        Assert.assertEquals(p.getAlphaM().getReal() + raan.getReal(), lM.getReal(), Utils.epsilonAngle * FastMath.abs(lM.getReal()));
        p = new FieldCircularOrbit<>(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                     p.getRightAscensionOfAscendingNode(),
                                     p.getAlphaV(), zero, PositionAngle.TRUE, p.getFrame(), date, zero.add(mu));

        p = new FieldCircularOrbit<>(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                     p.getRightAscensionOfAscendingNode(),
                                     p.getAlphaV(), lM.subtract(raan), PositionAngle.MEAN, p.getFrame(), date, zero.add(mu));
        Assert.assertEquals(p.getAlphaV().getReal() + raan.getReal(), lv.getReal(), Utils.epsilonAngle * FastMath.abs(lv.getReal()));
        Assert.assertEquals(p.getAlphaE().getReal() + raan.getReal(), lE.getReal(), Utils.epsilonAngle * FastMath.abs(lE.getReal()));
        Assert.assertEquals(p.getAlphaM().getReal() + raan.getReal(), lM.getReal(), Utils.epsilonAngle * FastMath.abs(lM.getReal()));

    }

    private <T extends RealFieldElement<T>> void doTestAnomalyCirc(Field<T> field) {
        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));
        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<>( position, velocity);
        FieldCircularOrbit<T>  p = new FieldCircularOrbit<>(pvCoordinates, FramesFactory.getEME2000(), date, zero.add(mu));
        T raan = p.getRightAscensionOfAscendingNode();

        // circular orbit
        p = new FieldCircularOrbit<>(p.getA() , zero, zero, p.getRightAscensionOfAscendingNode(),
                                     p.getAlphaV(), p.getAlphaV(), PositionAngle.TRUE, p.getFrame(), date, zero.add(mu));

        T lv = zero.add(1.1);
        T lE = lv;
        T lM = lE;

        p = new FieldCircularOrbit<>(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                     p.getRightAscensionOfAscendingNode(),
                                     p.getAlphaV(), lv.subtract(raan), PositionAngle.TRUE, p.getFrame(), date, zero.add(mu));
        Assert.assertEquals(p.getAlphaV().getReal() + raan.getReal(), lv.getReal(), Utils.epsilonAngle * FastMath.abs(lv.getReal()));
        Assert.assertEquals(p.getAlphaE().getReal() + raan.getReal(), lE.getReal(), Utils.epsilonAngle * FastMath.abs(lE.getReal()));
        Assert.assertEquals(p.getAlphaM().getReal() + raan.getReal(), lM.getReal(), Utils.epsilonAngle * FastMath.abs(lM.getReal()));
        p = new FieldCircularOrbit<>(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                     p.getRightAscensionOfAscendingNode(),
                                     p.getAlphaV(), zero, PositionAngle.TRUE, p.getFrame(), date, zero.add(mu));

        p = new FieldCircularOrbit<>(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                     p.getRightAscensionOfAscendingNode(),
                                     p.getAlphaV(), lE.subtract(raan), PositionAngle.ECCENTRIC, p.getFrame(), date, zero.add(mu));

        Assert.assertEquals(p.getAlphaV().getReal() + raan.getReal(), lv.getReal(), Utils.epsilonAngle * FastMath.abs(lv.getReal()));
        Assert.assertEquals(p.getAlphaE().getReal() + raan.getReal(), lE.getReal(), Utils.epsilonAngle * FastMath.abs(lE.getReal()));
        Assert.assertEquals(p.getAlphaM().getReal() + raan.getReal(), lM.getReal(), Utils.epsilonAngle * FastMath.abs(lM.getReal()));
        p = new FieldCircularOrbit<>(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                     p.getRightAscensionOfAscendingNode(),
                                     p.getAlphaV(), zero, PositionAngle.TRUE, p.getFrame(), date, zero.add(mu));

        p = new FieldCircularOrbit<>(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                     p.getRightAscensionOfAscendingNode(),
                                     p.getAlphaV(), lM.subtract(raan), PositionAngle.MEAN, p.getFrame(), date, zero.add(mu));
        Assert.assertEquals(p.getAlphaV().getReal() + raan.getReal(), lv.getReal(), Utils.epsilonAngle * FastMath.abs(lv.getReal()));
        Assert.assertEquals(p.getAlphaE().getReal() + raan.getReal(), lE.getReal(), Utils.epsilonAngle * FastMath.abs(lE.getReal()));
        Assert.assertEquals(p.getAlphaM().getReal() + raan.getReal(), lM.getReal(), Utils.epsilonAngle * FastMath.abs(lM.getReal()));

    }

    private <T extends RealFieldElement<T>> void doTestPositionVelocityNormsEll(Field<T> field) {
        T zero =  field.getZero();
        T one =  field.getOne();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        // elliptic and non equatorial (i retrograde) orbit
        T hx =  zero.add(1.2);
        T hy =  zero.add(2.1);
        T i  = hx.multiply(hx).add(hy.multiply(hy)).sqrt().atan().multiply(2);
        T raan = hy.atan2(hx);
        FieldCircularOrbit<T> p =
            new FieldCircularOrbit<>(zero.add(42166.712), zero.add(0.5), zero.add(-0.5), i, raan,
                                     raan.negate().add(0.67), PositionAngle.TRUE,
                                     FramesFactory.getEME2000(), date, zero.add(mu));

        T ex = p.getEquinoctialEx();
        T ey = p.getEquinoctialEy();
        T lv = p.getLv();
        T ksi     = ex.multiply(lv.cos()).add(1).add(ey.multiply(lv.sin()));
        T nu      = ex.multiply(lv.sin()).subtract(ey.multiply(lv.cos()));
        T epsilon = one.subtract(ex.multiply(ex)).subtract(ey.multiply(ey)).sqrt();

        T a  = p.getA();
        T na = a.reciprocal().multiply(mu).sqrt();

        Assert.assertEquals(a.getReal() * epsilon.getReal() * epsilon.getReal() / ksi.getReal(),
                     p.getPVCoordinates().getPosition().getNorm().getReal(),
                     Utils.epsilonTest * FastMath.abs(p.getPVCoordinates().getPosition().getNorm().getReal()));
        Assert.assertEquals(na.getReal() * FastMath.sqrt(ksi.getReal() * ksi.getReal() + nu.getReal() * nu.getReal()) / epsilon.getReal(),
                     p.getPVCoordinates().getVelocity().getNorm().getReal(),
                     Utils.epsilonTest * FastMath.abs(p.getPVCoordinates().getVelocity().getNorm().getReal()));

    }

    private <T extends RealFieldElement<T>> void doTestNumericalIssue25(Field<T> field) {

        T zero =  field.getZero();
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(3782116.14107698), zero.add(416663.11924914), zero.add(5875541.62103057));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-6349.7848910501), zero.add(288.4061811651), zero.add(4066.9366759691));
        FieldCircularOrbit<T> orbit = new FieldCircularOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                               FramesFactory.getEME2000(),
                                                               new FieldAbsoluteDate<>(field, "2004-01-01T23:00:00.000",
                                                                                       TimeScalesFactory.getUTC()),
                                                               zero.add(3.986004415E14));
        Assert.assertEquals(0.0, orbit.getE().getReal(), 2.0e-14);
    }

    private <T extends RealFieldElement<T>> void doTestPerfectlyEquatorial(Field<T> field) {
        T zero =  field.getZero();
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(-7293947.695148368), zero.add( 5122184.668436634), zero.add(0.0));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-3890.4029433398), zero.add( -5369.811285264604), zero.add(0.0));
        FieldCircularOrbit<T> orbit = new FieldCircularOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                               FramesFactory.getEME2000(),
                                                               new FieldAbsoluteDate<>(field, "2004-01-01T23:00:00.000",
                                                                                       TimeScalesFactory.getUTC()),
                                                               zero.add(3.986004415E14));
        Assert.assertEquals(0.0, orbit.getI().getReal(), 2.0e-14);
        Assert.assertEquals(0.0, orbit.getRightAscensionOfAscendingNode().getReal(), 2.0e-14);
    }

    private <T extends RealFieldElement<T>> void doTestPositionVelocityNormsCirc(Field<T> field) {
        T zero =  field.getZero();
        T one =  field.getOne();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        // elliptic and non equatorial (i retrograde) orbit
        T hx =  zero.add(0.1e-8);
        T hy =  zero.add(0.1e-8);
        T i  = hx.multiply(hx).add(hy.multiply(hy)).sqrt().atan().multiply(2);
        T raan = hy.atan2(hx);
        FieldCircularOrbit<T> pCirEqua =
            new FieldCircularOrbit<>(zero.add(42166.712), zero.add(0.1e-8), zero.add(0.1e-8), i, raan,
                                     raan.negate().add(0.67), PositionAngle.TRUE,
                                     FramesFactory.getEME2000(), date, zero.add(mu));

        T ex = pCirEqua.getEquinoctialEx();
        T ey = pCirEqua.getEquinoctialEy();
        T lv = pCirEqua.getLv();
        T ksi     = ex.multiply(lv.cos()).add(1).add(ey.multiply(lv.sin()));
        T nu      = ex.multiply(lv.sin()).subtract(ey.multiply(lv.cos()));
        T epsilon = one.subtract(ex.multiply(ex)).subtract(ey.multiply(ey)).sqrt();

        T a  = pCirEqua.getA();
        T na = a.reciprocal().multiply(mu).sqrt();

        Assert.assertEquals(a.getReal() * epsilon.getReal() * epsilon.getReal() / ksi.getReal(),
                            pCirEqua.getPVCoordinates().getPosition().getNorm().getReal(),
                            Utils.epsilonTest * FastMath.abs(pCirEqua.getPVCoordinates().getPosition().getNorm().getReal()));
        Assert.assertEquals(na.getReal() * FastMath.sqrt(ksi.getReal() * ksi.getReal() + nu.getReal() * nu.getReal()) / epsilon.getReal(),
                            pCirEqua.getPVCoordinates().getVelocity().getNorm().getReal(),
                            Utils.epsilonTest * FastMath.abs(pCirEqua.getPVCoordinates().getVelocity().getNorm().getReal()));
    }

    private <T extends RealFieldElement<T>> void doTestGeometryEll(Field<T> field) {

        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        // elliptic and non equatorial (i retrograde) orbit
        T hx =  zero.add(1.2);
        T hy =  zero.add(2.1);
        T i  = hx.multiply(hx).add(hy.multiply(hy)).sqrt().atan().multiply(2);
        T raan = hy.atan2(hx);
        FieldCircularOrbit<T> p =
            new FieldCircularOrbit<>(zero.add(42166.712), zero.add(0.5), zero.add(-0.5), i, raan,
                                     raan.negate().add(0.67), PositionAngle.TRUE,
                                     FramesFactory.getEME2000(), date, zero.add(mu));

        FieldVector3D<T> position = p.getPVCoordinates().getPosition();
        FieldVector3D<T> velocity = p.getPVCoordinates().getVelocity();
        FieldVector3D<T> momentum = p.getPVCoordinates().getMomentum().normalize();

        T apogeeRadius  = p.getA().multiply( p.getE().add(1));
        T perigeeRadius = p.getA().multiply(p.getE().negate().add(1));

        for (T alphaV = zero; alphaV.getReal() <= 2 * FastMath.PI; alphaV=alphaV.add(zero.add(2).multiply(FastMath.PI/100.))) {
            p = new FieldCircularOrbit<>(p.getA() , p.getCircularEx(), p.getCircularEy(), p.getI(),
                                         p.getRightAscensionOfAscendingNode(),
                                         alphaV, PositionAngle.TRUE, p.getFrame(), date, zero.add(mu));
            position = p.getPVCoordinates().getPosition();
            // test if the norm of the position is in the range [perigee radius, apogee radius]
            // Warning: these tests are without absolute value by choice
            Assert.assertTrue((position.getNorm().getReal() - apogeeRadius.getReal())  <= (  apogeeRadius.getReal() * Utils.epsilonTest));
            Assert.assertTrue((position.getNorm().getReal() - perigeeRadius.getReal()) >= (- perigeeRadius.getReal() * Utils.epsilonTest));

            position= position.normalize();
            velocity = p.getPVCoordinates().getVelocity();
            velocity= velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and momemtum) are normalized here

            // test of orthogonality between position and momentum
            Assert.assertTrue(FastMath.abs(Vector3D.dotProduct(position.toVector3D(), momentum.toVector3D())) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assert.assertTrue(FastMath.abs(Vector3D.dotProduct(velocity.toVector3D(), momentum.toVector3D())) < Utils.epsilonTest);
        }

    }

    private <T extends RealFieldElement<T>> void doTestGeometryCirc(Field<T> field) {

        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        //  circular and equatorial orbit
        T hx =  zero.add(0.1e-8);
        T hy =  zero.add(0.1e-8);
        T i  =  hx.multiply(hx).add(hy.multiply(hy)).sqrt().atan().multiply(2);
        T raan = hy.atan2(hx);
        FieldCircularOrbit<T> pCirEqua =
            new FieldCircularOrbit<>(zero.add(42166.712), zero.add(0.1e-8), zero.add(0.1e-8), i, raan,
                                    raan.negate().add(0.67), PositionAngle.TRUE,
                                    FramesFactory.getEME2000(), date, zero.add(mu));

        FieldVector3D<T> position = pCirEqua.getPVCoordinates().getPosition();
        FieldVector3D<T> velocity = pCirEqua.getPVCoordinates().getVelocity();
        FieldVector3D<T> momentum = pCirEqua.getPVCoordinates().getMomentum().normalize();

        T apogeeRadius  = pCirEqua.getA().multiply( pCirEqua.getE().add(1));
        T perigeeRadius = pCirEqua.getA().multiply(pCirEqua.getE().negate().add(1));
        // test if apogee equals perigee
        Assert.assertEquals(perigeeRadius.getReal(), apogeeRadius.getReal(), 1.e+4 * Utils.epsilonTest * apogeeRadius.getReal());

        for (T alphaV = zero; alphaV.getReal() <= 2 * FastMath.PI; alphaV = alphaV.add(zero.add(2 * FastMath.PI/100.))) {
            pCirEqua = new FieldCircularOrbit<>(pCirEqua.getA() , pCirEqua.getCircularEx(), pCirEqua.getCircularEy(), pCirEqua.getI(),
                                               pCirEqua.getRightAscensionOfAscendingNode(),
                                               alphaV, PositionAngle.TRUE, pCirEqua.getFrame(), date, zero.add(mu));
            position = pCirEqua.getPVCoordinates().getPosition();

            // test if the norm pf the position is in the range [perigee radius, apogee radius]
            Assert.assertTrue((position.getNorm().getReal() - apogeeRadius.getReal())  <= (  apogeeRadius.getReal() * Utils.epsilonTest));
            Assert.assertTrue((position.getNorm().getReal() - perigeeRadius.getReal()) >= (- perigeeRadius.getReal() * Utils.epsilonTest));

            position= position.normalize();
            velocity = pCirEqua.getPVCoordinates().getVelocity();
            velocity= velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and momemtum) are normalized here

            // test of orthogonality between position and momentum
            Assert.assertTrue(FastMath.abs(Vector3D.dotProduct(position.toVector3D(), momentum.toVector3D())) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assert.assertTrue(FastMath.abs(Vector3D.dotProduct(velocity.toVector3D(), momentum.toVector3D())) < Utils.epsilonTest);
        }
    }

    private <T extends RealFieldElement<T>> void doTestSymmetryEll(Field<T> field) {

        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        // elliptic and non equatorial orbit
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(4512.9), zero.add(18260.), zero.add(-5127.));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(134664.6), zero.add(90066.8), zero.add(72047.6));
        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<>(position, velocity);

        FieldCircularOrbit<T> p = new FieldCircularOrbit<>(pvCoordinates, FramesFactory.getEME2000(), date, zero.add(mu));

        FieldVector3D<T> positionOffset = p.getPVCoordinates().getPosition();
        FieldVector3D<T> velocityOffset = p.getPVCoordinates().getVelocity();

        positionOffset = positionOffset.subtract(position);
        velocityOffset = velocityOffset.subtract(velocity);

        Assert.assertEquals(0.0, positionOffset.getNorm().getReal(), position.getNorm().getReal() * Utils.epsilonTest);
        Assert.assertEquals(0.0, velocityOffset.getNorm().getReal(), velocity.getNorm().getReal() * Utils.epsilonTest);

    }

    private <T extends RealFieldElement<T>> void doTestSymmetryCir(Field<T> field) {
        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        // circular and equatorial orbit
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(33051.2), zero.add(26184.9), zero.add(-1.3E-5));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-60376.2), zero.add(76208.), zero.add(2.7E-4));
        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<>(position, velocity);

        FieldCircularOrbit<T> p = new FieldCircularOrbit<>(pvCoordinates, FramesFactory.getEME2000(), date, zero.add(mu));

        FieldVector3D<T> positionOffset = p.getPVCoordinates().getPosition().subtract(position);
        FieldVector3D<T> velocityOffset = p.getPVCoordinates().getVelocity().subtract(velocity);

        Assert.assertEquals(0.0, positionOffset.getNorm().getReal(), position.getNorm().getReal() * Utils.epsilonTest);
        Assert.assertEquals(0.0, velocityOffset.getNorm().getReal(), velocity.getNorm().getReal() * Utils.epsilonTest);

    }

    private <T extends RealFieldElement<T>> void doTestNonInertialFrame(Field<T> field) throws IllegalArgumentException {
        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        FieldVector3D<T> position = new FieldVector3D<>(zero.add(33051.2), zero.add(26184.9), zero.add(-1.3E-5));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-60376.2), zero.add(76208.), zero.add(2.7E-4));
        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<>( position, velocity);
        new FieldCircularOrbit<>(pvCoordinates,
                                 new Frame(FramesFactory.getEME2000(), Transform.IDENTITY, "non-inertial", false),
                                 date, zero.add(mu));
    }

    private <T extends RealFieldElement<T>> void doTestJacobianReference(Field<T> field) {
        T zero =  field.getZero();
        FieldAbsoluteDate<T> dateTca = new FieldAbsoluteDate<>(field, 2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        FieldCircularOrbit<T> orbCir = new FieldCircularOrbit<>(zero.add(7000000.0), zero.add(0.01), zero.add(-0.02), zero.add(1.2), zero.add(2.1),
                                                                zero.add(0.7), PositionAngle.MEAN,
                                                                FramesFactory.getEME2000(), dateTca, zero.add(mu));

        // the following reference values have been computed using the free software
        // version 6.2 of the MSLIB fortran library by the following program:
        //        program cir_jacobian
        //
        //        use mslib
        //        implicit none
        //
        //        integer, parameter :: nb = 11
        //        integer :: i,j
        //        type(tm_code_retour)      ::  code_retour
        //
        //        real(pm_reel), parameter :: mu= 3.986004415e+14_pm_reel
        //        real(pm_reel),dimension(3)::vit_car,pos_car
        //        type(tm_orb_cir)::cir
        //        real(pm_reel), dimension(6,6)::jacob
        //        real(pm_reel)::norme
        //
        //
        //        cir%a=7000000_pm_reel
        //        cir%ex=0.01_pm_reel
        //        cir%ey=-0.02_pm_reel
        //        cir%i=1.2_pm_reel
        //        cir%gom=2.1_pm_reel
        //        cir%pso_M=0.7_pm_reel
        //
        //        call mv_cir_car(mu,cir,pos_car,vit_car,code_retour)
        //        write(*,*)code_retour%valeur
        //        write(*,1000)pos_car,vit_car
        //
        //
        //        call mu_norme(pos_car,norme,code_retour)
        //        write(*,*)norme
        //
        //        call mv_car_cir (mu, pos_car, vit_car, cir, code_retour, jacob)
        //        write(*,*)code_retour%valeur
        //
        //        write(*,*)"circular = ", cir%a, cir%ex, cir%ey, cir%i, cir%gom, cir%pso_M
        //
        //        do i = 1,6
        //           write(*,*) " ",(jacob(i,j),j=1,6)
        //        end do
        //
        //        1000 format (6(f24.15,1x))
        //        end program cir_jacobian
        FieldVector3D<T> pRef = new FieldVector3D<>(zero.add(-4106905.105389204807580), zero.add( 3603162.539798960555345), zero.add(4439730.167038885876536));
        FieldVector3D<T> vRef = new FieldVector3D<>(zero.add(740.132407342422994), zero.add(-5308.773280141396754), zero.add( 5250.338353483879473));
        double[][] jRefR = {
            { -1.1535467596325562      ,        1.0120556393573172,        1.2470306024626943,        181.96913090864561,       -1305.2162699469984,        1290.8494448855752 },
            { -5.07367368325471104E-008, -1.27870567070456834E-008,  1.31544531338558113E-007, -3.09332106417043592E-005, -9.60781276304445404E-005,  1.91506964883791605E-004 },
            { -6.59428471712402018E-008,  1.24561703203882533E-007, -1.41907027322388158E-008,  7.63442601186485441E-005, -1.77446722746170009E-004,  5.99464401287846734E-005 },
            {  7.55079920652274275E-008,  4.41606835295069131E-008,  3.40079310688458225E-008,  7.89724635377817962E-005,  4.61868720707717372E-005,  3.55682891687782599E-005 },
            { -9.20788748896973282E-008, -5.38521280004949642E-008, -4.14712660805579618E-008,  7.78626692360739821E-005,  4.55378113077967091E-005,  3.50684505810897702E-005 },
            {  1.85082436324531617E-008,  1.20506219457886855E-007, -8.31277842285972640E-008,  1.27364008345789645E-004, -1.54770720974742483E-004, -1.78589436862677754E-004 }
        };

        T[][] jRef = MathArrays.buildArray(field, 6, 6);

        for (int ii = 0; ii<6 ; ii++){
            for (int jj = 0; jj<6 ; jj++){
                jRef[ii][jj] = zero.add(jRefR[ii][jj]);
            }
        }

        FieldPVCoordinates<T> pv = orbCir.getPVCoordinates();
        Assert.assertEquals(0, pv.getPosition().subtract(pRef).getNorm().getReal(), 3.0e-16 * pRef.getNorm().getReal());
        Assert.assertEquals(0, pv.getVelocity().subtract(vRef).getNorm().getReal(), 2.0e-12 * vRef.getNorm().getReal());

        T[][] jacobian = MathArrays.buildArray(field, 6, 6);

        orbCir.getJacobianWrtCartesian(PositionAngle.MEAN, jacobian);

        for (int i = 0; i < jacobian.length; i++) {
            T[] row    = jacobian[i];
            T[] rowRef = jRef[i];
            for (int j = 0; j < row.length; j++) {
                Assert.assertEquals(0, (row[j].getReal() - rowRef[j].getReal()) / rowRef[j].getReal(), 1e-14);
            }
        }

    }

    private <T extends RealFieldElement<T>> void doTestJacobianFinitedifferences(Field<T> field) {
        T zero =  field.getZero();

        FieldAbsoluteDate<T> dateTca = new FieldAbsoluteDate<>(field, 2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        FieldCircularOrbit<T> orbCir = new FieldCircularOrbit<>(zero.add(7000000.0), zero.add(0.01), zero.add(-0.02), zero.add(1.2), zero.add(2.1),
                                                                zero.add(0.7), PositionAngle.MEAN,
                                                                FramesFactory.getEME2000(), dateTca, zero.add(mu));

        for (PositionAngle type : PositionAngle.values()) {
            T hP = zero.add(2.0);
            T[][] finiteDiffJacobian = finiteDifferencesJacobian(type, orbCir, hP);
            T[][] jacobian = MathArrays.buildArray(field, 6, 6);
            orbCir.getJacobianWrtCartesian(type, jacobian);

            for (int i = 0; i < jacobian.length; i++) {
                T[] row    = jacobian[i];
                T[] rowRef = finiteDiffJacobian[i];

                for (int j = 0; j < row.length; j++) {
                    Assert.assertEquals(0, (row[j].getReal() - rowRef[j].getReal()) / rowRef[j].getReal(), 8.0e-9);
                }

           }

            T[][] invJacobian = MathArrays.buildArray(field, 6, 6);
            orbCir.getJacobianWrtParameters(type, invJacobian);
            MatrixUtils.createFieldMatrix(jacobian).
                            multiply(MatrixUtils.createFieldMatrix(invJacobian)).
            walkInRowOrder(new FieldMatrixPreservingVisitor<T>() {
                public void start(int rows, int columns,
                                  int startRow, int endRow, int startColumn, int endColumn) {
                }

                public void visit(int row, int column, T value) {
                    Assert.assertEquals(row == column ? 1.0 : 0.0, value.getReal(), 3.0e-9);
                }

                public T end() {
                    return null;
                }
            });

        }

    }

    private <T extends RealFieldElement<T>> T[][] finiteDifferencesJacobian(PositionAngle type, FieldCircularOrbit<T> orbit, T hP)
        {
        Field<T> field = hP.getField();
        T[][] jacobian = MathArrays.buildArray(field, 6, 6);
        for (int i = 0; i < 6; ++i) {
            fillColumn(type, i, orbit, hP, jacobian);
        }
        return jacobian;
    }

    private <T extends RealFieldElement<T>> void fillColumn(PositionAngle type, int i, FieldCircularOrbit<T> orbit, T hP, T[][] jacobian) {

        T zero = hP.getField().getZero();
        // at constant energy (i.e. constant semi major axis), we have dV = -mu dP / (V * r^2)
        // we use this to compute a velocity step size from the position step size
        FieldVector3D<T> p = orbit.getPVCoordinates().getPosition();
        FieldVector3D<T> v = orbit.getPVCoordinates().getVelocity();
        T hV =  hP.multiply(orbit.getMu()).divide(v.getNorm().multiply(p.getNormSq()));

        T h;
        FieldVector3D<T> dP = new FieldVector3D<>(zero, zero, zero);
        FieldVector3D<T> dV = new FieldVector3D<>(zero, zero, zero);
        switch (i) {
        case 0:
            h = hP;
            dP = new FieldVector3D<>(hP, zero, zero);
            break;
        case 1:
            h = hP;
            dP = new FieldVector3D<>(zero, hP, zero);
            break;
        case 2:
            h = hP;
            dP = new FieldVector3D<>(zero, zero, hP);
            break;
        case 3:
            h = hV;
            dV = new FieldVector3D<>(hV, zero, zero);
            break;
        case 4:
            h = hV;
            dV = new FieldVector3D<>(zero, hV, zero);
            break;
        default:
            h = hV;
            dV = new FieldVector3D<>(zero, zero, hV);
            break;
        }

        FieldCircularOrbit<T> oM4h = new FieldCircularOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(1, p, -4, dP), new FieldVector3D<>(1, v, -4, dV)),
                                                              orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldCircularOrbit<T> oM3h = new FieldCircularOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(1, p, -3, dP), new FieldVector3D<>(1, v, -3, dV)),
                                                              orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldCircularOrbit<T> oM2h = new FieldCircularOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(1, p, -2, dP), new FieldVector3D<>(1, v, -2, dV)),
                                                              orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldCircularOrbit<T> oM1h = new FieldCircularOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(1, p, -1, dP), new FieldVector3D<>(1, v, -1, dV)),
                                                              orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldCircularOrbit<T> oP1h = new FieldCircularOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(1, p, +1, dP), new FieldVector3D<>(1, v, +1, dV)),
                                                              orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldCircularOrbit<T> oP2h = new FieldCircularOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(1, p, +2, dP), new FieldVector3D<>(1, v, +2, dV)),
                                                              orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldCircularOrbit<T> oP3h = new FieldCircularOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(1, p, +3, dP), new FieldVector3D<>(1, v, +3, dV)),
                                                              orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldCircularOrbit<T> oP4h = new FieldCircularOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(1, p, +4, dP), new FieldVector3D<>(1, v, +4, dV)),
                                                              orbit.getFrame(), orbit.getDate(), orbit.getMu());


        jacobian[0][i] =(zero.add( -3).multiply(oP4h.getA()                            .subtract( oM4h.getA()))  .add(
                         zero.add( 32).multiply(oP3h.getA()                            .subtract( oM3h.getA()))) .subtract(
                         zero.add(168).multiply(oP2h.getA()                            .subtract( oM2h.getA()))) .add(
                         zero.add(672).multiply(oP1h.getA()                            .subtract( oM1h.getA())))).divide(h.multiply(840));
        jacobian[1][i] =(zero.add( -3).multiply(oP4h.getCircularEx()                   .subtract( oM4h.getCircularEx()))  .add(
                         zero.add( 32).multiply(oP3h.getCircularEx()                   .subtract( oM3h.getCircularEx()))) .subtract(
                         zero.add(168).multiply(oP2h.getCircularEx()                   .subtract( oM2h.getCircularEx()))) .add(
                         zero.add(672).multiply(oP1h.getCircularEx()                   .subtract( oM1h.getCircularEx())))).divide(h.multiply(840));
        jacobian[2][i] =(zero.add( -3).multiply(oP4h.getCircularEy()                   .subtract( oM4h.getCircularEy()))  .add(
                         zero.add( 32).multiply(oP3h.getCircularEy()                   .subtract( oM3h.getCircularEy()))) .subtract(
                         zero.add(168).multiply(oP2h.getCircularEy()                   .subtract( oM2h.getCircularEy()))) .add(
                         zero.add(672).multiply(oP1h.getCircularEy()                   .subtract( oM1h.getCircularEy())))).divide(h.multiply(840));
        jacobian[3][i] =(zero.add( -3).multiply(oP4h.getI()                            .subtract( oM4h.getI()))   .add(
                         zero.add( 32).multiply(oP3h.getI()                            .subtract( oM3h.getI())) ) .subtract(
                         zero.add(168).multiply(oP2h.getI()                            .subtract( oM2h.getI())) ) .add(
                         zero.add(672).multiply(oP1h.getI()                            .subtract( oM1h.getI())))).divide(h.multiply(840));
        jacobian[4][i] =(zero.add( -3).multiply(oP4h.getRightAscensionOfAscendingNode().subtract( oM4h.getRightAscensionOfAscendingNode()))   .add(
                         zero.add( 32).multiply(oP3h.getRightAscensionOfAscendingNode().subtract( oM3h.getRightAscensionOfAscendingNode()))) .subtract(
                         zero.add(168).multiply(oP2h.getRightAscensionOfAscendingNode().subtract( oM2h.getRightAscensionOfAscendingNode()))) .add(
                         zero.add(672).multiply(oP1h.getRightAscensionOfAscendingNode().subtract( oM1h.getRightAscensionOfAscendingNode())))).divide(h.multiply(840));
        jacobian[5][i] =(zero.add( -3).multiply(oP4h.getAlpha(type)                    .subtract( oM4h.getAlpha(type)))   .add(
                         zero.add( 32).multiply(oP3h.getAlpha(type)                    .subtract( oM3h.getAlpha(type)))) .subtract(
                         zero.add(168).multiply(oP2h.getAlpha(type)                    .subtract( oM2h.getAlpha(type)))) .add(
                         zero.add(672).multiply(oP1h.getAlpha(type)                    .subtract( oM1h.getAlpha(type))))).divide(h.multiply(840));

    }

    private <T extends RealFieldElement<T>> void doTestInterpolation(Field<T> field, boolean useDerivatives,
                                                                     double shiftErrorWithin, double interpolationErrorWithin,
                                                                     double shiftErrorSlightlyPast, double interpolationErrorSlightlyPast,
                                                                     double shiftErrorFarPast, double interpolationErrorFarPast)
        {
        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        final T ehMu       = zero.add(3.9860047e14);
        final double ae    = 6.378137e6;
        final double c20   = -1.08263e-3;
        final double c30   =  2.54e-6;
        final double c40   =  1.62e-6;
        final double c50   =  2.3e-7;
        final double c60   =  -5.5e-7;

        date = date.shiftedBy(584.);
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(3220103.), zero.add(69623.), zero.add(6449822.));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(6414.7), zero.add(-2006.), zero.add(-3180.));
        final FieldCircularOrbit<T> initialOrbit = new FieldCircularOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                                            FramesFactory.getEME2000(), date, ehMu);

        FieldEcksteinHechlerPropagator<T> propagator =
                new FieldEcksteinHechlerPropagator<>(initialOrbit, ae, ehMu, c20, c30, c40, c50, c60);

        // set up a 5 points sample
        List<FieldOrbit<T>> sample = new ArrayList<FieldOrbit<T>>();
        for (T dt = zero; dt.getReal() < 300.0; dt = dt.add(60.0)) {
            FieldOrbit<T> orbit = propagator.propagate(date.shiftedBy(dt)).getOrbit();
            if (!useDerivatives) {
                // remove derivatives
                T[] stateVector = MathArrays.buildArray(field, 6);
                orbit.getType().mapOrbitToArray(orbit, PositionAngle.TRUE, stateVector, null);
                orbit = orbit.getType().mapArrayToOrbit(stateVector, null, PositionAngle.TRUE,
                                                        orbit.getDate(), orbit.getMu(), orbit.getFrame());
            }
            sample.add(orbit);
        }

        // well inside the sample, interpolation should be much better than Keplerian shift
        double maxShiftError = 0.0;
        double maxInterpolationError = 0.0;
        for (T dt = zero; dt.getReal() < 241.0; dt = dt.add(1.0)) {
            FieldAbsoluteDate<T> t        = initialOrbit.getDate().shiftedBy(dt);
            FieldVector3D<T> shifted      = initialOrbit.shiftedBy(dt.getReal()).getPVCoordinates().getPosition();
            FieldVector3D<T> interpolated = initialOrbit.interpolate(t, sample).getPVCoordinates().getPosition();
            FieldVector3D<T> propagated   = propagator.propagate(t).getPVCoordinates().getPosition();
            maxShiftError = FastMath.max(maxShiftError, shifted.subtract(propagated).getNorm().getReal());
            maxInterpolationError = FastMath.max(maxInterpolationError, interpolated.subtract(propagated).getNorm().getReal());
        }
        Assert.assertEquals(shiftErrorWithin, maxShiftError, 0.01 * shiftErrorWithin);
        Assert.assertEquals(interpolationErrorWithin, maxInterpolationError, 0.01 * interpolationErrorWithin);

        // slightly past sample end, interpolation should quickly increase, but remain reasonable
        maxShiftError = 0;
        maxInterpolationError = 0;
        for (T dt = zero.add(240); dt.getReal() < 300.0; dt = dt.add(1.0)) {
            FieldAbsoluteDate<T> t        = initialOrbit.getDate().shiftedBy(dt);
            FieldVector3D<T> shifted      = initialOrbit.shiftedBy(dt).getPVCoordinates().getPosition();
            FieldVector3D<T> interpolated = initialOrbit.interpolate(t, sample).getPVCoordinates().getPosition();
            FieldVector3D<T> propagated   = propagator.propagate(t).getPVCoordinates().getPosition();
            maxShiftError = FastMath.max(maxShiftError, shifted.subtract(propagated).getNorm().getReal());
            maxInterpolationError = FastMath.max(maxInterpolationError, interpolated.subtract(propagated).getNorm().getReal());
        }
        Assert.assertEquals(shiftErrorSlightlyPast, maxShiftError, 0.01 * shiftErrorSlightlyPast);
        Assert.assertEquals(interpolationErrorSlightlyPast, maxInterpolationError, 0.01 * interpolationErrorSlightlyPast);

        // far past sample end, interpolation should become really wrong
        // (in this test case, break even occurs at around 863 seconds, with a 3.9 km error)
        maxShiftError = 0;
        maxInterpolationError = 0;
        for (T dt = zero.add(300); dt.getReal() < 1000; dt = dt.add(1.0)) {
            FieldAbsoluteDate<T> t        = initialOrbit.getDate().shiftedBy(dt);
            FieldVector3D<T> shifted      = initialOrbit.shiftedBy(dt).getPVCoordinates().getPosition();
            FieldVector3D<T> interpolated = initialOrbit.interpolate(t, sample).getPVCoordinates().getPosition();
            FieldVector3D<T> propagated   = propagator.propagate(t).getPVCoordinates().getPosition();
            maxShiftError = FastMath.max(maxShiftError, shifted.subtract(propagated).getNorm().getReal());
            maxInterpolationError = FastMath.max(maxInterpolationError, interpolated.subtract(propagated).getNorm().getReal());
        }
        Assert.assertEquals(shiftErrorFarPast, maxShiftError, 0.01 * shiftErrorFarPast);
        Assert.assertEquals(interpolationErrorFarPast, maxInterpolationError, 0.01 * interpolationErrorFarPast);

    }

    private <T extends RealFieldElement<T>> void doTestNonKeplerianDerivatives(Field<T> field) {
        final T zero = field.getZero();

        final FieldAbsoluteDate<T> date         = new FieldAbsoluteDate<>(field, "2003-05-01T00:00:20.000", TimeScalesFactory.getUTC());
        final FieldVector3D<T>     position     = new FieldVector3D<>(field.getZero().add(6896874.444705),  field.getZero().add(1956581.072644),  field.getZero().add(-147476.245054));
        final FieldVector3D<T>     velocity     = new FieldVector3D<>(field.getZero().add(166.816407662), field.getZero().add(-1106.783301861), field.getZero().add(-7372.745712770));
        final FieldVector3D <T>    acceleration = new FieldVector3D<>(field.getZero().add(-7.466182457944), field.getZero().add(-2.118153357345),  field.getZero().add(0.160004048437));
        final TimeStampedFieldPVCoordinates<T> pv = new TimeStampedFieldPVCoordinates<>(date, position, velocity, acceleration);
        final Frame frame = FramesFactory.getEME2000();
        final T mu   = zero.add(Constants.EIGEN5C_EARTH_MU);
        final FieldCircularOrbit<T> orbit = new FieldCircularOrbit<>(pv, frame, mu);

        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getA()),
                            orbit.getADot().getReal(),
                            4.3e-8);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getEquinoctialEx()),
                            orbit.getEquinoctialExDot().getReal(),
                            2.1e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getEquinoctialEy()),
                            orbit.getEquinoctialEyDot().getReal(),
                            5.4e-16);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getHx()),
                            orbit.getHxDot().getReal(),
                            1.6e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getHy()),
                            orbit.getHyDot().getReal(),
                            7.3e-17);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLv()),
                            orbit.getLvDot().getReal(),
                            3.4e-16);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLE()),
                            orbit.getLEDot().getReal(),
                            3.5e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLM()),
                            orbit.getLMDot().getReal(),
                            5.3e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getE()),
                            orbit.getEDot().getReal(),
                            6.8e-16);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getI()),
                            orbit.getIDot().getReal(),
                            5.7e-16);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getCircularEx()),
                            orbit.getCircularExDot().getReal(),
                            2.2e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getCircularEy()),
                            orbit.getCircularEyDot().getReal(),
                            5.3e-17);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getAlphaV()),
                            orbit.getAlphaVDot().getReal(),
                            4.3e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getAlphaE()),
                            orbit.getAlphaEDot().getReal(),
                            1.2e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getAlphaM()),
                            orbit.getAlphaMDot().getReal(),
                            3.7e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getAlpha(PositionAngle.TRUE)),
                            orbit.getAlphaDot(PositionAngle.TRUE).getReal(),
                            4.3e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getAlpha(PositionAngle.ECCENTRIC)),
                            orbit.getAlphaDot(PositionAngle.ECCENTRIC).getReal(),
                            1.2e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getAlpha(PositionAngle.MEAN)),
                            orbit.getAlphaDot(PositionAngle.MEAN).getReal(),
                            3.7e-15);

    }

    private <T extends RealFieldElement<T>, S extends Function<FieldCircularOrbit<T>, T>>
    double differentiate(TimeStampedFieldPVCoordinates<T> pv, Frame frame, T mu, S picker) {
        final DSFactory factory = new DSFactory(1, 1);
        FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(8, 0.1);
        UnivariateDifferentiableFunction diff = differentiator.differentiate(new UnivariateFunction() {
            public double value(double dt) {
                return picker.apply(new FieldCircularOrbit<>(pv.shiftedBy(dt), frame, mu)).getReal();
            }
        });
        return diff.value(factory.variable(0, 0.0)).getPartialDerivative(1);
     }

    private <T extends RealFieldElement<T>> void doTestPositionAngleDerivatives(final Field<T> field) {
        final T zero = field.getZero();

        final FieldAbsoluteDate<T> date         = new FieldAbsoluteDate<>(field, "2003-05-01T00:00:20.000", TimeScalesFactory.getUTC());
        final FieldVector3D<T>     position     = new FieldVector3D<>(field.getZero().add(6896874.444705),  field.getZero().add(1956581.072644),  field.getZero().add(-147476.245054));
        final FieldVector3D<T>     velocity     = new FieldVector3D<>(field.getZero().add(166.816407662), field.getZero().add(-1106.783301861), field.getZero().add(-7372.745712770));
        final FieldVector3D <T>    acceleration = new FieldVector3D<>(field.getZero().add(-7.466182457944), field.getZero().add(-2.118153357345),  field.getZero().add(0.160004048437));
        final TimeStampedFieldPVCoordinates<T> pv = new TimeStampedFieldPVCoordinates<>(date, position, velocity, acceleration);
        final Frame frame = FramesFactory.getEME2000();
        final double mu   = Constants.EIGEN5C_EARTH_MU;
        final FieldCircularOrbit<T> orbit = new FieldCircularOrbit<>(pv, frame, zero.add(mu));

        for (PositionAngle type : PositionAngle.values()) {
            final FieldCircularOrbit<T> rebuilt = new FieldCircularOrbit<>(orbit.getA(),
                                                                           orbit.getCircularEx(),
                                                                           orbit.getCircularEy(),
                                                                           orbit.getI(),
                                                                           orbit.getRightAscensionOfAscendingNode(),
                                                                           orbit.getAlpha(type),
                                                                           orbit.getADot(),
                                                                           orbit.getCircularExDot(),
                                                                           orbit.getCircularEyDot(),
                                                                           orbit.getIDot(),
                                                                           orbit.getRightAscensionOfAscendingNodeDot(),
                                                                           orbit.getAlphaDot(type),
                                                                           type, orbit.getFrame(), orbit.getDate(), orbit.getMu());
            Assert.assertThat(rebuilt.getA().getReal(),                                relativelyCloseTo(orbit.getA().getReal(),                                1));
            Assert.assertThat(rebuilt.getCircularEx().getReal(),                       relativelyCloseTo(orbit.getCircularEx().getReal(),                       1));
            Assert.assertThat(rebuilt.getCircularEy().getReal(),                       relativelyCloseTo(orbit.getCircularEy().getReal(),                       1));
            Assert.assertThat(rebuilt.getE().getReal(),                                relativelyCloseTo(orbit.getE().getReal(),                                1));
            Assert.assertThat(rebuilt.getI().getReal(),                                relativelyCloseTo(orbit.getI().getReal(),                                1));
            Assert.assertThat(rebuilt.getRightAscensionOfAscendingNode().getReal(),    relativelyCloseTo(orbit.getRightAscensionOfAscendingNode().getReal(),    1));
            Assert.assertThat(rebuilt.getADot().getReal(),                             relativelyCloseTo(orbit.getADot().getReal(),                             1));
            Assert.assertThat(rebuilt.getCircularExDot().getReal(),                    relativelyCloseTo(orbit.getCircularExDot().getReal(),                    1));
            Assert.assertThat(rebuilt.getCircularEyDot().getReal(),                    relativelyCloseTo(orbit.getCircularEyDot().getReal(),                    1));
            Assert.assertThat(rebuilt.getEDot().getReal(),                             relativelyCloseTo(orbit.getEDot().getReal(),                             1));
            Assert.assertThat(rebuilt.getIDot().getReal(),                             relativelyCloseTo(orbit.getIDot().getReal(),                             1));
            Assert.assertThat(rebuilt.getRightAscensionOfAscendingNodeDot().getReal(), relativelyCloseTo(orbit.getRightAscensionOfAscendingNodeDot().getReal(), 1));
            for (PositionAngle type2 : PositionAngle.values()) {
                Assert.assertThat(rebuilt.getAlpha(type2).getReal(),    relativelyCloseTo(orbit.getAlpha(type2).getReal(),    1));
                Assert.assertThat(rebuilt.getAlphaDot(type2).getReal(), relativelyCloseTo(orbit.getAlphaDot(type2).getReal(), 1));
            }
        }

    }

    private <T extends RealFieldElement<T>> void doTestEquatorialRetrograde(final Field<T> field) {
        T zero = field.getZero();
        FieldVector3D<T> position = new FieldVector3D<>(field.getZero().add(10000000.0), field.getZero(), field.getZero());
        FieldVector3D<T> velocity = new FieldVector3D<>(field.getZero(), field.getZero().add(-6500.0), field.getZero());
        T r2 = position.getNormSq();
        T r  = r2.sqrt();
        FieldVector3D<T> acceleration = new FieldVector3D<>(r.multiply(r2.reciprocal().multiply(-mu)), position,
                                                            field.getOne(), new FieldVector3D<>(field.getZero().add(-0.1),
                                                                                                field.getZero().add(0.2),
                                                                                                field.getZero().add(0.3)));
        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<>(position, velocity, acceleration);
        FieldCircularOrbit<T> orbit = new FieldCircularOrbit<>(pvCoordinates, FramesFactory.getEME2000(),
                                                               FieldAbsoluteDate.getJ2000Epoch(field), zero.add(mu));
        Assert.assertEquals(10637829.465, orbit.getA().getReal(), 1.0e-3);
        Assert.assertEquals(-738.145, orbit.getADot().getReal(), 1.0e-3);
        Assert.assertEquals(0.05995861, orbit.getE().getReal(), 1.0e-8);
        Assert.assertEquals(-6.523e-5, orbit.getEDot().getReal(), 1.0e-8);
        Assert.assertEquals(FastMath.PI, orbit.getI().getReal(), 1.0e-15);
        Assert.assertEquals(-4.615e-5, orbit.getIDot().getReal(), 1.0e-8);
        Assert.assertTrue(Double.isNaN(orbit.getHx().getReal()));
        Assert.assertTrue(Double.isNaN(orbit.getHxDot().getReal()));
        Assert.assertTrue(Double.isNaN(orbit.getHy().getReal()));
        Assert.assertTrue(Double.isNaN(orbit.getHyDot().getReal()));
    }

    private <T extends RealFieldElement<T>> void doTestDerivativesConversionSymmetry(Field<T> field) {
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
        FieldCircularOrbit<T> orbit = new FieldCircularOrbit<>(pvCoordinates, FramesFactory.getEME2000(),
                                                               date, zero.add(Constants.EIGEN5C_EARTH_MU));
        Assert.assertTrue(orbit.hasDerivatives());
        T r2 = position.getNormSq();
        T r  = r2.sqrt();
        FieldVector3D<T> keplerianAcceleration = new FieldVector3D<>(r.multiply(r2).reciprocal().multiply(orbit.getMu().negate()),
                                                                     position);
        Assert.assertEquals(0.0101, FieldVector3D.distance(keplerianAcceleration, acceleration).getReal(), 1.0e-4);

        for (OrbitType type : OrbitType.values()) {
            FieldOrbit<T> converted = type.convertType(orbit);
            Assert.assertTrue(converted.hasDerivatives());
            FieldCircularOrbit<T> rebuilt = (FieldCircularOrbit<T>) OrbitType.CIRCULAR.convertType(converted);
            Assert.assertTrue(rebuilt.hasDerivatives());
            Assert.assertEquals(orbit.getADot().getReal(),                             rebuilt.getADot().getReal(),                             3.0e-13);
            Assert.assertEquals(orbit.getCircularExDot().getReal(),                    rebuilt.getCircularExDot().getReal(),                    1.0e-15);
            Assert.assertEquals(orbit.getCircularEyDot().getReal(),                    rebuilt.getCircularEyDot().getReal(),                    1.0e-15);
            Assert.assertEquals(orbit.getIDot().getReal(),                             rebuilt.getIDot().getReal(),                             1.0e-15);
            Assert.assertEquals(orbit.getRightAscensionOfAscendingNodeDot().getReal(), rebuilt.getRightAscensionOfAscendingNodeDot().getReal(), 1.0e-15);
            Assert.assertEquals(orbit.getAlphaVDot().getReal(),                        rebuilt.getAlphaVDot().getReal(),                        1.0e-15);
        }

    }

    private <T extends RealFieldElement<T>> void doTestToString(Field<T> field) {
        T zero = field.getZero();
        FieldVector3D<T> position = new FieldVector3D<>(field.getZero().add(-29536113.0),
                                                        field.getZero().add(30329259.0),
                                                        field.getZero().add(-100125.0));
        FieldVector3D<T> velocity = new FieldVector3D<>(field.getZero().add(-2194.0),
                                                        field.getZero().add(-2141.0),
                                                        field.getZero().add(-8.0));
        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<>(position, velocity);
        FieldCircularOrbit<T> orbit = new FieldCircularOrbit<>(pvCoordinates, FramesFactory.getEME2000(),
                                                               FieldAbsoluteDate.getJ2000Epoch(field), zero.add(mu));
        Assert.assertEquals("circular parameters: {a: 4.225517000282565E7, ex: 0.002082917137146049, ey: 5.173980074371024E-4, i: 0.20189257051515358, raan: -87.91788415673473, alphaV: -137.84099636616548;}",
                            orbit.toString());
    }

    private <T extends RealFieldElement<T>> void doTestCopyNonKeplerianAcceleration(Field<T> field)
        {

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
        final FieldOrbit<T> orbit = new FieldCircularOrbit<>(pv, eme2000, FieldAbsoluteDate.getJ2000Epoch(field), zero.add(mu));

        // Build another KeplerianOrbit as a copy of the first one
        final FieldOrbit<T> orbitCopy = new FieldCircularOrbit<>(orbit);

        // Shift the orbit of a time-interval
        final FieldOrbit<T> shiftedOrbit     = orbit.shiftedBy(10); // This works good
        final FieldOrbit<T> shiftedOrbitCopy = orbitCopy.shiftedBy(10); // This does not work

        Assert.assertEquals(0.0,
                            FieldVector3D.distance(shiftedOrbit.getPVCoordinates().getPosition(),
                                                   shiftedOrbitCopy.getPVCoordinates().getPosition()).getReal(),
                            1.0e-10);
        Assert.assertEquals(0.0,
                            FieldVector3D.distance(shiftedOrbit.getPVCoordinates().getVelocity(),
                                                   shiftedOrbitCopy.getPVCoordinates().getVelocity()).getReal(),
                            1.0e-10);

    }

}
