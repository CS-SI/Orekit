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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
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
import org.orekit.attitudes.FieldLofOffset;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.FieldCircularOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.events.FieldApsideDetector;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldElevationDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.FieldNodeDetector;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.propagation.sampling.FieldOrekitFixedStepHandler;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedFieldPVCoordinates;


public class FieldEcksteinHechlerPropagatorTest {

    private double mu;
    private double ae;

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        mu  = 3.9860047e14;
        ae  = 6.378137e6;
        double[][] cnm = new double[][] {
            { 0 }, { 0 }, { -1.08263e-3 }, { 2.54e-6 }, { 1.62e-6 }, { 2.3e-7 }, { -5.5e-7 }
           };
        double[][] snm = new double[][] {
            { 0 }, { 0 }, { 0 }, { 0 }, { 0 }, { 0 }, { 0 }
           };

        provider = GravityFieldFactory.getUnnormalizedProvider(ae, mu, TideSystem.UNKNOWN, cnm, snm);
    }

    @Test
    public void sameDateCartesian() throws OrekitException {
        doSameDateCartesian(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doSameDateCartesian(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        // Definition of initial conditions with position and velocity
        // ------------------------------------------------------------
        // with e around e = 1.4e-4 and i = 1.7 rad
        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(3220103.), zero.add(69623.), zero.add(6449822.));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(6414.7), zero.add(-2006.), zero.add(-3180.));

        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        FieldOrbit<T> initialOrbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position, velocity),
                                                  FramesFactory.getEME2000(), initDate, provider.getMu());

        // Extrapolator definition
        // -----------------------
        FieldEcksteinHechlerPropagator<T> extrapolator =
            new FieldEcksteinHechlerPropagator<T>(initialOrbit, provider);

        // Extrapolation at the initial date
        // ---------------------------------
        FieldSpacecraftState<T> finalOrbit = extrapolator.propagate(initDate);

        // positions match perfectly
        Assert.assertEquals(0.0,
                            FieldVector3D.distance(initialOrbit.getPVCoordinates().getPosition(),
                                              finalOrbit.getPVCoordinates().getPosition()).getReal(),
                            1.0e-8);

        // velocity and circular parameters do *not* match, this is EXPECTED!
        // the reason is that we ensure position/velocity are consistent with the
        // evolution of the orbit, and this includes the non-Keplerian effects,
        // whereas the initial orbit is Keplerian only. The implementation of the
        // model is such that rather than having a perfect match at initial point
        // (either in velocity or in circular parameters), we have a propagated orbit
        // that remains close to a numerical reference throughout the orbit.
        // This is shown in the testInitializationCorrectness() where a numerical
        // fit is used to check initialization

        Assert.assertEquals(0.137,
                            FieldVector3D.distance(initialOrbit.getPVCoordinates().getVelocity(),
                                              finalOrbit.getPVCoordinates().getVelocity()).getReal(),
                            1.0e-3);
        Assert.assertEquals(125.2, finalOrbit.getA().getReal() - initialOrbit.getA().getReal(), 0.1);

    }

    @Test
    public void sameDateKeplerian() throws OrekitException {
        doSameDateKeplerian(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doSameDateKeplerian(Field<T> field) throws OrekitException {

        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

        // Definition of initial conditions with keplerian parameters
        // -----------------------------------------------------------
        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<T>(zero.add(7209668.0), zero.add(0.5e-4), zero.add(1.7),zero.add( 2.1),zero.add( 2.9),
                                        zero.add(6.2), PositionAngle.TRUE,
                                                FramesFactory.getEME2000(), initDate, provider.getMu());

        // Extrapolator definition
        // -----------------------
        FieldEcksteinHechlerPropagator<T> extrapolator =
            new FieldEcksteinHechlerPropagator<T>(initialOrbit, zero.add(Propagator.DEFAULT_MASS), provider);

        // Extrapolation at the initial date
        // ---------------------------------
        FieldSpacecraftState<T> finalOrbit = extrapolator.propagate(initDate);

        // positions match perfectly
        Assert.assertEquals(0.0,
                            FieldVector3D.distance(initialOrbit.getPVCoordinates().getPosition(),
                                              finalOrbit.getPVCoordinates().getPosition()).getReal(),
                            3.0e-8);

        // velocity and circular parameters do *not* match, this is EXPECTED!
        // the reason is that we ensure position/velocity are consistent with the
        // evolution of the orbit, and this includes the non-Keplerian effects,
        // whereas the initial orbit is Keplerian only. The implementation of the
        // model is such that rather than having a perfect match at initial point
        // (either in velocity or in circular parameters), we have a propagated orbit
        // that remains close to a numerical reference throughout the orbit.
        // This is shown in the testInitializationCorrectness() where a numerical
        // fit is used to check initialization
        Assert.assertEquals(0.137,
                            FieldVector3D.distance(initialOrbit.getPVCoordinates().getVelocity(),
                                              finalOrbit.getPVCoordinates().getVelocity()).getReal(),
                            1.0e-3);
        Assert.assertEquals(126.8, finalOrbit.getA().getReal() - initialOrbit.getA().getReal(), 0.1);

    }

    @Test
    public void almostSphericalBody() throws OrekitException {
        doAlmostSphericalBody(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doAlmostSphericalBody(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

        // Definition of initial conditions
        // ---------------------------------
        // with e around e = 1.4e-4 and i = 1.7 rad
        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(3220103.), zero.add(69623.), zero.add(6449822.));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(6414.7), zero.add(-2006.), zero.add(-3180.));

        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        FieldOrbit<T> initialOrbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position, velocity),
                                                  FramesFactory.getEME2000(), initDate, provider.getMu());

        // Initialisation to simulate a keplerian extrapolation
        // To be noticed: in order to simulate a keplerian extrapolation with the
        // analytical
        // extrapolator, one should put the zonal coefficients to 0. But due to
        // numerical pbs
        // one must put a non 0 value.
        UnnormalizedSphericalHarmonicsProvider kepProvider =
                GravityFieldFactory.getUnnormalizedProvider(6.378137e6, 3.9860047e14,
                                                            TideSystem.UNKNOWN,
                                                            new double[][] {
                                                                { 0 }, { 0 }, { 0.1e-10 }, { 0.1e-13 }, { 0.1e-13 }, { 0.1e-14 }, { 0.1e-14 }
                                                            }, new double[][] {
                                                                { 0 }, { 0 },  { 0 }, { 0 }, { 0 }, { 0 }, { 0 }
                                                            });

        // Extrapolators definitions
        // -------------------------
        FieldEcksteinHechlerPropagator<T> extrapolatorAna =
            new FieldEcksteinHechlerPropagator<T>(initialOrbit,
                                          kepProvider);
        FieldKeplerianPropagator<T> extrapolatorKep = new FieldKeplerianPropagator<T>(initialOrbit);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        double delta_t = 100.0; // extrapolation duration in seconds
        FieldAbsoluteDate<T> extrapDate = initDate.shiftedBy(delta_t);

        FieldSpacecraftState<T> finalOrbitAna = extrapolatorAna.propagate(extrapDate);
        FieldSpacecraftState<T> finalOrbitKep = extrapolatorKep.propagate(extrapDate);

        Assert.assertEquals(finalOrbitAna.getDate().durationFrom(extrapDate).getReal(), 0.0,
                     Utils.epsilonTest);
        // comparison of each orbital parameters
        Assert.assertEquals(finalOrbitAna.getA().getReal(), finalOrbitKep.getA().getReal(), 10
                     * Utils.epsilonTest * finalOrbitKep.getA().getReal());
        Assert.assertEquals(finalOrbitAna.getEquinoctialEx().getReal(), finalOrbitKep.getEquinoctialEx().getReal(), Utils.epsilonE
                     * finalOrbitKep.getE().getReal());
        Assert.assertEquals(finalOrbitAna.getEquinoctialEy().getReal(), finalOrbitKep.getEquinoctialEy().getReal(), Utils.epsilonE
                     * finalOrbitKep.getE().getReal());
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getHx().getReal(), finalOrbitKep.getHx().getReal()),
                     finalOrbitKep.getHx().getReal(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getI().getReal()));
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getHy().getReal(), finalOrbitKep.getHy().getReal()),
                     finalOrbitKep.getHy().getReal(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getI().getReal()));
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getLv().getReal(), finalOrbitKep.getLv().getReal()),
                     finalOrbitKep.getLv().getReal(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getLv().getReal()));
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getLE().getReal(), finalOrbitKep.getLE().getReal()),
                     finalOrbitKep.getLE().getReal(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getLE().getReal()));
        Assert.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getLM().getReal(), finalOrbitKep.getLM().getReal()),
                     finalOrbitKep.getLM().getReal(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getLM().getReal()));

    }

    @Test
    public void propagatedCartesian() throws OrekitException {
        doPropagatedCartesian(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doPropagatedCartesian(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        // Definition of initial conditions with position and velocity
        // ------------------------------------------------------------
        // with e around e = 1.4e-4 and i = 1.7 rad
        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(3220103.), zero.add(69623.), zero.add(6449822.));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(6414.7), zero.add(-2006.), zero.add(-3180.));

        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        FieldOrbit<T> initialOrbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position, velocity),
                                                  FramesFactory.getEME2000(), initDate, provider.getMu());

        // Extrapolator definition
        // -----------------------
        FieldEcksteinHechlerPropagator<T> extrapolator =
            new FieldEcksteinHechlerPropagator<T>(initialOrbit,
                                          new FieldLofOffset<T>(initialOrbit.getFrame(),
                                                        LOFType.VNC, RotationOrder.XYZ, zero, zero, zero),
                                          provider);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        double delta_t = 100000.0; // extrapolation duration in seconds
        FieldAbsoluteDate<T> extrapDate = initDate.shiftedBy(delta_t);

        FieldSpacecraftState<T> finalOrbit = extrapolator.propagate(extrapDate);

        Assert.assertEquals(0.0, finalOrbit.getDate().durationFrom(extrapDate).getReal(), 1.0e-9);

        // computation of M final orbit
        T LM = finalOrbit.getLE().subtract(finalOrbit.getEquinoctialEx().multiply(
        finalOrbit.getLE().sin())).add(finalOrbit.getEquinoctialEy()
        .multiply(finalOrbit.getLE().cos()));

        Assert.assertEquals(LM.getReal(), finalOrbit.getLM().getReal(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbit.getLM().getReal()));

        // test of tan ((LE - Lv)/2) :
        Assert.assertEquals(FastMath.tan((finalOrbit.getLE().getReal() - finalOrbit.getLv().getReal()) / 2.),
                     tangLEmLv(finalOrbit.getLv(), finalOrbit.getEquinoctialEx(), finalOrbit
                               .getEquinoctialEy()).getReal(), Utils.epsilonAngle);

        // test of evolution of M vs E: LM = LE - ex*sin(LE) + ey*cos(LE)
        T deltaM = finalOrbit.getLM().subtract(initialOrbit.getLM());
        T deltaE = finalOrbit.getLE().subtract(initialOrbit.getLE());
        T delta = finalOrbit.getEquinoctialEx().multiply(finalOrbit.getLE().sin()).subtract(
                 initialOrbit.getEquinoctialEx().multiply(initialOrbit.getLE().sin())).subtract(
                 finalOrbit.getEquinoctialEy().multiply(finalOrbit.getLE().cos())).add(
                 initialOrbit.getEquinoctialEy().multiply(initialOrbit.getLE().cos()));

        Assert.assertEquals(deltaM.getReal(), deltaE.getReal() - delta.getReal(), Utils.epsilonAngle
                     * FastMath.abs(deltaE.getReal() - delta.getReal()));

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

        T x3 = ex.negate().add(ey2.multiply(beta).negate().add(1.).multiply(LE.cos())).add(beta.multiply(ex).multiply(ey).multiply(
        LE.sin()));
        T y3 = ey.negate().add(ex2.negate().multiply(beta).add(1).multiply(LE.sin())).add(beta.multiply(ex).multiply(ey).multiply(LE.cos()));

        FieldVector3D<T> U = new FieldVector3D<T>(hx2.add(1).subtract(hy2).divide(h2p1), hx.multiply(hy).multiply(2).divide(h2p1),
                                  hy.multiply(-2).divide(h2p1));

        FieldVector3D<T> V = new FieldVector3D<T>(hx.multiply(2).multiply(hy).divide(h2p1),hy2.add(1).subtract(hx2).divide(h2p1),
                                  hx.multiply(2).divide(h2p1));

        FieldVector3D<T> r = new FieldVector3D<T>(finalOrbit.getA(), (new FieldVector3D<T>(x3, U, y3, V)));

        Assert.assertEquals(finalOrbit.getPVCoordinates().getPosition().getNorm().getReal(), r.getNorm().getReal(),
                     Utils.epsilonTest * r.getNorm().getReal());

    }

    @Test
    public void propagatedKeplerian() throws OrekitException {
        
        doPropagatedKeplerian(Decimal64Field.getInstance());
        
    }

    private <T extends RealFieldElement<T>> void doPropagatedKeplerian(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        // Definition of initial conditions with keplerian parameters
        // -----------------------------------------------------------
        FieldAbsoluteDate<T> initDate = date.shiftedBy(584.);
        FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<T>(zero.add(7209668.0), zero.add(0.5e-4), zero.add(1.7), zero.add(2.1), zero.add(2.9),
                                              zero.add(6.2), PositionAngle.TRUE,
                                              FramesFactory.getEME2000(), initDate, provider.getMu());

        // Extrapolator definition
        // -----------------------
        FieldEcksteinHechlerPropagator<T> extrapolator =
            new FieldEcksteinHechlerPropagator<T>(initialOrbit,
                                          new FieldLofOffset<T>(initialOrbit.getFrame(),
                                                        LOFType.VNC, RotationOrder.XYZ, zero, zero, zero),
                                          zero.add(2000.0), provider);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        double delta_t = 100000.0; // extrapolation duration in seconds
        FieldAbsoluteDate<T> extrapDate = initDate.shiftedBy(delta_t);

        FieldSpacecraftState<T> finalOrbit = extrapolator.propagate(extrapDate);

        Assert.assertEquals(0.0, finalOrbit.getDate().durationFrom(extrapDate).getReal(), 1.0e-9);

        // computation of M final orbit
        T LM = finalOrbit.getLE().subtract(finalOrbit.getEquinoctialEx().multiply(
        finalOrbit.getLE().sin())).add(finalOrbit.getEquinoctialEy().multiply(
        finalOrbit.getLE().cos()));

        Assert.assertEquals(LM.getReal(), finalOrbit.getLM().getReal(), Utils.epsilonAngle);

        // test of tan((LE - Lv)/2) :
        Assert.assertEquals(FastMath.tan((finalOrbit.getLE().getReal() - finalOrbit.getLv().getReal()) / 2.),
                     tangLEmLv(finalOrbit.getLv(), finalOrbit.getEquinoctialEx(), finalOrbit
                               .getEquinoctialEy()).getReal(), Utils.epsilonAngle);

        // test of evolution of M vs E: LM = LE - ex*sin(LE) + ey*cos(LE)
        // with ex and ey the same for initial and final orbit
        T deltaM = finalOrbit.getLM().subtract(initialOrbit.getLM());
        T deltaE = finalOrbit.getLE().subtract(initialOrbit.getLE());
        T delta = finalOrbit.getEquinoctialEx().multiply(finalOrbit.getLE().sin()).subtract(
                initialOrbit.getEquinoctialEx().multiply(initialOrbit.getLE().sin())).subtract(
                  finalOrbit.getEquinoctialEy().multiply(finalOrbit.getLE().cos())).add(
                initialOrbit.getEquinoctialEy().multiply(initialOrbit.getLE().cos()));

        Assert.assertEquals(deltaM.getReal(), deltaE.getReal() - delta.getReal(), Utils.epsilonAngle
                     * FastMath.abs(deltaE.getReal() - delta.getReal()));

        // for final orbit
        T ex = finalOrbit.getEquinoctialEx();
        T ey = finalOrbit.getEquinoctialEy();
        T hx = finalOrbit.getHx();
        T hy = finalOrbit.getHy();
        T LE = finalOrbit.getLE();

        T ex2 = ex.multiply( ex);
        T ey2 = ey.multiply( ey);
        T hx2 = hx.multiply( hx);
        T hy2 = hy.multiply( hy);
        T h2p1 = hx2.add(1).add(hy2);
        T beta = ex2.negate().add(1.).subtract(ey2).sqrt().add(1).reciprocal();

        T x3 = ex.negate().add(beta.negate().multiply(ey2).add(1).multiply(LE.cos())).add(
               beta.multiply(ex).multiply(ey).multiply(LE.sin()));
        T y3 = ey.negate().add(beta.negate().multiply(ex2).add(1).multiply(LE.sin())).add(
              beta.multiply(ex).multiply(ey).multiply(LE.cos()));

        FieldVector3D<T> U = new FieldVector3D<T>(hx2.subtract(hy2).add(1.).divide(h2p1),
                                                  hx.multiply(2).multiply(hy).divide(h2p1),
                                                  hy.multiply(-2.).divide(h2p1));
        FieldVector3D<T> V = new FieldVector3D<T>(hx.multiply(2.).multiply(hy).divide(h2p1),
                                                  hy2.subtract(hx2).add(1.).divide(h2p1),
                                                  hx.multiply(2).divide(h2p1));
        FieldVector3D<T> r = new FieldVector3D<T>(finalOrbit.getA(), (new FieldVector3D<T>(x3, U, y3, V)));

        Assert.assertEquals(finalOrbit.getPVCoordinates().getPosition().getNorm().getReal(), r.getNorm().getReal(),
                     Utils.epsilonTest * r.getNorm().getReal());

    }

    @Test
    public void undergroundOrbit() throws OrekitException {
        doUndergroundOrbit(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doUndergroundOrbit(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        // for a semi major axis < equatorial radius
        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(7.0e6),zero.add( 1.0e6), zero.add(4.0e6));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-500.0), zero.add(800.0), zero.add(100.0));
        FieldAbsoluteDate<T> initDate = date;
        FieldOrbit<T> initialOrbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position, velocity),
                                                  FramesFactory.getEME2000(), initDate, provider.getMu());
        try {
            // Extrapolator definition
            // -----------------------
            FieldEcksteinHechlerPropagator<T> extrapolator =
                            new FieldEcksteinHechlerPropagator<T>(initialOrbit, provider);

            // Extrapolation at the initial date
            // ---------------------------------
            double delta_t = 0.0;
            FieldAbsoluteDate<T> extrapDate = initDate.shiftedBy(delta_t);
            extrapolator.propagate(extrapDate);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.TRAJECTORY_INSIDE_BRILLOUIN_SPHERE, oe.getSpecifier());
        }
    }

    @Test
    public void equatorialOrbit() throws OrekitException {
        doEquatorialOrbit(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doEquatorialOrbit(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);

        FieldAbsoluteDate<T> initDate = date;
        FieldOrbit<T> initialOrbit = new FieldCircularOrbit<T>(zero.add(7000000), zero.add(1.0e-4), zero.add(-1.5e-4),
                                               zero, zero.add(1.2), zero.add(2.3), PositionAngle.MEAN,
                                               FramesFactory.getEME2000(),
                                               initDate, provider.getMu());
        try {
            // Extrapolator definition
            // -----------------------
            FieldEcksteinHechlerPropagator<T> extrapolator =
                            new FieldEcksteinHechlerPropagator<T>(initialOrbit, provider);

            // Extrapolation at the initial date
            // ---------------------------------
            double delta_t = 0.0;
            FieldAbsoluteDate<T> extrapDate = initDate.shiftedBy(delta_t);
            extrapolator.propagate(extrapDate);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.ALMOST_EQUATORIAL_ORBIT, oe.getSpecifier());
        }
    }

    @Test
    public void criticalInclination() throws OrekitException {
        doCriticalInclination(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doCriticalInclination(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<T>(field);
        FieldOrbit<T> initialOrbit = new FieldCircularOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(zero.add(-3862363.8474653554),
                                                                                                zero.add(-3521533.9758022362),
                                                                                                zero.add(4647637.852558916)),
                                                                 new FieldVector3D<T>(zero.add(65.36170817232278),
                                                                                 zero.add(-6056.563439401233),
                                                                                 zero.add(-4511.1247889782757))),
                                               FramesFactory.getEME2000(),
                                               initDate, provider.getMu());

        try {
            // Extrapolator definition
            // -----------------------
            FieldEcksteinHechlerPropagator<T> extrapolator =
                            new FieldEcksteinHechlerPropagator<T>(initialOrbit, provider);

            // Extrapolation at the initial date
            // ---------------------------------
            double delta_t = 0.0;
            FieldAbsoluteDate<T> extrapDate = initDate.shiftedBy(delta_t);
            extrapolator.propagate(extrapDate);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.ALMOST_CRITICALLY_INCLINED_ORBIT, oe.getSpecifier());
        }
    }

    @Test
    public void tooEllipticalOrbit() throws OrekitException {
        doTooEllipticalOrbit(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTooEllipticalOrbit(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        // for an eccentricity too big for the model
        FieldVector3D<T> position = new FieldVector3D<T>(zero.add(7.0e6), zero.add(1.0e6),zero.add( 4.0e6));
        FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-500.0),zero.add( 8000.0), zero.add(1000.0));
        FieldAbsoluteDate<T> initDate = date;
        FieldOrbit<T> initialOrbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position, velocity),
                                                  FramesFactory.getEME2000(), initDate, provider.getMu());
        try {
            // Extrapolator definition
            // -----------------------
            FieldEcksteinHechlerPropagator<T> extrapolator =
                            new FieldEcksteinHechlerPropagator<T>(initialOrbit, provider);

            // Extrapolation at the initial date
            // ---------------------------------
            double delta_t = 0.0;
            FieldAbsoluteDate<T> extrapDate = initDate.shiftedBy(delta_t);
            extrapolator.propagate(extrapDate);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.TOO_LARGE_ECCENTRICITY_FOR_PROPAGATION_MODEL, oe.getSpecifier());
        }
    }

    @Test
    public void hyperbolic() throws OrekitException {
        doHyperbolic(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doHyperbolic(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        FieldKeplerianOrbit<T> hyperbolic =
            new FieldKeplerianOrbit<T>(zero.add(-1.0e10), zero.add(2), zero, zero, zero, zero, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), date, 3.986004415e14);
        try {
            FieldEcksteinHechlerPropagator<T> propagator =
                            new FieldEcksteinHechlerPropagator<T>(hyperbolic, provider);
            propagator.propagate(date.shiftedBy(10.0));
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.TRAJECTORY_INSIDE_BRILLOUIN_SPHERE, oe.getSpecifier());
        }
    }

    @Test
    public void wrongAttitude() throws OrekitException {
        doWrongAttitude(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doWrongAttitude(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<T>(zero.add(1.0e10), zero.add(1.0e-4), zero.add(1.0e-2), zero, zero, zero, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), date, 3.986004415e14);
        final DummyLocalizable gasp = new DummyLocalizable("gasp");
        FieldAttitudeProvider<T> wrongLaw = new FieldAttitudeProvider<T>() {
            public FieldAttitude<T> getAttitude(FieldPVCoordinatesProvider<T> pvProv, FieldAbsoluteDate<T> date, Frame frame) throws OrekitException {
                throw new OrekitException(gasp, new RuntimeException());
            }
        };
        try {
            FieldEcksteinHechlerPropagator<T> propagator =
                            new FieldEcksteinHechlerPropagator<T>(orbit, wrongLaw, provider);
            propagator.propagate(date.shiftedBy(10.0));
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertSame(gasp, oe.getSpecifier());
        }
    }

    @Test
    public void testAcceleration() throws OrekitException {
        doTestAcceleration(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestAcceleration(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<T>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                               FramesFactory.getEME2000(), date, provider.getMu());
        FieldEcksteinHechlerPropagator<T> propagator =
            new FieldEcksteinHechlerPropagator<T>(orbit, provider);
        FieldAbsoluteDate<T> target = date.shiftedBy(10000.0);
        List<TimeStampedFieldPVCoordinates<T>> sample = new ArrayList<TimeStampedFieldPVCoordinates<T>>();
        for (double dt : Arrays.asList(-0.5, 0.0, 0.5)) {
            sample.add(propagator.propagate(target.shiftedBy(dt)).getPVCoordinates());
        }
        TimeStampedFieldPVCoordinates<T> interpolated =
                TimeStampedFieldPVCoordinates.interpolate(target, CartesianDerivativesFilter.USE_P, sample);
        FieldVector3D<T> computedP     = sample.get(1).getPosition();
        FieldVector3D<T> computedV     = sample.get(1).getVelocity();
        FieldVector3D<T> referenceP    = interpolated.getPosition();
        FieldVector3D<T> referenceV    = interpolated.getVelocity();
        FieldVector3D<T> computedA     = sample.get(1).getAcceleration();
        FieldVector3D<T> referenceA    = interpolated.getAcceleration();
        final FieldCircularOrbit<T> propagated = (FieldCircularOrbit<T>) OrbitType.CIRCULAR.convertType(propagator.propagateOrbit(target));
        final FieldCircularOrbit<T> keplerian =
                new FieldCircularOrbit<T>(propagated.getA(),
                                  propagated.getCircularEx(),
                                  propagated.getCircularEy(),
                                  propagated.getI(),
                                  propagated.getRightAscensionOfAscendingNode(),
                                  propagated.getAlphaM(), PositionAngle.MEAN,
                                  propagated.getFrame(),
                                  propagated.getDate(),
                                  propagated.getMu());
        FieldVector3D<T> keplerianP    = keplerian.getPVCoordinates().getPosition();
        FieldVector3D<T> keplerianV    = keplerian.getPVCoordinates().getVelocity();
        FieldVector3D<T> keplerianA    = keplerian.getPVCoordinates().getAcceleration();

        // perturbed orbit position should be similar to Keplerian orbit position
        Assert.assertEquals(0.0, FieldVector3D.distance(referenceP, computedP).getReal(), 1.0e-15);
        Assert.assertEquals(0.0, FieldVector3D.distance(referenceP, keplerianP).getReal(), 4.0e-9);

        // perturbed orbit velocity should be equal to Keplerian orbit because
        // it was in fact reconstructed from Cartesian coordinates
        T computationErrorV   = FieldVector3D.distance(referenceV, computedV);
        T nonKeplerianEffectV = FieldVector3D.distance(referenceV, keplerianV);
        Assert.assertEquals(0.0, nonKeplerianEffectV.getReal() - computationErrorV.getReal(), 9.0e-12);
        Assert.assertEquals(2.2e-4, computationErrorV.getReal(), 3.0e-6);

        // perturbed orbit acceleration should be different from Keplerian orbit because
        // Keplerian orbit doesn't take orbit shape changes into account
        // perturbed orbit acceleration should be consistent with position evolution
        T computationErrorA   = FieldVector3D.distance(referenceA, computedA);
        T nonKeplerianEffectA = FieldVector3D.distance(referenceA, keplerianA);
        Assert.assertEquals(1.0e-7,  computationErrorA.getReal(), 6.0e-9);
        Assert.assertEquals(6.37e-3, nonKeplerianEffectA.getReal(), 7.0e-6);
        Assert.assertTrue(computationErrorA.getReal() < nonKeplerianEffectA.getReal() / 60000);

    }

    @Test
    public void ascendingNode() throws OrekitException {
        doAscendingNode(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doAscendingNode(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<T>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                               FramesFactory.getEME2000(), date, provider.getMu());
        FieldEcksteinHechlerPropagator<T> propagator =
            new FieldEcksteinHechlerPropagator<T>(orbit, provider);
        FieldNodeDetector<T> detector = new FieldNodeDetector<T>(orbit, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Assert.assertTrue(FramesFactory.getITRF(IERSConventions.IERS_2010, true) == detector.getFrame());
        propagator.addEventDetector(detector);

        FieldAbsoluteDate<T> farTarget = date.shiftedBy(10000.0);

        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);

        FieldPVCoordinates<T> pv = propagated.getPVCoordinates(FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() > 3500.0);
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() < 4000.0);

        Assert.assertEquals(0, pv.getPosition().getZ().getReal(), 1.0e-6);
        Assert.assertTrue(pv.getVelocity().getZ().getReal() > 0);
        Collection<FieldEventDetector<T>> detectors = propagator.getEventsDetectors();
        Assert.assertEquals(1, detectors.size());
        propagator.clearEventsDetectors();
        Assert.assertEquals(0, propagator.getEventsDetectors().size());
    }

    @Test
    public void stopAtTargetDate() throws OrekitException {
        doStopAtTargetDate(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doStopAtTargetDate(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<T>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                               FramesFactory.getEME2000(), date, 3.986004415e14);
        FieldEcksteinHechlerPropagator<T> propagator =
            new FieldEcksteinHechlerPropagator<T>(orbit, provider);
        Frame itrf =  FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        propagator.addEventDetector(new FieldNodeDetector<T>(orbit, itrf).withHandler(
                                              new FieldContinueOnEvent<FieldNodeDetector<T>, T>()));
        FieldAbsoluteDate<T> farTarget = orbit.getDate().shiftedBy(10000.0);
        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);
        Assert.assertEquals(0.0, FastMath.abs(farTarget.durationFrom(propagated.getDate()).getReal()), 1.0e-3);
    }

    @Test
    public void perigee() throws OrekitException {
        doPerigee(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doPerigee(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<T>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                               FramesFactory.getEME2000(), date, provider.getMu());
        FieldEcksteinHechlerPropagator<T> propagator =
            new FieldEcksteinHechlerPropagator<T>(orbit, provider);
        propagator.addEventDetector(new FieldApsideDetector<T>(orbit));
        FieldAbsoluteDate<T> farTarget = date.shiftedBy(10000.0);
        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);
        FieldPVCoordinates<T> pv = propagated.getPVCoordinates(FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() > 3000.0);
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() < 3500.0);
        Assert.assertEquals(orbit.getA().getReal() * (1.0 - orbit.getE().getReal()), pv.getPosition().getNorm().getReal(), 410);
    }

    @Test
    public void date() throws OrekitException {
        doDate(Decimal64Field.getInstance());
        
    }

    private <T extends RealFieldElement<T>> void doDate(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<T>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                               FramesFactory.getEME2000(), date, 3.986004415e14);
        FieldEcksteinHechlerPropagator<T> propagator =
            new FieldEcksteinHechlerPropagator<T>(orbit, provider);
        final FieldAbsoluteDate<T> stopDate = date.shiftedBy(500.0);
        propagator.addEventDetector(new FieldDateDetector<T>(stopDate));
        FieldAbsoluteDate<T> farTarget = date.shiftedBy(10000.0);
        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);
        Assert.assertEquals(0, stopDate.durationFrom(propagated.getDate()).getReal(), 1.0e-10);
    }

    @Test
    public void fixedStep() throws OrekitException {
        
        doFixedStep(Decimal64Field.getInstance());
        
        
    }

    private <T extends RealFieldElement<T>> void doFixedStep(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<T>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                               FramesFactory.getEME2000(), date, 3.986004415e14);
        FieldEcksteinHechlerPropagator<T> propagator =
            new FieldEcksteinHechlerPropagator<T>(orbit, provider);
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
        FieldAbsoluteDate<T> farTarget = date.shiftedBy(10000.0);
        propagator.propagate(farTarget);
    }

    @Test
    public void setting() throws OrekitException {
        
        doSetting(Decimal64Field.getInstance());
        
    }

    private <T extends RealFieldElement<T>> void doSetting(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field);
        final FieldKeplerianOrbit<T> orbit =
            new FieldKeplerianOrbit<T>(zero.add(7.8e6), zero.add(0.032), zero.add(0.4), zero.add(0.1), zero.add(0.2), zero.add(0.3), PositionAngle.TRUE,
                               FramesFactory.getEME2000(), date, 3.986004415e14);
        FieldEcksteinHechlerPropagator<T> propagator =
            new FieldEcksteinHechlerPropagator<T>(orbit, provider);
        final OneAxisEllipsoid earthShape =
            new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final TopocentricFrame topo =
            new TopocentricFrame(earthShape, new GeodeticPoint(0.389, -2.962, 0), null);
        FieldElevationDetector<T> detector = new FieldElevationDetector<T>(zero.add(60), zero.add(1.0e-9), topo).withConstantElevation(0.09);
        Assert.assertEquals(0.09, detector.getMinElevation(), 1.0e-12);
        Assert.assertTrue(topo == detector.getTopocentricFrame());
        propagator.addEventDetector(detector);
        
        FieldAbsoluteDate<T> farTarget = date.shiftedBy(10000.0);
        FieldSpacecraftState<T> propagated = propagator.propagate(farTarget);
        final double elevation = topo.getElevation(propagated.getPVCoordinates().getPosition().toVector3D(),
                                                   propagated.getFrame(),
                                                   propagated.getDate().toAbsoluteDate());
        final double zVelocity = propagated.getPVCoordinates(topo).getVelocity().getZ().getReal();
        Assert.assertTrue(farTarget.durationFrom(propagated.getDate()).getReal() > 7800.0);
        Assert.assertTrue("Incorrect value " + farTarget.durationFrom(propagated.getDate()) + " !< 7900",
                          farTarget.durationFrom(propagated.getDate()).getReal() < 7900.0);
        Assert.assertEquals(0.09, elevation, 1.0e-11);
        Assert.assertTrue(zVelocity < 0);
    }

    private <T extends RealFieldElement<T>> T tangLEmLv(T Lv, T ex, T ey) {
        // tan ((LE - Lv) /2)) =
        return ey.multiply(Lv.cos()).subtract(ex.multiply(Lv.sin())).divide(
         ex.multiply(Lv.cos()).add(1.0).add(ey.multiply(Lv.sin()).add(ex.negate().multiply(ex).add(1.0).subtract(
                                                                 ey.multiply(ey)).sqrt())));

    }

    @After
    public void tearDown() {
        provider = null;
    }

    private UnnormalizedSphericalHarmonicsProvider provider;

}
