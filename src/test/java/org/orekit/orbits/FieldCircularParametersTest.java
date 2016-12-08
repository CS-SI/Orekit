/* Copyright 2002-2016 CS Systèmes d'Informationositio0
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
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.propagation.analytical.FieldEcksteinHechlerPropagator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.FieldPVCoordinates;


public class FieldCircularParametersTest {

    // Body mu
    private double mu;



    @Before
    public void setUp() {

        Utils.setDataRoot("regular-data");

        // Body mu
        mu = 3.9860047e14;

    }

    @Test
    public void doCircularToEquinocTest() throws OrekitException {
          testCircularToEquinoctialEll(Decimal64Field.getInstance());
    }

    @Test
    public void doCircToEquinocTest() throws OrekitException {
          testCircularToEquinoctialCirc(Decimal64Field.getInstance());
    }

    @Test
    public void doAnomalyCircTest() throws OrekitException {
          testAnomalyCirc(Decimal64Field.getInstance());
    }

    @Test
    public void doAnomalyEllTest() throws OrekitException {
          testAnomalyEll(Decimal64Field.getInstance());
    }

    @Test
    public void doCircToCartTest() throws OrekitException {
          testCircularToCartesian(Decimal64Field.getInstance());
    }

    @Test
    public void doCircToKeplTest() throws OrekitException {
          testCircularToKeplerian(Decimal64Field.getInstance());
    }

    @Test
    public void doGeometryCircTest() throws OrekitException {
          testGeometryCirc(Decimal64Field.getInstance());
    }

    @Test
    public void doGeometryEllTest() throws OrekitException {
          testGeometryEll(Decimal64Field.getInstance());
    }

    @Test
    public void doInterpolationTest() throws OrekitException {
          testInterpolation(Decimal64Field.getInstance());
    }

    @Test
    public void doJacobianFinitedTest() throws OrekitException {
          testJacobianFinitedifferences(Decimal64Field.getInstance());
    }

    @Test
    public void doJacoabianReferenceTest() throws OrekitException { 
          testJacobianReference(Decimal64Field.getInstance());
    }

    @Test
    public void doNumericalIssue25Test() throws OrekitException { 
          testNumericalIssue25(Decimal64Field.getInstance());
    }

    @Test
    public void doPerfectlyEquatorialTest() throws OrekitException { 
          testPerfectlyEquatorial(Decimal64Field.getInstance());
    }

    @Test
    public void doPositionVelocityNormsCircTest() throws OrekitException { 
          testPositionVelocityNormsCirc(Decimal64Field.getInstance());
    }

    @Test
    public void doPositionVelocityTest() throws OrekitException { 
          testPositionVelocityNormsEll(Decimal64Field.getInstance());
    }

    @Test
    public void doSymmetryCirTest() throws OrekitException { 
          testSymmetryCir(Decimal64Field.getInstance());
    }

    @Test
    public void doSymmetryEllTest() throws OrekitException { 
          testSymmetryEll(Decimal64Field.getInstance());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testErrors()  {
        testNonInertialFrame(Decimal64Field.getInstance());   //(expected=IllegalArgumentException.class)
    }
    @Test(expected=IllegalArgumentException.class)
    public void testErrors2() {
        testHyperbolic(Decimal64Field.getInstance());             //(expected=IllegalArgumentException.class)
    }

    public <T extends RealFieldElement<T>> void testCircularToEquinoctialEll(Field<T> field) {

        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

        T ix = zero.add(1.200e-04);
        T iy = zero.add(-1.16e-04);
        T i  = ix.multiply(ix).add(iy.multiply(iy)).divide(4).sqrt().asin().multiply(2);


        T raan = iy.atan2(ix);

        // elliptic orbit
        FieldCircularOrbit<T> circ =
            new FieldCircularOrbit<T>(zero.add(42166.712), zero.add(0.5), zero.add(-0.5), i, raan,
                                   zero.add(5.300).subtract(raan), PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), date, mu);
        FieldVector3D<T> pos = circ.getPVCoordinates().getPosition();
        FieldVector3D<T> vit = circ.getPVCoordinates().getVelocity();

        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<T>( pos, vit);



        FieldEquinoctialOrbit<T> param = new FieldEquinoctialOrbit<T>(pvCoordinates, FramesFactory.getEME2000(), date, mu);

        Assert.assertEquals(param.getA().getReal(),  circ.getA().getReal(), Utils.epsilonTest * circ.getA().getReal());
        Assert.assertEquals(param.getEquinoctialEx().getReal(), circ.getEquinoctialEx().getReal(), Utils.epsilonE * FastMath.abs(circ.getE().getReal()));
        Assert.assertEquals(param.getEquinoctialEy().getReal(), circ.getEquinoctialEy().getReal(), Utils.epsilonE * FastMath.abs(circ.getE().getReal()));
        Assert.assertEquals(param.getHx().getReal(), circ.getHx().getReal(), Utils.epsilonAngle * FastMath.abs(circ.getI().getReal()));
        Assert.assertEquals(param.getHy().getReal(), circ.getHy().getReal(), Utils.epsilonAngle * FastMath.abs(circ.getI().getReal()));


        Assert.assertEquals(MathUtils.normalizeAngle(param.getLv().getReal(),circ.getLv().getReal()), circ.getLv().getReal(), Utils.epsilonAngle * FastMath.abs(circ.getLv().getReal()));

    }

    public <T extends RealFieldElement<T>> void testCircularToEquinoctialCirc(Field<T> field) {

        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

        T ix = zero.add(1.200e-04);
        T iy = zero.add(-1.16e-04);
        T i  = ix.multiply(ix).add(iy.multiply(iy)).divide(4).sqrt().asin().multiply(2);
        T raan = iy.atan2(ix);

        // circular orbit
        FieldEquinoctialOrbit<T> circCir =
            new FieldEquinoctialOrbit<T>(zero.add(42166.712), zero.add(0.1e-10), zero.add(-0.1e-10), i, raan,
                                      raan.negate().add(5.300), PositionAngle.MEAN,
                                      FramesFactory.getEME2000(), date, mu);
        FieldVector3D<T> posCir = circCir.getPVCoordinates().getPosition();
        FieldVector3D<T> vitCir = circCir.getPVCoordinates().getVelocity();

        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<T>( posCir, vitCir);

        FieldEquinoctialOrbit<T> paramCir = new FieldEquinoctialOrbit<T>(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assert.assertEquals(paramCir.getA().getReal(), circCir.getA().getReal(), Utils.epsilonTest * circCir.getA().getReal());
        Assert.assertEquals(paramCir.getEquinoctialEx().getReal(), circCir.getEquinoctialEx().getReal(), Utils.epsilonEcir * FastMath.abs(circCir.getE().getReal()));
        Assert.assertEquals(paramCir.getEquinoctialEy().getReal(), circCir.getEquinoctialEy().getReal(), Utils.epsilonEcir * FastMath.abs(circCir.getE().getReal()));
        Assert.assertEquals(paramCir.getHx().getReal(), circCir.getHx().getReal(), Utils.epsilonAngle * FastMath.abs(circCir.getI().getReal()));
        Assert.assertEquals(paramCir.getHy().getReal(), circCir.getHy().getReal(), Utils.epsilonAngle * FastMath.abs(circCir.getI().getReal()));
        Assert.assertEquals(MathUtils.normalizeAngle(paramCir.getLv().getReal(),circCir.getLv().getReal()), circCir.getLv().getReal(), Utils.epsilonAngle * FastMath.abs(circCir.getLv().getReal()));

   }

    public <T extends RealFieldElement<T>> void testCircularToCartesian(Field<T> field) {
        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

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
            new FieldCircularOrbit<T>(zero.add(42166.712), ex, ey, i, raan,
                                   raan.negate().add(5.300), PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), date, mu);
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

    public <T extends RealFieldElement<T>> void testCircularToKeplerian(Field<T> field) {
        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

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
            new FieldCircularOrbit<T>(zero.add(42166.712), ex, ey, i, raan,
                                   raan.negate().add(5.300), PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), date, mu);
        FieldKeplerianOrbit<T> kep = new FieldKeplerianOrbit<T>(circ);

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

        Assert.assertEquals(FieldCircularOrbit.normalizeAngle(zero.add(3.65750858649982), kep.getMeanAnomaly()).getReal(),
                     kep.getMeanAnomaly().getReal(),
                     Utils.epsilonTest * 5 * FastMath.abs(kep.getMeanAnomaly().getReal())); //numerical propagation we changed to 6 time the precision used

    }

    public <T extends RealFieldElement<T>> void testHyperbolic(Field<T> field) {
        T zero =  field.getZero();
       FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        new FieldCircularOrbit<T>(zero.add(42166.712), zero.add(0.9), zero.add(0.5), zero.add(0.01), zero.add(-0.02),zero.add( 5.300),
                          PositionAngle.MEAN,  FramesFactory.getEME2000(), date, mu);
    }

    public <T extends RealFieldElement<T>> void testAnomalyEll(Field<T> field) {
        T zero =  field.getZero();
        T one =  field.getOne();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        // elliptic orbit
        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(7.0e6),zero.add(1.0e6), zero.add(4.0e6));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<T>( position, velocity);

        FieldCircularOrbit<T>  p   = new FieldCircularOrbit<T>(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        FieldKeplerianOrbit<T> kep = new FieldKeplerianOrbit<T>(p);

        T e       = p.getE();
        T eRatio  = one.subtract(e).divide(one.add(e)).sqrt();
        T raan    = kep.getRightAscensionOfAscendingNode();
        T paPraan = kep.getPerigeeArgument().add(raan);

        T lv = zero.add(1.1);
        // formulations for elliptic case
        T lE = lv.subtract(paPraan).divide(2).tan().multiply(eRatio).atan().multiply(2).add(paPraan);
        T lM = lE.subtract(e.multiply(lE.subtract(paPraan).sin()));

        p = new FieldCircularOrbit<T>(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), lv.subtract(raan), PositionAngle.TRUE, p.getFrame(), date, mu);
        Assert.assertEquals(p.getAlphaV().getReal() + raan.getReal(), lv.getReal(), Utils.epsilonAngle * FastMath.abs(lv.getReal()));
        Assert.assertEquals(p.getAlphaE().getReal() + raan.getReal(), lE.getReal(), Utils.epsilonAngle * FastMath.abs(lE.getReal()));
        Assert.assertEquals(p.getAlphaM().getReal() + raan.getReal(), lM.getReal(), Utils.epsilonAngle * FastMath.abs(lM.getReal()));
        p = new FieldCircularOrbit<T>(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), zero, PositionAngle.TRUE, p.getFrame(), date, mu);


        p = new FieldCircularOrbit<T>(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), lE.subtract(raan), PositionAngle.ECCENTRIC, p.getFrame(), date, mu);
        Assert.assertEquals(p.getAlphaV().getReal() + raan.getReal(), lv.getReal(), Utils.epsilonAngle * FastMath.abs(lv.getReal()));
        Assert.assertEquals(p.getAlphaE().getReal() + raan.getReal(), lE.getReal(), Utils.epsilonAngle * FastMath.abs(lE.getReal()));
        Assert.assertEquals(p.getAlphaM().getReal() + raan.getReal(), lM.getReal(), Utils.epsilonAngle * FastMath.abs(lM.getReal()));
        p = new FieldCircularOrbit<T>(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), zero, PositionAngle.TRUE, p.getFrame(), date, mu);

        p = new FieldCircularOrbit<T>(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), lM.subtract(raan), PositionAngle.MEAN, p.getFrame(), date, mu);
        Assert.assertEquals(p.getAlphaV().getReal() + raan.getReal(), lv.getReal(), Utils.epsilonAngle * FastMath.abs(lv.getReal()));
        Assert.assertEquals(p.getAlphaE().getReal() + raan.getReal(), lE.getReal(), Utils.epsilonAngle * FastMath.abs(lE.getReal()));
        Assert.assertEquals(p.getAlphaM().getReal() + raan.getReal(), lM.getReal(), Utils.epsilonAngle * FastMath.abs(lM.getReal()));

    }

    public <T extends RealFieldElement<T>> void testAnomalyCirc(Field<T> field) {
        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));
        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<T>( position, velocity);
        FieldCircularOrbit<T>  p = new FieldCircularOrbit<T>(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        T raan = p.getRightAscensionOfAscendingNode();

        // circular orbit
        p = new FieldCircularOrbit<T>(p.getA() , zero, zero, p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), p.getAlphaV(), PositionAngle.TRUE, p.getFrame(), date, mu);

        T lv = zero.add(1.1);
        T lE = lv;
        T lM = lE;

        p = new FieldCircularOrbit<T>(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), lv.subtract(raan), PositionAngle.TRUE, p.getFrame(), date, mu);
        Assert.assertEquals(p.getAlphaV().getReal() + raan.getReal(), lv.getReal(), Utils.epsilonAngle * FastMath.abs(lv.getReal()));
        Assert.assertEquals(p.getAlphaE().getReal() + raan.getReal(), lE.getReal(), Utils.epsilonAngle * FastMath.abs(lE.getReal()));
        Assert.assertEquals(p.getAlphaM().getReal() + raan.getReal(), lM.getReal(), Utils.epsilonAngle * FastMath.abs(lM.getReal()));
        p = new FieldCircularOrbit<T>(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), zero, PositionAngle.TRUE, p.getFrame(), date, mu);

        p = new FieldCircularOrbit<T>(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), lE.subtract(raan), PositionAngle.ECCENTRIC, p.getFrame(), date, mu);

        Assert.assertEquals(p.getAlphaV().getReal() + raan.getReal(), lv.getReal(), Utils.epsilonAngle * FastMath.abs(lv.getReal()));
        Assert.assertEquals(p.getAlphaE().getReal() + raan.getReal(), lE.getReal(), Utils.epsilonAngle * FastMath.abs(lE.getReal()));
        Assert.assertEquals(p.getAlphaM().getReal() + raan.getReal(), lM.getReal(), Utils.epsilonAngle * FastMath.abs(lM.getReal()));
        p = new FieldCircularOrbit<T>(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), zero, PositionAngle.TRUE, p.getFrame(), date, mu);

        p = new FieldCircularOrbit<T>(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), lM.subtract(raan), PositionAngle.MEAN, p.getFrame(), date, mu);
        Assert.assertEquals(p.getAlphaV().getReal() + raan.getReal(), lv.getReal(), Utils.epsilonAngle * FastMath.abs(lv.getReal()));
        Assert.assertEquals(p.getAlphaE().getReal() + raan.getReal(), lE.getReal(), Utils.epsilonAngle * FastMath.abs(lE.getReal()));
        Assert.assertEquals(p.getAlphaM().getReal() + raan.getReal(), lM.getReal(), Utils.epsilonAngle * FastMath.abs(lM.getReal()));

    }

    public <T extends RealFieldElement<T>> void testPositionVelocityNormsEll(Field<T> field) {
        T zero =  field.getZero();
        T one =  field.getOne();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

        // elliptic and non equatorial (i retrograde) orbit
        T hx =  zero.add(1.2);
        T hy =  zero.add(2.1);
        T i  = hx.multiply(hx).add(hy.multiply(hy)).sqrt().atan().multiply(2);
        T raan = hy.atan2(hx);
        FieldCircularOrbit<T> p =
            new FieldCircularOrbit<T>(zero.add(42166.712), zero.add(0.5), zero.add(-0.5), i, raan,
                                   raan.negate().add(0.67), PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), date, mu);

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

    public <T extends RealFieldElement<T>> void testNumericalIssue25(Field<T> field) throws OrekitException {

        T zero =  field.getZero();
        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(3782116.14107698), zero.add(416663.11924914), zero.add(5875541.62103057));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-6349.7848910501), zero.add(288.4061811651), zero.add(4066.9366759691));
        FieldCircularOrbit<T> orbit = new FieldCircularOrbit<T>(new FieldPVCoordinates<T>(position, velocity),
                                                FramesFactory.getEME2000(),
                                                new FieldAbsoluteDate<T>(field, "2004-01-01T23:00:00.000",
                                                                 TimeScalesFactory.getUTC()),
                                                                 3.986004415E14);
        Assert.assertEquals(0.0, orbit.getE().getReal(), 2.0e-14);
    }

    public <T extends RealFieldElement<T>> void testPerfectlyEquatorial(Field<T> field) throws OrekitException {
        T zero =  field.getZero();
        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(-7293947.695148368),zero.add( 5122184.668436634), zero.add(0.0));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-3890.4029433398),zero.add( -5369.811285264604), zero.add(0.0));
        FieldCircularOrbit<T> orbit = new FieldCircularOrbit<T>(new FieldPVCoordinates<T>(position, velocity),
                                                FramesFactory.getEME2000(),
                                                new FieldAbsoluteDate<T>(field,"2004-01-01T23:00:00.000",
                                                                 TimeScalesFactory.getUTC()),
                                                3.986004415E14);
        Assert.assertEquals(0.0, orbit.getI().getReal(), 2.0e-14);
        Assert.assertEquals(0.0, orbit.getRightAscensionOfAscendingNode().getReal(), 2.0e-14);
    }

    public <T extends RealFieldElement<T>> void testPositionVelocityNormsCirc(Field<T> field) {
        T zero =  field.getZero();
        T one =  field.getOne();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

        // elliptic and non equatorial (i retrograde) orbit
        T hx =  zero.add(0.1e-8);
        T hy =  zero.add(0.1e-8);
        T i  = hx.multiply(hx).add(hy.multiply(hy)).sqrt().atan().multiply(2);
        T raan = hy.atan2(hx);
        FieldCircularOrbit<T> pCirEqua =
            new FieldCircularOrbit<T>(zero.add(42166.712), zero.add(0.1e-8), zero.add(0.1e-8), i, raan,
                                   raan.negate().add(0.67), PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), date, mu);

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

    public <T extends RealFieldElement<T>> void testGeometryEll(Field<T> field) {

        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        // elliptic and non equatorial (i retrograde) orbit
        T hx =  zero.add(1.2);
        T hy =  zero.add(2.1);
        T i  = hx.multiply(hx).add(hy.multiply(hy)).sqrt().atan().multiply(2);
        T raan = hy.atan2(hx);
        FieldCircularOrbit<T> p =
            new FieldCircularOrbit<T>(zero.add(42166.712), zero.add(0.5), zero.add(-0.5), i, raan,
                                   raan.negate().add(0.67), PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), date, mu);

        FieldVector3D<T> position = p.getPVCoordinates().getPosition();
        FieldVector3D<T> velocity = p.getPVCoordinates().getVelocity();
        FieldVector3D<T> momentum = p.getPVCoordinates().getMomentum().normalize();

        T apogeeRadius  = p.getA().multiply( p.getE().add(1));
        T perigeeRadius = p.getA().multiply(p.getE().negate().add(1));

        for (T alphaV = zero; alphaV.getReal() <= 2 * FastMath.PI; alphaV=alphaV.add(zero.add(2).multiply(FastMath.PI/100.))) {
            p = new FieldCircularOrbit<T>(p.getA() , p.getCircularEx(), p.getCircularEy(), p.getI(),
                                       p.getRightAscensionOfAscendingNode(),
                                       alphaV, PositionAngle.TRUE, p.getFrame(), date, mu);
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

    public <T extends RealFieldElement<T>> void testGeometryCirc(Field<T> field) {

        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

        //  circular and equatorial orbit
        T hx =  zero.add(0.1e-8);
        T hy =  zero.add(0.1e-8);
        T i  =  hx.multiply(hx).add(hy.multiply(hy)).sqrt().atan().multiply(2);
        T raan = hy.atan2(hx);
        FieldCircularOrbit<T> pCirEqua =
            new FieldCircularOrbit<T>(zero.add(42166.712), zero.add(0.1e-8), zero.add(0.1e-8), i, raan,
                                   raan.negate().add(0.67), PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), date, mu);

        FieldVector3D<T> position = pCirEqua.getPVCoordinates().getPosition();
        FieldVector3D<T> velocity = pCirEqua.getPVCoordinates().getVelocity();
        FieldVector3D<T> momentum = pCirEqua.getPVCoordinates().getMomentum().normalize();

        T apogeeRadius  = pCirEqua.getA().multiply( pCirEqua.getE().add(1));
        T perigeeRadius = pCirEqua.getA().multiply(pCirEqua.getE().negate().add(1));
        // test if apogee equals perigee
        Assert.assertEquals(perigeeRadius.getReal(), apogeeRadius.getReal(), 1.e+4 * Utils.epsilonTest * apogeeRadius.getReal());

        for (T alphaV = zero; alphaV.getReal() <= 2 * FastMath.PI; alphaV = alphaV.add(zero.add(2 * FastMath.PI/100.))) {
            pCirEqua = new FieldCircularOrbit<T>(pCirEqua.getA() , pCirEqua.getCircularEx(), pCirEqua.getCircularEy(), pCirEqua.getI(),
                                              pCirEqua.getRightAscensionOfAscendingNode(),
                                              alphaV, PositionAngle.TRUE, pCirEqua.getFrame(), date, mu);
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

    public <T extends RealFieldElement<T>> void testSymmetryEll(Field<T> field) {

        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

        // elliptic and non equatorial orbit
        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(4512.9), zero.add(18260.), zero.add(-5127.));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(134664.6), zero.add(90066.8), zero.add(72047.6));
        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<T>(position, velocity);

        FieldCircularOrbit<T> p = new FieldCircularOrbit<T>(pvCoordinates, FramesFactory.getEME2000(), date, mu);

        FieldVector3D<T> positionOffset = p.getPVCoordinates().getPosition();
        FieldVector3D<T> velocityOffset = p.getPVCoordinates().getVelocity();

        positionOffset = positionOffset.subtract(position);
        velocityOffset = velocityOffset.subtract(velocity);

        Assert.assertEquals(0.0, positionOffset.getNorm().getReal(), position.getNorm().getReal() * Utils.epsilonTest);
        Assert.assertEquals(0.0, velocityOffset.getNorm().getReal(), velocity.getNorm().getReal() * Utils.epsilonTest);

    }

    public <T extends RealFieldElement<T>> void testSymmetryCir(Field<T> field) {
        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

        // circular and equatorial orbit
        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(33051.2), zero.add(26184.9), zero.add(-1.3E-5));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-60376.2), zero.add(76208.), zero.add(2.7E-4));
        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<T>(position, velocity);

        FieldCircularOrbit<T> p = new FieldCircularOrbit<T>(pvCoordinates, FramesFactory.getEME2000(), date, mu);

        FieldVector3D<T> positionOffset = p.getPVCoordinates().getPosition().subtract(position);
        FieldVector3D<T> velocityOffset = p.getPVCoordinates().getVelocity().subtract(velocity);

        Assert.assertEquals(0.0, positionOffset.getNorm().getReal(), position.getNorm().getReal() * Utils.epsilonTest);
        Assert.assertEquals(0.0, velocityOffset.getNorm().getReal(), velocity.getNorm().getReal() * Utils.epsilonTest);

    }

    public <T extends RealFieldElement<T>> void testNonInertialFrame(Field<T> field) throws IllegalArgumentException {
        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(33051.2), zero.add(26184.9), zero.add(-1.3E-5));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-60376.2), zero.add(76208.), zero.add(2.7E-4));
        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<T>( position, velocity);
        new FieldCircularOrbit<T>(pvCoordinates,
                          new Frame(FramesFactory.getEME2000(), Transform.IDENTITY, "non-inertial", false),
                          date, mu);
    }

    public <T extends RealFieldElement<T>> void testJacobianReference(Field<T> field) throws OrekitException {
        T zero =  field.getZero();
        FieldAbsoluteDate<T> dateTca = new FieldAbsoluteDate<T>(field,2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        FieldCircularOrbit<T> orbCir = new FieldCircularOrbit<T>(zero.add(7000000.0), zero.add(0.01), zero.add(-0.02), zero.add(1.2), zero.add(2.1),
                                    zero.add(0.7), PositionAngle.MEAN,
                                                 FramesFactory.getEME2000(), dateTca, mu);

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
        FieldVector3D<T> pRef = new FieldVector3D<T>(zero.add(-4106905.105389204807580),zero.add( 3603162.539798960555345), zero.add(4439730.167038885876536));
        FieldVector3D<T> vRef = new FieldVector3D<T>(zero.add(740.132407342422994), zero.add(-5308.773280141396754),zero.add( 5250.338353483879473));
        double[][] jRefR = {
            { -1.1535467596325562      ,        1.0120556393573172,        1.2470306024626943,        181.96913090864561,       -1305.2162699469984,        1290.8494448855752      },
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

    public <T extends RealFieldElement<T>> void testJacobianFinitedifferences(Field<T> field) throws OrekitException {
        T zero =  field.getZero();

        FieldAbsoluteDate<T> dateTca = new FieldAbsoluteDate<T>(field, 2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        FieldCircularOrbit<T> orbCir = new FieldCircularOrbit<T>(zero.add(7000000.0), zero.add(0.01), zero.add(-0.02), zero.add(1.2), zero.add(2.1),
                                                 zero.add(0.7), PositionAngle.MEAN,
                                                 FramesFactory.getEME2000(), dateTca, mu);

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
        throws OrekitException {
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
        FieldVector3D<T> dP = new FieldVector3D<T>(zero, zero, zero);
        FieldVector3D<T> dV = new FieldVector3D<T>(zero, zero, zero);
        switch (i) {
        case 0:
            h = hP;
            dP = new FieldVector3D<T>(hP, zero, zero);
            break;
        case 1:
            h = hP;
            dP = new FieldVector3D<T>(zero, hP, zero);
            break;
        case 2:
            h = hP;
            dP = new FieldVector3D<T>(zero, zero, hP);
            break;
        case 3:
            h = hV;
            dV = new FieldVector3D<T>(hV, zero, zero);
            break;
        case 4:
            h = hV;
            dV = new FieldVector3D<T>(zero, hV, zero);
            break;
        default:
            h = hV;
            dV = new FieldVector3D<T>(zero, zero, hV);
            break;
        }

        FieldCircularOrbit<T> oM4h = new FieldCircularOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(1, p, -4, dP), new FieldVector3D<T>(1, v, -4, dV)),
                                               orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldCircularOrbit<T> oM3h = new FieldCircularOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(1, p, -3, dP), new FieldVector3D<T>(1, v, -3, dV)),
                                               orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldCircularOrbit<T> oM2h = new FieldCircularOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(1, p, -2, dP), new FieldVector3D<T>(1, v, -2, dV)),
                                               orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldCircularOrbit<T> oM1h = new FieldCircularOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(1, p, -1, dP), new FieldVector3D<T>(1, v, -1, dV)),
                                               orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldCircularOrbit<T> oP1h = new FieldCircularOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(1, p, +1, dP), new FieldVector3D<T>(1, v, +1, dV)),
                                               orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldCircularOrbit<T> oP2h = new FieldCircularOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(1, p, +2, dP), new FieldVector3D<T>(1, v, +2, dV)),
                                               orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldCircularOrbit<T> oP3h = new FieldCircularOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(1, p, +3, dP), new FieldVector3D<T>(1, v, +3, dV)),
                                               orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldCircularOrbit<T> oP4h = new FieldCircularOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(1, p, +4, dP), new FieldVector3D<T>(1, v, +4, dV)),
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

    public <T extends RealFieldElement<T>> void testInterpolation(Field<T> field) throws OrekitException {
        T zero =  field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

        final double ehMu  = 3.9860047e14;
        final double ae    = 6.378137e6;
        final T c20   = zero.add(-1.08263e-3);
        final T c30   = zero.add(2.54e-6);
        final T c40   = zero.add(1.62e-6);
        final T c50   = zero.add(2.3e-7);
        final T c60   = zero.add(-5.5e-7);

        date = date.shiftedBy(584.);
        final FieldVector3D<T> position = new FieldVector3D<T>(zero.add(3220103.), zero.add(69623.), zero.add(6449822.));
        final FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(6414.7), zero.add(-2006.), zero.add(-3180.));
        final FieldCircularOrbit<T> initialOrbit = new FieldCircularOrbit<T>(new FieldPVCoordinates<T>(position, velocity),
                                                             FramesFactory.getEME2000(), date, ehMu);

        FieldEcksteinHechlerPropagator<T> propagator =
                new FieldEcksteinHechlerPropagator<T>(initialOrbit, ae, ehMu, c20, c30, c40, c50, c60);

        // set up a 5 points sample
        List<FieldOrbit<T>> sample = new ArrayList<FieldOrbit<T>>();
        for (T dt = zero; dt.getReal() < 300.0; dt = dt.add(60.0)) {
            sample.add(propagator.propagate(date.shiftedBy(dt)).getOrbit());
        }

        // well inside the sample, interpolation should be much better than Keplerian shift
        double maxShiftError = 0.0;
        double maxInterpolationError = 0.0;
        for (T dt = zero; dt.getReal() < 241.0; dt = dt.add(1.0)) {
            FieldAbsoluteDate<T> t        = initialOrbit.getDate().shiftedBy(dt);
            FieldVector3D<T> shifted      = initialOrbit.shiftedBy(dt).getPVCoordinates().getPosition();
            FieldVector3D<T> interpolated = initialOrbit.interpolate(t, sample).getPVCoordinates().getPosition();
            FieldVector3D<T> propagated   = propagator.propagate(t).getPVCoordinates().getPosition();
            maxShiftError = FastMath.max(maxShiftError, shifted.subtract(propagated).getNorm().getReal());
            maxInterpolationError = FastMath.max(maxInterpolationError, interpolated.subtract(propagated).getNorm().getReal());
        }
        Assert.assertTrue(maxShiftError         > 390.0);
        Assert.assertTrue(maxInterpolationError < 0.04);

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
        Assert.assertTrue(maxShiftError         <  610.0);
        Assert.assertTrue(maxInterpolationError <    1.3);

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
        Assert.assertTrue(maxShiftError         < 5000.0);
        Assert.assertTrue(maxInterpolationError > 8800.0);

    }


}
