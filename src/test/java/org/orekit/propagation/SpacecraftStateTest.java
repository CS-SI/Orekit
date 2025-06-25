/* Copyright 2002-2025 CS GROUP
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
package org.orekit.propagation;

import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalStateException;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.events.Action;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.BodyCenterPointing;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.StaticTransform;
import org.orekit.frames.Transform;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.*;

import java.text.ParseException;


class SpacecraftStateTest {

    @Test
    void testWithAttitudeAbsolutePV() {
        // GIVEN
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                AbsoluteDate.ARBITRARY_EPOCH, new PVCoordinates());
        final SpacecraftState state = new SpacecraftState(absolutePVCoordinates);
        final Attitude attitude = Mockito.mock(Attitude.class);
        Mockito.when(attitude.getDate()).thenReturn(state.getDate());
        Mockito.when(attitude.getReferenceFrame()).thenReturn(state.getFrame());
        // WHEN
        final SpacecraftState stateWithAttitude = state.withAttitude(attitude);
        // THEN
        Assertions.assertEquals(attitude, stateWithAttitude.getAttitude());
        Assertions.assertEquals(state.getMass(), stateWithAttitude.getMass());
        Assertions.assertEquals(state.getAbsPVA(), stateWithAttitude.getAbsPVA());
    }

    @Test
    void testWithMassAbsolutePV() {
        // GIVEN
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                AbsoluteDate.ARBITRARY_EPOCH, new PVCoordinates());
        final SpacecraftState state = new SpacecraftState(absolutePVCoordinates);
        final double expectedMass = 123;
        // WHEN
        final SpacecraftState stateWithMass = state.withMass(expectedMass);
        // THEN
        Assertions.assertEquals(expectedMass, stateWithMass.getMass());
        Assertions.assertEquals(state.getAttitude(), stateWithMass.getAttitude());
        Assertions.assertEquals(state.getAbsPVA(), stateWithMass.getAbsPVA());
    }

    @Test
    void testWithAdditionalDataAndAbsolutePV() {
        // GIVEN
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                AbsoluteDate.ARBITRARY_EPOCH, new PVCoordinates());
        final SpacecraftState state = new SpacecraftState(absolutePVCoordinates);
        final DataDictionary dictionary = Mockito.mock(DataDictionary.class);
        // WHEN
        final SpacecraftState stateWithData = state.withAdditionalData(dictionary);
        // THEN
        Assertions.assertEquals(dictionary.getData(), stateWithData.getAdditionalDataValues().getData());
        Assertions.assertEquals(state.getMass(), stateWithData.getMass());
        Assertions.assertEquals(state.getAttitude(), stateWithData.getAttitude());
        Assertions.assertEquals(state.getAbsPVA(), stateWithData.getAbsPVA());
    }

    @Test
    void testWithAdditionalStatesDerivativesAndAbsolutePV() {
        // GIVEN
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                AbsoluteDate.ARBITRARY_EPOCH, new PVCoordinates());
        final SpacecraftState state = new SpacecraftState(absolutePVCoordinates);
        final DoubleArrayDictionary dictionary = Mockito.mock(DoubleArrayDictionary.class);
        // WHEN
        final SpacecraftState stateWithDerivatives = state.withAdditionalStatesDerivatives(dictionary);
        // THEN
        Assertions.assertEquals(dictionary.getData(), stateWithDerivatives.getAdditionalStatesDerivatives().getData());
        Assertions.assertEquals(state.getAbsPVA(), stateWithDerivatives.getAbsPVA());
        Assertions.assertEquals(state.getAttitude(), stateWithDerivatives.getAttitude());
        Assertions.assertEquals(state.getMass(), stateWithDerivatives.getMass());
    }

    @Test
    void testWithAttitudeAndOrbit() {
        // GIVEN
        final SpacecraftState state = new SpacecraftState(orbit);
        final Attitude attitude = Mockito.mock(Attitude.class);
        Mockito.when(attitude.getDate()).thenReturn(state.getDate());
        Mockito.when(attitude.getReferenceFrame()).thenReturn(state.getFrame());
        // WHEN
        final SpacecraftState stateWithAttitude = state.withAttitude(attitude);
        // THEN
        Assertions.assertEquals(attitude, stateWithAttitude.getAttitude());
        Assertions.assertEquals(state.getMass(), stateWithAttitude.getMass());
        Assertions.assertEquals(state.getOrbit(), stateWithAttitude.getOrbit());
    }

    @Test
    void testWithMassAndOrbit() {
        // GIVEN
        final SpacecraftState state = new SpacecraftState(orbit);
        final double expectedMass = 123;
        // WHEN
        final SpacecraftState stateWithMass = state.withMass(expectedMass);
        // THEN
        Assertions.assertEquals(expectedMass, stateWithMass.getMass());
        Assertions.assertEquals(state.getAttitude(), stateWithMass.getAttitude());
        Assertions.assertEquals(state.getOrbit(), stateWithMass.getOrbit());
    }

    @Test
    void testWithAdditionalDataAndOrbit() {
        // GIVEN
        final SpacecraftState state = new SpacecraftState(orbit);
        final DataDictionary dictionary = Mockito.mock(DataDictionary.class);
        // WHEN
        final SpacecraftState stateWithData = state.withAdditionalData(dictionary);
        // THEN
        Assertions.assertEquals(dictionary, stateWithData.getAdditionalDataValues());
        Assertions.assertEquals(state.getMass(), stateWithData.getMass());
        Assertions.assertEquals(state.getAttitude(), stateWithData.getAttitude());
        Assertions.assertEquals(state.getOrbit(), stateWithData.getOrbit());
    }

    @Test
    void testWithAdditionalStatesDerivativesAndOrbit() {
        // GIVEN
        final SpacecraftState state = new SpacecraftState(orbit);
        final DoubleArrayDictionary dictionary = Mockito.mock(DoubleArrayDictionary.class);
        // WHEN
        final SpacecraftState stateWithDerivatives = state.withAdditionalStatesDerivatives(dictionary);
        // THEN
        Assertions.assertEquals(dictionary.getData(), stateWithDerivatives.getAdditionalStatesDerivatives().getData());
        Assertions.assertEquals(state.getOrbit(), stateWithDerivatives.getOrbit());
        Assertions.assertEquals(state.getAttitude(), stateWithDerivatives.getAttitude());
        Assertions.assertEquals(state.getMass(), stateWithDerivatives.getMass());
    }

    @Test
    void testShiftVsEcksteinHechlerError()
        throws OrekitException {


        // polynomial models for interpolation error in position, velocity, acceleration and attitude
        // these models grow as follows
        //   interpolation time (s)    position error (m)   velocity error (m/s)   acceleration error (m/s²)  attitude error (°)
        //           60                        2                    0.07                  0.002               0.00002
        //          120                       12                    0.3                   0.005               0.00009
        //          300                      170                    1.6                   0.012               0.0009
        //          600                     1200                    5.7                   0.024               0.006
        //          900                     3600                   10.6                   0.034               0.02
        // the expected maximum residuals with respect to these models are about 0.4m, 0.5mm/s, 8μm/s² and 3e-6°
        PolynomialFunction pModel = new PolynomialFunction(1.5664070631933846e-01, 7.5504722733047560e-03, -8.2460562451009510e-05,
                6.9546332080305580e-06, -1.7045365367533077e-09, -4.2187860791066264e-13);
        PolynomialFunction vModel = new PolynomialFunction(-3.5472364019908720e-04, 1.6568103861124980e-05, 1.9637913327830596e-05,
                -3.4248792843039766e-09, -5.6565135131014254e-12, 1.4730170946808630e-15);
        PolynomialFunction aModel = new PolynomialFunction(3.0731707577766896e-06, 3.9770746399850350e-05, 1.9779039254538660e-09,
                8.0263328220724900e-12, -1.5600835252366078e-14, 1.1785257001549687e-18);
        PolynomialFunction rModel = new PolynomialFunction(-2.7689062063188115e-06, 1.7406542538258334e-07, 2.5109795349592287e-09,
                2.0399322661074575e-11, 9.9126348912426750e-15, -3.5015638905729510e-18);

        AbsoluteDate centerDate = orbit.getDate().shiftedBy(100.0);
        SpacecraftState centerState = propagator.propagate(centerDate);
        double maxResidualP = 0;
        double maxResidualV = 0;
        double maxResidualA = 0;
        double maxResidualR = 0;
        final double dtStep = 5;
        for (double dt = dtStep; dt < 900.0; dt += dtStep) {
            SpacecraftState shifted = centerState.shiftedBy(dt);
            SpacecraftState propagated = propagator.propagate(centerDate.shiftedBy(dt));
            PVCoordinates dpv = new PVCoordinates(propagated.getPVCoordinates(), shifted.getPVCoordinates());
            double residualP = pModel.value(dt) - dpv.getPosition().getNorm();
            double residualV = vModel.value(dt) - dpv.getVelocity().getNorm();
            double residualA = aModel.value(dt) - dpv.getAcceleration().getNorm();
            double residualR = rModel.value(dt) -
                               FastMath.toDegrees(Rotation.distance(shifted.getAttitude().getRotation(),
                                                                    propagated.getAttitude().getRotation()));
            maxResidualP = FastMath.max(maxResidualP, FastMath.abs(residualP));
            maxResidualV = FastMath.max(maxResidualV, FastMath.abs(residualV));
            maxResidualA = FastMath.max(maxResidualA, FastMath.abs(residualA));
            maxResidualR = FastMath.max(maxResidualR, FastMath.abs(residualR));

        }

        Assertions.assertEquals(0.40,   maxResidualP, 0.01);
        Assertions.assertEquals(4.9e-4, maxResidualV, 1.0e-5);
        Assertions.assertEquals(7.7e-6, maxResidualA, 1.0e-7);
        Assertions.assertEquals(2.8e-6, maxResidualR, 1.0e-1);

    }

    @Test
    void testDatesConsistency() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new SpacecraftState(orbit, attitudeLaw.getAttitude(orbit.shiftedBy(10.0),
                    orbit.getDate().shiftedBy(10.0), orbit.getFrame()));
        });
    }

    /**
     * Check orbit and attitude dates can be off by a few ulps. I see this when using
     * FixedRate attitude provider.
     */
    @Test
    void testDateConsistencyClose() {
        //setup
        Orbit orbit10Shifts = orbit;
        for (int i = 0; i < 10; i++) {
            orbit10Shifts = orbit10Shifts.shiftedBy(0.1);
        }
        final Orbit orbit1Shift = orbit.shiftedBy(1);
        Attitude shiftedAttitude = attitudeLaw.getAttitude(orbit1Shift, orbit1Shift.getDate(), orbit.getFrame());

        // since Orekit 13, dates are equal
        Assertions.assertEquals(shiftedAttitude.getDate(), orbit10Shifts.getDate());

        //action + verify no exception is thrown
        new SpacecraftState(orbit10Shifts, shiftedAttitude);
    }

    @Test
    void testFramesConsistency() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new SpacecraftState(orbit,
                    new Attitude(orbit.getDate(),
                            FramesFactory.getGCRF(),
                            Rotation.IDENTITY, Vector3D.ZERO, Vector3D.ZERO));
        });
    }

    @Test
    void testTransform()
        throws ParseException, OrekitException {

        double maxDP = 0;
        double maxDV = 0;
        double maxDA = 0;
        for (double t = 0; t < orbit.getKeplerianPeriod(); t += 60) {
            final SpacecraftState state = propagator.propagate(orbit.getDate().shiftedBy(t));
            final Transform transform = state.toTransform().getInverse();
            PVCoordinates pv = transform.transformPVCoordinates(PVCoordinates.ZERO);
            PVCoordinates dPV = new PVCoordinates(pv, state.getPVCoordinates());
            Vector3D mZDirection = transform.transformVector(Vector3D.MINUS_K);
            double alpha = Vector3D.angle(mZDirection, state.getPosition());
            maxDP = FastMath.max(maxDP, dPV.getPosition().getNorm());
            maxDV = FastMath.max(maxDV, dPV.getVelocity().getNorm());
            maxDA = FastMath.max(maxDA, FastMath.toDegrees(alpha));
        }
        Assertions.assertEquals(0.0, maxDP, 1.0e-6);
        Assertions.assertEquals(0.0, maxDV, 1.0e-9);
        Assertions.assertEquals(0.0, maxDA, 1.0e-12);

    }

    @Test
    void testGetAdditionalStateBadType() {
        final SpacecraftState state = new SpacecraftState(orbit).addAdditionalData("string", "hello there");
        OrekitException exception = Assertions.assertThrows(OrekitException.class, () -> state.getAdditionalState("string"));
        Assertions.assertEquals(OrekitMessages.ADDITIONAL_STATE_BAD_TYPE, exception.getSpecifier());
        Assertions.assertEquals("string", exception.getParts()[0]);
    }

    @Test
    void testAdditionalStates() {
        final SpacecraftState state = propagator.propagate(orbit.getDate().shiftedBy(60));
        final SpacecraftState extended =
                        state.
                        addAdditionalData("test-1", new double[] { 1.0, 2.0 }).
                        addAdditionalData("test-2", 42.0);
        Assertions.assertEquals(0, state.getAdditionalDataValues().size());
        Assertions.assertFalse(state.hasAdditionalData("test-1"));
        try {
            state.getAdditionalState("test-1");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_DATA);
            Assertions.assertEquals(oe.getParts()[0], "test-1");
        }
        try {
            state.ensureCompatibleAdditionalStates(extended);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_DATA);
            Assertions.assertTrue(oe.getParts()[0].toString().startsWith("test-"));
        }
        try {
            extended.ensureCompatibleAdditionalStates(state);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_DATA);
            Assertions.assertTrue(oe.getParts()[0].toString().startsWith("test-"));
        }
        try {
            extended.ensureCompatibleAdditionalStates(extended.addAdditionalData("test-2", new double[7]));
            Assertions.fail("an exception should have been thrown");
        } catch (MathIllegalStateException mise) {
            Assertions.assertEquals(LocalizedCoreFormats.DIMENSIONS_MISMATCH, mise.getSpecifier());
            Assertions.assertEquals(7, ((Integer) mise.getParts()[0]).intValue());
        }
        Assertions.assertEquals(2, extended.getAdditionalDataValues().getData().size());
        Assertions.assertTrue(extended.hasAdditionalData("test-1"));
        Assertions.assertTrue(extended.hasAdditionalData("test-2"));
        Assertions.assertEquals( 1.0, extended.getAdditionalState("test-1")[0], 1.0e-15);
        Assertions.assertEquals( 2.0, extended.getAdditionalState("test-1")[1], 1.0e-15);
        Assertions.assertEquals(42.0, extended.getAdditionalState("test-2")[0], 1.0e-15);

        // test various constructors
        DataDictionary dictionary = new DataDictionary();
        dictionary.put("test-3", new double[] { -6.0 });
        SpacecraftState sO = new SpacecraftState(state.getOrbit()).withAdditionalData(dictionary);
        Assertions.assertEquals(-6.0, sO.getAdditionalState("test-3")[0], 1.0e-15);

    }

    @Test
    void testAdditionalStatesDerivatives() {
        final SpacecraftState state = propagator.propagate(orbit.getDate().shiftedBy(60));
        final SpacecraftState extended =
                        state.
                        addAdditionalStateDerivative("test-1", new double[] { 1.0, 2.0 }).
                        addAdditionalStateDerivative("test-2", 42.0);
        Assertions.assertEquals(0, state.getAdditionalStatesDerivatives().size());
        Assertions.assertFalse(state.hasAdditionalStateDerivative("test-1"));
        try {
            state.getAdditionalStateDerivative("test-1");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_DATA);
            Assertions.assertEquals(oe.getParts()[0], "test-1");
        }
        try {
            state.ensureCompatibleAdditionalStates(extended);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_DATA);
            Assertions.assertTrue(oe.getParts()[0].toString().startsWith("test-"));
        }
        try {
            extended.ensureCompatibleAdditionalStates(state);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_DATA);
            Assertions.assertTrue(oe.getParts()[0].toString().startsWith("test-"));
        }
        try {
            extended.ensureCompatibleAdditionalStates(extended.addAdditionalStateDerivative("test-2", new double[7]));
            Assertions.fail("an exception should have been thrown");
        } catch (MathIllegalStateException mise) {
            Assertions.assertEquals(LocalizedCoreFormats.DIMENSIONS_MISMATCH, mise.getSpecifier());
            Assertions.assertEquals(7, ((Integer) mise.getParts()[0]).intValue());
        }
        Assertions.assertEquals(2, extended.getAdditionalStatesDerivatives().size());
        Assertions.assertTrue(extended.hasAdditionalStateDerivative("test-1"));
        Assertions.assertTrue(extended.hasAdditionalStateDerivative("test-2"));
        Assertions.assertEquals( 1.0, extended.getAdditionalStateDerivative("test-1")[0], 1.0e-15);
        Assertions.assertEquals( 2.0, extended.getAdditionalStateDerivative("test-1")[1], 1.0e-15);
        Assertions.assertEquals(42.0, extended.getAdditionalStateDerivative("test-2")[0], 1.0e-15);

        // test most complete constructor
        DoubleArrayDictionary dictionary = new DoubleArrayDictionary();
        dictionary.put("test-3", new double[] { -6.0 });
        SpacecraftState s = new SpacecraftState(state.getOrbit(), state.getAttitude(), state.getMass(), null, dictionary);
        Assertions.assertFalse(s.hasAdditionalData("test-3"));
        Assertions.assertEquals(-6.0, s.getAdditionalStateDerivative("test-3")[0], 1.0e-15);

    }

    @Test
    void testAdditionalTestResetOnEventAnalytical() {

        // Build orbit
        AbsoluteDate date0 = new AbsoluteDate(2000, 1, 1, TimeScalesFactory.getUTC());
        Orbit orbit = new KeplerianOrbit(7.1E6, 0, 0, 0, 0, 0,
                                         PositionAngleType.TRUE, FramesFactory.getGCRF(), date0,
                                         Constants.WGS84_EARTH_MU);

        // Build propagator
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);

        // Create initial state with one additional state and add it to the propagator
        final String name = "A";
        SpacecraftState initialState = new SpacecraftState(orbit).
                                       addAdditionalData(name, new double[] { -1 });

        propagator.resetInitialState(initialState);

        // Create date detector and handler
        AbsoluteDate changeDate = date0.shiftedBy(3);
        DateDetector dateDetector = new DateDetector(changeDate).
                                    withHandler(new EventHandler() {

            @Override
            public Action eventOccurred(SpacecraftState s, EventDetector detector, boolean increasing) {
              return Action.RESET_STATE;
            }

            @Override
            public SpacecraftState resetState(EventDetector detector, SpacecraftState oldState) {
                return oldState.addAdditionalData(name, new double[] { +1 });
            }

        });

        propagator.addEventDetector(dateDetector);
        propagator.setStepHandler(0.125, s -> {
            if (s.getDate().durationFrom(changeDate) < -0.001) {
                Assertions.assertEquals(-1, s.getAdditionalState(name)[0], 1.0e-15);
            } else if (s.getDate().durationFrom(changeDate) > +0.001) {
                Assertions.assertEquals(+1, s.getAdditionalState(name)[0], 1.0e-15);
            }
        });
        SpacecraftState finalState = propagator.propagate(date0, date0.shiftedBy(5));
        Assertions.assertEquals(+1, finalState.getAdditionalState(name)[0], 1.0e-15);

    }

    @Test
    void testAdditionalTestResetOnEventNumerical() {

        // Build orbit
        AbsoluteDate date0 = new AbsoluteDate(2000, 1, 1, TimeScalesFactory.getUTC());
        Orbit orbit = new KeplerianOrbit(7.1E6, 0, 0, 0, 0, 0,
                                         PositionAngleType.TRUE, FramesFactory.getGCRF(), date0,
                                         Constants.WGS84_EARTH_MU);

        // Build propagator
        ODEIntegrator odeIntegrator = new DormandPrince853Integrator(1E-3, 1E3, 1E-6, 1E-6);
        NumericalPropagator propagator = new NumericalPropagator(odeIntegrator);

        // Create initial state with one additional state and add it to the propagator
        final String name = "A";
        SpacecraftState initialState = new SpacecraftState(orbit).
                        addAdditionalData(name, new double[] { -1 });

        propagator.setInitialState(initialState);

        // Create date detector and handler
        AbsoluteDate changeDate = date0.shiftedBy(3);
        DateDetector dateDetector = new DateDetector(changeDate).
                                    withHandler(new EventHandler() {

            @Override
            public Action eventOccurred(SpacecraftState s, EventDetector detector, boolean increasing) {
                return Action.RESET_STATE;
            }

            @Override
            public SpacecraftState resetState(EventDetector detector, SpacecraftState oldState) {
                return oldState.addAdditionalData(name, new double[] { +1 });
            }

        });

        propagator.addEventDetector(dateDetector);
        propagator.setStepHandler(0.125, s -> {
            if (s.getDate().durationFrom(changeDate) < -0.001) {
                Assertions.assertEquals(-1, s.getAdditionalState(name)[0], 1.0e-15);
            } else if (s.getDate().durationFrom(changeDate) > +0.001) {
                Assertions.assertEquals(+1, s.getAdditionalState(name)[0], 1.0e-15);
            }
        });
        SpacecraftState finalState = propagator.propagate(date0, date0.shiftedBy(5));
        Assertions.assertEquals(+1, finalState.getAdditionalState(name)[0], 1.0e-15);

    }

    @Test
    void testAdditionalStatesAbsPV() {

        double x_f     = 0.8;
        double y_f     = 0.2;
        double z_f     = 0;
        double vx_f    = 0;
        double vy_f    = 0;
        double vz_f    = 0.1;

        AbsoluteDate date = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                                            TimeComponents.H00,
                                                            TimeScalesFactory.getUTC());

        PVCoordinates pva_f = new PVCoordinates(new Vector3D(x_f,y_f,z_f), new Vector3D(vx_f,vy_f,vz_f));

        AbsolutePVCoordinates absPV_f = new AbsolutePVCoordinates(FramesFactory.getEME2000(), date, pva_f);

        NumericalPropagator prop = new NumericalPropagator(new DormandPrince853Integrator(0.1, 500, 0.001, 0.001));
        prop.setOrbitType(null);

        final SpacecraftState initialState = new SpacecraftState(absPV_f);

        prop.resetInitialState(initialState);

        final SpacecraftState state = prop.propagate(absPV_f.getDate().shiftedBy(60));
        double[] add = new double[2];
        add[0] = 1.;
        add[1] = 2.;
        final SpacecraftState extended =
                state.
                 addAdditionalData("test-1", add).
                  addAdditionalData("test-2", 42.0);
        Assertions.assertEquals(0, state.getAdditionalDataValues().size());
        Assertions.assertFalse(state.hasAdditionalData("test-1"));
        try {
            state.getAdditionalState("test-1");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_DATA);
            Assertions.assertEquals(oe.getParts()[0], "test-1");
        }
        try {
            state.ensureCompatibleAdditionalStates(extended);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_DATA);
            Assertions.assertTrue(oe.getParts()[0].toString().startsWith("test-"));
        }
        try {
            extended.ensureCompatibleAdditionalStates(state);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_DATA);
            Assertions.assertTrue(oe.getParts()[0].toString().startsWith("test-"));
        }
        try {
            double[] kk = new double[7];
            extended.ensureCompatibleAdditionalStates(extended.addAdditionalData("test-2", kk));
            Assertions.fail("an exception should have been thrown");
        } catch (MathIllegalStateException mise) {
            Assertions.assertEquals(LocalizedCoreFormats.DIMENSIONS_MISMATCH, mise.getSpecifier());
            Assertions.assertEquals(7, ((Integer) mise.getParts()[0]).intValue());
        }
        Assertions.assertEquals(2, extended.getAdditionalDataValues().size());
        Assertions.assertTrue(extended.hasAdditionalData("test-1"));
        Assertions.assertTrue(extended.hasAdditionalData("test-2"));
        Assertions.assertEquals( 1.0, extended.getAdditionalState("test-1")[0], 1.0e-15);
        Assertions.assertEquals( 2.0, extended.getAdditionalState("test-1")[1], 1.0e-15);
        Assertions.assertEquals(42.0, extended.getAdditionalState("test-2")[0], 1.0e-15);

        // test various constructors
        double[] dd = new double[1];
        dd[0] = -6.0;
        DataDictionary additional = new DataDictionary();
        additional.put("test-3", dd);
        SpacecraftState sO = state.withAdditionalData(additional);
        Assertions.assertEquals(-6.0, sO.getAdditionalState("test-3")[0], 1.0e-15);

    }

    @Test
    void testAdditionalStatesDerivativesAbsPV() {

        double x_f     = 0.8;
        double y_f     = 0.2;
        double z_f     = 0;
        double vx_f    = 0;
        double vy_f    = 0;
        double vz_f    = 0.1;

        AbsoluteDate date = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                                            TimeComponents.H00,
                                                            TimeScalesFactory.getUTC());

        PVCoordinates pva_f = new PVCoordinates(new Vector3D(x_f,y_f,z_f), new Vector3D(vx_f,vy_f,vz_f));

        AbsolutePVCoordinates absPV_f = new AbsolutePVCoordinates(FramesFactory.getEME2000(), date, pva_f);

        NumericalPropagator prop = new NumericalPropagator(new DormandPrince853Integrator(0.1, 500, 0.001, 0.001));
        prop.setOrbitType(null);

        final SpacecraftState initialState = new SpacecraftState(absPV_f);

        prop.resetInitialState(initialState);

        final SpacecraftState state = prop.propagate(absPV_f.getDate().shiftedBy(60));
        double[] add = new double[2];
        add[0] = 1.;
        add[1] = 2.;
        final SpacecraftState extended =
                state.
                 addAdditionalStateDerivative("test-1", add).
                  addAdditionalStateDerivative("test-2", 42.0);
        Assertions.assertEquals(0, state.getAdditionalStatesDerivatives().getData().size());
        Assertions.assertFalse(state.hasAdditionalStateDerivative("test-1"));
        try {
            state.getAdditionalStateDerivative("test-1");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_DATA);
            Assertions.assertEquals(oe.getParts()[0], "test-1");
        }
        try {
            state.ensureCompatibleAdditionalStates(extended);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_DATA);
            Assertions.assertTrue(oe.getParts()[0].toString().startsWith("test-"));
        }
        try {
            extended.ensureCompatibleAdditionalStates(state);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_DATA);
            Assertions.assertTrue(oe.getParts()[0].toString().startsWith("test-"));
        }
        try {
            double[] kk = new double[7];
            extended.ensureCompatibleAdditionalStates(extended.addAdditionalStateDerivative("test-2", kk));
            Assertions.fail("an exception should have been thrown");
        } catch (MathIllegalStateException mise) {
            Assertions.assertEquals(LocalizedCoreFormats.DIMENSIONS_MISMATCH, mise.getSpecifier());
            Assertions.assertEquals(7, ((Integer) mise.getParts()[0]).intValue());
        }
        Assertions.assertEquals(2, extended.getAdditionalStatesDerivatives().getData().size());
        Assertions.assertTrue(extended.hasAdditionalStateDerivative("test-1"));
        Assertions.assertTrue(extended.hasAdditionalStateDerivative("test-2"));
        Assertions.assertEquals( 1.0, extended.getAdditionalStateDerivative("test-1")[0], 1.0e-15);
        Assertions.assertEquals( 2.0, extended.getAdditionalStateDerivative("test-1")[1], 1.0e-15);
        Assertions.assertEquals(42.0, extended.getAdditionalStateDerivative("test-2")[0], 1.0e-15);

        // test most complete constructor
        double[] dd = new double[1];
        dd[0] = -6.0;
        DoubleArrayDictionary dict = new DoubleArrayDictionary();
        dict.put("test-3", dd);
        SpacecraftState s = new SpacecraftState(state.getAbsPVA(), state.getAttitude(), state.getMass(), null, dict);
        Assertions.assertEquals(-6.0, s.getAdditionalStateDerivative("test-3")[0], 1.0e-15);

    }

    @Test
    void testShiftAdditionalDerivatives() {

        final String valueAndDerivative = "value-and-derivative";
        final String valueOnly          = "value-only";
        final String derivativeOnly     = "derivative-only";
        final SpacecraftState s0 = propagator.getInitialState().
                                   addAdditionalData(valueAndDerivative,           new double[] { 1.0,  2.0 }).
                                   addAdditionalStateDerivative(valueAndDerivative, new double[] { 3.0,  2.0 }).
                                   addAdditionalData(valueOnly,                    new double[] { 5.0,  4.0 }).
                                   addAdditionalStateDerivative(derivativeOnly,     new double[] { 1.0, -1.0 });
        Assertions.assertEquals( 1.0, s0.getAdditionalState(valueAndDerivative)[0],           1.0e-15);
        Assertions.assertEquals( 2.0, s0.getAdditionalState(valueAndDerivative)[1],           1.0e-15);
        Assertions.assertEquals( 3.0, s0.getAdditionalStateDerivative(valueAndDerivative)[0], 1.0e-15);
        Assertions.assertEquals( 2.0, s0.getAdditionalStateDerivative(valueAndDerivative)[1], 1.0e-15);
        Assertions.assertEquals( 5.0, s0.getAdditionalState(valueOnly)[0],                    1.0e-15);
        Assertions.assertEquals( 4.0, s0.getAdditionalState(valueOnly)[1],                    1.0e-15);
        Assertions.assertEquals( 1.0, s0.getAdditionalStateDerivative(derivativeOnly)[0],     1.0e-15);
        Assertions.assertEquals(-1.0, s0.getAdditionalStateDerivative(derivativeOnly)[1],     1.0e-15);
        final SpacecraftState s1 = s0.shiftedBy(-2.0);
        Assertions.assertEquals(-5.0, s1.getAdditionalState(valueAndDerivative)[0],           1.0e-15);
        Assertions.assertEquals(-2.0, s1.getAdditionalState(valueAndDerivative)[1],           1.0e-15);
        Assertions.assertEquals( 3.0, s1.getAdditionalStateDerivative(valueAndDerivative)[0], 1.0e-15);
        Assertions.assertEquals( 2.0, s1.getAdditionalStateDerivative(valueAndDerivative)[1], 1.0e-15);
        Assertions.assertEquals( 5.0, s1.getAdditionalState(valueOnly)[0],                    1.0e-15);
        Assertions.assertEquals( 4.0, s1.getAdditionalState(valueOnly)[1],                    1.0e-15);
        Assertions.assertEquals( 1.0, s1.getAdditionalStateDerivative(derivativeOnly)[0],     1.0e-15);
        Assertions.assertEquals(-1.0, s1.getAdditionalStateDerivative(derivativeOnly)[1],     1.0e-15);

    }

    @Test
    void testToStaticTransform() {
        // GIVEN
        final SpacecraftState state = new SpacecraftState(orbit,
                attitudeLaw.getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        // WHEN
        final StaticTransform actualStaticTransform = state.toStaticTransform();
        // THEN
        final StaticTransform expectedStaticTransform = state.toTransform();
        Assertions.assertEquals(expectedStaticTransform.getDate(), actualStaticTransform.getDate());
        Assertions.assertEquals(expectedStaticTransform.getTranslation(), actualStaticTransform.getTranslation());
        Assertions.assertEquals(0., Rotation.distance(expectedStaticTransform.getRotation(),
                actualStaticTransform.getRotation()));
    }

    @Test
    public void testIssue1557() {
        // GIVEN
        // Define orbit state
        final SpacecraftState orbitState = new SpacecraftState(TestUtils.getFakeOrbit());

        // Define PVA state
        final SpacecraftState pvaState = new SpacecraftState(TestUtils.getFakeAbsolutePVCoordinates());

        // WHEN
        final Vector3D pvaVelocity   = pvaState.getVelocity();
        final Vector3D orbitVelocity = orbitState.getVelocity();

        // THEN
        Assertions.assertEquals(pvaState.getVelocity(), pvaVelocity);
        Assertions.assertEquals(orbitState.getVelocity(), orbitVelocity);
    }

    @BeforeEach
    public void setUp() {
        try {
        Utils.setDataRoot("regular-data");
        double mu  = 3.9860047e14;
        double ae  = 6.378137e6;
        double c20 = -1.08263e-3;
        double c30 = 2.54e-6;
        double c40 = 1.62e-6;
        double c50 = 2.3e-7;
        double c60 = -5.5e-7;

        mass = 2500;
        double a = 7187990.1979844316;
        double e = 0.5e-4;
        double i = 1.7105407051081795;
        double omega = 1.9674147913622104;
        double OMEGA = FastMath.toRadians(261);
        double lv = 0;

        AbsoluteDate date = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                                 TimeComponents.H00,
                                                 TimeScalesFactory.getUTC());
        orbit = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngleType.TRUE,
                                   FramesFactory.getEME2000(), date, mu);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        attitudeLaw = new BodyCenterPointing(orbit.getFrame(), earth);
        propagator =
            new EcksteinHechlerPropagator(orbit, attitudeLaw, mass,
                                          ae, mu, c20, c30, c40, c50, c60);

        } catch (OrekitException oe) {
            Assertions.fail(oe.getLocalizedMessage());
        }
    }

    @AfterEach
    public void tearDown() {
        mass  = Double.NaN;
        orbit = null;
        attitudeLaw = null;
        propagator = null;
    }

    private double mass;
    private Orbit orbit;
    private AttitudeProvider attitudeLaw;
    private Propagator propagator;

}
