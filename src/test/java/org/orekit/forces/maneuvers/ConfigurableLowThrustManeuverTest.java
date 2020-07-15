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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.DormandPrince54Integrator;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.LofOffset;
import org.orekit.errors.OrekitException;
import org.orekit.forces.maneuvers.propulsion.ThrustDirectionAndAttitudeProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.BooleanDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.NegateDetector;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

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

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    public abstract class EquinoctialLongitudeIntervalDetector<T extends EventDetector>
            extends AbstractDetector<EquinoctialLongitudeIntervalDetector<T>> {

        /** */
        private double halfArcLength;
        /** */
        private final PositionAngle type;

        /**
         * Constructor.
         *
         * @param halfArcLength half length of the thrust arc. Must be in [0, pi]
         * @param type
         * @param handler
         */
        protected EquinoctialLongitudeIntervalDetector(final double halfArcLength, final PositionAngle type,
                final EventHandler<? super EquinoctialLongitudeIntervalDetector<T>> handler) {
            this(halfArcLength, type, maxCheck, maxThreshold, DEFAULT_MAX_ITER, handler);
        }

        public EquinoctialLongitudeIntervalDetector(final double halfArcLength, final PositionAngle type,
                final double maxCheck, final double threshold, final int maxIter,
                final EventHandler<? super EquinoctialLongitudeIntervalDetector<T>> handler) {
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

        public PositionAngle getType() {
            return type;
        }

        /**
         * 0 to disable the detector (will return -1 for g).
         *
         * @param halfArcLength
         */
        public void setHalfArcLength(final double halfArcLength) {
            if (Double.isNaN(halfArcLength)) {
                throw new RuntimeException("Trying to set an half arc with NaN value on " + getClass().getSimpleName());
            }
            if (halfArcLength < 0) {
                throw new RuntimeException("Trying to set a negative value (" + FastMath.toDegrees(halfArcLength) +
                        "deg) on  " + getClass().getSimpleName());
            }
            if (halfArcLength >= FastMath.PI) {
                throw new RuntimeException("Trying to set an half arc higher than PI (" +
                        FastMath.toDegrees(halfArcLength) + "deg) on " + getClass().getSimpleName());
            }

            this.halfArcLength = halfArcLength;
        }

    }

    public class PerigeeCenteredIntervalDetector
            extends EquinoctialLongitudeIntervalDetector<PerigeeCenteredIntervalDetector> {

        protected PerigeeCenteredIntervalDetector(final double halfArcLength, final PositionAngle type,
                final double maxCheck, final double threshold, final int maxIter,
                final EventHandler<? super EquinoctialLongitudeIntervalDetector<PerigeeCenteredIntervalDetector>> handler) {

            super(halfArcLength, type, maxCheck, threshold, maxIter, handler);
        }

        public PerigeeCenteredIntervalDetector(final double halfArcLength, final PositionAngle type,
                final EventHandler<? super EquinoctialLongitudeIntervalDetector<PerigeeCenteredIntervalDetector>> handler) {

            super(halfArcLength, type, maxCheck, maxThreshold, DEFAULT_MAX_ITER, handler);
        }

        @Override
        public double getReferenceEquinoctialLongitude(final SpacecraftState s) {

            final KeplerianOrbit orb = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(s.getOrbit());

            final double longitudeOfPerigee = orb.getRightAscensionOfAscendingNode() + orb.getPerigeeArgument();

            return longitudeOfPerigee;
        }

        @Override
        protected EquinoctialLongitudeIntervalDetector<PerigeeCenteredIntervalDetector> create(final double newMaxCheck,
                final double newThreshold, final int newMaxIter,
                final EventHandler<? super EquinoctialLongitudeIntervalDetector<PerigeeCenteredIntervalDetector>> newHandler) {
            return new PerigeeCenteredIntervalDetector(getHalfArcLength(), getType(), newMaxCheck, newThreshold,
                    newMaxIter, newHandler);
        }
    }

    public class ApogeeCenteredIntervalDetector
            extends EquinoctialLongitudeIntervalDetector<ApogeeCenteredIntervalDetector> {

        protected ApogeeCenteredIntervalDetector(final double halfArcLength, final PositionAngle type,
                final double maxCheck, final double threshold, final int maxIter,
                final EventHandler<? super EquinoctialLongitudeIntervalDetector<ApogeeCenteredIntervalDetector>> handler) {

            super(halfArcLength, type, maxCheck, threshold, maxIter, handler);
        }

        public ApogeeCenteredIntervalDetector(final double halfArcLength, final PositionAngle type,
                final EventHandler<? super EquinoctialLongitudeIntervalDetector<ApogeeCenteredIntervalDetector>> handler) {

            super(halfArcLength, type, maxCheck, maxThreshold, DEFAULT_MAX_ITER, handler);
        }

        @Override
        public double getReferenceEquinoctialLongitude(final SpacecraftState s) {

            final KeplerianOrbit orb = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(s.getOrbit());

            final double longitudeOfApogee = orb.getRightAscensionOfAscendingNode() + orb.getPerigeeArgument() +
                    FastMath.PI;

            return longitudeOfApogee;
        }

        @Override
        protected EquinoctialLongitudeIntervalDetector<ApogeeCenteredIntervalDetector> create(final double newMaxCheck,
                final double newThreshold, final int newMaxIter,
                final EventHandler<? super EquinoctialLongitudeIntervalDetector<ApogeeCenteredIntervalDetector>> newHandler) {

            return new ApogeeCenteredIntervalDetector(getHalfArcLength(), getType(), newMaxCheck, newThreshold,
                    newMaxIter, newHandler);
        }
    }

    public class DateIntervalDetector extends AbstractDetector<DateIntervalDetector> {

        /** */
        private final AbsoluteDate startDate;
        /** */
        private final AbsoluteDate endDate;

        public DateIntervalDetector(final AbsoluteDate startDate, final AbsoluteDate endDate) {
            this(startDate, endDate, 1.0e10, 1.e-9 /* values from DateDetector */, DEFAULT_MAX_ITER,
                    new StopOnEvent<DateIntervalDetector>());
        }

        protected DateIntervalDetector(final AbsoluteDate startDate, final AbsoluteDate endDate, final double maxCheck,
                final double threshold, final int maxIter, final EventHandler<? super DateIntervalDetector> handler) {
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
        protected DateIntervalDetector create(final double newMaxCheck, final double newThreshold, final int newMaxIter,
                final EventHandler<? super DateIntervalDetector> newHandler) {
            return new DateIntervalDetector(startDate, endDate, newMaxCheck, newThreshold, newMaxIter, newHandler);
        }

    }

    public static NumericalPropagator buildNumericalPropagator(final Orbit initialOrbit) {
        final double minStep = 1e-2;
        final double maxStep = 100;

        final double[] vecAbsoluteTolerance = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8, 1e-5 };
        final double[] vecRelativeTolerance = { 1e-10, 1e-10, 1e-10, 1e-10, 1e-10, 1e-10, 1e-10 };

        final DormandPrince54Integrator integrator = new DormandPrince54Integrator(minStep, maxStep,
                vecAbsoluteTolerance, vecRelativeTolerance);

        final NumericalPropagator propagator = new NumericalPropagator(integrator);
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

        final KeplerianOrbit kep = new KeplerianOrbit(sma, ecc, inc, pa, raan, anomaly, PositionAngle.MEAN,
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
                PositionAngle.MEAN, new ContinueOnEvent<>());
        final NegateDetector maneuverStopDetector = BooleanDetector.notCombine(maneuverStartDetector);

        // thrust in velocity direction to increase semi-major-axis
        return new ConfigurableLowThrustManeuver(buildVelocityThrustDirectionProvider(), maneuverStartDetector,
                maneuverStopDetector, thrust, isp);
    }

    private ConfigurableLowThrustManeuver buildPerigeeManeuver() {

        final PerigeeCenteredIntervalDetector maneuverStartDetector = new PerigeeCenteredIntervalDetector(halfThrustArc,
                PositionAngle.MEAN, new ContinueOnEvent<>());

        final NegateDetector maneuverStopDetector = BooleanDetector.notCombine(maneuverStartDetector);

        // thrust in velocity direction to increase semi-major-axis
        return new ConfigurableLowThrustManeuver(buildVelocityThrustDirectionProvider(), maneuverStartDetector,
                maneuverStopDetector, thrust, isp);
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
        Assert.assertEquals(expectedPropellantConsumption, finalStateNumerical.getMass() - initialState.getMass(),
                0.005);
        Assert.assertEquals(expectedDeltaSemiMajorAxisRealized, finalStateNumerical.getA() - initialState.getA(), 100);
    }

    @Test(expected = OrekitException.class)
    public void testBackwardPropagationDisabled() {
        /////////////////// initial conditions /////////////////////////////////
        final KeplerianOrbit initOrbit = buildInitOrbit();
        final double initMass = 20;
        final SpacecraftState initialState = new SpacecraftState(initOrbit, initMass);
        final AbsoluteDate initialDate = initOrbit.getDate();
        final double simulationDuration = -2 * 86400; // backward
        final AbsoluteDate finalDate = initialDate.shiftedBy(simulationDuration);

        /////////////////// propagation /////////////////////////////////
        final NumericalPropagator numericalPropagatorForward = buildNumericalPropagator(initOrbit);
        numericalPropagatorForward.addForceModel(buildApogeeManeuver());
        numericalPropagatorForward.setInitialState(initialState);
        numericalPropagatorForward.propagate(finalDate);

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
        Assert.assertTrue(finalState.getMass() == initialState.getMass());
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
        numericalPropagatorForward.addForceModel(buildApogeeManeuver());
        numericalPropagatorForward.setInitialState(initialState);
        final SpacecraftState finalState = numericalPropagatorForward.propagate(finalDate);
        // check firing happened
        Assert.assertTrue(finalState.getMass() < initialState.getMass());
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
        final NegateDetector maneuverStopDetector = BooleanDetector.notCombine(maneuverStartDetector);

        final ConfigurableLowThrustManeuver maneuver = new ConfigurableLowThrustManeuver(
                buildVelocityThrustDirectionProvider(), maneuverStartDetector, maneuverStopDetector, thrust, isp);

        /////////////////// propagation /////////////////////////////////
        final NumericalPropagator numericalPropagatorForward = buildNumericalPropagator(initOrbit);
        numericalPropagatorForward.addForceModel(maneuver);
        numericalPropagatorForward.setInitialState(initialState);
        final SpacecraftState finalState = numericalPropagatorForward.propagate(finalDate);
        // check firing happened
        Assert.assertTrue(finalState.getMass() < initialState.getMass());
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
        Assert.assertTrue(finalState.getMass() < initialState.getMass());
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
        Assert.assertTrue(finalState.getMass() == initialState.getMass());
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
        final NegateDetector maneuverStopDetector = BooleanDetector.notCombine(maneuverStartDetector);

        final ConfigurableLowThrustManeuver maneuver = new ConfigurableLowThrustManeuver(
                buildVelocityThrustDirectionProvider(), maneuverStartDetector, maneuverStopDetector, thrust, isp);

        /////////////////// propagation /////////////////////////////////
        final NumericalPropagator numericalPropagatorForward = buildNumericalPropagator(initOrbit);
        numericalPropagatorForward.addForceModel(maneuver);
        numericalPropagatorForward.setInitialState(initialState);
        final SpacecraftState finalState = numericalPropagatorForward.propagate(finalDate);
        // check firing did not happen
        Assert.assertTrue(finalState.getMass() == initialState.getMass());
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
        Assert.assertTrue(finalState.getMass() == initialState.getMass());
    }

    @Test
    public void testGetters() {
        final ApogeeCenteredIntervalDetector maneuverStartDetector = new ApogeeCenteredIntervalDetector(halfThrustArc,
                PositionAngle.MEAN, new ContinueOnEvent<>());
        final NegateDetector maneuverStopDetector = BooleanDetector.notCombine(maneuverStartDetector);

        final ThrustDirectionAndAttitudeProvider attitudeProvider = buildVelocityThrustDirectionProvider();
        final ConfigurableLowThrustManeuver maneuver = new ConfigurableLowThrustManeuver(attitudeProvider,
                maneuverStartDetector, maneuverStopDetector, thrust, isp);
        Assert.assertEquals(isp, maneuver.getISP(), 1e-9);
        Assert.assertEquals(thrust, maneuver.getThrust(), 1e-9);
        Assert.assertEquals(attitudeProvider, maneuver.getThrustDirectionProvider());

    }

    @Test(expected = OrekitException.class)
    public void testStartDetectorNotSet() {
        new ConfigurableLowThrustManeuver(buildVelocityThrustDirectionProvider(), null,
                new ApogeeCenteredIntervalDetector(halfThrustArc, PositionAngle.MEAN, new ContinueOnEvent<>()), thrust,
                isp);

    }

    @Test(expected = OrekitException.class)
    public void testStopDetectorNotSet() {
        new ConfigurableLowThrustManeuver(buildVelocityThrustDirectionProvider(),
                new ApogeeCenteredIntervalDetector(halfThrustArc, PositionAngle.MEAN, new ContinueOnEvent<>()), null,
                thrust, isp);

    }

}
