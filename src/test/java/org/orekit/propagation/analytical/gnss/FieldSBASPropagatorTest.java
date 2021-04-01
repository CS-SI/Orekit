package org.orekit.propagation.analytical.gnss;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.frames.FramesFactory;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

public class FieldSBASPropagatorTest {

	/** Threshold for test validation. */
	private static double eps = 1.0e-15;

	/** SBAS orbital elements. */
	private SBASNavigationData soe;
	private Frames frames;

	@Before
	public <T extends RealFieldElement<T>> void setUp() {
		// Reference data are taken from IGS file brdm0370.17p
		soe = new SBASNavigationData(127, 1935, 1.23303e+05, 2.406022248000e+07, -2.712500000000e-01,
				3.250000000000e-04, 3.460922568000e+07, 3.063125000000e-00, -1.500000000000e-04, 1.964040000000e+04,
				1.012000000000e-00, -1.250000000000e-04);
		frames = DataContext.getDefault().getFrames();
	}

	@BeforeClass
	public static void setUpBeforeClass() {
		Utils.setDataRoot("gnss");
	}

	@Test
	public void testPropagationAtReferenceTime() {
		doTestPropagation(Decimal64Field.getInstance());
	}

	@Test
	public void testPropagation() {
		doTestPropagationAtReferenceTime(Decimal64Field.getInstance());
	}

	@Test
	public void testFrames() {
		doTestFrames(Decimal64Field.getInstance());
	}

	@Test
	public void testDerivativesConsistency() {
		doTestDerivativesConsistency(Decimal64Field.getInstance());
	}

	@Test
	public void testNoReset() {
		doTestNoReset(Decimal64Field.getInstance());
	}

	public <T extends RealFieldElement<T>> void doTestPropagationAtReferenceTime(Field<T> field) {
		// SBAS propagator
		final FieldSBASNavigationData<T> soe = new FieldSBASNavigationData<>(field, this.soe);
		final FieldSBASPropagator<T> propagator = new FieldSBASPropagator<T>(field, soe);
		// Propagation
		final FieldPVCoordinates<T> pv = propagator.propagateInEcef(soe.getDate());
		// Position/Velocity/Acceleration
		final FieldVector3D<T> position = pv.getPosition();
		final FieldVector3D<T> velocity = pv.getVelocity();
		final FieldVector3D<T> acceleration = pv.getAcceleration();
		// Verify
		System.out.println(soe.getXDotDot().getReal() + "\t" + acceleration.getX().getReal() + "\t" + eps);
		System.out.println(soe.getYDotDot().getReal() + "\t" + acceleration.getY().getReal() + "\t" + eps);
		System.out.println(soe.getZDotDot().getReal() + "\t" + acceleration.getZ().getReal() + "\t" + eps);
		Assert.assertEquals(soe.getX().getReal(), position.getX().getReal(), eps);
		Assert.assertEquals(soe.getY().getReal(), position.getY().getReal(), eps);
		Assert.assertEquals(soe.getZ().getReal(), position.getZ().getReal(), eps);
		Assert.assertEquals(soe.getXDot().getReal(), velocity.getX().getReal(), eps);
		Assert.assertEquals(soe.getYDot().getReal(), velocity.getY().getReal(), eps);
		Assert.assertEquals(soe.getZDot().getReal(), velocity.getZ().getReal(), eps);
		Assert.assertEquals(soe.getXDotDot().getReal(), acceleration.getX().getReal(), eps);
		Assert.assertEquals(soe.getYDotDot().getReal(), acceleration.getY().getReal(), eps);
		Assert.assertEquals(soe.getZDotDot().getReal(), acceleration.getZ().getReal(), eps);
	}

	public <T extends RealFieldElement<T>> void doTestPropagation(Field<T> field) {
		// SBAS propagator
		final FieldSBASNavigationData<T> soe = new FieldSBASNavigationData<>(field, this.soe);
		final FieldSBASPropagator<T> propagator = new FieldSBASPropagator<T>(field, soe);
		// Propagation
		final FieldPVCoordinates<T> pv = propagator.propagateInEcef(soe.getDate().shiftedBy(1.0));
		// Position/Velocity/Acceleration
		final FieldVector3D<T> position = pv.getPosition();
		final FieldVector3D<T> velocity = pv.getVelocity();
		final FieldVector3D<T> acceleration = pv.getAcceleration();
		// Verify
		System.out.println(soe.getXDotDot().getReal() + "\t" + acceleration.getX().getReal() + "\t" + eps);
		System.out.println(soe.getYDotDot().getReal() + "\t" + acceleration.getY().getReal() + "\t" + eps);
		System.out.println(soe.getZDotDot().getReal() + "\t" + acceleration.getZ().getReal() + "\t" + eps);
		Assert.assertEquals(24060222.2089125, position.getX().getReal(), eps);
		Assert.assertEquals(34609228.7430500, position.getY().getReal(), eps);
		Assert.assertEquals(19641.4119375, position.getZ().getReal(), eps);
		Assert.assertEquals(-0.270925, velocity.getX().getReal(), eps);
		Assert.assertEquals(3.062975, velocity.getY().getReal(), eps);
		Assert.assertEquals(1.011875, velocity.getZ().getReal(), eps);
		Assert.assertEquals(soe.getXDotDot().getReal(), acceleration.getX().getReal(), eps);
		Assert.assertEquals(soe.getYDotDot().getReal(), acceleration.getY().getReal(), eps);
		Assert.assertEquals(soe.getZDotDot().getReal(), acceleration.getZ().getReal(), eps);
	}

	public <T extends RealFieldElement<T>> void doTestFrames(Field<T> field) {
		// SBAS propagator
		final FieldSBASNavigationData<T> soe = new FieldSBASNavigationData<>(field, this.soe);
		final FieldSBASPropagator<T> propagator = new FieldSBASPropagator<T>(field, soe);
		Assert.assertEquals("EME2000", propagator.getFrame().getName());
		Assert.assertEquals(3.986005e+14, propagator.getMU().getReal(), 1.0e6);
		Assert.assertEquals(propagator.getECI().getName(), propagator.getFrame().getName());
		// Defines some date
		final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field, 2017, 2, 3, 12, 0, 0.,
				TimeScalesFactory.getUTC());
		// Get PVCoordinates at the date in the ECEF
		final FieldPVCoordinates<T> pv0 = propagator.propagateInEcef(date);
		// Get PVCoordinates at the date in the ECEF
		final FieldPVCoordinates<T> pv1 = propagator.getPVCoordinates(date, propagator.getECEF());

		// Checks
		Assert.assertEquals(0., pv0.getPosition().distance(pv1.getPosition()).getReal(), 7.7e-9);
		Assert.assertEquals(0., pv0.getVelocity().distance(pv1.getVelocity()).getReal(), 3.8e-12);
	}

	public <T extends RealFieldElement<T>> void doTestDerivativesConsistency(Field<T> field) {
		final Frame eme2000 = FramesFactory.getEME2000();
		double errorP = 0;
		double errorV = 0;
		double errorA = 0;

		// SBAS propagator
		final FieldSBASNavigationData<T> soe = new FieldSBASNavigationData<>(field, this.soe);
		final FieldSBASPropagator<T> propagator = new FieldSBASPropagator<T>(field, soe);

		FieldSBASOrbitalElements<T> elements = propagator.getSBASOrbitalElements();
		FieldAbsoluteDate<T> t0 = new FieldGNSSDate<>(elements.getWeek(), elements.getTime().multiply(0.001),
				SatelliteSystem.SBAS).getDate();
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
		Assert.assertEquals(0.0, errorV, 6.7e-8);
		Assert.assertEquals(0.0, errorA, 1.8e-8);

	}

	public <T extends RealFieldElement<T>> void doTestNoReset(Field<T> field) {
		try {
			// SBAS propagator
			final FieldSBASNavigationData<T> soe = new FieldSBASNavigationData<>(field, this.soe);
			final FieldSBASPropagator<T> propagator = new FieldSBASPropagator<T>(field, soe);

			propagator.resetInitialState(propagator.getInitialState());
			Assert.fail("an exception should have been thrown");
		} catch (OrekitException oe) {
			Assert.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
		}
		try {
			// SBAS propagator
			final FieldSBASNavigationData<T> soe = new FieldSBASNavigationData<>(field, this.soe);
			final FieldSBASPropagator<T> propagator = new FieldSBASPropagator<T>(field, soe);

			propagator.resetIntermediateState(propagator.getInitialState(), true);
			Assert.fail("an exception should have been thrown");
		} catch (OrekitException oe) {
			Assert.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
		}
	}

	// END of Tests

	/** SBAS orbital elements as read from navigation data files. */
	private class SBASNavigationData implements SBASOrbitalElements {

		private int prn;
		private int week;
		private double time;
		private double x;
		private double xDot;
		private double xDotDot;
		private double y;
		private double yDot;
		private double yDotDot;
		private double z;
		private double zDot;
		private double zDotDot;

		/**
		 * Constructor.
		 * 
		 * @param prn     prn code of the satellote
		 * @param week    week number
		 * @param time    reference time (s)
		 * @param x       ECEF-X component of satellite coordinates (m)
		 * @param xDot    ECEF-X component of satellite velocity (m/s)
		 * @param xDotDot ECEF-X component of satellite acceleration (m/s²)
		 * @param y       ECEF-Y component of satellite coordinates (m)
		 * @param yDot    ECEF-Y component of satellite velocity (m/s)
		 * @param yDotDot ECEF-Y component of satellite acceleration (m/s²)
		 * @param z       ECEF-Z component of satellite coordinates (m)
		 * @param zDot    ECEF-Z component of satellite velocity (m/s)
		 * @param zDotDot ECEF-Z component of satellite acceleration (m/s²)
		 */
		public SBASNavigationData(final int prn, final int week, final double time, final double x, final double xDot,
				final double xDotDot, final double y, final double yDot, final double yDotDot, final double z,
				final double zDot, final double zDotDot) {
			this.prn = prn;
			this.week = week;
			this.time = time;
			this.x = x;
			this.xDot = xDot;
			this.xDotDot = xDotDot;
			this.y = y;
			this.yDot = yDot;
			this.yDotDot = yDotDot;
			this.z = z;
			this.zDot = zDot;
			this.zDotDot = zDotDot;
		}

		@Override
		public AbsoluteDate getDate() {
			return new GNSSDate(week, time * 1000.0, SatelliteSystem.SBAS).getDate();
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
		public double getTime() {
			return time;
		}

		@Override
		public double getX() {
			return x;
		}

		@Override
		public double getXDot() {
			return xDot;
		}

		@Override
		public double getXDotDot() {
			return xDotDot;
		}

		@Override
		public double getY() {
			return y;
		}

		@Override
		public double getYDot() {
			return yDot;
		}

		@Override
		public double getYDotDot() {
			return yDotDot;
		}

		@Override
		public double getZ() {
			return z;
		}

		@Override
		public double getZDot() {
			return zDot;
		}

		@Override
		public double getZDotDot() {
			return zDotDot;
		}

	}

	/** SBAS orbital elements as read from navigation data files. */
	private class FieldSBASNavigationData<T extends RealFieldElement<T>> implements FieldSBASOrbitalElements<T> {

		private int prn;
		private int week;
		private T time;
		private T x;
		private T xDot;
		private T xDotDot;
		private T y;
		private T yDot;
		private T yDotDot;
		private T z;
		private T zDot;
		private T zDotDot;

		/**
		 * Constructor
		 * 
		 * @param prn
		 * @param week
		 * @param time
		 * @param x
		 * @param xDot
		 * @param xDotDot
		 * @param y
		 * @param yDot
		 * @param yDotDot
		 * @param z
		 * @param zDot
		 * @param zDotDot
		 */
		public FieldSBASNavigationData(final int prn, final int week, final T time, final T x, final T xDot,
				final T xDotDot, final T y, final T yDot, final T yDotDot, final T z, final T zDot, final T zDotDot) {
			this.prn = prn;
			this.week = week;
			this.time = time;
			this.x = x;
			this.xDot = xDot;
			this.xDotDot = xDotDot;
			this.y = y;
			this.yDot = yDot;
			this.yDotDot = yDotDot;
			this.z = z;
			this.zDot = zDot;
			this.zDotDot = zDotDot;
		}

		public FieldSBASNavigationData(Field<T> field, SBASNavigationData sbasNavigationData) {
			final T zero = field.getZero();
			this.prn = sbasNavigationData.getPRN();
			this.week = sbasNavigationData.getWeek();
			this.time = zero.add(sbasNavigationData.getTime());
			this.x = zero.add(sbasNavigationData.getX());
			this.xDot = zero.add(sbasNavigationData.getXDot());
			this.xDotDot = zero.add(sbasNavigationData.getXDotDot());
			this.y = zero.add(sbasNavigationData.getY());
			this.yDot = zero.add(sbasNavigationData.getYDot());
			this.yDotDot = zero.add(sbasNavigationData.getYDotDot());
			this.z = zero.add(sbasNavigationData.getZ());
			this.zDot = zero.add(sbasNavigationData.getZDot());
			this.zDotDot = zero.add(sbasNavigationData.getZDotDot());
		}

		@Override
		public FieldAbsoluteDate<T> getDate() {
			return new FieldGNSSDate<>(week, time.multiply(1000.0), SatelliteSystem.SBAS).getDate();
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
			return time;
		}

		@Override
		public T getX() {
			return x;
		}

		@Override
		public T getXDot() {
			return xDot;
		}

		@Override
		public T getXDotDot() {
			return xDotDot;
		}

		@Override
		public T getY() {
			return y;
		}

		@Override
		public T getYDot() {
			return yDot;
		}

		@Override
		public T getYDotDot() {
			return yDotDot;
		}

		@Override
		public T getZ() {
			return z;
		}

		@Override
		public T getZDot() {
			return zDot;
		}

		@Override
		public T getZDotDot() {
			return zDotDot;
		}

		@Override
		public int getIODN() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public T getAGf0() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public T getAGf1() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public T getToc() {
			// TODO Auto-generated method stub
			return null;
		}

	}
}
