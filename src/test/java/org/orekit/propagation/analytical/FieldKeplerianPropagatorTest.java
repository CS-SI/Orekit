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
package org.orekit.propagation.analytical;


import java.io.IOException;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.attitudes.FieldAttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldCircularOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldBoundedPropagator;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
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
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.IERSConventions;


public class FieldKeplerianPropagatorTest {

    // Body mu
    private double mu;

    @Test
    public void doSameDateCartesianTest() throws OrekitException, ClassNotFoundException, IOException{
        sameDateCartesian(Decimal64Field.getInstance());
    }
    

    @Test
    public void doSameDateKeplerianTest() throws OrekitException, ClassNotFoundException, IOException{
        sameDateKeplerian(Decimal64Field.getInstance());
    }
    

    @Test
    public void doPropagatedCartesianTest() throws OrekitException, ClassNotFoundException, IOException{
        propagatedCartesian(Decimal64Field.getInstance());
    }
    

    @Test
    public void doPropagatedKeplerianTest() throws OrekitException, ClassNotFoundException, IOException{
        propagatedKeplerian(Decimal64Field.getInstance());
    }
    

    @Test
    public void doAscendingNodeTest() throws OrekitException, ClassNotFoundException, IOException{
        ascendingNode(Decimal64Field.getInstance());
    }
    

    @Test
    public void doStopAtTargetDateTest() throws OrekitException, ClassNotFoundException, IOException{
        stopAtTargetDate(Decimal64Field.getInstance());
    }
    

    @Test
    public void doFixedStepTest() throws OrekitException, ClassNotFoundException, IOException{
        fixedStep(Decimal64Field.getInstance());
    }
    

    @Test
    public void doVariableStepTest() throws OrekitException, ClassNotFoundException, IOException{
        variableStep(Decimal64Field.getInstance());
    }
    

    @Test
    public void doEphemerisTest() throws OrekitException, ClassNotFoundException, IOException{
        ephemeris(Decimal64Field.getInstance());}
    

    @Test
    public void doIssue14Test() throws OrekitException, ClassNotFoundException, IOException{
        testIssue14(Decimal64Field.getInstance());
    }
    

    @Test
    public void doIssue107Test() throws OrekitException, ClassNotFoundException, IOException{
        testIssue107(Decimal64Field.getInstance());
    }
    

    @Test
    public void doMuTest() throws OrekitException, ClassNotFoundException, IOException{
        testMu(Decimal64Field.getInstance());
    }

    @Test(expected = OrekitException.class)
    public void testErr1() throws OrekitException{
        wrongAttitude(Decimal64Field.getInstance());
    }

    @Test(expected = OrekitException.class)
    public void testErr2() throws OrekitException{
        testStepException(Decimal64Field.getInstance());
    }

    @Test(expected = OrekitException.class)
    public void testErr3() throws OrekitException{
        tesWrapedAttitudeException(Decimal64Field.getInstance());
    }

    public <T extends RealFieldElement<T>> void sameDateCartesian(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        // Definition of initial conditions with position and velocity
        //------------------------------------------------------------
        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<T>(field).shiftedBy(584.);
        FieldOrbit<T> initialOrbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position, velocity),
                                                  FramesFactory.getEME2000(), initDate, mu);

        // Extrapolator definition
        // -----------------------
        FieldKeplerianPropagator<T> extrapolator = new FieldKeplerianPropagator<T>(initialOrbit);

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
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbit.getLM().getReal(),initialOrbit.getLM().getReal()), initialOrbit.getLM().getReal(),
                            Utils.epsilonAngle * FastMath.abs(initialOrbit.getLM().getReal()));

        Assert.assertEquals(finalOrbit.getA().getReal(), initialOrbit.getA().getReal(),
                            Utils.epsilonTest * initialOrbit.getA().getReal());
        Assert.assertEquals(finalOrbit.getE().getReal(), initialOrbit.getE().getReal(),
                            Utils.epsilonE * initialOrbit.getE().getReal());
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbit.getI().getReal(), initialOrbit.getI().getReal()),
                            initialOrbit.getI().getReal(), Utils.epsilonAngle * FastMath.abs(initialOrbit.getI().getReal()));

    }

    public <T extends RealFieldElement<T>> void sameDateKeplerian(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        // Definition of initial conditions with keplerian parameters
        //-----------------------------------------------------------
        FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<T>(field).shiftedBy(584.);
        FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<T>(zero.add(7209668.0), zero.add(0.5e-4), zero.add(1.7), zero.add(2.1), zero.add(2.9),
                                                zero.add(6.2), PositionAngle.TRUE,
                                                FramesFactory.getEME2000(), initDate, mu);

        // Extrapolator definition
        // -----------------------
        FieldKeplerianPropagator<T> extrapolator = new FieldKeplerianPropagator<T>(initialOrbit);

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
                     Utils.epsilonTest * FastMath.max(100.,FastMath.abs(n.getReal()*delta_t.getReal())));
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbit.getLM().getReal(),initialOrbit.getLM().getReal()),
                            initialOrbit.getLM().getReal(), Utils.epsilonAngle * FastMath.abs(initialOrbit.getLM().getReal()));

        Assert.assertEquals(finalOrbit.getA().getReal(), initialOrbit.getA().getReal(),
                            Utils.epsilonTest * initialOrbit.getA().getReal());
        Assert.assertEquals(finalOrbit.getE().getReal(), initialOrbit.getE().getReal(),
                            Utils.epsilonE * initialOrbit.getE().getReal());
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbit.getI().getReal(),initialOrbit.getI().getReal()),
                            initialOrbit.getI().getReal(),
                            Utils.epsilonAngle * FastMath.abs(initialOrbit.getI().getReal()));

    }

    public <T extends RealFieldElement<T>> void propagatedCartesian(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        // Definition of initial conditions with position and velocity
        //------------------------------------------------------------
        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));
        double mu = 3.9860047e14;

        FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<T>(field).shiftedBy(584.);
        FieldOrbit<T> initialOrbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position, velocity),
                                                  FramesFactory.getEME2000(), initDate, mu);

        // Extrapolator definition
        // -----------------------
        FieldKeplerianPropagator<T> extrapolator = new FieldKeplerianPropagator<T>(initialOrbit);

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
                     tangLEmLv(finalOrbit.getLv(),finalOrbit.getEquinoctialEx(),finalOrbit.getEquinoctialEy()).getReal(),
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

        FieldVector3D<T> U = new FieldVector3D<T>(hx2.add(1.).subtract(hy2).divide(h2p1),
                                  hx.multiply(2.).multiply(hy).divide(h2p1),
                                  hy.multiply(-2.).divide(h2p1));

        FieldVector3D<T> V = new FieldVector3D<T>(hx.multiply(2.).multiply(hy).divide(h2p1),
                                  hy2.subtract(hx2).add(1).divide(h2p1),
                                  hx.multiply(2.).divide(h2p1));

        FieldVector3D<T> r = new FieldVector3D<T>(finalOrbit.getA(),(new FieldVector3D<T>(x3,U,y3,V)));

        Assert.assertEquals(finalOrbit.getPVCoordinates().getPosition().getNorm().getReal(), r.getNorm().getReal(), Utils.epsilonTest * r.getNorm().getReal());

    }

    public <T extends RealFieldElement<T>> void propagatedKeplerian(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        // Definition of initial conditions with keplerian parameters
        //-----------------------------------------------------------
        FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<T>(field).shiftedBy(584.);
        FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<T>(zero.add(7209668.0), zero.add(0.5e-4), zero.add(1.7), zero.add(2.1), zero.add(2.9),
                                                zero.add(6.2), PositionAngle.TRUE,
                                                FramesFactory.getEME2000(), initDate, mu);

        // Extrapolator definition
        // -----------------------
        FieldKeplerianPropagator<T> extrapolator = new FieldKeplerianPropagator<T>(initialOrbit);

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
                     tangLEmLv(finalOrbit.getLv(),finalOrbit.getEquinoctialEx(),finalOrbit.getEquinoctialEy()).getReal(),
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

        FieldVector3D<T> U = new FieldVector3D<T>(hx2.add(1.).subtract(hy2).divide(h2p1),
                                  hx.multiply(2.).multiply(hy).divide(h2p1),
                                  hy.multiply(-2).divide(h2p1));

        FieldVector3D<T> V = new FieldVector3D<T>(hx.multiply(2).multiply(hy).divide(h2p1),
                                  hy2.subtract(hx2).add(1.).divide(h2p1),
                                  hx.multiply(2).divide(h2p1));

        FieldVector3D<T> r = new FieldVector3D<T>(finalOrbit.getA(),(new FieldVector3D<T>(x3,U,y3,V)));

        Assert.assertEquals(finalOrbit.getPVCoordinates().getPosition().getNorm().getReal(), r.getNorm().getReal(), Utils.epsilonTest * r.getNorm().getReal());

    }

    public <T extends RealFieldElement<T>> void wrongAttitude(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<T>(zero.add(1.0e10), zero.add(1.0e-4), zero.add(1.0e-2), zero, zero, zero, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), new FieldAbsoluteDate<T>(field), 3.986004415e14);
        FieldAttitudeProvider<T> wrongLaw = new FieldAttitudeProvider<T>() {
            @Override
            public FieldAttitude<T> getAttitude(FieldPVCoordinatesProvider<T> pvProv, FieldAbsoluteDate<T> date, Frame frame) throws OrekitException {
                throw new OrekitException(new DummyLocalizable("gasp"), new RuntimeException());
            }
        };
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<T>(orbit, wrongLaw);
        propagator.propagate(new FieldAbsoluteDate<T>(field).shiftedBy(10.0));
    }

    public <T extends RealFieldElement<T>> void testStepException(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<T>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                               FramesFactory.getEME2000(), new FieldAbsoluteDate<T>(field), 3.986004415e14);
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<T>(orbit);
        FieldOrekitStepHandlerMultiplexer<T> multiplexer = new FieldOrekitStepHandlerMultiplexer<T>();
        propagator.setMasterMode(multiplexer);
        multiplexer.add(new FieldOrekitStepHandler<T>() {
            public void init(FieldSpacecraftState<T> s0, FieldAbsoluteDate<T> t) {
            }
            public void handleStep(FieldOrekitStepInterpolator<T> interpolator,
                                   boolean isLast) throws OrekitException {
                if (isLast) {
                    throw new OrekitException((Throwable) null, new DummyLocalizable("dummy error"));
                }
            }
        });

        propagator.propagate(orbit.getDate().shiftedBy(-3600));

    }

    public <T extends RealFieldElement<T>> void tesWrapedAttitudeException(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<T>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                               FramesFactory.getEME2000(), new FieldAbsoluteDate<T>(field), 3.986004415e14);
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<T>(orbit,
                                                                 new FieldAttitudeProvider<T>() {
                                                                    public FieldAttitude<T> getAttitude(FieldPVCoordinatesProvider<T> pvProv, FieldAbsoluteDate<T> date,
                                                                                                Frame frame)
                                                                        throws OrekitException {
                                                                        throw new OrekitException((Throwable) null,
                                                                                                  new DummyLocalizable("dummy error"));
                                                                    }
                                                                });
        propagator.propagate(orbit.getDate().shiftedBy(10.09));
    }

    public <T extends RealFieldElement<T>> void ascendingNode(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<T>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                               FramesFactory.getEME2000(), new FieldAbsoluteDate<T>(field), 3.986004415e14);
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<T>(orbit);
        propagator.addEventDetector(new FieldNodeDetector<T>(orbit, FramesFactory.getITRF(IERSConventions.IERS_2010, true)));
        FieldAbsoluteDate<T> farTarget = new FieldAbsoluteDate<T>(field).shiftedBy(10000.0);
        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);
        FieldPVCoordinates<T> pv = propagated.getPVCoordinates(FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() > 3500.0);
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() < 4000.0);
        Assert.assertEquals(0, pv.getPosition().getZ().getReal(), 2.0e-6);
        Assert.assertTrue(pv.getVelocity().getZ().getReal() > 0);
    }

    public <T extends RealFieldElement<T>> void stopAtTargetDate(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<T>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                               FramesFactory.getEME2000(), new FieldAbsoluteDate<T>(field), 3.986004415e14);
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<T>(orbit);
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        propagator.addEventDetector(new FieldNodeDetector<T>(orbit, itrf).withHandler(new FieldContinueOnEvent<FieldNodeDetector<T>, T>()));
        FieldAbsoluteDate<T> farTarget = orbit.getDate().shiftedBy(10000.0);
        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);
        Assert.assertEquals(0.0, FastMath.abs(farTarget.durationFrom(propagated.getDate()).getReal()), 1.0e-3);
    }

//    public <T extends RealFieldElement<T>> void perigee(Field<T> field) throws OrekitException {
//        T zero = field.getZero();
//        final FieldKeplerianOrbit<T> orbit =
//            new FieldKeplerianOrbit<T>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
//                               FramesFactory.getEME2000(), new FieldAbsoluteDate<T>(field), 3.986004415e14);
//        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<T>(orbit);
//        propagator.addEventDetector(new ApsideDetector(orbit));
//        FieldAbsoluteDate<T> farTarget = new FieldAbsoluteDate<T>(field).shiftedBy(10000.0);
//        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);
//        FieldPVCoordinates<T> pv = propagated.getFieldPVCoordinates<T>(FramesFactory.getITRF(IERSConventions.IERS_2010, true));
//        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) > 3000.0);
//        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) < 3500.0);
//        Assert.assertEquals(orbit.getA() * (1.0 - orbit.getE()), pv.getPosition().getNorm(), 1.0e-6);
//    }

//    public <T extends RealFieldElement<T>> void altitude(Field<T> field) throws OrekitException {
//        T zero = field.getZero();
//        final FieldKeplerianOrbit<T> orbit =
//            new FieldKeplerianOrbit<T>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
//                               FramesFactory.getEME2000(), new FieldAbsoluteDate<T>(field), 3.986004415e14);
//        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<T>(orbit);
//        BodyShape bodyShape =
//            new OneAxisEllipsoid(6378137.0, 1.0 / 298.257222101, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
//        AltitudeDetector detector =
//            new AltitudeDetector(0.05 * orbit.getKeplerianPeriod(),
//                                 1500000, bodyShape);
//        Assert.assertEquals(1500000, detector.getAltitude(), 1.0e-12);
//        propagator.addEventDetector(detector);
//        FieldAbsoluteDate<T> farTarget = new FieldAbsoluteDate<T>(field).shiftedBy(10000.0);
//        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);
//        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) > 5400.0);
//        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) < 5500.0);
//        GeodeticPoint gp = bodyShape.transform(propagated.getFieldPVCoordinates<T>().getPosition(),
//                                               propagated.getFrame(), propagated.getDate());
//        Assert.assertEquals(1500000, gp.getAltitude(), 0.1);
//    }

//    public <T extends RealFieldElement<T>> void date(Field<T> field) throws OrekitException {
//        T zero = field.getZero();
//        final FieldKeplerianOrbit<T> orbit =
//            new FieldKeplerianOrbit<T>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
//                               FramesFactory.getEME2000(), new FieldAbsoluteDate<T>(field), 3.986004415e14);
//        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<T>(orbit);
//        final FieldAbsoluteDate<T> stopDate = new FieldAbsoluteDate<T>(field).shiftedBy(500.0);
//        propagator.addEventDetector(new DateDetector(stopDate));
//        FieldAbsoluteDate<T> farTarget = new FieldAbsoluteDate<T>(field).shiftedBy(10000.0);
//        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);
//        Assert.assertEquals(0, stopDate.durationFrom(propagated.getDate()), 1.0e-10);
//    }

//    public <T extends RealFieldElement<T>> void setting(Field<T> field) throws OrekitException {
//        T zero = field.getZero();
//        final FieldKeplerianOrbit<T> orbit =
//            new FieldKeplerianOrbit<T>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
//                               FramesFactory.getEME2000(), new FieldAbsoluteDate<T>(field), 3.986004415e14);
//        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<T>(orbit);
//        final OneAxisEllipsoid earthShape =
//            new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
//        final TopocentricFrame topo =
//            new TopocentricFrame(earthShape, new GeodeticPoint(0.389, -2.962, 0), null);
//        propagator.addEventDetector(new ElevationDetector(60, FieldAbstractDetector<T>.DEFAULT_THRESHOLD, topo).withConstantElevation(0.09));
//        FieldAbsoluteDate<T> farTarget = new FieldAbsoluteDate<T>(field).shiftedBy(10000.0);
//        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);
//        final T elevation = topo.getElevation(propagated.getFieldPVCoordinates<T>().getPosition(),
//                                                   propagated.getFrame(),
//                                                   propagated.getDate());
//        final T zVelocity = propagated.getFieldPVCoordinates<T>(topo).getVelocity().getZ();
//        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) > 7800.0);
//        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()) < 7900.0);
//        Assert.assertEquals(0.09, elevation, 1.0e-9);
//        Assert.assertTrue(zVelocity < 0);
//    }


    public <T extends RealFieldElement<T>> void fixedStep(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<T>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                               FramesFactory.getEME2000(), new FieldAbsoluteDate<T>(field), 3.986004415e14);
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<T>(orbit);
        final T step = zero.add(100.0);
        propagator.setMasterMode(step, new FieldOrekitFixedStepHandler<T>() {
            private FieldAbsoluteDate<T> previous;
            public void handleStep(FieldSpacecraftState<T> currentState, boolean isLast)
            throws OrekitException {
                if (previous != null) {
                    Assert.assertEquals(step.getReal(), currentState.getDate().durationFrom(previous).getReal(), 1.0e-10);
                }
                previous = currentState.getDate();
            }
        });
        FieldAbsoluteDate<T> farTarget = new FieldAbsoluteDate<T>(field).shiftedBy(10000.0);
        propagator.propagate(farTarget);
    }

    public <T extends RealFieldElement<T>> void variableStep(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<T>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                               FramesFactory.getEME2000(), new FieldAbsoluteDate<T>(field), 3.986004415e14);
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<T>(orbit);
        final T step = orbit.getKeplerianPeriod().divide(100);
        propagator.setMasterMode(new FieldOrekitStepHandler<T>() {
            private FieldAbsoluteDate<T> previous;
            public void init(FieldSpacecraftState<T> s0, FieldAbsoluteDate<T> t) {
            }
            public void handleStep(FieldOrekitStepInterpolator<T> interpolator,
                                   boolean isLast) throws OrekitException {
                if ((previous != null) && !isLast) {
                    Assert.assertEquals(step.getReal(), interpolator.getCurrentState().getDate().durationFrom(previous).getReal(), 1.0e-10);
                }
                previous = interpolator.getCurrentState().getDate();
            }
        });
        FieldAbsoluteDate<T> farTarget = new FieldAbsoluteDate<T>(field).shiftedBy(10000.0);
        propagator.propagate(farTarget);
    }

    public <T extends RealFieldElement<T>> void ephemeris(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<T>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                               FramesFactory.getEME2000(), new FieldAbsoluteDate<T>(field), 3.986004415e14);
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<T>(orbit);
        propagator.setEphemerisMode();
        FieldAbsoluteDate<T> farTarget = new FieldAbsoluteDate<T>(field).shiftedBy(10000.0);
        propagator.setEphemerisMode();
        propagator.propagate(farTarget);
        FieldBoundedPropagator<T> ephemeris = propagator.getGeneratedEphemeris();
        Assert.assertEquals(0.0, ephemeris.getMinDate().durationFrom(orbit.getDate()).getReal(), 1.0e-10);
        Assert.assertEquals(0.0, ephemeris.getMaxDate().durationFrom(farTarget).getReal(), 1.0e-10);
    }
//
    public <T extends RealFieldElement<T>> void testIssue14(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldAbsoluteDate<T> initialDate = new FieldAbsoluteDate<T>(field);
        final FieldKeplerianOrbit<T> initialOrbit =
            new FieldKeplerianOrbit<T>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                               FramesFactory.getEME2000(), initialDate, 3.986004415e14);
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<T>(initialOrbit);

        propagator.setEphemerisMode();
        propagator.propagate(initialDate.shiftedBy(initialOrbit.getKeplerianPeriod()));
        FieldPVCoordinates<T> pv1 = propagator.getPVCoordinates(initialDate, FramesFactory.getEME2000());

        propagator.setEphemerisMode();
        propagator.propagate(initialDate.shiftedBy(initialOrbit.getKeplerianPeriod()));
        FieldPVCoordinates<T> pv2 = propagator.getGeneratedEphemeris().getPVCoordinates(initialDate, FramesFactory.getEME2000());

        Assert.assertEquals(0.0, pv1.getPosition().subtract(pv2.getPosition()).getNorm().getReal(), 1.0e-15);
        Assert.assertEquals(0.0, pv1.getVelocity().subtract(pv2.getVelocity()).getNorm().getReal(), 1.0e-15);

    }

    public <T extends RealFieldElement<T>> void testIssue107(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        final TimeScale utc = TimeScalesFactory.getUTC();
        final FieldVector3D<T> position = new FieldVector3D<T>(zero.add(-6142438.668), zero.add(3492467.56), zero.add(-25767.257));
        final FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(505.848),zero.add( 942.781), zero.add(7435.922));
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field, 2003, 9, 16, utc);
        final FieldOrbit<T> orbit = new FieldCircularOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
                                              FramesFactory.getEME2000(), date, mu);

        FieldPropagator<T> propagator = new FieldKeplerianPropagator<T>(orbit) {
            FieldAbsoluteDate<T> lastDate = FieldAbsoluteDate.getPastInfinity(field);

            protected FieldSpacecraftState<T> basicPropagate(final FieldAbsoluteDate<T> date) throws OrekitException {
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

    public <T extends RealFieldElement<T>> void testMu(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        final FieldKeplerianOrbit<T> orbit1 =
                new FieldKeplerianOrbit<T>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), new FieldAbsoluteDate<T>(field),
                                   Constants.WGS84_EARTH_MU);
        final FieldKeplerianOrbit<T> orbit2 =
                new FieldKeplerianOrbit<T>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), new FieldAbsoluteDate<T>(field),
                                   Constants.EIGEN5C_EARTH_MU);
        final FieldAbsoluteDate<T> target = orbit1.getDate().shiftedBy(10000.0);
        FieldPVCoordinates<T> pv1       = new FieldKeplerianPropagator<T>(orbit1).propagate(target).getPVCoordinates();
        FieldPVCoordinates<T> pv2       = new FieldKeplerianPropagator<T>(orbit2).propagate(target).getPVCoordinates();
        FieldPVCoordinates<T> pvWithMu1 = new FieldKeplerianPropagator<T>(orbit2, orbit1.getMu()).propagate(target).getPVCoordinates();
        Assert.assertEquals(0.026054, FieldVector3D.distance(pv1.getPosition(), pv2.getPosition()).getReal(),       1.0e-6);
        Assert.assertEquals(0.0,      FieldVector3D.distance(pv1.getPosition(), pvWithMu1.getPosition()).getReal(), 1.0e-15);
    }

    private <T extends RealFieldElement<T>> T tangLEmLv(T Lv,T ex,T ey){
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

