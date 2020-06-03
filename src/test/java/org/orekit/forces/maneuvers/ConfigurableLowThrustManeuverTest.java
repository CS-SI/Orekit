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
import org.orekit.forces.maneuvers.ConfigurableLowThrustManeuver;
import org.orekit.forces.maneuvers.ThrustDirectionProvider;
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
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class ConfigurableLowThrustManeuverTest  {
    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }
    
	public static double maxCheck = 100;
	public static double maxThreshold = AbstractDetector.DEFAULT_THRESHOLD;

	public abstract class EquinoctialLongitudeIntervalDetector<T extends EventDetector>
	extends AbstractDetector<EquinoctialLongitudeIntervalDetector<T>> {


		private double halfArcLength;
		private final PositionAngle type;

		/**
		 * @param halfArcLength: half length of the thrust arc. Must be in [0, pi]
		 * @param type
		 * @param handler
		 */
		protected EquinoctialLongitudeIntervalDetector (double halfArcLength, PositionAngle type,
				EventHandler<? super EquinoctialLongitudeIntervalDetector<T>> handler) {
			this(halfArcLength, type, maxCheck, maxThreshold, DEFAULT_MAX_ITER, handler);
		}

		public EquinoctialLongitudeIntervalDetector (double halfArcLength, PositionAngle type,
				double maxCheck, double threshold, int maxIter,
				EventHandler<? super EquinoctialLongitudeIntervalDetector<T>> handler) {
			super(maxCheck, threshold, maxIter, handler);
			this.halfArcLength = halfArcLength;
			this.type = type;
		}

		public abstract double getReferenceEquinoctialLongitude(SpacecraftState s); // node ...

		@Override
		public double g(SpacecraftState s) {
			if (halfArcLength <= 0) {
				return -1;
			}
			// cast is safe because type guaranteed to match
			EquinoctialOrbit orbit = (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(s.getOrbit());
			double current = MathUtils.normalizeAngle(orbit.getL(type), 0);
			double ret;
			double lonStart = getReferenceEquinoctialLongitude(s) - halfArcLength;
			double lonEnd = getReferenceEquinoctialLongitude(s) + halfArcLength;

			double diffStart = MathUtils.normalizeAngle(current, lonStart) - lonStart;
			double diffEnd = MathUtils.normalizeAngle(lonEnd, current) - current;

			double sin1 = FastMath.sin(diffStart);
			double sin2 = FastMath.sin(-diffEnd);
			if (lonEnd - lonStart < FastMath.PI) {
				ret = FastMath.min(sin1, -sin2);
			} else {
				ret = FastMath.max(sin1, -sin2);
			}
			if (Double.isNaN(ret)) {
				if (Double.isNaN(current)) {
					throw new RuntimeException(
							"Detector of type " + this.getClass().getSimpleName()
							+ " returned NaN because bad orbit provided");
				}
				throw new RuntimeException(
						"Detector of type " + this.getClass().getSimpleName() + " returned NaN");
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
		 * 0 to disable the detector (will return -1 for g)
		 */
		public void setHalfArcLength(double halfArcLength) {
			if (Double.isNaN(halfArcLength)) {
				throw new RuntimeException(
						"Trying to set an half arc with NaN value on " + getClass().getSimpleName());
			}
			if (halfArcLength < 0) {
				throw new RuntimeException("Trying to set a negative value ("
						+ FastMath.toDegrees(halfArcLength) + "deg) on " + getClass().getSimpleName());
			}
			if (halfArcLength >= FastMath.PI) {
				throw new RuntimeException("Trying to set an half arc higher than PI ("
						+ FastMath.toDegrees(halfArcLength) + "deg) on " + getClass().getSimpleName());
			}

			this.halfArcLength = halfArcLength;
		}

	}


	public class PerigeeCenteredIntervalDetector
	extends EquinoctialLongitudeIntervalDetector<PerigeeCenteredIntervalDetector> {

		protected PerigeeCenteredIntervalDetector (double halfArcLength, PositionAngle type,
				double maxCheck, double threshold, int maxIter,
				EventHandler<? super EquinoctialLongitudeIntervalDetector<PerigeeCenteredIntervalDetector>> handler) {

			super(halfArcLength, type, maxCheck, threshold, maxIter, handler);
		}

		public PerigeeCenteredIntervalDetector (double halfArcLength, PositionAngle type,
				EventHandler<? super EquinoctialLongitudeIntervalDetector<PerigeeCenteredIntervalDetector>> handler) {

			super(halfArcLength, type, maxCheck, maxThreshold, DEFAULT_MAX_ITER, handler);
		}

		@Override
		public double getReferenceEquinoctialLongitude(SpacecraftState s) {

			KeplerianOrbit orb = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(s.getOrbit());

			double longitudeOfPerigee = orb.getRightAscensionOfAscendingNode()
					+ orb.getPerigeeArgument();

			return longitudeOfPerigee;
		}

		@Override
		protected EquinoctialLongitudeIntervalDetector<PerigeeCenteredIntervalDetector> create(
				double newMaxCheck, double newThreshold, int newMaxIter,
				EventHandler<? super EquinoctialLongitudeIntervalDetector<PerigeeCenteredIntervalDetector>> newHandler) {
			return new PerigeeCenteredIntervalDetector(getHalfArcLength(), getType(), newMaxCheck,
					newThreshold, newMaxIter, newHandler);
		}
	}
	
	public class ApogeeCenteredIntervalDetector
	extends EquinoctialLongitudeIntervalDetector<ApogeeCenteredIntervalDetector> {

		protected ApogeeCenteredIntervalDetector (double halfArcLength, PositionAngle type,
				double maxCheck, double threshold, int maxIter,
				EventHandler<? super EquinoctialLongitudeIntervalDetector<ApogeeCenteredIntervalDetector>> handler) {

			super(halfArcLength, type, maxCheck, threshold, maxIter, handler);
		}

		public ApogeeCenteredIntervalDetector (double halfArcLength, PositionAngle type,
				EventHandler<? super EquinoctialLongitudeIntervalDetector<ApogeeCenteredIntervalDetector>> handler) {

			super(halfArcLength, type, maxCheck, maxThreshold, DEFAULT_MAX_ITER, handler);
		}

		@Override
		public double getReferenceEquinoctialLongitude(SpacecraftState s) {

			KeplerianOrbit orb = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(s.getOrbit());

			double longitudeOfApogee = orb.getRightAscensionOfAscendingNode() + orb.getPerigeeArgument()
			+ FastMath.PI;

			return longitudeOfApogee;
		}

		@Override
		protected EquinoctialLongitudeIntervalDetector<ApogeeCenteredIntervalDetector> create(
				double newMaxCheck, double newThreshold, int newMaxIter,
				EventHandler<? super EquinoctialLongitudeIntervalDetector<ApogeeCenteredIntervalDetector>> newHandler) {

			return new ApogeeCenteredIntervalDetector(getHalfArcLength(), getType(), newMaxCheck,
					newThreshold, newMaxIter, newHandler);
		}
	}
	
	public static NumericalPropagator buildNumericalPropagator(Orbit initialOrbit) {
		double minStep = 1e-2;
		double maxStep = 100;

		double[] vecAbsoluteTolerance = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8, 1e-5 };
		double[] vecRelativeTolerance = { 1e-10, 1e-10, 1e-10, 1e-10, 1e-10, 1e-10, 1e-10 };

		DormandPrince54Integrator integrator = new DormandPrince54Integrator(minStep, maxStep,
				vecAbsoluteTolerance, vecRelativeTolerance);

		final NumericalPropagator propagator = new NumericalPropagator(integrator);
		propagator.setAttitudeProvider(buildVelocityAttitudeProvider(initialOrbit.getFrame()));
		return propagator;
	}

	private static KeplerianOrbit initOrbit() throws IllegalArgumentException {
		AbsoluteDate date = new AbsoluteDate(2020, 01, 01, TimeScalesFactory.getUTC());

		double sma = Constants.EGM96_EARTH_EQUATORIAL_RADIUS + 700e3;
		double ecc = 0.01;
		double inc = FastMath.toRadians(60);
		double pa = FastMath.toRadians(0);
		double raan = 0.;
		double anomaly = FastMath.toRadians(0);

		KeplerianOrbit kep = new KeplerianOrbit(sma, ecc, inc, pa, raan, anomaly,
				PositionAngle.TRUE, FramesFactory.getCIRF(IERSConventions.IERS_2010, true), date,
				Constants.EGM96_EARTH_MU);
		return kep;
	}

	private static AttitudeProvider buildVelocityAttitudeProvider(Frame frame) {
		return new LofOffset(frame, LOFType.TNW);
	}

	private static ThrustDirectionProvider buildVelocityThrustDirectionProvider() {
		return ThrustDirectionProvider.buildFromFixedDirectionInSatelliteFrame(Vector3D.PLUS_I);
	}

	@Test
	public void runTest() {
		/////////////////// initial conditions /////////////////////////////////
		KeplerianOrbit intitOrbit = initOrbit();
		double initMass = 20;
		SpacecraftState initialState = new SpacecraftState(intitOrbit, initMass);
		AbsoluteDate initialDate = intitOrbit.getDate();
		double simulationDuration = 20 * 86400;
		AbsoluteDate finalDate = initialDate.shiftedBy(simulationDuration);

		/////////////////// maneuvers configuration /////////////////////////////////
		// 2 maneuvers on opposite nodes to preserve eccentricity
		double thrust = 2e-3;
		double isp = 800;
		double halfThrustArc = FastMath.PI / 4;
		ApogeeCenteredIntervalDetector maneuver1StartDetector = new ApogeeCenteredIntervalDetector(
				halfThrustArc, PositionAngle.MEAN, new ContinueOnEvent<>());
		PerigeeCenteredIntervalDetector maneuver2StartDetector = new PerigeeCenteredIntervalDetector(
				halfThrustArc, PositionAngle.MEAN, new ContinueOnEvent<>());

		NegateDetector maneuver1StopDetector = BooleanDetector.notCombine(maneuver1StartDetector);
		NegateDetector maneuver2StopDetector = BooleanDetector.notCombine(maneuver2StartDetector);

		// thrust in velocity direction to increase semi-major-axis
		ConfigurableLowThrustManeuver maneuver1Numerical = new ConfigurableLowThrustManeuver(
				buildVelocityThrustDirectionProvider(), maneuver1StartDetector,
				maneuver1StopDetector, thrust, isp);
		ConfigurableLowThrustManeuver maneuver2Numerical = new ConfigurableLowThrustManeuver(
				buildVelocityThrustDirectionProvider(), maneuver2StartDetector,
				maneuver2StopDetector, thrust, isp);

		/////////////////// propagations /////////////////////////////////

		NumericalPropagator numericalPropagator = buildNumericalPropagator(intitOrbit);
		numericalPropagator.addForceModel(maneuver1Numerical);
		numericalPropagator.addForceModel(maneuver2Numerical);
		numericalPropagator.setInitialState(initialState);
		SpacecraftState finalStateNumerical = numericalPropagator.propagate(finalDate);

		/////////////////// results check /////////////////////////////////
		double expectedPropellantConsumption = -0.22;
		double expectedDeltaSemiMajorAxisRealized = 166821;
		Assert.assertEquals(expectedPropellantConsumption,
				finalStateNumerical.getMass() - initialState.getMass(), 0.005);
		Assert.assertEquals(expectedDeltaSemiMajorAxisRealized,
				finalStateNumerical.getA() - initialState.getA(), 100);
	}

}
