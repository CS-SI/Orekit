/* Copyright 2002-2025 CS GROUP
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

import org.hipparchus.util.FastMath;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeShiftable;
import org.orekit.time.TimeStamped;

/**
 * Implements a time-stamped {@link GeodeticPoint}.
 * @author Jérôme Tabeaud
 * @since 13.1
 */
public class TimeStampedGeodeticPoint extends GeodeticPoint implements TimeStamped, TimeShiftable<TimeStampedGeodeticPoint> {
    /**
     * Date at which the {@link GeodeticPoint} is set.
     */
    private final AbsoluteDate date;

    /**
     * Build a new instance from geodetic coordinates.
     *
     * @param date      date of the point
     * @param latitude  geodetic latitude (rad)
     * @param longitude geodetic longitude (rad)
     * @param altitude  altitude above ellipsoid (m)
     */
    public TimeStampedGeodeticPoint(final AbsoluteDate date, final double latitude, final double longitude,
                                    final double altitude) {
        super(latitude, longitude, altitude);
        this.date = date;
    }

    /**
     * Build a new instance from a {@link GeodeticPoint}.
     *
     * @param date      date of the point
     * @param point     geodetic point
     */
    public TimeStampedGeodeticPoint(final AbsoluteDate date, final GeodeticPoint point) {
        this(date, point.getLatitude(), point.getLongitude(), point.getAltitude());
    }

    @Override
    public AbsoluteDate getDate() {
        return date;
    }

    @Override
    public String toString() {
        return "{" +
                "date: " + date +
                ", lat: " + FastMath.toDegrees(getLatitude()) +
                " deg, lon: " + FastMath.toDegrees(getLongitude()) +
                " deg, alt: " + getAltitude() +
                "}";
    }

    @Override
    public boolean equals(final Object object) {
        if (object instanceof TimeStampedGeodeticPoint) {
            final TimeStampedGeodeticPoint other = (TimeStampedGeodeticPoint) object;
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
    public TimeStampedGeodeticPoint shiftedBy(final double dt) {
        return new TimeStampedGeodeticPoint(date.shiftedBy(dt), getLatitude(), getLongitude(), getAltitude());
    }
}
