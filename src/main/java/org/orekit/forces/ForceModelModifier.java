/* Copyright 2022-2024 Romain Serra
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
package org.orekit.forces;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.utils.ParameterDriver;

import java.util.List;
import java.util.stream.Stream;

/**
 * Interface to wrap another force model.
 * By default, methods do not modify anything.
 *
 * @since 13.0
 * @author Romain Serra
 *
 */
public interface ForceModelModifier extends ForceModel {

    /**
     * Get the underlying force model.
     * @return underlying model
     */
    ForceModel getUnderlyingModel();

    /** {@inheritDoc} */
    @Override
    default Vector3D acceleration(SpacecraftState s, double[] parameters) {
        return getUnderlyingModel().acceleration(s, parameters);
    }

    /** {@inheritDoc} */
    @Override
    default <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(FieldSpacecraftState<T> s,
                                                                              T[] parameters) {
        return getUnderlyingModel().acceleration(s, parameters);
    }

    /** {@inheritDoc} */
    @Override
    default boolean dependsOnPositionOnly() {
        return getUnderlyingModel().dependsOnPositionOnly();
    }

    /** {@inheritDoc} */
    @Override
    default boolean dependsOnAttitudeRate() {
        return getUnderlyingModel().dependsOnAttitudeRate();
    }

    /** {@inheritDoc} */
    @Override
    default Stream<EventDetector> getEventDetectors() {
        return getUnderlyingModel().getEventDetectors();
    }

    /** {@inheritDoc} */
    @Override
    default <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(Field<T> field) {
        return getUnderlyingModel().getFieldEventDetectors(field);
    }

    /** {@inheritDoc} */
    @Override
    default List<ParameterDriver> getParametersDrivers() {
        return getUnderlyingModel().getParametersDrivers();
    }
}
