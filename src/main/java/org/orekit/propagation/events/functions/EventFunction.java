/* Copyright 2022-2026 Romain Serra
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
package org.orekit.propagation.events.functions;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;

import java.util.function.Function;

/**
 * Interface for functions defining events via their roots a.k.a. zeroes.
 * It regroups standard (double) and Field methods.
 * @see EventDetector
 * @see FieldEventDetector
 * @since 14.0
 * @author Romain Serra
 */
@FunctionalInterface
public interface EventFunction {

    /**
     * Evaluate function.
     * @param state spacecraft state
     * @return value
     */
    double value(SpacecraftState state);

    /**
     * Evaluate Field function. By default, calls the non-Field version.
     * This is suitable for detection but not be for applications like automatic differentiation,
     * where a proper implementation should be used instead.
     * @param fieldState spacecraft state
     * @param <T> field type
     * @return value
     */
    default <T extends CalculusFieldElement<T>> T value(final FieldSpacecraftState<T> fieldState) {
        final Field<T> field = fieldState.getDate().getField();
        final double gReal = value(fieldState.toSpacecraftState());
        return field.getZero().newInstance(gReal);
    }

    /**
     * Method returning true if and only if the event function does not depend on dependent variables,
     * just the independent one i.e. time. This information is used for performance in propagation
     * and derivatives correction with switches in the dynamics.
     * @return flag
     */
    default boolean dependsOnTimeOnly() {
        return false;
    }

    /**
     * Method returning true if and only if the event function does not depend on dependent variables,
     * other than the Cartesian coordinates (or equivalent), mass and attitude (excepts for its rates).
     * It should thus return false if the STM is needed to evaluate the event.
     * This information is used for performance in propagation.
     * @return flag
     */
    default boolean dependsOnMainVariablesOnly() {
        return true;
    }

    /**
     * Builds an instance from a mapping with a single Field. Generalizes naively to all other Field via real part.
     * Flags are set to default values.
     * @param field original field
     * @param fieldStateFunction Field function
     * @return generic event function working for any field
     * @param <T> field type
     */
    static <T extends CalculusFieldElement<T>> EventFunction of(final Field<T> field,
                                                                final Function<FieldSpacecraftState<T>, T> fieldStateFunction) {
        return new EventFunction() {

            @Override
            public double value(final SpacecraftState state) {
                return value(new FieldSpacecraftState<>(field, state)).getReal();
            }

            @Override
            @SuppressWarnings("unchecked")
            public <S extends CalculusFieldElement<S>> S value(final FieldSpacecraftState<S> fieldState) {
                if (fieldState.getMass().getField().equals(field)) {
                    // call input implementation if applicable
                    final FieldSpacecraftState<T> castFieldState = (FieldSpacecraftState<T>) fieldState;
                    return (S) fieldStateFunction.apply(castFieldState);
                } else {
                    // otherwise, use constant from non-Field version
                    return fieldState.getMass().newInstance(value(fieldState.toSpacecraftState()));
                }
            }
        };
    }
}
