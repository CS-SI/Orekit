/* Copyright 2002-2020 CS GROUP
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
package org.orekit.files.general;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.InertialProvider;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.AEMParser;
import org.orekit.files.ccsds.AEMWriter;
import org.orekit.files.general.AttitudeEphemerisFile.AttitudeEphemerisSegment;
import org.orekit.files.general.OrekitAttitudeEphemerisFile.OrekitSatelliteAttitudeEphemeris;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.TimeStampedAngularCoordinates;

public class OrekitAttitudeEphemerisFileTest {

    @Before
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testGetSatellites() {
        final String id1 = "ID1";
        final String id2 = "ID2";
        OrekitAttitudeEphemerisFile file = new OrekitAttitudeEphemerisFile();
        OrekitSatelliteAttitudeEphemeris ephem1 = file.addSatellite(id1);
        assertNotNull(ephem1);
        OrekitSatelliteAttitudeEphemeris ephem2 = file.addSatellite(id2);
        assertNotNull(ephem2);
    }

    @Test
    public void testWritingToAEM() throws IOException {
        final double quaternionTolerance = 1e-5;
        final String satId = "SATELLITE1";
        final double sma = 10000000;
        final double inc = Math.toRadians(45.0);
        final double ecc = 0.1;
        final double raan = 0.0;
        final double pa = 0.0;
        final double ta = 0.0;
        final AbsoluteDate date = new AbsoluteDate();
        final Frame frame = FramesFactory.getEME2000();
        final CelestialBody body = CelestialBodyFactory.getEarth();
        final double mu = body.getGM();
        KeplerianOrbit initialOrbit = new KeplerianOrbit(sma, ecc, inc, pa, raan, ta, PositionAngle.TRUE,
                                                         frame, date, mu);

        // Initialize a Keplerian propagator with an Inertial attitude provider
        // It is expected that all attitude data lines will have the same value
        final Rotation refRot = new Rotation(0.72501, -0.64585, 0.018542, -0.23854, false);
        AttitudeProvider inertialPointing = new InertialProvider(refRot);
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, inertialPointing);

        final double propagationDurationSeconds = 1200.0;
        final double stepSizeSeconds = 60.0;
        List<SpacecraftState> states = new ArrayList<SpacecraftState>();

        for (double dt = 0.0; dt < propagationDurationSeconds; dt += stepSizeSeconds) {
            states.add(propagator.propagate(date.shiftedBy(dt)));
        }

        OrekitAttitudeEphemerisFile ephemerisFile = new OrekitAttitudeEphemerisFile();
        OrekitSatelliteAttitudeEphemeris satellite = ephemerisFile.addSatellite(satId);
        satellite.addNewSegment(states);

        // Test of all getters for OrekitSatelliteAttitudeEphemeris
        assertEquals(satId, satellite.getId());
        assertEquals(0.0, states.get(0).getDate().durationFrom(satellite.getStart()), 1.0e-15);
        assertEquals(0.0, states.get(states.size() - 1).getDate().durationFrom(satellite.getStop()), 1.0e-15);

        // Test of all getters for OrekitAttitudeEphemerisSegment
        AttitudeEphemerisSegment segment = satellite.getSegments().get(0);
        assertEquals(OrekitSatelliteAttitudeEphemeris.DEFAULT_INTERPOLATION_METHOD, segment.getInterpolationMethod());
        assertEquals(OrekitSatelliteAttitudeEphemeris.DEFAULT_INTERPOLATION_SIZE, segment.getInterpolationSamples());
        assertEquals(OrekitSatelliteAttitudeEphemeris.DEFAULT_ATTITUDE_TYPE, segment.getAttitudeType());
        assertEquals(OrekitSatelliteAttitudeEphemeris.DEFAULT_IS_FIRST, segment.isFirst());
        assertEquals(OrekitSatelliteAttitudeEphemeris.DEFAULT_ROTATION_ORDER, segment.getRotationOrder());
        assertEquals(OrekitSatelliteAttitudeEphemeris.DEFAULT_REF_FRAME_A, segment.getRefFrameAString());
        assertEquals(OrekitSatelliteAttitudeEphemeris.DEFAULT_REF_FRAME_B, segment.getRefFrameBString());
        assertEquals(OrekitSatelliteAttitudeEphemeris.DEFAULT_ATTITUDE_DIR, segment.getAttitudeDirection());
        assertEquals(DataContext.getDefault().getCelestialBodies().getEarth().getName(), segment.getFrameCenterString());
        assertEquals(DataContext.getDefault().getTimeScales().getUTC().getName(), segment.getTimeScaleString());
        assertEquals(DataContext.getDefault().getTimeScales().getUTC(), segment.getTimeScale());
        assertEquals(0.0, states.get(0).getDate().durationFrom(segment.getStart()), 1.0e-15);
        assertEquals(0.0, states.get(states.size() - 1).getDate().durationFrom(segment.getStop()), 1.0e-15);
        Assert.assertEquals(AngularDerivativesFilter.USE_R, segment.getAvailableDerivatives());

        // Verify attitude
        final Attitude attitude = segment.getAttitudeProvider().getAttitude(initialOrbit, date, frame);
        Assert.assertEquals(frame, attitude.getReferenceFrame());
        Assert.assertEquals(refRot.getQ0(), attitude.getRotation().getQ0(), quaternionTolerance);
        Assert.assertEquals(refRot.getQ1(), attitude.getRotation().getQ1(), quaternionTolerance);
        Assert.assertEquals(refRot.getQ2(), attitude.getRotation().getQ2(), quaternionTolerance);
        Assert.assertEquals(refRot.getQ3(), attitude.getRotation().getQ3(), quaternionTolerance);

        String tempAemFile = Files.createTempFile("OrekitAttitudeEphemerisFileTest", ".aem").toString();
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(tempAemFile), StandardCharsets.UTF_8)) {
            new AEMWriter().write(writer, ephemerisFile);
        }

        AttitudeEphemerisFile ephemerisFromFile = new AEMParser().parse(tempAemFile);
        Files.delete(Paths.get(tempAemFile));
        
        segment = ephemerisFromFile.getSatellites().get(satId).getSegments().get(0);
        assertEquals(states.get(0).getDate(), segment.getStart());
        assertEquals(states.get(states.size() - 1).getDate(), segment.getStop());
        assertEquals(states.size(), segment.getAngularCoordinates().size());
        assertEquals(body.getName().toUpperCase(), segment.getFrameCenterString());
        for (int i = 0; i < states.size(); i++) {
            TimeStampedAngularCoordinates expected = states.get(i).getAttitude().getOrientation();
            TimeStampedAngularCoordinates actual = segment.getAngularCoordinates().get(i);
            assertEquals(expected.getDate(), actual.getDate());
            assertEquals(0.0, Rotation.distance(refRot, actual.getRotation()), quaternionTolerance);
        }

    }

    @Test
    public void testNoStates() {

        // Satellite ID
        final String satId = "SATELLITE1";

        // Create an empty list of states
        List<SpacecraftState> states = new ArrayList<SpacecraftState>();

        // Create a new satellite attitude ephemeris
        OrekitAttitudeEphemerisFile ephemerisFile = new OrekitAttitudeEphemerisFile();
        OrekitSatelliteAttitudeEphemeris satellite = ephemerisFile.addSatellite(satId);

        // Try to add a new segment
        try {
            satellite.addNewSegment(states);
        } catch (OrekitIllegalArgumentException oiae) {
            Assert.assertEquals(OrekitMessages.NULL_ARGUMENT, oiae.getSpecifier());
        }


    }

    @Test
    public void testNoEnoughDataForInterpolation() {

        // Create a spacecraft state
        final String satId = "SATELLITE1";
        final double sma = 10000000;
        final double inc = Math.toRadians(45.0);
        final double ecc = 0.1;
        final double raan = 0.0;
        final double pa = 0.0;
        final double ta = 0.0;
        final AbsoluteDate date = new AbsoluteDate();
        final Frame frame = FramesFactory.getEME2000();
        final CelestialBody body = CelestialBodyFactory.getEarth();
        final double mu = body.getGM();
        KeplerianOrbit initialOrbit = new KeplerianOrbit(sma, ecc, inc, pa, raan, ta, PositionAngle.TRUE,
                                                         frame, date, mu);
        SpacecraftState state = new SpacecraftState(initialOrbit);

        // Add the state to the list of spacecraft states
        List<SpacecraftState> states = new ArrayList<SpacecraftState>();
        states.add(state);

        // Create a new satellite attitude ephemeris
        OrekitAttitudeEphemerisFile ephemerisFile = new OrekitAttitudeEphemerisFile();
        OrekitSatelliteAttitudeEphemeris satellite = ephemerisFile.addSatellite(satId);

        // Try to add a new segment
        try {
            satellite.addNewSegment(states, "LINEAR", 1);
        } catch (OrekitIllegalArgumentException oiae) {
            Assert.assertEquals(OrekitMessages.NOT_ENOUGH_DATA_FOR_INTERPOLATION, oiae.getSpecifier());
        }
    }

}

