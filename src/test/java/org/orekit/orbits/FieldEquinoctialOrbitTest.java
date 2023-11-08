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

import java.util.Objects;
import java.util.function.Function;

import static org.orekit.OrekitMatchers.relativelyCloseTo;


public class FieldEquinoctialOrbitTest {

    // Body mu
    private double mu;

    @BeforeEach
    public void setUp() {

        Utils.setDataRoot("regular-data");

        // Body mu
        mu = 3.9860047e14;
    }

    @Test
    public void testEquinoctialToEquinoctialEll() {
        doTestEquinoctialToEquinoctialEll(Binary64Field.getInstance());
    }

    @Test
    public void testEquinoctialToEquinoctialCirc() {
        doTestEquinoctialToEquinoctialCirc(Binary64Field.getInstance());
    }

    @Test
    public void testEquinoctialToCartesian() {
        doTestEquinoctialToCartesian(Binary64Field.getInstance());
    }

    @Test
    public void testEquinoctialToKeplerian() {
        doTestEquinoctialToKeplerian(Binary64Field.getInstance());
    }

    @Test
    public void testNumericalIssue25() {
        doTestNumericalIssue25(Binary64Field.getInstance());
    }

    @Test
    public void testAnomaly() {
        doTestAnomaly(Binary64Field.getInstance());
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
    public void testRadiusOfCurvature() {
        doTestRadiusOfCurvature(Binary64Field.getInstance());
    }

    @Test
    public void testSymmetry() {
        doTestSymmetry(Binary64Field.getInstance());
    }

    @Test
    public void testJacobianReference() {
        doTestJacobianReference(Binary64Field.getInstance());
    }

    @Test
    public void testJacobianFinitedifferences() {
        doTestJacobianFinitedifferences(Binary64Field.getInstance());
    }

    @Test
    public void testHyperbolic() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            doTestHyperbolic(Binary64Field.getInstance());
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
    public void testDerivativesConversionSymmetry() {
        doTestDerivativesConversionSymmetry(Binary64Field.getInstance());
    }

    @Test
    public void testToString() {
        doTestToString(Binary64Field.getInstance());
    }

    @Test
    public void testNonInertialFrame() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            doTestNonInertialFrame(Binary64Field.getInstance());
        });
    }

    @Test
    public void testNonKeplerianDerivatives() {
        doTestNonKeplerianDerivatives(Binary64Field.getInstance());
    }

    @Test
    public void testPositionAngleDerivatives() {
        doTestPositionAngleDerivatives(Binary64Field.getInstance());
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
    void testRemoveRates() {
        // GIVEN
        final ComplexField field = ComplexField.getInstance();
        final EquinoctialOrbit expectedOrbit = createOrbitTestFromEquinoctialOrbit(true);
        final FieldEquinoctialOrbit<Complex> fieldOrbit = new FieldEquinoctialOrbit<>(field, expectedOrbit);
        // WHEN
        final FieldEquinoctialOrbit<Complex> actualFieldOrbit = fieldOrbit.removeRates();
        // THEN
        Assertions.assertFalse(actualFieldOrbit.hasRates());
        Assertions.assertTrue(Objects.isNull(actualFieldOrbit.getADot()));
        Assertions.assertEquals(fieldOrbit.getMu(), actualFieldOrbit.getMu());
        Assertions.assertEquals(fieldOrbit.getDate(), actualFieldOrbit.getDate());
        Assertions.assertEquals(fieldOrbit.getFrame(), actualFieldOrbit.getFrame());
        Assertions.assertEquals(fieldOrbit.getPosition(), actualFieldOrbit.getPosition());
    }

    @Test
    void testFromEquinoctialOrbitWithDerivatives() {
        // GIVEN
        final ComplexField field = ComplexField.getInstance();
        final EquinoctialOrbit expectedOrbit = createOrbitTestFromEquinoctialOrbit(true);
        // WHEN
        final FieldEquinoctialOrbit<Complex> fieldOrbit = new FieldEquinoctialOrbit<>(field, expectedOrbit);
        // THEN
        compareFieldOrbitToOrbit(fieldOrbit, expectedOrbit);
    }

    @Test
    void testFromEquinoctialOrbitWithoutDerivatives() {
        // GIVEN
        final ComplexField field = ComplexField.getInstance();
        final EquinoctialOrbit expectedOrbit = createOrbitTestFromEquinoctialOrbit(false);
        // WHEN
        final FieldEquinoctialOrbit<Complex> fieldOrbit = new FieldEquinoctialOrbit<>(field, expectedOrbit);
        // THEN
        compareFieldOrbitToOrbit(fieldOrbit, expectedOrbit);
    }

    private EquinoctialOrbit createOrbitTestFromEquinoctialOrbit(final boolean withDerivatives) {
        final PositionAngleType positionAngleType = PositionAngleType.TRUE;
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame frame = FramesFactory.getEME2000();
        final double a = 10000.e3;
        final double ex = 1e-4;
        final double ey = 1e-3;
        final double hx = 1.e-2;
        final double hy = -2.e-3;
        final double lv = 0.5;
        if (withDerivatives) {
            final double derivative = 0.;
            return new EquinoctialOrbit(a, ex, ey, hx, hy, lv, derivative, derivative, derivative, derivative,
                    derivative, derivative, positionAngleType, frame, date, mu);
        } else {
            return new EquinoctialOrbit(a, ex, ey, hx, hy, lv, positionAngleType, frame, date, mu);
        }
    }

    private <T extends CalculusFieldElement<T>> void compareFieldOrbitToOrbit(final FieldEquinoctialOrbit<T> fieldOrbit,
                                                                              final EquinoctialOrbit orbit) {
        Assertions.assertEquals(orbit.getFrame(), fieldOrbit.getFrame());
        Assertions.assertEquals(orbit.getMu(), fieldOrbit.getMu().getReal());
        Assertions.assertEquals(orbit.getDate(), fieldOrbit.getDate().toAbsoluteDate());
        Assertions.assertEquals(orbit.getA(), fieldOrbit.getA().getReal());
        Assertions.assertEquals(orbit.getEquinoctialEx(), fieldOrbit.getEquinoctialEx().getReal());
        Assertions.assertEquals(orbit.getEquinoctialEy(), fieldOrbit.getEquinoctialEy().getReal());
        Assertions.assertEquals(orbit.getHx(), fieldOrbit.getHx().getReal());
        Assertions.assertEquals(orbit.getHy(), fieldOrbit.getHy().getReal());
        Assertions.assertEquals(orbit.getLv(), fieldOrbit.getLv().getReal());
        Assertions.assertEquals(orbit.hasDerivatives(), fieldOrbit.hasDerivatives());
        if (orbit.hasDerivatives()) {
            Assertions.assertEquals(orbit.getADot(), fieldOrbit.getADot().getReal());
            Assertions.assertEquals(orbit.getEquinoctialExDot(), fieldOrbit.getEquinoctialExDot().getReal());
            Assertions.assertEquals(orbit.getEquinoctialEyDot(), fieldOrbit.getEquinoctialEyDot().getReal());
            Assertions.assertEquals(orbit.getHxDot(), fieldOrbit.getHxDot().getReal());
            Assertions.assertEquals(orbit.getHyDot(), fieldOrbit.getHyDot().getReal());
            Assertions.assertEquals(orbit.getLvDot(), fieldOrbit.getLvDot().getReal());
        } else {
            Assertions.assertNull(fieldOrbit.getADot());
            Assertions.assertNull(fieldOrbit.getEquinoctialExDot());
            Assertions.assertNull(fieldOrbit.getEquinoctialEyDot());
            Assertions.assertNull(fieldOrbit.getHxDot());
            Assertions.assertNull(fieldOrbit.getHyDot());
            Assertions.assertNull(fieldOrbit.getLvDot());
        }
    }

    private <T extends CalculusFieldElement<T>> void doTestEquinoctialToEquinoctialEll(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        T ix = zero.add(1.200e-04);
        T iy = zero.add(-1.16e-04);
        T inc = ix.multiply(ix).add(iy.multiply(iy)).divide(4.).sqrt().asin().multiply(2);
        T hx = inc.divide(2.).tan().multiply(ix).divide(inc.divide(2.).sin().multiply(2));
        T hy = inc.divide(2.).tan().multiply(iy).divide(inc.divide(2.).sin().multiply(2));

        // elliptic orbit
        FieldEquinoctialOrbit<T> equi =
            new FieldEquinoctialOrbit<>(zero.add(42166712.0), zero.add( 0.5), zero.add(-0.5), hx, hy,
                                        zero.add(5.300), PositionAngleType.MEAN,
                                        FramesFactory.getEME2000(), date, zero.add(mu));
        FieldVector3D<T> pos = equi.getPosition();
        FieldVector3D<T> vit = equi.getPVCoordinates().getVelocity();

        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<>(pos, vit);

        FieldEquinoctialOrbit<T> param = new FieldEquinoctialOrbit<>(FieldPVCoordinates, FramesFactory.getEME2000(), date, zero.add(mu));
        Assertions.assertEquals(param.getA().getReal(), equi.getA().getReal(), Utils.epsilonTest * equi.getA().getReal());
        Assertions.assertEquals(param.getEquinoctialEx().getReal(), equi.getEquinoctialEx().getReal(),
                     Utils.epsilonE * FastMath.abs(equi.getE().getReal()));
        Assertions.assertEquals(param.getEquinoctialEy().getReal(), equi.getEquinoctialEy().getReal(),
                     Utils.epsilonE * FastMath.abs(equi.getE().getReal()));
        Assertions.assertEquals(param.getHx().getReal(), equi.getHx().getReal(), Utils.epsilonAngle
                     * FastMath.abs(equi.getI().getReal()));
        Assertions.assertEquals(param.getHy().getReal(), equi.getHy().getReal(), Utils.epsilonAngle
                     * FastMath.abs(equi.getI().getReal()));
        Assertions.assertEquals(MathUtils.normalizeAngle(param.getLv().getReal(), equi.getLv().getReal()), equi.getLv().getReal(),
                     Utils.epsilonAngle * FastMath.abs(equi.getLv().getReal()));

    }

    private <T extends CalculusFieldElement<T>> void doTestEquinoctialToEquinoctialCirc(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        T ix = zero.add(1.200e-04);
        T iy = zero.add(-1.16e-04);
        T inc = ix.multiply(ix).add(iy.multiply(iy)).divide(4.).sqrt().asin().multiply(2);
        T hx = inc.divide(2.).tan().multiply(ix).divide(inc.divide(2.).sin().multiply(2));
        T hy = inc.divide(2.).tan().multiply(iy).divide(inc.divide(2.).sin().multiply(2));

        // circular orbit
        FieldEquinoctialOrbit<T> equiCir =
            new FieldEquinoctialOrbit<>(zero.add(42166712.0), zero.add(0.1e-10), zero.add(-0.1e-10), hx, hy,
                                        zero.add(5.300), PositionAngleType.MEAN,
                                        FramesFactory.getEME2000(), date, zero.add(mu));
        FieldVector3D<T> posCir = equiCir.getPosition();
        FieldVector3D<T> vitCir = equiCir.getPVCoordinates().getVelocity();

        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<>(posCir, vitCir);

        FieldEquinoctialOrbit<T> paramCir = new FieldEquinoctialOrbit<>(FieldPVCoordinates, FramesFactory.getEME2000(),
                                                         date, zero.add(mu));
        Assertions.assertEquals(paramCir.getA().getReal(), equiCir.getA().getReal(), Utils.epsilonTest
                     * equiCir.getA().getReal());
        Assertions.assertEquals(paramCir.getEquinoctialEx().getReal(), equiCir.getEquinoctialEx().getReal(),
                     Utils.epsilonEcir * FastMath.abs(equiCir.getE().getReal()));
        Assertions.assertEquals(paramCir.getEquinoctialEy().getReal(), equiCir.getEquinoctialEy().getReal(),
                     Utils.epsilonEcir * FastMath.abs(equiCir.getE().getReal()));
        Assertions.assertEquals(paramCir.getHx().getReal(), equiCir.getHx().getReal(), Utils.epsilonAngle
                     * FastMath.abs(equiCir.getI().getReal()));
        Assertions.assertEquals(paramCir.getHy().getReal(), equiCir.getHy().getReal(), Utils.epsilonAngle
                     * FastMath.abs(equiCir.getI().getReal()));
        Assertions.assertEquals(MathUtils.normalizeAngle(paramCir.getLv().getReal(), equiCir.getLv().getReal()), equiCir
                     .getLv().getReal(), Utils.epsilonAngle * FastMath.abs(equiCir.getLv().getReal()));

    }

    private <T extends CalculusFieldElement<T>> void doTestEquinoctialToCartesian(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        T ix = zero.add(1.200e-04);
        T iy = zero.add(-1.16e-04);
        T inc = ix.multiply(ix).add(iy.multiply(iy)).divide(4.).sqrt().asin().multiply(2);
        T hx = inc.divide(2.).tan().multiply(ix).divide(inc.divide(2.).sin().multiply(2));
        T hy = inc.divide(2.).tan().multiply(iy).divide(inc.divide(2.).sin().multiply(2));

        FieldEquinoctialOrbit<T> equi =
                        new FieldEquinoctialOrbit<>(zero.add(42166712.0), zero.add(-7.900e-06), zero.add(1.100e-04), hx, hy,
                                                    zero.add(5.300), PositionAngleType.MEAN,
                                                   FramesFactory.getEME2000(), date, zero.add(mu));
        FieldVector3D<T> pos = equi.getPosition();
        FieldVector3D<T> vit = equi.getPVCoordinates().getVelocity();

        // verif of 1/a = 2/X - V2/mu
        T oneovera = (pos.getNorm().reciprocal().multiply(2)).subtract(vit.getNorm().multiply(vit.getNorm()).divide(mu));
        Assertions.assertEquals(oneovera.getReal(), 1. / equi.getA().getReal(), 1.0e-7);

        Assertions.assertEquals(0.233745668678733e+08, pos.getX().getReal(), Utils.epsilonTest
                     * FastMath.abs(pos.getX().getReal()));
        Assertions.assertEquals(-0.350998914352669e+08, pos.getY().getReal(), Utils.epsilonTest
                     * FastMath.abs(pos.getY().getReal()));
        Assertions.assertEquals(-0.150053723123334e+04, pos.getZ().getReal(), Utils.epsilonTest
                     * FastMath.abs(pos.getZ().getReal()));

        Assertions.assertEquals(2558.7096558809967, vit.getX().getReal(), Utils.epsilonTest
                     * FastMath.abs(vit.getX().getReal()));
        Assertions.assertEquals(1704.1586039092576, vit.getY().getReal(), Utils.epsilonTest
                     * FastMath.abs(vit.getY().getReal()));
        Assertions.assertEquals(0.5013093577879, vit.getZ().getReal(), Utils.epsilonTest
                     * FastMath.abs(vit.getZ().getReal()));
    }

    private <T extends CalculusFieldElement<T>> void doTestEquinoctialToKeplerian(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        T ix = zero.add(1.200e-04);
        T iy = zero.add(-1.16e-04);
        T inc = ix.multiply(ix).add(iy.multiply(iy)).divide(4.).sqrt().asin().multiply(2);
        T hx = inc.divide(2.).tan().multiply(ix).divide(inc.divide(2.).sin().multiply(2));
        T hy = inc.divide(2.).tan().multiply(iy).divide(inc.divide(2.).sin().multiply(2));

        FieldEquinoctialOrbit<T> equi =
                        new FieldEquinoctialOrbit<>(zero.add(42166712.0), zero.add(-7.900e-06), zero.add(1.100e-04), hx, hy,
                                                    zero.add(5.300), PositionAngleType.MEAN,
                                                    FramesFactory.getEME2000(), date, zero.add(mu));

        FieldKeplerianOrbit<T> kep = new FieldKeplerianOrbit<>(equi);

        Assertions.assertEquals(42166712.000, equi.getA().getReal(), Utils.epsilonTest * kep.getA().getReal());
        Assertions.assertEquals(0.110283316961361e-03, kep.getE().getReal(), Utils.epsilonE
                     * FastMath.abs(kep.getE().getReal()));
        Assertions.assertEquals(0.166901168553917e-03, kep.getI().getReal(), Utils.epsilonAngle
                     * FastMath.abs(kep.getI().getReal()));
        Assertions.assertEquals(MathUtils.normalizeAngle(-3.87224326008837, kep.getPerigeeArgument().getReal()),
                     kep.getPerigeeArgument().getReal(), Utils.epsilonTest
                     * FastMath.abs(kep.getPerigeeArgument().getReal()));
        Assertions.assertEquals(MathUtils.normalizeAngle(5.51473467358854, kep
                                     .getRightAscensionOfAscendingNode().getReal()), kep
                                     .getRightAscensionOfAscendingNode().getReal(), Utils.epsilonTest
                                     * FastMath.abs(kep.getRightAscensionOfAscendingNode().getReal()));
        Assertions.assertEquals(MathUtils.normalizeAngle(3.65750858649982, kep.getMeanAnomaly().getReal()), kep
                     .getMeanAnomaly().getReal(), Utils.epsilonTest * FastMath.abs(kep.getMeanAnomaly().getReal()));

    }

    private <T extends CalculusFieldElement<T>> void doTestHyperbolic(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        new FieldEquinoctialOrbit<>(zero.add(42166712.0), zero.add(0.9), zero.add(0.5), zero.add(0.01), zero.add(-0.02), zero.add(5.300),
                             PositionAngleType.MEAN,  FramesFactory.getEME2000(), date, zero.add(mu));
    }

    private <T extends CalculusFieldElement<T>> void doTestToOrbitWithoutDerivatives(Field<T> field) {
        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));
        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<>(position, velocity);
        FieldEquinoctialOrbit<T>  fieldOrbit = new FieldEquinoctialOrbit<>(pvCoordinates, FramesFactory.getEME2000(), date, zero.add(mu));
        EquinoctialOrbit orbit = fieldOrbit.toOrbit();
        Assertions.assertFalse(orbit.hasDerivatives());
        MatcherAssert.assertThat(orbit.getA(),                             relativelyCloseTo(fieldOrbit.getA().getReal(),             0));
        MatcherAssert.assertThat(orbit.getEquinoctialEx(),                 relativelyCloseTo(fieldOrbit.getEquinoctialEx().getReal(), 0));
        MatcherAssert.assertThat(orbit.getEquinoctialEy(),                 relativelyCloseTo(fieldOrbit.getEquinoctialEy().getReal(), 0));
        MatcherAssert.assertThat(orbit.getHx(),                            relativelyCloseTo(fieldOrbit.getHx().getReal(),            0));
        MatcherAssert.assertThat(orbit.getHy(),                            relativelyCloseTo(fieldOrbit.getHy().getReal(),            0));
        MatcherAssert.assertThat(orbit.getLv(),                            relativelyCloseTo(fieldOrbit.getLv().getReal(),            0));
        Assertions.assertTrue(Double.isNaN(orbit.getADot()));
        Assertions.assertTrue(Double.isNaN(orbit.getEquinoctialExDot()));
        Assertions.assertTrue(Double.isNaN(orbit.getEquinoctialEyDot()));
        Assertions.assertTrue(Double.isNaN(orbit.getHxDot()));
        Assertions.assertTrue(Double.isNaN(orbit.getHyDot()));
        Assertions.assertTrue(Double.isNaN(orbit.getLvDot()));
        Assertions.assertTrue(Double.isNaN(orbit.getLEDot()));
        Assertions.assertTrue(Double.isNaN(orbit.getLMDot()));
        Assertions.assertTrue(Double.isNaN(orbit.getEDot()));
        Assertions.assertTrue(Double.isNaN(orbit.getIDot()));
        Assertions.assertTrue(Double.isNaN(orbit.getLDot(PositionAngleType.TRUE)));
        Assertions.assertTrue(Double.isNaN(orbit.getLDot(PositionAngleType.ECCENTRIC)));
        Assertions.assertTrue(Double.isNaN(orbit.getLDot(PositionAngleType.MEAN)));
    }

    private <T extends CalculusFieldElement<T>> void doTestToOrbitWithDerivatives(Field<T> field) {
        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));
        T r2 = position.getNormSq();
        T r = r2.sqrt();
        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<>(position, velocity,
                                                                       new FieldVector3D<>(r.multiply(r2).reciprocal().multiply(-mu),
                                                                                           position));
        FieldEquinoctialOrbit<T>  fieldOrbit = new FieldEquinoctialOrbit<>(pvCoordinates, FramesFactory.getEME2000(), date, zero.add(mu));
        EquinoctialOrbit orbit = fieldOrbit.toOrbit();
        Assertions.assertTrue(orbit.hasDerivatives());
        MatcherAssert.assertThat(orbit.getA(),                             relativelyCloseTo(fieldOrbit.getA().getReal(),                           0));
        MatcherAssert.assertThat(orbit.getEquinoctialEx(),                 relativelyCloseTo(fieldOrbit.getEquinoctialEx().getReal(),               0));
        MatcherAssert.assertThat(orbit.getEquinoctialEy(),                 relativelyCloseTo(fieldOrbit.getEquinoctialEy().getReal(),               0));
        MatcherAssert.assertThat(orbit.getHx(),                            relativelyCloseTo(fieldOrbit.getHx().getReal(),                          0));
        MatcherAssert.assertThat(orbit.getHy(),                            relativelyCloseTo(fieldOrbit.getHy().getReal(),                          0));
        MatcherAssert.assertThat(orbit.getLv(),                            relativelyCloseTo(fieldOrbit.getLv().getReal(),                          0));
        MatcherAssert.assertThat(orbit.getADot(),                          relativelyCloseTo(fieldOrbit.getADot().getReal(),                        0));
        MatcherAssert.assertThat(orbit.getEquinoctialExDot(),              relativelyCloseTo(fieldOrbit.getEquinoctialExDot().getReal(),            0));
        MatcherAssert.assertThat(orbit.getEquinoctialEyDot(),              relativelyCloseTo(fieldOrbit.getEquinoctialEyDot().getReal(),            0));
        MatcherAssert.assertThat(orbit.getHxDot(),                         relativelyCloseTo(fieldOrbit.getHxDot().getReal(),                       0));
        MatcherAssert.assertThat(orbit.getHyDot(),                         relativelyCloseTo(fieldOrbit.getHyDot().getReal(),                       0));
        MatcherAssert.assertThat(orbit.getLvDot(),                         relativelyCloseTo(fieldOrbit.getLvDot().getReal(),                       0));
        MatcherAssert.assertThat(orbit.getLEDot(),                         relativelyCloseTo(fieldOrbit.getLEDot().getReal(),                       0));
        MatcherAssert.assertThat(orbit.getLMDot(),                         relativelyCloseTo(fieldOrbit.getLMDot().getReal(),                       0));
        MatcherAssert.assertThat(orbit.getEDot(),                          relativelyCloseTo(fieldOrbit.getEDot().getReal(),                        0));
        MatcherAssert.assertThat(orbit.getIDot(),                          relativelyCloseTo(fieldOrbit.getIDot().getReal(),                        0));
        MatcherAssert.assertThat(orbit.getLDot(PositionAngleType.TRUE),        relativelyCloseTo(fieldOrbit.getLDot(PositionAngleType.TRUE).getReal(),      0));
        MatcherAssert.assertThat(orbit.getLDot(PositionAngleType.ECCENTRIC),   relativelyCloseTo(fieldOrbit.getLDot(PositionAngleType.ECCENTRIC).getReal(), 0));
        MatcherAssert.assertThat(orbit.getLDot(PositionAngleType.MEAN),        relativelyCloseTo(fieldOrbit.getLDot(PositionAngleType.MEAN).getReal(),      0));
    }

    private <T extends CalculusFieldElement<T>> void doTestNumericalIssue25(Field<T> field) {
        T zero = field.getZero();
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(3782116.14107698), zero.add(416663.11924914), zero.add(5875541.62103057));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-6349.7848910501), zero.add(288.4061811651), zero.add(4066.9366759691));
        FieldEquinoctialOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                                     FramesFactory.getEME2000(),
                                                                     new FieldAbsoluteDate<>(field, "2004-01-01T23:00:00.000",
                                                                                             TimeScalesFactory.getUTC()),
                                                                     zero.add(3.986004415E14));
        Assertions.assertEquals(0.0, orbit.getE().getReal(), 2.0e-14);
    }

    private <T extends CalculusFieldElement<T>> void doTestAnomaly(Field<T> field) {

        T one = field.getOne();
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);
        // elliptic orbit
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        FieldEquinoctialOrbit<T> p = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position, velocity), FramesFactory.getEME2000(), date, zero.add(mu));
        FieldKeplerianOrbit<T> kep = new FieldKeplerianOrbit<>(p);

        T e = p.getE();
        T eRatio = one.subtract(e).divide(e.add(1)).sqrt();
        T paPraan = kep.getPerigeeArgument().add(
                       kep.getRightAscensionOfAscendingNode());

        T lv = zero.add(1.1);
        // formulations for elliptic case
        T lE = eRatio.multiply(lv.subtract(paPraan).divide(2).tan()).atan().multiply(2).add(paPraan);
        T lM = lE.subtract(e.multiply(lE.subtract(paPraan).sin()));

        p = new FieldEquinoctialOrbit<>(p.getA(), p.getEquinoctialEx(),
                                        p.getEquinoctialEy() , p.getHx(), p.getHy() , lv , PositionAngleType.TRUE,
                                        p.getFrame(), p.getDate(), p.getMu());
        Assertions.assertEquals(p.getLv().getReal(), lv.getReal(), Utils.epsilonAngle * FastMath.abs(lv.getReal()));
        Assertions.assertEquals(p.getLE().getReal(), lE.getReal(), Utils.epsilonAngle * FastMath.abs(lE.getReal()));
        Assertions.assertEquals(p.getLM().getReal(), lM.getReal(), Utils.epsilonAngle * FastMath.abs(lM.getReal()));
        p = new FieldEquinoctialOrbit<>(p.getA(), p.getEquinoctialEx(),
                                        p.getEquinoctialEy() , p.getHx(), p.getHy() , zero , PositionAngleType.TRUE,
                                        p.getFrame(), p.getDate(), p.getMu());

        p = new FieldEquinoctialOrbit<>(p.getA(), p.getEquinoctialEx(),
                                        p.getEquinoctialEy() , p.getHx(), p.getHy() , lE , PositionAngleType.ECCENTRIC,
                                        p.getFrame(), p.getDate(), p.getMu());
        Assertions.assertEquals(p.getLv().getReal(), lv.getReal(), Utils.epsilonAngle * FastMath.abs(lv.getReal()));
        Assertions.assertEquals(p.getLE().getReal(), lE.getReal(), Utils.epsilonAngle * FastMath.abs(lE.getReal()));
        Assertions.assertEquals(p.getLM().getReal(), lM.getReal(), Utils.epsilonAngle * FastMath.abs(lM.getReal()));
        p = new FieldEquinoctialOrbit<>(p.getA(), p.getEquinoctialEx(),
                                        p.getEquinoctialEy() , p.getHx(), p.getHy() , zero , PositionAngleType.TRUE,
                                        p.getFrame(), p.getDate(), p.getMu());

        p = new FieldEquinoctialOrbit<>(p.getA(), p.getEquinoctialEx(),
                                        p.getEquinoctialEy() , p.getHx(), p.getHy() , lM , PositionAngleType.MEAN,
                                      p.getFrame(), p.getDate(), p.getMu());
        Assertions.assertEquals(p.getLv  ().getReal(), lv.getReal(), Utils.epsilonAngle * FastMath.abs(lv.getReal()));
        Assertions.assertEquals(p.getLE().getReal(), lE.getReal(), Utils.epsilonAngle * FastMath.abs(lE.getReal()));
        Assertions.assertEquals(p.getLM().getReal(), lM.getReal(), Utils.epsilonAngle * FastMath.abs(lM.getReal()));

        // circular orbit
        p = new FieldEquinoctialOrbit<>(p.getA(), zero ,
                                        zero, p.getHx(), p.getHy() , p.getLv() , PositionAngleType.TRUE,
                                        p.getFrame(), p.getDate(), p.getMu());

        lE = lv;
        lM = lE;

        p = new FieldEquinoctialOrbit<>(p.getA(), p.getEquinoctialEx(),
                                        p.getEquinoctialEy() , p.getHx(), p.getHy() , lv , PositionAngleType.TRUE,
                                        p.getFrame(), p.getDate(), p.getMu());
        Assertions.assertEquals(p.getLv().getReal(), lv.getReal(), Utils.epsilonAngle * FastMath.abs(lv.getReal()));
        Assertions.assertEquals(p.getLE().getReal(), lE.getReal(), Utils.epsilonAngle * FastMath.abs(lE.getReal()));
        Assertions.assertEquals(p.getLM().getReal(), lM.getReal(), Utils.epsilonAngle * FastMath.abs(lM.getReal()));
        p = new FieldEquinoctialOrbit<>(p.getA(), p.getEquinoctialEx(),
                                        p.getEquinoctialEy() , p.getHx(), p.getHy() , zero , PositionAngleType.TRUE,
                                        p.getFrame(), p.getDate(), p.getMu());

        p = new FieldEquinoctialOrbit<>(p.getA(), p.getEquinoctialEx(),
                                        p.getEquinoctialEy() , p.getHx(), p.getHy() , lE , PositionAngleType.ECCENTRIC,
                                        p.getFrame(), p.getDate(), p.getMu());
        Assertions.assertEquals(p.getLv().getReal(), lv.getReal(), Utils.epsilonAngle * FastMath.abs(lv.getReal()));
        Assertions.assertEquals(p.getLE().getReal(), lE.getReal(), Utils.epsilonAngle * FastMath.abs(lE.getReal()));
        Assertions.assertEquals(p.getLM().getReal(), lM.getReal(), Utils.epsilonAngle * FastMath.abs(lM.getReal()));
        p = new FieldEquinoctialOrbit<>(p.getA(), p.getEquinoctialEx(),
                                        p.getEquinoctialEy() , p.getHx(), p.getHy() , zero , PositionAngleType.TRUE,
                                        p.getFrame(), p.getDate(), p.getMu());

        p = new FieldEquinoctialOrbit<>(p.getA(), p.getEquinoctialEx(),
                                        p.getEquinoctialEy() , p.getHx(), p.getHy() , lM , PositionAngleType.MEAN, p.getFrame(), p.getDate(), p.getMu());
        Assertions.assertEquals(p.getLv().getReal(), lv.getReal(), Utils.epsilonAngle * FastMath.abs(lv.getReal()));
        Assertions.assertEquals(p.getLE().getReal(), lE.getReal(), Utils.epsilonAngle * FastMath.abs(lE.getReal()));
        Assertions.assertEquals(p.getLM().getReal(), lM.getReal(), Utils.epsilonAngle * FastMath.abs(lM.getReal()));
    }

    private <T extends CalculusFieldElement<T>> void doTestPositionVelocityNorms(Field<T> field) {

        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        // elliptic and non equatorial (i retrograde) orbit
        FieldEquinoctialOrbit<T> p =
            new FieldEquinoctialOrbit<>(zero.add(42166712.0), zero.add(0.5), zero.add(-0.5), zero.add(1.200), zero.add(2.1),
                                        zero.add(0.67), PositionAngleType.TRUE,
                                        FramesFactory.getEME2000(), date, zero.add(mu));

        T ex = p.getEquinoctialEx();
        T ey = p.getEquinoctialEy();
        T lv = p.getLv();
        T ksi = ex.multiply(lv.cos()).add(ey.multiply(lv.sin())).add(1);
        T nu = ex.multiply(lv.sin()).subtract(ey.multiply(lv.cos()));
        T epsilon = ex.negate().multiply(ex).subtract(ey.multiply(ey)).add(1.0).sqrt();

        T a = p.getA();
        T na = a.reciprocal().multiply(p.getMu()).sqrt();

        Assertions.assertEquals(a.getReal() * epsilon.getReal() * epsilon.getReal() / ksi.getReal(),
                            p.getPosition().getNorm().getReal(),
                     Utils.epsilonTest * FastMath.abs(p.getPosition().getNorm().getReal()));
        Assertions.assertEquals(na.getReal() * FastMath.sqrt(ksi.getReal() * ksi.getReal() + nu.getReal() * nu.getReal()) / epsilon.getReal(),
                            p.getPVCoordinates().getVelocity().getNorm().getReal(),
                            Utils.epsilonTest * FastMath.abs(p.getPVCoordinates().getVelocity().getNorm().getReal()));

        // circular and equatorial orbit
        FieldEquinoctialOrbit<T> pCirEqua =
            new FieldEquinoctialOrbit<>(zero.add(42166712.0), zero.add(0.1e-8), zero.add(0.1e-8), zero.add(0.1e-8), zero.add(0.1e-8),
                                        zero.add(0.67), PositionAngleType.TRUE,
                                        FramesFactory.getEME2000(), date, zero.add(mu));

        ex = pCirEqua.getEquinoctialEx();
        ey = pCirEqua.getEquinoctialEy();
        lv = pCirEqua.getLv();
        ksi = ex.multiply(lv.cos()).add(ey.multiply(lv.sin())).add(1);
        nu = ex.multiply(lv.sin()).subtract(ey.multiply(lv.cos()));
        epsilon = ex.negate().multiply(ex).subtract(ey.multiply(ey)).add(1.0).sqrt();

        a = pCirEqua.getA();
        na = a.reciprocal().multiply(p.getMu()).sqrt();

        Assertions.assertEquals(a.getReal() * epsilon.getReal() * epsilon.getReal() / ksi.getReal(), pCirEqua.getPosition()
                            .getNorm().getReal(), Utils.epsilonTest
                            * FastMath.abs(pCirEqua.getPosition().getNorm().getReal()));
        Assertions.assertEquals(na.getReal() * FastMath.sqrt(ksi.getReal() * ksi.getReal() + nu.getReal() * nu.getReal()) / epsilon.getReal(),
                            pCirEqua.getPVCoordinates().getVelocity().getNorm().getReal(), Utils.epsilonTest
                            * FastMath.abs(pCirEqua.getPVCoordinates().getVelocity().getNorm().getReal()));
    }

    private <T extends CalculusFieldElement<T>> void doTestGeometry(Field<T> field) {


        T one = field.getOne();
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);


        // elliptic and non equatorial (i retrograde) orbit
        FieldEquinoctialOrbit<T> p =
            new FieldEquinoctialOrbit<>(zero.add(42166712.0), zero.add(0.5), zero.add(-0.5), zero.add(1.200), zero.add(2.1),
                                        zero.add(0.67), PositionAngleType.TRUE,
                                        FramesFactory.getEME2000(), date, zero.add(mu));

        FieldVector3D<T> position = p.getPosition();
        FieldVector3D<T> velocity = p.getPVCoordinates().getVelocity();
        FieldVector3D<T> momentum = p.getPVCoordinates().getMomentum().normalize();

        T apogeeRadius = p.getA().multiply(p.getE().add(1.0));
        T perigeeRadius = p.getA().multiply(one.subtract(p.getE()));

        for (T lv = zero; lv.getReal() <= 2 * FastMath.PI; lv = lv.add(2 * FastMath.PI / 100.)) {
            p = new FieldEquinoctialOrbit<>(p.getA(), p.getEquinoctialEx(),
                                            p.getEquinoctialEy() , p.getHx(), p.getHy() , lv , PositionAngleType.TRUE,
                                            p.getFrame(), p.getDate(), p.getMu());
            position = p.getPosition();

            // test if the norm of the position is in the range [perigee radius,
            // apogee radius]
            // Warning: these tests are without absolute value by choice
            Assertions.assertTrue((position.getNorm().getReal() - apogeeRadius.getReal()) <= (apogeeRadius.getReal() * Utils.epsilonTest));
            Assertions.assertTrue((position.getNorm().getReal() - perigeeRadius.getReal()) >= (-perigeeRadius.getReal() * Utils.epsilonTest));

            position = position.normalize();
            velocity = p.getPVCoordinates().getVelocity();
            velocity = velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and
            // momemtum) are normalized here

            // test of orthogonality between position and momentum
            Assertions.assertTrue(FastMath.abs(FieldVector3D.dotProduct(position, momentum).getReal()) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assertions.assertTrue(FastMath.abs(FieldVector3D.dotProduct(velocity, momentum).getReal()) < Utils.epsilonTest);
        }


        // circular and equatorial orbit
        FieldEquinoctialOrbit<T> pCirEqua =
            new FieldEquinoctialOrbit<>(zero.add(42166712.0), zero.add(0.1e-8), zero.add(0.1e-8), zero.add(0.1e-8), zero.add(0.1e-8),
                                        zero.add(0.67), PositionAngleType.TRUE,
                                        FramesFactory.getEME2000(), date, zero.add(mu));

        position = pCirEqua.getPosition();
        velocity = pCirEqua.getPVCoordinates().getVelocity();

        momentum = FieldVector3D.crossProduct(position, velocity).normalize();

        apogeeRadius = pCirEqua.getA().multiply(pCirEqua.getE().add(1));
        perigeeRadius = pCirEqua.getA().multiply(one.subtract(pCirEqua.getE()));
        // test if apogee equals perigee
        Assertions.assertEquals(perigeeRadius.getReal(), apogeeRadius.getReal(), 1.e+4 * Utils.epsilonTest
                     * apogeeRadius.getReal());

        for (T lv = zero; lv.getReal() <= 2 * FastMath.PI; lv =lv.add(2 * FastMath.PI / 100.)) {
            pCirEqua = new FieldEquinoctialOrbit<>(pCirEqua.getA(), pCirEqua.getEquinoctialEx(),
                                                   pCirEqua.getEquinoctialEy() , pCirEqua.getHx(), pCirEqua.getHy() , lv , PositionAngleType.TRUE,
                                                   pCirEqua.getFrame(), p.getDate(), p.getMu());
            position = pCirEqua.getPosition();

            // test if the norm pf the position is in the range [perigee radius,
            // apogee radius]
            Assertions.assertTrue((position.getNorm().getReal() - apogeeRadius.getReal()) <= (apogeeRadius.getReal() * Utils.epsilonTest));
            Assertions.assertTrue((position.getNorm().getReal() - perigeeRadius.getReal()) >= (-perigeeRadius.getReal() * Utils.epsilonTest));

            position = position.normalize();
            velocity = pCirEqua.getPVCoordinates().getVelocity();
            velocity = velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and
            // momemtum) are normalized here

            // test of orthogonality between position and momentum
            Assertions.assertTrue(FastMath.abs(FieldVector3D.dotProduct(position, momentum).getReal()) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assertions.assertTrue(FastMath.abs(FieldVector3D.dotProduct(velocity, momentum).getReal()) < Utils.epsilonTest);
        }
    }

    private <T extends CalculusFieldElement<T>> void doTestRadiusOfCurvature(Field<T> field) {
        T one = field.getOne();
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        // elliptic and non equatorial (i retrograde) orbit
        FieldEquinoctialOrbit<T> p =
            new FieldEquinoctialOrbit<>(zero.add(42166712.0), zero.add(0.5), zero.add(-0.5), zero.add(1.200), zero.add(2.1),
                                        zero.add(0.67), PositionAngleType.TRUE,
                                        FramesFactory.getEME2000(), date, zero.add(mu));

        // arbitrary orthogonal vectors in the orbital plane
        FieldVector3D<T> u = p.getPVCoordinates().getMomentum().orthogonal();
        FieldVector3D<T> v = FieldVector3D.crossProduct(p.getPVCoordinates().getMomentum(), u).normalize();

        // compute radius of curvature in the orbital plane from Cartesian coordinates
        T xDot    = FieldVector3D.dotProduct(p.getPVCoordinates().getVelocity(),     u);
        T yDot    = FieldVector3D.dotProduct(p.getPVCoordinates().getVelocity(),     v);
        T xDotDot = FieldVector3D.dotProduct(p.getPVCoordinates().getAcceleration(), u);
        T yDotDot = FieldVector3D.dotProduct(p.getPVCoordinates().getAcceleration(), v);
        T dot2    = xDot.multiply(xDot).add(yDot.multiply(yDot));
        T rCart   = dot2.multiply(dot2.sqrt()).divide(
                         xDot.multiply(yDotDot).subtract(yDot.multiply(xDotDot).abs()));

        // compute radius of curvature in the orbital plane from orbital parameters
        T ex   = p.getEquinoctialEx();
        T ey   = p.getEquinoctialEy();
        T f    = ex.multiply(p.getLE().cos()).add( ey.multiply(p.getLE().sin()));

        T oMf2 = one.subtract(f.multiply(f));
        T rOrb = p.getA().multiply(oMf2).multiply(oMf2.divide(
                                         one.subtract(ex.multiply(ex).add(ey.multiply(ey)))).sqrt());

        // both methods to compute radius of curvature should match
        Assertions.assertEquals(rCart.getReal(), rOrb.getReal(), 2.0e-15 * p.getA().getReal());

        // at this place for such an eccentric orbit,
        // the radius of curvature is much smaller than semi major axis
        Assertions.assertEquals(0.8477 * p.getA().getReal(), rCart.getReal(), 1.0e-4 * p.getA().getReal());

    }

    private <T extends CalculusFieldElement<T>> void doTestSymmetry(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        // elliptic and non equatorial orbit
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(4512.9), zero.add(18260.), zero.add( -5127.));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(134664.6), zero.add(90066.8), zero.add(72047.6));

        FieldEquinoctialOrbit<T> p = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                                 FramesFactory.getEME2000(), date, zero.add(mu));

        FieldVector3D<T> positionOffset = p.getPosition().subtract(position);
        FieldVector3D<T> velocityOffset = p.getPVCoordinates().getVelocity().subtract(velocity);

        Assertions.assertEquals(0, positionOffset.getNorm().getReal(), 7.5e-12);
        Assertions.assertEquals(0, velocityOffset.getNorm().getReal(), 1.0e-15);

        // circular and equatorial orbit
        position = new FieldVector3D<>(zero.add(33051.2), zero.add(26184.9), zero.add(-1.3E-5));
        velocity = new FieldVector3D<>(zero.add(-60376.2), zero.add(76208.), zero.add(2.7E-4));

        p = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                        FramesFactory.getEME2000(), date, zero.add(mu));

        positionOffset = p.getPosition().subtract(position);
        velocityOffset = p.getPVCoordinates().getVelocity().subtract(velocity);

        Assertions.assertEquals(0, positionOffset.getNorm().getReal(), 1.1e-11);
        Assertions.assertEquals(0, velocityOffset.getNorm().getReal(), 1.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void doTestNonInertialFrame(Field<T> field) throws IllegalArgumentException {

        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field);

        FieldVector3D<T> position = new FieldVector3D<>(zero.add(4512.9), zero.add(18260.), zero.add(-5127.));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(134664.6), zero.add(90066.8), zero.add(72047.6));
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<>( position, velocity);
        new FieldEquinoctialOrbit<>(FieldPVCoordinates,
                                    new Frame(FramesFactory.getEME2000(), Transform.IDENTITY, "non-inertial", false),
                                    date, zero.add(mu));
    }

    private <T extends CalculusFieldElement<T>> void doTestJacobianReference(Field<T> field) {

        T zero = field.getZero();
        FieldAbsoluteDate<T> dateTca = new FieldAbsoluteDate<>(field, 2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        FieldEquinoctialOrbit<T> orbEqu = new FieldEquinoctialOrbit<>(zero.add(7000000.0), zero.add(0.01), zero.add(-0.02), zero.add(1.2), zero.add(2.1),
                                                                      zero.add(FastMath.toRadians(40.)), PositionAngleType.MEAN,
                                                                      FramesFactory.getEME2000(), dateTca, zero.add(mu));

        // the following reference values have been computed using the free software
        // version 6.2 of the MSLIB fortran library by the following program:
        //         program equ_jacobian
        //
        //         use mslib
        //         implicit none
        //
        //         integer, parameter :: nb = 11
        //         integer :: i,j
        //         type(tm_code_retour)      ::  code_retour
        //
        //         real(pm_reel), parameter :: mu= 3.986004415e+14_pm_reel
        //         real(pm_reel),dimension(3)::vit_car,pos_car
        //         type(tm_orb_cir_equa)::cir_equa
        //         real(pm_reel), dimension(6,6)::jacob
        //         real(pm_reel)::norme,hx,hy,f,dix,diy
        //         intrinsic sqrt
        //
        //         cir_equa%a=7000000_pm_reel
        //         cir_equa%ex=0.01_pm_reel
        //         cir_equa%ey=-0.02_pm_reel
        //
        //         ! mslib cir-equ parameters use ix = 2 sin(i/2) cos(gom) and iy = 2 sin(i/2) sin(gom)
        //         ! equinoctial parameters use hx = tan(i/2) cos(gom) and hy = tan(i/2) sin(gom)
        //         ! the conversions between these parameters and their differentials can be computed
        //         ! from the ratio f = 2cos(i/2) which can be found either from (ix, iy) or (hx, hy):
        //         !   f = sqrt(4 - ix^2 - iy^2) =  2 / sqrt(1 + hx^2 + hy^2)
        //         !  hx = ix / f,  hy = iy / f
        //         !  ix = hx * f, iy = hy *f
        //         ! dhx = ((1 + hx^2) / f) dix + (hx hy / f) diy, dhy = (hx hy / f) dix + ((1 + hy^2) /f) diy
        //         ! dix = ((1 - ix^2 / 4) f dhx - (ix iy / 4) f dhy, diy = -(ix iy / 4) f dhx + (1 - iy^2 / 4) f dhy
        //         hx=1.2_pm_reel
        //         hy=2.1_pm_reel
        //         f=2_pm_reel/sqrt(1+hx*hx+hy*hy)
        //         cir_equa%ix=hx*f
        //         cir_equa%iy=hy*f
        //
        //         cir_equa%pso_M=40_pm_reel*pm_deg_rad
        //
        //         call mv_cir_equa_car(mu,cir_equa,pos_car,vit_car,code_retour)
        //         write(*,*)code_retour%valeur
        //         write(*,1000)pos_car,vit_car
        //
        //
        //         call mu_norme(pos_car,norme,code_retour)
        //         write(*,*)norme
        //
        //         call mv_car_cir_equa (mu, pos_car, vit_car, cir_equa, code_retour, jacob)
        //         write(*,*)code_retour%valeur
        //
        //         f=sqrt(4_pm_reel-cir_equa%ix*cir_equa%ix-cir_equa%iy*cir_equa%iy)
        //         hx=cir_equa%ix/f
        //         hy=cir_equa%iy/f
        //         write(*,*)"ix = ", cir_equa%ix, ", iy = ", cir_equa%iy
        //         write(*,*)"equinoctial = ", cir_equa%a, cir_equa%ex, cir_equa%ey, hx, hy, cir_equa%pso_M*pm_rad_deg
        //
        //         do j = 1,6
        //           dix=jacob(4,j)
        //           diy=jacob(5,j)
        //           jacob(4,j)=((1_pm_reel+hx*hx)*dix+(hx*hy)*diy)/f
        //           jacob(5,j)=((hx*hy)*dix+(1_pm_reel+hy*hy)*diy)/f
        //         end do
        //
        //         do i = 1,6
        //            write(*,*) " ",(jacob(i,j),j=1,6)
        //         end do
        //
        //         1000 format (6(f24.15,1x))
        //         end program equ_jacobian
        FieldVector3D<T> pRef = new FieldVector3D<>(zero.add(2004367.298657628707588), zero.add(6575317.978060320019722), zero.add(-1518024.843913963763043));
        FieldVector3D<T> vRef = new FieldVector3D<>(zero.add(5574.048661495634406), zero.add(-368.839015744295409), zero.add(5009.529487849066754));
        double[][] jRefR = {
            {  0.56305379787310628,        1.8470954710993663,      -0.42643364527246025,        1370.4369387322224,       -90.682848736736688 ,       1231.6441195141242      },
            {  9.52434720041122055E-008,  9.49704503778007296E-008,  4.46607520107935678E-008,  1.69704446323098610E-004,  7.05603505855828105E-005,  1.14825140460141970E-004 },
            { -5.41784097802642701E-008,  9.54903765833015538E-008, -8.95815777332234450E-008,  1.01864980963344096E-004, -1.03194262242761416E-004,  1.40668700715197768E-004 },
            {  1.96680305426455816E-007, -1.12388745957974467E-007, -2.27118924123407353E-007,  2.06472886488132167E-004, -1.17984506564646906E-004, -2.38427023682723818E-004 },
            { -2.24382495052235118E-007,  1.28218568601277626E-007,  2.59108357381747656E-007,  1.89034327703662092E-004, -1.08019615830663994E-004, -2.18289640324466583E-004 },
            { -3.04001022071876804E-007,  1.22214683774559989E-007,  1.35141804810132761E-007, -1.34034616931480536E-004, -2.14283975204169379E-004,  1.29018773893081404E-004 }
        };
        T[][] jRef = MathArrays.buildArray(field, 6, 6);
        for (int ii = 0; ii<6; ii++){
            for (int jj = 0; jj< 6; jj++){
                jRef[ii][jj] = zero.add(jRefR[ii][jj]);
            }
        }
        FieldPVCoordinates<T> pv = orbEqu.getPVCoordinates();
        Assertions.assertEquals(0, pv.getPosition().subtract(pRef).getNorm().getReal(), 2.0e-16 * pRef.getNorm().getReal());
        Assertions.assertEquals(0, pv.getVelocity().subtract(vRef).getNorm().getReal(), 2.0e-16 * vRef.getNorm().getReal());

        T[][] jacobian = MathArrays.buildArray(field, 6, 6);
        orbEqu.getJacobianWrtCartesian(PositionAngleType.MEAN, jacobian);


        for (int i = 0; i < jacobian.length; i++) {
            T[] row    = jacobian[i];
            T[] rowRef = jRef[i];
            for (int j = 0; j < row.length; j++) {
                Assertions.assertEquals(0, (row[j].getReal() - rowRef[j].getReal()) / rowRef[j].getReal(), 4.0e-15);
            }
        }

    }

    private <T extends CalculusFieldElement<T>> void doTestJacobianFinitedifferences(Field<T> field) {

        T zero = field.getZero();
        FieldAbsoluteDate<T> dateTca = new FieldAbsoluteDate<>(field, 2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        FieldEquinoctialOrbit<T> orbEqu = new FieldEquinoctialOrbit<>(zero.add(7000000.0), zero.add(0.01), zero.add(-0.02), zero.add(1.2), zero.add(2.1),
                                                                      zero.add(FastMath.toRadians(40.)), PositionAngleType.MEAN,
                                                                      FramesFactory.getEME2000(), dateTca, zero.add(mu));

        for (PositionAngleType type : PositionAngleType.values()) {
            T hP = zero.add(2.0);
            T[][] finiteDiffJacobian = finiteDifferencesJacobian(type, orbEqu, hP);
            T[][] jacobian = MathArrays.buildArray(field, 6, 6);
            orbEqu.getJacobianWrtCartesian(type, jacobian);

            for (int i = 0; i < jacobian.length; i++) {
                T[] row    = jacobian[i];
                T[] rowRef = finiteDiffJacobian[i];
                for (int j = 0; j < row.length; j++) {
                    Assertions.assertEquals(0, (row[j].getReal() - rowRef[j].getReal()) / rowRef[j].getReal(), 8.0e-9);
                }
            }

            T[][] invJacobian = MathArrays.buildArray(field, 6, 6);
            orbEqu.getJacobianWrtParameters(type, invJacobian);
            MatrixUtils.createFieldMatrix(jacobian).
                            multiply(MatrixUtils.createFieldMatrix(invJacobian)).
            walkInRowOrder(new FieldMatrixPreservingVisitor<T>() {
                public void start(int rows, int columns,
                                  int startRow, int endRow, int startColumn, int endColumn) {
                }

                public void visit(int row, int column, T value) {
                    Assertions.assertEquals(row == column ? 1.0 : 0.0, value.getReal(), 1.4e-9);
                }

                public T end() {
                    return null;
                }
            });

        }

    }

    private <T extends CalculusFieldElement<T>>  T[][] finiteDifferencesJacobian(PositionAngleType type, FieldEquinoctialOrbit<T> orbit, T hP)
        {
        Field<T> field = hP.getField();
        T[][] jacobian = MathArrays.buildArray(field, 6, 6);
        for (int i = 0; i < 6; ++i) {
            fillColumn(type, i, orbit, hP, jacobian);
        }
        return jacobian;
    }

    private <T extends CalculusFieldElement<T>> void fillColumn(PositionAngleType type, int i, FieldEquinoctialOrbit<T> orbit, T hP, T[][] jacobian) {
        T zero = hP.getField().getZero();
        // at constant energy (i.e. constant semi major axis), we have dV = -mu dP / (V * r^2)
        // we use this to compute a velocity step size from the position step size
        FieldVector3D<T> p = orbit.getPosition();
        FieldVector3D<T> v = orbit.getPVCoordinates().getVelocity();
        T hV = hP.multiply(orbit.getMu()).divide(v.getNorm().multiply(p.getNormSq()));

        T h;
        FieldVector3D<T> dP = new FieldVector3D<>(hP.getField().getZero(), hP.getField().getZero(), hP.getField().getZero());
        FieldVector3D<T> dV = new FieldVector3D<>(hP.getField().getZero(), hP.getField().getZero(), hP.getField().getZero());
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

        FieldEquinoctialOrbit<T> oM4h = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(1, p, -4, dP), new FieldVector3D<>(1, v, -4, dV)),
                                                                    orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldEquinoctialOrbit<T> oM3h = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(1, p, -3, dP), new FieldVector3D<>(1, v, -3, dV)),
                                                                    orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldEquinoctialOrbit<T> oM2h = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(1, p, -2, dP), new FieldVector3D<>(1, v, -2, dV)),
                                                                    orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldEquinoctialOrbit<T> oM1h = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(1, p, -1, dP), new FieldVector3D<>(1, v, -1, dV)),
                                                                    orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldEquinoctialOrbit<T> oP1h = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(1, p, +1, dP), new FieldVector3D<>(1, v, +1, dV)),
                                                                    orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldEquinoctialOrbit<T> oP2h = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(1, p, +2, dP), new FieldVector3D<>(1, v, +2, dV)),
                                                                    orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldEquinoctialOrbit<T> oP3h = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(1, p, +3, dP), new FieldVector3D<>(1, v, +3, dV)),
                                                                    orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldEquinoctialOrbit<T> oP4h = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(1, p, +4, dP), new FieldVector3D<>(1, v, +4, dV)),
                                                                    orbit.getFrame(), orbit.getDate(), orbit.getMu());

        jacobian[0][i] =(zero.add( -3).multiply(oP4h.getA()            .subtract(oM4h.getA())).add(
                         zero.add( 32).multiply(oP3h.getA()            .subtract(oM3h.getA()))).subtract(
                         zero.add(168).multiply(oP2h.getA()            .subtract(oM2h.getA()))).add(
                         zero.add(672).multiply(oP1h.getA()            .subtract(oM1h.getA())))).divide(h.multiply(840));
        jacobian[1][i] =(zero.add( -3).multiply(oP4h.getEquinoctialEx().subtract(oM4h.getEquinoctialEx())).add(
                         zero.add( 32).multiply(oP3h.getEquinoctialEx().subtract(oM3h.getEquinoctialEx()))).subtract(
                         zero.add(168).multiply(oP2h.getEquinoctialEx().subtract(oM2h.getEquinoctialEx()))).add(
                         zero.add(672).multiply(oP1h.getEquinoctialEx().subtract(oM1h.getEquinoctialEx())))).divide(h.multiply(840));
        jacobian[2][i] =(zero.add( -3).multiply(oP4h.getEquinoctialEy().subtract(oM4h.getEquinoctialEy())).add(
                         zero.add( 32).multiply(oP3h.getEquinoctialEy().subtract(oM3h.getEquinoctialEy()))).subtract(
                         zero.add(168).multiply(oP2h.getEquinoctialEy().subtract(oM2h.getEquinoctialEy()))).add(
                         zero.add(672).multiply(oP1h.getEquinoctialEy().subtract(oM1h.getEquinoctialEy())))).divide(h.multiply(840));
        jacobian[3][i] =(zero.add( -3).multiply(oP4h.getHx()           .subtract(oM4h.getHx())).add(
                         zero.add( 32).multiply(oP3h.getHx()           .subtract(oM3h.getHx()))).subtract(
                         zero.add(168).multiply(oP2h.getHx()           .subtract(oM2h.getHx()))).add(
                         zero.add(672).multiply(oP1h.getHx()           .subtract(oM1h.getHx())))).divide(h.multiply(840));
        jacobian[4][i] =(zero.add( -3).multiply(oP4h.getHy()           .subtract(oM4h.getHy())).add(
                         zero.add( 32).multiply(oP3h.getHy()           .subtract(oM3h.getHy()))).subtract(
                         zero.add(168).multiply(oP2h.getHy()           .subtract(oM2h.getHy()))).add(
                         zero.add(672).multiply(oP1h.getHy()           .subtract(oM1h.getHy())))).divide(h.multiply(840));
        jacobian[5][i] =(zero.add( -3).multiply(oP4h.getL(type)        .subtract(oM4h.getL(type))).add(
                         zero.add( 32).multiply(oP3h.getL(type)        .subtract(oM3h.getL(type)))).subtract(
                         zero.add(168).multiply(oP2h.getL(type)        .subtract(oM2h.getL(type)))).add(
                         zero.add(672).multiply(oP1h.getL(type)        .subtract(oM1h.getL(type))))).divide(h.multiply(840));

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
        final FieldEquinoctialOrbit<T> orbit = new FieldEquinoctialOrbit<>(pv, frame, mu);

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
                            1.5e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLv()),
                            orbit.getLvDot().getReal(),
                            1.2e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLE()),
                            orbit.getLEDot().getReal(),
                            7.7e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLM()),
                            orbit.getLMDot().getReal(),
                            8.8e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getE()),
                            orbit.getEDot().getReal(),
                            6.9e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getI()),
                            orbit.getIDot().getReal(),
                            3.5e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getL(PositionAngleType.TRUE)),
                            orbit.getLDot(PositionAngleType.TRUE).getReal(),
                            1.2e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getL(PositionAngleType.ECCENTRIC)),
                            orbit.getLDot(PositionAngleType.ECCENTRIC).getReal(),
                            7.7e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getL(PositionAngleType.MEAN)),
                            orbit.getLDot(PositionAngleType.MEAN).getReal(),
                            8.8e-16);

    }

    private <T extends CalculusFieldElement<T>, S extends Function<FieldEquinoctialOrbit<T>, T>>
    double differentiate(TimeStampedFieldPVCoordinates<T> pv, Frame frame, T mu, S picker) {
        final DSFactory factory = new DSFactory(1, 1);
        FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(8, 0.1);
        UnivariateDifferentiableFunction diff = differentiator.differentiate(new UnivariateFunction() {
            public double value(double dt) {
                return picker.apply(new FieldEquinoctialOrbit<>(pv.shiftedBy(dt), frame, mu)).getReal();
            }
        });
        return diff.value(factory.variable(0, 0.0)).getPartialDerivative(1);
     }

    private <T extends CalculusFieldElement<T>> void doTestPositionAngleDerivatives(final Field<T> field) {
        final T zero = field.getZero();

        final FieldAbsoluteDate<T> date         = new FieldAbsoluteDate<>(field, "2003-05-01T00:00:20.000", TimeScalesFactory.getUTC());
        final FieldVector3D<T>     position     = new FieldVector3D<>(field.getZero().add(6896874.444705),  field.getZero().add(1956581.072644),  field.getZero().add(-147476.245054));
        final FieldVector3D<T>     velocity     = new FieldVector3D<>(field.getZero().add(166.816407662), field.getZero().add(-1106.783301861), field.getZero().add(-7372.745712770));
        final FieldVector3D <T>    acceleration = new FieldVector3D<>(field.getZero().add(-7.466182457944), field.getZero().add(-2.118153357345),  field.getZero().add(0.160004048437));
        final TimeStampedFieldPVCoordinates<T> pv = new TimeStampedFieldPVCoordinates<>(date, position, velocity, acceleration);
        final Frame frame = FramesFactory.getEME2000();
        final double mu   = Constants.EIGEN5C_EARTH_MU;
        final FieldEquinoctialOrbit<T> orbit = new FieldEquinoctialOrbit<>(pv, frame, zero.add(mu));

        for (PositionAngleType type : PositionAngleType.values()) {
            final FieldEquinoctialOrbit<T> rebuilt = new FieldEquinoctialOrbit<>(orbit.getA(),
                                                                                 orbit.getEquinoctialEx(),
                                                                                 orbit.getEquinoctialEy(),
                                                                                 orbit.getHx(),
                                                                                 orbit.getHy(),
                                                                                 orbit.getL(type),
                                                                                 orbit.getADot(),
                                                                                 orbit.getEquinoctialExDot(),
                                                                                 orbit.getEquinoctialEyDot(),
                                                                                 orbit.getHxDot(),
                                                                                 orbit.getHyDot(),
                                                                                 orbit.getLDot(type),
                                                                                 type, orbit.getFrame(), orbit.getDate(), orbit.getMu());
            MatcherAssert.assertThat(rebuilt.getA().getReal(),                                relativelyCloseTo(orbit.getA().getReal(),                1));
            MatcherAssert.assertThat(rebuilt.getEquinoctialEx().getReal(),                    relativelyCloseTo(orbit.getEquinoctialEx().getReal(),    1));
            MatcherAssert.assertThat(rebuilt.getEquinoctialEy().getReal(),                    relativelyCloseTo(orbit.getEquinoctialEy().getReal(),    1));
            MatcherAssert.assertThat(rebuilt.getHx().getReal(),                               relativelyCloseTo(orbit.getHx().getReal(),               1));
            MatcherAssert.assertThat(rebuilt.getHy().getReal(),                               relativelyCloseTo(orbit.getHy().getReal(),               1));
            MatcherAssert.assertThat(rebuilt.getADot().getReal(),                             relativelyCloseTo(orbit.getADot().getReal(),             1));
            MatcherAssert.assertThat(rebuilt.getEquinoctialExDot().getReal(),                 relativelyCloseTo(orbit.getEquinoctialExDot().getReal(), 1));
            MatcherAssert.assertThat(rebuilt.getEquinoctialEyDot().getReal(),                 relativelyCloseTo(orbit.getEquinoctialEyDot().getReal(), 1));
            MatcherAssert.assertThat(rebuilt.getHxDot().getReal(),                            relativelyCloseTo(orbit.getHxDot().getReal(),            1));
            MatcherAssert.assertThat(rebuilt.getHyDot().getReal(),                            relativelyCloseTo(orbit.getHyDot().getReal(),            1));
            for (PositionAngleType type2 : PositionAngleType.values()) {
                MatcherAssert.assertThat(rebuilt.getL(type2).getReal(),    relativelyCloseTo(orbit.getL(type2).getReal(),    1));
                MatcherAssert.assertThat(rebuilt.getLDot(type2).getReal(), relativelyCloseTo(orbit.getLDot(type2).getReal(), 1));
            }
        }

    }

    private <T extends CalculusFieldElement<T>> void doTestEquatorialRetrograde(final Field<T> field) {
            final T zero = field.getZero();
            FieldVector3D<T> position = new FieldVector3D<>(field.getZero().add(10000000.0), field.getZero(), field.getZero());
            FieldVector3D<T> velocity = new FieldVector3D<>(field.getZero(), field.getZero().add(-6500.0), field.getZero());
            T r2 = position.getNormSq();
            T r  = r2.sqrt();
            FieldVector3D<T> acceleration = new FieldVector3D<>(r.multiply(r2.reciprocal().multiply(zero.add(mu).negate())), position,
                                                                field.getOne(), new FieldVector3D<>(field.getZero().add(-0.1),
                                                                                                    field.getZero().add(0.2),
                                                                                                    field.getZero().add(0.3)));
            FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<>(position, velocity, acceleration);
            // we use an intermediate Keplerian orbit so eccentricity can be computed
            // when using directly PV, eccentricity ends up in NaN, due to the way computation is organized
            // this is not really considered a problem as anyway retrograde equatorial cannot be fully supported
            FieldEquinoctialOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldKeplerianOrbit<>(pvCoordinates,
                                                                                                   FramesFactory.getEME2000(),
                                                                                                   FieldAbsoluteDate.getJ2000Epoch(field),
                                                                                                   zero.add(mu)));
            Assertions.assertEquals(10637829.465, orbit.getA().getReal(), 1.0e-3);
            Assertions.assertEquals(-738.145, orbit.getADot().getReal(), 1.0e-3);
            Assertions.assertEquals(0.05995861, orbit.getE().getReal(), 1.0e-8);
            Assertions.assertEquals(-6.523e-5, orbit.getEDot().getReal(), 1.0e-8);
            Assertions.assertTrue(Double.isNaN(orbit.getI().getReal()));
            Assertions.assertTrue(Double.isNaN(orbit.getIDot().getReal()));
            Assertions.assertTrue(Double.isNaN(orbit.getHx().getReal()));
            Assertions.assertTrue(Double.isNaN(orbit.getHxDot().getReal()));
            Assertions.assertTrue(Double.isNaN(orbit.getHy().getReal()));
            Assertions.assertTrue(Double.isNaN(orbit.getHyDot().getReal()));

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
        FieldEquinoctialOrbit<T> orbit = new FieldEquinoctialOrbit<>(pvCoordinates, FramesFactory.getEME2000(),
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
            FieldEquinoctialOrbit<T> rebuilt = (FieldEquinoctialOrbit<T>) OrbitType.EQUINOCTIAL.convertType(converted);
            Assertions.assertTrue(rebuilt.hasDerivatives());
            Assertions.assertEquals(orbit.getADot().getReal(),             rebuilt.getADot().getReal(),             3.0e-13);
            Assertions.assertEquals(orbit.getEquinoctialExDot().getReal(), rebuilt.getEquinoctialExDot().getReal(), 1.0e-15);
            Assertions.assertEquals(orbit.getEquinoctialEyDot().getReal(), rebuilt.getEquinoctialEyDot().getReal(), 1.0e-15);
            Assertions.assertEquals(orbit.getHxDot().getReal(),            rebuilt.getHxDot().getReal(),            1.0e-15);
            Assertions.assertEquals(orbit.getHyDot().getReal(),            rebuilt.getHyDot().getReal(),            1.0e-15);
            Assertions.assertEquals(orbit.getLvDot().getReal(),            rebuilt.getLvDot().getReal(),            1.0e-15);
        }

    }

    private <T extends CalculusFieldElement<T>> void doTestToString(Field<T> field) {
        FieldVector3D<T> position = new FieldVector3D<>(field.getZero().add(-29536113.0),
                                                        field.getZero().add(30329259.0),
                                                        field.getZero().add(-100125.0));
        FieldVector3D<T> velocity = new FieldVector3D<>(field.getZero().add(-2194.0),
                                                        field.getZero().add(-2141.0),
                                                        field.getZero().add(-8.0));
        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<>(position, velocity);
        FieldEquinoctialOrbit<T> orbit = new FieldEquinoctialOrbit<>(pvCoordinates, FramesFactory.getEME2000(),
                                                                     FieldAbsoluteDate.getJ2000Epoch(field), field.getZero().add(mu));
        Assertions.assertEquals("equinoctial parameters: {a: 4.225517000282565E7; ex: 5.927324978565528E-4; ey: -0.002062743969643666; hx: 6.401103130239252E-5; hy: -0.0017606836670756732; lv: 134.24111947709974;}",
                            orbit.toString());
    }

    private <T extends CalculusFieldElement<T>> void doTestCopyNonKeplerianAcceleration(Field<T> field) {

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
        final FieldOrbit<T> orbit = new FieldKeplerianOrbit<>(pv, eme2000, FieldAbsoluteDate.getJ2000Epoch(field), field.getZero().add(mu));

        // Build another KeplerianOrbit as a copy of the first one
        final FieldOrbit<T> orbitCopy = new FieldKeplerianOrbit<>(orbit);

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
        FieldEquinoctialOrbit<T> withoutDerivatives =
                        new FieldEquinoctialOrbit<>(zero.newInstance(42166712.0), zero.newInstance(0.005),
                                                    zero.newInstance(-0.025), zero.newInstance(0.17),
                                                    zero.newInstance(0.34), zero.newInstance(0.4), PositionAngleType.MEAN,
                                                    FramesFactory.getEME2000(), FieldAbsoluteDate.getJ2000Epoch(field),
                                                    zero.newInstance(mu));
        FieldEquinoctialOrbit<T> ref =
                        new FieldEquinoctialOrbit<>(zero.newInstance(24000000.0), zero.newInstance(-0.012),
                                                    zero.newInstance(0.01), zero.newInstance(0.2),
                                                    zero.newInstance(0.1), zero.newInstance(-6.28), PositionAngleType.MEAN,
                                                    FramesFactory.getEME2000(), FieldAbsoluteDate.getJ2000Epoch(field),
                                                    zero.newInstance(mu));

        FieldEquinoctialOrbit<T> normalized1 = (FieldEquinoctialOrbit<T>) OrbitType.EQUINOCTIAL.normalize(withoutDerivatives, ref);
        Assertions.assertFalse(normalized1.hasDerivatives());
        Assertions.assertEquals(0.0, normalized1.getA().subtract(withoutDerivatives.getA()).getReal(),          1.0e-6);
        Assertions.assertEquals(0.0, normalized1.getEquinoctialEx().subtract(withoutDerivatives.getEquinoctialEx()).getReal(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized1.getEquinoctialEy().subtract(withoutDerivatives.getEquinoctialEy()).getReal(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized1.getHx().subtract(withoutDerivatives.getHx()).getReal(),          1.0e-10);
        Assertions.assertEquals(0.0, normalized1.getHy().subtract(withoutDerivatives.getHy()).getReal(), 1.0e-10);
        Assertions.assertEquals(-MathUtils.TWO_PI, normalized1.getLv().subtract(withoutDerivatives.getLv()).getReal(), 1.0e-10);
        Assertions.assertNull(normalized1.getADot());
        Assertions.assertNull(normalized1.getEquinoctialExDot());
        Assertions.assertNull(normalized1.getEquinoctialEyDot());
        Assertions.assertNull(normalized1.getHxDot());
        Assertions.assertNull(normalized1.getHyDot());
        Assertions.assertNull(normalized1.getLvDot());

        T[] p    = MathArrays.buildArray(field, 6);
        T[] pDot = MathArrays.buildArray(field, 6);
        for (int i = 0; i < pDot.length; ++i) {
            pDot[i] = zero.newInstance(i);
        }
        OrbitType.EQUINOCTIAL.mapOrbitToArray(withoutDerivatives, PositionAngleType.TRUE, p, null);
        FieldEquinoctialOrbit<T> withDerivatives = (FieldEquinoctialOrbit<T>) OrbitType.EQUINOCTIAL.mapArrayToOrbit(p, pDot,
                                                                                                                    PositionAngleType.TRUE,
                                                                                                                    withoutDerivatives.getDate(),
                                                                                                                    withoutDerivatives.getMu(),
                                                                                                                    withoutDerivatives.getFrame());
        FieldEquinoctialOrbit<T> normalized2 = (FieldEquinoctialOrbit<T>) OrbitType.EQUINOCTIAL.normalize(withDerivatives, ref);
        Assertions.assertTrue(normalized2.hasDerivatives());
        Assertions.assertEquals(0.0, normalized2.getA().subtract(withDerivatives.getA()).getReal(),          1.0e-6);
        Assertions.assertEquals(0.0, normalized2.getEquinoctialEx().subtract(withDerivatives.getEquinoctialEx()).getReal(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getEquinoctialEy().subtract(withDerivatives.getEquinoctialEy()).getReal(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getHx().subtract(withDerivatives.getHx()).getReal(),          1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getHy().subtract(withDerivatives.getHy()).getReal(), 1.0e-10);
        Assertions.assertEquals(-MathUtils.TWO_PI, normalized2.getLv().subtract(withDerivatives.getLv()).getReal(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getADot().subtract(withDerivatives.getADot()).getReal(),          1.0e-6);
        Assertions.assertEquals(0.0, normalized2.getEquinoctialExDot().subtract(withDerivatives.getEquinoctialExDot()).getReal(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getEquinoctialEyDot().subtract(withDerivatives.getEquinoctialEyDot()).getReal(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getHxDot().subtract(withDerivatives.getHxDot()).getReal(),          1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getHyDot().subtract(withDerivatives.getHyDot()).getReal(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getLvDot().subtract(withDerivatives.getLvDot()).getReal(), 1.0e-10);

    }

}
