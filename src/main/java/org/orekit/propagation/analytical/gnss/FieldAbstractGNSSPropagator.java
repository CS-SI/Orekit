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

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.Precision;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldAbstractAnalyticalPropagator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;

/**
 * Common handling of {@link FieldAbstractAnalyticalPropagator} methods for GNSS
 * propagators.
 * <p>
 * This abstract class allows to provide easily a subset of
 * {@link FieldAbstractAnalyticalPropagator} methods for specific GNSS
 * propagators.
 * </p>
 * 
 * @author Pascal Parraud
 * @author Nicolas Fialton (field translation)
 */

public abstract class FieldAbstractGNSSPropagator<T extends RealFieldElement<T>>
		extends FieldAbstractAnalyticalPropagator<T> {

	// Data used to solve Kepler's equation
	/** First coefficient to compute Kepler equation solver starter. */
	private static final double A;

	/** Second coefficient to compute Kepler equation solver starter. */
	private static final double B;

	static {
		final double k1 = 3 * FastMath.PI + 2;
		final double k2 = FastMath.PI - 1;
		final double k3 = 6 * FastMath.PI - 1;
		A = 3 * k2 * k2 / k1;
		B = k3 * k3 / (6 * k1);
	}

	/** The GNSS orbital elements used. */
	private final FieldGNSSOrbitalElements<T> gnssOrbit;

	/** Mean angular velocity of the Earth. */
	private final double av;

	/** Duration of the GNSS cycle in seconds. */
	private final double cycleDuration;

	/** The spacecraft mass (kg). */
	private final T mass;

	/** The Earth gravity coefficient used for GNSS propagation. */
	private final T mu;

	/** The ECI frame used for GNSS propagation. */
	private final Frame eci;

	/** The ECEF frame used for GNSS propagation. */
	private final Frame ecef;

	/**
	 * Build a new instance.
	 * 
	 * @param field
	 * @param gnssOrbit        the common Field GNSS orbital elements to be used by the Field
	 *                         Abstract GNSS propagator
	 * @param attitudeProvider provider for attitude computation
	 * @param eci              the ECI frame used for GNSS propagation
	 * @param ecef             the ECEF frame used for GNSS propagation
	 * @param mass             the spacecraft mass (kg)
	 * @param av               mean angular velocity of the Earth (rad/s)
	 * @param cycleDuration    duration of the GNSS cycle in seconds
	 * @param mu               the Earth gravity coefficient used for GNSS
	 *                         propagation
	 */

	protected FieldAbstractGNSSPropagator(final Field<T> field, final FieldGNSSOrbitalElements<T> gnssOrbit,
			final AttitudeProvider attitudeProvider, final Frame eci, final Frame ecef, final double mass,
			final double av, final double cycleDuration, final double mu) {
		super(field, attitudeProvider);
		this.gnssOrbit = gnssOrbit;
		this.av = av;
		this.cycleDuration = cycleDuration;
		this.mass = field.getZero().add(mass);
		this.mu = field.getZero().add(mu);
		// Sets the Earth Centered Inertial frame
		this.eci = eci;
		// Sets the Earth Centered Earth Fixed frame
		this.ecef = ecef;
		// Sets the start date as the date of the orbital elements
		setStartDate(gnssOrbit.getDate());
	}

	/**
	 * Get the duration from GNSS Reference epoch.
	 * <p>
	 * This takes the GNSS week roll-over into account.
	 * </p>
	 * 
	 * @param date the considered date
	 * @return the duration from GNSS orbit Reference epoch (s)
	 */
	private T getTk(final FieldAbsoluteDate<T> date) {
		// Time from ephemeris reference epoch
		T tk = date.durationFrom(gnssOrbit.getDate());
		// Adjusts the time to take roll over week into account
		while (tk.getReal() > 0.5 * cycleDuration) {
			tk = tk.subtract(cycleDuration);
		}
		while (tk.getReal() < -0.5 * cycleDuration) {
			tk = tk.add(cycleDuration);
		}
		// Returns the time from ephemeris reference epoch
		return tk;
	}

	/**
	 * Gets the FieldPVCoordinates of the GNSS SV in {@link #getECEF() ECEF frame}.
	 *
	 * <p>
	 * The algorithm uses automatic differentiation to compute velocity and
	 * acceleration.
	 * </p>
	 *
	 * @param date the computation date
	 * @return the GNSS SV FieldPVCoordinates in {@link #getECEF() ECEF frame}
	 */
	public FieldPVCoordinates<T> propagateInEcef(final FieldAbsoluteDate<T> date) {
		// Field
		final Field<T> field = date.getField();
		// Duration from GNSS ephemeris Reference date
		final FieldUnivariateDerivative2<T> tk = new FieldUnivariateDerivative2<>(getTk(date), field.getOne(),
				field.getZero());
		// Mean anomaly
		final FieldUnivariateDerivative2<T> mk = tk.multiply(gnssOrbit.getMeanMotion()).add(gnssOrbit.getM0());
		// Eccentric Anomaly
		final FieldUnivariateDerivative2<T> ek = getEccentricAnomaly(mk);
		// True Anomaly
		final FieldUnivariateDerivative2<T> vk = getTrueAnomaly(ek);
		// Argument of Latitude
		final FieldUnivariateDerivative2<T> phik = vk.add(gnssOrbit.getPa());
		final FieldUnivariateDerivative2<T> twoPhik = phik.multiply(2);
		final FieldUnivariateDerivative2<T> c2phi = twoPhik.cos();
		final FieldUnivariateDerivative2<T> s2phi = twoPhik.sin();
		// Argument of Latitude Correction
		final FieldUnivariateDerivative2<T> dphik = c2phi.multiply(gnssOrbit.getCuc())
				.add(s2phi.multiply(gnssOrbit.getCus()));
		// Radius Correction
		final FieldUnivariateDerivative2<T> drk = c2phi.multiply(gnssOrbit.getCrc())
				.add(s2phi.multiply(gnssOrbit.getCrs()));
		// Inclination Correction
		final FieldUnivariateDerivative2<T> dik = c2phi.multiply(gnssOrbit.getCic())
				.add(s2phi.multiply(gnssOrbit.getCis()));
		// Corrected Argument of Latitude
		final FieldUnivariateDerivative2<T> uk = phik.add(dphik);
		// Corrected Radius
		final FieldUnivariateDerivative2<T> rk = ek.cos().multiply((gnssOrbit.getE()).negate()).add(1)
				.multiply(gnssOrbit.getSma()).add(drk);
		// Corrected Inclination
		final FieldUnivariateDerivative2<T> ik = tk.multiply(gnssOrbit.getIDot()).add(gnssOrbit.getI0()).add(dik);
		final FieldUnivariateDerivative2<T> cik = ik.cos();
		// Positions in orbital plane
		final FieldUnivariateDerivative2<T> xk = uk.cos().multiply(rk);
		final FieldUnivariateDerivative2<T> yk = uk.sin().multiply(rk);
		// Corrected longitude of ascending node
		final FieldUnivariateDerivative2<T> omk = tk.multiply((gnssOrbit.getOmegaDot()).subtract(av))
				.add(gnssOrbit.getOmega0().subtract(gnssOrbit.getTime().multiply(av)));
		final FieldUnivariateDerivative2<T> comk = omk.cos();
		final FieldUnivariateDerivative2<T> somk = omk.sin();
		// returns the Earth-fixed coordinates
		final FieldVector3D<FieldUnivariateDerivative2<T>> positionwithDerivatives = new FieldVector3D<>(
				xk.multiply(comk).subtract(yk.multiply(somk).multiply(cik)),
				xk.multiply(somk).add(yk.multiply(comk).multiply(cik)), yk.multiply(ik.sin()));
		return new FieldPVCoordinates<T>(
				new FieldVector3D<T>(positionwithDerivatives.getX().getValue(),
						positionwithDerivatives.getY().getValue(), positionwithDerivatives.getZ().getValue()),
				new FieldVector3D<T>(positionwithDerivatives.getX().getFirstDerivative(),
						positionwithDerivatives.getY().getFirstDerivative(),
						positionwithDerivatives.getZ().getFirstDerivative()),
				new FieldVector3D<T>(positionwithDerivatives.getX().getSecondDerivative(),
						positionwithDerivatives.getY().getSecondDerivative(),
						positionwithDerivatives.getZ().getSecondDerivative()));
	}

	/**
	 * Gets eccentric anomaly from mean anomaly.
	 * <p>
	 * The algorithm used to solve the Kepler equation has been published in:
	 * "Procedures for solving Kepler's Equation", A. W. Odell and R. H. Gooding,
	 * Celestial Mechanics 38 (1986) 307-334
	 * </p>
	 * <p>
	 * It has been copied from the OREKIT library (KeplerianOrbit class).
	 * </p>
	 *
	 * @param mk the mean anomaly (rad)
	 * @return the eccentric anomaly (rad)
	 */
	private FieldUnivariateDerivative2<T> getEccentricAnomaly(final FieldUnivariateDerivative2<T> mk) {

		final T zero = mk.getValue().getField().getZero();
		// reduce M to [-PI PI] interval
		final FieldUnivariateDerivative2<T> reducedM = new FieldUnivariateDerivative2<T>(
				MathUtils.normalizeAngle(mk.getValue(), zero), mk.getFirstDerivative(), mk.getSecondDerivative());

		// compute start value according to A. W. Odell and R. H. Gooding S12 starter
		FieldUnivariateDerivative2<T> ek;
		if (FastMath.abs(reducedM.getValue().getReal()) < 1.0 / 6.0) {
			if (FastMath.abs(reducedM.getValue().getReal()) < Precision.SAFE_MIN) {
				// this is an Orekit change to the S12 starter.
				// If reducedM is 0.0, the derivative of cbrt is infinite which induces NaN
				// appearing later in
				// the computation. As in this case E and M are almost equal, we initialize ek
				// with reducedM
				ek = reducedM;
			} else {
				// this is the standard S12 starter
				ek = reducedM.add(reducedM.multiply(6).cbrt().subtract(reducedM).multiply(gnssOrbit.getE()));
			}
		} else {
			if (reducedM.getValue().getReal() < 0) {
				final FieldUnivariateDerivative2<T> w = reducedM.add(FastMath.PI);
				ek = reducedM.add(w.multiply(-A).divide(w.subtract(B)).subtract(FastMath.PI).subtract(reducedM)
						.multiply(gnssOrbit.getE()));
			} else {
				final FieldUnivariateDerivative2<T> minusW = reducedM.subtract(FastMath.PI);
				ek = reducedM.add(minusW.multiply(A).divide(minusW.add(B)).add(FastMath.PI).subtract(reducedM)
						.multiply(gnssOrbit.getE()));
			}
		}

		final T e1 = gnssOrbit.getE().negate().add(1.0);
		final boolean noCancellationRisk = (e1.getReal()
				+ ek.getValue().getReal() * ek.getValue().getReal() / 6.0) >= 0.1;

		// perform two iterations, each consisting of one Halley step and one
		// Newton-Raphson step
		for (int j = 0; j < 2; ++j) {
			final FieldUnivariateDerivative2<T> f;
			FieldUnivariateDerivative2<T> fd;
			final FieldUnivariateDerivative2<T> fdd = ek.sin().multiply(gnssOrbit.getE());
			final FieldUnivariateDerivative2<T> fddd = ek.cos().multiply(gnssOrbit.getE());
			if (noCancellationRisk) {
				f = ek.subtract(fdd).subtract(reducedM);
				fd = fddd.subtract(1).negate();
			} else {
				f = eMeSinE(ek).subtract(reducedM);
				final FieldUnivariateDerivative2<T> s = ek.multiply(0.5).sin();
				fd = s.multiply(s).multiply(gnssOrbit.getE().multiply(2.0)).add(e1);
			}
			final FieldUnivariateDerivative2<T> dee = f.multiply(fd)
					.divide(f.multiply(0.5).multiply(fdd).subtract(fd.multiply(fd)));

			// update eccentric anomaly, using expressions that limit underflow problems
			final FieldUnivariateDerivative2<T> w = fd
					.add(dee.multiply(0.5).multiply(fdd.add(dee.multiply(fdd).divide(3))));
			fd = fd.add(dee.multiply(fdd.add(dee.multiply(0.5).multiply(fdd))));
			ek = ek.subtract(f.subtract(dee.multiply(fd.subtract(w))).divide(fd));
		}

		// expand the result back to original range
		ek = ek.add((mk.getValue()).subtract(reducedM.getValue()));

		// Returns the eccentric anomaly
		return ek;
	}

	/**
	 * Accurate computation of E - e sin(E).
	 *
	 * @param E eccentric anomaly
	 * @return E - e sin(E)
	 */
	private FieldUnivariateDerivative2<T> eMeSinE(final FieldUnivariateDerivative2<T> E) {
		FieldUnivariateDerivative2<T> x = E.sin().multiply(gnssOrbit.getE().negate().add(1));
		final FieldUnivariateDerivative2<T> mE2 = E.negate().multiply(E);
		FieldUnivariateDerivative2<T> term = E;
		FieldUnivariateDerivative2<T> d = E.getField().getZero();
		// the inequality test below IS intentional and should NOT be replaced by a
		// check with a small tolerance
		for (FieldUnivariateDerivative2<T> x0 = d.add(Double.NaN); !(x.getValue()).equals(x0.getValue());) {
			d = d.add(2);
			term = term.multiply(mE2.divide(d.multiply(d.add(1))));
			x0 = x;
			x = x.subtract(term);
		}
		return x;
	}

	/**
	 * Gets true anomaly from eccentric anomaly.
	 *
	 * @param ek the eccentric anomaly (rad)
	 * @return the true anomaly (rad)
	 */
	private FieldUnivariateDerivative2<T> getTrueAnomaly(final FieldUnivariateDerivative2<T> ek) {
		final FieldUnivariateDerivative2<T> svk = ek.sin()
				.multiply(FastMath.sqrt((gnssOrbit.getE().multiply(gnssOrbit.getE())).negate().add(1.0)));
		final FieldUnivariateDerivative2<T> cvk = ek.cos().subtract(gnssOrbit.getE());
		return svk.atan2(cvk);
	}

	/** {@inheritDoc} */
	protected FieldOrbit<T> propagateOrbit(final FieldAbsoluteDate<T> date, final T[] parameters) {
		// Gets the PVCoordinates in ECEF frame
		final FieldPVCoordinates<T> pvaInECEF = propagateInEcef(date);
		// Transforms the PVCoordinates to ECI frame
		final FieldPVCoordinates<T> pvaInECI = ecef.getTransformTo(eci, date).transformPVCoordinates(pvaInECEF);
		// Returns the Cartesian orbit
		return new FieldCartesianOrbit<T>(pvaInECI, eci, date, mu);
	}

	/**
	 * Get the Earth gravity coefficient used for GNSS propagation.
	 * 
	 * @return the Earth gravity coefficient.
	 */
	public T getMU() {
		return mu;
	}

	/** {@inheritDoc} */
	public Frame getFrame() {
		return eci;
	}

	/** {@inheritDoc} */
	protected T getMass(final FieldAbsoluteDate<T> date) {
		return mass;
	}

	/** {@inheritDoc} */
	public void resetInitialState(final FieldSpacecraftState<T> state) {
		throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
	}

	/** {@inheritDoc} */
	protected void resetIntermediateState(final FieldSpacecraftState<T> state, final boolean forward) {
		throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
	}

	/**
	 * Gets the Earth Centered Inertial frame used to propagate the orbit.
	 *
	 * @return the ECI frame
	 */
	public Frame getECI() {
		return eci;
	}

	/**
	 * Gets the Earth Centered Earth Fixed frame used to propagate GNSS orbits
	 * according to the Interface Control Document.
	 *
	 * @return the ECEF frame
	 */
	public Frame getECEF() {
		return ecef;
	}

}
