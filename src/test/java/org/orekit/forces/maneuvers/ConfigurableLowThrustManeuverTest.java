/* Copyright 2020 Exotrail
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Exotrail licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.forces.maneuvers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.DormandPrince54FieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince54Integrator;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.LofOffset;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.maneuvers.propulsion.ThrustDirectionAndAttitudeProvider;
import org.orekit.forces.maneuvers.trigger.DateBasedManeuverTriggers;
import org.orekit.forces.maneuvers.trigger.ManeuverTriggersResetter;
import org.orekit.forces.maneuvers.trigger.StartStopEventsTrigger;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.AdaptableInterval;
import org.orekit.propagation.events.BooleanDetector;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldAbstractDetector;
import org.orekit.propagation.events.FieldAdaptableInterval;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldNegateDetector;
import org.orekit.propagation.events.NegateDetector;
import org.orekit.propagation.events.PositionAngleDetector;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnEvent;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;

public class ConfigurableLowThrustManeuverTest {
    /** */
    private static double maxCheck = 100;
    /** */
    private static double maxThreshold = AbstractDetector.DEFAULT_THRESHOLD;

    /** */
    private double thrust = 2e-3;
    /** */
    private double isp = 800;
    /** */
    private double halfThrustArc = FastMath.PI / 4;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    private abstract class EquinoctialLongitudeIntervalDetector<T extends EventDetector>
            extends AbstractDetector<EquinoctialLongitudeIntervalDetector<T>> {

        /** */
        private double halfArcLength;
        /** */
        private final PositionAngleType type;

        public EquinoctialLongitudeIntervalDetector(final double halfArcLength, final PositionAngleType type,
                                                    final AdaptableInterval maxCheck, final double threshold, final int maxIter,
                                                    final EventHandler handler) {
            super(maxCheck, threshold, maxIter, handler);
            this.halfArcLength = halfArcLength;
            this.type = type;
        }

        public abstract double getReferenceEquinoctialLongitude(SpacecraftState s); // node ...

        @Override
        public double g(final SpacecraftState s) {
            if (halfArcLength <= 0) {
                return -1;
            }
            // cast is safe because type guaranteed to match
            final EquinoctialOrbit orbit = (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(s.getOrbit());
            final double current = MathUtils.normalizeAngle(orbit.getL(type), 0);
            final double ret;
            final double lonStart = getReferenceEquinoctialLongitude(s) - halfArcLength;
            final double lonEnd = getReferenceEquinoctialLongitude(s) + halfArcLength;

            final double diffStart = MathUtils.normalizeAngle(current, lonStart) - lonStart;
            final double diffEnd = MathUtils.normalizeAngle(lonEnd, current) - current;

            final double sin1 = FastMath.sin(diffStart);
            final double sin2 = FastMath.sin(-diffEnd);
            if (lonEnd - lonStart < FastMath.PI) {
                ret = FastMath.min(sin1, -sin2);
            } else {
                ret = FastMath.max(sin1, -sin2);
            }
            if (Double.isNaN(ret)) {
                if (Double.isNaN(current)) {
                    throw new RuntimeException("Detector of type   " + this.getClass().getSimpleName() +
                            " returned NaN because bad orbit provided");
                }
                throw new RuntimeException("Detector of type  " + this.getClass().getSimpleName() + "  returned NaN");
            }
            return ret;
        }

        public double getHalfArcLength() {
            return halfArcLength;
        }

        public PositionAngleType getType() {
            return type;
        }

    }

    private class PerigeeCenteredIntervalDetector
            extends EquinoctialLongitudeIntervalDetector<PerigeeCenteredIntervalDetector> {

        protected PerigeeCenteredIntervalDetector(final double halfArcLength, final PositionAngleType type,
                                                  final AdaptableInterval maxCheck, final double threshold, final int maxIter,
                                                  final EventHandler handler) {

            super(halfArcLength, type, maxCheck, threshold, maxIter, handler);
        }

        public PerigeeCenteredIntervalDetector(final double halfArcLength, final PositionAngleType type,
                final EventHandler handler) {

            super(halfArcLength, type, s -> maxCheck, maxThreshold, DEFAULT_MAX_ITER, handler);
        }

        @Override
        public double getReferenceEquinoctialLongitude(final SpacecraftState s) {

            final KeplerianOrbit orb = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(s.getOrbit());

            final double longitudeOfPerigee = orb.getRightAscensionOfAscendingNode() + orb.getPerigeeArgument();

            return longitudeOfPerigee;
        }

        @Override
        protected EquinoctialLongitudeIntervalDetector<PerigeeCenteredIntervalDetector> create(final AdaptableInterval newMaxCheck,
                                                                                               final double newThreshold, final int newMaxIter,
                                                                                               final EventHandler newHandler) {
            return new PerigeeCenteredIntervalDetector(getHalfArcLength(), getType(), newMaxCheck, newThreshold,
                    newMaxIter, newHandler);
        }
    }

    private class ApogeeCenteredIntervalDetector
            extends EquinoctialLongitudeIntervalDetector<ApogeeCenteredIntervalDetector> {

        protected ApogeeCenteredIntervalDetector(final double halfArcLength, final PositionAngleType type,
                                                 final AdaptableInterval maxCheck, final double threshold, final int maxIter,
                                                 final EventHandler handler) {

            super(halfArcLength, type, maxCheck, threshold, maxIter, handler);
        }

        public ApogeeCenteredIntervalDetector(final double halfArcLength, final PositionAngleType type, final EventHandler handler) {
            super(halfArcLength, type, s -> maxCheck, maxThreshold, DEFAULT_MAX_ITER, handler);
        }

        @Override
        public double getReferenceEquinoctialLongitude(final SpacecraftState s) {

            final KeplerianOrbit orb = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(s.getOrbit());

            final double longitudeOfApogee = orb.getRightAscensionOfAscendingNode() + orb.getPerigeeArgument() +
                    FastMath.PI;

            return longitudeOfApogee;
        }

        @Override
        protected EquinoctialLongitudeIntervalDetector<ApogeeCenteredIntervalDetector> create(final AdaptableInterval newMaxCheck,
                final double newThreshold, final int newMaxIter, final EventHandler newHandler) {

            return new ApogeeCenteredIntervalDetector(getHalfArcLength(), getType(), newMaxCheck, newThreshold,
                    newMaxIter, newHandler);
        }
    }

    private static class DateIntervalDetector extends AbstractDetector<DateIntervalDetector> {

        private final AbsoluteDate startDate;
        private final AbsoluteDate endDate;

        public DateIntervalDetector(final AbsoluteDate startDate, final AbsoluteDate endDate) {
            this(startDate, endDate, s -> DateDetector.DEFAULT_MAX_CHECK, DateDetector.DEFAULT_THRESHOLD, DEFAULT_MAX_ITER,
                    new StopOnEvent());
        }

        protected DateIntervalDetector(final AbsoluteDate startDate, final AbsoluteDate endDate,
                                       final AdaptableInterval maxCheck, final double threshold,
                                       final int maxIter, final EventHandler handler) {
            super(maxCheck, threshold, maxIter, handler);
            this.startDate = startDate;
            this.endDate = endDate;
            if (startDate.durationFrom(endDate) >= 0) {
                throw new RuntimeException("StartDate(" + startDate + ") should be before EndDate(" + endDate + ")");
            }
        }

        @Override
        public double g(final SpacecraftState s) {
            final AbsoluteDate gDate = s.getDate();
            final double durationFromStart = gDate.durationFrom(startDate);
            if (durationFromStart < 0) {
                return durationFromStart; // before interval
            }
            final double durationBeforeEnd = endDate.durationFrom(gDate);
            if (durationBeforeEnd < 0) {
                return durationBeforeEnd; // after interval
            }

            final double ret = FastMath.min(durationFromStart, durationBeforeEnd); // take the closest date
            if (Double.isNaN(ret)) {
                throw new RuntimeException("Detector of type " + this.getClass().getSimpleName() + " returned NaN");
            }
            return ret;
        }

        @Override
        protected DateIntervalDetector create(final AdaptableInterval newMaxCheck, final double newThreshold, final int newMaxIter,
                                              final EventHandler newHandler) {
            return new DateIntervalDetector(startDate, endDate, newMaxCheck, newThreshold, newMaxIter, newHandler);
        }

    }

    private static class DateIntervalFieldDetector<T extends CalculusFieldElement<T>> extends FieldAbstractDetector<DateIntervalFieldDetector<T>, T> {

        private final FieldAbsoluteDate<T> startDate;
        private final FieldAbsoluteDate<T> endDate;

        public DateIntervalFieldDetector(final FieldAbsoluteDate<T> startDate, final FieldAbsoluteDate<T> endDate) {
            this(startDate, endDate,
                 s -> DateDetector.DEFAULT_MAX_CHECK,
                 startDate.getField().getZero().newInstance(DateDetector.DEFAULT_THRESHOLD),
                 DEFAULT_MAX_ITER, new FieldStopOnEvent<>());
        }

        protected DateIntervalFieldDetector(final FieldAbsoluteDate<T> startDate, final FieldAbsoluteDate<T> endDate,
                                            final FieldAdaptableInterval<T> maxCheck, final T threshold, final int maxIter,
                                            final FieldEventHandler<T> handler) {
            super(maxCheck, threshold, maxIter, handler);
            this.startDate = startDate;
            this.endDate = endDate;
            if (startDate.durationFrom(endDate).getReal() >= 0) {
                throw new RuntimeException("StartDate(" + startDate + ") should be before EndDate(" + endDate + ")");
            }
        }

        @Override
        public T g(final FieldSpacecraftState<T> s) {
            final FieldAbsoluteDate<T> gDate = s.getDate();
            final T durationFromStart = gDate.durationFrom(startDate);
            if (durationFromStart.getReal() < 0) {
                return durationFromStart; // before interval
            }
            final T durationBeforeEnd = endDate.durationFrom(gDate);
            if (durationBeforeEnd.getReal() < 0) {
                return durationBeforeEnd; // after interval
            }

            final T ret = FastMath.min(durationFromStart, durationBeforeEnd); // take the closest date
            if (ret.isNaN()) {
                throw new RuntimeException("Detector of type " + this.getClass().getSimpleName() + " returned NaN");
            }
            return ret;
        }

        @Override
        protected DateIntervalFieldDetector<T> create(final FieldAdaptableInterval<T> newMaxCheck, final T newThreshold, final int newMaxIter,
                                                      final FieldEventHandler<T> newHandler) {
            return new DateIntervalFieldDetector<>(startDate, endDate, newMaxCheck, newThreshold, newMaxIter, newHandler);
        }

    }

    private static class StartStopInterval extends StartStopEventsTrigger<DateIntervalDetector, NegateDetector> {

        public StartStopInterval(final DateIntervalDetector intervalDetector) {
            super(intervalDetector, BooleanDetector.notCombine(intervalDetector));
        }

        @Override
        protected <D extends FieldAbstractDetector<D, S>, S extends CalculusFieldElement<S>>
            FieldAbstractDetector<D, S> convertStartDetector(Field<S> field, DateIntervalDetector detector) {
            final FieldAbsoluteDate<S> fieldStart = new FieldAbsoluteDate<>(field, detector.startDate);
            final FieldAbsoluteDate<S> fieldEnd   = new FieldAbsoluteDate<>(field, detector.endDate);
            @SuppressWarnings("unchecked")
            final FieldAbstractDetector<D, S> converted = (FieldAbstractDetector<D, S>) new DateIntervalFieldDetector<>(fieldStart, fieldEnd);
            return converted;
        }

        @Override
        protected <D extends FieldAbstractDetector<D, S>, S extends CalculusFieldElement<S>>
            FieldAbstractDetector<D, S> convertStopDetector(Field<S> field, NegateDetector detector) {
            DateIntervalDetector       did        = (DateIntervalDetector) detector.getOriginal();
            final FieldAbsoluteDate<S> fieldStart = new FieldAbsoluteDate<>(field, did.startDate);
            final FieldAbsoluteDate<S> fieldEnd   = new FieldAbsoluteDate<>(field, did.endDate);
            final FieldAdaptableInterval<S> maxCheck = s -> did.getMaxCheckInterval().currentInterval(s.toSpacecraftState());
            @SuppressWarnings("unchecked")
            final FieldAbstractDetector<D, S> converted = (FieldAbstractDetector<D, S>)
            new FieldNegateDetector<>(new FieldDateDetector<S>(field, fieldStart, fieldEnd).
                                      withMaxCheck(maxCheck).
                                      withThreshold(field.getZero().newInstance(did.getThreshold())));
            return converted;
        }

        @Override
        public List<ParameterDriver> getParametersDrivers() {
            return Collections.emptyList();
        }

    }

    private static class StartStopNoField<A extends AbstractDetector<A>>
        extends StartStopEventsTrigger<A, NegateDetector> {

        public StartStopNoField(final A intervalDetector) {
            super(intervalDetector, BooleanDetector.notCombine(intervalDetector));
        }

        @Override
        protected <D extends FieldAbstractDetector<D, S>, S extends CalculusFieldElement<S>>
            FieldAbstractDetector<D, S> convertStartDetector(Field<S> field, A detector) {
            throw new OrekitException(OrekitMessages.FUNCTION_NOT_IMPLEMENTED,
                                      "StartStopNoField with CalculusFieldElement");
        }

        @Override
        protected <D extends FieldAbstractDetector<D, S>, S extends CalculusFieldElement<S>>
            FieldAbstractDetector<D, S> convertStopDetector(Field<S> field, NegateDetector detector) {
            throw new OrekitException(OrekitMessages.FUNCTION_NOT_IMPLEMENTED,
                                      "StartStopNoField with CalculusFieldElement");
        }

        @Override
        public List<ParameterDriver> getParametersDrivers() {
            return Collections.emptyList();
        }

    }

    private static class RegisteringResetter implements ManeuverTriggersResetter {

        private final List<AbsoluteDate> startStates = new ArrayList<>();
        private final List<AbsoluteDate> stopStates  = new ArrayList<>();
        
        @Override
        public void maneuverTriggered(SpacecraftState state, boolean start) {
            if (start) {
                startStates.add(state.getDate());
            } else {
                stopStates.add(state.getDate());
            }
        }

        @Override
        public SpacecraftState resetState(SpacecraftState state) {
            return state;
        }

    }

    public static NumericalPropagator buildNumericalPropagator(final Orbit initialOrbit) {
        final OrbitType orbitType = OrbitType.EQUINOCTIAL;
        final double minStep = 1e-6;
        final double maxStep = 100;

        final double[][] tol = NumericalPropagator.tolerances(1.0e-5, initialOrbit, orbitType);
        final DormandPrince54Integrator integrator = new DormandPrince54Integrator(minStep, maxStep, tol[0], tol[1]);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(orbitType);
        propagator.setAttitudeProvider(buildVelocityAttitudeProvider(initialOrbit.getFrame()));
        return propagator;
    }

    private static KeplerianOrbit buildInitOrbit() {
        return buildInitOrbitWithAnomaly(FastMath.toRadians(0));
    }

    private static KeplerianOrbit buildInitOrbitWithAnomaly(final double anomaly) {
        final AbsoluteDate date = new AbsoluteDate(2020, 01, 01, TimeScalesFactory.getUTC());

        final double sma = Constants.EGM96_EARTH_EQUATORIAL_RADIUS + 700e3;
        final double ecc = 0.01;
        final double inc = FastMath.toRadians(60);
        final double pa = FastMath.toRadians(0);
        final double raan = 0.;

        final KeplerianOrbit kep = new KeplerianOrbit(sma, ecc, inc, pa, raan, anomaly, PositionAngleType.MEAN,
                FramesFactory.getCIRF(IERSConventions.IERS_2010, true), date, Constants.EGM96_EARTH_MU);
        return kep;
    }

    private static AttitudeProvider buildVelocityAttitudeProvider(final Frame frame) {
        return new LofOffset(frame, LOFType.TNW);
    }

    private static ThrustDirectionAndAttitudeProvider buildVelocityThrustDirectionProvider() {
        return ThrustDirectionAndAttitudeProvider.buildFromFixedDirectionInSatelliteFrame(Vector3D.PLUS_I);
    }

    private ConfigurableLowThrustManeuver buildApogeeManeuver() {

        final ApogeeCenteredIntervalDetector maneuverStartDetector = new ApogeeCenteredIntervalDetector(halfThrustArc,
                PositionAngleType.MEAN, new ContinueOnEvent());

        // thrust in velocity direction to increase semi-major-axis
        return new ConfigurableLowThrustManeuver(buildVelocityThrustDirectionProvider(),
                                                 new StartStopNoField<>(maneuverStartDetector),
                                                 thrust, isp);
    }

    private ConfigurableLowThrustManeuver buildPerigeeManeuver() {

        final PerigeeCenteredIntervalDetector maneuverStartDetector = new PerigeeCenteredIntervalDetector(halfThrustArc,
                PositionAngleType.MEAN, new ContinueOnEvent());

        // thrust in velocity direction to increase semi-major-axis
        return new ConfigurableLowThrustManeuver(buildVelocityThrustDirectionProvider(),
                                                 new StartStopNoField<>(maneuverStartDetector),
                                                 thrust, isp);
    }

    private ConfigurableLowThrustManeuver buildPsoManeuver() {

        final PositionAngleDetector maneuverStartDetector = new PositionAngleDetector(OrbitType.EQUINOCTIAL,
                                                                                      PositionAngleType.MEAN, FastMath.toRadians(0.0));

        // thrust in velocity direction to increase semi-major-axis
        return new ConfigurableLowThrustManeuver(buildVelocityThrustDirectionProvider(),
                                                 new StartStopNoField<>(maneuverStartDetector),
                                                 thrust, isp);
    }

    @Test
    public void testNominalUseCase() {
        /////////////////// initial conditions /////////////////////////////////
        final KeplerianOrbit initOrbit = buildInitOrbit();
        final double initMass = 20;
        final SpacecraftState initialState = new SpacecraftState(initOrbit, initMass);
        final AbsoluteDate initialDate = initOrbit.getDate();
        final double simulationDuration = 2 * 86400;
        final AbsoluteDate finalDate = initialDate.shiftedBy(simulationDuration);

        /////////////////// propagations /////////////////////////////////

        final NumericalPropagator numericalPropagator = buildNumericalPropagator(initOrbit);
        numericalPropagator.addForceModel(buildApogeeManeuver());
        numericalPropagator.addForceModel(buildPerigeeManeuver());
        numericalPropagator.setInitialState(initialState);
        final SpacecraftState finalStateNumerical = numericalPropagator.propagate(finalDate);

        /////////////////// results check /////////////////////////////////
        final double expectedPropellantConsumption = -0.022;
        final double expectedDeltaSemiMajorAxisRealized = 16397;
        Assertions.assertEquals(expectedPropellantConsumption, finalStateNumerical.getMass() - initialState.getMass(),
                0.005);
        Assertions.assertEquals(expectedDeltaSemiMajorAxisRealized, finalStateNumerical.getA() - initialState.getA(), 100);
    }

    @Test
    public void testFielddPropagationDisabled() {
        doTestFielddPropagationDisabled(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFielddPropagationDisabled(Field<T> field) {
        /////////////////// initial conditions /////////////////////////////////
        final KeplerianOrbit o = buildInitOrbit();
        final FieldKeplerianOrbit<T> initOrbit = new FieldKeplerianOrbit<>(new FieldPVCoordinates<>(field, o.getPVCoordinates()),
                                                                           o.getFrame(), new FieldAbsoluteDate<>(field, o.getDate()),
                                                                           field.getZero().newInstance(o.getMu()));
        final double initMass = 20;
        final FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(initOrbit, field.getZero().newInstance(initMass));
        final FieldAbsoluteDate<T> initialDate = initOrbit.getDate();
        final double simulationDuration = -2 * 86400; // backward
        final FieldAbsoluteDate<T> finalDate = initialDate.shiftedBy(simulationDuration);

        /////////////////// propagation /////////////////////////////////
        final OrbitType orbitType = OrbitType.EQUINOCTIAL;
        final double minStep = 1e-6;
        final double maxStep = 100;

        final double[][] tol = FieldNumericalPropagator.tolerances(field.getZero().newInstance(1.0e-5), initOrbit, orbitType);
        final DormandPrince54FieldIntegrator<T> integrator = new DormandPrince54FieldIntegrator<>(field, minStep, maxStep, tol[0], tol[1]);
        final FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(orbitType);
        propagator.setAttitudeProvider(buildVelocityAttitudeProvider(o.getFrame()));
        propagator.addForceModel(buildApogeeManeuver());
        propagator.setInitialState(initialState);
        try {
            propagator.propagate(finalDate);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.FUNCTION_NOT_IMPLEMENTED, oe.getSpecifier());
            Assertions.assertEquals("StartStopNoField with CalculusFieldElement", oe.getParts()[0]);
        }

    }

    @Test
    public void testDateBasedManeuverTriggers() {

        final double f = 0.1;
        final double isp = 2000;
        final double duration = 3000.0;

        final Orbit orbit =
            new KeplerianOrbit(24396159, 0.72831215, FastMath.toRadians(7),
                               FastMath.toRadians(180), FastMath.toRadians(261),
                               FastMath.toRadians(0.0), PositionAngleType.MEAN,
                               FramesFactory.getEME2000(),
                               new AbsoluteDate(new DateComponents(2004, 01, 01),
                                                new TimeComponents(23, 30, 00.000),
                                                TimeScalesFactory.getUTC()),
                               Constants.EIGEN5C_EARTH_MU);
        final AttitudeProvider law = new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS);
        final SpacecraftState initialState =
            new SpacecraftState(orbit, law.getAttitude(orbit, orbit.getDate(), orbit.getFrame()), 2500.0);
        final AbsoluteDate startDate = orbit.getDate().shiftedBy(17461.084);

        // forward propagation
        final NumericalPropagator forwardPropagator = buildNumericalPropagator(orbit);
        forwardPropagator.addForceModel(new ConfigurableLowThrustManeuver(buildVelocityThrustDirectionProvider(),
                                                                          new DateBasedManeuverTriggers(startDate, duration),
                                                                          f, isp));
        forwardPropagator.setInitialState(initialState);
        final SpacecraftState finalStateNumerical = forwardPropagator.propagate(startDate.shiftedBy(duration + 900.0));

        // backward propagation
        final NumericalPropagator backwardPropagator = buildNumericalPropagator(finalStateNumerical.getOrbit());
        backwardPropagator.addForceModel(new ConfigurableLowThrustManeuver(buildVelocityThrustDirectionProvider(),
                                                                           new DateBasedManeuverTriggers(startDate, duration),
                                                                           f, isp));
        backwardPropagator.setInitialState(finalStateNumerical);
        final SpacecraftState recoveredStateNumerical = backwardPropagator.propagate(orbit.getDate());

        /////////////////// results check /////////////////////////////////
        Assertions.assertEquals(0.0,
                            Vector3D.distance(orbit.getPosition(),
                                              recoveredStateNumerical.getPosition()),
                            0.015);

    }

    @Test
    public void testBackwardPropagationEnabled() {

        final double f = 0.1;
        final double isp = 2000;
        final double duration = 3000.0;

        final Orbit orbit =
            new KeplerianOrbit(24396159, 0.72831215, FastMath.toRadians(7),
                               FastMath.toRadians(180), FastMath.toRadians(261),
                               FastMath.toRadians(0.0), PositionAngleType.MEAN,
                               FramesFactory.getEME2000(),
                               new AbsoluteDate(new DateComponents(2004, 01, 01),
                                                new TimeComponents(23, 30, 00.000),
                                                TimeScalesFactory.getUTC()),
                               Constants.EIGEN5C_EARTH_MU);
        final AttitudeProvider law = new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS);
        final SpacecraftState initialState =
            new SpacecraftState(orbit, law.getAttitude(orbit, orbit.getDate(), orbit.getFrame()), 2500.0);
        final AbsoluteDate startDate = orbit.getDate().shiftedBy(17461.084);
        final DateIntervalDetector intervalDetector = new DateIntervalDetector(startDate, startDate.shiftedBy(duration));

        // forward propagation
        final NumericalPropagator forwardPropagator = buildNumericalPropagator(orbit);
        final StartStopEventsTrigger<DateIntervalDetector, NegateDetector> forwardDetector = new StartStopInterval(intervalDetector);
        final RegisteringResetter forwardRegistering = new RegisteringResetter();
        forwardDetector.addResetter(forwardRegistering);
        forwardPropagator.addForceModel(new ConfigurableLowThrustManeuver(buildVelocityThrustDirectionProvider(),
                                                                          forwardDetector, f, isp));
        forwardPropagator.setInitialState(initialState);
        final SpacecraftState finalStateNumerical = forwardPropagator.propagate(startDate.shiftedBy(duration + 900.0));
        Assertions.assertEquals(0.0, forwardRegistering.startStates.get(0).durationFrom(startDate), 1.0e-16);
        Assertions.assertEquals(duration, forwardRegistering.stopStates.get(0).durationFrom(startDate), 1.0e-16);

        // backward propagation
        final NumericalPropagator backwardPropagator = buildNumericalPropagator(finalStateNumerical.getOrbit());
        final StartStopEventsTrigger<DateIntervalDetector, NegateDetector> backwardDetector = new StartStopInterval(intervalDetector);
        final RegisteringResetter backwardRegistering = new RegisteringResetter();
        backwardDetector.addResetter(backwardRegistering);
        backwardPropagator.addForceModel(new ConfigurableLowThrustManeuver(buildVelocityThrustDirectionProvider(),
                                                                           backwardDetector, f, isp));
        backwardPropagator.setInitialState(finalStateNumerical);
        final SpacecraftState recoveredStateNumerical = backwardPropagator.propagate(orbit.getDate());
        Assertions.assertEquals(0.0, backwardRegistering.startStates.get(0).durationFrom(startDate), 1.0e-16);
        Assertions.assertEquals(duration, backwardRegistering.stopStates.get(0).durationFrom(startDate), 1.0e-16);

        Assertions.assertFalse(backwardDetector.isFiring(new FieldAbsoluteDate<>(Binary64Field.getInstance(), startDate.shiftedBy(-0.001)),
                                                     null));
        Assertions.assertTrue(backwardDetector.isFiring(new FieldAbsoluteDate<>(Binary64Field.getInstance(), startDate.shiftedBy(+0.001)),
                                                     null));
        Assertions.assertTrue(backwardDetector.isFiring(new FieldAbsoluteDate<>(Binary64Field.getInstance(), startDate.shiftedBy(duration - 0.001)),
                                                     null));
        Assertions.assertFalse(backwardDetector.isFiring(new FieldAbsoluteDate<>(Binary64Field.getInstance(), startDate.shiftedBy(duration + 0.001)),
                                                     null));
        /////////////////// results check /////////////////////////////////
        Assertions.assertEquals(0.0,
                            Vector3D.distance(orbit.getPosition(),
                                              recoveredStateNumerical.getPosition()),
                            0.015);

    }

    @Test
    public void testInitBefore() {
        // thrust arc is around apogee which is on anomaly PI
        /////////////////// initial conditions /////////////////////////////////
        final KeplerianOrbit initOrbit = buildInitOrbitWithAnomaly(0);
        final double initMass = 20;
        final SpacecraftState initialState = new SpacecraftState(initOrbit, initMass);
        final AbsoluteDate initialDate = initOrbit.getDate();
        final double simulationDuration = 10;
        final AbsoluteDate finalDate = initialDate.shiftedBy(simulationDuration);

        /////////////////// propagation /////////////////////////////////
        final NumericalPropagator numericalPropagatorForward = buildNumericalPropagator(initOrbit);
        numericalPropagatorForward.addForceModel(buildApogeeManeuver());
        numericalPropagatorForward.setInitialState(initialState);
        final SpacecraftState finalState = numericalPropagatorForward.propagate(finalDate);
        // check firing did not happened
        Assertions.assertTrue(finalState.getMass() == initialState.getMass());
    }

    @Test
    public void testInitNearStart() {
        // thrust arc is around apogee which is on anomaly PI
        /////////////////// initial conditions /////////////////////////////////
        // we can not start exactly on start due to angle conversion, the g function is
        // equal to 1e-15 so this is not exactly the test of the specific specific use
        // case. Another test based on
        // other detectors will do that
        final KeplerianOrbit initOrbit = buildInitOrbitWithAnomaly(FastMath.PI - halfThrustArc);
        final double initMass = 20;
        final SpacecraftState initialState = new SpacecraftState(initOrbit, initMass);
        final AbsoluteDate initialDate = initOrbit.getDate();
        final double simulationDuration = 10;
        final AbsoluteDate finalDate = initialDate.shiftedBy(simulationDuration);

        /////////////////// propagation /////////////////////////////////
        final NumericalPropagator numericalPropagatorForward = buildNumericalPropagator(initOrbit);
        final ConfigurableLowThrustManeuver maneuver = buildApogeeManeuver();
        numericalPropagatorForward.addForceModel(maneuver);
        numericalPropagatorForward.setInitialState(initialState);
        final SpacecraftState finalState = numericalPropagatorForward.propagate(finalDate);
        // check firing happened
        Assertions.assertTrue(finalState.getMass() < initialState.getMass());

        // call init again, to check nothing weir happens (and improving test coverage)
        maneuver.init(initialState, finalDate);

    }

    @Test
    public void testInitOnStart() {
        /////////////////// initial conditions /////////////////////////////////
        final KeplerianOrbit initOrbit = buildInitOrbit();
        final double initMass = 20;
        final SpacecraftState initialState = new SpacecraftState(initOrbit, initMass);
        final AbsoluteDate initialDate = initOrbit.getDate();
        final double simulationDuration = 10;
        final AbsoluteDate finalDate = initialDate.shiftedBy(simulationDuration);

        final DateIntervalDetector maneuverStartDetector = new DateIntervalDetector(initialDate,
                initialDate.shiftedBy(60));

        final ConfigurableLowThrustManeuver maneuver =
                        new ConfigurableLowThrustManeuver(buildVelocityThrustDirectionProvider(),
                                                          new StartStopNoField<>(maneuverStartDetector),
                                                          thrust, isp);

        /////////////////// propagation /////////////////////////////////
        final NumericalPropagator numericalPropagatorForward = buildNumericalPropagator(initOrbit);
        numericalPropagatorForward.addForceModel(maneuver);
        numericalPropagatorForward.setInitialState(initialState);
        final SpacecraftState finalState = numericalPropagatorForward.propagate(finalDate);
        // check firing happened
        Assertions.assertTrue(finalState.getMass() < initialState.getMass());
    }

    @Test
    public void testInitFiring() {
        // thrust arc is around apogee which is on anomaly PI
        /////////////////// initial conditions /////////////////////////////////
        final KeplerianOrbit initOrbit = buildInitOrbitWithAnomaly(FastMath.PI);
        final double initMass = 20;
        final SpacecraftState initialState = new SpacecraftState(initOrbit, initMass);
        final AbsoluteDate initialDate = initOrbit.getDate();
        final double simulationDuration = 10;
        final AbsoluteDate finalDate = initialDate.shiftedBy(simulationDuration);

        /////////////////// propagation /////////////////////////////////
        final NumericalPropagator numericalPropagatorForward = buildNumericalPropagator(initOrbit);
        numericalPropagatorForward.addForceModel(buildApogeeManeuver());
        numericalPropagatorForward.setInitialState(initialState);
        final SpacecraftState finalState = numericalPropagatorForward.propagate(finalDate);
        // check firing happened
        Assertions.assertTrue(finalState.getMass() < initialState.getMass());
    }

    @Test
    public void testInitNearEndOfStart() {
        // thrust arc is around apogee which is on anomaly PI
        /////////////////// initial conditions /////////////////////////////////
        // we can not start exactly on end of start due to angle conversion, the g
        // function is
        // equal to 1e-15 so this is not exactly the test of the specific use case.
        // Another test based on
        // other detector will do that
        final KeplerianOrbit initOrbit = buildInitOrbitWithAnomaly(FastMath.PI + halfThrustArc);
        final double initMass = 20;
        final SpacecraftState initialState = new SpacecraftState(initOrbit, initMass);
        final AbsoluteDate initialDate = initOrbit.getDate();
        final double simulationDuration = 10;
        final AbsoluteDate finalDate = initialDate.shiftedBy(simulationDuration);

        /////////////////// propagation /////////////////////////////////
        final NumericalPropagator numericalPropagatorForward = buildNumericalPropagator(initOrbit);
        numericalPropagatorForward.addForceModel(buildApogeeManeuver());
        numericalPropagatorForward.setInitialState(initialState);
        final SpacecraftState finalState = numericalPropagatorForward.propagate(finalDate);
        // check firing did not happened
        Assertions.assertTrue(finalState.getMass() == initialState.getMass());
    }

    @Test
    public void testInitOnStop() {
        /////////////////// initial conditions /////////////////////////////////
        final KeplerianOrbit initOrbit = buildInitOrbit();
        final double initMass = 20;
        final SpacecraftState initialState = new SpacecraftState(initOrbit, initMass);
        final AbsoluteDate initialDate = initOrbit.getDate();
        final double simulationDuration = 10;
        final AbsoluteDate finalDate = initialDate.shiftedBy(simulationDuration);

        final DateIntervalDetector maneuverStartDetector = new DateIntervalDetector(initialDate.shiftedBy(-60),
                initialDate);

        final ConfigurableLowThrustManeuver maneuver =
                        new ConfigurableLowThrustManeuver(buildVelocityThrustDirectionProvider(),
                                                          new StartStopNoField<>(maneuverStartDetector),
                                                          thrust, isp);

        /////////////////// propagation /////////////////////////////////
        final NumericalPropagator numericalPropagatorForward = buildNumericalPropagator(initOrbit);
        numericalPropagatorForward.addForceModel(maneuver);
        numericalPropagatorForward.setInitialState(initialState);
        final SpacecraftState finalState = numericalPropagatorForward.propagate(finalDate);
        // check firing did not happen
        Assertions.assertTrue(finalState.getMass() == initialState.getMass());
    }

    @Test
    public void testInitAfter() {
        // thrust arc is around apogee which is on anomaly PI
        /////////////////// initial conditions /////////////////////////////////
        final KeplerianOrbit initOrbit = buildInitOrbitWithAnomaly(FastMath.PI + 2 * halfThrustArc);
        final double initMass = 20;
        final SpacecraftState initialState = new SpacecraftState(initOrbit, initMass);
        final AbsoluteDate initialDate = initOrbit.getDate();
        final double simulationDuration = 10;
        final AbsoluteDate finalDate = initialDate.shiftedBy(simulationDuration);

        /////////////////// propagation /////////////////////////////////
        final NumericalPropagator numericalPropagatorForward = buildNumericalPropagator(initOrbit);
        numericalPropagatorForward.addForceModel(buildApogeeManeuver());
        numericalPropagatorForward.setInitialState(initialState);
        final SpacecraftState finalState = numericalPropagatorForward.propagate(finalDate);
        // check firing did not happened
        Assertions.assertTrue(finalState.getMass() == initialState.getMass());
    }

    @Test
    public void testGetters() {
        final ApogeeCenteredIntervalDetector maneuverStartDetector = new ApogeeCenteredIntervalDetector(halfThrustArc,
                PositionAngleType.MEAN, new ContinueOnEvent());

        final ThrustDirectionAndAttitudeProvider attitudeProvider = buildVelocityThrustDirectionProvider();
        final ConfigurableLowThrustManeuver maneuver =
                        new ConfigurableLowThrustManeuver(attitudeProvider,
                                                          new StartStopNoField<>(maneuverStartDetector),
                                                          thrust, isp);
        Assertions.assertEquals(isp, maneuver.getIsp(), 1e-9);
        Assertions.assertEquals(thrust, maneuver.getThrustMagnitude(), 1e-9);
        Assertions.assertEquals(attitudeProvider, maneuver.getThrustDirectionProvider());

    }

    @Test
    public void testIssue874() {
        /////////////////// initial conditions /////////////////////////////////
        final KeplerianOrbit initOrbit = buildInitOrbit();
        final double initMass = 20;
        final SpacecraftState initialState = new SpacecraftState(initOrbit, initMass);
        final AbsoluteDate initialDate = initOrbit.getDate();
        final double simulationDuration = 2 * 86400;
        final AbsoluteDate finalDate = initialDate.shiftedBy(simulationDuration);

        /////////////////// propagations /////////////////////////////////

        final NumericalPropagator numericalPropagator = buildNumericalPropagator(initOrbit);
        numericalPropagator.addForceModel(buildPsoManeuver());
        numericalPropagator.setInitialState(initialState);
        final SpacecraftState finalStateNumerical = numericalPropagator.propagate(finalDate);

        /////////////////// results check /////////////////////////////////
        final double expectedPropellantConsumption = -0.0227;
        final double expectedDeltaSemiMajorAxisRealized = 16838;
        Assertions.assertEquals(expectedPropellantConsumption, finalStateNumerical.getMass() - initialState.getMass(),
                0.005);
        Assertions.assertEquals(expectedDeltaSemiMajorAxisRealized, finalStateNumerical.getA() - initialState.getA(), 100);
    }

}
