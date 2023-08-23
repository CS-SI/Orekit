/* Copyright 2002-2023 Luc Maisonobe
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
package org.orekit.estimation.measurements.generation;

import java.util.Collections;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.orekit.estimation.measurements.ComparableMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.time.AbsoluteDate;


/** Subscriber that gather all generated measurements in a sorted set.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class GatheringSubscriber implements GeneratedMeasurementSubscriber {

    /** Set for holding measurements. */
    private SortedSet<ObservedMeasurement<?>> measurements;

    /** Simple constructor.
     */
    public GatheringSubscriber() {
        measurements = Collections.emptySortedSet();
    }

    /** {@inheritDoc} */
    @Override
    public void init(final AbsoluteDate start, final AbsoluteDate end) {
        final Comparator<ObservedMeasurement<?>> comparator = end.isAfterOrEqualTo(start) ?
                                                              Comparator.naturalOrder() :
                                                              Comparator.reverseOrder();
        measurements = new TreeSet<>(comparator);
    }

    /** {@inheritDoc} */
    @Override
    public void handleGeneratedMeasurement(final ObservedMeasurement<?> measurement) {
        measurements.add(measurement);
    }

    /** Get generated measurements.
     * <p>
     * The measurements are sorted according to {@link ComparableMeasurement} if
     * generation was chronological, or reversed {@link ComparableMeasurement} if
     * generation was non-chronological.
     * </p>
     * @return unmodifiable view of generated measurements
     */
    public SortedSet<ObservedMeasurement<?>> getGeneratedMeasurements() {
        return Collections.unmodifiableSortedSet(measurements);
    }

}
