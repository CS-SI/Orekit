package org.orekit.files.iirv;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalStateException;
import org.orekit.files.iirv.terms.CoordinateSystemTerm;
import org.orekit.files.iirv.terms.CrossSectionalAreaTerm;
import org.orekit.files.iirv.terms.DataSourceTerm;
import org.orekit.files.iirv.terms.DragCoefficientTerm;
import org.orekit.files.iirv.terms.MassTerm;
import org.orekit.files.iirv.terms.MessageClassTerm;
import org.orekit.files.iirv.terms.OriginIdentificationTerm;
import org.orekit.files.iirv.terms.OriginatorRoutingIndicatorTerm;
import org.orekit.files.iirv.terms.RoutingIndicatorTerm;
import org.orekit.files.iirv.terms.SolarReflectivityCoefficientTerm;
import org.orekit.files.iirv.terms.SupportIdCodeTerm;
import org.orekit.files.iirv.terms.VectorTypeTerm;
import org.orekit.files.iirv.terms.VehicleIdCodeTerm;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.UTCScale;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IIRVMessageTest {

    private static UTCScale UTC;
    private IIRVMessage stereoAheadIIRVMessage;
    private DataSource allVectorsIIRVDataSource;
    private DataSource firstVectorOnlyIIRVDataSource;
    private IIRVParser parser;

    @BeforeEach
    public void setUp() {
        // Sets the root of data to read
        Utils.setDataRoot("regular-data");
        UTC = TimeScalesFactory.getUTC();
        parser = new IIRVParser(2024, UTC);

        final String stereoAheadIirvFile = "/iirv/ahead_20240909_01.iirv";
        stereoAheadIIRVMessage = parser.parse(
                new DataSource(stereoAheadIirvFile, () -> getClass().getResourceAsStream(stereoAheadIirvFile)))
            .getIIRV();

        final String testIIRVAllVectors = "/iirv/Test-IIRV-Metadata-All-Vectors.iirv";
        final String testIIRVFirstVectorOnly = "/iirv/Test-IIRV-Metadata-First-Vector-Only.iirv";

        allVectorsIIRVDataSource = new DataSource(testIIRVAllVectors, () -> getClass().getResourceAsStream(testIIRVAllVectors));
        firstVectorOnlyIIRVDataSource = new DataSource(testIIRVFirstVectorOnly, () -> getClass().getResourceAsStream(testIIRVFirstVectorOnly));

    }

    @Test
    void testIIRVMessageConstructors() {
        IIRVVector first = stereoAheadIIRVMessage.get(0);
        IIRVVector second = stereoAheadIIRVMessage.get(1);

        IIRVMessage message1 = new IIRVMessage(first, second);
        IIRVMessage message2 = new IIRVMessage(Arrays.asList(first, second));
        IIRVMessage message3 = new IIRVMessage();
        message3.add(first);
        message3.add(second);
        IIRVMessage message4 = parser.parse(allVectorsIIRVDataSource).getIIRV();
        IIRVMessage message5 = parser.parse(firstVectorOnlyIIRVDataSource).getIIRV();
        IIRVMessage message6 = new IIRVMessage(message1);

        // Compare different ways of creating IIRV string
        assertEquals(message1, message2);
        assertEquals(message1.toMessageString(IIRVMessage.IncludeMessageMetadata.ALL_VECTORS),
            message2.toMessageString(IIRVMessage.IncludeMessageMetadata.ALL_VECTORS));
        assertEquals(message1.toMessageString(IIRVMessage.IncludeMessageMetadata.FIRST_VECTOR_ONLY),
            message2.toMessageString(IIRVMessage.IncludeMessageMetadata.FIRST_VECTOR_ONLY));

        // Check against all other created messages
        assertEquals(message1, message1);
        assertEquals(message1, message3);
        assertEquals(message1, message4);
        assertEquals(message1, message5);
        assertEquals(message4, message5);
        assertEquals(message4, message6);
        assertNotEquals(new IIRVMessage(), null);
        assertNotEquals(new IIRVMessage(first), first);

        // Check hashcodes
        assertEquals(message1.hashCode(), message2.hashCode());
        assertEquals(message1.hashCode(), message3.hashCode());
        assertEquals(message1.hashCode(), message4.hashCode());
        assertEquals(message1.hashCode(), message5.hashCode());

        // non-increasing exception
        assertThrows(OrekitIllegalStateException.class, () -> new IIRVMessage(second, first));

        // Three vectors (ok)
        IIRVMessage threeVectorMethod = new IIRVMessage(first, second, stereoAheadIIRVMessage.get(2));
        assertEquals(3, threeVectorMethod.size());
    }

    @Test
    void testConstructorErrorHandling() {
        // Blank file
        final String blankFile = "/iirv/invalid/blank.iirv";
        assertThrows(OrekitException.class, () -> parser.parse(
            new DataSource(blankFile, () -> getClass().getResourceAsStream(blankFile))
        ));

        // Wrong sequence numbers
        assertThrows(OrekitIllegalStateException.class, () -> new IIRVMessage(stereoAheadIIRVMessage.get(0),  // (1,3)
            stereoAheadIIRVMessage.get(2)));
        assertThrows(OrekitIllegalStateException.class, () -> new IIRVMessage(stereoAheadIIRVMessage.get(0), // (1,2,4)
            stereoAheadIIRVMessage.get(1), stereoAheadIIRVMessage.get(3)));

        // Missing line 5
        final String missingLine5 = "/iirv/invalid/missing_line5.iirv";
        assertThrows(OrekitException.class, () -> parser.parse(
            new DataSource(missingLine5, () -> getClass().getResourceAsStream(missingLine5))
        ));

        // Missing a line break
        final String missingLinebeak = "/iirv/invalid/missing_linebreak.iirv";
        assertThrows(OrekitException.class, () -> parser.parse(
            new DataSource(missingLinebeak, () -> getClass().getResourceAsStream(missingLinebeak))
        ));
    }

    @Test
    void testMethods() {

        // Test "isEmpty" method
        assertTrue(new IIRVMessage().isEmpty());
        assertEquals(0, new IIRVMessage().size());
        assertFalse(stereoAheadIIRVMessage.isEmpty());
        assertEquals(97, stereoAheadIIRVMessage.size());

        // Test "getVectors"
        assertEquals(stereoAheadIIRVMessage.get(0), stereoAheadIIRVMessage.getVectors().get(0));
    }

    @Test
    void testIIRVMessageValidation() {
        IIRVVector first = stereoAheadIIRVMessage.get(0);
        IIRVVector second = stereoAheadIIRVMessage.get(1);

        TimeStampedPVCoordinates c = second.getTimeStampedPVCoordinates(2024);

        IIRVBuilder iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setMessageID(second.getMessageID());
        iirvBuilder.setMessageClass(second.getMessageClass());
        iirvBuilder.setOriginIdentification(second.getOriginIdentification());
        iirvBuilder.setRoutingIndicator(second.getRoutingIndicator());
        iirvBuilder.setVectorType(second.getVectorType());
        iirvBuilder.setDataSource(second.getDataSource());
        iirvBuilder.setSupportIdCode(second.getSupportIdCode());
        iirvBuilder.setVehicleIdCode(second.getVehicleIdCode());
        iirvBuilder.setSequenceNumber(second.getSequenceNumber());
        iirvBuilder.setMass(second.getMass());
        iirvBuilder.setCrossSectionalArea(second.getCrossSectionalArea());
        iirvBuilder.setDragCoefficient(second.getDragCoefficient());
        iirvBuilder.setSolarReflectivityCoefficient(second.getSolarReflectivityCoefficient());
        iirvBuilder.setOriginatorRoutingIndicator(second.getOriginatorRoutingIndicator());

        // Message ID term
        iirvBuilder.setMessageID(0);
        assertThrows(OrekitIllegalStateException.class, () -> new IIRVMessage(first, iirvBuilder.buildVector(c)));
        iirvBuilder.setMessageID(second.getMessageID());

        // Message Class term
        iirvBuilder.setMessageClass(MessageClassTerm.INFLIGHT_UPDATE);
        assertThrows(OrekitIllegalStateException.class, () -> new IIRVMessage(first, iirvBuilder.buildVector(c)));
        iirvBuilder.setMessageClass(second.getMessageClass());

        // Origin ID term
        iirvBuilder.setOriginIdentification(OriginIdentificationTerm.JPL);
        assertThrows(OrekitIllegalStateException.class, () -> new IIRVMessage(first, iirvBuilder.buildVector(c)));
        iirvBuilder.setOriginIdentification(second.getOriginIdentification());

        // Routing indicator term
        iirvBuilder.setRoutingIndicator(RoutingIndicatorTerm.ETR);
        assertThrows(OrekitIllegalStateException.class, () -> new IIRVMessage(first, iirvBuilder.buildVector(c)));
        iirvBuilder.setRoutingIndicator(second.getRoutingIndicator());

        // Vector type term
        iirvBuilder.setVectorType(VectorTypeTerm.REENTRY);
        assertThrows(OrekitIllegalStateException.class, () -> new IIRVMessage(first, iirvBuilder.buildVector(c)));
        iirvBuilder.setVectorType(second.getVectorType());

        // Data source
        iirvBuilder.setDataSource(DataSourceTerm.OFFLINE);
        assertThrows(OrekitIllegalStateException.class, () -> new IIRVMessage(first, iirvBuilder.buildVector(c)));
        iirvBuilder.setDataSource(second.getDataSource());

        // Coordinate system term
        iirvBuilder.setCoordinateSystem(CoordinateSystemTerm.HELIOCENTRIC_B1950);
        assertThrows(OrekitIllegalStateException.class, () -> new IIRVMessage(first, iirvBuilder.buildVector(c)));
        iirvBuilder.setCoordinateSystem(second.getCoordinateSystem());

        // SIC term
        iirvBuilder.setSupportIdCode(new SupportIdCodeTerm(1));
        assertThrows(OrekitIllegalStateException.class, () -> new IIRVMessage(first, iirvBuilder.buildVector(c)));
        iirvBuilder.setSupportIdCode(second.getSupportIdCode());

        // VID term
        iirvBuilder.setVehicleIdCode(new VehicleIdCodeTerm(2));
        assertThrows(OrekitIllegalStateException.class, () -> new IIRVMessage(first, iirvBuilder.buildVector(c)));
        iirvBuilder.setVehicleIdCode(second.getVehicleIdCode());

    }

    @Test
    void testSequenceNumberMonotonic() {
        IIRVVector vec1 = stereoAheadIIRVMessage.get(0);
        IIRVVector origVec2 = stereoAheadIIRVMessage.get(1);

        String line2SkipSequenceNumber = "1111023401003253001500000033";
        IIRVVector vec2 = new IIRVVector(origVec2.buildLine1(true), line2SkipSequenceNumber,
            origVec2.buildLine3(), origVec2.buildLine4(), origVec2.buildLine5(), origVec2.buildLine6(), UTC);

        assertThrows(OrekitIllegalStateException.class, () -> new IIRVMessage(vec1, vec2));
    }

    /**
     * Tests IIRV files from the STEREO mission.
     * <p>
     * Source: <a href="https://stereo-ssc.nascom.nasa.gov/stations/iirv/">Stereo Science Center</a>
     */
    @Test
    @DefaultDataContext
    void testStereoIIRV() {
        final UTCScale utc = TimeScalesFactory.getUTC();

        // Tests "ahead" IIRV file from the STEREO mission.
        //  Source: https://stereo-ssc.nascom.nasa.gov/stations/iirv/

        // Website delivers one IIRV vector for each day at 00:00 UTC
        AbsoluteDate aheadAbsoluteDate = new AbsoluteDate(
            2024, 9, 9, 0, 0, 0, utc);
        DateComponents aheadDate = aheadAbsoluteDate.getComponents(utc).getDate();

        // Test ahead IIRV Vector
        IIRVVector iirv = stereoAheadIIRVMessage.get(0);
        assertEquals(iirv, iirv); // Trivial self-equality check

        // Check Line 1
        assertEquals(1234567, iirv.getMessageID().value());
        assertEquals(MessageClassTerm.NOMINAL, iirv.getMessageClass());
        assertEquals(OriginIdentificationTerm.GSFC, iirv.getOriginIdentification());
        assertEquals(RoutingIndicatorTerm.MANY, iirv.getRoutingIndicator());

        // Check Line 2
        assertEquals(VectorTypeTerm.FREE_FLIGHT, iirv.getVectorType());
        assertEquals(DataSourceTerm.NOMINAL, iirv.getDataSource());
        assertEquals(CoordinateSystemTerm.GEOCENTRIC_TRUE_OF_DATE_ROTATING, iirv.getCoordinateSystem());
        assertEquals(234, iirv.getSupportIdCode().value());
        assertEquals(1, iirv.getVehicleIdCode().value());
        assertEquals(1, iirv.getSequenceNumber().value());
        assertEquals(aheadDate, iirv.getDayOfYear().getDateComponents(aheadDate.getYear()));

        // Check Line 3
        assertEquals(-17325900294L, iirv.getXPosition().value());
        assertEquals(55126516659L, iirv.getYPosition().value());
        assertEquals(25045637815L, iirv.getZPosition().value());

        // Check Line 4
        assertEquals(4007847.475, iirv.getXVelocity().value());
        assertEquals(1261889.943, iirv.getYVelocity().value());
        assertEquals(325.189, iirv.getZVelocity().value());

        // Check Line 5
        assertEquals(MassTerm.UNUSED, iirv.getMass());
        assertEquals(CrossSectionalAreaTerm.UNUSED, iirv.getCrossSectionalArea());
        assertEquals(DragCoefficientTerm.UNUSED, iirv.getDragCoefficient());
        assertEquals(SolarReflectivityCoefficientTerm.UNUSED, iirv.getSolarReflectivityCoefficient());

        // Check Line 6
        assertEquals(OriginatorRoutingIndicatorTerm.GAQD, iirv.getOriginatorRoutingIndicator());
    }

    @Test
    void parseSingleVectorFile() {
        final String iirvFile = "/iirv/ISS_ZARYA_25544_NASA_IIRV.iirv";  // IIRV for the ISS, generated by STK
        final DataSource source = new DataSource(iirvFile, () -> getClass().getResourceAsStream(iirvFile));

        IIRVMessage iirvMessage = parser.parse(source).getIIRV();
        assertEquals(1, iirvMessage.size()); // Verify only one vector is loaded

        // Validate a few arbitrary fields
        assertEquals(2.2, iirvMessage.get(0).getDragCoefficient().value());
        assertEquals(0, iirvMessage.get(0).getMessageID().value());
    }

    @Test
    void parseMultipleVectorFile() {
        // Path to a TLE generated for the international space station
        final String iirvFile = "/iirv/ISS_ZARYA_25544_NASA_IIRV_1DAY.iirv";
        final DataSource source = new DataSource(iirvFile, () -> getClass().getResourceAsStream(iirvFile));

        IIRVMessage iirvMessage = parser.parse(source).getIIRV();
        assertEquals(6, iirvMessage.size());

        // Validate the sequence numbers are in order from 0-5
        for (int i = 0; i < 6; i++) {
            assertEquals(i, iirvMessage.get(i).getSequenceNumber().value());
        }
    }


}
