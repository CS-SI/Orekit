package org.orekit.files.ccsds;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/**
 * The set of time scales defined in Annex A of the ODM CCSDS standard 502.0-B-2.
 *
 * @author Evan Ward
 */
public enum CcsdsTimeScale {

    /** Greenwich Mean Sidereal Time */
    GMST {
        @Override
        public AbsoluteDate parseDate(final String date,
                                      final IERSConventions conventions,
                                      final AbsoluteDate missionReferenceDate)
                throws OrekitException {
            return new AbsoluteDate(date, TimeScalesFactory.getGMST(conventions, false));
        }
    },
    /** Global Positioning System */
    GPS {
        @Override
        public AbsoluteDate parseDate(final String date,
                                      final IERSConventions conventions,
                                      final AbsoluteDate missionReferenceDate) {
            return new AbsoluteDate(date, TimeScalesFactory.getGPS());
        }
    },
    /** Mission Elapsed Time */
    MET {
        @Override
        public AbsoluteDate parseDate(final String date,
                                      final IERSConventions conventions,
                                      final AbsoluteDate missionReferenceDate) {
            final DateTimeComponents clock = DateTimeComponents.parseDateTime(date);
            final double offset = clock.getDate().getYear() * Constants.JULIAN_YEAR +
                    clock.getDate().getDayOfYear() * Constants.JULIAN_DAY +
                    clock.getTime().getSecondsInUTCDay();
            return missionReferenceDate.shiftedBy(offset);
        }
    },
    /** Mission Relative Time */
    MRT {
        @Override
        public AbsoluteDate parseDate(final String date,
                                      final IERSConventions conventions,
                                      final AbsoluteDate missionReferenceDate) {
            final DateTimeComponents clock = DateTimeComponents.parseDateTime(date);
            final double offset = clock.getDate().getYear() * Constants.JULIAN_YEAR +
                    clock.getDate().getDayOfYear() * Constants.JULIAN_DAY +
                    clock.getTime().getSecondsInUTCDay();
            return missionReferenceDate.shiftedBy(offset);
        }
    },
    /** Spacecraft Clock. Not currently Implemented. */
    SCLK {
        @Override
        public AbsoluteDate parseDate(final String date,
                                      final IERSConventions conventions,
                                      final AbsoluteDate missionReferenceDate)
                throws OrekitException {
            throw new OrekitException(
                    OrekitMessages.CCSDS_TIME_SYSTEM_NOT_IMPLEMENTED,
                    "SCLK");
        }
    },
    /** International Atomic Time */
    TAI {
        @Override
        public AbsoluteDate parseDate(final String date,
                                      final IERSConventions conventions,
                                      final AbsoluteDate missionReferenceDate) {
            return new AbsoluteDate(date, TimeScalesFactory.getTAI());
        }
    },
    /** Barycentric Coordinate Time */
    TCB {
        @Override
        public AbsoluteDate parseDate(final String date,
                                      final IERSConventions conventions,
                                      final AbsoluteDate missionReferenceDate) {
            return new AbsoluteDate(date, TimeScalesFactory.getTCB());
        }
    },
    /** Barycentric Dynamical Time */
    TDB {
        @Override
        public AbsoluteDate parseDate(final String date,
                                      final IERSConventions conventions,
                                      final AbsoluteDate missionReferenceDate) {
            return new AbsoluteDate(date, TimeScalesFactory.getTDB());
        }
    },
    /** Geocentric Coordinate Time */
    TCG {
        @Override
        public AbsoluteDate parseDate(final String date,
                                      final IERSConventions conventions,
                                      final AbsoluteDate missionReferenceDate) {
            return new AbsoluteDate(date, TimeScalesFactory.getTCG());
        }
    },
    /** Terrestrial Time */
    TT {
        @Override
        public AbsoluteDate parseDate(final String date,
                                      final IERSConventions conventions,
                                      final AbsoluteDate missionReferenceDate) {
            return new AbsoluteDate(date, TimeScalesFactory.getTT());
        }
    },
    /** Univeral Time */
    UT1 {
        @Override
        public AbsoluteDate parseDate(final String date,
                                      final IERSConventions conventions,
                                      final AbsoluteDate missionReferenceDate)
                throws OrekitException {
            return new AbsoluteDate(date, TimeScalesFactory.getUT1(conventions, false));
        }
    },
    /** Universal Coordinated Time */
    UTC {
        @Override
        public AbsoluteDate parseDate(final String date,
                                      final IERSConventions conventions,
                                      final AbsoluteDate missionReferenceDate)
                throws OrekitException {
            return new AbsoluteDate(date, TimeScalesFactory.getUTC());
        }
    };

    /**
     * Parse a date in this time scale.
     *
     * @param date                 a CCSDS date string.
     * @param conventions          IERS conventions for {@link #UT1} and {@link #GMST}.
     * @param missionReferenceDate epoch for {@link #MET} and {@link #MRT}.
     * @return parsed {@code date}.
     * @throws OrekitException if an {@link AbsoluteDate} cannot be created from {@code
     *                         date} in this time scale.
     */
    public abstract AbsoluteDate parseDate(String date,
                                           IERSConventions conventions,
                                           AbsoluteDate missionReferenceDate)
            throws OrekitException;

    /**
     * Check if {@code timeScale} is one of the values supported by this enum.
     *
     * @param timeScale specifier.
     * @return {@code true} if {@link #valueOf(String)} will not throw an exception with
     * the same string.
     */
    public static boolean contains(final String timeScale) {
        for (final CcsdsTimeScale scale : values()) {
            if (scale.name().equals(timeScale)) {
                return true;
            }
        }
        return false;
    }

}
