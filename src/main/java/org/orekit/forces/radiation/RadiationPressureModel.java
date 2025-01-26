/* Copyright 2022-2025 Romain Serra
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
package org.orekit.forces.radiation;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;

import java.util.List;
import java.util.stream.Stream;

/**
 * Class representing a light-induced radiation pressure force, by leveraging on a given flux model.
 *
 * <p>
 *     This class should not be used in addition to {@link SolarRadiationPressure}, which is another way of
 *     representing the same orbital perturbation.
 * </p>
 *
 * @author Romain Serra
 * @see LightFluxModel
 * @see RadiationSensitive
 * @since 12.1
 */
public class RadiationPressureModel implements RadiationForceModel {

    /**
     * Light flux model (including eclipse conditions).
     */
    private final LightFluxModel lightFluxModel;

    /**
     * Object defining radiation properties defining the acceleration from the pressure.
     */
    private final RadiationSensitive radiationSensitive;

    /**
     * Constructor.
     * @param lightFluxModel model for light flux
     * @param radiationSensitive object defining radiation properties
     */
    public RadiationPressureModel(final LightFluxModel lightFluxModel,
                                  final RadiationSensitive radiationSensitive) {
        this.lightFluxModel = lightFluxModel;
        this.radiationSensitive = radiationSensitive;
    }

    /**
     * Getter for radiation sensitive object.
     * @return radiation sensitive object
     */
    public RadiationSensitive getRadiationSensitive() {
        return radiationSensitive;
    }

    /**
     * Getter for light flux model.
     * @return flux model
     */
    public LightFluxModel getLightFluxModel() {
        return lightFluxModel;
    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return radiationSensitive instanceof IsotropicRadiationClassicalConvention || radiationSensitive instanceof IsotropicRadiationCNES95Convention || radiationSensitive instanceof IsotropicRadiationSingleCoefficient;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target) {
        RadiationForceModel.super.init(initialState, target);
        lightFluxModel.init(initialState, target);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> void init(final FieldSpacecraftState<T> initialState,
                                                         final FieldAbsoluteDate<T> target) {
        RadiationForceModel.super.init(initialState, target);
        lightFluxModel.init(initialState, target);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState s, final double[] parameters) {
        final Vector3D fluxVector = lightFluxModel.getLightFluxVector(s);
        return radiationSensitive.radiationPressureAcceleration(s, fluxVector, parameters);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s, final T[] parameters) {
        final FieldVector3D<T> fluxVector = lightFluxModel.getLightFluxVector(s);
        return radiationSensitive.radiationPressureAcceleration(s, fluxVector, parameters);
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return radiationSensitive.getRadiationParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventDetectors() {
        final List<EventDetector> eventDetectors = lightFluxModel.getEclipseConditionsDetector();
        return Stream.concat(RadiationForceModel.super.getEventDetectors(), eventDetectors.stream());
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
        final List<FieldEventDetector<T>> eventDetectors = lightFluxModel.getFieldEclipseConditionsDetector(field);
        return Stream.concat(RadiationForceModel.super.getFieldEventDetectors(field), eventDetectors.stream());
    }
}
