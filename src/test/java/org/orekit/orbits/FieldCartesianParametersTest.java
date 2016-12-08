/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
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
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.propagation.analytical.FieldEcksteinHechlerPropagator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.FieldPVCoordinates;


public class FieldCartesianParametersTest {


    // Body mu
    private double mu;

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        // Body mu
        mu = 3.9860047e14;
    }
    @Test
    public void doCartToCartTest() throws OrekitException{
        testCartesianToCartesian(Decimal64Field.getInstance());
    }

    @Test
    public void doCartToEquinTest() throws OrekitException{
        testCartesianToEquinoctial(Decimal64Field.getInstance());
    }

    @Test
    public void doCartToKeplTest() throws OrekitException{
        testCartesianToKeplerian(Decimal64Field.getInstance());
    }

    @Test
    public void doPositionToVelocityNormsTest() throws OrekitException{
        testPositionVelocityNorms(Decimal64Field.getInstance());
    }

    @Test
    public void doGeometryTest() throws OrekitException{
        testGeometry(Decimal64Field.getInstance());
    }

    @Test
    public void doNumericalIssue25Test() throws OrekitException{
        testNumericalIssue25(Decimal64Field.getInstance());
    }

    @Test
    public void doShiftEllipticTest() throws OrekitException{
        testShiftElliptic(Decimal64Field.getInstance());
    }

    @Test
    public void doShiftCircularTest() throws OrekitException{
        testShiftCircular(Decimal64Field.getInstance());
    }

    @Test
    public void doShiftHyperbolicTest() throws OrekitException{
        testShiftHyperbolic(Decimal64Field.getInstance());
    }

    @Test
    public void doNumericalIssue135Test() throws OrekitException{
        testNumericalIssue135(Decimal64Field.getInstance());
    }

    @Test
    public void doJacobianReferenceTest() throws OrekitException{
        testJacobianReference(Decimal64Field.getInstance());
    }

    @Test
    public void doInteroplationTest() throws OrekitException{
        testInterpolation(Decimal64Field.getInstance());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testErr1(){
        test(Decimal64Field.getInstance());
    }

    public <T extends RealFieldElement<T>> void testCartesianToCartesian(Field<T> field) {

        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(-29536113.0), zero.add(30329259.0), zero.add(-100125.0));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-2194.0), zero.add(-2141.0), zero.add(-8.0));
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<T>( position, velocity);
        double mu = 3.9860047e14;

        FieldCartesianOrbit<T> p = new FieldCartesianOrbit<T>(FieldPVCoordinates, FramesFactory.getEME2000(), date, mu);

        Assert.assertEquals(p.getPVCoordinates().getPosition().getX().getReal(), FieldPVCoordinates.getPosition().getX().getReal(), Utils.epsilonTest * FastMath.abs(FieldPVCoordinates.getPosition().getX().getReal()));
        Assert.assertEquals(p.getPVCoordinates().getPosition().getY().getReal(), FieldPVCoordinates.getPosition().getY().getReal(), Utils.epsilonTest * FastMath.abs(FieldPVCoordinates.getPosition().getY().getReal()));
        Assert.assertEquals(p.getPVCoordinates().getPosition().getZ().getReal(), FieldPVCoordinates.getPosition().getZ().getReal(), Utils.epsilonTest * FastMath.abs(FieldPVCoordinates.getPosition().getZ().getReal()));
        Assert.assertEquals(p.getPVCoordinates().getVelocity().getX().getReal(), FieldPVCoordinates.getVelocity().getX().getReal(), Utils.epsilonTest * FastMath.abs(FieldPVCoordinates.getVelocity().getX().getReal()));
        Assert.assertEquals(p.getPVCoordinates().getVelocity().getY().getReal(), FieldPVCoordinates.getVelocity().getY().getReal(), Utils.epsilonTest * FastMath.abs(FieldPVCoordinates.getVelocity().getY().getReal()));
        Assert.assertEquals(p.getPVCoordinates().getVelocity().getZ().getReal(), FieldPVCoordinates.getVelocity().getZ().getReal(), Utils.epsilonTest * FastMath.abs(FieldPVCoordinates.getVelocity().getZ().getReal()));
    }

    public <T extends RealFieldElement<T>> void testCartesianToEquinoctial(Field<T> field) {
        T zero = field.getZero();

        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(-29536113.0), zero.add(30329259.0), zero.add(-100125.0));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-2194.0), zero.add(-2141.0), zero.add(-8.0));
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<T>( position, velocity);

        FieldCartesianOrbit<T> p = new FieldCartesianOrbit<T>(FieldPVCoordinates, FramesFactory.getEME2000(),
                                                              FieldAbsoluteDate.getJ2000Epoch(field), mu);

        Assert.assertEquals(42255170.0028257,  p.getA().getReal(), Utils.epsilonTest * p.getA().getReal());
        Assert.assertEquals(0.592732497856475e-03,  p.getEquinoctialEx().getReal(), Utils.epsilonE * FastMath.abs(p.getE().getReal()));
        Assert.assertEquals(-0.206274396964359e-02, p.getEquinoctialEy().getReal(), Utils.epsilonE * FastMath.abs(p.getE().getReal()));
        Assert.assertEquals(FastMath.sqrt(FastMath.pow(0.592732497856475e-03,2)+FastMath.pow(-0.206274396964359e-02,2)), p.getE().getReal(), Utils.epsilonAngle * FastMath.abs(p.getE().getReal()));
        Assert.assertEquals(MathUtils.normalizeAngle(2*FastMath.asin(FastMath.sqrt((FastMath.pow(0.128021863908325e-03,2)+FastMath.pow(-0.352136186881817e-02,2))/4.)),p.getI().getReal()), p.getI().getReal(), Utils.epsilonAngle * FastMath.abs(p.getI().getReal()));
        Assert.assertEquals(MathUtils.normalizeAngle(0.234498139679291e+01,p.getLM().getReal()), p.getLM().getReal(), Utils.epsilonAngle * FastMath.abs(p.getLM().getReal()));
    }

    public <T extends RealFieldElement<T>> void testCartesianToKeplerian(Field<T> field){
        T zero = field.getZero();

        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(-26655470.0), zero.add(29881667.0),zero.add(-113657.0));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-1125.0),zero.add(-1122.0),zero.add(195.0));
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<T>( position, velocity);
        double mu = 3.9860047e14;

        FieldCartesianOrbit<T> p = new FieldCartesianOrbit<T>(FieldPVCoordinates, FramesFactory.getEME2000(),
                                                              FieldAbsoluteDate.getJ2000Epoch(field), mu);
        FieldKeplerianOrbit<T> kep = new FieldKeplerianOrbit<T>(p);

        Assert.assertEquals(22979265.3030773,  p.getA().getReal(), Utils.epsilonTest  * p.getA().getReal());
        Assert.assertEquals(0.743502611664700, p.getE().getReal(), Utils.epsilonE     * FastMath.abs(p.getE().getReal()));
        Assert.assertEquals(0.122182096220906, p.getI().getReal(), Utils.epsilonAngle * FastMath.abs(p.getI().getReal()));
        T pa = kep.getPerigeeArgument();
        Assert.assertEquals(MathUtils.normalizeAngle(3.09909041016672, pa.getReal()), pa.getReal(),
                     Utils.epsilonAngle * FastMath.abs(pa.getReal()));
        T raan = kep.getRightAscensionOfAscendingNode();
        Assert.assertEquals(MathUtils.normalizeAngle(2.32231010979999, raan.getReal()), raan.getReal(),
                     Utils.epsilonAngle * FastMath.abs(raan.getReal()));
        T m = kep.getMeanAnomaly();
        Assert.assertEquals(MathUtils.normalizeAngle(3.22888977629034, m.getReal()), m.getReal(),
                     Utils.epsilonAngle * FastMath.abs(FastMath.abs(m.getReal())));
    }

    public <T extends RealFieldElement<T>> void testPositionVelocityNorms(Field<T> field){ T zero=field.getZero();T one=field.getOne(); FieldAbsoluteDate<T> date=new FieldAbsoluteDate<T>(field);

        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(-29536113.0), zero.add(30329259.0), zero.add(-100125.0));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-2194.0), zero.add(-2141.0), zero.add(-8.0));
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<T>( position, velocity);

        FieldCartesianOrbit<T> p = new FieldCartesianOrbit<T>(FieldPVCoordinates, FramesFactory.getEME2000(), date, mu);

        T e       = p.getE();
        T v       = new FieldKeplerianOrbit<T>(p).getTrueAnomaly();
        T ksi     = e.multiply(v.cos()).add(1);
        T nu      = e.multiply(v.sin());
        T epsilon = one.subtract(e).multiply(e.add(1)).sqrt();

        T a  = p.getA();
        T na = a.reciprocal().multiply(mu).sqrt();

        // validation of: r = a .(1 - e2) / (1 + e.cos(v))
        Assert.assertEquals(a.getReal() * epsilon.getReal() * epsilon.getReal() / ksi.getReal(),
                     p.getPVCoordinates().getPosition().getNorm().getReal(),
                     Utils.epsilonTest * FastMath.abs(p.getPVCoordinates().getPosition().getNorm().getReal()));

        // validation of: V = sqrt(mu.(1+2e.cos(v)+e2)/a.(1-e2) )
        Assert.assertEquals(na.getReal() * FastMath.sqrt(ksi.getReal() * ksi.getReal() + nu .getReal()* nu.getReal()) / epsilon.getReal(),
                     p.getPVCoordinates().getVelocity().getNorm().getReal(),
                     Utils.epsilonTest * FastMath.abs(p.getPVCoordinates().getVelocity().getNorm().getReal()));

    }

    public <T extends RealFieldElement<T>> void testGeometry(Field<T> field) {
        T zero = field.getZero();

        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(-29536113.0), zero.add(30329259.0), zero.add(-100125.0));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-2194.0), zero.add(-2141.0), zero.add(-8.0));
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<T>( position, velocity);

        FieldVector3D<T> momentum = FieldPVCoordinates.getMomentum().normalize();

        FieldEquinoctialOrbit<T> p = new FieldEquinoctialOrbit<T>(FieldPVCoordinates, FramesFactory.getEME2000(),
                                                                  FieldAbsoluteDate.getJ2000Epoch(field), mu);

        T apogeeRadius  = p.getA().multiply( p.getE().add(1.0));
        T perigeeRadius = p.getA().multiply( p.getE().negate().add(1.0));

        for (T lv = zero; lv.getReal() <= 2 * FastMath.PI; lv = lv.add(2 * FastMath.PI/100.)) {
            p = new FieldEquinoctialOrbit<T>(p.getA(), p.getEquinoctialEx(), p.getEquinoctialEy(),
                                             p.getHx(), p.getHy(), lv, PositionAngle.TRUE, p.getFrame(),
                                             FieldAbsoluteDate.getJ2000Epoch(field), mu);
            position = p.getPVCoordinates().getPosition();

            // test if the norm of the position is in the range [perigee radius, apogee radius]
            // Warning: these tests are without absolute value by choice
            Assert.assertTrue((position.getNorm().getReal() - apogeeRadius.getReal())  <= (  apogeeRadius.getReal() * Utils.epsilonTest));
            Assert.assertTrue((position.getNorm().getReal() - perigeeRadius.getReal()) >= (- perigeeRadius.getReal() * Utils.epsilonTest));
            // Assert.assertTrue(position.getNorm() <= apogeeRadius);
            // Assert.assertTrue(position.getNorm() >= perigeeRadius);

            position= position.normalize();
            velocity = p.getPVCoordinates().getVelocity().normalize();

            // at this stage of computation, all the vectors (position, velocity and momemtum) are normalized here

            // test of orthogonality between position and momentum
            Assert.assertTrue(FastMath.abs(FieldVector3D.dotProduct(position, momentum).getReal()) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assert.assertTrue(FastMath.abs(FieldVector3D.dotProduct(velocity, momentum).getReal()) < Utils.epsilonTest);
        }
    }

    public <T extends RealFieldElement<T>> void testNumericalIssue25(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(3782116.14107698), zero.add(416663.11924914), zero.add(5875541.62103057));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-6349.7848910501), zero.add(288.4061811651), zero.add(4066.9366759691));
        FieldCartesianOrbit<T> orbit = new FieldCartesianOrbit<T>(new FieldPVCoordinates<T>(position, velocity),
                                                  FramesFactory.getEME2000(),
                                                  new FieldAbsoluteDate<T>(field,"2004-01-01T23:00:00.000",
                                                                   TimeScalesFactory.getUTC()),
                                                                   3.986004415E14);
        Assert.assertEquals(0.0, orbit.getE().getReal(), 2.0e-14);
    }

    public <T extends RealFieldElement<T>> void testShiftElliptic(Field<T> field) {
        T zero = field.getZero();
        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(-29536113.0), zero.add(30329259.0), zero.add(-100125.0));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-2194.0), zero.add(-2141.0), zero.add(-8.0));
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<T>( position, velocity);
        FieldCartesianOrbit<T> orbit = new FieldCartesianOrbit<T>(FieldPVCoordinates, FramesFactory.getEME2000(),
                                                                  FieldAbsoluteDate.getJ2000Epoch(field), mu);
        testShift(orbit, new FieldKeplerianOrbit<T>(orbit), 2e-15);
    }

    public <T extends RealFieldElement<T>> void testShiftCircular(Field<T> field) {
        T zero = field.getZero();
        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(-29536113.0), zero.add(30329259.0), zero.add(-100125.0));
        FieldVector3D<T> velocity = new FieldVector3D<T>(position.getNorm().reciprocal().multiply(mu).sqrt(), position.orthogonal());
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<T>( position, velocity);
        FieldCartesianOrbit<T> orbit = new FieldCartesianOrbit<T>(FieldPVCoordinates, FramesFactory.getEME2000(),
                                                                  FieldAbsoluteDate.getJ2000Epoch(field), mu);
        testShift(orbit, new FieldCircularOrbit<T>(orbit), 2e-15);
    }

    public <T extends RealFieldElement<T>> void testShiftHyperbolic(Field<T> field) {
        T zero = field.getZero();
        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(-29536113.0), zero.add(30329259.0), zero.add(-100125.0));
        FieldVector3D<T> velocity = new FieldVector3D<T>(position.getNorm().reciprocal().multiply(mu).sqrt().multiply(3.0), position.orthogonal());
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<T>(position, velocity);
        FieldCartesianOrbit<T> orbit = new FieldCartesianOrbit<T>(FieldPVCoordinates, FramesFactory.getEME2000(),
                                                                  FieldAbsoluteDate.getJ2000Epoch(field), mu);
        testShift(orbit, new FieldKeplerianOrbit<T>(orbit), 1e-15);
    }

    public <T extends RealFieldElement<T>> void testNumericalIssue135(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(-6.7884943832e7), zero.add(-2.1423006112e7), zero.add(-3.1603915377e7));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-4732.55), zero.add(-2472.086), zero.add(-3022.177));
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<T>(position, velocity);
        FieldCartesianOrbit<T> orbit = new FieldCartesianOrbit<T>(FieldPVCoordinates, FramesFactory.getEME2000(),
                                                                  FieldAbsoluteDate.getJ2000Epoch(field),
                                                                  324858598826460.);
        testShift(orbit, new FieldKeplerianOrbit<T>(orbit), 3.0e-15);
    }

    private <T extends RealFieldElement<T>> void testShift(FieldCartesianOrbit<T> tested, FieldOrbit<T> reference, double threshold) {
        Field<T> field = tested.getA().getField();
        T zero = field.getZero();
        for (T dt = zero.add(- 1000); dt.getReal() < 1000; dt = dt.add(10.0)) {

            FieldPVCoordinates<T> pvTested    = tested.shiftedBy(dt).getPVCoordinates();
            FieldVector3D<T>      pTested     = pvTested.getPosition();
            FieldVector3D<T>      vTested     = pvTested.getVelocity();

            FieldPVCoordinates<T> pvReference = reference.shiftedBy(dt).getPVCoordinates();
            FieldVector3D<T>      pReference  = pvReference.getPosition();
            FieldVector3D<T>      vReference  = pvReference.getVelocity();
            Assert.assertEquals(0, pTested.subtract(pReference).getNorm().getReal(), threshold * pReference.getNorm().getReal());
            Assert.assertEquals(0.0, vTested.getX().getReal() - vReference.getX().getReal(), threshold * vReference.getNorm().getReal());
            Assert.assertEquals(0.0, vTested.getY().getReal() - vReference.getY().getReal(), threshold * vReference.getNorm().getReal());
            Assert.assertEquals(0.0, vTested.getZ().getReal() - vReference.getZ().getReal(), threshold * vReference.getNorm().getReal());

        }
    }

    public <T extends RealFieldElement<T> >void test(Field<T> field) throws IllegalArgumentException {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(-26655470.0), zero.add(29881667.0),zero.add(-113657.0));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-1125.0),zero.add(-1122.0),zero.add(195.0));
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<T>( position, velocity);
        double mu = 3.9860047e14;
        new FieldCartesianOrbit<T>(FieldPVCoordinates,
                           new Frame(FramesFactory.getEME2000(), Transform.IDENTITY, "non-inertial", false),
                           date, mu);
    }

    public <T extends RealFieldElement<T>> void testJacobianReference(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(-29536113.0), zero.add(30329259.0), zero.add(-100125.0));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-2194.0), zero.add(-2141.0), zero.add(-8.0));
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<T>( position, velocity);
        FieldCartesianOrbit<T> orbit = new FieldCartesianOrbit<T>(FieldPVCoordinates, FramesFactory.getEME2000(),
                                                                  FieldAbsoluteDate.getJ2000Epoch(field), mu);

        T[][] jacobian = MathArrays.buildArray(field, 6, 6);
        orbit.getJacobianWrtCartesian(PositionAngle.MEAN, jacobian);

        for (int i = 0; i < jacobian.length; i++) {
            T[] row    = jacobian[i];
            for (int j = 0; j < row.length; j++) {
                Assert.assertEquals((i == j) ? 1 : 0, row[j].getReal(), 1.0e-15);
            }
        }

        T[][] invJacobian = MathArrays.buildArray(field, 6, 6);
        orbit.getJacobianWrtParameters(PositionAngle.MEAN, invJacobian);
        MatrixUtils.createFieldMatrix(jacobian).
                        multiply(MatrixUtils.createFieldMatrix(invJacobian)).
        walkInRowOrder(new FieldMatrixPreservingVisitor<T>() {
            public void start(int rows, int columns,
                              int startRow, int endRow, int startColumn, int endColumn) {
            }

            public void visit(int row, int column, T value) {
                Assert.assertEquals(row == column ? 1.0 : 0.0, value.getReal(), 1.0e-15);
            }

            public T end() {
                return null;
            }
        });

    }

    public <T extends RealFieldElement<T>> void testInterpolation(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        final double ehMu  = 3.9860047e14;
        final double ae  = 6.378137e6;
        final T c20 = zero.add(-1.08263e-3);
        final T c30 = zero.add(2.54e-6);
        final T c40 = zero.add(1.62e-6);
        final T c50 = zero.add(2.3e-7);
        final T c60 = zero.add(-5.5e-7);

        final FieldAbsoluteDate<T> date = FieldAbsoluteDate.getJ2000Epoch(field).shiftedBy(584.);
        final FieldVector3D<T> position = new FieldVector3D<T>(zero.add(3220103.), zero.add(69623.), zero.add(6449822.));
        final FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(6414.7), zero.add(-2006.), zero.add(-3180.));
        final FieldCartesianOrbit<T> initialOrbit = new FieldCartesianOrbit<T>(new FieldPVCoordinates<T>(position, velocity),
                                                              FramesFactory.getEME2000(), date, ehMu);

        FieldEcksteinHechlerPropagator<T> propagator =
                new FieldEcksteinHechlerPropagator<T>(initialOrbit, ae, ehMu, c20, c30, c40, c50, c60);

        // set up a 5 points sample
        List<FieldOrbit<T>> sample = new ArrayList<FieldOrbit<T>>();
        for (T dt = zero; dt.getReal() < 251.0; dt = dt.add(60.0)) {
            sample.add(propagator.propagate(date.shiftedBy(dt)).getOrbit());
        }

        // well inside the sample, interpolation should be much better than Keplerian shift
        // this is bacause we take the full non-Keplerian acceleration into account in
        // the Cartesian parameters, which in this case is preserved by the
        // Eckstein-Hechler propagator
        double maxShiftPError = 0;
        double maxInterpolationPError = 0;
        double maxShiftVError = 0;
        double maxInterpolationVError = 0;
        for (T dt = zero; dt.getReal() < 240.0; dt = dt.add(1.0)) {
            FieldAbsoluteDate<T> t                   = initialOrbit.getDate().shiftedBy(dt);
            FieldPVCoordinates<T> propagated         = propagator.propagate(t).getPVCoordinates();
            FieldPVCoordinates<T> shiftError         = new FieldPVCoordinates<T>(propagated,
                                                                 initialOrbit.shiftedBy(dt).getPVCoordinates());
            FieldPVCoordinates<T> interpolationError = new FieldPVCoordinates<T>(propagated,
                                                                 initialOrbit.interpolate(t, sample).getPVCoordinates());
            maxShiftPError                   = FastMath.max(maxShiftPError,
                                                            shiftError.getPosition().getNorm().getReal());
            maxInterpolationPError           = FastMath.max(maxInterpolationPError,
                                                            interpolationError.getPosition().getNorm().getReal());
            maxShiftVError                   = FastMath.max(maxShiftVError,
                                                            shiftError.getVelocity().getNorm().getReal());
            maxInterpolationVError           = FastMath.max(maxInterpolationVError,
                                                            interpolationError.getVelocity().getNorm().getReal());
        }
        Assert.assertTrue(maxShiftPError         > 390.0);
        Assert.assertTrue(maxInterpolationPError < 3.0e-8);
        Assert.assertTrue(maxShiftVError         > 3.0);
        Assert.assertTrue(maxInterpolationVError < 2.0e-9);

        // if we go far past sample end, interpolation becomes worse than Keplerian shift
        maxShiftPError = 0;
        maxInterpolationPError = 0;
        maxShiftVError = 0;
        maxInterpolationVError = 0;
        for (T dt = zero.add(500.0); dt.getReal() < 725.0; dt = dt.add(1.0)) {
            FieldAbsoluteDate<T> t                   = initialOrbit.getDate().shiftedBy(dt);
            FieldPVCoordinates<T> propagated         = propagator.propagate(t).getPVCoordinates();
            FieldPVCoordinates<T> shiftError         = new FieldPVCoordinates<T>(propagated,
                                                                 initialOrbit.shiftedBy(dt).getPVCoordinates());
            FieldPVCoordinates<T> interpolationError = new FieldPVCoordinates<T>(propagated,
                                                                 initialOrbit.interpolate(t, sample).getPVCoordinates());
            maxShiftPError                   = FastMath.max(maxShiftPError,
                                                            shiftError.getPosition().getNorm().getReal());
            maxInterpolationPError           = FastMath.max(maxInterpolationPError,
                                                            interpolationError.getPosition().getNorm().getReal());
            maxShiftVError                   = FastMath.max(maxShiftVError,
                                                            shiftError.getVelocity().getNorm().getReal());
            maxInterpolationVError           = FastMath.max(maxInterpolationVError,
                                                            interpolationError.getVelocity().getNorm().getReal());
        }
        Assert.assertTrue(maxShiftPError         < 3000.0);
        Assert.assertTrue(maxInterpolationPError > 6000.0);
        Assert.assertTrue(maxShiftVError         <    7.0);
        Assert.assertTrue(maxInterpolationVError >  170.0);

    }


}

