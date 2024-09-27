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
package org.orekit.propagation.events.handlers;

import org.hipparchus.ode.events.Action;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;

/**
 * Event handler wrapping another, arbitrary one whilst remembering date of last detection.
 * If never used, the cache is null.
 * If used but nothing detected, it returns past infinity in case of forward propagation and future infinity otherwise.
 * @author Romain Serra
 * @see RecordAndContinue
 * @since 12.1
 */
public class RecallLastOccurrence implements EventHandler {

    /** Wrapped event handler. */
    private final EventHandler wrappedHandler;

    /** Last date at which the wrapped event occurred. */
    private AbsoluteDate lastOccurrence;

    /** Constructor.
     * @param wrappedHandler event handler to wrap
     */
    public RecallLastOccurrence(final EventHandler wrappedHandler) {
        this.wrappedHandler = wrappedHandler;
    }

    /** Getter for last occurrence.
     * @return last date when underlying event was detected
     */
    public AbsoluteDate getLastOccurrence() {
        return lastOccurrence;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target, final EventDetector detector) {
        final boolean isForward = target.isAfter(initialState.getDate());
        lastOccurrence = isForward ? AbsoluteDate.PAST_INFINITY : AbsoluteDate.FUTURE_INFINITY;
        wrappedHandler.init(initialState, target, detector);
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final SpacecraftState s, final EventDetector detector, final boolean increasing) {
        lastOccurrence = s.getDate();
        return wrappedHandler.eventOccurred(s, detector, increasing);
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState resetState(final EventDetector detector, final SpacecraftState oldState) {
        return wrappedHandler.resetState(detector, oldState);
    }

    /** {@inheritDoc} */
    @Override
    public void finish(final SpacecraftState finalState, final EventDetector detector) {
        wrappedHandler.finish(finalState, detector);
    }
}
