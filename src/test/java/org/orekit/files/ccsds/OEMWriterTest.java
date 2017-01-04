package org.orekit.files.ccsds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.OEMFile.EphemeridesBlock;
import org.orekit.files.ccsds.OEMWriter.InterpolationMethod;
import org.orekit.files.general.EphemerisFile;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;

public class OEMWriterTest {
    private static final double POSITION_PRECISION = 1e-9;
    private static final double VELOCITY_PRECISION = 1e-9;

    @Before
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testOEMWriter() {
        assertNotNull(new OEMWriter());
    }

    @Test
    public void testOEMWriterInterpolationMethodStringStringString() {
        assertNotNull(new OEMWriter(OEMWriter.DEFAULT_INTERPOLATION_METHOD, null, null, null));
        assertNotNull(new OEMWriter(InterpolationMethod.HERMITE, "FakeOriginator", null, null));
        assertNotNull(new OEMWriter(InterpolationMethod.HERMITE, null, "FakeObjectId", null));
        assertNotNull(new OEMWriter(InterpolationMethod.HERMITE, null, null, "Fake Object Name"));
        assertNotNull(new OEMWriter(InterpolationMethod.HERMITE, "FakeOriginator", "FakeObjectId", null));
        assertNotNull(new OEMWriter(InterpolationMethod.HERMITE, "FakeOriginator", null, "Fake Object Name"));
        assertNotNull(new OEMWriter(InterpolationMethod.HERMITE, null, "FakeObjectId", "Fake Object Name"));
        assertNotNull(new OEMWriter(InterpolationMethod.HERMITE, "FakeOriginator", "FakeObjectId", "Fake Object Name"));
    }

    @Test
    public void testWriteOEM1() throws OrekitException, IOException {
        final String ex = "/ccsds/OEMExample.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final OEMParser parser = new OEMParser().withMu(CelestialBodyFactory.getEarth().getGM())
                .withConventions(IERSConventions.IERS_2010);
        final OEMFile oemFile = parser.parse(inEntry, "OEMExample.txt");
        final EphemerisFile ephemerisFile = (EphemerisFile) oemFile;

        String originator = oemFile.getOriginator();
        String objectName = oemFile.getEphemeridesBlocks().get(0).getMetaData().getObjectName();
        String objectID = oemFile.getEphemeridesBlocks().get(0).getMetaData().getObjectID();
        String interpolationMethodString = oemFile.getEphemeridesBlocks().get(0).getInterpolationMethod();
        InterpolationMethod interpolationMethod = Enum.valueOf(InterpolationMethod.class, interpolationMethodString);
        String tempOEMFilePath = Files.createTempFile("TestWriteOEM1", ".oem").toString();
        OEMWriter writer = new OEMWriter(interpolationMethod, originator, objectID, objectName);
        writer.write(tempOEMFilePath, ephemerisFile);

        final OEMFile generatedOemFile = parser.parse(tempOEMFilePath);
        compareOemFiles(oemFile, generatedOemFile);
    }

    @Test(expected = OrekitIllegalArgumentException.class)
    public void testUnfoundSpaceId() throws OrekitException, IOException {
        final String ex = "/ccsds/OEMExample.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final OEMParser parser = new OEMParser().withMu(CelestialBodyFactory.getEarth().getGM())
                .withConventions(IERSConventions.IERS_2010);
        final OEMFile oemFile = parser.parse(inEntry, "OEMExample.txt");
        final EphemerisFile ephemerisFile = (EphemerisFile) oemFile;

        String badObjectId = "12345";
        String interpolationMethodString = oemFile.getEphemeridesBlocks().get(0).getInterpolationMethod();
        InterpolationMethod interpolationMethod = Enum.valueOf(InterpolationMethod.class, interpolationMethodString);
        String tempOEMFilePath = Files.createTempFile("TestOEMUnfoundSpaceId", ".oem").toString();
        OEMWriter writer = new OEMWriter(interpolationMethod, null, badObjectId, null);
        writer.write(tempOEMFilePath, ephemerisFile);

    }

    @Test
    public void testUnisatelliteFileWithDefault() throws OrekitException, IOException {
        final String ex = "/ccsds/OEMExample.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final OEMParser parser = new OEMParser().withMu(CelestialBodyFactory.getEarth().getGM())
                .withConventions(IERSConventions.IERS_2010);
        final OEMFile oemFile = parser.parse(inEntry, "OEMExample.txt");
        final EphemerisFile ephemerisFile = (EphemerisFile) oemFile;

        String tempOEMFilePath = Files.createTempFile("TestOEMUnisatelliteWithDefault", ".oem").toString();
        OEMWriter writer = new OEMWriter();
        writer.write(tempOEMFilePath, ephemerisFile);

        final OEMFile generatedOemFile = parser.parse(tempOEMFilePath);
        assertEquals(oemFile.getEphemeridesBlocks().get(0).getMetaData().getObjectID(),
                generatedOemFile.getEphemeridesBlocks().get(0).getMetaData().getObjectID());
    }

    @Test
    public void testMultisatelliteFile() throws OrekitException, IOException {
        final String id1 = "ID1";
        final String id2 = "ID2";
        StandInEphemerisFile file = new StandInEphemerisFile();
        file.getSatellites().put(id1, new StandInSatelliteEphemeris(id1));
        file.getSatellites().put(id2, new StandInSatelliteEphemeris(id2));
        
        EphemerisFile ephemerisFile = (EphemerisFile) file;
        String tempOEMFilePath = Files.createTempFile("TestOEMMultisatellite", ".oem").toString();
        
        OEMWriter writer1 = new OEMWriter();
        
        try {
            writer1.write(tempOEMFilePath, ephemerisFile);
            fail("Should have thrown OrekitIllegalArgumentException due to multiple satellites");
        } catch (OrekitIllegalArgumentException e) {
            
        }
        
        tempOEMFilePath = Files.createTempFile("TestOEMMultisatellite", ".oem").toString();
        OEMWriter writer2 = new OEMWriter(OEMWriter.DEFAULT_INTERPOLATION_METHOD, null, id1, null);
        writer2.write(tempOEMFilePath, ephemerisFile);
    }

    private void compareOemEphemerisBlocks(EphemeridesBlock block1, EphemeridesBlock block2) {
        compareOemEphemerisBlocksMetadata(block1.getMetaData(), block2.getMetaData());
        assertEquals(block1.getStart(), block2.getStart());
        assertEquals(block1.getStop(), block2.getStop());
        assertEquals(block1.getInterpolationDegree(), block2.getInterpolationDegree());
        assertEquals(block1.getInterpolationMethod(), block2.getInterpolationMethod());
        assertEquals(block1.getEphemeridesDataLines().size(), block2.getEphemeridesDataLines().size());
        for (int i = 0; i < block1.getEphemeridesDataLines().size(); i++) {
            TimeStampedPVCoordinates c1 = block1.getEphemeridesDataLines().get(i);
            TimeStampedPVCoordinates c2 = block2.getEphemeridesDataLines().get(i);
            assertEquals(c1.getDate(), c2.getDate());
            assertEquals(c1.getPosition() + " -> " + c2.getPosition(), 0.0,
                    Vector3D.distance(c1.getPosition(), c2.getPosition()), POSITION_PRECISION);
            assertEquals(c1.getVelocity() + " -> " + c2.getVelocity(), 0.0,
                    Vector3D.distance(c1.getVelocity(), c2.getVelocity()), VELOCITY_PRECISION);
        }

    }

    private void compareOemEphemerisBlocksMetadata(ODMMetaData meta1, ODMMetaData meta2) {
        assertEquals(meta1.getObjectID(), meta2.getObjectID());
        assertEquals(meta1.getObjectName(), meta2.getObjectName());
        assertEquals(meta1.getCenterName(), meta2.getCenterName());
        assertEquals(meta1.getFrameString(), meta2.getFrameString());
        assertEquals(meta1.getTimeSystem(), meta2.getTimeSystem());
    }

    private void compareOemFiles(OEMFile file1, OEMFile file2) {
        assertEquals(file1.getOriginator(), file2.getOriginator());
        assertEquals(file1.getEphemeridesBlocks().size(), file2.getEphemeridesBlocks().size());
        for (int i = 0; i < file1.getEphemeridesBlocks().size(); i++) {
            compareOemEphemerisBlocks(file1.getEphemeridesBlocks().get(i), file2.getEphemeridesBlocks().get(i));
        }
    }

    private class StandInSatelliteEphemeris implements EphemerisFile.SatelliteEphemeris {
        final String id;

        public StandInSatelliteEphemeris(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public double getMu() {
            // TODO Auto-generated method stub
            return 0;
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public List<? extends EphemerisFile.EphemerisSegment> getSegments() {
            return new ArrayList();
        }

        @Override
        public AbsoluteDate getStart() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public AbsoluteDate getStop() {
            // TODO Auto-generated method stub
            return null;
        }

    }

    private class StandInEphemerisFile implements EphemerisFile {
        private final Map<String, StandInSatelliteEphemeris> satEphem;

        public StandInEphemerisFile() {
            this.satEphem = new HashMap<String, StandInSatelliteEphemeris>();
        }

        @Override
        public Map<String, StandInSatelliteEphemeris> getSatellites() {
            return satEphem;
        }

    }

}
