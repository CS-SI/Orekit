/* Copyright 2022-2025 Luc Maisonobe
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
package org.orekit.files.sinex;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.sinex.Station.ReferenceSystem;
import org.orekit.gnss.GnssSignal;
import org.orekit.gnss.PredefinedGnssSignal;
import org.orekit.gnss.SatInSystem;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;

import java.util.function.Predicate;

/** Predicates for blocks containing a single type of lines.
 * @author Luc Maisonobe
 * @since 13.0
 */
enum SingleLineBlockPredicate implements Predicate<SinexParseInfo> {

    /** Predicate for SITE/ID block content. */
    SITE_ID {
        /** {@inheritDoc} */
        @Override
        public void parse(final SinexParseInfo parseInfo) {
            // read site id. data
            final Station station = new Station();
            station.setSiteCode(parseInfo.parseString(1, 4));
            station.setDomes(parseInfo.parseString(9, 9));
            parseInfo.addStation(station);
        }
    },

    /** Predicate for SITE/ANTENNA block content. */
    SITE_ANTENNA {
        /** {@inheritDoc} */
        @Override
        public void parse(final SinexParseInfo parseInfo) {

            // read antenna type data
            final Station station = parseInfo.getCurrentLineStation(1);

            // validity range
            final AbsoluteDate start = parseInfo.getCurrentLineStartDate();
            final AbsoluteDate end   = parseInfo.getCurrentLineEndDate();

            // antenna key
            final AntennaKey key = new AntennaKey(parseInfo.parseString(42, 16),
                                                  parseInfo.parseString(58,  4),
                                                  parseInfo.parseString(63,  5));

            // special implementation for the first entry
            if (station.getAntennaKeyTimeSpanMap().getSpansNumber() == 1) {
                // we want null values outside validity limits of the station
                station.addAntennaKeyValidBefore(key, end);
                station.addAntennaKeyValidBefore(null, start);
            } else {
                station.addAntennaKeyValidBefore(key, end);
            }

        }
    },

    /** Predicate for SITE/ECCENTRICITY block content. */
    SITE_ECCENTRICITY {
        /** {@inheritDoc} */
        @Override
        public void parse(final SinexParseInfo parseInfo) {

            // read antenna eccentricities data
            final Station station = parseInfo.getCurrentLineStation(1);

            // validity range
            final AbsoluteDate start = parseInfo.getCurrentLineStartDate();
            final AbsoluteDate end = parseInfo.getCurrentLineEndDate();

            // reference system UNE or XYZ
            station.setEccRefSystem(ReferenceSystem.getEccRefSystem(parseInfo.parseString(42, 3)));

            // eccentricity vector
            final Vector3D eccStation = new Vector3D(parseInfo.parseDouble(46, 8),
                                                     parseInfo.parseDouble(55, 8),
                                                     parseInfo.parseDouble(64, 8));

            // special implementation for the first entry
            if (station.getEccentricitiesTimeSpanMap().getSpansNumber() == 1) {
                // we want null values outside validity limits of the station
                station.addStationEccentricitiesValidBefore(eccStation, end);
                station.addStationEccentricitiesValidBefore(null, start);
            } else {
                station.addStationEccentricitiesValidBefore(eccStation, end);
            }

        }
    },

    /** Predicate for SOLUTION/EPOCHS block content. */
    SOLUTION_EPOCHS {
        /** {@inheritDoc} */
        @Override
        public void parse(final SinexParseInfo parseInfo) {

            // station
            final Station station = parseInfo.getCurrentLineStation(1);

            // validity range
            station.setValidFrom(parseInfo.getCurrentLineStartDate());
            station.setValidUntil(parseInfo.getCurrentLineEndDate());

        }
    },

    /** Predicate for SITE/GPS_PHASE_CENTER block content. */
    SITE_GPS_PHASE_CENTER {
        /** {@inheritDoc} */
        @Override
        public void parse(final SinexParseInfo parseInfo) {

            // antenna key
            final AntennaKey key = new AntennaKey(parseInfo.parseString(1, 16),
                                                  parseInfo.parseString(17, 4),
                                                  parseInfo.parseString(22, 5));

            // phase center for first signal in current line
            final Vector3D phaseCenter1 = new Vector3D(parseInfo.parseDouble(28, 6),
                                                       parseInfo.parseDouble(35, 6),
                                                       parseInfo.parseDouble(42, 6));
            parseInfo.addStationPhaseCenter(key, phaseCenter1, GPS_SIGNALS);

            // phase center for second signal in current line
            final Vector3D phaseCenter2 = new Vector3D(parseInfo.parseDouble(49, 6),
                                                       parseInfo.parseDouble(56, 6),
                                                       parseInfo.parseDouble(63, 6));
            parseInfo.addStationPhaseCenter(key, phaseCenter2, GPS_SIGNALS);

        }
    },

    /** Predicate for SITE/GAL_PHASE_CENTER block content. */
    SITE_GAL_PHASE_CENTER {
        /** {@inheritDoc} */
        @Override
        public void parse(final SinexParseInfo parseInfo) {

            // antenna key
            final AntennaKey key = new AntennaKey(parseInfo.parseString(1, 16),
                                                  parseInfo.parseString(17, 4),
                                                  parseInfo.parseString(22, 5));

            // phase center for first signal in current line
            final Vector3D phaseCenter1 = new Vector3D(parseInfo.parseDouble(28, 6),
                                                       parseInfo.parseDouble(35, 6),
                                                       parseInfo.parseDouble(42, 6));
            parseInfo.addStationPhaseCenter(key, phaseCenter1, GALILEO_SIGNALS);

            if (!parseInfo.parseString(49, 6).trim().isEmpty()) {
                // phase center for second signal in current line
                final Vector3D phaseCenter2 = new Vector3D(parseInfo.parseDouble(49, 6),
                                                           parseInfo.parseDouble(56, 6),
                                                           parseInfo.parseDouble(63, 6));
                parseInfo.addStationPhaseCenter(key, phaseCenter2, GALILEO_SIGNALS);
            }

        }
    },

    /** Predicate for SATELLITE/PHASE_CENTER block content. */
    SATELLITE_PHASE_CENTER {
        /** {@inheritDoc} */
        @Override
        public void parse(final SinexParseInfo parseInfo) {

            // satellite id
            final SatInSystem satInSystem = new SatInSystem(parseInfo.parseString(1, 4));

            // first signal in current line
            final GnssSignal signal1     = decode(satInSystem.getSystem(), parseInfo.parseInt(6, 1), parseInfo);
            // beware! the fields are in order Z, X, Y in the file
            final Vector3D   phaseCenter1 = new Vector3D(parseInfo.parseDouble(15, 6),
                                                         parseInfo.parseDouble(22, 6),
                                                         parseInfo.parseDouble( 8, 6));
            parseInfo.addSatellitePhaseCenter(satInSystem, signal1, phaseCenter1);

            if (!parseInfo.parseString(31, 6).trim().isEmpty()) {
                // second signal in current line
                final GnssSignal signal2      = decode(satInSystem.getSystem(), parseInfo.parseInt(29, 1), parseInfo);
                // beware! the fields are in order Z, X, Y in the file
                final Vector3D   phaseCenter2 = new Vector3D(parseInfo.parseDouble(38, 6),
                                                             parseInfo.parseDouble(45, 6),
                                                             parseInfo.parseDouble(31, 6));
                parseInfo.addSatellitePhaseCenter(satInSystem, signal2, phaseCenter2);
            }

        }

        /** Decode GNSS signal.
         * @param system satellite system
         * @param code frequency code
         * @param parseInfo holder for parse info
         * @return GNSS signal
         */
        private GnssSignal decode(final SatelliteSystem system, final int code, final SinexParseInfo parseInfo) {
            if (system == SatelliteSystem.GPS) {
                if (code == 1) {
                    return PredefinedGnssSignal.G01;
                } else if (code == 2) {
                    return PredefinedGnssSignal.G02;
                } else if (code == 5) {
                    return PredefinedGnssSignal.G05;
                }
            } else if (system == SatelliteSystem.GALILEO) {
                if (code == 1) {
                    return PredefinedGnssSignal.E01;
                } else if (code == 5) {
                    return PredefinedGnssSignal.E05;
                } else if (code == 6) {
                    return PredefinedGnssSignal.E06;
                } else if (code == 7) {
                    return PredefinedGnssSignal.E07;
                } else if (code == 8) {
                    return PredefinedGnssSignal.E08;
                }
            } else if (system == SatelliteSystem.GLONASS) {
                if (code == 1) {
                    // here, the SINEX specification lists L1 frequency, R01 is the closest one
                    return PredefinedGnssSignal.R01;
                } else if (code == 2) {
                    // here, the SINEX specification lists L2 frequency, R02 is the closest one
                    return PredefinedGnssSignal.R02;
                } else if (code == 5) {
                    // here, the SINEX specification lists L5 frequency, R03 is the closest one
                    return PredefinedGnssSignal.R03;
                }
            } else if (system == SatelliteSystem.QZSS) {
                // this is not in the SINEX specification, but some files use it
                if (code == 1) {
                    return PredefinedGnssSignal.J01;
                } else if (code == 2) {
                    return PredefinedGnssSignal.J02;
                } else if (code == 5) {
                    return PredefinedGnssSignal.J05;
                }
            }
            throw new OrekitException(OrekitMessages.UNKNOWN_GNSS_FREQUENCY,
                                      system, code, parseInfo.getLineNumber(), parseInfo.getName());
        }

    };

    /** Signals used for SITE/GPS_PHASE_CENTER. */
    private static final PredefinedGnssSignal[] GPS_SIGNALS =
        new PredefinedGnssSignal[] {
            PredefinedGnssSignal.G01, PredefinedGnssSignal.G02
        };

    /** Signals used for SITE/GAL_PHASE_CENTER. */
    private static final PredefinedGnssSignal[] GALILEO_SIGNALS =
        new PredefinedGnssSignal[] {
            PredefinedGnssSignal.E01, PredefinedGnssSignal.E05,
            PredefinedGnssSignal.E06, PredefinedGnssSignal.E07,
            PredefinedGnssSignal.E08
        };

    /** {@inheritDoc} */
    @Override
    public boolean test(final SinexParseInfo parseInfo) {
        if (parseInfo.getLine().charAt(0) != '-') {
            parse(parseInfo);
            return true;
        } else {
            return false;
        }
    }

    /** Parse one line.
     * @param parseInfo container for parse info
     */
    protected abstract void parse(SinexParseInfo parseInfo);

}
