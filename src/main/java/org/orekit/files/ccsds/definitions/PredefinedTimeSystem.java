/* Contributed in the public domain.
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
package org.orekit.files.ccsds.definitions;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.utils.parsing.ParsingContext;
import org.orekit.time.SatelliteClockScale;

/**
 * The set of time systems defined in CCSDS standards (ADM, ODM, NDM).
 *
 * @author Evan Ward
 */
public enum PredefinedTimeSystem {

    /** Greenwich Mean Sidereal Time. */
    GMST {
        /** {@inheritDoc} */
        public TimeSystem getTimeSystem(final ParsingContext context) {
            return new TimeSystem(context.getDataContext().getTimeScales().getGMST(context.getConventions(), false));
        }
    },

    /** Global Positioning System. */
    GPS {
        /** {@inheritDoc} */
        public TimeSystem getTimeSystem(final ParsingContext context) {
            return new TimeSystem(context.getDataContext().getTimeScales().getGPS());
        }
    },

    /** Mission Elapsed Time. */
    MET {
        /** {@inheritDoc} */
        public TimeSystem getTimeSystem(final ParsingContext context) {
            return new TimeSystem(new SatelliteClockScale("MET",
                                                          context.getReferenceDate(),
                                                          context.getDataContext().getTimeScales().getUTC(),
                                                          0.0, 0.0));
        }
    },

    /** Mission Relative Time. */
    MRT {
        /** {@inheritDoc} */
        public TimeSystem getTimeSystem(final ParsingContext context) {
            return new TimeSystem(new SatelliteClockScale("MRT",
                                                          context.getReferenceDate(),
                                                          context.getDataContext().getTimeScales().getUTC(),
                                                          0.0, 0.0));
        }
    },

    /** Spacecraft Clock. Not currently Implemented. */
    SCLK {
        /** {@inheritDoc} */
        public TimeSystem getTimeSystem(final ParsingContext context) {
            return new TimeSystem(new SatelliteClockScale("SCLK",
                                                          context.getReferenceDate(),
                                                          context.getDataContext().getTimeScales().getUTC(),
                                                          context.getClockCount(),
                                                          context.getClockRate() - 1.0));
        }
    },

    /** International Atomic Time. */
    TAI {
        /** {@inheritDoc} */
        public TimeSystem getTimeSystem(final ParsingContext context) {
            return new TimeSystem(context.getDataContext().getTimeScales().getTAI());
        }
    },

    /** Barycentric Coordinate Time. */
    TCB {
        /** {@inheritDoc} */
        public TimeSystem getTimeSystem(final ParsingContext context) {
            return new TimeSystem(context.getDataContext().getTimeScales().getTCB());
        }
    },

    /** Barycentric Dynamical Time. */
    TDB {
        /** {@inheritDoc} */
        public TimeSystem getTimeSystem(final ParsingContext context) {
            return new TimeSystem(context.getDataContext().getTimeScales().getTDB());
        }
    },

    /** Geocentric Coordinate Time. */
    TCG {
        /** {@inheritDoc} */
        public TimeSystem getTimeSystem(final ParsingContext context) {
            return new TimeSystem(context.getDataContext().getTimeScales().getTCG());
        }
    },

    /** Terrestrial Time. */
    TT {
        /** {@inheritDoc} */
        public TimeSystem getTimeSystem(final ParsingContext context) {
            return new TimeSystem(context.getDataContext().getTimeScales().getTT());
        }
    },

    /** Universal Time. */
    UT1 {
        /** {@inheritDoc} */
        public TimeSystem getTimeSystem(final ParsingContext context) {
            return new TimeSystem(context.getDataContext().getTimeScales().getUT1(context.getConventions(), false));
        }
    },

    /** Universal Coordinated Time. */
    UTC {
        /** {@inheritDoc} */
        public TimeSystem getTimeSystem(final ParsingContext context) {
            return new TimeSystem(context.getDataContext().getTimeScales().getUTC());
        }
    };

    /** Get associated {@link TimeSystem}.
     * @param context parsing context
     * @return time system for reading/writing date
     * @since 11.0
     */
    public abstract TimeSystem getTimeSystem(ParsingContext context);

    /** Parse a value from a key=value entry.
     * @param value value to parse
     * @return CCSDS time system corresponding to the value
     */
    public static PredefinedTimeSystem parse(final String value) {
        for (final PredefinedTimeSystem scale : values()) {
            if (scale.name().equals(value)) {
                return scale;
            }
        }
        throw new OrekitException(OrekitMessages.CCSDS_TIME_SYSTEM_NOT_IMPLEMENTED, value);
    }

}
