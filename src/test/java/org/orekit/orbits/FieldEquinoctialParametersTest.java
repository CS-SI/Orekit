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


public class FieldEquinoctialParametersTest {

    // Body mu
    private double mu;

    @Before
    public void setUp() {

        Utils.setDataRoot("regular-data");

        // Body mu
        mu = 3.9860047e14;
    }

    @Test
    public void doEquinToEquinTest() throws OrekitException{
        testEquinoctialToEquinoctialEll(Decimal64Field.getInstance());
    }

    @Test
    public void doEquinToEquinCircTest() throws OrekitException{
        testEquinoctialToEquinoctialCirc(Decimal64Field.getInstance());
    }

    @Test
    public void doEquinToCartTest() throws OrekitException{
        testEquinoctialToCartesian(Decimal64Field.getInstance());
    }

    @Test
    public void doEquinToKeplTest() throws OrekitException{
        testEquinoctialToKeplerian(Decimal64Field.getInstance());
    }

    @Test
    public void doNumerialIssue25Test() throws OrekitException{
        testNumericalIssue25(Decimal64Field.getInstance());
    }

    @Test
    public void doAnomalyTest() throws OrekitException{
        testAnomaly(Decimal64Field.getInstance());
    }

    @Test
    public void doPositionVelocityTest() throws OrekitException{
        testPositionVelocityNorms(Decimal64Field.getInstance());
    }

    @Test
    public void doGeometryTest() throws OrekitException{
        testGeometry(Decimal64Field.getInstance());
    }

    @Test
    public void doRadiusOfCurvTest() throws OrekitException{
        testRadiusOfCurvature(Decimal64Field.getInstance());
    }

    @Test
    public void doSymmetryTest() throws OrekitException{
        testSymmetry(Decimal64Field.getInstance());
    }

    @Test
    public void doJacobianReferenceTest() throws OrekitException{
        testJacobianReference(Decimal64Field.getInstance());
    }

    @Test
    public void doJacobianFinitedDiffTest() throws OrekitException{
        testJacobianFinitedifferences(Decimal64Field.getInstance());
    }

    @Test
    public void doInterpolationTest() throws OrekitException{
        testInterpolation(Decimal64Field.getInstance());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public <T extends RealFieldElement<T>> void testErr1(){
        testHyperbolic(Decimal64Field.getInstance());
    }

    @Test(expected=IllegalArgumentException.class)
    public <T extends RealFieldElement<T>> void testErr2(){
        testNonInertialFrame(Decimal64Field.getInstance());
    }



    public <T extends RealFieldElement<T>> void testEquinoctialToEquinoctialEll(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

        T ix = zero.add(1.200e-04);
        T iy = zero.add(-1.16e-04);
        T inc = ix.multiply(ix).add(iy.multiply(iy)).divide(4.).sqrt().asin().multiply(2);
        T hx = inc.divide(2.).tan().multiply(ix).divide(inc.divide(2.).sin().multiply(2));
        T hy = inc.divide(2.).tan().multiply(iy).divide(inc.divide(2.).sin().multiply(2));

        // elliptic orbit
        FieldEquinoctialOrbit<T> equi =
            new FieldEquinoctialOrbit<T>(zero.add(42166.712),zero.add( 0.5), zero.add(-0.5), hx, hy,
                                 zero.add(5.300), PositionAngle.MEAN,
                                      FramesFactory.getEME2000(), date, mu);
        FieldVector3D<T> pos = equi.getPVCoordinates().getPosition();
        FieldVector3D<T> vit = equi.getPVCoordinates().getVelocity();

        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<T>(pos,vit);

        FieldEquinoctialOrbit<T> param = new FieldEquinoctialOrbit<T>(FieldPVCoordinates, FramesFactory.getEME2000(), date, mu);
        Assert.assertEquals(param.getA().getReal(), equi.getA().getReal(), Utils.epsilonTest * equi.getA().getReal());
        Assert.assertEquals(param.getEquinoctialEx().getReal(), equi.getEquinoctialEx().getReal(),
                     Utils.epsilonE * FastMath.abs(equi.getE().getReal()));
        Assert.assertEquals(param.getEquinoctialEy().getReal(), equi.getEquinoctialEy().getReal(),
                     Utils.epsilonE * FastMath.abs(equi.getE().getReal()));
        Assert.assertEquals(param.getHx().getReal(), equi.getHx().getReal(), Utils.epsilonAngle
                     * FastMath.abs(equi.getI().getReal()));
        Assert.assertEquals(param.getHy().getReal(), equi.getHy().getReal(), Utils.epsilonAngle
                     * FastMath.abs(equi.getI().getReal()));
        Assert.assertEquals(MathUtils.normalizeAngle(param.getLv().getReal(), equi.getLv().getReal()), equi.getLv().getReal(),
                     Utils.epsilonAngle * FastMath.abs(equi.getLv().getReal()));

    }

    public <T extends RealFieldElement<T>>  void testEquinoctialToEquinoctialCirc(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

        T ix = zero.add(1.200e-04);
        T iy = zero.add(-1.16e-04);
        T inc = ix.multiply(ix).add(iy.multiply(iy)).divide(4.).sqrt().asin().multiply(2);
        T hx = inc.divide(2.).tan().multiply(ix).divide(inc.divide(2.).sin().multiply(2));
        T hy = inc.divide(2.).tan().multiply(iy).divide(inc.divide(2.).sin().multiply(2));

        // circular orbit
        FieldEquinoctialOrbit<T> equiCir =
            new FieldEquinoctialOrbit<T>(zero.add(42166.712), zero.add(0.1e-10), zero.add(-0.1e-10), hx, hy,
                            zero.add(5.300), PositionAngle.MEAN,
                                      FramesFactory.getEME2000(), date, mu);
        FieldVector3D<T> posCir = equiCir.getPVCoordinates().getPosition();
        FieldVector3D<T> vitCir = equiCir.getPVCoordinates().getVelocity();

        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<T>(posCir,vitCir);

        FieldEquinoctialOrbit<T> paramCir = new FieldEquinoctialOrbit<T>(FieldPVCoordinates, FramesFactory.getEME2000(),
                                                         date, mu);
        Assert.assertEquals(paramCir.getA().getReal(), equiCir.getA().getReal(), Utils.epsilonTest
                     * equiCir.getA().getReal());
        Assert.assertEquals(paramCir.getEquinoctialEx().getReal(), equiCir.getEquinoctialEx().getReal(),
                     Utils.epsilonEcir * FastMath.abs(equiCir.getE().getReal()));
        Assert.assertEquals(paramCir.getEquinoctialEy().getReal(), equiCir.getEquinoctialEy().getReal(),
                     Utils.epsilonEcir * FastMath.abs(equiCir.getE().getReal()));
        Assert.assertEquals(paramCir.getHx().getReal(), equiCir.getHx().getReal(), Utils.epsilonAngle
                     * FastMath.abs(equiCir.getI().getReal()));
        Assert.assertEquals(paramCir.getHy().getReal(), equiCir.getHy().getReal(), Utils.epsilonAngle
                     * FastMath.abs(equiCir.getI().getReal()));
        Assert.assertEquals(MathUtils.normalizeAngle(paramCir.getLv().getReal(), equiCir.getLv().getReal()), equiCir
                     .getLv().getReal(), Utils.epsilonAngle * FastMath.abs(equiCir.getLv().getReal()));

    }

    public <T extends RealFieldElement<T>>  void testEquinoctialToCartesian(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

        T ix = zero.add(1.200e-04);
        T iy = zero.add(-1.16e-04);
        T inc = ix.multiply(ix).add(iy.multiply(iy)).divide(4.).sqrt().asin().multiply(2);
        T hx = inc.divide(2.).tan().multiply(ix).divide(inc.divide(2.).sin().multiply(2));
        T hy = inc.divide(2.).tan().multiply(iy).divide(inc.divide(2.).sin().multiply(2));

        FieldEquinoctialOrbit<T> equi =
                        new FieldEquinoctialOrbit<T>(zero.add(42166.712), zero.add(-7.900e-06), zero.add(1.100e-04), hx, hy,
                                        zero.add(5.300), PositionAngle.MEAN,
                                                  FramesFactory.getEME2000(), date, mu);
        FieldVector3D<T> pos = equi.getPVCoordinates().getPosition();
        FieldVector3D<T> vit = equi.getPVCoordinates().getVelocity();

        // verif of 1/a = 2/X - V2/mu
        T oneovera = (pos.getNorm().reciprocal().multiply(2)).subtract(vit.getNorm().multiply(vit.getNorm()).divide(mu));
        Assert.assertEquals(oneovera.getReal(), 1. / equi.getA().getReal(), 1.0e-7);

        Assert.assertEquals(0.233745668678733e+05, pos.getX().getReal(), Utils.epsilonTest
                     * FastMath.abs(pos.getX().getReal()));
        Assert.assertEquals(-0.350998914352669e+05, pos.getY().getReal(), Utils.epsilonTest
                     * FastMath.abs(pos.getY().getReal()));
        Assert.assertEquals(-0.150053723123334e+01, pos.getZ().getReal(), Utils.epsilonTest
                     * FastMath.abs(pos.getZ().getReal()));

        Assert.assertEquals(0.809135038364960e+05, vit.getX().getReal(), Utils.epsilonTest
                     * FastMath.abs(vit.getX().getReal()));
        Assert.assertEquals(0.538902268252598e+05, vit.getY().getReal(), Utils.epsilonTest
                     * FastMath.abs(vit.getY().getReal()));
        Assert.assertEquals(0.158527938296630e+02, vit.getZ().getReal(), Utils.epsilonTest
                     * FastMath.abs(vit.getZ().getReal()));
    }

    public <T extends RealFieldElement<T>>  void testEquinoctialToKeplerian(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

        T ix = zero.add(1.200e-04);
        T iy = zero.add(-1.16e-04);
        T inc = ix.multiply(ix).add(iy.multiply(iy)).divide(4.).sqrt().asin().multiply(2);
        T hx = inc.divide(2.).tan().multiply(ix).divide(inc.divide(2.).sin().multiply(2));
        T hy = inc.divide(2.).tan().multiply(iy).divide(inc.divide(2.).sin().multiply(2));

        FieldEquinoctialOrbit<T> equi =
                        new FieldEquinoctialOrbit<T>(zero.add(42166.712), zero.add(-7.900e-06), zero.add(1.100e-04), hx, hy,
                                        zero.add(5.300), PositionAngle.MEAN,
                                                  FramesFactory.getEME2000(), date, mu);

        FieldKeplerianOrbit<T> kep = new FieldKeplerianOrbit<T>(equi);

        Assert.assertEquals(42166.71200, equi.getA().getReal(), Utils.epsilonTest * kep.getA().getReal());
        Assert.assertEquals(0.110283316961361e-03, kep.getE().getReal(), Utils.epsilonE
                     * FastMath.abs(kep.getE().getReal()));
        Assert.assertEquals(0.166901168553917e-03, kep.getI().getReal(), Utils.epsilonAngle
                     * FastMath.abs(kep.getI().getReal()));
        Assert.assertEquals(MathUtils.normalizeAngle(-3.87224326008837, kep.getPerigeeArgument().getReal()),
                     kep.getPerigeeArgument().getReal(), Utils.epsilonTest
                     * FastMath.abs(kep.getPerigeeArgument().getReal()));
        Assert.assertEquals(MathUtils.normalizeAngle(5.51473467358854, kep
                                     .getRightAscensionOfAscendingNode().getReal()), kep
                                     .getRightAscensionOfAscendingNode().getReal(), Utils.epsilonTest
                                     * FastMath.abs(kep.getRightAscensionOfAscendingNode().getReal()));
        Assert.assertEquals(MathUtils.normalizeAngle(3.65750858649982, kep.getMeanAnomaly().getReal()), kep
                     .getMeanAnomaly().getReal(), Utils.epsilonTest * FastMath.abs(kep.getMeanAnomaly().getReal()));

    }

    public <T extends RealFieldElement<T>> void testHyperbolic(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        new FieldEquinoctialOrbit<T>(zero.add(42166.712), zero.add(0.9), zero.add(0.5), zero.add(0.01), zero.add(-0.02), zero.add(5.300),
                             PositionAngle.MEAN,  FramesFactory.getEME2000(), date, mu);
    }

    public <T extends RealFieldElement<T>>  void testNumericalIssue25(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(3782116.14107698), zero.add(416663.11924914), zero.add(5875541.62103057));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-6349.7848910501), zero.add(288.4061811651), zero.add(4066.9366759691));
        FieldEquinoctialOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position, velocity),
                                                      FramesFactory.getEME2000(),
                                                      new FieldAbsoluteDate<T>(field, "2004-01-01T23:00:00.000",
                                                                       TimeScalesFactory.getUTC()),
                                                                       3.986004415E14);
        Assert.assertEquals(0.0, orbit.getE().getReal(), 2.0e-14);
    }

    public <T extends RealFieldElement<T>>  void testAnomaly(Field<T> field) {

        T one = field.getOne();
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        // elliptic orbit
        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        FieldEquinoctialOrbit<T> p = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position, velocity), FramesFactory.getEME2000(), date, mu);
        FieldKeplerianOrbit<T> kep = new FieldKeplerianOrbit<T>(p);

        T e = p.getE();
        T eRatio = one.subtract(e).divide(e.add(1)).sqrt();
        T paPraan = kep.getPerigeeArgument().add(
                       kep.getRightAscensionOfAscendingNode());

        T lv = zero.add(1.1);
        // formulations for elliptic case
        T lE = eRatio.multiply(lv.subtract(paPraan).divide(2).tan()).atan().multiply(2).add(paPraan);
        T lM = lE.subtract(e.multiply(lE.subtract(paPraan).sin()));

        p = new FieldEquinoctialOrbit<T>(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , lv , PositionAngle.TRUE,
                                      p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getLv().getReal(), lv.getReal(), Utils.epsilonAngle * FastMath.abs(lv.getReal()));
        Assert.assertEquals(p.getLE().getReal(), lE.getReal(), Utils.epsilonAngle * FastMath.abs(lE.getReal()));
        Assert.assertEquals(p.getLM().getReal(), lM.getReal(), Utils.epsilonAngle * FastMath.abs(lM.getReal()));
        p = new FieldEquinoctialOrbit<T>(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , zero , PositionAngle.TRUE,
                                      p.getFrame(), p.getDate(), p.getMu());

        p = new FieldEquinoctialOrbit<T>(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , lE , PositionAngle.ECCENTRIC,
                                      p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getLv().getReal(), lv.getReal(), Utils.epsilonAngle * FastMath.abs(lv.getReal()));
        Assert.assertEquals(p.getLE().getReal(), lE.getReal(), Utils.epsilonAngle * FastMath.abs(lE.getReal()));
        Assert.assertEquals(p.getLM().getReal(), lM.getReal(), Utils.epsilonAngle * FastMath.abs(lM.getReal()));
        p = new FieldEquinoctialOrbit<T>(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , zero , PositionAngle.TRUE,
                                      p.getFrame(), p.getDate(), p.getMu());

        p = new FieldEquinoctialOrbit<T>(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , lM , PositionAngle.MEAN,
                                      p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getLv().getReal(), lv.getReal(), Utils.epsilonAngle * FastMath.abs(lv.getReal()));
        Assert.assertEquals(p.getLE().getReal(), lE.getReal(), Utils.epsilonAngle * FastMath.abs(lE.getReal()));
        Assert.assertEquals(p.getLM().getReal(), lM.getReal(), Utils.epsilonAngle * FastMath.abs(lM.getReal()));

        // circular orbit
        p = new FieldEquinoctialOrbit<T>(p.getA() ,zero ,
                                      zero, p.getHx(), p.getHy() , p.getLv() , PositionAngle.TRUE,
                                      p.getFrame(), p.getDate(), p.getMu());

        lE = lv;
        lM = lE;

        p = new FieldEquinoctialOrbit<T>(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , lv , PositionAngle.TRUE,
                                      p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getLv().getReal(), lv.getReal(), Utils.epsilonAngle * FastMath.abs(lv.getReal()));
        Assert.assertEquals(p.getLE().getReal(), lE.getReal(), Utils.epsilonAngle * FastMath.abs(lE.getReal()));
        Assert.assertEquals(p.getLM().getReal(), lM.getReal(), Utils.epsilonAngle * FastMath.abs(lM.getReal()));
        p = new FieldEquinoctialOrbit<T>(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , zero , PositionAngle.TRUE,
                                      p.getFrame(), p.getDate(), p.getMu());

        p = new FieldEquinoctialOrbit<T>(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , lE , PositionAngle.ECCENTRIC,
                                      p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getLv().getReal(), lv.getReal(), Utils.epsilonAngle * FastMath.abs(lv.getReal()));
        Assert.assertEquals(p.getLE().getReal(), lE.getReal(), Utils.epsilonAngle * FastMath.abs(lE.getReal()));
        Assert.assertEquals(p.getLM().getReal(), lM.getReal(), Utils.epsilonAngle * FastMath.abs(lM.getReal()));
        p = new FieldEquinoctialOrbit<T>(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , zero , PositionAngle.TRUE,
                                      p.getFrame(), p.getDate(), p.getMu());

        p = new FieldEquinoctialOrbit<T>(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , lM , PositionAngle.MEAN, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getLv().getReal(), lv.getReal(), Utils.epsilonAngle * FastMath.abs(lv.getReal()));
        Assert.assertEquals(p.getLE().getReal(), lE.getReal(), Utils.epsilonAngle * FastMath.abs(lE.getReal()));
        Assert.assertEquals(p.getLM().getReal(), lM.getReal(), Utils.epsilonAngle * FastMath.abs(lM.getReal()));
    }

    public <T extends RealFieldElement<T>>  void testPositionVelocityNorms(Field<T> field) {

        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

        // elliptic and non equatorial (i retrograde) orbit
        FieldEquinoctialOrbit<T> p =
            new FieldEquinoctialOrbit<T>(zero.add(42166.712), zero.add(0.5), zero.add(-0.5), zero.add(1.200), zero.add(2.1),
                            zero.add(0.67), PositionAngle.TRUE,
                                      FramesFactory.getEME2000(), date, mu);

        T ex = p.getEquinoctialEx();
        T ey = p.getEquinoctialEy();
        T lv = p.getLv();
        T ksi = ex.multiply(lv.cos()).add(ey.multiply(lv.sin())).add(1);
        T nu = ex.multiply(lv.sin()).subtract(ey.multiply(lv.cos()));
        T epsilon = ex.negate().multiply(ex).subtract(ey.multiply(ey)).add(1.0).sqrt();

        T a = p.getA();
        T na = a.reciprocal().multiply(p.getMu()).sqrt();

        Assert.assertEquals(a.getReal() * epsilon.getReal() * epsilon.getReal() / ksi.getReal(),
                            p.getPVCoordinates().getPosition().getNorm().getReal(),
                     Utils.epsilonTest * FastMath.abs(p.getPVCoordinates().getPosition().getNorm().getReal()));
        Assert.assertEquals(na.getReal() * FastMath.sqrt(ksi.getReal() * ksi.getReal() + nu.getReal() * nu.getReal()) / epsilon.getReal(),
                            p.getPVCoordinates().getVelocity().getNorm().getReal(),
                            Utils.epsilonTest * FastMath.abs(p.getPVCoordinates().getVelocity().getNorm().getReal()));

        // circular and equatorial orbit
        FieldEquinoctialOrbit<T> pCirEqua =
            new FieldEquinoctialOrbit<T>(zero.add(42166.712), zero.add(0.1e-8), zero.add(0.1e-8), zero.add(0.1e-8), zero.add(0.1e-8),
                            zero.add(0.67), PositionAngle.TRUE,
                                      FramesFactory.getEME2000(), date, mu);

        ex = pCirEqua.getEquinoctialEx();
        ey = pCirEqua.getEquinoctialEy();
        lv = pCirEqua.getLv();
        ksi = ex.multiply(lv.cos()).add(ey.multiply(lv.sin())).add(1);
        nu = ex.multiply(lv.sin()).subtract(ey.multiply(lv.cos()));
        epsilon = ex.negate().multiply(ex).subtract(ey.multiply(ey)).add(1.0).sqrt();

        a = pCirEqua.getA();
        na = a.reciprocal().multiply(p.getMu()).sqrt();

        Assert.assertEquals(a.getReal() * epsilon.getReal() * epsilon.getReal() / ksi.getReal(), pCirEqua.getPVCoordinates().getPosition()
                            .getNorm().getReal(), Utils.epsilonTest
                            * FastMath.abs(pCirEqua.getPVCoordinates().getPosition().getNorm().getReal()));
        Assert.assertEquals(na.getReal() * FastMath.sqrt(ksi.getReal() * ksi.getReal() + nu.getReal() * nu.getReal()) / epsilon.getReal(),
                            pCirEqua.getPVCoordinates().getVelocity().getNorm().getReal(), Utils.epsilonTest
                            * FastMath.abs(pCirEqua.getPVCoordinates().getVelocity().getNorm().getReal()));
    }

    public <T extends RealFieldElement<T>>  void testGeometry(Field<T> field) {


        T one = field.getOne();
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);


        // elliptic and non equatorial (i retrograde) orbit
        FieldEquinoctialOrbit<T> p =
            new FieldEquinoctialOrbit<T>(zero.add(42166.712), zero.add(0.5), zero.add(-0.5), zero.add(1.200), zero.add(2.1),
                            zero.add(0.67), PositionAngle.TRUE,
                                      FramesFactory.getEME2000(), date, mu);

        FieldVector3D<T> position = p.getPVCoordinates().getPosition();
        FieldVector3D<T> velocity = p.getPVCoordinates().getVelocity();
        FieldVector3D<T> momentum = p.getPVCoordinates().getMomentum().normalize();

        T apogeeRadius = p.getA().multiply(p.getE().add(1.0));
        T perigeeRadius = p.getA().multiply(one.subtract(p.getE()));

        for (T lv = zero; lv.getReal() <= 2 * FastMath.PI; lv = lv.add(2 * FastMath.PI / 100.)) {
            p = new FieldEquinoctialOrbit<T>(p.getA() ,p.getEquinoctialEx(),
                                          p.getEquinoctialEy() , p.getHx(), p.getHy() , lv , PositionAngle.TRUE,
                                          p.getFrame(), p.getDate(), p.getMu());
            position = p.getPVCoordinates().getPosition();

            // test if the norm of the position is in the range [perigee radius,
            // apogee radius]
            // Warning: these tests are without absolute value by choice
            Assert.assertTrue((position.getNorm().getReal() - apogeeRadius.getReal()) <= (apogeeRadius.getReal() * Utils.epsilonTest));
            Assert.assertTrue((position.getNorm().getReal() - perigeeRadius.getReal()) >= (-perigeeRadius.getReal() * Utils.epsilonTest));

            position = position.normalize();
            velocity = p.getPVCoordinates().getVelocity();
            velocity = velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and
            // momemtum) are normalized here

            // test of orthogonality between position and momentum
            Assert.assertTrue(FastMath.abs(FieldVector3D.dotProduct(position, momentum).getReal()) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assert.assertTrue(FastMath.abs(FieldVector3D.dotProduct(velocity, momentum).getReal()) < Utils.epsilonTest);
        }


        // circular and equatorial orbit
        FieldEquinoctialOrbit<T> pCirEqua =
            new FieldEquinoctialOrbit<T>(zero.add(42166.712), zero.add(0.1e-8), zero.add(0.1e-8), zero.add(0.1e-8), zero.add(0.1e-8),
                            zero.add(0.67), PositionAngle.TRUE,
                                      FramesFactory.getEME2000(), date, mu);

        position = pCirEqua.getPVCoordinates().getPosition();
        velocity = pCirEqua.getPVCoordinates().getVelocity();

        momentum = FieldVector3D.crossProduct(position, velocity).normalize();

        apogeeRadius = pCirEqua.getA().multiply(pCirEqua.getE().add(1));
        perigeeRadius = pCirEqua.getA().multiply(one.subtract(pCirEqua.getE()));
        // test if apogee equals perigee
        Assert.assertEquals(perigeeRadius.getReal(), apogeeRadius.getReal(), 1.e+4 * Utils.epsilonTest
                     * apogeeRadius.getReal());

        for (T lv = zero; lv.getReal() <= 2 * FastMath.PI; lv =lv.add(2 * FastMath.PI / 100.)) {
            pCirEqua = new FieldEquinoctialOrbit<T>(pCirEqua.getA() ,pCirEqua.getEquinoctialEx(),
                                                 pCirEqua.getEquinoctialEy() , pCirEqua.getHx(), pCirEqua.getHy() , lv , PositionAngle.TRUE,
                                                 pCirEqua.getFrame(), p.getDate(), p.getMu());
            position = pCirEqua.getPVCoordinates().getPosition();

            // test if the norm pf the position is in the range [perigee radius,
            // apogee radius]
            Assert.assertTrue((position.getNorm().getReal() - apogeeRadius.getReal()) <= (apogeeRadius.getReal() * Utils.epsilonTest));
            Assert.assertTrue((position.getNorm().getReal() - perigeeRadius.getReal()) >= (-perigeeRadius.getReal() * Utils.epsilonTest));

            position = position.normalize();
            velocity = pCirEqua.getPVCoordinates().getVelocity();
            velocity = velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and
            // momemtum) are normalized here

            // test of orthogonality between position and momentum
            Assert.assertTrue(FastMath.abs(FieldVector3D.dotProduct(position, momentum).getReal()) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assert.assertTrue(FastMath.abs(FieldVector3D.dotProduct(velocity, momentum).getReal()) < Utils.epsilonTest);
        }
    }

    public <T extends RealFieldElement<T>>  void testRadiusOfCurvature(Field<T> field) {
        T one = field.getOne();
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

        // elliptic and non equatorial (i retrograde) orbit
        FieldEquinoctialOrbit<T> p =
            new FieldEquinoctialOrbit<T>(zero.add(42166.712), zero.add(0.5), zero.add(-0.5), zero.add(1.200), zero.add(2.1),
                            zero.add(0.67), PositionAngle.TRUE,
                                      FramesFactory.getEME2000(), date, mu);

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


        Assert.assertEquals(rCart.getReal(), rOrb.getReal(), 1.0e-15 * p.getA().getReal());

        // at this place for such an eccentric orbit,
        // the radius of curvature is much smaller than semi major axis
        Assert.assertEquals(0.8477 * p.getA().getReal(), rCart.getReal(), 1.0e-4 * p.getA().getReal());

    }

    public <T extends RealFieldElement<T>>  void testSymmetry(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

        // elliptic and non equatorial orbit
        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(4512.9), zero.add(18260.),zero.add( -5127.));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(134664.6), zero.add(90066.8), zero.add(72047.6));

        FieldEquinoctialOrbit<T> p = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position, velocity),
                                                  FramesFactory.getEME2000(), date, mu);

        FieldVector3D<T> positionOffset = p.getPVCoordinates().getPosition().subtract(position);
        FieldVector3D<T> velocityOffset = p.getPVCoordinates().getVelocity().subtract(velocity);

        Assert.assertTrue(positionOffset.getNorm().getReal() < Utils.epsilonTest);
        Assert.assertTrue(velocityOffset.getNorm().getReal() < Utils.epsilonTest);

        // circular and equatorial orbit
        position = new FieldVector3D<T>(zero.add(33051.2), zero.add(26184.9), zero.add(-1.3E-5));
        velocity = new FieldVector3D<T>(zero.add(-60376.2), zero.add(76208.), zero.add(2.7E-4));

        p = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position, velocity),
                                 FramesFactory.getEME2000(), date, mu);

        positionOffset = p.getPVCoordinates().getPosition().subtract(position);
        velocityOffset = p.getPVCoordinates().getVelocity().subtract(velocity);

        Assert.assertTrue(positionOffset.getNorm().getReal() < Utils.epsilonTest);
        Assert.assertTrue(velocityOffset.getNorm().getReal() < Utils.epsilonTest);
    }

    public <T extends RealFieldElement<T>>  void testNonInertialFrame(Field<T> field) throws IllegalArgumentException {

        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(4512.9), zero.add(18260.), zero.add(-5127.));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(134664.6), zero.add(90066.8), zero.add(72047.6));
        FieldPVCoordinates<T> FieldPVCoordinates = new FieldPVCoordinates<T>( position, velocity);
        new FieldEquinoctialOrbit<T>(FieldPVCoordinates,
                             new Frame(FramesFactory.getEME2000(), Transform.IDENTITY, "non-inertial", false),
                             date, mu);
    }

    public <T extends RealFieldElement<T>>  void testJacobianReference(Field<T> field) throws OrekitException {

        T zero = field.getZero();
        FieldAbsoluteDate<T> dateTca = new FieldAbsoluteDate<T>(field, 2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        FieldEquinoctialOrbit<T> orbEqu = new FieldEquinoctialOrbit<T>(zero.add(7000000.0), zero.add(0.01), zero.add(-0.02), zero.add(1.2), zero.add(2.1),
                        zero.add(FastMath.toRadians(40.)), PositionAngle.MEAN,
                                          FramesFactory.getEME2000(), dateTca, mu);

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
        FieldVector3D<T> pRef = new FieldVector3D<T>(zero.add(2004367.298657628707588), zero.add(6575317.978060320019722), zero.add(-1518024.843913963763043));
        FieldVector3D<T> vRef = new FieldVector3D<T>(zero.add(5574.048661495634406), zero.add(-368.839015744295409), zero.add(5009.529487849066754));
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
        Assert.assertEquals(0, pv.getPosition().subtract(pRef).getNorm().getReal(), 2.0e-16 * pRef.getNorm().getReal());
        Assert.assertEquals(0, pv.getVelocity().subtract(vRef).getNorm().getReal(), 2.0e-16 * vRef.getNorm().getReal());

        T[][] jacobian = MathArrays.buildArray(field, 6, 6);
        orbEqu.getJacobianWrtCartesian(PositionAngle.MEAN, jacobian);


        for (int i = 0; i < jacobian.length; i++) {
            T[] row    = jacobian[i];
            T[] rowRef = jRef[i];
            for (int j = 0; j < row.length; j++) {
                Assert.assertEquals(0, (row[j].getReal() - rowRef[j].getReal()) / rowRef[j].getReal(), 4.0e-15);
            }
        }

    }

    public <T extends RealFieldElement<T>>  void testJacobianFinitedifferences(Field<T> field) throws OrekitException {

        T zero = field.getZero();
        FieldAbsoluteDate<T> dateTca = new FieldAbsoluteDate<T>(field, 2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        FieldEquinoctialOrbit<T> orbEqu = new FieldEquinoctialOrbit<T>(zero.add(7000000.0), zero.add(0.01), zero.add(-0.02), zero.add(1.2), zero.add(2.1),
                        zero.add(FastMath.toRadians(40.)), PositionAngle.MEAN,
                                                       FramesFactory.getEME2000(), dateTca, mu);

        for (PositionAngle type : PositionAngle.values()) {
            T hP = zero.add(2.0);
            T[][] finiteDiffJacobian = finiteDifferencesJacobian(type, orbEqu, hP);
            T[][] jacobian = MathArrays.buildArray(field, 6, 6);
            orbEqu.getJacobianWrtCartesian(type, jacobian);

            for (int i = 0; i < jacobian.length; i++) {
                T[] row    = jacobian[i];
                T[] rowRef = finiteDiffJacobian[i];
                for (int j = 0; j < row.length; j++) {
                    Assert.assertEquals(0, (row[j].getReal() - rowRef[j].getReal()) / rowRef[j].getReal(), 4.0e-9);
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
                    Assert.assertEquals(row == column ? 1.0 : 0.0, value.getReal(), 1.0e-9);
                }

                public T end() {
                    return null;
                }
            });

        }

    }

    private <T extends RealFieldElement<T>>  T[][] finiteDifferencesJacobian(PositionAngle type, FieldEquinoctialOrbit<T> orbit, T hP)
        throws OrekitException {
        Field<T> field = hP.getField();
        T[][] jacobian = MathArrays.buildArray(field, 6, 6);
        for (int i = 0; i < 6; ++i) {
            fillColumn(type, i, orbit, hP, jacobian);
        }
        return jacobian;
    }

    private <T extends RealFieldElement<T>> void fillColumn(PositionAngle type, int i, FieldEquinoctialOrbit<T> orbit, T hP, T[][] jacobian) {
        T zero = hP.getField().getZero();
        // at constant energy (i.e. constant semi major axis), we have dV = -mu dP / (V * r^2)
        // we use this to compute a velocity step size from the position step size
        FieldVector3D<T> p = orbit.getPVCoordinates().getPosition();
        FieldVector3D<T> v = orbit.getPVCoordinates().getVelocity();
        T hV = hP.multiply(orbit.getMu()).divide(v.getNorm().multiply(p.getNormSq()));

        T h;
        FieldVector3D<T> dP = new FieldVector3D<T>(hP.getField().getZero(),hP.getField().getZero(),hP.getField().getZero());
        FieldVector3D<T> dV = new FieldVector3D<T>(hP.getField().getZero(),hP.getField().getZero(),hP.getField().getZero());
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

        FieldEquinoctialOrbit<T> oM4h = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(1, p, -4, dP), new FieldVector3D<T>(1, v, -4, dV)),
                                                     orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldEquinoctialOrbit<T> oM3h = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(1, p, -3, dP), new FieldVector3D<T>(1, v, -3, dV)),
                                                     orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldEquinoctialOrbit<T> oM2h = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(1, p, -2, dP), new FieldVector3D<T>(1, v, -2, dV)),
                                                     orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldEquinoctialOrbit<T> oM1h = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(1, p, -1, dP), new FieldVector3D<T>(1, v, -1, dV)),
                                                     orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldEquinoctialOrbit<T> oP1h = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(1, p, +1, dP), new FieldVector3D<T>(1, v, +1, dV)),
                                                     orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldEquinoctialOrbit<T> oP2h = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(1, p, +2, dP), new FieldVector3D<T>(1, v, +2, dV)),
                                                     orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldEquinoctialOrbit<T> oP3h = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(1, p, +3, dP), new FieldVector3D<T>(1, v, +3, dV)),
                                                     orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldEquinoctialOrbit<T> oP4h = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(1, p, +4, dP), new FieldVector3D<T>(1, v, +4, dV)),
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

    public <T extends RealFieldElement<T>>  void testInterpolation(Field<T> field) throws OrekitException {

        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

        final double ehMu  = 3.9860047e14;
        final double ae    = 6.378137e6;
        final T c20   =zero.add( -1.08263e-3);
        final T c30   =zero.add(  2.54e-6);
        final T c40   =zero.add( 1.62e-6);
        final T c50   =zero.add(  2.3e-7);
        final T c60   =zero.add( -5.5e-7);

        date = date.shiftedBy(584.);
        final FieldVector3D<T> position = new FieldVector3D<T>(zero.add(3220103.), zero.add(69623.), zero.add(6449822.));
        final FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(6414.7), zero.add(-2006.), zero.add(-3180.));
        final FieldEquinoctialOrbit<T> initialOrbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position, velocity),
                                                                   FramesFactory.getEME2000(), date, ehMu);

        FieldEcksteinHechlerPropagator<T> propagator =
                new FieldEcksteinHechlerPropagator<T>(initialOrbit, ae, ehMu, c20, c30, c40, c50, c60);

        // set up a 5 points sample
        List<FieldOrbit<T>> sample = new ArrayList<FieldOrbit<T>>();
        for (double dt = 0; dt < 300.0; dt += 60.0) {
            sample.add(propagator.propagate(date.shiftedBy(dt)).getOrbit());
        }

        // well inside the sample, interpolation should be much better than Keplerian shift
        double maxShiftError = 0;
        double maxInterpolationError = 0;
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
        Assert.assertTrue(maxInterpolationError < 1.3);

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
