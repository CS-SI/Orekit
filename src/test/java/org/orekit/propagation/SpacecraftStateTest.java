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
package org.orekit.propagation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;

import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalStateException;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.events.Action;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;


public class SpacecraftStateTest {

    @Test
    public void testShiftVsEcksteinHechlerError()
        throws ParseException, OrekitException {


        // polynomial models for interpolation error in position, velocity, acceleration and attitude
        // these models grow as follows
        //   interpolation time (s)    position error (m)   velocity error (m/s)   acceleration error (m/s²)  attitude error (°)
        //           60                        2                    0.07                  0.002               0.00002
        //          120                       12                    0.3                   0.005               0.00009
        //          300                      170                    1.6                   0.012               0.0009
        //          600                     1200                    5.7                   0.024               0.006
        //          900                     3600                   10.6                   0.034               0.02
        // the expected maximum residuals with respect to these models are about 0.4m, 0.5mm/s, 8μm/s² and 3e-6°
        PolynomialFunction pModel = new PolynomialFunction(new double[] {
            1.5664070631933846e-01,  7.5504722733047560e-03, -8.2460562451009510e-05,
            6.9546332080305580e-06, -1.7045365367533077e-09, -4.2187860791066264e-13
        });
        PolynomialFunction vModel = new PolynomialFunction(new double[] {
           -3.5472364019908720e-04,  1.6568103861124980e-05,  1.9637913327830596e-05,
           -3.4248792843039766e-09, -5.6565135131014254e-12,  1.4730170946808630e-15
        });
        PolynomialFunction aModel = new PolynomialFunction(new double[] {
            3.0731707577766896e-06,  3.9770746399850350e-05,  1.9779039254538660e-09,
            8.0263328220724900e-12, -1.5600835252366078e-14,  1.1785257001549687e-18
        });
        PolynomialFunction rModel = new PolynomialFunction(new double[] {
           -2.7689062063188115e-06,  1.7406542538258334e-07,  2.5109795349592287e-09,
            2.0399322661074575e-11,  9.9126348912426750e-15, -3.5015638905729510e-18
        });

        AbsoluteDate centerDate = orbit.getDate().shiftedBy(100.0);
        SpacecraftState centerState = propagator.propagate(centerDate);
        double maxResidualP = 0;
        double maxResidualV = 0;
        double maxResidualA = 0;
        double maxResidualR = 0;
        for (double dt = 0; dt < 900.0; dt += 5) {
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
    public void testDatesConsistency() {
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
    public void testDateConsistencyClose() {
        //setup
        Orbit orbit10Shifts = orbit;
        for (int i = 0; i < 10; i++) {
            orbit10Shifts = orbit10Shifts.shiftedBy(0.1);
        }
        final Orbit orbit1Shift = orbit.shiftedBy(1);
        Attitude shiftedAttitude = attitudeLaw
                .getAttitude(orbit1Shift, orbit1Shift.getDate(), orbit.getFrame());

        //verify dates are very close, but not equal
        Assertions.assertNotEquals(shiftedAttitude.getDate(), orbit10Shifts.getDate());
        Assertions.assertEquals(
                shiftedAttitude.getDate().durationFrom(orbit10Shifts.getDate()),
                0, Precision.EPSILON);

        //action + verify no exception is thrown
        new SpacecraftState(orbit10Shifts, shiftedAttitude);
    }

    @Test
    public void testFramesConsistency() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new SpacecraftState(orbit,
                    new Attitude(orbit.getDate(),
                            FramesFactory.getGCRF(),
                            Rotation.IDENTITY, Vector3D.ZERO, Vector3D.ZERO));
        });
    }

    @Test
    public void testTransform()
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
    public void testAdditionalStates() {
        final SpacecraftState state = propagator.propagate(orbit.getDate().shiftedBy(60));
        final SpacecraftState extended =
                        state.
                        addAdditionalState("test-1", new double[] { 1.0, 2.0 }).
                        addAdditionalState("test-2", 42.0);
        Assertions.assertEquals(0, state.getAdditionalStatesValues().size());
        Assertions.assertFalse(state.hasAdditionalState("test-1"));
        try {
            state.getAdditionalState("test-1");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_STATE);
            Assertions.assertEquals(oe.getParts()[0], "test-1");
        }
        try {
            state.ensureCompatibleAdditionalStates(extended);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_STATE);
            Assertions.assertTrue(oe.getParts()[0].toString().startsWith("test-"));
        }
        try {
            extended.ensureCompatibleAdditionalStates(state);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_STATE);
            Assertions.assertTrue(oe.getParts()[0].toString().startsWith("test-"));
        }
        try {
            extended.ensureCompatibleAdditionalStates(extended.addAdditionalState("test-2", new double[7]));
            Assertions.fail("an exception should have been thrown");
        } catch (MathIllegalStateException mise) {
            Assertions.assertEquals(LocalizedCoreFormats.DIMENSIONS_MISMATCH, mise.getSpecifier());
            Assertions.assertEquals(7, ((Integer) mise.getParts()[0]).intValue());
        }
        Assertions.assertEquals(2, extended.getAdditionalStatesValues().getData().size());
        Assertions.assertTrue(extended.hasAdditionalState("test-1"));
        Assertions.assertTrue(extended.hasAdditionalState("test-2"));
        Assertions.assertEquals( 1.0, extended.getAdditionalState("test-1")[0], 1.0e-15);
        Assertions.assertEquals( 2.0, extended.getAdditionalState("test-1")[1], 1.0e-15);
        Assertions.assertEquals(42.0, extended.getAdditionalState("test-2")[0], 1.0e-15);

        // test various constructors
        DoubleArrayDictionary dictionary = new DoubleArrayDictionary();
        dictionary.put("test-3", new double[] { -6.0 });
        SpacecraftState sO = new SpacecraftState(state.getOrbit(), dictionary);
        Assertions.assertEquals(-6.0, sO.getAdditionalState("test-3")[0], 1.0e-15);
        SpacecraftState sOA = new SpacecraftState(state.getOrbit(), state.getAttitude(), dictionary);
        Assertions.assertEquals(-6.0, sOA.getAdditionalState("test-3")[0], 1.0e-15);
        SpacecraftState sOM = new SpacecraftState(state.getOrbit(), state.getMass(), dictionary);
        Assertions.assertEquals(-6.0, sOM.getAdditionalState("test-3")[0], 1.0e-15);
        SpacecraftState sOAM = new SpacecraftState(state.getOrbit(), state.getAttitude(), state.getMass(), dictionary);
        Assertions.assertEquals(-6.0, sOAM.getAdditionalState("test-3")[0], 1.0e-15);

    }

    @Test
    public void testAdditionalStatesDerivatives() {
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
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_STATE);
            Assertions.assertEquals(oe.getParts()[0], "test-1");
        }
        try {
            state.ensureCompatibleAdditionalStates(extended);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_STATE);
            Assertions.assertTrue(oe.getParts()[0].toString().startsWith("test-"));
        }
        try {
            extended.ensureCompatibleAdditionalStates(state);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_STATE);
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
        Assertions.assertFalse(s.hasAdditionalState("test-3"));
        Assertions.assertEquals(-6.0, s.getAdditionalStateDerivative("test-3")[0], 1.0e-15);

    }

    @Test
    public void testAdditionalTestResetOnEventAnalytical() {

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
                                       addAdditionalState(name, new double[] { -1 });

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
                return oldState.addAdditionalState(name, new double[] { +1 });
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
    public void testAdditionalTestResetOnEventNumerical() {

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
                        addAdditionalState(name, new double[] { -1 });

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
                return oldState.addAdditionalState(name, new double[] { +1 });
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
    public void testSerialization()
            throws IOException, ClassNotFoundException, OrekitException {

        propagator.resetInitialState(propagator.getInitialState().
                                     addAdditionalState("p1", 12.25).
                                     addAdditionalState("p2", 1, 2, 3));
        SpacecraftState state = propagator.propagate(orbit.getDate().shiftedBy(123.456));

        Assertions.assertEquals(2, state.getAdditionalStatesValues().size());
        Assertions.assertEquals(1, state.getAdditionalState("p1").length);
        Assertions.assertEquals(12.25, state.getAdditionalState("p1")[0], 1.0e-15);
        Assertions.assertEquals(3, state.getAdditionalState("p2").length);
        Assertions.assertEquals(1.0, state.getAdditionalState("p2")[0], 1.0e-15);
        Assertions.assertEquals(2.0, state.getAdditionalState("p2")[1], 1.0e-15);
        Assertions.assertEquals(3.0, state.getAdditionalState("p2")[2], 1.0e-15);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(state);

        Assertions.assertTrue(bos.size() >  900);
        Assertions.assertTrue(bos.size() < 1000);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        SpacecraftState deserialized  = (SpacecraftState) ois.readObject();
        Assertions.assertEquals(0.0,
                            Vector3D.distance(state.getPosition(),
                                              deserialized.getPosition()),
                            1.0e-10);
        Assertions.assertEquals(0.0,
                            Vector3D.distance(state.getPVCoordinates().getVelocity(),
                                              deserialized.getPVCoordinates().getVelocity()),
                            1.0e-10);
        Assertions.assertEquals(0.0,
                            Rotation.distance(state.getAttitude().getRotation(),
                                              deserialized.getAttitude().getRotation()),
                            1.0e-10);
        Assertions.assertEquals(0.0,
                            Vector3D.distance(state.getAttitude().getSpin(),
                                              deserialized.getAttitude().getSpin()),
                            1.0e-10);
        Assertions.assertEquals(state.getDate(), deserialized.getDate());
        Assertions.assertEquals(state.getMu(), deserialized.getMu(), 1.0e-10);
        Assertions.assertEquals(state.getFrame().getName(), deserialized.getFrame().getName());
        Assertions.assertEquals(2, deserialized.getAdditionalStatesValues().size());
        Assertions.assertEquals(1, deserialized.getAdditionalState("p1").length);
        Assertions.assertEquals(12.25, deserialized.getAdditionalState("p1")[0], 1.0e-15);
        Assertions.assertEquals(3, deserialized.getAdditionalState("p2").length);
        Assertions.assertEquals(1.0, deserialized.getAdditionalState("p2")[0], 1.0e-15);
        Assertions.assertEquals(2.0, deserialized.getAdditionalState("p2")[1], 1.0e-15);
        Assertions.assertEquals(3.0, deserialized.getAdditionalState("p2")[2], 1.0e-15);

    }

    @Test
    public void testSerializationWithAbsPV()
            throws IOException, ClassNotFoundException, OrekitException {

        final NumericalPropagator numPropagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(10.0));
        final AbsolutePVCoordinates pva = new AbsolutePVCoordinates(orbit.getFrame(), orbit.getDate(),
                                                                    orbit.getPosition(),
                                                                    orbit.getPVCoordinates().getVelocity());
        numPropagator.setOrbitType(null);
        numPropagator.setIgnoreCentralAttraction(true);
        numPropagator.setInitialState(new SpacecraftState(pva).
                                     addAdditionalState("p1", 12.25).
                                     addAdditionalState("p2", 1, 2, 3));
        SpacecraftState state = numPropagator.propagate(pva.getDate().shiftedBy(123.456));

        Assertions.assertEquals(2, state.getAdditionalStatesValues().size());
        Assertions.assertEquals(1, state.getAdditionalState("p1").length);
        Assertions.assertEquals(12.25, state.getAdditionalState("p1")[0], 1.0e-15);
        Assertions.assertEquals(3, state.getAdditionalState("p2").length);
        Assertions.assertEquals(1.0, state.getAdditionalState("p2")[0], 1.0e-15);
        Assertions.assertEquals(2.0, state.getAdditionalState("p2")[1], 1.0e-15);
        Assertions.assertEquals(3.0, state.getAdditionalState("p2")[2], 1.0e-15);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(state);

        Assertions.assertTrue(bos.size() >  900);
        Assertions.assertTrue(bos.size() < 1000);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        SpacecraftState deserialized  = (SpacecraftState) ois.readObject();
        Assertions.assertEquals(0.0,
                            Vector3D.distance(state.getPosition(),
                                              deserialized.getPosition()),
                            1.0e-10);
        Assertions.assertEquals(0.0,
                            Vector3D.distance(state.getPVCoordinates().getVelocity(),
                                              deserialized.getPVCoordinates().getVelocity()),
                            1.0e-10);
        Assertions.assertEquals(0.0,
                            Rotation.distance(state.getAttitude().getRotation(),
                                              deserialized.getAttitude().getRotation()),
                            1.0e-10);
        Assertions.assertEquals(0.0,
                            Vector3D.distance(state.getAttitude().getSpin(),
                                              deserialized.getAttitude().getSpin()),
                            1.0e-10);
        Assertions.assertEquals(state.getDate(), deserialized.getDate());
        Assertions.assertEquals(state.getMu(), deserialized.getMu(), 1.0e-10);
        Assertions.assertEquals(state.getFrame().getName(), deserialized.getFrame().getName());
        Assertions.assertEquals(2, deserialized.getAdditionalStatesValues().size());
        Assertions.assertEquals(1, deserialized.getAdditionalState("p1").length);
        Assertions.assertEquals(12.25, deserialized.getAdditionalState("p1")[0], 1.0e-15);
        Assertions.assertEquals(3, deserialized.getAdditionalState("p2").length);
        Assertions.assertEquals(1.0, deserialized.getAdditionalState("p2")[0], 1.0e-15);
        Assertions.assertEquals(2.0, deserialized.getAdditionalState("p2")[1], 1.0e-15);
        Assertions.assertEquals(3.0, deserialized.getAdditionalState("p2")[2], 1.0e-15);

    }

    @Test
    public void testAdditionalStatesAbsPV() {

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
                 addAdditionalState("test-1", add).
                  addAdditionalState("test-2", 42.0);
        Assertions.assertEquals(0, state.getAdditionalStatesValues().size());
        Assertions.assertFalse(state.hasAdditionalState("test-1"));
        try {
            state.getAdditionalState("test-1");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_STATE);
            Assertions.assertEquals(oe.getParts()[0], "test-1");
        }
        try {
            state.ensureCompatibleAdditionalStates(extended);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_STATE);
            Assertions.assertTrue(oe.getParts()[0].toString().startsWith("test-"));
        }
        try {
            extended.ensureCompatibleAdditionalStates(state);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_STATE);
            Assertions.assertTrue(oe.getParts()[0].toString().startsWith("test-"));
        }
        try {
            double[] kk = new double[7];
            extended.ensureCompatibleAdditionalStates(extended.addAdditionalState("test-2", kk));
            Assertions.fail("an exception should have been thrown");
        } catch (MathIllegalStateException mise) {
            Assertions.assertEquals(LocalizedCoreFormats.DIMENSIONS_MISMATCH, mise.getSpecifier());
            Assertions.assertEquals(7, ((Integer) mise.getParts()[0]).intValue());
        }
        Assertions.assertEquals(2, extended.getAdditionalStatesValues().size());
        Assertions.assertTrue(extended.hasAdditionalState("test-1"));
        Assertions.assertTrue(extended.hasAdditionalState("test-2"));
        Assertions.assertEquals( 1.0, extended.getAdditionalState("test-1")[0], 1.0e-15);
        Assertions.assertEquals( 2.0, extended.getAdditionalState("test-1")[1], 1.0e-15);
        Assertions.assertEquals(42.0, extended.getAdditionalState("test-2")[0], 1.0e-15);

        // test various constructors
        double[] dd = new double[1];
        dd[0] = -6.0;
        DoubleArrayDictionary additional = new DoubleArrayDictionary();
        additional.put("test-3", dd);
        SpacecraftState sO = new SpacecraftState(state.getAbsPVA(), additional);
        Assertions.assertEquals(-6.0, sO.getAdditionalState("test-3")[0], 1.0e-15);
        SpacecraftState sOA = new SpacecraftState(state.getAbsPVA(), state.getAttitude(), additional);
        Assertions.assertEquals(-6.0, sOA.getAdditionalState("test-3")[0], 1.0e-15);
        SpacecraftState sOM = new SpacecraftState(state.getAbsPVA(), state.getMass(), additional);
        Assertions.assertEquals(-6.0, sOM.getAdditionalState("test-3")[0], 1.0e-15);
        SpacecraftState sOAM = new SpacecraftState(state.getAbsPVA(), state.getAttitude(), state.getMass(), additional);
        Assertions.assertEquals(-6.0, sOAM.getAdditionalState("test-3")[0], 1.0e-15);

    }

    @Test
    public void testAdditionalStatesDerivativesAbsPV() {

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
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_STATE);
            Assertions.assertEquals(oe.getParts()[0], "test-1");
        }
        try {
            state.ensureCompatibleAdditionalStates(extended);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_STATE);
            Assertions.assertTrue(oe.getParts()[0].toString().startsWith("test-"));
        }
        try {
            extended.ensureCompatibleAdditionalStates(state);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_STATE);
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
    public void testShiftAdditionalDerivatives() {

        final String valueAndDerivative = "value-and-derivative";
        final String valueOnly          = "value-only";
        final String derivativeOnly     = "derivative-only";
        final SpacecraftState s0 = propagator.getInitialState().
                                   addAdditionalState(valueAndDerivative,           new double[] { 1.0,  2.0 }).
                                   addAdditionalStateDerivative(valueAndDerivative, new double[] { 3.0,  2.0 }).
                                   addAdditionalState(valueOnly,                    new double[] { 5.0,  4.0 }).
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
        final StaticTransform expectedStaticTransform = state.toTransform().toStaticTransform();
        Assertions.assertEquals(expectedStaticTransform.getDate(), actualStaticTransform.getDate());
        Assertions.assertEquals(expectedStaticTransform.getTranslation(), actualStaticTransform.getTranslation());
        Assertions.assertEquals(0., Rotation.distance(expectedStaticTransform.getRotation(),
                actualStaticTransform.getRotation()));
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
