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
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.frames.FramesFactory;
import org.orekit.gnss.IRNSSAlmanac;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

public class FieldIRNSSPropagatorTest {

	private static IRNSSOrbitalElements almanac;
	private static Frames frames;

	@BeforeClass
	public static void setUpBeforeClass() {
		Utils.setDataRoot("gnss");

		// Almanac for satellite 1 for April 1st 2014 (Source: Rinex 3.04 format - Table
		// A19)
		final int week = 1786;
		final double toa = 172800.0;
		almanac = new IRNSSAlmanac(1, week, toa, 6.493487739563E03, 2.257102518342E-03, 4.758105460020e-01,
				-8.912102146884E-01, -4.414469594664e-09, -2.999907424014, -1.396094758025, -9.473115205765e-04,
				1.250555214938e-12, new GNSSDate(week, toa * 1000, SatelliteSystem.IRNSS).getDate());
		frames = DataContext.getDefault().getFrames();
	}

	@Test
	public void testBeidouCycle() {
		doTestIRNSSCycle(Decimal64Field.getInstance());
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
	public void testIssue544() {
		doTestIssue544(Decimal64Field.getInstance());
	}

	public <T extends RealFieldElement<T>> void doTestIRNSSCycle(Field<T> field) {
		// Builds the IRNSS propagator from the almanac
		FieldIRNSSAlmanac<T> almanac = new FieldIRNSSAlmanac<T>(field, FieldIRNSSPropagatorTest.almanac);
		final FieldIRNSSPropagator<T> propagator = new FieldIRNSSPropagator<T>(field, almanac, frames);
		// Propagate at the IRNSS date and one IRNSS cycle later
		final FieldAbsoluteDate<T> date0 = almanac.getDate();
		final FieldVector3D<T> p0 = propagator.propagateInEcef(date0).getPosition();
		final double bdtCycleDuration = IRNSSOrbitalElements.IRNSS_WEEK_IN_SECONDS * IRNSSOrbitalElements.IRNSS_WEEK_NB;
		final FieldAbsoluteDate<T> date1 = date0.shiftedBy(bdtCycleDuration);
		final FieldVector3D<T> p1 = propagator.propagateInEcef(date1).getPosition();

		// Checks
		Assert.assertEquals(0., p0.distance(p1).getReal(), 0.);
	}

	public <T extends RealFieldElement<T>> void doTestFrames(Field<T> field) {
		// Builds the IRNSS propagator from the almanac
		FieldIRNSSAlmanac<T> almanac = new FieldIRNSSAlmanac<T>(field, FieldIRNSSPropagatorTest.almanac);
		final FieldIRNSSPropagator<T> propagator = new FieldIRNSSPropagator<T>(field, almanac, frames);
		Assert.assertEquals("EME2000", propagator.getFrame().getName());
		Assert.assertEquals(3.986005e+14, IRNSSOrbitalElements.IRNSS_MU, 1.0e6);
		// Defines some date
		final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2016, 3, 3, 12, 0, 0.,
				TimeScalesFactory.getUTC());
		// Get PVCoordinates at the date in the ECEF
		final FieldPVCoordinates<T> pv0 = propagator.propagateInEcef(date);
		// Get PVCoordinates at the date in the ECEF
		final FieldPVCoordinates<T> pv1 = propagator.getPVCoordinates(date, propagator.getECEF());

		// Checks
		Assert.assertEquals(0., pv0.getPosition().distance(pv1.getPosition()).getReal(), 3.3e-8);
		Assert.assertEquals(0., pv0.getVelocity().distance(pv1.getVelocity()).getReal(), 3.9e-12);
	}

	public <T extends RealFieldElement<T>> void doTestNoReset(Field<T> field) {
		try {
			// Builds the IRNSS propagator from the almanac
			FieldIRNSSAlmanac<T> almanac = new FieldIRNSSAlmanac<T>(field, FieldIRNSSPropagatorTest.almanac);
			final FieldIRNSSPropagator<T> propagator = new FieldIRNSSPropagator<T>(field, almanac, frames);
			propagator.resetInitialState(propagator.getInitialState());
			Assert.fail("an exception should have been thrown");
		} catch (OrekitException oe) {
			Assert.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
		}
		try {
			// Builds the IRNSS propagator from the almanac
			FieldIRNSSAlmanac<T> almanac = new FieldIRNSSAlmanac<T>(field, FieldIRNSSPropagatorTest.almanac);
			final FieldIRNSSPropagator<T> propagator = new FieldIRNSSPropagator<T>(field, almanac, frames);
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
		// Builds the IRNSS propagator from the almanac
		FieldIRNSSAlmanac<T> almanac = new FieldIRNSSAlmanac<T>(field, FieldIRNSSPropagatorTest.almanac);
		final FieldIRNSSPropagator<T> propagator = new FieldIRNSSPropagator<T>(field, almanac, frames);
		FieldIRNSSOrbitalElements<T> elements = propagator.getFieldIRNSSOrbitalElements();
		FieldAbsoluteDate<T> t0 = new FieldGNSSDate<>(elements.getWeek(), elements.getTime().multiply(0.001),
				SatelliteSystem.IRNSS).getDate();
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
		Assert.assertEquals(0.0, errorV, 2.6e-7);
		Assert.assertEquals(0.0, errorA, 6.5e-8);

	}

	public <T extends RealFieldElement<T>> void doTestIssue544(Field<T> field) {
		// Builds the IRNSS propagator from the almanac
		FieldIRNSSAlmanac<T> almanac = new FieldIRNSSAlmanac<T>(field, FieldIRNSSPropagatorTest.almanac);
		final FieldIRNSSPropagator<T> propagator = new FieldIRNSSPropagator<T>(field, almanac, frames);
		// In order to test the issue, we volontary set a Double.NaN value in the date.
		final FieldAbsoluteDate<T> date0 = new FieldAbsoluteDate<>(field, 2010, 5, 7, 7, 50, Double.NaN,
				TimeScalesFactory.getUTC());
		final FieldPVCoordinates<T> pv0 = propagator.propagateInEcef(date0);
		// Verify that an infinite loop did not occur
		Assert.assertEquals(FieldVector3D.getNaN(field), pv0.getPosition());
		Assert.assertEquals(FieldVector3D.getNaN(field), pv0.getVelocity());
	}
}
