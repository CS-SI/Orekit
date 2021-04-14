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
import org.orekit.gnss.GPSAlmanac;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

public class FieldGPSPropagatorTest {

	private static GPSAlmanac almanac;

	@BeforeClass
	public static void setUpBeforeClass() {
		Utils.setDataRoot("gnss");

		almanac = new GPSAlmanac("SEM", 1, 63, 862, 319488, 5.15360253906250E+03, 5.10072708129883E-03,
				6.84547424316406E-03, -2.08778738975525E-01, -2.48837750405073E-09, 1.46086812019348E-01,
				4.55284833908081E-01, 1.33514404296875E-05, 0.00000000000000E+00, 0, 0, 11);
	}

	@Test
	public void testGPSCycle() {
		doTestGPSCycle(Decimal64Field.getInstance());
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

	public <T extends RealFieldElement<T>> void doTestGPSCycle(Field<T> field) {
		// Builds the GPSPropagator from the almanac
		FieldGPSAlmanac<T> almanac = new FieldGPSAlmanac<T>(field, FieldGPSPropagatorTest.almanac);
		final FieldGPSPropagator<T> propagator = new FieldGPSPropagator<>(field, almanac);
		// Propagate at the GPS date and one GPS cycle later
		final FieldAbsoluteDate<T> date0 = almanac.getDate();
		final FieldVector3D<T> p0 = propagator.propagateInEcef(date0).getPosition();
		final double gpsCycleDuration = GPSOrbitalElements.GPS_WEEK_IN_SECONDS * GPSOrbitalElements.GPS_WEEK_NB;
		final FieldAbsoluteDate<T> date1 = date0.shiftedBy(gpsCycleDuration);
		final FieldVector3D<T> p1 = propagator.propagateInEcef(date1).getPosition();

		// Checks
		Assert.assertEquals(0., p0.distance(p1).getReal(), 0.);
	}

	public <T extends RealFieldElement<T>> void doTestFrames(Field<T> field) {
		// Builds the GPSPropagator from the almanac
		FieldGPSAlmanac<T> almanac = new FieldGPSAlmanac<T>(field, FieldGPSPropagatorTest.almanac);
		final FieldGPSPropagator<T> propagator = new FieldGPSPropagator<>(field, almanac);
		Assert.assertEquals("EME2000", propagator.getFrame().getName());
		Assert.assertEquals(3.986005e14, GPSOrbitalElements.GPS_MU, 1.0e6);
		// Defines some date
		final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2016, 3, 3, 12, 0, 0.,
				TimeScalesFactory.getUTC());
		// Get PVCoordinates at the date in the ECEF
		final FieldPVCoordinates<T> pv0 = propagator.propagateInEcef(date);
		// Get PVCoordinates at the date in the ECEF
		final FieldPVCoordinates<T> pv1 = propagator.getPVCoordinates(date, propagator.getECEF());

		// Checks
		Assert.assertEquals(0., pv0.getPosition().distance(pv1.getPosition()).getReal(), 9.4e-8);
		Assert.assertEquals(0., pv0.getVelocity().distance(pv1.getVelocity()).getReal(), 2.8e-10);
	}

	public <T extends RealFieldElement<T>> void doTestNoReset(Field<T> field) {
		try {
			// Builds the GPSPropagator from the almanac
			FieldGPSAlmanac<T> almanac = new FieldGPSAlmanac<T>(field, FieldGPSPropagatorTest.almanac);
			final FieldGPSPropagator<T> propagator = new FieldGPSPropagator<>(field, almanac);
			propagator.resetInitialState(propagator.getInitialState());
			Assert.fail("an exception should have been thrown");
		} catch (OrekitException oe) {
			Assert.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
		}
		try {
			// Builds the GPSPropagator from the almanac
			FieldGPSAlmanac<T> almanac = new FieldGPSAlmanac<T>(field, FieldGPSPropagatorTest.almanac);
			final FieldGPSPropagator<T> propagator = new FieldGPSPropagator<>(field, almanac);
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

		// Builds the GPSPropagator from the almanac
		FieldGPSAlmanac<T> almanac = new FieldGPSAlmanac<T>(field, FieldGPSPropagatorTest.almanac);
		final FieldGPSPropagator<T> propagator = new FieldGPSPropagator<>(field, almanac);
		FieldGPSOrbitalElements<T> elements = propagator.getFieldGPSOrbitalElements();
		FieldAbsoluteDate<T> t0 = new FieldGNSSDate<>(elements.getWeek(), elements.getTime().multiply(0.001),
				SatelliteSystem.GPS).getDate();
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
		
		Assert.assertEquals(0.0, errorP, 3.8e-9);
		Assert.assertEquals(0.0, errorV, 3.8e-6);
		Assert.assertEquals(0.0, errorA, 1.7e-8);
	}

	public <T extends RealFieldElement<T>> void doTestPosition(Field<T> field) {
		final T zero = field.getZero();
		// Initial GPS orbital elements (Ref: IGS)
		FieldGPSOrbitalElements<T> goe = new FieldGPSEphemeris<T>(7, 0, zero.add(288000), zero.add(5153.599830627441),
				zero.add(0.012442796607501805), zero.add(4.419469802942352E-9), zero.add(0.9558937988021613),
				zero.add(-2.4608167886110235E-10), zero.add(1.0479401362158658), zero.add(-7.967117576712062E-9),
				zero.add(-2.4719019944000538), zero.add(-1.0899023379614294), zero.add(4.3995678424835205E-6),
				zero.add(1.002475619316101E-5), zero.add(183.40625), zero.add(87.03125), zero.add(3.203749656677246E-7),
				zero.add(4.0978193283081055E-8));

		// Date of the GPS orbital elements
		final FieldAbsoluteDate<T> target = goe.getDate();
		// Build the GPS propagator
		final FieldGPSPropagator<T> propagator = new FieldGPSPropagator<>(field, goe);
		// Compute the PV coordinates at the date of the GPS orbital elements
		final FieldPVCoordinates<T> pv = propagator.getPVCoordinates(target,
				FramesFactory.getITRF(IERSConventions.IERS_2010, true));
		// Computed position
		final FieldVector3D<T> computedPos = pv.getPosition();
		// Expected position (reference from IGS file igu20484_00.sp3)
		final FieldVector3D<T> expectedPos = new FieldVector3D<T>(zero.add(-4920705.292), zero.add(24248099.200),
				zero.add(9236130.101));

		Assert.assertEquals(0., FieldVector3D.distance(expectedPos, computedPos).getReal(), 3.2);
	}

	public <T extends RealFieldElement<T>> void doTestIssue544(Field<T> field) {
		// Builds the GPSPropagator from the almanac
		FieldGPSAlmanac<T> almanac = new FieldGPSAlmanac<T>(field, FieldGPSPropagatorTest.almanac);
		final FieldGPSPropagator<T> propagator = new FieldGPSPropagator<>(field, almanac);
		// In order to test the issue, we volontary set a Double.NaN value in the date.
		final FieldAbsoluteDate<T> date0 = new FieldAbsoluteDate<>(field, 2010, 5, 7, 7, 50, Double.NaN,
				TimeScalesFactory.getUTC());
		final FieldPVCoordinates<T> pv0 = propagator.propagateInEcef(date0);
		// Verify that an infinite loop did not occur
		Assert.assertEquals(FieldVector3D.getNaN(field), pv0.getPosition());
		Assert.assertEquals(FieldVector3D.getNaN(field), pv0.getVelocity());
	}

	private class FieldGPSEphemeris<T extends RealFieldElement<T>> implements FieldGPSOrbitalElements<T> {

		private int prn;
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
		 FieldGPSEphemeris(int prn, int week, T toe, T sqa, T ecc,
				 T deltaN, T inc, T iDot, T om0,
				 T dom, T aop, T anom, T cuc,
				 T cus, T crc, T crs, T cic, T cis) {
			 
			 this.prn    = prn;
			 this.week   = week;
			 this.toe    = toe;
			 this.sma    = sqa.multiply(sqa);
			 this.ecc    = ecc;
			 this.deltaN = deltaN;
			 this.inc    = inc;
			 this.iDot   = iDot;
			 this.om0    = om0;
			 this.dom    = dom;
			 this.aop    = aop;
			 this.anom   = anom;
			 this.cuc    = cuc;
			 this.cus    = cus;
			 this.crc    = crc;
			 this.crs    = crs;
			 this.cic    = cic;
			 this.cis    = cis;
		 }
		 
		    @Override
			public int getPRN() {
				return prn;
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
				return FastMath.sqrt(absA.getField().getZero().add(GPS_MU).divide(absA)).divide(absA).add(deltaN);
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
				return new FieldGNSSDate<>(week, toe.multiply(1000), SatelliteSystem.GPS).getDate();
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
			public int getIODC() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public int getIODE() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public T getTGD() {
				// TODO Auto-generated method stub
				return null;
			}

	}

}
