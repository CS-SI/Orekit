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

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.FieldAbsoluteDate;

public class FieldJ2DifferentialEffect<T extends RealFieldElement<T>>
		implements FieldAdapterPropagator.FieldDifferentialEffect<T> {
	/** Reference date. */
	private final FieldAbsoluteDate<T> referenceDate;

	/** Differential drift on perigee argument. */
	private final T dPaDot;

	/** Differential drift on ascending node. */
	private final T dRaanDot;

	/** Indicator for applying effect before reference date. */
	private final boolean applyBefore;

	/**
	 * Simple constructor.
	 * <p>
	 * The {@code applyBefore} parameter is mainly used when the differential effect
	 * is associated with a maneuver. In this case, the parameter must be set to
	 * {@code false}.
	 * </p>
	 * 
	 * @param original     original state at reference date
	 * @param directEffect direct effect changing the orbit
	 * @param applyBefore  if true, effect is applied both before and after
	 *                     reference date, if false it is only applied after
	 *                     reference date
	 * @param gravityField gravity field to use
	 */
	public FieldJ2DifferentialEffect(Field<T> field, final FieldSpacecraftState<T> original,
			final FieldAdapterPropagator.FieldDifferentialEffect<T> directEffect, final boolean applyBefore,
			final UnnormalizedSphericalHarmonicsProvider gravityField) {
		this(field, original, directEffect, applyBefore, gravityField.getAe(), gravityField.getMu(),
				-gravityField.onDate(original.getDate().toAbsoluteDate()).getUnnormalizedCnm(2, 0));
	}

	/**
	 * Simple constructor.
	 * <p>
	 * The {@code applyBefore} parameter is mainly used when the differential effect
	 * is associated with a maneuver. In this case, the parameter must be set to
	 * {@code false}.
	 * </p>
	 * 
	 * @param orbit0       original orbit at reference date
	 * @param orbit1       shifted orbit at reference date
	 * @param applyBefore  if true, effect is applied both before and after
	 *                     reference date, if false it is only applied after
	 *                     reference date
	 * @param gravityField gravity field to use
	 */
	public FieldJ2DifferentialEffect(Field<T> field, final FieldOrbit<T> orbit0, final FieldOrbit<T> orbit1,
			final boolean applyBefore, final UnnormalizedSphericalHarmonicsProvider gravityField) {
		this(field, orbit0, orbit1, applyBefore, gravityField.getAe(), gravityField.getMu(),
				-gravityField.onDate(orbit0.getDate().toAbsoluteDate()).getUnnormalizedCnm(2, 0));
	}

	/**
	 * Simple constructor.
	 * <p>
	 * The {@code applyBefore} parameter is mainly used when the differential effect
	 * is associated with a maneuver. In this case, the parameter must be set to
	 * {@code false}.
	 * </p>
	 * 
	 * @param original        original state at reference date
	 * @param directEffect    direct effect changing the orbit
	 * @param applyBefore     if true, effect is applied both before and after
	 *                        reference date, if false it is only applied after
	 *                        reference date
	 * @param referenceRadius reference radius of the Earth for the potential model
	 *                        (m)
	 * @param mu              central attraction coefficient (m³/s²)
	 * @param j2              un-normalized zonal coefficient (about +1.08e-3 for
	 *                        Earth)
	 */
	public FieldJ2DifferentialEffect(Field<T> field, final FieldSpacecraftState<T> original,
			final FieldAdapterPropagator.FieldDifferentialEffect<T> directEffect, final boolean applyBefore,
			final double referenceRadius, final double mu, final double j2) {
		this(field, original.getOrbit(), directEffect.apply(original.shiftedBy(0.001)).getOrbit().shiftedBy(-0.001),
				applyBefore, referenceRadius, mu, j2);
	}

	/**
	 * Simple constructor.
	 * <p>
	 * The {@code applyBefore} parameter is mainly used when the differential effect
	 * is associated with a maneuver. In this case, the parameter must be set to
	 * {@code false}.
	 * </p>
	 * 
	 * @param orbit0          original orbit at reference date
	 * @param orbit1          shifted orbit at reference date
	 * @param applyBefore     if true, effect is applied both before and after
	 *                        reference date, if false it is only applied after
	 *                        reference date
	 * @param referenceRadius reference radius of the Earth for the potential model
	 *                        (m)
	 * @param mu              central attraction coefficient (m³/s²)
	 * @param j2              un-normalized zonal coefficient (about +1.08e-3 for
	 *                        Earth)
	 */
	public FieldJ2DifferentialEffect(Field<T> field, final FieldOrbit<T> orbit0, final FieldOrbit<T> orbit1,
			final boolean applyBefore, final double referenceRadius, double mu, double j2) {

		this.referenceDate = orbit0.getDate();
		this.applyBefore = applyBefore;

		// extract useful parameters
		final T a0 = orbit0.getA();
		final T e0 = orbit0.getE();
		final T i0 = orbit0.getI();
		final T a1 = orbit1.getA();
		final T e1 = orbit1.getE();
		final T i1 = orbit1.getI();

		// compute reference drifts
		final T zero = field.getZero();
		final T oMe2 = zero.add(1).subtract(e0.multiply(e0));
		final T ratio = zero.add(referenceRadius).divide(a0.multiply(oMe2));
		final FieldSinCos<T> scI = FastMath.sinCos(i0);
		final T n = FastMath.sqrt(zero.add(mu).divide(a0)).divide(a0);
		final T c = ratio.multiply(ratio).multiply(n).multiply(j2);
		final T refPaDot = zero.add(0.75).multiply(c)
				.multiply(zero.add(4).subtract(zero.add(5).multiply(scI.sin()).multiply(scI.sin())));
		final T refRaanDot = zero.add(-1.5).multiply(c).multiply(scI.cos());

		// differential model on perigee argument drift
		final T dPaDotDa = zero.add(-3.5).multiply(refPaDot).divide(a0);
		final T dPaDotDe = zero.add(4).multiply(refPaDot).multiply(e0).divide(oMe2);
		final T dPaDotDi = zero.add(-7.5).multiply(c).multiply(scI.sin()).multiply(scI.cos());
		dPaDot = dPaDotDa.multiply(a1.subtract(a0)).add(dPaDotDe.multiply(e1.subtract(e0)))
				.add(dPaDotDi.multiply(i1.subtract(i0)));

		// differential model on ascending node drift
		final T dRaanDotDa = zero.add(-3.5).multiply(refRaanDot).divide(a0);
		final T dRaanDotDe = zero.add(4).multiply(refRaanDot).multiply(e0).divide(oMe2);
		final T dRaanDotDi = zero.subtract(refRaanDot).multiply(FastMath.tan(i0));
		dRaanDot = dRaanDotDa.multiply(a1.subtract(a0)).add(dRaanDotDe.multiply(e1.subtract(e0))).add(dRaanDotDi.multiply(i1.subtract(i0)));
	}

	/**
	 * Compute the effect of the maneuver on an orbit.
	 * 
	 * @param orbit1 original orbit at t₁, without maneuver
	 * @return orbit at t₁, taking the maneuver into account if t₁ &gt; t₀
	 * @see #apply(SpacecraftState)
	 */
	public FieldOrbit<T> apply(final FieldOrbit<T> orbit1) {

		if (orbit1.getDate().compareTo(referenceDate) <= 0 && !applyBefore) {
			// the orbit change has not occurred yet, don't change anything
			return orbit1;
		}

		return updateOrbit(orbit1);

	}

	/** {@inheritDoc} */
	public FieldSpacecraftState<T> apply(final FieldSpacecraftState<T> state1) {

		if (state1.getDate().compareTo(referenceDate) <= 0 && !applyBefore) {
			// the orbit change has not occurred yet, don't change anything
			return state1;
		}

		return new FieldSpacecraftState<>(updateOrbit(state1.getOrbit()), state1.getAttitude(), state1.getMass());

	}

	/**
	 * Compute the differential effect of J2 on an orbit.
	 * 
	 * @param orbit1 original orbit at t₁, without differential J2
	 * @return orbit at t₁, always taking the effect into account
	 */
	private FieldOrbit<T> updateOrbit(final FieldOrbit<T> orbit1) {

		// convert current orbital state to equinoctial elements
		final FieldEquinoctialOrbit<T> original = (FieldEquinoctialOrbit<T>) OrbitType.EQUINOCTIAL.convertType(orbit1);

		// compute differential effect
		final FieldAbsoluteDate<T> date = original.getDate();
		final T dt = date.durationFrom(referenceDate);
		final T dPaRaan = (dPaDot.add(dRaanDot)).multiply(dt);
		final FieldSinCos<T> scPaRaan = FastMath.sinCos(dPaRaan);
		final T dRaan = dRaanDot.multiply(dt);
		final FieldSinCos<T> scRaan = FastMath.sinCos(dRaan);

		final T ex = original.getEquinoctialEx().multiply(scPaRaan.cos())
				.subtract(original.getEquinoctialEy().multiply(scPaRaan.sin()));
		final T ey = original.getEquinoctialEx().multiply(scPaRaan.sin())
				.add(original.getEquinoctialEy().multiply(scPaRaan.cos()));
		final T hx = original.getHx().multiply(scRaan.cos()).subtract(original.getHy().multiply(scRaan.sin()));
		final T hy = original.getHx().multiply(scRaan.sin()).add(original.getHy().multiply(scRaan.cos()));
		final T lambda = original.getLv().add(dPaRaan);

		// build updated orbit
		final FieldEquinoctialOrbit<T> updated = new FieldEquinoctialOrbit<>(original.getA(), ex, ey, hx, hy, lambda,
				PositionAngle.TRUE, original.getFrame(), date, original.getMu());

		// convert to required type
		return orbit1.getType().convertType(updated);

	}

}
