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
import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.propagation.Propagator;
import org.orekit.utils.IERSConventions;

/**
 * This class aims at propagating a IRNSS orbit from
 * {@link FieldIRNSSOrbitalElements}.
 *
 * @see "Indian Regional Navigation Satellite System, Signal In Space ICD for
 *      standard positioning service, version 1.1"
 *
 * @author Bryan Cazabonne
 * @author Nicolas Fialton (field translation)
 */
public class FieldIRNSSPropagator<T extends RealFieldElement<T>> extends FieldAbstractGNSSPropagator<T> {
	// Constants
	/** WGS 84 value of the earth's rotation rate in rad/s. */
	private static final double IRNSS_AV = 7.2921151467e-5;

	/** Duration of the IRNSS cycle in seconds. */
	private static final double IRNSS_CYCLE_DURATION = FieldIRNSSOrbitalElements.IRNSS_WEEK_IN_SECONDS
			* FieldIRNSSOrbitalElements.IRNSS_WEEK_NB;

	// Fields
	/** The IRNSS orbital elements used. */
	private final FieldIRNSSOrbitalElements<T> irnssOrbit;

	/**
	 * Default constructor.
	 * 
	 * @param field
	 * @param irnssOrbit
	 */
	@DefaultDataContext
	public FieldIRNSSPropagator(final Field<T> field, final FieldIRNSSOrbitalElements<T> irnssOrbit) {
		this(field, irnssOrbit, DataContext.getDefault().getFrames());
	}

	/**
	 * Constructor.
	 * 
	 * @param field
	 * @param irnssOrbit
	 * @param frames
	 */
	public FieldIRNSSPropagator(final Field<T> field, final FieldIRNSSOrbitalElements<T> irnssOrbit,
			final Frames frames) {
		this(field, irnssOrbit, Propagator.getDefaultLaw(frames), DEFAULT_MASS, frames.getEME2000(),
				frames.getITRF(IERSConventions.IERS_2010, true));
	}

	/**
	 * Constructor.
	 * 
	 * @param field
	 * @param irnssOrbit
	 * @param attitudeProvider
	 * @param mass
	 * @param eci
	 * @param ecef
	 */
	public FieldIRNSSPropagator(final Field<T> field, final FieldIRNSSOrbitalElements<T> irnssOrbit,
			final AttitudeProvider attitudeProvider, final double mass, final Frame eci, final Frame ecef) {
		super(field, irnssOrbit, attitudeProvider, eci, ecef, mass, IRNSS_AV, IRNSS_CYCLE_DURATION,
				FieldIRNSSOrbitalElements.IRNSS_MU);
		// Stores the IRNSS orbital elements
		this.irnssOrbit = irnssOrbit;
	}

	public FieldIRNSSOrbitalElements<T> getFieldIRNSSOrbitalElements() {
		return irnssOrbit;
	}

}
