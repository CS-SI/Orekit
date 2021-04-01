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

import java.io.IOException;
import java.text.ParseException;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.maneuvers.ConstantThrustManeuver;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.FieldCircularOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldAdditionalStateProvider;
import org.orekit.propagation.FieldBoundedPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;

public class FieldAdapterPropagatorTest {

	@Test
	public void testLowEarthOrbit() throws ParseException, IOException{
		doTestLowEarthOrbit(Decimal64Field.getInstance());
	}

	@Test
	public void testEccentricOrbit() throws ParseException, IOException {
		doTestEccentricOrbit(Decimal64Field.getInstance());
	}

	@Test
	public void testNonKeplerian() throws ParseException, IOException {
		doTestNonKeplerian(Decimal64Field.getInstance());
	}

	public <T extends RealFieldElement<T>> void doTestLowEarthOrbit(Field<T> field) throws ParseException, IOException {
		final T zero = field.getZero();
		FieldOrbit<T> leo = new FieldCircularOrbit<T>(zero.add(7200000.0), zero.add(-1.0e-5), zero.add(2.0e-4),
				zero.add(FastMath.toRadians(98.0)), zero.add(FastMath.toRadians(123.456)), zero.add(0.0),
				PositionAngle.MEAN, FramesFactory.getEME2000(),
				new FieldAbsoluteDate<T>(field, new AbsoluteDate(new DateComponents(2004, 01, 01),
						new TimeComponents(23, 30, 00.000), TimeScalesFactory.getUTC())),
				zero.add(Constants.EIGEN5C_EARTH_MU));
		T mass = zero.add(5600.0);
		FieldAbsoluteDate<T> t0 = leo.getDate().shiftedBy(1000.0);
		FieldVector3D<T> dV = new FieldVector3D<>(zero.add(-0.1), zero.add(0.2), zero.add(0.3));
		T f = zero.add(20.0);
		T isp = zero.add(315.0);
		T vExhaust = zero.add(Constants.G0_STANDARD_GRAVITY).multiply(isp);
		T dt = zero.subtract(mass.multiply(vExhaust).divide(f))
				.multiply(FastMath.expm1(zero.subtract(dV.getNorm()).divide(vExhaust)));
		FieldBoundedPropagator<T> withoutManeuver = getEphemeris(leo, mass, 5,
				new LofOffset(leo.getFrame(), LOFType.LVLH), t0, FieldVector3D.getZero(field), f, isp, false, false, null);
		FieldBoundedPropagator<T> withManeuver = getEphemeris(leo, mass, 5, new LofOffset(leo.getFrame(), LOFType.LVLH),
				t0, dV, f, isp, false, false, null);

// we set up a model that reverts the maneuvers
		FieldAdapterPropagator<T> adapterPropagator = new FieldAdapterPropagator<T>(field, withManeuver);
		FieldAdapterPropagator.FieldDifferentialEffect<T> effect = new FieldSmallManeuverAnalyticalModel<>(field,
				adapterPropagator.propagate(t0), dV.negate(), isp);
		adapterPropagator.addEffect(effect);
		adapterPropagator.addAdditionalStateProvider(new FieldAdditionalStateProvider<T>() {
			public String getName() {
				return "dummy 3";
			}

			public T[] getAdditionalState(FieldSpacecraftState<T> state) {
				return MathArrays.buildArray(state.getDate().getField(), 3);  //new double[3];
			}
		});

// the adapted propagators do not manage the additional states from the reference,
// they simply forward them
		Assert.assertFalse(adapterPropagator.isAdditionalStateManaged("dummy 1"));
		Assert.assertFalse(adapterPropagator.isAdditionalStateManaged("dummy 2"));
		Assert.assertTrue(adapterPropagator.isAdditionalStateManaged("dummy 3"));

		for (FieldAbsoluteDate<T> t = t0.shiftedBy(zero.add(0.5).multiply(dt)); t
				.compareTo(withoutManeuver.getMaxDate()) < 0; t = t.shiftedBy(60.0)) {
			FieldPVCoordinates<T> pvWithout = withoutManeuver.getPVCoordinates(t, leo.getFrame());
			FieldPVCoordinates<T> pvReverted = adapterPropagator.getPVCoordinates(t, leo.getFrame());
			T revertError = new FieldPVCoordinates<T>(pvWithout, pvReverted).getPosition().getNorm();
			Assert.assertEquals(0, revertError.getReal(), 0.45);
			Assert.assertEquals(2, adapterPropagator.propagate(t).getAdditionalState("dummy 1").length);
			Assert.assertEquals(1, adapterPropagator.propagate(t).getAdditionalState("dummy 2").length);
			Assert.assertEquals(3, adapterPropagator.propagate(t).getAdditionalState("dummy 3").length);
		}
	}

	public <T extends RealFieldElement<T>> void doTestEccentricOrbit(Field<T> field) throws ParseException, IOException {
		final T zero = field.getZero();
		FieldOrbit<T> heo = new FieldKeplerianOrbit<>(zero.add(90000000.0), zero.add(0.92),
				zero.add(FastMath.toRadians(98.0)), zero.add(FastMath.toRadians(12.3456)),
				zero.add(FastMath.toRadians(123.456)), zero.add(FastMath.toRadians(1.23456)), PositionAngle.MEAN,
				FramesFactory.getEME2000(),
				new FieldAbsoluteDate<>(field, new AbsoluteDate(new DateComponents(2004, 01, 01),
						new TimeComponents(23, 30, 00.000), TimeScalesFactory.getUTC())),
				zero.add(Constants.EIGEN5C_EARTH_MU));
		T mass = zero.add(5600.0);
		FieldAbsoluteDate<T> t0 = heo.getDate().shiftedBy(1000.0);
		FieldVector3D<T> dV = new FieldVector3D<>(zero.add(-0.01), zero.add(0.02), zero.add(0.03));
		T f = zero.add(20.0);
		T isp = zero.add(315.0);
		T vExhaust = zero.add(Constants.G0_STANDARD_GRAVITY).multiply(isp);
		T dt = zero.subtract(mass.multiply(vExhaust).divide(f))
				.multiply(FastMath.expm1(zero.subtract(dV.getNorm()).divide(vExhaust)));
		FieldBoundedPropagator<T> withoutManeuver = getEphemeris(heo, mass, 5,
				new LofOffset(heo.getFrame(), LOFType.LVLH), t0, FieldVector3D.getZero(field), f, isp, false, false, null);
		FieldBoundedPropagator<T> withManeuver = getEphemeris(heo, mass, 5, new LofOffset(heo.getFrame(), LOFType.LVLH),
				t0, dV, f, isp, false, false, null);

// we set up a model that reverts the maneuvers
		FieldAdapterPropagator<T> adapterPropagator = new FieldAdapterPropagator<T>(field, withManeuver);
		FieldAdapterPropagator.FieldDifferentialEffect<T> effect = new FieldSmallManeuverAnalyticalModel<>(field,
				adapterPropagator.propagate(t0), dV.negate(), isp);
		adapterPropagator.addEffect(effect);
		adapterPropagator.addAdditionalStateProvider(new FieldAdditionalStateProvider<T>() {
			public String getName() {
				return "dummy 3";
			}

			public T[] getAdditionalState(FieldSpacecraftState<T> state) {
				return MathArrays.buildArray(state.getDate().getField(), 3);  //new double[3];
			}

		});

// the adapted propagators do not manage the additional states from the reference,
// they simply forward them
		Assert.assertFalse(adapterPropagator.isAdditionalStateManaged("dummy 1"));
		Assert.assertFalse(adapterPropagator.isAdditionalStateManaged("dummy 2"));
		Assert.assertTrue(adapterPropagator.isAdditionalStateManaged("dummy 3"));

		for (FieldAbsoluteDate<T> t = t0.shiftedBy(dt.multiply(0.5)); t
				.compareTo(withoutManeuver.getMaxDate()) < 0; t = t.shiftedBy(300.0)) {
			FieldPVCoordinates<T> pvWithout = withoutManeuver.getPVCoordinates(t, heo.getFrame());
			FieldPVCoordinates<T> pvReverted = adapterPropagator.getPVCoordinates(t, heo.getFrame());
			T revertError = FieldVector3D.distance(pvWithout.getPosition(), pvReverted.getPosition());
			Assert.assertEquals(0, revertError.getReal(), 2.5e-5 * heo.getA().getReal());
			Assert.assertEquals(2, adapterPropagator.propagate(t).getAdditionalState("dummy 1").length);
			Assert.assertEquals(1, adapterPropagator.propagate(t).getAdditionalState("dummy 2").length);
			Assert.assertEquals(3, adapterPropagator.propagate(t).getAdditionalState("dummy 3").length);
		}

	}

	public <T extends RealFieldElement<T>> void doTestNonKeplerian(Field<T> field) throws ParseException, IOException {
		final T zero = field.getZero();
		FieldOrbit<T> leo = new FieldCircularOrbit<>(zero.add(7204319.233600575), zero.add(4.434564637450575E-4),
				zero.add(0.0011736728299091088), zero.add(1.7211611441767323), zero.add(5.5552084166959474),
				zero.add(24950.321259193086), PositionAngle.TRUE, FramesFactory.getEME2000(),
				new FieldAbsoluteDate<>(field, new AbsoluteDate(new DateComponents(2003, 9, 16),
						new TimeComponents(23, 11, 20.264), TimeScalesFactory.getUTC())),
				zero.add(Constants.EIGEN5C_EARTH_MU));
		T mass = zero.add(4093.0);
		FieldAbsoluteDate<T> t0 = new FieldAbsoluteDate<>(field, new AbsoluteDate(new DateComponents(2003, 9, 16),
				new TimeComponents(23, 14, 40.264), TimeScalesFactory.getUTC()));
		FieldVector3D<T> dV = new FieldVector3D<>(zero.add(0.0), zero.add(3.0), zero.add(0.0));
		T f = zero.add(40.0);
		T isp = zero.add(300.0);
		T vExhaust = zero.add(Constants.G0_STANDARD_GRAVITY).multiply(isp);
		T dt = zero.subtract(mass.multiply(vExhaust).divide(f))
				.multiply(FastMath.expm1(zero.subtract(dV.getNorm()).divide(vExhaust)));
// setup a specific coefficient file for gravity potential as it will also
// try to read a corrupted one otherwise
		GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("g007_eigen_05c_coef", false));
		NormalizedSphericalHarmonicsProvider gravityField = GravityFieldFactory.getNormalizedProvider(8, 8);
		FieldBoundedPropagator<T> withoutManeuver = getEphemeris(leo, mass, 10,
				new LofOffset(leo.getFrame(), LOFType.VNC), t0, FieldVector3D.getZero(field), f, isp, true, true, gravityField);
		FieldBoundedPropagator<T> withManeuver = getEphemeris(leo, mass, 10, new LofOffset(leo.getFrame(), LOFType.VNC),
				t0, dV, f, isp, true, true, gravityField);

// we set up a model that reverts the maneuvers
		FieldAdapterPropagator<T> adapterPropagator = new FieldAdapterPropagator<>(field, withManeuver);
		FieldSpacecraftState<T> state0 = adapterPropagator.propagate(t0);
		FieldAdapterPropagator.FieldDifferentialEffect<T> directEffect = new FieldSmallManeuverAnalyticalModel<>(field,
				state0, dV.negate(), isp);
		FieldAdapterPropagator.FieldDifferentialEffect<T> derivedEffect = new FieldJ2DifferentialEffect<>(field, state0,
				directEffect, false, GravityFieldFactory.getUnnormalizedProvider(gravityField));
		adapterPropagator.addEffect(directEffect);
		adapterPropagator.addEffect(derivedEffect);
		adapterPropagator.addAdditionalStateProvider(new FieldAdditionalStateProvider<T>() {
			public String getName() {
				return "dummy 3";
			}

			public T[] getAdditionalState(FieldSpacecraftState<T> state) {
				return MathArrays.buildArray(field, 3);
			}
		});

// the adapted propagators do not manage the additional states from the reference,
// they simply forward them
		Assert.assertFalse(adapterPropagator.isAdditionalStateManaged("dummy 1"));
		Assert.assertFalse(adapterPropagator.isAdditionalStateManaged("dummy 2"));
		Assert.assertTrue(adapterPropagator.isAdditionalStateManaged("dummy 3"));

		T maxDelta = zero.add(0);
		T maxNominal = zero.add(0);
		for (FieldAbsoluteDate<T> t = t0.shiftedBy(dt.multiply(0.5)); t
				.compareTo(withoutManeuver.getMaxDate()) < 0; t = t.shiftedBy(600.0)) {
			FieldPVCoordinates<T> pvWithout = withoutManeuver.getPVCoordinates(t, leo.getFrame());
			FieldPVCoordinates<T> pvWith = withManeuver.getPVCoordinates(t, leo.getFrame());
			FieldPVCoordinates<T> pvReverted = adapterPropagator.getPVCoordinates(t, leo.getFrame());
			T nominal = new FieldPVCoordinates<>(pvWithout, pvWith).getPosition().getNorm();
			T revertError = new FieldPVCoordinates<>(pvWithout, pvReverted).getPosition().getNorm();
			maxDelta = FastMath.max(maxDelta, revertError);
			maxNominal = FastMath.max(maxNominal, nominal);
			Assert.assertEquals(2, adapterPropagator.propagate(t).getAdditionalState("dummy 1").length);
			Assert.assertEquals(1, adapterPropagator.propagate(t).getAdditionalState("dummy 2").length);
			Assert.assertEquals(3, adapterPropagator.propagate(t).getAdditionalState("dummy 3").length);
		}
		Assert.assertTrue(maxDelta.getReal() < 120);
		Assert.assertTrue(maxNominal.getReal() > 2800);
	}

	private <T extends RealFieldElement<T>> FieldBoundedPropagator<T> getEphemeris(final FieldOrbit<T> leo, final T mass, final int nbOrbits,
			final AttitudeProvider law, final FieldAbsoluteDate<T> t0, final FieldVector3D<T> zero2, final T f, final T isp,
			final boolean sunAttraction, final boolean moonAttraction,
			final NormalizedSphericalHarmonicsProvider gravityField) throws ParseException, IOException {

		FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(leo,
				law.getAttitude(leo, leo.getDate(), leo.getFrame()), mass);

// add some dummy additional states
		final T zero = mass.getField().getZero();
		initialState = initialState.addAdditionalState("dummy 1", zero.add(1.25), zero.add(2.5));
		initialState = initialState.addAdditionalState("dummy 2", zero.add(5.0));

// set up numerical propagator
		final T dP = zero.add(1);
		double[][] tolerances = FieldNumericalPropagator.tolerances(dP, leo, leo.getType());
		AdaptiveStepsizeFieldIntegrator<T> integrator = new DormandPrince853FieldIntegrator<>(mass.getField(), 0.001, 1000.0, tolerances[0],
				tolerances[1]);
		integrator.setInitialStepSize(leo.getKeplerianPeriod().divide(100.0));
		final FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(mass.getField(), integrator);
		propagator.addAdditionalStateProvider(new FieldAdditionalStateProvider<T>() {
			public String getName() {
				return "dummy 2";
			}

			public T[] getAdditionalState(FieldSpacecraftState<T> state) {
				T[] tab = MathArrays.buildArray(state.getDate().getField(), 1);
				tab[0] = zero.add(5.0);
				return tab;
			}

			
		});
		propagator.setInitialState(initialState);
		propagator.setAttitudeProvider(law);

		if (zero2.getNorm().getReal() > 1.0e-6) {
// set up maneuver
			final T vExhaust = isp.multiply(Constants.G0_STANDARD_GRAVITY);
			final T dt = zero.subtract(mass.multiply(vExhaust).divide(f)).multiply(FastMath.expm1( zero.subtract(zero2.getNorm()).divide(vExhaust)));
			final ConstantThrustManeuver maneuver = new ConstantThrustManeuver(t0.shiftedBy(dt.multiply(-0.5)).toAbsoluteDate(), dt.getReal(), f.getReal(), isp.getReal(),
					zero2.normalize().toVector3D());
			propagator.addForceModel(maneuver);
		}

		if (sunAttraction) {
			propagator.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getSun()));
		}

		if (moonAttraction) {
			propagator.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getMoon()));
		}

		if (gravityField != null) {
			propagator.addForceModel(new HolmesFeatherstoneAttractionModel(FramesFactory.getGTOD(false), gravityField));
		}

		propagator.setEphemerisMode();
		propagator.propagate(t0.shiftedBy(leo.getKeplerianPeriod().multiply(nbOrbits)));

		final FieldBoundedPropagator<T> ephemeris = propagator.getGeneratedEphemeris();

// both the initial propagator and generated ephemeris manage one of the two
// additional states, but they also contain unmanaged copies of the other one
		Assert.assertFalse(propagator.isAdditionalStateManaged("dummy 1"));
		Assert.assertTrue(propagator.isAdditionalStateManaged("dummy 2"));
		Assert.assertFalse(ephemeris.isAdditionalStateManaged("dummy 1"));
		Assert.assertTrue(ephemeris.isAdditionalStateManaged("dummy 2"));
		Assert.assertEquals(2, ephemeris.getInitialState().getAdditionalState("dummy 1").length);
		Assert.assertEquals(1, ephemeris.getInitialState().getAdditionalState("dummy 2").length);

		return ephemeris;

	}

	@Before
	public void setUp() {
		Utils.setDataRoot("regular-data:potential/icgem-format");
	}
}
