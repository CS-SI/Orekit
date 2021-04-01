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
package org.orekit.propagation.analytical.gnss;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

public class FieldGalileoPropagatorTest {

	@Test
	public void testGalileoCycle() {
		doTestGalileoCycle(Decimal64Field.getInstance());
	}

	@Test
	public void testFrames() {
		doTestFrames(Decimal64Field.getInstance());
	}

	@Test
	public void testNoReset() {
		doTestNoReset(Decimal64Field.getInstance());
	}

	@Test
	public void testDerivativesConsistency() {
		doTestDerivativesConsistency(Decimal64Field.getInstance());
	}

	@Test
	public void testPosition() {
		doTestPosition(Decimal64Field.getInstance());
	}

	@Test
	public void testIssue544() {
		doTestIssue544(Decimal64Field.getInstance());
	}

	@BeforeClass
	public static void setUpBeforeClass() {
		Utils.setDataRoot("gnss");
	}

	public <T extends RealFieldElement<T>> void doTestGalileoCycle(Field<T> field) {
		final T zero = field.getZero();
		// Reference for the almanac: 2019-05-28T09:40:01.0Z
		final FieldGalileoAlmanac<T> almanac = new FieldGalileoAlmanac<>(1, 1024, zero.add(293400.0),
				zero.add(0.013671875), zero.add(0.000152587890625), zero.add(0.003356933593), 4,
				zero.add(0.2739257812499857891), zero.add(-1.74622982740407E-9), zero.add(0.7363586425),
				zero.add(0.27276611328124), zero.add(-0.0006141662597), zero.add(-7.275957614183E-12), 0, 0, 0);
		// Intermediate verification
		Assert.assertEquals(1, almanac.getPRN());
		Assert.assertEquals(1024, almanac.getWeek());
		Assert.assertEquals(4, almanac.getIOD());
		Assert.assertEquals(0, almanac.getHealthE1());
		Assert.assertEquals(0, almanac.getHealthE5a());
		Assert.assertEquals(0, almanac.getHealthE5b());
		Assert.assertEquals(-0.0006141662597, almanac.getAf0().getReal(), 1.0e-15);
		Assert.assertEquals(-7.275957614183E-12, almanac.getAf1().getReal(), 1.0e-15);

		// Builds the GalileoPropagator from the almanac
		FieldGalileoPropagator<T> propagator = new FieldGalileoPropagator<>(field, almanac);
		// Propagate at the Galileo date and one Galileo cycle later
		final FieldAbsoluteDate<T> date0 = almanac.getDate();
		final FieldVector3D<T> p0 = propagator.propagateInEcef(date0).getPosition();
		final double galCycleDuration = FieldGalileoOrbitalElements.GALILEO_WEEK_IN_SECONDS
				* FieldGalileoOrbitalElements.GALILEO_WEEK_NB;
		final FieldAbsoluteDate<T> date1 = date0.shiftedBy(galCycleDuration);
		final FieldVector3D<T> p1 = propagator.propagateInEcef(date1).getPosition();

		// Checks
		Assert.assertEquals(0., p0.distance(p1).getReal(), 0.);
	}

	public <T extends RealFieldElement<T>> void doTestFrames(Field<T> field) {
		final T zero = field.getZero();
		FieldGalileoOrbitalElements<T> goe = new FieldGalileoEphemeris<T>(4, 1024, zero.add(293400.0),
				zero.add(5440.602949142456), zero.add(3.7394414770330066E-9), zero.add(2.4088891223073006E-4),
				zero.add(0.9531656087278083), zero.add(-2.36081262303612E-10), zero.add(-0.36639513583951266),
				zero.add(-5.7695260382035525E-9), zero.add(-1.6870064194345724), zero.add(-0.38716557650888),
				zero.add(-8.903443813323975E-7), zero.add(6.61797821521759E-6), zero.add(194.0625), zero.add(-18.78125),
				zero.add(3.166496753692627E-8), zero.add(-1.862645149230957E-8));
		// Builds the GalileoPropagator from the ephemeris
		FieldGalileoPropagator<T> propagator = new FieldGalileoPropagator<>(field, goe);
		Assert.assertEquals("EME2000", propagator.getFrame().getName());
		Assert.assertEquals(3.986004418e+14, FieldGalileoOrbitalElements.GALILEO_MU, 1.0e6);
		// Defines some date
		final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2016, 3, 3, 12, 0, 0.,
				TimeScalesFactory.getUTC());
		// Get PVCoordinates at the date in the ECEF
		final FieldPVCoordinates<T> pv0 = propagator.propagateInEcef(date);
		// Get PVCoordinates at the date in the ECEF
		final FieldPVCoordinates<T> pv1 = propagator.getPVCoordinates(date, propagator.getECEF());

		// Checks
		Assert.assertEquals(0., pv0.getPosition().distance(pv1.getPosition()).getReal(), 2.4e-8);
		Assert.assertEquals(0., pv0.getVelocity().distance(pv1.getVelocity()).getReal(), 2.8e-12);
	}

	public <T extends RealFieldElement<T>> void doTestNoReset(Field<T> field) {
		try {
			final T zero = field.getZero();
			FieldGalileoOrbitalElements<T> goe = new FieldGalileoEphemeris<T>(4, 1024, zero.add(293400.0),
					zero.add(5440.602949142456), zero.add(3.7394414770330066E-9), zero.add(2.4088891223073006E-4),
					zero.add(0.9531656087278083), zero.add(-2.36081262303612E-10), zero.add(-0.36639513583951266),
					zero.add(-5.7695260382035525E-9), zero.add(-1.6870064194345724), zero.add(-0.38716557650888),
					zero.add(-8.903443813323975E-7), zero.add(6.61797821521759E-6), zero.add(194.0625), zero.add(-18.78125),
					zero.add(3.166496753692627E-8), zero.add(-1.862645149230957E-8));
			FieldGalileoPropagator<T> propagator = new FieldGalileoPropagator<>(field, goe);
			propagator.resetInitialState(propagator.getInitialState());
			Assert.fail("an exception should have been thrown");
		} catch (OrekitException oe) {
			Assert.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
		}
		try {
			final T zero = field.getZero();
			FieldGalileoOrbitalElements<T> goe = new FieldGalileoEphemeris<T>(4, 1024, zero.add(293400.0),
					zero.add(5440.602949142456), zero.add(3.7394414770330066E-9), zero.add(2.4088891223073006E-4),
					zero.add(0.9531656087278083), zero.add(-2.36081262303612E-10), zero.add(-0.36639513583951266),
					zero.add(-5.7695260382035525E-9), zero.add(-1.6870064194345724), zero.add(-0.38716557650888),
					zero.add(-8.903443813323975E-7), zero.add(6.61797821521759E-6), zero.add(194.0625), zero.add(-18.78125),
					zero.add(3.166496753692627E-8), zero.add(-1.862645149230957E-8));
			FieldGalileoPropagator<T> propagator = new FieldGalileoPropagator<>(field, goe);
			propagator.resetIntermediateState(propagator.getInitialState(), true);
			Assert.fail("an exception should have been thrown");
		} catch (OrekitException oe) {
			Assert.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
		}
	}

	public <T extends RealFieldElement<T>> void doTestDerivativesConsistency(Field<T> field) {
		final Frame eme2000 = FramesFactory.getEME2000();
		double errorP = 0;
		double errorV = 0;
		double errorA = 0;
		final T zero = field.getZero();
		FieldGalileoOrbitalElements<T> goe = new FieldGalileoEphemeris<T>(4, 1024, zero.add(293400.0),
				zero.add(5440.602949142456), zero.add(3.7394414770330066E-9), zero.add(2.4088891223073006E-4),
				zero.add(0.9531656087278083), zero.add(-2.36081262303612E-10), zero.add(-0.36639513583951266),
				zero.add(-5.7695260382035525E-9), zero.add(-1.6870064194345724), zero.add(-0.38716557650888),
				zero.add(-8.903443813323975E-7), zero.add(6.61797821521759E-6), zero.add(194.0625), zero.add(-18.78125),
				zero.add(3.166496753692627E-8), zero.add(-1.862645149230957E-8));
		FieldGalileoPropagator<T> propagator = new FieldGalileoPropagator<>(field, goe);
		FieldGalileoOrbitalElements<T> elements = propagator.getFieldGalileoOrbitalElements();
		FieldAbsoluteDate<T> t0 = new FieldGNSSDate<>(elements.getWeek(), elements.getTime().multiply(0.001),
				SatelliteSystem.GALILEO).getDate();
		for (double dt = 0; dt < Constants.JULIAN_DAY; dt += 600) {
			final FieldAbsoluteDate<T> central = t0.shiftedBy(dt);
			final FieldPVCoordinates<T> pv = propagator.getPVCoordinates(central, eme2000);
			final double h = 10.0;
			List<TimeStampedFieldPVCoordinates<T>> sample = new ArrayList<TimeStampedFieldPVCoordinates<T>>();
			for (int i = -3; i <= 3; ++i) {
				sample.add(propagator.getPVCoordinates(central.shiftedBy(i * h), eme2000));
			}
			final FieldPVCoordinates<T> interpolated = TimeStampedFieldPVCoordinates.interpolate(central,
					CartesianDerivativesFilter.USE_P, sample);
			errorP = FastMath.max(errorP,
					FieldVector3D.distance(pv.getPosition(), interpolated.getPosition()).getReal());
			errorV = FastMath.max(errorV,
					FieldVector3D.distance(pv.getVelocity(), interpolated.getVelocity()).getReal());
			errorA = FastMath.max(errorA,
					FieldVector3D.distance(pv.getAcceleration(), interpolated.getAcceleration()).getReal());
		}
		Assert.assertEquals(0.0, errorP, 1.5e-11);
		Assert.assertEquals(0.0, errorV, 2.2e-7);
		Assert.assertEquals(0.0, errorA, 4.9e-8);

	}

	public <T extends RealFieldElement<T>> void doTestPosition(Field<T> field) {
		final T zero = field.getZero();
		FieldGalileoOrbitalElements<T> goe = new FieldGalileoEphemeris<T>(4, 1024, zero.add(293400.0),
				zero.add(5440.602949142456), zero.add(3.7394414770330066E-9), zero.add(2.4088891223073006E-4),
				zero.add(0.9531656087278083), zero.add(-2.36081262303612E-10), zero.add(-0.36639513583951266),
				zero.add(-5.7695260382035525E-9), zero.add(-1.6870064194345724), zero.add(-0.38716557650888),
				zero.add(-8.903443813323975E-7), zero.add(6.61797821521759E-6), zero.add(194.0625), zero.add(-18.78125),
				zero.add(3.166496753692627E-8), zero.add(-1.862645149230957E-8));
		// Date of the Galileo orbital elements, 10 April 2019 at 09:30:00 UTC
		final FieldAbsoluteDate<T> target = goe.getDate();
		// Build the Galileo propagator
		FieldGalileoPropagator<T> propagator = new FieldGalileoPropagator<>(field, goe);
		// Compute the PV coordinates at the date of the Galileo orbital elements
		final FieldPVCoordinates<T> pv = propagator.getPVCoordinates(target,
				FramesFactory.getITRF(IERSConventions.IERS_2010, true));
		// Computed position
		final FieldVector3D<T> computedPos = pv.getPosition();
		// Expected position (reference from IGS file
		// WUM0MGXULA_20191010500_01D_15M_ORB.sp3)
		final FieldVector3D<T> expectedPos = new FieldVector3D<>(zero.add(10487480.721), zero.add(17867448.753),
				zero.add(-21131462.002));
		Assert.assertEquals(0., FieldVector3D.distance(expectedPos, computedPos).getReal(), 2.1);
	}

	public <T extends RealFieldElement<T>> void doTestIssue544(Field<T> field) {
		final T zero = field.getZero();
		FieldGalileoOrbitalElements<T> goe = new FieldGalileoEphemeris<T>(4, 1024, zero.add(293400.0),
				zero.add(5440.602949142456), zero.add(3.7394414770330066E-9), zero.add(2.4088891223073006E-4),
				zero.add(0.9531656087278083), zero.add(-2.36081262303612E-10), zero.add(-0.36639513583951266),
				zero.add(-5.7695260382035525E-9), zero.add(-1.6870064194345724), zero.add(-0.38716557650888),
				zero.add(-8.903443813323975E-7), zero.add(6.61797821521759E-6), zero.add(194.0625), zero.add(-18.78125),
				zero.add(3.166496753692627E-8), zero.add(-1.862645149230957E-8));
		// Builds the GalileoPropagator from the almanac
		FieldGalileoPropagator<T> propagator = new FieldGalileoPropagator<>(field, goe);
		// In order to test the issue, we volontary set a Double.NaN value in the date.
		final FieldAbsoluteDate<T> date0 = new FieldAbsoluteDate<>(field, 2010, 5, 7, 7, 50, Double.NaN,
				TimeScalesFactory.getUTC());
		final FieldPVCoordinates<T> pv0 = propagator.propagateInEcef(date0);
		// Verify that an infinite loop did not occur
		Assert.assertEquals(FieldVector3D.getNaN(field), pv0.getPosition());
		Assert.assertEquals(FieldVector3D.getNaN(field), pv0.getVelocity());
	}

	private class FieldGalileoEphemeris<T extends RealFieldElement<T>> implements FieldGalileoOrbitalElements<T> {

		private int satID;
		private int week;
		private T toe;
		private T sma;
		private T deltaN;
		private T ecc;
		private T inc;
		private T iDot;
		private T om0;
		private T dom;
		private T aop;
		private T anom;
		private T cuc;
		private T cus;
		private T crc;
		private T crs;
		private T cic;
		private T cis;

		/**
		 * Build a new instance.
		 */
		public FieldGalileoEphemeris(int satID, int week, T toe, T sqa, T deltaN, T ecc, T inc, T iDot, T om0, T dom,
				T aop, T anom, T cuc, T cus, T crc, T crs, T cic, T cis) {
			this.satID = satID;
			this.week = week;
			this.toe = toe;
			this.sma = sqa.multiply(sqa);
			this.deltaN = deltaN;
			this.ecc = ecc;
			this.inc = inc;
			this.iDot = iDot;
			this.om0 = om0;
			this.dom = dom;
			this.aop = aop;
			this.anom = anom;
			this.cuc = cuc;
			this.cus = cus;
			this.crc = crc;
			this.crs = crs;
			this.cic = cic;
			this.cis = cis;
		}

		@Override
		public int getPRN() {
			return satID;
		}

		@Override
		public int getWeek() {
			return week;
		}

		@Override
		public T getTime() {
			return toe;
		}

		@Override
		public T getSma() {
			return sma;
		}

		@Override
		public T getMeanMotion() {
			final T absA = FastMath.abs(sma);
			return FastMath.sqrt(absA.getField().getZero().add(GALILEO_MU).divide(absA)).divide(absA).add(deltaN);
		}

		@Override
		public T getE() {
			return ecc;
		}

		@Override
		public T getI0() {
			return inc;
		}

		@Override
		public T getIDot() {
			return iDot;
		}

		@Override
		public T getOmega0() {
			return om0;
		}

		@Override
		public T getOmegaDot() {
			return dom;
		}

		@Override
		public T getPa() {
			return aop;
		}

		@Override
		public T getM0() {
			return anom;
		}

		@Override
		public T getCuc() {
			return cuc;
		}

		@Override
		public T getCus() {
			return cus;
		}

		@Override
		public T getCrc() {
			return crc;
		}

		@Override
		public T getCrs() {
			return crs;
		}

		@Override
		public T getCic() {
			return cic;
		}

		@Override
		public T getCis() {
			return cis;
		}

		@Override
		public FieldAbsoluteDate<T> getDate() {
			return new FieldGNSSDate<>(week, toe.multiply(1000), SatelliteSystem.GALILEO).getDate();
		}

		@Override
		public T getAf0() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public T getAf1() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public T getAf2() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public T getToc() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getIODNav() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public T getBGD() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public T getBGDE1E5a() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public T getBGDE5bE1() {
			// TODO Auto-generated method stub
			return null;
		}

	}

}
