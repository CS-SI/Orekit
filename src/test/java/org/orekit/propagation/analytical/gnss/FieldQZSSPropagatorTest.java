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
import org.orekit.gnss.QZSSAlmanac;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

public class FieldQZSSPropagatorTest {

	private static QZSSAlmanac almanac;

	@BeforeClass
	public static void setUpBeforeClass() {
		Utils.setDataRoot("gnss");

		// Almanac for satellite 193 for May 27th 2019 (q201914.alm)
		almanac = new QZSSAlmanac(null, 193, 7, 348160.0, 6493.145996, 7.579761505E-02, 0.7201680272, -1.643310999,
				-3.005839491E-09, -1.561775201, -4.050903957E-01, -2.965927124E-04, 7.275957614E-12, 0);
	}

	@Test
	public void testQZSSCycle() {
		doTestQZSSCycle(Decimal64Field.getInstance());
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

	public <T extends RealFieldElement<T>> void doTestQZSSCycle(Field<T> field) {
		// Builds the QZSSPropagator from the almanac
		FieldQZSSAlmanac<T> almanac = new FieldQZSSAlmanac<T>(field, FieldQZSSPropagatorTest.almanac);
		final FieldQZSSPropagator<T> propagator = new FieldQZSSPropagator<>(field, almanac);

		// Propagate at the QZSS date and one QZSS cycle later
		final FieldAbsoluteDate<T> date0 = almanac.getDate();
		final FieldVector3D<T> p0 = propagator.propagateInEcef(date0).getPosition();
		final double gpsCycleDuration = QZSSOrbitalElements.QZSS_WEEK_IN_SECONDS * QZSSOrbitalElements.QZSS_WEEK_NB;
		final FieldAbsoluteDate<T> date1 = date0.shiftedBy(gpsCycleDuration);
		final FieldVector3D<T> p1 = propagator.propagateInEcef(date1).getPosition();

		// Checks
		Assert.assertEquals(0., p0.distance(p1).getReal(), 0.);
	}

	public <T extends RealFieldElement<T>> void doTestFrames(Field<T> field) {
		// Builds the QZSSPropagator from the almanac
		FieldQZSSAlmanac<T> almanac = new FieldQZSSAlmanac<T>(field, FieldQZSSPropagatorTest.almanac);
		final FieldQZSSPropagator<T> propagator = new FieldQZSSPropagator<>(field, almanac);
		Assert.assertEquals("EME2000", propagator.getFrame().getName());
		Assert.assertEquals(3.986005e+14, FieldQZSSOrbitalElements.QZSS_MU, 1.0e6);
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
			// Builds the QZSSPropagator from the almanac
			FieldQZSSAlmanac<T> almanac = new FieldQZSSAlmanac<T>(field, FieldQZSSPropagatorTest.almanac);
			final FieldQZSSPropagator<T> propagator = new FieldQZSSPropagator<>(field, almanac);
			propagator.resetInitialState(propagator.getInitialState());
			Assert.fail("an exception should have been thrown");
		} catch (OrekitException oe) {
			Assert.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
		}
		try {
			// Builds the QZSSPropagator from the almanac
			FieldQZSSAlmanac<T> almanac = new FieldQZSSAlmanac<T>(field, FieldQZSSPropagatorTest.almanac);
			final FieldQZSSPropagator<T> propagator = new FieldQZSSPropagator<>(field, almanac);
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

		// Builds the QZSSPropagator from the almanac
		FieldQZSSAlmanac<T> almanac = new FieldQZSSAlmanac<T>(field, FieldQZSSPropagatorTest.almanac);
		final FieldQZSSPropagator<T> propagator = new FieldQZSSPropagator<>(field, almanac);
		FieldQZSSOrbitalElements<T> elements = propagator.getFieldQZSSOrbitalElements();
		FieldAbsoluteDate<T> t0 = new FieldGNSSDate<>(elements.getWeek(), elements.getTime().multiply(0.001),
				SatelliteSystem.QZSS).getDate();
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
		Assert.assertEquals(0.0, errorV, 8.4e-8);
		Assert.assertEquals(0.0, errorA, 2.21e-8);
	}

	public <T extends RealFieldElement<T>> void doTestPosition(Field<T> field) {
		final T zero = field.getZero();
		// Initial QZSS orbital elements (Ref: IGS)
		final FieldQZSSOrbitalElements<T> qoe = new FieldQZSSEphemeris<>(
				195, 
				21, 
				zero.add(226800.0),
				zero.add(6493.226968765259), 
				zero.add(0.07426900835707784), 
				zero.add(4.796628370253418E-10),
				zero.add(0.7116940567084221), 
				zero.add(4.835915721014987E-10), 
				zero.add(0.6210371871830609),
				zero.add(-8.38963517626603E-10), 
				zero.add(-1.5781555771543598), 
				zero.add(1.077008903618136),
				zero.add(-8.8568776845932E-6), 
				zero.add(1.794286072254181E-5), 
				zero.add(-344.03125),
				zero.add(-305.6875), 
				zero.add(1.2032687664031982E-6), 
				zero.add(-2.6728957891464233E-6));
		// Date of the QZSS orbital elements
		final FieldAbsoluteDate<T> target = qoe.getDate();
		final FieldQZSSPropagator<T> propagator = new FieldQZSSPropagator<>(field, qoe);
		// Compute the PV coordinates at the date of the QZSS orbital elements
		final FieldPVCoordinates<T> pv = propagator.getPVCoordinates(target,
				FramesFactory.getITRF(IERSConventions.IERS_2010, true));
		// Computed position
		final FieldVector3D<T> computedPos = pv.getPosition();
		// Expected position (reference from QZSS sp3 file qzu20693_00.sp3)
		final FieldVector3D<T> expectedPos = new FieldVector3D<T>(zero.add(-35047225.493), zero.add(18739632.916),
				zero.add(-9522204.569));
		Assert.assertEquals(0., FieldVector3D.distance(expectedPos, computedPos).getReal(), 0.7);
	}

	public <T extends RealFieldElement<T>> void doTestIssue544(Field<T> field) {
		// Builds the QZSSPropagator from the almanac
		FieldQZSSAlmanac<T> almanac = new FieldQZSSAlmanac<T>(field, FieldQZSSPropagatorTest.almanac);
		final FieldQZSSPropagator<T> propagator = new FieldQZSSPropagator<>(field, almanac);
		// In order to test the issue, we volontary set a Double.NaN value in the date.
		final FieldAbsoluteDate<T> date0 = new FieldAbsoluteDate<>(field, 2010, 5, 7, 7, 50, Double.NaN,
				TimeScalesFactory.getUTC());
		final FieldPVCoordinates<T> pv0 = propagator.propagateInEcef(date0);
		// Verify that an infinite loop did not occur
		Assert.assertEquals(FieldVector3D.getNaN(field), pv0.getPosition());
		Assert.assertEquals(FieldVector3D.getNaN(field), pv0.getVelocity());
	}

	// ENF of Tests

	private class FieldQZSSEphemeris<T extends RealFieldElement<T>> implements FieldQZSSOrbitalElements<T> {

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
		FieldQZSSEphemeris(int prn, int week, T toe, T sqa, T ecc, T deltaN, T inc, T iDot, T om0, T dom, T aop, T anom,
				T cuc, T cus, T crc, T crs, T cic, T cis) {
			this.prn = prn;
			this.week = week;
			this.toe = toe;
			this.sma = sqa.multiply(sqa);
			this.ecc = ecc;
			this.deltaN = deltaN;
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
		public T getE() {
			return ecc;
		}
		
		@Override
		public T getMeanMotion() {
			final T absA = FastMath.abs(sma);
			return FastMath.sqrt(absA.getField().getZero().add(QZSS_MU).divide(absA)).divide(absA).add(deltaN);
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
			return new FieldGNSSDate<>(week, toe.multiply(1000), SatelliteSystem.QZSS).getDate();
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
