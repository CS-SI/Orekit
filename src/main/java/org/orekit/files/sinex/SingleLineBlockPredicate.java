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
import org.orekit.files.sinex.Station.ReferenceSystem;
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
            final AbsoluteDate end = parseInfo.getCurrentLineEndDate();

            // antenna type
            final String type = parseInfo.parseString(42, 20);

            // special implementation for the first entry
            if (station.getAntennaTypeTimeSpanMap().getSpansNumber() == 1) {
                // we want null values outside validity limits of the station
                station.addAntennaTypeValidBefore(type, end);
                station.addAntennaTypeValidBefore(null, start);
            } else {
                station.addAntennaTypeValidBefore(type, end);
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
