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
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;

/** Event function modifier.
 * Default implementations simply wrap the base function.
 * @author Romain Serra
 * @see EventFunction
 * @since 14.0
 */
public interface EventFunctionModifier extends EventFunction {

    EventFunction getBaseFunction();

    @Override
    default double value(final SpacecraftState state) {
        return getBaseFunction().value(state);
    }

    @Override
    default <T extends CalculusFieldElement<T>> T value(final FieldSpacecraftState<T> fieldState) {
        return getBaseFunction().value(fieldState);
    }

    @Override
    default boolean dependsOnTimeOnly() {
        return getBaseFunction().dependsOnTimeOnly();
    }

    @Override
    default boolean dependsOnMainVariablesOnly() {
        return getBaseFunction().dependsOnMainVariablesOnly();
    }

    static EventFunctionModifier addReal(final EventFunction eventFunction, final double increment) {
        return new EventFunctionModifier() {
            @Override
            public EventFunction getBaseFunction() {
                return eventFunction;
            }

            @Override
            public double value(final SpacecraftState state) {
                return eventFunction.value(state) + increment;
            }

            @Override
            public <T extends CalculusFieldElement<T>> T value(final FieldSpacecraftState<T> fieldState) {
                return eventFunction.value(fieldState).add(increment);
            }
        };
    }

    static <S extends CalculusFieldElement<S>> EventFunctionModifier addFieldValue(final EventFunction eventFunction,
                                                                                   final S increment) {
        return new EventFunctionModifier() {
            @Override
            public EventFunction getBaseFunction() {
                return eventFunction;
            }

            @Override
            public double value(final SpacecraftState state) {
                return eventFunction.value(state) + increment.getReal();
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T extends CalculusFieldElement<T>> T value(final FieldSpacecraftState<T> fieldState) {
                final T g = eventFunction.value(fieldState);
                if (g.getField().equals(increment.getField())) {
                    return (T) ((S) g).add(increment);
                } else {
                    return g.add(increment.getReal());
                }
            }
        };
    }
}
