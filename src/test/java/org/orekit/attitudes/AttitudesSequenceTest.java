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
package org.orekit.attitudes;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.*;
import org.hipparchus.ode.events.Action;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.analytical.FieldEcksteinHechlerPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EclipseDetector;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.EventsLogger;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.FieldOrekitFixedStepHandler;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.*;

public class AttitudesSequenceTest {

    private AbsoluteDate lastChange;
    private boolean inEclipse;

    @Test
    public void testDayNightSwitch() {
        //  Initial state definition : date, orbit
        final AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, TimeScalesFactory.getUTC());
        final Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        final Orbit initialOrbit = new KeplerianOrbit(new PVCoordinates(position, velocity),
                                                      FramesFactory.getEME2000(), initialDate,
                                                      Constants.EIGEN5C_EARTH_MU);

        final

        // Attitudes sequence definition
        EventsLogger logger = new EventsLogger();
        final AttitudesSequence attitudesSequence = new AttitudesSequence();
        final AttitudeProvider dayObservationLaw = new LofOffset(initialOrbit.getFrame(), LOFType.LVLH_CCSDS,
                                                                 RotationOrder.XYZ, FastMath.toRadians(20), FastMath.toRadians(40), 0);
        final AttitudeProvider nightRestingLaw   = new LofOffset(initialOrbit.getFrame(), LOFType.LVLH_CCSDS);
        final ExtendedPVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final EclipseDetector ed =
                        new EclipseDetector(sun, 696000000.,
                                            new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                 0.0,
                                                                 FramesFactory.getGTOD(IERSConventions.IERS_2010, true))).
                withHandler(new ContinueOnEvent() {
                    public Action eventOccurred(final SpacecraftState s, final EventDetector d, final boolean increasing) {
                        setInEclipse(s.getDate(), !increasing);
                        return Action.RESET_STATE;
                    }
                });
        final EventDetector monitored = logger.monitorDetector(ed);
        final Handler dayToNightHandler = new Handler(dayObservationLaw, nightRestingLaw);
        final Handler nightToDayHandler = new Handler(nightRestingLaw, dayObservationLaw);
        attitudesSequence.addSwitchingCondition(dayObservationLaw, nightRestingLaw,
                                                monitored, false, true, 300.0,
                                                AngularDerivativesFilter.USE_RRA, dayToNightHandler);
        attitudesSequence.addSwitchingCondition(nightRestingLaw, dayObservationLaw,
                                                monitored, true, false, 300.0,
                                                AngularDerivativesFilter.USE_RRA, nightToDayHandler);
        SpacecraftState initialState = new SpacecraftState(initialOrbit);
        initialState = initialState.addAdditionalState("fortyTwo", 42.0);
        if (ed.g(initialState) >= 0) {
            // initial position is in daytime
            setInEclipse(initialDate, false);
            attitudesSequence.resetActiveProvider(dayObservationLaw);
        } else {
            // initial position is in nighttime
            setInEclipse(initialDate, true);
            attitudesSequence.resetActiveProvider(nightRestingLaw);
        }

        // Propagator : consider the analytical Eckstein-Hechler model
        final Propagator propagator = new EcksteinHechlerPropagator(initialOrbit, attitudesSequence,
                                                                    Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                                    Constants.EIGEN5C_EARTH_MU,  Constants.EIGEN5C_EARTH_C20,
                                                                    Constants.EIGEN5C_EARTH_C30, Constants.EIGEN5C_EARTH_C40,
                                                                    Constants.EIGEN5C_EARTH_C50, Constants.EIGEN5C_EARTH_C60);

        // Register the switching events to the propagator
        attitudesSequence.registerSwitchEvents(propagator);

        propagator.setStepHandler(60.0, new OrekitFixedStepHandler() {
            public void handleStep(SpacecraftState currentState) {
                // the Earth position in spacecraft frame should be along spacecraft Z axis
                // during night time and away from it during day time due to roll and pitch offsets
                final Vector3D earth = currentState.toTransform().transformPosition(Vector3D.ZERO);
                final double pointingOffset = Vector3D.angle(earth, Vector3D.PLUS_K);

                // the g function is the eclipse indicator, its an angle between Sun and Earth limb,
                // positive when Sun is outside of Earth limb, negative when Sun is hidden by Earth limb
                final double eclipseAngle = ed.g(currentState);

                if (currentState.getDate().durationFrom(lastChange) > 300) {
                    if (inEclipse) {
                        Assertions.assertTrue(eclipseAngle <= 0);
                        Assertions.assertEquals(0.0, pointingOffset, 1.0e-6);
                    } else {
                        Assertions.assertTrue(eclipseAngle >= 0);
                        Assertions.assertEquals(0.767215, pointingOffset, 1.0e-6);
                    }
                } else {
                    // we are in transition
                    Assertions.assertTrue(pointingOffset <= 0.7672155,
                            pointingOffset + " " + (0.767215 - pointingOffset));
                }
            }
        });

        // Propagate from the initial date for the fixed duration
        propagator.propagate(initialDate.shiftedBy(12600.));

        // as we have 2 switch events (even if they share the same underlying event detector),
        // and these events are triggered at both eclipse entry and exit, we get 8
        // raw events on 2 orbits
        Assertions.assertEquals(8, logger.getLoggedEvents().size());

        // we have 4 attitudes switch on 2 orbits, 2 of each type
        Assertions.assertEquals(2, dayToNightHandler.dates.size());
        Assertions.assertEquals(2, nightToDayHandler.dates.size());

    }

    @Test
    public void testDayNightSwitchField() {
        doTestDayNightSwitchField(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestDayNightSwitchField(final Field<T> field)
        {

        //  Initial state definition : date, orbit
        final FieldAbsoluteDate<T> initialDate = new FieldAbsoluteDate<>(field, 2004, 01, 01, 23, 30, 00.000, TimeScalesFactory.getUTC());
        final FieldVector3D<T> position  = new FieldVector3D<>(field,
                                                               new Vector3D(-6142438.668, 3492467.560, -25767.25680));
        final FieldVector3D<T> velocity  = new FieldVector3D<>(field,
                                                               new Vector3D(505.8479685, 942.7809215, 7435.922231));
        final FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(new FieldPVCoordinates<>(position, velocity),
                                                                     FramesFactory.getEME2000(), initialDate,
                                                                     field.getZero().add(Constants.EIGEN5C_EARTH_MU));

        // Attitudes sequence definition
        EventsLogger logger = new EventsLogger();
        final AttitudesSequence attitudesSequence = new AttitudesSequence();
        final AttitudeProvider dayObservationLaw = new LofOffset(initialOrbit.getFrame(), LOFType.LVLH_CCSDS,
                                                                 RotationOrder.XYZ, FastMath.toRadians(20), FastMath.toRadians(40), 0);
        final AttitudeProvider nightRestingLaw   = new LofOffset(initialOrbit.getFrame(), LOFType.LVLH_CCSDS);
        final ExtendedPVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final EclipseDetector ed =
                new EclipseDetector(sun, 696000000.,
                                    new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                         0.0,
                                                         FramesFactory.getGTOD(IERSConventions.IERS_2010, true))).
                withHandler(new ContinueOnEvent() {
                    int count = 0;
                    public Action eventOccurred(final SpacecraftState s,
                                                             final EventDetector d,
                                                             final boolean increasing) {
                        setInEclipse(s.getDate(), !increasing);
                        if (count++ == 7) {
                            return Action.STOP;
                        } else {
                            switch (count % 3) {
                                case 0 :
                                    return Action.CONTINUE;
                                case 1 :
                                    return Action.RESET_DERIVATIVES;
                                default :
                                    return Action.RESET_STATE;
                            }
                        }
                    }
                });
        final EventDetector monitored = logger.monitorDetector(ed);
        final Handler dayToNightHandler = new Handler(dayObservationLaw, nightRestingLaw);
        final Handler nightToDayHandler = new Handler(nightRestingLaw, dayObservationLaw);
        attitudesSequence.addSwitchingCondition(dayObservationLaw, nightRestingLaw,
                                                monitored, false, true, 300.0,
                                                AngularDerivativesFilter.USE_RRA, dayToNightHandler);
        attitudesSequence.addSwitchingCondition(nightRestingLaw, dayObservationLaw,
                                                monitored, true, false, 300.0,
                                                AngularDerivativesFilter.USE_RRA, nightToDayHandler);
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(initialOrbit);
        initialState = initialState.addAdditionalState("fortyTwo", field.getZero().add(42.0));
        if (ed.g(initialState.toSpacecraftState()) >= 0) {
            // initial position is in daytime
            setInEclipse(initialDate.toAbsoluteDate(), false);
            attitudesSequence.resetActiveProvider(dayObservationLaw);
        } else {
            // initial position is in nighttime
            setInEclipse(initialDate.toAbsoluteDate(), true);
            attitudesSequence.resetActiveProvider(nightRestingLaw);
        }

        // Propagator : consider the analytical Eckstein-Hechler model
        final FieldPropagator<T> propagator = new FieldEcksteinHechlerPropagator<T>(initialOrbit, attitudesSequence,
                                                                                    Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                                                    field.getZero().add(Constants.EIGEN5C_EARTH_MU),
                                                                                    Constants.EIGEN5C_EARTH_C20,
                                                                                    Constants.EIGEN5C_EARTH_C30, Constants.EIGEN5C_EARTH_C40,
                                                                                    Constants.EIGEN5C_EARTH_C50, Constants.EIGEN5C_EARTH_C60);

        // Register the switching events to the propagator
        attitudesSequence.registerSwitchEvents(field, propagator);

        propagator.setStepHandler(field.getZero().add(60.0), new FieldOrekitFixedStepHandler<T>() {
            public void handleStep(FieldSpacecraftState<T> currentState) {
                // the Earth position in spacecraft frame should be along spacecraft Z axis
                // during night time and away from it during day time due to roll and pitch offsets
                final FieldVector3D<T> earth = currentState.toTransform().transformPosition(Vector3D.ZERO);
                final T pointingOffset = FieldVector3D.angle(earth, Vector3D.PLUS_K);

                // the g function is the eclipse indicator, its an angle between Sun and Earth limb,
                // positive when Sun is outside of Earth limb, negative when Sun is hidden by Earth limb
                final double eclipseAngle = ed.g(currentState.toSpacecraftState());

                if (currentState.getDate().durationFrom(lastChange).getReal() > 300) {
                    if (inEclipse) {
                        Assertions.assertTrue(eclipseAngle <= 0);
                        Assertions.assertEquals(0.0, pointingOffset.getReal(), 1.0e-6);
                    } else {
                        Assertions.assertTrue(eclipseAngle >= 0);
                        Assertions.assertEquals(0.767215, pointingOffset.getReal(), 1.0e-6);
                    }
                } else {
                    // we are in transition
                    Assertions.assertTrue(pointingOffset.getReal() <= 0.7672155,
                            pointingOffset.getReal() + " " + (0.767215 - pointingOffset.getReal()));
                }
            }
        });

        // Propagate from the initial date for the fixed duration
        propagator.propagate(initialDate.shiftedBy(12600.));

        // as we have 2 switch events (even if they share the same underlying event detector),
        // and these events are triggered at both eclipse entry and exit, we get 8
        // raw events on 2 orbits
        Assertions.assertEquals(8, logger.getLoggedEvents().size());

        // we have 4 attitudes switch on 2 orbits, 2 of each type
        Assertions.assertEquals(2, dayToNightHandler.dates.size());
        Assertions.assertEquals(2, nightToDayHandler.dates.size());

    }

    @Test
    public void testBackwardPropagation() {

        //  Initial state definition : date, orbit
        final AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, TimeScalesFactory.getUTC());
        final Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        final Orbit initialOrbit = new KeplerianOrbit(new PVCoordinates(position, velocity),
                                                      FramesFactory.getEME2000(), initialDate,
                                                      Constants.EIGEN5C_EARTH_MU);

        final AttitudesSequence attitudesSequence = new AttitudesSequence();
        final AttitudeProvider past    = new FrameAlignedProvider(Rotation.IDENTITY);
        final AttitudeProvider current = new FrameAlignedProvider(Rotation.IDENTITY);
        final AttitudeProvider future  = new FrameAlignedProvider(Rotation.IDENTITY);
        final Handler handler = new Handler(current, past);
        attitudesSequence.addSwitchingCondition(past, current,
                                                new DateDetector(initialDate.shiftedBy(-500.0)),
                                                true, false, 10.0, AngularDerivativesFilter.USE_R, handler);
        attitudesSequence.addSwitchingCondition(current, future,
                                                new DateDetector(initialDate.shiftedBy(+500.0)),
                                                true, false, 10.0, AngularDerivativesFilter.USE_R, null);
        attitudesSequence.resetActiveProvider(current);

        SpacecraftState initialState = new SpacecraftState(initialOrbit);
        initialState = initialState.addAdditionalState("fortyTwo", 42.0);
        final Propagator propagator = new EcksteinHechlerPropagator(initialOrbit, attitudesSequence,
                                                                    Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                                    Constants.EIGEN5C_EARTH_MU,  Constants.EIGEN5C_EARTH_C20,
                                                                    Constants.EIGEN5C_EARTH_C30, Constants.EIGEN5C_EARTH_C40,
                                                                    Constants.EIGEN5C_EARTH_C50, Constants.EIGEN5C_EARTH_C60);
        propagator.resetInitialState(initialState);
        Assertions.assertEquals(42.0, propagator.getInitialState().getAdditionalState("fortyTwo")[0], 1.0e-10);

        // Register the switching events to the propagator
        attitudesSequence.registerSwitchEvents(propagator);

        SpacecraftState finalState = propagator.propagate(initialDate.shiftedBy(-10000.0));
        Assertions.assertEquals(42.0, finalState.getAdditionalState("fortyTwo")[0], 1.0e-10);
        Assertions.assertEquals(1, handler.dates.size());
        Assertions.assertEquals(-500.0, handler.dates.get(0).durationFrom(initialDate), 1.0e-3);
        Assertions.assertEquals(-490.0, finalState.getDate().durationFrom(initialDate), 1.0e-3);

    }

    @Test
    public void testTooShortTransition() {
        double threshold      = 1.5;
        double transitionTime = 0.5;
        try {
            new AttitudesSequence().addSwitchingCondition(new FrameAlignedProvider(Rotation.IDENTITY),
                                                          new FrameAlignedProvider(Rotation.IDENTITY),
                                                          new DateDetector(AbsoluteDate.J2000_EPOCH).
                                                          withMaxCheck(1000.0).
                                                          withThreshold(threshold),
                                                          true, false, transitionTime,
                                                          AngularDerivativesFilter.USE_R, null);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.TOO_SHORT_TRANSITION_TIME_FOR_ATTITUDES_SWITCH, oe.getSpecifier());
            Assertions.assertEquals(transitionTime, ((Double) oe.getParts()[0]).doubleValue(), 1.0e-10);
            Assertions.assertEquals(threshold, ((Double) oe.getParts()[1]).doubleValue(), 1.0e-10);
        }
    }

    @Test
    public void testOutOfSyncCalls() {
        //  Initial state definition : date, orbit
        final AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, TimeScalesFactory.getUTC());
        final Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        final Orbit initialOrbit = new KeplerianOrbit(new PVCoordinates(position, velocity),
                                                      FramesFactory.getEME2000(), initialDate,
                                                      Constants.EIGEN5C_EARTH_MU);

        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final TopocentricFrame volgograd = new TopocentricFrame(earth,
                                                                new GeodeticPoint(FastMath.toRadians(48.7),
                                                                                  FastMath.toRadians(44.5),
                                                                                  24.0),
                                                             "Волгоград");
        final AttitudesSequence attitudesSequence = new AttitudesSequence();
        final double            transitionTime    = 250.0;
        final AttitudeProvider  nadirPointing     = new NadirPointing(initialOrbit.getFrame(), earth);
        final AttitudeProvider  targetPointing    = new TargetPointing(initialOrbit.getFrame(), volgograd.getPoint(), earth);
        final ElevationDetector eventDetector     = new ElevationDetector(volgograd).
                                                    withConstantElevation(FastMath.toRadians(5.0)).
                                                    withHandler(new ContinueOnEvent());
        final Handler nadirToTarget =  new Handler(nadirPointing, targetPointing);
        attitudesSequence.addSwitchingCondition(nadirPointing, targetPointing, eventDetector,
                                                true, false, transitionTime, AngularDerivativesFilter.USE_RR,
                                                nadirToTarget);
        final Handler targetToNadir =  new Handler(targetPointing, nadirPointing);
        attitudesSequence.addSwitchingCondition(targetPointing, nadirPointing, eventDetector,
                                                false, true, transitionTime, AngularDerivativesFilter.USE_RR,
                                                targetToNadir);
        final double[][] tolerance = NumericalPropagator.tolerances(10.0, initialOrbit, initialOrbit.getType());
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 300.0, tolerance[0], tolerance[1]);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("g007_eigen_05c_coef", false));
        propagator.addForceModel(new HolmesFeatherstoneAttractionModel(earth.getBodyFrame(),
                                                                       GravityFieldFactory.getNormalizedProvider(8, 8)));
        propagator.setInitialState(new SpacecraftState(initialOrbit,
                                                       nadirPointing.getAttitude(initialOrbit,
                                                                                 initialOrbit.getDate(),
                                                                                 initialOrbit.getFrame())));
        propagator.setAttitudeProvider(attitudesSequence);
        attitudesSequence.registerSwitchEvents(propagator);
        propagator.setStepHandler(10, state -> {

            Attitude nadirAttitude  = nadirPointing.getAttitude(state.getOrbit(), state.getDate(), state.getFrame());
            Attitude targetAttitude = targetPointing.getAttitude(state.getOrbit(), state.getDate(), state.getFrame());
            Attitude stateAttitude  = state.getAttitude();

            if (nadirToTarget.dates.isEmpty() || state.getDate().durationFrom(nadirToTarget.dates.get(0)) < 0) {
                // we are stabilized in nadir pointing, before first switch
                checkEqualAttitudes(nadirAttitude, stateAttitude);
            } else if (state.getDate().durationFrom(nadirToTarget.dates.get(0)) <= transitionTime) {
                // we are in transition from nadir to target
                checkBetweenAttitudes(nadirAttitude, targetAttitude, stateAttitude);
            } else if (targetToNadir.dates.isEmpty() || state.getDate().durationFrom(targetToNadir.dates.get(0)) < 0) {
                // we are stabilized in target pointing between the two switches
                checkEqualAttitudes(targetAttitude, stateAttitude);
            } else if (state.getDate().durationFrom(targetToNadir.dates.get(0)) <= transitionTime) {
                // we are in transition from target to nadir
                checkBetweenAttitudes(targetAttitude, nadirAttitude, stateAttitude);
            } else {
                // we are stabilized back in nadir pointing, after second switch
                checkEqualAttitudes(nadirAttitude, stateAttitude);
            }

        });
        propagator.propagate(initialDate.shiftedBy(6000));

    }

    /**
     * this test have been completed to test the issue 552 fix
     */
    @Test
    public void testResetDuringTransitionForward() {
        //  Initial state definition : date, orbit
        final AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, TimeScalesFactory.getUTC());
        final Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        final Orbit initialOrbit = new KeplerianOrbit(new PVCoordinates(position, velocity),
                                                      FramesFactory.getEME2000(), initialDate,
                                                      Constants.EIGEN5C_EARTH_MU);

        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final TopocentricFrame volgograd = new TopocentricFrame(earth,
                                                                new GeodeticPoint(FastMath.toRadians(48.7),
                                                                                  FastMath.toRadians(44.5),
                                                                                  24.0),
                                                             "Волгоград");
        final AttitudesSequence attitudesSequence = new AttitudesSequence();
        final double            transitionTime    = 250.0;
        final AttitudeProvider  nadirPointing     = new NadirPointing(initialOrbit.getFrame(), earth);
        final AttitudeProvider  targetPointing    = new TargetPointing(initialOrbit.getFrame(), volgograd.getPoint(), earth);
        final ElevationDetector eventDetector     = new ElevationDetector(volgograd).
                                                    withConstantElevation(FastMath.toRadians(5.0)).
                                                    withHandler(new ContinueOnEvent());
        final List<AbsoluteDate> nadirToTarget = new ArrayList<>();
        attitudesSequence.addSwitchingCondition(nadirPointing, targetPointing, eventDetector,
                                                true, false, transitionTime, AngularDerivativesFilter.USE_RR,
                                                (previous, next, state) -> nadirToTarget.add(state.getDate()));
        final double[][] tolerance = NumericalPropagator.tolerances(10.0, initialOrbit, initialOrbit.getType());
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 300.0, tolerance[0], tolerance[1]);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("g007_eigen_05c_coef", false));
        propagator.addForceModel(new HolmesFeatherstoneAttractionModel(earth.getBodyFrame(),
                                                                       GravityFieldFactory.getNormalizedProvider(8, 8)));
        propagator.setInitialState(new SpacecraftState(initialOrbit,
                                                       nadirPointing.getAttitude(initialOrbit,
                                                                                 initialOrbit.getDate(),
                                                                                 initialOrbit.getFrame())));
        propagator.setAttitudeProvider(attitudesSequence);
        attitudesSequence.registerSwitchEvents(propagator);
        propagator.propagate(initialDate.shiftedBy(6000));

        // check that if we restart a forward propagation from an intermediate state
        // we properly get an interpolated attitude despite we missed the event trigger

        final AbsoluteDate midTransition = nadirToTarget.get(0).shiftedBy(0.5 * transitionTime);
        SpacecraftState state   = propagator.propagate(midTransition.shiftedBy(-60), midTransition);
        Rotation nadirR  = nadirPointing.getAttitude(state.getOrbit(), state.getDate(), state.getFrame()).getRotation();
        Rotation targetR = targetPointing.getAttitude(state.getOrbit(), state.getDate(), state.getFrame()).getRotation();
        final double reorientationAngle = Rotation.distance(nadirR, targetR);
        Assertions.assertEquals(0.5 * reorientationAngle, Rotation.distance(state.getAttitude().getRotation(), nadirR),
                0.03 * reorientationAngle);

        // check that if we restart a forward propagation from an intermediate state
        // we properly get the "after" attitude law despite we missed the event trigger
        // This check have been added to the test after the issue #552 fix

        final AbsoluteDate afterTransition = midTransition.shiftedBy(transitionTime);
        state = propagator.propagate(midTransition, afterTransition);
        targetR = targetPointing.getAttitude(state.getOrbit(), state.getDate(), state.getFrame()).getRotation();

        Assertions.assertEquals(targetR.getQ0(), state.getAttitude().getRotation().getQ0(), 1.0e-16);
        Assertions.assertEquals(targetR.getQ1(), state.getAttitude().getRotation().getQ1(), 1.0e-16);
        Assertions.assertEquals(targetR.getQ2(), state.getAttitude().getRotation().getQ2(), 1.0e-16);
        Assertions.assertEquals(targetR.getQ3(), state.getAttitude().getRotation().getQ3(), 1.0e-16);
    }

    @Test
    public void testResetDuringTransitionBackward() {
        //  Initial state definition : date, orbit
        final AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, TimeScalesFactory.getUTC());
        final Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        final Orbit initialOrbit = new KeplerianOrbit(new PVCoordinates(position, velocity),
                                                      FramesFactory.getEME2000(), initialDate,
                                                      Constants.EIGEN5C_EARTH_MU);

        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final TopocentricFrame volgograd = new TopocentricFrame(earth,
                                                                new GeodeticPoint(FastMath.toRadians(48.7),
                                                                                  FastMath.toRadians(44.5),
                                                                                  24.0),
                                                             "Волгоград");
        final AttitudesSequence attitudesSequence = new AttitudesSequence();
        final double            transitionTime    = 250.0;
        final AttitudeProvider  nadirPointing     = new NadirPointing(initialOrbit.getFrame(), earth);
        final AttitudeProvider  targetPointing    = new TargetPointing(initialOrbit.getFrame(), volgograd.getPoint(), earth);
        final ElevationDetector eventDetector     = new ElevationDetector(volgograd).
                                                    withConstantElevation(FastMath.toRadians(5.0)).
                                                    withHandler(new ContinueOnEvent());
        final List<AbsoluteDate> nadirToTarget = new ArrayList<>();
        attitudesSequence.addSwitchingCondition(nadirPointing, targetPointing, eventDetector,
                                                true, false, transitionTime, AngularDerivativesFilter.USE_RR,
                                                (previous, next, state) -> nadirToTarget.add(state.getDate()));
        final double[][] tolerance = NumericalPropagator.tolerances(10.0, initialOrbit, initialOrbit.getType());
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 300.0, tolerance[0], tolerance[1]);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("g007_eigen_05c_coef", false));
        propagator.addForceModel(new HolmesFeatherstoneAttractionModel(earth.getBodyFrame(),
                                                                       GravityFieldFactory.getNormalizedProvider(8, 8)));
        propagator.setInitialState(new SpacecraftState(initialOrbit,
                                                       nadirPointing.getAttitude(initialOrbit,
                                                                                 initialOrbit.getDate(),
                                                                                 initialOrbit.getFrame())));
        propagator.setAttitudeProvider(attitudesSequence);
        attitudesSequence.registerSwitchEvents(propagator);
        propagator.propagate(initialDate.shiftedBy(6000));

        // check that if we restart a backward propagation from an intermediate state
        // we properly get an interpolated attitude despite we missed the event trigger
        final AbsoluteDate midTransition = nadirToTarget.get(0).shiftedBy(0.5 * transitionTime);
        SpacecraftState state   = propagator.propagate(midTransition.shiftedBy(+60), midTransition);
        Rotation nadirR  = nadirPointing.getAttitude(state.getOrbit(), state.getDate(), state.getFrame()).getRotation();
        Rotation targetR = targetPointing.getAttitude(state.getOrbit(), state.getDate(), state.getFrame()).getRotation();
        final double reorientationAngle = Rotation.distance(nadirR, targetR);
        Assertions.assertEquals(0.5 * reorientationAngle, Rotation.distance(state.getAttitude().getRotation(), targetR),
                0.03 * reorientationAngle);


    }

    /**
     * Test for the issue 551 fix
     */
    @Test
    public void testAnalyticalPropagatorTransition() {
        // Define initial state
        final AbsoluteDate initialDate = new AbsoluteDate(2017, 03, 27, 0, 0, 00.000, TimeScalesFactory.getUTC());
        final Vector3D position  = new Vector3D(-39098981.4866597, -15784239.3610601, 78908.2289853595);
        final Vector3D velocity  = new Vector3D(1151.00321021175, -2851.14864755189, -2.02133248357321);
        final Orbit initialOrbit = new KeplerianOrbit(new PVCoordinates(position, velocity),
                                                      FramesFactory.getGCRF(), initialDate,
                                                      Constants.WGS84_EARTH_MU);

        // Define attitude laws
        final AttitudeProvider before  = new FrameAlignedProvider(new Rotation(0, 0, 0, 1, false));
        final AttitudeProvider current = new FrameAlignedProvider(Rotation.IDENTITY);
        final AttitudeProvider after   = new FrameAlignedProvider(new Rotation(0, 0, 0, -1, false));

        // Define attitude sequence
        final AbsoluteDate forwardSwitchDate = initialDate.shiftedBy(600);
        final AbsoluteDate backwardSwitchDate = initialDate.shiftedBy(-600);
        final DateDetector forwardSwitchDetector = new DateDetector(forwardSwitchDate).withHandler(new ContinueOnEvent());
        final DateDetector backwardSwitchDetector = new DateDetector(backwardSwitchDate).withHandler(new ContinueOnEvent());

        // Initialize the attitude sequence
        final AttitudesSequence attitudeSequence = new AttitudesSequence();
        attitudeSequence.resetActiveProvider(current);
        attitudeSequence.addSwitchingCondition(before, current, backwardSwitchDetector, true, true, 60, AngularDerivativesFilter.USE_RR, null);
        attitudeSequence.addSwitchingCondition(current, after, forwardSwitchDetector, true, true, 60, AngularDerivativesFilter.USE_RR, null);

        // Initialize analytical propagator
        Propagator propagator = new KeplerianPropagator(initialOrbit);
        propagator.setAttitudeProvider(attitudeSequence);
        attitudeSequence.registerSwitchEvents(propagator);

        SpacecraftState stateAfter = propagator.propagate(initialDate, initialDate.shiftedBy(1200));
        SpacecraftState stateBefore = propagator.propagate(initialDate, initialDate.shiftedBy(-1200));

        // Check that the dates are correct
        Assertions.assertEquals(1200, stateAfter.getDate().durationFrom(initialDate), 1.0E-3);
        Assertions.assertEquals(-1200, stateBefore.getDate().durationFrom(initialDate), 1.0E-3);

        // Check that the attitudes are correct
        Assertions.assertEquals(
                before.getAttitude(stateBefore.getOrbit(), stateBefore.getDate(), stateBefore.getFrame()).getRotation().getQ0(),
                stateBefore.getAttitude().getRotation().getQ0(), 1.0E-16);
        Assertions.assertEquals(
                before.getAttitude(stateBefore.getOrbit(), stateBefore.getDate(), stateBefore.getFrame()).getRotation().getQ1(),
                stateBefore.getAttitude().getRotation().getQ1(), 1.0E-16);
        Assertions.assertEquals(
                before.getAttitude(stateBefore.getOrbit(), stateBefore.getDate(), stateBefore.getFrame()).getRotation().getQ2(),
                stateBefore.getAttitude().getRotation().getQ2(), 1.0E-16);
        Assertions.assertEquals(
                before.getAttitude(stateBefore.getOrbit(), stateBefore.getDate(), stateBefore.getFrame()).getRotation().getQ3(),
                stateBefore.getAttitude().getRotation().getQ3(), 1.0E-16);

        Assertions.assertEquals(
                after.getAttitude(stateAfter.getOrbit(), stateAfter.getDate(), stateAfter.getFrame()).getRotation().getQ0(),
                stateAfter.getAttitude().getRotation().getQ0(), 1.0E-16);
        Assertions.assertEquals(
                after.getAttitude(stateAfter.getOrbit(), stateAfter.getDate(), stateAfter.getFrame()).getRotation().getQ1(),
                stateAfter.getAttitude().getRotation().getQ1(), 1.0E-16);
        Assertions.assertEquals(
                after.getAttitude(stateAfter.getOrbit(), stateAfter.getDate(), stateAfter.getFrame()).getRotation().getQ2(),
                stateAfter.getAttitude().getRotation().getQ2(), 1.0E-16);
        Assertions.assertEquals(
                after.getAttitude(stateAfter.getOrbit(), stateAfter.getDate(), stateAfter.getFrame()).getRotation().getQ3(),
                stateAfter.getAttitude().getRotation().getQ3(), 1.0E-16);
    }

    @Test
    void testGetAttitudeRotation() {
        // GIVEN
        final AttitudeProvider attitudeProvider = new TestAttitudeProvider();
        final AttitudesSequence attitudesSequence = new AttitudesSequence();
        attitudesSequence.resetActiveProvider(attitudeProvider);
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final PVCoordinatesProvider mockPvCoordinatesProvider = Mockito.mock(PVCoordinatesProvider.class);
        // WHEN
        final Rotation actualRotation = attitudesSequence.getAttitudeRotation(mockPvCoordinatesProvider, date, frame);
        // THEN
        final Attitude attitude = attitudesSequence.getAttitude(mockPvCoordinatesProvider, date, frame);
        final Rotation expectedRotation = attitude.getRotation();
        Assertions.assertEquals(0., Rotation.distance(expectedRotation, actualRotation));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetAttitudeRotationFieldTest() {
        // GIVEN
        final GradientField field = GradientField.getField(1);
        final AttitudeProvider attitudeProvider = new TestAttitudeProvider();
        final AttitudesSequence attitudesSequence = new AttitudesSequence();
        attitudesSequence.resetActiveProvider(attitudeProvider);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(field, date);
        final FieldPVCoordinatesProvider<Gradient> pvCoordinatesProvider = Mockito.mock(FieldPVCoordinatesProvider.class);
        final Frame mockFrame = Mockito.mock(Frame.class);
        // WHEN
        final FieldRotation<Gradient> actualRotation = attitudesSequence.getAttitudeRotation(pvCoordinatesProvider, fieldDate, mockFrame);
        // THEN
        final FieldAttitude<Gradient> attitude = attitudesSequence.getAttitude(pvCoordinatesProvider, fieldDate, mockFrame);
        final FieldRotation<Gradient> expectedRotation = attitude.getRotation();
        Assertions.assertEquals(0., Rotation.distance(expectedRotation.toRotation(), actualRotation.toRotation()));
    }

    private static class TestAttitudeProvider implements AttitudeProvider {

        TestAttitudeProvider() {
            // nothing to do
        }

        @Override
        public Attitude getAttitude(PVCoordinatesProvider pvProv, AbsoluteDate date, Frame frame) {
            return new Attitude(date, frame, new AngularCoordinates());
        }

        @Override
        public <T extends CalculusFieldElement<T>> FieldAttitude<T> getAttitude(FieldPVCoordinatesProvider<T> pvProv,
                                                                                FieldAbsoluteDate<T> date, Frame frame) {
            return new FieldAttitude<>(date.getField(), new Attitude(date.toAbsoluteDate(), frame,
                    new AngularCoordinates()));
        }

    }

    private static class Handler implements AttitudesSequence.SwitchHandler {

        private AttitudeProvider   expectedPrevious;
        private AttitudeProvider   expectedNext;
        private List<AbsoluteDate> dates;

        public Handler(final AttitudeProvider expectedPrevious, final AttitudeProvider expectedNext) {
            this.expectedPrevious = expectedPrevious;
            this.expectedNext     = expectedNext;
            this.dates            = new ArrayList<AbsoluteDate>();
        }

        @Override
        public void switchOccurred(AttitudeProvider previous, AttitudeProvider next,
                                   SpacecraftState state) {
            Assertions.assertTrue(previous == expectedPrevious);
            Assertions.assertTrue(next     == expectedNext);
            dates.add(state.getDate());
        }

    }

    private void setInEclipse(AbsoluteDate lastChange, boolean inEclipse) {
        this.lastChange = lastChange;
        this.inEclipse = inEclipse;
    }

    private void checkEqualAttitudes(final Attitude expected, final Attitude tested) {
        Assertions.assertEquals(0.0, Rotation.distance(expected.getRotation(), tested.getRotation()), 1.0e-14);
        Assertions.assertEquals(0.0, Vector3D.distance(expected.getSpin(), tested.getSpin()), 1.0e-11);
        Assertions.assertEquals(0.0,
                Vector3D.distance(expected.getRotationAcceleration(), tested.getRotationAcceleration()), 1.0e-9);
    }

    private void checkBetweenAttitudes(final Attitude limit1, final Attitude limit2, final Attitude tested) {
        final Rotation r1 = limit1.getRotation();
        final Rotation r2 = limit2.getRotation();
        final Rotation t  = tested.getRotation();
        final double reorientationAngle = Rotation.distance(r1, r2);
        Assertions.assertTrue(Rotation.distance(t, r1) < reorientationAngle);
        Assertions.assertTrue(Rotation.distance(t, r2) < reorientationAngle);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:potential");
    }

}

