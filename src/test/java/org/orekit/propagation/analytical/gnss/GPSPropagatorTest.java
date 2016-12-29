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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.gnss.GPSAlmanac;
import org.orekit.gnss.SEMParser;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.analytical.tle.TLESeries;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


public class GPSPropagatorTest {

    private static List<GPSAlmanac> almanacs;

    @BeforeClass
    public static void setUpBeforeClass() throws OrekitException {
        Utils.setDataRoot("gnss");
        // Get the parser to read a SEM file
        SEMParser reader = new SEMParser(null);
        // Reads the SEM file
        reader.loadData();
        // Gets the first SEM almanac
        almanacs = reader.getAlmanacs();
    }

    @Test
    public void testGPSCycle() throws OrekitException {
        // Builds the GPSPropagator from the almanac
        final GPSPropagator propagator = new GPSPropagator.Builder(almanacs.get(0)).build();
        // Propagate at the GPS date and one GPS cycle later
        final AbsoluteDate date0 = almanacs.get(0).getDate();
        final Vector3D p0 = propagator.propagateInEcef(date0).getPosition();
        final double gpsCycleDuration = GPSOrbitalElements.GPS_WEEK_IN_SECONDS * GPSOrbitalElements.GPS_WEEK_NB;
        final AbsoluteDate date1 = date0.shiftedBy(gpsCycleDuration);
        final Vector3D p1 = propagator.propagateInEcef(date1).getPosition();

        // Checks
        Assert.assertEquals(0., p0.distance(p1), 0.);
    }

    @Test
    public void testFrames() throws OrekitException {
        // Builds the GPSPropagator from the almanac
        final GPSPropagator propagator = new GPSPropagator.Builder(almanacs.get(0)).build();
        // Defines some date
        final AbsoluteDate date = new AbsoluteDate(2016, 3, 3, 12, 0, 0., TimeScalesFactory.getUTC());
        // Get PVCoordinates at the date in the ECEF
        final PVCoordinates pv0 = propagator.propagateInEcef(date);
        // Get PVCoordinates at the date in the ECEF
        final PVCoordinates pv1 = propagator.getPVCoordinates(date, propagator.getECEF());

        // Checks
        Assert.assertEquals(0., pv0.getPosition().distance(pv1.getPosition()), 3.3e-8);
        Assert.assertEquals(0., pv0.getVelocity().distance(pv1.getVelocity()), 3.9e-12);
    }

    @Test
    public void testTLE() throws OrekitException {

        List<GPSPropagator> gpsPropagators = new ArrayList<GPSPropagator>();
        for (final GPSAlmanac almanac : almanacs) {
            gpsPropagators.add(new GPSPropagator.Builder(almanac).build());
        }

        // the following map corresponds to the constellation status
        // in early 2016, compliant with the gps-week-862.txt TLE file
        final Map<Integer, Integer> prnToSatNumber = new HashMap<Integer, Integer>();
        prnToSatNumber.put( 1, 37753);
        prnToSatNumber.put( 2, 28474);
        prnToSatNumber.put( 3, 40294);
        prnToSatNumber.put( 4, 34661);
        prnToSatNumber.put( 5, 35752);
        prnToSatNumber.put( 6, 39741);
        prnToSatNumber.put( 7, 32711);
        prnToSatNumber.put( 8, 40730);
        prnToSatNumber.put( 9, 40105);
        prnToSatNumber.put(10, 41019);
        prnToSatNumber.put(11, 25933);
        prnToSatNumber.put(12, 29601);
        prnToSatNumber.put(13, 24876);
        prnToSatNumber.put(14, 26605);
        prnToSatNumber.put(15, 32260);
        prnToSatNumber.put(16, 27663);
        prnToSatNumber.put(17, 28874);
        prnToSatNumber.put(18, 26690);
        prnToSatNumber.put(19, 28190);
        prnToSatNumber.put(20, 26360);
        prnToSatNumber.put(21, 27704);
        prnToSatNumber.put(22, 28129);
        prnToSatNumber.put(23, 28361);
        prnToSatNumber.put(24, 38833);
        prnToSatNumber.put(25, 36585);
        prnToSatNumber.put(26, 40534);
        prnToSatNumber.put(27, 39166);
        prnToSatNumber.put(28, 26407);
        prnToSatNumber.put(29, 32384);
        prnToSatNumber.put(30, 39533);
        prnToSatNumber.put(31, 29486);
        prnToSatNumber.put(32, 41328);
        TLESeries series = new TLESeries("^gps-week-862\\.txt$", true);

        for (final GPSPropagator gpsPropagator : gpsPropagators) {
            final int prn = gpsPropagator.getGPSOrbitalElements().getPRN();
            final int satNumber = prnToSatNumber.get(prn);
            series.loadTLEData(satNumber);
            TLE tle = series.getFirst();
            TLEPropagator tlePropagator = TLEPropagator.selectExtrapolator(tle);
            for (double dt = 0; dt < Constants.JULIAN_DAY; dt += 600) {
                final AbsoluteDate date = tlePropagator.getInitialState().getDate().shiftedBy(dt);
                final PVCoordinates gpsPV = gpsPropagator.getPVCoordinates(date, gpsPropagator.getECI());
                final PVCoordinates tlePV = tlePropagator.getPVCoordinates(date, gpsPropagator.getECI());
                Assert.assertEquals(0.0,
                                    Vector3D.distance(gpsPV.getPosition(), tlePV.getPosition()),
                                    8400.0);
            }
        }
    }

    @Test
    public void testDerivativesConsistency() throws OrekitException {

        final Frame eme2000 = FramesFactory.getEME2000();
        double errorP = 0;
        double errorV = 0;
        double errorA = 0;
        for (final GPSAlmanac almanac : almanacs) {
            GPSPropagator propagator = new GPSPropagator.Builder(almanac).build();
            GPSOrbitalElements elements = propagator.getGPSOrbitalElements();
            AbsoluteDate t0 = AbsoluteDate.createGPSDate(elements.getWeek(),
                                                           0.001 * elements.getTime());
            for (double dt = 0; dt < Constants.JULIAN_DAY; dt += 600) {
                final AbsoluteDate central = t0.shiftedBy(dt);
                final PVCoordinates pv = propagator.getPVCoordinates(central, eme2000);
                final double h = 10.0;
                List<TimeStampedPVCoordinates> sample = new ArrayList<TimeStampedPVCoordinates>();
                for (int i = -3; i <= 3; ++i) {
                    sample.add(propagator.getPVCoordinates(central.shiftedBy(i * h), eme2000));
                }
                final PVCoordinates interpolated =
                                TimeStampedPVCoordinates.interpolate(central,
                                                                     CartesianDerivativesFilter.USE_P,
                                                                     sample);
                errorP = FastMath.max(errorP, Vector3D.distance(pv.getPosition(), interpolated.getPosition()));
                errorV = FastMath.max(errorV, Vector3D.distance(pv.getVelocity(), interpolated.getVelocity()));
                errorA = FastMath.max(errorA, Vector3D.distance(pv.getAcceleration(), interpolated.getAcceleration()));
            }
        }
        Assert.assertEquals(0.0, errorP, 3.8e-9);
        Assert.assertEquals(0.0, errorV, 3.5e-8);
        Assert.assertEquals(0.0, errorA, 1.1e-8);

    }

}
