package org.orekit.files.iirv;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.files.iirv.terms.DayOfYearTerm;
import org.orekit.files.iirv.terms.VectorEpochTerm;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.UTCScale;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests how time variables are handled in IIRV terms
 */
public class IIRVTimeTest {
    private static UTCScale UTC;

    /** Sample TimeComponents object to use in tests */
    private final TimeComponents testTimeComponents = new TimeComponents(17, 1, 22.231);

    @BeforeEach
    public void setUp() {
        // Sets the root of data to read
        Utils.setDataRoot("regular-data");
        UTC = TimeScalesFactory.getUTC();
    }

    /**
     * Test initialization of {@link DayOfYearTerm}
     */
    @Test
    @DefaultDataContext
    void testDayOfYearTerm() {
        final UTCScale utc = TimeScalesFactory.getUTC();

        int testDayOfYearAfterLeapDay = 344; // Day of year that occurs after leap day for tests: 344 (Dec. 9, 2024 )
        int year = 2024;  // Use 2024 as the year in all tests

        DayOfYearTerm doyTerm = new DayOfYearTerm(testDayOfYearAfterLeapDay);
        AbsoluteDate absoluteDate4 = new AbsoluteDate(year, 12, 9, utc);

        // Check the initialized terms are what we expect
        Assertions.assertEquals(doyTerm, new DayOfYearTerm(absoluteDate4, UTC)); // Constructed from AbsoluteDate
        Assertions.assertEquals(doyTerm, new DayOfYearTerm("344"));// Constructed from String

        // Check leap year Dec. 31
        DayOfYearTerm dayOfYearTermAfterLeapDay = new DayOfYearTerm("366");
        DateComponents dec31LeapYear = dayOfYearTermAfterLeapDay.getDateComponents(2024);
        assertEquals(31, dec31LeapYear.getDay());
        assertEquals(12, dec31LeapYear.getMonth());
        assertEquals(366, dec31LeapYear.getDayOfYear());

        // Check date components throw error for a leap year (366 days)
        assertThrows(OrekitIllegalArgumentException.class,
            () -> new DayOfYearTerm("366").getDateComponents(2023)
        );

        // Check rejected outside date range
        assertThrows(OrekitIllegalArgumentException.class,
            () -> new DayOfYearTerm("377").getDateComponents(2023));

        // Check rejected double value
        assertThrows(OrekitIllegalArgumentException.class,
            () -> new DayOfYearTerm("2.2")); // reject double value

    }

    @Test
    @DefaultDataContext
    void testVectorEpochTerm() {

        // Construct from TimeComponents
        final VectorEpochTerm vectorEpoch = new VectorEpochTerm(testTimeComponents);

        // Construct from String
        VectorEpochTerm vectorEpochFromStr = new VectorEpochTerm(vectorEpoch.toEncodedString());

        // Construct from AbsoluteDate
        AbsoluteDate absoluteDate = new AbsoluteDate(
            DateComponents.J2000_EPOCH,
            testTimeComponents,
            TimeScalesFactory.getUTC()
        );
        VectorEpochTerm vectorEpochFromAbsoluteDate = new VectorEpochTerm(absoluteDate, UTC);

        Assertions.assertEquals(vectorEpoch, vectorEpochFromStr);
        Assertions.assertEquals(vectorEpoch, vectorEpochFromAbsoluteDate);
        Assertions.assertEquals("170122231", vectorEpoch.toEncodedString());
        Assertions.assertEquals("17", vectorEpoch.hh());
        Assertions.assertEquals("01", vectorEpoch.mm());
        Assertions.assertEquals("22", vectorEpoch.ss());

        Assertions.assertEquals(vectorEpoch.toEncodedString(), vectorEpochFromStr.toEncodedString()); // Check consistency
    }


    @Test
    void testTimeRounding() {
        // Millisecond rounding: 17:01:22.231 -> 17:01:22.232 (up) / 17:01:22.231 (down)
        final VectorEpochTerm vectorEpochMillisecond = new VectorEpochTerm(testTimeComponents);
        verifyRoundUpAndDown(vectorEpochMillisecond, "170122231", "170122232");

        // Second rounding: 17:01:22.999 -> 17:01:23.000 (up) / 17:01:22.999 (down)
        final VectorEpochTerm vectorEpochSecond = new VectorEpochTerm("170102999");
        verifyRoundUpAndDown(vectorEpochSecond, "170102999", "170103000");

        // Minute rounding: 17:01:59.999 -> 17:02:00.000 (up) / 17:01:59.999 (down)
        final VectorEpochTerm vectorEpochMinute = new VectorEpochTerm("170159999");
        verifyRoundUpAndDown(vectorEpochMinute, "170159999", "170200000");

        // Hour rounding: 17:59:59.999 -> 18:00:00.000 (up) / 17:59:59.999 (down)
        final VectorEpochTerm vectorEpochHour = new VectorEpochTerm("175959999");
        verifyRoundUpAndDown(vectorEpochHour, "175959999", "180000000");

        // Test day rounding: 23:59:59.999 -> 00:00:00.000 (up) / 23:59:59.999 (down)
        final VectorEpochTerm vectorEpochDay = new VectorEpochTerm("235959999");
        verifyRoundUpAndDown(vectorEpochDay, "235959999", "000000000");
    }

    private void verifyRoundUpAndDown(final VectorEpochTerm originalVectorEpoch,
                                      final String roundedDown,
                                      final String roundedUp) {
        AbsoluteDate absoluteDate = new AbsoluteDate(
            DateComponents.J2000_EPOCH, // arbitrary date
            originalVectorEpoch.value(),
            TimeScalesFactory.getUTC());
        Instant instant = absoluteDate.toInstant(TimeScalesFactory.getTimeScales());

        // Check rounding down
        AbsoluteDate absoluteDateRoundDown = new AbsoluteDate(instant.plus(Duration.ofNanos(400000)));
        VectorEpochTerm epochRoundDown = new VectorEpochTerm(absoluteDateRoundDown, UTC);
        VectorEpochTerm epochRoundDownExpected = new VectorEpochTerm(roundedDown);
        Assertions.assertEquals(epochRoundDownExpected, epochRoundDown);
        Assertions.assertEquals(epochRoundDownExpected, originalVectorEpoch);

        // Check rounding up
        AbsoluteDate absoluteDateRoundUp = new AbsoluteDate(instant.plus(Duration.ofNanos(600000)));
        VectorEpochTerm epochRoundUp = new VectorEpochTerm(absoluteDateRoundUp, UTC);
        VectorEpochTerm epochRoundUpExpected = new VectorEpochTerm(roundedUp);
        Assertions.assertEquals(epochRoundUpExpected, epochRoundUp);

    }

}
