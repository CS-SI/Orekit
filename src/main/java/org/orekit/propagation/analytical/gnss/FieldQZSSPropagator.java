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
 * This class aims at propagating a QZSS orbit from {@link FieldQZSSOrbitalElements}.
 *
 * @see <a href="http://qzss.go.jp/en/technical/download/pdf/ps-is-qzss/is-qzss-pnt-003.pdf?t=1549268771755">
 *       QZSS Interface Specification</a>
 *
 * @author Bryan Cazabonne
 * @author Nicolas Fialton (field translation)
 */
public class FieldQZSSPropagator<T extends RealFieldElement<T>> extends FieldAbstractGNSSPropagator<T> {

	// Constants
	/** WGS 84 value of the earth's rotation rate in rad/s. */
	private static final double QZSS_AV = 7.2921151467e-5;

	/** Duration of the QZSS cycle in seconds. */
	private static final double QZSS_CYCLE_DURATION = FieldQZSSOrbitalElements.QZSS_WEEK_IN_SECONDS *
			FieldQZSSOrbitalElements.QZSS_WEEK_NB;

	// Fields
	/** The QZSS orbital elements used. */
	private final FieldQZSSOrbitalElements<T> qzssOrbit;

	/**
	 * Default constructor.
	 * 
	 * <p>The Field QZSS orbital elements is the only requested parameter to build a FieldQZSSPropagator.</p>
	 * <p>The attitude provider is set by default to the
	 *  {@link org.orekit.propagation.Propagator#DEFAULT_LAW DEFAULT_LAW} in the
	 *  default data context.<br>
	 * The mass is set by default to the
	 *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
	 * The ECI frame is set by default to the
	 *  {@link org.orekit.frames.Predefined#EME2000 EME2000 frame} in the default data
	 *  context.<br>
	 * The ECEF frame is set by default to the
	 *  {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP
	 *  CIO/2010-based ITRF simple EOP} in the default data context.
	 * </p>
	 *
	 * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
	 * Another data context can be set using
	 * {@code FieldQZSSPropagator(final Field<T> field, final FieldQZSSOrbitalElements<T> qzssOrbit,
			final Frames frames)}</p>
	 * 
	 * @param field
	 * @param qzssOrbit the Field QZSS orbital elements to be used by the FieldQZSSpropagator.
	 * @see #attitudeProvider(AttitudeProvider provider)
	 * @see #mass(double mass)
	 * @see #eci(Frame inertial)
	 * @see #ecef(Frame bodyFixed)
	 */
	@DefaultDataContext
	public FieldQZSSPropagator(final Field<T> field, final FieldQZSSOrbitalElements<T> qzssOrbit) {
		this(field, qzssOrbit, DataContext.getDefault().getFrames());
	}

	/**
	 * Constructor.
	 * 
	 * <p>The Field QZSS orbital elements is the only requested parameter to build a FieldQZSSPropagator.</p>
	 * <p>The attitude provider is set by default to the
	 *  {@link org.orekit.propagation.Propagator#DEFAULT_LAW DEFAULT_LAW} in the
	 *  default data context.<br>
	 * The mass is set by default to the
	 *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
	 * The ECI frame is set by default to the
	 *  {@link org.orekit.frames.Predefined#EME2000 EME2000 frame} in the default data
	 *  context.<br>
	 * The ECEF frame is set by default to the
	 *  {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP
	 *  CIO/2010-based ITRF simple EOP} in the default data context.
	 * </p>
	 *
	 * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
	 * Another data context can be set using
	 * {@code FieldQZSSPropagator(final Field<T> field, final FieldQZSSOrbitalElements<T> qzssOrbit,
			final AttitudeProvider attitudeProvider, final double mass, final Frame eci, final Frame ecef)}</p>
	 * 
	 * @param field
	 * @param qzssOrbit  the Field QZSS orbital elements to be used by the FieldQZSSpropagator.
	 * @param frames set of reference frames to use to initialize {@link
	 *                    #ecef(Frame)}, {@link #eci(Frame)}, and {@link
	 *                    #attitudeProvider(AttitudeProvider)}.
	 */
	public FieldQZSSPropagator(final Field<T> field, final FieldQZSSOrbitalElements<T> qzssOrbit,
			final Frames frames) {
		this(field, qzssOrbit, Propagator.getDefaultLaw(frames), DEFAULT_MASS, frames.getEME2000(),
				frames.getITRF(IERSConventions.IERS_2010, true));
	}

	/**
	 * Constructor.
	 * 
	 * * <p>The Field QZSS orbital elements is the only requested parameter to build a FieldQZSSPropagator.</p>
	 * <p>The attitude provider is set by default to the
	 *  {@link org.orekit.propagation.Propagator#DEFAULT_LAW DEFAULT_LAW} in the
	 *  default data context.<br>
	 * The mass is set by default to the
	 *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
	 * The ECI frame is set by default to the
	 *  {@link org.orekit.frames.Predefined#EME2000 EME2000 frame} in the default data
	 *  context.<br>
	 * The ECEF frame is set by default to the
	 *  {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP
	 *  CIO/2010-based ITRF simple EOP} in the default data context.
	 * </p>
	 * 
	 * @param field
	 * @param qzssOrbit the Field QZSS orbital elements to be used by the FieldQZSSpropagator.
	 * @param attitudeProvider
	 * @param mass
	 * @param eci
	 * @param ecef
	 */
	public FieldQZSSPropagator(final Field<T> field, final FieldQZSSOrbitalElements<T> qzssOrbit,
			final AttitudeProvider attitudeProvider, final double mass, final Frame eci, final Frame ecef) {
		super(field, qzssOrbit, attitudeProvider, eci, ecef, mass, QZSS_AV, QZSS_CYCLE_DURATION,
				FieldQZSSOrbitalElements.QZSS_MU);
		// Stores the QZSS orbital elements
		this.qzssOrbit = qzssOrbit;
	}

	/**
	 * Get the underlying Field QZSS orbital elements.
	 *
	 * @return the underlying Field QZSS orbital elements
	 */
	public FieldQZSSOrbitalElements<T> getFieldQZSSOrbitalElements() {
		return qzssOrbit;
	}

	/** {@inheritDoc} */
	@Override
	protected List<ParameterDriver> getParametersDrivers() {
		// The QZSS propagation model does not have parameter drivers.
		return Collections.emptyList();
	}
}
