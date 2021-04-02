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
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.SatelliteClockScale;

/**
 * The set of time systems defined in CCSDS standards (ADM, ODM, NDM).
 *
 * @author Evan Ward
 */
public enum TimeSystem {

    /** Greenwich Mean Sidereal Time. */
    GMST {
        /** {@inheritDoc} */
        public TimeConverter getConverter(final ContextBinding context) {
            return new TimeConverter(context.getDataContext().getTimeScales().getGMST(context.getConventions(), false),
                                     context.getReferenceDate());
        }
    },

    /** Global Positioning System. */
    GPS {
        /** {@inheritDoc} */
        public TimeConverter getConverter(final ContextBinding context) {
            return new TimeConverter(context.getDataContext().getTimeScales().getGPS(),
                                     context.getReferenceDate());
        }
    },

    /** Mission Elapsed Time. */
    MET {
        /** {@inheritDoc} */
        public TimeConverter getConverter(final ContextBinding context) {
            return new TimeConverter(new SatelliteClockScale("MET",
                                                             context.getReferenceDate(),
                                                             context.getDataContext().getTimeScales().getUTC(),
                                                             0.0, 0.0),
                                     context.getReferenceDate());
        }
    },

    /** Mission Relative Time. */
    MRT {
        /** {@inheritDoc} */
        public TimeConverter getConverter(final ContextBinding context) {
            return new TimeConverter(new SatelliteClockScale("MRT",
                                                             context.getReferenceDate(),
                                                             context.getDataContext().getTimeScales().getUTC(),
                                                             0.0, 0.0),
                                     context.getReferenceDate());
        }
    },

    /** Spacecraft Clock. */
    SCLK {
        /** {@inheritDoc} */
        public TimeConverter getConverter(final ContextBinding context) {
            return new TimeConverter(new SatelliteClockScale("SCLK",
                                                             context.getReferenceDate(),
                                                             context.getDataContext().getTimeScales().getUTC(),
                                                             context.getClockCount(),
                                                             context.getClockRate() - 1.0),
                                     context.getReferenceDate()) {
                /** {@inheritDoc}
                 * <p>
                 * Special implementation: the offset is a clock count rather than a duration
                 * </p>
                 */
                @Override
                public double offset(final AbsoluteDate date) {
                    return ((SatelliteClockScale) getTimeScale()).countAtDate(date);
                }
            };
        }
    },

    /** International Atomic Time. */
    TAI {
        /** {@inheritDoc} */
        public TimeConverter getConverter(final ContextBinding context) {
            return new TimeConverter(context.getDataContext().getTimeScales().getTAI(),
                                     context.getReferenceDate());
        }
    },

    /** Barycentric Coordinate Time. */
    TCB {
        /** {@inheritDoc} */
        public TimeConverter getConverter(final ContextBinding context) {
            return new TimeConverter(context.getDataContext().getTimeScales().getTCB(),
                                     context.getReferenceDate());
        }
    },

    /** Barycentric Dynamical Time. */
    TDB {
        /** {@inheritDoc} */
        public TimeConverter getConverter(final ContextBinding context) {
            return new TimeConverter(context.getDataContext().getTimeScales().getTDB(),
                                     context.getReferenceDate());
        }
    },

    /** Geocentric Coordinate Time. */
    TCG {
        /** {@inheritDoc} */
        public TimeConverter getConverter(final ContextBinding context) {
            return new TimeConverter(context.getDataContext().getTimeScales().getTCG(),
                                     context.getReferenceDate());
        }
    },

    /** Terrestrial Time. */
    TT {
        /** {@inheritDoc} */
        public TimeConverter getConverter(final ContextBinding context) {
            return new TimeConverter(context.getDataContext().getTimeScales().getTT(),
                                     context.getReferenceDate());
        }
    },

    /** Universal Time. */
    UT1 {
        /** {@inheritDoc} */
        public TimeConverter getConverter(final ContextBinding context) {
            return new TimeConverter(context.getDataContext().getTimeScales().getUT1(context.getConventions(), false),
                                     context.getReferenceDate());
        }
    },

    /** Universal Coordinated Time. */
    UTC {
        /** {@inheritDoc} */
        public TimeConverter getConverter(final ContextBinding context) {
            return new TimeConverter(context.getDataContext().getTimeScales().getUTC(),
                                     context.getReferenceDate());
        }
    };

    /** Get associated {@link TimeConverter}.
     * @param context context binding
     * @return time system for reading/writing date
     * @since 11.0
     */
    public abstract TimeConverter getConverter(ContextBinding context);

    /** Parse a value from a key=value entry.
     * @param value value to parse
     * @return CCSDS time system corresponding to the value
     */
    public static TimeSystem parse(final String value) {
        for (final TimeSystem scale : values()) {
            if (scale.name().equals(value)) {
                return scale;
            }
        }
        throw new OrekitException(OrekitMessages.CCSDS_TIME_SYSTEM_NOT_IMPLEMENTED, value);
    }

}
