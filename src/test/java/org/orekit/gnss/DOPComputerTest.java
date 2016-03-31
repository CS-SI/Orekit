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

import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.analytical.tle.TLESeries;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;


public class DOPComputerTest {
    
    private static List<Propagator> propagators;
    private static OneAxisEllipsoid earth;
    private static GeodeticPoint location;
    private static AbsoluteDate date;

    @BeforeClass
    public static void setUpBeforeClass() throws OrekitException {
        // Sets the root of data to read
        Utils.setDataRoot("regular-data:gnss");
        // Reads the TLEs for all the SV from the GPS constellation ...
        TLESeries series = new TLESeries("^gps-ops\\.txt$", true);
        // .. and gets a list of TLEPropagators from each TLE read
        propagators = new ArrayList<Propagator>();
        for (int i: series.getAvailableSatelliteNumbers()) {
            series.loadTLEData(i);
            propagators.add(TLEPropagator.selectExtrapolator(series.getFirst()));
        }
        // Defines the Earth shape
        earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                     Constants.WGS84_EARTH_FLATTENING,
                                     FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        // Defines the location where to compute the DOP
        location = new GeodeticPoint(FastMath.toRadians(43.6), FastMath.toRadians(1.45), 0.);
        // Defines the computation date
        date = new AbsoluteDate(2016, 3, 27, 12, 0, 0., TimeScalesFactory.getUTC());
    }

    @Test
    public void testSimpleCompute() throws OrekitException {

        // Creates the computer
        final DOPComputer computer = DOPComputer.create(earth, location);

        // Computes the DOP with all the SV from the GPS constellation 
        final DOP dop = computer.compute(date, propagators);

        Assert.assertEquals(11, dop.getGnssNb());
        Assert.assertEquals(location, dop.getLocation());
        Assert.assertEquals(date, dop.getDate());
        Assert.assertEquals(1.40, dop.getGdop(), 0.01);
        Assert.assertEquals(0.81, dop.getHdop(), 0.01);
        Assert.assertEquals(1.28, dop.getPdop(), 0.01);
        Assert.assertEquals(0.56, dop.getTdop(), 0.01);
        Assert.assertEquals(0.99, dop.getVdop(), 0.01);
    }

    @Test
    public void testComputeWithConstantElevation() throws OrekitException {

        // Creates the computer
        final DOPComputer computer = DOPComputer.create(earth, location).withMinElevation(FastMath.toRadians(5.));

        // Computes the DOP with all the SV from the GPS constellation 
        final DOP dop = computer.compute(date, propagators);

        Assert.assertEquals(9, dop.getGnssNb());
        Assert.assertEquals(location, dop.getLocation());
        Assert.assertEquals(date, dop.getDate());
        Assert.assertEquals(1.83, dop.getGdop(), 0.01);
        Assert.assertEquals(0.91, dop.getHdop(), 0.01);
        Assert.assertEquals(1.63, dop.getPdop(), 0.01);
        Assert.assertEquals(0.83, dop.getTdop(), 0.01);
        Assert.assertEquals(1.35, dop.getVdop(), 0.01);
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

        // Computes the DOP with all the SV from the GPS constellation 
        computer.compute(date, gps);
    }

}
