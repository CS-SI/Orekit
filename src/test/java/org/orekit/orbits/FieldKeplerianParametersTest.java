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
import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
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
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;


public class FieldKeplerianParametersTest {

     // Computation date


     // Body mu
    public double mu;

    @Test 
    public void doKepToKepTest() throws OrekitException {
          testKeplerianToKeplerian(Decimal64Field.getInstance());
    }
    
    @Test 
    public void doKepToCartTest() throws OrekitException {
          testKeplerianToCartesian(Decimal64Field.getInstance());
    }
    
    @Test 
    public void doKepToEquinTest() throws OrekitException {
          testKeplerianToEquinoctial(Decimal64Field.getInstance());
    }
    
    @Test 
    public void doAnomalyTest() throws OrekitException {
          testAnomaly(Decimal64Field.getInstance());
    }
    
    @Test 
    public void doPositionVelocityNormsTest() throws OrekitException {
        testPositionVelocityNorms(Decimal64Field.getInstance());
    }
    
    @Test 
    public void doGeometryTest() throws OrekitException {
        testGeometry(Decimal64Field.getInstance());
    }
    
    @Test 
    public void doSymmetryTest() throws OrekitException {
        testSymmetry(Decimal64Field.getInstance());
    }
    
    @Test (expected=IllegalArgumentException.class)
    public void doNonInertialFrameTest() throws OrekitException {
        testNonInertialFrame(Decimal64Field.getInstance());
    }
    
    @Test 
    public void doPeriodTest() throws OrekitException {
        testPeriod(Decimal64Field.getInstance());
    }
    
    @Test 
    public void doHyperbolaTest() throws OrekitException {
        testHyperbola(Decimal64Field.getInstance());
    }
    
    @Test 
    public void doVeryLargeEccentricityTest() throws OrekitException {
        testVeryLargeEccentricity(Decimal64Field.getInstance());
    }
    
    @Test 
    public void doKeplerEquationTest() throws OrekitException {
        testKeplerEquation(Decimal64Field.getInstance());
    }
    
    @Test 
    public void doNumericalIssueTest() throws OrekitException {
        testNumericalIssue25(Decimal64Field.getInstance());
    }
    
    @Test 
    public void doJacobianReferenceEllipseTest() throws OrekitException {
        testJacobianReferenceEllipse(Decimal64Field.getInstance());
    }
    
    @Test 
    public void doJacobianFinitedDiffTest() throws OrekitException {
        testJacobianFinitedifferencesEllipse(Decimal64Field.getInstance());
    }
    
    @Test 
    public void doJacobianReferenceHyperbolaTest() throws OrekitException {
        testJacobianReferenceHyperbola(Decimal64Field.getInstance());
    }
    
    @Test 
    public void doJacobianFinitDiffHyperbolaTest() throws OrekitException {
        testJacobianFinitedifferencesHyperbola(Decimal64Field.getInstance());
    }
    
    @Test 
    public void doTest() throws OrekitException {
        testKeplerianDerivatives(Decimal64Field.getInstance());
    }

    @Test(expected=IllegalArgumentException.class)
    public void doOutOfRangeVTest() throws OrekitException {
        testOutOfRangeV(Decimal64Field.getInstance());
    }
    
    @Test
    public void doInterpolationTest() throws OrekitException {
        testInterpolation(Decimal64Field.getInstance());
    }
    @Test
    public void doPerfectlyEquatorialConversionTest() throws OrekitException {
        testPerfectlyEquatorialConversion(Decimal64Field.getInstance());
    }
    
    public <T extends RealFieldElement<T>> void testKeplerianToKeplerian(final Field<T> field) {

        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        T a=        field.getZero().add(24464560.0);

        T e=        field.getZero().add(0.7311);
        T i=        field.getZero().add(0.122138);
        T pa=       field.getZero().add(3.10686);
        T raan=     field.getZero().add(1.00681);
        T anomaly=  field.getZero().add(0.048363);
        //

        // elliptic orbit
        FieldKeplerianOrbit<T> kep =
            new FieldKeplerianOrbit<T>(a,e,i,pa,raan,anomaly, PositionAngle.MEAN,
                                    FramesFactory.getEME2000(), date, mu);

        FieldVector3D<T> pos = kep.getPVCoordinates().getPosition();

        FieldVector3D<T> vit = kep.getPVCoordinates().getVelocity();

        FieldKeplerianOrbit<T> param = new FieldKeplerianOrbit<T>(new FieldPVCoordinates<T>(pos,vit),
                                                  FramesFactory.getEME2000(), date, mu);

        Assert.assertEquals(param.getA().getReal(), kep.getA().getReal(), kep.getA().abs().multiply(Utils.epsilonTest).getReal());
        Assert.assertEquals(param.getE().getReal(), kep.getE().getReal(), kep.getE().abs().multiply(Utils.epsilonTest).getReal());
        Assert.assertEquals(FieldKeplerianOrbit.normalizeAngle(param.getI(), kep.getI()).getReal(), kep.getI().getReal(), kep.getI().abs().multiply(Utils.epsilonTest).getReal());
        Assert.assertEquals(FieldKeplerianOrbit.normalizeAngle(param.getPerigeeArgument(), kep.getPerigeeArgument()).getReal(), kep.getPerigeeArgument().getReal(), kep.getPerigeeArgument().abs().multiply(Utils.epsilonTest).getReal());
        Assert.assertEquals(FieldKeplerianOrbit.normalizeAngle(param.getRightAscensionOfAscendingNode(), kep.getRightAscensionOfAscendingNode()).getReal(), kep.getRightAscensionOfAscendingNode().getReal(), kep.getRightAscensionOfAscendingNode().abs().multiply(Utils.epsilonTest).getReal());
        Assert.assertEquals(FieldKeplerianOrbit.normalizeAngle(param.getMeanAnomaly(), kep.getMeanAnomaly()).getReal(), kep.getMeanAnomaly().getReal(), kep.getMeanAnomaly().abs().multiply(Utils.epsilonTest).getReal());



        // circular orbit
        T aC=        field.getZero().add(24464560.0);

        T eC=        field.getZero().add(0.0);
        T iC=        field.getZero().add(0.122138);
        T paC=       field.getZero().add(3.10686);
        T raanC=     field.getZero().add(1.00681);
        T anomalyC=  field.getZero().add(0.048363);


        FieldKeplerianOrbit<T> kepCir =
            new FieldKeplerianOrbit<T>(aC,eC,iC,paC,raanC,anomalyC, PositionAngle.MEAN,
                                    FramesFactory.getEME2000(), date, mu);

        FieldVector3D<T> posCir = kepCir.getPVCoordinates().getPosition();
        FieldVector3D<T> vitCir = kepCir.getPVCoordinates().getVelocity();

        FieldKeplerianOrbit<T> paramCir = new FieldKeplerianOrbit<T>(new FieldPVCoordinates<T>(posCir,vitCir),
                                                     FramesFactory.getEME2000(), date, mu);
        Assert.assertEquals(paramCir.getA().getReal(), kepCir.getA().getReal(), kep.getA().abs().multiply(Utils.epsilonTest).getReal());
        Assert.assertEquals(paramCir.getE().getReal(), kepCir.getE().getReal(), kep.getE().abs().multiply(Utils.epsilonTest).getReal());
        Assert.assertEquals(FieldKeplerianOrbit.normalizeAngle(paramCir.getI(), kepCir.getI()).getReal(), kepCir.getI().getReal(), kep.getI().abs().multiply(Utils.epsilonTest).getReal());
        Assert.assertEquals(FieldKeplerianOrbit.normalizeAngle(paramCir.getLM(), kepCir.getLM()).getReal(), kepCir.getLM().getReal(), kep.getLM().abs().multiply(Utils.epsilonTest).getReal());
        Assert.assertEquals(FieldKeplerianOrbit.normalizeAngle(paramCir.getLE(), kepCir.getLE()).getReal(), kepCir.getLE().getReal(), kep.getLM().abs().multiply(Utils.epsilonTest).getReal());
        Assert.assertEquals(FieldKeplerianOrbit.normalizeAngle(paramCir.getLv(), kepCir.getLv()).getReal(), kepCir.getLv().getReal(), kep.getLv().abs().multiply(Utils.epsilonTest).getReal());

        // hyperbolic orbit
        T aH=        field.getZero().add(-24464560.0);

        T eH=        field.getZero().add(1.7311);
        T iH=        field.getZero().add(0.122138);
        T paH=       field.getZero().add(3.10686);
        T raanH=     field.getZero().add(1.00681);
        T anomalyH=  field.getZero().add(0.048363);



        FieldKeplerianOrbit<T> kepHyp =
            new FieldKeplerianOrbit<T>(aH, eH, iH, paH, raanH,
                                    anomalyH, PositionAngle.MEAN,
                                    FramesFactory.getEME2000(), date, mu);

        FieldVector3D<T> posHyp = kepHyp.getPVCoordinates().getPosition();

        FieldVector3D<T> vitHyp = kepHyp.getPVCoordinates().getVelocity();

        FieldKeplerianOrbit<T> paramHyp = new FieldKeplerianOrbit<T>(new FieldPVCoordinates<T>(posHyp,vitHyp),
                                                  FramesFactory.getEME2000(), date, mu);

        Assert.assertEquals(paramHyp.getA().getReal(), kepHyp.getA().getReal(), kepHyp.getA().abs().multiply(Utils.epsilonTest).getReal());
        Assert.assertEquals(paramHyp.getE().getReal(), kepHyp.getE().getReal(), kepHyp.getE().abs().multiply(Utils.epsilonTest).getReal());
        Assert.assertEquals(FieldKeplerianOrbit.normalizeAngle(paramHyp.getI(), kepHyp.getI()).getReal(), kepHyp.getI().getReal(), kepHyp.getI().abs().multiply(Utils.epsilonTest).getReal());
        Assert.assertEquals(FieldKeplerianOrbit.normalizeAngle(paramHyp.getPerigeeArgument(), kepHyp.getPerigeeArgument()).getReal(), kepHyp.getPerigeeArgument().getReal(), kepHyp.getPerigeeArgument().abs().multiply(Utils.epsilonTest).getReal());
        Assert.assertEquals(FieldKeplerianOrbit.normalizeAngle(paramHyp.getRightAscensionOfAscendingNode(), kepHyp.getRightAscensionOfAscendingNode()).getReal(), kepHyp.getRightAscensionOfAscendingNode().getReal(), kepHyp.getRightAscensionOfAscendingNode().abs().multiply(Utils.epsilonTest).getReal());
        Assert.assertEquals(FieldKeplerianOrbit.normalizeAngle(paramHyp.getMeanAnomaly(), kepHyp.getMeanAnomaly()).getReal(), kepHyp.getMeanAnomaly().getReal(), kepHyp.getMeanAnomaly().abs().multiply(Utils.epsilonTest).getReal());
    }

    public <T extends RealFieldElement<T>> void testKeplerianToCartesian(final Field<T> field) {

        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

        T a=        field.getZero().add(24464560.0);
        T e=        field.getZero().add(0.7311);
        T i=        field.getZero().add(0.122138);
        T pa=       field.getZero().add(3.10686);
        T raan=     field.getZero().add(1.00681);
        T anomaly=  field.getZero().add(0.048363);

        // elliptic orbit
        FieldKeplerianOrbit<T> kep =
            new FieldKeplerianOrbit<T>(a,e,i,pa,raan,anomaly, PositionAngle.MEAN,
                                    FramesFactory.getEME2000(), date, mu);

        FieldVector3D<T> pos = kep.getPVCoordinates().getPosition();
        FieldVector3D<T> vit = kep.getPVCoordinates().getVelocity();
        Assert.assertEquals(-0.107622532467967e+07, pos.getX().getReal(), Utils.epsilonTest * FastMath.abs(pos.getX().getReal()));
        Assert.assertEquals(-0.676589636432773e+07, pos.getY().getReal(), Utils.epsilonTest * FastMath.abs(pos.getY().getReal()));
        Assert.assertEquals(-0.332308783350379e+06, pos.getZ().getReal(), Utils.epsilonTest * FastMath.abs(pos.getZ().getReal()));

        Assert.assertEquals( 0.935685775154103e+04, vit.getX().getReal(), Utils.epsilonTest * FastMath.abs(vit.getX().getReal()));
        Assert.assertEquals(-0.331234775037644e+04, vit.getY().getReal(), Utils.epsilonTest * FastMath.abs(vit.getY().getReal()));
        Assert.assertEquals(-0.118801577532701e+04, vit.getZ().getReal(), Utils.epsilonTest * FastMath.abs(vit.getZ().getReal()));
    }

    public <T extends RealFieldElement<T>> void testKeplerianToEquinoctial(final Field<T> field) {
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        T a=        field.getZero().add(24464560.0);

        T e=        field.getZero().add(0.7311);
        T i=        field.getZero().add(0.122138);
        T pa=       field.getZero().add(3.10686);
        T raan=     field.getZero().add(1.00681);
        T anomaly=  field.getZero().add(0.048363);

        // elliptic orbit
        FieldKeplerianOrbit<T> kep =
            new FieldKeplerianOrbit<T>(a,e,i,pa,raan,anomaly, PositionAngle.MEAN,
                                    FramesFactory.getEME2000(), date, mu);

        Assert.assertEquals(24464560.0, kep.getA().getReal(), Utils.epsilonTest * kep.getA().getReal());
        Assert.assertEquals(-0.412036802887626, kep.getEquinoctialEx().getReal(), Utils.epsilonE * FastMath.abs(kep.getE().getReal()));
        Assert.assertEquals(-0.603931190671706, kep.getEquinoctialEy().getReal(), Utils.epsilonE * FastMath.abs(kep.getE().getReal()));
        Assert.assertEquals(MathUtils.normalizeAngle(2*FastMath.asin(FastMath.sqrt((FastMath.pow(0.652494417368829e-01,2)+FastMath.pow(0.103158450084864,2))/4.)),kep.getI().getReal()), kep.getI().getReal(), Utils.epsilonAngle * FastMath.abs(kep.getI().getReal()));
        Assert.assertEquals(MathUtils.normalizeAngle(0.416203300000000e+01,kep.getLM().getReal()), kep.getLM().getReal(),Utils.epsilonAngle * FastMath.abs(kep.getLM().getReal()));

    }

    public <T extends RealFieldElement<T>> void testAnomaly(final Field<T> field) {
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        FieldVector3D<T> position = new FieldVector3D<T>(field.getZero().add(7.0e6), field.getZero().add(1.0e6), field.getZero().add(4.0e6));
        FieldVector3D<T> velocity = new FieldVector3D<T>(field.getZero().add(-500.0),field.getZero().add(8000.0),field.getZero().add(1000.0));
        FieldKeplerianOrbit<T> p = new FieldKeplerianOrbit<T>(new FieldPVCoordinates<T>(position, velocity),
                                              FramesFactory.getEME2000(), date, mu);

        // elliptic orbit
        T e = p.getE();
        T eRatio = (e.multiply(-1).add(1)).divide(e.add(1)).sqrt();

        T v = field.getZero().add(1.1);
        // formulations for elliptic case
        T E = v.divide(2).tan().multiply(eRatio).atan().multiply(2);
        T M = E.sin().multiply(e).multiply(-1).add(E);

        p = new FieldKeplerianOrbit<T>(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), v , PositionAngle.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());

        Assert.assertEquals(p.getTrueAnomaly().getReal(), v.getReal(), Utils.epsilonAngle * FastMath.abs(v.getReal()));
        Assert.assertEquals(p.getEccentricAnomaly().getReal(), E.getReal(), Utils.epsilonAngle * FastMath.abs(E.getReal()));
        Assert.assertEquals(p.getMeanAnomaly().getReal(), M.getReal(), Utils.epsilonAngle * FastMath.abs(M.getReal()));

        p = new FieldKeplerianOrbit<T>(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), field.getZero() , PositionAngle.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());

        p = new FieldKeplerianOrbit<T>(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), E , PositionAngle.ECCENTRIC,
                                    p.getFrame(), p.getDate(), p.getMu());

        Assert.assertEquals(p.getTrueAnomaly().getReal(), v.getReal(), Utils.epsilonAngle * FastMath.abs(v.getReal()));
        Assert.assertEquals(p.getEccentricAnomaly().getReal(), E.getReal(), Utils.epsilonAngle * FastMath.abs(E.getReal()));
        Assert.assertEquals(p.getMeanAnomaly().getReal(), M.getReal(), Utils.epsilonAngle * FastMath.abs(M.getReal()));

        p = new FieldKeplerianOrbit<T>(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), field.getZero() , PositionAngle.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());

        p = new FieldKeplerianOrbit<T>(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), M, PositionAngle.MEAN,
                                    p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getTrueAnomaly().getReal(), v.getReal(), Utils.epsilonAngle * FastMath.abs(v.getReal()));
        Assert.assertEquals(p.getEccentricAnomaly().getReal(), E.getReal(), Utils.epsilonAngle * FastMath.abs(E.getReal()));
        Assert.assertEquals(p.getMeanAnomaly().getReal(), M.getReal(), Utils.epsilonAngle * FastMath.abs(M.getReal()));

        // circular orbit
        p = new FieldKeplerianOrbit<T>(p.getA(),field.getZero(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), p.getLv() , PositionAngle.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());

        E = v;
        M = E;

        p = new FieldKeplerianOrbit<T>(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), v , PositionAngle.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getTrueAnomaly().getReal(), v.getReal(), Utils.epsilonAngle * FastMath.abs(v.getReal()));
        Assert.assertEquals(p.getEccentricAnomaly().getReal(), E.getReal(), Utils.epsilonAngle * FastMath.abs(E.getReal()));
        Assert.assertEquals(p.getMeanAnomaly().getReal(), M.getReal(), Utils.epsilonAngle * FastMath.abs(M.getReal()));

        p = new FieldKeplerianOrbit<T>(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), field.getZero() , PositionAngle.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());

        p = new FieldKeplerianOrbit<T>(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), E , PositionAngle.ECCENTRIC, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getTrueAnomaly().getReal(), v.getReal(), Utils.epsilonAngle * FastMath.abs(v.getReal()));
        Assert.assertEquals(p.getEccentricAnomaly().getReal(), E.getReal(), Utils.epsilonAngle * FastMath.abs(E.getReal()));
        Assert.assertEquals(p.getMeanAnomaly().getReal(), M.getReal(), Utils.epsilonAngle * FastMath.abs(M.getReal()));

        p = new FieldKeplerianOrbit<T>(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), field.getZero() , PositionAngle.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());

        p = new FieldKeplerianOrbit<T>(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), M, PositionAngle.MEAN,
                                    p.getFrame(), p.getDate(), p.getMu());

        Assert.assertEquals(p.getTrueAnomaly().getReal(), v.getReal(), Utils.epsilonAngle * FastMath.abs(v.getReal()));
        Assert.assertEquals(p.getEccentricAnomaly().getReal(), E.getReal(), Utils.epsilonAngle * FastMath.abs(E.getReal()));
        Assert.assertEquals(p.getMeanAnomaly().getReal(), M.getReal(), Utils.epsilonAngle * FastMath.abs(M.getReal()));

    }

    public <T extends RealFieldElement<T>> void testPositionVelocityNorms(final Field<T> field) {
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        T aa=       field.getZero().add(24464560.0);
        T ee=       field.getZero().add(0.7311);
        T i=        field.getZero().add(2.1);
        T pa=       field.getZero().add(3.10686);
        T raan=     field.getZero().add(1.00681);
        T anomaly=  field.getZero().add(0.67);

        // elliptic orbit
        FieldKeplerianOrbit<T> p =
            new FieldKeplerianOrbit<T>(aa,ee,i,pa,raan,anomaly, PositionAngle.MEAN,
                                    FramesFactory.getEME2000(), date, mu);



        T e       = p.getE();
        T v       = p.getTrueAnomaly();
        T ksi     = v.cos().multiply(e).add(1);
        T nu      = v.sin().multiply(e);
        T epsilon = e.multiply(-1).add(1).multiply(e.add(1)).sqrt();

        T a  = p.getA();
        T na = a.pow(-1).multiply(mu).sqrt();

        // validation of: r = a .(1 - e2) / (1 + e.cos(v))
        Assert.assertEquals(a.getReal() * epsilon.getReal() * epsilon.getReal() / ksi.getReal(),
                     p.getPVCoordinates().getPosition().getNorm().getReal(),
                     Utils.epsilonTest * FastMath.abs(p.getPVCoordinates().getPosition().getNorm().getReal()));

        // validation of: V = sqrt(mu.(1+2e.cos(v)+e2)/a.(1-e2) )
        Assert.assertEquals(na.getReal() * FastMath.sqrt(ksi.getReal() * ksi.getReal() + nu.getReal() * nu.getReal()) / epsilon.getReal(),
                     p.getPVCoordinates().getVelocity().getNorm().getReal(),
                     Utils.epsilonTest * FastMath.abs(p.getPVCoordinates().getVelocity().getNorm().getReal()));


        //  circular and equatorial orbit
        FieldKeplerianOrbit<T> pCirEqua =
            new FieldKeplerianOrbit<T>(field.getZero().add(24464560.0),field.getZero().add(0.1e-10), field.getZero().add(0.1e-8), field.getZero().add(3.10686),field.getZero().add(1.00681),
                            field.getZero().add(0.67), PositionAngle.TRUE,
                                    FramesFactory.getEME2000(), date, mu);

        e       = pCirEqua.getE();
        v       = pCirEqua.getTrueAnomaly();
        ksi     = v.cos().multiply(e).add(1);
        nu      = v.sin().multiply(e);
        epsilon = e.multiply(-1).add(1).multiply(e.add(1)).sqrt();

        a  = pCirEqua.getA();
        na = a.pow(-1).multiply(mu).sqrt();

        // validation of: r = a .(1 - e2) / (1 + e.cos(v))
        Assert.assertEquals(a.getReal() * epsilon.getReal() * epsilon.getReal() / ksi.getReal(),
                     pCirEqua.getPVCoordinates().getPosition().getNorm().getReal(),
                     Utils.epsilonTest * FastMath.abs(pCirEqua.getPVCoordinates().getPosition().getNorm().getReal()));

        // validation of: V = sqrt(mu.(1+2e.cos(v)+e2)/a.(1-e2) )
        Assert.assertEquals(na.getReal() * FastMath.sqrt(ksi.getReal() * ksi.getReal() + nu.getReal() * nu.getReal()) / epsilon.getReal(),
                     pCirEqua.getPVCoordinates().getVelocity().getNorm().getReal(),
                     Utils.epsilonTest * FastMath.abs(pCirEqua.getPVCoordinates().getVelocity().getNorm().getReal()));
    }

    public <T extends RealFieldElement<T>> void testGeometry(final Field<T> field) {
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        // elliptic and non equatorial orbit
        FieldKeplerianOrbit<T> p =
            new FieldKeplerianOrbit<T>(field.getZero().add(24464560.0), field.getZero().add(0.7311), field.getZero().add(2.1), field.getZero().add(3.10686), field.getZero().add(1.00681),
                            field.getZero().add(0.67), PositionAngle.TRUE,
                                    FramesFactory.getEME2000(), date, mu);

        FieldVector3D<T> position = p.getPVCoordinates().getPosition();
        FieldVector3D<T> velocity = p.getPVCoordinates().getVelocity();
        FieldVector3D<T> momentum = p.getPVCoordinates().getMomentum().normalize();

        T apogeeRadius  = p.getA().multiply(p.getE().add(1));
        T perigeeRadius = p.getA().multiply(p.getE().multiply(-1).add(1));

        for (T lv = field.getZero(); lv.getReal() <= field.getZero().add(2 * FastMath.PI).getReal(); lv =lv.add(2 * FastMath.PI/100.)) {
            p = new FieldKeplerianOrbit<T>(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                        p.getRightAscensionOfAscendingNode(), lv , PositionAngle.TRUE,
                                        p.getFrame(), p.getDate(), p.getMu());
            position = p.getPVCoordinates().getPosition();


            // test if the norm of the position is in the range [perigee radius, apogee radius]


            Assert.assertTrue((position.getNorm().getReal() - apogeeRadius.getReal())  <= (  apogeeRadius.getReal() * Utils.epsilonTest));
            Assert.assertTrue((position.getNorm().getReal() - perigeeRadius.getReal()) >= (- perigeeRadius.getReal() * Utils.epsilonTest));

            position = position.normalize();
            velocity = p.getPVCoordinates().getVelocity();
            velocity = velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and momemtum) are normalized here

            // test of orthogonality between position and momentum
            Assert.assertTrue(FastMath.abs(FieldVector3D.dotProduct(position, momentum).getReal()) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assert.assertTrue(FastMath.abs(FieldVector3D.dotProduct(velocity, momentum).getReal()) < Utils.epsilonTest);

        }

        // apsides
        p = new FieldKeplerianOrbit<T>(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), field.getZero(), PositionAngle.TRUE, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getPVCoordinates().getPosition().getNorm().getReal(), perigeeRadius.getReal(), perigeeRadius.getReal() * Utils.epsilonTest);

        p = new FieldKeplerianOrbit<T>(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), field.getZero().add(FastMath.PI) , PositionAngle.TRUE, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getPVCoordinates().getPosition().getNorm().getReal(), apogeeRadius.getReal(), apogeeRadius.getReal() * Utils.epsilonTest);

        // nodes
        // descending node
        p = new FieldKeplerianOrbit<T>(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), field.getZero().add(FastMath.PI).subtract(p.getPerigeeArgument()) , PositionAngle.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());

        Assert.assertTrue(FastMath.abs(p.getPVCoordinates().getPosition().getZ().getReal()) < p.getPVCoordinates().getPosition().getNorm().getReal() * Utils.epsilonTest);

        Assert.assertTrue(p.getPVCoordinates().getVelocity().getZ().getReal() < 0);

        // ascending node
        p = new FieldKeplerianOrbit<T>(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(),field.getZero().add(2.0 * FastMath.PI - p.getPerigeeArgument().getReal()) , PositionAngle.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());
        Assert.assertTrue(FastMath.abs(p.getPVCoordinates().getPosition().getZ().getReal()) < p.getPVCoordinates().getPosition().getNorm().getReal() * Utils.epsilonTest);
        Assert.assertTrue(p.getPVCoordinates().getVelocity().getZ().getReal() > 0);

        //  circular and equatorial orbit
        FieldKeplerianOrbit<T> pCirEqua =
            new FieldKeplerianOrbit<T>(field.getZero().add(24464560.0),
                                        field.getZero().add(0.1e-10),
                                        field.getZero().add(0.1e-8),
                                        field.getZero().add(3.10686),
                                        field.getZero().add(1.00681),
                                        field.getZero().add(0.67),
                                        PositionAngle.TRUE,
                                        FramesFactory.getEME2000(), date, mu);

        position = pCirEqua.getPVCoordinates().getPosition();
        velocity = pCirEqua.getPVCoordinates().getVelocity();
        momentum = FieldVector3D.crossProduct(position,velocity).normalize();

        apogeeRadius  = pCirEqua.getA().multiply(pCirEqua.getE().add(1));
        perigeeRadius = pCirEqua.getA().multiply(pCirEqua.getE().multiply(-1).add(1));

        // test if apogee equals perigee
        Assert.assertEquals(perigeeRadius.getReal(), apogeeRadius.getReal(),1.e+4 * Utils.epsilonTest * apogeeRadius.getReal());
//TESTED UNTIL HERE


       for (T lv = field.getZero(); lv.getReal() <= 2 * FastMath.PI; lv = lv.add(2*FastMath.PI/100.)) {

           pCirEqua = new FieldKeplerianOrbit<T>(pCirEqua.getA(),pCirEqua.getE(),pCirEqua.getI(), pCirEqua.getPerigeeArgument(),
                                               pCirEqua.getRightAscensionOfAscendingNode(), lv, PositionAngle.TRUE,
                                               pCirEqua.getFrame(), pCirEqua.getDate(), pCirEqua.getMu());
            position = pCirEqua.getPVCoordinates().getPosition();

            // test if the norm pf the position is in the range [perigee radius, apogee radius]
            // Warning: these tests are without absolute value by choice
            Assert.assertTrue((position.getNorm().getReal() - apogeeRadius.getReal())  <= (  apogeeRadius.getReal() * Utils.epsilonTest));
            Assert.assertTrue((position.getNorm().getReal() - perigeeRadius.getReal()) >= (- perigeeRadius.getReal() * Utils.epsilonTest));

            position = position.normalize();
            velocity = pCirEqua.getPVCoordinates().getVelocity();
            velocity = velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and momemtum) are normalized here

            // test of orthogonality between position and momentum
          Assert.assertTrue(FastMath.abs(FieldVector3D.dotProduct(position, momentum).getReal()) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assert.assertTrue(FastMath.abs(FieldVector3D.dotProduct(velocity, momentum).getReal()) < Utils.epsilonTest);
        }
    }

    public <T extends RealFieldElement<T>> void testSymmetry(final Field<T> field) {
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        // elliptic and non equatorial orbit
        FieldVector3D<T> position = new FieldVector3D<T>(field.getZero().add(-4947831.), field.getZero().add(-3765382.), field.getZero().add(-3708221.));
        FieldVector3D<T> velocity = new FieldVector3D<T>(field.getZero().add(-2079.), field.getZero().add(5291.), field.getZero().add(-7842.));

        FieldKeplerianOrbit<T> p = new FieldKeplerianOrbit<T>(new FieldPVCoordinates<T>(position, velocity),
                                              FramesFactory.getEME2000(), date, mu);
        FieldVector3D<T> positionOffset = p.getPVCoordinates().getPosition().subtract(position);
        FieldVector3D<T> velocityOffset = p.getPVCoordinates().getVelocity().subtract(velocity);

       Assert.assertTrue(positionOffset.getNorm().getReal() < Utils.epsilonTest);
       Assert.assertTrue(velocityOffset.getNorm().getReal() < Utils.epsilonTest);

        // circular and equatorial orbit
        position = new FieldVector3D<T>(field.getZero().add(1742382.), field.getZero().add(-2.440243e7), field.getZero().add(-0.014517));
        velocity = new FieldVector3D<T>(field.getZero().add(4026.2), field.getZero().add(287.479), field.getZero().add(-3.e-6));


        p = new FieldKeplerianOrbit<T>(new FieldPVCoordinates<T>(position, velocity),
                               FramesFactory.getEME2000(), date, mu);
        positionOffset = p.getPVCoordinates().getPosition().subtract(position);
        velocityOffset = p.getPVCoordinates().getVelocity().subtract(velocity);

       Assert.assertTrue(positionOffset.getNorm().getReal() < Utils.epsilonTest);
       Assert.assertTrue(velocityOffset.getNorm().getReal() < Utils.epsilonTest);

    }



    public <T extends RealFieldElement<T>> void testNonInertialFrame(final Field<T> field) throws IllegalArgumentException {
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        FieldVector3D<T> position = new FieldVector3D<T>(field.getZero().add(-4947831.), field.getZero().add(-3765382.), field.getZero().add(-3708221.));
        FieldVector3D<T> velocity = new FieldVector3D<T>(field.getZero().add(-2079.), field.getZero().add(5291.), field.getZero().add(-7842.));
        FieldPVCoordinates<T> pvCoordinates = new FieldPVCoordinates<T>( position, velocity);
        new FieldKeplerianOrbit<T>(pvCoordinates,
                           new Frame(FramesFactory.getEME2000(), Transform.IDENTITY, "non-inertial", false),
                           date, mu);
    }

    public <T extends RealFieldElement<T>> void testPeriod(final Field<T> field) {
        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<T>(field.getZero().add(7654321.0), field.getZero().add(0.1), field.getZero().add(0.2), field.getZero(), field.getZero(), field.getZero(),
                        PositionAngle.TRUE,
                        FramesFactory.getEME2000(), new FieldAbsoluteDate<T>(field),
                        mu);
        Assert.assertEquals(6664.5521723383589487, orbit.getKeplerianPeriod().getReal(), 1.0e-12);
        Assert.assertEquals(0.00094277682051291315229, orbit.getKeplerianMeanMotion().getReal(), 1.0e-16);
    }



    public <T extends RealFieldElement<T>> void testHyperbola(final Field<T> field) {
        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<T>(field.getZero().add(-10000000.0),field.getZero().add(2.5), field.getZero().add(0.3),field.getZero(), field.getZero(),field.getZero(),
                        PositionAngle.TRUE,
                        FramesFactory.getEME2000(), new FieldAbsoluteDate<T>(field),
                        mu);
        FieldVector3D<T> perigeeP  = orbit.getPVCoordinates().getPosition();
        FieldVector3D<T> u = perigeeP.normalize();
        FieldVector3D<T> focus1 = new FieldVector3D<T>(field.getZero(),field.getZero(),field.getZero());
        FieldVector3D<T> focus2 = new FieldVector3D<T>(orbit.getA().multiply(-2).multiply(orbit.getE()), u);
        for (T dt = field.getZero().add(-5000); dt.getReal() < 5000; dt = dt.add(60)) {
            FieldPVCoordinates<T> pv = orbit.shiftedBy(dt).getPVCoordinates();
            T d1 = FieldVector3D.distance(pv.getPosition(), focus1);
            T d2 = FieldVector3D.distance(pv.getPosition(), focus2);
            Assert.assertEquals(orbit.getA().multiply(-2).getReal(), d1.subtract(d2).abs().getReal(), 1.0e-6);
            FieldKeplerianOrbit<T> rebuilt =
                            new FieldKeplerianOrbit<T>(pv, orbit.getFrame(), orbit.getDate().shiftedBy(dt), mu);
            Assert.assertEquals(-10000000.0, rebuilt.getA().getReal(), 1.0e-6);
            Assert.assertEquals(2.5, rebuilt.getE().getReal(), 1.0e-13);
        }
    }


    public <T extends RealFieldElement<T>> void testVeryLargeEccentricity(final Field<T> field) {
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        final Frame eme2000 = FramesFactory.getEME2000();
        final double meanAnomaly = 1.;
        final FieldKeplerianOrbit<T> orb0 = new FieldKeplerianOrbit<T>(field.getZero().add(42600e3), field.getZero().add(0.9), field.getZero().add(0.00001), field.getZero().add(0), field.getZero().add(0),
                        field.getZero().add(FastMath.toRadians(meanAnomaly)),
                        PositionAngle.MEAN, eme2000, date, mu);
        // big dV along Y
        final FieldVector3D<T> deltaV = new FieldVector3D<T>(field.getZero().add(0.0), field.getZero().add(110000.0), field.getZero());
        final FieldPVCoordinates<T> pv1 = new FieldPVCoordinates<T>(orb0.getPVCoordinates().getPosition(),
                        orb0.getPVCoordinates().getVelocity().add(deltaV));
        final FieldKeplerianOrbit<T> orb1 = new FieldKeplerianOrbit<T>(pv1, eme2000, date, mu);

        // Despite large eccentricity, the conversion of mean anomaly to hyperbolic eccentric anomaly
        // converges in less than 50 iterations (issue #114)
        final FieldPVCoordinates<T> pvTested    = orb1.shiftedBy(field.getZero()).getPVCoordinates();
        final FieldVector3D<T>      pTested     = pvTested.getPosition();
        final FieldVector3D<T>      vTested     = pvTested.getVelocity();


        final FieldPVCoordinates<T> pvReference = orb1.getPVCoordinates();
        final FieldVector3D<T>      pReference  = pvReference.getPosition();
        final FieldVector3D<T>      vReference  = pvReference.getVelocity();

        final double threshold = 1.e-15;
        Assert.assertEquals(0, pTested.subtract(pReference).getNorm().getReal(), pReference.getNorm().multiply(threshold).getReal());
        Assert.assertEquals(0, vTested.subtract(vReference).getNorm().getReal(), vReference.getNorm().multiply(threshold).getReal());

    }


    public <T extends RealFieldElement<T>> void testKeplerEquation(final Field<T> field) {
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        for (T M = field.getZero().add(-6 * FastMath.PI); M.getReal() < 6 * FastMath.PI; M = M.add(0.01)) {
            FieldKeplerianOrbit<T> pElliptic =
                            new FieldKeplerianOrbit<T>(field.getZero().add(24464560.0), field.getZero().add(0.7311), field.getZero().add(2.1), field.getZero().add(3.10686), field.getZero().add(1.00681),
                                            field.getZero().add(M), PositionAngle.MEAN,
                                            FramesFactory.getEME2000(), date, mu);
            T E = pElliptic.getEccentricAnomaly();
            T e = pElliptic.getE();
            Assert.assertEquals(M.getReal(), E.getReal() - e.getReal() * FastMath.sin(E.getReal()), 2.0e-14);
        }

        for (T M = field.getZero().add(-6 * FastMath.PI); M.getReal() < 6 * FastMath.PI; M = M.add(0.01)) {

            FieldKeplerianOrbit<T> pAlmostParabolic =
                            new FieldKeplerianOrbit<T>(field.getZero().add(24464560.0), field.getZero().add(0.9999), field.getZero().add(2.1), field.getZero().add(3.10686), field.getZero().add(1.00681),
                                            field.getZero().add(M), PositionAngle.MEAN,
                                            FramesFactory.getEME2000(), date, mu);

            T E = pAlmostParabolic.getEccentricAnomaly();
            T e = pAlmostParabolic.getE();
            Assert.assertEquals(M.getReal(), E.getReal() - e.getReal() * FastMath.sin(E.getReal()), 3.0e-13);
        }

    }

    public <T extends RealFieldElement<T>> void testOutOfRangeV(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        new FieldKeplerianOrbit<T>(zero.add(-7000434.460140012),
                                   zero.add(1.1999785407363386),
                                   zero.add(1.3962787004479158),
                                   zero.add(1.3962320168955138),
                                   zero.add(0.3490728321331678),
                                   zero.add(-2.55593407037698),
                    PositionAngle.TRUE, FramesFactory.getEME2000(),
                    new FieldAbsoluteDate<T>(field,"2000-01-01T12:00:00.391", TimeScalesFactory.getUTC()),
                    3.986004415E14);
    }

    public <T extends RealFieldElement<T>>void testNumericalIssue25(final Field<T> field) throws OrekitException {
        FieldVector3D<T> position = new FieldVector3D<T>(field.getZero().add(3782116.14107698), field.getZero().add(416663.11924914), field.getZero().add(5875541.62103057));
        FieldVector3D<T> velocity = new FieldVector3D<T>(field.getZero().add(-6349.7848910501), field.getZero().add(288.4061811651), field.getZero().add(4066.9366759691));
        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<T>(new FieldPVCoordinates<T>(position, velocity),
                        FramesFactory.getEME2000(),
                        new FieldAbsoluteDate<T>(field,"2004-01-01T23:00:00.000",
                                        TimeScalesFactory.getUTC()),
                        3.986004415E14);
        Assert.assertEquals(0.0, orbit.getE().getReal(), 2.0e-14);
    }


    public <T extends RealFieldElement<T>> void testPerfectlyEquatorial(final Field<T> field) throws OrekitException {

        FieldVector3D<T> position = new FieldVector3D<T>(field.getZero().add(6957904.3624652653594), field.getZero().add(766529.11411558074507),field.getZero());
        FieldVector3D<T> velocity = new FieldVector3D<T>(field.getZero().add(-7538.2817012412102845), field.getZero().add(342.38751001881413381),field.getZero());
        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<T>(new FieldPVCoordinates<T>(position, velocity),
                        FramesFactory.getEME2000(),
                        new FieldAbsoluteDate<T>(field,"2004-01-01T23:00:00.000",
                                        TimeScalesFactory.getUTC()),
                        3.986004415E14);
        Assert.assertEquals(0.0, orbit.getI().getReal(), 2.0e-14);
        Assert.assertEquals(0.0, orbit.getRightAscensionOfAscendingNode().getReal(), 2.0e-14);
    }

    public <T extends RealFieldElement<T>> void testJacobianReferenceEllipse(final Field<T>  field) throws OrekitException {

        FieldAbsoluteDate<T> dateTca = new FieldAbsoluteDate<T>(field,2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        FieldKeplerianOrbit<T> orbKep = new FieldKeplerianOrbit<T>(field.getZero().add(7000000.0),
                                                                   field.getZero().add(0.01),
                                                                   field.getZero().add(FastMath.toRadians(80.)),
                                                                   field.getZero().add(FastMath.toRadians(80.)),
                                                                   field.getZero().add(FastMath.toRadians(20.)),
                        field.getZero().add(FastMath.toRadians(40.)), PositionAngle.MEAN,
                        FramesFactory.getEME2000(), dateTca, mu);

        // the following reference values have been computed using the free software
        // version 6.2 of the MSLIB fortran library by the following program:
        //        program kep_jacobian
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
        //        type(tm_orb_kep)::kep
        //        real(pm_reel), dimension(6,6)::jacob
        //        real(pm_reel)::norme
        //
        //        kep%a=7000000_pm_reel
        //        kep%e=0.01_pm_reel
        //        kep%i=80_pm_reel*pm_deg_rad
        //        kep%pom=80_pm_reel*pm_deg_rad
        //        kep%gom=20_pm_reel*pm_deg_rad
        //        kep%M=40_pm_reel*pm_deg_rad
        //
        //        call mv_kep_car(mu,kep,pos_car,vit_car,code_retour)
        //        write(*,*)code_retour%valeur
        //        write(*,1000)pos_car,vit_car
        //
        //
        //        call mu_norme(pos_car,norme,code_retour)
        //        write(*,*)norme
        //
        //        call mv_car_kep (mu, pos_car, vit_car, kep, code_retour, jacob)
        //        write(*,*)code_retour%valeur
        //
        //        write(*,*)"kep = ", kep%a, kep%e, kep%i*pm_rad_deg,&
        //                            kep%pom*pm_rad_deg, kep%gom*pm_rad_deg, kep%M*pm_rad_deg
        //
        //        do i = 1,6
        //           write(*,*) " ",(jacob(i,j),j=1,6)
        //        end do
        //
        //        1000 format (6(f24.15,1x))
        //        end program kep_jacobian
        FieldVector3D<T> pRef = new FieldVector3D<T>(field.getZero().add(-3691555.569874833337963), field.getZero().add(-240330.253992714860942), field.getZero().add(5879700.285850423388183));
        FieldVector3D<T> vRef = new FieldVector3D<T>(field.getZero().add(-5936.229884450408463), field.getZero().add(-2871.067660163344044), field.getZero().add(-3786.209549192726627));

        double[][] jRef = {
                           { -1.0792090588217809,       -7.02594292049818631E-002,  1.7189029642216496,       -1459.4829009393857,       -705.88138246206040,       -930.87838644776593       },
                           { -1.31195762636625214E-007, -3.90087231593959271E-008,  4.65917592901869866E-008, -2.02467187867647177E-004, -7.89767994436215424E-005, -2.81639203329454407E-005 },
                           {  4.18334478744371316E-008, -1.14936453412947957E-007,  2.15670500707930151E-008, -2.26450325965329431E-005,  6.22167157217876380E-005, -1.16745469637130306E-005 },
                           {  3.52735168061691945E-006,  3.82555734454450974E-006,  1.34715077236557634E-005, -8.06586262922115264E-003, -6.13725651685311825E-003, -1.71765290503914092E-002 },
                           {  2.48948022169790885E-008, -6.83979069529389238E-008,  1.28344057971888544E-008,  3.86597661353874888E-005, -1.06216834498373629E-004,  1.99308724078785540E-005 },
                           { -3.41911705254704525E-006, -3.75913623359912437E-006, -1.34013845492518465E-005,  8.19851888816422458E-003,  6.16449264680494959E-003,  1.69495878276556648E-002 }
        };




        FieldPVCoordinates<T> pv = orbKep.getPVCoordinates();
        Assert.assertEquals(0, pv.getPosition().subtract(pRef).getNorm().getReal(), 1.0e-15 * pRef.getNorm().getReal());
        Assert.assertEquals(0, pv.getVelocity().subtract(vRef).getNorm().getReal(), 1.0e-16 * vRef.getNorm().getReal());

        T[][] jacobian = MathArrays.buildArray(field, 6 , 6);
        orbKep.getJacobianWrtCartesian(PositionAngle.MEAN, jacobian);

        for (int i = 0; i < jacobian.length; i++) {
            T[] row    = jacobian[i];
            double[] rowRef = jRef[i];
            for (int j = 0; j < row.length; j++) {
                Assert.assertEquals(0, (row[j].getReal() - rowRef[j]) / rowRef[j], 2.0e-12);
            }
        }

    }


    public <T extends RealFieldElement<T>>void testJacobianFinitedifferencesEllipse(final Field<T> field) throws OrekitException {

        FieldAbsoluteDate<T> dateTca = new FieldAbsoluteDate<T>(field,2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        FieldKeplerianOrbit<T> orbKep = new FieldKeplerianOrbit<T>(field.getZero().add(7000000.0), field.getZero().add(0.01), field.getZero().add(FastMath.toRadians(80.)), field.getZero().add(FastMath.toRadians(80.)), field.getZero().add(FastMath.toRadians(20.)),
                        field.getZero().add(FastMath.toRadians(40.)), PositionAngle.MEAN,
                        FramesFactory.getEME2000(), dateTca, mu);

        for (PositionAngle type : PositionAngle.values()) {
            T hP = field.getZero().add(2.0);
            T[][] finiteDiffJacobian = finiteDifferencesJacobian(type, orbKep, hP, field);
            T[][] jacobian = MathArrays.buildArray(field, 6, 6);
            orbKep.getJacobianWrtCartesian(type, jacobian);



            for (int i = 0; i < jacobian.length; i++) {
                T[] row    = jacobian[i];
                T[] rowRef = finiteDiffJacobian[i];
                for (int j = 0; j < row.length; j++) {
                    Assert.assertEquals(0, (row[j].getReal() - rowRef[j].getReal()) / rowRef[j].getReal(), 2.0e-7);
                }
            }

            T[][] invJacobian = MathArrays.buildArray(field, 6, 6);
            orbKep.getJacobianWrtParameters(type, invJacobian);
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


    public <T extends RealFieldElement<T>> void testJacobianReferenceHyperbola(final Field<T> field) throws OrekitException {

        FieldAbsoluteDate<T> dateTca = new FieldAbsoluteDate<T>(field,2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        FieldKeplerianOrbit<T> orbKep = new FieldKeplerianOrbit<T>(field.getZero().add(-7000000.0), field.getZero().add(1.2), field.getZero().add(FastMath.toRadians(80.)), field.getZero().add(FastMath.toRadians(80.)), field.getZero().add(FastMath.toRadians(20.)),
                        field.getZero().add(FastMath.toRadians(40.)), PositionAngle.MEAN,
                        FramesFactory.getEME2000(), dateTca, mu);

        // the following reference values have been computed using the free software
        // version 6.2 of the MSLIB fortran library by the following program:
        //        program kep_hyperb_jacobian
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
        //        type(tm_orb_kep)::kep
        //        real(pm_reel), dimension(6,6)::jacob
        //        real(pm_reel)::norme
        //
        //        kep%a=7000000_pm_reel
        //        kep%e=1.2_pm_reel
        //        kep%i=80_pm_reel*pm_deg_rad
        //        kep%pom=80_pm_reel*pm_deg_rad
        //        kep%gom=20_pm_reel*pm_deg_rad
        //        kep%M=40_pm_reel*pm_deg_rad
        //
        //        call mv_kep_car(mu,kep,pos_car,vit_car,code_retour)
        //        write(*,*)code_retour%valeur
        //        write(*,1000)pos_car,vit_car
        //
        //
        //        call mu_norme(pos_car,norme,code_retour)
        //        write(*,*)norme
        //
        //        call mv_car_kep (mu, pos_car, vit_car, kep, code_retour, jacob)
        //        write(*,*)code_retour%valeur
        //
        //        write(*,*)"kep = ", kep%a, kep%e, kep%i*pm_rad_deg,&
        //                            kep%pom*pm_rad_deg, kep%gom*pm_rad_deg, kep%M*pm_rad_deg
        //
        //        ! convert the sign of da row since mslib uses a > 0 for all orbits
        //        ! whereas we use a < 0 for hyperbolic orbits
        //        write(*,*) " ",(-jacob(1,j),j=1,6)
        //        do i = 2,6
        //           write(*,*) " ",(jacob(i,j),j=1,6)
        //        end do
        //
        //        1000 format (6(f24.15,1x))
        //        end program kep_hyperb_jacobian

        FieldVector3D<T> pRef = new FieldVector3D<T>(field.getZero().add(-7654711.206549182534218), field.getZero().add(-3460171.872979687992483), field.getZero().add(-3592374.514463655184954));
        FieldVector3D<T> vRef = new FieldVector3D<T>(field.getZero().add(-7886.368091820805603), field.getZero().add(-4359.739012331759113),  field.getZero().add(  -7937.060044548694350));
        double[][] jRef = {
                           {  -0.98364725131848019,      -0.44463970750901238,      -0.46162803814668391,       -1938.9443476028839,       -1071.8864775981751,       -1951.4074832397598      },
                           {  -1.10548813242982574E-007, -2.52906747183730431E-008,  7.96500937398593591E-008, -9.70479823470940108E-006, -2.93209076428001017E-005, -1.37434463892791042E-004 },
                           {   8.55737680891616672E-008, -2.35111995522618220E-007,  4.41171797903162743E-008, -8.05235180390949802E-005,  2.21236547547460423E-004, -4.15135455876865407E-005 },
                           {  -1.52641427784095578E-007,  1.10250447958827901E-008,  1.21265251605359894E-007,  7.63347077200903542E-005, -3.54738331412232378E-005, -2.31400737283033359E-004 },
                           {   7.86711766048035274E-008, -2.16147281283624453E-007,  4.05585791077187359E-008, -3.56071805267582894E-005,  9.78299244677127374E-005, -1.83571253224293247E-005 },
                           {  -2.41488884881911384E-007, -1.00119615610276537E-007, -6.51494225096757969E-008, -2.43295075073248163E-004, -1.43273725071890463E-004, -2.91625510452094873E-004 }
        };

        FieldPVCoordinates<T> pv = orbKep.getPVCoordinates();
        Assert.assertEquals(0, pv.getPosition().subtract(pRef).getNorm().getReal() / pRef.getNorm().getReal(), 1.0e-16);
        // Assert.assertEquals(0, pv.getPosition().subtract(pRef).getNorm() / pRef.getNorm(), 2.0e-15);
        Assert.assertEquals(0, pv.getVelocity().subtract(vRef).getNorm().getReal() / vRef.getNorm().getReal(), 3.0e-16);

        T[][] jacobian = MathArrays.buildArray(field, 6, 6);
        orbKep.getJacobianWrtCartesian(PositionAngle.MEAN, jacobian);

        for (int i = 0; i < jacobian.length; i++) {

            T[] row    = jacobian[i];
            double[] rowRef = jRef[i];
            for (int j = 0; j < row.length; j++) {


                Assert.assertEquals(0, (row[j].getReal() - rowRef[j]) / rowRef[j], 1.0e-14);
            }

        }

    }

    public <T extends RealFieldElement<T>>void testJacobianFinitedifferencesHyperbola(final Field<T> field) throws OrekitException {

        FieldAbsoluteDate<T> dateTca = new FieldAbsoluteDate<T>(field,2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        FieldKeplerianOrbit<T> orbKep = new FieldKeplerianOrbit<T>(field.getZero().add(-7000000.0), field.getZero().add(1.2), field.getZero().add(FastMath.toRadians(80.)), field.getZero().add(FastMath.toRadians(80.)), field.getZero().add(FastMath.toRadians(20.)),
                        field.getZero().add(FastMath.toRadians(40.)), PositionAngle.MEAN,
                        FramesFactory.getEME2000(), dateTca, mu);

        for (PositionAngle type : PositionAngle.values()) {
            T hP =field.getZero().add(2.0);
            T[][] finiteDiffJacobian = finiteDifferencesJacobian(type, orbKep, hP, field);
            T[][] jacobian = MathArrays.buildArray(field, 6, 6);
            orbKep.getJacobianWrtCartesian(type, jacobian);
            for (int i = 0; i < jacobian.length; i++) {
                T[] row    = jacobian[i];
                T[] rowRef = finiteDiffJacobian[i];

                for (int j = 0; j < row.length; j++) {
                    Assert.assertEquals(0, (row[j].getReal() - rowRef[j].getReal()) / rowRef[j].getReal(), 3.0e-8);
                }
            }

            T[][] invJacobian = MathArrays.buildArray(field, 6, 6);
            orbKep.getJacobianWrtParameters(type, invJacobian);
            MatrixUtils.createFieldMatrix(jacobian).
            multiply(MatrixUtils.createFieldMatrix(invJacobian)).
            walkInRowOrder(new FieldMatrixPreservingVisitor<T>() {
                public void start(int rows, int columns,
                                  int startRow, int endRow, int startColumn, int endColumn) {
                }

                public void visit(int row, int column, T value) {
                    Assert.assertEquals(row == column ? 1.0 : 0.0, value.getReal(), 2.0e-8);
                }

                public T end() {
                    return null;
                }
            });
        }

    }

    private <T extends RealFieldElement<T>> T[][] finiteDifferencesJacobian(PositionAngle type, FieldKeplerianOrbit<T> orbit, T hP, final Field<T> field)
                    throws OrekitException {
        T[][] jacobian = MathArrays.buildArray(field, 6, 6);
        for (int i = 0; i < 6; ++i) {
            fillColumn(type, i, orbit, hP, jacobian);
        }
        return jacobian;
    }

    private <T extends RealFieldElement<T>> void fillColumn(PositionAngle type, int i, FieldKeplerianOrbit<T> orbit, T hP, T[][] jacobian) {

        // at constant energy (i.e. constant semi major axis), we have dV = -mu dP / (V * r^2)
        // we use this to compute a velocity step size from the position step size
        FieldVector3D<T> p = orbit.getPVCoordinates().getPosition();
        FieldVector3D<T> v = orbit.getPVCoordinates().getVelocity();
        T hV = hP.multiply(orbit.getMu()).divide(v.getNorm().multiply(p.getNormSq()));
        T h;
        FieldVector3D<T> dP = new FieldVector3D<T>(p.getX().getField().getZero(),p.getX().getField().getZero(),p.getX().getField().getZero());
        FieldVector3D<T> dV = new FieldVector3D<T>(p.getX().getField().getZero(),p.getX().getField().getZero(),p.getX().getField().getZero());
        switch (i) {
            case 0:
                h = hP;
                dP = new FieldVector3D<T>(hP,p.getX().getField().getZero(),p.getX().getField().getZero());
                break;
            case 1:
                h = hP;
                dP = new FieldVector3D<T>(p.getX().getField().getZero(), hP,p.getX().getField().getZero());
                break;
            case 2:
                h = hP;
                dP = new FieldVector3D<T>(p.getX().getField().getZero(), p.getX().getField().getZero(), hP);
                break;
            case 3:
                h = hV;
                dV = new FieldVector3D<T>(hV, p.getX().getField().getZero(),p.getX().getField().getZero());
                break;
            case 4:
                h = hV;
                dV = new FieldVector3D<T>(p.getX().getField().getZero(), hV, p.getX().getField().getZero());
                break;
            default:
                h = hV;
                dV = new FieldVector3D<T>(p.getX().getField().getZero(),p.getX().getField().getZero(), hV);
                break;
        }

        FieldKeplerianOrbit<T> oM4h = new FieldKeplerianOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(1, p, -4, dP), new FieldVector3D<T>(1, v, -4, dV)),
                        orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldKeplerianOrbit<T> oM3h = new FieldKeplerianOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(1, p, -3, dP), new FieldVector3D<T>(1, v, -3, dV)),
                        orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldKeplerianOrbit<T> oM2h = new FieldKeplerianOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(1, p, -2, dP), new FieldVector3D<T>(1, v, -2, dV)),
                        orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldKeplerianOrbit<T> oM1h = new FieldKeplerianOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(1, p, -1, dP), new FieldVector3D<T>(1, v, -1, dV)),
                        orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldKeplerianOrbit<T> oP1h = new FieldKeplerianOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(1, p, +1, dP), new FieldVector3D<T>(1, v, +1, dV)),
                        orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldKeplerianOrbit<T> oP2h = new FieldKeplerianOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(1, p, +2, dP), new FieldVector3D<T>(1, v, +2, dV)),
                        orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldKeplerianOrbit<T> oP3h = new FieldKeplerianOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(1, p, +3, dP), new FieldVector3D<T>(1, v, +3, dV)),
                        orbit.getFrame(), orbit.getDate(), orbit.getMu());
        FieldKeplerianOrbit<T> oP4h = new FieldKeplerianOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(1, p, +4, dP), new FieldVector3D<T>(1, v, +4, dV)),
                        orbit.getFrame(), orbit.getDate(), orbit.getMu());

        jacobian[0][i] = (oP4h.getA().subtract(oM4h.getA()).multiply(-3)).
                        add(oP3h.getA().subtract(oM3h.getA()).multiply(32)).
                        subtract(oP2h.getA().subtract(oM2h.getA()).multiply(168)).
                        add(oP1h.getA().subtract(oM1h.getA()).multiply(672)).
                        divide(h.multiply(840));

        jacobian[1][i] = (oP4h.getE().subtract(oM4h.getE()).multiply(-3)).
                        add(oP3h.getE().subtract(oM3h.getE()).multiply(32)).
                        subtract(oP2h.getE().subtract(oM2h.getE()).multiply(168)).
                        add(oP1h.getE().subtract(oM1h.getE()).multiply(672)).
                        divide(h.multiply(840));

        jacobian[2][i] = (oP4h.getI().subtract(oM4h.getI()).multiply(-3)).
                        add(oP3h.getI().subtract(oM3h.getI()).multiply(32)).
                        subtract(oP2h.getI().subtract(oM2h.getI()).multiply(168)).
                        add(oP1h.getI().subtract(oM1h.getI()).multiply(672)).
                        divide(h.multiply(840));
        jacobian[3][i] = (oP4h.getPerigeeArgument().subtract(oM4h.getPerigeeArgument()).multiply(-3)).
                        add(oP3h.getPerigeeArgument().subtract(oM3h.getPerigeeArgument()).multiply(32)).
                        subtract(oP2h.getPerigeeArgument().subtract(oM2h.getPerigeeArgument()).multiply(168)).
                        add(oP1h.getPerigeeArgument().subtract(oM1h.getPerigeeArgument()).multiply(672)).
                        divide(h.multiply(840));

        jacobian[4][i] =  (oP4h.getRightAscensionOfAscendingNode().subtract(oM4h.getRightAscensionOfAscendingNode()).multiply(-3)).
                        add(oP3h.getRightAscensionOfAscendingNode().subtract(oM3h.getRightAscensionOfAscendingNode()).multiply(32)).
                        subtract(oP2h.getRightAscensionOfAscendingNode().subtract(oM2h.getRightAscensionOfAscendingNode()).multiply(168)).
                        add(oP1h.getRightAscensionOfAscendingNode().subtract(oM1h.getRightAscensionOfAscendingNode()).multiply(672)).
                        divide(h.multiply(840));
        jacobian[5][i] = (oP4h.getAnomaly(type).subtract(oM4h.getAnomaly(type)).multiply(-3)).
                        add(oP3h.getAnomaly(type).subtract(oM3h.getAnomaly(type)).multiply(32)).
                        subtract(oP2h.getAnomaly(type).subtract(oM2h.getAnomaly(type)).multiply(168)).
                        add(oP1h.getAnomaly(type).subtract(oM1h.getAnomaly(type)).multiply(672)).
                        divide(h.multiply(840));

    }

    public <T extends RealFieldElement<T>> void testInterpolation(final Field<T> field) throws OrekitException {
        final T zero = field.getZero();
        final double ehMu  = 3.9860047e14;
        final double ae  = 6.378137e6;
        final T c20 = zero.add(-1.08263e-3);
        final T c30 = zero.add(2.54e-6);
        final T c40 = zero.add( 1.62e-6);
        final T c50 = zero.add(2.3e-7);
        final T c60 = zero.add(-5.5e-7);

        final FieldAbsoluteDate<T> date = FieldAbsoluteDate.getJ2000Epoch(field).shiftedBy(584.);
        final FieldVector3D<T> position = new FieldVector3D<T>(field.getZero().add(3220103.), field.getZero().add(69623.), field.getZero().add(6449822.));
        final FieldVector3D<T> velocity = new FieldVector3D<T>(field.getZero().add(6414.7), field.getZero().add(-2006.), field.getZero().add(-3180.));
        final FieldKeplerianOrbit<T> initialOrbit = new FieldKeplerianOrbit<T>(new FieldPVCoordinates<T>(position, velocity),
                        FramesFactory.getEME2000(), date, ehMu);

        FieldEcksteinHechlerPropagator<T> propagator =
                        new FieldEcksteinHechlerPropagator<T>(initialOrbit, ae, ehMu, c20, c30, c40, c50, c60);

        // set up a 5 points sample
        List<FieldOrbit<T>> sample = new ArrayList<FieldOrbit<T>>();
        for (double dt = 0; dt < 300.0; dt += 60.0) {
            sample.add(propagator.propagate(date.shiftedBy(dt)).getOrbit());
        }

        // well inside the sample, interpolation should be slightly better than Keplerian shift
        // the relative bad behaviour here is due to eccentricity, which cannot be
        // accurately interpolated with a polynomial in this case
        T maxShiftPositionError = zero;
        T maxInterpolationPositionError = zero;
        T maxShiftEccentricityError = zero;
        T maxInterpolationEccentricityError = zero;
        for (double dt = 0; dt < 241.0; dt += 1.0) {
            FieldAbsoluteDate<T> t         = initialOrbit.getDate().shiftedBy(dt);
            FieldVector3D<T> shiftedP      = initialOrbit.shiftedBy(zero.add(dt)).getPVCoordinates().getPosition();
            FieldVector3D<T> interpolatedP = initialOrbit.interpolate(t, sample).getPVCoordinates().getPosition();
            FieldVector3D<T> propagatedP   = propagator.propagate(t).getPVCoordinates().getPosition();
            T shiftedE        = initialOrbit.shiftedBy(zero.add(dt)).getE();
            T interpolatedE   = initialOrbit.interpolate(t, sample).getE();
            T propagatedE     = propagator.propagate(t).getE();
            maxShiftPositionError =  maxShiftPositionError.getReal() > shiftedP.subtract(propagatedP).getNorm().getReal() ?
                                        maxShiftPositionError       :     shiftedP.subtract(propagatedP).getNorm();
            maxInterpolationPositionError = maxInterpolationPositionError.getReal() > interpolatedP.subtract(propagatedP).getNorm().getReal() ?
                                               maxInterpolationPositionError : interpolatedP.subtract(propagatedP).getNorm();
            maxShiftEccentricityError = maxShiftEccentricityError.getReal()  > FastMath.abs(shiftedE.getReal() - propagatedE.getReal()) ?
                                           maxShiftEccentricityError     : shiftedE.subtract(propagatedE).abs();
            maxInterpolationEccentricityError = maxInterpolationEccentricityError.getReal() > FastMath.abs(interpolatedE.getReal() - propagatedE.getReal()) ?
                                                   maxInterpolationEccentricityError   :  interpolatedE.subtract(propagatedE).abs()  ;
        }
        Assert.assertTrue(maxShiftPositionError.getReal()             > 390.0);
        Assert.assertTrue(maxInterpolationPositionError.getReal()    < 62.0);
        Assert.assertTrue(maxShiftEccentricityError.getReal()         > 4.5e-4);
        Assert.assertTrue(maxInterpolationEccentricityError.getReal() < 2.6e-5);

        // slightly past sample end, bad eccentricity interpolation shows up
        // (in this case, interpolated eccentricity exceeds 1.0 btween 1900
        // and 1910s, while semi-majaxis remains positive, so this is not
        // even a proper hyperbolic orbit...)
        maxShiftPositionError = zero;
        maxInterpolationPositionError = zero;
        maxShiftEccentricityError = zero;
        maxInterpolationEccentricityError = zero;
        for (double dt = 240; dt < 600; dt += 1.0) {
            FieldAbsoluteDate<T> t         = initialOrbit.getDate().shiftedBy(dt);
            FieldVector3D<T> shiftedP      = initialOrbit.shiftedBy(zero.add(dt)).getPVCoordinates().getPosition();
            FieldVector3D<T> interpolatedP = initialOrbit.interpolate(t, sample).getPVCoordinates().getPosition();
            FieldVector3D<T> propagatedP   = propagator.propagate(t).getPVCoordinates().getPosition();
            T shiftedE        = initialOrbit.shiftedBy(zero.add(dt)).getE();
            T interpolatedE   = initialOrbit.interpolate(t, sample).getE();
            T propagatedE     = propagator.propagate(t).getE();
            maxShiftPositionError         =  maxShiftPositionError.getReal() > shiftedP.subtract(propagatedP).getNorm().getReal() ?
                                               maxShiftPositionError       :     shiftedP.subtract(propagatedP).getNorm();
            maxInterpolationPositionError = maxInterpolationPositionError.getReal() > interpolatedP.subtract(propagatedP).getNorm().getReal() ?
                                               maxInterpolationPositionError : interpolatedP.subtract(propagatedP).getNorm();
            maxShiftEccentricityError = maxShiftEccentricityError.getReal()  > FastMath.abs(shiftedE.getReal() - propagatedE.getReal()) ?
                                               maxShiftEccentricityError     : shiftedE.subtract(propagatedE).abs();
            maxInterpolationEccentricityError = maxInterpolationEccentricityError.getReal() > FastMath.abs(interpolatedE.getReal() - propagatedE.getReal()) ?
                                                   maxInterpolationEccentricityError   :  interpolatedE.subtract(propagatedE).abs();
        }
        Assert.assertTrue(maxShiftPositionError.getReal()            <  2200.0);
        Assert.assertTrue(maxInterpolationPositionError.getReal()    > 72000.0);
        Assert.assertTrue(maxShiftEccentricityError.getReal()         <  1.2e-3);
        Assert.assertTrue(maxInterpolationEccentricityError.getReal() >  3.8e-3);

    }


    public <T extends RealFieldElement<T>>void testPerfectlyEquatorialConversion(final Field<T> field) throws OrekitException {
        FieldAbsoluteDate<T> dateTca = new FieldAbsoluteDate<T>(field,2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());

        FieldKeplerianOrbit<T> initial = new FieldKeplerianOrbit<T>(field.getZero().add(13378000.0),
                                                                    field.getZero().add(0.05),
                                                                    field.getZero().add(0.0),
                                                                    field.getZero().add(0.0),
                                                                    field.getZero().add(FastMath.PI),
                                                                    field.getZero().add(0.0), PositionAngle.MEAN,
                                                                    FramesFactory.getEME2000(), dateTca,
                                                                    Constants.EIGEN5C_EARTH_MU);
        FieldEquinoctialOrbit<T> equ = (FieldEquinoctialOrbit<T>) OrbitType.EQUINOCTIAL.convertType(initial);
        FieldKeplerianOrbit<T> converted = (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(equ);
        Assert.assertEquals(FastMath.PI,
                            MathUtils.normalizeAngle(converted.getRightAscensionOfAscendingNode().getReal() +
                                                     converted.getPerigeeArgument().getReal(), FastMath.PI),
                            1.0e-10);
    }

    public <T extends RealFieldElement<T>>void testKeplerianDerivatives(final Field<T> field) {
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        final FieldKeplerianOrbit<T> o = new FieldKeplerianOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(field.getZero().add(-4947831.),
                                                                                                                   field.getZero().add(-3765382.),
                                                                                                                   field.getZero().add(-3708221.)),
                                                                                              new FieldVector3D<T>(field.getZero().add(-2079.),
                                                                                                                   field.getZero().add(5291.),
                                                                                                                   field.getZero().add(-7842.))),
                                                                    FramesFactory.getEME2000(), date, 3.9860047e14);
        final FieldVector3D<T> p = o.getPVCoordinates().getPosition();
        final FieldVector3D<T> v = o.getPVCoordinates().getVelocity();
        final FieldVector3D<T> a = o.getPVCoordinates().getAcceleration();

        // check that despite we did not provide acceleration, it got recomputed
        Assert.assertEquals(7.605422, a.getNorm().getReal(), 1.0e-6);

        FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(8, 0.1);

        // check velocity is the derivative of position
        double vx = differentiator.differentiate(new UnivariateFunction() {
            public double value(double dt) {
                return o.shiftedBy(field.getZero().add(dt)).getPVCoordinates().getPosition().getX().getReal();
            }
        }).value(new DerivativeStructure(1, 1, 0, 0.0)).getPartialDerivative(1);
        Assert.assertEquals(o.getPVCoordinates().getVelocity().getX().getReal(), vx, 3.0e-12 * v.getNorm().getReal());
        double vy = differentiator.differentiate(new UnivariateFunction() {
            public double value(double dt) {
                return o.shiftedBy(field.getZero().add(dt)).getPVCoordinates().getPosition().getY().getReal();
            }
        }).value(new DerivativeStructure(1, 1, 0, 0.0)).getPartialDerivative(1);
        Assert.assertEquals(o.getPVCoordinates().getVelocity().getY().getReal(), vy, 3.0e-12 * v.getNorm().getReal());
        double vz = differentiator.differentiate(new UnivariateFunction() {
            public double value(double dt) {
                return o.shiftedBy(field.getZero().add(dt)).getPVCoordinates().getPosition().getZ().getReal();
            }
        }).value(new DerivativeStructure(1, 1, 0, 0.0)).getPartialDerivative(1);
        Assert.assertEquals(o.getPVCoordinates().getVelocity().getZ().getReal(), vz, 3.0e-12 * v.getNorm().getReal());

        // check acceleration is the derivative of velocity
        double ax = differentiator.differentiate(new UnivariateFunction() {
            public double value(double dt) {
                return o.shiftedBy(field.getZero().add(dt)).getPVCoordinates().getVelocity().getX().getReal();
            }
        }).value(new DerivativeStructure(1, 1, 0, 0.0)).getPartialDerivative(1);
        Assert.assertEquals(o.getPVCoordinates().getAcceleration().getX().getReal(), ax, 3.0e-12 * a.getNorm().getReal());
        double ay = differentiator.differentiate(new UnivariateFunction() {
            public double value(double dt) {
                return o.shiftedBy(field.getZero().add(dt)).getPVCoordinates().getVelocity().getY().getReal();
            }
        }).value(new DerivativeStructure(1, 1, 0, 0.0)).getPartialDerivative(1);
        Assert.assertEquals(o.getPVCoordinates().getAcceleration().getY().getReal(), ay, 3.0e-12 * a.getNorm().getReal());
        double az = differentiator.differentiate(new UnivariateFunction() {
            public double value(double dt) {
                return o.shiftedBy(field.getZero().add(dt)).getPVCoordinates().getVelocity().getZ().getReal();
            }
        }).value(new DerivativeStructure(1, 1, 0, 0.0)).getPartialDerivative(1);
        Assert.assertEquals(o.getPVCoordinates().getAcceleration().getZ().getReal(), az, 3.0e-12 * a.getNorm().getReal());

        // check jerk is the derivative of acceleration
        final T r2 = p.getNormSq();
        final T r  = r2.sqrt();
        FieldVector3D<T> keplerianJerk = new FieldVector3D<T>(FieldVector3D.dotProduct(p, v).multiply(-3).divide(r2), a,
                                                              a.getNorm().divide(r).multiply(-1), v);
        double jx = differentiator.differentiate(new UnivariateFunction() {
            public double value(double dt) {
                return o.shiftedBy(field.getZero().add(dt)).getPVCoordinates().getAcceleration().getX().getReal();
            }
        }).value(new DerivativeStructure(1, 1, 0, 0.0)).getPartialDerivative(1);


        Assert.assertEquals(keplerianJerk.getX().getReal(), jx, 4.0e-12 * keplerianJerk.getNorm().getReal());
        double jy = differentiator.differentiate(new UnivariateFunction() {
            public double value(double dt) {
                return o.shiftedBy(field.getZero().add(dt)).getPVCoordinates().getAcceleration().getY().getReal();
            }
        }).value(new DerivativeStructure(1, 1, 0, 0.0)).getPartialDerivative(1);
        Assert.assertEquals(keplerianJerk.getY().getReal(), jy, 4.0e-12 * keplerianJerk.getNorm().getReal());
        double jz = differentiator.differentiate(new UnivariateFunction() {
            public double value(double dt) {
                return o.shiftedBy(field.getZero().add(dt)).getPVCoordinates().getAcceleration().getZ().getReal();
            }
        }).value(new DerivativeStructure(1, 1, 0, 0.0)).getPartialDerivative(1);
        Assert.assertEquals(keplerianJerk.getZ().getReal(), jz, 4.0e-12 * keplerianJerk.getNorm().getReal());

    }

    @Before
    public void setUp() {

        Utils.setDataRoot("regular-data");

        // Body mu
        mu = 3.9860047e14;

    }

}