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
 * This class aims at propagating a Galileo orbit from
 * {@link GalileoOrbitalElements}.
 *
 * @see <a href=
 *      "https://www.gsc-europa.eu/system/files/galileo_documents/Galileo-OS-SIS-ICD.pdf">Galileo
 *      Interface Control Document</a>
 *
 * @author Bryan Cazabonne
 * @author Nicolas Fialton (field translation)
 */
public class FieldGalileoPropagator<T extends RealFieldElement<T>> extends FieldAbstractGNSSPropagator<T> {

	// Constants
	/** Value of the earth's rotation rate in rad/s. */
	private static final double GALILEO_AV = 7.2921151467e-5;

	/** Duration of the Galileo cycle in seconds. */
	private static final double GALILEO_CYCLE_DURATION = GalileoOrbitalElements.GALILEO_WEEK_IN_SECONDS
			* GalileoOrbitalElements.GALILEO_WEEK_NB;

	// Fields
	/** The Galileo orbital elements used. */
	private final FieldGalileoOrbitalElements<T> galileoOrbit;

	/**
	 * Default constructor
	 * 
	 * @param field
	 * @param galileoOrbit
	 */
	@DefaultDataContext
	public FieldGalileoPropagator(final Field<T> field, final FieldGalileoOrbitalElements<T> galileoOrbit) {
		this(field, galileoOrbit, DataContext.getDefault().getFrames());
	}

	/**
	 * Constructor
	 * 
	 * @param field
	 * @param galileoOrbit
	 * @param frames
	 */
	public FieldGalileoPropagator(final Field<T> field, final FieldGalileoOrbitalElements<T> galileoOrbit,
			final Frames frames) {
		this(field, galileoOrbit, Propagator.getDefaultLaw(frames), DEFAULT_MASS, frames.getEME2000(),
				frames.getITRF(IERSConventions.IERS_2010, true));
	}

	/**
	 * Constructor
	 * 
	 * @param field
	 * @param galileoOrbit
	 * @param attitudeProvider
	 * @param mass
	 * @param eci
	 * @param ecef
	 */
	public FieldGalileoPropagator(final Field<T> field, final FieldGalileoOrbitalElements<T> galileoOrbit,
			final AttitudeProvider attitudeProvider, final double mass, final Frame eci, final Frame ecef) {
		super(field, galileoOrbit, attitudeProvider, eci, ecef, mass, GALILEO_AV, GALILEO_CYCLE_DURATION,
				FieldGalileoOrbitalElements.GALILEO_MU);
		// Stores the Beidou orbital elements
		this.galileoOrbit = galileoOrbit;
	}

	/**
	 * Get the underlying Beidou orbital elements.
	 *
	 * @return the underlying Beidou orbital elements
	 */
	public FieldGalileoOrbitalElements<T> getFieldGalileoOrbitalElements() {
		return galileoOrbit;
	}

}
