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
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeOffset;
import org.orekit.time.TimeShiftable;
import org.orekit.time.TimeStamped;

/**
 * Class representing single date detection.
 * It is negative before the epoch.
 * @author Romain Serra
 * @since 14.0
 */
public class SingleDateEventFunction implements EventFunction, TimeStamped, TimeShiftable<SingleDateEventFunction> {

    /** Event date. */
    private final AbsoluteDate date;

    /**
     * Constructor.
     * @param date event date
     */
    public SingleDateEventFunction(final AbsoluteDate date) {
        this.date = date;
    }

    @Override
    public double value(final SpacecraftState state) {
        return state.durationFrom(date);
    }

    @Override
    public <T extends CalculusFieldElement<T>> T value(final FieldSpacecraftState<T> fieldState) {
        return fieldState.durationFrom(date);
    }

    @Override
    public boolean dependsOnTimeOnly() {
        return true;
    }

    @Override
    public AbsoluteDate getDate() {
        return date;
    }

    @Override
    public SingleDateEventFunction shiftedBy(final double dt) {
        return shiftedBy(new TimeOffset(dt));
    }

    @Override
    public SingleDateEventFunction shiftedBy(final TimeOffset dt) {
        return new SingleDateEventFunction(date.shiftedBy(dt));
    }
}
