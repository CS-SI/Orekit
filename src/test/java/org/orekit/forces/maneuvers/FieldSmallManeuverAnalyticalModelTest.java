package org.orekit.forces.maneuvers;

import java.lang.reflect.Array;

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
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.FieldCircularOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldBoundedPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldSmallManeuverAnalyticalModel;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;

public class FieldSmallManeuverAnalyticalModelTest {

	@Test
	public void testLowEarthOrbit1() {
		doTestLowEarthOrbit1(Decimal64Field.getInstance());
	}

	@Test
	public void testLowEarthOrbit2() {
		doTestLowEarthOrbit2(Decimal64Field.getInstance());
	}

	@Test
	public void testEccentricOrbit() {
		doTestEccentricOrbit(Decimal64Field.getInstance());
	}

	@Test
	public void testJacobian() {
		doTestJacobian(Decimal64Field.getInstance());
	}

	public <T extends RealFieldElement<T>> void doTestLowEarthOrbit1(Field<T> field) {

		final T zero = field.getZero();
		FieldOrbit<T> leo = new FieldCircularOrbit<>(zero.add(7200000.0), zero.add(-1.0e-5), zero.add(2.0e-4),
				zero.add(FastMath.toRadians(98.0)), zero.add(FastMath.toRadians(123.456)), zero.add(0.0),
				PositionAngle.MEAN, FramesFactory.getEME2000(),
				new FieldAbsoluteDate<>(field, new AbsoluteDate(new DateComponents(2004, 01, 01),
						new TimeComponents(23, 30, 00.000), TimeScalesFactory.getUTC())),
				zero.add(Constants.EIGEN5C_EARTH_MU));
		T mass = zero.add(5600.0);
		FieldAbsoluteDate<T> t0 = leo.getDate().shiftedBy(1000.0);
		FieldVector3D<T> dV = new FieldVector3D<>(zero.add(-0.01), zero.add(0.02), zero.add(0.03));
		T f = zero.add(20.0);
		T isp = zero.add(315.0);
		FieldBoundedPropagator<T> withoutManeuver = getEphemeris(leo, mass, t0, FieldVector3D.getZero(field), f, isp);
		FieldBoundedPropagator<T> withManeuver = getEphemeris(leo, mass, t0, dV, f, isp);
		FieldSmallManeuverAnalyticalModel<T> model = new FieldSmallManeuverAnalyticalModel<>(field,
				withoutManeuver.propagate(t0), dV, isp);
		Assert.assertEquals(t0, model.getDate());

		for (FieldAbsoluteDate<T> t = withoutManeuver.getMinDate(); t.compareTo(withoutManeuver.getMaxDate()) < 0; t = t
				.shiftedBy(60.0)) {
			FieldPVCoordinates<T> pvWithout = withoutManeuver.getPVCoordinates(t, leo.getFrame());
			FieldPVCoordinates<T> pvWith = withManeuver.getPVCoordinates(t, leo.getFrame());
			FieldPVCoordinates<T> pvModel = model.apply(withoutManeuver.propagate(t)).getPVCoordinates(leo.getFrame());
			T nominalDeltaP = new FieldPVCoordinates<>(pvWith, pvWithout).getPosition().getNorm();
			T modelError = new FieldPVCoordinates<>(pvWith, pvModel).getPosition().getNorm();
			if (t.compareTo(t0) < 0) {
// before maneuver, all positions should be equal
				Assert.assertEquals(0, nominalDeltaP.getReal(), 1.0e-10);
				Assert.assertEquals(0, modelError.getReal(), 1.0e-10);
			} else {
// after maneuver, model error should be less than 0.8m,
// despite nominal deltaP exceeds 1 kilometer after less than 3 orbits
				if (t.durationFrom(t0).getReal() > 0.1 * leo.getKeplerianPeriod().getReal()) {
					Assert.assertTrue(modelError.getReal() < 0.009 * nominalDeltaP.getReal());
				}
				Assert.assertTrue(modelError.getReal() < 0.8);
			}
		}
	}

	public <T extends RealFieldElement<T>> void doTestLowEarthOrbit2(Field<T> field) {

		final T zero = field.getZero();
		FieldOrbit<T> leo = new FieldCircularOrbit<>(zero.add(7200000.0), zero.add(-1.0e-5), zero.add(2.0e-4),
				zero.add(FastMath.toRadians(98.0)), zero.add(FastMath.toRadians(123.456)), zero.add(0.0),
				PositionAngle.MEAN, FramesFactory.getEME2000(),
				new FieldAbsoluteDate<>(field, new AbsoluteDate(new DateComponents(2004, 01, 01),
						new TimeComponents(23, 30, 00.000), TimeScalesFactory.getUTC())),
				zero.add(Constants.EIGEN5C_EARTH_MU));
		T mass = zero.add(5600.0);
		FieldAbsoluteDate<T> t0 = leo.getDate().shiftedBy(1000.0);
		FieldVector3D<T> dV = new FieldVector3D<>(zero.add(-0.01), zero.add(0.02), zero.add(0.03));
		T f = zero.add(20.0);
		T isp = zero.add(315.0);
		FieldBoundedPropagator<T> withoutManeuver = getEphemeris(leo, mass, t0, FieldVector3D.getZero(field), f, isp);
		FieldBoundedPropagator<T> withManeuver = getEphemeris(leo, mass, t0, dV, f, isp);
		FieldSmallManeuverAnalyticalModel<T> model = new FieldSmallManeuverAnalyticalModel<>(field,
				withoutManeuver.propagate(t0), dV, isp);
		Assert.assertEquals(t0, model.getDate());

		for (FieldAbsoluteDate<T> t = withoutManeuver.getMinDate(); t.compareTo(withoutManeuver.getMaxDate()) < 0; t = t
				.shiftedBy(60.0)) {
			FieldPVCoordinates<T> pvWithout = withoutManeuver.getPVCoordinates(t, leo.getFrame());
			FieldPVCoordinates<T> pvWith = withManeuver.getPVCoordinates(t, leo.getFrame());
			FieldPVCoordinates<T> pvModel = model.apply(withoutManeuver.propagate(t)).getPVCoordinates(leo.getFrame());
			T nominalDeltaP = new FieldPVCoordinates<>(pvWith, pvWithout).getPosition().getNorm();
			T modelError = new FieldPVCoordinates<>(pvWith, pvModel).getPosition().getNorm();
			if (t.compareTo(t0) < 0) {
// before maneuver, all positions should be equal
				Assert.assertEquals(0, nominalDeltaP.getReal(), 1.0e-10);
				Assert.assertEquals(0, modelError.getReal(), 1.0e-10);
			} else {
// after maneuver, model error should be less than 0.8m,
// despite nominal deltaP exceeds 1 kilometer after less than 3 orbits
				if (t.durationFrom(t0).getReal() > 0.1 * leo.getKeplerianPeriod().getReal()) {
					Assert.assertTrue(modelError.getReal() < 0.009 * nominalDeltaP.getReal());
				}
				Assert.assertTrue(modelError.getReal() < 0.8);
			}
		}
	}

	public <T extends RealFieldElement<T>> void doTestEccentricOrbit(Field<T> field) {
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
		FieldBoundedPropagator<T> withoutManeuver = getEphemeris(heo, mass, t0, FieldVector3D.getZero(field), f, isp);
		FieldBoundedPropagator<T> withManeuver = getEphemeris(heo, mass, t0, dV, f, isp);
		FieldSpacecraftState<T> s0 = withoutManeuver.propagate(t0);
		FieldSmallManeuverAnalyticalModel<T> model = new FieldSmallManeuverAnalyticalModel<>(field, s0, dV, isp);
		Assert.assertEquals(t0, model.getDate());
		Assert.assertEquals(0.0,
				FieldVector3D.distance(dV, s0.getAttitude().getRotation().applyTo(model.getInertialDV())).getReal(),
				1.0e-15);
		Assert.assertSame(FramesFactory.getEME2000(), model.getInertialFrame());

		for (FieldAbsoluteDate<T> t = withoutManeuver.getMinDate(); t.compareTo(withoutManeuver.getMaxDate()) < 0; t = t
				.shiftedBy(600.0)) {
			FieldPVCoordinates<T> pvWithout = withoutManeuver.getPVCoordinates(t, heo.getFrame());
			FieldPVCoordinates<T> pvWith = withManeuver.getPVCoordinates(t, heo.getFrame());
			FieldPVCoordinates<T> pvModel = model.apply(withoutManeuver.propagate(t)).getPVCoordinates(heo.getFrame());
			T nominalDeltaP = new FieldPVCoordinates<>(pvWith, pvWithout).getPosition().getNorm();
			T modelError = new FieldPVCoordinates<>(pvWith, pvModel).getPosition().getNorm();
			if (t.compareTo(t0) < 0) {
// before maneuver, all positions should be equal
				Assert.assertEquals(0, nominalDeltaP.getReal(), 1.0e-10);
				Assert.assertEquals(0, modelError.getReal(), 1.0e-10);
			} else {
// after maneuver, model error should be less than 1700m,
// despite nominal deltaP exceeds 300 kilometers at perigee, after 3 orbits
				if (t.durationFrom(t0).getReal() > 0.01 * heo.getKeplerianPeriod().getReal()) {
					Assert.assertTrue(modelError.getReal() < 0.005 * nominalDeltaP.getReal());
				}
				Assert.assertTrue(modelError.getReal() < 1700);
			}
		}

	}

	public <T extends RealFieldElement<T>> void doTestJacobian(Field<T> field) {
		Frame eme2000 = FramesFactory.getEME2000();
		final T zero = field.getZero();
		final T one = field.getOne();
		FieldOrbit<T> leo = new FieldCircularOrbit<>(zero.add(7200000.0), zero.add(-1.0e-2), zero.add(2.0e-3),
				zero.add(FastMath.toRadians(98.0)), zero.add(FastMath.toRadians(123.456)), zero.add(0.3),
				PositionAngle.MEAN, FramesFactory.getEME2000(),
				new FieldAbsoluteDate<>(field, new AbsoluteDate(new DateComponents(2004, 01, 01),
						new TimeComponents(23, 30, 00.000), TimeScalesFactory.getUTC())),
				zero.add(Constants.EIGEN5C_EARTH_MU));
		T mass = zero.add(5600.0);
		FieldAbsoluteDate<T> t0 = leo.getDate().shiftedBy(1000.0);
		FieldVector3D<T> dV0 = new FieldVector3D<>(zero.add(-0.1), zero.add(0.2), zero.add(0.3));
		T f = zero.add(400.0);
		T isp = zero.add(315.0);

		for (OrbitType orbitType : OrbitType.values()) {
			for (PositionAngle positionAngle : PositionAngle.values()) {
				FieldBoundedPropagator<T> withoutManeuver = getEphemeris(orbitType.convertType(leo), mass, t0,
						FieldVector3D.getZero(field), f, isp);

				FieldSpacecraftState<T> state0 = withoutManeuver.propagate(t0);
				FieldSmallManeuverAnalyticalModel<T> model = new FieldSmallManeuverAnalyticalModel<>(field, state0,
						eme2000, dV0, isp);
				Assert.assertEquals(t0, model.getDate());

				@SuppressWarnings("unchecked")
				FieldVector3D<T>[] velDirs = (FieldVector3D<T>[]) Array.newInstance(FieldVector3D.class, 4);
				velDirs[0] = FieldVector3D.getPlusI(field);
				velDirs[1] = FieldVector3D.getPlusJ(field);
				velDirs[2] = FieldVector3D.getPlusK(field);
				velDirs[3] = FieldVector3D.getZero(field);

				T[] timeDirs = MathArrays.buildArray(field, 4);
				timeDirs[0] = zero;
				timeDirs[1] = zero;
				timeDirs[2] = zero;
				timeDirs[3] = one;
				T h = one;
				FieldAbsoluteDate<T> t1 = t0.shiftedBy(20.0);
				for (int i = 0; i < 4; ++i) {

					@SuppressWarnings("unchecked")
					FieldSmallManeuverAnalyticalModel<T>[] models = (FieldSmallManeuverAnalyticalModel<T>[]) Array
							.newInstance(FieldSmallManeuverAnalyticalModel.class, 8);
					models[0] = new FieldSmallManeuverAnalyticalModel<T>(field,
							withoutManeuver.propagate(t0.shiftedBy(zero.add(-4).multiply(h).multiply(timeDirs[i]))),
							eme2000, new FieldVector3D<T>(zero.add(1), dV0, zero.add(-4).multiply(h), velDirs[i]), isp);
					models[1] = new FieldSmallManeuverAnalyticalModel<T>(field,
							withoutManeuver.propagate(t0.shiftedBy(zero.add(-3).multiply(h).multiply(timeDirs[i]))),
							eme2000, new FieldVector3D<T>(zero.add(1), dV0, zero.add(-3).multiply(h), velDirs[i]), isp);
					models[2] = new FieldSmallManeuverAnalyticalModel<T>(field,
							withoutManeuver.propagate(t0.shiftedBy(zero.add(-2).multiply(h).multiply(timeDirs[i]))),
							eme2000, new FieldVector3D<T>(zero.add(1), dV0, zero.add(-2).multiply(h), velDirs[i]), isp);
					models[3] = new FieldSmallManeuverAnalyticalModel<T>(field,
							withoutManeuver.propagate(t0.shiftedBy(zero.add(-1).multiply(h).multiply(timeDirs[i]))),
							eme2000, new FieldVector3D<T>(zero.add(1), dV0, zero.add(-1).multiply(h), velDirs[i]), isp);
					models[4] = new FieldSmallManeuverAnalyticalModel<T>(field,
							withoutManeuver.propagate(t0.shiftedBy(zero.add(+1).multiply(h).multiply(timeDirs[i]))),
							eme2000, new FieldVector3D<T>(zero.add(1), dV0, zero.add(+1).multiply(h), velDirs[i]), isp);
					models[5] = new FieldSmallManeuverAnalyticalModel<T>(field,
							withoutManeuver.propagate(t0.shiftedBy(zero.add(+2).multiply(h).multiply(timeDirs[i]))),
							eme2000, new FieldVector3D<T>(zero.add(1), dV0, zero.add(+2).multiply(h), velDirs[i]), isp);
					models[6] = new FieldSmallManeuverAnalyticalModel<T>(field,
							withoutManeuver.propagate(t0.shiftedBy(zero.add(+3).multiply(h).multiply(timeDirs[i]))),
							eme2000, new FieldVector3D<T>(zero.add(1), dV0, zero.add(+3).multiply(h), velDirs[i]), isp);
					models[7] = new FieldSmallManeuverAnalyticalModel<T>(field,
							withoutManeuver.propagate(t0.shiftedBy(zero.add(+4).multiply(h).multiply(timeDirs[i]))),
							eme2000, new FieldVector3D<T>(zero.add(1), dV0, zero.add(+4).multiply(h), velDirs[i]), isp);

					T[][] array = MathArrays.buildArray(field, models.length, 6);

					FieldOrbit<T> orbitWithout = withoutManeuver.propagate(t1).getOrbit();

					// compute reference orbit gradient by finite differences
					T c = one.divide(h.multiply(840));
					for (int j = 0; j < models.length; ++j) {
						orbitType.mapOrbitToArray(models[j].apply(orbitWithout), positionAngle, array[j], null);
					}
					T[] orbitGradient = MathArrays.buildArray(field, 6);
					for (int k = 0; k < orbitGradient.length; ++k) {
						T d4 = array[7][k].subtract(array[0][k]);
						T d3 = array[6][k].subtract(array[1][k]);
						T d2 = array[5][k].subtract(array[2][k]);
						T d1 = array[4][k].subtract(array[3][k]);
						orbitGradient[k] = (d4.multiply(-3).add(d3.multiply(32)).subtract(d2.multiply(168))
								.add(d1.multiply(672))).multiply(c);
					}

					// analytical Jacobian to check
					T[][] jacobian = MathArrays.buildArray(field, 6, 4);
					model.getJacobian(orbitWithout, positionAngle, jacobian);

					for (int j = 0; j < orbitGradient.length; ++j) {
						//System.out.println(orbitGradient[j].getReal() + "\t" + jacobian[j][i].getReal() + "\t" + FastMath.abs(orbitGradient[j]).multiply(1.6e-4).getReal());
						Assert.assertEquals(orbitGradient[j].getReal(), jacobian[j][i].getReal(),
								FastMath.abs(orbitGradient[j]).multiply(1.7e-4).getReal());
					}

				}

			}

		}
	}

	private <T extends RealFieldElement<T>> FieldBoundedPropagator<T> getEphemeris(final FieldOrbit<T> orbit, final T mass, final FieldAbsoluteDate<T> t0,
			final FieldVector3D<T> dV, final T f, final T isp) {

		AttitudeProvider law = new LofOffset(orbit.getFrame(), LOFType.LVLH);
		final FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(orbit,
				law.getAttitude(orbit, orbit.getDate(), orbit.getFrame()), mass);

// set up numerical propagator
		final T zero = mass.getField().getZero();
		final T dP = zero.add(1);
		double[][] tolerances = FieldNumericalPropagator.tolerances(dP, orbit, orbit.getType());
		AdaptiveStepsizeFieldIntegrator<T> integrator = new DormandPrince853FieldIntegrator<>(mass.getField(), 0.001, 1000.0, tolerances[0],
				tolerances[1]);
		integrator.setInitialStepSize(orbit.getKeplerianPeriod().divide(100.0));
		final FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(mass.getField(), integrator);
		propagator.setOrbitType(orbit.getType());
		propagator.setInitialState(initialState);
		propagator.setAttitudeProvider(law);

		if (dV.getNorm().getReal() > 1.0e-6) {
// set up maneuver
			final T vExhaust = isp.multiply(Constants.G0_STANDARD_GRAVITY);
			final T dt = zero.subtract(mass.multiply(vExhaust).divide(f)).multiply(FastMath.expm1( zero.subtract(dV.getNorm()).divide(vExhaust)));
			final ConstantThrustManeuver maneuver = new ConstantThrustManeuver(t0.toAbsoluteDate(), dt.getReal(), f.getReal(), isp.getReal(), dV.normalize().toVector3D());
			propagator.addForceModel(maneuver);
		}

		propagator.setEphemerisMode();
		propagator.propagate(t0.shiftedBy(orbit.getKeplerianPeriod().multiply(5)));
		return propagator.getGeneratedEphemeris();

	}

	@Before
	public void setUp() {
		Utils.setDataRoot("regular-data");
	}

}
