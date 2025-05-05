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
package org.orekit.propagation.events.handlers;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.ode.events.Action;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/**
 * Event handler wrapping another, arbitrary one whilst remembering date of last detection.
 * If never used, the cache is null.
 * If used but nothing detected, it returns past infinity in case of forward propagation and future infinity otherwise.
 * @author Romain Serra
 * @see RecallLastOccurrence
 * @since 12.1
 * @param <T> field type
 */
public class FieldRecallLastOccurrence<T extends CalculusFieldElement<T>> implements FieldEventHandler<T> {

    /** Wrapped event handler. */
    private final FieldEventHandler<T> wrappedHandler;

    /** Last date at which the wrapped event occurred. */
    private FieldAbsoluteDate<T> lastOccurrence;

    /** Constructor.
     * @param wrappedHandler event handler to wrap
     */
    public FieldRecallLastOccurrence(final FieldEventHandler<T> wrappedHandler) {
        this.wrappedHandler = wrappedHandler;
    }

    /** Getter for last occurrence.
     * @return last date when underlying event was detected
     */
    public FieldAbsoluteDate<T> getLastOccurrence() {
        return lastOccurrence;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final FieldSpacecraftState<T> initialState, final FieldAbsoluteDate<T> target,
                     final FieldEventDetector<T> detector) {
        final boolean isForward = target.isAfter(initialState.getDate());
        final Field<T> field = target.getField();
        final AbsoluteDate date = isForward ? AbsoluteDate.PAST_INFINITY : AbsoluteDate.FUTURE_INFINITY;
        lastOccurrence = new FieldAbsoluteDate<>(field, date);
        wrappedHandler.init(initialState, target, detector);
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final FieldSpacecraftState<T> s, final FieldEventDetector<T> detector,
                                final boolean increasing) {
        lastOccurrence = s.getDate();
        return wrappedHandler.eventOccurred(s, detector, increasing);
    }

    /** {@inheritDoc} */
    @Override
    public FieldSpacecraftState<T> resetState(final FieldEventDetector<T> detector,
                                              final FieldSpacecraftState<T> oldState) {
        return wrappedHandler.resetState(detector, oldState);
    }

    /** {@inheritDoc} */
    @Override
    public void finish(final FieldSpacecraftState<T> finalState, final FieldEventDetector<T> detector) {
        wrappedHandler.finish(finalState, detector);
    }
}
