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
package org.orekit.propagation.events;

import org.orekit.propagation.events.intervals.AdaptableInterval;

/**
 * Class containing parameters for event detection.
 *
 * @author Romain Serra.
 * @since 12.2
 * @see EventDetector
 */
public class EventDetectionSettings {

    /** Default maximum checking interval (s). */
    public static final double DEFAULT_MAX_CHECK = 600;

    /** Default convergence threshold (s). */
    public static final double DEFAULT_THRESHOLD = 1.e-6;

    /** Default maximum number of iterations in the event time search. */
    public static final int DEFAULT_MAX_ITER = 100;

    /** Adaptable interval for maximum time without event evaluation. */
    private final AdaptableInterval maxCheckInterval;

    /** Detection threshold. */
    private final double threshold;

    /** Maximum iteration number when detecting event. */
    private final int maxIterationCount;

    /**
     * Constructor.
     *
     * @param maxCheckInterval  adaptable interval
     * @param threshold         detection threshold on time
     * @param maxIterationCount maximum iteration number
     */
    public EventDetectionSettings(final AdaptableInterval maxCheckInterval, final double threshold,
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
    public EventDetectionSettings(final double maxCheck, final double threshold, final int maxIterationCount) {
        this(AdaptableInterval.of(maxCheck), threshold, maxIterationCount);
    }

    /**
     * Getter for adaptable interval.
     * @return adaptable interval
     */
    public AdaptableInterval getMaxCheckInterval() {
        return maxCheckInterval;
    }

    /**
     * Getter for threshold.
     * @return threshold
     */
    public double getThreshold() {
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
     * Builds a new instance with a new max. check interval.
     * @param newMaxCheckInterval new max. check.
     * @return new object
     * @since 13.0
     */
    public EventDetectionSettings withMaxCheckInterval(final AdaptableInterval newMaxCheckInterval) {
        return new EventDetectionSettings(newMaxCheckInterval, threshold, maxIterationCount);
    }

    /**
     * Builds a new instance with a new threshold value.
     * @param newThreshold detection threshold in seconds
     * @return new object
     * @since 13.0
     */
    public EventDetectionSettings withThreshold(final double newThreshold) {
        return new EventDetectionSettings(maxCheckInterval, newThreshold, maxIterationCount);
    }

    /**
     * Builds a new instance with a new max. iteration count.
     * @param newMaxIterationCount new max iteration count.
     * @return new object
     * @since 13.0
     */
    public EventDetectionSettings withMaxIter(final int newMaxIterationCount) {
        return new EventDetectionSettings(maxCheckInterval, threshold, newMaxIterationCount);
    }

    /**
     * Returns default settings for event detections.
     * @return default settings
     */
    public static EventDetectionSettings getDefaultEventDetectionSettings() {
        return new EventDetectionSettings(DEFAULT_MAX_CHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER);
    }
}
