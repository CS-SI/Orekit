package org.orekit.propagation.analytical.gnss;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.gnss.GPSAlmanac;
import org.orekit.gnss.SEMParser;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;


public class GPSPropagatorTest {

    private static GPSAlmanac almanac;

    @BeforeClass
    public static void setUpBeforeClass() throws OrekitException {
        Utils.setDataRoot("regular-data:gnss");
        // Get the parser to read a SEM file
        SEMParser reader = new SEMParser(null);
        // Reads the SEM file
        reader.loadData();
        // Gets the first SEM almanac
        almanac = reader.getAlmanacs().get(0);
    }

    @Test
    public void testGPSCycle() throws OrekitException {
        // Builds the GPSPropagator from the almanac
        final GPSPropagator propagator = new GPSPropagator.Builder(almanac).build();
        // Propagate at the GPS date and one GPS cycle later
        final AbsoluteDate date0 = almanac.getDate();
        final Vector3D p0 = propagator.progate(date0);
        final double gpsCycleDuration = GPSOrbitalElements.GPS_WEEK_IN_SECONDS * GPSOrbitalElements.GPS_WEEK_NB;
        final AbsoluteDate date1 = date0.shiftedBy(gpsCycleDuration);
        final Vector3D p1 = propagator.progate(date1);

        // Checks
        Assert.assertEquals(0., p0.distance(p1), 0.);
    }

    @Test
    public void testFrames() throws OrekitException {
        // Builds the GPSPropagator from the almanac
        final GPSPropagator propagator = new GPSPropagator.Builder(almanac).build();
        // Defines some date
        final AbsoluteDate date = new AbsoluteDate(2016, 3, 3, 12, 0, 0., TimeScalesFactory.getUTC());
        // Propagates at the date in the ECEF
        final Vector3D pos = propagator.progate(date);
        // Get PVCoordinates at the date in the ECEF
        final PVCoordinates pv0 = propagator.getPVCoordinates(date);
        // Get PVCoordinates at the date in the ECEF
        final PVCoordinates pv1 = propagator.getPVCoordinates(date, propagator.getECEF());

        // Checks
        Assert.assertEquals(0., pos.distance(pv0.getPosition()), 0.);
        Assert.assertEquals(0., pos.distance(pv1.getPosition()), 1.1e-8);
    }

}
