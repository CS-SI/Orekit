/* Copyright 2002-2024 CS GROUP
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
package org.orekit.propagation.conversion.osc2mean;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.MathArrays;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.FieldShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.forces.ShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;

/**
 * DSST theory for osculating to mean orbit conversion.
 *
 * @author Pascal Parraud
 * @since 13.0
 */
public class DSSTTheory implements MeanTheory {

    /** Theory used for converting from osculating to mean orbit. */
    private static final String THEORY = "DSST";

    /** Force models used to compute short periodic terms. */
    private final List<DSSTForceModel> forceModels;

    /** Spacecraft mass (kg). */
    private final double mass;

    /** Attitude provider. */
    private AttitudeProvider attitudeProvider;

    /**
     * Constructor with default values.
     * @param forceModels the force models
     */
    public DSSTTheory(final List<DSSTForceModel> forceModels) {
        this(forceModels, null, Propagator.DEFAULT_MASS);
    }

    /**
     * Full constructor.
     * @param forceModels the force models
     * @param attitudeProvider the attitude law
     * @param mass the mass (kg)
     */
    public DSSTTheory(final List<DSSTForceModel> forceModels,
                      final AttitudeProvider attitudeProvider,
                      final double mass) {
        this.forceModels = forceModels;
        this.attitudeProvider = attitudeProvider;
        this.mass = mass;
    }

    /** {@inheritDoc} */
    @Override
    public String getTheoryName() {
        return THEORY;
    }

    /** {@inheritDoc} */
    @Override
    public double getReferenceRadius() {
        return Constants.IERS2010_EARTH_EQUATORIAL_RADIUS;
    };

    /** {@inheritDoc} */
    @Override
    public Orbit preprocessing(final Orbit osculating) {
        // If not defined
        if (attitudeProvider == null) {
            // Creates a default attitude provider
            attitudeProvider = FrameAlignedProvider.of(osculating.getFrame());
        }
        // ensure all Gaussian force models can rely on attitude
        for (final DSSTForceModel force : forceModels) {
            force.registerAttitudeProvider(attitudeProvider);
        }
        // Returns an equinoctial orbit
        return OrbitType.EQUINOCTIAL.convertType(osculating);
    }

    /** {@inheritDoc} */
    @Override
    public Orbit meanToOsculating(final Orbit mean) {

        // Create the spacecraft state
        final Attitude attitude = attitudeProvider.getAttitude(mean, mean.getDate(), mean.getFrame());
        final SpacecraftState meanState = new SpacecraftState(mean, attitude, mass);

        // Create the auxiliary object
        final AuxiliaryElements aux = new AuxiliaryElements(mean, +1);

        // Set the force models
        final List<ShortPeriodTerms> shortPeriodTerms = new ArrayList<>();
        for (final DSSTForceModel force : forceModels) {
            shortPeriodTerms.addAll(force.initializeShortPeriodTerms(aux, PropagationType.OSCULATING,
                                                                     force.getParameters(mean.getDate())));
            force.updateShortPeriodTerms(force.getParametersAllValues(), meanState);
        }

        // recompute the osculating parameters from the current mean parameters
        return computeOsculatingOrbit(mean, shortPeriodTerms);
    }

    /** Compute osculating orbit from mean orbit.
     * <p>
     * Compute and add the short periodic variation to the mean orbit.
     * </p>
     * @param meanOrbit initial mean orbit
     * @param shortPeriodTerms short period terms
     * @return osculating orbit
     */
    private Orbit computeOsculatingOrbit(final Orbit meanOrbit,
                                         final List<ShortPeriodTerms> shortPeriodTerms) {

        final double[] mean = new double[6];
        final double[] meanDot = new double[6];
        OrbitType.EQUINOCTIAL.mapOrbitToArray(meanOrbit, PositionAngleType.MEAN, mean, meanDot);
        final double[] y = mean.clone();
        for (final ShortPeriodTerms spt : shortPeriodTerms) {
            final double[] shortPeriodic = spt.value(meanOrbit);
            for (int i = 0; i < shortPeriodic.length; i++) {
                y[i] += shortPeriodic[i];
            }
        }
        return OrbitType.EQUINOCTIAL.mapArrayToOrbit(y, meanDot, PositionAngleType.MEAN,
                                                     meanOrbit.getDate(), meanOrbit.getMu(),
                                                     meanOrbit.getFrame());
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldOrbit<T> preprocessing(final FieldOrbit<T> osculating) {
        // If not defined
        if (attitudeProvider == null) {
            // Creates a default attitude provider
            attitudeProvider = FrameAlignedProvider.of(osculating.getFrame());
        }
        // ensure all Gaussian force models can rely on attitude
        for (final DSSTForceModel force : forceModels) {
            force.registerAttitudeProvider(attitudeProvider);
        }
        // Returns an equinoctial orbit
        return OrbitType.EQUINOCTIAL.convertType(osculating);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends CalculusFieldElement<T>> FieldOrbit<T> meanToOsculating(final FieldOrbit<T> mean) {

        final FieldAbsoluteDate<T> date = mean.getDate();
        final Field<T> field = date.getField();

        // Create the spacecraft state
        final T fieldMass = field.getZero().newInstance(mass);
        final FieldAttitude<T> attitude = attitudeProvider.getAttitude(mean, mean.getDate(), mean.getFrame());
        final FieldSpacecraftState<T> meanState = new FieldSpacecraftState<>(mean, attitude, fieldMass);

        //Create the auxiliary object
        final FieldAuxiliaryElements<T> aux = new FieldAuxiliaryElements<>(mean, +1);

        // Set the force models
        final List<FieldShortPeriodTerms<T>> shortPeriodTerms = new ArrayList<>();
        for (final DSSTForceModel force : forceModels) {
            shortPeriodTerms.addAll(force.initializeShortPeriodTerms(aux, PropagationType.OSCULATING,
                                                                     force.getParameters(field, date)));
            force.updateShortPeriodTerms(force.getParametersAllValues(field), meanState);
        }

        // recompute the osculating parameters from the current mean parameters
        return computeOsculatingOrbit(mean, shortPeriodTerms);
    }

    /** Compute osculating orbit from mean orbit.
     * <p>
     * Compute and add the short periodic variation to the mean {@link SpacecraftState}.
     * </p>
     * @param meanOrbit initial mean orbit
     * @param shortPeriodTerms short period terms
     * @param <T> type of the elements
     * @return osculating orbit
     */
    private <T extends CalculusFieldElement<T>> FieldOrbit<T> computeOsculatingOrbit(final FieldOrbit<T> meanOrbit,
                                                                                     final List<FieldShortPeriodTerms<T>> shortPeriodTerms) {

        final T[] mean = MathArrays.buildArray(meanOrbit.getDate().getField(), 6);
        final T[] meanDot = MathArrays.buildArray(meanOrbit.getDate().getField(), 6);
        OrbitType.EQUINOCTIAL.mapOrbitToArray(meanOrbit, PositionAngleType.MEAN, mean, meanDot);
        final T[] y = mean.clone();
        for (final FieldShortPeriodTerms<T> spt : shortPeriodTerms) {
            final T[] shortPeriodic = spt.value(meanOrbit);
            for (int i = 0; i < shortPeriodic.length; i++) {
                y[i] = y[i].add(shortPeriodic[i]);
            }
        }
        return OrbitType.EQUINOCTIAL.mapArrayToOrbit(y, meanDot,
                                                     PositionAngleType.MEAN,
                                                     meanOrbit.getDate(),
                                                     meanOrbit.getMu(),
                                                     meanOrbit.getFrame());
    }
}
