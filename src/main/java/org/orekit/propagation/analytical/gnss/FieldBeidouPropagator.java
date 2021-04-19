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
 * This class aims at propagating a Beidou orbit from
 * {@link FieldBeidouOrbitalElements}.
 *
 * @see <a href=
 *      "http://www2.unb.ca/gge/Resources/beidou_icd_english_ver2.0.pdf">Beidou
 *      Interface Control Document</a>
 * @author Bryan Cazabonne
 * @author Nicolas Fialton (field translation)
 */
public class FieldBeidouPropagator<T extends RealFieldElement<T>>
    extends
    FieldAbstractGNSSPropagator<T> {

    // Constants
    /** Value of the earth's rotation rate in rad/s. */
    private static final double BEIDOU_AV = 7.2921150e-5;

    /** Duration of the Beidou cycle in seconds. */
    private static final double BEIDOU_CYCLE_DURATION =
        FieldBeidouOrbitalElements.BEIDOU_WEEK_IN_SECONDS *
                                                        FieldBeidouOrbitalElements.BEIDOU_WEEK_NB;

    // Fields
    /** The Beidou orbital elements used. */
    private final FieldBeidouOrbitalElements<T> bdsOrbit;

    /**
     * Default constructor.
     * <p>
     * The Field Beidou orbital elements is the only requested parameter to
     * build a FieldBeidouPropagator.
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
     * {@code FieldBeidouPropagator(final Field<T> field, final FieldBeidouOrbitalElements<T> bdsOrbit, final Frames frames)}
     * </p>
     *
     * @param field
     * @param bdsOrbit the Field Beidou orbital elements to be used by the Field Beidou propagator.
     * @see #attitudeProvider(AttitudeProvider provider)
     * @see #mass(double mass)
     * @see #eci(Frame inertial)
     * @see #ecef(Frame bodyFixed)
     */
    @DefaultDataContext
    public FieldBeidouPropagator(final Field<T> field,
                                 final FieldBeidouOrbitalElements<T> bdsOrbit) {
        this(field, bdsOrbit, DataContext.getDefault().getFrames());
    }

    /**
     * Constructor.
     * <p>
     * The Field Beidou orbital elements is the only requested parameter to
     * build a FieldBeidouPropagator.
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
     * {@code FieldBeidouPropagator(final Field<T> field, final FieldBeidouOrbitalElements<T> bdsOrbit, final AttitudeProvider attitudeProvider, final double mass, final Frame eci, final Frame ecef)}
     * </p>
     *
     * @param field
     * @param bdsOrbit the Field Beidou orbital elements to be used by the Field
     *        Beidou propagator.
     * @param frames set of frames to use building the propagator.
     * @see #attitudeProvider(AttitudeProvider provider)
     * @see #mass(double mass)
     * @see #eci(Frame inertial)
     * @see #ecef(Frame bodyFixed)
     */
    public FieldBeidouPropagator(final Field<T> field,
                                 final FieldBeidouOrbitalElements<T> bdsOrbit,
                                 final Frames frames) {
        this(field, bdsOrbit, Propagator.getDefaultLaw(frames), DEFAULT_MASS,
             frames.getEME2000(),
             frames.getITRF(IERSConventions.IERS_2010, true));
    }

    /**
     * Constructor.
     * <p>
     * The Field Beidou orbital elements is the only requested parameter to
     * build a FieldBeidouPropagator.
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
     * @param bdsOrbit the Field Beidou orbital elements to be used by the Field
     *        Beidou propagator.
     * @param attitudeProvider
     * @param mass
     * @param eci
     * @param ecef
     */
    public FieldBeidouPropagator(final Field<T> field,
                                 final FieldBeidouOrbitalElements<T> bdsOrbit,
                                 final AttitudeProvider attitudeProvider,
                                 final double mass, final Frame eci,
                                 final Frame ecef) {
        super(field, bdsOrbit, attitudeProvider, eci, ecef, mass, BEIDOU_AV,
              BEIDOU_CYCLE_DURATION, FieldBeidouOrbitalElements.BEIDOU_MU);
        // Stores the Beidou orbital elements
        this.bdsOrbit = bdsOrbit;
    }

    /**
     * Get the underlying Field Beidou orbital elements.
     *
     * @return the underlying Field Beidou orbital elements
     */
    public FieldBeidouOrbitalElements<T> getFieldBeidouOrbitalElements() {
        return bdsOrbit;
    }

    /**
     * Get the parameters driver for the Field Beidou propagation model.
     *
     * @return an empty list.
     */
    @Override
    protected List<ParameterDriver> getParametersDrivers() {
        // Field Beidou propagation model does not have parameter drivers.
        return Collections.emptyList();
    }

}
