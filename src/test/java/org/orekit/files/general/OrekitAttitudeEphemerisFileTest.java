/* Copyright 2002-2023 CS GROUP
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.CelestialBodyFrame;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.SpacecraftBodyFrame;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.WriterBuilder;
import org.orekit.files.ccsds.ndm.adm.AdmHeader;
import org.orekit.files.ccsds.ndm.adm.AttitudeType;
import org.orekit.files.ccsds.ndm.adm.aem.AemMetadata;
import org.orekit.files.ccsds.ndm.adm.aem.AemSegment;
import org.orekit.files.ccsds.ndm.adm.aem.AttitudeWriter;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.general.AttitudeEphemerisFile.AttitudeEphemerisSegment;
import org.orekit.files.general.OrekitAttitudeEphemerisFile.OrekitSatelliteAttitudeEphemeris;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedAngularCoordinates;

public class OrekitAttitudeEphemerisFileTest {

    @BeforeEach
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testGetSatellites() {
        final String id1 = "ID1";
        final String id2 = "ID2";
        OrekitAttitudeEphemerisFile file = new OrekitAttitudeEphemerisFile();
        OrekitSatelliteAttitudeEphemeris ephem1 = file.addSatellite(id1);
        Assertions.assertNotNull(ephem1);
        OrekitSatelliteAttitudeEphemeris ephem2 = file.addSatellite(id2);
        Assertions.assertNotNull(ephem2);
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
        KeplerianOrbit initialOrbit = new KeplerianOrbit(sma, ecc, inc, pa, raan, ta, PositionAngleType.TRUE,
                                                         frame, date, mu);

        // Initialize a Keplerian propagator with an Inertial attitude provider
        // It is expected that all attitude data lines will have the same value
        final Rotation refRot = new Rotation(0.72501, -0.64585, 0.018542, -0.23854, false);
        AttitudeProvider inertialPointing = new FrameAlignedProvider(refRot);
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, inertialPointing);

        final double propagationDurationSeconds = 1200.0;
        final double stepSizeSeconds = 60.0;
        List<SpacecraftState> states = new ArrayList<SpacecraftState>();

        for (double dt = 0.0; dt < propagationDurationSeconds; dt += stepSizeSeconds) {
            states.add(propagator.propagate(date.shiftedBy(dt)));
        }

        OrekitAttitudeEphemerisFile ephemerisFile = new OrekitAttitudeEphemerisFile();
        OrekitSatelliteAttitudeEphemeris satellite = ephemerisFile.addSatellite(satId);
        satellite.addNewSegment(states,
                                OrekitSatelliteAttitudeEphemeris.DEFAULT_INTERPOLATION_METHOD,
                                OrekitSatelliteAttitudeEphemeris.DEFAULT_INTERPOLATION_SIZE,
                                AngularDerivativesFilter.USE_RR);

        // Test of all getters for OrekitSatelliteAttitudeEphemeris
        Assertions.assertEquals(satId, satellite.getId());
        Assertions.assertEquals(0.0, states.get(0).getDate().durationFrom(satellite.getStart()), 1.0e-15);
        Assertions.assertEquals(0.0, states.get(states.size() - 1).getDate().durationFrom(satellite.getStop()), 1.0e-15);

        // Test of all getters for OrekitAttitudeEphemerisSegment
        AttitudeEphemerisSegment<TimeStampedAngularCoordinates> segment = satellite.getSegments().get(0);
        Assertions.assertEquals(OrekitSatelliteAttitudeEphemeris.DEFAULT_INTERPOLATION_METHOD, segment.getInterpolationMethod());
        Assertions.assertEquals(OrekitSatelliteAttitudeEphemeris.DEFAULT_INTERPOLATION_SIZE, segment.getInterpolationSamples());
        Assertions.assertEquals(0.0, states.get(0).getDate().durationFrom(segment.getStart()), 1.0e-15);
        Assertions.assertEquals(0.0, states.get(states.size() - 1).getDate().durationFrom(segment.getStop()), 1.0e-15);
        Assertions.assertEquals(AngularDerivativesFilter.USE_RR, segment.getAvailableDerivatives());

        // Verify attitude
        final Attitude attitude = segment.getAttitudeProvider().getAttitude(initialOrbit, date, frame);
        Assertions.assertEquals(frame, attitude.getReferenceFrame());
        Assertions.assertEquals(refRot.getQ0(), attitude.getRotation().getQ0(), quaternionTolerance);
        Assertions.assertEquals(refRot.getQ1(), attitude.getRotation().getQ1(), quaternionTolerance);
        Assertions.assertEquals(refRot.getQ2(), attitude.getRotation().getQ2(), quaternionTolerance);
        Assertions.assertEquals(refRot.getQ3(), attitude.getRotation().getQ3(), quaternionTolerance);

        String tempAem = Files.createTempFile("OrekitAttitudeEphemerisFileTest", ".aem").toString();
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(tempAem), StandardCharsets.UTF_8)) {
            final AdmHeader header = new AdmHeader();
            header.setFormatVersion(1.0);
            new AttitudeWriter(new WriterBuilder().buildAemWriter(),
                               header, dummyMetadata(), FileFormat.KVN, "", Constants.JULIAN_DAY, 60).
            write(writer, ephemerisFile);
        }

        AttitudeEphemerisFile<TimeStampedAngularCoordinates, AemSegment> ephemerisFrom =
                        new ParserBuilder().buildAemParser().parseMessage(new DataSource(tempAem));
        Files.delete(Paths.get(tempAem));

        segment = ephemerisFrom.getSatellites().get(satId).getSegments().get(0);
        Assertions.assertEquals(states.get(0).getDate(), segment.getStart());
        Assertions.assertEquals(states.get(states.size() - 1).getDate(), segment.getStop());
        Assertions.assertEquals(states.size(), segment.getAngularCoordinates().size());
        for (int i = 0; i < states.size(); i++) {
            TimeStampedAngularCoordinates expected = states.get(i).getAttitude().getOrientation();
            TimeStampedAngularCoordinates actual = segment.getAngularCoordinates().get(i);
            Assertions.assertEquals(expected.getDate(), actual.getDate());
            Assertions.assertEquals(0.0, Rotation.distance(refRot, actual.getRotation()), quaternionTolerance);
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
            satellite.addNewSegment(states,
                                    OrekitSatelliteAttitudeEphemeris.DEFAULT_INTERPOLATION_METHOD,
                                    OrekitSatelliteAttitudeEphemeris.DEFAULT_INTERPOLATION_SIZE,
                                    AngularDerivativesFilter.USE_RR);
        } catch (OrekitIllegalArgumentException oiae) {
            Assertions.assertEquals(OrekitMessages.NULL_ARGUMENT, oiae.getSpecifier());
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
        KeplerianOrbit initialOrbit = new KeplerianOrbit(sma, ecc, inc, pa, raan, ta, PositionAngleType.TRUE,
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
            satellite.addNewSegment(states, "LINEAR", 1, AngularDerivativesFilter.USE_R);
        } catch (OrekitIllegalArgumentException oiae) {
            Assertions.assertEquals(OrekitMessages.NOT_ENOUGH_DATA, oiae.getSpecifier());
        }
    }

    private AemMetadata dummyMetadata() {
        AemMetadata metadata = new AemMetadata(4);
        metadata.setTimeSystem(TimeSystem.TT);
        metadata.setObjectID("SATELLITE1");
        metadata.setObjectName("transgalactic");
        metadata.getEndpoints().setFrameA(new FrameFacade(FramesFactory.getGCRF(), CelestialBodyFrame.GCRF,
                                                          null, null, "GCRF"));
        metadata.getEndpoints().setFrameB(new FrameFacade(null, null, null,
                                                          new SpacecraftBodyFrame(SpacecraftBodyFrame.BaseEquipment.GYRO_FRAME, "1"),
                                                          "GYRO FRAME 1"));
        metadata.getEndpoints().setA2b(true);
        metadata.setStartTime(AbsoluteDate.J2000_EPOCH.shiftedBy(80 * Constants.JULIAN_CENTURY));
        metadata.setStopTime(metadata.getStartTime().shiftedBy(Constants.JULIAN_YEAR));
        metadata.setAttitudeType(AttitudeType.QUATERNION_DERIVATIVE);
        metadata.setIsFirst(true);
        return metadata;
    }

}

