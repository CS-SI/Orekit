/* Copyright 2022-2026 Luc Maisonobe
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
package org.orekit.files.sinex.orbex;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.sinex.ParseInfo;
import org.orekit.gnss.SatInSystem;
import org.orekit.time.DateTimeComponents;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Predicates for ephemeris data blocks.
 * @author Luc Maisonobe
 * @since 14.0
 */
enum EphemerisDataPredicate implements Predicate<OrbexParseInfo> {

    /** Predicate for time tag line. */
    TIME_TAG(RegexParts.LINE_START + "##" +
             RegexParts.STORED_FOUR_DIGITS + RegexParts.STORED_INTEGER + RegexParts.STORED_INTEGER +
             RegexParts.STORED_INTEGER + RegexParts.STORED_INTEGER + RegexParts.STORED_SINGLE_REAL +
             RegexParts.STORED_INTEGER + RegexParts.LINE_END,
             -1) {

        /** {@inheritDoc} */
        @Override
        protected void store(final Matcher matcher, final double[] columns, final OrbexParseInfo parseInfo) {
            parseInfo.timeTag(new DateTimeComponents(Integer.parseInt(matcher.group(1)),
                                                     Integer.parseInt(matcher.group(2)),
                                                     Integer.parseInt(matcher.group(3)),
                                                     Integer.parseInt(matcher.group(4)),
                                                     Integer.parseInt(matcher.group(5)),
                                                     Double.parseDouble(matcher.group(6))),
                              Integer.parseInt(matcher.group(7)));
        }

    },

    /** Predicate for PCS record. */
    PCS(RegexParts.LINE_START + " PCS" + RegexParts.STORED_SAT_ID +
        RegexParts.IGNORED_FLAGS +
        RegexParts.STORED_ONE_DIGIT + RegexParts.STORED_MULTIPLE_REALS + RegexParts.LINE_END,
        2, 3, 4, 7, 8) {
        @Override
        protected void store(final Matcher matcher, final double[] columns, final OrbexParseInfo parseInfo) {
            final SatInSystem satId = new SatInSystem(matcher.group(1));
            parseInfo.addPosition(satId, new Vector3D(columns[0], columns[1], columns[2]), name());
            if (columns.length > 3) {
                parseInfo.addClockCorrection(satId, columns[3], name());
            }
        }
    },

    /** Predicate for VCS record. */
    VCS(RegexParts.LINE_START + " VCS" + RegexParts.STORED_SAT_ID +
        RegexParts.STORED_ONE_DIGIT + RegexParts.STORED_MULTIPLE_REALS + RegexParts.LINE_END,
        2, 3, 4, 7, 8) {
        @Override
        protected void store(final Matcher matcher, final double[] columns, final OrbexParseInfo parseInfo) {
            final SatInSystem satId = new SatInSystem(matcher.group(1));
            parseInfo.addVelocity(satId, new Vector3D(columns[0], columns[1], columns[2]), name());
            if (columns.length > 3) {
                parseInfo.addClockRate(satId, columns[3], name());
            }
        }
    },

    /** Predicate for POS record. */
    POS(RegexParts.LINE_START + " POS" + RegexParts.STORED_SAT_ID +
        RegexParts.IGNORED_FLAGS +
        RegexParts.STORED_ONE_DIGIT + RegexParts.STORED_MULTIPLE_REALS + RegexParts.LINE_END,
        2, 3) {
        @Override
        protected void store(final Matcher matcher, final double[] columns, final OrbexParseInfo parseInfo) {
            final SatInSystem satId = new SatInSystem(matcher.group(1));
            parseInfo.addPosition(satId, new Vector3D(columns[0], columns[1], columns[2]), name());
        }
    },

    /** Predicate for VEL record. */
    VEL(RegexParts.LINE_START + " VEL" + RegexParts.STORED_SAT_ID +
        RegexParts.STORED_ONE_DIGIT + RegexParts.STORED_MULTIPLE_REALS + RegexParts.LINE_END,
        2, 3) {
        @Override
        protected void store(final Matcher matcher, final double[] columns, final OrbexParseInfo parseInfo) {
            final SatInSystem satId = new SatInSystem(matcher.group(1));
            parseInfo.addVelocity(satId, new Vector3D(columns[0], columns[1], columns[2]), name());
        }
    },

    /** Predicate for CLK record. */
    CLK(RegexParts.LINE_START + " CLK" + RegexParts.STORED_SAT_ID +
        RegexParts.STORED_ONE_DIGIT + RegexParts.STORED_MULTIPLE_REALS + RegexParts.LINE_END,
        2, 1) {
        @Override
        protected void store(final Matcher matcher, final double[] columns, final OrbexParseInfo parseInfo) {
            final SatInSystem satId = new SatInSystem(matcher.group(1));
            parseInfo.addClockCorrection(satId, columns[0], name());
        }
    },

    /** Predicate for CRT record. */
    CRT(RegexParts.LINE_START + " CRT" + RegexParts.STORED_SAT_ID +
        RegexParts.STORED_ONE_DIGIT + RegexParts.STORED_MULTIPLE_REALS + RegexParts.LINE_END,
        2, 1) {
        @Override
        protected void store(final Matcher matcher, final double[] columns, final OrbexParseInfo parseInfo) {
            final SatInSystem satId = new SatInSystem(matcher.group(1));
            parseInfo.addClockRate(satId, columns[0], name());
        }
    },

    /** Predicate for ATT record. */
    ATT(RegexParts.LINE_START + " ATT" + RegexParts.STORED_SAT_ID +
        RegexParts.STORED_ONE_DIGIT + RegexParts.STORED_MULTIPLE_REALS + RegexParts.LINE_END,
        2, 4) {
        @Override
        protected void store(final Matcher matcher, final double[] columns, final OrbexParseInfo parseInfo) {
            final SatInSystem satId = new SatInSystem(matcher.group(1));
            parseInfo.addAttitude(satId, new Rotation(columns[0], columns[1], columns[2], columns[3], true));
        }
    };

    /** Pattern for the record. */
    private final Pattern pattern;

    /** Group index of the column number. */
    private final int columnGroup;

    /** Allowed numbers of columns. */
    private final int[] allowedColumns;

    /** Simple constructor.
     * @param regex regex for the record
     * @param columnGroup group index of the column number
     * @param allowedColumns allowed numbers of columns
     */
    EphemerisDataPredicate(final String regex, final int columnGroup, final int... allowedColumns) {
        this.pattern        = Pattern.compile(regex);
        this.columnGroup    = columnGroup;
        this.allowedColumns = allowedColumns.clone();
    }

    /** {@inheritDoc} */
    @Override
    public boolean test(final OrbexParseInfo parseInfo) {
        final Matcher matcher = pattern.matcher(parseInfo.getLine());
        if (matcher.matches()) {
            // the line corresponds to this predicate

            // check the number of columns
            final double[] columns;
            if (columnGroup < 0) {
                columns = null;
            } else {

                // check the internal consistency of the line itself
                // beware that the fields array as a spurious empty first element
                final int nbAnnounced = Integer.parseInt(matcher.group(columnGroup));
                final String[] fields = ParseInfo.SPLIT_AT_BLANKS.split(matcher.group(columnGroup + 1));
                if (nbAnnounced != fields.length - 1) {
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              parseInfo.getLineNumber(), parseInfo.getName(),
                                              parseInfo.getLine());
                }

                // check the number of fields with respect to the type of record
                boolean found = false;
                for (final int allowed : allowedColumns) {
                    // check if the announced number of columns matches one of the allowed numbers
                    found |= nbAnnounced == allowed;
                }
                if (!found) {
                    throw new OrekitException(OrekitMessages.ORBEX_WRONG_COLUMNS,
                                              parseInfo.getLineNumber(), parseInfo.getName(),
                                              name(), nbAnnounced);
                }

                // get the columns as real numbers
                columns = new double[nbAnnounced];
                for (int i = 0; i < nbAnnounced; i++) {
                    columns[i] = Double.parseDouble(fields[i + 1]);
                }

            }

            store(matcher, columns, parseInfo);
            return true;

        } else {
            // the line corresponds to another predicate
            return false;
        }
    }

    /**
     * Store parsed fields.
     *
     * @param matcher   matcher for the record regex
     * @param columns   real columns
     * @param parseInfo container for parse info
     */
    protected abstract void store(Matcher matcher, double[] columns, OrbexParseInfo parseInfo);

    /** Utility class for defining regex parts. */
    private static class RegexParts {

        /** Regex for line start. */
        private static final String LINE_START = "^";

        /** Regex for satellite ID. */
        private static final String STORED_SAT_ID = "\\p{Blank}(\\p{Alpha}\\p{Digit}{2})";

        /** Regex for one digit integer. */
        private static final String STORED_ONE_DIGIT = "\\p{Blank}+(\\p{Digit})";

        /** Regex for four digits integer. */
        private static final String STORED_FOUR_DIGITS = "\\p{Blank}+(\\p{Digit}{4})";

        /** Regex for integer. */
        private static final String STORED_INTEGER = "\\p{Blank}+(\\p{Digit}+)";

        /** Regex for real number. */
        private static final String STORED_SINGLE_REAL = "\\p{Blank}+([-0-9.]+)";

        /** Regex for several real numbers. */
        private static final String STORED_MULTIPLE_REALS = "((?:\\p{Blank}+[-0-9.]+)+)";

        /** Regex for ignored flagS. */
        private static final String IGNORED_FLAGS = " {4}[ E][ P] {2}[ M][ P] {3}";

        /** Regex for line end. */
        private static final String LINE_END = "\\p{Blank}*$";

        /** Private constructor for a utility class. */
        private RegexParts() {
            // nothing to do
        }

    }
}
