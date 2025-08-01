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

package org.orekit.utils;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeInterval;

/**
 * Interface for bounded PV coordinates providers.
 *
 * @author Romain Serra
 * @since 13.1
 * @see PVCoordinatesProvider
 */
public interface BoundedPVCoordinatesProvider extends PVCoordinatesProvider {

    /** Get the first date of the range.
     * @return the first date of the range
     */
    AbsoluteDate getMinDate();

    /** Get the last date of the range.
     * @return the last date of the range
     */
    AbsoluteDate getMaxDate();

    /**
     * Bound a given coordinates provider.
     * @param interval time interval
     * @param provider input provider
     * @return bounded provider
     */
    static BoundedPVCoordinatesProvider of( final PVCoordinatesProvider provider, final TimeInterval interval) {
        return new BoundedPVCoordinatesProvider() {
            @Override
            public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
                return provider.getPVCoordinates(date, frame);
            }

            @Override
            public Vector3D getPosition(final AbsoluteDate date, final Frame frame) {
                return provider.getPosition(date, frame);
            }

            @Override
            public AbsoluteDate getMinDate() {
                return interval.getStartDate();
            }

            @Override
            public AbsoluteDate getMaxDate() {
                return interval.getEndDate();
            }
        };
    }
}
