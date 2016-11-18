/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.gnss;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.gnss.GPSPropagator;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.analytical.tle.TLESeries;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.ElevationMask;
import org.orekit.utils.IERSConventions;


public class DOPComputerTest {

    private static OneAxisEllipsoid earth;
    private static GeodeticPoint location;

    @BeforeClass
    public static void setUpBeforeClass() throws OrekitException {
        // Sets the root of data to read
        Utils.setDataRoot("gnss");
        // Defines the Earth shape
        earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                     Constants.WGS84_EARTH_FLATTENING,
                                     FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Defines the location where to compute the DOP
        location = new GeodeticPoint(FastMath.toRadians(43.6), FastMath.toRadians(1.45), 0.);
    }

    @Test
    public void testBasicCompute() throws OrekitException {

        // Creates the computer
        final DOPComputer computer = DOPComputer.create(earth, location);
        Assert.assertEquals(DOPComputer.DOP_MIN_ELEVATION, computer.getMinElevation(), 0.);
        Assert.assertNull(computer.getElevationMask());

        // Defines the computation date
        final AbsoluteDate date = new AbsoluteDate(2016, 3, 31, 2, 0, 0.,
                                                   TimeScalesFactory.getUTC());

        // Computes the DOP with all the SV from the GPS constellation
        final DOP dop = computer.compute(date, getGpsPropagators());

        // Checks: expected values come from Trimble Planning software
        Assert.assertEquals(11, dop.getGnssNb());
        Assert.assertEquals(location, dop.getLocation());
        Assert.assertEquals(date, dop.getDate());
        Assert.assertEquals(1.53, dop.getGdop(), 0.01);
        Assert.assertEquals(0.71, dop.getTdop(), 0.01);
        Assert.assertEquals(1.35, dop.getPdop(), 0.01);
        Assert.assertEquals(0.84, dop.getHdop(), 0.01);
        Assert.assertEquals(1.06, dop.getVdop(), 0.01);
    }

    @Test
    public void testComputeWithMinElevation() throws OrekitException {

        // Creates the computer
        final DOPComputer computer = DOPComputer.create(earth, location)
                                     .withMinElevation(FastMath.toRadians(10.));
        Assert.assertEquals(FastMath.toRadians(10.), computer.getMinElevation(), 0.);
        Assert.assertNull(computer.getElevationMask());

        // Defines the computation date
        final AbsoluteDate date = new AbsoluteDate(2016, 3, 31, 13, 0, 0.,
                                                   TimeScalesFactory.getUTC());

        // Computes the DOP with all the SV from the GPS constellation
        final DOP dop = computer.compute(date, getGpsPropagators());

        // Checks: expected values come from Trimble Planning software
        Assert.assertEquals(10, dop.getGnssNb());
        Assert.assertEquals(location, dop.getLocation());
        Assert.assertEquals(date, dop.getDate());
        Assert.assertEquals(1.94, dop.getGdop(), 0.01);
        Assert.assertEquals(0.89, dop.getTdop(), 0.01);
        Assert.assertEquals(1.72, dop.getPdop(), 0.01);
        Assert.assertEquals(0.82, dop.getHdop(), 0.01);
        Assert.assertEquals(1.51, dop.getVdop(), 0.01);
    }

    @Test
    public void testComputeWithElevationMask() throws OrekitException {

        // Creates the computer
        final DOPComputer computer = DOPComputer.create(earth, location).withElevationMask(getMask());
        Assert.assertEquals(DOPComputer.DOP_MIN_ELEVATION, computer.getMinElevation(), 0.);
        Assert.assertNotNull(computer.getElevationMask());

        // Defines the computation date
        final AbsoluteDate date = new AbsoluteDate(2016, 3, 31, 7, 0, 0.,
                                                   TimeScalesFactory.getUTC());

        // Computes the DOP with all the SV from the GPS constellation
        final DOP dop = computer.compute(date, getGpsPropagators());

        // Checks: expected values come from Trimble Planning software
        Assert.assertEquals(6, dop.getGnssNb());
        Assert.assertEquals(location, dop.getLocation());
        Assert.assertEquals(date, dop.getDate());
        Assert.assertEquals(3.26, dop.getGdop(), 0.01);
        Assert.assertEquals(1.79, dop.getTdop(), 0.01);
        Assert.assertEquals(2.72, dop.getPdop(), 0.01);
        Assert.assertEquals(1.29, dop.getHdop(), 0.01);
        Assert.assertEquals(2.40, dop.getVdop(), 0.01);
    }

    @Test
    public void testNoDOPComputed() throws OrekitException {

        // Creates the computer
        final DOPComputer computer = DOPComputer.create(earth, location).withElevationMask(getMask());
        Assert.assertEquals(DOPComputer.DOP_MIN_ELEVATION, computer.getMinElevation(), 0.);
        Assert.assertNotNull(computer.getElevationMask());

        // Defines the computation date
        final AbsoluteDate date = new AbsoluteDate(2016, 3, 31, 10, 0, 0.,
                                                   TimeScalesFactory.getUTC());

        // Computes the DOP with all the SV from the GPS constellation
        final DOP dop = computer.compute(date, getGpsPropagators());

        // Checks: comparison is made with results from Trimble Planning software
        Assert.assertEquals(3, dop.getGnssNb());
        Assert.assertEquals(location, dop.getLocation());
        Assert.assertEquals(date, dop.getDate());
        Assert.assertTrue(Double.isNaN(dop.getGdop()));
        Assert.assertTrue(Double.isNaN(dop.getHdop()));
        Assert.assertTrue(Double.isNaN(dop.getPdop()));
        Assert.assertTrue(Double.isNaN(dop.getTdop()));
        Assert.assertTrue(Double.isNaN(dop.getVdop()));
    }

    @Test
    public void testComputeFromTLE() throws OrekitException {

        // Creates the computer
        final DOPComputer computer = DOPComputer.create(earth, location);
        Assert.assertEquals(DOPComputer.DOP_MIN_ELEVATION, computer.getMinElevation(), 0.);
        Assert.assertNull(computer.getElevationMask());

        // Defines the computation date
        final AbsoluteDate date = new AbsoluteDate(2016, 3, 27, 12, 0, 0.,
                                                   TimeScalesFactory.getUTC());

        // Computes the DOP with all the SV from the GPS constellation
        final DOP dop = computer.compute(date, getTlePropagators());

        // Checks
        Assert.assertEquals(11, dop.getGnssNb());
        Assert.assertEquals(location, dop.getLocation());
        Assert.assertEquals(date, dop.getDate());
        Assert.assertEquals(1.40, dop.getGdop(), 0.01);
        Assert.assertEquals(0.81, dop.getHdop(), 0.01);
        Assert.assertEquals(1.28, dop.getPdop(), 0.01);
        Assert.assertEquals(0.56, dop.getTdop(), 0.01);
        Assert.assertEquals(1.00, dop.getVdop(), 0.01);
    }

    @Test(expected=OrekitException.class)
    public void testNotEnoughSV() throws OrekitException {

        // Reads the TLEs for 3 SV from the GPS constellation ...
        TLESeries series = new TLESeries("^3gps\\.txt$", true);
        // .. and gets a list of TLEPropagators from each TLE read
        List<Propagator> gps = new ArrayList<Propagator>();
        for (int i: series.getAvailableSatelliteNumbers()) {
            series.loadTLEData(i);
            gps.add(TLEPropagator.selectExtrapolator(series.getFirst()));
        }
        Assert.assertEquals(3, gps.size());

        // Creates the computer
        final DOPComputer computer = DOPComputer.create(earth, location);

        // Defines the computation date
        final AbsoluteDate date = new AbsoluteDate(2016, 3, 27, 12, 0, 0.,
                                                   TimeScalesFactory.getUTC());
        // Computes the DOP with all the SV from the GPS constellation
        computer.compute(date, gps);
    }

    private List<Propagator> getGpsPropagators() throws OrekitException {
        // Gets the GPS almanacs from the Yuma file
        final YUMAParser reader = new YUMAParser(null);
        reader.loadData();
        final List<GPSAlmanac> almanacs = reader.getAlmanacs();

        // Creates the GPS propagators from the almanacs
        final List<Propagator> propagators = new ArrayList<Propagator>();
        for (GPSAlmanac almanac: almanacs) {
            propagators.add(new GPSPropagator.Builder(almanac).build());
        }
        return propagators;
    }

    private List<Propagator> getTlePropagators() throws OrekitException {
        // Reads the TLEs for all the SV from the GPS constellation ...
        TLESeries series = new TLESeries("^gps-week-862\\.txt$", true);
        // .. and gets a list of TLEPropagators from each TLE read
        List<Propagator> propagators = new ArrayList<Propagator>();
        for (int i: series.getAvailableSatelliteNumbers()) {
            series.loadTLEData(i);
            propagators.add(TLEPropagator.selectExtrapolator(series.getFirst()));
        }
        return propagators;
    }

    private ElevationMask getMask() {
        final double [][] mask = {
            {FastMath.toRadians(0.),   FastMath.toRadians(5.00)},
            {FastMath.toRadians(45.),  FastMath.toRadians(50.00)},
            {FastMath.toRadians(90.),  FastMath.toRadians(5.00)},
            {FastMath.toRadians(135.), FastMath.toRadians(50.00)},
            {FastMath.toRadians(180.), FastMath.toRadians(5.00)},
            {FastMath.toRadians(225.), FastMath.toRadians(50.00)},
            {FastMath.toRadians(270.), FastMath.toRadians(5.00)},
            {FastMath.toRadians(315.), FastMath.toRadians(50.00)}
        };
        return new ElevationMask(mask);
    }
}
