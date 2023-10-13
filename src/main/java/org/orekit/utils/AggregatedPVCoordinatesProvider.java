/* Copyright 2002-2023 Joseph Reed
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Joseph Reed licenses this file to You under the Apache License, Version 2.0
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

import java.util.Objects;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/** Aggreate multiple {@link PVCoordinatesProvider} instances together
 *
 * This can be used to describe an aircraft or surface vehicle.
 *
 * @author Joe Reed
 * @since 11.3
 */
public class AggregatedPVCoordinatesProvider implements PVCoordinatesProvider {

    /** Map of provider instances by transition time. */
    private final TimeSpanMap<PVCoordinatesProvider> pvProvMap;

    /** Earliest date at which {@link #getPVCoordinates(AbsoluteDate, Frame)} will return a valid result. */
    private final AbsoluteDate minDate;

    /** Latest date at which {@link #getPVCoordinates(AbsoluteDate, Frame)} will return a valid result. */
    private final AbsoluteDate maxDate;

    /** Class constructor.
     *
     * Note the provided {@code map} is used directly. Modification of the
     * map after calling this constructor may result in undefined behavior.
     *
     * @param map the map of {@link PVCoordinatesProvider} instances by time.
     */
    public AggregatedPVCoordinatesProvider(final TimeSpanMap<PVCoordinatesProvider> map) {
        this(map, null, null);
    }

    /** Class constructor.
     *
     * Note the provided {@code map} is used directly. Modification of the
     * map after calling this constructor may result in undefined behavior.
     *
     * @param map the map of {@link PVCoordinatesProvider} instances by time.
     * @param minDate the earliest valid date, {@code null} if always valid
     * @param maxDate the latest valid date, {@code null} if always valid
     */
    public AggregatedPVCoordinatesProvider(final TimeSpanMap<PVCoordinatesProvider> map,
            final AbsoluteDate minDate, final AbsoluteDate maxDate) {
        this.pvProvMap = Objects.requireNonNull(map, "PVCoordinatesProvider map must be non-null");
        this.minDate = minDate == null ? AbsoluteDate.PAST_INFINITY : minDate;
        this.maxDate = maxDate == null ? AbsoluteDate.FUTURE_INFINITY : maxDate;
    }

    /** Get the first date of the range.
     * @return the first date of the range
     */
    public AbsoluteDate getMinDate() {
        return minDate;
    }

    /** Get the last date of the range.
     * @return the last date of the range
     */
    public AbsoluteDate getMaxDate() {
        return maxDate;
    }

    @Override
    public Vector3D getPosition(final AbsoluteDate date, final Frame frame) {
        if (date.isBefore(minDate) || date.isAfter(maxDate)) {
            throw new OrekitIllegalArgumentException(OrekitMessages.OUT_OF_RANGE_DATE, date, minDate, maxDate);
        }
        return pvProvMap.get(date).getPosition(date, frame);
    }

    @Override
    public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
        if (date.isBefore(minDate) || date.isAfter(maxDate)) {
            throw new OrekitIllegalArgumentException(OrekitMessages.OUT_OF_RANGE_DATE, date, minDate, maxDate);
        }
        return pvProvMap.get(date).getPVCoordinates(date, frame);
    }

    /**
     * Builder class for {@link AggregatedPVCoordinatesProvider}.
     */
    public static class Builder {

        /** Time span map holding the incremental values. */
        private TimeSpanMap<PVCoordinatesProvider> pvProvMap = null;

        /**
         * Create a builder using the {@link InvalidPVProvider} as the initial provider.
         */
        public Builder() {
            this(new InvalidPVProvider());
        }

        /**
         * Create a builder using the provided initial provider.
         *
         * @param initialProvider the inital provider
         */
        public Builder(final PVCoordinatesProvider initialProvider) {
            pvProvMap = new TimeSpanMap<PVCoordinatesProvider>(initialProvider);
        }

        /** Add a {@link PVCoordinatesProvider} to the collection.
         *
         * The provided date is the transition time, at which this provider will be used.
         *
         * @param date the transition date
         * @param pvProv the provider
         * @param erasesLater if true, the entry erases all existing transitions that are later than {@code date}
         * @return this builder instance
         * @see TimeSpanMap#addValidAfter(Object, AbsoluteDate, boolean)
         */
        public Builder addPVProviderAfter(final AbsoluteDate date,
                                          final PVCoordinatesProvider pvProv,
                                          final boolean erasesLater) {
            pvProvMap.addValidAfter(pvProv, date, erasesLater);
            return this;
        }

        /** Add a {@link PVCoordinatesProvider} to the collection.
         *
         * The provided date is the final transition time, before which this provider will be used.
         *
         * @param date the transition date
         * @param pvProv the provider
         * @param erasesEarlier if true, the entry erases all existing transitions that are earlier than {@code date}
         * @return this builder instance
         * @see TimeSpanMap#addValidBefore(Object, AbsoluteDate, boolean)
         */
        public Builder addPVProviderBefore(final AbsoluteDate date, final PVCoordinatesProvider pvProv, final boolean erasesEarlier) {
            pvProvMap.addValidBefore(pvProv, date, erasesEarlier);
            return this;
        }

        /** Indicate the date before which the resulting PVCoordinatesProvider is invalid.
         *
         * @param firstValidDate first date at which the resuling provider should be valid
         * @return this instance
         */
        public Builder invalidBefore(final AbsoluteDate firstValidDate) {
            pvProvMap.addValidBefore(new InvalidPVProvider(), firstValidDate, true);
            return this;
        }

        /** Indicate the date after which the resulting PVCoordinatesProvider is invalid.
         *
         * @param lastValidDate last date at which the resuling provider should be valid
         * @return this instance
         */
        public Builder invalidAfter(final AbsoluteDate lastValidDate) {
            pvProvMap.addValidAfter(new InvalidPVProvider(), lastValidDate, true);
            return this;
        }

        /** Build the aggregated PVCoordinatesProvider.
         *
         * @return the new provider instance.
         */
        public AggregatedPVCoordinatesProvider build() {
            AbsoluteDate minDate = null;
            AbsoluteDate maxDate = null;
            // check the first span
            if (pvProvMap.getFirstTransition() != null) {
                if (pvProvMap.getFirstTransition().getBefore() instanceof InvalidPVProvider) {
                    minDate = pvProvMap.getFirstTransition().getDate();
                }
            }
            if (pvProvMap.getLastTransition() != null) {
                if (pvProvMap.getLastTransition().getAfter() instanceof InvalidPVProvider) {
                    maxDate = pvProvMap.getLastTransition().getDate();
                }
            }
            return new AggregatedPVCoordinatesProvider(pvProvMap, minDate, maxDate);
        }
    }

    /**  Implementation of {@link PVCoordinatesProvider} that throws an illegal state exception.
     *
     */
    public static class InvalidPVProvider implements PVCoordinatesProvider {

        /** Empty constructor.
         * <p>
         * This constructor is not strictly necessary, but it prevents spurious
         * javadoc warnings with JDK 18 and later.
         * </p>
         * @since 12.0
         */
        public InvalidPVProvider() {
            // nothing to do
        }

        @Override
        public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
            throw new IllegalStateException();
        }

    }

}
