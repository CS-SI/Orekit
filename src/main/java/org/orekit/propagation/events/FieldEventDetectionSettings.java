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
package org.orekit.propagation.events;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.propagation.FieldSpacecraftState;

/**
 * Class containing parameters for event detection.
 *
 * @author Romain Serra.
 * @since 12.2
 * @see EventDetectionSettings
 * @see FieldEventDetector
 */
public class FieldEventDetectionSettings <T extends CalculusFieldElement<T>> {

    /** Default maximum checking interval (s). */
    public static final double DEFAULT_MAXCHECK = 600;

    /** Default convergence threshold (s). */
    public static final double DEFAULT_THRESHOLD = 1.e-6;

    /** Default maximum number of iterations in the event time search. */
    public static final int DEFAULT_MAX_ITER = 100;

    /** Adaptable interval for maximum time without event evaluation. */
    private final FieldAdaptableInterval<T> maxCheckInterval;

    /** Detection threshold (s). */
    private final T threshold;

    /** Maximum iteration number when detecting event. */
    private final int maxIterationCount;

    /**
     * Constructor.
     *
     * @param maxCheckInterval  adaptable interval
     * @param threshold         detection threshold on time
     * @param maxIterationCount maximum iteration number
     */
    public FieldEventDetectionSettings(final FieldAdaptableInterval<T> maxCheckInterval, final T threshold,
                                       final int maxIterationCount) {
        this.maxCheckInterval = maxCheckInterval;
        this.maxIterationCount = maxIterationCount;
        this.threshold = threshold;
    }

    /**
     * Constructor with maximum check as double.
     *
     * @param maxCheck          constant maximum check for adaptable interval
     * @param threshold         detection threshold on time
     * @param maxIterationCount maximum iteration number
     */
    public FieldEventDetectionSettings(final double maxCheck, final T threshold, final int maxIterationCount) {
        this(FieldAdaptableInterval.of(maxCheck), threshold, maxIterationCount);
    }

    /**
     * Constructor from non-Field settings.
     *
     * @param field field
     * @param eventDetectionSettings non-Field detection settings
     */
    public FieldEventDetectionSettings(final Field<T> field, final EventDetectionSettings eventDetectionSettings) {
        this(state -> eventDetectionSettings.getMaxCheckInterval().currentInterval(state.toSpacecraftState()),
            field.getZero().newInstance(eventDetectionSettings.getThreshold()), eventDetectionSettings.getMaxIterationCount());
    }

    /**
     * Getter for adaptable interval.
     * @return adaptable interval
     */
    public FieldAdaptableInterval<T> getMaxCheckInterval() {
        return maxCheckInterval;
    }

    /**
     * Getter for threshold.
     * @return threshold
     */
    public T getThreshold() {
        return threshold;
    }

    /**
     * Getter for max iter.
     * @return max iter
     */
    public int getMaxIterationCount() {
        return maxIterationCount;
    }

    /**
     * Create a non-Field equivalent object.
     * @return event detection settings
     */
    public EventDetectionSettings toEventDetectionSettings() {
        return new EventDetectionSettings(state -> getMaxCheckInterval().currentInterval(new FieldSpacecraftState<>(getThreshold().getField(), state)),
                getThreshold().getReal(), getMaxIterationCount());
    }
}
