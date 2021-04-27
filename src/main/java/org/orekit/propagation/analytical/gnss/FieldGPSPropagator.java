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
 * This class aims at propagating a GPS orbit from
 * {@link FieldGPSOrbitalElements}.
 *
 * @see <a href=
 *      "http://www.gps.gov/technical/icwg/IS-GPS-200H.pdf">GPS
 *      Interface Specification</a>
 * @author Pascal Parraud
 * @author Nicolas Fialton (field translation)
 */
public class FieldGPSPropagator<T extends RealFieldElement<T>> extends FieldAbstractGNSSPropagator<T> {

    // Constants
    /** WGS 84 value of the earth's rotation rate in rad/s. */
    private static final double GPS_AV = 7.2921151467e-5;

    /** Duration of the GPS cycle in seconds. */
    private static final double GPS_CYCLE_DURATION =
        FieldGPSOrbitalElements.GPS_WEEK_IN_SECONDS *
                                                     FieldGPSOrbitalElements.GPS_WEEK_NB;

    /** The GPS orbital elements used. */
    private final FieldGPSOrbitalElements<T> gpsOrbit;

    /**
     * Default constructor.
     * <p>
     * The Field GPS orbital elements is the only requested parameter to build a
     * FieldGPSPropagator.
     * </p>
     * <p>
     * The attitude provider is set by default to the
     * {@link org.orekit.propagation.Propagator#DEFAULT_LAW DEFAULT_LAW} in the
     * default data context.<br>
     * The mass is set by default to the
     * {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
     * The ECI frame is set by default to the
     * {@link org.orekit.frames.Predefined#EME2000 EME2000 frame} in the default
     * data context.<br>
     * The ECEF frame is set by default to the
     * {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP
     * CIO/2010-based ITRF simple EOP} in the default data context.
     * </p>
     * <p>
     * This constructor uses the {@link DataContext#getDefault() default data
     * context}. Another data context can be set using
     * {@code FieldGPSPropagator(final Field<T> field, final FieldGPSOrbitalElements<T> gpsOrbit, final Frames frames)}
     * </p>
     *
     * @param field
     * @param gpsOrbit the Field GPS orbital elements to be used by the
     *        GPSpropagator.
     * @see #attitudeProvider(AttitudeProvider provider)
     * @see #mass(double mass)
     * @see #eci(Frame inertial)
     * @see #ecef(Frame bodyFixed)
     */
    @DefaultDataContext
    public FieldGPSPropagator(final Field<T> field,
                              final FieldGPSOrbitalElements<T> gpsOrbit) {
        this(field, gpsOrbit, DataContext.getDefault().getFrames());
    }

    /**
     * Constructor.
     * <p>
     * The Field GPS orbital elements is the only requested parameter to build a
     * FieldGPSPropagator.
     * </p>
     * <p>
     * The attitude provider is set by default to the
     * {@link org.orekit.propagation.Propagator#DEFAULT_LAW DEFAULT_LAW} in the
     * default data context.<br>
     * The mass is set by default to the
     * {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
     * The ECI frame is set by default to the
     * {@link org.orekit.frames.Predefined#EME2000 EME2000 frame} in the default
     * data context.<br>
     * The ECEF frame is set by default to the
     * {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP
     * CIO/2010-based ITRF simple EOP} in the default data context.
     * </p>
     * <p>
     * This constructor uses the {@link DataContext#getDefault() default data
     * context}. Another data context can be set using
     * {@code FieldGPSPropagator(final Field<T> field, final FieldGPSOrbitalElements<T> gpsOrbit, final AttitudeProvider attitudeProvider, final double mass, final Frame eci, final Frame ecef)}
     * </p>
     *
     * @param field
     * @param gpsOrbit the Field GPS orbital elements to be used by the
     *        GPSpropagator.
     * @param frames set of frames to use.
     * @see #attitudeProvider(AttitudeProvider provider)
     * @see #mass(double mass)
     * @see #eci(Frame inertial)
     * @see #ecef(Frame bodyFixed)
     */
    public FieldGPSPropagator(final Field<T> field,
                              final FieldGPSOrbitalElements<T> gpsOrbit,
                              final Frames frames) {
        this(field, gpsOrbit, Propagator.getDefaultLaw(frames), DEFAULT_MASS,
             frames.getEME2000(),
             frames.getITRF(IERSConventions.IERS_2010, true));
    }

    /**
     * Constructor.
     * <p>
     * The Field GPS orbital elements is the only requested parameter to build a
     * FieldGPSPropagator.
     * </p>
     * <p>
     * The attitude provider is set by default to the
     * {@link org.orekit.propagation.Propagator#DEFAULT_LAW DEFAULT_LAW} in the
     * default data context.<br>
     * The mass is set by default to the
     * {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
     * The ECI frame is set by default to the
     * {@link org.orekit.frames.Predefined#EME2000 EME2000 frame} in the default
     * data context.<br>
     * The ECEF frame is set by default to the
     * {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP
     * CIO/2010-based ITRF simple EOP} in the default data context.
     * </p>
     *
     * @param field
     * @param gpsOrbit the Field GPS orbital elements to be used by the
     *        GPSpropagator.
     * @param attitudeProvider
     * @param mass
     * @param eci
     * @param ecef
     */
    public FieldGPSPropagator(final Field<T> field,
                              final FieldGPSOrbitalElements<T> gpsOrbit,
                              final AttitudeProvider attitudeProvider,
                              final double mass, final Frame eci,
                              final Frame ecef) {
        super(field, gpsOrbit, attitudeProvider, eci, ecef, field.getZero().add(mass), field.getZero().add(GPS_AV),
              field.getZero().add(GPS_CYCLE_DURATION), field.getZero().add(FieldGPSOrbitalElements.GPS_MU));
        // Stores the GPS orbital elements
        this.gpsOrbit = gpsOrbit;
    }

    /**
     * Get the underlying Field GPS orbital elements.
     *
     * @return the underlying Field GPS orbital elements
     */
    public FieldGPSOrbitalElements<T> getFieldGPSOrbitalElements() {
        return gpsOrbit;
    }

    /**
     * Get the parameters driver for the Field GPS propagation model.
     *
     * @return an empty list.
     */
    @Override
    protected List<ParameterDriver> getParametersDrivers() {
        // The Field GPS propagation model does not have parameter drivers.
        return Collections.emptyList();
    }
}
