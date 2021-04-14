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

import java.util.Collections;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.propagation.Propagator;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;

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
	 * <p>The Field IRNSS orbital elements is the only requested parameter to build a FieldIRNSSPropagator.</p>
	 * <p>The attitude provider is set by default to be aligned with the J2000 frame.<br>
	 * The mass is set by default to the
	 *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
	 * The ECI frame is set by default to the
	 *  {@link org.orekit.frames.Predefined#EME2000 EME2000 frame}.<br>
	 * The ECEF frame is set by default to the
	 *  {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP CIO/2010-based ITRF simple EOP}.
	 * </p>
	 * 
	 * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
	 * Another data context can be set using
	 * {@code FieldIRNSSPropagator(final Field<T> field, final FieldIRNSSOrbitalElements<T> irnssOrbit,
			final Frames frames)}</p>
	 * 
	 * @param field
	 * @param irnssOrbit the Field IRNSS orbital elements to be used by the IRNSSpropagator.
	 */
	@DefaultDataContext
	public FieldIRNSSPropagator(final Field<T> field, final FieldIRNSSOrbitalElements<T> irnssOrbit) {
		this(field, irnssOrbit, DataContext.getDefault().getFrames());
	}

	/**
	 * Constructor.
	 * 
	 * <p>The Field IRNSS orbital elements is the only requested parameter to build a FieldIRNSSPropagator.</p>
	 * <p>The attitude provider is set by default to be aligned with the J2000 frame.<br>
	 * The mass is set by default to the
	 *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
	 * The ECI frame is set by default to the
	 *  {@link org.orekit.frames.Predefined#EME2000 EME2000 frame}.<br>
	 * The ECEF frame is set by default to the
	 *  {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP CIO/2010-based ITRF simple EOP}.
	 * </p>
	 * 
	 * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
	 * Another data context can be set using
	 * {@code FieldIRNSSPropagator(final Field<T> field, final FieldIRNSSOrbitalElements<T> irnssOrbit,
			final AttitudeProvider attitudeProvider, final double mass, final Frame eci, final Frame ecef)}</p>
	 * 
	 * @param field
	 * @param irnssOrbit the Field IRNSS orbital elements to be used by the IRNSSpropagator.
	 * @param frames set of reference frames to use to initialize {@link
	 *                    #ecef(Frame)}, {@link #eci(Frame)}, and {@link
	 *                    #attitudeProvider(AttitudeProvider)}.
	 * @see #attitudeProvider(AttitudeProvider provider)
	 * @see #mass(double mass)
	 * @see #eci(Frame inertial)
	 * @see #ecef(Frame bodyFixed)
	 */
	public FieldIRNSSPropagator(final Field<T> field, final FieldIRNSSOrbitalElements<T> irnssOrbit,
			final Frames frames) {
		this(field, irnssOrbit, Propagator.getDefaultLaw(frames), DEFAULT_MASS, frames.getEME2000(),
				frames.getITRF(IERSConventions.IERS_2010, true));
	}

	/**
	 * Constructor.
	 * 
	 * <p>The Field IRNSS orbital elements is the only requested parameter to build a FieldIRNSSPropagator.</p>
	 * <p>The attitude provider is set by default to be aligned with the J2000 frame.<br>
	 * The mass is set by default to the
	 *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
	 * The ECI frame is set by default to the
	 *  {@link org.orekit.frames.Predefined#EME2000 EME2000 frame}.<br>
	 * The ECEF frame is set by default to the
	 *  {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP CIO/2010-based ITRF simple EOP}.
	 * </p>
	 * 
	 * @param field
	 * @param irnssOrbit the Field IRNSS orbital elements to be used by the IRNSSpropagator.
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
	

	/** Get the parameters driver for the Field IRNSS propagation model.
     * @return an empty list.
     */
	@Override
	protected List<ParameterDriver> getParametersDrivers() {
		// Field IRNSS propagation model does not have parameter drivers.
		return Collections.emptyList();
	}

}
