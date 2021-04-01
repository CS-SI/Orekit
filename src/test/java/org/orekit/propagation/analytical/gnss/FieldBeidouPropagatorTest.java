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

public class FieldBeidouPropagatorTest {

	@Test
	public void testBeidouCycle() {
		doTestBeidouCycle(Decimal64Field.getInstance());
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

	public <T extends RealFieldElement<T>> void doTestBeidouCycle(Field<T> field) {
		final T zero = field.getZero();
		final FieldBeidouAlmanac<T> almanac = new FieldBeidouAlmanac<T>(18, 694, zero.add(4096), zero.add(6493.3076),
				zero.add(0.00482368), zero.add(0.0), zero.add(-0.01365602), zero.add(1.40069711),
				zero.add(-2.11437379e-9), zero.add(3.11461541), zero.add(-2.53029382), zero.add(0.0001096725),
				zero.add(7.27596e-12), 0);
		// Builds the BeiDou propagator from the almanac
		final FieldBeidouPropagator<T> propagator = new FieldBeidouPropagator<>(field, almanac);
		// Intermediate verification
		Assert.assertEquals(18, almanac.getPRN());
		Assert.assertEquals(0, almanac.getHealth());
		Assert.assertEquals(0.0001096725, almanac.getAf0().getReal(), 1.0e-15);
		Assert.assertEquals(7.27596e-12, almanac.getAf1().getReal(), 1.0e-15);
		// Propagate at the BeiDou date and one BeiDou cycle later
		final FieldAbsoluteDate<T> date0 = almanac.getDate();
		final FieldVector3D<T> p0 = propagator.propagateInEcef(date0).getPosition();
		final double bdtCycleDuration = BeidouOrbitalElements.BEIDOU_WEEK_IN_SECONDS
				* BeidouOrbitalElements.BEIDOU_WEEK_NB;
		final FieldAbsoluteDate<T> date1 = date0.shiftedBy(bdtCycleDuration);
		final FieldVector3D<T> p1 = propagator.propagateInEcef(date1).getPosition();

		// Checks
		Assert.assertEquals(0., p0.distance(p1).getReal(), 0.);
	}

	public <T extends RealFieldElement<T>> void doTestFrames(Field<T> field) {
		final T zero = field.getZero();
		final FieldBeidouAlmanac<T> almanac = new FieldBeidouAlmanac<T>(18, 694, zero.add(4096), zero.add(6493.3076),
				zero.add(0.00482368), zero.add(0.0), zero.add(-0.01365602), zero.add(1.40069711),
				zero.add(-2.11437379e-9), zero.add(3.11461541), zero.add(-2.53029382), zero.add(0.0001096725),
				zero.add(7.27596e-12), 0);
		// Builds the BeiDou propagator from the almanac
		final FieldBeidouPropagator<T> propagator = new FieldBeidouPropagator<>(field, almanac);
		Assert.assertEquals("EME2000", propagator.getFrame().getName());
		Assert.assertEquals(3.986004418e+14, BeidouOrbitalElements.BEIDOU_MU, 1.0e6);
		// Defines some date
		final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<T>(field, 2016, 3, 3, 12, 0, 0.,
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
			final T zero = field.getZero();
			final FieldBeidouAlmanac<T> almanac = new FieldBeidouAlmanac<T>(18, 694, zero.add(4096),
					zero.add(6493.3076), zero.add(0.00482368), zero.add(0.0), zero.add(-0.01365602),
					zero.add(1.40069711), zero.add(-2.11437379e-9), zero.add(3.11461541), zero.add(-2.53029382),
					zero.add(0.0001096725), zero.add(7.27596e-12), 0);
			// Builds the BeiDou propagator from the almanac
			final FieldBeidouPropagator<T> propagator = new FieldBeidouPropagator<>(field, almanac);
			propagator.resetInitialState(propagator.getInitialState());
			Assert.fail("an exception should have been thrown");
		} catch (OrekitException oe) {
			Assert.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
		}
		try {
			final T zero = field.getZero();
			final FieldBeidouAlmanac<T> almanac = new FieldBeidouAlmanac<T>(18, 694, zero.add(4096),
					zero.add(6493.3076), zero.add(0.00482368), zero.add(0.0), zero.add(-0.01365602),
					zero.add(1.40069711), zero.add(-2.11437379e-9), zero.add(3.11461541), zero.add(-2.53029382),
					zero.add(0.0001096725), zero.add(7.27596e-12), 0);
			// Builds the BeiDou propagator from the almanac
			final FieldBeidouPropagator<T> propagator = new FieldBeidouPropagator<>(field, almanac);
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
		final FieldBeidouAlmanac<T> almanac = new FieldBeidouAlmanac<T>(18, 694, zero.add(4096), zero.add(6493.3076),
				zero.add(0.00482368), zero.add(0.0), zero.add(-0.01365602), zero.add(1.40069711),
				zero.add(-2.11437379e-9), zero.add(3.11461541), zero.add(-2.53029382), zero.add(0.0001096725),
				zero.add(7.27596e-12), 0);
		// Builds the BeiDou propagator from the almanac
		final FieldBeidouPropagator<T> propagator = new FieldBeidouPropagator<>(field, almanac);
		FieldBeidouOrbitalElements<T> elements = propagator.getFieldBeidouOrbitalElements();
		FieldAbsoluteDate<T> t0 = new FieldGNSSDate<>(elements.getWeek(), elements.getTime().multiply(0.001),
				SatelliteSystem.BEIDOU).getDate();
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
		Assert.assertEquals(0.0, errorV, 8.0e-8);
		Assert.assertEquals(0.0, errorA, 2.0e-8);
	}

	public <T extends RealFieldElement<T>> void doTestPosition(Field<T> field) {
		final T zero = field.getZero();
		// Initial BeiDou orbital elements (Ref: IGS)
        final FieldBeidouOrbitalElements<T> boe = new FieldBeidouEphemeris<>(7, 713, zero.add(284400.0),zero.add(6492.84515953064), zero.add(0.00728036486543715),
        		zero.add(2.1815194404696853E-9), zero.add(0.9065628903946735), zero.add(0.0), zero.add(-0.6647664535282437),
        		zero.add(-3.136916379444212E-9), zero.add(-2.6584351442773304), zero.add(0.9614806010234702),
        		zero.add(7.306225597858429E-6), zero.add(-6.314832717180252E-6), zero.add(406.96875),
        		zero.add(225.9375), zero.add(-7.450580596923828E-9), zero.add(-1.4062970876693726E-7));
        // Date of the BeiDou orbital elements (GPStime - BDTtime = 14s)
        final FieldAbsoluteDate<T> target = boe.getDate().shiftedBy(-14.0);
        // Build the BeiDou propagator
        final FieldBeidouPropagator<T> propagator = new FieldBeidouPropagator<>(field, boe);
        // Compute the PV coordinates at the date of the BeiDou orbital elements
        final FieldPVCoordinates<T> pv = propagator.getPVCoordinates(target, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Computed position
        final FieldVector3D<T> computedPos = pv.getPosition();
        // Expected position (reference from sp3 file WUM0MGXULA_20192470700_01D_05M_ORB.SP33)
        final FieldVector3D<T> expectedPos = new FieldVector3D<>(zero.add(-10260690.520),zero.add(24061180.795) ,zero.add(-32837341.074));
        Assert.assertEquals(0., FieldVector3D.distance(expectedPos, computedPos).getReal(), 3.1);
	}

	public <T extends RealFieldElement<T>> void doTestIssue544(Field<T> field) {
		final T zero = field.getZero();
		final FieldBeidouAlmanac<T> almanac = new FieldBeidouAlmanac<T>(18, 694, zero.add(4096), zero.add(6493.3076),
				zero.add(0.00482368), zero.add(0.0), zero.add(-0.01365602), zero.add(1.40069711),
				zero.add(-2.11437379e-9), zero.add(3.11461541), zero.add(-2.53029382), zero.add(0.0001096725),
				zero.add(7.27596e-12), 0);
		// Builds the BeiDou propagator from the almanac
		final FieldBeidouPropagator<T> propagator = new FieldBeidouPropagator<>(field, almanac);
		// In order to test the issue, we volontary set a Double.NaN value in the date.
		final FieldAbsoluteDate<T> date0 = new FieldAbsoluteDate<>(field, 2010, 5, 7, 7, 50, Double.NaN,
				TimeScalesFactory.getUTC());
		final FieldPVCoordinates<T> pv0 = propagator.propagateInEcef(date0);
		// Verify that an infinite loop did not occur
		Assert.assertEquals(FieldVector3D.getNaN(field), pv0.getPosition());
		Assert.assertEquals(FieldVector3D.getNaN(field), pv0.getVelocity());
	}
	
	
	
	
	private class FieldBeidouEphemeris<T extends RealFieldElement<T>> implements FieldBeidouOrbitalElements<T> {

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
        FieldBeidouEphemeris(int prn, int week, T toe, T sqa, T ecc,
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
            return FastMath.sqrt(absA.getField().getZero().add(BEIDOU_MU).divide(absA)).divide(absA).add(deltaN);
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
            return new FieldGNSSDate<>(week, toe.multiply(1000), SatelliteSystem.BEIDOU).getDate();
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
		public int getAODC() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getAODE() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getIOD() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public T getTGD1() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public T getTGD2() {
			// TODO Auto-generated method stub
			return null;
		}
        
    }
}
