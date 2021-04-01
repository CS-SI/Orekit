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
 * This class aims at propagating a GPS orbit from {@link GPSOrbitalElements}.
 *
 * @see <a href="http://www.gps.gov/technical/icwg/IS-GPS-200H.pdf">GPS
 *      Interface Specification</a>
 * @author Pascal Parraud
 * @author Nicolas Fialton (field translation)
 */
public class FieldGPSPropagator<T extends RealFieldElement<T>> extends FieldAbstractGNSSPropagator<T> {

	// Constants
	/** WGS 84 value of the earth's rotation rate in rad/s. */
	private static final double GPS_AV = 7.2921151467e-5;

	/** Duration of the GPS cycle in seconds. */
	private static final double GPS_CYCLE_DURATION = FieldGPSOrbitalElements.GPS_WEEK_IN_SECONDS
			* FieldGPSOrbitalElements.GPS_WEEK_NB;

	/** The GPS orbital elements used. */
	private final FieldGPSOrbitalElements<T> gpsOrbit;

	/**
	 * Default constructor.
	 * 
	 * @param field
	 * @param gpsOrbit
	 */
	@DefaultDataContext
	public FieldGPSPropagator(final Field<T> field, final FieldGPSOrbitalElements<T> gpsOrbit) {
		this(field, gpsOrbit, DataContext.getDefault().getFrames());
	}

	/**
	 * Constructor.
	 * 
	 * @param field
	 * @param gpsOrbit
	 * @param frames
	 */
	public FieldGPSPropagator(final Field<T> field, final FieldGPSOrbitalElements<T> gpsOrbit, final Frames frames) {
		this(field, gpsOrbit, Propagator.getDefaultLaw(frames), DEFAULT_MASS, frames.getEME2000(),
				frames.getITRF(IERSConventions.IERS_2010, true));
	}

	/**
	 * Constructor.
	 * 
	 * @param field
	 * @param gpsOrbit
	 * @param attitudeProvider
	 * @param mass
	 * @param eci
	 * @param ecef
	 */
	public FieldGPSPropagator(final Field<T> field, final FieldGPSOrbitalElements<T> gpsOrbit,
			final AttitudeProvider attitudeProvider, final double mass, final Frame eci, final Frame ecef) {
		super(field, gpsOrbit, attitudeProvider, eci, ecef, mass, GPS_AV, GPS_CYCLE_DURATION,
				FieldGPSOrbitalElements.GPS_MU);
		// Stores the GPS orbital elements
		this.gpsOrbit = gpsOrbit;
	}

	/**
	 * Get the underlying GPS orbital elements.
	 *
	 * @return the underlying GPS orbital elements
	 */
	public FieldGPSOrbitalElements<T> getFieldGPSOrbitalElements() {
		return gpsOrbit;
	}
}
