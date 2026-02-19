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
package org.orekit.utils;

import java.util.function.ToDoubleFunction;

import org.hipparchus.CalculusFieldElement;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;

/** Interface representing a scalar function depending only on {@link SpacecraftState} or its field counterpart.
 * @author Romain Serra
 * @since 14.0
 */
@FunctionalInterface
public interface ExtendedStateScalarFunction {

    /** Evaluate the function.
     * @param state spacecraft state as the sole free parameter of the function.
     * @return scalar value of the function
     */
    double value(SpacecraftState state);

    /** Evaluate the Field function.
     * @param fieldState spacecraft state as the sole free parameter of the function.
     * @param <T> field type
     * @return scalar value of the function
     */
    default <T extends CalculusFieldElement<T>> T value(final FieldSpacecraftState<T> fieldState) {
        return fieldState.getMass().getField().getZero().newInstance(value(fieldState.toSpacecraftState()));
    }

    /**
     * Create instance from pure double function.
     * @param function function of double
     * @return extended function
     */
    static ExtendedStateScalarFunction of(final ToDoubleFunction<SpacecraftState> function) {
        return function::applyAsDouble;
    }
}
