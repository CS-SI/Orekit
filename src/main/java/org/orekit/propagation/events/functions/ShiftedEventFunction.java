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

import java.util.function.ToDoubleFunction;

import org.hipparchus.CalculusFieldElement;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ExtendedStateScalarFunction;

/** Time-shifted event function. The value can depend on the state or be constant.
 * @author Romain Serra
 * @since 14.0
 */
public class ShiftedEventFunction implements EventFunctionModifier {

    /** Original event function. */
    private final EventFunction baseFunction;

    /** Event time shift. */
    private final ExtendedStateScalarFunction timeShiftFunction;

    /**
     * Constructor with constant shift.
     * @param baseFunction event function to shift in time
     * @param timeShift shift value
     */
    public ShiftedEventFunction(final EventFunction baseFunction, final double timeShift) {
        this(baseFunction, (ToDoubleFunction<SpacecraftState>) value -> timeShift);
    }

    /**
     * Constructor with non-Field function.
     * @param baseFunction event function to shift in time
     * @param timeShiftFunction shift value function
     */
    public ShiftedEventFunction(final EventFunction baseFunction, final ToDoubleFunction<SpacecraftState> timeShiftFunction) {
        this(baseFunction, ExtendedStateScalarFunction.of(timeShiftFunction));
    }

    /**
     * Constructor.
     * @param baseFunction event function to shift in time
     * @param timeShiftFunction shift value function
     */
    public ShiftedEventFunction(final EventFunction baseFunction, final ExtendedStateScalarFunction timeShiftFunction) {
        this.baseFunction = baseFunction;
        this.timeShiftFunction = timeShiftFunction;
    }

    /**
     * Getter for time shift function.
     * @return shift function
     */
    public ExtendedStateScalarFunction getTimeShiftFunction() {
        return timeShiftFunction;
    }

    @Override
    public EventFunction getBaseFunction() {
        return baseFunction;
    }

    @Override
    public double value(final SpacecraftState state) {
        return baseFunction.value(state.shiftedBy(timeShiftFunction.value(state)));
    }

    @Override
    public <T extends CalculusFieldElement<T>> T value(final FieldSpacecraftState<T> fieldState) {
        return baseFunction.value(fieldState.shiftedBy(timeShiftFunction.value(fieldState)));
    }

}
