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
package org.orekit.propagation.analytical;

import org.hamcrest.MatcherAssert;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.stat.descriptive.rank.Max;
import org.hipparchus.stat.descriptive.rank.Min;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
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
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldBoundedPropagator;
import org.orekit.propagation.FieldEphemerisGenerator;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.FieldAbstractDetector;
import org.orekit.propagation.events.FieldAltitudeDetector;
import org.orekit.propagation.events.FieldApsideDetector;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldElevationDetector;
import org.orekit.propagation.events.FieldNodeDetector;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.propagation.sampling.FieldOrekitFixedStepHandler;
import org.orekit.propagation.sampling.FieldOrekitStepHandler;
import org.orekit.propagation.sampling.FieldOrekitStepInterpolator;
import org.orekit.propagation.sampling.FieldStepHandlerMultiplexer;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
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
                                                6.2, PositionAngleType.TRUE,
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
                                                  PositionAngleType.MEAN,
                                                  FramesFactory.getEME2000(),
                                                  new FieldAbsoluteDate<>(initDate, new Tuple(0.0, 0.0)),
                                                  new Tuple(mu, mu));
        Field<Tuple> field = twoOrbits.getDate().getField();
        FieldPropagator<Tuple> propagator = new FieldKeplerianPropagator<>(twoOrbits);
        Min minTangential = new Min();
        Max maxTangential = new Max();
        Min minRadial     = new Min();
        Max maxRadial     = new Max();
        propagator.setStepHandler(field.getZero().add(10.0),
                                 s -> {
                                     FieldVector3D<Tuple> p = s.getPosition();
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
        Assertions.assertEquals(-72525.685, minTangential.getResult(), 1.0e-3);
        Assertions.assertEquals(   926.046, maxTangential.getResult(), 1.0e-3);
        Assertions.assertEquals(   -92.800, minRadial.getResult(),     1.0e-3);
        Assertions.assertEquals(  7739.532, maxRadial.getResult(),     1.0e-3);

    }

    @Test
    public void testSameDateCartesian() {
        doTestSameDateCartesian(Binary64Field.getInstance());
    }


    @Test
    public void testSameDateKeplerian() {
        doTestSameDateKeplerian(Binary64Field.getInstance());
    }


    @Test
    public void testPropagatedCartesian() {
        doTestPropagatedCartesian(Binary64Field.getInstance());
    }


    @Test
    public void testPropagatedKeplerian() {
        doTestPropagatedKeplerian(Binary64Field.getInstance());
    }


    @Test
    public void testAscendingNode() {
        doTestAscendingNode(Binary64Field.getInstance());
    }


    @Test
    public void testStopAtTargetDate() {
        doTestStopAtTargetDate(Binary64Field.getInstance());
    }


    @Test
    public void testFixedStep() {
        doTestFixedStep(Binary64Field.getInstance());
    }


    @Test
    public void testVariableStep() {
        doTestVariableStep(Binary64Field.getInstance());
    }


    @Test
    public void testEphemeris() {
        doTestEphemeris(Binary64Field.getInstance());
    }


    @Test
    public void testAdditionalState() {
        doTestAdditionalState(Binary64Field.getInstance());
    }


    @Test
    public void testIssue14() {
        doTestIssue14(Binary64Field.getInstance());
    }


    @Test
    public void testIssue107() {
        doTestIssue107(Binary64Field.getInstance());
    }


    @Test
    public void testMu() {
        doTestMu(Binary64Field.getInstance());
    }

    @Test
    public void testNoDerivatives() {
        doTestNoDerivatives(Binary64Field.getInstance());
    }

    @Test
    public void testWrongAttitude() {
        Assertions.assertThrows(OrekitException.class, () -> {
            doTestWrongAttitude(Binary64Field.getInstance());
        });
    }

    @Test
    public void testStepException() {
        Assertions.assertThrows(OrekitException.class, () -> {
            doTestStepException(Binary64Field.getInstance());
        });
    }

    @Test
    public void testWrappedAttitudeException() {
        Assertions.assertThrows(OrekitException.class, () -> {
            doTestWrappedAttitudeException(Binary64Field.getInstance());
        });
    }

    @Test
    public void testPerigee() {
        doTestPerigee(Binary64Field.getInstance());
    }

    @Test
    public void testAltitude() {
        doTestAltitude(Binary64Field.getInstance());
    }

    @Test
    public void testDate() {
        doTestDate(Binary64Field.getInstance());
    }

    @Test
    public void testSetting() {
        doTestSetting(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestSameDateCartesian(Field<T> field) {
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

        Assertions.assertEquals(n.getReal()*delta_t.getReal(),
                            finalOrbit.getLM().getReal() - initialOrbit.getLM().getReal(),
                            Utils.epsilonTest * FastMath.abs(n.getReal()*delta_t.getReal()));
        Assertions.assertEquals(MathUtils.normalizeAngle(finalOrbit.getLM().getReal(), initialOrbit.getLM().getReal()), initialOrbit.getLM().getReal(),
                            Utils.epsilonAngle * FastMath.abs(initialOrbit.getLM().getReal()));

        Assertions.assertEquals(finalOrbit.getA().getReal(), initialOrbit.getA().getReal(),
                            Utils.epsilonTest * initialOrbit.getA().getReal());
        Assertions.assertEquals(finalOrbit.getE().getReal(), initialOrbit.getE().getReal(),
                            Utils.epsilonE * initialOrbit.getE().getReal());
        Assertions.assertEquals(MathUtils.normalizeAngle(finalOrbit.getI().getReal(), initialOrbit.getI().getReal()),
                            initialOrbit.getI().getReal(), Utils.epsilonAngle * FastMath.abs(initialOrbit.getI().getReal()));

    }

    private <T extends CalculusFieldElement<T>> void doTestSameDateKeplerian(Field<T> field) {
        T zero = field.getZero();
        // Definition of initial conditions with Keplerian parameters
        //-----------------------------------------------------------
        FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field).shiftedBy(584.);
        FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(zero.add(7209668.0), zero.add(0.5e-4), zero.add(1.7), zero.add(2.1), zero.add(2.9),
                                                               zero.add(6.2), PositionAngleType.TRUE,
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

        Assertions.assertEquals(n.getReal()*delta_t.getReal(),
                     finalOrbit.getLM().getReal() - initialOrbit.getLM().getReal(),
                     Utils.epsilonTest * FastMath.max(100., FastMath.abs(n.getReal()*delta_t.getReal())));
        Assertions.assertEquals(MathUtils.normalizeAngle(finalOrbit.getLM().getReal(), initialOrbit.getLM().getReal()),
                            initialOrbit.getLM().getReal(), Utils.epsilonAngle * FastMath.abs(initialOrbit.getLM().getReal()));

        Assertions.assertEquals(finalOrbit.getA().getReal(), initialOrbit.getA().getReal(),
                            Utils.epsilonTest * initialOrbit.getA().getReal());
        Assertions.assertEquals(finalOrbit.getE().getReal(), initialOrbit.getE().getReal(),
                            Utils.epsilonE * initialOrbit.getE().getReal());
        Assertions.assertEquals(MathUtils.normalizeAngle(finalOrbit.getI().getReal(), initialOrbit.getI().getReal()),
                            initialOrbit.getI().getReal(),
                            Utils.epsilonAngle * FastMath.abs(initialOrbit.getI().getReal()));

    }

    private <T extends CalculusFieldElement<T>> void doTestPropagatedCartesian(Field<T> field) {
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

        Assertions.assertEquals(n.getReal() * delta_t.getReal(),
                            finalOrbit.getLM().getReal() - initialOrbit.getLM().getReal(),
                            Utils.epsilonAngle);

        // computation of M final orbit
        T LM = finalOrbit.getLE().subtract(
        finalOrbit.getEquinoctialEx().multiply(finalOrbit.getLE().sin())).add(
        finalOrbit.getEquinoctialEy().multiply(finalOrbit.getLE().cos()));

        Assertions.assertEquals(LM.getReal() , finalOrbit.getLM().getReal() , Utils.epsilonAngle);

        // test of tan ((LE - Lv)/2) :
        Assertions.assertEquals(FastMath.tan((finalOrbit.getLE().getReal() - finalOrbit.getLv().getReal())/2.),
                            tangLEmLv(finalOrbit.getLv(), finalOrbit.getEquinoctialEx(), finalOrbit.getEquinoctialEy()).getReal(),
                            Utils.epsilonAngle);

        // test of evolution of M vs E: LM = LE - ex*sin(LE) + ey*cos(LE)
        // with ex and ey the same for initial and final orbit
        T deltaM = finalOrbit.getLM().subtract(initialOrbit.getLM());
        T deltaE = finalOrbit.getLE().subtract(initialOrbit.getLE());
        T delta  = finalOrbit.getEquinoctialEx().multiply(finalOrbit.getLE().sin().subtract(initialOrbit.getLE().sin())).subtract(
                   finalOrbit.getEquinoctialEy().multiply(finalOrbit.getLE().cos().subtract(initialOrbit.getLE().cos())));

        Assertions.assertEquals(deltaM.getReal(), deltaE.getReal() - delta.getReal(), Utils.epsilonAngle);

        // the orbital elements except for Mean/True/Eccentric latitude arguments are the same
        Assertions.assertEquals(finalOrbit.getA().getReal(), initialOrbit.getA().getReal(), Utils.epsilonTest * initialOrbit.getA().getReal());
        Assertions.assertEquals(finalOrbit.getEquinoctialEx().getReal(), initialOrbit.getEquinoctialEx().getReal(), Utils.epsilonE);
        Assertions.assertEquals(finalOrbit.getEquinoctialEy().getReal(), initialOrbit.getEquinoctialEy().getReal(), Utils.epsilonE);
        Assertions.assertEquals(finalOrbit.getHx().getReal(), initialOrbit.getHx().getReal(), Utils.epsilonAngle);
        Assertions.assertEquals(finalOrbit.getHy().getReal(), initialOrbit.getHy().getReal(), Utils.epsilonAngle);

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

        Assertions.assertEquals(finalOrbit.getPosition().getNorm().getReal(), r.getNorm().getReal(), Utils.epsilonTest * r.getNorm().getReal());

    }

    private <T extends CalculusFieldElement<T>> void doTestPropagatedKeplerian(Field<T> field) {
        T zero = field.getZero();
        // Definition of initial conditions with Keplerian parameters
        //-----------------------------------------------------------
        FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field).shiftedBy(584.);
        FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(zero.add(7209668.0), zero.add(0.5e-4), zero.add(1.7), zero.add(2.1), zero.add(2.9),
                                                               zero.add(6.2), PositionAngleType.TRUE,
                                                               FramesFactory.getEME2000(), initDate, zero.add(mu));

        // Extrapolator definition
        // -----------------------
        FieldKeplerianPropagator<T> extrapolator = new FieldKeplerianPropagator<>(initialOrbit);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        T delta_t = zero.add(100000.0); // extrapolation duration in seconds
        FieldAbsoluteDate<T> extrapDate = initDate.shiftedBy(delta_t);

        FieldSpacecraftState<T> finalOrbit = extrapolator.propagate(extrapDate);
        Assertions.assertEquals(6092.3362422560844633, finalOrbit.getKeplerianPeriod().getReal(), 1.0e-12);
        Assertions.assertEquals(0.001031326088602888358, finalOrbit.getKeplerianMeanMotion().getReal(), 1.0e-16);

        // computation of (M final - M initial) with another method
        T a = finalOrbit.getA();
        // another way to compute n
        T n = a.pow(3).reciprocal().multiply(finalOrbit.getMu()).sqrt();

        Assertions.assertEquals(n.getReal() * delta_t.getReal(),
                     finalOrbit.getLM().getReal() - initialOrbit.getLM().getReal(),
                     Utils.epsilonAngle);

        // computation of M final orbit
        T LM = finalOrbit.getLE().subtract(
               finalOrbit.getEquinoctialEx().multiply(finalOrbit.getLE().sin())).add(
               finalOrbit.getEquinoctialEy().multiply(finalOrbit.getLE().cos()));

        Assertions.assertEquals(LM.getReal() , finalOrbit.getLM().getReal() , Utils.epsilonAngle);

        // test of tan ((LE - Lv)/2) :
        Assertions.assertEquals(FastMath.tan((finalOrbit.getLE().getReal() - finalOrbit.getLv().getReal())/2.),
                     tangLEmLv(finalOrbit.getLv(), finalOrbit.getEquinoctialEx(), finalOrbit.getEquinoctialEy()).getReal(),
                     Utils.epsilonAngle);

        // test of evolution of M vs E: LM = LE - ex*sin(LE) + ey*cos(LE)
        // with ex and ey the same for initial and final orbit
        T deltaM = finalOrbit.getLM().subtract(initialOrbit.getLM());
        T deltaE = finalOrbit.getLE().subtract(initialOrbit.getLE());
        T delta  = finalOrbit.getEquinoctialEx().multiply(finalOrbit.getLE().sin().subtract(initialOrbit.getLE().sin())).subtract(
                   finalOrbit.getEquinoctialEy().multiply(finalOrbit.getLE().cos().subtract(initialOrbit.getLE().cos())));

        Assertions.assertEquals(deltaM.getReal(), deltaE.getReal() - delta.getReal(), Utils.epsilonAngle);

        // the orbital elements except for Mean/True/Eccentric latitude arguments are the same
        Assertions.assertEquals(finalOrbit.getA().getReal(), initialOrbit.getA().getReal(), Utils.epsilonTest * initialOrbit.getA().getReal());
        Assertions.assertEquals(finalOrbit.getEquinoctialEx().getReal(), initialOrbit.getEquinoctialEx().getReal(), Utils.epsilonE);
        Assertions.assertEquals(finalOrbit.getEquinoctialEy().getReal(), initialOrbit.getEquinoctialEy().getReal(), Utils.epsilonE);
        Assertions.assertEquals(finalOrbit.getHx().getReal(), initialOrbit.getHx().getReal(), Utils.epsilonAngle);
        Assertions.assertEquals(finalOrbit.getHy().getReal(), initialOrbit.getHy().getReal(), Utils.epsilonAngle);

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

        Assertions.assertEquals(finalOrbit.getPosition().getNorm().getReal(), r.getNorm().getReal(), Utils.epsilonTest * r.getNorm().getReal());

    }

    private <T extends CalculusFieldElement<T>> void doTestWrongAttitude(Field<T> field) {
        T zero = field.getZero();
        FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(1.0e10), zero.add(1.0e-4), zero.add(1.0e-2), zero, zero, zero, PositionAngleType.TRUE,
                                      FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field), zero.add(3.986004415e14));
        AttitudeProvider wrongLaw = new AttitudeProvider() {
            @Override
            public Attitude getAttitude(PVCoordinatesProvider pvProv, AbsoluteDate date, Frame frame) {
                throw new OrekitException(new DummyLocalizable("gasp"), new RuntimeException());
            }
            @Override
            public <Q extends CalculusFieldElement<Q>> FieldAttitude<Q> getAttitude(FieldPVCoordinatesProvider<Q> pvProv,
                                                                                FieldAbsoluteDate<Q> date,
                                                                                Frame frame) {
                throw new OrekitException(new DummyLocalizable("gasp"), new RuntimeException());
            }
        };
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit, wrongLaw);
        propagator.propagate(new FieldAbsoluteDate<>(field).shiftedBy(10.0));
    }

    private <T extends CalculusFieldElement<T>> void doTestStepException(Field<T> field) {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngleType.TRUE,
                                      FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field), zero.add(3.986004415e14));
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);
        FieldStepHandlerMultiplexer<T> multiplexer = new FieldStepHandlerMultiplexer<>();
        propagator.setStepHandler(multiplexer);
        multiplexer.add(new FieldOrekitStepHandler<T>() {
            @Override
            public void init(FieldSpacecraftState<T> s0, FieldAbsoluteDate<T> t) {
            }
            @Override
            public void handleStep(FieldOrekitStepInterpolator<T> interpolator) {
            }
            @Override
            public void finish(FieldSpacecraftState<T> finalState) {
                throw new OrekitException((Throwable) null, new DummyLocalizable("dummy error"));
            }
        });

        propagator.propagate(orbit.getDate().shiftedBy(-3600));

    }

    private <T extends CalculusFieldElement<T>> void doTestWrappedAttitudeException(Field<T> field) {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngleType.TRUE,
                                      FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field), zero.add(3.986004415e14));
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit,
                        new AttitudeProvider() {
            @Override
            public Attitude getAttitude(PVCoordinatesProvider pvProv, AbsoluteDate date, Frame frame) {
                throw new OrekitException((Throwable) null, new DummyLocalizable("dummy error"));
            }
            @Override
            public <Q extends CalculusFieldElement<Q>> FieldAttitude<Q> getAttitude(FieldPVCoordinatesProvider<Q> pvProv,
                                                                                FieldAbsoluteDate<Q> date,
                                                                                Frame frame) {
                throw new OrekitException((Throwable) null, new DummyLocalizable("dummy error"));
            }
        });
        propagator.propagate(orbit.getDate().shiftedBy(10.09));
    }

    private <T extends CalculusFieldElement<T>> void doTestAscendingNode(Field<T> field) {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngleType.TRUE,
                                      FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field), zero.add(3.986004415e14));
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);
        propagator.addEventDetector(new FieldNodeDetector<>(orbit, FramesFactory.getITRF(IERSConventions.IERS_2010, true)));
        FieldAbsoluteDate<T> farTarget = new FieldAbsoluteDate<>(field).shiftedBy(10000.0);
        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);
        FieldPVCoordinates<T> pv = propagated.getPVCoordinates(FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() > 3500.0);
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() < 4000.0);
        Assertions.assertEquals(0, pv.getPosition().getZ().getReal(), 2.0e-6);
        Assertions.assertTrue(pv.getVelocity().getZ().getReal() > 0);
    }

    private <T extends CalculusFieldElement<T>> void doTestStopAtTargetDate(Field<T> field) {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngleType.TRUE,
                                      FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field), zero.add(3.986004415e14));
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        propagator.addEventDetector(new FieldNodeDetector<>(orbit, itrf).withHandler(new FieldContinueOnEvent<>()));
        FieldAbsoluteDate<T> farTarget = orbit.getDate().shiftedBy(10000.0);
        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);
        Assertions.assertEquals(0.0, FastMath.abs(farTarget.durationFrom(propagated.getDate()).getReal()), 1.0e-3);
    }

    private <T extends CalculusFieldElement<T>> void doTestPerigee(Field<T> field) {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngleType.TRUE,
                                      FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field), zero.add(3.986004415e14));
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);
        propagator.addEventDetector(new FieldApsideDetector<>(orbit));
        FieldAbsoluteDate<T> farTarget = new FieldAbsoluteDate<>(field).shiftedBy(10000.0);
        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);
        FieldVector3D<T> position = propagated.getPosition(FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() > 3000.0);
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() < 3500.0);
        Assertions.assertEquals(orbit.getA().getReal() * (1.0 - orbit.getE().getReal()), position.getNorm().getReal(), 1.0e-6);
    }

    private <T extends CalculusFieldElement<T>> void doTestAltitude(Field<T> field) {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngleType.TRUE,
                                      FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field), zero.add(3.986004415e14));
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);
        BodyShape bodyShape =
            new OneAxisEllipsoid(6378137.0, 1.0 / 298.257222101, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        FieldAltitudeDetector<T> detector =
            new FieldAltitudeDetector<>(orbit.getKeplerianPeriod().multiply(0.05),
                                        zero.add(1500000), bodyShape);
        Assertions.assertEquals(1500000, detector.getAltitude().getReal(), 1.0e-12);
        propagator.addEventDetector(detector);
        FieldAbsoluteDate<T> farTarget = new FieldAbsoluteDate<>(field).shiftedBy(10000.0);
        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() > 5400.0);
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() < 5500.0);
        FieldGeodeticPoint<T> gp = bodyShape.transform(propagated.getPosition(),
                                                       propagated.getFrame(), propagated.getDate());
        Assertions.assertEquals(1500000, gp.getAltitude().getReal(), 0.1);
    }

    private <T extends CalculusFieldElement<T>> void doTestDate(Field<T> field) {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngleType.TRUE,
                                      FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field), zero.add(3.986004415e14));
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);
        final FieldAbsoluteDate<T> stopDate = new FieldAbsoluteDate<>(field).shiftedBy(500.0);
        @SuppressWarnings("unchecked")
        FieldDateDetector<T> detector = new FieldDateDetector<>(field, stopDate);
        propagator.addEventDetector(detector);
        FieldAbsoluteDate<T> farTarget = new FieldAbsoluteDate<>(field).shiftedBy(10000.0);
        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);
        Assertions.assertEquals(0, stopDate.durationFrom(propagated.getDate()).getReal(), 1.0e-10);
    }

    private <T extends CalculusFieldElement<T>> void doTestSetting(Field<T> field) {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngleType.TRUE,
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
        final double elevation = topo.
                                 getTrackingCoordinates(propagated.getPosition().toVector3D(),
                                                        propagated.getFrame(),
                                                        propagated.getDate().toAbsoluteDate()).
                                 getElevation();
        final T zVelocity = propagated.getPVCoordinates(topo).getVelocity().getZ();
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() > 7800.0);
        Assertions.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() < 7900.0);
        Assertions.assertEquals(0.09, elevation, 1.0e-9);
        Assertions.assertTrue(zVelocity.getReal() < 0);
    }

    private <T extends CalculusFieldElement<T>> void doTestFixedStep(Field<T> field) {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngleType.TRUE,
                                      FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field), zero.add(3.986004415e14));
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);
        final T step = zero.add(100.0);
        final int[] counter = new int[] {0};  // mutable int
        propagator.setStepHandler(step, new FieldOrekitFixedStepHandler<T>() {
            private FieldAbsoluteDate<T> previous;
            @Override
            public void handleStep(FieldSpacecraftState<T> currentState) {
                if (previous != null) {
                    Assertions.assertEquals(step.getReal(), currentState.getDate().durationFrom(previous).getReal(), 1.0e-10);
                }
                // check state is accurate
                FieldPVCoordinates<T> expected = new FieldKeplerianPropagator<>(orbit)
                        .propagate(currentState.getDate()).getPVCoordinates();
                MatcherAssert.assertThat(
                        currentState.getPVCoordinates().toTimeStampedPVCoordinates(),
                        OrekitMatchers.pvIs(expected.toPVCoordinates()));
                previous = currentState.getDate();
                counter[0]++;
            }
        });
        FieldAbsoluteDate<T> farTarget = new FieldAbsoluteDate<>(field).shiftedBy(10000.0);
        propagator.propagate(farTarget);
        // check the step handler was executed
        Assertions.assertEquals(
                counter[0],
                (int) (farTarget.durationFrom(orbit.getDate()).getReal() / step.getReal()) + 1);
    }

    private <T extends CalculusFieldElement<T>> void doTestVariableStep(Field<T> field) {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngleType.TRUE,
                                      FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field), zero.add(3.986004415e14));
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);
        final T step = orbit.getKeplerianPeriod().divide(100);
        final int[] counter = new int[] {0};  // mutable int
        propagator.setStepHandler(new FieldOrekitStepHandler<T>() {
            private FieldAbsoluteDate<T> t = orbit.getDate();
            @Override
            public void handleStep(FieldOrekitStepInterpolator<T> interpolator) {
                // check the states provided by the interpolator are accurate.
                do {
                    PVCoordinates expected = new FieldKeplerianPropagator<>(orbit)
                            .propagate(t).getPVCoordinates().toTimeStampedPVCoordinates();
                    MatcherAssert.assertThat(
                            interpolator.getInterpolatedState(t).getPVCoordinates()
                                    .toTimeStampedPVCoordinates(),
                            OrekitMatchers.pvIs(expected));
                    t = t.shiftedBy(step);
                    counter[0]++;
                } while (t.compareTo(interpolator.getCurrentState().getDate()) <= 0);
            }
        });
        FieldAbsoluteDate<T> farTarget = new FieldAbsoluteDate<>(field).shiftedBy(10000.0);
        propagator.propagate(farTarget);
        // check the step handler was executed
        Assertions.assertEquals(
                counter[0],
                (int) (farTarget.durationFrom(orbit.getDate()).getReal() / step.getReal()) + 1);
    }

    private <T extends CalculusFieldElement<T>> void doTestEphemeris(Field<T> field) {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngleType.TRUE,
                                      FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field), zero.add(3.986004415e14));
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);
        FieldAbsoluteDate<T> farTarget = new FieldAbsoluteDate<>(field).shiftedBy(10000.0);
        final FieldEphemerisGenerator<T> generator = propagator.getEphemerisGenerator();
        propagator.propagate(farTarget);
        FieldBoundedPropagator<T> ephemeris = generator.getGeneratedEphemeris();
        Assertions.assertEquals(0.0, ephemeris.getMinDate().durationFrom(orbit.getDate()).getReal(), 1.0e-10);
        Assertions.assertEquals(0.0, ephemeris.getMaxDate().durationFrom(farTarget).getReal(), 1.0e-10);
    }

    private <T extends CalculusFieldElement<T>> void doTestAdditionalState(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> initDate = FieldAbsoluteDate.getGPSEpoch(field);
        FieldOrbit<T> ic = new FieldKeplerianOrbit<>(zero.newInstance(6378137 + 500e3), zero.newInstance(1e-3),
                                                     zero, zero, zero, zero, PositionAngleType.TRUE,
                                                     FramesFactory.getGCRF(), initDate, zero.newInstance(mu));
        FieldPropagator<T> propagator = new FieldKeplerianPropagator<>(ic);
        FieldSpacecraftState<T> initialState = propagator.getInitialState().addAdditionalState("myState", zero.newInstance(4.2));
        propagator.resetInitialState(initialState);
        FieldAbsoluteDate<T> end = initDate.shiftedBy(90 * 60);
        FieldEphemerisGenerator<T> generator = propagator.getEphemerisGenerator();
        FieldSpacecraftState<T> finalStateKeplerianPropagator = propagator.propagate(end);
        FieldBoundedPropagator<T> ephemeris = generator.getGeneratedEphemeris();
        FieldSpacecraftState<T> ephemerisInitialState = ephemeris.getInitialState();
        FieldSpacecraftState<T> finalStateBoundedPropagator = ephemeris.propagate(end);
        Assertions.assertEquals(4.2, finalStateKeplerianPropagator.getAdditionalState("myState")[0].getReal(), 1.0e-15);
        Assertions.assertEquals(4.2, ephemerisInitialState.getAdditionalState("myState")[0].getReal(), 1.0e-15);
        Assertions.assertEquals(4.2, finalStateBoundedPropagator.getAdditionalState("myState")[0].getReal(), 1.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue14(Field<T> field) {
        T zero = field.getZero();
        FieldAbsoluteDate<T> initialDate = new FieldAbsoluteDate<>(field);
        final FieldKeplerianOrbit<T> initialOrbit =
            new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngleType.TRUE,
                                      FramesFactory.getEME2000(), initialDate, zero.add(3.986004415e14));
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(initialOrbit);

        propagator.propagate(initialDate.shiftedBy(initialOrbit.getKeplerianPeriod()));
        FieldPVCoordinates<T> pv1 = propagator.getPVCoordinates(initialDate, FramesFactory.getEME2000());

        final FieldEphemerisGenerator<T> generator = propagator.getEphemerisGenerator();
        propagator.propagate(initialDate.shiftedBy(initialOrbit.getKeplerianPeriod()));
        FieldPVCoordinates<T> pv2 = generator.getGeneratedEphemeris().getPVCoordinates(initialDate, FramesFactory.getEME2000());

        Assertions.assertEquals(0.0, pv1.getPosition().subtract(pv2.getPosition()).getNorm().getReal(), 1.0e-15);
        Assertions.assertEquals(0.0, pv1.getVelocity().subtract(pv2.getVelocity()).getNorm().getReal(), 1.0e-15);

    }

    private <T extends CalculusFieldElement<T>> void doTestIssue107(Field<T> field) {
        T zero = field.getZero();
        final TimeScale utc = TimeScalesFactory.getUTC();
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(-6142438.668), zero.add(3492467.56), zero.add(-25767.257));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(505.848), zero.add( 942.781), zero.add(7435.922));
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2003, 9, 16, utc);
        final FieldOrbit<T> orbit = new FieldCircularOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                             FramesFactory.getEME2000(), date, zero.add(mu));

        FieldPropagator<T> propagator = new FieldKeplerianPropagator<T>(orbit) {
            FieldAbsoluteDate<T> lastDate = FieldAbsoluteDate.getPastInfinity(field);

            @Override
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
        Assertions.assertEquals(3600.0, finalState.getDate().durationFrom(date).getReal(), 1.0e-15);

    }

    private <T extends CalculusFieldElement<T>> void doTestMu(Field<T> field) {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit1 =
                new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngleType.TRUE,
                                          FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field),
                                          zero.add(Constants.WGS84_EARTH_MU));
        final FieldKeplerianOrbit<T> orbit2 =
                new FieldKeplerianOrbit<>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngleType.TRUE,
                                          FramesFactory.getEME2000(), new FieldAbsoluteDate<>(field),
                                          zero.add(Constants.EIGEN5C_EARTH_MU));
        final FieldAbsoluteDate<T> target = orbit1.getDate().shiftedBy(10000.0);
        FieldPVCoordinates<T> pv1       = new FieldKeplerianPropagator<>(orbit1).propagate(target).getPVCoordinates();
        FieldPVCoordinates<T> pv2       = new FieldKeplerianPropagator<>(orbit2).propagate(target).getPVCoordinates();
        FieldPVCoordinates<T> pvWithMu1 = new FieldKeplerianPropagator<>(orbit2, orbit1.getMu()).propagate(target).getPVCoordinates();
        Assertions.assertEquals(0.026054, FieldVector3D.distance(pv1.getPosition(), pv2.getPosition()).getReal(),       1.0e-6);
        Assertions.assertEquals(0.0,      FieldVector3D.distance(pv1.getPosition(), pvWithMu1.getPosition()).getReal(), 1.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void doTestNoDerivatives(Field<T> field) {
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
            Assertions.assertEquals(type, initial.getType());

            // the derivatives are available at this stage
            checkDerivatives(initial, true);

            FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(initial);
            Assertions.assertEquals(type, propagator.getInitialState().getOrbit().getType());

            // non-Keplerian derivatives are explicitly removed when building the Keplerian-only propagator
            checkDerivatives(propagator.getInitialState().getOrbit(), false);

            FieldPVCoordinates<T> initPV = propagator.getInitialState().getOrbit().getPVCoordinates();
            Assertions.assertEquals(nonKeplerAcceleration.getNorm().getReal(),
                                FieldVector3D.distance(acceleration, initPV.getAcceleration()).getReal(),
                                2.0e-15);
            Assertions.assertEquals(0.0,
                                FieldVector3D.distance(keplerAcceleration, initPV.getAcceleration()).getReal(),
                                5.0e-15);

            T dt = initial.getKeplerianPeriod().multiply(0.2);
            FieldOrbit<T> orbit = propagator.propagateOrbit(initial.getDate().shiftedBy(dt), null);
            Assertions.assertEquals(type, orbit.getType());

            // at the end, we don't have non-Keplerian derivatives
            checkDerivatives(orbit, false);

            // using shiftedBy on the initial orbit, non-Keplerian derivatives would have been preserved
            checkDerivatives(initial.shiftedBy(dt), true);

        }
    }

    private <T extends CalculusFieldElement<T>> void checkDerivatives(final FieldOrbit<T> orbit,
                                                                  final boolean expectedDerivatives) {
        Assertions.assertEquals(expectedDerivatives, orbit.hasDerivatives());
        if (expectedDerivatives) {
            Assertions.assertNotNull(orbit.getADot());
            Assertions.assertNotNull(orbit.getEquinoctialExDot());
            Assertions.assertNotNull(orbit.getEquinoctialEyDot());
            Assertions.assertNotNull(orbit.getHxDot());
            Assertions.assertNotNull(orbit.getHyDot());
            Assertions.assertNotNull(orbit.getLEDot());
            Assertions.assertNotNull(orbit.getLvDot());
            Assertions.assertNotNull(orbit.getLMDot());
            Assertions.assertNotNull(orbit.getEDot());
            Assertions.assertNotNull(orbit.getIDot());
        } else {
            Assertions.assertNull(orbit.getADot());
            Assertions.assertNull(orbit.getEquinoctialExDot());
            Assertions.assertNull(orbit.getEquinoctialEyDot());
            Assertions.assertNull(orbit.getHxDot());
            Assertions.assertNull(orbit.getHyDot());
            Assertions.assertNull(orbit.getLEDot());
            Assertions.assertNull(orbit.getLvDot());
            Assertions.assertNull(orbit.getLMDot());
            Assertions.assertNull(orbit.getEDot());
            Assertions.assertNull(orbit.getIDot());
        }
    }

    private <T extends CalculusFieldElement<T>> T tangLEmLv(T Lv, T ex, T ey){
        // tan ((LE - Lv) /2)) =
        return ey.multiply(Lv.cos()).subtract(ex.multiply(Lv.sin())).divide(
               ex.multiply(Lv.cos()).add(1.).add(ey.multiply(Lv.sin())).add( ex.multiply(ex).negate().add(1.).subtract(ey.multiply(ey)).sqrt()));
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        mu  = 3.9860047e14;
    }

    @AfterEach
    public void tearDown() {
        mu   = Double.NaN;
    }

}

