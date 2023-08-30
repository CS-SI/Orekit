/* Contributed in the public domain.
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
package org.orekit.propagation.events;

import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;

/**
 * An event detector that negates the sign on another event detector's {@link
 * EventDetector#g(SpacecraftState) g} function.
 *
 * @author Evan Ward
 */
public class NegateDetector extends AbstractDetector<NegateDetector> {

    /** the delegate event detector. */
    private final EventDetector original;

    /**
     * Create a new event detector that negates an existing event detector.
     *
     * <p> This detector will be initialized with the same {@link
     * EventDetector#getMaxCheckInterval()}, {@link EventDetector#getThreshold()}, and
     * {@link EventDetector#getMaxIterationCount()} as {@code original}. Initially this
     * detector will use the {@link ContinueOnEvent} event handler.
     *
     * @param original detector.
     */
    public NegateDetector(final EventDetector original) {
        this(original.getMaxCheckInterval(),
                original.getThreshold(),
                original.getMaxIterationCount(),
                new ContinueOnEvent(),
                original);
    }

    /**
     * Private constructor.
     *
     * @param newMaxCheck  max check interval.
     * @param newThreshold convergence threshold in seconds.
     * @param newMaxIter   max iterations.
     * @param newHandler   event handler.
     * @param original     event detector.
     */
    protected NegateDetector(final AdaptableInterval newMaxCheck,
                             final double newThreshold,
                             final int newMaxIter,
                             final EventHandler newHandler,
                             final EventDetector original) {
        super(newMaxCheck, newThreshold, newMaxIter, newHandler);
        this.original = original;
    }

    /**
     * Get the delegate event detector.
     * @return the delegate event detector
     * @since 10.2
     */
    public EventDetector getOriginal() {
        return original;
    }

    @Override
    public void init(final SpacecraftState s0,
                     final AbsoluteDate t) {
        super.init(s0, t);
        this.original.init(s0, t);
    }

    @Override
    public double g(final SpacecraftState s) {
        return -this.original.g(s);
    }

    @Override
    protected NegateDetector create(
            final AdaptableInterval newMaxCheck,
            final double newThreshold,
            final int newMaxIter,
            final EventHandler newHandler) {
        return new NegateDetector(newMaxCheck, newThreshold, newMaxIter, newHandler,
                this.original);
    }

}
