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
package org.orekit.propagation.analytical;


import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.stat.descriptive.rank.Max;
import org.hipparchus.stat.descriptive.rank.Min;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.Tuple;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.attitudes.InertialProvider;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldCircularOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldBoundedPropagator;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.FieldAbstractDetector;
import org.orekit.propagation.events.FieldAltitudeDetector;
import org.orekit.propagation.events.FieldApsideDetector;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldElevationDetector;
//import org.orekit.propagation.events.AltitudeDetector;
//import org.orekit.propagation.events.ApsideDetector;
//import org.orekit.propagation.events.DateDetector;
//import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.FieldNodeDetector;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.propagation.sampling.FieldOrekitFixedStepHandler;
import org.orekit.propagation.sampling.FieldOrekitStepHandler;
import org.orekit.propagation.sampling.FieldOrekitStepHandlerMultiplexer;
import org.orekit.propagation.sampling.FieldOrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


public class FieldKeplerianPropagatorTest {

    // Body mu
    private double mu;

    @Test
    public void testTuple() {

        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        KeplerianOrbit k0 = new KeplerianOrbit(7209668.0, 0.5e-4, 1.7, 2.1, 2.9,
                                                6.2, PositionAngle.TRUE,
                                                FramesFactory.getEME2000(), initDate, mu);
        TimeStampedPVCoordinates pv0 = k0.getPVCoordinates();
        TimeStampedPVCoordinates pv1 = new TimeStampedPVCoordinates(pv0.getDate(),
                                                                    pv0.getPosition(),
                                                                    pv0.getVelocity().add(new Vector3D(2.0, pv0.getVelocity().normalize())));
        KeplerianOrbit k1 = new KeplerianOrbit(pv1, k0.getFrame(), k0.getMu());
        FieldOrbit<Tuple> twoOrbits =
                        new FieldKeplerianOrbit<>(new Tuple(k0.getA(),                             k1.getA()),
                                                  new Tuple(k0.getE(),                             k1.getE()),
                                                  new Tuple(k0.getI(),                             k1.getI()),
                                                  new Tuple(k0.getPerigeeArgument(),               k1.getPerigeeArgument()),
                                                  new Tuple(k0.getRightAscensionOfAscendingNode(), k1.getRightAscensionOfAscendingNode()),
                                                  new Tuple(k0.getMeanAnomaly(),                   k1.getMeanAnomaly()),
                                                  PositionAngle.MEAN,
                                                  FramesFactory.getEME2000(),
                                                  new FieldAbsoluteDate<>(initDate, new Tuple(0.0, 0.0)),
                                                  new Tuple(mu, mu));
        Field<Tuple> field = twoOrbits.getDate().getField();
        FieldPropagator<Tuple> propagator = new FieldKeplerianPropagator<>(twoOrbits);
        Min minTangential = new Min();
        Max maxTangential = new Max();
        Min minRadial     = new Min();
        Max maxRadial     = new Max();
        propagator.setMasterMode(field.getZero().add(10.0),
                                 (s, isLast) -> {
                                     FieldVector3D<Tuple> p = s.getPVCoordinates().getPosition();
                                     FieldVector3D<Tuple> v = s.getPVCoordinates().getVelocity();
                                     Vector3D p0 = new Vector3D(p.getX().getComponent(0),
                                                                p.getY().getComponent(0),
                                                                p.getZ().getComponent(0));
                                     Vector3D v0 = new Vector3D(v.getX().getComponent(0),
                                                                v.getY().getComponent(0),
                                                                v.getZ().getComponent(0));
                                     Vector3D t  = v0.normalize();
                                     Vector3D r  = Vector3D.crossProduct(v0, Vector3D.crossProduct(p0, v0)).normalize();
                                     Vector3D p1 = new Vector3D(p.getX().getComponent(1),
                                                                p.getY().getComponent(1),
                                                                p.getZ().getComponent(1));
                                     double dT = Vector3D.dotProduct(p1.subtract(p0), t);
                                     double dR = Vector3D.dotProduct(p1.subtract(p0), r);
                                     minTangential.increment(dT);
                                     maxTangential.increment(dT);
                                     minRadial.increment(dR);
                                     maxRadial.increment(dR);
                                 });
        propagator.propagate(twoOrbits.getDate().shiftedBy(Constants.JULIAN_DAY / 8));
        Assert.assertEquals(-72525.685, minTangential.getResult(), 1.0e-3);
        Assert.assertEquals(   926.046, maxTangential.getResult(), 1.0e-3);
        Assert.assertEquals(   -92.800, minRadial.getResult(),     1.0e-3);
        Assert.assertEquals(  7739.532, maxRadial.getResult(),     1.0e-3);
        
    }

    @Test
    public void testSameDateCartesian() {
        doTestSameDateCartesian(Decimal64Field.getInstance());
    }


    @Test
    public void testSameDateKeplerian() {
        doTestSameDateKeplerian(Decimal64Field.getInstance());
    }


    @Test
    public void testPropagatedCartesian() {
        doTestPropagatedCartesian(Decimal64Field.getInstance());
    }


    @Test
    public void testPropagatedKeplerian() {
        doTestPropagatedKeplerian(Decimal64Field.getInstance());
    }


    @Test
    public void testAscendingNode() {
        doTestAscendingNode(Decimal64Field.getInstance());
    }


    @Test
    public void testStopAtTargetDate() {
        doTestStopAtTargetDate(Decimal64Field.getInstance());
    }


    @Test
    public void testFixedStep() {
        doTestFixedStep(Decimal64Field.getInstance());
    }


    @Test
    public void testVariableStep() {
        doTestVariableStep(Decimal64Field.getInstance());
    }


    @Test
    public void testEphemeris() {
        doTestEphemeris(Decimal64Field.getInstance());}


    @Test
    public void testIssue14() {
        doTestIssue14(Decimal64Field.getInstance());
    }


    @Test
    public void testIssue107() {
        doTestIssue107(Decimal64Field.getInstance());
    }


    @Test
    public void testMu() {
        doTestMu(Decimal64Field.getInstance());
    }

    @Test
    public void testNoDerivatives() {
        doTestNoDerivatives(Decimal64Field.getInstance());
    }

    @Test(expected = OrekitException.class)
    public void testWrongAttitude() {
        doTestWrongAttitude(Decimal64Field.getInstance());
    }

    @Test(expected = OrekitException.class)
    public void testStepException() {
        doTestStepException(Decimal64Field.getInstance());
    }

    @Test(expected = OrekitException.class)
    public void testWrappedAttitudeException() {
        doTestWrappedAttitudeException(Decimal64Field.getInstance());
    }

    @Test
    public void testPerigee() {
        doTestPerigee(Decimal64Field.getInstance());
    }

    @Test
    public void testAltitude() {
        doTestAltitude(Decimal64Field.getInstance());
    }

    @Test
    public void testDate() {
        doTestDate(Decimal64Field.getInstance());
    }

    @Test
    public void testSetting() {
        doTestSetting(Decimal64Field.getInstance());
    }

    @Test
    public void testDefaultLaw() {
        Assert.assertSame(InertialProvider.EME2000_ALIGNED, FieldPropagator.DEFAULT_LAW);
    }

    private <T extends RealFieldElement<T>> void doTestSameDateCartesian(Field<T> field) {
        T zero = field.getZero();
        // Definition of initial conditions with position and velocity
        //------------------------------------------------------------
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field).shiftedBy(584.);
        FieldOrbit<T> initialOrbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                                 FramesFactory.getEME2000(), initDate, zero.add(mu));

        // Extrapolator definition
        // -----------------------
        FieldKeplerianPropagator<T> extrapolator = new FieldKeplerianPropagator<>(initialOrbit);

        // Extrapolation at the initial date
        // ---------------------------------
        T delta_t = zero; // extrapolation duration in seconds
        FieldAbsoluteDate<T> extrapDate = initDate.shiftedBy(delta_t);

        FieldSpacecraftState<T> finalOrbit = extrapolator.propagate(extrapDate);

        T a = finalOrbit.getA();
        // another way to compute n
        T n = a.pow(3).reciprocal().multiply(finalOrbit.getMu()).sqrt();

        Assert.assertEquals(n.getReal()*delta_t.getReal(),
                            finalOrbit.getLM().getReal() - initialOrbit.getLM().getReal(),
                            Utils.epsilonTest * FastMath.abs(n.getReal()*delta_t.getReal()));
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbit.getLM().getReal(), initialOrbit.getLM().getReal()), initialOrbit.getLM().getReal(),
                            Utils.epsilonAngle * FastMath.abs(initialOrbit.getLM().getReal()));

        Assert.assertEquals(finalOrbit.getA().getReal(), initialOrbit.getA().getReal(),
                            Utils.epsilonTest * initialOrbit.getA().getReal());
        Assert.assertEquals(finalOrbit.getE().getReal(), initialOrbit.getE().getReal(),
                            Utils.epsilonE * initialOrbit.getE().getReal());
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbit.getI().getReal(), initialOrbit.getI().getReal()),
                            initialOrbit.getI().getReal(), Utils.epsilonAngle * FastMath.abs(initialOrbit.getI().getReal()));

    }

    private <T extends RealFieldElement<T>> void doTestSameDateKeplerian(Field<T> field) {
        T zero = field.getZero();
        // Definition of initial conditions with Keplerian parameters
        //-----------------------------------------------------------
        FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field).shiftedBy(584.);
        FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(zero.add(7209668.0), zero.add(0.5e-4), zero.add(1.7), zero.add(2.1), zero.add(2.9),
                                                               zero.add(6.2), PositionAngle.TRUE,
                                                               FramesFactory.getEME2000(), initDate, zero.add(mu));

        // Extrapolator definition
        // -----------------------
        FieldKeplerianPropagator<T> extrapolator = new FieldKeplerianPropagator<>(initialOrbit);

        // Extrapolation at the initial date
        // ---------------------------------
        T delta_t = zero; // extrapolation duration in seconds
        FieldAbsoluteDate<T> extrapDate = initDate.shiftedBy(delta_t);

        FieldSpacecraftState<T> finalOrbit = extrapolator.propagate(extrapDate);

        T a = finalOrbit.getA();
        // another way to compute n
        T n = a.pow(3).reciprocal().multiply(finalOrbit.getMu()).sqrt();

        Assert.assertEquals(n.getReal()*delta_t.getReal(),
                     finalOrbit.getLM().getReal() - initialOrbit.getLM().getReal(),
                     Utils.epsilonTest * FastMath.max(100., FastMath.abs(n.getReal()*delta_t.getReal())));
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbit.getLM().getReal(), initialOrbit.getLM().getReal()),
                            initialOrbit.getLM().getReal(), Utils.epsilonAngle * FastMath.abs(initialOrbit.getLM().getReal()));

        Assert.assertEquals(finalOrbit.getA().getReal(), initialOrbit.getA().getReal(),
                            Utils.epsilonTest * initialOrbit.getA().getReal());
        Assert.assertEquals(finalOrbit.getE().getReal(), initialOrbit.getE().getReal(),
                            Utils.epsilonE * initialOrbit.getE().getReal());
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbit.getI().getReal(), initialOrbit.getI().getReal()),
                            initialOrbit.getI().getReal(),
                            Utils.epsilonAngle * FastMath.abs(initialOrbit.getI().getReal()));

    }

    private <T extends RealFieldElement<T>> void doTestPropagatedCartesian(Field<T> field) {
        T zero = field.getZero();
        // Definition of initial conditions with position and velocity
        //------------------------------------------------------------
        FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));
        double mu = 3.9860047e14;

        FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field).shiftedBy(584.);
        FieldOrbit<T> initialOrbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                                 FramesFactory.getEME2000(), initDate, zero.add(mu));

        // Extrapolator definition
        // -----------------------
        FieldKeplerianPropagator<T> extrapolator = new FieldKeplerianPropagator<>(initialOrbit);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        T delta_t = zero.add(100000.0); // extrapolation duration in seconds
        FieldAbsoluteDate<T> extrapDate = initDate.shiftedBy(delta_t);

        FieldSpacecraftState<T> finalOrbit = extrapolator.propagate(extrapDate);


        // computation of (M final - M initial) with another method
        T a = finalOrbit.getA();
        // another way to compute n
        T n = a.pow(3).reciprocal().multiply(finalOrbit.getMu()).sqrt();

        Assert.assertEquals(n.getReal() * delta_t.getReal(),
                            finalOrbit.getLM().getReal() - initialOrbit.getLM().getReal(),
                            Utils.epsilonAngle);

        // computation of M final orbit
        T LM = finalOrbit.getLE().subtract(
        finalOrbit.getEquinoctialEx().multiply(finalOrbit.getLE().sin())).add(
        finalOrbit.getEquinoctialEy().multiply(finalOrbit.getLE().cos()));

        Assert.assertEquals(LM.getReal() , finalOrbit.getLM().getReal() , Utils.epsilonAngle);

        // test of tan ((LE - Lv)/2) :
        Assert.assertEquals(FastMath.tan((finalOrbit.getLE().getReal() - finalOrbit.getLv().getReal())/2.),
                            tangLEmLv(finalOrbit.getLv(), finalOrbit.getEquinoctialEx(), finalOrbit.getEquinoctialEy()).getReal(),
                            Utils.epsilonAngle);

        // test of evolution of M vs E: LM = LE - ex*sin(LE) + ey*cos(LE)
        // with ex and ey the same for initial and final orbit
        T deltaM = finalOrbit.getLM().subtract(initialOrbit.getLM());
        T deltaE = finalOrbit.getLE().subtract(initialOrbit.getLE());
        T delta  = finalOrbit.getEquinoctialEx().multiply(finalOrbit.getLE().sin().subtract(initialOrbit.getLE().sin())).subtract(
                   finalOrbit.getEquinoctialEy().multiply(finalOrbit.getLE().cos().subtract(initialOrbit.getLE().cos())));

        Assert.assertEquals(deltaM.getReal(), deltaE.getReal() - delta.getReal(), Utils.epsilonAngle);

        // the orbital elements except for Mean/True/Eccentric latitude arguments are the same
        Assert.assertEquals(finalOrbit.getA().getReal(), initialOrbit.getA().getReal(), Utils.epsilonTest * initialOrbit.getA().getReal());
        Assert.assertEquals(finalOrbit.getEquinoctialEx().getReal(), initialOrbit.getEquinoctialEx().getReal(), Utils.epsilonE);
        Assert.assertEquals(finalOrbit.getEquinoctialEy().getReal(), initialOrbit.getEquinoctialEy().getReal(), Utils.epsilonE);
        Assert.assertEquals(finalOrbit.getHx().getReal(), initialOrbit.getHx().getReal(), Utils.epsilonAngle);
        Assert.assertEquals(finalOrbit.getHy().getReal(), initialOrbit.getHy().getReal(), Utils.epsilonAngle);

        // for final orbit
        T ex = finalOrbit.getEquinoctialEx();
        T ey = finalOrbit.getEquinoctialEy();
        T hx = finalOrbit.getHx();
        T hy = finalOrbit.getHy();
        T LE = finalOrbit.getLE();

        T ex2 = ex.multiply(ex);
        T ey2 = ey.multiply(ey);
        T hx2 = hx.multiply(hx);
        T hy2 = hy.multiply(hy);
        T h2p1 = hx2.add(1.).add(hy2);
        T beta = ex2.negate().add(1.).subtract(ey2).sqrt().add(1.).reciprocal();

        T x3 = ex.negate().add(beta.negate().multiply(ey2).add(1.).multiply(LE.cos())).add(beta.multiply(ex).multiply(ey).multiply(LE.sin()));
        T y3 = ey.negate().add(beta.negate().multiply(ex2).add(1.).multiply(LE.sin())).add(beta.multiply(ex).multiply(ey).multiply(LE.cos()));
        // ey.negate.add(beta.negate().multiply(ex2).add(1.).multiply(LE.sin())).add(beta.multiply(ex).multiply(ey).multiply(LE.cos()));

        FieldVector3D<T> U = new FieldVector3D<>(hx2.add(1.).subtract(hy2).divide(h2p1),
                                                 hx.multiply(2.).multiply(hy).divide(h2p1),
                                                 hy.multiply(-2.).divide(h2p1));

        FieldVector3D<T> V = new FieldVector3D<>(hx.multiply(2.).multiply(hy).divide(h2p1),
                                                 hy2.subtract(hx2).add(1).divide(h2p1),
                                                 hx.multiply(2.).divide(h2p1));

        FieldVector3D<T> r = new FieldVector3D<>(finalOrbit.getA(), new FieldVector3D<>(x3, U, y3, V));

        Assert.assertEquals(finalOrbit.getPVCoordinates().getPosition().getNorm().getReal(), r.getNorm().getReal(), Utils.epsilonTest * r.getNorm().getReal());

    }

    private <T extends RealFieldElement<T>> void doTestPropagatedKeplerian(Field<T> field) {
        T zero = field.getZero();
        // Definition of initial conditions with Keplerian parameters
        //-----------------------------------------------------------
        FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field).shiftedBy(584.);
        FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(zero.add(7209668.0), zero.add(0.5e-4), zero.add(1.7), zero.add(2.1), zero.add(2.9),
                                                               zero.add(6.2), PositionAngle.TRUE,
                                                               FramesFactory.getEME2000(), initDate, zero.add(mu));

        // Extrapolator definition
        // -----------------------
        FieldKeplerianPropagator<T> extrapolator = new FieldKeplerianPropagator<>(initialOrbit);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        T delta_t = zero.add(100000.0); // extrapolation duration in seconds
        FieldAbsoluteDate<T> extrapDate = initDate.shiftedBy(delta_t);

        FieldSpacecraftState<T> finalOrbit = extrapolator.propagate(extrapDate);
        Assert.assertEquals(6092.3362422560844633, finalOrbit.getKeplerianPeriod().getReal(), 1.0e-12);
        Assert.assertEquals(0.001031326088602888358, finalOrbit.getKeplerianMeanMotion().getReal(), 1.0e-16);

        // computation of (M final - M initial) with another method
        T a = finalOrbit.getA();
        // another way to compute n
        T n = a.pow(3).reciprocal().multiply(finalOrbit.getMu()).sqrt();

        Assert.assertEquals(n.getReal() * delta_t.getReal(),
                     finalOrbit.getLM().getReal() - initialOrbit.getLM().getReal(),
                     Utils.epsilonAngle);

        // computation of M final orbit
        T LM = finalOrbit.getLE().subtract(
               finalOrbit.getEquinoctialEx().multiply(finalOrbit.getLE().sin())).add(
               finalOrbit.getEquinoctialEy().multiply(finalOrbit.getLE().cos()));

        Assert.assertEquals(LM.getReal() , finalOrbit.getLM().getReal() , Utils.epsilonAngle);

        // test of tan ((LE - Lv)/2) :
        Assert.assertEquals(FastMath.tan((finalOrbit.getLE().getReal() - finalOrbit.getLv().getReal())/2.),
                     tangLEmLv(finalOrbit.getLv(), finalOrbit.getEquinoctialEx(), finalOrbit.getEquinoctialEy()).getReal(),
                     Utils.epsilonAngle);

        // test of evolution of M vs E: LM = LE - ex*sin(LE) + ey*cos(LE)
        // with ex and ey the same for initial and final orbit
        T deltaM = finalOrbit.getLM().subtract(initialOrbit.getLM());
        T deltaE = finalOrbit.getLE().subtract(initialOrbit.getLE());
        T delta  = finalOrbit.getEquinoctialEx().multiply(finalOrbit.getLE().sin().subtract(initialOrbit.getLE().sin())).subtract(
                   finalOrbit.getEquinoctialEy().multiply(finalOrbit.getLE().cos().subtract(initialOrbit.getLE().cos())));

        Assert.assertEquals(deltaM.getReal(), deltaE.getReal() - delta.getReal(), Utils.epsilonAngle);

        // the orbital elements except for Mean/True/Eccentric latitude arguments are the same
        Assert.assertEquals(finalOrbit.getA().getReal(), initialOrbit.getA().getReal(), Utils.epsilonTest * initialOrbit.getA().getReal());
        Assert.assertEquals(finalOrbit.getEquinoctialEx().getReal(), initialOrbit.getEquinoctialEx().getReal(), Utils.epsilonE);
        Assert.assertEquals(finalOrbit.getEquinoctialEy().getReal(), initialOrbit.getEquinoctialEy().getReal(), Utils.epsilonE);
        Assert.assertEquals(finalOrbit.getHx().getReal(), initialOrbit.getHx().getReal(), Utils.epsilonAngle);
        Assert.assertEquals(finalOrbit.getHy().getReal(), initialOrbit.getHy().getReal(), Utils.epsilonAngle);

        // for final orbit
        T ex = finalOrbit.getEquinoctialEx();
        T ey = finalOrbit.getEquinoctialEy();
        T hx = finalOrbit.getHx();
        T hy = finalOrbit.getHy();
        T LE = finalOrbit.getLE();

        T ex2 = ex.multiply(ex);
        T ey2 = ey.multiply(ey);
        T hx2 = hx.multiply(hx);
        T hy2 = hy.multiply(hy);
        T h2p1 = hx2.add(hy2).add(1.);
        T beta = ex2.negate().add(1.).subtract(ey2).sqrt().add(1.).reciprocal();

        T x3 = ex.negate().add(beta.negate().multiply(ey2).add(1.).multiply(LE.cos())).add(beta.multiply(ex).multiply(ey).multiply(LE.sin()));
        T y3 = ey.negate().add(beta.negate().multiply(ex2).add(1.).multiply(LE.sin())).add(beta.multiply(ex).multiply(ey).multiply(LE.cos()));

        FieldVector3D<T> U = new FieldVector3D<>(hx2.add(1.).subtract(hy2).divide(h2p1),
                                                 hx.multiply(2.).multiply(hy).divide(h2p1),
                                                 hy.multiply(-2).divide(h2p1));

        FieldVector3D<T> V = new FieldVector3D<>(hx.multiply(2).multiply(hy).divide(h2p1),
                                                 hy2.subtract(hx2).add(1.).divide(h2p1),
                                                 hx.multiply(2).divide(h2p1));

        FieldVector3D<T> r = new FieldVector3D<>(finalOrbit.getA(), new FieldVector3D<>(x3, U, y3, V));

        Assert.assertEquals(finalOrbit.getPVCoordinates().getPosition().getNorm().getReal(), r.getNorm().getReal(), Utils.epsilonTest * r.getNorm().getReal());

    }

    private <T extends RealFieldElement<T>> void doTestWrongAttitude(Field<T> field) {
        T zero = field.getZero();
        FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(1.0e10), zero.add(1.0e-4), zero.add(1.0e-2), zero, zero, zero, PositionAngle.TRUE,
                                      FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field), zero.add(3.986004415e14));
        AttitudeProvider wrongLaw = new AttitudeProvider() {
            @Override
            public Attitude getAttitude(PVCoordinatesProvider pvProv, AbsoluteDate date, Frame frame) {
                throw new OrekitException(new DummyLocalizable("gasp"), new RuntimeException());
            }
            @Override
            public <Q extends RealFieldElement<Q>> FieldAttitude<Q> getAttitude(FieldPVCoordinatesProvider<Q> pvProv,
                                                                                FieldAbsoluteDate<Q> date,
                                                                                Frame frame) {
                throw new OrekitException(new DummyLocalizable("gasp"), new RuntimeException());
            }
        };
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit, wrongLaw);
        propagator.propagate(new FieldAbsoluteDate<>(field).shiftedBy(10.0));
    }

    private <T extends RealFieldElement<T>> void doTestStepException(Field<T> field) {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                                      FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field), zero.add(3.986004415e14));
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);
        FieldOrekitStepHandlerMultiplexer<T> multiplexer = new FieldOrekitStepHandlerMultiplexer<>();
        propagator.setMasterMode(multiplexer);
        multiplexer.add(new FieldOrekitStepHandler<T>() {
            public void init(FieldSpacecraftState<T> s0, FieldAbsoluteDate<T> t) {
            }
            public void handleStep(FieldOrekitStepInterpolator<T> interpolator,
                                   boolean isLast) {
                if (isLast) {
                    throw new OrekitException((Throwable) null, new DummyLocalizable("dummy error"));
                }
            }
        });

        propagator.propagate(orbit.getDate().shiftedBy(-3600));

    }

    private <T extends RealFieldElement<T>> void doTestWrappedAttitudeException(Field<T> field) {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                                      FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field), zero.add(3.986004415e14));
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit,
                        new AttitudeProvider() {
            public Attitude getAttitude(PVCoordinatesProvider pvProv, AbsoluteDate date, Frame frame) {
                throw new OrekitException((Throwable) null, new DummyLocalizable("dummy error"));
            }
            public <Q extends RealFieldElement<Q>> FieldAttitude<Q> getAttitude(FieldPVCoordinatesProvider<Q> pvProv,
                                                                                FieldAbsoluteDate<Q> date,
                                                                                Frame frame) {
                throw new OrekitException((Throwable) null, new DummyLocalizable("dummy error"));
            }
        });
        propagator.propagate(orbit.getDate().shiftedBy(10.09));
    }

    private <T extends RealFieldElement<T>> void doTestAscendingNode(Field<T> field) {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                                      FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field), zero.add(3.986004415e14));
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);
        propagator.addEventDetector(new FieldNodeDetector<>(orbit, FramesFactory.getITRF(IERSConventions.IERS_2010, true)));
        FieldAbsoluteDate<T> farTarget = new FieldAbsoluteDate<>(field).shiftedBy(10000.0);
        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);
        FieldPVCoordinates<T> pv = propagated.getPVCoordinates(FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() > 3500.0);
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() < 4000.0);
        Assert.assertEquals(0, pv.getPosition().getZ().getReal(), 2.0e-6);
        Assert.assertTrue(pv.getVelocity().getZ().getReal() > 0);
    }

    private <T extends RealFieldElement<T>> void doTestStopAtTargetDate(Field<T> field) {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                                      FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field), zero.add(3.986004415e14));
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        propagator.addEventDetector(new FieldNodeDetector<>(orbit, itrf).withHandler(new FieldContinueOnEvent<FieldNodeDetector<T>, T>()));
        FieldAbsoluteDate<T> farTarget = orbit.getDate().shiftedBy(10000.0);
        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);
        Assert.assertEquals(0.0, FastMath.abs(farTarget.durationFrom(propagated.getDate()).getReal()), 1.0e-3);
    }

    private <T extends RealFieldElement<T>> void doTestPerigee(Field<T> field) {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                                      FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field), zero.add(3.986004415e14));
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);
        propagator.addEventDetector(new FieldApsideDetector<>(orbit));
        FieldAbsoluteDate<T> farTarget = new FieldAbsoluteDate<>(field).shiftedBy(10000.0);
        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);
        FieldPVCoordinates<T> pv = propagated.getPVCoordinates(FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() > 3000.0);
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() < 3500.0);
        Assert.assertEquals(orbit.getA().getReal() * (1.0 - orbit.getE().getReal()), pv.getPosition().getNorm().getReal(), 1.0e-6);
    }

    private <T extends RealFieldElement<T>> void doTestAltitude(Field<T> field) {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                                      FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field), zero.add(3.986004415e14));
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);
        BodyShape bodyShape =
            new OneAxisEllipsoid(6378137.0, 1.0 / 298.257222101, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        FieldAltitudeDetector<T> detector =
            new FieldAltitudeDetector<>(orbit.getKeplerianPeriod().multiply(0.05),
                                        zero.add(1500000), bodyShape);
        Assert.assertEquals(1500000, detector.getAltitude().getReal(), 1.0e-12);
        propagator.addEventDetector(detector);
        FieldAbsoluteDate<T> farTarget = new FieldAbsoluteDate<>(field).shiftedBy(10000.0);
        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() > 5400.0);
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() < 5500.0);
        FieldGeodeticPoint<T> gp = bodyShape.transform(propagated.getPVCoordinates().getPosition(),
                                                       propagated.getFrame(), propagated.getDate());
        Assert.assertEquals(1500000, gp.getAltitude().getReal(), 0.1);
    }

    private <T extends RealFieldElement<T>> void doTestDate(Field<T> field) {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                                      FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field), zero.add(3.986004415e14));
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);
        final FieldAbsoluteDate<T> stopDate = new FieldAbsoluteDate<>(field).shiftedBy(500.0);
        propagator.addEventDetector(new FieldDateDetector<>(stopDate));
        FieldAbsoluteDate<T> farTarget = new FieldAbsoluteDate<>(field).shiftedBy(10000.0);
        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);
        Assert.assertEquals(0, stopDate.durationFrom(propagated.getDate()).getReal(), 1.0e-10);
    }

    private <T extends RealFieldElement<T>> void doTestSetting(Field<T> field) {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                                      FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field), zero.add(3.986004415e14));
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);
        final OneAxisEllipsoid earthShape =
            new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final TopocentricFrame topo =
            new TopocentricFrame(earthShape, new GeodeticPoint(0.389, -2.962, 0), null);
        propagator.addEventDetector(new FieldElevationDetector<>(zero.add(60),
                                                                 zero.add(FieldAbstractDetector.DEFAULT_THRESHOLD),
                                                                 topo).withConstantElevation(0.09));
        FieldAbsoluteDate<T> farTarget = new FieldAbsoluteDate<>(field).shiftedBy(10000.0);
        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);
        final double elevation = topo.getElevation(propagated.getPVCoordinates().getPosition().toVector3D(),
                                                   propagated.getFrame(),
                                                   propagated.getDate().toAbsoluteDate());
        final T zVelocity = propagated.getPVCoordinates(topo).getVelocity().getZ();
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() > 7800.0);
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() < 7900.0);
        Assert.assertEquals(0.09, elevation, 1.0e-9);
        Assert.assertTrue(zVelocity.getReal() < 0);
    }

    private <T extends RealFieldElement<T>> void doTestFixedStep(Field<T> field) {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                                      FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field), zero.add(3.986004415e14));
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);
        final T step = zero.add(100.0);
        propagator.setMasterMode(step, new FieldOrekitFixedStepHandler<T>() {
            private FieldAbsoluteDate<T> previous;
            public void handleStep(FieldSpacecraftState<T> currentState, boolean isLast)
            {
                if (previous != null) {
                    Assert.assertEquals(step.getReal(), currentState.getDate().durationFrom(previous).getReal(), 1.0e-10);
                }
                previous = currentState.getDate();
            }
        });
        FieldAbsoluteDate<T> farTarget = new FieldAbsoluteDate<>(field).shiftedBy(10000.0);
        propagator.propagate(farTarget);
    }

    private <T extends RealFieldElement<T>> void doTestVariableStep(Field<T> field) {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                                      FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field), zero.add(3.986004415e14));
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);
        final T step = orbit.getKeplerianPeriod().divide(100);
        propagator.setMasterMode(new FieldOrekitStepHandler<T>() {
            private FieldAbsoluteDate<T> previous;
            public void init(FieldSpacecraftState<T> s0, FieldAbsoluteDate<T> t) {
            }
            public void handleStep(FieldOrekitStepInterpolator<T> interpolator,
                                   boolean isLast) {
                if ((previous != null) && !isLast) {
                    Assert.assertEquals(step.getReal(), interpolator.getCurrentState().getDate().durationFrom(previous).getReal(), 1.0e-10);
                }
                previous = interpolator.getCurrentState().getDate();
            }
        });
        FieldAbsoluteDate<T> farTarget = new FieldAbsoluteDate<>(field).shiftedBy(10000.0);
        propagator.propagate(farTarget);
    }

    private <T extends RealFieldElement<T>> void doTestEphemeris(Field<T> field) {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                                      FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field), zero.add(3.986004415e14));
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);
        propagator.setEphemerisMode();
        FieldAbsoluteDate<T> farTarget = new FieldAbsoluteDate<>(field).shiftedBy(10000.0);
        propagator.setEphemerisMode();
        propagator.propagate(farTarget);
        FieldBoundedPropagator<T> ephemeris = propagator.getGeneratedEphemeris();
        Assert.assertEquals(0.0, ephemeris.getMinDate().durationFrom(orbit.getDate()).getReal(), 1.0e-10);
        Assert.assertEquals(0.0, ephemeris.getMaxDate().durationFrom(farTarget).getReal(), 1.0e-10);
    }

    private <T extends RealFieldElement<T>> void doTestIssue14(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> initialDate = new FieldAbsoluteDate<>(field);
        final FieldKeplerianOrbit<T> initialOrbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                                      FramesFactory.getEME2000(), initialDate, zero.add(3.986004415e14));
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(initialOrbit);

        propagator.setEphemerisMode();
        propagator.propagate(initialDate.shiftedBy(initialOrbit.getKeplerianPeriod()));
        FieldPVCoordinates<T> pv1 = propagator.getPVCoordinates(initialDate, FramesFactory.getEME2000());

        propagator.setEphemerisMode();
        propagator.propagate(initialDate.shiftedBy(initialOrbit.getKeplerianPeriod()));
        FieldPVCoordinates<T> pv2 = propagator.getGeneratedEphemeris().getPVCoordinates(initialDate, FramesFactory.getEME2000());

        Assert.assertEquals(0.0, pv1.getPosition().subtract(pv2.getPosition()).getNorm().getReal(), 1.0e-15);
        Assert.assertEquals(0.0, pv1.getVelocity().subtract(pv2.getVelocity()).getNorm().getReal(), 1.0e-15);

    }

    private <T extends RealFieldElement<T>> void doTestIssue107(Field<T> field) {
        T zero = field.getZero();
        final TimeScale utc = TimeScalesFactory.getUTC();
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(-6142438.668), zero.add(3492467.56), zero.add(-25767.257));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(505.848), zero.add( 942.781), zero.add(7435.922));
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2003, 9, 16, utc);
        final FieldOrbit<T> orbit = new FieldCircularOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                             FramesFactory.getEME2000(), date, zero.add(mu));

        FieldPropagator<T> propagator = new FieldKeplerianPropagator<T>(orbit) {
            FieldAbsoluteDate<T> lastDate = FieldAbsoluteDate.getPastInfinity(field);

            protected FieldSpacecraftState<T> basicPropagate(final FieldAbsoluteDate<T> date) {
                if (date.compareTo(lastDate) < 0) {
                    throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                                   "no backward propagation allowed");
                }
                lastDate = date;
                return super.basicPropagate(date);
            }
        };

        FieldSpacecraftState<T> finalState = propagator.propagate(date.shiftedBy(3600.0));
        Assert.assertEquals(3600.0, finalState.getDate().durationFrom(date).getReal(), 1.0e-15);

    }

    private <T extends RealFieldElement<T>> void doTestMu(Field<T> field) {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit1 =
                new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                                          FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field),
                                          zero.add(Constants.WGS84_EARTH_MU));
        final FieldKeplerianOrbit<T> orbit2 =
                new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                                          FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field),
                                          zero.add(Constants.EIGEN5C_EARTH_MU));
        final FieldAbsoluteDate<T> target = orbit1.getDate().shiftedBy(10000.0);
        FieldPVCoordinates<T> pv1       = new FieldKeplerianPropagator<>(orbit1).propagate(target).getPVCoordinates();
        FieldPVCoordinates<T> pv2       = new FieldKeplerianPropagator<>(orbit2).propagate(target).getPVCoordinates();
        FieldPVCoordinates<T> pvWithMu1 = new FieldKeplerianPropagator<>(orbit2, orbit1.getMu()).propagate(target).getPVCoordinates();
        Assert.assertEquals(0.026054, FieldVector3D.distance(pv1.getPosition(), pv2.getPosition()).getReal(),       1.0e-6);
        Assert.assertEquals(0.0,      FieldVector3D.distance(pv1.getPosition(), pvWithMu1.getPosition()).getReal(), 1.0e-15);
    }

    private <T extends RealFieldElement<T>> void doTestNoDerivatives(Field<T> field) {
        T zero = field.getZero();
        for (OrbitType type : OrbitType.values()) {

            // create an initial orbit with non-Keplerian acceleration
            final FieldAbsoluteDate<T> date         = new FieldAbsoluteDate<>(field, 2003, 9, 16, TimeScalesFactory.getUTC());
            final FieldVector3D<T>     position     = new FieldVector3D<>(zero.add(-6142438.668),
                                                                          zero.add(3492467.56),
                                                                          zero.add(-25767.257));
            final FieldVector3D<T>     velocity     = new FieldVector3D<>(zero.add(505.848),
                                                                          zero.add(942.781),
                                                                          zero.add(7435.922));
            final FieldVector3D<T>     keplerAcceleration = new FieldVector3D<>(position.getNormSq().reciprocal().multiply(zero.add(mu).negate()),
                                                                               position.normalize());
            final FieldVector3D<T>     nonKeplerAcceleration = new FieldVector3D<>(zero.add(0.001),
                                                                                   zero.add(0.002),
                                                                                   zero.add(0.003));
            final FieldVector3D<T>     acceleration = keplerAcceleration.add(nonKeplerAcceleration);
            final TimeStampedFieldPVCoordinates<T> pva = new TimeStampedFieldPVCoordinates<>(date, position, velocity, acceleration);
            final FieldOrbit<T> initial = type.convertType(new FieldCartesianOrbit<>(pva, FramesFactory.getEME2000(), zero.add(mu)));
            Assert.assertEquals(type, initial.getType());

            // the derivatives are available at this stage
            checkDerivatives(initial, true);

            FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(initial);
            Assert.assertEquals(type, propagator.getInitialState().getOrbit().getType());

            // non-Keplerian derivatives are explicitly removed when building the Keplerian-only propagator
            checkDerivatives(propagator.getInitialState().getOrbit(), false);

            FieldPVCoordinates<T> initPV = propagator.getInitialState().getOrbit().getPVCoordinates();
            Assert.assertEquals(nonKeplerAcceleration.getNorm().getReal(),
                                FieldVector3D.distance(acceleration, initPV.getAcceleration()).getReal(),
                                2.0e-15);
            Assert.assertEquals(0.0,
                                FieldVector3D.distance(keplerAcceleration, initPV.getAcceleration()).getReal(),
                                5.0e-15);

            T dt = initial.getKeplerianPeriod().multiply(0.2);
            FieldOrbit<T> orbit = propagator.propagateOrbit(initial.getDate().shiftedBy(dt));
            Assert.assertEquals(type, orbit.getType());

            // at the end, we don't have non-Keplerian derivatives
            checkDerivatives(orbit, false);

            // using shiftedBy on the initial orbit, non-Keplerian derivatives would have been preserved
            checkDerivatives(initial.shiftedBy(dt), true);

        }
    }

    private <T extends RealFieldElement<T>> void checkDerivatives(final FieldOrbit<T> orbit,
                                                                  final boolean expectedDerivatives) {
        Assert.assertEquals(expectedDerivatives, orbit.hasDerivatives());
        if (expectedDerivatives) {
            Assert.assertNotNull(orbit.getADot());
            Assert.assertNotNull(orbit.getEquinoctialExDot());
            Assert.assertNotNull(orbit.getEquinoctialEyDot());
            Assert.assertNotNull(orbit.getHxDot());
            Assert.assertNotNull(orbit.getHyDot());
            Assert.assertNotNull(orbit.getLEDot());
            Assert.assertNotNull(orbit.getLvDot());
            Assert.assertNotNull(orbit.getLMDot());
            Assert.assertNotNull(orbit.getEDot());
            Assert.assertNotNull(orbit.getIDot());
        } else {
            Assert.assertNull(orbit.getADot());
            Assert.assertNull(orbit.getEquinoctialExDot());
            Assert.assertNull(orbit.getEquinoctialEyDot());
            Assert.assertNull(orbit.getHxDot());
            Assert.assertNull(orbit.getHyDot());
            Assert.assertNull(orbit.getLEDot());
            Assert.assertNull(orbit.getLvDot());
            Assert.assertNull(orbit.getLMDot());
            Assert.assertNull(orbit.getEDot());
            Assert.assertNull(orbit.getIDot());
        }
    }

    private <T extends RealFieldElement<T>> T tangLEmLv(T Lv, T ex, T ey){
        // tan ((LE - Lv) /2)) =
        return ey.multiply(Lv.cos()).subtract(ex.multiply(Lv.sin())).divide(
               ex.multiply(Lv.cos()).add(1.).add(ey.multiply(Lv.sin())).add( ex.multiply(ex).negate().add(1.).subtract(ey.multiply(ey)).sqrt()));
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        mu  = 3.9860047e14;
    }

    @After
    public void tearDown() {
        mu   = Double.NaN;
    }

}

