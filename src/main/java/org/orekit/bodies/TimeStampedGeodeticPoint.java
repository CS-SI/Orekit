package org.orekit.bodies;

import org.hipparchus.util.FastMath;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeShiftable;
import org.orekit.time.TimeStamped;

/**
 * Implements a time-stamped {@link GeodeticPoint}.
 */
public class TimeStampedGeodeticPoint extends GeodeticPoint implements TimeStamped, TimeShiftable<TimeStampedGeodeticPoint> {
    /**
     * Date at which the {@link GeodeticPoint} is set.
     */
    final AbsoluteDate date;

    /**
     * Build a new instance from geodetic coordinates.
     *
     * @param date      date of the point
     * @param latitude  geodetic latitude (rad)
     * @param longitude geodetic longitude (rad)
     * @param altitude  altitude above ellipsoid (m)
     */
    public TimeStampedGeodeticPoint(final AbsoluteDate date, final double latitude, final double longitude, final double altitude) {
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
        super(point.getLatitude(), point.getLongitude(), point.getAltitude());
        this.date = date;
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
    public TimeStampedGeodeticPoint shiftedBy(double dt) {
        return new TimeStampedGeodeticPoint(date.shiftedBy(dt), getLatitude(), getLongitude(), getAltitude());
    }
}
