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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.sinex.ParseInfo;
import org.orekit.utils.units.Prefix;
import org.orekit.utils.units.Unit;

/** Predicates for file/description block.
 * @author Luc Maisonobe
 * @since 14.0
 */
enum FileDescriptionPredicate
    implements Predicate<OrbexParseInfo> {

    /** Predicate for description. */
    DESCRIPTION {

        /** {@inheritDoc} */
        @Override
        protected void store(final OrbexParseInfo parseInfo, final String information) {
            parseInfo.setDescription(information);
        }

    },

    /** Predicate for created by. */
    CREATED_BY {
        @Override
        protected void store(final OrbexParseInfo parseInfo, final String information) {
            parseInfo.setCreatedBy(information);
        }
    },

    /** Predicate for creation date. */
    CREATION_DATE {
        @Override
        protected void store(final OrbexParseInfo parseInfo, final String information) {
            parseInfo.setCreationDate(information);
        }
    },

    /** Predicate for input data. */
    INPUT_DATA {
        @Override
        protected void store(final OrbexParseInfo parseInfo, final String information) {
            parseInfo.setInputData(information);
        }
    },

    /** Predicate for contact. */
    CONTACT {
        @Override
        protected void store(final OrbexParseInfo parseInfo, final String information) {
            parseInfo.setContact(information);
        }
    },

    /** Predicate for time system. */
    TIME_SYSTEM {
        @Override
        protected void store(final OrbexParseInfo parseInfo, final String information) {
            parseInfo.setTimeSystem(information);
        }
    },

    /** Predicate for start time. */
    START_TIME {
        @Override
        protected void store(final OrbexParseInfo parseInfo, final String information) {
            // note we currently only parse the calendar part
            // (anyway, all files found in the wild only contain this part)
            parseInfo.setStartDateIfEarlier(information.substring(0, 32));
        }
    },

    /** Predicate for end time. */
    END_TIME {
        @Override
        protected void store(final OrbexParseInfo parseInfo, final String information) {
            // note we currently only parse the calendar part
            // (anyway, all files found in the wild only contain this part)
            parseInfo.setEndDateIfLater(information.substring(0, 32));
        }
    },

    /** Predicate for epoch interval. */
    EPOCH_INTERVAL {
        @Override
        protected void store(final OrbexParseInfo parseInfo, final String information) {
            if ("IRREGULAR".equals(information.toUpperCase(Locale.ROOT))) {
                parseInfo.setEpochInterval(Double.NaN);
            } else {
                parseInfo.setEpochInterval(Double.parseDouble(information));
            }
        }
    },

    /** Predicate for coordinates system. */
    COORD_SYSTEM {
        @Override
        protected void store(final OrbexParseInfo parseInfo, final String information) {
            parseInfo.setCoordinateSystem(information);
        }
    },

    /** Predicate for frame type. */
    FRAME_TYPE {
        @Override
        protected void store(final OrbexParseInfo parseInfo, final String information) {
            parseInfo.setFrameType(information);
        }
    },

    /** Predicate for orbit type. */
    ORBIT_TYPE {
        @Override
        protected void store(final OrbexParseInfo parseInfo, final String information) {
            parseInfo.setOrbitType(information);
        }
    },

    /** Predicate for list of record types. */
    LIST_OF_REC_TYPES {
        @Override
        protected void store(final OrbexParseInfo parseInfo, final String information) {
            final List<EphemerisDataPredicate> recordTypes = new ArrayList<>();
            try {
                for (final String type : ParseInfo.SPLIT_AT_BLANKS.split(information)) {
                    recordTypes.add(EphemerisDataPredicate.valueOf(type));
                }
            } catch (IllegalArgumentException iae) {
                throw new OrekitException(iae,
                                          OrekitMessages.UNEXPECTED_DATA_AT_LINE_IN_FILE,
                                          parseInfo.getLineNumber(), parseInfo.getName());
            }
            parseInfo.setRecordTypes(recordTypes);
        }
    },

    /** Predicate for position units. */
    ORBIT_XYZ_UNITS {
        @Override
        protected void store(final OrbexParseInfo parseInfo, final String information) {
            parseInfo.setPositionUnit(Unit.parse(normalizeUnit(information)));
        }
    },

    /** Predicate for position reference. */
    ORBIT_XYZ_REFERENCE {
        @Override
        protected void store(final OrbexParseInfo parseInfo, final String information) {
            parseInfo.setOrbitReference(information);
        }
    },

    /** Predicate for velocity units. */
    ORBIT_VEL_UNITS {
        @Override
        protected void store(final OrbexParseInfo parseInfo, final String information) {
            parseInfo.setVelocityUnit(Unit.parse(normalizeUnit(information)));
        }
    },

    /** Predicate for clock units. */
    SVCLK_UNITS {
        @Override
        protected void store(final OrbexParseInfo parseInfo, final String information) {
            parseInfo.setClockCorrectionUnit(Unit.parse(normalizeUnit(information)));
        }
    },

    /** Predicate for clock rate units. */
    SVCLK_RATE_UNITS {
        @Override
        protected void store(final OrbexParseInfo parseInfo, final String information) {
            parseInfo.setClockRateUnit(Unit.parse(normalizeUnit(information)));
        }
    };

    /** {@inheritDoc} */
    @Override
    public boolean test(final OrbexParseInfo parseInfo) {
        if (name().equals(parseInfo.parseString(1, 19))) {
            // this is the data type we are concerned with
            store(parseInfo, parseInfo.parseString(21, 99));
            return true;
        } else {
            // it is a data type for another predicate
            return false;
        }
    }

    /** Store parsed fields.
     * @param parseInfo container for parse info
     * @param information information associated to the label
     */
    protected abstract void store(OrbexParseInfo parseInfo, String information);

    /** Normalize Unit.
     * <p>
     * This normalization method is very limited, it is only intended to support the
     * position, time and their first time derivative, and only in a few cases.
     * </p>
     * @param expanded expanded unit (like MICROSECONDS, or DECIMETERS/SECOND, or KILOMETERS)
     * @return normalized unit (like µs, dm/s, or km)
     */
    private static String normalizeUnit(final String expanded) {

        // handle prefix
        String result = expanded.toUpperCase(Locale.ROOT);
        for (final Prefix prefix : Prefix.values()) {
            result = result.replaceAll(prefix.name(), prefix.getSymbol());
        }

        // handle units
        result = result.replace("METERS",  "m");
        result = result.replace("METER",   "m");
        result = result.replace("SECONDS", "s");
        result = result.replace("SECOND",  "s");
        result = result.replace("SEC",     "s");

        return result;

    }

}
