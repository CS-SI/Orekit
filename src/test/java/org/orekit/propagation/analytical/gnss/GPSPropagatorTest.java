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
