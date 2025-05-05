package org.orekit.files.iirv;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.files.iirv.terms.CheckSumTerm;
import org.orekit.files.iirv.terms.CoordinateSystemTerm;
import org.orekit.files.iirv.terms.CrossSectionalAreaTerm;
import org.orekit.files.iirv.terms.DataSourceTerm;
import org.orekit.files.iirv.terms.DayOfYearTerm;
import org.orekit.files.iirv.terms.DragCoefficientTerm;
import org.orekit.files.iirv.terms.IIRVTermUtils;
import org.orekit.files.iirv.terms.MassTerm;
import org.orekit.files.iirv.terms.MessageClassTerm;
import org.orekit.files.iirv.terms.MessageEndConstantTerm;
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
import org.orekit.files.iirv.terms.SpareConstantTerm;
import org.orekit.files.iirv.terms.SupportIdCodeTerm;
import org.orekit.files.iirv.terms.TransferTypeConstantTerm;
import org.orekit.files.iirv.terms.VectorTypeTerm;
import org.orekit.files.iirv.terms.VehicleIdCodeTerm;
import org.orekit.files.iirv.terms.VelocityVectorComponentTerm;
import org.orekit.files.iirv.terms.base.DoubleValuedIIRVTerm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests various functionality related to individual IIRV terms
 */
public class IIRVTermTest {

    @BeforeEach
    public void setUp() {
        // Sets the root of data to read
        Utils.setDataRoot("regular-data");
    }

    /**
     * Test initialization of {@link org.orekit.files.iirv.terms.CheckSumTerm}
     */
    @Test
    void testCheckSumTerm() {
        // Verify valid inputs are accepted
        assertEquals(12, new CheckSumTerm(12).value());
        assertEquals(12, new CheckSumTerm("012").value());
        assertEquals("012", new CheckSumTerm("012").toEncodedString());

        // Verify errors are thrown for incorrect inputs
        assertThrows(OrekitIllegalArgumentException.class, () -> new CheckSumTerm(1000));
        assertThrows(OrekitIllegalArgumentException.class, () -> new CheckSumTerm("x"));
        assertThrows(OrekitIllegalArgumentException.class, () -> new CheckSumTerm(-12));

        // Verify checksum validation
        assertTrue(CheckSumTerm.validateLineCheckSum("00000000000000000-0000000001"));
        assertFalse(CheckSumTerm.validateLineCheckSum("00000000000000000-0000000000"));
        assertTrue(CheckSumTerm.validateLineCheckSum("00000000000000000 0000000000"));
        assertFalse(CheckSumTerm.validateLineCheckSum("00000000000000000 0000000001"));

        // Verify checksum computation
        MessageClassTerm term1 = MessageClassTerm.NOMINAL; // "10" = 1 + 0 = 1
        DayOfYearTerm term2 = new DayOfYearTerm(202); // "202" = 2+0+2 = 4
        VelocityVectorComponentTerm term3 = new VelocityVectorComponentTerm(-5.211); // "-5.211" = 1+5+2+1+1 = 10

        assertEquals(1, CheckSumTerm.fromIIRVTerms(term1).value());
        assertEquals(4, CheckSumTerm.fromIIRVTerms(term2).value());
        assertEquals(10, CheckSumTerm.fromIIRVTerms(term3).value());
        assertEquals(15, CheckSumTerm.fromIIRVTerms(term1, term2, term3).value());
    }

    /**
     * Test initialization of {@link org.orekit.files.iirv.terms.CoordinateSystemTerm}
     */
    @Test
    void testCoordinateSystemTerm() {


        // Check all the valid values
        assertEquals(new CoordinateSystemTerm("1"), CoordinateSystemTerm.GEOCENTRIC_TRUE_OF_DATE_ROTATING);
        assertEquals(new CoordinateSystemTerm("2"), CoordinateSystemTerm.GEOCENTRIC_MEAN_B1950);
        assertEquals(new CoordinateSystemTerm("3"), CoordinateSystemTerm.HELIOCENTRIC_B1950);
        assertEquals(new CoordinateSystemTerm("4"), CoordinateSystemTerm.JPL_RESERVED_1);
        assertEquals(new CoordinateSystemTerm("5"), CoordinateSystemTerm.JPL_RESERVED_2);
        assertEquals(new CoordinateSystemTerm("6"), CoordinateSystemTerm.GEOCENTRIC_MEAN_OF_J2000);
        assertEquals(new CoordinateSystemTerm("7"), CoordinateSystemTerm.HELIOCENTRIC_J2000);

        // Verify initialization is same from string/integer
        assertEquals(new CoordinateSystemTerm("6"), new CoordinateSystemTerm(6));

        // hashCode
        assertEquals(new CoordinateSystemTerm("6").hashCode(), new CoordinateSystemTerm(6).hashCode());

        // Verify errors are thrown for incorrect inputs
        assertThrows(OrekitIllegalArgumentException.class, () -> new CoordinateSystemTerm("0"));
        assertThrows(OrekitIllegalArgumentException.class, () -> new CoordinateSystemTerm("-1"));
        assertThrows(OrekitIllegalArgumentException.class, () -> new CoordinateSystemTerm(" 1"));
        assertThrows(OrekitIllegalArgumentException.class, () -> new CoordinateSystemTerm("1 "));
        assertThrows(OrekitIllegalArgumentException.class, () -> new CoordinateSystemTerm(0));
        assertThrows(OrekitIllegalArgumentException.class, () -> new CoordinateSystemTerm(-1));
        assertThrows(OrekitIllegalArgumentException.class, () -> new CoordinateSystemTerm(8));
        assertThrows(OrekitIllegalArgumentException.class, () -> new CoordinateSystemTerm("8"));

        // Verify Orekit frame retrieval
        assertEquals("GTOD/2010 simple EOP", CoordinateSystemTerm.GEOCENTRIC_TRUE_OF_DATE_ROTATING.getFrame().toString());
        assertEquals("EME2000", CoordinateSystemTerm.GEOCENTRIC_MEAN_OF_J2000.getFrame().toString());
        assertEquals("Sun/inertial", CoordinateSystemTerm.HELIOCENTRIC_J2000.getFrame().toString());

        // Unmapped frames
        assertThrows(OrekitException.class, CoordinateSystemTerm.GEOCENTRIC_MEAN_B1950::getFrame);
        assertThrows(OrekitException.class, CoordinateSystemTerm.HELIOCENTRIC_B1950::getFrame);
        assertThrows(OrekitException.class, CoordinateSystemTerm.JPL_RESERVED_1::getFrame);
        assertThrows(OrekitException.class, CoordinateSystemTerm.JPL_RESERVED_2::getFrame);
        assertThrows(OrekitException.class, CoordinateSystemTerm.JPL_RESERVED_2::getFrame);
    }

    /**
     * Test initialization of {@link org.orekit.files.iirv.terms.CrossSectionalAreaTerm}
     */
    @Test
    void testCrossSectionalAreaTerm() {
        assertEquals(CrossSectionalAreaTerm.UNUSED, new CrossSectionalAreaTerm(0));

        assertEquals(new CrossSectionalAreaTerm("23181"), new CrossSectionalAreaTerm(231.81));
        assertEquals(new CrossSectionalAreaTerm("23181"), new CrossSectionalAreaTerm(231.811));
        assertEquals(new CrossSectionalAreaTerm("23181"), new CrossSectionalAreaTerm(231.808));

        assertEquals(new CrossSectionalAreaTerm("23181").hashCode(), new CrossSectionalAreaTerm(231.808).hashCode());
    }

    /**
     * Test initialization of {@link org.orekit.files.iirv.terms.DataSourceTerm}
     */
    @Test
    void testDataSourceTerm() {
        assertEquals(DataSourceTerm.NOMINAL, new DataSourceTerm("1"));
        assertEquals(DataSourceTerm.NOMINAL, new DataSourceTerm(1));
        assertEquals(DataSourceTerm.REAL_TIME, new DataSourceTerm("2"));
        assertEquals(DataSourceTerm.REAL_TIME, new DataSourceTerm(2));
        assertEquals(DataSourceTerm.OFFLINE, new DataSourceTerm("3"));
        assertEquals(DataSourceTerm.OFFLINE, new DataSourceTerm(3));
        assertEquals(DataSourceTerm.OFFLINE_MEAN, new DataSourceTerm("4"));
        assertEquals(DataSourceTerm.OFFLINE_MEAN, new DataSourceTerm(4));
    }

    /**
     * Test initialization of {@link org.orekit.files.iirv.terms.DragCoefficientTerm}
     */
    @Test
    void testDragCoefficientTerm() {
        assertEquals(new DragCoefficientTerm("1234"), new DragCoefficientTerm(12.34));
        assertEquals(DragCoefficientTerm.UNUSED, new DragCoefficientTerm(0));

    }

    /**
     * Test initialization of {@link org.orekit.files.iirv.terms.MassTerm}
     */
    @Test
    void testMassTerm() {
        assertEquals(new MassTerm("00010000"), new MassTerm(1000.0));
        assertEquals(MassTerm.UNUSED, new MassTerm(0));
    }

    /**
     * Test initialization of {@link org.orekit.files.iirv.terms.MessageClassTerm}
     */
    @Test
    void testMessageClassTerm() {
        assertEquals(MessageClassTerm.NOMINAL, new MessageClassTerm(10));
        assertEquals(MessageClassTerm.NOMINAL, new MessageClassTerm("10"));
        assertEquals(MessageClassTerm.INFLIGHT_UPDATE, new MessageClassTerm(15));
        assertEquals(MessageClassTerm.INFLIGHT_UPDATE, new MessageClassTerm("15"));

        // Check rejected
        assertThrows(OrekitIllegalArgumentException.class, () -> new MessageClassTerm(1));
        assertThrows(OrekitIllegalArgumentException.class, () -> new MessageClassTerm("1"));
        assertThrows(OrekitIllegalArgumentException.class, () -> new MessageClassTerm(-10));
    }

    /**
     * Test initialization of {@link org.orekit.files.iirv.terms.MessageEndConstantTerm}
     */
    @Test
    void testMessageEndConstantTerm() {
        assertEquals(new MessageEndConstantTerm().value(), "ITERM");
        assertEquals(new MessageEndConstantTerm().value(), MessageEndConstantTerm.MESSAGE_END_TERM_STRING);
    }

    /**
     * Test initialization of {@link org.orekit.files.iirv.terms.MessageIDTerm}
     */
    @Test
    void testMessageIDTerm() {
        assertEquals(new MessageIDTerm(9381225), new MessageIDTerm("9381225"));

        // Check non-number within string
        assertThrows(OrekitIllegalArgumentException.class, () -> new MessageIDTerm("938122A"));
    }

    /**
     * Test initialization of {@link MessageSourceTerm}
     */
    @Test
    void testMessageSourceConstantTerm() {
        assertEquals("0", MessageSourceTerm.DEFAULT.value());
        assertEquals("0", new MessageSourceTerm("0").value());
        assertEquals("A", new MessageSourceTerm("A").value());
        assertEquals("a", new MessageSourceTerm("a").value());

        assertThrows(OrekitIllegalArgumentException.class, () -> new MessageSourceTerm(","));
        assertThrows(OrekitIllegalArgumentException.class, () -> new MessageSourceTerm(""));
    }

    /**
     * Test initialization of {@link org.orekit.files.iirv.terms.MessageStartConstantTerm}
     */
    @Test
    void testMessageStartConstantTerm() {
        assertEquals("GIIRV", new MessageStartConstantTerm().value());
    }

    /**
     * Test initialization of {@link MessageTypeTerm}
     */
    @Test
    void testMessageTypeConstantTerm() {
        assertEquals(new MessageTypeTerm("03"), MessageTypeTerm.DEFAULT);
        assertEquals("03", new MessageTypeTerm("03").value());
        assertEquals("B9", new MessageTypeTerm("B9").value());
        assertEquals("a ", new MessageTypeTerm("a ").value());

        assertThrows(OrekitIllegalArgumentException.class, () -> new MessageTypeTerm("$2"));
        assertThrows(OrekitIllegalArgumentException.class, () -> new MessageTypeTerm("222"));
        assertThrows(OrekitIllegalArgumentException.class, () -> new MessageTypeTerm("q"));
    }

    /**
     * Test initialization of {@link org.orekit.files.iirv.terms.OriginatorRoutingIndicatorTerm}
     */
    @Test
    void testOriginatorRoutingIndicatorTerm() {
        assertEquals(new OriginatorRoutingIndicatorTerm("GCQU"), OriginatorRoutingIndicatorTerm.GCQU);
        assertEquals(new OriginatorRoutingIndicatorTerm("GAQD"), OriginatorRoutingIndicatorTerm.GAQD);
    }

    /**
     * Test initialization of {@link org.orekit.files.iirv.terms.OriginIdentificationTerm}
     */
    @Test
    void testOriginIdentificationTerm() {
        assertEquals(new OriginIdentificationTerm(" "), OriginIdentificationTerm.GSFC);
        assertEquals(new OriginIdentificationTerm("Z"), OriginIdentificationTerm.WLP);
        assertEquals(new OriginIdentificationTerm("E"), OriginIdentificationTerm.ETR);
        assertEquals(new OriginIdentificationTerm("L"), OriginIdentificationTerm.JPL);
        assertEquals(new OriginIdentificationTerm("W"), OriginIdentificationTerm.WTR);
        assertEquals(new OriginIdentificationTerm("J"), OriginIdentificationTerm.JSC);
        assertEquals(new OriginIdentificationTerm("P"), OriginIdentificationTerm.PMR);
        assertEquals(new OriginIdentificationTerm("A"), OriginIdentificationTerm.CSTC);
        assertEquals(new OriginIdentificationTerm("K"), OriginIdentificationTerm.KMR);
        assertEquals(new OriginIdentificationTerm("C"), OriginIdentificationTerm.CNES);

        // Check can't initialize from the name of the originator string
        assertThrows(OrekitIllegalArgumentException.class, () -> new OriginIdentificationTerm("GSFC"));
    }

    /**
     * Test initialization of {@link org.orekit.files.iirv.terms.PositionVectorComponentTerm} and
     * {@link org.orekit.files.iirv.terms.base.DoubleValuedIIRVTerm}
     */
    @Test
    void testPositionVectorComponentTerm() {

        // Verify: initializes correctly from positive double
        assertEquals(new PositionVectorComponentTerm(1800799.012), new PositionVectorComponentTerm(" 000001800799"));

        // Verify: initializes correctly from negative double
        assertEquals(new PositionVectorComponentTerm(-2068482.799), new PositionVectorComponentTerm("-000002068483"));

        // Verify: initializes correctly from positive integer
        final int posComponentInt = 2954112;
        final long posComponentLong = 2954112L;
        final String posComponentString = " 000002954112";
        assertEquals(new PositionVectorComponentTerm(posComponentInt), new PositionVectorComponentTerm(posComponentString));

        // Verify: initializes correctly from positive long
        assertEquals(new PositionVectorComponentTerm(posComponentLong), new PositionVectorComponentTerm(posComponentInt));

        // Verify: initializes correctly from negative integer
        assertEquals(new PositionVectorComponentTerm(-2954112), new PositionVectorComponentTerm("-000002954112"));

        // Verify: toInt method works as expected
        assertEquals(posComponentInt, new PositionVectorComponentTerm(posComponentLong).toInt());

        // Verify: value computation from integer (never used, but doesn't hurt to check in case of updates to term formats)
        assertEquals(1234, DoubleValuedIIRVTerm.computeValueFromString("1234", 0));

        // Check for error: initialize with wrong number of characters (too few)
        assertThrows(OrekitIllegalArgumentException.class, () -> new PositionVectorComponentTerm(" 00000799"));

        // Check for error: initialize with wrong number of characters (too many)
        assertThrows(OrekitIllegalArgumentException.class, () -> new PositionVectorComponentTerm(" 00000000000000799"));

        // Check for error: initialize with non-numeric character
        assertThrows(OrekitIllegalArgumentException.class, () -> new PositionVectorComponentTerm(" 0000079a"));

        // Check for error: initialize with no sign character
        assertThrows(OrekitIllegalArgumentException.class, () -> new PositionVectorComponentTerm("0000001800799"));

        // Check for error: initialize with number that is too large for the maximum precision
        assertThrows(OrekitIllegalArgumentException.class, () -> new PositionVectorComponentTerm(99999999999991L));
        assertThrows(OrekitIllegalArgumentException.class, () -> new PositionVectorComponentTerm(-99999999999991L));

        // Check that +/- zero are the same
        PositionVectorComponentTerm positiveZero = new PositionVectorComponentTerm(" 000000000000");
        PositionVectorComponentTerm negativeZero = new PositionVectorComponentTerm("-000000000000");
        assertEquals(positiveZero.toInt(), negativeZero.toInt());
        assertEquals(positiveZero.value(), negativeZero.value());
        assertEquals(positiveZero.toEncodedString(), negativeZero.toEncodedString());
    }

    /**
     * Test initialization of {@link org.orekit.files.iirv.terms.RoutingIndicatorTerm}
     */
    @Test
    void testRoutingIndicatorTerm() {
        assertEquals(new RoutingIndicatorTerm("GSFC"), RoutingIndicatorTerm.GSFC);
        assertEquals(new RoutingIndicatorTerm(" WLP"), RoutingIndicatorTerm.WLP);
        assertEquals(new RoutingIndicatorTerm(" ETR"), RoutingIndicatorTerm.ETR);
        assertEquals(new RoutingIndicatorTerm(" JPL"), RoutingIndicatorTerm.JPL);
        assertEquals(new RoutingIndicatorTerm(" WTR"), RoutingIndicatorTerm.WTR);
        assertEquals(new RoutingIndicatorTerm(" JSC"), RoutingIndicatorTerm.JSC);
        assertEquals(new RoutingIndicatorTerm(" PMR"), RoutingIndicatorTerm.PMR);
        assertEquals(new RoutingIndicatorTerm("CSTC"), RoutingIndicatorTerm.CSTC);
        assertEquals(new RoutingIndicatorTerm(" KMR"), RoutingIndicatorTerm.KMR);
        assertEquals(new RoutingIndicatorTerm("CNES"), RoutingIndicatorTerm.CNES);
        assertEquals(new RoutingIndicatorTerm("MANY"), RoutingIndicatorTerm.MANY);

        // Check can't initialize from the name of the originator string
        assertThrows(OrekitIllegalArgumentException.class, () -> new RoutingIndicatorTerm(" "));

        // Check that a space is needed before three-character ones
        assertThrows(OrekitIllegalArgumentException.class, () -> new RoutingIndicatorTerm("KMR"));
    }

    /**
     * Test initialization of {@link org.orekit.files.iirv.terms.SequenceNumberTerm}
     */
    @Test
    void testSequenceNumberTerm() {
        assertEquals(new SequenceNumberTerm("000"), new SequenceNumberTerm(0));
        assertEquals(new SequenceNumberTerm("123"), new SequenceNumberTerm(123));

        // Check min/max
        assertThrows(OrekitIllegalArgumentException.class, () -> new SequenceNumberTerm(-1));
        assertThrows(OrekitIllegalArgumentException.class, () -> new SequenceNumberTerm(1000));
    }

    /**
     * Test initialization of {@link org.orekit.files.iirv.terms.SolarReflectivityCoefficientTerm}
     */
    @Test
    void testSolarReflectivityCoefficientTerm() {

        // Test string vs int
        assertEquals(new SolarReflectivityCoefficientTerm(" 1000000"), new SolarReflectivityCoefficientTerm(1));

        // Test unused
        assertEquals(SolarReflectivityCoefficientTerm.UNUSED, new SolarReflectivityCoefficientTerm(0));

        // Test negative
        assertEquals(new SolarReflectivityCoefficientTerm("-1000001"), new SolarReflectivityCoefficientTerm(-1.000001));

        // Test rounding
        assertEquals(new SolarReflectivityCoefficientTerm("-1000001"), new SolarReflectivityCoefficientTerm(-1.0000006));

        // Test overflow checking
        assertThrows(OrekitIllegalArgumentException.class, () -> new SolarReflectivityCoefficientTerm(10));
    }

    /**
     * Test initialization of {@link org.orekit.files.iirv.terms.SpareConstantTerm}
     */
    @Test
    void testSpareConstantTerm() {
        assertEquals(new SpareConstantTerm().value(), " ");
        assertEquals(new SpareConstantTerm().value(), SpareConstantTerm.SPARE_TERM_STRING);
    }

    /**
     * Test initialization of {@link org.orekit.files.iirv.terms.SupportIdCodeTerm}
     */
    @Test
    void testSupportIdCodeTerm() {
        assertEquals(new SupportIdCodeTerm("3254"), new SupportIdCodeTerm(3254));

        // Check min/max
        assertThrows(OrekitIllegalArgumentException.class, () -> new SupportIdCodeTerm(-1));
        assertThrows(OrekitIllegalArgumentException.class, () -> new SupportIdCodeTerm(10000));
    }

    /**
     * Test initialization of {@link org.orekit.files.iirv.terms.TransferTypeConstantTerm}
     */
    @Test
    void testTransferTypeConstantTerm() {
        assertEquals(new TransferTypeConstantTerm().value(), "1");
        assertEquals(new TransferTypeConstantTerm().value(), TransferTypeConstantTerm.TRANSFER_TYPE_TERM_STRING);
    }

    /**
     * Test initialization of {@link org.orekit.files.iirv.terms.VectorTypeTerm}
     */
    @Test
    void testVectorTypeTerm() {
        assertEquals(VectorTypeTerm.FREE_FLIGHT, new VectorTypeTerm("1"));
        assertEquals(VectorTypeTerm.FORCED, new VectorTypeTerm("2"));
        assertEquals(VectorTypeTerm.SPARE3, new VectorTypeTerm("3"));
        assertEquals(VectorTypeTerm.MANEUVER_IGNITION, new VectorTypeTerm("4"));
        assertEquals(VectorTypeTerm.MANEUVER_CUTOFF, new VectorTypeTerm("5"));
        assertEquals(VectorTypeTerm.REENTRY, new VectorTypeTerm("6"));
        assertEquals(VectorTypeTerm.POWERED_FLIGHT, new VectorTypeTerm("7"));
        assertEquals(VectorTypeTerm.STATIONARY, new VectorTypeTerm("8"));
        assertEquals(VectorTypeTerm.SPARE9, new VectorTypeTerm("9"));

        // Init from integer
        assertEquals(new VectorTypeTerm(8), new VectorTypeTerm("8"));
    }

    /**
     * Test initialization of {@link org.orekit.files.iirv.terms.VehicleIdCodeTerm}
     */
    @Test
    void testVehicleIdCodeTerm() {
        assertEquals(VehicleIdCodeTerm.DEFAULT, new VehicleIdCodeTerm(1));
        assertEquals(new VehicleIdCodeTerm(2), new VehicleIdCodeTerm("02"));

        // 0 not allowed
        assertThrows(OrekitIllegalArgumentException.class, () -> new VehicleIdCodeTerm(0));

        // Single digit
        assertThrows(OrekitIllegalArgumentException.class, () -> new VehicleIdCodeTerm("1"));
    }

    /**
     * Test initialization of {@link org.orekit.files.iirv.terms.VelocityVectorComponentTerm} and
     * {@link org.orekit.files.iirv.terms.base.DoubleValuedIIRVTerm}
     */
    @Test
    void testVelocityVectorComponentTerm() {

        // Verify: initializes correctly from positive double
        assertEquals(new VelocityVectorComponentTerm(1800.799), new VelocityVectorComponentTerm(" 000001800799"));

        // Verify: initializes correctly from negative double
        assertEquals(new VelocityVectorComponentTerm(-1800.799), new VelocityVectorComponentTerm("-000001800799"));

        // Verify: initializes correctly from positive integer
        assertEquals(new VelocityVectorComponentTerm(1800), new VelocityVectorComponentTerm(" 000001800000"));

        // Verify: initializes correctly from positive long
        assertEquals(new VelocityVectorComponentTerm(1800L), new VelocityVectorComponentTerm(1800));

        // Verify: initializes correctly from negative integer
        assertEquals(new VelocityVectorComponentTerm(-1800), new VelocityVectorComponentTerm("-000001800000"));

        // Verify: correctly rounds up to integer
        assertEquals(new VelocityVectorComponentTerm(1800.99999), new VelocityVectorComponentTerm(" 000001801000"));

        // Verify: correctly rounds up to ones place
        assertEquals(new VelocityVectorComponentTerm(1800.7999), new VelocityVectorComponentTerm(" 000001800800"));

        // Verify: correctly rounds down to integer
        assertEquals(new VelocityVectorComponentTerm(1800.00001), new VelocityVectorComponentTerm(" 000001800000"));

        // Verify: correctly rounds down to floating point
        assertEquals(new VelocityVectorComponentTerm(1800.7991), new VelocityVectorComponentTerm(" 000001800799"));

        // Verify: correctly rounds up to floating point
        assertEquals(new VelocityVectorComponentTerm(1800.7925), new VelocityVectorComponentTerm(" 000001800793"));

        // Verify: correctly rounds negative number
        assertEquals(new VelocityVectorComponentTerm(-1800.7999), new VelocityVectorComponentTerm("-000001800800"));

        // Verify: same hash code
        assertEquals(new VelocityVectorComponentTerm(1800.799).hashCode(), new VelocityVectorComponentTerm(" 000001800799").hashCode());

        // Check that +/- zero are the same
        VelocityVectorComponentTerm posZeroOne = new VelocityVectorComponentTerm(" 000000000000");
        VelocityVectorComponentTerm posZeroTwo = new VelocityVectorComponentTerm(1e-16);
        VelocityVectorComponentTerm negZeroOne = new VelocityVectorComponentTerm("-000000000000");
        VelocityVectorComponentTerm negZeroTwo = new VelocityVectorComponentTerm(-1e-16);
        assertEquals(posZeroOne.toEncodedString(), posZeroTwo.toEncodedString());
        assertEquals(posZeroOne.toEncodedString(), negZeroOne.toEncodedString());
        assertEquals(posZeroOne.toEncodedString(), negZeroTwo.toEncodedString());

        // Check for error: initialize with wrong number of characters (too few)
        assertThrows(OrekitIllegalArgumentException.class, () -> new VelocityVectorComponentTerm(" 00000799"));

        // Check for error: initialize with wrong number of characters (too many)
        assertThrows(OrekitIllegalArgumentException.class, () -> new VelocityVectorComponentTerm(" 00000000000000799"));

        // Check for error: initialize with non-numeric character
        assertThrows(OrekitIllegalArgumentException.class, () -> new VelocityVectorComponentTerm(" 0000079a"));

        // Check for error: initialize with no sign character
        assertThrows(OrekitIllegalArgumentException.class, () -> new VelocityVectorComponentTerm("0000001800799"));

        // Check for error: initialize with number that is too large for the maximum precision
        assertThrows(OrekitIllegalArgumentException.class, () -> new VelocityVectorComponentTerm(9999999999.999));
        assertThrows(OrekitIllegalArgumentException.class, () -> new VelocityVectorComponentTerm(-9999999999.999));


    }

    /**
     * Test line padding manipulation, implemented in {@link org.orekit.files.iirv.terms.IIRVTermUtils#addPadding}
     */
    @Test
    void testLinePadding() {
        final String testString = "X";

        assertEquals("--X", IIRVTermUtils.addPadding(testString, '-', 3, true));
        assertEquals("X--", IIRVTermUtils.addPadding(testString, '-', 3, false));
        assertEquals("---", IIRVTermUtils.addPadding("", '-', 3, false));

        // Negative value input for size
        assertThrows(OrekitException.class, () -> IIRVTermUtils.addPadding(testString, '-', -1, true));

    }
}
