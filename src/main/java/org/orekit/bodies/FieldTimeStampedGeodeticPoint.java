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
package org.orekit.bodies;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeStamped;

/**
 * Implements a time-stamped {@link FieldGeodeticPoint}.
 * @author Romain Serra
 * @since 13.1
 */
public class FieldTimeStampedGeodeticPoint<T extends CalculusFieldElement<T>> extends FieldGeodeticPoint<T>
        implements FieldTimeStamped<T> {
    /**
     * Date at which the {@link FieldGeodeticPoint} is set.
     */
    private final FieldAbsoluteDate<T> date;

    /**
     * Build a new instance from geodetic coordinates.
     *
     * @param date      date of the point
     * @param latitude  geodetic latitude (rad)
     * @param longitude geodetic longitude (rad)
     * @param altitude  altitude above ellipsoid (m)
     */
    public FieldTimeStampedGeodeticPoint(final FieldAbsoluteDate<T> date, final T latitude, final T longitude,
                                         final T altitude) {
        super(latitude, longitude, altitude);
        this.date = date;
    }

    /**
     * Build a new instance from a {@link FieldGeodeticPoint}.
     *
     * @param date      date of the point
     * @param point     geodetic point
     */
    public FieldTimeStampedGeodeticPoint(final FieldAbsoluteDate<T> date, final FieldGeodeticPoint<T> point) {
        this(date, point.getLatitude(), point.getLongitude(), point.getAltitude());
    }

    /**
     * Build a new instance from a {@link TimeStampedGeodeticPoint}.
     *
     * @param field field
     * @param timeStampedGeodeticPoint non-Field point
     */
    public FieldTimeStampedGeodeticPoint(final Field<T> field, final TimeStampedGeodeticPoint timeStampedGeodeticPoint) {
        this(new FieldAbsoluteDate<>(field, timeStampedGeodeticPoint.getDate()), new FieldGeodeticPoint<>(field, timeStampedGeodeticPoint));
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(final Object object) {
        if (object instanceof FieldTimeStampedGeodeticPoint<?>) {
            final FieldTimeStampedGeodeticPoint<T> other = (FieldTimeStampedGeodeticPoint<T>) object;
            return other.date.isEqualTo(date) && super.equals(other);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return date.hashCode() + super.hashCode();
    }

    @Override
    public FieldAbsoluteDate<T> getDate() {
        return date;
    }
}
