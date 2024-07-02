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

import org.hipparchus.CalculusFieldElement;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.time.FieldAbsoluteDate;

/**
 * An event detector that negates the sign on another event detector's {@link
 * FieldEventDetector#g(FieldSpacecraftState) g} function.
 *
 * @since 12.0
 * @param <T> type of the field element
 * @author Evan Ward
 * @author Luc Maisonobe
 */
public class FieldNegateDetector<T extends CalculusFieldElement<T>>  extends FieldAbstractDetector<FieldNegateDetector<T>, T> {

    /** the delegate event detector. */
    private final FieldEventDetector<T> original;

    /**
     * Create a new event detector that negates an existing event detector.
     *
     * <p> This detector will be initialized with the same {@link
     * FieldEventDetector#getMaxCheckInterval()}, {@link FieldEventDetector#getThreshold()}, and
     * {@link FieldEventDetector#getMaxIterationCount()} as {@code original}. Initially this
     * detector will use the {@link FieldContinueOnEvent} event handler.
     *
     * @param original detector.
     */
    public FieldNegateDetector(final FieldEventDetector<T> original) {
        this(original.getMaxCheckInterval(),
             original.getThreshold(),
             original.getMaxIterationCount(),
             new FieldContinueOnEvent<>(),
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
    protected FieldNegateDetector(final FieldAdaptableInterval<T> newMaxCheck,
                                  final T newThreshold,
                                  final int newMaxIter,
                                  final FieldEventHandler<T> newHandler,
                                  final FieldEventDetector<T> original) {
        super(newMaxCheck, newThreshold, newMaxIter, newHandler);
        this.original = original;
    }

    /**
     * Get the delegate event detector.
     * @return the delegate event detector
     */
    public FieldEventDetector<T> getOriginal() {
        return original;
    }

    @Override
    public void init(final FieldSpacecraftState<T> s0,
                     final FieldAbsoluteDate<T> t) {
        super.init(s0, t);
        original.init(s0, t);
    }

    @Override
    public T g(final FieldSpacecraftState<T> s) {
        return original.g(s).negate();
    }

    @Override
    protected FieldNegateDetector<T> create(final FieldAdaptableInterval<T> newMaxCheck,
                                            final T newThreshold,
                                            final int newMaxIter,
                                            final FieldEventHandler<T> newHandler) {
        return new FieldNegateDetector<>(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                         original);
    }

}
