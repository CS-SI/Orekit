package org.orekit.files.iirv;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.files.iirv.terms.CheckSumTerm;
import org.orekit.files.iirv.terms.CoordinateSystemTerm;
import org.orekit.files.iirv.terms.CrossSectionalAreaTerm;
import org.orekit.files.iirv.terms.DataSourceTerm;
import org.orekit.files.iirv.terms.DayOfYearTerm;
import org.orekit.files.iirv.terms.DragCoefficientTerm;
import org.orekit.files.iirv.terms.MassTerm;
import org.orekit.files.iirv.terms.MessageClassTerm;
import org.orekit.files.iirv.terms.MessageIDTerm;
import org.orekit.files.iirv.terms.MessageSourceTerm;
import org.orekit.files.iirv.terms.MessageStartConstantTerm;
import org.orekit.files.iirv.terms.MessageTypeTerm;
import org.orekit.files.iirv.terms.OriginIdentificationTerm;
import org.orekit.files.iirv.terms.OriginatorRoutingIndicatorTerm;
import org.orekit.files.iirv.terms.PositionVectorComponentTerm;
import org.orekit.files.iirv.terms.RoutingIndicatorTerm;
import org.orekit.files.iirv.terms.SequenceNumberTerm;
import org.orekit.files.iirv.terms.SolarReflectivityCoefficientTerm;
import org.orekit.files.iirv.terms.SupportIdCodeTerm;
import org.orekit.files.iirv.terms.TransferTypeConstantTerm;
import org.orekit.files.iirv.terms.VectorEpochTerm;
import org.orekit.files.iirv.terms.VectorTypeTerm;
import org.orekit.files.iirv.terms.VehicleIdCodeTerm;
import org.orekit.files.iirv.terms.VelocityVectorComponentTerm;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.UTCScale;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Unit tests for {@link IIRVVector}
 */
public class IIRVVectorTest {

    private static UTCScale UTC;
    private final String line1 = "030000001010GIIRV GSFC";
    private final String line1NoMetadata = "GIIRV GSFC";
    private final String line2 = "1111123401001001000000000017";
    private final String line3 = " 000003038560-000003031452 000005261153067";
    private final String line4 = " 004300791000 005897352000 000909949000103";
    private final String line5 = "00001000010000220 1000000007";
    private final String line6 = "ITERM GCQU";

    final IIRVVector iirv = new IIRVVector(line1, line2, line3, line4, line5, line6, UTC);
    private final List<String> lines = new ArrayList<>(Arrays.asList(line1, line2, line3, line4, line5, line6));

    @BeforeEach
    @DefaultDataContext
    public void setUp() {
        // Sets the root of data to read
        Utils.setDataRoot("regular-data");
        UTC = TimeScalesFactory.getUTC();
    }

    @Test
    void parseLineStrings() {
        final String computedLine1 = iirv.buildLine1(true);
        final String computedLine2 = iirv.buildLine2();
        final String computedLine3 = iirv.buildLine3();
        final String computedLine4 = iirv.buildLine4();
        final String computedLine5 = iirv.buildLine5();
        final String computedLine6 = iirv.buildLine6();

        // Check Line 1
        assertEquals(MessageTypeTerm.DEFAULT, iirv.getMessageType());
        assertEquals(new MessageIDTerm("0000001"), iirv.getMessageID());
        assertEquals(MessageSourceTerm.DEFAULT, iirv.getMessageSource());
        assertEquals(MessageClassTerm.NOMINAL, iirv.getMessageClass());
        assertEquals(new MessageStartConstantTerm(), iirv.getMessageStart());
        assertEquals(OriginIdentificationTerm.GSFC, iirv.getOriginIdentification());
        assertEquals(RoutingIndicatorTerm.GSFC, iirv.getRoutingIndicator());
        assertEquals(line1, computedLine1);

        // Check Line 2
        assertEquals(VectorTypeTerm.FREE_FLIGHT, iirv.getVectorType());
        assertEquals(DataSourceTerm.NOMINAL, iirv.getDataSource());
        assertEquals(new TransferTypeConstantTerm(), iirv.getTransferType());
        assertEquals(CoordinateSystemTerm.GEOCENTRIC_TRUE_OF_DATE_ROTATING, iirv.getCoordinateSystem());
        assertEquals(new SupportIdCodeTerm("1234"), iirv.getSupportIdCode());
        assertEquals(new VehicleIdCodeTerm("01"), iirv.getVehicleIdCode());
        assertEquals(new SequenceNumberTerm("001"), iirv.getSequenceNumber());
        assertEquals(new DayOfYearTerm("001").getDateComponents(2024),
            iirv.getDayOfYear().getDateComponents(2024));
        assertChecksumIsValid(iirv.getLine2CheckSum(), line2);
        assertEquals(line2, computedLine2);

        // Check Line 3
        assertEquals(3038560, iirv.getXPosition().value());
        assertEquals(-3031452, iirv.getYPosition().value());
        assertEquals(5261153, iirv.getZPosition().value());
        assertChecksumIsValid(iirv.getLine3CheckSum(), line3);
        assertEquals(line3, computedLine3);

        // Check Line 4
        assertEquals(4300791, iirv.getXVelocity().value());
        assertEquals(5897352, iirv.getYVelocity().value());
        assertEquals(909949, iirv.getZVelocity().value());
        assertChecksumIsValid(iirv.getLine4CheckSum(), line4);
        assertEquals(line4, computedLine4);

        // Check Line 5
        assertEquals(0000100.0, iirv.getMass().value());
        assertEquals(010.00, iirv.getCrossSectionalArea().value());
        assertEquals(2.2, iirv.getDragCoefficient().value());
        assertEquals(1.000000, iirv.getSolarReflectivityCoefficient().value());
        assertChecksumIsValid(iirv.getLine5CheckSum(), line5);
        assertEquals(line5, computedLine5);

        // Check Line 6
        assertEquals("ITERM", iirv.getMessageEnd().value());
        assertEquals(" ", iirv.getSpareTerm().value());
        assertEquals(new OriginatorRoutingIndicatorTerm("GCQU"), iirv.getOriginatorRoutingIndicator());
        assertEquals(line6, computedLine6);
    }

    @Test
    void iirvValidConstructorValidationTests() {
        // Term-based constructor
        final IIRVVector iirvFromTerms = new IIRVVector(
            MessageTypeTerm.DEFAULT,
            new MessageIDTerm(1),
            MessageSourceTerm.DEFAULT,
            MessageClassTerm.NOMINAL,
            OriginIdentificationTerm.GSFC,
            RoutingIndicatorTerm.GSFC,
            VectorTypeTerm.FREE_FLIGHT,
            DataSourceTerm.NOMINAL,
            CoordinateSystemTerm.GEOCENTRIC_TRUE_OF_DATE_ROTATING,
            new SupportIdCodeTerm(1234),
            new VehicleIdCodeTerm(1),
            new SequenceNumberTerm(1),
            new DayOfYearTerm(1),
            new VectorEpochTerm("000000000"),
            new PositionVectorComponentTerm(3038560),
            new PositionVectorComponentTerm(-3031452),
            new PositionVectorComponentTerm(5261153),
            new VelocityVectorComponentTerm(4300791),
            new VelocityVectorComponentTerm(5897352),
            new VelocityVectorComponentTerm(909949),
            new MassTerm(100),
            new CrossSectionalAreaTerm(10),
            new DragCoefficientTerm(2.2),
            new SolarReflectivityCoefficientTerm(1),
            OriginatorRoutingIndicatorTerm.GCQU,
            UTC
        );

        final IIRVVector iirvFromLines = new IIRVVector(lines, UTC);   // Constructor form list of lines
        final IIRVVector iirvFromCopy = new IIRVVector(iirvFromTerms); // Copy constructor
        final IIRVVector iirvFromString = new IIRVParser(2024, UTC).parse(lines).getIIRV().get(0); // Parsed string constructor

        final List<String> linesWithSeparators = new ArrayList<>();
        for (String l : lines) {
            linesWithSeparators.add(l + IIRVVector.LINE_SEPARATOR);
        }
        final IIRVVector iirvFromLinesWithSeparators = new IIRVVector(linesWithSeparators, UTC);

        // Compare line-by-line for equality
        String truthIIRVString = lines.stream()
            .map(Object::toString)
            .collect(Collectors.joining(IIRVVector.LINE_SEPARATOR)) + IIRVVector.LINE_SEPARATOR;

        // Compare each of the different ways of creating an IIRV that should be identical
        for (IIRVVector compareIIRV : Arrays.asList(iirv, iirvFromTerms, iirvFromLines, iirvFromCopy, iirvFromString, iirvFromLinesWithSeparators)){
            assertEquals(truthIIRVString, compareIIRV.toIIRVString(true));  // Compare equality by string
            assertEquals(iirv, compareIIRV);  // Compare equality by ==

            // Check that generated IIRV vector strings that include/exclude message metadata are equal
            assertEquals(iirv.toIIRVString(true), compareIIRV.toIIRVString(true));
            assertEquals(iirv.toIIRVString(false), compareIIRV.toIIRVString(false));
        }
    }

    @Test
    void testIIRVValidation() {
        // Check function: IIRVVector.validateLines
        assertDoesNotThrow(() -> IIRVVector.validateLines(
            line1, line2, line3, line4, line5, line6, true));
        assertDoesNotThrow(() -> IIRVVector.validateLines(
            line1NoMetadata, line2, line3, line4, line5, line6, false));

        // Check function: IIRVVector.isFormatOK
        assertTrue(IIRVVector.isFormatOK(line1, line2, line3, line4, line5, line6));
        assertTrue(IIRVVector.isFormatOK(line1NoMetadata, line2, line3, line4, line5, line6));
        assertTrue(IIRVVector.isFormatOK(Arrays.asList(line1, line2, line3, line4, line5, line6)));
        assertTrue(IIRVVector.isFormatOK(Arrays.asList(line1NoMetadata, line2, line3, line4, line5, line6)));

        // Check == error handling
        assertNotEquals("Wrong data type", iirv);
        assertNotEquals(null, iirv);

        // Check line validation with/without line separator
        assertTrue(IIRVVector.validateLine(0, line1 + IIRVVector.LINE_SEPARATOR));
        assertTrue(IIRVVector.validateLine(1, line2 ));
        assertTrue(IIRVVector.LINE_2_PATTERN.matcher(line2 ).matches());
        assertTrue(IIRVVector.LINE_2_PATTERN.matcher(line2 + IIRVVector.LINE_SEPARATOR).matches());
        assertTrue(IIRVVector.validateLine(1, line2 + IIRVVector.LINE_SEPARATOR));
        assertTrue(IIRVVector.validateLine(2, line3 + IIRVVector.LINE_SEPARATOR));
        assertTrue(IIRVVector.validateLine(3, line4 + IIRVVector.LINE_SEPARATOR));
        assertTrue(IIRVVector.validateLine(4, line5 + IIRVVector.LINE_SEPARATOR));
        assertTrue(IIRVVector.validateLine(5, line6 + IIRVVector.LINE_SEPARATOR));
    }

    @Test
    void iirvErrorHandlingTests() {
        // Check constructor errors
        assertThrows(OrekitIllegalArgumentException.class, // Line 1 w/ metadata: "GIIRB" instead of "GIIRV"
            () -> new IIRVVector("030000001010GIIRB GSFC", line2, line3, line4, line5, line6, UTC)
        );
        assertThrows(OrekitIllegalArgumentException.class, // Line 1 w/out metadata: "GIIRB" instead of "GIIRV"
            () -> new IIRVVector("GIIRB GSFC", line2, line3, line4, line5, line6, UTC)
        );
        assertThrows(OrekitIllegalArgumentException.class, // Empty string
            () -> new IIRVVector("", line2, line3, line4, line5, line6, UTC)
        );
        assertThrows(OrekitIllegalArgumentException.class, // null value
            () -> new IIRVVector(null, line2, line3, line4, line5, line6, UTC)
        );

        // Check function: IIRVVector.validateLines
        assertThrows(OrekitIllegalArgumentException.class, // Empty list
            () -> IIRVVector.validateLines(Collections.emptyList(), true)
        );
        assertThrows(OrekitIllegalArgumentException.class, // null list
            () -> IIRVVector.validateLines(null, true)
        );
        assertThrows(OrekitIllegalArgumentException.class, // String too short
            () -> IIRVVector.validateLine(10, "xy")
        );
        assertThrows(OrekitIllegalArgumentException.class, // invalid line index in validation
            () -> IIRVVector.validateLine(10, line1)
        );

        // Check function: IIRVVector.isFormatOK
        assertFalse(IIRVVector.isFormatOK(Collections.emptyList()));
        assertFalse(IIRVVector.isFormatOK(null));

        // Check invalid lines 2-6 for both versions of line 1 (w/ and w/out metadata)
        for (String l1 : Arrays.asList(line1, line1NoMetadata)) {
            assertThrows(OrekitIllegalArgumentException.class, // Line 2: "0" is not a valid vector type
                () -> new IIRVVector(l1, "0111123401001001000000000017", line3, line4, line5, line6, UTC)
            );
            assertThrows(OrekitIllegalArgumentException.class, // Line 2: Invalid checksum
                () -> new IIRVVector(l1, "0111123401001001000000000018", line3, line4, line5, line6, UTC)
            );
            assertThrows(OrekitIllegalArgumentException.class, // Line 2: null value
                () -> new IIRVVector(l1, null, line3, line4, line5, line6, UTC)
            );

            assertThrows(OrekitIllegalArgumentException.class, // Line 3: First character must be ' ' or '-'
                () -> new IIRVVector(l1, line2, "0000003038560-000003031452 000005261153067", line4, line5, line6, UTC)
            );
            assertThrows(OrekitIllegalArgumentException.class, // Line 3: Invalid checksum
                () -> new IIRVVector(l1, line2, "0000003038560-000003031452 000005261153167", line4, line5, line6, UTC)
            );
            assertThrows(OrekitIllegalArgumentException.class, // Line 3: null value
                () -> new IIRVVector(l1, line2, null, line4, line5, line6, UTC)
            );

            assertThrows(OrekitIllegalArgumentException.class, // Line 4: incorrect length
                () -> new IIRVVector(l1, line2, line3, " 004300791000 005897352000 0009099490000228", line5, line6, UTC)
            );
            assertThrows(OrekitIllegalArgumentException.class, // Line 4: Invalid checksum
                () -> new IIRVVector(l1, line2, line3, " 004300791000 005897352000 000909949000228", line5, line6, UTC)
            );
            assertThrows(OrekitIllegalArgumentException.class, // Line 4: null value
                () -> new IIRVVector(l1, line2, line3, null, line5, line6, UTC)
            );

            assertThrows(OrekitIllegalArgumentException.class, // Line 5: Unexpected alphanumeric character
                () -> new IIRVVector(l1, line2, line3, line4, "000010a0010000220 1000000007", line6, UTC)
            );
            assertThrows(OrekitIllegalArgumentException.class, // Line 5: Checksum
                () -> new IIRVVector(l1, line2, line3, line4, "000010a0010000220 1000000017", line6, UTC)
            );
            assertThrows(OrekitIllegalArgumentException.class, // Line 5: null value
                () -> new IIRVVector(l1, line2, line3, line4, null, line6, UTC)
            );

            assertThrows(OrekitIllegalArgumentException.class, // Line 6: missing space
                () -> new IIRVVector(l1, line2, line3, line4, line5, "ITERMGCQU", UTC)
            );
            assertThrows(OrekitIllegalArgumentException.class, // Line 6: null value
                () -> new IIRVVector(l1, line2, line3, line4, line5, null, UTC)
            );
        }
    }

    void testParsesLinesWithSeparators() {

    }

    @Test
    @DefaultDataContext
    void iirvAbsoluteTimeTest() {
        final IIRVVector iirv = new IIRVVector(lines, UTC);
        AbsoluteDate absoluteDateFromIIRV = iirv.getAbsoluteDate(2024);
        AbsoluteDate absoluteDateToCompare = new AbsoluteDate(
            2024, 1, 1,
            0, 0, 0, UTC);
        assertEquals(absoluteDateToCompare, absoluteDateFromIIRV);
    }

    @Test
    void iirvVectorCreationTests() {
        final IIRVVector iirv = new IIRVVector(lines, UTC);

        Vector3D iirvPos = iirv.getPositionVector();
        Vector3D iirvVel = iirv.getVelocityVector();

        assertEquals(new Vector3D(3038560, -3031452, 5261153), iirvPos);
        assertEquals(new Vector3D(4300791, 5897352, 909949), iirvVel);
    }


    @Test
    void coordinatesTest() {
        final IIRVVector iirv = new IIRVVector(lines, UTC);
        PVCoordinates pvCoordinates = iirv.getPVCoordinates();
        TimeStampedPVCoordinates timeStampedPVCoordinates = iirv.getTimeStampedPVCoordinates(1984);
        assertEquals(pvCoordinates.getPosition(), timeStampedPVCoordinates.getPosition());
        assertEquals(pvCoordinates.getVelocity(), timeStampedPVCoordinates.getVelocity());
    }

    @Test
    void iirvHumanReadableTest() {
        final IIRVVector iirv = new IIRVVector(lines, UTC);
        final List<String> iirvStrings = iirv.toIIRVStrings(true);
        final List<String> humanReadableStrings = iirv.toHumanReadableLines();
        for (int i = 0; i < 6; i++) {
            assertEquals(humanReadableStrings.get(i).replace("/", ""), iirvStrings.get(i));

        }
    }

    /**
     * Performs an assertion to check that the computed check sum matches the actual
     * check sum for a given line
     *
     * @param checkSum check sum term to generate the checksum
     * @param line     IIRV message line against which to validate the computed check sum
     */
    void assertChecksumIsValid(CheckSumTerm checkSum, String line) {
        int line2ComputedCheckSum = CheckSumTerm.computeChecksum(line.substring(0, line.length() - 3));
        assertEquals(checkSum.value(), line2ComputedCheckSum);
        assertTrue(checkSum.validateAgainstLineString(line));
    }
}