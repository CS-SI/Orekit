package org.orekit.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.TimeStampedGeodeticPoint;
import org.orekit.time.AbsoluteDate;

public class TimeStampedGeodeticPointTest {
    @Test
    public void testFullConstructor() {
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final TimeStampedGeodeticPoint geo = new TimeStampedGeodeticPoint(date, 1.0, 2.0, 3.0);
        Assertions.assertEquals(date, geo.getDate());
        Assertions.assertEquals(1.0, geo.getLatitude());
        Assertions.assertEquals(2.0, geo.getLongitude());
        Assertions.assertEquals(3.0, geo.getAltitude());
    }

    @Test
    public void testGeodeticPointConstructor() {
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final GeodeticPoint geo = new GeodeticPoint(1.0, 2.0, 3.0);
        final TimeStampedGeodeticPoint timedGeo = new TimeStampedGeodeticPoint(date, geo);
        Assertions.assertEquals(date, timedGeo.getDate());
        Assertions.assertEquals(geo.getLatitude(), timedGeo.getLatitude());
        Assertions.assertEquals(geo.getLongitude(), timedGeo.getLongitude());
        Assertions.assertEquals(geo.getAltitude(), timedGeo.getAltitude());
    }

    @Test
    public void testShiftedBy() {
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final GeodeticPoint geo = new GeodeticPoint(1.0, 2.0, 3.0);
        final TimeStampedGeodeticPoint timedGeo = new TimeStampedGeodeticPoint(date, geo);

        // Shift point in time
        final double dt = 3600;
        final AbsoluteDate newDate = date.shiftedBy(dt);
        final TimeStampedGeodeticPoint timedGeoShifted = timedGeo.shiftedBy(dt);

        // Check that shifted point has not moved but date has been updated
        Assertions.assertEquals(newDate, timedGeoShifted.getDate());
        Assertions.assertEquals(geo.getLatitude(), timedGeoShifted.getLatitude());
        Assertions.assertEquals(geo.getLongitude(), timedGeoShifted.getLongitude());
        Assertions.assertEquals(geo.getAltitude(), timedGeoShifted.getAltitude());
    }

    @Test
    public void testToString() {
        Utils.setDataRoot("regular-data");
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final TimeStampedGeodeticPoint geo = new TimeStampedGeodeticPoint(date, 1.0, 2.0, 3.0);
        Assertions.assertEquals("{date: 2000-01-01T11:58:55.816Z, lat: 57.29577951308232 deg, lon: 114.59155902616465 deg, alt: 3.0}", geo.toString());
    }
}
