package org.orekit.files.general;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.files.ccsds.OEMParser;
import org.orekit.files.ccsds.OEMWriter;
import org.orekit.files.general.EphemerisFile.EphemerisSegment;
import org.orekit.files.general.OrekitEphemerisFile.OrekitSatelliteEphemeris;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

public class OrekitEphemerisFileTest {

    @Before
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testOrekitEphemerisFile() {
        assertNotNull(new OrekitEphemerisFile());
    }

    @Test
    public void testGetSatellites() {
        final String id1 = "ID1";
        final String id2 = "ID2";
        OrekitEphemerisFile file = new OrekitEphemerisFile();
        OrekitSatelliteEphemeris ephem1 = file.addSatellite(id1);
        assertNotNull(ephem1);
        OrekitSatelliteEphemeris ephem2 = file.addSatellite(id2);
        assertNotNull(ephem2);
    }

    @Test
    public void testWritingToOEM() throws OrekitException, IOException {
        final double muTolerance = 1e-12;
        final double positionTolerance = 1e-8;
        final double velocityTolerance = 1e-8;
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
        KeplerianOrbit initialOrbit = new KeplerianOrbit(sma, ecc, inc, pa, raan, ta, PositionAngle.TRUE, frame, date,
                mu);
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);

        final double propagationDurationSeconds = 3600.0;
        final double stepSizeSeconds = 60.0;
        List<SpacecraftState> states = new ArrayList<SpacecraftState>();

        for (double dt = 0.0; dt < propagationDurationSeconds; dt += stepSizeSeconds) {
            states.add(propagator.propagate(date.shiftedBy(dt)));
        }

        OrekitEphemerisFile ephemerisFile = new OrekitEphemerisFile();
        OrekitSatelliteEphemeris satellite = ephemerisFile.addSatellite(satId);
        satellite.addNewSegment(states);

        String tempOemFile = Files.createTempFile("OrekitEphemerisFileTest", ".oem").toString();
        new OEMWriter().write(tempOemFile, ephemerisFile);
        
        EphemerisFile ephemerisFromFile = new OEMParser().parse(tempOemFile);
        EphemerisSegment segment = ephemerisFromFile.getSatellites().get(satId).getSegments().get(0);
        assertEquals(states.get(0).getDate(), segment.getStart());
        assertEquals(states.get(states.size() - 1).getDate(), segment.getStop());
        assertEquals(states.size(), segment.getCoordinates().size());
        assertEquals(frame, segment.getFrame());
        assertEquals(body.getName().toUpperCase(), segment.getFrameCenterString());
        assertEquals(body.getGM(), segment.getMu(), muTolerance);
        for(int i = 0 ; i < states.size() ; i++) {
            TimeStampedPVCoordinates expected = states.get(i).getPVCoordinates();
            TimeStampedPVCoordinates actual = segment.getCoordinates().get(i);
            assertEquals(expected.getDate(), actual.getDate());
            assertEquals(0.0, Vector3D.distance(expected.getPosition(), actual.getPosition()), positionTolerance);
            assertEquals(0.0, Vector3D.distance(expected.getVelocity(), actual.getVelocity()), velocityTolerance);
        }
       
    }

}
