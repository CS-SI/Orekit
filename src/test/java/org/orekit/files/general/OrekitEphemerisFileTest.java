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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.files.ccsds.definitions.BodyFacade;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.WriterBuilder;
import org.orekit.files.ccsds.ndm.odm.oem.EphemerisOemWriter;
import org.orekit.files.ccsds.ndm.odm.oem.OemMetadata;
import org.orekit.files.ccsds.ndm.odm.oem.OemParser;
import org.orekit.files.ccsds.ndm.odm.oem.OemSegment;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.general.EphemerisFile.EphemerisSegment;
import org.orekit.files.general.OrekitEphemerisFile.OrekitSatelliteEphemeris;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.SpacecraftStateInterpolator;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.EventsLogger;
import org.orekit.propagation.events.EventsLogger.LoggedEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeInterpolator;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class OrekitEphemerisFileTest {

    @BeforeEach
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testOrekitEphemerisFile() {
        Assertions.assertNotNull(new OrekitEphemerisFile());
    }

    @Test
    public void testGetSatellites() {
        final String id1 = "ID1";
        final String id2 = "ID2";
        OrekitEphemerisFile file = new OrekitEphemerisFile();
        OrekitSatelliteEphemeris ephem1 = file.addSatellite(id1);
        Assertions.assertNotNull(ephem1);
        OrekitSatelliteEphemeris ephem2 = file.addSatellite(id2);
        Assertions.assertNotNull(ephem2);
    }

    @Test
    public void testWritingToOEMKvn() throws IOException {
        doTestWritingToOEM(FileFormat.KVN);
    }

    @Test
    public void testWritingToOEMXml() throws IOException {
        doTestWritingToOEM(FileFormat.XML);
    }

    private void doTestWritingToOEM(final FileFormat fileFormat) throws IOException {
        final double muTolerance = 1e-12;
        // As the default format for position is 3 digits after decimal point in km the max precision in m is 1
        final double positionTolerance = 1.;
        // As the default format for velocity is 5 digits after decimal point in km/s the max precision in m/s is 1e-2
        final double velocityTolerance = 1e-2;
        final String satId = "SATELLITE1";
        final double sma = 10000000;
        final double inc = Math.toRadians(45.0);
        final double ecc = 0.001;
        final double raan = 0.0;
        final double pa = 0.0;
        final double ta = 0.0;
        final AbsoluteDate date = new AbsoluteDate();
        final Frame frame = FramesFactory.getGCRF();
        final CelestialBody body = CelestialBodyFactory.getEarth();
        final double mu = body.getGM();
        KeplerianOrbit initialOrbit = new KeplerianOrbit(sma, ecc, inc, pa, raan, ta, PositionAngleType.TRUE, frame, date,
                mu);
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);

        final double propagationDurationSeconds = 86400.0;
        final double stepSizeSeconds = 60.0;
        List<SpacecraftState> states = new ArrayList<SpacecraftState>();

        for (double dt = 0.0; dt < propagationDurationSeconds; dt += stepSizeSeconds) {
            states.add(propagator.propagate(date.shiftedBy(dt)));
        }

        OrekitEphemerisFile ephemerisFile = new OrekitEphemerisFile();
        OrekitSatelliteEphemeris satellite = ephemerisFile.addSatellite(satId);
        satellite.addNewSegment(states);
        Assertions.assertEquals(satId, satellite.getId());
        Assertions.assertEquals(body.getGM(), satellite.getMu(), muTolerance);
        Assertions.assertEquals(0.0, states.get(0).getDate().durationFrom(satellite.getStart()), 1.0e-15);
        Assertions.assertEquals(0.0, states.get(states.size() - 1).getDate().durationFrom(satellite.getStop()), 1.0e-15);
        Assertions.assertEquals(CartesianDerivativesFilter.USE_PV,
                     satellite.getSegments().get(0).getAvailableDerivatives());
        Assertions.assertEquals("GCRF",
                     satellite.getSegments().get(0).getFrame().getName());
        Assertions.assertEquals(body.getGM(),
                     satellite.getSegments().get(0).getMu(), muTolerance);

        String tempOem = Files.createTempFile("OrekitEphemerisFileTest", ".oem").toString();
        OemMetadata template = new OemMetadata(2);
        template.setTimeSystem(TimeSystem.UTC);
        template.setObjectID(satId);
        template.setObjectName(satId);
        template.setCenter(new BodyFacade("EARTH", CelestialBodyFactory.getCelestialBodies().getEarth()));
        template.setReferenceFrame(FrameFacade.map(FramesFactory.getEME2000()));
        EphemerisOemWriter writer = new EphemerisOemWriter(new WriterBuilder().buildOemWriter(),
                                                           null, template, fileFormat, "dummy",
                                                           Constants.JULIAN_DAY, 60);
        writer.write(tempOem, ephemerisFile);

        OemParser parser = new ParserBuilder().withMu(body.getGM()).withDefaultInterpolationDegree(2).buildOemParser();
        EphemerisFile<TimeStampedPVCoordinates, OemSegment> ephemerisFrom = parser.parse(new DataSource(tempOem));
        Files.delete(Paths.get(tempOem));

        EphemerisSegment<TimeStampedPVCoordinates> segment = ephemerisFrom.getSatellites().get(satId).getSegments().get(0);
        Assertions.assertEquals(states.get(0).getDate(), segment.getStart());
        Assertions.assertEquals(states.get(states.size() - 1).getDate(), segment.getStop());
        Assertions.assertEquals(states.size(), segment.getCoordinates().size());
        Assertions.assertEquals(frame, segment.getFrame());
        Assertions.assertEquals(body.getGM(), segment.getMu(), muTolerance);
        Assertions.assertEquals(CartesianDerivativesFilter.USE_PV, segment.getAvailableDerivatives());
        Assertions.assertEquals("GCRF", segment.getFrame().getName());
        for (int i = 0; i < states.size(); i++) {
            TimeStampedPVCoordinates expected = states.get(i).getPVCoordinates();
            TimeStampedPVCoordinates actual = segment.getCoordinates().get(i);
            Assertions.assertEquals(expected.getDate(), actual.getDate());
            Assertions.assertEquals(0.0, Vector3D.distance(expected.getPosition(), actual.getPosition()), positionTolerance);
            Assertions.assertEquals(0.0, Vector3D.distance(expected.getVelocity(), actual.getVelocity()), velocityTolerance);
        }

        // test ingested ephemeris generates access intervals
        final OneAxisEllipsoid parentShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final double latitude = 0.0;
        final double longitude = 0.0;
        final double altitude = 0.0;
        final GeodeticPoint point = new GeodeticPoint(latitude, longitude, altitude);
        final TopocentricFrame topo = new TopocentricFrame(parentShape, point, "testPoint1");
        final ElevationDetector elevationDetector = new ElevationDetector(topo);
        final EphemerisSegmentPropagator<TimeStampedPVCoordinates> ephemerisSegmentPropagator =
                        new EphemerisSegmentPropagator<>(segment, new FrameAlignedProvider(segment.getInertialFrame()));
        final EventsLogger lookupLogger = new EventsLogger();
        ephemerisSegmentPropagator.addEventDetector(lookupLogger.monitorDetector(elevationDetector));

        final EventsLogger referenceLogger = new EventsLogger();
        propagator.clearEventsDetectors();
        propagator.addEventDetector(referenceLogger.monitorDetector(elevationDetector));

        propagator.propagate(segment.getStart(), segment.getStop());
        ephemerisSegmentPropagator.propagate(segment.getStart(), segment.getStop());

        final double dateEpsilon = 4.2e-5;
        Assertions.assertTrue(referenceLogger.getLoggedEvents().size() > 0);
        Assertions.assertEquals(referenceLogger.getLoggedEvents().size(), lookupLogger.getLoggedEvents().size());
        for (int i = 0; i < referenceLogger.getLoggedEvents().size(); i++) {
            LoggedEvent reference = referenceLogger.getLoggedEvents().get(i);
            LoggedEvent actual = lookupLogger.getLoggedEvents().get(i);
            Assertions.assertEquals(0.0,
                         FastMath.abs(reference.getState().getDate().durationFrom(actual.getState().getDate())),
                         dateEpsilon);
        }

        final Propagator embeddedPropagator = segment.getPropagator(new FrameAlignedProvider(segment.getInertialFrame()));
        final EventsLogger embeddedPropLogger = new EventsLogger();
        embeddedPropagator.addEventDetector(embeddedPropLogger.monitorDetector(elevationDetector));
        embeddedPropagator.propagate(segment.getStart(), segment.getStop());
        Assertions.assertEquals(referenceLogger.getLoggedEvents().size(), embeddedPropLogger.getLoggedEvents().size());
        for (int i = 0; i < referenceLogger.getLoggedEvents().size(); i++) {
            LoggedEvent reference = referenceLogger.getLoggedEvents().get(i);
            LoggedEvent actual = embeddedPropLogger.getLoggedEvents().get(i);
            Assertions.assertEquals(0.0,
                         FastMath.abs(reference.getState().getDate().durationFrom(actual.getState().getDate())),
                         dateEpsilon);

        }

        final List<SpacecraftState> readInStates = new ArrayList<SpacecraftState>();
        segment.getCoordinates().forEach(c -> {
            try {
                readInStates.add(new SpacecraftState(new CartesianOrbit(c, frame, mu)));
            } catch (IllegalArgumentException | OrekitException e) {
                Assertions.fail(e.getLocalizedMessage());
            }
        });

        // Create interpolator
        final int interpolationPoints = 5;
        final TimeInterpolator<SpacecraftState> interpolator =
                new SpacecraftStateInterpolator(interpolationPoints, frame, frame);

        Ephemeris directEphemProp = new Ephemeris(readInStates, interpolator);
        final EventsLogger directEphemPropLogger = new EventsLogger();
        directEphemProp.addEventDetector(directEphemPropLogger.monitorDetector(elevationDetector));
        directEphemProp.propagate(segment.getStart(), segment.getStop());
        Assertions.assertEquals(referenceLogger.getLoggedEvents().size(), directEphemPropLogger.getLoggedEvents().size());
        for (int i = 0; i < referenceLogger.getLoggedEvents().size(); i++) {
            LoggedEvent reference = referenceLogger.getLoggedEvents().get(i);
            LoggedEvent actual = directEphemPropLogger.getLoggedEvents().get(i);
            Assertions.assertEquals(0.0,
                         FastMath.abs(reference.getState().getDate().durationFrom(actual.getState().getDate())),
                         dateEpsilon);
        }

    }

}
