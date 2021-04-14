/* Copyright 2002-2021 CS GROUP
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.AttitudesSequence;
import org.orekit.attitudes.CelestialBodyPointed;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.TimeStampedCacheException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldAdditionalStateProvider;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.sampling.FieldOrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

public class FieldEphemerisTest {

	@Before
	public void setUp() throws IllegalArgumentException, OrekitException {
		Utils.setDataRoot("regular-data");
	}

	private <T extends RealFieldElement<T>> T calculatePositionDelta(FieldSpacecraftState<T> state1,
			FieldSpacecraftState<T> state2) {
		return FieldVector3D.distance(state1.getPVCoordinates().getPosition(), state2.getPVCoordinates().getPosition());
	}

	private <T extends RealFieldElement<T>> T calculateVelocityDelta(FieldSpacecraftState<T> state1,
			FieldSpacecraftState<T> state2) {
		return FieldVector3D.distance(state1.getPVCoordinates().getVelocity(), state2.getPVCoordinates().getVelocity());
	}

	private <T extends RealFieldElement<T>> T calculateAttitudeDelta(FieldSpacecraftState<T> state1,
			FieldSpacecraftState<T> state2) {
		return FieldRotation.distance(state1.getAttitude().getRotation(), state2.getAttitude().getRotation());
	}

	// TESTS

	@Test
	public void testAttitudeOverride() {
		doTestAttitudeOverride(Decimal64Field.getInstance());
	}

	@Test
	public void testAttitudeSequenceTransition() {
		doTestAttitudeSequenceTransition(Decimal64Field.getInstance());
	}

	@Test
	public void testNonResettableState() {
		doTestNonResettableState(Decimal64Field.getInstance());
	}

	@Test
	public void testAdditionalStates() {
		doTestAdditionalStates(Decimal64Field.getInstance());
	}

	@Test
	public void testProtectedMethods() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		doTestProtectedMethods(Decimal64Field.getInstance());
	}

	@Test
	public void testExtrapolation() {
		doTestExtrapolation(Decimal64Field.getInstance());
	}

	@Test
	public void testIssue662() {
		doTestIssue662(Decimal64Field.getInstance());
	}

	@Test
	public void testIssue680() {
		doTestIssue680(Decimal64Field.getInstance());
	}

	public <T extends RealFieldElement<T>> void doTestAttitudeOverride(Field<T> field) {
		final T zero = field.getZero();
		// Conversion from double to Field
		FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field,
				new AbsoluteDate(new DateComponents(2004, 01, 01), TimeComponents.H00, TimeScalesFactory.getUTC()));
		FieldAbsoluteDate<T> finalDate = new FieldAbsoluteDate<>(field,
				new AbsoluteDate(new DateComponents(2004, 01, 02), TimeComponents.H00, TimeScalesFactory.getUTC()));
		T a = zero.add(7187990.1979844316);
		T e = zero.add(0.5e-4);
		T i = zero.add(1.7105407051081795);
		T omega = zero.add(1.9674147913622104);
		T OMEGA = zero.add(FastMath.toRadians(261));
		T lv = zero;
		T mu = zero.add(3.9860047e14);
		Frame inertialFrame = FramesFactory.getEME2000();

		FieldOrbit<T> initialState = new FieldKeplerianOrbit<>(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
				inertialFrame, initDate, mu);
		FieldPropagator<T> propagator = new FieldKeplerianPropagator<>(initialState);

		//

		final T positionTolerance = zero.add(1e-6);
		final T velocityTolerance = zero.add(1e-5);
		final T attitudeTolerance = zero.add(1e-6);

		int numberOfInterals = 1440;
		T deltaT = finalDate.durationFrom(initDate).divide(numberOfInterals);

		propagator.setAttitudeProvider(new LofOffset(inertialFrame, LOFType.VVLH));

		List<SpacecraftState> states = new ArrayList<SpacecraftState>(numberOfInterals + 1);
		for (int j = 0; j <= numberOfInterals; j++) {
			states.add(propagator.propagate(initDate.shiftedBy((deltaT.multiply(j)))).toSpacecraftState());
		}

		int numInterpolationPoints = 2;
		FieldEphemeris<T> ephemPropagator = new FieldEphemeris<>(field, states, numInterpolationPoints);
		Assert.assertEquals(0, ephemPropagator.getManagedAdditionalStates().length);

		// First test that we got position, velocity and attitude nailed
		int numberEphemTestIntervals = 2880;
		deltaT = finalDate.durationFrom(initDate).divide(numberEphemTestIntervals);
		for (int j = 0; j <= numberEphemTestIntervals; j++) {
			FieldAbsoluteDate<T> currentDate = initDate.shiftedBy(deltaT.multiply(j));
			FieldSpacecraftState<T> ephemState = ephemPropagator.propagate(currentDate);
			FieldSpacecraftState<T> keplerState = propagator.propagate(currentDate);
			T positionDelta = calculatePositionDelta(ephemState, keplerState);
			T velocityDelta = calculateVelocityDelta(ephemState, keplerState);
			T attitudeDelta = calculateAttitudeDelta(ephemState, keplerState);
			Assert.assertEquals("VVLH Unmatched Position at: " + currentDate, 0.0, positionDelta.getReal(),
					positionTolerance.getReal());
			Assert.assertEquals("VVLH Unmatched Velocity at: " + currentDate, 0.0, velocityDelta.getReal(),
					velocityTolerance.getReal());
			Assert.assertEquals("VVLH Unmatched Attitude at: " + currentDate, 0.0, attitudeDelta.getReal(),
					attitudeTolerance.getReal());
		}

		// Now force an override on the attitude and check it against a Keplerian
		// propagator
		// setup identically to the first but with a different attitude
		// If override isn't working this will fail.
		propagator = new FieldKeplerianPropagator<>(propagator.getInitialState().getOrbit());
		propagator.setAttitudeProvider(new LofOffset(inertialFrame, LOFType.QSW));

		ephemPropagator.setAttitudeProvider(new LofOffset(inertialFrame, LOFType.QSW));
		for (int j = 0; j <= numberEphemTestIntervals; j++) {
			FieldAbsoluteDate<T> currentDate = initDate.shiftedBy(deltaT.multiply(j));
			FieldSpacecraftState<T> ephemState = ephemPropagator.propagate(currentDate);
			FieldSpacecraftState<T> keplerState = propagator.propagate(currentDate);
			T positionDelta = calculatePositionDelta(ephemState, keplerState);
			T velocityDelta = calculateVelocityDelta(ephemState, keplerState);
			T attitudeDelta = calculateAttitudeDelta(ephemState, keplerState);
			Assert.assertEquals("QSW Unmatched Position at: " + currentDate, 0.0, positionDelta.getReal(),
					positionTolerance.getReal());
			Assert.assertEquals("QSW Unmatched Velocity at: " + currentDate, 0.0, velocityDelta.getReal(),
					velocityTolerance.getReal());
			Assert.assertEquals("QSW Unmatched Attitude at: " + currentDate, 0.0, attitudeDelta.getReal(),
					attitudeTolerance.getReal());
		}
	}

	public <T extends RealFieldElement<T>> void doTestAttitudeSequenceTransition(Field<T> field) {
		final T zero = field.getZero();
		// Initialize the orbit
		final FieldAbsoluteDate<T> initialDate = new FieldAbsoluteDate<>(field, 2003, 01, 01, 0, 0, 00.000,
				TimeScalesFactory.getUTC());
		final FieldVector3D<T> position = new FieldVector3D<>(zero.add(-39098981.4866597), zero.add(-15784239.3610601),
				zero.add(78908.2289853595));
		final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(1151.00321021175), zero.add(-2851.14864755189),
				zero.add(-2.02133248357321));
		final FieldOrbit<T> initialOrbit = new FieldKeplerianOrbit<>(new FieldPVCoordinates<>(position, velocity),
				FramesFactory.getGCRF(), initialDate, zero.add(Constants.WGS84_EARTH_MU));
		final FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(initialOrbit);

		// Define attitude laws
		AttitudeProvider before = new CelestialBodyPointed(FramesFactory.getICRF(), CelestialBodyFactory.getSun(),
				Vector3D.PLUS_K, Vector3D.PLUS_I, Vector3D.PLUS_K);
		AttitudeProvider after = new CelestialBodyPointed(FramesFactory.getICRF(), CelestialBodyFactory.getEarth(),
				Vector3D.PLUS_K, Vector3D.PLUS_I, Vector3D.PLUS_K);

		// Define attitude sequence
		FieldAbsoluteDate<T> switchDate = initialDate.shiftedBy(86400.0);
		double transitionTime = 600;
		DateDetector switchDetector = new DateDetector(switchDate.toAbsoluteDate()).withHandler(new ContinueOnEvent<DateDetector>());

		AttitudesSequence attitudeSequence = new AttitudesSequence();
		attitudeSequence.resetActiveProvider(before);
		attitudeSequence.addSwitchingCondition(before, after, switchDetector, true, false, transitionTime, AngularDerivativesFilter.USE_RR, null);

		FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field,
				new DormandPrince853FieldIntegrator<>(field, 0.1, 500, 1e-9, 1e-9));
		propagator.setInitialState(initialState);

		// Propagate and build ephemeris
		final List<SpacecraftState> propagatedStates = new ArrayList<>();

		propagator.setMasterMode(zero.add(60), new FieldOrekitFixedStepHandler<T>() {

			@Override
			public void handleStep(FieldSpacecraftState<T> currentState, boolean isLast) throws OrekitException {
				propagatedStates.add(currentState.toSpacecraftState());
			}
		});
		propagator.propagate(initialDate.shiftedBy(zero.add(2 * 86400.0)));
		final FieldEphemeris<T> ephemeris = new FieldEphemeris<>(field, propagatedStates, 8);

		// Add attitude switch event to ephemeris
		ephemeris.setAttitudeProvider(attitudeSequence);
		attitudeSequence.registerSwitchEvents(field, ephemeris);

		// Propagate with a step during the transition
		FieldAbsoluteDate<T> endDate = initialDate.shiftedBy(zero.add(2 * 86400.0));
		FieldSpacecraftState<T> stateBefore = ephemeris.getInitialState();
		ephemeris.propagate(switchDate.shiftedBy(transitionTime/2));
		FieldSpacecraftState<T> stateAfter = ephemeris.propagate(endDate);

		// Check that the attitudes are correct
		Assert.assertEquals(before.getAttitude(stateBefore.getOrbit(), stateBefore.getDate(), stateBefore.getFrame())
				.getRotation().getQ0().getReal(), stateBefore.getAttitude().getRotation().getQ0().getReal(), 1.0E-16);
		Assert.assertEquals(before.getAttitude(stateBefore.getOrbit(), stateBefore.getDate(), stateBefore.getFrame())
				.getRotation().getQ1().getReal(), stateBefore.getAttitude().getRotation().getQ1().getReal(), 1.0E-16);
		Assert.assertEquals(before.getAttitude(stateBefore.getOrbit(), stateBefore.getDate(), stateBefore.getFrame())
				.getRotation().getQ2().getReal(), stateBefore.getAttitude().getRotation().getQ2().getReal(), 1.0E-16);
		Assert.assertEquals(before.getAttitude(stateBefore.getOrbit(), stateBefore.getDate(), stateBefore.getFrame())
				.getRotation().getQ3().getReal(), stateBefore.getAttitude().getRotation().getQ3().getReal(), 1.0E-16);

		Assert.assertEquals(after.getAttitude(stateAfter.getOrbit(), stateAfter.getDate(), stateAfter.getFrame())
				.getRotation().getQ0().getReal(), stateAfter.getAttitude().getRotation().getQ0().getReal(), 1.0E-16);
		Assert.assertEquals(after.getAttitude(stateAfter.getOrbit(), stateAfter.getDate(), stateAfter.getFrame())
				.getRotation().getQ1().getReal(), stateAfter.getAttitude().getRotation().getQ1().getReal(), 1.0E-16);
		Assert.assertEquals(after.getAttitude(stateAfter.getOrbit(), stateAfter.getDate(), stateAfter.getFrame())
				.getRotation().getQ2().getReal(), stateAfter.getAttitude().getRotation().getQ2().getReal(), 1.0E-16);
		Assert.assertEquals(after.getAttitude(stateAfter.getOrbit(), stateAfter.getDate(), stateAfter.getFrame())
				.getRotation().getQ3().getReal(), stateAfter.getAttitude().getRotation().getQ3().getReal(), 1.0E-16);
	}

	public <T extends RealFieldElement<T>> void doTestNonResettableState(Field<T> field) {
		try {
			final T zero = field.getZero();
			// Conversion from double to Field
			FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field,
					new AbsoluteDate(new DateComponents(2004, 01, 01), TimeComponents.H00, TimeScalesFactory.getUTC()));
			T a = zero.add(7187990.1979844316);
			T e = zero.add(0.5e-4);
			T i = zero.add(1.7105407051081795);
			T omega = zero.add(1.9674147913622104);
			T OMEGA = zero.add(FastMath.toRadians(261));
			T lv = zero;
			T mu = zero.add(3.9860047e14);
			Frame inertialFrame = FramesFactory.getEME2000();

			FieldOrbit<T> initialState = new FieldKeplerianOrbit<>(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
					inertialFrame, initDate, mu);
			FieldPropagator<T> propagator = new FieldKeplerianPropagator<>(initialState);

			//
			propagator.setAttitudeProvider(new LofOffset(inertialFrame, LOFType.VVLH));

			List<SpacecraftState> states = new ArrayList<SpacecraftState>();
			for (double dt = 0; dt >= -1200; dt -= 60.0) {
				states.add(propagator.propagate(initDate.shiftedBy(dt)).toSpacecraftState());
			}

			new FieldEphemeris<>(field, states, 2).resetInitialState(propagator.getInitialState());
			Assert.fail("an exception should have been thrown");
		} catch (OrekitException oe) {
			Assert.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
		}
	}

	public <T extends RealFieldElement<T>> void doTestAdditionalStates(Field<T> field) {
		final T zero = field.getZero();
		// Conversion from double to Field
		FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field,
				new AbsoluteDate(new DateComponents(2004, 01, 01), TimeComponents.H00, TimeScalesFactory.getUTC()));
		FieldAbsoluteDate<T> finalDate = new FieldAbsoluteDate<>(field,
				new AbsoluteDate(new DateComponents(2004, 01, 02), TimeComponents.H00, TimeScalesFactory.getUTC()));
		T a = zero.add(7187990.1979844316);
		T e = zero.add(0.5e-4);
		T i = zero.add(1.7105407051081795);
		T omega = zero.add(1.9674147913622104);
		T OMEGA = zero.add(FastMath.toRadians(261));
		T lv = zero;
		T mu = zero.add(3.9860047e14);
		Frame inertialFrame = FramesFactory.getEME2000();

		FieldOrbit<T> initialState = new FieldKeplerianOrbit<>(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
				inertialFrame, initDate, mu);
		FieldPropagator<T> propagator = new FieldKeplerianPropagator<>(initialState);

		//
		final String name1 = "dt0";
		final String name2 = "dt1";
		propagator.setAttitudeProvider(new LofOffset(inertialFrame, LOFType.VVLH));

		List<SpacecraftState> states = new ArrayList<SpacecraftState>();
		for (double dt = 0; dt >= -1200; dt -= 60.0) {
			final FieldSpacecraftState<T> original = propagator.propagate(initDate.shiftedBy(dt));
			final FieldSpacecraftState<T> expanded = original.addAdditionalState(name2,
					original.getDate().durationFrom(finalDate));
			states.add(expanded.toSpacecraftState());
		}

		final FieldPropagator<T> ephem = new FieldEphemeris<>(field, states, 2);
		ephem.addAdditionalStateProvider(new FieldAdditionalStateProvider<T>() {
			public String getName() {
				return name1;
			}

			@Override
			public T[] getAdditionalState(FieldSpacecraftState<T> state) {
				T[] tab = MathArrays.buildArray(field, 1);
				tab[0] = state.getDate().durationFrom(initDate);
				return tab;
			}
		});

		final String[] additional = ephem.getManagedAdditionalStates();
		Arrays.sort(additional);
		Assert.assertEquals(2, additional.length);
		Assert.assertEquals(name1, ephem.getManagedAdditionalStates()[0]);
		Assert.assertEquals(name2, ephem.getManagedAdditionalStates()[1]);
		Assert.assertTrue(ephem.isAdditionalStateManaged(name1));
		Assert.assertTrue(ephem.isAdditionalStateManaged(name2));
		Assert.assertFalse(ephem.isAdditionalStateManaged("not managed"));

		FieldSpacecraftState<T> s = ephem.propagate(initDate.shiftedBy(-270.0));
		Assert.assertEquals(-270.0, s.getAdditionalState(name1)[0].getReal(), 1.0e-15);
		Assert.assertEquals(-86670.0, s.getAdditionalState(name2)[0].getReal(), 1.0e-15);
	}

	public <T extends RealFieldElement<T>> void doTestProtectedMethods(Field<T> field)
	    throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		final T zero = field.getZero();
		// Conversion from double to Field
		FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field,
				new AbsoluteDate(new DateComponents(2004, 01, 01), TimeComponents.H00, TimeScalesFactory.getUTC()));
		T a = zero.add(7187990.1979844316);
		T e = zero.add(0.5e-4);
		T i = zero.add(1.7105407051081795);
		T omega = zero.add(1.9674147913622104);
		T OMEGA = zero.add(FastMath.toRadians(261));
		T lv = zero;
		T mu = zero.add(3.9860047e14);
		Frame inertialFrame = FramesFactory.getEME2000();

		FieldOrbit<T> initialState = new FieldKeplerianOrbit<>(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
				inertialFrame, initDate, mu);
		FieldPropagator<T> propagator = new FieldKeplerianPropagator<>(initialState);

		//
		propagator.setAttitudeProvider(new LofOffset(inertialFrame, LOFType.VVLH));

		List<SpacecraftState> states = new ArrayList<SpacecraftState>();
		for (double dt = 0; dt >= -1200; dt -= 60.0) {
			final FieldSpacecraftState<T> original = propagator.propagate(initDate.shiftedBy(dt));
			final FieldSpacecraftState<T> modified = new FieldSpacecraftState<T>(original.getOrbit(),
					original.getAttitude(), original.getMass().subtract(0.0625 * dt));
			states.add(modified.toSpacecraftState());
		}

		final FieldPropagator<T> ephem = new FieldEphemeris<>(field, states, 2);
		Method propagateOrbit = FieldEphemeris.class.getDeclaredMethod("propagateOrbit", FieldAbsoluteDate.class, RealFieldElement[].class);
		propagateOrbit.setAccessible(true);
		Method getMass = FieldEphemeris.class.getDeclaredMethod("getMass", FieldAbsoluteDate.class);
		getMass.setAccessible(true);

		FieldSpacecraftState<T> s = ephem.propagate(initDate.shiftedBy(-270.0));
		@SuppressWarnings("unchecked")
		FieldOrbit<T> o = (FieldOrbit<T>) propagateOrbit.invoke(ephem, s.getDate(), MathArrays.buildArray(field, 0));
		//double m = ((Double) getMass.invoke(ephem, s.getDate())).doubleValue();
		 @SuppressWarnings("unchecked")
		T m = ((T) getMass.invoke(ephem, s.getDate()));
		Assert.assertEquals(0.0, FieldVector3D
				.distance(s.getPVCoordinates().getPosition(), o.getPVCoordinates().getPosition()).getReal(), 1.0e-15);
		Assert.assertEquals(s.getMass().getReal(), m.getReal(), 1.0e-15);
	}

	public <T extends RealFieldElement<T>> void doTestExtrapolation(Field<T> field) {
		final T zero = field.getZero();
		// Conversion from double to Field
		FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field,
				new AbsoluteDate(new DateComponents(2004, 01, 01), TimeComponents.H00, TimeScalesFactory.getUTC()));
		FieldAbsoluteDate<T> finalDate = new FieldAbsoluteDate<>(field,
				new AbsoluteDate(new DateComponents(2004, 01, 02), TimeComponents.H00, TimeScalesFactory.getUTC()));
		T a = zero.add(7187990.1979844316);
		T e = zero.add(0.5e-4);
		T i = zero.add(1.7105407051081795);
		T omega = zero.add(1.9674147913622104);
		T OMEGA = zero.add(FastMath.toRadians(261));
		T lv = zero;
		T mu = zero.add(3.9860047e14);
		Frame inertialFrame = FramesFactory.getEME2000();

		FieldOrbit<T> initialState = new FieldKeplerianOrbit<>(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
				inertialFrame, initDate, mu);
		FieldPropagator<T> propagator = new FieldKeplerianPropagator<>(initialState);

		//
		double dt = finalDate.durationFrom(initDate).getReal();
		double timeStep = dt / 20.0;
		List<SpacecraftState> states = new ArrayList<SpacecraftState>();

		for (double t = 0; t <= dt; t += timeStep) {
			states.add(propagator.propagate(initDate.shiftedBy(t)).toSpacecraftState());
		}

		final int interpolationPoints = 5;
		FieldEphemeris<T> ephemeris = new FieldEphemeris<>(field, states, interpolationPoints);
		Assert.assertEquals(finalDate, ephemeris.getMaxDate());

		T tolerance = ephemeris.getExtrapolationThreshold();

		ephemeris.propagate(ephemeris.getMinDate());
		ephemeris.propagate(ephemeris.getMaxDate());
		ephemeris.propagate(ephemeris.getMinDate().shiftedBy(zero.subtract(tolerance).divide(2.0)));
		ephemeris.propagate(ephemeris.getMaxDate().shiftedBy(tolerance.divide(2.0)));

		try {
			ephemeris.propagate(ephemeris.getMinDate().shiftedBy(tolerance.multiply(-2.0)));
			Assert.fail("an exception should have been thrown");
		} catch (TimeStampedCacheException ex) {
			// supposed to fail since out of bounds
		}

		try {
			ephemeris.propagate(ephemeris.getMaxDate().shiftedBy(tolerance.multiply(+2.0)));
			Assert.fail("an exception should have been thrown");
		} catch (TimeStampedCacheException ex) {
			// supposed to fail since out of bounds
		}
	}

	public <T extends RealFieldElement<T>> void doTestIssue662(Field<T> field) {
		final T zero = field.getZero();
		// Conversion from double to Field
		FieldAbsoluteDate<T> initDate = new FieldAbsoluteDate<>(field,
				new AbsoluteDate(new DateComponents(2004, 01, 01), TimeComponents.H00, TimeScalesFactory.getUTC()));
		FieldAbsoluteDate<T> finalDate = new FieldAbsoluteDate<>(field,
				new AbsoluteDate(new DateComponents(2004, 01, 02), TimeComponents.H00, TimeScalesFactory.getUTC()));
		T a = zero.add(7187990.1979844316);
		T e = zero.add(0.5e-4);
		T i = zero.add(1.7105407051081795);
		T omega = zero.add(1.9674147913622104);
		T OMEGA = zero.add(FastMath.toRadians(261));
		T lv = zero;
		T mu = zero.add(3.9860047e14);
		Frame inertialFrame = FramesFactory.getEME2000();

		FieldOrbit<T> initialState = new FieldKeplerianOrbit<>(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
				inertialFrame, initDate, mu);
		FieldPropagator<T> propagator = new FieldKeplerianPropagator<>(initialState);

		//

		// Input parameters
		int numberOfInterals = 1440;
		T deltaT = finalDate.durationFrom(initDate).divide(numberOfInterals);

		// Build the list of spacecraft states
		String additionalName = "testValue";
		T additionalValue = zero.add(1.0);
		List<SpacecraftState> states = new ArrayList<SpacecraftState>(numberOfInterals + 1);
		for (int j = 0; j <= numberOfInterals; j++) {
			states.add(propagator.propagate(initDate.shiftedBy((deltaT.multiply(j)))).addAdditionalState(additionalName,
					additionalValue).toSpacecraftState());
		}

		// Build the epemeris propagator
		FieldEphemeris<T> ephemPropagator = new FieldEphemeris<>(field, states, 2);

		// State before adding an attitude provider
		FieldSpacecraftState<T> stateBefore = ephemPropagator.propagate(ephemPropagator.getMaxDate().shiftedBy(-60.0));
		Assert.assertEquals(1, stateBefore.getAdditionalState(additionalName).length);
		Assert.assertEquals(additionalValue.getReal(), stateBefore.getAdditionalState(additionalName)[0].getReal(),
				Double.MIN_VALUE);

		// Set an attitude provider
		ephemPropagator.setAttitudeProvider(new LofOffset(inertialFrame, LOFType.VVLH));

		// State after adding an attitude provider
		FieldSpacecraftState<T> stateAfter = ephemPropagator.propagate(ephemPropagator.getMaxDate().shiftedBy(-60.0));
		Assert.assertEquals(1, stateAfter.getAdditionalState(additionalName).length);
		Assert.assertEquals(additionalValue.getReal(), stateAfter.getAdditionalState(additionalName)[0].getReal(),
				Double.MIN_VALUE);

	}

	public <T extends RealFieldElement<T>> void doTestIssue680(Field<T> field) {

		// Conversion from double to Field
		AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2004, 01, 01), TimeComponents.H00, TimeScalesFactory.getUTC());
		AbsoluteDate finalDate = new AbsoluteDate(new DateComponents(2004, 01, 02), TimeComponents.H00, TimeScalesFactory.getUTC());
		Frame inertialFrame = FramesFactory.getEME2000();

		// Initial PV coordinates
        AbsolutePVCoordinates initPV = new AbsolutePVCoordinates(inertialFrame,
                new TimeStampedPVCoordinates(initDate,
                                             new PVCoordinates(new Vector3D(-29536113.0, 30329259.0, -100125.0),
                                                               new Vector3D(-2194.0, -2141.0, -8.0))));

        // Input parameters
        int numberOfInterals = 1440;
        double deltaT = finalDate.durationFrom(initDate)/((double)numberOfInterals);

        // Build the list of spacecraft states
        List<SpacecraftState> states = new ArrayList<SpacecraftState>(numberOfInterals + 1);
        for (int j = 0; j<= numberOfInterals; j++) {
            states.add(new SpacecraftState(initPV).shiftedBy(j * deltaT));
        }

		// Build the epemeris propagator
		FieldEphemeris<T> ephemPropagator = new FieldEphemeris<>(field, states, 2);

		// Get initial state without attitude provider
		FieldSpacecraftState<T> withoutAttitudeProvider = ephemPropagator.getInitialState();
		Assert.assertEquals(0.0,
				FieldVector3D.distance(withoutAttitudeProvider.getAbsPVA().getPosition(), initPV.getPosition()).getReal(), 1.0e-10);
		Assert.assertEquals(0.0,
				FieldVector3D.distance(withoutAttitudeProvider.getAbsPVA().getVelocity(), initPV.getVelocity()).getReal(), 1.0e-10);

		// Set an attitude provider
		ephemPropagator.setAttitudeProvider(new LofOffset(inertialFrame, LOFType.VVLH));

		// Get initial state with attitude provider
		FieldSpacecraftState<T> withAttitudeProvider = ephemPropagator.getInitialState();
		Assert.assertEquals(0.0,
				FieldVector3D.distance(withAttitudeProvider.getAbsPVA().getPosition(), initPV.getPosition()).getReal(), 1.0e-10);
		Assert.assertEquals(0.0,
				FieldVector3D.distance(withAttitudeProvider.getAbsPVA().getVelocity(), initPV.getVelocity()).getReal(), 1.0e-10);

	}

}
